#!/usr/bin/env python3
"""Pip's voice from real recordings:
  kerzoven, "Cat purr & meow" (CC0)        https://opengameart.org/content/cat-purr-meow
  ignasd, "Meow" (CC0)                      https://opengameart.org/content/meow
  macro, "Cat SFX" - upset cat (CC-BY 3.0)  https://opengameart.org/content/cat-sfx
Trimmed, cleaned of rumble, levelled. usage: gen_cat_sounds.py <dir with the downloaded files>"""
import os, subprocess, sys, tempfile, json
import numpy as np
import soundfile as sf

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, "../common/src/main/resources/assets/bloodsea")
SRC = sys.argv[1]
SR = 44100


def load(name, filt="highpass=f=140,lowpass=f=9000"):
    with tempfile.NamedTemporaryFile(suffix=".wav") as t:
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", os.path.join(SRC, name), "-ac", "1", "-ar", str(SR), "-af", filt, t.name], check=True)
        x, _ = sf.read(t.name)
    return x


def trim(x, thr=0.04):
    a = np.abs(x)
    env = np.convolve(a, np.ones(400) / 400, mode="same")
    on = np.where(env > thr * env.max())[0]
    if len(on) == 0:
        return x
    x = x[max(0, on[0] - 800):min(len(x), on[-1] + 2400)]
    n = min(len(x) // 4, 2000)
    x[:n] *= np.linspace(0, 1, n)
    x[-n:] *= np.linspace(1, 0, n)
    return x


def seg(x, a, b):
    y = x[int(a * SR):int(b * SR)].copy()
    n = min(len(y) // 4, 2500)
    y[:n] *= np.linspace(0, 1, n)
    y[-n:] *= np.linspace(1, 0, n)
    return y


def norm(x, p=0.85):
    return x / (np.abs(x).max() + 1e-9) * p


def save(name, xs):
    files = []
    for i, x in enumerate(xs):
        fn = f"{name}{i if len(xs) > 1 else ''}"
        sf.write(os.path.join(A, "sounds", fn + ".ogg"), norm(x).astype(np.float32), SR, format="OGG", subtype="VORBIS")
        files.append(f"bloodsea:{fn}")
    return files


def main():
    sj = os.path.join(A, "sounds.json")
    d = json.load(open(sj))
    for k in list(d):
        if k.startswith("pip_"):
            for f in d[k]["sounds"]:
                n = f.split(":")[1] if isinstance(f, str) else f["name"].split(":")[1]
                p = os.path.join(A, "sounds", n + ".ogg")
                if os.path.exists(p):
                    os.remove(p)
    mewfood, softmew, mewpurr, mewpurr2, meow = (trim(load(n)) for n in ("cat_mewfood.wav", "cat_softmew.wav", "cat_mewpurr.wav", "cat_mewpurr2.wav", "Meow.ogg"))
    purr_a, purr_s = load("cat_purractive_loop.wav", "highpass=f=40,lowpass=f=4000"), load("cat_purrsleepy_loop.wav", "highpass=f=40,lowpass=f=4000")
    upset = [trim(load(f"cat_by_{i}.wav")) for i in range(1, 6)]
    out = {
        "pip_meow": save("pip_meow", [mewfood, softmew, meow, seg(mewpurr2, 0, min(1.3, len(mewpurr2) / SR))]),
        "pip_chirp": save("pip_chirp", [seg(softmew, 0, min(0.45, len(softmew) / SR)), seg(mewpurr, 0, min(0.5, len(mewpurr) / SR))]),
        "pip_purr": save("pip_purr", [seg(purr_a, 0.3, 4.3), seg(purr_s, 0.2, 4.2)]),
        "pip_hiss": save("pip_hiss", [upset[0], upset[1], upset[4]]),
        "pip_scream": save("pip_scream", [upset[2], upset[3]]),
    }
    for k, files in out.items():
        d[k] = {"sounds": files, "subtitle": f"subtitles.bloodsea.{k}"}
    json.dump(d, open(sj, "w"), indent=1)
    print("cat sounds:", {k: len(v) for k, v in out.items()})


if __name__ == "__main__":
    main()
