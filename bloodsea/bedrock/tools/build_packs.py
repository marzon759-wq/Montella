"""Builds the Bedrock (PE) add-on of Blood Sea from the Java mod's assets.

out/BP  behaviour pack: entities, blocks, items, the compiled story script
out/RP  resource pack:  models, animations (driven by an int property), textures, sounds, language, the sky, the
                        title screen (logo, panorama, splash)
Run after `npx tsc` (which writes out/BP/scripts). Everything here is generated; nothing in out/ is edited by hand.
"""
import json
import math
import os
import re
import shutil

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
JAVA = os.path.abspath(os.path.join(ROOT, "..", "common", "src", "main"))
JA = os.path.join(JAVA, "resources", "assets", "bloodsea")
OUT = os.path.join(ROOT, "out")
BP = os.path.join(OUT, "BP")
RP = os.path.join(OUT, "RP")

VERSION = [1, 0, 0]
MIN_ENGINE = [1, 21, 0]
BP_UUID, BP_DATA, BP_SCRIPT = "c1583f36-e379-474b-be00-7f0cb28006d3", "7e942757-ff2c-4e12-bfc7-b39b9dd1b265", "544bd25a-23ce-4506-a882-80d088e44e2c"
RP_UUID, RP_RES = "2c0a622d-e5a4-40d4-9d30-52b820f3baab", "caa570b1-edf3-4c3a-b327-68d7df900a3b"

# entities: name -> (geometry ids, animations from which file, collision w/h)
ANIMATED = {
    "raft": ("raft", (4.6, 0.7)),
    "elias": ("elias", (0.6, 1.8)),
    "rescuer": ("elias", (0.6, 1.8)),
    "pip": ("pip", (0.5, 0.5)),
    "wake": ("wake", (2.0, 1.0)),
    "gatherer": ("gatherer", (1.4, 4.0)),
    "caller": ("caller", (0.9, 3.2)),
    "changer": ("changer", (0.9, 1.0)),
    "leviathan": ("leviathan", (6.0, 8.0)),
}
GIANTS = ["giant_skull", "giant_ribcage", "giant_ribcage_broken", "leviathan_head", "leviathan_segment", "leviathan_tail",
          "leviathan_tail_end", "maw"]


def wjson(path, obj, compact=False):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        if compact:
            json.dump(obj, f, separators=(",", ":"), ensure_ascii=False)
        else:
            json.dump(obj, f, indent=2, ensure_ascii=False)


def anim_names(model):
    d = json.load(open(os.path.join(JA, "animations", f"{model}.animation.json")))
    return [k.split(".")[-1] for k in d["animations"]]


# ------------------------------------------------------------------ manifests
def manifests():
    wjson(os.path.join(BP, "manifest.json"), {
        "format_version": 2,
        "header": {"name": "Blood Sea §c(Behaviour)", "description": "A story-driven horror survival add-on. by krolasyon",
                   "uuid": BP_UUID, "version": VERSION, "min_engine_version": MIN_ENGINE},
        "modules": [{"type": "data", "uuid": BP_DATA, "version": VERSION},
                    {"type": "script", "language": "javascript", "uuid": BP_SCRIPT, "entry": "scripts/main.js", "version": VERSION}],
        "dependencies": [{"module_name": "@minecraft/server", "version": "1.11.0"},
                         {"module_name": "@minecraft/server-ui", "version": "1.2.0"},
                         {"uuid": RP_UUID, "version": VERSION}],
        "metadata": {"authors": ["krolasyon"]},
    })
    wjson(os.path.join(RP, "manifest.json"), {
        "format_version": 2,
        "header": {"name": "Blood Sea §c(Resources)", "description": "Models, sounds, the red sky and the title screen of Blood Sea.",
                   "uuid": RP_UUID, "version": VERSION, "min_engine_version": MIN_ENGINE},
        "modules": [{"type": "resources", "uuid": RP_RES, "version": VERSION}],
        "dependencies": [{"uuid": BP_UUID, "version": VERSION}],
        "metadata": {"authors": ["krolasyon"]},
    })
    icon = Image.open(os.path.join(JAVA, "resources", "bloodsea_logo.png")).convert("RGB").resize((256, 256))
    for p in (BP, RP):
        icon.save(os.path.join(p, "pack_icon.png"))


# ------------------------------------------------------------------ models, animations, client entities
def anim_controller(name, anims, blend=0.15):
    states = {}
    for i, a in enumerate(anims):
        tr = [{f"s{j}": f"q.property('bloodsea:anim') == {j}"} for j in range(len(anims)) if j != i]
        states[f"s{i}"] = {"animations": [a], "transitions": tr, "blend_transition": blend}
    return {"initial_state": "s0", "states": states}


def models():
    controllers = {}
    os.makedirs(os.path.join(RP, "models", "entity"), exist_ok=True)
    os.makedirs(os.path.join(RP, "animations"), exist_ok=True)
    os.makedirs(os.path.join(RP, "textures", "entity", "bloodsea"), exist_ok=True)
    for g in os.listdir(os.path.join(JA, "geo")):
        # compact: some are megabytes of cubes
        wjson(os.path.join(RP, "models", "entity", g), json.load(open(os.path.join(JA, "geo", g))), compact=True)
    for a in os.listdir(os.path.join(JA, "animations")):
        d = json.load(open(os.path.join(JA, "animations", a)))
        wjson(os.path.join(RP, "animations", a), d, compact=True)
    for t in os.listdir(os.path.join(JA, "textures", "entity")):
        shutil.copy(os.path.join(JA, "textures", "entity", t), os.path.join(RP, "textures", "entity", "bloodsea", t))

    for name, (model, _) in ANIMATED.items():
        anims = anim_names(model)
        ctl = f"controller.animation.bloodsea.{name}"
        controllers[ctl] = anim_controller(name, anims)
        desc = {
            "identifier": f"bloodsea:{name}",
            "materials": {"default": "entity_alphatest"},
            "textures": {"default": f"textures/entity/bloodsea/{name if name != 'rescuer' else 'rescuer'}"},
            "geometry": {"default": f"geometry.bloodsea.{model}"},
            "animations": {**{a: f"animation.{model}.{a}" for a in anims}, "ctrl": ctl},
            "scripts": {"animate": ["ctrl"]},
            "render_controllers": ["controller.render.default"],
        }
        if name in ("elias", "rescuer"):
            desc["spawn_egg"] = {"base_color": "#2a2f3a", "overlay_color": "#8a1010"}
        wjson(os.path.join(RP, "entity", f"{name}.entity.json"), {"format_version": "1.10.0", "minecraft:client_entity": {"description": desc}})
    wjson(os.path.join(RP, "animation_controllers", "bloodsea.animation_controllers.json"),
          {"format_version": "1.10.0", "animation_controllers": controllers})

    # the giant dead: one entity, the shape chosen by a property
    geos = {g: json.load(open(os.path.join(JA, "geo", f"{g}.geo.json")))["minecraft:geometry"][0]["description"]["identifier"] for g in GIANTS}
    wjson(os.path.join(RP, "entity", "giant.entity.json"), {"format_version": "1.10.0", "minecraft:client_entity": {"description": {
        "identifier": "bloodsea:giant", "materials": {"default": "entity_alphatest"},
        "textures": {"default": "textures/entity/bloodsea/giant", "leviathan": "textures/entity/bloodsea/leviathan"},
        "geometry": {g: geos[g] for g in GIANTS}, "render_controllers": ["controller.render.bloodsea.giant"]}}})
    wjson(os.path.join(RP, "render_controllers", "bloodsea.render_controllers.json"), {"format_version": "1.8.0", "render_controllers": {
        "controller.render.bloodsea.giant": {
            "arrays": {"geometries": {"Array.geo": [f"Geometry.{g}" for g in GIANTS]}},
            "geometry": "Array.geo[q.property('bloodsea:kind')]",
            "materials": [{"*": "Material.default"}],
            "textures": ["Texture.default"]},
    }})
    # the crate: a lashed barrel of cargo, made here
    crate_model()
    watcher_model()


def crate_model():
    W, H = 64, 64
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    rng = np.random.default_rng(3)
    a = np.zeros((H, W, 4), np.uint8)
    for y in range(H):
        for x in range(W):
            stave = (x // 4) % 2
            base = np.array([92, 64, 38]) if stave else np.array([110, 78, 46])
            n = rng.normal(0, 8)
            col = np.clip(base + n - (12 if y % 16 in (2, 13) else 0), 0, 255)
            if y % 16 in (3, 12):
                col = np.array([40, 36, 34])  # iron hoops
            a[y, x] = [*col.astype(int), 255]
    Image.fromarray(a, "RGBA").save(os.path.join(RP, "textures", "entity", "bloodsea", "crate.png"))
    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry.bloodsea.crate", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 2, "visible_bounds_height": 2, "visible_bounds_offset": [0, 0.5, 0]},
        "bones": [{"name": "root", "pivot": [0, 0, 0], "cubes": [
            {"origin": [-6, 0, -6], "size": [12, 14, 12], "uv": [0, 0]},
            {"origin": [-7, 3, -7], "size": [14, 1, 14], "uv": [0, 26], "inflate": 0.05},
            {"origin": [-7, 10, -7], "size": [14, 1, 14], "uv": [0, 26], "inflate": 0.05}]}]}]}
    wjson(os.path.join(RP, "models", "entity", "crate.geo.json"), geo)
    wjson(os.path.join(RP, "animations", "crate.animation.json"), {"format_version": "1.8.0", "animations": {
        "animation.crate.bob": {"loop": True, "bones": {"root": {
            "position": [0, "math.sin(q.life_time * 70) * 1.2", 0], "rotation": ["math.sin(q.life_time * 50) * 6", 0, "math.cos(q.life_time * 40) * 5"]}}}}})
    wjson(os.path.join(RP, "entity", "crate.entity.json"), {"format_version": "1.10.0", "minecraft:client_entity": {"description": {
        "identifier": "bloodsea:crate", "materials": {"default": "entity_alphatest"}, "textures": {"default": "textures/entity/bloodsea/crate"},
        "geometry": {"default": "geometry.bloodsea.crate"}, "animations": {"bob": "animation.crate.bob"}, "scripts": {"animate": ["bob"]},
        "render_controllers": ["controller.render.default"]}}})


def watcher_model():
    """the eye in the sun: a camera-facing particle the script re-makes along the sun's direction. Its picture is one of
    eight frames (closed, a crack, ... wide open), chosen by variable.open the script passes in."""
    env = os.path.join(JA, "textures", "environment")
    sun = Image.open(os.path.join(env, "white_sun.png")).convert("RGBA").resize((256, 256))
    eye = Image.open(os.path.join(env, "watcher_eye.png")).convert("RGBA").resize((256, 256))
    pupil = Image.open(os.path.join(env, "watcher_pupil.png")).convert("RGBA")
    N = 8
    atlas = Image.new("RGBA", (256 * N, 256), (0, 0, 0, 0))
    for k in range(N):
        o = k / (N - 1)
        o = o * o * (3 - 2 * o)
        f = sun.copy()
        h = int(128 * o)
        if h > 0:
            band = eye.crop((0, 128 - h, 256, 128 + h))
            f.alpha_composite(band, (0, 128 - h))
            ph = max(2, min(int(256 * 0.31), int(h * 0.92)))
            pp = pupil.resize((54, ph * 2))
            f.alpha_composite(pp, (128 - 27, 128 - ph))
        if k > 0:
            d = ImageDraw.Draw(f)
            cw = int(math.sqrt(max(0, 128 * 128 - h * h)))
            for yy in (128 - h, 128 + h):
                d.rectangle([128 - cw, yy - 1, 128 + cw, yy + 1], fill=(60, 8, 10, 230))
        atlas.paste(f, (256 * k, 0))
    os.makedirs(os.path.join(RP, "textures", "particle"), exist_ok=True)
    atlas.save(os.path.join(RP, "textures", "particle", "bloodsea_watcher.png"))
    wjson(os.path.join(RP, "particles", "watcher.particle.json"), {"format_version": "1.10.0", "particle_effect": {
        "description": {"identifier": "bloodsea:watcher",
                        "basic_render_parameters": {"material": "particles_blend", "texture": "textures/particle/bloodsea_watcher"}},
        "components": {
            "minecraft:emitter_rate_instant": {"num_particles": 1},
            "minecraft:emitter_lifetime_once": {"active_time": 0.05},
            "minecraft:emitter_shape_point": {},
            "minecraft:particle_lifetime_expression": {"max_lifetime": 0.16},
            "minecraft:particle_appearance_billboard": {
                "size": ["variable.size", "variable.size"], "facing_camera_mode": "rotate_xyz",
                "uv": {"texture_width": 256 * N, "texture_height": 256,
                       "uv": [f"math.floor(math.clamp(variable.open, 0, 1) * {N - 1} + 0.5) * 256", 0], "uv_size": [256, 256]}},
        }}})


# ------------------------------------------------------------------ behaviour entities
def still_actor(ident, w, h, props, extra=None, gravity=False):
    comps = {
        "minecraft:collision_box": {"width": w, "height": h},
        "minecraft:physics": {"has_gravity": gravity, "has_collision": gravity},
        "minecraft:pushable": {"is_pushable": False, "is_pushable_by_piston": False},
        "minecraft:damage_sensor": {"triggers": [{"cause": "all", "deals_damage": False}]},
        "minecraft:health": {"value": 1000, "max": 1000},
        "minecraft:persistent": {},
        "minecraft:fire_immune": {},
        "minecraft:knockback_resistance": {"value": 1.0},
        "minecraft:type_family": {"family": ["bloodsea", ident.split(":")[1]]},
        "minecraft:breathable": {"breathes_water": True, "breathes_air": True, "suffocate_time": 0, "total_supply": 1000},
    }
    if extra:
        comps.update(extra)
    return {"format_version": "1.21.0", "minecraft:entity": {
        "description": {"identifier": ident, "is_spawnable": False, "is_summonable": True, "is_experimental": False, "properties": props},
        "components": comps}}


def touchable(e, event, text):
    """a right-click (a tap) fires an entity event the script hears"""
    m = e["minecraft:entity"]
    m["components"]["minecraft:interact"] = {"interactions": [{
        "on_interact": {"filters": {"test": "is_family", "subject": "other", "value": "player"}, "event": event, "target": "self"},
        "interact_text": text, "swing": True}]}
    m["component_groups"] = {"bloodsea:touched": {"minecraft:mark_variant": {"value": 1}}}
    m["events"] = {event: {"add": {"component_groups": ["bloodsea:touched"]}}}
    return e


def behaviours():
    anim_prop = lambda n: {"bloodsea:anim": {"type": "int", "range": [0, max(1, n)], "default": 0, "client_sync": True}}
    for name, (model, (w, h)) in ANIMATED.items():
        if name == "raft":
            continue
        e = still_actor(f"bloodsea:{name}", w, h, anim_prop(len(anim_names(model))))
        if name == "pip":
            touchable(e, "bloodsea:petted", "action.interact.bloodsea.pet")
        wjson(os.path.join(BP, "entities", f"{name}.json"), e)
    # the raft: ridden and steered by the rower; anyone else on it stands on its deck
    raft = still_actor("bloodsea:raft", 4.6, 0.7, anim_prop(2), gravity=True, extra={
        "minecraft:rideable": {"seat_count": 1, "family_types": ["player"], "interact_text": "action.interact.bloodsea.oar",
                               "crouching_skip_interact": True, "controlling_seat": 0,
                               "seats": [{"position": [0, 0.75, -1.6], "lock_rider_rotation": 0}]},
        "minecraft:input_ground_controlled": {},
        "minecraft:behavior.player_ride_tamed": {},
        "minecraft:is_tamed": {},
        "minecraft:movement": {"value": 0.16},
        "minecraft:movement.basic": {},
        "minecraft:is_collidable": {},
        "minecraft:conditional_bandwidth_optimization": {},
    })
    wjson(os.path.join(BP, "entities", "raft.json"), raft)
    wjson(os.path.join(BP, "entities", "crate.json"), touchable(still_actor("bloodsea:crate", 1.0, 1.0, {}), "bloodsea:hauled",
                                                                  "action.interact.bloodsea.crate"))
    wjson(os.path.join(BP, "entities", "giant.json"), still_actor("bloodsea:giant", 1.0, 1.0,
                                                                   {"bloodsea:kind": {"type": "int", "range": [0, len(GIANTS) - 1], "default": 0, "client_sync": True}}))


# ------------------------------------------------------------------ blocks and items
def blocks_items():
    def block(ident, tex, collision_h=16, opaque=True, light=0):
        comps = {
            "minecraft:collision_box": {"origin": [-8, 0, -8], "size": [16, collision_h, 16]} if collision_h else False,
            "minecraft:destructible_by_mining": False,
            "minecraft:destructible_by_explosion": False,
            "minecraft:geometry": "minecraft:geometry.full_block",
            "minecraft:material_instances": {"*": {"texture": tex, "render_method": "opaque" if opaque else "blend"}},
        }
        if light:
            comps["minecraft:light_emission"] = light
        return {"format_version": "1.21.0", "minecraft:block": {"description": {"identifier": ident, "menu_category": {"category": "nature"}},
                                                                 "components": comps}}
    wjson(os.path.join(BP, "blocks", "blood.json"), block("bloodsea:blood", "bloodsea_blood", collision_h=10))
    wjson(os.path.join(BP, "blocks", "flesh.json"), block("bloodsea:flesh", "bloodsea_flesh"))
    wjson(os.path.join(BP, "blocks", "bone_mud.json"), block("bloodsea:bone_mud", "bloodsea_bone_mud"))
    wjson(os.path.join(BP, "blocks", "edge_stone.json"), block("bloodsea:edge_stone", "bloodsea_edge_stone"))
    wjson(os.path.join(BP, "blocks", "lighthouse_lamp.json"), block("bloodsea:lighthouse_lamp", "bloodsea_lighthouse_lamp", light=15))
    tb = os.path.join(RP, "textures", "blocks", "bloodsea")
    os.makedirs(tb, exist_ok=True)
    for n in ("blood_still", "flesh", "bone_mud", "edge_stone", "lighthouse_lamp", "lighthouse_wall"):
        shutil.copy(os.path.join(JA, "textures", "block", f"{n}.png"), os.path.join(tb, f"{n}.png"))
    wjson(os.path.join(RP, "textures", "terrain_texture.json"), {"resource_pack_name": "bloodsea", "texture_name": "atlas.terrain", "padding": 8,
                                                                 "num_mip_levels": 4, "texture_data": {
            "bloodsea_blood": {"textures": "textures/blocks/bloodsea/blood_still"},
            "bloodsea_flesh": {"textures": "textures/blocks/bloodsea/flesh"},
            "bloodsea_bone_mud": {"textures": "textures/blocks/bloodsea/bone_mud"},
            "bloodsea_edge_stone": {"textures": "textures/blocks/bloodsea/edge_stone"},
            "bloodsea_lighthouse_lamp": {"textures": "textures/blocks/bloodsea/lighthouse_lamp"}}})
    wjson(os.path.join(RP, "textures", "flipbook_textures.json"), [
        {"flipbook_texture": "textures/blocks/bloodsea/blood_still", "atlas_tile": "bloodsea_blood", "ticks_per_frame": 3, "blend_frames": True}])
    wjson(os.path.join(RP, "blocks.json"), {"format_version": [1, 1, 0], "bloodsea:blood": {"sound": "slime"}, "bloodsea:flesh": {"sound": "slime"},
                                            "bloodsea:bone_mud": {"sound": "mud"}, "bloodsea:edge_stone": {"sound": "stone"},
                                            "bloodsea:lighthouse_lamp": {"sound": "glass"}})
    # the map
    ti = os.path.join(RP, "textures", "items", "bloodsea")
    os.makedirs(ti, exist_ok=True)
    shutil.copy(os.path.join(JA, "textures", "item", "sea_map.png"), os.path.join(ti, "sea_map.png"))
    wjson(os.path.join(RP, "textures", "item_texture.json"), {"resource_pack_name": "bloodsea", "texture_name": "atlas.items",
                                                              "texture_data": {"bloodsea_sea_map": {"textures": "textures/items/bloodsea/sea_map"}}})
    # touch controls for phones: things in the hotbar you tap
    from PIL import ImageDraw as _D
    def icon(name, draw):
        im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        draw(_D.Draw(im))
        im.save(os.path.join(ti, f"{name}.png"))
    icon("ctl_oar", lambda d: (d.line([(6, 26), (24, 8)], fill=(120, 84, 48, 255), width=4), d.ellipse([20, 2, 30, 12], fill=(150, 108, 64, 255))))
    icon("ctl_pip", lambda d: (d.ellipse([7, 10, 25, 28], fill=(20, 20, 24, 255)), d.polygon([(8, 13), (10, 3), (15, 11)], fill=(20, 20, 24, 255)),
                               d.polygon([(24, 13), (22, 3), (17, 11)], fill=(20, 20, 24, 255)), d.ellipse([11, 15, 14, 18], fill=(200, 220, 40, 255)),
                               d.ellipse([18, 15, 21, 18], fill=(200, 220, 40, 255))))
    icon("ctl_skip", lambda d: (d.polygon([(5, 6), (16, 16), (5, 26)], fill=(230, 230, 230, 255)), d.polygon([(16, 6), (27, 16), (16, 26)], fill=(230, 230, 230, 255))))
    tex = {"bloodsea_sea_map": {"textures": "textures/items/bloodsea/sea_map"}}
    for n in ("ctl_oar", "ctl_pip", "ctl_skip"):
        tex[f"bloodsea_{n}"] = {"textures": f"textures/items/bloodsea/{n}"}
        wjson(os.path.join(BP, "items", f"{n}.json"), {"format_version": "1.21.0", "minecraft:item": {
            "description": {"identifier": f"bloodsea:{n}", "menu_category": {"category": "items"}},
            "components": {"minecraft:icon": f"bloodsea_{n}", "minecraft:max_stack_size": 1, "minecraft:glint": False,
                           "minecraft:display_name": {"value": f"item.bloodsea.{n}"}}}})
    wjson(os.path.join(RP, "textures", "item_texture.json"), {"resource_pack_name": "bloodsea", "texture_name": "atlas.items", "texture_data": tex})
    wjson(os.path.join(BP, "items", "sea_map.json"), {"format_version": "1.21.0", "minecraft:item": {
        "description": {"identifier": "bloodsea:sea_map", "menu_category": {"category": "items"}},
        "components": {"minecraft:icon": "bloodsea_sea_map", "minecraft:max_stack_size": 1,
                       "minecraft:display_name": {"value": "item.bloodsea.sea_map"}}}})


# ------------------------------------------------------------------ sounds
def sounds():
    src = os.path.join(JA, "sounds")
    dst = os.path.join(RP, "sounds", "bloodsea")
    os.makedirs(os.path.join(dst, "music"), exist_ok=True)
    for f in os.listdir(src):
        if f.endswith(".ogg"):
            shutil.copy(os.path.join(src, f), os.path.join(dst, f))
    for f in os.listdir(os.path.join(src, "music")):
        shutil.copy(os.path.join(src, "music", f), os.path.join(dst, "music", f))
    java = json.load(open(os.path.join(JA, "sounds.json")))
    defs = {}
    loud = ("lev_", "gatherer_roar", "omen_bell", "wake_", "caller_scream", "watcher")
    for k, v in java.items():
        files = []
        for s in v["sounds"]:
            if isinstance(s, dict):
                files.append({"name": "sounds/bloodsea/" + s["name"].split(":", 1)[1], "stream": True})
            else:
                files.append("sounds/bloodsea/" + s.split(":", 1)[1])
        if not files or not all(os.path.exists(os.path.join(RP, (f["name"] if isinstance(f, dict) else f) + ".ogg")) for f in files):
            continue
        cat = "music" if k.startswith("music_") else "hostile" if any(k.startswith(p) for p in loud) else "neutral"
        d = {"category": cat, "sounds": files}
        if cat != "music":
            d["max_distance"] = 96 if any(k.startswith(p) for p in loud) else 32
        defs[f"bloodsea.{k}"] = d
    for k in ("talk_blip",):
        defs[f"bloodsea.{k}"]["category"] = "ui"
    wjson(os.path.join(RP, "sounds", "sound_definitions.json"), {"format_version": "1.20.20", "sound_definitions": defs})


# ------------------------------------------------------------------ language
LANG_EXTRA = {
    "en_us": {"item.bloodsea.ctl_oar": "Oar (tap: sit / stand)", "item.bloodsea.ctl_pip": "Call Pip (tap)", "item.bloodsea.ctl_skip": "Skip scene (tap)", "action.interact.bloodsea.oar": "Sit at the oar", "action.interact.bloodsea.pet": "Stroke Pip",
              "action.interact.bloodsea.crate": "Pull it aboard", "item.bloodsea.sea_map": "Hand-drawn Map",
              "entity.bloodsea:pip.name": "Pip", "entity.bloodsea:elias.name": "Elias", "entity.bloodsea:raft.name": "Raft",
              "entity.bloodsea:wake.name": "Wake Follower", "entity.bloodsea:gatherer.name": "Bone Gatherer",
              "entity.bloodsea:caller.name": "Name Caller", "entity.bloodsea:changer.name": "The Changer",
              "entity.bloodsea:leviathan.name": "Leviathan", "entity.bloodsea:crate.name": "Floating Crate",
              "entity.bloodsea:giant.name": "The Giant Dead", "entity.bloodsea:watcher.name": "The Watcher",
              "entity.bloodsea:rescuer.name": "Fisherman",
              "tile.bloodsea:blood.name": "Blood", "tile.bloodsea:flesh.name": "Flesh", "tile.bloodsea:bone_mud.name": "Ossuary Mud",
              "tile.bloodsea:edge_stone.name": "Edge Stone", "tile.bloodsea:lighthouse_lamp.name": "Lighthouse Lamp",
              "ui.bloodsea.welcome.title": "BLOOD SEA",
              "ui.bloodsea.welcome.body": "A story-driven horror survival.\n\nYou wake face down on a raft in an endless sea of blood.\n\nFor the best experience: a new world, Normal difficulty, sound on.\n\n§7by Kyvix Photo Editor\n§7Google Play: play.google.com/store/apps/details?id=com.kyvix.photoeditor",
              "ui.bloodsea.welcome.start": "Begin", "ui.bloodsea.map.title": "Hand-drawn Map",
              "ui.bloodsea.map.close": "Fold the map", "ui.bloodsea.map.where": "You are in: %s\nDistance from the middle of the sea: %s\n\n%s",
              "ui.bloodsea.map.dir": "%s lies %s, about %s blocks away.",
              "dir.bloodsea.n": "north", "dir.bloodsea.ne": "north-east", "dir.bloodsea.e": "east", "dir.bloodsea.se": "south-east",
              "dir.bloodsea.s": "south", "dir.bloodsea.sw": "south-west", "dir.bloodsea.w": "west", "dir.bloodsea.nw": "north-west",
              "ui.bloodsea.ending.close": "Back to the sea", "ui.bloodsea.objective": "§c§lOBJECTIVE§r\n%s"},
    "tr_tr": {"item.bloodsea.ctl_oar": "Kürek (dokun: otur / kalk)", "item.bloodsea.ctl_pip": "Pip'i çağır (dokun)", "item.bloodsea.ctl_skip": "Sahneyi geç (dokun)", "action.interact.bloodsea.oar": "Küreğe otur", "action.interact.bloodsea.pet": "Pip'i sev",
              "action.interact.bloodsea.crate": "Sala çek", "item.bloodsea.sea_map": "Elle Çizilmiş Harita",
              "entity.bloodsea:pip.name": "Pip", "entity.bloodsea:elias.name": "Elias", "entity.bloodsea:raft.name": "Sal",
              "entity.bloodsea:wake.name": "Dümen Suyu Takipçisi", "entity.bloodsea:gatherer.name": "Kemik Toplayıcı",
              "entity.bloodsea:caller.name": "İsim Çağıran", "entity.bloodsea:changer.name": "Değişen",
              "entity.bloodsea:leviathan.name": "Leviathan", "entity.bloodsea:crate.name": "Yüzen Sandık",
              "entity.bloodsea:giant.name": "Dev Ölü", "entity.bloodsea:watcher.name": "Gözcü",
              "entity.bloodsea:rescuer.name": "Balıkçı",
              "tile.bloodsea:blood.name": "Kan", "tile.bloodsea:flesh.name": "Et", "tile.bloodsea:bone_mud.name": "Kemik Çamuru",
              "tile.bloodsea:edge_stone.name": "Kenar Taşı", "tile.bloodsea:lighthouse_lamp.name": "Fener Lambası",
              "ui.bloodsea.welcome.title": "BLOOD SEA",
              "ui.bloodsea.welcome.body": "Hikâye odaklı bir korku ve hayatta kalma macerası.\n\nSonsuz bir kan denizinde, bir salın üstünde yüzüstü uyanıyorsun.\n\nEn iyi deneyim için: yeni bir dünya, Normal zorluk, ses açık.\n\n§7by Kyvix Photo Editor\n§7Google Play: play.google.com/store/apps/details?id=com.kyvix.photoeditor",
              "ui.bloodsea.welcome.start": "Başla", "ui.bloodsea.map.title": "Elle Çizilmiş Harita",
              "ui.bloodsea.map.close": "Haritayı katla", "ui.bloodsea.map.where": "Bulunduğun yer: %s\nDenizin ortasına uzaklık: %s\n\n%s",
              "ui.bloodsea.map.dir": "%s, %s yönünde, yaklaşık %s blok uzakta.",
              "dir.bloodsea.n": "kuzey", "dir.bloodsea.ne": "kuzeydoğu", "dir.bloodsea.e": "doğu", "dir.bloodsea.se": "güneydoğu",
              "dir.bloodsea.s": "güney", "dir.bloodsea.sw": "güneybatı", "dir.bloodsea.w": "batı", "dir.bloodsea.nw": "kuzeybatı",
              "ui.bloodsea.ending.close": "Denize dön", "ui.bloodsea.objective": "§c§lGÖREV§r\n%s"},
}


def language():
    texts = os.path.join(RP, "texts")
    os.makedirs(texts, exist_ok=True)
    for jl, bl in (("en_us", "en_US"), ("tr_tr", "tr_TR")):
        d = json.load(open(os.path.join(JA, "lang", f"{jl}.json")))
        d.update(LANG_EXTRA[jl])
        lines = []
        for k, v in d.items():
            v = v.replace("\n", "~LINEBREAK~")
            lines.append(f"{k}={v}")
        open(os.path.join(texts, f"{bl}.lang"), "w", encoding="utf-8").write("\n".join(lines) + "\n")
    wjson(os.path.join(texts, "languages.json"), ["en_US", "tr_TR"])
    wjson(os.path.join(texts, "language_names.json"), [["en_US", "English (US)"], ["tr_TR", "Türkçe (Türkiye)"]])
    # the title screen's splash: the app it is brought to you by
    wjson(os.path.join(RP, "splashes.json"), {"splashes": ["by Kyvix Photo Editor!", "Don't look at the sun.", "Never touch the blood."]})


# ------------------------------------------------------------------ the sky, the fog, the colour of everything
def sky():
    env = os.path.join(RP, "textures", "environment")
    os.makedirs(env, exist_ok=True)
    sun = Image.open(os.path.join(JA, "textures", "environment", "white_sun.png")).convert("RGBA").resize((64, 64))
    canvas = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    canvas.alpha_composite(sun)
    canvas.save(os.path.join(env, "sun.png"))
    moon = Image.open(os.path.join(JA, "textures", "environment", "blood_moon.png")).convert("RGBA").resize((32, 32))
    phases = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    for i in range(4):
        for j in range(2):
            phases.paste(moon, (i * 32, j * 32))
    phases.save(os.path.join(env, "moon_phases.png"))
    # red fog and red water everywhere in this world
    fog = {"format_version": "1.16.100", "minecraft:fog_settings": {"description": {"identifier": "bloodsea:fog"}, "distance": {
        "air": {"fog_start": 0.25, "fog_end": 0.95, "fog_color": "#4A0A0C", "render_distance_type": "render"},
        "water": {"fog_start": 0, "fog_end": 6, "fog_color": "#3A0204", "render_distance_type": "fixed"}}}}
    wjson(os.path.join(RP, "fogs", "bloodsea_fog.json"), fog)
    biomes = ["default", "ocean", "deep_ocean", "plains", "forest", "desert", "taiga", "swampland", "river", "beach", "jungle", "savanna",
              "mesa", "ice_plains", "cold_ocean", "warm_ocean", "lukewarm_ocean", "frozen_ocean", "deep_cold_ocean", "deep_warm_ocean",
              "deep_lukewarm_ocean", "deep_frozen_ocean", "birch_forest", "roofed_forest", "extreme_hills", "mushroom_island",
              "meadow", "grove", "snowy_slopes", "jagged_peaks", "frozen_peaks", "stony_peaks", "cherry_grove", "mangrove_swamp",
              "dripstone_caves", "lush_caves", "deep_dark", "sunflower_plains", "flower_forest", "cold_taiga", "mega_taiga",
              "savanna_plateau", "stone_beach", "cold_beach", "frozen_river", "bamboo_jungle", "jungle_edge", "pale_garden"]
    wjson(os.path.join(RP, "biomes_client.json"), {"biomes": {b: {"fog_identifier": "bloodsea:fog", "water_surface_color": "#7A0A0C",
                                                                  "water_fog_color": "#3A0204", "water_surface_transparency": 0.9} for b in biomes}})


def title_screen():
    ui = os.path.join(RP, "textures", "ui")
    os.makedirs(ui, exist_ok=True)
    logo = Image.open(os.path.join(JA, "textures", "gui", "menu_logo.png")).convert("RGBA")
    logo.save(os.path.join(ui, "title.png"))
    # a panorama: the blood sea all round, the white sun low in the east, a raft far out
    N = 512
    faces = {0: (0, 0), 1: (90, 0), 2: (180, 0), 3: (270, 0), 4: (0, 90), 5: (0, -90)}
    rng = np.random.default_rng(8)
    for i, (yaw, pitch) in faces.items():
        v, u = np.mgrid[0:N, 0:N]
        x = (u + 0.5) / N * 2 - 1
        y = -((v + 0.5) / N * 2 - 1)
        z = np.ones_like(x)
        d = np.stack([x, y, z], -1)
        d /= np.linalg.norm(d, axis=-1, keepdims=True)
        p = math.radians(pitch)
        dy = d[..., 1] * math.cos(p) + d[..., 2] * math.sin(p)
        dz = -d[..., 1] * math.sin(p) + d[..., 2] * math.cos(p)
        dx = d[..., 0]
        a = math.radians(yaw)
        wx = dx * math.cos(a) + dz * math.sin(a)
        wz = -dx * math.sin(a) + dz * math.cos(a)
        el = np.degrees(np.arcsin(np.clip(dy, -1, 1)))
        az = np.degrees(np.arctan2(wx, wz))
        img = np.zeros((N, N, 3))
        # sky
        t = np.clip(el / 60, 0, 1)
        img[..., 0] = 90 * (1 - t) + 14 * t
        img[..., 1] = 10 * (1 - t) + 2 * t
        img[..., 2] = 12 * (1 - t) + 4 * t
        # sun at az 40, el 14
        sx, sy = 40, 14
        sv = np.array([math.sin(math.radians(sx)) * math.cos(math.radians(sy)), math.sin(math.radians(sy)), math.cos(math.radians(sx)) * math.cos(math.radians(sy))])
        dist = np.degrees(np.arccos(np.clip(wx * sv[0] + dy * sv[1] + wz * sv[2], -1, 1)))
        img[dist < 6] = [246, 240, 232]
        glow = np.exp(-(dist / 18) ** 2) * 80
        img[..., 0] += glow; img[..., 1] += glow * 0.7; img[..., 2] += glow * 0.6
        # the sea below the horizon
        sea = el < 0
        depth = np.clip(-el / 30, 0, 1)
        wave = np.sin(az * 0.9 + (1 / np.maximum(0.02, -dy)) * 3.0) * 0.5 + 0.5
        sr = 70 * (1 - depth) + 20 * depth + wave * 12 * (1 - depth)
        img[..., 0] = np.where(sea, sr, img[..., 0])
        img[..., 1] = np.where(sea, 4 + wave * 2, img[..., 1])
        img[..., 2] = np.where(sea, 6, img[..., 2])
        # the road of light under the sun
        road = sea & (np.abs(az - sx) < (2 + depth * 6)) & (wave > 0.55)
        img[road] = img[road] * 0.4 + np.array([230, 190, 170]) * 0.6
        # a raft far out, a figure on it
        rx = -30
        raft = sea & (np.abs(az - rx) < 1.6) & (el > -1.2)
        img[raft] = [18, 6, 6]
        man = (np.abs(az - (rx + 0.5)) < 0.25) & (el >= 0) & (el < 1.4)
        img[man] = [12, 4, 4]
        img += rng.normal(0, 2.5, img.shape)
        Image.fromarray(np.clip(img, 0, 255).astype(np.uint8)).save(os.path.join(ui, f"panorama_{i}.png"))


def main():
    if os.path.isdir(OUT):
        keep = os.path.join(BP, "scripts")
        tmp = None
        if os.path.isdir(keep):
            tmp = os.path.join(ROOT, ".scripts_keep")
            shutil.rmtree(tmp, ignore_errors=True)
            shutil.copytree(keep, tmp)
        shutil.rmtree(OUT)
        if tmp:
            shutil.copytree(tmp, keep)
            shutil.rmtree(tmp)
    os.makedirs(BP, exist_ok=True)
    os.makedirs(RP, exist_ok=True)
    manifests()
    models()
    behaviours()
    blocks_items()
    sounds()
    language()
    sky()
    title_screen()
    print("packs built in", OUT)


if __name__ == "__main__":
    main()
