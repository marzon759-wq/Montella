#!/usr/bin/env python3
"""3D models for the story items in the hand (and on the ground / in frames).

The inventory keeps the flat 32px icon (gen_gui.py writes it as <name>_icon); everywhere else the item is a real
model built from the same materials as the farm props. Run after gen_gui.py.
Every model points "forward" towards north (-z): the lens of the flashlight, the tip of the crowbar.
"""
import json, os
from gen_farm import box, A

T = lambda n: f"mimicry:block/m_{n}"


def write(name, textures, els, fp=(0, 0, 0), fpt=(-0.5, 4.5, -4), fps=0.33, tp=(90, 0, 0), tpt=(0, 3, 1), tps=0.85, gs=0.45):
    textures = dict(textures)
    textures.setdefault("particle", list(textures.values())[0])
    display = {
        "firstperson_righthand": {"rotation": list(fp), "translation": list(fpt), "scale": [fps] * 3},
        "firstperson_lefthand": {"rotation": [fp[0], -fp[1], -fp[2]], "translation": list(fpt), "scale": [fps] * 3},
        "thirdperson_righthand": {"rotation": list(tp), "translation": list(tpt), "scale": [tps] * 3},
        "thirdperson_lefthand": {"rotation": [tp[0], -tp[1], -tp[2]], "translation": list(tpt), "scale": [tps] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [gs] * 3},
        "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [0.7] * 3},
        "head": {"rotation": [0, 0, 0], "translation": [0, 10, 0], "scale": [0.8] * 3},
    }
    base = {"textures": textures, "elements": els, "display": display}
    # the inventory shows the drawn icon; the hand, the ground and item frames show the model
    model = {
        "loader": "forge:separate_transforms",
        "gui_light": "front",
        "base": base,
        "perspectives": {"gui": {"parent": f"mimicry:item/{name}_icon"}},
    }
    with open(os.path.join(A, f"models/item/{name}.json"), "w") as f:
        json.dump(model, f, separators=(",", ":"))


def ring(t, x1, x2, z1, z2, y1, y2, w):
    return [box([x1, y1, z1], [x2, y2, z1 + w], t), box([x1, y1, z2 - w], [x2, y2, z2], t),
            box([x1, y1, z1 + w], [x1 + w, y2, z2 - w], t), box([x2 - w, y1, z1 + w], [x2, y2, z2 - w], t)]


def flame(x, y, z, h):
    """two crossed planes of the animated flame"""
    return [box([x, y, z - 1.1], [x, y + h, z + 1.1], "f", faces={"east": "f", "west": "f", "north": None, "south": None, "up": None, "down": None}, shade=False),
            box([x - 1.1, y, z], [x + 1.1, y + h, z], "f", faces={"north": "f", "south": "f", "east": None, "west": None, "up": None, "down": None}, shade=False)]


def gen():
    # ---------------------------------------------------------------- flashlight: a heavy old metal torch
    els = [
        box([6.5, 6.5, 4], [9.5, 9.5, 15], "b"),
        box([5.5, 5.5, 1], [10.5, 10.5, 4], "m"),
        box([6.0, 6.0, 3.6], [10.0, 10.0, 4.6], "b"),
        box([5.2, 5.2, 0.6], [10.8, 10.8, 1.3], "b"),
        box([5.9, 5.9, 0.4], [10.1, 10.1, 0.6], "g", shade=False),
        box([7.3, 9.5, 5.8], [8.7, 10.2, 7.6], "r"),
        box([6.8, 6.8, 15], [9.2, 9.2, 15.8], "m"),
    ]
    for z in (9, 11, 13):
        els.append(box([6.3, 6.3, z], [9.7, 9.7, z + 0.6], "m"))
    write("flashlight", {"b": T("black"), "m": T("iron"), "g": T("lamp_glass_on"), "r": T("red_paint")}, els,
          fp=(0, -6, 0), fpt=(-0.5, 5.0, -4), fps=0.45)

    # ---------------------------------------------------------------- battery: a big D cell
    write("battery", {"b": T("black"), "y": T("brass"), "m": T("iron")}, [
        box([6, 6, 4], [10, 10, 12], "b"),
        box([5.9, 5.9, 4], [10.1, 10.1, 6.6], "y"),
        box([7.2, 7.2, 3.3], [8.8, 8.8, 4], "m"),
        box([6.4, 6.4, 12], [9.6, 9.6, 12.3], "m"),
    ], fp=(0, -30, 0), fpt=(-0.5, 5.5, -4), fps=0.4)

    # ---------------------------------------------------------------- keys
    def key(t, extra=()):
        return ring(t, 5, 11, 10.5, 16, 7.6, 8.4, 1.2) + [
            box([7.4, 7.6, 1], [8.6, 8.4, 10.5], t),
            box([8.6, 7.6, 1.6], [10, 8.4, 2.8], t),
            box([8.6, 7.6, 4.2], [9.5, 8.4, 5.4], t),
            box([8.6, 7.6, 6.6], [10.2, 8.4, 7.8], t),
        ] + list(extra)
    write("house_key", {"k": T("brass")}, key("k"), fp=(0, -10, -20), fpt=(-0.5, 6.0, -4), fps=0.45)
    write("truck_keys", {"k": T("iron"), "r": T("black"), "c": T("cloth_red")}, key("k", [
        box([4.5, 7.1, 10.3], [11.5, 8.9, 14.6], "r"),
        box([10.4, 7.8, 14.2], [13.4, 8.2, 14.8], "k"), box([12.8, 7.8, 14.2], [13.4, 8.2, 17.4], "k"),
        box([12.0, 7.4, 16.6], [15.0, 8.6, 20.0], "c"),
    ]), fp=(0, -10, -20), fpt=(-0.5, 6.0, -4), fps=0.45)

    # ---------------------------------------------------------------- walkie-talkie: upright, the speaker towards you
    write("walkie", {"b": T("black"), "m": T("iron"), "s": T("radio_face"), "g": T("lamp_glass_on"), "r": T("red_paint")}, [
        box([5.5, 1, 6], [10.5, 12, 9.4], "b"),
        box([6, 5.5, 9.4], [10, 11, 9.6], "s"),
        box([6.4, 2.2, 9.4], [9.6, 4.4, 9.5], "g", shade=False),
        box([8.9, 12, 7.1], [9.7, 20, 7.9], "b"),
        box([8.7, 19.6, 6.9], [9.9, 20.4, 8.1], "m"),
        box([6.1, 12, 7], [7.5, 13.2, 8.4], "m"),
        box([10.5, 6, 7], [10.9, 9, 8.2], "r"),
        box([5.3, 3, 6.6], [5.5, 10, 8.8], "m"),
    ], fp=(0, -15, 0), fpt=(-0.5, 4.0, -4), fps=0.4, tp=(0, 0, 0))

    # ---------------------------------------------------------------- crowbar: red paint worn off at both ends
    write("crowbar", {"r": T("red_paint"), "m": T("rust")}, [
        box([7.5, 7.5, -1], [8.5, 8.5, 13], "r"),
        box([7.4, 7.0, -3.2], [8.6, 8.0, -0.6], "m", rot=("x", 22.5, [8, 8, -1])),
        box([7.5, 8.2, 12.6], [8.5, 11.0, 13.6], "m"),
        box([7.5, 10.2, 13.6], [8.5, 11.2, 16.0], "m"),
        box([7.6, 9.4, 15.6], [8.4, 10.6, 16.6], "m"),
    ], fp=(0, -4, 0), fpt=(-0.5, 4.5, -4), fps=0.45, tp=(40, 0, 0), tpt=(0, 4, 0))

    # ---------------------------------------------------------------- bolt cutters
    write("bolt_cutters", {"r": T("red_paint"), "b": T("black"), "m": T("iron")}, [
        box([6.6, 7.5, 5], [7.6, 8.5, 13], "r", rot=("y", -22.5, [8, 8, 5])),
        box([8.4, 7.5, 5], [9.4, 8.5, 13], "r", rot=("y", 22.5, [8, 8, 5])),
        box([6.4, 7.3, 12], [7.8, 8.7, 17], "b", rot=("y", -22.5, [8, 8, 5])),
        box([8.2, 7.3, 12], [9.6, 8.7, 17], "b", rot=("y", 22.5, [8, 8, 5])),
        box([6.6, 7.2, 3], [9.4, 8.8, 6], "m"),
        box([6.2, 6.8, 4], [9.8, 9.2, 5], "m"),
        box([6.9, 7.4, -1.5], [7.9, 8.6, 3], "m", rot=("y", 22.5, [8, 8, 3])),
        box([8.1, 7.4, -1.5], [9.1, 8.6, 3], "m", rot=("y", -22.5, [8, 8, 3])),
    ], fp=(0, -4, 0), fpt=(-0.5, 5.0, -4), fps=0.5, tp=(40, 0, 0), tpt=(0, 4, 0))

    # ---------------------------------------------------------------- gas can, carried by the handle
    write("gas_can", {"r": T("gas_can"), "m": T("iron"), "b": T("black")}, [
        box([5, 0, 3], [11, 10, 13], "r"),
        box([5.6, 10, 5], [6.6, 12, 6], "b"), box([5.6, 10, 10], [6.6, 12, 11], "b"),
        box([5.6, 12, 5], [6.6, 13, 11], "b"),
        box([8.4, 10, 3.6], [10.4, 11.4, 5.6], "m"),
        box([9.0, 11, 1.6], [9.8, 11.8, 4.4], "m", rot=("x", 45, [9.4, 11.4, 4])),
    ], fp=(0, 90, 0), fpt=(-0.5, 1.0, -4), fps=0.33, tp=(-90, 0, 0), tpt=(0, -2, 1), tps=0.45)

    # ---------------------------------------------------------------- meat: a raw cut on the bone
    write("meat", {"f": T("flesh"), "w": T("fat"), "b": T("bone"), "d": T("blood")}, [
        box([4.5, 5, 4], [11.5, 10, 12], "f"),
        box([4.5, 10, 4], [11.5, 10.8, 12], "w"),
        box([5.5, 4.6, 5], [10.5, 5, 11], "d"),
        box([7, 6.5, 12], [9, 8.5, 15.5], "b"),
        box([6.4, 6, 15.4], [9.6, 9, 17], "b"),
    ], tp=(0, 0, 0), fp=(0, -20, 0), fpt=(-0.5, 5.0, -4), fps=0.38)

    # ---------------------------------------------------------------- lighter: an old flip lighter, lit
    write("lighter", {"t": T("tin"), "m": T("iron"), "f": T("flame")}, [
        box([6.5, 3, 7], [9.5, 8, 8.6], "t"),
        box([6.6, 8, 8.4], [9.4, 11.2, 8.7], "t"),
        box([7.0, 8, 7.2], [9.0, 9.8, 8.2], "m"),
        box([8.6, 9.0, 7.4], [9.2, 9.6, 8.0], "m"),
    ] + flame(8, 9.4, 7.7, 3.6), tp=(0, 0, 0), fp=(0, -10, 0), fpt=(-0.5, 5.5, -4), fps=0.5)

    # ---------------------------------------------------------------- pitchfork
    els = [box([7.5, 7.5, -2], [8.5, 8.5, 22], "w"),
           box([5, 7.4, -3], [11, 8.6, -1.8], "m")]
    for x in (5.0, 7.6, 10.2):
        els.append(box([x, 7.6, -11], [x + 0.8, 8.4, -2.6], "m"))
    write("pitchfork", {"w": T("wood_old"), "m": T("rust")}, els, fp=(0, -4, 0), fpt=(-0.5, 3.5, -4), fps=0.5, tp=(40, 0, 0), tpt=(0, 4, 0))


def icons():
    """move the flat icons gen_gui.py wrote to <name>_icon"""
    for name in ("flashlight", "battery", "house_key", "walkie", "crowbar", "bolt_cutters", "gas_can", "truck_keys",
                 "meat", "lighter", "pitchfork"):
        p = os.path.join(A, f"models/item/{name}.json")
        m = json.load(open(p))
        if "loader" in m:
            continue
        m["parent"] = "minecraft:item/generated"
        with open(os.path.join(A, f"models/item/{name}_icon.json"), "w") as f:
            json.dump(m, f)


if __name__ == "__main__":
    icons()
    gen()
    print("held ok")
