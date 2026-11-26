package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.net.Msg;
import com.krolasyon.bloodsea.net.Net;
import com.krolasyon.bloodsea.story.Cut;
import com.krolasyon.bloodsea.story.Cuts;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;

/** Plays a cutscene: owns the camera, draws the letterbox, the subtitles, the fades; speaks the lines. */
public class CutScreen extends com.krolasyon.bloodsea.compat.CScreen {
    private static Marker cam;
    public static float fov = -1;
    /** for recording trailers: no letterbox, no subtitles, no hint */
    public static boolean clean;
    private final Cut cut;
    private final double ox, oy, oz;
    private final float yaw;
    private int t, skipHold;
    private boolean sent, onRaft;
    /** what stands on the raft's deck, in the cut's local frame (x0, x1, y0, y1, z0, z1): the camera keeps out of it */
    private static final double[][] PROPS = {
            {-0.2, 0.2, -0.5, 4.2, 1.8, 2.2}, {-1.7, 1.7, 4.2, 4.5, 1.85, 2.15}, {-1.5, 1.5, 2.6, 3.9, 1.8, 2.3},
            {-2.05, -1.8, -0.5, 2.4, 1.8, 2.1}, {-1.8, -1.3, 1.4, 2.3, 1.7, 2.2},
            {1.2, 2.2, -0.5, 1.15, 1.2, 2.2}, {1.2, 2.2, -0.5, 1.15, 0.33, 1.3}, {0.35, 1.15, -0.5, 0.8, 1.45, 2.3}, {-0.25, 0.25, -0.5, 1.0, -5, -1.15}};

    public CutScreen(String id, double ox, double oy, double oz, float yaw) {
        this.cut = Cuts.ALL.get(id);
        this.ox = ox; this.oy = oy; this.oz = oz; this.yaw = yaw;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (cam == null && mc.level != null) cam = new Marker(EntityType.MARKER, mc.level);
        onRaft = mc.level != null && !mc.level.getEntitiesOfClass(com.krolasyon.bloodsea.entity.RaftEntity.class,
                new net.minecraft.world.phys.AABB(ox - 2, oy - 3, oz - 2, ox + 2, oy + 3, oz + 2)).isEmpty();
        SeaClient.cinematic = true;
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.options.hideGui = true;
        camera(0);
    }

    Vec3 at(double[] l) {
        double a = Math.toRadians(yaw), c = Math.cos(a), s = Math.sin(a);
        return new Vec3(ox + l[0] * c - l[2] * s, oy + l[1], oz + l[0] * s + l[2] * c);
    }

    private static boolean inProp(double x, double y, double z, double pad) {
        for (double[] b : PROPS)
            if (x > b[0] - pad && x < b[1] + pad && y > b[2] - pad && y < b[3] + pad && z > b[4] - pad && z < b[5] + pad) return true;
        return false;
    }

    /** like a third-person spring arm: if a mast, barrel or crate stands between the subject and the lens, the lens comes in front of it */
    static double[] clear(double[] c, double[] t) {
        double dx = c[0] - t[0], dy = c[1] - t[1], dz = c[2] - t[2];
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double[] out = c.clone();
        // wide shots of the whole raft keep their distance; only the close work needs the arm
        if (d > 0.5 && d < 8) {
            int n = (int) (d / 0.04);
            for (int i = 1; i <= n; i++) {
                double f = i / (double) n;
                if (d * f < 0.4) continue;
                if (inProp(t[0] + dx * f, t[1] + dy * f, t[2] + dz * f, 0.12)) {
                    double g = Math.max(0.6 / d, f - 0.1 / d);
                    out = new double[]{t[0] + dx * g, t[1] + dy * g, t[2] + dz * g};
                    break;
                }
            }
        }
        // never under the logs
        if (Math.abs(out[0]) < 2.6 && Math.abs(out[2]) < 2.6 && out[1] < 0.15) out[1] = 0.15;
        return out;
    }

    public void camera(float pt) {
        if (cut == null || cam == null) return;
        float tt = t + pt;
        Cut.Shot s = cut.shotAt(tt);
        if (s == null) return;
        float k = Mth.clamp((tt - s.t0) / (float) Math.max(1, s.t1 - s.t0), 0, 1);
        k = k * 0.55f + (k * k * (3 - 2 * k)) * 0.45f;
        double[] pl = new double[3], ll = new double[3];
        for (int i = 0; i < 3; i++) { pl[i] = Mth.lerp(k, s.p0[i], s.p1[i]); ll[i] = Mth.lerp(k, s.l0[i], s.l1[i]); }
        if (onRaft) pl = clear(pl, ll);
        Vec3 p = at(pl), l = at(ll);
        // a hand-held breath in the camera
        p = p.add(Math.sin(tt * 0.05) * 0.02, Math.sin(tt * 0.07) * 0.015, 0);
        Vec3 d = l.subtract(p);
        float yw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
        float pitch = (float) -(Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        cam.setPos(p.x, p.y, p.z);
        cam.xo = p.x; cam.yo = p.y; cam.zo = p.z;
        cam.xOld = p.x; cam.yOld = p.y; cam.zOld = p.z;
        cam.setYRot(yw); cam.yRotO = yw;
        cam.setXRot(pitch); cam.xRotO = pitch;
        fov = Mth.lerp(k, s.fov0, s.fov1);
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() != cam) mc.setCameraEntity(cam);
    }

    @Override
    public void tick() {
        if (cut == null) { finish(); return; }
        if (!clean) for (Cut.Say l : cut.lines) {
            int len = com.krolasyon.bloodsea.story.Lines.ticks(l.key());
            if (t + 1 >= l.t() && t < l.t() + len) {
                String txt = I18n.get("line.bloodsea." + l.key());
                StoryHud.murmur(com.krolasyon.bloodsea.story.Lines.ALL.get(l.key()).speaker(), txt,
                        t < l.t() ? 0 : StoryHud.typed(txt, len, t - l.t()), StoryHud.typed(txt, len, t + 1 - l.t()));
            }
        }
        t++;
        if (t >= cut.length) finish();
        long win = Minecraft.getInstance().getWindow().getWindow();
        if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(win, 32) && t > 40) {
            if (++skipHold > 24) finish();
        } else skipHold = 0;
    }

    private void finish() {
        if (sent) return;
        sent = true;
        Net.toServer(Msg.of("cut_done"));
    }

    public static void release() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.setCameraEntity(mc.player);
        cam = null;
        fov = -1;
        SeaClient.cinematic = false;
        mc.options.hideGui = false;
        if (mc.screen instanceof CutScreen) mc.setScreen(null);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        camera(pt);
        int w = width, h = height;
        float tt = t + pt;
        int bar = clean ? 0 : (int) (h * 0.12f);
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        if (cut == null) return;
        float fade = 0;
        if (tt < 20) fade = 1 - tt / 20f;
        if (tt > cut.length - 20) fade = Math.max(fade, (tt - (cut.length - 20)) / 20f);
        for (Cut.Shot s : cut.shots) if (s.t0 > 0) {
            float d = Math.abs(tt - s.t0);
            if (d < 5) fade = Math.max(fade, 0.7f * (1 - d / 5f));
        }
        if (cut.blackFrom >= 0 && tt >= cut.blackFrom && tt < cut.blackTo) {
            float edge = Math.min(1f, Math.min((tt - cut.blackFrom) / 6f + 1, (cut.blackTo - tt) / 14f));
            fade = Math.max(fade, edge);
        }
        if (fade > 0) g.fill(0, 0, w, h, StoryHud.argb(Math.min(1, fade), 0x000000));
        if (cut.whiteFrom >= 0 && tt > cut.whiteFrom) g.fill(0, 0, w, h, StoryHud.argb(Math.min(1f, (tt - cut.whiteFrom) / 50f), 0xFFFFF4));
        if (!clean) for (Cut.Say l : cut.lines) {
            int len = com.krolasyon.bloodsea.story.Lines.ticks(l.key());
            if (tt >= l.t() && tt < l.t() + len) {
                float a = Math.min(1f, Math.min((tt - l.t()) / 6f, (l.t() + len - tt) / 8f));
                String txt = I18n.get("line.bloodsea." + l.key());
                StoryHud.subtitle(g, font, w, h - bar - 36, com.krolasyon.bloodsea.story.Lines.ALL.get(l.key()).speaker(), txt, a, StoryHud.typed(txt, len, tt - l.t()));
            }
        }
        if (!clean && (t > 40 && t < 160 || skipHold > 0)) {
            String s = I18n.get("hud.bloodsea.skip");
            g.drawString(font, s, w - font.width(s) - 10, h - bar + 8, 0x80FFFFFF, false);
            if (skipHold > 0) g.fill(w - font.width(s) - 10, h - bar + 19, w - font.width(s) - 10 + (int) (font.width(s) * skipHold / 24f), h - bar + 20, 0xFFB02020);
        }
    }

    @Override public boolean keyPressed(int key, int sc, int mod) { return true; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}
