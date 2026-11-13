#!/usr/bin/env python3
"""Generates block/item models, blockstates, textures, GUI art and shaders for The Hatman: Hollow House."""
import json, math, os, random, sys
from PIL import Image, ImageDraw, ImageFilter

OUT = sys.argv[1] if len(sys.argv) > 1 else "common/src/main/resources/assets/hatman"
R = random.Random(1962)


def W(path, obj):
    p = os.path.join(OUT, path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w") as f:
        json.dump(obj, f, separators=(",", ":"))


def save(img, path):
    p = os.path.join(OUT, "textures", path + ".png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def cl(v):
    return max(0, min(255, int(v)))


def tex(size, base, amp=8, seed=0):
    r = random.Random(seed or hash(base))
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            n = r.randint(-amp, amp)
            px[x, y] = (cl(base[0] + n), cl(base[1] + n), cl(base[2] + n), 255)
    return img


def shade(img, f):
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            k = f(x, y)
            px[x, y] = (cl(r + k), cl(g + k), cl(b + k), a)
    return img


def wood(base, size=16, seed=1, planks=0):
    img = tex(size, base, 5, seed)
    r = random.Random(seed)
    px = img.load()
    for y in range(size):
        streak = r.randint(-9, 6)
        for x in range(size):
            c = px[x, y]
            k = streak + (4 if (x * 7 + y * 3 + seed) % 11 == 0 else 0)
            px[x, y] = (cl(c[0] + k), cl(c[1] + k), cl(c[2] + k), 255)
    if planks:
        d = ImageDraw.Draw(img)
        for y in range(0, size, size // planks):
            d.line([(0, y), (size, y)], fill=(cl(base[0] - 30), cl(base[1] - 30), cl(base[2] - 30), 255))
            off = r.randint(2, size - 3)
            d.line([(off, y), (off, y + size // planks)], fill=(cl(base[0] - 26), cl(base[1] - 26), cl(base[2] - 26), 255))
    return img


def stains(img, n, color, seed, rad=(2, 6), alpha=110):
    r = random.Random(seed)
    w, h = img.size
    ov = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    for _ in range(n):
        x, y, rr = r.randint(0, w), r.randint(0, h), r.randint(*rad)
        d.ellipse([x - rr, y - rr * r.uniform(0.6, 1.6), x + rr, y + rr * r.uniform(0.6, 1.6)], fill=color + (r.randint(alpha // 3, alpha),))
    ov = ov.filter(ImageFilter.GaussianBlur(1.1))
    img.alpha_composite(ov)
    return img


def wallpaper(base, accent, seed, stained=False):
    s = 32
    img = tex(s, base, 4, seed)
    d = ImageDraw.Draw(img)
    for cx, cy in ((8, 8), (24, 24), (8 + 16, 8 - 16), (8 - 16, 8 + 16)):
        for ox in (-32, 0, 32):
            for oy in (-32, 0, 32):
                x, y = cx + ox, cy + oy
                d.polygon([(x, y - 6), (x + 4, y), (x, y + 6), (x - 4, y)], outline=accent + (255,))
                d.ellipse([x - 1, y - 1, x + 1, y + 1], fill=accent + (255,))
                d.line([(x - 6, y - 7), (x - 4, y - 5)], fill=accent + (255,))
                d.line([(x + 6, y - 7), (x + 4, y - 5)], fill=accent + (255,))
    for x in (0, 16):
        d.line([(x, 0), (x, s)], fill=(cl(base[0] - 14), cl(base[1] - 14), cl(base[2] - 14), 255))
    shade(img, lambda x, y: -8 * (y / s))
    stains(img, 5, (20, 16, 10), seed + 5, (2, 5), 60)
    if stained:
        stains(img, 9, (40, 30, 14), seed + 9, (4, 11), 170)
        stains(img, 4, (8, 8, 6), seed + 3, (3, 8), 190)
        r = random.Random(seed)
        px = img.load()
        for _ in range(3):  # peeled strips down to the plaster
            x0, y0, ln = r.randint(2, 28), r.randint(0, 12), r.randint(8, 20)
            for y in range(y0, min(s, y0 + ln)):
                for x in range(x0, min(s, x0 + r.randint(2, 4))):
                    px[x, y] = (150 + r.randint(-12, 8), 142 + r.randint(-12, 8), 124 + r.randint(-12, 8), 255)
    return img


def gen_textures():
    T = {}
    T["wallpaper_green"] = wallpaper((44, 70, 62), (70, 100, 86), 11)
    T["wallpaper_stained"] = wallpaper((50, 66, 56), (70, 92, 78), 12, True)
    T["wallpaper_red"] = wallpaper((84, 34, 36), (118, 56, 50), 13)
    T["wallpaper_blue"] = wallpaper((50, 66, 96), (86, 104, 132), 14)
    # wainscot: a dark framed panel
    img = wood((40, 27, 20), 16, 21)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(24, 15, 11, 255))
    d.rectangle([2, 3, 13, 13], outline=(58, 40, 30, 255))
    d.rectangle([3, 4, 12, 12], outline=(26, 17, 12, 255))
    d.line([(0, 1), (15, 1)], fill=(66, 46, 34, 255))
    T["wainscot"] = stains(img, 3, (10, 8, 6), 22, (1, 3), 120)
    img = tex(16, (156, 150, 136), 6, 31)
    d = ImageDraw.Draw(img)
    r = random.Random(31)
    x, y = 3, 0
    for _ in range(14):
        nx, ny = x + r.randint(-2, 2), y + r.randint(1, 2)
        d.line([(x, y), (nx, ny)], fill=(96, 90, 80, 255))
        x, y = nx, ny
    T["plaster"] = stains(img, 5, (70, 60, 40), 32, (2, 5), 90)
    img = wood((58, 44, 32), 16, 41, planks=4)
    T["rotten_planks"] = stains(stains(img, 6, (14, 12, 9), 42, (1, 4), 200), 4, (40, 56, 30), 43, (1, 3), 110)
    img = tex(16, (10, 12, 16), 3, 51)
    d = ImageDraw.Draw(img)
    for i, (y0, tilt) in enumerate(((1, 1), (6, -1), (11, 1))):
        for x in range(16):
            yy = y0 + (x * tilt) // 9
            c = wood((84, 62, 42), 16, 52 + i).getpixel((x, 3))
            for t in range(4):
                if 0 <= yy + t < 16:
                    img.putpixel((x, yy + t), (cl(c[0] - (10 if t in (0, 3) else 0)), cl(c[1] - (10 if t in (0, 3) else 0)), cl(c[2] - (8 if t in (0, 3) else 0)), 255))
        d.point([(2, y0 + 1), (13, y0 + 2 + tilt)], fill=(30, 30, 32, 255))
    T["boarded_window"] = img
    img = tex(16, (58, 52, 48), 7, 61)
    d = ImageDraw.Draw(img)
    for y in range(0, 16, 4):
        d.line([(0, y), (16, y)], fill=(30, 27, 25, 255))
        for x in range((y // 4 % 2) * 4, 16, 8):
            d.line([(x, y), (x, y + 4)], fill=(30, 27, 25, 255))
    T["cellar_brick"] = stains(img, 5, (18, 24, 14), 62, (1, 4), 130)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            dark = ((x // 8) + (y // 8)) % 2 == 0
            n = R.randint(-6, 6)
            c = (52, 54, 52) if dark else (150, 146, 132)
            img.putpixel((x, y), (cl(c[0] + n), cl(c[1] + n), cl(c[2] + n), 255))
    d = ImageDraw.Draw(img)
    d.line([(0, 0), (15, 0)], fill=(30, 30, 28, 255)); d.line([(0, 8), (15, 8)], fill=(30, 30, 28, 255))
    d.line([(0, 0), (0, 15)], fill=(30, 30, 28, 255)); d.line([(8, 0), (8, 15)], fill=(30, 30, 28, 255))
    T["tile_floor"] = stains(img, 4, (30, 22, 12), 63, (1, 4), 120)

    # ---- material sheets used by the furniture models
    T["m_wood_dark"] = wood((44, 30, 22), 16, 71)
    T["m_wood"] = wood((92, 66, 44), 16, 72)
    T["m_wood_light"] = wood((150, 118, 82), 16, 73)
    img = tex(16, (74, 50, 44), 7, 74)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                c = px[x, y]
                px[x, y] = (cl(c[0] + 14), cl(c[1] + 10), cl(c[2] + 8), 255)
    T["m_fabric"] = stains(img, 4, (20, 14, 10), 75, (1, 4), 130)
    T["m_metal"] = shade(tex(16, (62, 66, 68), 5, 76), lambda x, y: 10 - y)
    T["m_metal_green"] = stains(shade(tex(16, (50, 70, 54), 5, 77), lambda x, y: 8 - y), 5, (70, 40, 20), 78, (1, 3), 150)
    T["m_brass"] = shade(tex(16, (150, 116, 50), 6, 79), lambda x, y: 16 - 2 * y)
    T["m_black"] = tex(16, (16, 15, 17), 3, 80)
    T["m_felt_grey"] = tex(16, (92, 88, 84), 8, 81)
    T["m_porcelain"] = stains(shade(tex(16, (198, 194, 182), 4, 82), lambda x, y: 6 - y // 2), 4, (90, 70, 40), 83, (1, 4), 110)
    T["m_glass_dark"] = shade(tex(16, (20, 26, 30), 2, 84), lambda x, y: (10 if (x + y) in (9, 10, 16) else 0))
    T["m_paper"] = stains(tex(16, (196, 184, 152), 6, 85), 3, (120, 96, 50), 86, (1, 4), 90)
    T["m_cloth_pale"] = stains(tex(16, (150, 140, 124), 7, 87), 4, (70, 60, 44), 88, (1, 4), 120)
    img = tex(16, (150, 30, 30), 5, 89)
    d = ImageDraw.Draw(img)
    for x in range(0, 16, 4):
        d.line([(x, 0), (x + 1, 15)], fill=(200, 190, 170, 255))
    T["m_stripes"] = img
    # hat: felt with a band on the lower rows
    img = tex(16, (15, 14, 16), 3, 90)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 12, 15, 14], fill=(96, 16, 18, 255))
    T["m_hat"] = img
    # cans sheet: 4 cans side by side, label bands
    img = Image.new("RGBA", (16, 16))
    for i, c in enumerate(((150, 38, 30), (198, 180, 140), (60, 84, 110), (120, 110, 60))):
        for x in range(i * 4, i * 4 + 4):
            for y in range(16):
                n = R.randint(-8, 8)
                k = -18 if x % 4 in (0, 3) else 0
                band = 30 if 5 <= y <= 9 else 0
                img.putpixel((x, y), (cl(c[0] + n + k + band), cl(c[1] + n + k + band), cl(c[2] + n + k + band), 255))
            img.putpixel((x, 0), (90, 90, 90, 255)); img.putpixel((x, 15), (70, 70, 70, 255))
    T["m_cans"] = stains(img, 5, (60, 34, 14), 91, (1, 2), 150)
    # mirror: tarnished silver
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = 40 + (x - y) * 2 + R.randint(-4, 4)
            img.putpixel((x, y), (cl(k * 0.9), cl(k), cl(k * 1.08), 255))
    T["m_mirror"] = stains(img, 7, (10, 10, 8), 92, (1, 3), 200)
    # wardrobe door front: two panelled doors, slats near the top
    img = wood((50, 34, 24), 16, 93)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(26, 17, 12, 255))
    d.line([(8, 0), (8, 15)], fill=(22, 14, 10, 255))
    d.rectangle([2, 2, 6, 13], outline=(72, 50, 36, 255)); d.rectangle([10, 2, 14, 13], outline=(72, 50, 36, 255))
    d.point([(7, 8), (9, 8)], fill=(170, 136, 60, 255))
    T["wardrobe_low"] = img.copy()
    for y in (4, 6, 8, 10):
        d.line([(3, y), (5, y)], fill=(12, 8, 6, 255)); d.line([(11, y), (13, y)], fill=(12, 8, 6, 255))
    T["wardrobe_high"] = img
    # drawer fronts
    img = wood((66, 46, 32), 16, 94)
    d = ImageDraw.Draw(img)
    for y0 in (0, 5, 10):
        d.rectangle([1, y0 + 1, 14, y0 + 4 + (1 if y0 == 10 else 0)], outline=(34, 22, 15, 255))
        d.rectangle([7, y0 + 2, 8, y0 + 3], fill=(170, 136, 60, 255))
    T["drawers"] = img
    img = wood((66, 46, 32), 16, 95)
    d = ImageDraw.Draw(img)
    d.rectangle([1, 1, 14, 14], outline=(34, 22, 15, 255)); d.rectangle([6, 7, 9, 8], fill=(170, 136, 60, 255))
    T["drawer_front"] = img
    # safe door
    img = T["m_metal"].copy()
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], outline=(30, 32, 34, 255)); d.rectangle([2, 2, 13, 13], outline=(90, 94, 96, 255))
    d.ellipse([5, 5, 10, 10], fill=(40, 42, 44, 255), outline=(170, 136, 60, 255)); d.point((7, 7), fill=(220, 200, 120, 255))
    d.rectangle([12, 6, 13, 9], fill=(170, 136, 60, 255))
    T["safe_door"] = img
    img = tex(16, (8, 8, 9), 2, 96)
    ImageDraw.Draw(img).rectangle([0, 7, 15, 8], fill=(40, 42, 44, 255))
    T["safe_inside"] = img
    # clock face & pendulum window
    img = wood((44, 30, 22), 16, 97)
    d = ImageDraw.Draw(img)
    d.ellipse([3, 3, 12, 12], fill=(200, 190, 160, 255), outline=(150, 116, 50, 255))
    d.line([(8, 8), (8, 4)], fill=(20, 16, 12, 255)); d.line([(8, 8), (10, 6)], fill=(20, 16, 12, 255))
    for a in range(12):
        d.point((round(7.5 + 4 * math.sin(a * math.pi / 6)), round(7.5 - 4 * math.cos(a * math.pi / 6))), fill=(60, 40, 20, 255))
    T["clock_face"] = img
    img = wood((44, 30, 22), 16, 98)
    d = ImageDraw.Draw(img)
    d.rectangle([5, 1, 10, 13], fill=(12, 14, 16, 255), outline=(26, 17, 12, 255))
    d.line([(8, 1), (7, 10)], fill=(150, 116, 50, 255)); d.ellipse([5, 9, 9, 13], fill=(170, 136, 60, 255))
    T["clock_body"] = img
    # tv fronts
    def tv(mode, frame=0):
        img = wood((60, 42, 30), 16, 99)
        d = ImageDraw.Draw(img)
        d.rectangle([1, 1, 14, 14], outline=(30, 20, 14, 255))
        d.rectangle([2, 2, 11, 12], fill=(8, 10, 12, 255), outline=(20, 20, 22, 255))
        d.rectangle([13, 3, 13, 4], fill=(150, 116, 50, 255)); d.rectangle([13, 6, 13, 7], fill=(150, 116, 50, 255))
        for y in range(9, 13):
            d.point((13, y), fill=(20, 16, 12, 255))
        r = random.Random(100 + frame)
        if mode == 1:
            for y in range(3, 12):
                for x in range(3, 11):
                    v = r.randint(40, 230)
                    img.putpixel((x, y), (v, v, cl(v * 1.05), 255))
        elif mode == 2:
            for y in range(3, 12):
                for x in range(3, 11):
                    v = 150 + r.randint(-10, 10) - (y - 3) * 4
                    img.putpixel((x, y), (cl(v * 0.9), cl(v), cl(v * 0.86), 255))
            # a tall figure in a hat, centre screen
            d.rectangle([5, 4, 8, 4], fill=(6, 6, 6, 255)); d.rectangle([6, 3, 7, 3], fill=(6, 6, 6, 255))
            d.rectangle([6, 5, 7, 11], fill=(6, 6, 6, 255)); d.point([(5, 7), (8, 7), (5, 8), (8, 8)], fill=(6, 6, 6, 255))
        else:
            d.line([(3, 3), (5, 3)], fill=(30, 34, 38, 255)); d.point((3, 4), fill=(30, 34, 38, 255))
        return img
    T["tv_off"] = tv(0)
    T["tv_on"] = tv(2)
    strip = Image.new("RGBA", (16, 64))
    for f in range(4):
        strip.paste(tv(1, f), (0, f * 16))
    T["tv_static"] = strip
    # phone top: dial
    img = tex(16, (18, 17, 19), 3, 101)
    d = ImageDraw.Draw(img)
    d.ellipse([4, 4, 11, 11], outline=(150, 116, 50, 255)); d.ellipse([6, 6, 9, 9], fill=(190, 180, 150, 255))
    T["phone_top"] = img
    # masks
    for i, (base, style, acc) in enumerate((((214, 208, 196), 0, (150, 120, 40)), ((200, 196, 190), 1, (30, 60, 110)), ((190, 170, 120), 2, (110, 20, 20)),
                                            ((120, 30, 30), 0, (200, 180, 90)), ((40, 40, 46), 1, (190, 190, 190)), ((224, 220, 210), 2, (60, 60, 60)))):
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        d.ellipse([1, 0, 14, 15], fill=base + (255,), outline=(cl(base[0] - 60), cl(base[1] - 60), cl(base[2] - 60), 255))
        d.ellipse([3, 5, 6, 7], fill=(4, 4, 4, 255)); d.ellipse([9, 5, 12, 7], fill=(4, 4, 4, 255))
        d.line([(3, 3), (6, 3)], fill=acc + (255,)); d.line([(9, 3), (12, 3)], fill=acc + (255,))
        if style == 0:
            d.arc([4, 8, 11, 13], 20, 160, fill=(30, 10, 10, 255))
        elif style == 1:
            d.arc([4, 11, 11, 15], 200, 340, fill=(30, 10, 10, 255))
        else:
            d.line([(6, 12), (9, 12)], fill=(50, 40, 40, 255))
        rr = random.Random(200 + i)
        x, y = rr.randint(4, 11), 0
        for _ in range(6):
            nx, ny = x + rr.randint(-1, 1), y + rr.randint(1, 2)
            d.line([(x, y), (nx, ny)], fill=(cl(base[0] - 90), cl(base[1] - 90), cl(base[2] - 90), 255))
            x, y = nx, ny
        T[f"mask_{i}"] = img
    # papers
    def sheet(lines=True, tint=(196, 184, 152), seed=300):
        img = stains(tex(16, tint, 6, seed), 3, (120, 96, 50), seed + 1, (1, 4), 90)
        if lines:
            d = ImageDraw.Draw(img)
            rr = random.Random(seed)
            for y in range(3, 14, 2):
                d.line([(2, y), (rr.randint(8, 13), y)], fill=(70, 60, 50, 255))
        return img
    T["paper_note"] = sheet()
    T["paper_pile"] = sheet(seed=310, tint=(180, 170, 140))
    img = sheet(seed=320, tint=(206, 196, 168))
    ImageDraw.Draw(img).line([(8, 0), (8, 15)], fill=(90, 76, 60, 255))
    T["paper_book"] = img
    img = tex(16, (150, 140, 118), 6, 330)
    d = ImageDraw.Draw(img)
    d.rectangle([1, 2, 14, 13], fill=(60, 56, 50, 255), outline=(210, 204, 186, 255))
    for x in (4, 7, 10):
        d.rectangle([x, 6, x + 1, 11], fill=(150, 144, 130, 255)); d.point((x, 5), fill=(190, 184, 168, 255))
    d.line([(12, 4), (13, 12)], fill=(20, 18, 18, 255))
    T["paper_photo"] = img
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([0, 0, 15, 15], fill=(204, 200, 188, 255), outline=(150, 146, 134, 255))
    d.ellipse([3, 3, 12, 12], outline=(170, 166, 150, 255)); d.ellipse([6, 7, 9, 10], fill=(90, 70, 50, 255))
    T["paper_plate"] = img
    img = sheet(seed=340, tint=(170, 164, 146))
    d = ImageDraw.Draw(img)
    d.rectangle([2, 1, 13, 3], fill=(40, 36, 32, 255)); d.rectangle([9, 5, 13, 10], fill=(90, 86, 78, 255))
    T["paper_news"] = img
    # streamer: ragged paper garland
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cols = ((150, 40, 40), (190, 170, 110), (60, 80, 110), (140, 130, 120))
    for x in range(16):
        y = int(3 + 3 * math.sin(x / 15 * math.pi))
        c = cols[(x // 4) % 4]
        for t in range(2):
            img.putpixel((x, y + t), c + (255,))
        if x % 4 == 1:
            for t in range(2, 7 + (x % 3)):
                if y + t < 16:
                    img.putpixel((x, y + t), (cl(c[0] - 20), cl(c[1] - 20), cl(c[2] - 20), 255))
    T["streamer"] = img
    # lamp glass
    for name, c in (("lamp_off", (60, 56, 50)), ("lamp_on", (255, 214, 130)), ("lamp_dim", (150, 104, 50))):
        img = tex(16, c, 6, 400)
        shade(img, lambda x, y: 16 - abs(x - 8) * 3 - abs(y - 8) * 2 if name != "lamp_off" else 0)
        T[name] = img
    # piano keys
    img = Image.new("RGBA", (16, 16), (224, 216, 196, 255))
    d = ImageDraw.Draw(img)
    for x in range(0, 16, 2):
        d.line([(x, 0), (x, 15)], fill=(120, 112, 100, 255))
        if x % 14 not in (4, 12):
            d.rectangle([x + 1, 0, x + 1, 9], fill=(14, 14, 16, 255))
    T["piano_keys"] = img
    img = T["m_metal_green"].copy()
    d = ImageDraw.Draw(img)
    for y in (3, 5, 7):
        d.line([(2, y), (9, y)], fill=(20, 26, 22, 255))
    d.rectangle([11, 3, 13, 5], fill=(60, 20, 20, 255))
    T["generator_off"] = img.copy()
    d.rectangle([11, 3, 13, 5], fill=(90, 255, 110, 255))
    T["generator_on"] = img
    # camcorder
    img = tex(16, (30, 30, 34), 4, 500)
    d = ImageDraw.Draw(img)
    d.ellipse([4, 4, 11, 11], fill=(8, 8, 14, 255), outline=(70, 70, 76, 255)); d.point((6, 6), fill=(120, 140, 200, 255))
    T["cam_lens"] = img
    for k, v in T.items():
        save(v, "block/" + k)
    with open(os.path.join(OUT, "textures/block/tv_static.png.mcmeta"), "w") as f:
        json.dump({"animation": {"frametime": 2}}, f)


# ------------------------------------------------------------------ models
def box(frm, to, t, faces=None, uv=None, rot=None, shade_=True):
    e = {"from": list(frm), "to": list(to), "faces": {}}
    for f in ("north", "south", "east", "west", "up", "down"):
        tx = (faces or {}).get(f, t)
        if tx is None:
            continue
        face = {"texture": "#" + tx}
        if uv and f in uv:
            face["uv"] = uv[f]
        e["faces"][f] = face
    if rot:
        e["rotation"] = rot
    if not shade_:
        e["shade"] = False
    return e


def model(name, textures, elements, cutout=False, parent="block/block"):
    m = {"parent": "minecraft:" + parent, "textures": {k: "hatman:block/" + v for k, v in textures.items()}, "elements": elements}
    first = next(iter(textures.values()))
    m["textures"]["particle"] = "hatman:block/" + first
    if cutout:
        m["render_type"] = "minecraft:cutout"
    m["display"] = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
                    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
                    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
                    "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]}}
    W(f"models/block/{name}.json", m)


ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
STATES = {}
ITEMS = {}


def facing_state(name, variants):
    """variants: list of (extra-property-string, model)"""
    v = {}
    for extra, mdl in variants:
        for f, r in ROT.items():
            key = f"facing={f}" + ("," + extra if extra else "")
            e = {"model": "hatman:block/" + mdl}
            if r:
                e["y"] = r
            v[key] = e
    W(f"blockstates/{name}.json", {"variants": v})
    ITEMS[name] = variants[0][1]


def legs(t, x1, z1, x2, z2, h, s=1.5):
    return [box((x1, 0, z1), (x1 + s, h, z1 + s), t), box((x2 - s, 0, z1), (x2, h, z1 + s), t),
            box((x1, 0, z2 - s), (x1 + s, h, z2), t), box((x2 - s, 0, z2 - s), (x2, h, z2), t)]


def gen_models():
    for c in ("wallpaper_green", "wallpaper_stained", "wallpaper_red", "wallpaper_blue", "wainscot", "plaster", "rotten_planks", "boarded_window", "cellar_brick", "tile_floor"):
        W(f"models/block/{c}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "hatman:block/" + c}})
        W(f"blockstates/{c}.json", {"variants": {"": {"model": "hatman:block/" + c}}})
        ITEMS[c] = c

    model("armchair", {"f": "m_fabric", "w": "m_wood_dark"}, legs("w", 1, 2, 15, 15, 2) + [
        box((1, 2, 2), (15, 7, 15), "f"), box((1, 7, 11.5), (15, 16, 15), "f"),
        box((1, 7, 2), (3.5, 11, 11.5), "f"), box((12.5, 7, 2), (15, 11, 11.5), "f"),
        box((3.5, 7, 2.5), (12.5, 8, 11.5), "f")])
    facing_state("armchair", [("", "armchair")])

    model("dining_chair", {"w": "m_wood_dark", "f": "m_fabric"}, legs("w", 3, 3, 13, 13, 7, 1.5) + [
        box((3, 7, 3), (13, 8.5, 13), "f"), box((3, 8.5, 11.5), (4.5, 19, 13), "w"), box((11.5, 8.5, 11.5), (13, 19, 13), "w"),
        box((4.5, 16, 11.8), (11.5, 19, 12.8), "w"), box((4.5, 11, 12), (11.5, 13, 12.6), "w")])
    facing_state("dining_chair", [("", "dining_chair")])

    cans = []
    rr = random.Random(7)
    for shelf_y in (1, 9):
        x = 1.0
        while x < 13.5:
            w = rr.choice((2.5, 3, 3))
            h = rr.choice((3.5, 4.5, 5.5))
            i = rr.randint(0, 3)
            if rr.random() < 0.82:
                z = rr.choice((9, 10, 11))
                cans.append(box((x, shelf_y, z), (x + w, shelf_y + h, z + w), "c",
                                uv={f: [i * 4, 16 - h * 2.4, i * 4 + 4, 16] for f in ("north", "south", "east", "west")} | {"up": [i * 4, 0, i * 4 + 4, 1], "down": [i * 4, 15, i * 4 + 4, 16]}))
            x += w + rr.choice((0.4, 0.8, 1.6))
    model("can_shelf", {"w": "m_wood", "c": "m_cans"}, [
        box((0, 0, 15), (16, 16, 16), "w"), box((0, 0, 8), (16, 1, 15), "w"), box((0, 8, 8), (16, 9, 15), "w"),
        box((0, 0, 8), (0.8, 16, 15), "w"), box((15.2, 0, 8), (16, 16, 15), "w")] + cans)
    facing_state("can_shelf", [("", "can_shelf")])

    model("mirror", {"w": "m_wood_dark", "g": "m_mirror"}, [
        box((1, 0, 15), (15, 16, 16), "w"), box((1, 0, 14.4), (2.2, 16, 15), "w"), box((13.8, 0, 14.4), (15, 16, 15), "w"),
        box((2.2, 0, 14.6), (13.8, 16, 15), "g")])
    facing_state("mirror", [("", "mirror")])

    model("camcorder", {"b": "m_black", "m": "m_metal", "l": "cam_lens"}, [
        box((5, 0, 5), (11, 5, 13), "b"), box((6.5, 1, 3), (9.5, 4, 5), "m", faces={"north": "l"}),
        box((11, 2.5, 8), (12.5, 5.5, 12), "m"), box((6, 5, 7), (10, 5.8, 11), "m"), box((4.2, 1, 9), (5, 4, 12), "b")])
    facing_state("camcorder", [("", "camcorder")])

    model("hat_box", {"s": "m_stripes", "p": "m_paper"}, [
        box((2.5, 0, 2.5), (13.5, 7, 13.5), "s"), box((2, 7, 2), (14, 9, 14), "p"), box((7.2, 9, 7.2), (8.8, 9.6, 8.8), "s")])
    facing_state("hat_box", [("", "hat_box")])

    block = [box((5.5, 0, 5.5), (10.5, 2, 10.5), "w"), box((5, 2, 5), (11, 8, 11), "w")]
    hats = {
        0: [box((2, 8, 2), (14, 9, 14), "h"), box((4.5, 9, 4.5), (11.5, 16, 11.5), "h", uv={f: [0, 2, 16, 16] for f in ("north", "south", "east", "west")})],
        1: [box((3, 8, 3), (13, 9, 13), "h"), box((5, 9, 5), (11, 13, 11), "h", uv={f: [0, 8, 16, 16] for f in ("north", "south", "east", "west")})],
        2: [box((0.5, 8, 0.5), (15.5, 8.8, 15.5), "h", uv={f: [0, 0, 16, 2] for f in ("north", "south", "east", "west")}),
            box((4.5, 8.8, 4.5), (11.5, 13.2, 11.5), "h", uv={f: [0, 7, 16, 16] for f in ("north", "south", "east", "west")})],
        3: [],
        4: [box((4, 8, 4), (12, 10, 12), "g"), box((5, 10, 5), (11, 13, 11), "g")],
        5: [box((5, 8, 5), (11, 10, 11), "s"), box((6, 10, 6), (10, 13, 10), "s"), box((7, 13, 7), (9, 16, 9), "s"), box((7.5, 16, 7.5), (8.5, 17, 8.5), "g")],
    }
    for i in range(6):
        model(f"hat_stand_{i}", {"w": "m_wood_light", "h": "m_hat", "g": "m_felt_grey", "s": "m_stripes"}, block + hats[i])
    facing_state("hat_stand", [(f"variant={i}", f"hat_stand_{i}") for i in range(6)])

    model("recorder", {"b": "m_black", "m": "m_metal", "r": "m_brass"}, [
        box((3, 0, 5), (13, 3, 12), "b"), box((4.5, 3, 6.2), (7.5, 3.6, 9.2), "m"), box((8.5, 3, 6.2), (11.5, 3.6, 9.2), "m"),
        box((4, 3, 10), (5.5, 3.5, 11.3), "r"), box((6, 3, 10), (7.5, 3.5, 11.3), "m"), box((8, 3, 10), (9.5, 3.5, 11.3), "m")])
    facing_state("recorder", [("", "recorder")])

    model("name_plate", {"r": "m_brass", "m": "m_metal"}, [
        box((3.5, 4, 15.5), (12.5, 8, 16), "r"), box((7.4, 11, 14.4), (8.6, 12.2, 16), "m"), box((7.4, 10, 14.4), (8.6, 11, 15), "m")])
    facing_state("name_plate", [("", "name_plate")])

    model("streamer", {"s": "streamer"}, [
        box((0, 0, 7.9), (16, 16, 8.1), "s", faces={"east": None, "west": None, "up": None, "down": None}, shade_=False),
        box((7.9, 0, 0), (8.1, 16, 16), "s", faces={"north": None, "south": None, "up": None, "down": None}, shade_=False)], cutout=True)
    facing_state("streamer", [("", "streamer")])

    model("piano", {"w": "m_wood_dark", "k": "piano_keys", "b": "m_black"}, [
        box((0, 0, 6), (16, 16, 16), "w"), box((0, 7, 2), (16, 9, 6), "w", faces={"up": "k"}),
        box((0, 0, 2), (1.5, 7, 6), "w"), box((14.5, 0, 2), (16, 7, 6), "w"), box((3, 11, 5.5), (13, 14, 6), "b")])
    facing_state("piano", [("", "piano")])

    model("bathtub", {"p": "m_porcelain", "m": "m_metal"}, [
        box((0, 2, 1), (16, 4, 15), "p"), box((0, 4, 1), (16, 9, 2.5), "p"), box((0, 4, 13.5), (16, 9, 15), "p"),
        box((0, 4, 2.5), (1.5, 9, 13.5), "p"), box((1, 0, 2), (3, 2, 4), "m"), box((1, 0, 12), (3, 2, 14), "m"),
        box((13, 0, 2), (15, 2, 4), "m"), box((13, 0, 12), (15, 2, 14), "m")])
    facing_state("bathtub", [("", "bathtub")])

    def desk(open_):
        z = -5 if open_ else 1
        return [box((0, 14, 0), (16, 16, 16), "w"), box((0, 0, 1), (5, 14, 16), "w", faces={"north": "d"}),
                box((11, 0, 1), (16, 14, 16), "w", faces={"north": "d"}), box((5, 5, 14), (11, 14, 16), "w"),
                box((5, 10, z), (11, 14, z + 12), "w", faces={"north": "f", "up": "p" if open_ else "w"})]
    model("desk", {"w": "m_wood", "d": "drawers", "f": "drawer_front", "p": "m_paper"}, desk(False))
    model("desk_open", {"w": "m_wood", "d": "drawers", "f": "drawer_front", "p": "m_paper"}, desk(True))
    facing_state("desk", [("open=false", "desk"), ("open=true", "desk_open")])

    def cab(open_):
        e = [box((1, 0, 3), (15, 15, 16), "w", faces={"north": "d"}), box((0, 15, 2), (16, 16, 16), "w")]
        if open_:
            e.append(box((2, 5.5, -3), (14, 10, 6), "w", faces={"north": "f", "up": "p"}))
        return e
    model("cabinet", {"w": "m_wood_dark", "d": "drawers", "f": "drawer_front", "p": "m_black"}, cab(False))
    model("cabinet_open", {"w": "m_wood_dark", "d": "drawers", "f": "drawer_front", "p": "m_black"}, cab(True))
    facing_state("cabinet", [("open=false", "cabinet"), ("open=true", "cabinet_open")])

    model("safe", {"m": "m_metal", "d": "safe_door"}, [box((1, 0, 3), (15, 16, 16), "m", faces={"north": "d"}), box((7, 7, 2), (9, 9, 3), "m")])
    model("safe_open", {"m": "m_metal", "d": "safe_door", "i": "safe_inside"}, [
        box((1, 0, 3), (15, 16, 16), "m", faces={"north": "i"}), box((1, 1, -9), (2, 15, 3), "m", faces={"east": "d", "west": "d"})])
    facing_state("safe", [("open=false", "safe"), ("open=true", "safe_open")])

    model("trunk", {"w": "m_wood", "m": "m_metal", "r": "m_brass"}, [
        box((1, 0, 2), (15, 7, 14), "w"), box((1, 7, 2), (15, 10, 14), "w"), box((0.8, 0, 4), (15.2, 10.2, 5), "m"),
        box((0.8, 0, 11), (15.2, 10.2, 12), "m"), box((7, 5.5, 1.5), (9, 8, 2), "r")])
    model("trunk_open", {"w": "m_wood", "m": "m_metal", "r": "m_brass", "i": "m_black"}, [
        box((1, 0, 2), (15, 7, 14), "w", faces={"up": "i"}), box((1, 7, 12), (15, 19, 14.5), "w"),
        box((0.8, 0, 4), (15.2, 7.2, 5), "m"), box((0.8, 0, 11), (15.2, 7.2, 12), "m")])
    facing_state("trunk", [("open=false", "trunk"), ("open=true", "trunk_open")])

    model("wardrobe_lower", {"w": "m_wood_dark", "d": "wardrobe_low"}, [box((0, 0, 1), (16, 16, 16), "w", faces={"north": "d"}), box((0, 0, 0.5), (16, 1.5, 1), "w")])
    model("wardrobe_upper", {"w": "m_wood_dark", "d": "wardrobe_high"}, [box((0, 0, 1), (16, 14, 16), "w", faces={"north": "d"}), box((-0.5, 14, 0), (16.5, 16, 16), "w")])
    facing_state("wardrobe", [("half=lower", "wardrobe_lower"), ("half=upper", "wardrobe_upper")])

    model("clock_lower", {"w": "m_wood_dark", "b": "clock_body"}, [box((4, 0, 7), (12, 16, 16), "w", faces={"north": "b"}), box((3, 0, 6), (13, 3, 16), "w")])
    model("clock_upper", {"w": "m_wood_dark", "b": "clock_body", "f": "clock_face"}, [
        box((4, 0, 7), (12, 8, 16), "w", faces={"north": "b"}), box((3, 8, 6), (13, 17, 16), "w", faces={"north": "f"}), box((2.5, 17, 5.5), (13.5, 18.5, 16), "w")])
    facing_state("clock", [("half=lower", "clock_lower"), ("half=upper", "clock_upper")])

    model("mannequin_lower", {"w": "m_wood_dark", "c": "m_cloth_pale"}, [
        box((5, 0, 5), (11, 1, 11), "w"), box((7.4, 1, 7.4), (8.6, 11, 8.6), "w"), box((5, 11, 6), (11, 16, 10.5), "c")])
    model("mannequin_upper", {"w": "m_wood_dark", "c": "m_cloth_pale"}, [
        box((5, 0, 6), (11, 4, 10.5), "c"), box((4.5, 4, 5.8), (11.5, 10, 10.8), "c"), box((3.5, 8, 6.2), (12.5, 11, 10.4), "c"),
        box((7, 11, 7.2), (9, 13, 9.2), "w"), box((6.6, 13, 6.8), (9.4, 13.8, 9.6), "w")])
    facing_state("mannequin", [("half=lower", "mannequin_lower"), ("half=upper", "mannequin_upper")])

    for i in range(6):
        model(f"wall_mask_{i}", {"m": f"mask_{i}", "b": "m_black"}, [
            box((4, 3, 15.2), (12, 13, 15.6), "m", faces={"south": "b", "east": None, "west": None, "up": None, "down": None}),
            box((7.4, 6.5, 14.6), (8.6, 9, 15.2), "m", uv={f: [7, 6, 9, 9] for f in ("north", "east", "west", "up", "down")})], cutout=True)
    facing_state("wall_mask", [(f"variant={i}", f"wall_mask_{i}") for i in range(6)])

    flat = {"north": None, "south": None, "east": None, "west": None, "down": None}
    model("paper_0", {"p": "paper_note"}, [box((3, 0, 3), (13, 0.2, 13), "p", faces=flat, rot={"origin": [8, 0, 8], "axis": "y", "angle": 22.5})])
    model("paper_1", {"p": "paper_pile", "q": "paper_note"}, [
        box((2, 0, 3), (12, 0.2, 13), "p", faces=flat), box((4, 0.2, 2), (14, 0.4, 12), "q", faces=flat, rot={"origin": [8, 0, 8], "axis": "y", "angle": -22.5}),
        box((3, 0.4, 4), (13, 0.6, 14), "p", faces=flat, rot={"origin": [8, 0, 8], "axis": "y", "angle": 45})])
    model("paper_2", {"p": "paper_book", "c": "m_wood_dark"}, [box((2.5, 0, 4), (13.5, 1, 12), "c", faces={"up": "p"}), box((7.7, 1, 4), (8.3, 1.3, 12), "c")])
    model("paper_3", {"p": "paper_photo"}, [box((4, 0, 4.5), (12, 0.2, 11.5), "p", faces=flat, rot={"origin": [8, 0, 8], "axis": "y", "angle": -22.5})])
    model("paper_4", {"p": "paper_plate", "m": "m_porcelain"}, [box((4, 0, 4), (12, 0.6, 12), "m", faces={"up": "p"})], cutout=True)
    model("paper_5", {"p": "paper_news"}, [box((2, 0, 3), (14, 0.4, 12), "p", faces=flat)])
    facing_state("paper", [(f"variant={i}", f"paper_{i}") for i in range(6)])

    for mode, t in ((0, "tv_off"), (1, "tv_static"), (2, "tv_on")):
        model(f"crt_tv_{mode}", {"w": "m_wood", "s": t, "m": "m_metal"}, [
            box((1, 0, 3), (15, 13, 15), "w", faces={"north": "s"}), box((2, 13, 6), (14, 13.6, 13), "w"),
            box((5, 13, 9), (5.5, 20, 9.5), "m", rot={"origin": [5, 13, 9], "axis": "z", "angle": 22.5}),
            box((10.5, 13, 9), (11, 20, 9.5), "m", rot={"origin": [11, 13, 9], "axis": "z", "angle": -22.5})])
    facing_state("crt_tv", [(f"mode={i}", f"crt_tv_{i}") for i in range(3)])

    for mode in range(3):
        lift = 1.2 if mode == 1 else 0
        model(f"phone_{mode}", {"b": "m_black", "t": "phone_top", "r": "m_brass"}, [
            box((4, 0, 5), (12, 3, 12), "b", faces={"up": "t"}), box((5, 3, 5.5), (6.5, 4.5, 7), "r"), box((9.5, 3, 5.5), (11, 4.5, 7), "r"),
            box((3, 4.2 + lift, 5.2), (13, 5.6 + lift, 7.2), "b"), box((3, 3.2 + lift, 5), (5.4, 4.4 + lift, 7.4), "b"), box((10.6, 3.2 + lift, 5), (13, 4.4 + lift, 7.4), "b")])
    facing_state("phone", [(f"mode={i}", f"phone_{i}") for i in range(3)])

    for mode in range(3):
        model(f"generator_{mode}", {"g": "m_metal_green", "p": "generator_on" if mode else "generator_off", "m": "m_metal", "b": "m_black"}, [
            box((1, 0, 2), (15, 10, 14), "g", faces={"north": "p"}), box((3, 10, 4), (13, 14, 12), "m"),
            box((6.5, 14, 6.5), (9.5, 15.5, 9.5), "b"), box((0, 0, 3), (1, 3, 5), "b"), box((15, 0, 3), (16, 3, 5), "b"),
            box((0, 0, 11), (1, 3, 13), "b"), box((15, 0, 11), (16, 3, 13), "b"), box((13, 10, 5), (14, 16, 6), "m")])
    facing_state("generator", [(f"mode={i}", f"generator_{i}") for i in range(3)])

    for mode in range(3):
        fig = [box((7.5, 4, 7), (8.5, 7.5, 8), "s", rot={"origin": [8, 4, 7.5], "axis": "y", "angle": 45 if mode == 1 else 0})]
        model(f"music_box_{mode}", {"w": "m_wood_light", "r": "m_brass", "s": "m_stripes", "f": "m_fabric"}, [
            box((4, 0, 5), (12, 4, 11), "w", faces={"up": "f"}), box((4, 4, 10.4), (12, 9, 11), "w"), box((7.5, 1.5, 4.6), (8.5, 2.5, 5), "r")] + fig)
    facing_state("music_box", [(f"mode={i}", f"music_box_{i}") for i in range(3)])

    for mode, t in ((0, "lamp_off"), (1, "lamp_on"), (2, "lamp_dim")):
        model(f"wall_lamp_{mode}", {"r": "m_brass", "g": t}, [
            box((6.5, 3, 15), (9.5, 7, 16), "r"), box((7.4, 4.5, 11), (8.6, 5.7, 15), "r"), box((7.4, 5.7, 11), (8.6, 7.5, 12.2), "r"),
            box((5.5, 7.5, 9), (10.5, 12.5, 14), "g", shade_=mode == 0), box((6.5, 12.5, 10), (9.5, 13.2, 13), "r")])
        model(f"ceiling_lamp_{mode}", {"r": "m_brass", "g": t}, [
            box((7.4, 11, 7.4), (8.6, 16, 8.6), "r"), box((5, 10, 5), (11, 11, 11), "r"),
            box((4, 5, 4), (12, 10, 12), "g", shade_=mode == 0), box((6.5, 4, 6.5), (9.5, 5, 9.5), "g", shade_=mode == 0)])
    facing_state("wall_lamp", [(f"mode={i}", f"wall_lamp_{i}") for i in range(3)])
    facing_state("ceiling_lamp", [(f"mode={i}", f"ceiling_lamp_{i}") for i in range(3)])

    for name, mdl in ITEMS.items():
        W(f"models/item/{name}.json", {"parent": "hatman:block/" + mdl})


def gen_items():
    def blank():
        return Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    def key(col, teeth):
        img = blank()
        d = ImageDraw.Draw(img)
        d.ellipse([1, 1, 7, 7], outline=col + (255,)); d.ellipse([3, 3, 5, 5], fill=(0, 0, 0, 0))
        d.line([(6, 6), (13, 13)], fill=col + (255,), width=1); d.line([(7, 6), (14, 13)], fill=(cl(col[0] - 40), cl(col[1] - 40), cl(col[2] - 40), 255))
        for i in range(teeth):
            d.line([(11 + i, 13 - i), (11 + i - 2, 15 - i)], fill=col + (255,))
        return img

    items = {}
    img = blank(); d = ImageDraw.Draw(img)
    d.rectangle([2, 5, 11, 12], fill=(34, 34, 38, 255), outline=(14, 14, 16, 255)); d.rectangle([11, 6, 14, 10], fill=(60, 60, 66, 255))
    d.ellipse([12, 7, 14, 9], fill=(20, 30, 60, 255)); d.rectangle([4, 3, 8, 5], fill=(50, 50, 56, 255)); d.point((4, 7), fill=(255, 40, 40, 255))
    d.line([(3, 9), (9, 9)], fill=(70, 70, 76, 255))
    items["camcorder"] = img
    img = blank(); d = ImageDraw.Draw(img)
    d.rectangle([5, 3, 10, 13], fill=(196, 150, 40, 255), outline=(60, 44, 10, 255)); d.rectangle([6, 1, 9, 3], fill=(150, 150, 150, 255))
    d.rectangle([5, 9, 10, 13], fill=(30, 30, 34, 255)); d.line([(7, 5), (8, 5)], fill=(255, 240, 200, 255)); d.line([(7, 4), (7, 6)], fill=(255, 240, 200, 255))
    items["battery"] = img
    items["cellar_key"] = key((120, 122, 124), 2)
    items["workshop_key"] = key((176, 140, 60), 3)
    items["stair_key"] = key((200, 170, 90), 1)
    items["attic_key"] = key((70, 60, 54), 2)
    items["cold_key"] = key((150, 190, 200), 2)
    img = blank(); d = ImageDraw.Draw(img)
    d.rectangle([3, 4, 12, 14], fill=(150, 34, 28, 255), outline=(60, 12, 10, 255)); d.rectangle([4, 2, 7, 4], fill=(40, 40, 40, 255))
    d.line([(9, 4), (12, 2)], fill=(40, 40, 40, 255)); d.rectangle([5, 7, 10, 11], fill=(210, 190, 120, 255))
    items["fuel_can"] = img
    img = blank(); d = ImageDraw.Draw(img)
    d.rectangle([2, 2, 13, 14], fill=(70, 30, 24, 255), outline=(30, 12, 10, 255)); d.rectangle([4, 4, 11, 7], fill=(176, 140, 60, 255))
    d.line([(13, 3), (13, 13)], fill=(200, 190, 160, 255)); d.line([(5, 10), (10, 10)], fill=(176, 140, 60, 255))
    items["ledger"] = img
    img = blank(); d = ImageDraw.Draw(img)
    d.rectangle([1, 4, 14, 12], fill=(30, 30, 34, 255), outline=(10, 10, 12, 255)); d.rectangle([3, 5, 12, 9], fill=(200, 190, 160, 255))
    d.ellipse([4, 6, 6, 8], fill=(20, 20, 22, 255)); d.ellipse([9, 6, 11, 8], fill=(20, 20, 22, 255)); d.line([(4, 11), (11, 11)], fill=(70, 70, 76, 255))
    items["recorder_tape"] = img
    img = blank(); d = ImageDraw.Draw(img)
    d.polygon([(8, 1), (14, 3), (13, 10), (8, 15), (3, 10), (2, 3)], fill=(176, 140, 60, 255), outline=(80, 60, 20, 255))
    d.polygon([(8, 4), (9, 7), (12, 7), (10, 9), (11, 12), (8, 10), (5, 12), (6, 9), (4, 7), (7, 7)], fill=(230, 210, 140, 255))
    items["badge"] = img
    img = blank(); d = ImageDraw.Draw(img)
    d.rectangle([5, 6, 10, 14], fill=(120, 100, 40, 200), outline=(60, 50, 20, 255)); d.rectangle([6, 2, 9, 6], fill=(150, 150, 140, 255))
    d.rectangle([6, 9, 9, 12], fill=(200, 190, 150, 255)); d.rectangle([7, 1, 8, 2], fill=(100, 70, 40, 255))
    items["lamp_oil"] = img
    img = blank(); d = ImageDraw.Draw(img)
    d.rectangle([5, 7, 10, 14], fill=(150, 150, 156, 255), outline=(60, 60, 64, 255)); d.rectangle([5, 5, 10, 7], fill=(100, 100, 106, 255))
    d.polygon([(7, 5), (9, 3), (8, 1), (7, 3)], fill=(255, 170, 40, 255)); d.point((8, 3), fill=(255, 240, 160, 255))
    items["lighter"] = img
    img = blank(); d = ImageDraw.Draw(img)
    d.ellipse([2, 1, 13, 15], fill=(206, 198, 184, 255), outline=(110, 100, 90, 255))
    d.ellipse([4, 5, 6, 7], fill=(10, 8, 8, 255)); d.ellipse([9, 5, 11, 7], fill=(10, 8, 8, 255))
    d.line([(5, 11), (10, 11)], fill=(60, 30, 30, 255))
    for x in (5, 7, 9):
        d.line([(x, 10), (x, 12)], fill=(50, 40, 36, 255))
    d.line([(8, 1), (7, 4)], fill=(120, 110, 100, 255))
    items["original_face"] = img
    for k, v in items.items():
        save(v, "item/" + k)
        W(f"models/item/{k}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "hatman:item/" + k}})
    W("models/item/hatman_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})


def gen_gui():
    for i in range(4):
        r = random.Random(900 + i)
        img = Image.new("RGBA", (64, 64))
        px = img.load()
        for y in range(64):
            row = r.randint(-30, 30)
            for x in range(64):
                v = cl(128 + r.randint(-120, 120) + row)
                px[x, y] = (v, v, v, r.randint(60, 255))
        save(img, f"gui/noise_{i}")
    img = Image.new("RGBA", (256, 256))
    px = img.load()
    for y in range(256):
        for x in range(256):
            dx, dy = (x - 127.5) / 127.5, (y - 127.5) / 127.5
            d = math.sqrt(dx * dx + dy * dy * 1.1)
            a = max(0.0, min(1.0, (d - 0.42) / 0.75))
            px[x, y] = (0, 0, 0, cl(255 * a * a))
    save(img, "gui/vignette")
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for y in range(0, 64, 8):
        d.rectangle([0, y, 63, y + 5], fill=(6, 4, 3, 255))
        d.line([(0, y + 5), (63, y + 5)], fill=(30, 20, 14, 255))
        d.rectangle([0, y + 6, 63, y + 7], fill=(0, 0, 0, 70))
    save(img, "gui/slats")
    # aged paper with a torn edge
    r = random.Random(77)
    img = Image.new("RGBA", (256, 320))
    px = img.load()
    for y in range(320):
        for x in range(256):
            n = r.randint(-7, 7)
            edge = min(x, y, 255 - x, 319 - y)
            k = -min(40, max(0, 26 - edge)) * 1.4
            px[x, y] = (cl(214 + n + k), cl(200 + n + k), cl(166 + n + k * 1.2), 255)
    stains(img, 16, (120, 90, 40), 78, (8, 34), 70)
    stains(img, 6, (60, 40, 20), 79, (3, 10), 90)
    px = img.load()
    for x in range(256):
        t = int(3 + 3 * math.sin(x / 9.0) + r.randint(0, 3))
        b = int(3 + 3 * math.sin(x / 7.0 + 2) + r.randint(0, 3))
        for y in range(t):
            px[x, y] = (0, 0, 0, 0)
        for y in range(320 - b, 320):
            px[x, y] = (0, 0, 0, 0)
    for y in range(320):
        l = int(2 + 2 * math.sin(y / 11.0) + r.randint(0, 2))
        rr = int(2 + 2 * math.sin(y / 8.0 + 1) + r.randint(0, 2))
        for x in range(l):
            px[x, y] = (0, 0, 0, 0)
        for x in range(256 - rr, 256):
            px[x, y] = (0, 0, 0, 0)
    save(img, "gui/paper")
    # the last thing you see
    img = Image.new("RGB", (512, 384), (0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([150, 60, 362, 400], fill=(168, 160, 148))
    d.polygon([(200, 400), (256, 330), (312, 400)], fill=(120, 112, 104))
    for cx in (210, 302):
        for rad, c in ((46, (60, 54, 50)), (36, (20, 16, 16)), (26, (0, 0, 0))):
            d.ellipse([cx - rad, 190 - rad * 1.15, cx + rad, 190 + rad * 1.15], fill=c)
        d.ellipse([cx - 3, 187, cx + 3, 193], fill=(255, 255, 255))
    d.polygon([(150, 300), (190, 322), (256, 330), (322, 322), (362, 300), (330, 362), (256, 384), (182, 362)], fill=(6, 0, 0))
    for x in range(176, 340, 16):
        d.line([(x, 296), (x + 3, 372)], fill=(60, 44, 40), width=3)
    d.polygon([(40, 96), (472, 96), (512, 130), (0, 130)], fill=(4, 4, 5))
    d.rectangle([150, 0, 362, 100], fill=(6, 6, 7))
    d.rectangle([150, 78, 362, 98], fill=(70, 10, 12))
    img = img.filter(ImageFilter.GaussianBlur(2.2))
    px = img.load()
    r = random.Random(13)
    for y in range(384):
        shift = r.randint(-18, 18) if r.random() < 0.06 else 0
        for x in range(512):
            c = px[(x + shift) % 512, y]
            n = r.randint(-26, 26)
            s = 0.74 if y % 3 == 0 else 1.0
            px[x, y] = (cl((c[0] + n) * s * 1.06), cl((c[1] + n) * s * 0.96), cl((c[2] + n) * s * 0.92))
    save(img.convert("RGBA"), "gui/jumpscare")


VSH = """#version 150
in vec4 Position;
uniform mat4 ProjMat;
uniform vec2 InSize;
uniform vec2 OutSize;
out vec2 texCoord;
void main() {
    vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);
    texCoord = Position.xy / OutSize;
}
"""
NV_FSH = """#version 150
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Time;
in vec2 texCoord;
out vec4 fragColor;
float hash(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
void main() {
    float t = Time * 20.0;
    vec2 uv = texCoord;
    // tape wobble: every few scanlines slip sideways for a frame
    float band = floor(uv.y * InSize.y / 4.0);
    float slip = step(0.985, hash(vec2(band, floor(t * 2.0)))) * (hash(vec2(band, t)) - 0.5) * 0.02;
    vec3 c = texture(DiffuseSampler, uv + vec2(slip, 0.0)).rgb;
    // cheap bloom so lamps bleed like an image intensifier tube
    vec2 px = 2.5 / InSize;
    vec3 b = texture(DiffuseSampler, uv + vec2(px.x, 0.0)).rgb + texture(DiffuseSampler, uv - vec2(px.x, 0.0)).rgb
           + texture(DiffuseSampler, uv + vec2(0.0, px.y)).rgb + texture(DiffuseSampler, uv - vec2(0.0, px.y)).rgb;
    float l = dot(c, vec3(0.30, 0.59, 0.11));
    float lb = dot(b * 0.25, vec3(0.30, 0.59, 0.11));
    l = pow(l, 0.72) * 1.18 + max(0.0, lb - 0.55) * 0.9;
    float n = hash(uv * InSize + vec2(t * 17.0, t * 31.0));
    l += (n - 0.5) * 0.17;
    float scan = 0.86 + 0.14 * sin(uv.y * InSize.y * 1.57);
    vec2 d = uv - 0.5;
    float vig = smoothstep(0.80, 0.22, length(d * vec2(1.0, 1.12)));
    vec3 g = vec3(0.12, 1.0, 0.26) * l * scan;
    g += vec3(0.5, 0.6, 0.5) * max(0.0, l - 0.92) * 2.0;
    g = g * (0.18 + 0.82 * vig) + vec3(0.0, 0.022, 0.004);
    fragColor = vec4(g, 1.0);
}
"""
FILM_FSH = """#version 150
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Time;
in vec2 texCoord;
out vec4 fragColor;
float hash(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
void main() {
    float t = Time * 20.0;
    vec3 c = texture(DiffuseSampler, texCoord).rgb;
    float l = dot(c, vec3(0.30, 0.59, 0.11));
    // drained, cold, with crushed blacks
    c = mix(vec3(l), c, 0.62);
    c *= vec3(0.94, 0.98, 1.05);
    c = max(vec3(0.0), c - 0.012) * 1.04;
    float n = hash(texCoord * InSize + vec2(t * 13.0, t * 29.0));
    c += (n - 0.5) * 0.045;
    vec2 d = texCoord - 0.5;
    c *= 0.55 + 0.45 * smoothstep(0.86, 0.30, length(d));
    fragColor = vec4(c, 1.0);
}
"""


def gen_shaders():
    base = OUT + "/shaders"
    os.makedirs(base + "/program", exist_ok=True)
    os.makedirs(base + "/post", exist_ok=True)
    for name, fsh in (("nv", NV_FSH), ("film", FILM_FSH)):
        with open(f"{base}/program/{name}.vsh", "w") as f:
            f.write(VSH)
        with open(f"{base}/program/{name}.fsh", "w") as f:
            f.write(fsh)
        with open(f"{base}/program/{name}.json", "w") as f:
            json.dump({"blend": {"func": "add", "srcrgb": "one", "dstrgb": "zero"}, "vertex": "hatman:" + name, "fragment": "hatman:" + name,
                       "attributes": ["Position"], "samplers": [{"name": "DiffuseSampler"}],
                       "uniforms": [{"name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]},
                                    {"name": "InSize", "type": "float", "count": 2, "values": [1, 1]},
                                    {"name": "OutSize", "type": "float", "count": 2, "values": [1, 1]},
                                    {"name": "Time", "type": "float", "count": 1, "values": [0]}]}, f, indent=1)
        with open(f"{base}/post/{name}.json", "w") as f:
            json.dump({"targets": ["swap"], "passes": [{"name": "hatman:" + name, "intarget": "minecraft:main", "outtarget": "swap"},
                                                       {"name": "blit", "intarget": "swap", "outtarget": "minecraft:main"}]}, f, indent=1)


if __name__ == "__main__":
    gen_textures()
    gen_models()
    gen_items()
    gen_gui()
    gen_shaders()
    print("assets ok")
