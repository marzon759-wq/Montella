package com.krolasyon.mimic.story;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Camera work and subtitles for every cutscene, in farm-local coordinates. Shared by client and server. */
public final class Cutscenes {
    public static final class Shot {
        public final int t0, t1;
        public final double x0, y0, z0, x1, y1, z1;
        public final float yaw0, pitch0, yaw1, pitch1, fov0, fov1;

        Shot(int t0, int t1, double x0, double y0, double z0, float yaw0, float pitch0, double x1, double y1, double z1, float yaw1, float pitch1, float fov0, float fov1) {
            this.t0 = t0; this.t1 = t1;
            this.x0 = x0; this.y0 = y0; this.z0 = z0; this.yaw0 = yaw0; this.pitch0 = pitch0;
            this.x1 = x1; this.y1 = y1; this.z1 = z1; this.yaw1 = yaw1; this.pitch1 = pitch1;
            this.fov0 = fov0; this.fov1 = fov1;
        }
    }

    public static final class Line {
        public final int t, dur;
        public final String key, speaker;

        Line(int t, int dur, String key, String speaker) { this.t = t; this.dur = dur; this.key = key; this.speaker = speaker; }
    }

    public static final class Cut {
        public final String id;
        public int length;
        public final List<Shot> shots = new ArrayList<>();
        public final List<Line> lines = new ArrayList<>();
        public float fog = 40;
        /** the picture is black (an audio only moment) */
        public boolean black = false;
        private int cursor = 0;

        Cut(String id) { this.id = id; }

        Cut shot(int dur, double x0, double y0, double z0, float yaw0, float pitch0, double x1, double y1, double z1, float yaw1, float pitch1, float fov0, float fov1) {
            shots.add(new Shot(cursor, cursor + dur, x0, y0, z0, yaw0, pitch0, x1, y1, z1, yaw1, pitch1, fov0, fov1));
            cursor += dur;
            length = cursor;
            return this;
        }

        /** a held shot */
        Cut hold(int dur, double x, double y, double z, float yaw, float pitch, float fov) {
            return shot(dur, x, y, z, yaw, pitch, x, y, z, yaw, pitch, fov, fov);
        }

        Cut line(int t, int dur, String speaker) {
            lines.add(new Line(t, dur, "cut." + id + "." + lines.size(), speaker));
            return this;
        }

        Cut fog(float f) { fog = f; return this; }

        Cut black() { black = true; return this; }

        /** black at this moment: the whole cutscene, or a held shot at 0,0,0 */
        public boolean blackAt(int t) {
            if (black) return true;
            for (Shot sh : shots)
                if (t >= sh.t0 && t < sh.t1) return sh.x0 == 0 && sh.y0 == 0 && sh.z0 == 0;
            return false;
        }
    }

    public static final Map<String, Cut> ALL = new HashMap<>();

    private static Cut cut(String id) {
        Cut c = new Cut(id);
        ALL.put(id, c);
        return c;
    }

    static {
        // yaw: 180 looks north, 0 south, 90 west, -90 east
        cut("intro").fog(90)
                .shot(160, 50.5, 4.5, 114, 180, -3, 50.5, 3.4, 106, 180, -1, 62, 56)        // the road, the gate, the farm beyond
                .shot(120, 42.6, 2.4, 84.6, -45, 2, 43.6, 2.2, 85.4, -38, -2, 50, 40)        // the scarecrows by the road
                .shot(160, 58, 6, 74, 194, -6, 60, 5.5, 66, 190, -9, 58, 50)                // the barn and the silo from the corn
                .shot(130, 5, 8.5, 71, -119, -3, 7, 8.3, 69.5, -116, -3, 50, 42)            // a light on in Lily's window
                .hold(140, 50.5, 2.62, 103, 180, 2, 60)                                   // you, at the gate
                .line(14, 140, "lily").line(170, 130, "lily").line(310, 130, "lily").line(450, 120, "lily").line(600, 80, "you");

        cut("landing").fog(30)
                .shot(70, 25.5, 7.62, 62.4, 180, 2, 25.5, 7.62, 60.6, 180, 2, 60, 52)
                .shot(56, 25.5, 9.0, 57.2, 180, 12, 25.5, 9.2, 56.6, 180, 14, 46, 36)
                .line(20, 50, "you");

        cut("barn").fog(40)
                .shot(110, 70.5, 2.62, 49.5, 180, -2, 70.5, 2.62, 43.5, 180, -18, 60, 54)
                .shot(140, 68.2, 1.8, 34.5, 150, -48, 68.6, 1.8, 33.5, 152, -52, 52, 44)
                .shot(80, 70.5, 2.2, 38.5, 180, -30, 70.5, 2.2, 38.0, 180, -30, 60, 58)
                .line(130, 120, "you").line(250, 80, "you");

        cut("jump").fog(40)
                .shot(30, 62.5, 8.62, 44.2, 180, 30, 62.5, 8.62, 45.6, 180, 55, 70, 76)
                .shot(40, 63.5, 3.5, 56, 0, -24, 63.5, 3.2, 55, 0, -14, 60, 60)
                .line(40, 40, "you");

        cut("rescue").fog(26)
                .shot(80, 83.5, 2.62, 31.5, -90, 0, 85.2, 2.62, 31.5, -90, 4, 60, 55)
                .shot(120, 86.6, 1.8, 33.4, -130, 6, 86.6, 1.8, 33.0, -134, 6, 46, 42)
                .shot(80, 85.4, 2.62, 31.5, -90, 6, 85.6, 2.62, 31.5, -90, 6, 50, 50)
                .line(60, 96, "lily").line(160, 56, "you").line(220, 58, "lily");

        cut("ambush").fog(36)
                .shot(60, 66.5, 2.62, 52.5, 160, 0, 66.5, 2.62, 52.5, 172, -2, 60, 58)
                .shot(40, 68.5, 2.2, 49.5, 180, -8, 68.5, 2.0, 49.0, 180, -12, 44, 36)
                .shot(80, 64.5, 1.2, 52.5, -150, -30, 64.5, 1.0, 52.5, -150, -34, 60, 60)
                .line(70, 40, "lily").line(115, 64, "lily");

        cut("two").fog(44)
                .shot(90, 45.2, 2.62, 51.0, 170, 4, 45.6, 2.62, 49.6, 176, 6, 56, 46)
                .shot(110, 47.5, 2.62, 50.5, -56, 0, 47.5, 2.62, 50.5, -64, 0, 60, 50)
                .line(20, 115, "lily_q").line(140, 58, "lily");

        cut("burn").fog(40)
                .shot(80, 76.5, 2.62, 77.0, 0, 6, 76.5, 2.62, 79.0, 0, 8, 60, 52)
                .shot(60, 74.5, 1.8, 82.0, -60, -6, 74.5, 1.8, 82.0, -66, -10, 50, 44)
                .shot(140, 76.5, 3.6, 77.5, 0, 14, 76.5, 4.4, 76.0, 0, 18, 56, 60)
                .line(30, 60, "you").line(220, 60, "lily");

        // ---- the endings. A held shot at 0,0,0 is a black screen (sound only).
        cut("end_dawn").fog(170)
                .shot(120, 75.0, 8.0, 78.5, -12, 48, 75.4, 7.0, 79.5, -12, 52, 56, 50)          // it lies in the corn, the fork standing out of it
                .shot(130, 69.0, 2.2, 75.0, -45, 6, 69.4, 2.2, 75.4, -45, 6, 50, 44)             // Lily, holding on to you
                .shot(80, 40.0, 4.0, 58.0, -150, 12, 41.0, 4.2, 56.5, -150, 12, 58, 54)          // the yard at dawn, the truck turning over
                .shot(130, 54.0, 1.7, 104.0, 148, -4, 53.8, 1.7, 103.4, 150, -3, 62, 60)         // the chained gate - straight through it
                .shot(110, 51.6, 2.6, 100.5, -8, 2, 51.4, 2.8, 101.0, -4, 4, 56, 50)             // the road, the truck going away
                .shot(260, 50.5, 14.0, 124.0, 180, 24, 50.5, 18.0, 128.0, 180, 26, 62, 58)      // the farm from the road, the sun coming up
                .line(20, 100, "lily").line(140, 90, "you").line(260, 70, "lily").line(470, 80, "you").line(600, 80, "lily_q").line(700, 60, "you");

        cut("end_harvest").fog(60)
                .shot(120, 72.5, 2.62, 80.0, 0, 4, 72.5, 2.62, 78.0, 0, 2, 60, 52)             // it has her
                .shot(100, 42.0, 2.5, 52.0, -150, 6, 42.6, 2.5, 51.2, -150, 6, 56, 52)           // the truck: you, alone
                .shot(120, 54.0, 1.7, 104.0, 148, -4, 53.8, 1.7, 103.4, 150, -3, 62, 60)         // the gate
                .shot(160, 51.6, 2.6, 100.5, -8, 2, 51.4, 2.8, 101.0, -4, 4, 56, 50)             // tail lights down the road, the walkie talking
                .shot(220, 50.5, 6.0, 124.0, 180, 8, 50.5, 6.5, 120.0, 180, 8, 50, 40)           // the farm - and something at the gate, watching
                .line(20, 70, "lily").line(150, 80, "you").line(370, 120, "lily_q").line(560, 60, "lily_q");

        cut("end_passenger").fog(60)
                .shot(110, 42.0, 2.5, 52.0, -150, 6, 42.6, 2.5, 51.2, -150, 6, 56, 52)           // the truck pulls away, Lily beside you
                .shot(120, 54.0, 1.7, 104.0, 148, -4, 53.8, 1.7, 103.4, 150, -3, 62, 60)         // the gate
                .shot(150, 50.5, 4, 106, 180, 2, 50.5, 4, 116, 180, 2, 56, 50)                   // the farm falling away behind
                .shot(110, 46.4, 2.4, 48.2, -125, 2, 46.5, 2.4, 48.0, -125, 2, 54, 48)          // the passenger seat
                .shot(50, 46.5, 2.5, 47.9, -125, 0, 46.6, 2.5, 47.8, -125, 0, 40, 34)
                .hold(90, 0, 0, 0, 0, 0, 60)
                .line(280, 70, "you").line(400, 60, "lily_q").line(470, 40, "lily_q");

        cut("end_learned").fog(40)
                .hold(80, 0, 0, 0, 0, 0, 60)                                                      // black: the last thing you hear
                .shot(170, 25.5, 3.0, 88.0, 180, 2, 25.5, 3.0, 82.0, 180, 2, 58, 50)             // morning: the farmhouse, quiet
                .shot(160, 25.4, 2.6, 62.6, 162, 10, 25.2, 2.6, 61.6, 162, 12, 56, 44)           // the telephone in the hall, ringing
                .shot(150, 25.5, 2.6, 67.8, 180, -24, 25.5, 2.7, 67.0, 180, -26, 60, 50)         // up the stairs: it is standing there
                .hold(100, 0, 0, 0, 0, 0, 60)
                .line(110, 100, "you").line(300, 110, "you").line(420, 80, "it");
    }

    private Cutscenes() {}
}
