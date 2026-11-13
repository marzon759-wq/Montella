#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Builds en_us.json and tr_tr.json and checks that every key the code uses exists."""
import json, os, re, sys

sys.path.insert(0, os.path.dirname(__file__))
from lang_story import L
from lang_notes import N

OUT = sys.argv[1] if len(sys.argv) > 1 else "common/src/main/resources/assets/hatman"


def add(key, en, tr):
    L[key] = (en, tr)


for nid, (te, be, tt, bt) in N.items():
    add(f"note.hatman.{nid}.title", te, tt)
    add(f"note.hatman.{nid}.body", be, bt)

UI = {
    "itemGroup.hatman.main": ("The Hatman: Hollow House", "Şapkalı Adam: Oyuk Ev"),
    "entity.hatman.hatman": ("The Hatman", "Şapkalı Adam"),
    "entity.hatman.guest": ("Masked Guest", "Maskeli Misafir"),
    "key.hatman.camera": ("Night vision camera on/off", "Gece görüş kamerası aç/kapat"),
    "key.categories.hatman": ("The Hatman", "Şapkalı Adam"),
    "hud.hatman.objective": ("OBJECTIVE", "GÖREV"),
    "hud.hatman.notes": ("Notes %s/%s", "Notlar %s/%s"),
    "hud.hatman.chapter": ("CHAPTER %s", "BÖLÜM %s"),
    "hud.hatman.page": ("page %s / %s   —   click to turn, ESC to close", "sayfa %s / %s   —   çevirmek için tıkla, kapatmak için ESC"),
    "hud.hatman.close": ("click or ESC to close", "kapatmak için tıkla ya da ESC"),
    "hud.hatman.skip": ("hold SPACE to skip", "geçmek için BOŞLUK'u basılı tut"),
    "hud.hatman.safe": ("MARLOWE & SON", "MARLOWE VE OĞLU"),
    "hint.hatman.cam": ("Press [%s] to switch Ruth's night vision camera on and off. It sees in the dark. It sees HIM. It eats batteries: look in drawers.",
                        "Ruth'un gece görüş kamerasını açıp kapatmak için [%s] tuşuna bas. Karanlıkta görür. ONU görür. Pil yer: çekmecelere bak."),
    "speaker.hatman.hatman": ("The Hatman", "Şapkalı Adam"),
    "speaker.hatman.voice": ("A voice on the line", "Hattaki ses"),
    "speaker.hatman.ruth": ("Ruth Calloway", "Ruth Calloway"),
    "speaker.hatman.tv": ("Television — 1962", "Televizyon — 1962"),
}
MSG = {
    "busy": ("Someone else is already inside Marlowe House.", "Marlowe Köşkü'nde zaten biri var."),
    "building": ("Marlowe House is being raised around you. Hold still...", "Marlowe Köşkü etrafında yükseliyor. Kıpırdama..."),
    "not_running": ("No story is running. Use /hatman start", "Çalışan bir hikâye yok. /hatman basla yaz"),
    "stopped": ("The story has been stopped. The house is unlocked.", "Hikâye durduruldu. Evin kilitleri açıldı."),
    "done": ("%s — Thank you for playing THE HATMAN: HOLLOW HOUSE. Use /hatman start to play again and answer him differently.",
             "%s — ŞAPKALI ADAM: OYUK EV'i oynadığın için teşekkürler. Tekrar oynayıp ona farklı cevaplar vermek için /hatman basla yaz."),
    "picked": ("Picked up: %s", "Alındı: %s"),
    "unlocked": ("Unlocked.", "Kilit açıldı."),
    "sealed": ("It will not open. The house has not finished with you.", "Açılmıyor. Evin seninle işi bitmedi."),
    "locked_inside": ("Locked. From the inside.", "Kilitli. İçeriden."),
    "locked_key": ("Locked. It needs a key.", "Kilitli. Anahtar lazım."),
    "locked": ("Locked.", "Kilitli."),
    "drawer_empty": ("Nothing else in here.", "İçinde başka bir şey yok."),
    "drawer_junk0": ("Moth-eaten gloves. Nothing useful.", "Güve yemiş eldivenler. İşe yarar bir şey yok."),
    "drawer_junk1": ("Hat pins, dozens of them, all bent.", "Onlarca şapka iğnesi; hepsi eğrilmiş."),
    "drawer_junk2": ("Receipts from 1961. Dust.", "1961'den kalma makbuzlar. Toz."),
    "drawer_junk3": ("A child's tooth in a matchbox.", "Kibrit kutusunda bir çocuk dişi."),
    "drawer_junk4": ("Empty. The dust inside has finger marks in it. Long ones.", "Boş. İçindeki tozda parmak izleri var. Uzun parmaklar."),
    "hide_cam": ("The camera's lamp glows through the slats. TURN IT OFF.", "Kameranın lambası aralıklardan parlıyor. KAPAT ONU."),
    "hide_hint": ("You are hidden. Stay still. Sneak or jump to step out.", "Saklandın. Kıpırdama. Çıkmak için eğil ya da zıpla."),
    "hide_now": ("Get in a wardrobe. NOW.", "Bir gardıroba gir. ŞİMDİ."),
    "hide_exit": ("[SNEAK] step out", "[EĞİL] dışarı çık"),
    "cam_dead": ("The battery is dead. Find another.", "Pil bitti. Yenisini bul."),
    "cam_swap": ("Battery replaced.", "Pil değiştirildi."),
    "cam_hint": ("Something is there. You can't see it. The camera can.", "Orada bir şey var. Sen göremiyorsun. Kamera görebilir."),
    "bar_hint": ("Bar the door! (use the study door from inside)", "Kapıyı sürgüle! (çalışma odası kapısını içeriden kullan)"),
    "barred": ("You drop the bar across the door.", "Sürgüyü kapıya indiriyorsun."),
    "bar_stays": ("The bar stays where it is.", "Sürgü yerinde kalıyor."),
    "he_is_there": ("He is standing right behind this door.", "Tam bu kapının arkasında duruyor."),
    "gen_running": ("The generator is running.", "Jeneratör çalışıyor."),
    "gen_empty": ("The tank is empty. It needs fuel.", "Depo boş. Yakıt lazım."),
    "phone_dead": ("No dial tone. The cord has been cut for sixty years.", "Çevir sesi yok. Kablosu altmış yıldır kesik."),
    "tv_static": ("Only static. For now.", "Yalnızca parazit. Şimdilik."),
    "clock": ("It is ticking. Nobody has wound it since 1962.", "Tıkırdıyor. 1962'den beri kimse kurmadı."),
    "plate_you": ("The plate under the empty hook reads: %s", "Boş kancanın altındaki plakada şöyle yazıyor: %s"),
    "musicbox": ("You close the lid. It plays two more notes with the lid shut.", "Kapağını kapatıyorsun. Kapalıyken iki nota daha çalıyor."),
    "recorder_later": ("A tape recorder. Not yet.", "Bir kayıt cihazı. Henüz değil."),
    "hook_you": ("An empty hook. The plate reads: %s. The engraving is fresh.", "Boş bir kanca. Plakada şöyle yazıyor: %s. Kazıması taze."),
    "hooks": ("A hat on every hook. A name under every hat.", "Her kancada bir şapka. Her şapkanın altında bir isim."),
    "hatblock_stuck": ("A wooden hat block. It is fixed to the counter. Odd.", "Tahta bir şapka kalıbı. Tezgâha sabitlenmiş. Tuhaf."),
    "hatblock_hint": ("The hat block on the counter by the shelves. PULL IT!", "Rafların yanındaki tezgâhta duran şapka kalıbı. ÇEK!"),
    "shelf_open": ("The shelving swings inward.", "Raflar içeri doğru açılıyor."),
    "safe_empty": ("The safe is empty. The baize is still warm.", "Kasa boş. Çuha hâlâ sıcak."),
    "wrong_code": ("The dial does not catch.", "Kadran tutmuyor."),
    "litter": ("Old papers. Nothing you can read.", "Eski kâğıtlar. Okunacak bir şey yok."),
    "mannequin": ("A dress form. It is facing a different way than it was.", "Bir prova mankeni. Az öncekinden farklı bir yöne bakıyor."),
    "ruth_coat": ("Ruth's coat, on a dress form. It has been taken in at the shoulders. Fitted.", "Ruth'un paltosu, bir mankenin üstünde. Omuzları daraltılmış. Prova edilmiş."),
    "mirror_attic": ("You are the only one in the mirror. You count twice.", "Aynada yalnızca sen varsın. İki kez sayıyorsun."),
    "mirror_bath": ("Written in the dust: DON'T LIE TO HIM", "Toza yazılmış: ONA YALAN SÖYLEME"),
    "seat": ("A place card: %s", "Bir isim kartı: %s"),
    "host_chair": ("The head of the table. The cushion is pressed down.", "Sofranın başı. Minder çökmüş."),
    "his_chair": ("The leather is cold. Something very tall sits here.", "Deri soğuk. Burada çok uzun bir şey oturuyor."),
    "fireplace": ("Cold ashes. Sixty years cold.", "Soğuk küller. Altmış yıllık soğuk."),
    "shelf": ("Shelving. Solid. There is a draught coming through it.", "Raflar. Sağlam. Arasından soğuk bir hava sızıyor."),
    "tunnelgate": ("An iron gate. It is not locked. It simply will not open for you yet.", "Demir bir parmaklık. Kilitli değil. Sadece henüz sana açılmıyor."),
    "ruth_chair": ("A chair in the middle of the ice.", "Buzun ortasında bir sandalye."),
    "bed": ("Nobody sleeps in this house.", "Bu evde kimse uyumaz."),
}
for k, v in MSG.items():
    UI["msg.hatman." + k] = v
BLOCKS = {
    "wallpaper_green": ("Green Damask Wallpaper", "Yeşil Damask Duvar Kâğıdı"), "wallpaper_stained": ("Stained Wallpaper", "Lekeli Duvar Kâğıdı"),
    "wallpaper_red": ("Red Damask Wallpaper", "Kırmızı Damask Duvar Kâğıdı"), "wallpaper_blue": ("Blue Nursery Wallpaper", "Mavi Çocuk Odası Duvar Kâğıdı"),
    "wainscot": ("Dark Wainscot", "Koyu Lambri"), "plaster": ("Cracked Plaster", "Çatlak Sıva"), "rotten_planks": ("Rotten Planks", "Çürük Tahtalar"),
    "boarded_window": ("Boarded Window", "Tahtayla Kapatılmış Pencere"), "cellar_brick": ("Cellar Brick", "Bodrum Tuğlası"), "tile_floor": ("Checkered Tiles", "Damalı Karo"),
    "armchair": ("Worn Armchair", "Eskimiş Koltuk"), "dining_chair": ("Dining Chair", "Yemek Sandalyesi"), "can_shelf": ("Shelf of Tins", "Teneke Kutulu Raf"),
    "mirror": ("Tarnished Mirror", "Kararmış Ayna"), "camcorder": ("Ruth's Camcorder", "Ruth'un Kamerası"), "hat_box": ("Hat Box", "Şapka Kutusu"),
    "hat_stand": ("Hat Block", "Şapka Kalıbı"), "recorder": ("Tape Recorder", "Kayıt Cihazı"), "name_plate": ("Brass Name Plate", "Pirinç İsim Plakası"),
    "streamer": ("Paper Streamer", "Kâğıt Süs"), "piano": ("Upright Piano", "Duvar Piyanosu"), "bathtub": ("Bathtub", "Küvet"), "desk": ("Writing Desk", "Yazı Masası"),
    "cabinet": ("Chest of Drawers", "Çekmeceli Dolap"), "safe": ("Marlowe's Safe", "Marlowe'un Kasası"), "trunk": ("Travel Trunk", "Seyahat Sandığı"),
    "wardrobe": ("Wardrobe", "Gardırop"), "clock": ("Grandfather Clock", "Ayaklı Saat"), "mannequin": ("Dress Form", "Prova Mankeni"),
    "wall_mask": ("Masquerade Mask", "Balo Maskesi"), "paper": ("Papers", "Kâğıtlar"), "crt_tv": ("Television Set", "Televizyon"), "phone": ("Telephone", "Telefon"),
    "generator": ("Petrol Generator", "Benzinli Jeneratör"), "music_box": ("Music Box", "Müzik Kutusu"), "wall_lamp": ("Wall Lamp", "Duvar Lambası"),
    "ceiling_lamp": ("Ceiling Lamp", "Tavan Lambası"),
}
for k, v in BLOCKS.items():
    UI["block.hatman." + k] = v
ITEMS = {
    "camcorder": ("Ruth's Camcorder", "Ruth'un Kamerası"), "battery": ("Camera Battery", "Kamera Pili"), "cellar_key": ("Cellar Key", "Bodrum Anahtarı"),
    "fuel_can": ("Fuel Can", "Yakıt Bidonu"), "workshop_key": ("Workshop Key", "Atölye Anahtarı"), "ledger": ("Marlowe's Ledger", "Marlowe'un Defteri"),
    "stair_key": ("Small Brass Key", "Küçük Pirinç Anahtar"), "recorder_tape": ("Ruth's Tape", "Ruth'un Kaseti"), "attic_key": ("Attic Key", "Çatı Katı Anahtarı"),
    "badge": ("Ruth's Badge", "Ruth'un Rozeti"), "lamp_oil": ("Lamp Oil", "Lamba Yağı"), "lighter": ("Lighter", "Çakmak"),
    "original_face": ("The Deposit", "Depozito"), "cold_key": ("Cold Room Key", "Soğuk Oda Anahtarı"), "hatman_spawn_egg": ("Hatman Spawn Egg", "Şapkalı Adam Çağırma Yumurtası"),
}
for k, v in ITEMS.items():
    UI["item.hatman." + k] = v
SUB = {
    "hm.step": ("Heavy footsteps", "Ağır ayak sesleri"), "hm.breath": ("Something breathes", "Bir şey nefes alıyor"), "hm.growl": ("A low growl", "Boğuk bir hırıltı"),
    "hm.scream": ("The Hatman screams", "Şapkalı Adam çığlık atıyor"), "hm.whisper": ("Whispering", "Fısıltılar"), "hm.emerge": ("Something unfolds", "Bir şey doğruluyor"),
    "hm.vanish": ("Something is gone", "Bir şey kayboldu"), "hm.knock": ("Three knocks", "Üç vuruş"), "hm.twitch": ("Joints crack", "Eklemler çıtırdıyor"),
    "hm.grab": ("Fingers close", "Parmaklar kapanıyor"), "hm.voice": ("A voice", "Bir ses"), "hm.bang": ("Pounding on the door", "Kapı yumruklanıyor"),
    "hm.search": ("Something is listening", "Bir şey dinliyor"), "fx.static": ("Static", "Parazit"), "fx.jumpscare": ("!!!", "!!!"), "fx.thunder": ("Thunder", "Gök gürültüsü"),
    "fx.creak": ("Wood creaks", "Tahta gıcırdıyor"), "fx.door_slam": ("A door slams", "Bir kapı çarpıyor"), "fx.door_creak": ("A door creaks open", "Bir kapı gıcırdayarak açılıyor"),
    "fx.lock": ("Locked", "Kilitli"), "fx.unlock": ("A lock turns", "Bir kilit dönüyor"), "fx.drawer": ("A drawer slides", "Bir çekmece açılıyor"),
    "fx.paper": ("Paper rustles", "Kâğıt hışırtısı"), "fx.pickup": ("Item taken", "Eşya alındı"), "fx.phone_ring": ("A telephone rings", "Bir telefon çalıyor"),
    "fx.phone_pick": ("Receiver lifted", "Ahize kaldırıldı"), "fx.tv_on": ("A television switches on", "Bir televizyon açılıyor"),
    "fx.generator_start": ("Generator starts", "Jeneratör çalışıyor"), "fx.power_down": ("The power dies", "Elektrik kesiliyor"), "fx.switch": ("A click", "Bir tık sesi"),
    "fx.bulb_pop": ("A bulb bursts", "Bir ampul patlıyor"), "fx.glass": ("Glass breaks", "Cam kırılıyor"), "fx.wood_break": ("Wood splinters", "Tahta parçalanıyor"),
    "fx.music_box": ("A music box plays", "Bir müzik kutusu çalıyor"), "fx.clock_tick": ("A clock ticks", "Bir saat tıkırdıyor"), "fx.clock_chime": ("A clock strikes", "Bir saat çalıyor"),
    "fx.cam_on": ("Camera on", "Kamera açıldı"), "fx.cam_off": ("Camera off", "Kamera kapandı"), "fx.cam_low": ("Battery warning", "Pil uyarısı"),
    "fx.keypad": ("Dial clicks", "Kadran tıklıyor"), "fx.safe_open": ("A safe opens", "Bir kasa açılıyor"), "fx.fake_steps": ("Footsteps, somewhere", "Bir yerlerde ayak sesleri"),
    "fx.crow": ("A crow", "Bir karga"), "fx.sting": ("Dread", "Dehşet"), "fx.chapter": ("A low toll", "Derin bir tını"), "fx.type": ("Tick", "Tık"),
    "fx.voice_ruth": ("Ruth speaks", "Ruth konuşuyor"), "fx.thump": ("A thump", "Bir güm sesi"), "fx.piano": ("A piano note", "Bir piyano notası"), "fx.tape": ("Tape hiss", "Kaset hışırtısı"),
}
for k, v in SUB.items():
    UI["subtitles.hatman." + k] = v
for k, v in UI.items():
    L[k] = v

os.makedirs(OUT + "/lang", exist_ok=True)
for idx, name in ((0, "en_us"), (1, "tr_tr")):
    with open(f"{OUT}/lang/{name}.json", "w", encoding="utf-8") as f:
        json.dump({k: v[idx] for k, v in L.items()}, f, ensure_ascii=False, indent=1)

# ---- verification against the source
root = os.path.join(os.path.dirname(__file__), "..", "common", "src", "main", "java")
src = ""
for dp, _, fs in os.walk(root):
    for fn in fs:
        if fn.endswith(".java"):
            src += open(os.path.join(dp, fn), encoding="utf-8").read() + "\n"
need = set()
for m in re.finditer(r'\bthink\("([a-z0-9_]+)"', src):
    need.add("line." + m.group(1))
for m in re.finditer(r'(?<![a-zA-Z.])msg\("([a-z0-9_]+)"', src):
    need.add("msg.hatman." + m.group(1))
for m in re.finditer(r'(?<![a-zA-Z.])obj\("([a-z0-9_]+)"', src):
    need.add("obj.hatman." + m.group(1))
for m in re.finditer(r'"((?:msg|hud|hint|speaker|chapter|ending|credits|key|itemGroup)\.hatman\.[a-z0-9_.]+)"', src):
    k = m.group(1)
    if not k.endswith("."):
        need.add(k)
for m in re.finditer(r'checkpoint|n\("([a-z0-9_]+)", "', src):
    if m.group(1):
        need.add("dlg." + m.group(1))
missing = sorted(k for k in need if k not in L and not re.search(r'drawer_junk$|caught$|hunt$|tape$', k))
print("lang keys:", len(L), "| referenced:", len(need), "| missing:", missing)
