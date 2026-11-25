#!/usr/bin/env python3
"""The living things of the Blood Sea, sculpted as distance fields and painted per voxel (see ckit.py):

  wake       - the Wake Follower: a long pale ribbed body that swims under the blood behind the raft
  gatherer   - the Bone Gatherer: a giant put together from the dead of the Bone Reach, a crown of skulls for a head
  caller     - the Name Caller: a very tall starved thing in the fog with a mouth split down its whole face
  changer    - the Changer: a barrel of flesh with one eye that hides among the raft's cargo
  leviathan  - the Leviathan: a skull the size of a ship, flesh hanging from it, blood pouring from its jaws

Model pixels, 16 to a block, y up, every creature faces -z. y = 0 is the surface of the blood for the things that
stand in it."""
import math, os, sys
import numpy as np
import ckit
from ckit import Creature, fbm, vnoise, ramp
from vox import sphere, ellipsoid, capsule, box, union, smooth_union, cut, chain, arc

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, "../common/src/main/resources/assets/bloodsea")


def smin(a, b, k):
    return smooth_union(a, b, k)


def ssub(a, b, k):
    """smooth subtraction: a minus b"""
    return -smooth_union(-a, b, k)


def P3(s):
    return s.X, s.Y, s.Z


def wet(N):
    """upward and outward faces catch the light: 0..1"""
    return np.clip(N[:, 1] * 0.6 + 0.4, 0, 1)


def blood_drips(P, seed, density=0.5):
    """vertical streaks of blood running down: 0..1 per point"""
    x, y, z = P[:, 0], P[:, 1], P[:, 2]
    col = vnoise(x * 1.0, y * 0.06, z * 1.0, 3.0, seed)
    run = vnoise(x, y * 0.25, z, 6.0, seed + 5)
    return np.clip((col - (1 - density)) * 4, 0, 1) * np.clip(run * 1.6 - 0.2, 0, 1)


# ============================================================================ the Name Caller
def caller():
    """about six blocks of it stand above the blood; arms long enough to rest its hands on the surface either side"""
    def paint(mat, P, ao, N, tag):
        x, y, z = P[:, 0], P[:, 1], P[:, 2]
        n1 = fbm(x, y, z, 7.0, 3)
        n2 = vnoise(x, y, z, 1.6, 4)
        streak = vnoise(x * 1.0, y * 0.12, z * 1.0, 2.2, 6)
        v = 0.2 + 0.55 * n1 + 0.2 * n2 - 0.08 * streak
        skin = ramp(v, [(0, "#1a0304"), (0.35, "#3e0b0b"), (0.6, "#621813"), (0.85, "#8e2c22"), (1, "#b04a38")])
        skin *= (0.55 + 0.45 * ao)[:, None]
        skin += (wet(N) * 18 * np.clip(n2 - 0.55, 0, 1) * 3)[:, None] * np.array([1.0, 0.45, 0.4])
        dark = ramp(0.3 * n1 + 0.3 * n2, [(0, "#0c0101"), (1, "#3a0707")]) * (0.6 + 0.4 * ao)[:, None]
        teeth = ramp(n2, [(0, "#8c7462"), (0.6, "#d2bea4"), (1, "#efe2cc")]) * (0.7 + 0.3 * ao)[:, None]
        nail = ramp(n2, [(0, "#1a1210"), (1, "#4a3a30")])
        out = skin
        out = np.where((mat == 2)[:, None], dark, out)
        out = np.where((mat == 3)[:, None], teeth, out)
        out = np.where((mat == 4)[:, None], nail, out)
        return out

    c = Creature("caller", paint, 1024)
    c.bone("root", (0, 0, 0))
    c.bone("waist", (0, -6, 2), "root")
    c.bone("chest", (0, 36, 2), "waist", rotation=[9, 0, 0])
    c.bone("neck", (0, 74, 2), "chest", rotation=[16, 0, 0])
    c.bone("head", (0, 88, -4), "neck", rotation=[-10, 0, -24])
    c.bone("lip_l", (1.5, 104, -6), "head")
    c.bone("lip_r", (-1.5, 104, -6), "head")

    def torso(s):
        X, Y, Z = P3(s)
        pel = ellipsoid(s, (0, -14, 1), (13, 13, 9))
        ab = capsule(s, (0, -6, 1.5), (0, 38, 2.5), 9.5, 10.5)
        d = smin(pel, ab, 6)
        # the belly sucked in under the ribs, hips jutting
        d = ssub(d, ellipsoid(s, (0, 16, -11.5), (9, 15, 4.5)), 3)
        d = smin(d, union(ellipsoid(s, (9.5, -4, -2), (4, 5, 5)), ellipsoid(s, (-9.5, -4, -2), (4, 5, 5))), 3)
        # spine knuckles down the back
        for k in range(9):
            d = smin(d, sphere(s, (0, -4 + k * 5, 11.0), 2.2), 1.5)
        d = d + (fbm(X, Y, Z, 5, 11) - 0.5) * 1.4
        return d, 1

    def chest(s):
        X, Y, Z = P3(s)
        cage = ellipsoid(s, (0, 53, 2), (16.5, 21, 10.5))
        cage = smin(cage, capsule(s, (0, 36, 2), (0, 52, 2), 10.5, 13), 5)
        # ribs: ridges running down and round toward the breastbone, the skin sunk between them
        ph = Y * 0.62 + np.abs(X) * 0.22 - np.where(Z < 2, 0, (Z - 2) * 0.1)
        rib = (0.5 + 0.5 * np.sin(ph)) ** 3
        inrib = np.clip((Y - 34) / 8, 0, 1) * np.clip((72 - Y) / 6, 0, 1)
        cage = cage + rib * 1.6 * inrib
        sh = capsule(s, (-19, 70, 3), (19, 70, 3), 6.2)
        trap = union(capsule(s, (16, 71, 3), (2, 80, 4), 5.5, 4.5), capsule(s, (-16, 71, 3), (-2, 80, 4), 5.5, 4.5))
        d = smin(cage, smin(sh, trap, 5), 6)
        clav = union(capsule(s, (2.5, 72, -7.5), (17, 71, -3), 2.0), capsule(s, (-2.5, 72, -7.5), (-17, 71, -3), 2.0))
        d = smin(d, clav, 1.5)
        d = smin(d, capsule(s, (0, 70, -8.5), (0, 44, -9.5), 2.0, 1.5), 2)   # breastbone
        for k in range(8):
            d = smin(d, sphere(s, (0, 40 + k * 5, 12.0), 2.3), 1.5)
        # shoulder blades
        d = smin(d, union(ellipsoid(s, (9, 60, 11), (6, 8, 2.5)), ellipsoid(s, (-9, 60, 11), (6, 8, 2.5))), 2)
        d = d + (fbm(X, Y, Z, 5, 12) - 0.5) * 1.2
        return d, 1

    def neck(s):
        X, Y, Z = P3(s)
        d = capsule(s, (0, 72, 3), (0, 90, -4), 5.2, 4.2)
        d = smin(d, union(capsule(s, (2.6, 75, -2), (1.5, 89, -7), 1.6), capsule(s, (-2.6, 75, -2), (-1.5, 89, -7), 1.6)), 1.2)
        d = d + (fbm(X, Y, Z, 4, 13) - 0.5) * 0.8
        return d, 1

    # the head, upright in its own frame (the bone's rest turn tilts it): a long seed split down the front
    def head_shape(s):
        d = ellipsoid(s, (0, 106, -6), (8.8, 20, 9.6))
        d = smin(d, capsule(s, (0, 90, -2), (0, 98, -5), 5.5, 7), 4)
        d = smin(d, ellipsoid(s, (0, 120, -4), (6, 9, 7)), 4)   # the crown runs up into a point
        # brow hollows where eyes should be
        d = ssub(d, union(ellipsoid(s, (5, 110, -15.5), (3.5, 5, 2.5)), ellipsoid(s, (-5, 110, -15.5), (3.5, 5, 2.5))), 2)
        X, Y, Z = P3(s)
        d = d + (fbm(X, Y, Z, 3.5, 14) - 0.5) * 0.9
        return d

    MOUTH = (93, 121)

    def slit(s):
        """the cut down the face: thin at the ends, gaping in the middle"""
        X, Y, Z = P3(s)
        t = np.clip((Y - MOUTH[0]) / (MOUTH[1] - MOUTH[0]), 0, 1)
        half = 0.6 + 2.2 * np.sin(np.pi * t)
        inside = (Y > MOUTH[0]) & (Y < MOUTH[1]) & (Z < -6)
        return np.where(inside, np.abs(X) - half, 50.0)

    def teeth(s, side):
        X, Y, Z = P3(s)
        d = None
        rng = np.random.default_rng(40 + side)
        for k in range(19):
            y = MOUTH[0] + 1.2 + k * 1.42 + rng.uniform(-0.3, 0.3)
            t = (y - MOUTH[0]) / (MOUTH[1] - MOUTH[0])
            half = 0.6 + 2.2 * math.sin(math.pi * t)
            zf = float(front_z(y)) + 1.2
            L = rng.uniform(0.9, 1.9)
            tt = capsule(s, (side * (half + 0.9), y, zf + 0.6), (side * max(0.1, half - L), y - rng.uniform(0.2, 0.9), zf + 0.2), 0.62, 0.12)
            d = tt if d is None else union(d, tt)
        return d

    def front_z(y):
        t = np.clip((y - 106) / 20, -0.999, 0.999)
        return -6 - 9.6 * np.sqrt(1 - t * t)

    def rim(s, side):
        pts = []
        for k in range(15):
            y = MOUTH[0] + k * (MOUTH[1] - MOUTH[0]) / 14
            t = (y - MOUTH[0]) / (MOUTH[1] - MOUTH[0])
            half = 0.6 + 2.2 * math.sin(math.pi * t)
            pts.append((side * (half + 1.3), y, float(front_z(y)) + 0.9))
        return chain(s, pts, 1.7, 1.7)

    def head(s):
        d = head_shape(s)
        X, Y, Z = P3(s)
        core = (Z > -6) | (Y < MOUTH[0] - 1) | (Y > MOUTH[1] + 1)
        d = np.where(core, d, 50.0)
        d = cut(d, slit(s))
        t = np.clip((Y - MOUTH[0]) / (MOUTH[1] - MOUTH[0]), 0, 1)
        half = 0.6 + 2.2 * np.sin(np.pi * t)
        mat = np.where((Z < -1) & (np.abs(X) < half + 1.5) & (Y > MOUTH[0] - 1) & (Y < MOUTH[1] + 1), 2, 1)
        return d, mat

    def lip(side):
        def f(s):
            X, Y, Z = P3(s)
            d = head_shape(s)
            front = (Z <= -6) & (Y >= MOUTH[0] - 1) & (Y <= MOUTH[1] + 1) & (X * side >= 0)
            d = np.where(front, d, 50.0)
            d = smin(d, np.where(front, rim(s, side), 50.0), 1.2)
            d = cut(d, slit(s))
            tt = teeth(s, side)
            mat = np.where(tt < 0, 3, np.where(slit(s) < 0.8, 2, 1))
            return union(d, tt), mat
        return f

    c.part("waist", (-16, -32, -14), (16, 42, 15), 1.0, torso)
    c.part("chest", (-27, 32, -14), (27, 84, 16), 1.0, chest)
    c.part("neck", (-8, 68, -10), (8, 95, 9), 1.0, neck)
    c.part("head", (-11, 84, -18), (11, 131, 6), 0.5, head)
    c.part("lip_l", (-1, 90, -18), (11, 124, -5), 0.5, lip(1))
    c.part("lip_r", (-11, 90, -18), (1, 124, -5), 0.5, lip(-1))

    # arms: long, starved, splayed out to the surface either side
    for side, sx in (("l", 1), ("r", -1)):
        S = (sx * 19, 70, 3)
        E = (sx * 42, 40, -8)
        Wr = (sx * 63, 5, -15)
        c.bone(f"arm_{side}", S, "chest")
        c.bone(f"fore_{side}", E, f"arm_{side}")
        c.bone(f"hand_{side}", Wr, f"fore_{side}")

        def upper(s, S=S, E=E, sx=sx):
            X, Y, Z = P3(s)
            d = capsule(s, S, E, 4.6, 3.0)
            d = smin(d, sphere(s, S, 5.5), 3)
            d = smin(d, sphere(s, E, 3.9), 2)
            d = d + (fbm(X, Y, Z, 3, 21) - 0.5) * 0.8
            return d, 1

        def fore(s, E=E, Wr=Wr, sx=sx):
            X, Y, Z = P3(s)
            d = capsule(s, E, Wr, 3.0, 2.2)
            d = smin(d, capsule(s, (E[0] + sx * 0.5, E[1] - 1.5, E[2] + 1.5), (Wr[0], Wr[1] + 1, Wr[2] + 1.5), 1.6, 1.2), 1)
            d = smin(d, sphere(s, Wr, 2.6), 1)
            d = d + (fbm(X, Y, Z, 3, 22) - 0.5) * 0.7
            # torn skin hanging from the underside of the forearm
            strips = None
            rng = np.random.default_rng(31 if sx > 0 else 32)
            for k in range(7):
                t = 0.15 + k * 0.11 + rng.uniform(-0.03, 0.03)
                p = np.array(E) + (np.array(Wr) - np.array(E)) * t
                L = rng.uniform(5, 13)
                q = p + np.array([sx * rng.uniform(-1.5, 1.5), -L, rng.uniform(-1, 1)])
                st = capsule(s, (p[0], p[1] - 1.5, p[2]), q, 1.1, 0.35)
                strips = st if strips is None else union(strips, st)
            mat = np.where(strips < d, 2, 1)
            return union(d, strips), mat

        def hand(s, Wr=Wr, sx=sx):
            X, Y, Z = P3(s)
            palm = ellipsoid(s, (Wr[0] + sx * 3, Wr[1] - 3, Wr[2] - 4), (5.2, 2.2, 6.0))
            d = smin(palm, capsule(s, Wr, (Wr[0] + sx * 3, Wr[1] - 3, Wr[2] - 3), 2.2), 1.5)
            d = d + (fbm(X, Y, Z, 2.5, 23) - 0.5) * 0.5
            return d, 1

        c.part(f"arm_{side}", tuple(min(S[i], E[i]) - 7 for i in range(3)), tuple(max(S[i], E[i]) + 7 for i in range(3)), 1.0, upper)
        c.part(f"fore_{side}", tuple(min(E[i], Wr[i]) - 6 - (14 if i == 1 else 0) for i in range(3)), tuple(max(E[i], Wr[i]) + 6 for i in range(3)), 1.0, fore)
        c.part(f"hand_{side}", (Wr[0] - 9, Wr[1] - 7, Wr[2] - 12), (Wr[0] + 9, Wr[1] + 4, Wr[2] + 5), 0.5, hand)
        # five long fingers spread on the surface of the blood, each its own bone
        base = np.array([Wr[0] + sx * 3, Wr[1] - 3, Wr[2] - 4])
        for i in range(5):
            ang = math.radians(-60 + i * 30) if i < 4 else math.radians(70)   # four forward-splayed, the thumb out to the side
            dirv = np.array([sx * math.sin(ang), 0, -math.cos(ang)])
            k0 = base + dirv * (4.5 if i < 4 else 3) + np.array([0, -0.3, 0])
            L = [17, 20, 19, 15, 10][i]
            pts = [k0]
            for j in range(3):
                seg = L / 3
                droop = -0.6 - j * 0.9
                pts.append(pts[-1] + dirv * seg + np.array([0, droop, 0]))
            name = f"fing{i}_{side}"
            c.bone(name, tuple(k0), f"hand_{side}")

            def finger(s, pts=pts):
                X, Y, Z = P3(s)
                d = None
                for j in range(3):
                    r0 = 1.6 - j * 0.3
                    seg = capsule(s, tuple(pts[j]), tuple(pts[j + 1]), r0, r0 - 0.3)
                    knuck = sphere(s, tuple(pts[j]), r0 + 0.35)
                    d = smin(seg, knuck, 0.6) if d is None else union(d, smin(seg, knuck, 0.6))
                nail = capsule(s, tuple(pts[2] + (pts[3] - pts[2]) * 0.6), tuple(pts[3] + (pts[3] - pts[2]) * 0.15), 0.8, 0.3)
                mat = np.where(nail < d, 4, 1)
                return union(d, nail), mat
            lo = np.min(pts, 0) - 3
            hi = np.max(pts, 0) + 3
            c.part(name, tuple(lo), tuple(hi), 0.5, finger)
    return c


# ============================================================================ the Bone Gatherer
import gen_props


def local(s, center, radius, fn):
    """evaluate fn only near center (a big speed-up for small detailed things on a large grid)"""
    m = ((s.X - center[0]) ** 2 + (s.Y - center[1]) ** 2 + (s.Z - center[2]) ** 2) < radius * radius
    out = np.full(s.X.shape, 50.0)
    if m.any():
        sub = type(s)(s.X[m], s.Y[m], s.Z[m])
        out[m] = fn(sub)
    return out


def skull_at(s, pos, rot, k, jaw=True, holes=False):
    """a human skull of height about 10*k px, centred near pos, facing -z after rot; with holes, also the distance to
    its eye sockets and nose, to paint them dark"""
    ss = s.moved(t=(pos[0], pos[1] - 5 * k, pos[2]), rot=rot, scale=k)
    d, _ = gen_props.skull(ss, jaw=jaw, eyes=False)
    if not holes:
        return d * k
    h = None
    for sx in (1.8, -1.8):
        a = union(capsule(ss, (sx, 4.7, -7.0), (sx * 0.9, 4.5, -2.6), 1.75, 1.3), sphere(ss, (sx * 1.15, 4.4, -5.6), 1.35))
        h = a if h is None else union(h, a)
    h = union(h, capsule(ss, (0, 3.4, -7.0), (0, 2.8, -3.5), 0.9, 0.7))
    return d * k, h * k


def long_bone(s, a, b, r):
    return gen_props.long_bone(s, a, b, r)


def rib(s, start, outward, length, bend, r0, r1, up=(0, -1, 0), n=8):
    return chain(s, arc(start, outward, up, length, bend, n), r0, r1)


def gatherer():
    """seven blocks of bone wading knee deep: every skull in its crown is a bone of its own that can turn"""
    def paint(mat, P, ao, N, tag):
        x, y, z = P[:, 0], P[:, 1], P[:, 2]
        n1 = fbm(x, y, z, 6.0, 51)
        n2 = vnoise(x, y, z, 1.5, 52)
        grime = fbm(x, y, z, 14.0, 53)
        v = 0.25 + 0.45 * n1 + 0.3 * n2
        bone = ramp(v, [(0, "#4a3a28"), (0.35, "#7e6a4e"), (0.6, "#a8946f"), (0.85, "#c8b693"), (1, "#ddd0b2")])
        dirt = ramp(n2, [(0, "#2a1d12"), (1, "#5a4630")])
        g = np.clip((grime - 0.5) * 3, 0, 0.8)[:, None]
        bone = bone * (1 - g) + dirt * g
        drip = blood_drips(P, 54, 0.45)[:, None]
        blood = ramp(n2, [(0, "#2a0405"), (1, "#6a0e0e")])
        bone = bone * (1 - drip * 0.85) + blood * drip * 0.85
        # blood pools in the low faces and the hollows
        low = np.clip((0.75 - ao) * 2, 0, 1)[:, None]
        bone = bone * (1 - low * 0.5) + blood * low * 0.5
        sinew = ramp(0.5 * n1 + 0.5 * n2, [(0, "#1a0606"), (0.5, "#3d1410"), (1, "#5e261c")])
        out = np.where((mat == 2)[:, None], sinew, bone)
        hole = ramp(n2, [(0, "#060202"), (1, "#1e0a06")])
        out = np.where((mat == 3)[:, None], hole, out)
        # under the blood it is all stained
        sub = np.clip(-y / 6, 0, 1)[:, None]
        out = out * (1 - sub * 0.6) + blood * sub * 0.6
        return out * (0.35 + 0.65 * np.clip(ao, 0, 1.1))[:, None]

    c = Creature("gatherer", paint, 1024)
    c.bone("root", (0, 0, 0))
    c.bone("pelvis", (0, 36, 2), "root")
    c.bone("torso", (0, 46, 2), "pelvis", rotation=[8, 0, 0])
    c.bone("neck", (0, 90, 2), "torso", rotation=[10, 0, 0])
    c.bone("head", (0, 100, -2), "neck")

    def pelvis(s):
        d = ellipsoid(s, (0, 38, 2), (15, 7, 8))
        d = cut(d, ellipsoid(s, (0, 36, 2), (10, 8, 5)))                     # the open basin
        d = union(d, rib(s, (0, 44, 9), (1, 0.2, -0.3), 20, 40, 2.2, 1.6), rib(s, (0, 44, 9), (-1, 0.2, -0.3), 20, 40, 2.2, 1.6))
        d = union(d, local(s, (0, 36, -7), 11, lambda q: skull_at(q, (0, 36, -7), (8, 0, 180), 1.05, jaw=False)))  # a skull upside down in the pelvis
        sin = ellipsoid(s, (0, 40, 3), (11, 6, 6))
        mat = np.where(sin < d, 2, 1)
        return union(d, sin), mat

    def torso(s):
        X, Y, Z = P3(s)
        d = None
        # the spine: vertebrae up the back
        for k in range(10):
            y = 46 + k * 4.6
            z = 9 - 2.5 * math.sin(k / 9 * math.pi)
            v = union(ellipsoid(s, (0, y, z), (3.4, 2.0, 3.0)), capsule(s, (0, y, z + 1), (0, y - 1.5, z + 5.5), 1.0, 0.6),
                      capsule(s, (-4.5, y, z + 1), (4.5, y, z + 1), 0.9))
            d = v if d is None else union(d, v)
        # ribs: seven pairs round from the spine to the front, sagging, some broken
        rng = np.random.default_rng(55)
        for k in range(7):
            y = 84 - k * 5.2
            for sx in (1, -1):
                L = 37 + 7 * math.sin((k + 1) / 8 * math.pi) - (rng.uniform(6, 14) if rng.random() < 0.2 else 0)
                r = rib(s, (sx * 3.5, y, 7), (sx * 1, -0.15, -0.1), L, 150, 2.2, 1.4, up=(0, -0.3, -1))
                d = union(d, r)
                if k < 5:   # a second, crooked cage of someone else's ribs over the first
                    r2 = rib(s, (sx * 7, y - 2.6, 10), (sx * 1, 0.1, -0.25), 40 + rng.uniform(-4, 4), 135 + rng.uniform(-10, 10), 1.7, 1.0, up=(0, -0.2, -1))
                    d = union(d, r2)
        # breastbone, collarbones, shoulder blades
        d = union(d, capsule(s, (0, 86, -11), (0, 60, -12), 2.0, 1.6))
        for sx in (1, -1):
            d = union(d, long_bone(s, (sx * 2, 88, -9), (sx * 25, 90, -1), 1.6))
            d = union(d, ellipsoid(s, (sx * 12, 78, 10), (7, 10, 2)))
            # stray long bones jammed in across the shoulders
            d = union(d, long_bone(s, (sx * 6, 92, 6), (sx * 30, 76, -2), 2.0))
            d = union(d, long_bone(s, (sx * 20, 70, -10), (sx * 8, 54, -9), 1.6))
        # skulls set into the chest and the belly
        for pos, rot, k in (((0, 72, -14), (5, 0, 4), 1.2), ((-11, 83, -11), (0, 20, -8), 1.0), ((12, 62, -12), (6, -18, 10), 1.05),
                            ((-2, 51, -11), (-4, 8, -6), 1.1), ((15, 83, -9), (0, -35, 6), 0.9), ((-15, 64, -10), (0, 25, -10), 0.95),
                            ((sx_ := 27, 95, 0), (0, -80, 0), 0.85), ((-27, 95, 0), (0, 80, 0), 0.85)):
            d = union(d, local(s, pos, 9 + 8 * k, lambda q, pos=pos, rot=rot, k=k: skull_at(q, pos, rot, k)))
        # what holds it together: dark sinew inside the cage
        sin = smin(ellipsoid(s, (0, 68, 0), (21, 22, 12)), capsule(s, (0, 48, 2), (0, 88, 3), 9, 11), 4)
        sin = sin + (fbm(X, Y, Z, 4, 56) - 0.5) * 3
        mat = np.where(sin < d, 2, 1)
        return union(d, sin), mat

    def neck(s):
        d = None
        for k in range(4):
            y = 90 + k * 3.4
            v = union(ellipsoid(s, (0, y, 2 - k), (2.8, 1.6, 2.6)), capsule(s, (0, y, 3 - k), (0, y - 1, 7 - k), 0.8, 0.5))
            d = v if d is None else union(d, v)
        sin = capsule(s, (0, 89, 1), (0, 103, -2), 3.4, 3.0)
        mat = np.where(sin < d, 2, 1)
        return union(d, sin), mat

    c.part("pelvis", (-20, 24, -20), (20, 56, 14), 1.0, pelvis)
    c.part("torso", (-46, 40, -28), (46, 104, 18), 1.0, torso)
    c.part("neck", (-8, 86, -8), (8, 107, 12), 1.0, neck)

    # the crown: skulls packed on a dome round a knot of sinew, every face turned outward
    def crown(s):
        X, Y, Z = P3(s)
        d = ellipsoid(s, (0, 121, 9), (17, 18, 14)) + (fbm(X, Y, Z, 3, 57) - 0.5) * 2.5
        return d, 2
    c.part("head", (-21, 100, -8), (21, 142, 26), 1.0, crown)
    rng = np.random.default_rng(58)
    C, R = np.array([0, 121, 8.0]), 19.0
    layout = [(0, 2, 1.35), (-36, 4, 1.1), (36, 4, 1.1), (-70, 0, 1.0), (70, 0, 1.0), (-20, -30, 1.05), (20, -30, 1.05),
              (0, 32, 1.12), (-40, 33, 1.02), (40, 33, 1.02), (-22, 60, 1.0), (22, 60, 1.0), (0, 84, 0.95)]
    for i, (az, el, k) in enumerate(layout):
        k *= 1.12
        a, e = math.radians(az), math.radians(el)
        dirv = np.array([math.sin(a) * math.cos(e), math.sin(e), -math.cos(a) * math.cos(e)])
        pos = tuple(C + dirv * R)
        rot = (el * 0.45 + rng.uniform(-6, 8), -az * 0.8 + rng.uniform(-8, 8), rng.uniform(-12, 12))
        name = f"sk{i}"
        c.bone(name, pos, "head")

        def sk(s, pos=pos, rot=rot, k=k):
            d, h = skull_at(s, pos, rot, k, holes=True)
            return d, np.where(h < 0.3 * k, 3, 1)
        r = 9.5 * k
        c.part(name, (pos[0] - r, pos[1] - r, pos[2] - r), (pos[0] + r, pos[1] + r, pos[2] + r), 0.6, sk)

    # arms: bundles of long bones, hands of long bones that reach down to the knees
    for side, sx in (("l", 1), ("r", -1)):
        S, E, Wr = (sx * 27, 89, 1), (sx * 38, 60, -6), (sx * 41, 30, -11)
        c.bone(f"arm_{side}", S, "torso")
        c.bone(f"fore_{side}", E, f"arm_{side}")
        c.bone(f"hand_{side}", Wr, f"fore_{side}")

        def upper(s, S=S, E=E, sx=sx):
            d = long_bone(s, S, E, 3.0)
            d = union(d, long_bone(s, (S[0] + sx * 2, S[1] - 2, S[2] + 4), (E[0] + sx * 3, E[1] + 4, E[2] + 3), 1.5))
            d = union(d, long_bone(s, (S[0] - sx * 3, S[1] - 4, S[2] - 3), (E[0] - sx * 2, E[1] + 3, E[2] - 3), 1.3))
            d = union(d, long_bone(s, (S[0] + sx * 1, S[1] - 6, S[2] - 5), (E[0] + sx * 1, E[1] + 6, E[2] - 5), 1.6))
            d = union(d, sphere(s, S, 6.5))
            for k in range(3):   # ribs wrapped round the upper arm
                p = np.array(S) + (np.array(E) - np.array(S)) * (0.25 + k * 0.22)
                d = union(d, rib(s, tuple(p + np.array([0, 0, 4])), (sx, 0, -0.6), 13, 200, 1.1, 0.8, up=(0, 0, -1)))
            sin = capsule(s, S, E, 4.4, 3.4)
            mat = np.where(sin < d, 2, 1)
            return union(d, sin), mat

        def fore(s, E=E, Wr=Wr, sx=sx):
            d = long_bone(s, E, Wr, 2.2)
            d = union(d, long_bone(s, (E[0] + sx * 2.5, E[1], E[2] + 2), (Wr[0] + sx * 2, Wr[1], Wr[2] + 2), 1.7))
            d = union(d, long_bone(s, (E[0] - sx * 2, E[1] - 2, E[2] - 2), (Wr[0] - sx * 1.5, Wr[1] + 3, Wr[2] - 2), 1.4))
            d = union(d, sphere(s, E, 4.6))
            d = union(d, capsule(s, (E[0] - sx * 1, E[1] + 2, E[2] + 4), (E[0] - sx * 2, E[1] + 9, E[2] + 8), 1.4, 0.4))  # elbow spur
            return d, 1

        def hand(s, Wr=Wr, sx=sx):
            d = None
            for k in range(5):  # the carpals, a knot of small bones
                p = (Wr[0] + sx * (k % 2) * 1.6, Wr[1] - 1.5 - k * 1.1, Wr[2] - 0.5 - (k % 3) * 1.2)
                b = ellipsoid(s, p, (1.8, 1.4, 1.6))
                d = b if d is None else union(d, b)
            return d, 1
        c.part(f"arm_{side}", tuple(min(S[i], E[i]) - 10 for i in range(3)), tuple(max(S[i], E[i]) + 10 for i in range(3)), 1.0, upper)
        c.part(f"fore_{side}", tuple(min(E[i], Wr[i]) - 6 for i in range(3)), tuple(max(E[i], Wr[i]) + 10 for i in range(3)), 1.0, fore)
        c.part(f"hand_{side}", (Wr[0] - 5, Wr[1] - 9, Wr[2] - 6), (Wr[0] + 5, Wr[1] + 3, Wr[2] + 4), 1.0, hand)
        # four long fingers and a thumb, curled a little, claws at the tips
        for i in range(5):
            fx = [-2.4, -0.8, 0.8, 2.4, 0][i] * 1.2
            base = np.array([Wr[0] + sx * (2.6 if i == 4 else fx * 0.6), Wr[1] - 6, Wr[2] - 3 - (2.5 if i == 4 else 0) + fx * 0.6])
            L = [26, 30, 29, 24, 14][i]
            dirv = np.array([sx * (0.15 + i * 0.05) if i < 4 else sx * 0.6, -1, -0.12 if i < 4 else -0.6])
            dirv /= np.linalg.norm(dirv)
            name = f"fing{i}_{side}"
            c.bone(name, tuple(base), f"hand_{side}")
            pts = arc(base, dirv, (0, 0, -1), L, 35, 3)

            def finger(s, pts=pts):
                d = None
                for j in range(3):
                    r0 = 1.25 - j * 0.25
                    seg = long_bone(s, tuple(pts[j]), tuple(pts[j + 1]), r0)
                    d = seg if d is None else union(d, seg)
                tip = pts[3] + (pts[3] - pts[2]) * 0.5
                d = union(d, capsule(s, tuple(pts[3]), tuple(tip), 0.9, 0.15))
                return d, 1
            lo = np.min(pts, 0) - 4
            hi = np.max(pts, 0) + 6
            c.part(name, tuple(lo), tuple(hi), 0.75, finger)

    # legs: a femur caged in ribs for a thigh, shins of bundled bone, all of it under the blood below the knee
    for side, sx in (("l", 1), ("r", -1)):
        H, K, F = (sx * 13, 34, 1), (sx * 16, 6, -5), (sx * 16, -26, 0)
        c.bone(f"thigh_{side}", H, "pelvis")
        c.bone(f"shin_{side}", K, f"thigh_{side}")

        def thigh(s, H=H, K=K, sx=sx):
            d = long_bone(s, H, K, 3.2)
            for k in range(5):
                p = np.array(H) + (np.array(K) - np.array(H)) * (0.12 + k * 0.18)
                for ss in (1, -1):
                    d = union(d, rib(s, tuple(p + np.array([0, 0, 6])), (ss, -0.2, -0.3), 21, 190, 1.5, 0.9, up=(0, 0, -1)))
            sin = capsule(s, H, K, 6, 5)
            mat = np.where(sin < d, 2, 1)
            return union(d, sin), mat

        def shin(s, K=K, F=F, sx=sx):
            d = long_bone(s, K, F, 2.6)
            d = union(d, long_bone(s, (K[0] + sx * 3, K[1] - 1, K[2] + 2), (F[0] + sx * 3, F[1], F[2] + 1), 1.6))
            d = union(d, sphere(s, K, 4.5))
            d = union(d, ellipsoid(s, (F[0], F[1] - 1, F[2] - 6), (4, 2, 8)))
            return d, 1
        c.part(f"thigh_{side}", tuple(min(H[i], K[i]) - 12 for i in range(3)), tuple(max(H[i], K[i]) + 12 for i in range(3)), 1.0, thigh)
        c.part(f"shin_{side}", tuple(min(K[i], F[i]) - 8 for i in range(3)), tuple(max(K[i], F[i]) + 8 for i in range(3)), 1.0, shin)
    return c


# ============================================================================ the Wake Follower
def wake():
    """sixteen blocks of pale ribbed body, most of it under the blood; a blunt eyeless head whose underside splits open"""
    Z0, Z1 = -128, 132

    def radius(z):
        t = np.clip((z - Z0) / (Z1 - Z0), 0, 1)
        # blunt head, widest a third of the way back, a long taper to the tail
        prof = np.where(t < 0.3, np.sqrt(np.clip(t / 0.3, 0, 1)) * 0.75 + 0.25 * np.clip(t / 0.3, 0, 1), 1 - (t - 0.3) / 0.7 * 0.82)
        return 19 * prof + 1.5, 13 * prof + 1.2

    def body_d(s):
        X, Y, Z = P3(s)
        a, b = radius(Z)
        yc = -1 + 1.5 * np.sin((Z - Z0) / 60)
        q = np.sqrt((X / a) ** 2 + ((Y - yc) / b) ** 2)
        d = (q - 1) * np.minimum(a, b)
        d = np.where(Z < Z0, np.sqrt(np.maximum(d, 0) ** 2 + (Z0 - Z) ** 2) + np.minimum(d, 0), d)
        d = np.where(Z > Z1, np.sqrt(np.maximum(d, 0) ** 2 + (Z - Z1) ** 2) + np.minimum(d, 0), d)
        # pleats: deep folds running across the back and down the flanks
        ph = Z * 0.74 + np.abs(X) * 0.28 + np.sin(X * 0.15) * 0.6
        top = np.clip((Y - yc + b * 0.5) / (b * 1.2), 0, 1)
        d = d + ((0.5 + 0.5 * np.sin(ph)) ** 3) * 2.6 * top * np.clip((Z - Z0 - 26) / 20, 0, 1)
        # a ridge of spine knuckles down the middle of the back
        sp = capsule(s, (0, 6, Z0 + 30), (0, 2, Z1 - 30), 2.4, 1.2)
        d = smin(d, sp + (0.5 + 0.5 * np.sin(Z * 0.45)) * 1.5, 1.5)
        d = d + (fbm(X, Y, Z, 6, 71) - 0.5) * 0.8
        return d

    def paint(mat, P, ao, N, tag):
        x, y, z = P[:, 0], P[:, 1], P[:, 2]
        n1 = fbm(x, y, z, 9.0, 72)
        n2 = vnoise(x, y, z, 2.0, 73)
        v = 0.3 + 0.45 * n1 + 0.25 * n2
        skin = ramp(v, [(0, "#4e3632"), (0.35, "#8a6c62"), (0.6, "#ad8f82"), (0.85, "#c9aea0"), (1, "#dcc6b6")])
        veins = np.clip(1 - np.abs(vnoise(x, y, z, 7, 74) - 0.5) * 18, 0, 1) * 0.5
        skin = skin * (1 - veins[:, None]) + np.array([110, 40, 44]) * veins[:, None]
        skin *= (0.4 + 0.6 * np.clip(ao, 0, 1.1))[:, None]
        blood = ramp(n2, [(0, "#2c0406"), (1, "#701214")])
        wetline = np.clip((2 - y) / 6, 0, 1)[:, None]            # stained where the blood washes over it
        skin = skin * (1 - wetline * 0.75) + blood * wetline * 0.75
        sheen = (np.clip(N[:, 1], 0, 1) * np.clip(n2 - 0.6, 0, 1) * 160)[:, None] * np.array([1, 0.85, 0.8])
        dark = ramp(n2, [(0, "#0e0203"), (1, "#3c0a0a")])
        tooth = ramp(n2, [(0, "#8a7464"), (1, "#e6d6bc")])
        out = np.where((mat == 2)[:, None], dark, skin + sheen)
        out = np.where((mat == 3)[:, None], tooth, out)
        return out

    c = Creature("wake", paint, 1024)
    c.bone("root", (0, 0, 0))
    cuts = [Z0 + 32, -64, -32, 0, 32, 64, 96]
    parent = "root"
    names = []
    for i, zc in enumerate(cuts):
        n = f"seg{i}"
        c.bone(n, (0, -4, zc), parent)
        names.append(n)
        parent = n
    c.bone("head", (0, -4, cuts[0]), "seg0")
    c.bone("jaw", (0, -8, cuts[0] - 4), "head")
    c.bone("fluke", (0, -4, Z1 - 6), names[-1])

    # body segments: each runs from its pivot to the next one and overlaps it a little, so bends do not open gaps
    bounds = cuts + [Z1 - 6]
    for i, n in enumerate(names):
        za, zb = bounds[i], bounds[i + 1]

        def seg(s, za=za, zb=zb):
            d = body_d(s)
            Z = s.Z
            inner = d + 1.2
            d = np.where((Z >= za) & (Z < zb), d, np.where((Z >= zb) & (Z < zb + 5), inner, 50.0))
            return d, 1
        a, b = radius(np.array([za, zb]))
        R = float(max(a.max(), b.max())) + 8
        c.part(n, (-R, -R - 6, za), (R, R, zb + 5), 1.0, seg)

    # the head: blunt, eyeless, a long slit along the underside that is the mouth
    def head_d(s):
        d = body_d(s)
        X, Y, Z = P3(s)
        return np.where(Z < bounds[0], d, 50.0)

    def head(s):
        d = head_d(s)
        X, Y, Z = P3(s)
        under = Y < -8
        d = np.where(under & (Z < bounds[0] - 4), 50.0, d)   # the lower jaw is its own bone
        d = np.where(Z >= bounds[0] - 4, np.where(Z < bounds[0], d, 50.0), d)
        # teeth hanging down from the upper jaw
        tt = None
        for k in range(12):
            z = Z0 + 4 + k * 2.2
            for sx in (1, -1):
                xr = float(radius(np.array([z]))[0][0]) * 0.72
                t1 = capsule(s, (sx * xr, -7.5, z), (sx * xr * 0.85, -12, z - 0.5), 0.9, 0.2)
                tt = t1 if tt is None else union(tt, t1)
        inner = (Y < -6) & (np.abs(X) < 10)
        mat = np.where(tt < 0, 3, np.where(inner, 2, 1))
        return union(d, tt), mat

    def jaw(s):
        d = head_d(s)
        X, Y, Z = P3(s)
        d = np.where((Y < -8) & (Z < bounds[0] - 4), d, 50.0)
        d = cut(d, ellipsoid(s, (0, -8, Z0 + 14), (12, 3.5, 20)))   # the hollow of the mouth
        mat = np.where(Y > -10, 2, 1)
        return d, mat
    c.part("head", (-24, -24, Z0 - 2), (24, 18, bounds[0] + 2), 1.0, head)
    c.part("jaw", (-24, -24, Z0 - 2), (24, -6, bounds[0]), 1.0, jaw)

    # a fluke at the end of the tail, flat and torn
    def fluke(s):
        X, Y, Z = P3(s)
        d = ellipsoid(s, (0, -4, Z1 + 8), (24, 2.2, 10))
        d = cut(d, ellipsoid(s, (0, -4, Z1 + 20), (9, 6, 9)))
        d = smin(d, capsule(s, (0, -4, Z1 - 6), (0, -4, Z1 + 6), 4, 3), 3)
        d = d + (fbm(X, Y, Z, 3, 75) - 0.5) * 1.5
        return d, 1
    c.part("fluke", (-28, -10, Z1 - 8), (28, 4, Z1 + 20), 1.0, fluke)
    return c


# ============================================================================ the Changer
def changer():
    """a barrel of the raft's cargo that is not a barrel: skin stretched over ribs for staves, one human eye between
    two of them, a hand curled over the rim of its lid, and long fingers folded underneath for legs"""
    H = 21.0
    EYE = np.array([0.0, 11.0, -9.6])

    def rad(y):
        return 8.6 + 1.6 * np.sin(np.pi * np.clip(y / H, 0, 1))

    def paint(mat, P, ao, N, tag):
        x, y, z = P[:, 0], P[:, 1], P[:, 2]
        n1 = fbm(x, y, z, 4.0, 81)
        n2 = vnoise(x, y, z, 0.9, 82)
        ang = np.arctan2(x, -z)
        stave = (np.floor((ang + np.pi) / (2 * np.pi) * 14)).astype(int)
        v = 0.3 + 0.45 * n1 + 0.25 * n2
        flesh = ramp(v, [(0, "#3e1612"), (0.35, "#7a3a30"), (0.6, "#a4594a"), (0.85, "#c48070"), (1, "#d8a090")])
        veins = np.clip(1 - np.abs(vnoise(x, y * 0.6, z, 3, 83) - 0.5) * 16, 0, 1) * 0.55
        flesh = flesh * (1 - veins[:, None]) + np.array([90, 20, 30]) * veins[:, None]
        wood = ramp(0.5 * n1 + 0.5 * vnoise(x * 3, y * 0.2, z * 3, 1.0, 84), [(0, "#2a1e14"), (0.5, "#4e3a28"), (1, "#7a6040")])
        woodstave = np.isin(stave % 14, [1, 3, 5, 8, 10, 12])
        out = np.where(((mat == 1) & woodstave)[:, None], wood, flesh)
        iron = ramp(n2 * 0.6 + n1 * 0.4, [(0, "#1c1410"), (0.5, "#3e2c20"), (0.8, "#6a4024"), (1, "#8a5430")])
        out = np.where((mat == 2)[:, None], iron, out)
        out = np.where((mat == 3)[:, None], wood, out)
        # the eye: sclera, iris, pupil by the angle from where it looks (-z)
        e = P - EYE
        en = e / np.maximum(np.linalg.norm(e, axis=1), 1e-6)[:, None]
        th = np.degrees(np.arccos(np.clip(-en[:, 2], -1, 1)))
        scl = ramp(n2, [(0, "#b8a890"), (1, "#e4dcc8")])
        redv = np.clip(1 - np.abs(vnoise(x * 2, y * 2, z * 2, 1, 85) - 0.5) * 10, 0, 1) * np.clip(th / 60, 0, 1)
        scl = scl * (1 - redv[:, None] * 0.7) + np.array([150, 20, 24]) * redv[:, None] * 0.7
        iris = ramp(n2 * 0.7 + n1 * 0.3, [(0, "#2a1a0c"), (0.5, "#5a3c1c"), (1, "#8a6a34")])
        eye = np.where((th < 30)[:, None], iris, scl)
        eye = np.where((th < 13)[:, None], np.array([[8, 6, 6]]), eye)
        eye = np.where((th < 5) & (e[:, 1] > 0.6), 255, eye) if False else eye
        out = np.where((mat == 4)[:, None], eye, out)
        nail = ramp(n2, [(0, "#3a2420"), (1, "#7a5a48")])
        out = np.where((mat == 5)[:, None], nail, out)
        out = np.where((mat == 6)[:, None], flesh, out)
        blood = ramp(n2, [(0, "#240304"), (1, "#5a0a0c")])
        drip = blood_drips(P * 3, 86, 0.4)[:, None]
        out = np.where((mat == 4)[:, None], out, out * (1 - drip * 0.7) + blood * drip * 0.7)
        return out * (0.4 + 0.6 * np.clip(ao, 0, 1.1))[:, None]

    c = Creature("changer", paint, 1024)
    c.bone("root", (0, 0, 0))
    c.bone("body", (0, 0, 0), "root")
    c.bone("lid", (0, H, 9), "body")
    c.bone("eye", tuple(EYE), "body")
    c.bone("lid_up", tuple(EYE), "body")
    c.bone("lid_down", tuple(EYE), "body")

    def barrel_d(s):
        X, Y, Z = P3(s)
        r = rad(Y)
        d = np.sqrt(X * X + Z * Z) - r
        d = np.maximum(d, np.maximum(-Y, Y - H))
        ang = np.arctan2(X, -Z)
        seam = np.abs(np.sin(ang * 7))
        d = d + np.clip(0.18 - seam, 0, 1) * 2.0          # grooves between the staves
        return d

    def body(s):
        X, Y, Z = P3(s)
        d = barrel_d(s)
        d = d + (fbm(X, Y, Z, 2.5, 87) - 0.5) * 0.5
        # under the skin, ribs push out where the staves should be flat
        ang = np.arctan2(X, -Z)
        d = d - np.clip(np.sin(Y * 0.9 + ang * 2) * 0.5, 0, 1) * 0.6 * (np.abs(ang) > 0.9)
        # the socket of the eye: an almond cut into the front
        e = ellipsoid(s, tuple(EYE + [0, 0, -0.4]), (4.6, 2.7, 3.2))
        d = cut(d, e)
        d = d - 0.4 * np.clip(1 - np.abs(Y - H) / 1.2, 0, 1)
        mat = np.ones(d.shape, np.int16)
        hoops = np.zeros(d.shape, bool)
        for hy in (2.6, 7.0, 15.2, 19.0):
            hoop = np.abs(Y - hy) < 0.9
            hoops |= hoop
        dh = np.where(hoops, barrel_d(s) - 0.55, 50.0)
        dh = np.where(np.abs(np.arctan2(X, -Z)) < 0.55, np.where((Y > 5) & (Y < 17), 50.0, dh), dh)   # the hoop is broken across the eye
        mat = np.where(dh < d + 0.01, 2, mat)
        # wooden straps across the front, nailed over the skin
        strap = union(box(s.moved(t=(-2, 16.5, -10.2), rot=(0, 0, 18)), (0, 0, 0), (8, 1.1, 0.7)),
                      box(s.moved(t=(2.5, 5.5, -10.2), rot=(0, 0, -12)), (0, 0, 0), (7, 1.0, 0.7)))
        strap = np.maximum(strap, np.sqrt(X * X + Z * Z) - rad(Y) - 0.8)
        d = union(d, dh, strap)
        mat = np.where(strap <= d + 0.01, 3, mat)
        return d, mat

    def eye(s):
        d = sphere(s, tuple(EYE), 3.3)
        return d, 4

    def lid_up(s):
        X, Y, Z = P3(s)
        sh = np.maximum(sphere(s, tuple(EYE), 3.95), -sphere(s, tuple(EYE), 3.45))
        d = np.maximum(sh, -(Y - EYE[1] - 1.3))
        d = np.maximum(d, Z - EYE[2] + 0.2)
        d = np.maximum(d, np.abs(X) - 4.2)
        return d, 1

    def lid_down(s):
        X, Y, Z = P3(s)
        sh = np.maximum(sphere(s, tuple(EYE), 3.9), -sphere(s, tuple(EYE), 3.45))
        d = np.maximum(sh, (Y - EYE[1] + 1.7))
        d = np.maximum(d, Z - EYE[2] + 0.2)
        d = np.maximum(d, np.abs(X) - 4.4)
        return d, 1

    def lid(s):
        X, Y, Z = P3(s)
        d = np.maximum(np.sqrt(X * X + Z * Z) - rad(H) - 0.2, np.maximum(H - Y, Y - H - 1.6))
        d = d + np.clip(0.12 - np.abs(np.sin(X * 0.55)), 0, 1) * 1.5     # planks
        return d, 3

    c.part("body", (-12, -1, -12), (12, H + 1, 12), 0.5, body)
    c.part("eye", tuple(EYE - 4), tuple(EYE + 4), 0.25, eye)
    c.part("lid_up", tuple(EYE - 5), tuple(EYE + 5), 0.25, lid_up)
    c.part("lid_down", tuple(EYE - 5), tuple(EYE + 5), 0.25, lid_down)
    c.part("lid", (-12, H - 1, -12), (12, H + 3, 12), 0.5, lid)

    # the hand: out from under the lid at the front right, fingers curled down over the rim
    WR = np.array([4.5, H + 2.6, -4.0])
    c.bone("hand", tuple(WR), "body")

    def hand(s):
        X, Y, Z = P3(s)
        d = capsule(s, (3.5, H - 0.5, 1.0), tuple(WR), 1.7, 1.9)
        d = smin(d, ellipsoid(s, (5.6, H + 2.6, -6.8), (3.8, 1.3, 3.2)), 1.0)
        d = d + (fbm(X, Y, Z, 1.2, 88) - 0.5) * 0.3
        return d, 6
    c.part("hand", (-1, H - 3, -12), (11, H + 6, 4), 0.25, hand)
    for i in range(5):
        thumb = i == 4
        kx = [3.0, 4.9, 6.7, 8.3, 2.4][i]
        k0 = np.array([kx, H + 2.5, -9.0 + (0.4 if i in (0, 3) else 0)]) if not thumb else np.array([2.6, H + 2.0, -5.0])
        name = f"fing{i}"
        c.bone(name, tuple(k0), "hand")
        if thumb:
            pts = [k0, k0 + [-1.8, -0.6, -2.0], k0 + [-2.2, -2.2, -3.4]]
        else:
            L = [3.4, 3.8, 3.6, 2.9][i]
            rimz = -float(rad(H)) - 1.0
            pts = [k0, np.array([kx, H + 2.2, rimz + 0.4]), np.array([kx + 0.2, H - L * 0.8, rimz - 0.2]), np.array([kx + 0.3, H - L * 1.6, rimz + 0.6])]

        def finger(s, pts=pts):
            X, Y, Z = P3(s)
            d = None
            for j in range(len(pts) - 1):
                r0 = 0.95 - j * 0.15
                seg = smin(capsule(s, tuple(pts[j]), tuple(pts[j + 1]), r0, r0 - 0.12), sphere(s, tuple(pts[j]), r0 + 0.2), 0.3)
                d = seg if d is None else union(d, seg)
            tip = pts[-1]
            nl = ellipsoid(s, tuple(tip + (pts[-1] - pts[-2]) * 0.05 + [0, 0, -0.55]), (0.65, 0.9, 0.3))
            mat = np.where(nl < d + 0.05, 5, 6)
            return union(d, nl), mat
        lo = np.min(pts, 0) - 2
        hi = np.max(pts, 0) + 2
        c.part(name, tuple(lo), tuple(hi), 0.25, finger)

    # legs: six long jointed fingers under the barrel, folded flat against its bottom until it runs
    for i in range(6):
        a = math.radians(30 + i * 60)
        base = np.array([math.sin(a) * 6.5, 0.8, -math.cos(a) * 6.5])
        out = np.array([math.sin(a), 0, -math.cos(a)])
        knee = base + out * 7 + np.array([0, 5, 0])
        foot = knee + out * 6 + np.array([0, -6.3, 0])
        n1, n2 = f"leg{i}", f"leg{i}b"
        c.bone(n1, tuple(base), "body")
        c.bone(n2, tuple(knee), n1)

        def l1(s, base=base, knee=knee):
            return smin(capsule(s, tuple(base), tuple(knee), 1.0, 0.8), sphere(s, tuple(knee), 1.1), 0.3), 6

        def l2(s, knee=knee, foot=foot):
            d = capsule(s, tuple(knee), tuple(foot), 0.8, 0.35)
            return d, np.where(capsule(s, tuple(knee + (foot - knee) * 0.8), tuple(foot), 0.9, 0.5) < 0, 5, 6)
        c.part(n1, tuple(np.minimum(base, knee) - 2), tuple(np.maximum(base, knee) + 2), 0.5, l1)
        c.part(n2, tuple(np.minimum(knee, foot) - 2), tuple(np.maximum(knee, foot) + 2), 0.5, l2)
    return c


# ============================================================================ the Leviathan
def leviathan():
    """the living one: the same long skull as the dead leviathan of the east, but with its flesh still on in rags, a neck
    of spined vertebrae behind it, and a jaw that opens wide enough to take a raft whole. Built in units of 8 px."""
    K = 8.0
    HY = 3.0   # the jaw hinge stands this many units above the blood

    def U(s):
        """the space in units, with the hinge at the origin"""
        return type(s)(s.X / K, s.Y / K - HY, s.Z / K)

    def skull_bone(u):
        cran = ellipsoid(u, (0, 7.5, 8), (12.5, 11.5, 12.5))
        snout = capsule(u, (0, 5.0, 2), (0, 3.4, -21), 9.4, 6.2)
        d = smin(cran, snout, 4.5)
        d = union(d, ellipsoid(u, (9.2, 4.5, 6), (3.4, 4.8, 6.8)), ellipsoid(u, (-9.2, 4.5, 6), (3.4, 4.8, 6.8)))
        d = union(d, capsule(u, (6.6, 15.5, 5), (3.6, 12.0, -7), 2.8, 1.5), capsule(u, (-6.6, 15.5, 5), (-3.6, 12.0, -7), 2.8, 1.5))
        d = union(d, capsule(u, (0, 18.2, 2), (0, 14.5, 20), 1.8, 2.8))
        # horns of bone sweeping back from the brow
        for sx in (1, -1):
            d = union(d, chain(u, arc((sx * 7.5, 15, 8), (sx * 0.4, 0.5, 1), (0, -1, 0.3), 18, 50, 6), 3.0, 0.5))
        for sx in (6.0, -6.0):
            d = cut(d, sphere(u, (sx * 1.05, 12.0, 0.5), 4.6))              # the orbits, huge and high
            d = cut(d, ellipsoid(u, (sx * 1.35, 8.5, 13), (2.4, 3.6, 4.8)))
            d = cut(d, sphere(u, (sx * 0.4, 7.4, -21.5), 1.7))
        d = np.maximum(d, -(u.Y - 0.4 + 0.02 * u.Z))   # the palate: nothing of the upper skull below it
        return d

    def upper_teeth(u):
        t = None
        for k in range(10):
            f = k / 9
            for sx in (1, -1):
                z = 4 - f * 24
                x = sx * (8.4 - f * 3.2)
                L = 8.0 + 3.5 * math.sin(f * math.pi) + (4 if k in (7, 8) else 0)
                tip = (x * 0.9, 0.4 - L, z + 1.6)
                a = chain(u, [(x, 1.6, z), (x * 0.97, 0.4 - L * 0.55, z + 0.3), tip], 1.9 if k not in (7, 8) else 2.4, 0.15)
                t = a if t is None else union(t, a)
        return t

    def jaw_bone(u):
        d = union(capsule(u, (8.4, -0.8, 13), (5.0, -2.2, -20), 3.6, 2.6), capsule(u, (-8.4, -0.8, 13), (-5.0, -2.2, -20), 3.6, 2.6),
                  capsule(u, (5.0, -2.2, -20), (-5.0, -2.2, -20), 2.8))
        d = smin(d, union(ellipsoid(u, (7.8, -1, 11), (2.6, 3.5, 4)), ellipsoid(u, (-7.8, -1, 11), (2.6, 3.5, 4))), 1.5)
        return d

    def lower_teeth(u):
        t = None
        for k in range(9):
            f = k / 8
            for sx in (1, -1):
                z = 8 - f * 26
                x = sx * (7.4 - f * 2.4)
                L = 6.5 + 3.0 * math.sin(f * math.pi) + (4 if k == 7 else 0)
                a = chain(u, [(x, -0.4, z), (x * 0.97, -0.4 + L * 0.6, z - 0.4), (x * 0.9, -0.4 + L, z + 0.6)], 1.7 if k != 7 else 2.2, 0.15)
                t = a if t is None else union(t, a)
        return t

    def rags(u, d_bone, seed, amount):
        """flesh still on the bone in patches, thick and torn"""
        n = fbm(u.X, u.Y, u.Z, 7.0, seed)
        fl = d_bone - 1.1 - (n - 0.5) * 2.0 + (vnoise(u.X, u.Y, u.Z, 1.2, seed + 1) - 0.5) * 0.8
        return np.where(n > 1 - amount, fl, 50.0)

    def paint(mat, P, ao, N, tag):
        x, y, z = P[:, 0], P[:, 1], P[:, 2]
        n1 = fbm(x, y, z, 30.0, 91)
        n2 = vnoise(x, y, z, 5.0, 92)
        v = 0.25 + 0.45 * n1 + 0.3 * n2
        bone = ramp(v, [(0, "#1c140c"), (0.35, "#3e3022"), (0.6, "#5e4c3a"), (0.85, "#7e6a52"), (1, "#9a8668")])
        gr = np.clip((fbm(x, y, z, 60.0, 90) - 0.45) * 2.5, 0, 1)[:, None]
        bone = bone * (1 - gr * 0.6) + np.array([60, 20, 16]) * gr * 0.6
        flesh = ramp(v, [(0, "#160404"), (0.4, "#3a0c0a"), (0.7, "#561812"), (1, "#74281e")])
        tooth = ramp(0.6 * n2 + 0.4 * np.clip((y - P[:, 1].min()) / 200, 0, 1), [(0, "#5a4a36"), (0.5, "#bba886"), (1, "#e6dcc4")])
        blood = ramp(n2, [(0, "#3a0406"), (0.6, "#7a0a0c"), (1, "#a01a16")])
        dark = ramp(n2, [(0, "#030101"), (1, "#140404")])
        out = np.where((mat == 2)[:, None], flesh, bone)
        out = np.where((mat == 3)[:, None], tooth, out)
        out = np.where((mat == 4)[:, None], blood, out)
        out = np.where((mat == 5)[:, None], dark, out)
        drip = blood_drips(P * 0.3, 93, 0.5)[:, None]
        out = np.where((mat == 4)[:, None] | (mat == 5)[:, None], out, out * (1 - drip * 0.8) + blood * drip * 0.8)
        out = out + (np.clip(N[:, 1], 0, 1) * np.clip(n2 - 0.62, 0, 1) * 120 * ((mat == 2) | (mat == 4)))[:, None] * np.array([1, 0.6, 0.6])
        return out * (0.3 + 0.7 * np.clip(ao, 0, 1.1))[:, None]

    c = Creature("leviathan", paint, 2048)
    c.bone("root", (0, 0, 0))
    c.bone("neck2", (0, HY * K, 40 * K), "root")
    c.bone("neck1", (0, HY * K, 24 * K), "neck2")
    c.bone("head", (0, HY * K, 13 * K), "neck1")
    c.bone("jaw", (0, HY * K, 12 * K), "head", rotation=[32, 0, 0])

    def head(s):
        u = U(s)
        db = skull_bone(u)
        fl = rags(u, db, 94, 0.58)
        tt = upper_teeth(u)
        # rags of flesh hanging from the palate at the back of the mouth
        rng = np.random.default_rng(95)
        hang = None
        for k in range(16):
            x = rng.uniform(-6.5, 6.5)
            z = rng.uniform(-4, 12)
            L = rng.uniform(3, 10)
            h = capsule(u, (x, 0.6, z), (x + rng.uniform(-0.6, 0.6), 0.6 - L, z + rng.uniform(-0.5, 0.5)), rng.uniform(0.35, 0.7), 0.15)
            hang = h if hang is None else union(hang, h)
        d = union(db, fl, tt, hang)
        mat = np.where(db <= d + 0.01, 1, 2)
        mat = np.where(tt <= d + 0.01, 3, mat)
        mat = np.where(hang <= d + 0.01, 4, mat)
        orb = union(sphere(u, (6.3, 12.0, 0.5), 5.4), sphere(u, (-6.3, 12.0, 0.5), 5.4))
        mat = np.where((orb < 0) & (mat != 3), 5, mat)
        return d * K, mat

    def jaw(s):
        u = U(s)
        db = jaw_bone(u)
        fl = rags(u, db, 96, 0.5)
        tt = lower_teeth(u)
        # blood pouring over the lip of the jaw and down
        rng = np.random.default_rng(97)
        pour = None
        for k in range(22):
            f = rng.uniform(0.05, 0.95)
            sx = 1 if k % 2 else -1
            z = 9 - f * 31
            x = sx * (8.5 - f * 3.0)
            L = rng.uniform(4, 14)
            p = capsule(u, (x, -1.0, z), (x + sx * rng.uniform(0, 0.8), -1.0 - L, z + rng.uniform(-0.5, 0.5)), rng.uniform(0.3, 0.65), 0.2)
            pour = p if pour is None else union(pour, p)
        d = union(db, fl, tt, pour)
        mat = np.where(db <= d + 0.01, 1, 2)
        mat = np.where(tt <= d + 0.01, 3, mat)
        mat = np.where(pour <= d + 0.01, 4, mat)
        return d * K, mat

    def neck(z0, z1, seed):
        def f(s):
            u = U(s)
            X, Y, Z = P3(u)
            body = capsule(u, (0, 1.5, z0), (0, -2.5, z1), 10.5 - (z0 - 13) * 0.06, 10 - (z1 - 13) * 0.06)
            body = body + (fbm(X, Y, Z, 3, seed) - 0.5) * 2.0
            # vertebral arches standing out of the back, curving rearward
            sp = None
            for k in range(int((z1 - z0) / 4)):
                z = z0 + 2 + k * 4
                yb = 1.5 - (z - z0) / (z1 - z0) * 4 + 8.5
                a = chain(u, arc((0, yb, z), (0, 1, 0.25), (0, 0, 1), 9 - k * 0.3, 55, 5), 1.4, 0.3)
                for sx in (1, -1):   # ribs breaking through the flanks
                    a = union(a, chain(u, arc((sx * 8, yb - 4, z), (sx * 1, 0.6, 0.1), (0, -1, 0.2), 9, 70, 5), 1.0, 0.3))
                sp = a if sp is None else union(sp, a)
            d = union(body, sp)
            mat = np.where(sp <= d + 0.01, 1, 2)
            return d * K, mat
        return f

    c.part("head", (-15 * K, (HY - 16) * K, -28 * K), (15 * K, (HY + 28) * K, 25 * K), 4.0, head)
    c.part("jaw", (-14 * K, (HY - 18) * K, -26 * K), (14 * K, (HY + 12) * K, 17 * K), 4.0, jaw)
    c.part("neck1", (-17 * K, (HY - 14) * K, 12 * K), (17 * K, (HY + 21) * K, 26 * K), 4.0, neck(13, 25, 98))
    c.part("neck2", (-17 * K, (HY - 16) * K, 24 * K), (17 * K, (HY + 19) * K, 52 * K), 4.0, neck(24, 50, 99))
    return c


CREATURES = {"leviathan": leviathan, "changer": changer, "wake": wake, "caller": caller, "gatherer": gatherer}

if __name__ == "__main__":
    names = sys.argv[1:] or list(CREATURES)
    for n in names:
        if n != "watcher":
            CREATURES[n]().build(A)


# ============================================================================ the Watcher: the Pale Sun is an eye
def watcher_textures():
    from PIL import Image, ImageDraw, ImageFilter
    out = os.path.join(A, "textures/environment")
    rng = np.random.default_rng(101)
    n = 512
    yy, xx = np.mgrid[0:n, 0:n]
    u, v = (xx + 0.5) / n * 2 - 1, (yy + 0.5) / n * 2 - 1
    r = np.sqrt(u * u + v * v)
    th = np.arctan2(v, u)
    # the eye: sclera, a glowing white iris of radial fibres, a dark limbal ring; the pupil is drawn on its own
    fib = vnoise(np.cos(th) * 40, np.sin(th) * 40, r * 6, 1.0, 102) * 0.6 + vnoise(np.cos(th) * 90, np.sin(th) * 90, r * 12, 1.0, 103) * 0.4
    iris = np.clip(1.15 - r / 0.62, 0, 1) ** 0.6
    col = np.zeros((n, n, 3))
    scl = ramp((vnoise(u * 9, v * 9, 0, 1.0, 104)).ravel(), [(0, "#b8ac98"), (1, "#e8e0cc")]).reshape(n, n, 3)
    irisc = ramp((0.55 * iris + 0.45 * fib).ravel(), [(0, "#8a8070"), (0.4, "#d8d2c0"), (0.75, "#fbf8ee"), (1, "#ffffff")]).reshape(n, n, 3)
    w = np.clip((0.66 - r) / 0.04, 0, 1)[..., None]
    col = scl * (1 - w) + irisc * w
    limb = np.exp(-((r - 0.66) / 0.025) ** 2)[..., None]
    col = col * (1 - limb * 0.8) + np.array([40, 30, 24]) * limb * 0.8
    # red veins crawling in over the white from the rim
    vein = np.zeros((n, n))
    img = Image.new("L", (n, n), 0)
    dr = ImageDraw.Draw(img)
    for k in range(70):
        a = rng.uniform(0, 2 * np.pi)
        rr = 0.99
        x, y = np.cos(a) * rr, np.sin(a) * rr
        wdt = rng.uniform(1.5, 3.5)
        for step in range(40):
            a2 = math.atan2(-y, -x) + rng.uniform(-0.8, 0.8)
            nx, ny = x + math.cos(a2) * 0.012, y + math.sin(a2) * 0.012
            dr.line([((x + 1) / 2 * n, (y + 1) / 2 * n), ((nx + 1) / 2 * n, (ny + 1) / 2 * n)], fill=int(200 * wdt / 3.5), width=max(1, int(wdt)))
            x, y = nx, ny
            wdt *= 0.96
            if math.hypot(x, y) < 0.7:
                break
    vein = np.asarray(img.filter(ImageFilter.GaussianBlur(0.8)), float) / 255
    col = col * (1 - vein[..., None]) + np.array([150, 18, 22]) * vein[..., None]
    alpha = np.clip((1.0 - r) / 0.03, 0, 1)
    Image.fromarray(np.dstack([np.clip(col, 0, 255), alpha * 255]).astype(np.uint8), "RGBA").save(os.path.join(out, "watcher_eye.png"))
    # the pupil: a tall slit
    m = 256
    yy, xx = np.mgrid[0:m, 0:m]
    u, v = (xx + 0.5) / m * 2 - 1, (yy + 0.5) / m * 2 - 1
    half = 0.17 * np.sqrt(np.clip(1 - v * v, 0, 1)) ** 1.3
    a = np.clip((half - np.abs(u)) / 0.03, 0, 1)
    pc = np.zeros((m, m, 3)) + np.array([6, 3, 3])
    Image.fromarray(np.dstack([pc, a * 255]).astype(np.uint8), "RGBA").save(os.path.join(out, "watcher_pupil.png"))
    # the lid: pale wrinkled skin under the eye, seen when it closes
    yy, xx = np.mgrid[0:n, 0:n]
    u, v = (xx + 0.5) / n * 2 - 1, (yy + 0.5) / n * 2 - 1
    r = np.sqrt(u * u + v * v)
    wr = vnoise(u * 3, v * 40, 0, 1.0, 105) * 0.6 + vnoise(u * 20, v * 20, 0, 1.0, 106) * 0.4
    sk = ramp(wr.ravel(), [(0, "#6e5a52"), (0.5, "#a8948a"), (1, "#cbb8ac")]).reshape(n, n, 3)
    crease = np.exp(-(v / 0.04) ** 2)[..., None] * np.clip(1 - np.abs(u), 0, 1)[..., None]
    sk = sk * (1 - crease * 0.85) + np.array([40, 10, 12]) * crease * 0.85
    alpha = np.clip((1.04 - r) / 0.06, 0, 1)
    Image.fromarray(np.dstack([sk, alpha * 255]).astype(np.uint8), "RGBA").save(os.path.join(out, "watcher_lid.png"))
    # the veins: cracks of red across the whole sky around it
    n = 1024
    img = Image.new("L", (n, n), 0)
    dr = ImageDraw.Draw(img)

    def branch(x, y, a, w, depth):
        for step in range(60):
            a += rng.uniform(-0.35, 0.35)
            nx, ny = x + math.cos(a) * 0.012, y + math.sin(a) * 0.012
            dr.line([((x + 1) / 2 * n, (y + 1) / 2 * n), ((nx + 1) / 2 * n, (ny + 1) / 2 * n)], fill=int(min(255, 90 + w * 40)), width=max(1, int(w)))
            x, y = nx, ny
            w *= 0.975
            if depth < 3 and rng.random() < 0.06:
                branch(x, y, a + rng.choice([-1, 1]) * rng.uniform(0.4, 0.9), w * 0.7, depth + 1)
            if abs(x) > 0.98 or abs(y) > 0.98 or w < 0.6:
                return
    for k in range(26):
        a = k / 26 * 2 * math.pi + rng.uniform(-0.1, 0.1)
        branch(math.cos(a) * 0.14, math.sin(a) * 0.14, a, rng.uniform(4, 7), 0)
    vein = np.asarray(img.filter(ImageFilter.GaussianBlur(1.2)), float) / 255
    yy, xx = np.mgrid[0:n, 0:n]
    r = np.sqrt(((xx + 0.5) / n * 2 - 1) ** 2 + ((yy + 0.5) / n * 2 - 1) ** 2)
    fade = np.clip((1 - r) / 0.25, 0, 1)
    vc = np.zeros((n, n, 3)) + np.array([120, 14, 18])
    Image.fromarray(np.dstack([vc, np.clip(vein * fade * 1.3, 0, 1) * 255]).astype(np.uint8), "RGBA").save(os.path.join(out, "watcher_veins.png"))
    print("watcher textures ok")


CREATURES["watcher"] = None

if __name__ == "__main__" and "watcher" in (sys.argv[1:] or ["watcher"]):
    watcher_textures()
