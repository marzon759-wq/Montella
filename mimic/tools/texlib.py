"""Procedural textures for the farm: weathered wood, peeling paint, rust, straw, gore. 32px unless noted."""
import math, random
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

N = 32


def rng(seed):
    return np.random.default_rng(seed)


def noise(w, h, scale, seed, octaves=4, tile=True):
    """tileable value noise in 0..1"""
    r = rng(seed)
    out = np.zeros((h, w))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        cw, ch = max(2, int(w / scale * 2 ** o)), max(2, int(h / scale * 2 ** o))
        g = r.random((ch, cw))
        if tile:
            g = np.pad(g, ((0, 1), (0, 1)), mode="wrap")
            img = Image.fromarray((g * 255).astype(np.uint8)).resize((w + w // cw + 1, h + h // ch + 1), Image.BICUBIC)
            a = np.asarray(img, float)[:h, :w] / 255
        else:
            a = np.asarray(Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC), float) / 255
        out += a * amp
        tot += amp
        amp *= 0.5
    return out / tot


def ramp(a, lo, hi):
    lo, hi = np.array(lo, float), np.array(hi, float)
    a = np.clip(a, 0, 1)[..., None]
    rgb = lo + (hi - lo) * a
    return rgb


def img(rgb, alpha=None):
    rgb = np.clip(rgb, 0, 255).astype(np.uint8)
    if alpha is None:
        alpha = np.full(rgb.shape[:2], 255, np.uint8)
    return Image.fromarray(np.dstack([rgb, np.clip(alpha, 0, 255).astype(np.uint8)]), "RGBA")


def arr(im):
    return np.asarray(im.convert("RGBA"), float)


def grain(a, seed, amt=10):
    r = rng(seed)
    a = a.copy()
    a[..., :3] += r.normal(0, amt, a.shape[:2])[..., None]
    return a


def boards(w, h, base, seed, n=4, vertical=True, gap=(14, 10, 8), wear=0.5, knots=2):
    """wood boards with grain streaks, dark gaps and nail heads"""
    r = random.Random(seed)
    W, H = (w, h) if vertical else (h, w)
    a = np.zeros((H, W, 3))
    edges = [0]
    while edges[-1] < W:
        edges.append(edges[-1] + max(4, W // n + r.randint(-2, 2)))
    edges[-1] = W
    for i in range(len(edges) - 1):
        x0, x1 = edges[i], edges[i + 1]
        tone = r.uniform(-18, 14)
        g = noise(x1 - x0, H, 3, seed + i * 7, 3, tile=False)
        streak = noise(x1 - x0, H, 1.2, seed + i * 13, 2, tile=False)
        y = np.linspace(0, 1, H)[:, None]
        lines = (np.sin((np.arange(x1 - x0)[None, :] * 2.2 + streak * 6 + y * 3) * 1.7) * 0.5 + 0.5) ** 3
        v = g * 0.6 + lines * 0.4
        col = np.array(base, float) + tone
        a[:, x0:x1] = col * (0.72 + 0.5 * v[..., None])
        a[:, x0] = np.array(gap, float)  # gap between boards
        for _ in range(knots if r.random() < 0.6 else 0):
            ky, kx = r.randrange(H), r.randrange(x0 + 1, max(x0 + 2, x1 - 1))
            for yy in range(max(0, ky - 2), min(H, ky + 3)):
                for xx in range(max(x0 + 1, kx - 1), min(x1, kx + 2)):
                    a[yy, xx] = a[yy, xx] * 0.55
        # end cuts and nails
        cut = r.randrange(H)
        a[cut, x0:x1] = a[cut, x0:x1] * 0.45
        for ny in (2, H - 3):
            nx = (x0 + x1) // 2
            if nx < W:
                a[ny, nx] = (40, 36, 34)
    if wear:
        d = noise(W, H, 6, seed + 99, 3)
        a *= (1 - wear * 0.35 * (d > 0.62)[..., None])
    if not vertical:
        a = a.transpose(1, 0, 2)
    return a


def peel(a, paint, seed, amount=0.45, chips=0.6):
    """paint over wood that has flaked off in patches"""
    H, W = a.shape[:2]
    n = noise(W, H, 5, seed, 4)
    m = (n > amount).astype(float)
    m = np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.4)), float) / 255
    pv = noise(W, H, 4, seed + 1, 3)
    pc = np.array(paint, float)[None, None, :] * (0.75 + 0.45 * pv[..., None])
    out = a * (1 - m[..., None]) + pc * m[..., None]
    # keep the wood's dark gaps
    dark = a.sum(-1) < 60
    out[dark] = a[dark]
    return out


def stains(a, n, color, seed, rmin=1, rmax=5, alpha=0.6, drips=False):
    H, W = a.shape[:2]
    ov = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(ov)
    r = random.Random(seed)
    for _ in range(n):
        x, y, rr = r.randrange(W), r.randrange(H), r.uniform(rmin, rmax)
        al = int(255 * alpha * r.uniform(0.5, 1))
        d.ellipse([x - rr, y - rr * r.uniform(0.6, 1.4), x + rr, y + rr * r.uniform(0.6, 1.4)], fill=tuple(color) + (al,))
        if drips and r.random() < 0.7:
            ln = r.randint(3, H // 2)
            d.line([(x, y), (x + r.choice((0, 0, 1)), y + ln)], fill=tuple(color) + (al,), width=max(1, int(rr / 2)))
    ov = ov.filter(ImageFilter.GaussianBlur(0.5))
    o = arr(ov)
    al = o[..., 3:4] / 255
    return a * (1 - al) + o[..., :3] * al


def blood(a, n, seed, drips=True, alpha=0.85, big=4):
    a = stains(a, n, (74, 4, 6), seed, 1, big, alpha, drips)
    a = stains(a, max(1, n // 3), (40, 0, 2), seed + 7, 0.6, big * 0.6, alpha, False)
    return a


def rust(w, h, seed, ribs=0, base=(96, 52, 30)):
    n = noise(w, h, 4, seed, 4)
    a = ramp(n, (52, 28, 18), (150, 86, 46))
    if ribs:
        x = np.arange(w)
        rib = (np.sin(x / w * math.tau * ribs) * 0.5 + 0.5)[None, :, None]
        a = a * (0.75 + 0.45 * rib)
    a = stains(a, 6, (38, 22, 16), seed + 3, 1, 4, 0.5, True)
    return grain(a, seed, 6)


def save(a, path, alpha=None):
    im = img(a, alpha) if isinstance(a, np.ndarray) else a
    im.save(path)


# ---------------------------------------------------------------------------------- the material set
def make_all(out):
    """returns dict name -> RGBA image (32x32 unless noted)"""
    T = {}
    S = N

    def put(name, a, alpha=None):
        T[name] = img(a, alpha) if isinstance(a, np.ndarray) else a

    # ---- wood
    put("m_wood_old", grain(boards(S, S, (104, 92, 80), 11, 3, wear=0.6), 1))
    put("m_wood_dark", grain(boards(S, S, (62, 42, 30), 12, 3), 2))
    put("m_wood_light", grain(boards(S, S, (150, 118, 80), 13, 3), 3))
    put("m_wood_grey", grain(boards(S, S, (96, 92, 86), 14, 4, wear=0.7), 4))
    put("m_wood_beam", grain(boards(S, S, (78, 58, 42), 15, 1, knots=3), 5))
    put("m_wood_red", grain(peel(boards(S, S, (90, 76, 66), 16, 3), (122, 30, 26), 16, 0.4), 6))
    # ---- barn and house shells (full blocks)
    put("barn_wall", grain(peel(boards(S, S, (88, 76, 66), 21, 4, gap=(10, 6, 5)), (118, 28, 24), 21, 0.42), 21, 7))
    put("barn_wall_dark", grain(peel(boards(S, S, (70, 60, 52), 22, 4, gap=(8, 5, 4)), (86, 20, 18), 22, 0.5), 22, 7))
    put("barn_floor", stains(grain(boards(S, S, (92, 78, 60), 23, 4, vertical=False, wear=0.6), 23), 10, (130, 112, 60), 24, 1, 3, 0.5))
    put("siding", grain(boards(S, S, (126, 120, 110), 25, 5, vertical=False, gap=(40, 38, 36), wear=0.5), 25, 6))
    put("siding_dark", grain(boards(S, S, (78, 74, 70), 26, 5, vertical=False, gap=(24, 22, 20)), 26, 6))
    put("floorboards", grain(boards(S, S, (96, 70, 48), 27, 4, vertical=False), 27))
    put("floorboards_dark", grain(boards(S, S, (58, 40, 28), 28, 4, vertical=False), 28))
    a = boards(S, S, (70, 50, 36), 29, 2)
    a[:, :] *= 0.9
    d = np.zeros((S, S))
    a2 = a.copy()
    a2[3:29, 3:29] = a[3:29, 3:29] * 1.12
    a2[3, 3:29] = a2[28, 3:29] = (34, 22, 16)
    a2[3:29, 3] = a2[3:29, 28] = (34, 22, 16)
    put("wainscot", grain(a2, 29))
    # wallpaper: small faded damask, nicotine-yellow, damp stains (RE7)
    W = 64
    base = ramp(noise(W, W, 10, 31, 3), (70, 62, 46), (98, 88, 64))
    im = img(base)
    dr = ImageDraw.Draw(im)
    for cx in range(0, W + 8, 8):
        for cy in range(0, W + 8, 8):
            ox = 4 if (cy // 8) % 2 else 0
            x, y = cx + ox, cy
            col = (58, 48, 34, 255)
            dr.polygon([(x, y - 3), (x + 2, y), (x, y + 3), (x - 2, y)], outline=col)
            dr.point([(x, y)], fill=(104, 70, 50, 255))
    for x in range(0, W, 16):
        dr.line([(x, 0), (x, W)], fill=(54, 46, 32, 255))
    a = arr(im)[..., :3]
    a = a * (0.85 + 0.25 * noise(W, W, 3, 32, 2)[..., None])
    a = stains(a, 5, (44, 36, 20), 33, 4, 14, 0.55, True)
    a = stains(a, 8, (30, 24, 14), 34, 2, 7, 0.45)
    put("wallpaper", grain(a, 35, 4))
    a = stains(a, 6, (58, 6, 6), 36, 2, 5, 0.85, True)
    a2 = a.copy()
    r = random.Random(37)
    for _ in range(5):  # peeled strips showing plaster
        x0, y0, ln, wd = r.randint(0, W - 6), r.randint(0, W // 2), r.randint(14, 40), r.randint(3, 8)
        a2[y0:y0 + ln, x0:x0 + wd] = ramp(noise(wd, min(ln, W - y0), 2, 38 + x0), (150, 140, 120), (184, 174, 150))[: min(ln, W - y0)]
    put("wallpaper_peeled", grain(a2, 39, 4))
    put("wallpaper_bloody", grain(blood(a.copy(), 14, 40, True, 0.9, 6), 41, 4))
    put("plaster", grain(stains(ramp(noise(S, S, 5, 42, 4), (150, 142, 126), (190, 182, 164)), 6, (100, 92, 76), 43, 1, 4, 0.5, True), 42, 5))
    put("plaster_bloody", grain(blood(arr(T["plaster"])[..., :3], 10, 44, True, 0.9, 4), 44, 4))
    put("kitchen_tile", grain(tiles(S, (160, 156, 140), (90, 86, 74), 8, 45), 45, 5))
    put("butcher_tile", grain(blood(tiles(S, (170, 168, 158), (80, 78, 72), 8, 46), 9, 47, True, 0.9, 4), 46, 4))
    put("stone_foundation", grain(stones(S, 48), 48, 6))
    put("cellar_stone", grain(stones(S, 49, (70, 66, 60)), 49, 6))
    put("packed_dirt", grain(ramp(noise(S, S, 4, 50, 4), (52, 40, 28), (92, 74, 52)), 50, 8))
    put("mud", grain(stains(ramp(noise(S, S, 4, 51, 4), (40, 30, 22), (70, 56, 40)), 8, (28, 20, 14), 52, 2, 5, 0.6), 51, 6))
    put("slaughter_floor", grain(blood(ramp(noise(S, S, 4, 53, 4), (70, 66, 60), (110, 104, 96)), 18, 54, False, 0.9, 6), 53, 6))
    put("straw_floor", grain(straw(S, 55, (150, 124, 66)), 55, 6))
    put("hay", grain(straw(S, 56, (176, 146, 74), dense=True), 56, 6))
    a = straw(S, 57, (168, 138, 70), dense=True)
    a[13:16, :] = (64, 48, 30)  # twine bands
    a[:, 7] = a[:, 24] = (60, 44, 28)
    put("hay_bale_side", grain(a, 57, 5))
    put("hay_bale_top", grain(straw(S, 58, (172, 142, 72), dense=True, vertical=False), 58, 5))
    put("shingles", grain(shingles(S, 59), 59, 6))
    put("barn_roof", grain(rust(S, S, 60, ribs=4), 60, 5))
    put("silo_wall", grain(silo(S, 61), 61, 5))
    put("brick", grain(bricks(S, 62), 62, 6))
    a = boards(S, S, (70, 58, 46), 63, 2, vertical=False)
    a[:, :] *= 0.85
    put("boarded_window", grain(a, 63))
    # ---- metals
    put("m_rust", rust(S, S, 70))
    put("m_iron", grain(ramp(noise(S, S, 4, 71, 3), (30, 30, 32), (74, 72, 72)), 71, 5))
    put("m_cast_iron", grain(stains(ramp(noise(S, S, 3, 72, 3), (20, 20, 22), (52, 50, 50)), 6, (80, 46, 28), 73, 1, 3, 0.6), 72, 4))
    put("m_tin", grain(ramp(noise(S, S, 4, 74, 3), (110, 112, 112), (170, 170, 166)), 74, 6))
    put("m_brass", grain(ramp(noise(S, S, 4, 75, 3), (110, 80, 30), (196, 156, 70)), 75, 6))
    put("m_red_paint", grain(stains(ramp(noise(S, S, 4, 76, 3), (110, 22, 18), (158, 40, 30)), 6, (70, 40, 30), 77, 1, 4, 0.7), 76, 5))
    put("m_green_paint", grain(stains(ramp(noise(S, S, 4, 78, 3), (40, 62, 40), (72, 98, 60)), 6, (70, 50, 30), 79, 1, 4, 0.7), 78, 5))
    # ---- organics & gore
    put("m_bone", grain(stains(ramp(noise(S, S, 4, 80, 3), (150, 138, 112), (222, 212, 186)), 5, (110, 80, 50), 81, 1, 3, 0.5), 80, 6))
    put("m_bone_bloody", grain(blood(arr(T["m_bone"])[..., :3], 6, 82, False, 0.9, 3), 82, 4))
    meat = ramp(noise(S, S, 3, 83, 4), (96, 10, 14), (178, 54, 52))
    fat = noise(S, S, 5, 84, 3) > 0.62
    meat[fat] = meat[fat] * 0.4 + np.array((220, 196, 170)) * 0.6
    put("m_flesh", grain(stains(meat, 8, (50, 0, 4), 85, 1, 3, 0.7), 83, 8))
    put("m_fat", grain(ramp(noise(S, S, 4, 86, 3), (196, 170, 140), (236, 220, 196)), 86, 6))
    put("m_blood", grain(ramp(noise(S, S, 3, 87, 3), (36, 0, 2), (98, 6, 10)), 87, 5))
    put("m_skin_dead", grain(stains(ramp(noise(S, S, 4, 88, 3), (120, 108, 96), (168, 150, 134)), 10, (80, 60, 70), 89, 1, 4, 0.5), 88, 5))
    put("m_hide", grain(ramp(noise(S, S, 3, 90, 3), (60, 44, 32), (110, 84, 62)), 90, 8))
    put("m_leather", grain(stains(ramp(noise(S, S, 4, 91, 3), (64, 36, 20), (118, 72, 40)), 6, (40, 22, 12), 92, 1, 3, 0.5), 91, 5))
    put("m_rope", grain(rope(S, 93), 93, 5))
    put("m_canvas", grain(ramp(noise(S, S, 3, 94, 3), (110, 98, 74), (152, 140, 110)), 94, 9))
    put("m_cloth_red", grain(rug(64, 95), 95, 4))
    put("m_rug_runner", grain(rug(64, 96, (60, 54, 70), (120, 104, 60)), 96, 4))
    put("m_fabric_floral", grain(ramp(noise(S, S, 3, 97, 3), (70, 54, 44), (118, 92, 70)), 97, 8))
    put("m_fabric_dirty", grain(stains(ramp(noise(S, S, 3, 98, 3), (130, 120, 102), (170, 160, 140)), 8, (90, 76, 50), 99, 1, 4, 0.6), 98, 6))
    put("m_paper", grain(ramp(noise(S, S, 4, 100, 3), (186, 174, 140), (222, 212, 180)), 100, 4))
    put("m_wax", grain(ramp(noise(S, S, 4, 101, 3), (200, 192, 160), (236, 230, 206)), 101, 3))
    put("m_glass_dirty", grain(ramp(noise(S, S, 4, 102, 3), (40, 46, 46), (90, 96, 92)), 102, 4), np.full((S, S), 170))
    put("m_glass_lit", grain(ramp(noise(S, S, 4, 103, 3), (210, 160, 70), (255, 214, 120)), 103, 4))
    put("m_black", grain(ramp(noise(S, S, 4, 104, 3), (10, 10, 12), (30, 28, 30)), 104, 3))
    put("m_porcelain", grain(stains(ramp(noise(S, S, 4, 105, 3), (190, 186, 172), (226, 222, 210)), 6, (130, 110, 70), 106, 1, 3, 0.5), 105, 4))
    put("m_porcelain_bloody", grain(blood(arr(T["m_porcelain"])[..., :3], 10, 107, True, 0.9, 4), 107, 3))
    put("m_corn_leaf", grain(corn(S, 108), 108, 5))
    put("m_dry_grass", grain(ramp(noise(S, S, 2, 109, 3), (110, 92, 50), (176, 150, 92)), 109, 8))
    put("m_ash", grain(ramp(noise(S, S, 3, 110, 3), (30, 28, 28), (80, 76, 72)), 110, 6))
    put("m_coal_glow", grain(ramp(noise(S, S, 2, 111, 3), (120, 20, 0), (255, 150, 40)), 111, 8))
    put("m_dirt", grain(ramp(noise(S, S, 4, 112, 4), (54, 40, 28), (96, 76, 52)), 112, 8))
    put("m_stone", grain(ramp(noise(S, S, 4, 113, 4), (80, 78, 74), (130, 126, 118)), 113, 8))
    put("m_book", books(S, 114))
    put("m_jars", jars(S, 115))
    put("m_photo", photo(S, 116))
    put("m_painting_0", painting(S, 117, 0))
    put("m_painting_1", painting(S, 118, 1))
    put("m_painting_2", painting(S, 119, 2))
    put("m_painting_3", painting(S, 120, 3))
    put("m_clock_face", clock_face(S))
    put("m_stove_door", stove_door(S, False))
    put("m_stove_door_lit", stove_door(S, True))
    put("m_flame", flame(16, 4))  # animated strip
    put("m_radio_face", radio(S))
    put("m_tape_face", tape(S))
    put("m_handprint", handprint(S, 121), None)
    put("m_cobweb", cobweb(S, 122), None)
    put("m_blood_pool", bloodpool(S, 123), None)
    put("m_curtain", curtain(S, 124), None)
    put("m_window", window(S, False), None)
    put("m_window_lit", window(S, True), None)
    put("m_wire", wire(S), None)
    put("m_drawer_front", drawer(S, 125))
    put("m_crate", crate(S, 126))
    put("m_barrel", barrel(S, 127))
    put("m_lamp_glass_off", ramp(noise(S, S, 4, 128, 2), (60, 54, 40), (100, 92, 70)))
    put("m_lamp_glass_on", ramp(noise(S, S, 4, 129, 2), (255, 190, 90), (255, 236, 170)))
    put("m_door", door(S, 130, False))
    put("m_door_top", door(S, 130, True))
    put("m_barn_door", grain(peel(boards(S, S, (88, 74, 62), 131, 3), (112, 26, 22), 131, 0.38), 131, 6))
    put("m_trapdoor", trapdoor(S, 132))
    put("m_ladder", ladder(S, 133), None)
    put("m_gas_can", grain(stains(ramp(noise(S, S, 4, 134, 3), (120, 14, 12), (178, 34, 24)), 6, (60, 40, 30), 135, 1, 3, 0.6), 134, 5))
    put("m_saddle", grain(stains(ramp(noise(S, S, 3, 136, 3), (70, 36, 18), (130, 80, 44)), 4, (36, 18, 8), 137, 1, 3, 0.5), 136, 5))
    put("m_carcass", grain(carcass(S, 138), 138, 6))
    put("m_antler", grain(ramp(noise(S, S, 3, 139, 3), (110, 96, 76), (190, 176, 150)), 139, 6))
    put("m_scarecrow_cloth", grain(stains(ramp(noise(S, S, 3, 140, 3), (70, 60, 44), (110, 96, 70)), 10, (40, 30, 20), 141, 1, 4, 0.6), 140, 9))
    put("m_burlap_face", burlap_face(S, 142))
    sb = straw(S, 143, (170, 140, 72))
    al = (noise(S, S, 2, 144, 2) > 0.45) * 255.0
    put("m_straw_bits", img(sb, al))
    return T


def tiles(S, c1, grout, n, seed):
    a = ramp(noise(S, S, 4, seed, 3), np.array(c1) * 0.85, c1)
    step = S // (S // n)
    for i in range(0, S, n):
        a[i, :] = grout
        a[:, i] = grout
    return a


def stones(S, seed, base=(96, 90, 82)):
    r = random.Random(seed)
    a = ramp(noise(S, S, 3, seed, 3), np.array(base) * 0.75, np.array(base) * 1.2)
    rows = [0, 7, 15, 22, 32]
    for i in range(len(rows) - 1):
        y0, y1 = rows[i], rows[i + 1]
        a[y0, :] = (34, 32, 30)
        x = r.randint(0, 8)
        while x < S:
            a[y0:y1, x % S] = (34, 32, 30)
            x += r.randint(7, 13)
    return a


def bricks(S, seed):
    a = ramp(noise(S, S, 3, seed, 3), (90, 40, 30), (140, 70, 50))
    for y in range(0, S, 6):
        a[y, :] = (60, 56, 50)
        off = 0 if (y // 6) % 2 == 0 else 6
        for x in range(off, S, 12):
            a[y:y + 6, x] = (60, 56, 50)
    return a


def straw(S, seed, base, dense=False, vertical=True):
    r = random.Random(seed)
    a = np.array(base, float) * 0.55 * np.ones((S, S, 3))
    n = 420 if dense else 220
    for _ in range(n):
        x, y = r.randrange(S), r.randrange(S)
        ln = r.randint(3, 9)
        dx, dy = (r.uniform(-0.6, 0.6), 1) if vertical else (1, r.uniform(-0.6, 0.6))
        if not vertical and r.random() < 0.5:
            dx, dy = r.uniform(-1, 1), r.uniform(-1, 1)
        c = np.array(base, float) * r.uniform(0.75, 1.25)
        for k in range(ln):
            xx, yy = int(x + dx * k) % S, int(y + dy * k) % S
            a[yy, xx] = c
    return a


def shingles(S, seed):
    r = random.Random(seed)
    a = np.zeros((S, S, 3))
    for row in range(4):
        y0 = row * 8
        off = 0 if row % 2 == 0 else 4
        for x0 in range(-8 + off, S, 8):
            tone = r.uniform(-14, 10)
            for y in range(y0, y0 + 8):
                for x in range(max(0, x0), min(S, x0 + 8)):
                    k = (y - y0) / 8
                    a[y, x] = np.array((62, 58, 56)) + tone - 18 * k
            if 0 <= x0 < S:
                a[y0:y0 + 8, x0] = (24, 22, 22)
        a[y0 + 7, :] = (22, 20, 20)
    return stains(a, 6, (60, 70, 40), seed + 1, 1, 3, 0.4)


def silo(S, seed):
    a = rust(S, S, seed, ribs=6)
    for y in range(0, S, 8):
        a[y, :] = a[y, :] * 0.55
        for x in range(2, S, 6):
            a[y + 1 if y + 1 < S else y, x] = (150, 140, 120)
    return a


def rope(S, seed):
    a = np.zeros((S, S, 3))
    for y in range(S):
        for x in range(S):
            v = (math.sin((x + y) * 0.9) * 0.5 + 0.5)
            a[y, x] = np.array((90, 70, 42)) + 50 * v
    return a


def rug(S, seed, c1=(110, 26, 24), c2=(150, 120, 60)):
    a = ramp(noise(S, S, 6, seed, 3), np.array(c1) * 0.7, c1)
    yy, xx = np.mgrid[0:S, 0:S]
    border = (np.minimum(np.minimum(xx, S - 1 - xx), np.minimum(yy, S - 1 - yy)) < 5)
    a[border] = np.array(c2) * 0.7
    inner = (np.minimum(np.minimum(xx, S - 1 - xx), np.minimum(yy, S - 1 - yy)) == 7)
    a[inner] = c2
    cx, cy = S / 2, S / 2
    dia = (np.abs(xx - cx) + np.abs(yy - cy))
    a[(dia > 12) & (dia < 15)] = c2
    a[(dia < 6)] = np.array(c2) * 0.9
    a *= (0.75 + 0.35 * noise(S, S, 3, seed + 1, 2))[..., None]  # wear
    return stains(a, 5, (50, 30, 20), seed + 2, 2, 6, 0.5)


def corn(S, seed):
    r = random.Random(seed)
    a = np.zeros((S, S, 3))
    for x in range(S):
        c = np.array((150, 128, 76)) * r.uniform(0.75, 1.15)
        for y in range(S):
            a[y, x] = c * (0.85 + 0.15 * math.sin(y * 0.4 + x))
    return stains(a, 8, (80, 60, 34), seed + 1, 1, 3, 0.6)


def books(S, seed):
    r = random.Random(seed)
    a = np.zeros((S, S, 3)) + (30, 20, 14)
    for row in (0, 16):
        x = 1
        while x < S - 2:
            w = r.randint(2, 4)
            h = r.randint(10, 15)
            col = np.array(r.choice([(90, 30, 24), (40, 50, 70), (60, 70, 40), (110, 90, 60), (50, 30, 20), (80, 70, 60)]), float) * r.uniform(0.7, 1.1)
            a[row + 15 - h:row + 15, x:x + w] = col
            a[row + 15 - h + 2, x:x + w] = col * 1.4
            x += w
        a[row + 15, :] = (50, 34, 22)
    return a


def jars(S, seed):
    r = random.Random(seed)
    a = np.zeros((S, S, 3)) + (24, 18, 14)
    for row in (0, 16):
        x = 1
        while x < S - 4:
            w = r.randint(4, 6)
            h = r.randint(8, 12)
            col = np.array(r.choice([(140, 40, 30), (90, 110, 40), (160, 120, 40), (80, 30, 50), (120, 60, 50)]), float)
            a[row + 14 - h:row + 14, x:x + w] = col * 0.7
            a[row + 14 - h:row + 15 - h + 1, x:x + w] = (150, 150, 140)
            a[row + 16 - h:row + 13, x + 1] = col * 1.2
            x += w + 1
        a[row + 14:row + 16, :] = (60, 44, 30)
    return a


def photo(S, seed):
    a = ramp(noise(S, S, 3, seed, 3), (60, 54, 44), (150, 140, 120))
    a[:2, :] = a[-2:, :] = a[:, :2] = a[:, -2:] = (210, 204, 190)
    for cx in (11, 20):
        yy, xx = np.mgrid[0:S, 0:S]
        m = (xx - cx) ** 2 + (yy - 12) ** 2 < 14
        a[m] = (190, 176, 150)
        a[(np.abs(xx - cx) < 4) & (yy > 15) & (yy < 28)] = (70, 60, 50)
    return a


def painting(S, seed, kind):
    a = ramp(noise(S, S, 4, seed, 3), (40, 34, 26), (90, 80, 60))
    yy, xx = np.mgrid[0:S, 0:S]
    if kind == 0:  # a farm at dusk
        a[yy > 20] = (70, 60, 30)
        a[(xx > 8) & (xx < 20) & (yy > 12) & (yy < 21)] = (110, 30, 24)
        a[(yy < 12) & (np.abs(xx - 14) < 12 - yy)] = (70, 26, 20)
    elif kind == 1:  # a portrait whose eyes have been scratched out
        m = (xx - 16) ** 2 / 50 + (yy - 13) ** 2 / 80 < 1
        a[m] = (170, 140, 110)
        a[(yy > 22)] = (40, 40, 50)
        a[(np.abs(yy - 11) < 1.2) & ((np.abs(xx - 12) < 2.2) | (np.abs(xx - 20) < 2.2))] = (8, 6, 6)
        a = stains(a, 3, (90, 10, 10), seed, 1, 2, 0.8, True)
    elif kind == 2:  # family photograph, a face burned away
        a[:] = ramp(noise(S, S, 3, seed, 3), (90, 84, 70), (170, 160, 140))
        for cx, h in ((8, 14), (16, 18), (24, 12)):
            a[(xx - cx) ** 2 + (yy - (30 - h)) ** 2 < 8] = (190, 170, 150)
            a[(np.abs(xx - cx) < 3) & (yy > 32 - h) & (yy < 31)] = (60, 50, 44)
        a[(xx - 16) ** 2 + (yy - 12) ** 2 < 12] = (14, 10, 8)
    else:  # a cow skull
        a[:] = ramp(noise(S, S, 3, seed, 3), (30, 26, 20), (60, 52, 40))
        a[((xx - 16) ** 2 / 40 + (yy - 14) ** 2 / 70) < 1] = (200, 190, 168)
        a[((xx - 13) ** 2 + (yy - 12) ** 2) < 4] = (20, 14, 10)
        a[((xx - 19) ** 2 + (yy - 12) ** 2) < 4] = (20, 14, 10)
    a[:2, :] = a[-2:, :] = a[:, :2] = a[:, -2:] = (70, 50, 24)
    return a


def clock_face(S):
    a = np.zeros((S, S, 3)) + (40, 28, 18)
    yy, xx = np.mgrid[0:S, 0:S]
    m = (xx - 15.5) ** 2 + (yy - 15.5) ** 2 < 13 ** 2
    a[m] = (200, 188, 160)
    for k in range(12):
        ang = k / 12 * math.tau
        x, y = int(15.5 + math.cos(ang) * 11), int(15.5 + math.sin(ang) * 11)
        a[y, x] = (30, 24, 20)
    for i in range(9):
        a[int(15.5 - i), 15] = (20, 16, 14)
    for i in range(6):
        a[15, 15 + i] = (20, 16, 14)
    return a


def stove_door(S, lit):
    a = np.zeros((S, S, 3)) + (24, 22, 22)
    a[4:28, 4:28] = (34, 32, 32)
    for x in range(8, 26, 4):
        a[18:26, x:x + 2] = (240, 120, 30) if lit else (14, 12, 12)
    a[8:14, 8:24] = (56, 54, 52)
    return a


def flame(S, frames):
    im = Image.new("RGBA", (S, S * frames), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for f in range(frames):
        y0 = f * S
        w = 3 + (f % 2)
        h = 9 + (f * 3 % 4)
        d.ellipse([S / 2 - w, y0 + S - 2 - h, S / 2 + w, y0 + S - 2], fill=(255, 150, 40, 255))
        d.ellipse([S / 2 - w / 2, y0 + S - 2 - h * 0.7, S / 2 + w / 2, y0 + S - 3], fill=(255, 236, 160, 255))
    return im


def radio(S):
    a = np.zeros((S, S, 3)) + (60, 40, 26)
    a[4:16, 3:29] = (150, 130, 80)
    for x in range(5, 27, 3):
        a[8:12, x] = (40, 30, 20)
    yy, xx = np.mgrid[0:S, 0:S]
    a[((xx - 9) ** 2 + (yy - 23) ** 2) < 10] = (30, 24, 20)
    a[((xx - 23) ** 2 + (yy - 23) ** 2) < 10] = (30, 24, 20)
    return a


def tape(S):
    a = np.zeros((S, S, 3)) + (30, 30, 34)
    a[8:24, 4:28] = (190, 180, 150)
    yy, xx = np.mgrid[0:S, 0:S]
    a[((xx - 11) ** 2 + (yy - 16) ** 2) < 9] = (40, 30, 30)
    a[((xx - 21) ** 2 + (yy - 16) ** 2) < 9] = (40, 30, 30)
    return a


def handprint(S, seed):
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    col = (96, 4, 6, 230)
    d.ellipse([10, 14, 22, 26], fill=col)
    for k, (x, ln) in enumerate(((10, 8), (13, 11), (16, 12), (19, 10))):
        d.line([(x + 1, 15), (x + 1 + (k - 1.5) * 1.2, 15 - ln)], fill=col, width=2)
    d.line([(21, 22), (27, 17)], fill=col, width=2)
    d.line([(14, 26), (13, 31)], fill=col, width=1)
    d.line([(19, 26), (20, 31)], fill=col, width=1)
    return im


def cobweb(S, seed):
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = (210, 210, 200, 140)
    for k in range(7):
        ang = k / 7 * math.pi / 2
        d.line([(0, 0), (math.cos(ang) * S, math.sin(ang) * S)], fill=c)
    for r_ in (6, 12, 19, 27):
        pts = [(math.cos(k / 6 * math.pi / 2) * r_, math.sin(k / 6 * math.pi / 2) * r_) for k in range(7)]
        d.line(pts, fill=c)
    return im


def bloodpool(S, seed):
    a = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(a)
    r = random.Random(seed)
    for _ in range(9):
        x, y, rr = r.uniform(8, 24), r.uniform(8, 24), r.uniform(3, 9)
        d.ellipse([x - rr, y - rr * 0.8, x + rr, y + rr * 0.8], fill=(70, 2, 4, 235))
    for _ in range(14):
        x, y, rr = r.uniform(1, 31), r.uniform(1, 31), r.uniform(0.6, 1.6)
        d.ellipse([x - rr, y - rr, x + rr, y + rr], fill=(86, 4, 6, 220))
    d.ellipse([12, 11, 16, 13], fill=(150, 40, 40, 200))  # glint
    return a


def curtain(S, seed):
    a = ramp(noise(S, S, 3, seed, 3), (120, 110, 90), (176, 166, 140))
    x = np.arange(S)
    a = a * (0.75 + 0.3 * (np.sin(x * 0.8) * 0.5 + 0.5))[None, :, None]
    al = np.full((S, S), 220.0)
    r = random.Random(seed)
    for _ in range(14):  # holes
        cx, cy, rr = r.randrange(S), r.randrange(S), r.uniform(0.8, 2.5)
        yy, xx = np.mgrid[0:S, 0:S]
        al[(xx - cx) ** 2 + (yy - cy) ** 2 < rr * rr] = 0
    return img(stains(a, 5, (90, 70, 40), seed + 1, 1, 4, 0.5, True), al)


def window(S, lit):
    al = np.full((S, S), 255.0)
    a = np.zeros((S, S, 3)) + (52, 40, 30)
    glass = np.ones((S, S), bool)
    glass[:3, :] = glass[-3:, :] = glass[:, :3] = glass[:, -3:] = False
    glass[15:17, :] = glass[:, 15:17] = False
    g = ramp(noise(S, S, 4, 300 + lit, 3), (230, 160, 70), (255, 220, 140)) if lit else ramp(noise(S, S, 4, 301, 3), (24, 30, 34), (60, 70, 74))
    a[glass] = g[glass]
    al[glass] = 255 if lit else 150
    return img(a, al)


def wire(S):
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for y in (6, 16, 26):
        d.line([(0, y), (S, y + 1)], fill=(90, 84, 78, 255))
        for x in range(2, S, 7):
            d.line([(x - 1, y - 1), (x + 1, y + 2)], fill=(70, 64, 60, 255))
            d.line([(x + 1, y - 1), (x - 1, y + 2)], fill=(70, 64, 60, 255))
    return im


def drawer(S, seed):
    a = boards(S, S, (70, 50, 36), seed, 1, vertical=False)
    a[0, :] = a[-1, :] = a[:, 0] = a[:, -1] = (28, 18, 12)
    a[14:18, 13:19] = (150, 120, 60)
    return grain(a, seed)


def crate(S, seed):
    a = boards(S, S, (128, 100, 66), seed, 3, vertical=False)
    a[:3, :] = a[-3:, :] = a[:, :3] = a[:, -3:] = np.array((96, 72, 46))
    for i in range(S):
        j = int(i * (S - 1) / (S - 1))
        a[max(0, j - 1):j + 2, i] = (100, 76, 48)
    return grain(stains(a, 4, (60, 40, 20), seed, 1, 3, 0.4), seed)


def barrel(S, seed):
    a = boards(S, S, (90, 64, 40), seed, 5)
    for y in (4, 27):
        a[y:y + 3, :] = (50, 46, 44)
    return grain(a, seed)


def door(S, seed, top):
    a = boards(S, S, (66, 46, 32), seed, 3)
    a[:2, :] = a[-2:, :] = a[:, :2] = a[:, -2:] = (34, 22, 14)
    a[5:27, 5:27] *= 1.1
    a[5, 5:27] = a[26, 5:27] = (36, 24, 16)
    a[5:27, 5] = a[5:27, 26] = (36, 24, 16)
    if not top:
        a[2:6, 24:28] = (160, 130, 60)  # knob
        a = blood(a, 2, seed + 1, True, 0.8, 2)
    return grain(a, seed)


def trapdoor(S, seed):
    a = boards(S, S, (80, 60, 42), seed, 4)
    a[3:6, :] = a[26:29, :] = (40, 38, 36)
    a[14:18, 20:28] = (60, 58, 56)
    return grain(a, seed)


def ladder(S, seed):
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle([3, 0, 7, S], fill=(110, 86, 58, 255))
    d.rectangle([24, 0, 28, S], fill=(110, 86, 58, 255))
    for y in (4, 12, 20, 28):
        d.rectangle([7, y, 24, y + 2], fill=(124, 98, 66, 255))
    return im


def carcass(S, seed):
    a = ramp(noise(S, S, 3, seed, 4), (110, 16, 18), (190, 70, 64))
    yy, xx = np.mgrid[0:S, 0:S]
    rib = (np.sin(yy * 1.1) > 0.6) & (np.abs(xx - 16) < 11)
    a[rib] = (230, 214, 190)
    fat = noise(S, S, 4, seed + 1, 3) > 0.66
    a[fat] = (222, 200, 170)
    return stains(a, 8, (60, 0, 4), seed + 2, 1, 3, 0.8, True)


def burlap_face(S, seed):
    a = ramp(noise(S, S, 2, seed, 3), (120, 100, 66), (160, 136, 92))
    yy, xx = np.mgrid[0:S, 0:S]
    a[((xx - 10) ** 2 + (yy - 12) ** 2) < 9] = (10, 8, 6)
    a[((xx - 22) ** 2 + (yy - 12) ** 2) < 9] = (10, 8, 6)
    a[(yy > 20) & (yy < 23) & (np.abs(xx - 16) < 9)] = (20, 14, 10)
    for x in range(8, 25, 3):
        a[19:25, x] = (40, 30, 20)
    return blood(a, 3, seed, True, 0.8, 2)
