#!/usr/bin/env python3
"""Harrow Farm: every block, prop model, blockstate, item model and texture - and the Java registry that matches them.

Run from mimic/tools. Writes into ../common/src/main/resources/assets/mimicry and
../common/src/main/java/com/krolasyon/mimic/registry/ModBlocks.java
"""
import json, math, os, random, shutil
import numpy as np
from PIL import Image, ImageDraw
import texlib

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..")
A = os.path.join(ROOT, "common/src/main/resources/assets/mimicry")
JAVA = os.path.join(ROOT, "common/src/main/java/com/krolasyon/mimic/registry/ModBlocks.java")
NS = "mimicry"


def W(path, obj):
    p = os.path.join(A, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w") as f:
        json.dump(obj, f, separators=(",", ":"))


# ============================================================================ model building
def box(frm, to, t, faces=None, uv=None, rot=None, shade=True, cull=None):
    e = {"from": [round(v, 3) for v in frm], "to": [round(v, 3) for v in to], "faces": {}}
    for f in ("north", "south", "east", "west", "up", "down"):
        tx = (faces or {}).get(f, t)
        if tx is None:
            continue
        face = {"texture": "#" + tx}
        if uv and f in uv:
            face["uv"] = uv[f]
        else:
            face["uv"] = auto_uv(frm, to, f)
        e["faces"][f] = face
    if rot:
        axis, ang, origin = rot
        e["rotation"] = {"angle": ang, "axis": axis, "origin": list(origin)}
    if not shade:
        e["shade"] = False
    return e


def auto_uv(frm, to, f):
    """uv from the element's position, wrapped into 0..16 so textures stay continuous across parts"""
    x1, y1, z1 = frm
    x2, y2, z2 = to
    if f in ("north", "south"):
        u0, u1, v0, v1 = x1, x2, 16 - y2, 16 - y1
    elif f in ("east", "west"):
        u0, u1, v0, v1 = z1, z2, 16 - y2, 16 - y1
    else:
        u0, u1, v0, v1 = x1, x2, z1, z2

    def wrap(a, b):
        if b - a >= 16:
            return 0, 16
        off = math.floor(a / 16) * 16
        a, b = a - off, b - off
        if b > 16:
            a, b = 16 - (b - a), 16
        return a, b
    u0, u1 = wrap(u0, u1)
    v0, v1 = wrap(v0, v1)
    return [round(u0, 3), round(v0, 3), round(u1, 3), round(v1, 3)]


def model(name, textures, elements, cutout=False, translucent=False, parent="block/block", ao=True):
    m = {"parent": "minecraft:" + parent, "textures": {k: f"{NS}:block/{v}" for k, v in textures.items()}, "elements": elements}
    m["textures"]["particle"] = f"{NS}:block/" + next(iter(textures.values()))
    if cutout:
        m["render_type"] = "minecraft:cutout"
    if translucent:
        m["render_type"] = "minecraft:translucent"
    if not ao:
        m["ambientocclusion"] = False
    m["display"] = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
                    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
                    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
                    "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
                    "fixed": {"scale": [0.5, 0.5, 0.5]}}
    W(f"models/block/{name}.json", m)


def legs(t, x1, z1, x2, z2, h, s=1.5, y0=0):
    return [box((x1, y0, z1), (x1 + s, h, z1 + s), t), box((x2 - s, y0, z1), (x2, h, z1 + s), t),
            box((x1, y0, z2 - s), (x1 + s, h, z2), t), box((x2 - s, y0, z2 - s), (x2, h, z2), t)]


ROT = {"north": 0, "east": 90, "south": 180, "west": 270}

# --------------------------------------------------------------------------- registry spec, turned into Java
SPEC = []      # (name, java expression)
ITEM_MODEL = {}  # block name -> model used for its item


def j_props(kind="prop", light=None, nocol=False, hide=False):
    base = {"wood": "wood()", "stone": "stone()", "metal": "metal()", "prop": "prop()", "glass": "glass()", "soft": "soft()",
            "plant": "plant()", "flesh": "flesh()"}[kind]
    if light:
        base += f".lightLevel(s -> {light})"
    if nocol:
        base += ".noCollission()"
    if hide:
        base += ".isSuffocating((st, l, p) -> false).isViewBlocking((st, l, p) -> false)"
    return base


def jshape(b):
    return "new double[]{" + ", ".join(f"{v:g}" for v in b) + "}"


def facing_state(name, variants):
    """variants: list of (extra-property-string, model)"""
    v = {}
    for extra, mdl in variants:
        for f, r in ROT.items():
            key = f"facing={f}" + ("," + extra if extra else "")
            e = {"model": f"{NS}:block/{mdl}"}
            if r:
                e["y"] = r
            v[key] = e
    W(f"blockstates/{name}.json", {"variants": v})
    ITEM_MODEL[name] = variants[0][1]


def cube(name, tex, kind="wood", side=None, top=None, bottom=None):
    if side:
        W(f"models/block/{name}.json", {"parent": "minecraft:block/cube_bottom_top",
                                        "textures": {"side": f"{NS}:block/{side}", "top": f"{NS}:block/{top}", "bottom": f"{NS}:block/{bottom or top}"}})
    else:
        W(f"models/block/{name}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{tex}"}})
    W(f"blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
    ITEM_MODEL[name] = name
    SPEC.append((name, f"new Block({j_props(kind)})"))


def facing(name, tex, els, shape, collide=True, kind="prop", cutout=False, light=None, nocol=False, hide=False, translucent=False):
    model(name, tex, els, cutout=cutout, translucent=translucent)
    facing_state(name, [("", name)])
    SPEC.append((name, f"new FacingBlock({j_props(kind, light, nocol, hide)}, {jshape(shape)}, {str(collide).lower()})"))


def openable(name, tex, closed, opened, shape, collide=True, kind="prop"):
    model(name, tex, closed)
    model(name + "_open", tex, opened)
    facing_state(name, [("open=false", name), ("open=true", name + "_open")])
    SPEC.append((name, f"new OpenableBlock({j_props(kind)}, {jshape(shape)}, {str(collide).lower()})"))


def moded(name, tex_by_mode, els_by_mode, shape, light, collide=False, kind="prop", cutout=False):
    """els_by_mode: function(mode) -> elements ; tex_by_mode: function(mode) -> textures"""
    for m in range(3):
        model(f"{name}_{m}", tex_by_mode(m), els_by_mode(m), cutout=cutout)
    facing_state(name, [(f"mode={m}", f"{name}_{m}") for m in range(3)])
    SPEC.append((name, f"new ModeBlock({j_props(kind, light, nocol=not collide)}, {jshape(shape)}, {str(collide).lower()})"))


def tall(name, tex, lower, upper, shape, collide=True, kind="prop", hide=False, cutout=False, nocol=False):
    model(name + "_lower", tex, lower, cutout=cutout)
    model(name + "_upper", tex, upper, cutout=cutout)
    facing_state(name, [("half=lower", name + "_lower"), ("half=upper", name + "_upper")])
    ITEM_MODEL[name] = name + "_lower"
    SPEC.append((name, f"new TallBlock({j_props(kind, nocol=nocol, hide=hide)}, {jshape(shape)}, {str(collide).lower()})"))


def variant(name, tex_fn, els_fn, n, shape, collide=True, kind="prop", cutout=False, nocol=False):
    for i in range(n):
        model(f"{name}_{i}", tex_fn(i), els_fn(i), cutout=cutout)
    v = {}
    for f, r in ROT.items():
        for i in range(6):
            e = {"model": f"{NS}:block/{name}_{min(i, n - 1)}"}
            if r:
                e["y"] = r
            v[f"facing={f},variant={i}"] = e
    W(f"blockstates/{name}.json", {"variants": v})
    ITEM_MODEL[name] = f"{name}_0"
    SPEC.append((name, f"new VariantBlock({j_props(kind, nocol=nocol)}, {jshape(shape)}, {str(collide).lower()})"))


# ============================================================================ the props
def gen():
    T = texlib.make_all(None)
    tdir = os.path.join(A, "textures/block")
    if os.path.isdir(tdir):
        shutil.rmtree(tdir)
    os.makedirs(tdir)
    for n, im in T.items():
        im.save(os.path.join(tdir, n + ".png"))
    with open(os.path.join(tdir, "m_flame.png.mcmeta"), "w") as f:
        json.dump({"animation": {"frametime": 3, "interpolate": True}}, f)
    for d in ("models/block", "blockstates", "models/item"):
        p = os.path.join(A, d)
        if os.path.isdir(p):
            shutil.rmtree(p)

    # ------------------------------------------------------------------ shells (full blocks)
    for n, k in (("barn_wall", "wood"), ("barn_wall_dark", "wood"), ("barn_floor", "wood"), ("siding", "wood"), ("siding_dark", "wood"),
                 ("floorboards", "wood"), ("floorboards_dark", "wood"), ("wainscot", "wood"), ("wallpaper", "wood"), ("wallpaper_peeled", "wood"),
                 ("wallpaper_bloody", "wood"), ("plaster", "stone"), ("plaster_bloody", "stone"), ("kitchen_tile", "stone"), ("butcher_tile", "stone"),
                 ("stone_foundation", "stone"), ("cellar_stone", "stone"), ("packed_dirt", "soft"), ("mud", "soft"), ("slaughter_floor", "stone"),
                 ("straw_floor", "soft"), ("shingles", "wood"), ("barn_roof", "metal"), ("silo_wall", "metal"), ("brick", "stone"),
                 ("boarded_window", "wood"), ("beam_block", "wood")):
        cube(n, "m_wood_beam" if n == "beam_block" else n, k)
    cube("hay_bale", None, "soft", side="hay_bale_side", top="hay_bale_top")

    # ------------------------------------------------------------------ roofs and stairs (vanilla stair geometry, our textures)
    for n, t, k in (("shingle_stairs", "shingles", "wood"), ("tin_stairs", "barn_roof", "metal"), ("plank_stairs", "floorboards_dark", "wood"),
                    ("siding_stairs", "siding", "wood")):
        stairs_block(n, t, k)
    for n, t, k in (("shingle_slab", "shingles", "wood"), ("tin_slab", "barn_roof", "metal"), ("plank_slab", "floorboards_dark", "wood"),
                    ("barn_slab", "barn_floor", "wood")):
        slab_block(n, t, k)
    staircase()

    # ------------------------------------------------------------------ doors, hatches, ladders, fences
    door_block("farm_door", "m_door", "m_door_top")
    door_block("barn_door", "m_barn_door", "m_barn_door")
    trapdoor_block("hatch", "m_trapdoor")
    ladder_block()
    barbed_fence()
    farm_gate()

    # ------------------------------------------------------------------ timber
    facing("post", {"w": "m_wood_beam"}, [box((5, 0, 5), (11, 16, 11), "w")], [5, 0, 5, 11, 16, 11], kind="wood")
    facing("beam", {"w": "m_wood_beam"}, [box((5, 10, 0), (11, 16, 16), "w")], [5, 10, 0, 11, 16, 16], kind="wood")
    facing("brace", {"w": "m_wood_beam"}, [box((6, 7, -3), (10, 11, 19), "w", rot=("x", 45, (8, 9, 8)))], [6, 0, 0, 10, 16, 16], collide=False, kind="wood")
    facing("rafter", {"w": "m_wood_beam"}, [box((6, -3, 6), (10, 19, 10), "w", rot=("z", -45, (8, 8, 8))),
                                           box((6, 13, 0), (10, 16, 16), "w")], [6, 13, 0, 10, 16, 16], collide=False, kind="wood")

    # a banister along one edge of a cell (south edge for facing north): turned posts and a hand rail
    ban = [box((0, 14, 13.5), (16, 16, 16), "w")]
    for x in (1, 5, 9, 13):
        ban.append(box((x, 0, 14.2), (x + 1.6, 14, 15.8), "w"))
    ban.append(box((0, 0, 13.8), (2, 16, 16), "w"))
    facing("banister", {"w": "m_wood_dark"}, ban, [0, 0, 14, 16, 16, 16], kind="prop")

    # ------------------------------------------------------------------ furniture (farmhouse)
    chair = legs("w", 3, 3, 13, 13, 7.5, 1.4) + [
        box((2.5, 7.5, 2.5), (13.5, 9, 13.5), "w"), box((3, 9, 12), (4.4, 22, 13.4), "w"), box((11.6, 9, 12), (13, 22, 13.4), "w"),
        box((4.4, 19.5, 12.2), (11.6, 21.5, 13.2), "w"), box((4.4, 15.5, 12.4), (11.6, 16.8, 13), "w"), box((4.4, 12, 12.4), (11.6, 13.2, 13), "w"),
        box((3.4, 3, 3.4), (12.6, 3.8, 4.2), "w"), box((3.4, 3, 11.8), (12.6, 3.8, 12.6), "w")]
    facing("chair", {"w": "m_wood_old"}, chair, [3, 0, 3, 13, 9, 13])
    chair_fallen = [box(e["from"], e["to"], "w") for e in chair]
    facing("chair_fallen", {"w": "m_wood_old"}, [dict(e, rotation={"angle": -45, "axis": "x", "origin": [8, 0, 13]}) for e in chair_fallen],
           [2, 0, 2, 14, 6, 14], collide=False)
    facing("bench", {"w": "m_wood_old"}, [box((0, 7, 3), (16, 9, 13), "w"), box((1, 0, 4), (3, 7, 12), "w"), box((13, 0, 4), (15, 7, 12), "w"),
                                          box((0, 9, 12), (16, 10.5, 13.5), "w", rot=("x", -22.5, (8, 9, 13))),
                                          box((3, 3, 7), (13, 4.5, 9), "w")], [0, 0, 3, 16, 9, 13])
    facing("table", {"w": "m_wood_old", "c": "m_wood_dark"}, legs("c", 1.5, 1.5, 14.5, 14.5, 14, 1.8) + [
        box((0, 14, 0), (16, 16, 16), "w"), box((2, 11.5, 2), (14, 13, 3), "c"), box((2, 11.5, 13), (14, 13, 14), "c")], [0, 0, 0, 16, 16, 16])
    facing("table_cloth", {"w": "m_wood_old", "c": "m_fabric_dirty", "b": "m_blood"}, legs("w", 1.5, 1.5, 14.5, 14.5, 14, 1.8) + [
        box((0, 14, 0), (16, 16, 16), "c"), box((-0.5, 9, -0.5), (16.5, 16, 0), "c"), box((-0.5, 9, 16), (16.5, 16, 16.5), "c"),
        box((3, 16, 4), (9, 16.2, 10), "b")], [0, 0, 0, 16, 16, 16])
    facing("stool", {"w": "m_wood_old"}, legs("w", 4, 4, 12, 12, 9, 1.4) + [box((3.5, 9, 3.5), (12.5, 10.5, 12.5), "w")], [4, 0, 4, 12, 10, 12])
    facing("armchair", {"f": "m_fabric_floral", "w": "m_wood_dark"}, legs("w", 1, 2, 15, 15, 3) + [
        box((1, 3, 2), (15, 8, 15), "f"), box((1, 8, 11.5), (15, 20, 15), "f"), box((0, 8, 2), (3, 13, 11.5), "f"),
        box((13, 8, 2), (16, 13, 11.5), "f"), box((3, 8, 2.5), (13, 9.5, 11.5), "f")], [1, 0, 2, 15, 12, 15])
    facing("bookcase", {"w": "m_wood_dark", "b": "m_book"}, [box((0, 0, 4), (16, 16, 16), "w", faces={"north": "b"}),
                                                             box((0, 0, 3), (1, 16, 4), "w"), box((15, 0, 3), (16, 16, 4), "w"), box((0, 15, 3), (16, 16, 4), "w")],
           [0, 0, 3, 16, 16, 16])
    facing("shelf_jars", {"w": "m_wood_old", "j": "m_jars"}, [box((0, 0, 10), (16, 16, 16), "w", faces={"north": "j"}),
                                                              box((0, 0, 9), (16, 1, 10), "w"), box((0, 7.5, 9), (16, 8.5, 10), "w")], [0, 0, 9, 16, 16, 16])
    facing("counter", {"w": "m_wood_old", "t": "m_wood_dark", "d": "m_drawer_front"}, [box((0, 0, 2), (16, 14, 16), "w", faces={"north": "d"}),
                                                                                      box((0, 14, 0), (16, 16, 16), "t")], [0, 0, 0, 16, 16, 16])
    facing("sink", {"w": "m_wood_old", "p": "m_porcelain_bloody", "m": "m_tin"}, [
        box((0, 0, 2), (16, 13, 16), "w"), box((0, 13, 0), (16, 16, 3), "p"), box((0, 13, 13), (16, 16, 16), "p"),
        box((0, 13, 3), (2, 16, 13), "p"), box((14, 13, 3), (16, 16, 13), "p"), box((2, 13, 3), (14, 13.5, 13), "p"),
        box((7, 16, 13), (9, 21, 14.5), "m"), box((7, 20, 9), (9, 21.5, 14.5), "m")], [0, 0, 0, 16, 16, 16])
    facing("cook_stove", {"i": "m_cast_iron", "d": "m_stove_door", "m": "m_tin"}, legs("i", 1, 2, 15, 15, 3, 2) + [
        box((0, 3, 1), (16, 14, 16), "i", faces={"north": "d"}), box((-1, 14, 0), (17, 15.5, 16), "i"),
        box((4, 15.5, 4), (8, 16, 8), "i"), box((9, 15.5, 4), (13, 16, 8), "i"), box((2, 15.5, 13), (14, 22, 16), "i"),
        box((6, 22, 13.5), (10, 32, 16), "m")], [0, 0, 1, 16, 16, 16])
    moded("wood_stove", lambda m: {"i": "m_cast_iron", "d": "m_stove_door_lit" if m else "m_stove_door", "p": "m_rust"},
          lambda m: legs("i", 2, 2, 14, 14, 3, 2) + [box((1, 3, 1), (15, 15, 15), "i", faces={"north": "d"}),
                                                     box((2, 15, 2), (14, 16, 14), "i"), box((5.5, 16, 5.5), (10.5, 32, 10.5), "p")],
          [1, 0, 1, 15, 16, 15], "s.getValue(ModeBlock.MODE) == 0 ? 0 : 9", collide=True)
    facing("stove_pipe", {"p": "m_rust"}, [box((5.5, 0, 5.5), (10.5, 16, 10.5), "p")], [5.5, 0, 5.5, 10.5, 16, 10.5])
    model("bed_head", {"w": "m_wood_dark", "s": "m_fabric_dirty", "p": "m_canvas", "b": "m_blood"}, [
        box((0, 3, 0), (16, 8, 16), "s"), box((0, 0, 14.5), (16, 22, 16), "w"), box((0, 0, 14.5), (1.5, 24, 16), "w"), box((14.5, 0, 14.5), (16, 24, 16), "w"),
        box((2, 8, 10), (14, 10.5, 14), "p"), box((1, 0, 1), (2.5, 3, 2.5), "w"), box((13.5, 0, 1), (15, 3, 2.5), "w"), box((4, 8, 2), (11, 8.2, 8), "b")])
    facing_state("bed_head", [("", "bed_head")])
    SPEC.append(("bed_head", f"new FacingBlock({j_props()}, {jshape([0, 0, 0, 16, 9, 16])}, true)"))
    facing("bed_foot", {"w": "m_wood_dark", "s": "m_fabric_dirty"}, [box((0, 3, 0), (16, 8, 16), "s"), box((0, 0, 0), (16, 12, 1.5), "w"),
                                                                    box((0, 0, 14), (1.5, 3, 15.5), "w"), box((14.5, 0, 14), (16, 3, 15.5), "w"),
                                                                    box((-0.2, 1, 1.5), (16.2, 7, 14), "s")], [0, 0, 0, 16, 9, 16])
    openable("dresser", {"w": "m_wood_dark", "d": "m_drawer_front", "i": "m_black"},
             [box((0, 0, 3), (16, 16, 16), "w", faces={"north": "d"}), box((-0.5, 16, 2.5), (16.5, 17, 16), "w")],
             [box((0, 0, 3), (16, 16, 16), "w", faces={"north": "d"}), box((-0.5, 16, 2.5), (16.5, 17, 16), "w"),
              box((1.5, 9, -4), (14.5, 14, 5), "w", faces={"up": "i", "north": "d"})], [0, 0, 3, 16, 16, 16])
    openable("nightstand", {"w": "m_wood_dark", "d": "m_drawer_front", "i": "m_black"},
             legs("w", 3, 4, 13, 14, 3, 1.4) + [box((3, 3, 4), (13, 12, 14), "w", faces={"north": "d"}), box((2.5, 12, 3.5), (13.5, 13, 14.5), "w")],
             legs("w", 3, 4, 13, 14, 3, 1.4) + [box((3, 3, 4), (13, 12, 14), "w"), box((2.5, 12, 3.5), (13.5, 13, 14.5), "w"),
                                                box((3.5, 7, -2), (12.5, 11, 6), "w", faces={"up": "i"})], [3, 0, 4, 13, 13, 14])
    openable("cabinet", {"w": "m_wood_old", "d": "m_drawer_front", "i": "m_black"},
             [box((1, 0, 3), (15, 15, 16), "w", faces={"north": "d"}), box((0, 15, 2), (16, 16, 16), "w")],
             [box((1, 0, 3), (15, 15, 16), "w", faces={"north": "i"}), box((0, 15, 2), (16, 16, 16), "w"),
              box((1, 1, -6), (2, 14, 3), "w", faces={"east": "d", "west": "d"})], [1, 0, 2, 16, 16, 16])
    openable("desk", {"w": "m_wood_dark", "d": "m_drawer_front", "p": "m_paper"},
             [box((0, 14, 0), (16, 16, 16), "w"), box((0, 0, 1), (5, 14, 16), "w", faces={"north": "d"}), box((11, 0, 1), (16, 14, 16), "w", faces={"north": "d"}),
              box((5, 5, 14), (11, 14, 16), "w"), box((5, 10, 1), (11, 14, 13), "w", faces={"north": "d"})],
             [box((0, 14, 0), (16, 16, 16), "w"), box((0, 0, 1), (5, 14, 16), "w", faces={"north": "d"}), box((11, 0, 1), (16, 14, 16), "w", faces={"north": "d"}),
              box((5, 5, 14), (11, 14, 16), "w"), box((5, 10, -5), (11, 14, 7), "w", faces={"north": "d", "up": "p"})], [0, 0, 0, 16, 16, 16])
    openable("trunk", {"w": "m_wood_dark", "m": "m_rust", "r": "m_brass", "i": "m_black"},
             [box((1, 0, 2), (15, 8, 14), "w"), box((1, 8, 2), (15, 11, 14), "w"), box((0.8, 0, 4), (15.2, 11.2, 5), "m"), box((0.8, 0, 11), (15.2, 11.2, 12), "m"),
              box((7, 7, 1.6), (9, 9.5, 2), "r")],
             [box((1, 0, 2), (15, 8, 14), "w", faces={"up": "i"}), box((1, 8, 13), (15, 20, 16), "w"), box((0.8, 0, 4), (15.2, 8.2, 5), "m"),
              box((0.8, 0, 11), (15.2, 8.2, 12), "m")], [1, 0, 2, 15, 11, 14])
    openable("crate", {"c": "m_crate", "i": "m_black", "s": "m_straw_bits"},
             [box((1, 0, 1), (15, 13, 15), "c")],
             [box((1, 0, 1), (15, 12, 15), "c", faces={"up": "i"}), box((1, 0, 15), (15, 1.5, 29), "c", rot=("x", 22.5, (8, 0, 15)))], [1, 0, 1, 15, 13, 15])
    openable("barrel", {"b": "m_barrel", "t": "m_wood_old", "i": "m_black"},
             [box((2, 0, 2), (14, 15, 14), "b", faces={"up": "t", "down": "t"}), box((1.5, 2, 1.5), (14.5, 4, 14.5), "b", faces={"up": None, "down": None}),
              box((1.5, 11, 1.5), (14.5, 13, 14.5), "b", faces={"up": None, "down": None})],
             [box((2, 0, 2), (14, 15, 14), "b", faces={"up": "i", "down": "t"}), box((1.5, 2, 1.5), (14.5, 4, 14.5), "b", faces={"up": None, "down": None}),
              box((14, 0, 2), (26, 1.5, 14), "t")], [2, 0, 2, 14, 15, 14])
    openable("toolbox", {"r": "m_red_paint", "m": "m_iron", "i": "m_black"},
             [box((3, 0, 5), (13, 5, 11), "r"), box((6, 5, 7.5), (10, 6.5, 8.5), "m")],
             [box((3, 0, 5), (13, 5, 11), "r", faces={"up": "i"}), box((3, 5, 11), (13, 6, 17), "r")], [3, 0, 5, 13, 6, 11], collide=False)
    tall("wardrobe", {"w": "m_wood_dark", "d": "m_wood_old", "s": "m_black"},
         [box((0, 0, 2), (16, 16, 16), "w", faces={"north": "d"}), box((7.8, 1, 1.6), (8.2, 16, 2), "s"), box((10, 8, 1.2), (11, 11, 2), "w")],
         [box((0, 0, 2), (16, 14, 16), "w", faces={"north": "d"}), box((-0.5, 14, 1.5), (16.5, 16, 16.5), "w"), box((7.8, 0, 1.6), (8.2, 13, 2), "s"),
          box((2, 10, 1.6), (6, 12, 2), "s"), box((10, 10, 1.6), (14, 12, 2), "s")], [0, 0, 2, 16, 16, 16], hide=True)
    tall("grandfather_clock", {"w": "m_wood_dark", "f": "m_clock_face", "g": "m_glass_dirty", "r": "m_brass"},
         [box((3, 0, 6), (13, 16, 15), "w"), box((5, 3, 5.6), (11, 15, 6), "g"), box((7.5, 4, 6.5), (8.5, 12, 7.5), "r"), box((7, 3.5, 6.5), (9, 5.5, 7.5), "r")],
         [box((3, 0, 6), (13, 12, 15), "w", faces={"north": "f"}), box((2.5, 12, 5.5), (13.5, 15, 15.5), "w"), box((5, 15, 6), (11, 16.5, 14), "w")],
         [3, 0, 6, 13, 16, 15])
    variant("painting", lambda i: {"w": "m_wood_dark", "p": f"m_painting_{i}"},
            lambda i: [box((1, 2, 15), (15, 15, 16), "w", faces={"north": "p"})], 4, [1, 2, 15, 15, 15, 16], collide=False, nocol=True)
    facing("mirror", {"w": "m_wood_dark", "g": "m_glass_dirty"}, [box((3, 0, 15), (13, 16, 16), "w", faces={"north": "g"}),
                                                                 box((2.5, -0.5, 14.6), (13.5, 0.5, 16), "w"), box((2.5, 15.5, 14.6), (13.5, 16.5, 16), "w")],
           [3, 0, 15, 13, 16, 16], collide=False, nocol=True)
    facing("rug", {"r": "m_cloth_red"}, [box((0, 0, 0), (16, 0.4, 16), "r")], [0, 0, 0, 16, 0.4, 16], collide=False, nocol=True)
    facing("runner", {"r": "m_rug_runner"}, [box((3, 0, 0), (13, 0.4, 16), "r")], [3, 0, 0, 13, 0.4, 16], collide=False, nocol=True)
    facing("curtain", {"c": "m_curtain"}, [box((0, 0, 14.5), (16, 16, 15), "c", faces={"up": None, "down": None})], [0, 0, 14.5, 16, 16, 15],
           collide=False, nocol=True, cutout=True)
    facing("window_pane", {"w": "m_window", "f": "m_wood_dark"}, [box((0, 0, 7.5), (16, 16, 8.5), "w")], [0, 0, 7.5, 16, 16, 8.5], cutout=True, kind="glass")
    facing("window_lit", {"w": "m_window_lit"}, [box((0, 0, 7.5), (16, 16, 8.5), "w", shade=False)], [0, 0, 7.5, 16, 16, 8.5], kind="glass",
           light="12")
    facing("cobweb", {"c": "m_cobweb"}, [box((0, 0, 15.9), (16, 16, 16), "c", faces={"south": None, "up": None, "down": None, "east": None, "west": None}),
                                         box((0, 15.9, 0), (16, 16, 16), "c", faces={"north": None, "south": None, "up": None, "east": None, "west": None})],
           [0, 15, 15, 16, 16, 16], collide=False, nocol=True, cutout=True)
    facing("handprint", {"h": "m_handprint"}, [box((0, 0, 15.9), (16, 16, 16), "h", faces={"south": None, "up": None, "down": None, "east": None, "west": None})],
           [0, 0, 15.9, 16, 16, 16], collide=False, nocol=True, cutout=True)
    facing("blood_pool", {"b": "m_blood_pool"}, [box((0, 0.05, 0), (16, 0.15, 16), "b", faces={"north": None, "south": None, "east": None, "west": None, "down": None})],
           [0, 0, 0, 16, 0.2, 16], collide=False, nocol=True, cutout=True)
    facing("bathtub", {"p": "m_porcelain_bloody", "m": "m_iron", "b": "m_blood"}, [
        box((0, 2, 1), (16, 4, 15), "p"), box((0, 4, 1), (16, 10, 2.5), "p"), box((0, 4, 13.5), (16, 10, 15), "p"),
        box((0, 4, 2.5), (1.5, 10, 13.5), "p"), box((14.5, 4, 2.5), (16, 10, 13.5), "p"), box((1.5, 4, 2.5), (14.5, 7, 13.5), "b"),
        box((1, 0, 2), (3, 2, 4), "m"), box((1, 0, 12), (3, 2, 14), "m"), box((13, 0, 2), (15, 2, 4), "m"), box((13, 0, 12), (15, 2, 14), "m")],
           [0, 0, 1, 16, 10, 15])
    facing("toilet", {"p": "m_porcelain", "w": "m_wood_old"}, [box((4, 0, 4), (12, 7, 12), "p"), box((3.5, 7, 3), (12.5, 8, 12.5), "w"),
                                                               box((3, 7, 12.5), (13, 17, 16), "p")], [3, 0, 3, 13, 16, 16])

    # ------------------------------------------------------------------ lights (mode 0 off, 1 lit, 2 dying)
    lit = lambda m: "m_lamp_glass_on" if m == 1 else "m_lamp_glass_off" if m == 0 else "m_lamp_glass_on"
    moded("lantern_hang", lambda m: {"m": "m_rust", "g": lit(m), "f": "m_flame"},
          lambda m: [box((5, 2, 5), (11, 3, 11), "m"), box((5.5, 3, 5.5), (10.5, 9, 10.5), "g", shade=m == 0), box((5, 9, 5), (11, 10, 11), "m"),
                     box((7, 10, 7), (9, 16, 9), "m", faces={"up": None, "down": None})],
          [5, 2, 5, 11, 16, 11], "s.getValue(ModeBlock.MODE) == 1 ? 13 : s.getValue(ModeBlock.MODE) == 2 ? 8 : 0")
    moded("lantern", lambda m: {"m": "m_rust", "g": lit(m)},
          lambda m: [box((5, 0, 5), (11, 1, 11), "m"), box((5.5, 1, 5.5), (10.5, 7, 10.5), "g", shade=m == 0), box((5, 7, 5), (11, 8, 11), "m"),
                     box((7.5, 8, 7.5), (8.5, 10, 8.5), "m")],
          [5, 0, 5, 11, 10, 11], "s.getValue(ModeBlock.MODE) == 1 ? 12 : s.getValue(ModeBlock.MODE) == 2 ? 7 : 0")
    moded("oil_lamp", lambda m: {"b": "m_brass", "g": lit(m)},
          lambda m: [box((5.5, 0, 5.5), (10.5, 1, 10.5), "b"), box((6, 1, 6), (10, 4, 10), "b"), box((6.5, 4, 6.5), (9.5, 10, 9.5), "g", shade=m == 0)],
          [5.5, 0, 5.5, 10.5, 10, 10.5], "s.getValue(ModeBlock.MODE) == 1 ? 11 : s.getValue(ModeBlock.MODE) == 2 ? 6 : 0")

    def chandelier(m):
        e = [box((7.5, 10, 7.5), (8.5, 16, 8.5), "b"), box((6, 6, 6), (10, 10, 10), "b"), box((7, 4, 7), (9, 6, 9), "b")]
        for k in range(6):
            ang = k / 6 * math.tau
            x, z = 8 + math.cos(ang) * 6.5, 8 + math.sin(ang) * 6.5
            e.append(box((min(x, 8) - 0.4, 7, min(z, 8) - 0.4), (max(x, 8) + 0.4, 7.8, max(z, 8) + 0.4), "b"))
            e.append(box((x - 0.8, 7.8, z - 0.8), (x + 0.8, 8.4, z + 0.8), "b"))
            e.append(box((x - 0.5, 8.4, z - 0.5), (x + 0.5, 11, z + 0.5), "w"))
            if m:
                e.append(box((x - 0.5, 11, z - 0.5), (x + 0.5, 12.5, z + 0.5), "f", shade=False, faces={"up": None, "down": None}))
        return e
    moded("chandelier", lambda m: {"b": "m_brass", "w": "m_wax", "f": "m_flame"}, chandelier, [1, 4, 1, 15, 16, 15],
          "s.getValue(ModeBlock.MODE) == 1 ? 14 : s.getValue(ModeBlock.MODE) == 2 ? 9 : 0", cutout=True)
    moded("sconce", lambda m: {"b": "m_brass", "w": "m_wax", "f": "m_flame"},
          lambda m: [box((6, 4, 15), (10, 9, 16), "b"), box((7.5, 6, 11), (8.5, 7, 15), "b"), box((6.5, 7, 10), (9.5, 7.6, 13), "b"),
                     box((7.3, 7.6, 11.1), (8.7, 11, 11.9 + 0.5), "w")] + ([box((7.3, 11, 11.1), (8.7, 12.8, 12.4), "f", shade=False, faces={"up": None, "down": None})] if m else []),
          [6, 4, 10, 10, 13, 16], "s.getValue(ModeBlock.MODE) == 1 ? 12 : s.getValue(ModeBlock.MODE) == 2 ? 7 : 0", cutout=True)
    moded("bulb", lambda m: {"m": "m_iron", "g": lit(m)},
          lambda m: [box((7.5, 10, 7.5), (8.5, 16, 8.5), "m"), box((7, 9, 7), (9, 10, 9), "m"), box((6.8, 6.5, 6.8), (9.2, 9, 9.2), "g", shade=m == 0)],
          [6.8, 6.5, 6.8, 9.2, 16, 9.2], "s.getValue(ModeBlock.MODE) == 1 ? 14 : s.getValue(ModeBlock.MODE) == 2 ? 8 : 0")

    # ------------------------------------------------------------------ machines and story props
    moded("radio_set", lambda m: {"w": "m_wood_dark", "f": "m_radio_face", "m": "m_iron", "g": lit(m)},
          lambda m: [box((2, 0, 5), (14, 9, 12), "w", faces={"north": "f"}), box((11, 9, 9), (11.6, 16, 9.6), "m"),
                     box((3, 7, 4.6), (5, 8.5, 5), "g", shade=m == 0)], [2, 0, 5, 14, 9, 12], "s.getValue(ModeBlock.MODE) == 0 ? 0 : 3", collide=True)
    moded("phone", lambda m: {"b": "m_black", "m": "m_brass"},
          lambda m: [box((4, 3, 14), (12, 13, 16), "b"), box((5, 10, 12.8), (11, 11.5, 14), "b"), box((5, 6, 12), (6.5, 10, 13.6), "b"),
                     box((9.5, 6, 12), (11, 10, 13.6), "b"), box((6, 4, 13.4), (10, 6, 14), "m")] if m != 2 else
          [box((4, 3, 14), (12, 13, 16), "b"), box((6, 4, 13.4), (10, 6, 14), "m"), box((6.5, -8, 12), (8, 3, 13.4), "b")],
          [4, 3, 12, 12, 13, 16], "0")
    moded("cassette_player", lambda m: {"b": "m_black", "t": "m_tape_face", "m": "m_tin"},
          lambda m: [box((3, 0, 5), (13, 4, 11), "b", faces={"up": "t"}), box((4, 4, 6), (6, 4.6, 7), "m"), box((10, 4, 6), (12, 4.6, 7), "m")],
          [3, 0, 5, 13, 5, 11], "0")
    moded("generator", lambda m: {"r": "m_red_paint", "i": "m_iron", "m": "m_rust"},
          lambda m: [box((1, 0, 3), (15, 2, 13), "i"), box((2, 2, 4), (14, 11, 12), "r"), box((4, 11, 6), (8, 13, 10), "m"),
                     box((14, 4, 6), (16, 9, 10), "i"), box((1, 5, 7), (2, 8, 9), "i")], [1, 0, 3, 15, 13, 13], "0", collide=True)
    moded("fuse_box", lambda m: {"g": "m_green_paint", "i": "m_iron", "l": lit(m)},
          lambda m: [box((3, 2, 13), (13, 14, 16), "g"), box((5, 4, 12.6), (11, 12, 13), "i"), box((11.5, 12, 12.6), (12.5, 13, 13), "l", shade=m == 0)],
          [3, 2, 12, 13, 14, 16], "0")
    facing("workbench", {"w": "m_wood_old", "t": "m_wood_dark", "m": "m_iron"}, legs("w", 0.5, 1, 15.5, 15, 13, 2) + [
        box((0, 13, 0), (16, 16, 16), "t"), box((0.5, 3, 1), (15.5, 4, 15), "w"), box((12, 16, 1), (15, 19, 5), "m")], [0, 0, 0, 16, 16, 16])
    facing("tool_rack", {"w": "m_wood_old", "m": "m_iron", "r": "m_rust"}, [
        box((0, 10, 15), (16, 12, 16), "w"), box((2, 0, 14.5), (3, 16, 15.2), "w"), box((1.4, 15, 14), (3.6, 17, 15.4), "m"),
        box((7, -2, 14.5), (8, 16, 15.2), "w"), box((6, -4, 14), (9.5, -1, 15), "r"), box((12, 1, 14.5), (13, 16, 15.2), "w"),
        box((11, 13, 14), (14, 17, 15), "r")], [0, 0, 14, 16, 16, 16], collide=False, nocol=True)
    facing("pitchfork", {"w": "m_wood_old", "m": "m_rust"}, [box((7.5, 0, 7.5), (8.5, 22, 8.5), "w", rot=("x", -22.5, (8, 0, 8))),
                                                            box((5.5, 21, 7.2), (10.5, 22, 8.8), "m", rot=("x", -22.5, (8, 0, 8))),
                                                            box((5.5, 22, 7.6), (6.3, 28, 8.4), "m", rot=("x", -22.5, (8, 0, 8))),
                                                            box((7.6, 22, 7.6), (8.4, 28, 8.4), "m", rot=("x", -22.5, (8, 0, 8))),
                                                            box((9.7, 22, 7.6), (10.5, 28, 8.4), "m", rot=("x", -22.5, (8, 0, 8)))],
           [5, 0, 5, 11, 16, 16], collide=False, nocol=True)
    facing("shovel_prop", {"w": "m_wood_old", "m": "m_rust"}, [box((7.5, 6, 7.5), (8.5, 24, 8.5), "w", rot=("x", -22.5, (8, 0, 8))),
                                                              box((5.5, 0, 7.3), (10.5, 7, 8.7), "m", rot=("x", -22.5, (8, 0, 8))),
                                                              box((6.5, 23, 7.3), (9.5, 24, 8.7), "w", rot=("x", -22.5, (8, 0, 8)))],
           [5, 0, 5, 11, 16, 16], collide=False, nocol=True)
    facing("bucket", {"m": "m_tin", "b": "m_blood"}, [box((4.5, 0, 4.5), (11.5, 7, 11.5), "m", faces={"up": "b"}), box((4, 6, 7.6), (12, 9, 8.4), "m", faces={"up": None, "down": None})],
           [4.5, 0, 4.5, 11.5, 7, 11.5])
    facing("milk_can", {"m": "m_tin"}, [box((4, 0, 4), (12, 10, 12), "m"), box((5.5, 10, 5.5), (10.5, 13, 10.5), "m"), box((5, 13, 5), (11, 14, 11), "m")],
           [4, 0, 4, 12, 14, 12])
    facing("gas_can", {"r": "m_gas_can", "m": "m_iron"}, [box((4, 0, 3), (12, 10, 13), "r"), box((6, 10, 7), (10, 11.5, 9), "r"), box((10, 9, 10), (11.5, 12, 11.5), "m")],
           [4, 0, 3, 12, 12, 13], collide=False)
    facing("sacks", {"s": "m_canvas", "r": "m_rope"}, [box((1, 0, 2), (9, 6, 14), "s"), box((8, 0, 3), (15, 5, 13), "s"), box((3, 6, 4), (12, 10, 12), "s"),
                                                       box((6, 10, 6), (9, 11, 9), "r")], [1, 0, 2, 15, 10, 14])
    facing("hay_pile", {"h": "hay", "s": "straw_floor"}, [box((0, 0, 0), (16, 6, 16), "h"), box((2, 6, 1), (14, 10, 15), "h"), box((4, 10, 3), (12, 12, 13), "s")],
           [0, 0, 0, 16, 6, 16], collide=False, hide=True)
    tall("hay_mound", {"h": "hay", "s": "straw_floor"},
         [box((-1, 0, -1), (17, 16, 17), "h"), box((-2, 0, 2), (0, 9, 14), "s"), box((16, 0, 2), (18, 9, 14), "s")],
         [box((0, 0, 0), (16, 8, 16), "h"), box((2, 8, 2), (14, 12, 14), "h"), box((5, 12, 5), (11, 13.5, 11), "s")], [0, 0, 0, 16, 16, 16],
         collide=False, hide=True)
    facing("straw_layer", {"s": "m_straw_bits"}, [box((0, 0, 0), (16, 0.6, 16), "s")], [0, 0, 0, 16, 0.6, 16], collide=False, nocol=True, cutout=True)
    facing("saddle", {"l": "m_saddle", "w": "m_wood_old", "m": "m_iron"}, [
        box((3, 8, 13), (13, 9.5, 16), "w"), box((3.5, 4, 8), (12.5, 10, 14), "l"), box((4.5, 10, 9), (11.5, 11.5, 13.5), "l"),
        box((3, 0, 9), (4, 6, 13), "l"), box((12, 0, 9), (13, 6, 13), "l"), box((2.5, -2, 10), (4.5, 0, 12), "m"), box((11.5, -2, 10), (13.5, 0, 12), "m")],
           [3, 0, 8, 13, 12, 16], collide=False, nocol=True)
    facing("bridle", {"l": "m_leather", "m": "m_iron"}, [box((7.5, 12, 14), (8.5, 14, 16), "m"), box((5, 2, 15), (6, 13, 15.6), "l"),
                                                         box((10, 2, 15), (11, 13, 15.6), "l"), box((5, 6, 15), (11, 7, 15.6), "l"),
                                                         box((6, 0, 14.8), (10, 2, 15.6), "m")], [5, 0, 14, 11, 14, 16], collide=False, nocol=True)
    facing("trough", {"w": "m_wood_old", "b": "m_blood"}, [box((0, 0, 3), (16, 6, 13), "w", faces={"up": "b"}), box((0, 6, 3), (16, 7, 4.5), "w"),
                                                           box((0, 6, 11.5), (16, 7, 13), "w")], [0, 0, 3, 16, 7, 13])
    facing("wagon_wheel", {"w": "m_wood_old", "m": "m_rust"}, wheel(), [0, 0, 13, 16, 16, 16], collide=False, nocol=True)

    # ------------------------------------------------------------------ the slaughter
    tall("meat_hook", {"m": "m_rust"},
         [box((7.6, 6, 7.6), (8.4, 16, 8.4), "m"), box((7.6, 4, 7.6), (8.4, 6, 10), "m", rot=("x", -22.5, (8, 5, 8))), box((7.6, 4, 9.2), (8.4, 7, 10), "m")],
         chain_els(), [7, 0, 7, 9, 16, 9], collide=False, nocol=True)
    tall("carcass", {"m": "m_rust", "c": "m_carcass", "f": "m_flesh", "t": "m_fat"},
         [box((4, -2, 5), (12, 14, 11), "c", faces={"up": "f", "down": "f"}), box((5, -6, 6), (8, -2, 10), "f"), box((8.5, -5, 6), (11, -2, 10), "f"),
          box((5.5, 14, 6.5), (7.5, 16, 9.5), "t"), box((8.5, 14, 6.5), (10.5, 16, 9.5), "t")],
         [box((5.5, 0, 6.5), (7.5, 6, 9.5), "t", rot=("z", 22.5, (6.5, 0, 8))), box((8.5, 0, 6.5), (10.5, 6, 9.5), "t", rot=("z", -22.5, (9.5, 0, 8))),
          box((7.6, 6, 7.6), (8.4, 16, 8.4), "m"), box((5, 5, 7.6), (11, 6, 8.4), "m")], [4, 0, 5, 12, 16, 11], collide=False, nocol=True)
    tall("hanging_body", {"m": "m_rust", "s": "m_skin_dead", "c": "m_scarecrow_cloth", "b": "m_blood", "f": "m_flesh"},
         [box((5, 2, 6), (11, 14, 10), "c"), box((5.2, -10, 6.5), (7.8, 2, 9.5), "c"), box((8.2, -10, 6.5), (10.8, 2, 9.5), "c"),
          box((3.5, 3, 7), (5, 14, 9), "s"), box((11, 3, 7), (12.5, 14, 9), "s"), box((6, 4, 5.8), (10, 9, 6), "b"),
          box((5.5, -12, 6), (7.8, -10, 9.6), "f"), box((8.2, -12, 6), (10.5, -10, 9.6), "f")],
         [box((6, 0, 6), (10, 5, 10), "s", faces={"north": "b"}), box((7.6, 5, 7.6), (8.4, 16, 8.4), "m"), box((5.8, -0.5, 5.8), (10.2, 0.5, 10.2), "m")],
         [5, 0, 6, 11, 16, 10], collide=False, nocol=True)
    facing("butcher_table", {"w": "m_wood_old", "b": "m_blood", "f": "m_flesh", "m": "m_iron"}, legs("w", 1, 1, 15, 15, 13, 2.5) + [
        box((0, 13, 0), (16, 16, 16), "w", faces={"up": "b"}), box((3, 16, 3), (11, 17.5, 9), "f"), box((11, 16, 9), (14, 16.4, 14), "m"),
        box((13, 16, 10.5), (15.5, 16.8, 12), "w")], [0, 0, 0, 16, 16, 16])
    facing("chopping_block", {"w": "m_wood_light", "b": "m_blood", "m": "m_iron", "h": "m_wood_old"}, [
        box((2, 0, 2), (14, 10, 14), "w", faces={"up": "b"}), box((7, 10, 4), (8, 15, 12), "m", rot=("x", 22.5, (8, 10, 8))),
        box((7.3, 13, 7.5), (8.7, 23, 9), "h", rot=("x", 22.5, (8, 10, 8)))], [2, 0, 2, 14, 10, 14])
    facing("bone_pile", {"b": "m_bone", "d": "m_bone_bloody"}, bones(), [1, 0, 1, 15, 5, 15], collide=False, nocol=True)
    facing("skull", {"b": "m_bone", "k": "m_black"}, [box((5, 0, 5), (11, 5, 12), "b"), box((5.5, 5, 5.5), (10.5, 6, 11.5), "b"),
                                                     box((6, 2.5, 4.9), (7.5, 4, 5), "k"), box((8.5, 2.5, 4.9), (10, 4, 5), "k"), box((6, 0, 3.5), (10, 2, 5), "b")],
           [5, 0, 3.5, 11, 6, 12], collide=False)
    facing("antlers", {"a": "m_antler", "b": "m_bone", "w": "m_wood_dark"}, [
        box((5, 4, 15), (11, 12, 16), "w"), box((6.5, 5, 12), (9.5, 10, 15), "b"),
        box((5, 9, 13), (6, 16, 14), "a", rot=("z", 22.5, (5.5, 9, 13.5))), box((10, 9, 13), (11, 16, 14), "a", rot=("z", -22.5, (10.5, 9, 13.5))),
        box((2, 13, 13), (3, 18, 14), "a", rot=("z", 45, (2.5, 13, 13.5))), box((13, 13, 13), (14, 18, 14), "a", rot=("z", -45, (13.5, 13, 13.5)))],
           [2, 4, 12, 14, 16, 16], collide=False, nocol=True)
    model("corpse_head", {"s": "m_skin_dead", "c": "m_scarecrow_cloth", "b": "m_blood", "f": "m_flesh", "h": "m_black"}, [
        box((5, 0, 9), (11, 5, 15), "s", faces={"up": "b"}), box((5, 3.5, 9), (11, 5.2, 15), "h", faces={"north": None}),
        box((2, 0, -2), (14, 4, 9), "c"), box((4, 4, 0), (11, 4.2, 7), "b"), box((-1, 0, 1), (2, 3, 8), "c"), box((14, 0, 0), (17, 3, 6), "c"),
        box((6, 4, 2), (10, 4.6, 6), "f")])
    facing_state("corpse_head", [("", "corpse_head")])
    SPEC.append(("corpse_head", f"new FacingBlock({j_props('flesh', nocol=True)}, {jshape([0, 0, 0, 16, 5, 16])}, false)"))
    facing("corpse_legs", {"c": "m_scarecrow_cloth", "l": "m_leather", "b": "m_blood"}, [
        box((3, 0, 0), (13, 4, 4), "c"), box((3.5, 0, 4), (7.5, 3, 14), "c"), box((8.5, 0, 4), (12.5, 3, 14), "c"),
        box((3, 0, 14), (8, 5, 17), "l"), box((8, 0, 14), (13, 5, 17), "l"), box((5, 3, 6), (11, 3.2, 12), "b")], [0, 0, 0, 16, 5, 16], collide=False,
           nocol=True, kind="flesh")
    facing("drifter", {"b": "m_bone", "c": "m_scarecrow_cloth", "k": "m_black"}, [
        box((4, 0, 6), (12, 3, 14), "c"), box((5, 3, 11), (11, 12, 14), "c"), box((6, 12, 10), (10, 16, 14), "b", faces={"north": "b"}),
        box((6.5, 13.5, 9.9), (7.8, 14.8, 10), "k"), box((8.2, 13.5, 9.9), (9.5, 14.8, 10), "k"), box((4.5, 1, 0), (6.5, 3, 7), "c"),
        box((9.5, 1, 0), (11.5, 3, 7), "c"), box((3, 4, 7), (4.5, 12, 9), "b"), box((11.5, 4, 7), (13, 12, 9), "b")], [4, 0, 6, 12, 16, 14], collide=False)
    tall("scarecrow", {"w": "m_wood_old", "c": "m_scarecrow_cloth", "f": "m_burlap_face", "s": "m_bone", "a": "m_antler", "r": "m_rope", "h": "hay"},
         [box((7, 0, 7), (9, 16, 9), "w"), box((5, 8, 6), (11, 16, 10), "c"), box((4.5, 7, 5.5), (11.5, 9, 10.5), "h"),
          box((6, 0, 6.5), (7.5, 8, 9.5), "s", rot=("z", -22.5, (6.5, 8, 8))), box((8.5, 0, 6.5), (10, 8, 9.5), "s", rot=("z", 22.5, (9.5, 8, 8)))],
         [box((7, 0, 7), (9, 14, 9), "w"), box((5, 0, 6), (11, 6, 10), "c"), box((-6, 4, 7.2), (22, 5.5, 8.8), "w"),
          box((-5, 1, 6.5), (-1, 5, 9.5), "h"), box((17, 1, 6.5), (21, 5, 9.5), "h"), box((-8, -3, 7), (-5, 5, 9), "s"), box((21, -3, 7), (24, 5, 9), "s"),
          box((4.5, 6, 5), (11.5, 14, 11), "c", faces={"north": "f"}), box((5, 14, 5.5), (11, 15.5, 10.5), "r"),
          box((4, 13, 7.5), (5, 22, 8.5), "a", rot=("z", 22.5, (4.5, 13, 8))), box((11, 13, 7.5), (12, 22, 8.5), "a", rot=("z", -22.5, (11.5, 13, 8))),
          box((1, 18, 7.5), (2, 23, 8.5), "a", rot=("z", 45, (1.5, 18, 8))), box((14, 18, 7.5), (15, 23, 8.5), "a", rot=("z", -45, (14.5, 18, 8))),
          box((5, 6, 4.5), (11, 8, 5.2), "s")], [5, 0, 5, 11, 16, 11])
    tall("corn", {"c": "m_corn_leaf", "s": "m_dry_grass"}, corn_els(False), corn_els(True), [2, 0, 2, 14, 16, 14], collide=False, nocol=True, cutout=True,
         kind="plant")
    facing("dry_grass", {"g": "m_dry_grass"}, [box((0, 0, 7.9), (16, 12, 8.1), "g", rot=("y", 45, (8, 8, 8)), shade=False, faces={"up": None, "down": None, "east": None, "west": None}),
                                               box((0, 0, 7.9), (16, 12, 8.1), "g", rot=("y", -45, (8, 8, 8)), shade=False, faces={"up": None, "down": None, "east": None, "west": None})],
           [2, 0, 2, 14, 10, 14], collide=False, nocol=True, cutout=True, kind="plant")
    facing("well", {"s": "m_stone", "w": "m_wood_old", "r": "m_rope", "k": "m_black", "m": "m_rust"}, [
        box((0, 0, 0), (16, 9, 3), "s"), box((0, 0, 13), (16, 9, 16), "s"), box((0, 0, 3), (3, 9, 13), "s"), box((13, 0, 3), (16, 9, 13), "s"),
        box((3, 0, 3), (13, 6, 13), "k"), box((1, 9, 7), (2.5, 26, 9), "w"), box((13.5, 9, 7), (15, 26, 9), "w"), box((1, 22, 7.5), (15, 23, 8.5), "w"),
        box((7.6, 12, 7.6), (8.4, 22, 8.4), "r"), box((6, 10, 6), (10, 13, 10), "m"),
        box((-1, 25, 1), (17, 26.5, 9), "w", rot=("x", 22.5, (8, 26, 8))), box((-1, 25, 7), (17, 26.5, 15), "w", rot=("x", -22.5, (8, 26, 8)))],
           [0, 0, 0, 16, 16, 16])
    facing("tire", {"k": "m_black"}, [box((1, 0, 1), (15, 4, 4), "k"), box((1, 0, 12), (15, 4, 15), "k"), box((1, 0, 4), (4, 4, 12), "k"), box((12, 0, 4), (15, 4, 12), "k")],
           [1, 0, 1, 15, 4, 15])
    facing("padlock", {"m": "m_iron", "r": "m_rust"}, [box((5, 3, 14), (11, 9, 16), "m"), box((6, 9, 14.5), (7, 13, 15.5), "r"), box((9, 9, 14.5), (10, 13, 15.5), "r"),
                                                     box((6, 12, 14.5), (10, 13, 15.5), "r")], [5, 3, 14, 11, 13, 16], collide=False, nocol=True)
    facing("chain_wall", {"m": "m_rust"}, [box((7.5, 0, 15), (8.5, 16, 16), "m")] + [box((6.8, y, 14.6), (9.2, y + 2, 15.4), "m") for y in range(0, 16, 4)],
           [6.8, 0, 14.6, 9.2, 16, 16], collide=False, nocol=True)
    facing("note_paper", {"p": "m_paper"}, [box((3, 0, 4), (13, 0.3, 12), "p", rot=("y", 22.5, (8, 0, 8)))], [3, 0, 3, 13, 1, 13], collide=False, nocol=True)
    facing("photo_frame", {"w": "m_wood_dark", "p": "m_photo"}, [box((5, 0, 9), (11, 7, 10), "w", faces={"north": "p"}, rot=("x", -22.5, (8, 0, 10))),
                                                                box((7, 0, 10), (9, 5, 12), "w")], [5, 0, 9, 11, 7, 12], collide=False)
    facing("candles", {"w": "m_wax", "f": "m_flame", "m": "m_tin"}, [box((5, 0, 5), (11, 0.5, 11), "m"), box((6, 0.5, 6), (7.5, 5, 7.5), "w"),
                                                                    box((8.5, 0.5, 7), (10, 3.5, 8.5), "w"), box((6.5, 0.5, 9), (8, 4, 10.5), "w"),
                                                                    box((6, 5, 6), (7.5, 6.5, 7.5), "f", shade=False), box((8.5, 3.5, 7), (10, 5, 8.5), "f", shade=False)],
           [5, 0, 5, 11, 6, 11], collide=False, cutout=True, light="8")

    facing("walkie", {"b": "m_black", "m": "m_iron", "g": "m_lamp_glass_on"}, [box((6, 0, 7), (10, 7, 9.5), "b"), box((8.6, 7, 7.6), (9.2, 12, 8.2), "m"),
                                                                           box((6.6, 4.5, 6.8), (9.4, 6.5, 7), "m"), box((7, 6.6, 6.8), (7.6, 7, 7), "g")],
           [6, 0, 7, 10, 12, 9.5], collide=False)
    facing("birdbox", {"w": "m_wood_grey", "k": "m_black", "r": "m_rust"}, [box((5, 0, 7), (11, 7, 13), "w", faces={"north": "w"}),
                                                                           box((4.5, 7, 6.5), (11.5, 8, 13.5), "w", rot=("x", -22.5, (8, 7, 10))),
                                                                           box((7, 3.5, 6.9), (9, 5.5, 7), "k"), box((7.6, 2, 6), (8.4, 2.8, 7), "r"),
                                                                           box((7, -2, 12), (9, 0, 13), "w")], [5, 0, 7, 11, 8, 13], collide=False)
    facing("meat_chunk", {"f": "m_flesh", "t": "m_fat", "b": "m_bone"}, [box((4, 0, 4), (12, 4, 11), "f", faces={"up": "t"}), box((11, 1, 6), (15, 2.5, 7.5), "b"),
                                                                        box((5, 4, 5), (9, 5, 8), "f")], [4, 0, 4, 12, 5, 11], collide=False, kind="flesh")

    # items for every block
    os.makedirs(os.path.join(A, "models/item"), exist_ok=True)
    for name, mdl in ITEM_MODEL.items():
        W(f"models/item/{name}.json", {"parent": f"{NS}:block/{mdl}"})
    write_java()
    print("blocks", len(SPEC), "textures", len(T))


# ---------------------------------------------------------------------------------------------------- helpers for odd shapes
def wheel():
    e = [box((7, 7, 13.5), (9, 9, 16), "m")]
    for k in range(8):
        ang = k / 8 * 360
        e.append(box((7.2, 1, 14), (8.8, 15, 15), "w", rot=("z", [0, 45, -45, 22.5, -22.5, 0, 45, -45][k % 8], (8, 8, 14.5))) if k < 4 else
                 box((1, 7.2, 14), (15, 8.8, 15), "w"))
    for (x1, y1, x2, y2) in ((2, 0, 14, 1.5), (2, 14.5, 14, 16), (0, 2, 1.5, 14), (14.5, 2, 16, 14)):
        e.append(box((x1, y1, 13.6), (x2, y2, 15.4), "w"))
    return e


def chain_els():
    e = []
    for y in range(0, 16, 3):
        if (y // 3) % 2 == 0:
            e.append(box((7.6, y, 7), (8.4, y + 3, 9), "m", faces={"up": None, "down": None}))
        else:
            e.append(box((7, y, 7.6), (9, y + 3, 8.4), "m", faces={"up": None, "down": None}))
    return e


def bones():
    r = random.Random(5)
    e = []
    for i in range(14):
        x, z = r.uniform(1, 13), r.uniform(1, 13)
        ln = r.uniform(4, 9)
        t = "d" if i % 3 == 0 else "b"
        ang = r.choice((-45, -22.5, 0, 22.5, 45))
        e.append(box((x, 0, z), (x + 1.2, 1.2, min(16, z + ln)), t, rot=("y", ang, (x + 0.6, 0.6, z + ln / 2))))
        e.append(box((x - 0.3, 0, z - 0.3), (x + 1.5, 1.6, z + 0.6), t))
    e.append(box((5, 0, 5), (10, 4, 10), "b"))
    e.append(box((5.5, 1.5, 4.9), (6.8, 2.8, 5), "d"))
    return e


def corn_els(upper):
    r = random.Random(9 if upper else 8)
    e = []
    for (x, z) in ((4, 4), (11, 5), (6, 11), (12, 12)):
        h = 16
        e.append(box((x, 0, z), (x + 1, h, z + 1), "s"))
        for k in range(3):
            y = r.uniform(2, 14)
            ang = r.choice((-45, 45, -22.5, 22.5))
            e.append(box((x - 4, y, z + 0.4), (x + 5, y + 6, z + 0.6), "c", rot=("y", ang, (x + 0.5, y, z + 0.5)), shade=False,
                         faces={"up": None, "down": None, "east": None, "west": None}))
        if upper:
            e.append(box((x - 0.5, 10, z - 0.5), (x + 1.5, 15, z + 1.5), "c"))
    return e


def stairs_block(name, tex, kind):
    t = {"bottom": f"{NS}:block/{tex}", "top": f"{NS}:block/{tex}", "side": f"{NS}:block/{tex}"}
    for suf, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        W(f"models/block/{name}{suf}.json", {"parent": f"minecraft:block/{parent}", "textures": t})
    W(f"blockstates/{name}.json", vanilla_stairs_state(f"{NS}:block/{name}"))
    ITEM_MODEL[name] = name
    SPEC.append((name, f"Compat.stairs(Blocks.OAK_PLANKS.defaultBlockState(), {j_props(kind)})"))


def vanilla_state(kind, base):
    """copy a vanilla blockstate file (taken from the local Minecraft client jar, not shipped) and point it at our models"""
    p = os.path.join(HERE, "vanilla", f"oak_{kind}.json")
    if not os.path.exists(p):
        import glob, zipfile
        jars = glob.glob(os.path.expanduser("~/.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client-extra.jar"))
        os.makedirs(os.path.dirname(p), exist_ok=True)
        with zipfile.ZipFile(jars[0]) as z:
            open(p, "wb").write(z.read(f"assets/minecraft/blockstates/oak_{kind}.json"))
    src = open(p).read()
    return json.loads(src.replace(f"minecraft:block/oak_{kind}", base))


def vanilla_stairs_state(base):
    return vanilla_state("stairs", base)


def _old_vanilla_stairs_state(base):
    v = {}
    fy = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing_, y0 in fy.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                mdl = base + ("" if shape == "straight" else "_inner" if shape.startswith("inner") else "_outer")
                y = y0
                if shape in ("inner_left", "outer_left"):
                    y = (y + 270) % 360
                x = 0
                if half == "top":
                    x = 180
                    if shape in ("inner_left", "outer_left"):
                        y = (y + 90) % 360
                    elif shape in ("inner_right", "outer_right"):
                        y = (y + 90) % 360
                e = {"model": mdl}
                if x:
                    e["x"] = x
                if y:
                    e["y"] = y
                if x or y:
                    e["uvlock"] = True
                v[f"facing={facing_},half={half},shape={shape}"] = e
    return {"variants": v}


def slab_block(name, tex, kind):
    t = {"bottom": f"{NS}:block/{tex}", "top": f"{NS}:block/{tex}", "side": f"{NS}:block/{tex}"}
    W(f"models/block/{name}.json", {"parent": "minecraft:block/slab", "textures": t})
    W(f"models/block/{name}_top.json", {"parent": "minecraft:block/slab_top", "textures": t})
    W(f"models/block/{name}_double.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{tex}"}})
    W(f"blockstates/{name}.json", {"variants": {"type=bottom": {"model": f"{NS}:block/{name}"}, "type=top": {"model": f"{NS}:block/{name}_top"},
                                                "type=double": {"model": f"{NS}:block/{name}_double"}}})
    ITEM_MODEL[name] = name
    SPEC.append((name, f"new SlabBlock({j_props(kind)})"))


def staircase():
    """the farmhouse staircase: treads with a nosing, a dark riser, a carved stringer and a runner down the middle"""
    # the core fills exactly the vanilla stair shape (bottom slab + back half), so faces it hides are really covered
    els = [box((0, 0, 0), (16, 8, 16), "r"), box((0, 8, 0), (16, 16, 8), "r"),  # solid core (dark)
           box((0, 7, -0.6), (16, 8.2, 8), "t"), box((0, 15, 7.4), (16, 16.2, 16), "t"),  # treads with nosing
           box((4, 8.2, 0), (12, 8.4, 7.4), "c"), box((4, 16.2, 8), (12, 16.4, 16), "c"),  # runner
           box((4, 0, -0.1), (12, 7, 0), "c"), box((4, 8, 7.9), (12, 15, 8), "c")]
    model("staircase", {"r": "floorboards_dark", "t": "m_wood_dark", "c": "m_rug_runner"}, els)
    v = {}
    fy = {"north": 0, "east": 90, "south": 180, "west": 270}
    for facing_, y in fy.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                e = {"model": f"{NS}:block/staircase"}
                if y:
                    e["y"] = y
                v[f"facing={facing_},half={half},shape={shape}"] = e
    # the model's high side is +z at y... it rises toward -z; vanilla stairs facing=north rise toward north. rotate so they agree
    for k, e in v.items():
        e["y"] = (e.get("y", 0) + 0) % 360
        if not e["y"]:
            e.pop("y")
    W("blockstates/staircase.json", {"variants": v})
    ITEM_MODEL["staircase"] = "staircase"
    SPEC.append(("staircase", f"Compat.stairs(Blocks.OAK_PLANKS.defaultBlockState(), {j_props('wood')})"))


def door_block(name, tex_bottom, tex_top):
    t = {"bottom": f"{NS}:block/{tex_bottom}", "top": f"{NS}:block/{tex_top}"}
    parts = ["bottom_left", "bottom_left_open", "bottom_right", "bottom_right_open", "top_left", "top_left_open", "top_right", "top_right_open"]
    for p in parts:
        W(f"models/block/{name}_{p}.json", {"parent": f"minecraft:block/door_{p}", "textures": t, "render_type": "minecraft:cutout"})
    W(f"blockstates/{name}.json", vanilla_state("door", f"{NS}:block/{name}"))
    W(f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:block/{tex_bottom}"}})
    SPEC.append((name, f"Compat.door({j_props('wood')}.noOcclusion())"))


def trapdoor_block(name, tex):
    t = {"texture": f"{NS}:block/{tex}"}
    for p in ("bottom", "top", "open"):
        W(f"models/block/{name}_{p}.json", {"parent": f"minecraft:block/template_trapdoor_{p}", "textures": t, "render_type": "minecraft:cutout"})
    W(f"blockstates/{name}.json", vanilla_state("trapdoor", f"{NS}:block/{name}"))
    ITEM_MODEL[name] = f"{name}_bottom"
    SPEC.append((name, f"Compat.trapdoor({j_props('wood')}.noOcclusion())"))


def ladder_block():
    els = [box((2, 0, 14), (4, 16, 16), "w"), box((12, 0, 14), (14, 16, 16), "w")]
    for y in (2, 6, 10, 14):
        els.append(box((4, y, 14.4), (12, y + 1.2, 15.6), "w"))
    model("farm_ladder", {"w": "m_wood_old"}, els)
    v = {}
    for f, r in ROT.items():
        for wl in ("false", "true"):
            e = {"model": f"{NS}:block/farm_ladder"}
            if r:
                e["y"] = r
            v[f"facing={f},waterlogged={wl}"] = e
    W("blockstates/farm_ladder.json", {"variants": v})
    ITEM_MODEL["farm_ladder"] = "farm_ladder"
    SPEC.append(("farm_ladder", f"new LadderBlock({j_props('wood')}.noOcclusion())"))


def barbed_fence():
    model("barbed_post", {"w": "m_wood_old"}, [box((6.5, 0, 6.5), (9.5, 16, 9.5), "w"), box((6, 14, 6), (10, 16.5, 10), "w")])
    model("barbed_side", {"x": "m_wire"}, [box((7.9, 0, 0), (8.1, 16, 8), "x", faces={"up": None, "down": None, "north": None, "south": None}, shade=False)],
          cutout=True)
    mp = [{"apply": {"model": f"{NS}:block/barbed_post"}}]
    for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        a = {"model": f"{NS}:block/barbed_side", "uvlock": False}
        if y:
            a["y"] = y
        mp.append({"when": {d: "true"}, "apply": a})
    W("blockstates/barbed_fence.json", {"multipart": mp})
    ITEM_MODEL["barbed_fence"] = "barbed_post"
    SPEC.append(("barbed_fence", f"new FenceBlock({j_props('wood')}.noOcclusion())"))


def farm_gate():
    closed = [box((0, 0, 7), (2, 16, 9), "w"), box((14, 0, 7), (16, 16, 9), "w")]
    for y in (2, 6, 10, 14):
        closed.append(box((2, y, 7.3), (14, y + 1.5, 8.7), "w"))
    closed.append(box((2, 2, 7.2), (14, 3.5, 8.8), "w", rot=("z", 45, (8, 8, 8))))
    closed.append(box((6, 7, 6.6), (11, 9, 7.2), "m"))
    opened = [box((0, 0, 7), (2, 16, 9), "w")] + [box((0.5, y, 9), (1.9, y + 1.5, 21), "w") for y in (2, 6, 10, 14)]
    openable("farm_gate", {"w": "m_wood_grey", "m": "m_rust"}, closed, opened, [0, 0, 7, 16, 16, 9])


def write_java():
    lines = []
    for name, expr in SPEC:
        const = name.upper()
        lines.append(f'    public static final RegistryObject<Block> {const} = reg("{name}", () -> {expr});')
    src = f'''package com.krolasyon.mimic.registry;

import com.krolasyon.mimic.MimicMod;
import com.krolasyon.mimic.block.FacingBlock;
import com.krolasyon.mimic.block.ModeBlock;
import com.krolasyon.mimic.block.OpenableBlock;
import com.krolasyon.mimic.block.TallBlock;
import com.krolasyon.mimic.block.VariantBlock;
import com.krolasyon.mimic.compat.Compat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** GENERATED by tools/gen_farm.py - do not edit by hand. */
public final class ModBlocks {{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MimicMod.MODID);
    public static final List<RegistryObject<Block>> ALL = new ArrayList<>();

    private static BlockBehaviour.Properties wood() {{ return BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).sound(SoundType.WOOD); }}
    private static BlockBehaviour.Properties stone() {{ return BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).sound(SoundType.STONE); }}
    private static BlockBehaviour.Properties metal() {{ return BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).sound(SoundType.METAL); }}
    private static BlockBehaviour.Properties soft() {{ return BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).sound(SoundType.GRASS); }}
    private static BlockBehaviour.Properties plant() {{ return BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).sound(SoundType.CROP).noOcclusion(); }}
    private static BlockBehaviour.Properties flesh() {{ return BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).sound(SoundType.SLIME_BLOCK).noOcclusion(); }}
    private static BlockBehaviour.Properties glass() {{ return BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).sound(SoundType.GLASS).noOcclusion(); }}
    private static BlockBehaviour.Properties prop() {{ return BlockBehaviour.Properties.of().strength(-1.0f, 3600000.0f).sound(SoundType.WOOD).noOcclusion(); }}

    private static RegistryObject<Block> reg(String name, Supplier<Block> s) {{
        RegistryObject<Block> r = BLOCKS.register(name, s);
        ALL.add(r);
        return r;
    }}

{chr(10).join(lines)}

    private ModBlocks() {{}}
}}
'''
    with open(JAVA, "w") as f:
        f.write(src)


if __name__ == "__main__":
    gen()
