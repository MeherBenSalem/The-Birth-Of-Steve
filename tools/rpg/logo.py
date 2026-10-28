#!/usr/bin/env python3
"""Draws the mod logo: 40 x 40 pixel art scaled to 600 x 600 (15 px per pixel),
the size and grid of the Remnants family logos.

The Fractured Archive's gateway arch, a teal memory vortex inside it and the
Memory Key standing in the light, over a dusk sky and ruined columns. Colours
come from the item palette. Writes shared/common/src/main/resources/logo.png and
marketing/source/logo.png. Run from the repository root:

    python3 tools/rpg/logo.py
"""
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUTS = [ROOT / 'shared/common/src/main/resources/logo.png', ROOT / 'marketing/source/logo.png']
N = 40
SCALE = 15

INK = (13, 11, 20)
SKY = [(20, 16, 42), (28, 22, 56), (40, 31, 76), (52, 40, 100), (70, 55, 130)]
STONE = [(255, 250, 236), (230, 216, 184), (201, 180, 138), (154, 132, 98), (94, 78, 56), (52, 42, 30)]
TEAL = [(215, 255, 247), (156, 242, 230), (63, 176, 172), (31, 110, 116), (16, 58, 66)]
BRONZE = [(255, 226, 155), (232, 192, 122), (190, 139, 74), (138, 96, 51), (94, 64, 36), (58, 39, 22)]
SHADOW = (24, 20, 40)


def put(img, x, y, c):
    if 0 <= x < N and 0 <= y < N:
        img[y][x] = c


def draw() -> list[list[tuple]]:
    img = [[None] * N for _ in range(N)]
    # Sky: banded dusk, lightest at the horizon, with an ordered dither between bands.
    for y in range(N):
        t = y / (N - 1)
        band = t * (len(SKY) - 1)
        lo = int(band)
        frac = band - lo
        for x in range(N):
            bayer = ((x % 2) * 2 + (y % 2) * 3) % 4 / 4 + 0.125
            c = SKY[min(lo + (1 if frac > bayer else 0), len(SKY) - 1)]
            img[y][x] = c
    for x, y, c in ((4, 3, TEAL[0]), (33, 5, TEAL[1]), (7, 11, TEAL[1]), (36, 14, TEAL[0]), (27, 2, TEAL[2]),
                    (12, 6, TEAL[2]), (2, 18, TEAL[2]), (37, 22, TEAL[1])):
        put(img, x, y, c)

    cx, top, base = 20, 7, 35
    r_out, r_in = 13.6, 9.4
    arch_cy = top + r_out

    def in_arch(x, y, r):
        px, py = x + 0.5, y + 0.5
        if py < arch_cy:
            return math.hypot(px - cx, py - arch_cy) <= r
        return abs(px - cx) <= r and py <= base

    # Vortex light behind the opening.
    for y in range(N):
        for x in range(N):
            if in_arch(x, y, r_in):
                px, py = x + 0.5, y + 0.5
                d = math.hypot(px - cx, (py - (arch_cy + 3)) * 0.85)
                ang = math.atan2(py - (arch_cy + 3), px - cx)
                swirl = (d * 0.9 + ang * 2.2) % 3.0
                level = 4 - min(4, int(d / 2.9))
                if swirl < 0.9 and level > 0:
                    level -= 1
                img[y][x] = TEAL[4 - level] if level > 0 else TEAL[4]
    # Arch stones with joints, lit from the upper left.
    for y in range(N):
        for x in range(N):
            if in_arch(x, y, r_out) and not in_arch(x, y, r_in):
                px, py = x + 0.5, y + 0.5
                if py < arch_cy:
                    ang = math.degrees(math.atan2(py - arch_cy, px - cx))
                    joint = abs(((ang + 180) % 22.5) - 11.25) > 10.2
                    rad = math.hypot(px - cx, py - arch_cy)
                    shade = 1 if px < cx else 2
                    if rad > r_out - 1.2:
                        shade += 1
                    if rad < r_in + 1.1:
                        shade = 3 if px < cx else 4
                    img[y][x] = STONE[4] if joint else STONE[shade]
                else:
                    side_left = px < cx
                    edge = abs(px - cx) < r_in + 1.2
                    shade = (1 if side_left else 2) + (2 if edge else 0)
                    joint = (y - int(arch_cy)) % 5 == 0
                    img[y][x] = STONE[5] if joint else STONE[min(5, shade)]
    # Keystone crystal.
    ky = int(top + 1)
    for dx, dy, c in ((0, 0, TEAL[1]), (-1, 1, TEAL[1]), (0, 1, TEAL[0]), (1, 1, TEAL[2]), (-1, 2, TEAL[2]),
                      (0, 2, TEAL[2]), (1, 2, TEAL[3]), (0, 3, TEAL[3])):
        put(img, cx + dx, ky + dy, c)

    # Ruined columns either side, as dusk silhouettes so the arch stays the subject.
    ruin = (31, 25, 54)
    for x0, height, broken in ((3, 12, 1), (34, 9, 2)):
        for y in range(base - height, base):
            for x in range(x0, x0 + 3):
                put(img, x, y, ruin)
        for x in range(x0 - 1, x0 + 4):
            put(img, x, base - height - 1, ruin)
        put(img, x0 + broken, base - height - 2, ruin)
    for x, h in ((9, 1), (10, 2), (30, 1), (29, 1), (1, 1)):
        for y in range(base - h, base):
            put(img, x, y, ruin)
    # Ground and steps.
    for y in range(base, N):
        for x in range(N):
            img[y][x] = SHADOW
    for x in range(int(cx - r_out) - 1, int(cx + r_out) + 2):
        put(img, x, base, STONE[3] if x % 3 else STONE[4])
        put(img, x, base + 1, STONE[4] if x % 4 else STONE[5])

    # The Memory Key standing in the vortex: bow below, bit above.
    kx = cx
    for y in range(11, 26):
        put(img, kx - 1, y, BRONZE[0])
        put(img, kx, y, BRONZE[2])
    for y, c in ((13, BRONZE[1]), (14, BRONZE[3]), (16, BRONZE[1]), (17, BRONZE[3])):
        put(img, kx + 1, y, c)
        put(img, kx + 2, y, BRONZE[3] if c == BRONZE[1] else BRONZE[4])
    put(img, kx + 1, 11, BRONZE[2])
    for x in (kx - 2, kx + 1):
        for y in (21, 22):
            put(img, x, y, BRONZE[1] if x < kx else BRONZE[3])
    bcx, bcy = kx - 0.5, 28.5
    for y in range(24, 34):
        for x in range(kx - 6, kx + 6):
            d = math.hypot(x + 0.5 - bcx - 0.5, y + 0.5 - bcy)
            if 2.2 <= d <= 4.3:
                light = (x + 0.5 - bcx) + (y + 0.5 - bcy)
                put(img, x, y, BRONZE[0] if light < -2 else BRONZE[1] if light < 0 else BRONZE[3] if light < 2.5 else BRONZE[4])
            elif d < 2.2:
                put(img, x, y, TEAL[0] if (x + y) % 3 == 0 and d < 1.2 else TEAL[1] if d < 1.6 else TEAL[2])
    # Outline everything that is not sky, vortex, ground or column.
    sky_like = set(SKY) | set(TEAL) | {SHADOW}
    out = [row[:] for row in img]
    key_cells = set()
    for y in range(N):
        for x in range(N):
            if img[y][x] in BRONZE:
                key_cells.add((x, y))
    for y in range(N):
        for x in range(N):
            if (x, y) in key_cells:
                continue
            if any((x + dx, y + dy) in key_cells for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                out[y][x] = INK
    # Outline the arch against the sky.
    for y in range(N):
        for x in range(N):
            if img[y][x] in SKY and any(0 <= x + dx < N and 0 <= y + dy < N and img[y + dy][x + dx] in STONE
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out[y][x] = INK
    # Rounded frame, as on the other Remnants logos.
    for y in range(N):
        for x in range(N):
            cxn = min(x, N - 1 - x)
            cyn = min(y, N - 1 - y)
            if cxn < 3 and cyn < 3 and math.hypot(2.5 - cxn, 2.5 - cyn) > 2.6:
                out[y][x] = None
            elif cxn == 0 or cyn == 0 or (cxn < 3 and cyn < 3 and math.hypot(2.5 - cxn, 2.5 - cyn) > 1.7):
                out[y][x] = INK
    return out


def main():
    grid = draw()
    small = Image.new('RGBA', (N, N))
    for y in range(N):
        for x in range(N):
            small.putpixel((x, y), (0, 0, 0, 0) if grid[y][x] is None else grid[y][x] + (255,))
    big = small.resize((N * SCALE, N * SCALE), Image.NEAREST)
    for out in OUTS:
        out.parent.mkdir(parents=True, exist_ok=True)
        big.save(out)
        print(f'wrote {out.relative_to(ROOT)}')


if __name__ == '__main__':
    main()
