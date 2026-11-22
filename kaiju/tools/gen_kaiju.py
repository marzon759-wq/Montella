#!/usr/bin/env python3
"""KAIJU: geometry and textures (GeckoLib / bedrock geometry, Minecraft 1.20.1).

A charcoal-black, heavy-legged, upright kaiju: small head with an overhanging brow, thick neck, barrel chest,
short clawed arms, column legs with elephant feet, and a very long tail that curls up at the tip. Three rows of
jagged, frost-like dorsal plates run from the neck to the tail tip. The skin is a voxel crust of lumps.

Everything is axis-aligned stepped boxes and small protruding cubes, so it reads as Minecraft, not as a smooth mesh.
Model units: 16 px = 1 block. Front = -z. Ground = y 0. The model is about 7 blocks tall; the renderer scales it up.

Outputs (out/):
  kaiju.geo.json
  kaiju.png               charcoal skin
  kaiju_glowmask.png      atomic charge: dorsal plates and mouth glow cyan
  kaiju_burning.png       molten variant: orange cracks over chest, neck, arms and thighs
  kaiju_burning_glowmask.png
"""
import json, math, os, random, sys, zlib
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "out")
TEX = 512
S = 2  # texels per model pixel (pixel-art density)
R = random.Random(1954)


def shash(v):
    return zlib.crc32(str(v).encode())


# ====================================================================== textures (pixel art)
def voronoi(w, h, cell, seed, aniso=(1.0, 1.0)):
    """distance to nearest/second nearest seed and the offset to the nearest one; cheap brute force"""
    rng = np.random.default_rng(seed)
    n = int(w * h / (cell * cell)) + 8
    pts = rng.random((n, 2)) * [w, h]
    # wrap so patches tile
    pts = np.concatenate([pts + [dx, dy] for dx in (-w, 0, w) for dy in (-h, 0, h)])
    yy, xx = np.mgrid[0:h, 0:w]
    P = np.stack([xx, yy], -1).reshape(-1, 1, 2).astype(float)
    best = np.full((h * w,), np.inf)
    second = np.full((h * w,), np.inf)
    off = np.zeros((h * w, 2))
    for chunk in np.array_split(pts, max(1, len(pts) // 256)):
        d = (P - chunk[None]) * [1 / aniso[0], 1 / aniso[1]]
        dist = np.hypot(d[..., 0], d[..., 1])
        i = dist.argmin(1)
        m = dist[np.arange(len(i)), i]
        # update second best from this chunk too
        dist2 = dist.copy()
        dist2[np.arange(len(i)), i] = np.inf
        m2 = dist2.min(1)
        newbest = m < best
        second = np.where(newbest, np.minimum(best, m2), np.minimum(second, m))
        off = np.where(newbest[:, None], (P[:, 0] - chunk[i]), off)
        best = np.where(newbest, m, best)
    return best.reshape(h, w), second.reshape(h, w), off.reshape(h, w, 2)


def quant(v, palette):
    """map 0..1 to a fixed palette: crunchy pixel-art shading"""
    pal = np.array(palette, float)
    idx = np.clip((v * len(pal)).astype(int), 0, len(pal) - 1)
    return pal[idx]


def lumps(w, h, seed, cell, palette, aniso=(1, 1), crease=1.1, light=(-0.6, -0.8)):
    """keloid scales: each cell a lump lit from the top-left, deep black creases between them"""
    d1, d2, off = voronoi(w, h, cell, seed, aniso)
    rng = np.random.default_rng(seed + 1)
    lx, ly = light
    n = np.hypot(off[..., 0], off[..., 1]) + 1e-6
    lit = (off[..., 0] * lx + off[..., 1] * ly) / n  # -1..1, which side of the lump faces the light
    dome = 1 - np.clip(d1 / (cell * 0.75), 0, 1)
    v = 0.35 + 0.30 * dome + 0.22 * lit * (1 - dome) + rng.normal(0, 0.07, (h, w))
    v = np.where(d2 - d1 < crease, 0.02, v)  # creases
    return quant(np.clip(v, 0, 0.999), palette)


SKIN = [(20, 20, 21), (37, 36, 37), (52, 50, 52), (66, 65, 65), (81, 78, 78), (98, 95, 93), (122, 117, 113)]
BELLY = [(23, 21, 23), (43, 42, 42), (61, 58, 58), (75, 72, 71), (90, 87, 84), (110, 106, 101), (136, 130, 124)]
KNOB_OLD = [(29, 27, 29), (58, 56, 56), (81, 78, 78), (101, 98, 95), (124, 120, 116), (151, 146, 140)]
PLATE = [(44, 45, 48), (72, 74, 78), (100, 102, 106), (128, 130, 134), (156, 158, 162), (186, 188, 191), (214, 215, 217)]
KNOB = SKIN[1:]
CLAW = [(70, 66, 60), (104, 98, 88), (138, 131, 118), (170, 163, 148), (198, 192, 178)]
TOOTH = [(150, 140, 118), (190, 182, 160), (222, 216, 196), (240, 236, 222)]
MOUTH = [(70, 20, 28), (110, 38, 46), (146, 60, 68), (178, 88, 94), (204, 120, 124)]
EYE = [(6, 4, 3), (26, 14, 8), (90, 52, 20)]


def plate_tex(w, h, seed):
    """dorsal plate: pale crystalline ridges running to the tips, dark grooves, black rim"""
    rng = np.random.default_rng(seed)
    x = np.arange(w)[None, :]
    y = np.arange(h)[:, None]
    ridge = (np.sin(x * 0.9 + np.sin(y * 0.15) * 2 + rng.random() * 6) * 0.5 + 0.5) ** 2
    grad = 1 - y / h  # lighter toward the tip (top of the texture)
    v = 0.25 + 0.45 * ridge * (0.5 + grad * 0.5) + 0.25 * grad + rng.normal(0, 0.08, (h, w))
    d1, d2, _ = voronoi(w, h, 7, seed + 3, (1, 2.4))
    v = np.where(d2 - d1 < 0.9, v * 0.45, v)
    return quant(np.clip(v, 0, 0.999), PLATE)


def plate_glow(base):
    """the same plate texture re-coloured as an atomic glow: cyan with white-hot ridges"""
    lum = base.mean(-1) / 255
    lo, hi = np.array([20, 140, 230.]), np.array([225, 255, 255.])
    t = np.clip((lum - 0.12) / 0.6, 0, 1)[..., None]
    rgb = lo + (hi - lo) * t ** 1.3
    return rgb


def simple(w, h, seed, palette, grain=0.12, streak=False):
    rng = np.random.default_rng(seed)
    v = 0.55 + rng.normal(0, grain, (h, w))
    if streak:
        v += np.linspace(0.25, -0.25, w)[None, :]
    return quant(np.clip(v, 0, 0.999), palette)


def cracks(w, h, seed, n, length):
    """branching magma veins: returns core (1), hot (2) and rim (3) masks as an int map"""
    rng = random.Random(seed)
    m = np.zeros((h, w), int)
    def walk(x, y, ang, ln, width):
        for _ in range(ln):
            ang += rng.uniform(-0.55, 0.55)
            x = (x + math.cos(ang)) % w
            y = (y + math.sin(ang)) % h
            xi, yi = int(x), int(y)
            for dx in range(-width, width + 1):
                for dy in range(-width, width + 1):
                    xx, yy = (xi + dx) % w, (yi + dy) % h
                    lvl = 1 if (dx == 0 and dy == 0) else 2
                    if m[yy, xx] == 0 or m[yy, xx] > lvl:
                        m[yy, xx] = lvl
            if rng.random() < 0.06 and ln > 6:
                walk(x, y, ang + rng.choice((-1, 1)) * rng.uniform(0.7, 1.4), ln // 2, max(0, width - 1))
    for _ in range(n):
        walk(rng.uniform(0, w), rng.uniform(0, h), rng.uniform(0, math.tau), rng.randint(length // 2, length), rng.choice((0, 0, 1)))
    # dark rim around the veins
    rim = np.zeros_like(m, bool)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        rim |= np.roll(np.roll(m > 0, dx, 1), dy, 0)
    m = np.where((m == 0) & rim, 3, m)
    return m


def make_textures():
    """atlas layout (texels): every material is a 128x128 patch so a 64 px face fits at 2 texels/px"""
    P = {}
    base = np.zeros((TEX, TEX, 3))
    alpha = np.zeros((TEX, TEX))
    patches = {}

    def put(name, rgb, x, y):
        h, w = rgb.shape[:2]
        base[y:y + h, x:x + w] = rgb
        alpha[y:y + h, x:x + w] = 255
        P[name] = (x, y, w, h)
        patches[name] = rgb

    put("skin", lumps(128, 128, 11, 4.2, SKIN), 0, 0)
    put("skin2", lumps(128, 128, 12, 6.0, SKIN), 128, 0)            # coarser: thighs, tail base, shoulders
    put("belly", lumps(128, 128, 13, 4.0, BELLY, aniso=(2.6, 0.8)), 256, 0)  # horizontal ventral folds
    put("knob", lumps(128, 128, 14, 2.6, KNOB, crease=0.8), 384, 0)     # the protruding lumps
    put("plate", plate_tex(128, 128, 15), 0, 128)
    put("plate2", plate_tex(128, 128, 16), 128, 128)
    put("claw", simple(64, 64, 17, CLAW, streak=True), 256, 128)
    put("tooth", simple(64, 64, 18, TOOTH, 0.1), 320, 128)
    put("mouth", lumps(64, 64, 19, 3.0, MOUTH, crease=0.6), 256, 192)
    put("tongue", simple(64, 64, 20, MOUTH[1:], 0.12), 320, 192)
    put("eye", simple(32, 32, 21, EYE, 0.25), 384, 128)
    put("face", lumps(128, 128, 22, 3.0, SKIN, crease=0.8), 256, 256)  # finer lumps for the head
    put("sole", lumps(128, 128, 23, 5.0, SKIN[:4]), 384, 256)

    normal = np.dstack([base, alpha]).astype(np.uint8)

    # atomic glow mask: plates + mouth
    glow = np.zeros((TEX, TEX, 4))
    for name in ("plate", "plate2"):
        x, y, w, h = P[name]
        glow[y:y + h, x:x + w, :3] = plate_glow(patches[name])
        glow[y:y + h, x:x + w, 3] = 255
    for name, col in (("mouth", (120, 230, 255)), ("tongue", (170, 245, 255))):
        x, y, w, h = P[name]
        lum = patches[name].mean(-1, keepdims=True) / 255
        glow[y:y + h, x:x + w, :3] = np.array(col) * (0.6 + lum)
        glow[y:y + h, x:x + w, 3] = 255

    # burning variant: magma veins, dense on the chest/belly, sparse elsewhere
    burn = base.copy()
    bglow = np.zeros((TEX, TEX, 4))
    HOT, CORE, RIM = np.array([236, 84, 18.]), np.array([255, 190, 80.]), np.array([40, 10, 6.])
    for name, n, ln in (("belly", 11, 46), ("skin", 4, 34), ("skin2", 3, 34), ("face", 2, 22)):
        x, y, w, h = P[name]
        m = cracks(w, h, shash(name), n, ln)
        reg = burn[y:y + h, x:x + w]
        reg[m == 1], reg[m == 2], reg[m == 3] = CORE, HOT, RIM
        g = bglow[y:y + h, x:x + w]
        g[m == 1] = [*CORE, 255]
        g[m == 2] = [*HOT, 255]
    burning = np.dstack([burn, alpha]).astype(np.uint8)
    return P, normal, glow.astype(np.uint8), burning, bglow.astype(np.uint8)


# ====================================================================== geometry
class Rig:
    def __init__(self, P):
        self.P = P
        self.bones = []
        self.by = {}

    def bone(self, name, parent, pivot, rot=None):
        assert name not in self.by, name
        b = {"name": name, "pivot": [round(v, 3) for v in pivot], "cubes": []}
        if parent:
            b["parent"] = parent
        if rot:
            b["rotation"] = [round(v, 3) for v in rot]
        self.bones.append(b)
        self.by[name] = b
        return name

    def uv(self, mat, fw, fh, seed):
        x, y, w, h = self.P[mat]
        tw, th = min(w, max(1, round(fw * S))), min(h, max(1, round(fh * S)))
        rr = random.Random(seed)
        return {"uv": [x + rr.randint(0, w - tw), y + rr.randint(0, h - th)], "uv_size": [tw, th]}

    def cube(self, bone, origin, size, mat="skin", rot=None, pivot=None, faces=None):
        w, h, d = size
        if min(size) <= 0:
            return
        seed = R.randint(0, 1 << 30)
        dims = {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}
        uv = {}
        for f, (fw, fh) in dims.items():
            m = (faces or {}).get(f, mat)
            if m is None:
                continue
            uv[f] = self.uv(m, fw, fh, seed + shash(f) % 997)
        c = {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": uv}
        if rot and any(rot):
            c["rotation"] = [round(v, 3) for v in rot]
            c["pivot"] = [round(v, 3) for v in (pivot or (origin[0] + w / 2, origin[1] + h / 2, origin[2] + d / 2))]
        self.by[bone]["cubes"].append(c)

    def box(self, bone, centre, half, mat="skin", **kw):
        cx, cy, cz = centre
        hx, hy, hz = half
        self.cube(bone, (cx - hx, cy - hy, cz - hz), (2 * hx, 2 * hy, 2 * hz), mat, **kw)

    def blob(self, bone, c, r, mat="skin", layers=6, axis="y", fat=0.78):
        """stepped voxel ellipsoid: a stack of slabs, each slab a cross of two boxes (octagonal section)"""
        cx, cy, cz = c
        rx, ry, rz = r
        for i in range(layers):
            t0, t1 = -1 + 2 * i / layers, -1 + 2 * (i + 1) / layers
            tm = (t0 + t1) / 2
            f = math.sqrt(max(0.0, 1 - tm * tm)) ** 0.85
            if axis == "y":
                y0, y1 = cy + t0 * ry, cy + t1 * ry
                self.cube(bone, (cx - rx * f, y0, cz - rz * f * fat), (2 * rx * f, y1 - y0, 2 * rz * f * fat), mat)
                self.cube(bone, (cx - rx * f * fat, y0, cz - rz * f), (2 * rx * f * fat, y1 - y0, 2 * rz * f), mat)
            elif axis == "z":
                z0, z1 = cz + t0 * rz, cz + t1 * rz
                self.cube(bone, (cx - rx * f, cy - ry * f * fat, z0), (2 * rx * f, 2 * ry * f * fat, z1 - z0), mat)
                self.cube(bone, (cx - rx * f * fat, cy - ry * f, z0), (2 * rx * f * fat, 2 * ry * f, z1 - z0), mat)

    def knobs(self, bone, c, r, n, seed, size=(1.0, 2.4), mat="knob", where=None, sink=0.45, axis_aligned=True):
        """voxel lumps poking out of an ellipsoid surface: the gnarled, keloid hide"""
        rr = random.Random(seed)
        cx, cy, cz = c
        rx, ry, rz = r
        made = tries = 0
        while made < n and tries < n * 20:
            tries += 1
            u, v = rr.uniform(-1, 1), rr.uniform(0, math.tau)
            s = math.sqrt(1 - u * u)
            nx, ny, nz = s * math.cos(v), u, s * math.sin(v)
            px, py, pz = cx + nx * rx, cy + ny * ry, cz + nz * rz
            if where and not where(px, py, pz, nx, ny, nz):
                continue
            k = rr.uniform(*size)
            kw, kh, kd = k * rr.uniform(0.8, 1.3), k * rr.uniform(0.7, 1.2), k * rr.uniform(0.8, 1.3)
            # push the cube out along the normal so only its outer part shows
            ox, oy, oz = px + nx * k * (0.5 - sink), py + ny * k * (0.5 - sink), pz + nz * k * (0.5 - sink)
            rot = None if axis_aligned else (rr.uniform(-20, 20), rr.uniform(-30, 30), rr.uniform(-20, 20))
            self.cube(bone, (ox - kw / 2, oy - kh / 2, oz - kd / 2), (kw, kh, kd), mat, rot=rot)
            made += 1

    def json(self):
        bones = []
        for b in self.bones:
            bb = dict(b)
            if not bb["cubes"]:
                bb.pop("cubes")
            bones.append(bb)
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry.kaiju", "texture_width": TEX, "texture_height": TEX,
                            "visible_bounds_width": 16, "visible_bounds_height": 12, "visible_bounds_offset": [0, 5, 0]},
            "bones": bones}]}


SIDES = (("l", 1), ("r", -1))


def plate(rig, parent, name, base, height, width, tilt_back, tilt_out=0.0, seed=0, mat="plate", thick=None):
    """one dorsal plate: a jagged, frost-like leaf of thin slabs. It is its own bone so it can glow/flex later.
    Built growing +y in the yz plane (thin in x), then the bone tilts it back (+z) and outward."""
    rr = random.Random(seed)
    bx, by, bz = base
    b = rig.bone(name, parent, base, rot=(-tilt_back, 0, tilt_out))
    t = thick or max(1.2, min(3.2, height * 0.125))
    h, w = height, width
    # root lobe, sunk into the back, and the broad lower body of the plate
    rig.cube(b, (bx - t / 2 - 0.25, by - 2.0, bz - w * 0.5), (t + 0.5, 3.0, w), mat)
    rig.cube(b, (bx - t / 2, by, bz - w * 0.42), (t, h * 0.32, w * 0.84), mat)
    rig.cube(b, (bx - t / 2, by + h * 0.3, bz - w * 0.3), (t, h * 0.28, w * 0.6), mat)
    # main spike
    rig.cube(b, (bx - t * 0.4, by + h * 0.5, bz - w * 0.13), (t * 0.8, h * 0.5, w * 0.26), mat,
             rot=(rr.uniform(-6, 6), 0, 0), pivot=(bx, by + h * 0.5, bz))
    rig.cube(b, (bx - t * 0.3, by + h * 0.92, bz - w * 0.06), (t * 0.6, h * 0.16, w * 0.12), mat)
    # side prongs: the jagged "maple leaf" outline, leaning forward and back
    nprong = 2 if h < 9 else (3 if h < 16 else 4)
    for k in range(nprong):
        for sgn in (-1, 1):
            fy = 0.18 + 0.62 * (k + rr.uniform(0.1, 0.6)) / nprong
            ln = h * rr.uniform(0.32, 0.5) * (1 - fy * 0.45)
            pw = max(0.8, w * rr.uniform(0.12, 0.2) * (1 - fy * 0.4))
            ang = sgn * rr.uniform(38, 62)
            pz = bz + sgn * w * 0.25 * (1 - fy * 0.7)
            py = by + h * fy
            rig.cube(b, (bx - t * 0.4, py, pz - pw / 2), (t * 0.8, ln, pw), mat, rot=(ang, 0, 0), pivot=(bx, py, pz))
            # a small serration on the prong
            if ln > 3:
                rig.cube(b, (bx - t * 0.3, py + ln * 0.45, pz - pw * 0.3), (t * 0.6, ln * 0.4, pw * 0.6), mat,
                         rot=(ang + sgn * 32, 0, 0), pivot=(bx, py + ln * 0.35, pz + sgn * math.sin(math.radians(abs(ang))) * ln * 0.3))
    return b


def build(P):
    rig = Rig(P)
    # ---------------------------------------------------------------- root and hips
    rig.bone("root", None, (0, 0, 0))
    rig.bone("body", "root", (0, 40, 4))
    rig.blob("body", (0, 40, 3), (19.5, 11, 14.5), "skin2")
    rig.blob("body", (0, 33, 10), (14, 7, 9), "skin2")  # rump into the tail
    rig.knobs("body", (0, 40, 3), (18, 10, 14), 70, 101, (1.2, 3.0), where=lambda x, y, z, *n: z > -6)
    rig.knobs("body", (0, 40, 3), (18, 10, 14), 25, 1011, (0.8, 1.8), mat="belly", where=lambda x, y, z, *n: z <= -6)

    # ---------------------------------------------------------------- torso: belly, barrel chest, hunched upper back
    rig.bone("torso", "body", (0, 46, 2), rot=(9, 0, 0))
    rig.blob("torso", (0, 52, -0.5), (17.5, 10.5, 15), "skin")
    rig.blob("torso", (0, 64.5, -2.5), (15.6, 10, 14.8), "skin2")
    rig.blob("torso", (0, 73.5, -0.5), (13.5, 5, 12), "skin2", layers=4)  # trapezius sloping into the neck
    rig.blob("torso", (0, 69, 5.5), (13, 9, 10.5), "skin2")  # hump between the shoulders
    for side, sg in SIDES:  # pectorals
        rig.blob("torso", (sg * 7.5, 67, -11.5), (8.5, 6.5, 5), "belly", layers=4)
    # ventral folds: stacked horizontal bands down the front, the belly of the photographs
    for i in range(9):
        y = 45.5 + i * 2.55
        wd = 12.5 - abs(i - 4) * 0.9
        rig.cube("torso", (-wd, y, -15.0 - (0.6 if i % 2 else 0) + min(i, 6) * -0.15), (2 * wd, 2.1, 3.5), "belly")
    rig.knobs("torso", (0, 52, -0.5), (17.5, 10.5, 15), 70, 102, (1.0, 2.6), where=lambda x, y, z, *n: z > -9)
    rig.knobs("torso", (0, 64.5, -2.5), (15.6, 10, 14.8), 90, 103, (1.2, 3.0), where=lambda x, y, z, *n: z > -10)
    rig.knobs("torso", (0, 70, 6), (15, 9, 10), 60, 104, (1.4, 3.2))
    for side, sg in SIDES:
        rig.knobs("torso", (sg * 7.5, 67, -11.5), (8.5, 6.5, 5), 16, 105 + sg, (0.8, 1.8), mat="belly")

    # ---------------------------------------------------------------- neck: thick, leaning forward, folds at the throat
    rig.bone("neck", "torso", (0, 74, -2), rot=(-9, 0, 0))
    rig.blob("neck", (0, 78.5, -5), (12.5, 7.5, 12), "skin2", layers=5)
    rig.blob("neck", (0, 84, -9.5), (9.5, 6, 9), "skin", layers=5)
    for i in range(4):  # throat folds
        rig.cube("neck", (-7 + i * 0.6, 73.5 + i * 2.6, -16.6 + i * -0.9), (14 - i * 1.2, 2.0, 4.5), "belly")
    rig.knobs("neck", (0, 78.5, -5), (12, 7, 11.5), 50, 106, (1.0, 2.6), where=lambda x, y, z, *n: z > -12)
    rig.knobs("neck", (0, 84, -9.5), (9.5, 6, 9), 40, 107, (0.9, 2.2), where=lambda x, y, z, *n: z > -15)

    # ---------------------------------------------------------------- head: small, brow ridge, rounded snout
    rig.bone("head", "neck", (0, 87, -13))
    rig.blob("head", (0, 88.5, -19.5), (7, 5.5, 6.5), "face", layers=4)     # back of the skull into the neck
    rig.cube("head", (-5.5, 89.5, -27), (11, 4.5, 10), "face")              # cranium
    rig.cube("head", (-4.5, 93.5, -26), (9, 1.6, 8), "face")                # cranium top
    rig.cube("head", (-6.5, 91.6, -28.4), (13, 2.4, 4.4), "face")           # heavy brow ridge, overhanging the eyes
    rig.cube("head", (-5.2, 86.8, -35.2), (10.4, 4.4, 9.2), "face")         # snout / maxilla
    rig.cube("head", (-4.2, 87.3, -36.6), (8.4, 3.6, 1.6), "face")          # rounded nose
    rig.cube("head", (-3.6, 91.0, -34.4), (7.2, 1.6, 7.0), "face")          # bridge of the nose
    rig.cube("head", (-2.4, 90.2, -37.0), (4.8, 1.8, 2.2), "face")          # nose tip bump
    for side, sg in SIDES:
        rig.cube("head", (sg * 6.2 - 1.2, 87.0, -27.0), (2.4, 4.6, 8.0), "face")   # cheeks / jaw muscles
        rig.cube("head", (sg * 4.9 - 0.5, 90.6, -37.0 + 1.4), (1.0, 1.0, 1.0), "eye")  # nostrils
        # the eye: small, deep-set under the brow
        rig.cube("head", (sg * 5.9 - 0.8, 90.2, -28.6), (1.6, 1.4, 1.6), "eye")
        rig.cube("head", (sg * 6.5 - 0.6, 89.6, -29.2), (1.2, 0.6, 2.4), "face")   # lower lid
    rig.knobs("head", (0, 91.5, -25), (6.6, 3.6, 6), 26, 108, (0.6, 1.4), mat="face", where=lambda x, y, z, *n: y > 91)
    rig.knobs("head", (0, 89, -32), (5.6, 2.8, 5), 18, 109, (0.5, 1.1), mat="face", where=lambda x, y, z, *n: y > 89)
    # upper mouth: palate and a ragged row of teeth along the lip
    rig.cube("head", (-4.4, 86.2, -35.4), (8.8, 0.7, 9.6), "mouth", faces={"up": None})
    rr = random.Random(77)
    for i in range(7):  # front teeth around the nose
        x = -3.6 + i * 1.2
        ln = rr.uniform(1.0, 1.6)
        rig.cube("head", (x - 0.35, 86.4 - ln, -36.1 + abs(x) * 0.25), (0.7, ln, 0.7), "tooth")
    for side, sg in SIDES:
        for k in range(7):
            ln = rr.uniform(0.9, 1.7)
            rig.cube("head", (sg * 4.75 - 0.35, 86.4 - ln, -34.0 + k * 1.35), (0.7, ln, 0.7), "tooth")

    # lower jaw
    rig.bone("jaw", "head", (0, 86.8, -22))
    rig.cube("jaw", (-4.9, 83.2, -34.6), (9.8, 3.0, 13.0), "face")
    rig.cube("jaw", (-4.0, 82.6, -33.2), (8.0, 1.2, 10.0), "belly")  # chin underside
    rig.cube("jaw", (-4.1, 86.0, -34.0), (8.2, 0.5, 11.6), "mouth", faces={"down": None})
    rig.cube("jaw", (-2.4, 86.2, -31.5), (4.8, 0.9, 8.0), "tongue")
    for i in range(6):
        x = -3.0 + i * 1.2
        ln = rr.uniform(0.9, 1.4)
        rig.cube("jaw", (x - 0.35, 86.1, -34.3 + abs(x) * 0.2), (0.7, ln, 0.7), "tooth")
    for side, sg in SIDES:
        for k in range(6):
            ln = rr.uniform(0.8, 1.5)
            rig.cube("jaw", (sg * 4.3 - 0.35, 86.1, -32.6 + k * 1.4), (0.7, ln, 0.7), "tooth")
        rig.cube("jaw", (sg * 5.0 - 0.6, 83.6, -26.0), (1.2, 2.8, 4.5), "face")  # jaw corner
    rig.knobs("jaw", (0, 84, -28), (5, 1.6, 6.5), 14, 110, (0.5, 1.2), mat="face", where=lambda x, y, z, *n: y < 84.5)

    # ---------------------------------------------------------------- arms: short, muscular, four claws
    for side, sg in SIDES:
        sh = f"arm_{side}"
        rig.bone(sh, "torso", (sg * 16.5, 68, -6))
        rig.blob(sh, (sg * 17.5, 67, -6.5), (5.5, 5, 5.5), "skin2", layers=4)   # shoulder
        rig.blob(sh, (sg * 19.5, 62.5, -9), (4.6, 6.8, 4.6), "skin", layers=5)   # upper arm
        rig.knobs(sh, (sg * 19.5, 64, -8), (5, 8, 5), 28, 120 + sg, (0.8, 2.0))
        fa = f"forearm_{side}"
        rig.bone(fa, sh, (sg * 19.5, 57.5, -11))
        rig.blob(fa, (sg * 19.5, 56.5, -16), (3.8, 3.8, 6.5), "skin", layers=5, axis="z")
        rig.knobs(fa, (sg * 19.5, 56.5, -16), (3.8, 3.8, 6.5), 20, 122 + sg, (0.7, 1.6))
        hd = f"hand_{side}"
        rig.bone(hd, fa, (sg * 19.5, 56.5, -22))
        rig.box(hd, (sg * 19.5, 56.0, -23.5), (2.6, 2.4, 2.0), "skin")
        for k in range(4):  # fingers: two joints, curled down, then the pale claw
            fx = sg * 19.5 + (k - 1.5) * 1.35 * sg
            fb = f"finger{k}_{side}"
            rig.bone(fb, hd, (fx, 56.5, -25.2), rot=(16 + k * 3, 0, 0))
            rig.cube(fb, (fx - 0.55, 55.6, -28.6), (1.1, 1.3, 3.6), "skin")
            rig.cube(fb, (fx - 0.45, 55.4, -30.6), (0.9, 1.1, 2.4), "skin", rot=(28, 0, 0), pivot=(fx, 56.0, -28.4))
            rig.cube(fb, (fx - 0.35, 54.0, -32.0), (0.7, 1.0, 2.0), "claw", rot=(62, 0, 0), pivot=(fx, 55.6, -30.2))
        # the thumb, inner side
        tb = f"thumb_{side}"
        rig.bone(tb, hd, (sg * 17.2, 55.5, -23.5), rot=(10, -sg * 25, 0))
        rig.cube(tb, (sg * 17.2 - 0.6, 54.8, -26.6), (1.2, 1.2, 3.0), "skin")
        rig.cube(tb, (sg * 17.2 - 0.35, 53.8, -27.8), (0.7, 1.0, 1.8), "claw", rot=(55, 0, 0), pivot=(sg * 17.2, 55.0, -26.4))

    # ---------------------------------------------------------------- legs: huge thighs, column shins, elephant feet
    for side, sg in SIDES:
        th = f"thigh_{side}"
        rig.bone(th, "body", (sg * 12, 40, 2))
        rig.blob(th, (sg * 14.5, 31, 0), (11.5, 13, 13), "skin2", layers=6)
        rig.blob(th, (sg * 15, 23, -4), (9.2, 6, 9.2), "skin", layers=4)        # knee
        rig.knobs(th, (sg * 14.5, 31, 0), (11.5, 13, 13), 95, 130 + sg, (1.2, 3.2))
        sn = f"shin_{side}"
        rig.bone(sn, th, (sg * 15, 22, -3))
        rig.blob(sn, (sg * 15, 13, -1.5), (8.6, 10, 9), "skin", layers=6)
        rig.knobs(sn, (sg * 15, 13, -1.5), (8.6, 10, 9), 60, 132 + sg, (1.0, 2.4))
        ft = f"foot_{side}"
        rig.bone(ft, sn, (sg * 15, 5, -1))
        rig.cube(ft, (sg * 15 - 8.5, 0, -10.0), (17, 4.5, 17), "skin2", faces={"down": "sole"})
        rig.cube(ft, (sg * 15 - 7.5, 4.5, -8), (15, 3, 13), "skin")
        rig.knobs(ft, (sg * 15, 3.5, -1.5), (8.5, 3, 8.5), 26, 134 + sg, (0.8, 1.8), where=lambda x, y, z, *n: y > 2)
        for k, dx in enumerate((-5.4, 0.0, 5.4)):  # three front toes, each with a thick blunt claw
            tx = sg * 15 + dx * sg
            rig.cube(ft, (tx - 2.0, 0, -15), (4.6, 3.8, 4.5), "skin")
            rig.cube(ft, (tx - 1.5, 0, -15.5), (3.0, 2.6, 2.2), "claw", rot=(-8, 0, 0), pivot=(tx, 0, -15))
        rig.cube(ft, (sg * 15 - sg * 6.5 - 1.2, 0, 4.5), (2.4, 2.2, 3.0), "claw")  # back toe

    # ---------------------------------------------------------------- tail: twelve segments, curls up at the tip
    lens = [14, 14, 14, 13.5, 13, 12.5, 12, 11.5, 11, 10, 9.5, 9]
    rads = [13.5, 12.3, 11.0, 9.8, 8.7, 7.7, 6.8, 5.9, 5.1, 4.3, 3.5, 2.8, 2.0]
    # bend: (x pitch, y yaw) per joint; - pitch droops. Goes down to the ground, sweeps aside, tip hooks up
    bend = [(-20, 0), (-16, 3), (-8, 5), (14, 7), (18, 8), (12, 8), (0, 6), (0, 4), (16, 0), (24, -4), (28, -6), (28, -6)]
    z = 12.0
    parent = "body"
    tails = []
    for i, L in enumerate(lens):
        name = f"tail{i + 1}"
        rig.bone(name, parent, (0, 40, z), rot=(bend[i][0], bend[i][1], 0))
        r0, r1 = rads[i], rads[i + 1]
        ry0, ry1 = r0 * 0.95, r1 * 0.95
        for k in range(2):  # two tapered steps per segment
            a = r0 + (r1 - r0) * (k / 2)
            ay = ry0 + (ry1 - ry0) * (k / 2)
            z0, z1 = z + L * k / 2 - 0.6, z + L * (k + 1) / 2 + 0.6
            rig.cube(name, (-a, 40 - ay * 0.8, z0), (2 * a, ay * 1.6, z1 - z0), "skin2" if i < 4 else "skin")
            rig.cube(name, (-a * 0.8, 40 - ay, z0), (2 * a * 0.8, ay * 2, z1 - z0), "skin2" if i < 4 else "skin")
        # underside scutes
        rig.cube(name, (-r0 * 0.55, 40 - r0 * 0.95 - 0.5, z + 1), (r0 * 1.1, 1.0, L - 2), "belly")
        rig.knobs(name, (0, 40, z + L / 2), (r0 * 0.95, r0 * 0.98, L / 2), int(10 + r0 * 2.6), 140 + i, (0.7, max(1.0, r0 * 0.26)),
                  where=lambda x, y, zz, nx, ny, nz: ny > -0.5 and abs(nz) < 0.85)
        tails.append((name, z, L, r0, r1))
        parent = name
        z += L

    # ---------------------------------------------------------------- dorsal plates: three rows, neck to tail tip
    pid = [0]

    def P_(parent, base, h, w, back, out=0, mat=None):
        pid[0] += 1
        plate(rig, parent, f"plate{pid[0]}", base, h, w, back, out, seed=pid[0] * 31, mat=mat or ("plate" if pid[0] % 2 else "plate2"))

    # (parent, base point on the spine, centre plate height, width, lean back, side spacing)
    spine = [
        ("neck", (0, 88.0, -9.5), 7.5, 6.0, 14, 3.2),
        ("neck", (0, 85.0, -3.5), 12.0, 8.5, 22, 4.2),
        ("neck", (0, 80.5, 2.5), 17.0, 11.0, 30, 5.4),
        ("torso", (0, 75.5, 8.5), 23.0, 14.0, 38, 6.8),
        ("torso", (0, 69.5, 13.0), 26.0, 15.5, 44, 7.6),
        ("torso", (0, 62.5, 13.8), 26.0, 15.5, 50, 8.0),
        ("torso", (0, 55.5, 13.5), 22.0, 14.0, 56, 7.8),
        ("body", (0, 49.0, 13.5), 18.0, 12.0, 62, 7.2),
        ("body", (0, 45.5, 16.0), 15.0, 10.5, 66, 6.6),
    ]
    for par, (x, y, zz), h, w, back, sp in spine:
        P_(par, (x, y, zz), h, w, back)
        for side, sg in SIDES:  # side rows: smaller, staggered, splayed outward
            P_(par, (sg * sp * 1.15, y - 2.0, zz - 1.6), h * 0.5, w * 0.55, back - 4, -sg * 30)
    for i, (name, z0, L, r0, r1) in enumerate(tails):
        for k in range(2):
            f = (k + 0.5) / 2
            r = r0 + (r1 - r0) * f
            zz = z0 + L * f
            h = max(2.2, 13.5 * (1 - i / 12.5) ** 1.15 - k * 0.5)
            w = max(2.0, h * 0.62)
            P_(name, (0, 40 + r * 0.9, zz), h, w, 18 - i * 0.5)
            if h > 4:
                for side, sg in SIDES:
                    P_(name, (sg * r * 0.45, 40 + r * 0.78, zz + L * 0.25), h * 0.55, w * 0.6, 16, -sg * 30)
    return rig


def main():
    os.makedirs(OUT, exist_ok=True)
    P, normal, glow, burning, bglow = make_textures()
    rig = build(P)
    Image.fromarray(normal, "RGBA").save(os.path.join(OUT, "kaiju.png"))
    Image.fromarray(glow, "RGBA").save(os.path.join(OUT, "kaiju_glowmask.png"))
    Image.fromarray(burning, "RGBA").save(os.path.join(OUT, "kaiju_burning.png"))
    Image.fromarray(bglow, "RGBA").save(os.path.join(OUT, "kaiju_burning_glowmask.png"))
    with open(os.path.join(OUT, "kaiju.geo.json"), "w") as f:
        json.dump(rig.json(), f, separators=(",", ":"))
    n = sum(len(b.get("cubes", [])) for b in rig.bones)
    print(f"bones {len(rig.bones)} cubes {n}")


if __name__ == "__main__":
    main()
