package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.CommonBeybladeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class SpecialClashFx {
   private static final float[] BLACK = new float[]{0.0F, 0.0F, 0.0F};

   private SpecialClashFx() {
   }

   private static float[] k(float[] c, float k) {
      return new float[]{c[0] * k, c[1] * k, c[2] * k};
   }

   public static float[] color(BeybladeEntity e) {
      if (e instanceof CommonBeybladeEntity c) {
         return c.variant().rgb(true);
      } else {
         String c = e.kindId();

         return switch (c) {
            case "ldrago" -> new float[]{1.0F, 0.15F, 0.25F};
            case "leone" -> new float[]{0.35F, 1.0F, 0.5F};
            case "eagle" -> new float[]{0.7F, 0.45F, 1.0F};
            case "pegasus" -> new float[]{0.35F, 0.7F, 1.0F};
            case "tempo" -> new float[]{0.75F, 0.85F, 1.0F};
            case "variares" -> new float[]{1.0F, 0.2F, 0.1F};
            case "beffal" -> new float[]{1.0F, 0.35F, 0.75F};
            default -> new float[]{0.5F, 1.0F, 0.5F};
         };
      }
   }

   public static void render(BeybladeEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      float boom = t - (float)e.sclashBoomTime;
      if (boom >= 0.0F && boom < 30.0F) {
         renderBoom(e, boom, t, ex, ey, ez, pose, buf, cam);
      }

      int pid = e.getSpecialClashPartner();
      if (pid >= 0 && pid >= e.getId()) {
         if (e.level().getEntity(pid) instanceof BeybladeEntity other) {
            float age = t - (float)e.sclashClientStart;
            if (!(age < 0.0F)) {
               float kk = Mth.clamp(age / 30.0F, 0.0F, 1.0F);
               double ox = Mth.lerp((double)partial, other.xo, other.getX()) - ex;
               double oy = Mth.lerp((double)partial, other.yo, other.getY()) - ey;
               double oz = Mth.lerp((double)partial, other.zo, other.getZ()) - ez;
               Vec3 a = new Vec3(0.0, 0.55, 0.0);
               Vec3 b = new Vec3(ox, oy + 0.55, oz);
               double f = BeybladeEntity.sclashMeet(e.getSpecialClashRatio(), kk) + 0.035 * Math.sin((double)t * 1.4) * (1.0 - (double)kk * 0.6);
               Vec3 m = a.add(b.subtract(a).scale(f));
               float[] ca = color(e);
               float[] cb = color(other);
               float grow = Mth.clamp(age / 5.0F, 0.0F, 1.0F);
               float pulse = 0.8F + 0.2F * Mth.sin(t * 2.1F);
               Matrix4f mx = pose.last().pose();
               VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
               Fx.beam(vc, mx, a, m, 0.55F * grow * pulse, k(ca, 1.4F), k(ca, 1.6F), 0.5F, t * 0.2F);
               Fx.beam(vc, mx, b, m, 0.55F * grow * pulse, k(cb, 1.4F), k(cb, 1.6F), 0.5F, -t * 0.2F);
               vc = Fx.buf(buf, Fx.WAVE);
               Fx.beam(vc, mx, a, m, 0.9F * grow, k(ca, 0.6F), k(ca, 0.9F), 0.3F, -t * 0.15F);
               Fx.beam(vc, mx, b, m, 0.9F * grow, k(cb, 0.6F), k(cb, 0.9F), 0.3F, t * 0.15F);
               vc = Fx.buf(buf, Fx.BOLT);

               for (int i = 0; i < 5; i++) {
                  double ang = (double)t * 0.9 + (double)i * 1.257;
                  Vec3 tip = m.add(Math.cos(ang) * 1.6, Math.sin(ang * 1.7) * 1.1, Math.sin(ang) * 1.6);
                  float[] c = i % 2 == 0 ? ca : cb;
                  Fx.beamV(vc, mx, m, tip, 0.45F * grow, k(c, 1.3F), 0.5F, t * 0.8F + (float)i);
               }

               vc = Fx.buf(buf, Fx.FLARE);
               Fx.billboard(
                  vc, pose, cam, m.x, m.y, m.z, (2.4F + 0.8F * kk) * grow * pulse, t * 9.0F, k(new float[]{1.0F, 1.0F, 1.0F}, 0.9F)
               );
               Fx.billboard(vc, pose, cam, m.x, m.y, m.z, 3.6F * grow, -t * 5.0F, k(ca, 0.7F));
               Fx.billboard(vc, pose, cam, m.x, m.y, m.z, 3.2F * grow, t * 6.0F, k(cb, 0.7F));
               vc = Fx.buf(buf, Fx.WAVE);
               Vec3 axis = b.subtract(a);
               if (axis.lengthSqr() > 1.0E-4) {
                  Fx.ringAround(vc, mx, m, axis.normalize(), 0.6F + 0.4F * pulse, 1.3F + 0.3F * pulse, 32, k(new float[]{1.0F, 0.9F, 0.6F}, grow), t * 0.1F);
               }

               Fx.ring(vc, mx, 0.05F, 0.8F, 1.6F, 32, BLACK, k(ca, 0.8F * grow), 4.0F, t * 0.08F);
               pose.pushPose();
               pose.translate(ox, oy, oz);
               Fx.ring(vc, pose.last().pose(), 0.05F, 0.8F, 1.6F, 32, BLACK, k(cb, 0.8F * grow), 4.0F, -t * 0.08F);
               pose.popPose();
            }
         }
      }
   }

   private static void renderBoom(
      BeybladeEntity e, float boom, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam
   ) {
      float f = 1.0F - boom / 30.0F;
      double x = e.boomX - ex;
      double y = e.boomY - ey;
      double z = e.boomZ - ez;
      float[] c = e.boomDraw ? new float[]{1.0F, 0.95F, 0.7F} : color(e);
      pose.pushPose();
      pose.translate(x, y, z);
      Matrix4f m = pose.last().pose();
      float r = 0.5F + (1.0F - (1.0F - boom / 14.0F) * (1.0F - boom / 14.0F)) * 8.0F;
      VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
      Fx.ring(vc, m, -0.4F, Math.max(0.0F, r - 1.8F), r, 64, BLACK, k(new float[]{1.0F, 0.9F, 0.6F}, 1.5F * f), 6.0F, 0.0F);
      Fx.ring(vc, m, -0.3F, Math.max(0.0F, r * 0.6F - 1.0F), r * 0.6F, 64, BLACK, k(c, 1.3F * f), 6.0F, 0.3F);
      vc = Fx.buf(buf, Fx.BOLT);
      float len = Math.min(1.0F, boom / 4.0F) * 5.0F;

      for (int i = 0; i < 10; i++) {
         double a = (double)i * Math.PI / 5.0 + 0.2;
         Fx.beamV(
            vc,
            m,
            new Vec3(0.0, 0.0, 0.0),
            new Vec3(Math.cos(a) * (double)len, Math.sin(a * 3.0) * 1.2, Math.sin(a) * (double)len),
            0.7F,
            k(c, 1.3F * f),
            0.4F,
            (float)i * 0.4F
         );
      }

      if (boom < 12.0F) {
         float g = 1.0F - boom / 12.0F;
         vc = Fx.buf(buf, Fx.FLARE);
         Fx.billboard(vc, pose, cam, 0.0, 0.0, 0.0, 11.0F * g, t * 3.0F, k(new float[]{1.0F, 0.95F, 0.85F}, 1.4F * g));
         Fx.billboard(vc, pose, cam, 0.0, 0.0, 0.0, 7.0F * g, -t * 4.0F, k(c, 1.2F * g));
      }

      pose.popPose();
   }
}
