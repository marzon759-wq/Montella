"""Vertical trailer: picture (graded, punched-in, captioned, flashed and shaken on the hits) and sound (score bed,
impacts, whooshes, the game's own sounds) built frame by frame, then muxed. Usage: compose.py en|tr"""
import os
import subprocess
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

LANG = sys.argv[1] if len(sys.argv) > 1 else "en"
T = os.path.dirname(os.path.abspath(__file__))
A = "/home/user/Montella/bloodsea/common/src/main/resources/assets/bloodsea"
W, H, FPS = 1080, 1920, 30
SR = 44100
FONT = "/usr/share/fonts/opentype/inter/Inter-ExtraBold.otf"
SERIF = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"

# (segment, start in the edit, seconds)
SHOTS = [("eye", 0.0, 3.3), ("raft", 3.3, 2.5), ("hands", 5.8, 2.0), ("pip", 7.8, 2.0), ("pet", 9.8, 1.8),
         ("rule", 11.6, 2.4), ("moon", 14.0, 3.4), ("gath", 17.4, 3.5), ("gath2", 20.9, 1.1), ("hand", 22.0, 3.0),
         ("sob", 25.0, 1.6)]
END0, END1 = 26.6, 31.2
TOTAL = END1

CAP = {
    "en": [(0.10, 1.45, "DON'T LOOK\nAT THE SUN", None), (1.70, 3.25, "...IT LOOKS BACK.", None),
           (3.40, 5.75, "YOU WAKE UP\nON A RAFT", None), (5.85, 7.75, "IN AN OCEAN\nOF BLOOD", "BLOOD"),
           (7.85, 11.55, "A CAT YOU'VE\nNEVER SEEN", None), (11.65, 13.95, "NEVER TOUCH\nTHE BLOOD", "BLOOD"),
           (14.05, 17.35, "SURVIVE THE\nBLOOD MOON", "BLOOD MOON"), (17.45, 21.95, "SOMETHING IS\nDOWN THERE", None),
           (22.05, 26.5, "AND IT TAKES\nWHAT YOU LOVE", None)],
    "tr": [(0.10, 1.45, "GÜNEŞE\nBAKMA", None), (1.70, 3.25, "...O DA SANA BAKAR.", None),
           (3.40, 5.75, "BİR SALDA\nUYANIYORSUN", None), (5.85, 7.75, "KAN DENİZİNİN\nORTASINDA", "KAN"),
           (7.85, 11.55, "HİÇ GÖRMEDİĞİN\nBİR KEDİ", None), (11.65, 13.95, "KANA ASLA\nDOKUNMA", "KANA"),
           (14.05, 17.35, "KANLI AY'DAN\nSAĞ ÇIK", "KANLI AY'DAN"), (17.45, 21.95, "AŞAĞIDA\nBİR ŞEY VAR", None),
           (22.05, 26.5, "VE SEVDİĞİNİ\nALIR", None)],
}
END_TXT = {"en": ("A MINECRAFT HORROR STORY", "FREE ON CURSEFORGE", "Forge 1.20.1  ·  1.21.1"),
           "tr": ("MINECRAFT HİKÂYELİ KORKU MODU", "CURSEFORGE'DA ÜCRETSİZ", "Forge 1.20.1  ·  1.21.1")}
# hits: time -> strength (a flash and a shake on the picture, an impact in the sound)
HITS = [(1.45, 1.0), (5.8, 0.4), (11.6, 0.3), (13.1, 0.7), (14.0, 0.5), (17.4, 0.6), (22.25, 1.0), (26.6, 1.0)]

rng = np.random.default_rng(5)


# ------------------------------------------------------------------ picture
def load(seg, i):
    d = os.path.join(T, "f", seg)
    n = len(os.listdir(d))
    return Image.open(os.path.join(d, "%04d.jpg" % (min(n, max(1, i + 1))))).convert("RGB")


VIGNETTE = None


def grade(im, k_dark=0.0):
    global VIGNETTE
    a = np.asarray(im).astype(np.float32) / 255
    # contrast and a push to deep red, lifted blacks a touch
    a = np.clip((a - 0.5) * 1.18 + 0.5, 0, 1)
    lum = a.mean(-1, keepdims=True)
    a = np.clip(lum + (a - lum) * 1.22, 0, 1)
    a[..., 0] = np.clip(a[..., 0] * 1.04, 0, 1)
    a[..., 2] *= 0.92
    if VIGNETTE is None:
        y, x = np.mgrid[0:H, 0:W]
        r = np.hypot((x - W / 2) / (W / 2), (y - H / 2) / (H / 2))
        VIGNETTE = (1 - 0.55 * np.clip(r - 0.45, 0, 1) ** 1.4)[..., None]
    a = a * VIGNETTE * (1 - k_dark)
    # film grain
    a = np.clip(a + rng.normal(0, 0.018, (H, W, 1)), 0, 1)
    return Image.fromarray((a * 255).astype(np.uint8))


def punch(im, k, shake=(0, 0)):
    """a slow push in (k from 0 to 1 across the shot), plus a shake offset"""
    s = 1.0 + 0.07 * k
    w, h = int(W / s), int(H / s)
    x0 = (W - w) // 2 + int(shake[0]); y0 = (H - h) // 2 + int(shake[1])
    x0 = max(0, min(W - w, x0)); y0 = max(0, min(H - h, y0))
    return im.crop((x0, y0, x0 + w, y0 + h)).resize((W, H), Image.BILINEAR)


def text_layer(txt, accent, size, y_center, alpha, scale):
    lay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    font = ImageFont.truetype(FONT, int(size * scale))
    d = ImageDraw.Draw(lay)
    lines = txt.split("\n")
    lh = int(size * scale * 1.08)
    y = y_center - lh * len(lines) // 2
    for ln in lines:
        tw = d.textlength(ln, font=font)
        x = (W - tw) / 2
        # word by word so the accent word can be red
        cx = x
        for wi, word in enumerate(ln.split(" ")):
            red = accent is not None and word.strip(".,") in accent.split(" ")
            col = (214, 20, 24, int(255 * alpha)) if red else (255, 250, 244, int(255 * alpha))
            d.text((cx, y), word, font=font, fill=col, stroke_width=max(3, int(size * scale / 11)), stroke_fill=(0, 0, 0, int(235 * alpha)))
            cx += d.textlength(word + " ", font=font)
        y += lh
    # soft shadow under it
    sh = lay.split()[3].filter(ImageFilter.GaussianBlur(14)).point(lambda v: int(v * 0.6))
    base = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    base.putalpha(sh)
    return Image.alpha_composite(base, lay)


def caption_at(t):
    for (a, b, txt, acc) in CAP[LANG]:
        if a <= t < b:
            k = (t - a)
            pop = min(1.0, k / 0.16)
            out = min(1.0, (b - t) / 0.12)
            scale = 1.0 + 0.14 * (1 - pop) ** 2
            return text_layer(txt, acc, 92, int(H * 0.25), min(pop, out), scale)
    return None


def end_card(t):
    """the eye opens in the dark, then the title bleeds in, then the call"""
    k = t - END0
    im = Image.new("RGB", (W, H), (6, 0, 1))
    a = np.asarray(im).astype(np.float32)
    y = np.linspace(0, 1, H)[:, None]
    glow = np.exp(-((y - 0.30) / 0.22) ** 2) * min(1, k / 0.8)
    a[..., 0] += 70 * glow
    im = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))
    env = A + "/textures/environment/"
    R = 300
    cx, cy = W // 2, int(H * 0.30)
    sun = Image.open(env + "white_sun.png").convert("RGBA").resize((2 * R, 2 * R))
    halo = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    hd = ImageDraw.Draw(halo)
    hd.ellipse([cx - R * 1.5, cy - R * 1.5, cx + R * 1.5, cy + R * 1.5], fill=(255, 220, 200, int(70 * min(1, k / 0.6))))
    im = Image.alpha_composite(im.convert("RGBA"), halo.filter(ImageFilter.GaussianBlur(60)))
    s_alpha = min(1, k / 0.5)
    sun.putalpha(sun.split()[3].point(lambda v: int(v * s_alpha)))
    im.alpha_composite(sun, (cx - R, cy - R))
    o = np.clip((k - 0.7) / 0.6, 0, 1)
    o = o * o * (3 - 2 * o)
    if o > 0.01:
        eye = Image.open(env + "watcher_eye.png").convert("RGBA").resize((2 * R, 2 * R))
        h = max(1, int(R * o))
        band = eye.crop((0, R - h, 2 * R, R + h))
        im.alpha_composite(band, (cx - R, cy - h))
        pup = Image.open(env + "watcher_pupil.png").convert("RGBA").resize((int(R * 0.42), max(2, int(min(R * 0.62, h * 0.92) * 2))))
        im.alpha_composite(pup, (cx - pup.width // 2, cy - pup.height // 2))
        d = ImageDraw.Draw(im)
        cw = int(np.sqrt(max(0, R * R - h * h)))
        d.rectangle([cx - cw, cy - h - 2, cx + cw, cy - h + 2], fill=(50, 6, 8, 255))
        d.rectangle([cx - cw, cy + h - 2, cx + cw, cy + h + 2], fill=(50, 6, 8, 255))
    # the title
    la = np.clip((k - 1.2) / 0.5, 0, 1)
    if la > 0:
        logo = Image.open(A + "/textures/gui/menu_logo.png").convert("RGBA")
        lw = int(W * 0.94)
        logo = logo.resize((lw, int(lw * 300 / 1024)))
        # the drips run as it appears
        reveal = int(logo.height * min(1, 0.55 + 0.45 * np.clip((k - 1.2) / 1.4, 0, 1)))
        logo = logo.crop((0, 0, lw, reveal))
        logo.putalpha(logo.split()[3].point(lambda v: int(v * la)))
        im.alpha_composite(logo, ((W - lw) // 2, int(H * 0.53)))
    sub, cta, ver = END_TXT[LANG]
    d = ImageDraw.Draw(im)
    for (start, txt, size, yy, col) in ((1.8, sub, 50, 0.53 + 0.17, (226, 200, 190)), (2.4, cta, 74, 0.53 + 0.24, (255, 255, 255)),
                                        (2.7, ver, 40, 0.53 + 0.30, (170, 150, 140))):
        al = np.clip((k - start) / 0.35, 0, 1)
        if al <= 0:
            continue
        f = ImageFont.truetype(FONT, size)
        tw = d.textlength(txt, font=f)
        lay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        ImageDraw.Draw(lay).text(((W - tw) / 2, int(H * yy)), txt, font=f, fill=col + (int(255 * al),),
                                 stroke_width=4, stroke_fill=(0, 0, 0, int(220 * al)))
        im = Image.alpha_composite(im, lay)
    return im.convert("RGB")


def frame(i):
    t = i / FPS
    if t >= END0:
        im = end_card(t)
    else:
        seg, s0, dur = [s for s in SHOTS if s[1] <= t < s[1] + s[2]][0]
        local = t - s0
        im = load(seg, int(local * FPS))
        shake = (0, 0)
        for (ht, st) in HITS:
            if 0 <= t - ht < 0.35:
                k = 1 - (t - ht) / 0.35
                shake = (rng.normal(0, 22 * st * k), rng.normal(0, 22 * st * k))
        im = punch(im, local / dur, shake)
        # a dip to dark at each cut, very short
        dark = 0.0
        if local < 0.06: dark = 0.6 * (1 - local / 0.06)
        im = grade(im, dark)
    # flashes on the hits
    a = None
    for (ht, st) in HITS:
        if 0 <= t - ht < 0.16:
            k = (1 - (t - ht) / 0.16) * st
            a = np.asarray(im).astype(np.float32)
            a = a + (255 - a) * 0.85 * k
            im = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))
    cap = caption_at(t)
    if cap is not None:
        im = Image.alpha_composite(im.convert("RGBA"), cap).convert("RGB")
    # fade out at the very end
    if t > TOTAL - 0.35:
        a = np.asarray(im).astype(np.float32) * max(0, (TOTAL - t) / 0.35)
        im = Image.fromarray(a.astype(np.uint8))
    return im


# ------------------------------------------------------------------ sound
def ogg(name, gain=1.0):
    p = name if name.startswith("/") else os.path.join(A, "sounds", name + ".ogg")
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", p, "-ac", "2", "-ar", str(SR), "-f", "f32le", "-"], capture_output=True).stdout
    return np.frombuffer(raw, np.float32).reshape(-1, 2) * gain


def place(mix, x, at):
    i = int(at * SR)
    e = min(len(mix), i + len(x))
    if e > i:
        mix[i:e] += x[:e - i]


def impact(strength=1.0):
    n = int(2.2 * SR)
    t = np.arange(n) / SR
    boom = np.sin(2 * np.pi * (38 + 60 * np.exp(-t * 9)) * t) * np.exp(-t * 2.2)
    crack = rng.normal(0, 1, n) * np.exp(-t * 28)
    from scipy import signal
    b, a = signal.butter(2, 2500 / (SR / 2))
    crack = signal.lfilter(b, a, crack)
    x = (boom * 0.9 + crack * 0.45) * strength
    return np.stack([x, x], -1).astype(np.float32)


def whoosh(d=0.55, rise=True):
    n = int(d * SR)
    t = np.arange(n) / SR
    from scipy import signal
    noise = rng.normal(0, 1, n)
    out = np.zeros(n)
    block = 1024
    for s in range(0, n, block):
        k = s / n
        c = 300 + (2800 if rise else -0) * (k if rise else 1 - k)
        b, a = signal.butter(2, [max(80, c * 0.6) / (SR / 2), min(SR / 2 - 100, c * 1.6) / (SR / 2)], btype="band")
        out[s:s + block] = signal.lfilter(b, a, noise[s:s + block + 0])[:len(out[s:s + block])]
    env = np.sin(np.pi * np.clip(t / d, 0, 1)) ** 2
    x = out * env * 0.35
    return np.stack([x * 0.9, x], -1).astype(np.float32)


def sound():
    n = int(TOTAL * SR) + SR
    mix = np.zeros((n, 2), np.float32)
    bed = ogg(A + "/sounds/music/music_dread.ogg")
    bed = bed[int(26 * SR): int(26 * SR) + int(END0 * SR)]
    k = np.ones(len(bed)); f = int(0.4 * SR); k[:f] = np.linspace(0, 1, f); k[-int(0.2 * SR):] = np.linspace(1, 0, int(0.2 * SR))
    place(mix, bed * k[:, None] * 0.8, 0)
    # the drift's lullaby under the end card, broken
    drift = ogg(A + "/sounds/music/music_night.ogg")[int(70 * SR): int(70 * SR) + int((TOTAL - END0) * SR)]
    kk = np.linspace(0, 1, len(drift)) ** 0.5
    place(mix, drift * kk[:, None] * 0.9, END0 + 0.2)
    for (ht, st) in HITS:
        place(mix, impact(0.5 + 0.6 * st), ht)
    for (seg, s0, dur) in SHOTS[1:]:
        place(mix, whoosh(0.45), max(0, s0 - 0.3))
    place(mix, ogg("watcher_stare", 0.9), 1.3)
    place(mix, ogg("blood_splash", 0.5), 3.4)
    place(mix, ogg("pip_meow1", 1.3), 7.95)
    place(mix, ogg("pip_purr0", 1.1), 9.8)
    place(mix, ogg("blood_splash", 1.2), 13.05)
    place(mix, ogg("pip_hiss0", 1.0), 13.25)
    place(mix, ogg("lev_heartbeat", 1.2), 14.0)
    place(mix, ogg("gatherer_assemble", 1.2), 17.4)
    place(mix, ogg("gatherer_roar", 1.2), 19.6)
    place(mix, ogg("caller_lunge", 1.3), 22.1)
    place(mix, ogg("pip_scream0", 1.4), 22.4)
    place(mix, ogg("blood_splash", 1.1), 24.3)
    place(mix, ogg("heartbeat_near", 1.0), 25.0)
    place(mix, ogg("watcher_stare", 1.0), END0 + 0.6)
    mix = mix[: int(TOTAL * SR)]
    # master: a gentle glue and a ceiling
    peak = np.abs(mix).max()
    mix = np.tanh(mix / peak * 1.6) / np.tanh(1.6) * 0.92
    wav = os.path.join(T, "trailer_%s.wav" % LANG)
    import wave
    w = wave.open(wav, "wb"); w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes((mix * 32767).astype(np.int16).tobytes()); w.close()
    return wav


if __name__ == "__main__":
    wav = sound()
    out = os.path.join(T, "BloodSea_TikTok_%s.mp4" % LANG)
    p = subprocess.Popen(["ffmpeg", "-v", "error", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", "%dx%d" % (W, H), "-r", str(FPS), "-i", "-",
                          "-i", wav, "-c:v", "libx264", "-preset", "slow", "-crf", "21", "-pix_fmt", "yuv420p", "-profile:v", "high",
                          "-c:a", "aac", "-b:a", "256k", "-shortest", "-movflags", "+faststart", out], stdin=subprocess.PIPE)
    N = int(TOTAL * FPS)
    for i in range(N):
        p.stdin.write(frame(i).tobytes())
        if i % 150 == 0:
            print(i, "/", N, flush=True)
    p.stdin.close()
    p.wait()
    print("done", out)
