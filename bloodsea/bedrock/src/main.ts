// Blood Sea for Bedrock: the wiring. Everything runs from here: the clocks, the events, the story.
import { world, system, Player, Entity } from "@minecraft/server";
import { tickClock, active, st, save, setAnim } from "./util";
import { defineCuts } from "./cuts.gen";
import { tickUi, showMap } from "./ui";
import { tickSessions, inCut } from "./session";
import { keepSea, bloodTick, cratesTouch, haul, raftOf } from "./sea";
import { tickPips, onPetted } from "./pip";
import { tickCreatures, directCreatures, alertAll } from "./creatures";
import { tickWatcher, eye } from "./watcher";
import { tickMusic } from "./music";
import { tickOmens, forceOmen, tickRain, sweepGhosts } from "./omens";
import { tickStory, onJoin, onRespawn, onCrate, onPet, jump, resetStory, start, firstPerson } from "./story";

defineCuts();

let ticks = 0;

system.runInterval(() => {
    ticks++;
    tickClock();
    tickUi();
    tickSessions();
    tickPips();
    tickCreatures();
    tickWatcher(p => alertAll(p));
    tickRain();
    for (const p of world.getAllPlayers()) bloodTick(p, inCut(p));
    if (ticks % 20 === 0) {
        keepSea();
        tickMusic();
        for (const p of world.getAllPlayers()) {
            if (!active(p)) continue;
            const cut = inCut(p);
            try { tickStory(p); } catch (e) { console.warn("[bloodsea] story: " + e); }
            try { directCreatures(p, cut); } catch (e) { console.warn("[bloodsea] creatures: " + e); }
            try { tickOmens(p, cut); } catch (e) { console.warn("[bloodsea] omens: " + e); }
            if (!cut) cratesTouch(p, onCrate);
            if (!cut && st(p).eye_free && Math.random() < 1 / 150) eye(p, 160);
        }
    }
    if (ticks % 200 === 0) sweepGhosts();
    // the raft rows when it is rowed
    if (ticks % 5 === 0) for (const p of world.getAllPlayers()) {
        const r = raftOf(p);
        if (!r) continue;
        const v = r.getVelocity();
        setAnim(r, Math.hypot(v.x, v.z) > 0.02 ? "row" : "idle");
    }
}, 1);

world.afterEvents.playerSpawn.subscribe(ev => {
    if (ev.initialSpawn) onJoin(ev.player);
    else onRespawn(ev.player);
});

world.afterEvents.itemUse.subscribe(ev => {
    if (ev.itemStack.typeId === "bloodsea:sea_map") showMap(ev.source as Player);
});

world.afterEvents.dataDrivenEntityTrigger.subscribe(ev => {
    const e: Entity = ev.entity;
    if (ev.eventId === "bloodsea:petted") onPetted(e, onPet);
    if (ev.eventId === "bloodsea:hauled") {
        const p = e.dimension.getPlayers({ location: e.location, maxDistance: 8, closest: 1 })[0];
        if (p) haul(p, e, onCrate);
    }
});

// development and help: /scriptevent bloodsea:<what> [arg]
system.afterEvents.scriptEventReceive.subscribe(ev => {
    const p = ev.sourceEntity as Player | undefined;
    if (!p || p.typeId !== "minecraft:player") return;
    switch (ev.id) {
        case "bloodsea:start": resetStory(p); start(p); break;
        case "bloodsea:stage": jump(p, ev.message.trim()); break;
        case "bloodsea:omen": forceOmen(p, ev.message.trim()); break;
        case "bloodsea:eye": eye(p, Number(ev.message) || 200); break;
        case "bloodsea:fp": firstPerson(p); break;
    }
});
