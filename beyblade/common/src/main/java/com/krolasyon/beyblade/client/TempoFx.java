package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.entity.TempoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class TempoFx {
   public static final ResourceLocation CLOCK = V.rl("librabeyblade", "textures/fx/clock.png");
   private static final float[] SILVER = new float[]{0.75F, 0.85F, 1.0F};
   private static final float[] ICE = new float[]{0.45F, 0.75F, 1.0F};
   private static final float[] GOLD = new float[]{1.0F, 0.85F, 0.45F};
   private static final float[] BLACK = new float[]{0.0F, 0.0F, 0.0F};

   private TempoFx() {
   }

   private static float[] k(float[] c, float k) {
      return new float[]{c[0] * k, c[1] * k, c[2] * k};
   }

   private static float easeOut(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return 1.0F - (1.0F - x) * (1.0F - x) * (1.0F - x);
   }

   private static void dial(MultiBufferSource buf, PoseStack pose, float y, float r, float rot, float[] col) {
      pose.pushPose();
      pose.mulPose(Axis.YP.rotation(-rot));
      VertexConsumer vc = Fx.buf(buf, CLOCK);
      Fx.groundQuad(vc, pose.last().pose(), y, r, col);
      pose.popPose();
   }

   private static void hand(VertexConsumer vc, Matrix4f m, double angle, float len, float width, float y, float[] col) {
      Vec3 a = new Vec3(0.0, (double)y, 0.0);
      Vec3 b = new Vec3(Math.cos(angle) * (double)len, (double)y, Math.sin(angle) * (double)len);
      Fx.beam(vc, m, a, b, width, col, k(col, 0.5F), 0.4F, 0.0F);
      Vec3 tip = new Vec3(Math.cos(angle) * ((double)len + 0.6), (double)y, Math.sin(angle) * ((double)len + 0.6));
      Fx.beam(vc, m, b.subtract(new Vec3(-Math.sin(angle), 0.0, Math.cos(angle)).scale((double)width)), tip, width * 0.8F, col, col, 0.4F, 0.0F);
      Fx.beam(vc, m, b.add(new Vec3(-Math.sin(angle), 0.0, Math.cos(angle)).scale((double)width)), tip, width * 0.8F, col, col, 0.4F, 0.0F);
   }

   public static void render(TempoEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      Matrix4f m = pose.last().pose();
      if (e.isShielded()) {
         float s = 1.0F;
         float pulse = 0.75F + 0.25F * Mth.sin(t * 0.3F);
         dial(buf, pose, 0.06F, 9.0F, 0.0F, k(SILVER, 0.75F * pulse));
         VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
         double tick = Math.floor((double)(t / 20.0F)) * Math.PI / 30.0;
         hand(vc, m, (-Math.PI / 2) + tick, 7.65F, 0.25F, 0.1F, k(ICE, 0.9F));
         hand(vc, m, Math.PI / 3, 4.9500003F, 0.4F, 0.11F, k(ICE, 0.9F));
         vc = Fx.bufT(buf, Fx.HEX, 0.12F * pulse * s);
         Fx.band(vc, m, 9.0F, 0.0F, 9.0F, 3.0F, 64, ICE, ICE, 8.0F, 0.0F, 2.0F, 0.0F);
         Fx.alpha = 1.0F;

         for (Entity o : e.enemiesWithin(9.0)) {
            pose.pushPose();
            pose.translate(
               Mth.lerp((double)partial, o.xo, o.getX()) - ex,
               Mth.lerp((double)partial, o.yo, o.getY()) - ey,
               Mth.lerp((double)partial, o.zo, o.getZ()) - ez
            );
            dial(buf, pose, 0.08F, 1.1F, 0.0F, k(ICE, 0.9F));
            pose.popPose();
         }
      }

      float rw = t - (float)e.rewindTime;
      int ab = e.clientAbility;
      float at = e.abilityAge(partial);
      if (ab == 2 || rw >= 0.0F && rw < 30.0F) {
         float f = Mth.clamp(1.0F - Math.max(0.0F, rw - 22.0F) / 8.0F, 0.0F, 1.0F);
         dial(buf, pose, 0.08F, 2.6F, t * 0.35F, k(ICE, f));
         VertexConsumer vc = Fx.buf(buf, Fx.WAVE);

         for (int i = 0; i < 3; i++) {
            float ph = (t * 0.08F + (float)i / 3.0F) % 1.0F;
            float r = 3.2F * (1.0F - ph);
            Fx.ring(vc, m, 0.12F, Math.max(0.0F, r - 0.5F), r, 40, BLACK, k(ICE, f * ph), 3.0F, 0.0F);
         }
      }

      float tl = t - (float)e.tollTime;
      if (tl >= 0.0F && tl < 30.0F) {
         toll(e, tl, t, pose, buf, cam);
      }

      if (ab != 0 && !(at < 0.0F)) {
         switch (ab) {
            case 3: {
               float f = Mth.clamp(at / 6.0F, 0.0F, 1.0F) * (at > 48.0F ? Math.max(0.0F, 1.0F - (at - 48.0F) / 4.0F) : 1.0F);
               dial(buf, pose, 0.06F, 8.3F, 0.0F, k(SILVER, 0.8F * f));
               VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
               hand(vc, m, TempoEntity.minuteAngle(at), 7.5F, 0.55F, 0.35F, k(GOLD, 1.2F * f));
               hand(vc, m, TempoEntity.hourAngle(at), 5.5F, 0.85F, 0.3F, k(SILVER, 1.2F * f));
               vc = Fx.buf(buf, Fx.WISP);
               double a0 = TempoEntity.minuteAngle(at);

               for (int i = 0; i < 10; i++) {
                  double a1 = a0 - 0.08 * (double)(i + 1);
                  double a2 = a0 - 0.08 * (double)i;
                  float g = (1.0F - (float)i / 10.0F) * 0.6F * f;
                  Fx.quad(
                     vc,
                     m,
                     new float[]{0.0F, 0.3F, 0.0F},
                     new float[]{(float)(Math.cos(a2) * 7.5), 0.3F, (float)(Math.sin(a2) * 7.5)},
                     new float[]{(float)(Math.cos(a1) * 7.5), 0.3F, (float)(Math.sin(a1) * 7.5)},
                     new float[]{0.0F, 0.3F, 0.0F},
                     0.0F,
                     0.0F,
                     1.0F,
                     1.0F,
                     k(GOLD, g * 0.3F),
                     k(GOLD, g)
                  );
               }

               VertexConsumer fl = Fx.buf(buf, Fx.FLARE);
               Fx.billboard(fl, pose, cam, 0.0, 0.4, 0.0, 1.6F * f, t * 6.0F, k(SILVER, f));
               break;
            }
            case 4: {
               float f = Mth.clamp(at / 4.0F, 0.0F, 1.0F);
               dial(buf, pose, 0.07F, 1.6F, -t * 0.9F, k(GOLD, f));
               VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
               hand(vc, m, (double)(t * 0.9F), 1.5F, 0.15F, 0.12F, k(GOLD, f));
               break;
            }
            case 5:
               clock(e, at, t, pose, buf, cam);
         }
      }
   }

   private static void clock(TempoEntity e, float at, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      float f = easeOut((at - 2.0F) / 14.0F) * (at > 92.0F ? Math.max(0.0F, 1.0F - (at - 92.0F) / 20.0F) : 1.0F);
      if (!(f <= 0.01F)) {
         float R = 10.200001F * f;
         float y = 7.0F;
         dial(buf, pose, y, R, 0.0F, k(SILVER, 1.1F * f));
         dial(buf, pose, 0.06F, R, 0.0F, k(ICE, 0.35F * f));
         Matrix4f m = pose.last().pose();
         VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
         float p = Mth.clamp((at - 14.0F) / 78.0F, 0.0F, 1.0F);
         double minute = (-Math.PI / 2) + (double)(1.0F - p) * Math.PI * 6.0;
         double hour = (-Math.PI / 2) + (double)(1.0F - p) * Math.PI * 1.5;
         pose.pushPose();
         pose.translate(0.0, (double)y + 0.05, 0.0);
         Matrix4f hm = pose.last().pose();
         hand(vc, hm, minute, R * 0.85F, 0.6F, 0.0F, k(GOLD, 1.2F * f));
         hand(vc, hm, hour, R * 0.55F, 1.0F, 0.02F, k(SILVER, 1.2F * f));
         pose.popPose();
         if (at >= 14.0F && at < 92.0F) {
            vc = Fx.buf(buf, Fx.WISP);

            for (Entity o : e.enemiesWithin(12.0)) {
               Vec3 rel = o.position().subtract(e.position());
               Fx.beam(
                  vc,
                  m,
                  new Vec3(rel.x * 0.4, (double)y, rel.z * 0.4),
                  rel.add(0.0, 0.4, 0.0),
                  0.5F,
                  k(ICE, 0.7F * f),
                  k(SILVER, 0.3F * f),
                  0.5F,
                  -t * 0.2F
               );
            }

            vc = Fx.buf(buf, Fx.WAVE);
            float r = R * (1.0F - t * 0.04F % 1.0F);
            Fx.ring(vc, m, 0.1F, Math.max(0.0F, r - 1.2F), r, 64, BLACK, k(ICE, 0.8F * f), 6.0F, 0.0F);
         }
      }
   }

   private static void toll(TempoEntity e, float tl, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      float f = 1.0F - tl / 30.0F;
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
      float r = 0.5F + easeOut(tl / 14.0F) * 12.0F * 1.2F;
      Fx.ring(vc, m, 0.1F, r - 2.0F, r, 72, BLACK, k(SILVER, 1.6F * f), 8.0F, 0.0F);
      if (tl < 12.0F) {
         float g = 1.0F - tl / 12.0F;
         vc = Fx.buf(buf, Fx.SOFT);

         for (Entity o : e.enemiesWithin(14.0)) {
            Vec3 rel = o.position().subtract(e.position());
            Fx.beam(vc, m, rel.add(0.0, 7.0, 0.0), rel, 1.4F * g, k(SILVER, 1.4F * g), k(ICE, g), 0.3F, 0.0F);
         }

         vc = Fx.buf(buf, Fx.FLARE);
         Fx.billboard(vc, pose, cam, 0.0, 7.0, 0.0, 14.0F * g, t * 2.0F, k(SILVER, g));
      }
   }
}
