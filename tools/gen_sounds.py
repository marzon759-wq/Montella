#!/usr/bin/env python3
"""Cuts, layers and masters the sound effects. All sources are CC0 recordings from OpenGameArt (see CREDITS.md),
plus two clips from the previous Hatman mod. Nothing here is synthesised.

usage: gen_sounds.py <raw_dir> <old_mod_sounds_dir> <assets/hatman dir>
"""
import json, os, subprocess, sys

RAW, OLD, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
SND = os.path.join(OUT, "sounds")
os.makedirs(SND, exist_ok=True)
G = "x_qubodup-GhostMoans/qubodup-GhostMoans/wav/qubodup-GhostMoan0%d.wav"
D = "x_qubodup-DoorSet/qubodup-DoorSet/flac/qubodup-Door%s.flac"
WD = "x_independent_nu_ljudbank-wood_crack_hit_destruction/wood_impact/%s.mp3.flac"
AT = "x_independent_nu_ljudbank-atmosphere_moody/atmosmoody/atmoseerie0%d.mp3.flac"
V = "x_vocal_samples/%s.ogg"
M = "x_monster-sounds-volume-2/Monster-Sounds-Volume-2/%s.wav"
NORM = "loudnorm=I=-17:TP=-1.5:LRA=11"


def ff(out, inputs, filt, norm=True, extra=()):
    """inputs: list of (file, [pre-input args]); filt: filter_complex producing [o]"""
    cmd = ["ffmpeg", "-v", "error", "-y"]
    for f in inputs:
        if isinstance(f, tuple):
            cmd += list(f[1]) + ["-i", os.path.join(RAW, f[0])]
        else:
            cmd += ["-i", os.path.join(OLD if f.startswith("OLD:") else RAW, f[4:] if f.startswith("OLD:") else f)]
    fc = filt + ("[pre];[pre]" + NORM + "[o]" if norm else "[o]")
    cmd += ["-filter_complex", fc, "-map", "[o]", "-ac", "1", "-ar", "44100", "-c:a", "libvorbis", "-q:a", "3"] + list(extra) + [os.path.join(SND, out + ".ogg")]
    r = subprocess.run(cmd, capture_output=True, text=True)
    if r.returncode != 0:
        print("FAILED", out, r.stderr[-400:])
    return out


def one(out, src, chain, norm=True, ss=None, t=None):
    pre = []
    if ss is not None:
        pre += ["-ss", str(ss)]
    if t is not None:
        pre += ["-t", str(t)]
    return ff(out, [(src, pre)] if not src.startswith("OLD:") else [src], "[0:a]aformat=channel_layouts=mono," + chain, norm)


def pitch(k):
    return f"asetrate=44100*{k},aresample=44100"


E = {}  # event -> list of files


def ev(name, *files):
    E[name] = list(files)


# ---------------- ambience beds
ev("amb.rain", one("amb_rain", "x_Rain_OGG/3.ogg", "aresample=44100,highpass=f=120,afade=t=in:d=1.5,afade=t=out:st=43:d=2,volume=0.8", t=45))
ev("amb.house", ff("amb_house", [("drone55.wav", ["-ss", "8", "-t", "40"]), (AT % 2, [])],
                   "[0:a]aformat=channel_layouts=mono,aresample=44100,lowpass=f=700,volume=0.9[a];[1:a]aformat=channel_layouts=mono,aresample=44100,aloop=loop=1:size=882000,lowpass=f=1800,volume=0.5[b];"
                   "[a][b]amix=inputs=2:duration=first,afade=t=in:d=3,afade=t=out:st=36:d=4"))
ev("amb.cellar", ff("amb_cellar", [("drone57.wav", ["-ss", "20", "-t", "44"]), (AT % 3, [])],
                    "[0:a]aformat=channel_layouts=mono,aresample=44100," + pitch(0.8) + ",lowpass=f=500[a];[1:a]aformat=channel_layouts=mono,aresample=44100," + pitch(0.7) + ",aloop=loop=1:size=1300000,volume=0.6[b];"
                    "[a][b]amix=inputs=2:duration=shortest,aecho=0.8:0.7:240|420:0.4|0.25,afade=t=in:d=3,afade=t=out:st=30:d=4"))
ev("amb.chase", ff("amb_chase", [("excited_horror_sound.wav", []), ("heartbeat_fast_0.wav", []), ("drone60.wav", ["-ss", "30", "-t", "14"])],
                   "[0:a]aformat=channel_layouts=mono,aresample=44100,aloop=loop=1:size=289700[a];[1:a]aformat=channel_layouts=mono,aresample=44100,aloop=loop=11:size=49612,lowpass=f=260,volume=2.2[b];"
                   "[2:a]aformat=channel_layouts=mono,aresample=44100,volume=0.7[c];[a][b][c]amix=inputs=3:duration=shortest:normalize=0,afade=t=in:d=0.4,afade=t=out:st=12.3:d=0.8"))
ev("amb.heartbeat", one("amb_heartbeat", "heartbeat_slow_0.wav", "aresample=44100,lowpass=f=320,volume=1.6"))
ev("amb.generator", one("amb_generator", "loop_generator_1.mp3", "aresample=44100,lowpass=f=2400"))
ev("amb.breath", one("amb_breath", "breathing_tired.wav", "aresample=44100," + pitch(0.92) + ",highpass=f=200,afade=t=in:d=0.1,afade=t=out:st=3.2:d=0.2"))
E["amb.fire"] = ["minecraft:fire/fire"]

# ---------------- the hatman
ev("hm.step", *[one(f"hm_step{i}", f"0{i+3}-footstep{'' if i else ''}.ogg", "aresample=44100," + pitch(0.72) + ",lowpass=f=1500,bass=g=8:f=110,aecho=0.8:0.5:60:0.25") for i in range(3)])
ev("hm.breath", one("hm_breath0", "ghostbreath.flac", "aresample=44100," + pitch(0.85) + ",afade=t=out:st=3.5:d=0.5"),
   one("hm_breath1", "x_horrorbreathing/horrorbreathing/horrorbreathing.ogg", "aresample=44100," + pitch(0.9) + ",afade=t=in:d=0.2,afade=t=out:st=3.6:d=0.6", t=3.8))
ev("hm.growl", *[one(f"hm_growl{i}", f"silent_beast_growl{'' if i == 0 else '_' + str(i)}.mp3", "aresample=44100," + pitch(0.82) + ",bass=g=5:f=140,aecho=0.8:0.6:90:0.3") for i in range(4)])
ev("hm.scream", ff("hm_scream0", [("scream_horror1_0.mp3", ["-t", "3.4"]), ("monster-snarl-attack.ogg", ["-t", "3.0"])],
                   "[0:a]aformat=channel_layouts=mono,aresample=44100,volume=1.0[a];[1:a]aformat=channel_layouts=mono,aresample=44100," + pitch(0.8) + ",volume=1.2[b];"
                   "[a][b]amix=inputs=2:duration=longest:normalize=0,aecho=0.8:0.7:110|190:0.35|0.2,afade=t=out:st=2.6:d=0.8"),
   ff("hm_scream1", [("scream_horror1_0.mp3", ["-ss", "4.0", "-t", "3.0"]), ("monsters-snarls-3_0.ogg", ["-ss", "2", "-t", "3.0"])],
      "[0:a]aformat=channel_layouts=mono,aresample=44100," + pitch(0.92) + "[a];[1:a]aformat=channel_layouts=mono,aresample=44100," + pitch(0.78) + ",volume=1.1[b];"
      "[a][b]amix=inputs=2:duration=longest:normalize=0,aecho=0.8:0.7:110|190:0.35|0.2,afade=t=in:d=0.05,afade=t=out:st=2.4:d=0.8"))
ev("hm.whisper", one("hm_whisper0", G % 5, "aresample=44100,highpass=f=500," + pitch(1.1) + ",aecho=0.8:0.8:140|260:0.4|0.25,volume=0.8"),
   one("hm_whisper1", G % 1, "aresample=44100,highpass=f=400,aecho=0.8:0.8:140|260:0.4|0.25,afade=t=out:st=2.3:d=0.6,volume=0.8"),
   one("hm_whisper2", "OLD:whisper.ogg", "aresample=44100,aecho=0.8:0.7:120:0.3"))
ev("hm.emerge", ff("hm_emerge", [(G % 3, ["-t", "2.6"]), (WD % "crack03", [])],
                   "[0:a]aformat=channel_layouts=mono,aresample=44100," + pitch(0.7) + "[a];[1:a]aformat=channel_layouts=mono,aresample=44100," + pitch(0.6) + ",volume=0.8[b];"
                   "[a][b]amix=inputs=2:duration=longest:normalize=0,aecho=0.8:0.7:160:0.35,afade=t=out:st=3.0:d=0.7"))
ev("hm.vanish", ff("hm_vanish", [("ScatterNoise1.mp3", ["-ss", "3", "-t", "1.1"]), (G % 5, [])],
                   "[0:a]aformat=channel_layouts=mono,aresample=44100,afade=t=out:st=0.3:d=0.8[a];[1:a]aformat=channel_layouts=mono,aresample=44100," + pitch(0.6) + ",volume=0.9[b];"
                   "[a][b]amix=inputs=2:duration=longest:normalize=0,afade=t=out:st=1.6:d=0.9"))
ev("hm.knock", ff("hm_knock", [(WD % "impactwood03", ["-t", "0.34"]), (WD % "impactwood03", ["-t", "0.34"]), (WD % "impactwood03", ["-t", "0.5"])],
                  "[0:a]aformat=channel_layouts=mono,aresample=44100,afade=t=out:st=0.2:d=0.14[a];[1:a]aformat=channel_layouts=mono,aresample=44100,afade=t=out:st=0.2:d=0.14,adelay=420[b];"
                  "[2:a]aformat=channel_layouts=mono,aresample=44100,afade=t=out:st=0.3:d=0.2,adelay=840[c];[a][b][c]amix=inputs=3:duration=longest:normalize=0," + pitch(0.85) + ",aecho=0.8:0.6:70:0.3"))
ev("hm.twitch", one("hm_twitch0", WD % "crack07", "aresample=44100," + pitch(1.35) + ",afade=t=out:st=0.35:d=0.2", t=0.8),
   one("hm_twitch1", WD % "crack09", "aresample=44100," + pitch(1.5) + ",afade=t=out:st=0.35:d=0.2", t=0.8))
ev("hm.grab", ff("hm_grab", [(M % "Monster-1", []), ("silent_beast_growl_1.mp3", [])],
                 "[0:a]aformat=channel_layouts=mono,aresample=44100," + pitch(0.7) + "[a];[1:a]aformat=channel_layouts=mono,aresample=44100," + pitch(0.75) + "[b];[a][b]amix=inputs=2:duration=longest:normalize=0"))
ev("hm.voice", *[one(f"hm_voice{i}", V % f"demon_{i+1}", "aresample=44100,bass=g=4:f=160") for i in range(4)])
ev("hm.bang", one("hm_bang0", WD % "impactwood01", "aresample=44100," + pitch(0.75) + ",bass=g=7:f=90,afade=t=out:st=1.2:d=0.5", t=1.8),
   one("hm_bang1", WD % "impactwood02", "aresample=44100," + pitch(0.7) + ",bass=g=7:f=90,afade=t=out:st=1.2:d=0.5", t=1.8))
ev("hm.search", one("hm_search0", G % 4, "aresample=44100," + pitch(0.8) + ",aecho=0.8:0.7:150:0.3,volume=0.8"),
   one("hm_search1", "monster-snarls-2_0.ogg", "aresample=44100," + pitch(0.8) + ",afade=t=out:st=3.4:d=0.8,volume=0.8", t=3.6))

# ---------------- world
ev("fx.static", one("fx_static0", "static1.wav", "aresample=44100,afade=t=in:d=0.05,afade=t=out:st=1.9:d=0.5", ss=5, t=2.4),
   one("fx_static1", "static5.wav", "aresample=44100,afade=t=in:d=0.05,afade=t=out:st=1.9:d=0.5", ss=9, t=2.4))
ev("fx.jumpscare", ff("fx_jumpscare", [("monster-snarl-attack.ogg", ["-t", "2.4"]), ("static3.wav", ["-ss", "4", "-t", "2.4"]), (WD % "impactwood04", []), ("scream_horror1_0.mp3", ["-t", "2.4"])],
                      "[0:a]aformat=channel_layouts=mono,aresample=44100,volume=1.3[a];[1:a]aformat=channel_layouts=mono,aresample=44100,volume=0.5[b];"
                      "[2:a]aformat=channel_layouts=mono,aresample=44100," + pitch(0.5) + ",volume=1.6[c];[3:a]aformat=channel_layouts=mono,aresample=44100,volume=0.9[d];"
                      "[a][b][c][d]amix=inputs=4:duration=longest:normalize=0,afade=t=out:st=1.8:d=0.6,alimiter=limit=0.9"))
ev("fx.creak", one("fx_creak0", "tree_creak.ogg", "aresample=44100," + pitch(0.85) + ",afade=t=in:d=0.05,afade=t=out:st=2.4:d=0.5", ss=0.2, t=2.6),
   one("fx_creak1", "tree_creak.ogg", "aresample=44100," + pitch(1.05) + ",afade=t=in:d=0.05,afade=t=out:st=2.0:d=0.5", ss=3.2, t=2.6))
ev("fx.door_slam", one("fx_door_slam0", D % "Close03", "aresample=44100," + pitch(0.8) + ",bass=g=8:f=100,aecho=0.8:0.6:80:0.35"),
   one("fx_door_slam1", D % "Close07", "aresample=44100," + pitch(0.75) + ",bass=g=8:f=100,aecho=0.8:0.6:80:0.35"))
ev("fx.door_creak", one("fx_door_creak0", D % "Open01", "aresample=44100," + pitch(0.6) + ",aecho=0.8:0.5:60:0.2"),
   one("fx_door_creak1", D % "Open04", "aresample=44100," + pitch(0.55) + ",aecho=0.8:0.5:60:0.2"))
ev("fx.lock", one("fx_lock", D % "Close09", "aresample=44100," + pitch(1.5) + ",afade=t=out:st=0.3:d=0.2", t=0.8))
ev("fx.unlock", ff("fx_unlock", [("light_switch_turn_on_0.mp3", []), (D % "Open02", ["-t", "0.7"])],
                   "[0:a]aformat=channel_layouts=mono,aresample=44100,volume=1.5[a];[1:a]aformat=channel_layouts=mono,aresample=44100,adelay=160,volume=0.7[b];[a][b]amix=inputs=2:duration=longest:normalize=0"))
ev("fx.drawer", one("fx_drawer0", D % "Open06", "aresample=44100," + pitch(1.3) + ",afade=t=out:st=0.5:d=0.2", t=0.9),
   one("fx_drawer1", D % "Open08", "aresample=44100," + pitch(1.2) + ",afade=t=out:st=0.5:d=0.2", t=0.9))
E["fx.paper"] = ["minecraft:item/book/open_flip1", "minecraft:item/book/open_flip2", "minecraft:item/book/open_flip3"]
E["fx.pickup"] = ["minecraft:random/pop"]
ev("fx.phone_ring", one("fx_phone_ring", "doorbell-old-tring.ogg", "aresample=44100,afade=t=out:st=2.2:d=0.5", t=2.7))
ev("fx.phone_pick", one("fx_phone_pick", "tape_recorder_opening.wav", "aresample=44100," + pitch(0.9)))
ev("fx.tv_on", ff("fx_tv_on", [("light_switch_turn_on_0.mp3", []), ("static10.wav", ["-ss", "3", "-t", "1.6"])],
                  "[0:a]aformat=channel_layouts=mono,aresample=44100,volume=1.4[a];[1:a]aformat=channel_layouts=mono,aresample=44100,afade=t=in:d=0.25,afade=t=out:st=0.9:d=0.7,adelay=120,volume=0.7[b];[a][b]amix=inputs=2:duration=longest:normalize=0"))
ev("fx.generator_start", ff("fx_generator_start", [("loop_generator_1.mp3", ["-t", "3.2"]), ("spark.wav", [])],
                            "[0:a]aformat=channel_layouts=mono,aresample=44100,afade=t=in:d=2.2,afade=t=out:st=2.7:d=0.5[a];[1:a]aformat=channel_layouts=mono,aresample=44100,volume=0.8[b];[a][b]amix=inputs=2:duration=longest:normalize=0"))
ev("fx.power_down", ff("fx_power_down", [("loop_generator_1.mp3", ["-t", "2.4"]), ("spark.wav", [])],
                       "[0:a]aformat=channel_layouts=mono,aresample=44100," + pitch(0.7) + ",afade=t=out:st=0.4:d=2.6,lowpass=f=1200[a];[1:a]aformat=channel_layouts=mono,aresample=44100,volume=1.2[b];[a][b]amix=inputs=2:duration=longest:normalize=0"))
ev("fx.switch", one("fx_switch", "light_switch_turn_off_0.mp3", "aresample=44100"))
ev("fx.bulb_pop", ff("fx_bulb_pop", [("spark.wav", []), ("glass_breaking.wav", ["-t", "0.7"])],
                     "[0:a]aformat=channel_layouts=mono,aresample=44100[a];[1:a]aformat=channel_layouts=mono,aresample=44100," + pitch(1.4) + ",volume=0.5[b];[a][b]amix=inputs=2:duration=longest:normalize=0"))
ev("fx.glass", one("fx_glass", "glass_breaking.wav", "aresample=44100"))
ev("fx.wood_break", ff("fx_wood_break0", [(WD % "crack01", []), (WD % "impactwood04", [])],
                       "[0:a]aformat=channel_layouts=mono,aresample=44100[a];[1:a]aformat=channel_layouts=mono,aresample=44100," + pitch(0.8) + "[b];[a][b]amix=inputs=2:duration=longest:normalize=0"),
   one("fx_wood_break1", WD % "crack04", "aresample=44100," + pitch(0.85) + ",bass=g=5:f=120"))
ev("fx.music_box", one("fx_music_box", "OLD:s_music_box.ogg", "aresample=44100,aecho=0.8:0.6:180:0.3"))
ev("fx.clock_tick", one("fx_clock_tick", "ticking_clock.wav", "aresample=44100,afade=t=out:st=3.5:d=0.5", t=4))
E["fx.clock_chime"] = ["minecraft:block/bell/bell_use01"]
ev("fx.cam_on", one("fx_cam_on", "tape_recorder_opening.wav", "aresample=44100," + pitch(1.15)))
ev("fx.cam_off", one("fx_cam_off", "tape_recorder_closing.wav", "aresample=44100," + pitch(1.15)))
E["fx.cam_low"] = ["minecraft:note/bit"]
ev("fx.keypad", one("fx_keypad", "light_switch_turn_on_0.mp3", "aresample=44100," + pitch(1.4)))
ev("fx.safe_open", ff("fx_safe_open", [(D % "Open08", []), (D % "Close09", ["-t", "0.5"])],
                      "[0:a]aformat=channel_layouts=mono,aresample=44100," + pitch(0.55) + ",adelay=350[a];[1:a]aformat=channel_layouts=mono,aresample=44100," + pitch(1.3) + "[b];[a][b]amix=inputs=2:duration=longest:normalize=0"))
ev("fx.fake_steps", one("fx_fake_steps0", "steps_in_wood_floor.ogg", "aresample=44100," + pitch(0.85) + ",lowpass=f=1800,aecho=0.8:0.6:90:0.3"),
   ff("fx_fake_steps1", [("03-footstep.ogg", []), ("04-footstep.ogg", []), ("05-footstep.ogg", []), ("03-footstep.ogg", [])],
      "[0:a]aformat=channel_layouts=mono,aresample=44100[a];[1:a]aformat=channel_layouts=mono,aresample=44100,adelay=620[b];[2:a]aformat=channel_layouts=mono,aresample=44100,adelay=1240[c];"
      "[3:a]aformat=channel_layouts=mono,aresample=44100,adelay=1900[d];[a][b][c][d]amix=inputs=4:duration=longest:normalize=0," + pitch(0.7) + ",lowpass=f=1400,aecho=0.8:0.6:90:0.3"))
ev("fx.crow", one("fx_crow", "crow_caw.wav", "aresample=44100,aecho=0.8:0.5:200:0.25"))
ev("fx.sting", one("fx_sting0", "x_horror_sfx/horror_effect2.wav", "aresample=44100,afade=t=in:d=0.02,afade=t=out:st=3.2:d=1.6", t=4.8),
   one("fx_sting1", AT % 4, "aresample=44100," + pitch(0.8) + ",afade=t=in:d=0.05,afade=t=out:st=3.0:d=1.8", t=4.8))
ev("fx.chapter", one("fx_chapter", WD % "impactwood02", "aresample=44100," + pitch(0.4) + ",aecho=0.8:0.8:320|640:0.5|0.3,bass=g=8:f=70,afade=t=out:st=3.5:d=2", t=3))
ev("fx.type", one("fx_type", "x_ticks/ticking clock - tick1.wav", "aresample=44100"))
ev("fx.voice_ruth", *[one(f"fx_voice_ruth{i}", V % f"female_standard_{i+1}", "aresample=44100") for i in range(4)])
ev("fx.thump", one("fx_thump", WD % "impactwood03", "aresample=44100," + pitch(0.55) + ",bass=g=8:f=80,aecho=0.8:0.6:120:0.3"))
E["fx.piano"] = ["minecraft:note/harp"]
ev("fx.tape", ff("fx_tape", [("tape_recorder_opening.wav", []), ("static3.wav", ["-ss", "2", "-t", "0.9"])],
                 "[0:a]aformat=channel_layouts=mono,aresample=44100[a];[1:a]aformat=channel_layouts=mono,aresample=44100,afade=t=in:d=0.1,afade=t=out:st=0.5:d=0.4,adelay=300,volume=0.4[b];[a][b]amix=inputs=2:duration=longest:normalize=0"))

# thunder: find the loudest moment of the storm recording and cut around it
import numpy as np
raw = subprocess.run(["ffmpeg", "-v", "error", "-i", os.path.join(RAW, "rain-thunder.ogg"), "-ac", "1", "-ar", "8000", "-f", "s16le", "-"], capture_output=True).stdout
a = np.frombuffer(raw, dtype=np.int16).astype(np.float32)
win = 4000
env = np.sqrt(np.convolve(a * a, np.ones(win) / win, mode="same"))
peak = int(np.argmax(env)) / 8000.0
start = max(0.0, peak - 1.0)
ev("fx.thunder", one("fx_thunder0", "rain-thunder.ogg", "aresample=44100,lowpass=f=3500,afade=t=in:d=0.4,afade=t=out:st=5.5:d=2.5", ss=round(start, 2), t=8),
   one("fx_thunder1", "rain-thunder.ogg", "aresample=44100," + pitch(0.8) + ",lowpass=f=2500,afade=t=in:d=0.4,afade=t=out:st=6.5:d=3", ss=round(start, 2), t=8))

stream = {"amb.rain", "amb.house", "amb.cellar", "amb.chase"}
sounds = {}
for name, files in E.items():
    lst = []
    for f in files:
        ref = f if f.startswith("minecraft:") else "hatman:" + f
        lst.append({"name": ref, "stream": True} if name in stream else ref)
    entry = {"sounds": lst}
    if not name.startswith("amb."):
        entry["subtitle"] = "subtitles.hatman." + name
    sounds[name] = entry
with open(os.path.join(OUT, "sounds.json"), "w") as f:
    json.dump(sounds, f, indent=1)
missing = [f for fs in E.values() for f in fs if not f.startswith("minecraft:") and not os.path.exists(os.path.join(SND, f + ".ogg"))]
print("events", len(E), "files", len(os.listdir(SND)), "missing", missing, "thunder peak at", round(peak, 1))
