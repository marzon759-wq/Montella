"""Vanilla (Java Edition) ModelPart geometry, written the way Minecraft itself builds and draws it.

Coordinates inside a Part follow vanilla: pixels, +y DOWN, the head looks toward -z.
For readability the builder helpers take "up" heights (+up = -y), so box(x, y_bottom, z, w, h, d) is a box whose
bottom face is y_bottom pixels above the part's pivot.
Rotations are vanilla radians applied as R = Rz * Ry * Rx (PoseStack.mulPose(rotationZYX)).
"""
import math
import numpy as np


def rot_zyx(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def mat(R=None, t=(0, 0, 0)):
    m = np.eye(4)
    if R is not None:
        m[:3, :3] = R
    m[:3, 3] = t
    return m


class Cube:
    __slots__ = ("o", "s", "mat", "uv", "part")

    def __init__(self, o, s, material):
        self.o = tuple(float(v) for v in o)      # vanilla origin (min corner)
        self.s = tuple(float(v) for v in s)      # w, h, d
        self.mat = material
        self.uv = None

    def uv_size(self):
        w, h, d = (int(math.ceil(v)) for v in self.s)
        return 2 * d + 2 * w, d + h

    def faces(self):
        """The six quads exactly as ModelPart.Cube builds them: (name, 4 vertices, u1, v1, u2, v2).
        Polygon.remap: v0->(u2,v1) v1->(u1,v1) v2->(u1,v2) v3->(u2,v2)."""
        x0, y0, z0 = self.o
        w, h, d = self.s
        x1, y1, z1 = x0 + w, y0 + h, z0 + d
        V7 = (x0, y0, z0); V0 = (x1, y0, z0); V1 = (x1, y1, z0); V2 = (x0, y1, z0)
        V3 = (x0, y0, z1); V4 = (x1, y0, z1); V5 = (x1, y1, z1); V6 = (x0, y1, z1)
        u, v = self.uv or (0, 0)
        W, H, D = (math.ceil(t) for t in self.s)  # we only use integer sizes
        f4, f5, f6, f7, f8, f9 = u, u + D, u + D + W, u + D + W + W, u + D + W + D, u + D + W + D + W
        f10, f11, f12 = v, v + D, v + D + H
        return [
            ("down", (V4, V3, V7, V0), f5, f10, f6, f11),   # vanilla 'DOWN' = min y = visually the TOP
            ("up", (V1, V2, V6, V5), f6, f11, f7, f10),     # max y = visually the BOTTOM
            ("west", (V7, V3, V6, V2), f4, f11, f5, f12),
            ("north", (V0, V7, V2, V1), f5, f11, f6, f12),
            ("east", (V5, V4, V0, V1), f6, f11, f8, f12),
            ("south", (V3, V4, V5, V6), f8, f11, f9, f12),
        ]


class Part:
    def __init__(self, model, name, parent, pivot_up=(0, 0, 0), rot_deg=(0, 0, 0)):
        self.model, self.name, self.parent = model, name, parent
        x, yu, z = pivot_up
        self.pivot = [float(x), float(-yu), float(z)]
        self.rot = [math.radians(a) for a in rot_deg]
        self.cubes = []
        self.children = []
        if parent is not None:
            parent.children.append(self)
        model.parts.append(self)

    # ---- builders (y given as UP heights) ----
    def box(self, x, y, z, w, h, d, material="skin"):
        w, h, d = (max(1, int(round(v))) for v in (w, h, d))
        c = Cube((x, -(y + h), z), (w, h, d), material)
        c.part = self
        self.cubes.append(c)
        return c

    def cbox(self, cx, cy, cz, w, h, d, material="skin"):
        """box centred on (cx, cy, cz) (cy = up)."""
        w, h, d = (max(1, int(round(v))) for v in (w, h, d))
        return self.box(cx - w / 2, cy - h / 2, cz - d / 2, w, h, d, material)

    def rbox(self, cx, cy, cz, w, h, d, r=2, material="skin", faces="xyz"):
        """rounded box: the union of three boxes, each one 2r shorter on the other two axes. Gives the chunky
        organic silhouette without leaving the cube grammar."""
        w, h, d = (int(round(v)) for v in (w, h, d))
        out = []
        if r <= 0:
            return [self.cbox(cx, cy, cz, w, h, d, material)]
        out.append(self.cbox(cx, cy, cz, w - 2 * r, h - 2 * r, d, material))
        out.append(self.cbox(cx, cy, cz, w, h - 2 * r, d - 2 * r, material))
        out.append(self.cbox(cx, cy, cz, w - 2 * r, h, d - 2 * r, material))
        return out

    def child(self, name, pivot_up=(0, 0, 0), rot_deg=(0, 0, 0)):
        return Part(self.model, name, self, pivot_up, rot_deg)

    def local(self):
        return mat(t=np.array(self.pivot) / 16.0) @ mat(rot_zyx(*self.rot))

    def world(self, pose=None):
        m = self.local() if pose is None else pose(self)
        return m if self.parent is None else self.parent.world(pose) @ m


class Model:
    def __init__(self):
        self.parts = []

    def part(self, name):
        for p in self.parts:
            if p.name == name:
                return p
        raise KeyError(name)

    def cubes(self):
        for p in self.parts:
            yield from p.cubes

    def pack_uv(self, width=1024):
        """shelf-pack every cube's box-UV net into one atlas"""
        cs = sorted(self.cubes(), key=lambda c: (-c.uv_size()[1], -c.uv_size()[0]))
        x = y = shelf = 0
        for c in cs:
            w, h = c.uv_size()
            if x + w > width:
                x, y, shelf = 0, y + shelf, 0
            c.uv = (x, y)
            x += w
            shelf = max(shelf, h)
        height = y + shelf
        th = 64
        while th < height:
            th *= 2
        return width, th

    def to_json(self, tex):
        out = {"texture": list(tex), "parts": []}
        for p in self.parts:
            out["parts"].append({
                "name": p.name, "parent": p.parent.name if p.parent else None,
                "pivot": [round(v, 4) for v in p.pivot], "rot": [round(v, 5) for v in p.rot],
                "cubes": [[c.uv[0], c.uv[1], *[round(v, 4) for v in c.o], *[int(v) for v in c.s]] for c in p.cubes],
            })
        return out
