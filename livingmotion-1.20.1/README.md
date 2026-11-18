# Living Motion / Canlı Hareket — Forge 1.20.1 + GeckoLib 4

Client-side mod that remodels and reanimates vanilla mobs with **GeckoLib**, and adds procedural motion and combat
effects for every other living entity. Built jar: `../dist/living_motion-1.20.1-forge-1.1.0.jar`.

**Kurulum:** Forge 1.20.1 (47+) + GeckoLib 4.4+ (Forge, 1.20.1) + bu jar → `mods/`. Sadece istemcide gerekir;
sunucuya kurmak gerekmez.

## GeckoLib remodels (17 mobs)
zombie, husk, drowned, skeleton, stray, wither skeleton, creeper, spider, cave spider, cow, pig, chicken,
sheep, iron golem, enderman, villager, wandering trader

- Extra detail: feathery chicken (comb, neck ruff, puffed chest, tail fan, wing primaries, toes), cow tail with tuft
  and ears, curly pig tail and floppy ears, bristly spider abdomen, extra-thick curly sheep fleece (dyed colours,
  jeb_ rainbow, hidden when sheared), vines on iron golems.
- Species behaviour: golem fist slam, cracks and flower offering; enderman split-open jaw and jitter when provoked,
  carried block; villager unibrow, wiggling nose, "no" head shake, nodding while trading, panicked running,
  biome/profession/level outfits; sheep graze when they really eat grass.

- Models rebuilt 1:1 from the vanilla geometry, so vanilla textures and resource packs still fit.
- Animations: idle, walk, run/chase, attack (claw swipe, sword chop, spider pounce), hurt, death
  (fall over / spider flips and curls its legs), creeper fuse shake, bow drawing, chicken flapping, cow/pig grazing.
- Faces: blinking (random, per mob), squinting when hurt, angry slanted lids when aggressive or fusing, eyes
  closed when dead/asleep, mouths and jaws (zombie groans and chomps, skeleton jaw rattle, chewing, gasping),
  twitching spider fangs. Heads follow their look direction; skeleton arms aim the bow with the head.
- Keeps vanilla extras: held items, armour (dye, trims, glint), pumpkins/skulls on heads, stray clothing, drowned
  outer layer, charged-creeper swirl, glowing spider eyes, baby proportions, creeper swelling & white flash.

## Every other living entity (incl. modded)
Gait roll and bob from the walk cycle, forward lean when running, hit recoil, attack lunge, breathing and landing
squash, plus particles: hit sparks and material bits
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
