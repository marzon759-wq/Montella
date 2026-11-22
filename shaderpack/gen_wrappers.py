# Generates the per-dimension program entry points. Run: python3 gen_wrappers.py
import os
P = {
 'gbuffers_basic':        ('gbuffers_solid', ['BASIC']),
 'gbuffers_textured':     ('gbuffers_solid', ['PARTICLES']),
 'gbuffers_textured_lit': ('gbuffers_solid', ['PARTICLES']),
 'gbuffers_terrain':      ('gbuffers_solid', ['TERRAIN']),
 'gbuffers_block':        ('gbuffers_solid', ['BLOCK']),
 'gbuffers_beaconbeam':   ('gbuffers_solid', ['BEAM']),
 'gbuffers_entities':     ('gbuffers_solid', ['ENTITIES']),
 'gbuffers_entities_glowing': ('gbuffers_solid', ['ENTITIES']),
 'gbuffers_hand':         ('gbuffers_solid', ['HAND']),
 'gbuffers_damagedblock': ('gbuffers_overlay', ['DAMAGE']),
 'gbuffers_armor_glint':  ('gbuffers_overlay', ['GLINT']),
 'gbuffers_spidereyes':   ('gbuffers_overlay', ['GLINT']),
 'gbuffers_skybasic':     ('gbuffers_sky', []),
 'gbuffers_skytextured':  ('gbuffers_sky', ['TEXTURED']),
 'gbuffers_clouds':       ('gbuffers_sky', []),
 'gbuffers_water':        ('gbuffers_translucent', []),
 'gbuffers_hand_water':   ('gbuffers_translucent', ['ENTITY_TRANSLUCENT']),
 'gbuffers_weather':      ('gbuffers_weather', []),
 'shadow':                ('shadow', []),
 'deferred':              ('deferred', []),
 'composite':             ('composite', []),
 'composite1':            ('composite1', []),
 'final':                 ('final', []),
}
here = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'MontellaRealism', 'shaders')
for folder, world in (('', 0), ('world-1', -1), ('world1', 1)):
    d = os.path.join(here, folder)
    os.makedirs(d, exist_ok=True)
    for name, (body, defs) in P.items():
        for stage in ('VSH', 'FSH'):
            ext = 'vsh' if stage == 'VSH' else 'fsh'
            lines = ['#version 330 compatibility', '#define ' + stage, '#define WORLD ' + str(world)]
            lines += ['#define ' + x for x in defs]
            if name in ('composite', 'final') and stage == 'FSH':
                lines.append('#include "/lib/buffers.glsl"')
            lines.append('#include "/program/%s.glsl"' % body)
            open(os.path.join(d, name + '.' + ext), 'w').write('\n'.join(lines) + '\n')
print('ok')
