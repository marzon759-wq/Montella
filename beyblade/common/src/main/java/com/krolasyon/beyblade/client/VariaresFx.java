package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.entity.VariaresEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class VariaresFx {
   public static final ResourceLocation SWORD = V.rl("librabeyblade", "textures/fx/sword.png");
   private static final float[] CRIMSON = new float[]{1.0F, 0.12F, 0.12F};
   private static final float[] GOLD = new float[]{1.0F, 0.8F, 0.3F};
   private static final float[] VOLT = new float[]{1.0F, 0.95F, 0.6F};
   private static final float[] BLACK = new float[]{0.0F, 0.0F, 0.0F};

   private VariaresFx() {
   }

   private static float[] k(float[] c, float k) {
      return new float[]{c[0] * k, c[1] * k, c[2] * k};
   }

   private static float easeOut(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return 1.0F - (1.0F - x) * (1.0F - x) * (1.0F - x);
   }

   private static void sword(MultiBufferSource buf, PoseStack pose, Quaternionf cam, double x, double tipY, double z, float len, float a) {
      if (!(a <= 0.01F)) {
         Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F);
         cam.transform(right);
         Vec3 r = new Vec3((double)right.x(), 0.0, (double)right.z());
         if (r.lengthSqr() < 1.0E-4) {
            r = new Vec3(1.0, 0.0, 0.0);
         }

         r = r.normalize().scale((double)(len / 8.0F));
         Matrix4f m = pose.last().pose();
         VertexConsumer vc = Fx.bufT(buf, SWORD, a);
         float[] w = new float[]{1.0F, 1.0F, 1.0F};
         float[] p0 = new float[]{(float)(x - r.x), (float)(tipY + (double)len), (float)(z - r.z)};
         float[] p1 = new float[]{(float)(x - r.x), (float)tipY, (float)(z - r.z)};
         float[] p2 = new float[]{(float)(x + r.x), (float)tipY, (float)(z + r.z)};
         float[] p3 = new float[]{(float)(x + r.x), (float)(tipY + (double)len), (float)(z + r.z)};
         Fx.quad(vc, m, p0, p1, p2, p3, 0.0F, 0.0F, 1.0F, 1.0F, w, w);
         Fx.alpha = 1.0F;
      }
   }

   public static void render(VariaresEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      Matrix4f m = pose.last().pose();
      if (e.isShielded()) {
         float p = 0.75F + 0.25F * Mth.sin(t * 0.6F);
         VertexConsumer vc = Fx.bufT(buf, Fx.WISP, 0.75F * p);
         Fx.band(vc, m, 0.55F, 0.0F, 0.3F, 2.6F, 24, new float[]{1.0F, 0.25F, 0.1F}, new float[]{0.6F, 0.02F, 0.05F}, 2.0F, t * 0.2F, 1.5F, -t * 0.12F);
         Fx.alpha = 1.0F;
         vc = Fx.buf(buf, Fx.SOFT);
         Fx.ring(vc, m, 0.06F, 0.9F, 1.25F, 40, k(GOLD, 0.8F * p), BLACK, 1.0F, 0.0F);

         for (int i = 0; i < 3; i++) {
            Fx.helix(
               vc, m, 0.0F, 2.2F, 0.8F, 0.4F, t * 0.3F + (float)i * (float) (Math.PI * 2) / 3.0F, 1.2F, 0.18F, 20, k(CRIMSON, p), k(GOLD, 0.3F * p), t * 0.1F
            );
         }
      }

      float sl = t - (float)e.slashTime;
      if (sl >= 0.0F && sl < 9.0F) {
         float f = 1.0F - sl / 9.0F;
         Vec3 a = new Vec3(e.slashX0 - ex, e.slashY - ey, e.slashZ0 - ez);
         Vec3 b = new Vec3(e.slashX1 - ex, e.slashY - ey, e.slashZ1 - ez);
         VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
         Fx.beam(vc, m, a, b, 0.5F * f + 0.1F, k(VOLT, 1.5F * f), k(CRIMSON, 1.2F * f), 0.5F, 0.0F);
         vc = Fx.buf(buf, Fx.BOLT);
         Fx.beamV(vc, m, a, b, 1.1F * f, k(VOLT, 1.2F * f), 0.4F, t * 0.7F);
         vc = Fx.buf(buf, Fx.FLARE);
         Vec3 mid = a.add(b).scale(0.5);
         Fx.billboard(vc, pose, cam, mid.x, mid.y, mid.z, 2.6F * f, t * 12.0F, k(VOLT, f));
      }

      int ab = e.clientAbility;
      float at = e.abilityAge(partial);
      float hit = t - (float)e.swordHitTime;
      if (hit >= 0.0F && hit < 40.0F) {
         swordImpact(e, hit, t, ex, ey, ez, pose, buf, cam);
      }

      if (ab != 0 && !(at < 0.0F)) {
         switch (ab) {
            case 3:
               double vx = e.getX() - e.xo;
               double vz = e.getZ() - e.zo;
               double l = Math.sqrt(vx * vx + vz * vz);
               if (!(l < 0.001)) {
                  float ang = (float)Math.atan2(vz / l, vx / l);
                  float f = Mth.clamp(at / 4.0F, 0.0F, 1.0F);
                  VertexConsumer vc = Fx.bufT(buf, Fx.HEX, 0.65F * f);
                  int N = 12;

                  for (int i = 0; i < N; i++) {
                     float a0 = ang - 1.1F + 2.2F * (float)i / (float)N;
                     float a1 = ang - 1.1F + 2.2F * (float)(i + 1) / (float)N;
                     float r = 1.0F;
                     Fx.quad(
                        vc,
                        m,
                        new float[]{Mth.cos(a0) * r, 0.0F, Mth.sin(a0) * r},
                        new float[]{Mth.cos(a0) * r, 1.3F, Mth.sin(a0) * r},
                        new float[]{Mth.cos(a1) * r, 1.3F, Mth.sin(a1) * r},
                        new float[]{Mth.cos(a1) * r, 0.0F, Mth.sin(a1) * r},
                        (float)i / (float)N,
                        0.0F,
                        (float)(i + 1) / (float)N,
                        1.0F,
                        CRIMSON,
                        new float[]{1.0F, 0.5F, 0.3F}
                     );
                  }

                  Fx.alpha = 1.0F;
               }
               break;
            case 4:
               float f = Mth.clamp(at / 6.0F, 0.0F, 1.0F) * Mth.clamp(((float)VariaresEntity.VA_DURATION[4] - at) / 6.0F, 0.0F, 1.0F);
               VertexConsumer vc = Fx.buf(buf, Fx.SOFT);

               for (int i = 0; i < 4; i++) {
                  double a = (double)t * 0.55 * (double)e.spinDir() + (double)i * Math.PI * 2.0 / 4.0;
                  Vec3 c = new Vec3(Math.cos(a) * 2.3, 0.45, Math.sin(a) * 2.3);
                  Vec3 tan = new Vec3(-Math.sin(a), 0.0, Math.cos(a)).scale((double)e.spinDir());
                  Fx.beam(vc, m, c.subtract(tan.scale(0.9)), c.add(tan.scale(0.6)), 0.35F * f, k(CRIMSON, 1.3F * f), k(GOLD, 1.3F * f), 0.5F, 0.0F);

                  for (int s = 1; s <= 5; s++) {
                     double a2 = a - 0.12 * (double)s * (double)e.spinDir();
                     Vec3 c2 = new Vec3(Math.cos(a2) * 2.3, 0.45, Math.sin(a2) * 2.3);
                     double a3 = a - 0.12 * (double)(s - 1) * (double)e.spinDir();
                     Vec3 c3 = new Vec3(Math.cos(a3) * 2.3, 0.45, Math.sin(a3) * 2.3);
                     Fx.beam(
                        vc, m, c3, c2, 0.3F * f, k(CRIMSON, 0.6F * f * (1.0F - (float)s / 6.0F)), k(CRIMSON, 0.5F * f * (1.0F - (float)s / 6.0F)), 0.5F, 0.0F
                     );
                  }
               }

               vc = Fx.buf(buf, Fx.WAVE);
               Fx.ring(vc, m, 0.08F, 2.0F, 2.6F, 48, BLACK, k(CRIMSON, 0.7F * f), 4.0F, t * 0.05F);
               break;
            case 5:
               if (e.swordTime >= e.clientAbilityStart) {
                  double sx = e.swordX - ex;
                  double sy = e.swordY - ey;
                  double sz = e.swordZ - ez;
                  float form = easeOut(at / 26.0F);
                  float tip;
                  if (at < 26.0F) {
                     tip = 15.0F;
                  } else if (at < 36.0F) {
                     float k = (at - 26.0F) / 10.0F;
                     tip = 15.0F * (1.0F - k * k);
                  } else {
                     tip = 0.0F;
                  }

                  if (at < 38.0F) {
                     sword(buf, pose, cam, sx, sy + (double)tip - 0.5, sz, 9.0F, form);
                     vc = Fx.buf(buf, Fx.BOLT);

                     for (int i = 0; i < 3; i++) {
                        double a = (double)t * 0.3 + (double)i * 2.1;
                        Vec3 p = new Vec3(sx + Math.cos(a) * 1.4, sy + (double)tip + 2.0 + (double)(i * 2), sz + Math.sin(a) * 1.4);
                        Fx.beamV(vc, m, p, new Vec3(sx, sy + (double)tip + 1.0 + (double)i * 2.5, sz), 0.7F * form, k(VOLT, form), 0.4F, t * 0.6F + (float)i);
                     }

                     vc = Fx.buf(buf, Fx.WAVE);
                     pose.pushPose();
                     pose.translate(sx, sy + 0.1, sz);
                     Fx.ring(vc, pose.last().pose(), 0.0F, 2.6F - form, 3.0F, 48, BLACK, k(CRIMSON, 1.2F * form), 5.0F, t * 0.04F);
                     pose.popPose();
                  }
               }
         }
      }
   }

   private static void swordImpact(
      VariaresEntity e, float hit, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam
   ) {
      float f = 1.0F - hit / 40.0F;
      double sx = e.swordX - ex;
      double sy = e.swordY - ey;
      double sz = e.swordZ - ez;
      sword(buf, pose, cam, sx, sy - 1.5, sz, 9.0F, f);
      pose.pushPose();
      pose.translate(sx, sy + 0.1, sz);
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
      float r = 0.5F + easeOut(hit / 14.0F) * 9.0F;
      Fx.ring(vc, m, 0.05F, r - 2.0F, r, 72, BLACK, k(CRIMSON, 1.6F * f), 6.0F, 0.0F);
      Fx.ring(vc, m, 0.08F, r * 0.6F - 1.0F, r * 0.6F, 72, BLACK, k(VOLT, 1.2F * f), 6.0F, 0.3F);
      vc = Fx.buf(buf, Fx.BOLT);
      float len = Math.min(1.0F, hit / 4.0F) * 6.5F;

      for (int i = 0; i < 8; i++) {
         double a = (double)i * Math.PI / 4.0 + 0.3;
         Fx.beamV(
            vc,
            m,
            new Vec3(0.0, 0.05, 0.0),
            new Vec3(Math.cos(a) * (double)len, 0.05, Math.sin(a) * (double)len),
            0.8F,
            k(VOLT, 1.3F * f),
            0.4F,
            (float)i * 0.3F
         );
      }

      if (hit < 12.0F) {
         float g = 1.0F - hit / 12.0F;
         vc = Fx.buf(buf, Fx.FLARE);
         Fx.billboard(vc, pose, cam, 0.0, 1.2, 0.0, 13.0F * g, t * 2.0F, k(new float[]{1.0F, 0.6F, 0.4F}, 1.3F * g));
      }

      pose.popPose();
   }
}
