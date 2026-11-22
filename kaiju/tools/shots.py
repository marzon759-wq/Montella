#!/usr/bin/env python3
"""Kaiju previews.
  python3 shots.py stills [names...]   -> prev/*.png
  python3 shots.py atomic              -> prev/atomic/*.png frames + prev/atomic_breath.mp4
  python3 shots.py strip               -> prev/atomic_strip.png (key moments of the atomic breath)
Per-plate glow follows each plate's slide-out (offset / travel); the mouth glows while charging and firing."""
import json, math, os, subprocess, sys, time
from multiprocessing import Pool
import numpy as np
from PIL import Image, ImageDraw
import preview as pv

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "out")
DST = os.path.join(HERE, "prev")
os.makedirs(DST, exist_ok=True)
GEO = json.load(open(os.path.join(SRC, "kaiju.geo.json")))
ANIMS = json.load(open(os.path.join(SRC, "kaiju.animation.json")))
TRAVEL = {p["bone"]: p["travel"] for p in json.load(open(os.path.join(SRC, "plates.json")))}
TEX = {k: Image.open(os.path.join(SRC, f"kaiju{k}.png")).convert("RGBA") for k in ("", "_glowmask", "_burning", "_burning_glowmask")}
ATOMIC = "animation.kaiju.atomic_breath"
FIRE0, FIRE1 = 3.35, 5.3


def mouth_glow(t):
    if t < 2.4 or t > 5.9:
        return 0.0
    if t < FIRE0:
        return (t - 2.4) / (FIRE0 - 2.4) * 0.8
    if t <= FIRE1:
        return 1.0
    return max(0.0, 1 - (t - FIRE1) / 0.6)


def atomic_glow(anim, t):
    def fn(bone):
        if bone in TRAVEL:
            _, p, _ = anim.pose(bone, t)
            ext = 0.0 if p is None else p[1] / TRAVEL[bone]
            flick = 0.9 + 0.1 * math.sin(t * 40 + hash(bone) % 7)
            return max(0.0, min(1.0, ext)) * flick * (1.25 if FIRE0 <= t <= FIRE1 else 1.0)
        if bone in ("head", "jaw", "tongue1", "tongue2"):
            return mouth_glow(t)
        return 0.0
    return fn


def beam(world, t):
    if not FIRE0 <= t <= FIRE1 + 0.12:
        return []
    grow = min(1.0, (t - FIRE0) / 0.18)
    fade = 1.0 if t <= FIRE1 else max(0.0, 1 - (t - FIRE1) / 0.12)
    H = world["head"]
    mouth = (H @ np.array([0, 86.0, -37.5, 1]))[:3]
    d = H[:3, :3] @ np.array([0, 0.22, -1.0])
    out = []
    for r, col in ((7.5, (0.05, 0.35, 0.9)), (4.5, (0.15, 0.6, 1.0)), (2.4, (0.6, 0.95, 1.0)), (1.1, (1.0, 1.0, 1.0))):
        w = r * (0.9 + 0.1 * math.sin(t * 60 + r))
        out += pv.box_quads(mouth, d, 1600 * grow, w * fade, tuple(c * fade for c in col))
    return out


def shot(name, pose=None, t=0.0, yaw=30, pitch=6, dist=230, ty=52, tx=0, tz=0, skin="", glow=None, W=1280, H=720, fov=40,
         tint=(1, 1, 1), atomic=False, out=None):
    t0 = time.time()
    a = pv.Anim(ANIMS, pose) if pose else None
    quads, world = pv.build_quads(GEO, a, t)
    gfn = None
    gtex = TEX[glow] if glow else None
    if atomic:
        gtex = TEX["_glowmask"]
        gfn = atomic_glow(a, t)
        quads = quads + beam(world, t)
    img = pv.render(quads, TEX[skin], gtex, W, H, yaw, pitch, dist, (tx, ty, tz), fov, glow_fn=gfn, sky_img=pv.sky(W, H, 5, tint))
    img.save(out or os.path.join(DST, name + ".png"))
    print(name, f"{time.time() - t0:.1f}s", flush=True)


STILLS = {
    "01_front": dict(yaw=0, pitch=4, dist=190, ty=50),
    "02_three_quarter": dict(yaw=38, pitch=8, dist=200, ty=50),
    "03_side": dict(yaw=90, pitch=4, dist=270, ty=46, tx=0),
    "04_back": dict(yaw=150, pitch=16, dist=250, ty=46),
    "05_head_closeup": dict(pose="pose.roar", yaw=32, pitch=2, dist=95, ty=89, W=1080, H=1080),
    "06_roar": dict(pose="pose.roar", yaw=-28, pitch=-4, dist=185, ty=56),
    "07_atomic_charge": dict(pose=ATOMIC, t=4.2, atomic=True, yaw=-62, pitch=2, dist=260, ty=58, tint=(0.75, 0.9, 1.05)),
    "08_burning": dict(pose="pose.roar", yaw=22, pitch=-4, dist=190, ty=54, skin="_burning", glow="_burning_glowmask", tint=(1.05, 0.92, 0.85)),
    "09_plates_closeup": dict(pose=ATOMIC, t=2.6, atomic=True, yaw=125, pitch=12, dist=150, ty=66, tint=(0.75, 0.9, 1.05)),
    "10_hand_detail": dict(yaw=-35, pitch=8, dist=48, ty=58, tx=-19, tz=-22, W=1080, H=1080),
}

FPS = 15


def frame(i):
    t = i / FPS
    yaw = -75 + 30 * (t / 7.0)
    shot(f"atomic_{i:03d}", pose=ATOMIC, t=t, atomic=True, yaw=yaw, pitch=3, dist=275, ty=56, tx=-20, W=960, H=540,
         tint=(0.75, 0.9, 1.05), out=os.path.join(DST, "atomic", f"{i:03d}.png"))


def main():
    mode = sys.argv[1] if len(sys.argv) > 1 else "stills"
    if mode == "stills":
        names = sys.argv[2:] or list(STILLS)
        with Pool(4) as p:
            p.starmap(_still, [(n,) for n in names])
    elif mode == "atomic":
        os.makedirs(os.path.join(DST, "atomic"), exist_ok=True)
        n = int(7.0 * FPS) + 1
        with Pool(4) as p:
            p.map(frame, range(n))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(FPS), "-i", os.path.join(DST, "atomic", "%03d.png"),
                        "-vf", "minterpolate=fps=30:mi_mode=blend", "-c:v", "libx264", "-pix_fmt", "yuv420p", "-crf", "18",
                        os.path.join(DST, "atomic_breath.mp4")], check=True)
    elif mode == "strip":
        moments = [(0.0, "0.0s  dinlenme"), (1.05, "1.0s  kuyruk plakalari cikiyor"), (1.9, "1.9s  dalga sirta ulasti"),
                   (2.45, "2.4s  tum plakalar disarida"), (4.0, "4.0s  atom nefesi"), (5.75, "5.7s  plakalar geri giriyor")]
        tiles = []
        for t, label in moments:
            p = os.path.join(DST, f"_strip_{t:.2f}.png")
            shot("strip", pose=ATOMIC, t=t, atomic=True, yaw=-100, pitch=6, dist=280, ty=56, tx=-10, W=720, H=480, tint=(0.75, 0.9, 1.05), out=p)
            im = Image.open(p)
            ImageDraw.Draw(im).text((14, 12), label, fill=(235, 245, 255))
            tiles.append(im)
        sheet = Image.new("RGB", (1440, 1440))
        for k, im in enumerate(tiles):
            sheet.paste(im, ((k % 2) * 720, (k // 2) * 480))
        sheet.save(os.path.join(DST, "atomic_strip.png"))


def _still(n):
    shot(n, **STILLS[n])


if __name__ == "__main__":
    main()
