# Kaiju (Forge 1.20.1), model stage

A huge atomic kaiju for Minecraft **Forge 1.20.1**. It doesn't need GeckoLib. The model uses the game's own
`ModelPart` / `HierarchicalModel` classes.

This stage covers only the **model**: the creature walks, breathes, sways its tail and turns its head, but it does not attack yet.

## In game

- Find the **Kaiju Spawn Egg** in the creative *Spawn Eggs* tab.
- Right-click the kaiju with an **empty hand** to switch its look:
  - **Atomic**: dark keloid scales and dorsal fins that glow blue.
  - **Burning**: molten red-orange fissures across the chest, neck, face and limbs.
- It stands about 18 blocks tall. You can change the size with `KaijuEntity.SCALE`.

## How the model is built

`tools/gen_kaiju.py` generates the whole model: 258 parts, about 2,200 box-UV cubes and a 1024×512 texture atlas.
- Body: a pear-shaped torso leaning forward, with horizontal belly plates, very large thighs, columnar shins and elephant-like feet with 3 toes, a heel claw and a claw on each toe.
- Arms: short and muscular, held forward. Each hand has 5 fingers with long claws.
- Head: a heavy brow ridge, small deep-set eyes, a short muzzle, nostrils, upper and lower rows of teeth, a palate and tongue, and an opening jaw.
- Dorsal fins: three rows. Each fin is a fan of thick crystal spikes. The fins are biggest over the shoulders and shrink toward the neck and the tip of the tail.
- Tail: 12 chained segments with underside scales. It drops toward the ground and then lifts at the tip.
- Skin: small raised blocks scattered over the body, like keloid scales. The procedural texture is painted in 3D model space, so the patterns run across part boundaries without seams.

The geometry is written to `assets/kaiju/models/entity/kaiju.json`.
`KaijuModel.createBodyLayer()` reads it and builds the same `MeshDefinition` that a hand-written
`CubeListBuilder.addBox(...)` call chain would produce. The glow is a full-bright `RenderType.eyes` layer.

```bash
cd tools && ./install.sh                 # regenerate model + textures into the mod
python3 sheet.py atomic prev/sheet.png   # offline preview (same ModelPart rules)
cd ../forge-1.20.1 && gradle build       # JDK 17 -> build/libs/ (ready jar: dist/kaiju-1.20.1-forge-0.1.0.jar)
```

To take automatic screenshots in game: `KAIJU_SHOTS=1 KAIJU_SIZE=1600x900 gradle runClient`. The shots are saved to `run/screenshots/`.
