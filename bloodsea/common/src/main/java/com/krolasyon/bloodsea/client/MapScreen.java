package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.compat.Compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** The map, unfolded in front of you: where you are on it, and which way the raft is pointing. */
public class MapScreen extends com.krolasyon.bloodsea.compat.CScreen {
    private static final ResourceLocation TEX = Compat.rl("textures/gui/sea_map.png");
    private int t;

    @Override public void tick() { t++; }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        Minecraft mc = Minecraft.getInstance();
        int w = width, h = height;
        g.fill(0, 0, w, h, 0x88000000);
        int size = Math.min(w, h) - 30;
        int x0 = (w - size) / 2, y0 = (h - size) / 2;
        float a = Math.min(1f, (t + pt) / 6f);
        g.pose().pushPose();
        g.pose().translate(w / 2f, h / 2f, 0);
        g.pose().scale(0.92f + 0.08f * a, 0.92f + 0.08f * a, 1);
        g.pose().translate(-w / 2f, -h / 2f, 0);
        g.blit(TEX, x0, y0, 0, 0, size, size, size, size);
        if (mc.player != null) {
            double s = size / 512.0 / 8.0;
            int px = (int) (x0 + size / 2.0 + mc.player.getX() * s), py = (int) (y0 + size / 2.0 + mc.player.getZ() * s);
            double yaw = Math.toRadians((mc.player.getVehicle() != null ? mc.player.getVehicle().getYRot() : mc.player.getYRot()));
            int fx = (int) (-Math.sin(yaw) * 7), fz = (int) (Math.cos(yaw) * 7);
            boolean blink = (t / 8) % 2 == 0;
            g.fill(px - 2, py - 2, px + 3, py + 3, blink ? 0xFFB01010 : 0xFF600808);
            for (int k = 1; k <= 6; k++) g.fill(px + fx * k / 6, py + fz * k / 6, px + fx * k / 6 + 1, py + fz * k / 6 + 1, 0xFF3A2010);
        }
        g.pose().popPose();
    }

    @Override public boolean isPauseScreen() { return false; }
}
