// The Watcher: the white sun is an eye. While the story keeps it open, an eye is drawn over the sun (a camera-facing
// particle, re-made every couple of ticks along the sun's direction): it opens like an eyelid, stays, and closes.
// Stare at it while it is open and every creature in the sea learns where you are.
import { world, Player, MolangVariableMap } from "@minecraft/server";
import { st, save, isDay, sfx, later } from "./util";

interface Open { until: number; t: number; }
const OPEN = new Map<string, Open>();
let tick = 0;

/** open the eye for this player, for so many ticks (0 shuts it) */
export function eye(p: Player, ticks: number): void {
    if (ticks <= 0) { OPEN.delete(p.id); return; }
    OPEN.set(p.id, { until: tick + ticks, t: 0 });
    st(p).eye_until = world.getAbsoluteTime() + ticks;
    save(p);
}

export function eyeOpen(p: Player): boolean { return (st(p).eye_until ?? 0) > world.getAbsoluteTime(); }

function sunDir(): { x: number; y: number; z: number } {
    const t = world.getTimeOfDay() % 24000;
    const a = ((t - 6000) / 24000) * Math.PI * 2;
    return { x: -Math.sin(a), y: Math.cos(a), z: 0 };
}

const STARE = new Map<string, number>();

export function tickWatcher(onAlert: (p: Player) => void): void {
    tick++;
    for (const p of world.getAllPlayers()) {
        const o = OPEN.get(p.id);
        if (o) {
            o.t++;
            const left = o.until - tick;
            if (left < -30) { OPEN.delete(p.id); continue; }
            // 0..1: opening over two seconds, closing over one and a half
            const open = Math.max(0, Math.min(1, Math.min(o.t / 40, left / 30)));
            if (tick % 2 === 0 && isDay()) {
                const d = sunDir();
                if (d.y > -0.05) {
                    const e = p.getHeadLocation();
                    const D = 28;
                    const vars = new MolangVariableMap();
                    vars.setFloat("variable.open", open);
                    vars.setFloat("variable.size", 7.2);
                    try { p.dimension.spawnParticle("bloodsea:watcher", { x: e.x + d.x * D, y: e.y + d.y * D, z: e.z + d.z * D }, vars); } catch { /* */ }
                }
            }
        }
        // the stare
        if (!isDay() || !eyeOpen(p)) { STARE.delete(p.id); continue; }
        const v = p.getViewDirection(), d = sunDir();
        let s = STARE.get(p.id) ?? 0;
        if (v.x * d.x + v.y * d.y + v.z * d.z > 0.985) {
            s++;
            if (s === 1) sfx(p.dimension, "watcher_stare", p.location, 1.2, 1);
            if (s >= 40) {
                onAlert(p);
                try { p.addEffect("weakness", 200, { showParticles: false }); } catch { /* */ }
                sfx(p.dimension, "heartbeat_near", p.location, 1.5, 1);
                s = -120;
            }
        } else if (s > 0) s = 0;
        else if (s < 0) s++;
        STARE.set(p.id, s);
    }
}
