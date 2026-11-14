package com.krolasyon.hatman.client;

import com.krolasyon.hatman.entity.HatmanEntity;
import com.krolasyon.hatman.net.Msg;
import com.krolasyon.hatman.net.Net;
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
    private int timeLimit = 0;
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
        s.shown = 0;
        s.nodeAge = 0;
        s.choiceAge = 0;
        s.answered = false;
        ClientState.dialogue = true;
        mc.options.hideGui = true;
        ClientState.anger = m.f("anger");
        if (mc.screen != s) mc.setScreen(s);
    }

    private boolean textDone() { return shown >= text.length(); }

    @Override
    public void tick() {
        super.tick();
        nodeAge++;
        if (!textDone()) {
            int before = shown;
            shown = Math.min(text.length(), shown + 2);
            if (shown / 4 != before / 4 && !speaker.equals("narr")) {
                boolean ruth = speaker.equals("ruth");
                float pitch = ruth ? 0.95f + (float) Math.random() * 0.2f : 0.55f + (float) Math.random() * 0.14f + ClientState.anger * 0.002f;
                ClientNet.ui(ruth ? "fx.voice_ruth" : "hm.voice", pitch, 0.5f);
            }
        } else if (!answered) {
            choiceAge++;
            if (!choices.isEmpty() && timeLimit > 0 && choiceAge > timeLimit) send(-1);
            if (choices.isEmpty() && choiceAge > 200) send(-2);
        }
        // he wants you to look at him
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null && (speaker.equals("hatman"))) {
            for (Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof HatmanEntity h && h.distanceTo(mc.player) < 18) {
                    double dx = h.getX() - mc.player.getX(), dz = h.getZ() - mc.player.getZ();
                    double dy = (h.getY() + (h.flag(HatmanEntity.F_SIT) ? 2.3 : 3.1)) - mc.player.getEyeY();
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

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        int w = this.width, h = this.height;
        int bar = (int) (h * 0.09f * Math.min(1f, age / 12f));
        g.fill(0, 0, w, bar, 0xFF000000);
        g.fill(0, h - bar, w, h, 0xFF000000);
        int bw = Math.min(420, w - 40), bx = (w - bw) / 2, by = boxY();
        g.fill(bx, by, bx + bw, by + 86, 0xD0080808);
        int col = Hud.speakerColor(speaker);
        g.fill(bx, by, bx + bw, by + 1, 0xFF000000 | col);
        if (!speaker.equals("narr")) g.drawString(font, I18n.get("speaker.hatman." + speaker), bx + 10, by + 7, 0xFF000000 | col, false);
        String part = text.substring(0, Math.min(shown, text.length()));
        List<FormattedCharSequence> lines = font.split(Component.literal(part), bw - 20);
        int y = by + 20;
        // an angry host: the letters will not sit still
        float jit = speaker.equals("hatman") || speaker.equals("voice") ? Math.max(0, (ClientState.anger - 30) / 70f) : 0;
        for (FormattedCharSequence l : lines) {
            int jx = jit > 0 ? (int) ((Math.random() - 0.5) * 2.4 * jit) : 0;
            g.drawString(font, l, bx + 10 + jx, y, speaker.equals("narr") ? 0xFFB0B0A8 : 0xFFF0ECE0, false);
            y += 10;
        }
        hover = -1;
        if (textDone() && !answered) {
            if (choices.isEmpty()) {
                if ((age / 8) % 2 == 0) g.drawString(font, "▼", bx + bw - 16, by + 74, 0xFFB8A26A, false);
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
        if (!textDone()) { shown = text.length(); return true; }
        if (choices.isEmpty()) send(-2);
        else if (hover >= 0) send(choiceIdx.get(hover));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (key == 256) return true; // no walking away from this conversation
        if (!textDone()) {
            if (key == 32 || key == 257) shown = text.length();
            return true;
        }
        if (choices.isEmpty()) {
            if (key == 32 || key == 257) send(-2);
        } else if (key >= 49 && key < 49 + choices.size()) {
            send(choiceIdx.get(key - 49));
        }
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

}
