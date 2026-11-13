package com.krolasyon.hatman.client;

import com.krolasyon.hatman.net.Msg;
import com.krolasyon.hatman.net.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;

/** The dial of Marlowe's safe. */
public class KeypadScreen extends HScreen {
    private String code = "";

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, width, height, 0xA0000000);
        int x = width / 2 - 62, y = height / 2 - 76;
        g.fill(x, y, x + 124, y + 152, 0xFF16181A);
        g.fill(x + 2, y + 2, x + 122, y + 150, 0xFF24282C);
        String title = I18n.get("hud.hatman.safe");
        g.drawString(font, title, width / 2 - font.width(title) / 2, y + 8, 0xFFB8A26A, false);
        g.fill(x + 14, y + 22, x + 110, y + 40, 0xFF050605);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(i < code.length() ? code.charAt(i) : '_').append(i < 3 ? " " : "");
        g.drawString(font, sb.toString(), width / 2 - font.width(sb.toString()) / 2, y + 27, 0xFF60FF70, false);
        for (int i = 0; i < 12; i++) {
            int bx = x + 14 + (i % 3) * 33, by = y + 48 + (i / 3) * 24;
            boolean hv = mx >= bx && mx < bx + 30 && my >= by && my < by + 21;
            g.fill(bx, by, bx + 30, by + 21, hv ? 0xFF4A5056 : 0xFF34383C);
            String s = label(i);
            g.drawString(font, s, bx + 15 - font.width(s) / 2, by + 7, 0xFFE0E0D8, false);
        }
    }

    private static String label(int i) {
        return i < 9 ? "" + (i + 1) : i == 9 ? "C" : i == 10 ? "0" : "OK";
    }

    private void press(String s) {
        if (s.equals("C")) code = "";
        else if (s.equals("OK")) submit();
        else if (code.length() < 4) {
            code += s;
            ClientNet.ui("fx.keypad", 1f, 0.7f);
        }
    }

    private void submit() {
        if (code.length() == 4) {
            Net.toServer(Msg.of("keypad").s("code", code));
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = width / 2 - 62, y = height / 2 - 76;
        for (int i = 0; i < 12; i++) {
            int bx = x + 14 + (i % 3) * 33, by = y + 48 + (i / 3) * 24;
            if (mx >= bx && mx < bx + 30 && my >= by && my < by + 21) press(label(i));
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (key >= 48 && key <= 57) { press("" + (key - 48)); return true; }
        if (key >= 320 && key <= 329) { press("" + (key - 320)); return true; }
        if (key == 259) { if (!code.isEmpty()) code = code.substring(0, code.length() - 1); return true; }
        if (key == 257 || key == 335) { submit(); return true; }
        return super.keyPressed(key, sc, mod);
    }
}
