// A cutscene playing for one player: the camera on its shots, Elias and Pip and whatever else on their marks,
// the lines, the sounds, the fades. The player's own body waits, unseen, at the stern.
import { world, system, Player, Entity, Vector3, Dimension, EasingType, ItemStack } from "@minecraft/server";
import { Cut, CUTS } from "./cutlib";
import {
    local, lerpV, clamp, wrapDeg, yawTo, setAnim, act, later, sfx, cmd, particle, TOP, add, dist,
} from "./util";
import { say } from "./ui";
import { getPip, pipScripted, pipMeow, pipGone as killPip } from "./pip";
import { eye as watcherEye } from "./watcher";
import { raftOf, aboard } from "./sea";
import { scriptCreature } from "./creatures";

interface Mover { e: Entity; a: Vector3; b: Vector3; ya: number; yb: number; t0: number; dur: number; }

const TYPES: Record<string, string> = {
    pip: "bloodsea:pip", wake: "bloodsea:wake", gatherer: "bloodsea:gatherer", caller: "bloodsea:caller",
    leviathan: "bloodsea:leviathan", rescuer: "bloodsea:rescuer", changer: "bloodsea:changer",
};

export const SESSIONS = new Map<string, Session>();

export function inCut(p: Player): boolean { return SESSIONS.has(p.id); }

export class Session {
    readonly p: Player;
    readonly cut: Cut;
    readonly o: Vector3;
    readonly yaw: number;
    readonly dim: Dimension;
    readonly then: () => void;
    t = 0;
    actorE?: Entity;
    pipE?: Entity;
    things = new Map<string, Entity>();
    keepers = new Set<string>();
    movers: Mover[] = [];
    home: Vector3;
    ended = false;
    ramp?: [number, number, number, number];
    skipHeld = 0;

    constructor(p: Player, cut: Cut, o: Vector3, yaw: number, then: () => void, home?: Vector3) {
        this.p = p; this.cut = cut; this.o = o; this.yaw = yaw; this.then = then;
        this.dim = p.dimension;
        this.home = home ?? p.location;
    }

    at(lx: number, ly: number, lz: number): Vector3 { return local(this.o, this.yaw, lx, ly, lz); }

    begin(): void {
        const p = this.p;
        try { p.addEffect("invisibility", 20 * 3600, { showParticles: false }); } catch { /* */ }
        try { p.addEffect("resistance", 20 * 3600, { amplifier: 255, showParticles: false }); } catch { /* */ }
        cmd(p, "inputpermission set @s movement disabled");
        for (const el of ["hotbar", "crosshair", "health", "hunger", "armor", "air_bubbles", "horse_health", "status_effects", "item_text", "paperdoll", "progress_bar"])
            cmd(p, `hud @s hide ${el}`);
        this.pipE = getPip(p);
        if (this.pipE) pipScripted(this.pipE, true);
        try { p.camera.fade({ fadeTime: { fadeInTime: 0, holdTime: 0.3, fadeOutTime: 1.0 }, fadeColor: { red: 0, green: 0, blue: 0 } }); } catch { /* */ }
        console.warn(`[bloodsea] cut ${this.cut.id} begins`);
    }

    tick(): void {
        if (this.ended) return;
        const p = this.p;
        if (!p.isValid()) { this.end(false); return; }
        const t = this.t;
        for (const e of this.cut.events) if (e.t === t) {
            try { e.run(this); } catch (err) { console.warn(`[bloodsea] event failed in ${this.cut.id} at ${t}: ${err}`); }
        }
        for (const l of this.cut.lines) if (l.t === t) say(p, l.key);
        for (let i = this.movers.length - 1; i >= 0; i--) if (this.step(this.movers[i])) this.movers.splice(i, 1);
        if (this.ramp) {
            const [a, b, t0, d] = this.ramp;
            const k = clamp((t - t0) / d, 0, 1);
            try { world.setTimeOfDay(Math.round(a + (b - a) * k) % 24000); } catch { /* */ }
        }
        this.camera(t);
        // the body waits where it was, out of sight
        try { p.teleport(this.home); } catch { /* */ }
        // hold crouch to skip
        if (t > 40 && p.isSneaking) { if (++this.skipHeld > 24) { this.end(true); return; } } else this.skipHeld = 0;
        this.t++;
        if (this.t > this.cut.length + 20) this.end(true);
    }

    private camera(t: number): void {
        const s = this.cut.shotAt(t);
        if (!s) return;
        const p = this.p;
        const toW = (v: [number, number, number]) => this.at(v[0], v[1], v[2]);
        try {
            if (t === s.t0) {
                p.camera.setCamera("minecraft:free", { location: toW(s.p0), facingLocation: toW(s.l0) });
                if (s.t0 > 0 && !(this.cut.blackFrom >= 0 && t >= this.cut.blackFrom && t < this.cut.blackTo))
                    p.camera.fade({ fadeTime: { fadeInTime: 0.05, holdTime: 0.05, fadeOutTime: 0.25 }, fadeColor: { red: 0, green: 0, blue: 0 } });
            } else if (t === s.t0 + 1) {
                const moves = s.p0.some((x, i) => x !== s.p1[i]) || s.l0.some((x, i) => x !== s.l1[i]);
                if (moves) p.camera.setCamera("minecraft:free", {
                    location: toW(s.p1), facingLocation: toW(s.l1),
                    easeOptions: { easeTime: Math.max(0.05, (s.t1 - s.t0 - 1) / 20), easeType: EasingType.InOutSine },
                });
            }
            if (t === this.cut.blackFrom) {
                const hold = Math.max(0, (this.cut.blackTo - this.cut.blackFrom) / 20 - 0.3);
                p.camera.fade({ fadeTime: { fadeInTime: 0.3, holdTime: hold, fadeOutTime: 0.7 }, fadeColor: { red: 0, green: 0, blue: 0 } });
            }
            if (t === this.cut.whiteFrom) {
                const hold = Math.max(0.5, (this.cut.length - this.cut.whiteFrom) / 20 - 2.5);
                p.camera.fade({ fadeTime: { fadeInTime: 2.5, holdTime: hold, fadeOutTime: 0.6 }, fadeColor: { red: 1, green: 1, blue: 0.96 } });
            }
        } catch { /* camera not ready */ }
    }

    end(restore: boolean): void {
        if (this.ended) return;
        this.ended = true;
        SESSIONS.delete(this.p.id);
        this.cleanup(restore);
        console.warn(`[bloodsea] cut ${this.cut.id} ends`);
        try { this.then(); } catch (e) { console.warn("[bloodsea] after cut: " + e); }
    }

    cleanup(restore: boolean): void {
        const p = this.p;
        if (this.actorE?.isValid()) this.actorE.remove();
        for (const [k, e] of this.things) if (e.isValid() && !this.keepers.has(k)) e.remove();
        if (this.pipE?.isValid()) pipScripted(this.pipE, false);
        if (!p.isValid()) return;
        try { p.camera.clear(); } catch { /* */ }
        try { p.removeEffect("invisibility"); p.removeEffect("resistance"); } catch { /* */ }
        cmd(p, "inputpermission set @s movement enabled");
        cmd(p, "hud @s reset");
        if (restore) {
            const r = raftOf(p);
            if (r && dist(r.location, p.location) < 64) { aboard(p, r); later(4, () => { try { p.camera.setCamera("minecraft:first_person"); } catch { /* */ } }); }
            else try { p.teleport(this.home); } catch { /* */ }
        }
    }

    // ------------------------------------------------------------------ what a scene can do (the names Cuts.java uses)
    private put(e: Entity, lx: number, ly: number, lz: number, lyaw: number): void {
        this.movers = this.movers.filter(m => m.e !== e);
        try { e.teleport(this.at(lx, ly, lz), { rotation: { x: 0, y: this.yaw + lyaw } }); } catch { /* */ }
    }

    private move(e: Entity, lx: number, ly: number, lz: number, lyaw: number, ticks: number): void {
        this.movers = this.movers.filter(m => m.e !== e);
        this.movers.push({ e, a: e.location, b: this.at(lx, ly, lz), ya: e.getRotation().y, yb: this.yaw + lyaw, t0: this.t, dur: Math.max(1, ticks) });
    }

    /** walking pace: a short ease in and out and an even stride between, facing the way it goes */
    private step(m: Mover): boolean {
        if (!m.e.isValid()) return true;
        const k = clamp((this.t - m.t0) / m.dur, 0, 1);
        const r = 0.15;
        const s = k < r ? k * k / (2 * r * (1 - r)) : k > 1 - r ? 1 - (1 - k) * (1 - k) / (2 * r * (1 - r)) : (k - r / 2) / (1 - r);
        const v = lerpV(m.a, m.b, s);
        const dx = m.b.x - m.a.x, dz = m.b.z - m.a.z;
        let y: number;
        if (dx * dx + dz * dz > 0.25 && m.b.y - m.a.y > -1.5) {
            const h = Math.atan2(dz, dx) * 180 / Math.PI - 90;
            if (k < 0.15) y = m.ya + wrapDeg(h - m.ya) * (k / 0.15);
            else if (k < 0.8) y = h;
            else y = h + wrapDeg(m.yb - h) * ((k - 0.8) / 0.2);
        } else y = m.ya + wrapDeg(m.yb - m.ya) * s;
        try { m.e.teleport(v, { rotation: { x: 0, y } }); } catch { /* */ }
        return k >= 1;
    }

    actor(anim?: string): Entity | undefined {
        if (!this.actorE || !this.actorE.isValid()) {
            try {
                this.actorE = this.dim.spawnEntity("bloodsea:elias", this.at(0, 0, 0));
                this.actorE.setRotation({ x: 0, y: this.yaw });
            } catch { return undefined; }
        }
        if (anim !== undefined) setAnim(this.actorE, anim);
        return this.actorE;
    }
    actorAt(lx: number, ly: number, lz: number, lyaw: number): void { const a = this.actor(); if (a) this.put(a, lx, ly, lz, lyaw); }
    actorWalk(lx: number, ly: number, lz: number, lyaw: number, ticks: number): void { const a = this.actor(); if (a) this.move(a, lx, ly, lz, lyaw, ticks); }

    pip(anim: string): void { if (this.pipE?.isValid()) setAnim(this.pipE, anim); }
    pipAt(lx: number, ly: number, lz: number, lyaw: number): void { if (this.pipE?.isValid()) this.put(this.pipE, lx, ly, lz, lyaw); }
    pipWalk(lx: number, ly: number, lz: number, lyaw: number, ticks: number): void { if (this.pipE?.isValid()) this.move(this.pipE, lx, ly, lz, lyaw, ticks); }
    pipGone(): void { if (this.pipE?.isValid()) killPip(this.p, this.pipE); this.pipE = undefined; }
    pipMeow(): void { if (this.pipE?.isValid()) pipMeow(this.pipE); }

    spawn(kind: string, lx: number, ly: number, lz: number, lyaw: number): Entity | undefined {
        try {
            const e = this.dim.spawnEntity(TYPES[kind] ?? "bloodsea:changer", this.at(lx, ly, lz));
            e.setRotation({ x: 0, y: this.yaw + lyaw });
            if (kind === "pip") pipScripted(e, true); else scriptCreature(e, true);
            this.things.set(kind, e);
            return e;
        } catch { return undefined; }
    }
    keep(kind: string): void { this.keepers.add(kind); }
    anim(kind: string, a: string): void { setAnim(this.things.get(kind), a); }
    act(kind: string, a: string): void { act(this.things.get(kind), a); }
    moveThing(kind: string, lx: number, ly: number, lz: number, lyaw: number, ticks: number): void {
        const e = this.things.get(kind);
        if (e?.isValid()) this.move(e, lx, ly, lz, lyaw, ticks);
    }
    gone(kind: string): void { const e = this.things.get(kind); this.things.delete(kind); if (e?.isValid()) e.remove(); }

    sound(name: string, vol: number, pitch: number): void { sfx(this.dim, name, add(this.o, { x: 0, y: 1, z: 0 }), vol, pitch); }
    soundAt(name: string, lx: number, ly: number, lz: number, vol: number, pitch: number): void { sfx(this.dim, name, this.at(lx, ly, lz), vol, pitch); }
    time(a: number, b: number, ticks: number): void { this.ramp = [a, b, this.t, Math.max(1, ticks)]; }
    eye(ticks: number): void { watcherEye(this.p, ticks); }
    shake(ticks: number): void { cmd(this.p, `camerashake add @s 0.35 ${(ticks / 20).toFixed(2)} positional`); }
    puff(lx: number, ly: number, lz: number, n: number): void {
        for (let i = 0; i < Math.min(8, Math.ceil(n / 4)); i++) particle(this.dim, "minecraft:large_explosion", this.at(lx + (Math.random() - 0.5) * 0.6, ly, lz + (Math.random() - 0.5) * 0.6));
    }
    splash(lx: number, ly: number, lz: number): void {
        for (let i = 0; i < 12; i++) particle(this.dim, "minecraft:redstone_ore_dust_particle", this.at(lx + (Math.random() - 0.5), ly + Math.random() * 0.6, lz + (Math.random() - 0.5)));
        this.soundAt("blood_splash", lx, ly, lz, 1.5, 0.9);
    }
    drop(item: string, lx: number, ly: number, lz: number): void {
        try { this.dim.spawnItem(new ItemStack(item, 1), this.at(lx, ly, lz)); } catch { /* */ }
    }
    clearItems(): void {
        for (const e of this.dim.getEntities({ type: "minecraft:item", location: this.o, maxDistance: 12 })) e.remove();
    }
}

/** play a cutscene around the player's raft */
export function play(p: Player, id: string, then: () => void, raft?: Entity): void {
    const r = raft ?? raftOf(p);
    let o = p.location, yaw = p.getRotation().y;
    if (r) {
        yaw = Math.round(r.getRotation().y / 90) * 90;
        try { r.setRotation({ x: 0, y: yaw }); } catch { /* */ }
        try { (r.getComponent("minecraft:rideable") as any)?.ejectRiders(); } catch { /* */ }
        o = { x: r.location.x, y: r.location.y + TOP, z: r.location.z };
    }
    const home = r ? local(r.location, yaw, 0, TOP + 0.05, -1.2) : p.location;
    playAt(p, id, o, yaw, then, home);
}

export function playAt(p: Player, id: string, o: Vector3, yaw: number, then: () => void, home?: Vector3): void {
    const c = CUTS.get(id);
    if (!c) { then(); return; }
    const old = SESSIONS.get(p.id);
    if (old) old.cleanup(false);
    const s = new Session(p, c, o, yaw, then, home);
    SESSIONS.set(p.id, s);
    s.begin();
}

export function tickSessions(): void {
    for (const s of [...SESSIONS.values()]) s.tick();
}
