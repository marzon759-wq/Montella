#!/usr/bin/env python3
"""Offline renderer for the kaiju GeckoLib model: textured, z-buffered, shadow-mapped, per-bone glow with bloom,
an additive atomic beam, a stormy sky and a blocky ground. Rotation convention as GeckoLib/Blockbench:
R = Rz(z) * Ry(-y) * Rx(-x), model front = -z. Child bone offsets are applied in the parent's rotated frame.
"""
import json, math
import numpy as np
from PIL import Image, ImageFilter


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
    def val(v):
        if isinstance(v, dict):
            v = v.get("post", v.get("pre", v.get("vector")))
        return v

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
                if isinstance(v1, dict) and v1.get("lerp_mode") == "catmullrom":
                    f = f * f * (3 - 2 * f)
                return np.array(Anim.val(v0), float) * (1 - f) + np.array(Anim.val(v1), float) * f
        return np.array(Anim.val(keys[-1][1]), float)

    def pose(self, bone, t):
        if not self.a or bone not in self.a.get("bones", {}):
            return None, None, None
        b = self.a["bones"][bone]
        g = lambda k: self.sample(b[k], t) if k in b else None
        return g("rotation"), g("position"), g("scale")


FACES = {
    "north": [(1, 1, 0), (0, 1, 0), (0, 0, 0), (1, 0, 0)],
    "south": [(0, 1, 1), (1, 1, 1), (1, 0, 1), (0, 0, 1)],
    "east": [(1, 1, 1), (1, 1, 0), (1, 0, 0), (1, 0, 1)],
    "west": [(0, 1, 0), (0, 1, 1), (0, 0, 1), (0, 0, 0)],
    "up": [(1, 1, 1), (0, 1, 1), (0, 1, 0), (1, 1, 0)],
    "down": [(1, 0, 0), (0, 0, 0), (0, 0, 1), (1, 0, 1)],
}


def build_quads(geo, anim=None, t=0.0):
    """returns ([(pts4x3, uv, uv_size, bone)], {bone: world matrix})"""
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
            m = m @ about(b["pivot"], np.diag(np.maximum(asc, 1e-4)))
        world[name] = m
        return m

    quads = []
    for name, b in bones.items():
        m = wm(name)
        for c in b.get("cubes", []):
            o = np.array(c["origin"], float)
            s = np.array(c["size"], float)
            cm = m
            if "rotation" in c:
                cm = m @ about(c.get("pivot", o + s / 2), rot(c["rotation"]))
            for f, cor in FACES.items():
                if f not in c["uv"]:
                    continue
                pts = np.array([o + s * np.array(k) for k in cor])
                pts = (cm @ np.c_[pts, np.ones(4)].T).T[:, :3]
                u = c["uv"][f]
                quads.append((pts, u["uv"], u["uv_size"], name))
    return quads, world


def box_quads(centre, axis, length, radius, rgb):
    """an additive four-sided tube (the beam) from centre along axis"""
    a = np.array(axis, float)
    a /= np.linalg.norm(a)
    up = np.array([0, 1, 0.0]) if abs(a[1]) < 0.9 else np.array([1, 0, 0.0])
    u = np.cross(a, up)
    u /= np.linalg.norm(u)
    v = np.cross(u, a)
    c0 = np.array(centre, float)
    out = []
    for i in range(4):
        th0, th1 = i * math.pi / 2 + math.pi / 4, (i + 1) * math.pi / 2 + math.pi / 4
        p0 = c0 + radius * (math.cos(th0) * u + math.sin(th0) * v)
        p1 = c0 + radius * (math.cos(th1) * u + math.sin(th1) * v)
        seg = 20.0  # short pieces, so the part passing behind the camera can be dropped on its own
        k = 0.0
        while k < length:
            k1 = min(length, k + seg)
            out.append((np.array([p0 + a * k, p1 + a * k, p1 + a * k1, p0 + a * k1]), None, rgb, "__beam__"))
            k = k1
    return out


def sky(W, H, seed=3, tint=(1.0, 1.0, 1.0)):
    rng = np.random.default_rng(seed)
    acc = np.zeros((H, W))
    amp, tot = 1.0, 0.0
    for o in range(6):
        gw, gh = 4 * 2 ** o, max(2, int(4 * 2 ** o * H / W * 2.2))
        g = rng.random((gh, gw))
        acc += np.asarray(Image.fromarray((g * 255).astype(np.uint8)).resize((W, H), Image.BICUBIC), float) / 255 * amp
        tot += amp
        amp *= 0.55
    c = acc / tot
    y = np.linspace(0, 1, H)[:, None]
    v = (0.2 + 0.45 * y ** 1.3) * (0.55 + 0.9 * c)
    return np.clip(np.stack([v * tint[0], v * tint[1], v * 1.04 * tint[2]], -1), 0, 1)


TRIS = (((0, 1, 2), ((0, 0), (1, 0), (1, 1))), ((0, 2, 3), ((0, 0), (1, 1), (0, 1))))


def raster(sp, W, H):
    """yields (ys, xs, l0, l1, l2, tri, st) for the two triangles of a projected quad (screen-space barycentrics)"""
    x0, y0 = int(max(0, math.floor(sp[:, 0].min()))), int(max(0, math.floor(sp[:, 1].min())))
    x1, y1 = int(min(W - 1, math.ceil(sp[:, 0].max()))), int(min(H - 1, math.ceil(sp[:, 1].max())))
    if x1 < x0 or y1 < y0:
        return
    ys, xs = np.mgrid[y0:y1 + 1, x0:x1 + 1]
    px, py = xs + 0.5, ys + 0.5
    for tri, st in TRIS:
        a, b, c = sp[list(tri)]
        v0x, v0y, v1x, v1y = b[0] - a[0], b[1] - a[1], c[0] - a[0], c[1] - a[1]
        den = v0x * v1y - v1x * v0y
        if abs(den) < 1e-9:
            continue
        dx, dy = px - a[0], py - a[1]
        l1 = (dx * v1y - v1x * dy) / den
        l2 = (v0x * dy - dx * v0y) / den
        l0 = 1 - l1 - l2
        inside = (l0 >= -1e-4) & (l1 >= -1e-4) & (l2 >= -1e-4)
        if inside.any():
            yield ys[inside], xs[inside], l0[inside], l1[inside], l2[inside], tri, st


class ShadowMap:
    def __init__(self, quads, L, res=1400):
        L = np.array(L, float) / np.linalg.norm(L)
        self.L = L
        self.lx = np.cross([0, 0, 1.0], L) if abs(L[2]) < 0.9 else np.cross([1.0, 0, 0], L)
        self.lx /= np.linalg.norm(self.lx)
        self.ly = np.cross(L, self.lx)
        pts = np.concatenate([q[0] for q in quads])
        U, V = pts @ self.lx, pts @ self.ly
        self.u0, self.v0 = U.min() - 4, V.min() - 4
        span = max(U.max() - self.u0, V.max() - self.v0) + 8
        self.res = res
        self.k = res / span
        self.d = np.full((res, res), np.inf)
        for pts, uv, us, bone in quads:
            sp = np.c_[(pts @ self.lx - self.u0) * self.k, (pts @ self.ly - self.v0) * self.k, -(pts @ L)]
            for ys, xs, l0, l1, l2, tri, st in raster(sp, res, res):
                a, b, c = sp[list(tri)]
                z = l0 * a[2] + l1 * b[2] + l2 * c[2]
                np.minimum.at(self.d, (ys, xs), z)

    def lit(self, P, bias=0.9):
        u = ((P @ self.lx - self.u0) * self.k).astype(int)
        v = ((P @ self.ly - self.v0) * self.k).astype(int)
        ok = (u >= 0) & (u < self.res) & (v >= 0) & (v < self.res)
        d = -(P @ self.L)
        uu, vv = np.clip(u, 0, self.res - 1), np.clip(v, 0, self.res - 1)
        acc = np.zeros(len(P))
        for du in (-1, 0, 1):  # 3x3 PCF, soft-ish edges
            for dv in (-1, 0, 1):
                s = self.d[np.clip(vv + dv, 0, self.res - 1), np.clip(uu + du, 0, self.res - 1)]
                acc += (d <= s + bias)
        return np.where(ok, acc / 9, 1.0)


def render(quads, tex, glow=None, W=1280, H=720, yaw=25, pitch=8, dist=200, target=(0, 50, 0), fov=40,
           light=(0.45, 0.8, -0.55), ambient=0.36, glow_fn=None, sky_img=None, ground=True, shadows=True,
           ground_col=(0.2, 0.19, 0.18), exposure=1.0, sun=1.0):
    tex = np.asarray(tex.convert("RGBA"), float) / 255
    TH, TW = tex.shape[:2]
    gtex = np.asarray(glow.convert("RGBA"), float) / 255 if glow is not None else None
    yr, pr = math.radians(yaw), math.radians(pitch)
    tgt = np.array(target, float)
    cam = tgt + dist * np.array([math.sin(yr) * math.cos(pr), math.sin(pr), -math.cos(yr) * math.cos(pr)])
    fwd = tgt - cam
    fwd /= np.linalg.norm(fwd)
    right = np.cross(fwd, [0, 1, 0])
    right /= np.linalg.norm(right)
    up = np.cross(right, fwd)
    f = 0.5 * H / math.tan(math.radians(fov) / 2)
    L = np.array(light, float)
    L /= np.linalg.norm(L)
    model = [q for q in quads if q[3] != "__beam__"]
    sm = ShadowMap(model, L) if shadows else None

    img = sky_img.copy() if sky_img is not None else np.full((H, W, 3), 0.1)
    gbuf = np.zeros((H, W, 3))
    zbuf = np.full((H, W), np.inf)

    def proj(pts):
        d = pts - cam
        x, y, z = d @ right, d @ up, d @ fwd
        return np.c_[W / 2 + f * x / z, H / 2 - f * y / z, z]

    items = []
    if ground:
        T_ = 40
        for gx0 in range(-800, 800, T_):
            for gz0 in range(-800, 800, T_):
                items.append((np.array([[gx0, 0, gz0], [gx0 + T_, 0, gz0], [gx0 + T_, 0, gz0 + T_], [gx0, 0, gz0 + T_]], float), "ground", None, None))
    items += model

    for pts, uv, us, bone in items:
        sp = proj(pts)
        if np.any(sp[:, 2] <= 1):
            continue
        n = -np.cross(pts[1] - pts[0], pts[3] - pts[0])  # FACES winding is clockwise seen from outside
        nl = np.linalg.norm(n)
        if nl < 1e-9:
            continue
        n /= nl
        isg = isinstance(uv, str)
        if not isg and n @ (cam - pts[0]) < 0:  # back face
            continue
        diff = max(0.0, float(n @ L)) * sun
        rim = 0.3 * max(0.0, float(n @ np.array([-0.3, 0.25, 0.92]))) ** 3
        fill = 0.12 * max(0.0, float(n[1]))
        gg = (glow_fn(bone) if glow_fn else 1.0) if (gtex is not None and not isg) else 0.0
        for ys, xs, l0, l1, l2, tri, st in raster(sp, W, H):
            a, b, c = sp[list(tri)]
            z = 1 / np.maximum(l0 / a[2] + l1 / b[2] + l2 / c[2], 1e-9)
            closer = z < zbuf[ys, xs]
            if not closer.any():
                continue
            ys, xs, z, l0, l1, l2 = ys[closer], xs[closer], z[closer], l0[closer], l1[closer], l2[closer]
            w0, w1, w2 = l0 / a[2] * z, l1 / b[2] * z, l2 / c[2] * z
            A, B, C = pts[list(tri)]
            P = w0[:, None] * A + w1[:, None] * B + w2[:, None] * C
            lit = sm.lit(P) if (sm is not None and diff > 0) else np.ones(len(z))
            shade = (ambient + fill + rim + diff * lit)[:, None]
            gcol = None
            if isg:
                bxz = (np.floor(P[:, 0] / 16).astype(int) * 7 + np.floor(P[:, 2] / 16).astype(int) * 13) % 5
                col = np.array(ground_col)[None, :] * (0.82 + 0.07 * bxz[:, None]) * shade
                fogf = np.clip((z - 220) / 900, 0, 0.9)[:, None]
                col = col * (1 - fogf) + img[ys, xs] * fogf
                keep = np.ones(len(z), bool)
            else:
                s0, s1, s2 = (np.array(v, float) for v in st)
                S = w0 * s0[0] + w1 * s1[0] + w2 * s2[0]
                T = w0 * s0[1] + w1 * s1[1] + w2 * s2[1]
                tu = np.clip((uv[0] + S * us[0]).astype(int), 0, TW - 1)
                tv = np.clip((uv[1] + T * us[1]).astype(int), 0, TH - 1)
                tc = tex[tv, tu]
                keep = tc[:, 3] > 0.5
                col = tc[:, :3] * shade
                if gg > 0:
                    gc = gtex[tv, tu]
                    gcol = gc[:, :3] * gc[:, 3:4] * gg
                    col = col * (1 - gc[:, 3:4] * min(1.0, gg)) + gcol
            ys, xs = ys[keep], xs[keep]
            img[ys, xs] = col[keep]
            zbuf[ys, xs] = z[keep]
            gbuf[ys, xs] = gcol[keep] if gcol is not None else 0

    # the beam: additive, hidden behind the model but never hiding it
    for pts, _, rgb, _ in (q for q in quads if q[3] == "__beam__"):
        sp = proj(pts)
        if np.any(sp[:, 2] <= 1):
            continue
        for ys, xs, l0, l1, l2, tri, st in raster(sp, W, H):
            a, b, c = sp[list(tri)]
            z = 1 / np.maximum(l0 / a[2] + l1 / b[2] + l2 / c[2], 1e-9)
            vis = z < zbuf[ys, xs] + 0.5
            img[ys[vis], xs[vis]] += np.array(rgb) * 0.5
            gbuf[ys[vis], xs[vis]] += np.array(rgb) * 0.45

    if gbuf.max() > 0:
        g8 = Image.fromarray((np.clip(gbuf, 0, 1) * 255).astype(np.uint8))
        for r_, k in ((W / 200, 0.8), (W / 60, 0.6), (W / 20, 0.45), (W / 7, 0.3)):
            img += np.asarray(g8.filter(ImageFilter.GaussianBlur(r_)), float) / 255 * k
    img *= exposure
    yy, xx = np.mgrid[0:H, 0:W]
    v = 1 - 0.3 * (((xx - W / 2) / (W / 2)) ** 2 + ((yy - H / 2) / (H / 2)) ** 2) ** 1.2
    img *= np.clip(v, 0.3, 1)[..., None]
    img = img / (1 + 0.15 * img)
    return Image.fromarray((np.clip(img * 1.12, 0, 1) * 255).astype(np.uint8))
