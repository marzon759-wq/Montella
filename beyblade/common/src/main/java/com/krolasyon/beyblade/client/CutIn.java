package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.CommonBeybladeEntity;
import com.krolasyon.beyblade.registry.ModSounds;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * The anime "cut-in" when a special move starts: a slanted band slams across the screen with the bey's emblem,
 * the move's name and speed lines, then flies off. Also draws the white impact flash. Drawn after the frame, so
 * it shows even with the HUD hidden (F1) and in recordings.
 */
@EventBusSubscriber(modid = "librabeyblade", bus = EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CutIn {
   private static final double LIFE = 46.0;
   /** The text band is off (no on-screen texts); the start of a special move is felt through sound, shake and flash. */
   private static final boolean BANNER = false;
   private static String name = "";
   private static String title = "";
   private static ResourceLocation icon;
   private static int color = 0xFFFFFFFF;
   private static double start = -1000.0;

   private CutIn() {
   }

   public static void show(BeybladeEntity e, int ability) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.gameRenderer.getMainCamera().getPosition().distanceTo(e.position()) > 48.0) {
         return;
      }
      String key = e.abilityKey(ability);
      if (key == null || key.isEmpty()) {
         return;
      }
      name = I18n.get(key).toUpperCase(java.util.Locale.ROOT);
      title = I18n.get("hud.librabeyblade.title." + e.kindId());
      String kind = e instanceof CommonBeybladeEntity c ? "common_" + Math.floorMod(c.getVariant(), 10) : e.kindId();
      icon = V.rl("librabeyblade", "textures/item/" + kind + "_icon.png");
      float[] g = BeyRig.of(e, 0.0F).glowCol;
      color = 0xFF000000 | (int)(Mth.clamp(g[0], 0.0F, 1.0F) * 255.0F) << 16 | (int)(Mth.clamp(g[1], 0.0F, 1.0F) * 255.0F) << 8 | (int)(Mth.clamp(g[2], 0.0F, 1.0F) * 255.0F);
      start = CameraFx.now();
      CameraFx.near(e.position(), 0.6F, 40.0);
      mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.CUT_IN.get(), 1.0F, 0.9F));
   }

   private static float easeOut(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return 1.0F - (1.0F - x) * (1.0F - x) * (1.0F - x);
   }

   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onRender(TickEvent.RenderTickEvent e) {
      if (e.phase != TickEvent.Phase.END) {
         return;
      }
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null || mc.screen != null) {
         return;
      }
      double age = CameraFx.now() - start;
      float fl = CameraFx.flash();
      if ((age < 0.0 || age > LIFE) && fl < 0.01F) {
         return;
      }
      final double ageF = age;
      com.krolasyon.beyblade.compat.VClient.overlay(g -> draw(mc, g, ageF, fl));
   }

   private static void draw(Minecraft mc, GuiGraphics g, double age, float fl) {
      int W = g.guiWidth();
      int H = g.guiHeight();
      if (BANNER && age >= 0.0 && age <= LIFE) {
         float a = (float)age;
         float off;
         if (a < 6.0F) {
            off = -W * (1.0F - easeOut(a / 6.0F));
         } else if (a > LIFE - 8.0) {
            off = W * easeOut((a - (float)(LIFE - 8.0)) / 8.0F);
         } else {
            off = (a - 6.0F) * 0.6F;
         }
         int band = Math.max(26, H / 9);
         g.pose().pushPose();
         g.pose().translate(W * 0.5F + off, H * 0.64F, 0.0F);
         g.pose().mulPose(Axis.ZP.rotationDegrees(-7.0F));
         int dark = 0xE00A0A12;
         g.fill(-W, -band, W, band, dark);
         g.fill(-W, -band - 3, W, -band, color);
         g.fill(-W, band, W, band + 3, color);
         // speed lines racing through the band
         for (int i = 0; i < 18; i++) {
            int y = -band + 4 + (int)((i * 37L) % (band * 2 - 8));
            int len = 40 + (i * 53) % 90;
            int x = (int)((-W + ((i * 131 + (int)(a * 55.0F)) % (W * 2))));
            g.fill(-x, y, -x + len, y + 1, (i % 3 == 0 ? color & 0x00FFFFFF | 0x90000000 : 0x50FFFFFF));
         }
         // emblem
         int iconSize = band * 2 - 8;
         if (icon != null) {
            g.blit(icon, -W / 2 + 30, -iconSize / 2, 0.0F, 0.0F, iconSize, iconSize, iconSize, iconSize);
         }
         // move name (big) and bey name
         float sc = Math.max(1.6F, band / 13.0F);
         float room = W * 0.92F - (40 + iconSize);
         int tw = Math.max(1, mc.font.width(name));
         sc = Math.min(sc, room / tw);
         g.pose().pushPose();
         g.pose().translate(-W / 2.0F + 40 + iconSize, -band * 0.55F, 0.0F);
         g.pose().scale(sc, sc, 1.0F);
         g.drawString(mc.font, name, 0, 0, 0xFFFFFFFF, true);
         g.pose().popPose();
         g.pose().pushPose();
         g.pose().translate(-W / 2.0F + 42 + iconSize, band * 0.35F, 0.0F);
         g.pose().scale(sc * 0.55F, sc * 0.55F, 1.0F);
         g.drawString(mc.font, title, 0, 0, color, true);
         g.pose().popPose();
         g.pose().popPose();
         // a white slash flash when it lands
         if (a < 4.0F) {
            int al = (int)((1.0F - a / 4.0F) * 120.0F);
            g.fill(0, 0, W, H, al << 24 | 0xFFFFFF);
         }
      }
      if (fl >= 0.01F) {
         g.fill(0, 0, W, H, (int)(Math.min(1.0F, fl) * 200.0F) << 24 | 0xFFF4E0);
      }
   }
}
