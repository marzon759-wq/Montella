#!/usr/bin/env python3
"""The giant dead of the Blood Sea as GeckoLib models: the leviathan (spine segments with rib arches, a long skull, a
tapering tail), the Maw (two rings of curved fangs and a gum ridge), giant human rib cages and skulls half sunk in the
Bone Reach. Curved bones are chains of rotated, tapering cubes; skulls are sculpted with distance fields."""
import json, math, os, random
import numpy as np
from PIL import Image
import vox
from vox import Grid, Space, sphere, ellipsoid, capsule, box, union, cut, chain, smooth_union
import gen_props

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, "../common/src/main/resources/assets/bloodsea")
TEX = 512
# GeckoLib mirrors x when it reads bedrock geometry, so a turn about z runs the other way
ROT_SIGN = -1
# texture regions (u, v, w, h)
REG = {"bone": (0, 0, 256, 256), "grime": (256, 0, 256, 256), "flesh": (0, 256, 256, 256), "tooth": (256, 256, 128, 128),
       "dark": (384, 256, 128, 128), "eye": (256, 384, 128, 128)}


# collision: cells (in blocks, model space) each giant fills, written next to its model
COLL = set()
SEGS = []


def mark_segment(p0, p1, t):
    """cells along a rod of thickness t px from p0 to p1 (px)"""
    p0 = np.array(p0, float)
    p1 = np.array(p1, float)
    L = np.linalg.norm(p1 - p0)
    r = max(t / 2, 4)
    for k in range(int(L / 4) + 1):
        c = p0 + (p1 - p0) * (k * 4 / max(L, 1e-6))
        for dx in (-r, 0, r):
            for dy in (-r, 0, r):
                for dz in (-r, 0, r):
                    COLL.add((int(math.floor((c[0] + dx * 0.7) / 16)), int(math.floor((c[1] + dy * 0.7) / 16)), int(math.floor((c[2] + dz * 0.7) / 16))))


def mark_box(o, sz):
    for x in range(int(math.floor(o[0] / 16)), int(math.floor((o[0] + sz[0] - 0.01) / 16)) + 1):
        for y in range(int(math.floor(o[1] / 16)), int(math.floor((o[1] + sz[1] - 0.01) / 16)) + 1):
            for z in range(int(math.floor(o[2] / 16)), int(math.floor((o[2] + sz[2] - 0.01) / 16)) + 1):
                COLL.add((x, y, z))


def mark_voxels(mask, grid, px):
    for i, j, k in np.argwhere(mask):
        COLL.add((int(math.floor((grid.lo[0] + i + 0.5) * px / 16)), int(math.floor((grid.lo[1] + j + 0.5) * px / 16)),
                  int(math.floor((grid.lo[2] + k + 0.5) * px / 16))))


def write_collision(name):
    cells = sorted(COLL)
    p = os.path.join(HERE, "../common/src/main/resources/data/bloodsea/giant_collision", name + ".json")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w") as f:
        json.dump({"cells": [list(c) for c in cells]}, f, separators=(",", ":"))
    COLL.clear()
    return len(cells)


def W(path, obj):
    p = os.path.join(A, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w") as f:
        json.dump(obj, f, separators=(",", ":"))


def geo(name, bones, bounds=(64, 48)):
    W(f"geo/{name}.geo.json", {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.bloodsea.{name}", "texture_width": TEX, "texture_height": TEX,
                        "visible_bounds_width": bounds[0], "visible_bounds_height": bounds[1], "visible_bounds_offset": [0, bounds[1] / 2, 0]},
        "bones": bones}]})
    n = sum(len(b.get("cubes", [])) for b in bones)
    c = write_collision(name)
    print(f"{name:20s} {n:5d} cubes {c:5d} collision cells")


def cube(rng, origin, size, mat, pivot=None, rotation=None, window=32):
    return vox.geo_cube(origin, size, REG[mat], rng, pivot, rotation, window)


# ============================================================================ curved bones as chains of cubes
def bent_bone(rng, start, L, n, t0, t1, a0, da, mat="bone", plane="xy", jitter=0.0):
    """a bone in a plane, starting at `start` (x, y, z), segment length L, n segments, thickness t0 -> t1, starting angle a0
    (degrees, 0 = +x, 90 = +y) turning da per segment. Each segment is a cube along +x rotated about z, pivoting at its start."""
    cubes = []
    x, y, z = start
    a = a0
    for k in range(n):
        t = t0 + (t1 - t0) * k / max(1, n - 1)
        ang = a + rng.uniform(-jitter, jitter)
        cubes.append(cube(rng, [x, y - t / 2, z - t / 2], [L + t * 0.3, t, t], mat, pivot=[x, y, z], rotation=[0, 0, ROT_SIGN * ang]))
        nx = x + L * math.cos(math.radians(ang))
        ny = y + L * math.sin(math.radians(ang))
        SEGS.append(((x, y, z), (nx, ny, z), t))
        x, y = nx, ny
        a += da
    return cubes


def rib_group(rng, name, root, side, L, n, t0, t1, a0, da, tilt=0.0, mat="bone"):
    """one rib: built pointing +x and mirrored for the left side by a 180 degree turn of its bone"""
    SEGS.clear()
    cubes = bent_bone(rng, (root[0], root[1], root[2]), L, n, t0, t1, a0, da, mat, jitter=1.5)
    # the same rib in the model's frame: tilted about x at its root, mirrored for the left side
    ca, sa = math.cos(math.radians(tilt)), math.sin(math.radians(tilt))

    def place(p):
        x, y, z = p
        y0, z0 = root[1], root[2]
        yy, zz = y - y0, z - z0
        y, z = y0 + yy * ca - zz * sa, z0 + yy * sa + zz * ca
        return (-x if side < 0 else x, y, z)
    for p0, p1, t in SEGS:
        mark_segment(place(p0), place(p1), t)
    b = {"name": name, "parent": "root", "pivot": [0, root[1], root[2]], "cubes": cubes}
    rot = [tilt, 180 if side < 0 else 0, 0]
    if any(rot):
        b["rotation"] = rot
    return b


def vertebra_cubes(rng, z, s, mat="bone"):
    """a vertebra lying with its centrum on the spine line, processes out to the sides and down"""
    mark_box([-20 * s, -20 * s, z - 26 * s], [40 * s, 40 * s, 52 * s])
    mark_box([-58 * s, -6 * s, z - 8 * s], [116 * s, 12 * s, 16 * s])
    c = [cube(rng, [-20 * s, -20 * s, z - 26 * s], [40 * s, 40 * s, 52 * s], mat),
         cube(rng, [-24 * s, -14 * s, z - 18 * s], [48 * s, 28 * s, 36 * s], mat),
         cube(rng, [-58 * s, -6 * s, z - 8 * s], [116 * s, 12 * s, 16 * s], "grime" if rng.random() < 0.3 else mat),
         cube(rng, [-8 * s, -70 * s, z - 10 * s], [16 * s, 52 * s, 20 * s], mat, pivot=[0, -20 * s, z], rotation=[-25, 0, 0])]
    return c


# ============================================================================ the leviathan
def leviathan_segment(seed, ribs=True, scale=1.0, name="leviathan_segment"):
    rng = random.Random(seed)
    root = {"name": "root", "pivot": [0, 0, 0], "cubes": []}
    bones = [root]
    for i in range(8):
        z = -224 + i * 64
        root["cubes"] += vertebra_cubes(rng, z, scale)
        if not ribs:
            continue
        for side in (1, -1):
            if rng.random() < 0.08:
                continue  # a rib long gone
            n = 11 if rng.random() > 0.25 else rng.randint(4, 8)  # broken short
            bones.append(rib_group(rng, f"rib{i}{'l' if side < 0 else 'r'}", (34 * scale, 4 * scale, z), side,
                                   62 * scale, n, 30 * scale, 9 * scale, 8, 11.0 + rng.uniform(-1.2, 1.2), tilt=rng.uniform(-7, 7),
                                   mat="grime" if rng.random() < 0.15 else "bone"))
    geo(name, bones, (160 * scale, 64))


def leviathan_tail(seed, scale, name):
    rng = random.Random(seed)
    root = {"name": "root", "pivot": [0, 0, 0], "cubes": []}
    for i in range(8):
        s = scale * (1 - i * 0.07)
        z = -224 + i * 64
        root["cubes"] += vertebra_cubes(rng, z, s)
        # short spines along the top
        root["cubes"].append(cube(rng, [-5 * s, 18 * s, z - 6 * s], [10 * s, 60 * s, 12 * s], "bone", pivot=[0, 18 * s, z], rotation=[30, 0, 0]))
    geo(name, [root], (60, 30))


PX = 4  # pixels per voxel of the leviathan's skull


def leviathan_skull_sdf(s):
    """a long, flat, crocodile-like skull of something that fed on whales: in units of 8 px, facing -z"""
    cran = ellipsoid(s, (0, 5, 10), (11, 7.5, 12))
    snout = capsule(s, (0, 3.8, 4), (0, 2.6, -26), 8, 4.2)
    d = smooth_union(cran, snout, 4)
    d = union(d, ellipsoid(s, (8.8, 5, 6), (3.2, 4.5, 6.5)), ellipsoid(s, (-8.8, 5, 6), (3.2, 4.5, 6.5)))   # cheekbones
    d = union(d, capsule(s, (6, 10.5, 6), (3, 9, -6), 2.2, 1.2), capsule(s, (-6, 10.5, 6), (-3, 9, -6), 2.2, 1.2))  # brow ridges
    d = union(d, capsule(s, (0, 12, 2), (0, 9.5, 20), 1.4, 2.2))  # a crest down the middle
    for sx in (5.8, -5.8):
        d = cut(d, sphere(s, (sx, 9.0, 1.5), 3.6))                    # the orbits, high and set back
        d = cut(d, ellipsoid(s, (sx * 1.3, 6, 13), (2.0, 3.0, 4.2)))  # openings behind them
        d = cut(d, sphere(s, (sx * 0.38, 5.2, -25.5), 1.4))           # nostrils at the tip
    d = cut(d, ellipsoid(s, (0, 5, 10), (9, 5.5, 10)))  # hollow
    # the lower jaw, hanging open
    jaw = union(capsule(s, (7, -1, 14), (3.8, -12, -22), 2.6, 1.7), capsule(s, (-7, -1, 14), (-3.8, -12, -22), 2.6, 1.7),
                capsule(s, (3.8, -12, -22), (-3.8, -12, -22), 2.0))
    # teeth: down from the snout, up from the jaw, long and hooked
    teeth = None
    for k in range(10):
        t = k / 9
        for sx in (1, -1):
            zu = 3 - t * 27
            xu = sx * (7.4 - t * 3.4)
            a = capsule(s, (xu, 2.0 - t * 0.8, zu), (xu * 0.96, -3.6 - t * 0.6, zu - 0.9), 1.05, 0.15)
            zl = 10 - t * 31
            xl = sx * (6.4 - t * 2.8)
            yl = -1 - 11 * min(1, (14 - zl) / 36)
            b = capsule(s, (xl, yl + 0.6, zl), (xl * 0.96, yl + 5.0, zl - 0.7), 1.0, 0.15)
            teeth = union(a, b) if teeth is None else union(teeth, a, b)
    return d, jaw, teeth


def leviathan_head(seed):
    rng = random.Random(seed)
    k = 8 / PX
    grid = Grid((-16 * k, -16 * k, -32 * k), (16 * k, 18 * k, 28 * k))
    s = Space(grid.X, grid.Y, grid.Z).moved(rot=(-12, 0, 0), scale=k)
    d, jaw, teeth = leviathan_skull_sdf(s)
    labels = np.zeros(d.shape, np.int8)
    labels[(jaw < 0)] = 2
    labels[(d < 0)] = 1
    labels[(teeth < 0) & (labels == 0)] = 3
    low = grid.Y < -7 * k
    labels[(labels == 1) & low] = 2
    mb = vox.boxes_by_material(labels, ["bone", "grime", "tooth"])
    cubes = vox.geo_cubes(mb, grid, PX, TEX, REG, rng)
    mark_voxels(labels > 0, grid, PX)
    geo("leviathan_head", [{"name": "root", "pivot": [0, 0, 0], "cubes": cubes}], (40, 30))


# ============================================================================ the Maw
def maw(seed):
    rng = random.Random(seed)
    bones = [{"name": "root", "pivot": [0, 0, 0], "cubes": []}]
    # the gum: a ring of dark flesh at the waterline
    for k in range(28):
        a = k / 28 * 360
        x, z = 360 * math.cos(math.radians(a)), 360 * math.sin(math.radians(a))
        bones[0]["cubes"].append(cube(rng, [x - 48, 40, z - 48], [96, 52, 96], "flesh", pivot=[x, 60, z], rotation=[0, -a, 0]))
        mark_box([x - 40, 40, z - 40], [80, 52, 80])
    # two rings of fangs, curving in over the throat
    for ring, (R, count, L, t0, n, da) in enumerate(((352, 22, 38, 50, 10, 6.6), (290, 16, 30, 36, 8, 8))):
        for k in range(count):
            a = (k + ring * 0.5) / count * 360
            length = L * rng.uniform(0.8, 1.15)
            SEGS.clear()
            cubes = bent_bone(rng, (R, 50, 0), length, n, t0 * rng.uniform(0.85, 1.1), 5, 92, da + rng.uniform(-2, 2), "tooth", jitter=1)
            ca, sa = math.cos(math.radians(a)), math.sin(math.radians(a))
            for p0, p1, t in SEGS:
                rot = lambda p: (p[0] * ca + p[2] * sa, p[1], -p[0] * sa + p[2] * ca)
                mark_segment(rot(p0), rot(p1), t)
            bones.append({"name": f"fang{ring}_{k}", "parent": "root", "pivot": [0, 0, 0], "rotation": [0, a + rng.uniform(-3, 3), 0], "cubes": cubes})
    geo("maw", bones, (60, 40))


# ============================================================================ the Bone Reach: giant human dead
def giant_ribcage(seed, name, broken):
    rng = random.Random(seed)
    root = {"name": "root", "pivot": [0, 0, 0], "cubes": []}
    bones = [root]
    for i in range(12):
        z = -176 + i * 32
        root["cubes"] += vertebra_cubes(rng, z, 0.6)
    for i in range(8):
        z = -150 + i * 34
        for side in (1, -1):
            if broken and rng.random() < 0.35:
                continue
            n = 12 if not broken or rng.random() > 0.3 else rng.randint(5, 9)
            L = 30 + 6 * math.sin(math.pi * (i + 0.5) / 8)
            bones.append(rib_group(rng, f"rib{i}{'l' if side < 0 else 'r'}", (16, 2, z), side, L, n, 18, 7, 12, 10.0 + rng.uniform(-1, 1),
                                   tilt=-12 + rng.uniform(-6, 6), mat="grime" if rng.random() < 0.2 else "bone"))
    # the sternum lying across the top where the ribs nearly meet
    if not broken:
        root["cubes"].append(cube(rng, [-12, 232, -120], [24, 10, 210], "bone", pivot=[0, 236, -20], rotation=[-6, 0, 0]))
    geo(name, bones, (40, 30))


def giant_skull(seed):
    rng = random.Random(seed)
    grid = Grid((-28, -16, -30), (28, 34, 26))
    s = Space(grid.X, grid.Y, grid.Z)
    sc = 3.0
    ss = s.moved(t=(0, -6, 0), rot=(-14, 25, 12), scale=sc)
    d, e = gen_props.skull(ss, jaw=False, eyes=False)
    d = d * sc
    labels = np.zeros(d.shape, np.int8)
    labels[d < 0] = 1
    labels[(d < 0) & (grid.Y < -2)] = 2
    mb = vox.boxes_by_material(labels, ["bone", "grime"])
    cubes = vox.geo_cubes(mb, grid, 5, TEX, REG, rng)
    mark_voxels(labels > 0, grid, 5)
    geo("giant_skull", [{"name": "root", "pivot": [0, 0, 0], "cubes": cubes}], (24, 20))


# ============================================================================ texture and the empty animation file
def texture():
    def noise(n, seed, scale):
        return gen_props.tex_noise(n, seed, scale)
    img = np.zeros((TEX, TEX, 3))

    def fill(region, arr):
        u, v, w, h = region
        img[v:v + h, u:u + w] = arr

    n = 256
    v = noise(n, 1, 6) * 0.55 + noise(n, 2, 24) * 0.3 + np.random.default_rng(3).random((n, n)) * 0.15
    fill(REG["bone"], gen_props.ramp(v, [(0, "#7a6a50"), (0.45, "#b0a07e"), (0.75, "#cdbf9c"), (1, "#e2d6ba")]))
    v = noise(n, 4, 6) * 0.6 + noise(n, 5, 30) * 0.4
    fill(REG["grime"], gen_props.ramp(v, [(0, "#2a2018"), (0.5, "#544634"), (1, "#8a7a5a")]))
    v = noise(n, 6, 5) * 0.6 + noise(n, 7, 20) * 0.4
    fill(REG["flesh"], gen_props.ramp(v, [(0, "#1e0204"), (0.5, "#4a080c"), (1, "#7a1a1a")]))
    v = noise(128, 8, 5)
    fill(REG["tooth"], gen_props.ramp(v, [(0, "#a8987a"), (1, "#f0e8d4")]))
    fill(REG["dark"], np.full((128, 128, 3), 12.0))
    fill(REG["eye"], gen_props.ramp(noise(128, 9, 4), [(0, "#300000"), (1, "#7a0a0a")]))
    p = os.path.join(A, "textures/entity/giant.png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    Image.fromarray(img.astype(np.uint8), "RGB").convert("RGBA").save(p)
    W("animations/giant.animation.json", {"format_version": "1.8.0", "animations": {}})


def main():
    texture()
    leviathan_segment(11)
    leviathan_tail(12, 0.75, "leviathan_tail")
    leviathan_tail(13, 0.45, "leviathan_tail_end")
    leviathan_head(14)
    maw(15)
    giant_ribcage(16, "giant_ribcage", False)
    giant_ribcage(17, "giant_ribcage_broken", True)
    giant_skull(18)
    print("giants ok")


if __name__ == "__main__":
    main()
