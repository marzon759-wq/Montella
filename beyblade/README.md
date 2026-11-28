# Beyblade Metal Fight (librabeyblade) — 2.0.0

Forge **1.20.1** and **1.21.1**, now animated with **GeckoLib 4**.

## Kurulum / Install

1. Forge + **GeckoLib** (Forge sürümü, aynı Minecraft sürümü için) kur.
2. `dist/` içinden doğru jar'ı `mods/` klasörüne at:
   - `beyblade_mod-1.20.1-forge-2.0.0.jar` — Minecraft 1.20.1, Forge 47+, GeckoLib 4.4+
   - `beyblade_mod-1.21.1-forge-2.0.0.jar` — Minecraft 1.21.1, Forge 52+, GeckoLib 4.7+

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
