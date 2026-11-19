#!/usr/bin/env python3
"""The Mimic: animations. Every animation is a python function of time returning a pose; it is sampled every 0.05s.

Sign conventions (GeckoLib/Blockbench, front = -z): rotation x < 0 swings a hanging limb forward, x > 0 leans the
torso forward, jaw x > 0 opens the mouth, z * side spreads a limb outward (side = +1 left, -1 right).
"""
import json, math, os, zlib, sys


def shash(v):
    return zlib.crc32(str(v).encode())


HERE = os.path.dirname(os.path.abspath(__file__))
OUT = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "out")
DT = 0.05
SIDES = (("l", 1), ("r", -1))
TAGS = ("1", "2", "3")


def sm(x):  # smoothstep 0..1
    x = max(0.0, min(1.0, x))
    return x * x * (3 - 2 * x)


def ramp(t, a, b):
    return sm((t - a) / (b - a)) if b > a else float(t >= a)


def lerp(a, b, f):
    return a + (b - a) * f


def noise(t, seed, speed=1.0):
    """smooth pseudo-random wobble in -1..1"""
    return (math.sin(t * 1.7 * speed + seed * 3.1) * 0.5 + math.sin(t * 2.9 * speed + seed * 1.3) * 0.3
            + math.sin(t * 5.3 * speed + seed * 7.7) * 0.2)


def twitch(t, seed, rate=0.9, sharp=18):
    """occasional violent jerks: mostly 0, short spikes"""
    ph = (t * rate + seed * 0.37) % 1.0
    return math.exp(-((ph - 0.5) * sharp) ** 2) * (1 if int(t * rate + seed) % 2 else -1)


class Pose(dict):
    scales = None

    def s(self, bone, x=1, y=1, z=1):
        if self.scales is None:
            self.scales = {}
        self.scales[bone] = [x, y, z]
        return self

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


# ---------------------------------------------------------------- reusable body parts
def fingers(P, tag, side, curl, spread=0.0, seed=0, t=0, jitter=0.0):
    n = 3 if tag == "3" else 4
    for i in range(n):
        j = noise(t, seed + i * 5, 3) * jitter
        c = curl + j
        P.r(f"f{i}a{tag}_{side}", -c * 0.7, 0, (i - (n - 1) / 2) * spread * (1 if side == "l" else -1))
        P.r(f"f{i}b{tag}_{side}", -c * 1.0)
        P.r(f"f{i}c{tag}_{side}", -c * 0.9)
    P.r(f"thumb{tag}_{side}", -curl * 0.5, 0, 0)


def mouth(P, a, tongue=0.0):
    """a: 0 closed .. 1 unhinged - rotates down AND dislocates forward/down like the jaw is coming out of its sockets"""
    a = max(-0.1, a)
    P.r("jaw", 38 * min(a, 1) + 10 * max(0, a - 1))
    if a > 0.35:
        k = (a - 0.35) / 0.65
        P.p("jaw", 0, -3.2 * k, -1.6 * k)
    if tongue:
        P.r("tongue", -10 * tongue).p("tongue", 0, 0, -1.2 * tongue)
        P.r("tongue2", 30 * tongue)
        P.s("tongue2", 1, 1, 1 + 4 * tongue)


def legs_stand(P, bend=1.0):
    for side, sg in SIDES:
        P.r(f"thigh_{side}", -24 * bend, 0, sg * 4)
        P.r(f"shin_{side}", 52 * bend)
        P.r(f"foot_{side}", -28 * bend)
    P.p("all", 0, -2.2 * bend, 0)


def leg(P, side, sg, ph, stride=1.0, lift=1.0, bend=1.0):
    """digitigrade walk cycle for one leg, ph in 0..1"""
    s = math.sin(ph * math.tau)
    up = max(0.0, math.sin(ph * math.tau + math.pi / 2)) if False else max(0.0, -math.cos(ph * math.tau))
    P.r(f"thigh_{side}", -24 * bend - s * 26 * stride - up * 14 * lift, 0, sg * 4)
    P.r(f"shin_{side}", 52 * bend + up * 34 * lift)
    P.r(f"foot_{side}", -28 * bend + s * 14 * stride - up * 18 * lift)
    for k in range(3):
        P.r(f"toe{k}_{side}", up * 25 * lift - (1 - up) * 4)


def arm_hang(P, tag, side, sg, t, out=8, bend=18, curl=22, sway=1.0, seed=0):
    big = {"1": 1.0, "2": 0.85, "3": 0.6}[tag]
    P.r(f"arm{tag}_{side}", -6 + noise(t, seed, 0.6) * 3 * sway, 0, sg * (out + noise(t, seed + 9, 0.5) * 2 * sway))
    P.r(f"fore{tag}_{side}", -bend - noise(t, seed + 3, 0.7) * 4 * sway, 0, -sg * out * 0.35)
    P.r(f"hand{tag}_{side}", -8, sg * 6, 0)
    fingers(P, tag, side, curl * big, 2, seed, t, 4 * sway)


def breathe(P, t, rate=0.55, amt=1.0):
    b = math.sin(t * math.tau * rate)
    P.r("chest", -1.5 * b * amt).r("spine", 1.0 * b * amt).p("chest", 0, 0.15 * b * amt, 0)
    return b


# ---------------------------------------------------------------- animations
ANIMS = {}
HOLD = {"death"}  # one-shot animations that freeze on their last frame; the rest hand back to the loop


def anim(name, length, loop=True):
    def deco(fn):
        ANIMS[name] = (fn, length, loop)
        return fn
    return deco


@anim("idle", 6.0)
def idle(t):
    P = Pose()
    legs_stand(P)
    b = breathe(P, t, 1 / 3)
    P.r("hips", 4).r("spine", 8).r("chest", 10).r("neck", 14 + noise(t, 2, 0.4) * 3)
    P.r("head", -22 + noise(t, 4, 0.5) * 4, noise(t, 5, 0.35) * 14, noise(t, 6, 0.3) * 8 + twitch(t, 1, 1 / 3) * 14)
    mouth(P, (6 + 3 * (b + 1) + twitch(t, 3, 1 / 3, 30) * 6) / 38)
    for side, sg in SIDES:
        for tag in TAGS:
            arm_hang(P, tag, side, sg, t, out={"1": 10, "2": 16, "3": 26}[tag], bend={"1": 16, "2": 30, "3": 60}[tag],
                     seed=shash(tag + side) % 50)
    # the small arms fidget with each other, like nervous hands
    for side, sg in SIDES:
        P.r(f"fore3_{side}", -20 * (0.5 + 0.5 * math.sin(t * 2.1 + sg)), 0, -sg * 25)
    return P


@anim("walk", 1.6)
def walk(t):
    P = Pose()
    ph = t / 1.6
    legs_stand(P, 1.0)
    for side, sg in SIDES:
        leg(P, side, sg, ph + (0 if side == "l" else 0.5), 1.0, 1.0)
    bob = math.cos(ph * math.tau * 2)
    P.p("all", 0, -1.0 * bob, 0)
    P.r("hips", 8, math.sin(ph * math.tau) * 6, math.sin(ph * math.tau) * 3)
    P.r("spine", 10, -math.sin(ph * math.tau) * 4).r("chest", 12, -math.sin(ph * math.tau) * 5)
    P.r("neck", 10).r("head", -16 + bob * 2, math.sin(ph * math.tau) * 6, math.sin(ph * math.tau * 0.5 + 1) * 5)
    mouth(P, (8 + math.sin(t * 3) * 3) / 38)
    for side, sg in SIDES:
        s = math.sin(ph * math.tau + (math.pi if side == "l" else 0))
        P.r(f"arm1_{side}", -6 + s * 18, 0, sg * 12).r(f"fore1_{side}", -24 - max(0, s) * 18, 0, -sg * 4)
        P.r(f"hand1_{side}", -6 - s * 8)
        fingers(P, "1", side, 24, 3, 0, t, 3)
        P.r(f"arm2_{side}", -4 - s * 14, 0, sg * 18).r(f"fore2_{side}", -30, 0, -sg * 6)
        fingers(P, "2", side, 26, 3, 3, t, 3)
        P.r(f"arm3_{side}", -20, 0, sg * 25).r(f"fore3_{side}", -70, 0, -sg * 20)
        fingers(P, "3", side, 35)
    return P


@anim("stalk", 2.4)
def stalk(t):
    """low, slow, deliberate: hunched over, the long front hands touching the floor between steps"""
    P = Pose()
    ph = t / 2.4
    legs_stand(P, 1.5)
    for side, sg in SIDES:
        leg(P, side, sg, ph + (0 if side == "l" else 0.5), 0.6, 0.6, 1.5)
    P.p("all", 0, -4 - math.cos(ph * math.tau * 2) * 0.6, 0)
    P.r("hips", 22).r("spine", 18).r("chest", 20, math.sin(ph * math.tau) * 5)
    P.r("neck", -10).r("head", -36 + noise(t, 1, 0.4) * 4, noise(t, 2, 0.5) * 18, noise(t, 3, 0.4) * 10)
    mouth(P, (4 + 3 * math.sin(t * 2)) / 38)
    for side, sg in SIDES:
        s = math.sin(ph * math.tau + (0 if side == "l" else math.pi))
        reach = max(0.0, s)
        P.r(f"arm1_{side}", -48 - s * 22, 0, sg * 14).r(f"fore1_{side}", -6 + reach * 10, 0, 0)
        P.r(f"hand1_{side}", 30 - reach * 30)
        fingers(P, "1", side, 10 + (1 - reach) * 25, 6, 0, t, 5)
        P.r(f"arm2_{side}", -34 + s * 16, 0, sg * 24).r(f"fore2_{side}", -30, 0, 0)
        fingers(P, "2", side, 20, 6, 1, t, 4)
        P.r(f"arm3_{side}", -30, 0, sg * 20).r(f"fore3_{side}", -60)
        fingers(P, "3", side, 40)
    return P


@anim("run", 0.8)
def run(t):
    """it drops onto its six hands and gallops: legs push, arms claw forward, head stays level and locked on"""
    P = Pose()
    ph = t / 0.8
    P.p("all", 0, -9 - 1.6 * math.cos(ph * math.tau * 2), -2)
    P.r("hips", 52 + 6 * math.sin(ph * math.tau)).r("spine", 14 - 8 * math.sin(ph * math.tau)).r("chest", 10)
    P.r("neck", -38).r("head", -40 + 4 * math.sin(ph * math.tau * 2), 0, twitch(t, 2, 1.25) * 10)
    mouth(P, (22 + 8 * math.sin(ph * math.tau * 2)) / 38)
    for side, sg in SIDES:
        o = 0 if side == "l" else 0.5
        lp = (ph + o) % 1
        s = math.sin(lp * math.tau)
        up = max(0.0, -math.cos(lp * math.tau))
        P.r(f"thigh_{side}", -40 - s * 40 - up * 20, 0, sg * 8).r(f"shin_{side}", 70 + up * 40).r(f"foot_{side}", -20 + s * 20)
        for tag, off, amp in (("1", 0.25, 1.0), ("2", 0.6, 0.8), ("3", 0.9, 0.5)):
            ap = (ph + o + off) % 1
            a = math.sin(ap * math.tau)
            lift = max(0.0, -math.cos(ap * math.tau))
            P.r(f"arm{tag}_{side}", (-60 - a * 38) * amp, 0, sg * (16 + lift * 10))
            P.r(f"fore{tag}_{side}", -10 - lift * 50 * amp, 0, 0)
            P.r(f"hand{tag}_{side}", 40 - lift * 40)
            fingers(P, tag, side, 10 + lift * 50, 8)
    return P


@anim("roar", 3.0, loop=False)
def roar(t):
    """the pose from the picture: rising up, all six arms flung open, jaw unhinging to its full depth"""
    P = Pose()
    up = ramp(t, 0.0, 0.7)
    hold = ramp(t, 2.4, 3.0)
    k = up * (1 - hold)
    shake = math.sin(t * 60) * 1.2 * ramp(t, 0.7, 0.9) * (1 - hold)
    legs_stand(P, lerp(1.0, 1.3, k))
    P.r("hips", lerp(4, -6, k)).r("spine", lerp(8, -10, k)).r("chest", lerp(10, -8, k) + shake)
    P.r("neck", lerp(14, 30, k)).r("head", lerp(-22, -28, k) + shake * 2, shake * 2, shake)
    mouth(P, (lerp(6, 62, ramp(t, 0.5, 1.0) * (1 - hold)) + shake * 3) / 38, 0.6 * ramp(t, 0.8, 1.4) * (1 - hold))
    for side, sg in SIDES:
        for tag, raise_, outw, fb in (("1", -150, 70, -40), ("2", -60, 85, -20), ("3", -20, 55, -40)):
            P.r(f"arm{tag}_{side}", lerp(-6, raise_ * 0.4, k), 0, sg * lerp(10, outw, k))
            P.r(f"fore{tag}_{side}", lerp(-20, fb, k), 0, sg * lerp(0, 25, k))
            P.r(f"hand{tag}_{side}", lerp(-8, 20, k), 0, sg * lerp(0, 15, k))
            fingers(P, tag, side, lerp(24, -12, k), lerp(2, 14, k), shash(tag) % 9, t, 8 * k)
    return P


@anim("scream", 2.2, loop=False)
def scream(t):
    """a recoil and a scream straight at the camera; jaw shaking, head jerking"""
    P = Pose()
    a = ramp(t, 0, 0.25)
    rel = ramp(t, 1.8, 2.2)
    k = a * (1 - rel)
    j = math.sin(t * 70) * k
    legs_stand(P, 1.2)
    P.r("hips", 10 * k).r("spine", 12 * k).r("chest", 18 * k + j)
    P.r("neck", 30 * k).r("head", -40 * k + j * 4, twitch(t, 1, 2.3, 10) * 12 * k, j * 3)
    mouth(P, (6 + 70 * k + j * 5) / 38)
    for side, sg in SIDES:
        for tag in TAGS:
            P.r(f"arm{tag}_{side}", -40 * k, 0, sg * (14 + 30 * k)).r(f"fore{tag}_{side}", -50 * k)
            fingers(P, tag, side, -10 * k + 20 * (1 - k), 12 * k, 0, t, 10 * k)
    return P


@anim("mimic_talk", 4.0)
def mimic_talk(t):
    """speaking in someone else's voice: the body perfectly still, only the jaw moving, a little too wide, a little off-beat"""
    P = idle(t * 0.3)
    syll = abs(math.sin(t * 7.3)) * (0.6 + 0.4 * math.sin(t * 1.9)) * (1 if (t % 1.3) < 1.05 else 0.15)
    mouth(P, (syll * 26 - 4) / 38)
    P.r("head", 2 * syll, 0, 18 + noise(t, 3, 0.3) * 4)  # head tilted like a listening dog
    P.r("neck", 6)
    return P


@anim("listen", 3.0)
def listen(t):
    """freezes, head jerks toward a sound in little steps, very still between"""
    P = Pose()
    legs_stand(P, 1.2)
    step = math.floor(t * 2.5)
    targ = [0, 35, 35, 70, 70, 50, 50, 20][int(step) % 8]
    P.r("hips", 10).r("spine", 10).r("chest", 12)
    P.r("neck", 20, targ * 0.3).r("head", -26, targ * 0.6, 26 + twitch(t, 4, 2.5, 22) * 4)
    mouth(P, (3) / 38)
    for side, sg in SIDES:
        for tag in TAGS:
            arm_hang(P, tag, side, sg, t, out=14, bend=24, sway=0.2)
    return P


@anim("sniff", 2.0)
def sniff(t):
    P = Pose()
    legs_stand(P, 1.5)
    P.p("all", 0, -3, 0)
    s = abs(math.sin(t * math.tau * 2.5)) * ramp(t, 0, 0.2)
    P.r("hips", 26).r("spine", 18).r("chest", 20).r("neck", 10 - s * 6).r("head", -20 + s * 8, math.sin(t * 2) * 25)
    mouth(P, (2 + s * 4) / 38)
    for side, sg in SIDES:
        P.r(f"arm1_{side}", -50, 0, sg * 12).r(f"fore1_{side}", -10)
        P.r(f"hand1_{side}", 30)
        fingers(P, "1", side, 14, 6, 0, t, 6)
        arm_hang(P, "2", side, sg, t, out=22, bend=30)
        arm_hang(P, "3", side, sg, t, out=30, bend=70)
    return P


@anim("search", 4.0)
def search(t):
    """long fingers feeling through the dark: hands sweep left and right, head following one beat late"""
    P = Pose()
    legs_stand(P, 1.3)
    sw = math.sin(t / 4.0 * math.tau)
    P.r("hips", 14, sw * 12).r("spine", 12, sw * 10).r("chest", 12, sw * 10)
    P.r("neck", 6).r("head", -20, math.sin(t / 4.0 * math.tau - 0.8) * 30, noise(t, 1, 0.5) * 8)
    mouth(P, (5 + noise(t, 5) * 3) / 38)
    for side, sg in SIDES:
        P.r(f"arm1_{side}", -70 + sw * sg * 12, 0, sg * (20 - sw * sg * 10)).r(f"fore1_{side}", -30)
        P.r(f"hand1_{side}", 10, 0, sg * 10)
        fingers(P, "1", side, 10 + 10 * math.sin(t * 3 + sg), 10, 0, t, 10)
        P.r(f"arm2_{side}", -40, 0, sg * 30).r(f"fore2_{side}", -40)
        fingers(P, "2", side, 20, 8, 1, t, 8)
        arm_hang(P, "3", side, sg, t, out=26, bend=60)
    return P


@anim("grab", 1.4, loop=False)
def grab(t):
    """lunge: the big hands shoot forward and close"""
    P = Pose()
    wind = ramp(t, 0, 0.3)
    hit = ramp(t, 0.3, 0.45)
    back = ramp(t, 0.9, 1.4)
    k = hit * (1 - back)
    legs_stand(P, 1.2)
    P.p("all", 0, 0, -4 * k)
    P.r("hips", 10 + 10 * k).r("spine", 8 + 10 * k - 8 * wind * (1 - hit)).r("chest", 10 + 14 * k - 10 * wind * (1 - hit))
    P.r("neck", 10 + 10 * k).r("head", -20 - 10 * k)
    mouth(P, (10 + 40 * k) / 38)
    for side, sg in SIDES:
        P.r(f"arm1_{side}", lerp(lerp(-6, -120, wind), -95, hit) * (1 - back) + -6 * back, 0, sg * lerp(40 * wind, 6, hit))
        P.r(f"fore1_{side}", lerp(-60 * wind, -5, hit) * (1 - back), 0, -sg * 10 * k)
        fingers(P, "1", side, lerp(lerp(20, -15, wind), 70, ramp(t, 0.42, 0.55)) * (1 - back) + 20 * back, 8)
        P.r(f"arm2_{side}", -70 * k, 0, sg * (16 + 20 * wind * (1 - hit))).r(f"fore2_{side}", -30 * k)
        fingers(P, "2", side, 40 * k + 10, 6)
        arm_hang(P, "3", side, sg, t, out=24, bend=60)
    return P


@anim("kill", 4.0, loop=False)
def kill(t):
    """the player is held up in the big hands: the head cocks, the jaw opens past breaking, it lunges down twice and tears"""
    P = Pose()
    legs_stand(P, 1.1)
    lift = ramp(t, 0, 0.5)
    b1 = ramp(t, 1.4, 1.6) * (1 - ramp(t, 1.9, 2.3))
    b2 = ramp(t, 2.5, 2.7) * (1 - ramp(t, 3.2, 3.8))
    tear = ramp(t, 3.0, 3.3) * (1 - ramp(t, 3.6, 4.0))
    bite = max(b1, b2)
    shake = math.sin(t * 45) * bite * 3
    P.r("hips", 6).r("spine", 4 + 10 * bite).r("chest", 4 + 14 * bite + shake)
    P.r("neck", 10 + 30 * bite).r("head", -10 - 40 * bite + shake * 2, shake * 3, 20 * (1 - bite) * lift)
    mouth(P, (8 + 60 * ramp(t, 0.8, 1.3) * (1 - bite * 0.7) + 20 * ramp(t, 1.2, 1.4) * (1 - ramp(t, 3.6, 4))) / 38, ramp(t, 0.9, 1.3) * (1 - bite) * (1 - ramp(t, 3.4, 4)))
    for side, sg in SIDES:
        # hands hold the victim up at head height in front
        P.r(f"arm1_{side}", -110 * lift + 15 * bite, 0, sg * (12 + 30 * tear)).r(f"fore1_{side}", -40 * lift + 20 * bite, 0, -sg * 20 * lift)
        P.r(f"hand1_{side}", 10 * lift, 0, -sg * 20 * lift)
        fingers(P, "1", side, 60, 4, 0, t, 6 * bite)
        P.r(f"arm2_{side}", -80 * lift, 0, sg * (10 + 40 * tear)).r(f"fore2_{side}", -60 * lift + 30 * tear)
        fingers(P, "2", side, 55 - 50 * tear, 4)
        P.r(f"arm3_{side}", -40, 0, sg * 20).r(f"fore3_{side}", -80 + 20 * math.sin(t * 9))
        fingers(P, "3", side, 30 + 30 * math.sin(t * 9 + sg), 4)
    return P


@anim("eat", 3.0)
def eat(t):
    """crouched over something on the floor, tearing pieces and feeding them into the jaw"""
    P = Pose()
    legs_stand(P, 1.9)
    P.p("all", 0, -6, 0)
    c = (t / 3.0 * 2) % 1
    pull = ramp(c, 0.1, 0.35) * (1 - ramp(c, 0.55, 0.8))
    P.r("hips", 40).r("spine", 22).r("chest", 18).r("neck", 8 + 8 * pull).r("head", -30 + 20 * pull, 0, math.sin(t * 13) * 3 * pull)
    mouth(P, (10 + 30 * pull * abs(math.sin(t * 9))) / 38)
    for side, sg in SIDES:
        f = pull if side == "l" else ramp((c + 0.5) % 1, 0.1, 0.35) * (1 - ramp((c + 0.5) % 1, 0.55, 0.8))
        P.r(f"arm1_{side}", lerp(-30, -80, f), 0, sg * 12).r(f"fore1_{side}", lerp(0, -80, f), 0, -sg * 15 * f)
        P.r(f"hand1_{side}", 30 * (1 - f))
        fingers(P, "1", side, lerp(30, 70, f), 3, 0, t, 6)
        P.r(f"arm2_{side}", -40, 0, sg * 20).r(f"fore2_{side}", -20)
        P.r(f"hand2_{side}", 40)
        fingers(P, "2", side, 50 + 15 * math.sin(t * 7 + sg), 3)
        arm_hang(P, "3", side, sg, t, out=30, bend=80)
    return P


@anim("crawl", 1.2)
def crawl(t):
    """belly flat to the floor, slithering under tables and through vents"""
    P = Pose()
    ph = t / 1.2
    P.p("all", 0, -18, 6)
    P.r("all", 0, 0, 0)
    P.r("hips", 82).r("spine", 4 + 4 * math.sin(ph * math.tau), 8 * math.sin(ph * math.tau)).r("chest", 2, -8 * math.sin(ph * math.tau))
    P.r("neck", -50).r("head", -32, 10 * math.sin(ph * math.tau), 0)
    mouth(P, (6) / 38)
    for side, sg in SIDES:
        o = 0 if side == "l" else 0.5
        for tag, off in (("1", 0), ("2", 0.33), ("3", 0.66)):
            a = math.sin((ph + o + off) * math.tau)
            lift = max(0.0, -math.cos((ph + o + off) * math.tau))
            P.r(f"arm{tag}_{side}", -80 - a * 30, 0, sg * (40 + lift * 10)).r(f"fore{tag}_{side}", -10 - lift * 40, 0, 0)
            P.r(f"hand{tag}_{side}", 50 - lift * 30)
            fingers(P, tag, side, 15 + 40 * lift, 10)
        s = math.sin((ph + o) * math.tau)
        P.r(f"thigh_{side}", -10 + s * 20, 0, sg * (30 + 10 * s)).r(f"shin_{side}", 70 - s * 30).r(f"foot_{side}", 20)
    return P


@anim("climb_wall", 1.6)
def climb_wall(t):
    """spread across a stairwell or wall like in the picture: limbs reaching out to the sides, body rising in jerks"""
    P = Pose()
    ph = t / 1.6
    P.r("hips", -6).r("spine", -4).r("chest", -6).r("neck", 14).r("head", -18, 0, twitch(t, 1, 1.25, 14) * 18)
    mouth(P, (20 + 8 * math.sin(t * 5)) / 38)
    P.p("all", 0, 2 * math.sin(ph * math.tau * 2), 0)
    for side, sg in SIDES:
        o = 0 if side == "l" else 0.5
        for tag, base, off in (("1", -150, 0), ("2", -90, 0.33), ("3", -40, 0.66)):
            a = math.sin((ph + o + off) * math.tau)
            P.r(f"arm{tag}_{side}", -20 + a * 15, 0, sg * (-base * 0.55 + 30 + a * 15))
            P.r(f"fore{tag}_{side}", -10, 0, sg * (20 - a * 15))
            P.r(f"hand{tag}_{side}", 0, 0, sg * 20)
            fingers(P, tag, side, 40 + 20 * a, 14)
        s = math.sin((ph + o) * math.tau)
        P.r(f"thigh_{side}", -40 + s * 25, 0, sg * (45 + s * 10)).r(f"shin_{side}", 80 - s * 20).r(f"foot_{side}", -30)
    return P


@anim("emerge", 3.0, loop=False)
def emerge(t):
    """unfolding from the floor / out of a hole: arms first, hands slapping down, then the body hauling itself up"""
    P = Pose()
    k = ramp(t, 0.6, 2.4)
    P.p("all", 0, lerp(-46, 0, k), lerp(4, 0, k))
    P.r("hips", lerp(60, 4, k)).r("spine", lerp(30, 8, k)).r("chest", lerp(20, 10, k))
    P.r("neck", lerp(-40, 14, k)).r("head", lerp(-10, -22, k), twitch(t, 1, 2, 10) * 20 * (1 - k), twitch(t, 2, 1.7, 12) * 25)
    mouth(P, (20 + 30 * ramp(t, 1.8, 2.2) * (1 - ramp(t, 2.6, 3))) / 38)
    legs_stand(P, lerp(2.2, 1.0, k))
    for side, sg in SIDES:
        reach = ramp(t, 0 if side == "l" else 0.2, 0.5)
        slap = ramp(t, 0.5, 0.65)
        P.r(f"arm1_{side}", lerp(-170 * reach, -60, slap) * (1 - k) + -6 * k, 0, sg * lerp(30, 10, k))
        P.r(f"fore1_{side}", lerp(-20, 0, slap) * (1 - k) - 16 * k)
        P.r(f"hand1_{side}", 60 * slap * (1 - k))
        fingers(P, "1", side, lerp(-10, 30, slap), 12, 0, t, 8)
        P.r(f"arm2_{side}", -90 * (1 - k) - 6 * k, 0, sg * 30).r(f"fore2_{side}", -30)
        fingers(P, "2", side, 30, 8)
        arm_hang(P, "3", side, sg, t, out=26, bend=60)
    return P


@anim("bang", 1.0)
def bang(t):
    """pounding a door with the big hands, alternating"""
    P = Pose()
    legs_stand(P, 1.2)
    P.r("hips", 8).r("spine", 10).r("chest", 10).r("neck", 10).r("head", -16, 0, math.sin(t * 6.28) * 6)
    mouth(P, (30) / 38)
    for side, sg in SIDES:
        c = (t + (0 if side == "l" else 0.5)) % 1
        strike = ramp(c, 0.0, 0.12) * (1 - ramp(c, 0.2, 0.7))
        P.r(f"arm1_{side}", lerp(-150, -95, strike), 0, sg * 16).r(f"fore1_{side}", lerp(-50, -5, strike))
        P.r(f"hand1_{side}", 20 * strike)
        fingers(P, "1", side, 70)
        arm_hang(P, "2", side, sg, t, out=30, bend=40)
        arm_hang(P, "3", side, sg, t, out=30, bend=60)
    return P


@anim("twitch", 2.5)
def twitch_anim(t):
    """standing still, but wrong: violent jerks of the head and limbs"""
    P = idle(t)
    P.r("head", twitch(t, 1, 1.6, 30) * 40, twitch(t, 2, 1.2, 30) * 50, twitch(t, 3, 2.0, 30) * 60)
    P.r("chest", twitch(t, 4, 0.8, 30) * 12, 0, twitch(t, 5, 0.8, 30) * 10)
    for side, sg in SIDES:
        P.r(f"arm2_{side}", twitch(t, 6 + sg, 1.1, 25) * 50, 0, sg * twitch(t, 8 + sg, 1.3, 25) * 30)
        P.r(f"fore1_{side}", twitch(t, 10 + sg, 0.9, 25) * 30)
    mouth(P, (abs(twitch(t, 11, 1.4, 20)) * 40) / 38)
    return P


@anim("death", 3.0, loop=False)
def death(t):
    P = Pose()
    k = ramp(t, 0, 1.6)
    legs_stand(P, lerp(1, 2.4, k))
    P.p("all", 0, lerp(0, 2.0, ramp(t, 0.8, 2.0)), lerp(0, 6, ramp(t, 1.0, 2.0)))
    P.r("all", lerp(0, -80, ramp(t, 1.0, 2.0)), 0, 0)
    P.r("chest", math.sin(t * 40) * 8 * (1 - k)).r("head", -20 + 40 * k, 0, 30 * k)
    mouth(P, (60 * k) / 38)
    for side, sg in SIDES:
        for tag in TAGS:
            P.r(f"arm{tag}_{side}", lerp(-6, -160, k) * (0.6 if tag != "1" else 1), 0, sg * lerp(10, 50, k))
            P.r(f"fore{tag}_{side}", -20 - 40 * k)
            fingers(P, tag, side, lerp(20, 80, k), 6, 0, t, 15 * (1 - k))
    return P


@anim("spread", 4.0)
def spread(t):
    """the photo pose held as a loop: rising tall, all six arms open, the jaw hanging"""
    P = roar(1.2 + 0.0 * t)
    b = math.sin(t / 4.0 * math.tau)
    P.r("chest", b * 2).r("head", 0, noise(t, 1, 0.4) * 10, twitch(t, 2, 0.5, 20) * 14)
    mouth(P, (-14 + 6 * b) / 38)
    for side, sg in SIDES:
        for tag in TAGS:
            P.r(f"arm{tag}_{side}", noise(t, shash(tag + side) % 20, 0.6) * 4)
            fingers(P, tag, side, 6 * math.sin(t * 1.5 + int(tag)), 0, 0, t, 4)
    return P


def build_json():
    out = {"format_version": "1.8.0", "animations": {}}
    for name, (fn, length, loop) in ANIMS.items():
        frames = int(round(length / DT))
        bones = {}
        for i in range(frames + 1):
            t = i * DT
            P = fn(t if i < frames or not loop else 0.0)  # loops end exactly where they start
            for bone, sc in (P.scales or {}).items():
                bones.setdefault(bone, {}).setdefault("scale", {})[f"{t:.2f}"] = [round(v, 3) for v in sc]
            for bone, (r, p) in P.items():
                b = bones.setdefault(bone, {})
                if r is not None:
                    b.setdefault("rotation", {})[f"{t:.2f}"] = [round(v, 2) for v in r]
                if p is not None:
                    b.setdefault("position", {})[f"{t:.2f}"] = [round(v, 3) for v in p]
        # drop channels that never change
        for bone, chans in bones.items():
            for c in list(chans):
                vals = list(chans[c].values())
                if all(v == vals[0] for v in vals):
                    chans[c] = vals[0]
        a = {"animation_length": length, "bones": bones}
        if loop:
            a["loop"] = True
        elif name in HOLD:
            a["loop"] = "hold_on_last_frame"
        out["animations"][f"animation.mimic.{name}"] = a
    return out


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    j = build_json()
    with open(os.path.join(OUT, "mimic.animation.json"), "w") as f:
        json.dump(j, f, separators=(",", ":"))
    print(len(j["animations"]), "animations")
