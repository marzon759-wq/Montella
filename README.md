# THE HATMAN: HOLLOW HOUSE / ŞAPKALI ADAM: OYUK EV

A story-driven analog horror mod for Minecraft **Forge 1.20.1** and **Forge 1.21.1** (requires GeckoLib 4).
English and Turkish.

You are a detective. Your partner, Ruth Calloway, walked into Marlowe House nineteen days ago and left you one voicemail:
*"Whatever he asks you — do not lie to him."*

## Playing / Oynanış

- `/hatman start` (or `/hatman basla`) — builds Marlowe House in front of you and begins the story. Singleplayer.
- `/hatman stop` (`bitir`), `/hatman status` (`durum`).
- **V** — Ruth's night-vision camcorder (rebindable). It sees in the dark and it sees *him*. It eats batteries; search drawers.
- Notes are never on the floor: they are on desks and tables, or inside drawers, cabinets and trunks.
- Wardrobes are hiding places. Get in **before** he sees you, and switch the camera **off** inside.
- When he asks you a question, how you answer matters: lies and insults make him angrier (and the house darker),
  agreeing with everything makes you *his*. There are five endings.

The game language decides the story language (English / Türkçe).

## Install / Kurulum

1. Install Forge for your Minecraft version and **GeckoLib** (Forge build) for the same version.
2. Drop the matching jar from `dist/` into `mods/`:
   - `hatman_hollow_house-1.20.1-forge-1.1.0.jar` — Minecraft 1.20.1, Forge 47+, GeckoLib 4.4+
   - `hatman_hollow_house-1.21.1-forge-1.1.0.jar` — Minecraft 1.21.1, Forge 52+, GeckoLib 4.7+
3. Create a world (flat or normal), stand somewhere open and type `/hatman start` (`/hatman basla`).

## Layout

- `common/` — all gameplay code and assets, shared by both versions
- `forge-1.20.1/`, `forge-1.21.1/` — Gradle projects with a thin version-specific `compat` layer
- `tools/` — generators for models, animations, textures, sounds and language files
- `dist/` — built jars

## Building

```bash
cd forge-1.20.1 && gradle build     # needs JDK 17
cd forge-1.21.1 && gradle build     # needs JDK 21
```

See [CREDITS.md](CREDITS.md) for sound sources (all CC0).

## Living Motion (separate mod)

`livingmotion-1.20.1/` holds a second, independent client-side mod for Forge 1.20.1 + GeckoLib: animated GeckoLib
remodels with facial expressions for 17 vanilla mobs, plus procedural motion and combat effects for all other
mobs. Jar: `dist/living_motion-1.20.1-forge-1.1.0.jar`. See [livingmotion-1.20.1/README.md](livingmotion-1.20.1/README.md).
