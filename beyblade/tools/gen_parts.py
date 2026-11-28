#!/usr/bin/env python3
"""Assets for the added Metal series performance tips and spin tracks:
item icons (drawn from the same lathe profile the game turns), item models, recipes, stadium loot and en/tr names.
Re-runnable: existing entries are overwritten, nothing else is touched."""
import json
import math
import os
from PIL import Image, ImageDraw

RES = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'common', 'src', 'main', 'resources')
A = os.path.join(RES, 'assets', 'librabeyblade')
D = os.path.join(RES, 'data', 'librabeyblade')

# id, enum, colour, accent, en name, tr name, en desc, tr desc, recipe ingredients
TIPS = [
    ('tip_hf', 'hole_flat', 0xE8E8EC, 0x404040, 'HF (Hole Flat)', 'HF (Delikli Düz)',
     'Hole flat: a flat tip with a hole, a little less grip than F', 'Delikli düz: ortası delik düz uç, F\'den biraz az tutuş', ['iron_nugget', 'flint', 'stone_button']),
    ('tip_wf', 'wide_flat', 0xE0E0E4, 0x305FDA, 'WF (Wide Flat)', 'WF (Geniş Düz)',
     'Wide flat: big flat contact, fast and hard to tip over', 'Geniş düz: geniş temas, hızlı ve zor devrilir', ['iron_nugget', 'flint', 'iron_nugget']),
    ('tip_wsf', 'wide_semi_flat', 0xF0F0F0, 0x909090, 'WSF (Wide Semi Flat)', 'WSF (Geniş Yarı Düz)',
     'Wide semi flat: balanced movement on a broad base', 'Geniş yarı düz: geniş tabanda dengeli hareket', ['iron_nugget', 'flint', 'clay_ball']),
    ('tip_rs', 'rubber_sharp', 0x2A2A30, 0xB02E26, 'RS (Rubber Sharp)', 'RS (Lastik Sivri)',
     'Rubber sharp: a sharp point with rubber bite, holds the centre against attacks', 'Lastik sivri: lastik tutuşlu sivri uç, saldırıya karşı merkezde kalır', ['iron_nugget', 'slime_ball', 'flint']),
    ('tip_rb', 'rubber_ball', 0x30343C, 0x3399FF, 'RB (Rubber Ball)', 'RB (Lastik Top)',
     'Rubber ball: rolls and grips, absorbs hits and pushes back', 'Lastik top: yuvarlanır ve tutunur, darbeyi emip geri iter', ['iron_nugget', 'slime_ball', 'snowball']),
    ('tip_rdf', 'rubber_defense_flat', 0x2A2A30, 0xFF9800, 'RDF (Rubber Defense Flat)', 'RDF (Lastik Savunma Düz)',
     'Rubber defense flat: wide rubber flat, very hard to knock out', 'Lastik savunma düz: geniş lastik taban, sahadan atmak çok zor', ['iron_nugget', 'slime_ball', 'iron_ingot']),
    ('tip_lrf', 'left_rubber_flat', 0x2A2A30, 0x993399, 'LRF (Left Rubber Flat)', 'LRF (Sol Lastik Düz)',
     'Left rubber flat: rubber cut for left spin, fastest on a left spinner', 'Sol lastik düz: sola dönüş için kesilmiş lastik, sol dönen beyde en hızlısı', ['iron_nugget', 'slime_ball', 'purple_dye']),
    ('tip_sd', 'semi_defense', 0xE8E8EC, 0x707078, 'SD (Semi Defense)', 'SD (Yarı Savunma)',
     'Semi defense: rounded defense tip, stamina with some movement', 'Yarı savunma: yuvarlak savunma ucu, biraz hareketli dayanıklılık', ['iron_nugget', 'flint', 'cobblestone']),
    ('tip_ds', 'defense_sharp', 0xF0F0F0, 0x3333FF, 'DS (Defense Sharp)', 'DS (Savunma Sivri)',
     'Defense sharp: a sharp point under a wide skirt, stays up when tilted', 'Savunma sivri: geniş eteğin altında sivri uç, eğilince bile ayakta kalır', ['iron_nugget', 'flint', 'iron_nugget']),
    ('tip_ms', 'metal_sharp', 0xC8CCD4, 0x909090, 'MS (Metal Sharp)', 'MS (Metal Sivri)',
     'Metal sharp: a heavy steel point, the least floor friction of any sharp', 'Metal sivri: ağır çelik uç, sivriler içinde en az sürtünme', ['iron_ingot', 'flint']),
    ('tip_mb', 'metal_ball', 0xC8CCD4, 0x707880, 'MB (Metal Ball)', 'MB (Metal Top)',
     'Metal ball: a steel ball, heavy and steady, slides instead of gripping', 'Metal top: çelik top, ağır ve dengeli, tutunmak yerine kayar', ['iron_ingot', 'snowball']),
    ('tip_gb', 'gravity_ball', 0x3A6A3A, 0xD0D0D0, 'GB (Gravity Ball)', 'GB (Yerçekimi Top)',
     'Gravity ball: a wide weighted ball, tough defense', 'Yerçekimi top: geniş ağırlıklı top, sağlam savunma', ['iron_ingot', 'snowball', 'iron_nugget']),
    ('tip_q', 'quake', 0xE8E8EC, 0xFF6633, 'Q (Quake)', 'Q (Deprem)',
     'Quake: an uneven tip that makes the bey jump around unpredictably', 'Deprem: düzensiz uç, beyi rastgele zıplatır', ['iron_nugget', 'flint', 'gravel']),
    ('tip_bd', 'bearing_drive', 0xD0D0D0, 0x205FE0, 'B:D (Bearing Drive)', 'B:D (Rulman Sürüş)',
     'Bearing drive: a free spinning bearing tip, the best stamina of all', 'Rulman sürüş: serbest dönen rulmanlı uç, en yüksek dayanıklılık', ['iron_ingot', 'flint', 'redstone']),
]
# id, height, trait key, colour, accent, en, tr, ingredients
TRACKS = [
    ('track_120', 120, None, 0x50545C, 0x8A8F98, '120 Spin Track', '120 Dönüş Yolu', ['iron_ingot', 'stick']),
    ('track_130', 130, None, 0x404650, 0x8090A0, '130 Spin Track', '130 Dönüş Yolu', ['iron_ingot', 'stick', 'stick']),
    ('track_160', 160, None, 0x404650, 0x8090A0, '160 Spin Track', '160 Dönüş Yolu', ['iron_ingot', 'iron_ingot', 'stick']),
    ('track_ch120', 120, 'twin', 0x30343C, 0xFFD040, 'CH120 (Change Height) Spin Track', 'CH120 (Yükseklik Değiştiren) Dönüş Yolu', ['iron_ingot', 'stick', 'gold_nugget']),
    ('track_ed145', 145, 'ring', 0x30343C, 0xD0E0FF, 'ED145 (Eternal Defense) Spin Track', 'ED145 (Sonsuz Savunma) Dönüş Yolu', ['iron_ingot', 'stick', 'shield']),
    ('track_df145', 145, 'wing', 0x2A6ACA, 0xF0F0F0, 'DF145 (Down Force) Spin Track', 'DF145 (Bastırma Kuvveti) Dönüş Yolu', ['iron_ingot', 'stick', 'feather']),
    ('track_ad145', 145, 'ring', 0x606060, 0xFFB400, 'AD145 (Armor Defense) Spin Track', 'AD145 (Zırh Savunma) Dönüş Yolu', ['iron_ingot', 'stick', 'iron_chestplate']),
    ('track_sw145', 145, 'claw', 0x30343C, 0xFF6633, 'SW145 (Switch) Spin Track', 'SW145 (Anahtar) Dönüş Yolu', ['iron_ingot', 'stick', 'lever']),
    ('track_lw160', 160, 'wing', 0x993399, 0xF0F0F0, 'LW160 (Left Wing) Spin Track', 'LW160 (Sol Kanat) Dönüş Yolu', ['iron_ingot', 'stick', 'phantom_membrane']),
]


def rgb(c, k=1.0):
    return (min(255, int((c >> 16 & 255) * k)), min(255, int((c >> 8 & 255) * k)), min(255, int((c & 255) * k)), 255)


def tip_profile(enum):
    top = 0.38 if enum in ('wide_flat', 'wide_semi_flat', 'gravity_ball', 'rubber_defense_flat') else (0.4 if enum == 'bearing_drive' else 0.36)
    P = [(0.26, top, False)]
    if enum == 'hole_flat':
        P += [(0.31, top - .08, False), (0.31, .12, False), (0.23, .03, True), (0.2, 0, True), (0.1, 0, True), (0.08, .05, False)]
    elif enum == 'wide_flat':
        P += [(0.42, top - .08, False), (0.42, .12, False), (0.33, .03, True), (0.3, 0, True)]
    elif enum == 'wide_semi_flat':
        P += [(0.42, top - .08, False), (0.38, .14, False), (0.22, .03, True), (0.16, 0, True)]
    elif enum in ('rubber_sharp', 'metal_sharp'):
        P += [(0.3, top - .08, False), (0.27, .19, enum == 'metal_sharp'), (0.14, .07, True), (0.04, .01, True)]
    elif enum in ('rubber_ball', 'metal_ball', 'gravity_ball'):
        w = 0.36 if enum == 'gravity_ball' else 0.31
        P += [(w, top - .1, enum == 'metal_ball')]
        for i in range(1, 6):
            a = i / 5 * math.pi / 2
            P.append((w * math.cos(a * .85) * .95, (top - .1) * math.cos(a), i >= 2 or enum == 'metal_ball'))
    elif enum == 'rubber_defense_flat':
        P += [(0.34, top - .08, False), (0.34, .18, False), (0.26, .06, True), (0.22, 0, True)]
    elif enum == 'left_rubber_flat':
        P += [(0.31, top - .08, False), (0.31, .16, False), (0.32, .12, True), (0.30, .02, True), (0.26, 0, True)]
    elif enum == 'semi_defense':
        P += [(0.34, top - .08, False), (0.32, .16, False), (0.2, .05, True), (0.1, 0, True)]
    elif enum == 'defense_sharp':
        P += [(0.34, top - .08, False), (0.34, .18, False), (0.14, .07, True), (0.04, .008, True)]
    elif enum == 'quake':
        P += [(0.31, top - .08, False), (0.31, .14, False), (0.24, .05, True), (0.2, 0, True), (0.14, .035, True), (0.08, 0, True)]
    elif enum == 'bearing_drive':
        P += [(0.3, top - .08, False), (0.3, .2, False), (0.22, .17, True), (0.2, .13, True), (0.12, .06, False), (0.03, .008, True)]
    P.append((0, 0, True))
    return P


def track_profile(h, trait):
    top = h * 0.0034482758
    P = [(0.3, top, False), (0.3, top - .04, False), (0.21, top - .08, False)]
    fr, fy, ft = {'twin': (0.36, 0.65, 0.04), 'ring': (0.44, 0.22, 0.05), 'wing': (0.46, 0.45, 0.025), 'claw': (0.42, 0.6, 0.04)}.get(trait, (0, 0, 0))
    if fr:
        yc = top * fy
        P += [(0.2, yc + ft / 2 + .03, False), (fr, yc + ft / 2, True), (fr, yc - ft / 2, True), (0.2, yc - ft / 2 - .03, False)]
    P += [(0.19, .04, False), (0.26, 0, False)]
    return P, top


def draw_icon(path, prof, col, acc, height, rubber=False):
    """Side view of the turned part: mirror the profile around the axis, shade like metal / matte rubber."""
    S = 32
    img = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    scale = min(26 / max(0.001, 2 * max(p[0] for p in prof)), 26 / max(0.001, height))
    cx = S / 2
    base = S / 2 + height * scale / 2

    def pt(r, y):
        return (cx + r * scale, base - y * scale)

    for i in range(len(prof) - 1):
        a, b = prof[i], prof[i + 1]
        c = acc if b[2] else col
        for side in (1, -1):
            poly = [pt(side * a[0], a[1]), pt(side * b[0], b[1]), pt(0, b[1]), pt(0, a[1])]
            k = 1.0 if side > 0 else 0.78
            if rubber and b[2]:
                k *= 0.85
            d.polygon(poly, fill=rgb(c, k))
    # specular stripe for metal look, outline
    px = img.load()
    for y in range(S):
        for x in range(S):
            r, g, b, al = px[x, y]
            if al == 0:
                continue
            u = (x - cx) / 13.0
            hl = 1.0 + 0.35 * math.exp(-((u - 0.35) ** 2) / 0.02)
            px[x, y] = (min(255, int(r * hl)), min(255, int(g * hl)), min(255, int(b * hl)), 255)
    out = img.copy()
    po = out.load()
    for y in range(S):
        for x in range(S):
            if px[x, y][3] == 0 and any(0 <= x + dx < S and 0 <= y + dy < S and px[x + dx, y + dy][3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                po[x, y] = (24, 24, 28, 255)
    out.save(path)


def merge_lang(fn, entries):
    p = os.path.join(A, 'lang', fn)
    with open(p, encoding='utf-8') as f:
        data = json.load(f)
    data.update(entries)
    with open(p, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=1)


def item_files(pid, ingredients):
    iid = 'part_' + pid
    with open(os.path.join(A, 'models', 'item', iid + '.json'), 'w') as f:
        json.dump({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'librabeyblade:item/' + iid}}, f)
    with open(os.path.join(D, 'recipes', iid + '.json'), 'w') as f:
        json.dump({'type': 'minecraft:crafting_shapeless', 'ingredients': [{'item': 'minecraft:' + i} for i in ingredients],
                   'result': {'item': 'librabeyblade:' + iid}}, f, indent=1)


def main():
    en, tr = {}, {}
    loot = []
    for pid, enum, col, acc, n_en, n_tr, d_en, d_tr, ing in TIPS:
        prof = tip_profile(enum)
        draw_icon(os.path.join(A, 'textures', 'item', 'part_' + pid + '.png'), prof, col, acc, max(p[1] for p in prof), rubber='rubber' in enum)
        item_files(pid, ing)
        for lang, n, dsc in ((en, n_en + ' Performance Tip', d_en), (tr, n_tr + ' Performans Ucu', d_tr)):
            lang['part.librabeyblade.' + pid] = n
            lang['item.librabeyblade.part_' + pid] = n
            lang['tip.librabeyblade.' + enum] = dsc
        loot.append(pid)
    for pid, h, trait, col, acc, n_en, n_tr, ing in TRACKS:
        prof, top = track_profile(h, trait)
        draw_icon(os.path.join(A, 'textures', 'item', 'part_' + pid + '.png'), prof, col, acc, top)
        item_files(pid, ing)
        for lang, n in ((en, n_en), (tr, n_tr)):
            lang['part.librabeyblade.' + pid] = n
            lang['item.librabeyblade.part_' + pid] = n
        loot.append(pid)
    merge_lang('en_us.json', en)
    merge_lang('tr_tr.json', tr)
    # stadium chest loot: add the new parts to the parts pool
    lp = os.path.join(D, 'loot_tables', 'chests', 'stadium.json')
    with open(lp) as f:
        lt = json.load(f)
    for pool in lt['pools']:
        names = [e.get('name', '') for e in pool['entries']]
        if any('part_tip_' in n for n in names):
            for pid in loot:
                if 'librabeyblade:part_' + pid not in names:
                    pool['entries'].append({'type': 'minecraft:item', 'name': 'librabeyblade:part_' + pid, 'weight': 1})
    with open(lp, 'w') as f:
        json.dump(lt, f, indent=1)
    print('%d tips, %d tracks' % (len(TIPS), len(TRACKS)))


if __name__ == '__main__':
    main()
