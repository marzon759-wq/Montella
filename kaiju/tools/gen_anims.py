#!/usr/bin/env python3
"""Builds every clip of the atomic kaiju and the data the server plays it with.

Writes:
  out/kaiju.animation.json            GeckoLib animations (animation.kaiju.<clip>)
  out/atomic_kaiju.json               server data: spec, collision nodes, attack catalogue, and for every clip a pose
                                      track: the model-space position of each collision node at every game tick (20 Hz),
                                      sampled from the very same animation the client plays, so hit spheres follow the
                                      visible body exactly
"""
import base64, json, math, os, struct, sys
import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
sys.path.insert(0, HERE)
import moves
import preview as pv

PLATES = json.load(open(os.path.join(OUT, "plates.json")))
GEO = json.load(open(os.path.join(OUT, "kaiju.geo.json")))
BONES = {b["name"]: b for b in GEO["minecraft:geometry"][0]["bones"]}

# collision nodes: bone, point in model px (bedrock space), radius px, burn region, severable group
LENS = [11, 11, 11, 11, 10.5, 10.5, 10, 10, 9.5, 9.5, 9, 9, 8.5, 8, 8, 7.5]
RADS = [13.5, 12.6, 11.7, 10.8, 9.9, 9.1, 8.3, 7.6, 6.9, 6.2, 5.6, 5.0, 4.4, 3.8, 3.2, 2.6, 1.9]
NODES = [
    ("head", "head", (0, 90, -26), 7.0, "head", ""),
    ("snout", "head", (0, 88, -34.5), 4.6, "head", ""),
    ("jaw", "jaw", (0, 84.5, -30), 4.0, "head", ""),
    ("neck", "neck2", (0, 82, -8), 10.5, "neck", ""),
    ("chest", "chest", (0, 65, -3), 15.0, "chest", ""),
    ("belly", "belly", (0, 52, -1), 15.0, "belly", ""),
    ("hips", "body", (0, 40, 3), 17.0, "belly", ""),
    ("shoulder_l", "shoulder_l", (17, 67, -6.5), 6.0, "arm_l", ""),
    ("shoulder_r", "shoulder_r", (-17, 67, -6.5), 6.0, "arm_r", ""),
    ("elbow_l", "forearm_l", (19.5, 57, -12), 4.5, "arm_l", "arm_l"),
    ("elbow_r", "forearm_r", (-19.5, 57, -12), 4.5, "arm_r", "arm_r"),
    ("hand_l", "hand_l", (19.5, 56, -27), 4.2, "arm_l", "arm_l"),
    ("hand_r", "hand_r", (-19.5, 56, -27), 4.2, "arm_r", "arm_r"),
    ("thigh_l", "thigh_l", (14.5, 31, 0), 12.0, "legs", ""),
    ("thigh_r", "thigh_r", (-14.5, 31, 0), 12.0, "legs", ""),
    ("knee_l", "thigh_l", (15, 22, -4), 9.0, "legs", ""),
    ("knee_r", "thigh_r", (-15, 22, -4), 9.0, "legs", ""),
    ("shin_l", "shin_l", (15, 12, -1.5), 8.6, "legs", ""),
    ("shin_r", "shin_r", (-15, 12, -1.5), 8.6, "legs", ""),
    ("foot_l", "foot_l", (15, 2.5, -3), 8.0, "legs", ""),
    ("foot_r", "foot_r", (-15, 2.5, -3), 8.0, "legs", ""),
]
z = 12.0
for i, L in enumerate(LENS):
    NODES.append((f"tail{i + 1}", f"tail{i + 1}", (0, 40, z + L / 2), (RADS[i] + RADS[i + 1]) / 2, "tail", "tail_tip" if i >= 10 else ""))
    z += L
NODES.append(("mouth", "head", (0, 86.5, -38), 0.0, "head", ""))
NODES.append(("aim", "head", (0, 86.5, -78), 0.0, "head", ""))  # mouth -> aim = where the head points

SEVER = {"arm_l": "forearm_l", "arm_r": "forearm_r", "tail_tip": "tail11"}
REGIONS = {"head": ["head"], "neck": ["neck1", "throat"], "chest": ["chest"], "belly": ["body", "belly"],
           "arm_l": ["shoulder_l"], "arm_r": ["shoulder_r"], "legs": ["thigh_l", "thigh_r"], "tail": ["tail1"]}


def node_tracks(anim):
    """positions of all nodes at 20 Hz for one animation (preview's GeckoLib-equivalent bone math, no cubes)"""
    n = max(1, int(round(anim.len * 20)))
    need = set()
    for _, b, *_ in NODES:
        while b:
            need.add(b)
            b = BONES[b].get("parent")
    order = [b for b in BONES if b in need]
    out = np.zeros((n, len(NODES), 3))
    for k in range(n):
        t = k / 20
        world = {}
        for name in order:
            b = BONES[name]
            parent = world[b["parent"]] if "parent" in b else np.eye(4)
            r = np.array(b.get("rotation", [0, 0, 0]), float)
            ar, ap, asc = anim.pose(name, t)
            if ar is not None:
                r = r + ar
            m = parent @ (pv.M(t=ap) if ap is not None else np.eye(4)) @ pv.about(b["pivot"], pv.rot(r))
            if asc is not None:
                m = m @ pv.about(b["pivot"], np.diag(np.maximum(asc, 1e-4)))
            world[name] = m
        for j, (_, bone, p, *_rest) in enumerate(NODES):
            out[k, j] = (world[bone] @ np.array([*p, 1.0]))[:3]
    return out


def pack(arr):
    q = np.clip(np.round(arr * 10), -32767, 32767).astype("<i2")
    return base64.b64encode(q.tobytes()).decode()


def main():
    clips, attacks = moves.build(PLATES)
    anims = {}
    for name, c in clips.items():
        anims[f"animation.kaiju.{name}"] = c.to_json()
    with open(os.path.join(OUT, "kaiju.animation.json"), "w") as f:
        json.dump({"format_version": "1.8.0", "animations": anims}, f, separators=(",", ":"))
    data = {"format_version": "1.8.0", "animations": anims}

    tracks = {}
    for name in clips:
        a = pv.Anim(data, f"animation.kaiju.{name}")
        a.len = clips[name].length
        tr = node_tracks(a)
        tracks[name] = {"ticks": int(tr.shape[0]), "loop": clips[name].loop, "data": pack(tr)}
    spec = {
        "id": "atomic_kaiju",
        "model_height_px": 96, "scale": 8.0, "mass": 20000.0, "max_hp": 6000.0,
        "walk_speed_px": 18.0, "fast_speed_px": 30.0, "turn_deg": 2.4,
        "nodes": [{"name": n, "bone": b, "r": r, "region": reg, "sever": sv} for n, b, p, r, reg, sv in NODES],
        "sever_bones": SEVER, "regions": REGIONS,
        "plates": PLATES,
        "clips": {k: {"len": c.length, "loop": c.loop, "impacts": [round(t, 3) for t, s in c.impacts if s >= 0.3]} for k, c in clips.items()},
        "attacks": attacks,
        "tracks": tracks,
    }
    with open(os.path.join(OUT, "atomic_kaiju.json"), "w") as f:
        json.dump(spec, f, separators=(",", ":"))
    sz = os.path.getsize(os.path.join(OUT, "kaiju.animation.json")) / 1e6
    sz2 = os.path.getsize(os.path.join(OUT, "atomic_kaiju.json")) / 1e6
    print(f"clips {len(clips)} attacks {len(attacks)} anim.json {sz:.1f} MB data {sz2:.1f} MB")


if __name__ == "__main__":
    main()
