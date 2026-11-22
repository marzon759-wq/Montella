"""Destoroyah's clips and attacks.

It shares the rig names with the atomic kaiju, so the whole generic fighting set (claws, tail, bites, body, legs,
grapples, world, mounted, from-the-ground, defense, reactions, locomotion) is reused; every one of those clips gets the
wings held half-folded behind the shoulders (with a little life in them). On top come Destoroyah's own powers:
  micro-oxygen beam (mouth, magenta; burst / sweep / from the air), the laser horn (a cutting beam from the horn tip),
  horn gore and horn uppercut, wing buffet (a gust that staggers) and wing shield, the scorpion tail pincer (clamp the
  neck and inject micro-oxygen, or stab over the shoulder), flight (take off, hover, fly, land), dive-claw from the air
  and the film's grab-and-drop: snatch the enemy, climb, let go.
"""
import math
import moves
from moves import clip, attack, swing, CLIPS, ATTACKS, HANDS, FEET, HEAD, TAIL_TIP
from animlib import merge, tail_curve, neck, arm, leg, jaw, body, root, torso, crouch, stance_wide, scale_pose, TAILS, SIDES

WING_BONES = [f"{b}_{s}" for b in ("wing", "wingfore", "wingtip") for s in ("l", "r")]


def wings(raise_=0.0, sweep=0.0, fore=0.0, tip=0.0, side=None):
    """raise_: + lifts the wing up; sweep: + swings it back; fore/tip: + folds the forearm/fingers back and down"""
    p = {}
    for s, sg in SIDES:
        if side and s != side:
            continue
        p[f"wing_{s}"] = [0, sg * sweep, sg * raise_]
        p[f"wingfore_{s}"] = [0, sg * fore * 0.8, -sg * fore * 0.6]
        p[f"wingtip_{s}"] = [0, sg * tip * 0.7, -sg * tip * 0.7]
    return p


HALF = wings(8, 34, 38, 34)        # half-folded behind the shoulders: how it fights on foot
SPREAD = wings(6, -4, 0, 0)        # spread wide: threat, roar, beam
FLAP_UP = wings(40, 6, -14, -8)
FLAP_DOWN = wings(-34, -6, 12, 18)


def add_wing_life(c, base=HALF, amp=3.0, period=3.0):
    """wings held in a pose with a slow breathing sway (procedural, on top of any clip)"""
    for b in WING_BONES:
        v = base.get(b, [0, 0, 0])
        ph = 0.0 if b.endswith("_l") else 0.6
        c.add_fn(b, lambda t, v=v, ph=ph: [v[0], v[1], v[2] + amp * math.sin(2 * math.pi * t / period + ph) * (1 if v[2] >= 0 else -1)])


def flight():
    # flying: body leaned forward from the hips, legs trailing, tail straight back, big slow wingbeats
    def flap_clip(name, L, lean, legs_back, amp):
        c = clip(name, L, True)
        w = 2 * math.pi / L
        c.add_fn("body", lambda t: [lean + 3 * math.sin(w * t + 1.2), 0, 0])
        c.add_fn("body", lambda t: [0, 4 * math.sin(w * t + 0.6), 0], kind="position")
        c.add_fn("torso", lambda t: [6, 0, 0])
        c.add_fn("neck1", lambda t: [-lean * 0.45, 0, 0])
        c.add_fn("head", lambda t: [-lean * 0.3 + 2 * math.sin(w * t), 0, 0])
        for s, sg in SIDES:
            c.add_fn(f"thigh_{s}", lambda t: [legs_back, 0, 0])
            c.add_fn(f"shin_{s}", lambda t: [legs_back * 0.4, 0, 0])
            c.add_fn(f"foot_{s}", lambda t: [30, 0, 0])
            c.add_fn(f"arm_{s}", lambda t: [-30, 0, 0])
            c.add_fn(f"forearm_{s}", lambda t: [-40, 0, 0])
            c.add_fn(f"wing_{s}", lambda t, sg=sg: [0, sg * 4 * math.sin(w * t - 0.5), sg * (4 + amp * math.sin(w * t))])
            c.add_fn(f"wingfore_{s}", lambda t, sg=sg: [0, 0, -sg * (6 + 14 * math.sin(w * t - 0.7))])
            c.add_fn(f"wingtip_{s}", lambda t, sg=sg: [0, 0, -sg * (8 + 16 * math.sin(w * t - 1.2))])
        for i, b in enumerate(TAILS):
            c.add_fn(b, lambda t, i=i: [-lean / 16 + 1.2 * math.sin(w * t - i * 0.3), 2.2 * math.sin(w * 0.5 * t - i * 0.3), 0])
        c.springs = True
        return c

    flap_clip("fly", 1.4, 58, 50, 36)
    flap_clip("hover", 1.8, 18, 18, 42)
    # take off: crouch, spread, leap with a huge downstroke
    c = clip("takeoff", 2.0)
    c.key(0, merge({}, HALF)).key(0.6, merge(crouch(1.6), torso(16), SPREAD, FLAP_UP)) \
     .key(1.0, merge(root(0, y=6), leg("l", 10, 20), leg("r", 10, 20), FLAP_DOWN, torso(4)), "in") \
     .key(1.5, merge(body(18), FLAP_UP, leg("l", 30, 30), leg("r", 30, 30))).key(2.0, merge(body(30), FLAP_DOWN, leg("l", 40, 20), leg("r", 40, 20)), "in")
    c.impact(1.0, 0.8)
    # land: flare the wings, reach with the legs, slam down, fold
    c = clip("land", 1.8)
    c.key(0, merge(body(30), FLAP_UP)).key(0.6, merge(body(-6), SPREAD, wings(20, -10), leg("l", -30, 10), leg("r", -30, 10))) \
     .key(0.95, merge(crouch(1.5), torso(18), FLAP_DOWN), "in").key(1.8, HALF)
    c.impact(0.95, 1.0)
    attack("takeoff", "fly", rng=(0, 9999), arc="any", events=[[1.0, "takeoff", {"altitude": 70}]], cd=8.0, need=["can_fly", "on_ground"], tags=["fly"])
    attack("land", "fly", rng=(0, 9999), arc="any", events=[[0.6, "land", {}]], cd=2.0, need=["flying"], tags=["fly"])

    # dive from the air: fold, stoop, rake with the feet, pull up
    c = clip("fly_dive_claw", 2.4)
    stoop = merge(body(70), wings(10, 50, 50, 40), leg("l", -20, 30), leg("r", -20, 30), neck(-10))
    rake = merge(body(10), SPREAD, leg("l", -80, 10, 20), leg("r", -80, 10, 20), arm("l", 60, 30, fingers=-20), arm("r", 60, 30, fingers=-20), jaw(40, 1))
    c.key(0, merge(body(58), SPREAD)).key(0.7, stoop).key(1.3, rake, "in").key(1.7, merge(body(30), FLAP_DOWN)).key(2.4, merge(body(55), FLAP_UP))
    c.impact(1.3, 1.0)
    attack("fly_dive_claw", "fly", rng=(0, 260), arc="front", hits=[[1.1, 1.45, FEET["l"] + FEET["r"] + HANDS["l"] + HANDS["r"], "cut", 1.8]],
           move=[[0.6, 1.4, 0, 160]], events=[[0.6, "dive", {"depth": 1.0}], [1.6, "climb", {}]], cd=6.0, need=["flying"], tags=["heavy"])
    # the film's grab and drop: snatch with the feet, climb, let go from on high
    c = clip("fly_grab_drop", 4.6)
    grab = merge(body(10), SPREAD, leg("l", -70, 30, 0, 10), leg("r", -70, 30, 0, 10))
    hold = merge(body(14), leg("l", -60, 60, 40), leg("r", -60, 60, 40))
    c.key(0, merge(body(58), SPREAD)).key(0.9, merge(body(70), wings(10, 40, 40, 30)), "in").key(1.3, grab, "in") \
     .key(1.8, merge(hold, FLAP_DOWN)).key(2.3, merge(hold, FLAP_UP)).key(2.8, merge(hold, FLAP_DOWN)).key(3.3, merge(hold, FLAP_UP)) \
     .key(3.7, merge(body(30), leg("l", -30, 20), leg("r", -30, 20), FLAP_DOWN)).key(4.6, merge(body(58), SPREAD))
    attack("fly_grab_drop", "fly", rng=(0, 200), arc="front", events=[[0.8, "dive", {"depth": 1.0}], [1.3, "grab", {"node": "foot_r", "target": "chest"}],
                                                                     [1.5, "climb", {"height": 60}], [3.7, "drop", {}]],
           move=[[0.7, 1.4, 0, 120]], cd=14.0, need=["flying"], tags=["grapple", "heavy"])


def beams():
    # ---------------------------------------------------------------- micro-oxygen beam from the mouth
    brace = merge(crouch(0.7), torso(10), SPREAD, wings(14), arm("l", 30, 30, elbow=20, fingers=-20), arm("r", 30, 30, elbow=20, fingers=-20), stance_wide(1.2))
    rear = merge(brace, torso(-10), neck(-22, head_pitch=-24), jaw(30, 0.8), {"throat": [-8, 0, 0]})
    fire = merge(crouch(0.7), stance_wide(1.3), torso(14), neck(14, head_pitch=-4), jaw(48, 1), SPREAD, wings(18),
                 arm("l", 26, 34, elbow=24, fingers=-20), arm("r", 26, 34, elbow=24, fingers=-20), tail_curve(0, 20))

    def charge(name, L, pose_end=rear):
        c = clip(name, L)
        c.key(0, HALF).key(L * 0.4, brace).key(L, pose_end, "in")
        c.extra[("scale", "chest")] = {"0": [1, 1, 1], f"{L * 0.6:.2f}": [1, 1, 1], f"{L:.2f}": [1.06, 1.05, 1.07]}
        return c

    def fire_loop(name, pose, extra=None, L=1.0):
        c = clip(name, L, True)
        c.key(0, pose).key(L, pose)
        c.add_fn("head", lambda t: [0.6 * math.sin(t * 100) - 2 * max(0, math.sin(t * 8)), 0.6 * math.sin(t * 77), 0])
        c.add_fn("torso", lambda t: [-1.8 * max(0, math.sin(t * 8)) ** 2, 0, 0])
        for b, f in (extra or {}).items():
            c.add_fn(b, f)
        c.springs = False
        return c

    def release(name, pose):
        c = clip(name, 1.0)
        c.key(0, pose).key(0.4, merge(pose, jaw(-30)), "smooth").key(1.0, HALF)
        return c

    charge("mo_charge", 1.6)
    charge("mo_charge_quick", 0.8)
    fire_loop("mo_fire", fire)
    sweep = lambda a: (lambda t: [0, a * math.sin(t / 2.2 * 2 * math.pi), 0])
    fire_loop("mo_fire_sweep", fire, {"neck1": sweep(18), "neck2": sweep(14), "torso": sweep(16)}, L=2.2)
    release("mo_release", fire)
    air_fire = merge(body(30), neck(-10, head_pitch=10), jaw(48, 1), SPREAD)
    fa = fire_loop("mo_fire_air", air_fire)
    for s, sg in SIDES:
        fa.add_fn(f"wing_{s}", lambda t, sg=sg: [0, 0, sg * (8 + 22 * math.sin(t * 2 * math.pi / 1.0))])

    def beam_attack(id_, ch, fi, re, secs, rng, power, width, style, extra=None, cd=8.0, need=(), tags=()):
        clen = CLIPS[ch].length
        attack(id_, "beam", clip_name=ch, length=clen + secs + CLIPS[re].length, rng=rng, arc="front", cd=cd,
               need=list(need) + ["breath_ready"], tags=["breath", "ranged"] + list(tags),
               phases=[[ch, clen], [fi, secs], [re, CLIPS[re].length]],
               beam=dict({"power": power, "width": width, "style": style, "fire_start": clen, "fire_end": clen + secs}, **(extra or {})))

    beam_attack("micro_oxygen_beam", "mo_charge", "mo_fire", "mo_release", 2.6, (40, 3500), 1.0, 0.9, 1)
    beam_attack("micro_oxygen_burst", "mo_charge_quick", "mo_fire", "mo_release", 0.9, (30, 2000), 0.75, 0.75, 1, cd=5.0)
    beam_attack("micro_oxygen_sweep", "mo_charge", "mo_fire_sweep", "mo_release", 4.4, (40, 2500), 0.8, 0.9, 1, {"sweep": 1}, cd=10.0)
    beam_attack("aerial_micro_oxygen", "mo_charge_quick", "mo_fire_air", "mo_release", 2.2, (40, 3000), 0.9, 0.9, 1, {"pitch": 25}, need=["flying"], tags=["air"])

    # ---------------------------------------------------------------- the laser horn: a cutting beam from the horn tip
    hbrace = merge(crouch(0.5), torso(16), neck(12, head_pitch=14), SPREAD, wings(10), arm("l", 20, 30, elbow=20), arm("r", 20, 30, elbow=20), {"horn1": [-6, 0, 0]})
    c = clip("horn_charge", 1.2)
    c.key(0, HALF).key(1.2, hbrace)
    hfire = merge(hbrace, neck(6, head_pitch=6))
    fire_loop("horn_fire", hfire, {"neck1": sweep(10), "head": lambda t: [0.4 * math.sin(t * 90), 6 * math.sin(t / 1.6 * 2 * math.pi), 0]}, L=1.6)
    release("horn_release", hfire)
    beam_attack("horn_laser", "horn_charge", "horn_fire", "horn_release", 1.6, (40, 900), 1.1, 0.45, 2,
                {"origin": "horn_tip", "kind": "cut", "sweep": 1}, cd=9.0, tags=["sever"])


def specials():
    # horn gore: head down, charge, drive the horn in
    c = swing("horn_gore", 2.2, [(0, HALF), (0.6, merge(HALF, crouch(1.0), torso(20), neck(26, head_pitch=24))),
                                 (1.05, merge(HALF, torso(30), neck(30, head_pitch=26), body(z=-6), leg("l", -24, 30)), "in"),
                                 (1.35, merge(HALF, torso(10), neck(-10, head_pitch=-20)), "out"), (2.2, HALF)], [(1.05, 0.8)])
    attack("horn_gore", "horn", rng=(20, 120), hits=[[0.85, 1.15, ["horn", "head"], "cut", 1.6]], move=[[0.6, 1.1, 0, 120]], cd=4.0, tags=["heavy", "gap_close"])
    c = swing("horn_uppercut", 1.6, [(0, HALF), (0.5, merge(HALF, crouch(0.8), neck(30, head_pitch=30))),
                                     (0.8, merge(HALF, torso(-14), neck(-30, head_pitch=-34), jaw(20)), "in"), (1.6, HALF)], [(0.8, 0.6)])
    attack("horn_uppercut", "horn", rng=(15, 60), hits=[[0.6, 0.85, ["horn", "head", "snout"], "cut", 1.3]], cd=2.5)
    c = swing("horn_slash", 1.4, [(0, HALF), (0.45, merge(HALF, neck(10, 40, head_pitch=10))),
                                  (0.7, merge(HALF, neck(10, -46, head_pitch=10), torso(0, -12)), "in"), (1.4, HALF)])
    attack("horn_slash", "horn", rng=(15, 60), hits=[[0.5, 0.75, ["horn", "head"], "cut", 1.1]], cd=1.6)
    # wing buffet: wings raised high and slammed forward, the gust knocks everything back
    buffet_up = merge(wings(60, 30, -20, -20), torso(-10), neck(-12), jaw(30, 1))
    buffet_down = merge(wings(-20, -60, 20, 30), torso(18), neck(10), crouch(0.6))
    swing("wing_buffet", 2.0, [(0, HALF), (0.7, buffet_up), (1.05, buffet_down, "in"), (2.0, HALF)], [(1.05, 0.8)])
    attack("wing_buffet", "wing", rng=(0, 140), arc="front", hits=[[0.85, 1.15, ["wing_l", "wing_r"], "blunt", 1.2]],
           events=[[1.05, "gust", {"radius": 130, "power": 1.0}]], cd=8.0, tags=["surround"])
    shield = merge(wings(20, -70, 40, 50), neck(14, head_pitch=10), crouch(0.6), arm("l", 60, 0, elbow=60), arm("r", 60, 0, elbow=60))
    swing("wing_shield", 1.8, [(0, HALF), (0.25, shield, "out"), (1.4, shield), (1.8, HALF)])
    attack("wing_shield", "defense", rng=(0, 9999), arc="any", events=[[0.1, "guard", {"secs": 1.4}]], cd=2.0, tags=["defense"])
    # scorpion tail: arch over the shoulder, clamp the neck with the pincer and inject micro-oxygen
    # scorpion: the bend is all in the base of the tail, the rest reaches over the shoulder and forward
    scorp = lambda base, rest: {f"tail{i}": [base if i <= 6 else rest, 0, 0] for i in range(1, 17)}
    arch = merge(HALF, scorp(30, -2), torso(10), crouch(0.6))
    strike = merge(HALF, scorp(33, -7), torso(22), crouch(0.8), body(z=-2))
    c = swing("tail_pincer_grab", 4.0, [(0, HALF), (0.8, merge(arch, {"pincer_a": [0, 40, 0], "pincer_b": [0, -40, 0]})),
                                        (1.15, merge(strike, {"pincer_a": [0, 40, 0], "pincer_b": [0, -40, 0]}), "in"),
                                        (1.3, merge(strike, {"pincer_a": [0, -10, 0], "pincer_b": [0, 10, 0]}), "out"),
                                        (3.0, merge(strike, {"pincer_a": [0, -10, 0], "pincer_b": [0, 10, 0]}, torso(-4))),
                                        (3.4, merge(HALF, tail_curve(-60, 40), {"pincer_a": [0, 40, 0], "pincer_b": [0, -40, 0]}), "in"), (4.0, HALF)])
    c.tail_stiff = 0.8
    attack("tail_pincer_grab", "tail", rng=(25, 70), arc="front", hits=[[1.0, 1.3, ["tail16", "tail15"], "cut", 0.8]],
           events=[[1.3, "grab", {"node": "tail16", "target": "neck"}], [1.8, "pulse_hit", {"power": 0.7, "inject": 1}], [2.3, "pulse_hit", {"power": 0.7, "inject": 1}],
                   [2.8, "pulse_hit", {"power": 0.7, "inject": 1}], [3.4, "release", {"fwd": -20, "side": 160, "up": 10}]], cd=10.0, need=["tail"], tags=["grapple"])
    c = swing("tail_pincer_stab", 2.0, [(0, HALF), (0.7, merge(arch, {"pincer_a": [0, 30, 0], "pincer_b": [0, -30, 0]})),
                                        (1.0, strike, "in"), (2.0, HALF)], [(1.0, 0.5)])
    c.tail_stiff = 0.8
    attack("tail_pincer_stab", "tail", rng=(25, 75), arc="front", hits=[[0.8, 1.1, ["tail16", "tail15", "tail14"], "cut", 1.3]], cd=3.0, need=["tail"])


def build():
    CLIPS.clear()
    ATTACKS.clear()
    moves.locomotion()
    moves.reactions()
    moves.claws()
    moves.tails()
    moves.head_attacks()
    moves.body_attacks()
    moves.leg_attacks()
    moves.grapples()
    moves.world()
    moves.mounted_and_ground()
    moves.defense()
    # every shared clip: wings half folded with a little life; the roar spreads them wide
    for name, c in CLIPS.items():
        if name == "roar":
            for b in WING_BONES:
                v = SPREAD.get(b, [0, 0, 0])
                c.add_fn(b, lambda t, v=v, b=b: [v[0], v[1], v[2] + (25 if b.startswith("wing_") else 0) * (1 if b.endswith("_l") else -1) * min(1, max(0, (t - 0.9) / 0.5)) * min(1, max(0, (4.4 - t) / 0.6))])
        else:
            add_wing_life(c)
    flight()
    beams()
    specials()
    for a in ATTACKS:
        if not a["len"]:
            a["len"] = CLIPS[a["clip"]].length
    return CLIPS, ATTACKS
