#!/usr/bin/env python3
"""The Turkish voices: the same lines (lang_tr.VOICES_TR), recorded in Turkish and treated exactly like the English ones
(walkie, telephone, tape, the mimic's doubled throat). Adds "tr.v.*" events next to the English "v.*" ones; the client
plays them when the game language is Turkish (VoiceLang.java).

usage: gen_sounds_tr.py <dir with work/<event>.wav Turkish recordings> <hatman sounds dir>
"""
import json, os, subprocess, sys

HERE = os.path.dirname(os.path.abspath(__file__))
SRC, HMDIR = sys.argv[1], sys.argv[2]
sys.argv = [sys.argv[0], SRC, SRC, HMDIR]
sys.path.insert(0, HERE)
import gen_sounds as g
from lang_tr import VOICES_TR


def main():
    made = {}
    for key in VOICES_TR:
        src = os.path.join(SRC, "work", key + ".wav")
        if not os.path.exists(src):
            print("no recording", key)
            continue
        who = key.split(".")[1]
        name = key.split(".", 1)[1]
        out = "tr_" + key.replace(".", "_")
        if who == "lily":
            chain = g.RADIO if (name.startswith("r") and name[1:].isdigit()) else g.PHONE if name.startswith("vm") else g.ROOM
            g.ff(out, [(src, [])], f"[0:a]aformat=channel_layouts=mono,aresample=44100,apad=pad_dur=0.25,{chain}[x]")
        elif who == "dad":
            chain = g.TAPE if name.startswith("tape") else g.PHONE
            g.ff(out, [(src, []), (g.hm("fx_static0"), ["-stream_loop", "-1"])],
                 f"[0:a]aformat=channel_layouts=mono,aresample=44100,apad=pad_dur=0.4,{chain}[v];[1:a]aformat=channel_layouts=mono,aresample=44100,volume=0.04,highpass=f=2000[n];"
                 f"[v][n]amix=inputs=2:duration=first:normalize=0[x]")
        elif who == "sam":
            g.ff(out, [(src, [])], "[0:a]aformat=channel_layouts=mono,aresample=44100," + ("volume=1.3" if name in ("loud", "ask", "lily") else "volume=0.8,lowpass=f=6000") + "[x]")
        else:
            tail = g.PHONE if name.startswith("phone") or name.startswith("machine") else g.FAR if name.startswith("call") else g.ROOM
            g.ff(out, [(src, [])], f"[0:a]aformat=channel_layouts=mono,aresample=44100,apad=pad_dur=0.6,{g.mimic_chain(tail)}[x]")
        made["tr." + key] = out
    sj_p = os.path.join(g.A, "sounds.json")
    vl_p = os.path.join(g.A, "voice_lengths.json")
    sj = json.load(open(sj_p))
    vl = json.load(open(vl_p))
    for ev, f in made.items():
        sj[ev] = {"sounds": [{"name": f"mimicry:{f}"}], "subtitle": "sub." + ev[3:]}
        d = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", os.path.join(g.SND, f + ".ogg")],
                           capture_output=True, text=True).stdout.strip()
        vl[ev] = int(float(d) * 20)
    json.dump(sj, open(sj_p, "w"), indent=1)
    json.dump(vl, open(vl_p, "w"), indent=1)
    print("turkish voices", len(made), "of", len(VOICES_TR))


if __name__ == "__main__":
    main()
