package com.krolasyon.hatman.client;

import com.krolasyon.hatman.net.Msg;
import com.krolasyon.hatman.net.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public class CreditsScreen extends HScreen {
    private final List<String[]> rows = new ArrayList<>();
    private boolean sent = false;

    public CreditsScreen(String ending, int notes, int notesMax, int caught, int lies) {
        rows.add(new String[]{"big", I18n.get("ending.hatman." + ending)});
        rows.add(new String[]{"", ""});
        for (int i = 0; i < 4; i++) rows.add(new String[]{"", I18n.get("ending.hatman." + ending + "." + i, ClientState.playerName)});
        rows.add(new String[]{"", ""});
        rows.add(new String[]{"dim", I18n.get("credits.hatman.stats", notes, notesMax, caught, lies)});
        rows.add(new String[]{"", ""});
        rows.add(new String[]{"", ""});
        for (int i = 0; i < 9; i++) rows.add(new String[]{i == 0 ? "big" : i % 2 == 1 ? "dim" : "", I18n.get("credits.hatman." + i)});
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xFF000000);
        Hud.noise(g, width, height, 0.05f);
        float scroll = (age + pt) * 0.45f;
        float y = height - scroll + 10;
        for (String[] r : rows) {
            boolean big = r[0].equals("big");
            List<FormattedCharSequence> lines = font.split(Component.literal(r[1]), Math.min(340, width - 60));
            if (lines.isEmpty()) y += 10;
            for (FormattedCharSequence l : lines) {
                if (y > -20 && y < height + 10) {
                    if (big) {
                        g.pose().pushPose();
                        g.pose().translate(width / 2f, y, 0);
                        g.pose().scale(1.6f, 1.6f, 1f);
                        g.drawString(font, l, -font.width(l) / 2, 0, 0xFFC8A050, false);
                        g.pose().popPose();
                    } else {
                        g.drawString(font, l, (width - font.width(l)) / 2, (int) y, r[0].equals("dim") ? 0xFF807868 : 0xFFE0DACA, false);
                    }
                }
                y += big ? 22 : 12;
            }
        }
        if (y < -10 && age > 100) onClose();
        String s = I18n.get("hud.hatman.close");
        if (age > 100) g.drawString(font, s, width - font.width(s) - 8, height - 14, 0x60FFFFFF, false);
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (age > 100 && (key == 256 || key == 257 || key == 32)) { onClose(); return true; }
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void removed() {
        if (!sent) {
            sent = true;
            Net.toServer(Msg.of("credits_done"));
        }
    }
}
