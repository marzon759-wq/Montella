#!/usr/bin/env python3
"""contact sheet of clips: python3 clipsheet.py out.png clip1 clip2 ... (6 frames each)"""
import json, os, sys
from multiprocessing import Pool
from PIL import Image, ImageDraw
import preview as pv
HERE = os.path.dirname(os.path.abspath(__file__))
GEO = json.load(open(os.path.join(HERE, "out/kaiju.geo.json")))
AN = json.load(open(os.path.join(HERE, "out/kaiju.animation.json")))
TEX = Image.open(os.path.join(HERE, "out/kaiju.png")).convert("RGBA")
W, H = 400, 260

def one(args):
    clip, t, yaw = args
    a = pv.Anim(AN, "animation.kaiju." + clip)
    q, _ = pv.build_quads(GEO, a, t)
    im = pv.render(q, TEX, None, W, H, yaw, 18, 330, (0, 45, 20), 40, sky_img=pv.sky(W, H), shadows=False)
    ImageDraw.Draw(im).text((6, 4), f"{clip} {t:.2f}", fill=(255, 255, 255))
    return im

if __name__ == "__main__":
    out, clips = sys.argv[1], sys.argv[2:]
    jobs = []
    for c in clips:
        L = AN["animations"]["animation.kaiju." + c]["animation_length"]
        for k in range(6):
            jobs.append((c, L * k / 5 * 0.999, 40))
    with Pool(4) as p:
        ims = p.map(one, jobs)
    sheet = Image.new("RGB", (W * 6, H * len(clips)))
    for i, im in enumerate(ims):
        sheet.paste(im, ((i % 6) * W, (i // 6) * H))
    sheet.save(out)
