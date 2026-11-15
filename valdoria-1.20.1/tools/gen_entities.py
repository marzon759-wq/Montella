#!/usr/bin/env python3
"""Generates the creature models of Valdoria: Java LayerDefinitions + base and glow textures, from one description.

Run from valdoria-1.20.1/:  python3 tools/gen_entities.py
"""
import math
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources", "assets", "valdoria", "textures", "entity")
JAVA = os.path.join(HERE, "..", "src", "main", "java", "com", "krolasyon", "valdoria", "client", "model", "ValdoriaModels.java")


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def P(*hexes):
    return [hexrgb(h) for h in hexes]


# ---------------------------------------------------------------------------------------------------------------
# Model description: part(name, parent, pivot, rot, cubes) ; cube(origin, size, material)
# ---------------------------------------------------------------------------------------------------------------

def part(name, parent, pivot=(0, 0, 0), rot=(0, 0, 0), cubes=()):
    return {"name": name, "parent": parent, "pivot": pivot, "rot": rot, "cubes": list(cubes)}


def cube(o, s, mat):
    return {"o": o, "s": s, "mat": mat}


def mirror_x(parts_, suffix_from="_l", suffix_to="_r"):
    """Returns right-side copies of left-side parts (x negated, y/z rotations negated)."""
    out = []
    for p in parts_:
        q = dict(p)
        q["name"] = p["name"].replace(suffix_from, suffix_to)
        q["parent"] = p["parent"].replace(suffix_from, suffix_to)
        q["pivot"] = (-p["pivot"][0], p["pivot"][1], p["pivot"][2])
        q["rot"] = (p["rot"][0], -p["rot"][1], -p["rot"][2])
        q["cubes"] = [{"o": (-(c["o"][0] + c["s"][0]), c["o"][1], c["o"][2]), "s": c["s"], "mat": c["mat"], "flip": True} for c in p["cubes"]]
        out.append(q)
    return out


def veilstag():
    parts = [
        part("body", "root", (0, -13, 0), cubes=[cube((-3.5, -3.5, -8), (7, 7, 16), "stag_fur")]),
        part("neck", "body", (0, -1.5, -7), (0.45, 0, 0), [cube((-2, -10, -2), (4, 11, 4), "stag_neck")]),
        part("head", "neck", (0, -10, 0), (-0.45, 0, 0), [cube((-2.5, -3.5, -5), (5, 5, 6), "stag_head")]),
        part("snout", "head", cubes=[cube((-1.5, -1.5, -8), (3, 3, 3), "stag_snout")]),
        part("tail", "body", (0, -2.5, 8), (0.35, 0, 0), [cube((-1, 0, -0.5), (2, 4, 1), "stag_tail")]),
    ]
    left = [
        part("ear_l", "head", (2.5, -3, -0.5), (0, 0, -0.9), [cube((0, -1, -0.5), (4, 2, 1), "stag_ear")]),
        part("antler_l", "head", (1.5, -3.5, -1.5), (-0.25, 0, 0.45), [cube((-0.5, -9, -0.5), (1, 9, 1), "antler")]),
        part("tine_l1", "antler_l", (0, -3, 0), (-1.0, 0, 0), [cube((-0.5, -4, -0.5), (1, 4, 1), "antler")]),
        part("tine_l2", "antler_l", (0, -6, 0), (0, 0, 0.9), [cube((-0.5, -4, -0.5), (1, 4, 1), "antler_tip")]),
        part("tine_l3", "antler_l", (0, -9, 0), (0.6, 0, 0), [cube((-0.5, -3, -0.5), (1, 3, 1), "antler_tip")]),
        part("tine_l4", "antler_l", (0, -7.5, 0), (-0.9, 0, 0), [cube((-0.5, -3, -0.5), (1, 3, 1), "antler_tip")]),
        part("moss_l", "antler_l", (0, -5, 0), (0.25, 0, -0.45), [cube((0, 0, -0.5), (0, 7, 1), "moss")]),
        part("moss_l2", "antler_l", (0, -8, 0), (-0.2, 0, -0.45), [cube((0, 0, -0.5), (0, 5, 1), "moss")]),
    ]
    parts += left + mirror_x(left)
    for n, x, z in (("leg_fl", 2, -6), ("leg_fr", -2, -6), ("leg_bl", 2, 6), ("leg_br", -2, 6)):
        parts.append(part(n, "root", (x, -10, z), cubes=[cube((-1, 0, -1), (2, 10, 2), "stag_leg")]))
    return {"name": "veilstag", "tex": (128, 64), "parts": parts}


def gloomlynx():
    parts = [
        part("body", "root", (0, -10, 0), cubes=[cube((-3, -3, -7), (6, 6, 14), "lynx_fur")]),
        part("head", "body", (0, -2, -7), cubes=[cube((-3, -3, -5), (6, 5, 5), "lynx_head")]),
        part("muzzle", "head", cubes=[cube((-1.5, -0.5, -7), (3, 3, 2), "lynx_muzzle")]),
        part("tail", "body", (0, -2, 7), (-0.6, 0, 0), [cube((-1, -1, 0), (2, 2, 10), "lynx_tail")]),
    ]
    left = [
        part("ear_l", "head", (2, -3, -2.5), (0, 0, 0.2), [cube((-1, -3, -0.5), (2, 3, 1), "lynx_ear")]),
        part("tuft_l", "ear_l", (0, -3, 0), cubes=[cube((-0.5, -3, 0), (1, 3, 0), "tuft")]),
        part("ruff_l", "head", (3, 0, -2), (0, -0.3, 0), cubes=[cube((0, -1, -2), (1, 4, 3), "lynx_ruff")]),
    ]
    parts += left + mirror_x(left)
    for n, x, z in (("leg_fl", 2, -5), ("leg_fr", -2, -5), ("leg_bl", 2, 5), ("leg_br", -2, 5)):
        parts.append(part(n, "root", (x, -7, z), cubes=[cube((-1, 0, -1), (2, 7, 2), "lynx_leg")]))
    return {"name": "gloomlynx", "tex": (64, 64), "parts": parts}


def vinestalker():
    parts = [
        part("body", "root", (0, -11, 0), cubes=[cube((-3, -2, -4), (6, 4, 8), "carapace")]),
        part("abdomen", "body", (0, -1, 4), (-0.35, 0, 0), [cube((-3, -2.5, 0), (6, 5, 9), "abdomen")]),
        part("head", "body", (0, 0, -4), cubes=[cube((-2.5, -2, -4), (5, 4, 4), "stalker_head")]),
        part("tendril_1", "body", (-2, 2, -1), cubes=[cube((0, 0, -0.5), (0, 9, 1), "tendril")]),
        part("tendril_2", "body", (2, 2, 1), cubes=[cube((0, 0, -0.5), (0, 11, 1), "tendril")]),
        part("tendril_3", "abdomen", (-1.5, 2.5, 4), cubes=[cube((0, 0, -0.5), (0, 13, 1), "tendril")]),
        part("tendril_4", "abdomen", (1.5, 2.5, 7), cubes=[cube((0, 0, -0.5), (0, 8, 1), "tendril")]),
    ]
    left = [
        part("mandible_l", "head", (1.5, 1.5, -4), (0.3, 0, 0), [cube((-0.5, 0, -2), (1, 1, 2), "mandible")]),
    ]
    for i, (z, yr) in enumerate(((-2.5, 0.6), (0.5, 0.0), (3.5, -0.6))):
        left.append(part(f"leg{i}_l", "body", (3, 0, z), (0, yr, -0.8), [cube((0, -0.5, -0.5), (9, 1, 1), "stalker_leg")]))
        left.append(part(f"shin{i}_l", f"leg{i}_l", (9, 0, 0), (0, 0, 1.95), [cube((0, -0.5, -0.5), (19, 1, 1), "stalker_shin")]))
    parts += left + mirror_x(left)
    return {"name": "vinestalker", "tex": (64, 64), "parts": parts}


def lumen_moth():
    parts = [
        part("body", "root", (0, -4, 0), cubes=[cube((-1, -1, -2), (2, 2, 5), "moth_body")]),
        part("head", "body", cubes=[cube((-1, -1, -4), (2, 2, 2), "moth_head")]),
    ]
    left = [
        part("antenna_l", "body", (0.5, -1, -3.5), (-0.5, 0, 0.35), [cube((0, -4, 0), (1, 4, 0), "antenna")]),
        part("wing_l", "body", (1, -1, -1.5), cubes=[cube((0, 0, -3), (8, 0, 7), "forewing")]),
        part("hindwing_l", "body", (1, -0.8, 1), cubes=[cube((0, 0, 0), (5, 0, 9), "hindwing")]),
    ]
    parts += left + mirror_x(left)
    return {"name": "lumen_moth", "tex": (64, 32), "parts": parts}


MODELS = [veilstag(), gloomlynx(), vinestalker(), lumen_moth()]


# ---------------------------------------------------------------------------------------------------------------
# Painting
# ---------------------------------------------------------------------------------------------------------------

MATS = {
    "stag_fur": P("#3a2c44", "#43334f", "#4b395a", "#352840"),
    "stag_neck": P("#4b395a", "#55426a", "#3f2f4a"),
    "stag_head": P("#43334f", "#4b395a", "#3a2c44"),
    "stag_snout": P("#2a1f31", "#30243a"),
    "stag_tail": P("#d8d0e8", "#c8bede"),
    "stag_ear": P("#43334f", "#4b395a"),
    "stag_leg": P("#2e2236", "#35283f", "#281e2f"),
    "antler": P("#cfc6b8", "#bdb3a4", "#e0d8cc"),
    "antler_tip": P("#cfc6b8", "#bdb3a4"),
    "moss": P("#1d1524", "#271c30"),
    "lynx_fur": P("#3b3550", "#443d5c", "#353049", "#4c4566"),
    "lynx_head": P("#443d5c", "#4c4566", "#3b3550"),
    "lynx_muzzle": P("#cfc8dc", "#bdb5cc"),
    "lynx_tail": P("#3b3550", "#443d5c"),
    "lynx_ear": P("#2a2539", "#332d45"),
    "tuft": P("#15121d",),
    "lynx_ruff": P("#a69dbd", "#968daf", "#b5adc9"),
    "lynx_leg": P("#353049", "#3b3550"),
    "carapace": P("#140e19", "#1b1322", "#22182b", "#2a1e34"),
    "abdomen": P("#1b1322", "#22182b", "#2a1e34"),
    "stalker_head": P("#1b1322", "#22182b"),
    "tendril": P("#100b14", "#1e1626"),
    "mandible": P("#4a3758", "#3b2a46"),
    "stalker_leg": P("#1b1322", "#22182b", "#2a1e34"),
    "stalker_shin": P("#140e19", "#1b1322", "#22182b"),
    "moth_body": P("#d9f3ef", "#c3e8e3", "#e8faf7"),
    "moth_head": P("#c3e8e3", "#d9f3ef"),
    "antenna": P("#e8faf7",),
    "forewing": P("#9fe8e0", "#b4f0e8", "#8adbd2"),
    "hindwing": P("#9fe8e0", "#8adbd2"),
}
GLOW = hexrgb("#7cf5ff")
GLOW_HI = hexrgb("#d6feff")


class Canvas:
    def __init__(self, w, h, seed):
        self.base = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.bp = self.base.load()
        self.gp = self.glow.load()
        self.r = random.Random(seed)

    def put(self, x, y, c, glow=None):
        self.bp[x, y] = c + (255,)
        if glow:
            self.gp[x, y] = glow + (255,)

    def clear(self, x, y):
        self.bp[x, y] = (0, 0, 0, 0)


def paint_face(cv, mat, face, x0, y0, w, h, flip):
    pal = MATS[mat]
    r = cv.r
    for y in range(h):
        for x in range(w):
            c = pal[r.randrange(len(pal))]
            # light from above, darker underneath
            if face == "bottom":
                c = tuple(max(0, v - 14) for v in c)
            cv.put(x0 + x, y0 + y, c)
    lx = (lambda x: w - 1 - x) if flip else (lambda x: x)

    if mat == "stag_fur" and face == "top":
        # a faint glowing line of spots down the spine
        for y in range(1, h, 3):
            cv.put(x0 + w // 2, y0 + y, hexrgb("#4fb8c4"), GLOW)
    if mat == "stag_fur" and face in ("left", "right"):
        for _ in range(3):
            cv.put(x0 + r.randrange(w), y0 + r.randrange(max(1, h // 2)), hexrgb("#5a4870"))
    if mat == "stag_neck" and face == "front":
        for y in range(h // 3, h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, hexrgb("#bfb3d4") if r.random() < 0.7 else hexrgb("#a89cc0"))
    if mat == "stag_head" and face in ("left", "right"):
        # eye near the front edge of the side face
        ex = 1 if face == "left" else w - 2
        ex = w - 2 if (face == "left") != flip else 1
        cv.put(x0 + ex, y0 + 1, hexrgb("#9ef9ff"), GLOW_HI)
    if mat == "stag_snout" and face == "front":
        for x in range(w):
            cv.put(x0 + x, y0, hexrgb("#120c16"))
    if mat == "stag_leg":
        for x in range(w):
            for y in range(max(0, h - 2), h):
                cv.put(x0 + x, y0 + y, hexrgb("#120c16"))
    if mat == "antler_tip":
        if face in ("front", "back", "left", "right"):
            cv.put(x0, y0, GLOW_HI, GLOW_HI)
            if h > 1:
                cv.put(x0, y0 + 1, GLOW, GLOW)
    if mat == "antler" and face in ("front", "left") and h > 4:
        cv.put(x0, y0 + r.randrange(h), hexrgb("#4fb8c4"), GLOW)
    if mat == "moss":
        for y in range(h):
            for x in range(w):
                if r.random() < 0.25:
                    cv.clear(x0 + x, y0 + y)
        cv.put(x0 + w - 1 if w else x0, y0 + h - 1, GLOW_HI, GLOW_HI)
        if h > 3:
            cv.put(x0, y0 + h // 2, GLOW, GLOW)

    if mat == "lynx_fur" and face in ("top", "left", "right"):
        # dark rosettes, a few of them faintly glowing
        for _ in range(max(2, w * h // 12)):
            x, y = r.randrange(w), r.randrange(h)
            glow = r.random() < 0.18
            cv.put(x0 + x, y0 + y, hexrgb("#4fd8e6") if glow else hexrgb("#25213a"), GLOW if glow else None)
    if mat == "lynx_head" and face == "front":
        for ex in (1, w - 2):
            cv.put(x0 + ex, y0 + 1, GLOW_HI, GLOW_HI)
            cv.put(x0 + ex + (1 if ex == 1 else -1), y0 + 1, GLOW, GLOW)
        for x in range(w):
            cv.put(x0 + x, y0 + h - 1, hexrgb("#cfc8dc"))
    if mat == "lynx_muzzle" and face == "front":
        cv.put(x0 + w // 2, y0, hexrgb("#2a1f31"))
    if mat == "lynx_tail":
        if face in ("back",):
            for y in range(h):
                for x in range(w):
                    cv.put(x0 + x, y0 + y, GLOW, GLOW)
        if face in ("top", "left", "right", "bottom"):
            for i in range(0, max(w, h), 3):
                if face in ("top", "bottom") and i < h:
                    for x in range(w):
                        cv.put(x0 + x, y0 + i, hexrgb("#25213a"))
                elif face in ("left", "right") and i < w:
                    for y in range(h):
                        cv.put(x0 + i, y0 + y, hexrgb("#25213a"))
    if mat == "tuft":
        for y in range(h):
            if y < h - 3:
                cv.clear(x0, y0 + y)

    if mat in ("carapace", "abdomen") and face in ("top", "left", "right", "back"):
        # vine-like ridges and a few bioluminescent pores
        for _ in range(max(2, w * h // 10)):
            x, y = r.randrange(w), r.randrange(h)
            cv.put(x0 + x, y0 + y, hexrgb("#33244a"))
        for _ in range(max(1, w * h // 28)):
            x, y = r.randrange(w), r.randrange(h)
            cv.put(x0 + x, y0 + y, hexrgb("#5a2e86"), hexrgb("#a364cf"))
    if mat == "stalker_head" and face == "front":
        for (x, y) in ((1, 1), (3, 1), (2, 0), (0, 2), (4, 2), (2, 2)):
            if x < w and y < h:
                cv.put(x0 + x, y0 + y, GLOW_HI if (x + y) % 2 == 0 else GLOW, GLOW_HI if (x + y) % 2 == 0 else GLOW)
    if mat == "tendril":
        for y in range(h):
            for x in range(w):
                if r.random() < 0.2:
                    cv.clear(x0 + x, y0 + y)
        if h > 2:
            cv.put(x0 + (w - 1 if w else 0), y0 + h - 1, GLOW, GLOW)
    if mat == "stalker_shin" and face in ("front", "back", "left", "right"):
        if w > 6:
            cv.put(x0 + (w - 1 if not flip else 0), y0, hexrgb("#4a3758"))

    if mat == "moth_head" and face == "front":
        cv.put(x0, y0, hexrgb("#1d1524"))
        cv.put(x0 + w - 1, y0, hexrgb("#1d1524"))
    if mat == "moth_body":
        for y in range(h):
            for x in range(w):
                if (y + x) % 3 == 0:
                    cv.put(x0 + x, y0 + y, hexrgb("#a6d8d2"), hexrgb("#6fd8d0") if face == "bottom" else None)
    if mat == "antenna":
        for y in range(h):
            for x in range(w):
                cv.put(x0 + x, y0 + y, hexrgb("#e8faf7"), hexrgb("#9ef9ff") if y == 0 else None)


def paint_wing(cv, x0, y0, w, h, hind, flip):
    """Wing planes: shaped (transparent outside), mint with a glowing vein network and an eye spot."""
    r = cv.r
    for y in range(h):
        for x in range(w):
            xx = (w - 1 - x) if flip else x
            if hind:
                # tapering long tail toward the back (y grows backward)
                t = y / (h - 1)
                width = (w - 0.5) * (1 - t) ** 0.9 + 0.8
                inside = xx < width and not (t > 0.55 and xx < 1 and y % 2 == 0 and False)
            else:
                # rounded forewing, widest at the tip
                cx, cy = (w - 1) * 0.55, (h - 1) * 0.5
                inside = ((xx - cx) / (w * 0.62)) ** 2 + ((y - cy) / (h * 0.56)) ** 2 <= 1.0 or (xx < 2 and 1 <= y <= h - 2)
            if not inside:
                cv.clear(x0 + x, y0 + y)
                continue
            c = MATS["hindwing" if hind else "forewing"][r.randrange(2)]
            edge = False
            if hind:
                t = y / (h - 1)
                width = (w - 0.5) * (1 - t) ** 0.9 + 0.8
                edge = xx >= width - 1.2
            else:
                cx, cy = (w - 1) * 0.55, (h - 1) * 0.5
                edge = ((xx - cx) / (w * 0.62)) ** 2 + ((y - cy) / (h * 0.56)) ** 2 > 0.62
            if edge:
                cv.put(x0 + x, y0 + y, hexrgb("#5fd0c8"), GLOW)
            else:
                cv.put(x0 + x, y0 + y, c)
    # veins
    if not hind:
        for xx in range(w):
            x = (w - 1 - xx) if flip else xx
            cv.put(x0 + x, y0 + h // 2, hexrgb("#c8fdff"), GLOW_HI if xx % 2 == 0 else GLOW)
        ex = (w - 1 - (w - 3)) if flip else (w - 3)
        cv.put(x0 + ex, y0 + h // 2 - 1, hexrgb("#8f52c0"), hexrgb("#c08ae8"))
        cv.put(x0 + ex, y0 + h // 2 + 1, hexrgb("#8f52c0"), hexrgb("#c08ae8"))
    else:
        for y in range(1, h):
            x = 0 if not flip else w - 1
            cv.put(x0 + x, y0 + y, hexrgb("#c8fdff"), GLOW)


def layout(model):
    """Shelf-pack every cube's UV island and record its texOffs."""
    W, H = model["tex"]
    x = y = row_h = 0
    for p in model["parts"]:
        for c in p["cubes"]:
            w, h, d = (int(math.ceil(v)) for v in c["s"])
            iw, ih = 2 * (d + w), d + h
            iw = max(iw, 1)
            ih = max(ih, 1)
            if x + iw > W:
                x, y, row_h = 0, y + row_h, 0
            c["uv"] = (x, y)
            x += iw
            row_h = max(row_h, ih)
            if y + ih > H:
                raise SystemExit(f"{model['name']}: texture too small")


def paint(model, seed):
    W, H = model["tex"]
    cv = Canvas(W, H, seed)
    for p in model["parts"]:
        for c in p["cubes"]:
            u, v = c["uv"]
            w, h, d = (int(math.ceil(s)) for s in c["s"])
            flip = c.get("flip", False)
            if c["mat"] in ("forewing", "hindwing"):
                hind = c["mat"] == "hindwing"
                if w and d:
                    paint_wing(cv, u + d, v, w, d, hind, flip)
                    paint_wing(cv, u + d + w, v, w, d, hind, not flip)
                continue
            faces = [("top", u + d, v, w, d), ("bottom", u + d + w, v, w, d),
                     ("right" if not flip else "left", u, v + d, d, h), ("front", u + d, v + d, w, h),
                     ("left" if not flip else "right", u + d + w, v + d, d, h), ("back", u + 2 * d + w, v + d, w, h)]
            for face, x0, y0, fw, fh in faces:
                if fw > 0 and fh > 0:
                    paint_face(cv, c["mat"], face, x0, y0, fw, fh, flip)
    os.makedirs(RES, exist_ok=True)
    cv.base.save(os.path.join(RES, model["name"] + ".png"))
    cv.glow.save(os.path.join(RES, model["name"] + "_glow.png"))


def f(v):
    s = repr(float(v))
    return s + "F"


def java(models):
    out = ["package com.krolasyon.valdoria.client.model;", "",
           "import com.krolasyon.valdoria.ValdoriaMod;",
           "import net.minecraft.client.model.geom.ModelLayerLocation;",
           "import net.minecraft.client.model.geom.PartPose;",
           "import net.minecraft.client.model.geom.builders.CubeListBuilder;",
           "import net.minecraft.client.model.geom.builders.LayerDefinition;",
           "import net.minecraft.client.model.geom.builders.MeshDefinition;",
           "import net.minecraft.client.model.geom.builders.PartDefinition;", "",
           "/** GENERATED by tools/gen_entities.py — edit the generator, not this file. */",
           "public final class ValdoriaModels {",
           "    private ValdoriaModels() {}", ""]
    for m in models:
        const = m["name"].upper()
        out.append(f'    public static final ModelLayerLocation {const} = new ModelLayerLocation(ValdoriaMod.id("{m["name"]}"), "main");')
    out.append("")
    for m in models:
        camel = "".join(w.capitalize() for w in m["name"].split("_"))
        out.append(f"    public static LayerDefinition create{camel}() {{")
        out.append("        MeshDefinition mesh = new MeshDefinition();")
        out.append('        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));')
        for p in m["parts"]:
            cb = "CubeListBuilder.create()"
            for c in p["cubes"]:
                u, v = c["uv"]
                o, s = c["o"], c["s"]
                cb += f".texOffs({u}, {v}).addBox({f(o[0])}, {f(o[1])}, {f(o[2])}, {f(s[0])}, {f(s[1])}, {f(s[2])})"
            pv, rt = p["pivot"], p["rot"]
            pose = f"PartPose.offsetAndRotation({f(pv[0])}, {f(pv[1])}, {f(pv[2])}, {f(rt[0])}, {f(rt[1])}, {f(rt[2])})"
            parent = "root" if p["parent"] == "root" else "p_" + p["parent"]
            out.append(f'        PartDefinition p_{p["name"]} = {parent}.addOrReplaceChild("{p["name"]}", {cb}, {pose});')
        out.append(f"        return LayerDefinition.create(mesh, {m['tex'][0]}, {m['tex'][1]});")
        out.append("    }")
        out.append("")
    out.append("}")
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    with open(JAVA, "w") as fh:
        fh.write("\n".join(out) + "\n")


if __name__ == "__main__":
    for i, m in enumerate(MODELS):
        layout(m)
        paint(m, 100 + i)
    java(MODELS)
    print("models:", ", ".join(m["name"] for m in MODELS))
