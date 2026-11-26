"""Sounds of the omens at sea: a ship's bell far off (omen_bell), blood rain (omen_rain, loops), and a moan from the deep (omen_moan)."""
import json
import os
import sys

import numpy as np
from scipy import signal

sys.path.insert(0, os.path.dirname(__file__))
from gen_music import SR, lowpass, bandpass, reverb_ir, smooth_noise  # noqa: E402
from gen_talk_sounds import write_ogg as _w  # noqa: E402  (mono, 44.1k)

A = os.path.join(os.path.dirname(__file__), "..", "common", "src", "main", "resources", "assets", "bloodsea")
rng = np.random.default_rng(77)


def write(x, name):
    import gen_talk_sounds
    gen_talk_sounds.SR = SR
    _w(x / np.abs(x).max() * 0.9, os.path.join(A, "sounds", name + ".ogg"))


def bell():
    n = int(11 * SR)
    t = np.arange(n) / SR
    out = np.zeros(n)
    f = 176.0
    parts = [(0.5, 1.0, 0.6), (1.0, 0.8, 1.0), (1.19, 0.6, 1.3), (1.5, 0.5, 1.6), (2.0, 0.5, 2.2), (2.5, 0.3, 3.0), (3.0, 0.25, 3.6), (4.2, 0.15, 5.0)]
    for s in (0.3, 3.6, 6.9):
        i = int(s * SR)
        tt = t[: n - i]
        hit = sum(a * np.sin(2 * np.pi * f * r * tt * (1 + 0.0004 * np.sin(2 * np.pi * 0.7 * tt))) * np.exp(-tt * d * 0.55) for r, a, d in parts)
        hit += np.exp(-tt * 60) * rng.normal(0, 0.3, len(tt))
        out[i:] += hit
    # far away: the top end gone, a long wet tail
    out = lowpass(out, 1400)
    ir = reverb_ir(5.0, 1200, 5)
    out = out * 0.35 + signal.fftconvolve(out, ir)[:n] * 1.6
    return out


def rain():
    n = int(12 * SR)
    noise = rng.normal(0, 1, n)
    bed = bandpass(noise, 300, 5000) * 0.35 + lowpass(noise, 400) * 0.25
    drops = np.zeros(n)
    for _ in range(2600):
        i = rng.integers(0, n - 2000)
        d = int(rng.uniform(80, 400))
        tt = np.arange(d) / SR
        f = rng.uniform(500, 2600)
        # heavy, thick drops: a little lower and wetter than water
        drops[i:i + d] += rng.uniform(0.1, 0.6) * np.sin(2 * np.pi * f * tt * (1 - tt * 8)) * np.exp(-tt * rng.uniform(80, 200))
    x = bed + drops
    x *= 0.85 + 0.15 * smooth_noise(n, 0.3)
    # seamless loop
    f = int(1.5 * SR)
    k = np.linspace(0, 1, f)
    x[:f] = x[:f] * k + x[-f:] * (1 - k)
    return x[: n - f]


def moan():
    n = int(9 * SR)
    t = np.arange(n) / SR
    f0 = 46 + 22 * np.sin(np.pi * np.clip(t / 7.5, 0, 1)) ** 1.5 - 8 * np.clip((t - 6) / 3, 0, 1)
    ph = 2 * np.pi * np.cumsum(f0 * (1 + 0.01 * smooth_noise(n, 3))) / SR
    x = sum((1 / k) * np.sin(k * ph) for k in range(1, 16))
    # a mouth the size of a house: a moving formant
    form = np.zeros(n)
    block = SR // 4
    for s in range(0, n, block):
        c = 220 + 160 * np.sin(np.pi * s / n)
        seg = bandpass(x[max(0, s - 2048):s + block], c * 0.6, c * 1.6)
        form[s:s + block] = seg[-len(form[s:s + block]):]
    x = 0.6 * lowpass(x, 300) + 0.8 * form
    env = np.sin(np.pi * np.clip(t / 8.8, 0, 1)) ** 0.8
    x *= env
    ir = reverb_ir(6.0, 600, 9)
    return x * 0.4 + signal.fftconvolve(x, ir)[:n] * 1.4


if __name__ == "__main__":
    write(bell(), "omen_bell")
    write(rain(), "omen_rain")
    write(moan(), "omen_moan")
    p = os.path.join(A, "sounds.json")
    d = json.load(open(p))
    for k in ("omen_bell", "omen_rain", "omen_moan"):
        d[k] = {"sounds": ["bloodsea:" + k], "subtitle": "subtitles.bloodsea." + k}
    for k in ("drift", "night", "dread"):
        d["music_" + k] = {"sounds": [{"name": "bloodsea:music/music_" + k, "stream": True}]}
    json.dump(d, open(p, "w"), indent=1)
    print("ok")
