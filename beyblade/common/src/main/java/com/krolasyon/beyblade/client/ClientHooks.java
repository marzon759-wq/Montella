package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import net.minecraft.client.Minecraft;

public final class ClientHooks {
   private ClientHooks() {
   }

   public static void startSpinSound(BeybladeEntity e) {
      Minecraft.getInstance().getSoundManager().play(new SpinSound(e));
   }

   public static void tipFx(BeybladeEntity e) {
      TipFx.tick(e);
   }

   public static void clash(BeybladeEntity e, boolean heavy) {
      CameraFx.near(e.position(), heavy ? 1.0F : 0.3F, heavy ? 22.0 : 12.0);
   }

   public static void impact(BeybladeEntity e, float k) {
      CameraFx.near(e.position(), 1.4F * k, 40.0);
      CameraFx.flash(e.position(), 0.8F * k, 40.0);
   }

   public static void special(BeybladeEntity e, int ability) {
      CutIn.show(e, ability);
   }
}
