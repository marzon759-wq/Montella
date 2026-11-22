import sys
from PIL import Image
from preview import render
v = sys.argv[1]; out = sys.argv[2]; glow = float(sys.argv[3]) if len(sys.argv) > 3 else 1
views = [(0, 5), (35, 8), (90, 4), (160, 15), (215, 25), (270, 4)]
ims = [render(v, f"/tmp/claude-0/_v{i}.png", y, p, 520, glow=glow) for i, (y, p) in enumerate(views)]
S = Image.new("RGB", (520 * 3, 520 * 2))
for i, im in enumerate(ims):
    S.paste(im, ((i % 3) * 520, (i // 3) * 520))
S.save(out)
