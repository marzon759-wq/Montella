"""Shared exporter: GeckoLib animation json + the server spec (nodes, attacks, per-clip pose tracks) for one kaiju."""
import base64, json, os
import numpy as np
import preview as pv

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "out")


def node_tracks(bones, nodes, anim):
    n = max(1, int(round(anim.len * 20)))
    need = set()
    for _, b, *_ in nodes:
        while b:
            need.add(b)
            b = bones[b].get("parent")
    order = [b for b in bones if b in need]
    out = np.zeros((n, len(nodes), 3))
    for k in range(n):
        t = k / 20
        world = {}
        for name in order:
            b = bones[name]
            parent = world[b["parent"]] if "parent" in b else np.eye(4)
            r = np.array(b.get("rotation", [0, 0, 0]), float)
            ar, ap, asc = anim.pose(name, t)
            if ar is not None:
                r = r + ar
            m = parent @ (pv.M(t=ap) if ap is not None else np.eye(4)) @ pv.about(b["pivot"], pv.rot(r))
            if asc is not None:
                m = m @ pv.about(b["pivot"], np.diag(np.maximum(asc, 1e-4)))
            world[name] = m
        for j, (_, bone, p, *_r) in enumerate(nodes):
            out[k, j] = (world[bone] @ np.array([*p, 1.0]))[:3]
    return out


def pack(arr):
    q = np.clip(np.round(arr * 10), -32767, 32767).astype("<i2")
    return base64.b64encode(q.tobytes()).decode()


def export(spec_id, model_base, clips, attacks, nodes, sever, regions, plates, params):
    geo = json.load(open(os.path.join(OUT, f"{model_base}.geo.json")))
    bones = {b["name"]: b for b in geo["minecraft:geometry"][0]["bones"]}
    anims = {f"animation.kaiju.{name}": c.to_json() for name, c in clips.items()}
    # whole-body turns move the entity instead of the model root (no unwinding at the next clip)
    yaw_tracks = {}
    for name, c in clips.items():
        rot = anims[f"animation.kaiju.{name}"]["bones"].get("root", {}).get("rotation")
        if not isinstance(rot, dict):
            continue
        keys = sorted((float(k), v) for k, v in rot.items())
        if max(abs(v[1]) for _, v in keys) < 100:
            continue
        n = max(1, int(round(c.length * 20)))
        ys = []
        for i in range(n):
            t = i / 20
            y = keys[-1][1][1]
            for (t0, v0), (t1, v1) in zip(keys, keys[1:]):
                if t0 <= t <= t1:
                    f = (t - t0) / (t1 - t0) if t1 > t0 else 0
                    y = v0[1] + (v1[1] - v0[1]) * f
                    break
            ys.append(round(y, 2))
        yaw_tracks[name] = ys
        for k in rot:
            rot[k][1] = 0.0
    with open(os.path.join(OUT, f"{model_base}.animation.json"), "w") as f:
        json.dump({"format_version": "1.8.0", "animations": anims}, f, separators=(",", ":"))
    data = {"format_version": "1.8.0", "animations": anims}
    tracks = {}
    for name in clips:
        a = pv.Anim(data, f"animation.kaiju.{name}")
        a.len = clips[name].length
        tr = node_tracks(bones, nodes, a)
        tracks[name] = {"ticks": int(tr.shape[0]), "loop": clips[name].loop, "data": pack(tr)}
    spec = dict(params)
    spec.update({
        "id": spec_id, "model": model_base,
        "nodes": [{"name": n, "bone": b, "r": r, "region": reg, "sever": sv} for n, b, p, r, reg, sv in nodes],
        "sever_bones": sever, "regions": regions, "plates": plates,
        "clips": {k: dict({"len": c.length, "loop": c.loop, "impacts": [round(t, 3) for t, s in c.impacts if s >= 0.3]},
                           **({"yaw": yaw_tracks[k]} if k in yaw_tracks else {})) for k, c in clips.items()},
        "attacks": attacks, "tracks": tracks,
    })
    with open(os.path.join(OUT, f"{spec_id}.json"), "w") as f:
        json.dump(spec, f, separators=(",", ":"))
    sz = os.path.getsize(os.path.join(OUT, f"{model_base}.animation.json")) / 1e6
    sz2 = os.path.getsize(os.path.join(OUT, f"{spec_id}.json")) / 1e6
    print(f"{spec_id}: clips {len(clips)} attacks {len(attacks)} anim.json {sz:.1f} MB data {sz2:.1f} MB")
