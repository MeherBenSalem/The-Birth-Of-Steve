#!/usr/bin/env python3
"""Synthesises the 1.0 sound effects into assets/tbos/sounds/rpg/.

Every sound is built from modelled sources rather than samples: inharmonic bell,
glass-bar and singing-bowl partials (with beating pairs), FM, formant-filtered
voices, STFT-shaped noise sweeps, crackle grains and a frequency-damped synthetic
reverb. Each is mastered (high-pass, peak and loudness ceilings, fades) and
encoded as mono Ogg Vorbis, so world sounds stay positional.

The file names match tools/rpg/build_data.py SOUNDS. Everything is seeded, so a
rerun reproduces the same audio. Run from the repository root (needs numpy,
scipy and ffmpeg with libvorbis):

    python3 tools/rpg/sounds.py [--only name ...] [--sheet spectrograms.png]
"""
from __future__ import annotations

import argparse
import subprocess
import tempfile
import zlib
from pathlib import Path

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, fftconvolve, istft, sosfilt, stft

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'shared/common/src/main/resources/assets/tbos/sounds/rpg'
SR = 44100
TAU = 2 * np.pi

# Pitches (Hz). The set sits in D major, the Archive's key.
D3, F3, A3 = 146.83, 174.61, 220.00
D4, E4, F4, FS4, G4, A4, B4, C5 = 293.66, 329.63, 349.23, 369.99, 392.00, 440.00, 493.88, 523.25
CS5, D5, E5, FS5, G5, A5, B5 = 554.37, 587.33, 659.25, 739.99, 783.99, 880.00, 987.77
CS6, D6, E6, FS6, A6 = 1108.73, 1174.66, 1318.51, 1479.98, 1760.00


# ---- Building blocks ----------------------------------------------------------------------------------------


def T(dur: float) -> np.ndarray:
    return np.arange(int(dur * SR)) / SR


class Mix:
    """A growable mono bus; add() places a signal at a time offset."""

    def __init__(self, dur: float = 0.0):
        self.buf = np.zeros(int(dur * SR))

    def add(self, x: np.ndarray, at: float = 0.0, gain: float = 1.0) -> 'Mix':
        # Every part releases over its last fifth, so nothing stops while still ringing (a click).
        x = x.copy()
        tail = int(min(max(0.2 * len(x), 0.02 * SR), 0.3 * SR, len(x)))
        if tail:
            x[-tail:] *= np.cos(np.linspace(0, np.pi / 2, tail)) ** 2
        start = int(at * SR)
        end = start + len(x)
        if end > len(self.buf):
            self.buf = np.concatenate([self.buf, np.zeros(end - len(self.buf))])
        self.buf[start:end] += x * gain
        return self

    def out(self) -> np.ndarray:
        return self.buf


def band(x: np.ndarray, lo: float | None = None, hi: float | None = None, order: int = 2) -> np.ndarray:
    if lo and hi:
        sos = butter(order, [lo, hi], 'bandpass', fs=SR, output='sos')
    elif lo:
        sos = butter(order, lo, 'highpass', fs=SR, output='sos')
    elif hi:
        sos = butter(order, hi, 'lowpass', fs=SR, output='sos')
    else:
        return x
    return sosfilt(sos, x)


def onset(t: np.ndarray, attack: float) -> np.ndarray:
    return 1.0 - np.exp(-t / max(attack, 1e-4))


def modal(freqs, amps, taus, dur: float, rng, attack: float = 0.002, beat: float = 0.0) -> np.ndarray:
    """Damped sinusoids. With beat > 0 each mode is a slightly detuned pair, so it shimmers like real metal."""
    t = T(dur)
    y = np.zeros_like(t)
    for f, a, tau in zip(freqs, amps, taus):
        if f >= SR * 0.45 or a == 0:
            continue
        env = np.exp(-t / tau)
        if beat:
            d = beat * rng.uniform(0.6, 1.4)
            y += a * 0.5 * env * (np.sin(TAU * f * t + rng.uniform(0, TAU)) +
                                  np.sin(TAU * (f + d) * t + rng.uniform(0, TAU)))
        else:
            y += a * env * np.sin(TAU * f * t + rng.uniform(0, TAU))
    return y * onset(t, attack)


def mallet(rng, dur=0.004, lo=2500.0, hi=9000.0) -> np.ndarray:
    t = T(dur)
    return band(rng.standard_normal(len(t)) * np.exp(-t / (dur / 4)), lo, hi)


def glass(f: float, rng, dur: float = 2.0, tau: float = 0.9, bright: float = 1.0, hit: float = 0.12) -> np.ndarray:
    """A struck glass bar / crystal chime (free-free bar mode ratios)."""
    ratios = [1.0, 2.756, 5.404, 8.933]
    amps = [1.0, 0.42 * bright, 0.2 * bright, 0.08 * bright]
    taus = [tau, tau * 0.42, tau * 0.22, tau * 0.12]
    y = modal([f * r for r in ratios], amps, taus, dur, rng, attack=0.0015, beat=0.7)
    m = Mix().add(y).add(mallet(rng), gain=hit)
    return m.out()[:len(y)]


def bowl(f: float, rng, dur: float = 3.0, tau: float = 1.6, attack: float = 0.012) -> np.ndarray:
    """A singing bowl: few modes, long decay and strong beating."""
    ratios = [1.0, 2.71, 5.15, 8.17]
    return modal([f * r for r in ratios], [1.0, 0.55, 0.22, 0.08], [tau, tau * 0.7, tau * 0.38, tau * 0.2], dur,
                 rng, attack=attack, beat=1.6)


def bell(f: float, rng, dur: float = 4.0, tau: float = 1.4, strike: float = 0.25) -> np.ndarray:
    """A bronze bell (hum, prime, tierce, quint, nominal and upper partials)."""
    ratios = [0.5, 1.0, 1.183, 1.506, 2.0, 2.514, 2.662, 3.011, 4.166, 5.433, 6.796]
    amps = [0.38, 0.85, 0.55, 0.32, 1.0, 0.28, 0.42, 0.28, 0.22, 0.12, 0.08]
    taus = [tau * k for k in (1.7, 1.0, 0.8, 0.6, 0.72, 0.45, 0.5, 0.38, 0.28, 0.2, 0.14)]
    y = modal([f * r for r in ratios], amps, taus, dur, rng, attack=0.0012, beat=1.1)
    hit = band(rng.standard_normal(int(0.012 * SR)) * np.exp(-T(0.012) / 0.003), 600, 5000)
    return Mix().add(y).add(hit, gain=strike).out()[:len(y)]


def fm(fc: float, ratio: float, index: float, dur: float, tau: float, index_tau: float, rng) -> np.ndarray:
    t = T(dur)
    i = index * np.exp(-t / index_tau)
    return np.sin(TAU * fc * t + i * np.sin(TAU * fc * ratio * t + rng.uniform(0, TAU))) * np.exp(-t / tau) * onset(t, 0.002)


def glide(f0: float, f1: float, dur: float, curve: float = 1.0, vib: float = 0.0, vib_depth: float = 0.0) -> np.ndarray:
    """A sine whose pitch moves from f0 to f1 (exponentially), with optional vibrato."""
    t = T(dur)
    x = (t / dur) ** curve
    f = f0 * (f1 / f0) ** x
    if vib:
        f = f * (1 + vib_depth * np.sin(TAU * vib * t))
    return np.sin(TAU * np.cumsum(f) / SR)


def boom(rng, dur: float = 1.2, f0: float = 110.0, f1: float = 34.0, tau: float = 0.35, drive: float = 2.0) -> np.ndarray:
    t = T(dur)
    y = glide(f0, f1, dur, curve=0.35) * np.exp(-t / tau)
    y = np.tanh(y * drive) / np.tanh(drive)
    click = band(rng.standard_normal(int(0.006 * SR)) * np.exp(-T(0.006) / 0.0015), 200, 3000)
    return Mix().add(y).add(click, gain=0.35).out()[:len(y)]


def stft_shape(x: np.ndarray, gain_fn) -> np.ndarray:
    """Filters x with a time-varying magnitude response gain_fn(freqs, times) -> (F, N) array."""
    n = len(x)
    f, tt, z = stft(x, SR, nperseg=1024, noverlap=768, boundary='even')
    z *= gain_fn(f[:, None], tt[None, :])
    _, y = istft(z, SR, nperseg=1024, noverlap=768, boundary=True)
    return y[:n] if len(y) >= n else np.pad(y, (0, n - len(y)))


def sweep(rng, dur: float, centre, width_oct: float = 0.9, pink: bool = True) -> np.ndarray:
    """Noise through a band whose centre (Hz) follows centre(t); centre may be a callable or (f0, f1)."""
    noise = rng.standard_normal(int(dur * SR))
    if pink:
        noise = band(noise, hi=9000, order=1)
    if not callable(centre):
        a, b = centre
        centre_fn = lambda t: a * (b / a) ** np.clip(t / dur, 0, 1)
    else:
        centre_fn = centre

    def gain(fr, tt):
        c = centre_fn(tt)
        oct_ = np.log2(np.maximum(fr, 20.0) / c)
        return np.exp(-0.5 * (oct_ / width_oct) ** 2)

    return stft_shape(noise, gain)


def crackle(rng, dur: float, density, lo: float = 1800.0, hi: float = 8000.0, grain: float = 0.0025) -> np.ndarray:
    """Sparse random clicks; density (per second) may be a callable of time."""
    n = int(dur * SR)
    t = np.arange(n) / SR
    rate = density(t) if callable(density) else np.full(n, float(density))
    hits = rng.random(n) < rate / SR
    amps = rng.pareto(2.2, n) * hits * rng.choice([-1, 1], n)
    k = T(grain)
    kernel = rng.standard_normal(len(k)) * np.exp(-k / (grain / 4))
    y = fftconvolve(amps, kernel)[:n]
    return band(y, lo, hi)


def grains(rng, dur: float, density, f_lo: float, f_hi: float, tau: float = 0.08, amp: float = 1.0) -> np.ndarray:
    """Tiny glass pings scattered in time: sparkle and shimmer."""
    m = Mix(dur)
    t = 0.0
    while t < dur:
        rate = density(t) if callable(density) else density
        t += rng.exponential(1.0 / max(rate, 1e-3))
        if t >= dur:
            break
        f = np.exp(rng.uniform(np.log(f_lo), np.log(f_hi)))
        g = modal([f, f * 2.756], [1.0, 0.3], [tau, tau * 0.4], tau * 6, rng, attack=0.0008)
        m.add(g, t, amp * rng.uniform(0.3, 1.0))
    return m.out()[:int(dur * SR)]


FORMANTS = {
    'ah': [(730, 1.0, 90), (1090, 0.5, 110), (2440, 0.25, 160)],
    'oo': [(300, 1.0, 60), (870, 0.35, 90), (2240, 0.08, 140)],
    'oh': [(570, 1.0, 80), (840, 0.6, 90), (2410, 0.15, 140)],
}


def formant(x: np.ndarray, vowel: str) -> np.ndarray:
    y = np.zeros_like(x)
    for fc, g, bw in FORMANTS[vowel]:
        y += g * band(x, fc - bw, fc + bw, order=2)
    return y


def voice(f: float, dur: float, rng, vowel: str = 'ah', attack: float = 0.3, release: float = 0.6,
          detune=(-8.0, 0.0, 7.0), vib: float = 5.2, vib_depth: float = 0.005) -> np.ndarray:
    """A small choir section on one pitch: detuned band-limited saws, vibrato, formants."""
    t = T(dur)
    src = np.zeros_like(t)
    for cents in detune:
        ff = f * 2 ** (cents / 1200)
        inst = ff * (1 + vib_depth * np.sin(TAU * vib * t + rng.uniform(0, TAU)))
        phase = TAU * np.cumsum(inst) / SR + rng.uniform(0, TAU)
        for h in range(1, 40):
            if h * ff > SR * 0.42:
                break
            src += np.sin(h * phase) / h
    y = formant(src, vowel)
    env = onset(t, attack / 3) * np.clip((dur - t) / release, 0, 1)
    return y * env


def reverb(x: np.ndarray, rng, rt60: float = 1.6, mix: float = 0.3, predelay: float = 0.012,
           dark: float = 0.5, size: float = 1.0, early: bool = True) -> np.ndarray:
    """Convolution with a synthetic room: early taps plus a band-split, exponentially decaying tail."""
    n = int(rt60 * 1.15 * SR)
    t = np.arange(n) / SR
    noise = rng.standard_normal(n)
    low = band(noise, hi=700)
    mid = band(noise, 700, 3500)
    high = band(noise, lo=3500)
    tail = (low * np.exp(-6.9 * t / rt60) + mid * np.exp(-6.9 * t / (rt60 * (1 - 0.35 * dark))) +
            high * np.exp(-6.9 * t / (rt60 * (1 - 0.7 * dark))))
    tail *= onset(t, 0.02 * size)
    ir = np.zeros(n + int(0.08 * SR))
    taps = ((0, 1.0), (11, 0.42), (17, -0.33), (23, 0.28), (31, -0.22), (43, 0.17), (59, 0.12)) if early else ((0, 1.0),)
    for ms, g in taps:
        ir[int(ms * size * SR / 1000)] += g * 0.6
    start = int(predelay * SR)
    ir[start:start + n] += tail * 0.9
    ir /= np.sqrt(np.sum(ir ** 2))
    wet = fftconvolve(x, ir)
    dry = np.pad(x, (0, len(wet) - len(x)))
    wet *= np.max(np.abs(dry)) / (np.max(np.abs(wet)) + 1e-9)
    return dry * (1 - mix * 0.5) + wet * mix


def fade(x: np.ndarray, fin: float = 0.0015, fout: float = 0.04) -> np.ndarray:
    x = x.copy()
    a, b = int(fin * SR), int(fout * SR)
    if a:
        x[:a] *= np.linspace(0, 1, a)
    if b and b < len(x):
        x[-b:] *= np.cos(np.linspace(0, np.pi / 2, b)) ** 2
    return x


def master(x: np.ndarray, peak_db: float = -1.0, rms_db: float = -15.0, hp: float = 35.0) -> np.ndarray:
    x = band(x, lo=hp, order=2)
    # Trim the tail once it falls 60 dB under the peak.
    peak = np.max(np.abs(x)) + 1e-12
    env = np.abs(x) / peak
    alive = np.nonzero(env > 10 ** (-60 / 20))[0]
    if len(alive):
        x = x[:min(len(x), alive[-1] + int(0.02 * SR))]
    x = x / (np.max(np.abs(x)) + 1e-12) * 10 ** (peak_db / 20)
    # Loudness ceiling on the loudest 300 ms window.
    w = int(0.3 * SR)
    if len(x) > w:
        power = np.convolve(x ** 2, np.ones(w) / w, mode='valid')
        loud = 10 * np.log10(np.max(power) + 1e-12)
        if loud > rms_db:
            x *= 10 ** ((rms_db - loud) / 20)
    return fade(x)


# ---- The sounds ----------------------------------------------------------------------------------------------


def s_attune(rng):
    m = Mix(2.2)
    m.add(sweep(rng, 1.2, (1800, 7000), 0.6) * np.sin(np.linspace(0, np.pi, int(1.2 * SR))) ** 2, 0.0, 0.16)
    m.add(grains(rng, 1.1, lambda t: 20 + 40 * t, 3000, 9000, 0.05), 0.05, 0.12)
    m.add(bowl(D4, rng, 2.2, tau=1.3, attack=0.02), 0.0, 0.8)
    m.add(glass(A5, rng, 1.6, tau=0.8), 0.32, 0.35)
    m.add(glass(D6, rng, 1.4, tau=0.6, bright=0.7), 0.5, 0.18)
    return reverb(m.out(), rng, rt60=1.6, mix=0.32)


def s_distortion(rng):
    m = Mix(0.9)
    m.add(boom(rng, 0.5, 120, 55, tau=0.1, drive=1.5), 0, 0.7)
    m.add(fm(A3, 1.41, 3.2, 0.8, 0.22, 0.08, rng), 0, 0.45)
    m.add(fm(A3 * 2 ** (1 / 12), 1.41, 3.0, 0.8, 0.2, 0.07, rng), 0, 0.4)
    m.add(crackle(rng, 0.5, lambda t: 160 * np.exp(-t / 0.12), 1500, 6000), 0.02, 0.3)
    y = np.tanh(m.out() * 1.6)
    return reverb(band(y, hi=6000), rng, rt60=0.8, mix=0.22, dark=0.8)


def s_begin(rng):
    m = Mix(3.0)
    rise = sweep(rng, 1.0, (250, 3200), 0.8)
    m.add(rise * np.linspace(0, 1, len(rise)) ** 2, 0.0, 0.55)
    m.add(boom(rng, 1.6, 90, 32, tau=0.5, drive=2.5), 0.9, 0.95)
    m.add(sweep(rng, 1.2, (3000, 400), 1.0) * np.exp(-T(1.2) / 0.35), 0.9, 0.3)
    m.add(bell(D4, rng, 2.6, tau=1.1, strike=0.2), 0.9, 0.55)
    for f, dt in ((D5, 0.0), (FS5, 0.03), (A5, 0.06), (D6, 0.1)):
        m.add(glass(f, rng, 1.8, tau=0.9, bright=0.8), 0.92 + dt, 0.22)
    return reverb(m.out(), rng, rt60=2.4, mix=0.36)


def s_recollection(rng):
    chord = Mix(2.4)
    for f, dt in ((D5, 0.0), (FS5, 0.0), (A5, 0.0), (CS6, 0.0)):
        chord.add(glass(f, rng, 2.4, tau=1.1, bright=0.7, hit=0.05), dt, 0.3)
    hit = chord.out()
    # A swell made from the chord's own reversed reverb, leading into the strike.
    swell = reverb(hit, rng, rt60=2.0, mix=1.0, early=False)[::-1][-int(0.95 * SR):]
    swell *= np.linspace(0, 1, len(swell)) ** 1.5
    m = Mix(3.2)
    m.add(swell, 0.0, 0.7)
    m.add(hit, 0.95, 1.0)
    m.add(voice(D4, 2.2, rng, 'ah', attack=0.6, release=1.0), 0.8, 0.09)
    m.add(voice(A4, 2.2, rng, 'ah', attack=0.6, release=1.0), 0.8, 0.07)
    m.add(grains(rng, 1.6, lambda t: 30 * np.exp(-t), 4000, 10000, 0.05), 0.95, 0.1)
    return reverb(m.out(), rng, rt60=2.6, mix=0.38)


def s_recorded(rng):
    m = Mix(2.6)
    m.add(bell(A3, rng, 2.6, tau=1.0, strike=0.3), 0.0, 0.8)
    for i, f in enumerate((D5, FS5, A5, D6)):
        m.add(glass(f, rng, 1.6, tau=0.8, bright=0.8), 0.18 + i * 0.075, 0.26)
    return reverb(m.out(), rng, rt60=1.8, mix=0.3)


def s_key_rise(rng):
    m = Mix(1.6)
    for i, f in enumerate((D5, E5, FS5, A5, B5, D6, E6)):
        m.add(glass(f, rng, 1.0, tau=0.45 + i * 0.04, bright=0.6 + i * 0.08), i * 0.055, 0.28 + i * 0.02)
    m.add(sweep(rng, 0.8, (3000, 9000), 0.5) * np.sin(np.linspace(0, np.pi, int(0.8 * SR))), 0.0, 0.08)
    m.add(grains(rng, 0.9, 50, 5000, 11000, 0.04), 0.2, 0.08)
    return reverb(m.out(), rng, rt60=1.4, mix=0.3)


def s_remember(rng):
    m = Mix(2.8)
    for f, g in ((D4, 0.16), (FS4, 0.12), (A4, 0.12), (D5, 0.08)):
        m.add(voice(f, 2.3, rng, 'ah', attack=0.35, release=1.1), 0.05, g)
    m.add(bowl(D5, rng, 2.4, tau=1.2), 0.0, 0.5)
    m.add(glass(D6, rng, 1.6, tau=0.8, bright=0.6), 0.28, 0.2)
    m.add(glass(A5, rng, 1.6, tau=0.8, bright=0.6), 0.4, 0.16)
    return reverb(m.out(), rng, rt60=2.2, mix=0.34)


def s_forget(rng):
    m = Mix(2.8)
    fall = sweep(rng, 1.6, (3200, 140), 1.0)
    m.add(fall * np.sin(np.linspace(0, np.pi, len(fall))) ** 0.7, 0.0, 0.6)
    t = T(1.8)
    m.add(glide(160, 38, 1.8, curve=0.6) * np.exp(-t / 0.7) * onset(t, 0.15), 0.1, 0.8)
    for f in (D3, F3, A3):
        m.add(band(voice(f, 2.2, rng, 'oo', attack=0.4, release=1.2), hi=1800), 0.15, 0.12)
    y = reverb(m.out(), rng, rt60=2.6, mix=0.4, dark=0.85)
    return band(y, hi=7000)


def s_forgotten(rng):
    m = Mix(2.4)
    crack = band(rng.standard_normal(int(0.05 * SR)) * np.exp(-T(0.05) / 0.008), lo=2500)
    m.add(crack, 0.0, 0.6)
    m.add(grains(rng, 0.5, lambda t: 140 * np.exp(-t / 0.12), 3500, 11000, 0.03), 0.0, 0.35)
    for i, f in enumerate((D5, C5, A4, F4)):
        m.add(glass(f, rng, 1.4, tau=0.65, bright=0.55), 0.12 + i * 0.13, 0.32 - i * 0.03)
    return reverb(m.out(), rng, rt60=2.0, mix=0.36, dark=0.7)


def s_hourglass_tick(rng):
    m = Mix(0.3)
    m.add(modal([1850, 2960, 4210], [1.0, 0.5, 0.18], [0.022, 0.012, 0.06], 0.3, rng, attack=0.0004), 0, 0.9)
    m.add(mallet(rng, 0.003, 3000, 10000), 0, 0.35)
    m.add(modal([620], [1.0], [0.03], 0.2, rng), 0, 0.25)
    return reverb(m.out(), rng, rt60=0.35, mix=0.15, size=0.4)


def s_hourglass_end(rng):
    m = Mix(4.2)
    m.add(bell(D3, rng, 4.2, tau=2.0, strike=0.35), 0.0, 1.0)
    m.add(bowl(D4, rng, 3.6, tau=1.8), 0.0, 0.25)
    t = T(3.0)
    m.add(np.sin(TAU * D3 / 2 * t) * np.exp(-t / 1.4) * onset(t, 0.03), 0.0, 0.3)
    return reverb(m.out(), rng, rt60=2.8, mix=0.34)


def flip(rng, dur: float = 0.22) -> np.ndarray:
    """A card or page snapping over: a fast swish plus papery crinkle."""
    t = T(dur)
    env = (t / 0.03) ** 1.5 * np.exp(-t / 0.04)
    env /= env.max()
    swish = sweep(rng, dur, (1500, 5200), 0.8) * env
    crinkle = crackle(rng, dur, lambda tt: 2200 * np.exp(-tt / 0.05), 1200, 7000, 0.0015)
    return Mix().add(swish).add(crinkle, gain=0.25).out()[:len(t)]


def s_reveal_common(rng):
    m = Mix(0.8)
    m.add(flip(rng), 0.0, 0.8)
    m.add(modal([980, 1640], [1.0, 0.35], [0.04, 0.02], 0.3, rng), 0.06, 0.3)
    m.add(glass(A5, rng, 0.6, tau=0.25, bright=0.4), 0.08, 0.1)
    return reverb(m.out(), rng, rt60=0.7, mix=0.18)


def s_reveal_rare(rng):
    m = Mix(1.4)
    m.add(flip(rng), 0.0, 0.7)
    m.add(glass(A5, rng, 1.2, tau=0.6, bright=0.8), 0.07, 0.42)
    m.add(glass(E6, rng, 1.0, tau=0.45, bright=0.6), 0.16, 0.24)
    return reverb(m.out(), rng, rt60=1.2, mix=0.26)


def s_reveal_epic(rng):
    m = Mix(1.8)
    m.add(flip(rng), 0.0, 0.65)
    m.add(glass(A5, rng, 1.4, tau=0.7), 0.07, 0.36)
    m.add(glass(D6, rng, 1.4, tau=0.7), 0.17, 0.34)
    m.add(glass(FS6, rng, 1.2, tau=0.55, bright=0.7), 0.27, 0.22)
    m.add(grains(rng, 1.0, lambda t: 60 * np.exp(-t / 0.4), 4500, 11000, 0.05), 0.1, 0.12)
    m.add(sweep(rng, 0.7, (2500, 8000), 0.5) * np.sin(np.linspace(0, np.pi, int(0.7 * SR))), 0.05, 0.07)
    return reverb(m.out(), rng, rt60=1.6, mix=0.3)


def s_reveal_legendary(rng):
    m = Mix(3.0)
    m.add(flip(rng), 0.0, 0.6)
    m.add(boom(rng, 1.2, 100, 40, tau=0.3, drive=1.8), 0.06, 0.5)
    m.add(bell(D4, rng, 2.8, tau=1.2, strike=0.2), 0.06, 0.45)
    for i, f in enumerate((D5, FS5, A5, D6, FS6, A6)):
        m.add(glass(f, rng, 1.6, tau=0.75, bright=0.8), 0.1 + i * 0.06, 0.24)
    m.add(grains(rng, 1.8, lambda t: 80 * np.exp(-t / 0.6), 4000, 11000, 0.06), 0.1, 0.13)
    m.add(voice(D5, 1.8, rng, 'ah', attack=0.15, release=0.9), 0.1, 0.05)
    return reverb(m.out(), rng, rt60=2.2, mix=0.34)


def s_reveal_remnant(rng):
    m = Mix(3.2)
    m.add(flip(rng), 0.0, 0.5)
    m.add(boom(rng, 1.6, 80, 28, tau=0.5, drive=3.0), 0.05, 0.95)
    m.add(band(rng.standard_normal(int(0.4 * SR)) * np.exp(-T(0.4) / 0.07), hi=1200), 0.05, 0.4)
    m.add(bell(D4, rng, 2.8, tau=1.1, strike=0.3), 0.08, 0.35)
    m.add(bell(D4 * 2 ** (1 / 12), rng, 2.8, tau=1.0, strike=0.0), 0.08, 0.25)
    m.add(bell(D4 * 2 ** (6 / 12), rng, 2.4, tau=0.9, strike=0.0), 0.1, 0.18)
    m.add(crackle(rng, 2.2, lambda t: 90 * np.exp(-t / 0.9), 1500, 7000, 0.003), 0.15, 0.35)
    y = np.tanh(m.out() * 1.3)
    return reverb(y, rng, rt60=2.4, mix=0.36, dark=0.75)


def s_codex_open(rng):
    m = Mix(1.2)
    t = T(0.25)
    m.add(band(rng.standard_normal(len(t)) * np.exp(-t / 0.03), hi=500) * 1.6, 0.0, 0.7)
    m.add(np.sin(TAU * 105 * t) * np.exp(-t / 0.06), 0.0, 0.5)
    m.add(flip(rng, 0.35), 0.05, 0.5)
    m.add(crackle(rng, 0.5, lambda tt: 900 * np.exp(-tt / 0.15), 900, 6000, 0.002), 0.08, 0.2)
    m.add(glass(D6, rng, 0.9, tau=0.5, bright=0.5), 0.14, 0.09)
    return reverb(m.out(), rng, rt60=0.9, mix=0.2)


def s_codex_page(rng):
    m = Mix(0.45)
    m.add(flip(rng, 0.32), 0.0, 1.0)
    m.add(crackle(rng, 0.3, lambda tt: 1500 * np.exp(-((tt - 0.12) / 0.07) ** 2), 1000, 6500, 0.0018), 0.0, 0.3)
    return reverb(m.out(), rng, rt60=0.5, mix=0.12, size=0.5)


def s_codex_insight(rng):
    m = Mix(1.8)
    m.add(bowl(A4, rng, 1.8, tau=1.0), 0.0, 0.55)
    m.add(glass(E6, rng, 1.2, tau=0.6, bright=0.7), 0.1, 0.25)
    m.add(glass(A6, rng, 1.0, tau=0.5, bright=0.6), 0.2, 0.18)
    rise = sweep(rng, 0.7, (2000, 9000), 0.5)
    m.add(rise * np.sin(np.linspace(0, np.pi, len(rise))) ** 2, 0.0, 0.14)
    m.add(grains(rng, 0.8, lambda t: 30 + 60 * t, 5000, 11000, 0.04), 0.1, 0.1)
    return reverb(m.out(), rng, rt60=1.5, mix=0.3)


def s_codex_renown(rng):
    m = Mix(1.9)
    for i in range(9):
        f = rng.uniform(2600, 5200)
        m.add(modal([f, f * 1.47, f * 2.09], [1.0, 0.6, 0.3], [0.18, 0.12, 0.08], 0.6, rng, beat=4.0),
              i * 0.045 + rng.uniform(0, 0.02), 0.18)
    for f, dt in ((D6, 0.12), (FS6, 0.16), (A6, 0.2)):
        m.add(glass(f, rng, 1.3, tau=0.6, bright=0.7), dt, 0.22)
    return reverb(m.out(), rng, rt60=1.4, mix=0.3)


def s_party_invite(rng):
    m = Mix(1.1)
    m.add(glass(A5, rng, 0.9, tau=0.45, bright=0.6), 0.0, 0.45)
    m.add(glass(D6, rng, 0.9, tau=0.5, bright=0.6), 0.13, 0.45)
    m.add(bowl(D5, rng, 0.9, tau=0.5), 0.0, 0.2)
    return reverb(m.out(), rng, rt60=1.0, mix=0.24)


def s_party_join(rng):
    m = Mix(1.4)
    for i, f in enumerate((D5, FS5, A5)):
        m.add(glass(f, rng, 1.0, tau=0.5, bright=0.6), i * 0.1, 0.4)
    m.add(voice(D5, 1.0, rng, 'ah', attack=0.1, release=0.6), 0.2, 0.05)
    return reverb(m.out(), rng, rt60=1.2, mix=0.26)


def s_bolster(rng):
    dur = 1.1
    t = T(dur)
    src = np.zeros_like(t)
    for f, c in ((55.0, -6), (55.0, 7), (82.4, 0)):
        ph = TAU * np.cumsum(np.full_like(t, f * 2 ** (c / 1200))) / SR
        for h in range(1, 60):
            if h * f > SR * 0.4:
                break
            src += np.sin(h * ph + rng.uniform(0, TAU)) / h

    def gain(fr, tt):
        fc = 180 * (1600 / 180) ** np.clip(tt / 0.6, 0, 1)
        lp = 1 / np.sqrt(1 + (fr / fc) ** 6)
        res = 2.2 * np.exp(-0.5 * (np.log2(np.maximum(fr, 20) / fc) / 0.2) ** 2)
        return lp + res

    growl = stft_shape(src, gain) * onset(t, 0.04) * np.exp(-t / 0.55)
    m = Mix(dur + 0.6)
    m.add(np.tanh(growl * 1.2), 0.0, 0.7)
    m.add(modal([3150, 4870, 6920], [1.0, 0.6, 0.3], [0.3, 0.2, 0.12], 0.8, rng, beat=6.0), 0.12, 0.2)
    m.add(sweep(rng, 0.5, (4000, 9000), 0.5) * np.exp(-T(0.5) / 0.12), 0.12, 0.2)
    return reverb(m.out(), rng, rt60=1.0, mix=0.22, dark=0.6)


def s_volatile_hiss(rng):
    dur = 1.5
    t = T(dur)
    m = Mix(dur + 0.1)
    hiss = sweep(rng, dur, lambda tt: 3500 * (8500 / 3500) ** np.clip(tt / dur, 0, 1), 0.7)
    m.add(hiss * (0.25 + 0.75 * (t / dur) ** 2), 0.0, 0.35)
    m.add(crackle(rng, dur, lambda tt: 60 + 700 * (tt / dur) ** 2, 2000, 9000, 0.002), 0.0, 0.9)
    m.add(grains(rng, dur, lambda tt: 8 + 50 * (tt / dur) ** 2, 4000, 9500, 0.03), 0.0, 0.25)
    y = m.out()
    y[int(1.46 * SR):] *= np.linspace(1, 0, len(y) - int(1.46 * SR))
    return y


def s_volatile_burst(rng):
    m = Mix(1.8)
    t = T(0.6)
    m.add(band(rng.standard_normal(len(t)) * np.exp(-t / 0.09), 120, 9000), 0.0, 0.9)
    m.add(boom(rng, 1.2, 95, 30, tau=0.32, drive=3.0), 0.0, 1.0)
    m.add(crackle(rng, 1.2, lambda tt: 500 * np.exp(-tt / 0.25), 1500, 8000, 0.003), 0.03, 0.6)
    m.add(sweep(rng, 0.8, (5000, 600), 1.0) * np.exp(-T(0.8) / 0.18), 0.0, 0.35)
    y = np.tanh(m.out() * 1.8)
    return reverb(y, rng, rt60=1.1, mix=0.25, dark=0.6)


def s_haunt(rng):
    dur = 2.4
    t = T(dur)
    path = lambda tt: np.interp(tt, [0, 0.5, 1.2, 2.4], [470, 690, 600, 430])
    m = Mix(dur + 0.2)
    for detune, g in ((0.0, 0.5), (0.011, 0.4), (-0.009, 0.3)):
        f = path(t) * (1 + detune) * (1 + 0.012 * np.sin(TAU * 5.8 * t + rng.uniform(0, TAU)))
        tone = np.sin(TAU * np.cumsum(f) / SR) + 0.25 * np.sin(2 * TAU * np.cumsum(f) / SR)
        env = onset(t, 0.25) * np.clip((dur - t) / 0.9, 0, 1)
        m.add(formant(tone, 'oo') * env, 0.0, g)
    breath = band(rng.standard_normal(len(t)), 400, 3000)
    breath = formant(breath, 'oh') * onset(t, 0.4) * np.clip((dur - t) / 1.2, 0, 1)
    m.add(breath, 0.0, 0.35)
    m.add(sweep(rng, 1.4, (900, 3000), 0.6)[::-1] * np.linspace(0, 1, int(1.4 * SR)) ** 2, 0.0, 0.15)
    return reverb(m.out(), rng, rt60=2.6, mix=0.45, dark=0.8)


def s_click(rng):
    m = Mix(0.14)
    m.add(mallet(rng, 0.0025, 2500, 9000), 0.0, 0.6)
    m.add(modal([2230, 3520], [1.0, 0.45], [0.014, 0.008], 0.12, rng, attack=0.0003), 0.0, 0.7)
    m.add(modal([1120], [1.0], [0.01], 0.08, rng, attack=0.0003), 0.0, 0.3)
    return m.out()


# name -> (builder, peak dBFS, loudness ceiling dBFS)
SOUNDS = {
    'expedition/attune': (s_attune, -1.0, -15.0),
    'expedition/distortion': (s_distortion, -1.5, -15.0),
    'expedition/begin': (s_begin, -0.8, -13.0),
    'expedition/recollection': (s_recollection, -1.0, -15.0),
    'expedition/recorded': (s_recorded, -1.0, -15.0),
    'expedition/key_rise': (s_key_rise, -1.5, -16.0),
    'expedition/remember': (s_remember, -1.0, -15.0),
    'expedition/forget': (s_forget, -1.0, -14.0),
    'expedition/forgotten': (s_forgotten, -1.0, -15.0),
    'expedition/hourglass_tick': (s_hourglass_tick, -4.0, -18.0),
    'expedition/hourglass_end': (s_hourglass_end, -0.8, -14.0),
    'relic/reveal_common': (s_reveal_common, -3.0, -18.0),
    'relic/reveal_rare': (s_reveal_rare, -2.0, -17.0),
    'relic/reveal_epic': (s_reveal_epic, -1.5, -16.0),
    'relic/reveal_legendary': (s_reveal_legendary, -1.0, -14.0),
    'relic/reveal_remnant': (s_reveal_remnant, -0.8, -13.0),
    'codex/open': (s_codex_open, -2.0, -17.0),
    'codex/page': (s_codex_page, -4.0, -20.0),
    'codex/insight': (s_codex_insight, -1.5, -16.0),
    'codex/renown': (s_codex_renown, -1.5, -16.0),
    'party/invite': (s_party_invite, -2.0, -15.0),
    'party/join': (s_party_join, -2.0, -15.0),
    'distortion/bolster': (s_bolster, -1.0, -14.0),
    'distortion/volatile_hiss': (s_volatile_hiss, -2.0, -16.0),
    'distortion/volatile_burst': (s_volatile_burst, -0.5, -12.0),
    'distortion/haunt': (s_haunt, -1.0, -15.0),
    'ui/click': (s_click, -5.0, -20.0),
}


def encode(x: np.ndarray, path: Path):
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / 'x.wav'
        wavfile.write(wav, SR, (np.clip(x, -1, 1) * 32767).astype(np.int16))
        subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', str(wav), '-ac', '1', '-ar', str(SR), '-c:a', 'libvorbis',
                        '-q:a', '6', '-map_metadata', '-1', '-fflags', '+bitexact', '-flags:a', '+bitexact',
                        str(path)], check=True)


def spectrogram_sheet(rendered: dict, path: str):
    from PIL import Image, ImageDraw
    cols, w, h = 3, 420, 170
    rows = (len(rendered) + cols - 1) // cols
    sheet = Image.new('RGB', (cols * w, rows * h), (12, 12, 16))
    draw = ImageDraw.Draw(sheet)
    for i, (name, x) in enumerate(rendered.items()):
        f, tt, z = stft(x, SR, nperseg=1024, noverlap=896)
        mag = 20 * np.log10(np.abs(z) + 1e-7)
        mag = np.clip((mag - mag.max() + 80) / 80, 0, 1)
        # Log-frequency rows from 40 Hz to 16 kHz.
        rows_f = np.geomspace(40, 16000, 110)
        idx = np.clip(np.searchsorted(f, rows_f), 0, len(f) - 1)
        img = mag[idx][::-1]
        cols_t = np.linspace(0, img.shape[1] - 1, w - 10).astype(int)
        img = img[:, cols_t]
        rgb = np.stack([np.clip(img * 2.2 - 0.4, 0, 1), np.clip(img * 1.6 - 0.5, 0, 1) ** 1.2,
                        np.clip(0.35 + img * 0.9 - img ** 2 * 1.1, 0, 1)], -1)
        tile = Image.fromarray((rgb * 255).astype(np.uint8))
        x0, y0 = (i % cols) * w + 5, (i // cols) * h + 22
        sheet.paste(tile, (x0, y0))
        wave = np.abs(x)
        seg = np.array_split(wave, w - 10)
        for j, s in enumerate(seg):
            a = float(s.max()) if len(s) else 0
            draw.line([(x0 + j, y0 + 125), (x0 + j, y0 + 125 + int(a * 18))], fill=(120, 200, 190))
        draw.text((x0, y0 - 18), f'{name}  {len(x) / SR:.2f}s', fill=(230, 220, 200))
    sheet.save(path)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--only', nargs='*')
    parser.add_argument('--sheet')
    args = parser.parse_args()
    rendered = {}
    for name, (build, peak, loud) in SOUNDS.items():
        if args.only and name not in args.only:
            continue
        rng = np.random.default_rng(zlib.crc32(name.encode()))
        x = master(build(rng), peak, loud)
        encode(x, OUT / f'{name}.ogg')
        rendered[name] = x
        print(f'{name:32s} {len(x) / SR:5.2f}s  peak {20 * np.log10(np.max(np.abs(x))):6.1f} dB')
    if args.sheet:
        spectrogram_sheet(rendered, args.sheet)
        print(f'wrote {args.sheet}')


if __name__ == '__main__':
    main()
