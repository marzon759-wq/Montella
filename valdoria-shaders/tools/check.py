#!/usr/bin/env python3
"""Offline compile check for Valdoria Shaders: inlines Iris-style #include "/..." and runs glslangValidator."""
import os, re, subprocess, sys, tempfile

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "shaders")

def expand(path, seen=None):
    out = []
    for line in open(path):
        m = re.match(r'\s*#include\s+"(/[^"]+)"', line)
        if m:
            out.append(expand(os.path.join(ROOT, m.group(1).lstrip("/"))))
        else:
            out.append(line)
    return "".join(out)

bad = 0
for d in ("", "world-1", "world1"):
    folder = os.path.join(ROOT, d)
    for f in sorted(os.listdir(folder)):
        if not f.endswith((".vsh", ".fsh")):
            continue
        src = expand(os.path.join(folder, f))
        # Iris replaces this pseudo-directive; emulate the RENDERTARGETS-free build
        stage = "vert" if f.endswith(".vsh") else "frag"
        with tempfile.NamedTemporaryFile("w", suffix="." + stage, delete=False) as t:
            t.write(src)
        r = subprocess.run(["glslangValidator", "-S", stage, t.name], capture_output=True, text=True)
        os.unlink(t.name)
        if r.returncode != 0:
            bad += 1
            msgs = [l for l in r.stdout.splitlines() if "ERROR" in l][:6]
            print(f"{d}/{f}:\n  " + "\n  ".join(msgs))
print("failed:", bad)
sys.exit(1 if bad else 0)
