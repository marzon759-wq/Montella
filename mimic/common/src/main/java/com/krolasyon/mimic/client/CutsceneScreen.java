package com.krolasyon.mimic.client;

import com.krolasyon.mimic.net.Msg;
import com.krolasyon.mimic.net.Net;
import com.krolasyon.mimic.story.Cutscenes;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;

/** Plays a cutscene: owns the camera, the letterbox and the subtitles. */
public class CutsceneScreen extends HScreen {
    private static Marker cam;
    private static CameraType prevType;
    private final Cutscenes.Cut cut;
    private boolean sent = false;
    private int skipHold = 0;

    public CutsceneScreen(String id) {
        this.cut = Cutscenes.ALL.get(id);
        ClientState.cutId = id;
        ClientState.cutT = 0;
        ClientState.lineKey = null;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (cam == null && mc.level != null) {
            cam = new Marker(EntityType.MARKER, mc.level);
            prevType = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        updateCamera(0);
        if (cam != null) mc.setCameraEntity(cam);
        mc.options.hideGui = true;
    }

    public static void release() {
        Minecraft mc = Minecraft.getInstance();
        if (cam != null) {
            if (mc.player != null) mc.setCameraEntity(mc.player);
            if (prevType != null) mc.options.setCameraType(prevType);
            cam = null;
            mc.options.hideGui = false;
        }
        ClientState.cutId = null;
        ClientState.cutFov = -1;
    }

    private static float ease(float k) { return k * k * (3 - 2 * k); }

    public void updateCamera(float pt) {
        if (cut == null || cam == null) return;
        float t = ClientState.cutT + pt;
        Cutscenes.Shot s = cut.shots.get(cut.shots.size() - 1);
        for (Cutscenes.Shot c : cut.shots) if (t >= c.t0 && t < c.t1) { s = c; break; }
        float k = Mth.clamp((t - s.t0) / (float) (s.t1 - s.t0), 0, 1);
        // long, slow moves: ease only gently so the camera never visibly stops inside a shot
        k = k * 0.6f + ease(k) * 0.4f;
        double x = ClientState.ox + Mth.lerp(k, s.x0, s.x1);
        double y = ClientState.oy + Mth.lerp(k, s.y0, s.y1);
        double z = ClientState.oz + Mth.lerp(k, s.z0, s.z1);
        float sway = (float) Math.sin(t * 0.07) * 0.35f;
        float yaw = s.yaw0 + Mth.wrapDegrees(s.yaw1 - s.yaw0) * k + sway;
        float pitch = Mth.lerp(k, s.pitch0, s.pitch1) + (float) Math.cos(t * 0.05) * 0.25f;
        cam.setPos(x, y, z);
        cam.xo = x; cam.yo = y; cam.zo = z;
        cam.xOld = x; cam.yOld = y; cam.zOld = z;
        cam.setYRot(yaw);
        cam.setXRot(pitch);
        cam.yRotO = yaw;
        cam.xRotO = pitch;
        ClientState.cutFov = Mth.lerp(k, s.fov0, s.fov1);
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCameraEntity() != cam) mc.setCameraEntity(cam);
    }

    @Override
    public void tick() {
        super.tick();
        if (cut == null) { finish(); return; }
        if (ClientState.cutT >= cut.length) finish();
        if (ClientState.cutT > cut.length + 60) {
            release();
            Minecraft.getInstance().setScreen(null);
        }
        long win = Minecraft.getInstance().getWindow().getWindow();
        boolean space = com.mojang.blaze3d.platform.InputConstants.isKeyDown(win, 32);
        if (space && age > 40) {
            if (++skipHold > 24) finish();
        } else skipHold = 0;
    }

    private void finish() {
        if (sent) return;
        sent = true;
        Net.toServer(Msg.of("cut_done").s("id", ClientState.cutId == null ? "" : ClientState.cutId));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int w = this.width, h = this.height;
        float t = ClientState.cutT + pt;
        int bar = (int) (h * 0.115f);
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        Hud.noise(g, w, h, 0.05f);
        if (cut == null) return;
        // fade in / out and a dip to black on every cut between shots
        float fade = 0;
        if (t < 24) fade = 1 - t / 24f;
        if (t > cut.length - 24) fade = Math.max(fade, (t - (cut.length - 24)) / 24f);
        for (Cutscenes.Shot s : cut.shots) {
            if (s.t0 > 0) {
                float d = Math.abs(t - s.t0);
                if (d < 7) fade = Math.max(fade, 1 - d / 7f);
            }
        }
        if (fade > 0) g.fill(0, 0, w, h, Hud.argb(Math.min(1, fade), 0x000000));
        if (cut != null && cut.blackAt((int) t)) g.fill(0, 0, w, h, 0xFF000000);
        if (ClientState.flashT > 0) g.fill(0, 0, w, h, Hud.argb(ClientState.flashT * 0.08f, 0xDDE6FF));
        if (ClientState.jumpT > 0) g.fill(0, 0, w, h, Hud.argb(0.8f, 0x000000));
        for (Cutscenes.Line l : cut.lines) {
            if (t >= l.t && t < l.t + l.dur) {
                float a = Math.min(1f, Math.min((t - l.t) / 8f, (l.t + l.dur - t) / 10f));
                Hud.subtitle(g, font, w, h - bar - 34, l.speaker, I18n.get(l.key, ClientState.playerName), a);
            }
        }
        if (age > 40 && age < 140 || skipHold > 0) {
            String s = I18n.get("hud.mimic.skip");
            g.drawString(font, s, w - font.width(s) - 10, h - bar + 8, 0x80FFFFFF, false);
            if (skipHold > 0) g.fill(w - font.width(s) - 10, h - bar + 19, w - font.width(s) - 10 + (int) (font.width(s) * skipHold / 24f), h - bar + 20, 0xFFB8A26A);
        }
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) { return true; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void removed() {
        // the server ends the cutscene; if the screen is torn down early make sure the camera comes home
        if (sent || ClientState.cutT > 5) {
            finish();
            release();
        }
    }
}
