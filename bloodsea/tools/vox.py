"""Sculpting with distance fields: shapes are written as signed distance functions (bone = capsules, skulls = ellipsoids
with sockets cut out), sampled on a fine voxel grid, and the filled voxels merged into as few boxes as possible.
The boxes become Minecraft block-model elements or GeckoLib cubes."""
import math
import numpy as np


# ============================================================================ space
class Grid:
    """a sampling grid: res voxels per block, covering [lo, hi) in voxels on each axis"""

    def __init__(self, lo, hi):
        self.lo = np.array(lo, float)
        self.hi = np.array(hi, float)
        shape = (self.hi - self.lo).astype(int)
        xs = np.arange(shape[0]) + self.lo[0] + 0.5
        ys = np.arange(shape[1]) + self.lo[1] + 0.5
        zs = np.arange(shape[2]) + self.lo[2] + 0.5
        self.X, self.Y, self.Z = np.meshgrid(xs, ys, zs, indexing="ij")


def rot_matrix(rx=0.0, ry=0.0, rz=0.0):
    rx, ry, rz = map(math.radians, (rx, ry, rz))
    Rx = np.array([[1, 0, 0], [0, math.cos(rx), -math.sin(rx)], [0, math.sin(rx), math.cos(rx)]])
    Ry = np.array([[math.cos(ry), 0, math.sin(ry)], [0, 1, 0], [-math.sin(ry), 0, math.cos(ry)]])
    Rz = np.array([[math.cos(rz), -math.sin(rz), 0], [math.sin(rz), math.cos(rz), 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


class Space:
    """coordinates seen through a transform: shapes are written in their own frame and placed with move/rotate"""

    def __init__(self, X, Y, Z):
        self.X, self.Y, self.Z = X, Y, Z

    def moved(self, t=(0, 0, 0), rot=(0, 0, 0), scale=1.0):
        R = rot_matrix(*rot).T  # inverse rotation
        x, y, z = self.X - t[0], self.Y - t[1], self.Z - t[2]
        nx = (R[0, 0] * x + R[0, 1] * y + R[0, 2] * z) / scale
        ny = (R[1, 0] * x + R[1, 1] * y + R[1, 2] * z) / scale
        nz = (R[2, 0] * x + R[2, 1] * y + R[2, 2] * z) / scale
        return Space(nx, ny, nz)


# ============================================================================ shapes (signed distance: < 0 inside)
def sphere(s, c, r):
    return np.sqrt((s.X - c[0]) ** 2 + (s.Y - c[1]) ** 2 + (s.Z - c[2]) ** 2) - r


def ellipsoid(s, c, rad):
    k = np.sqrt(((s.X - c[0]) / rad[0]) ** 2 + ((s.Y - c[1]) / rad[1]) ** 2 + ((s.Z - c[2]) / rad[2]) ** 2)
    return (k - 1) * min(rad)


def capsule(s, a, b, r0, r1=None):
    """a rod from a to b, radius r0 at a narrowing (or swelling) to r1 at b"""
    r1 = r0 if r1 is None else r1
    a = np.array(a, float)
    b = np.array(b, float)
    ab = b - a
    L2 = max(1e-9, ab @ ab)
    px, py, pz = s.X - a[0], s.Y - a[1], s.Z - a[2]
    t = np.clip((px * ab[0] + py * ab[1] + pz * ab[2]) / L2, 0, 1)
    dx, dy, dz = px - ab[0] * t, py - ab[1] * t, pz - ab[2] * t
    return np.sqrt(dx * dx + dy * dy + dz * dz) - (r0 + (r1 - r0) * t)


def box(s, c, half):
    q = np.stack([np.abs(s.X - c[0]) - half[0], np.abs(s.Y - c[1]) - half[1], np.abs(s.Z - c[2]) - half[2]])
    return np.linalg.norm(np.maximum(q, 0), axis=0) + np.minimum(q.max(axis=0), 0)


def union(*ds):
    out = ds[0]
    for d in ds[1:]:
        out = np.minimum(out, d)
    return out


def smooth_union(a, b, k):
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0, 1)
    return b * (1 - h) + a * h - k * h * (1 - h)


def cut(a, b):
    return np.maximum(a, -b)


def chain(s, pts, r0, r1):
    """a curved bone through a list of points, tapering"""
    n = len(pts) - 1
    d = None
    for i in range(n):
        ra = r0 + (r1 - r0) * i / n
        rb = r0 + (r1 - r0) * (i + 1) / n
        c = capsule(s, pts[i], pts[i + 1], ra, rb)
        d = c if d is None else np.minimum(d, c)
    return d


def arc(start, direction, up, length, bend, n=10):
    """points along a bent line: start, initial direction, the axis it bends toward, total length, total bend (deg)"""
    p = np.array(start, float)
    d = np.array(direction, float)
    d /= np.linalg.norm(d)
    u = np.array(up, float)
    pts = [p.copy()]
    step = length / n
    for i in range(n):
        p = p + d * step
        pts.append(p.copy())
        a = math.radians(bend / n)
        d = d * math.cos(a) + u * math.sin(a)
        d /= np.linalg.norm(d)
        u = u - d * (u @ d)
        if np.linalg.norm(u) > 1e-6:
            u /= np.linalg.norm(u)
    return pts


# ============================================================================ voxels to boxes
def greedy(mask):
    """merge filled voxels into boxes: list of (x0, y0, z0, x1, y1, z1) in grid indices"""
    m = mask.copy()
    nx, ny, nz = m.shape
    boxes = []
    filled = np.argwhere(m)
    for x, y, z in filled:
        if not m[x, y, z]:
            continue
        x1 = x + 1
        while x1 < nx and m[x1, y, z]:
            x1 += 1
        y1 = y + 1
        while y1 < ny and m[x:x1, y1, z].all():
            y1 += 1
        z1 = z + 1
        while z1 < nz and m[x:x1, y:y1, z1].all():
            z1 += 1
        m[x:x1, y:y1, z:z1] = False
        boxes.append((int(x), int(y), int(z), int(x1), int(y1), int(z1)))
    return boxes


def hollow(mask):
    """drop voxels nobody can see (all six neighbours filled): fewer, larger boxes are not affected"""
    inner = mask.copy()
    inner[1:-1, 1:-1, 1:-1] = (mask[1:-1, 1:-1, 1:-1] & mask[:-2, 1:-1, 1:-1] & mask[2:, 1:-1, 1:-1] &
                               mask[1:-1, :-2, 1:-1] & mask[1:-1, 2:, 1:-1] & mask[1:-1, 1:-1, :-2] & mask[1:-1, 1:-1, 2:])
    inner[0, :, :] = inner[-1, :, :] = False
    inner[:, 0, :] = inner[:, -1, :] = False
    inner[:, :, 0] = inner[:, :, -1] = False
    return inner


def boxes_by_material(labels, materials):
    """labels: int grid (0 = empty, k = materials[k-1]); returns [(material, box)]"""
    out = []
    for k, name in enumerate(materials, 1):
        for b in greedy(labels == k):
            out.append((name, b))
    return out


# ============================================================================ exporters
def _wrap(a, b):
    if b - a >= 16:
        return 0.0, 16.0
    off = math.floor(a / 16) * 16
    a, b = a - off, b - off
    if b > 16:
        a, b = 16 - (b - a), 16
    return a, b


def _uv(frm, to, face):
    x1, y1, z1 = frm
    x2, y2, z2 = to
    if face in ("north", "south"):
        u0, u1, v0, v1 = x1, x2, 16 - y2, 16 - y1
    elif face in ("east", "west"):
        u0, u1, v0, v1 = z1, z2, 16 - y2, 16 - y1
    else:
        u0, u1, v0, v1 = x1, x2, z1, z2
    u0, u1 = _wrap(u0, u1)
    v0, v1 = _wrap(v0, v1)
    return [round(u0, 3), round(v0, 3), round(u1, 3), round(v1, 3)]


def block_elements(mboxes, grid, res, offset=(8, 0, 8)):
    """boxes (grid indices) to block-model elements, in pixels; the grid origin (0,0,0) lands at offset"""
    s = 16.0 / res
    els = []
    for mat, (x0, y0, z0, x1, y1, z1) in mboxes:
        frm = [grid.lo[0] + x0, grid.lo[1] + y0, grid.lo[2] + z0]
        to = [grid.lo[0] + x1, grid.lo[1] + y1, grid.lo[2] + z1]
        frm = [round(min(32, max(-16, offset[i] + frm[i] * s)), 3) for i in range(3)]
        to = [round(min(32, max(-16, offset[i] + to[i] * s)), 3) for i in range(3)]
        if any(to[i] - frm[i] <= 0 for i in range(3)):
            continue
        faces = {f: {"texture": "#" + mat, "uv": _uv(frm, to, f)} for f in ("north", "south", "east", "west", "up", "down")}
        els.append({"from": frm, "to": to, "faces": faces})
    return els


def geo_cubes(mboxes, grid, px_per_voxel, tex_size, mat_uv, rng):
    """boxes to GeckoLib cubes (pixels): each face samples a random window of its material's region of the texture"""
    cubes = []
    for mat, (x0, y0, z0, x1, y1, z1) in mboxes:
        o = [(grid.lo[0] + x0) * px_per_voxel, (grid.lo[1] + y0) * px_per_voxel, (grid.lo[2] + z0) * px_per_voxel]
        sz = [(x1 - x0) * px_per_voxel, (y1 - y0) * px_per_voxel, (z1 - z0) * px_per_voxel]
        cubes.append(geo_cube(o, sz, mat_uv[mat], rng))
    return cubes


def geo_cube(origin, size, region, rng, pivot=None, rotation=None, window=16):
    """one cube; region = (u, v, w, h) of the texture this material may use"""
    u0, v0, w, h = region
    faces = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        ww = min(window, w)
        hh = min(window, h)
        u = u0 + rng.randint(0, max(0, w - ww))
        v = v0 + rng.randint(0, max(0, h - hh))
        faces[f] = {"uv": [u, v], "uv_size": [ww, hh]}
    c = {"origin": [round(x, 3) for x in origin], "size": [round(x, 3) for x in size], "uv": faces}
    if rotation is not None:
        c["pivot"] = [round(x, 3) for x in pivot]
        c["rotation"] = [round(x, 3) for x in rotation]
    return c
