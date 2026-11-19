#!/usr/bin/env python3
"""Render an animation reel of the model with the offline previewer (for review without starting the game)."""
import json, math, os, subprocess, sys
from multiprocessing import Pool
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from preview import Anim, build_quads, render, Image, ImageDraw

OUT = sys.argv[1]
FR = os.path.join(os.path.dirname(OUT) or ".", "_frames")
FPS = 20
SHOTS = [  # animation, seconds, camera yaw start, yaw drift, distance, target height
    ("idle", 4.0, 30, -20, 100, 27), ("walk", 3.2, 70, 0, 110, 25), ("stalk", 4.8, 45, -10, 100, 22),
    ("run", 2.4, 80, 0, 110, 15), ("listen", 3.0, 15, 0, 60, 40), ("mimic_talk", 4.0, 10, 0, 45, 45),
    ("search", 4.0, 25, 0, 100, 27), ("sniff", 2.0, 40, 0, 90, 25), ("roar", 3.0, 10, 10, 110, 30),
    ("spread", 4.0, -20, 40, 120, 30), ("scream", 2.2, 5, 0, 50, 42), ("grab", 1.4, 30, 0, 100, 28),
    ("kill", 4.0, 35, -15, 80, 36), ("eat", 3.0, 60, 0, 90, 18), ("crawl", 2.4, 70, 0, 100, 10),
    ("climb_wall", 3.2, 0, 0, 120, 28), ("emerge", 3.0, 20, 0, 110, 22), ("bang", 2.0, 60, 0, 100, 30),
    ("twitch", 2.5, 15, 0, 80, 32), ("death", 3.0, 50, 0, 110, 20)]

geo = json.load(open(sys.argv[2] if len(sys.argv) > 2 else "out/mimic.geo.json"))
A = json.load(open("out/mimic.animation.json"))
tex = Image.open("out/mimic.png")


def job(args):
    i, name, t, yaw, dist, ty = args
    an = Anim(A, "animation.mimic." + name)
    loop = A["animations"]["animation.mimic." + name].get("loop") is True
    tt = t % an.len if loop else min(t, an.len)
    im = render(build_quads(geo, an, tt), tex, 540, yaw, 8, dist, (0, ty, 0), bg=(34, 32, 36), light=(0.3, 0.8, -0.6))
    ImageDraw.Draw(im).text((14, 12), name, fill=(200, 190, 180))
    im.save(f"{FR}/{i:05d}.png")


if __name__ == "__main__":
    os.makedirs(FR, exist_ok=True)
    jobs, i = [], 0
    for name, dur, y0, dy, dist, ty in SHOTS:
        n = int(dur * FPS)
        for k in range(n):
            jobs.append((i, name, k / FPS, y0 + dy * k / n, dist, ty))
            i += 1
    with Pool(4) as p:
        p.map(job, jobs, chunksize=8)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(FPS), "-i", f"{FR}/%05d.png", "-c:v", "libx264",
                    "-pix_fmt", "yuv420p", "-crf", "20", OUT], check=True)
    print("frames", i)
