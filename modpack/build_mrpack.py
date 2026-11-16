#!/usr/bin/env python3
"""Builds Valdoria.mrpack (Modrinth modpack format; imports into the Modrinth App, Prism Launcher, ATLauncher...).

Third-party mods and shader packs are referenced by their Modrinth downloads (not re-uploaded), as their licenses ask.
Valdoria itself and Valdoria Shaders are included as overrides.
"""
import hashlib, json, os, sys, urllib.request, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.join(HERE, "..")
VERSION = sys.argv[1] if len(sys.argv) > 1 else "0.5.0"

# Modrinth version ids
DEPS = {
    "geckolib": ("aC5KMoNg", "mods", "required"),
    "terrablender": ("IfcgopfA", "mods", "required"),
    "embeddium": ("UTbfe5d1", "mods", "required"),
    "oculus": ("iQ1SwGc3", "mods", "required"),
    "complementary-reimagined": ("Bqen1mJX", "shaderpacks", "optional"),
}


def get(url):
    req = urllib.request.Request(url, headers={"User-Agent": "valdoria-modpack-builder"})
    with urllib.request.urlopen(req) as r:
        return json.load(r)


files = []
for name, (vid, folder, client) in DEPS.items():
    v = get(f"https://api.modrinth.com/v2/version/{vid}")
    f = next((x for x in v["files"] if x["primary"]), v["files"][0])
    files.append({
        "path": f"{folder}/{f['filename']}",
        "hashes": {"sha1": f["hashes"]["sha1"], "sha512": f["hashes"]["sha512"]},
        "env": {"client": client, "server": "unsupported" if folder == "shaderpacks" or name in ("oculus", "embeddium") else "required"},
        "downloads": [f["url"]],
        "fileSize": f["size"],
    })
    print("dep", name, f["filename"])

index = {
    "formatVersion": 1, "game": "minecraft", "versionId": VERSION, "name": "Valdoria: Seven Realms",
    "summary": "A fantasy world of magic, dragons, kingdoms and dark realms.",
    "files": files,
    "dependencies": {"minecraft": "1.20.1", "forge": "47.4.10"},
}

jar = sorted(f for f in os.listdir(os.path.join(REPO, "dist")) if f.startswith("valdoria-1.20.1-forge-"))[-1]
out = os.path.join(HERE, f"Valdoria-{VERSION}.mrpack")
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    z.writestr("modrinth.index.json", json.dumps(index, indent=2))
    z.write(os.path.join(REPO, "dist", jar), f"overrides/mods/{jar}")
    for root, _, fs in os.walk(os.path.join(HERE, "overrides")):
        for f in fs:
            p = os.path.join(root, f)
            z.write(p, os.path.relpath(p, HERE))
    # Valdoria Shaders as a zipped shader pack
    shaders = os.path.join(REPO, "valdoria-shaders")
    import io
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as sz:
        for root, _, fs in os.walk(os.path.join(shaders, "shaders")):
            for f in fs:
                p = os.path.join(root, f)
                sz.write(p, os.path.relpath(p, shaders))
        sz.write(os.path.join(shaders, "README.md"), "README.md")
    z.writestr("overrides/shaderpacks/ValdoriaShaders.zip", buf.getvalue())
print("built", out)
