#!/usr/bin/env python3
"""Generates GeckoLib geometry, textures and animations for the Hatman and the Masked Guests."""
import json, math, os, random, sys
from PIL import Image, ImageDraw

OUT = sys.argv[1] if len(sys.argv) > 1 else "common/src/main/resources/assets/hatman"
S = 4  # texels per model pixel
random.seed(1962)


def clamp(v, a=0, b=255):
    return max(a, min(b, int(v)))


class Tex:
    def __init__(self, size):
        self.size = size
        self.img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        self.x = 1
        self.y = 1
        self.rowh = 0

    def alloc(self, w, h):
        w = max(1, int(math.ceil(w)))
        h = max(1, int(math.ceil(h)))
        if self.x + w + 1 > self.size:
            self.x = 1
            self.y += self.rowh + 1
            self.rowh = 0
        if self.y + h + 1 > self.size:
            raise RuntimeError("texture full")
        r = (self.x, self.y, w, h)
        self.x += w + 1
        self.rowh = max(self.rowh, h)
        return r


def noise_fill(img, rect, base, amp=6, seed=None):
    x0, y0, w, h = rect
    rnd = random.Random(seed if seed is not None else (x0 * 7919 + y0))
    px = img.load()
    for y in range(h):
        for x in range(w):
            n = rnd.randint(-amp, amp)
            px[x0 + x, y0 + y] = (clamp(base[0] + n), clamp(base[1] + n), clamp(base[2] + n), 255)


def shade_edges(img, rect, strength=18):
    x0, y0, w, h = rect
    px = img.load()
    for y in range(h):
        for x in range(w):
            d = min(x, y, w - 1 - x, h - 1 - y)
            if d == 0:
                r, g, b, a = px[x0 + x, y0 + y]
                px[x0 + x, y0 + y] = (clamp(r - strength), clamp(g - strength), clamp(b - strength), a)


def vgrad(img, rect, top=10, bottom=-14):
    x0, y0, w, h = rect
    px = img.load()
    for y in range(h):
        t = y / max(1, h - 1)
        k = top + (bottom - top) * t
        for x in range(w):
            r, g, b, a = px[x0 + x, y0 + y]
            px[x0 + x, y0 + y] = (clamp(r + k), clamp(g + k), clamp(b + k), a)


SUIT = (22, 22, 27)
SUIT2 = (30, 30, 36)
SKIN = (196, 190, 178)
HAT = (14, 13, 15)
BAND = (96, 16, 18)
SHOE = (9, 9, 10)
SHIRT = (170, 166, 156)


def paint(tex, glow, rect, mat, face, name):
    img = tex.img
    x0, y0, w, h = rect
    d = ImageDraw.Draw(img)
    if mat == "suit":
        noise_fill(img, rect, SUIT, 3)
        px = img.load()
        if face in ("north", "south", "east", "west"):
            for x in range(0, w, 5):  # pinstripes
                for y in range(h):
                    r, g, b, a = px[x0 + x, y0 + y]
                    px[x0 + x, y0 + y] = (clamp(r + 13), clamp(g + 13), clamp(b + 15), a)
        vgrad(img, rect, 5, -8)
        shade_edges(img, rect, 9)
        if name == "chest" and face == "north":
            cx = x0 + w // 2
            # shirt V and tie
            d.polygon([(cx - 7, y0), (cx + 7, y0), (cx, y0 + h - 3)], fill=SHIRT + (255,))
            d.polygon([(cx - 2, y0), (cx + 2, y0), (cx + 1, y0 + h - 5), (cx, y0 + h - 3), (cx - 1, y0 + h - 5)], fill=(60, 8, 10, 255))
            d.line([(cx - 8, y0), (cx - 1, y0 + h - 2)], fill=(8, 8, 10, 255))
            d.line([(cx + 8, y0), (cx + 1, y0 + h - 2)], fill=(8, 8, 10, 255))
        if name in ("waist", "pelvis") and face == "north":
            cx = x0 + w // 2
            d.line([(cx, y0), (cx, y0 + h - 1)], fill=(8, 8, 10, 255))
            for yy in range(y0 + 3, y0 + h - 1, 7):
                d.rectangle([cx + 2, yy, cx + 3, yy + 1], fill=(70, 66, 60, 255))
    elif mat == "coat":
        noise_fill(img, rect, (17, 17, 21), 3)
        vgrad(img, rect, 4, -10)
        px = img.load()
        rnd = random.Random(x0 + y0 * 31)
        # ragged hem
        for x in range(w):
            cut = rnd.randint(0, 5)
            for y in range(h - cut, h):
                if face in ("north", "south", "east", "west"):
                    px[x0 + x, y0 + y] = (0, 0, 0, 0)
    elif mat == "skin":
        noise_fill(img, rect, SKIN, 5)
        vgrad(img, rect, 6, -16)
        shade_edges(img, rect, 14)
        px = img.load()
        rnd = random.Random(x0 * 3 + y0)
        for _ in range(max(1, w * h // 40)):  # blotches / veins
            bx, by = rnd.randint(0, w - 1), rnd.randint(0, h - 1)
            r, g, b, a = px[x0 + bx, y0 + by]
            px[x0 + bx, y0 + by] = (clamp(r - 26), clamp(g - 30), clamp(b - 24), a)
        if name == "head" and face == "north":
            # hollow sockets: only a suggestion of a face
            ex = w // 4
            ey = h // 2 - 3
            for sx in (ex, w - ex - 1):
                for dy in range(-3, 4):
                    for dx in range(-3, 4):
                        dist = math.hypot(dx, dy * 0.9)
                        if dist < 3.4:
                            k = (3.4 - dist) / 3.4
                            r, g, b, a = px[x0 + sx + dx, y0 + ey + dy]
                            px[x0 + sx + dx, y0 + ey + dy] = (clamp(r - 150 * k), clamp(g - 150 * k), clamp(b - 140 * k), a)
                glow.putpixel((x0 + sx, y0 + ey), (255, 255, 255, 255))
            # stitched grin across the bottom of the head
            gy = y0 + h - 4
            d.line([(x0 + 2, gy - 2), (x0 + 5, gy), (x0 + w - 6, gy), (x0 + w - 3, gy - 2)], fill=(40, 14, 14, 255))
            for sx in range(4, w - 3, 3):
                d.line([(x0 + sx, gy - 2), (x0 + sx, gy + 2)], fill=(30, 22, 20, 255))
        if name == "jaw" and face == "north":
            for sx in range(2, w - 1, 3):
                d.line([(x0 + sx, y0), (x0 + sx, y0 + 2)], fill=(30, 22, 20, 255))
        if name.startswith("finger"):
            for y in range(h * 2 // 3, h):
                for x in range(w):
                    r, g, b, a = px[x0 + x, y0 + y]
                    px[x0 + x, y0 + y] = (clamp(r - 60), clamp(g - 66), clamp(b - 60), a)
    elif mat == "mouth":
        noise_fill(img, rect, (10, 2, 3), 2)
        px = img.load()
        if face == "north":
            for x in range(1, w - 1, 2):
                for y in range(0, 3):
                    px[x0 + x, y0 + y] = (150, 140, 120, 255)
                for y in range(h - 3, h):
                    px[x0 + x, y0 + y] = (130, 120, 100, 255)
    elif mat == "hat":
        noise_fill(img, rect, HAT, 3)
        vgrad(img, rect, 7, -5)
        shade_edges(img, rect, 6)
        if name == "crown" and face in ("north", "south", "east", "west"):
            d.rectangle([x0, y0 + h - 6, x0 + w - 1, y0 + h - 2], fill=BAND + (255,))
            d.line([(x0, y0 + h - 6), (x0 + w - 1, y0 + h - 6)], fill=(130, 30, 30, 255))
    elif mat == "shoe":
        noise_fill(img, rect, SHOE, 2)
        vgrad(img, rect, 16, -4)
    elif mat == "shirt":
        noise_fill(img, rect, SHIRT, 5)
        vgrad(img, rect, 4, -22)
    elif mat.startswith("g_"):
        guest_paint(tex, rect, mat, face, name)


def guest_paint(tex, rect, mat, face, name):
    img = tex.img
    x0, y0, w, h = rect
    d = ImageDraw.Draw(img)
    pal = GUEST_PAL
    if mat == "g_cloth":
        noise_fill(img, rect, pal["cloth"], 5)
        vgrad(img, rect, 6, -14)
        shade_edges(img, rect, 10)
        if name == "g_body" and face == "north":
            cx = x0 + w // 2
            d.polygon([(cx - 5, y0), (cx + 5, y0), (cx, y0 + 14)], fill=pal["shirt"] + (255,))
            d.line([(cx, y0 + 14), (cx, y0 + h - 1)], fill=(10, 10, 10, 255))
        # dust and mould
        px = img.load()
        rnd = random.Random(x0 + y0 * 13)
        for _ in range(w * h // 14):
            bx, by = rnd.randint(0, w - 1), rnd.randint(0, h - 1)
            r, g, b, a = px[x0 + bx, y0 + by]
            px[x0 + bx, y0 + by] = (clamp(r + 16), clamp(g + 18), clamp(b + 12), a)
    elif mat == "g_skin":
        noise_fill(img, rect, (150, 146, 136), 6)
        vgrad(img, rect, 4, -18)
        shade_edges(img, rect, 12)
    elif mat == "g_hair":
        noise_fill(img, rect, pal["hair"], 8)
        shade_edges(img, rect, 10)
    elif mat == "g_mask":
        noise_fill(img, rect, pal["mask"], 4)
        vgrad(img, rect, 10, -20)
        shade_edges(img, rect, 24)
        if face == "north":
            ex = w // 4
            ey = h // 3
            for sx in (ex, w - ex - 1):
                d.ellipse([x0 + sx - 3, y0 + ey - 2, x0 + sx + 3, y0 + ey + 2], fill=(4, 4, 4, 255))
            style = pal["style"]
            my = y0 + h * 2 // 3
            if style == 0:  # smile
                d.arc([x0 + 5, my - 6, x0 + w - 6, my + 4], 20, 160, fill=(20, 8, 8, 255))
            elif style == 1:  # frown
                d.arc([x0 + 5, my, x0 + w - 6, my + 9], 200, 340, fill=(20, 8, 8, 255))
            else:  # none - blank
                d.line([(x0 + 7, my + 1), (x0 + w - 8, my + 1)], fill=(40, 30, 30, 255))
            # cracks
            rnd = random.Random(pal["style"] * 17 + 5)
            cx, cy = x0 + rnd.randint(4, w - 5), y0
            for _ in range(10):
                nx, ny = cx + rnd.randint(-2, 2), cy + rnd.randint(1, 3)
                if ny >= y0 + h:
                    break
                d.line([(cx, cy), (nx, ny)], fill=(60, 50, 44, 255))
                cx, cy = nx, ny
            d.rectangle([x0 + ex - 4, y0 + ey - 6, x0 + ex + 4, y0 + ey - 5], fill=pal["accent"] + (255,))
            d.rectangle([x0 + w - ex - 5, y0 + ey - 6, x0 + w - ex + 3, y0 + ey - 5], fill=pal["accent"] + (255,))


GUEST_PAL = {}


class Model:
    def __init__(self, ident, tex_size, vb=(4, 5, 2)):
        self.ident = ident
        self.tex = Tex(tex_size)
        self.glow = Image.new("RGBA", (tex_size, tex_size), (0, 0, 0, 0))
        self.bones = []
        self.vb = vb
        self.do_paint = True
        self.rects = []

    def bone(self, name, parent, pivot, cubes=()):
        b = {"name": name, "pivot": list(pivot)}
        if parent:
            b["parent"] = parent
        cl = []
        for c in cubes:
            cname, origin, size, mat = c[:4]
            extra = c[4] if len(c) > 4 else {}
            w, h, dd = size
            faces = {"north": (w, h), "south": (w, h), "east": (dd, h), "west": (dd, h), "up": (w, dd), "down": (w, dd)}
            uv = {}
            for f, (fw, fh) in faces.items():
                rect = self.tex.alloc(fw * S, fh * S)
                fmat = extra.get("mat_" + f, mat)
                paint(self.tex, self.glow, rect, fmat, f, cname)
                uv[f] = {"uv": [rect[0], rect[1]], "uv_size": [rect[2], rect[3]]}
            # up/down faces in bedrock are flipped; fine for noise textures
            cube = {"origin": list(origin), "size": list(size), "uv": uv}
            if "inflate" in extra:
                cube["inflate"] = extra["inflate"]
            if "rotation" in extra:
                cube["rotation"] = extra["rotation"]
                cube["pivot"] = extra.get("pivot", [origin[0] + w / 2, origin[1] + h / 2, origin[2] + dd / 2])
            cl.append(cube)
        if cl:
            b["cubes"] = cl
        self.bones.append(b)

    def geo(self):
        return {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry." + self.ident, "texture_width": self.tex.size, "texture_height": self.tex.size,
                            "visible_bounds_width": self.vb[0], "visible_bounds_height": self.vb[1], "visible_bounds_offset": [0, self.vb[2], 0]},
            "bones": self.bones}]}


def build_hatman():
    m = Model("hatman", 512, (6, 6, 2))
    m.bone("root", None, (0, 0, 0))
    m.bone("all", "root", (0, 0, 0))
    m.bone("posture", "all", (0, 22, 0))
    m.bone("torso", "posture", (0, 22, 0), [
        ("pelvis", (-3.5, 21, -2), (7, 4, 4), "suit"),
        ("waist", (-3, 25, -1.75), (6, 6, 3.5), "suit"),
    ])
    m.bone("chest", "torso", (0, 30, 0), [
        ("chest", (-4.5, 30, -2.25), (9, 6.5, 4.5), "suit"),
        ("shoulders", (-5.5, 35, -1.75), (11, 1.5, 3.5), "suit"),
    ])
    m.bone("coat_back", "torso", (0, 22, 2), [("coat", (-3.5, 9, 1.9), (7, 13, 0.4), "coat")])
    m.bone("coat_l", "torso", (3.5, 22, 0), [("coat", (3.4, 10, -2), (0.4, 12, 4), "coat")])
    m.bone("coat_r", "torso", (-3.5, 22, 0), [("coat", (-3.8, 10, -2), (0.4, 12, 4), "coat")])
    m.bone("neck", "chest", (0, 36.5, 0), [("neck", (-1, 36, -1), (2, 4.5, 2), "skin")])
    m.bone("head_look", "neck", (0, 40, 0))
    m.bone("head", "head_look", (0, 40, 0), [
        ("head", (-3, 41.6, -3), (6, 5.8, 6), "skin"),
        ("mouth", (-2.4, 40, -2.4), (4.8, 2, 4.8), "mouth"),
    ])
    m.bone("jaw", "head", (0, 41.8, 2.6), [("jaw", (-2.9, 39.8, -2.9), (5.8, 2, 5.6), "skin")])
    m.bone("hat", "head", (0, 46.4, 0), [
        ("brim", (-7.5, 46.2, -7.5), (15, 0.8, 15), "hat"),
        ("crown", (-3.6, 47, -3.6), (7.2, 4.6, 7.2), "hat"),
    ])
    for side, sx in (("l", 1), ("r", -1)):
        ax = 5.6 * sx
        x0 = ax - 1.2
        m.bone("arm_" + side, "chest", (ax, 35.5, 0), [("upper", (x0, 23.5, -1.2), (2.4, 12.5, 2.4), "suit")])
        m.bone("forearm_" + side, "arm_" + side, (ax, 24, 0), [
            ("fore", (x0 + 0.15, 13, -1.05), (2.1, 11, 2.1), "suit"),
            ("cuff", (x0 + 0.05, 12.2, -1.15), (2.3, 1, 2.3), "shirt"),
        ])
        m.bone("hand_" + side, "forearm_" + side, (ax, 12.4, 0), [("palm", (ax - 1.3, 9.2, -0.7), (2.6, 3.2, 1.4), "skin")])
        m.bone("fingers_" + side, "hand_" + side, (ax, 9.3, 0), [
            ("finger1", (ax - 1.3, 3.6, -0.3), (0.55, 5.8, 0.55), "skin"),
            ("finger2", (ax - 0.6, 2.8, -0.3), (0.55, 6.6, 0.55), "skin"),
            ("finger3", (ax + 0.1, 3.0, -0.3), (0.55, 6.4, 0.55), "skin"),
            ("finger4", (ax + 0.8, 4.2, -0.3), (0.55, 5.2, 0.55), "skin"),
        ])
        m.bone("thumb_" + side, "hand_" + side, (ax + 1.2 * sx, 11, -0.4), [
            ("finger0", (ax + 1.1 * sx - 0.3, 7, -0.9), (0.6, 4, 0.6), "skin"),
        ])
        lx = 2.0 * sx
        m.bone("leg_" + side, "all", (lx, 22, 0), [("thigh", (lx - 1.5, 10.5, -1.5), (3, 11.5, 3), "suit")])
        m.bone("shin_" + side, "leg_" + side, (lx, 11, 0), [
            ("shin", (lx - 1.35, 1, -1.35), (2.7, 10, 2.7), "suit"),
            ("shoe", (lx - 1.5, 0, -3.6), (3, 1.4, 5.2), "shoe"),
        ])
    return m


def build_guest(variant):
    pals = [
        {"cloth": (38, 34, 40), "shirt": (150, 146, 136), "hair": (30, 24, 20), "mask": (214, 208, 196), "accent": (150, 120, 40), "style": 0},
        {"cloth": (70, 22, 26), "shirt": (120, 60, 60), "hair": (120, 110, 100), "mask": (200, 196, 190), "accent": (30, 60, 110), "style": 1},
        {"cloth": (30, 44, 40), "shirt": (140, 140, 124), "hair": (60, 40, 22), "mask": (190, 170, 120), "accent": (110, 20, 20), "style": 2},
    ]
    GUEST_PAL.clear()
    GUEST_PAL.update(pals[variant])
    m = Model("guest", 256, (3, 3, 1))
    m.bone("root", None, (0, 0, 0))
    m.bone("body", "root", (0, 12, 0), [("g_body", (-4, 12, -2), (8, 12, 4), "g_cloth")])
    m.bone("head", "body", (0, 24, 0), [
        ("g_head", (-4, 24, -4), (8, 8, 8), "g_skin", {"mat_up": "g_hair", "mat_south": "g_hair", "mat_east": "g_hair", "mat_west": "g_hair"}),
        ("g_mask", (-3.5, 24.3, -4.6), (7, 7.4, 0.7), "g_mask"),
    ])
    for side, sx in (("l", 1), ("r", -1)):
        m.bone("arm_" + side, "body", (5 * sx, 22, 0), [("g_arm", (4 * sx - (0 if sx > 0 else 4), 12, -2), (4, 12, 4), "g_cloth")])
        m.bone("leg_" + side, "root", (2 * sx, 12, 0), [("g_leg", (2 * sx - 2, 0, -2), (4, 12, 4), "g_cloth")])
    return m


# ---------------------------------------------------------------- animations

class Anim:
    def __init__(self, length, loop=False, hold=False):
        self.length = length
        self.loop = loop
        self.hold = hold
        self.bones = {}

    def key(self, bone, chan, t, vec, ease=None):
        b = self.bones.setdefault(bone, {}).setdefault(chan, {})
        k = "%.4f" % t
        k = k.rstrip("0").rstrip(".") if "." in k else k
        if k == "":
            k = "0"
        v = [round(float(x), 3) for x in vec]
        b[k if k != "0" else "0.0"] = {"vector": v, "easing": ease} if ease else v

    def rot(self, bone, keys, ease="easeInOutSine"):
        for i, (t, v) in enumerate(keys):
            self.key(bone, "rotation", t, v, ease if i > 0 else None)

    def pos(self, bone, keys, ease="easeInOutSine"):
        for i, (t, v) in enumerate(keys):
            self.key(bone, "position", t, v, ease if i > 0 else None)

    def scl(self, bone, keys, ease="easeInOutSine"):
        for i, (t, v) in enumerate(keys):
            self.key(bone, "scale", t, v, ease if i > 0 else None)

    def fn(self, bone, chan, f, step=0.05):
        """Sample a function of time into dense linear keys - gives smooth procedural motion."""
        n = int(round(self.length / step))
        for i in range(n + 1):
            t = min(self.length, i * step)
            self.key(bone, chan, t, f(t))

    def json(self):
        d = {"animation_length": self.length, "bones": self.bones}
        if self.loop:
            d["loop"] = True
        elif self.hold:
            d["loop"] = "hold_on_last_frame"
        return d


sin = math.sin
cos = math.cos
PI = math.pi
TAU = 2 * math.pi


def smooth(t, a, b):
    """smoothstep from 0 at a to 1 at b"""
    if a == b:
        return 1.0 if t >= b else 0.0
    x = max(0.0, min(1.0, (t - a) / (b - a)))
    return x * x * (3 - 2 * x)


def pulse(t, a, b, c, d):
    return smooth(t, a, b) * (1 - smooth(t, c, d))


def tremor(t, f=37.0, amp=1.0):
    return amp * (sin(t * f) * 0.6 + sin(t * f * 1.7 + 1.3) * 0.4)


def hatman_anims():
    A = {}

    # ---- idle: slow breathing, a tall thing pretending to be still
    a = Anim(8.0, loop=True)
    w = TAU / 8.0
    a.fn("torso", "rotation", lambda t: [1.5 + 1.2 * sin(2 * w * t), 1.5 * sin(w * t), 0.8 * sin(w * t + 1)])
    a.fn("chest", "rotation", lambda t: [2 + 1.5 * sin(2 * w * t + 0.6), 0, 0])
    a.fn("chest", "scale", lambda t: [1 + 0.012 * sin(2 * w * t), 1, 1 + 0.03 * sin(2 * w * t)])
    a.fn("neck", "rotation", lambda t: [4 + 1.5 * sin(2 * w * t + 1.2), 0, 0])
    a.fn("head", "rotation", lambda t: [-3 + 1.0 * sin(2 * w * t + 2), 2 * sin(w * t + 2),
                                        7 * pulse(t, 2.6, 3.4, 5.2, 6.0) + tremor(t, 31, 0.6) * pulse(t, 3.3, 3.4, 3.7, 3.9)])
    a.fn("hat", "rotation", lambda t: [-2, 0, 1.0 * sin(w * t)])
    for s, sg in (("l", 1), ("r", -1)):
        ph = 0 if s == "l" else 1.7
        a.fn("arm_" + s, "rotation", lambda t, sg=sg, ph=ph: [2 + 1.5 * sin(2 * w * t + ph), 0, -sg * (3 + 0.8 * sin(w * t + ph))])
        a.fn("forearm_" + s, "rotation", lambda t, ph=ph: [-6 - 2 * sin(2 * w * t + ph + 1), 0, 0])
        a.fn("hand_" + s, "rotation", lambda t, sg=sg, ph=ph: [0, 0, sg * 4 * sin(w * t + ph)])
        tw = 4.2 if s == "l" else 6.4
        a.fn("fingers_" + s, "rotation", lambda t, tw=tw, ph=ph: [-8 - 4 * sin(2 * w * t + ph) - 30 * pulse(t, tw, tw + 0.12, tw + 0.2, tw + 0.5)
                                                           - 22 * pulse(t, tw + 0.55, tw + 0.65, tw + 0.7, tw + 1.0), 0, 0])
        a.fn("thumb_" + s, "rotation", lambda t, sg=sg, ph=ph: [0, 0, -sg * (10 + 5 * sin(2 * w * t + ph))])
    a.fn("coat_back", "rotation", lambda t: [-2 - 1.5 * sin(2 * w * t + 0.4), 0, 0])
    a.fn("coat_l", "rotation", lambda t: [0, 0, -2 - 1 * sin(2 * w * t)])
    a.fn("coat_r", "rotation", lambda t: [0, 0, 2 + 1 * sin(2 * w * t)])
    A["idle"] = a

    # ---- freeze: caught mid-step, perfectly still (weeping-angel pose)
    a = Anim(1.0, loop=True)
    a.rot("torso", [(0, [14, -6, 0])])
    a.rot("chest", [(0, [8, -4, 0])])
    a.rot("neck", [(0, [-10, 0, 0])])
    a.rot("head", [(0, [-12, 0, -16])])
    a.rot("arm_l", [(0, [-38, 0, -8])])
    a.rot("forearm_l", [(0, [-28, 0, 0])])
    a.rot("fingers_l", [(0, [-24, 0, 0])])
    a.rot("arm_r", [(0, [18, 0, 6])])
    a.rot("forearm_r", [(0, [-10, 0, 0])])
    a.rot("fingers_r", [(0, [-40, 0, 0])])
    a.rot("leg_l", [(0, [-26, 0, 0])])
    a.rot("shin_l", [(0, [22, 0, 0])])
    a.rot("leg_r", [(0, [20, 0, 0])])
    a.rot("shin_r", [(0, [12, 0, 0])])
    a.rot("coat_back", [(0, [10, 0, 0])])
    A["freeze"] = a

    # ---- walk: long deliberate strides, arms hanging dead, head perfectly level
    def gait(length, stride, lean, armswing, bob, knee, headcomp, trail=0.0, shake=0.0, fingers=-10):
        a = Anim(length, loop=True)
        w = TAU / length
        a.fn("all", "position", lambda t: [0, -bob * (0.5 + 0.5 * cos(2 * w * t)) - (0.6 if lean > 20 else 0), 0])
        a.fn("torso", "rotation", lambda t: [lean + 2.0 * sin(2 * w * t + 0.5), 5 * sin(w * t), 2.2 * sin(w * t)])
        a.fn("chest", "rotation", lambda t: [lean * 0.35, -8 * sin(w * t), -1.5 * sin(w * t)])
        a.fn("neck", "rotation", lambda t: [-lean * 0.45 * headcomp, 0, 0])
        a.fn("head", "rotation", lambda t: [-lean * 0.7 * headcomp + shake * sin(2 * w * t), 3 * sin(w * t + 0.4), shake * 2.2 * sin(w * t + 1)])
        a.fn("hat", "rotation", lambda t: [-1 + 1.2 * sin(2 * w * t + 1.3), 0, 0])
        a.fn("jaw", "rotation", lambda t: [shake * 2.5 * (1 + sin(2 * w * t)), 0, 0])
        for s, sg in (("l", 1), ("r", -1)):
            ph = 0 if s == "l" else PI
            a.fn("leg_" + s, "rotation", lambda t, ph=ph: [-stride * sin(w * t + ph) - lean * 0.25, 0, 0])
            a.fn("shin_" + s, "rotation", lambda t, ph=ph: [knee * max(0.0, cos(w * t + ph + 0.35)) ** 1.5 + 3, 0, 0])
            a.fn("arm_" + s, "rotation", lambda t, ph=ph, sg=sg: [armswing * sin(w * t + ph) + trail - lean * 0.6, 0, -sg * (2 + trail * 0.12)])
            a.fn("forearm_" + s, "rotation", lambda t, ph=ph: [-8 - armswing * 0.35 * (1 + sin(w * t + ph - 0.6)) - trail * 0.25, 0, 0])
            a.fn("hand_" + s, "rotation", lambda t, ph=ph, sg=sg: [6 * sin(w * t + ph - 1.2), 0, sg * 3 * sin(2 * w * t)])
            a.fn("fingers_" + s, "rotation", lambda t, ph=ph: [fingers - 8 * sin(w * t + ph - 1.6), 0, 0])
        a.fn("coat_back", "rotation", lambda t: [lean * 0.9 + 6 + 5 * sin(2 * w * t + 2.2) + trail * 0.2, 0, 0])
        a.fn("coat_l", "rotation", lambda t: [stride * 0.3 * sin(w * t + 0.6), 0, -3 - 2 * sin(2 * w * t + 1)])
        a.fn("coat_r", "rotation", lambda t: [stride * 0.3 * sin(w * t + PI + 0.6), 0, 3 + 2 * sin(2 * w * t + 1)])
        return a

    A["walk"] = gait(1.5, 24, 6, 7, 0.7, 30, 1.0)
    A["stalk"] = gait(1.0, 30, 12, 4, 1.0, 38, 1.0, trail=-14, fingers=-26)
    A["run"] = gait(0.56, 46, 34, 10, 1.6, 62, 1.0, trail=52, shake=1.6, fingers=16)

    # ---- crawl: spine horizontal, limbs working like a spider
    a = Anim(0.8, loop=True)
    w = TAU / 0.8
    a.fn("all", "position", lambda t: [0, -9 + 0.6 * sin(2 * w * t), 0])
    a.fn("torso", "rotation", lambda t: [78 + 2 * sin(2 * w * t), 6 * sin(w * t), 0])
    a.fn("chest", "rotation", lambda t: [6, -8 * sin(w * t), 0])
    a.fn("neck", "rotation", lambda t: [-40, 0, 0])
    a.fn("head", "rotation", lambda t: [-44 + 2 * sin(2 * w * t), 6 * sin(w * t), 8 * sin(w * t + 1)])
    a.fn("jaw", "rotation", lambda t: [6 + 4 * sin(2 * w * t), 0, 0])
    for s, sg in (("l", 1), ("r", -1)):
        ph = 0 if s == "l" else PI
        a.fn("arm_" + s, "rotation", lambda t, ph=ph, sg=sg: [-150 + 32 * sin(w * t + ph), 0, -sg * 24])
        a.fn("forearm_" + s, "rotation", lambda t, ph=ph: [-36 - 30 * max(0, cos(w * t + ph)), 0, 0])
        a.fn("hand_" + s, "rotation", lambda t, ph=ph: [50, 0, 0])
        a.fn("fingers_" + s, "rotation", lambda t, ph=ph: [30 + 16 * sin(w * t + ph), 0, 0])
        a.fn("leg_" + s, "rotation", lambda t, ph=ph, sg=sg: [-60 + 30 * sin(w * t + ph + PI), 0, -sg * 16])
        a.fn("shin_" + s, "rotation", lambda t, ph=ph: [96 + 26 * max(0, cos(w * t + ph + PI)), 0, 0])
    a.fn("coat_back", "rotation", lambda t: [70 + 6 * sin(2 * w * t), 0, 0])
    A["crawl"] = a

    # ---- sit (armchair / head of the table)
    a = Anim(6.0, loop=True)
    w = TAU / 6.0
    a.fn("all", "position", lambda t: [0, -11.5, 2.5])
    a.fn("torso", "rotation", lambda t: [-6 + 0.8 * sin(w * t), 0, 0])
    a.fn("chest", "rotation", lambda t: [6 + 1.2 * sin(w * t + 0.5), 0, 0])
    a.fn("chest", "scale", lambda t: [1, 1, 1 + 0.03 * sin(w * t)])
    a.fn("neck", "rotation", lambda t: [6, 0, 0])
    a.fn("head", "rotation", lambda t: [2, 3 * sin(w * t * 0.5), 5 * sin(w * t * 0.5 + 1)])
    for s, sg in (("l", 1), ("r", -1)):
        a.fn("leg_" + s, "rotation", lambda t, sg=sg: [-88, sg * 7, 0])
        a.fn("shin_" + s, "rotation", lambda t: [84, 0, 0])
        a.fn("arm_" + s, "rotation", lambda t, sg=sg: [-34 + 0.8 * sin(w * t), 0, -sg * 5])
        a.fn("forearm_" + s, "rotation", lambda t: [-52, 0, 0])
        a.fn("hand_" + s, "rotation", lambda t: [10, 0, 0])
        tw = 2.0 if s == "l" else 4.1
        a.fn("fingers_" + s, "rotation", lambda t, tw=tw: [26 - 30 * pulse(t, tw, tw + 0.1, tw + 0.15, tw + 0.4) - 30 * pulse(t, tw + 0.45, tw + 0.55, tw + 0.6, tw + 0.85), 0, 0])
    a.fn("coat_back", "rotation", lambda t: [-60, 0, 0])
    A["sit"] = a

    # ---- duck: the deep bow he uses to pass through doorways (posture controller only)
    a = Anim(0.5, loop=True)
    a.rot("posture", [(0, [72, 0, 0])])
    a.rot("head_look", [(0, [-58, 0, 0])])
    A["duck"] = a
    a = Anim(0.5, loop=True)
    a.rot("posture", [(0, [0, 0, 0])])
    a.rot("head_look", [(0, [0, 0, 0])])
    A["upright"] = a

    # ================= one-shot actions =================
    # ---- emerge: unfolds out of the floor
    a = Anim(2.4, hold=False)
    a.fn("all", "position", lambda t: [tremor(t, 29, 0.25) * (1 - smooth(t, 1.4, 2.0)), -52 * (1 - smooth(t, 0.0, 1.5)) ** 1.6, 0])
    a.fn("all", "scale", lambda t: [0.7 + 0.3 * smooth(t, 0.2, 1.6), 1, 0.7 + 0.3 * smooth(t, 0.2, 1.6)])
    a.fn("torso", "rotation", lambda t: [70 * (1 - smooth(t, 0.9, 2.2)), 14 * sin(t * 5) * (1 - smooth(t, 0.8, 2.0)), 0])
    a.fn("chest", "rotation", lambda t: [30 * (1 - smooth(t, 1.2, 2.3)), 0, 0])
    a.fn("neck", "rotation", lambda t: [40 * (1 - smooth(t, 1.4, 2.3)), 0, 0])
    a.fn("head", "rotation", lambda t: [36 * (1 - smooth(t, 1.6, 2.4)), 0, 28 * (1 - smooth(t, 1.7, 2.1)) * sin(t * 9)])
    a.fn("jaw", "rotation", lambda t: [14 * pulse(t, 1.2, 1.5, 1.8, 2.3), 0, 0])
    for s, sg in (("l", 1), ("r", -1)):
        a.fn("arm_" + s, "rotation", lambda t, sg=sg: [-150 * (1 - smooth(t, 0.7, 2.2)), 0, -sg * 40 * pulse(t, 0.2, 0.9, 1.3, 2.3)])
        a.fn("forearm_" + s, "rotation", lambda t: [-70 * pulse(t, 0.3, 0.9, 1.4, 2.3), 0, 0])
        a.fn("fingers_" + s, "rotation", lambda t: [50 * (1 - smooth(t, 1.2, 2.3)), 0, 0])
        a.fn("leg_" + s, "rotation", lambda t: [0, 0, 0])
    A["emerge"] = a

    # ---- vanish: folds in on himself and is pulled down
    a = Anim(1.1, hold=True)
    a.fn("all", "position", lambda t: [tremor(t, 41, 0.5 * smooth(t, 0, 0.4)), -56 * smooth(t, 0.25, 1.05) ** 2, 0])
    a.fn("all", "scale", lambda t: [1 - 0.75 * smooth(t, 0.3, 1.0), 1 + 0.25 * smooth(t, 0.2, 0.9), 1 - 0.75 * smooth(t, 0.3, 1.0)])
    a.fn("torso", "rotation", lambda t: [30 * smooth(t, 0, 0.5), 60 * smooth(t, 0.2, 1.0), 0])
    a.fn("head", "rotation", lambda t: [40 * smooth(t, 0, 0.4), 0, 60 * smooth(t, 0.1, 0.5)])
    for s, sg in (("l", 1), ("r", -1)):
        a.fn("arm_" + s, "rotation", lambda t, sg=sg: [-20 * smooth(t, 0, 0.4), 0, sg * 30 * smooth(t, 0, 0.5)])
        a.fn("forearm_" + s, "rotation", lambda t: [-120 * smooth(t, 0, 0.5), 0, 0])
    A["vanish"] = a

    # ---- hat tip: the polite greeting
    a = Anim(2.2)
    a.fn("torso", "rotation", lambda t: [10 * pulse(t, 0.5, 1.0, 1.5, 2.1), 0, 0])
    a.fn("chest", "rotation", lambda t: [6 * pulse(t, 0.5, 1.0, 1.5, 2.1), 0, 0])
    a.fn("neck", "rotation", lambda t: [8 * pulse(t, 0.6, 1.0, 1.5, 2.1), 0, 0])
    a.fn("head", "rotation", lambda t: [14 * pulse(t, 0.6, 1.0, 1.5, 2.1), 0, -6 * pulse(t, 0.6, 1.0, 1.5, 2.1)])
    a.fn("arm_r", "rotation", lambda t: [-152 * pulse(t, 0.0, 0.6, 1.5, 2.1), 0, -24 * pulse(t, 0.0, 0.6, 1.5, 2.1)])
    a.fn("forearm_r", "rotation", lambda t: [-112 * pulse(t, 0.1, 0.6, 1.5, 2.0), 0, 0])
    a.fn("hand_r", "rotation", lambda t: [-30 * pulse(t, 0.3, 0.6, 1.5, 1.9), 0, 0])
    a.fn("fingers_r", "rotation", lambda t: [60 * pulse(t, 0.4, 0.62, 1.5, 1.8), 0, 0])
    a.fn("hat", "position", lambda t: [0, 2.6 * pulse(t, 0.7, 1.0, 1.4, 1.65), -1.6 * pulse(t, 0.7, 1.0, 1.4, 1.65)])
    a.fn("hat", "rotation", lambda t: [26 * pulse(t, 0.7, 1.0, 1.4, 1.65), 0, 0])
    a.fn("arm_l", "rotation", lambda t: [8 * pulse(t, 0.4, 1.0, 1.5, 2.1), 0, 0])
    A["hat_tip"] = a

    # ---- knock: three slow raps
    a = Anim(2.0)
    kn = lambda t: sum(pulse(t, k, k + 0.07, k + 0.09, k + 0.24) for k in (0.55, 0.95, 1.35))
    a.fn("torso", "rotation", lambda t: [6 * pulse(t, 0, 0.4, 1.6, 2.0), -8 * pulse(t, 0, 0.4, 1.6, 2.0), 0])
    a.fn("head", "rotation", lambda t: [0, 0, 14 * pulse(t, 0.1, 0.5, 1.6, 2.0)])
    a.fn("arm_r", "rotation", lambda t: [-78 * pulse(t, 0, 0.45, 1.6, 2.0) - 10 * kn(t), 0, 0])
    a.fn("forearm_r", "rotation", lambda t: [-92 * pulse(t, 0, 0.45, 1.6, 2.0) + 38 * kn(t), 0, 0])
    a.fn("hand_r", "rotation", lambda t: [-20 * kn(t), 0, 0])
    a.fn("fingers_r", "rotation", lambda t: [80 * pulse(t, 0.1, 0.45, 1.6, 1.9), 0, 0])
    A["knock"] = a

    # ---- scream: head thrown back, jaw unhinged, whole body shaking
    a = Anim(2.6)
    env = lambda t: pulse(t, 0.25, 0.5, 2.0, 2.5)
    a.fn("all", "position", lambda t: [tremor(t, 53, 0.35) * env(t), tremor(t, 47, 0.2) * env(t) - 1.5 * pulse(t, 0, 0.25, 0.3, 0.5), 0])
    a.fn("torso", "rotation", lambda t: [16 * pulse(t, 0, 0.22, 0.28, 0.5) - 14 * env(t) + tremor(t, 43, 1.2) * env(t), 0, tremor(t, 39, 1.0) * env(t)])
    a.fn("chest", "rotation", lambda t: [-12 * env(t), 0, 0])
    a.fn("chest", "scale", lambda t: [1 + 0.1 * env(t), 1, 1 + 0.2 * env(t)])
    a.fn("neck", "rotation", lambda t: [-14 * env(t), 0, 0])
    a.fn("head", "rotation", lambda t: [-30 * env(t) + tremor(t, 61, 3) * env(t), tremor(t, 57, 4) * env(t), tremor(t, 49, 5) * env(t)])
    a.fn("jaw", "rotation", lambda t: [58 * env(t) + tremor(t, 67, 4) * env(t), 0, 0])
    a.fn("jaw", "position", lambda t: [0, -1.4 * env(t), 0])
    a.fn("hat", "rotation", lambda t: [-8 * env(t), 0, tremor(t, 45, 2) * env(t)])
    for s, sg in (("l", 1), ("r", -1)):
        a.fn("arm_" + s, "rotation", lambda t, sg=sg: [18 * env(t) + tremor(t, 44, 2) * env(t), 0, -sg * 54 * env(t)])
        a.fn("forearm_" + s, "rotation", lambda t: [-30 * env(t), 0, 0])
        a.fn("hand_" + s, "rotation", lambda t, sg=sg: [0, 0, -sg * 30 * env(t)])
        a.fn("fingers_" + s, "rotation", lambda t: [-34 * env(t) + tremor(t, 71, 8) * env(t), 0, 0])
        a.fn("thumb_" + s, "rotation", lambda t, sg=sg: [0, 0, -sg * 40 * env(t)])
        a.fn("leg_" + s, "rotation", lambda t, sg=sg: [0, 0, -sg * 6 * env(t)])
    a.fn("coat_back", "rotation", lambda t: [12 * env(t) + tremor(t, 33, 4) * env(t), 0, 0])
    A["scream"] = a

    # ---- grab: the lunge
    a = Anim(0.9)
    a.fn("all", "position", lambda t: [0, -2 * pulse(t, 0, 0.15, 0.5, 0.9), -5 * pulse(t, 0.1, 0.3, 0.55, 0.9)])
    a.fn("torso", "rotation", lambda t: [-10 * pulse(t, 0, 0.12, 0.14, 0.24) + 38 * pulse(t, 0.14, 0.3, 0.55, 0.9), 0, 0])
    a.fn("head", "rotation", lambda t: [-30 * pulse(t, 0.14, 0.3, 0.55, 0.9), 0, 20 * pulse(t, 0.2, 0.3, 0.55, 0.9)])
    a.fn("jaw", "rotation", lambda t: [46 * pulse(t, 0.12, 0.28, 0.55, 0.85), 0, 0])
    for s, sg in (("l", 1), ("r", -1)):
        a.fn("arm_" + s, "rotation", lambda t, sg=sg: [40 * pulse(t, 0, 0.12, 0.14, 0.22) - 128 * pulse(t, 0.14, 0.28, 0.55, 0.9), 0, sg * 18 * pulse(t, 0.2, 0.32, 0.5, 0.9) - sg * 30 * pulse(t, 0, 0.12, 0.14, 0.24)])
        a.fn("forearm_" + s, "rotation", lambda t: [-20 * pulse(t, 0.14, 0.3, 0.55, 0.9), 0, 0])
        a.fn("fingers_" + s, "rotation", lambda t: [-30 * pulse(t, 0, 0.2, 0.26, 0.3) + 70 * pulse(t, 0.28, 0.36, 0.6, 0.9), 0, 0])
    a.fn("leg_l", "rotation", lambda t: [-34 * pulse(t, 0.1, 0.3, 0.55, 0.9), 0, 0])
    a.fn("leg_r", "rotation", lambda t: [26 * pulse(t, 0.1, 0.3, 0.55, 0.9), 0, 0])
    A["grab"] = a

    # ---- search: bends low, head sweeping, listening
    a = Anim(4.0)
    env = lambda t: pulse(t, 0, 0.7, 3.3, 4.0)
    a.fn("all", "position", lambda t: [0, -3 * env(t), 0])
    a.fn("torso", "rotation", lambda t: [44 * env(t), 26 * sin(t * 1.9) * env(t), 0])
    a.fn("chest", "rotation", lambda t: [12 * env(t), 14 * sin(t * 1.9 + 0.4) * env(t), 0])
    a.fn("neck", "rotation", lambda t: [-22 * env(t), 0, 0])
    a.fn("head", "rotation", lambda t: [-26 * env(t), 38 * sin(t * 1.9 + 0.9) * env(t), 18 * sin(t * 3.1) * env(t)])
    a.fn("jaw", "rotation", lambda t: [8 * env(t) * (1 + sin(t * 7)), 0, 0])
    for s, sg in (("l", 1), ("r", -1)):
        a.fn("arm_" + s, "rotation", lambda t, sg=sg: [-48 * env(t) + 8 * sin(t * 2.3 + sg), 0, -sg * 14 * env(t)])
        a.fn("forearm_" + s, "rotation", lambda t, sg=sg: [-30 * env(t) - 12 * sin(t * 2.9 + sg) * env(t), 0, 0])
        a.fn("fingers_" + s, "rotation", lambda t, sg=sg: [(-20 + 30 * sin(t * 6 + sg)) * env(t), 0, 0])
        a.fn("leg_" + s, "rotation", lambda t, sg=sg: [-14 * env(t), 0, -sg * 5 * env(t)])
        a.fn("shin_" + s, "rotation", lambda t: [24 * env(t), 0, 0])
    A["search"] = a

    # ---- point
    a = Anim(2.0)
    env = lambda t: pulse(t, 0, 0.35, 1.5, 2.0)
    a.fn("torso", "rotation", lambda t: [6 * env(t), -6 * env(t), 0])
    a.fn("head", "rotation", lambda t: [4 * env(t), 0, -12 * env(t)])
    a.fn("arm_r", "rotation", lambda t: [-86 * env(t) + tremor(t, 23, 0.8) * env(t), 0, 0])
    a.fn("forearm_r", "rotation", lambda t: [-6 * env(t), 0, 0])
    a.fn("fingers_r", "rotation", lambda t: [-6 * env(t), 0, 0])
    a.fn("thumb_r", "rotation", lambda t: [0, 0, 30 * env(t)])
    A["point"] = a

    # ---- twitch: a neck that breaks and resets
    a = Anim(0.7)
    a.fn("head", "rotation", lambda t: [0, 0, 62 * pulse(t, 0.02, 0.07, 0.3, 0.42) + tremor(t, 80, 6) * pulse(t, 0.05, 0.1, 0.3, 0.45)])
    a.fn("neck", "rotation", lambda t: [0, 0, 16 * pulse(t, 0.02, 0.07, 0.3, 0.42)])
    a.fn("chest", "rotation", lambda t: [0, 14 * pulse(t, 0.0, 0.06, 0.2, 0.5), tremor(t, 60, 3) * pulse(t, 0, 0.1, 0.3, 0.6)])
    a.fn("arm_l", "rotation", lambda t: [0, 0, -30 * pulse(t, 0.04, 0.1, 0.2, 0.5)])
    a.fn("fingers_l", "rotation", lambda t: [-50 * pulse(t, 0.04, 0.1, 0.25, 0.6), 0, 0])
    a.fn("fingers_r", "rotation", lambda t: [70 * pulse(t, 0.1, 0.16, 0.3, 0.65), 0, 0])
    A["twitch"] = a

    # ---- head tilt: curious
    a = Anim(2.4)
    a.fn("head", "rotation", lambda t: [-4 * pulse(t, 0, 0.6, 1.8, 2.4), 0, 34 * pulse(t, 0.1, 0.7, 1.7, 2.3)])
    a.fn("neck", "rotation", lambda t: [0, 0, 10 * pulse(t, 0.1, 0.7, 1.7, 2.3)])
    a.fn("torso", "rotation", lambda t: [8 * pulse(t, 0.2, 0.9, 1.6, 2.4), 0, 0])
    a.fn("fingers_l", "rotation", lambda t: [-30 * pulse(t, 0.6, 0.7, 0.8, 1.1) - 30 * pulse(t, 1.2, 1.3, 1.4, 1.7), 0, 0])
    A["head_tilt"] = a

    # ---- talk (calm): a small, courteous gesture
    a = Anim(3.0)
    env = lambda t: pulse(t, 0, 0.5, 2.4, 3.0)
    a.fn("torso", "rotation", lambda t: [5 * env(t), 4 * sin(t * 2.1) * env(t), 0])
    a.fn("head", "rotation", lambda t: [5 * sin(t * 4.2) * env(t), 6 * sin(t * 1.7) * env(t), 8 * sin(t * 1.3) * env(t)])
    a.fn("jaw", "rotation", lambda t: [5 * env(t) * (1 + sin(t * 13)), 0, 0])
    a.fn("arm_l", "rotation", lambda t: [-42 * env(t), 0, -10 * env(t)])
    a.fn("forearm_l", "rotation", lambda t: [-68 * env(t) - 8 * sin(t * 4.2) * env(t), 0, 0])
    a.fn("hand_l", "rotation", lambda t: [0, 40 * env(t), 16 * sin(t * 4.2) * env(t)])
    a.fn("fingers_l", "rotation", lambda t: [-14 * env(t) + 10 * sin(t * 5) * env(t), 0, 0])
    A["talk_calm"] = a

    # ---- talk (angry): leans over you, fingers working
    a = Anim(2.4)
    env = lambda t: pulse(t, 0, 0.3, 1.9, 2.4)
    a.fn("all", "position", lambda t: [tremor(t, 47, 0.18) * env(t), 0, -2 * env(t)])
    a.fn("torso", "rotation", lambda t: [26 * env(t), 0, tremor(t, 31, 0.9) * env(t)])
    a.fn("chest", "rotation", lambda t: [10 * env(t), 0, 0])
    a.fn("neck", "rotation", lambda t: [-12 * env(t), 0, 0])
    a.fn("head", "rotation", lambda t: [-18 * env(t) + tremor(t, 53, 2.2) * env(t), 0, 22 * sin(t * 3.5) * env(t)])
    a.fn("jaw", "rotation", lambda t: [16 * env(t) * (1 + sin(t * 17)), 0, 0])
    for s, sg in (("l", 1), ("r", -1)):
        a.fn("arm_" + s, "rotation", lambda t, sg=sg: [-22 * env(t), 0, -sg * 22 * env(t)])
        a.fn("forearm_" + s, "rotation", lambda t: [-44 * env(t), 0, 0])
        a.fn("fingers_" + s, "rotation", lambda t, sg=sg: [(10 + 44 * sin(t * 9 + sg)) * env(t), 0, 0])
    A["talk_angry"] = a

    # ---- lean in: brings the brim of the hat down to your eye level and holds there
    a = Anim(1.8, hold=True)
    env = lambda t: smooth(t, 0, 1.5)
    a.fn("all", "position", lambda t: [0, -2 * env(t), -3 * env(t)])
    a.fn("torso", "rotation", lambda t: [52 * env(t), 0, 0])
    a.fn("chest", "rotation", lambda t: [14 * env(t), 0, 0])
    a.fn("neck", "rotation", lambda t: [-30 * env(t), 0, 0])
    a.fn("head", "rotation", lambda t: [-30 * env(t), 0, 20 * smooth(t, 1.0, 1.7)])
    for s, sg in (("l", 1), ("r", -1)):
        a.fn("arm_" + s, "rotation", lambda t, sg=sg: [30 * env(t), 0, -sg * 8 * env(t)])
        a.fn("forearm_" + s, "rotation", lambda t: [-20 * env(t), 0, 0])
        a.fn("fingers_" + s, "rotation", lambda t: [-20 * env(t), 0, 0])
    A["lean_in"] = a

    # ---- offer: takes off the hat and holds it out to you. holds.
    a = Anim(3.0, hold=True)
    up = lambda t: pulse(t, 0.0, 0.7, 1.3, 2.2)
    out = lambda t: smooth(t, 1.5, 2.6)
    a.fn("torso", "rotation", lambda t: [6 * up(t) + 12 * out(t), 0, 0])
    a.fn("head", "rotation", lambda t: [10 * up(t) - 6 * out(t), 0, 10 * out(t)])
    a.fn("arm_r", "rotation", lambda t: [-150 * up(t) - 74 * out(t), 0, -22 * up(t)])
    a.fn("forearm_r", "rotation", lambda t: [-110 * up(t) - 24 * out(t), 0, 0])
    a.fn("hand_r", "rotation", lambda t: [-30 * up(t) - 60 * out(t), 0, 0])
    a.fn("fingers_r", "rotation", lambda t: [60 * smooth(t, 0.5, 0.75), 0, 0])
    a.fn("hat", "position", lambda t: [-5.6 * smooth(t, 1.0, 2.4), 3 * pulse(t, 0.75, 1.1, 1.3, 1.8) - 24 * smooth(t, 1.2, 2.5), -3 * pulse(t, 0.75, 1.1, 1.2, 1.5) - 15 * smooth(t, 1.3, 2.6)])
    a.fn("hat", "rotation", lambda t: [30 * pulse(t, 0.75, 1.1, 1.3, 1.7) - 16 * out(t), 0, 0])
    a.fn("arm_l", "rotation", lambda t: [6 * out(t), 0, -4 * out(t)])
    A["offer"] = a

    # ---- bow: the deep farewell, hat to his chest. holds.
    a = Anim(3.2, hold=True)
    up = lambda t: pulse(t, 0.0, 0.6, 1.0, 1.6)
    dn = lambda t: smooth(t, 1.2, 2.6)
    a.fn("torso", "rotation", lambda t: [6 * up(t) + 62 * dn(t), 0, 0])
    a.fn("chest", "rotation", lambda t: [14 * dn(t), 0, 0])
    a.fn("neck", "rotation", lambda t: [10 * dn(t), 0, 0])
    a.fn("head", "rotation", lambda t: [16 * dn(t), 0, 0])
    a.fn("arm_r", "rotation", lambda t: [-150 * up(t) - 40 * dn(t), 0, -22 * up(t) + 30 * dn(t)])
    a.fn("forearm_r", "rotation", lambda t: [-110 * up(t) - 118 * dn(t), 0, 0])
    a.fn("fingers_r", "rotation", lambda t: [60 * smooth(t, 0.4, 0.65), 0, 0])
    a.fn("hat", "position", lambda t: [-2.5 * dn(t), 3 * pulse(t, 0.65, 0.95, 1.1, 1.5) - 12.5 * dn(t), -5.5 * dn(t)])
    a.fn("hat", "rotation", lambda t: [-80 * dn(t), 0, 0])
    a.fn("arm_l", "rotation", lambda t: [20 * dn(t), 0, -30 * dn(t)])
    a.fn("forearm_l", "rotation", lambda t: [-30 * dn(t), 0, 0])
    a.fn("leg_r", "rotation", lambda t: [18 * dn(t), 0, 0])
    a.fn("shin_r", "rotation", lambda t: [14 * dn(t), 0, 0])
    A["bow"] = a

    # ---- drop: lands from the ceiling in a crouch and rises
    a = Anim(1.6)
    a.fn("all", "position", lambda t: [0, 46 * (1 - smooth(t, 0, 0.28)) ** 2 - 9 * pulse(t, 0.22, 0.34, 0.6, 1.4), 0])
    a.fn("torso", "rotation", lambda t: [58 * pulse(t, 0.2, 0.34, 0.7, 1.5), 0, 0])
    a.fn("neck", "rotation", lambda t: [-30 * pulse(t, 0.3, 0.5, 0.9, 1.5), 0, 0])
    a.fn("head", "rotation", lambda t: [-30 * pulse(t, 0.3, 0.5, 0.9, 1.5), 0, 30 * pulse(t, 0.5, 0.6, 0.9, 1.2)])
    for s, sg in (("l", 1), ("r", -1)):
        a.fn("leg_" + s, "rotation", lambda t, sg=sg: [-64 * pulse(t, 0.2, 0.34, 0.7, 1.5), 0, -sg * 14 * pulse(t, 0.2, 0.34, 0.7, 1.5)])
        a.fn("shin_" + s, "rotation", lambda t: [104 * pulse(t, 0.2, 0.34, 0.7, 1.5), 0, 0])
        a.fn("arm_" + s, "rotation", lambda t, sg=sg: [-160 * (1 - smooth(t, 0.1, 0.34)) - 40 * pulse(t, 0.25, 0.4, 0.7, 1.5), 0, -sg * 20 * pulse(t, 0.25, 0.4, 0.7, 1.5)])
        a.fn("fingers_" + s, "rotation", lambda t: [-40 * pulse(t, 0.25, 0.4, 0.7, 1.4), 0, 0])
    A["drop"] = a

    # ---- burst: shoulders through a door
    a = Anim(0.9)
    a.fn("all", "position", lambda t: [0, 0, 5 * pulse(t, 0, 0.2, 0.22, 0.3) - 6 * pulse(t, 0.22, 0.32, 0.5, 0.9)])
    a.fn("torso", "rotation", lambda t: [-14 * pulse(t, 0, 0.2, 0.22, 0.3) + 30 * pulse(t, 0.22, 0.32, 0.5, 0.9), 30 * pulse(t, 0.2, 0.32, 0.5, 0.9), 0])
    a.fn("head", "rotation", lambda t: [-20 * pulse(t, 0.22, 0.32, 0.5, 0.9), -24 * pulse(t, 0.22, 0.32, 0.5, 0.9), 0])
    a.fn("arm_l", "rotation", lambda t: [-60 * pulse(t, 0.2, 0.32, 0.5, 0.9), 0, 30 * pulse(t, 0.2, 0.32, 0.5, 0.9)])
    a.fn("forearm_l", "rotation", lambda t: [-90 * pulse(t, 0.2, 0.32, 0.5, 0.9), 0, 0])
    a.fn("arm_r", "rotation", lambda t: [40 * pulse(t, 0.2, 0.32, 0.5, 0.9), 0, 0])
    a.fn("leg_l", "rotation", lambda t: [-30 * pulse(t, 0.2, 0.32, 0.5, 0.9), 0, 0])
    a.fn("leg_r", "rotation", lambda t: [24 * pulse(t, 0.2, 0.32, 0.5, 0.9), 0, 0])
    A["burst"] = a

    # ---- stand up from sitting / sit down
    a = Anim(1.6)
    k = lambda t: 1 - smooth(t, 0.1, 1.4)
    a.fn("all", "position", lambda t: [0, -11.5 * k(t), 2.5 * k(t)])
    a.fn("torso", "rotation", lambda t: [-6 * k(t) + 40 * pulse(t, 0.1, 0.5, 0.8, 1.5), 0, 0])
    a.fn("head", "rotation", lambda t: [-30 * pulse(t, 0.1, 0.5, 0.8, 1.5), 0, 0])
    for s, sg in (("l", 1), ("r", -1)):
        a.fn("leg_" + s, "rotation", lambda t, sg=sg: [-88 * k(t), sg * 7 * k(t), 0])
        a.fn("shin_" + s, "rotation", lambda t: [84 * k(t), 0, 0])
        a.fn("arm_" + s, "rotation", lambda t, sg=sg: [-34 * k(t), 0, -sg * 5 * k(t)])
        a.fn("forearm_" + s, "rotation", lambda t: [-52 * k(t), 0, 0])
    A["stand_up"] = a

    return A


def guest_anims():
    A = {}
    a = Anim(7.0, loop=True)
    w = TAU / 7.0
    a.fn("root", "position", lambda t: [0, -3.5, 0])
    a.fn("body", "rotation", lambda t: [3 + 0.6 * sin(w * t), 0, 0])
    a.fn("head", "rotation", lambda t: [8 + 1.0 * sin(w * t + 1), 0, 0])
    for s, sg in (("l", 1), ("r", -1)):
        a.fn("leg_" + s, "rotation", lambda t: [-90, 0, 0])
        a.fn("arm_" + s, "rotation", lambda t, sg=sg: [-38, 0, 0])
    A["sit"] = a
    a = Anim(7.0, loop=True)
    a.fn("body", "rotation", lambda t: [2 + 0.6 * sin(w * t), 0, 1.0 * sin(w * t * 0.5)])
    a.fn("head", "rotation", lambda t: [10 + 1.0 * sin(w * t + 1), 0, 0])
    for s, sg in (("l", 1), ("r", -1)):
        a.fn("arm_" + s, "rotation", lambda t, sg=sg: [1.5 * sin(w * t + sg), 0, 0])
    A["stand"] = a
    a = Anim(0.5)
    a.fn("head", "rotation", lambda t: [0, 0, 40 * pulse(t, 0.02, 0.08, 0.2, 0.4) + tremor(t, 70, 6) * pulse(t, 0.05, 0.1, 0.2, 0.4)])
    A["twitch"] = a
    a = Anim(2.0, hold=True)
    for s, sg in (("l", 1), ("r", -1)):
        a.fn("arm_" + s, "rotation", lambda t, sg=sg: [-86 * smooth(t, 0, 1.4) if sg < 0 else 0, 0, 0])
    A["point"] = a
    return A


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, separators=(",", ":"))


def main():
    hm = build_hatman()
    write_json(f"{OUT}/geo/entity/hatman.geo.json", hm.geo())
    os.makedirs(f"{OUT}/textures/entity", exist_ok=True)
    hm.tex.img.save(f"{OUT}/textures/entity/hatman.png")
    hm.glow.save(f"{OUT}/textures/entity/hatman_glowmask.png")
    write_json(f"{OUT}/animations/entity/hatman.animation.json",
               {"format_version": "1.8.0", "animations": {"animation.hatman." + k: v.json() for k, v in hatman_anims().items()}})
    for v in range(3):
        g = build_guest(v)
        if v == 0:
            write_json(f"{OUT}/geo/entity/guest.geo.json", g.geo())
        g.tex.img.save(f"{OUT}/textures/entity/guest_{v}.png")
    write_json(f"{OUT}/animations/entity/guest.animation.json",
               {"format_version": "1.8.0", "animations": {"animation.guest." + k: v.json() for k, v in guest_anims().items()}})
    print("entity assets ok")


if __name__ == "__main__":
    main()
