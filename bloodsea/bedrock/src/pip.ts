// Pip: the black cat on the raft. She lives on its deck (her place is kept in the raft's own frame, so she goes where
// it goes), naps on the blanket, grooms, stretches, rubs against you, hisses at what is in the water, and hooks fish out
// of the blood for you - more often the more you care for her.
import { world, Player, Entity, ItemStack, Vector3, EntityInventoryComponent } from "@minecraft/server";
import { st, save, TOP, setAnim, getAnim, act, sfx, wrapDeg, clamp, rnd, isDay, later, flatDist } from "./util";
import { raftOf, raftLocal } from "./sea";
import { say } from "./ui";

type PState = "idle" | "wander" | "sleep" | "groom" | "stretch" | "rub" | "petted" | "eat" | "gofish" | "fish" | "carry" | "drop" | "hiss" | "scared" | "ride";

interface Cat {
    state: PState;
    t: number;
    lx: number; lz: number; yaw: number;   // where she is on the deck, and which way she faces (relative to the raft)
    gx: number; gz: number;                 // where she is going
    scripted: boolean;
    nextMeow: number;
    nextGift: number;
    petCool: number;
}

const CATS = new Map<string, Cat>();
const BLANKET = { x: -1.4, z: -1.45 };

function cat(e: Entity): Cat {
    let c = CATS.get(e.id);
    if (!c) {
        c = { state: "idle", t: 0, lx: BLANKET.x, lz: BLANKET.z, yaw: 0, gx: BLANKET.x, gz: BLANKET.z, scripted: false,
              nextMeow: 400 + Math.random() * 900, nextGift: 1400 + Math.random() * 1200, petCool: 0 };
        CATS.set(e.id, c);
    }
    return c;
}

/** the player's cat, if she is still with them */
export function getPip(p: Player): Entity | undefined {
    if (st(p).pip_gone) return undefined;
    const r = raftOf(p);
    const near = p.dimension.getEntities({ type: "bloodsea:pip", location: r ? r.location : p.location, maxDistance: 48, closest: 1 });
    return near[0];
}

export function makePip(p: Player): Entity | undefined {
    const have = getPip(p);
    if (have) return have;
    const r = raftOf(p);
    if (!r) return undefined;
    try {
        const e = p.dimension.spawnEntity("bloodsea:pip", raftLocal(r, BLANKET.x, TOP + 0.05, BLANKET.z));
        cat(e);
        return e;
    } catch { return undefined; }
}

export function pipScripted(e: Entity, on: boolean): void {
    const c = cat(e);
    c.scripted = on;
    if (!on) {
        // back on the deck where the scene left her
        const p = world.getAllPlayers()[0];
        const r = p ? raftOf(p) : undefined;
        if (r) {
            const a = r.getRotation().y * Math.PI / 180, dx = e.location.x - r.location.x, dz = e.location.z - r.location.z;
            c.lx = clamp(dx * Math.cos(a) + dz * Math.sin(a), -1.9, 1.9);
            c.lz = clamp(-dx * Math.sin(a) + dz * Math.cos(a), -1.9, 1.9);
        }
        go(c, "idle");
    }
}

export function pipMeow(e: Entity): void {
    act(e, "meow");
    sfx(e.dimension, "pip_meow", e.location, 1, rnd(0.95, 1.05));
}

export function pipGone(p: Player, e: Entity): void {
    CATS.delete(e.id);
    e.remove();
    st(p).pip_gone = true;
    save(p);
}

function go(c: Cat, s: PState): void { c.state = s; c.t = 0; }

const LOOP: Record<PState, string> = {
    idle: "sit", wander: "walk", sleep: "sleep", groom: "groom", stretch: "stretch", rub: "rub", petted: "petted", eat: "eat",
    gofish: "walk", fish: "swipe", carry: "carry", drop: "drop", hiss: "hiss", scared: "scared", ride: "sit",
};

/** right-click (tap) on her: food from your hand, or a stroke */
export function onPetted(e: Entity, onPet: (p: Player) => void): void {
    const c = cat(e);
    if (c.scripted) return;
    const p = e.dimension.getPlayers({ location: e.location, maxDistance: 8, closest: 1 })[0];
    if (!p) return;
    const inv = p.getComponent("minecraft:inventory") as EntityInventoryComponent;
    const slot = (p as any).selectedSlotIndex ?? (p as any).selectedSlot ?? 0;
    const held = inv?.container?.getItem(slot);
    const food = held && /cod|salmon|fish|bread|apple|potato|kelp|beef|chicken|mutton|porkchop|rabbit|cookie|carrot|berries|melon|pie/.test(held.typeId);
    const s = st(p);
    if (food && c.state !== "eat") {
        if (held!.amount > 1) { held!.amount -= 1; inv.container!.setItem(slot, held); } else inv.container!.setItem(slot, undefined);
        s.bond = (s.bond ?? 0) + 2; save(p);
        go(c, "eat");
        sfx(e.dimension, "pip_chirp", e.location, 0.8, 1.1);
        return;
    }
    if (c.petCool > 0) return;
    s.bond = (s.bond ?? 0) + 1; save(p);
    c.petCool = 60;
    go(c, "petted");
    sfx(e.dimension, "pip_purr", e.location, 0.9, 1);
    onPet(p);
}

/** every tick, for each cat */
export function tickPips(): void {
    for (const p of world.getAllPlayers()) {
        const e = getPip(p);
        if (!e) continue;
        const c = cat(e);
        if (c.scripted) continue;
        const r = raftOf(p);
        if (!r) continue;
        brain(p, e, c, r);
        // she is where the raft says she is
        const at = raftLocal(r, c.lx, TOP + 0.05, c.lz);
        try { e.teleport(at, { rotation: { x: 0, y: r.getRotation().y + c.yaw } }); } catch { /* */ }
        const want = LOOP[c.state];
        if (getAnim(e) !== want && getAnim(e) !== "meow") setAnim(e, want);
    }
}

function walkTo(c: Cat, gx: number, gz: number, speed: number): boolean {
    const dx = gx - c.lx, dz = gz - c.lz, d = Math.hypot(dx, dz);
    if (d < 0.05) return true;
    const s = Math.min(speed, d);
    c.lx += dx / d * s; c.lz += dz / d * s;
    const want = Math.atan2(dz, dx) * 180 / Math.PI - 90;
    c.yaw += clamp(wrapDeg(want - c.yaw), -12, 12);
    return d < 0.2;
}

function faceTo(c: Cat, x: number, z: number): void {
    const want = Math.atan2(z - c.lz, x - c.lx) * 180 / Math.PI - 90;
    c.yaw += clamp(wrapDeg(want - c.yaw), -10, 10);
}

function brain(p: Player, e: Entity, c: Cat, r: Entity): void {
    c.t++;
    if (c.petCool > 0) c.petCool--;
    const s = st(p);
    // the rower, in the raft's frame
    const a = r.getRotation().y * Math.PI / 180;
    const dx = p.location.x - r.location.x, dz = p.location.z - r.location.z;
    const ox = dx * Math.cos(a) + dz * Math.sin(a), oz = -dx * Math.sin(a) + dz * Math.cos(a);
    // the things in the sea
    const near = e.dimension.getEntities({ families: ["bloodsea"], location: e.location, maxDistance: 28 })
        .filter(x => ["bloodsea:wake", "bloodsea:gatherer", "bloodsea:caller", "bloodsea:leviathan", "bloodsea:changer"].includes(x.typeId));
    if (near.length && c.state !== "hiss" && c.state !== "scared") {
        go(c, flatDist(near[0].location, e.location) < 12 ? "scared" : "hiss");
        sfx(e.dimension, "pip_hiss", e.location, 1.2, 1);
    }
    if (c.state === "hiss" || c.state === "scared") {
        if (near.length) {
            const ndx = near[0].location.x - r.location.x, ndz = near[0].location.z - r.location.z;
            faceTo(c, ndx * Math.cos(a) + ndz * Math.sin(a), -ndx * Math.sin(a) + ndz * Math.cos(a));
        }
        if (!near.length && c.t > 60) go(c, "idle");
        if (c.t % 40 === 0 && c.state === "hiss") sfx(e.dimension, "pip_hiss", e.location, 1, 1);
        return;
    }
    if (--c.nextMeow <= 0) {
        c.nextMeow = 400 + Math.random() * 900;
        if (c.state !== "sleep") pipMeow(e);
    }
    const rowing = !!p.getComponent("minecraft:riding");
    switch (c.state) {
        case "idle": {
            if (rowing) { walkTo(c, BLANKET.x, BLANKET.z, 0.05); if (c.t > 40) go(c, "ride"); break; }
            if (c.t > 60 + Math.random() * 200) choose(p, c, ox, oz, s.bond ?? 0);
            break;
        }
        case "ride": if (walkTo(c, BLANKET.x, BLANKET.z, 0.05) && !rowing) go(c, "idle"); break;
        case "wander": if (walkTo(c, c.gx, c.gz, 0.045) || c.t > 140) go(c, "idle"); break;
        case "sleep": if (c.t > 600 && isDay()) go(c, "stretch"); break;
        case "groom": if (c.t > 160) go(c, "idle"); break;
        case "stretch": if (c.t > 60) go(c, "idle"); break;
        case "rub": {
            walkTo(c, clamp(ox + 0.5, -1.9, 1.9), clamp(oz, -1.9, 1.9), 0.04);
            if (c.t % 30 === 0) sfx(e.dimension, "pip_purr", e.location, 0.6, 1);
            if (c.t > 120) go(c, "idle");
            break;
        }
        case "petted": faceTo(c, ox, oz); if (c.t > 70) go(c, Math.random() < 0.33 ? "rub" : "idle"); break;
        case "eat": if (c.t > 60) go(c, "idle"); break;
        case "gofish": if (walkTo(c, c.gx, c.gz, 0.05) || c.t > 140) go(c, "fish"); break;
        case "fish": {
            if (c.t === 26) sfx(e.dimension, "pip_chirp", e.location, 0.6, 1.3);
            if (c.t > 36) { go(c, "carry"); sfx(e.dimension, "blood_splash", e.location, 0.6, 1.4); }
            break;
        }
        case "carry": if (walkTo(c, clamp(ox, -1.9, 1.9), clamp(oz, -1.9, 1.9), 0.06) || c.t > 200) go(c, "drop"); break;
        case "drop": {
            faceTo(c, ox, oz);
            if (c.t === 12) {
                try { e.dimension.spawnItem(new ItemStack("minecraft:cod", 1), p.location); } catch { /* */ }
                pipMeow(e);
                if (Math.random() < 0.5) say(p, "think.pet");
            }
            if (c.t > 40) go(c, "idle");
            break;
        }
    }
}

function choose(p: Player, c: Cat, ox: number, oz: number, bond: number): void {
    const day = isDay();
    if (!day) { c.gx = BLANKET.x; c.gz = BLANKET.z; go(c, "sleep"); c.lx = BLANKET.x; c.lz = BLANKET.z; return; }
    c.nextGift -= 300;
    if (c.nextGift <= 0) {
        c.nextGift = Math.max(700, 2400 - bond * 80) + Math.random() * 600;
        const side = Math.random() < 0.5 ? -1 : 1;
        c.gx = 2.0 * side; c.gz = rnd(-1.2, 1.2);
        go(c, "gofish");
        return;
    }
    const k = Math.random();
    if (k < 0.25) { c.gx = rnd(-1.8, 1.8); c.gz = rnd(-1.8, 1.8); go(c, "wander"); }
    else if (k < 0.4) go(c, "groom");
    else if (k < 0.5) go(c, "stretch");
    else if (k < 0.62 && Math.hypot(ox - c.lx, oz - c.lz) < 4) go(c, "rub");
    else if (k < 0.75) { c.lx = BLANKET.x; c.lz = BLANKET.z; go(c, "sleep"); }
    else go(c, "idle");
}

/** the call control: she comes to you and asks to be stroked */
export function callPip(p: Player, onPet: (p: Player) => void): void {
    const e = getPip(p);
    if (!e) return;
    const c = cat(e);
    if (c.scripted) return;
    pipMeow(e);
    if (flatDist(e.location, p.location) < 2.5) onPetted(e, onPet);
    else go(c, "rub");
}
