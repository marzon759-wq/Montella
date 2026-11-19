#!/usr/bin/env python3
"""The Mimic: geometry and texture.

A tall, starved, black-furred thing with three pairs of arms, enormous clawed hands, digitigrade legs and a pale
skull of a face with a jaw that opens far too wide. Everything is built from many small, rotated boxes so the
silhouette reads as organic rather than blocky; the fur is a cloud of thin strands.

Model units: 16 px = 1 block. Front of the model faces -z (north). Output: bedrock geometry + 1024px texture.
"""
import json, math, os, zlib, random, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter


def shash(v):
    return zlib.crc32(str(v).encode())


HERE = os.path.dirname(os.path.abspath(__file__))
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "out")
TEX = 1024
S = 8  # texels per model pixel when cutting a face out of a material patch
R = random.Random(1987)


# ====================================================================== texture
def noise(w, h, scale, seed, octaves=4):
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        sx = max(2, int(w / scale * 2 ** o)), max(2, int(h / scale * 2 ** o))
        small = rng.random((sx[1], sx[0]))
        img = Image.fromarray((small * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += np.asarray(img, dtype=float) / 255 * amp
        tot += amp
        amp *= 0.5
    return out / tot


def streaks(w, h, seed, density=0.35, vertical=True):
    """matted fur: long thin strands with highlights"""
    rng = np.random.default_rng(seed)
    img = np.zeros((h, w))
    n = int(w * h * density / 40)
    for _ in range(n):
        x = rng.integers(0, w)
        y0 = rng.integers(-h // 2, h)
        ln = rng.integers(h // 6, h // 2)
        val = rng.random() ** 2
        drift = rng.normal(0, 0.15)
        for k in range(ln):
            yy = y0 + k
            xx = int(x + drift * k) % w
            if 0 <= yy < h:
                img[yy, xx] = max(img[yy, xx], val * (1 - k / ln * 0.6))
    if not vertical:
        img = img.T
    return img


def to_rgb(arr, lo, hi):
    lo, hi = np.array(lo, float), np.array(hi, float)
    a = np.clip(arr, 0, 1)[..., None]
    return (lo + (hi - lo) * a).astype(np.uint8)


def splatter(img, n, color, seed, rmin=2, rmax=14, alpha=200):
    rng = random.Random(seed)
    ov = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    w, h = img.size
    for _ in range(n):
        x, y = rng.randrange(w), rng.randrange(h)
        r = rng.randint(rmin, rmax)
        a = rng.randint(alpha // 2, alpha)
        d.ellipse([x - r, y - r, x + r, y + r], fill=color + (a,))
        for _ in range(rng.randint(2, 7)):  # droplets and a drip
            dx, dy = rng.randint(-3 * r, 3 * r), rng.randint(-3 * r, 3 * r)
            rr = max(1, r // rng.randint(3, 6))
            d.ellipse([x + dx - rr, y + dy - rr, x + dx + rr, y + dy + rr], fill=color + (a,))
        if rng.random() < 0.6:
            ln = rng.randint(r, r * 5)
            d.line([(x, y), (x + rng.randint(-2, 2), y + ln)], fill=color + (a,), width=max(1, r // 3))
    img.alpha_composite(ov.filter(ImageFilter.GaussianBlur(0.6)))


def make_atlas():
    atlas = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    glow = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    P = {}

    def put(name, img, x, y):
        atlas.paste(img, (x, y))
        P[name] = (x, y, img.size[0], img.size[1])

    # --- fur: almost black, brown-grey highlights, vertical strands
    w = h = 256
    f = streaks(w, h, 1, 0.9) * 0.8 + noise(w, h, 18, 2) * 0.35
    fur = Image.fromarray(to_rgb(f, (3, 3, 4), (46, 45, 50))).convert("RGBA")
    splatter(fur, 10, (60, 6, 6), 11, 2, 9, 150)
    put("fur", fur, 0, 0)
    # --- hide: the stretched skin under the fur, grey-black and wet
    h2 = noise(w, h, 10, 3) * 0.7 + streaks(w, h, 4, 0.3) * 0.4
    hide = Image.fromarray(to_rgb(h2, (6, 6, 7), (40, 38, 42))).convert("RGBA")
    splatter(hide, 14, (70, 8, 8), 12, 2, 10, 170)
    put("hide", hide, 256, 0)
    # --- bone: off-white, cracked, stained
    b = noise(w, h, 14, 5) * 0.6 + noise(w, h, 4, 6, 2) * 0.4
    bone = Image.fromarray(to_rgb(b, (128, 118, 100), (214, 206, 186))).convert("RGBA")
    d = ImageDraw.Draw(bone)
    rr = random.Random(7)
    for _ in range(40):
        x, y = rr.randrange(w), rr.randrange(h)
        for _ in range(rr.randint(4, 14)):
            nx, ny = x + rr.randint(-6, 6), y + rr.randint(-6, 6)
            d.line([(x, y), (nx, ny)], fill=(70, 60, 50, 255), width=1)
            x, y = nx, ny
    splatter(bone, 8, (90, 30, 18), 13, 3, 14, 110)
    put("bone", bone, 512, 0)
    # --- claw: glossy black keratin with a long highlight
    c = noise(w, h, 6, 8) * 0.5 + np.linspace(0, 1, w)[None, :] * 0.0
    claw = Image.fromarray(to_rgb(c, (4, 3, 3), (40, 34, 32))).convert("RGBA")
    d = ImageDraw.Draw(claw)
    for x in range(0, w, 16):
        d.line([(x + 5, 0), (x + 5, h)], fill=(96, 88, 84, 255), width=1)
    splatter(claw, 18, (110, 6, 6), 14, 2, 8, 220)
    put("claw", claw, 768, 0)
    # --- tooth: ivory to rotten brown at the root
    t = np.tile(np.linspace(1, 0, h)[:, None], (1, w)) * 0.7 + noise(w, h, 8, 9) * 0.3
    tooth = Image.fromarray(to_rgb(t, (92, 70, 40), (222, 210, 168))).convert("RGBA")
    splatter(tooth, 12, (120, 10, 10), 15, 2, 6, 180)
    put("tooth", tooth, 0, 256)
    # --- gum / mouth: black-red, wet
    g = noise(w, h, 8, 10) * 0.8 + noise(w, h, 2, 11, 2) * 0.2
    gum = Image.fromarray(to_rgb(g, (18, 0, 2), (132, 18, 22))).convert("RGBA")
    put("gum", gum, 256, 256)
    # --- the face: drawn as one picture so the eyes and cavities sit exactly where they should
    FW, FH = 224, 224
    fa = noise(FW, FH, 14, 21) * 0.55 + noise(FW, FH, 4, 22, 2) * 0.25
    face = Image.fromarray(to_rgb(fa, (138, 128, 112), (226, 218, 198))).convert("RGBA")
    d = ImageDraw.Draw(face)
    # stretched skin shading toward the edges
    sh = Image.new("L", (FW, FH), 0)
    ds = ImageDraw.Draw(sh)
    for i in range(30):
        ds.rectangle([i * 2, i * 2, FW - i * 2, FH - i * 2], outline=255 - i * 8)
    face.alpha_composite(Image.merge("RGBA", (Image.new("L", (FW, FH), 20),) * 3 + (sh.filter(ImageFilter.GaussianBlur(10)).point(lambda v: v // 2),)))
    gl = ImageDraw.Draw(glow)
    for cx in (62, 162):
        cy = 100
        d.ellipse([cx - 40, cy - 46, cx + 40, cy + 46], fill=(70, 58, 50, 255))  # bruised ring
        d.ellipse([cx - 32, cy - 38, cx + 32, cy + 38], fill=(8, 4, 4, 255))  # socket
        d.ellipse([cx - 22, cy - 24, cx + 22, cy + 24], fill=(2, 0, 0, 255))
        # a pale, pinprick pupil - looking slightly inward
        px = cx + (8 if cx < FW / 2 else -8)
        d.ellipse([px - 6, cy - 6, px + 6, cy + 6], fill=(236, 230, 210, 255))
        d.ellipse([px - 2, cy - 2, px + 2, cy + 2], fill=(255, 255, 255, 255))
        gl.ellipse([512 + 256 // 2 - FW // 2 + px - 6 + 16, 256 + 16 + cy - 6, 512 + 256 // 2 - FW // 2 + px + 6 + 16, 256 + 16 + cy + 6], fill=(255, 255, 255, 255))
        # weeping streak
        d.line([(cx, cy + 38), (cx - 3, cy + 88)], fill=(60, 6, 6, 230), width=4)
    # nasal cavity
    d.polygon([(112, 140), (98, 176), (112, 170), (126, 176)], fill=(10, 4, 4, 255))
    # cheek cracks
    rr = random.Random(23)
    for sx in (20, 200):
        x, y = sx, 150
        for _ in range(10):
            nx, ny = x + rr.randint(-8, 8), y + rr.randint(2, 8)
            d.line([(x, y), (nx, ny)], fill=(60, 50, 44, 255), width=2)
            x, y = nx, ny
    splatter(face, 6, (100, 10, 10), 24, 2, 7, 160)
    put("face", face, 512 + 16, 256 + 16)
    # --- eyes glowmask region also holds a copy of the pupils (the region index above)
    # --- inner mouth: deep throat
    th = Image.new("RGBA", (128, 128), (0, 0, 0, 255))
    dt = ImageDraw.Draw(th)
    for i in range(30):
        dt.ellipse([i * 2, i * 2, 128 - i * 2, 128 - i * 2], fill=(40 - i, 0, 2, 255))
    put("throat", th, 768, 256)
    # --- the upper jaw front: gums with tooth roots
    uj = gum.copy().resize((128, 48))
    put("maxilla", uj, 768, 400)
    return atlas, glow, P


# ====================================================================== geometry
class Rig:
    def __init__(self, patches):
        self.bones = []
        self.names = set()
        self.P = patches

    def bone(self, name, parent, pivot, rot=None):
        assert name not in self.names, name
        self.names.add(name)
        b = {"name": name, "pivot": [round(v, 3) for v in pivot]}
        if parent:
            b["parent"] = parent
        if rot:
            b["rotation"] = [round(v, 3) for v in rot]
        b["cubes"] = []
        self.bones.append(b)
        return b

    def find(self, name):
        for b in self.bones:
            if b["name"] == name:
                return b
        raise KeyError(name)

    def uv(self, mat, fw, fh, face_seed):
        x, y, w, h = self.P[mat]
        tw, th = max(1, fw * S), max(1, fh * S)
        tw, th = min(tw, w), min(th, h)
        rr = random.Random(face_seed)
        ox = x + rr.randint(0, max(0, w - int(tw)))
        oy = y + rr.randint(0, max(0, h - int(th)))
        return {"uv": [ox, oy], "uv_size": [round(tw, 3), round(th, 3)]}

    def cube(self, bone, origin, size, mat="fur", rot=None, pivot=None, faces=None, inflate=None):
        """origin/size in model px. rot rotates the cube about pivot (default: its centre)."""
        w, h, d = size
        seed = R.randint(0, 1 << 30)
        dims = {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}
        uv = {}
        for f, (fw, fh) in dims.items():
            m = (faces or {}).get(f, mat)
            if m is None:
                continue
            if isinstance(m, tuple):  # an exact region: (mat, None) -> whole patch
                x, y, pw, ph = self.P[m[0]]
                uv[f] = {"uv": [x, y], "uv_size": [pw, ph]}
            else:
                uv[f] = self.uv(m, fw, fh, seed + shash(f) % 997)
        c = {"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": uv}
        if rot:
            c["rotation"] = [round(v, 3) for v in rot]
            c["pivot"] = [round(v, 3) for v in (pivot or (origin[0] + w / 2, origin[1] + h / 2, origin[2] + d / 2))]
        if inflate:
            c["inflate"] = inflate
        self.find(bone)["cubes"].append(c)

    def limb(self, bone, top, length, thick, mat="hide", taper=1.0, segments=3, wobble=0.25, seed=0):
        """a limb hanging straight down from `top`, built from a few overlapping, slightly offset boxes"""
        rr = random.Random(seed)
        x, y, z = top
        seg = length / segments
        for i in range(segments):
            t = thick * (1 - (1 - taper) * (i / max(1, segments - 1)))
            ox, oz = rr.uniform(-wobble, wobble), rr.uniform(-wobble, wobble)
            self.cube(bone, (x - t / 2 + ox, y - seg * (i + 1) - 0.15, z - t / 2 + oz), (t, seg + 0.3, t), mat)
        # a knot at the joint
        self.cube(bone, (x - thick * 0.6, y - 0.6, z - thick * 0.6), (thick * 1.2, 1.2, thick * 1.2), mat)

    def strands(self, bone, centre, radius, count, length, seed, ymin=None, ymax=None, droop=55, thick=(0.35, 0.7), back_bias=0.0, mat="fur"):
        """a cloud of thin fur strands growing out of an ellipsoid surface, hanging down and outward"""
        rr = random.Random(seed)
        cx, cy, cz = centre
        rx, ry, rz = radius
        for _ in range(count):
            a = rr.uniform(0, math.tau)
            if back_bias and rr.random() < back_bias:
                a = rr.uniform(math.pi * 0.25, math.pi * 0.75)  # toward +z (the back)
            v = rr.uniform(-0.9, 0.9)
            px = cx + math.cos(a) * rx * math.sqrt(1 - v * v)
            pz = cz + math.sin(a) * rz * math.sqrt(1 - v * v)
            py = cy + v * ry
            if ymin is not None and py < ymin or ymax is not None and py > ymax:
                continue
            ln = rr.uniform(*length)
            t = rr.uniform(*thick)
            # a strand hangs from its root: tilt outward by the angle around the body, plus some chaos
            out_deg = math.degrees(a)
            tilt = rr.uniform(droop * 0.4, droop)
            rx_deg = -math.sin(a) * tilt + rr.uniform(-12, 12)
            rz_deg = math.cos(a) * tilt + rr.uniform(-12, 12)
            self.cube(bone, (px - t / 2, py - ln, pz - t / 2), (t, ln, t), mat,
                      rot=(rx_deg, rr.uniform(-40, 40), rz_deg), pivot=(px, py, pz))

    def scale_bones(self, names, centre, k):
        """grow a group of bones (and their cubes) about a point"""
        c = centre
        sc = lambda v: [c[i] + (v[i] - c[i]) * k for i in range(3)]
        for b in self.bones:
            if b["name"] not in names:
                continue
            b["pivot"] = [round(v, 3) for v in sc(b["pivot"])]
            for cu in b["cubes"]:
                cu["origin"] = [round(v, 3) for v in sc(cu["origin"])]
                cu["size"] = [round(v * k, 3) for v in cu["size"]]
                if "pivot" in cu:
                    cu["pivot"] = [round(v, 3) for v in sc(cu["pivot"])]

    def json(self):
        bones = []
        for b in self.bones:
            bb = dict(b)
            if not bb["cubes"]:
                bb.pop("cubes")
            bones.append(bb)
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry.mimic", "texture_width": TEX, "texture_height": TEX,
                            "visible_bounds_width": 8, "visible_bounds_height": 6, "visible_bounds_offset": [0, 2.5, 0]},
            "bones": bones}]}


SIDES = (("l", 1), ("r", -1))


def hand(rig, side, sg, tag, wrist, palm=(3.0, 3.2, 1.2), fingers=(4.2, 3.6, 3.0), fthick=0.75, claw=2.2, nfing=4, spread=0.85):
    """palm hanging down from the wrist, long jointed fingers, curved claws; each finger joint is its own bone"""
    wx, wy, wz = wrist
    hb = f"hand{tag}_{side}"
    pw, ph, pd = palm
    rig.cube(hb, (wx - pw / 2, wy - ph, wz - pd / 2), (pw, ph, pd), "hide", faces={"north": "hide"})
    rig.cube(hb, (wx - pw / 2 + 0.2, wy - ph * 0.55, wz - pd / 2 - 0.25), (pw - 0.4, ph * 0.5, 0.4), "hide")  # knuckle pad
    for i in range(nfing):
        fx = wx - pw / 2 + 0.35 + i * (pw - 0.7) / max(1, nfing - 1)
        top = wy - ph
        parent = hb
        lens = [L * (1.0 - 0.12 * abs(i - (nfing - 1) / 2)) for L in fingers]
        y = top
        for j, seg in enumerate("abc"):
            bn = f"f{i}{seg}{tag}_{side}"
            rig.bone(bn, parent, (fx, y, wz), rot=[0, 0, sg * (i - (nfing - 1) / 2) * spread * 6] if j == 0 else None)
            th = fthick * (1 - 0.18 * j)
            rig.cube(bn, (fx - th / 2, y - lens[j], wz - th / 2), (th, lens[j] + 0.15, th), "hide")
            rig.cube(bn, (fx - th * 0.65, y - 0.5, wz - th * 0.65), (th * 1.3, 0.9, th * 1.3), "hide")  # knuckle
            y -= lens[j]
            parent = bn
        # the claw: two tapered boxes, curling inward (toward -z)
        cb = f"f{i}c{tag}_{side}"
        rig.cube(cb, (fx - 0.3, y - claw, wz - 0.3), (0.6, claw, 0.6), "claw", rot=(-18, 0, 0), pivot=(fx, y, wz))
        rig.cube(cb, (fx - 0.18, y - claw * 1.55, wz - 0.95), (0.36, claw * 0.75, 0.36), "claw", rot=(-38, 0, 0), pivot=(fx, y - claw * 0.8, wz - 0.4))
    # thumb
    tb = f"thumb{tag}_{side}"
    rig.bone(tb, hb, (wx + sg * pw / 2, wy - 0.8, wz - 0.2), rot=[0, 0, sg * 22])
    rig.cube(tb, (wx + sg * pw / 2 - 0.35, wy - 0.8 - fingers[0] * 0.9, wz - 0.55), (0.7, fingers[0] * 0.9, 0.7), "hide")
    rig.cube(tb, (wx + sg * pw / 2 - 0.25, wy - 0.8 - fingers[0] * 0.9 - claw, wz - 0.45), (0.5, claw, 0.5), "claw", rot=(-20, 0, 0),
             pivot=(wx + sg * pw / 2, wy - 0.8 - fingers[0] * 0.9, wz - 0.2))


def arm(rig, side, sg, tag, parent, shoulder, upper, fore, thick, hand_kw, fur=True):
    sx, sy, sz = shoulder
    a = f"arm{tag}_{side}"
    f = f"fore{tag}_{side}"
    h = f"hand{tag}_{side}"
    rig.bone(a, parent, shoulder)
    rig.limb(a, shoulder, upper, thick, "hide", taper=0.8, seed=shash(a) % 1000)
    # bony elbow spur
    rig.cube(a, (sx - 0.4, sy - upper - 0.8, sz + thick * 0.3), (0.8, 1.6, 1.4), "bone", rot=(25, 0, 0))
    if fur:
        rig.strands(a, (sx, sy - upper * 0.35, sz), (thick * 0.7, upper * 0.4, thick * 0.7), 40, (3.0, 8.0), shash(a) % 999, droop=70)
    elbow = (sx, sy - upper, sz)
    rig.bone(f, a, elbow)
    rig.limb(f, elbow, fore, thick * 0.75, "hide", taper=0.75, seed=shash(f) % 1000)
    if fur:
        rig.strands(f, (sx, elbow[1] - fore * 0.25, sz), (thick * 0.5, fore * 0.3, thick * 0.5), 14, (2.0, 4.5), shash(f) % 999, droop=75, thick=(0.3, 0.5))
    wrist = (sx, elbow[1] - fore, sz)
    rig.bone(h, f, wrist)
    hand(rig, side, sg, tag, wrist, **hand_kw)


def build(P):
    rig = Rig(P)
    rig.bone("root", None, (0, 0, 0))
    rig.bone("all", "root", (0, 0, 0))
    rig.bone("hips", "all", (0, 25, 0.5))
    # ---------------------------------------------------------------- pelvis and spine
    rig.cube("hips", (-4.6, 23.0, -2.4), (9.2, 5, 6.0), "hide")
    rig.cube("hips", (-3.6, 22.0, -1.8), (7.2, 1.8, 4.6), "hide")
    rig.strands("hips", (0, 25.5, 0.5), (5.2, 2.8, 3.8), 160, (4.0, 12.0), 101, droop=35, thick=(0.5, 1.1))
    rig.bone("spine", "hips", (0, 27, 0.6))
    rig.cube("spine", (-3.6, 27, -2.0), (7.2, 6, 5.0), "hide")
    rig.cube("spine", (-2.8, 28, 2.6), (5.6, 4, 1.2), "hide")
    for i in range(5):  # vertebrae pushing through the fur
        rig.cube("spine", (-0.6, 27.5 + i * 1.3, 3.2), (1.2, 0.8, 1.2), "bone", rot=(20, 0, 0))
    rig.strands("spine", (0, 30, 0.6), (4.4, 3.4, 3.4), 170, (4.0, 10.0), 102, droop=40, thick=(0.5, 1.1))
    rig.bone("chest", "spine", (0, 33, 0.8))
    rig.cube("chest", (-5.6, 33, -2.6), (11.2, 6.8, 6.8), "hide")
    rig.cube("chest", (-4.6, 39.6, -1.8), (9.2, 1.8, 5.4), "hide")
    rig.cube("chest", (-8.0, 37.4, -1.4), (16.0, 2.8, 4.6), "hide")  # shoulder yoke
    # ribs: pale arcs showing through the matted front
    for i in range(5):
        y = 34 + i * 1.15
        for sg in (1, -1):
            rig.cube("chest", (sg * 1.2 - (3.6 if sg < 0 else 0), y, -2.85), (3.6, 0.5, 0.5), "bone", rot=(0, sg * 18, sg * -14))
    for i in range(6):
        rig.cube("chest", (-0.55, 34 + i * 1.2, 4.0), (1.1, 0.8, 1.2), "bone", rot=(15, 0, 0))
    rig.strands("chest", (0, 36.5, 0.8), (6.4, 4.0, 4.2), 320, (4.0, 13.0), 103, droop=40, back_bias=0.35, thick=(0.5, 1.2))
    # ---------------------------------------------------------------- neck and head
    rig.bone("neck", "chest", (0, 40.5, -0.2))
    rig.cube("neck", (-1.8, 40.2, -1.8), (3.6, 4.3, 3.4), "hide", rot=(-14, 0, 0))
    for i in range(3):
        rig.cube("neck", (-0.45, 40.8 + i * 1.3, 0.7), (0.9, 0.7, 0.9), "bone", rot=(-14, 0, 0), pivot=(0, 40.5, -0.2))
    rig.strands("neck", (0, 42, -0.2), (1.8, 2.0, 1.8), 40, (3.0, 7.0), 104, droop=30)
    rig.cube("neck", (-0.9, 43.6, -1.6), (1.8, 2.6, 1.8), "hide", rot=(-20, 0, 0))
    rig.bone("head_look", "neck", (0, 45.6, -1.2))
    rig.bone("head", "head_look", (0, 45.6, -1.2))
    H = 1.6  # the whole head sits this much higher than the old layout; the jaw has room to drop
    # skull: a tall oval made of stepped boxes; the face plate carries the painted eyes
    rig.cube("head", (-3.8, 45.8 + H, -4.8), (7.6, 6.6, 7.2), "bone", faces={"north": ("face",)})
    rig.cube("head", (-3.3, 52.4 + H, -4.2), (6.6, 1.2, 6.2), "bone")
    rig.cube("head", (-2.6, 53.6 + H, -3.5), (5.2, 0.8, 4.8), "bone")
    rig.cube("head", (-4.2, 46.4 + H, -4.0), (8.4, 5.6, 5.6), "bone", faces={"north": None})  # temples
    rig.cube("head", (-3.6, 51.0 + H, -5.15), (7.2, 0.9, 0.6), "bone")  # brow ridge
    for sg in (1, -1):  # cheekbones jutting out, running back to the jaw hinge
        rig.cube("head", (sg * 3.1 - 0.9, 45.2 + H, -4.9), (1.8, 1.6, 2.6), "bone", rot=(0, sg * 20, 0))
        rig.cube("head", (sg * 3.4 - 0.6, 44.0 + H, -2.6), (1.2, 2.2, 3.2), "bone")
    rig.strands("head", (0, 50.6 + H, -0.3), (3.8, 3.4, 3.4), 80, (3.5, 12.0), 105, ymin=49.0 + H, droop=45, back_bias=0.6)
    # the upper jaw: gums and a row of long, uneven teeth
    top = 45.4 + H
    rig.cube("head", (-3.2, top - 0.4, -5.0), (6.4, 1.4, 4.4), "gum", faces={"north": ("maxilla",)})
    # the dark inside of the mouth: only seen when the jaw drops
    rig.cube("head", (-2.8, top - 6.5, -3.2), (5.6, 6.2, 3.6), "throat", faces={"north": ("throat",), "south": "gum", "up": None})
    rr = random.Random(55)
    for i in range(10):
        x = -2.9 + i * 0.64
        ln = rr.uniform(1.4, 2.4) * (1.45 if i in (1, 8) else 1.0)
        rig.cube("head", (x - 0.22, top - 0.3 - ln, -4.9 + rr.uniform(0, 0.4)), (0.44, ln, 0.5), "tooth",
                 rot=(rr.uniform(-8, 8), 0, rr.uniform(-6, 6)), pivot=(x, top - 0.3, -4.6))
    for sg in (1, -1):  # side teeth along the cheeks
        for k in range(4):
            ln = rr.uniform(1.0, 2.0)
            rig.cube("head", (sg * 2.95 - 0.2, top - 0.3 - ln, -4.0 + k * 0.9), (0.42, ln, 0.42), "tooth")
    # the jaw: hinged just behind the cheeks; animations swing it down AND pull it out of its sockets
    rig.bone("jaw", "head", (0, top, 0.2))
    floor = top - 3.2
    rig.cube("jaw", (-3.1, floor - 1.2, -5.0), (6.2, 1.3, 5.4), "bone")  # chin and mandible
    rig.cube("jaw", (-2.7, floor + 0.1, -4.7), (5.4, 0.5, 4.8), "gum")
    for sg in (1, -1):
        rig.cube("jaw", (sg * 3.0 - 0.6, floor - 1.0, -5.0), (1.2, 4.0, 1.2), "bone", rot=(-4, 0, 0))  # mandible corners
        rig.cube("jaw", (sg * 3.0 - 0.5, floor - 0.6, -3.8), (1.0, 3.6, 4.0), "hide")  # torn cheek skin
    for i in range(10):
        x = -2.8 + i * 0.62
        ln = rr.uniform(1.2, 2.2) * (1.4 if i in (2, 7) else 1.0)
        rig.cube("jaw", (x - 0.22, floor + 0.4, -4.8 + rr.uniform(0, 0.3)), (0.44, ln, 0.5), "tooth",
                 rot=(rr.uniform(-10, 4), 0, rr.uniform(-6, 6)), pivot=(x, floor + 0.4, -4.6))
    rig.strands("jaw", (0, floor - 0.6, -1.6), (3.0, 0.6, 3.0), 30, (2.5, 6.0), 106, droop=20)
    # a long tongue lying in the jaw, it can slide out
    rig.bone("tongue", "jaw", (0, floor + 0.6, -0.5))
    rig.cube("tongue", (-1.0, floor + 0.3, -3.6), (2.0, 0.6, 3.4), "gum")
    rig.bone("tongue2", "tongue", (0, floor + 0.6, -3.6))
    rig.cube("tongue2", (-0.8, floor + 0.35, -4.7), (1.6, 0.5, 1.2), "gum")  # stretched by scale in the animations
    rig.cube("tongue2", (-0.5, floor + 0.4, -5.0), (1.0, 0.4, 0.4), "gum")

    # ---------------------------------------------------------------- three pairs of arms
    big = dict(palm=(4.2, 4.4, 1.6), fingers=(5.6, 4.6, 3.8), fthick=1.05, claw=3.0)
    mid = dict(palm=(3.4, 3.6, 1.3), fingers=(4.6, 3.8, 3.2), fthick=0.85, claw=2.5)
    small = dict(palm=(2.4, 2.6, 1.0), fingers=(3.0, 2.6, 2.2), fthick=0.65, claw=1.8, nfing=3)
    for side, sg in SIDES:
        arm(rig, side, sg, "1", "chest", (sg * 7.4, 38.6, 0.2), 13.5, 14.5, 2.4, big)
        arm(rig, side, sg, "2", "chest", (sg * 6.0, 34.2, 0.6), 11.5, 12.5, 2.0, mid)
        arm(rig, side, sg, "3", "spine", (sg * 4.2, 29.2, 0.0), 6.5, 6.5, 1.4, small, fur=False)
    # ---------------------------------------------------------------- digitigrade legs
    for side, sg in SIDES:
        hipx = sg * 3.0
        rig.bone(f"thigh_{side}", "hips", (hipx, 24.0, 0.5))
        rig.limb(f"thigh_{side}", (hipx, 24.0, 0.5), 11.5, 2.9, "hide", taper=0.7, seed=7 + sg)
        rig.strands(f"thigh_{side}", (hipx, 20.0, 0.5), (2.2, 4.0, 2.2), 45, (3.5, 8.0), 110 + sg, droop=40, thick=(0.45, 0.9))
        rig.bone(f"shin_{side}", f"thigh_{side}", (hipx, 12.5, 0.5))
        rig.limb(f"shin_{side}", (hipx, 12.5, 0.5), 10.0, 2.0, "hide", taper=0.7, seed=9 + sg)
        rig.cube(f"shin_{side}", (hipx - 0.5, 11.0, 1.3), (1.0, 2.0, 1.0), "bone", rot=(-25, 0, 0))  # heel spur
        rig.bone(f"foot_{side}", f"shin_{side}", (hipx, 2.5, 0.5))
        rig.cube(f"foot_{side}", (hipx - 0.8, 0.0, -0.6), (1.6, 2.6, 1.8), "hide")
        for k, dx in enumerate((-0.9, 0.0, 0.9)):  # three long toes with claws
            tb = f"toe{k}_{side}"
            rig.bone(tb, f"foot_{side}", (hipx + dx, 0.6, -0.4))
            ln = 3.6 if k == 1 else 2.8
            rig.cube(tb, (hipx + dx - 0.3, 0.0, -0.4 - ln), (0.6, 0.6, ln), "hide", rot=(0, dx * 14 * sg, 0), pivot=(hipx + dx, 0.3, -0.4))
            rig.cube(tb, (hipx + dx - 0.22, -0.1, -0.4 - ln - 1.6), (0.44, 0.5, 1.8), "claw", rot=(14, dx * 14 * sg, 0), pivot=(hipx + dx, 0.3, -0.4))
        rig.cube(f"foot_{side}", (hipx - 0.25, 0.2, 1.0), (0.5, 0.5, 1.6), "claw")  # dew claw
    # a bigger skull, like the photograph: everything above the neck grows about the head pivot
    rig.scale_bones({"head_look", "head", "jaw", "tongue", "tongue2"}, (0, 45.6, -1.2), 1.28)
    return rig


def main():
    os.makedirs(OUT, exist_ok=True)
    atlas, glow, P = make_atlas()
    rig = build(P)
    atlas.save(os.path.join(OUT, "mimic.png"))
    glow.save(os.path.join(OUT, "mimic_glowmask.png"))
    with open(os.path.join(OUT, "mimic.geo.json"), "w") as f:
        json.dump(rig.json(), f, separators=(",", ":"))
    ncubes = sum(len(b.get("cubes", [])) for b in rig.bones)
    print(f"bones {len(rig.bones)} cubes {ncubes}")


if __name__ == "__main__":
    main()
