package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.compat.Compat;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The Blood Sea's own title screen: a red sea under a white sun that sometimes opens, a raft far out on the swell, the
 * dripping title, a short menu written like a ship's log - and in the corner, the app it is brought to you by.
 */
public class TitleMenu extends TitleScreen {
    public static final String APP_URL = "https://play.google.com/store/apps/details?id=com.kyvix.photoeditor&hl=tr";
    static final ResourceLocation LOGO = Compat.rl("textures/gui/menu_logo.png");
    static final ResourceLocation APP = Compat.rl("textures/gui/menu_app.png");
    static final ResourceLocation SUN = Compat.rl("textures/environment/white_sun.png");
    static final ResourceLocation EYE = Compat.rl("textures/environment/watcher_eye.png");
    static final ResourceLocation PUPIL = Compat.rl("textures/environment/watcher_pupil.png");

    private final List<Entry> entries = new ArrayList<>();
    private Entry app;
    private int ticks;
    private float eye, eyeO;
    private int eyeHold, nextOpen = 140;
    private final float[] ash = new float[120];

    public TitleMenu() {
        super();
        for (int i = 0; i < ash.length; i++) ash[i] = (float) Math.random();
    }

    /** a menu line: no box, just words; a blood rule runs out under the one you point at */
    final class Entry extends AbstractButton {
        final Runnable action;
        final boolean small;
        float hover;

        Entry(int x, int y, int w, int h, Component label, Runnable action, boolean small) {
            super(x, y, w, h, label);
            this.action = action;
            this.small = small;
        }

        @Override public void onPress() { action.run(); }

        @Override
        protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            boolean on = isHoveredOrFocused();
            hover = Mth.lerp(0.25f, hover, on ? 1f : 0f);
            int col = on ? 0xFFF2E6D8 : 0xFFB8A89A;
            if (small) {
                g.drawString(font, getMessage(), getX(), getY() + (height - 8) / 2, col, true);
                return;
            }
            String txt = getMessage().getString();
            int tx = getX() + (int) (10 * hover);
            g.pose().pushPose();
            g.pose().translate(tx, getY() + 2, 0);
            g.pose().scale(1.5f, 1.5f, 1f);
            g.drawString(font, txt, 0, 0, col, true);
            g.pose().popPose();
            // the marker, and the rule that runs out like a cut
            if (hover > 0.02f) {
                int a = (int) (hover * 255) << 24;
                g.drawString(font, "›", getX() - 4, getY() + 3, a | 0xB01818, false);
                int len = (int) ((font.width(txt) * 1.5f + 6) * hover);
                g.fill(tx, getY() + height - 3, tx + len, getY() + height - 2, a | 0x8A0A0A);
                g.fill(tx + len - 2, getY() + height - 2, tx + len - 1, getY() + height + (int) (3 * hover), a | 0x8A0A0A);
            }
        }

        @Override protected void updateWidgetNarration(NarrationElementOutput out) { defaultButtonNarrationText(out); }
    }

    @Override
    protected void init() {
        entries.clear();
        clearWidgets();
        Minecraft mc = Minecraft.getInstance();
        int x = Math.max(28, width / 12), y = height / 2 - 6, step = 22;
        add(x, y, "menu.bloodsea.start", () -> mc.setScreen(new net.minecraft.client.gui.screens.worldselection.SelectWorldScreen(this)));
        add(x, y + step, "menu.bloodsea.multiplayer", () -> mc.setScreen(new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(this)));
        add(x, y + step * 2, "menu.bloodsea.settings", () -> mc.setScreen(com.krolasyon.bloodsea.compat.ClientCompat.options(this)));
        add(x, y + step * 3, "menu.bloodsea.quit", mc::stop);
        // the app this is brought to you by: straight to its store page
        Component by = Component.literal(I18n.get("menu.bloodsea.by"));
        int bw = font.width(by) + 30;
        app = new Entry(width - bw - 8, height - 26, bw, 20, by, () -> Util.getPlatform().openUri(APP_URL), true);
        entries.add(app);
        addWidget(app);
    }

    private void add(int x, int y, String key, Runnable r) {
        Component label = Component.translatable(key);
        Entry e = new Entry(x, y, (int) (font.width(label) * 1.5f) + 24, 18, label, r, false);
        entries.add(e);
        addWidget(e);
    }

    @Override
    public void tick() {
        ticks++;
        eyeO = eye;
        // the sun opens, now and then, and looks at the menu
        if (eyeHold > 0) eyeHold--;
        else if (--nextOpen <= 0) { eyeHold = 70 + (int) (Math.random() * 60); nextOpen = 260 + (int) (Math.random() * 300); }
        eye = Mth.approach(eye, eyeHold > 0 ? 1f : 0f, eyeHold > 0 ? 0.05f : 0.07f);
        for (int i = 0; i < ash.length; i++) { ash[i] += 0.0016f + (i % 7) * 0.0004f; if (ash[i] > 1) ash[i] -= 1; }
        MusicClient.menu(true);
    }

    @Override
    public void removed() {
        MusicClient.menu(false);
        super.removed();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float t = ticks + pt;
        int w = width, h = height;
        int horizon = (int) (h * 0.58f);
        // sky: dark at the top, a sick red glow along the horizon
        g.fillGradient(0, 0, w, horizon, 0xFF0C0103, 0xFF5A0A0C);
        // the white sun, the size of the eye it really is
        int sr = Math.max(24, (int) (h / 7.5f));
        int sx = (int) (w * 0.75f), sy = (int) (h * 0.3f);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 0.18f);
        g.blit(SUN, sx - sr * 2, sy - sr * 2, sr * 4, sr * 4, 0, 0, 512, 512, 512, 512);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.blit(SUN, sx - sr, sy - sr, sr * 2, sr * 2, 0, 0, 512, 512, 512, 512);
        float e = Mth.lerp(pt, eyeO, eye);
        float o = e * e * (3 - 2 * e);
        if (o > 0.01f) {
            int bh = Math.max(1, (int) (sr * o));
            int v0 = (int) (256 - 256 * o);
            g.blit(EYE, sx - sr, sy - bh, sr * 2, bh * 2, 0, v0, 512, (int) (512 * o), 512, 512);
            int pw = (int) (sr * 0.42f), ph = Math.min((int) (sr * 0.62f), (int) (bh * 0.92f));
            int look = (int) (Math.sin(t * 0.03) * sr * 0.12);
            if (ph > 1) g.blit(PUPIL, sx - pw / 2 + look, sy - ph, pw, ph * 2, 0, 0, 256, 256, 256, 256);
            int cw = (int) Math.sqrt(Math.max(0, sr * sr - bh * bh));
            g.fill(sx - cw, sy - bh, sx + cw, sy - bh + 1, 0xC0300006);
            g.fill(sx - cw, sy + bh - 1, sx + cw, sy + bh, 0xC0300006);
        }
        // the sea: swells in layers, darker as they come near; a road of light under the sun
        for (int layer = 0; layer < 5; layer++) {
            float k = layer / 4f;
            int base = horizon + (int) (k * k * (h - horizon) * 0.85f);
            int amp = 1 + (int) (k * 6);
            int col = 0xFF000000 | (int) Mth.lerp(k, 0x6A, 0x22) << 16 | (int) Mth.lerp(k, 0x0C, 0x02) << 8 | (int) Mth.lerp(k, 0x0E, 0x04);
            for (int x = 0; x < w; x += 2) {
                double yy = base + Math.sin(x * (0.035 - k * 0.02) + t * (0.03 + k * 0.03) + layer) * amp
                        + Math.sin(x * 0.011 + t * 0.017 + layer * 2) * amp * 0.7;
                g.fill(x, (int) yy, x + 2, h, col);
                // a crest
                g.fill(x, (int) yy, x + 2, (int) yy + 1, (col & 0x00FFFFFF) | 0xFF000000 | 0x00180404);
            }
        }
        for (int i = 0; i < 26; i++) {
            float k = i / 26f;
            int yy = horizon + 2 + (int) (k * k * (h - horizon) * 0.8f);
            int half = (int) (sr * (0.25f + k * 0.9f) * (0.75f + 0.25f * Math.sin(t * 0.07 + i)));
            g.fill(sx - half, yy, sx + half, yy + 1, ((int) (70 * (1 - k)) << 24) | 0xFFD8C0);
        }
        // a raft far out, rising and falling, a man standing on it
        int rx = (int) (w * 0.36f), ry = horizon + (int) (h * 0.06f) + (int) (Math.sin(t * 0.05) * 2);
        int rw = Math.max(18, w / 28);
        g.fill(rx - rw / 2, ry, rx + rw / 2, ry + 3, 0xFF140406);
        g.fill(rx - 1, ry - rw, rx, ry, 0xFF140406);
        g.fill(rx, ry - rw + 1, rx + rw / 3, ry - rw / 2, 0xFF2A1A16);
        g.fill(rx + rw / 4, ry - 7, rx + rw / 4 + 2, ry, 0xFF0E0204);
        // ash falling
        for (int i = 0; i < ash.length; i++) {
            float fx = (i * 0.618034f) % 1f;
            int ax = (int) ((fx * w + Math.sin(t * 0.02 + i) * 12) % w);
            int ay = (int) (ash[i] * h);
            g.fill(ax, ay, ax + 1, ay + 1, 0x60D8B8A8);
        }
        // a vignette
        g.fillGradient(0, 0, w, h / 4, 0xA0000000, 0x00000000);
        g.fillGradient(0, h - h / 4, w, h, 0x00000000, 0xC0000000);
        for (int i = 0; i < 24; i++) {
            int x0 = i * w / 48, x1 = (i + 1) * w / 48;
            g.fill(x0, 0, x1, h, ((int) (0x58 * (1 - i / 24f)) << 24));
        }
        // the title, a little unsteady
        int lw = Math.min(w - 40, (int) (w * 0.52f)), lh = lw * 300 / 1024;
        int lx = Math.max(20, w / 14), ly = (int) (h * 0.12f) + (int) (Math.sin(t * 0.04) * 1.5);
        g.blit(LOGO, lx, ly, lw, lh, 0, 0, 1024, 300, 1024, 300);
        g.drawString(font, I18n.get("menu.bloodsea.tagline"), lx + 8, ly + lh - 4, 0xFF9A7A6A, true);
        // the menu
        for (Entry en : entries) en.render(g, mx, my, pt);
        // the app badge in the corner
        int bx = app.getX(), by = app.getY();
        g.blit(APP, bx + app.getWidth() - 20, by + 1, 18, 18, 0, 0, 128, 128, 128, 128);
        if (app.isHoveredOrFocused()) g.fill(bx, by + 19, bx + app.getWidth() - 24, by + 20, 0xFFB01818);
        String v = "Blood Sea 1.0";
        g.drawString(font, v, 6, h - 12, 0x806A5A50, false);
        RenderSystem.disableBlend();
    }

    @Override public boolean isPauseScreen() { return false; }

    /** testing: open the eye now */
    void forceEye() { eyeHold = 120; }

    /** every client tick: if the game's own title screen got through by some other way, swap it */
    public static void check() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null && mc.screen.getClass() == TitleScreen.class && mc.getOverlay() == null) mc.setScreen(new TitleMenu());
    }

    /** shown in place of the game's own title screen */
    public static Screen replace(Screen s) {
        return s != null && s.getClass() == TitleScreen.class ? new TitleMenu() : s;
    }
}
