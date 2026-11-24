package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.net.Msg;
import com.krolasyon.bloodsea.net.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/** The end of a voyage: the screen goes to blood and then to black, and the lines come one at a time. */
public class EndingScreen extends Screen {
    private final String id;
    private final int seen;
    private int t = 0;
    private boolean sent = false;
    private static final int LINES = 4, PER = 90, LEN = 60 + LINES * PER + 140;

    public EndingScreen(String id, int seen) {
        super(Component.literal(""));
        this.id = id;
        this.seen = seen;
    }

    @Override
    public void tick() {
        if (++t > LEN) done();
    }

    private void done() {
        if (sent) return;
        sent = true;
        Net.toServer(Msg.of("ending_done"));
        onClose();
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (key == 256 && t > 120) done();
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float tt = t + pt;
        int w = width, h = height;
        // blood first, then black
        float red = Math.max(0, 1 - tt / 50f);
        g.fill(0, 0, w, h, 0xFF000000);
        if (red > 0) g.fill(0, 0, w, h, ((int) (red * 255) << 24) | 0x6A0000);
        float ta = clamp((tt - 30) / 30f);
        String title = I18n.get("ending.bloodsea." + id + ".title");
        g.pose().pushPose();
        g.pose().translate(w / 2f, h * 0.22f, 0);
        g.pose().scale(2.6f, 2.6f, 1f);
        g.drawCenteredString(font, title, 0, 0, argb(ta, 0xE8DCC8));
        g.pose().popPose();
        String label = I18n.get("ending.bloodsea.label", seen, 4);
        g.drawCenteredString(font, label, w / 2, (int) (h * 0.22f) + 30, argb(ta * 0.6f, 0xB02020));
        for (int i = 0; i < LINES; i++) {
            float a = clamp((tt - 60 - i * PER) / 30f);
            if (a <= 0) continue;
            g.drawCenteredString(font, I18n.get("ending.bloodsea." + id + "." + i), w / 2, (int) (h * 0.42f) + i * 18, argb(a, 0xD8D0C0));
        }
        if (t > 120) g.drawString(font, I18n.get("ending.bloodsea.skip"), w - font.width(I18n.get("ending.bloodsea.skip")) - 8, h - 14, 0x60FFFFFF, false);
        float out = clamp((tt - (LEN - 40)) / 40f);
        if (out > 0) g.fill(0, 0, w, h, ((int) (out * 255) << 24));
    }

    private static float clamp(float v) { return Math.max(0, Math.min(1, v)); }

    private static int argb(float a, int rgb) { return ((int) (Math.max(0.02f, a) * 255) << 24) | rgb; }
}
