"""Animation authoring library for the kaiju rig.

A clip is a list of full-body keyframes: (time, pose, ease). A pose is {bone: [rx, ry, rz]} for rotations and
{("pos", bone): [x, y, z]} for offsets; bones not named in a key are at rest in that key. Between keys the value is
eased ("smooth", "in" = accelerate into the key, e.g. a strike reaching contact at full speed, "out" = decelerate,
"linear", "hold" = step).

After keying, a physics pass adds secondary motion so the body reads as heavy:
  * the tail is a chain of damped springs on the segments' world angles; spins and lunges of the body make it lag,
    then whip through and overshoot, and the wave runs down to the tip
  * the neck/head and the arms get a stiffer follow-through
  * impacts (clip.impacts) shake the belly/chest/throat (scale jiggle) and jolt the head

Rotation conventions (bedrock/GeckoLib, model front = -z):
  x: + pitches a forward-pointing part down (jaw opens, head nods, torso leans forward); + raises the tail;
     - raises a hanging arm forward
  y: + turns a forward-pointing part to the model's left (+x); + swings the tail to the right (-x)
  z: + rolls toward the model's right; on a hanging left arm + swings it outward (use side sign sg)
"""
import math
from collections import defaultdict

import numpy as np

DT = 0.05
TAILS = [f"tail{i}" for i in range(1, 17)]
NECK = ["neck1", "neck2", "neck3"]
SIDES = (("l", 1), ("r", -1))


SUB = 20


def spring_step(x, v, target, w, z):
    """one DT of a damped spring, integrated in small semi-implicit substeps so stiff springs stay stable"""
    h = DT / SUB
    for _ in range(SUB):
        a = w * w * (target - x) - 2 * z * w * v
        v = v + a * h
        x = x + v * h
    return x, v


def ease(kind, f):
    f = min(1.0, max(0.0, f))
    if kind == "linear":
        return f
    if kind == "in":
        return f * f * f
    if kind == "out":
        return 1 - (1 - f) ** 3
    if kind == "hold":
        return 0.0 if f < 1 else 1.0
    return f * f * (3 - 2 * f)


def merge(*poses):
    """sum of poses (vectors add)"""
    out = {}
    for p in poses:
        if not p:
            continue
        for k, v in p.items():
            if k in out:
                out[k] = [a + b for a, b in zip(out[k], v)]
            else:
                out[k] = list(v)
    return out


def scale_pose(p, s):
    return {k: [x * s for x in v] for k, v in p.items()}


class Clip:
    def __init__(self, name, length, loop=False):
        self.name = name
        self.length = length
        self.loop = loop
        self.keys = []  # (t, pose, ease)
        self.impacts = []  # (t, strength 0..1) for jiggle
        self.springs = True
        self.tail_stiff = 1.0
        self.fns = {}  # (kind, bone) -> f(t) added on top (procedural loops)
        self.extra = {}  # raw channels written as-is: (kind, bone) -> {time: vec}

    def key(self, t, pose=None, e="smooth"):
        self.keys.append((t, pose or {}, e))
        return self

    def impact(self, t, s=1.0):
        self.impacts.append((t, s))
        return self

    def add_fn(self, bone, f, kind="rotation"):
        self.fns[(kind, bone)] = f
        return self

    # ------------------------------------------------------------------ evaluation
    def channels(self):
        chans = set()
        for _, p, _ in self.keys:
            for k in p:
                chans.add(("position", k[1]) if isinstance(k, tuple) else ("rotation", k))
        chans |= set(self.fns)
        return chans

    def value(self, ch, t):
        kind, bone = ch
        kk = ("pos", bone) if kind == "position" else bone
        keys = sorted(self.keys, key=lambda k: k[0])
        v = [0.0, 0.0, 0.0]
        if keys:
            get = lambda p: p.get(kk, [0.0, 0.0, 0.0])
            if t <= keys[0][0]:
                v = get(keys[0][1])
            elif t >= keys[-1][0]:
                v = get(keys[-1][1])
            else:
                for (t0, p0, _), (t1, p1, e1) in zip(keys, keys[1:]):
                    if t0 <= t <= t1:
                        f = ease(e1, (t - t0) / (t1 - t0) if t1 > t0 else 1)
                        a, b = get(p0), get(p1)
                        v = [x + (y - x) * f for x, y in zip(a, b)]
                        break
        if ch in self.fns:
            v = [a + b for a, b in zip(v, self.fns[ch](t))]
        return v

    def sample(self):
        """-> {(kind, bone): np.array (n, 3)} on the DT grid, with the physics pass applied"""
        n = int(round(self.length / DT)) + 1
        ts = np.arange(n) * DT
        chans = self.channels()
        data = {ch: np.array([self.value(ch, t) for t in ts]) for ch in chans}
        if self.springs:
            self._tail_physics(data, ts)
            self._follow(data, ts, NECK + ["head"], freq=4.5, zeta=0.55, gain=0.35)
            for side, _ in SIDES:
                self._follow(data, ts, [f"forearm_{side}", f"hand_{side}"], freq=5.0, zeta=0.5, gain=0.25)
        self._jiggle(data, ts)
        return ts, data

    def _get(self, data, ch, n):
        if ch not in data:
            data[ch] = np.zeros((n, 3))
        return data[ch]

    def _tail_physics(self, data, ts):
        n = len(ts)
        if n < 3:
            return
        # world angle of the tail root = root + body (+ the keyed tail angles down the chain)
        base = self._get(data, ("rotation", "root"), n)[:, :2] + self._get(data, ("rotation", "body"), n)[:, :2]
        keyed = [self._get(data, ("rotation", b), n)[:, :2].copy() for b in TAILS]
        world_prev_sim = base.copy()
        world_prev_key = base.copy()
        reps = 3 if self.loop else 1
        for i, b in enumerate(TAILS):
            target_world = world_prev_key + keyed[i]  # where the keyed animation puts this segment
            # spring follows (parent's simulated world angle + this segment's keyed local angle)
            drive = world_prev_sim + keyed[i]
            w = 2 * math.pi * (3.4 - 0.13 * i) * self.tail_stiff
            z = 0.42
            x = drive[0].copy()
            v = np.zeros(2)
            out = np.zeros_like(drive)
            for r in range(reps):  # loops: run a couple of periods so the steady state wraps around
                for k in range(n):
                    x, v = spring_step(x, v, drive[k], w, z)
                    out[k] = x
            local = np.clip(out - world_prev_sim, -60, 60)  # a vertebra joint bends only so far
            data[("rotation", b)][:, :2] = local
            out = world_prev_sim + local
            world_prev_sim = out
            world_prev_key = target_world

    def _follow(self, data, ts, bones, freq, zeta, gain):
        """stiff follow-through: each bone adds a lagged copy of its parent chain's angular motion (overshoot)"""
        n = len(ts)
        acc = np.zeros((n, 3))
        for b in bones:
            ch = ("rotation", b)
            if ch not in data:
                continue
            sig = data[ch] + acc
            w = 2 * math.pi * freq
            x = sig[0].copy()
            v = np.zeros(3)
            out = np.zeros_like(sig)
            for k in range(n):
                x, v = spring_step(x, v, sig[k], w, zeta)
                out[k] = x
            lag = np.clip((out - sig) * gain, -25, 25)
            data[ch] = data[ch] + lag
            acc = acc + data[ch] * 0.0

    def _jiggle(self, data, ts):
        if not self.impacts:
            return
        n = len(ts)
        for bone, amp, f in (("belly", 0.05, 7.0), ("chest", 0.03, 6.0), ("throat", 0.08, 8.0)):
            ch = ("scale", bone)
            s = np.ones((n, 3))
            for t0, st in self.impacts:
                d = np.clip(ts - t0, 0, None)
                env = np.where(ts >= t0, np.exp(-d * 6) * np.sin(d * f * 2 * math.pi), 0)
                s[:, 0] += amp * st * env
                s[:, 1] -= amp * 0.6 * st * env
                s[:, 2] += amp * st * env
            data[ch] = s
        hc = ("rotation", "head")
        if hc not in data:
            data[hc] = np.zeros((n, 3))
        for t0, st in self.impacts:
            d = np.clip(ts - t0, 0, None)
            data[hc][:, 0] += np.where(ts >= t0, 6 * st * np.exp(-d * 8) * np.sin(d * 18), 0)

    def to_json(self, fast=None):
        """bedrock keyframes; fast bones (tail, jaw, head, hands) keep every DT sample, the rest every 2nd"""
        ts, data = self.sample()
        fast = fast or set(TAILS) | {"head", "jaw", "neck3", "root", "body", "hand_l", "hand_r", "forearm_l", "forearm_r"}
        bones = defaultdict(dict)
        for (kind, bone), arr in data.items():
            if np.allclose(arr, 1.0 if kind == "scale" else 0.0, atol=1e-3):
                continue
            step = 1 if (bone in fast or kind == "scale" or bone.startswith("plate_")) else 2
            keys = {}
            for k in range(0, len(ts), step):
                keys[f"{ts[k]:.2f}".rstrip("0").rstrip(".") or "0"] = [round(float(x), 2) for x in arr[k]]
            last = f"{ts[-1]:.2f}".rstrip("0").rstrip(".")
            keys[last] = [round(float(x), 2) for x in arr[-1]]
            bones[bone][kind] = keys
        for (kind, bone), keys in self.extra.items():
            bones[bone][kind] = keys
        out = {"animation_length": round(self.length, 3), "bones": dict(bones)}
        out["loop"] = True if self.loop else "hold_on_last_frame"
        return out


# ---------------------------------------------------------------------- reusable body parts
def tail_curve(yaw=0.0, pitch=0.0, wave=0.0, phase=0.0, start=0, end=16, taper=1.0):
    """spread a total bend over tail segments; wave adds an S-shape"""
    out = {}
    m = end - start
    for i in range(start, end):
        f = (i - start) / max(1, m - 1)
        w = (taper ** i)
        out[TAILS[i]] = [pitch / m * w, yaw / m * w + wave * math.sin(f * math.pi * 2 + phase), 0]
    return out


def neck(pitch=0.0, yaw=0.0, roll=0.0, head_pitch=None, head_yaw=None):
    out = {b: [pitch / 3, yaw / 3, roll / 3] for b in NECK}
    out["head"] = [pitch / 3 if head_pitch is None else head_pitch, yaw / 3 if head_yaw is None else head_yaw, 0]
    return out


def arm(side, raise_fwd=0.0, out_=0.0, twist=0.0, elbow=0.0, wrist=0.0, fingers=0.0):
    """raise_fwd: degrees forward/up; out_: abduction; elbow: + flexes the forearm up; fingers: + curls (claw)"""
    sg = 1 if side == "l" else -1
    p = {f"arm_{side}": [-raise_fwd, sg * twist, sg * out_],
         f"forearm_{side}": [-elbow, 0, 0],
         f"hand_{side}": [wrist, 0, 0]}
    if fingers:
        for k in range(4):
            for j in (1, 2, 3):
                p[f"finger{k}_{j}_{side}"] = [fingers * (0.6 + 0.2 * j), 0, 0]
        p[f"thumb_2_{side}"] = [fingers, 0, 0]
    return p


def leg(side, hip=0.0, knee=0.0, ankle=0.0, out_=0.0):
    """hip: + swings the thigh back (x on a hanging leg), knee: + bends (shin back), ankle: + toes down"""
    sg = 1 if side == "l" else -1
    return {f"thigh_{side}": [hip, 0, sg * out_], f"shin_{side}": [knee, 0, 0], f"foot_{side}": [ankle, 0, 0]}


def jaw(open_=0.0, snarl=0.0):
    p = {"jaw": [open_, 0, 0]}
    if snarl:
        p[("pos", "lip_upper")] = [0, snarl, 0]
        p["brow_l"] = [0, 0, 6 * snarl]
        p["brow_r"] = [0, 0, -6 * snarl]
    return p


def body(pitch=0.0, yaw=0.0, roll=0.0, x=0.0, y=0.0, z=0.0):
    p = {"body": [pitch, yaw, roll]}
    if x or y or z:
        p[("pos", "body")] = [x, y, z]
    return p


def root(pitch=0.0, yaw=0.0, roll=0.0, x=0.0, y=0.0, z=0.0):
    p = {"root": [pitch, yaw, roll]}
    if x or y or z:
        p[("pos", "root")] = [x, y, z]
    return p


def torso(pitch=0.0, yaw=0.0, roll=0.0):
    return {"torso": [pitch, yaw, roll]}


def crouch(d=1.0):
    """bend the knees and drop the hips: weight before a big move"""
    return merge(body(y=-2.5 * d), leg("l", -10 * d, 14 * d, -4 * d), leg("r", -10 * d, 14 * d, -4 * d))


def stance_wide(d=1.0):
    return merge(leg("l", 0, 4 * d, 0, 8 * d), leg("r", 0, 4 * d, 0, 8 * d), body(y=-1.2 * d))


REST = {}
