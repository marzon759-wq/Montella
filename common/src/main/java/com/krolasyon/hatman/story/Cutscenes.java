package com.krolasyon.hatman.story;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Camera work and subtitles for every cutscene, in house-local coordinates. Shared by client and server. */
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
        /** fog far plane while this plays */
        public float fog = 40;
        private int cursor = 0;

        Cut(String id) { this.id = id; }

        /** a shot lasting dur ticks, moving from A to B */
        Cut shot(int dur, double x0, double y0, double z0, float yaw0, float pitch0, double x1, double y1, double z1, float yaw1, float pitch1, float fov0, float fov1) {
            shots.add(new Shot(cursor, cursor + dur, x0, y0, z0, yaw0, pitch0, x1, y1, z1, yaw1, pitch1, fov0, fov1));
            cursor += dur;
            length = cursor;
            return this;
        }

        Cut line(int t, int dur, String speaker) {
            lines.add(new Line(t, dur, "cut." + id + "." + lines.size(), speaker));
            return this;
        }

        Cut fog(float f) { fog = f; return this; }
    }

    public static final Map<String, Cut> ALL = new HashMap<>();

    private static Cut cut(String id) {
        Cut c = new Cut(id);
        ALL.put(id, c);
        return c;
    }

    static {
        // yaw: 180 looks north (toward the house from the gate), 0 south, 90 west, -90 east
        cut("intro").fog(64)
                .shot(170, 22.5, 4.2, 57.5, 180, -6, 22.5, 3.2, 54.0, 180, -9, 62, 58)
                .shot(150, 23.4, 1.5, 50.0, 180, -15, 22.6, 1.7, 41.0, 180, -17, 60, 60)
                .shot(110, 30.0, 2.2, 37.0, 150, -4, 25.0, 2.0, 35.6, 128, -2, 55, 48)
                .shot(110, 22.5, 2.0, 38.0, 180, -26, 22.5, 2.2, 36.0, 180, -32, 50, 42)
                .shot(110, 23.6, 2.7, 58.6, 172, 4, 23.0, 2.6, 57.6, 176, 2, 58, 54)
                .line(20, 90, "ruth").line(120, 100, "ruth").line(230, 90, "ruth").line(330, 90, "ruth")
                .line(430, 90, "ruth").line(530, 60, "ruth").line(595, 50, "you");

        cut("power").fog(48)
                .shot(70, 34.0, -3.6, 8.5, 215, 12, 35.0, -3.8, 7.0, 210, 10, 60, 55)
                .shot(110, 42.0, 2.6, 15.5, 90, 4, 34.0, 2.6, 15.5, 90, 4, 64, 60)
                .shot(110, 32.0, 3.4, 19.5, -62, 14, 33.5, 2.9, 20.5, -58, 10, 60, 50)
                .shot(80, 19.5, 2.4, 23.5, 78, 14, 17.8, 2.3, 22.9, 82, 16, 50, 40)
                .line(8, 60, "you").line(118, 60, "you").line(196, 80, "you").line(300, 60, "you");

        cut("tv").fog(28)
                .shot(150, 7.5, 2.3, 24.5, -90, 4, 10.6, 2.2, 24.5, -90, 3, 55, 34)
                .shot(110, 12.2, 2.4, 24.5, 90, 2, 12.4, 2.4, 24.5, 90, 0, 60, 60)
                .shot(80, 12.4, 2.4, 24.5, 90, 0, 12.4, 2.4, 24.5, 90, -4, 60, 44)
                .line(10, 70, "tv").line(86, 70, "tv").line(160, 70, "tv").line(236, 50, "tv").line(292, 44, "you");

        cut("masquerade").fog(34)
                .shot(140, 5.0, 8.6, 15.5, -90, 6, 16.0, 8.6, 15.5, -90, 6, 62, 62)
                .shot(90, 20.0, 8.2, 15.2, -150, -2, 23.0, 8.2, 15.0, -168, -2, 52, 46)
                .shot(100, 26.5, 8.4, 14.6, 0, 6, 26.5, 8.3, 15.2, 0, 4, 46, 30)
                .shot(90, 28.5, 8.2, 14.4, 0, 18, 28.5, 8.0, 15.1, 0, 14, 40, 26)
                .line(10, 80, "you").line(150, 70, "you").line(240, 80, "you").line(340, 70, "you");

        cut("fitting").fog(34)
                .shot(110, 16.0, 14.6, 10.0, 60, 4, 12.0, 14.4, 12.0, 80, 4, 62, 58)
                .shot(100, 5.0, 14.2, 11.5, 150, 0, 9.0, 14.2, 11.0, 172, 0, 54, 50)
                .shot(100, 6.0, 14.4, 15.5, 90, 14, 3.9, 14.0, 15.5, 90, 20, 50, 30)
                .line(10, 80, "you").line(120, 70, "you").line(220, 80, "you");

        cut("supper").fog(40)
                .shot(120, 37.5, 4.2, 20.0, 0, 30, 37.5, 3.6, 21.0, 0, 26, 66, 60)
                .shot(110, 33.0, 2.5, 23.6, -90, 2, 37.0, 2.5, 23.6, -90, 2, 56, 50)
                .shot(110, 39.0, 2.9, 24.5, -90, 4, 40.4, 2.9, 24.5, -90, 2, 50, 36)
                .shot(60, 33.6, 2.4, 25.6, 60, 0, 33.2, 2.4, 25.4, 66, 0, 54, 50)
                .line(10, 90, "you").line(130, 80, "you").line(250, 80, "hatman");

        // ---------------- endings
        cut("end_closed").fog(56)
                .shot(140, 34.0, 2.6, 24.5, -90, 2, 37.5, 2.7, 24.5, -90, 0, 54, 40)
                .shot(140, 37.0, 3.6, 20.2, -30, 20, 36.0, 3.4, 20.6, -40, 18, 60, 56)
                .shot(140, 22.5, 2.4, 27.0, 0, 0, 22.5, 2.2, 29.0, 0, -2, 62, 58)
                .shot(160, 22.5, 3.6, 46.0, 180, -4, 22.5, 4.6, 53.0, 180, -8, 58, 62)
                .line(10, 110, "hatman").line(130, 110, "hatman").line(260, 110, "narr").line(390, 90, "ruth").line(490, 80, "narr");

        cut("end_newface").fog(30)
                .shot(140, 34.5, 2.6, 24.5, -90, 0, 38.0, 2.8, 24.5, -90, -2, 56, 36)
                .shot(140, 26.0, 8.6, 15.2, 30, 10, 27.6, 8.3, 15.2, 16, 8, 50, 30)
                .shot(140, 22.5, 3.0, 44.0, 180, -8, 22.5, 3.4, 50.0, 180, -8, 58, 60)
                .line(10, 110, "hatman").line(140, 110, "narr").line(270, 120, "narr");

        cut("end_worn").fog(30)
                .shot(140, 33.6, 2.5, 24.5, -90, 0, 36.0, 2.6, 24.5, -90, 0, 56, 44)
                .shot(140, 41.4, 2.9, 24.5, 90, 4, 39.5, 2.8, 24.5, 90, 2, 50, 40)
                .shot(140, 22.5, 3.0, 44.0, 180, -8, 22.5, 3.4, 50.0, 180, -8, 58, 60)
                .line(10, 110, "narr").line(140, 110, "hatman").line(270, 120, "narr");

        cut("end_ashes").fog(72)
                .shot(150, 22.5, 3.0, 56.5, 180, -8, 22.5, 4.4, 58.0, 180, -10, 62, 58)
                .shot(130, 30.0, 2.4, 40.0, 160, -18, 28.0, 2.4, 38.0, 158, -22, 54, 44)
                .shot(140, 22.5, 2.6, 58.8, 180, 0, 22.5, 2.6, 58.8, 180, -2, 50, 40)
                .line(10, 120, "narr").line(150, 110, "narr").line(280, 120, "narr");

        cut("end_cold").fog(60)
                .shot(140, 14.0, 2.4, 47.0, 150, -4, 18.0, 2.6, 52.0, 164, -6, 60, 58)
                .shot(140, 22.5, 3.2, 56.5, 180, -8, 22.5, 3.2, 56.5, 180, -12, 56, 40)
                .shot(140, 22.5, 2.6, 58.8, 180, 0, 22.5, 2.6, 58.8, 180, -2, 50, 40)
                .line(10, 110, "narr").line(140, 110, "narr").line(270, 120, "narr");
    }

    private Cutscenes() {}
}
