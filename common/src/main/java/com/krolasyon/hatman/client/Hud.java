package com.krolasyon.hatman.client;

import com.krolasyon.hatman.compat.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** Everything drawn over the world while the story runs. */
public final class Hud {
    private static ResourceLocation[] NOISE;
    private static ResourceLocation VIGNETTE, JUMP, SLATS;

    private Hud() {}

    private static void tex() {
        if (NOISE != null) return;
        NOISE = new ResourceLocation[4];
        for (int i = 0; i < 4; i++) NOISE[i] = Compat.rl("textures/gui/noise_" + i + ".png");
        VIGNETTE = Compat.rl("textures/gui/vignette.png");
        JUMP = Compat.rl("textures/gui/jumpscare.png");
        SLATS = Compat.rl("textures/gui/slats.png");
    }

    static int argb(float a, int rgb) {
        return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    static void noise(GuiGraphics g, int w, int h, float alpha) {
        tex();
        int f = (ClientState.ticks / 2) & 3;
        int ox = (ClientState.ticks * 37) % 64, oy = (ClientState.ticks * 53) % 64;
        g.setColor(1f, 1f, 1f, alpha);
        for (int x = -ox; x < w; x += 64)
            for (int y = -oy; y < h; y += 64) g.blit(NOISE[f], x, y, 0, 0, 64, 64, 64, 64);
        g.setColor(1f, 1f, 1f, 1f);
    }

    public static String keyName() {
        return ClientEvents.KEY_CAM.getTranslatedKeyMessage().getString().toUpperCase();
    }

    public static void render(GuiGraphics g, float pt, int w, int h) {
        if (!ClientState.active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        tex();
        Font font = mc.font;
        boolean cut = ClientState.cutId != null;
        int t = ClientState.ticks;

        // ---- mood: vignette, heavier with fear
        float vig = 0.55f + ClientState.fearSmooth * 0.4f + (ClientState.hidden ? 0.2f : 0f);
        g.setColor(1f, 1f, 1f, Math.min(1f, vig));
        g.blit(VIGNETTE, 0, 0, w, h, 0, 0, 256, 256, 256, 256);
        g.setColor(1f, 1f, 1f, 1f);

        // ---- the camcorder
        float nv = ClientState.nvFade;
        if (nv > 0.02f && !cut) {
            g.fill(0, 0, w, h, argb(0.10f * nv, 0x20FF50));
            for (int y = (t % 3); y < h; y += 3) g.fill(0, y, w, y + 1, argb(0.10f * nv, 0x000000));
            noise(g, w, h, 0.10f * nv);
            int m = 14, L = 22, c = argb(0.85f * nv, 0xE8FFE8);
            g.fill(m, m, m + L, m + 1, c); g.fill(m, m, m + 1, m + L, c);
            g.fill(w - m - L, m, w - m, m + 1, c); g.fill(w - m - 1, m, w - m, m + L, c);
            g.fill(m, h - m - 1, m + L, h - m, c); g.fill(m, h - m - L, m + 1, h - m, c);
            g.fill(w - m - L, h - m - 1, w - m, h - m, c); g.fill(w - m - 1, h - m - L, w - m, h - m, c);
            g.fill(w / 2 - 6, h / 2, w / 2 + 7, h / 2 + 1, argb(0.35f * nv, 0xE8FFE8));
            g.fill(w / 2, h / 2 - 6, w / 2 + 1, h / 2 + 7, argb(0.35f * nv, 0xE8FFE8));
            if ((t / 10) % 2 == 0) g.fill(m + 8, m + 9, m + 14, m + 15, argb(nv, 0xFF2020));
            g.drawString(font, "REC", m + 18, m + 8, argb(nv, 0xFFFFFF), false);
            int secs = t / 20;
            String tc = String.format("%02d:%02d:%02d", secs / 3600, (secs / 60) % 60, secs % 60);
            g.drawString(font, tc, w - m - 8 - font.width(tc), m + 8, argb(nv, 0xFFFFFF), false);
            g.drawString(font, "NIGHT SHOT", m + 8, h - m - 16, argb(0.8f * nv, 0xC8FFC8), false);
            // battery
            int bw = 30, bx = w - m - 8 - bw, by = h - m - 17;
            float bat = ClientState.battery / 100f;
            int bc = bat < 0.15f ? ((t / 6) % 2 == 0 ? 0xFF3030 : 0x601010) : 0xE8FFE8;
            g.fill(bx - 1, by - 1, bx + bw + 1, by, argb(nv, bc)); g.fill(bx - 1, by + 8, bx + bw + 1, by + 9, argb(nv, bc));
            g.fill(bx - 1, by, bx, by + 8, argb(nv, bc)); g.fill(bx + bw, by, bx + bw + 1, by + 8, argb(nv, bc));
            g.fill(bx + bw + 1, by + 2, bx + bw + 3, by + 6, argb(nv, bc));
            g.fill(bx + 1, by + 1, bx + 1 + (int) ((bw - 2) * bat), by + 7, argb(nv, bc));
            String sp = "x" + ClientState.spare;
            g.drawString(font, sp, bx - 6 - font.width(sp), by, argb(0.8f * nv, 0xC8FFC8), false);
        }

        // ---- inside a wardrobe
        if (ClientState.hidden) {
            g.blit(SLATS, 0, 0, w, h, 0, 0, 64, 64, 64, 64);
            String s = I18n.get("msg.hatman.hide_exit");
            g.drawString(font, s, (w - font.width(s)) / 2, h - 34, 0x90FFFFFF, true);
        }
        if (ClientState.slatsT > 0 && ClientState.hidden) {
            // his shadow crosses the slats
            float k = (float) Math.sin(Math.PI * (1 - ClientState.slatsT / 90f));
            g.fill(0, 0, w, h, argb(0.55f * Math.max(0, k), 0x000000));
        }

        // ---- interference
        if (ClientState.staticT > 0) noise(g, w, h, Math.min(0.85f, 0.25f + ClientState.staticT * 0.06f));
        if (ClientState.flashT > 0) g.fill(0, 0, w, h, argb(ClientState.flashT * 0.07f, 0xDDE6FF));
        if (ClientState.jumpT > 0) {
            int sx = (int) (Math.sin(t * 2.7) * 6), sy = (int) (Math.cos(t * 3.9) * 5);
            g.fill(0, 0, w, h, 0xFF000000);
            int iw = (int) (h * 1.5f) + 30, ih = h + 24;
            g.blit(JUMP, (w - iw) / 2 + sx, -12 + sy, iw, ih, 0, 0, 512, 384, 512, 384);
            noise(g, w, h, 0.4f);
            if (ClientState.jumpT < 10) g.fill(0, 0, w, h, argb(1f - ClientState.jumpT / 10f, 0x000000));
        }
        if (ClientState.fadeInT > 0) g.fill(0, 0, w, h, argb(ClientState.fadeInT / (float) ClientState.fadeInMax, 0x000000));
        if (ClientState.freezeT > 0) g.fill(0, 0, w, h, argb(0.25f, 0x000000));

        if (cut || ClientState.jumpT > 0) return;

        // ---- objective
        if (!ClientState.objective.isEmpty() && !ClientState.dialogue) {
            String txt = I18n.get("obj.hatman." + ClientState.objective, ClientState.objArg);
            float flash = ClientState.objFlash < 60 ? 1f : 0.72f;
            int x = 12, y = nv > 0.5f ? 40 : 12;
            String head = I18n.get("hud.hatman.objective");
            g.drawString(font, head, x, y, argb(flash, ClientState.objFlash < 60 && (t / 5) % 2 == 0 ? 0xFFFFFF : 0xB8A26A), true);
            List<FormattedCharSequence> lines = font.split(Component.literal(txt), Math.min(220, w / 2));
            int yy = y + 11;
            for (FormattedCharSequence l : lines) {
                g.drawString(font, l, x, yy, argb(flash, 0xE6E0D0), true);
                yy += 10;
            }
            String notes = I18n.get("hud.hatman.notes", ClientState.notes, ClientState.notesMax);
            g.drawString(font, notes, x, yy + 3, argb(0.55f, 0xB0B0A0), true);
        }

        // ---- subtitles / inner voice
        if (ClientState.lineKey != null && !ClientState.dialogue) {
            String txt = I18n.get(ClientState.lineKey, ClientState.playerName);
            float a = Math.min(1f, Math.min(ClientState.lineT / 8f, (ClientState.lineDur - ClientState.lineT) / 10f));
            subtitle(g, font, w, h - 62, ClientState.lineSpeaker, txt, a);
        }

        // ---- chapter card
        if (ClientState.chapterT >= 0) {
            int ct = ClientState.chapterT;
            float a = Math.min(1f, Math.min(ct / 20f, (150 - ct) / 30f));
            g.fill(0, h / 2 - 34, w, h / 2 + 30, argb(0.6f * a, 0x000000));
            String c1 = I18n.get("hud.hatman.chapter", ClientState.chapterN);
            String c2 = I18n.get("chapter.hatman." + ClientState.chapterN);
            String c3 = I18n.get("chapter.hatman." + ClientState.chapterN + ".sub");
            g.drawString(font, c1, (w - font.width(c1)) / 2, h / 2 - 26, argb(a, 0xB8A26A), false);
            g.pose().pushPose();
            g.pose().translate(w / 2f, h / 2f - 12, 0);
            g.pose().scale(2f, 2f, 1f);
            g.drawString(font, c2, -font.width(c2) / 2, 0, argb(a, 0xF0EAD8), false);
            g.pose().popPose();
            g.drawString(font, c3, (w - font.width(c3)) / 2, h / 2 + 14, argb(a * 0.8f, 0xA0A098), false);
        }

        // ---- hints
        if (ClientState.hintKey != null) {
            String txt = I18n.get("hint.hatman." + ClientState.hintKey, keyName());
            float a = Math.min(1f, Math.min(ClientState.hintT / 10f, (260 - ClientState.hintT) / 20f));
            List<FormattedCharSequence> lines = font.split(Component.literal(txt), 260);
            int y = h / 2 + 40;
            g.fill(w / 2 - 140, y - 5, w / 2 + 140, y + lines.size() * 10 + 4, argb(0.6f * a, 0x000000));
            for (FormattedCharSequence l : lines) {
                g.drawString(font, l, (w - font.width(l)) / 2, y, argb(a, 0xE8E2C8), false);
                y += 10;
            }
        }
    }

    static int speakerColor(String speaker) {
        return switch (speaker) {
            case "hatman", "voice" -> 0xC8A050;
            case "ruth" -> 0x8FB4C8;
            case "tv" -> 0xA8C8A0;
            default -> 0xC8C8C0;
        };
    }

    static void subtitle(GuiGraphics g, Font font, int w, int y, String speaker, String txt, float a) {
        boolean thought = speaker.equals("you") || speaker.equals("narr") || speaker.isEmpty();
        String name = thought ? "" : I18n.get("speaker.hatman." + speaker);
        List<FormattedCharSequence> lines = font.split(Component.literal(txt), Math.min(340, w - 60));
        int hh = lines.size() * 10 + (name.isEmpty() ? 0 : 11);
        int mw = 0;
        for (FormattedCharSequence l : lines) mw = Math.max(mw, font.width(l));
        g.fill((w - mw) / 2 - 8, y - 4, (w + mw) / 2 + 8, y + hh + 3, argb(0.55f * a, 0x000000));
        if (!name.isEmpty()) {
            g.drawString(font, name, (w - font.width(name)) / 2, y, argb(a, speakerColor(speaker)), false);
            y += 11;
        }
        for (FormattedCharSequence l : lines) {
            g.drawString(font, l, (w - font.width(l)) / 2, y, argb(a, thought ? 0xB8B8B0 : 0xF0ECE0), false);
            y += 10;
        }
    }
}
