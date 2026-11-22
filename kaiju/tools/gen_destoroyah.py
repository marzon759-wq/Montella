#!/usr/bin/env python3
"""DESTOROYAH (perfect form): geometry and textures, same voxel style and bone naming as the atomic kaiju so the
shared combat, reaction and grapple clips play on it too.

A hunched crimson demon: digitigrade-thick legs on huge claws, a broad torso with a pale bony chest shield of petals,
red ribbed neck, a long-snouted head full of teeth under a heavy brow with burning yellow eyes, a great bone horn
rising from the forehead and swept-back red crest blades, red spiked pauldrons, long arms ending in three huge claws,
enormous bat wings (humerus, forearm and four finger spars with membrane), and a long spiked tail ending in a pincer.

Extra bones beyond the shared rig: horn1-3, eye_l/r, crest, wing_l/r, wingfore_l/r, wingtip_l/r, pincer_a/b.
Outputs (out/): destoroyah.geo.json, destoroyah.png, destoroyah_glowmask.png, destoroyah_burning.png,
destoroyah_burning_glowmask.png
"""
import json, math, os, random, sys
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_kaiju as gk
from gen_kaiju import Rig, keloid, ramp, simple, eye_tex, cracks, vnoise, voronoi, quant, emboss, shash

OUT = os.path.join(HERE, "out")
TEX = gk.TEX
SIDES = (("l", 1), ("r", -1))

SKIN = ramp((10, 2, 3), (118, 34, 36), 12, 1.35)
SKIN2 = ramp((8, 2, 3), (92, 24, 28), 12, 1.4)
RIB = ramp((30, 4, 6), (205, 52, 46), 10, 1.1)
SPIKE = ramp((40, 4, 6), (226, 60, 52), 10, 1.0)
BONE = ramp((96, 84, 64), (238, 226, 196), 10, 1.0)
MEMB = ramp((50, 8, 14), (190, 70, 84), 12, 1.05)
CLAW = ramp((90, 80, 64), (240, 228, 200), 8)
TOOTH = ramp((150, 138, 112), (248, 242, 228), 6)
MOUTH = ramp((60, 8, 22), (210, 90, 120), 8)
FACE = ramp((10, 2, 4), (124, 36, 38), 12, 1.35)


def membrane_tex(w, h, seed):
    rng = np.random.default_rng(seed)
    n = vnoise(w, h, 10, seed) * 0.5 + vnoise(w, h, 30, seed + 1) * 0.3
    y = np.arange(h)[:, None] / h
    x = np.arange(w)[None, :] / w
    veins = np.clip(1 - np.abs(np.sin((x * 7 + n * 1.5) * math.pi)) * 6, 0, 1)
    fine = np.clip(1 - np.abs(np.sin((y * 25 + n * 3) * math.pi)) * 5, 0, 1) * 0.4
    v = 0.45 + 0.25 * n - 0.35 * veins - 0.15 * fine + rng.normal(0, 0.03, (h, w))
    return quant(np.clip(v, 0, 0.999), MEMB)


def chest_tex(w, h, seed):
    d1, d2, _ = voronoi(w, h, 22, seed, (1.4, 0.9))
    hf = np.clip(1 - d1 / 16, 0, 1) ** 0.6 + vnoise(w, h, 14, seed) * 0.3
    hf = np.where(d2 - d1 < 1.8, hf * 0.2, hf)
    v = 0.2 + 0.5 * hf + emboss(hf) * 0.9
    return quant(np.clip(v, 0, 0.999), BONE)


def spike_tex(w, h, seed):
    y = np.arange(h)[:, None] / h
    n = vnoise(w, h, 8, seed)
    ridge = np.abs(np.sin((np.arange(w)[None, :] / w * 6 + n) * math.pi))
    v = 0.25 + 0.45 * (1 - y) + 0.2 * (1 - ridge) + (n - 0.5) * 0.2
    return quant(np.clip(v, 0, 0.999), SPIKE)


def rib_tex(w, h, seed):
    y = np.arange(h)[:, None] / h
    bands = (np.sin(y * h / 9 * math.pi * 2) * 0.5 + 0.5) ** 0.6
    n = vnoise(w, h, 10, seed)
    v = 0.15 + 0.6 * bands + (n - 0.5) * 0.25
    v = np.repeat(v, 1, 0) * np.ones((1, w))
    return quant(np.clip(v, 0, 0.999), RIB)


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
    put("skin", keloid(Q, Q, 31, 14, SKIN, ridges=0.6), 0, 0)
    put("skin2", keloid(Q, Q, 32, 22, SKIN2, ridges=0.8), Q, 0)
    put("belly", rib_tex(Q, Q, 33), 2 * Q, 0)
    put("knob", keloid(Q, Q, 34, 9, SKIN, ridges=0.2, fine=0.6, crease=1.0), 3 * Q, 0)
    put("membrane", membrane_tex(Q, Q, 35), 0, Q)
    put("plate", chest_tex(Q, Q, 36), Q, Q)
    put("face", keloid(Q, Q, 37, 10, FACE, ridges=0.35, fine=0.5), 2 * Q, Q)
    put("fold", keloid(Q, Q, 38, 12, SKIN2, aniso=(3.5, 0.6), ridges=0.2), 3 * Q, Q)
    put("spike", spike_tex(Q, Q, 39), 0, 2 * Q)
    put("claw", simple(128, 128, 40, CLAW, grad=0.25), Q, 2 * Q)
    put("tooth", simple(128, 128, 41, TOOTH, 0.08, grad=0.2), Q + 128, 2 * Q)
    put("mouth", keloid(128, 128, 42, 9, MOUTH, ridges=0.3, crease=0.8), Q, 2 * Q + 128)
    put("tongue", simple(128, 128, 43, MOUTH[2:], 0.1), Q + 128, 2 * Q + 128)
    put("eye", simple(32, 32, 44, [(140, 110, 10), (230, 200, 40), (255, 240, 120), (255, 255, 210)], 0.2), 2 * Q, 2 * Q)
    put("socket", simple(64, 64, 45, [(4, 1, 2), (14, 4, 6), (24, 8, 10)], 0.2), 2 * Q + 64, 2 * Q)
    put("horn", simple(128, 128, 46, BONE, 0.08, grad=0.35), 2 * Q + 128, 2 * Q)
    put("sole", keloid(Q, Q, 47, 18, SKIN2[:7]), 3 * Q, 2 * Q)
    put("crease", keloid(Q, Q, 48, 12, ramp((6, 1, 2), (70, 20, 22), 10, 1.3), ridges=0.3), 0, 3 * Q)
    normal = np.dstack([base, alpha]).astype(np.uint8)

    glow = np.zeros((TEX, TEX, 4))
    for name, col in (("eye", (255, 236, 90)), ("horn", (255, 226, 150)), ("mouth", (240, 120, 255)), ("tongue", (255, 170, 255))):
        x, y, w, h = P[name]
        lum = patches[name].mean(-1, keepdims=True) / 255
        glow[y:y + h, x:x + w, :3] = np.clip(np.array(col) * (0.55 + lum), 0, 255)
        glow[y:y + h, x:x + w, 3] = 255

    burn = base.copy()
    bglow = np.zeros((TEX, TEX, 4))
    HOT, CORE, RIM = np.array([238, 88, 20.]), np.array([255, 205, 96.]), np.array([34, 9, 5.])
    for name, n, ln in (("belly", 6, 90), ("skin", 4, 70), ("skin2", 3, 70), ("fold", 4, 60), ("face", 2, 40), ("membrane", 5, 80)):
        x, y, w, h = P[name]
        m = cracks(w, h, shash(name) + 7, n, ln)
        reg = burn[y:y + h, x:x + w]
        reg[m == 1], reg[m == 2], reg[m == 3] = CORE, HOT, RIM
        g = bglow[y:y + h, x:x + w]
        g[m == 1] = [*CORE, 255]
        g[m == 2] = [*HOT, 255]
    burning = np.dstack([burn, alpha]).astype(np.uint8)
    return P, normal, glow.astype(np.uint8), burning, bglow.astype(np.uint8)


# ====================================================================== geometry helpers
def seg(rig, bone, p0, p1, w, mat, t=None):
    """a box along the segment p0->p1 (w wide, t thick; the thin axis faces +-z after the rotation)"""
    p0, p1 = np.array(p0, float), np.array(p1, float)
    d = p1 - p0
    L = float(np.linalg.norm(d))
    if L < 0.05:
        return
    d /= L
    ry = math.degrees(math.asin(max(-1, min(1, d[2]))))
    rz = math.degrees(math.atan2(d[1], d[0]))
    mid = (p0 + p1) / 2
    t = w if t is None else t
    rig.cube(bone, (mid[0] - L / 2, mid[1] - w / 2, mid[2] - t / 2), (L, w, t), mat, rot=(0, ry, rz), pivot=tuple(mid))


def sector(rig, bone, apex, a, b, mat="membrane", thick=0.7):
    """membrane fan from apex to the edge a-b: overlapping thin ribs"""
    apex, a, b = (np.array(v, float) for v in (apex, a, b))
    edge = float(np.linalg.norm(b - a))
    n = max(3, int(math.ceil(edge / 3.2)))
    for i in range(n + 1):
        p = a + (b - a) * (i / n)
        seg(rig, bone, apex, p, edge / n * 1.45, mat, thick)


def taper_spike(rig, bone, base, tip, w0, mat="spike", steps=3):
    base, tip = np.array(base, float), np.array(tip, float)
    for k in range(steps):
        f0, f1 = k / steps, (k + 1) / steps
        seg(rig, bone, base + (tip - base) * f0, base + (tip - base) * (f1 + 0.05), w0 * (1 - 0.7 * f0), mat)


def build(P):
    rig = Rig(P)
    R = random.Random(77)
    rig.bone("root", None, (0, 0, 0))
    # ---------------------------------------------------------------- hips, torso, chest shield
    rig.bone("body", "root", (0, 40, 4))
    rig.blob("body", (0, 40, 4), (20, 11, 15), "skin2", layers=8)
    rig.blob("body", (0, 34, 12), (15, 8, 10), "skin2", layers=6)
    rig.knobs("body", (0, 40, 4), (20, 11, 15), 110, 201, (1.2, 3.0), where=lambda x, y, z, *n: z > -6)
    rig.bone("torso", "body", (0, 46, 2), rot=(14, 0, 0))
    rig.bone("belly", "torso", (0, 50, -6))
    rig.blob("belly", (0, 52, -1), (17.5, 10.5, 15), "skin", layers=8)
    rig.knobs("belly", (0, 52, -1), (17.5, 10.5, 15), 70, 202, (1.0, 2.6), where=lambda x, y, z, *n: z > -8)
    rig.bone("chest", "torso", (0, 60, -4))
    rig.blob("chest", (0, 64.5, -4), (18, 10.5, 15.5), "skin2", layers=8)
    rig.blob("chest", (0, 72, 4), (14, 8, 10), "skin2", layers=6)  # hunched back
    # the pale chest shield: a rosette of bony petals around a boss
    cx, cy, cz = 0, 60, -19.5
    rig.blob("chest", (cx, cy, cz + 1.5), (10.5, 11, 2.2), "plate", layers=5)
    for i in range(7):
        a = i / 7 * math.tau + 0.2
        px, py = cx + math.cos(a) * 6.2, cy + math.sin(a) * 6.8
        rig.cube("chest", (px - 2.6, py - 3.8, cz - 1.6), (5.2, 7.6, 1.8), "plate", rot=(0, 0, math.degrees(a) - 90), pivot=(px, py, cz - 0.7))
    rig.cube("chest", (cx - 2.2, cy - 2.2, cz - 2.4), (4.4, 4.4, 2.2), "plate")
    rig.knobs("chest", (0, 64.5, -4), (18, 10.5, 15.5), 100, 203, (1.2, 3.0), where=lambda x, y, z, *n: z > -8)
    for side, sg in SIDES:  # spiked pauldrons
        taper_spike(rig, "chest", (sg * 16, 72, -4), (sg * 27, 92, 6), 6.5)
        taper_spike(rig, "chest", (sg * 19, 68, 2), (sg * 32, 80, 10), 5)
        taper_spike(rig, "chest", (sg * 13, 75, 4), (sg * 18, 88, 14), 4)
    for i in range(6):  # spine spikes down the hunched back
        z = 4 + i * 2.5
        y = 78 - i * 6
        taper_spike(rig, "chest" if i < 4 else "body", (0, y, z + 6), (0, y + 9 - i, z + 12), 3.8 - i * 0.3)

    # ---------------------------------------------------------------- neck: long, forward, red ribbed front
    rig.bone("neck1", "chest", (0, 74, -8), rot=(-6, 0, 0))
    rig.blob("neck1", (0, 77, -12), (11, 6.5, 10), "skin2", layers=6)
    rig.cube("neck1", (-7, 72, -21.5), (14, 9, 3), "belly", proj=True)
    rig.bone("neck2", "neck1", (0, 80, -15))
    rig.blob("neck2", (0, 82.5, -18.5), (9, 5.5, 8.5), "skin", layers=5)
    rig.cube("neck2", (-5.6, 78, -26), (11.2, 8, 3), "belly", proj=True)
    rig.bone("throat", "neck2", (0, 79, -22))
    rig.cube("throat", (-5, 76, -25.5), (10, 5, 3), "belly")
    rig.bone("neck3", "neck2", (0, 85, -21))
    rig.blob("neck3", (0, 87, -24), (7.5, 4.8, 7), "skin", layers=5)
    rig.knobs("neck3", (0, 87, -24), (7.5, 4.8, 7), 24, 204, (0.8, 1.8), where=lambda x, y, z, *n: n[1] > 0)

    # ---------------------------------------------------------------- head: long snout, horn, crest, yellow eyes
    rig.bone("head", "neck3", (0, 88, -27))
    rig.blob("head", (0, 90.5, -33.5), (6.4, 4.8, 7.2), "face", layers=6, axis="z")
    rig.blob("head", (0, 88.6, -44), (4.4, 3.1, 6.4), "face", layers=6, axis="z")
    rig.cube("head", (-3.8, 86.6, -50.5), (7.6, 3.2, 2.2), "face")
    rig.knobs("head", (0, 92.5, -34), (5.6, 2.2, 6), 26, 205, (0.5, 1.3), mat="face", where=lambda x, y, z, *n: n[1] > 0.2)
    for side, sg in SIDES:
        rig.cube("head", (sg * 4.6 - 1.0, 90.4, -39.6), (2.0, 1.8, 2.6), "socket")
        eb = rig.bone(f"eye_{side}", "head", (sg * 4.8, 91.3, -39))
        rig.cube(eb, (sg * 4.9 - 0.6, 90.7, -39.8), (1.2, 1.1, 1.4), "eye")
        br = rig.bone(f"brow_{side}", "head", (sg * 3, 93, -37))
        rig.cube(br, (sg * 4.4 - 2.6, 92.0, -41.5), (5.2, 1.9, 5.0), "spike", rot=(0, 0, -sg * 14), pivot=(sg * 4.4, 93, -39))
        el = rig.bone(f"eyelid_{side}", "head", (sg * 4.8, 92, -39))
        rig.cube(el, (sg * 4.8 - 0.7, 91.6, -40.0), (1.4, 0.5, 1.6), "face")
        rig.cube("head", (sg * 1.6 - 0.45, 88.6, -50.9), (0.9, 0.8, 0.5), "socket")
    # crest: swept-back red blades
    rig.bone("crest", "head", (0, 92, -30))
    for side, sg in SIDES:
        seg(rig, "crest", (sg * 5, 93, -31), (sg * 12, 104, -18), 3.6, "spike", 1.4)
        seg(rig, "crest", (sg * 12, 104, -18), (sg * 14, 109, -10), 2.0, "spike", 1.0)
        seg(rig, "crest", (sg * 6.5, 89, -28), (sg * 15, 95, -16), 3.0, "spike", 1.2)
        seg(rig, "crest", (sg * 6, 86, -26), (sg * 13, 85, -15), 2.4, "spike", 1.1)
    # the horn: bone white, three tapering joints so it can flex and glow
    rig.bone("horn1", "head", (0, 94, -37))
    seg(rig, "horn1", (0, 93.5, -37), (0, 103, -40), 4.0, "horn", 3.2)
    rig.bone("horn2", "horn1", (0, 103, -40))
    seg(rig, "horn2", (0, 102.5, -40), (0, 112, -42), 2.8, "horn", 2.3)
    rig.bone("horn3", "horn2", (0, 112, -42))
    seg(rig, "horn3", (0, 111.5, -42), (0, 120, -42.6), 1.6, "horn", 1.4)
    seg(rig, "horn3", (0, 119.5, -42.6), (0, 123, -42.4), 0.8, "horn", 0.7)
    # mouth, lips, teeth, jaw, tongue (shared bone names)
    rig.cube("head", (-3.6, 86.0, -50), (7.2, 0.8, 14), "mouth", faces={"up": None})
    for side, sg in SIDES:
        rig.cube("head", (sg * 3.5 - 0.25, 83.6, -49.0), (0.5, 2.6, 13.0), "mouth")
    rig.bone("lip_upper", "head", (0, 87.5, -44))
    rig.cube("lip_upper", (-3.9, 86.2, -50.6), (7.8, 1.0, 1.0), "face")
    rr = random.Random(5)
    for i in range(7):
        x = -3.0 + i * 1.0
        ln = rr.uniform(1.2, 2.0) * (1.6 if i in (1, 5) else 1.0)
        rig.cube("head", (x - 0.36, 86.3 - ln, -50.2 + abs(x) * 0.2), (0.72, ln, 0.72), "tooth", rot=(rr.uniform(-10, 4), 0, 0), pivot=(x, 86.3, -50))
    for side, sg in SIDES:
        for kk in range(10):
            ln = rr.uniform(1.0, 1.9)
            rig.cube("head", (sg * 3.6 - 0.36, 86.3 - ln, -48.6 + kk * 1.3), (0.72, ln, 0.72), "tooth")
    rig.bone("jaw", "head", (0, 86.6, -32))
    rig.blob("jaw", (0, 84.8, -41), (4.0, 1.7, 7.6), "face", layers=6, axis="z")
    rig.cube("jaw", (-3.4, 85.9, -49.0), (6.8, 0.5, 14), "mouth", faces={"down": None})
    for side, sg in SIDES:
        for kk in range(10):
            ln = rr.uniform(0.9, 1.6)
            rig.cube("jaw", (sg * 3.3 - 0.35, 86.1, -48.4 + kk * 1.3), (0.7, ln, 0.7), "tooth")
    rig.bone("lip_lower", "jaw", (0, 85.5, -45))
    rig.cube("lip_lower", (-3.6, 85.0, -49.4), (7.2, 1.0, 1.0), "face")
    rig.bone("tongue1", "jaw", (0, 86.3, -36))
    rig.cube("tongue1", (-2.2, 85.9, -42), (4.4, 1.0, 6), "tongue")
    rig.bone("tongue2", "tongue1", (0, 86.3, -42))
    rig.cube("tongue2", (-1.8, 85.95, -46.5), (3.6, 0.85, 4.5), "tongue")

    # ---------------------------------------------------------------- arms: long, three great claws
    for side, sg in SIDES:
        sh = rig.bone(f"shoulder_{side}", "chest", (sg * 13, 69, -4))
        rig.blob(sh, (sg * 17, 67, -6), (6, 5.5, 6), "skin2", layers=6)
        ua = rig.bone(f"arm_{side}", sh, (sg * 18, 66, -6))
        rig.blob(ua, (sg * 20.5, 60, -8.5), (4.6, 7.4, 4.6), "skin", layers=7)
        rig.knobs(ua, (sg * 20.5, 60, -8.5), (4.6, 7.4, 4.6), 26, 210 + sg, (0.8, 2.0))
        fa = rig.bone(f"forearm_{side}", ua, (sg * 21, 54, -10))
        rig.blob(fa, (sg * 21, 52.5, -16.5), (3.9, 3.9, 7.2), "skin", layers=7, axis="z")
        taper_spike(rig, fa, (sg * 23, 54, -14), (sg * 28, 56, -6), 2.2)  # elbow spur
        hd = rig.bone(f"hand_{side}", fa, (sg * 21, 52.5, -23.5))
        rig.blob(hd, (sg * 21, 52, -25.5), (3.2, 2.6, 2.4), "skin", layers=4)
        for kk in range(4):
            back = kk == 3
            fx = sg * 21 + ((kk - 1) * 2.0 * sg if not back else sg * -2.6)
            f1 = rig.bone(f"finger{kk}_1_{side}", hd, (fx, 52.4, -27.5), rot=(18, -(kk - 1) * 9 * sg if not back else sg * 50, 0))
            rig.cube(f1, (fx - 0.8, 51.6, -30.4), (1.6, 1.7, 3.0), "skin")
            f2 = rig.bone(f"finger{kk}_2_{side}", f1, (fx, 52.4, -30.2), rot=(18, 0, 0))
            rig.cube(f2, (fx - 0.7, 51.7, -32.6), (1.4, 1.5, 2.5), "claw")
            f3 = rig.bone(f"finger{kk}_3_{side}", f2, (fx, 52.4, -32.4), rot=(30, 0, 0))
            L = 2.4 if back else 4.6
            rig.cube(f3, (fx - 0.55, 51.8, -32.4 - L), (1.1, 1.2, L), "claw")
            rig.cube(f3, (fx - 0.35, 51.9, -33.6 - L), (0.7, 0.8, 1.4), "claw", rot=(28, 0, 0), pivot=(fx, 52.3, -32.4 - L))
        t1 = rig.bone(f"thumb_1_{side}", hd, (sg * 19, 51.8, -25), rot=(10, -sg * 30, 0))
        rig.cube(t1, (sg * 19 - 0.6, 51.2, -27.4), (1.2, 1.2, 2.4), "skin")
        t2 = rig.bone(f"thumb_2_{side}", t1, (sg * 19, 51.8, -27.3), rot=(30, 0, 0))
        rig.cube(t2, (sg * 19 - 0.4, 51.3, -29.4), (0.8, 1.0, 2.1), "claw")

    # ---------------------------------------------------------------- legs: massive, claws like scythes
    for side, sg in SIDES:
        th = rig.bone(f"thigh_{side}", "body", (sg * 12, 40, 2))
        rig.blob(th, (sg * 15, 30, 1), (12, 13, 13.5), "skin2", layers=9)
        rig.blob(th, (sg * 16, 22, -4), (9.5, 6, 9.5), "skin", layers=6)
        rig.knobs(th, (sg * 15, 30, 1), (12, 13, 13.5), 110, 220 + sg, (1.2, 3.2))
        taper_spike(rig, th, (sg * 21, 34, 6), (sg * 28, 40, 16), 4)  # hip spur
        sn = rig.bone(f"shin_{side}", th, (sg * 16, 22, -3))
        rig.blob(sn, (sg * 16, 13, -1), (8.8, 10, 9), "skin", layers=8)
        rig.knobs(sn, (sg * 16, 13, -1), (8.8, 10, 9), 60, 222 + sg, (1.0, 2.4))
        ft = rig.bone(f"foot_{side}", sn, (sg * 16, 5, -1))
        rig.cube(ft, (sg * 16 - 8.5, 0, -10), (17, 4.5, 16), "skin2", faces={"down": "sole"})
        rig.blob(ft, (sg * 16, 5, -2), (8, 2.4, 7.5), "skin", layers=4)
        for kk, dx in enumerate((-5.6, 0.0, 5.6)):
            tx = sg * 16 + dx * sg
            t1 = rig.bone(f"toe{kk}_1_{side}", ft, (tx, 2.0, -10), rot=(0, -dx * sg * 2.4, 0))
            rig.blob(t1, (tx, 2.0, -12.2), (2.4, 2.0, 2.6), "skin", layers=4, axis="z")
            t2 = rig.bone(f"toe{kk}_2_{side}", t1, (tx, 1.6, -14.5))
            seg(rig, t2, (tx, 2.4, -14.4), (tx, 2.6, -19.5), 2.8, "claw", 2.6)
            seg(rig, t2, (tx, 2.6, -19.2), (tx, 0.3, -23.0), 2.0, "claw", 1.8)
        dc = rig.bone(f"dewclaw_{side}", ft, (sg * 16 - sg * 6.5, 1.5, 4.5))
        seg(rig, dc, (sg * 16 - sg * 6.5, 2, 4), (sg * 16 - sg * 8, 0.3, 8.5), 2.2, "claw")

    # ---------------------------------------------------------------- wings
    for side, sg in SIDES:
        B = (sg * 10, 76, 10)    # shoulder joint
        E = (sg * 40, 96, 16)    # elbow
        W = (sg * 70, 88, 22)    # wrist
        T = [(sg * 108, 100, 30), (sg * 114, 72, 34), (sg * 100, 48, 36), (sg * 78, 34, 32)]
        H = (sg * 14, 46, 10)    # membrane anchor on the flank
        wb = rig.bone(f"wing_{side}", "chest", B)
        seg(rig, wb, B, E, 4.4, "skin2")
        seg(rig, wb, E, (sg * 44, 99, 17), 5.2, "skin2")
        taper_spike(rig, wb, E, (sg * 44, 108, 20), 3.0)  # elbow claw
        sector(rig, wb, E, H, T[3])
        sector(rig, wb, B, H, E)
        wf = rig.bone(f"wingfore_{side}", wb, E)
        seg(rig, wf, E, W, 3.4, "skin2")
        sector(rig, wf, W, E, T[3])
        wt = rig.bone(f"wingtip_{side}", wf, W)
        for i, tip in enumerate(T):
            seg(rig, wt, W, tip, 1.8 - i * 0.15, "skin2")
            taper_spike(rig, wt, tip, tuple(np.array(tip) + (np.array(tip) - np.array(W)) * 0.06), 1.2, "claw", 1)
        for a, b in zip(T, T[1:]):
            sector(rig, wt, W, a, b)
        seg(rig, wt, W, (sg * 74, 95, 23), 2.6, "claw")  # thumb claw on the wrist

    # ---------------------------------------------------------------- tail: 16 segments with spikes, a pincer
    lens = [10, 10, 10, 10, 9.5, 9.5, 9, 9, 8.5, 8.5, 8, 8, 7.5, 7.5, 7, 7]
    rads = [12.5, 11.6, 10.8, 10.0, 9.2, 8.5, 7.8, 7.1, 6.5, 5.9, 5.3, 4.8, 4.3, 3.8, 3.4, 3.0, 2.8]
    bend = [(-14, 0), (-11, 2), (-9, 3), (-4, 3), (8, 4), (12, 5), (10, 5), (4, 4),
            (0, 3), (2, 2), (6, 0), (10, -2), (12, -3), (14, -3), (12, -2), (8, 0)]
    z = 13.0
    parent = "body"
    for i, L in enumerate(lens):
        name = rig.bone(f"tail{i + 1}", parent, (0, 40, z), rot=(bend[i][0], bend[i][1], 0))
        r0, r1 = rads[i], rads[i + 1]
        for k in range(3):
            a = r0 + (r1 - r0) * (k / 3)
            z0, z1 = z + L * k / 3 - 0.5, z + L * (k + 1) / 3 + 0.5
            for fa, fb in ((1.0, 0.72), (0.9, 0.9), (0.72, 1.0)):
                rig.cube(name, (-a * fa, 40 - a * fb * 0.95, z0), (2 * a * fa, 2 * a * fb * 0.95, z1 - z0), "skin2" if i < 6 else "skin", proj=True)
        rig.cube(name, (-r0 * 0.55, 40 - r0 * 0.95 - 0.5, z + 0.8), (r0 * 1.1, 1.0, L - 1.6), "belly")
        taper_spike(rig, name, (0, 40 + r0 * 0.85, z + L * 0.5), (0, 40 + r0 * 0.85 + 4.5 - i * 0.2, z + L * 0.5 + 3.5), max(1.0, 3.0 - i * 0.12))
        for side, sg in SIDES:
            if i % 3 == 0:
                taper_spike(rig, name, (sg * r0 * 0.75, 40 + r0 * 0.3, z + L * 0.5), (sg * (r0 * 0.75 + 3), 40 + r0 * 0.3 + 1.5, z + L * 0.5 + 2.5), 1.6)
        rig.knobs(name, (0, 40, z + L / 2), (r0 * 0.93, r0 * 0.93, L / 2), int(10 + r0 * 2.4), 240 + i, (0.7, max(1.0, r0 * 0.25)),
                  where=lambda x, y, zz, nx, ny, nz: ny > -0.5 and abs(nz) < 0.85)
        parent = name
        z += L
    # the pincer: two curved claws that open and clamp
    for nm, sg in (("pincer_a", 1), ("pincer_b", -1)):
        pb = rig.bone(nm, "tail16", (sg * 1.4, 40, z - 0.5), rot=(0, -sg * 14, 0))
        seg(rig, pb, (sg * 1.4, 40, z - 1), (sg * 4.5, 40.5, z + 6), 3.0, "claw", 2.2)
        seg(rig, pb, (sg * 4.5, 40.5, z + 6), (sg * 3.0, 40.6, z + 12), 2.2, "claw", 1.7)
        seg(rig, pb, (sg * 3.0, 40.6, z + 12), (sg * 0.2, 40.5, z + 15), 1.4, "claw", 1.2)
    return rig, z


def main():
    os.makedirs(OUT, exist_ok=True)
    gk.R.seed(4040)
    P, normal, glow, burning, bglow = make_textures()
    rig, tail_end = build(P)
    j = rig.json()
    j["minecraft:geometry"][0]["description"]["identifier"] = "geometry.destoroyah"
    j["minecraft:geometry"][0]["description"]["visible_bounds_width"] = 32
    Image.fromarray(normal, "RGBA").save(os.path.join(OUT, "destoroyah.png"))
    Image.fromarray(glow, "RGBA").save(os.path.join(OUT, "destoroyah_glowmask.png"))
    Image.fromarray(burning, "RGBA").save(os.path.join(OUT, "destoroyah_burning.png"))
    Image.fromarray(bglow, "RGBA").save(os.path.join(OUT, "destoroyah_burning_glowmask.png"))
    with open(os.path.join(OUT, "destoroyah.geo.json"), "w") as f:
        json.dump(j, f, separators=(",", ":"))
    n = sum(len(b.get("cubes", [])) for b in rig.bones)
    print(f"destoroyah: bones {len(rig.bones)} cubes {n} tail_end_z {tail_end}")


if __name__ == "__main__":
    main()
