"""Vectorised 3D value noise / fBm and Worley (F1, F2) noise for painting textures in model space."""
import numpy as np


def _hash(ix, iy, iz, seed):
    h = (ix.astype(np.int64) * 73856093) ^ (iy.astype(np.int64) * 19349663) ^ (iz.astype(np.int64) * 83492791) ^ (seed * 2654435761)
    h = (h ^ (h >> 13)) * 1274126177
    h = h ^ (h >> 16)
    return (h & 0xFFFFFF) / float(0xFFFFFF)


def value(p, seed=0):
    p = np.asarray(p, float)
    i = np.floor(p).astype(np.int64)
    f = p - i
    f = f * f * (3 - 2 * f)
    out = 0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (f[..., 0] if dx else 1 - f[..., 0]) * (f[..., 1] if dy else 1 - f[..., 1]) * (f[..., 2] if dz else 1 - f[..., 2])
                out = out + w * _hash(i[..., 0] + dx, i[..., 1] + dy, i[..., 2] + dz, seed)
    return out


def fbm(p, octaves=4, seed=0):
    p = np.asarray(p, float)
    tot, amp, s = 0.0, 1.0, 0.0
    for o in range(octaves):
        s = s + amp * value(p * (2 ** o), seed + o * 31)
        tot += amp
        amp *= 0.5
    return s / tot


def worley(p, seed=0):
    p = np.asarray(p, float)
    i = np.floor(p).astype(np.int64)
    f1 = np.full(p.shape[:-1], 9.0)
    f2 = np.full(p.shape[:-1], 9.0)
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                cx, cy, cz = i[..., 0] + dx, i[..., 1] + dy, i[..., 2] + dz
                fp = np.stack([cx + _hash(cx, cy, cz, seed), cy + _hash(cx, cy, cz, seed + 1), cz + _hash(cx, cy, cz, seed + 2)], -1)
                d = np.linalg.norm(fp - p, axis=-1)
                f2 = np.where(d < f1, f1, np.minimum(f2, d))
                f1 = np.minimum(f1, d)
    return f1, f2
