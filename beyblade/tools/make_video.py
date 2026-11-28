#!/usr/bin/env python3
"""Turns a VideoDirector recording (run/video/) into the final gameplay video.

  frames + frames.csv  -> constant 30 fps video (frames are timed by game time, so motion plays at game speed)
  captions.tsv         -> burned-in ASS captions ("Title|subtitle" for title cards)
  sounds.csv           -> the mod's own sounds, placed where the server played them, attenuated by camera distance
  hum.csv              -> the spin hum of the beys near the camera
  + a small synthesised music bed

usage: make_video.py <run/video dir> <out.mp4>
"""
import csv
import json
import math
import os
import subprocess
import sys

import numpy as np

SR = 48000
HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, '..', 'common', 'src', 'main', 'resources', 'assets', 'librabeyblade')


def load_frames(d):
    rows = list(csv.DictReader(open(os.path.join(d, 'frames.csv'))))
    vt = np.array([float(r['vt']) for r in rows])
    gt = np.array([float(r['gt']) for r in rows])
    cam = np.array([[float(r['x']), float(r['y']), float(r['z'])] for r in rows])
    return vt, gt, cam


def decode(path):
    raw = subprocess.run(['ffmpeg', '-v', 'error', '-i', path, '-f', 'f32le', '-ac', '1', '-ar', str(SR), '-'], capture_output=True, check=True).stdout
    return np.frombuffer(raw, dtype=np.float32).copy()


def sound_files():
    d = json.load(open(os.path.join(ASSETS, 'sounds.json')))
    out = {}
    for k, v in d.items():
        files = []
        for s in v['sounds']:
            name = s['name'] if isinstance(s, dict) else s
            files.append(os.path.join(ASSETS, 'sounds', name.split(':', 1)[1] + '.ogg'))
        out['librabeyblade:' + k] = files
    return out


def music(length):
    """Driving 128 bpm bed: kick, off-beat hat, a minor bass line and a soft pad."""
    n = int(length * SR)
    t = np.arange(n) / SR
    out = np.zeros(n, dtype=np.float32)
    beat = 60.0 / 128.0
    # kick
    for b in np.arange(0, length, beat):
        i = int(b * SR)
        k = np.arange(min(int(0.25 * SR), n - i)) / SR
        out[i:i + len(k)] += 0.55 * np.sin(2 * np.pi * (50 + 90 * np.exp(-k * 30)) * k) * np.exp(-k * 9)
    # hats
    rng = np.random.default_rng(3)
    for b in np.arange(beat / 2, length, beat):
        i = int(b * SR)
        k = min(int(0.05 * SR), n - i)
        out[i:i + k] += 0.08 * rng.standard_normal(k) * np.exp(-np.arange(k) / SR * 90)
    # bass: A minor riff, 8th notes
    riff = [45, 45, 57, 45, 48, 48, 60, 50, 43, 43, 55, 43, 40, 40, 52, 47]
    step = beat / 2
    for j, b in enumerate(np.arange(0, length, step)):
        i = int(b * SR)
        k = min(int(step * SR), n - i)
        f = 440.0 * 2 ** ((riff[j % len(riff)] - 69) / 12)
        tt = np.arange(k) / SR
        saw = 2 * ((tt * f) % 1.0) - 1
        out[i:i + k] += 0.12 * saw * np.exp(-tt * 6)
    # pad: chord per bar
    chords = [[57, 60, 64], [53, 57, 60], [55, 59, 62], [52, 55, 59]]
    bar = beat * 4
    for j, b in enumerate(np.arange(0, length, bar)):
        i = int(b * SR)
        k = min(int(bar * SR), n - i)
        tt = np.arange(k) / SR
        env = np.minimum(1, tt / 0.4) * np.minimum(1, (bar - tt) / 0.4)
        for m in chords[j % 4]:
            f = 440.0 * 2 ** ((m - 69) / 12)
            out[i:i + k] += 0.025 * env * (np.sin(2 * np.pi * f * tt) + 0.5 * np.sin(2 * np.pi * f * 1.005 * tt))
    return out


def build_audio(d, vt, gt, cam, length, out_wav):
    n = int(length * SR) + SR
    mix = np.zeros(n, dtype=np.float32)
    files = sound_files()
    cache = {}
    count = 0
    if os.path.exists(os.path.join(d, 'sounds.csv')):
        for r in csv.DictReader(open(os.path.join(d, 'sounds.csv'))):
            sid = r['id']
            if sid not in files or sid.endswith('spin_loop'):
                continue
            g = float(r['gt'])
            if g < gt[0] or g > gt[-1]:
                continue
            t = float(np.interp(g, gt, vt))
            c = np.array([np.interp(t, vt, cam[:, k]) for k in range(3)])
            p = np.array([float(r['x']), float(r['y']), float(r['z'])])
            vol = float(r['vol'])
            dist = float(np.linalg.norm(p - c))
            att = vol * max(0.0, 1.0 - dist / (16.0 * max(1.0, vol)))
            if att <= 0.01:
                continue
            f = files[sid][count % len(files[sid])]
            if f not in cache:
                if not os.path.exists(f):
                    continue
                cache[f] = decode(f)
            s = cache[f]
            pitch = max(0.5, min(2.0, float(r['pitch'])))
            if abs(pitch - 1.0) > 0.01:
                idx = np.arange(0, len(s) - 1, pitch)
                s = np.interp(idx, np.arange(len(s)), s).astype(np.float32)
            i = int(t * SR)
            k = min(len(s), n - i)
            if k > 0:
                mix[i:i + k] += 0.6 * att * s[:k]
                count += 1
    # spin hum
    hum_rows = list(csv.DictReader(open(os.path.join(d, 'hum.csv'))))
    if hum_rows:
        ht = np.array([float(r['vt']) for r in hum_rows])
        hl = np.array([float(r['level']) for r in hum_rows])
        loop = decode(os.path.join(ASSETS, 'sounds', 'spin_loop.ogg'))
        reps = int(math.ceil(n / len(loop)))
        bed = np.tile(loop, reps)[:n]
        env = np.interp(np.arange(n) / SR, ht, np.minimum(1.6, hl))
        # smooth the envelope (50 ms)
        w = int(0.05 * SR)
        env = np.convolve(env, np.ones(w) / w, mode='same')
        mix += 0.22 * bed * env.astype(np.float32)
    mus = music(n / SR)
    fade = np.minimum(1, np.arange(n) / (2 * SR)) * np.minimum(1, (n - np.arange(n)) / (3 * SR))
    mix += 0.5 * mus * fade
    peak = np.max(np.abs(mix)) or 1.0
    mix = np.tanh(mix / peak * 1.6) * 0.89
    pcm = (mix * 32767).astype(np.int16)
    import wave
    with wave.open(out_wav, 'w') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    print('audio: %d sound events placed' % count)


def ass_time(t):
    h = int(t // 3600)
    m = int(t % 3600 // 60)
    s = t % 60
    return '%d:%02d:%05.2f' % (h, m, s)


def build_ass(d, out_ass, W, H):
    lines = [l.rstrip('\n').split('\t', 1) for l in open(os.path.join(d, 'captions.tsv'), encoding='utf-8') if l.strip()]
    ev = []
    for i in range(len(lines) - 1):
        t0 = float(lines[i][0])
        t1 = float(lines[i + 1][0])
        text = lines[i][1] if len(lines[i]) > 1 else ''
        if not text.strip() or t1 - t0 < 0.2:
            continue
        t1 = min(t1, t0 + 9.0)
        if '|' in text:
            a, b = text.split('|', 1)
            ev.append('Dialogue: 0,%s,%s,Title,,0,0,0,,{\\fad(400,400)}%s\\N{\\rSub}%s' % (ass_time(t0), ass_time(t1), a, b))
        else:
            ev.append('Dialogue: 0,%s,%s,Cap,,0,0,0,,{\\fad(250,250)}%s' % (ass_time(t0), ass_time(t1), text))
    head = """[Script Info]
ScriptType: v4.00+
PlayResX: %d
PlayResY: %d

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Cap,DejaVu Sans,34,&H00FFFFFF,&H00FFFFFF,&H00101010,&H90000000,1,0,0,0,100,100,0,0,1,3,1,2,40,40,46,1
Style: Title,DejaVu Sans,64,&H0000D8FF,&H00FFFFFF,&H00101010,&H90000000,1,0,0,0,100,100,1,0,1,4,2,5,40,40,40,1
Style: Sub,DejaVu Sans,30,&H00FFFFFF,&H00FFFFFF,&H00101010,&H90000000,1,0,0,0,100,100,0,0,1,3,1,5,40,40,40,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
""" % (W, H)
    open(out_ass, 'w', encoding='utf-8').write(head + '\n'.join(ev) + '\n')


def main():
    d, out = sys.argv[1], sys.argv[2]
    vt, gt, cam = load_frames(d)
    length = float(vt[-1])
    fdir = os.path.join(d, 'frames')
    with open(os.path.join(d, 'concat.txt'), 'w') as f:
        for i in range(len(vt)):
            dur = (vt[i + 1] - vt[i]) if i + 1 < len(vt) else 1.0 / 30
            f.write("file '%s'\nduration %.5f\n" % (os.path.join(fdir, 'f_%06d.jpg' % i), max(dur, 0.001)))
        f.write("file '%s'\n" % os.path.join(fdir, 'f_%06d.jpg' % (len(vt) - 1)))
    from PIL import Image
    W, H = Image.open(os.path.join(fdir, 'f_000000.jpg')).size
    build_ass(d, os.path.join(d, 'captions.ass'), W, H)
    build_audio(d, vt, gt, cam, length, os.path.join(d, 'audio.wav'))
    vf = "fps=30,subtitles=%s,fade=t=in:st=0:d=1.2,fade=t=out:st=%.2f:d=2" % (os.path.join(d, 'captions.ass'), length - 2.0)
    subprocess.run(['ffmpeg', '-y', '-v', 'warning', '-f', 'concat', '-safe', '0', '-i', os.path.join(d, 'concat.txt'), '-i', os.path.join(d, 'audio.wav'),
                    '-vf', vf, '-c:v', 'libx264', '-preset', 'medium', '-crf', '20', '-pix_fmt', 'yuv420p', '-c:a', 'aac', '-b:a', '192k',
                    '-t', '%.2f' % length, '-movflags', '+faststart', out], check=True)
    print('video: %.1f s -> %s' % (length, out))


if __name__ == '__main__':
    main()
