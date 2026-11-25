"""The soft blip a line of dialogue makes as it types itself out (talk_blip0..3). Pitch is set per speaker in game."""
import json, os, subprocess, tempfile, wave
import numpy as np

ROOT = os.path.join(os.path.dirname(__file__), "..", "common", "src", "main", "resources", "assets", "bloodsea")
SR = 44100


def blip(f0, vowel, seed):
    rng = np.random.default_rng(seed)
    n = int(SR * 0.065)
    t = np.arange(n) / SR
    # a little glide, like a syllable
    f = f0 * (1 + 0.06 * np.exp(-t * 60)) * (1 - 0.04 * t / t[-1])
    ph = 2 * np.pi * np.cumsum(f) / SR
    # a voiced buzz through two soft formants: it reads as a murmured syllable, not a beep
    src = sum(np.sin(k * ph) / k ** 1.2 for k in range(1, 14))
    out = np.zeros(n)
    for fc, bw, g in ((vowel[0], 90, 1.0), (vowel[1], 140, 0.5)):
        w = np.exp(-((np.arange(1, 14)[:, None] * f[None, :] - fc) / bw) ** 2)
        out += g * sum(w[k - 1] * np.sin(k * ph) for k in range(1, 14))
    out = 0.6 * out + 0.15 * src
    env = np.minimum(1, t / 0.006) * np.exp(-t * 38)
    out = out * env
    out += rng.normal(0, 0.004, n) * env
    return out / np.abs(out).max() * 0.55


def write_ogg(x, path):
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tf:
        w = wave.open(tf.name, "wb")
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((np.clip(x, -1, 1) * 32767).astype(np.int16).tobytes())
        w.close()
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tf.name, "-c:a", "libvorbis", "-q:a", "4", path], check=True)
        os.unlink(tf.name)


if __name__ == "__main__":
    vowels = [(700, 1200), (500, 1700), (400, 2000), (600, 1000)]
    for i, v in enumerate(vowels):
        write_ogg(blip(260 + i * 9, v, 300 + i), os.path.join(ROOT, "sounds", "talk_blip%d.ogg" % i))
    p = os.path.join(ROOT, "sounds.json")
    d = json.load(open(p))
    d["talk_blip"] = {"sounds": ["bloodsea:talk_blip%d" % i for i in range(4)]}
    json.dump(d, open(p, "w"), indent=2)
    print("ok")
