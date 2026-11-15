#!/usr/bin/env python3
"""Generates the creature models of Valdoria for GeckoLib: geometry (.geo.json), animations and base + glowmask textures.

Run from valdoria-1.20.1/:  python3 tools/gen_entities.py
"""
import json
import math
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "src", "main", "resources", "assets", "valdoria", "textures", "entity")
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "valdoria")
GEO = os.path.join(ASSETS, "geo", "entity")
ANIM = os.path.join(ASSETS, "animations", "entity")


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
    cv.glow.save(os.path.join(RES, model["name"] + "_glowmask.png"))


# ---------------------------------------------------------------------------------------------------------------
# GeckoLib geometry. Parts are described Java-style (y down, feet at y=24, offsets relative to the parent);
# Bedrock wants absolute, y-up coordinates. Rotations keep their sign (GeckoLib mirrors x and negates x/y rotations,
# which together match the Java renderer's own flip), converted to degrees.
# ---------------------------------------------------------------------------------------------------------------

def r3(v):
    return [round(float(x), 4) for x in v]


def geo(model):
    by = {p["name"]: p for p in model["parts"]}

    def abs_pivot(p):
        x, y, z = p["pivot"]
        if p["parent"] != "root":
            px, py, pz = abs_pivot(by[p["parent"]])
            return (x + px, y + py, z + pz)
        return (x, y + 24, z)

    bones = [{"name": "root", "pivot": [0, 0, 0]}]
    for p in model["parts"]:
        P = abs_pivot(p)
        b = {"name": p["name"], "parent": p["parent"], "pivot": r3([P[0], 24 - P[1], P[2]])}
        if any(p["rot"]):
            b["rotation"] = r3([math.degrees(v) for v in p["rot"]])
        cubes = []
        for c in p["cubes"]:
            o, sz = c["o"], c["s"]
            cubes.append({"origin": r3([P[0] + o[0], 24 - (P[1] + o[1] + sz[1]), P[2] + o[2]]), "size": r3(sz), "uv": list(c["uv"])})
        if cubes:
            b["cubes"] = cubes
        bones.append(b)
    W, H = model["tex"]
    out = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry." + model["name"], "texture_width": W, "texture_height": H,
                        "visible_bounds_width": 4, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]},
        "bones": bones}]}
    os.makedirs(GEO, exist_ok=True)
    with open(os.path.join(GEO, model["name"] + ".geo.json"), "w") as fh:
        json.dump(out, fh, separators=(",", ":"))


# ---------------------------------------------------------------------------------------------------------------
# Animations: sampled from functions of time into dense linear keyframes. Rotation values are degree offsets added
# to the rest pose (same sign convention as the parts above); position y is up.
# ---------------------------------------------------------------------------------------------------------------

sin, cos, PI, TAU = math.sin, math.cos, math.pi, 2 * math.pi


class Anim:
    def __init__(self, length, loop=True, step=0.05):
        self.length, self.loop, self.step = length, loop, step
        self.bones = {}

    def fn(self, bone, chan, f):
        n = max(1, int(round(self.length / self.step)))
        keys = {}
        for i in range(n + 1):
            t = min(self.length, i * self.step)
            keys[f"{t:.4f}".rstrip("0").rstrip(".") if t else "0.0"] = r3(f(t))
        self.bones.setdefault(bone, {})[chan] = keys

    def rot(self, bone, f):
        self.fn(bone, "rotation", f)

    def pos(self, bone, f):
        self.fn(bone, "position", f)

    def json(self):
        d = {"animation_length": self.length, "bones": self.bones}
        d["loop"] = True if self.loop is True else self.loop
        return d


def pulse(t, a, b, c, d):
    def sm(x, lo, hi):
        if hi == lo:
            return 1.0 if x >= hi else 0.0
        u = max(0.0, min(1.0, (x - lo) / (hi - lo)))
        return u * u * (3 - 2 * u)
    return sm(t, a, b) * (1 - sm(t, c, d))


def veilstag_anims():
    A = {}
    a = Anim(6.0)
    w = TAU / 6.0
    a.rot("neck", lambda t: [2 * sin(w * t), 0, 0])
    a.pos("body", lambda t: [0, 0.15 * sin(2 * w * t), 0])
    a.rot("ear_l", lambda t: [0, 0, 4 * sin(w * t) - 28 * pulse(t, 2.0, 2.1, 2.2, 2.4)])
    a.rot("ear_r", lambda t: [0, 0, -4 * sin(w * t + 1.3) + 28 * pulse(t, 4.4, 4.5, 4.6, 4.8)])
    a.rot("tail", lambda t: [5 * sin(2 * w * t), 0, 0])
    for s, ph in (("l", 0), ("r", 2.0)):
        a.rot("moss_" + s, lambda t, ph=ph: [8 * sin(w * t + ph), 0, 0])
        a.rot("moss_" + s + "2", lambda t, ph=ph: [6 * sin(w * t + ph + 1), 0, 0])
    A["idle"] = a

    for name, L, amp, bob in (("walk", 1.2, 34, 0.6), ("run", 0.62, 52, 1.2)):
        a = Anim(L)
        w = TAU / L
        run = name == "run"
        if run:
            a.rot("leg_fl", lambda t: [amp * sin(w * t), 0, 0])
            a.rot("leg_fr", lambda t: [amp * sin(w * t + 0.5), 0, 0])
            a.rot("leg_bl", lambda t: [amp * sin(w * t + PI), 0, 0])
            a.rot("leg_br", lambda t: [amp * sin(w * t + PI + 0.5), 0, 0])
            a.rot("body", lambda t: [4 * sin(w * t), 0, 0])
            a.rot("neck", lambda t: [-12 + 4 * sin(w * t + 1), 0, 0])
            a.rot("tail", lambda t: [-55, 0, 0])
            a.rot("ear_l", lambda t: [0, 0, 25])
            a.rot("ear_r", lambda t: [0, 0, -25])
        else:
            a.rot("leg_fl", lambda t: [amp * sin(w * t), 0, 0])
            a.rot("leg_br", lambda t: [amp * sin(w * t), 0, 0])
            a.rot("leg_fr", lambda t: [-amp * sin(w * t), 0, 0])
            a.rot("leg_bl", lambda t: [-amp * sin(w * t), 0, 0])
            a.rot("neck", lambda t: [3 * sin(2 * w * t), 0, 0])
            a.rot("tail", lambda t: [-12 + 6 * sin(2 * w * t), 0, 0])
        a.pos("body", lambda t: [0, bob * abs(sin(w * t)), 0])
        for s, ph in (("l", 0), ("r", 1.5)):
            a.rot("moss_" + s, lambda t, ph=ph: [18 + 10 * sin(w * t + ph), 0, 0])
            a.rot("moss_" + s + "2", lambda t, ph=ph: [14 + 8 * sin(w * t + ph + 1), 0, 0])
        A[name] = a

    a = Anim(2.0)
    w = TAU / 2.0
    a.rot("neck", lambda t: [76 + 2 * sin(w * t), 0, 0])
    a.rot("head", lambda t: [16 + 5 * max(0.0, sin(2 * w * t)), 0, 0])
    a.rot("tail", lambda t: [6 * sin(2 * w * t), 0, 0])
    a.rot("ear_l", lambda t: [0, 0, 10])
    a.rot("ear_r", lambda t: [0, 0, -10])
    A["graze"] = a

    a = Anim(2.0)
    w = TAU / 2.0
    a.rot("neck", lambda t: [-16, 6 * sin(w * t), 0])
    a.rot("head", lambda t: [8, 10 * sin(w * t + 0.5), 0])
    a.rot("tail", lambda t: [-60, 0, 0])
    a.rot("ear_l", lambda t: [0, 0, -22])
    a.rot("ear_r", lambda t: [0, 0, 22])
    A["alert"] = a

    a = Anim(0.7, loop=False)
    a.rot("neck", lambda t: [55 * pulse(t, 0.0, 0.2, 0.35, 0.7), 0, 0])
    a.rot("head", lambda t: [20 * pulse(t, 0.0, 0.2, 0.35, 0.7), 0, 0])
    a.pos("body", lambda t: [0, 0, -2 * pulse(t, 0.05, 0.25, 0.35, 0.7)])
    A["attack"] = a
    return A


def gloomlynx_anims():
    A = {}
    a = Anim(4.0)
    w = TAU / 4.0
    a.rot("tail", lambda t: [4 * sin(w * t), 16 * sin(w * t + 0.5), 0])
    a.pos("body", lambda t: [0, 0.12 * sin(2 * w * t), 0])
    a.rot("ear_l", lambda t: [0, 0, -18 * pulse(t, 1.4, 1.45, 1.55, 1.7)])
    a.rot("head", lambda t: [2 * sin(w * t), 6 * sin(w * t * 0.5), 0])
    A["idle"] = a

    a = Anim(1.0)
    w = TAU / 1.0
    a.rot("leg_fl", lambda t: [32 * sin(w * t), 0, 0])
    a.rot("leg_br", lambda t: [32 * sin(w * t), 0, 0])
    a.rot("leg_fr", lambda t: [-32 * sin(w * t), 0, 0])
    a.rot("leg_bl", lambda t: [-32 * sin(w * t), 0, 0])
    a.rot("tail", lambda t: [18, 10 * sin(w * t), 0])
    a.rot("head", lambda t: [2 * sin(2 * w * t), 0, 0])
    a.pos("body", lambda t: [0, 0.3 * abs(sin(w * t)), 0])
    A["walk"] = a

    a = Anim(0.5)
    w = TAU / 0.5
    a.rot("leg_fl", lambda t: [58 * sin(w * t), 0, 0])
    a.rot("leg_fr", lambda t: [58 * sin(w * t + 0.4), 0, 0])
    a.rot("leg_bl", lambda t: [58 * sin(w * t + PI), 0, 0])
    a.rot("leg_br", lambda t: [58 * sin(w * t + PI + 0.4), 0, 0])
    a.rot("body", lambda t: [6 * sin(w * t), 0, 0])
    a.pos("body", lambda t: [0, 0.8 * abs(sin(w * t)), 0])
    a.rot("tail", lambda t: [32 + 6 * sin(w * t), 0, 0])
    a.rot("ear_l", lambda t: [0, 0, 22])
    a.rot("ear_r", lambda t: [0, 0, -22])
    A["run"] = a

    a = Anim(4.0)
    w = TAU / 4.0
    a.pos("body", lambda t: [0, -5.5 + 0.15 * sin(w * t), 0])
    a.rot("head", lambda t: [20, 34, 0])
    a.pos("head", lambda t: [0, -1, 0])
    a.rot("tail", lambda t: [-6, 108 + 3 * sin(w * t), 0])
    for leg, x in (("leg_fl", -86), ("leg_fr", -86), ("leg_bl", 86), ("leg_br", 86)):
        a.rot(leg, lambda t, x=x: [x, 0, 0])
        a.pos(leg, lambda t: [0, -5, 0])
    a.rot("ear_l", lambda t: [0, 0, -17])
    a.rot("ear_r", lambda t: [0, 0, 17])
    A["sleep"] = a

    a = Anim(1.6)
    w = TAU / 1.6
    a.pos("body", lambda t: [0, -1.6, 0])
    a.rot("head", lambda t: [8, 0, 0])
    a.rot("ear_l", lambda t: [0, 0, 30])
    a.rot("ear_r", lambda t: [0, 0, -30])
    a.rot("tail", lambda t: [-10, 8 * sin(2 * w * t), 0])
    a.rot("leg_fl", lambda t: [10 * sin(w * t), 0, 0])
    a.rot("leg_fr", lambda t: [-10 * sin(w * t), 0, 0])
    A["stalk"] = a

    a = Anim(0.5, loop=False)
    a.rot("body", lambda t: [-16 * pulse(t, 0, 0.15, 0.25, 0.5), 0, 0])
    a.rot("leg_fl", lambda t: [-70 * pulse(t, 0, 0.15, 0.25, 0.5), 0, 0])
    a.rot("leg_fr", lambda t: [-60 * pulse(t, 0.03, 0.18, 0.28, 0.5), 0, 0])
    a.rot("head", lambda t: [-15 * pulse(t, 0, 0.15, 0.25, 0.5), 0, 0])
    A["attack"] = a
    return A


def vinestalker_anims():
    A = {}
    tendrils = ("tendril_1", "tendril_2", "tendril_3", "tendril_4")
    a = Anim(5.0)
    w = TAU / 5.0
    for i, tb in enumerate(tendrils):
        a.rot(tb, lambda t, i=i: [10 * sin(w * t + i * 1.7), 0, 6 * cos(w * t + i)])
    a.rot("abdomen", lambda t: [3 * sin(w * t), 0, 0])
    a.rot("mandible_l", lambda t: [0, -11 - 3 * sin(2 * w * t), 0])
    a.rot("mandible_r", lambda t: [0, 11 + 3 * sin(2 * w * t), 0])
    A["idle"] = a

    a = Anim(0.8)
    w = TAU / 0.8
    for i in range(3):
        for side, sg, off in (("l", -1, 0), ("r", 1, PI)):
            ph = i * 2.1 + off
            a.rot(f"leg{i}_{side}", lambda t, ph=ph, sg=sg: [0, 20 * cos(w * t + ph), sg * 26 * max(0.0, sin(w * t + ph))])
    for i, tb in enumerate(tendrils):
        a.rot(tb, lambda t, i=i: [26 + 8 * sin(w * t + i), 0, 0])
    a.pos("body", lambda t: [0, 0.3 * sin(2 * w * t), 0])
    A["walk"] = a

    a = Anim(4.0)
    w = TAU / 4.0
    for i in range(3):
        a.rot(f"leg{i}_l", lambda t: [0, 0, 14])
        a.rot(f"leg{i}_r", lambda t: [0, 0, -14])
        a.rot(f"shin{i}_l", lambda t: [0, 0, -20])
        a.rot(f"shin{i}_r", lambda t: [0, 0, 20])
    for i, tb in enumerate(tendrils):
        a.rot(tb, lambda t, i=i: [180 + 7 * sin(w * t + i * 1.3), 0, 0])
    a.rot("mandible_l", lambda t: [0, -8 - 2 * sin(w * t), 0])
    a.rot("mandible_r", lambda t: [0, 8 + 2 * sin(w * t), 0])
    A["hang"] = a

    a = Anim(0.55, loop=False)
    a.rot("body", lambda t: [-22 * pulse(t, 0, 0.15, 0.25, 0.55), 0, 0])
    a.rot("leg0_l", lambda t: [0, 0, -40 * pulse(t, 0, 0.15, 0.25, 0.55)])
    a.rot("leg0_r", lambda t: [0, 0, 40 * pulse(t, 0, 0.15, 0.25, 0.55)])
    a.rot("mandible_l", lambda t: [0, -30 * pulse(t, 0, 0.1, 0.3, 0.55), 0])
    a.rot("mandible_r", lambda t: [0, 30 * pulse(t, 0, 0.1, 0.3, 0.55), 0])
    A["attack"] = a
    return A


def lumen_moth_anims():
    A = {}
    a = Anim(0.25, step=0.025)
    w = TAU / 0.25
    a.rot("wing_l", lambda t: [0, 0, -(49 * sin(w * t) - 9)])
    a.rot("wing_r", lambda t: [0, 0, 49 * sin(w * t) - 9])
    a.rot("hindwing_l", lambda t: [0, -9, -(39 * sin(w * t - 0.3) - 7)])
    a.rot("hindwing_r", lambda t: [0, 9, 39 * sin(w * t - 0.3) - 7])
    a.pos("body", lambda t: [0, 0.5 * sin(w * t), 0])
    a.rot("body", lambda t: [9, 0, 0])
    a.rot("antenna_l", lambda t: [5 * sin(w * t), 0, 0])
    a.rot("antenna_r", lambda t: [5 * sin(w * t + 1), 0, 0])
    A["fly"] = a
    return A


ANIMS = {"veilstag": veilstag_anims, "gloomlynx": gloomlynx_anims, "vinestalker": vinestalker_anims, "lumen_moth": lumen_moth_anims}


def write_anims(name):
    out = {"format_version": "1.8.0", "animations": {f"animation.{name}.{k}": v.json() for k, v in ANIMS[name]().items()}}
    os.makedirs(ANIM, exist_ok=True)
    with open(os.path.join(ANIM, name + ".animation.json"), "w") as fh:
        json.dump(out, fh, separators=(",", ":"))


if __name__ == "__main__":
    for i, m in enumerate(MODELS):
        layout(m)
        paint(m, 100 + i)
        geo(m)
        write_anims(m["name"])
    print("models:", ", ".join(m["name"] for m in MODELS))
