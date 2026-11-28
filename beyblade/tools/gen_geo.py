#!/usr/bin/env python3
"""Generates the GeckoLib skeleton and every keyframed animation of the beyblades.

  assets/librabeyblade/geo/beyblade.geo.json
  assets/librabeyblade/animations/beyblade.animation.json

The skeleton is shared by every bey:   root > tilt > motion > spin > { tip_lag > tip, track, wheel, ring, bolt }
  root/tilt/spin   procedural (BeyRig): lift, precession wobble + lean, the spin itself
  motion           keyframed: hops, recoil, special moves, sleep out
  parts            keyframed: burst finish, face bolt rise, rubber squash, ball roll ...
The meshes of the parts are drawn at their bones by the render layer (BeyBones / BeyMesh).
Units: positions in pixels (1/16 block), rotations in degrees, time in seconds.
"""
import json
import math
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'common', 'src', 'main', 'resources', 'assets', 'librabeyblade')

# mesh units * 0.45 (world scale) * 16 px
PX = 0.45 * 16
PIVOTS = {
    'tip': 0.2 * PX,
    'track': 0.6 * PX,
    'wheel': 1.0 * PX,
    'ring': 1.2 * PX,
    'bolt': 1.33 * PX,
}


def geo():
    bones = [
        {'name': 'root', 'pivot': [0, 0, 0]},
        {'name': 'tilt', 'parent': 'root', 'pivot': [0, 0, 0]},
        {'name': 'motion', 'parent': 'tilt', 'pivot': [0, 0, 0]},
        {'name': 'spin', 'parent': 'motion', 'pivot': [0, 0, 0]},
    ]
    bones.append({'name': 'tip_lag', 'parent': 'spin', 'pivot': [0, 0, 0]})
    for n, y in PIVOTS.items():
        bones.append({'name': n, 'parent': 'tip_lag' if n == 'tip' else 'spin', 'pivot': [0, round(y, 3), 0]})
    return {
        'format_version': '1.12.0',
        'minecraft:geometry': [{
            'description': {
                'identifier': 'geometry.beyblade',
                'texture_width': 16,
                'texture_height': 16,
                'visible_bounds_width': 4,
                'visible_bounds_height': 3,
                'visible_bounds_offset': [0, 1, 0],
            },
            'bones': bones,
        }],
    }


def r3(v):
    return [round(float(x), 4) for x in v]


def track(keys, smooth=True):
    """keys: list of (time, [x,y,z])"""
    out = {}
    for t, v in keys:
        k = '%.4g' % t
        if smooth:
            out[k] = {'post': r3(v), 'lerp_mode': 'catmullrom'}
        else:
            out[k] = r3(v)
    return out


def anim(length, bones, loop=False):
    a = {'animation_length': round(length, 4), 'bones': bones}
    if loop:
        a['loop'] = True
    return a


def osc(length, steps, fn):
    """samples fn(phase 0..1) -> vec along a full loop"""
    return [(length * i / steps, fn(i / steps)) for i in range(steps + 1)]


def jitter(t0, t1, n, amp, axis=(1, 0, 1)):
    keys = []
    for i in range(n + 1):
        s = 1 if i % 2 == 0 else -1
        fall = 1.0 - i / n
        keys.append((t0 + (t1 - t0) * i / n, [axis[0] * amp * s * fall, axis[1] * amp * s * fall, axis[2] * amp * -s * fall]))
    keys[-1] = (t1, [0, 0, 0])
    return keys


A = {}

# ------------------------------------------------------------------ body loops
A['spin'] = anim(0.6, {
    'motion': {'position': track(osc(0.6, 4, lambda p: [0, 0.12 * math.sin(p * 2 * math.pi), 0]))},
}, loop=True)
A['spin_low'] = anim(0.5, {
    'motion': {
        'rotation': track(osc(0.5, 4, lambda p: [3 * math.sin(p * 2 * math.pi), 0, 3 * math.cos(p * 2 * math.pi)])),
        'position': track(osc(0.5, 4, lambda p: [0, 0.3 * abs(math.sin(p * 2 * math.pi)), 0])),
    },
}, loop=True)
A['spin_dying'] = anim(0.4, {
    'motion': {
        'rotation': track(osc(0.4, 4, lambda p: [6 * math.sin(p * 2 * math.pi), 0, 6 * math.cos(p * 2 * math.pi)])),
        'position': track(osc(0.4, 4, lambda p: [0, 0.65 * abs(math.sin(p * 2 * math.pi)), 0])),
    },
}, loop=True)
A['airborne'] = anim(0.5, {
    'motion': {
        'rotation': track(osc(0.5, 4, lambda p: [14 + 4 * math.sin(p * 2 * math.pi), 0, 4 * math.cos(p * 2 * math.pi)])),
    },
}, loop=True)
A['recall'] = anim(0.5, {
    'motion': {
        'rotation': track([(0, [0, 0, 0]), (0.25, [0, 0, 180]), (0.5, [0, 0, 360])], smooth=False),
        'scale': track([(0, [0.9, 0.9, 0.9]), (0.25, [0.85, 0.85, 0.85]), (0.5, [0.9, 0.9, 0.9])]),
    },
}, loop=True)
# sleep out: the bey loses its balance, rolls around its rim and falls flat
A['sleep_out'] = anim(1.5, {
    'motion': {
        'rotation': track([(0, [0, 0, 0]), (0.25, [6, 0, 12]), (0.5, [-10, 0, 28]), (0.75, [8, 0, 52]), (1.0, [0, 0, 80]),
                           (1.15, [0, 0, 74]), (1.3, [0, 0, 79]), (1.5, [0, 0, 78])]),
        'position': track([(0, [0, 0, 0]), (0.5, [0.4, 0.2, 0]), (1.0, [2.6, 1.2, 0]), (1.15, [3.0, 1.7, 0]), (1.5, [3.0, 1.4, 0])]),
    },
}, loop=False)
# burst finish: the face bolt, energy ring, fusion wheel, spin track and tip fly apart and land flat
FLOOR = {n: -y for n, y in PIVOTS.items()}
A['burst'] = anim(1.8, {
    'motion': {'position': track([(0, [0, 0, 0]), (0.12, [0, 2.5, 0]), (0.3, [0, 0, 0])])},
    'bolt': {
        'position': track([(0, [0, 0, 0]), (0.1, [1, 6, 0]), (0.45, [4, 16, -3]), (0.9, [7, 4, -6]), (1.05, [7.5, FLOOR['bolt'] + 1.2, -6.4]),
                           (1.2, [7.8, FLOOR['bolt'] + 2.2, -6.6]), (1.4, [8, FLOOR['bolt'] + 1.2, -6.8]), (1.8, [8, FLOOR['bolt'] + 1.2, -6.8])]),
        'rotation': track([(0, [0, 0, 0]), (0.45, [140, 60, 0]), (1.05, [270, 140, 0]), (1.4, [270, 160, 0]), (1.8, [270, 160, 0])]),
    },
    'ring': {
        'position': track([(0, [0, 0, 0]), (0.1, [0, 4, 1]), (0.5, [3, 12, 7]), (0.95, [6, 1, 12]), (1.1, [6.5, FLOOR['ring'] + 0.4, 13]),
                           (1.25, [6.7, FLOOR['ring'] + 1.3, 13.4]), (1.45, [6.9, FLOOR['ring'] + 0.4, 13.6]), (1.8, [6.9, FLOOR['ring'] + 0.4, 13.6])]),
        'rotation': track([(0, [0, 0, 0]), (0.5, [-90, 0, 30]), (1.1, [-180, 0, 0]), (1.45, [-180, 20, 0]), (1.8, [-180, 20, 0])]),
    },
    'wheel': {
        'position': track([(0, [0, 0, 0]), (0.12, [-1, 5, 0]), (0.5, [-6, 10, 3]), (1.0, [-10, 0, 6]), (1.15, [-10.6, FLOOR['wheel'] + 0.6, 6.4]),
                           (1.3, [-11, FLOOR['wheel'] + 1.6, 6.8]), (1.5, [-11.2, FLOOR['wheel'] + 0.6, 7]), (1.8, [-11.2, FLOOR['wheel'] + 0.6, 7])]),
        'rotation': track([(0, [0, 0, 0]), (0.5, [0, 120, 70]), (1.15, [0, 200, 180]), (1.5, [0, 230, 180]), (1.8, [0, 230, 180])]),
    },
    'track': {
        'position': track([(0, [0, 0, 0]), (0.1, [0, 3, -1]), (0.45, [-2, 7, -6]), (0.9, [-4, 0, -9]), (1.05, [-4.3, FLOOR['track'] + 1.6, -9.5]),
                           (1.3, [-4.5, FLOOR['track'] + 2.1, -9.8]), (1.8, [-4.6, FLOOR['track'] + 1.6, -10])]),
        'rotation': track([(0, [0, 0, 0]), (0.45, [60, 0, 0]), (1.05, [90, 0, 0]), (1.8, [90, 30, 0])]),
    },
    'tip': {
        'position': track([(0, [0, 0, 0]), (0.15, [1, 2, -1]), (0.6, [2, 3, -2.5]), (0.9, [2.4, FLOOR['tip'] + 1.4, -3]), (1.8, [2.6, FLOOR['tip'] + 1.4, -3.2])]),
        'rotation': track([(0, [0, 0, 0]), (0.6, [0, 0, 60]), (0.9, [0, 0, 90]), (1.8, [0, 0, 90])]),
    },
}, loop=False)

# ------------------------------------------------------------------ tip loops
A['tip_idle'] = anim(1.0, {}, loop=True)
A['tip_rubber'] = anim(0.25, {  # rubber squashes against the floor as it grips
    'tip': {'scale': track(osc(0.25, 4, lambda p: [1 + 0.05 * abs(math.sin(p * 2 * math.pi)), 1 - 0.08 * abs(math.sin(p * 2 * math.pi)),
                                                   1 + 0.05 * abs(math.sin(p * 2 * math.pi))]))},
}, loop=True)
A['tip_ball'] = anim(0.8, {  # the ball rolls under the bey
    'tip': {'rotation': track(osc(0.8, 8, lambda p: [5 * math.sin(p * 2 * math.pi), 0, 5 * math.cos(p * 2 * math.pi)]))},
}, loop=True)
A['tip_bearing'] = anim(0.3, {  # bearing / free spin: tiny shimmer, the lag is procedural
    'tip': {'position': track(osc(0.3, 4, lambda p: [0, 0.05 * math.sin(p * 2 * math.pi), 0]))},
}, loop=True)
A['tip_sharp'] = anim(0.2, {
    'tip': {'rotation': track(osc(0.2, 4, lambda p: [1.2 * math.sin(p * 2 * math.pi), 0, 1.2 * math.cos(p * 2 * math.pi)]))},
}, loop=True)
A['tip_flat'] = anim(0.4, {
    'tip': {'position': track(osc(0.4, 4, lambda p: [0.08 * math.sin(p * 2 * math.pi), 0, 0.08 * math.cos(p * 2 * math.pi)]))},
}, loop=True)

# ------------------------------------------------------------------ one shot actions
A['hit_light'] = anim(0.3, {
    'motion': {
        'position': track([(0, [0, 0, 0]), (0.06, [0, 1.2, 0]), (0.16, [0, 0.2, 0]), (0.3, [0, 0, 0])]),
        'rotation': track([(0, [0, 0, 0]), (0.05, [0, 0, -12]), (0.14, [0, 0, 6]), (0.3, [0, 0, 0])]),
        'scale': track([(0, [1, 1, 1]), (0.05, [1.08, 0.9, 1.08]), (0.14, [0.97, 1.04, 0.97]), (0.3, [1, 1, 1])]),
    },
    'wheel': {'rotation': track([(0, [0, 0, 0]), (0.06, [0, 25, 0]), (0.3, [0, 0, 0])])},
    'ring': {'position': track(jitter(0, 0.25, 5, 0.25, (1, 0, 1)))},
})
A['hit_heavy'] = anim(0.55, {
    'motion': {
        'position': track([(0, [0, 0, 0]), (0.08, [0, 3.5, 0]), (0.22, [0, 0, 0]), (0.32, [0, 0.8, 0]), (0.55, [0, 0, 0])]),
        'rotation': track([(0, [0, 0, 0]), (0.06, [-25, 0, 10]), (0.18, [12, 0, -8]), (0.32, [-5, 0, 3]), (0.55, [0, 0, 0])]),
        'scale': track([(0, [1, 1, 1]), (0.05, [1.14, 0.84, 1.14]), (0.16, [0.94, 1.08, 0.94]), (0.3, [1.03, 0.98, 1.03]), (0.55, [1, 1, 1])]),
    },
    'bolt': {'position': track(jitter(0, 0.4, 8, 0.35, (1, 0.5, 1)))},
    'ring': {'position': track(jitter(0, 0.4, 8, 0.5, (1, 0.3, 1)))},
    'wheel': {'rotation': track([(0, [0, 0, 0]), (0.08, [0, 45, 0]), (0.55, [0, 0, 0])])},
})
A['hit_wall'] = anim(0.35, {
    'motion': {
        'position': track([(0, [0, 0, 0]), (0.07, [0, 1.5, 0]), (0.35, [0, 0, 0])]),
        'rotation': track([(0, [0, 0, 0]), (0.06, [18, 0, 0]), (0.2, [-6, 0, 0]), (0.35, [0, 0, 0])]),
    },
})
A['land'] = anim(0.35, {
    'motion': {
        'scale': track([(0, [1.15, 0.8, 1.15]), (0.1, [0.95, 1.08, 0.95]), (0.2, [1.03, 0.97, 1.03]), (0.35, [1, 1, 1])]),
        'position': track([(0, [0, -0.5, 0]), (0.1, [0, 0.6, 0]), (0.35, [0, 0, 0])]),
    },
    'tip': {'scale': track([(0, [1.2, 0.7, 1.2]), (0.12, [1, 1, 1])])},
})
A['launch'] = anim(0.7, {
    'motion': {
        'rotation': track([(0, [30, 0, 0]), (0.25, [-10, 0, 6]), (0.45, [4, 0, -3]), (0.7, [0, 0, 0])]),
        'scale': track([(0, [0.9, 1.1, 0.9]), (0.2, [1.05, 0.95, 1.05]), (0.7, [1, 1, 1])]),
    },
    'wheel': {'rotation': track([(0, [0, 0, 0]), (0.35, [0, -360, 0]), (0.7, [0, -540, 0])], smooth=False)},
    'ring': {'rotation': track([(0, [0, 0, 0]), (0.35, [0, -360, 0]), (0.7, [0, -540, 0])], smooth=False)},
    'bolt': {'rotation': track([(0, [0, 0, 0]), (0.35, [0, -360, 0]), (0.7, [0, -540, 0])], smooth=False)},
})
A['evade'] = anim(0.4, {
    'motion': {
        'position': track([(0, [0, 0, 0]), (0.1, [0, 2, 0]), (0.25, [0, 0.4, 0]), (0.4, [0, 0, 0])]),
        'rotation': track([(0, [0, 0, 0]), (0.1, [0, 0, 20]), (0.25, [0, 0, -6]), (0.4, [0, 0, 0])]),
    },
})
A['shield_hit'] = anim(0.3, {
    'motion': {'scale': track([(0, [1, 1, 1]), (0.05, [1.07, 0.95, 1.07]), (0.3, [1, 1, 1])])},
    'ring': {'scale': track([(0, [1, 1, 1]), (0.06, [1.12, 1, 1.12]), (0.3, [1, 1, 1])])},
})
A['grind'] = anim(0.25, {
    'motion': {'rotation': track(jitter(0, 0.25, 6, 4, (1, 0, 1)))},
    'wheel': {'position': track(jitter(0, 0.25, 6, 0.2, (1, 0, 1)))},
})
A['special_clash'] = anim(0.8, {
    'motion': {
        'position': track([(0, [0, 0, 0]), (0.15, [0, 4, 0]), (0.4, [0, 0.5, 0]), (0.8, [0, 0, 0])]),
        'rotation': track([(0, [0, 0, 0]), (0.15, [-30, 0, 0]), (0.4, [10, 0, 0]), (0.8, [0, 0, 0])]),
        'scale': track([(0, [1, 1, 1]), (0.15, [1.1, 1.1, 1.1]), (0.8, [1, 1, 1])]),
    },
})
A['spirit'] = anim(1.0, {
    'motion': {
        'position': track([(0, [0, 0, 0]), (0.3, [0, 2.5, 0]), (0.7, [0, 2.0, 0]), (1.0, [0, 0, 0])]),
        'scale': track([(0, [1, 1, 1]), (0.3, [1.12, 1.12, 1.12]), (1.0, [1, 1, 1])]),
    },
    'bolt': {'rotation': track([(0, [0, 0, 0]), (0.5, [0, 180, 0]), (1.0, [0, 360, 0])], smooth=False),
             'position': track([(0, [0, 0, 0]), (0.3, [0, 1, 0]), (1.0, [0, 0, 0])])},
})
A['xtreme_dash'] = anim(0.5, {
    'motion': {
        'rotation': track([(0, [0, 0, 0]), (0.08, [-30, 0, 0]), (0.3, [-12, 0, 0]), (0.5, [0, 0, 0])]),
        'position': track([(0, [0, 0, 0]), (0.08, [0, 1, 0]), (0.5, [0, 0, 0])]),
        'scale': track([(0, [1, 1, 1]), (0.08, [0.9, 1.1, 0.9]), (0.25, [1.06, 0.95, 1.06]), (0.5, [1, 1, 1])]),
    },
})
A['rail'] = anim(0.5, {
    'motion': {'rotation': track([(0, [0, 0, 0]), (0.1, [0, 0, 35]), (0.35, [0, 0, 28]), (0.5, [0, 0, 0])])},
})

# ------------------------------------------------------------------ special move archetypes
A['sp_awaken'] = anim(1.5, {
    'motion': {
        'position': track([(0, [0, 0, 0]), (0.4, [0, 5, 0]), (0.7, [0, 4.2, 0]), (1.0, [0, 4.8, 0]), (1.5, [0, 0, 0])]),
        'scale': track([(0, [1, 1, 1]), (0.4, [1.25, 1.25, 1.25]), (0.8, [1.15, 1.15, 1.15]), (1.5, [1, 1, 1])]),
    },
    'bolt': {'position': track([(0, [0, 0, 0]), (0.4, [0, 1.6, 0]), (1.1, [0, 1.2, 0]), (1.5, [0, 0, 0])]),
             'scale': track([(0, [1, 1, 1]), (0.4, [1.2, 1.2, 1.2]), (1.5, [1, 1, 1])])},
    'ring': {'rotation': track([(0, [0, 0, 0]), (0.75, [0, -360, 0]), (1.5, [0, -720, 0])], smooth=False)},
})
A['sp_dive'] = anim(2.0, {
    'motion': {
        'position': track([(0, [0, 0, 0]), (0.15, [0, -1, 0]), (0.6, [0, 26, 0]), (0.9, [0, 30, 0]), (1.15, [0, -1.5, 0]),
                           (1.3, [0, 1.2, 0]), (1.5, [0, 0, 0]), (2.0, [0, 0, 0])]),
        'rotation': track([(0, [0, 0, 0]), (0.4, [-25, 0, 0]), (0.85, [-20, 0, 10]), (1.12, [20, 0, 0]), (1.5, [0, 0, 0]), (2.0, [0, 0, 0])]),
        'scale': track([(0, [1, 1, 1]), (0.15, [1.15, 0.85, 1.15]), (0.6, [0.95, 1.1, 0.95]), (1.15, [1.3, 0.72, 1.3]),
                        (1.35, [0.95, 1.06, 0.95]), (1.6, [1, 1, 1]), (2.0, [1, 1, 1])]),
    },
})
A['sp_drill'] = anim(1.4, {
    'motion': {
        'position': track([(0, [0, 0, 0]), (0.3, [0, -3, 0]), (1.1, [0, -3.2, 0]), (1.4, [0, 0, 0])]),
        'rotation': track([(0, [0, 0, 0])] + jitter(0.3, 1.1, 16, 5, (1, 0, 1))[1:] + [(1.4, [0, 0, 0])]),
        'scale': track([(0, [1, 1, 1]), (0.3, [1.05, 0.9, 1.05]), (1.1, [1.05, 0.9, 1.05]), (1.4, [1, 1, 1])]),
    },
    'tip': {'scale': track([(0, [1, 1, 1]), (0.3, [1.3, 0.7, 1.3]), (1.1, [1.3, 0.7, 1.3]), (1.4, [1, 1, 1])])},
})
A['sp_dash'] = anim(1.0, {
    'motion': {
        'position': track([(0, [0, 0, 0]), (0.12, [0, 1.5, 0]), (0.3, [0, 0.4, 0]), (1.0, [0, 0, 0])]),
        'scale': track([(0, [0.92, 1.12, 0.92]), (0.12, [1.12, 0.88, 1.12]), (0.35, [1.04, 0.97, 1.04]), (1.0, [1, 1, 1])]),
    },
    'wheel': {'rotation': track([(0, [0, 0, 0]), (0.5, [0, -180, 0]), (1.0, [0, -360, 0])], smooth=False)},
})
A['sp_vortex'] = anim(2.0, {
    'motion': {
        'rotation': track([(i * 0.125, [15 * math.sin(i * math.pi / 2) * (1 if i < 15 else 0), 0, 15 * math.cos(i * math.pi / 2) * (1 if 0 < i < 15 else 0)])
                           for i in range(17)]),
        'position': track(osc(2.0, 8, lambda p: [0, 1.5 * math.sin(p * math.pi), 0])),
        'scale': track([(0, [1, 1, 1]), (0.4, [1.08, 1.08, 1.08]), (1.6, [1.08, 1.08, 1.08]), (2.0, [1, 1, 1])]),
    },
})
A['sp_guard'] = anim(1.2, {
    'motion': {
        'position': track([(0, [0, 0, 0]), (0.2, [0, -1, 0]), (0.9, [0, -1, 0]), (1.2, [0, 0, 0])]),
        'scale': track([(0, [1, 1, 1]), (0.2, [1.12, 0.88, 1.12]), (0.9, [1.1, 0.9, 1.1]), (1.2, [1, 1, 1])]),
    },
    'ring': {'scale': track([(0, [1, 1, 1]), (0.25, [1.15, 1.05, 1.15]), (0.9, [1.12, 1.04, 1.12]), (1.2, [1, 1, 1])])},
})
A['sp_steal'] = anim(1.4, {
    'motion': {
        'rotation': track([(0, [0, 0, 0]), (0.2, [0, 0, 18]), (0.45, [0, 0, -18]), (0.7, [0, 0, 18]), (0.95, [0, 0, -18]), (1.4, [0, 0, 0])]),
        'position': track([(0, [0, 0, 0]), (0.2, [0, -0.6, 0]), (1.2, [0, -0.6, 0]), (1.4, [0, 0, 0])]),
    },
    'wheel': {'rotation': track([(0, [0, 0, 0]), (0.7, [0, 180, 0]), (1.4, [0, 360, 0])], smooth=False)},
})
A['sp_levitate'] = anim(2.0, {
    'motion': {
        'position': track([(0, [0, 0, 0]), (0.6, [0, 8, 0]), (1.0, [0, 9, 0]), (1.4, [0, 7.5, 0]), (2.0, [0, 0, 0])]),
        'rotation': track([(0, [0, 0, 0]), (0.6, [8, 0, -6]), (1.0, [-6, 0, 8]), (1.4, [6, 0, -4]), (2.0, [0, 0, 0])]),
        'scale': track([(0, [1, 1, 1]), (0.6, [1.1, 1.1, 1.1]), (1.6, [1.1, 1.1, 1.1]), (2.0, [1, 1, 1])]),
    },
})
A['sp_finisher'] = anim(2.6, {
    'motion': {
        'position': track([(0, [0, 0, 0]), (0.5, [0, 10, 0]), (0.9, [0, 12, 0]), (1.2, [0, 11.5, 0]), (1.5, [0, -1, 0]), (1.7, [0, 0.8, 0]),
                           (1.9, [0, 0, 0]), (2.6, [0, 0, 0])]),
        'rotation': track([(0, [0, 0, 0]), (0.4, [-15, 0, 0]), (0.9, [0, 0, 8]), (1.4, [15, 0, 0]), (1.9, [0, 0, 0]), (2.6, [0, 0, 0])]),
        'scale': track([(0, [1, 1, 1]), (0.5, [1.2, 1.2, 1.2]), (1.2, [1.3, 1.3, 1.3]), (1.5, [1.35, 0.7, 1.35]), (1.75, [0.95, 1.05, 0.95]),
                        (2.0, [1, 1, 1]), (2.6, [1, 1, 1])]),
    },
    'bolt': {'position': track([(0, [0, 0, 0]), (1.0, [0, 2, 0]), (1.5, [0, 0, 0]), (2.6, [0, 0, 0])])},
    'ring': {'rotation': track([(0, [0, 0, 0]), (0.6, [0, -360, 0]), (1.2, [0, -720, 0]), (1.5, [0, -900, 0]), (2.6, [0, -1080, 0])], smooth=False)},
})
A['sp_upper'] = anim(1.0, {
    'motion': {
        'rotation': track([(0, [0, 0, 0]), (0.15, [35, 0, 0]), (0.45, [20, 0, 0]), (0.7, [0, 0, 0]), (1.0, [0, 0, 0])]),
        'position': track([(0, [0, 0, 0]), (0.3, [0, 4, 0]), (0.6, [0, 0, 0]), (1.0, [0, 0, 0])]),
    },
})


def main():
    geo_dir = os.path.join(ROOT, 'geo')
    an_dir = os.path.join(ROOT, 'animations')
    os.makedirs(geo_dir, exist_ok=True)
    os.makedirs(an_dir, exist_ok=True)
    with open(os.path.join(geo_dir, 'beyblade.geo.json'), 'w') as f:
        json.dump(geo(), f, indent=1)
    data = {'format_version': '1.8.0', 'animations': {'animation.beyblade.' + k: v for k, v in A.items()}}
    with open(os.path.join(an_dir, 'beyblade.animation.json'), 'w') as f:
        json.dump(data, f, indent=1)
    print('geo + %d animations written' % len(A))


if __name__ == '__main__':
    main()
