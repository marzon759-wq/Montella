#!/usr/bin/env python3
"""Builds the battle and special move sounds from real CC0 recordings (OpenGameArt, see CREDITS.md):
metal clashes, explosions, rock debris, wind, rocket and energy sounds, layered and mastered for an anime-style mix.

usage: gen_sfx.py <folder with the downloaded OpenGameArt packs>
The packs: see SOURCES below (page name on opengameart.org/content/<page>). All CC0.
"""
import os
import subprocess
import sys

import numpy as np

SR = 44100
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', 'common', 'src', 'main', 'resources', 'assets', 'librabeyblade', 'sounds')
RAW = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, 'raw_sfx')


def load(rel, start=0.0, dur=None, rate=1.0, rev=False):
    """Decode a file to mono float, optionally cut, resample-pitch (rate>1 = higher/shorter) and reverse."""
    cmd = ['ffmpeg', '-v', 'error', '-i', os.path.join(RAW, rel), '-f', 'f32le', '-ac', '1', '-ar', str(SR), '-']
    x = np.frombuffer(subprocess.run(cmd, capture_output=True, check=True).stdout, dtype=np.float32).copy()
    # trim leading silence
    a = np.abs(x)
    thr = max(1e-4, a.max() * 0.02)
    nz = np.nonzero(a > thr)[0]
    if len(nz):
        x = x[max(0, nz[0] - 200):nz[-1] + 2000]
    s = int(start * SR)
    x = x[s:]
    if dur is not None:
        x = x[:int(dur * SR)]
    if rate != 1.0:
        idx = np.arange(0, len(x) - 1, rate)
        x = np.interp(idx, np.arange(len(x)), x).astype(np.float32)
    if rev:
        x = x[::-1].copy()
    m = np.max(np.abs(x)) or 1.0
    return x / m


def fade(x, fin=0.005, fout=0.2):
    n = len(x)
    i = min(n, int(fin * SR))
    o = min(n, int(fout * SR))
    if i:
        x[:i] *= np.linspace(0, 1, i)
    if o:
        x[n - o:] *= np.linspace(1, 0, o)
    return x


def lowpass(x, cut):
    # one pole, run twice
    a = np.exp(-2 * np.pi * cut / SR)
    for _ in range(2):
        y = np.empty_like(x)
        acc = 0.0
        for i in range(len(x)):
            acc = (1 - a) * x[i] + a * acc
            y[i] = acc
        x = y
    return x


def lp(x, cut):
    """fast low pass via FFT"""
    f = np.fft.rfft(x)
    fr = np.fft.rfftfreq(len(x), 1 / SR)
    f *= 1.0 / (1.0 + (fr / cut) ** 4)
    return np.fft.irfft(f, len(x)).astype(np.float32)


def hp(x, cut):
    f = np.fft.rfft(x)
    fr = np.fft.rfftfreq(len(x), 1 / SR)
    f *= 1.0 / (1.0 + (cut / np.maximum(fr, 1.0)) ** 4)
    return np.fft.irfft(f, len(x)).astype(np.float32)


def mix(parts, length=None):
    """parts: (signal, start seconds, gain)"""
    end = max(int(t * SR) + len(s) for s, t, g in parts)
    n = int(length * SR) if length else end
    out = np.zeros(max(n, end), dtype=np.float32)
    for s, t, g in parts:
        i = int(t * SR)
        out[i:i + len(s)] += s * g
    out = out[:n] if length else out
    return out


def save(name, x, fout=0.15, drive=1.0, decay=None):
    x = x.astype(np.float32)
    if decay:
        x = env(x / (np.max(np.abs(x)) or 1.0), 0.004, decay)
    x = fade(x, 0.003, fout)
    x = np.tanh(x / (np.max(np.abs(x)) or 1.0) * drive)
    x = x / (np.max(np.abs(x)) or 1.0) * 0.92
    pcm = (x * 32767).astype(np.int16).tobytes()
    path = os.path.join(OUT, name + '.ogg')
    subprocess.run(['ffmpeg', '-v', 'error', '-y', '-f', 's16le', '-ar', str(SR), '-ac', '1', '-i', '-', '-c:a', 'libvorbis', '-q:a', '5', path],
                   input=pcm, check=True)
    print('%-16s %.2fs' % (name, len(x) / SR))


def env(x, attack, decay):
    t = np.arange(len(x)) / SR
    return x * np.minimum(1, t / max(attack, 1e-3)) * np.exp(-t / decay)


BF = 'sfx_breaking_and_falling/'
SC = 'sci-fi-sfx/'
SW = 'sword_clash_-_starninjas_0/'


def main():
    # ---------------------------------------------------------------- beyblade on beyblade
    for i, (sw, cl) in enumerate([(1, 'clink1_0.wav'), (4, 'clink2.wav'), (8, 'clink3.wav')]):
        a = load(SW + 'sword_clash.%d.ogg' % sw, dur=0.7, rate=1.15)
        b = load(cl, dur=0.5, rate=1.3)
        c = hp(load(BF + 'bfh1_metal_hit_0%d.ogg' % (i + 1), dur=0.4), 900)
        save('clash%d' % (i + 1), mix([(a, 0, 0.8), (b, 0.0, 0.5), (c, 0.0, 0.6)]), fout=0.25)
    heavy = mix([
        (load('clangs/clang - Marker #3.wav', dur=1.0, rate=1.1), 0, 0.8),
        (load(SW + 'sword_clash.6.ogg', dur=1.0), 0.0, 0.7),
        (lp(load('thud2.wav', dur=0.5), 400), 0.0, 0.9),
        (lp(load('explosion.wav', dur=0.8, rate=1.3), 1500), 0.02, 0.35),
    ])
    save('clash_heavy', heavy, fout=0.35, decay=0.35)
    save('wall_hit', mix([(load(BF + 'bfh1_hit_03.ogg', dur=0.4), 0, 0.9), (load('clink2.wav', dur=0.3, rate=1.5), 0, 0.4)]), fout=0.1)
    save('land', mix([(lp(load('thud3.wav', dur=0.35), 700), 0, 1.0), (load(BF + 'bfh1_metal_hit_05.ogg', dur=0.3, rate=1.4), 0.0, 0.4)]), fout=0.1)
    launch = mix([
        (load('swishes/swishes/swish-9.wav'), 0.0, 0.7),
        (load('swishes/swishes/swish-7.wav', rate=0.8), 0.06, 0.7),
        (load('whoosh2_0.wav', dur=0.9, rate=1.2), 0.05, 0.9),
        (load('clink3.wav', dur=0.25, rate=1.6), 0.02, 0.35),
    ])
    save('launch', launch, fout=0.3)
    # ---------------------------------------------------------------- world impact
    crater = mix([
        (lp(load('Chunky_Explosion.mp3', dur=2.5, rate=0.85), 2500), 0.0, 1.0),
        (load('explosion.wav', dur=1.6, rate=0.8), 0.0, 0.6),
        (load(BF + 'bfh1_rock_breaking_01.ogg'), 0.12, 0.7),
        (lp(load('low-rumbling-176033.mp3', dur=2.5), 300), 0.0, 0.7),
    ], 3.0)
    save('crater', crater, fout=1.0, drive=1.2, decay=1.1)
    save('meteor_impact', mix([(crater, 0, 1.0), (load(SC + 'explosion_01.ogg'), 0.0, 0.5)]), fout=1.2, drive=1.2, decay=1.2)
    save('inferno_impact', mix([(crater, 0, 1.0), (load(SC + 'explosion_02.ogg', rate=0.8), 0.05, 0.4)]), fout=1.2, drive=1.2, decay=1.2)
    debris = mix([
        (load(BF + 'bfh1_rock_falling_01.ogg'), 0.0, 0.9),
        (load(BF + 'bfh1_rock_falling_03.ogg'), 0.35, 0.8),
        (load(BF + 'bfh1_rock_falling_09.ogg'), 0.7, 0.8),
        (load(BF + 'bfh1_rock_falling_05.ogg'), 1.0, 0.6),
        (load('cave_in_-_starninjas_0.ogg', dur=2.2), 0.1, 0.5),
    ], 2.6)
    save('debris', debris, fout=0.8, decay=1.4)
    wind = load('wind_woosh_loop.ogg', dur=3.2)
    tornado = mix([
        (wind, 0, 1.0),
        (load('short_wind_sound.wav', dur=3.0), 0.2, 0.6),
        (lp(load('low-rumbling-176033.mp3', start=1.0, dur=3.0), 250), 0.0, 0.6),
        (load('whoosh2_0.wav', rate=0.6), 0.6, 0.5),
    ], 3.2)
    save('tornado', fade(tornado, 0.3, 0.9), fout=0.9)
    save('shockwave', mix([
        (load('whoosh2_0.wav', rate=0.75), 0.0, 0.9),
        (lp(load(SC + 'explosion_02.ogg'), 1200), 0.0, 0.8),
        (load(BF + 'bfh1_hit_01.ogg'), 0.0, 0.6),
    ], 1.6), fout=0.5, decay=0.5)
    # ---------------------------------------------------------------- special move parts
    sky = mix([
        (hp(load('rocket_engine.001.wav', dur=1.8), 300), 0.0, 0.8),
        (load('whoosh2_0.wav', rate=0.9), 0.0, 0.8),
        (load('swishes/swishes/swish-9.wav', rate=0.7), 0.0, 0.6),
        (load('power_up_sound_v2_0.ogg', rate=1.3), 0.1, 0.5),
    ], 2.0)
    save('sky_launch', fade(sky, 0.01, 0.8), fout=0.8)
    dive = load('whoosh2_0.wav', rate=0.5)
    rocket = load(SC + 'rocket_01.ogg')
    # rising pitch: play the rocket faster and faster
    k = np.linspace(0.8, 1.6, 4000)
    idx = np.cumsum(np.repeat(k, len(rocket) // 4000 + 1))[:len(rocket)]
    idx = idx[idx < len(rocket) - 1]
    rise = np.interp(idx, np.arange(len(rocket)), rocket).astype(np.float32)
    save('meteor_dive', mix([(rise, 0, 0.8), (dive, 0.1, 0.9), (load('wind_woosh_loop.ogg', dur=1.5), 0, 0.5)], 1.8), fout=0.3)
    aura = mix([(load('movingshield_sound.ogg', dur=2.0), 0, 0.8), (load('powerup.wav'), 0, 0.7), (load('power_up_sound_v3_0.ogg'), 0.1, 0.5)], 2.0)
    save('aura', aura, fout=0.6)
    save('charge', mix([(load('chargestart.wav'), 0, 0.8), (load('charge.wav', dur=1.2), 0.1, 0.7)], 1.5), fout=0.4)
    # clash wind: the gust thrown sideways by a hit
    save('clash_wind', mix([(load('whoosh2_0.wav', rate=1.1), 0.0, 0.9), (load('short_wind_sound.wav', dur=1.0, rate=1.2), 0.0, 0.6),
                            (load('swishes/swishes/swish-3.wav', rate=0.6), 0.0, 0.6)], 1.0), fout=0.5, decay=0.45)
    # cut-in: swish + blade ring
    save('cut_in', mix([(load('swishes/swishes/swish-9.wav', rate=0.9), 0.0, 0.8), (load(SW + 'sword_clash.3.ogg', rate=1.25), 0.06, 0.7),
                        (load('power_up_sound_v1_0.ogg', rate=1.4), 0.05, 0.35)], 1.2), fout=0.5)
    # thunder: the loudest crack of the rain + thunder recording
    rt = load('rain-thunder.ogg')
    w = int(0.05 * SR)
    e = np.convolve(np.abs(rt), np.ones(w) / w, mode='same')
    p = int(np.argmax(e))
    crack = rt[max(0, p - int(0.25 * SR)):p + int(3.0 * SR)]
    save('thunder_strike', mix([(crack, 0, 1.0), (load('crackleelectricityloop.wav', dur=0.6), 0.2, 0.3)]), fout=1.0, drive=1.1)
    save('storm_cloud', mix([(lp(load('low-rumbling-176033.mp3', dur=4.0), 400), 0, 0.9), (load('wind_woosh_loop.ogg', dur=4.0), 0, 0.6),
                              (crack[:int(2.5 * SR)] * 0.5, 1.0, 0.4)], 4.0), fout=1.2)


if __name__ == '__main__':
    main()
