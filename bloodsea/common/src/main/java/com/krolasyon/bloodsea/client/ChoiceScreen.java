package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.net.Msg;
import com.krolasyon.bloodsea.net.Net;
import com.krolasyon.bloodsea.story.Lines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/** A choice with a clock on it. Say nothing and the first answer is taken for you. */
public class ChoiceScreen extends com.krolasyon.bloodsea.compat.CScreen {
    private final String id;
    private final String[] opts;
    private final int secs;
    private int t, hover = -1;
    private boolean sent;

    public ChoiceScreen(String id, int secs) {
        this.id = id;
        this.secs = secs;
        this.opts = Lines.CHOICES.getOrDefault(id, new String[0]);
    }

    @Override
    public void tick() { if (++t > secs * 20) pick(0); }

    public void pick(int i) {
        if (sent || opts.length == 0) return;
        sent = true;
        Net.toServer(Msg.of("pick").s("id", id).s("pick", opts[Math.max(0, Math.min(opts.length - 1, i))]));
        onClose();
    }

    int rowY(int i) { return height / 2 - (opts.length * 24) / 2 + i * 24 + 20; }

    @Override
    public boolean mouseClicked(double mx, double my, int b) {
        for (int i = 0; i < opts.length; i++) if (Math.abs(my - (rowY(i) + 6)) < 11 && Math.abs(mx - width / 2.0) < 150) { pick(i); return true; }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (key >= 49 && key < 49 + opts.length) { pick(key - 49); return true; }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int w = width, h = height;
        g.fill(0, 0, w, h, 0x99000000);
        float a = Math.min(1f, (t + pt) / 10f);
        g.drawCenteredString(font, I18n.get("choiceq.bloodsea." + id), w / 2, rowY(0) - 30, StoryHud.argb(a, 0xE8DCC8));
        hover = -1;
        for (int i = 0; i < opts.length; i++) {
            int y = rowY(i);
            boolean on = Math.abs(my - (y + 6)) < 11 && Math.abs(mx - w / 2) < 150;
            if (on) hover = i;
            g.fill(w / 2 - 150, y - 4, w / 2 + 150, y + 16, StoryHud.argb(a * (on ? 0.7f : 0.4f), on ? 0x5A0808 : 0x000000));
            g.drawCenteredString(font, (i + 1) + "   " + I18n.get("choice.bloodsea." + id + "." + opts[i]), w / 2, y + 2, StoryHud.argb(a, on ? 0xFFFFFF : 0xD8D0C0));
        }
        float left = 1 - (t + pt) / (secs * 20f);
        int bw = (int) (300 * Math.max(0, left));
        g.fill(w / 2 - 150, rowY(opts.length) + 4, w / 2 - 150 + bw, rowY(opts.length) + 6, StoryHud.argb(a, 0xB02020));
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public boolean isPauseScreen() { return false; }
}
