#!/usr/bin/env python3
"""The hand-drawn map: old paper, ink and a little blood. No words on it - drawings: the islands of bone to the north,
the great serpent to the east, the falling edge to the south, the lighthouse to the west, an X in the middle.
1 pixel = 8 blocks; the middle of the sea is the middle of the picture."""
import math, os, json
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from ckit import vnoise, fbm, ramp

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, "../common/src/main/resources/assets/bloodsea")
N = 512
C = N / 2
S = 1 / 8.0          # pixels per block


def P(x, z):
    return (C + x * S, C + z * S)


def main():
    rng = np.random.default_rng(5)
    yy, xx = np.mgrid[0:N, 0:N]
    v = fbm(xx.astype(float), yy.astype(float), np.zeros_like(xx, float), 60, 201) * 0.6 + vnoise(xx.astype(float), yy.astype(float), 0, 6, 202) * 0.4
    paper = ramp(v.ravel(), [(0, "#9c8460"), (0.5, "#c8b088"), (1, "#e0cca4")]).reshape(N, N, 3)
    r = np.sqrt((xx - C) ** 2 + (yy - C) ** 2) / C
    paper *= np.clip(1.15 - r ** 3 * 0.5, 0.5, 1.1)[..., None]
    stain = np.clip((fbm(xx.astype(float), yy.astype(float), np.zeros_like(xx, float), 40, 203) - 0.62) * 4, 0, 0.7)
    paper = paper * (1 - stain[..., None]) + np.array([90, 20, 18]) * stain[..., None]
    img = Image.fromarray(np.clip(paper, 0, 255).astype(np.uint8), "RGB")
    d = ImageDraw.Draw(img)
    ink = (40, 28, 20)

    def wobble_line(pts, w=2):
        out = []
        for i in range(len(pts) - 1):
            (x0, y0), (x1, y1) = pts[i], pts[i + 1]
            for k in range(8):
                t = k / 8
                out.append((x0 + (x1 - x0) * t + rng.normal(0, 0.6), y0 + (y1 - y0) * t + rng.normal(0, 0.6)))
        out.append(pts[-1])
        d.line(out, fill=ink, width=w)

    # the calm middle: a dotted circle, the X
    for k in range(64):
        a0, a1 = k / 64 * 2 * math.pi, (k + 0.5) / 64 * 2 * math.pi
        d.line([P(420 * math.cos(a0), 420 * math.sin(a0)), P(420 * math.cos(a1), 420 * math.sin(a1))], fill=ink, width=1)
    cx, cy = P(0, 0)
    d.line([(cx - 7, cy - 7), (cx + 7, cy + 7)], fill=(120, 10, 10), width=3)
    d.line([(cx - 7, cy + 7), (cx + 7, cy - 7)], fill=(120, 10, 10), width=3)
    # the four ways out: dashed lines
    for ang in (45, 135, 225, 315):
        a = math.radians(ang)
        for k in range(14):
            r0, r1 = 440 + k * 110, 440 + k * 110 + 60
            d.line([P(r0 * math.sin(a), -r0 * math.cos(a)), P(r1 * math.sin(a), -r1 * math.cos(a))], fill=ink, width=1)
    # north: islands drawn like bones and skulls
    for k in range(9):
        x, z = rng.uniform(-500, 500), rng.uniform(-1700, -800)
        px, py = P(x, z)
        rr = rng.uniform(6, 14)
        d.ellipse([px - rr, py - rr * 0.6, px + rr, py + rr * 0.6], outline=ink, width=2)
    sx, sy = P(0, -1450)
    d.ellipse([sx - 16, sy - 18, sx + 16, sy + 12], outline=ink, width=3)       # a skull, on the throne island
    d.ellipse([sx - 9, sy - 8, sx - 3, sy - 2], fill=ink)
    d.ellipse([sx + 3, sy - 8, sx + 9, sy - 2], fill=ink)
    d.line([(sx - 6, sy + 8), (sx + 6, sy + 8)], fill=ink, width=2)
    # east: the serpent, its coils in and out of the sea, and its heart
    pts = []
    for k in range(60):
        t = k / 59
        x = 650 + t * 850
        z = 140 * math.sin(x / 200) + 30 * math.sin(t * 20)
        pts.append(P(x, z))
    wobble_line(pts, 3)
    hx, hy = P(1450, 0)
    for rr in (14, 20, 26):
        d.arc([hx - rr, hy - rr, hx + rr, hy + rr], 200, 340, fill=(120, 10, 10), width=2)
    d.polygon([(hx - 10, hy - 4), (hx, hy + 10), (hx + 10, hy - 4), (hx + 5, hy - 10), (hx, hy - 6), (hx - 5, hy - 10)], fill=(130, 12, 12))
    # south: the edge, an arc with lines falling from it
    a0, a1 = math.radians(135), math.radians(225)
    arc = [P(1900 * math.sin(math.radians(ang)), -1900 * math.cos(math.radians(ang))) for ang in np.linspace(135, 225, 40)]
    wobble_line(arc, 3)
    for k in range(0, 40, 2):
        x, y = arc[k]
        d.line([(x, y), (x + rng.normal(0, 1), y + 12 + rng.uniform(0, 8))], fill=ink, width=1)
    # west: the lighthouse, with its beams
    lx, ly = P(-1450, 0)
    d.polygon([(lx - 6, ly + 14), (lx + 6, ly + 14), (lx + 3, ly - 12), (lx - 3, ly - 12)], outline=ink, fill=(200, 180, 140))
    d.rectangle([lx - 4, ly - 18, lx + 4, ly - 12], outline=ink, fill=(230, 200, 90))
    for k in (-1, 1):
        d.line([(lx, ly - 15), (lx + k * 30, ly - 30)], fill=(150, 120, 40), width=1)
        d.line([(lx, ly - 15), (lx + k * 30, ly - 2)], fill=(150, 120, 40), width=1)
    # a compass
    kx, ky = 70, 440
    d.line([(kx, ky - 26), (kx, ky + 26)], fill=ink, width=2)
    d.line([(kx - 26, ky), (kx + 26, ky)], fill=ink, width=2)
    d.polygon([(kx, ky - 30), (kx - 5, ky - 18), (kx + 5, ky - 18)], fill=(120, 10, 10))
    img = img.filter(ImageFilter.GaussianBlur(0.4))
    os.makedirs(os.path.join(A, "textures/gui"), exist_ok=True)
    img.save(os.path.join(A, "textures/gui/sea_map.png"))
    # the item: a folded sheet with a red X
    ic = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    di = ImageDraw.Draw(ic)
    di.polygon([(2, 3), (13, 2), (14, 13), (3, 14)], fill=(200, 176, 132, 255), outline=(90, 66, 40, 255))
    di.line([(8, 3), (8, 13)], fill=(150, 128, 90, 255))
    di.line([(5, 6), (10, 11)], fill=(140, 20, 20, 255))
    di.line([(10, 6), (5, 11)], fill=(140, 20, 20, 255))
    ic.save(os.path.join(A, "textures/item/sea_map.png"))
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "bloodsea:item/sea_map"}},
              open(os.path.join(A, "models/item/sea_map.json"), "w"))
    print("map ok")


if __name__ == "__main__":
    main()
