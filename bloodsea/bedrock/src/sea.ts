// The sea itself: the blood laid down around wherever you are, the regions and what stands in them, the raft,
// the rule that the blood kills, and the crates of cargo drifting past.
import { world, system, Player, Entity, Dimension, Vector3, GameMode, ItemStack, EntityInventoryComponent } from "@minecraft/server";
import {
    SEA_Y, SURFACE, TOP, DRIFT_R, EDGE_R, THRONE, MAW, LIGHTHOUSE, overworld, local, st, save, active, later, sfx, cmd, particle,
    rnd, flatDist, yawTo, setAnim,
} from "./util";

// ------------------------------------------------------------------ noise (the Java mod's, so the sea has the same shape)
function hash(x: number, z: number, seed: number): number {
    // 32-bit mix: not the Java mod's 64-bit numbers, but the same idea and stable across runs
    let h = Math.imul(x | 0, 0x27d4eb2d) ^ Math.imul(z | 0, 0x165667b1) ^ Math.imul(seed | 0, 0x9e3779b1);
    h ^= h >>> 15; h = Math.imul(h, 0x85ebca6b); h ^= h >>> 13; h = Math.imul(h, 0xc2b2ae35); h ^= h >>> 16;
    return h >>> 0;
}
export function rand(x: number, z: number, seed: number): number { return hash(x, z, seed) / 4294967296; }
function smooth(t: number): number { return t * t * (3 - 2 * t); }
export function value(x: number, z: number, sc: number, seed: number): number {
    const fx = x / sc, fz = z / sc, ix = Math.floor(fx), iz = Math.floor(fz), tx = smooth(fx - ix), tz = smooth(fz - iz);
    const a = rand(ix, iz, seed), b = rand(ix + 1, iz, seed), c = rand(ix, iz + 1, seed), d = rand(ix + 1, iz + 1, seed);
    return (a + (b - a) * tx + (c - a) * tz + (a - b - c + d) * tx * tz) * 2 - 1;
}
export function fbm(x: number, z: number, sc: number, seed: number): number {
    return value(x, z, sc, seed) * 0.6 + value(x, z, sc / 2.3, seed + 7) * 0.28 + value(x, z, sc / 5.1, seed + 13) * 0.12;
}

export type Region = "drift" | "bones" | "leviathan" | "edge" | "mirage";

export function regionAt(x: number, z: number): Region {
    const r = Math.hypot(x, z);
    const wob = value(x, z, 160, 91) * 60;
    if (r < DRIFT_R + wob) return "drift";
    let ang = Math.atan2(x, -z) * 180 / Math.PI + value(x, z, 260, 92) * 12;
    if (ang < 0) ang += 360;
    if (ang >= 315 || ang < 45) return "bones";
    if (ang < 135) return "leviathan";
    if (ang < 225) return "edge";
    return "mirage";
}

const S = 4471;

// ------------------------------------------------------------------ laying the blood down
const DONE = new Set<string>();
const QUEUE: [number, number][] = [];

function fill(dim: Dimension, x0: number, y0: number, z0: number, x1: number, y1: number, z1: number, block: string): void {
    cmd(dim, `fill ${x0} ${y0} ${z0} ${x1} ${y1} ${z1} ${block}`);
}

function setB(dim: Dimension, x: number, y: number, z: number, block: string): void {
    cmd(dim, `setblock ${x} ${y} ${z} ${block}`);
}

/** every second: the chunks around each player get their blood (and whatever else stands there), a few at a time */
export function keepSea(): void {
    const dim = overworld();
    for (const p of world.getAllPlayers()) {
        if (!active(p)) continue;
        const cx = Math.floor(p.location.x / 16), cz = Math.floor(p.location.z / 16);
        for (let r = 0; r <= 4; r++)
            for (let dx = -r; dx <= r; dx++)
                for (let dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) !== r) continue;
                    const k = `${cx + dx},${cz + dz}`;
                    if (DONE.has(k)) continue;
                    QUEUE.push([cx + dx, cz + dz]);
                    DONE.add(k);
                }
    }
    let n = 0;
    while (QUEUE.length && n < 6) {
        const [cx, cz] = QUEUE.shift()!;
        if (!layChunk(dim, cx, cz)) { DONE.delete(`${cx},${cz}`); }
        n++;
    }
}

function layChunk(dim: Dimension, cx: number, cz: number): boolean {
    const x0 = cx * 16, z0 = cz * 16;
    let probe;
    try { probe = dim.getBlock({ x: x0 + 8, y: SEA_Y, z: z0 + 8 }); } catch { return false; }
    if (!probe) return false;            // not loaded yet: try again later
    const fresh = probe.typeId === "minecraft:air";
    // the blood, except past the rim at the end of the sea
    const rim = (x: number, z: number) => regionAt(x, z) === "edge" && Math.hypot(x, z) > EDGE_R;
    let any = false;
    for (let x = 0; x < 16; x++) {
        let runStart = -1;
        for (let z = 0; z <= 16; z++) {
            const ok = z < 16 && !rim(x0 + x, z0 + z);
            if (ok && runStart < 0) runStart = z;
            if ((!ok || z === 16) && runStart >= 0) {
                fill(dim, x0 + x, SEA_Y, z0 + runStart, x0 + x, SEA_Y, z0 + z - 1, "bloodsea:blood");
                runStart = -1;
                any = true;
            }
        }
    }
    if (!any || !fresh) { if (!any) edgeRim(dim, x0, z0); return true; }
    const reg = regionAt(x0 + 8, z0 + 8);
    try {
        if (reg === "bones") bones(dim, x0, z0);
        if (reg === "leviathan") leviathan(dim, x0, z0);
        if (reg === "mirage") mirage(dim, x0, z0);
        if (reg === "edge") edgeRim(dim, x0, z0);
        if (Math.abs(x0 + 8 - THRONE.x) < 48 && Math.abs(z0 + 8 - THRONE.z) < 48) throne(dim, x0, z0);
        if (Math.abs(x0 + 8 - LIGHTHOUSE.x) < 24 && Math.abs(z0 + 8 - LIGHTHOUSE.z) < 24) lighthouse(dim, x0, z0);
    } catch (e) { console.warn("[bloodsea] decor: " + e); }
    return true;
}

/** lay the blood around a player now (the opening: there must be a sea before the raft can sit on it) */
export function layNow(p: Player, radius: number): number {
    const dim = overworld();
    const cx = Math.floor(p.location.x / 16), cz = Math.floor(p.location.z / 16);
    let n = 0;
    for (let dx = -radius; dx <= radius; dx++) for (let dz = -radius; dz <= radius; dz++) {
        const k = `${cx + dx},${cz + dz}`;
        if (DONE.has(k)) { n++; continue; }
        if (layChunk(dim, cx + dx, cz + dz)) { DONE.add(k); n++; }
    }
    return n;
}

// ------------------------------------------------------------------ the Bone Reach: mud islands, the dead, the giant dead, the throne
const GROUND = ["minecraft:skeleton_skull", "minecraft:bone_block", "minecraft:skeleton_skull", "minecraft:bone_block"];

function bones(dim: Dimension, x0: number, z0: number): void {
    for (let x = x0; x < x0 + 16; x++)
        for (let z = z0; z < z0 + 16; z++) {
            const v = fbm(x, z, 70, S + 10);
            if (v > 0.3) {
                const top = SEA_Y + Math.min(9, Math.floor((v - 0.3) * 40));
                fill(dim, x, SEA_Y, z, x, top, z, "bloodsea:bone_mud");
                if (rand(x, z, S + 11) < 0.16) setB(dim, x, top + 1, z, GROUND[Math.floor(rand(x, z, S + 12) * GROUND.length)]);
            } else if (rand(x, z, S + 15) < 0.012) {
                setB(dim, x, SEA_Y + 1, z, "minecraft:skeleton_skull");
            }
        }
    // the giant dead: rib cages and skulls, one per cell at most
    const cell = 56;
    for (let gx = Math.floor(x0 / cell); gx <= Math.floor((x0 + 15) / cell); gx++)
        for (let gz = Math.floor(z0 / cell); gz <= Math.floor((z0 + 15) / cell); gz++) {
            if (rand(gx, gz, S + 20) > 0.42) continue;
            const ox = gx * cell + rand(gx, gz, S + 21) * cell, oz = gz * cell + rand(gx, gz, S + 22) * cell;
            if (Math.floor(ox / 16) !== Math.floor(x0 / 16) || Math.floor(oz / 16) !== Math.floor(z0 / 16)) continue;
            if (regionAt(ox, oz) !== "bones" || Math.hypot(ox - THRONE.x, oz - THRONE.z) < 60) continue;
            if (fbm(ox, oz, 70, S + 10) > 0.22) continue;
            const k = rand(gx, gz, S + 24);
            giant(dim, k < 0.22 ? 0 : k < 0.6 ? 1 : 2, { x: ox, y: k < 0.22 ? SEA_Y + 1 : SEA_Y - 1, z: oz }, rand(gx, gz, S + 23) * 360);
        }
}

function throne(dim: Dimension, x0: number, z0: number): void {
    const P = SEA_Y + 17;
    for (let x = x0; x < x0 + 16; x++)
        for (let z = z0; z < z0 + 16; z++) {
            const dt = Math.hypot(x - THRONE.x, z - THRONE.z);
            if (dt >= 40) continue;
            const top = Math.floor(Math.min(P, SEA_Y + 3 + (40 - dt) * 0.42));
            fill(dim, x, SEA_Y, z, x, top, z, "bloodsea:bone_mud");
            if (dt > 5 && rand(x, z, S + 30) < 0.3) setB(dim, x, top + 1, z, rand(x, z, S + 31) < 0.5 ? "minecraft:skeleton_skull" : "minecraft:bone_block");
            const lx = x - THRONE.x, lz = z - THRONE.z;
            if (Math.abs(lx) <= 4 && Math.abs(lz) <= 4) {
                fill(dim, x, SEA_Y, z, x, P, z, "bloodsea:bone_mud");
                if (lz === -2 && Math.abs(lx) <= 2) { fill(dim, x, P + 1, z, x, P + (Math.abs(lx) === 2 ? 5 : 4), z, "minecraft:bone_block"); setB(dim, x, P + 6, z, "minecraft:skeleton_skull"); }
                if (Math.abs(lx) === 2 && lz >= -1 && lz <= 0) setB(dim, x, P + 1, z, "minecraft:bone_block");
                if (lx === 0 && lz === -1) { setB(dim, x, P + 1, z, "minecraft:bone_block"); setB(dim, x, P + 2, z, "minecraft:redstone_block"); }
            }
        }
}

// ------------------------------------------------------------------ Leviathan's Deep: the dead leviathan laid along its spine, and the Maw
function spineZ(x: number): number { return 140 * Math.sin(x / 200); }
let LEV_PARTS: { kind: number; at: Vector3; yaw: number }[] | undefined;
function leviathanParts() {
    if (LEV_PARTS) return LEV_PARTS;
    const out: { kind: number; at: Vector3; yaw: number }[] = [];
    let x = 540, along = 0, next = 0, n = 0;
    while (x < 1300) {
        const z = spineZ(x);
        if (along >= next) {
            const dz = 0.7 * Math.cos(x / 200);
            const yaw = -Math.atan2(1, dz) * 180 / Math.PI;
            out.push({ kind: n === 0 ? 6 : n < 3 ? 5 : 4, at: { x, y: SEA_Y - 4, z }, yaw });
            next += 32; n++;
        }
        const nx = x + 0.25;
        along += Math.sqrt(0.0625 + Math.pow(spineZ(nx) - z, 2));
        x = nx;
    }
    const dz = 0.7 * Math.cos(x / 200);
    out.push({ kind: 3, at: { x: x + 10, y: SEA_Y + 5, z: spineZ(x + 10) }, yaw: -Math.atan2(1, dz) * 180 / Math.PI + 180 });
    out.push({ kind: 7, at: { x: MAW.x + 0.5, y: SEA_Y - 3, z: MAW.z + 0.5 }, yaw: 0 });
    LEV_PARTS = out;
    return out;
}

function leviathan(dim: Dimension, x0: number, z0: number): void {
    for (const g of leviathanParts())
        if (Math.floor(g.at.x / 16) === Math.floor(x0 / 16) && Math.floor(g.at.z / 16) === Math.floor(z0 / 16)) giant(dim, g.kind, g.at, g.yaw);
    // the throat in the middle of the Maw
    if (Math.abs(x0 + 8 - MAW.x) < 16 && Math.abs(z0 + 8 - MAW.z) < 16)
        for (let x = x0; x < x0 + 16; x++) for (let z = z0; z < z0 + 16; z++)
            if (Math.hypot(x - MAW.x, z - MAW.z) < 5) setB(dim, x, SEA_Y, z, "minecraft:black_concrete");
}

function giant(dim: Dimension, kind: number, at: Vector3, yaw: number): void {
    try {
        const e = dim.spawnEntity("bloodsea:giant", at);
        e.setProperty("bloodsea:kind", kind);
        e.setRotation({ x: 0, y: yaw });
    } catch (e) { console.warn("[bloodsea] giant: " + e); }
}

// ------------------------------------------------------------------ the Mirage: islands, some of them there; the lighthouse of home
function mirage(dim: Dimension, x0: number, z0: number): void {
    const cell = 64;
    for (let cx = Math.floor(x0 / cell) - 1; cx <= Math.floor((x0 + 15) / cell) + 1; cx++)
        for (let cz = Math.floor(z0 / cell) - 1; cz <= Math.floor((z0 + 15) / cell) + 1; cz++) {
            if (rand(cx, cz, S + 50) > 0.55) continue;
            const ox = cx * cell + 12 + rand(cx, cz, S + 51) * (cell - 24), oz = cz * cell + 12 + rand(cx, cz, S + 52) * (cell - 24);
            if (regionAt(ox, oz) !== "mirage") continue;
            const R = 7 + rand(cx, cz, S + 53) * 9;
            for (let x = x0; x < x0 + 16; x++) for (let z = z0; z < z0 + 16; z++) {
                const d = Math.hypot(x - ox, z - oz) + value(x, z, 6, S + 54) * 1.5;
                if (d > R) continue;
                const top = SEA_Y + 1 + Math.floor((R - d) * 0.3);
                fill(dim, x, SEA_Y, z, x, top, z, "minecraft:sand");
                if (R - d > 2.5) setB(dim, x, top, z, "minecraft:grass_block");
                if (Math.hypot(x - ox, z - oz) < 0.75 && R > 9) {
                    fill(dim, x, top + 1, z, x, top + 6, z, "minecraft:oak_log");
                    fill(dim, x - 2, top + 5, z - 2, x + 2, top + 7, z + 2, "minecraft:oak_leaves keep");
                }
            }
        }
}

function lighthouse(dim: Dimension, x0: number, z0: number): void {
    const L = LIGHTHOUSE, base = SEA_Y + 3;
    for (let x = x0; x < x0 + 16; x++) for (let z = z0; z < z0 + 16; z++) {
        const dl = Math.hypot(x - L.x, z - L.z);
        if (dl >= 18) continue;
        const top = SEA_Y + 1 + Math.floor(Math.max(0, (18 - dl) * 0.12));
        fill(dim, x, SEA_Y, z, x, top, z, "minecraft:sand");
        if (dl < 15) setB(dim, x, top, z, "minecraft:grass_block");
        if (dl <= 4.4) fill(dim, x, top, z, x, base - 1, z, "minecraft:stone_bricks");
        if (dl <= 3.4) {
            if (dl >= 2.4) fill(dim, x, base, z, x, base + 30, z, "minecraft:white_concrete");
            setB(dim, x, base + 31, z, "minecraft:white_concrete");
            if (dl >= 1.5) fill(dim, x, base + 32, z, x, base + 33, z, "minecraft:glass");
            else fill(dim, x, base + 32, z, x, base + 33, z, "bloodsea:lighthouse_lamp");
            setB(dim, x, base + 34, z, "minecraft:red_concrete");
        }
    }
}

function edgeRim(dim: Dimension, x0: number, z0: number): void {
    for (let x = x0; x < x0 + 16; x++) for (let z = z0; z < z0 + 16; z++) {
        const r = Math.hypot(x, z);
        if (regionAt(x, z) !== "edge" || r < EDGE_R - 1.5 || r > EDGE_R + 0.5) continue;
        setB(dim, x, SEA_Y - 1, z, "bloodsea:edge_stone");
        if (rand(x, z, S + 70) < 0.05) fill(dim, x, SEA_Y + 1, z, x, SEA_Y + 2 + Math.floor(rand(x, z, S + 71) * 6), z, "bloodsea:edge_stone");
    }
}

// ------------------------------------------------------------------ the raft
export function raftOf(p: Player): Entity | undefined {
    const ride = p.getComponent("minecraft:riding") as any;
    if (ride && ride.entityRidingOn && ride.entityRidingOn.typeId === "bloodsea:raft") return ride.entityRidingOn;
    const id = p.getDynamicProperty("bloodsea:raft");
    if (typeof id === "string") {
        const e = world.getEntity(id);
        if (e && e.isValid()) return e;
    }
    const near = p.dimension.getEntities({ type: "bloodsea:raft", location: p.location, maxDistance: 64, closest: 1, excludeTags: ["bloodsea_ghost"] });
    if (near.length) { p.setDynamicProperty("bloodsea:raft", near[0].id); return near[0]; }
    return undefined;
}

/** the raft, made again where it was last seen if it is lost */
export function ensureRaft(p: Player): Entity | undefined {
    const r = raftOf(p);
    if (r) return r;
    const s = st(p);
    const at = { x: s.raft_x ?? 0.5, y: SURFACE, z: s.raft_z ?? 0.5 };
    try {
        const e = p.dimension.spawnEntity("bloodsea:raft", at);
        e.setRotation({ x: 0, y: s.raft_yaw ?? 0 });
        p.setDynamicProperty("bloodsea:raft", e.id);
        return e;
    } catch { return undefined; }
}

export function raftLocal(r: Entity, lx: number, ly: number, lz: number): Vector3 { return local(r.location, r.getRotation().y, lx, ly, lz); }

export function rememberRaft(p: Player, r: Entity): void {
    const s = st(p);
    s.raft_x = r.location.x; s.raft_z = r.location.z; s.raft_yaw = r.getRotation().y;
}

/** back aboard: at the oar, sitting */
export function aboard(p: Player, r: Entity): void {
    const st0 = raftLocal(r, 0, TOP + 0.05, -1.2);
    try { p.teleport(st0, { rotation: { x: 0, y: r.getRotation().y } }); } catch { /* */ }
    later(2, () => { try { (r.getComponent("minecraft:rideable") as any)?.addRider(p); } catch { /* */ } });
}

/** the deck holds you up: anyone standing inside the raft's outline stays on its logs */
export function deckSupport(p: Player): boolean {
    if (p.getComponent("minecraft:riding")) return false;
    const r = raftOf(p);
    if (!r) return false;
    const a = r.getRotation().y * Math.PI / 180;
    const dx = p.location.x - r.location.x, dz = p.location.z - r.location.z;
    const lx = dx * Math.cos(a) + dz * Math.sin(a), lz = -dx * Math.sin(a) + dz * Math.cos(a);
    if (Math.abs(lx) > 2.35 || Math.abs(lz) > 2.35) return false;
    const deck = r.location.y + TOP;
    if (p.location.y < deck - 0.02 && p.location.y > deck - 2.0) {
        try { p.teleport({ x: p.location.x, y: deck + 0.01, z: p.location.z }, { keepVelocity: false }); } catch { /* */ }
    }
    return p.location.y > deck - 2.0;
}

// ------------------------------------------------------------------ the blood kills
const IN_BLOOD = new Map<string, number>();

export function bloodTick(p: Player, inCut: boolean): void {
    if (inCut || !active(p) || deckSupport(p)) { IN_BLOOD.delete(p.id); return; }
    const gm = p.getGameMode();
    if (gm === GameMode.creative || gm === GameMode.spectator) return;
    const riding = !!p.getComponent("minecraft:riding");
    const loc = p.location;
    let touching = false;
    if (!riding) {
        if (loc.y < SEA_Y - 0.5) touching = true;
        else if (loc.y < SEA_Y + 1.0) {
            try {
                const b = p.dimension.getBlock({ x: Math.floor(loc.x), y: SEA_Y, z: Math.floor(loc.z) });
                touching = !!b && b.typeId === "bloodsea:blood";
            } catch { /* */ }
        }
    }
    if (!touching) { IN_BLOOD.delete(p.id); return; }
    const n = (IN_BLOOD.get(p.id) ?? 0) + 1;
    IN_BLOOD.set(p.id, n);
    if (n === 1) {
        sfx(p.dimension, "blood_splash", loc, 1.5, 0.8);
        try { p.addEffect("nausea", 100, { amplifier: 1, showParticles: false }); p.addEffect("wither", 40, { amplifier: 3, showParticles: false }); } catch { /* */ }
    }
    if (n % 3 === 0) particle(p.dimension, "minecraft:redstone_ore_dust_particle", { x: loc.x, y: loc.y + 0.5, z: loc.z });
    if (n >= 14) { IN_BLOOD.delete(p.id); try { p.kill(); } catch { /* */ } }
}

// ------------------------------------------------------------------ cargo drifting past: the only food out here, besides Pip's fish
export function keepCrates(p: Player, want: number): void {
    const dim = p.dimension;
    const near = dim.getEntities({ type: "bloodsea:crate", location: p.location, maxDistance: 90 });
    for (const c of near) if (flatDist(c.location, p.location) > 85) c.remove();
    if (near.length >= want || Math.random() > 0.34) return;
    const r = raftOf(p);
    const yaw = r ? r.getRotation().y : p.getRotation().y;
    const a = yaw * Math.PI / 180;
    const f = { x: -Math.sin(a), z: Math.cos(a) }, side = { x: -f.z, z: f.x };
    const d = rnd(22, 46), sd = rnd(-15, 15);
    const at = { x: p.location.x + f.x * d + side.x * sd, y: SURFACE - 0.1, z: p.location.z + f.z * d + side.z * sd };
    try { dim.spawnEntity("bloodsea:crate", at); } catch { /* unloaded */ }
}

const LOOT: [string, number][][] = [
    [["minecraft:bread", 3]], [["minecraft:cooked_cod", 2], ["minecraft:bread", 1]], [["minecraft:apple", 3]],
    [["minecraft:dried_kelp", 6], ["minecraft:bread", 1]], [["minecraft:baked_potato", 3]],
];

export function haul(p: Player, crate: Entity, onCrate: (p: Player) => void): void {
    if (!crate.isValid()) return;
    const inv = p.getComponent("minecraft:inventory") as EntityInventoryComponent;
    for (const [id, n] of LOOT[Math.floor(Math.random() * LOOT.length)]) {
        const it = new ItemStack(id, n);
        const left = inv?.container?.addItem(it);
        if (left) p.dimension.spawnItem(left, p.location);
    }
    cmd(p.dimension, `playsound block.barrel.open @a ${crate.location.x} ${crate.location.y} ${crate.location.z} 1 0.8`);
    particle(p.dimension, "minecraft:redstone_ore_dust_particle", crate.location);
    crate.remove();
    onCrate(p);
}

/** a cargo crate the raft runs into is hauled aboard by whoever rows */
export function cratesTouch(p: Player, onCrate: (p: Player) => void): void {
    const r = raftOf(p);
    if (!r) return;
    for (const c of p.dimension.getEntities({ type: "bloodsea:crate", location: r.location, maxDistance: 3.4 })) haul(p, c, onCrate);
}
