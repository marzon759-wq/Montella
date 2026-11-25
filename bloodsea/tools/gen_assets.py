#!/usr/bin/env python3
"""Every asset of BLOOD SEA: block textures, the blood itself (animated), the Pale Sun and the Blood Moon, models,
blockstates, the dimension, its five biomes and the English and Turkish text."""
import json, math, os, random
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
R = os.path.join(HERE, "../common/src/main/resources")
A = os.path.join(R, "assets/bloodsea")
D = os.path.join(R, "data/bloodsea")
NS = "bloodsea"


def W(path, obj):
    p = os.path.join(R, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=1, ensure_ascii=False)


def save(img, path, frametime=None):
    p = os.path.join(A, "textures", path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    if frametime:
        with open(p + ".mcmeta", "w") as f:
            json.dump({"animation": {"frametime": frametime, "interpolate": True}}, f)


def rgb(c):
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5))


def noise(n, seed, scale=4.0, octaves=3):
    """tileable value noise in 0..1"""
    rng = np.random.default_rng(seed)
    out = np.zeros((n, n))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        g = max(2, int(scale * 2 ** o))
        grid = rng.random((g, g))
        xs = np.linspace(0, g, n, endpoint=False)
        x0 = np.floor(xs).astype(int)
        t = xs - x0
        t = t * t * (3 - 2 * t)
        x1 = (x0 + 1) % g
        a = grid[np.ix_(x0, x0)]
        b = grid[np.ix_(x0, x1)]
        c = grid[np.ix_(x1, x0)]
        d = grid[np.ix_(x1, x1)]
        ty, tx = np.meshgrid(t, t, indexing="ij")
        out += amp * (a * (1 - tx) * (1 - ty) + b * tx * (1 - ty) + c * (1 - tx) * ty + d * tx * ty)
        tot += amp
        amp *= 0.5
    return out / tot


def ramp(v, stops):
    """map 0..1 through colour stops [(t, '#rrggbb')]"""
    stops = [(t, np.array(rgb(c), float)) for t, c in stops]
    out = np.zeros(v.shape + (3,))
    for i in range(len(stops) - 1):
        t0, c0 = stops[i]
        t1, c1 = stops[i + 1]
        m = (v >= t0) & (v <= t1)
        k = ((v - t0) / (t1 - t0))[..., None]
        out[m] = (c0 + (c1 - c0) * k)[m]
    out[v < stops[0][0]] = stops[0][1]
    out[v > stops[-1][0]] = stops[-1][1]
    return out


def img(arr, alpha=None):
    a = np.clip(arr, 0, 255).astype(np.uint8)
    if alpha is None:
        return Image.fromarray(a, "RGB").convert("RGBA")
    return Image.fromarray(np.dstack([a, np.clip(alpha, 0, 255).astype(np.uint8)]), "RGBA")


# ============================================================================ the blood
def blood_frames(n, frames, seed, flow=False):
    out = Image.new("RGBA", (n, n * frames))
    rng = np.random.default_rng(seed)
    base = noise(n, seed, 2, 2)
    base2 = noise(n, seed + 1, 3, 2)
    for f in range(frames):
        ph = f / frames
        if flow:
            sh = int(ph * n)
            v = np.roll(base, sh, axis=0) * 0.6 + np.roll(base2, sh, axis=0) * 0.4
        else:
            v = base * (0.5 + 0.5 * math.cos(ph * math.tau)) + base2 * (0.5 - 0.5 * math.cos(ph * math.tau))
            v = np.roll(v, int(ph * 3), axis=1)
        v = 0.5 + (v - 0.5) * 0.55
        col = ramp(v, [(0.0, "#3a0005"), (0.45, "#5e0309"), (0.62, "#74060e"), (1.0, "#8e1018")])
        # the sheen of something thick: a few slow highlights, never noise
        hi = v > 0.7
        col[hi] = col[hi] * 0.75 + np.array([170, 40, 46]) * 0.25
        frame = img(col, np.full((n, n), 236))
        out.paste(frame, (0, n * f))
    return out


def gen_blood():
    save(blood_frames(16, 32, 11), "block/blood_still.png", 3)
    save(blood_frames(32, 32, 12, flow=True), "block/blood_flow.png", 2)
    ov = blood_frames(16, 1, 13)
    a = np.array(ov)
    a[..., 3] = 150
    save(Image.fromarray(a), "block/blood_overlay.png")
    # under the surface: a red murk with darker edges
    n = 256
    yy, xx = np.mgrid[0:n, 0:n]
    d = np.sqrt((xx - n / 2) ** 2 + (yy - n / 2) ** 2) / (n / 2)
    col = ramp(noise(n, 14, 6, 3) * 0.5 + 0.25, [(0, "#1a0000"), (1, "#5a0004")])
    save(img(col, 150 + 90 * np.clip(d, 0, 1)), "misc/under_blood.png")


# ============================================================================ bones and flesh
BONE = ["#5a4a3a", "#a8987c", "#d8ccb0", "#efe6d2"]


def human_bones(seed=21):
    n = 16
    rng = random.Random(seed)
    im = Image.fromarray(ramp(noise(n, seed, 3), [(0, "#1a0c08"), (1, "#3a1e16")]).astype(np.uint8)).convert("RGBA")
    d = ImageDraw.Draw(im)
    # long bones every which way, knuckled at both ends
    for _ in range(9):
        x, y = rng.uniform(-2, 16), rng.uniform(-2, 16)
        a = rng.uniform(0, math.pi)
        L = rng.uniform(5, 10)
        x2, y2 = x + math.cos(a) * L, y + math.sin(a) * L
        shade = rng.choice(BONE[1:])
        for ox in (-16, 0, 16):
            for oy in (-16, 0, 16):
                d.line([(x + ox, y + oy), (x2 + ox, y2 + oy)], fill=shade, width=2)
                for ex, ey in ((x, y), (x2, y2)):
                    d.ellipse([ex + ox - 1.4, ey + oy - 1.4, ex + ox + 1.4, ey + oy + 1.4], fill=BONE[3])
    # a small skull showing between them
    sx, sy = rng.randint(2, 10), rng.randint(2, 10)
    d.ellipse([sx, sy, sx + 5, sy + 5], fill=BONE[2])
    d.point([(sx + 1, sy + 2), (sx + 3, sy + 2)], fill="#140604")
    d.line([(sx + 1, sy + 4), (sx + 4, sy + 4)], fill="#5a4a3a")
    return im


def skull_tile(lit, seed):
    n = 16
    im = Image.fromarray(ramp(noise(n, seed, 3), [(0, "#120806"), (1, "#2a1410")]).astype(np.uint8)).convert("RGBA")
    d = ImageDraw.Draw(im)
    rng = random.Random(seed)
    for cx, cy in ((4, 4), (12, 4), (0, 12), (8, 12), (16, 12)):
        cx += rng.randint(-1, 1)
        cy += rng.randint(-1, 1)
        d.ellipse([cx - 3.4, cy - 3.6, cx + 3.4, cy + 2.6], fill=rng.choice([BONE[2], BONE[3], BONE[2]]))
        d.rectangle([cx - 2, cy + 1, cx + 2, cy + 3], fill=BONE[1])
        eye = "#ff2a14" if lit else "#0e0403"
        d.rectangle([cx - 2, cy - 1, cx - 1, cy], fill=eye)
        d.rectangle([cx + 1, cy - 1, cx + 2, cy], fill=eye)
        d.point([(cx, cy + 1)], fill="#2a1410")
        for tx in (-1, 0, 1):
            d.point([(cx + tx, cy + 3)], fill="#3a2a1e")
    return im


def rib_side():
    n = 16
    x = np.linspace(0, 1, n)
    prof = 0.35 + 0.65 * np.sin(x * math.pi) ** 0.6
    v = np.tile(prof, (n, 1)) * (0.8 + 0.2 * noise(n, 31, 4))
    return img(ramp(v, [(0, "#6a5a46"), (0.5, "#b8a888"), (1, "#ece2ca")]))


def rib_top():
    n = 16
    yy, xx = np.mgrid[0:n, 0:n]
    r = np.sqrt((xx - 7.5) ** 2 + (yy - 7.5) ** 2)
    col = ramp(np.clip(1 - r / 9, 0, 1) * (0.8 + 0.2 * noise(n, 32, 4)), [(0, "#6a5a46"), (0.5, "#d8ccb0"), (1, "#efe6d2")])
    m = r < 3.2
    col[m] = ramp(noise(n, 33, 6), [(0, "#5a0a08"), (1, "#9a3020")])[m]
    return img(col)


def vertebra():
    im = Image.fromarray(ramp(noise(16, 41, 3), [(0, "#7a6a52"), (1, "#c8b898")]).astype(np.uint8)).convert("RGBA")
    d = ImageDraw.Draw(im)
    d.ellipse([3, 4, 12, 12], outline="#efe6d2", width=2)
    d.ellipse([6, 6, 9, 9], fill="#4a0a08")
    d.polygon([(7, 0), (9, 0), (8, 4)], fill="#e0d4bc")
    d.line([(0, 7), (3, 8)], fill="#e0d4bc", width=2)
    d.line([(16, 7), (13, 8)], fill="#e0d4bc", width=2)
    return im


def ivory(seed, dark=0.0):
    n = 16
    v = noise(n, seed, 2, 4)
    col = ramp(v, [(0, "#8a7656"), (0.5, "#c8b48c"), (1, "#e8dcc0")])
    col = col * (1 - dark)
    im = img(col)
    d = ImageDraw.Draw(im)
    rng = random.Random(seed)
    for _ in range(3):  # cracks
        x, y = rng.randint(0, 15), rng.randint(0, 15)
        for _ in range(5):
            nx, ny = x + rng.randint(-2, 2), y + rng.randint(1, 3)
            d.line([(x, y), (nx, ny)], fill="#5a4630")
            x, y = nx, ny
    for _ in range(10):  # pores
        d.point([(rng.randint(0, 15), rng.randint(0, 15))], fill="#7a6648")
    return im


def ivory_top(seed):
    n = 16
    yy, xx = np.mgrid[0:n, 0:n]
    r = np.sqrt((xx - 7.5) ** 2 + (yy - 7.5) ** 2)
    rings = (np.sin(r * 1.6) * 0.5 + 0.5) * 0.4 + noise(n, seed, 4) * 0.6
    col = ramp(rings, [(0, "#8a7656"), (1, "#e8dcc0")])
    col[r < 2.6] = rgb("#3a0806")
    return img(col)


def tooth_side():
    n = 16
    y = np.linspace(0, 1, n)[:, None]
    v = np.clip(0.95 - y * 0.35 + (noise(n, 51, 3) - 0.5) * 0.3, 0, 1)
    col = ramp(v, [(0, "#6a5a3a"), (0.5, "#c8b890"), (1, "#f4ecd8")])
    return img(col)


def bone_mud():
    """wet black mud with rotten leaves and splinters of bone in it, a sheen of standing water"""
    n = 32
    v = noise(n, 171, 3, 4)
    col = ramp(v, [(0, "#14100e"), (0.5, "#2a221e"), (1, "#40342c")])
    rng = np.random.default_rng(172)
    leaf = (noise(n, 173, 6, 2) > 0.66) & (rng.random((n, n)) < 0.7)
    col[leaf] = ramp(noise(n, 174, 8, 2), [(0, "#4a2a14"), (1, "#7a4a24")])[leaf]
    wet = noise(n, 175, 2, 2) > 0.7
    col[wet] = col[wet] * 0.6 + np.array([70, 60, 58]) * 0.4
    im = img(col)
    d = ImageDraw.Draw(im)
    rr = random.Random(176)
    for _ in range(7):  # splinters of bone
        x, y = rr.uniform(0, 32), rr.uniform(0, 32)
        a = rr.uniform(0, math.pi)
        L = rr.uniform(2, 5)
        d.line([(x, y), (x + math.cos(a) * L, y + math.sin(a) * L)], fill=rr.choice(["#bfb090", "#d8ccb0", "#8a7a5a"]), width=1)
    return im


def flesh():
    n = 16
    v = noise(n, 61, 2, 3)
    fib = (np.sin(np.linspace(0, 6 * math.pi, n))[None, :] * 0.5 + 0.5) * 0.35
    col = ramp(np.clip(v * 0.65 + fib, 0, 1), [(0, "#3a0406"), (0.5, "#7a1010"), (0.85, "#a8282a"), (1, "#c86058")])
    rng = np.random.default_rng(62)
    fat = noise(n, 63, 5) > 0.78
    col[fat] = rgb("#d8b8a0")
    return img(col)


def clot():
    n = 16
    v = noise(n, 71, 3, 4)
    col = ramp(v, [(0, "#0c0000"), (0.6, "#2a0204"), (1, "#4a0608")])
    rng = np.random.default_rng(72)
    shine = rng.random((n, n)) < 0.04
    col[shine] = rgb("#8a2a2a")
    return img(col)


def edge_stone():
    n = 16
    v = noise(n, 81, 3, 4)
    col = ramp(v, [(0, "#060506"), (1, "#1e1a1c")])
    vein = np.abs(noise(n, 82, 2, 3) - 0.5) < 0.04
    col[vein] = rgb("#7a0a0c")
    return img(col)


def sand(seed, base=("#c8b078", "#e8d8a8")):
    v = noise(16, seed, 4, 3) + (np.random.default_rng(seed).random((16, 16)) - 0.5) * 0.3
    return img(ramp(np.clip(v, 0, 1), [(0, base[0]), (1, base[1])]))


def grass_top(seed):
    v = noise(16, seed, 4, 3) + (np.random.default_rng(seed).random((16, 16)) - 0.5) * 0.4
    return img(ramp(np.clip(v, 0, 1), [(0, "#4a7a2a"), (1, "#8ab84a")]))


def grass_side(seed):
    im = sand(seed + 1, ("#8a6a42", "#a88a5a"))
    top = grass_top(seed)
    a = np.array(im)
    t = np.array(top)
    rng = np.random.default_rng(seed)
    for x in range(16):
        h = 3 + rng.integers(0, 3)
        a[:h, x] = t[:h, x]
    return Image.fromarray(a)


def log_side():
    n = 16
    x = np.linspace(0, 6 * math.pi, n)
    v = (np.sin(x)[None, :] * 0.5 + 0.5) * 0.5 + noise(n, 91, 4) * 0.5
    return img(ramp(v, [(0, "#5a4430"), (1, "#a8865a")]))


def log_top():
    return ivory_top(92).convert("RGBA")


def leaves():
    v = noise(16, 101, 5, 3)
    col = ramp(v, [(0, "#2a5a1a"), (1, "#6aa83a")])
    alpha = np.where(np.random.default_rng(101).random((16, 16)) < 0.2, 0, 255)
    return img(col, alpha)


def lighthouse():
    col = np.zeros((16, 16, 3))
    col[:8] = rgb("#ece8e0")
    col[8:] = rgb("#a81818")
    col *= (0.9 + 0.1 * noise(16, 111, 4))[..., None]
    return img(col)


def lamp():
    v = noise(16, 121, 3)
    im = img(ramp(v, [(0, "#f8c860"), (1, "#fff4c0")]))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], outline="#6a4a20")
    d.line([(8, 0), (8, 15)], fill="#6a4a20")
    return im


def door():
    im = img(ramp(noise(16, 131, 3), [(0, "#4a3020"), (1, "#7a5434")]))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], outline="#2a1a10")
    for y in (5, 10):
        d.line([(1, y), (14, y)], fill="#3a2414")
    d.rectangle([4, 1, 11, 4], fill="#f8d880")      # a lit window: somebody is home
    d.point([(12, 8)], fill="#d8b040")
    return im


def heart_frames():
    out = Image.new("RGBA", (16, 32))
    for f, k in enumerate((1.0, 1.12)):
        im = img(ramp(noise(16, 141, 3), [(0, "#3a0004"), (1, "#7a0a10")]))
        d = ImageDraw.Draw(im)
        c = 8
        s = 5 * k
        d.ellipse([c - s, c - s * 0.9, c, c + s * 0.2], fill="#b01418")
        d.ellipse([c, c - s * 0.9, c + s, c + s * 0.2], fill="#b01418")
        d.polygon([(c - s, c - s * 0.1), (c + s, c - s * 0.1), (c, c + s * 1.1)], fill="#b01418")
        d.line([(c - 2, c - 3), (c + 1, c + 2)], fill="#5a0006")
        d.line([(c + 2, c - 4), (c, c)], fill="#e04040")
        out.paste(im, (0, 16 * f))
    return out


def maw_frames():
    n, frames = 16, 16
    out = Image.new("RGBA", (n, n * frames))
    yy, xx = np.mgrid[0:n, 0:n]
    r = np.sqrt((xx - 7.5) ** 2 + (yy - 7.5) ** 2)
    a = np.arctan2(yy - 7.5, xx - 7.5)
    for f in range(frames):
        v = np.sin(a * 3 + r * 0.9 - f / frames * math.tau) * 0.5 + 0.5
        v = v * np.clip(r / 9, 0, 1)
        col = ramp(v, [(0, "#000000"), (0.6, "#160204"), (1, "#4a0a0c")])
        out.paste(img(col), (0, n * f))
    return out


def phantom():
    im = Image.new("RGBA", (16, 16), (255, 255, 255, 0))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], outline=(255, 255, 255, 90))
    return im


def bucket():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.polygon([(3, 5), (12, 5), (11, 14), (4, 14)], fill="#8a8a8e", outline="#4a4a4e")
    d.ellipse([3, 3, 12, 7], fill="#6a0008", outline="#4a4a4e")
    d.point([(6, 4), (8, 5)], fill="#b02028")
    d.arc([2, 0, 13, 9], 180, 360, fill="#3a3a3e")
    return im


# ============================================================================ the sky
def pale_sun():
    n = 64
    yy, xx = np.mgrid[0:n, 0:n]
    r = np.sqrt((xx - 31.5) ** 2 + (yy - 31.5) ** 2) / 32
    a = np.clip(1.25 - r * 1.25, 0, 1) ** 0.5
    disc = r < 0.62
    col = np.full((n, n, 3), 255.0)
    col[..., 2] = 250
    alpha = np.where(disc, 255, a * 140)
    # a faint ring inside: an iris that is not quite looking at you
    ring = (r > 0.36) & (r < 0.42)
    col[ring] = [232, 228, 226]
    return img(col, alpha)


def blood_moon():
    n = 64
    yy, xx = np.mgrid[0:n, 0:n]
    r = np.sqrt((xx - 31.5) ** 2 + (yy - 31.5) ** 2) / 32
    v = noise(n, 151, 3, 4)
    col = ramp(v, [(0, "#3a0004"), (0.5, "#8a0a0c"), (1, "#c8282a")])
    shade = np.clip(1.1 - ((xx - 24) ** 2 + (yy - 24) ** 2) ** 0.5 / 60, 0.5, 1.1)
    col *= shade[..., None]
    rng = random.Random(152)
    im = img(col, np.where(r < 0.9, 255, 0))
    d = ImageDraw.Draw(im)
    for _ in range(7):
        x, y, s = rng.uniform(14, 50), rng.uniform(14, 50), rng.uniform(2, 6)
        if ((x - 31.5) ** 2 + (y - 31.5) ** 2) ** 0.5 + s > 28:
            continue
        d.ellipse([x - s, y - s, x + s, y + s], fill=(96, 4, 8, 255))
    return im.filter(ImageFilter.GaussianBlur(0.8)).convert("RGBA") if False else im


def glow():
    n = 128
    yy, xx = np.mgrid[0:n, 0:n]
    r = np.sqrt((xx - 63.5) ** 2 + (yy - 63.5) ** 2) / 64
    a = np.clip(1 - r, 0, 1) ** 2.2 * 255
    return img(np.full((n, n, 3), 255.0), a)


# ============================================================================ models, states, items
BLOCK_ITEMS = []


def cube_all(name, tex=None):
    W(f"assets/{NS}/models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{tex or name}"}})
    W(f"assets/{NS}/blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
    BLOCK_ITEMS.append((name, name))


def pillar(name):
    W(f"assets/{NS}/models/block/{name}.json", {"parent": "minecraft:block/cube_column",
                                                "textures": {"side": f"{NS}:block/{name}", "end": f"{NS}:block/{name}_top"}})
    W(f"assets/{NS}/models/block/{name}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal",
                                                           "textures": {"side": f"{NS}:block/{name}", "end": f"{NS}:block/{name}_top"}})
    W(f"assets/{NS}/blockstates/{name}.json", {"variants": {
        "axis=y": {"model": f"{NS}:block/{name}"},
        "axis=z": {"model": f"{NS}:block/{name}_horizontal", "x": 90},
        "axis=x": {"model": f"{NS}:block/{name}_horizontal", "x": 90, "y": 90}}})
    BLOCK_ITEMS.append((name, name))


def gen_models():
    for n in ("bone_mud", "flesh", "clot", "edge_stone", "mirage_sand", "lighthouse_wall", "lighthouse_lamp", "home_door", "maw_core"):
        cube_all(n)
    W(f"assets/{NS}/models/block/mirage_leaves.json", {"parent": "minecraft:block/leaves", "textures": {"all": f"{NS}:block/mirage_leaves"}})
    W(f"assets/{NS}/blockstates/mirage_leaves.json", {"variants": {"": {"model": f"{NS}:block/mirage_leaves"}}})
    BLOCK_ITEMS.append(("mirage_leaves", "mirage_leaves"))
    W(f"assets/{NS}/models/block/mirage_grass.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": f"{NS}:block/mirage_grass_top", "side": f"{NS}:block/mirage_grass_side", "bottom": f"{NS}:block/mirage_sand"}})
    W(f"assets/{NS}/blockstates/mirage_grass.json", {"variants": {"": {"model": f"{NS}:block/mirage_grass"}}})
    BLOCK_ITEMS.append(("mirage_grass", "mirage_grass"))
    W(f"assets/{NS}/models/block/mirage_log.json", {"parent": "minecraft:block/cube_column", "textures": {"side": f"{NS}:block/mirage_log", "end": f"{NS}:block/mirage_log_top"}})
    W(f"assets/{NS}/blockstates/mirage_log.json", {"variants": {"": {"model": f"{NS}:block/mirage_log"}}})
    BLOCK_ITEMS.append(("mirage_log", "mirage_log"))

    W(f"assets/{NS}/models/block/phantom_wall.json", {"textures": {"particle": f"{NS}:block/phantom_wall"}})
    W(f"assets/{NS}/blockstates/phantom_wall.json", {"variants": {"": {"model": f"{NS}:block/phantom_wall"}}})
    W(f"assets/{NS}/models/item/phantom_wall.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/phantom_wall"}})
    for i in range(1, 9):
        W(f"assets/{NS}/models/block/mud_layer_{i}.json", {"parent": "minecraft:block/thin_block" if False else "minecraft:block/block", "textures": {"t": f"{NS}:block/bone_mud", "particle": f"{NS}:block/bone_mud"},
                                                         "elements": [{"from": [0, 0, 0], "to": [16, i * 2, 16], "faces": {
                                                             f: {"texture": "#t", **({"cullface": f} if f != "up" or i == 8 else {}), "uv": [0, 16 - i * 2, 16, 16] if f not in ("up", "down") else [0, 0, 16, 16]}
                                                             for f in ("north", "south", "east", "west", "up", "down")}}]})
    W(f"assets/{NS}/blockstates/mud_layer.json", {"variants": {f"layers={i}": {"model": f"{NS}:block/mud_layer_{i}"} for i in range(1, 9)}})
    BLOCK_ITEMS.append(("mud_layer", "mud_layer_2"))
    W(f"assets/{NS}/models/block/bone_collider.json", {"textures": {"particle": f"{NS}:block/bone_mud"}})
    W(f"assets/{NS}/blockstates/bone_collider.json", {"variants": {"": {"model": f"{NS}:block/bone_collider"}}})
    W(f"assets/{NS}/models/block/blood.json", {"textures": {"particle": f"{NS}:block/blood_still"}})
    W(f"assets/{NS}/blockstates/blood.json", {"variants": {"": {"model": f"{NS}:block/blood"}}})
    W(f"assets/{NS}/models/item/blood_bucket.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/blood_bucket"}})
    for name, model in BLOCK_ITEMS:
        W(f"assets/{NS}/models/item/{name}.json", {"parent": f"{NS}:block/{model}"})


# ============================================================================ the dimension
BIOMES = {
    # sky, fog, particle
    "drift": ("#7a1414", "#4a0a0a", ("minecraft:crimson_spore", 0.004)),
    "bones": ("#6a1c14", "#3e1410", ("minecraft:ash", 0.006)),
    "leviathan": ("#3a0808", "#1e0404", None),
    "edge": ("#c8b4b0", "#e8dcd8", ("minecraft:white_ash", 0.01)),
    "mirage": ("#a84a5a", "#9a5a68", ("minecraft:white_ash", 0.003)),
}


def hexint(c):
    return int(c[1:], 16)


def gen_data():
    for name, (sky, fog, part) in BIOMES.items():
        eff = {"sky_color": hexint(sky), "fog_color": hexint(fog), "water_color": 0x8a0008, "water_fog_color": 0x300004,
               "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 3000, "block_search_extent": 8, "offset": 2.0},
               "music": {"sound": "minecraft:ambient.basalt_deltas.mood", "min_delay": 3000, "max_delay": 9000, "replace_current_music": True}}
        if part:
            eff["particle"] = {"options": {"type": part[0]}, "probability": part[1]}
        W(f"data/{NS}/worldgen/biome/{name}.json", {"has_precipitation": False, "temperature": 0.9, "downfall": 0.0, "effects": eff,
                                                    "spawners": {}, "spawn_costs": {}, "carvers": {}, "features": []})
    W(f"data/{NS}/dimension_type/blood_sea.json", {
        "ultrawarm": False, "natural": True, "piglin_safe": False, "respawn_anchor_works": False, "bed_works": True, "has_raids": False,
        "has_skylight": True, "has_ceiling": False, "coordinate_scale": 1.0, "ambient_light": 0.06, "logical_height": 384,
        "effects": f"{NS}:blood_sea", "infiniburn": "#minecraft:infiniburn_overworld", "min_y": -64, "height": 384,
        "monster_spawn_light_level": 0, "monster_spawn_block_light_limit": 0})
    W(f"data/{NS}/dimension/blood_sea.json", {"type": f"{NS}:blood_sea", "generator": {
        "type": f"{NS}:blood_sea", "biome_source": {"type": f"{NS}:regions", "drift": f"{NS}:drift", "bones": f"{NS}:bones",
                                                     "leviathan": f"{NS}:leviathan", "edge": f"{NS}:edge", "mirage": f"{NS}:mirage"}}})


# ============================================================================ text
EN = {
    "itemGroup.bloodsea.main": "Blood Sea",
    "block.bloodsea.blood": "Blood", "fluid_type.bloodsea.blood": "Blood", "item.bloodsea.blood_bucket": "Bucket of Blood",
    
    "block.bloodsea.clot": "Clotted Blood", "block.bloodsea.bone_mud": "Ossuary Mud", "block.bloodsea.mud_layer": "Ossuary Mud Layer", "block.bloodsea.bone_collider": "Giant Bone", "block.bloodsea.flesh": "Flesh",
    "block.bloodsea.edge_stone": "Edge Stone", 
    "block.bloodsea.maw_core": "The Throat", "block.bloodsea.mirage_sand": "Mirage Sand",
    "block.bloodsea.mirage_grass": "Mirage Grass", "block.bloodsea.mirage_log": "Mirage Log", "block.bloodsea.mirage_leaves": "Mirage Leaves",
    "block.bloodsea.phantom_wall": "Phantom Wall", "block.bloodsea.lighthouse_wall": "Lighthouse Wall", "block.bloodsea.lighthouse_lamp": "Lighthouse Lamp",
    "block.bloodsea.home_door": "The Door Home",
    "block.bloodsea.skull": "Skull", "block.bloodsea.skull_side": "Fallen Skull", "block.bloodsea.skull_sunk": "Drowned Skull", "block.bloodsea.skull_heap": "Heap of Skulls", "block.bloodsea.ribs": "Ribs in the Mud", "block.bloodsea.ribs_sunk": "Drowned Ribs", "block.bloodsea.ribcage": "Rib Cage", "block.bloodsea.skeleton": "Skeleton", "block.bloodsea.skeleton_sunk": "Drowned Skeleton", "block.bloodsea.hand": "Reaching Hand", "block.bloodsea.bone_pile": "Bone Pile", "block.bloodsea.spine": "Spine", "block.bloodsea.heart": "The Heart on the Throne", "entity.bloodsea.giant": "Giant Bones",

    "region.bloodsea.drift": "THE DRIFT", "region.bloodsea.drift.sub": "Calm blood, in every direction",
    "region.bloodsea.bones": "THE BONE REACH", "region.bloodsea.bones.sub": "Everyone the sea ever took is here",
    "region.bloodsea.leviathan": "LEVIATHAN'S DEEP", "region.bloodsea.leviathan.sub": "Something below is as large as the dark",
    "region.bloodsea.edge": "THE EDGE OF THE WORLD", "region.bloodsea.edge.sub": "The sea ends. It does not stop.",
    "region.bloodsea.mirage": "THE MIRAGE", "region.bloodsea.mirage.sub": "Not everything you see is there",

    "sky.bloodsea.sun": "THE PALE SUN", "sky.bloodsea.sun.sub": "It sees everything. Lies show in its light. Do not look back at it.",
    "sky.bloodsea.moon": "THE BLOOD MOON", "sky.bloodsea.moon.sub": "The sea is hungry. It will feed you, if you go into it.",
    "msg.bloodsea.stare": "The Pale Sun looked back.",

    "ending.bloodsea.label": "Ending %s of %s",
    "ending.bloodsea.skip": "ESC",
    "ending.bloodsea.bone.title": "The Ossuary Throne",
    "ending.bloodsea.bone.0": "You touched the heart, and it was still warm.",
    "ending.bloodsea.bone.1": "The skulls turned, all of them at once, to look at their new king.",
    "ending.bloodsea.bone.2": "The sea keeps everything it takes.",
    "ending.bloodsea.bone.3": "Now it keeps you - on a throne made of everyone who came before.",
    "ending.bloodsea.leviathan.title": "The Maw",
    "ending.bloodsea.leviathan.0": "You sailed between the teeth, into the black.",
    "ending.bloodsea.leviathan.1": "It did not chew. It did not need to.",
    "ending.bloodsea.leviathan.2": "Somewhere far below, something older than the sea opened one eye.",
    "ending.bloodsea.leviathan.3": "It did not eat you. It remembered you.",
    "ending.bloodsea.edge.title": "Beyond the Edge",
    "ending.bloodsea.edge.0": "The blood went over the edge, and you went with it.",
    "ending.bloodsea.edge.1": "You fell for a long time. Then you were still falling.",
    "ending.bloodsea.edge.2": "Under the Pale Sun, the whole sea pours out into nothing.",
    "ending.bloodsea.edge.3": "There is no bottom. There was never a bottom.",
    "ending.bloodsea.mirage.title": "Home",
    "ending.bloodsea.mirage.0": "The door opened onto your own kitchen. The kettle was on.",
    "ending.bloodsea.mirage.1": "Everyone you had lost was sitting at the table, smiling.",
    "ending.bloodsea.mirage.2": "Nobody asked where you had been. Nobody blinked.",
    "ending.bloodsea.mirage.3": "Outside the window, the sea never stopped.",
}

TR = {
    "itemGroup.bloodsea.main": "Kan Denizi",
    "block.bloodsea.blood": "Kan", "fluid_type.bloodsea.blood": "Kan", "item.bloodsea.blood_bucket": "Kan Kovası",
    
    "block.bloodsea.clot": "Pıhtılaşmış Kan", "block.bloodsea.bone_mud": "Kemik Çamuru", "block.bloodsea.mud_layer": "Kemik Çamuru Katmanı", "block.bloodsea.bone_collider": "Dev Kemik", "block.bloodsea.flesh": "Et",
    "block.bloodsea.edge_stone": "Kıyı Taşı", 
    "block.bloodsea.maw_core": "Gırtlak", "block.bloodsea.mirage_sand": "Serap Kumu",
    "block.bloodsea.mirage_grass": "Serap Çimi", "block.bloodsea.mirage_log": "Serap Kütüğü", "block.bloodsea.mirage_leaves": "Serap Yaprakları",
    "block.bloodsea.phantom_wall": "Hayalet Duvar", "block.bloodsea.lighthouse_wall": "Deniz Feneri Duvarı", "block.bloodsea.lighthouse_lamp": "Deniz Feneri Lambası",
    "block.bloodsea.home_door": "Eve Açılan Kapı",
    "block.bloodsea.skull": "Kafatası", "block.bloodsea.skull_side": "Devrik Kafatası", "block.bloodsea.skull_sunk": "Boğulmuş Kafatası", "block.bloodsea.skull_heap": "Kafatası Yığını", "block.bloodsea.ribs": "Çamurdaki Kaburgalar", "block.bloodsea.ribs_sunk": "Boğulmuş Kaburgalar", "block.bloodsea.ribcage": "Göğüs Kafesi", "block.bloodsea.skeleton": "İskelet", "block.bloodsea.skeleton_sunk": "Boğulmuş İskelet", "block.bloodsea.hand": "Uzanan El", "block.bloodsea.bone_pile": "Kemik Yığını", "block.bloodsea.spine": "Omurga", "block.bloodsea.heart": "Tahttaki Kalp", "entity.bloodsea.giant": "Dev Kemikler",

    "region.bloodsea.drift": "SÜRÜKLENME", "region.bloodsea.drift.sub": "Her yönde durgun kan",
    "region.bloodsea.bones": "KEMİK DİYARI", "region.bloodsea.bones.sub": "Denizin aldığı herkes burada",
    "region.bloodsea.leviathan": "LEVİATHAN ÇUKURU", "region.bloodsea.leviathan.sub": "Aşağıdaki şey karanlık kadar büyük",
    "region.bloodsea.edge": "DÜNYANIN SONU", "region.bloodsea.edge.sub": "Deniz biter. Akmaya devam eder.",
    "region.bloodsea.mirage": "SERAP", "region.bloodsea.mirage.sub": "Gördüğün her şey orada değil",

    "sky.bloodsea.sun": "SOLGUN GÜNEŞ", "sky.bloodsea.sun.sub": "Her şeyi görür. Yalanlar onun ışığında belli olur. Ona geri bakma.",
    "sky.bloodsea.moon": "KAN AYI", "sky.bloodsea.moon.sub": "Deniz aç. İçine girersen seni besler.",
    "msg.bloodsea.stare": "Solgun Güneş sana geri baktı.",

    "ending.bloodsea.label": "Son %s / %s",
    "ending.bloodsea.skip": "ESC",
    "ending.bloodsea.bone.title": "Kemik Tahtı",
    "ending.bloodsea.bone.0": "Kalbe dokundun; hâlâ sıcaktı.",
    "ending.bloodsea.bone.1": "Kafataslarının hepsi aynı anda, yeni krallarına bakmak için döndü.",
    "ending.bloodsea.bone.2": "Deniz aldığı her şeyi saklar.",
    "ending.bloodsea.bone.3": "Artık seni saklıyor - senden önce gelen herkesten yapılmış bir tahtta.",
    "ending.bloodsea.leviathan.title": "Ağız",
    "ending.bloodsea.leviathan.0": "Dişlerin arasından karanlığa doğru yelken açtın.",
    "ending.bloodsea.leviathan.1": "Çiğnemedi. Gerek yoktu.",
    "ending.bloodsea.leviathan.2": "Çok aşağıda, denizden de yaşlı bir şey tek gözünü açtı.",
    "ending.bloodsea.leviathan.3": "Seni yemedi. Seni hatırladı.",
    "ending.bloodsea.edge.title": "Kenarın Ötesi",
    "ending.bloodsea.edge.0": "Kan kenardan aşağı aktı, sen de onunla gittin.",
    "ending.bloodsea.edge.1": "Uzun süre düştün. Sonra hâlâ düşüyordun.",
    "ending.bloodsea.edge.2": "Solgun Güneş'in altında bütün deniz hiçliğe dökülüyor.",
    "ending.bloodsea.edge.3": "Dibi yok. Hiç olmadı.",
    "ending.bloodsea.mirage.title": "Ev",
    "ending.bloodsea.mirage.0": "Kapı kendi mutfağına açıldı. Çaydanlık ocaktaydı.",
    "ending.bloodsea.mirage.1": "Kaybettiğin herkes masada oturmuş, gülümsüyordu.",
    "ending.bloodsea.mirage.2": "Kimse nerede olduğunu sormadı. Kimse gözünü kırpmadı.",
    "ending.bloodsea.mirage.3": "Pencerenin dışında deniz hiç durmadı.",
}


def gen_lang():
    """written over what is there, keeping keys other tools added (the creatures' names and subtitles)"""
    missing = [k for k in EN if k not in TR]
    assert not missing, missing
    for f, d in (("en_us", EN), ("tr_tr", TR)):
        p = os.path.join(R, f"assets/{NS}/lang/{f}.json")
        old = json.load(open(p, encoding="utf-8")) if os.path.exists(p) else {}
        old.update(d)
        W(f"assets/{NS}/lang/{f}.json", old)


def gen_textures():
    gen_blood()
    save(bone_mud(), "block/bone_mud.png")
    save(flesh(), "block/flesh.png")
    save(clot(), "block/clot.png")
    save(edge_stone(), "block/edge_stone.png")
    save(sand(161), "block/mirage_sand.png")
    save(grass_top(162), "block/mirage_grass_top.png")
    save(grass_side(162), "block/mirage_grass_side.png")
    save(log_side(), "block/mirage_log.png")
    save(log_top(), "block/mirage_log_top.png")
    save(leaves(), "block/mirage_leaves.png")
    save(lighthouse(), "block/lighthouse_wall.png")
    save(lamp(), "block/lighthouse_lamp.png")
    save(door(), "block/home_door.png")
    save(maw_frames(), "block/maw_core.png", 2)
    save(phantom(), "block/phantom_wall.png")
    save(bucket(), "item/blood_bucket.png")
    save(pale_sun(), "environment/pale_sun.png")
    save(blood_moon(), "environment/blood_moon.png")
    save(glow(), "environment/glow.png")


if __name__ == "__main__":
    gen_textures()
    gen_models()
    gen_data()
    gen_lang()
    print("assets ok:", len(BLOCK_ITEMS), "block items")
