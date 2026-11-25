#!/usr/bin/env python3
"""The sounds of the creatures of the Blood Sea.

Sources: the CC0 recordings already processed for the MIMIC mod (roars, growls, screams, clicks, steps, water,
heartbeat), slowed, layered and reverberated here; whispers and calls voiced with the Kokoro TTS model (Apache-2.0)
and turned to breath by throwing away their pitch; a few things synthesised outright (the sun's drone, the rumble of
the sea rising).

usage: gen_creature_sounds.py <tts dir with kokoro.onnx + voices.bin> <mimic sounds dir>
Writes assets/bloodsea/sounds/*.ogg and sounds.json."""
import json, os, sys
import numpy as np
import soundfile as sf

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, "../common/src/main/resources/assets/bloodsea")
OUT = os.path.join(A, "sounds")
SR = 44100
TTS = sys.argv[1] if len(sys.argv) > 1 else None
SRC = sys.argv[2] if len(sys.argv) > 2 else os.path.join(HERE, "../../mimic/common/src/main/resources/assets/mimicry/sounds")
RNG = np.random.default_rng(7)


# ============================================================================ dsp
def load(name):
    """decoded with ffmpeg: libsndfile reads some of these Vorbis files as empty"""
    import subprocess, tempfile
    with tempfile.NamedTemporaryFile(suffix=".wav") as tmp:
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", os.path.join(SRC, name + ".ogg"), "-ac", "1", "-ar", str(SR), tmp.name], check=True)
        x, sr = sf.read(tmp.name, always_2d=True)
    x = x.mean(1)
    if sr != SR:
        x = resample(x, SR / sr)
    return x


def resample(x, f):
    """stretch by f (f > 1 longer); used as pitch: slowing lowers the pitch"""
    n = int(len(x) * f)
    return np.interp(np.linspace(0, len(x) - 1, n), np.arange(len(x)), x)


def pitch(x, p):
    return resample(x, 1 / p)


def lowpass(x, fc):
    a = np.exp(-2 * np.pi * fc / SR)
    y = np.zeros_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = (1 - a) * x[i] + a * acc
        y[i] = acc
    return y


def lp(x, fc):
    """fast one-pole low-pass through an FFT"""
    n = len(x)
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(n, 1 / SR)
    return np.fft.irfft(X / np.sqrt(1 + (f / fc) ** 4), n)


def hp(x, fc):
    n = len(x)
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(n, 1 / SR)
    h = (f / fc) ** 2 / np.sqrt(1 + (f / fc) ** 4)
    return np.fft.irfft(X * h, n)


def reverb(x, seconds=2.0, wet=0.35, dark=3000):
    n = int(seconds * SR)
    t = np.arange(n) / SR
    ir = RNG.standard_normal(n) * np.exp(-t * 6.9 / seconds)
    ir = lp(ir, dark)
    ir[0] = 0
    ir /= np.sqrt((ir ** 2).sum()) + 1e-9
    m = len(x) + n
    L = 1 << int(np.ceil(np.log2(m)))
    y = np.fft.irfft(np.fft.rfft(x, L) * np.fft.rfft(ir, L), L)[:m]
    dry = np.concatenate([x, np.zeros(n)])
    return dry * (1 - wet) + y * wet * 3


def mix(*parts):
    """(signal, offset seconds, gain) ..."""
    n = max(int(o * SR) + len(s) for s, o, g in parts)
    y = np.zeros(n)
    for s, o, g in parts:
        i = int(o * SR)
        y[i:i + len(s)] += s * g
    return y


def fade(x, a=0.01, b=0.2):
    na, nb = int(a * SR), int(b * SR)
    x = x.copy()
    if na:
        x[:na] *= np.linspace(0, 1, na)
    if nb:
        x[-nb:] *= np.linspace(1, 0, nb)
    return x


def norm(x, peak=0.9):
    m = np.abs(x).max()
    return x / m * peak if m > 0 else x


def brown(sec):
    w = RNG.standard_normal(int(sec * SR))
    b = np.cumsum(w)
    b -= np.convolve(b, np.ones(2000) / 2000, mode="same")
    return b / (np.abs(b).max() + 1e-9)


def drive(x, k=2.5):
    return np.tanh(x * k) / np.tanh(k)


def trim(x, sec):
    return x[:int(sec * SR)]


def slosh(sec):
    """thick liquid moving: low noise swelling and falling, with bubbles bursting in it"""
    n = int(sec * SR)
    t = np.arange(n) / SR
    body = lp(RNG.standard_normal(n), 700)
    env = np.clip(0.55 + 0.45 * np.sin(2 * np.pi * 0.7 * t + 2 * np.sin(2 * np.pi * 0.23 * t)), 0, 1)
    y = body * env * 6
    for k in range(int(sec * 9)):
        i = RNG.integers(0, n - 4000)
        f0 = RNG.uniform(120, 420)
        tt = np.arange(3000) / SR
        y[i:i + 3000] += np.sin(2 * np.pi * f0 * tt * (1 + tt * 6)) * np.exp(-tt * 70) * RNG.uniform(0.2, 0.6)
    return y / (np.abs(y).max() + 1e-9)


SOUNDS = {}


def save(name, *variants, subtitle=True):
    files = []
    for i, x in enumerate(variants):
        fn = f"{name}{i if len(variants) > 1 else ''}"
        sf.write(os.path.join(OUT, fn + ".ogg"), norm(x).astype(np.float32), SR, format="OGG", subtype="VORBIS")
        files.append(f"bloodsea:{fn}")
    SOUNDS[name] = {"sounds": files}
    if subtitle:
        SOUNDS[name]["subtitle"] = f"subtitles.bloodsea.{name}"
    print("sound", name, len(variants))


# ============================================================================ voices
def whisperise(x):
    """keep the shape of the words, throw away the voice: STFT magnitude with random phase"""
    n, hop = 1024, 256
    win = np.hanning(n)
    frames = [x[i:i + n] * win for i in range(0, len(x) - n, hop)]
    out = np.zeros(len(x) + n)
    for k, f in enumerate(frames):
        S = np.abs(np.fft.rfft(f))
        ph = np.exp(1j * RNG.uniform(0, 2 * np.pi, len(S)))
        out[k * hop:k * hop + n] += np.fft.irfft(S * ph, n) * win
    return hp(out[:len(x)], 900)


def tts(lines, voice, speed):
    from kokoro_onnx import Kokoro
    k = Kokoro(os.path.join(TTS, "kokoro.onnx"), os.path.join(TTS, "voices.bin"))
    out = []
    for text in lines:
        s, sr = k.create(text, voice=voice, speed=speed, lang="en-us")
        s = np.asarray(s, float)
        if sr != SR:
            s = resample(s, SR / sr)
        out.append(s)
    return out


# ============================================================================ the creatures
def main():
    os.makedirs(OUT, exist_ok=True)
    water, roar0, roar1 = slosh(4.0), load("mm_roar0"), load("mm_roar1")
    clicks = [load(f"mm_click{i}") for i in range(3)]
    steps = [load(f"mm_step{i}") for i in range(3)]
    growls = [load(f"mm_growl{i}") for i in range(4)]
    eats = [load(f"mm_eat{i}") for i in range(3)]
    scr0, scr1 = load("mm_scream0"), load("mm_scream1")

    # ---- the Wake
    save("wake_swim", *[fade(lp(mix((pitch(trim(water, 3.0), p), 0, 0.8), (brown(3.2), 0, 0.5)), 900), 0.3, 1.0) for p in (0.55, 0.65)])
    save("wake_surface", fade(reverb(mix((pitch(water, 0.45), 0, 1.0), (pitch(load("mm_breath0"), 0.45), 0.4, 0.9)), 2.5, 0.3), 0.02, 0.8))
    save("wake_lunge", fade(reverb(mix((pitch(roar0, 0.55), 0, 1.0), (pitch(water, 0.8), 0.1, 0.9), (pitch(water, 0.5), 0.7, 0.8)), 2.0, 0.3), 0.01, 0.8))

    # ---- the Bone Gatherer
    def rattle(sec, rate, lo=0.6, hi=1.4):
        parts = []
        t = 0
        while t < sec:
            c = clicks[RNG.integers(3)]
            parts.append((pitch(c, RNG.uniform(lo, hi)), t, RNG.uniform(0.4, 1.0)))
            t += RNG.exponential(1 / rate)
        return mix(*parts)
    save("gatherer_rattle", *[fade(reverb(rattle(1.6, 14), 0.8, 0.25), 0.01, 0.4) for _ in range(3)])
    save("gatherer_step", *[fade(mix((pitch(steps[i], 0.55), 0, 1.0), (rattle(0.5, 20, 0.8, 1.5), 0.05, 0.5), (pitch(trim(water, 0.6), 0.7), 0, 0.5)), 0.005, 0.2) for i in range(3)])
    chorus = mix(*[(pitch(roar1, p), RNG.uniform(0, 0.15), g) for p, g in ((0.58, 1.0), (0.72, 0.8), (0.9, 0.6), (1.15, 0.45))], (pitch(scr0, 0.8), 0.3, 0.4))
    save("gatherer_roar", fade(reverb(drive(norm(chorus), 1.8), 2.5, 0.35), 0.02, 1.0))
    acc = mix(*[(rattle(0.6, 6 + k * 6), k * 0.55, 0.6 + k * 0.08) for k in range(7)], (pitch(load("mm_twitch0"), 0.7), 3.2, 0.7))
    save("gatherer_assemble", fade(reverb(acc, 1.5, 0.3), 0.02, 0.6))
    sweep = brown(0.7) * np.sin(np.linspace(0, np.pi, int(0.7 * SR))) ** 2
    save("gatherer_swipe", fade(mix((hp(sweep, 300), 0, 1.0), (rattle(0.4, 25), 0.25, 0.6)), 0.01, 0.2))

    # ---- the Name Caller
    if TTS:
        wl = tts(["I can hear you.", "Where are you going?", "Come back to the water.", "Don't stop rowing.", "Say my name."], "af_bella", 0.82)
        wl2 = tts(["I'm right here.", "Why won't you look at me?"], "am_onyx", 0.8)
        save("caller_whisper", *[fade(reverb(whisperise(x), 1.6, 0.3, 5000), 0.02, 0.4) for x in wl + wl2])
        calls = tts(["Hey. It's me. Turn around.", "Are you awake?", "Wait for me.", "You left me here."], "af_bella", 0.88)
        calls += tts(["Son? Come here.", "It's cold in the water."], "am_onyx", 0.84)
        save("caller_call", *[fade(reverb(mix((lp(pitch(x, 0.93), 3500), 0, 0.7), (whisperise(pitch(x, 0.93)), 0, 0.5)), 2.2, 0.4, 3000), 0.02, 0.6) for x in calls])
    save("caller_scream", fade(reverb(drive(mix((pitch(scr1, 0.72), 0, 1.0), (pitch(scr0, 1.08), 0.05, 0.6), (pitch(scr1, 0.5), 0.0, 0.5)), 2.2), 2.5, 0.35), 0.01, 0.8))
    save("caller_wade", *[fade(pitch(trim(water, 1.2), p), 0.05, 0.4) for p in (0.7, 0.8, 0.9)])
    save("caller_lunge", fade(reverb(mix((pitch(growls[1], 0.8), 0, 1.0), (pitch(trim(water, 1.0), 1.1), 0.1, 0.7)), 1.2, 0.25), 0.01, 0.3))

    # ---- the Changer
    smack = lp(RNG.standard_normal(int(0.08 * SR)) * np.exp(-np.arange(int(0.08 * SR)) / (0.012 * SR)), 1800)
    save("changer_blink", *[fade(mix((smack, 0, 1.0), (pitch(clicks[i], 1.6), 0.005, 0.4)), 0.001, 0.03) for i in range(2)])
    save("changer_scuttle", *[fade(rattle(0.35, 40, 1.6, 2.4), 0.002, 0.08) for _ in range(3)])
    save("changer_bite", fade(mix((pitch(eats[0], 1.0), 0, 1.0), (pitch(clicks[2], 0.5), 0.0, 0.9)), 0.002, 0.2))
    save("changer_squelch", *[fade(pitch(eats[i], p), 0.005, 0.2) for i, p in ((1, 0.7), (2, 0.8))])

    # ---- the Leviathan
    hb = load("amb_heartbeat")
    save("lev_heartbeat", fade(lp(pitch(trim(hb, 2.4), 0.6), 300) * 1.6, 0.01, 0.3))
    save("lev_roar", fade(reverb(drive(mix((pitch(roar0, 0.4), 0, 1.0), (pitch(roar1, 0.45), 0.2, 0.9), (lp(brown(5), 200), 0, 0.6)), 1.6), 5.0, 0.45, 2000), 0.05, 2.0))
    rumble = lp(brown(6.0), 160) * np.linspace(0.2, 1, int(6 * SR)) ** 1.5
    save("lev_rise", fade(reverb(mix((rumble, 0, 1.0), (pitch(water, 0.4), 1.0, 0.8), (pitch(load("mm_emerge"), 0.5), 3.0, 0.8)), 3.0, 0.35, 2000), 0.5, 1.5))
    thump = lp(RNG.standard_normal(int(0.6 * SR)) * np.exp(-np.arange(int(0.6 * SR)) / (0.1 * SR)), 120) * 4
    save("lev_bite", fade(reverb(mix((pitch(load("mm_kill"), 0.5), 0, 0.9), (thump, 0.25, 1.0), (pitch(water, 0.5), 0.3, 0.6)), 3.0, 0.35, 2000), 0.01, 1.2))

    # ---- the Watcher: a drone that is felt more than heard, and a thin high tone over it
    t = np.arange(int(4 * SR)) / SR
    env = np.sin(np.pi * t / 4) ** 2
    drone = (np.sin(2 * np.pi * 41 * t) + 0.5 * np.sin(2 * np.pi * 61.5 * t + np.sin(t * 2))) * env
    high = np.sin(2 * np.pi * 1760 * t + 3 * np.sin(2 * np.pi * 0.4 * t)) * env * 0.08
    breath = hp(RNG.standard_normal(len(t)), 2000) * env * 0.05
    save("watcher_stare", reverb(drone + high + breath, 3.0, 0.3))

    with open(os.path.join(A, "sounds.json"), "w") as f:
        json.dump(SOUNDS, f, indent=1)
    print(len(SOUNDS), "sound events")


if __name__ == "__main__":
    main()
