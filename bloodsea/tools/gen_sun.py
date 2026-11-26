"""The white sun the Watcher hides behind (white_sun.png, exactly the eye's size), and the seam it opens along (sun_seam.png)."""
import os
import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "common", "src", "main", "resources", "assets", "bloodsea", "textures", "environment")
N = 512


def white_sun():
    y, x = np.mgrid[0:N, 0:N]
    r = np.hypot(x - N / 2 + 0.5, y - N / 2 + 0.5) / (N / 2)
    rng = np.random.default_rng(7)
    # a plain white disc: bright core, a little limb darkening, a soft rim; faint granulation so it is not flat
    noise = np.zeros((N, N))
    for s in (64, 32, 16):
        g = rng.normal(0, 1, (N // s + 2, N // s + 2))
        g = np.kron(g, np.ones((s, s)))[:N, :N]
        noise += g / (64 / s)
    core = np.clip(1 - r, 0, 1) ** 0.25
    lum = 0.84 + 0.16 * core + 0.012 * noise
    col = np.stack([lum, lum * 0.995, lum * 0.975], -1)
    a = np.clip((0.985 - r) / 0.03, 0, 1)
    img = np.dstack([np.clip(col, 0, 1) * 255, a * 255]).astype(np.uint8)
    Image.fromarray(img, "RGBA").save(os.path.join(OUT, "white_sun.png"))


def seam():
    # a thin dark line, soft at the ends: the lid edge, drawn where the white parts
    w, h = 512, 32
    y, x = np.mgrid[0:h, 0:w]
    v = np.exp(-((y - h / 2 + 0.5) / 3.2) ** 2)
    u = np.clip(np.sin(np.pi * (x + 0.5) / w), 0, 1) ** 0.6
    a = v * u
    img = np.dstack([np.full((h, w), 60), np.full((h, w), 8), np.full((h, w), 10), a * 235]).astype(np.uint8)
    Image.fromarray(img, "RGBA").save(os.path.join(OUT, "sun_seam.png"))


if __name__ == "__main__":
    white_sun()
    seam()
    print("ok")
