#!/usr/bin/env python3
"""Builds the kaiju: vanilla ModelPart geometry (no GeckoLib), box-UV atlas, procedural textures and glow masks.

python3 gen_kaiju.py            -> out/kaiju.json, out/kaiju_<variant>.png, out/kaiju_<variant>_glow.png
Anatomy is written in "up" pixels with the ground at 0 and the head toward -z.  16 px = 1 block at scale 1
(the renderer scales the whole thing up).
"""
import json, math, os, random
import numpy as np
from PIL import Image
from geom import Model, Part
import noise3 as N

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "out")


# ---------------------------------------------------------------------------------------------- geometry helpers
def silhouette(p, H, W, t0, peaks, step, mat="fin", x_off=0.0):
    """A blocky leaf/flame silhouette in the y-z plane: columns `step` px wide whose heights follow the max of
    triangular peaks (z centre as a fraction of W, relative height, half width as a fraction of W)."""
    n = max(1, int(math.ceil(W / step)))
    z0 = -n * step / 2
    for i in range(n):
        zc = (z0 + (i + 0.5) * step) / W
        h = 0.0
        for pz, ph, pw in peaks:
            h = max(h, ph * max(0.0, 1 - abs(zc - pz) / pw) ** 1.35)
        h = H * h
        if h < 1:
            continue
        edge = 1 - abs(zc) * 1.6
        t = max(1, int(round(t0 * max(0.35, edge))))
        # thicker root, thinner tip: split each column in two stacked boxes
        h1 = max(1, round(h * 0.55))
        p.box(x_off - t / 2, 0, z0 + i * step, t, h1, step, mat)
        if h - h1 >= 1:
            t2 = max(1, t - (1 if t0 > 1 else 0) - (1 if t0 >= 4 else 0))
            p.box(x_off - t2 / 2, h1, z0 + i * step, t2, round(h - h1), step, mat)


FIN_SHAPES = [
    # (z, height, half width): broad leaf with a tall point leaning back, a front horn and a back horn
    [(0.08, 1.0, 0.55), (-0.30, 0.66, 0.32), (0.40, 0.70, 0.26)],
    [(0.12, 1.0, 0.50), (-0.24, 0.78, 0.34), (0.45, 0.55, 0.24)],
    [(0.00, 1.0, 0.55), (-0.36, 0.55, 0.28), (0.32, 0.80, 0.30)],
]


def spike(p, H, Wb, Tb, lean=0.0, mat="fin"):
    """a pyramidal crystal: stacked layers that narrow in both x and z, leaning back (+z) as they rise"""
    n = max(2, int(round(H / 2.5)))
    hs = H / n
    for i in range(n):
        f = i / n
        w = max(1, round(Wb * (1 - f) ** 0.9))
        t = max(1, round(Tb * (1 - f) ** 0.8))
        p.box(-t / 2, i * hs, -w / 2 + lean * f * H, t, math.ceil(hs), w, mat)


def fin(parent, name, x, y, z, H, rx, rz=0.0, seed=0):
    """Atomic dorsal fin: a fan of thick crystal spikes rising from one root, like the reference plates."""
    r = random.Random(seed)
    root = parent.child(name, (x, y, z), (rx, r.uniform(-5, 5), rz))
    root.fin_h = H
    Wb = max(1, H * 0.36)
    Tb = max(1, min(6, H * 0.17))
    spike(root, H, Wb, Tb, lean=0.10)
    if H >= 6:
        root.box(-Tb / 2 - 0.5, 0, -H * 0.32, Tb + 1, max(1, H * 0.12), H * 0.64, "fin")   # shared root plate
    k = 0
    if H >= 6:
        arms = [(-0.22, 0.10, 34, 0.62, 0), (0.24, 0.08, -36, 0.66, 0)]
        if H >= 16:
            arms += [(-0.36, 0.05, 62, 0.40, 0), (0.38, 0.05, -64, 0.44, 0)]
        if H >= 24:
            arms += [(0.0, 0.18, 10, 0.55, 22), (0.05, 0.15, -14, 0.50, -22)]
        for zf, yf, ang, hf, tilt in arms:
            sp = root.child(f"{name}_s{k}", (0, H * yf, H * zf), (ang + r.uniform(-7, 7), 0, tilt + r.uniform(-5, 5)))
            k += 1
            h = H * hf * r.uniform(0.88, 1.1)
            spike(sp, h, max(1, h * 0.42), max(1, min(5, h * 0.2)), lean=0.06)
    return root


def bumps(p, cx, cy, cz, w, h, d, faces, density, seed, mat="skin", m=1.0):
    """keloid scales: small blocks proud of the given faces of a (centred, up-coords) box."""
    r = random.Random(seed)
    for f in faces:
        if f in "xX":
            area = h * d
        elif f in "zZ":
            area = h * w
        else:
            area = w * d
        for _ in range(int(area * density)):
            bw, bh, bd = r.choice([(2, 2, 1), (3, 2, 1), (2, 3, 1), (3, 3, 1), (4, 2, 1), (2, 2, 2)])
            if f in "xX":
                bw, bd = 1 if r.random() < 0.8 else 2, bw
                bx = cx + w / 2 - 0.5 if f == "X" else cx - w / 2 - bw + 0.5
                by = cy - h / 2 + r.uniform(m, max(m, h - bh - m))
                bz = cz - d / 2 + r.uniform(m, max(m, d - bd - m))
            elif f in "zZ":
                bd = 1 if r.random() < 0.8 else 2
                bz = cz + d / 2 - 0.5 if f == "Z" else cz - d / 2 - bd + 0.5
                bx = cx - w / 2 + r.uniform(m, max(m, w - bw - m))
                by = cy - h / 2 + r.uniform(m, max(m, h - bh - m))
            else:  # top
                bh, bd = 1, bh
                by = cy + h / 2 - 0.5
                bx = cx - w / 2 + r.uniform(m, max(m, w - bw - m))
                bz = cz - d / 2 + r.uniform(m, max(m, d - bd - m))
            p.box(round(bx), round(by), round(bz), bw, bh, bd, mat)


def lumpy(p, cx, cy, cz, w, h, d, r=3, faces="xXzZ", density=0.03, seed=0, mat="skin"):
    p.rbox(cx, cy, cz, w, h, d, r, mat)
    if density > 0:
        bumps(p, cx, cy, cz, w, h, d, faces, density, seed, mat, m=r + 0.5)


# ---------------------------------------------------------------------------------------------- the kaiju
def build():
    m = Model()
    root = Part(m, "root", None, (0, -24, 0))   # vanilla y=24 is the ground
    HIP = 46
    body = root.child("body", (0, HIP, 6))

    # ---- pelvis / hips: the widest, heaviest mass
    lumpy(body, 0, 2, 2, 38, 24, 33, 7, "xXZ", 0.02, 1)
    lumpy(body, 0, -4, 1, 30, 18, 28, 6, "", 0, 2)
    body.rbox(0, 6, -10, 34, 16, 8, 4)                   # lower belly bulge
    for i in range(3):                                    # belly folds continue onto the pelvis
        body.box(-12 + i, 2 - i * 5, -14 - (1 if i == 0 else 0), 24 - 2 * i, 3, 2, "belly")

    # ---- torso: pear shaped, leaning forward
    torso = body.child("torso", (0, 10, 0), (12, 0, 0))
    lumpy(torso, 0, 9, 0, 44, 22, 33, 7, "xXZ", 0.022, 3)     # gut
    lumpy(torso, 0, 27, -1, 40, 18, 30, 6, "xXZ", 0.022, 4)   # chest
    lumpy(torso, 0, 41, 1, 32, 14, 26, 5, "xXZ", 0.025, 5)    # upper chest / shoulders
    torso.rbox(0, 50, 3, 22, 8, 18, 3)                         # neck base
    # pectoral and shoulder masses
    for s in (-1, 1):
        torso.rbox(s * 10, 33, -12, 17, 14, 8, 3)
        torso.rbox(s * 16, 40, -3, 11, 13, 17, 3)
        bumps(torso, s * 16, 40, -3, 11, 13, 17, "X" if s > 0 else "x", 0.04, 30 + s)
    # belly plates: thick horizontal folds stacked up the front, widest at the gut
    for i in range(9):
        y = -1 + i * 4.6
        w = 26 - abs(i - 3) * 1.6
        z = -16 - (1 if 2 <= i <= 5 else 0) + (1 if i >= 7 else 0)
        torso.box(-w / 2, y, z, w, 3, 3, "belly")
        torso.box(-w / 2 + 2, y + 3, z + 1, w - 4, 1.6, 2, "belly_dark")

    # ---- neck: thick, thrust forward
    neck = torso.child("neck", (0, 50, -1), (34, 0, 0))
    lumpy(neck, 0, 5, -1, 25, 16, 22, 5, "xXZ", 0.03, 6)
    lumpy(neck, 0, 15, -3, 21, 10, 19, 4, "xXZ", 0.03, 7)
    for i in range(4):  # throat folds
        neck.box(-7, 1 + i * 4, -12, 14, 3, 2, "belly")

    # ---- head: small against the body, heavy brow, short muzzle
    head = neck.child("head", (0, 20, -4), (-42, 0, 0))
    lumpy(head, 0, 4, -2, 17, 12, 17, 3, "xXY", 0.04, 8)        # cranium
    head.rbox(0, 1, 2, 21, 10, 11, 3)                                # jaw muscles / cheeks
    for s in (-1, 1):
        head.rbox(s * 9.5, 2, 1, 4, 8, 9, 1)
    head.rbox(0, 8.5, -10, 15, 4, 7, 1)                             # brow ridge
    head.box(-5.5, 10, -9, 11, 2, 9)                                # skull roof scales
    head.rbox(0, 3, -15, 12, 7, 11, 2)                              # muzzle / upper jaw
    head.rbox(0, 5.5, -16, 10, 3, 9, 1)                             # nose bridge
    head.box(-3.5, 6, -21, 7, 3, 2)                                 # snout tip
    head.box(-4, -0.5, -20, 8, 1, 15, "mouth")                      # palate
    for s in (-1, 1):
        head.box(s * 2.5 - 0.5, 6.5, -21.5, 1, 1, 1, "nostril")
        head.box(s * 6.8 - 1, 5.5, -9.5, 2, 2, 1, "eye")            # small deep-set eyes
        head.box(s * 7.3 - 1, 7.5, -10.5, 2, 1, 3)                  # eyelid ridge
        for k in range(6):                                          # upper teeth
            head.box(s * 4.8 - 0.5, -2, -19.5 + k * 2.6, 1, 2 + (k == 1), 1, "tooth")
    head.box(-1.5, -2, -20.5, 1, 2, 1, "tooth")
    head.box(0.5, -2, -20.5, 1, 2, 1, "tooth")
    jaw = head.child("jaw", (0, 0, 0), (22, 0, 0))
    jaw.rbox(0, -2.5, -8, 12, 5, 18, 2)
    jaw.box(-6.5, -4.5, -4, 13, 3, 9)                               # chin / throat
    jaw.box(-4.5, -1, -17, 9, 1, 14, "mouth")                       # tongue bed
    jaw.box(-2.5, -0.5, -15, 5, 1, 9, "tongue")
    for s in (-1, 1):
        for k in range(6):
            jaw.box(s * 4.6 - 0.5, 0, -16.5 + k * 2.6, 1, 2 + (k == 0), 1, "tooth")

    # ---- arms: short, muscular, held forward with long claws
    for s, nm in ((1, "l"), (-1, "r")):
        arm = torso.child(f"arm_{nm}", (s * 20, 38, -7), (8, 0, -s * 30))
        lumpy(arm, 0, -8, 0, 12, 19, 13, 3, "xXz", 0.035, 40 + s)
        fore = arm.child(f"forearm_{nm}", (0, -15, 0), (-68, s * 18, s * 16))
        lumpy(fore, 0, -8, 0, 11, 17, 11, 3, "xXZ", 0.035, 42 + s)
        hand = fore.child(f"hand_{nm}", (0, -16, 0), (-10, 0, 0))
        hand.rbox(0, -2.5, 0, 11, 6, 10, 2)
        for k, (fx, fz, fl) in enumerate([(-4, -3, 7), (-1.3, -3.5, 8), (1.3, -3.5, 8), (4, -3, 7), (4.5, 2.5, 5)]):
            fg = hand.child(f"finger_{nm}{k}", (fx * s, -4, fz + 2), (-10 if k < 4 else 30, 0, s * (fx * 4)))
            fg.box(-1.5, -fl, -1.5, 3, fl, 3)
            cl = fg.child(f"claw_{nm}{k}", (0, -fl, 0), (-30, 0, 0))
            cl.box(-1, -3, -1.5, 2, 3, 3, "claw")
            cl.box(-0.5, -6, -1, 1, 3, 2, "claw")

    # ---- legs: colossal thighs, columnar shins, elephantine clawed feet
    for s, nm in ((1, "l"), (-1, "r")):
        thigh = body.child(f"thigh_{nm}", (s * 16, -2, 1), (-18, 0, -s * 4))
        lumpy(thigh, 0, -7, 0, 28, 34, 30, 7, "xXZz", 0.025, 50 + s)
        thigh.rbox(s * 6, -4, 2, 12, 24, 25, 4)                  # outer thigh bulge
        thigh.rbox(0, -16, -6, 18, 10, 14, 3)                    # knee
        shin = thigh.child(f"shin_{nm}", (0, -21, -1), (30, 0, s * 4))
        lumpy(shin, 0, -8, 1, 18, 21, 19, 4, "xXzZ", 0.03, 60 + s)
        foot = shin.child(f"foot_{nm}", (0, -17, 1), (-12, 0, 0))
        lumpy(foot, 0, -2, -3, 21, 8, 24, 3, "xXY", 0.03, 70 + s)
        foot.rbox(0, 2, 2, 18, 7, 15, 2)
        for k, tx in enumerate((-6.5, 0, 6.5)):
            toe = foot.child(f"toe_{nm}{k}", (tx, -3, -14), ((-8, 0, s * (tx * 1.5))))
            toe.rbox(0, 0, -3, 6, 6, 8, 1)
            toe.box(-1.5, -2.5, -9, 3, 3, 3, "claw")
            toe.box(-1, -3, -11, 2, 2, 2, "claw")
        heel = foot.child(f"heel_{nm}", (0, -3, 9), (10, 0, 0))
        heel.rbox(0, 0, 2, 7, 5, 6, 1)
        heel.box(-1, -2.5, 4, 2, 2, 3, "claw")

    # ---- tail: very long, tapering, sweeping down to the ground and lifting at the tip
    segs = 12
    tail_rx = [-17, -12, -8, -4, -1, 2, 5, 9, 13, 17, 21, 25]
    tail_ry = [0, 4, 6, 6, 5, 2, -3, -6, -8, -8, -7, -6]
    parent, zp = body, 13
    tails = []
    for i in range(segs):
        f = i / (segs - 1)
        L = 17 - 7 * f
        w = 28 - 24 * f ** 0.8
        h = 25 - 21 * f ** 0.8
        t = parent.child(f"tail{i}", (0, -1 if i == 0 else 0, zp), (tail_rx[i], tail_ry[i], 0))
        lumpy(t, 0, 0, L / 2, w, h, L + 3, max(1, int(w / 7)), "xXY", 0.03 if w > 7 else 0, 80 + i)
        t.box(-w / 2 + 2, -h / 2 - 0.5, 0, w - 4, 1, L + 1, "belly_dark")   # underside scutes
        tails.append((t, L, w, h))
        parent, zp = t, L

    # ---- dorsal fins: three rows, biggest over the shoulders, shrinking down the tail
    sid = 1000
    def row(part, pts, side=True, side_k=0.6):
        nonlocal sid
        for j, (y, z, H, rx) in enumerate(pts):
            fin(part, f"fin{sid}", 0, y, z, H, rx, 0, sid)
            sid += 1
            if side and H >= 5:
                # side rows sit between the centre fins
                ny, nz = (pts[j + 1][0], pts[j + 1][1]) if j + 1 < len(pts) else (y - 6, z)
                for s in (-1, 1):
                    fin(part, f"fin{sid}", s * 6, (y + ny) / 2 - 2, (z + nz) / 2 - 2, H * side_k, rx + 4, s * 32, sid)
                    sid += 1

    row(neck, [(16, 7, 10, -30), (8, 10, 16, -34)])
    row(torso, [(52, 10, 25, -26), (43, 14, 37, -30), (32, 15, 44, -36), (21, 15, 40, -42), (10, 15, 32, -50)])
    row(body, [(8, 16, 25, -62)])
    for i, (t, L, w, h) in enumerate(tails):
        H = max(3, 21 - i * 1.7)
        row(t, [(h / 2 - 1, L * 0.45, H, -12)], side=(i < 7), side_k=0.65)

    ground(m)
    return m


def ground(m):
    """lift/sink the body so the lowest foot point rests exactly on y=24 (vanilla ground)."""
    lo = -1e9
    for c in m.cubes():
        if not c.part.name.startswith(("foot", "toe", "heel")):
            continue
        W = c.part.world()
        for name, verts, *_ in c.faces():
            for v in verts:
                p = W @ np.array([v[0] / 16, v[1] / 16, v[2] / 16, 1])
                lo = max(lo, p[1] * 16)
    body = m.part("body")
    body.pivot[1] += 24 - lo


# ---------------------------------------------------------------------------------------------- texturing
SKINS = {
    # base skin, scale highlight, crevice, fin, fin dark, belly
    "atomic": dict(skin=(58, 56, 53), hi=(96, 92, 85), lo=(24, 23, 22), fin=(128, 125, 120), fin_lo=(52, 50, 48),
                   belly=(66, 62, 57)),
    "burning": dict(skin=(40, 38, 36), hi=(84, 80, 74), lo=(16, 15, 14), fin=(70, 67, 63), fin_lo=(30, 28, 27),
                    belly=(48, 45, 42)),
}


def face_points(c, face, W):
    """texel centres of one face -> (uv int coords, world points, world normal)"""
    name, verts, u1, v1, u2, v2 = face
    us = np.arange(min(u1, u2), max(u1, u2)) + 0.5
    vs = np.arange(min(v1, v2), max(v1, v2)) + 0.5
    if len(us) == 0 or len(vs) == 0:
        return None
    U, V = np.meshgrid(us, vs)
    a = (U - u1) / (u2 - u1)
    b = (V - v1) / (v2 - v1)
    P = [np.array(v, float) for v in verts]
    pts = ((1 - a) * (1 - b))[..., None] * P[1] + (a * (1 - b))[..., None] * P[0] + \
          ((1 - a) * b)[..., None] * P[2] + (a * b)[..., None] * P[3]
    hom = np.concatenate([pts / 16, np.ones(pts.shape[:2] + (1,))], -1)
    wp = (hom @ W.T)[..., :3] * 16
    o, s = np.array(c.o), np.array(c.s)
    n = (sum(P) / 4 - (o + s / 2)) / np.maximum(s / 2, 1e-6)
    n = n / (np.linalg.norm(n) + 1e-9)
    n = W[:3, :3] @ n
    n = n * np.array([1, -1, 1])          # -> y up
    return U.astype(int), V.astype(int), wp, n


def paint(m, size, variant):
    S = SKINS[variant]
    Wt, Ht = size
    base = np.zeros((Ht, Wt, 4), np.uint8)
    glow = np.zeros((Ht, Wt, 4), np.uint8)
    worlds = {p.name: p.world() for p in m.parts}
    for c in m.cubes():
        Wm = worlds[c.part.name]
        for face in c.faces():
            r = face_points(c, face, Wm)
            if r is None:
                continue
            U, V, wp, n = r
            col, gl = shade(c, wp, n, S, variant)
            base[V, U, :3] = col
            base[V, U, 3] = 255
            glow[V, U, :3] = gl
            glow[V, U, 3] = 255
    return base, glow


def shade(c, wp, n, S, variant):
    x, y, z = wp[..., 0], -wp[..., 1], wp[..., 2]   # world, y up (in px; ground = -24)
    sh = wp.shape[:2]
    mat_ = c.mat
    g = np.zeros(sh + (3,))
    if mat_ in ("skin", "belly", "belly_dark"):
        # keloid scales: cellular bumps with dark crevices + large blotches
        f1, f2 = N.worley(wp / 3.2, seed=3)
        cell = np.clip((f2 - f1) * 2.2, 0, 1)
        bl = N.fbm(wp / 22.0, 3, seed=5)
        fine = N.fbm(wp / 1.7, 2, seed=9)
        base = np.array(S["belly"] if mat_ != "skin" else S["skin"], float)
        col = base * (0.78 + 0.35 * bl[..., None])
        col = col * (1 - 0.35 * fine[..., None]) + np.array(S["hi"]) * (0.45 * (cell ** 0.6) * (fine > 0.45))[..., None]
        crev = (cell < 0.18)
        col[crev] = col[crev] * 0.5 + np.array(S["lo"]) * 0.5
        if mat_ == "belly":
            # horizontal plate edge: lighter top lip
            col = col * 1.08
        if mat_ == "belly_dark":
            col = col * 0.62
        # undersides darker (baked occlusion)
        down = np.clip(-n[..., 1], 0, 1)          # world normal points down -> occluded
        col *= (1 - 0.35 * down)[..., None]
        if variant == "burning" and mat_ != "belly_dark":
            # molten fissures: cell borders of a coarser lattice, masked to the front of the body and limbs
            h1, h2 = N.worley(wp / np.array([6.5, 4.5, 6.5]), seed=21)
            e1 = h2 - h1
            line = np.clip(1 - e1 / 0.11, 0, 1)                      # thin fissure network
            l2a, l2b = N.worley(wp / 2.6, seed=23)
            line2 = np.clip(1 - (l2b - l2a) / 0.07, 0, 1) * 0.55     # hairline cracks branching off
            mask = N.fbm(wp / 16.0, 3, seed=33)
            front = np.clip(-n[..., 2] * 0.5 + 0.6, 0, 1)
            name = c.part.name
            region = 0.55 if name.startswith(("tail", "foot", "toe", "heel")) else 1.0
            m_ = np.clip((mask - 0.40) * 4.0, 0, 1) * front * region
            k = np.maximum(line, line2 * np.clip(line * 0 + m_ * 1.3, 0, 1)) * m_
            k = np.where(k > 0.3, np.clip(k * 1.3, 0, 1), 0)
            hot = np.array([255, 190, 90.0]) * (k ** 2)[..., None] + np.array([255, 70, 18.0]) * (1 - k ** 2)[..., None]
            col = np.where((k > 0)[..., None], col * (1 - k[..., None]) + np.array([170, 50, 14.0]) * k[..., None], col)
            g = np.where((k > 0)[..., None], hot * np.clip(k * 1.2, 0, 1)[..., None], g)
            # embers glowing in the scale crevices around the fissures
            emb = (crev & (m_ > 0.5) & (N.value(wp * 1.3, 41) > 0.72))
            g[emb] = np.maximum(g[emb], np.array([150, 35, 8.0]))
        return np.clip(col, 0, 255).astype(np.uint8), np.clip(g, 0, 255).astype(np.uint8)
    if mat_ == "fin":
        # bony, crystalline: streaky along the blade, darker cracks, paler tips
        st = N.fbm(wp / 5.0, 3, seed=11)
        f1, f2 = N.worley(wp / 2.6, seed=13)
        crack = (f2 - f1) < 0.12
        col = np.array(S["fin"], float) * (0.72 + 0.4 * st[..., None])
        col[crack] = np.array(S["fin_lo"], float)
        fp = c.part
        while not hasattr(fp, "fin_h"):
            fp = fp.parent
        Wf = fp.world()
        rel = np.clip(np.linalg.norm(wp - Wf[:3, 3] * 16, axis=-1) / fp.fin_h, 0, 1)
        rootk = np.clip(1 - rel * 2.2, 0, 1)[..., None]          # dark, skin-like root, pale crystal tips
        col = col * (1 - rootk * 0.65) + np.array(S["lo"], float) * rootk * 0.65
        down = np.clip(-n[..., 1], 0, 1)
        col *= (1 - 0.3 * down)[..., None]
        if variant == "atomic":
            core = np.clip(st * 1.2, 0, 1)
            gc = np.array([40, 150, 255.0]) * (1 - core[..., None]) + np.array([150, 240, 255.0]) * core[..., None]
            gc[crack] *= 0.35
            g = gc * 0.85
            col = col * 0.85 + np.array([110, 140, 160.0]) * 0.15
        else:
            ember = np.clip((N.fbm(wp / 4.0, 2, seed=17) - 0.55) * 4, 0, 1) * (crack)
            g = np.array([255, 80, 20.0]) * ember[..., None]
        return np.clip(col, 0, 255).astype(np.uint8), np.clip(g, 0, 255).astype(np.uint8)
    flat = {
        "tooth": (214, 204, 182), "claw": (196, 186, 166), "mouth": (122, 52, 56), "tongue": (156, 74, 80),
        "nostril": (12, 11, 10), "eye": (20, 18, 16),
    }[mat_]
    v = N.fbm(wp / 1.5, 2, seed=19)
    col = np.array(flat, float) * (0.8 + 0.35 * v[..., None])
    if mat_ == "eye":
        g[:] = (255, 70, 30) if variant == "burning" else (120, 220, 255)
        g *= 0.85
        col = np.array([40, 30, 26.0]) + g * 0.15
    if mat_ == "mouth" and variant == "burning":
        g[:] = (90, 22, 6)
    return np.clip(col, 0, 255).astype(np.uint8), np.clip(g, 0, 255).astype(np.uint8)


def main():
    os.makedirs(OUT, exist_ok=True)
    m = build()
    tex = m.pack_uv(1024)
    print(f"parts={len(m.parts)} cubes={sum(1 for _ in m.cubes())} texture={tex}")
    with open(os.path.join(OUT, "kaiju.json"), "w") as f:
        json.dump(m.to_json(tex), f, separators=(",", ":"))
    for v in ([os.environ["ONLY"]] if os.environ.get("ONLY") else SKINS):
        b, g = paint(m, tex, v)
        Image.fromarray(b, "RGBA").save(os.path.join(OUT, f"kaiju_{v}.png"))
        Image.fromarray(g, "RGBA").save(os.path.join(OUT, f"kaiju_{v}_glow.png"))
    return m


if __name__ == "__main__":
    main()
