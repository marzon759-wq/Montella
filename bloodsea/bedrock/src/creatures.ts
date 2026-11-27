// The five that cannot be killed, each with its own way of hunting (ported from the Java mod's state machines),
// and the director that decides who comes, where, and when.
import { world, Player, Entity, Vector3, Dimension } from "@minecraft/server";
import {
    SURFACE, setAnim, getAnim, act, sfx, rnd, flatDist, wrapDeg, clamp, isDay, active, st, facing, later,
} from "./util";
import { regionAt } from "./sea";

interface Brain { kind: string; state: string; t: number; life: number; scripted: boolean; mem: Record<string, any>; }
const BRAINS = new Map<string, Brain>();

const START: Record<string, string> = { wake: "follow", gatherer: "assemble", caller: "rise", changer: "disguise", leviathan: "rise" };
const LOOP: Record<string, Record<string, string>> = {
    wake: { follow: "swim", approach: "follow", lunge: "follow", dive: "dive" },
    gatherer: { assemble: "assemble", stalk: "walk", swipe: "walk" },
    caller: { rise: "rise", listen: "idle", stalk: "walk", whisper: "whisper", lunge: "idle", sink: "sink" },
    changer: { disguise: "disguise", peek: "peek", creep: "run", bite: "peek", flee: "run" },
    leviathan: { rise: "rise", loom: "idle", roar: "idle", bite: "idle", sink: "sink" },
};

function brain(e: Entity): Brain {
    let b = BRAINS.get(e.id);
    if (!b) {
        const kind = e.typeId.replace("bloodsea:", "");
        b = { kind, state: START[kind] ?? "idle", t: 0, life: 0, scripted: false, mem: {} };
        BRAINS.set(e.id, b);
    }
    return b;
}

export function scriptCreature(e: Entity, on: boolean): void { if (START[e.typeId.replace("bloodsea:", "")] !== undefined) brain(e).scripted = on; }

function go(b: Brain, s: string): void { b.state = s; b.t = 0; }

function nearest(e: Entity, r: number): Player | undefined {
    return e.dimension.getPlayers({ location: e.location, maxDistance: r, closest: 1, excludeGameModes: ["spectator" as any] })[0];
}

function watchedBy(p: Player, e: Entity, cos: number, h = 1): boolean {
    const eye = p.getHeadLocation();
    const to = { x: e.location.x - eye.x, y: e.location.y + h - eye.y, z: e.location.z - eye.z };
    const l = Math.hypot(to.x, to.y, to.z);
    if (l < 1e-3) return true;
    const v = p.getViewDirection();
    return (to.x * v.x + to.y * v.y + to.z * v.z) / l > cos;
}

/** glide toward a point at a speed, turning to face the way it goes; the distance left */
function glide(e: Entity, tx: number, tz: number, speed: number, y: number): number {
    const dx = tx - e.location.x, dz = tz - e.location.z, d = Math.hypot(dx, dz);
    const yaw0 = e.getRotation().y;
    let x = e.location.x, z = e.location.z, yaw = yaw0;
    if (d > 1e-3) {
        const s = Math.min(speed, d);
        x += dx / d * s; z += dz / d * s;
        yaw = yaw0 + clamp(wrapDeg(Math.atan2(dz, dx) * 180 / Math.PI - 90 - yaw0), -6, 6);
    }
    try { e.teleport({ x, y, z }, { rotation: { x: 0, y: yaw } }); } catch { /* */ }
    return d;
}

function face(e: Entity, at: Vector3, step: number): void {
    const yaw0 = e.getRotation().y;
    const yaw = yaw0 + clamp(wrapDeg(Math.atan2(at.z - e.location.z, at.x - e.location.x) * 180 / Math.PI - 90 - yaw0), -step, step);
    try { e.setRotation({ x: 0, y: yaw }); } catch { /* */ }
}

function strike(p: Player, e: Entity, dmg: number, knock = 0.8): void {
    try { p.applyDamage(dmg, { cause: "entityAttack" as any, damagingEntity: e }); } catch { /* */ }
    const dx = p.location.x - e.location.x, dz = p.location.z - e.location.z, d = Math.hypot(dx, dz) || 1;
    try {
        const ride = p.getComponent("minecraft:riding") as any;
        if (ride?.entityRidingOn) (ride.entityRidingOn.getComponent("minecraft:rideable") as any)?.ejectRider(p);
        p.applyKnockback(dx / d, dz / d, knock, 0.45);
    } catch { /* */ }
}

/** how much noise a player makes (rowing, running, jumping; crouching is silent) */
function noise(p: Player): number {
    if (p.isSneaking) return 0.1;
    const v = p.getVelocity();
    const sp = Math.hypot(v.x, v.z);
    const rowing = !!p.getComponent("minecraft:riding");
    return clamp(0.25 + sp * 4 + (rowing ? 0.4 : 0) + (p.isJumping ? 0.4 : 0), 0, 1.2);
}

function speedOf(p: Player): number {
    const ride = p.getComponent("minecraft:riding") as any;
    const v = (ride?.entityRidingOn ?? p).getVelocity();
    return Math.hypot(v.x, v.z);
}

// ------------------------------------------------------------------ every tick
export function tickCreatures(): void {
    const dim = world.getDimension("overworld");
    for (const e of dim.getEntities({ families: ["bloodsea"] })) {
        const kind = e.typeId.replace("bloodsea:", "");
        if (START[kind] === undefined) continue;
        const b = brain(e);
        const loop = LOOP[kind]?.[b.state];
        if (b.scripted) continue;
        b.t++; b.life++;
        try { think(e, b); } catch (err) { console.warn(`[bloodsea] ${kind}: ${err}`); }
        if (!e.isValid()) { BRAINS.delete(e.id); continue; }
        if (loop && getAnim(e) !== loop && !b.mem.acting) setAnim(e, loop);
        if (b.life % 40 === 0 && !nearest(e, 144)) { e.remove(); BRAINS.delete(e.id); }
    }
}

function action(e: Entity, b: Brain, a: string, ticks: number): void {
    b.mem.acting = true;
    act(e, a);
    later(ticks, () => { b.mem.acting = false; });
}

function think(e: Entity, b: Brain): void {
    switch (b.kind) {
        case "wake": return wake(e, b);
        case "gatherer": return gatherer(e, b);
        case "caller": return caller(e, b);
        case "changer": return changer(e, b);
        case "leviathan": return leviathan(e, b);
    }
}

function wake(e: Entity, b: Brain): void {
    const p = nearest(e, 120);
    const y = SURFACE - 0.6;
    if (!p) return;
    if (isDay() && b.state !== "dive") go(b, "dive");
    switch (b.state) {
        case "follow": {
            const f = facing(p.getRotation().y);
            const side = Math.sin(b.life / 70) * 5;
            const tx = p.location.x - f.x * 22 - f.z * side, tz = p.location.z - f.z * 22 + f.x * side;
            const d = Math.hypot(tx - e.location.x, tz - e.location.z);
            glide(e, tx, tz, Math.min(0.5, 0.08 + d * 0.025), y);
            b.mem.still = speedOf(p) < 0.04 ? (b.mem.still ?? 0) + 1 : Math.max(0, (b.mem.still ?? 0) - 3);
            if (b.life % 160 === 0) sfx(e.dimension, "wake_swim", e.location, 1.6, rnd(0.7, 0.9));
            if (b.mem.still > 120) { go(b, "approach"); sfx(e.dimension, "wake_surface", e.location, 2, 0.8); }
            break;
        }
        case "approach": {
            const dx = p.location.x - e.location.x, dz = p.location.z - e.location.z, d = Math.hypot(dx, dz) || 1;
            glide(e, p.location.x - dx / d * 7, p.location.z - dz / d * 7, 0.22, y);
            if (speedOf(p) > 0.18 && b.t > 40) { b.mem.still = 0; go(b, "follow"); }
            else if (d < 10) { go(b, "lunge"); action(e, b, "lunge", 44); sfx(e.dimension, "wake_lunge", e.location, 3, 0.9); }
            break;
        }
        case "lunge": {
            face(e, p.location, 8);
            if (b.t === 22) {
                const f = facing(e.getRotation().y);
                const head = { x: e.location.x + f.x * 7.5, y: p.location.y, z: e.location.z + f.z * 7.5 };
                if (flatDist(head, p.location) < 6) strike(p, e, 7, 0.9);
            }
            if (b.t > 44) go(b, "dive");
            break;
        }
        case "dive": if (b.t > 45) e.remove(); break;
    }
}

function gatherer(e: Entity, b: Brain): void {
    const p = nearest(e, 110);
    if (!p) return;
    const y = e.location.y;
    switch (b.state) {
        case "assemble":
            face(e, p.location, 3);
            if (b.t === 2) sfx(e.dimension, "gatherer_assemble", e.location, 3, 0.9);
            if (b.t === 85) { go(b, "stalk"); action(e, b, "roar", 48); sfx(e.dimension, "gatherer_roar", e.location, 4, 0.8); }
            break;
        case "stalk": {
            if (b.mem.acting) break;
            const watched = watchedBy(p, e, 0.55, 2);
            const d = glide(e, p.location.x, p.location.z, watched ? 0.045 : 0.14, y);
            if (b.t % 26 === 0) sfx(e.dimension, "gatherer_step", e.location, 1.6, rnd(0.8, 1.0));
            if (Math.random() < 1 / 70) sfx(e.dimension, "gatherer_rattle", e.location, 1.4, rnd(0.8, 1.1));
            if (d < 4.4 && b.t > 20) { go(b, "swipe"); action(e, b, "swipe", 32); sfx(e.dimension, "gatherer_swipe", e.location, 2, 0.8); }
            break;
        }
        case "swipe":
            face(e, p.location, 10);
            if (b.t === 13 && flatDist(p.location, e.location) < 6) strike(p, e, 9, 1.1);
            if (b.t > 34) go(b, "stalk");
            break;
    }
}

function caller(e: Entity, b: Brain): void {
    const p = nearest(e, 120);
    if (!p) return;
    const y = e.location.y;
    const d = flatDist(p.location, e.location);
    if (d < 60 * noise(p)) { b.mem.heard = { x: p.location.x, z: p.location.z }; b.mem.lastHeard = b.life; }
    const fresh = b.life - (b.mem.lastHeard ?? -999) < 12;
    const heard = b.mem.heard;
    switch (b.state) {
        case "rise":
            face(e, p.location, 2);
            if (b.t === 2) sfx(e.dimension, "caller_wade", e.location, 2, 0.7);
            if (b.t > 60) go(b, "listen");
            break;
        case "listen":
            if (heard) face(e, { x: heard.x, y: 0, z: heard.z }, 3);
            if (!b.mem.screamed && d < 16 && watchedBy(p, e, 0.93, 2.4)) {
                b.mem.screamed = true;
                action(e, b, "scream", 48);
                sfx(e.dimension, "caller_scream", e.location, 4, 0.9);
                b.mem.heard = { x: p.location.x, z: p.location.z }; b.mem.lastHeard = b.life;
            } else if (fresh && heard && Math.hypot(heard.x - e.location.x, heard.z - e.location.z) > 2) go(b, "stalk");
            else if (b.t > (b.mem.nextWhisper ?? 200)) { go(b, "whisper"); b.mem.nextWhisper = 140 + Math.random() * 160; }
            else if (b.life - (b.mem.lastHeard ?? 0) > 900) go(b, "sink");
            break;
        case "stalk": {
            if (!heard) { go(b, "listen"); break; }
            const left = glide(e, heard.x, heard.z, b.mem.screamed && b.life - b.mem.lastHeard < 200 ? 0.16 : 0.095, y);
            if (b.t % 22 === 0) sfx(e.dimension, "caller_wade", e.location, 1.2, rnd(0.8, 1.0));
            if (d < 3.4) go(b, "lunge");
            else if (left < 1.2 && !fresh) go(b, "listen");
            break;
        }
        case "whisper":
            face(e, p.location, 4);
            if (b.t === 4) {
                sfx(e.dimension, "caller_whisper", e.location, 2.5, rnd(0.92, 1.04));
                const v = p.getViewDirection();
                sfx(e.dimension, "caller_call", { x: p.location.x - v.x * 2.5, y: p.location.y + 1, z: p.location.z - v.z * 2.5 }, 0.55, rnd(0.95, 1.05));
            }
            if (fresh && b.t > 20) go(b, "stalk");
            else if (b.t > 80) go(b, "listen");
            break;
        case "lunge":
            face(e, p.location, 12);
            if (b.t === 1) { action(e, b, "lunge", 28); sfx(e.dimension, "caller_lunge", e.location, 3, 0.9); }
            if (b.t === 10 && d < 4.8) strike(p, e, 10);
            if (b.t > 30) go(b, "listen");
            break;
        case "sink": if (b.t > 62) e.remove(); break;
    }
}

function changer(e: Entity, b: Brain): void {
    const p = nearest(e, 48);
    if (!p) return;
    const d = flatDist(p.location, e.location);
    const seen = d < 40 && watchedBy(p, e, 0.8, 0.5);
    b.mem.unseen = seen ? 0 : (b.mem.unseen ?? 0) + 1;
    switch (b.state) {
        case "disguise": if (b.mem.unseen > 30) { go(b, "peek"); sfx(e.dimension, "changer_blink", e.location, 0.6, rnd(0.9, 1.1)); } break;
        case "peek":
            face(e, p.location, 8);
            if (seen) { if ((b.t > 2 && Math.random() < 0.66) || b.t > 12) go(b, "disguise"); }
            else if (b.t > 40) go(b, d > 2 ? "creep" : "bite");
            break;
        case "creep":
            if (seen) { go(b, "disguise"); break; }
            glide(e, p.location.x, p.location.z, 0.12, e.location.y);
            if (b.t % 5 === 0) sfx(e.dimension, "changer_scuttle", e.location, 0.8, rnd(0.9, 1.2));
            if (d < 1.9) go(b, "bite");
            break;
        case "bite":
            face(e, p.location, 12);
            if (b.t === 1) action(e, b, "bite", 18);
            if (b.t === 8) { sfx(e.dimension, "changer_bite", e.location, 1.6, 1); if (d < 2.8) strike(p, e, 5, 0.4); }
            if (b.t > 20) { go(b, "flee"); action(e, b, "reveal", 32); }
            break;
        case "flee": {
            const dx = e.location.x - p.location.x, dz = e.location.z - p.location.z, l = Math.hypot(dx, dz) || 1;
            glide(e, e.location.x + dx / l * 8, e.location.z + dz / l * 8, 0.2, e.location.y);
            if (b.t % 4 === 0) sfx(e.dimension, "changer_scuttle", e.location, 1, 1.3);
            if (b.t > 60) { sfx(e.dimension, "changer_squelch", e.location, 1.2, 0.6); e.remove(); }
            break;
        }
    }
}

function leviathan(e: Entity, b: Brain): void {
    const p = nearest(e, 260);
    if (!p) return;
    const reach = () => {
        const f = facing(e.getRotation().y);
        const dx = p.location.x - e.location.x, dz = p.location.z - e.location.z;
        return [dx * f.x + dz * f.z, Math.abs(-dx * f.z + dz * f.x)];
    };
    switch (b.state) {
        case "rise": face(e, p.location, 1.5); if (b.t === 2) sfx(e.dimension, "lev_rise", e.location, 8, 0.8); if (b.t > 100) go(b, "loom"); break;
        case "loom": face(e, p.location, 1.2); if (b.t > 30) go(b, "roar"); break;
        case "roar":
            face(e, p.location, 0.8);
            if (b.t === 1) { action(e, b, "roar", 80); sfx(e.dimension, "lev_roar", e.location, 10, 0.85); }
            if (b.t > 80) { const [f, s] = reach(); go(b, f > -2 && f < 30 && s < 11 ? "bite" : "sink"); }
            break;
        case "bite":
            if (b.t === 1) { action(e, b, "bite", 52); sfx(e.dimension, "lev_bite", e.location, 10, 0.8); }
            if (b.t === 24) { const [f, s] = reach(); if (f > -2 && f < 24 && s < 10) strike(p, e, 40, 1.2); }
            if (b.t > 54) go(b, "sink");
            break;
        case "sink": if (b.t > 100) e.remove(); break;
    }
}

// ------------------------------------------------------------------ the director: who comes, during the story
export function allows(p: Player, kind: string, inCut: boolean): boolean {
    if (!active(p)) return true;
    if (inCut) return false;
    const s = st(p).stage;
    switch (kind) {
        case "wake": return ["night1", "night1b", "dest", "fog"].includes(s);
        case "changer": return !["wake", "oar", "rowing"].includes(s);
        case "caller": case "gatherer": return s === "dest";
        default: return false;
    }
}

function none(p: Player, type: string, r: number): boolean {
    return p.dimension.getEntities({ type: `bloodsea:${type}`, location: p.location, maxDistance: r }).length === 0;
}

/** put one where it should come from */
export function spawnCreature(p: Player, kind: string): Entity | undefined {
    const f = facing(p.getRotation().y);
    const dim = p.dimension;
    let at: Vector3;
    switch (kind) {
        case "wake": at = { x: p.location.x - f.x * 34, y: SURFACE - 0.6, z: p.location.z - f.z * 34 }; break;
        case "gatherer": at = { x: p.location.x + f.x * 30 + rnd(-8, 8), y: SURFACE - 0.2, z: p.location.z + f.z * 30 + rnd(-8, 8) }; break;
        case "caller": { const a = Math.random() * Math.PI * 2; at = { x: p.location.x + Math.cos(a) * 36, y: SURFACE - 0.3, z: p.location.z + Math.sin(a) * 36 }; break; }
        case "leviathan": at = { x: p.location.x + f.x * 28, y: SURFACE - 1, z: p.location.z + f.z * 28 }; break;
        default: {
            const a = Math.atan2(-f.z, -f.x) + rnd(-1, 1), rr = rnd(1.0, 2.0);
            at = { x: p.location.x + Math.cos(a) * rr, y: p.location.y, z: p.location.z + Math.sin(a) * rr };
        }
    }
    try {
        const e = dim.spawnEntity(`bloodsea:${kind}`, at);
        e.setRotation({ x: 0, y: Math.atan2(p.location.z - at.z, p.location.x - at.x) * 180 / Math.PI - 90 });
        brain(e);
        return e;
    } catch { return undefined; }
}

/** every second, for each player in the sea */
export function directCreatures(p: Player, inCut: boolean): void {
    if (inCut) return;
    const day = isDay();
    const reg = regionAt(p.location.x, p.location.z);
    const onRaft = !!p.getComponent("minecraft:riding");
    if (active(p)) {
        if (!day && onRaft && allows(p, "wake", inCut) && none(p, "wake", 110) && Math.random() < 1 / 40) spawnCreature(p, "wake");
        if (allows(p, "changer", inCut) && !onRaft && none(p, "changer", 40) && Math.random() < (day ? 1 / 240 : 1 / 120)) spawnCreature(p, "changer");
        if (reg === "bones" && allows(p, "gatherer", inCut) && none(p, "gatherer", 110) && Math.random() < 1 / 60) spawnCreature(p, "gatherer");
        if (reg === "mirage" && allows(p, "caller", inCut) && none(p, "caller", 120) && Math.random() < 1 / 60) spawnCreature(p, "caller");
        return;
    }
    if (!day && none(p, "wake", 110) && Math.random() < 1 / 25) spawnCreature(p, "wake");
    if (reg === "bones" && none(p, "gatherer", 110) && Math.random() < (day ? 1 / 45 : 1 / 25)) spawnCreature(p, "gatherer");
    if ((reg === "mirage" || !day) && none(p, "caller", 120) && Math.random() < 1 / 30) spawnCreature(p, "caller");
}

/** the story lets one off the leash after its scene */
export function release(p: Player, kind: string): void {
    for (const e of p.dimension.getEntities({ type: `bloodsea:${kind}`, location: p.location, maxDistance: 80 })) {
        const b = brain(e);
        b.scripted = false;
        go(b, kind === "gatherer" ? "stalk" : START[kind]);
    }
}

export function anyHunting(p: Player): boolean {
    return p.dimension.getEntities({ families: ["bloodsea"], location: p.location, maxDistance: 30 })
        .some(e => ["bloodsea:gatherer", "bloodsea:caller", "bloodsea:leviathan"].includes(e.typeId)
            || (e.typeId === "bloodsea:wake" && flatDist(e.location, p.location) < 12));
}

/** the Watcher has seen them: everything in the sea now knows where they are */
export function alertAll(p: Player): void {
    for (const e of p.dimension.getEntities({ families: ["bloodsea"], location: p.location, maxDistance: 120 })) {
        const kind = e.typeId.replace("bloodsea:", "");
        if (START[kind] === undefined) continue;
        const b = brain(e);
        if (b.scripted) continue;
        b.mem.heard = { x: p.location.x, z: p.location.z };
        b.mem.lastHeard = b.life;
        if (kind === "wake" && b.state === "follow") go(b, "approach");
        if (kind === "changer" && b.state === "disguise") go(b, "peek");
    }
}
