package com.krolasyon.titans.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;

/** Camera shake that scales with how close the viewer is to whatever just hit the ground. */
final class Shake {
    private static float trauma;

    static void add(double x, double y, double z, float amount) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        double d = Math.sqrt(mc.gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z));
        float k = (float) Math.max(0, 1 - d / 280);
        trauma = Math.min(1.3f, trauma + amount * k * k);
    }

    static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && !Minecraft.getInstance().isPaused()) trauma *= 0.87f;
    }

    static void camera(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (trauma < 0.01f || mc.level == null) return;
        float t = (float) ((mc.level.getGameTime() % 100000) + e.getPartialTick());
        e.setPitch(e.getPitch() + Mth.sin(t * 2.7f) * trauma * 1.5f);
        e.setYaw(e.getYaw() + Mth.cos(t * 3.1f) * trauma * 1.1f);
        e.setRoll(e.getRoll() + Mth.sin(t * 2.1f + 1) * trauma * 1.4f);
    }
}
