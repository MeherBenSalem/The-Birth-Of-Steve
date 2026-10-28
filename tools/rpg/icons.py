#!/usr/bin/env python3
"""Paints textures/gui/rpg/icons.png, the 1.0 GUI icon sheet.

Each icon is authored as layered SVG in a 256-unit box, in the bronze-and-verdigris
style of textures/gui/greek/icons.png: ink outlines, bevelled bronze with patina
patches, marble, teal crystal and a little glow. Chromium renders every icon at
512 px; Pillow downsamples to 128 px cells (Lanczos plus a light unsharp mask) on
a transparent 1024 x 1024 sheet, 8 columns.

The cell order must match com.nightbeam.tbos.client.rpg.Icons. Run from the
repository root (needs node with Playwright and Pillow):

    python3 tools/rpg/icons.py [--preview out.png]
"""
from __future__ import annotations

import argparse
import math
import subprocess
import tempfile
from pathlib import Path

from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'shared/common/src/main/resources/assets/tbos/textures/gui/rpg/icons.png'
RENDER = 512
CELL = 128
COLUMNS = 8
SHEET = 1024
INK = '#0d0b14'

# Ramps run light to dark. The first entry is reserved for highlights.
RAMPS = {
    'bronze': ['#fff2c8', '#ffe29b', '#e8c07a', '#be8b4a', '#8a6033', '#5e4024', '#3a2716'],
    'gold': ['#fffbe6', '#fff0b0', '#f7d470', '#e0a93e', '#b07a24', '#6e4812'],
    'teal': ['#f2fffc', '#9cf2e6', '#5fd0c8', '#3fb0ac', '#1f6e74', '#103a42'],
    'marble': ['#fffdf6', '#f6eedb', '#e6d8b8', '#c9b48a', '#9a8462', '#5e4e38'],
    'parchment': ['#fffaea', '#f8ebc8', '#ecd6a0', '#d4b576', '#a8844c', '#6a5030'],
    'steel': ['#eef3fa', '#b8c2d0', '#8b96a8', '#555f70', '#3e4654', '#1e222a'],
    'iron': ['#a8b0c0', '#6a7484', '#4a5262', '#323946', '#1e222a', '#121419'],
    'purple': ['#f6e6ff', '#d9a8f0', '#b678d8', '#8a48b0', '#5a2a86', '#281f4c'],
    'red': ['#ffe4da', '#ff9e86', '#ee5a44', '#c0302a', '#7e1a16', '#3e0a0a'],
    'ember': ['#fffdea', '#fff0a0', '#ffc650', '#ff8a2a', '#e0501c', '#8a2410'],
    'wood': ['#f4d6a6', '#d6a66a', '#a8743e', '#7c5028', '#553418', '#33200e'],
    'ghost': ['#ffffff', '#efe6ff', '#c8b2f0', '#9a7ad8', '#6a4aa8', '#33205e'],
    'abyss': ['#b48ae0', '#7a4ab8', '#4e2a86', '#2e1858', '#1a0e36', '#0c0618'],
}


def _hex(c: str) -> tuple[int, int, int]:
    c = c.lstrip('#')
    return int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16)


def mix(a: str, b: str, t: float) -> str:
    x, y = _hex(a), _hex(b)
    return '#%02x%02x%02x' % tuple(round(x[i] + (y[i] - x[i]) * t) for i in range(3))


def ramp_from(base: str) -> list[str]:
    return [mix(base, '#ffffff', 0.85), mix(base, '#ffffff', 0.55), mix(base, '#ffffff', 0.25), base,
            mix(base, '#000000', 0.35), mix(base, '#000000', 0.62)]


RARITIES = ['#c9c4ba', '#7fd67a', '#62a9f2', '#b97af0', '#f2a43a', '#ea4b3f']
for _i, _c in enumerate(RARITIES):
    RAMPS[f'rarity{_i}'] = ramp_from(_c)


def ramp_at(name: str, t: float) -> str:
    r = RAMPS[name]
    t = min(max(t, 0.0), 1.0) * (len(r) - 1)
    i = int(t)
    if i >= len(r) - 1:
        return r[-1]
    return mix(r[i], r[i + 1], t - i)


def f(v: float) -> str:
    return ('%.2f' % v).rstrip('0').rstrip('.')


def poly(points) -> str:
    return 'M' + ' L'.join(f'{f(x)},{f(y)}' for x, y in points) + ' Z'


def circle(cx, cy, r) -> str:
    return (f'M{f(cx - r)},{f(cy)} A{f(r)},{f(r)} 0 1 0 {f(cx + r)},{f(cy)} '
            f'A{f(r)},{f(r)} 0 1 0 {f(cx - r)},{f(cy)} Z')


def ellipse(cx, cy, rx, ry) -> str:
    return (f'M{f(cx - rx)},{f(cy)} A{f(rx)},{f(ry)} 0 1 0 {f(cx + rx)},{f(cy)} '
            f'A{f(rx)},{f(ry)} 0 1 0 {f(cx - rx)},{f(cy)} Z')


def ring(cx, cy, r_out, r_in) -> str:
    """An annulus; draw with fill-rule evenodd."""
    return circle(cx, cy, r_out) + ' ' + circle(cx, cy, r_in)


def star_points(cx, cy, r1, r2, n, rot=-90.0):
    pts = []
    for i in range(n * 2):
        a = math.radians(rot + i * 180.0 / n)
        r = r1 if i % 2 == 0 else r2
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def regular(cx, cy, r, n, rot=-90.0, sx=1.0, sy=1.0):
    return [(cx + sx * r * math.cos(math.radians(rot + i * 360.0 / n)),
             cy + sy * r * math.sin(math.radians(rot + i * 360.0 / n))) for i in range(n)]


def arc_path(cx, cy, r, a0, a1) -> str:
    """An open arc from angle a0 to a1 (degrees, clockwise on screen)."""
    x0, y0 = cx + r * math.cos(math.radians(a0)), cy + r * math.sin(math.radians(a0))
    x1, y1 = cx + r * math.cos(math.radians(a1)), cy + r * math.sin(math.radians(a1))
    large = 1 if (a1 - a0) % 360 > 180 else 0
    return f'M{f(x0)},{f(y0)} A{f(r)},{f(r)} 0 {large} 1 {f(x1)},{f(y1)}'


class Svg:
    """One icon: body markup plus per-icon defs (clip paths, masks)."""

    def __init__(self, index: int):
        self.index = index
        self.defs: list[str] = []
        self.body: list[str] = []
        self.n = 0

    def uid(self, prefix: str) -> str:
        self.n += 1
        return f'i{self.index}{prefix}{self.n}'

    def add(self, markup: str):
        self.body.append(markup)

    @property
    def patina(self) -> str:
        return f'patina{self.index % 4}'

    # ---- Primitives -----------------------------------------------------------------------------------

    def shape(self, d: str, mat: str = 'bronze', ow: float = 7, bevel: float = 4, patina: bool = False,
              grad: str = 'g', opacity: float = 1.0, evenodd: bool = False, fill: str | None = None,
              transform: str = '', glow: str | None = None, hl: str | None = None, sh: str | None = None):
        rule = ' fill-rule="evenodd" clip-rule="evenodd"' if evenodd else ''
        t = f' transform="{transform}"' if transform else ''
        g_open = f'<g{t} opacity="{f(opacity)}">' if (t or opacity < 1) else '<g>'
        out = [g_open]
        if glow:
            out.append(f'<path d="{d}"{rule} fill="{glow}" filter="url(#blur10)" opacity="0.85"/>')
        if ow:
            out.append(f'<path d="{d}"{rule} fill="none" stroke="{INK}" stroke-width="{f(ow * 2)}" '
                       f'stroke-linejoin="round" stroke-linecap="round"/>')
        paint = fill or f'url(#{grad}-{mat})'
        filt = f' filter="url(#{self.patina})"' if patina else ''
        out.append(f'<path d="{d}"{rule} fill="{paint}"{filt}/>')
        if bevel and mat in RAMPS:
            cid = self.uid('c')
            self.defs.append(f'<clipPath id="{cid}"><path d="{d}"{rule}/></clipPath>')
            light = hl or RAMPS[mat][0]
            dark = sh or RAMPS[mat][-1]
            out.append(f'<g clip-path="url(#{cid})">'
                       f'<path d="{d}" fill="none" stroke="{dark}" stroke-width="{f(bevel * 2.2)}" opacity="0.75" '
                       f'stroke-linejoin="round" transform="translate({f(-bevel)} {f(-bevel)})"/>'
                       f'<path d="{d}" fill="none" stroke="{light}" stroke-width="{f(bevel * 2)}" opacity="0.85" '
                       f'stroke-linejoin="round" transform="translate({f(bevel * 0.9)} {f(bevel * 0.9)})"/></g>')
        out.append('</g>')
        self.add(''.join(out))

    def stroke(self, d: str, color: str, width: float, ow: float = 0, cap: str = 'round', opacity: float = 1.0,
               glow: bool = False, dash: str = '', transform: str = ''):
        t = f' transform="{transform}"' if transform else ''
        da = f' stroke-dasharray="{dash}"' if dash else ''
        out = [f'<g{t} opacity="{f(opacity)}">']
        if ow:
            out.append(f'<path d="{d}" fill="none" stroke="{INK}" stroke-width="{f(width + ow * 2)}" '
                       f'stroke-linecap="{cap}" stroke-linejoin="round"{da}/>')
        filt = ' filter="url(#glow)"' if glow else ''
        out.append(f'<path d="{d}" fill="none" stroke="{color}" stroke-width="{f(width)}" '
                   f'stroke-linecap="{cap}" stroke-linejoin="round"{da}{filt}/>')
        out.append('</g>')
        self.add(''.join(out))

    def glow(self, d: str, color: str, opacity: float = 0.6, blur: str = 'blur16'):
        self.add(f'<path d="{d}" fill="{color}" opacity="{f(opacity)}" filter="url(#{blur})"/>')

    def aura(self, color: str, opacity: float = 0.35, cx=128.0, cy=128.0, r=70.0):
        """The soft backdrop glow; sized so it fades out before the cell edge."""
        self.add(f'<circle cx="{f(cx)}" cy="{f(cy)}" r="{f(r)}" fill="{color}" opacity="{f(opacity)}" '
                 f'filter="url(#blur16)"/>')

    def begin(self, transform: str):
        self.add(f'<g transform="{transform}">')

    def end(self):
        self.add('</g>')

    def sparkle(self, x, y, r, color='#ffffff', opacity=1.0):
        d = poly(star_points(x, y, r, r * 0.22, 4))
        self.add(f'<path d="{d}" fill="{color}" opacity="{f(opacity)}" filter="url(#glow)"/>')

    def highlight(self, d: str, opacity=0.55, width=5):
        self.add(f'<path d="{d}" fill="none" stroke="#ffffff" stroke-width="{f(width)}" stroke-linecap="round" '
                 f'opacity="{f(opacity)}"/>')

    def gem(self, outer, mat: str, table: float = 0.52, ow: float = 6, lift=(0.0, -0.06), glow: str | None = None,
            facet_lines: bool = True, transform: str = ''):
        """A faceted stone: outer polygon, a smaller table, and quads between them shaded by facing."""
        cx = sum(p[0] for p in outer) / len(outer)
        cy = sum(p[1] for p in outer) / len(outer)
        span = max(max(abs(p[0] - cx), abs(p[1] - cy)) for p in outer)
        tx, ty = cx + lift[0] * span, cy + lift[1] * span
        inner = [(tx + (x - cx) * table, ty + (y - cy) * table) for x, y in outer]
        t = f' transform="{transform}"' if transform else ''
        out = [f'<g{t}>']
        if glow:
            out.append(f'<path d="{poly(outer)}" fill="{glow}" opacity="0.75" filter="url(#blur16)"/>')
        out.append(f'<path d="{poly(outer)}" fill="none" stroke="{INK}" stroke-width="{f(ow * 2)}" '
                   f'stroke-linejoin="round"/>')
        light = (-0.55, -0.83)
        n = len(outer)
        for i in range(n):
            a, b = outer[i], outer[(i + 1) % n]
            mx, my = (a[0] + b[0]) / 2 - cx, (a[1] + b[1]) / 2 - cy
            ln = math.hypot(mx, my) or 1
            facing = (mx * light[0] + my * light[1]) / ln
            col = ramp_at(mat, 0.52 - 0.42 * facing)
            quad = [a, b, inner[(i + 1) % n], inner[i]]
            out.append(f'<path d="{poly(quad)}" fill="{col}"/>')
        out.append(f'<path d="{poly(inner)}" fill="url(#r-{mat})"/>')
        if facet_lines:
            for i in range(n):
                a, b = outer[i], inner[i]
                out.append(f'<path d="M{f(a[0])},{f(a[1])} L{f(b[0])},{f(b[1])}" stroke="{RAMPS[mat][0]}" '
                           f'stroke-width="1.6" opacity="0.45"/>')
            out.append(f'<path d="{poly(inner)}" fill="none" stroke="{RAMPS[mat][0]}" stroke-width="1.8" '
                       f'opacity="0.5"/>')
        # Specular glint on the table's upper left.
        gx = tx - (tx - min(p[0] for p in inner)) * 0.45
        gy = ty - (ty - min(p[1] for p in inner)) * 0.45
        out.append(f'<path d="{poly(star_points(gx, gy, span * 0.2, span * 0.045, 4))}" fill="#ffffff" '
                   f'opacity="0.9"/>')
        out.append('</g>')
        self.add(''.join(out))

    def meander_ring(self, cx, cy, r, count, size, color, width=2.6, opacity=0.85):
        unit = ('M{a},{b} L{c},{b} L{c},{d} L{e},{d} L{e},{g} L{h},{g}')
        s = size / 2
        d = unit.format(a=f(-s), b=f(-s), c=f(s), d=f(s), e=f(-s * 0.3), g=f(-s * 0.1), h=f(s * 0.35))
        parts = []
        for i in range(count):
            ang = i * 360.0 / count
            x = cx + r * math.cos(math.radians(ang))
            y = cy + r * math.sin(math.radians(ang))
            parts.append(f'<path d="{d}" transform="translate({f(x)} {f(y)}) rotate({f(ang + 90)})"/>')
        self.add(f'<g fill="none" stroke="{color}" stroke-width="{f(width)}" stroke-linecap="square" '
                 f'opacity="{f(opacity)}">' + ''.join(parts) + '</g>')

    def svg(self) -> str:
        return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{RENDER}" height="{RENDER}" '
                f'viewBox="0 0 256 256"><defs>{"".join(self.defs)}</defs>{"".join(self.body)}</svg>')


def shared_defs() -> str:
    out = []
    for name, r in RAMPS.items():
        stops = r[1:]
        lin = ''.join(f'<stop offset="{f(i / (len(stops) - 1))}" stop-color="{c}"/>' for i, c in enumerate(stops))
        out.append(f'<linearGradient id="g-{name}" x1="0.15" y1="0" x2="0.85" y2="1">{lin}</linearGradient>')
        rad = ''.join(f'<stop offset="{f(i / (len(r) - 1))}" stop-color="{c}"/>' for i, c in enumerate(r))
        out.append(f'<radialGradient id="r-{name}" cx="0.4" cy="0.34" r="0.78" fx="0.34" fy="0.28">{rad}'
                   f'</radialGradient>')
        # Vertical, for liquids, flames and sand.
        out.append(f'<linearGradient id="v-{name}" x1="0" y1="0" x2="0" y2="1">{lin}</linearGradient>')
    for seed in range(4):
        out.append(
            f'<filter id="patina{seed}" x="-10" y="-10" width="276" height="276" filterUnits="userSpaceOnUse">'
            f'<feTurbulence type="fractalNoise" baseFrequency="0.038" numOctaves="3" seed="{seed * 7 + 3}" result="n"/>'
            '<feColorMatrix in="n" type="matrix" values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  7.5 0 0 0 -3.65" result="m"/>'
            '<feFlood flood-color="#3fb0ac" flood-opacity="0.82" result="t"/>'
            '<feComposite in="t" in2="m" operator="in" result="tm"/>'
            '<feComposite in="tm" in2="SourceAlpha" operator="in" result="tp"/>'
            f'<feTurbulence type="fractalNoise" baseFrequency="0.55" numOctaves="2" seed="{seed + 11}" result="g"/>'
            '<feColorMatrix in="g" type="matrix" values="0 0 0 0 0.05  0 0 0 0 0.04  0 0 0 0 0.03  0 0 0 1.6 -0.62" result="gm"/>'
            '<feComposite in="gm" in2="SourceAlpha" operator="in" result="grit"/>'
            '<feMerge><feMergeNode in="SourceGraphic"/><feMergeNode in="tp"/><feMergeNode in="grit"/></feMerge>'
            '</filter>')
    out.append('<filter id="glow" x="-60%" y="-60%" width="220%" height="220%">'
               '<feGaussianBlur in="SourceGraphic" stdDeviation="5" result="b"/>'
               '<feMerge><feMergeNode in="b"/><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>')
    for b in (6, 10, 16, 24):
        out.append(f'<filter id="blur{b}" x="-80%" y="-80%" width="260%" height="260%">'
                   f'<feGaussianBlur stdDeviation="{b}"/></filter>')
    out.append('<filter id="dissolve" x="-10" y="-10" width="276" height="276" filterUnits="userSpaceOnUse">'
               '<feTurbulence type="fractalNoise" baseFrequency="0.07" numOctaves="2" seed="5" result="n"/>'
               '<feColorMatrix in="n" type="matrix" values="0 0 0 0 0  0 0 0 0 0  0 0 0 0 0  9 0 0 0 -3.6" result="m"/>'
               '<feComposite in="SourceGraphic" in2="m" operator="in"/></filter>')
    return f'<svg width="0" height="0" style="position:absolute"><defs>{"".join(out)}</defs></svg>'


# ---- Reusable objects ----------------------------------------------------------------------------------------


def hourglass(s: Svg, sand: str, cx=128.0, cy=128.0, k=1.0, cracked=False):
    def P(x, y):
        return cx + (x - 128) * k, cy + (y - 128) * k

    def pts(*xy):
        return [P(x, y) for x, y in xy]

    glass = ('M{} C{} {} {} C{} {} {} L{} C{} {} {} C{} {} {} Z').format(
        '%s,%s' % tuple(map(f, P(88, 50))),
        '%s,%s' % tuple(map(f, P(88, 100))), '%s,%s' % tuple(map(f, P(120, 110))), '%s,%s' % tuple(map(f, P(120, 128))),
        '%s,%s' % tuple(map(f, P(120, 146))), '%s,%s' % tuple(map(f, P(88, 156))), '%s,%s' % tuple(map(f, P(88, 206))),
        '%s,%s' % tuple(map(f, P(168, 206))),
        '%s,%s' % tuple(map(f, P(168, 156))), '%s,%s' % tuple(map(f, P(136, 146))), '%s,%s' % tuple(map(f, P(136, 128))),
        '%s,%s' % tuple(map(f, P(136, 110))), '%s,%s' % tuple(map(f, P(168, 100))), '%s,%s' % tuple(map(f, P(168, 50))))
    s.shape(glass, fill='#d8f4f2', mat='teal', ow=5 * k, bevel=0, opacity=1)
    s.add(f'<path d="{glass}" fill="url(#r-teal)" opacity="0.28"/>')
    top_sand = poly(pts((98, 86), (158, 86), (150, 104), (131, 122), (125, 122), (106, 104)))
    s.shape(top_sand, mat=sand, grad='v', ow=0, bevel=0)
    bottom = poly(pts((94, 204), (100, 182), (114, 172), (128, 166), (142, 172), (156, 182), (162, 204)))
    s.shape(bottom, mat=sand, grad='v', ow=0, bevel=0)
    s.stroke(f'M{f(P(128, 122)[0])},{f(P(128, 122)[1])} L{f(P(128, 168)[0])},{f(P(128, 168)[1])}',
             ramp_at(sand, 0.3), 3.5 * k)
    s.highlight(f'M{f(P(98, 64)[0])},{f(P(98, 64)[1])} C{f(P(100, 90)[0])},{f(P(100, 90)[1])} '
                f'{f(P(108, 102)[0])},{f(P(108, 102)[1])} {f(P(114, 110)[0])},{f(P(114, 110)[1])}', 0.75, 5 * k)
    s.highlight(f'M{f(P(98, 196)[0])},{f(P(98, 196)[1])} C{f(P(100, 178)[0])},{f(P(100, 178)[1])} '
                f'{f(P(106, 166)[0])},{f(P(106, 166)[1])} {f(P(112, 160)[0])},{f(P(112, 160)[1])}', 0.5, 4 * k)
    if cracked:
        crack = pts((150, 60), (140, 74), (150, 84), (138, 98))
        s.stroke('M' + ' L'.join(f'{f(x)},{f(y)}' for x, y in crack), '#ffffff', 2.6 * k, opacity=0.95)
    # Posts, then plates over them.
    for x in (64, 180):
        s.shape(poly(pts((x, 46), (x + 12, 46), (x + 12, 210), (x, 210))), 'bronze', ow=5 * k, bevel=2.5 * k)
    for y in (28, 204):
        d = poly(pts((50, y), (206, y), (200, y + 24), (56, y + 24)))
        s.shape(d, 'bronze', ow=6 * k, bevel=4 * k, patina=True)
        s.add(f'<path d="M{f(P(64, y + 12)[0])},{f(P(64, y + 12)[1])} L{f(P(192, y + 12)[0])},'
              f'{f(P(192, y + 12)[1])}" stroke="{RAMPS["bronze"][-2]}" stroke-width="{f(3 * k)}" '
              f'stroke-dasharray="{f(9 * k)} {f(5 * k)}" opacity="0.7"/>')


def card(s: Svg, cx, cy, angle, glyph: str = 'diamond', opacity=1.0, face='parchment', dissolve=False, k=1.0,
         cracked=False):
    g_attr = f'transform="translate({f(cx)} {f(cy)}) rotate({f(angle)}) scale({f(k)})"'
    inner = Svg(s.index)
    inner.n = s.n + 100
    w, h = 36, 52
    frame = (f'M{-w + 8},{-h} L{w - 8},{-h} Q{w},{-h} {w},{-h + 8} L{w},{h - 8} Q{w},{h} {w - 8},{h} '
             f'L{-w + 8},{h} Q{-w},{h} {-w},{h - 8} L{-w},{-h + 8} Q{-w},{-h} {-w + 8},{-h} Z')
    inner.shape(frame, 'bronze', ow=6, bevel=3.5, patina=True)
    w2, h2 = w - 9, h - 9
    face_d = f'M{-w2},{-h2} L{w2},{-h2} L{w2},{h2} L{-w2},{h2} Z'
    inner.shape(face_d, face, ow=2.5, bevel=3)
    if glyph == 'diamond':
        inner.gem(regular(0, 0, 16, 4), 'teal', ow=3.5, glow='#5fd0c8')
    elif glyph == 'eye':
        inner.gem(regular(0, 0, 15, 6), 'purple', ow=3.5, glow='#b678d8')
    else:
        inner.gem(regular(0, 0, 15, 8), 'teal', ow=3.5, glow='#5fd0c8')
    for y in (-30, 30):
        inner.add(f'<path d="M-14,{y} L14,{y}" stroke="{RAMPS["parchment"][-2]}" stroke-width="3" '
                  f'stroke-linecap="round" opacity="0.8"/>')
    if cracked:
        inner.add(f'<path d="M-30,-40 L-8,-14 L-20,6 L6,26 L-2,48" stroke="{INK}" stroke-width="4" fill="none" '
                  f'stroke-linejoin="round"/>')
    s.defs.extend(inner.defs)
    s.n = inner.n
    filt = ' filter="url(#dissolve)"' if dissolve else ''
    s.add(f'<g {g_attr} opacity="{f(opacity)}"{filt}>' + ''.join(inner.body) + '</g>')


def key(s: Svg, x, y, angle, scale=1.0, gem_mat='teal'):
    inner = Svg(s.index)
    inner.n = s.n + 200
    # Local frame: bow on the left, bit on the right, shaft along +x.
    inner.shape(poly([(-36, -9), (92, -9), (92, 9), (-36, 9)]), 'bronze', ow=6, bevel=3, patina=True)
    bit = poly([(58, 6), (96, 6), (96, 46), (84, 46), (84, 32), (74, 32), (74, 46), (58, 46)])
    inner.shape(bit, 'bronze', ow=6, bevel=3.5, patina=True)
    for cx in (-18, 22):
        inner.shape(poly([(cx - 6, -15), (cx + 6, -15), (cx + 6, 15), (cx - 6, 15)]), 'gold', ow=5, bevel=2.5)
    inner.shape(ring(-66, 0, 46, 28), 'bronze', ow=6, bevel=4, patina=True, evenodd=True)
    for a in range(0, 360, 45):
        px = -66 + 37 * math.cos(math.radians(a))
        py = 37 * math.sin(math.radians(a))
        inner.add(f'<circle cx="{f(px)}" cy="{f(py)}" r="3.2" fill="{RAMPS["bronze"][1]}" stroke="{INK}" '
                  f'stroke-width="1.6"/>')
    inner.gem(regular(-66, 0, 27, 6, rot=0), gem_mat, ow=4, glow=RAMPS[gem_mat][2])
    s.defs.extend(inner.defs)
    s.n = inner.n
    s.add(f'<g transform="translate({f(x)} {f(y)}) rotate({f(angle)}) scale({f(scale)})">' + ''.join(inner.body)
          + '</g>')


def sword(s: Svg, x, y, angle, scale=1.0, mat='steel', opacity=1.0, ghost=False):
    inner = Svg(s.index)
    inner.n = s.n + 300
    blade = 'M0,-118 C15,-86 21,-44 14,-4 L10,30 L-10,30 L-14,-4 C-21,-44 -15,-86 0,-118 Z'
    if ghost:
        inner.shape(blade, 'teal', ow=0, bevel=0, glow='#5fd0c8', fill='#9cf2e6')
    else:
        inner.shape(blade, mat, ow=6, bevel=4)
        inner.add('<path d="M0,-96 L0,22" stroke="#555f70" stroke-width="3.5" opacity="0.7"/>')
        inner.highlight('M-6,-84 C-10,-56 -11,-30 -8,-6', 0.7, 3.5)
    if not ghost:
        inner.shape(poly([(-38, 28), (38, 28), (32, 44), (-32, 44)]), 'bronze', ow=6, bevel=3, patina=True)
        inner.shape(poly([(-8, 44), (8, 44), (8, 90), (-8, 90)]), 'wood', ow=5, bevel=2.5)
        for yy in (54, 66, 78):
            inner.add(f'<path d="M-8,{yy} L8,{yy + 6}" stroke="{RAMPS["wood"][-1]}" stroke-width="3"/>')
        inner.shape(circle(0, 100, 13), 'bronze', ow=5, bevel=3, grad='r')
    s.defs.extend(inner.defs)
    s.n = inner.n
    s.add(f'<g transform="translate({f(x)} {f(y)}) rotate({f(angle)}) scale({f(scale)})" opacity="{f(opacity)}">'
          + ''.join(inner.body) + '</g>')


def arrow_arc(s: Svg, cx, cy, r, a0, a1, color='#5fd0c8', width=16):
    s.stroke(arc_path(cx, cy, r, a0, a1), color, width, ow=5, cap='butt', glow=False)
    # Arrowhead at a1, pointing along the clockwise tangent.
    ax, ay = cx + r * math.cos(math.radians(a1)), cy + r * math.sin(math.radians(a1))
    tx, ty = -math.sin(math.radians(a1)), math.cos(math.radians(a1))
    nx, ny = math.cos(math.radians(a1)), math.sin(math.radians(a1))
    head = [(ax + nx * 22, ay + ny * 22), (ax + tx * 30, ay + ty * 30), (ax - nx * 22, ay - ny * 22)]
    s.shape(poly(head), 'teal', ow=5, bevel=2.5, fill=color)


def laurel_branch(s: Svg, side: int, cx=128, cy=128, r=92, a_from=100, a_to=230, leaves=8, mat='gold', size=1.0):
    """Leaves along an arc; side=+1 mirrors to the right."""
    stem = []
    for i in range(25):
        a = math.radians(a_from + (a_to - a_from) * i / 24)
        stem.append((cx - side * r * math.cos(a), cy - r * math.sin(a)))
    s.stroke('M' + ' L'.join(f'{f(x)},{f(y)}' for x, y in stem), f'url(#g-{mat})', 5 * size, ow=2.5)
    for i in range(leaves):
        t = (i + 0.5) / leaves
        a = math.radians(a_from + (a_to - a_from) * t)
        px = cx - side * r * math.cos(a)
        py = cy - r * math.sin(a)
        # Direction of travel along the branch, towards its tip.
        sign = 1 if a_to >= a_from else -1
        dx = side * r * math.sin(a) * sign
        dy = -r * math.cos(a) * sign
        ang = math.degrees(math.atan2(dy, dx))
        for flip in (-1, 1):
            leaf = 'M0,0 C6,-8 20,-12 34,-6 C22,2 8,4 0,0 Z'
            rot = ang + flip * 34
            lscale = size * (1.0 - 0.35 * t)
            s.shape(leaf, mat, ow=3.2, bevel=2, transform=f'translate({f(px)} {f(py)}) rotate({f(rot)}) '
                                                             f'scale({f(lscale * 1.25)})')


# ---- Icons, in Icons.java order ----------------------------------------------------------------------------


def i_fortified(s: Svg):
    s.aura('#ee5a44', 0.35, 128, 128, 74)
    outer = ('M128,22 C160,34 190,38 214,38 L214,118 C214,176 178,212 128,236 C78,212 42,176 42,118 L42,38 '
             'C66,38 96,34 128,22 Z')
    s.shape(outer, 'bronze', ow=7, bevel=5, patina=True)
    face = ('M128,46 C152,55 176,59 194,59 L194,120 C194,164 166,194 128,214 C90,194 62,164 62,120 L62,59 '
            'C80,59 104,55 128,46 Z')
    s.shape(face, 'red', ow=4, bevel=5, hl='#ff9e86', sh='#3e0a0a')
    s.shape(poly([(118, 50), (138, 50), (138, 210), (118, 210)]), 'bronze', ow=4, bevel=3, patina=True)
    s.shape(poly([(64, 98), (192, 98), (192, 118), (64, 118)]), 'bronze', ow=4, bevel=3, patina=True)
    s.shape(circle(128, 108, 27), 'bronze', ow=5, bevel=4, grad='r', patina=True)
    s.gem(regular(128, 108, 15, 8, rot=22.5), 'red', ow=3.5, glow='#ee5a44')
    for x, y in ((80, 74), (176, 74), (84, 160), (172, 160)):
        s.add(f'<circle cx="{x}" cy="{y}" r="6" fill="url(#r-bronze)" stroke="{INK}" stroke-width="2.5"/>')
    s.highlight('M58,48 C80,46 104,40 126,30', 0.6, 4)


def i_tyrannical(s: Svg):
    s.aura('#ee5a44', 0.35, 128, 132, 74)
    body = ('M50,176 L34,80 L80,126 L96,66 L114,122 L128,36 L142,122 L160,66 L176,126 L222,80 L206,176 Z')
    s.shape(body, 'gold', ow=7, bevel=5, patina=True)
    band = 'M44,166 L212,166 L208,210 C160,220 96,220 48,210 Z'
    s.shape(band, 'bronze', ow=6, bevel=4, patina=True)
    for x, r in ((80, 11), (128, 15), (176, 11)):
        s.gem(regular(x, 188, r, 6, rot=0), 'red', ow=3.5, glow='#ee5a44')
    for x, y, r in ((34, 80, 10), (96, 66, 9), (128, 36, 12), (160, 66, 9), (222, 80, 10)):
        s.shape(circle(x, y, r), 'gold', ow=4, bevel=2.5, grad='r')
    s.highlight('M60,160 L48,96', 0.55, 4)
    s.highlight('M128,56 L120,104', 0.5, 4)


def chevron(cx, y, w, h, t):
    return poly([(cx - w, y + h), (cx, y), (cx + w, y + h), (cx + w, y + h + t), (cx, y + t), (cx - w, y + h + t)])


def i_bolstering(s: Svg):
    s.aura('#ff8a2a', 0.35, 128, 120, 74)
    s.shape(chevron(128, 146, 84, 52, 32), 'bronze', ow=7, bevel=4, patina=True)
    s.shape(chevron(128, 90, 84, 52, 32), 'gold', ow=7, bevel=4, patina=True)
    s.shape(chevron(128, 30, 84, 52, 32), 'ember', ow=7, bevel=4, glow='#ff8a2a')
    s.highlight('M58,78 L128,36', 0.75, 4)
    for x, y, r in ((40, 40, 6), (214, 48, 7), (196, 22, 4), (60, 18, 4)):
        s.sparkle(x, y, r * 1.6, '#ffc650')


def i_volatile(s: Svg):
    s.aura('#ff8a2a', 0.5, 128, 136, 74)
    burst = poly(star_points(128, 136, 120, 58, 12, rot=-75))
    s.shape(burst, 'ember', ow=0, bevel=0, opacity=0.9, fill='url(#r-ember)')
    s.shape(circle(128, 136, 70), 'iron', ow=7, bevel=5, grad='r')
    cracks = ['M128,136 L104,112 L96,86 L78,74', 'M128,136 L154,118 L166,92', 'M128,136 L150,160 L178,168',
              'M128,136 L112,164 L118,196', 'M128,136 L94,142 L66,134']
    for c in cracks:
        s.stroke(c, '#fff0a0', 6, ow=0, glow=True)
    s.add('<circle cx="128" cy="136" r="16" fill="#fffdea" filter="url(#glow)"/>')
    s.highlight('M84,110 C92,90 108,80 126,78', 0.45, 6)
    for x, y, r in ((36, 54, 7), (214, 70, 6), (220, 200, 7), (40, 206, 5), (128, 18, 6)):
        s.sparkle(x, y, r * 1.8, '#ffc650')


def i_haunted(s: Svg):
    s.aura('#9a7ad8', 0.45, 128, 124, 74)
    body = ('M128,24 C178,24 206,64 206,114 L206,206 L186,190 L168,220 L150,194 L128,228 L106,194 L88,220 '
            'L70,190 L50,206 L50,114 C50,64 78,24 128,24 Z')
    s.shape(body, 'ghost', ow=6, bevel=5, opacity=0.95, grad='r', glow='#9a7ad8')
    s.shape(ellipse(128, 104, 46, 50), 'abyss', ow=4, bevel=0, fill='#14102a')
    for x in (108, 148):
        s.add(f'<ellipse cx="{x}" cy="104" rx="10" ry="13" fill="#9cf2e6" filter="url(#glow)"/>')
        s.add(f'<ellipse cx="{x - 2}" cy="100" rx="3.5" ry="4.5" fill="#ffffff"/>')
    s.add('<ellipse cx="128" cy="134" rx="9" ry="12" fill="#5fd0c8" opacity="0.55" filter="url(#glow)"/>')
    for d in ('M36,170 C20,150 26,120 44,110', 'M220,170 C236,150 230,120 212,110', 'M30,224 C46,214 60,216 70,224'):
        s.stroke(d, '#c8b2f0', 5, opacity=0.7)
    s.highlight('M74,72 C86,48 106,36 126,34', 0.7, 5)


HEART = ('M128,222 C66,176 26,140 26,94 C26,58 54,34 86,34 C106,34 120,46 128,62 C136,46 150,34 170,34 '
         'C202,34 230,58 230,94 C230,140 190,176 128,222 Z')


def i_frailty(s: Svg):
    s.aura('#ee5a44', 0.35, 128, 128, 74)
    s.shape(HEART, 'red', ow=7, bevel=6, grad='r')
    crack = poly([(128, 62), (114, 94), (136, 120), (112, 148), (132, 176), (124, 222), (134, 210), (142, 176),
                  (122, 148), (148, 120), (126, 94), (136, 62)])
    s.add(f'<path d="{crack}" fill="{INK}"/>')
    s.highlight('M52,76 C60,58 76,52 92,54', 0.8, 7)
    for x, y, r, rot in ((196, 206, 9, 20), (214, 184, 6, -10), (176, 230, 5, 40)):
        s.shape(poly(regular(x, y, r, 3, rot=rot)), 'red', ow=3, bevel=1.5)


def i_hourglass_distortion(s: Svg):
    s.aura('#ee5a44', 0.32, 128, 128, 74)
    hourglass(s, 'red', cracked=True)
    for x, y, r in ((30, 60, 5), (226, 92, 6), (220, 184, 5)):
        s.sparkle(x, y, r * 1.8, '#ff9e86')


def i_amnesia(s: Svg):
    s.aura('#8a48b0', 0.35, 128, 128, 74)
    card(s, 180, 138, 18, 'eye', opacity=0.5, face='marble', k=1.12, cracked=True)
    card(s, 76, 138, -18, 'diamond', k=1.12)
    card(s, 128, 124, 0, 'round', k=1.18)
    for x, y, r in ((222, 62, 7), (232, 96, 5), (204, 40, 5), (236, 128, 4)):
        s.add(f'<rect x="{x - r}" y="{y - r}" width="{r * 2}" height="{r * 2}" fill="#b678d8" stroke="{INK}" '
              f'stroke-width="2" transform="rotate(30 {x} {y})"/>')


def i_steady_hand(s: Svg):
    s.aura('#3fb0ac', 0.3, 128, 128, 74)
    arrow_arc(s, 128, 128, 102, 150, 400)
    loop = ellipse(128, 76, 30, 36) + ' ' + ellipse(128, 76, 14, 20)
    s.shape(loop, 'bronze', ow=7, bevel=4, patina=True, evenodd=True)
    s.shape('M78,110 L178,110 L184,134 L72,134 Z', 'bronze', ow=7, bevel=4, patina=True)
    s.shape('M116,130 L140,130 L154,214 L102,214 Z', 'bronze', ow=7, bevel=4, patina=True)
    s.gem(regular(128, 122, 14, 4), 'teal', ow=3.5, glow='#5fd0c8')
    s.highlight('M104,64 C106,52 114,44 124,42', 0.6, 4)


def spiral_path(cx, cy, r0, r1, turns, a0=0.0):
    pts = []
    steps = 120
    for i in range(steps + 1):
        t = i / steps
        a = math.radians(a0 + 360 * turns * t)
        r = r0 + (r1 - r0) * t
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return 'M' + ' L'.join(f'{f(x)},{f(y)}' for x, y in pts)


def i_deep_recall(s: Svg):
    s.shape(ring(128, 128, 108, 82), 'bronze', ow=7, bevel=5, patina=True, evenodd=True)
    s.meander_ring(128, 128, 95, 14, 11, RAMPS['bronze'][-1])
    s.shape(circle(128, 128, 82), 'teal', ow=4, bevel=0, fill='url(#r-abyss)')
    s.add('<circle cx="128" cy="128" r="80" fill="url(#r-teal)" opacity="0.35"/>')
    s.stroke(spiral_path(128, 128, 6, 74, 2.4, 30), '#9cf2e6', 7, glow=True)
    s.add('<circle cx="128" cy="128" r="12" fill="#f2fffc" filter="url(#glow)"/>')
    s.sparkle(128, 128, 26, '#fff0b0')
    for x, y in ((96, 92), (166, 104), (110, 170)):
        s.sparkle(x, y, 7, '#ffffff', 0.85)


def i_keen_eye(s: Svg):
    s.aura('#3fb0ac', 0.3, 128, 128, 74)
    outer = 'M18,128 C62,54 194,54 238,128 C194,202 62,202 18,128 Z'
    inner = 'M44,128 C80,78 176,78 212,128 C176,178 80,178 44,128 Z'
    s.shape(outer + ' ' + inner, 'bronze', ow=7, bevel=4, patina=True, evenodd=True)
    s.shape(inner, 'marble', ow=0, bevel=4, grad='r')
    s.shape(circle(128, 128, 42), 'teal', ow=5, bevel=0, grad='r', glow='#5fd0c8')
    s.add(f'<path d="{poly(star_points(128, 128, 26, 9, 4))}" fill="{INK}"/>')
    s.add('<circle cx="114" cy="114" r="8" fill="#ffffff" opacity="0.9"/>')
    for x, y, rot in ((128, 40, 0), (78, 52, -28), (178, 52, 28)):
        s.shape(poly([(x - 8, y + 12), (x, y - 14), (x + 8, y + 12)]), 'bronze', ow=4, bevel=2,
                transform=f'rotate({rot} {x} {y})')


def i_archivists_grace(s: Svg):
    s.aura('#5fd0c8', 0.35, 128, 140, 74)
    s.stroke(ellipse(128, 40, 56, 15), '#f7d470', 9, ow=4, glow=True)
    pts = []
    for i in range(28):
        t = i / 28 * 2 * math.pi
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        pts.append((128 + x * 5.6, 144 + y * 5.6))
    s.gem(pts, 'teal', table=0.48, ow=7, glow='#5fd0c8', facet_lines=False)
    laurel_branch(s, -1, 128, 150, 92, 150, 215, leaves=4, mat='gold', size=0.85)
    laurel_branch(s, 1, 128, 150, 92, 150, 215, leaves=4, mat='gold', size=0.85)


def i_bulwark_memory(s: Svg):
    s.shape(circle(128, 128, 110), 'bronze', ow=7, bevel=5, patina=True, grad='r')
    s.meander_ring(128, 128, 99, 16, 11, RAMPS['bronze'][-1])
    s.shape(circle(128, 128, 87), 'teal', ow=4, bevel=5, grad='r', hl='#9cf2e6', sh='#103a42')
    s.add('<circle cx="128" cy="128" r="85" fill="#3fb0ac" opacity="0.25" filter="url(#patina1)"/>')
    s.shape(poly(star_points(128, 128, 70, 22, 8, rot=-90)), 'bronze', ow=5, bevel=3, patina=True)
    s.shape(circle(128, 128, 20), 'gold', ow=5, bevel=3, grad='r')
    for i in range(12):
        a = math.radians(i * 30 + 15)
        s.add(f'<circle cx="{f(128 + 99 * math.cos(a))}" cy="{f(128 + 99 * math.sin(a))}" r="0" />')
    s.highlight('M44,96 C56,64 82,40 116,30', 0.55, 5)


def i_echo_strike(s: Svg):
    s.aura('#5fd0c8', 0.3, 150, 110, 74)
    sword(s, 158, 104, 42, 1.08, ghost=True, opacity=0.7)
    s.stroke('M196,24 C238,64 240,128 204,172', '#9cf2e6', 9, glow=True, opacity=0.9)
    s.stroke('M174,16 C224,56 230,120 198,164', '#5fd0c8', 4, opacity=0.65)
    sword(s, 118, 140, 42, 1.12)


def i_vampiric_recall(s: Svg):
    s.aura('#ee5a44', 0.28, 128, 128, 74)
    cup = 'M58,86 L198,86 C198,140 170,168 128,170 C86,168 58,140 58,86 Z'
    s.shape(cup, 'bronze', ow=7, bevel=5, patina=True)
    s.shape(ellipse(128, 88, 68, 13), 'red', ow=4, bevel=0, grad='v')
    s.add('<ellipse cx="112" cy="85" rx="24" ry="4" fill="#ffe4da" opacity="0.6"/>')
    s.shape(poly([(120, 168), (136, 168), (134, 200), (122, 200)]), 'bronze', ow=5, bevel=2.5)
    s.shape(ellipse(128, 184, 18, 8), 'gold', ow=4, bevel=2)
    s.shape('M76,226 C84,204 172,204 180,226 Z', 'bronze', ow=6, bevel=3, patina=True)
    s.add(f'<path d="M70,108 C90,118 166,118 186,108" stroke="{RAMPS["bronze"][-2]}" stroke-width="4" '
          f'fill="none" stroke-dasharray="10 6" opacity="0.8"/>')
    drop = 'M128,10 C142,32 150,44 150,54 C150,66 140,74 128,74 C116,74 106,66 106,54 C106,44 114,32 128,10 Z'
    s.shape(drop, 'red', ow=5, bevel=4, grad='r')
    s.add('<ellipse cx="120" cy="50" rx="5" ry="9" fill="#ffffff" opacity="0.8"/>')
    s.highlight('M74,100 C76,124 88,142 104,152', 0.5, 5)
    s.sparkle(206, 60, 11, '#9cf2e6')
    s.sparkle(48, 54, 8, '#9cf2e6')


def i_hourglass_mastery(s: Svg):
    s.aura('#3fb0ac', 0.28, 128, 128, 74)
    arrow_arc(s, 128, 128, 108, 200, 320, width=14)
    arrow_arc(s, 128, 128, 108, 20, 140, width=14)
    hourglass(s, 'teal', k=0.78)


def i_swift_recollection(s: Svg):
    for i, y in enumerate((150, 172, 194)):
        s.stroke(f'M{8 + i * 6},{y} L{48 + i * 4},{y}', '#5fd0c8', 6, ow=3, opacity=0.9)
    feathers = [(-118, 1.12), (-100, 1.02), (-82, 0.9), (-64, 0.78)]
    for ang, sc in feathers:
        s.shape('M0,0 C14,-22 20,-62 8,-100 C-6,-64 -10,-22 0,0 Z', 'marble', ow=4.5, bevel=3,
                transform=f'translate(104 120) rotate({ang + 90}) scale({f(sc)})')
    upper = 'M80,186 L92,98 L142,90 L160,150 L206,176 L206,190 L80,190 Z'
    s.shape(upper, 'bronze', ow=7, bevel=4, patina=True)
    sole = 'M48,194 C48,184 64,180 92,180 L204,178 C226,178 236,188 232,200 C228,212 210,214 196,214 L62,214 C52,214 48,206 48,194 Z'
    s.shape(sole, 'wood', ow=7, bevel=4)
    for d in ('M96,112 L146,136', 'M90,146 L156,156', 'M150,160 L200,184'):
        s.stroke(d, '#3fb0ac', 9, ow=3, cap='butt')
    s.gem(regular(118, 126, 9, 4), 'teal', ow=3)


def i_waystone_ward(s: Svg):
    s.add('<path d="M22,208 A106,106 0 0 1 234,208 Z" fill="url(#r-teal)" opacity="0.28"/>')
    s.stroke('M22,208 A106,106 0 0 1 234,208', '#9cf2e6', 6, ow=0, glow=True)
    for d in ('M60,208 A68,96 0 0 1 196,208', 'M128,102 L128,208', 'M36,160 C90,150 166,150 220,160'):
        s.stroke(d, '#9cf2e6', 2.4, opacity=0.45)
    s.shape(ellipse(128, 212, 96, 14), 'stone', ow=5, bevel=0, fill=f'url(#g-bronze)')
    s.shape('M98,214 L106,78 L128,50 L150,78 L158,214 Z', 'marble', ow=7, bevel=5)
    s.stroke('M128,92 L128,176 M112,110 L144,124 M112,142 L144,156', '#5fd0c8', 5.5, glow=True)
    s.add(f'<path d="M140,190 L150,170 M118,64 L112,84" stroke="{RAMPS["marble"][-2]}" stroke-width="2.5"/>')


def i_second_draft(s: Svg):
    card(s, 100, 128, -14, 'diamond', k=1.1)
    card(s, 156, 134, 10, 'round', k=1.1)
    arrow_arc(s, 128, 128, 112, 205, 300, width=13)
    arrow_arc(s, 128, 128, 112, 25, 120, width=13)


def i_lucid_start(s: Svg):
    s.begin('translate(-6 18) scale(0.9)')
    s.aura('#5fd0c8', 0.45, 206, 70, 52)
    flame = 'M206,10 C226,40 236,62 232,80 C228,98 218,106 206,106 C194,106 184,98 180,80 C176,62 186,40 206,10 Z'
    s.shape(flame, 'teal', ow=5, bevel=0, grad='v', glow='#5fd0c8', fill='url(#r-teal)')
    s.add('<path d="M206,46 C216,62 220,74 216,86 C212,96 200,96 196,86 C192,74 196,62 206,46 Z" '
          'fill="#fff0b0" filter="url(#glow)"/>')
    s.shape(ring(46, 152, 30, 15), 'bronze', ow=6, bevel=3, evenodd=True, patina=True)
    body = ('M40,166 C40,136 76,122 124,122 L176,122 C190,122 200,116 210,104 L224,110 C220,132 204,146 186,152 '
            'C178,178 152,196 114,196 C70,196 40,190 40,166 Z')
    s.shape(body, 'bronze', ow=7, bevel=5, patina=True)
    s.shape(ellipse(116, 124, 30, 7), 'bronze', ow=4, bevel=0, fill='#3a2716')
    s.shape('M84,196 L150,196 L160,222 L74,222 Z', 'bronze', ow=6, bevel=3, patina=True)
    for i in range(5):
        x = 76 + i * 18
        s.shape('M0,0 C4,-6 12,-8 20,-4 C12,2 4,2 0,0 Z', 'gold', ow=2.4, bevel=0,
                transform=f'translate({x} {164 + (i % 2) * 6}) rotate({-20 + (i % 2) * 40})')
    s.highlight('M58,150 C70,138 92,132 116,132', 0.55, 4.5)
    s.end()


def i_keymaker(s: Svg):
    s.aura('#ffc650', 0.3)
    s.shape(poly([(-9, -70), (9, -70), (9, 100), (-9, 100)]), 'wood', ow=6, bevel=3,
            transform='translate(128 128) rotate(45)')
    s.shape(poly([(-44, -112), (44, -112), (50, -70), (-50, -70)]), 'iron', ow=7, bevel=4,
            transform='translate(128 128) rotate(45)')
    s.shape(poly([(-50, -78), (50, -78), (50, -70), (-50, -70)]), 'bronze', ow=0, bevel=0,
            transform='translate(128 128) rotate(45)', fill='#be8b4a')
    key(s, 96, 96, 45, 0.9)
    for x, y, r in ((128, 128, 16), (160, 104, 9), (104, 156, 8)):
        s.sparkle(x, y, r, '#ffc650')


def i_hoarder(s: Svg):
    s.shape('M46,66 L210,66 L200,118 L56,118 Z', 'wood', ow=7, bevel=4)
    s.shape('M60,78 L196,78 L190,114 L66,114 Z', 'wood', ow=0, bevel=0, fill='#33200e')
    coins = [(84, 110), (106, 102), (130, 98), (152, 104), (174, 112), (96, 120), (120, 116), (146, 118), (166, 124)]
    for x, y in coins:
        s.shape(ellipse(x, y, 14, 9), 'gold', ow=3.5, bevel=2, grad='r')
    s.gem(regular(118, 92, 13, 6), 'teal', ow=3.5, glow='#5fd0c8')
    s.gem(regular(158, 92, 11, 4), 'red', ow=3.5)
    s.gem(regular(86, 98, 10, 5), 'purple', ow=3)
    s.shape('M38,126 L218,126 L212,218 L44,218 Z', 'wood', ow=7, bevel=5)
    for x in (64, 192):
        s.shape(poly([(x - 9, 126), (x + 9, 126), (x + 9, 218), (x - 9, 218)]), 'bronze', ow=4, bevel=2.5,
                patina=True)
    s.shape(poly([(38, 126), (218, 126), (218, 142), (38, 142)]), 'bronze', ow=4, bevel=2.5, patina=True)
    s.shape('M110,140 L146,140 L146,180 L128,192 L110,180 Z', 'gold', ow=5, bevel=3)
    s.add(f'<circle cx="128" cy="160" r="5" fill="{INK}"/><path d="M126,162 L130,162 L131,174 L125,174 Z" '
          f'fill="{INK}"/>')
    s.shape(ellipse(196, 214, 14, 9), 'gold', ow=3.5, bevel=2, grad='r')
    s.shape(ellipse(218, 202, 12, 8), 'gold', ow=3.5, bevel=2, grad='r')
    s.sparkle(70, 52, 10, '#fff0b0')
    s.sparkle(190, 56, 8, '#fff0b0')


def i_party_bond(s: Svg):
    centres = [(128, 90), (88, 156), (168, 156)]
    for cx, cy in centres:
        s.stroke(circle(cx, cy, 50), 'url(#g-bronze)', 18, ow=6)
    # Weave: redraw the top ring's lower-right arc over the right ring, and so on.
    for (cx, cy), (a0, a1) in zip(centres, ((40, 80), (-60, -20), (170, 210))):
        s.stroke(arc_path(cx, cy, 50, a0, a1), 'url(#g-bronze)', 18, ow=6, cap='butt')
    for cx, cy in centres:
        s.add(f'<path d="{arc_path(cx, cy, 50, 200, 260)}" stroke="#fff2c8" stroke-width="4" fill="none" '
              f'opacity="0.65" stroke-linecap="round"/>')
    s.gem(regular(128, 134, 15, 4), 'teal', ow=4, glow='#5fd0c8')


def i_last_stand(s: Svg):
    s.aura('#ee5a44', 0.3, 128, 128, 74)
    crest = 'M54,82 C50,22 206,22 202,82 L184,86 C178,48 78,48 72,86 Z'
    s.shape(crest, 'red', ow=6, bevel=4)
    for i in range(9):
        a = math.radians(200 + i * 17.5)
        x0, y0 = 128 + 64 * math.cos(a), 84 + 52 * math.sin(a)
        x1, y1 = 128 + 74 * math.cos(a), 84 + 62 * math.sin(a)
        s.add(f'<path d="M{f(x0)},{f(y0)} L{f(x1)},{f(y1)}" stroke="#7e1a16" stroke-width="3"/>')
    helm = ('M62,92 C62,52 194,52 194,92 L194,172 C194,200 174,218 152,228 L144,170 L128,160 L112,170 '
            'L104,228 C82,218 62,200 62,172 Z')
    s.shape(helm, 'bronze', ow=7, bevel=5, patina=True)
    for d in ('M78,122 C90,108 116,110 122,124 C114,140 90,140 78,122 Z',
              'M178,122 C166,108 140,110 134,124 C142,140 166,140 178,122 Z'):
        s.shape(d, 'abyss', ow=3, bevel=0, fill='#14102a')
    for x in (104, 152):
        s.add(f'<ellipse cx="{x}" cy="125" rx="7" ry="5" fill="#ff8a6a" filter="url(#glow)"/>')
    s.stroke('M178,70 L166,96 L178,110 L164,134', INK, 4)
    s.highlight('M76,96 C80,76 96,64 116,60', 0.55, 5)


def setting(s: Svg, r, mat='bronze'):
    s.shape(circle(128, 128, r), mat, ow=6, bevel=4, grad='r', patina=(mat == 'bronze'))


def i_rarity(i: int):
    mat = f'rarity{i}'
    color = RARITIES[i]

    def paint(s: Svg):
        glow = [0.0, 0.25, 0.32, 0.42, 0.55, 0.6][i]
        if glow:
            s.aura(color, glow, r=74)
        if i == 0:
            setting(s, 96)
            s.shape(ellipse(128, 128, 74, 62), mat, ow=5, bevel=6, grad='r')
            s.add('<ellipse cx="104" cy="104" rx="26" ry="16" fill="#ffffff" opacity="0.55" '
                  'transform="rotate(-30 104 104)"/>')
        elif i == 1:
            setting(s, 100)
            s.gem(regular(128, 128, 84, 4), mat, table=0.5, ow=6)
        elif i == 2:
            setting(s, 104)
            s.gem(regular(128, 128, 86, 6, rot=-90), mat, table=0.5, ow=6, glow=color)
        elif i == 3:
            s.gem(regular(128, 128, 104, 8, rot=-112.5), mat, table=0.62, ow=6, glow=color)
            s.add(f'<path d="{poly(regular(128, 122, 40, 8, rot=-112.5))}" fill="none" stroke="#ffffff" '
                  f'stroke-width="2" opacity="0.5"/>')
        elif i == 4:
            s.shape(poly(star_points(128, 128, 122, 64, 8, rot=-90)), 'gold', ow=6, bevel=4)
            s.gem(regular(128, 128, 64, 10), mat, table=0.55, ow=5, glow=color)
            for a in (-90, 0, 90, 180):
                s.sparkle(128 + 100 * math.cos(math.radians(a + 45)), 128 + 100 * math.sin(math.radians(a + 45)),
                          12, '#fff0b0')
        else:
            shard = [(128, 14), (166, 70), (204, 60), (186, 124), (222, 176), (150, 170), (128, 242), (104, 168),
                     (36, 182), (72, 124), (48, 64), (92, 72)]
            s.gem(shard, mat, table=0.42, ow=6, glow=color)
            for d in ('M128,64 L120,104 L136,128 L122,156', 'M90,110 L118,124', 'M170,136 L138,132'):
                s.stroke(d, '#fff0a0', 3.4, glow=True)
            for x, y in ((30, 40), (224, 34), (226, 226), (24, 222)):
                s.sparkle(x, y, 9, '#ff9e86')
    return paint


def i_renown(s: Svg):
    s.aura('#f7d470', 0.3, 128, 128, 74)
    laurel_branch(s, -1, 128, 124, 96, 262, 128, leaves=7, size=1.15)
    laurel_branch(s, 1, 128, 124, 96, 262, 128, leaves=7, size=1.15)
    s.shape('M100,206 L128,222 L156,206 L162,236 L128,226 L94,236 Z', 'red', ow=5, bevel=2.5)
    s.shape(circle(128, 216, 11), 'red', ow=4, bevel=2, grad='r')
    s.shape(poly(star_points(128, 118, 56, 23, 5)), 'gold', ow=7, bevel=5, glow='#f7d470')
    s.highlight('M110,90 L124,66', 0.7, 4)


def i_key(s: Svg):
    s.aura('#5fd0c8', 0.3, 84, 84, 60)
    key(s, 120, 124, 45, 1.08)


def i_hourglass(s: Svg):
    hourglass(s, 'teal')


def figure(s: Svg, cx, cy, k, mat, gem=False):
    head = circle(cx, cy - 46 * k, 24 * k)
    body = (f'M{f(cx - 48 * k)},{f(cy + 52 * k)} C{f(cx - 48 * k)},{f(cy + 2 * k)} {f(cx - 30 * k)},{f(cy - 14 * k)} '
            f'{f(cx)},{f(cy - 14 * k)} C{f(cx + 30 * k)},{f(cy - 14 * k)} {f(cx + 48 * k)},{f(cy + 2 * k)} '
            f'{f(cx + 48 * k)},{f(cy + 52 * k)} Z')
    s.shape(body, mat, ow=6, bevel=4, patina=(mat == 'bronze'))
    s.shape(head, mat, ow=6, bevel=4, grad='r')
    if gem:
        s.gem(regular(cx, cy + 10 * k, 10 * k, 4), 'teal', ow=3, glow='#5fd0c8')


def i_party(s: Svg):
    figure(s, 66, 150, 0.84, 'gold')
    figure(s, 190, 150, 0.84, 'gold')
    figure(s, 128, 166, 1.06, 'bronze', gem=True)


def i_weekly(s: Svg):
    s.aura('#f7d470', 0.3, 118, 128, 74)
    mid = s.uid('m')
    s.defs.append(f'<mask id="{mid}"><rect width="256" height="256" fill="#fff"/>'
                  f'<circle cx="150" cy="104" r="62" fill="#000"/></mask>')
    s.add(f'<g mask="url(#{mid})">'
          f'<circle cx="114" cy="132" r="82" fill="{INK}"/>'
          f'<circle cx="114" cy="132" r="75" fill="url(#r-gold)"/>'
          f'<circle cx="114" cy="132" r="75" fill="#3fb0ac" opacity="0.25" filter="url(#patina2)"/></g>')
    s.add(f'<circle cx="150" cy="104" r="62" fill="none" stroke="{INK}" stroke-width="7" '
          f'mask="url(#{mid}i)"/>')
    s.defs.append(f'<mask id="{mid}i"><circle cx="114" cy="132" r="82" fill="#fff"/></mask>')
    s.highlight('M54,158 C60,110 84,80 112,70', 0.6, 5)
    for i in range(7):
        a = math.radians(-70 + i * 26)
        x = 132 + 104 * math.cos(a) * 0.95
        y = 128 + 104 * math.sin(a)
        r = 15 if i == 3 else 10
        s.shape(poly(star_points(x + 4, y, r, r * 0.38, 4)), 'teal', ow=3, bevel=0, fill='#9cf2e6',
                glow='#5fd0c8')


def i_records(s: Svg):
    s.shape('M60,212 L60,70 C60,32 196,32 196,70 L196,212 Z', 'marble', ow=7, bevel=5)
    s.shape('M40,206 L216,206 L216,234 L40,234 Z', 'marble', ow=6, bevel=4, grad='r')
    for y, w in ((106, 52), (128, 60), (150, 44), (172, 56)):
        s.add(f'<path d="M{128 - w},{y} L{128 + w},{y}" stroke="{RAMPS["marble"][-2]}" stroke-width="5" '
              f'stroke-linecap="round"/>')
        s.add(f'<path d="M{128 - w},{y + 2.5} L{128 + w},{y + 2.5}" stroke="#ffffff" stroke-width="2" '
              f'stroke-linecap="round" opacity="0.7"/>')
    s.shape(poly(star_points(128, 68, 22, 9, 5)), 'gold', ow=4, bevel=2.5, glow='#f7d470')
    s.add(f'<path d="M180,90 L172,110 L180,124" stroke="{RAMPS["marble"][-1]}" stroke-width="2.5" fill="none"/>')
    s.add('<path d="M196,206 L196,120" stroke="#3fb0ac" stroke-width="5" opacity="0.4"/>')


def i_bestiary(s: Svg):
    s.shape('M128,82 C98,66 60,62 22,70 L22,214 C60,206 98,210 128,224 C158,210 196,206 234,214 L234,70 '
            'C196,62 158,66 128,82 Z', 'wood', ow=7, bevel=4)
    s.shape('M128,72 C100,56 66,54 34,60 L34,198 C66,192 100,194 128,208 Z', 'parchment', ow=4, bevel=4)
    s.shape('M128,72 C156,56 190,54 222,60 L222,198 C190,192 156,194 128,208 Z', 'parchment', ow=4, bevel=4)
    for i, y in enumerate((88, 104, 120, 136, 152, 168)):
        w = 56 if i % 2 else 46
        s.add(f'<path d="M{48},{y + 4 - i * 0.5} C70,{y - 2} 96,{y} {48 + w + 10},{y + 8}" '
              f'stroke="{RAMPS["parchment"][-2]}" stroke-width="3" fill="none" opacity="0.7"/>')
    for i, dx in enumerate((-24, 0, 24)):
        slash = f'M{186 + dx},{60 + i * 4} C{170 + dx},{110} {150 + dx},{150} {120 + dx},{196 - i * 4} ' \
                f'L{128 + dx},{190 - i * 4} C{158 + dx},{148} {176 + dx},{108} {186 + dx},{60 + i * 4} Z'
        s.shape(slash, 'red', ow=3.5, bevel=0, fill='#7e1a16')
    s.add(f'<path d="M128,76 L128,206" stroke="{RAMPS["parchment"][-1]}" stroke-width="3" opacity="0.6"/>')


def i_relic(s: Svg):
    s.aura('#5fd0c8', 0.3, 128, 158, 70)
    s.stroke('M48,20 C56,70 92,96 128,98 C164,96 200,70 208,20', 'url(#g-gold)', 7, ow=3.5, dash='10 5',
             cap='butt')
    pend = 'M128,92 C172,92 200,122 200,156 C200,196 164,226 128,238 C92,226 56,196 56,156 C56,122 84,92 128,92 Z'
    s.shape(pend, 'bronze', ow=7, bevel=5, patina=True)
    s.shape(ring(128, 92, 13, 6), 'gold', ow=4, bevel=2, evenodd=True)
    s.gem(regular(128, 160, 42, 8, rot=-90, sx=0.86), 'teal', table=0.5, ow=5, glow='#5fd0c8')
    for a in range(0, 360, 30):
        x = 128 + 58 * math.cos(math.radians(a)) * 0.98
        y = 162 + 58 * math.sin(math.radians(a)) * 1.08
        s.add(f'<circle cx="{f(x)}" cy="{f(y)}" r="3.4" fill="#ffe29b" stroke="{INK}" stroke-width="1.6"/>')


def i_remember(s: Svg):
    s.aura('#5fd0c8', 0.55, 128, 132, 74)
    s.stroke('M98,48 C98,14 158,14 158,48', 'url(#g-bronze)', 9, ow=4)
    s.shape('M86,46 L170,46 L182,72 L74,72 Z', 'bronze', ow=6, bevel=3.5, patina=True)
    s.shape('M82,72 L174,72 L174,190 L82,190 Z', 'teal', ow=6, bevel=0, fill='url(#r-teal)', opacity=0.9)
    s.add('<path d="M128,92 C146,118 154,136 150,152 C146,166 138,172 128,172 C118,172 110,166 106,152 '
          'C102,136 110,118 128,92 Z" fill="#f2fffc" filter="url(#glow)"/>')
    s.add('<path d="M128,124 C136,138 138,148 136,156 C134,162 122,162 120,156 C118,148 120,138 128,124 Z" '
          'fill="#fff0b0"/>')
    for x in (82, 174):
        s.shape(poly([(x - 8, 70), (x + 8, 70), (x + 8, 192), (x - 8, 192)]), 'bronze', ow=5, bevel=3, patina=True)
    s.shape(poly([(124, 70), (132, 70), (132, 96), (124, 96)]), 'bronze', ow=3, bevel=1.5)
    s.shape('M70,190 L186,190 L176,220 L80,220 Z', 'bronze', ow=6, bevel=3.5, patina=True)
    s.highlight('M92,84 L92,176', 0.5, 4)


def i_forget(s: Svg):
    s.aura('#8a48b0', 0.5, 128, 150, 74)
    s.shape(ellipse(128, 150, 108, 52), 'abyss', ow=6, bevel=0, fill='url(#r-abyss)')
    for i, (y, rx, ry) in enumerate(((150, 96, 44), (156, 74, 33), (162, 52, 23), (167, 32, 14), (170, 14, 6))):
        col = mix('#c58ae6', '#ee5a44', i / 4)
        s.stroke(ellipse(128, y, rx, ry), col, 5 - i * 0.5, glow=True, opacity=0.9)
    s.add('<ellipse cx="128" cy="170" rx="10" ry="4" fill="#0c0618"/>')
    s.shape('M114,14 L142,14 L142,98 L170,98 L128,150 L86,98 L114,98 Z', 'bronze', ow=7, bevel=4, patina=True)
    s.highlight('M120,24 L120,100', 0.6, 4)


def i_momentum(s: Svg):
    s.aura('#ffc650', 0.32, 150, 128, 74)
    for i, y in enumerate((92, 128, 164)):
        s.stroke(f'M{10 + i * 4},{y} L{56 - i * 4},{y}', '#ffc650', 6, ow=3, opacity=0.85)

    def right_chevron(x):
        return poly([(x, 40), (x + 52, 128), (x, 216), (x + 30, 216), (x + 82, 128), (x + 30, 40)])

    s.shape(right_chevron(48), 'bronze', ow=7, bevel=4, patina=True)
    s.shape(right_chevron(98), 'gold', ow=7, bevel=4)
    s.shape(right_chevron(148), 'ember', ow=7, bevel=4, glow='#ff8a2a')
    s.highlight('M154,48 L196,120', 0.7, 4)


def i_insight(s: Svg):
    s.aura('#5fd0c8', 0.28, 128, 128, 74)
    body = 'M128,48 C178,48 204,94 204,148 C204,198 170,230 128,230 C86,230 52,198 52,148 C52,94 78,48 128,48 Z'
    s.shape('M60,76 L52,20 L102,58 Z', 'bronze', ow=6, bevel=3)
    s.shape('M196,76 L204,20 L154,58 Z', 'bronze', ow=6, bevel=3)
    s.shape(body, 'bronze', ow=7, bevel=5, patina=True)
    for row, y in enumerate((168, 186, 204)):
        for i in range(4 - (row == 2)):
            x = 92 + i * 24 + (row % 2) * 12
            s.add(f'<path d="M{x - 10},{y} Q{x},{y + 12} {x + 10},{y}" stroke="{RAMPS["bronze"][-1]}" '
                  f'stroke-width="3.5" fill="none" opacity="0.8"/>')
    s.shape('M56,128 C40,160 48,196 76,214 C70,186 70,156 80,136 Z', 'bronze', ow=5, bevel=3)
    s.shape('M200,128 C216,160 208,196 180,214 C186,186 186,156 176,136 Z', 'bronze', ow=5, bevel=3)
    for x in (98, 158):
        s.shape(circle(x, 110, 32), 'marble', ow=5, bevel=3, grad='r')
        s.add(f'<circle cx="{x}" cy="110" r="19" fill="url(#r-teal)" filter="url(#glow)"/>')
        s.add(f'<circle cx="{x}" cy="110" r="8.5" fill="{INK}"/>')
        s.add(f'<circle cx="{x - 6}" cy="104" r="4.5" fill="#ffffff"/>')
    s.shape('M118,128 L138,128 L128,152 Z', 'gold', ow=4, bevel=1.5)


def i_leader(s: Svg):
    s.aura('#f7d470', 0.35, 128, 140, 74)
    s.shape('M28,198 L28,72 L82,128 L128,48 L174,128 L228,72 L228,198 Z', 'gold', ow=9, bevel=6)
    s.shape(poly([(24, 170), (232, 170), (232, 212), (24, 212)]), 'bronze', ow=8, bevel=4, patina=True)
    s.gem(regular(128, 191, 18, 4), 'teal', ow=4.5, glow='#5fd0c8')
    for x, y in ((28, 70), (128, 46), (228, 70)):
        s.shape(circle(x, y, 16), 'gold', ow=6, bevel=3, grad='r')


def i_archive(s: Svg):
    s.shape('M18,96 L128,28 L238,96 Z', 'marble', ow=8, bevel=5)
    s.add('<circle cx="128" cy="74" r="13" fill="#5fd0c8" filter="url(#glow)"/>')
    s.add(f'<circle cx="128" cy="74" r="13" fill="none" stroke="{INK}" stroke-width="4"/>')
    s.shape(poly([(26, 96), (230, 96), (230, 118), (26, 118)]), 'marble', ow=7, bevel=4)
    for x in (54, 102, 154, 202):
        s.shape(poly([(x - 15, 118), (x + 15, 118), (x + 13, 196), (x - 13, 196)]), 'marble', ow=7, bevel=4)
        s.add(f'<path d="M{x - 5},126 L{x - 5},190 M{x + 5},126 L{x + 5},190" stroke="{RAMPS["marble"][-2]}" '
              f'stroke-width="2.6" opacity="0.8"/>')
    s.shape(poly([(22, 196), (234, 196), (234, 214), (22, 214)]), 'marble', ow=7, bevel=4)
    s.shape(poly([(8, 214), (248, 214), (248, 234), (8, 234)]), 'marble', ow=7, bevel=4)


def i_sealed(s: Svg):
    pts = []
    for i in range(36):
        a = math.radians(i * 10)
        wob = 1 + 0.06 * math.sin(i * 2.7) + 0.04 * math.cos(i * 5.3)
        pts.append((128 + 100 * wob * math.cos(a), 124 + 96 * wob * math.sin(a)))
    blob = poly(pts)
    s.shape(blob, 'red', ow=7, bevel=6, grad='r')
    for x, h in ((88, 30), (150, 44), (176, 26)):
        s.shape(f'M{x - 10},210 C{x - 10},{210 + h} {x + 10},{210 + h} {x + 10},210 Z', 'red', ow=5, bevel=3)
    s.shape(blob, 'red', ow=0, bevel=0, fill='url(#r-red)')
    s.shape(circle(128, 124, 66), 'red', ow=0, bevel=6, fill='#a3201a', hl='#3e0a0a', sh='#ff9e86')
    s.meander_ring(128, 124, 56, 14, 9, '#5a0e0c', width=2.4)
    s.add(f'<circle cx="128" cy="108" r="17" fill="{INK}"/>'
          f'<path d="M118,116 L138,116 L146,162 L110,162 Z" fill="{INK}"/>')
    s.add('<circle cx="124" cy="104" r="5" fill="#ff9e86" opacity="0.6"/>')
    s.highlight('M52,96 C62,62 90,40 124,34', 0.55, 6)


ICONS = [
    i_fortified, i_tyrannical, i_bolstering, i_volatile, i_haunted, i_frailty, i_hourglass_distortion, i_amnesia,
    i_steady_hand, i_deep_recall, i_keen_eye, i_archivists_grace, i_bulwark_memory, i_echo_strike,
    i_vampiric_recall, i_hourglass_mastery, i_swift_recollection, i_waystone_ward, i_second_draft, i_lucid_start,
    i_keymaker, i_hoarder, i_party_bond, i_last_stand,
    *[i_rarity(i) for i in range(6)],
    i_renown, i_key, i_hourglass, i_party, i_weekly, i_records, i_bestiary, i_relic, i_remember, i_forget,
    i_momentum, i_insight, i_leader, i_archive, i_sealed,
]
assert len(ICONS) == 45, len(ICONS)


def _edge_mask() -> Image.Image:
    """Fades the outermost pixels of a cell so glows never end in a hard line, even with linear filtering."""
    mask = Image.new('L', (CELL, CELL), 0)
    px = mask.load()
    for y in range(CELL):
        for x in range(CELL):
            d = min(x, y, CELL - 1 - x, CELL - 1 - y)
            px[x, y] = 255 if d >= 6 else round(255 * (d / 6) ** 1.5)
    return mask


EDGE = _edge_mask()


def render(html: str, width: int, height: int) -> Image.Image:
    with tempfile.TemporaryDirectory() as tmp:
        src = Path(tmp) / 'icons.html'
        out = Path(tmp) / 'icons.png'
        src.write_text(html, encoding='utf-8')
        subprocess.run(['node', str(Path(__file__).with_name('render.cjs')), str(src), str(out), str(width),
                        str(height)], check=True)
        return Image.open(out).convert('RGBA').copy()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--preview', help='also write an enlarged preview on parchment and slate')
    parser.add_argument('--only', type=int, nargs='*', help='render only these cells (for iteration)')
    args = parser.parse_args()
    rows = math.ceil(len(ICONS) / COLUMNS)
    cells = []
    for i, paint in enumerate(ICONS):
        s = Svg(i)
        if args.only is None or i in args.only:
            paint(s)
        cells.append(f'<div style="position:absolute;left:{(i % COLUMNS) * RENDER}px;top:{(i // COLUMNS) * RENDER}px;'
                     f'width:{RENDER}px;height:{RENDER}px">{s.svg()}</div>')
    html = ('<!doctype html><html><head><meta charset="utf-8"><style>html,body{margin:0;background:transparent}'
            'svg{display:block}</style></head><body>' + shared_defs() + ''.join(cells) + '</body></html>')
    big = render(html, COLUMNS * RENDER, rows * RENDER)
    sheet = Image.new('RGBA', (SHEET, SHEET), (0, 0, 0, 0))
    for i in range(len(ICONS)):
        x, y = (i % COLUMNS) * RENDER, (i // COLUMNS) * RENDER
        cell = big.crop((x, y, x + RENDER, y + RENDER))
        # Downsample in premultiplied space so the ink edge does not halo.
        cell = cell.convert('RGBa').resize((CELL, CELL), Image.LANCZOS).convert('RGBA')
        cell = cell.filter(ImageFilter.UnsharpMask(radius=0.8, percent=60, threshold=1))
        cell.putalpha(Image.composite(cell.getchannel('A'), Image.new('L', cell.size, 0), EDGE))
        sheet.alpha_composite(cell, ((i % COLUMNS) * CELL, (i // COLUMNS) * CELL))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(OUT, optimize=True)
    print(f'wrote {OUT.relative_to(ROOT)}')
    if args.preview:
        preview = Image.new('RGBA', (COLUMNS * 160, rows * 160 * 2), (0, 0, 0, 255))
        for half, bg in enumerate(((243, 233, 210, 255), (28, 26, 34, 255))):
            panel = Image.new('RGBA', (COLUMNS * 160, rows * 160), bg)
            for i in range(len(ICONS)):
                icon = sheet.crop(((i % COLUMNS) * CELL, (i // COLUMNS) * CELL, (i % COLUMNS + 1) * CELL,
                                   (i // COLUMNS + 1) * CELL))
                panel.alpha_composite(icon, ((i % COLUMNS) * 160 + 16, (i // COLUMNS) * 160 + 16))
            preview.alpha_composite(panel, (0, half * rows * 160))
        preview.save(args.preview)
        print(f'wrote {args.preview}')


if __name__ == '__main__':
    main()
