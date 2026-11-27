// The score: drift by day, night after dark, dread when the eye is open or something hunts close. One at a time,
// faded across, replacing the game's own music.
import { world, Player } from "@minecraft/server";
import { isDay, active } from "./util";
import { eyeOpen } from "./watcher";
import { anyHunting } from "./creatures";

let playing = "";
let dreadHold = 0;

export function dread(ticks: number): void { dreadHold = Math.max(dreadHold, ticks); }

/** every second */
export function tickMusic(): void {
    if (dreadHold > 0) dreadHold -= 20;
    const ps = world.getAllPlayers().filter(p => active(p));
    let want = "";
    if (ps.length) {
        if (ps.some(p => eyeOpen(p) || anyHunting(p))) dreadHold = Math.max(dreadHold, 160);
        want = dreadHold > 0 ? "dread" : isDay() ? "drift" : "night";
    }
    if (want === playing) return;
    playing = want;
    try {
        if (want === "") world.stopMusic();
        else world.playMusic(`bloodsea.music_${want}`, { fade: 3, loop: true, volume: want === "dread" ? 0.85 : 0.7 });
    } catch (e) { console.warn("[bloodsea] music: " + e); }
}

export function restartMusic(): void { playing = ""; }
