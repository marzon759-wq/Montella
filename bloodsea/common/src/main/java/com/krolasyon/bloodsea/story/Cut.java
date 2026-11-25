package com.krolasyon.bloodsea.story;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * One cutscene: camera moves, spoken lines and things that happen, on a timeline in ticks. Positions are in a local
 * frame (blocks: x to the left, y up, z ahead) around an origin and a heading the server picks when it starts the
 * cutscene - usually the raft's deck. Shared by client (camera, lines) and server (events).
 */
public final class Cut {
    public static final class Shot {
        public final int t0, t1;
        public final double[] p0, p1, l0, l1;
        public final float fov0, fov1;

        Shot(int t0, int t1, double[] p0, double[] l0, double[] p1, double[] l1, float fov0, float fov1) {
            this.t0 = t0; this.t1 = t1; this.p0 = p0; this.l0 = l0; this.p1 = p1; this.l1 = l1; this.fov0 = fov0; this.fov1 = fov1;
        }
    }

    public record Say(int t, String key) {}

    public record Ev(int t, Consumer<Story.Session> run) {}

    public final String id;
    public int length;
    public final List<Shot> shots = new ArrayList<>();
    public final List<Say> lines = new ArrayList<>();
    public final List<Ev> events = new ArrayList<>();
    /** the picture is black between these ticks (memories, heard and not seen) */
    public int blackFrom = -1, blackTo = -1;
    /** white out (a blinding light) from this tick to the end */
    public int whiteFrom = -1;
    /** the choice put to the player when it ends, if any */
    public String choice;
    private int cursor;

    Cut(String id) { this.id = id; }

    static double[] v(double x, double y, double z) { return new double[]{x, y, z}; }

    /** a moving shot: from p0 looking at l0 to p1 looking at l1 */
    Cut shot(int dur, double[] p0, double[] l0, double[] p1, double[] l1, float fov0, float fov1) {
        shots.add(new Shot(cursor, cursor + dur, p0, l0, p1, l1, fov0, fov1));
        cursor += dur;
        length = Math.max(length, cursor);
        return this;
    }

    Cut shot(int dur, double[] p0, double[] l0, double[] p1, double[] l1) { return shot(dur, p0, l0, p1, l1, 60, 60); }

    Cut hold(int dur, double[] p, double[] l) { return shot(dur, p, l, p, l, 60, 60); }

    Cut say(int t, String key) {
        lines.add(new Say(t, key));
        length = Math.max(length, t + Lines.ticks(key));
        return this;
    }

    Cut at(int t, Consumer<Story.Session> e) {
        events.add(new Ev(t, e));
        return this;
    }

    Cut black(int from, int to) { blackFrom = from; blackTo = to; return this; }
    Cut white(int from) { whiteFrom = from; return this; }
    Cut ask(String c) { choice = c; return this; }
    Cut len(int l) { length = Math.max(length, l); return this; }

    public Shot shotAt(float t) {
        for (Shot s : shots) if (t >= s.t0 && t < s.t1) return s;
        return shots.isEmpty() ? null : shots.get(shots.size() - 1);
    }
}
