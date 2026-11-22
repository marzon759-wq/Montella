#!/usr/bin/env python3
"""Offline renderer for the generated vanilla model: same cube/UV/rotation rules as ModelPart, same
two-light entity shading, glow layer added on top full-bright.

python3 preview.py VARIANT out.png --yaw 30 --pitch 10 --size 900 [--dist 1.0] [--glow 1]
"""
import argparse, json, math, os
import numpy as np
from PIL import Image
from geom import rot_zyx, mat

HERE = os.path.dirname(os.path.abspath(__file__))


def load(path):
    d = json.load(open(path))
    parts = {p["name"]: p for p in d["parts"]}
    return d, parts


def world(parts, name, cache, pose=None):
    if name in cache:
        return cache[name]
    p = parts[name]
    rx, ry, rz = p["rot"]
    if pose and name in pose:
        a = pose[name]
        rx, ry, rz = rx + a[0], ry + a[1], rz + a[2]
    m = mat(t=np.array(p["pivot"]) / 16) @ mat(rot_zyx(rx, ry, rz))
    if p["parent"]:
        m = world(parts, p["parent"], cache, pose) @ m
    cache[name] = m
    return m


def quads(d, parts, pose=None):
    cache = {}
    Vs, UVs = [], []
    for p in d["parts"]:
        W = world(parts, p["name"], cache, pose)
        for u, v, x0, y0, z0, w, h, dd in p["cubes"]:
            x1, y1, z1 = x0 + w, y0 + h, z0 + dd
            V7 = (x0, y0, z0); V0 = (x1, y0, z0); V1 = (x1, y1, z0); V2 = (x0, y1, z0)
            V3 = (x0, y0, z1); V4 = (x1, y0, z1); V5 = (x1, y1, z1); V6 = (x0, y1, z1)
            f4, f5, f6, f7, f8, f9 = u, u + dd, u + dd + w, u + dd + w + w, u + dd + w + dd, u + dd + w + dd + w
            f10, f11, f12 = v, v + dd, v + dd + h
            for verts, u1, v1, u2, v2 in (((V4, V3, V7, V0), f5, f10, f6, f11), ((V1, V2, V6, V5), f6, f11, f7, f10),
                                          ((V7, V3, V6, V2), f4, f11, f5, f12), ((V0, V7, V2, V1), f5, f11, f6, f12),
                                          ((V5, V4, V0, V1), f6, f11, f8, f12), ((V3, V4, V5, V6), f8, f11, f9, f12)):
                P = np.array(verts, float) / 16
                P = (np.c_[P, np.ones(4)] @ W.T)[:, :3]
                Vs.append(P)
                UVs.append(((u2, v1), (u1, v1), (u1, v2), (u2, v2)))
    V = np.array(Vs)
    V = V * np.array([-1, -1, 1]) + np.array([0, 1.501, 0])   # LivingEntityRenderer: scale(-1,-1,1), translate(0,-1.501,0)
    return V, np.array(UVs, float)


def render(variant, out, yaw=30, pitch=10, size=900, aspect=1.0, dist=1.0, glow=1.0, pose=None, bg="storm",
           json_path=None, tex_dir=None, fov=35, target=None):
    d, parts = load(json_path or os.path.join(HERE, "out", "kaiju.json"))
    tex_dir = tex_dir or os.path.join(HERE, "out")
    T = np.asarray(Image.open(os.path.join(tex_dir, f"kaiju_{variant}.png")).convert("RGBA"), float)
    G = np.asarray(Image.open(os.path.join(tex_dir, f"kaiju_{variant}_glow.png")).convert("RGB"), float)
    V, UV = quads(d, parts, pose)
    lo, hi = V.reshape(-1, 3).min(0), V.reshape(-1, 3).max(0)
    center = (lo + hi) / 2 if target is None else np.array(target)
    radius = np.linalg.norm(hi - lo) / 2
    W_, H_ = int(size * aspect), size
    cy, sy = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    fwd = np.array([-sy * cp, -sp, -cy * cp])          # camera looks along fwd; yaw 0 = looking at the face (-z side of model is +z display)
    fwd = -fwd
    D = radius / math.tan(math.radians(fov / 2)) * 1.05 * dist
    eye = center - fwd * D
    right = np.cross(fwd, [0, 1, 0]); right /= np.linalg.norm(right)
    up = np.cross(right, fwd)
    f = (H_ / 2) / math.tan(math.radians(fov / 2))

    Pc = V - eye
    xs, ys, zs = Pc @ right, Pc @ up, Pc @ fwd
    sx = W_ / 2 + f * xs / zs
    sy_ = H_ / 2 - f * ys / zs

    # lighting (vanilla entity lights)
    n = np.cross(V[:, 1] - V[:, 0], V[:, 2] - V[:, 0])
    nn = np.linalg.norm(n, axis=1, keepdims=True); nn[nn == 0] = 1
    n /= nn
    L0 = np.array([0.2, 1.0, -0.7]); L0 /= np.linalg.norm(L0)
    L1 = np.array([-0.2, 1.0, 0.7]); L1 /= np.linalg.norm(L1)
    # flat faces of a box: pick the sign so the normal points away from the quad's cube centre is unknown here;
    # use two-sided lighting like the vanilla no-cull entity pass (abs of the normal dot is wrong for backfaces,
    # so orient normals toward the camera)
    tocam = (eye - V.mean(1))
    flip = np.sign(np.sum(n * tocam, 1, keepdims=True)); flip[flip == 0] = 1
    n *= flip
    light = np.clip(0.4 + 0.6 * (np.clip(n @ L0, 0, None) + np.clip(n @ L1, 0, None)), 0, 1)

    img = background(W_, H_, bg)
    zbuf = np.full((H_, W_), np.inf)
    col = img.copy()
    for q in range(len(V)):
        if np.any(zs[q] <= 0.05):
            continue
        for tri in ((0, 1, 2), (0, 2, 3)):
            X, Y, Z = sx[q, list(tri)], sy_[q, list(tri)], zs[q, list(tri)]
            x0, x1 = int(max(0, math.floor(X.min()))), int(min(W_ - 1, math.ceil(X.max())))
            y0, y1 = int(max(0, math.floor(Y.min()))), int(min(H_ - 1, math.ceil(Y.max())))
            if x1 < x0 or y1 < y0:
                continue
            den = (Y[1] - Y[2]) * (X[0] - X[2]) + (X[2] - X[1]) * (Y[0] - Y[2])
            if abs(den) < 1e-9:
                continue
            gx, gy = np.meshgrid(np.arange(x0, x1 + 1) + 0.5, np.arange(y0, y1 + 1) + 0.5)
            a = ((Y[1] - Y[2]) * (gx - X[2]) + (X[2] - X[1]) * (gy - Y[2])) / den
            b = ((Y[2] - Y[0]) * (gx - X[2]) + (X[0] - X[2]) * (gy - Y[2])) / den
            c = 1 - a - b
            inside = (a >= -1e-6) & (b >= -1e-6) & (c >= -1e-6)
            if not inside.any():
                continue
            iz = a / Z[0] + b / Z[1] + c / Z[2]
            z = 1 / iz
            region = zbuf[y0:y1 + 1, x0:x1 + 1]
            ok = inside & (z < region)
            if not ok.any():
                continue
            uv = UV[q, list(tri)]
            u = (a * uv[0, 0] / Z[0] + b * uv[1, 0] / Z[1] + c * uv[2, 0] / Z[2]) * z
            v = (a * uv[0, 1] / Z[0] + b * uv[1, 1] / Z[1] + c * uv[2, 1] / Z[2]) * z
            ui = np.clip(np.floor(u).astype(int), 0, T.shape[1] - 1)
            vi = np.clip(np.floor(v).astype(int), 0, T.shape[0] - 1)
            texel = T[vi, ui]
            ok &= texel[..., 3] > 8
            if not ok.any():
                continue
            rgb = texel[..., :3] * light[q] + G[vi, ui] * glow
            region[ok] = z[ok]
            cr = col[y0:y1 + 1, x0:x1 + 1]
            cr[ok] = rgb[ok]
    # soft glow bloom
    out_img = np.clip(col, 0, 255).astype(np.uint8)
    im = Image.fromarray(out_img)
    if glow > 0:
        gmask = Image.fromarray(np.clip(col - img, 0, 255).astype(np.uint8))
    im.save(out)
    return im


def background(W, H, kind):
    y = np.linspace(0, 1, H)[:, None, None]
    if kind == "storm":
        top, bot = np.array([70, 74, 80.0]), np.array([150, 150, 148.0])
    elif kind == "night":
        top, bot = np.array([8, 10, 16.0]), np.array([24, 28, 36.0])
    else:
        top, bot = np.array([235, 235, 235.0]), np.array([235, 235, 235.0])
    return np.broadcast_to(top * (1 - y) + bot * y, (H, W, 3)).copy()


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("variant")
    ap.add_argument("out")
    ap.add_argument("--yaw", type=float, default=30)
    ap.add_argument("--pitch", type=float, default=10)
    ap.add_argument("--size", type=int, default=900)
    ap.add_argument("--aspect", type=float, default=1.0)
    ap.add_argument("--dist", type=float, default=1.0)
    ap.add_argument("--glow", type=float, default=1.0)
    ap.add_argument("--bg", default="storm")
    a = ap.parse_args()
    render(a.variant, a.out, a.yaw, a.pitch, a.size, a.aspect, a.dist, a.glow, bg=a.bg)
