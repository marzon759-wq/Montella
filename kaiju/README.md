# KAIJU (Minecraft Forge 1.20.1) — model aşaması

Bu klasör kaiju modunun **model** aşamasını içerir (henüz entity/oynanış kodu yok).

- `common/src/main/resources/assets/kaiju/geo/entity/kaiju.geo.json` — GeckoLib modeli (125 kemik, ~2700 küp)
- `.../textures/entity/kaiju.png` — kömür siyahı, pullu deri (512×512 piksel-art)
- `.../textures/entity/kaiju_glowmask.png` — atom yüklemesi: sırt plakaları ve ağız camgöbeği parlar
- `.../textures/entity/kaiju_burning.png` + `kaiju_burning_glowmask.png` — turuncu magma çatlaklı varyant
- `.../animations/entity/kaiju.animation.json` — önizleme pozu (`pose.roar`)
- `previews/` — render görüntüleri

Kemikler: `root, body, torso, neck, head, jaw, arm_*/forearm_*/hand_*/finger*_*/thumb_*, thigh_*/shin_*/foot_*,
tail1..tail12, plate1..N` (her sırt plakası ayrı kemik: ileride tek tek parlatılabilir/oynatılabilir).
Ölçü: 16 px = 1 blok, model ~6 blok boyunda; renderer'da ölçeklenecek.

Yeniden üretmek için:

```bash
cd kaiju/tools
python3 gen_kaiju.py      # out/ altına model + dokular
python3 shots.py          # prev/ altına önizlemeler
```
