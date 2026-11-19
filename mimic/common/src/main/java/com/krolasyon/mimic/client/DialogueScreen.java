package com.krolasyon.mimic.client;

import com.krolasyon.mimic.entity.MimicEntity;
import com.krolasyon.mimic.net.Msg;
import com.krolasyon.mimic.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

/** A conversation. The world keeps running behind it - and the clock on your answer keeps running too. */
public class DialogueScreen extends HScreen {
    private String node = "", speaker = "", text = "";
    private final List<String> choices = new ArrayList<>();
    private final List<Integer> choiceIdx = new ArrayList<>();
    private int timeLimit = 0, auto = 0;
    private boolean big = false;
    private int shown = 0, nodeAge = 0, choiceAge = 0;
    private boolean answered = false;
    private int hover = -1;
    /** the node the server is waiting on; re-opened if something else closes the screen */
    public static Msg pending = null;

    public static void show(Msg m) {
        Minecraft mc = Minecraft.getInstance();
        pending = m;
        DialogueScreen s = mc.screen instanceof DialogueScreen d ? d : new DialogueScreen();
        s.node = m.s("node");
        s.speaker = m.s("speaker");
        s.text = I18n.get(m.s("key"), ClientState.playerName);
        s.choices.clear();
        s.choiceIdx.clear();
        for (int i = 0; i < m.i("count"); i++) {
            s.choices.add(I18n.get(m.s("c" + i), ClientState.playerName));
            s.choiceIdx.add(m.i("ci" + i));
        }
        s.timeLimit = m.i("time") * 20;
        s.big = m.b("big");
        s.auto = m.i("auto");
        s.shown = 0;
        s.nodeAge = 0;
        s.choiceAge = 0;
        s.answered = false;
        ClientState.dialogue = true;
        mc.options.hideGui = true;
        if (mc.screen != s) mc.setScreen(s);
    }

    private boolean textDone() { return shown >= text.length(); }

    /** for the automated playthrough */
    String nodeId() { return node; }
    boolean ready() { return textDone() && !answered; }
    int choiceCount() { return choices.size(); }
    int textLength() { return text.length(); }

    @Override
    public void tick() {
        super.tick();
        nodeAge++;
        if (!textDone()) {
            shown = Math.min(text.length(), shown + (big ? 6 : 2));
        } else if (!answered) {
            choiceAge++;
            if (!choices.isEmpty() && timeLimit > 0 && choiceAge > timeLimit) send(-1);
            if (choices.isEmpty() && auto <= 0 && choiceAge > 220) send(-2);
        }
        // he wants you to look at him
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && (speaker.equals("mimic"))) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof MimicEntity h && h.distanceTo(mc.player) < 18) {
                    double dx = h.getX() - mc.player.getX(), dz = h.getZ() - mc.player.getZ();
                    double dy = (h.getY() + (h.flag(MimicEntity.F_SIT) ? 2.3 : 3.1)) - mc.player.getEyeY();
                    float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
                    float pitch = (float) -(Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
                    mc.player.setYRot(mc.player.getYRot() + Mth.wrapDegrees(yaw - mc.player.getYRot()) * 0.15f);
                    mc.player.setXRot(mc.player.getXRot() + (pitch - mc.player.getXRot()) * 0.15f);
                    break;
                }
            }
        }
    }

    private void send(int i) {
        if (answered) return;
        answered = true;
        Net.toServer(Msg.of("dlg_choice").s("node", node).i("i", i));
        if (i >= 0) ClientNet.ui("fx.type", 1.0f, 0.8f);
    }

    private int boxY() { return this.height - 98; }

    /** a decision in the moment: the world darkens, two or three big choices, a bar running out from both ends */
    private void renderBig(GuiGraphics g, int mx, int my, float pt) {
        int w = this.width, h = this.height;
        g.fill(0, 0, w, h, 0x50000000);
        int cy = h / 2 + 10;
        g.fill(0, cy - 46, w, cy + 46, 0xB0000000);
        String part = text.substring(0, Math.min(shown, text.length()));
        if (!part.isEmpty()) g.drawCenteredString(font, part, w / 2, cy - 38, 0xFFE0D8C0);
        hover = -1;
        int n = choices.size();
        if (n == 0) return;
        int bw = Math.min(150, (w - 40) / n - 10), gap = 14;
        int total = n * bw + (n - 1) * gap, x0 = (w - total) / 2;
        float pulse = 0.5f + 0.5f * (float) Math.sin((age + pt) * 0.35f);
        for (int i = 0; i < n; i++) {
            int x = x0 + i * (bw + gap), y = cy - 16;
            boolean hv = !answered && mx >= x && mx <= x + bw && my >= y && my <= y + 34;
            if (hv) hover = i;
            int edge = hv ? 0xFFFFFFFF : Hud.argb(0.55f + 0.35f * pulse, 0xC8B070);
            g.fill(x, y, x + bw, y + 34, hv ? 0xC0302010 : 0xA0100C08);
            g.fill(x, y, x + bw, y + 1, edge);
            g.fill(x, y + 33, x + bw, y + 34, edge);
            g.fill(x, y, x + 1, y + 34, edge);
            g.fill(x + bw - 1, y, x + bw, y + 34, edge);
            g.pose().pushPose();
            String label = choices.get(i).toUpperCase();
            float sc = Math.min(1.5f, (bw - 12) / (float) Math.max(1, font.width(label)));
            g.pose().translate(x + bw / 2f, y + 17 - 4 * sc, 0);
            g.pose().scale(sc, sc, 1);
            g.drawCenteredString(font, label, 0, 0, hv ? 0xFFFFFFFF : 0xFFEDE4CC);
            g.pose().popPose();
            g.drawCenteredString(font, "[" + (i + 1) + "]", x + bw / 2, y + 38, 0xFF908870);
        }
        if (timeLimit > 0 && !answered) {
            float k = 1f - Math.min(1f, (choiceAge + pt) / timeLimit);
            int half = (int) (w * 0.4f * k);
            int c = k < 0.3f ? 0xFFD03028 : 0xFFE8DCC0;
            g.fill(w / 2 - half, cy + 36, w / 2 + half, cy + 38, c);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        if (big) {
            renderBig(g, mx, my, pt);
            return;
        }
        int w = this.width, h = this.height;
        int bar = (int) (h * 0.09f * Math.min(1f, age / 12f));
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        int bw = Math.min(420, w - 40), bx = (w - bw) / 2, by = boxY();
        g.fill(bx, by, bx + bw, by + 86, 0xD0080808);
        int col = Hud.speakerColor(speaker);
        g.fill(bx, by, bx + bw, by + 1, 0xFF000000 | col);
        if (!speaker.equals("narr")) g.drawString(font, I18n.get("speaker.mimic." + speaker), bx + 10, by + 7, 0xFF000000 | col, false);
        String part = text.substring(0, Math.min(shown, text.length()));
        List<FormattedCharSequence> lines = font.split(Component.literal(part), bw - 20);
        int y = by + 20;
        // an angry host: the letters will not sit still
        float jit = speaker.equals("lily_q") || speaker.equals("it") ? 0.6f : 0;
        for (FormattedCharSequence l : lines) {
            int jx = jit > 0 ? (int) ((Math.random() - 0.5) * 2.4 * jit) : 0;
            g.drawString(font, l, bx + 10 + jx, y, speaker.equals("narr") ? 0xFFB0B0A8 : 0xFFF0ECE0, false);
            y += 10;
        }
        hover = -1;
        if (textDone() && !answered) {
            if (choices.isEmpty()) {
                if (auto <= 0 && (age / 8) % 2 == 0) g.drawString(font, "▼", bx + bw - 16, by + 74, 0xFFB8A26A, false);
            } else {
                int cy = by + 86 + 4;
                int total = choices.size() * 14 + 6;
                cy = Math.min(cy, h - total - 4);
                int top = by - total - 6;
                // choices go above the box so the answer timer stays visible under them
                cy = top;
                g.fill(bx, cy - 3, bx + bw, cy + total - 3, 0xD0080808);
                for (int i = 0; i < choices.size(); i++) {
                    int yy = cy + i * 14;
                    boolean hv = mx >= bx && mx <= bx + bw && my >= yy - 2 && my < yy + 12;
                    if (hv) {
                        hover = i;
                        g.fill(bx + 2, yy - 2, bx + bw - 2, yy + 11, 0x40B8A26A);
                    }
                    String s = (i + 1) + ".  " + choices.get(i);
                    g.drawString(font, font.plainSubstrByWidth(s, bw - 16), bx + 8, yy, hv ? 0xFFFFFFFF : 0xFFD8D0B8, false);
                }
                if (timeLimit > 0) {
                    float k = 1f - Math.min(1f, (choiceAge + pt) / timeLimit);
                    int c = k < 0.3f ? 0xFFC03028 : 0xFFB8A26A;
                    g.fill(bx, by + 84, bx + (int) (bw * k), by + 86, c);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!textDone()) { shown = text.length(); if (!big) return true; }
        if (choices.isEmpty()) { if (auto <= 0) send(-2); }
        else if (hover >= 0) send(choiceIdx.get(hover));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (key == 256) return true; // no walking away from this conversation
        if (!textDone()) {
            if (key == 32 || key == 257) shown = text.length();
            if (!big) return true;
            shown = text.length();
        }
        if (choices.isEmpty()) {
            if ((key == 32 || key == 257) && auto <= 0) send(-2);
        } else if (key >= 49 && key < 49 + choices.size()) {
            send(choiceIdx.get(key - 49));
        }
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

}
