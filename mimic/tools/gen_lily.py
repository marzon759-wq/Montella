#!/usr/bin/env python3
"""Lily Harrow, sixteen: flannel shirt, jeans, boots, long brown hair, three days in a silo. Geometry, texture, animations."""
import json, math, os, random, sys
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_mimic as gm
from gen_mimic_anims import Pose, ramp, lerp, noise, sm

OUT = os.path.join(HERE, "out")
TEX = 512


def tex():
    atlas = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    P = {}

    def put(name, im, x, y):
        atlas.paste(im, (x, y))
        P[name] = (x, y, im.size[0], im.size[1])

    def ramp_img(w, h, lo, hi, seed, scale=6):
        n = gm.noise(w, h, scale, seed, 3)
        return Image.fromarray(gm.to_rgb(n, lo, hi)).convert("RGBA")

    put("skin", ramp_img(96, 96, (176, 138, 116), (214, 178, 154), 1), 0, 0)
    put("hair", ramp_img(96, 96, (40, 24, 14), (92, 60, 34), 2, 3), 96, 0)
    # flannel: red and black check
    fl = Image.new("RGBA", (128, 128))
    d = ImageDraw.Draw(fl)
    for x in range(0, 128, 16):
        for y in range(0, 128, 16):
            dark = ((x // 16) + (y // 16)) % 2 == 0
            d.rectangle([x, y, x + 15, y + 15], fill=(124, 22, 20, 255) if dark else (160, 40, 34, 255))
            d.rectangle([x, y + 6, x + 15, y + 9], fill=(30, 14, 14, 255))
            d.rectangle([x + 6, y, x + 9, y + 15], fill=(36, 16, 16, 255))
    a = np.asarray(fl, float)
    a[..., :3] *= (0.8 + 0.3 * gm.noise(128, 128, 4, 3, 2))[..., None]
    fl = Image.fromarray(a.clip(0, 255).astype(np.uint8))
    gm.splatter(fl, 8, (70, 4, 4), 4, 2, 7, 180)
    put("flannel", fl, 192, 0)
    den = ramp_img(128, 128, (40, 52, 76), (80, 96, 124), 5, 3)
    gm.splatter(den, 6, (60, 40, 30), 6, 3, 9, 140)
    gm.splatter(den, 5, (70, 4, 4), 7, 2, 6, 170)
    put("denim", den, 320, 0)
    put("boot", ramp_img(96, 96, (34, 24, 18), (70, 50, 36), 8), 0, 96)
    put("tshirt", ramp_img(96, 96, (170, 166, 150), (214, 210, 196), 9), 96, 96)
    # the face: dirty, tear tracks, a cut on the brow
    FW, FH = 64, 64
    face = ramp_img(FW, FH, (176, 138, 116), (212, 176, 150), 10, 4)
    d = ImageDraw.Draw(face)
    for cx in (20, 44):
        d.ellipse([cx - 6, 25, cx + 6, 33], fill=(236, 232, 226, 255))
        d.ellipse([cx - 3, 26, cx + 3, 32], fill=(70, 96, 70, 255))
        d.ellipse([cx - 1, 28, cx + 1, 30], fill=(10, 10, 10, 255))
        d.line([(cx - 7, 22), (cx + 6, 21)], fill=(60, 36, 20, 255), width=2)
        d.line([(cx, 34), (cx - 1, 46)], fill=(150, 120, 104, 255), width=1)  # tear tracks through the dirt
    d.line([(30, 40), (34, 40)], fill=(150, 110, 90, 255))
    d.line([(25, 50), (39, 50)], fill=(130, 70, 66, 255), width=2)
    d.line([(46, 14), (52, 20)], fill=(120, 10, 10, 255), width=2)
    gm.splatter(face, 6, (60, 44, 30), 11, 1, 4, 120)
    put("face", face, 192, 128)
    put("blood", ramp_img(64, 64, (60, 0, 2), (120, 10, 12), 12, 3), 256, 128)
    return atlas, P


def build(P):
    gm.S = 8
    r = gm.Rig(P)
    r.bone("root", None, (0, 0, 0))
    r.bone("all", "root", (0, 0, 0))
    r.bone("body", "all", (0, 13, 0))
    r.cube("body", (-3.5, 13, -1.8), (7, 10, 3.6), "flannel")
    r.cube("body", (-2.0, 19.5, -2.0), (4, 3.6, 0.4), "tshirt")
    r.cube("body", (-3.7, 12.6, -2.0), (7.4, 2.4, 4.0), "flannel")  # untucked hem
    r.bone("head", "body", (0, 23, 0))
    r.cube("head", (-3.4, 23, -3.4), (6.8, 7.4, 6.8), "skin", faces={"north": ("face",), "up": "hair", "south": "hair"})
    # long hair: a cap, side curtains, strands down the back
    r.cube("head", (-3.7, 28.4, -3.7), (7.4, 2.4, 7.4), "hair")
    r.cube("head", (-3.9, 22.0, -2.6), (0.8, 7.2, 6.2), "hair")
    r.cube("head", (3.1, 22.0, -2.6), (0.8, 7.2, 6.2), "hair")
    r.cube("head", (-3.6, 17.0, 2.9), (7.2, 11.0, 1.2), "hair")
    r.cube("head", (-3.2, 29.0, -3.9), (6.4, 1.2, 0.6), "hair")
    rr = random.Random(3)
    for i in range(14):
        x = rr.uniform(-3.4, 3.0)
        ln = rr.uniform(7, 12)
        r.cube("head", (x, 28.5 - ln, 3.4 + rr.uniform(0, 0.6)), (0.6, ln, 0.6), "hair", rot=(rr.uniform(-4, 10), 0, rr.uniform(-6, 6)), pivot=(x, 28.5, 3.6))
    for side, sg in (("l", 1), ("r", -1)):
        a = f"arm_{side}"
        f = f"fore_{side}"
        r.bone(a, "body", (sg * 4.6, 22, 0))
        r.cube(a, (sg * 4.6 - 1.3, 16.5, -1.3), (2.6, 6.0, 2.6), "flannel")
        r.bone(f, a, (sg * 4.6, 16.5, 0))
        r.cube(f, (sg * 4.6 - 1.2, 11.5, -1.2), (2.4, 5.2, 2.4), "flannel")
        r.cube(f, (sg * 4.6 - 1.0, 9.6, -1.0), (2.0, 2.0, 2.0), "skin")  # hand
        l = f"leg_{side}"
        s = f"shin_{side}"
        r.bone(l, "all", (sg * 1.8, 13, 0))
        r.cube(l, (sg * 1.8 - 1.7, 6.5, -1.7), (3.4, 6.6, 3.4), "denim")
        r.bone(s, l, (sg * 1.8, 6.5, 0))
        r.cube(s, (sg * 1.8 - 1.6, 1.6, -1.6), (3.2, 5.0, 3.2), "denim")
        r.cube(s, (sg * 1.8 - 1.7, 0, -2.4), (3.4, 2.4, 4.2), "boot")
    j = r.json()
    j["minecraft:geometry"][0]["description"].update({"identifier": "geometry.lily", "texture_width": TEX, "texture_height": TEX,
                                                      "visible_bounds_width": 3, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1, 0]})
    return j


SIDES = (("l", 1), ("r", -1))
ANIMS = {}


def anim(name, length, loop=True):
    def deco(fn):
        ANIMS[name] = (fn, length, loop)
        return fn
    return deco


def breathe(P, t, rate=0.9, amt=1.0):
    b = math.sin(t * math.tau * rate)
    P.r("body", -1.5 * b * amt).p("body", 0, 0.12 * b * amt, 0)
    return b


@anim("idle", 4.0)
def idle(t):
    """hugging herself, glancing around"""
    P = Pose()
    breathe(P, t, 0.5, 1.4)
    P.r("body", 6).r("head", 8 + noise(t, 1, 0.6) * 6, noise(t, 2, 0.5) * 30, noise(t, 3, 0.4) * 6)
    for side, sg in SIDES:
        P.r(f"arm_{side}", -30, 0, -sg * 10).r(f"fore_{side}", -85, -sg * 55, 0)
    return P


@anim("walk", 1.0)
def walk(t):
    P = Pose()
    ph = t * math.tau
    for side, sg in SIDES:
        s = math.sin(ph + (0 if side == "l" else math.pi))
        P.r(f"leg_{side}", -s * 28).r(f"shin_{side}", max(0, s) * 30)
        P.r(f"arm_{side}", s * 24, 0, sg * 4).r(f"fore_{side}", -14)
    P.p("all", 0, abs(math.sin(ph)) * 0.5, 0)
    P.r("body", 6).r("head", -4, math.sin(ph * 0.5) * 14)
    return P


@anim("run", 0.6)
def run(t):
    P = Pose()
    ph = t / 0.6 * math.tau
    for side, sg in SIDES:
        s = math.sin(ph + (0 if side == "l" else math.pi))
        P.r(f"leg_{side}", -s * 50).r(f"shin_{side}", max(0, s) * 70 + 10)
        P.r(f"arm_{side}", s * 50, 0, sg * 6).r(f"fore_{side}", -60)
    P.p("all", 0, abs(math.sin(ph)) * 1.0, 0)
    P.r("body", 16).r("head", -10, math.sin(ph * 0.5) * 10)
    return P


@anim("sit", 4.0)
def sit(t):
    """huddled on the floor of the silo, knees up, arms around them"""
    P = Pose()
    b = breathe(P, t, 0.6, 1.2)
    P.p("all", 0, -9, 0)
    P.r("body", 22 + b).r("head", 20, noise(t, 1, 0.3) * 10, 0)
    for side, sg in SIDES:
        P.r(f"leg_{side}", -100, sg * 6, 0).r(f"shin_{side}", 120)
        P.r(f"arm_{side}", -70, 0, -sg * 16).r(f"fore_{side}", -40, sg * 30, 0)
    return P


@anim("dragged", 2.0)
def dragged(t):
    """on her back in the corn, being pulled by the ankles, clawing at the dirt"""
    P = Pose()
    P.p("all", 0, 1.5, 0).r("all", -88, 0, 0)
    s = math.sin(t * math.tau * 1.5)
    P.r("head", -30 + s * 10, s * 25, 0)
    for side, sg in SIDES:
        P.r(f"leg_{side}", 10, 0, sg * 8)
        P.r(f"arm_{side}", -160 + math.sin(t * 6 + sg) * 30, 0, sg * (30 + s * 10)).r(f"fore_{side}", -30 + math.sin(t * 7 + sg) * 20)
    return P


@anim("scream", 1.5, loop=False)
def scream(t):
    P = Pose()
    k = ramp(t, 0, 0.15) * (1 - ramp(t, 1.2, 1.5))
    P.r("body", -10 * k).r("head", -20 * k, math.sin(t * 40) * 3 * k)
    for side, sg in SIDES:
        P.r(f"arm_{side}", -120 * k, 0, sg * 30 * k).r(f"fore_{side}", -60 * k)
    return P


@anim("hug", 2.5, loop=False)
def hug(t):
    P = Pose()
    k = ramp(t, 0, 0.4) * (1 - ramp(t, 2.0, 2.5))
    P.r("body", 10 * k).r("head", 12 * k, 0, 10 * k)
    for side, sg in SIDES:
        P.r(f"arm_{side}", -80 * k, -sg * 30 * k, 0).r(f"fore_{side}", -40 * k, sg * 40 * k, 0)
    return P


@anim("point", 2.0, loop=False)
def point(t):
    P = idle(t)
    k = ramp(t, 0, 0.3) * (1 - ramp(t, 1.6, 2.0))
    P.r("arm_r", -80 * k, 0, 0).r("fore_r", 60 * k, -30 * k, 0)
    return P


def anims_json():
    out = {"format_version": "1.8.0", "animations": {}}
    for name, (fn, length, loop) in ANIMS.items():
        frames = int(round(length / 0.05))
        bones = {}
        for i in range(frames + 1):
            t = i * 0.05
            P = fn(t if i < frames or not loop else 0.0)
            for bone, (rv, pv) in P.items():
                b = bones.setdefault(bone, {})
                if rv is not None:
                    b.setdefault("rotation", {})[f"{t:.2f}"] = [round(v, 2) for v in rv]
                if pv is not None:
                    b.setdefault("position", {})[f"{t:.2f}"] = [round(v, 3) for v in pv]
        a = {"animation_length": length, "bones": bones}
        if loop:
            a["loop"] = True
        out["animations"][f"animation.lily.{name}"] = a
    return out


if __name__ == "__main__":
    atlas, P = tex()
    geo = build(P)
    A = os.path.join(HERE, "../common/src/main/resources/assets/mimicry")
    atlas.save(os.path.join(A, "textures/entity/lily.png"))
    atlas.save(os.path.join(OUT, "lily.png"))
    for p in (os.path.join(A, "geo/entity/lily.geo.json"), os.path.join(OUT, "lily.geo.json")):
        json.dump(geo, open(p, "w"), separators=(",", ":"))
    an = anims_json()
    for p in (os.path.join(A, "animations/entity/lily.animation.json"), os.path.join(OUT, "lily.animation.json")):
        json.dump(an, open(p, "w"), separators=(",", ":"))
    print("lily ok", len(ANIMS))
