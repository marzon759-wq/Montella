#!/usr/bin/env python3
"""The Hatman: the original model from the first Hatman mod (geometry and texture unchanged), with a few invisible
control bones added, and a completely new set of animations written for its skeleton.

Skeleton (heights in model pixels, 16 px = 1 block, about 3.5 blocks tall):
  root > all > base > hips(30) > posture(31.5) > waist > chest(38.5) > neck(46.8) > head_look > head(49.4) > skull > hat
  chest > {left,right}_upper(45.2) > _fore(32) > _hand(18.4) > _f{0..3}{a,b,c} + _thumb_{a,b}
  hips > {left,right}_thigh(30) > _shin(15.2) > _foot(1.7)
"""
import json, math, os, shutil, sys

sys.path.insert(0, os.path.dirname(__file__))
from gen_entity import Anim, smooth, pulse, tremor, sin, cos, PI, TAU

HERE = os.path.dirname(__file__)
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "..", "common/src/main/resources/assets/hatman")
SIDES = (("l", 1), ("r", -1))
FINGERS = ("f0", "f1", "f2", "f3")


def build_geo():
    g = json.load(open(os.path.join(HERE, "src/hatman_original.geo.json")))
    geo = g["minecraft:geometry"][0]
    bones = geo["bones"]
    by = {b["name"]: b for b in bones}
    out = []
    for b in bones:
        if b["name"] == "base":
            out.append({"name": "all", "parent": "root", "pivot": [0, 0, 0]})
            b["parent"] = "all"
        if b["name"] == "waist":
            out.append({"name": "posture", "parent": "hips", "pivot": [0, 31.5, 0]})
            b["parent"] = "posture"
        if b["name"] == "head":
            out.append({"name": "head_look", "parent": "neck", "pivot": list(by["head"]["pivot"])})
            b["parent"] = "head_look"
        out.append(b)
    geo["bones"] = out
    geo["description"]["visible_bounds_height"] = 7
    geo["description"]["visible_bounds_offset"] = [0, 3, 0]
    return g


class HAnim(Anim):
    """Anim with helpers that know about the hand: a single 'fingers' curl is spread over three knuckles of four
    fingers, with a small delay from the index to the little finger, so hands always move like hands."""

    def fingers(self, side, f, spread=None, stagger=0.06, step=0.05):
        n = int(round(self.length / step))
        for i, fn in enumerate(FINGERS):
            d = stagger * i
            for seg, k in (("a", 0.42), ("b", 0.34), ("c", 0.30)):
                bone = f"{side}_{fn}{seg}" if False else f"{'left' if side == 'l' else 'right'}_{fn}{seg}"
                for j in range(n + 1):
                    t = min(self.length, j * step)
                    tt = t - d if self.loop is not True else (t - d) % self.length
                    tt = max(0.0, tt)
                    z = 0.0
                    if spread is not None and seg == "a":
                        z = spread(t) * (i - 1.5) * (1 if side == "l" else -1)
                    self.key(bone, "rotation", t, [f(tt) * k, 0, z])

    def thumb(self, side, f):
        s = "left" if side == "l" else "right"
        self.fn(f"{s}_thumb_a", "rotation", lambda t: [f(t) * 0.5, 0, 0])
        self.fn(f"{s}_thumb_b", "rotation", lambda t: [f(t) * 0.5, 0, 0])


def B(kind, side):
    s = "left" if side == "l" else "right"
    return {"arm": f"{s}_upper", "fore": f"{s}_fore", "hand": f"{s}_hand", "thigh": f"{s}_thigh", "shin": f"{s}_shin", "foot": f"{s}_foot"}[kind]


def anims():
    A = {}

    # ------------------------------------------------------------ idle: a tall thing pretending to be a coat on a hook
    a = HAnim(8.0, loop=True)
    w = TAU / 8.0
    br = lambda t: sin(2 * w * t)  # breathing, 4 s
    a.fn("waist", "rotation", lambda t: [3 + 1.0 * br(t), 1.5 * sin(w * t), 0.8 * sin(w * t + 1)])
    a.fn("chest", "rotation", lambda t: [4 + 1.6 * br(t + 0.3), 0, 0])
    a.fn("chest", "scale", lambda t: [1 + 0.012 * br(t), 1, 1 + 0.035 * br(t)])
    a.fn("neck", "rotation", lambda t: [8 + 1.2 * br(t + 0.6), 0, 0])
    # the head drifts, settles, then cocks sharply to one side and holds
    a.fn("head", "rotation", lambda t: [-6 + 1.0 * br(t + 1), 3 * sin(w * t + 2),
                                        14 * pulse(t, 2.5, 2.62, 4.6, 5.4) + tremor(t, 31, 0.8) * pulse(t, 2.6, 2.65, 2.9, 3.1)])
    a.fn("hat", "rotation", lambda t: [0, 0, 1.2 * sin(w * t)])
    for s, sg in SIDES:
        ph = 0 if s == "l" else 1.7
        a.fn(B("arm", s), "rotation", lambda t, sg=sg, ph=ph: [3 + 1.5 * sin(2 * w * t + ph), 0, -sg * (2.5 + 0.8 * sin(w * t + ph))])
        a.fn(B("fore", s), "rotation", lambda t, ph=ph: [-5 - 2 * sin(2 * w * t + ph + 1), 0, 0])
        a.fn(B("hand", s), "rotation", lambda t, sg=sg, ph=ph: [0, 6 * sin(w * t + ph), sg * 3 * sin(w * t + ph)])
        tw = 4.2 if s == "l" else 6.3
        a.fingers(s, lambda t, tw=tw, ph=ph: -12 - 6 * sin(2 * w * t + ph) - 70 * pulse(t, tw, tw + 0.25, tw + 0.35, tw + 0.9),
                  spread=lambda t: 3 + 2 * sin(w * t), stagger=0.09)
        a.thumb(s, lambda t, ph=ph: -10 - 4 * sin(2 * w * t + ph))
        a.fn(B("thigh", s), "rotation", lambda t, sg=sg: [0, 0, -sg * 1.5])
    A["idle"] = a

    # ------------------------------------------------------------ freeze: caught mid-step
    a = HAnim(1.0, loop=True)
    a.rot("waist", [(0, [12, -8, 3])])
    a.rot("chest", [(0, [10, -6, 0])])
    a.rot("neck", [(0, [-6, 0, 0])])
    a.rot("head", [(0, [-14, 8, -22])])
    a.rot(B("arm", "l"), [(0, [-48, 0, -6])])
    a.rot(B("fore", "l"), [(0, [-36, 0, 0])])
    a.rot(B("hand", "l"), [(0, [-20, 0, 0])])
    a.rot(B("arm", "r"), [(0, [22, 0, 8])])
    a.rot(B("fore", "r"), [(0, [-12, 0, 0])])
    a.fingers("l", lambda t: -60, spread=lambda t: 8, stagger=0)
    a.fingers("r", lambda t: -15, stagger=0)
    a.rot(B("thigh", "l"), [(0, [-30, 0, 0])])
    a.rot(B("shin", "l"), [(0, [26, 0, 0])])
    a.rot(B("foot", "l"), [(0, [4, 0, 0])])
    a.rot(B("thigh", "r"), [(0, [18, 0, 0])])
    a.rot(B("shin", "r"), [(0, [14, 0, 0])])
    a.rot(B("foot", "r"), [(0, [-30, 0, 0])])
    A["freeze"] = a

    # ------------------------------------------------------------ locomotion
    def gait(length, stride, lean, swing, bob, knee, trail=0.0, shake=0.0, curl=-14, crouch=0.0, bow=0.0):
        a = HAnim(length, loop=True)
        w = TAU / length
        # two steps per cycle: the body is lowest when a foot lands
        a.fn("all", "position", lambda t: [0, -bob * (0.5 + 0.5 * cos(2 * w * t)) - crouch * 9, 0])
        a.fn("hips", "rotation", lambda t: [0, -6 * sin(w * t), 3 * sin(w * t)])
        a.fn("waist", "rotation", lambda t: [lean + bow + 2.5 * sin(2 * w * t + 0.6), 5 * sin(w * t), -2 * sin(w * t)])
        a.fn("chest", "rotation", lambda t: [lean * 0.4, -9 * sin(w * t + 0.3), 1.5 * sin(w * t)])
        # head stays level: a predator's head does not bob
        a.fn("neck", "rotation", lambda t: [-(lean + bow) * 0.45, 0, 0])
        a.fn("head", "rotation", lambda t: [-(lean + bow) * 0.6 - 2.5 * sin(2 * w * t + 0.6) + shake * sin(2 * w * t),
                                            4 * sin(w * t + 0.6), shake * 2.5 * sin(w * t + 1)])
        a.fn("hat", "rotation", lambda t: [1.5 * sin(2 * w * t + 1.4), 0, 0])
        for s, sg in SIDES:
            ph = 0 if s == "l" else PI
            leg = lambda t, ph=ph: sin(w * t + ph)
            a.fn(B("thigh", s), "rotation", lambda t, ph=ph: [-stride * sin(w * t + ph) - (lean + bow) * 0.2 - crouch * 50, 0, 0])
            # the knee folds while the leg swings forward, stays nearly straight under load
            a.fn(B("shin", s), "rotation", lambda t, ph=ph: [knee * max(0.0, cos(w * t + ph + 0.4)) ** 1.6 + 3 + crouch * 95, 0, 0])
            # ankle: toe lifts on the swing, foot lands flat
            a.fn(B("foot", s), "rotation", lambda t, ph=ph: [-knee * 0.35 * max(0.0, cos(w * t + ph + 0.4)) ** 2 + stride * 0.25 * sin(w * t + ph) - crouch * 45, 0, 0])
            a.fn(B("arm", s), "rotation", lambda t, ph=ph, sg=sg: [swing * sin(w * t + ph + PI) + trail - lean * 0.5 - bow * 0.8, 0, -sg * (2 + abs(trail) * 0.1)])
            a.fn(B("fore", s), "rotation", lambda t, ph=ph: [-6 - swing * 0.4 * (1 + sin(w * t + ph + PI - 0.6)) - abs(trail) * 0.2, 0, 0])
            # the hands lag behind the arms and flop
            a.fn(B("hand", s), "rotation", lambda t, ph=ph, sg=sg: [8 * sin(w * t + ph + PI - 1.3), 0, sg * 4 * sin(2 * w * t)])
            a.fingers(s, lambda t, ph=ph: curl - 14 * sin(w * t + ph - 1.8), spread=lambda t: 4, stagger=0.04)
            a.thumb(s, lambda t, ph=ph: -8 - 6 * sin(w * t + ph - 1.6))
        return a

    A["walk"] = gait(1.5, 22, 5, 9, 0.9, 34)
    A["stalk"] = gait(1.1, 26, 14, 3, 1.2, 40, trail=-16, curl=-50)
    A["run"] = gait(0.62, 44, 30, 6, 2.2, 70, trail=46, shake=1.8, curl=20)
    A["duck_walk"] = gait(1.25, 18, 6, 4, 0.6, 30, crouch=1.0, bow=62, curl=-30)

    a = HAnim(3.0, loop=True)
    w = TAU / 3.0
    a.fn("all", "position", lambda t: [0, -9, 0])
    a.fn("waist", "rotation", lambda t: [66 + 2 * sin(w * t), 0, 0])
    a.fn("chest", "rotation", lambda t: [6 + 1.5 * sin(w * t + 0.4), 0, 0])
    a.fn("neck", "rotation", lambda t: [-34, 0, 0])
    a.fn("head", "rotation", lambda t: [-30, 6 * sin(w * t * 0.5), 8 * sin(w * t * 0.5 + 1)])
    for s, sg in SIDES:
        a.fn(B("thigh", s), "rotation", lambda t: [-50, 0, 0])
        a.fn(B("shin", s), "rotation", lambda t: [95, 0, 0])
        a.fn(B("foot", s), "rotation", lambda t: [-45, 0, 0])
        a.fn(B("arm", s), "rotation", lambda t, sg=sg: [-40, 0, -sg * 6])
        a.fn(B("fore", s), "rotation", lambda t: [-20, 0, 0])
        a.fingers(s, lambda t: -40 - 10 * sin(w * t), stagger=0.1)
    A["duck_idle"] = a

    # ------------------------------------------------------------ crawl: on all fours, limbs like a spider's
    a = HAnim(0.9, loop=True)
    w = TAU / 0.9
    a.fn("all", "position", lambda t: [0, -20 + 0.8 * sin(2 * w * t), 4])
    a.fn("waist", "rotation", lambda t: [80 + 2 * sin(2 * w * t), 7 * sin(w * t), 0])
    a.fn("chest", "rotation", lambda t: [8, -10 * sin(w * t), 0])
    a.fn("neck", "rotation", lambda t: [-45, 0, 0])
    a.fn("head", "rotation", lambda t: [-42 + 3 * sin(2 * w * t), 8 * sin(w * t), 10 * sin(w * t + 1)])
    for s, sg in SIDES:
        ph = 0 if s == "l" else PI
        a.fn(B("arm", s), "rotation", lambda t, ph=ph, sg=sg: [-152 + 34 * sin(w * t + ph), 0, -sg * 18])
        a.fn(B("fore", s), "rotation", lambda t, ph=ph: [-30 - 34 * max(0, cos(w * t + ph)), 0, 0])
        a.fn(B("hand", s), "rotation", lambda t: [70, 0, 0])
        a.fingers(s, lambda t, ph=ph: 40 + 25 * sin(w * t + ph), spread=lambda t: 12, stagger=0.03)
        a.fn(B("thigh", s), "rotation", lambda t, ph=ph, sg=sg: [-70 + 28 * sin(w * t + ph + PI), 0, -sg * 18])
        a.fn(B("shin", s), "rotation", lambda t, ph=ph: [110 + 22 * max(0, cos(w * t + ph + PI)), 0, 0])
        a.fn(B("foot", s), "rotation", lambda t: [-40, 0, 0])
    A["crawl"] = a

    # ------------------------------------------------------------ sit: knees higher than the armrests
    a = HAnim(6.0, loop=True)
    w = TAU / 6.0
    a.fn("all", "position", lambda t: [0, -17, 3])
    a.fn("waist", "rotation", lambda t: [-8 + 0.8 * sin(w * t), 0, 0])
    a.fn("chest", "rotation", lambda t: [10 + 1.4 * sin(w * t + 0.5), 0, 0])
    a.fn("chest", "scale", lambda t: [1, 1, 1 + 0.03 * sin(w * t)])
    a.fn("neck", "rotation", lambda t: [8, 0, 0])
    a.fn("head", "rotation", lambda t: [2, 4 * sin(w * t * 0.5), 9 * sin(w * t * 0.5 + 1)])
    for s, sg in SIDES:
        a.fn(B("thigh", s), "rotation", lambda t, sg=sg: [-95, sg * 10, 0])
        a.fn(B("shin", s), "rotation", lambda t: [100, 0, 0])
        a.fn(B("foot", s), "rotation", lambda t: [-5, 0, 0])
        a.fn(B("arm", s), "rotation", lambda t: [-28 + 0.8 * sin(w * t), 0, 0])
        a.fn(B("fore", s), "rotation", lambda t: [-58, 0, 0])
        a.fn(B("hand", s), "rotation", lambda t: [20, 0, 0])
        tw = 1.5 if s == "l" else 3.8
        # drumming on the armrest, index to little finger
        a.fingers(s, lambda t, tw=tw: 20 - 55 * pulse(t, tw, tw + 0.08, tw + 0.12, tw + 0.3) - 55 * pulse(t, tw + 0.5, tw + 0.58, tw + 0.62, tw + 0.8),
                  stagger=0.12)
    A["sit"] = a

    # ================================================================ one-shot actions
    a = HAnim(2.6)  # emerge: unfolds out of the floor, joint by joint
    a.fn("all", "position", lambda t: [tremor(t, 29, 0.3) * (1 - smooth(t, 1.5, 2.1)), -64 * (1 - smooth(t, 0.0, 1.6)) ** 1.5, 0])
    a.fn("waist", "rotation", lambda t: [80 * (1 - smooth(t, 0.9, 2.3)), 16 * sin(t * 5) * (1 - smooth(t, 0.8, 2.0)), 0])
    a.fn("chest", "rotation", lambda t: [30 * (1 - smooth(t, 1.2, 2.4)), 0, 0])
    a.fn("neck", "rotation", lambda t: [50 * (1 - smooth(t, 1.5, 2.4)), 0, 0])
    a.fn("head", "rotation", lambda t: [40 * (1 - smooth(t, 1.7, 2.5)), 0, 40 * (1 - smooth(t, 1.8, 2.2)) * sin(t * 9)])
    for s, sg in SIDES:
        a.fn(B("arm", s), "rotation", lambda t, sg=sg: [-160 * (1 - smooth(t, 0.7, 2.3)), 0, -sg * 45 * pulse(t, 0.2, 0.9, 1.3, 2.3)])
        a.fn(B("fore", s), "rotation", lambda t: [-80 * pulse(t, 0.3, 0.9, 1.4, 2.4), 0, 0])
        a.fn(B("hand", s), "rotation", lambda t: [60 * pulse(t, 0.3, 0.8, 1.4, 2.3), 0, 0])
        a.fingers(s, lambda t: 70 * (1 - smooth(t, 1.2, 2.4)) - 30 * pulse(t, 2.0, 2.2, 2.3, 2.6), spread=lambda t: 15 * (1 - smooth(t, 1.5, 2.4)))
        a.fn(B("thigh", s), "rotation", lambda t: [-60 * (1 - smooth(t, 0.6, 1.8)), 0, 0])
        a.fn(B("shin", s), "rotation", lambda t: [100 * (1 - smooth(t, 0.6, 1.8)), 0, 0])
    A["emerge"] = a

    a = HAnim(1.1, hold=True)  # vanish: folds in on himself and is pulled down
    a.fn("all", "position", lambda t: [tremor(t, 41, 0.6 * smooth(t, 0, 0.4)), -64 * smooth(t, 0.25, 1.05) ** 2, 0])
    a.fn("all", "scale", lambda t: [1 - 0.7 * smooth(t, 0.3, 1.0), 1 + 0.2 * smooth(t, 0.2, 0.9), 1 - 0.7 * smooth(t, 0.3, 1.0)])
    a.fn("waist", "rotation", lambda t: [30 * smooth(t, 0, 0.5), 70 * smooth(t, 0.2, 1.0), 0])
    a.fn("head", "rotation", lambda t: [40 * smooth(t, 0, 0.4), 0, 70 * smooth(t, 0.1, 0.5)])
    for s, sg in SIDES:
        a.fn(B("arm", s), "rotation", lambda t, sg=sg: [-30 * smooth(t, 0, 0.4), 0, sg * 40 * smooth(t, 0, 0.5)])
        a.fn(B("fore", s), "rotation", lambda t: [-130 * smooth(t, 0, 0.5), 0, 0])
        a.fingers(s, lambda t: 90 * smooth(t, 0, 0.4), stagger=0.02)
    A["vanish"] = a

    a = HAnim(2.4)  # hat tip: the polite greeting, slow and exact
    up = lambda t: pulse(t, 0.0, 0.65, 1.55, 2.25)
    a.fn("waist", "rotation", lambda t: [12 * pulse(t, 0.55, 1.05, 1.5, 2.2), 0, 0])
    a.fn("chest", "rotation", lambda t: [6 * pulse(t, 0.55, 1.05, 1.5, 2.2), 0, 0])
    a.fn("neck", "rotation", lambda t: [10 * pulse(t, 0.6, 1.05, 1.5, 2.2), 0, 0])
    a.fn("head", "rotation", lambda t: [14 * pulse(t, 0.6, 1.05, 1.5, 2.2), 0, -8 * pulse(t, 0.6, 1.05, 1.5, 2.2)])
    a.fn(B("arm", "r"), "rotation", lambda t: [-150 * up(t), 0, -20 * up(t)])
    a.fn(B("fore", "r"), "rotation", lambda t: [-118 * pulse(t, 0.1, 0.65, 1.55, 2.15), 0, 0])
    a.fn(B("hand", "r"), "rotation", lambda t: [-28 * pulse(t, 0.3, 0.65, 1.55, 2.0), 0, 0])
    a.fingers("r", lambda t: -10 + 75 * pulse(t, 0.45, 0.68, 1.5, 1.85), stagger=0.03)
    a.thumb("r", lambda t: 60 * pulse(t, 0.45, 0.68, 1.5, 1.85))
    a.fn("hat", "position", lambda t: [0, 2.6 * pulse(t, 0.75, 1.05, 1.4, 1.7), -1.4 * pulse(t, 0.75, 1.05, 1.4, 1.7)])
    a.fn("hat", "rotation", lambda t: [24 * pulse(t, 0.75, 1.05, 1.4, 1.7), 0, 0])
    a.fn(B("arm", "l"), "rotation", lambda t: [6 * pulse(t, 0.4, 1.0, 1.5, 2.2), 0, 0])
    a.fingers("l", lambda t: -12 - 40 * pulse(t, 1.0, 1.1, 1.2, 1.6), stagger=0.08)
    A["hat_tip"] = a

    a = HAnim(2.2)  # knock: three slow raps with the knuckles
    kn = lambda t: sum(pulse(t, k, k + 0.07, k + 0.1, k + 0.26) for k in (0.6, 1.02, 1.44))
    env = lambda t: pulse(t, 0, 0.45, 1.75, 2.2)
    a.fn("waist", "rotation", lambda t: [8 * env(t), -10 * env(t), 0])
    a.fn("head", "rotation", lambda t: [6 * env(t), 0, 18 * env(t)])
    a.fn(B("arm", "r"), "rotation", lambda t: [-70 * env(t) - 8 * kn(t), 0, 0])
    a.fn(B("fore", "r"), "rotation", lambda t: [-96 * env(t) + 34 * kn(t), 0, 0])
    a.fn(B("hand", "r"), "rotation", lambda t: [-30 * kn(t), 0, 0])
    a.fingers("r", lambda t: 95 * env(t), stagger=0.0)
    A["knock"] = a

    a = HAnim(2.6)  # scream: head back, chest heaving, every finger splayed
    env = lambda t: pulse(t, 0.25, 0.5, 2.0, 2.5)
    a.fn("all", "position", lambda t: [tremor(t, 53, 0.4) * env(t), -2 * pulse(t, 0, 0.25, 0.3, 0.5), 0])
    a.fn("waist", "rotation", lambda t: [18 * pulse(t, 0, 0.22, 0.28, 0.5) - 14 * env(t) + tremor(t, 43, 1.3) * env(t), 0, tremor(t, 39, 1.0) * env(t)])
    a.fn("chest", "rotation", lambda t: [-14 * env(t), 0, 0])
    a.fn("chest", "scale", lambda t: [1 + 0.1 * env(t), 1, 1 + 0.22 * env(t)])
    a.fn("neck", "rotation", lambda t: [-20 * env(t), 0, 0])
    a.fn("head", "rotation", lambda t: [-34 * env(t) + tremor(t, 61, 3.5) * env(t), tremor(t, 57, 4) * env(t), tremor(t, 49, 5) * env(t)])
    a.fn("hat", "rotation", lambda t: [-10 * env(t), 0, tremor(t, 45, 2) * env(t)])
    for s, sg in SIDES:
        a.fn(B("arm", s), "rotation", lambda t, sg=sg: [24 * env(t) + tremor(t, 44, 2) * env(t), 0, -sg * 62 * env(t)])
        a.fn(B("fore", s), "rotation", lambda t: [-34 * env(t), 0, 0])
        a.fn(B("hand", s), "rotation", lambda t, sg=sg: [0, 0, -sg * 30 * env(t)])
        a.fingers(s, lambda t: -40 * env(t) + tremor(t, 71, 10) * env(t), spread=lambda t: 22 * env(t), stagger=0.01)
        a.thumb(s, lambda t: -40 * env(t))
        a.fn(B("thigh", s), "rotation", lambda t, sg=sg: [0, 0, -sg * 7 * env(t)])
    A["scream"] = a

    a = HAnim(0.9)  # grab: wind up, lunge, close
    a.fn("all", "position", lambda t: [0, -3 * pulse(t, 0, 0.15, 0.5, 0.9), -7 * pulse(t, 0.1, 0.3, 0.55, 0.9)])
    a.fn("waist", "rotation", lambda t: [-12 * pulse(t, 0, 0.12, 0.14, 0.24) + 42 * pulse(t, 0.14, 0.3, 0.55, 0.9), 0, 0])
    a.fn("head", "rotation", lambda t: [-34 * pulse(t, 0.14, 0.3, 0.55, 0.9), 0, 22 * pulse(t, 0.2, 0.3, 0.55, 0.9)])
    for s, sg in SIDES:
        a.fn(B("arm", s), "rotation", lambda t, sg=sg: [40 * pulse(t, 0, 0.12, 0.14, 0.22) - 120 * pulse(t, 0.14, 0.28, 0.55, 0.9), 0, sg * 20 * pulse(t, 0.2, 0.32, 0.5, 0.9) - sg * 34 * pulse(t, 0, 0.12, 0.14, 0.24)])
        a.fn(B("fore", s), "rotation", lambda t: [-24 * pulse(t, 0.14, 0.3, 0.55, 0.9), 0, 0])
        a.fingers(s, lambda t: -50 * pulse(t, 0, 0.2, 0.26, 0.3) + 100 * pulse(t, 0.28, 0.36, 0.6, 0.9), spread=lambda t: 18 * pulse(t, 0, 0.2, 0.26, 0.32), stagger=0.0)
    a.fn(B("thigh", "l"), "rotation", lambda t: [-36 * pulse(t, 0.1, 0.3, 0.55, 0.9), 0, 0])
    a.fn(B("shin", "l"), "rotation", lambda t: [20 * pulse(t, 0.1, 0.3, 0.55, 0.9), 0, 0])
    a.fn(B("thigh", "r"), "rotation", lambda t: [26 * pulse(t, 0.1, 0.3, 0.55, 0.9), 0, 0])
    A["grab"] = a

    a = HAnim(4.0)  # search: bends low, head sweeping, sniffing, fingers testing the air
    env = lambda t: pulse(t, 0, 0.7, 3.3, 4.0)
    a.fn("all", "position", lambda t: [0, -5 * env(t), 0])
    a.fn("waist", "rotation", lambda t: [48 * env(t), 26 * sin(t * 1.9) * env(t), 0])
    a.fn("chest", "rotation", lambda t: [12 * env(t), 14 * sin(t * 1.9 + 0.4) * env(t), 0])
    a.fn("neck", "rotation", lambda t: [-26 * env(t), 0, 0])
    a.fn("head", "rotation", lambda t: [-28 * env(t) + 4 * sin(t * 11) * env(t) * pulse(t, 1.2, 1.3, 1.9, 2.1), 42 * sin(t * 1.9 + 0.9) * env(t), 20 * sin(t * 3.1) * env(t)])
    for s, sg in SIDES:
        a.fn(B("arm", s), "rotation", lambda t, sg=sg: [-40 * env(t) + 8 * sin(t * 2.3 + sg), 0, -sg * 16 * env(t)])
        a.fn(B("fore", s), "rotation", lambda t, sg=sg: [-30 * env(t) - 14 * sin(t * 2.9 + sg) * env(t), 0, 0])
        a.fingers(s, lambda t, sg=sg: (-20 + 45 * sin(t * 6 + sg)) * env(t), spread=lambda t: 10 * env(t), stagger=0.1)
        a.fn(B("thigh", s), "rotation", lambda t, sg=sg: [-18 * env(t), 0, -sg * 6 * env(t)])
        a.fn(B("shin", s), "rotation", lambda t: [30 * env(t), 0, 0])
        a.fn(B("foot", s), "rotation", lambda t: [-12 * env(t), 0, 0])
    A["search"] = a

    a = HAnim(2.2)  # point: a long finger, held, trembling
    env = lambda t: pulse(t, 0, 0.4, 1.7, 2.2)
    a.fn("waist", "rotation", lambda t: [6 * env(t), -6 * env(t), 0])
    a.fn("head", "rotation", lambda t: [4 * env(t), 0, -14 * env(t)])
    a.fn(B("arm", "r"), "rotation", lambda t: [-84 * env(t) + tremor(t, 23, 0.8) * env(t), 0, 0])
    a.fn(B("fore", "r"), "rotation", lambda t: [-6 * env(t), 0, 0])
    a.fn(B("hand", "r"), "rotation", lambda t: [-6 * env(t), 0, 0])
    for i, f in enumerate(FINGERS):
        curl = 0 if i == 0 else 95
        for seg, k in (("a", 0.42), ("b", 0.34), ("c", 0.3)):
            a.fn(f"right_{f}{seg}", "rotation", lambda t, curl=curl, k=k: [curl * k * env(t), 0, 0])
    a.thumb("r", lambda t: 60 * env(t))
    A["point"] = a

    a = HAnim(0.7)  # twitch: the neck breaks and resets
    a.fn("head", "rotation", lambda t: [0, 0, 70 * pulse(t, 0.02, 0.07, 0.3, 0.42) + tremor(t, 80, 6) * pulse(t, 0.05, 0.1, 0.3, 0.45)])
    a.fn("neck", "rotation", lambda t: [0, 0, 18 * pulse(t, 0.02, 0.07, 0.3, 0.42)])
    a.fn("chest", "rotation", lambda t: [0, 16 * pulse(t, 0.0, 0.06, 0.2, 0.5), tremor(t, 60, 3) * pulse(t, 0, 0.1, 0.3, 0.6)])
    a.fn(B("arm", "l"), "rotation", lambda t: [0, 0, -34 * pulse(t, 0.04, 0.1, 0.2, 0.5)])
    a.fingers("l", lambda t: -70 * pulse(t, 0.04, 0.1, 0.25, 0.6), spread=lambda t: 14 * pulse(t, 0.04, 0.1, 0.25, 0.6), stagger=0.0)
    a.fingers("r", lambda t: 85 * pulse(t, 0.1, 0.16, 0.3, 0.65), stagger=0.02)
    A["twitch"] = a

    a = HAnim(2.4)  # head tilt: curious
    a.fn("head", "rotation", lambda t: [-4 * pulse(t, 0, 0.6, 1.8, 2.4), 0, 40 * pulse(t, 0.1, 0.7, 1.7, 2.3)])
    a.fn("neck", "rotation", lambda t: [0, 0, 12 * pulse(t, 0.1, 0.7, 1.7, 2.3)])
    a.fn("waist", "rotation", lambda t: [8 * pulse(t, 0.2, 0.9, 1.6, 2.4), 0, 0])
    a.fingers("l", lambda t: -12 - 60 * pulse(t, 0.6, 0.8, 0.9, 1.3), stagger=0.1)
    A["head_tilt"] = a

    a = HAnim(3.0)  # talk, calm: one hand turns as he explains
    env = lambda t: pulse(t, 0, 0.5, 2.4, 3.0)
    a.fn("waist", "rotation", lambda t: [5 * env(t), 4 * sin(t * 2.1) * env(t), 0])
    a.fn("head", "rotation", lambda t: [5 * sin(t * 4.2) * env(t), 6 * sin(t * 1.7) * env(t), 9 * sin(t * 1.3) * env(t)])
    a.fn(B("arm", "l"), "rotation", lambda t: [-38 * env(t), 0, -8 * env(t)])
    a.fn(B("fore", "l"), "rotation", lambda t: [-72 * env(t) - 8 * sin(t * 4.2) * env(t), 0, 0])
    a.fn(B("hand", "l"), "rotation", lambda t: [0, 50 * env(t), 18 * sin(t * 4.2) * env(t)])
    a.fingers("l", lambda t: (-14 + 22 * sin(t * 5)) * env(t), spread=lambda t: 10 * env(t), stagger=0.08)
    A["talk_calm"] = a

    a = HAnim(2.4)  # talk, angry: leans in over you, fingers working
    env = lambda t: pulse(t, 0, 0.3, 1.9, 2.4)
    a.fn("all", "position", lambda t: [tremor(t, 47, 0.2) * env(t), 0, -3 * env(t)])
    a.fn("waist", "rotation", lambda t: [28 * env(t), 0, tremor(t, 31, 1.0) * env(t)])
    a.fn("chest", "rotation", lambda t: [10 * env(t), 0, 0])
    a.fn("neck", "rotation", lambda t: [-14 * env(t), 0, 0])
    a.fn("head", "rotation", lambda t: [-20 * env(t) + tremor(t, 53, 2.4) * env(t), 0, 24 * sin(t * 3.5) * env(t)])
    for s, sg in SIDES:
        a.fn(B("arm", s), "rotation", lambda t, sg=sg: [-24 * env(t), 0, -sg * 24 * env(t)])
        a.fn(B("fore", s), "rotation", lambda t: [-46 * env(t), 0, 0])
        a.fingers(s, lambda t, sg=sg: (10 + 60 * sin(t * 9 + sg)) * env(t), spread=lambda t: 12 * env(t), stagger=0.05)
    A["talk_angry"] = a

    a = HAnim(1.8, hold=True)  # lean in: the brim comes down to your eye level and stays there
    env = lambda t: smooth(t, 0, 1.5)
    a.fn("all", "position", lambda t: [0, -3 * env(t), -4 * env(t)])
    a.fn("waist", "rotation", lambda t: [58 * env(t), 0, 0])
    a.fn("chest", "rotation", lambda t: [14 * env(t), 0, 0])
    a.fn("neck", "rotation", lambda t: [-32 * env(t), 0, 0])
    a.fn("head", "rotation", lambda t: [-34 * env(t), 0, 22 * smooth(t, 1.0, 1.7)])
    for s, sg in SIDES:
        a.fn(B("arm", s), "rotation", lambda t, sg=sg: [24 * env(t), 0, -sg * 10 * env(t)])
        a.fn(B("fore", s), "rotation", lambda t: [-24 * env(t), 0, 0])
        a.fingers(s, lambda t: -30 * env(t) - 20 * pulse(t, 1.3, 1.4, 1.5, 1.8), stagger=0.1)
        a.fn(B("thigh", s), "rotation", lambda t: [-10 * env(t), 0, 0])
        a.fn(B("shin", s), "rotation", lambda t: [16 * env(t), 0, 0])
    A["lean_in"] = a

    a = HAnim(3.0, hold=True)  # offer: takes off the hat and holds it out
    up = lambda t: pulse(t, 0.0, 0.7, 1.3, 2.2)
    out = lambda t: smooth(t, 1.5, 2.6)
    a.fn("waist", "rotation", lambda t: [6 * up(t) + 14 * out(t), 0, 0])
    a.fn("head", "rotation", lambda t: [10 * up(t) - 8 * out(t), 0, 12 * out(t)])
    a.fn(B("arm", "r"), "rotation", lambda t: [-150 * up(t) - 70 * out(t), 0, -20 * up(t)])
    a.fn(B("fore", "r"), "rotation", lambda t: [-118 * up(t) - 22 * out(t), 0, 0])
    a.fn(B("hand", "r"), "rotation", lambda t: [-30 * up(t) - 50 * out(t), 0, 0])
    a.fingers("r", lambda t: 75 * smooth(t, 0.5, 0.75), stagger=0.03)
    a.fn("hat", "position", lambda t: [-6 * smooth(t, 1.0, 2.4), 3 * pulse(t, 0.75, 1.1, 1.3, 1.8) - 26 * smooth(t, 1.2, 2.5), -3 * pulse(t, 0.75, 1.1, 1.2, 1.5) - 17 * smooth(t, 1.3, 2.6)])
    a.fn("hat", "rotation", lambda t: [30 * pulse(t, 0.75, 1.1, 1.3, 1.7) - 16 * out(t), 0, 0])
    a.fn(B("arm", "l"), "rotation", lambda t: [6 * out(t), 0, -4 * out(t)])
    A["offer"] = a

    a = HAnim(3.2, hold=True)  # bow: the deep farewell, hat to his chest
    up = lambda t: pulse(t, 0.0, 0.6, 1.0, 1.6)
    dn = lambda t: smooth(t, 1.2, 2.6)
    a.fn("waist", "rotation", lambda t: [6 * up(t) + 64 * dn(t), 0, 0])
    a.fn("chest", "rotation", lambda t: [14 * dn(t), 0, 0])
    a.fn("neck", "rotation", lambda t: [10 * dn(t), 0, 0])
    a.fn("head", "rotation", lambda t: [16 * dn(t), 0, 0])
    a.fn(B("arm", "r"), "rotation", lambda t: [-150 * up(t) - 40 * dn(t), 0, -20 * up(t) + 30 * dn(t)])
    a.fn(B("fore", "r"), "rotation", lambda t: [-118 * up(t) - 110 * dn(t), 0, 0])
    a.fingers("r", lambda t: 75 * smooth(t, 0.4, 0.65), stagger=0.03)
    a.fn("hat", "position", lambda t: [-2.5 * dn(t), 3 * pulse(t, 0.65, 0.95, 1.1, 1.5) - 14 * dn(t), -6 * dn(t)])
    a.fn("hat", "rotation", lambda t: [-80 * dn(t), 0, 0])
    a.fn(B("arm", "l"), "rotation", lambda t: [20 * dn(t), 0, -30 * dn(t)])
    a.fn(B("fore", "l"), "rotation", lambda t: [-30 * dn(t), 0, 0])
    a.fn(B("thigh", "r"), "rotation", lambda t: [18 * dn(t), 0, 0])
    a.fn(B("shin", "r"), "rotation", lambda t: [14 * dn(t), 0, 0])
    A["bow"] = a

    a = HAnim(1.6)  # drop: lands from the ceiling in a crouch and rises
    a.fn("all", "position", lambda t: [0, 56 * (1 - smooth(t, 0, 0.28)) ** 2 - 14 * pulse(t, 0.22, 0.34, 0.6, 1.4), 0])
    a.fn("waist", "rotation", lambda t: [60 * pulse(t, 0.2, 0.34, 0.7, 1.5), 0, 0])
    a.fn("neck", "rotation", lambda t: [-34 * pulse(t, 0.3, 0.5, 0.9, 1.5), 0, 0])
    a.fn("head", "rotation", lambda t: [-34 * pulse(t, 0.3, 0.5, 0.9, 1.5), 0, 34 * pulse(t, 0.5, 0.6, 0.9, 1.2)])
    for s, sg in SIDES:
        a.fn(B("thigh", s), "rotation", lambda t, sg=sg: [-70 * pulse(t, 0.2, 0.34, 0.7, 1.5), 0, -sg * 16 * pulse(t, 0.2, 0.34, 0.7, 1.5)])
        a.fn(B("shin", s), "rotation", lambda t: [120 * pulse(t, 0.2, 0.34, 0.7, 1.5), 0, 0])
        a.fn(B("foot", s), "rotation", lambda t: [-50 * pulse(t, 0.2, 0.34, 0.7, 1.5), 0, 0])
        a.fn(B("arm", s), "rotation", lambda t, sg=sg: [-160 * (1 - smooth(t, 0.1, 0.34)) - 40 * pulse(t, 0.25, 0.4, 0.7, 1.5), 0, -sg * 24 * pulse(t, 0.25, 0.4, 0.7, 1.5)])
        a.fingers(s, lambda t: 60 * pulse(t, 0.25, 0.4, 0.7, 1.4), spread=lambda t: 16 * pulse(t, 0.25, 0.4, 0.7, 1.4), stagger=0.0)
    A["drop"] = a

    a = HAnim(0.9)  # burst: shoulders through a door
    a.fn("all", "position", lambda t: [0, 0, 6 * pulse(t, 0, 0.2, 0.22, 0.3) - 8 * pulse(t, 0.22, 0.32, 0.5, 0.9)])
    a.fn("waist", "rotation", lambda t: [-14 * pulse(t, 0, 0.2, 0.22, 0.3) + 34 * pulse(t, 0.22, 0.32, 0.5, 0.9), 32 * pulse(t, 0.2, 0.32, 0.5, 0.9), 0])
    a.fn("head", "rotation", lambda t: [-22 * pulse(t, 0.22, 0.32, 0.5, 0.9), -26 * pulse(t, 0.22, 0.32, 0.5, 0.9), 0])
    a.fn(B("arm", "l"), "rotation", lambda t: [-60 * pulse(t, 0.2, 0.32, 0.5, 0.9), 0, 30 * pulse(t, 0.2, 0.32, 0.5, 0.9)])
    a.fn(B("fore", "l"), "rotation", lambda t: [-90 * pulse(t, 0.2, 0.32, 0.5, 0.9), 0, 0])
    a.fn(B("arm", "r"), "rotation", lambda t: [40 * pulse(t, 0.2, 0.32, 0.5, 0.9), 0, 0])
    a.fn(B("thigh", "l"), "rotation", lambda t: [-30 * pulse(t, 0.2, 0.32, 0.5, 0.9), 0, 0])
    a.fn(B("thigh", "r"), "rotation", lambda t: [24 * pulse(t, 0.2, 0.32, 0.5, 0.9), 0, 0])
    A["burst"] = a

    a = HAnim(1.6)  # stand up from the armchair
    k = lambda t: 1 - smooth(t, 0.1, 1.4)
    a.fn("all", "position", lambda t: [0, -17 * k(t), 3 * k(t)])
    a.fn("waist", "rotation", lambda t: [-8 * k(t) + 44 * pulse(t, 0.1, 0.5, 0.8, 1.5), 0, 0])
    a.fn("head", "rotation", lambda t: [-34 * pulse(t, 0.1, 0.5, 0.8, 1.5), 0, 0])
    for s, sg in SIDES:
        a.fn(B("thigh", s), "rotation", lambda t, sg=sg: [-95 * k(t), sg * 10 * k(t), 0])
        a.fn(B("shin", s), "rotation", lambda t: [100 * k(t), 0, 0])
        a.fn(B("arm", s), "rotation", lambda t: [-28 * k(t), 0, 0])
        a.fn(B("fore", s), "rotation", lambda t: [-58 * k(t), 0, 0])
    A["stand_up"] = a
    return A


def main():
    os.makedirs(f"{OUT}/geo/entity", exist_ok=True)
    os.makedirs(f"{OUT}/textures/entity", exist_ok=True)
    os.makedirs(f"{OUT}/animations/entity", exist_ok=True)
    with open(f"{OUT}/geo/entity/hatman.geo.json", "w") as f:
        json.dump(build_geo(), f, separators=(",", ":"))
    shutil.copy(os.path.join(HERE, "src/hatman_original.png"), f"{OUT}/textures/entity/hatman.png")
    gm = f"{OUT}/textures/entity/hatman_glowmask.png"
    if os.path.exists(gm):
        os.remove(gm)
    with open(f"{OUT}/animations/entity/hatman.animation.json", "w") as f:
        json.dump({"format_version": "1.8.0", "animations": {"animation.hatman." + k: v.json() for k, v in anims().items()}}, f, separators=(",", ":"))
    print("hatman ok")


if __name__ == "__main__":
    main()
