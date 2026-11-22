# KAIJU (Minecraft Forge 1.20.1): model and animation stage

The model, its textures and its animations. There is no entity or gameplay code yet.

## Files (`common/src/main/resources/assets/kaiju/`)

| File | What it is |
|---|---|
| `geo/entity/kaiju.geo.json` | GeckoLib model: 307 bones, ~4300 cubes, 1024×1024 texture |
| `textures/entity/kaiju.png` | charcoal-black embossed keloid skin (4 texels per model pixel) |
| `textures/entity/kaiju_glowmask.png` | atomic charge: dorsal plates and mouth glow cyan |
| `textures/entity/kaiju_burning.png` + `_glowmask` | variant with orange magma cracks |
| `animations/entity/kaiju.animation.json` | `idle`, `walk`, `roar`, `atomic_breath`, `pose.roar` |

## Moving parts

- **Body:** `body` (hips), `torso`, `belly` and `chest` (these two scale for breathing)
- **Neck:** `neck1`, `neck2`, `neck3`, `throat`
- **Head:** `head`, `brow_l/r`, `eyelid_l/r` (blinking), `lip_upper` (snarling), `jaw`, `lip_lower`, `tongue1`, `tongue2`
- **Arms:** `shoulder_*`, `arm_*`, `forearm_*`, `hand_*`; four fingers with 3 joints each (`finger{0-3}_{1-3}_*`); a two-joint thumb (`thumb_{1,2}_*`)
- **Legs:** `thigh_*`, `shin_*`, `foot_*`; three toes with 2 joints each (`toe{0-2}_{1,2}_*`); `dewclaw_*`
- **Tail:** `tail1` to `tail16`
- **Dorsal plates:** 108 plates in three rows. Each plate has two bones:
  - `plate_mount_N` holds the fixed tilt.
  - `plate_N` is a slider that moves along the plate's own axis. In the animation, its `position.y` is how far the plate is pushed out.

## Atomic breath (`animation.kaiju.atomic_breath`, 7 s)

1. **0–0.6 s:** the creature braces and lowers itself.
2. **0.5–2.5 s:** the plates come out one by one, from the tail tip to the head, each with a small overshoot.
3. **2.4–3.3 s:** it inhales (the chest swells), rears back, and the mouth starts to glow.
4. **3.35–5.3 s:** it lunges forward and fires with its jaw wide open. The head shudders the whole time.
5. **5.3–6.2 s:** the plates slam back in, from the head to the tail.
6. **6.2–7.0 s:** it returns to rest.

The mod code can work out each plate's glow from that plate's slider offset (`offset / travel`), so the light follows the plates out and dies down as they go back in. The travel of every plate is listed in `tools/out/plates.json`.

## Rebuilding

```bash
cd kaiju/tools
python3 gen_kaiju.py      # model + textures + plates.json  -> out/
python3 gen_anims.py      # animations                       -> out/
python3 shots.py stills   # still images                     -> prev/
python3 shots.py strip    # atomic breath, key moments       -> prev/atomic_strip.png
python3 shots.py atomic   # atomic breath video              -> prev/atomic_breath.mp4
```

`preview.py` is an offline renderer that follows GeckoLib's rotation and bone order. It draws shadows, per-bone glow and bloom, and the beam.
