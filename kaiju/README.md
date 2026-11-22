# KAIJU: Atomic (Minecraft Forge 1.20.1, GeckoLib 4)

A film-scale atomic kaiju (about 50 blocks tall) with physics-driven combat, 87 attacks, a world it can tear apart, and
an atomic breath whose dorsal plates slide out one by one from the tail to the head.

## Play

- **Atomic Kaiju Egg** (Spawn Eggs tab): use it on the ground to hatch a kaiju. You own it.
- **Kaiju Controller** (Tools tab): right-click to take control of the nearest kaiju you own; right-click again to let go.
  In creative mode you can take any kaiju.
- Kaiju fight each other by themselves. Hatch two, or use `/kaiju fight`.

### Controls while riding

| Key | Action |
|---|---|
| WASD, mouse | walk relative to the camera; Ctrl = walk faster |
| Left mouse | light attack (picks the best claw / bite / kick for the range and side) |
| Right mouse | heavy attack (tail, body, grapple) |
| R (hold) | atomic breath. Hold to keep firing; aim with the crosshair. Ctrl+R sweeps, Shift+R full charge, looking up fires into the sky, an enemy on the ground gets a downward breath |
| G | grab: an enemy close by (grab and throw, Shift = lift and slam), otherwise a tree; Ctrl+G tears up the ground. Holding something: throw it (swing it if the enemy is close) |
| V | tail attack, by the enemy's side (left/right whip, behind: slam or stab, in front: 360 spin) |
| C | bite / grapple bite |
| Space | stomp (Ctrl+Space: ground pound) |
| Shift | guard |
| Left Alt | dodge (A/D sidestep, otherwise step back) |
| N | nuclear pulse · B roar |
| Z | menu of all attacks; clicking one performs it and assigns it to Y |
| H | AI assist (it fights by itself, you ride along) |
| F7 | show the server's collision spheres |

On the ground: left mouse kicks, right mouse sweeps the tail, R fires the breath from the ground, Alt rolls away, Space or W gets up.

## Commands (`/kaiju`, op)

`fight [distance]`, `attack <id>` (tab completion lists every attack), `list`, `control`, `release`, `scale <v>`,
`ai <true|false>`, `griefing <true|false>`, `sever <arm_l|arm_r|tail_tip>`, `burn`, `clear`.

Config: `config/kaiju-common.toml` sets griefing, flying debris, the block budget per tick, the default size, and
whether kaiju hatched by the same player fight each other.

## How it works

**The server plays the same animation as the client.** `tools/gen_anims.py` writes the GeckoLib animations and,
alongside them, a *pose track* for every clip: the position of 39 collision spheres (head, jaw, neck, chest, arms,
legs, 16 tail segments...) at every game tick. The server reads the track for the clip being played, so the hit
spheres follow the visible body exactly.

**Hits are momentum.** The weapon sphere's path during the tick is checked against the victim's spheres as a swept
sphere, so even a tail tip moving 10 blocks a tick cannot pass through a body.
- Impulse: `J = (1+e) * μ * v_closing`.
- Change in the victim's velocity: `Δv = η J / M`.
- Δv decides flinch, stagger or knockdown, together with poise.
- Damage grows with the momentum delivered and the type of weapon (blunt, cut, bite, crush, stomp).
- Both bodies freeze for a few frames (hit-stop).

Bodies push each other out with mass-weighted corrections and lose their closing speed, so tails and arms never pass
through each other.

**Animation physics.** The tail is simulated as a chain of damped springs: when the body spins, the tail lags,
whips and overshoots. The neck and arms have their own follow-through. Impacts jiggle the belly and throat.

**Atomic breath.** In the charge clip, every plate's slider bone comes out in order from the tail to the head, and the
client lights each plate in proportion to how far it is out. The beam starts from the mouth sphere and aims where the
head points, within a cone the neck can reach.
- It digs slowly through terrain, vaporises it, and leaves magma and fire behind.
- At the point of impact it leaves a nuclear explosion and a mushroom cloud.
- Where it hits a kaiju, that body region's heat rises and the skin cracks orange.

Two beams that touch lock into a *clash*. The meeting point slides toward the weaker beam, and the overpowered
kaiju's own face takes the blast.

**Severed limbs.** The arms and the tail tip have their own health. When it runs out from cut, bite or beam damage
the part comes off: a severed-part entity falls and twitches. After 7 seconds the stump grows back like the film:
lumpy, raw and pulsing flesh, over 30 seconds. Attacks that need that part are disabled until then.

**The world.**
- Moving body spheres plough through what they touch, and feet leave footprints.
- Falls crush houses.
- Shockwaves throw rubble.
- Trees are pulled out by the roots, and chunks of houses or slabs of ground are torn off. Each becomes one entity
  that can be carried, swung or thrown; on impact it scatters into real falling blocks.
- Everything respects mobGriefing and the per-tick block budget.

**AI.** It scores every legal attack by distance, which side the enemy is on, and whether either kaiju is down,
staggered, grabbed, charging or firing. It also weighs its own health and its success so far, then picks with some
randomness. It knows how to:
- answer a charging breath with its own (clash),
- stand over a fallen enemy and stomp or fire,
- fight back from the ground,
- throw a tree from a distance,
- break free of a grapple with the nuclear pulse.

## Adding another kaiju

1. Add its model, textures and animations to `assets/kaiju/...`. Keep the bone names used by the nodes (or write your own node list).
2. In `tools/`, write a moves file and a NODES list like the existing ones. Running `gen_anims.py` generates
   `data/kaiju/kaiju/<id>.json` (spec, attacks, pose tracks).
3. Java: a three-line subclass, like `AtomicKaijuEntity`, which calls `super(type, level, "<id>")`. Register its
   entity type and renderer (`KaijuMod`, `KaijuClient`).

The combat, physics, AI, beam clash, severing and world damage are shared, so the two kaiju fight each other
automatically.

## Building

```bash
cd kaiju/tools && python3 gen_kaiju.py && python3 gen_anims.py && python3 gen_assets.py
cd ../forge-1.20.1 && gradle build      # JDK 17 -> build/libs/kaiju-1.20.1-forge-*.jar
```

Requires GeckoLib 4.4+ (Forge 1.20.1).
