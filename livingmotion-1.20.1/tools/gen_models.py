#!/usr/bin/env python3
"""
Living Motion - GeckoLib geometry + animation generator.

Every model is rebuilt 1:1 from the vanilla Java model (same cube sizes, same box-UV layout) so the
*vanilla textures* (and resource packs that keep the vanilla layout) map onto it unchanged. On top of that
each model gets:
  * an animation-friendly bone hierarchy (root > core > torso ...),
  * face bones: eyelids (blink / squint / angry brows), mouth or fangs,
  * "*_outer" bones for the vanilla overlay layers (stray clothes, drowned outer layer, charged creeper swirl).

Coordinates: Java model parts are given as (pivot, cube offset) like in the vanilla code and converted to Bedrock
geometry (x same, y = 24 - y, z same). Default part rotations carry over 1:1 (GeckoLib's bake flips them the
same way the vanilla renderer's scale(-1,-1,1) does).

Run:  python3 tools/gen_models.py
"""
import json
import math
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'livingmotion')
GEO = os.path.join(ROOT, 'geo')
ANIM = os.path.join(ROOT, 'animations')


# ----------------------------------------------------------------------------------------------------------------
# geometry helpers
# ----------------------------------------------------------------------------------------------------------------
def r(v):
    return round(v, 4)


def jp(p):
    """Java part pivot -> bedrock pivot."""
    return [r(p[0]), r(24 - p[1]), r(p[2])]


def jc(p, off, size, uv, inflate=0.0, mirror=False):
    """Java cube (relative to part pivot p) -> bedrock cube."""
    c = {'origin': [r(p[0] + off[0]), r(24 - (p[1] + off[1] + size[1])), r(p[2] + off[2])],
         'size': [r(s) for s in size], 'uv': list(uv)}
    if inflate:
        c['inflate'] = inflate
    if mirror:
        c['mirror'] = True
    return c


def solid(origin, size, px, rot=None, pivot=None, inflate=0.0):
    """A cube painted with a single texture pixel on every face (eyelids, mouths, fangs, feathers, tufts ...)."""
    face = {'uv': [px[0], px[1]], 'uv_size': [1, 1]}
    c = {'origin': [r(v) for v in origin], 'size': [r(v) for v in size],
         'uv': {k: dict(face) for k in ('north', 'south', 'east', 'west', 'up', 'down')}}
    if rot:
        c['rotation'] = [r(v) for v in rot]
        c['pivot'] = [r(v) for v in (pivot or [origin[0] + size[0] / 2, origin[1], origin[2] + size[2] / 2])]
    if inflate:
        c['inflate'] = inflate
    return c


def bone(name, parent, pivot, cubes=None, rotation=None):
    b = {'name': name, 'pivot': [r(v) for v in pivot]}
    if parent:
        b['parent'] = parent
    if rotation:
        b['rotation'] = [r(v) for v in rotation]
    if cubes:
        b['cubes'] = cubes
    return b


class Face:
    """Face plane of a head cube: front z, left x (column 0 = the mob's right side), top y."""

    def __init__(self, front_z, left_x, top_y):
        self.z, self.x, self.y = front_z, left_x, top_y

    def lid(self, name, parent, col, row, w, h, px, depth=0.15):
        x0 = self.x + col
        y_top = self.y - row
        return bone(name, parent, [x0 + w / 2, y_top, self.z],
                    [solid([x0 - 0.02, y_top - h - 0.02, self.z - depth], [w + 0.04, h + 0.04, depth], px)])

    def mouth(self, name, parent, col, row, w, px, depth=0.15):
        x0 = self.x + col
        y_top = self.y - row
        return bone(name, parent, [x0 + w / 2, y_top, self.z],
                    [solid([x0, y_top - 1, self.z - depth], [w, 1, depth], px)])


def geo(identifier, tw, th, bones):
    return {'format_version': '1.12.0',
            'minecraft:geometry': [{'description': {'identifier': 'geometry.livingmotion.' + identifier,
                                                    'texture_width': tw, 'texture_height': th,
                                                    'visible_bounds_width': 4, 'visible_bounds_height': 3.5,
                                                    'visible_bounds_offset': [0, 1.5, 0]},
                                    'bones': bones}]}


# ----------------------------------------------------------------------------------------------------------------
# models
# ----------------------------------------------------------------------------------------------------------------
def humanoid(skeleton, drowned=False):
    """Zombie family (64x64) or skeleton family (64x32). Same bone names so they share animations.
    Zombie/husk textures leave the left-limb area empty: vanilla HumanoidModel mirrors the right limbs there."""
    H = (0, 0, 0)
    B = []
    B.append(bone('root', None, [0, 0, 0]))
    B.append(bone('core', 'root', [0, 0, 0]))
    B.append(bone('torso', 'core', [0, 12, 0]))
    B.append(bone('body', 'torso', jp(H), [jc(H, (-4, 0, -2), (8, 12, 4), (16, 16))]))
    B.append(bone('head', 'torso', jp(H)))
    B.append(bone('skull', 'head', jp(H), [jc(H, (-4, -8, -4), (8, 8, 8), (0, 0))]))
    face = Face(-4, -4, 32)
    if skeleton:
        # eye sockets have no lids; the jaw drops instead (row 6 is the teeth line)
        B.append(face.mouth('mouth', 'skull', 1, 6, 6, (9, 12)))           # dark socket pixel
    else:
        B.append(face.lid('eyelid_right', 'skull', 1, 4, 2, 1, (11, 11)))   # forehead skin pixel
        B.append(face.lid('eyelid_left', 'skull', 5, 4, 2, 1, (11, 11)))
        B.append(face.mouth('mouth', 'skull', 2, 6, 4, (9, 12)))            # eye pixel = darkest
    RA, LA = (-5, 2, 0), (5, 2, 0)
    if skeleton:
        RL, LL = (-2, 12, 0), (2, 12, 0)
        B.append(bone('right_arm', 'torso', jp(RA), [jc(RA, (-1, -2, -1), (2, 12, 2), (40, 16))]))
        B.append(bone('left_arm', 'torso', jp(LA), [jc(LA, (-1, -2, -1), (2, 12, 2), (40, 16), mirror=True)]))
        B.append(bone('right_leg', 'core', jp(RL), [jc(RL, (-1, 0, -1), (2, 12, 2), (0, 16))]))
        B.append(bone('left_leg', 'core', jp(LL), [jc(LL, (-1, 0, -1), (2, 12, 2), (0, 16), mirror=True)]))
        luv = {'larm': ((40, 16), True), 'lleg': ((0, 16), True)}
    else:
        RL, LL = (-1.9, 12, 0), (1.9, 12, 0)
        B.append(bone('right_arm', 'torso', jp(RA), [jc(RA, (-3, -2, -2), (4, 12, 4), (40, 16))]))
        luv = ({'larm': ((32, 48), False), 'lleg': ((16, 48), False)} if drowned
               else {'larm': ((40, 16), True), 'lleg': ((0, 16), True)})
        B.append(bone('left_arm', 'torso', jp(LA), [jc(LA, (-1, -2, -2), (4, 12, 4), luv['larm'][0], mirror=luv['larm'][1])]))
        B.append(bone('right_leg', 'core', jp(RL), [jc(RL, (-2, 0, -2), (4, 12, 4), (0, 16))]))
        B.append(bone('left_leg', 'core', jp(LL), [jc(LL, (-2, 0, -2), (4, 12, 4), luv['lleg'][0], mirror=luv['lleg'][1])]))
    # overlay layer (stray clothes / drowned outer layer): vanilla uses the plain humanoid mesh inflated by 0.25
    o = 0.25
    B.append(bone('skull_outer', 'skull', jp(H), [jc(H, (-4, -8, -4), (8, 8, 8), (0, 0), o),
                                                   jc(H, (-4, -8, -4), (8, 8, 8), (32, 0), o + 0.5)]))
    B.append(bone('body_outer', 'body', jp(H), [jc(H, (-4, 0, -2), (8, 12, 4), (16, 16), o)]))
    B.append(bone('right_arm_outer', 'right_arm', jp(RA), [jc(RA, (-3, -2, -2), (4, 12, 4), (40, 16), o)]))
    B.append(bone('left_arm_outer', 'left_arm', jp(LA),
                  [jc(LA, (-1, -2, -2), (4, 12, 4), luv['larm'][0], o, luv['larm'][1])]))
    B.append(bone('right_leg_outer', 'right_leg', jp(RL), [jc(RL, (-2, 0, -2), (4, 12, 4), (0, 16), o)]))
    B.append(bone('left_leg_outer', 'left_leg', jp(LL),
                  [jc(LL, (-2, 0, -2), (4, 12, 4), luv['lleg'][0], o, luv['lleg'][1])]))
    return geo('skeleton' if skeleton else ('drowned' if drowned else 'zombie'), 64, 32 if skeleton else 64, B)


def creeper():
    B = [bone('root', None, [0, 0, 0]), bone('core', 'root', [0, 0, 0]), bone('torso', 'core', [0, 6, 0])]
    HP = (0, 6, 0)
    B.append(bone('body', 'torso', jp(HP), [jc(HP, (-4, 0, -2), (8, 12, 4), (16, 16))]))
    B.append(bone('head', 'torso', jp(HP)))
    B.append(bone('skull', 'head', jp(HP), [jc(HP, (-4, -8, -4), (8, 8, 8), (0, 0))]))
    face = Face(-4, -4, 26)
    B.append(face.lid('eyelid_right', 'skull', 1, 2, 2, 2, (11, 11)))
    B.append(face.lid('eyelid_left', 'skull', 5, 2, 2, 2, (11, 11)))
    B.append(face.mouth('mouth', 'skull', 2, 6, 4, (11, 14)))   # black mouth pixel; grows downward = jaw drop
    legs = {'right_hind_leg': (-2, 18, 4), 'left_hind_leg': (2, 18, 4),
            'right_front_leg': (-2, 18, -4), 'left_front_leg': (2, 18, -4)}
    for n, p in legs.items():
        B.append(bone(n, 'core', jp(p), [jc(p, (-2, 0, -2), (4, 6, 4), (0, 16))]))
    # charged creeper swirl (vanilla CreeperPowerLayer: same mesh inflated by 2)
    B.append(bone('skull_outer', 'skull', jp(HP), [jc(HP, (-4, -8, -4), (8, 8, 8), (0, 0), 2.0)]))
    B.append(bone('body_outer', 'body', jp(HP), [jc(HP, (-4, 0, -2), (8, 12, 4), (16, 16), 2.0)]))
    for n, p in legs.items():
        B.append(bone(n + '_outer', n, jp(p), [jc(p, (-2, 0, -2), (4, 6, 4), (0, 16), 2.0)]))
    return geo('creeper', 64, 32, B)


def spider():
    B = [bone('root', None, [0, 0, 0]), bone('core', 'root', [0, 0, 0]), bone('torso', 'core', [0, 9, 0])]
    N = (0, 15, 0)
    A = (0, 15, 9)
    HD = (0, 15, -3)
    B.append(bone('neck', 'torso', jp(N), [jc(N, (-3, -3, -3), (6, 6, 6), (0, 0))]))
    B.append(bone('abdomen', 'torso', jp(A), [jc(A, (-5, -4, -6), (10, 8, 12), (0, 12))]))
    B.append(bone('head', 'torso', jp(HD)))
    B.append(bone('skull', 'head', jp(HD), [jc(HD, (-4, -4, -8), (8, 8, 8), (32, 4))]))
    # chelicerae / fangs under the face (painted with the pale fang pixel of the texture)
    for n, x in (('fang_right', -3), ('fang_left', 2)):
        B.append(bone(n, 'skull', [x + 0.5, 6, -10.5], [solid([x, 3.5, -11.2], [1, 2.5, 1], (41, 18))]))
    hairs = []
    for x, z, h, rx, rz in ((-3.5, 5, 1.5, -20, -15), (2.5, 5.5, 1.3, -15, 15), (-1, 7.5, 1.8, -25, 0),
                            (-4, 10, 1.4, -10, -25), (3, 10.5, 1.6, -5, 20), (-0.5, 12.5, 1.5, 15, 5),
                            (-2.5, 14, 1.2, 30, -10), (1.5, 14.2, 1.2, 30, 10)):
        hairs.append(solid([x, 13, z], [0.5, h, 0.5], (40, 12), rot=[rx, 0, rz], pivot=[x + 0.25, 13, z + 0.25]))
    B.append(bone('bristles', 'abdomen', jp(A), hairs))
    f = 45.0
    legs = [('right_hind_leg', (-4, 15, 2), 45, -f), ('left_hind_leg', (4, 15, 2), -45, f),
            ('right_middle_hind_leg', (-4, 15, 1), 22.5, -33.3), ('left_middle_hind_leg', (4, 15, 1), -22.5, 33.3),
            ('right_middle_front_leg', (-4, 15, 0), -22.5, -33.3), ('left_middle_front_leg', (4, 15, 0), 22.5, 33.3),
            ('right_front_leg', (-4, 15, -1), -45, -f), ('left_front_leg', (4, 15, -1), 45, f)]
    for n, p, ry, rz in legs:
        off = (-15, -1, -1) if n.startswith('right') else (-1, -1, -1)
        B.append(bone(n, 'core', jp(p), [jc(p, off, (16, 2, 2), (18, 0))], rotation=[0, ry, rz]))
    return geo('spider', 64, 32, B)


def quadruped(kind):
    B = [bone('root', None, [0, 0, 0]), bone('core', 'root', [0, 0, 0])]
    if kind == 'cow':
        HP, BP, legh, legs = (0, 4, -8), (0, 5, 2), 12, {
            'right_hind_leg': ((-4, 12, 7), False), 'left_hind_leg': ((4, 12, 7), True),
            'right_front_leg': ((-4, 12, -6), False), 'left_front_leg': ((4, 12, -6), True)}
        B.append(bone('torso', 'core', [0, 13, 0]))
        B.append(bone('body', 'torso', jp(BP), [jc(BP, (-6, -10, -7), (12, 18, 10), (18, 4)),
                                                 jc(BP, (-2, 2, -8), (4, 6, 1), (52, 0))], rotation=[90, 0, 0]))
        B.append(bone('head', 'torso', jp(HP)))
        B.append(bone('skull', 'head', jp(HP), [jc(HP, (-4, -4, -6), (8, 8, 6), (0, 0)),
                                                 jc(HP, (-5, -5, -4), (1, 3, 1), (22, 0)),
                                                 jc(HP, (4, -5, -4), (1, 3, 1), (22, 0))]))
        face = Face(-14, -4, 24)
        B.append(face.lid('eyelid_right', 'skull', 0, 3, 2, 1, (6, 10)))
        B.append(face.lid('eyelid_left', 'skull', 6, 3, 2, 1, (6, 10)))
        # jaw: hangs under the muzzle while chewing / mooing
        B.append(bone('mouth', 'skull', [0, 16, -12.5], [solid([-2, 15, -14], [4, 1, 3], (9, 13))]))
        B.append(bone('ear_right', 'skull', [-4, 21.5, -10.5], [solid([-6.5, 21, -11], [2.5, 1.2, 1], (6, 16), [0, 0, -12], [-4, 21.5, -10.5])]))
        B.append(bone('ear_left', 'skull', [4, 21.5, -10.5], [solid([4, 21, -11], [2.5, 1.2, 1], (6, 16), [0, 0, 12], [4, 21.5, -10.5])]))
        B.append(bone('tail', 'torso', [0, 21.5, 10], [solid([-0.5, 13, 9.6], [1, 8.5, 1], (6, 16)),
                                                       solid([-1, 10.5, 9.1], [2, 3, 2], (8, 16))]))
    else:  # pig
        HP, BP, legh = (0, 12, -6), (0, 11, 2), 6
        legs = {'right_hind_leg': ((-3, 18, 7), False), 'left_hind_leg': ((3, 18, 7), False),
                'right_front_leg': ((-3, 18, -5), False), 'left_front_leg': ((3, 18, -5), False)}
        B.append(bone('torso', 'core', [0, 10, 0]))
        B.append(bone('body', 'torso', jp(BP), [jc(BP, (-5, -10, -7), (10, 16, 8), (28, 8))], rotation=[90, 0, 0]))
        B.append(bone('head', 'torso', jp(HP)))
        B.append(bone('skull', 'head', jp(HP), [jc(HP, (-4, -4, -8), (8, 8, 8), (0, 0)),
                                                 jc(HP, (-2, 0, -9), (4, 3, 1), (16, 16))]))
        face = Face(-14, -4, 16)
        B.append(face.lid('eyelid_right', 'skull', 0, 3, 2, 1, (8, 12)))
        B.append(face.lid('eyelid_left', 'skull', 6, 3, 2, 1, (8, 12)))
        B.append(bone('mouth', 'skull', [0, 8, -12.5], [solid([-2, 7, -14.5], [4, 1, 3], (8, 11))]))
        B.append(bone('ear_right', 'skull', [-3, 16, -11], [solid([-4.3, 15.6, -12.3], [2.6, 0.8, 2.4], (4, 16), [-20, 0, -25], [-3, 16, -11])]))
        B.append(bone('ear_left', 'skull', [3, 16, -11], [solid([1.7, 15.6, -12.3], [2.6, 0.8, 2.4], (4, 16), [-20, 0, 25], [3, 16, -11])]))
        B.append(bone('tail', 'torso', [0, 12.5, 8], [solid([-0.5, 11.5, 8], [1, 1, 1.5], (9, 16)),
                                                      solid([-0.5, 12.5, 9], [1, 1.5, 1], (9, 16)),
                                                      solid([-0.5, 13.5, 8.2], [1, 1, 1], (9, 16)),
                                                      solid([-0.5, 12.6, 7.8], [1, 0.9, 0.6], (9, 16))]))
    for n, (p, m) in legs.items():
        B.append(bone(n, 'core', jp(p), [jc(p, (-2, 0, -2), (4, legh, 4), (0, 16), mirror=m)]))
    return geo(kind, 64, 32, B)


def chicken():
    B = [bone('root', None, [0, 0, 0]), bone('core', 'root', [0, 0, 0]), bone('torso', 'core', [0, 5, 0])]
    HP = (0, 15, -4)
    BP = (0, 16, 0)
    B.append(bone('body', 'torso', jp(BP), [jc(BP, (-3, -4, -3), (6, 8, 6), (0, 9))], rotation=[90, 0, 0]))
    B.append(bone('head', 'torso', jp(HP)))
    B.append(bone('skull', 'head', jp(HP), [jc(HP, (-2, -6, -2), (4, 6, 3), (0, 0))]))
    B.append(bone('beak', 'skull', jp(HP), [jc(HP, (-2, -4, -4), (4, 2, 2), (14, 0))]))
    B.append(bone('wattle', 'skull', [0, 11, -6], [jc(HP, (-1, -2, -3), (2, 2, 2), (14, 4))]))
    face = Face(-6, -2, 15)
    B.append(face.lid('eyelid_right', 'skull', 0, 1, 1, 1, (4, 5)))
    B.append(face.lid('eyelid_left', 'skull', 3, 1, 1, 1, (4, 5)))
    RED, WHITE, LIGHT, GREY, YEL = (16, 4), (13, 9), (6, 9), (6, 13), (36, 4)
    # comb on the crown
    B.append(bone('comb', 'skull', [0, 15, -4.5], [solid([-0.5, 15, -5.6], [1, 1.6, 1.2], RED),
                                                   solid([-0.5, 15, -4.5], [1, 2.1, 1.1], RED),
                                                   solid([-0.5, 15, -3.5], [1, 1.4, 1.0], RED)]))
    # neck ruff and a puffed chest
    B.append(bone('ruff', 'torso', [0, 9, -4.5], [solid([-2.6, 8.4, -6.5], [5.2, 1.8, 4.2], LIGHT),
                                                  solid([-3.2, 6.2, -4.7], [6.4, 4, 1.2], WHITE),
                                                  solid([-2.6, 5.4, -4.4], [5.2, 1.2, 1], LIGHT)]))
    # tail fan
    B.append(bone('tail', 'torso', [0, 10, 3.5], [
        solid([-0.6, 10, 3.2], [1.2, 4.2, 1.2], WHITE, [-35, 0, 0], [0, 10, 3.8]),
        solid([-1.6, 10, 3.2], [1, 3.6, 1], LIGHT, [-30, 0, -22], [-1.1, 10, 3.8]),
        solid([0.6, 10, 3.2], [1, 3.6, 1], LIGHT, [-30, 0, 22], [1.1, 10, 3.8]),
        solid([-2.4, 9.6, 3.1], [0.8, 2.6, 0.9], GREY, [-20, 0, -40], [-2, 9.6, 3.5]),
        solid([1.6, 9.6, 3.1], [0.8, 2.6, 0.9], GREY, [-20, 0, 40], [2, 9.6, 3.5])]))
    for n, p, x0 in (('right_leg', (-2, 19, 1), -3), ('left_leg', (1, 19, 1), 0)):
        B.append(bone(n, 'core', jp(p), [jc(p, (-1, 0, -3), (3, 5, 3), (26, 0)),
                                         solid([x0, 0, -3.6], [0.8, 0.5, 1.8], YEL, [0, -18, 0], [x0 + 0.4, 0, -2]),
                                         solid([x0 + 1.1, 0, -4.0], [0.8, 0.5, 2.2], YEL),
                                         solid([x0 + 2.2, 0, -3.6], [0.8, 0.5, 1.8], YEL, [0, 18, 0], [x0 + 2.6, 0, -2]),
                                         solid([x0 + 1.1, 0, 0.9], [0.8, 0.5, 1.2], YEL)]))
    for n, p, ox, xs in (('right_wing', (-4, 13, 0), 0, -4.25), ('left_wing', (4, 13, 0), -1, 3.25)):
        B.append(bone(n, 'torso', jp(p), [jc(p, (ox, 0, -3), (1, 4, 6), (24, 13)),
                                          solid([xs, 6.2, -1], [1, 1.4, 4.6], GREY),
                                          solid([xs, 5.6, 1], [1, 1.2, 3.2], GREY),
                                          solid([xs, 9.6, -3.4], [1, 1.6, 2.2], WHITE)]))
    return geo('chicken', 64, 32, B)


def sheep():
    """Sheep (sheep.png) + fluffy fleece shell (sheep_fur.png, tinted by wool colour, hidden when sheared)."""
    B = [bone('root', None, [0, 0, 0]), bone('core', 'root', [0, 0, 0]), bone('torso', 'core', [0, 13, 0])]
    HP, BP = (0, 6, -8), (0, 5, 2)
    B.append(bone('body', 'torso', jp(BP), [jc(BP, (-4, -10, -7), (8, 16, 6), (28, 8))], rotation=[90, 0, 0]))
    B.append(bone('head', 'torso', jp(HP)))
    B.append(bone('skull', 'head', jp(HP), [jc(HP, (-3, -4, -6), (6, 6, 8), (0, 0))]))
    face = Face(-14, -3, 22)
    B.append(face.lid('eyelid_right', 'skull', 0, 2, 2, 1, (9, 9)))
    B.append(face.lid('eyelid_left', 'skull', 4, 2, 2, 1, (9, 9)))
    B.append(face.mouth('mouth', 'skull', 2, 5, 2, (8, 10)))
    B.append(bone('ear_right', 'skull', [-3, 21, -10], [solid([-5.2, 20.2, -10.5], [2.4, 1, 1.4], (10, 9), [0, 0, -30], [-3, 21, -10])]))
    B.append(bone('ear_left', 'skull', [3, 21, -10], [solid([2.8, 20.2, -10.5], [2.4, 1, 1.4], (10, 9), [0, 0, 30], [3, 21, -10])]))
    legs = {'right_hind_leg': (-3, 12, 7), 'left_hind_leg': (3, 12, 7), 'right_front_leg': (-3, 12, -5), 'left_front_leg': (3, 12, -5)}
    for n, p in legs.items():
        B.append(bone(n, 'core', jp(p), [jc(p, (-2, 0, -2), (4, 12, 4), (0, 16))]))
    B.append(bone('tail', 'torso', [0, 17, 8], [solid([-1, 13.5, 8], [2, 4, 1.2], (9, 9))]))
    # fleece (vanilla SheepFurModel inflations) + extra tufts so it reads as thick, curly wool
    W = (34, 8)
    B.append(bone('skull_outer', 'skull', jp(HP), [jc(HP, (-3, -4, -4), (6, 6, 6), (0, 0), 0.6),
                                                    solid([-3.2, 21.6, -10.6], [6.4, 1.6, 4.8], W),
                                                    solid([-2.4, 22.6, -9.8], [4.8, 1.0, 3.4], W)]))
    B.append(bone('body_outer', 'body', jp(BP), [jc(BP, (-4, -10, -7), (8, 16, 6), (28, 8), 1.75)]))
    tufts = [([-4.5, 19.4, -8.5], [4, 1.8, 4.5]), ([0.3, 19.6, -6.5], [4.2, 1.6, 5]), ([-4.2, 19.5, -2.5], [4.5, 1.7, 4.5]),
             ([0.5, 19.4, 0.5], [3.8, 1.8, 4.6]), ([-3.6, 19.3, 4], [4, 1.6, 4.2]), ([0.2, 19.5, 5], [3.6, 1.5, 3.8]),
             ([5.3, 12.5, -7.5], [1.4, 5.5, 5]), ([5.4, 13.5, -1.5], [1.4, 5, 5.5]), ([5.3, 12.8, 4], [1.4, 5.4, 4.5]),
             ([-6.7, 13, -7], [1.4, 5.2, 5.2]), ([-6.8, 12.6, -1], [1.4, 5.6, 5]), ([-6.7, 13.4, 4.2], [1.4, 5, 4.4]),
             ([-3.5, 12.5, 9.4], [7, 6, 1.3]), ([-4, 10.5, -10.9], [8, 7, 1.3]), ([-4.5, 9.4, -8], [9, 1.2, 15])]
    B.append(bone('fleece_outer', 'torso', [0, 15, 0], [solid(o, sz, W) for o, sz in tufts]))
    for n, p in legs.items():
        B.append(bone(n + '_outer', n, jp(p), [jc(p, (-2, 0, -2), (4, 6, 4), (0, 16), 0.5)]))
    return geo('sheep', 64, 32, B)


def iron_golem():
    B = [bone('root', None, [0, 0, 0]), bone('core', 'root', [0, 0, 0]), bone('torso', 'core', [0, 16, 0])]
    HP, BP = (0, -7, -2), (0, -7, 0)
    B.append(bone('body', 'torso', jp(BP), [jc(BP, (-9, -2, -6), (18, 12, 11), (0, 40)),
                                             jc(BP, (-4.5, 10, -3), (9, 5, 6), (0, 70), 0.5)]))
    B.append(bone('head', 'torso', jp(HP)))
    B.append(bone('skull', 'head', jp(HP), [jc(HP, (-4, -12, -5.5), (8, 10, 8), (0, 0))]))
    B.append(bone('nose', 'skull', [0, 34, -8], [jc(HP, (-1, -5, -7.5), (2, 4, 2), (24, 0))]))
    face = Face(-7.5, -4, 43)
    B.append(face.lid('eyelid_right', 'skull', 1, 5, 2, 2, (11, 11)))
    B.append(face.lid('eyelid_left', 'skull', 5, 5, 2, 2, (11, 11)))
    B.append(bone('brow', 'skull', [0, 38.5, -7.5], [solid([-4, 38, -7.75], [8, 1, 0.3], (8, 12))]))
    RA, LA = (-11, -7, 0), (11, -7, 0)
    B.append(bone('right_arm', 'torso', jp(RA), [jc((0, -7, 0), (-13, -2.5, -3), (4, 30, 6), (60, 21))]))
    B.append(bone('left_arm', 'torso', jp(LA), [jc((0, -7, 0), (9, -2.5, -3), (4, 30, 6), (60, 58))]))
    RL, LL = (-4, 11, 0), (5, 11, 0)
    B.append(bone('right_leg', 'core', jp(RL), [jc(RL, (-3.5, -3, -3), (6, 16, 5), (37, 0))]))
    B.append(bone('left_leg', 'core', jp(LL), [jc(LL, (-3.5, -3, -3), (6, 16, 5), (60, 0), mirror=True)]))
    # a few hanging vines on the shoulders (vine pixels of the texture)
    B.append(bone('vines', 'body', jp(BP), [solid([-8.5, 24, -6.3], [1, 8, 0.4], (12, 41)), solid([6.5, 26, -6.3], [1, 6, 0.4], (12, 41)),
                                             solid([-9.3, 27, -3], [0.4, 5, 1], (12, 41))]))
    return geo('iron_golem', 128, 128, B)


def enderman():
    B = [bone('root', None, [0, 0, 0]), bone('core', 'root', [0, 0, 0]), bone('torso', 'core', [0, 29, 0])]
    HP, BP = (0, -13, 0), (0, -14, 0)
    B.append(bone('body', 'torso', jp(BP), [jc(BP, (-4, 0, -2), (8, 12, 4), (32, 16))]))
    B.append(bone('head', 'torso', jp(HP)))
    B.append(bone('jaw', 'head', jp(HP), [solid([-3.6, 37.4, -3.6], [7.2, 7.2, 7.2], (8, 8))]))   # dark mouth cavity revealed when the head splits
    B.append(bone('skull', 'head', jp(HP), [jc(HP, (-4, -8, -4), (8, 8, 8), (0, 0))]))
    face = Face(-4, -4, 45)
    B.append(face.lid('eyelid_right', 'skull', 0, 4, 3, 1, (8, 8)))
    B.append(face.lid('eyelid_left', 'skull', 5, 4, 3, 1, (8, 8)))
    for n, p, m in (('right_arm', (-5, -12, 0), False), ('left_arm', (5, -12, 0), True)):
        B.append(bone(n, 'torso', jp(p), [jc(p, (-1, -2, -1), (2, 30, 2), (56, 0), mirror=m)]))
    for n, p, m in (('right_leg', (-2, -5, 0), False), ('left_leg', (2, -5, 0), True)):
        B.append(bone(n, 'core', jp(p), [jc(p, (-1, 0, -1), (2, 30, 2), (56, 0), mirror=m)]))
    return geo('enderman', 64, 32, B)


def villager():
    """Villager / wandering trader (64x64). Profession, biome and level overlays re-render the same mesh."""
    B = [bone('root', None, [0, 0, 0]), bone('core', 'root', [0, 0, 0]), bone('torso', 'core', [0, 12, 0])]
    H = (0, 0, 0)
    B.append(bone('body', 'torso', jp(H), [jc(H, (-4, 0, -3), (8, 12, 6), (16, 20)),
                                            jc(H, (-4, 0, -3), (8, 20, 6), (0, 38), 0.5)]))
    B.append(bone('head', 'torso', jp(H)))
    B.append(bone('skull', 'head', jp(H), [jc(H, (-4, -10, -4), (8, 10, 8), (0, 0)),
                                            jc(H, (-4, -10, -4), (8, 10, 8), (32, 0), 0.51)]))
    B.append(bone('hat_rim', 'skull', [0, 24, 0], [jc(H, (-8, -8, -6), (16, 16, 1), (30, 47))], rotation=[-90, 0, 0]))
    B.append(bone('nose', 'skull', [0, 26, -4], [jc((0, -2, 0), (-1, -1, -6), (2, 4, 2), (24, 0))]))
    face = Face(-4, -4, 34)
    B.append(face.lid('eyelid_right', 'skull', 1, 6, 2, 1, (8, 8)))
    B.append(face.lid('eyelid_left', 'skull', 5, 6, 2, 1, (8, 8)))
    B.append(bone('brow', 'skull', [0, 28.5, -4], [solid([-3, 28, -4.18], [6, 1, 0.2], (9, 13))]))
    B.append(face.mouth('mouth', 'skull', 2, 8, 4, (9, 13)))
    AP = (0, 3, -1)
    B.append(bone('arms', 'torso', jp(AP), [jc(AP, (-8, -2, -2), (4, 8, 4), (44, 22)),
                                             jc(AP, (4, -2, -2), (4, 8, 4), (44, 22), mirror=True),
                                             jc(AP, (-4, 2, -2), (8, 4, 4), (40, 38))], rotation=[-42.97, 0, 0]))
    for n, p, m in (('right_leg', (-2, 12, 0), False), ('left_leg', (2, 12, 0), True)):
        B.append(bone(n, 'core', jp(p), [jc(p, (-2, 0, -2), (4, 12, 4), (0, 22), mirror=m)]))
    return geo('villager', 64, 64, B)


# ----------------------------------------------------------------------------------------------------------------
# animation helpers
# ----------------------------------------------------------------------------------------------------------------
def ts(t):
    return ('%.4f' % t).rstrip('0').rstrip('.') if t else '0.0'


def wave(length, steps, fn):
    """Sample fn(phase 0..1) -> [x,y,z] into keyframes covering one loop (first == last)."""
    out = {}
    for i in range(steps + 1):
        p = i / steps
        out[ts(p * length)] = [r(v) for v in fn(p)]
    return out


def keys(*pairs):
    """keys((t, [x,y,z]), ...) - plain linear keyframes."""
    return {ts(t): [r(v) for v in val] for t, val in pairs}


def S(p, k=1.0, ph=0.0):
    return math.sin((p * k + ph) * 2 * math.pi)


def C(p, k=1.0, ph=0.0):
    return math.cos((p * k + ph) * 2 * math.pi)


def anim(length, loop, bones):
    a = {'animation_length': length, 'bones': bones}
    if loop is True:
        a['loop'] = True
    elif loop == 'hold':
        a['loop'] = 'hold_on_last_frame'
    return a


def faster(a, f):
    """Time-scale an animation (death poses must land before vanilla removes the body at 20 ticks)."""
    out = {'animation_length': r(a['animation_length'] * f), 'bones': {}}
    if 'loop' in a:
        out['loop'] = a['loop']
    for b, chans in a['bones'].items():
        out['bones'][b] = {ch: {ts(float(t) * f): v for t, v in kf.items()} for ch, kf in chans.items()}
    return out


def write_anims(name, anims):
    if 'death' in anims:
        anims = dict(anims)
        anims['death'] = faster(anims['death'], 0.62)
    data = {'format_version': '1.8.0', 'animations': {'animation.' + name + '.' + k: v for k, v in anims.items()}}
    with open(os.path.join(ANIM, name + '.animation.json'), 'w') as f:
        json.dump(data, f, indent=1)


# Shared reactions --------------------------------------------------------------------------------------------
def hurt_humanoid():
    return anim(0.45, False, {
        'core': {'rotation': keys((0, [0, 0, 0]), (0.07, [-11, 0, 0]), (0.2, [-6, 0, 0]), (0.45, [0, 0, 0])),
                 'position': keys((0, [0, 0, 0]), (0.07, [0, 0.4, 1.2]), (0.45, [0, 0, 0]))},
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.06, [-18, 6, 9]), (0.22, [-6, -3, -4]), (0.45, [0, 0, 0]))},
        'right_arm': {'rotation': keys((0, [0, 0, 0]), (0.08, [-25, 0, 22]), (0.45, [0, 0, 0]))},
        'left_arm': {'rotation': keys((0, [0, 0, 0]), (0.08, [-25, 0, -22]), (0.45, [0, 0, 0]))},
    })


def death_humanoid():
    return anim(1.0, 'hold', {
        'core': {'rotation': keys((0, [0, 0, 0]), (0.25, [-12, 0, 4]), (0.5, [-8, 0, 3]), (0.95, [-90, 0, 4]),
                                  (1.0, [-88, 0, 4])),
                 'position': keys((0, [0, 0, 0]), (0.5, [0, -1.5, 0]), (0.95, [0, 2.2, 0]), (1.0, [0, 2, 0]))},
        'right_leg': {'rotation': keys((0, [0, 0, 0]), (0.45, [-35, 0, 0]), (0.9, [-5, 0, 6]))},
        'left_leg': {'rotation': keys((0, [0, 0, 0]), (0.45, [-30, 0, 0]), (0.9, [-10, 0, -6]))},
        'right_arm': {'rotation': keys((0, [0, 0, 0]), (0.3, [-150, 0, 25]), (0.9, [-170, 0, 40]))},
        'left_arm': {'rotation': keys((0, [0, 0, 0]), (0.35, [-140, 0, -20]), (0.9, [-165, 0, -35]))},
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.3, [-25, 0, 10]), (0.95, [10, 15, 20]))},
    })


# ----------------------------------------------------------------------------------------------------------------
# humanoid animations (zombie family + skeleton family)
# ----------------------------------------------------------------------------------------------------------------
def humanoid_anims():
    A = {}
    # zombie: arms held forward, slouched, swaying
    A['zombie_idle'] = anim(3.2, True, {
        'torso': {'rotation': wave(3.2, 16, lambda p: [4 + 1.6 * S(p), 2 * S(p, 1, 0.3), 1.5 * S(p, 1, 0.1)])},
        'right_arm': {'rotation': wave(3.2, 16, lambda p: [-82 + 3 * S(p, 1, 0.2), 4 * S(p), 4 + 2 * C(p)])},
        'left_arm': {'rotation': wave(3.2, 16, lambda p: [-80 + 3 * S(p, 1, 0.45), -4 * S(p), -4 - 2 * C(p, 1, .2)])},
        'skull': {'rotation': wave(3.2, 16, lambda p: [3 * S(p, 1, 0.25), 0, 6 * S(p)])},
        'core': {'position': wave(3.2, 8, lambda p: [0, 0.15 * S(p, 2), 0])},
    })
    A['zombie_walk'] = anim(1.2, True, {
        'right_leg': {'rotation': wave(1.2, 12, lambda p: [32 * C(p), 0, 0])},
        'left_leg': {'rotation': wave(1.2, 12, lambda p: [-32 * C(p), 0, 0])},
        'right_arm': {'rotation': wave(1.2, 12, lambda p: [-84 - 7 * C(p), 3 * S(p), 5])},
        'left_arm': {'rotation': wave(1.2, 12, lambda p: [-84 + 7 * C(p), -3 * S(p), -5])},
        'torso': {'rotation': wave(1.2, 12, lambda p: [7, 6 * S(p), 4 * S(p)])},
        'core': {'position': wave(1.2, 12, lambda p: [0, 0.7 * abs(S(p)), 0])},
        'skull': {'rotation': wave(1.2, 12, lambda p: [-3, 0, 5 * S(p, 1, 0.15)])},
    })
    A['zombie_chase'] = anim(0.8, True, {
        'right_leg': {'rotation': wave(0.8, 12, lambda p: [42 * C(p), 0, 0])},
        'left_leg': {'rotation': wave(0.8, 12, lambda p: [-42 * C(p), 0, 0])},
        'right_arm': {'rotation': wave(0.8, 12, lambda p: [-100 - 14 * C(p), 6 * S(p), 8])},
        'left_arm': {'rotation': wave(0.8, 12, lambda p: [-100 + 14 * C(p), -6 * S(p), -8])},
        'torso': {'rotation': wave(0.8, 12, lambda p: [16, 8 * S(p), 3 * S(p)])},
        'core': {'position': wave(0.8, 12, lambda p: [0, 1.0 * abs(S(p)), 0])},
        'skull': {'rotation': wave(0.8, 12, lambda p: [-14, 0, 4 * S(p)])},
    })
    A['zombie_attack'] = anim(0.5, False, {
        'right_arm': {'rotation': keys((0, [-90, 0, 5]), (0.14, [-155, 18, 12]), (0.27, [-45, -22, -6]),
                                       (0.5, [-88, 0, 5]))},
        'left_arm': {'rotation': keys((0, [-90, 0, -5]), (0.2, [-150, -18, -12]), (0.34, [-50, 22, 6]),
                                      (0.5, [-88, 0, -5]))},
        'torso': {'rotation': keys((0, [6, 0, 0]), (0.14, [-6, 12, 0]), (0.3, [20, -12, 0]), (0.5, [6, 0, 0]))},
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.14, [-15, 0, 0]), (0.3, [12, 0, 0]), (0.5, [0, 0, 0]))},
    })
    # skeleton: arms down, bony rattle
    A['skeleton_idle'] = anim(2.6, True, {
        'torso': {'rotation': wave(2.6, 13, lambda p: [1.5 * S(p), 1.5 * S(p, 1, 0.3), 0])},
        'right_arm': {'rotation': wave(2.6, 13, lambda p: [3 * S(p), 0, 4 + 2.5 * C(p)])},
        'left_arm': {'rotation': wave(2.6, 13, lambda p: [-3 * S(p), 0, -4 - 2.5 * C(p)])},
        'skull': {'rotation': wave(2.6, 13, lambda p: [0, 0, 4 * S(p, 1, 0.1)])},
    })
    A['skeleton_walk'] = anim(1.0, True, {
        'right_leg': {'rotation': wave(1.0, 12, lambda p: [30 * C(p), 0, 0])},
        'left_leg': {'rotation': wave(1.0, 12, lambda p: [-30 * C(p), 0, 0])},
        'right_arm': {'rotation': wave(1.0, 12, lambda p: [-28 * C(p), 0, 4])},
        'left_arm': {'rotation': wave(1.0, 12, lambda p: [28 * C(p), 0, -4])},
        'torso': {'rotation': wave(1.0, 12, lambda p: [2, 5 * S(p), 2 * S(p)])},
        'core': {'position': wave(1.0, 12, lambda p: [0, 0.6 * abs(S(p)), 0])},
        'skull': {'rotation': wave(1.0, 12, lambda p: [0, 0, 3 * S(p, 2)])},
    })
    A['skeleton_run'] = anim(0.7, True, {
        'right_leg': {'rotation': wave(0.7, 12, lambda p: [44 * C(p), 0, 0])},
        'left_leg': {'rotation': wave(0.7, 12, lambda p: [-44 * C(p), 0, 0])},
        'right_arm': {'rotation': wave(0.7, 12, lambda p: [-40 * C(p), 0, 6])},
        'left_arm': {'rotation': wave(0.7, 12, lambda p: [40 * C(p), 0, -6])},
        'torso': {'rotation': wave(0.7, 12, lambda p: [10, 7 * S(p), 0])},
        'core': {'position': wave(0.7, 12, lambda p: [0, 0.9 * abs(S(p)), 0])},
    })
    A['skeleton_attack'] = anim(0.5, False, {   # melee chop (wither skeletons / skeletons without a bow)
        'right_arm': {'rotation': keys((0, [-20, 0, 0]), (0.16, [-170, 10, 12]), (0.28, [-30, -15, -4]),
                                       (0.5, [-10, 0, 0]))},
        'torso': {'rotation': keys((0, [0, 0, 0]), (0.16, [-8, 18, 0]), (0.3, [16, -16, 0]), (0.5, [0, 0, 0]))},
        'left_arm': {'rotation': keys((0, [0, 0, 0]), (0.16, [20, 0, -15]), (0.3, [-20, 0, -5]), (0.5, [0, 0, 0]))},
    })
    A['skeleton_draw'] = anim(0.6, True, {   # bow aim: tension shake on the torso (arms are aimed in code)
        'torso': {'rotation': wave(0.6, 12, lambda p: [0, -8 + 0.8 * S(p, 3), 0.6 * S(p, 4)])},
        'core': {'position': wave(0.6, 6, lambda p: [0, 0, 0.2 * S(p)])},
        'right_leg': {'rotation': keys((0, [-8, 0, 4]))},
        'left_leg': {'rotation': keys((0, [10, 0, -4]))},
    })
    A['hurt'] = hurt_humanoid()
    A['death'] = death_humanoid()
    write_anims('humanoid', A)


def creeper_anims():
    legs4 = lambda amp: {
        'right_hind_leg': {'rotation': wave(1.0, 12, lambda p: [amp * C(p), 0, 0])},
        'left_hind_leg': {'rotation': wave(1.0, 12, lambda p: [-amp * C(p), 0, 0])},
        'right_front_leg': {'rotation': wave(1.0, 12, lambda p: [-amp * C(p), 0, 0])},
        'left_front_leg': {'rotation': wave(1.0, 12, lambda p: [amp * C(p), 0, 0])},
    }
    A = {}
    A['idle'] = anim(4.0, True, {
        'torso': {'rotation': wave(4.0, 16, lambda p: [1.2 * S(p), 0, 1.5 * S(p, 1, 0.25)])},
        'skull': {'rotation': wave(4.0, 16, lambda p: [2 * S(p, 2), 0, 3 * S(p, 1, 0.5)])},
        'body': {'scale': wave(4.0, 8, lambda p: [1 + 0.012 * S(p, 2), 1, 1 + 0.012 * S(p, 2)])},
    })
    w = legs4(26)
    w['torso'] = {'rotation': wave(1.0, 12, lambda p: [3, 3 * S(p), 3.5 * S(p, 2)])}
    w['core'] = {'position': wave(1.0, 12, lambda p: [0, 0.4 * abs(S(p, 1)), 0])}
    w['skull'] = {'rotation': wave(1.0, 12, lambda p: [0, 0, -2.5 * S(p, 2)])}
    A['walk'] = anim(1.0, True, w)
    A['fuse'] = anim(0.5, True, {
        'torso': {'rotation': wave(0.5, 10, lambda p: [8 + 1.5 * S(p, 3), 2.5 * S(p, 5), 3 * S(p, 4)])},
        'skull': {'rotation': wave(0.5, 10, lambda p: [10 + 2 * S(p, 4), 0, 3 * S(p, 3, .2)])},
        'right_front_leg': {'rotation': keys((0, [-14, 0, 6]))},
        'left_front_leg': {'rotation': keys((0, [-14, 0, -6]))},
        'right_hind_leg': {'rotation': keys((0, [10, 0, 4]))},
        'left_hind_leg': {'rotation': keys((0, [10, 0, -4]))},
        'core': {'position': wave(0.5, 10, lambda p: [0.2 * S(p, 5), -0.6, 0.2 * S(p, 4)])},
    })
    A['hurt'] = anim(0.4, False, {
        'core': {'rotation': keys((0, [0, 0, 0]), (0.07, [-10, 0, 6]), (0.4, [0, 0, 0]))},
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.07, [-14, 8, -8]), (0.4, [0, 0, 0]))},
        'body': {'scale': keys((0, [1, 1, 1]), (0.06, [1.06, 0.94, 1.06]), (0.2, [0.98, 1.02, 0.98]), (0.4, [1, 1, 1]))},
    })
    A['death'] = anim(0.9, 'hold', {
        'core': {'rotation': keys((0, [0, 0, 0]), (0.3, [0, 0, -14]), (0.85, [0, 0, 88]), (0.9, [0, 0, 86])),
                 'position': keys((0, [0, 0, 0]), (0.85, [-1.5, 2, 0]))},
        'right_front_leg': {'rotation': keys((0, [0, 0, 0]), (0.5, [-30, 0, 20]), (0.9, [-45, 0, 30]))},
        'left_front_leg': {'rotation': keys((0, [0, 0, 0]), (0.5, [-25, 0, -25]), (0.9, [-40, 0, -15]))},
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.85, [20, 0, 15]))},
    })
    write_anims('creeper', A)


SPIDER_LEGS = ['right_hind_leg', 'left_hind_leg', 'right_middle_hind_leg', 'left_middle_hind_leg',
               'right_middle_front_leg', 'left_middle_front_leg', 'right_front_leg', 'left_front_leg']
# vanilla rest pose (deg) - animations are relative to the bone's rest rotation, so only offsets are written


def spider_anims():
    A = {}
    # alternating tetrapod gait: legs (R1,L2,R3,L4) vs (L1,R2,L3,R4)
    def gait(L, swing, lift, n=12):
        bones = {}
        for i, leg in enumerate(SPIDER_LEGS):
            pair = i // 2
            right = leg.startswith('right')
            group = (pair + (0 if right else 1)) % 2
            ph = 0.5 * group
            side = 1 if right else -1
            bones[leg] = {'rotation': wave(L, n, lambda p, ph=ph, side=side: [
                0, side * swing * S(p, 1, ph), -side * lift * max(0.0, C(p, 1, ph))])}
        return bones
    w = gait(0.6, 20, 16)
    w['torso'] = {'rotation': wave(0.6, 12, lambda p: [0, 2 * S(p, 2), 1.5 * S(p, 2, 0.25)])}
    w['core'] = {'position': wave(0.6, 12, lambda p: [0, 0.3 * abs(S(p, 2)), 0])}
    w['abdomen'] = {'rotation': wave(0.6, 12, lambda p: [2 * S(p, 2), 3 * S(p), 0])}
    A['walk'] = anim(0.6, True, w)
    i = {}
    for k, leg in enumerate(SPIDER_LEGS):
        side = 1 if leg.startswith('right') else -1
        i[leg] = {'rotation': wave(3.0, 12, lambda p, k=k, side=side: [0, side * 2 * S(p, 1, k * 0.13),
                                                                        -side * 2.5 * max(0.0, S(p, 2, k * 0.21))])}
    i['abdomen'] = {'scale': wave(3.0, 12, lambda p: [1 + 0.03 * S(p, 2), 1 + 0.04 * S(p, 2), 1 + 0.02 * S(p, 2)]),
                    'rotation': wave(3.0, 12, lambda p: [2 * S(p), 0, 0])}
    i['skull'] = {'rotation': wave(3.0, 12, lambda p: [0, 4 * S(p, 1, 0.3), 2 * S(p, 2)])}
    A['idle'] = anim(3.0, True, i)
    A['attack'] = anim(0.6, False, {
        'torso': {'rotation': keys((0, [0, 0, 0]), (0.2, [-22, 0, 0]), (0.32, [14, 0, 0]), (0.6, [0, 0, 0]))},
        'core': {'position': keys((0, [0, 0, 0]), (0.2, [0, 2.5, 1]), (0.32, [0, 0, -3]), (0.6, [0, 0, 0]))},
        'right_front_leg': {'rotation': keys((0, [0, 0, 0]), (0.2, [0, 20, -40]), (0.32, [0, -10, 10]), (0.6, [0, 0, 0]))},
        'left_front_leg': {'rotation': keys((0, [0, 0, 0]), (0.2, [0, -20, 40]), (0.32, [0, 10, -10]), (0.6, [0, 0, 0]))},
        'right_middle_front_leg': {'rotation': keys((0, [0, 0, 0]), (0.2, [0, 10, -22]), (0.6, [0, 0, 0]))},
        'left_middle_front_leg': {'rotation': keys((0, [0, 0, 0]), (0.2, [0, -10, 22]), (0.6, [0, 0, 0]))},
    })
    A['hurt'] = anim(0.4, False, {
        'core': {'rotation': keys((0, [0, 0, 0]), (0.07, [-8, 0, 5]), (0.4, [0, 0, 0]))},
        'abdomen': {'scale': keys((0, [1, 1, 1]), (0.07, [1.08, 0.9, 1.08]), (0.4, [1, 1, 1]))},
        **{leg: {'rotation': keys((0, [0, 0, 0]), (0.07, [0, 0, (-15 if leg.startswith('right') else 15)]),
                                  (0.4, [0, 0, 0]))} for leg in SPIDER_LEGS},
    })
    d = {leg: {'rotation': keys((0, [0, 0, 0]), (0.25, [0, 0, (28 if leg.startswith('right') else -28)]),
                                (0.7, [0, (10 if leg.startswith('right') else -10), (70 if leg.startswith('right') else -70)]))}
         for leg in SPIDER_LEGS}
    d['core'] = {'rotation': keys((0, [0, 0, 0]), (0.2, [-10, 0, 0]), (0.75, [0, 0, 180])),
                 'position': keys((0, [0, 0, 0]), (0.3, [0, 3, 0]), (0.75, [0, 9, 0]))}
    A['death'] = anim(0.8, 'hold', d)
    write_anims('spider', A)


def quadruped_anims():
    def legs(L, amp, n=12):
        return {
            'right_hind_leg': {'rotation': wave(L, n, lambda p: [amp * C(p), 0, 0])},
            'left_hind_leg': {'rotation': wave(L, n, lambda p: [-amp * C(p), 0, 0])},
            'right_front_leg': {'rotation': wave(L, n, lambda p: [-amp * C(p), 0, 0])},
            'left_front_leg': {'rotation': wave(L, n, lambda p: [amp * C(p), 0, 0])},
        }
    A = {}
    A['idle'] = anim(4.0, True, {
        'body': {'scale': wave(4.0, 8, lambda p: [1 + 0.02 * S(p, 2), 1, 1 + 0.02 * S(p, 2)])},
        'skull': {'rotation': wave(4.0, 16, lambda p: [2 * S(p, 1), 0, 2.5 * S(p, 1, 0.3)])},
        'torso': {'rotation': wave(4.0, 8, lambda p: [0, 0, 0.8 * S(p)])},
    })
    w = legs(1.1, 28)
    w['torso'] = {'rotation': wave(1.1, 12, lambda p: [0, 2 * S(p), 2 * S(p)])}
    w['core'] = {'position': wave(1.1, 12, lambda p: [0, 0.35 * abs(S(p)), 0])}
    w['skull'] = {'rotation': wave(1.1, 12, lambda p: [4 * S(p, 2), 0, 0])}
    A['walk'] = anim(1.1, True, w)
    r_ = legs(0.6, 45)
    r_['torso'] = {'rotation': wave(0.6, 12, lambda p: [4 * S(p), 0, 0])}
    r_['core'] = {'position': wave(0.6, 12, lambda p: [0, 1.2 * abs(S(p)), 0])}
    r_['skull'] = {'rotation': wave(0.6, 12, lambda p: [-10 + 6 * S(p, 1, 0.25), 0, 0])}
    A['run'] = anim(0.6, True, r_)
    A['graze'] = anim(3.0, False, {
        'head': {'rotation': keys((0, [0, 0, 0]), (0.4, [42, 0, 0]), (2.5, [42, 0, 0]), (3.0, [0, 0, 0]))},
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.6, [6, 0, 0]), (0.9, [10, 4, 0]), (1.2, [6, 0, 0]),
                                   (1.5, [10, -4, 0]), (1.8, [6, 0, 0]), (2.1, [10, 3, 0]), (2.4, [6, 0, 0]),
                                   (3.0, [0, 0, 0]))},
    })
    A['hurt'] = anim(0.45, False, {
        'core': {'rotation': keys((0, [0, 0, 0]), (0.08, [-6, 0, 5]), (0.25, [2, 0, -2]), (0.45, [0, 0, 0])),
                 'position': keys((0, [0, 0, 0]), (0.08, [0, 1.2, 0]), (0.45, [0, 0, 0]))},
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.08, [-20, 10, 0]), (0.25, [-5, -8, 0]), (0.45, [0, 0, 0]))},
    })
    A['death'] = anim(1.0, 'hold', {
        'core': {'rotation': keys((0, [0, 0, 0]), (0.3, [0, 0, -8]), (0.9, [0, 0, 90]), (1.0, [0, 0, 88])),
                 'position': keys((0, [0, 0, 0]), (0.9, [-2, 1.5, 0]))},
        'right_front_leg': {'rotation': keys((0, [0, 0, 0]), (0.6, [-30, 0, 0]), (1.0, [-20, 0, 10]))},
        'left_front_leg': {'rotation': keys((0, [0, 0, 0]), (0.6, [-20, 0, 0]), (1.0, [-35, 0, -10]))},
        'right_hind_leg': {'rotation': keys((0, [0, 0, 0]), (0.6, [30, 0, 0]), (1.0, [20, 0, 10]))},
        'left_hind_leg': {'rotation': keys((0, [0, 0, 0]), (0.6, [20, 0, 0]), (1.0, [35, 0, -10]))},
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.9, [15, 0, 25]))},
    })
    write_anims('quadruped', A)


def chicken_anims():
    A = {}
    A['idle'] = anim(4.0, True, {
        'skull': {'rotation': keys((0, [0, 0, 0]), (1.0, [0, 18, 0]), (1.3, [0, 18, 0]), (1.4, [0, -12, 0]),
                                   (2.4, [0, -12, 0]), (2.5, [0, 0, 0]), (2.9, [0, 0, 0]), (3.05, [40, 0, 0]),
                                   (3.15, [5, 0, 0]), (3.3, [42, 0, 0]), (3.45, [0, 0, 0]), (4.0, [0, 0, 0]))},
        'torso': {'rotation': wave(4.0, 8, lambda p: [2 * S(p, 2), 0, 0])},
        'right_wing': {'rotation': wave(4.0, 8, lambda p: [0, 0, 2 * S(p, 2)])},
        'left_wing': {'rotation': wave(4.0, 8, lambda p: [0, 0, -2 * S(p, 2)])},
    })
    A['walk'] = anim(0.6, True, {
        'right_leg': {'rotation': wave(0.6, 12, lambda p: [40 * C(p), 0, 0])},
        'left_leg': {'rotation': wave(0.6, 12, lambda p: [-40 * C(p), 0, 0])},
        'torso': {'rotation': wave(0.6, 12, lambda p: [0, 0, 6 * S(p)])},
        'head': {'position': wave(0.6, 12, lambda p: [0, 0, -0.8 * S(p, 2)])},
        'tail': {'rotation': wave(0.6, 12, lambda p: [6 * S(p, 2), 0, 8 * S(p)])},
        'ruff': {'rotation': wave(0.6, 12, lambda p: [3 * S(p, 2, 0.1), 0, 0])},
        'core': {'position': wave(0.6, 12, lambda p: [0, 0.4 * abs(S(p)), 0])},
    })
    A['flap'] = anim(0.25, True, {
        'right_wing': {'rotation': wave(0.25, 8, lambda p: [0, 0, 40 + 35 * S(p)])},
        'tail': {'rotation': wave(0.25, 8, lambda p: [-15 + 8 * S(p), 0, 0])},
        'left_wing': {'rotation': wave(0.25, 8, lambda p: [0, 0, -40 - 35 * S(p)])},
        'right_leg': {'rotation': keys((0, [20, 0, 0]))},
        'left_leg': {'rotation': keys((0, [20, 0, 0]))},
        'torso': {'rotation': keys((0, [-10, 0, 0]))},
    })
    A['hurt'] = anim(0.5, False, {
        'core': {'position': keys((0, [0, 0, 0]), (0.1, [0, 3, 0]), (0.3, [0, 1, 0]), (0.5, [0, 0, 0]))},
        'right_wing': {'rotation': keys((0, [0, 0, 0]), (0.08, [0, 0, 80]), (0.16, [0, 0, 20]), (0.24, [0, 0, 80]),
                                        (0.5, [0, 0, 0]))},
        'left_wing': {'rotation': keys((0, [0, 0, 0]), (0.08, [0, 0, -80]), (0.16, [0, 0, -20]), (0.24, [0, 0, -80]),
                                       (0.5, [0, 0, 0]))},
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.1, [-30, 0, 15]), (0.5, [0, 0, 0]))},
    })
    A['death'] = anim(0.8, 'hold', {
        'core': {'rotation': keys((0, [0, 0, 0]), (0.75, [0, 0, 90])),
                 'position': keys((0, [0, 0, 0]), (0.2, [0, 2, 0]), (0.75, [-1, 1, 0]))},
        'right_wing': {'rotation': keys((0, [0, 0, 0]), (0.15, [0, 0, 70]), (0.3, [0, 0, 10]), (0.45, [0, 0, 60]),
                                        (0.8, [0, 0, 30]))},
        'left_wing': {'rotation': keys((0, [0, 0, 0]), (0.15, [0, 0, -70]), (0.3, [0, 0, -10]), (0.45, [0, 0, -60]),
                                       (0.8, [0, 0, -30]))},
        'right_leg': {'rotation': keys((0, [0, 0, 0]), (0.8, [-60, 0, 0]))},
        'left_leg': {'rotation': keys((0, [0, 0, 0]), (0.8, [-50, 0, 0]))},
    })
    write_anims('chicken', A)


def golem_anims():
    A = {}
    A['idle'] = anim(4.0, True, {
        'torso': {'rotation': wave(4.0, 16, lambda p: [1.5 * S(p), 0, 1 * S(p, 1, 0.25)])},
        'right_arm': {'rotation': wave(4.0, 16, lambda p: [2 * S(p, 1, 0.1), 0, 3 + 1.5 * S(p)])},
        'left_arm': {'rotation': wave(4.0, 16, lambda p: [-2 * S(p, 1, 0.1), 0, -3 - 1.5 * S(p)])},
        'body': {'scale': wave(4.0, 8, lambda p: [1 + 0.015 * S(p, 2), 1, 1 + 0.015 * S(p, 2)])},
        'skull': {'rotation': wave(4.0, 16, lambda p: [0, 4 * S(p, 1, 0.3), 0])},
    })
    A['walk'] = anim(1.6, True, {   # heavy, rolling stomp
        'right_leg': {'rotation': wave(1.6, 16, lambda p: [26 * C(p), 0, 0])},
        'left_leg': {'rotation': wave(1.6, 16, lambda p: [-26 * C(p), 0, 0])},
        'right_arm': {'rotation': wave(1.6, 16, lambda p: [-24 * C(p), 0, 4])},
        'left_arm': {'rotation': wave(1.6, 16, lambda p: [24 * C(p), 0, -4])},
        'torso': {'rotation': wave(1.6, 16, lambda p: [4, 6 * S(p), 4 * S(p)])},
        'core': {'position': wave(1.6, 16, lambda p: [0, -0.6 + 0.8 * abs(S(p)), 0])},
    })
    A['run'] = anim(1.1, True, {
        'right_leg': {'rotation': wave(1.1, 16, lambda p: [36 * C(p), 0, 0])},
        'left_leg': {'rotation': wave(1.1, 16, lambda p: [-36 * C(p), 0, 0])},
        'right_arm': {'rotation': wave(1.1, 16, lambda p: [-36 * C(p), 0, 8])},
        'left_arm': {'rotation': wave(1.1, 16, lambda p: [36 * C(p), 0, -8])},
        'torso': {'rotation': wave(1.1, 16, lambda p: [12, 8 * S(p), 3 * S(p)])},
        'core': {'position': wave(1.1, 16, lambda p: [0, 1.2 * abs(S(p)), 0])},
    })
    A['attack'] = anim(0.6, False, {   # both fists up, then a ground-shaking slam
        'right_arm': {'rotation': keys((0, [0, 0, 0]), (0.2, [-165, 0, -12]), (0.32, [-40, 0, 6]), (0.6, [0, 0, 0]))},
        'left_arm': {'rotation': keys((0, [0, 0, 0]), (0.2, [-165, 0, 12]), (0.32, [-40, 0, -6]), (0.6, [0, 0, 0]))},
        'torso': {'rotation': keys((0, [0, 0, 0]), (0.2, [-14, 0, 0]), (0.32, [24, 0, 0]), (0.6, [0, 0, 0]))},
        'core': {'position': keys((0, [0, 0, 0]), (0.2, [0, 1.5, 0]), (0.32, [0, -1.5, -1]), (0.6, [0, 0, 0]))},
    })
    A['hurt'] = anim(0.5, False, {
        'torso': {'rotation': keys((0, [0, 0, 0]), (0.08, [-6, 4, 3]), (0.5, [0, 0, 0]))},
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.08, [-10, -6, 0]), (0.5, [0, 0, 0]))},
    })
    A['death'] = anim(1.0, 'hold', {   # knees buckle, topples forward
        'right_leg': {'rotation': keys((0, [0, 0, 0]), (0.35, [-50, 0, 0]), (0.9, [-80, 0, 6]))},
        'left_leg': {'rotation': keys((0, [0, 0, 0]), (0.35, [-45, 0, 0]), (0.9, [-80, 0, -6]))},
        'core': {'rotation': keys((0, [0, 0, 0]), (0.35, [12, 0, 0]), (0.9, [86, 0, 4]), (1.0, [84, 0, 4])),
                 'position': keys((0, [0, 0, 0]), (0.35, [0, -4, 0]), (0.9, [0, 3, -2]))},
        'right_arm': {'rotation': keys((0, [0, 0, 0]), (0.5, [-50, 0, 20]), (0.9, [-170, 0, 30]))},
        'left_arm': {'rotation': keys((0, [0, 0, 0]), (0.5, [-50, 0, -20]), (0.9, [-170, 0, -30]))},
    })
    A['offer'] = anim(2.0, True, {
        'right_arm': {'rotation': keys((0, [-60, 0, 0]), (1.0, [-62, 0, 4]), (2.0, [-60, 0, 0]))},
        'skull': {'rotation': keys((0, [10, 0, 8]), (1.0, [12, 0, 12]), (2.0, [10, 0, 8]))},
    })
    write_anims('iron_golem', A)


def enderman_anims():
    A = {}
    A['idle'] = anim(5.0, True, {
        'torso': {'rotation': wave(5.0, 20, lambda p: [1.5 * S(p), 2 * S(p, 1, 0.3), 1.5 * S(p, 1, 0.1)])},
        'right_arm': {'rotation': wave(5.0, 20, lambda p: [2 * S(p, 1, 0.2), 0, 2 + 1.5 * S(p)])},
        'left_arm': {'rotation': wave(5.0, 20, lambda p: [-2 * S(p, 1, 0.2), 0, -2 - 1.5 * S(p)])},
        'skull': {'rotation': wave(5.0, 20, lambda p: [0, 0, 6 * S(p, 1, 0.4)])},
    })
    A['walk'] = anim(1.6, True, {   # long, gliding strides
        'right_leg': {'rotation': wave(1.6, 16, lambda p: [20 * C(p), 0, 0])},
        'left_leg': {'rotation': wave(1.6, 16, lambda p: [-20 * C(p), 0, 0])},
        'right_arm': {'rotation': wave(1.6, 16, lambda p: [-14 * C(p), 0, 3])},
        'left_arm': {'rotation': wave(1.6, 16, lambda p: [14 * C(p), 0, -3])},
        'torso': {'rotation': wave(1.6, 16, lambda p: [3, 4 * S(p), 2 * S(p)])},
        'core': {'position': wave(1.6, 16, lambda p: [0, 0.5 * abs(S(p)), 0])},
    })
    A['run'] = anim(0.7, True, {   # predatory sprint, arms trailing
        'right_leg': {'rotation': wave(0.7, 14, lambda p: [34 * C(p), 0, 0])},
        'left_leg': {'rotation': wave(0.7, 14, lambda p: [-34 * C(p), 0, 0])},
        'right_arm': {'rotation': wave(0.7, 14, lambda p: [30 - 10 * C(p), 0, 12])},
        'left_arm': {'rotation': wave(0.7, 14, lambda p: [30 + 10 * C(p), 0, -12])},
        'torso': {'rotation': wave(0.7, 14, lambda p: [18, 5 * S(p), 0])},
        'core': {'position': wave(0.7, 14, lambda p: [0, 1.0 * abs(S(p)), 0])},
    })
    A['attack'] = anim(0.5, False, {
        'right_arm': {'rotation': keys((0, [0, 0, 0]), (0.15, [-150, 0, 20]), (0.28, [-50, 0, -10]), (0.5, [0, 0, 0]))},
        'left_arm': {'rotation': keys((0, [0, 0, 0]), (0.2, [-140, 0, -20]), (0.33, [-45, 0, 10]), (0.5, [0, 0, 0]))},
        'torso': {'rotation': keys((0, [0, 0, 0]), (0.15, [-10, 0, 0]), (0.3, [22, 0, 0]), (0.5, [0, 0, 0]))},
    })
    A['hurt'] = anim(0.4, False, {
        'core': {'position': keys((0, [0, 0, 0]), (0.05, [1.5, 0, 0]), (0.1, [-1.5, 0, 0]), (0.15, [1, 0, 0]), (0.2, [0, 0, 0]))},
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.06, [-15, 20, 0]), (0.4, [0, 0, 0]))},
    })
    A['death'] = anim(1.0, 'hold', {   # folds in on itself
        'right_leg': {'rotation': keys((0, [0, 0, 0]), (0.4, [-70, 0, 0]), (0.9, [-90, 0, 10]))},
        'left_leg': {'rotation': keys((0, [0, 0, 0]), (0.4, [-65, 0, 0]), (0.9, [-90, 0, -10]))},
        'core': {'position': keys((0, [0, 0, 0]), (0.4, [0, -12, 4]), (0.9, [0, -22, 6]))},
        'torso': {'rotation': keys((0, [0, 0, 0]), (0.4, [20, 0, 0]), (0.9, [60, 0, 10]))},
        'right_arm': {'rotation': keys((0, [0, 0, 0]), (0.5, [-30, 0, 40]), (0.9, [-10, 0, 70]))},
        'left_arm': {'rotation': keys((0, [0, 0, 0]), (0.5, [-30, 0, -40]), (0.9, [-10, 0, -70]))},
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.9, [30, 0, 25]))},
    })
    write_anims('enderman', A)


def villager_anims():
    A = {}
    A['idle'] = anim(3.6, True, {
        'torso': {'rotation': wave(3.6, 16, lambda p: [1.5 * S(p), 0, 1.5 * S(p, 1, 0.25)])},
        'skull': {'rotation': wave(3.6, 16, lambda p: [2 * S(p, 2), 0, 3 * S(p)])},
        'arms': {'rotation': wave(3.6, 8, lambda p: [1.5 * S(p, 2), 0, 0])},
    })
    A['walk'] = anim(1.1, True, {
        'right_leg': {'rotation': wave(1.1, 12, lambda p: [28 * C(p), 0, 0])},
        'left_leg': {'rotation': wave(1.1, 12, lambda p: [-28 * C(p), 0, 0])},
        'torso': {'rotation': wave(1.1, 12, lambda p: [2, 5 * S(p), 3 * S(p)])},
        'core': {'position': wave(1.1, 12, lambda p: [0, 0.6 * abs(S(p)), 0])},
        'arms': {'rotation': wave(1.1, 12, lambda p: [3 * S(p, 2), 0, 0])},
    })
    A['run'] = anim(0.6, True, {   # panic: arms flung up, frantic
        'right_leg': {'rotation': wave(0.6, 12, lambda p: [45 * C(p), 0, 0])},
        'left_leg': {'rotation': wave(0.6, 12, lambda p: [-45 * C(p), 0, 0])},
        'torso': {'rotation': wave(0.6, 12, lambda p: [14, 8 * S(p), 0])},
        'core': {'position': wave(0.6, 12, lambda p: [0, 1.3 * abs(S(p)), 0])},
        'arms': {'rotation': wave(0.6, 12, lambda p: [-40 + 10 * S(p, 2), 0, 0])},
        'skull': {'rotation': wave(0.6, 12, lambda p: [-12, 10 * S(p), 0])},
    })
    A['nod'] = anim(1.2, True, {
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.25, [14, 0, 0]), (0.5, [0, 0, 0]), (0.75, [12, 0, 0]), (1.2, [0, 0, 0]))},
    })
    A['hurt'] = anim(0.45, False, {
        'core': {'rotation': keys((0, [0, 0, 0]), (0.07, [-10, 0, 4]), (0.45, [0, 0, 0]))},
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.07, [-16, 8, 6]), (0.45, [0, 0, 0]))},
        'arms': {'rotation': keys((0, [0, 0, 0]), (0.08, [-30, 0, 0]), (0.45, [0, 0, 0]))},
    })
    A['death'] = anim(1.0, 'hold', {
        'core': {'rotation': keys((0, [0, 0, 0]), (0.3, [6, 0, 0]), (0.95, [88, 0, 6])),
                 'position': keys((0, [0, 0, 0]), (0.3, [0, -1.5, 0]), (0.95, [0, 3, 0]))},
        'right_leg': {'rotation': keys((0, [0, 0, 0]), (0.3, [-30, 0, 0]), (0.9, [0, 0, 5]))},
        'left_leg': {'rotation': keys((0, [0, 0, 0]), (0.3, [-25, 0, 0]), (0.9, [5, 0, -5]))},
        'skull': {'rotation': keys((0, [0, 0, 0]), (0.9, [-20, 15, 15]))},
    })
    write_anims('villager', A)


def main():
    os.makedirs(GEO, exist_ok=True)
    os.makedirs(ANIM, exist_ok=True)
    models = {'zombie': humanoid(False), 'drowned': humanoid(False, True), 'sheep': sheep(),
              'iron_golem': iron_golem(), 'enderman': enderman(), 'villager': villager(), 'skeleton': humanoid(True), 'creeper': creeper(), 'spider': spider(),
              'cow': quadruped('cow'), 'pig': quadruped('pig'), 'chicken': chicken()}
    for n, g in models.items():
        with open(os.path.join(GEO, n + '.geo.json'), 'w') as f:
            json.dump(g, f, indent=1)
    humanoid_anims()
    creeper_anims()
    spider_anims()
    quadruped_anims()
    chicken_anims()
    golem_anims()
    enderman_anims()
    villager_anims()
    print('wrote', len(models), 'models')


if __name__ == '__main__':
    main()
