"""Generates the particle textures and the synthesized sounds of Montella Water Physics.
Everything is procedural (no third-party recordings). Run: python3 tools/gen_assets.py"""
import os, subprocess, tempfile
import numpy as np
from PIL import Image, ImageFilter
from scipy.signal import butter, sosfilt
from scipy.io import wavfile

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'forge-1.20.1', 'src', 'main', 'resources', 'assets', 'montella_water')
TEX = os.path.join(ROOT, 'textures', 'particle')
SND = os.path.join(ROOT, 'sounds')
rng = np.random.default_rng(7)

# ------------------------------------------------------------------ textures
def save(a, name):
    a = np.clip(a, 0, 255).astype(np.uint8)
    Image.fromarray(a, 'RGBA').save(os.path.join(TEX, name + '.png'))

def radial(n):
    y, x = np.mgrid[0:n, 0:n] + 0.5
    return np.hypot(x - n / 2, y - n / 2) / (n / 2)

def value_noise(n, cells, seed):
    r = np.random.default_rng(seed)
    g = r.random((cells + 1, cells + 1))
    g[-1, :] = g[0, :]; g[:, -1] = g[:, 0]
    img = Image.fromarray((g * 255).astype(np.uint8)).resize((n, n), Image.BICUBIC)
    return np.asarray(img).astype(float) / 255.0

def fbm(n, seed, octaves=4):
    s, a, tot = 0, 1.0, 0
    for o in range(octaves):
        s += value_noise(n, 3 * 2 ** o, seed + o) * a; tot += a; a *= 0.5
    return s / tot

# spray droplets: small bright beads with a highlight
for i in range(4):
    n = 8
    r = radial(n) * (1.0 + 0.15 * i)
    a = np.clip(1.0 - r, 0, 1) ** 0.8
    c = np.zeros((n, n, 4)); c[..., 0] = 225; c[..., 1] = 238; c[..., 2] = 248; c[..., 3] = a * 255
    c[2:4, 2:4, :3] = 255
    save(c, 'spray_%d' % i)

# mist: soft noisy puffs
for i in range(4):
    n = 32
    r = radial(n)
    f = fbm(n, 10 + i * 5)
    a = np.clip(1.0 - r, 0, 1) ** 1.6 * (0.45 + 0.75 * f)
    c = np.zeros((n, n, 4)); c[..., 0] = 235; c[..., 1] = 241; c[..., 2] = 246; c[..., 3] = np.clip(a, 0, 1) * 255
    save(c, 'mist_%d' % i)

# foam patches: clusters of bubbles with darker rims, lacy holes
for i in range(6):
    n = 32
    c = np.zeros((n, n, 4))
    yy, xx = np.mgrid[0:n, 0:n] + 0.5
    env = np.clip(1.0 - radial(n) * (0.95 + 0.1 * rng.random()), 0, 1)
    alpha = np.zeros((n, n)); shade = np.ones((n, n))
    for b in range(int(40 + 20 * rng.random())):
        cx, cy = rng.random() * n, rng.random() * n
        rad = 0.8 + rng.random() ** 2 * 3.2
        d = np.hypot(xx - cx, yy - cy) / rad
        bubble = np.clip(1.0 - d, 0, 1)
        rim = np.exp(-((d - 0.85) / 0.18) ** 2)
        alpha = np.maximum(alpha, (bubble > 0) * (0.55 + 0.45 * rim))
        shade = np.where(bubble > 0, np.minimum(shade, 0.82 + 0.18 * (1 - rim)), shade)
    lace = fbm(n, 50 + i)
    alpha *= env * np.clip((lace - 0.25) * 2.2, 0, 1)
    c[..., 0] = 236 * shade; c[..., 1] = 242 * shade; c[..., 2] = 246 * shade; c[..., 3] = np.clip(alpha, 0, 1) * 245
    save(c, 'foam_%d' % i)

# ripple ring
for i in range(2):
    n = 32
    r = radial(n)
    ring = np.exp(-((r - 0.82) / 0.07) ** 2) + 0.35 * np.exp(-((r - 0.62) / 0.05) ** 2) * i
    c = np.zeros((n, n, 4)); c[..., :3] = 240; c[..., 3] = np.clip(ring, 0, 1) * 200
    save(c, 'ripple_%d' % i)

# ------------------------------------------------------------------ sounds (mono, 44.1 kHz, ogg)
SR = 44100

def band(x, lo, hi, order=4):
    if lo <= 0: sos = butter(order, hi, 'low', fs=SR, output='sos')
    elif hi >= SR / 2: sos = butter(order, lo, 'high', fs=SR, output='sos')
    else: sos = butter(order, [lo, hi], 'band', fs=SR, output='sos')
    return sosfilt(sos, x)

def pink(n):
    w = rng.standard_normal(n)
    f = np.fft.rfft(w); k = np.arange(len(f)); k[0] = 1
    return np.fft.irfft(f / np.sqrt(k), n)

def brown(n):
    b = np.cumsum(rng.standard_normal(n)); b -= np.convolve(b, np.ones(4410) / 4410, 'same')
    return b

def norm(x, peak=0.9):
    return x / (np.max(np.abs(x)) + 1e-9) * peak

def loopify(x, fade):
    """crossfade the tail into the head so the loop has no seam"""
    body = x[:-fade].copy()
    t = np.linspace(0, 1, fade)
    body[:fade] = body[:fade] * np.sqrt(t) + x[-fade:] * np.sqrt(1 - t)
    return body

def bubbles(n, rate, f_lo, f_hi, dur=(0.015, 0.06), amp=1.0):
    out = np.zeros(n)
    count = int(rate * n / SR)
    for _ in range(count):
        s = rng.integers(0, n - 4000)
        d = rng.uniform(*dur)
        m = int(d * SR)
        t = np.arange(m) / SR
        f0 = rng.uniform(f_lo, f_hi)
        f = f0 * (1 + 2.5 * t / d * 0.4)          # bubbles rise in pitch as they shrink
        ph = 2 * np.pi * np.cumsum(f) / SR
        env = np.exp(-t / (d * 0.35)) * (1 - np.exp(-t / 0.002))
        out[s:s + m] += np.sin(ph) * env * rng.uniform(0.3, 1.0) * amp
    return out

def write(name, x):
    x = norm(x)
    with tempfile.TemporaryDirectory() as tmp:
        w = os.path.join(tmp, 'a.wav')
        wavfile.write(w, SR, (x * 32767).astype(np.int16))
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', w, '-c:a', 'libvorbis', '-q:a', '5', os.path.join(SND, name + '.ogg')], check=True)

# waterfall roar: rumble + roar + hiss, slowly breathing
n = SR * 9
rumble = band(brown(n), 30, 260) * 1.2
roar = band(pink(n), 250, 2600) * 1.0
hiss = band(rng.standard_normal(n), 2500, 9000) * 0.18
tt = np.arange(n) / SR
am = 1.0 + 0.12 * np.sin(2 * np.pi * 0.23 * tt) + 0.08 * np.sin(2 * np.pi * 0.61 * tt + 1.0)
splat = band(bubbles(n, 60, 300, 1400, amp=0.4), 200, 4000)
write('waterfall', loopify((norm(rumble) * 0.9 + norm(roar) * 1.0 + norm(hiss) * 0.35) * am + splat * 0.2, SR))

# stream: babbling brook = soft bed of noise + many bubble plinks
n = SR * 8
bed = band(pink(n), 300, 3500) * 0.25
plinks = bubbles(n, 38, 500, 1600, (0.02, 0.07))
gurgle = band(bubbles(n, 10, 180, 450, (0.05, 0.12)), 120, 1200)
write('stream', loopify(norm(bed) * 0.45 + norm(plinks) * 0.8 + norm(gurgle) * 0.35, SR))

# splashes
for i in range(3):
    n = int(SR * 0.9)
    t = np.arange(n) / SR
    hit = band(rng.standard_normal(n), 300, 6000) * np.exp(-t / (0.06 + 0.02 * i)) * (1 - np.exp(-t / 0.002))
    wash = band(pink(n), 200, 3000) * np.exp(-t / 0.22) * 0.5
    bub = bubbles(n, 40, 400, 1500, (0.02, 0.06), 0.6) * np.exp(-t / 0.35)
    write('splash_small_%d' % i, norm(hit) + norm(wash) * 0.6 + norm(bub) * 0.5)
for i in range(3):
    n = int(SR * 1.8)
    t = np.arange(n) / SR
    thump = band(brown(n), 40, 300) * np.exp(-t / 0.12) * (1 - np.exp(-t / 0.004))
    hit = band(rng.standard_normal(n), 250, 5000) * np.exp(-t / 0.12)
    wash = band(pink(n), 150, 2500) * np.exp(-t / 0.5)
    bub = bubbles(n, 70, 250, 1200, (0.03, 0.09), 0.7) * np.exp(-t / 0.6)
    rain = band(rng.standard_normal(n), 1500, 7000) * np.exp(-((t - 0.45) / 0.25) ** 2) * 0.3  # falling droplets
    write('splash_big_%d' % i, norm(thump) * 0.9 + norm(hit) + norm(wash) * 0.7 + norm(bub) * 0.45 + norm(rain) * 0.35)
print('assets done')
