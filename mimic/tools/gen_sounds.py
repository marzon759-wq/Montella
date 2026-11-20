#!/usr/bin/env python3
"""Every sound of MIMIC: Harrow Farm.

Voices: synthesised with the Kokoro TTS model (Apache-2.0), then treated: the walkie (band-limited, crackle), the tape
(wow and hiss), the telephone, and the mimic itself (pitched wrong, doubled, a growl under the words, reverb).
Effects: CC0 recordings from OpenGameArt (see CREDITS.md) and the CC0 set already cut for The Hatman, re-treated.

usage: gen_sounds.py <tts_dir with kokoro.onnx+voices.bin> <raw_dir> <hatman sounds dir>
"""
import json, os, random, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from gen_lang import VOICES

TTS, RAW, HM = sys.argv[1], sys.argv[2], sys.argv[3]
A = os.path.join(HERE, "../common/src/main/resources/assets/mimicry")
SND = os.path.join(A, "sounds")
WORK = os.path.join(TTS, "work")
os.makedirs(SND, exist_ok=True)
os.makedirs(WORK, exist_ok=True)
NORM = "loudnorm=I=-16:TP=-1.5:LRA=11"


def run(cmd):
    try:
        r = subprocess.run(cmd, capture_output=True, text=True, timeout=90)
    except subprocess.TimeoutExpired:
        print("TIMEOUT", cmd[-1])
        return False
    if r.returncode != 0:
        print("FAILED", " ".join(cmd[-1:]), r.stderr[-500:])
        return False
    return True


def ff(out, inputs, filt, norm=True):
    """inputs: list of (path, pre-args); filt: filter_complex producing [x]"""
    cmd = ["ffmpeg", "-nostdin", "-v", "error", "-y"]
    for p, pre in inputs:
        cmd += list(pre) + ["-i", p]
    fc = filt + (f";[x]{NORM}[o]" if norm else ";[x]anull[o]")
    cmd += ["-filter_complex", fc, "-map", "[o]", "-ac", "1", "-ar", "44100", "-c:a", "libvorbis", "-q:a", "4", os.path.join(SND, out + ".ogg")]
    run(cmd)
    return out


def one(out, path, chain, norm=True, ss=None, t=None):
    pre = []
    if ss is not None:
        pre += ["-ss", str(ss)]
    if t is not None:
        pre += ["-t", str(t)]
    return ff(out, [(path, pre)], "[0:a]aformat=channel_layouts=mono,aresample=44100," + chain + "[x]", norm)


def pitch(k):
    return f"asetrate=44100*{k},aresample=44100"


E = {}


def ev(name, *files, stream=False, sub=None):
    E[name] = {"files": list(files), "stream": stream}


def raw(p):
    return os.path.join(RAW, p)


def hm(p):
    return os.path.join(HM, p + ".ogg")


# =========================================================================================== voices
SPEAKER = {"lily": "af_bella", "mlily": "af_bella", "dad": "am_onyx", "mdad": "am_onyx", "sam": "am_liam", "msam": "am_liam"}
SPEED = {"lily": 0.97, "mlily": 0.9, "dad": 0.86, "mdad": 0.82, "sam": 1.0, "msam": 0.88}


def tts_all():
    from kokoro_onnx import Kokoro
    import soundfile as sf
    k = Kokoro(os.path.join(TTS, "kokoro.onnx"), os.path.join(TTS, "voices.bin"))
    for key, text in VOICES.items():
        who = key.split(".")[1]
        wav = os.path.join(WORK, key + ".wav")
        if os.path.exists(wav):
            continue
        spoken = text.replace("(whisper)", "").replace("...", ", ")
        s, sr = k.create(spoken, voice=SPEAKER[who], speed=SPEED[who], lang="en-us")
        sf.write(wav, s, sr)
        print("tts", key)


# the walkie: band-limited, a little crushed, squelch at both ends
RADIO = "highpass=f=380,lowpass=f=3200,acompressor=threshold=-20dB:ratio=6,volume=1.6,aecho=0.6:0.3:12:0.2"
TAPE = "highpass=f=160,lowpass=f=5200,vibrato=f=0.6:d=0.06,aecho=0.8:0.5:30:0.15"
PHONE = "highpass=f=500,lowpass=f=2800,acompressor=threshold=-22dB:ratio=8,volume=1.8"
ROOM = "aecho=0.8:0.72:60|130:0.28|0.16"
FAR = "lowpass=f=4200,aecho=0.8:0.8:180|420:0.35|0.22"


def mimic_chain(k):
    """the thing wearing a voice: a little too slow, a little too low, doubled with a second throat a fifth below"""
    return (f"asplit=3[a][b][c];[a]{pitch(0.94)},vibrato=f=5.5:d=0.18[a1];[b]{pitch(0.66)},volume=0.55,lowpass=f=1200[b1];"
            f"[c]areverse,{pitch(0.8)},volume=0.18,areverse[c1];[a1][b1][c1]amix=inputs=3:normalize=0,flanger=delay=2:depth=3:speed=0.4,{k}")


def voices():
    tts_all()
    hiss = raw("crickets-ambient-noise-loopable--crickets_1.mp3")
    for key in VOICES:
        who = key.split(".")[1]
        src = os.path.join(WORK, key + ".wav")
        out = key.replace(".", "_")
        name = key.split(".", 1)[1]
        if who == "lily":
            radio = name.startswith("r") or name in ("field1",)
            chain = RADIO if (name.startswith("r") and name[1:].isdigit()) else ROOM if not name.startswith("vm") else PHONE
            if name.startswith("vm"):
                chain = PHONE
            ff(out, [(src, [])], f"[0:a]aformat=channel_layouts=mono,aresample=44100,apad=pad_dur=0.25,{chain}[x]")
        elif who == "dad":
            chain = TAPE if name.startswith("tape") else PHONE
            ff(out, [(src, []), (hm("fx_static0"), ["-stream_loop", "-1"])],
               f"[0:a]aformat=channel_layouts=mono,aresample=44100,apad=pad_dur=0.4,{chain}[v];[1:a]aformat=channel_layouts=mono,aresample=44100,volume=0.04,highpass=f=2000[n];"
               f"[v][n]amix=inputs=2:duration=first:normalize=0[x]")
        elif who == "sam":
            ff(out, [(src, [])], "[0:a]aformat=channel_layouts=mono,aresample=44100," + ("volume=1.3" if name in ("loud", "ask", "lily") else "volume=0.8,lowpass=f=6000") + "[x]")
        elif who in ("mlily", "mdad", "msam"):
            tail = PHONE if name.startswith("phone") or name.startswith("machine") else FAR if name.startswith("call") else ROOM
            ff(out, [(src, [])], f"[0:a]aformat=channel_layouts=mono,aresample=44100,apad=pad_dur=0.6,{mimic_chain(tail)}[x]")
        ev(key, out)


# =========================================================================================== the thing
def creature():
    # steps: heavy, clawed; the Hatman's steps lowered, with a click of nails on top
    for i in range(3):
        ff(f"mm_step{i}", [(hm(f"hm_step{i}"), []), (hm("fx_clock_tick"), ["-ss", str(0.1 * i), "-t", "0.2"])],
           f"[0:a]aformat=channel_layouts=mono,aresample=44100,{pitch(0.72)},lowpass=f=2500,volume=1.6[s];[1:a]aformat=channel_layouts=mono,aresample=44100,{pitch(1.6)},volume=0.7[c];"
           f"[s][c]amix=inputs=2:normalize=0[x]")
    ev("mm.step", "mm_step0", "mm_step1", "mm_step2")
    for i in range(2):
        one(f"mm_breath{i}", hm(f"hm_breath{i}"), f"{pitch(0.7)},lowpass=f=2400,{ROOM}")
    ev("mm.breath", "mm_breath0", "mm_breath1")
    for i in range(4):
        one(f"mm_growl{i}", hm(f"hm_growl{i}"), f"{pitch(0.68 + 0.04 * i)},bass=g=6,{ROOM}")
    ev("mm.growl", *[f"mm_growl{i}" for i in range(4)])
    # the roar: two screams, one an octave down, under a long growl
    ff("mm_roar0", [(hm("hm_scream0"), []), (hm("hm_growl1"), [])],
       f"[0:a]aformat=channel_layouts=mono,aresample=44100,asplit=2[a][b];[a]{pitch(0.78)}[a1];[b]{pitch(0.52)},volume=0.8[b1];[1:a]aformat=channel_layouts=mono,aresample=44100,{pitch(0.6)},volume=0.9[g];"
       f"[a1][b1][g]amix=inputs=3:normalize=0,{ROOM}[x]")
    ff("mm_roar1", [(hm("hm_scream1"), []), (hm("hm_growl3"), [])],
       f"[0:a]aformat=channel_layouts=mono,aresample=44100,asplit=2[a][b];[a]{pitch(0.74)}[a1];[b]{pitch(0.5)},volume=0.8[b1];[1:a]aformat=channel_layouts=mono,aresample=44100,{pitch(0.58)},volume=0.9[g];"
       f"[a1][b1][g]amix=inputs=3:normalize=0,{ROOM}[x]")
    ev("mm.roar", "mm_roar0", "mm_roar1")
    for i in range(2):
        one(f"mm_scream{i}", hm(f"hm_scream{i}"), f"asplit=2[a][b];[a]{pitch(0.92)}[a1];[b]{pitch(0.61)},volume=0.7[b1];[a1][b1]amix=inputs=2:normalize=0,{ROOM}")
    ev("mm.scream", "mm_scream0", "mm_scream1")
    # clicking: the throat working, like a dry wooden ratchet
    for i in range(3):
        ff(f"mm_click{i}", [(hm("fx_clock_tick"), ["-t", "1.4"]), (hm(f"hm_twitch{i % 2}"), [])],
           f"[0:a]aformat=channel_layouts=mono,aresample=44100,{pitch(0.55 + 0.1 * i)},atempo=1.8,volume=1.4[c];[1:a]aformat=channel_layouts=mono,aresample=44100,{pitch(0.7)},volume=0.5[t];"
           f"[c][t]amix=inputs=2:normalize=0,{ROOM}[x]")
    ev("mm.click", "mm_click0", "mm_click1", "mm_click2")
    for i in range(2):
        one(f"mm_sniff{i}", hm(f"hm_breath{i}"), f"atempo=2.0,{pitch(1.15)},highpass=f=600,volume=1.5,{ROOM}", t=2.2)
    ev("mm.sniff", "mm_sniff0", "mm_sniff1")
    crunch = os.path.join(RAW, "7-eating-crunches--crunch_-_tito")
    for i in range(3):
        ff(f"mm_eat{i}", [(os.path.join(crunch, f"crunch.{i + 1}.ogg"), []), (os.path.join(crunch, f"crunch.{i + 4}.ogg"), []),
                          (raw(f"squish-sounds-effects--squish_0{i + 1}{'_0' if i == 0 else ''}.mp3"), []), (hm("hm_growl2"), ["-t", "1.5"])],
           f"[0:a]aformat=channel_layouts=mono,aresample=44100,{pitch(0.8)}[a];[1:a]aformat=channel_layouts=mono,aresample=44100,{pitch(0.7)},adelay=500[b];"
           f"[2:a]aformat=channel_layouts=mono,aresample=44100,{pitch(0.7)},adelay=250[c];[3:a]aformat=channel_layouts=mono,aresample=44100,{pitch(0.6)},volume=0.4[d];"
           f"[a][b][c][d]amix=inputs=4:normalize=0:duration=longest,{ROOM}[x]")
    ev("mm.eat", "mm_eat0", "mm_eat1", "mm_eat2")
    brk = os.path.join(RAW, "deep-bone-crackbreak-sfx--deep_breaks")
    ff("mm_kill", [(hm("hm_scream0"), []), (os.path.join(brk, "Deep Break 3.wav"), []), (os.path.join(brk, "Deep Break 7.wav"), []),
                   (raw("squish-sounds-effects--squish_02.mp3"), []), (hm("fx_jumpscare"), [])],
       f"[0:a]aformat=channel_layouts=mono,aresample=44100,{pitch(0.62)}[a];[1:a]aformat=channel_layouts=mono,aresample=44100,adelay=300[b];"
       f"[2:a]aformat=channel_layouts=mono,aresample=44100,adelay=700[c];[3:a]aformat=channel_layouts=mono,aresample=44100,adelay=500,volume=1.5[d];"
       f"[4:a]aformat=channel_layouts=mono,aresample=44100,volume=0.8[e];[a][b][c][d][e]amix=inputs=5:normalize=0:duration=longest[x]")
    ev("mm.kill", "mm_kill")
    for i in range(2):
        one(f"mm_climb{i}", hm(f"fx_creak{i}"), f"{pitch(1.3)},highpass=f=300,{ROOM}")
    ev("mm.climb", "mm_climb0", "mm_climb1")
    ff("mm_drop", [(hm("fx_thump"), []), (hm("hm_growl0"), ["-t", "1.2"])],
       f"[0:a]aformat=channel_layouts=mono,aresample=44100,{pitch(0.7)},volume=1.6[a];[1:a]aformat=channel_layouts=mono,aresample=44100,{pitch(0.6)},volume=0.6[b];[a][b]amix=inputs=2:normalize=0,{ROOM}[x]")
    ev("mm.drop", "mm_drop")
    one("mm_emerge", hm("hm_emerge"), f"{pitch(0.75)},{ROOM}")
    ev("mm.emerge", "mm_emerge")
    for i in range(3):
        one(f"mm_whisper{i}", hm(f"hm_whisper{i}"), f"{pitch(0.85)},{ROOM}")
    ev("mm.whisper", "mm_whisper0", "mm_whisper1", "mm_whisper2")
    # a howl far off across the fields: a scream stretched and lowered, lots of distance
    one("mm_howl0", hm("hm_scream1"), f"{pitch(0.5)},atempo=0.75,lowpass=f=1800,{FAR},afade=t=out:st=2.6:d=1.2", t=4)
    one("mm_howl1", hm("hm_scream0"), f"{pitch(0.46)},atempo=0.8,lowpass=f=1500,{FAR},afade=t=out:st=2.6:d=1.2", t=4)
    ev("mm.howl", "mm_howl0", "mm_howl1")
    for i in range(2):
        one(f"mm_bang{i}", hm(f"hm_bang{i}"), f"{pitch(0.8)},volume=1.5")
    ev("mm.bang", "mm_bang0", "mm_bang1")
    for i in range(2):
        one(f"mm_twitch{i}", hm(f"hm_twitch{i}"), f"{pitch(0.75)}")
    ev("mm.twitch", "mm_twitch0", "mm_twitch1")
    one("mm_grab", hm("hm_grab"), f"{pitch(0.7)},volume=1.4")
    ev("mm.grab", "mm_grab")


# =========================================================================================== effects and beds
def effects():
    simple = {
        "fx.door_creak": ["fx_door_creak0", "fx_door_creak1"], "fx.door_slam": ["fx_door_slam0", "fx_door_slam1"], "fx.lock": ["fx_lock"],
        "fx.unlock": ["fx_unlock"], "fx.drawer": ["fx_drawer0", "fx_drawer1"], "fx.phone_pick": ["fx_phone_pick"], "fx.phone_ring": ["fx_phone_ring"],
        "fx.tape": ["fx_tape"], "fx.thunder": ["fx_thunder0", "fx_thunder1"], "fx.creak": ["fx_creak0", "fx_creak1"], "fx.thump": ["fx_thump"],
        "fx.jumpscare": ["fx_jumpscare"], "fx.sting": ["fx_sting0", "fx_sting1"], "fx.chapter": ["fx_chapter"], "fx.glass": ["fx_glass"],
        "fx.wood_break": ["fx_wood_break0", "fx_wood_break1"], "fx.crow": ["fx_crow"], "fx.clock_tick": ["fx_clock_tick"],
        "fx.static": ["fx_static0", "fx_static1"], "fx.type": ["fx_type"], "fx.light_flicker": ["fx_bulb_pop"],
    }
    for e, fs in simple.items():
        outs = []
        for f in fs:
            one("x_" + f, hm(f), "anull", norm=False)
            outs.append("x_" + f)
        ev(e, *outs)
    one("fx_radio_static", hm("fx_static1"), "highpass=f=900,lowpass=f=5000,volume=0.8", t=1.6)
    ev("fx.radio_static", "fx_radio_static")
    one("fx_radio_click", hm("fx_switch"), f"{pitch(1.3)},highpass=f=600")
    ev("fx.radio_click", "fx_radio_click")
    one("fx_light_on", hm("fx_switch"), f"{pitch(1.5)},volume=1.2")
    ev("fx.light_on", "fx_light_on")
    one("fx_light_off", hm("fx_switch"), f"{pitch(1.2)},volume=1.0")
    ev("fx.light_off", "fx_light_off")
    one("fx_pickup", hm("fx_drawer0"), f"{pitch(1.6)},atempo=1.6", t=0.6)
    ev("fx.pickup", "fx_pickup")
    one("fx_paper", hm("fx_type"), f"{pitch(0.7)},lowpass=f=5000")
    ev("fx.paper", "fx_paper")
    one("fx_bolt_cut", hm("fx_lock"), f"{pitch(0.7)},volume=1.6,aecho=0.8:0.5:40:0.3")
    ev("fx.bolt_cut", "fx_bolt_cut")
    one("fx_crowbar", hm("fx_wood_break0"), f"{pitch(1.15)},volume=1.3")
    ev("fx.crowbar", "fx_crowbar")
    one("fx_bell", hm("fx_clock_chime") if os.path.exists(hm("fx_clock_chime")) else hm("fx_sting0"), f"{pitch(0.8)},aecho=0.8:0.9:400|900:0.5|0.3")
    ev("fx.bell", "fx_bell")
    one("fx_well", raw("squish-sounds-effects--squish_03.mp3"), f"{pitch(0.6)},aecho=0.8:0.9:220|500|900:0.6|0.4|0.25")
    ev("fx.well", "fx_well")
    one("fx_bucket", hm("fx_glass"), f"{pitch(0.55)},lowpass=f=3000,volume=1.2")
    ev("fx.bucket", "fx_bucket")
    one("fx_gas_pour", raw("fireplace-sound-loop--fire.wav"), "highpass=f=1200,lowpass=f=5000,volume=0.8,afade=t=in:d=0.3,afade=t=out:st=2.4:d=0.5", t=3, ss=10)
    ev("fx.gas_pour", "fx_gas_pour")
    one("fx_engine_fail", raw("car-engine-start-01--generic_car_startengine_1.wav"), "volume=1.2", t=2.5)
    ev("fx.engine_fail", "fx_engine_fail")
    one("fx_engine_start", raw("car-engine-start-up-02--engine_start_up_01.wav"), "volume=1.2")
    ev("fx.engine_start", "fx_engine_start")
    one("fx_fire", raw("fire-crackling--fire-1.wav"), "volume=1.4")
    ev("fx.fire", "fx_fire")
    one("fx_clock_chime", hm("fx_clock_chime") if os.path.exists(hm("fx_clock_chime")) else hm("fx_sting1"), "anull")
    ev("fx.clock_chime", "fx_clock_chime")
    one("fx_breath_hold", hm("hm_breath0"), f"{pitch(1.3)},highpass=f=300,volume=0.7", t=1.5)
    ev("fx.breath_hold", "fx_breath_hold")
    one("fx_gasp", hm("hm_breath1"), f"{pitch(1.45)},atempo=1.4,volume=1.4", t=1.0)
    ev("fx.gasp", "fx_gasp")
    one("fx_qte", hm("fx_sting1"), "volume=0.9", t=1.5)
    ev("fx.qte", "fx_qte")
    one("fx_choice", hm("fx_sting0"), f"{pitch(1.2)},volume=0.8", t=1.6)
    ev("fx.choice", "fx_choice")
    one("fx_hay", raw("squish-sounds-effects--squish_01_0.mp3"), f"{pitch(1.8)},highpass=f=1500,volume=1.2")
    ev("fx.hay", "fx_hay")
    one("fx_corn", hm("fx_drawer1"), f"{pitch(1.9)},highpass=f=1800,volume=0.8")
    ev("fx.corn", "fx_corn")
    one("fx_stove", hm("fx_door_creak1"), f"{pitch(1.4)},volume=1.0", t=1.2)
    ev("fx.stove", "fx_stove")
    one("fx_water", raw("fireplace-sound-loop--fire.wav"), "highpass=f=600,lowpass=f=2500,volume=0.8,afade=t=out:st=1.5:d=0.5", t=2, ss=30)
    ev("fx.water", "fx_water")
    one("fx_cow", hm("hm_growl3"), f"{pitch(1.25)},lowpass=f=1400,{FAR}")
    ev("fx.cow", "fx_cow")
    one("fx_chain", hm("fx_keypad") if os.path.exists(hm("fx_keypad")) else hm("fx_lock"), f"{pitch(0.8)},aecho=0.8:0.6:40:0.4")
    ev("fx.chain", "fx_chain")
    one("fx_truck_door", hm("fx_door_slam0"), f"{pitch(1.2)},highpass=f=200")
    ev("fx.truck_door", "fx_truck_door")
    ff("fx_gore", [(raw("squish-sounds-effects--squish_02.mp3"), []), (os.path.join(RAW, "deep-bone-crackbreak-sfx--deep_breaks", "Deep Break 1.wav"), [])],
       "[0:a]aformat=channel_layouts=mono,aresample=44100[a];[1:a]aformat=channel_layouts=mono,aresample=44100,volume=0.6,adelay=120[b];[a][b]amix=inputs=2:normalize=0[x]")
    ev("fx.gore", "fx_gore")
    one("fx_stab", os.path.join(RAW, "deep-bone-crackbreak-sfx--deep_breaks", "Deep Break 5.wav"), "volume=1.4")
    ev("fx.stab", "fx_stab")
    one("fx_lighter", hm("fx_switch"), f"{pitch(0.8)},volume=1.2")
    ev("fx.lighter", "fx_lighter")
    one("fx_throw", hm("hm_grab"), f"{pitch(1.4)},volume=0.8")
    ev("fx.throw", "fx_throw")
    # beds (looping, streamed)
    one("amb_wind", raw("storm-arwen-2022--storm_arwen_2022.flac"), "lowpass=f=2400,volume=0.9,afade=t=in:d=2,afade=t=out:st=58:d=2", t=60, ss=30)
    ev("amb.wind", "amb_wind", stream=True)
    ff("amb_field", [(raw("crickets-ambient-noise-loopable--crickets_1.mp3"), ["-stream_loop", "3"]), (raw("wind-whoosh-loop--wind%20woosh%20loop.ogg"), ["-stream_loop", "6"])],
       "[0:a]aformat=channel_layouts=mono,aresample=44100,volume=0.5,highpass=f=2500[c];[1:a]aformat=channel_layouts=mono,aresample=44100,lowpass=f=1800,volume=0.8[w];"
       "[c][w]amix=inputs=2:normalize=0:duration=shortest,atrim=0:40[x]")
    ev("amb.field", "amb_field", stream=True)
    one("amb_house", hm("amb_house"), "lowpass=f=1500,volume=0.9")
    ev("amb.house", "amb_house", stream=True)
    ff("amb_barn", [(hm("amb_house"), []), (hm("fx_creak0"), ["-stream_loop", "6"])],
       f"[0:a]aformat=channel_layouts=mono,aresample=44100,{pitch(0.85)},lowpass=f=1200[a];[1:a]aformat=channel_layouts=mono,aresample=44100,{pitch(0.8)},volume=0.25,adelay=3000[b];"
       "[a][b]amix=inputs=2:normalize=0:duration=first[x]")
    ev("amb.barn", "amb_barn", stream=True)
    one("amb_cellar", hm("amb_cellar"), "anull")
    ev("amb.cellar", "amb_cellar", stream=True)
    one("amb_silo", hm("amb_cellar"), f"{pitch(0.8)},aecho=0.8:0.9:300:0.4")
    ev("amb.silo", "amb_silo", stream=True)
    one("amb_chase", hm("amb_chase"), "anull")
    ev("amb.chase", "amb_chase", stream=True)
    one("amb_heartbeat", hm("amb_heartbeat"), "anull")
    ev("amb.heartbeat", "amb_heartbeat", stream=True)
    # flies: high buzzing from the crickets bed, pitched and modulated
    one("amb_flies", raw("crickets-ambient-noise-loopable--crickets_1.mp3"), f"{pitch(0.55)},tremolo=f=11:d=0.5,lowpass=f=1800,volume=1.2")
    ev("amb.flies", "amb_flies", stream=True)


def main():
    for f in os.listdir(SND):
        os.remove(os.path.join(SND, f))
    voices()
    creature()
    effects()
    sj = {}
    for name, d in E.items():
        sounds = []
        for f in d["files"]:
            s = {"name": f"mimicry:{f}"}
            if d["stream"]:
                s["stream"] = True
            sounds.append(s)
        sj[name] = {"sounds": sounds}
        if name.startswith("v."):
            sj[name]["subtitle"] = "sub." + name
    json.dump(sj, open(os.path.join(A, "sounds.json"), "w"), indent=1)
    lengths = {}
    for name, d in E.items():
        if name.startswith("v."):
            p = os.path.join(SND, d["files"][0] + ".ogg")
            out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", p], capture_output=True, text=True).stdout.strip()
            try:
                lengths[name] = int(float(out) * 20)
            except ValueError:
                pass
    json.dump(lengths, open(os.path.join(A, "voice_lengths.json"), "w"), indent=1)
    missing = [f for d in E.values() for f in d["files"] if not os.path.exists(os.path.join(SND, f + ".ogg"))]
    print("events", len(E), "files", len(os.listdir(SND)), "missing", missing)


if __name__ == "__main__":
    main()
