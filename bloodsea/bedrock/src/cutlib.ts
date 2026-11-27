// The cutscene builder: the same chain the Java mod writes its scenes with (shot, hold, say, at, black, white, len).
import type { Session } from "./session";
import { LINES } from "./lines.gen";

export type V = [number, number, number];
export function v(x: number, y: number, z: number): V { return [x, y, z]; }

export interface Shot { t0: number; t1: number; p0: V; l0: V; p1: V; l1: V; }
export interface Say { t: number; key: string; }
export interface Ev { t: number; run: (s: Session) => void; }

export class Cut {
    readonly id: string;
    length = 0;
    shots: Shot[] = [];
    lines: Say[] = [];
    events: Ev[] = [];
    blackFrom = -1;
    blackTo = -1;
    whiteFrom = -1;
    choice?: string;
    private cursor = 0;

    constructor(id: string) { this.id = id; }

    shot(dur: number, p0: V, l0: V, p1: V, l1: V, _fov0 = 60, _fov1 = 60): Cut {
        this.shots.push({ t0: this.cursor, t1: this.cursor + dur, p0, l0, p1, l1 });
        this.cursor += dur;
        this.length = Math.max(this.length, this.cursor);
        return this;
    }

    hold(dur: number, p: V, l: V): Cut { return this.shot(dur, p, l, p, l); }

    say(t: number, key: string): Cut {
        this.lines.push({ t, key });
        this.length = Math.max(this.length, t + (LINES[key] ? LINES[key][1] : 60));
        return this;
    }

    at(t: number, run: (s: Session) => void): Cut {
        this.events.push({ t, run });
        return this;
    }

    black(from: number, to: number): Cut { this.blackFrom = from; this.blackTo = to; return this; }
    white(from: number): Cut { this.whiteFrom = from; return this; }
    ask(c: string): Cut { this.choice = c; return this; }
    len(l: number): Cut { this.length = Math.max(this.length, l); return this; }

    shotAt(t: number): Shot | undefined {
        for (const s of this.shots) if (t >= s.t0 && t < s.t1) return s;
        return this.shots.length ? this.shots[this.shots.length - 1] : undefined;
    }
}

export const CUTS = new Map<string, Cut>();

export function cut(id: string): Cut {
    const c = new Cut(id);
    CUTS.set(id, c);
    return c;
}
