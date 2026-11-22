#!/usr/bin/env python3
"""KAIJU animations (GeckoLib / bedrock animation json).

  animation.kaiju.idle           4 s loop: breathing, neck and tail sway, a blink
  animation.kaiju.walk           2.4 s loop: heavy stomping gait
  animation.kaiju.roar           3.5 s: rears up, jaw wide, arms out, snarl
  animation.kaiju.atomic_breath  7 s: brace, the dorsal plates shoot out of the back one by one from the tail tip to
                                 the head (with a little overshoot, like the film), inhale and rear back, lunge and
                                 fire with the jaw wide open, then the plates slam back in from the head to the tail
  pose.roar                      static pose for stills

Plate sliders move along the plate's own axis (their mount bone carries the tilt), so position y = how far out.
A renderer can derive each plate's glow from that offset (offset / travel), so the glow wave follows the plates.
Writes out/kaiju.animation.json (reads out/plates.json).
"""
import json, math, os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")
PLATES = json.load(open(os.path.join(OUT, "plates.json")))
SIDES = (("l", 1), ("r", -1))
TAILS = [f"tail{i}" for i in range(1, 17)]
NECK = ["neck1", "neck2", "neck3"]


def smooth(f):
    f = min(1.0, max(0.0, f))
    return f * f * (3 - 2 * f)


def keyed(keys, t):
    """piecewise smooth interpolation through (time, value) keys; value is a 3-list"""
    if t <= keys[0][0]:
        return keys[0][1]
    for (t0, v0), (t1, v1) in zip(keys, keys[1:]):
        if t0 <= t <= t1:
            f = smooth((t - t0) / (t1 - t0)) if t1 > t0 else 1
            return [a + (b - a) * f for a, b in zip(v0, v1)]
    return keys[-1][1]


def sampled(fn, length, step=0.05):
    out = {}
    n = int(round(length / step))
    for i in range(n + 1):
        t = round(i * step, 4)
        v = fn(t)
        out[f"{t:g}"] = [round(x, 3) for x in v]
    return out


def channel(keys, length, step=0.05):
    return sampled(lambda t: keyed(keys, t), length, step)


class A:
    def __init__(self, length, loop):
        self.length = length
        self.loop = loop
        self.bones = {}

    def set(self, bone, kind, data):
        self.bones.setdefault(bone, {})[kind] = data

    def rot(self, bone, keys, step=0.05):
        self.set(bone, "rotation", channel(keys, self.length, step))

    def pos(self, bone, keys, step=0.05):
        self.set(bone, "position", channel(keys, self.length, step))

    def scale(self, bone, keys, step=0.05):
        self.set(bone, "scale", channel(keys, self.length, step))

    def fn(self, bone, kind, f, step=0.05):
        self.set(bone, kind, sampled(f, self.length, step))

    def json(self):
        return {"loop": self.loop, "animation_length": self.length, "bones": self.bones}


def idle():
    a = A(4.0, True)
    w = 2 * math.pi / 4.0
    a.fn("chest", "scale", lambda t: [1 + 0.015 * math.sin(w * t), 1 + 0.02 * math.sin(w * t), 1 + 0.02 * math.sin(w * t)])
    a.fn("belly", "scale", lambda t: [1 + 0.012 * math.sin(w * t - 0.4), 1, 1 + 0.015 * math.sin(w * t - 0.4)])
    for i, b in enumerate(NECK):
        a.fn(b, "rotation", lambda t, i=i: [1.5 * math.sin(w * t + i * 0.3), 2.5 * math.sin(w * t * 0.5 + i * 0.4), 0])
    a.fn("head", "rotation", lambda t: [-1.5 * math.sin(w * t + 1), 4 * math.sin(w * t * 0.5 + 1.4), 0])
    a.fn("jaw", "rotation", lambda t: [1.5 + 1.5 * math.sin(w * t), 0, 0])
    for i, b in enumerate(TAILS):
        a.fn(b, "rotation", lambda t, i=i: [0.8 * math.sin(w * t - i * 0.35), 2.2 * math.sin(w * 0.5 * t - i * 0.3), 0])
    for side, sg in SIDES:
        a.fn(f"arm_{side}", "rotation", lambda t, sg=sg: [-2 * math.sin(w * t), 0, sg * 1.5 * math.sin(w * t)])
        a.fn(f"eyelid_{side}", "position", lambda t: [0, -0.9 if 2.5 <= t <= 2.65 else 0, 0], step=0.05)
    return a


def walk():
    L = 2.4
    a = A(L, True)
    w = 2 * math.pi / L
    a.fn("body", "position", lambda t: [0, -1.2 * abs(math.sin(w * t)) + 0.4, 0])
    a.fn("body", "rotation", lambda t: [0, 4 * math.sin(w * t), 3 * math.sin(w * t)])
    a.fn("torso", "rotation", lambda t: [2 + 1.5 * abs(math.sin(w * t)), -3 * math.sin(w * t), -2 * math.sin(w * t)])
    a.fn("neck1", "rotation", lambda t: [-1.5 * abs(math.sin(w * t)), 2 * math.sin(w * t), 0])
    a.fn("head", "rotation", lambda t: [2 * abs(math.sin(w * t)) - 1, -2 * math.sin(w * t), 0])
    for side, sg in SIDES:
        ph = 0 if sg > 0 else math.pi
        a.fn(f"thigh_{side}", "rotation", lambda t, ph=ph: [16 * math.sin(w * t + ph), 0, 0])
        a.fn(f"shin_{side}", "rotation", lambda t, ph=ph: [-14 * max(0, math.sin(w * t + ph + 1.2)), 0, 0])
        a.fn(f"foot_{side}", "rotation", lambda t, ph=ph: [-10 * math.sin(w * t + ph) + 8 * max(0, math.sin(w * t + ph + 1.2)), 0, 0])
        a.fn(f"arm_{side}", "rotation", lambda t, ph=ph: [-6 * math.sin(w * t + ph), 0, 0])
    for i, b in enumerate(TAILS):
        a.fn(b, "rotation", lambda t, i=i: [1.2 * math.sin(2 * w * t - i * 0.3), -4 * math.sin(w * t - i * 0.32), 0])
    return a


def roar():
    L = 3.5
    a = A(L, False)
    k = lambda *pts: [(t, list(v)) for t, v in pts]
    a.rot("torso", k((0, [0, 0, 0]), (0.5, [5, 0, 0]), (1.0, [-8, 0, 0]), (2.8, [-8, 0, 0]), (3.5, [0, 0, 0])))
    for i, b in enumerate(NECK):
        a.rot(b, k((0, [0, 0, 0]), (0.5, [6, 0, 0]), (1.0, [-10, 0, 0]), (2.8, [-12, 0, 0]), (3.5, [0, 0, 0])))
    a.rot("head", k((0, [0, 0, 0]), (0.5, [8, 0, 0]), (1.05, [-24, 0, 0]), (2.8, [-26, 0, 0]), (3.5, [0, 0, 0])))
    a.fn("head", "position", lambda t: [0.15 * math.sin(t * 90) if 1.1 < t < 2.8 else 0, 0, 0], step=0.05)
    a.rot("jaw", k((0, [0, 0, 0]), (0.5, [4, 0, 0]), (1.05, [40, 0, 0]), (2.8, [42, 0, 0]), (3.3, [0, 0, 0])))
    a.pos("lip_upper", k((0, [0, 0, 0]), (1.0, [0, 0.8, 0]), (2.8, [0, 0.8, 0]), (3.3, [0, 0, 0])))
    a.rot("tongue1", k((0, [0, 0, 0]), (1.05, [-10, 0, 0]), (2.8, [-14, 0, 0]), (3.3, [0, 0, 0])))
    a.rot("throat", k((0, [0, 0, 0]), (1.05, [10, 0, 0]), (2.8, [10, 0, 0]), (3.3, [0, 0, 0])))
    a.scale("chest", k((0, [1, 1, 1]), (0.5, [1.05, 1.04, 1.05]), (1.1, [1, 1, 1]), (3.5, [1, 1, 1])))
    for side, sg in SIDES:
        a.rot(f"brow_{side}", k((0, [0, 0, 0]), (0.8, [0, 0, sg * 6]), (2.8, [0, 0, sg * 6]), (3.5, [0, 0, 0])))
        a.rot(f"arm_{side}", k((0, [0, 0, 0]), (1.0, [-34, 0, -sg * 22]), (2.8, [-36, 0, -sg * 24]), (3.5, [0, 0, 0])))
        a.rot(f"forearm_{side}", k((0, [0, 0, 0]), (1.0, [-22, 0, 0]), (2.8, [-24, 0, 0]), (3.5, [0, 0, 0])))
        for f in range(4):
            for j in (1, 2):
                a.rot(f"finger{f}_{j}_{side}", k((0, [0, 0, 0]), (1.0, [-18, 0, 0]), (2.8, [-18, 0, 0]), (3.5, [0, 0, 0])))
    for i, b in enumerate(TAILS):
        a.rot(b, k((0, [0, 0, 0]), (1.0, [3 + i * 0.3, 0, 0]), (2.8, [3, 0, 0]), (3.5, [0, 0, 0])))
    return a


def atomic_breath():
    L = 7.0
    a = A(L, False)
    k = lambda *pts: [(t, list(v)) for t, v in pts]
    # body language: brace -> charge -> rear back (inhale) -> lunge and fire -> recover
    a.pos("body", k((0, [0, 0, 0]), (0.6, [0, -1.5, 0]), (5.4, [0, -1.5, 0]), (6.4, [0, 0, 0])))
    for side, sg in SIDES:
        a.rot(f"thigh_{side}", k((0, [0, 0, 0]), (0.6, [-7, 0, -sg * 2]), (3.3, [-7, 0, -sg * 2]), (3.6, [-4, 0, -sg * 2]), (5.4, [-4, 0, 0]), (6.4, [0, 0, 0])))
        a.rot(f"shin_{side}", k((0, [0, 0, 0]), (0.6, [9, 0, 0]), (5.4, [9, 0, 0]), (6.4, [0, 0, 0])))
        a.rot(f"foot_{side}", k((0, [0, 0, 0]), (0.6, [-2, 0, 0]), (5.4, [-2, 0, 0]), (6.4, [0, 0, 0])))
        a.rot(f"arm_{side}", k((0, [0, 0, 0]), (0.6, [-14, 0, -sg * 10]), (2.9, [-20, 0, -sg * 18]), (3.3, [-8, 0, -sg * 8]), (5.4, [-10, 0, -sg * 8]), (6.4, [0, 0, 0])))
        a.rot(f"forearm_{side}", k((0, [0, 0, 0]), (0.6, [-12, 0, 0]), (5.4, [-14, 0, 0]), (6.4, [0, 0, 0])))
        for f in range(4):
            for j in (1, 2, 3):
                a.rot(f"finger{f}_{j}_{side}", k((0, [0, 0, 0]), (0.6, [-14, 0, 0]), (2.9, [-20, 0, 0]), (3.3, [6, 0, 0]), (5.4, [6, 0, 0]), (6.4, [0, 0, 0])))
        a.rot(f"brow_{side}", k((0, [0, 0, 0]), (0.6, [0, 0, sg * 5]), (5.4, [0, 0, sg * 7]), (6.4, [0, 0, 0])))
    a.rot("torso", k((0, [0, 0, 0]), (0.6, [5, 0, 0]), (2.4, [6, 0, 0]), (2.95, [-7, 0, 0]), (3.35, [9, 0, 0]), (5.3, [7, 0, 0]), (6.4, [0, 0, 0])))
    a.scale("chest", k((0, [1, 1, 1]), (2.4, [1, 1, 1]), (2.95, [1.06, 1.05, 1.07]), (3.35, [1, 1, 1])))
    a.scale("belly", k((0, [1, 1, 1]), (2.4, [1, 1, 1]), (2.95, [1.04, 1, 1.06]), (3.35, [1, 1, 1])))
    for i, b in enumerate(NECK):
        a.rot(b, k((0, [0, 0, 0]), (0.6, [5, 0, 0]), (2.4, [7, 0, 0]), (2.95, [-14, 0, 0]), (3.35, [9, 0, 0]), (5.3, [9, 0, 0]), (6.4, [0, 0, 0])))
    a.rot("head", k((0, [0, 0, 0]), (0.6, [5, 0, 0]), (2.4, [6, 0, 0]), (2.95, [-20, 0, 0]), (3.35, [3, 0, 0]), (5.3, [3, 0, 0]), (6.4, [0, 0, 0])))
    # the head shudders while it fires
    a.fn("head", "position", lambda t: [0.12 * math.sin(t * 110), 0.1 * math.sin(t * 87), 0] if 3.35 < t < 5.3 else [0, 0, 0])
    a.rot("jaw", k((0, [0, 0, 0]), (2.4, [3, 0, 0]), (2.95, [18, 0, 0]), (3.35, [40, 0, 0]), (5.3, [40, 0, 0]), (5.8, [8, 0, 0]), (6.4, [0, 0, 0])))
    a.pos("lip_upper", k((0, [0, 0, 0]), (2.95, [0, 0.4, 0]), (3.35, [0, 0.9, 0]), (5.3, [0, 0.9, 0]), (5.8, [0, 0, 0])))
    a.rot("tongue1", k((0, [0, 0, 0]), (3.35, [-8, 0, 0]), (5.3, [-8, 0, 0]), (5.8, [0, 0, 0])))
    a.rot("throat", k((0, [0, 0, 0]), (2.95, [-6, 0, 0]), (3.35, [10, 0, 0]), (5.3, [10, 0, 0]), (6.0, [0, 0, 0])))
    for i, b in enumerate(TAILS):
        f = i / 15
        a.rot(b, k((0, [0, 0, 0]), (0.6, [2 + 2 * f, 0, 0]), (2.4, [3 + 3 * f, 0, 0]), (2.95, [-2, 0, 0]), (3.35, [5 + 5 * f, 0, 0]),
                   (5.3, [4 + 4 * f, 0, 0]), (6.4, [0, 0, 0])), step=0.1)
    # the plates: out one by one from the tail tip (s=0) to the head (s=1), then back in from the head to the tail
    for p in PLATES:
        s, T = p["s"], p["travel"]
        t_out = 0.5 + 1.8 * s
        t_in = 5.3 + 0.75 * (1 - s)
        keys = {
            "0": [0, 0, 0],
            f"{t_out:.3f}": [0, 0, 0],
            f"{t_out + 0.09:.3f}": [0, round(T * 1.18, 3), 0],
            f"{t_out + 0.17:.3f}": [0, round(T * 0.94, 3), 0],
            f"{t_out + 0.24:.3f}": [0, round(T, 3), 0],
            f"{t_in:.3f}": [0, round(T, 3), 0],
            f"{t_in + 0.07:.3f}": [0, round(-T * 0.1, 3), 0],
            f"{t_in + 0.14:.3f}": [0, 0, 0],
        }
        a.set(p["bone"], "position", keys)
    return a


def pose_roar():
    return {"loop": True, "animation_length": 1, "bones": {
        "neck1": {"rotation": [-5, 0, 0]}, "neck2": {"rotation": [-4, 0, 0]}, "neck3": {"rotation": [-4, 0, 0]},
        "head": {"rotation": [-20, 0, 0]}, "jaw": {"rotation": [38, 0, 0]}, "lip_upper": {"position": [0, 0.8, 0]},
        "torso": {"rotation": [-4, 0, 0]}, "throat": {"rotation": [10, 0, 0]},
        "arm_l": {"rotation": [-30, 0, -14]}, "arm_r": {"rotation": [-30, 0, 14]},
        "forearm_l": {"rotation": [-20, 0, 0]}, "forearm_r": {"rotation": [-20, 0, 0]},
        "brow_l": {"rotation": [0, 0, 6]}, "brow_r": {"rotation": [0, 0, -6]},
    }}


def main():
    data = {"format_version": "1.8.0", "animations": {
        "animation.kaiju.idle": idle().json(),
        "animation.kaiju.walk": walk().json(),
        "animation.kaiju.roar": roar().json(),
        "animation.kaiju.atomic_breath": atomic_breath().json(),
        "pose.roar": pose_roar(),
    }}
    with open(os.path.join(OUT, "kaiju.animation.json"), "w") as f:
        json.dump(data, f, separators=(",", ":"))
    print("animations:", ", ".join(data["animations"]))


if __name__ == "__main__":
    main()
