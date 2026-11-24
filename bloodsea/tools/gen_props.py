#!/usr/bin/env python3
"""The bones of the drowned, as real shapes: skulls with sockets and teeth, ribs breaking out of the mud, skeletons lying
where they fell, a hand reaching out of the blood. Each is sculpted from distance fields at 1/32 of a block, merged into
boxes and written as a block model. Also writes registry/ModProps.java with their names and shapes."""
import json, math, os, random
import numpy as np
from PIL import Image
import vox
from vox import Grid, Space, sphere, ellipsoid, capsule, box, union, cut, chain, arc, smooth_union

HERE = os.path.dirname(os.path.abspath(__file__))
R = os.path.join(HERE, "../common/src/main/resources")
A = os.path.join(R, "assets/bloodsea")
JAVA = os.path.join(HERE, "../common/src/main/java/com/krolasyon/bloodsea/registry/ModProps.java")
NS = "bloodsea"
RES = 32  # voxels per block


def W(path, obj):
    p = os.path.join(R, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w") as f:
        json.dump(obj, f, separators=(",", ":"))


# ============================================================================ a human skull (units: 1 = about 1.6 cm)
def skull(s, jaw=True, eyes=True):
    """returns (bone distance, eye distance) in the skull's own frame: it faces -z, sits on y=0"""
    cran = ellipsoid(s, (0, 6.2, 1.5), (4.7, 4.6, 5.9))
    brow = ellipsoid(s, (0, 6.0, -3.4), (3.9, 1.6, 1.5))
    face = ellipsoid(s, (0, 3.4, -3.3), (3.4, 3.0, 3.1))
    cheeks = union(ellipsoid(s, (3.6, 3.6, -2.2), (1.3, 1.1, 2.6)), ellipsoid(s, (-3.6, 3.6, -2.2), (1.3, 1.1, 2.6)))
    arches = union(capsule(s, (3.9, 3.7, -1.5), (4.3, 3.9, 1.6), 0.55), capsule(s, (-3.9, 3.7, -1.5), (-4.3, 3.9, 1.6), 0.55))
    upper = box(s, (0, 1.7, -4.4), (2.3, 0.8, 1.2))
    d = smooth_union(cran, face, 1.2)
    d = smooth_union(d, brow, 0.8)
    d = union(d, cheeks, arches, upper)
    # teeth: the gaps between them
    for i in range(-2, 3):
        d = cut(d, box(s, (i * 0.95, 1.3, -5.5), (0.1, 0.55, 0.5)))
    if jaw:
        pts = [(-3.2, 2.4, 0.4), (-3.1, 0.7, -1.4), (-2.1, 0.0, -4.0), (0, -0.3, -4.9), (2.1, 0.0, -4.0), (3.1, 0.7, -1.4), (3.2, 2.4, 0.4)]
        d = union(d, chain(s, pts, 0.85, 0.85), box(s, (0, 0.7, -4.4), (2.0, 0.45, 0.9)))
    # hollows: the eyes, the nose, the temples, and the inside
    for sx in (1.8, -1.8):  # deep orbits, a little drooping at the outer corner
        d = cut(d, capsule(s, (sx, 4.7, -7.0), (sx * 0.9, 4.5, -2.6), 1.75, 1.3))
        d = cut(d, sphere(s, (sx * 1.15, 4.4, -5.6), 1.35))
    d = cut(d, capsule(s, (0, 3.4, -7.0), (0, 2.8, -3.5), 0.9, 0.7))   # the nose, an upside-down heart
    d = cut(d, capsule(s, (0.35, 2.6, -6.6), (-0.35, 2.6, -6.6), 0.65))
    d = cut(d, ellipsoid(s, (4.9, 5.0, -0.4), (0.9, 1.4, 2.2)))
    d = cut(d, ellipsoid(s, (-4.9, 5.0, -0.4), (0.9, 1.4, 2.2)))
    d = cut(d, ellipsoid(s, (0, 6.2, 1.5), (3.8, 3.7, 4.9)))
    e = None
    if eyes:
        e = union(sphere(s, (1.7, 4.6, -3.6), 1.0), sphere(s, (-1.7, 4.6, -3.6), 1.0))
    return d, e


def long_bone(s, a, b, r=1.0):
    """a femur or a humerus: a shaft with knuckled ends"""
    a = np.array(a, float)
    b = np.array(b, float)
    ab = b - a
    n = ab / np.linalg.norm(ab)
    side = np.cross(n, [0, 1, 0])
    if np.linalg.norm(side) < 1e-3:
        side = np.array([1, 0, 0])
    side = side / np.linalg.norm(side)
    d = capsule(s, a, b, r * 0.8, r * 0.7)
    d = union(d, sphere(s, a + n * r * 0.6, r * 1.45), sphere(s, a + n * r * 0.4 + side * r * 0.9, r * 1.0))
    d = union(d, sphere(s, b - n * r * 0.5 + side * r * 0.8, r * 1.2), sphere(s, b - n * r * 0.5 - side * r * 0.8, r * 1.2))
    return d


def vertebra(s, c, direction, size=1.0):
    c = np.array(c, float)
    dvec = np.array(direction, float)
    dvec /= np.linalg.norm(dvec)
    body = capsule(s, c - dvec * 0.9 * size, c + dvec * 0.9 * size, 1.7 * size)
    up = np.array([0, 1, 0])
    side = np.cross(dvec, up)
    spine = capsule(s, c + up * 1.2 * size, c + up * 3.3 * size - dvec * 1.0 * size, 0.7 * size, 0.4 * size)
    trans = capsule(s, c + side * 2.6 * size + up * 0.6 * size, c - side * 2.6 * size + up * 0.6 * size, 0.55 * size)
    return union(body, spine, trans)


def rib_arc(s, start, outward, length, bend, r0=0.9, r1=0.55, up=(0, 1, 0)):
    return chain(s, arc(start, outward, up, length, bend, 12), r0, r1)


# ============================================================================ the props
def p_skull(s, tilt=(8, 0, 0), jaw=False, drop=0.0, scale=1.45):
    ss = s.moved(t=(0, drop, 0), rot=tilt, scale=scale)
    d, e = skull(ss, jaw=jaw)
    return d * scale, e * scale


def p_skull_up(s):
    return p_skull(s, tilt=(6, 25, 4), jaw=False)


def p_skull_side(s):
    d, e = p_skull(s, tilt=(0, -30, 78), jaw=True, drop=9)
    return d, e


def p_skull_sunk(s):
    return p_skull(s, tilt=(-18, 160, -10), jaw=False, drop=-9)


def p_skull_heap(s):
    rng = random.Random(7)
    ds, es = [], []
    for x, y, z, rx, ry, rz in ((-6, 0, -5, 0, 20, 5), (8, 0, 4, 10, -140, -8), (-4, 0, 9, 0, 200, 70), (6, 11, -4, 25, 40, 10), (-2, 13, 6, -15, 170, -20)):
        ss = s.moved(t=(x, y, z), rot=(rx, ry, rz), scale=1.3)
        d, e = skull(ss, jaw=rng.random() < 0.3)
        ds.append(d * 1.3)
        es.append(e * 1.3)
    d = union(*ds)
    for a, b in (((-14, 2, 12), (13, 3, -10)), ((-12, 4, -13), (14, 5, 12))):
        d = union(d, long_bone(s, a, b, 1.3))
    return d, union(*es)


def p_ribs_rise(s, sink=0):
    d = None
    rng = random.Random(3)
    for i in range(5):
        x = -12 + i * 6 + rng.uniform(-1, 1)
        z = -4 + rng.uniform(-3, 3)
        start = (x, -8 + sink, z)
        out = (0.15 * (i - 2), 1.0, -0.35)
        c = rib_arc(s, start, out, 42 - abs(i - 2) * 4 + rng.uniform(-3, 3), 75 + rng.uniform(-10, 10), 1.5, 0.8, up=(0, 0, 1))
        d = c if d is None else union(d, c)
    return d, None


def p_ribs_rise_sunk(s):
    return p_ribs_rise(s, sink=-12)


def ribcage(s, z0=-16, z1=16, y=3, width=1.0, n=7):
    d = None
    for i in range(n):
        z = z0 + (z1 - z0) * i / (n - 1)
        L = (22 + 10 * math.sin(math.pi * (i + 0.5) / n)) * width
        for side in (1, -1):
            c = rib_arc(s, (side * 2.5, y, z), (side, 0.35, 0), L, 118, 1.1, 0.7, up=(0, 1, 0))
            d = c if d is None else union(d, c)
    for i in range(n + 3):
        z = z0 - 6 + (z1 - z0 + 12) * i / (n + 2)
        v = vertebra(s, (0, y, z), (0, 0, 1), 1.0)
        d = union(d, v)
    return d


def p_ribcage(s):
    return ribcage(s), None


def p_skeleton(s, sink=0.0):
    ss = s.moved(t=(0, sink, 0))
    d = ribcage(ss, -14, 10, 3, 0.82, 6)
    # the skull, fallen back
    sk, e = skull(ss.moved(t=(2, 1, -27), rot=(-70, 15, 10), scale=1.35))
    d = union(d, sk * 1.35)
    e = e * 1.35
    # pelvis
    d = union(d, ellipsoid(ss, (5, 4, 17), (5, 4.5, 2.4)), ellipsoid(ss, (-5, 4, 17), (5, 4.5, 2.4)))
    d = cut(d, sphere(ss, (0, 4, 17), 3.2))
    # legs: femur, tibia, a scatter of foot bones
    for side in (1, -1):
        d = union(d, long_bone(ss, (side * 5, 2.5, 19), (side * 7, 2, 34), 1.35))
        d = union(d, long_bone(ss, (side * 7, 2, 35), (side * 8, 1.8, 46), 1.1))
        for k in range(3):
            d = union(d, capsule(ss, (side * 8 + k - 1, 1.4, 47), (side * 8 + k * 1.4 - 1, 1.2, 50), 0.5))
    # arms by the side, one flung out
    d = union(d, long_bone(ss, (9, 3, -12), (12, 2, 2), 1.0), long_bone(ss, (12, 2, 3), (14, 1.8, 13), 0.8))
    d = union(d, long_bone(ss, (-9, 3, -12), (-20, 2, -16), 1.0), long_bone(ss, (-21, 2, -16), (-30, 1.8, -12), 0.8))
    return d, e


def p_skeleton_sunk(s):
    return p_skeleton(s, sink=-6)


def p_hand(s):
    d = union(capsule(s, (-1.2, -26, 1), (-1.0, 6, 2), 1.3, 1.0), capsule(s, (1.2, -26, -1), (1.0, 6, 0), 1.2, 0.9))
    d = union(d, ellipsoid(s, (0, 9, 1), (3.0, 2.4, 1.6)))
    tips = []
    for i, ang in enumerate((-22, -8, 6, 20)):
        a = math.radians(ang)
        base = np.array([math.sin(a) * 2.6, 11, 1])
        dirv = np.array([math.sin(a) * 0.5, 1, -0.15])
        pts = arc(base, dirv, (0, 0, -1), 18 - abs(i - 1.5) * 1.6, 75, 6)
        d = union(d, chain(s, pts, 0.75, 0.5))
        for p in pts[2::2]:
            d = union(d, sphere(s, p, 0.85))
    pts = arc((3, 8, 1), (1, 0.6, -0.4), (0, 0, -1), 12, 45, 4)
    d = union(d, chain(s, pts, 0.8, 0.5))
    return d, None


def p_bone_pile(s):
    d = None
    for a, b, r in (((-18, 2, -6), (14, 2.4, 6), 1.5), ((-12, 2.2, 12), (16, 2.6, -10), 1.3), ((-6, 4.5, -16), (4, 4.2, 15), 1.2),
                    ((6, 2, 14), (20, 2.2, 2), 1.0), ((-20, 2, 4), (-8, 2, 18), 0.9)):
        c = long_bone(s, a, b, r)
        d = c if d is None else union(d, c)
    sk, e = skull(s.moved(t=(-4, 2, -2), rot=(10, 120, 70), scale=1.25))
    return union(d, sk * 1.25), e * 1.25


def p_spine(s):
    pts = arc((-20, 2.2, -18), (1, 0, 0.6), (0, 0, 1), 52, 120, 14)
    d = None
    for i in range(len(pts) - 1):
        dirv = np.array(pts[i + 1]) - np.array(pts[i])
        v = vertebra(s, pts[i], dirv, 1.0 - i * 0.03)
        d = v if d is None else union(d, v)
    return d, None


def p_heart(s):
    ss = s.moved(t=(0, 15, 0), rot=(0, 0, -18))
    d = ellipsoid(ss, (0, -1, 0), (6.5, 8.0, 5.8))
    d = smooth_union(d, ellipsoid(ss, (3.5, 4.5, -1), (3.6, 3.3, 3.4)), 1.5)
    d = smooth_union(d, ellipsoid(ss, (-3.2, 4, 1), (3.3, 3.0, 3.0)), 1.5)
    tip = ellipsoid(ss, (1.5, -8, 0), (2.5, 3.0, 2.4))
    d = smooth_union(d, tip, 2.0)
    aorta = chain(ss, [(0, 6, 0), (0, 11, 0.5), (-3, 13, 1), (-6, 10, 1)], 1.8, 1.6)
    pulm = chain(ss, [(2.5, 6, -2), (4, 10.5, -2.5), (7, 11, -2)], 1.5, 1.3)
    veins = union(capsule(ss, (-5, 6, 2), (-7, 9, 4), 1.0), capsule(ss, (5, 5, 2), (7.5, 7, 4), 0.9))
    vessels = union(aorta, pulm, veins)
    return d, vessels


PROPS = {
    # name: (function, materials of the second field, lit, collision box in px or None, light)
    "skull": (p_skull_up, "eye", True, (3, 0, 3, 13, 9, 13)),
    "skull_side": (p_skull_side, "eye", True, (2, 0, 2, 14, 8, 14)),
    "skull_sunk": (p_skull_sunk, "eye", True, None),
    "skull_heap": (p_skull_heap, "eye", True, (0, 0, 0, 16, 14, 16)),
    "ribs": (p_ribs_rise, None, False, None),
    "ribs_sunk": (p_ribs_rise_sunk, None, False, None),
    "ribcage": (p_ribcage, None, False, (0, 0, 0, 16, 6, 16)),
    "skeleton": (p_skeleton, "eye", True, (0, 0, 0, 16, 4, 16)),
    "skeleton_sunk": (p_skeleton_sunk, "eye", True, None),
    "hand": (p_hand, None, False, None),
    "bone_pile": (p_bone_pile, "eye", True, (0, 0, 0, 16, 5, 16)),
    "spine": (p_spine, None, False, (0, 0, 0, 16, 4, 16)),
}


# ============================================================================ textures
def tex_noise(n, seed, scale):
    rng = np.random.default_rng(seed)
    g = rng.random((scale + 1, scale + 1))
    xs = np.linspace(0, scale, n, endpoint=False)
    x0 = np.floor(xs).astype(int)
    t = xs - x0
    t = t * t * (3 - 2 * t)
    a = g[np.ix_(x0, x0)]
    b = g[np.ix_(x0, x0 + 1)]
    c = g[np.ix_(x0 + 1, x0)]
    d = g[np.ix_(x0 + 1, x0 + 1)]
    ty, tx = np.meshgrid(t, t, indexing="ij")
    return a * (1 - tx) * (1 - ty) + b * tx * (1 - ty) + c * (1 - tx) * ty + d * tx * ty


def ramp(v, stops):
    stops = [(t, np.array([int(c[i:i + 2], 16) for i in (1, 3, 5)], float)) for t, c in stops]
    out = np.zeros(v.shape + (3,))
    for i in range(len(stops) - 1):
        t0, c0 = stops[i]
        t1, c1 = stops[i + 1]
        m = (v >= t0) & (v <= t1)
        k = ((v - t0) / (t1 - t0))[..., None]
        out[m] = (c0 + (c1 - c0) * k)[m]
    out[v < stops[0][0]] = stops[0][1]
    out[v > stops[-1][0]] = stops[-1][1]
    return out


def save_tex(arr, name, alpha=255):
    a = np.clip(arr, 0, 255).astype(np.uint8)
    im = Image.fromarray(a, "RGB").convert("RGBA")
    p = os.path.join(A, "textures/block/prop", name + ".png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    im.save(p)


def textures():
    n = 32
    v = tex_noise(n, 1, 4) * 0.6 + tex_noise(n, 2, 9) * 0.3 + np.random.default_rng(3).random((n, n)) * 0.1
    save_tex(ramp(v, [(0, "#8c7c62"), (0.45, "#bfae8c"), (0.75, "#d8cbab"), (1, "#e9dfc6")]), "bone")
    v = tex_noise(n, 4, 4) * 0.6 + tex_noise(n, 5, 10) * 0.4
    save_tex(ramp(v, [(0, "#2a221c"), (0.5, "#4e4234"), (1, "#7a6a52")]), "bone_grime")
    save_tex(np.full((n, n, 3), (14, 8, 6), float), "eye")
    v = tex_noise(n, 6, 3)
    save_tex(ramp(v, [(0, "#ff2a10"), (1, "#ff8a40")]), "eye_lit")
    v = tex_noise(n, 7, 4) * 0.7 + tex_noise(n, 8, 12) * 0.3
    save_tex(ramp(v, [(0, "#4a0408"), (0.5, "#8a1014"), (0.85, "#b02a2a"), (1, "#c8504a")]), "heart")
    v = tex_noise(n, 9, 6)
    save_tex(ramp(v, [(0, "#2a0a20"), (1, "#5a1a3a")]), "vessel")


# ============================================================================ build every prop
def build(name, fn, second, lit_capable):
    grid = Grid((-48, -32, -48), (48, 64, 48))
    s = Space(grid.X, grid.Y, grid.Z)
    d, e = fn(s)
    bone = d < 0
    labels = np.zeros(bone.shape, np.int8)
    # grime: low down and in the hollows, as if it had lain in the mud
    rng = np.random.default_rng(hash(name) % 1000)
    dirt = tex = tex_noise(96, 11, 6)
    low = grid.Y < (2 if "sunk" not in name and name != "hand" else -4)
    grime = bone & (low | (np.random.default_rng(5).random(bone.shape) < 0.0))
    labels[bone] = 1
    labels[grime] = 2
    mats = ["bone", "grime"]
    if e is not None:
        em = (e < 0) & ~bone
        labels[em] = 3
        mats.append("eye" if second == "eye" else "vessel")
    if name == "heart":
        labels[bone] = 1
        mats = ["heart", "grime", "vessel"]
    vis = labels.copy()
    vis[vox.hollow(labels > 0)] = 0
    # inner voxels can go: boxes still cover them because greedy merges the visible shell only
    mb = vox.boxes_by_material(labels, mats)
    els = vox.block_elements(mb, grid, RES)
    tex = {"bone": f"{NS}:block/prop/bone", "grime": f"{NS}:block/prop/bone_grime", "eye": f"{NS}:block/prop/eye",
           "vessel": f"{NS}:block/prop/vessel", "heart": f"{NS}:block/prop/heart", "particle": f"{NS}:block/prop/bone"}
    W(f"assets/{NS}/models/block/prop/{name}.json", {"textures": tex, "elements": els, "ambientocclusion": False})
    if lit_capable:
        t2 = dict(tex)
        t2["eye"] = f"{NS}:block/prop/eye_lit"
        W(f"assets/{NS}/models/block/prop/{name}_lit.json", {"textures": t2, "elements": els, "ambientocclusion": False})
    W(f"assets/{NS}/models/item/{name}.json", {"parent": f"{NS}:block/prop/{name}"})
    return len(els)


def blockstate(name, lit_capable):
    v = {}
    for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for lit in ((False, True) if lit_capable else (None,)):
            key = f"facing={f}" + ("" if lit is None else f",lit={str(lit).lower()}")
            m = {"model": f"{NS}:block/prop/{name}" + ("_lit" if lit else "")}
            if r:
                m["y"] = r
            v[key] = m
    W(f"assets/{NS}/blockstates/{name}.json", {"variants": v})


def java(entries):
    lines = []
    for name, (fn, second, lit, shape) in entries:
        sh = "null" if shape is None else "new double[]{" + ", ".join(str(x) for x in shape) + "}"
        lines.append(f'        prop("{name}", {str(lit).lower()}, {sh});')
    src = f"""package com.krolasyon.bloodsea.registry;

import com.krolasyon.bloodsea.block.BonePropBlock;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Generated by tools/gen_props.py: the bones of the drowned, as real shapes. */
public final class ModProps {{
    public static final Map<String, RegistryObject<Block>> ALL = new LinkedHashMap<>();

    static {{
{chr(10).join(lines)}
        prop("heart", false, new double[]{{3, 0, 3, 13, 14, 13}});
    }}

    private static void prop(String name, boolean lit, double[] shape) {{
        RegistryObject<Block> r = ModBlocks.BLOCKS.register(name, () -> new BonePropBlock(lit, shape, name.equals("heart")));
        ALL.put(name, r);
        ModBlocks.WITH_ITEM.add(r);
    }}

    public static Block get(String name) {{ return ALL.get(name).get(); }}

    /** class loading hook: the static block runs before the registers are handed to the event bus */
    public static void init() {{}}

    private ModProps() {{}}
}}
"""
    open(JAVA, "w").write(src)


def main():
    textures()
    total = 0
    for name, (fn, second, lit, shape) in PROPS.items():
        n = build(name, fn, second, lit)
        blockstate(name, lit)
        total += n
        print(f"{name:14s} {n:5d} boxes")
    n = build("heart", p_heart, "vessel", False)
    blockstate("heart", False)
    print(f"{'heart':14s} {n:5d} boxes")
    java(PROPS.items())
    print("props ok, total boxes", total + n)


if __name__ == "__main__":
    main()
