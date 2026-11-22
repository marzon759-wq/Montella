#!/usr/bin/env python3
"""Destoroyah's animations and server spec (nodes, attacks, pose tracks)."""
import json, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import moves_destoroyah
import specgen

LENS = [10, 10, 10, 10, 9.5, 9.5, 9, 9, 8.5, 8.5, 8, 8, 7.5, 7.5, 7, 7]
RADS = [12.5, 11.6, 10.8, 10.0, 9.2, 8.5, 7.8, 7.1, 6.5, 5.9, 5.3, 4.8, 4.3, 3.8, 3.4, 3.0, 2.8]
NODES = [
    ("head", "head", (0, 90.5, -34), 7.0, "head", ""),
    ("snout", "head", (0, 88.5, -45), 4.6, "head", ""),
    ("jaw", "jaw", (0, 85, -42), 4.0, "head", ""),
    ("horn", "horn2", (0, 106, -41), 3.0, "head", ""),
    ("neck", "neck2", (0, 82.5, -18.5), 9.5, "neck", ""),
    ("chest", "chest", (0, 64, -5), 16.0, "chest", ""),
    ("belly", "belly", (0, 52, -1), 15.5, "belly", ""),
    ("hips", "body", (0, 40, 4), 17.0, "belly", ""),
    ("shoulder_l", "shoulder_l", (17, 67, -6), 6.0, "arm_l", ""),
    ("shoulder_r", "shoulder_r", (-17, 67, -6), 6.0, "arm_r", ""),
    ("elbow_l", "forearm_l", (21, 53, -13), 4.6, "arm_l", "arm_l"),
    ("elbow_r", "forearm_r", (-21, 53, -13), 4.6, "arm_r", "arm_r"),
    ("hand_l", "hand_l", (21, 52, -31), 4.6, "arm_l", "arm_l"),
    ("hand_r", "hand_r", (-21, 52, -31), 4.6, "arm_r", "arm_r"),
    ("thigh_l", "thigh_l", (15, 30, 1), 12.5, "legs", ""),
    ("thigh_r", "thigh_r", (-15, 30, 1), 12.5, "legs", ""),
    ("knee_l", "thigh_l", (16, 22, -4), 9.0, "legs", ""),
    ("knee_r", "thigh_r", (-16, 22, -4), 9.0, "legs", ""),
    ("shin_l", "shin_l", (16, 12, -1), 8.8, "legs", ""),
    ("shin_r", "shin_r", (-16, 12, -1), 8.8, "legs", ""),
    ("foot_l", "foot_l", (16, 2.5, -4), 8.2, "legs", ""),
    ("foot_r", "foot_r", (-16, 2.5, -4), 8.2, "legs", ""),
    ("wing_l", "wingfore_l", (55, 92, 19), 9.0, "chest", ""),
    ("wing_r", "wingfore_r", (-55, 92, 19), 9.0, "chest", ""),
]
z = 13.0
for i, L in enumerate(LENS):
    NODES.append((f"tail{i + 1}", f"tail{i + 1}", (0, 40, z + L / 2), (RADS[i] + RADS[i + 1]) / 2, "tail", "tail_tip" if i >= 10 else ""))
    z += L
NODES.append(("mouth", "head", (0, 86.8, -52), 0.0, "head", ""))
NODES.append(("aim", "head", (0, 86.8, -92), 0.0, "head", ""))
NODES.append(("horn_tip", "horn3", (0, 122, -42.5), 0.0, "head", ""))

SEVER = {"arm_l": "forearm_l", "arm_r": "forearm_r", "tail_tip": "tail11"}
REGIONS = {"head": ["head"], "neck": ["neck1", "throat"], "chest": ["chest"], "belly": ["body", "belly"],
           "arm_l": ["shoulder_l"], "arm_r": ["shoulder_r"], "legs": ["thigh_l", "thigh_r"], "tail": ["tail1"]}
PARAMS = {
    "model_height_px": 96, "scale": 8.0, "mass": 22000.0, "max_hp": 6400.0,
    "walk_speed_px": 16.0, "fast_speed_px": 50.0, "turn_deg": 2.6,
    "can_fly": True, "fly_speed_px": 90.0, "breath_pacing": [9, 16], "breath_min_px": 70,
    "beam_style": 1,
    "keys": {"breath": "micro_oxygen_beam", "breath_down": "micro_oxygen_burst", "breath_up": "micro_oxygen_beam",
             "breath_sprint": "micro_oxygen_sweep", "breath_guard": "horn_laser", "breath_air": "aerial_micro_oxygen",
             "breath_ground": "micro_oxygen_burst", "pulse": "wing_buffet", "bite": "horn_gore", "special": "tail_pincer_grab",
             "air_attack": "fly_dive_claw", "air_grab": "fly_grab_drop"},
}


def main():
    clips, attacks = moves_destoroyah.build()
    specgen.export("destoroyah", "destoroyah", clips, attacks, NODES, SEVER, REGIONS, [], PARAMS)


if __name__ == "__main__":
    main()
