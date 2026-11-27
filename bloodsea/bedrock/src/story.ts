// The story of the man on the raft: a chain of stages, each waiting for something (a place, a time, an action) and
// then playing a cutscene, putting a choice, setting an objective. Choices weigh four ways - hope, acceptance,
// courage, fury - and the heaviest decides where the voyage ends. Ported stage for stage from the Java mod.
import { world, Player, ItemStack, EntityInventoryComponent, Entity } from "@minecraft/server";
import {
    st, save, active, once, later, SEA_Y, SURFACE, TOP, DRIFT_R, EDGE_R, THRONE, MAW, LIGHTHOUSE, cmd, isDay, local, flatDist, tr, forget,
} from "./util";
import { say, obj, ask, hint, showObjective, welcome, endingCard } from "./ui";
import { play, playAt, inCut } from "./session";
import { raftOf, ensureRaft, rememberRaft, aboard, keepCrates, layNow, regionAt, raftLocal } from "./sea";
import { makePip, getPip } from "./pip";
import { spawnCreature, release } from "./creatures";
import { eye } from "./watcher";
import { dread, restartMusic } from "./music";
import { hospital, stomach, rescue, HOSPITAL, STOMACH, RESCUE } from "./sets";

const ASKING = new Set<string>();
const MOONRISE = 14200, FIRST_LIGHT = 22600;
const NIGHT_STEP = (FIRST_LIGHT - MOONRISE) / (6.5 * 60);

function stage(p: Player, s: string): void {
    const t = st(p);
    t.stage = s; t.t = 0; t.x0 = p.location.x; t.z0 = p.location.z;
    save(p);
    console.warn(`[bloodsea] ${p.name} -> ${s}`);
}

function score(p: Player, k: "hope" | "accept" | "courage" | "fury", n: number): void {
    const s = st(p) as any;
    s["s_" + k] = (s["s_" + k] ?? 0) + n;
    save(p);
}

function destination(p: Player): string {
    const s = st(p) as any;
    let best = "hope", bv = s.s_hope ?? 0;
    for (const k of ["courage", "accept", "fury"]) if ((s["s_" + k] ?? 0) > bv) { bv = s["s_" + k]; best = k; }
    return best;
}

function setTime(t: number): void { try { world.setTimeOfDay(Math.round(t) % 24000); } catch { /* */ } }
function tod(): number { return world.getTimeOfDay() % 24000; }

function choose(p: Player, id: string, then: (pick: string) => void): void {
    ASKING.add(p.id);
    ask(p, id, pick => { ASKING.delete(p.id); console.warn(`[bloodsea] choice ${id} -> ${pick}`); then(pick); });
}

export function busy(p: Player): boolean { return inCut(p) || ASKING.has(p.id); }

/** the touch controls in the hotbar (and the map): oar, map, call Pip, skip a scene. They stay in their slots. */
const CONTROLS: [number, string][] = [[0, "bloodsea:ctl_oar"], [1, "bloodsea:sea_map"], [2, "bloodsea:ctl_pip"], [8, "bloodsea:ctl_skip"]];
function giveMap(p: Player): void {
    const inv = p.getComponent("minecraft:inventory") as EntityInventoryComponent;
    const c = inv?.container;
    if (!c) return;
    for (const [slot, id] of CONTROLS) {
        let have = false;
        for (let i = 0; i < c.size; i++) if (c.getItem(i)?.typeId === id) { have = true; break; }
        if (have) continue;
        const it = new ItemStack(id, 1);
        try { (it as any).lockMode = "slot"; it.keepOnDeath = true; } catch { /* */ }
        const old = c.getItem(slot);
        c.setItem(slot, it);
        if (old) c.addItem(old);
    }
}

export function giveControls(p: Player): void { giveMap(p); }

// ------------------------------------------------------------------ starting
export function onJoin(p: Player): void {
    const s = st(p);
    if (active(p)) { later(40, () => resume(p)); return; }
    if (s.began || s.stage === "done") return;
    later(30, () => welcome(p, () => start(p)));
}

export function start(p: Player): void {
    const s = st(p);
    s.began = true;
    save(p);
    for (const c of ["gamerule dodaylightcycle false", "gamerule doweathercycle false", "gamerule keepinventory true",
        "gamerule domobspawning false", "gamerule showcoordinates false", "weather clear"]) cmd(p.dimension, c);
    setTime(1000);
    // up to where the sea is; the body waits unseen while the world arrives
    try { p.addEffect("slow_falling", 400, { showParticles: false }); p.addEffect("invisibility", 400, { showParticles: false }); p.addEffect("resistance", 400, { amplifier: 255, showParticles: false }); } catch { /* */ }
    try { p.teleport({ x: 0.5, y: SEA_Y + 3, z: -1.0 }); } catch { /* */ }
    try { p.camera.fade({ fadeTime: { fadeInTime: 0, holdTime: 3, fadeOutTime: 1 }, fadeColor: { red: 0, green: 0, blue: 0 } }); } catch { /* */ }
    const tryLay = (tries: number) => {
        if (!p.isValid()) return;
        try { p.teleport({ x: 0.5, y: SEA_Y + 3, z: -1.0 }); } catch { /* */ }
        if (layNow(p, 2) < 25 && tries < 40) { later(10, () => tryLay(tries + 1)); return; }
        const r = ensureRaft(p);
        if (!r) { later(10, () => tryLay(tries + 1)); return; }
        later(10, () => {
            makePip(p);
            p.getComponent("minecraft:health");
            try { (p as any).getComponent("minecraft:player.hunger")?.setCurrentValue(14); } catch { /* */ }
            stage(p, "wake");
            play(p, "wake", () => {
                giveMap(p);
                obj(p, "oar");
                stage(p, "oar");
                firstPerson(p);
            }, r);
        });
    };
    later(20, () => tryLay(0));
}

/** back to where the story was (a reload, a rejoin) */
function resume(p: Player): void {
    if (!p.isValid() || inCut(p)) return;
    const r = ensureRaft(p);
    if (r) aboard(p, r);
    if (!getPip(p) && !st(p).pip_gone) makePip(p);
    firstPerson(p);
    showObjective(p);
    restartMusic();
}

export function firstPerson(p: Player): void { try { p.camera.setCamera("minecraft:first_person"); } catch { /* */ } }

export function onRespawn(p: Player): void {
    const s = st(p);
    if (!active(p) || s.stage === "ending") return;
    later(2, () => {
        const r = ensureRaft(p);
        if (r) aboard(p, r);
        giveMap(p);
        firstPerson(p);
        if (s.stage.startsWith("night")) later(40, () => say(p, "night.died"));
    });
}

// ------------------------------------------------------------------ hooks
export function onCrate(p: Player): void {
    if (st(p).stage === "hungry") {
        say(p, "think.crate");
        obj(p, "explore");
        stage(p, "day1");
    }
}

export function onPet(p: Player): void { if (Math.random() < 0.25) say(p, "think.pet"); }

// ------------------------------------------------------------------ every second, for each player in the story
export function tickStory(p: Player): void {
    if (!active(p) || busy(p)) return;
    const s = st(p);
    const t = (s.t += 20);
    const r = raftOf(p);
    if (r) rememberRaft(p, r);
    if (t % 200 === 0) save(p);
    const reg = regionAt(p.location.x, p.location.z);
    const moved = Math.hypot(p.location.x - s.x0, p.location.z - s.z0);
    const sg = s.stage;
    // the day goes by while you row
    if (["rowing", "hungry", "day1"].includes(sg)) { const n = tod(); if (n < 10400) setTime(n + 10); }
    else if (["north", "north2", "escape", "east0", "east"].includes(sg)) { const n = tod(); if (n < 11000) setTime(n + 4); }
    if (t % 600 === 300 && !sg.startsWith("night")) showObjective(p);
    switch (sg) {
        case "oar":
            if (p.getComponent("minecraft:riding")) { say(p, "think.row"); obj(p, "explore"); stage(p, "rowing"); }
            break;
        case "rowing":
            if (t > 300 && r) play(p, "rule", () => {
                try { (p as any).getComponent("minecraft:player.hunger")?.setCurrentValue(5); } catch { /* */ }
                say(p, "think.hungry"); obj(p, "food"); stage(p, "hungry");
            });
            break;
        case "hungry": keepCrates(p, 3); break;
        case "day1":
            keepCrates(p, 2);
            if (t > 1400 && r) {
                if (tod() < 10500) setTime(10500);
                play(p, "gift", () => choose(p, "gift", pick => {
                    if (pick === "share") { score(p, "courage", 1); s.bond = (s.bond ?? 0) + 3; } else score(p, "fury", 1);
                    say(p, "gift." + pick);
                    later(120, () => { say(p, "night.dusk"); obj(p, "dusk"); });
                    stage(p, "dusk");
                }));
            }
            break;
        case "dusk": {
            keepCrates(p, 2);
            const n = tod();
            if (n < 12000) setTime(Math.min(12000, n + 25));
            if (n >= 12000 && t > 200) play(p, "moon", () => { obj(p, "night"); say(p, "night.start"); stage(p, "night1"); });
            break;
        }
        case "night1": case "night1b": {
            keepCrates(p, 2);
            let n = tod();
            if (n < 12000) n += 24000;
            if (n < FIRST_LIGHT) setTime(Math.min(FIRST_LIGHT, n + Math.round(NIGHT_STEP)));
            const f = Math.max(0, Math.min(1, (n - MOONRISE) / (FIRST_LIGHT - MOONRISE)));
            if (t % 100 === 0) {
                const secs = Math.max(0, Math.round((FIRST_LIGHT - n) / NIGHT_STEP));
                hint(p, { rawtext: [{ text: "§6" }, { translate: "hud.bloodsea.dawn", with: [`${Math.floor(secs / 60)}:${String(secs % 60).padStart(2, "0")}`] }] });
            }
            if (f > 0.08 && once(p, "n_cold")) say(p, "night.cold");
            if (sg === "night1") {
                if (f > 0.2) play(p, "bump", () => choose(p, "bump", pick => {
                    if (pick === "look") score(p, "courage", 1);
                    else if (pick === "quiet") score(p, "hope", 1);
                    else { score(p, "fury", 1); try { p.applyDamage(3); } catch { /* */ } cmd(p, "camerashake add @s 0.4 1.5 positional"); }
                    say(p, "bump." + pick);
                    stage(p, "night1b");
                }));
                break;
            }
            if (f > 0.3 && r && once(p, "n_wake")) { spawnCreature(p, "wake"); later(60, () => say(p, "night.wake")); dread(600); }
            if (f > 0.5 && once(p, "n_half")) say(p, "night.half");
            if (f > 0.62 && r && once(p, "n_caller")) { spawnCreature(p, "caller"); say(p, "night.caller"); dread(600); }
            if (f > 0.88 && once(p, "n_late")) say(p, "night.late");
            if (f >= 1 && r) {
                for (const e of p.dimension.getEntities({ location: p.location, maxDistance: 120, families: ["bloodsea"] }))
                    if (e.typeId === "bloodsea:caller" || e.typeId === "bloodsea:wake") e.remove();
                play(p, "dawn", () => play(p, "mem1", () => { say(p, "think.north"); obj(p, "north"); stage(p, "north"); }));
            }
            break;
        }
        case "north":
            keepCrates(p, 2);
            if ((moved > 220 || t > 2600) && r) {
                setTime(6000);
                play(p, "eye", () => choose(p, "eye", pick => {
                    score(p, pick === "stare" ? "courage" : "hope", 1);
                    say(p, "eye." + pick);
                    eye(p, 0);
                    s.eye_free = true; save(p);
                    stage(p, "north2");
                }));
            }
            break;
        case "north2":
            keepCrates(p, 2);
            if (reg === "bones" && r) play(p, "bones", () => { obj(p, "escape"); release(p, "gatherer"); stage(p, "escape"); });
            break;
        case "escape": {
            const g = p.dimension.getEntities({ type: "bloodsea:gatherer", location: p.location, maxDistance: 70 });
            if (!g.length || t > 1400) { g.forEach(e => e.remove()); say(p, "bones.after"); obj(p, "east"); stage(p, "east0"); }
            break;
        }
        case "east0": if (t > 160) { say(p, "think.east"); stage(p, "east"); } break;
        case "east":
            keepCrates(p, 2);
            if (reg === "leviathan" && Math.hypot(p.location.x, p.location.z) > 560 && r) {
                setTime(8000);
                play(p, "lev", () => choose(p, "lev", pick => {
                    score(p, pick === "look" ? "courage" : pick === "close" ? "accept" : "fury", 1);
                    say(p, "lev." + pick);
                    stage(p, "lev_sink");
                }));
            }
            break;
        case "lev_sink":
            if (t > 120) {
                for (const e of p.dimension.getEntities({ type: "bloodsea:leviathan", location: p.location, maxDistance: 160 })) e.remove();
                play(p, "mem2", () => { say(p, "think.west"); obj(p, "west"); stage(p, "west"); });
            }
            break;
        case "west":
            keepCrates(p, 2);
            if ((reg === "mirage" || moved > 380) && r) {
                setTime(15500);
                play(p, "voice", () => choose(p, "voice", pick => {
                    score(p, pick === "answer" ? "hope" : pick === "silent" ? "courage" : "accept", pick === "answer" ? 2 : 1);
                    say(p, "voice." + pick);
                    stage(p, "fog");
                }));
            }
            break;
        case "fog":
            if (t > 400 && r) play(p, "taken", () => {
                s.pip_gone = true; save(p);
                const pip = p.dimension.getEntities({ type: "bloodsea:pip", location: p.location, maxDistance: 48 });
                pip.forEach(e => e.remove());
                choose(p, "after", pick => {
                    score(p, pick as any, 2);
                    const dest = destination(p);
                    s.dest = dest; save(p);
                    say(p, "dest." + dest);
                    obj(p, "dest_" + dest);
                    setTime(4000);
                    stage(p, "dest");
                });
            });
            break;
        case "dest": {
            keepCrates(p, 1);
            if (t % 1200 === 600) say(p, "think.alone");
            const dest = s.dest ?? "hope";
            const goal = dest === "hope" ? { x: LIGHTHOUSE.x + 40, z: LIGHTHOUSE.z } : dest === "accept" ? { x: THRONE.x, z: THRONE.z + 70 }
                : dest === "courage" ? { x: MAW.x - 80, z: MAW.z } : undefined;
            const there = goal ? Math.hypot(p.location.x - goal.x, p.location.z - goal.z) < 60
                : Math.hypot(p.location.x, p.location.z) > EDGE_R - 40 && p.location.z > 0;
            if (there && r) ending(p, dest);
            break;
        }
    }
}

// ------------------------------------------------------------------ endings
function ending(p: Player, dest: string): void {
    stage(p, "ending");
    const id = dest === "hope" ? "mirage" : dest === "accept" ? "bone" : dest === "courage" ? "leviathan" : "edge";
    const end = () => finish(p, id);
    const atSet = (where: { x: number; y: number; z: number }, build: () => { x: number; y: number; z: number }, cutId: string, then: () => void) => {
        try { p.addEffect("invisibility", 900, { showParticles: false }); p.addEffect("resistance", 900, { amplifier: 255, showParticles: false }); } catch { /* */ }
        try { p.teleport({ x: where.x + 2.5, y: where.y + 1, z: where.z + 2.5 }); } catch { /* */ }
        // the world there arrives, the set is built, the scene plays in it
        later(60, () => {
            const o = build();
            later(20, () => playAt(p, cutId, o, 0, then, { x: o.x + 2.5, y: o.y + 1, z: o.z + 2.5 }));
        });
    };
    switch (id) {
        case "mirage": play(p, "mirage", () => atSet(HOSPITAL, hospitalB, "hospital", () => atSet(STOMACH, stomachB, "stomach", end))); break;
        case "bone": play(p, "throne", end); break;
        case "leviathan": play(p, "maw", () => atSet(RESCUE, rescueB, "rescue", end)); break;
        default: play(p, "edge", end);
    }
}

const hospitalB = () => hospital(world.getDimension("overworld"));
const stomachB = () => stomach(world.getDimension("overworld"));
const rescueB = () => rescue(world.getDimension("overworld"));

function finish(p: Player, id: string): void {
    const s = st(p);
    s.endings = s.endings ?? {};
    s.endings[id] = true;
    stage(p, "done");
    cmd(p.dimension, "weather clear");
    endingCard(p, id, Object.keys(s.endings).length, () => {
        const r = raftOf(p) ?? ensureRaft(p);
        if (r) aboard(p, r);
        try { p.camera.clear(); } catch { /* */ }
    });
}

/** development: /scriptevent bloodsea:stage <name> */
export function jump(p: Player, s: string): void {
    if (!active(p)) { st(p).began = true; save(p); }
    if (s.startsWith("dest_")) { st(p).dest = s.substring(5); st(p).pip_gone = true; s = "dest"; }
    stage(p, s);
    st(p).t = 100000;
    save(p);
}

export function resetStory(p: Player): void {
    p.setDynamicProperty("bloodsea:st", undefined);
    forget(p);
}
