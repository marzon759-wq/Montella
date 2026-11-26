# CurseForge'a yükleme rehberi (Blood Sea 1.0.0)

## 1. Projeyi oluştur
CurseForge'da: **Create Project → Minecraft → Mods**

| Alan | Ne yazılacak |
|---|---|
| Project name | Blood Sea |
| Summary | Wake on a raft in an endless sea of blood. Story-driven horror survival: cutscenes, choices, four endings. |
| Logo / avatar | `release_icon_400.png` (400×400) |
| Description | `curseforge_description.md` dosyasının tamamı (editörde Markdown seçeneğini kullan) |
| Main category | Adventure and RPG |
| Diğer kategoriler | Mobs, Dimensions |
| License | All Rights Reserved (mod dosyasındaki lisans bu; istersen değiştir) |

## 2. Dosyaları yükle
Her sürüm için **ayrı dosya** yükle.

**bloodsea-1.20.1-forge-1.0.0.jar**
- Release type: Release
- Game version: 1.20.1
- Mod loader: Forge
- Java: 17
- Changelog: `changelog_1.0.0.md`
- Relations → **GeckoLib → Required Dependency**

**bloodsea-1.21.1-forge-1.0.0.jar**
- Release type: Release
- Game version: 1.21.1
- Mod loader: Forge
- Java: 21
- Changelog: `changelog_1.0.0.md`
- Relations → **GeckoLib → Required Dependency**

## 3. Galeri
`gallery` klasöründeki 12 görseli **Images** sekmesinden yükle. Önerilen başlıklar dosya adlarında yazıyor. İlk görsel `01_title_screen.png` olsun.

## 4. Gönder ve onayı bekle
CurseForge her yeni projeyi ve dosyayı elle inceler. Onay genelde birkaç saat ile birkaç gün sürer.

## Bilmen gerekenler
- **Uygulama bağlantısı:** Ana menüdeki uygulama bağlantısını açıklamada açıkça belirttim. CurseForge'un incelemesinde mod içi tanıtım bağlantılarına soru gelebilir. Ret gelirse en kolay çözüm, bağlantıyı açıklamada bırakıp menüden kaldırmak. Bunu tek satırla yapabilirim.
- **Kedi sesleri:** Kedi seslerinden biri CC-BY 3.0 lisanslı. Açıklamadaki "Credits" bölümünü silme; bu lisans isim belirtmeyi zorunlu kılıyor.
- **Sürüm güncelleme:** Bir sonraki sürümde `gradle.properties` içindeki `mod_version` değerini artır, örneğin 1.0.1.
