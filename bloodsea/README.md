# Blood Sea

A horror dimension for Minecraft Forge **1.20.1** and **1.21.1**: an endless ocean of blood under a white sun and a blood moon.

`/bloodsea enter` takes you to a raft in the middle of the sea (with a boat), `/bloodsea leave` brings you home,
`/bloodsea where` names the region you are in.

## The sea
- **Blood** is a real fluid: thick and slow, boats float on it, you can swim and drown in it, and under the surface you
  see two blocks of red murk.
- **The Pale Sun** (day): it sees everything. Out in the open it dries you out (hunger drains). Look straight at it and it
  looks back: blindness and weakness. In its light the Mirage's lies show - fake land shimmers.
- **The Blood Moon** (night): the sea is hungry. Swim in the blood and it heals you, and makes you hungrier. The skulls of
  the Bone Reach open red eyes. Whispers on the water.

## The regions (from the raft at 0,0)
| Direction | Region | What is there | How it ends |
|---|---|---|---|
| middle | **The Drift** | calm blood in every direction, the raft | - |
| north | **The Bone Reach** | islands and floating mats of human bones, skull piles, rib cages arching out of the sea | **The Ossuary Throne** at 0, -1450: touch the heart on the throne |
| east | **Leviathan's Deep** | a trench as deep as the dark, the skeleton of a dead leviathan with ribs taller than the sky | **The Maw** at 1450, 0: a ring of fangs; sail into the black throat |
| south | **The Edge of the World** | white haze, black spires, and at 1900 blocks the sea pours over the edge into nothing | **Beyond the Edge**: go over the waterfall |
| west | **The Mirage** | pink haze, islands that are not there (you fall through), walls you cannot see | **Home** at -1450, 0: the lighthouse door |

Each ending shows its own epilogue and sends you back to the overworld. The creatures come next.

## The dead
No bones are cubes. Human remains are sculpted shapes (tools/gen_props.py): skulls with sockets, nose and teeth (upright,
fallen, half sunk in the blood, heaped), rib cages, whole skeletons lying where they fell, ribs breaking out of the mud,
a hand reaching up out of the blood, bone piles and spines. Skulls open red eyes under the Blood Moon. Bone islands are
wet black mud with the dead lying on them.

The giant dead are GeckoLib models (tools/gen_giants.py) placed by the world generator: the leviathan (a tail, segments
of curved rib arches, a long toothed skull), the Maw (two rings of curved fangs over a gum ridge), and giant human rib
cages and skulls half sunk in the Bone Reach. They are solid: the generator lays invisible blocks along their bones
(from the collision cells tools/gen_giants.py writes), so boats stop against ribs and fangs and you can climb a skull.
Run the client with BLOODSEA_SHOWCOLL=1 to see those blocks as glass.

## The story: `/bloodsea start`
You wake face down on a raft in a sea of blood, with a black cat you have never seen and a hand-drawn map. Who are you,
where is this, and why are you here? Everything outside the cutscenes is played in first person.

- **The raft** is the whole world: sit at the steering oar (use it) and row with the movement keys. Stand on it when it is
  still; it will not let you step off by accident. **Touching the blood kills you.**
- **Survival**: you get hungry. Food comes from floating crates of cargo (row into them or use them) and from Pip, who
  hooks fish out of the blood and brings them to you - more often the more you care for her. Stroke her (use, empty
  hand), feed her, and she purrs, rubs against you, sleeps on the blanket, hisses at what is in the water.
- **18 cutscenes** with subtitled dialogue (English and Turkish, no spoken voice), and **timed choices**. Every choice weighs
  hope, acceptance, courage or fury; what weighs most decides where the voyage ends:
  - *The Stomach* (the lighthouse): a hospital bed, Pip alive in your lap... and what is really there.
  - *Acceptance* (the throne of bones), *Return* (through the Leviathan), *The Fall* (the edge of the sea).
- **The Watcher**: the sun is only a sun - until the story opens its eye, and after that it opens at the worst moments.
- The map (use it) shows where you are and where you are pointing.

## The living
Five creatures and the sky (tools/gen_creatures.py sculpts them as distance fields, one per bone, and paints every
visible voxel face into its own texels; tools/gen_creature_anims.py animates them; tools/gen_creature_sounds.py voices them).
None of them can be killed.

| Creature | Where | What it does |
|---|---|---|
| Wake Follower | open blood, Blood Moon | swims 20 blocks behind you; stop rowing and it comes in and lunges |
| Bone Gatherer | Bone Reach | assembles out of the blood, comes slowly while watched and fast while not; every skull turns to you |
| Name Caller | Mirage fog, sometimes the Blood Moon | blind, hunts by sound (running, rowing, jumping; crouch to be silent), whispers |
| The Changer | among the barrels on the raft | a barrel while you look; opens its eye and creeps closer when you don't; bites |
| Leviathan | the eastern deep | heartbeat under the sea; rises in front of you, roars, then bites whatever is still in reach |
| The Watcher | the sky by day | the Pale Sun is an eye: it looks about and blinks; stare and it stares back, and every creature learns where you are |

Commands (op): `/bloodsea summon <wake|gatherer|caller|changer|leviathan>`, `/bloodsea debug freeze <bool>`, `/bloodsea debug state <n> [action]`.

Build: `python3 tools/gen_creatures.py && python3 tools/gen_creature_anims.py && python3 tools/gen_assets.py && python3 tools/gen_props.py && python3 tools/gen_giants.py`, then `gradle build` in `forge-1.20.1` and `forge-1.21.1`.

## Sound credits
Pip's meows, chirps, purrs, hisses and cries are cut from real cat recordings (tools/gen_cat_sounds.py):
- "Cat purr & meow" by kerzoven (freesound.org), CC0
- "Meow" by ignasd (freesound.org), CC0
- "Cat SFX" by macro (freesound.org), CC-BY 3.0 - https://creativecommons.org/licenses/by/3.0/
