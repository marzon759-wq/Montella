// Shared pieces: where the sea is, the raft's frame of reference, a tick clock, each player's story state.
import { world, system, Player, Entity, Vector3, Dimension, RawMessage } from "@minecraft/server";
import { ANIMS } from "./anims.gen";

/** the blood is one row of blocks at this height, far above whatever the world has below */
export const SEA_Y = 280;
/** where things resting on the blood stand (the blood's collision is 10/16 of a block) */
export const SURFACE = SEA_Y + 10 / 16;
/** the raft's deck above its own origin */
export const TOP = 0.55;

/** the map of the sea: the calm middle, then four ways */
export const DRIFT_R = 420;
export const EDGE_R = 1900;
export const THRONE = { x: 0, z: -1450 };
export const MAW = { x: 1450, z: 0 };
export const LIGHTHOUSE = { x: -1450, z: 0 };

export function overworld(): Dimension { return world.getDimension("overworld"); }

export function v3(x: number, y: number, z: number): Vector3 { return { x, y, z }; }
export function add(a: Vector3, b: Vector3): Vector3 { return { x: a.x + b.x, y: a.y + b.y, z: a.z + b.z }; }
export function sub(a: Vector3, b: Vector3): Vector3 { return { x: a.x - b.x, y: a.y - b.y, z: a.z - b.z }; }
export function scale(a: Vector3, k: number): Vector3 { return { x: a.x * k, y: a.y * k, z: a.z * k }; }
export function len(a: Vector3): number { return Math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z); }
export function dist(a: Vector3, b: Vector3): number { return len(sub(a, b)); }
export function flatDist(a: Vector3, b: Vector3): number { return Math.hypot(a.x - b.x, a.z - b.z); }
export function lerp(a: number, b: number, k: number): number { return a + (b - a) * k; }
export function lerpV(a: Vector3, b: Vector3, k: number): Vector3 { return { x: lerp(a.x, b.x, k), y: lerp(a.y, b.y, k), z: lerp(a.z, b.z, k) }; }
export function clamp(x: number, a: number, b: number): number { return x < a ? a : x > b ? b : x; }
export function wrapDeg(a: number): number { a = a % 360; if (a >= 180) a -= 360; if (a < -180) a += 360; return a; }
export function rnd(a: number, b: number): number { return a + Math.random() * (b - a); }

/** a point in a frame (x to the left, y up, z ahead) turned by yaw around an origin: the raft's own frame */
export function local(o: Vector3, yaw: number, lx: number, ly: number, lz: number): Vector3 {
    const a = yaw * Math.PI / 180, c = Math.cos(a), s = Math.sin(a);
    return { x: o.x + lx * c - lz * s, y: o.y + ly, z: o.z + lx * s + lz * c };
}

/** the yaw that looks from a to b */
export function yawTo(a: Vector3, b: Vector3): number { return Math.atan2(b.z - a.z, b.x - a.x) * 180 / Math.PI - 90; }

/** the direction a yaw faces, on the level */
export function facing(yaw: number): Vector3 { const a = yaw * Math.PI / 180; return { x: -Math.sin(a), y: 0, z: Math.cos(a) }; }

export function viewDir(p: Player): Vector3 { return p.getViewDirection(); }

// ------------------------------------------------------------------ the clock
let clock = 0;
const LATER: { at: number; fn: () => void }[] = [];

/** run something some ticks from now */
export function later(ticks: number, fn: () => void): void { LATER.push({ at: clock + Math.max(1, ticks), fn }); }

export function tickClock(): void {
    clock++;
    for (let i = LATER.length - 1; i >= 0; i--) {
        if (LATER[i].at <= clock) {
            const f = LATER[i].fn;
            LATER.splice(i, 1);
            try { f(); } catch (e) { console.warn("[bloodsea] later: " + e); }
        }
    }
}

export function now(): number { return clock; }

// ------------------------------------------------------------------ a player's story, kept on the player
export interface State {
    stage: string;
    t: number;
    x0: number;
    z0: number;
    obj?: string;
    dest?: string;
    s_hope?: number; s_accept?: number; s_courage?: number; s_fury?: number;
    pip_gone?: boolean;
    eye_until?: number;
    eye_free?: boolean;
    bond?: number;
    flags?: Record<string, boolean>;
    endings?: Record<string, boolean>;
    raft_x?: number; raft_z?: number; raft_yaw?: number;
    began?: boolean;
}

const CACHE = new Map<string, State>();

export function st(p: Player): State {
    let s = CACHE.get(p.id);
    if (!s) {
        const raw = p.getDynamicProperty("bloodsea:st");
        s = typeof raw === "string" ? JSON.parse(raw) as State : { stage: "", t: 0, x0: 0, z0: 0 };
        CACHE.set(p.id, s);
    }
    return s;
}

export function save(p: Player): void {
    const s = CACHE.get(p.id);
    if (s) p.setDynamicProperty("bloodsea:st", JSON.stringify(s));
}

export function forget(p: Player): void { CACHE.delete(p.id); }

export function active(p: Player): boolean { const s = st(p).stage; return s !== "" && s !== "done"; }

export function once(p: Player, flag: string): boolean {
    const s = st(p);
    s.flags = s.flags || {};
    if (s.flags[flag]) return false;
    s.flags[flag] = true;
    save(p);
    return true;
}

export function players(): Player[] { return world.getAllPlayers(); }

// ------------------------------------------------------------------ entities, animations
export function animIndex(kind: string, anim: string): number {
    const list = ANIMS[kind];
    if (!list) return 0;
    const i = list.findIndex(a => a[0] === anim);
    return i < 0 ? 0 : i;
}

export function animLength(kind: string, anim: string): number {
    const list = ANIMS[kind];
    const a = list ? list.find(x => x[0] === anim) : undefined;
    return a ? a[1] : 20;
}

export function kindOf(e: Entity): string { return e.typeId.replace("bloodsea:", ""); }

export function setAnim(e: Entity | undefined, anim: string): void {
    if (!e || !e.isValid()) return;
    try { e.setProperty("bloodsea:anim", animIndex(kindOf(e), anim)); } catch { /* not ours */ }
}

export function getAnim(e: Entity): string {
    try {
        const i = e.getProperty("bloodsea:anim") as number;
        const list = ANIMS[kindOf(e)];
        return list && list[i] ? list[i][0] : "";
    } catch { return ""; }
}

/** a one-shot animation, then back to whatever it was doing */
export function act(e: Entity | undefined, action: string, back?: string): void {
    if (!e || !e.isValid()) return;
    const was = back ?? getAnim(e);
    setAnim(e, action);
    later(animLength(kindOf(e), action), () => { if (e.isValid() && getAnim(e) === action) setAnim(e, was); });
}

export function sfx(dim: Dimension, sound: string, at: Vector3, volume = 1, pitch = 1): void {
    try { dim.playSound("bloodsea." + sound, at, { volume, pitch }); } catch { /* unloaded */ }
}

export function playTo(p: Player, sound: string, volume = 1, pitch = 1): void {
    try { p.playSound("bloodsea." + sound, { volume, pitch }); } catch { /* gone */ }
}

export function cmd(target: Player | Dimension, c: string): boolean {
    try { (target as any).runCommand(c); return true; } catch { return false; }
}

export function particle(dim: Dimension, id: string, at: Vector3): void {
    try { dim.spawnParticle(id, at); } catch { /* unloaded */ }
}

export function tr(key: string, ...with_: string[]): RawMessage {
    return with_.length ? { translate: key, with: with_ } : { translate: key };
}

export function isDay(): boolean { const t = world.getTimeOfDay() % 24000; return t < 12600 || t > 23400; }
