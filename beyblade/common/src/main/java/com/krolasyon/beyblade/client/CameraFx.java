package com.krolasyon.beyblade.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Screen shake, field of view punch and white flash for heavy clashes and special move impacts near the camera.
 * Timed in game ticks, so it also slows down in slow motion.
 */
@EventBusSubscriber(modid = "librabeyblade", bus = EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CameraFx {
   private static float shakePeak;
   private static double shakeStart = -1000.0;
   private static float flashPeak;
   private static double flashStart = -1000.0;

   private CameraFx() {
   }

   static double now() {
      Minecraft mc = Minecraft.getInstance();
      return mc.level == null ? 0.0 : (double)mc.level.getGameTime() + (double)com.krolasyon.beyblade.compat.VClient.partialTick();
   }

   private static double dist(Vec3 p) {
      Minecraft mc = Minecraft.getInstance();
      return mc.gameRenderer.getMainCamera().getPosition().distanceTo(p);
   }

   public static float shake() {
      double age = now() - shakeStart;
      return age < 0.0 ? 0.0F : shakePeak * (float)Math.exp(-age / 6.0);
   }

   public static float flash() {
      double age = now() - flashStart;
      return age < 0.0 ? 0.0F : flashPeak * (float)Math.exp(-age / 2.5);
   }

   public static void near(Vec3 p, float amp, double range) {
      float k = amp * (float)Math.max(0.0, 1.0 - dist(p) / range);
      if (k > shake()) {
         shakePeak = k;
         shakeStart = now();
      }
   }

   public static void flash(Vec3 p, float amp, double range) {
      float k = amp * (float)Math.max(0.0, 1.0 - dist(p) / range);
      if (k > flash()) {
         flashPeak = k;
         flashStart = now();
      }
   }

   @SubscribeEvent
   public static void onAngles(ViewportEvent.ComputeCameraAngles e) {
      float a = shake();
      if (a > 0.005F) {
         double n = now() * 1.9;
         e.setYaw(e.getYaw() + (float)Math.sin(n * 2.3) * a * 1.6F);
         e.setPitch(e.getPitch() + (float)Math.cos(n * 2.9) * a * 1.2F);
         e.setRoll(e.getRoll() + (float)Math.sin(n * 3.7) * a * 2.2F);
      }
   }

   @SubscribeEvent
   public static void onFov(ViewportEvent.ComputeFov e) {
      float a = shake();
      if (a > 0.005F) {
         e.setFOV(e.getFOV() * (1.0 - 0.05 * Math.min(1.5F, a)));
      }
   }
}
