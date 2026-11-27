// Plays the whole story in the stand-in world: start, every stage, every cutscene, every choice, and each of the
// four endings. Prints every warning the script makes and fails on any error.
import { world, system, tick, addPlayer, LOG, SOUNDS } from "./mc-server.mjs";
import { PICKS } from "./mc-server-ui.mjs";

const warnings = [];
const origWarn = console.warn;
console.warn = (...a) => { warnings.push(a.join(" ")); };

await import(process.argv[2]);
const run = process.argv[3] || "hope";

const p = addPlayer();
const st = () => JSON.parse(p.dp["bloodsea:st"] || "{}");
async function ticks(n) { for (let i = 0; i < n; i++) { tick(); if (i % 10 === 0) await new Promise(r => setTimeout(r, 0)); } }
const raft = () => world.getDimension().getEntities({ type: "bloodsea:raft" }).find(e => !e.tags.has("bloodsea_ghost"));
function moveTo(x, z) {
    const r = raft();
    if (r) r.location = { x, y: r.location.y, z };
    p.location = { x, y: 281.2, z };
}
async function until(cond, max, what) {
    for (let i = 0; i < max; i++) { if (cond()) return true; await ticks(20); }
    throw new Error(`stuck waiting for ${what}; stage=${st().stage} t=${st().t}`);
}
const stages = [];
let last = "";
const watch = setInterval(() => { const s = st().stage; if (s && s !== last) { stages.push(s); last = s; } }, 0);

world.afterEvents.playerSpawn.fire({ player: p, initialSpawn: true });
await until(() => ["oar", "rowing", "hungry"].includes(st().stage), 200, "oar");
// CONTROLS TEST: the hotbar controls are there, and they work
const held = p.inv.container.items.filter(Boolean).map(i => i.typeId);
for (const id of ["bloodsea:ctl_oar", "bloodsea:sea_map", "bloodsea:ctl_pip", "bloodsea:ctl_skip"]) if (!held.includes(id)) throw new Error("missing control " + id);
world.afterEvents.itemUse.fire({ source: p, itemStack: { typeId: "bloodsea:ctl_oar" } });
const sat = !!p.ridingOn;
world.afterEvents.itemUse.fire({ source: p, itemStack: { typeId: "bloodsea:ctl_oar" } });
if (sat === !!p.ridingOn) throw new Error("oar control did nothing");
world.afterEvents.itemUse.fire({ source: p, itemStack: { typeId: "bloodsea:ctl_pip" } });
world.afterEvents.itemUse.fire({ source: p, itemStack: { typeId: "bloodsea:sea_map" } });
console.log("controls ok:", held.join(", "));
if (!p.ridingOn) p.ridingOn = raft();
await until(() => st().stage === "hungry", 300, "hungry");
const crate = world.getDimension().spawnEntity("bloodsea:crate", { x: p.location.x + 2, y: p.location.y, z: p.location.z });
world.afterEvents.dataDrivenEntityTrigger.fire({ entity: crate, eventId: "bloodsea:hauled" });
await until(() => st().stage === "day1", 50, "day1");
// skip the next scene with the control once it starts
let skipped = false;
const skipper = setInterval(() => { if (!skipped && p.cameraCalls.length && p.effects.invisibility) { world.afterEvents.itemUse.fire({ source: p, itemStack: { typeId: "bloodsea:ctl_skip" } }); skipped = true; } }, 0);
// stroke the cat once
const pip = world.getDimension().getEntities({ type: "bloodsea:pip" })[0];
if (!pip) throw new Error("no Pip");
world.afterEvents.dataDrivenEntityTrigger.fire({ entity: pip, eventId: "bloodsea:petted" });
await until(() => st().stage === "north", 2500, "north (through the night)");
if (run !== "hope") {
    // the other endings: straight to the last stretch
    const dest = run === "hope2" ? "hope" : run;
    system.afterEvents.scriptEventReceive.fire({ id: "bloodsea:stage", message: "dest_" + dest, sourceEntity: p });
    const goal = { hope: [-1410, 0], accept: [0, -1380], courage: [1370, 0], fury: [0, 1880] }[dest];
    moveTo(goal[0], goal[1]);
    await until(() => st().stage === "done", 2500, "the end");
} else {
    moveTo(0, -300);
    await until(() => st().stage === "north2", 300, "north2");
    moveTo(0, -700);
    await until(() => st().stage === "east0", 2500, "east0");
    await until(() => st().stage === "east", 300, "east");
    moveTo(700, 0);
    await until(() => st().stage === "west", 1500, "west");
    moveTo(-700, 0);
    PICKS.next = 0;
    await until(() => st().stage === "dest", 2500, "dest");
    console.log("destination", st().dest);
    const g2 = { hope: [-1410, 0], accept: [0, -1380], courage: [1370, 0], fury: [0, 1880] }[st().dest];
    moveTo(g2[0], g2[1]);
    await until(() => st().stage === "done", 3000, "the end");
}
await ticks(100);
clearInterval(watch);
console.warn = origWarn;
const errors = warnings.filter(w => /failed|Error|error|undefined|NaN|not a function|cannot/i.test(w));
console.log("run", run, "stages:", stages.join(" > "));
console.log("forms shown:", PICKS.shown.length, " sounds used:", SOUNDS.size, " kills:", p.killed || 0);
console.log("cuts:", warnings.filter(w => w.includes("begins")).map(w => w.split(" ")[2]).join(" "));
if (errors.length) { console.log("ERRORS:\n" + [...new Set(errors)].slice(0, 40).join("\n")); process.exit(1); }
console.log("OK");
process.exit(0);
