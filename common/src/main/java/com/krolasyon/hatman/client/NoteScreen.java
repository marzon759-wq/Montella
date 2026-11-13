package com.krolasyon.hatman.client;

import com.krolasyon.hatman.compat.Compat;
import com.krolasyon.hatman.net.Msg;
import com.krolasyon.hatman.net.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class NoteScreen extends HScreen {
    private static ResourceLocation PAPER;
    private final String id;
    private final List<List<FormattedCharSequence>> pages = new ArrayList<>();
    private int page = 0;
    private boolean sent = false;

    public NoteScreen(String id) { this.id = id; }

    @Override
    protected void init() {
        if (PAPER == null) PAPER = Compat.rl("textures/gui/paper.png");
        pages.clear();
        String body = I18n.get("note.hatman." + id + ".body", ClientState.playerName);
        int pw = paperW() - 44;
        int perPage = Math.max(6, (paperH() - 70) / 10);
        List<FormattedCharSequence> cur = new ArrayList<>();
        for (String para : body.split("\n", -1)) {
            List<FormattedCharSequence> ls = para.isEmpty() ? List.of(FormattedCharSequence.EMPTY) : font.split(Component.literal(para), pw);
            for (FormattedCharSequence l : ls) {
                if (cur.size() >= perPage) {
                    pages.add(cur);
                    cur = new ArrayList<>();
                }
                cur.add(l);
            }
        }
        pages.add(cur);
        page = Math.min(page, pages.size() - 1);
    }

    private int paperH() { return Math.min(this.height - 16, 250); }
    private int paperW() { return Math.min(this.width - 20, (int) (paperH() * 0.86f)); }

    @Override
    public boolean isPauseScreen() { return true; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xB0000000);
        int pw = paperW(), ph = paperH();
        int x = (width - pw) / 2, y = (height - ph) / 2;
        g.blit(PAPER, x, y, pw, ph, 0, 0, 256, 320, 256, 320);
        String title = I18n.get("note.hatman." + id + ".title");
        g.drawString(font, title, x + 22, y + 20, 0xFF2A2218, false);
        g.fill(x + 22, y + 31, x + pw - 22, y + 32, 0x802A2218);
        int yy = y + 40;
        for (FormattedCharSequence l : pages.get(page)) {
            g.drawString(font, l, x + 22, yy, 0xFF332A1E, false);
            yy += 10;
        }
        String foot = pages.size() > 1 ? I18n.get("hud.hatman.page", page + 1, pages.size()) : I18n.get("hud.hatman.close");
        g.drawString(font, foot, x + (pw - font.width(foot)) / 2, y + ph - 22, 0xFF6A5A44, false);
    }

    private void next() {
        if (page < pages.size() - 1) {
            page++;
            ClientNet.ui("fx.paper", 1.1f, 0.6f);
        } else onClose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 1 && page > 0) page--;
        else next();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (key == 256 || key == 69) { onClose(); return true; }
        if (key == 32 || key == 257 || key == 262) { next(); return true; }
        if (key == 263 && page > 0) { page--; return true; }
        return super.keyPressed(key, sc, mod);
    }

    @Override
    public void removed() {
        if (!sent) {
            sent = true;
            Net.toServer(Msg.of("note_closed").s("id", id));
        }
    }
}
