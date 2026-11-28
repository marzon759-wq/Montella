# CurseForge yükleme rehberi — Beyblade Metal Fight 2.3.0

Proje zaten CurseForge'da varsa **A** bölümüyle yeni sürümü yükle, **B** bölümüyle sayfayı güncelle.
Metinler İngilizce; olduğu gibi kopyala-yapıştır.

---

## A. Yeni dosya yükleme (Upload File)

**authors.curseforge.com → projen → Files → Upload File.** İki jar'ı **ayrı ayrı** yükle (her biri kendi sürümüyle).

### Dosya 1: 1.20.1

| Alan | Değer |
|---|---|
| File | `mods/beyblade_mod-1.20.1-forge-2.3.0.jar` |
| Display name | `Beyblade Metal Fight 2.3.0 (Forge 1.20.1)` |
| Release type | **Release** |
| Game versions | **Minecraft 1.20.1**, mod loader **Forge** |
| Java | Java 17 |
| Changelog | `changelog_2.3.0.md` içeriği (editörde Markdown seç) |
| Related projects | **GeckoLib → Required Dependency** |

### Dosya 2: 1.21.1

| Alan | Değer |
|---|---|
| File | `mods/beyblade_mod-1.21.1-forge-2.3.0.jar` |
| Display name | `Beyblade Metal Fight 2.3.0 (Forge 1.21.1)` |
| Release type | **Release** |
| Game versions | **Minecraft 1.21.1**, mod loader **Forge** |
| Java | Java 21 |
| Changelog | `changelog_2.3.0.md` içeriği |
| Related projects | **GeckoLib → Required Dependency** |

> GeckoLib'i "Required Dependency" olarak eklemek önemli: CurseForge uygulaması o zaman GeckoLib'i otomatik kurar.

---

## B. Proje sayfasını güncelleme

### Summary (kısa özet)

```
Metal Fight beyblades, animated with GeckoLib: build your bey, rip it, unleash special moves that tear up the world.
```

### Description (uzun açıklama)

1. **Images** sekmesine `banner_1920x1080.jpg` ve `screenshots/` içindeki görselleri yükle.
2. **Description → Edit**, editörde **Markdown**'ı seç, `description.md` içeriğini yapıştır.
3. En üstteki `BANNER_LINK` yerine yüklediğin banner'ın bağlantısını koy (ya da o satırı silip editörün resim
   düğmesiyle banner'ı en üste ekle).

### Logo

`logo_400x400.png` (daha büyük gerekirse `logo_800x800.png`).

### Kategoriler

- Main: **Adventure and RPG** (ya da **Mobs**)
- Additional: **Mobs**, **Equipment**

### Gallery (Images)

`screenshots/` klasöründeki 1920×1080 görsellerin hepsini yükle; başlıklar dosya adlarında yazıyor.

### Video

`beyblade_2.2_showcase.mp4` CurseForge'a doğrudan yüklenmez. YouTube'a yükleyip bağlantısını açıklamaya
ekleyebilirsin: Markdown'da `[Watch the trailer](YOUTUBE_LINK)`.

---

## Not: isim ve marka

"Beyblade" ve bey adları Takara Tomy / Hasbro'nun markaları. Açıklamanın sonundaki *fan-made, not affiliated*
notunu silme; CurseForge hayran modlarında bunu bekler.
