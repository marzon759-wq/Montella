package com.krolasyon.mimic.client;

import com.krolasyon.mimic.compat.Compat;
import com.mojang.blaze3d.systems.RenderSystem;
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
    private static ResourceLocation VIGNETTE, JUMP, SLATS, HAY, BEAM, BLOOD, EYE;

    private Hud() {}

    private static void tex() {
        if (NOISE != null) return;
        NOISE = new ResourceLocation[4];
        for (int i = 0; i < 4; i++) NOISE[i] = Compat.rl("textures/gui/noise_" + i + ".png");
        VIGNETTE = Compat.rl("textures/gui/vignette.png");
        JUMP = Compat.rl("textures/gui/jumpscare.png");
        SLATS = Compat.rl("textures/gui/slats.png");
        HAY = Compat.rl("textures/gui/hay.png");
        BEAM = Compat.rl("textures/gui/beam.png");
        BLOOD = Compat.rl("textures/gui/blood.png");
        EYE = Compat.rl("textures/gui/eye.png");
    }

    static int argb(float a, int rgb) {
        return ((int) (Math.max(0, Math.min(1, a)) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    static void blend() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    static void noise(GuiGraphics g, int w, int h, float alpha) {
        tex();
        g.flush();
        blend();
        int f = (ClientState.ticks / 2) & 3;
        int ox = (ClientState.ticks * 37) % 64, oy = (ClientState.ticks * 53) % 64;
        g.setColor(1f, 1f, 1f, alpha);
        for (int x = -ox; x < w; x += 64)
            for (int y = -oy; y < h; y += 64) g.blit(NOISE[f], x, y, 0, 0, 64, 64, 64, 64);
        g.setColor(1f, 1f, 1f, 1f);
    }

    public static String keyName() {
        return ClientEvents.KEY_LIGHT.getTranslatedKeyMessage().getString().toUpperCase();
    }

    /** the prompt for the story thing under the crosshair, or null */
    static String actionPrompt(Minecraft mc) {
        if (!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bh) || bh.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return null;
        if (mc.player == null || bh.getLocation().distanceTo(mc.player.getEyePosition()) > 4.2) return null;
        com.krolasyon.mimic.story.House.ensureIndexed();
        net.minecraft.core.BlockPos l = bh.getBlockPos().offset(-ClientState.ox, -ClientState.oy, -ClientState.oz);
        String tag = com.krolasyon.mimic.story.House.HOT.get(l);
        if (tag == null) return null;
        String k;
        String arg = "";
        if (tag.startsWith("pickup:")) {
            k = "take";
            arg = I18n.get("item.mimicry." + tag.substring(7));
        } else if (tag.startsWith("drawer:")) k = "search";
        else if (tag.startsWith("note:")) k = "read";
        else if (tag.startsWith("wardrobe:")) k = "hide";
        else if (tag.startsWith("look:")) k = "look";
        else if (tag.startsWith("door:")) k = "open";
        else if (tag.equals("coat")) k = "coat";
        else if (tag.equals("tape")) k = "tape";
        else k = "use";
        return I18n.get("hud.mimic.act." + k, arg);
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
        float vig = 0.34f + ClientState.fearSmooth * 0.5f + (ClientState.hidden ? 0.2f : 0f);
        g.flush();
        blend();
        g.setColor(1f, 1f, 1f, Math.min(1f, vig));
        g.blit(VIGNETTE, 0, 0, w, h, 0, 0, 256, 256, 256, 256);
        g.setColor(1f, 1f, 1f, 1f);

        // ---- the flashlight: a soft beam in the middle of a dark frame
        float lf = ClientState.lightFade;
        if (lf > 0.02f && !cut && !ClientState.hidden) {
            g.flush();
            blend();
            g.setColor(1f, 1f, 1f, 0.55f * lf);
            g.blit(BEAM, 0, 0, w, h, 0, 0, 256, 256, 256, 256);
            g.setColor(1f, 1f, 1f, 1f);
        }
        float vt = ClientState.ticks;
        if (!cut && ClientState.hasLight) {
            // battery, bottom right
            int bw = 34, bx = w - 14 - bw, by = h - 22;
            float bat = ClientState.battery / 100f;
            int bc = bat < 0.15f ? ((t / 6) % 2 == 0 ? 0xD03030 : 0x601010) : ClientState.lightOn ? 0xF0E6C8 : 0x807868;
            float a = ClientState.lightOn ? 0.9f : 0.5f;
            g.fill(bx - 1, by - 1, bx + bw + 1, by, argb(a, bc)); g.fill(bx - 1, by + 8, bx + bw + 1, by + 9, argb(a, bc));
            g.fill(bx - 1, by, bx, by + 8, argb(a, bc)); g.fill(bx + bw, by, bx + bw + 1, by + 8, argb(a, bc));
            g.fill(bx + bw + 1, by + 2, bx + bw + 3, by + 6, argb(a, bc));
            g.fill(bx + 1, by + 1, bx + 1 + (int) ((bw - 2) * bat), by + 7, argb(a, bc));
            String sp = "x" + ClientState.spare;
            g.drawString(font, sp, bx - 6 - font.width(sp), by, argb(0.7f, 0xD8D0B8), false);
            String k = "[" + keyName() + "]";
            g.drawString(font, k, bx, by - 11, argb(0.45f, 0xD8D0B8), false);
        }
        // ---- breath: a thin line over the hotbar, only while it is not full
        if (!cut && !ClientState.hidden && ClientState.stamina < 99) {
            int bw = 80, bx = (w - bw) / 2, by = h - 52;
            float st = ClientState.stamina / 100f;
            int col = ClientState.tired ? ((t / 5) % 2 == 0 ? 0xD03028 : 0x802018) : 0xD8D0B8;
            g.fill(bx - 1, by - 1, bx + bw + 1, by + 2, 0x70000000);
            g.fill(bx, by, bx + (int) (bw * st), by + 1, argb(ClientState.tired ? 0.9f : 0.55f, col));
        }
        // ---- how loud you are: rings under the crosshair
        if (!cut && !ClientState.hidden && ClientState.noise > 0.5f) {
            float n = Math.min(1f, ClientState.noise / 17f);
            int r = 6 + (int) (n * 14);
            int c = argb(0.18f + 0.4f * n, n > 0.8f ? 0xFF5040 : 0xE8E0C8);
            g.fill(w / 2 - r, h / 2 + 22, w / 2 + r, h / 2 + 23, c);
        }
        // ---- what the crosshair is on: a quiet prompt, so nothing usable goes unnoticed
        if (!cut && !ClientState.hidden && !ClientState.dialogue && mc.screen == null) {
            String act = actionPrompt(mc);
            if (act != null) g.drawString(font, act, (w - font.width(act)) / 2, h / 2 + 12, argb(0.75f, 0xE8E0C8), true);
        }
        if (!cut && ClientState.cover) {
            String s2 = I18n.get("hud.mimic.cover");
            g.drawString(font, s2, (w - font.width(s2)) / 2, h - 50, argb(0.6f, 0xB8C890), true);
        }
        // ---- the sleeper stirring
        if (!cut && ClientState.disturb >= 0) {
            float d = ClientState.disturb;
            g.flush();
            blend();
            float pulse = d > 0.6f ? 0.7f + 0.3f * (float) Math.sin(vt * 0.6f) : 1f;
            g.setColor(1f, 1f - d * 0.8f, 1f - d * 0.8f, (0.35f + 0.6f * d) * pulse);
            g.blit(EYE, w / 2 - 16, 8, 32, 16, 0, (int) (Math.min(3, d * 4)) * 16, 32, 16, 32, 64);
            g.setColor(1f, 1f, 1f, 1f);
            g.fill(w / 2 - 30, 27, w / 2 + 30, 29, 0x60000000);
            g.fill(w / 2 - 30, 27, w / 2 - 30 + (int) (60 * d), 29, argb(0.8f, d > 0.7f ? 0xD03028 : 0xC8B890));
        }

        // ---- inside a wardrobe
        if (ClientState.hidden) {
            g.flush();
            blend();
            if (ClientState.hayHide) g.blit(HAY, 0, 0, w, h, 0, 0, 256, 256, 256, 256);
            else g.blit(SLATS, 0, 0, w, h, 0, 0, 64, 64, 64, 64);
            String s = I18n.get("msg.mimic.hide_exit");
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
        if (ClientState.killT > 0) {
            int kt = ClientState.killT;
            int sx = (int) (Math.sin(t * 3.1) * 9), sy = (int) (Math.cos(t * 4.3) * 7);
            g.fill(0, 0, w, h, 0xFF000000);
            if (kt > 24) {
                float zoom = 1f + (64 - kt) * 0.012f;
                int iw = (int) ((h * 1.5f + 30) * zoom), ih = (int) ((h + 24) * zoom);
                g.blit(JUMP, (w - iw) / 2 + sx, (h - ih) / 2 + sy, iw, ih, 0, 0, 512, 384, 512, 384);
            }
            g.flush();
            blend();
            g.setColor(1f, 1f, 1f, Math.min(1f, (64 - kt) / 14f));
            g.blit(BLOOD, 0, 0, w, h, 0, 0, 256, 256, 256, 256);
            g.setColor(1f, 1f, 1f, 1f);
            noise(g, w, h, 0.3f);
            if (kt < 16) g.fill(0, 0, w, h, argb(1f - kt / 16f, 0x000000));
        }
        // ---- quick time events
        if (Qte.type != null && !cut) qte(g, font, w, h);
        if (ClientState.fadeInT > 0) g.fill(0, 0, w, h, argb(ClientState.fadeInT / (float) ClientState.fadeInMax, 0x000000));
        if (ClientState.freezeT > 0) g.fill(0, 0, w, h, argb(0.25f, 0x000000));

        if (cut || ClientState.jumpT > 0 || ClientState.killT > 0) return;

        // ---- objective
        if (!ClientState.objective.isEmpty() && !ClientState.dialogue) {
            String txt = I18n.get("obj.mimic." + ClientState.objective, ClientState.objArg);
            float flash = ClientState.objFlash < 60 ? 1f : 0.72f;
            int x = 12, y = 12;
            String head = I18n.get("hud.mimic.objective");
            g.drawString(font, head, x, y, argb(flash, ClientState.objFlash < 60 && (t / 5) % 2 == 0 ? 0xFFFFFF : 0xB8A26A), true);
            List<FormattedCharSequence> lines = font.split(Component.literal(txt), Math.min(220, w / 2));
            int yy = y + 11;
            for (FormattedCharSequence l : lines) {
                g.drawString(font, l, x, yy, argb(flash, 0xE6E0D0), true);
                yy += 10;
            }
            String notes = I18n.get("hud.mimic.notes", ClientState.notes, ClientState.notesMax);
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
            String c1 = I18n.get("hud.mimic.chapter", ClientState.chapterN);
            String c2 = I18n.get("chapter.mimic." + ClientState.chapterN);
            String c3 = I18n.get("chapter.mimic." + ClientState.chapterN + ".sub");
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
            String txt = I18n.get("hint.mimic." + ClientState.hintKey, keyName());
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
            case "lily" -> 0x9FC0D8;
            case "lily_q", "it" -> 0xC86A50;
            case "dad", "machine" -> 0xC8B070;
            default -> 0xC8C8C0;
        };
    }

    private static void qte(GuiGraphics g, Font font, int w, int h) {
        int t = ClientState.ticks;
        int cy = h / 2 + 46;
        switch (Qte.type) {
            case "breath" -> {
                String s = I18n.get("qte.mimic.breath");
                g.drawCenteredString(font, s, w / 2, cy - 14, argb(0.8f + 0.2f * (float) Math.sin(t * 0.5), 0xF0E8D0));
                float b = Qte.breath / 100f;
                g.fill(w / 2 - 60, cy, w / 2 + 60, cy + 5, 0x90000000);
                g.fill(w / 2 - 60, cy, w / 2 - 60 + (int) (120 * b), cy + 5, argb(0.9f, b < 0.3f ? 0xD03028 : 0x9FC0D8));
            }
            case "still" -> {
                String s = I18n.get("qte.mimic.still");
                g.pose().pushPose();
                g.pose().translate(w / 2f, h / 2f - 40, 0);
                g.pose().scale(2f, 2f, 1f);
                g.drawCenteredString(font, s, 0, 0, argb(0.85f, 0xF0E8D0));
                g.pose().popPose();
                float m = Math.min(1f, Qte.motion / 40f);
                g.fill(w / 2 - 50, cy, w / 2 + 50, cy + 4, 0x90000000);
                g.fill(w / 2 - (int) (50 * m), cy, w / 2 + (int) (50 * m), cy + 4, argb(0.9f, 0xD03028));
            }
            case "mash" -> {
                String s = I18n.get("qte.mimic.mash");
                float k = Qte.mash / 100f;
                float jit = (float) Math.sin(t * 1.7) * 2;
                g.drawCenteredString(font, s, w / 2 + (int) jit, cy - 14, 0xFFF0E8D0);
                g.fill(w / 2 - 70, cy, w / 2 + 70, cy + 6, 0x90000000);
                g.fill(w / 2 - 70, cy, w / 2 - 70 + (int) (140 * k), cy + 6, argb(0.95f, 0xE8C060));
            }
            default -> {}
        }
        float tl = Qte.t / (float) Qte.max;
        g.fill(w / 2 - 40, cy + 10, w / 2 - 40 + (int) (80 * tl), cy + 11, 0x80E8E0C8);
    }

    static void subtitle(GuiGraphics g, Font font, int w, int y, String speaker, String txt, float a) {
        boolean thought = speaker.equals("you") || speaker.equals("narr") || speaker.isEmpty();
        String name = thought ? "" : I18n.get("speaker.mimic." + speaker);
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
