"""Generates the tileable skin textures for Kaiju Titans (deterministic)."""
import os, numpy as np
from PIL import Image, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'kaiju', 'textures')
rng = np.random.default_rng(1954)

def noise(n, scale, seed=None):
    """Periodic (tileable) smooth noise via spectral filtering. scale ~ feature size in px."""
    r = np.random.default_rng(seed) if seed is not None else rng
    w = r.standard_normal((n, n))
    f = np.fft.fft2(w)
    ky = np.fft.fftfreq(n)[:, None]; kx = np.fft.fftfreq(n)[None, :]
    k = np.sqrt(kx * kx + ky * ky)
    filt = np.exp(-(k * scale) ** 2 * 4)
    out = np.real(np.fft.ifft2(f * filt))
    out -= out.min(); out /= out.max() + 1e-9
    return out

def fbm(n, base, octaves=5, seed=0):
    out = np.zeros((n, n)); amp = 1; tot = 0
    for o in range(octaves):
        out += amp * noise(n, base / (2 ** o), seed + o); tot += amp; amp *= 0.55
    return out / tot

def voronoi(n, cells, seed, stretch=(1.0, 1.0), jitter=1.0):
    """Toroidal Voronoi: returns F1, F2 distances (px) and cell id."""
    r = np.random.default_rng(seed)
    g = int(np.sqrt(cells))
    pts = []
    for i in range(g):
        for j in range(g):
            pts.append(((i + 0.5 + (r.random() - 0.5) * jitter) / g * n, (j + 0.5 + (r.random() - 0.5) * jitter) / g * n))
    pts = np.array(pts)
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    f1 = np.full((n, n), 1e9, np.float32); f2 = np.full((n, n), 1e9, np.float32); cid = np.zeros((n, n), np.int32)
    for idx, (py, px) in enumerate(pts):
        dy = np.abs(yy - py); dy = np.minimum(dy, n - dy)
        dx = np.abs(xx - px); dx = np.minimum(dx, n - dx)
        d = np.sqrt((dx * stretch[0]) ** 2 + (dy * stretch[1]) ** 2)
        m1 = d < f1
        f2 = np.where(m1, f1, np.minimum(f2, d))
        cid = np.where(m1, idx, cid)
        f1 = np.where(m1, d, f1)
    return f1, f2, cid, len(pts)

def lerp(a, b, t): return a + (b - a) * t[..., None]

def save(img, path, alpha=None):
    full = os.path.join(ROOT, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    arr = np.clip(img, 0, 255).astype(np.uint8)
    if alpha is not None:
        arr = np.dstack([arr, np.clip(alpha, 0, 255).astype(np.uint8)])
        Image.fromarray(arr, 'RGBA').save(full)
    else:
        Image.fromarray(arr, 'RGB').convert('RGBA').save(full)
    print('wrote', path)

def shade_from_height(h, strength=2.0):
    """Lambert shading from a height map (light from upper-left)."""
    gy, gx = np.gradient(h)
    nx, ny = -gx * strength, -gy * strength
    nz = np.ones_like(h)
    l = np.sqrt(nx * nx + ny * ny + nz * nz)
    lx, ly, lz = -0.5, -0.6, 0.62
    return np.clip((nx * lx + ny * ly + nz * lz) / l, 0, 1)

N = 512

def wrap_grad(h):
    # periodic gradient for tileable shading
    gx = (np.roll(h, -1, 1) - np.roll(h, 1, 1)) * 0.5
    gy = (np.roll(h, -1, 0) - np.roll(h, 1, 0)) * 0.5
    return gx, gy

def shade(h, strength):
    gx, gy = wrap_grad(h)
    nx, ny, nz = -gx * strength, -gy * strength, np.ones_like(h)
    l = np.sqrt(nx * nx + ny * ny + nz * nz)
    return np.clip((nx * -0.45 + ny * -0.6 + nz * 0.66) / l, 0, 1)

# ======================================================================= GODZILLA
def godzilla_skin():
    f1, f2, cid, n = voronoi(N, 26 * 26, 7, jitter=0.9)
    edge = np.clip((f2 - f1) / 6.0, 0, 1)                    # 0 in crevices
    bump = np.clip(1 - f1 / 14.0, 0, 1) ** 0.7               # domed cells
    f1b, f2b, cidb, nb = voronoi(N, 9 * 9, 11, jitter=1.0)   # large plates / folds
    big = np.clip((f2b - f1b) / 10.0, 0, 1)
    fine = fbm(N, 6, 4, 20)
    h = bump * 0.6 + edge * 0.5 + big * 0.35 + fine * 0.25
    s = shade(h * 9, 2.2)
    tone = np.random.default_rng(3).random(n)[cid] * 0.25 + noise(N, 60, 31) * 0.3
    dark = np.array([22, 23, 25.]); mid = np.array([58, 58, 58.]); light = np.array([112, 110, 104.])
    base = lerp(dark, mid, np.clip(edge * 0.9 + tone, 0, 1))
    col = base * (0.45 + 0.9 * s[..., None])
    col = lerp(col, light, np.clip((bump - 0.75) * 2.5, 0, 1) * s * 0.6)
    col = lerp(col, dark * 0.6, np.clip(1 - big * 1.4, 0, 1) * 0.7)  # deep fold lines
    return col

def godzilla_belly():
    yy = np.mgrid[0:N, 0:N][0].astype(float)
    bands = (yy / N * 12) % 1.0                              # 12 transverse plates per tile
    warp = noise(N, 40, 40) * 0.25
    bands = ((yy / N * 12) + warp) % 1.0
    ridge = np.clip(np.minimum(bands, 1 - bands) * 7, 0, 1)
    f1, f2, cid, n = voronoi(N, 32 * 32, 44, jitter=0.9)
    pebble = np.clip((f2 - f1) / 4.0, 0, 1)
    h = ridge * 0.8 + pebble * 0.3 + fbm(N, 8, 3, 50) * 0.2
    s = shade(h * 8, 2.0)
    a = np.array([40, 38, 36.]); b = np.array([92, 86, 78.])
    col = lerp(a, b, np.clip(ridge * 0.7 + pebble * 0.3 + noise(N, 50, 51) * 0.3, 0, 1)) * (0.5 + 0.8 * s[..., None])
    return col

def godzilla_plate():
    n = 256
    yy, xx = np.mgrid[0:n, 0:n].astype(float) / n
    streak = noise(n, 3, 60)
    streak = np.repeat(streak[:1, :], n, axis=0) * 0.6 + noise(n, 10, 61) * 0.4
    tip = 1 - yy                                              # v=0 is the tip
    h = streak + fbm(n, 6, 3, 62) * 0.5
    s = shade_from_height(h * 6, 2)
    dark = np.array([30, 31, 33.]); bone = np.array([150, 150, 148.])
    col = lerp(dark, bone, np.clip(tip ** 2.2 * 0.9 + streak * 0.2, 0, 1)) * (0.55 + 0.7 * s[..., None])
    # glow: branching veins, brighter towards the tips
    v = np.zeros((n, n))
    r = np.random.default_rng(63)
    for k in range(26):
        x = r.random() * n; y = n - 1.0; ang = -np.pi / 2 + (r.random() - 0.5) * 0.6
        width = 1.5 + r.random() * 2
        for step in range(400):
            ix, iy = int(x) % n, int(y)
            if iy < 0 or iy >= n: break
            v[max(0, iy - 2):iy + 2, max(0, ix - int(width)):ix + int(width) + 1] = 1
            ang += (r.random() - 0.5) * 0.5
            x += np.cos(ang) * 1.2; y += np.sin(ang) * 1.2
            if r.random() < 0.01: width *= 0.8
    vimg = Image.fromarray((v * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2.0))
    v = np.asarray(vimg).astype(float) / 255
    glow = np.clip(v * 1.6 + tip ** 3 * 0.55 + (1 - yy) * 0.1, 0, 1)
    gcol = np.dstack([glow * 120, glow * 230, glow * 255])
    return col, gcol

def solid(color, var=12, n=64, scale=6, seed=0):
    t = noise(n, scale, seed)
    return np.array(color, float) + (t[..., None] - 0.5) * var * 2

def godzilla():
    save(godzilla_skin(), 'entity/godzilla/skin.png')
    save(godzilla_belly(), 'entity/godzilla/belly.png')
    p, g = godzilla_plate()
    save(p, 'entity/godzilla/plate.png'); save(g, 'entity/godzilla/plate_glow.png')
    save(solid((222, 212, 188), 14, seed=70), 'entity/godzilla/teeth.png')
    m = solid((105, 28, 30), 20, seed=71)
    yy = np.mgrid[0:64, 0:64][0]
    m *= (0.75 + 0.25 * ((yy // 4) % 2))[..., None]
    save(m, 'entity/godzilla/mouth.png')
    save(solid((30, 28, 26), 10, seed=72), 'entity/godzilla/claw.png')
    eye = solid((200, 150, 60), 25, seed=73)
    save(eye, 'entity/godzilla/eye.png')
    save(np.array([255, 210, 140.]) * np.ones((64, 64, 1)) * (0.4 + 0.6 * noise(64, 6, 74)[..., None]), 'entity/godzilla/eye_glow.png')

# ======================================================================= GHIDORAH
def scales(n, rows, seed, overlap=0.65):
    """Overlapping imbricated scales: each scale is a rounded shield, lighter at its free edge."""
    h = np.zeros((n, n)); edge = np.ones((n, n)); tone = np.zeros((n, n))
    yy, xx = np.mgrid[0:n, 0:n].astype(float)
    cell = n / rows
    r = np.random.default_rng(seed)
    for row in range(rows + 1):
        for col in range(rows + 1):
            cx = (col + (0.5 if row % 2 else 0)) * cell + (r.random() - 0.5) * cell * 0.15
            cy = row * cell * overlap * 1.0 / overlap
            for oy in (-n, 0, n):
                for ox in (-n, 0, n):
                    dx = (xx - cx - ox) / (cell * 0.62); dy = (yy - cy - oy) / (cell * 0.75)
                    d = np.sqrt(dx * dx + dy * dy)
                    mask = (d < 1) & (dy > -0.6)
                    val = (1 - d) * 0.6 + (dy + 0.6) * 0.35
                    upd = mask & (cy + oy >= 0)
                    h = np.where(upd, val, h)
                    edge = np.where(upd, np.clip((1 - d) * 5, 0, 1), edge)
                    tone = np.where(upd, r.random(), tone)
    return h, edge, tone

def ghidorah_skin():
    h, edge, tone = scales(N, 16, 80)
    fine = fbm(N, 5, 3, 81)
    s = shade(h * 10 + fine * 2, 2.0)
    gap = np.array([70, 48, 18.]); gold = np.array([176, 132, 52.]); bright = np.array([236, 200, 112.])
    col = lerp(gap, gold, np.clip(edge * 1.2, 0, 1))
    col = lerp(col, bright, np.clip(h * 0.9 + tone * 0.25 - 0.25, 0, 1))
    col *= (0.5 + 0.75 * s[..., None])
    col = lerp(col, col * np.array([0.8, 0.85, 0.7]), noise(N, 80, 82))
    return col

def ghidorah_belly():
    yy = np.mgrid[0:N, 0:N][0].astype(float)
    bands = ((yy / N * 10) + noise(N, 50, 90) * 0.2) % 1.0
    ridge = np.clip(np.minimum(bands, 1 - bands) * 6, 0, 1)
    h = ridge + fbm(N, 6, 3, 91) * 0.25
    s = shade(h * 8, 2.0)
    col = lerp(np.array([110, 80, 35.]), np.array([228, 196, 120.]), np.clip(ridge * 0.85 + noise(N, 40, 92) * 0.2, 0, 1)) * (0.55 + 0.7 * s[..., None])
    return col

def ghidorah_wing():
    n = 512
    yy, xx = np.mgrid[0:n, 0:n].astype(float) / n
    mott = fbm(n, 30, 5, 100)
    veins = np.zeros((n, n))
    r = np.random.default_rng(101)
    for k in range(60):
        x = r.random() * n; y = r.random() * n; ang = r.random() * np.pi * 2; w = 1 + r.random() * 1.5
        for step in range(300):
            ix, iy = int(x) % n, int(y) % n
            veins[iy, ix] = max(veins[iy, ix], w)
            ang += (r.random() - 0.5) * 0.35; x += np.cos(ang) * 1.5; y += np.sin(ang) * 1.5
    veins = np.asarray(Image.fromarray((np.clip(veins / 2.5, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.2))).astype(float) / 255
    wr = noise(n, 4, 102)
    base = lerp(np.array([150, 108, 52.]), np.array([214, 172, 102.]), np.clip(mott * 1.1, 0, 1))
    base *= (0.85 + 0.25 * wr)[..., None]
    col = lerp(base, np.array([95, 62, 28.]), np.clip(veins * 1.4, 0, 1))
    return col

def ghidorah_horn():
    n = 128
    yy = np.mgrid[0:n, 0:n][0].astype(float) / n
    rings = 0.5 + 0.5 * np.sin(yy * 60 + noise(n, 8, 110) * 3)
    col = lerp(np.array([45, 32, 18.]), np.array([120, 92, 52.]), np.clip(rings * 0.5 + noise(n, 6, 111) * 0.5, 0, 1))
    return col

def crackle():
    n = 512
    v = np.zeros((n, n))
    r = np.random.default_rng(120)
    for k in range(70):
        x = r.random() * n; y = r.random() * n; ang = r.random() * np.pi * 2
        for step in range(160):
            ix, iy = int(x) % n, int(y) % n
            v[iy, ix] = 1
            if r.random() < 0.25: ang += (r.random() - 0.5) * 1.6
            x += np.cos(ang) * 2; y += np.sin(ang) * 2
    img = Image.fromarray((v * 255).astype(np.uint8))
    core = np.asarray(img.filter(ImageFilter.MaxFilter(3))).astype(float) / 255
    haze = np.asarray(img.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.GaussianBlur(4))).astype(float) / 255
    g = np.clip(core * 0.9 + haze * 1.5, 0, 1)
    return np.dstack([g * 255, g * 225, g * 120])

def ghidorah():
    save(ghidorah_skin(), 'entity/ghidorah/skin.png')
    save(ghidorah_belly(), 'entity/ghidorah/belly.png')
    save(ghidorah_wing(), 'entity/ghidorah/wing.png')
    save(ghidorah_horn(), 'entity/ghidorah/horn.png')
    save(solid((225, 214, 186), 14, seed=130), 'entity/ghidorah/teeth.png')
    save(solid((120, 30, 25), 25, seed=131), 'entity/ghidorah/mouth.png')
    save(solid((230, 90, 30), 30, seed=132), 'entity/ghidorah/eye.png')
    save(np.array([255, 150, 60.]) * np.ones((64, 64, 1)) * (0.6 + 0.4 * noise(64, 6, 133)[..., None]), 'entity/ghidorah/eye_glow.png')
    save(crackle(), 'entity/ghidorah/crackle.png')
    w = noise(64, 5, 134)
    save(np.dstack([255 * (0.6 + 0.4 * w), 170 * (0.5 + 0.5 * w), 60 * w]), 'entity/ghidorah/wound.png')

if __name__ == '__main__':
    godzilla()
    ghidorah()
