package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.story.Lines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** What sits over the game: the current objective, Elias' thoughts as he speaks them, and the camera's shaking. */
public final class StoryHud {
    static String obj, sayKey;
    static int objT, sayT, sayLen, shakeT;

    private StoryHud() {}

    public static void obj(String key) { obj = key; objT = 0; }

    public static void say(String key) {
        sayKey = key;
        sayT = 0;
        sayLen = Lines.ticks(key) + 30;
    }

    public static void shake(int t) { shakeT = Math.max(shakeT, t); }

    public static void tick() {
        objT++;
        if (sayKey != null) {
            String txt = I18n.get("line.bloodsea." + sayKey);
            int d = Lines.ticks(sayKey);
            murmur(Lines.ALL.get(sayKey).speaker(), txt, sayT == 0 ? 0 : typed(txt, d, sayT), typed(txt, d, sayT + 1));
            if (++sayT > sayLen) sayKey = null;
        }
        if (shakeT > 0) shakeT--;
    }

    /** camera shake offsets in degrees: {pitch, yaw, roll} */
    public static float[] shake(float pt) {
        if (shakeT <= 0) return null;
        float k = Math.min(1f, shakeT / 10f);
        long n = System.nanoTime();
        return new float[]{(float) Math.sin(n * 7.1e-8) * 2.2f * k, (float) Math.sin(n * 5.3e-8) * 1.6f * k, (float) Math.sin(n * 6.2e-8) * 2.5f * k};
    }

    public static void render(GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CutScreen || mc.options.hideGui) return;
        Font f = mc.font;
        if (obj != null) {
            float a = Math.min(1f, objT / 20f);
            String head = I18n.get("hud.bloodsea.objective").toUpperCase();
            String txt = I18n.get("obj.bloodsea." + obj);
            List<FormattedCharSequence> ls = f.split(Component.literal(txt), 200);
            int bh = 14 + ls.size() * 10;
            g.fill(6, 6, 214, 10 + bh, argb(0.45f * a, 0x000000));
            g.fill(6, 6, 8, 10 + bh, argb(a, 0x8A1010));
            g.drawString(f, head, 13, 10, argb(a, 0xB02020), false);
            int y = 22;
            for (FormattedCharSequence l : ls) { g.drawString(f, l, 13, y, argb(a, 0xE0D8C8), false); y += 10; }
        }
        if (sayKey != null) {
            float a = Math.min(1f, Math.min(sayT / 8f, (sayLen - sayT) / 10f));
            String txt = I18n.get("line.bloodsea." + sayKey);
            subtitle(g, f, w, h - 70, Lines.ALL.get(sayKey).speaker(), txt, a, typed(txt, Lines.ticks(sayKey), sayT + pt));
        }
    }

    // ------------------------------------------------------------------ the line types itself out, with a murmur per syllable

    /** how many characters of a line are showing this long after it began */
    public static int typed(String txt, int dur, float since) {
        float rate = Math.max(1.3f, txt.length() / Math.max(1f, dur * 0.55f));
        return Math.min(txt.length(), (int) (since * rate));
    }

    /** the murmur, pitched to whoever speaks: called each tick with how much was showing before and now */
    public static void murmur(String speaker, String txt, int before, int now) {
        int voiced = 0;
        for (int i = Math.max(0, before); i < now && i < txt.length(); i++) if (Character.isLetterOrDigit(txt.charAt(i))) voiced++;
        if (voiced == 0) return;
        // one murmur per couple of letters, not per letter
        if (((before / 3) == (now / 3)) && before > 0) return;
        float pitch = switch (speaker) { case "anna" -> 1.45f; case "voice" -> 0.62f; case "nurse" -> 1.3f; case "rescuer", "man" -> 0.85f; default -> 1.0f; };
        pitch *= 0.94f + (float) Math.random() * 0.12f;
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(com.krolasyon.bloodsea.registry.ModSounds.get("talk_blip"), pitch, 0.32f));
    }

    static void subtitle(GuiGraphics g, Font font, int w, int y, String speaker, String txt, float a) { subtitle(g, font, w, y, speaker, txt, a, txt.length()); }

    static void subtitle(GuiGraphics g, Font font, int w, int y, String speaker, String txt, float a, int shown) {
        String name = I18n.get("speaker.bloodsea." + speaker);
        List<String> lines = new java.util.ArrayList<>();
        for (var ft : font.getSplitter().splitLines(Component.literal(txt), Math.min(360, w - 60), net.minecraft.network.chat.Style.EMPTY)) lines.add(ft.getString());
        int hh = lines.size() * 10 + 11;
        int mw = font.width(name);
        for (String l : lines) mw = Math.max(mw, font.width(l));
        g.fill((w - mw) / 2 - 8, y - 4, (w + mw) / 2 + 8, y + hh + 3, argb(0.55f * a, 0x000000));
        int col = switch (speaker) { case "anna" -> 0xD8B070; case "voice" -> 0xB02828; case "nurse" -> 0xA8C8D8; case "rescuer", "man" -> 0xC8B890; default -> 0xE8C8A0; };
        g.drawString(font, name, (w - font.width(name)) / 2, y, argb(a, col), false);
        y += 11;
        int pos = 0;
        for (String l : lines) {
            int at = txt.indexOf(l, pos);
            if (at < 0) at = pos;
            pos = at + l.length();
            String part = l.substring(0, Math.max(0, Math.min(l.length(), shown - at)));
            g.drawString(font, part, (w - font.width(l)) / 2, y, argb(a, 0xF0ECE0), false);
            y += 10;
        }
    }

    static int argb(float a, int rgb) { return ((int) (Math.max(0.02f, Math.min(1f, a)) * 255) << 24) | rgb; }
}
