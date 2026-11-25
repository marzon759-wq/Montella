#!/usr/bin/env python3
"""Voices and story sounds: every line in story_lines.py voiced with Kokoro (Apache-2.0) and treated by speaker; Pip's
voice (meows, purring, hissing, a chirp) synthesised; the hospital monitor, the storm, splashes. Writes the sound
files, merges sounds.json and both language files, and writes Lines.java (each line's length in ticks).

usage: gen_story_audio.py <tts dir with kokoro.onnx + voices.bin>"""
import json, os, sys
import numpy as np
import soundfile as sf
import story_lines as SL
import gen_creature_sounds as G

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, "../common/src/main/resources/assets/bloodsea")
J = os.path.join(HERE, "../common/src/main/java/com/krolasyon/bloodsea/story")
SR = G.SR
RNG = np.random.default_rng(31)
VOICE = {"elias": ("bm_george", 0.9), "anna": ("bf_emma", 0.92), "voice": ("bf_emma", 0.84), "nurse": ("af_sarah", 1.0),
         "rescuer": ("bm_lewis", 1.05), "man": ("am_onyx", 1.08)}


def noise_bed(sec, kind):
    n = int(sec * SR)
    if kind == "storm":
        wind = G.lp(RNG.standard_normal(n), 500) * (0.6 + 0.4 * np.sin(np.linspace(0, 9, n)))
        rain = G.hp(RNG.standard_normal(n), 3000) * 0.25
        thunder = np.zeros(n)
        for k in range(2):
            i = RNG.integers(0, max(1, n - SR))
            m = min(int(2.5 * SR), n - i)
            thunder[i:i + m] += G.lp(RNG.standard_normal(m), 120) * np.exp(-np.arange(m) / (0.8 * SR)) * 6
        return wind * 1.2 + rain + thunder
    if kind == "rain":
        return G.hp(RNG.standard_normal(n), 2500) * 0.35 + G.lp(RNG.standard_normal(n), 300) * 0.5
    return np.zeros(n)


def treat(x, who):
    if who == "elias":
        return G.reverb(x, 0.6, 0.12, 6000)
    if who == "anna":       # a memory: far, soft, a little worn
        return G.reverb(G.lp(G.hp(x, 180), 4200), 2.2, 0.35, 3500)
    if who == "voice":      # her voice, worn by something else: slowed, doubled with a whisper, in the fog
        y = G.pitch(x, 0.9)
        w = G.whisperise(y)
        return G.reverb(G.mix((y, 0, 0.75), (w, 0, 0.6), (G.pitch(x, 0.62), 0.03, 0.22)), 3.0, 0.45, 2500)
    if who == "nurse":
        return G.reverb(x, 0.5, 0.15, 7000)
    if who == "man":
        y = G.drive(x * 1.4, 1.5)
        return G.mix((y, 0, 1.0), (noise_bed(len(y) / SR + 0.4, "storm"), 0, 0.35))
    if who == "rescuer":
        y = G.drive(x * 1.2, 1.3)
        return G.mix((y, 0, 1.0), (noise_bed(len(y) / SR + 0.3, "rain"), 0, 0.25))
    return x


def tts_all():
    from kokoro_onnx import Kokoro
    tts = sys.argv[1]
    k = Kokoro(os.path.join(tts, "kokoro.onnx"), os.path.join(tts, "voices.bin"))
    out_dir = os.path.join(A, "sounds/voice")
    os.makedirs(out_dir, exist_ok=True)
    sounds, durs = {}, {}
    for key, (who, en, tr) in SL.LINES.items():
        voice, speed = VOICE[who]
        s, sr = k.create(en.replace("...", ", "), voice=voice, speed=speed, lang="en-us" if voice.startswith("a") else "en-gb")
        x = np.asarray(s, float)
        if sr != SR:
            x = G.resample(x, SR / sr)
        y = treat(x, who)
        y = G.fade(y, 0.01, 0.12)
        fn = "v_" + key.replace(".", "_")
        sf.write(os.path.join(out_dir, fn + ".ogg"), G.norm(y, 0.85).astype(np.float32), SR, format="OGG", subtype="VORBIS")
        sounds[fn] = {"sounds": [{"name": f"bloodsea:voice/{fn}", "stream": False}]}
        durs[key] = int(len(y) / SR * 20) + 4
        print("voice", key, round(len(y) / SR, 1))
    return sounds, durs


# ============================================================================ Pip and the rest
def tone(f0, sec, form, breath=0.2):
    """a voiced sound with a pitch contour f0(t) and formants form(t) -> [(f, bw, gain)]"""
    n = int(sec * SR)
    t = np.arange(n) / SR
    f = np.array([f0(tt) for tt in t])
    ph = np.cumsum(2 * np.pi * f / SR)
    src = sum(np.sin(ph * h) / h for h in range(1, 14)) + RNG.standard_normal(n) * breath
    out = np.zeros(n)
    hop = 512
    for i in range(0, n, hop):
        seg = src[i:i + hop * 2]
        if len(seg) < 8:
            break
        S = np.fft.rfft(seg * np.hanning(len(seg)))
        fr = np.fft.rfftfreq(len(seg), 1 / SR)
        H = np.zeros_like(fr)
        for (ff, bw, g) in form(t[i]):
            H += g / (1 + ((fr - ff) / bw) ** 2)
        y = np.fft.irfft(S * H, len(seg))
        out[i:i + len(y)] += y
    return out


def meow(f_peak, sec, seed):
    rr = np.random.default_rng(seed)
    a, b = rr.uniform(0.25, 0.4), rr.uniform(0.6, 0.85)

    def f0(tt):
        k = tt / sec
        return f_peak * (0.7 + 0.3 * np.sin(np.pi * min(1, k / b)) ** 0.7) * (1 - 0.25 * max(0, k - b) / (1 - b))

    def form(tt):
        k = tt / sec
        f1 = 400 + 600 * np.sin(np.pi * min(1, k / 0.8))       # "mi-a-ow"
        f2 = 2400 - 1300 * min(1, k / 0.7)
        return [(f1, 160, 1.0), (f2, 260, 0.6), (3300, 400, 0.25)]
    y = tone(f0, sec, form, 0.12)
    env = np.minimum(1, np.arange(len(y)) / (0.04 * SR)) * np.minimum(1, (len(y) - np.arange(len(y))) / (0.12 * SR))
    return G.reverb(y * env, 0.4, 0.1, 8000)


def purr(sec):
    n = int(sec * SR)
    t = np.arange(n) / SR
    buzz = G.lp(RNG.standard_normal(n), 900) * (0.5 + 0.5 * np.sin(2 * np.pi * 26 * t)) ** 3
    breath = 0.6 + 0.4 * np.sin(2 * np.pi * t / 2.2)          # in and out
    return G.fade(buzz * breath + G.lp(RNG.standard_normal(n), 120) * 0.3 * breath, 0.2, 0.4)


def hiss(sec):
    n = int(sec * SR)
    env = np.minimum(1, np.arange(n) / (0.02 * SR)) * np.exp(-np.arange(n) / (sec * 0.6 * SR))
    return G.hp(RNG.standard_normal(n), 2200) * env + G.lp(RNG.standard_normal(n), 400) * env * 0.3


def misc():
    out = os.path.join(A, "sounds")
    made = {}

    def save(name, *xs):
        files = []
        for i, x in enumerate(xs):
            fn = name + (str(i) if len(xs) > 1 else "")
            sf.write(os.path.join(out, fn + ".ogg"), G.norm(x, 0.8).astype(np.float32), SR, format="OGG", subtype="VORBIS")
            files.append(f"bloodsea:{fn}")
        made[name] = {"sounds": files, "subtitle": f"subtitles.bloodsea.{name}"}
    # Pip's voice comes from real recordings: see gen_cat_sounds.py
    n = int(0.6 * SR)
    splash = G.lp(RNG.standard_normal(n), 1400) * np.exp(-np.arange(n) / (0.12 * SR))
    save("blood_splash", G.mix((splash, 0, 1.0), (G.slosh(0.8), 0.05, 0.4)))
    t = np.arange(int(1.0 * SR)) / SR
    beep = np.sin(2 * np.pi * 980 * t) * ((t < 0.12).astype(float)) * 0.6
    save("monitor_beep", beep)
    save("storm", G.fade(noise_bed(12, "storm"), 0.5, 1.5))
    churn = G.lp(RNG.standard_normal(int(8 * SR)), 140) * (0.5 + 0.5 * np.sin(np.linspace(0, 20, int(8 * SR)))) * 2
    save("stomach", G.fade(G.reverb(churn, 3.0, 0.4, 600), 0.5, 1.0))
    hb = G.load("amb_heartbeat")
    save("heartbeat_near", G.fade(G.lp(G.pitch(G.trim(hb, 2.0), 0.8), 400) * 1.6, 0.01, 0.2))
    return made


def write_lang(durs):
    for f, idx in (("en_us", 1), ("tr_tr", 2)):
        p = os.path.join(A, f"lang/{f}.json")
        d = json.load(open(p, encoding="utf-8"))
        for key, v in SL.LINES.items():
            d[f"line.bloodsea.{key}"] = v[idx]
        for cid, opts in SL.CHOICES.items():
            d[f"choiceq.bloodsea.{cid}"] = SL.QUESTIONS[cid][idx - 1]
            for o in opts:
                d[f"choice.bloodsea.{cid}.{o[0]}"] = o[idx]
        for k, v in SL.OBJECTIVES.items():
            d[f"obj.bloodsea.{k}"] = v[idx - 1]
        for k, v in SL.ENDINGS.items():
            d[f"endname.bloodsea.{k}"] = v[idx - 1]
            d[f"ending.bloodsea.{k}.title"] = v[idx - 1]
            for i, line in enumerate(SL.ENDING_TEXT[k]):
                d[f"ending.bloodsea.{k}.{i}"] = line[idx - 1]
        names = {"elias": ("Elias", "Elias"), "anna": ("Anna", "Anna"), "voice": ("Anna?", "Anna?"), "nurse": ("Nurse", "Hemşire"),
                 "rescuer": ("Fisherman", "Balıkçı"), "man": ("Crewman", "Tayfa")}
        for k, v in names.items():
            d[f"speaker.bloodsea.{k}"] = v[idx - 1]
        extra = {"subtitles.bloodsea.pip_meow": ("Pip meows", "Pip miyavlıyor"), "subtitles.bloodsea.pip_chirp": ("Pip chirps", "Pip mırıldanıyor"),
                 "subtitles.bloodsea.pip_purr": ("Pip purrs", "Pip mırlıyor"), "subtitles.bloodsea.pip_hiss": ("Pip hisses", "Pip tıslıyor"),
                 "subtitles.bloodsea.pip_scream": ("Pip screams", "Pip çığlık atıyor"), "subtitles.bloodsea.blood_splash": ("Blood splashes", "Kan sıçrıyor"),
                 "subtitles.bloodsea.monitor_beep": ("A monitor beeps", "Bir monitör bipliyor"), "subtitles.bloodsea.storm": ("A storm", "Fırtına"),
                 "subtitles.bloodsea.stomach": ("Something churns", "Bir şey çalkalanıyor"), "subtitles.bloodsea.heartbeat_near": ("A heartbeat", "Kalp atışı"),
                 "entity.bloodsea.pip": ("Pip", "Pip"), "entity.bloodsea.elias": ("Elias", "Elias"), "item.bloodsea.pip_fish": ("Pip's fish", "Pip'in balığı"),
                 "item.bloodsea.sea_map": ("Hand-drawn map", "Elle çizilmiş harita"), "entity.bloodsea.crate": ("Floating crate", "Yüzen sandık"),
                 "hud.bloodsea.objective": ("Objective", "Görev"), "hud.bloodsea.skip": ("Hold SPACE to skip", "Geçmek için BOŞLUK'a basılı tut"),
                 "hud.bloodsea.choose": ("Choose", "Seç"), "hud.bloodsea.crate": ("Use: pull the crate aboard", "Kullan: sandığı sala çek"),
                 "hud.bloodsea.oar": ("Use: sit at the oar", "Kullan: küreğe otur"), "hud.bloodsea.pet": ("Use: stroke Pip", "Kullan: Pip'i sev"),
                 "hud.bloodsea.ending": ("Ending", "Son"), "hud.bloodsea.endings_seen": ("%s of 4 endings", "4 sondan %s tanesi")}
        for k, v in extra.items():
            d[k] = v[idx - 1]
        json.dump(d, open(p, "w", encoding="utf-8"), ensure_ascii=False, indent=1)


def write_java(durs):
    os.makedirs(J, exist_ok=True)
    rows = ",\n".join(f'            e("{k}", "{SL.LINES[k][0]}", {durs[k]})' for k in SL.LINES)
    ch = ",\n".join('            Map.entry("%s", new String[]{%s})' % (c, ", ".join(f'"{o[0]}"' for o in opts)) for c, opts in SL.CHOICES.items())
    src = f'''package com.krolasyon.bloodsea.story;

import java.util.HashMap;
import java.util.Map;

/** Generated by tools/gen_story_audio.py: every spoken line, who says it and how long it lasts (ticks); the choices. */
public final class Lines {{
    public record Line(String key, String speaker, int ticks) {{}}

    public static final Map<String, Line> ALL = new HashMap<>();
    public static final Map<String, String[]> CHOICES = Map.ofEntries(
{ch});

    private static Line e(String k, String s, int t) {{ Line l = new Line(k, s, t); ALL.put(k, l); return l; }}

    static {{
        Line[] all = {{
{rows}
        }};
    }}

    public static int ticks(String key) {{ Line l = ALL.get(key); return l == null ? 60 : l.ticks(); }}
    public static String sound(String key) {{ return "v_" + key.replace('.', '_'); }}

    private Lines() {{}}
}}
'''
    open(os.path.join(J, "Lines.java"), "w").write(src)


def main():
    if "--lang" in sys.argv:
        d = {}
        import re
        for m in re.finditer(r'e\("([^"]+)", "[^"]+", (\d+)\)', open(os.path.join(J, "Lines.java")).read()):
            d[m.group(1)] = int(m.group(2))
        write_lang(d)
        return
    # the lines are not spoken any more (subtitles only); --voices makes the recordings again, for reference
    if "--voices" in sys.argv:
        sounds, durs = tts_all()
    else:
        import re as _re
        sounds, durs = {}, {}
        for m in _re.finditer(r'e\("([^"]+)", "[^"]+", (\d+)\)', open(os.path.join(J, "Lines.java")).read()):
            durs[m.group(1)] = int(m.group(2))
    sounds.update(misc())
    p = os.path.join(A, "sounds.json")
    d = json.load(open(p))
    d.update(sounds)
    json.dump(d, open(p, "w"), indent=1)
    write_lang(durs)
    write_java(durs)
    print(len(durs), "lines")


if __name__ == "__main__":
    main()
