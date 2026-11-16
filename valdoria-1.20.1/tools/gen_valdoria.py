#!/usr/bin/env python3
"""Generates every resource of Valdoria: textures, blockstates, models, loot tables, recipes, tags, worldgen and lang.

Run from valdoria-1.20.1/:  python3 tools/gen_valdoria.py
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "valdoria"
A = os.path.join(ROOT, "assets", NS)
D = os.path.join(ROOT, "data", NS)
MC = os.path.join(ROOT, "data", "minecraft")
FORGE = os.path.join(ROOT, "data", "forge")


def W(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


# ----------------------------------------------------------------------------------------------------------------
# Textures
# ----------------------------------------------------------------------------------------------------------------

def cl(v):
    return max(0, min(255, int(round(v))))


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(cl(a[i] + (b[i] - a[i]) * t) for i in range(3))


def new(size=16):
    return Image.new("RGBA", (size, size), (0, 0, 0, 0))


def save(img, name):
    p = os.path.join(A, "textures", name + ".png")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def palette_pick(r, pal):
    return pal[min(len(pal) - 1, int(r.random() * len(pal)))]


def noise_fill(img, pal, seed, weights=None):
    r = random.Random(seed)
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            c = palette_pick(r, pal) if weights is None else r.choices(pal, weights)[0]
            px[x, y] = c + (255,)
    return img


BARK = [hexrgb(c) for c in ("#1b1420", "#211827", "#2a1e31", "#31233a", "#3b2a46")]
BARK_HI = hexrgb("#4a3758")
STRIP = [hexrgb(c) for c in ("#4b2d63", "#56346f", "#603b7b", "#6b4487")]
PLANK = [hexrgb(c) for c in ("#3c2550", "#452b5b", "#4e3266", "#573870")]
LEAF = [hexrgb(c) for c in ("#5a2e86", "#6b379b", "#7c43ae", "#8f52c0", "#a364cf", "#b67ad9")]
CYAN = [hexrgb(c) for c in ("#5ff3ff", "#8ef8ff", "#c8fdff")]
DUSK = [hexrgb(c) for c in ("#1f2c30", "#24343a", "#2a3d42", "#2f474b", "#355155")]
DIRT = [hexrgb(c) for c in ("#21181f", "#281d26", "#30232d", "#382a35")]
VSTONE = [hexrgb(c) for c in ("#4a3d5c", "#54456a", "#5e4e76", "#685782", "#73628d")]
MOSS = [hexrgb(c) for c in ("#2b6f69", "#348179", "#3e938a", "#49a89c")]
FERN = [hexrgb(c) for c in ("#1f6f66", "#278479", "#319a8c", "#3db3a2", "#56c9b6")]


def bark_side(seed):
    r = random.Random(seed)
    img = new()
    px = img.load()
    cols = [r.choice(range(len(BARK))) for _ in range(16)]
    for x in range(16):
        base = cols[x]
        for y in range(16):
            k = max(0, min(len(BARK) - 1, base + r.choice((-1, 0, 0, 0, 1))))
            px[x, y] = BARK[k] + (255,)
    # deep vertical cracks
    for _ in range(4):
        x = r.randrange(16)
        y0 = r.randrange(16)
        for y in range(y0, y0 + r.randint(4, 10)):
            px[x, y % 16] = hexrgb("#120c16") + (255,)
            if r.random() < 0.3:
                px[(x + 1) % 16, y % 16] = BARK_HI + (255,)
    # a few faint violet highlights
    for _ in range(6):
        px[r.randrange(16), r.randrange(16)] = BARK_HI + (255,)
    return img


def log_top(seed, outer, inner):
    r = random.Random(seed)
    img = new()
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 6.6:
                c = palette_pick(r, outer)
            else:
                ring = int(d * 1.3) % 2
                c = inner[min(len(inner) - 1, ring * 2 + r.randrange(2))]
            px[x, y] = c + (255,)
    return img


def planks(seed):
    r = random.Random(seed)
    img = new()
    px = img.load()
    for y in range(16):
        row = y // 4
        shift = r.randrange(16)
        for x in range(16):
            c = PLANK[min(len(PLANK) - 1, max(0, (row % 2) + r.choice((0, 1, 1, 2)) - (1 if (x + shift) % 9 == 0 else 0)))]
            if y % 4 == 3:
                c = mix(c, (10, 5, 15), 0.55)
            px[x, y] = c + (255,)
        seam = (row * 7 + 3) % 16
        for yy in range(row * 4, row * 4 + 3):
            px[seam, yy] = mix(px[seam, yy][:3], (10, 5, 15), 0.5) + (255,)
    return img


def leaves(seed, glimmer=False):
    r = random.Random(seed)
    img = new()
    px = img.load()
    for y in range(16):
        for x in range(16):
            if r.random() < 0.13:
                continue  # holes, like vanilla leaves
            v = r.random()
            # clumps: brighter top-left of little 3x3 lobes
            lobe = ((x + 2 * (y // 3)) % 4 == 0) or ((y + x // 3) % 5 == 0)
            k = int(v * len(LEAF))
            if lobe:
                k = max(0, k - 2)
            px[x, y] = LEAF[min(len(LEAF) - 1, k)] + (255,)
    if glimmer:
        for _ in range(5):
            x, y = r.randrange(1, 15), r.randrange(1, 15)
            px[x, y] = CYAN[2] + (255,)
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if r.random() < 0.6:
                    px[x + dx, y + dy] = CYAN[0] + (255,)
    return img


def vine(seed, end=False, glow=False):
    r = random.Random(seed)
    img = new()
    px = img.load()
    dark = [hexrgb(c) for c in ("#100b14", "#17101d", "#1e1626", "#271c31")]
    strands = [(5, 0), (8, 1), (10, -1)]
    for sx, wob in strands:
        for y in range(16):
            if end and y > 10 + (sx % 3):
                break
            x = sx + int(round(math.sin(y / 3.0 + sx) * 0.8)) + wob * (y // 8)
            px[x, y] = palette_pick(r, dark) + (255,)
            if r.random() < 0.35:
                px[min(15, x + 1), y] = palette_pick(r, dark[1:]) + (255,)
    # small drooping leaf tufts
    for _ in range(5):
        x, y = r.randrange(3, 13), r.randrange(0, 12 if end else 15)
        px[x, y] = hexrgb("#3d2a4d") + (255,)
        if x + 1 < 16:
            px[x + 1, y] = hexrgb("#2c1f38") + (255,)
    if glow:
        pts = [(6, 13), (9, 6), (11, 11), (5, 4)] if not end else [(7, 12), (9, 13), (8, 11), (5, 8)]
        for x, y in pts:
            px[x, y] = CYAN[2] + (255,)
            for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1)):
                xx, yy = x + dx, y + dy
                if 0 <= xx < 16 and 0 <= yy < 16:
                    px[xx, yy] = CYAN[0] + (255,)
    return img


def lichen(seed):
    r = random.Random(seed)
    img = new()
    px = img.load()
    # faint dark-teal crust with a few tiny bright points of light
    for _ in range(18):
        x, y = r.randrange(16), r.randrange(16)
        px[x, y] = hexrgb("#1f5b63") + (255,)
    for _ in range(7):
        x, y = r.randrange(1, 15), r.randrange(1, 15)
        px[x, y] = CYAN[2] + (255,)
        if r.random() < 0.5:
            px[x + 1, y] = CYAN[0] + (255,)
    return img


def duskgrass_top(seed):
    img = noise_fill(new(), DUSK, seed)
    r = random.Random(seed + 1)
    px = img.load()
    for _ in range(10):
        px[r.randrange(16), r.randrange(16)] = hexrgb("#4b3a63") + (255,)
    for _ in range(6):
        px[r.randrange(16), r.randrange(16)] = hexrgb("#3f6a6a") + (255,)
    return img


def dirt(seed):
    return noise_fill(new(), DIRT, seed)


def duskgrass_side(seed):
    img = dirt(seed)
    r = random.Random(seed + 7)
    px = img.load()
    for x in range(16):
        depth = r.choice((2, 3, 3, 4, 5))
        for y in range(depth):
            px[x, y] = palette_pick(r, DUSK) + (255,)
    return img


def stone(seed, pal=VSTONE):
    img = noise_fill(new(), pal, seed)
    r = random.Random(seed + 3)
    px = img.load()
    for _ in range(5):
        x, y = r.randrange(15), r.randrange(15)
        px[x, y] = mix(px[x, y][:3], (20, 12, 30), 0.4) + (255,)
    return img


def bricks(seed, mossy=False):
    r = random.Random(seed)
    img = noise_fill(new(), VSTONE, seed)
    px = img.load()
    mortar = hexrgb("#2c2238")
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 0 if row % 2 == 0 else 4
            if y % 4 == 3 or (x + off) % 8 == 7:
                px[x, y] = mortar + (255,)
            elif y % 4 == 0:
                px[x, y] = mix(px[x, y][:3], (255, 240, 255), 0.08) + (255,)
    if mossy:
        for _ in range(3):
            cx, cy = r.randrange(16), r.randrange(16)
            for _ in range(14):
                x = max(0, min(15, cx + r.randint(-3, 3)))
                y = max(0, min(15, cy + r.randint(-2, 2)))
                px[x, y] = palette_pick(r, MOSS) + (255,)
        for y in range(3):
            for x in range(16):
                if r.random() < 0.6 - y * 0.2:
                    px[x, y] = palette_pick(r, MOSS) + (255,)
    return img


def fern(seed, part="single"):
    r = random.Random(seed)
    img = new()
    px = img.load()
    fronds = 5 if part != "top" else 4
    for i in range(fronds):
        base_x = 3 + i * 2.5 + r.uniform(-0.5, 0.5)
        lean = r.uniform(-0.25, 0.25)
        top = 0 if part == "bottom" else r.randint(1, 6)
        start = 15
        for y in range(start, top - 1, -1):
            t = (start - y)
            x = int(round(base_x + lean * t + (math.sin(t / 2.5 + i) * 0.6 if part != "bottom" else 0)))
            if 0 <= x < 16:
                px[x, y] = FERN[min(len(FERN) - 1, int(t / 4) + r.randrange(2))] + (255,)
                if t % 3 == 0 and part != "bottom":
                    for dx in (-1, 1):
                        xx = x + dx
                        if 0 <= xx < 16:
                            px[xx, y] = palette_pick(r, FERN[2:]) + (255,)
    if part == "top":
        for _ in range(3):
            x, y = r.randrange(3, 13), r.randrange(1, 6)
            px[x, y] = CYAN[1] + (255,)
    return img


def shimmercap(seed):
    r = random.Random(seed)
    img = new()
    px = img.load()
    caps = [(4, 9, 3), (10, 7, 3), (7, 11, 2)]
    for cx, cy, rad in caps:
        for y in range(cy, 16):
            px[cx, y] = hexrgb("#9fd8d6") + (255,)
        for dx in range(-rad, rad + 1):
            for dy in range(-rad // 2 - 1, 1):
                if abs(dx) + abs(dy) <= rad:
                    x, y = cx + dx, cy + dy
                    if 0 <= x < 16 and 0 <= y < 16:
                        px[x, y] = (CYAN[2] if dy == -rad // 2 - 1 or abs(dx) < 1 else CYAN[r.randrange(2)]) + (255,)
    return img


def sapling(seed):
    r = random.Random(seed)
    img = new()
    px = img.load()
    for y in range(7, 16):
        px[7 + (1 if y < 10 else 0), y] = BARK[3] + (255,)
    for y in range(1, 9):
        for x in range(2, 14):
            if (x - 7.5) ** 2 / 30 + (y - 5) ** 2 / 12 <= 1 and r.random() > 0.15:
                px[x, y] = palette_pick(r, LEAF) + (255,)
    for _ in range(3):
        x, y = r.randrange(3, 12), r.randrange(8, 13)
        px[x, y] = hexrgb("#1d1524") + (255,)
    px[5, 4] = CYAN[2] + (255,)
    return img


def glimmer_particle():
    img = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    px = img.load()
    for y in range(8):
        for x in range(8):
            d = math.hypot(x - 3.5, y - 3.5)
            a = max(0.0, 1 - d / 3.9)
            if a > 0:
                px[x, y] = (255, 255, 255, cl(255 * a ** 1.4))
    return img


def build_textures():
    save(bark_side(11), "block/veilwood_log")
    save(log_top(12, BARK, STRIP), "block/veilwood_log_top")
    save(noise_fill(new(), STRIP, 13), "block/stripped_veilwood_log")
    save(log_top(14, STRIP, PLANK), "block/stripped_veilwood_log_top")
    save(planks(15), "block/veilwood_planks")
    save(leaves(16), "block/veilwood_leaves")
    save(leaves(17, glimmer=True), "block/glimmering_veilwood_leaves")
    save(sapling(18), "block/veilwood_sapling")
    save(vine(19), "block/veil_vines")
    save(vine(20, end=True), "block/veil_vines_end")
    save(vine(19, glow=True), "block/veil_vines_glow")
    save(vine(20, end=True, glow=True), "block/veil_vines_end_glow")
    save(lichen(21), "block/glimmer_lichen")
    save(duskgrass_top(22), "block/duskgrass_block_top")
    save(duskgrass_side(23), "block/duskgrass_block_side")
    save(dirt(24), "block/duskgrass_block_bottom")
    save(fern(25), "block/veil_fern")
    save(fern(26, "bottom"), "block/tall_veil_fern_bottom")
    save(fern(27, "top"), "block/tall_veil_fern_top")
    save(shimmercap(28), "block/shimmercap")
    save(stone(29), "block/violetstone")
    save(bricks(30), "block/violetstone_bricks")
    save(bricks(31, mossy=True), "block/mossy_violetstone_bricks")
    save(glimmer_particle(), "particle/glimmer")


# ----------------------------------------------------------------------------------------------------------------
# Models, blockstates, loot
# ----------------------------------------------------------------------------------------------------------------

def bs(name, obj):
    W(os.path.join(A, "blockstates", name + ".json"), obj)


def bm(name, obj):
    W(os.path.join(A, "models", "block", name + ".json"), obj)


def im(name, obj):
    W(os.path.join(A, "models", "item", name + ".json"), obj)


def loot(name, pools):
    W(os.path.join(D, "loot_tables", "blocks", name + ".json"),
      {"type": "minecraft:block", "pools": pools, "random_sequence": f"{NS}:blocks/{name}"})


def pool(entry, conditions=None, rolls=1):
    p = {"rolls": rolls, "bonus_rolls": 0.0, "entries": [entry]}
    if conditions:
        p["conditions"] = conditions
    return p


SURVIVES = [{"condition": "minecraft:survives_explosion"}]
SHEARS = {"condition": "minecraft:match_tool", "predicate": {"items": ["minecraft:shears"]}}
SILK = {"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}}


def self_drop(name):
    loot(name, [pool({"type": "minecraft:item", "name": f"{NS}:{name}"}, SURVIVES)])


def shears_drop(name):
    loot(name, [pool({"type": "minecraft:item", "name": f"{NS}:{name}"}, [SHEARS])])


def B(path):
    return f"{NS}:block/{path}"


def simple_block(name, tex=None):
    bm(name, {"parent": "minecraft:block/cube_all", "textures": {"all": B(tex or name)}})
    bs(name, {"variants": {"": {"model": B(name)}}})
    im(name, {"parent": B(name)})
    self_drop(name)


def log(name, side, end):
    bm(name, {"parent": "minecraft:block/cube_column", "textures": {"side": B(side), "end": B(end)}})
    bm(name + "_horizontal", {"parent": "minecraft:block/cube_column_horizontal", "textures": {"side": B(side), "end": B(end)}})
    bs(name, {"variants": {
        "axis=y": {"model": B(name)},
        "axis=z": {"model": B(name + "_horizontal"), "x": 90},
        "axis=x": {"model": B(name + "_horizontal"), "x": 90, "y": 90}}})
    im(name, {"parent": B(name)})
    self_drop(name)


def stairs(name, tex):
    t = {"bottom": B(tex), "top": B(tex), "side": B(tex)}
    bm(name, {"parent": "minecraft:block/stairs", "textures": t})
    bm(name + "_inner", {"parent": "minecraft:block/inner_stairs", "textures": t})
    bm(name + "_outer", {"parent": "minecraft:block/outer_stairs", "textures": t})
    variants = {}
    facings = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, fy in facings.items():
        for half in ("bottom", "top"):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model = B(name) if shape == "straight" else B(name + ("_inner" if shape.startswith("inner") else "_outer"))
                y = fy
                if half == "bottom" and shape.endswith("left"):
                    y = (fy + 270) % 360
                if half == "top" and shape.endswith("right"):
                    y = (fy + 90) % 360
                v = {"model": model}
                if half == "top":
                    v["x"] = 180
                if y:
                    v["y"] = y
                if half == "top" or y:
                    v["uvlock"] = True
                variants[f"facing={facing},half={half},shape={shape}"] = v
    bs(name, {"variants": variants})
    im(name, {"parent": B(name)})
    self_drop(name)


def slab(name, tex, full):
    t = {"bottom": B(tex), "top": B(tex), "side": B(tex)}
    bm(name, {"parent": "minecraft:block/slab", "textures": t})
    bm(name + "_top", {"parent": "minecraft:block/slab_top", "textures": t})
    bs(name, {"variants": {"type=bottom": {"model": B(name)}, "type=top": {"model": B(name + "_top")}, "type=double": {"model": B(full)}}})
    im(name, {"parent": B(name)})
    loot(name, [pool({"type": "minecraft:item", "name": f"{NS}:{name}", "functions": [
        {"function": "minecraft:set_count", "count": 2.0, "add": False,
         "conditions": [{"condition": "minecraft:block_state_property", "block": f"{NS}:{name}", "properties": {"type": "double"}}]},
        {"function": "minecraft:explosion_decay"}]})])


def wall(name, tex):
    bm(name + "_post", {"parent": "minecraft:block/template_wall_post", "textures": {"wall": B(tex)}})
    bm(name + "_side", {"parent": "minecraft:block/template_wall_side", "textures": {"wall": B(tex)}})
    bm(name + "_side_tall", {"parent": "minecraft:block/template_wall_side_tall", "textures": {"wall": B(tex)}})
    bm(name + "_inventory", {"parent": "minecraft:block/wall_inventory", "textures": {"wall": B(tex)}})
    parts = [{"when": {"up": "true"}, "apply": {"model": B(name + "_post")}}]
    for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for h, suffix in (("low", "_side"), ("tall", "_side_tall")):
            a = {"model": B(name + suffix), "uvlock": True}
            if y:
                a["y"] = y
            parts.append({"when": {d: h}, "apply": a})
    bs(name, {"multipart": parts})
    im(name, {"parent": B(name + "_inventory")})
    self_drop(name)


def cross(name, tex=None, item_tex=None, render="minecraft:cutout"):
    bm(name, {"parent": "minecraft:block/cross", "textures": {"cross": B(tex or name)}, "render_type": render})
    im(name, {"parent": "minecraft:item/generated", "textures": {"layer0": B(item_tex or tex or name)}})


def build_blocks():
    # veilwood
    log("veilwood_log", "veilwood_log", "veilwood_log_top")
    log("veilwood_wood", "veilwood_log", "veilwood_log")
    log("stripped_veilwood_log", "stripped_veilwood_log", "stripped_veilwood_log_top")
    log("stripped_veilwood_wood", "stripped_veilwood_log", "stripped_veilwood_log")
    simple_block("veilwood_planks")
    stairs("veilwood_stairs", "veilwood_planks")
    slab("veilwood_slab", "veilwood_planks", "veilwood_planks")

    for leaf, glimmer in (("veilwood_leaves", False), ("glimmering_veilwood_leaves", True)):
        bm(leaf, {"parent": "minecraft:block/cube_all", "textures": {"all": B(leaf)}, "render_type": "minecraft:cutout_mipped"})
        bs(leaf, {"variants": {"": {"model": B(leaf)}}})
        im(leaf, {"parent": B(leaf)})
        sticks = {"type": "minecraft:item", "name": "minecraft:stick", "conditions": [
            {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [0.02, 0.022222223, 0.025, 0.033333335, 0.1]}],
            "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1.0, "max": 2.0}, "add": False},
                          {"function": "minecraft:explosion_decay"}]}
        no_shears = {"condition": "minecraft:inverted", "term": {"condition": "minecraft:any_of", "terms": [SHEARS, SILK]}}
        loot(leaf, [
            pool({"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "name": f"{NS}:{leaf}", "conditions": [{"condition": "minecraft:any_of", "terms": [SHEARS, SILK]}]},
                {"type": "minecraft:item", "name": f"{NS}:veilwood_sapling", "conditions": [
                    {"condition": "minecraft:survives_explosion"},
                    {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [0.03, 0.04, 0.05, 0.07, 0.1]}]}]}),
            {"rolls": 1, "bonus_rolls": 0.0, "entries": [sticks], "conditions": [no_shears]}])

    cross("veilwood_sapling")
    bs("veilwood_sapling", {"variants": {"": {"model": B("veilwood_sapling")}}})
    self_drop("veilwood_sapling")

    # vines
    for end in (False, True):
        for glow in (False, True):
            tex = "veil_vines" + ("_end" if end else "") + ("_glow" if glow else "")
            bm(tex, {"parent": "minecraft:block/cross", "textures": {"cross": B(tex)}, "render_type": "minecraft:cutout"})
    bs("veil_vines", {"variants": {
        f"end={str(e).lower()},glow={str(g).lower()}": {"model": B("veil_vines" + ("_end" if e else "") + ("_glow" if g else ""))}
        for e in (False, True) for g in (False, True)}})
    im("veil_vines", {"parent": "minecraft:item/generated", "textures": {"layer0": B("veil_vines_end_glow")}})
    shears_drop("veil_vines")

    # glimmer lichen: same layout as vanilla glow lichen
    bm("glimmer_lichen", {"ambientocclusion": False, "render_type": "minecraft:cutout",
                          "textures": {"particle": B("glimmer_lichen"), "lichen": B("glimmer_lichen")},
                          "elements": [{"from": [0, 0, 0.1], "to": [16, 16, 0.1], "faces": {
                              "north": {"uv": [16, 0, 0, 16], "texture": "#lichen"},
                              "south": {"uv": [0, 0, 16, 16], "texture": "#lichen"}}}]})
    none = {d: "false" for d in ("down", "east", "north", "south", "up", "west")}
    parts = []
    for face, rot in (("north", {}), ("east", {"y": 90}), ("south", {"y": 180}), ("west", {"y": 270}), ("up", {"x": 270}), ("down", {"x": 90})):
        a = {"model": B("glimmer_lichen")}
        if rot:
            a.update(rot)
            a["uvlock"] = True
        parts.append({"when": {face: "true"}, "apply": a})
        parts.append({"when": dict(none), "apply": dict(a)})
    bs("glimmer_lichen", {"multipart": parts})
    im("glimmer_lichen", {"parent": "minecraft:item/generated", "textures": {"layer0": B("glimmer_lichen")}})
    loot("glimmer_lichen", [pool({"type": "minecraft:item", "name": f"{NS}:glimmer_lichen", "functions": [
        {"function": "minecraft:set_count", "add": True, "count": 1.0, "conditions": [
            {"condition": "minecraft:block_state_property", "block": f"{NS}:glimmer_lichen", "properties": {d: "true"}}]} for d in
        ("down", "up", "north", "south", "west", "east")] + [{"function": "minecraft:set_count", "add": True, "count": -1.0},
                                                             {"function": "minecraft:explosion_decay"}]}, [SHEARS])])

    # forest floor
    bm("duskgrass_block", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": B("duskgrass_block_top"), "side": B("duskgrass_block_side"), "bottom": B("duskgrass_block_bottom")}})
    bs("duskgrass_block", {"variants": {"": [{"model": B("duskgrass_block")}, {"model": B("duskgrass_block"), "y": 90},
                                             {"model": B("duskgrass_block"), "y": 180}, {"model": B("duskgrass_block"), "y": 270}]}})
    im("duskgrass_block", {"parent": B("duskgrass_block")})
    loot("duskgrass_block", [pool({"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": f"{NS}:duskgrass_block", "conditions": [SILK]},
        {"type": "minecraft:item", "name": "minecraft:dirt", "conditions": SURVIVES}]})])

    cross("veil_fern")
    bs("veil_fern", {"variants": {"": {"model": B("veil_fern")}}})
    shears_drop("veil_fern")

    bm("tall_veil_fern_bottom", {"parent": "minecraft:block/cross", "textures": {"cross": B("tall_veil_fern_bottom")}, "render_type": "minecraft:cutout"})
    bm("tall_veil_fern_top", {"parent": "minecraft:block/cross", "textures": {"cross": B("tall_veil_fern_top")}, "render_type": "minecraft:cutout"})
    bs("tall_veil_fern", {"variants": {"half=lower": {"model": B("tall_veil_fern_bottom")}, "half=upper": {"model": B("tall_veil_fern_top")}}})
    im("tall_veil_fern", {"parent": "minecraft:item/generated", "textures": {"layer0": B("tall_veil_fern_top")}})
    loot("tall_veil_fern", [pool({"type": "minecraft:item", "name": f"{NS}:tall_veil_fern"}, [
        SHEARS, {"condition": "minecraft:block_state_property", "block": f"{NS}:tall_veil_fern", "properties": {"half": "lower"}}])])

    cross("shimmercap")
    bs("shimmercap", {"variants": {"": {"model": B("shimmercap")}}})
    self_drop("shimmercap")

    # violetstone
    simple_block("violetstone")
    simple_block("violetstone_bricks")
    simple_block("mossy_violetstone_bricks")
    stairs("violetstone_brick_stairs", "violetstone_bricks")
    slab("violetstone_brick_slab", "violetstone_bricks", "violetstone_bricks")
    wall("violetstone_brick_wall", "violetstone_bricks")

    W(os.path.join(A, "particles", "glimmer.json"), {"textures": [f"{NS}:glimmer"]})


# ----------------------------------------------------------------------------------------------------------------
# Tags and recipes
# ----------------------------------------------------------------------------------------------------------------

_TAGS = {}


def tag(base, kind, name, values):
    """Tags are merged across calls within one run (several builders add to e.g. minecraft:leaves)."""
    path = os.path.join(base, "tags", kind, name + ".json")
    merged = _TAGS.setdefault(path, [])
    for v in values:
        if v not in merged:
            merged.append(v)
    W(path, {"replace": False, "values": merged})


def V(*names):
    return [f"{NS}:{n}" for n in names]


def build_tags():
    logs = V("veilwood_log", "veilwood_wood", "stripped_veilwood_log", "stripped_veilwood_wood")
    for kind in ("blocks", "items"):
        tag(D, kind, "veilwood_logs", logs)
        tag(MC, kind, "logs_that_burn", [f"#{NS}:veilwood_logs"])
        tag(MC, kind, "planks", V("veilwood_planks"))
        tag(MC, kind, "wooden_stairs", V("veilwood_stairs"))
        tag(MC, kind, "wooden_slabs", V("veilwood_slab"))
        tag(MC, kind, "leaves", V("veilwood_leaves", "glimmering_veilwood_leaves"))
        tag(MC, kind, "saplings", V("veilwood_sapling"))
        tag(MC, kind, "stairs", V("violetstone_brick_stairs"))
        tag(MC, kind, "slabs", V("violetstone_brick_slab"))
        tag(MC, kind, "walls", V("violetstone_brick_wall"))
        tag(MC, kind, "dirt", V("duskgrass_block"))
    tag(MC, "blocks", "mineable/axe", logs + V("veilwood_planks", "veilwood_stairs", "veilwood_slab"))
    tag(MC, "blocks", "mineable/hoe", V("veilwood_leaves", "glimmering_veilwood_leaves"))
    tag(MC, "blocks", "mineable/shovel", V("duskgrass_block"))
    tag(MC, "blocks", "mineable/pickaxe", V("violetstone", "violetstone_bricks", "mossy_violetstone_bricks",
                                             "violetstone_brick_stairs", "violetstone_brick_slab", "violetstone_brick_wall"))
    tag(MC, "blocks", "climbable", V("veil_vines"))
    tag(MC, "blocks", "animals_spawnable_on", V("duskgrass_block"))
    tag(MC, "blocks", "replaceable_by_trees", V("veil_fern", "tall_veil_fern", "veil_vines", "glimmer_lichen"))
    tag(MC, "blocks", "sword_efficient", V("veil_vines", "veil_fern", "tall_veil_fern", "shimmercap"))
    tag(MC, "blocks", "enderman_holdable", V("shimmercap"))
    tag(MC, "items", "stone_crafting_materials", V("violetstone"))
    tag(MC, "items", "stone_tool_materials", V("violetstone"))


def recipe(name, obj):
    W(os.path.join(D, "recipes", name + ".json"), obj)


def item(n, count=1):
    r = {"item": n if ":" in n else f"{NS}:{n}"}
    if count != 1:
        r["count"] = count
    return r


def shaped(name, pattern, keys, result, count=1, group=None, cat="building"):
    o = {"type": "minecraft:crafting_shaped", "category": cat, "pattern": pattern,
         "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v if ":" in v else f"{NS}:{v}"}) for k, v in keys.items()},
         "result": item(result, count)}
    if group:
        o["group"] = group
    recipe(name, o)


def shapeless(name, ingredients, result, count=1, cat="building"):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": cat,
                  "ingredients": [({"tag": v[1:]} if v.startswith("#") else {"item": v if ":" in v else f"{NS}:{v}"}) for v in ingredients],
                  "result": item(result, count)})


def stonecut(name, ing, result, count=1):
    recipe(name, {"type": "minecraft:stonecutting", "ingredient": {"item": f"{NS}:{ing}"}, "result": f"{NS}:{result}", "count": count})


def build_recipes():
    shapeless("veilwood_planks", [f"#{NS}:veilwood_logs"], "veilwood_planks", 4)
    shaped("veilwood_wood", ["##", "##"], {"#": "veilwood_log"}, "veilwood_wood", 3)
    shaped("stripped_veilwood_wood", ["##", "##"], {"#": "stripped_veilwood_log"}, "stripped_veilwood_wood", 3)
    shaped("veilwood_stairs", ["#  ", "## ", "###"], {"#": "veilwood_planks"}, "veilwood_stairs", 4, "wooden_stairs")
    shaped("veilwood_slab", ["###"], {"#": "veilwood_planks"}, "veilwood_slab", 6, "wooden_slab")
    shaped("violetstone_bricks", ["##", "##"], {"#": "violetstone"}, "violetstone_bricks", 4)
    shapeless("mossy_violetstone_bricks_from_vines", ["violetstone_bricks", "veil_vines"], "mossy_violetstone_bricks")
    shapeless("mossy_violetstone_bricks_from_moss", ["violetstone_bricks", "minecraft:moss_block"], "mossy_violetstone_bricks")
    shaped("violetstone_brick_stairs", ["#  ", "## ", "###"], {"#": "violetstone_bricks"}, "violetstone_brick_stairs", 4)
    shaped("violetstone_brick_slab", ["###"], {"#": "violetstone_bricks"}, "violetstone_brick_slab", 6)
    shaped("violetstone_brick_wall", ["###", "###"], {"#": "violetstone_bricks"}, "violetstone_brick_wall", 6, cat="misc")
    stonecut("violetstone_bricks_stonecutting", "violetstone", "violetstone_bricks")
    stonecut("violetstone_brick_stairs_stonecutting", "violetstone_bricks", "violetstone_brick_stairs")
    stonecut("violetstone_brick_slab_stonecutting", "violetstone_bricks", "violetstone_brick_slab", 2)
    stonecut("violetstone_brick_wall_stonecutting", "violetstone_bricks", "violetstone_brick_wall")
    # light: glimmering leaves from leaves + shimmercap
    shapeless("glimmering_veilwood_leaves", ["veilwood_leaves", "shimmercap"], "glimmering_veilwood_leaves", cat="building")
    shapeless("cyan_dye_from_shimmercap", ["shimmercap"], "minecraft:cyan_dye", cat="misc")
    shapeless("purple_dye_from_veilwood_leaves", ["veilwood_leaves", "veilwood_leaves"], "minecraft:purple_dye", cat="misc")


# ----------------------------------------------------------------------------------------------------------------
# Worldgen
# ----------------------------------------------------------------------------------------------------------------

def wg(kind, name, obj):
    W(os.path.join(D, "worldgen", kind, name + ".json"), obj)


def patch(name, block, tries, spread, count):
    wg("configured_feature", name, {"type": "minecraft:random_patch", "config": {
        "tries": tries, "xz_spread": spread, "y_spread": 3,
        "feature": {"feature": {"type": "minecraft:simple_block", "config": {
            "to_place": {"type": "minecraft:simple_state_provider", "state": {"Name": f"{NS}:{block}"}}}},
            "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {
                "type": "minecraft:matching_blocks", "blocks": "minecraft:air"}}]}}})
    wg("placed_feature", name, {"feature": f"{NS}:{name}", "placement": [
        {"type": "minecraft:count", "count": count},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
        {"type": "minecraft:biome"}]})


def build_worldgen():
    wg("configured_feature", "giant_veilwood_tree", {"type": f"{NS}:giant_veilwood_tree", "config": {}})
    wg("placed_feature", "giant_veilwood_tree", {"feature": f"{NS}:giant_veilwood_tree", "placement": [
        {"type": "minecraft:count", "count": {"type": "minecraft:weighted_list", "distribution": [
            {"weight": 2, "data": 1}, {"weight": 3, "data": 2}]}},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
        {"type": "minecraft:biome"}]})
    wg("configured_feature", "violet_ruin", {"type": f"{NS}:violet_ruin", "config": {}})
    wg("placed_feature", "violet_ruin", {"feature": f"{NS}:violet_ruin", "placement": [
        {"type": "minecraft:rarity_filter", "chance": 3},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
        {"type": "minecraft:biome"}]})
    patch("veil_fern_patch", "veil_fern", 48, 7, 5)
    patch("tall_veil_fern_patch", "tall_veil_fern", 32, 6, 3)
    patch("shimmercap_patch", "shimmercap", 24, 5, 2)

    monsters = [
        {"type": "minecraft:spider", "weight": 100, "minCount": 4, "maxCount": 4},
        {"type": "minecraft:zombie", "weight": 95, "minCount": 4, "maxCount": 4},
        {"type": "minecraft:zombie_villager", "weight": 5, "minCount": 1, "maxCount": 1},
        {"type": "minecraft:skeleton", "weight": 100, "minCount": 4, "maxCount": 4},
        {"type": "minecraft:creeper", "weight": 100, "minCount": 4, "maxCount": 4},
        {"type": "minecraft:slime", "weight": 100, "minCount": 4, "maxCount": 4},
        {"type": "minecraft:enderman", "weight": 10, "minCount": 1, "maxCount": 4},
        {"type": "minecraft:witch", "weight": 5, "minCount": 1, "maxCount": 1},
        {"type": f"{NS}:vinestalker", "weight": 70, "minCount": 1, "maxCount": 2}]
    wg("biome", "glimmerveil_forest", {
        "has_precipitation": True,
        "temperature": 0.6,
        "downfall": 0.8,
        "effects": {
            "sky_color": 0x4FB3C0,
            "fog_color": 0x2E8C9A,
            "water_color": 0x2F8FA0,
            "water_fog_color": 0x0D2A33,
            "grass_color": 0x2F6E6A,
            "foliage_color": 0x7A4AA8,
            "particle": {"options": {"type": f"{NS}:glimmer"}, "probability": 0.006},
            "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
            "additions_sound": {"sound": "minecraft:ambient.warped_forest.additions", "tick_chance": 0.0111},
            "music": {"sound": "minecraft:music.overworld.forest", "min_delay": 12000, "max_delay": 24000, "replace_current_music": False}},
        "spawners": {
            "monster": monsters,
            "creature": [
                {"type": f"{NS}:veilstag", "weight": 14, "minCount": 3, "maxCount": 6},
                {"type": f"{NS}:gloomlynx", "weight": 4, "minCount": 2, "maxCount": 3},
                {"type": "minecraft:rabbit", "weight": 4, "minCount": 2, "maxCount": 3}],
            "ambient": [
                {"type": f"{NS}:lumen_moth", "weight": 40, "minCount": 5, "maxCount": 9},
                {"type": "minecraft:bat", "weight": 5, "minCount": 4, "maxCount": 6}],
            "axolotls": [], "underground_water_creature": [{"type": "minecraft:glow_squid", "weight": 10, "minCount": 4, "maxCount": 6}],
            "water_creature": [], "water_ambient": [], "misc": []},
        "spawn_costs": {},
        "carvers": {"air": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"]},
        "features": [
            [],
            ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"],
            ["minecraft:amethyst_geode"],
            ["minecraft:monster_room", "minecraft:monster_room_deep"],
            [f"{NS}:violet_ruin"],
            [],
            ["minecraft:ore_dirt", "minecraft:ore_gravel", "minecraft:ore_granite_upper", "minecraft:ore_granite_lower",
             "minecraft:ore_diorite_upper", "minecraft:ore_diorite_lower", "minecraft:ore_andesite_upper",
             "minecraft:ore_andesite_lower", "minecraft:ore_tuff", "minecraft:ore_coal_upper", "minecraft:ore_coal_lower",
             "minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_iron_small", "minecraft:ore_gold",
             "minecraft:ore_gold_lower", "minecraft:ore_redstone", "minecraft:ore_redstone_lower", "minecraft:ore_diamond",
             "minecraft:ore_diamond_large", "minecraft:ore_diamond_buried", "minecraft:ore_lapis", "minecraft:ore_lapis_buried",
             "minecraft:ore_copper", "minecraft:underwater_magma", "minecraft:disk_sand", "minecraft:disk_clay", "minecraft:disk_gravel"],
            [],
            ["minecraft:spring_water", "minecraft:spring_lava"],
            ["minecraft:glow_lichen", f"{NS}:giant_veilwood_tree", f"{NS}:tall_veil_fern_patch", f"{NS}:veil_fern_patch",
             f"{NS}:shimmercap_patch"],
            ["minecraft:freeze_top_layer"]]})


# ----------------------------------------------------------------------------------------------------------------
# Lang
# ----------------------------------------------------------------------------------------------------------------

NAMES = {
    "veilwood_log": ("Veilwood Log", "Peçeağacı Kütüğü"),
    "veilwood_wood": ("Veilwood", "Peçeağacı Odunu"),
    "stripped_veilwood_log": ("Stripped Veilwood Log", "Soyulmuş Peçeağacı Kütüğü"),
    "stripped_veilwood_wood": ("Stripped Veilwood", "Soyulmuş Peçeağacı Odunu"),
    "veilwood_planks": ("Veilwood Planks", "Peçeağacı Kalası"),
    "veilwood_stairs": ("Veilwood Stairs", "Peçeağacı Merdiveni"),
    "veilwood_slab": ("Veilwood Slab", "Peçeağacı Basamağı"),
    "veilwood_leaves": ("Veilwood Leaves", "Peçeağacı Yaprakları"),
    "glimmering_veilwood_leaves": ("Glimmering Veilwood Leaves", "Işıldayan Peçeağacı Yaprakları"),
    "veilwood_sapling": ("Veilwood Sapling", "Peçeağacı Fidanı"),
    "veil_vines": ("Veil Vines", "Peçe Sarmaşığı"),
    "glimmer_lichen": ("Glimmer Lichen", "Işıltı Likeni"),
    "duskgrass_block": ("Duskgrass Block", "Alacaçim Bloğu"),
    "veil_fern": ("Veil Fern", "Peçe Eğreltisi"),
    "tall_veil_fern": ("Tall Veil Fern", "Uzun Peçe Eğreltisi"),
    "shimmercap": ("Shimmercap", "Işıltı Mantarı"),
    "violetstone": ("Violetstone", "Menekşetaş"),
    "violetstone_bricks": ("Violetstone Bricks", "Menekşetaş Tuğlası"),
    "mossy_violetstone_bricks": ("Mossy Violetstone Bricks", "Yosunlu Menekşetaş Tuğlası"),
    "violetstone_brick_stairs": ("Violetstone Brick Stairs", "Menekşetaş Tuğla Merdiveni"),
    "violetstone_brick_slab": ("Violetstone Brick Slab", "Menekşetaş Tuğla Basamağı"),
    "violetstone_brick_wall": ("Violetstone Brick Wall", "Menekşetaş Tuğla Duvarı"),
}


CREATURES = {
    "veilstag": ("Veilstag", "Peçe Geyiği"),
    "lumen_moth": ("Lumen Moth", "Işıltı Güvesi"),
    "gloomlynx": ("Gloomlynx", "Alacakaranlık Vaşağı"),
    "vinestalker": ("Vinestalker", "Sarmaşık Pusucusu"),
}


def build_creatures():
    def eloot(name, pools):
        W(os.path.join(D, "loot_tables", "entities", name + ".json"),
          {"type": "minecraft:entity", "pools": pools, "random_sequence": f"{NS}:entities/{name}"})

    def drop(item_, lo, hi, looting=True, smelt=False, chance=None):
        fns = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
        if smelt:
            fns.append({"function": "minecraft:furnace_smelt", "conditions": [{"condition": "minecraft:entity_properties", "entity": "this",
                                                                             "predicate": {"flags": {"is_on_fire": True}}}]})
        if looting:
            fns.append({"function": "minecraft:looting_enchant", "count": {"type": "minecraft:uniform", "min": 0.0, "max": 1.0}})
        p = {"rolls": 1, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": item_, "functions": fns}]}
        if chance:
            p["conditions"] = [{"condition": "minecraft:random_chance_with_looting", "chance": chance, "looting_multiplier": 0.05}]
        return p

    eloot("veilstag", [drop("minecraft:leather", 0.0, 2.0), drop("minecraft:mutton", 1.0, 3.0, smelt=True),
                       drop(f"{NS}:shimmercap", 1.0, 1.0, looting=False, chance=0.15)])
    eloot("gloomlynx", [drop("minecraft:rabbit_hide", 1.0, 2.0), drop("minecraft:glow_ink_sac", 1.0, 1.0, chance=0.3)])
    eloot("vinestalker", [drop("minecraft:string", 1.0, 3.0), drop(f"{NS}:veil_vines", 1.0, 3.0),
                          drop("minecraft:spider_eye", 1.0, 1.0, chance=0.35)])
    eloot("lumen_moth", [drop("minecraft:glowstone_dust", 1.0, 1.0, looting=False, chance=0.35)])
    for n in CREATURES:
        im(n + "_spawn_egg", {"parent": "minecraft:item/template_spawn_egg"})


def build_lang():
    en = {"itemGroup.valdoria": "Valdoria", f"biome.{NS}.glimmerveil_forest": "Glimmerveil Forest"}
    tr = {"itemGroup.valdoria": "Valdoria: Yedi Diyar", f"biome.{NS}.glimmerveil_forest": "Işıltılı Peçe Ormanı"}
    for k, (e, t) in CREATURES.items():
        en[f"entity.{NS}.{k}"] = e
        tr[f"entity.{NS}.{k}"] = t
        en[f"item.{NS}.{k}_spawn_egg"] = e + " Spawn Egg"
        tr[f"item.{NS}.{k}_spawn_egg"] = t + " Çağırma Yumurtası"
    for k, (e, t) in NAMES.items():
        en[f"block.{NS}.{k}"] = e
        tr[f"block.{NS}.{k}"] = t
    for k, (e, t) in EXTRA_LANG.items():
        en[k] = e
        tr[k] = t
    W(os.path.join(A, "lang", "en_us.json"), en)
    W(os.path.join(A, "lang", "tr_tr.json"), tr)


# ================================================================================================================
# Mistfall Vale
# ================================================================================================================

MIST_ROCK = [hexrgb(c) for c in ("#6a5a96", "#7466a3", "#7f70ad", "#5f5089", "#8a7bb8")]
MIST_BAND = [hexrgb(c) for c in ("#8d7fb4", "#9c8fc0", "#a99dc9")]
MIST_DEEP = [hexrgb(c) for c in ("#3c3466", "#453c74", "#4f4580", "#36305c")]
SKY_BARK = [hexrgb(c) for c in ("#5a4a3e", "#6e5d4e", "#7d6a58", "#4c3f35", "#88745f")]
SKY_PLANK = [hexrgb(c) for c in ("#a08463", "#ad906d", "#937859", "#b89b77")]
SKY_LEAF = [hexrgb(c) for c in ("#3f97ad", "#4ea9bd", "#5ab8c9", "#6cc4d2", "#7ccfd9", "#a3e0e6")]
GOLD_LEAF = [hexrgb(c) for c in ("#9fae4a", "#b9a43f", "#c9b64f", "#d9c25a", "#e8d77a", "#f0e39a")]
PALM_LOG = [hexrgb(c) for c in ("#8a6743", "#a07b52", "#b08a5f", "#7a5a3a")]
PALM_LEAF = [hexrgb(c) for c in ("#3d8a3e", "#4fa04a", "#62b452", "#78c65e")]
BLOOM = [hexrgb(c) for c in ("#3a5fc0", "#4f7fe0", "#6b96ec", "#8fb2f4", "#b9cff8")]
CASCADE = [hexrgb(c) for c in ("#246a78", "#2f7f8f", "#3a8e9e", "#2a7584")]


def strata_rock(seed, pal, band_pal=None):
    r = random.Random(seed)
    img = new()
    px = img.load()
    for y in range(16):
        row_shift = r.choice((-1, 0, 0, 1))
        for x in range(16):
            k = (y + row_shift) % 5
            c = palette_pick(r, pal)
            if band_pal and k in (1, 2):
                c = palette_pick(r, band_pal)
            if k == 4 and r.random() < 0.6:
                c = mix(c, (20, 12, 40), 0.35)
            px[x, y] = c + (255,)
    return img


def bark(seed, pal):
    r = random.Random(seed)
    img = new()
    px = img.load()
    cols = [r.randrange(len(pal)) for _ in range(16)]
    for x in range(16):
        for y in range(16):
            k = max(0, min(len(pal) - 1, cols[x] + r.choice((-1, 0, 0, 1))))
            c = pal[k]
            if (x + (y // 6)) % 4 == 0:
                c = mix(c, (25, 18, 12), 0.45)  # deep furrows
            px[x, y] = c + (255,)
    return img


def ring_log(seed, pal, ring=4):
    r = random.Random(seed)
    img = new()
    px = img.load()
    for y in range(16):
        for x in range(16):
            c = palette_pick(r, pal)
            if y % ring == 0:
                c = mix(c, (40, 25, 10), 0.4)
            px[x, y] = c + (255,)
    return img


def leaf_tex(seed, pal, holes=0.1):
    r = random.Random(seed)
    img = new()
    px = img.load()
    for y in range(16):
        for x in range(16):
            if r.random() < holes:
                continue
            k = int(r.random() * len(pal))
            if (x + y * 2) % 5 == 0:
                k = min(len(pal) - 1, k + 2)
            px[x, y] = pal[k] + (255,)
    return img


def bloom_tex(seed, part="single"):
    r = random.Random(seed)
    img = new()
    px = img.load()
    green = [hexrgb("#2f7a3a"), hexrgb("#3f8f45"), hexrgb("#55a650")]
    if part in ("single", "bottom"):
        for x in (5, 8, 11):
            for y in range(8 if part == "single" else 0, 16):
                px[x + (y % 3 == 0), y] = palette_pick(r, green) + (255,)
        for _ in range(10):
            x, y = r.randrange(2, 14), r.randrange(9 if part == "single" else 2, 15)
            px[x, y] = palette_pick(r, green) + (255,)
    if part in ("single", "top"):
        clusters = [(5, 5), (10, 4), (8, 8)] if part == "single" else [(4, 6), (11, 5), (8, 10), (7, 3)]
        for cx, cy in clusters:
            for dx in range(-3, 4):
                for dy in range(-3, 4):
                    if dx * dx + dy * dy <= 7 and r.random() < 0.85:
                        x, y = cx + dx, cy + dy
                        if 0 <= x < 16 and 0 <= y < 16:
                            px[x, y] = palette_pick(r, BLOOM) + (255,)
        if part == "top":
            for x in (6, 9):
                for y in range(12, 16):
                    px[x, y] = palette_pick(r, green) + (255,)
    return img


def portal_frames(n=16):
    w = 16
    img = Image.new("RGBA", (w, w * n), (0, 0, 0, 0))
    px = img.load()
    r = random.Random(77)
    streaks = [(x, r.random() * 16, 0.6 + r.random() * 0.8) for x in range(16)]
    for f in range(n):
        for x, off, spd in streaks:
            for y in range(16):
                v = (y - f * spd * 16 / n * 2 + off) % 16 / 16.0
                bright = 0.5 + 0.5 * math.sin(v * math.pi * 2 + x)
                c = mix(hexrgb("#2f8fd0"), hexrgb("#bff6ff"), bright * 0.8)
                a = int(150 + 80 * bright)
                px[x, f * w + y] = c + (a,)
    return img


def cascade_tex(seed):
    img = noise_fill(new(), CASCADE, seed)
    r = random.Random(seed + 5)
    px = img.load()
    x = r.randrange(16)
    for y in range(16):
        x = max(0, min(15, x + r.choice((-1, 0, 0, 1))))
        px[x, y] = hexrgb("#9ff6ff") + (255,)
        if r.random() < 0.5:
            px[min(15, x + 1), y] = hexrgb("#5fd8e8") + (255,)
    for _ in range(4):
        px[r.randrange(16), r.randrange(16)] = hexrgb("#d6feff") + (255,)
    return img


def key_tex():
    img = new()
    px = img.load()
    gold = hexrgb("#d9b25a")
    dark = hexrgb("#8a6a2a")
    for i in range(9):
        px[3 + i, 12 - i] = gold + (255,)
        px[4 + i, 12 - i] = dark + (255,)
    for (x, y) in ((4, 13), (5, 14), (3, 11), (6, 12), (6, 13)):
        px[x, y] = gold + (255,)
    for dx in range(-2, 3):
        for dy in range(-2, 3):
            if abs(dx) + abs(dy) <= 2:
                px[12 + dx, 3 + dy] = (hexrgb("#d6feff") if dx == dy == 0 else hexrgb("#5fd8e8")) + (255,)
    return img


def mist_tex():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5) / 7.8
            a = max(0.0, 1 - d) ** 1.8
            px[x, y] = (255, 255, 255, cl(255 * a))
    return img


def build_mistfall():
    save(strata_rock(201, MIST_ROCK), "block/mistrock")
    save(strata_rock(202, MIST_ROCK, MIST_BAND), "block/banded_mistrock")
    save(strata_rock(203, MIST_DEEP), "block/deep_mistrock")
    img = noise_fill(new(), MIST_ROCK, 204)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if y % 4 == 3 or (x + (0 if (y // 4) % 2 == 0 else 4)) % 8 == 7:
                px[x, y] = hexrgb("#3c3466") + (255,)
    save(img, "block/mistrock_bricks")
    save(bark(205, SKY_BARK), "block/skyroot_log")
    save(log_top(206, SKY_BARK, SKY_PLANK), "block/skyroot_log_top")
    save(noise_fill(new(), SKY_PLANK, 207), "block/skyroot_planks")
    save(leaf_tex(208, SKY_LEAF), "block/skyroot_leaves")
    save(leaf_tex(209, GOLD_LEAF), "block/golden_skyroot_leaves")
    sap = new()
    sp = sap.load()
    for y in range(8, 16):
        sp[8, y] = SKY_BARK[2] + (255,)
    for x in range(2, 14):
        for y in range(3, 8):
            if (x - 7.5) ** 2 / 36 + (y - 5) ** 2 / 4 <= 1:
                sp[x, y] = SKY_LEAF[(x + y) % len(SKY_LEAF)] + (255,)
    save(sap, "block/skyroot_sapling")
    save(ring_log(210, PALM_LOG), "block/sunpalm_log")
    save(log_top(211, PALM_LOG, PALM_LOG), "block/sunpalm_log_top")
    save(leaf_tex(212, PALM_LEAF, 0.25), "block/sunpalm_fronds")
    save(bloom_tex(213), "block/azure_bloom")
    save(bloom_tex(214, "bottom"), "block/tall_azure_bloom_bottom")
    save(bloom_tex(215, "top"), "block/tall_azure_bloom_top")
    save(cascade_tex(216), "block/cascade_stone")
    save(portal_frames(), "block/mistfall_portal")
    with open(os.path.join(A, "textures", "block", "mistfall_portal.png.mcmeta"), "w") as fh:
        json.dump({"animation": {"frametime": 2, "interpolate": True}}, fh)
    save(key_tex(), "item/cascade_key")
    save(mist_tex(), "particle/mist")
    W(os.path.join(A, "particles", "mist.json"), {"textures": [f"{NS}:mist"]})

    for n in ("mistrock", "banded_mistrock", "deep_mistrock", "mistrock_bricks", "skyroot_planks", "cascade_stone"):
        simple_block(n)
    log("skyroot_log", "skyroot_log", "skyroot_log_top")
    log("skyroot_wood", "skyroot_log", "skyroot_log")
    log("sunpalm_log", "sunpalm_log", "sunpalm_log_top")
    for leaf, sap_drop in (("skyroot_leaves", True), ("golden_skyroot_leaves", True), ("sunpalm_fronds", False)):
        bm(leaf, {"parent": "minecraft:block/cube_all", "textures": {"all": B(leaf)}, "render_type": "minecraft:cutout_mipped"})
        bs(leaf, {"variants": {"": {"model": B(leaf)}}})
        im(leaf, {"parent": B(leaf)})
        children = [{"type": "minecraft:item", "name": f"{NS}:{leaf}", "conditions": [{"condition": "minecraft:any_of", "terms": [SHEARS, SILK]}]}]
        if sap_drop:
            children.append({"type": "minecraft:item", "name": f"{NS}:skyroot_sapling", "conditions": [
                {"condition": "minecraft:survives_explosion"},
                {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [0.04, 0.05, 0.07, 0.09, 0.12]}]})
        else:
            children.append({"type": "minecraft:item", "name": "minecraft:stick", "conditions": [{"condition": "minecraft:random_chance", "chance": 0.1}]})
        loot(leaf, [pool({"type": "minecraft:alternatives", "children": children})])
    cross("skyroot_sapling")
    bs("skyroot_sapling", {"variants": {"": {"model": B("skyroot_sapling")}}})
    self_drop("skyroot_sapling")
    cross("azure_bloom")
    bs("azure_bloom", {"variants": {"": {"model": B("azure_bloom")}}})
    self_drop("azure_bloom")
    bm("tall_azure_bloom_bottom", {"parent": "minecraft:block/cross", "textures": {"cross": B("tall_azure_bloom_bottom")}, "render_type": "minecraft:cutout"})
    bm("tall_azure_bloom_top", {"parent": "minecraft:block/cross", "textures": {"cross": B("tall_azure_bloom_top")}, "render_type": "minecraft:cutout"})
    bs("tall_azure_bloom", {"variants": {"half=lower": {"model": B("tall_azure_bloom_bottom")}, "half=upper": {"model": B("tall_azure_bloom_top")}}})
    im("tall_azure_bloom", {"parent": "minecraft:item/generated", "textures": {"layer0": B("tall_azure_bloom_top")}})
    loot("tall_azure_bloom", [pool({"type": "minecraft:item", "name": f"{NS}:tall_azure_bloom"}, [
        {"condition": "minecraft:block_state_property", "block": f"{NS}:tall_azure_bloom", "properties": {"half": "lower"}}])])
    # portal curtain: vanilla nether portal shapes, our animated texture
    bm("mistfall_portal_ns", {"parent": "minecraft:block/nether_portal_ns", "render_type": "minecraft:translucent",
                              "textures": {"particle": B("mistfall_portal"), "portal": B("mistfall_portal")}})
    bm("mistfall_portal_ew", {"parent": "minecraft:block/nether_portal_ew", "render_type": "minecraft:translucent",
                              "textures": {"particle": B("mistfall_portal"), "portal": B("mistfall_portal")}})
    bs("mistfall_portal", {"variants": {"axis=x": {"model": B("mistfall_portal_ns")}, "axis=z": {"model": B("mistfall_portal_ew")}}})
    im("cascade_key", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{NS}:item/cascade_key"}})

    rocks = V("mistrock", "banded_mistrock", "deep_mistrock")
    for kind in ("blocks", "items"):
        tag(MC, kind, "logs_that_burn", V("skyroot_log", "skyroot_wood", "sunpalm_log"))
        tag(MC, kind, "planks", V("skyroot_planks"))
        tag(MC, kind, "leaves", V("skyroot_leaves", "golden_skyroot_leaves", "sunpalm_fronds"))
        tag(MC, kind, "saplings", V("skyroot_sapling"))
        tag(MC, kind, "small_flowers", V("azure_bloom"))
        tag(MC, kind, "tall_flowers", V("tall_azure_bloom"))
    tag(MC, "blocks", "mineable/axe", V("skyroot_log", "skyroot_wood", "sunpalm_log", "skyroot_planks"))
    tag(MC, "blocks", "mineable/hoe", V("skyroot_leaves", "golden_skyroot_leaves", "sunpalm_fronds"))
    tag(MC, "blocks", "mineable/pickaxe", rocks + V("mistrock_bricks", "cascade_stone"))
    tag(MC, "blocks", "needs_iron_tool", V("cascade_stone"))
    tag(MC, "blocks", "stone_ore_replaceables", rocks)
    tag(MC, "blocks", "base_stone_overworld", rocks)
    tag(MC, "items", "stone_crafting_materials", V("mistrock"))
    tag(MC, "items", "stone_tool_materials", V("mistrock"))

    shapeless("skyroot_planks", [f"{NS}:skyroot_log"], "skyroot_planks", 4)
    shapeless("skyroot_planks_from_wood", [f"{NS}:skyroot_wood"], "skyroot_planks", 4)
    shapeless("skyroot_planks_from_sunpalm", [f"{NS}:sunpalm_log"], "skyroot_planks", 4)
    shaped("skyroot_wood", ["##", "##"], {"#": "skyroot_log"}, "skyroot_wood", 3)
    shaped("mistrock_bricks", ["##", "##"], {"#": "mistrock"}, "mistrock_bricks", 4)
    shaped("cascade_stone", ["PV", "VP"], {"P": "minecraft:prismarine_shard", "V": "violetstone_bricks"}, "cascade_stone", 2)
    shaped("cascade_key", ["GDG", " S ", " S "], {"G": "shimmercap", "D": "minecraft:diamond", "S": "minecraft:stick"}, "cascade_key", cat="equipment")
    shapeless("blue_dye_from_azure_bloom", ["azure_bloom"], "minecraft:blue_dye", 2, cat="misc")

    # --- worldgen ---
    wg("configured_feature", "skyroot_tree", {"type": f"{NS}:skyroot_tree", "config": {}})
    wg("placed_feature", "skyroot_tree", {"feature": f"{NS}:skyroot_tree", "placement": [
        {"type": "minecraft:count", "count": {"type": "minecraft:weighted_list", "distribution": [
            {"weight": 2, "data": 0}, {"weight": 3, "data": 1}, {"weight": 2, "data": 2}]}},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
        {"type": "minecraft:biome"}]})
    wg("configured_feature", "sunpalm", {"type": f"{NS}:sunpalm", "config": {}})
    wg("placed_feature", "sunpalm", {"feature": f"{NS}:sunpalm", "placement": [
        {"type": "minecraft:count", "count": 2}, {"type": "minecraft:in_square"},
        {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"}]})

    def vpatch(name, state, tries, spread, count):
        wg("configured_feature", name, {"type": "minecraft:random_patch", "config": {
            "tries": tries, "xz_spread": spread, "y_spread": 3,
            "feature": {"feature": {"type": "minecraft:simple_block", "config": {
                "to_place": {"type": "minecraft:simple_state_provider", "state": {"Name": state}}}},
                "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {
                    "type": "minecraft:matching_blocks", "blocks": "minecraft:air"}}]}}})
        wg("placed_feature", name, {"feature": f"{NS}:{name}", "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"}]})

    vpatch("azure_bloom_patch", f"{NS}:azure_bloom", 40, 6, 4)
    vpatch("tall_azure_bloom_patch", f"{NS}:tall_azure_bloom", 24, 5, 2)
    vpatch("mistfall_grass_patch", "minecraft:grass", 48, 7, 6)
    vpatch("mistfall_fern_patch", "minecraft:large_fern", 16, 5, 2)

    wg("biome", "mistfall_vale", {
        "has_precipitation": True, "temperature": 0.9, "downfall": 0.9,
        "effects": {
            "sky_color": 0x7EC8F5, "fog_color": 0xD8E9EE, "water_color": 0x3FD8D0, "water_fog_color": 0x1A6E78,
            "grass_color": 0x86C25A, "foliage_color": 0x6FC08A,
            "particle": {"options": {"type": f"{NS}:mist"}, "probability": 0.0015},
            "mood_sound": {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0},
            "music": {"sound": "minecraft:music.overworld.meadow", "min_delay": 12000, "max_delay": 24000, "replace_current_music": False}},
        "spawners": {"monster": [], "creature": [], "ambient": [], "axolotls": [], "underground_water_creature": [],
                     "water_creature": [{"type": "minecraft:tropical_fish", "weight": 10, "minCount": 4, "maxCount": 8}],
                     "water_ambient": [], "misc": []},
        "spawn_costs": {},
        "carvers": {},
        "features": [[], [], [], [], [], [],
                     ["minecraft:ore_coal_upper", "minecraft:ore_iron_upper", "minecraft:ore_iron_middle", "minecraft:ore_gold",
                      "minecraft:ore_redstone", "minecraft:ore_diamond", "minecraft:ore_lapis", "minecraft:ore_copper"],
                     [], [],
                     [f"{NS}:skyroot_tree", f"{NS}:sunpalm", f"{NS}:tall_azure_bloom_patch", f"{NS}:azure_bloom_patch",
                      f"{NS}:mistfall_grass_patch", f"{NS}:mistfall_fern_patch"],
                     []]})

    W(os.path.join(D, "dimension_type", "mistfall.json"), {
        "ultrawarm": False, "natural": True, "piglin_safe": False, "respawn_anchor_works": False, "bed_works": True,
        "has_raids": False, "has_skylight": True, "has_ceiling": False, "coordinate_scale": 1.0, "ambient_light": 0.0,
        "logical_height": 320, "min_y": 0, "height": 320, "effects": "minecraft:overworld",
        "infiniburn": "#minecraft:infiniburn_overworld", "fixed_time": 700,
        "monster_spawn_light_level": {"type": "minecraft:uniform", "value": {"min_inclusive": 0, "max_inclusive": 7}},
        "monster_spawn_block_light_limit": 0})
    W(os.path.join(D, "dimension", "mistfall.json"), {
        "type": f"{NS}:mistfall",
        "generator": {"type": f"{NS}:mistfall", "biome_source": {"type": "minecraft:fixed", "biome": f"{NS}:mistfall_vale"}}})

    for k, (e, t) in {
        "mistrock": ("Mistrock", "Sis Kayası"), "banded_mistrock": ("Banded Mistrock", "Bantlı Sis Kayası"),
        "deep_mistrock": ("Deep Mistrock", "Derin Sis Kayası"), "mistrock_bricks": ("Mistrock Bricks", "Sis Kayası Tuğlası"),
        "skyroot_log": ("Skyroot Log", "Gökkök Kütüğü"), "skyroot_wood": ("Skyroot Wood", "Gökkök Odunu"),
        "skyroot_planks": ("Skyroot Planks", "Gökkök Kalası"), "skyroot_leaves": ("Skyroot Leaves", "Gökkök Yaprakları"),
        "golden_skyroot_leaves": ("Golden Skyroot Leaves", "Altın Gökkök Yaprakları"), "skyroot_sapling": ("Skyroot Sapling", "Gökkök Fidanı"),
        "sunpalm_log": ("Sunpalm Log", "Güneş Palmiyesi Kütüğü"), "sunpalm_fronds": ("Sunpalm Fronds", "Güneş Palmiyesi Yaprakları"),
        "azure_bloom": ("Azure Bloom", "Gökmavi Çiçek"), "tall_azure_bloom": ("Tall Azure Bloom", "Uzun Gökmavi Çiçek"),
        "cascade_stone": ("Cascade Stone", "Şelale Taşı"), "mistfall_portal": ("Mistfall Portal", "Sisli Şelaleler Portalı"),
    }.items():
        NAMES[k] = (e, t)
    EXTRA_LANG.update({
        "item.valdoria.cascade_key": ("Cascade Key", "Şelale Anahtarı"),
        "biome.valdoria.mistfall_vale": ("Mistfall Vale", "Sisli Şelaleler Vadisi"),
    })


EXTRA_LANG = {}


if __name__ == "__main__":
    build_textures()
    build_blocks()
    build_tags()
    build_recipes()
    build_worldgen()
    build_creatures()
    build_mistfall()
    build_lang()
    print("Valdoria resources generated in", os.path.normpath(ROOT))
