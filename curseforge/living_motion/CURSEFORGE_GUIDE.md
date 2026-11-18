# CurseForge yayın rehberi — Living Motion

**Nereden:** https://authors.curseforge.com → **Create Project** → Game: **Minecraft** → Class: **Mods**.
Aşağıda her alanın adı (sitede göründüğü gibi, İngilizce), oraya ne yazacağın ve hangi dosyayı yükleyeceğin sırayla var.
Çerçeve içindeki metinleri olduğu gibi kopyala. Bütün görseller bu klasörde: `curseforge/living_motion/`.

---

## 1. Project name

```
Living Motion
```

> Bu ad CurseForge'da alınmışsa yedek: `Living Motion: Animated Mobs` veya `Living Motion - GeckoLib Mob Animations`

## 2. Summary (arama sonuçlarında görünen kısa özet, en fazla ~250 karakter)

```
Vanilla mobs remade with GeckoLib: new animations for walking, chasing, attacking, getting hurt and dying, faces that blink and scowl, and hit effects for every creature.
```

## 3. Project avatar / Logo (kare)

Yükle: **`logo_400x400.png`** (büyük istenirse `logo_800x800.png` veya `logo_1024x1024.png`)

## 4. Categories

- **Main category:** `Mobs`
- **Additional categories:** `Cosmetic`, `Utility & QoL`

## 5. License

`All Rights Reserved` (ya da başkalarının kodu kullanmasına izin vermek istersen `MIT`).
"Allow this project to be included in modpacks" / modpack izni açık kalsın — mod paketlerine girmesi indirmeyi artırır.

## 6. Links (isteğe bağlı)

- **Source:** GitHub repo bağlantın (repo herkese açıksa)
- **Issues:** GitHub Issues veya Discord bağlantın

---

## 7. Description (uzun açıklama)

1. Önce **Images** sekmesine (proje oluşturulduktan sonra sol menüde) şu dosyaları yükle:
   `cover_1920x1080.jpg`, `09_faces.jpg`, `01_hostile_mobs.jpg`, `03_golem_throw.jpg`, `06_feathery_chicken.jpg`, `08_villager_sheep.jpg`, `05_zombie_chase.jpg`
2. Her görselin bağlantısını kopyala ve aşağıdaki metinde `LINK_COVER`, `LINK_FACES` … yerlerine yapıştır.
   (Ya da editörün "Insert image" düğmesiyle görseli ilgili satıra ekle.)
3. Açıklama editöründe **Markdown** biçimini seç ve çerçevenin içini yapıştır.

```markdown
![Living Motion](LINK_COVER)

# Living Motion

**Living Motion** rebuilds vanilla mobs with **GeckoLib** and brings them to life: every remodelled mob has its own idle, walk, run, attack, hurt and death animations. Their faces move too. Everything else that lives in your world reacts to hits with recoil and impact effects.

It is **client-side only**: drop it into your own `mods` folder and it works on any server, including vanilla servers. The models use the **vanilla textures**, so it also works with resource packs that keep the vanilla layout.

---

## 🧟 17 remodelled mobs

**Zombie · Husk · Drowned · Skeleton · Stray · Wither Skeleton · Creeper · Spider · Cave Spider · Enderman · Iron Golem · Villager · Wandering Trader · Cow · Pig · Sheep · Chicken**

Each of them has its own set of moves:

- **Zombies** shuffle with arms out, sway and groan. When they chase you they lean in and chomp, and they attack with a two-handed claw swipe.
- **Skeletons** rattle their jaw, draw the bow with their arms following their aim, and chop with swords.
- **Creepers** tremble, crouch and drop their jaw while the fuse burns, with sparks and smoke. They still swell and flash like vanilla.
- **Spiders** walk with an alternating eight-leg gait, rear up and pounce. They have twitching fangs, a bristly abdomen and glowing eyes, and they flip over and curl their legs when they die.
- **Endermen** walk with long gliding strides and sprint when angry. When provoked, the head **splits open** and jitters. They carry blocks in their arms.
- **Iron golems** stomp heavily, raise both fists and **slam** down. They crack as they take damage and offer flowers.
- **Villagers** walk with crossed arms and panic-run with flailing arms. They nod while trading and shake their head when they say "no". Biome, profession and level outfits are all kept.
- **Cows, pigs and sheep** walk, run, graze and chew. Tails swish and ears twitch.
- **Sheep** have extra-thick, curly fleece in every dye colour (jeb_ works too), and it disappears when sheared.
- **Chickens** are much fluffier: comb, neck ruff, puffed chest, tail fan, wing feathers and toes. They flap when they fall.

## 😠 Faces that move

![Faces](LINK_FACES)

Eyes **blink** (each mob on its own rhythm), **squint** when hurt, get **angry slanted lids** when hostile, and **close** in death. Mouths and jaws **open** to groan, snap, gasp and chew. Villager and golem brows move, and villager noses wiggle.

## ⚔️ Combat and effects for every creature

These work on **all** living entities, including mobs from other mods:

- Hit **recoil**, attack **lunge**, landing **squash** and breathing
- Body **roll and bob** while walking, forward **lean** while running
- **Impact bits** that match the creature: blood, bone, slime, iron, ender particles…
- **Attack swipes** from hostile mobs, **dust** kicked up by running mobs, a **dust ring** on hard landings
- **Soul / smoke bursts** on death, creeper **fuse sparks**, **breath clouds** in cold biomes

![Hostile mobs](LINK_HOSTILE)
![Iron golem](LINK_GOLEM)
![Chicken](LINK_CHICKEN)
![Villager and sheep](LINK_VILLAGER)
![Zombie chase](LINK_CHASE)

## 🛡️ Vanilla details are kept

Held items, armour (dyes, trims and enchantment glint), pumpkins and mob heads, stray clothing, the drowned outer layer, the charged creeper aura, baby proportions, leads and name tags all still work.

## ⚙️ Configuration

`config/livingmotion-client.toml`. **Every switch applies instantly, no restart needed:**

- turn each remodel on or off (a disabled mob goes back to its vanilla model)
- faces, head-look, motion strength
- every particle effect separately, including a **"blood" switch** that keeps only the sparks

## 📦 Requirements

- **Minecraft 1.20.1**
- **Forge 47** or newer
- **[GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) 4.4+** (Forge, 1.20.1)

Only needed on the **client**. Servers do not need it.

## ❓ FAQ

**Does it work on servers?** Yes. Install it only on your client; the server needs nothing.

**Does it work with resource packs?** Yes, as long as the pack keeps the vanilla texture layout for these mobs.

**Modded mobs?** They get the motion and combat effects. The full remodels are for the vanilla mobs listed above.

**Can I use it in a modpack?** Yes!
```

---

## 8. Images / Gallery (proje sayfasındaki "Images" sekmesi)

Hepsini yükle; başlık (Title) ve açıklama (Description) alanlarına şunları yaz.
**Featured** olarak `cover_1920x1080.jpg`'yi işaretle.

| Dosya | Title | Description |
|---|---|---|
| `cover_1920x1080.jpg` | Living Motion | 17 vanilla mobs remade and reanimated with GeckoLib |
| `09_faces.jpg` | Faces that move | Blinking, angry brows, groaning and talking mouths |
| `01_hostile_mobs.jpg` | Hostile mobs | Zombies, skeletons, creepers and spiders, all remodelled |
| `02_friendly_mobs.jpg` | Village and farm | Villagers, golems, farm animals and the enderman |
| `03_golem_throw.jpg` | Iron golem fight | Heavy fist slams that send zombies flying |
| `04_golem_cracks.jpg` | Two-fisted slam | Cracks, impact bits and a ground-shaking smash |
| `05_zombie_chase.jpg` | Chasing zombies | They lean in, reach out and chomp |
| `06_feathery_chicken.jpg` | Fluffier chickens | Comb, ruff, tail fan, wing feathers and toes |
| `07_spiders.jpg` | Spiders | Bristles, twitching fangs and glowing eyes |
| `08_villager_sheep.jpg` | Villagers and sheep | Biome and profession outfits, thick dyed fleece |
| `10_death.jpg` | Deaths | Each remodelled mob collapses its own way |

---

## 9. Dosya yükleme (Files → Upload File)

- **File:** `dist/living_motion-1.20.1-forge-1.1.0.jar` (proje kök klasöründe, ya da gönderdiğim zip'in içinde)
- **Display name:** `Living Motion 1.1.0 (Forge 1.20.1)`
- **Release type:** `Release` (ilk sürümde temkinli olmak istersen `Beta`)
- **Game versions:** `1.20.1` + mod loader `Forge`
- **Environment:** `Client` (varsa "Client" seç; sunucuya gerek yok)
- **Java version:** `Java 17`
- **Related projects / Dependencies:** `GeckoLib` → **Required Dependency**
- **Changelog** (Markdown):

```markdown
**Living Motion 1.1.0**

- 17 vanilla mobs remodelled and reanimated with GeckoLib, each with its own idle, walk, run, attack, hurt and death animations
- Moving faces: blinking, squinting, angry brows, talking and groaning mouths
- New: sheep (thick dyed fleece), iron golem, enderman, villager and wandering trader
- Fluffier chickens; tails and ears for cows, pigs and sheep; bristly spiders
- Fixed: zombies and husks were missing their left leg and arm
- Motion and combat effects (recoil, lunge, gait, impact bits, dust, death bursts) for every other creature, including modded ones
- Live client config for every feature
```

---

## 10. Gönderme (Submit)

Kaydettikten sonra proje **"Under Review"** durumuna geçer. CurseForge ekibi genelde birkaç saat ile birkaç gün içinde onaylar.
Onaydan sonra sayfa herkese açılır. Sonraki sürümlerde sadece **Files → Upload File** ile yeni jar'ı ve changelog'u eklemen yeterli.

### Kontrol listesi

- [ ] Proje adı + özet
- [ ] Logo (`logo_400x400.png`)
- [ ] Kategoriler: Mobs, Cosmetic, Utility & QoL
- [ ] Açıklama (Markdown, görsel bağlantıları eklenmiş)
- [ ] Galeri görselleri (kapak "Featured")
- [ ] Jar + 1.20.1 + Forge + Client + GeckoLib "Required Dependency"
- [ ] Changelog
