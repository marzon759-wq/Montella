"""The Blood Sea's score, synthesised: three seamless loops (music_drift, music_night, music_dread).

drift  - day on the open blood: a low drone that breathes, slow minor chords that never resolve, wind,
         and a music-box lullaby that is a little out of tune, as if remembered wrongly.
night  - darker and emptier: the drone a fifth lower, glass harmonics beating against each other, distant
         bowed metal, the lullaby slower and further away.
dread  - when the eye is open or something is close: a heartbeat under everything, a cluster that climbs
         by itself, scraping, and the lullaby broken into single notes.
"""
import os
import subprocess
import tempfile
import wave

import numpy as np
from scipy import signal

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "common", "src", "main", "resources", "assets", "bloodsea", "sounds", "music")
rng = np.random.default_rng(1234)


def hz(note):
    """'D3', 'Bb2', 'C#4' -> Hz"""
    names = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}
    n = names[note[0]]
    i = 1
    if note[i:i + 1] == "#": n += 1; i += 1
    elif note[i:i + 1] == "b": n -= 1; i += 1
    octave = int(note[i:])
    return 440.0 * 2 ** ((n + 12 * (octave + 1) - 69) / 12)


def lfo(n, rate, depth=1.0, phase=None):
    t = np.arange(n) / SR
    ph = rng.uniform(0, 2 * np.pi) if phase is None else phase
    return depth * np.sin(2 * np.pi * rate * t + ph)


def smooth_noise(n, rate):
    """a slow random curve in [-1, 1]"""
    k = max(4, int(n / SR * rate) + 4)
    pts = rng.uniform(-1, 1, k)
    x = np.linspace(0, k - 3, n)
    i = x.astype(int)
    f = x - i
    f = f * f * (3 - 2 * f)
    return pts[i] * (1 - f) + pts[i + 1] * f


def env_adsr(n, a, r):
    e = np.ones(n)
    na, nr = int(a * SR), int(r * SR)
    na, nr = min(na, n // 2), min(nr, n // 2)
    e[:na] = np.sin(np.linspace(0, np.pi / 2, na)) ** 2
    e[n - nr:] = np.cos(np.linspace(0, np.pi / 2, nr)) ** 2
    return e


def lowpass(x, fc, order=2):
    b, a = signal.butter(order, fc / (SR / 2))
    return signal.lfilter(b, a, x)


def bandpass(x, lo, hi, order=2):
    b, a = signal.butter(order, [lo / (SR / 2), hi / (SR / 2)], btype="band")
    return signal.lfilter(b, a, x)


def reverb_ir(seconds, damp, seed):
    r = np.random.default_rng(seed)
    n = int(seconds * SR)
    t = np.arange(n) / SR
    ir = r.normal(0, 1, n) * np.exp(-t * 6.9 / seconds)
    # darker as it decays
    dark = lowpass(ir, damp)
    k = np.clip(t / (seconds * 0.5), 0, 1)
    ir = ir * (1 - k) * 0.5 + dark * (0.5 + 0.5 * k)
    ir[: int(0.012 * SR)] *= np.linspace(0, 1, int(0.012 * SR))
    return ir / np.sqrt((ir ** 2).sum())


def reverb(x, seconds=6.0, damp=2500, wet=0.5, seed=0):
    out = []
    for ch in range(2):
        ir = reverb_ir(seconds, damp, seed * 10 + ch)
        w = signal.fftconvolve(x[ch], ir)[: x.shape[1]]
        out.append(x[ch] * (1 - wet) + w * wet * 2.2)
    return np.array(out)


def pan(m, p):
    """mono -> stereo, p in [-1, 1]"""
    return np.array([m * np.sqrt((1 - p) / 2), m * np.sqrt((1 + p) / 2)])


# ------------------------------------------------------------------ voices

def drone(n, f0, bright=1.0, partials=28):
    """a bowed, breathing drone: additive saw with every partial swelling on its own"""
    t = np.arange(n) / SR
    out = np.zeros(n)
    for det in (-0.11, 0.0, 0.13):
        f = f0 + det
        vib = 1 + 0.0015 * smooth_noise(n, 0.2)
        ph = 2 * np.pi * np.cumsum(f * vib) / SR
        for k in range(1, partials + 1):
            amp = (1 / k) * (0.55 + 0.45 * smooth_noise(n, 0.07 + 0.01 * k)) * np.exp(-k / (9 * bright))
            out += amp * np.sin(k * ph + rng.uniform(0, 6.28))
    out /= np.abs(out).max()
    return out * (0.75 + 0.25 * smooth_noise(n, 0.05))


def pad_note(n, f, attack=5.0, release=6.0, warmth=1200):
    t = np.arange(n) / SR
    vib = 1 + 0.003 * np.sin(2 * np.pi * 4.6 * t + rng.uniform(0, 6)) * np.clip(t / 3, 0, 1)
    out = np.zeros(n)
    for det in (-0.22, 0.2):
        ph = 2 * np.pi * np.cumsum((f + det) * vib) / SR
        for k, a in ((1, 1.0), (2, 0.45), (3, 0.3), (4, 0.16), (5, 0.1), (6, 0.06)):
            out += a * np.sin(k * ph)
    out = lowpass(out, warmth)
    return out * env_adsr(n, attack, release)


def chords(total, seq, each, octave_shift=0, level=1.0, warmth=1200, seed=0):
    out = np.zeros((2, total))
    pos = 0
    i = 0
    overlap = int(4 * SR)
    while pos < total:
        notes = seq[i % len(seq)]
        n = min(int(each * SR) + overlap, total - pos)
        for j, nt in enumerate(notes):
            f = hz(nt) * 2 ** octave_shift
            out[:, pos:pos + n] += pan(pad_note(n, f, warmth=warmth), (j / max(1, len(notes) - 1)) * 1.2 - 0.6)
        pos += int(each * SR)
        i += 1
    return out * level / max(1e-9, np.abs(out).max())


def bell(f, dur, detune_cents):
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = f * 2 ** (detune_cents / 1200)
    out = (np.sin(2 * np.pi * f * t) * np.exp(-t * 1.6)
           + 0.5 * np.sin(2 * np.pi * f * 2.76 * t) * np.exp(-t * 4.0)
           + 0.25 * np.sin(2 * np.pi * f * 5.40 * t) * np.exp(-t * 7.5)
           + 0.12 * np.sin(2 * np.pi * f * 8.93 * t) * np.exp(-t * 12.0))
    click = np.exp(-t * 400) * rng.normal(0, 0.2, n)
    out = out + click
    out[: 40] *= np.linspace(0, 1, 40)
    return out


def music_box(total, melody, step, start, stop, flat=(-35, -8), level=1.0, wow=0.004):
    """a lullaby on a slightly broken music box; melody: list of (note or None, beats)"""
    out = np.zeros(total)
    pos = int(start * SR)
    i = 0
    while pos < int(stop * SR):
        nt, beats = melody[i % len(melody)]
        if nt:
            b = bell(hz(nt), 3.5, rng.uniform(*flat))
            e = min(len(b), total - pos)
            out[pos:pos + e] += b[:e] * rng.uniform(0.7, 1.0)
        pos += int(beats * step * SR * rng.uniform(0.97, 1.06))
        i += 1
    # tape wow: the whole box drifts in pitch
    t = np.arange(total) / SR
    warp = t + np.cumsum(wow * np.sin(2 * np.pi * 0.35 * t) + 0.5 * wow * smooth_noise(total, 0.6)) / SR
    out = np.interp(np.clip(warp, 0, t[-1]), t, out)
    return out * level / max(1e-9, np.abs(out).max())


def wind(total, lo, hi, level):
    out = np.zeros(total)
    block = SR * 2
    noise = rng.normal(0, 1, total + block)
    cen = (smooth_noise(total, 0.08) * 0.5 + 0.5)
    for s in range(0, total, block):
        c = lo + (hi - lo) * cen[min(s, total - 1)]
        seg = bandpass(noise[s:s + block + 2048], c * 0.7, c * 1.4)[1024:1024 + block]
        out[s:s + block] = seg[: len(out[s:s + block])]
    out = out * (0.4 + 0.6 * (smooth_noise(total, 0.05) * 0.5 + 0.5) ** 2)
    return out * level / max(1e-9, np.abs(out).max())


def glass(total, freqs, level):
    t = np.arange(total) / SR
    out = np.zeros(total)
    for f in freqs:
        for det in (0, 0.7 + rng.uniform(0, 0.8)):
            a = np.clip(smooth_noise(total, 0.03 + rng.uniform(0, 0.03)), 0, 1) ** 2
            out += a * np.sin(2 * np.pi * (f + det) * t + rng.uniform(0, 6))
    return out * level / max(1e-9, np.abs(out).max())


def bowed_metal(total, count, level):
    out = np.zeros(total)
    for _ in range(count):
        dur = rng.uniform(5, 9)
        n = int(dur * SR)
        s = int(rng.uniform(0, total / SR - dur) * SR)
        t = np.arange(n) / SR
        f0 = rng.uniform(180, 420)
        x = rng.normal(0, 1, n)
        y = np.zeros(n)
        for k, r in ((1, 1.0), (2.41, 0.7), (3.87, 0.5), (5.2, 0.35), (6.9, 0.2)):
            y += r * bandpass(x, f0 * k * 0.995, f0 * k * 1.005, 2)
        y *= np.sin(np.pi * t / dur) ** 1.5
        out[s:s + n] += y / max(1e-9, np.abs(y).max())
    return out * level / max(1e-9, np.abs(out).max())


def heartbeat(total, bpm, level, start=0.0):
    out = np.zeros(total)
    period = 60 / bpm
    t0 = start
    n = int(0.35 * SR)
    t = np.arange(n) / SR
    thump = np.sin(2 * np.pi * (55 * np.exp(-t * 6)) * t) * np.exp(-t * 14)
    while t0 < total / SR - 1:
        for off, a in ((0, 1.0), (0.24, 0.7)):
            s = int((t0 + off) * SR)
            e = min(n, total - s)
            out[s:s + e] += a * thump[:e]
        t0 += period * rng.uniform(0.97, 1.03)
    return lowpass(out, 160) * level / max(1e-9, np.abs(out).max())


def rising_cluster(total, base, level):
    """strings that climb by themselves, a quarter-tone apart"""
    t = np.arange(total) / SR
    out = np.zeros(total)
    for i in range(6):
        f = base * 2 ** ((i * 0.5 + rng.uniform(-0.2, 0.2)) / 12)
        glide = 2 ** ((np.clip(t / (total / SR), 0, 1) * 5 + 0.6 * smooth_noise(total, 0.1)) / 12)
        ph = 2 * np.pi * np.cumsum(f * glide * (1 + 0.004 * np.sin(2 * np.pi * 5.5 * t + i))) / SR
        saw = sum(np.sin(k * ph) / k for k in range(1, 12))
        out += saw
    out = lowpass(out, 2400)
    tremolo = 0.75 + 0.25 * np.sin(2 * np.pi * 7 * t)
    return out * tremolo * level / max(1e-9, np.abs(out).max())


def scrapes(total, count, level):
    out = np.zeros(total)
    for _ in range(count):
        dur = rng.uniform(1.2, 3.0)
        n = int(dur * SR)
        s = int(rng.uniform(0, total / SR - dur) * SR)
        x = rng.normal(0, 1, n)
        # a comb: the resonance of something long and hollow being dragged
        d = int(SR / rng.uniform(90, 260))
        a = np.zeros(d + 1); a[0] = 1; a[d] = -0.93
        y = signal.lfilter([1.0], a, x)
        y = bandpass(y, 300, 3500)
        t = np.arange(n) / SR
        y *= np.sin(np.pi * t / dur) ** 2 * (0.6 + 0.4 * np.sin(2 * np.pi * 9 * t))
        out[s:s + n] += y / max(1e-9, np.abs(y).max())
    return out * level / max(1e-9, np.abs(out).max())


# ------------------------------------------------------------------ the pieces

LULLABY = [("D5", 2), ("F5", 1), ("A5", 2), ("G5", 1), ("F5", 2), ("E5", 1), ("D5", 3), (None, 2),
           ("A4", 2), ("D5", 1), ("F5", 2), ("E5", 1), ("C#5", 3), (None, 1), ("E5", 2), ("D5", 4), (None, 4),
           ("F5", 2), ("A5", 1), ("Bb5", 2), ("A5", 1), ("G5", 2), ("E5", 1), ("F5", 3), (None, 1),
           ("D5", 2), ("E5", 1), ("F5", 2), ("Eb5", 1), ("D5", 6), (None, 6)]


def drift(seconds=152):
    n = int(seconds * SR)
    mix = np.zeros((2, n))
    mix += pan(drone(n, hz("D1")), -0.1) * 0.55
    mix += pan(drone(n, hz("A1"), bright=0.7), 0.15) * 0.3
    mix += chords(n, [["D3", "F3", "A3", "E4"], ["D3", "F3", "Bb3", "D4"], ["D3", "G3", "Bb3", "Eb4"], ["C#3", "E3", "G3", "Bb3"]],
                  16, level=0.35, warmth=1100)
    mix += pan(wind(n, 250, 900, 1.0), -0.3) * 0.12 + pan(wind(n, 400, 1400, 1.0), 0.4) * 0.09
    box = music_box(n, LULLABY, 0.42, 38, 118, level=1.0)
    mix += pan(box, 0.25) * 0.22
    mix += pan(glass(n, [1318, 1760], 1.0), -0.5) * 0.04
    return master(reverb(mix, 7.0, 2200, 0.45, 1), seconds)


def night(seconds=150):
    n = int(seconds * SR)
    mix = np.zeros((2, n))
    mix += pan(drone(n, hz("G1") / 1.0, bright=0.6), 0.0) * 0.6
    mix += pan(drone(n, hz("Ab1"), bright=0.4, partials=16), 0.2) * 0.18
    mix += chords(n, [["G2", "Bb2", "D3", "Ab3"], ["G2", "C3", "Eb3", "Gb3"], ["F#2", "A2", "C3", "Eb3"]], 20, level=0.28, warmth=800)
    mix += pan(glass(n, [1046, 1108, 1568, 2217], 1.0), 0.3) * 0.07
    mix += pan(bowed_metal(n, 6, 1.0), -0.4) * 0.12
    mix += pan(wind(n, 150, 600, 1.0), 0.0) * 0.14
    slow = [(nt, b * 1.6) for nt, b in LULLABY]
    mix += pan(lowpass(music_box(n, slow, 0.5, 60, 140, flat=(-60, -20), level=1.0, wow=0.008), 2500), -0.35) * 0.13
    return master(reverb(mix, 9.0, 1500, 0.55, 2), seconds)


def dread(seconds=96):
    n = int(seconds * SR)
    mix = np.zeros((2, n))
    mix += pan(drone(n, hz("C#1"), bright=1.3), 0.0) * 0.5
    mix += pan(heartbeat(n, 58, 1.0), 0.0) * 0.55
    mix += pan(rising_cluster(n, hz("D3"), 1.0), 0.1) * 0.22
    mix += pan(scrapes(n, 9, 1.0), 0.5) * 0.1 + pan(scrapes(n, 7, 1.0), -0.5) * 0.08
    mix += pan(glass(n, [2093, 2217, 2349], 1.0), -0.2) * 0.05
    broken = [("D5", 3), (None, 5), ("Eb5", 4), (None, 7), ("A4", 5), (None, 9)]
    mix += pan(music_box(n, broken, 0.6, 8, 90, flat=(-80, -30), level=1.0, wow=0.012), 0.4) * 0.12
    return master(reverb(mix, 6.0, 1800, 0.4, 3), seconds)


def master(x, seconds, fade=8.0):
    """make it loop without a seam: the last seconds are folded over the first"""
    n = int(seconds * SR)
    f = int(fade * SR)
    body = x[:, : n - f].copy()
    tail = x[:, n - f: n]
    k = np.sin(np.linspace(0, np.pi / 2, f)) ** 2
    body[:, :f] = body[:, :f] * k + tail * (1 - k)
    body = np.tanh(body * 1.4 / np.abs(body).max()) / np.tanh(1.4)
    return body * 0.85


def write(x, name):
    os.makedirs(OUT, exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tf:
        w = wave.open(tf.name, "wb")
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((np.clip(x.T, -1, 1) * 32767).astype(np.int16).tobytes())
        w.close()
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tf.name, "-c:a", "libvorbis", "-q:a", "3",
                        os.path.join(OUT, name + ".ogg")], check=True)
        os.unlink(tf.name)
    print(name, x.shape[1] / SR, "s")


if __name__ == "__main__":
    import sys
    which = sys.argv[1:] or ["drift", "night", "dread"]
    for wname in which:
        write(globals()[wname](), "music_" + wname)
