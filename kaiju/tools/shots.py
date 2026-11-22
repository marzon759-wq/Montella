#!/usr/bin/env python3
"""Renders the kaiju preview sheet: rest pose from several angles, a roar pose, and the glow variants.
python3 shots.py [outdir]"""
import json, os, sys, time
import numpy as np
from PIL import Image
import preview as pv

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "out")
DST = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "prev")
os.makedirs(DST, exist_ok=True)
geo = json.load(open(os.path.join(SRC, "kaiju.geo.json")))
anims = json.load(open(os.path.join(SRC, "kaiju.animation.json")))
tex = {k: Image.open(os.path.join(SRC, f"kaiju{k}.png")) for k in ("", "_glowmask", "_burning", "_burning_glowmask")}


def shot(name, pose=None, yaw=30, pitch=6, dist=260, ty=52, glow=None, skin="", W=1280, H=720, fov=40, gain=1.0, tint=(1, 1, 1), only=None):
    if only and name not in only:
        return
    t0 = time.time()
    a = pv.Anim(anims, pose) if pose else None
    q = pv.build_quads(geo, a, 0.0)
    sk = pv.sky(W, H, 5, tint)
    img = pv.render(q, tex[skin], W, yaw, pitch, dist, (0, ty, 0), fov, glow=tex[glow] if glow else None, height=H,
                    glow_gain=gain, sky_img=sk)
    img = Image.fromarray(np.clip(np.asarray(img, float) * 1.25 + 4, 0, 255).astype(np.uint8))
    img.save(os.path.join(DST, name + ".png"))
    print(name, f"{time.time() - t0:.1f}s", flush=True)


only = set(sys.argv[2:]) or None
shot("01_front", yaw=0, pitch=4, dist=190, ty=50, only=only)
shot("02_three_quarter", yaw=38, pitch=8, dist=200, ty=50, only=only)
shot("03_side", yaw=90, pitch=4, dist=270, ty=46, only=only)
shot("04_back", yaw=150, pitch=16, dist=250, ty=46, only=only)
shot("05_head_closeup", pose="pose.roar", yaw=32, pitch=2, dist=105, ty=88, W=1080, H=1080, only=only)
shot("06_roar", pose="pose.roar", yaw=-28, pitch=-4, dist=185, ty=56, only=only)
shot("07_atomic_charge", pose="pose.roar", yaw=-72, pitch=4, dist=250, ty=52, glow="_glowmask", gain=1.0, tint=(0.75, 0.9, 1.05), only=only)
shot("08_burning", pose="pose.roar", yaw=22, pitch=-4, dist=190, ty=54, skin="_burning", glow="_burning_glowmask", gain=1.0, tint=(1.05, 0.92, 0.85), only=only)
