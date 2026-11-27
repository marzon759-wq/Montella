// Omens: between the chapters the sea does things it never explains. A ship's bell far off, blood falling as rain,
// a moan from below, a woman's whisper behind you, lanterns that go out when looked at, the sun forgetting to be a
// sun - and a raft like yours with a man like you standing on it, watching.
import { world, Player, Entity, Vector3 } from "@minecraft/server";
import { st, SURFACE, TOP, sfx, cmd, particle, isDay, later, setAnim, flatDist, yawTo, local } from "./util";
import { say } from "./ui";
import { eye } from "./watcher";
import { dread } from "./music";

interface OState { t: number; next: number; recent: string[]; ghost?: Entity; double?: Entity; ghostT: number; lights: Vector3[]; lightsT: number; rainT: number; }
const STATES = new Map<string, OState>();
const CALM = new Set(["rowing", "hungry", "day1", "dusk", "night1", "night1b", "north", "north2", "escape", "east0", "east", "west", "fog"]);
const AFTER_EYE = new Set(["north", "north2", "escape", "east0", "east", "west", "fog"]);

function state(p: Player): OState {
    let s = STATES.get(p.id);
    if (!s) { s = { t: 0, next: 60, recent: [], ghostT: 0, lights: [], lightsT: 0, rainT: 0 }; STATES.set(p.id, s); }
    return s;
}

function around(p: Player, d: number, ang: number): Vector3 { return { x: p.location.x + Math.cos(ang) * d, y: SURFACE + 0.4, z: p.location.z + Math.sin(ang) * d }; }

/** every second, for a player in the story */
export function tickOmens(p: Player, inCut: boolean): void {
    const s = state(p);
    tickGhost(p, s, inCut);
    tickLights(p, s, inCut);
    if (s.rainT > 0) s.rainT -= 20;
    if (inCut || !CALM.has(st(p).stage)) return;
    if (++s.t < s.next) return;
    s.t = 0;
    s.next = 70 + Math.floor(Math.random() * 90);
    const day = isDay();
    let pool = ["bell", "moan", "whisper", "rain", "ghost"];
    if (day && AFTER_EYE.has(st(p).stage)) pool.push("blink");
    if (!day) pool.push("lights", "lights");
    pool = pool.filter(o => !s.recent.includes(o) && !(o === "ghost" && s.ghost));
    if (!pool.length) return;
    const o = pool[Math.floor(Math.random() * pool.length)];
    s.recent.push(o);
    while (s.recent.length > 3) s.recent.shift();
    run(o, p, s);
}

export function forceOmen(p: Player, o: string): void { run(o, p, state(p)); }

function run(o: string, p: Player, s: OState): void {
    const ang = Math.random() * Math.PI * 2;
    const dim = p.dimension;
    switch (o) {
        case "bell":
            sfx(dim, "omen_bell", around(p, 60, ang), 7, 0.92 + Math.random() * 0.1);
            later(50, () => say(p, "omen.bell"));
            later(280, () => say(p, "omen.bell2"));
            break;
        case "moan":
            sfx(dim, "omen_moan", { x: p.location.x, y: SURFACE - 8, z: p.location.z }, 5, 0.85 + Math.random() * 0.2);
            cmd(p, "camerashake add @s 0.4 3.5 positional");
            for (let i = 0; i < 8; i++) later(10 + i * 12, () => {
                for (let j = 0; j < 5; j++) {
                    const a = Math.random() * Math.PI * 2, r = 3.5 + Math.random() * 5;
                    particle(dim, "minecraft:redstone_ore_dust_particle", { x: p.location.x + Math.cos(a) * r, y: SURFACE + 0.4, z: p.location.z + Math.sin(a) * r });
                }
                if (i % 3 === 0) sfx(dim, "blood_splash", p.location, 0.5, 0.5);
            });
            later(110, () => say(p, "omen.moan"));
            dread(400);
            break;
        case "whisper": {
            const v = p.getViewDirection();
            sfx(dim, "caller_whisper", { x: p.location.x - v.x * 2.5, y: p.location.y + 1.5, z: p.location.z - v.z * 2.5 }, 1.4, 0.8);
            later(10, () => say(p, "omen.whisper"));
            later(110, () => say(p, "omen.whisper2"));
            break;
        }
        case "rain":
            s.rainT = 1100;
            cmd(p, "weather rain 1100");
            later(80, () => say(p, "omen.rain"));
            later(1100, () => cmd(p, "weather clear"));
            break;
        case "blink":
            eye(p, 60);
            later(150, () => say(p, "omen.blink"));
            break;
        case "lights":
            s.lights = [];
            for (let i = 0; i < 6; i++) {
                const a = ang + (i - 2.5) * 0.18 + (Math.random() - 0.5) * 0.1;
                const at = around(p, 55 + Math.random() * 20, a);
                s.lights.push({ x: at.x, y: at.y + 0.6, z: at.z });
            }
            s.lightsT = 70;
            later(40, () => say(p, "omen.lights"));
            break;
        case "ghost": {
            const at = around(p, 22, ang);
            try {
                const g = dim.spawnEntity("bloodsea:raft", { x: at.x, y: SURFACE, z: at.z });
                g.addTag("bloodsea_ghost");
                const face = yawTo(at, p.location);
                g.setRotation({ x: 0, y: Math.round(face / 90) * 90 });
                const d = dim.spawnEntity("bloodsea:elias", local(g.location, g.getRotation().y, 0, TOP + 0.02, 0.4));
                d.addTag("bloodsea_ghost");
                d.setRotation({ x: 0, y: face });
                setAnim(d, "stand");
                s.ghost = g; s.double = d; s.ghostT = 0;
                later(40, () => say(p, "omen.ghost"));
                later(200, () => { if (s.double?.isValid()) say(p, "omen.ghost2"); });
            } catch { /* unloaded */ }
            break;
        }
    }
}

function tickGhost(p: Player, s: OState, inCut: boolean): void {
    if (!s.ghost) return;
    s.ghostT++;
    const gone = !s.ghost.isValid() || !s.double || !s.double.isValid();
    const d = gone ? 0 : flatDist(s.ghost.location, p.location);
    if (!gone) { try { s.double!.setRotation({ x: 0, y: yawTo(s.double!.location, p.location) }); } catch { /* */ } }
    if (gone || d < 10 || s.ghostT > 50 || inCut) {
        if (!gone) {
            for (let i = 0; i < 8; i++) particle(p.dimension, "minecraft:large_explosion", { x: s.ghost.location.x + (Math.random() - 0.5) * 3, y: s.ghost.location.y + 1, z: s.ghost.location.z + (Math.random() - 0.5) * 3 });
            sfx(p.dimension, "omen_bell", s.ghost.location, 3, 0.6);
            if (d < 10) later(30, () => say(p, "omen.ghost3"));
        }
        if (s.ghost?.isValid()) s.ghost.remove();
        if (s.double?.isValid()) s.double.remove();
        s.ghost = undefined; s.double = undefined;
    }
}

function tickLights(p: Player, s: OState, inCut: boolean): void {
    if (!s.lights.length) return;
    if (--s.lightsT <= 0 || inCut) { s.lights = []; return; }
    const eye = p.getHeadLocation(), v = p.getViewDirection();
    s.lights = s.lights.filter(l => {
        const dx = l.x - eye.x, dy = l.y - eye.y, dz = l.z - eye.z, n = Math.hypot(dx, dy, dz);
        return (dx * v.x + dy * v.y + dz * v.z) / n < 0.995;   // look straight at one and it goes out
    });
    for (const l of s.lights) for (let k = 0; k < 4; k++) later(k * 5, () => particle(p.dimension, "minecraft:basic_flame_particle", l));
}

/** blood rain falling around a player (every tick) */
export function tickRain(): void {
    for (const p of world.getAllPlayers()) {
        const s = STATES.get(p.id);
        if (!s || s.rainT <= 0) continue;
        for (let i = 0; i < 6; i++)
            particle(p.dimension, "minecraft:falling_dust_red_sand_particle", { x: p.location.x + (Math.random() - 0.5) * 20, y: p.location.y + 6 + Math.random() * 6, z: p.location.z + (Math.random() - 0.5) * 20 });
    }
}

/** ghosts from a world that was saved while they stood: they do not stay */
export function sweepGhosts(): void {
    for (const e of world.getDimension("overworld").getEntities({ tags: ["bloodsea_ghost"] })) {
        let mine = false;
        for (const s of STATES.values()) if (s.ghost === e || s.double === e) mine = true;
        if (!mine) e.remove();
    }
}
