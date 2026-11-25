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


# ============================================================================ the raft
def raft_common(P, t, wind=1.0):
    P.r("lantern", noise(t, 1, 0.7) * 6 * wind, 0, noise(t, 2, 0.6) * 8 * wind)
    P.r("mast", 0, 0, noise(t, 3, 0.3) * 0.8 * wind)


@anim("raft", "idle", 8.0)
def raft_idle(t):
    P = Pose()
    raft_common(P, t, 0.6)
    P.r("oar", 0, noise(t, 4, 0.2) * 3, 0)
    return P


@anim("raft", "row", 2.0)
def raft_row(t):
    """the steering oar swept from side to side over the stern"""
    P = Pose()
    raft_common(P, t, 1.0)
    ph = t / 2.0 * math.tau
    P.r("oar", 6 * max(0, math.sin(ph * 2)), 26 * math.sin(ph), 0)
    return P


# ============================================================================ Pip
def pip_base(P, t, blink_seed=0, eyes=1.0, fish=False):
    """breath, ear flicks, blinks; eyes 0 shut .. 1 open"""
    b = math.sin(t * math.tau * 0.45)
    P.r("body", 0.8 * b).s("body", 1, 1 + 0.01 * b, 1)
    blink = math.exp(-(((t * 0.23 + blink_seed) % 1.0 - 0.5) * 30) ** 2)
    shut = max(0.0, min(1.0, 1 - eyes + blink))
    P.r("lid_l", 75 * shut).r("lid_r", 75 * shut)
    for side, sg in SIDES:
        f = max(0.0, twitch(t, 3 + sg, 0.29, 40))
        P.r(f"ear_{side}", 0, sg * -25 * f, sg * 12 * f)
    if not fish:
        P.s("fish", 0.01, 0.01, 0.01)
    return b


def tail_curl(P, t, up=1.0, sway=1.0, puff=1.0):
    for i in range(6):
        P.r(f"tail{i}", noise(t, i * 1.3, 0.5) * 4 * sway - (1 - up) * (14 if i < 2 else 4), math.sin(t * 1.6 - i * 0.5) * 6 * sway * (0.3 + i * 0.15), 0)
        if puff != 1.0:
            P.s(f"tail{i}", puff, puff, 1)


def legs_stand(P):
    pass


def legs_walk(P, ph, stride=1.0, lift=1.0):
    order = {"fl_l": 0.0, "hl_r": 0.0, "fl_r": 0.5, "hl_l": 0.5}
    for leg, off in order.items():
        a = (ph + off) * math.tau
        sw = math.sin(a)
        up = max(0.0, math.cos(a))
        side = leg[-1]
        if leg.startswith("fl"):
            P.r(leg, -22 * sw * stride)
            P.r(f"flp_{side}", 28 * up * lift)
        else:
            P.r(leg, -20 * sw * stride)
            P.r(f"hll_{side}", -24 * up * lift)


def sit_pose(P, k=1.0):
    """haunches down, chest up, front legs straight under the chest, tail round the feet"""
    P.p("root", 0, -2.0 * k, 0.6 * k)
    P.r("body", -34 * k)
    P.r("neck", 18 * k).r("head", 14 * k)
    for side, sg in SIDES:
        P.r(f"fl_{side}", 34 * k).r(f"flp_{side}", 0)
        P.r(f"hl_{side}", -60 * k).r(f"hll_{side}", 85 * k)
    for i in range(6):
        P.r(f"tail{i}", (40 if i == 0 else 8) * k, (22 + i * 10) * k, 0)


@anim("pip", "stand", 6.0)
def pip_stand(t):
    P = Pose()
    pip_base(P, t)
    tail_curl(P, t)
    P.r("head", noise(t, 2, 0.25) * 6, noise(t, 3, 0.2) * 18, noise(t, 4, 0.2) * 5)
    return P


@anim("pip", "walk", 0.9)
def pip_walk(t):
    P = Pose()
    ph = t / 0.9
    pip_base(P, t, 0.3)
    legs_walk(P, ph)
    P.p("root", 0, abs(math.sin(ph * math.tau)) * 0.25, 0)
    P.r("body", 0, math.sin(ph * math.tau) * 3, 0)
    P.r("head", 2 * math.sin(ph * math.tau * 2), -math.sin(ph * math.tau) * 3, 0)
    tail_curl(P, t, 1.0, 0.6)
    return P


@anim("pip", "run", 0.45)
def pip_run(t):
    P = Pose()
    ph = t / 0.45
    pip_base(P, t, 0.5)
    a = ph * math.tau
    for side in "lr":
        P.r(f"fl_{side}", -40 * math.sin(a + (0.15 if side == "l" else 0))).r(f"flp_{side}", 40 * max(0, math.cos(a)))
        P.r(f"hl_{side}", 38 * math.sin(a + (0.15 if side == "l" else 0))).r(f"hll_{side}", -40 * max(0, -math.cos(a)))
    P.r("body", 6 * math.sin(a)).p("root", 0, 0.6 * abs(math.sin(a)), 0)
    P.r("head", -6 * math.sin(a))
    tail_curl(P, t, 0.4, 0.3)
    return P


@anim("pip", "sit", 8.0)
def pip_sit(t):
    P = Pose()
    sit_pose(P)
    pip_base(P, t, 0.1)
    P.r("head", noise(t, 5, 0.2) * 5, noise(t, 6, 0.15) * 20, noise(t, 7, 0.2) * 6)
    # the tip of the tail flicks now and then
    P.r("tail5", 0, 25 * twitch(t, 1, 0.4, 20), 0).r("tail4", 0, 10 * twitch(t, 1, 0.4, 20), 0)
    return P


@anim("pip", "sleep", 6.0)
def pip_sleep(t):
    """curled up: legs tucked under, head down on the paws, tail wrapped round the nose, breathing slow"""
    P = Pose()
    b = math.sin(t * math.tau / 3.0)
    P.p("root", 0, -3.6, 0)
    P.r("body", 0.6 * b, 0, 3).s("body", 1 + 0.015 * b, 1 + 0.02 * b, 1)
    P.r("neck", 34).r("head", 26, -22, -8)
    for side, sg in SIDES:
        P.r(f"fl_{side}", 82).r(f"flp_{side}", -12)
        P.r(f"hl_{side}", -62).r(f"hll_{side}", 110)
    for i in range(6):
        P.r(f"tail{i}", 50 if i == 0 else 4, -(25 + i * 14), 0)
    P.r("lid_l", 75).r("lid_r", 75)
    P.s("fish", 0.01, 0.01, 0.01)
    for side, sg in SIDES:
        P.r(f"ear_{side}", 0, 0, sg * 6 * max(0, twitch(t, 2 + sg, 0.17, 40)))
    return P


@anim("pip", "groom", 4.0)
def pip_groom(t):
    """licking a raised front paw, then wiping it over the ear"""
    P = Pose()
    sit_pose(P)
    pip_base(P, t, 0.7, eyes=0.5)
    lick = math.sin(t * math.tau * 1.6)
    wipe = ramp(t, 2.4, 2.8) * (1 - ramp(t, 3.5, 3.9))
    P.r("fl_l", -30 - 30 * wipe).r("flp_l", 75 - 20 * wipe)
    P.r("head", 22 + 6 * lick - 30 * wipe, 22, 10 * wipe)
    P.r("jaw", 8 + 6 * max(0, lick))
    return P


@anim("pip", "stretch", 3.0, loop=False)
def pip_stretch(t):
    """front paws out, chest down, rear up, a long yawn"""
    P = Pose()
    k = ramp(t, 0.2, 0.9) * (1 - ramp(t, 2.2, 2.9))
    pip_base(P, t, 0, eyes=1 - 0.8 * k)
    P.r("body", 20 * k).p("root", 0, -1.0 * k, 0)
    for side in "lr":
        P.r(f"fl_{side}", -55 * k).r(f"flp_{side}", -10 * k)
        P.r(f"hl_{side}", -12 * k)
    P.r("neck", -10 * k).r("head", -20 * k).r("jaw", 32 * ramp(t, 0.6, 1.0) * (1 - ramp(t, 1.8, 2.2)))
    tail_curl(P, t, 1.0, 0.4)
    return P


@anim("pip", "meow", 1.4, loop=False)
def pip_meow(t):
    P = Pose()
    o = ramp(t, 0.15, 0.35) * (1 - ramp(t, 0.8, 1.1))
    pip_base(P, t)
    P.r("head", -18 * o).r("jaw", 26 * o)
    tail_curl(P, t)
    return P


@anim("pip", "rub", 2.4)
def pip_rub(t):
    """pressing the side of its head along your legs, tail high and quivering"""
    P = Pose()
    pip_base(P, t, eyes=0.4)
    w = math.sin(t / 2.4 * math.tau)
    P.r("head", 6, 30 * w, 26 * w).r("neck", 0, 12 * w, 10 * w)
    P.r("body", 0, 6 * w, 8 * w)
    P.p("root", 0, 0.3 * abs(w), 0)
    tail_curl(P, t * 4, 1.0, 0.25)
    return P


@anim("pip", "knead", 1.2)
def pip_knead(t):
    P = Pose()
    P.p("root", 0, -3.2, 0)
    for side, sg in SIDES:
        P.r(f"hl_{side}", -62).r(f"hll_{side}", 110)
    a = t / 1.2 * math.tau
    P.r("fl_l", -40 + 22 * max(0, math.sin(a))).r("flp_l", 50 - 30 * max(0, math.sin(a)))
    P.r("fl_r", -40 + 22 * max(0, -math.sin(a))).r("flp_r", 50 - 30 * max(0, -math.sin(a)))
    pip_base(P, t, eyes=0.25)
    P.r("head", 8, 0, 0)
    for i in range(6):
        P.r(f"tail{i}", 40 if i == 0 else 4, -(20 + i * 8), 0)
    return P


@anim("pip", "hiss", 1.6)
def pip_hiss(t):
    """back arched, every hair up, ears flat, teeth bared"""
    P = Pose()
    pip_base(P, t, eyes=1.0)
    j = math.sin(t * 50) * 0.5
    P.p("root", 0, 1.2, 0).s("body", 1.1, 1.15, 1.05)
    P.r("body", 0, 0, j)
    P.r("neck", 20).r("head", -12).r("jaw", 30 + j * 4)
    for side, sg in SIDES:
        P.r(f"ear_{side}", 50, sg * 30, 0)
        P.r(f"fl_{side}", 8).r(f"hl_{side}", -6)
    for i in range(6):
        P.r(f"tail{i}", -6 if i < 3 else 2, 0, 0).s(f"tail{i}", 1.3, 1.3, 1.3)
    return P


@anim("pip", "eat", 2.0)
def pip_eat(t):
    P = Pose()
    pip_base(P, t, eyes=0.6)
    chew = max(0.0, math.sin(t * math.tau * 2.5))
    P.r("neck", 42).r("head", 18 + 4 * chew).r("jaw", 14 * chew)
    for side in "lr":
        P.r(f"fl_{side}", 14).r(f"flp_{side}", 18)
    tail_curl(P, t, 0.8, 0.5)
    return P


@anim("pip", "carry", 0.9)
def pip_carry(t):
    """trotting back with a fish in its mouth, head high, very proud"""
    P = pip_walk(t)
    P.scales.pop("fish", None)
    P.r("head", -12).r("jaw", 8)
    return P


@anim("pip", "drop", 1.6, loop=False)
def pip_drop(t):
    """lays the fish at your feet and looks up at you"""
    P = Pose()
    k = ramp(t, 0.1, 0.5)
    pip_base(P, t, fish=t < 0.7)
    P.r("neck", 40 * k * (1 - ramp(t, 0.8, 1.2))).r("jaw", 14 * ramp(t, 0.5, 0.7))
    P.r("head", -20 * ramp(t, 1.0, 1.4))
    tail_curl(P, t)
    return P


@anim("pip", "look_up", 5.0)
def pip_look_up(t):
    """sitting, staring at the sky, the pupils wide"""
    P = Pose()
    sit_pose(P)
    pip_base(P, t, eyes=1.0)
    P.r("neck", -8).r("head", -38, noise(t, 1, 0.1) * 6, 0)
    for side, sg in SIDES:
        P.r(f"ear_{side}", -10, sg * -8, 0)
    return P


@anim("pip", "scared", 1.0)
def pip_scared(t):
    """flat to the deck, trembling, tail tucked"""
    P = Pose()
    pip_base(P, t, eyes=1.0)
    j = math.sin(t * 70) * 0.6
    P.p("root", 0, -2.2, 0.5).r("body", 3, 0, j)
    for side, sg in SIDES:
        P.r(f"fl_{side}", -30).r(f"flp_{side}", 60)
        P.r(f"hl_{side}", -40).r(f"hll_{side}", 70)
        P.r(f"ear_{side}", 55, sg * 35, 0)
    P.r("neck", 15).r("head", 5)
    for i in range(6):
        P.r(f"tail{i}", 60 if i == 0 else 12, 0, 0)
    return P


@anim("pip", "petted", 3.0)
def pip_petted(t):
    """eyes shut, pushing its head up into the hand, purring"""
    P = Pose()
    sit_pose(P)
    pip_base(P, t, eyes=0.0)
    w = math.sin(t / 3.0 * math.tau)
    P.r("head", -14 + 6 * w, 10 * w, 12 * w).r("neck", -6)
    for side, sg in SIDES:
        P.r(f"ear_{side}", 14, sg * 10, 0)
    P.s("body", 1, 1 + 0.01 * math.sin(t * 60), 1)
    return P


@anim("pip", "swipe", 1.8, loop=False)
def pip_swipe(t):
    """crouched at the edge, one paw strikes down into the blood and hooks something out"""
    P = Pose()
    pip_base(P, t, fish=t > 1.0)
    crouch = ramp(t, 0, 0.4)
    hit = ramp(t, 0.55, 0.7) * (1 - ramp(t, 0.9, 1.3))
    P.p("root", 0, -1.5 * crouch, 0).r("body", 12 * crouch)
    P.r("fl_r", -30 * crouch - 60 * hit).r("flp_r", 40 * crouch - 30 * hit)
    P.r("neck", 30 * crouch).r("head", 10 * crouch)
    tail_curl(P, t, 0.5, 0.2)
    return P


@anim("pip", "struggle", 0.5)
def pip_struggle(t):
    """caught: every leg clawing at the air, twisting, screaming"""
    P = Pose()
    pip_base(P, t, eyes=1.0)
    a = t / 0.5 * math.tau
    for side, sg in SIDES:
        P.r(f"fl_{side}", -50 + 40 * math.sin(a * 2 + sg), 0, sg * 20 * math.sin(a * 3))
        P.r(f"hl_{side}", 30 + 40 * math.sin(a * 2 - sg)).r(f"hll_{side}", 30 * math.sin(a * 3))
        P.r(f"ear_{side}", 50, sg * 30, 0)
    P.r("body", 10 * math.sin(a), 15 * math.sin(a * 1.5), 25 * math.sin(a))
    P.r("head", -25 + 10 * math.sin(a * 2), 20 * math.sin(a * 1.3)).r("jaw", 32)
    for i in range(6):
        P.r(f"tail{i}", 20 * math.sin(a * 2 - i), 25 * math.sin(a * 1.5 - i), 0).s(f"tail{i}", 1.25, 1.25, 1.25)
    return P


@anim("pip", "jump", 1.0, loop=False)
def pip_jump(t):
    P = Pose()
    pip_base(P, t)
    c = ramp(t, 0, 0.25) * (1 - ramp(t, 0.25, 0.35))
    air = ramp(t, 0.3, 0.45) * (1 - ramp(t, 0.7, 0.85))
    P.p("root", 0, -1.2 * c + 6 * air, -4 * air)
    for side in "lr":
        P.r(f"fl_{side}", 30 * c - 50 * air).r(f"hl_{side}", -20 * c + 40 * air)
    P.r("body", 10 * c - 15 * air)
    tail_curl(P, t, 1.0, 0.2)
    return P


# ============================================================================ Elias
# poses are dicts: bone -> (rx, ry, rz), plus "@root" -> position (x, y, z); blended with ease
E_STAND = {"arm_l": (0, 0, 4), "arm_r": (0, 0, -4), "fore_l": (-6, 0, 0), "fore_r": (-6, 0, 0)}
E_LYING = {"root": (90, 0, 0), "@root": (0, 2.4, 0), "head": (-20, 30, 0), "neck": (-10, 0, 0),
           "arm_l": (-20, 0, 30), "arm_r": (-150, 0, -20), "fore_l": (-40, 0, 0), "fore_r": (-30, 0, 0),
           "thigh_l": (0, 0, 6), "thigh_r": (-10, 0, -4), "shin_r": (20, 0, 0), "foot_l": (30, 0, 0), "foot_r": (40, 0, 0)}
E_PUSH = {"root": (70, 0, 0), "@root": (0, 4.5, 0), "head": (-45, 0, 0), "neck": (-20, 0, 0),
          "arm_l": (-70, 0, 10), "arm_r": (-70, 0, -10), "fore_l": (-20, 0, 0), "fore_r": (-20, 0, 0),
          "hand_l": (60, 0, 0), "hand_r": (60, 0, 0), "foot_l": (30, 0, 0), "foot_r": (30, 0, 0)}
E_FOURS = {"@root": (0, -6.6, 0), "hips": (75, 0, 0), "thigh_l": (-75, 0, 0), "thigh_r": (-75, 0, 0), "shin_l": (90, 0, 0), "shin_r": (90, 0, 0),
           "foot_l": (10, 0, 0), "foot_r": (10, 0, 0), "chest": (5, 0, 0), "neck": (-30, 0, 0), "head": (-15, 0, 0),
           "arm_l": (-80, 0, 6), "arm_r": (-80, 0, -6), "fore_l": (0, 0, 0), "fore_r": (0, 0, 0), "hand_l": (70, 0, 0), "hand_r": (70, 0, 0)}
E_KNEELSIT = {"@root": (0, -9.6, 2.5), "thigh_l": (-88, 0, 4), "thigh_r": (-88, 0, -4), "shin_l": (170, 0, 0), "shin_r": (170, 0, 0),
              "foot_l": (-60, 0, 0), "foot_r": (-60, 0, 0), "torso": (6, 0, 0), "chest": (8, 0, 0), "neck": (10, 0, 0), "head": (10, 0, 0),
              "arm_l": (-10, 0, 8), "arm_r": (-10, 0, -8), "fore_l": (-30, 0, 0), "fore_r": (-30, 0, 0)}
E_HANDS = dict(E_KNEELSIT, **{"arm_l": (-45, 0, 12), "arm_r": (-45, 0, -12), "fore_l": (-70, 20, 0), "fore_r": (-70, -20, 0),
                              "hand_l": (0, 0, -60), "hand_r": (0, 0, 60), "neck": (25, 0, 0), "head": (20, 0, 0)})
E_HOLDHEAD = dict(E_KNEELSIT, **{"arm_r": (-150, 0, -30), "fore_r": (-80, 0, 0), "hand_r": (0, 40, 0), "neck": (18, 0, 0), "head": (14, 10, -8)})
E_KNEEL = {"@root": (0, -6.2, 0), "thigh_l": (-90, 0, 0), "shin_l": (90, 0, 0), "foot_l": (0, 0, 0),
           "thigh_r": (0, 0, 0), "shin_r": (90, 0, 0), "foot_r": (-60, 0, 0), "chest": (10, 0, 0), "neck": (12, 0, 0)}
E_SIT_LEGS = {"@root": (0, -12.6, 0), "thigh_l": (-90, 0, 3), "thigh_r": (-90, 0, -3), "shin_l": (0, 0, 0), "shin_r": (0, 0, 0),
              "arm_l": (8, 0, 10), "arm_r": (8, 0, -10), "fore_l": (-20, 0, 0), "fore_r": (-20, 0, 0)}
E_LIE_BACK = dict(E_SIT_LEGS, **{"torso": (-86, 0, 0), "chest": (-4, 0, 0), "head": (6, 0, 0), "arm_l": (-6, 0, 10), "arm_r": (-6, 0, -10)})


def epose(P, pose, k=1.0):
    for b, v in pose.items():
        if b.startswith("@"):
            P.p(b[1:], v[0] * k, v[1] * k, v[2] * k)
        else:
            P.r(b, v[0] * k, v[1] * k, v[2] * k)


def eblend(P, a, b, k):
    """ease from pose a to pose b"""
    k = sm(k)
    keys = set(a) | set(b)
    for key in keys:
        va, vb = a.get(key, (0, 0, 0)), b.get(key, (0, 0, 0))
        v = [va[i] + (vb[i] - va[i]) * k for i in range(3)]
        if key.startswith("@"):
            P.p(key[1:], *v)
        else:
            P.r(key, *v)


def eseq(P, t, keys):
    """keys: [(time, pose)], eased between neighbours"""
    for (t0, a), (t1, b) in zip(keys, keys[1:]):
        if t <= t1:
            eblend(P, a, b, (t - t0) / (t1 - t0) if t1 > t0 else 1)
            return
    eblend(P, keys[-1][1], keys[-1][1], 1)


def e_life(P, t, eyes=1.0, breath=1.0, seed=0):
    b = math.sin(t * math.tau * 0.28)
    P.r("chest", -1.3 * b * breath).r("torso", 0.5 * b * breath)
    blink = math.exp(-(((t * 0.27 + seed) % 1.0 - 0.5) * 34) ** 2)
    shut = max(0.0, min(1.0, 1 - eyes + blink))
    P.r("lid_l", 70 * shut).r("lid_r", 70 * shut)


def e_talk(P, t, amt=1.0):
    P.r("jaw", amt * 6 * max(0.0, math.sin(t * 13) * math.sin(t * 3.1 + 1)))


@anim("elias", "stand", 6.0)
def e_stand(t):
    P = Pose()
    epose(P, E_STAND)
    e_life(P, t)
    P.r("head", noise(t, 1, 0.2) * 3, noise(t, 2, 0.15) * 8, 0)
    P.r("hips", 0, 0, noise(t, 3, 0.2) * 1.2)
    return P


@anim("elias", "talk", 4.0)
def e_talk_a(t):
    P = e_stand(t)
    e_talk(P, t)
    P.r("arm_r", -20 - 10 * max(0, math.sin(t * 1.7)), 0, -10).r("fore_r", -45)
    return P


@anim("elias", "lie", 6.0)
def e_lie(t):
    P = Pose()
    epose(P, E_LYING)
    e_life(P, t, eyes=0.0, breath=0.6)
    return P


@anim("elias", "wake", 16.0, loop=False, hold=True)
def e_wake(t):
    """face down on the logs; a finger moves; he coughs, pushes himself up, kneels back on his heels, looks at his hands
    - they are red - holds his head, gets up, staggers, and looks round at the sea"""
    P = Pose()
    stand_look = dict(E_STAND, **{"head": (-6, 0, 0)})
    eseq(P, t, [(0, E_LYING), (3.2, E_LYING), (4.6, E_PUSH), (5.8, E_FOURS), (7.2, E_KNEELSIT), (8.0, E_HANDS), (10.0, E_HANDS),
                (10.8, E_HOLDHEAD), (12.0, E_HOLDHEAD), (13.4, E_KNEEL), (14.6, stand_look), (16, stand_look)])
    eyes = ramp(t, 3.0, 3.6)
    e_life(P, t, eyes=eyes, breath=1.4)
    # a finger twitches, then the hand clenches
    tw = max(0.0, twitch(t, 1, 0.8, 30)) * (1 - ramp(t, 3, 3.2))
    P.r("hand_r", 30 * tw)
    # the cough as he comes up
    cough = math.exp(-((t - 4.0) * 5) ** 2) + math.exp(-((t - 4.5) * 5) ** 2)
    P.r("chest", 8 * cough).r("jaw", 14 * cough)
    # he sways getting up
    P.r("hips", 0, 0, 4 * math.sin(t * 2.3) * ramp(t, 13.4, 14) * (1 - ramp(t, 15.3, 16)))
    P.r("head", 0, 25 * math.sin((t - 14.4) * 1.4) * ramp(t, 14.4, 14.8), 0)
    return P


@anim("elias", "look_around", 8.0)
def e_look_around(t):
    P = Pose()
    epose(P, E_STAND)
    e_life(P, t)
    P.r("head", -4 + 6 * math.sin(t * 0.5), 50 * math.sin(t / 8 * math.tau), 0).r("chest", 0, 14 * math.sin(t / 8 * math.tau), 0)
    return P


@anim("elias", "walk", 1.2)
def e_walk(t):
    P = Pose()
    epose(P, E_STAND)
    e_life(P, t)
    a = t / 1.2 * math.tau
    for side, sg in SIDES:
        s = math.sin(a + (0 if sg > 0 else math.pi))
        up = max(0.0, math.cos(a + (0 if sg > 0 else math.pi)))
        P.r(f"thigh_{side}", -24 * s).r(f"shin_{side}", 30 * up).r(f"foot_{side}", -8 * s)
        P.r(f"arm_{side}", 18 * s)
    P.p("root", 0, -0.4 * abs(math.cos(a)), 0)
    P.r("hips", 0, 6 * math.sin(a), 0).r("chest", 2, -8 * math.sin(a), 0)
    return P


@anim("elias", "stagger", 1.8)
def e_stagger(t):
    P = e_walk(t * 1.2 / 1.8)
    P.r("hips", 0, 0, 5 * math.sin(t / 1.8 * math.tau * 0.5)).r("chest", 10, 0, -6 * math.sin(t / 1.8 * math.tau * 0.5))
    P.r("head", 12, 0, 8 * math.sin(t * 1.2))
    return P


@anim("elias", "kneel_reach", 6.0)
def e_kneel_reach(t):
    """down on one knee, a hand held out low and slow, the way you hold a hand out to a frightened cat"""
    P = Pose()
    epose(P, E_KNEEL)
    e_life(P, t)
    P.r("arm_r", -60 + 4 * math.sin(t * 0.8), 0, -6).r("fore_r", -10).r("hand_r", 20, 0, 0)
    P.r("arm_l", -10, 0, 8).r("fore_l", -60)
    P.r("neck", 14).r("head", 12, 8 * math.sin(t * 0.4), 0)
    return P


@anim("elias", "pet", 2.4)
def e_pet(t):
    """kneeling, stroking the cat from its head down its back"""
    P = Pose()
    epose(P, E_KNEEL)
    e_life(P, t, eyes=0.8)
    k = (t / 2.4) % 1.0
    stroke = math.sin(k * math.pi)
    P.r("arm_r", -55 + 20 * k, 0, -4).r("fore_r", -30 + 25 * stroke).r("hand_r", 30 - 20 * stroke)
    P.r("neck", 18).r("head", 18, 0, 6)
    return P


@anim("elias", "hold_head", 5.0)
def e_hold_head(t):
    P = Pose()
    epose(P, E_STAND)
    e_life(P, t, eyes=0.5)
    P.r("arm_l", -150, 0, 30).r("fore_l", -85).r("arm_r", -150, 0, -30).r("fore_r", -85)
    P.r("neck", 20).r("head", 16, 10 * math.sin(t * 0.9), 0).r("chest", 10)
    return P


@anim("elias", "look_up", 6.0)
def e_look_up(t):
    P = Pose()
    epose(P, E_STAND)
    e_life(P, t, eyes=1.0)
    P.r("neck", -20).r("head", -30, 4 * math.sin(t * 0.5), 0).r("chest", -6)
    P.r("arm_r", -20, 0, -10).r("fore_r", -40)
    return P


@anim("elias", "recoil", 1.6, loop=False, hold=True)
def e_recoil(t):
    """a step back, arms up, the breath caught"""
    P = Pose()
    epose(P, E_STAND)
    k = ramp(t, 0, 0.35)
    e_life(P, t, eyes=1.0)
    P.p("root", 0, 0, 6 * k)
    P.r("thigh_r", 20 * k).r("shin_r", 20 * k).r("chest", -12 * k).r("head", -10 * k)
    P.r("arm_l", -60 * k, 0, 20 * k).r("fore_l", -70 * k).r("arm_r", -60 * k, 0, -20 * k).r("fore_r", -70 * k)
    return P


@anim("elias", "sit_row", 6.0)
def e_sit_row(t):
    """sitting at the stern with the oar, the arms working it from side to side"""
    P = Pose()
    epose(P, E_SIT_LEGS)
    e_life(P, t)
    a = t / 2.0 * math.tau
    P.r("chest", 6 + 4 * math.sin(a), 14 * math.sin(a), 0)
    P.r("arm_l", -60, 20 * math.sin(a), 10).r("fore_l", -40).r("arm_r", -60, 20 * math.sin(a), -10).r("fore_r", -40)
    return P


@anim("elias", "sit", 6.0)
def e_sit(t):
    P = Pose()
    epose(P, E_SIT_LEGS)
    e_life(P, t)
    P.r("chest", 12).r("neck", 10).r("arm_l", -30, 0, 10).r("arm_r", -30, 0, -10).r("fore_l", -50).r("fore_r", -50)
    P.r("head", 6, noise(t, 1, 0.2) * 10, 0)
    return P


@anim("elias", "sob", 3.0)
def e_sob(t):
    """on his knees at the edge, bent double, shoulders shaking"""
    P = Pose()
    epose(P, E_KNEELSIT)
    e_life(P, t, eyes=0.1)
    sh = math.sin(t * 9) * (0.5 + 0.5 * math.sin(t * 1.3)) ** 2
    P.r("torso", 25).r("chest", 25 + 4 * sh).r("neck", 30).r("head", 15 + 3 * sh)
    P.r("arm_l", -40, 0, 20).r("fore_l", -110).r("arm_r", -40, 0, -20).r("fore_r", -110)
    P.r("jaw", 10 + 6 * sh)
    return P


@anim("elias", "reach_scream", 2.0)
def e_reach_scream(t):
    """thrown down at the edge, an arm stretched out over the blood toward what is gone"""
    P = Pose()
    epose(P, E_KNEEL)
    e_life(P, t, eyes=1.0)
    j = math.sin(t * 25) * 0.5
    P.r("chest", 30 + j).r("torso", 18)
    P.r("arm_r", -110 + 6 * math.sin(t * 2), 0, -6).r("fore_r", -5).r("hand_r", -20)
    P.r("arm_l", -30, 0, 20).r("fore_l", -30)
    P.r("head", -20).r("jaw", 26 + 4 * j)
    return P


@anim("elias", "collapse", 2.4, loop=False, hold=True)
def e_collapse(t):
    """the knees go, then the rest of him, onto his side"""
    P = Pose()
    k1 = ramp(t, 0, 0.6)
    k2 = ramp(t, 0.6, 1.4)
    eblend(P, E_STAND, E_KNEELSIT, k1)
    P.r("root", 0, 0, -85 * sm(k2)).p("root", 0, 3 * k2, 0)
    e_life(P, t, eyes=1 - ramp(t, 0.3, 1.0))
    P.r("head", 0, 0, 20 * k2).r("arm_r", -20 * k2, 0, -40 * k2)
    return P


@anim("elias", "lie_back", 6.0)
def e_lie_back(t):
    P = Pose()
    epose(P, E_LIE_BACK)
    e_life(P, t, eyes=0.0, breath=0.5)
    return P


@anim("elias", "sit_up_bed", 6.0, loop=False, hold=True)
def e_sit_up_bed(t):
    """eyes open on a white ceiling; he pushes himself up to sit, slowly"""
    P = Pose()
    eseq(P, t, [(0, E_LIE_BACK), (2.0, E_LIE_BACK), (2.6, dict(E_LIE_BACK, **{"arm_l": (40, 0, 20), "arm_r": (40, 0, -20)})),
                (5.0, E_SIT_LEGS), (6, E_SIT_LEGS)])
    e_life(P, t, eyes=ramp(t, 0.8, 1.8))
    P.r("head", 0, 30 * math.sin((t - 4.5) * 1.2) * ramp(t, 4.5, 5.0), 0)
    return P


@anim("elias", "bed_talk", 4.0)
def e_bed_talk(t):
    """sitting up in bed, a hand on the cat in his lap, talking"""
    P = Pose()
    epose(P, E_SIT_LEGS)
    e_life(P, t)
    e_talk(P, t)
    P.r("chest", 8).r("arm_r", -40, 0, -6).r("fore_r", -50 + 10 * math.sin(t * 1.5)).r("hand_r", 20)
    P.r("arm_l", -20, 0, 20).r("fore_l", -60)
    P.r("head", 10 + 4 * math.sin(t * 0.7), 10 * math.sin(t * 0.5), 0)
    return P


@anim("elias", "cradle", 4.0)
def e_cradle(t):
    """sitting, holding something in his arms against his chest, rocking"""
    P = Pose()
    epose(P, E_SIT_LEGS)
    e_life(P, t, eyes=0.4)
    r = math.sin(t / 4.0 * math.tau)
    P.r("chest", 14 + 4 * r).r("neck", 18).r("head", 16)
    P.r("arm_l", -50, -30, 10).r("fore_l", -90, 0, 0).r("arm_r", -50, 30, -10).r("fore_r", -90, 0, 0)
    e_talk(P, t, 0.4)
    return P


@anim("elias", "sit_throne", 6.0)
def e_sit_throne(t):
    P = Pose()
    epose(P, {"@root": (0, -6.8, 2.0), "thigh_l": (-88, 0, 6), "thigh_r": (-88, 0, -6), "shin_l": (88, 0, 0), "shin_r": (88, 0, 0),
              "arm_l": (-40, 0, 14), "arm_r": (-40, 0, -14), "fore_l": (-40, 0, 0), "fore_r": (-40, 0, 0), "chest": (-4, 0, 0)})
    e_life(P, t, eyes=0.6, breath=0.6)
    P.r("head", 8, 0, 6)
    return P


@anim("elias", "fall", 1.2)
def e_fall(t):
    """falling: arms and legs thrown about, turning slowly"""
    P = Pose()
    a = t / 1.2 * math.tau
    e_life(P, t, eyes=1.0)
    P.r("root", -30 + 10 * math.sin(a * 0.5), 0, 15 * math.sin(a * 0.5))
    for side, sg in SIDES:
        P.r(f"arm_{side}", -120 + 30 * math.sin(a + sg), 0, sg * (50 + 20 * math.sin(a * 2)))
        P.r(f"thigh_{side}", -20 + 25 * math.sin(a * 1.5 + sg)).r(f"shin_{side}", 30 + 20 * math.sin(a * 2))
    P.r("head", -20).r("jaw", 20)
    return P


@anim("elias", "jump", 1.6, loop=False, hold=True)
def e_jump(t):
    """a crouch at the edge, then off"""
    P = Pose()
    c = ramp(t, 0, 0.5) * (1 - ramp(t, 0.6, 0.8))
    air = ramp(t, 0.6, 1.0)
    epose(P, E_STAND)
    P.p("root", 0, -3 * c + 6 * air - 30 * ramp(t, 1.0, 1.6), -18 * air)
    for side, sg in SIDES:
        P.r(f"thigh_{side}", -50 * c + 20 * air).r(f"shin_{side}", 80 * c).r(f"arm_{side}", 30 * c - 150 * air, 0, sg * 20 * air)
    P.r("chest", 20 * c - 10 * air)
    e_life(P, t, eyes=1 - ramp(t, 0.4, 0.6))
    return P


@anim("elias", "float", 6.0)
def e_float(t):
    """on his back in grey water, arms out"""
    P = Pose()
    epose(P, {"root": (-90, 0, 0), "@root": (0, 1.5, 0), "arm_l": (0, 0, 70), "arm_r": (0, 0, -70), "head": (10, 0, 0)})
    e_life(P, t, eyes=0.2, breath=0.6)
    P.r("root", 2 * math.sin(t), 0, 3 * math.sin(t * 0.7))
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
    # the fisherman moves like Elias
    d = json.load(open(os.path.join(A, "elias.animation.json")))
    d["animations"] = {k.replace("animation.elias.", "animation.rescuer."): v for k, v in d["animations"].items()}
    json.dump(d, open(os.path.join(A, "rescuer.animation.json"), "w"), separators=(",", ":"))
