"""Every clip of the atomic kaiju (locomotion, reactions, attacks) and the attack catalogue the server plays.

Attack entry (exported to data/kaiju/kaiju/atomic_kaiju.json):
  id, clip (or phases for multi-clip attacks), cat, tags, rng [min, max] model px from body centre to target centre,
  arc (front/back/left/right/any), hits [[t0, t1, [nodes], kind, power]], events [[t, type, {args}]],
  move [[t0, t1, side px/s, forward px/s]], cd (cooldown s), need [conditions], beam {...} for breath.
Hit kinds: blunt, cut (can sever), bite (can sever, can hold), stomp, crush. power scales the effective mass.
"""
import math
from animlib import (Clip, merge, tail_curve, neck, arm, leg, jaw, body, root, torso, crouch, stance_wide,
                     scale_pose, TAILS, NECK, SIDES)

CLIPS = {}
ATTACKS = []

TAIL_TIP = [f"tail{i}" for i in range(9, 17)]
TAIL_MID = [f"tail{i}" for i in range(5, 13)]
HANDS = {"l": ["hand_l", "elbow_l"], "r": ["hand_r", "elbow_r"]}
FEET = {"l": ["foot_l", "shin_l"], "r": ["foot_r", "shin_r"]}
BODY = ["chest", "belly", "hips", "shoulder_l", "shoulder_r"]
HEAD = ["head", "snout", "jaw"]


def clip(name, length, loop=False):
    c = Clip(name, length, loop)
    CLIPS[name] = c
    return c


def attack(id_, cat, clip_name=None, length=None, rng=(0, 60), arc="front", hits=(), events=(), move=(), cd=2.0,
           need=(), tags=(), phases=None, beam=None, weight=1.0):
    ATTACKS.append({
        "id": id_, "clip": clip_name or id_, "cat": cat, "len": round(length or (CLIPS[clip_name or id_].length if (clip_name or id_) in CLIPS else 0), 3),
        "rng": list(rng), "arc": arc, "hits": [list(h) for h in hits], "events": [list(e) for e in events],
        "move": [list(m) for m in move], "cd": cd, "need": list(need), "tags": list(tags), "phases": phases, "beam": beam,
        "weight": weight})


# ====================================================================== locomotion
def locomotion():
    # ---------------------------------------------------------------- idle: heavy breathing, weight shifts, a slow look around
    c = clip("idle", 6.0, True)
    w = 2 * math.pi / 6.0
    br = lambda t: math.sin(w * 1.5 * t)  # breath cycle: 4 s
    c.add_fn("torso", lambda t: [10 + 1.2 * br(t), 1.5 * math.sin(w * t), 0])
    c.add_fn("body", lambda t: [0, 2 * math.sin(w * t), 1.5 * math.sin(w * t)])
    c.add_fn("body", lambda t: [1.2 * math.sin(w * t), -0.4 + 0.3 * br(t), 0], kind="position")
    c.add_fn("neck1", lambda t: [3 + 1.0 * br(t - 0.3), 4 * math.sin(w * t + 0.8), 0])
    c.add_fn("neck2", lambda t: [3, 3 * math.sin(w * t + 1.0), 0])
    c.add_fn("head", lambda t: [2 - 1.5 * br(t - 0.4), 8 * math.sin(w * t + 1.3), 0])
    c.add_fn("jaw", lambda t: [2 + 2 * max(0, br(t - 0.2)), 0, 0])
    c.add_fn("throat", lambda t: [2 * br(t), 0, 0])
    for side, sg in SIDES:
        c.add_fn(f"arm_{side}", lambda t, sg=sg: [-18 - 2 * br(t), 0, sg * (6 + 1.5 * br(t))])
        c.add_fn(f"forearm_{side}", lambda t: [-30, 0, 0])
        c.add_fn(f"thigh_{side}", lambda t, sg=sg: [-4, 0, sg * 3 + 1.2 * math.sin(w * t)])
        c.add_fn(f"shin_{side}", lambda t: [6, 0, 0])
        c.add_fn(f"foot_{side}", lambda t: [-2, 0, 0])
        c.add_fn(f"eyelid_{side}", lambda t: [0, -0.9 if 3.6 <= t <= 3.75 else 0, 0], kind="position")
    for i, b in enumerate(TAILS):
        c.add_fn(b, lambda t, i=i: [0.5 * math.sin(w * t - i * 0.3), 2.2 * math.sin(w * t - i * 0.32), 0])
    c.extra[("scale", "chest")] = {f"{t:.1f}": [round(1 + 0.022 * math.sin(w * 1.5 * t), 3), round(1 + 0.03 * math.sin(w * 1.5 * t), 3), round(1 + 0.03 * math.sin(w * 1.5 * t), 3)] for t in [k * 0.2 for k in range(31)]}
    c.extra[("scale", "belly")] = {f"{t:.1f}": [round(1 + 0.015 * math.sin(w * 1.5 * t - 0.5), 3), 1, round(1 + 0.02 * math.sin(w * 1.5 * t - 0.5), 3)] for t in [k * 0.2 for k in range(31)]}
    c.springs = False

    # ---------------------------------------------------------------- tired: shoulders slumped, panting, head low
    c = clip("tired", 2.0, True)
    w2 = 2 * math.pi / 2.0
    c.add_fn("torso", lambda t: [18 + 3 * math.sin(w2 * t), 0, 0])
    c.add_fn("body", lambda t: [0, -2.2 + 0.6 * math.sin(w2 * t), 0], kind="position")
    c.add_fn("neck1", lambda t: [10 + 2 * math.sin(w2 * t - 0.4), 0, 0])
    c.add_fn("head", lambda t: [6 - 4 * math.sin(w2 * t - 0.6), 0, 0])
    c.add_fn("jaw", lambda t: [16 + 10 * math.sin(w2 * t - 0.3), 0, 0])
    c.add_fn("throat", lambda t: [6 * math.sin(w2 * t), 0, 0])
    for side, sg in SIDES:
        c.add_fn(f"arm_{side}", lambda t, sg=sg: [-6, 0, sg * 4])
        c.add_fn(f"forearm_{side}", lambda t: [-14, 0, 0])
        c.add_fn(f"thigh_{side}", lambda t, sg=sg: [-12, 0, sg * 5])
        c.add_fn(f"shin_{side}", lambda t: [16, 0, 0])
        c.add_fn(f"foot_{side}", lambda t: [-4, 0, 0])
    for i, b in enumerate(TAILS):
        c.add_fn(b, lambda t, i=i: [-0.6 + 0.4 * math.sin(w2 * t - i * 0.3), 0.8 * math.sin(w2 * 0.5 * t - i * 0.3), 0])
    c.extra[("scale", "chest")] = {f"{t:.1f}": [round(1 + 0.05 * math.sin(w2 * t), 3), round(1 + 0.06 * math.sin(w2 * t), 3), round(1 + 0.06 * math.sin(w2 * t), 3)] for t in [k * 0.1 for k in range(21)]}
    c.springs = False

    def gait(name, L, stride, knee, bob, sway, yaw, lean, neckp, arms, tail_lift=0.0, back=False):
        """heavy biped gait. Left leg phase th = w t (right = th + pi); a foot strikes when its thigh is furthest
        forward (t = 0.75 L left, 0.25 L right), stays flat on the ground through stance and lifts with a bent knee
        while it swings. The body is lowest at each strike and rides over the stance leg; hips and shoulders twist
        against each other, the head is held steady, the tail swings against the hips and lags (spring physics)."""
        c = clip(name, L, True)
        w = 2 * math.pi / L
        s = -1 if back else 1

        def leg_angles(th):
            hip = s * stride * math.sin(th)
            swing = max(0.0, -math.cos(th))
            kn = knee * swing ** 0.8 + 5 * max(0.0, math.cos(th))
            stance = max(0.0, math.cos(th))
            ank = -(hip + kn) * (0.95 if stance > 0 else 0.55) + 8 * swing * math.sin(th * 2)
            return hip, kn, ank

        for side, ph in (("l", 0.0), ("r", math.pi)):
            c.add_fn(f"thigh_{side}", lambda t, ph=ph: [leg_angles(w * t + ph)[0], 0, 0])
            c.add_fn(f"shin_{side}", lambda t, ph=ph: [leg_angles(w * t + ph)[1], 0, 0])
            c.add_fn(f"foot_{side}", lambda t, ph=ph: [leg_angles(w * t + ph)[2], 0, 0])
            sgn = 1 if side == "l" else -1
            c.add_fn(f"arm_{side}", lambda t, sgn=sgn: [-arms[0] - 5 * sgn * math.sin(w * t) * s, 0, sgn * arms[2]])
            c.add_fn(f"forearm_{side}", lambda t: [-arms[1], 0, 0])
        c.add_fn("body", lambda t: [0, yaw * math.sin(w * t) * s, -sway * 1.1 * math.cos(w * t)])
        c.add_fn("body", lambda t: [sway * math.cos(w * t), bob * math.cos(2 * w * t), 0], kind="position")
        c.add_fn("torso", lambda t: [lean + 1.5 * math.cos(2 * w * t), -1.3 * yaw * math.sin(w * t) * s, sway * 0.6 * math.cos(w * t)])
        c.add_fn("neck1", lambda t: [neckp - 1.5 * math.cos(2 * w * t), 0.6 * yaw * math.sin(w * t) * s, 0])
        c.add_fn("neck2", lambda t: [neckp * 0.5, 0.3 * yaw * math.sin(w * t) * s, 0])
        c.add_fn("head", lambda t: [-lean * 0.6 - neckp * 1.2 + 1.0 * math.cos(2 * w * t), 0.2 * yaw * math.sin(w * t) * s, -0.5 * sway * math.cos(w * t)])
        for i, b in enumerate(TAILS):
            c.add_fn(b, lambda t, i=i: [tail_lift / 16 + 0.8 * math.cos(2 * w * t - i * 0.35), -1.6 * yaw / 4 * math.sin(w * t - i * 0.28) * s, 0])
        c.impact(L * 0.25, 0.45).impact(L * 0.75, 0.45)
        c.springs = True
        return c

    #     name          L    stride knee bob  sway yaw lean neck  arms(raise, elbow, out) tail
    gait("walk",       2.8, 17,    34,  1.6, 2.0, 6,  12,  5,    (14, 32, 7))
    gait("walk_fast",  1.3, 30,    64,  3.4, 1.1, 4,  28,  13,   (32, 58, 4), tail_lift=12)
    gait("walk_back",  3.2, 11,    26,  1.2, 1.6, 4,  6,   2,    (18, 30, 8), back=True)
    gait("turn",       2.0, 7,     22,  0.9, 1.6, 3,  10,  4,    (14, 32, 7))

    for name, L in (("hold_idle", 3.0), ("hold_walk", 2.8)):
        c = clip(name, L, True)
        w = 2 * math.pi / L
        for side, sg in SIDES:
            ph = 0 if sg > 0 else math.pi
            base = arm(side, 95, 18, elbow=40, fingers=40)
            c.add_fn(f"arm_{side}", lambda t, b=base[f"arm_{side}"]: [b[0] + 2 * math.sin(w * t), b[1], b[2]])
            c.add_fn(f"forearm_{side}", lambda t, b=base[f"forearm_{side}"]: b)
            if name == "hold_walk":
                c.add_fn(f"thigh_{side}", lambda t, ph=ph: [14 * math.sin(w * t + ph), 0, 0])
                c.add_fn(f"shin_{side}", lambda t, ph=ph: [28 * max(0, -math.cos(w * t + ph)) ** 0.8, 0, 0])
                c.add_fn(f"foot_{side}", lambda t, ph=ph: [-14 * math.sin(w * t + ph) * 0.9, 0, 0])
        c.add_fn("torso", lambda t: [-6, 0, 0])
        c.add_fn("head", lambda t: [6, 0, 0])
        for i, b in enumerate(TAILS):
            c.add_fn(b, lambda t, i=i: [0.8 * math.sin(2 * w * t - i * 0.3), -3 * math.sin(w * t - i * 0.32), 0])
        c.springs = False


# ====================================================================== lying poses, reactions
LIE_BACK = merge(root(-86, y=17), {"tail1": [-62, 0, 0], "tail2": [-22, 0, 0], "tail3": [-4, 0, 0]},
                 neck(18, head_pitch=26), arm("l", 40, 25, elbow=30), arm("r", 40, 25, elbow=30),
                 leg("l", -40, 30), leg("r", -30, 20))
LIE_FRONT = merge(root(84, y=15), {"tail1": [-80, 0, 0], "tail2": [-6, 0, 0]}, neck(-30, head_pitch=-26),
                  arm("l", 20, 30, elbow=60), arm("r", 20, 30, elbow=60), leg("l", 20, 10), leg("r", 30, 0))


def reactions():
    dirs = {"f": (1, 0), "b": (-1, 0), "l": (0, 1), "r": (0, -1)}  # hit FROM the front pushes back, etc.
    for d, (fz, fx) in dirs.items():
        # flinch: 0.6 s jolt
        c = clip(f"flinch_{d}", 0.6)
        hit = merge(torso(-8 * fz, 10 * fx, -6 * fx), neck(-12 * fz, 14 * fx), jaw(14))
        c.key(0, {}).key(0.08, hit, "out").key(0.6, {}, "smooth").impact(0.05, 0.6)
        # stagger: rocked, two heavy steps to keep balance
        c = clip(f"stagger_{d}", 1.5)
        rock = merge(body(-10 * fz, 16 * fx, 10 * fx, z=6 * fz, x=-5 * fx), torso(-12 * fz, 18 * fx, -8 * fx),
                     neck(-18 * fz, 20 * fx), jaw(22), arm("l", 30, 30, elbow=20), arm("r", 30, 30, elbow=20))
        step1 = merge(rock, leg("l", -16 * fz, 24, 0), body(-6 * fz, 10 * fx, 6 * fx, z=9 * fz, x=-7 * fx))
        step2 = merge(scale_pose(rock, 0.5), leg("r", -14 * fz, 20, 0))
        c.key(0, {}).key(0.12, rock, "out").key(0.5, step1).key(0.9, step2).key(1.5, {}).impact(0.06, 1.0)
    # knockdowns: the blow rocks it, it staggers trying to stay up, loses balance and topples like a felled tower:
    # the fall starts slowly and accelerates (gravity), the body pivots on its heels and comes down along an arc
    # (keyed densely so the lift follows sin(angle) and nothing floats), slams, settles with a tiny rebound
    def topple(name, end, lean_pitch, lean_roll, react_pose, step_pose, reach_pose):
        c = clip(name, 3.4)
        c.key(0, {}).key(0.2, react_pose, "out").key(0.75, step_pose).key(1.15, merge(reach_pose, root(lean_pitch * 0.12, 0, lean_roll * 0.12)))
        er = end[("pos", "root")][1] if ("pos", "root") in end else 0
        endp, endr = end["root"][0], end["root"][2]
        t0, t1 = 1.15, 2.25
        n = 8
        for k in range(1, n + 1):
            f = k / n
            g = f ** 2.2  # angle grows like a falling rigid body
            ang_p = lean_pitch * 0.12 + (endp - lean_pitch * 0.12) * g
            ang_r = lean_roll * 0.12 + (endr - lean_roll * 0.12) * g
            tilt = max(abs(ang_p), abs(ang_r))
            lift = er * math.sin(math.radians(min(90, tilt))) ** 1.3
            pose = merge(scale_pose(reach_pose, 1 - g), scale_pose({kk: v for kk, v in end.items() if kk not in ("root", ("pos", "root"))}, g),
                         {"root": [ang_p, 0, ang_r], ("pos", "root"): [0, lift, 0]}, jaw(30 * (1 - g) + 20 * g))
            c.key(t0 + (t1 - t0) * f, pose, "linear")
        rebound = merge(end, root(-1.5 if endp < 0 else 1.5, y=0.8))
        c.key(2.42, rebound, "out").key(2.6, end, "in").key(3.4, end)
        c.impact(2.25, 1.0).impact(2.6, 0.35)
        return c

    flail = lambda a, b: merge(arm("l", a, 40, elbow=30, fingers=40), arm("r", b, 40, elbow=30, fingers=40))
    topple("knockdown_b", LIE_BACK, -86, 0,
           merge(torso(-14), neck(-22, head_pitch=-14), jaw(36), flail(40, 50), body(z=4)),
           merge(torso(-12), neck(-16), jaw(30), flail(70, 60), leg("r", 20, 30), leg("l", -8, 20), body(-4, z=6)),
           merge(torso(-10), neck(10, head_pitch=14), jaw(34), flail(100, 90), leg("l", -30, 20), leg("r", -10, 10)))
    topple("knockdown_f", LIE_FRONT, 84, 0,
           merge(torso(16), neck(14), jaw(30), flail(30, 30), body(z=-4)),
           merge(torso(20), neck(10), jaw(26), flail(50, 40), leg("l", -24, 34), body(4, z=-6)),
           merge(torso(22), neck(-14, head_pitch=-12), jaw(32), arm("l", 100, 20, elbow=10, fingers=-20), arm("r", 100, 20, elbow=10, fingers=-20), leg("r", 20, 10)))
    topple("knockdown_l", merge(LIE_BACK, root(0, 0, -6)), -86, -6,
           merge(torso(-6, -14, 8), neck(-10, -18), jaw(34), flail(50, 30), body(x=4)),
           merge(torso(-8, -16, 12), neck(-6, -20), jaw(30), flail(60, 70), leg("r", 10, 30, 0, 14), body(roll=8, x=6)),
           merge(torso(-10, -10, 10), neck(10, -14), jaw(34), flail(90, 100), leg("l", -20, 20)))
    topple("knockdown_r", merge(LIE_BACK, root(0, 0, 6)), -86, 6,
           merge(torso(-6, 14, -8), neck(-10, 18), jaw(34), flail(30, 50), body(x=-4)),
           merge(torso(-8, 16, -12), neck(-6, 20), jaw(30), flail(70, 60), leg("l", 10, 30, 0, 14), body(roll=-8, x=-6)),
           merge(torso(-10, 10, -10), neck(10, 14), jaw(34), flail(100, 90), leg("r", -20, 20)))
    c = clip("lying_back", 3.0, True)
    c.key(0, LIE_BACK).key(1.5, merge(LIE_BACK, torso(3), jaw(8))).key(3.0, LIE_BACK)
    c = clip("lying_front", 3.0, True)
    c.key(0, LIE_FRONT).key(1.5, merge(LIE_FRONT, torso(-3), jaw(8))).key(3.0, LIE_FRONT)
    # get up: from the back roll onto the front, push up on the arms, kneel, rise
    side = merge(root(-10, 0, 80, y=13), neck(10, 30), arm("r", 60, 60, elbow=40), leg("l", -50, 50), leg("r", -30, 40))
    kneel = merge(root(25, y=-6), body(y=-6), leg("l", -80, 90, 0), leg("r", 20, 100, -30), arm("l", 30, 10, elbow=60), arm("r", 30, 10, elbow=60), neck(-10))
    rise = merge(crouch(1.2), neck(-4), torso(10))
    c = clip("getup_back", 4.2)
    c.key(0, LIE_BACK).key(1.0, side).key(1.9, LIE_FRONT, "in").key(2.9, kneel).key(3.6, rise).key(4.2, {}).impact(1.9, 0.5)
    c = clip("getup_front", 3.4)
    push = merge(root(60, y=12), arm("l", 70, 20, elbow=10), arm("r", 70, 20, elbow=10), neck(-10))
    c.key(0, LIE_FRONT).key(1.1, push).key(2.1, kneel).key(2.8, rise).key(3.4, {})
    # held in a grapple: flailing; thrown: airborne tumbling
    c = clip("held", 1.2, True)
    c.key(0, merge(arm("l", 60, 40, elbow=30, fingers=40), arm("r", 70, 50, elbow=20, fingers=40), jaw(30), neck(-10, 15), tail_curve(0, 30, 20)))
    c.key(0.6, merge(arm("l", 80, 50, elbow=10), arm("r", 50, 30, elbow=40), jaw(10), neck(-6, -15), tail_curve(0, 25, -20), leg("l", -20, 30), leg("r", 10, 20)))
    c.key(1.2, merge(arm("l", 60, 40, elbow=30, fingers=40), arm("r", 70, 50, elbow=20, fingers=40), jaw(30), neck(-10, 15), tail_curve(0, 30, 20)))
    c = clip("thrown", 1.0, True)
    c.key(0, merge(root(-30, 0, 20), arm("l", 100, 70), arm("r", 100, 70), leg("l", -40, 40), neck(30), jaw(35), tail_curve(0, 40, 15)))
    c.key(0.5, merge(root(-45, 0, -20), arm("l", 80, 80), arm("r", 120, 60), leg("r", -50, 30), neck(35), jaw(25), tail_curve(0, 50, -15)))
    c.key(1.0, merge(root(-30, 0, 20), arm("l", 100, 70), arm("r", 100, 70), leg("l", -40, 40), neck(30), jaw(35), tail_curve(0, 40, 15)))
    c = clip("pinned", 1.6, True)
    c.key(0, merge(LIE_BACK, arm("l", 70, 30, elbow=40, fingers=50), jaw(25))).key(0.8, merge(LIE_BACK, arm("r", 80, 40, elbow=30, fingers=50), jaw(35), neck(10, 20))).key(1.6, merge(LIE_BACK, arm("l", 70, 30, elbow=40, fingers=50), jaw(25)))
    c = clip("beam_recoil", 1.4)
    c.key(0, {}).key(0.15, merge(body(-14, 0, 0, z=8), torso(-18), neck(-25, 0, 0, head_pitch=-20), jaw(40), arm("l", 60, 50), arm("r", 60, 50)), "out").key(1.4, {}).impact(0.1, 1.0)
    c = clip("burn_writhe", 1.6)
    c.key(0, {}).key(0.3, merge(torso(-10, 20), neck(-20, 30), jaw(45, 0.8), arm("l", 50, 40, fingers=60))).key(0.8, merge(torso(-6, -20), neck(-16, -30), jaw(40, 0.8), arm("r", 50, 40, fingers=60))).key(1.6, {})
    c = clip("death", 4.0)
    c.key(0, {}).key(0.6, merge(neck(-40, 0, 0, head_pitch=-30), jaw(45), torso(-14), arm("l", 60, 40), arm("r", 60, 40))) \
     .key(1.6, merge(crouch(1.4), neck(20), jaw(20))).key(2.6, merge(LIE_BACK, neck(30, 20), jaw(30)), "in").key(4.0, merge(LIE_BACK, neck(34, 26), jaw(34))).impact(2.6, 1.0)
    c = clip("roar", 4.6)
    inhale = merge(crouch(0.6), torso(16), neck(14, head_pitch=10), jaw(6), arm("l", 10, 6, elbow=40), arm("r", 10, 6, elbow=40), tail_curve(0, -6))
    peak = merge(torso(-14), neck(-46, head_pitch=-30), jaw(46, 1), {"throat": [16, 0, 0]}, arm("l", 46, 34, elbow=26, fingers=-24),
                 arm("r", 46, 34, elbow=26, fingers=-24), tail_curve(0, 34, 12), stance_wide(0.8))
    hold = merge(peak, neck(-4, head_pitch=-4), {"throat": [4, 0, 0]})
    c.key(0, {}).key(0.9, inhale).key(1.35, peak, "out").key(3.4, hold).key(3.9, merge(scale_pose(peak, 0.4), jaw(-18)), "in").key(4.6, {})
    c.add_fn("head", lambda t: [0.9 * math.sin(t * 70), 0.9 * math.sin(t * 53), 0] if 1.35 < t < 3.4 else [0, 0, 0])
    c.add_fn("jaw", lambda t: [2.5 * math.sin(t * 41), 0, 0] if 1.35 < t < 3.4 else [0, 0, 0])
    c.extra[("scale", "chest")] = {"0": [1, 1, 1], "0.9": [1.07, 1.06, 1.08], "1.35": [1.02, 1.01, 1.02], "3.4": [0.97, 0.98, 0.97], "4.6": [1, 1, 1]}

# ====================================================================== attacks
def swing(name, L, keys, impacts=()):
    c = clip(name, L)
    for k in keys:
        c.key(*k)
    for t, s in impacts:
        c.impact(t, s)
    return c


def claws():
    for side, sg in SIDES:
        o = "r" if side == "l" else "l"
        # horizontal swipe: wind up out to the side, rip across the front
        swing(f"claw_swipe_{side}", 1.4, [
            (0, {}), (0.4, merge(torso(4, 22 * sg, 0), arm(side, 70, 55, 20, elbow=30, fingers=-30), neck(6, 10 * sg), jaw(6))),
            (0.62, merge(torso(10, -30 * sg, 0), arm(side, 80, -10, -30, elbow=10, fingers=50), neck(10, -16 * sg), jaw(18), body(z=-2)), "in"),
            (0.9, merge(torso(8, -36 * sg, 0), arm(side, 50, -20, -30, elbow=30, fingers=60), neck(6, -18 * sg)), "out"), (1.4, {})])
        attack(f"claw_swipe_{side}", "claw", rng=(20, 62), hits=[[0.45, 0.72, HANDS[side], "cut", 1.0]], move=[[0.35, 0.7, 0, 24]], cd=1.2, need=[f"arm_{side}"])
        swing(f"claw_uppercut_{side}", 1.4, [
            (0, {}), (0.45, merge(crouch(0.8), torso(14, 10 * sg), arm(side, -10, 20, elbow=10, fingers=-20), neck(12))),
            (0.68, merge(torso(-10, -8 * sg), arm(side, 150, 10, elbow=20, fingers=50), neck(-14), body(y=2), jaw(16)), "in"),
            (0.95, merge(torso(-8, -10 * sg), arm(side, 130, 10, elbow=40, fingers=50), neck(-10)), "out"), (1.4, {})])
        attack(f"claw_uppercut_{side}", "claw", rng=(18, 55), hits=[[0.5, 0.75, HANDS[side], "cut", 1.1]], cd=1.6, need=[f"arm_{side}"], tags=["launch"])
        swing(f"claw_stab_{side}", 1.2, [
            (0, {}), (0.35, merge(torso(-4, 18 * sg), arm(side, 40, 10, elbow=80, fingers=-40), neck(4, 8 * sg))),
            (0.52, merge(torso(12, -14 * sg), arm(side, 95, 0, elbow=-5, fingers=-30), neck(12, -8 * sg), body(z=-3)), "in"),
            (0.8, merge(torso(10, -10 * sg), arm(side, 90, 0, elbow=0, fingers=40)), "out"), (1.2, {})])
        attack(f"claw_stab_{side}", "claw", rng=(20, 60), hits=[[0.38, 0.6, HANDS[side], "cut", 0.9]], move=[[0.3, 0.55, 0, 30]], cd=1.1, need=[f"arm_{side}"])
        swing(f"backhand_{side}", 1.3, [
            (0, {}), (0.4, merge(torso(4, -26 * sg), arm(side, 70, -15, -20, elbow=40), neck(4, -10 * sg))),
            (0.6, merge(torso(4, 30 * sg), arm(side, 75, 70, 10, elbow=10), neck(4, 16 * sg), jaw(10)), "in"),
            (0.9, merge(torso(2, 34 * sg), arm(side, 60, 75, 10, elbow=20)), "out"), (1.3, {})])
        attack(f"backhand_{side}", "claw", rng=(18, 60), arc="left" if side == "l" else "right", hits=[[0.42, 0.68, HANDS[side], "blunt", 1.0]], cd=1.2, need=[f"arm_{side}"])
    swing("claw_double", 1.6, [
        (0, {}), (0.5, merge(torso(-10), arm("l", 120, 40, elbow=30, fingers=-30), arm("r", 120, 40, elbow=30, fingers=-30), neck(-10), jaw(20, 0.6))),
        (0.75, merge(torso(16), arm("l", 60, -10, elbow=0, fingers=60), arm("r", 60, -10, elbow=0, fingers=60), neck(14), jaw(30), body(z=-3)), "in"),
        (1.05, merge(torso(14), arm("l", 30, -5, elbow=20, fingers=60), arm("r", 30, -5, elbow=20, fingers=60), neck(10)), "out"), (1.6, {})], [(0.75, 0.5)])
    attack("claw_double", "claw", rng=(20, 58), hits=[[0.55, 0.85, HANDS["l"] + HANDS["r"], "cut", 1.3]], move=[[0.45, 0.8, 0, 20]], cd=2.0, need=["arm_l", "arm_r"])
    swing("overhead_smash", 1.9, [
        (0, {}), (0.7, merge(torso(-16), arm("l", 160, 10, elbow=40, fingers=40), arm("r", 160, 10, elbow=40, fingers=40), neck(-14), jaw(24, 0.8), crouch(-0.3))),
        (0.98, merge(torso(26), arm("l", 40, 0, elbow=0, fingers=60), arm("r", 40, 0, elbow=0, fingers=60), neck(22), crouch(0.8), body(z=-4)), "in"),
        (1.3, merge(torso(24), arm("l", 20, 0, elbow=10), arm("r", 20, 0, elbow=10), neck(18), crouch(0.8)), "out"), (1.9, {})], [(0.98, 1.0)])
    attack("overhead_smash", "claw", rng=(15, 55), hits=[[0.78, 1.05, HANDS["l"] + HANDS["r"], "blunt", 1.8]], events=[[1.0, "shockwave", {"node": "hand_r", "radius": 18, "power": 0.6}]], cd=3.0, need=["arm_l", "arm_r"], tags=["heavy"])
    swing("claw_combo3", 2.4, [
        (0, {}), (0.35, merge(torso(4, 22), arm("l", 70, 55, 20, elbow=30, fingers=-30))),
        (0.55, merge(torso(10, -30), arm("l", 80, -10, -30, elbow=10, fingers=50), neck(8, -14)), "in"),
        (0.95, merge(torso(4, -24), arm("r", 70, 55, 20, elbow=30, fingers=-30), arm("l", 40, 0, elbow=30))),
        (1.15, merge(torso(10, 30), arm("r", 80, -10, -30, elbow=10, fingers=50), neck(8, 14)), "in"),
        (1.55, merge(torso(-8), arm("l", 140, 20, elbow=30, fingers=-20), crouch(0.6))),
        (1.75, merge(torso(20), arm("l", 50, 0, elbow=0, fingers=60), neck(16), body(z=-3)), "in"), (2.4, {})], [(1.75, 0.6)])
    attack("claw_combo3", "claw", rng=(20, 58), hits=[[0.4, 0.6, HANDS["l"], "cut", 0.9], [1.0, 1.2, HANDS["r"], "cut", 0.9], [1.6, 1.8, HANDS["l"], "blunt", 1.3]], move=[[0.3, 1.8, 0, 14]], cd=3.0, need=["arm_l", "arm_r"])
    swing("chest_slash", 1.5, [
        (0, {}), (0.5, merge(torso(-14, 18), arm("r", 150, 30, elbow=20, fingers=-20), neck(-8))),
        (0.72, merge(torso(20, -24), arm("r", 30, -30, -20, elbow=10, fingers=60), neck(14, -10), body(z=-3)), "in"),
        (1.0, merge(torso(18, -26), arm("r", 20, -30, -20, elbow=20, fingers=60)), "out"), (1.5, {})])
    attack("chest_slash", "claw", rng=(18, 55), hits=[[0.55, 0.78, HANDS["r"], "cut", 1.3]], cd=2.0, need=["arm_r"])


def tails():
    for side, sg in SIDES:
        # spin whip: body turns away (wind up), spins so the tail sweeps through that side, the tail lags then cracks
        spin = -sg  # body yaw sign that throws the tail toward `side`
        for name, height, L, deg, pw in ((f"tail_whip_{side}", 0, 2.0, 120, 1.3), (f"tail_whip_high_{side}", 1, 2.1, 115, 1.2),
                                          (f"tail_sweep_low_{side}", -1, 2.0, 110, 1.1)):
            lift = {1: 34, 0: 12, -1: -6}[height]
            c = swing(name, L, [
                (0, {}), (0.5, merge(root(0, -spin * 25), crouch(0.7), torso(6), neck(6, spin * 10), tail_curve(-spin * 40, lift * 0.6))),
                (1.0, merge(root(0, spin * deg), crouch(0.5), torso(-4, spin * 10), neck(0, -spin * 25), tail_curve(spin * 30, lift), body(roll=spin * 6)), "in"),
                (1.35, merge(root(0, spin * (deg + 15)), torso(-2, spin * 12), neck(0, -spin * 30), tail_curve(spin * 60, lift * 0.6)), "out"),
                (L, {}, "smooth")], [(1.0, 0.5)])
            c.tail_stiff = 0.85
            attack(name, "tail", rng=(40, 165), arc="left" if side == "l" else "right",
                   hits=[[0.75, 1.3, TAIL_TIP + TAIL_MID[:3], "blunt", pw]], cd=2.4, need=["tail"],
                   tags=(["trip"] if height < 0 else []) + (["heavy"] if height == 0 else []))
    c = swing("tail_spin_360", 2.6, [
        (0, {}), (0.5, merge(root(0, 30), crouch(0.9), tail_curve(-40, 10))),
        (1.5, merge(root(0, -330), crouch(0.6), tail_curve(50, 14), body(roll=-8)), "in"),
        (1.9, merge(root(0, -360), tail_curve(60, 6))), (2.6, merge(root(0, -360)))], [(1.5, 0.6)])
    c.tail_stiff = 0.8
    attack("tail_spin_360", "tail", rng=(40, 170), arc="any", hits=[[0.7, 1.7, TAIL_TIP + TAIL_MID, "blunt", 1.5]], cd=4.0, need=["tail"], tags=["heavy", "surround"])
    c = swing("tail_double_spin", 3.4, [
        (0, {}), (0.5, merge(root(0, 30), crouch(0.9), tail_curve(-40, 10))),
        (1.4, merge(root(0, -330), crouch(0.6), tail_curve(50, 12)), "in"),
        (2.3, merge(root(0, -690), crouch(0.6), tail_curve(60, 10))), (2.7, merge(root(0, -720), tail_curve(40, 4))), (3.4, root(0, -720))], [(1.4, 0.5), (2.3, 0.6)])
    c.tail_stiff = 0.8
    attack("tail_double_spin", "tail", rng=(40, 170), arc="any", hits=[[0.7, 2.4, TAIL_TIP + TAIL_MID, "blunt", 1.4]], cd=6.0, need=["tail"], tags=["surround"])
    c = swing("tail_overhead_slam", 2.6, [
        (0, {}), (0.8, merge(torso(16), crouch(0.8), neck(12), tail_curve(0, 150, 0, start=2))),
        (1.3, merge(torso(28), crouch(1.0), neck(20), tail_curve(0, 210, 0, start=1)), "smooth"),
        (1.6, merge(torso(4), crouch(0.6), neck(4), tail_curve(0, -20, 0)), "in"),
        (2.0, merge(crouch(0.4), tail_curve(0, -10)), "out"), (2.6, {})], [(1.6, 1.0)])
    attack("tail_overhead_slam", "tail", rng=(50, 150), arc="back", hits=[[1.3, 1.7, TAIL_TIP, "crush", 1.8]], events=[[1.6, "shockwave", {"node": "tail14", "radius": 30, "power": 1.0}]], cd=4.0, need=["tail"], tags=["heavy"])
    c = swing("tail_stab_back", 1.6, [
        (0, {}), (0.5, merge(torso(14), neck(10), tail_curve(0, 40, 0), crouch(0.5))),
        (0.75, merge(torso(-6), tail_curve(0, -8, 0), body(z=4)), "in"), (1.6, {})])
    attack("tail_stab_back", "tail", rng=(60, 175), arc="back", hits=[[0.55, 0.8, TAIL_TIP[-4:], "cut", 1.0]], cd=1.8, need=["tail"])
    c = swing("tail_ground_slam_back", 2.2, [
        (0, {}), (0.7, merge(torso(20), crouch(0.7), tail_curve(0, 90, 0))),
        (1.05, merge(torso(-4), crouch(0.9), tail_curve(0, -30, 0)), "in"), (1.4, merge(crouch(0.5), tail_curve(0, -20, 0))), (2.2, {})], [(1.05, 0.8)])
    attack("tail_ground_slam_back", "tail", rng=(40, 170), arc="back", hits=[[0.85, 1.1, TAIL_TIP + TAIL_MID, "crush", 1.4]], events=[[1.05, "shockwave", {"node": "tail12", "radius": 40, "power": 1.2}]], cd=3.5, need=["tail"], tags=["trip"])
    for side, sg in SIDES:
        c = swing(f"tail_jab_{side}", 1.2, [
            (0, {}), (0.35, merge(root(0, sg * 20), tail_curve(sg * 30, 10))),
            (0.6, merge(root(0, -sg * 55), tail_curve(-sg * 30, 10)), "in"), (1.2, {})])
        attack(f"tail_jab_{side}", "tail", rng=(40, 150), arc="right" if side == "l" else "left", hits=[[0.4, 0.65, TAIL_TIP, "blunt", 0.8]], cd=1.2, need=["tail"])


def head_attacks():
    swing("bite_lunge", 1.5, [
        (0, {}), (0.45, merge(torso(-6), neck(-14, 0, 0, -10), jaw(44, 1), crouch(0.5))),
        (0.68, merge(torso(22), neck(26, 0, 0, 10), jaw(6), body(z=-5), arm("l", 40, 10), arm("r", 40, 10)), "in"),
        (0.95, merge(torso(18), neck(20), jaw(4)), "out"), (1.5, {})], [(0.68, 0.4)])
    attack("bite_lunge", "bite", rng=(25, 70), hits=[[0.55, 0.75, HEAD, "bite", 1.2]], move=[[0.45, 0.75, 0, 50]], cd=1.6)
    c = swing("bite_neck", 3.4, [
        (0, {}), (0.4, merge(torso(-4), neck(-10), jaw(44, 1), arm("l", 70, 30, elbow=30, fingers=50), arm("r", 70, 30, elbow=30, fingers=50))),
        (0.65, merge(torso(20), neck(24, 10), jaw(10), body(z=-4), arm("l", 85, 10, elbow=30, fingers=60), arm("r", 85, 10, elbow=30, fingers=60)), "in"),
        (1.3, merge(torso(14, 18), neck(18, 30), jaw(12), arm("l", 85, 10, elbow=40, fingers=60), arm("r", 85, 10, elbow=40, fingers=60))),
        (1.9, merge(torso(14, -18), neck(18, -30), jaw(12), arm("l", 85, 10, elbow=40, fingers=60), arm("r", 85, 10, elbow=40, fingers=60))),
        (2.5, merge(torso(14, 18), neck(18, 30), jaw(12))), (2.75, merge(torso(-4, -30), neck(-10, -40), jaw(40)), "in"), (3.4, {})])
    attack("bite_neck", "grapple", rng=(25, 62), hits=[[0.5, 0.7, HEAD, "bite", 1.0]], events=[[0.66, "grab", {"node": "snout", "target": "neck"}], [1.3, "pulse_hit", {"power": 0.5}], [1.9, "pulse_hit", {"power": 0.5}], [2.5, "pulse_hit", {"power": 0.5}], [2.75, "release", {"fwd": -40, "side": -120, "up": 30}]], move=[[0.4, 0.7, 0, 30]], cd=6.0, tags=["grapple"])
    c = swing("bite_arm", 2.6, [
        (0, {}), (0.4, merge(neck(-6, 20), jaw(40, 1))), (0.62, merge(torso(12, -12), neck(22, -18), jaw(8), body(z=-3)), "in"),
        (1.2, merge(torso(8, 20), neck(10, 30), jaw(10))), (1.7, merge(torso(-10, -40), neck(-10, -50), jaw(14), crouch(0.6)), "in"), (2.6, {})])
    attack("bite_arm", "bite", rng=(22, 62), hits=[[0.5, 0.65, HEAD, "bite", 1.0]], events=[[0.62, "grab", {"node": "snout", "target": "arm"}], [1.7, "sever_pull", {}], [1.75, "release", {"fwd": 10, "side": -60, "up": 10}]], cd=8.0, tags=["sever", "grapple"])
    swing("headbutt", 1.3, [
        (0, {}), (0.45, merge(torso(-10), neck(-22, head_pitch=-16), crouch(0.4))),
        (0.62, merge(torso(16), neck(24, head_pitch=22), body(z=-5)), "in"), (0.85, merge(torso(14), neck(20)), "out"), (1.3, {})], [(0.62, 0.6)])
    attack("headbutt", "bite", rng=(20, 55), hits=[[0.5, 0.7, ["head", "snout"], "blunt", 1.3]], move=[[0.4, 0.65, 0, 40]], cd=1.6)
    swing("head_slam_down", 1.8, [
        (0, {}), (0.7, merge(torso(-14), neck(-30, head_pitch=-20), jaw(20))),
        (1.0, merge(torso(40), neck(50, head_pitch=30), crouch(1.0), jaw(10)), "in"), (1.3, merge(torso(36), neck(44), crouch(1.0)), "out"), (1.8, {})], [(1.0, 0.8)])
    attack("head_slam_down", "bite", rng=(15, 60), hits=[[0.85, 1.05, ["head", "snout", "jaw"], "crush", 1.5]], cd=2.5, need=["enemy_low"])
    swing("bite_throw", 3.0, [
        (0, {}), (0.4, merge(neck(-10), jaw(44, 1))), (0.62, merge(torso(18), neck(22), jaw(10), body(z=-4)), "in"),
        (1.4, merge(torso(-6, 40), neck(-10, 50), jaw(12), crouch(0.6))), (1.75, merge(torso(-10, -70), neck(-16, -60), jaw(40)), "in"), (3.0, {})])
    attack("bite_throw", "grapple", rng=(22, 60), hits=[[0.5, 0.65, HEAD, "bite", 0.9]], events=[[0.62, "grab", {"node": "snout", "target": "neck"}], [1.75, "release", {"fwd": 40, "side": -260, "up": 80}]], cd=7.0, tags=["grapple", "throw_enemy"])
    swing("snap_quick", 0.8, [(0, {}), (0.25, merge(neck(-6), jaw(36))), (0.38, merge(neck(14), jaw(2), torso(8)), "in"), (0.8, {})])
    attack("snap_quick", "bite", rng=(22, 58), hits=[[0.28, 0.42, HEAD, "bite", 0.7]], cd=0.8)


def body_attacks():
    swing("shoulder_charge", 2.6, [
        (0, {}), (0.5, merge(crouch(1.0), torso(16, 22), neck(14, 14), arm("l", 10, 10, elbow=60))),
        (1.0, merge(torso(22, 26), neck(18, 16), body(z=-4), leg("l", -30, 30), leg("r", 20, 30))),
        (1.5, merge(torso(22, 26), neck(18, 16), body(z=-4), leg("r", -30, 30), leg("l", 20, 30))),
        (1.8, merge(torso(12, -10), neck(8), body(z=-6)), "in"), (2.6, {})], [(1.8, 1.0)])
    attack("shoulder_charge", "body", rng=(50, 220), hits=[[1.0, 1.9, ["shoulder_l", "chest", "shoulder_r"], "blunt", 2.0]], move=[[0.5, 1.85, 0, 170]], cd=5.0, tags=["heavy", "gap_close"])
    swing("body_slam", 2.2, [
        (0, {}), (0.6, merge(torso(-12), neck(-10), crouch(0.8), arm("l", 60, 40), arm("r", 60, 40))),
        (1.0, merge(root(28, z=-6), torso(10), neck(10), arm("l", 90, 30), arm("r", 90, 30)), "in"),
        (1.4, merge(root(20, z=-6), torso(6)), "out"), (2.2, {})], [(1.0, 1.0)])
    attack("body_slam", "body", rng=(25, 70), hits=[[0.8, 1.1, ["chest", "belly", "shoulder_l", "shoulder_r", "head"], "crush", 2.2]], move=[[0.6, 1.0, 0, 50]], cd=4.0, tags=["heavy"])
    for side, sg in SIDES:
        swing(f"hip_check_{side}", 1.4, [
            (0, {}), (0.45, merge(body(0, -sg * 14, -sg * 8, x=-sg * 4), crouch(0.5))),
            (0.68, merge(body(0, sg * 22, sg * 10, x=sg * 7), torso(0, -sg * 10), tail_curve(-sg * 30)), "in"), (1.4, {})], [(0.68, 0.6)])
        attack(f"hip_check_{side}", "body", rng=(20, 55), arc="left" if side == "l" else "right", hits=[[0.5, 0.75, ["hips", f"thigh_{side}"], "blunt", 1.6]], cd=1.8)
    swing("belly_bump", 1.2, [(0, {}), (0.4, merge(torso(-10), body(z=3), crouch(0.3))), (0.58, merge(torso(-14), body(z=-6)), "in"), (1.2, {})], [(0.58, 0.8)])
    attack("belly_bump", "body", rng=(15, 45), hits=[[0.45, 0.65, ["belly", "chest"], "blunt", 1.4]], move=[[0.4, 0.6, 0, 40]], cd=1.6)
    swing("leaping_slam", 3.0, [
        (0, {}), (0.7, merge(crouch(1.6), torso(10), arm("l", 40, 30), arm("r", 40, 30))),
        (1.2, merge(root(-10, y=14), leg("l", -30, 50), leg("r", -20, 40), arm("l", 120, 40), arm("r", 120, 40), neck(-14), jaw(40, 1)), "out"),
        (1.7, merge(root(10, y=0), crouch(1.6), torso(30), arm("l", 40, 10), arm("r", 40, 10)), "in"), (2.2, merge(crouch(1.2), torso(18))), (3.0, {})], [(1.7, 1.0)])
    attack("leaping_slam", "body", rng=(70, 160), hits=[[1.45, 1.75, ["chest", "belly", "hips", "foot_l", "foot_r"], "crush", 2.4]], events=[[1.7, "shockwave", {"node": "hips", "radius": 45, "power": 1.4}]], move=[[0.8, 1.7, 0, 110]], cd=8.0, tags=["heavy", "gap_close"])
    swing("bulldoze", 2.8, [
        (0, {}), (0.6, merge(crouch(0.9), torso(26), neck(10), arm("l", 70, 10, elbow=20, fingers=40), arm("r", 70, 10, elbow=20, fingers=40))),
        (1.4, merge(crouch(1.0), torso(28), neck(10), arm("l", 80, 10, elbow=10, fingers=40), arm("r", 80, 10, elbow=10, fingers=40), leg("l", -24, 24))),
        (2.1, merge(crouch(1.0), torso(28), neck(10), arm("l", 80, 10, elbow=10, fingers=40), arm("r", 80, 10, elbow=10, fingers=40), leg("r", -24, 24))), (2.8, {})])
    attack("bulldoze", "body", rng=(20, 55), hits=[[0.6, 2.2, ["chest", "hand_l", "hand_r", "shoulder_l", "shoulder_r"], "blunt", 0.8]], move=[[0.6, 2.2, 0, 70]], cd=5.0, tags=["push"])
    swing("chest_bump", 1.0, [(0, {}), (0.35, merge(torso(-14), neck(-10), arm("l", 20, 40), arm("r", 20, 40))), (0.5, merge(torso(14), body(z=-4)), "in"), (1.0, {})], [(0.5, 0.6)])
    attack("chest_bump", "body", rng=(15, 45), hits=[[0.38, 0.55, ["chest"], "blunt", 1.2]], cd=1.2)


def leg_attacks():
    for side, sg in SIDES:
        o = "r" if side == "l" else "l"
        swing(f"stomp_{side}", 1.4, [
            (0, {}), (0.5, merge(leg(side, -60, 70, -10), body(x=-sg * 3, roll=-sg * 6), torso(-6), neck(-6))),
            (0.72, merge(leg(side, -6, 8, 6), body(x=sg * 1, roll=sg * 3, y=-2), torso(10), neck(8)), "in"), (1.4, {})], [(0.72, 1.0)])
        attack(f"stomp_{side}", "leg", rng=(10, 55), hits=[[0.6, 0.75, FEET[side], "stomp", 1.5]], events=[[0.72, "shockwave", {"node": f"foot_{side}", "radius": 16, "power": 0.7}]], cd=1.6, tags=["low"])
        swing(f"side_kick_{side}", 1.5, [
            (0, {}), (0.45, merge(leg(side, -50, 80, 0, 10), body(x=-sg * 4, roll=-sg * 14), torso(0, 0, -sg * 6))),
            (0.65, merge(leg(side, -30, 0, 0, 55), body(x=-sg * 6, roll=-sg * 22), torso(0, 0, -sg * 10), tail_curve(sg * 20)), "in"), (1.5, {})], [(0.65, 0.5)])
        attack(f"side_kick_{side}", "leg", rng=(25, 60), arc="left" if side == "l" else "right", hits=[[0.5, 0.7, FEET[side], "blunt", 1.4]], cd=1.8)
    swing("front_kick", 1.6, [
        (0, {}), (0.5, merge(leg("r", -60, 90, 0), body(x=3, roll=6), torso(-10))),
        (0.72, merge(leg("r", -70, 0, -10), body(x=4, roll=8, z=-3), torso(-16), neck(-8)), "in"), (1.0, merge(leg("r", -50, 30), torso(-10))), (1.6, {})], [(0.72, 0.6)])
    attack("front_kick", "leg", rng=(25, 60), hits=[[0.55, 0.78, FEET["r"], "blunt", 1.6]], cd=2.0)
    swing("ground_pound", 2.4, [
        (0, {}), (0.8, merge(crouch(1.5), torso(16), arm("l", 50, 30), arm("r", 50, 30))),
        (1.2, merge(root(0, y=12), leg("l", -20, 30), leg("r", -20, 30), torso(-6), neck(-16), jaw(30, 1)), "out"),
        (1.5, merge(crouch(1.6), torso(20)), "in"), (2.4, {})], [(1.5, 1.0)])
    attack("ground_pound", "leg", rng=(0, 50), arc="any", hits=[[1.4, 1.55, FEET["l"] + FEET["r"], "stomp", 1.8]], events=[[1.5, "shockwave", {"node": "hips", "radius": 60, "power": 1.5}]], cd=7.0, tags=["surround", "trip"])
    swing("knee_strike", 1.2, [(0, {}), (0.4, merge(leg("l", 10, 40), torso(-6))), (0.58, merge(leg("l", -90, 110), torso(14), body(z=-3)), "in"), (1.2, {})], [(0.58, 0.5)])
    attack("knee_strike", "leg", rng=(15, 45), hits=[[0.45, 0.62, ["knee_l", "thigh_l"], "blunt", 1.4]], cd=1.6)


def grapples():
    grip = merge(arm("l", 85, 5, elbow=40, fingers=60), arm("r", 85, 5, elbow=40, fingers=60))
    swing("grab_throw", 3.2, [
        (0, {}), (0.5, merge(torso(10), arm("l", 80, 40, elbow=10, fingers=-20), arm("r", 80, 40, elbow=10, fingers=-20), neck(8))),
        (0.75, merge(torso(14), grip, body(z=-3)), "in"),
        (1.6, merge(torso(-12, 60), grip, neck(-6, 30), crouch(0.8), root(0, 40))),
        (2.05, merge(torso(4, -60), arm("l", 100, 50, elbow=10), arm("r", 100, 50, elbow=10), root(0, -70), neck(4, -40), jaw(30, 1)), "in"), (3.2, {})])
    attack("grab_throw", "grapple", rng=(20, 55), hits=[], events=[[0.75, "grab", {"node": "hand_r", "target": "chest"}], [2.05, "release", {"fwd": 120, "side": -200, "up": 110}]], cd=7.0, need=["arm_l", "arm_r"], tags=["grapple", "throw_enemy"])
    swing("grab_bodyslam", 3.4, [
        (0, {}), (0.5, merge(torso(10), arm("l", 80, 40, elbow=10, fingers=-20), arm("r", 80, 40, elbow=10, fingers=-20))),
        (0.75, merge(torso(14), grip), "in"),
        (1.8, merge(torso(-24), arm("l", 160, 10, elbow=10, fingers=60), arm("r", 160, 10, elbow=10, fingers=60), crouch(0.6), neck(-20), jaw(30, 1))),
        (2.25, merge(torso(36), arm("l", 40, 10, fingers=60), arm("r", 40, 10, fingers=60), crouch(1.4), neck(20)), "in"), (3.4, {})], [(2.25, 1.0)])
    attack("grab_bodyslam", "grapple", rng=(20, 55), events=[[0.75, "grab", {"node": "hand_r", "target": "chest"}], [2.25, "slam", {"power": 2.4}]], cd=9.0, need=["arm_l", "arm_r"], tags=["grapple"])
    swing("headlock_bite", 3.6, [
        (0, {}), (0.6, merge(torso(14), arm("l", 90, 20, elbow=60, fingers=60), arm("r", 70, -10, elbow=80, fingers=60), neck(10, 20), jaw(40))),
        (0.85, merge(torso(16, 10), arm("l", 80, 0, elbow=70, fingers=60), arm("r", 80, -20, elbow=90, fingers=60), neck(20, 20), jaw(6)), "in"),
        (1.6, merge(torso(16, 20), neck(22, 26), jaw(4))), (2.3, merge(torso(16, -10), neck(22, 10), jaw(4))),
        (3.0, merge(torso(-10, -40), neck(-6, -30), jaw(30)), "in"), (3.6, {})])
    attack("headlock_bite", "grapple", rng=(20, 55), hits=[[0.7, 0.9, HEAD, "bite", 1.0]], events=[[0.85, "grab", {"node": "hand_l", "target": "head"}], [1.6, "pulse_hit", {"power": 0.6}], [2.3, "pulse_hit", {"power": 0.6}], [3.0, "release", {"fwd": 30, "side": -140, "up": 20}]], cd=9.0, need=["arm_l"], tags=["grapple"])
    swing("clinch_shove", 2.0, [
        (0, {}), (0.45, merge(torso(10), arm("l", 80, 20, elbow=20, fingers=50), arm("r", 80, 20, elbow=20, fingers=50))),
        (0.7, merge(torso(14), grip, body(z=-2)), "in"), (1.2, merge(torso(-8), grip, crouch(0.8))),
        (1.45, merge(torso(20), arm("l", 90, 0, elbow=-10), arm("r", 90, 0, elbow=-10), body(z=-6)), "in"), (2.0, {})], [(1.45, 0.6)])
    attack("clinch_shove", "grapple", rng=(18, 55), events=[[0.7, "grab", {"node": "hand_l", "target": "chest"}], [1.45, "release", {"fwd": 260, "side": 0, "up": 30}]], cd=5.0, tags=["grapple", "push"])
    swing("hip_throw", 3.0, [
        (0, {}), (0.5, merge(torso(10), grip)), (0.75, merge(torso(14), grip, body(z=-3)), "in"),
        (1.5, merge(root(0, 90), torso(30, 0, 20), grip, crouch(1.2))), (2.0, merge(root(0, 150), torso(50, 0, 10), arm("l", 40, 10), arm("r", 40, 10), crouch(1.4)), "in"), (3.0, root(0, 150))], [(2.0, 0.8)])
    attack("hip_throw", "grapple", rng=(18, 55), events=[[0.75, "grab", {"node": "hand_r", "target": "chest"}], [2.0, "slam", {"power": 2.0}]], cd=9.0, need=["arm_l", "arm_r"], tags=["grapple"])
    for side, sg in SIDES:
        c = swing(f"tail_wrap_{side}", 2.8, [
            (0, {}), (0.6, merge(root(0, -sg * 60), tail_curve(sg * 80, 0, 0, start=4))),
            (1.0, merge(root(0, -sg * 100), tail_curve(sg * 200, -10, 0, start=6)), "in"),
            (1.8, merge(root(0, -sg * 60), tail_curve(sg * 160, -10, 0, start=6), crouch(0.8), torso(10))),
            (2.1, merge(root(0, sg * 10), tail_curve(-sg * 40, 0), crouch(0.6)), "in"), (2.8, {})])
        attack(f"tail_wrap_{side}", "grapple", rng=(50, 160), arc="left" if side == "l" else "right", hits=[[0.8, 1.05, TAIL_TIP, "blunt", 0.6]], events=[[1.0, "grab", {"node": "tail14", "target": "legs"}], [2.1, "slam", {"power": 1.4}]], cd=9.0, need=["tail"], tags=["grapple", "trip"])
    swing("pin_down", 3.0, [
        (0, {}), (0.6, merge(crouch(1.2), torso(30), arm("l", 60, 10, elbow=10, fingers=60), arm("r", 60, 10, elbow=10, fingers=60))),
        (0.85, merge(crouch(1.6), torso(44), neck(20), arm("l", 50, 0, fingers=60), arm("r", 50, 0, fingers=60)), "in"),
        (2.4, merge(crouch(1.6), torso(44), neck(30, 0, 0, 20), arm("l", 50, 0, fingers=60), arm("r", 50, 0, fingers=60), jaw(20, 1))), (3.0, {})], [(0.85, 0.7)])
    attack("pin_down", "grapple", rng=(15, 70), hits=[[0.7, 0.9, HANDS["l"] + HANDS["r"], "crush", 1.0]], events=[[0.85, "pin", {"secs": 2.0}]], cd=8.0, need=["enemy_down"], tags=["mount"])


def world():
    reach = merge(crouch(1.2), torso(34), neck(14), arm("l", 70, 20, elbow=10, fingers=-30), arm("r", 70, 20, elbow=10, fingers=-30))
    lift = merge(torso(-6), arm("l", 95, 18, elbow=40, fingers=40), arm("r", 95, 18, elbow=40, fingers=40), neck(-6))
    swing("uproot_tree", 2.6, [(0, {}), (0.8, reach), (1.1, merge(reach, arm("l", 75, 10, elbow=20, fingers=60), arm("r", 75, 10, elbow=20, fingers=60))),
                               (1.8, merge(crouch(0.6), torso(-12), lift, neck(-14), jaw(24)), "in"), (2.6, lift)], [(1.8, 0.5)])
    attack("uproot_tree", "world", rng=(0, 9999), arc="any", events=[[1.1, "uproot", {"node": "hand_r", "kind": "tree"}]], cd=6.0, need=["not_holding", "arm_l", "arm_r"], tags=["world"])
    swing("grab_chunk", 2.8, [(0, {}), (0.9, merge(reach, crouch(0.4))), (1.2, merge(reach, arm("l", 70, 10, elbow=20, fingers=70), arm("r", 70, 10, elbow=20, fingers=70))),
                              (2.0, merge(crouch(0.8), torso(-14), lift, neck(-16), jaw(34, 1)), "in"), (2.8, lift)], [(1.2, 0.6), (2.0, 0.6)])
    attack("grab_chunk", "world", rng=(0, 9999), arc="any", events=[[1.2, "uproot", {"node": "hand_r", "kind": "chunk"}]], cd=6.0, need=["not_holding", "arm_l", "arm_r"], tags=["world"])
    swing("throw_held", 1.8, [(0, lift), (0.6, merge(torso(-24, 20), arm("l", 160, 30, elbow=40, fingers=40), arm("r", 160, 30, elbow=40, fingers=40), neck(-16), crouch(0.5))),
                              (0.9, merge(torso(26, -10), arm("l", 60, 0, elbow=0), arm("r", 60, 0, elbow=0), neck(14), body(z=-4), jaw(30, 1)), "in"), (1.8, {})], [(0.9, 0.6)])
    attack("throw_held", "world", rng=(60, 3000), events=[[0.88, "release", {"aim": True, "speed": 520}]], cd=1.0, need=["holding"], tags=["ranged", "world"])
    swing("swing_held", 2.0, [(0, lift), (0.6, merge(torso(-8, 40), arm("l", 130, 70, elbow=30, fingers=40), arm("r", 130, 70, elbow=30, fingers=40), neck(0, 20))),
                              (0.95, merge(torso(14, -50), arm("l", 70, -30, elbow=10, fingers=40), arm("r", 70, -30, elbow=10, fingers=40), neck(10, -20), body(z=-4)), "in"),
                              (1.3, merge(torso(14, -56), arm("l", 50, -30, elbow=20), arm("r", 50, -30, elbow=20)), "out"), (2.0, lift)], [(0.95, 0.8)])
    attack("swing_held", "world", rng=(20, 90), hits=[[0.7, 1.0, ["hand_l", "hand_r", "held"], "blunt", 2.0]], events=[[1.0, "shatter_held", {}]], cd=1.5, need=["holding"], tags=["world"])
    swing("kick_debris", 1.6, [(0, {}), (0.5, merge(leg("r", 30, 60, 10), torso(-8))), (0.72, merge(leg("r", -70, 10, -20), torso(-14), body(z=-3)), "in"), (1.6, {})], [(0.72, 0.5)])
    attack("kick_debris", "world", rng=(60, 900), events=[[0.7, "kick_ground", {"node": "foot_r", "speed": 380}]], cd=4.0, tags=["ranged", "world"])
    swing("rip_ground_throw", 3.2, [(0, {}), (0.9, merge(reach, crouch(0.6), arm("l", 60, 10, fingers=70), arm("r", 60, 10, fingers=70))),
                                    (1.7, merge(crouch(0.8), torso(-20), arm("l", 150, 20, elbow=20, fingers=60), arm("r", 150, 20, elbow=20, fingers=60), neck(-20), jaw(40, 1)), "in"),
                                    (2.2, merge(torso(28), arm("l", 50, 0), arm("r", 50, 0), neck(16), body(z=-4)), "in"), (3.2, {})], [(1.7, 0.8), (2.2, 0.6)])
    attack("rip_ground_throw", "world", rng=(80, 2000), events=[[1.2, "uproot", {"node": "hand_r", "kind": "slab"}], [2.2, "release", {"aim": True, "speed": 440}]], cd=9.0, need=["not_holding", "arm_l", "arm_r"], tags=["ranged", "world"])


def plates_wave(c, t0, t1, out=True, plates=None):
    """plate sliders: out from the tail tip to the head (or back in from the head to the tail)"""
    for p in plates:
        s, T = p["s"], p["travel"]
        if out:
            ts = t0 + (t1 - t0) * s
            keys = {"0": [0, 0, 0], f"{ts:.3f}": [0, 0, 0], f"{ts + 0.09:.3f}": [0, round(T * 1.18, 3), 0],
                    f"{ts + 0.17:.3f}": [0, round(T * 0.94, 3), 0], f"{ts + 0.24:.3f}": [0, round(T, 3), 0]}
        else:
            ts = t0 + (t1 - t0) * (1 - s)
            keys = {"0": [0, round(T, 3), 0], f"{ts:.3f}": [0, round(T, 3), 0], f"{ts + 0.07:.3f}": [0, round(-T * 0.1, 3), 0], f"{ts + 0.14:.3f}": [0, 0, 0]}
        c.extra[("position", p["bone"])] = keys


def plates_hold(c, plates, jitter=True):
    for p in plates:
        T = p["travel"]
        keys = {}
        n = int(c.length / 0.1)
        for k in range(n + 1):
            t = k * 0.1
            j = T * 0.04 * math.sin(t * 50 + hash(p["bone"]) % 7) if jitter else 0
            keys[f"{t:.1f}"] = [0, round(T + j, 3), 0]
        c.extra[("position", p["bone"])] = keys


def atomic(plates):
    """breath = charge -> fire (loop, length decided by the server) -> release. Variants change the fire loop."""
    # film: hunch and plant the feet while the plates light up, stiffen the tail as a counterweight, then a deep
    # inhale that throws the head back; firing, the neck straightens and the head reaches forward, the jaw gapes,
    # the body is shoved back in pulses by the beam's recoil
    brace = merge(crouch(0.9), torso(10), neck(10, head_pitch=6), arm("l", 18, 14, elbow=20, fingers=-18), arm("r", 18, 14, elbow=20, fingers=-18),
                  stance_wide(1.3), tail_curve(0, 18, 0), {"throat": [4, 0, 0]})
    rear = merge(brace, torso(-14), neck(-26, head_pitch=-24), jaw(22, 0.6), {"throat": [-10, 0, 0]}, tail_curve(0, 10, 0))
    fire = merge(crouch(0.9), stance_wide(1.4), torso(14), neck(16, head_pitch=-6), jaw(50, 1), {"throat": [14, 0, 0]},
                 arm("l", 26, 26, elbow=22, fingers=24), arm("r", 26, 26, elbow=22, fingers=24), tail_curve(0, 26, 0))

    def charge(name, L, pose_end=rear, base=None):
        c = clip(name, L)
        b = base or {}
        c.key(0, b).key(0.6, merge(b, brace)).key(L - 0.5, merge(b, brace, neck(8), torso(4))).key(L, merge(b, pose_end), "in")
        plates_wave(c, 0.4, L - 0.5, True, plates)
        c.extra[("scale", "chest")] = {"0": [1, 1, 1], f"{L - 0.5:.2f}": [1, 1, 1], f"{L:.2f}": [1.06, 1.05, 1.07]}
        return c

    def fire_loop(name, L, pose, fn=None):
        c = clip(name, L, True)
        c.key(0, pose).key(L, pose)
        c.add_fn("head", lambda t: [0.6 * math.sin(t * 110) - 2.5 * max(0, math.sin(t * 9)), 0.6 * math.sin(t * 87), 0])
        c.add_fn("torso", lambda t: [-2.2 * max(0, math.sin(t * 9)) ** 2, 0, 0])
        c.add_fn("body", lambda t: [0, 0, 0.6 * math.sin(t * 9 + 1)], kind="position")
        if fn:
            for b, f in fn.items():
                c.add_fn(b, f)
        plates_hold(c, plates)
        c.springs = False
        return c

    def release(name, L=1.1, pose=fire):
        c = clip(name, L)
        c.key(0, pose).key(0.4, merge(pose, jaw(-30)), "smooth").key(L, {})
        plates_wave(c, 0.05, 0.75, False, plates)
        return c

    charge("atomic_charge", 2.6)
    charge("atomic_charge_quick", 1.3)
    charge("atomic_charge_max", 4.2)
    fire_loop("atomic_fire", 1.0, fire)
    fire_loop("atomic_fire_sweep", 2.4, fire, {"neck1": lambda t: [0, 20 * math.sin(t / 2.4 * 2 * math.pi), 0], "neck2": lambda t: [0, 14 * math.sin(t / 2.4 * 2 * math.pi), 0],
                                               "torso": lambda t: [0, 18 * math.sin(t / 2.4 * 2 * math.pi), 0]})
    fire_loop("atomic_fire_up", 1.0, merge(fire, neck(-50, head_pitch=-30), torso(-14)))
    fire_loop("atomic_fire_down", 1.0, merge(fire, neck(36, head_pitch=26), torso(24), crouch(0.6)))
    release("atomic_release")
    release("atomic_release_up", pose=merge(fire, neck(-50, head_pitch=-30), torso(-14)))
    release("atomic_release_down", pose=merge(fire, neck(36, head_pitch=26), torso(24), crouch(0.6)))
    # from the ground (lying on the back): lift the head and fire along the body
    gfire = merge(LIE_BACK, neck(-30, head_pitch=-10), jaw(40, 1))
    charge("atomic_charge_ground", 1.8, pose_end=merge(LIE_BACK, neck(-20), jaw(20)), base=LIE_BACK)
    fire_loop("atomic_fire_ground", 1.0, gfire)
    c = clip("atomic_release_ground", 1.1)
    c.key(0, gfire).key(1.1, LIE_BACK)
    plates_wave(c, 0.05, 0.75, False, plates)
    # nuclear pulse: plates out, curl in, then everything bursts outward
    c = clip("nuclear_pulse", 4.2)
    curl = merge(crouch(1.6), torso(30), neck(30, head_pitch=10), arm("l", 60, -10, elbow=70, fingers=60), arm("r", 60, -10, elbow=70, fingers=60), tail_curve(-60, 30))
    burst = merge(torso(-20), neck(-46, head_pitch=-30), jaw(46, 1), arm("l", 70, 70, elbow=0, fingers=-30), arm("r", 70, 70, elbow=0, fingers=-30), tail_curve(0, 30), body(y=2))
    c.key(0, {}).key(2.4, curl).key(2.7, burst, "in").key(3.4, burst).key(4.2, {}).impact(2.7, 1.0)
    plates_wave(c, 0.3, 2.0, True, plates)
    for p in plates:  # stay out through the burst, then slam in
        keys = c.extra[("position", p["bone"])]
        keys["3.400"] = [0, round(p["travel"], 3), 0]
        keys[f"{3.45 + 0.4 * (1 - p['s']):.3f}"] = [0, 0, 0]

    def breath(id_, charge_clip, fire_clip, release_clip, fire_secs, rng, power, width, extra=None, cd=10.0, need=(), tags=()):
        clen = CLIPS[charge_clip].length
        attack(id_, "atomic", clip_name=charge_clip, length=clen + fire_secs + CLIPS[release_clip].length, rng=rng, arc="front", cd=cd,
               need=list(need) + ["breath_ready"], tags=["breath", "ranged"] + list(tags),
               phases=[[charge_clip, clen], [fire_clip, fire_secs], [release_clip, CLIPS[release_clip].length]],
               beam=dict({"power": power, "width": width, "fire_start": clen, "fire_end": clen + fire_secs}, **(extra or {})))

    breath("atomic_breath", "atomic_charge", "atomic_fire", "atomic_release", 3.0, (60, 4000), 1.0, 1.0)
    breath("atomic_sweep", "atomic_charge", "atomic_fire_sweep", "atomic_release", 4.8, (40, 3000), 0.8, 1.0, {"sweep": 1})
    breath("atomic_burst", "atomic_charge_quick", "atomic_fire", "atomic_release", 0.9, (40, 2500), 0.7, 0.7, cd=6.0)
    breath("atomic_aerial", "atomic_charge", "atomic_fire_up", "atomic_release_up", 2.5, (60, 4000), 1.0, 1.0, {"pitch": -40}, tags=["anti_air"])
    breath("atomic_ground_pin", "atomic_charge_quick", "atomic_fire_down", "atomic_release_down", 2.5, (0, 90), 1.2, 1.0, {"pitch": 45}, need=["enemy_down"], tags=["mount"])
    breath("atomic_point_blank", "atomic_charge_quick", "atomic_fire", "atomic_release", 1.2, (0, 70), 1.3, 1.2, cd=8.0)
    breath("atomic_charged_max", "atomic_charge_max", "atomic_fire", "atomic_release", 4.0, (80, 5000), 1.8, 1.6, cd=20.0, tags=["heavy"])
    breath("atomic_ground", "atomic_charge_ground", "atomic_fire_ground", "atomic_release_ground", 2.0, (0, 3000), 0.9, 0.9, need=["self_down"], tags=["ground"])
    attack("nuclear_pulse", "atomic", rng=(0, 140), arc="any", events=[[2.7, "pulse", {"radius": 120, "power": 2.5}]], cd=30.0, need=["breath_ready"], tags=["surround", "escape"])


def mounted_and_ground():
    swing("stomp_downed", 1.8, [(0, {}), (0.6, merge(leg("r", -70, 80, -10), body(x=3, roll=6), torso(10), neck(16))),
                                (0.85, merge(leg("r", -10, 10, 10), body(roll=-3, y=-3), torso(20), neck(20)), "in"),
                                (1.1, merge(leg("r", -40, 60), torso(10))), (1.3, merge(leg("r", -6, 8), torso(20), body(y=-3)), "in"), (1.8, {})], [(0.85, 1.0), (1.3, 1.0)])
    attack("stomp_downed", "mount", rng=(10, 70), hits=[[0.7, 0.9, FEET["r"], "stomp", 1.8], [1.15, 1.35, FEET["r"], "stomp", 1.8]], events=[[0.85, "shockwave", {"node": "foot_r", "radius": 18, "power": 0.5}]], cd=2.5, need=["enemy_down"], tags=["mount"])
    swing("mount_claw", 2.6, [(0, {}), (0.6, merge(crouch(1.8), torso(44), neck(20))),
                              (0.9, merge(crouch(1.8), torso(50), arm("l", 120, 20, elbow=30, fingers=-20), neck(24))), (1.1, merge(crouch(1.8), torso(56), arm("l", 30, 0, fingers=60)), "in"),
                              (1.4, merge(crouch(1.8), torso(50), arm("r", 120, 20, elbow=30, fingers=-20))), (1.6, merge(crouch(1.8), torso(56), arm("r", 30, 0, fingers=60)), "in"), (2.6, {})])
    attack("mount_claw", "mount", rng=(10, 70), hits=[[0.95, 1.15, HANDS["l"], "cut", 1.2], [1.45, 1.65, HANDS["r"], "cut", 1.2]], cd=3.0, need=["enemy_down"], tags=["mount"])
    swing("elbow_drop", 2.4, [(0, {}), (0.7, merge(crouch(1.0), arm("r", 140, 40, elbow=120), torso(-8))),
                              (1.2, merge(root(60, y=-4), arm("r", 60, 40, elbow=130), torso(10), neck(10)), "in"), (1.6, merge(root(50, y=-4), torso(8))), (2.4, {})], [(1.2, 1.0)])
    attack("elbow_drop", "mount", rng=(20, 80), hits=[[0.95, 1.25, ["elbow_r", "shoulder_r", "chest"], "crush", 2.2]], cd=6.0, need=["enemy_down"], tags=["mount", "heavy"])
    swing("tail_slam_downed", 2.2, [(0, {}), (0.6, merge(root(0, 150), tail_curve(0, 120, 0, start=3))), (0.95, merge(root(0, 160), tail_curve(0, -20)), "in"), (2.2, root(0, 180))], [(0.95, 1.0)])
    attack("tail_slam_downed", "mount", rng=(60, 160), arc="any", hits=[[0.75, 1.0, TAIL_TIP, "crush", 1.8]], cd=3.5, need=["enemy_down", "tail"], tags=["mount"])
    swing("pin_bite", 2.6, [(0, {}), (0.6, merge(crouch(1.8), torso(46), neck(20), jaw(40, 1))),
                            (0.85, merge(crouch(1.8), torso(56), neck(40, 0, 0, 20), jaw(6)), "in"), (1.6, merge(crouch(1.8), torso(54), neck(36, 20), jaw(8))), (2.0, merge(crouch(1.8), torso(54), neck(36, -20), jaw(8))), (2.6, {})])
    attack("pin_bite", "mount", rng=(10, 70), hits=[[0.7, 0.9, HEAD, "bite", 1.5]], events=[[1.6, "pulse_hit", {"power": 0.6}], [2.0, "pulse_hit", {"power": 0.6}]], cd=4.0, need=["enemy_down"], tags=["mount"])
    # self on the ground: kick, sweep, roll, kip-up strike, grab an ankle
    swing("ground_kick", 1.4, [(0, LIE_BACK), (0.45, merge(LIE_BACK, leg("r", -90, 100))), (0.65, merge(LIE_BACK, leg("r", -110, 0, -20)), "in"), (1.4, LIE_BACK)])
    attack("ground_kick", "ground", clip_name="ground_kick", rng=(0, 60), arc="any", hits=[[0.5, 0.7, FEET["r"], "blunt", 1.4]], cd=1.5, need=["self_down"], tags=["defense"])
    # lying on the back the body's long axis is vertical-ish, so a sweep along the ground is a roll of the tail joints
    sweep = lambda deg: {f"tail{i}": [0, 0, deg * (0.45 if i == 1 else 0.55 / 7)] for i in range(1, 9)}
    swing("ground_tail_sweep", 1.8, [(0, LIE_BACK), (0.5, merge(LIE_BACK, sweep(55))),
                                     (0.95, merge(LIE_BACK, sweep(-65)), "in"), (1.8, LIE_BACK)])
    attack("ground_tail_sweep", "ground", rng=(0, 160), arc="any", hits=[[0.6, 1.0, TAIL_TIP + TAIL_MID, "blunt", 1.2]], cd=2.0, need=["self_down", "tail"], tags=["defense", "trip"])
    swing("roll_away", 1.6, [(0, LIE_BACK), (0.6, merge(root(-10, 0, 80, y=13, x=-20), leg("l", -50, 50))), (1.1, merge(LIE_FRONT, root(0, 0, 0, x=-40))), (1.6, merge(LIE_BACK, root(0, 0, 0, x=-50)))])
    attack("roll_away", "ground", rng=(0, 9999), arc="any", events=[[0.1, "dodge", {"secs": 1.0}]], move=[[0.1, 1.5, -30, 0]], cd=3.0, need=["self_down"], tags=["defense"])
    swing("kip_up_strike", 2.2, [(0, LIE_BACK), (0.6, merge(LIE_FRONT, arm("l", 80, 20))), (1.2, merge(crouch(1.5), torso(30), arm("r", 140, 30, elbow=20, fingers=-20))),
                                 (1.5, merge(crouch(0.8), torso(-6, -30), arm("r", 70, -20, -30, fingers=60), neck(0, -20)), "in"), (2.2, {})])
    attack("kip_up_strike", "ground", rng=(0, 60), arc="any", hits=[[1.25, 1.55, HANDS["r"], "cut", 1.2]], cd=3.0, need=["self_down"], tags=["defense", "getup"])
    swing("ground_claw_grab", 2.4, [(0, LIE_FRONT), (0.5, merge(LIE_FRONT, arm("r", 120, 20, elbow=10, fingers=-30))),
                                    (0.75, merge(LIE_FRONT, arm("r", 100, 10, elbow=30, fingers=70)), "in"), (1.5, merge(LIE_FRONT, arm("r", 30, 10, elbow=80, fingers=70), torso(-10)), "in"), (2.4, LIE_FRONT)])
    attack("ground_claw_grab", "ground", rng=(0, 55), arc="any", hits=[[0.6, 0.8, HANDS["r"], "cut", 0.8]], events=[[0.75, "trip", {}]], cd=4.0, need=["self_down", "arm_r"], tags=["defense", "trip"])


def defense():
    guard = merge(arm("l", 90, -10, elbow=80, fingers=40), arm("r", 90, -10, elbow=80, fingers=40), neck(14, head_pitch=10), crouch(0.7), torso(8))
    swing("block_arms", 1.6, [(0, {}), (0.2, guard, "out"), (1.3, guard), (1.6, {})])
    attack("block_arms", "defense", rng=(0, 9999), arc="any", events=[[0.1, "guard", {"secs": 1.2}]], cd=1.0, tags=["defense"])
    for side, sg in SIDES:
        swing(f"sidestep_{side}", 1.2, [(0, {}), (0.4, merge(leg(side, -20, 30, 0, 20), body(x=sg * 6, roll=sg * 6), torso(0, 0, -sg * 6))), (1.2, {})], [(0.5, 0.3)])
        attack(f"sidestep_{side}", "defense", rng=(0, 9999), arc="any", events=[[0.05, "dodge", {"secs": 0.6}]], move=[[0.05, 0.6, sg * 90, 0]], cd=2.5, tags=["defense", "dodge"])
    swing("backstep", 1.3, [(0, {}), (0.45, merge(leg("l", 25, 30), body(z=6), torso(-6), neck(-8))), (1.3, {})], [(0.55, 0.3)])
    attack("backstep", "defense", rng=(0, 9999), arc="any", events=[[0.05, "dodge", {"secs": 0.6}]], move=[[0.05, 0.7, 0, -90]], cd=2.5, tags=["defense", "dodge"])
    swing("parry_counter", 1.8, [(0, {}), (0.15, merge(arm("l", 80, 20, elbow=60, fingers=40), arm("r", 60, 20, elbow=40), crouch(0.5)), "out"),
                                 (0.7, merge(arm("l", 80, 20, elbow=60, fingers=40), arm("r", 60, 20, elbow=40), crouch(0.5))),
                                 (0.9, merge(torso(10, -30), arm("r", 80, -10, -30, elbow=10, fingers=50), neck(10, -14), body(z=-3)), "in"), (1.8, {})])
    attack("parry_counter", "defense", rng=(0, 70), arc="front", hits=[[0.75, 0.95, HANDS["r"], "cut", 1.4]], events=[[0.1, "parry", {"secs": 0.6}]], cd=4.0, need=["arm_r"], tags=["defense"])
    attack("roar", "intimidate", rng=(0, 9999), arc="any", events=[[1.35, "roar", {"radius": 300}]], cd=15.0, tags=["intimidate"])
    swing("tail_lash_intimidate", 2.0, [(0, {}), (0.5, merge(tail_curve(60, 40, 30), neck(-10), jaw(30, 1))), (1.0, merge(tail_curve(-60, 40, -30), neck(-10), jaw(30, 1)), "in"), (1.5, merge(tail_curve(40, 20, 20))), (2.0, {})])
    attack("tail_lash_intimidate", "intimidate", rng=(0, 9999), arc="any", events=[[1.0, "roar", {"radius": 120}]], cd=12.0, tags=["intimidate"])


def build(plates):
    CLIPS.clear()
    ATTACKS.clear()
    locomotion()
    reactions()
    claws()
    tails()
    head_attacks()
    body_attacks()
    leg_attacks()
    grapples()
    world()
    atomic(plates)
    mounted_and_ground()
    defense()
    for a in ATTACKS:
        if not a["len"]:
            a["len"] = CLIPS[a["clip"]].length
    return CLIPS, ATTACKS
