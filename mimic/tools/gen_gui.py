#!/usr/bin/env python3
"""GUI art (vignette, flashlight beam, hiding overlays, blood, the jump scare), the film shader and the item icons."""
import json, math, os, random, sys
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(HERE, "../common/src/main/resources/assets/mimicry")
sys.path.insert(0, HERE)


def save(im, rel):
    p = os.path.join(A, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    im.save(p)


def gui():
    S = 256
    yy, xx = np.mgrid[0:S, 0:S]
    r = np.sqrt(((xx - S / 2) / (S / 2)) ** 2 + ((yy - S / 2) / (S / 2)) ** 2)
    # vignette: black edges
    a = np.clip((r - 0.45) / 0.75, 0, 1) ** 1.4 * 255
    save(Image.fromarray(np.dstack([np.zeros((S, S, 3), np.uint8), a.astype(np.uint8)]), "RGBA"), "textures/gui/vignette.png")
    # the beam: dark everywhere but a warm soft disc a little above centre
    rb = np.sqrt(((xx - S / 2) / (S * 0.30)) ** 2 + ((yy - S * 0.48) / (S * 0.30)) ** 2)
    a = np.clip((rb - 0.55) / 0.9, 0, 1) ** 1.2 * 235
    ring = np.exp(-((rb - 0.62) / 0.06) ** 2) * 18
    rgb = np.zeros((S, S, 3))
    rgb[..., 0] = 10 + ring
    rgb[..., 1] = 8 + ring * 0.8
    rgb[..., 2] = 6
    save(Image.fromarray(np.dstack([rgb.astype(np.uint8), np.clip(a - ring * 3, 0, 255).astype(np.uint8)]), "RGBA"), "textures/gui/beam.png")
    # noise frames
    for i in range(4):
        rng = np.random.default_rng(100 + i)
        v = rng.integers(0, 255, (64, 64), dtype=np.uint8)
        save(Image.fromarray(np.dstack([v, v, v, np.full((64, 64), 255, np.uint8)]), "RGBA"), f"textures/gui/noise_{i}.png")
    # wardrobe slats
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 255))
    d = ImageDraw.Draw(im)
    for y in range(4, 64, 8):
        d.rectangle([6, y, 58, y + 2], fill=(0, 0, 0, 0))
    save(im, "textures/gui/slats.png")
    # inside the hay: straws across the view, a gap in the middle to watch through
    rr = random.Random(5)
    HS = 256
    im = Image.new("RGBA", (HS, HS), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    yy, xx = np.mgrid[0:HS, 0:HS]
    gapd = np.sqrt(((xx - HS / 2) / (HS * 0.34)) ** 2 + ((yy - HS * 0.5) / (HS * 0.16)) ** 2)
    for _ in range(1400):
        x, y = rr.randint(0, HS - 1), rr.randint(0, HS - 1)
        if gapd[y, x] < 1.0 and rr.random() < 0.85:
            continue
        ln = rr.randint(10, 46)
        ang = rr.uniform(-0.5, 0.5) + (0 if rr.random() < 0.6 else math.pi / 2)
        c = rr.randint(70, 170)
        d.line([(x, y), (x + math.cos(ang) * ln, y + math.sin(ang) * ln)], fill=(c, int(c * 0.8), int(c * 0.42), 255), width=rr.choice((1, 2, 2, 3)))
    dark = Image.fromarray(np.dstack([np.zeros((HS, HS, 3), np.uint8), (np.clip(gapd - 0.6, 0, 1) * 200).astype(np.uint8)]), "RGBA")
    dark.alpha_composite(im)
    im = dark
    save(im, "textures/gui/hay.png")
    # blood running down the screen
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    rr = random.Random(9)
    for _ in range(26):
        x = rr.randint(0, S)
        w = rr.randint(2, 9)
        ln = rr.randint(30, 200)
        d.line([(x, 0), (x + rr.randint(-6, 6), ln)], fill=(110, 0, 4, 230), width=w)
        d.ellipse([x - w, ln - w, x + w, ln + w], fill=(110, 0, 4, 230))
    for _ in range(40):
        x, y, rad = rr.randint(0, S), rr.randint(0, S // 2), rr.randint(4, 22)
        d.ellipse([x - rad, y - rad, x + rad, y + rad], fill=(90, 0, 2, 200))
    im = im.filter(ImageFilter.GaussianBlur(1.2))
    save(im, "textures/gui/blood.png")
    # the eye: four states of waking
    im = Image.new("RGBA", (32, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for k in range(4):
        y0 = k * 16
        op = [1, 3, 5, 7][k]
        d.ellipse([3, y0 + 8 - op, 29, y0 + 8 + op], outline=(240, 230, 210, 255), width=1)
        if k:
            d.ellipse([13, y0 + 8 - min(op, 3), 19, y0 + 8 + min(op, 3)], fill=(200, 30, 20, 255))
        d.line([(3, y0 + 8), (29, y0 + 8)], fill=(240, 230, 210, 255) if k == 0 else (0, 0, 0, 0))
    save(im, "textures/gui/eye.png")
    paper()
    jumpscare()


def paper():
    """old ruled paper: yellowed, creased, a coffee ring, a smear of something darker"""
    import texlib
    W, H = 256, 320
    a = texlib.ramp(texlib.noise(W, H, 40, 77, 4, tile=False), (178, 160, 118), (222, 208, 170))
    for y in range(36, H - 10, 14):
        a[y, 14:W - 14] = a[y, 14:W - 14] * 0.86 + np.array((120, 140, 170)) * 0.14
    a[:, 30] = a[:, 30] * 0.8 + np.array((170, 80, 80)) * 0.2
    a = texlib.stains(a, 6, (120, 96, 60), 78, 6, 22, 0.35)
    a = texlib.stains(a, 3, (80, 10, 8), 79, 3, 9, 0.55, True)
    yy, xx = np.mgrid[0:H, 0:W]
    edge = np.minimum(np.minimum(xx, W - 1 - xx), np.minimum(yy, H - 1 - yy))
    a *= (0.75 + 0.25 * np.clip(edge / 18, 0, 1))[..., None]
    crease = np.abs(yy - H * 0.52) < 1.2
    a[crease] *= 0.88
    save(texlib.img(texlib.grain(a, 80, 5)), "textures/gui/paper.png")


def jumpscare():
    """the creature's face, mouth unhinged, rendered with the offline previewer"""
    from preview import Anim, build_quads, render
    out = os.path.join(HERE, "out")
    geo = json.load(open(os.path.join(out, "mimic.geo.json")))
    A_ = json.load(open(os.path.join(out, "mimic.animation.json")))
    tex = Image.open(os.path.join(out, "mimic.png"))
    im = render(build_quads(geo, Anim(A_, "animation.mimic.scream"), 1.0), tex, 512, 0, -6, 32, (0, 50, 0), fov=50, bg=(6, 2, 2),
                light=(0.2, 0.6, -0.8), ground=False)
    a = np.asarray(im, float)
    a[..., 0] = np.clip(a[..., 0] * 1.25 + 10, 0, 255)
    a[..., 1] *= 0.8
    a[..., 2] *= 0.8
    im = Image.fromarray(a.astype(np.uint8)).crop((0, 64, 512, 448)).convert("RGBA")
    save(im, "textures/gui/jumpscare.png")


VSH = """#version 150
in vec4 Position;
uniform mat4 ProjMat;
uniform vec2 InSize;
uniform vec2 OutSize;
out vec2 texCoord;
void main() {
    vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);
    texCoord = Position.xy / OutSize;
}
"""

FILM_FSH = """#version 150
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Time;
in vec2 texCoord;
out vec4 fragColor;
float hash(vec2 p) { return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453); }
void main() {
    float t = Time * 20.0;
    vec2 d = texCoord - 0.5;
    // a little chromatic fringe toward the edges, like an old lens
    float ca = 0.0016 * length(d) * 2.0;
    vec3 c;
    c.r = texture(DiffuseSampler, texCoord + d * ca).r;
    c.g = texture(DiffuseSampler, texCoord).g;
    c.b = texture(DiffuseSampler, texCoord - d * ca).b;
    float l = dot(c, vec3(0.30, 0.59, 0.11));
    // drained, sickly: the colour of a farm at three in the morning
    c = mix(vec3(l), c, 0.62);
    c *= vec3(1.04, 0.98, 0.90);
    c = pow(c, vec3(0.86)) * 1.02;
    c = (c - 0.03) * 1.06;
    float n = hash(texCoord * InSize + vec2(t * 13.0, t * 29.0));
    c += (n - 0.5) * 0.05;
    c *= 0.70 + 0.30 * smoothstep(0.88, 0.28, length(d));
    fragColor = vec4(max(c, 0.0), 1.0);
}
"""


def shaders():
    base = os.path.join(A, "shaders")
    os.makedirs(base + "/program", exist_ok=True)
    os.makedirs(base + "/post", exist_ok=True)
    name = "film"
    open(f"{base}/program/{name}.vsh", "w").write(VSH)
    open(f"{base}/program/{name}.fsh", "w").write(FILM_FSH)
    json.dump({"blend": {"func": "add", "srcrgb": "one", "dstrgb": "zero"}, "vertex": "mimicry:" + name, "fragment": "mimicry:" + name,
               "attributes": ["Position"], "samplers": [{"name": "DiffuseSampler"}],
               "uniforms": [{"name": "ProjMat", "type": "matrix4x4", "count": 16, "values": [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1]},
                            {"name": "InSize", "type": "float", "count": 2, "values": [1, 1]},
                            {"name": "OutSize", "type": "float", "count": 2, "values": [1, 1]},
                            {"name": "Time", "type": "float", "count": 1, "values": [0]}]}, open(f"{base}/program/{name}.json", "w"), indent=1)
    json.dump({"targets": ["swap"], "passes": [{"name": "mimicry:" + name, "intarget": "minecraft:main", "outtarget": "swap"},
                                               {"name": "blit", "intarget": "swap", "outtarget": "minecraft:main"}]}, open(f"{base}/post/{name}.json", "w"), indent=1)


# ------------------------------------------------------------------ item icons, 32px
def icon(draw_fn):
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    draw_fn(d)
    return im


def items():
    I = {}
    def flashlight(d):
        d.polygon([(4, 22), (18, 8), (22, 12), (8, 26)], fill=(40, 40, 44, 255))
        d.polygon([(18, 8), (22, 4), (28, 10), (22, 12)], fill=(70, 70, 76, 255))
        d.line([(22, 4), (28, 10)], fill=(250, 230, 160, 255), width=2)
        d.line([(10, 18), (13, 21)], fill=(160, 30, 20, 255), width=2)
    I["flashlight"] = icon(flashlight)
    def battery(d):
        d.rectangle([11, 6, 21, 27], fill=(30, 30, 30, 255))
        d.rectangle([11, 6, 21, 12], fill=(190, 150, 40, 255))
        d.rectangle([14, 3, 18, 6], fill=(160, 160, 160, 255))
        d.text((12, 15), "D", fill=(220, 220, 220, 255))
    I["battery"] = icon(battery)
    def key(col):
        def f(d):
            d.ellipse([4, 10, 14, 20], outline=col, width=3)
            d.line([(14, 15), (28, 15)], fill=col, width=3)
            d.line([(24, 15), (24, 20)], fill=col, width=2)
            d.line([(27, 15), (27, 19)], fill=col, width=2)
        return f
    I["house_key"] = icon(key((176, 140, 60, 255)))
    def truck_keys(d):
        key((150, 150, 156, 255))(d)
        d.rectangle([2, 4, 10, 10], fill=(140, 20, 20, 255))
        d.line([(9, 8), (8, 12)], fill=(120, 120, 120, 255), width=1)
    I["truck_keys"] = icon(truck_keys)
    def walkie(d):
        d.rectangle([9, 9, 23, 30], fill=(30, 32, 30, 255))
        d.line([(20, 9), (20, 1)], fill=(60, 60, 60, 255), width=2)
        for y in range(13, 21, 2):
            d.line([(11, y), (21, y)], fill=(70, 70, 70, 255))
        d.rectangle([12, 23, 15, 25], fill=(200, 40, 30, 255))
    I["walkie"] = icon(walkie)
    def crowbar(d):
        d.line([(6, 28), (24, 6)], fill=(120, 30, 26, 255), width=3)
        d.line([(24, 6), (28, 8)], fill=(120, 30, 26, 255), width=3)
        d.line([(6, 28), (3, 26)], fill=(120, 30, 26, 255), width=3)
    I["crowbar"] = icon(crowbar)
    def cutters(d):
        d.line([(4, 28), (16, 14)], fill=(180, 30, 30, 255), width=3)
        d.line([(10, 30), (18, 16)], fill=(180, 30, 30, 255), width=3)
        d.polygon([(16, 14), (26, 2), (28, 6), (18, 16)], fill=(140, 140, 146, 255))
        d.polygon([(18, 16), (30, 8), (28, 12), (19, 18)], fill=(110, 110, 116, 255))
    I["bolt_cutters"] = icon(cutters)
    def gas(d):
        d.rectangle([7, 9, 25, 29], fill=(170, 28, 22, 255))
        d.rectangle([9, 11, 23, 27], outline=(120, 16, 12, 255))
        d.line([(22, 9), (27, 3)], fill=(60, 60, 60, 255), width=3)
        d.rectangle([10, 5, 16, 9], fill=(150, 24, 20, 255))
    I["gas_can"] = icon(gas)
    def meat(d):
        d.ellipse([5, 9, 25, 27], fill=(160, 30, 34, 255))
        d.ellipse([9, 13, 20, 22], fill=(210, 80, 80, 255))
        d.line([(22, 12), (29, 5)], fill=(230, 220, 200, 255), width=3)
        d.point([(14, 26), (20, 25)], fill=(90, 0, 0, 255))
    I["meat"] = icon(meat)
    def lighter(d):
        d.rectangle([10, 12, 22, 29], fill=(170, 170, 176, 255))
        d.rectangle([10, 7, 22, 12], fill=(130, 130, 136, 255))
        d.ellipse([13, 1, 19, 8], fill=(255, 170, 50, 255))
        d.ellipse([15, 3, 17, 7], fill=(255, 240, 180, 255))
    I["lighter"] = icon(lighter)
    def fork(d):
        d.line([(4, 30), (22, 10)], fill=(130, 100, 64, 255), width=2)
        for k in range(3):
            d.line([(18 + k * 3, 12 - k * 1), (26 + k * 3, 2 - k * 1)], fill=(110, 110, 116, 255), width=2)
        d.line([(18, 12), (25, 15)], fill=(110, 110, 116, 255), width=2)
    I["pitchfork"] = icon(fork)
    for name, im in I.items():
        save(im, f"textures/item/{name}.png")
        p = os.path.join(A, f"models/item/{name}.json")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        json.dump({"parent": "minecraft:item/handheld" if name in ("crowbar", "pitchfork", "bolt_cutters", "flashlight") else "minecraft:item/generated",
                   "textures": {"layer0": f"mimicry:item/{name}"}}, open(p, "w"))
    p = os.path.join(A, "models/item/mimic_spawn_egg.json")
    json.dump({"parent": "minecraft:item/template_spawn_egg"}, open(p, "w"))


if __name__ == "__main__":
    gui()
    shaders()
    items()
    print("gui ok")
