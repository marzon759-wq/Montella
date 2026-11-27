// What the player reads: spoken lines (with a murmur per speaker), the objective, the choices, the map, the endings.
import { Player, system, world } from "@minecraft/server";
import { ActionFormData, FormCancelationReason } from "@minecraft/server-ui";
import { LINES, CHOICES } from "./lines.gen";
import { st, save, later, playTo, tr, DRIFT_R, EDGE_R, THRONE, MAW, LIGHTHOUSE } from "./util";
import { regionAt } from "./sea";

const SPEAKER_COLOUR: Record<string, string> = { elias: "§6", anna: "§e", voice: "§4", nurse: "§b", rescuer: "§7", man: "§7" };
const SPEAKER_PITCH: Record<string, number> = { elias: 1.0, anna: 1.45, voice: 0.62, nurse: 1.3, rescuer: 0.85, man: 0.85 };

interface Line { key: string; until: number; next: number; }
const SPEAKING = new Map<string, Line>();
let tick = 0;

/** a line of dialogue on the action bar, with a murmur while it is "spoken" */
export function say(p: Player, key: string): void {
    const L = LINES[key];
    const ticks = L ? L[1] : 60;
    const speaker = L ? L[0] : "elias";
    SPEAKING.set(p.id, { key, until: tick + ticks + 30, next: tick });
    const murmur = Math.min(ticks, Math.max(20, ticks * 0.6));
    for (let i = 0; i < murmur; i += 3) {
        later(i + 1, () => {
            if (!p.isValid()) return;
            playTo(p, "talk_blip", 0.32, (SPEAKER_PITCH[speaker] ?? 1) * (0.94 + Math.random() * 0.12));
        });
    }
}

export function speaking(p: Player): boolean { const l = SPEAKING.get(p.id); return !!l && l.until > tick; }

/** every tick: keep the current line up */
export function tickUi(): void {
    tick++;
    for (const p of world.getAllPlayers()) {
        const l = SPEAKING.get(p.id);
        if (!l) continue;
        if (tick > l.until) { SPEAKING.delete(p.id); continue; }
        if (tick >= l.next) {
            l.next = tick + 20;
            const L = LINES[l.key];
            const speaker = L ? L[0] : "elias";
            try {
                p.onScreenDisplay.setActionBar({ rawtext: [{ text: (SPEAKER_COLOUR[speaker] ?? "§6") }, { translate: `speaker.bloodsea.${speaker}` },
                    { text: "§r: §f" }, { translate: `line.bloodsea.${l.key}` }] });
            } catch { /* not ready */ }
        }
    }
}

/** the objective: shown as a title card, and kept */
export function obj(p: Player, key: string): void {
    st(p).obj = key;
    save(p);
    showObjective(p);
}

export function showObjective(p: Player): void {
    const key = st(p).obj;
    if (!key) return;
    try {
        p.onScreenDisplay.setTitle(" ", { subtitle: { rawtext: [{ text: "§c" }, { translate: "hud.bloodsea.objective" }, { text: ": §f" }, { translate: `obj.bloodsea.${key}` }] },
            fadeInDuration: 10, stayDuration: 90, fadeOutDuration: 20 });
    } catch { /* not ready */ }
}

/** a small line on the action bar when nothing is being said (the dawn countdown, a reminder) */
export function hint(p: Player, msg: any): void {
    if (speaking(p)) return;
    try { p.onScreenDisplay.setActionBar(msg); } catch { /* not ready */ }
}

/** a choice: the question and its answers; the first answer if the player shuts it */
export function ask(p: Player, id: string, then: (pick: string) => void): void {
    const opts = CHOICES[id] ?? [];
    const f = new ActionFormData().title({ translate: "hud.bloodsea.choose" }).body({ translate: `choiceq.bloodsea.${id}` });
    for (const o of opts) f.button({ translate: `choice.bloodsea.${id}.${o}` });
    const show = (tries: number) => {
        f.show(p as any).then(r => {
            if (r.canceled) {
                if (r.cancelationReason === FormCancelationReason.UserBusy && tries < 40) { system.runTimeout(() => show(tries + 1), 10); return; }
                then(opts[0]);
                return;
            }
            then(opts[r.selection ?? 0] ?? opts[0]);
        }).catch(() => then(opts[0]));
    };
    show(0);
}

/** the welcome, once: what this is, and who it is brought to you by */
export function welcome(p: Player, then: () => void): void {
    const f = new ActionFormData().title({ translate: "ui.bloodsea.welcome.title" }).body({ translate: "ui.bloodsea.welcome.body" })
        .button({ translate: "ui.bloodsea.welcome.start" }, "textures/items/bloodsea/sea_map");
    const show = (tries: number) => {
        f.show(p as any).then(r => {
            if (r.canceled && r.cancelationReason === FormCancelationReason.UserBusy && tries < 60) { system.runTimeout(() => show(tries + 1), 10); return; }
            then();
        }).catch(() => then());
    };
    show(0);
}

const DIRS = ["n", "ne", "e", "se", "s", "sw", "w", "nw"];
function dirKey(dx: number, dz: number): string {
    const a = (Math.atan2(dx, -dz) * 180 / Math.PI + 360 + 22.5) % 360;
    return `dir.bloodsea.${DIRS[Math.floor(a / 45)]}`;
}

/** the hand-drawn map: where you are, and which way the places it marks lie */
export function showMap(p: Player): void {
    const x = p.location.x, z = p.location.z;
    const reg = regionAt(x, z);
    const s = st(p);
    let goalKey = "", gx = 0, gz = 0;
    const dest = s.dest;
    if (dest === "hope") { goalKey = "region.bloodsea.mirage"; gx = LIGHTHOUSE.x; gz = LIGHTHOUSE.z; }
    else if (dest === "accept") { goalKey = "region.bloodsea.bones"; gx = THRONE.x; gz = THRONE.z; }
    else if (dest === "courage") { goalKey = "region.bloodsea.leviathan"; gx = MAW.x; gz = MAW.z; }
    else if (dest === "fury") { goalKey = "region.bloodsea.edge"; gx = 0; gz = EDGE_R; }
    else if (s.stage.startsWith("north")) { goalKey = "region.bloodsea.bones"; gx = 0; gz = -DRIFT_R - 120; }
    else if (s.stage.startsWith("east")) { goalKey = "region.bloodsea.leviathan"; gx = DRIFT_R + 200; gz = 0; }
    else if (s.stage === "west" || s.stage === "fog") { goalKey = "region.bloodsea.mirage"; gx = -DRIFT_R - 120; gz = 0; }
    const line = goalKey
        ? { rawtext: [{ translate: "ui.bloodsea.map.dir", with: { rawtext: [{ translate: goalKey }, { translate: dirKey(gx - x, gz - z) }, { text: String(Math.round(Math.hypot(gx - x, gz - z))) }] } }] }
        : { rawtext: [{ text: "" }] };
    const body = { rawtext: [{ translate: "ui.bloodsea.map.where", with: { rawtext: [{ translate: `region.bloodsea.${reg}` }, { text: String(Math.round(Math.hypot(x, z))) }, line] } }] };
    new ActionFormData().title({ translate: "ui.bloodsea.map.title" }).body(body as any).button({ translate: "ui.bloodsea.map.close" })
        .show(p as any).catch(() => { /* shut */ });
}

/** the end card of an ending */
export function endingCard(p: Player, id: string, seen: number, then: () => void): void {
    const rows: any[] = [{ text: "§c§l" }, { translate: `ending.bloodsea.${id}.title` }, { text: "§r\n\n" }];
    for (let i = 0; i < 4; i++) rows.push({ translate: `ending.bloodsea.${id}.${i}` }, { text: "\n\n" });
    rows.push({ text: "§7" }, { translate: "ending.bloodsea.label", with: [String(seen), "4"] });
    rows.push({ text: "\n\n§7by Kyvix Photo Editor" });
    const f = new ActionFormData().title({ translate: "hud.bloodsea.ending" }).body({ rawtext: rows }).button({ translate: "ui.bloodsea.ending.close" });
    const show = (tries: number) => {
        f.show(p as any).then(r => {
            if (r.canceled && r.cancelationReason === FormCancelationReason.UserBusy && tries < 60) { system.runTimeout(() => show(tries + 1), 10); return; }
            then();
        }).catch(() => then());
    };
    show(0);
}
