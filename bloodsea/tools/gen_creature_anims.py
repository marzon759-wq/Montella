#!/usr/bin/env python3
"""Animations for the creatures of the Blood Sea. Each animation is a function of time returning a pose, sampled every
0.05 s into a GeckoLib animation file per creature.

Signs (GeckoLib): rotation x > 0 tips a bone's top forward (toward -z) and its front down; a jaw opens with x > 0.
z > 0 on the +x ("l") side swings a hanging limb outward. Positions are model pixels."""
import json, math, os

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, "../common/src/main/resources/assets/bloodsea/animations")
DT = 0.05
SIDES = (("l", 1), ("r", -1))


def sm(x):
    x = max(0.0, min(1.0, x))
    return x * x * (3 - 2 * x)


def ramp(t, a, b):
    return sm((t - a) / (b - a)) if b > a else float(t >= a)


def noise(t, seed, speed=1.0):
    return (math.sin(t * 1.7 * speed + seed * 3.1) * 0.5 + math.sin(t * 2.9 * speed + seed * 1.3) * 0.3
            + math.sin(t * 5.3 * speed + seed * 7.7) * 0.2)


def twitch(t, seed, rate=0.9, sharp=18):
    ph = (t * rate + seed * 0.37) % 1.0
    return math.exp(-((ph - 0.5) * sharp) ** 2) * (1 if int(t * rate + seed) % 2 else -1)


class Pose(dict):
    def __init__(self):
        super().__init__()
        self.scales = {}

    def r(self, bone, x=0, y=0, z=0):
        o = self.setdefault(bone, [None, None])
        a = o[0] or [0, 0, 0]
        o[0] = [a[0] + x, a[1] + y, a[2] + z]
        return self

    def p(self, bone, x=0, y=0, z=0):
        o = self.setdefault(bone, [None, None])
        a = o[1] or [0, 0, 0]
        o[1] = [a[0] + x, a[1] + y, a[2] + z]
        return self

    def s(self, bone, x=1, y=1, z=1):
        self.scales[bone] = [x, y, z]
        return self


ANIMS = {}


def anim(creature, name, length, loop=True, hold=False):
    def deco(fn):
        ANIMS.setdefault(creature, {})[name] = (fn, length, loop, hold)
        return fn
    return deco


# ============================================================================ the Name Caller
def caller_base(P, t, breathe=1.0):
    b = math.sin(t * math.tau * 0.22)
    P.r("chest", -1.2 * b * breathe).r("waist", 0.6 * b * breathe)
    for i in range(5):
        for side, sg in SIDES:
            P.r(f"fing{i}_{side}", 4 * noise(t, i * 3 + sg, 0.8) + 6 * max(0, twitch(t, i + sg * 5, 0.37, 30)))
    return b


def lips(P, a):
    """a: 0 shut .. 1 split wide"""
    P.r("lip_l", 0, 38 * a, 0).r("lip_r", 0, -38 * a, 0)
    P.p("lip_l", 0.6 * a, 0, 0).p("lip_r", -0.6 * a, 0, 0)


@anim("caller", "idle", 8.0)
def caller_idle(t):
    """it listens: the head tilts by slow degrees one way, then the other, then jerks"""
    P = Pose()
    caller_base(P, t)
    tilt = math.sin(t * math.tau / 8) * 14 + twitch(t, 2, 0.25, 26) * 9
    P.r("head", noise(t, 1, 0.2) * 4, noise(t, 3, 0.15) * 10, tilt)
    P.r("neck", 0, noise(t, 4, 0.12) * 6, 0)
    lips(P, 0.05 + 0.05 * max(0, noise(t, 5, 0.6)))
    return P


@anim("caller", "whisper", 4.0)
def caller_whisper(t):
    """it says a name: the split face parts on each syllable, the head leaning in"""
    P = Pose()
    caller_base(P, t)
    syl = max(0.0, math.sin(t * math.tau * 1.25)) ** 2 * (0.6 + 0.4 * math.sin(t * 2.1))
    lips(P, 0.12 + 0.35 * syl)
    P.r("head", 8 + 4 * syl, noise(t, 7, 0.3) * 5, -10 + noise(t, 8, 0.2) * 6)
    P.r("neck", 6, 0, 0)
    P.r("chest", 3, 0, 0)
    return P


@anim("caller", "walk", 3.2)
def caller_walk(t):
    """it pulls itself through the blood on its hands, left, right, the body rolling after"""
    P = Pose()
    caller_base(P, t, 0.5)
    ph = t / 3.2 * math.tau
    for side, sg in SIDES:
        s = math.sin(ph + (0 if sg > 0 else math.pi))
        lift = max(0.0, s)
        P.r(f"arm_{side}", -10 * s - 8, 0, sg * 10 * lift)
        P.r(f"fore_{side}", -14 * lift, 0, -sg * 6 * lift)
        P.r(f"hand_{side}", 10 * lift)
        for i in range(5):
            P.r(f"fing{i}_{side}", 20 * lift)
    P.r("waist", 3, math.sin(ph) * 6, math.sin(ph) * 4)
    P.r("chest", 6, math.sin(ph) * 5, -math.sin(ph) * 3)
    P.r("head", 0, -math.sin(ph) * 6, -14 + math.sin(ph) * 5)
    P.p("root", 0, abs(math.sin(ph)) * 2, 0)
    lips(P, 0.08)
    return P


@anim("caller", "scream", 2.4, loop=False)
def caller_scream(t):
    """found: the head snaps up, the face splits wide open, the arms rise out of the blood"""
    P = Pose()
    up = ramp(t, 0, 0.25) * (1 - ramp(t, 1.9, 2.4))
    j = math.sin(t * 60) * up
    caller_base(P, t)
    lips(P, up * 1.25 + j * 0.05)
    P.r("head", -28 * up + j * 3, j * 4, -6 * up)
    P.r("neck", -16 * up).r("chest", -10 * up)
    for side, sg in SIDES:
        P.r(f"arm_{side}", -25 * up, 0, sg * 26 * up)
        P.r(f"fore_{side}", -30 * up)
        for i in range(5):
            P.r(f"fing{i}_{side}", -25 * up + j * 6)
    return P


@anim("caller", "lunge", 1.4, loop=False)
def caller_lunge(t):
    """it falls on you from its full height, arms first"""
    P = Pose()
    go = ramp(t, 0.1, 0.45)
    back = ramp(t, 0.9, 1.4)
    k = go * (1 - back)
    caller_base(P, t)
    P.r("waist", 18 * k).r("chest", 26 * k).r("neck", 10 * k)
    P.r("head", -20 * k)
    lips(P, k * 1.2)
    for side, sg in SIDES:
        P.r(f"arm_{side}", -70 * k, 0, -sg * 8 * k)
        P.r(f"fore_{side}", -10 * k)
        for i in range(5):
            P.r(f"fing{i}_{side}", -20 * k)
    P.p("root", 0, 0, -18 * k)
    return P


@anim("caller", "sink", 3.0, loop=False, hold=True)
def caller_sink(t):
    P = Pose()
    k = ramp(t, 0, 3.0)
    caller_base(P, t)
    P.p("root", 0, -150 * k, 0)
    P.r("head", 10 * k, 0, -20)
    return P


@anim("caller", "rise", 3.0, loop=False)
def caller_rise(t):
    P = Pose()
    k = 1 - ramp(t, 0, 2.6)
    caller_base(P, t)
    P.p("root", 0, -150 * k, 0)
    P.r("head", 30 * k, 0, -20 * k)
    for side, sg in SIDES:
        P.r(f"arm_{side}", 0, 0, -sg * 30 * k)
    return P


# ============================================================================ the Bone Gatherer
SKULLS = 13


def gatherer_rattle(P, t, amt):
    for i in range(SKULLS):
        P.r(f"sk{i}", twitch(t, i * 1.7, 0.31 + i * 0.03, 28) * 9 * amt, twitch(t, i * 2.3 + 1, 0.27 + i * 0.02, 26) * 12 * amt, 0)


@anim("gatherer", "idle", 6.0)
def gatherer_idle(t):
    P = Pose()
    b = math.sin(t * math.tau / 6)
    P.r("torso", 2 * b, noise(t, 1, 0.2) * 3, 0).r("neck", noise(t, 2, 0.3) * 4, 0, 0)
    P.r("head", noise(t, 3, 0.25) * 5, noise(t, 4, 0.2) * 8, noise(t, 5, 0.3) * 4)
    for side, sg in SIDES:
        P.r(f"arm_{side}", -3 + noise(t, sg, 0.3) * 3, 0, sg * (4 + noise(t, sg + 4, 0.25) * 2))
        P.r(f"fore_{side}", -8 + noise(t, sg + 7, 0.3) * 3)
        for i in range(5):
            P.r(f"fing{i}_{side}", 10 + 6 * noise(t, i + sg * 9, 0.6))
    gatherer_rattle(P, t, 0.5)
    return P


@anim("gatherer", "walk", 2.6)
def gatherer_walk(t):
    """a heavy lurch: each step drops the whole weight onto one leg, the crown of skulls swaying after"""
    P = Pose()
    ph = t / 2.6 * math.tau
    for side, sg in SIDES:
        s = math.sin(ph + (0 if sg > 0 else math.pi))
        up = max(0.0, -math.cos(ph + (0 if sg > 0 else math.pi)))
        P.r(f"thigh_{side}", -22 * s - 12 * up)
        P.r(f"shin_{side}", 26 * up)
        P.r(f"arm_{side}", 14 * s - 4, 0, sg * 6)
        P.r(f"fore_{side}", -12 - 8 * max(0, s))
        for i in range(5):
            P.r(f"fing{i}_{side}", 14 + 8 * up)
    drop = abs(math.sin(ph))
    P.p("root", 0, -3 * drop, 0)
    P.r("pelvis", 0, math.sin(ph) * 6, math.sin(ph) * 4)
    P.r("torso", 6, -math.sin(ph) * 4, -math.sin(ph) * 5)
    P.r("head", 4 * drop, math.sin(ph - 0.6) * 6, math.sin(ph - 0.8) * 7)
    gatherer_rattle(P, t, 1.0)
    return P


@anim("gatherer", "swipe", 1.6, loop=False)
def gatherer_swipe(t):
    """the right arm comes round low and wide, fingers spread"""
    P = Pose()
    wind = ramp(t, 0, 0.45) * (1 - ramp(t, 0.55, 0.75))
    hit = ramp(t, 0.5, 0.75) * (1 - ramp(t, 1.1, 1.6))
    P.r("torso", 4 * hit, 30 * wind - 35 * hit, 0)
    P.r("arm_r", -40 * wind - 70 * hit, 30 * wind - 40 * hit, -50 * wind - 20 * hit)
    P.r("fore_r", -30 * wind - 10 * hit)
    for i in range(5):
        P.r(f"fing{i}_r", -20 * hit + 20 * wind, 0, 0)
    P.r("arm_l", 10 * hit, 0, 10 * hit)
    P.r("head", 10 * hit, -20 * hit, 0)
    gatherer_rattle(P, t, 1.5)
    return P


@anim("gatherer", "assemble", 4.0, loop=False)
def gatherer_assemble(t):
    """it pulls itself together out of the blood: skulls rolling into place, then it stands"""
    P = Pose()
    k = 1 - ramp(t, 0.2, 3.2)
    P.p("root", 0, -110 * k, 0)
    P.r("torso", 50 * k, 0, 20 * k * math.sin(t * 7))
    P.r("head", 40 * k, 0, 0)
    for i in range(SKULLS):
        late = 1 - ramp(t, 1.0 + i * 0.14, 1.4 + i * 0.14)
        P.r(f"sk{i}", 120 * late * (1 if i % 2 else -1), 90 * late, 0)
        P.s(f"sk{i}", 1 - 0.9 * late, 1 - 0.9 * late, 1 - 0.9 * late)
    for side, sg in SIDES:
        P.r(f"arm_{side}", -60 * k, 0, sg * 40 * k)
        P.r(f"fore_{side}", -50 * k)
    gatherer_rattle(P, t, 2.0)
    return P


@anim("gatherer", "roar", 2.4, loop=False)
def gatherer_roar(t):
    P = Pose()
    up = ramp(t, 0, 0.3) * (1 - ramp(t, 1.9, 2.4))
    P.r("torso", -14 * up).r("neck", -10 * up).r("head", -18 * up)
    for side, sg in SIDES:
        P.r(f"arm_{side}", -20 * up, 0, sg * 35 * up)
        P.r(f"fore_{side}", -25 * up)
        for i in range(5):
            P.r(f"fing{i}_{side}", -25 * up)
    gatherer_rattle(P, t * 3, 2.5 * up)
    return P


# ============================================================================ the Wake Follower
SEGS = 7


def wake_wave(P, t, speed, amp, pitch=1.0):
    for i in range(SEGS):
        P.r(f"seg{i}", math.sin(t * math.tau * speed * 0.5 - i * 0.7) * 1.5 * pitch, math.sin(t * math.tau * speed - i * 0.85) * amp * (0.5 + i * 0.12), 0)
    P.r("fluke", math.sin(t * math.tau * speed - 6.5) * 14 * pitch, math.sin(t * math.tau * speed - 7) * amp, 0)
    P.r("head", 0, -math.sin(t * math.tau * speed + 0.4) * amp * 0.6, 0)


@anim("wake", "swim", 4.0)
def wake_swim(t):
    P = Pose()
    wake_wave(P, t, 0.5, 5)
    P.p("root", 0, -6 + math.sin(t * math.tau / 4) * 3, 0)
    return P


@anim("wake", "follow", 2.0)
def wake_follow(t):
    """faster, lower, only the ridge of its back cutting the blood"""
    P = Pose()
    wake_wave(P, t, 1.0, 7, 0.6)
    P.p("root", 0, -12 + math.sin(t * math.tau / 2) * 1.5, 0)
    return P


@anim("wake", "lunge", 2.2, loop=False)
def wake_lunge(t):
    """the head comes up out of the blood, the underside of it splits, and it comes down on whatever is there"""
    P = Pose()
    up = ramp(t, 0.1, 0.7) * (1 - ramp(t, 1.0, 1.3))
    slam = ramp(t, 1.0, 1.3) * (1 - ramp(t, 1.6, 2.2))
    wake_wave(P, t, 1.3, 4)
    P.r("seg0", -35 * up + 10 * slam).r("seg1", -12 * up + 6 * slam).r("seg2", 10 * up)
    P.r("head", -15 * up + 20 * slam)
    P.r("jaw", 55 * up * (1 - slam * 0.9))
    P.p("root", 0, 20 * up - 4 * slam, -20 * up - 30 * slam)
    return P


@anim("wake", "dive", 2.0, loop=False, hold=True)
def wake_dive(t):
    P = Pose()
    k = ramp(t, 0, 2.0)
    wake_wave(P, t, 0.8, 5)
    P.r("seg0", 20 * k).r("seg1", 8 * k)
    P.p("root", 0, -90 * k, 0)
    return P


# ============================================================================ the Changer
LEGS = 6


def changer_eye(P, t, open_, seed=0, look=1.0):
    """open_: 0 shut .. 1 wide; the eye flicks about by itself"""
    blink = math.exp(-(((t * 0.31 + seed) % 1.0 - 0.5) * 40) ** 2)
    o = open_ * (1 - blink)
    P.r("lid_up", 62 * (1 - o)).r("lid_down", -30 * (1 - o))
    sx = math.floor(t * 1.3 + seed)
    P.r("eye", (noise(sx, 2) * 18) * look, (noise(sx, 5) * 30) * look, 0)


def legs_hidden(P):
    for i in range(LEGS):
        P.s(f"leg{i}", 0.05, 0.05, 0.05)


@anim("changer", "disguise", 6.0)
def changer_disguise(t):
    """a barrel. It breathes, very slightly, if you watch for long enough"""
    P = Pose()
    legs_hidden(P)
    changer_eye(P, t, 0.0, look=0)
    b = math.sin(t * math.tau / 6)
    P.s("body", 1 + 0.006 * b, 1 - 0.004 * b, 1 + 0.006 * b)
    for i in range(5):
        P.r(f"fing{i}", 2 * noise(t, i, 0.3))
    return P


@anim("changer", "peek", 5.0)
def changer_peek(t):
    """your back is turned: the eye opens and goes to and fro, the fingers curl on the rim"""
    P = Pose()
    legs_hidden(P)
    changer_eye(P, t, 0.85, 0.2)
    for i in range(5):
        P.r(f"fing{i}", 10 + 12 * max(0, noise(t, i * 3, 0.8)))
    P.r("lid", -4 - 3 * max(0, noise(t, 9, 0.5)))
    return P


@anim("changer", "reveal", 1.6, loop=False)
def changer_reveal(t):
    """caught: the lid flies up, the hand grips, the legs come out from under it"""
    P = Pose()
    k = ramp(t, 0, 0.35)
    legs = ramp(t, 0.3, 1.0)
    changer_eye(P, t, 1.0, 0.7, 0.4)
    P.r("lid", -45 * k * (1 - ramp(t, 0.9, 1.4)) - 6)
    for i in range(LEGS):
        P.s(f"leg{i}", 0.05 + 0.95 * legs, 0.05 + 0.95 * legs, 0.05 + 0.95 * legs)
        P.r(f"leg{i}b", 30 * (1 - legs))
    for i in range(5):
        P.r(f"fing{i}", 30 * k)
    P.p("body", 0, 6 * legs, 0)
    return P


@anim("changer", "run", 0.6)
def changer_run(t):
    """scuttling on six long fingers, the barrel bouncing on top of them"""
    P = Pose()
    ph = t / 0.6 * math.tau
    changer_eye(P, t, 1.0, 0.3, 0.3)
    for i in range(LEGS):
        s = math.sin(ph + i * math.pi * (1 if i % 2 else 0) + (math.pi if i % 2 else 0))
        P.r(f"leg{i}", 0, 18 * s, 0).r(f"leg{i}b", -14 * max(0, s))
    P.p("body", 0, 6 + 1.2 * abs(math.sin(ph * 2)), 0)
    P.r("body", 3 * math.sin(ph * 2), 0, 4 * math.sin(ph))
    P.r("lid", -8 - 6 * abs(math.sin(ph)))
    return P


@anim("changer", "bite", 0.9, loop=False)
def changer_bite(t):
    P = Pose()
    o = ramp(t, 0, 0.25) * (1 - ramp(t, 0.35, 0.45))
    changer_eye(P, t, 1.0, 0.1, 0)
    P.r("lid", -70 * o)
    P.p("body", 0, 6, -4 * o)
    for i in range(LEGS):
        P.r(f"leg{i}b", 10 * o)
    for i in range(5):
        P.r(f"fing{i}", -30 * o)
    return P


# ============================================================================ the Leviathan
@anim("leviathan", "idle", 7.0)
def lev_idle(t):
    P = Pose()
    b = math.sin(t * math.tau / 7)
    P.r("head", -2 * b + noise(t, 1, 0.2), noise(t, 2, 0.15) * 2, noise(t, 3, 0.12) * 1.5)
    P.r("jaw", 5 * b + 2 * max(0, noise(t, 4, 0.4)))
    P.r("neck1", b * 1.5).r("neck2", -b)
    P.p("root", 0, b * 6, 0)
    return P


@anim("leviathan", "rise", 5.0, loop=False)
def lev_rise(t):
    """out of the blood: the crown of the skull first, then the eyes, then the jaw, streaming"""
    P = Pose()
    k = 1 - ramp(t, 0, 4.5)
    P.p("root", 0, -420 * k, 0)
    P.r("head", -18 * k).r("jaw", -28 * k).r("neck1", 10 * k)
    return P


@anim("leviathan", "roar", 4.0, loop=False)
def lev_roar(t):
    P = Pose()
    up = ramp(t, 0.2, 0.9) * (1 - ramp(t, 3.2, 4.0))
    sh = math.sin(t * 40) * up
    P.r("head", -16 * up + sh * 1.2, sh * 0.8, 0).r("jaw", 30 * up + sh * 2)
    P.r("neck1", -6 * up).r("neck2", -4 * up)
    P.p("root", 0, 20 * up, 0)
    return P


@anim("leviathan", "bite", 2.6, loop=False)
def lev_bite(t):
    """wide, then forward and down, and shut"""
    P = Pose()
    op = ramp(t, 0, 0.8) * (1 - ramp(t, 1.15, 1.35))
    lunge = ramp(t, 0.8, 1.3) * (1 - ramp(t, 1.8, 2.6))
    P.r("jaw", 30 * op - 30 * lunge)
    P.r("head", -12 * op + 18 * lunge).r("neck1", 8 * lunge).r("neck2", 4 * lunge)
    P.p("root", 0, 10 * op - 40 * lunge, -160 * lunge)
    return P


@anim("leviathan", "sink", 5.0, loop=False, hold=True)
def lev_sink(t):
    P = Pose()
    k = ramp(t, 0.3, 5.0)
    P.p("root", 0, -420 * k, 0)
    P.r("head", 14 * k).r("jaw", -20 * k)
    return P


# ============================================================================ writer
def build(creature):
    out = {"format_version": "1.8.0", "animations": {}}
    for name, (fn, length, loop, hold) in ANIMS[creature].items():
        frames = int(round(length / DT))
        bones = {}
        for i in range(frames + 1):
            t = i * DT
            P = fn(t if i < frames or not loop else 0.0)
            for bone, sc in P.scales.items():
                bones.setdefault(bone, {}).setdefault("scale", {})[f"{t:.2f}"] = [round(v, 3) for v in sc]
            for bone, (r, p) in P.items():
                b = bones.setdefault(bone, {})
                if r is not None:
                    b.setdefault("rotation", {})[f"{t:.2f}"] = [round(v, 2) for v in r]
                if p is not None:
                    b.setdefault("position", {})[f"{t:.2f}"] = [round(v, 3) for v in p]
        for bone, chans in bones.items():
            for ch in list(chans):
                vals = list(chans[ch].values())
                if all(v == vals[0] for v in vals):
                    chans[ch] = vals[0]
        a = {"animation_length": length, "bones": bones}
        if loop:
            a["loop"] = True
        elif hold:
            a["loop"] = "hold_on_last_frame"
        out["animations"][f"animation.{creature}.{name}"] = a
    os.makedirs(A, exist_ok=True)
    with open(os.path.join(A, f"{creature}.animation.json"), "w") as f:
        json.dump(out, f, separators=(",", ":"))
    return len(out["animations"])


if __name__ == "__main__":
    for c in ANIMS:
        print(c, build(c), "animations")
