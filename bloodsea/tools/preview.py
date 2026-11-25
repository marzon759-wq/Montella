#!/usr/bin/env python3
"""Offline preview of a bedrock/GeckoLib model: textured, z-buffered, lit. Renders stills and animation frames.

python3 preview.py geo.json tex.png out.png [--anim anims.json NAME --t 0.5] [--yaw 30 --pitch 10 --size 720]
Rotation convention as GeckoLib/Blockbench: R = Rz(z) * Ry(-y) * Rx(-x), model front = -z.
"""
import argparse, json, math
import numpy as np
GR = 200
import os
FOG0, FOG1 = float(os.environ.get('FOG0', 60)), float(os.environ.get('FOG1', 120))
BRIGHT = float(os.environ.get('BRIGHT', 1))
from PIL import Image, ImageDraw


def rot(deg):
    x, y, z = (math.radians(v) for v in deg)
    x, y = -x, -y
    cx, sx, cy, sy, cz, sz = math.cos(x), math.sin(x), math.cos(y), math.sin(y), math.cos(z), math.sin(z)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def M(R=np.eye(3), t=(0, 0, 0)):
    m = np.eye(4)
    m[:3, :3] = R
    m[:3, 3] = t
    return m


def about(pivot, R):
    p = np.array(pivot, float)
    return M(t=p) @ M(R) @ M(t=-p)


class Anim:
    def __init__(self, data, name):
        self.a = data["animations"][name] if name else None
        self.len = self.a.get("animation_length", 1) if self.a else 0

    @staticmethod
    def sample(ch, t):
        if isinstance(ch, list):
            return np.array(ch, float)
        keys = sorted((float(k), v) for k, v in ch.items())
        if t <= keys[0][0]:
            return np.array(Anim.val(keys[0][1]), float)
        for (t0, v0), (t1, v1) in zip(keys, keys[1:]):
            if t0 <= t <= t1:
                f = (t - t0) / (t1 - t0) if t1 > t0 else 0
                return np.array(Anim.val(v0), float) * (1 - f) + np.array(Anim.val(v1), float) * f
        return np.array(Anim.val(keys[-1][1]), float)

    @staticmethod
    def val(v):
        if isinstance(v, dict):
            v = v.get("post", v.get("pre"))
        return v

    def pose(self, bone, t):
        if not self.a or bone not in self.a.get("bones", {}):
            return None, None, None
        b = self.a["bones"][bone]
        r = self.sample(b["rotation"], t) if "rotation" in b else None
        p = self.sample(b["position"], t) if "position" in b else None
        s = self.sample(b["scale"], t) if "scale" in b else None
        return r, p, s


FACES = {  # corners of a unit box per face key, (s,t) = (0,0), (1,0), (1,1), (0,1): GeckoLib's mapping (it mirrors x,
    # so "west" lands on the +x side); the preview itself does not mirror the geometry
    "north": [(0, 1, 0), (1, 1, 0), (1, 0, 0), (0, 0, 0)],
    "south": [(1, 1, 1), (0, 1, 1), (0, 0, 1), (1, 0, 1)],
    "west": [(1, 1, 0), (1, 1, 1), (1, 0, 1), (1, 0, 0)],
    "east": [(0, 1, 1), (0, 1, 0), (0, 0, 0), (0, 0, 1)],
    "up": [(0, 1, 1), (1, 1, 1), (1, 1, 0), (0, 1, 0)],
    "down": [(0, 0, 0), (1, 0, 0), (1, 0, 1), (0, 0, 1)],
}


def build_quads(geo, anim=None, t=0.0):
    g = geo["minecraft:geometry"][0]
    bones = {b["name"]: b for b in g["bones"]}
    world = {}

    def wm(name):
        if name in world:
            return world[name]
        b = bones[name]
        parent = wm(b["parent"]) if "parent" in b else np.eye(4)
        r = np.array(b.get("rotation", [0, 0, 0]), float)
        ar, ap, asc = anim.pose(name, t) if anim else (None, None, None)
        if ar is not None:
            r = r + ar
        m = parent @ (M(t=ap) if ap is not None else np.eye(4)) @ about(b["pivot"], rot(r))
        if asc is not None:
            m = m @ about(b["pivot"], np.diag(asc))
        world[name] = m
        return m

    quads = []
    for name, b in bones.items():
        m = wm(name)
        for c in b.get("cubes", []):
            o = np.array(c["origin"], float)
            s = np.array(c["size"], float)
            inf = c.get("inflate", 0)
            o, s = o - inf, s + 2 * inf
            cm = m
            if "rotation" in c:
                cm = m @ about(c.get("pivot", o + s / 2), rot(c["rotation"]))
            for f, cor in FACES.items():
                if f not in c["uv"]:
                    continue
                pts = np.array([o + s * np.array(k) for k in cor])
                pts = (cm @ np.c_[pts, np.ones(4)].T).T[:, :3]
                u = c["uv"][f]
                quads.append((pts, u["uv"], u["uv_size"]))
    return quads


def render(quads, tex, size=720, yaw=25, pitch=8, dist=None, target=(0, 28, 0), fov=35, bg=(10, 10, 12), light=(0.4, 0.7, -0.6),
           fog=True, ground=True):
    W = H = size
    tex = np.asarray(tex.convert("RGBA"), float) / 255
    TH, TW = tex.shape[:2]
    # camera: orbit around target, looking at the model's front from -z
    yr, pr = math.radians(yaw), math.radians(pitch)
    dist = dist or 95
    cam = np.array(target, float) + dist * np.array([math.sin(yr) * math.cos(pr), math.sin(pr), -math.cos(yr) * math.cos(pr)])
    fwd = np.array(target, float) - cam
    fwd /= np.linalg.norm(fwd)
    right = np.cross(fwd, [0, 1, 0])
    right /= np.linalg.norm(right)
    up = np.cross(right, fwd)
    f = 0.5 * W / math.tan(math.radians(fov) / 2)
    L = np.array(light, float)
    L /= np.linalg.norm(L)

    img = np.zeros((H, W, 3))
    img[:] = np.array(bg) / 255
    # a subtle vertical gradient
    img *= np.linspace(1.4, 0.6, H)[:, None, None]
    zbuf = np.full((H, W), np.inf)

    def proj(p):
        d = p - cam
        x, y, z = d @ right, d @ up, d @ fwd
        return np.array([W / 2 + f * x / z, H / 2 - f * y / z, z])

    tris = []
    if ground:  # a floor plane of dark planks so feet read as standing on something
        gq = np.array([[-GR, 0, -GR], [GR, 0, -GR], [GR, 0, GR], [-GR, 0, GR]], float)
        tris.append(("ground", gq))
    for pts, uv, us in quads:
        tris.append(((uv, us), pts))

    for meta, pts in tris:
        sp = np.array([proj(p) for p in pts])
        if np.any(sp[:, 2] <= 1):
            continue
        n = np.cross(pts[1] - pts[0], pts[3] - pts[0])
        nl = np.linalg.norm(n)
        if nl < 1e-9:
            continue
        n /= nl
        shade = 0.28 + 0.9 * max(0.0, float(n @ L)) + 0.12 * max(0.0, float(n @ -fwd))
        # rim light from behind
        shade += 0.35 * max(0.0, float(n @ np.array([-0.3, 0.2, 0.9]))) ** 3
        x0, y0 = int(max(0, math.floor(sp[:, 0].min()))), int(max(0, math.floor(sp[:, 1].min())))
        x1, y1 = int(min(W - 1, math.ceil(sp[:, 0].max()))), int(min(H - 1, math.ceil(sp[:, 1].max())))
        if x1 < x0 or y1 < y0:
            continue
        ys, xs = np.mgrid[y0:y1 + 1, x0:x1 + 1]
        P = np.stack([xs + 0.5, ys + 0.5], -1)
        # quad -> two triangles with (s,t) in the unit square
        for tri, st in (((0, 1, 2), ((0, 0), (1, 0), (1, 1))), ((0, 2, 3), ((0, 0), (1, 1), (0, 1)))):
            a, b, c = sp[list(tri)]
            v0, v1 = b[:2] - a[:2], c[:2] - a[:2]
            den = v0[0] * v1[1] - v1[0] * v0[1]
            if abs(den) < 1e-9:
                continue
            d = P - a[:2]
            l1 = (d[..., 0] * v1[1] - v1[0] * d[..., 1]) / den
            l2 = (v0[0] * d[..., 1] - d[..., 0] * v0[1]) / den
            l0 = 1 - l1 - l2
            inside = (l0 >= -1e-4) & (l1 >= -1e-4) & (l2 >= -1e-4)
            if not inside.any():
                continue
            # perspective-correct depth and texture coordinates
            iz = l0 / a[2] + l1 / b[2] + l2 / c[2]
            z = 1 / np.where(iz > 0, iz, 1e-9)
            yy, xx = ys[inside], xs[inside]
            zz = z[inside]
            closer = zz < zbuf[yy, xx]
            if not closer.any():
                continue
            yy, xx, zz = yy[closer], xx[closer], zz[closer]
            L0, L1, L2 = (l[inside][closer] for l in (l0, l1, l2))
            s0, s1, s2 = (np.array(v, float) for v in st)
            w0, w1, w2 = L0 / a[2] * zz, L1 / b[2] * zz, L2 / c[2] * zz
            S = w0 * s0[0] + w1 * s1[0] + w2 * s2[0]
            T = w0 * s0[1] + w1 * s1[1] + w2 * s2[1]
            if meta == "ground":
                gx = (pts[0][0] + (pts[1][0] - pts[0][0]) * S)
                gz = (pts[0][2] + (pts[3][2] - pts[0][2]) * T)
                wave = 0.5 + 0.5 * np.sin(gx * 0.11 + np.sin(gz * 0.07) * 2) * np.sin(gz * 0.09)
                col = np.stack([0.26 + 0.08 * wave, 0.015 + 0.01 * wave, 0.02 + 0.01 * wave], -1)
                col *= np.clip(1.25 - np.hypot(gx, gz) / GR, 0.2, 1)[:, None]
                alpha = np.ones_like(S)
            else:
                (u0, v0_), (uw, vh) = meta
                tu = np.clip((u0 + S * uw).astype(int), 0, TW - 1)
                tv = np.clip((v0_ + T * vh).astype(int), 0, TH - 1)
                tc = tex[tv, tu]
                alpha = tc[:, 3]
                col = tc[:, :3] * shade * BRIGHT
            keep = alpha > 0.5
            if fog and meta != "ground":
                fogf = np.clip((zz[keep] - FOG0) / FOG1, 0, 0.6)[:, None]
                col = col[keep] * (1 - fogf) + np.array(bg) / 255 * fogf
            else:
                col = col[keep]
            img[yy[keep], xx[keep]] = col
            zbuf[yy[keep], xx[keep]] = zz[keep]
    # vignette
    yy, xx = np.mgrid[0:H, 0:W]
    v = 1 - 0.55 * (((xx - W / 2) / (W / 2)) ** 2 + ((yy - H / 2) / (H / 2)) ** 2) ** 1.2
    img *= np.clip(v, 0.2, 1)[..., None]
    return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("geo")
    ap.add_argument("tex")
    ap.add_argument("out")
    ap.add_argument("--anim", nargs=2)
    ap.add_argument("--t", type=float, default=0)
    ap.add_argument("--yaw", type=float, default=25)
    ap.add_argument("--pitch", type=float, default=8)
    ap.add_argument("--size", type=int, default=720)
    ap.add_argument("--dist", type=float, default=95)
    ap.add_argument("--ty", type=float, default=28)
    a = ap.parse_args()
    geo = json.load(open(a.geo))
    anim = Anim(json.load(open(a.anim[0])), a.anim[1]) if a.anim else None
    q = build_quads(geo, anim, a.t)
    render(q, Image.open(a.tex), a.size, a.yaw, a.pitch, a.dist, (0, a.ty, 0)).save(a.out)


if __name__ == "__main__":
    main()
