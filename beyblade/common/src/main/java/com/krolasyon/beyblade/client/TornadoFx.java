package com.krolasyon.beyblade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

/** A swirling wind funnel (Storm Bringer and the other tornado moves), drawn from layered scrolling bands. */
public final class TornadoFx {
   private TornadoFx() {
   }

   /**
    * @param x,y,z  base of the funnel relative to the render origin
    * @param rb rt  radius at the bottom / top
    * @param col    rgb, @param a overall strength 0..1
    */
   public static void draw(MultiBufferSource buf, PoseStack pose, double x, double y, double z, float rb, float rt, float h, float t, float[] col, float a) {
      if (a <= 0.01F) {
         return;
      }
      pose.pushPose();
      pose.translate(x, y, z);
      Matrix4f m = pose.last().pose();
      int n = 14;
      for (int pass = 0; pass < 2; pass++) {
         VertexConsumer vc = Fx.buf(buf, pass == 0 ? Fx.WAVE : Fx.SOFT);
         for (int k = 0; k < n; k++) {
            float f0 = (float)k / n;
            float f1 = (float)(k + 1) / n;
            float r0 = rb + (rt - rb) * (float)Math.pow(f0, 1.4);
            float r1 = rb + (rt - rb) * (float)Math.pow(f1, 1.4);
            // the funnel snakes a little
            float wob = 0.25F * (float)Math.sin(t * 0.12F + f0 * 5.0F);
            float al0 = a * (0.55F - 0.35F * f0) * (pass == 0 ? 1.0F : 0.35F);
            float al1 = a * (0.55F - 0.35F * f1) * (pass == 0 ? 1.0F : 0.35F);
            pose.pushPose();
            Fx.band(vc, m, r0 + wob, f0 * h, r1 + wob, f1 * h, 28,
               Fx.c(col[0], col[1], col[2], al0), Fx.c(col[0], col[1], col[2], al1),
               3.0F, -t * (0.09F + 0.03F * k) + k * 0.37F, 1.0F, t * 0.05F);
            pose.popPose();
         }
         // streaks of debris spiralling up
         vc = Fx.buf(buf, Fx.SOFT);
         for (int i = 0; i < 4; i++) {
            Fx.helix(vc, m, 0.0F, h, rb * 1.1F, rt * 0.9F, t * 0.35F + i * 1.57F, 2.5F, 0.25F, 24,
               Fx.c(col[0], col[1], col[2], a * 0.8F), Fx.c(col[0], col[1], col[2], 0.0F), t * 0.1F);
         }
      }
      pose.popPose();
   }
}
