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

Build: `python3 tools/gen_assets.py`, then `gradle build` in `forge-1.20.1` and `forge-1.21.1`.
