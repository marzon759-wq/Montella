#!/usr/bin/env python3
"""Dad's 1979 pickup: faded red paint, rust, chrome, a cracked windshield. GeckoLib geometry, textures (headlights off/on), wheel animation."""
import json, os, sys
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_mimic as gm

TEX = 256
A = os.path.join(HERE, "../common/src/main/resources/assets/mimicry")


def textures():
    P = {}
    base = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    glow_on = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))

    def put(name, im, x, y):
        base.paste(im, (x, y))
        P[name] = (x, y, im.size[0], im.size[1])

    def rimg(w, h, lo, hi, seed, scale=6):
        return Image.fromarray(gm.to_rgb(gm.noise(w, h, scale, seed, 3), lo, hi)).convert("RGBA")

    paint = rimg(96, 96, (92, 26, 22), (138, 44, 36), 1)
    gm.splatter(paint, 14, (96, 56, 30), 2, 2, 8, 200)   # rust
    gm.splatter(paint, 10, (60, 50, 40), 3, 2, 6, 150)   # mud
    put("paint", paint, 0, 0)
    put("chrome", rimg(64, 64, (120, 120, 120), (200, 200, 196), 4, 3), 96, 0)
    put("tire", rimg(64, 64, (14, 14, 14), (40, 38, 36), 5, 2), 160, 0)
    put("glass", rimg(64, 64, (16, 20, 22), (46, 54, 56), 6, 4), 96, 64)
    put("dark", rimg(64, 64, (10, 9, 9), (28, 26, 24), 7, 3), 160, 64)
    put("rust", rimg(64, 64, (60, 32, 20), (128, 70, 36), 8, 3), 0, 96)
    put("bed", rimg(64, 64, (56, 44, 32), (96, 78, 58), 9, 3), 64, 128)
    lamp_off = rimg(32, 32, (120, 116, 96), (170, 166, 140), 10, 3)
    put("lamp", lamp_off, 128, 128)
    put("tail", rimg(32, 32, (90, 10, 10), (130, 20, 18), 11, 3), 160, 128)
    lit = base.copy()
    lamp_on = rimg(32, 32, (250, 240, 200), (255, 255, 240), 12, 3)
    lit.paste(lamp_on, (128, 128))
    tail_on = rimg(32, 32, (240, 30, 20), (255, 80, 60), 13, 3)
    lit.paste(tail_on, (160, 128))
    glow_on.paste(lamp_on, (128, 128))
    glow_on.paste(tail_on, (160, 128))
    return base, lit, glow_on, P


def build(P):
    gm.S = 4
    r = gm.Rig(P)
    r.bone("root", None, (0, 0, 0))
    r.bone("body", "root", (0, 8, 0))
    W = 15  # half width
    # chassis and floor
    r.cube("body", (-W + 1, 5, -38), (2 * W - 2, 3, 76), "dark")
    # hood and front fenders (front is -z)
    r.cube("body", (-W, 8, -40), (2 * W, 9, 26), "paint")
    r.cube("body", (-W + 2, 17, -38), (2 * W - 4, 1, 22), "paint")
    r.cube("body", (-W - 1.5, 6, -40), (3, 7, 14), "paint")
    r.cube("body", (W - 1.5, 6, -40), (3, 7, 14), "paint")
    # grille, lamps, bumper
    r.cube("body", (-10, 9, -40.6), (20, 7, 0.6), "chrome")
    for x in (-13, 9):
        r.cube("body", (x, 11, -40.8), (4, 4, 0.8), "lamp")
    r.cube("body", (-W - 1, 5, -42), (2 * W + 2, 3, 2), "chrome")
    # cab
    r.cube("body", (-W, 8, -14), (2 * W, 12, 18), "paint", faces={"north": "paint"})
    r.cube("body", (-W + 1, 20, -13), (2 * W - 2, 10, 15), "paint")
    r.cube("body", (-W + 2, 20, -14.2), (2 * W - 4, 9, 1.2), "glass")       # windshield
    r.cube("body", (-W + 2, 21, 1.6), (2 * W - 4, 7, 0.6), "glass")         # rear window
    r.cube("body", (-W - 0.2, 21, -11), (0.4, 7, 11), "glass")             # side windows
    r.cube("body", (W - 0.2, 21, -11), (0.4, 7, 11), "glass")
    r.cube("body", (-W + 1, 30, -13.5), (2 * W - 2, 1, 16), "paint")        # roof
    r.cube("body", (-W - 1.6, 21, -14), (1.2, 2, 2), "chrome")              # mirrors
    r.cube("body", (W + 0.4, 21, -14), (1.2, 2, 2), "chrome")
    # the bed
    r.cube("body", (-W, 8, 4), (2 * W, 2, 34), "bed")
    r.cube("body", (-W, 10, 4), (2, 9, 34), "paint")
    r.cube("body", (W - 2, 10, 4), (2, 9, 34), "paint")
    r.cube("body", (-W, 10, 36), (2 * W, 9, 2), "paint")
    r.cube("body", (-W - 1.5, 6, 18), (3, 7, 14), "paint")
    r.cube("body", (W - 1.5, 6, 18), (3, 7, 14), "paint")
    for x in (-W + 0.5, W - 3.5):
        r.cube("body", (x, 13, 38), (3, 4, 0.8), "tail")
    r.cube("body", (-W - 1, 5, 38), (2 * W + 2, 3, 2), "chrome")
    # junk in the bed: a tarp and a jerrycan
    r.cube("body", (-8, 10, 20), (12, 3, 10), "dark")
    r.cube("body", (6, 10, 28), (5, 7, 3), "rust")
    # wheels
    for name, x, z in (("wheel_fl", -W, -31), ("wheel_fr", W, -31), ("wheel_rl", -W, 25), ("wheel_rr", W, 25)):
        r.bone(name, "root", (x, 6, z))
        side = -1 if x < 0 else 1
        r.cube(name, (x - (3 if side > 0 else 0) + (0 if side > 0 else 0), 0, z - 6), (3, 12, 12), "tire")
        r.cube(name, (x - (3.4 if side > 0 else -0.4) + (0 if side > 0 else 0), 3, z - 3), (0.4 if side < 0 else 0.4, 6, 6), "chrome")
    j = r.json()
    j["minecraft:geometry"][0]["description"].update({"identifier": "geometry.truck", "texture_width": TEX, "texture_height": TEX,
                                                      "visible_bounds_width": 7, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]})
    return j


def anims():
    out = {"format_version": "1.8.0", "animations": {}}
    rot = {}
    for i in range(21):
        t = i * 0.05
        rot[f"{t:.2f}"] = [round(-360 * t, 1), 0, 0]
    drive = {"animation_length": 1.0, "loop": True, "bones": {w: {"rotation": rot} for w in ("wheel_fl", "wheel_fr", "wheel_rl", "wheel_rr")}}
    drive["bones"]["body"] = {"position": {f"{i * 0.05:.2f}": [0, round(0.15 * np.sin(i * 0.05 * 37), 3), 0] for i in range(21)}}
    out["animations"]["animation.truck.drive"] = drive
    out["animations"]["animation.truck.idle"] = {"animation_length": 1.0, "loop": True, "bones": {"body": {"position": [0, 0, 0]}}}
    return out


if __name__ == "__main__":
    base, lit, glow, P = textures()
    geo = build(P)
    os.makedirs(os.path.join(A, "geo/entity"), exist_ok=True)
    base.save(os.path.join(A, "textures/entity/truck.png"))
    lit.save(os.path.join(A, "textures/entity/truck_lit.png"))
    glow.save(os.path.join(A, "textures/entity/truck_lit_glowmask.png"))
    gm_ = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    gm_.putpixel((TEX - 1, TEX - 1), (255, 255, 255, 255))  # GeckoLib wants at least one glowing pixel; this one is never mapped
    gm_.save(os.path.join(A, "textures/entity/truck_glowmask.png"))
    json.dump(geo, open(os.path.join(A, "geo/entity/truck.geo.json"), "w"), separators=(",", ":"))
    json.dump(anims(), open(os.path.join(A, "animations/entity/truck.animation.json"), "w"), separators=(",", ":"))
    base.save(os.path.join(HERE, "out/truck.png"))
    json.dump(geo, open(os.path.join(HERE, "out/truck.geo.json"), "w"), separators=(",", ":"))
    print("truck ok")
