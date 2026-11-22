#!/usr/bin/env python3
"""KAIJU v2: geometry and textures (GeckoLib / bedrock geometry, Minecraft 1.20.1).

Charcoal-black, heavy-legged upright kaiju: small head under a heavy brow, thick folded neck, barrel chest with ventral
folds, short clawed arms, column legs on elephant feet, a very long tail that hooks up at the tip, and three rows of
jagged, frost-like dorsal plates. Rounded volumes are stepped voxel ellipsoids (three-box cross sections), the hide is a
crust of lumps and fold rings, textured with an embossed keloid pattern at 4 texels per model pixel.

Rig (all animatable): root/body/torso/belly/chest, neck1-3, throat, head, brow_l/r, eyelid_l/r, lip_upper, jaw,
lip_lower, tongue1-2, shoulder/arm/forearm/hand per side with 4 three-joint fingers and a two-joint thumb,
thigh/shin/foot with three two-joint toes and a dew claw, tail1..tail16, and every dorsal plate as a mount bone (fixed
tilt) with a slider child `plate_<n>` that moves along the plate's own axis: the atomic-breath animation pushes the
plates out of the back one by one from the tail tip to the head and slams them back in afterwards.

Model units: 16 px = 1 block, front = -z, ground = y 0, about 6.5 blocks tall; the renderer scales it.
Outputs (out/): kaiju.geo.json, kaiju.png, kaiju_glowmask.png, kaiju_burning.png, kaiju_burning_glowmask.png,
plates.json (slider bones in tail-to-head order with their travel, for the animation generator).
"""
import json, math, os, random, sys, zlib
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "out")
TEX = 1024
S = 4  # texels per model pixel
R = random.Random(1954)


def shash(v):
    return zlib.crc32(str(v).encode())


# ====================================================================== textures
def voronoi(w, h, cell, seed, aniso=(1.0, 1.0)):
    """tileable jittered-grid voronoi: nearest distance, second distance, offset to the nearest site (pixels)"""
    rng = np.random.default_rng(seed)
    cx, cy = cell * aniso[0], cell * aniso[1]
    gw, gh = max(2, round(w / cx)), max(2, round(h / cy))
    cx, cy = w / gw, h / gh
    jit = rng.random((gh, gw, 2)) * 0.9 + 0.05
    yy, xx = np.mgrid[0:h, 0:w].astype(float)
    X, Y = (xx + 0.5) / cx, (yy + 0.5) / cy
    ix, iy = np.floor(X).astype(int), np.floor(Y).astype(int)
    best = np.full((h, w), np.inf)
    second = np.full((h, w), np.inf)
    off = np.zeros((h, w, 2))
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            nx, ny = ix + dx, iy + dy
            j = jit[ny % gh, nx % gw]
            ox, oy = (X - (nx + j[..., 0])) * cx / aniso[0], (Y - (ny + j[..., 1])) * cy / aniso[1]
            d = np.hypot(ox, oy)
            nb = d < best
            second = np.where(nb, best, np.minimum(second, d))
            off = np.where(nb[..., None], np.stack([ox, oy], -1), off)
            best = np.where(nb, d, best)
    return best, second, off


def vnoise(w, h, cells, seed):
    """tileable value noise 0..1"""
    rng = np.random.default_rng(seed)
    g = rng.random((cells, cells))
    yy, xx = np.mgrid[0:h, 0:w]
    xx, yy = xx / w * cells, yy / h * cells
    x0, y0 = np.floor(xx).astype(int), np.floor(yy).astype(int)
    fx, fy = xx - x0, yy - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    a, b = g[y0 % cells, x0 % cells], g[y0 % cells, (x0 + 1) % cells]
    c, d = g[(y0 + 1) % cells, x0 % cells], g[(y0 + 1) % cells, (x0 + 1) % cells]
    return (a * (1 - fx) + b * fx) * (1 - fy) + (c * (1 - fx) + d * fx) * fy


def ramp(c0, c1, n, gamma=1.0):
    return [tuple(int(c0[i] + (c1[i] - c0[i]) * (k / (n - 1)) ** gamma) for i in range(3)) for k in range(n)]


def quant(v, pal):
    pal = np.array(pal, float)
    return pal[np.clip((v * len(pal)).astype(int), 0, len(pal) - 1)]


def emboss(hf, lx=-0.7, ly=-0.7):
    gx = np.roll(hf, -1, 1) - np.roll(hf, 1, 1)
    gy = np.roll(hf, -1, 0) - np.roll(hf, 1, 0)
    return -(gx * lx + gy * ly)


def keloid(w, h, seed, cell, pal, aniso=(1, 1), ridges=0.5, fine=0.45, crease=1.4):
    """the hide: big domed lumps, fine nodules on top, wandering keloid ridges; height-field embossed, AO in creases"""
    rng = np.random.default_rng(seed)
    d1, d2, _ = voronoi(w, h, cell, seed, aniso)
    dome = np.clip(1 - d1 / (cell * 0.72), 0, 1) ** 0.7
    f1, _, _ = voronoi(w, h, cell * 0.38, seed + 7)
    nod = np.clip(1 - f1 / (cell * 0.3), 0, 1) ** 0.8
    n = vnoise(w, h, 6, seed + 3) * 0.6 + vnoise(w, h, 12, seed + 4) * 0.4
    rid = np.clip(1 - np.abs(np.sin(n * 18)) * 3, 0, 1)
    hf = dome * 1.0 + nod * fine + rid * ridges
    hf = np.where(d2 - d1 < crease, hf * 0.15, hf)
    light = emboss(hf) * 0.9
    ao = np.clip(hf / (1 + fine + ridges) * 1.4, 0, 1)
    v = 0.12 + 0.55 * ao + light + rng.normal(0, 0.035, (h, w)) + (vnoise(w, h, 4, seed + 9) - 0.5) * 0.18
    return quant(np.clip(v, 0, 0.999), pal)


SKIN = ramp((8, 8, 9), (112, 107, 101), 12, 1.3)
SKIN_W = ramp((9, 8, 8), (118, 110, 101), 12, 1.3)
BELLY = ramp((11, 10, 10), (128, 120, 112), 12, 1.25)
FACE = ramp((8, 8, 9), (118, 112, 106), 12, 1.3)
PLATE = ramp((30, 31, 34), (226, 228, 230), 12, 1.0)
CLAW = ramp((60, 56, 50), (214, 206, 190), 8)
TOOTH = ramp((140, 128, 104), (246, 240, 226), 6)
MOUTH = ramp((52, 12, 18), (214, 128, 132), 8)
EYE = [(4, 3, 2), (18, 10, 6), (64, 34, 14), (150, 96, 40), (230, 200, 150)]


def plate_tex(w, h, seed):
    """dorsal plate: crystalline ridges running to the tip, pale toward the tip, darker toward the root"""
    rng = np.random.default_rng(seed)
    n = vnoise(w, h, 8, seed)
    x = np.arange(w)[None, :] / w
    y = np.arange(h)[:, None] / h
    ridge = np.abs(np.sin((x * 9 + n * 1.6) * math.pi))
    hf = (1 - ridge) ** 1.5 + vnoise(w, h, 24, seed + 1) * 0.3
    d1, d2, _ = voronoi(w, h, 26, seed + 2, (0.6, 2.2))
    hf = np.where(d2 - d1 < 1.5, hf * 0.3, hf)
    v = 0.12 + 0.45 * hf + emboss(hf) * 1.2 + 0.42 * (1 - y) ** 1.3 + rng.normal(0, 0.03, (h, w))
    return quant(np.clip(v, 0, 0.999), PLATE)


def plate_glow(base):
    lum = base.mean(-1) / 255
    lo, hi = np.array([10, 120, 235.]), np.array([235, 255, 255.])
    t = np.clip((lum - 0.08) / 0.62, 0, 1)[..., None]
    return lo + (hi - lo) * t ** 1.4


def simple(w, h, seed, pal, grain=0.1, grad=0.0):
    rng = np.random.default_rng(seed)
    v = 0.55 + rng.normal(0, grain, (h, w)) + (vnoise(w, h, 6, seed) - 0.5) * 0.3
    v += np.linspace(grad, -grad, h)[:, None]
    return quant(np.clip(v, 0, 0.999), pal)


def eye_tex(w, h):
    """a dark, wet eye with a dim amber ring and a glint"""
    yy, xx = np.mgrid[0:h, 0:w]
    r = np.hypot((xx - w / 2 + 0.5) / (w / 2), (yy - h / 2 + 0.5) / (h / 2))
    v = np.where(r < 0.45, 0.05, np.where(r < 0.8, 0.45, np.where(r < 1.0, 0.25, 0.1)))
    v = np.where((np.abs(xx - w * 0.35) < w * 0.08) & (np.abs(yy - h * 0.32) < h * 0.08), 0.95, v)
    return quant(v, EYE)


def cracks(w, h, seed, n, length):
    """branching magma veins: 1 white-hot core, 2 orange, 3 charred rim"""
    rng = random.Random(seed)
    m = np.zeros((h, w), int)

    def walk(x, y, ang, ln, width):
        for _ in range(ln):
            ang += rng.uniform(-0.45, 0.45)
            x, y = (x + math.cos(ang)) % w, (y + math.sin(ang)) % h
            xi, yi = int(x), int(y)
            for dx in range(-width, width + 1):
                for dy in range(-width, width + 1):
                    xx, yy = (xi + dx) % w, (yi + dy) % h
                    lvl = 1 if (abs(dx) + abs(dy) <= max(0, width - 1)) else 2
                    if m[yy, xx] == 0 or m[yy, xx] > lvl:
                        m[yy, xx] = lvl
            if rng.random() < 0.05 and ln > 10:
                walk(x, y, ang + rng.choice((-1, 1)) * rng.uniform(0.6, 1.3), ln // 2, max(1, width - 1))

    for _ in range(n):
        walk(rng.uniform(0, w), rng.uniform(0, h), rng.uniform(0, math.tau), rng.randint(length // 2, length), rng.choice((1, 1, 1, 2)))
    rim = np.zeros_like(m, bool)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (2, 0), (-2, 0), (0, 2), (0, -2)):
        rim |= np.roll(np.roll(m > 0, dx, 1), dy, 0)
    return np.where((m == 0) & rim, 3, m)


def make_textures():
    P, patches = {}, {}
    base = np.zeros((TEX, TEX, 3))
    alpha = np.zeros((TEX, TEX))

    def put(name, rgb, x, y):
        h, w = rgb.shape[:2]
        base[y:y + h, x:x + w] = rgb
        alpha[y:y + h, x:x + w] = 255
        P[name] = (x, y, w, h)
        patches[name] = rgb

    Q = 256
    put("skin", keloid(Q, Q, 11, 15, SKIN), 0, 0)
    put("skin2", keloid(Q, Q, 12, 24, SKIN_W, ridges=0.7), Q, 0)
    put("belly", keloid(Q, Q, 13, 15, BELLY, aniso=(2.8, 0.7), ridges=0.25, fine=0.3), 2 * Q, 0)
    put("knob", keloid(Q, Q, 14, 9, SKIN, ridges=0.2, fine=0.6, crease=1.0), 3 * Q, 0)
    put("plate", plate_tex(Q, Q, 15), 0, Q)
    put("plate2", plate_tex(Q, Q, 16), Q, Q)
    put("face", keloid(Q, Q, 22, 10, FACE, ridges=0.35, fine=0.5), 2 * Q, Q)
    put("fold", keloid(Q, Q, 23, 12, SKIN, aniso=(3.5, 0.6), ridges=0.2), 3 * Q, Q)
    put("sole", keloid(Q, Q, 24, 18, SKIN[:7]), 0, 2 * Q)
    put("crease", keloid(Q, Q, 26, 12, ramp((6, 6, 7), (70, 66, 62), 10, 1.3), ridges=0.3), 3 * Q, 2 * Q)  # step tops: folds in shadow
    put("claw", simple(128, 128, 17, CLAW, grad=0.25), Q, 2 * Q)
    put("tooth", simple(128, 128, 18, TOOTH, 0.08, grad=0.2), Q + 128, 2 * Q)
    put("mouth", keloid(128, 128, 19, 9, MOUTH, ridges=0.3, crease=0.8), Q, 2 * Q + 128)
    put("tongue", simple(128, 128, 20, MOUTH[2:], 0.1), Q + 128, 2 * Q + 128)
    put("eye", eye_tex(32, 32), 2 * Q, 2 * Q)
    put("socket", simple(64, 64, 25, [(4, 4, 4), (12, 11, 11), (20, 19, 19)], 0.2), 2 * Q + 64, 2 * Q)
    normal = np.dstack([base, alpha]).astype(np.uint8)

    glow = np.zeros((TEX, TEX, 4))
    for name in ("plate", "plate2"):
        x, y, w, h = P[name]
        glow[y:y + h, x:x + w, :3] = plate_glow(patches[name])
        glow[y:y + h, x:x + w, 3] = 255
    for name, col in (("mouth", (110, 225, 255)), ("tongue", (170, 245, 255))):
        x, y, w, h = P[name]
        lum = patches[name].mean(-1, keepdims=True) / 255
        glow[y:y + h, x:x + w, :3] = np.clip(np.array(col) * (0.6 + lum), 0, 255)
        glow[y:y + h, x:x + w, 3] = 255

    burn = base.copy()
    bglow = np.zeros((TEX, TEX, 4))
    HOT, CORE, RIM = np.array([238, 88, 20.]), np.array([255, 205, 96.]), np.array([34, 9, 5.])
    for name, n, ln in (("belly", 8, 90), ("skin", 4, 70), ("skin2", 3, 70), ("fold", 4, 60), ("face", 2, 40)):
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
            assert parent in self.by, parent
            b["parent"] = parent
        if rot and any(rot):
            b["rotation"] = [round(v, 3) for v in rot]
        self.bones.append(b)
        self.by[name] = b
        return name

    def uv(self, mat, fw, fh, seed, at=None):
        x, y, w, h = self.P[mat]
        tw, th = min(w, max(1, round(fw * S))), min(h, max(1, round(fh * S)))
        if at is not None:  # planar projection: neighbouring slabs line up, so the hide flows across the steps
            u, v = at
            return {"uv": [x + int(round(u * S + 997)) % max(1, w - tw + 1), y + int(round(v * S + 997)) % max(1, h - th + 1)], "uv_size": [tw, th]}
        rr = random.Random(seed)
        return {"uv": [x + rr.randint(0, w - tw), y + rr.randint(0, h - th)], "uv_size": [tw, th]}

    def cube(self, bone, origin, size, mat="skin", rot=None, pivot=None, faces=None, proj=False):
        w, h, d = size
        if min(size) <= 0.01:
            return
        seed = R.randint(0, 1 << 30)
        dims = {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}
        uv = {}
        for f, (fw, fh) in dims.items():
            m = (faces or {}).get(f, mat)
            if m is None:
                continue
            at = None
            if proj:
                x0, y0, z0 = origin
                at = {"north": (-(x0 + w), 64 - (y0 + h)), "south": (x0, 64 - (y0 + h)), "east": (-(z0 + d), 64 - (y0 + h)),
                      "west": (z0, 64 - (y0 + h)), "up": (x0, z0), "down": (x0, z0)}[f]
            uv[f] = self.uv(m, fw, fh, seed + shash(f) % 997, at)
        c = {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": uv}
        if rot and any(abs(r) > 1e-3 for r in rot):
            c["rotation"] = [round(v, 3) for v in rot]
            c["pivot"] = [round(v, 3) for v in (pivot or (origin[0] + w / 2, origin[1] + h / 2, origin[2] + d / 2))]
        self.by[bone]["cubes"].append(c)

    def box(self, bone, centre, half, mat="skin", **kw):
        cx, cy, cz = centre
        hx, hy, hz = half
        self.cube(bone, (cx - hx, cy - hy, cz - hz), (2 * hx, 2 * hy, 2 * hz), mat, **kw)

    def blob(self, bone, c, r, mat="skin", layers=8, axis="y"):
        """stepped voxel ellipsoid; each slab is three overlapping boxes, so the section is a rounded octagon"""
        cx, cy, cz = c
        rx, ry, rz = r
        span = 2 * (ry if axis == "y" else rz)
        layers = max(3, min(layers, round(span / 4.2)))
        for i in range(layers):
            t0, t1 = -1 + 2 * i / layers, -1 + 2 * (i + 1) / layers
            tm = (t0 + t1) / 2
            f = math.sqrt(max(0.0, 1 - tm * tm)) ** 0.9
            for a, b in ((1.0, 0.7), (0.9, 0.9), (0.7, 1.0)):
                top = i == layers - 1
                if axis == "y":
                    y0, y1 = cy + t0 * ry, cy + t1 * ry
                    fc = {"up": mat if top else "crease", "down": "crease"}
                    self.cube(bone, (cx - rx * f * a, y0, cz - rz * f * b), (2 * rx * f * a, y1 - y0, 2 * rz * f * b), mat, faces=fc, proj=True)
                else:
                    z0, z1 = cz + t0 * rz, cz + t1 * rz
                    fc = {"north": mat if i == 0 else "crease", "south": mat if top else "crease"}
                    self.cube(bone, (cx - rx * f * a, cy - ry * f * b, z0), (2 * rx * f * a, 2 * ry * f * b, z1 - z0), mat, faces=fc, proj=True)

    def knobs(self, bone, c, r, n, seed, size=(1.0, 2.4), mat="knob", where=None, sink=0.55, tilt=6):
        """voxel lumps poking out of an ellipsoid surface: the gnarled, keloid hide"""
        rr = random.Random(seed)
        cx, cy, cz = c
        rx, ry, rz = r
        made = tries = 0
        while made < n and tries < n * 25:
            tries += 1
            u, v = rr.uniform(-0.97, 0.97), rr.uniform(0, math.tau)
            s = math.sqrt(1 - u * u)
            nx, ny, nz = s * math.cos(v), u, s * math.sin(v)
            px, py, pz = cx + nx * rx, cy + ny * ry, cz + nz * rz
            if where and not where(px, py, pz, nx, ny, nz):
                continue
            k = rr.uniform(*size) * rr.choice((1, 1, 1, 1.4))
            kw, kh, kd = k * rr.uniform(0.8, 1.4), k * rr.uniform(0.6, 1.1), k * rr.uniform(0.8, 1.4)
            o = k * (0.5 - sink)
            ox, oy, oz = px + nx * o, py + ny * o, pz + nz * o
            self.cube(bone, (ox - kw / 2, oy - kh / 2, oz - kd / 2), (kw, kh, kd), mat,
                      rot=(rr.uniform(-tilt, tilt), rr.uniform(-tilt * 2, tilt * 2), rr.uniform(-tilt, tilt)))
            made += 1

    def rings(self, bone, c, r, ys, mat="fold", grow=0.9, th=1.1):
        """horizontal skin folds around a y-axis ellipsoid"""
        cx, cy, cz = c
        rx, ry, rz = r
        for y in ys:
            t = (y - cy) / ry
            if abs(t) >= 1:
                continue
            f = math.sqrt(1 - t * t) ** 0.9
            for a, b in ((1.0, 0.72), (0.74, 1.0)):
                self.cube(bone, (cx - rx * f * a - grow, y - th / 2, cz - rz * f * b - grow), (2 * (rx * f * a + grow), th, 2 * (rz * f * b + grow)), mat, proj=True)

    def json(self):
        bones = []
        for b in self.bones:
            bb = dict(b)
            if not bb["cubes"]:
                bb.pop("cubes")
            bones.append(bb)
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry.kaiju", "texture_width": TEX, "texture_height": TEX,
                            "visible_bounds_width": 18, "visible_bounds_height": 12, "visible_bounds_offset": [0, 5, 0]},
            "bones": bones}]}


SIDES = (("l", 1), ("r", -1))
PLATES = []  # (slider bone, spine position 0=tail tip..1=head, travel px)


def plate(rig, parent, n, base, height, width, tilt_back, tilt_out, seed, s_pos, travel, mat):
    """a dorsal plate: mount bone (fixed tilt) + slider `plate_<n>` that the animations push out along the plate axis.
    Built growing +y in the yz plane, thin in x: a buried root (revealed as it rises), a collar, three stepped slabs,
    a hooked main spike, and jagged prongs with serrations, like a maple leaf of frost."""
    rr = random.Random(seed)
    bx, by, bz = base
    mount = rig.bone(f"plate_mount_{n}", parent, base, rot=(-tilt_back, 0, tilt_out))
    b = rig.bone(f"plate_{n}", mount, base)
    PLATES.append((b, s_pos, travel))
    t = max(1.0, min(3.4, height * 0.13))
    h, w = height, width
    rig.cube(b, (bx - t * 0.45, by - travel - 1.5, bz - w * 0.36), (t * 0.9, travel + 2.0, w * 0.72), mat)
    rig.cube(b, (bx - t / 2 - 0.3, by - 1.6, bz - w * 0.5), (t + 0.6, 2.6, w), mat)
    rig.cube(b, (bx - t / 2, by, bz - w * 0.46), (t, h * 0.26, w * 0.92), mat)
    rig.cube(b, (bx - t * 0.42, by + h * 0.24, bz - w * 0.36), (t * 0.84, h * 0.24, w * 0.72), mat)
    rig.cube(b, (bx - t * 0.34, by + h * 0.46, bz - w * 0.24), (t * 0.68, h * 0.2, w * 0.48), mat)
    rig.cube(b, (bx - t * 0.3, by + h * 0.6, bz - w * 0.12), (t * 0.6, h * 0.34, w * 0.24), mat,
             rot=(-rr.uniform(0, 8), 0, 0), pivot=(bx, by + h * 0.6, bz))
    rig.cube(b, (bx - t * 0.2, by + h * 0.9, bz - w * 0.05 + 0.3), (t * 0.4, h * 0.14, w * 0.1 + 0.2), mat,
             rot=(-rr.uniform(6, 16), 0, 0), pivot=(bx, by + h * 0.9, bz))
    nprong = 2 if h < 8 else (3 if h < 15 else 4)
    for k in range(nprong):
        for sgn in (-1, 1):
            fy = 0.14 + 0.66 * (k + rr.uniform(0.15, 0.6)) / nprong
            ln = h * rr.uniform(0.3, 0.46) * (1 - fy * 0.4)
            pw = max(0.7, w * rr.uniform(0.11, 0.18) * (1 - fy * 0.4))
            ang = sgn * rr.uniform(36, 64)
            pz = bz + sgn * w * 0.3 * (1 - fy * 0.75)
            py = by + h * fy
            rig.cube(b, (bx - t * 0.32, py, pz - pw / 2), (t * 0.64, ln, pw), mat, rot=(ang, 0, 0), pivot=(bx, py, pz))
            if ln > 2.5:
                a2 = ang + sgn * rr.uniform(25, 40)
                rig.cube(b, (bx - t * 0.25, py + ln * 0.4, pz - pw * 0.3), (t * 0.5, ln * 0.42, pw * 0.6), mat,
                         rot=(a2, 0, 0), pivot=(bx, py + ln * 0.35, pz))
                if ln > 5:
                    rig.cube(b, (bx - t * 0.2, py + ln * 0.7, pz - pw * 0.2), (t * 0.4, ln * 0.25, pw * 0.4), mat,
                             rot=(ang - sgn * 20, 0, 0), pivot=(bx, py + ln * 0.6, pz))
    return b


def build(P):
    rig = Rig(P)
    PLATES.clear()
    rig.bone("root", None, (0, 0, 0))

    # ---------------------------------------------------------------- hips
    rig.bone("body", "root", (0, 40, 4))
    rig.blob("body", (0, 40, 3), (19.5, 11, 14.5), "skin2", layers=8)
    rig.blob("body", (0, 34, 11), (14.5, 8, 9.5), "skin2", layers=6)
    rig.knobs("body", (0, 40, 3), (19.5, 11, 14.5), 120, 101, (1.2, 3.2), where=lambda x, y, z, *n: z > -6)

    # ---------------------------------------------------------------- belly (breathes) and chest
    rig.bone("torso", "body", (0, 46, 2), rot=(9, 0, 0))
    rig.bone("belly", "torso", (0, 50, -6))
    rig.blob("belly", (0, 52, -0.5), (17.5, 10.5, 15), "skin", layers=8)
    for i in range(10):  # ventral folds
        y = 44.8 + i * 2.35
        wd = 12.8 - abs(i - 4.5) * 0.85
        bulge = 1.2 * math.sin(i / 9 * math.pi) + (0.5 if i % 2 else 0)
        rig.cube("belly", (-wd, y, -15.2 - bulge), (2 * wd, 2.0, 4.0), "belly")
        rig.cube("belly", (-wd + 1.2, y + 0.4, -15.8 - bulge), (2 * wd - 2.4, 1.2, 1.0), "belly")
    rig.knobs("belly", (0, 52, -0.5), (17.5, 10.5, 15), 90, 102, (1.0, 2.6), where=lambda x, y, z, *n: z > -9)

    rig.bone("chest", "torso", (0, 60, -4))
    rig.blob("chest", (0, 64.5, -2.5), (15.6, 10, 14.8), "skin2", layers=8)
    rig.blob("chest", (0, 73.5, -0.5), (13.5, 5, 12), "skin2", layers=5)
    rig.blob("chest", (0, 69, 5.5), (13, 9, 10.5), "skin2", layers=6)
    for side, sg in SIDES:
        rig.blob("chest", (sg * 7.5, 67, -11.5), (8.5, 6.5, 5), "belly", layers=6)
        rig.knobs("chest", (sg * 7.5, 67, -11.5), (8.5, 6.5, 5), 26, 105 + sg, (0.8, 1.8), mat="belly")
        rig.blob("chest", (sg * 13.5, 60, 0), (4, 8, 9), "skin", layers=5)
    rig.knobs("chest", (0, 64.5, -2.5), (15.6, 10, 14.8), 120, 103, (1.2, 3.0), where=lambda x, y, z, *n: z > -10)
    rig.knobs("chest", (0, 69, 5.5), (13, 9, 10.5), 80, 104, (1.4, 3.4))

    # ---------------------------------------------------------------- neck: three segments, fold rings, throat
    rig.bone("neck1", "chest", (0, 74, -2), rot=(-9, 0, 0))
    rig.blob("neck1", (0, 78, -4.5), (12.5, 6, 12), "skin2", layers=6)
    rig.rings("neck1", (0, 78, -4.5), (12.5, 6, 12), (77.0,))
    rig.knobs("neck1", (0, 78, -4.5), (12.5, 6, 12), 60, 106, (1.0, 2.6), where=lambda x, y, z, *n: z > -12)
    rig.bone("neck2", "neck1", (0, 80.5, -6))
    rig.blob("neck2", (0, 82.5, -8), (10.6, 4.5, 10), "skin", layers=5)
    rig.knobs("neck2", (0, 82.5, -8), (10.6, 4.5, 10), 40, 107, (0.9, 2.2), where=lambda x, y, z, *n: z > -15)
    rig.bone("throat", "neck2", (0, 80, -14))
    for i in range(4):
        rig.cube("throat", (-6.6 + i * 0.6, 74.0 + i * 2.6, -17.0 - i * 0.8), (13.2 - i * 1.2, 2.0, 4.8), "belly")
    rig.bone("neck3", "neck2", (0, 85, -10))
    rig.blob("neck3", (0, 86.2, -11.5), (9.0, 4.2, 8.5), "skin", layers=5)
    rig.knobs("neck3", (0, 86.2, -11.5), (9.0, 4.2, 8.5), 30, 1071, (0.8, 2.0), where=lambda x, y, z, *n: z > -17)

    # ---------------------------------------------------------------- head
    rig.bone("head", "neck3", (0, 87.5, -14))
    rig.blob("head", (0, 88.6, -18.5), (7.2, 5.6, 5.0), "face", layers=6)
    rig.blob("head", (0, 90.6, -24.0), (6.2, 3.9, 6.4), "face", layers=6, axis="z")
    rig.blob("head", (0, 89.0, -31.6), (5.1, 2.9, 5.6), "face", layers=6, axis="z")
    rig.cube("head", (-4.6, 86.8, -36.2), (9.2, 3.2, 3.0), "face")
    rig.cube("head", (-3.4, 87.4, -37.2), (6.8, 2.6, 1.2), "face")
    rig.blob("head", (0, 91.3, -33.5), (3.0, 1.0, 3.8), "face", layers=4, axis="z")
    rig.knobs("head", (0, 92.4, -24), (5.6, 2.2, 6.0), 34, 108, (0.5, 1.3), mat="face", where=lambda x, y, z, *n: n[1] > 0.2)
    rig.knobs("head", (0, 90.2, -32), (4.6, 1.8, 5.0), 20, 109, (0.4, 1.0), mat="face", where=lambda x, y, z, *n: n[1] > 0.3)
    for side, sg in SIDES:
        rig.blob("head", (sg * 5.6, 87.8, -22.8), (2.2, 2.9, 4.0), "face", layers=4)
        rig.cube("head", (sg * 5.4 - 0.9, 89.0, -30.2), (1.8, 1.6, 2.4), "socket")
        rig.cube("head", (sg * 5.8 - 0.55, 89.3, -29.9), (1.1, 1.1, 1.1), "eye")
        rig.cube("head", (sg * 2.0 - 0.45, 88.8, -37.4), (0.9, 0.8, 0.5), "socket")
        rig.cube("head", (sg * 5.0 - 0.8, 88.0, -29.4), (1.6, 0.8, 2.6), "face")
        br = rig.bone(f"brow_{side}", "head", (sg * 3.0, 92.0, -27))
        rig.cube(br, (sg * 4.3 - 2.6, 90.8, -31.0), (5.2, 2.0, 4.4), "face", rot=(0, 0, -sg * 12), pivot=(sg * 4.3, 91.8, -29))
        rig.knobs(br, (sg * 4.3, 92.4, -29.5), (2.4, 0.8, 2.0), 6, 1080 + sg, (0.4, 0.9), mat="face")
        el = rig.bone(f"eyelid_{side}", "head", (sg * 5.8, 90.5, -29.4))
        rig.cube(el, (sg * 5.8 - 0.7, 90.0, -30.25), (1.4, 0.6, 1.5), "face")
    rig.cube("head", (-4.4, 86.0, -35.0), (8.8, 0.8, 9.6), "mouth", faces={"up": None})
    for side, sg in SIDES:
        rig.cube("head", (sg * 4.15 - 0.25, 83.6, -34.0), (0.5, 2.6, 11.0), "mouth")
    rr = random.Random(77)
    rig.bone("lip_upper", "head", (0, 87.5, -30))
    rig.cube("lip_upper", (-4.8, 86.2, -36.6), (9.6, 1.0, 1.0), "face")
    for side, sg in SIDES:
        rig.cube("lip_upper", (sg * 4.85 - 0.5, 86.0, -35.8), (1.0, 1.2, 12.0), "face")
    for i in range(8):
        x = -3.7 + i * 1.06
        ln = rr.uniform(1.0, 1.7) * (1.45 if i in (1, 6) else 1.0)
        rig.cube("head", (x - 0.38, 86.3 - ln, -36.0 + abs(x) * 0.22), (0.76, ln, 0.76), "tooth",
                 rot=(rr.uniform(-8, 8), 0, rr.uniform(-6, 6)), pivot=(x, 86.3, -35.6))
    for side, sg in SIDES:
        for k in range(8):
            ln = rr.uniform(0.9, 1.6)
            rig.cube("head", (sg * 4.4 - 0.36, 86.3 - ln, -34.4 + k * 1.3), (0.72, ln, 0.72), "tooth",
                     rot=(rr.uniform(-6, 6), 0, sg * rr.uniform(0, 8)), pivot=(sg * 4.4, 86.3, -34.0 + k * 1.3))

    rig.bone("jaw", "head", (0, 86.6, -22))
    rig.blob("jaw", (0, 84.6, -28.5), (4.9, 1.8, 6.6), "face", layers=6, axis="z")
    rig.cube("jaw", (-3.9, 82.6, -33.0), (7.8, 1.4, 9.5), "belly")
    rig.cube("jaw", (-4.1, 85.9, -34.6), (8.2, 0.5, 12.0), "mouth", faces={"down": None})
    for side, sg in SIDES:
        rig.blob("jaw", (sg * 4.6, 84.4, -24.8), (1.3, 2.0, 3.8), "face", layers=4)
        for k in range(7):
            ln = rr.uniform(0.8, 1.4)
            rig.cube("jaw", (sg * 4.0 - 0.35, 86.1, -33.6 + k * 1.35), (0.7, ln, 0.7), "tooth",
                     rot=(rr.uniform(-6, 6), 0, sg * rr.uniform(0, 6)), pivot=(sg * 4.0, 86.1, -33.3 + k * 1.35))
    for i in range(7):
        x = -3.0 + i * 1.0
        ln = rr.uniform(0.9, 1.4) * (1.35 if i in (1, 5) else 1.0)
        rig.cube("jaw", (x - 0.36, 86.1, -34.7 + abs(x) * 0.22), (0.72, ln, 0.72), "tooth")
    rig.knobs("jaw", (0, 84, -28), (5, 1.6, 6.5), 18, 110, (0.5, 1.2), mat="face", where=lambda x, y, z, *n: y < 84.4)
    rig.bone("lip_lower", "jaw", (0, 85.5, -30))
    rig.cube("lip_lower", (-4.4, 85.0, -35.4), (8.8, 1.0, 1.0), "face")
    for side, sg in SIDES:
        rig.cube("lip_lower", (sg * 4.5 - 0.5, 85.0, -34.6), (1.0, 1.0, 11.0), "face")
    rig.bone("tongue1", "jaw", (0, 86.3, -24.5))
    rig.cube("tongue1", (-2.5, 85.9, -29.5), (5.0, 1.0, 5.5), "tongue")
    rig.bone("tongue2", "tongue1", (0, 86.3, -29.5))
    rig.cube("tongue2", (-2.1, 85.95, -33.0), (4.2, 0.85, 3.8), "tongue")

    # ---------------------------------------------------------------- arms
    for side, sg in SIDES:
        sh = rig.bone(f"shoulder_{side}", "chest", (sg * 13, 69, -4))
        rig.blob(sh, (sg * 16.8, 67, -6.5), (5.8, 5.2, 5.8), "skin2", layers=6)
        rig.knobs(sh, (sg * 16.8, 67, -6.5), (5.8, 5.2, 5.8), 26, 119 + sg, (0.9, 2.2))
        ua = rig.bone(f"arm_{side}", sh, (sg * 17.5, 67, -6.5))
        rig.blob(ua, (sg * 19.3, 62.2, -9), (4.6, 6.6, 4.6), "skin", layers=7)
        rig.blob(ua, (sg * 19.8, 63.5, -10.8), (3.0, 4.0, 2.8), "skin", layers=4)
        rig.knobs(ua, (sg * 19.3, 62.2, -9), (4.6, 6.6, 4.6), 34, 120 + sg, (0.8, 2.0))
        fa = rig.bone(f"forearm_{side}", ua, (sg * 19.5, 57.5, -11))
        rig.blob(fa, (sg * 19.5, 56.6, -16), (3.9, 3.9, 6.6), "skin", layers=7, axis="z")
        rig.knobs(fa, (sg * 19.5, 56.6, -16), (3.9, 3.9, 6.6), 26, 122 + sg, (0.7, 1.6))
        hd = rig.bone(f"hand_{side}", fa, (sg * 19.5, 56.5, -22))
        rig.blob(hd, (sg * 19.5, 56.0, -23.6), (2.8, 2.3, 2.2), "skin", layers=4)
        for k in range(4):
            fx = sg * 19.5 + (k - 1.5) * 1.4 * sg
            spread = (k - 1.5) * 7 * sg
            f1 = rig.bone(f"finger{k}_1_{side}", hd, (fx, 56.4, -25.2), rot=(14 + k * 3, -spread, 0))
            rig.cube(f1, (fx - 0.6, 55.7, -27.8), (1.2, 1.3, 2.8), "skin")
            f2 = rig.bone(f"finger{k}_2_{side}", f1, (fx, 56.3, -27.6), rot=(22, 0, 0))
            rig.cube(f2, (fx - 0.5, 55.75, -29.8), (1.0, 1.15, 2.3), "skin")
            f3 = rig.bone(f"finger{k}_3_{side}", f2, (fx, 56.3, -29.6), rot=(34, 0, 0))
            rig.cube(f3, (fx - 0.4, 55.8, -31.6), (0.8, 1.0, 2.1), "claw")
            rig.cube(f3, (fx - 0.25, 55.9, -32.4), (0.5, 0.7, 1.0), "claw", rot=(28, 0, 0), pivot=(fx, 56.2, -31.6))
        t1 = rig.bone(f"thumb_1_{side}", hd, (sg * 17.3, 55.6, -23.5), rot=(10, -sg * 28, 0))
        rig.cube(t1, (sg * 17.3 - 0.65, 54.9, -25.9), (1.3, 1.3, 2.4), "skin")
        t2 = rig.bone(f"thumb_2_{side}", t1, (sg * 17.3, 55.5, -25.8), rot=(40, 0, 0))
        rig.cube(t2, (sg * 17.3 - 0.4, 54.95, -27.7), (0.8, 1.0, 2.0), "claw")

    # ---------------------------------------------------------------- legs
    for side, sg in SIDES:
        th = rig.bone(f"thigh_{side}", "body", (sg * 12, 40, 2))
        rig.blob(th, (sg * 14.5, 31, 0), (11.5, 13, 13), "skin2", layers=9)
        rig.blob(th, (sg * 15, 23, -4), (9.2, 6, 9.2), "skin", layers=6)
        rig.blob(th, (sg * 16.5, 33, -4), (8, 8, 7), "skin2", layers=6)
        rig.knobs(th, (sg * 14.5, 31, 0), (11.5, 13, 13), 130, 130 + sg, (1.2, 3.4))
        sn = rig.bone(f"shin_{side}", th, (sg * 15, 22, -3))
        rig.blob(sn, (sg * 15, 13, -1.5), (8.6, 10, 9), "skin", layers=8)
        rig.rings(sn, (sg * 15, 13, -1.5), (8.6, 10, 9), (9.0, 15.0), grow=0.6, th=1.0)
        rig.knobs(sn, (sg * 15, 13, -1.5), (8.6, 10, 9), 70, 132 + sg, (1.0, 2.4))
        ft = rig.bone(f"foot_{side}", sn, (sg * 15, 5, -1))
        rig.cube(ft, (sg * 15 - 8.5, 0, -10.0), (17, 4.5, 17), "skin2", faces={"down": "sole"})
        rig.blob(ft, (sg * 15, 5.0, -2.0), (8.0, 2.4, 7.5), "skin", layers=4)
        rig.knobs(ft, (sg * 15, 3.5, -1.5), (8.5, 3, 8.5), 34, 134 + sg, (0.8, 1.8), where=lambda x, y, z, *n: y > 2)
        for k, dx in enumerate((-5.4, 0.0, 5.4)):
            tx = sg * 15 + dx * sg
            t1 = rig.bone(f"toe{k}_1_{side}", ft, (tx, 2.0, -10.0), rot=(0, -dx * sg * 2.2, 0))
            rig.blob(t1, (tx, 1.9, -12.3), (2.3, 1.9, 2.6), "skin", layers=4, axis="z")
            t2 = rig.bone(f"toe{k}_2_{side}", t1, (tx, 1.6, -14.6))
            rig.cube(t2, (tx - 1.6, 0, -16.6), (3.2, 2.6, 2.4), "claw", rot=(-10, 0, 0), pivot=(tx, 0, -14.6))
            rig.cube(t2, (tx - 1.0, 0, -17.4), (2.0, 1.6, 1.2), "claw")
        dc = rig.bone(f"dewclaw_{side}", ft, (sg * 15 - sg * 6.5, 1.5, 4.5))
        rig.cube(dc, (sg * 15 - sg * 6.5 - 1.2, 0, 4.0), (2.4, 2.2, 3.2), "claw")

    # ---------------------------------------------------------------- tail: 16 segments, hooks up at the tip
    lens = [11, 11, 11, 11, 10.5, 10.5, 10, 10, 9.5, 9.5, 9, 9, 8.5, 8, 8, 7.5]
    rads = [13.5, 12.6, 11.7, 10.8, 9.9, 9.1, 8.3, 7.6, 6.9, 6.2, 5.6, 5.0, 4.4, 3.8, 3.2, 2.6, 1.9]
    bend = [(-16, 0), (-13, 2), (-11, 3), (-6, 4), (10, 5), (14, 6), (12, 6), (4, 5),
            (0, 4), (0, 3), (8, 1), (14, -1), (18, -3), (22, -5), (24, -5), (24, -5)]
    z = 12.0
    parent = "body"
    tails = []
    for i, L in enumerate(lens):
        name = rig.bone(f"tail{i + 1}", parent, (0, 40, z), rot=(bend[i][0], bend[i][1], 0))
        r0, r1 = rads[i], rads[i + 1]
        mat = "skin2" if i < 6 else "skin"
        for k in range(3):
            a = r0 + (r1 - r0) * (k / 3)
            z0, z1 = z + L * k / 3 - 0.5, z + L * (k + 1) / 3 + 0.5
            for fa, fb in ((1.0, 0.72), (0.9, 0.9), (0.72, 1.0)):
                rig.cube(name, (-a * fa, 40 - a * fb * 0.95, z0), (2 * a * fa, 2 * a * fb * 0.95, z1 - z0), mat, proj=True)
        rig.cube(name, (-r0 * 0.55, 40 - r0 * 0.95 - 0.5, z + 0.8), (r0 * 1.1, 1.0, L - 1.6), "belly")
        rig.knobs(name, (0, 40, z + L / 2), (r0 * 0.93, r0 * 0.93, L / 2), int(14 + r0 * 3), 140 + i, (0.7, max(1.0, r0 * 0.27)),
                  where=lambda x, y, zz, nx, ny, nz: ny > -0.6 and abs(nz) < 0.85)
        tails.append((name, z, L, r0, r1))
        parent = name
        z += L

    # ---------------------------------------------------------------- dorsal plates, three rows
    n = [0]
    total_tail = sum(lens)

    def P_(par, base, h, w, back, out, s_pos, side_row=False):
        n[0] += 1
        travel = h * (0.28 if side_row else 0.38)
        plate(rig, par, n[0], base, h, w, back, out, n[0] * 31, s_pos, travel, "plate" if n[0] % 2 else "plate2")

    spine = [  # parent, base, height, width, lean back, side spacing, position along the spine (1 = head)
        ("neck3", (0, 89.5, -11.5), 7.5, 6.0, 12, 3.0, 1.0),
        ("neck2", (0, 86.0, -6.5), 11.0, 8.0, 18, 3.8, 0.97),
        ("neck1", (0, 83.0, -1.5), 15.0, 10.0, 26, 5.0, 0.94),
        ("neck1", (0, 79.5, 3.5), 19.0, 12.0, 32, 6.0, 0.91),
        ("chest", (0, 75.5, 8.5), 23.5, 14.5, 38, 6.8, 0.88),
        ("chest", (0, 70.0, 13.0), 27.0, 16.0, 44, 7.6, 0.85),
        ("chest", (0, 63.5, 13.8), 27.0, 16.0, 50, 8.0, 0.82),
        ("belly", (0, 56.5, 13.5), 23.0, 14.5, 56, 7.8, 0.79),
        ("body", (0, 50.0, 13.5), 19.0, 12.5, 62, 7.2, 0.76),
        ("body", (0, 46.0, 16.0), 15.5, 11.0, 66, 6.6, 0.73),
    ]
    for par, (x, y, zz), h, w, back, sp, s in spine:
        P_(par, (x, y, zz), h, w, back, 0, s)
        for side, sg in SIDES:
            P_(par, (sg * sp * 1.15, y - 2.0, zz - 1.6), h * 0.5, w * 0.55, back - 4, -sg * 30, s - 0.005, True)
    acc = 0.0
    for i, (name, z0, L, r0, r1) in enumerate(tails):
        for k in range(2):
            f = (k + 0.5) / 2
            r = r0 + (r1 - r0) * f
            zz = z0 + L * f
            s = 0.72 * (1 - (acc + L * f) / total_tail)
            h = max(2.0, 13.5 * (1 - i / 16.5) ** 1.15 - k * 0.4)
            w = max(1.8, h * 0.62)
            P_(name, (0, 40 + r * 0.9, zz), h, w, 16 - i * 0.4, 0, s)
            if h > 3.6:
                for side, sg in SIDES:
                    P_(name, (sg * r * 0.45, 40 + r * 0.78, zz + L * 0.22), h * 0.55, w * 0.6, 14, -sg * 30, s - 0.005, True)
        acc += L
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
    with open(os.path.join(OUT, "plates.json"), "w") as f:
        json.dump([{"bone": b, "s": round(s, 4), "travel": round(t, 3)} for b, s, t in sorted(PLATES, key=lambda p: p[1])], f, indent=0)
    nc = sum(len(b.get("cubes", [])) for b in rig.bones)
    print(f"bones {len(rig.bones)} cubes {nc} plates {len(PLATES)}")


if __name__ == "__main__":
    main()
