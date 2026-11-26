"""The title screen's art: the BLOOD SEA logo with the blood running off its letters (menu_logo.png), and the badge
for the app credited in the corner (menu_app.png)."""
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "common", "src", "main", "resources", "assets", "bloodsea", "textures", "gui")
SERIF = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
rng = np.random.default_rng(31)


def logo():
    W, H = 1024, 300
    font = ImageFont.truetype(SERIF, 150)
    mask = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(mask)
    text = "BLOOD SEA"
    tw = d.textlength(text, font=font)
    d.text(((W - tw) / 2, 30), text, font=font, fill=255)
    m = np.array(mask).astype(float) / 255
    # blood runs down from the bottom edges of the letters
    drip = m.copy()
    bottoms = []
    for x in range(W):
        col = np.nonzero(m[:, x] > 0.5)[0]
        if len(col):
            bottoms.append((x, col.max()))
    for (x, yb) in bottoms[::3]:
        if rng.random() < 0.16:
            length = int(rng.uniform(12, 95))
            width = rng.uniform(2.0, 5.5)
            for dy in range(length):
                w = width * (1 - dy / length * 0.55)
                y = yb + dy
                if y >= H:
                    break
                x0, x1 = int(x - w / 2), int(x + w / 2) + 1
                drip[y, max(0, x0):min(W, x1)] = 1
            # a bead at the end
            yy, xx = np.ogrid[:H, :W]
            r = width * 0.9
            bead = (xx - x) ** 2 + (yy - (yb + length)) ** 2 <= r * r
            drip[bead] = 1
    a = np.clip(drip, 0, 1)
    # colour: wet dark red, lighter at the top of each letter, a glint
    y = np.linspace(0, 1, H)[:, None]
    noise = rng.normal(0, 0.04, (H, W))
    r = np.clip(0.55 - 0.25 * y + noise + 0.25 * (1 - y) ** 6, 0, 1)
    g = np.clip(0.03 + 0.03 * (1 - y) ** 6 + noise * 0.2, 0, 1)
    b = np.clip(0.04 + noise * 0.2, 0, 1)
    rgb = np.stack([r, g, b], -1)
    # dark rim around everything
    rim = np.array(Image.fromarray((a * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(3))).astype(float) / 255
    shadow = np.array(Image.fromarray((rim * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(5))).astype(float) / 255
    out = np.zeros((H, W, 4))
    out[..., :3] = rgb * a[..., None]
    out[..., 3] = np.maximum(a, shadow * 0.42)
    # highlight along the top edges
    top = np.clip(a - np.roll(a, 3, axis=0), 0, 1)
    out[..., 0] = np.clip(out[..., 0] + top * 0.35, 0, 1)
    out[..., 1] = np.clip(out[..., 1] + top * 0.12, 0, 1)
    out[..., 2] = np.clip(out[..., 2] + top * 0.12, 0, 1)
    Image.fromarray((out * 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, "menu_logo.png"))


def app_badge():
    # a little photo-editor glyph: a rounded tile, a lens, a spark
    S = 128
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for i in range(S):
        t = i / S
        col = (int(250 - 60 * t), int(90 + 40 * t), int(80 + 150 * t), 255)
        d.line([(0, i), (S, i)], fill=col)
    mask = Image.new("L", (S, S), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, S - 1, S - 1], radius=30, fill=255)
    im.putalpha(mask)
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([24, 40, 104, 96], radius=12, outline=(255, 255, 255, 255), width=7)
    d.rectangle([44, 30, 70, 42], fill=(255, 255, 255, 255))
    d.ellipse([46, 50, 82, 86], outline=(255, 255, 255, 255), width=7)
    d.ellipse([88, 48, 96, 56], fill=(255, 255, 255, 255))
    im.save(os.path.join(OUT, "menu_app.png"))


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    logo()
    app_badge()
    print("ok")
