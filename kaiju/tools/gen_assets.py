#!/usr/bin/env python3
"""Mod resources besides the model: regrowth texture, item textures/models, language files; then installs everything
(model, textures, animations, server data) into common/src/main/resources."""
import json, math, os, shutil, sys
import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
RES = os.path.join(HERE, "..", "common", "src", "main", "resources")
A = os.path.join(RES, "assets", "kaiju")
sys.path.insert(0, HERE)


def regen_texture():
    """the same atlas re-coloured as raw, wet, regrowing flesh"""
    base = np.asarray(Image.open(os.path.join(OUT, "kaiju.png")).convert("RGBA"), float)
    lum = base[..., :3].mean(-1, keepdims=True) / 255
    lo, mid, hi = np.array([60, 4, 10.]), np.array([170, 34, 40.]), np.array([240, 140, 130.])
    t = np.clip(lum * 2.2, 0, 1)
    rgb = np.where(t < 0.5, lo + (mid - lo) * (t / 0.5), mid + (hi - mid) * ((t - 0.5) / 0.5))
    out = np.concatenate([rgb, base[..., 3:]], -1).astype(np.uint8)
    Image.fromarray(out, "RGBA").save(os.path.join(OUT, "kaiju_regen.png"))


def item_textures(dst):
    os.makedirs(dst, exist_ok=True)
    rng = np.random.default_rng(3)
    # egg: charcoal, keloid speckles, cyan cracks glowing through
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 5.6, (y - 8.6) / 7.2
            if dx * dx + dy * dy <= 1:
                v = int(30 + rng.integers(0, 26) - (y < 6) * -8)
                d.point((x, y), (v, v, v + 3, 255))
    for (x, y) in [(6, 4), (7, 5), (7, 6), (8, 7), (8, 8), (9, 9), (6, 9), (5, 10), (10, 11), (7, 12)]:
        d.point((x, y), (110, 230, 255, 255))
    d.point((5, 5), (90, 88, 86, 255))
    im.save(os.path.join(dst, "atomic_kaiju_egg.png"))
    # controller: a dark remote with a cyan screen and a red button
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rectangle([4, 2, 11, 14], fill=(36, 38, 42, 255), outline=(14, 14, 16, 255))
    d.rectangle([5, 3, 10, 7], fill=(40, 170, 210, 255))
    d.point((6, 4), (180, 250, 255, 255))
    d.rectangle([6, 9, 7, 10], fill=(200, 40, 40, 255))
    d.rectangle([9, 9, 9, 10], fill=(220, 200, 60, 255))
    d.rectangle([6, 12, 9, 12], fill=(90, 92, 96, 255))
    d.line([(11, 1), (12, 0)], fill=(120, 120, 120, 255))
    im.save(os.path.join(dst, "kaiju_controller.png"))


TR = {
    "claw_swipe_l": "Pençe Savuruşu (Sol)", "claw_swipe_r": "Pençe Savuruşu (Sağ)", "claw_uppercut_l": "Pençe Aparkatı (Sol)",
    "claw_uppercut_r": "Pençe Aparkatı (Sağ)", "claw_stab_l": "Pençe Saplama (Sol)", "claw_stab_r": "Pençe Saplama (Sağ)",
    "backhand_l": "Ters El (Sol)", "backhand_r": "Ters El (Sağ)", "claw_double": "Çift Pençe", "overhead_smash": "Tepeden Ezme",
    "claw_combo3": "Üçlü Pençe Kombosu", "chest_slash": "Göğüs Yarığı",
    "tail_whip_l": "Kuyruk Kırbacı (Sol)", "tail_whip_r": "Kuyruk Kırbacı (Sağ)", "tail_whip_high_l": "Yüksek Kuyruk (Sol)",
    "tail_whip_high_r": "Yüksek Kuyruk (Sağ)", "tail_sweep_low_l": "Alçak Kuyruk Süpürme (Sol)", "tail_sweep_low_r": "Alçak Kuyruk Süpürme (Sağ)",
    "tail_spin_360": "360° Kuyruk Dönüşü", "tail_double_spin": "Çift Kuyruk Dönüşü", "tail_overhead_slam": "Tepeden Kuyruk Çarpması",
    "tail_stab_back": "Geri Kuyruk Saplaması", "tail_ground_slam_back": "Arkaya Yer Çarpması", "tail_jab_l": "Kuyruk Dürtmesi (Sol)",
    "tail_jab_r": "Kuyruk Dürtmesi (Sağ)",
    "bite_lunge": "Atılarak Isırma", "bite_neck": "Boyundan Isırıp Sallama", "bite_arm": "Kol Koparan Isırık", "headbutt": "Kafa Atma",
    "head_slam_down": "Aşağı Kafa Çarpması", "bite_throw": "Isırıp Fırlatma", "snap_quick": "Hızlı Kapma",
    "shoulder_charge": "Omuz Hücumu", "body_slam": "Gövde Çarpması", "hip_check_l": "Kalça Darbesi (Sol)", "hip_check_r": "Kalça Darbesi (Sağ)",
    "belly_bump": "Göbek Vuruşu", "leaping_slam": "Sıçrayıp Çarpma", "bulldoze": "Buldozer İtişi", "chest_bump": "Göğüs Çarpması",
    "stomp_l": "Ezme Basışı (Sol)", "stomp_r": "Ezme Basışı (Sağ)", "side_kick_l": "Yan Tekme (Sol)", "side_kick_r": "Yan Tekme (Sağ)",
    "front_kick": "Ön Tekme", "ground_pound": "Yer Dövme", "knee_strike": "Diz Darbesi",
    "grab_throw": "Yakala ve Fırlat", "grab_bodyslam": "Kaldırıp Yere Çal", "headlock_bite": "Kafa Kilidi ve Isırık", "clinch_shove": "Sarıp İtme",
    "hip_throw": "Kalçadan Fırlatma", "tail_wrap_l": "Kuyrukla Bacak Dolama (Sol)", "tail_wrap_r": "Kuyrukla Bacak Dolama (Sağ)", "pin_down": "Yere Bastırma",
    "uproot_tree": "Ağacı Kökünden Sök", "grab_chunk": "Bina/Kaya Kopar", "throw_held": "Tutulanı Fırlat", "swing_held": "Tutulanla Vur",
    "kick_debris": "Enkaz Tekmesi", "rip_ground_throw": "Toprağı Söküp Fırlat",
    "atomic_breath": "Atomik Nefes", "atomic_sweep": "Süpüren Atomik Nefes", "atomic_burst": "Kısa Atomik Patlama", "atomic_aerial": "Havaya Atomik Nefes",
    "atomic_ground_pin": "Yerdekine Atomik Nefes", "atomic_point_blank": "Dipdibe Atomik Nefes", "atomic_charged_max": "Tam Yüklü Atomik Nefes",
    "atomic_ground": "Yerden Atomik Nefes", "nuclear_pulse": "Nükleer Dalga",
    "stomp_downed": "Yerdekini Ezme", "mount_claw": "Üstüne Çıkıp Pençeleme", "elbow_drop": "Dirsek Düşüşü", "tail_slam_downed": "Yerdekine Kuyruk Çarpması",
    "pin_bite": "Bastırıp Isırma",
    "ground_kick": "Yerden Tekme", "ground_tail_sweep": "Yerden Kuyruk Süpürme", "roll_away": "Yuvarlanıp Kaç", "kip_up_strike": "Sıçrayıp Kalkma Vuruşu",
    "ground_claw_grab": "Yerden Bilek Yakalama",
    "block_arms": "Kollarla Blok", "sidestep_l": "Sola Kaçış", "sidestep_r": "Sağa Kaçış", "backstep": "Geri Adım", "parry_counter": "Savuştur ve Karşı Saldır",
    "roar": "Kükreme", "tail_lash_intimidate": "Kuyruk Şaklatıp Gözdağı",
}
CATS = {"claw": ("Claws", "Pençe"), "tail": ("Tail", "Kuyruk"), "bite": ("Head & Bite", "Kafa & Isırık"), "body": ("Body", "Gövde"),
        "leg": ("Legs", "Bacak"), "grapple": ("Grapples", "Kavrama"), "world": ("World", "Dünya"), "atomic": ("Atomic", "Atomik"),
        "mount": ("On a downed foe", "Yerdeki Düşmana"), "ground": ("From the ground", "Yerden"), "defense": ("Defense", "Savunma"),
        "intimidate": ("Intimidate", "Gözdağı")}
REG = {"head": ("head", "kafa"), "neck": ("neck", "boyun"), "chest": ("chest", "göğüs"), "belly": ("belly", "karın"), "arm_l": ("left arm", "sol kol"),
       "arm_r": ("right arm", "sağ kol"), "legs": ("legs", "bacaklar"), "tail": ("tail", "kuyruk")}
PART = {"arm_l": ("left arm", "sol kol"), "arm_r": ("right arm", "sağ kol"), "tail_tip": ("tail tip", "kuyruk ucu")}


def en_name(i):
    words = i.replace("_l", " (L)").replace("_r", " (R)").replace("_", " ").split(" ")
    return " ".join(w.capitalize() if not w.startswith("(") else w for w in words)


def lang(attacks):
    keys_en = "LMB light · RMB heavy · R breath (hold) · G grab/throw · V tail · C bite · Space stomp · Shift guard · Alt dodge · Z all attacks · Y special · N pulse · B roar · H AI"
    keys_tr = "Sol tık hafif · Sağ tık ağır · R nefes (basılı) · G yakala/fırlat · V kuyruk · C ısırık · Boşluk ezme · Shift blok · Alt kaçış · Z tüm saldırılar · Y özel · N dalga · B kükreme · H YZ"
    base = {
        "entity.kaiju.atomic_kaiju": ("Atomic Kaiju", "Atomik Kaiju"),
        "entity.kaiju.debris": ("Debris", "Enkaz"), "entity.kaiju.severed_part": ("Severed Limb", "Kopmuş Uzuv"),
        "item.kaiju.atomic_kaiju_egg": ("Atomic Kaiju Egg", "Atomik Kaiju Yumurtası"),
        "item.kaiju.egg.tip": ("Use on the ground to hatch a film-scale kaiju (about 50 blocks tall).", "Film boyutunda (yaklaşık 50 blok) bir kaiju çıkarmak için yere kullan."),
        "item.kaiju.kaiju_controller": ("Kaiju Controller", "Kaiju Kumandası"),
        "item.kaiju.controller.tip": ("Right-click: control the nearest kaiju you hatched. Again: let go.", "Sağ tık: çıkardığın en yakın kaijuyu kontrol et. Tekrar: bırak."),
        "msg.kaiju.controlling": ("You are the kaiju. Z shows every attack.", "Artık kaiju sensin. Z tüm saldırıları gösterir."),
        "msg.kaiju.released": ("Control released.", "Kontrol bırakıldı."), "msg.kaiju.none": ("No kaiju of yours nearby.", "Yakında senin kaijun yok."),
        "msg.kaiju.ai_on": ("AI assist on: it fights by itself, you ride along.", "YZ yardımı açık: kendi savaşıyor, sen izliyorsun."),
        "msg.kaiju.ai_off": ("AI assist off: you are in control.", "YZ yardımı kapalı: kontrol sende."),
        "key.categories.kaiju": ("Kaiju", "Kaiju"),
        "key.kaiju.breath": ("Atomic breath (hold)", "Atomik nefes (basılı tut)"), "key.kaiju.grab": ("Grab / throw", "Yakala / fırlat"),
        "key.kaiju.tail": ("Tail attack", "Kuyruk saldırısı"), "key.kaiju.bite": ("Bite", "Isırık"), "key.kaiju.pulse": ("Nuclear pulse", "Nükleer dalga"),
        "key.kaiju.roar": ("Roar", "Kükreme"), "key.kaiju.dodge": ("Dodge", "Kaçış"), "key.kaiju.special": ("Special attack", "Özel saldırı"),
        "key.kaiju.wheel": ("All attacks", "Tüm saldırılar"), "key.kaiju.ai": ("AI assist", "YZ yardımı"), "key.kaiju.debug": ("Show hitboxes", "Çarpışma kürelerini göster"),
        "screen.kaiju.wheel": ("Attacks: click to perform (also set as special, Y)", "Saldırılar: tıkla ve yap (Y özel tuşuna da atanır)"),
        "hud.kaiju.firing": ("ATOMIC BREATH", "ATOMİK NEFES"), "hud.kaiju.charging": ("Plates charging", "Plakalar yükleniyor"),
        "hud.kaiju.special": ("Special (Y)", "Özel (Y)"), "hud.kaiju.burns": ("Burns", "Yanıklar"), "hud.kaiju.regen": ("Regrowing", "Yenileniyor"),
        "hud.kaiju.ai": ("AI assist", "YZ yardımı"), "hud.kaiju.keys": (keys_en, keys_tr),
    }
    en, tr = {}, {}
    for k, (e, t) in base.items():
        en[k], tr[k] = e, t
    for a in attacks:
        en[f"attack.kaiju.{a['id']}"] = en_name(a["id"])
        tr[f"attack.kaiju.{a['id']}"] = TR.get(a["id"], en_name(a["id"]))
    for c, (e, t) in CATS.items():
        en[f"cat.kaiju.{c}"], tr[f"cat.kaiju.{c}"] = e, t
    for c, (e, t) in REG.items():
        en[f"region.kaiju.{c}"], tr[f"region.kaiju.{c}"] = e, t
    for c, (e, t) in PART.items():
        en[f"part.kaiju.{c}"], tr[f"part.kaiju.{c}"] = e, t
    missing = [a["id"] for a in attacks if a["id"] not in TR]
    if missing:
        print("no Turkish name for:", missing)
    os.makedirs(os.path.join(A, "lang"), exist_ok=True)
    json.dump(en, open(os.path.join(A, "lang", "en_us.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    json.dump(tr, open(os.path.join(A, "lang", "tr_tr.json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)


def main():
    regen_texture()
    for sub in ("geo/entity", "textures/entity", "animations/entity", "models/item", "textures/item"):
        os.makedirs(os.path.join(A, sub), exist_ok=True)
    shutil.copy(os.path.join(OUT, "kaiju.geo.json"), os.path.join(A, "geo/entity"))
    shutil.copy(os.path.join(OUT, "kaiju.animation.json"), os.path.join(A, "animations/entity"))
    for t in ("kaiju", "kaiju_glowmask", "kaiju_burning", "kaiju_burning_glowmask", "kaiju_regen"):
        shutil.copy(os.path.join(OUT, t + ".png"), os.path.join(A, "textures/entity"))
    data = os.path.join(RES, "data", "kaiju", "kaiju")
    os.makedirs(data, exist_ok=True)
    shutil.copy(os.path.join(OUT, "atomic_kaiju.json"), data)
    item_textures(os.path.join(A, "textures/item"))
    for it in ("atomic_kaiju_egg", "kaiju_controller"):
        json.dump({"parent": "item/generated", "textures": {"layer0": f"kaiju:item/{it}"}}, open(os.path.join(A, "models/item", it + ".json"), "w"))
    spec = json.load(open(os.path.join(OUT, "atomic_kaiju.json")))
    lang(spec["attacks"])
    print("installed;", len(spec["attacks"]), "attacks")


if __name__ == "__main__":
    main()
