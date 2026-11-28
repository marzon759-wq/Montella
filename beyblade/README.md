# Beyblade Metal Fight (librabeyblade) — 2.3.0

Forge **1.20.1** and **1.21.1**, now animated with **GeckoLib 4**.

## Kurulum / Install

1. Forge + **GeckoLib** (Forge sürümü, aynı Minecraft sürümü için) kur.
2. `dist/` içinden doğru jar'ı `mods/` klasörüne at:
   - `beyblade_mod-1.20.1-forge-2.3.0.jar` — Minecraft 1.20.1, Forge 47+, GeckoLib 4.4+
   - `beyblade_mod-1.21.1-forge-2.3.0.jar` — Minecraft 1.21.1, Forge 52+, GeckoLib 4.7+

## 2.3: yazısız ekran, daha seyrek özel hamleler

- Ekrana gelen tüm yazılar kaldırıldı: özel hamle bandı (cut-in), aksiyon çubuğu mesajları (hamle duyurusu, KO,
  bekleme süresi, fırlatma gücü…), sohbet mesajları ve maç başlıkları. Hamlenin başladığını ses, kamera sarsıntısı
  ve flaş anlatır. (Geri açmak için `Msg.SHOW = true`.) Dönüş/istatistik HUD'u kalıcı gösterge olarak duruyor.
- Özel hamleler animedeki gibi zaman alıyor: bekleme süreleri 2,5 kat, maçın ilk 10 saniyesinde özel hamle yok,
  rakip beyler iki özel hamle arasında en az 20 saniye bekliyor ve daha seyrek deniyor; kalkan/uyanış gibi
  güçlendirmeler 1,5 kat uzun sürüyor.

## 2.2: savaşı hissettir

- **Çarpışma rüzgarı:** iki bey çarpıştığında rüzgar çarpışma çizgisine dik olarak iki yana savrulur (kıvrılan rüzgar
  izleri, zeminde koşan toz halkası), iki beyin enerji renkleri çarpışma noktasında parlar; ağır vuruşlarda hız
  çizgileri, yakındaki eşyalar/moblar savrulur, otlar yatar. Yeni rüzgar sesi.
- **3D L-Drago ejderhası:** düz logo yerine gerçek 3D Doğu ejderhası — pullu, sivrilen yılan gövde, sırt yüzgeçleri,
  pençeli dört bacak, timsah kafa (açılan çene, dişler, parlayan gözler, boynuzlar, bıyıklar), ışıma kabuğu.
  Uyanışta beyin etrafında sarılıp kükrer; Spin Steal'de girdap gibi döner; Wild Fire'da hedefe saldırır;
  Life Destructor'da dev boyutta hedefe atılıp ateş kusar.
- **Cut-in:** her özel hamle (kalkan/uyanış dahil) başladığında ekrana eğik bir bant kayar: bey ikonu, hamle adı, bey adı,
  hız çizgileri ve "shing" sesi. F1 ile arayüz gizliyken ve kayıtlarda da görünür.
- **Kamera:** ağır çarpışmada ve özel hamle darbelerinde kamera sarsıntısı + görüş açısı darbesi, büyük patlamalarda beyaz flaş.

## 2.1: özel hamlelerin dünyaya etkisi + yeni sesler

- **Storm Pegasus — Star Blast Attack:** bey gökyüzüne fırlar (~24 blok), Pegasus ruhu kanatlarını açar, hedefin
  üstünde yıldız kilidi belirir, kuyruklu yıldız gibi dalar: krater + şok dalgası.
- **Storm Pegasus — Storm Bringer:** Pegasus rakibin etrafında döner, rakibi içine çekip havaya kaldıran bir kasırga
  (fırtına bulutu, şimşek çatırtıları) ve sonunda yere çakma.
- **Dünyaya etki (`WorldImpact`):** krater (toprak / yanık magma / kum / yıldız / fırtına stili), çimi-çiçeği-yaprağı söken
  ve mobları-eşyaları taşıyan kasırgalar, bitkileri yatıran ve camı kıran şok dalgaları, magma ve kum alanları.
  - L-Drago dalışı: yanan magma krater, Wild Fire: zemin magmaya döner, Spin Steal girdabı her şeyi çeker.
  - Flame Libra Inferno Blast: zemin kuma döner, kum kasırgası yükselir, kum krateri; Sonic Wave halka şok dalgaları.
  - Rock Leone: Gale Force Wall rüzgar duvarı her şeyi dışarı fırlatır; deprem halkaları; dev kasırgalar.
  - Earth Eagle Diving Crush krateri, kasırgalar; Variares şimşek kılıcı krateri; Befall tüy fırtınası;
    Tempo zaman durdurma: çevredeki mobları ve eşyaları dondurur.
  - Tüm blok değişiklikleri **geçicidir**: ~30 saniye sonra dıştan içe kendiliğinden onarılır, dünya kapanırken hepsi
    geri konur. Sandık/blok-varlıkları ve kırılmaz bloklar asla değişmez. Kapatmak için:
    `/gamerule beybladeWorldDamage false`.
- **Sesler:** çarpışma, ağır çarpışma, fırlatma, iniş, krater, enkaz, kasırga, şok dalgası, gökyüzüne fırlama, meteor
  dalışı, aura, şarj, gök gürültüsü — OpenGameArt'tan CC0 gerçek kayıtlarla yeniden yapıldı (`tools/gen_sfx.py`,
  kaynaklar `CREDITS.md`). Animeden ses kullanılmadı; kendi seslerini kaynak paketiyle aynı dosya adlarıyla koyabilirsin.

## 2.0 ile gelenler / What's new

**GeckoLib iskeleti** — her bey aynı iskeleti kullanır:
`root > tilt > motion > spin > { tip_lag > tip, track, wheel, ring, bolt }`

- *Prosedürel* (her kare, `BeyRig`): dönüş, presesyon yalpalaması, gidiş yönüne yatma, vuruş titremesi,
  özel hareketlerde hedefe eğilme. Serbest dönen uçlar (ES, EWD, FD, FS, B:D) gövdeden yavaş döner.
- *Keyframe* (`animations/beyblade.animation.json`, 35 animasyon):
  - gövde döngüleri: `spin`, `spin_low`, `spin_dying` (dönüş azaldıkça artan sallantı), `airborne`, `recall`
  - bitişler: `sleep_out` (dengesini kaybedip yan yatar), `burst` (ağır darbeyle yenilince Face Bolt, Energy Ring,
    Fusion Wheel, Spin Track ve Performance Tip ayrı ayrı fırlayıp yere düşer)
  - uç döngüleri: `tip_rubber` (lastik zemine basıp ezilir), `tip_ball` (top yuvarlanır), `tip_bearing`, `tip_sharp`, `tip_flat`
  - tek seferlik: `hit_light`, `hit_heavy`, `hit_wall`, `land`, `launch`, `evade`, `shield_hit`, `grind`, `special_clash`,
    `spirit`, `xtreme_dash`, `rail`
  - özel hareket arketipleri: `sp_awaken`, `sp_dive`, `sp_drill`, `sp_dash`, `sp_vortex`, `sp_guard`, `sp_steal`,
    `sp_levitate`, `sp_finisher`, `sp_upper` — her beyin 5 yeteneği bunlardan birine bağlıdır (`BeyAnims.ability`).
- Mesh'ler parçalara bölünür (bolt / ring / wheel / track / tip) ve her parça kendi kemiğinde çizilir;
  özel parçalı (workbench) beylerde track ve tip torna profilinden çizilir.

**Uçlar (Performance Tip)** — Metal serisinden 14 yeni uç: HF, WF, WSF, RS, RB, RDF, LRF, SD, DS, MS, MB, GB, Q, B:D
(toplam 31 uç). Her uç ailesinin kendi fiziği var (`TipPhysics`):

| Aile | Hareket | Dönüş kaybı | Not |
|---|---|---|---|
| Lastik (RF, R2F, RSF, LRF, RS, RB, RDF, CS) | çiçek deseninde en hızlı sürüş, zemine tutunur | yüksek | lastik dönüş azaldıkça aşınır; LRF sol dönüşte +%30; dönüşlerde duman |
| Düz (F, XF, HF, WF, MF) | çiçek deseni sürüş | orta-yüksek | MF/MS/MB metal: kayar, kıvılcım çıkarır |
| Yarı düz (SF, WSF, RSF) | kontrollü | orta | |
| Sivri (S, ES, CS, DS, MS, B:D) | merkezde durur | düşük | en dengeli |
| Top (B, WB, RB, MB, GB) | yavaş gezinir | orta | darbeleri emer, eğilmeyi toparlar |
| Savunma (D, WD, EWD, SD, DS, RDF) | yavaş | düşük | sahadan atılmaya dirençli |
| Serbest dönen (ES, EWD, FD, FS, B:D) | — | çok düşük | darbelerden daha az dönüş kaybeder |
| Q (Quake) | rastgele zıplar | yüksek | |

**Spin Track** — 9 yeni: 120, 130, 160, CH120, ED145, DF145, AD145, SW145, LW160.
Yeni parçaların hepsinin ikonu, tarifi, stadyum sandığı ganimeti ve TR/EN isimleri var.

## Yapı / Layout

- `common/` — iki sürümün ortak kodu ve varlıkları
- `forge-1.20.1/`, `forge-1.21.1/` — Gradle projeleri ve ince `compat` katmanı
  (GeckoLib 4.4 / 4.8 paketleri, item NBT ↔ data component, vertex API, ağ kanalı, eşzamanlı veri, blok/item imzaları)
- `tools/gen_geo.py` — GeckoLib iskeleti + tüm animasyonlar
- `tools/gen_parts.py` — yeni uç/track ikonları, modelleri, tarifleri, ganimet, dil
- `tools/gen_data121.py` — 1.21'in tekil veri klasörleri (`recipe`, `loot_table`)
- `tools/build_all.sh` — iki jar'ı `dist/` içine üretir

## Derleme / Building

```bash
tools/build_all.sh          # JDK 17 ve JDK 21 gerekir
```

Oyun içi otomatik test: `LIBRA_AUTOTEST=true gradle runClient` (dizilim, özel parçalar, çarpışma, özel hareketler,
burst ve sleep-out ekran görüntüleri `run/screenshots/` içine düşer).
