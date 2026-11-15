# Kaiju Titans — Godzilla vs King Ghidorah (Forge 1.20.1)

Hayran yapımı (fan-made) mod. Gereksinim: Minecraft 1.20.1 + Forge 47+. Başka bağımlılık yok.

## Kurulum
`dist/kaiju_titans-1.20.1-forge-1.0.0.jar` dosyasını `.minecraft/mods/` klasörüne at.

## Kullanım
- `/kaiju fight` — önüne Godzilla ve King Ghidorah'ı karşı karşıya çağırır, hemen dövüşmeye başlarlar.
- `/kaiju godzilla`, `/kaiju ghidorah` — tek tek çağırma; `/kaiju clear` — hepsini siler.
- Yaratıcı moddaki çağırma yumurtaları da var.
- Blok yıkımı `mobGriefing` kuralına bağlı (`/gamerule mobGriefing false` ile kapat).

## Neler var
- Kemik iskeletine giydirilmiş organik modeller (blok değil): bükülen boyun/kuyruk, kas kütleleri,
  akçaağaç yaprağı biçimli 3 sıra sırt plakası, boynuzlar, dişler, kanat zarları; prosedürel pul dokuları.
- Boylar: Godzilla ~24 blok, Ghidorah ~27 blok (biraz daha uzun).
- Atomik nefes: plakalar kuyruktan başa dalga halinde yanar, ışın sarmal enerjiyle çıkar, durunca yavaşça söner.
- Ghidorah'ın 3 kafasından yerçekimi şimşekleri; ışınlar havada çarpışınca güç savaşı olur,
  güçlü taraf çarpışma noktasını zayıfın yüzüne iter ve onu geriye savurur.
- Fizik: ağır kütle/eylemsizlik, darbe gücüne göre sendeleme / devrilme / havaya savrulma,
  gerçek açısal ivmeyle yere düşme ve sekme, kollarla destek alıp kalkma, kamera sarsıntısı, krater.
- Kuyruk savurma: ters yöne eğilip dönerek vurur, kuyruk vurduğu bedende durur (içinden geçmez),
  güç kuyruk ucunun hızından hesaplanır, yoldaki blokları yıkar.
- Isırma → tutma → sürükleme → fırlatma; Godzilla Ghidorah'ın bir kafasını koparabilir,
  kafa düşer ve boyun güdüğünden animasyonla yeniden büyür.
- Ghidorah yere düşen Godzilla'nın üstüne çıkar ve ısırır; Godzilla atomik darbe (nuclear pulse) ile karşılık verir.
- Ghidorah Godzilla'yı ısırıp havaya kaldırabilir ve bırakır; uçuş, kanat rüzgârı, kükremeler, koşarak çarpma.

## Derleme
```bash
cd kaiju-1.20.1 && python3 tools/gen_textures.py && gradle build   # JDK 17
```
Geliştirme testi: `KAIJU_AUTOTEST=model KAIJU_FREEZE=true gradle runClient` veya `KAIJU_AUTOTEST=fight gradle runClient`
(ekran görüntüleri `run/screenshots/` altına).

Godzilla ve King Ghidorah, Toho Co., Ltd.'nin ticari markalarıdır; bu resmi olmayan, ticari amaç taşımayan bir hayran modudur.
