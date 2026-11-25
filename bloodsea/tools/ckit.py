"""Creature kit: sculpt an animated creature as distance fields, one field per bone, voxelise it, merge the voxels into
boxes and paint every visible voxel face into its own texels of a texture atlas. The paint is a function of the voxel's
position, material, ambient occlusion and facing, so the model carries its own shading, streaks and veins the way a
painted sculpt does, instead of a tiled texture.

Coordinates are model pixels (16 per block), y up, the creature faces -z. Bones carry a pivot; their voxels stay in
model space (bedrock geometry is not parented in space, only in motion)."""
import json, math, os
import numpy as np
from PIL import Image
from scipy import ndimage
import vox
from vox import Grid, Space

# GeckoLib reads bedrock geometry mirrored in x (x -> -x), so the face it calls "west" is drawn on the model's +x side
# and "east" on its -x side. FACE_DIR: the face key -> the side of the box it is drawn on (axis 0=x 1=y 2=z, sign).
# FACE_UV: which way that face's texels run as GeckoLib maps them (u axis, u sign, v axis, v sign); a sign of -1 means
# u (or v) grows as the coordinate falls. Read off GeckoLib's BakedModelFactory.VertexSet and GeoQuad.build.
FACE_DIR = {"north": (2, -1), "south": (2, 1), "west": (0, 1), "east": (0, -1), "up": (1, 1), "down": (1, -1)}
FACE_UV = {
    "north": (0, 1, 1, -1), "south": (0, -1, 1, -1),
    "west": (2, 1, 1, -1), "east": (2, -1, 1, -1),
    "up": (0, 1, 2, -1), "down": (0, 1, 2, 1),
}


# ============================================================================ noise
_TABLES = {}


def _table(seed):
    if seed not in _TABLES:
        _TABLES[seed] = np.random.default_rng(seed).random((64, 64, 64)).astype(np.float32)
    return _TABLES[seed]


def vnoise(X, Y, Z, scale, seed=0):
    """smooth value noise in 0..1"""
    t = _table(seed)
    x, y, z = np.asarray(X) / scale, np.asarray(Y) / scale, np.asarray(Z) / scale
    xi, yi, zi = np.floor(x).astype(int), np.floor(y).astype(int), np.floor(z).astype(int)
    fx, fy, fz = x - xi, y - yi, z - zi
    fx, fy, fz = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy), fz * fz * (3 - 2 * fz)
    out = 0
    for dx in (0, 1):
        wx = fx if dx else 1 - fx
        for dy in (0, 1):
            wy = fy if dy else 1 - fy
            for dz in (0, 1):
                wz = fz if dz else 1 - fz
                out = out + t[(xi + dx) % 64, (yi + dy) % 64, (zi + dz) % 64] * wx * wy * wz
    return out


def fbm(X, Y, Z, scale, seed=0, octaves=3):
    v, a, tot = 0, 1.0, 0
    for o in range(octaves):
        v = v + vnoise(X, Y, Z, scale / (2 ** o), seed + o * 17) * a
        tot += a
        a *= 0.5
    return v / tot


def hexcol(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], float)


def ramp(v, stops):
    """v (N,) in 0..1 through colour stops [(pos, '#rrggbb')] -> (N, 3)"""
    v = np.clip(np.asarray(v, float), 0, 1)
    pos = np.array([p for p, _ in stops])
    cols = np.array([hexcol(c) for _, c in stops])
    out = np.zeros(v.shape + (3,))
    for k in range(3):
        out[..., k] = np.interp(v, pos, cols[:, k])
    return out


# ============================================================================ the creature
class Creature:
    def __init__(self, name, paint, tex_width=1024):
        self.name = name
        self.paint = paint          # paint(mat, P, ao, normal, part_tag) -> (N, 3) rgb 0..255
        self.tex_width = tex_width
        self.bones = []
        self.parts = []

    def bone(self, name, pivot, parent=None, rotation=None):
        b = {"name": name, "pivot": [round(float(v), 3) for v in pivot]}
        if parent:
            b["parent"] = parent
        if rotation:
            b["rotation"] = list(rotation)
        self.bones.append(b)
        return name

    def part(self, bone, lo, hi, vs, fn, tag=None, ao=7):
        """voxels of size vs (px) inside the box lo..hi (px) wherever fn(space) gives d < 0; fn returns (d, mat)"""
        self.parts.append((bone, lo, hi, vs, fn, tag, ao))

    # ------------------------------------------------------------------ build
    def build(self, A, quiet=False):
        faces = []   # (bone, origin, size, {face: texel image})
        stats = [0, 0]
        for bone, lo, hi, vs, fn, tag, aok in self.parts:
            glo = [math.floor(l / vs) for l in lo]
            ghi = [math.ceil(h / vs) for h in hi]
            grid = Grid(glo, ghi)
            s = Space(grid.X * vs, grid.Y * vs, grid.Z * vs)
            d, mat = fn(s)
            mat = np.broadcast_to(np.asarray(mat), d.shape).astype(np.int16)
            mask = d < 0
            if not mask.any():
                continue
            occ = ndimage.uniform_filter(mask.astype(np.float32), size=aok, mode="constant")
            ao = np.clip(1.0 - (occ - 0.42) * 2.4, 0.3, 1.2)
            boxes = vox.greedy(mask)
            nx, ny, nz = mask.shape
            # every visible face texel: collect voxel indices, then paint them all at once
            reqs = []
            for (x0, y0, z0, x1, y1, z1) in boxes:
                rng_ = {0: (x0, x1), 1: (y0, y1), 2: (z0, z1)}
                fdict = {}
                for f, (axis, sgn) in FACE_DIR.items():
                    a0, a1 = rng_[axis]
                    k = a1 - 1 if sgn > 0 else a0
                    nb = k + sgn
                    ua, us, va, vsg = FACE_UV[f]
                    u0, u1 = rng_[ua]
                    v0, v1 = rng_[va]
                    uu = np.arange(u0, u1)[::us]
                    vv = np.arange(v0, v1)[::vsg]
                    V, U = np.meshgrid(vv, uu, indexing="ij")
                    idx = [None, None, None]
                    idx[axis] = np.full(U.shape, k)
                    idx[ua] = U
                    idx[va] = V
                    if 0 <= nb < mask.shape[axis]:
                        nidx = list(idx)
                        nidx[axis] = np.full(U.shape, nb)
                        hidden = mask[tuple(nidx)]
                        if hidden.all():
                            continue
                    fdict[f] = (idx, U.shape)
                if not fdict:
                    continue
                reqs.append(((x0, y0, z0, x1, y1, z1), fdict))
            # paint
            allidx = [[], [], []]
            normals = []
            for _, fdict in reqs:
                for f, (idx, shp) in fdict.items():
                    for a in range(3):
                        allidx[a].append(idx[a].ravel())
                    axis, sgn = FACE_DIR[f]
                    n = np.zeros(3)
                    n[axis] = sgn
                    normals.append(np.repeat(n[None], idx[0].size, 0))
            if not reqs:
                continue
            I = tuple(np.concatenate(a) for a in allidx)
            N = np.concatenate(normals)
            P = np.stack([(grid.lo[0] + I[0] + 0.5) * vs, (grid.lo[1] + I[1] + 0.5) * vs, (grid.lo[2] + I[2] + 0.5) * vs], -1)
            cols = self.paint(mat[I], P, ao[I], N, tag)
            cols = np.clip(cols, 0, 255).astype(np.uint8)
            off = 0
            for (x0, y0, z0, x1, y1, z1), fdict in reqs:
                imgs = {}
                for f, (idx, shp) in fdict.items():
                    n = shp[0] * shp[1]
                    imgs[f] = cols[off:off + n].reshape(shp + (3,))
                    off += n
                origin = [(grid.lo[0] + x0) * vs, (grid.lo[1] + y0) * vs, (grid.lo[2] + z0) * vs]
                size = [(x1 - x0) * vs, (y1 - y0) * vs, (z1 - z0) * vs]
                faces.append((bone, origin, size, imgs))
            stats[0] += len(reqs)
        # pack the atlas: shelves, tallest first
        items = []
        for ci, (_, _, _, imgs) in enumerate(faces):
            for f, im in imgs.items():
                items.append((im.shape[0], im.shape[1], ci, f))
        items.sort(key=lambda t: (-t[0], -t[1]))
        W = self.tex_width
        x = y = shelf = 0
        place = {}
        for h, w, ci, f in items:
            if x + w > W:
                y += shelf
                x = shelf = 0
            place[(ci, f)] = (x, y)
            x += w
            shelf = max(shelf, h)
        H = y + shelf
        H = int(math.ceil(H / 16) * 16)
        atlas = np.zeros((H, W, 4), np.uint8)
        bone_cubes = {b["name"]: [] for b in self.bones}
        for ci, (bone, origin, size, imgs) in enumerate(faces):
            uv = {}
            for f, im in imgs.items():
                px, py = place[(ci, f)]
                atlas[py:py + im.shape[0], px:px + im.shape[1], :3] = im
                atlas[py:py + im.shape[0], px:px + im.shape[1], 3] = 255
                uv[f] = {"uv": [px, py], "uv_size": [im.shape[1], im.shape[0]]}
            bone_cubes[bone].append({"origin": [round(v, 3) for v in origin], "size": [round(v, 3) for v in size], "uv": uv})
        stats[1] = len(items)
        bones = []
        for b in self.bones:
            bb = dict(b)
            if bone_cubes[b["name"]]:
                bb["cubes"] = bone_cubes[b["name"]]
            bones.append(bb)
        allo = np.array([c["origin"] for b in bones for c in b.get("cubes", [])])
        alls = np.array([c["size"] for b in bones for c in b.get("cubes", [])])
        ext = np.max(np.abs(np.concatenate([allo, allo + alls])), 0) / 16
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.bloodsea.{self.name}", "texture_width": W, "texture_height": H,
                            "visible_bounds_width": round(float(max(ext[0], ext[2]) * 2 + 2), 1),
                            "visible_bounds_height": round(float(ext[1] * 2 + 2), 1),
                            "visible_bounds_offset": [0, round(float(ext[1]), 1), 0]},
            "bones": bones}]}
        gp = os.path.join(A, f"geo/{self.name}.geo.json")
        os.makedirs(os.path.dirname(gp), exist_ok=True)
        with open(gp, "w") as fo:
            json.dump(geo, fo, separators=(",", ":"))
        tp = os.path.join(A, f"textures/entity/{self.name}.png")
        os.makedirs(os.path.dirname(tp), exist_ok=True)
        Image.fromarray(atlas, "RGBA").save(tp, optimize=True)
        if not quiet:
            print(f"{self.name:16s} {stats[0]:6d} cubes {stats[1]:6d} faces  atlas {W}x{H}  {os.path.getsize(gp) // 1024} KB geo")
        return gp, tp
