# Living Motion / Canlı Hareket — Forge 1.20.1 + GeckoLib 4

Client-side mod that remodels and reanimates vanilla mobs with **GeckoLib**, and adds procedural motion and combat
effects for every other living entity. Built jar: `../dist/living_motion-1.20.1-forge-1.0.0.jar`.

**Kurulum:** Forge 1.20.1 (47+) + GeckoLib 4.4+ (Forge, 1.20.1) + bu jar → `mods/`. Sadece istemcide gerekir;
sunucuya kurmak gerekmez.

## GeckoLib remodels (12 mobs)
zombie, husk, drowned, skeleton, stray, wither skeleton, creeper, spider, cave spider, cow, pig, chicken

- Models rebuilt 1:1 from the vanilla geometry, so vanilla textures and resource packs still fit.
- Animations: idle, walk, run/chase, attack (claw swipe, sword chop, spider pounce), hurt, death
  (fall over / spider flips and curls its legs), creeper fuse shake, bow drawing, chicken flapping, cow/pig grazing.
- Faces: blinking (random, per mob), squinting when hurt, angry slanted lids when aggressive or fusing, eyes
  closed when dead/asleep, mouths and jaws (zombie groans and chomps, skeleton jaw rattle, chewing, gasping),
  twitching spider fangs. Heads follow their look direction; skeleton arms aim the bow with the head.
- Keeps vanilla extras: held items, armour (dye, trims, glint), pumpkins/skulls on heads, stray clothing, drowned
  outer layer, charged-creeper swirl, glowing spider eyes, baby proportions, creeper swelling & white flash.

## Every other living entity (incl. modded)
Hit recoil, attack lunge, breathing and landing squash, plus particles: hit sparks and material bits
(blood / bone / slime / iron / portal …), hostile attack swipes, running dust, landing dust rings, death
soul/poof bursts, creeper fuse sparks, breath clouds in cold biomes.

## Config
`config/livingmotion-client.toml` — every remodel, faces, head look, motion strength and each effect can be switched
off; changes apply live (a disabled remodel falls back to the vanilla renderer).

## Building
```bash
python3 tools/gen_models.py   # regenerates geo/animation JSON
gradle build                  # JDK 17
LM_AUTOTEST=1 gradle runClient   # dev only: builds a test world and saves screenshots to run/screenshots
```
