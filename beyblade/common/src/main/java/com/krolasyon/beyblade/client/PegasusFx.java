package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.entity.PegasusEntity;
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

public final class PegasusFx {
   public static final ResourceLocation PEGASUS = V.rl("librabeyblade", "textures/fx/pegasus.png");
   public static final ResourceLocation STAR = V.rl("librabeyblade", "textures/fx/star.png");
   private static final float[] SKY = new float[]{0.4F, 0.75F, 1.0F};
   private static final float[] WHITE = new float[]{1.0F, 1.0F, 1.0F};
   private static final float[] GOLD = new float[]{1.0F, 0.85F, 0.4F};
   private static final float[] BLACK = new float[]{0.0F, 0.0F, 0.0F};

   private PegasusFx() {
   }

   private static float[] k(float[] c, float k) {
      return new float[]{c[0] * k, c[1] * k, c[2] * k};
   }

   private static float easeOut(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return 1.0F - (1.0F - x) * (1.0F - x) * (1.0F - x);
   }

   private static void spirit(MultiBufferSource buf, PoseStack pose, Quaternionf cam, double x, double y, double z, float size, float strength, boolean flip) {
      float a = Mth.clamp(strength, 0.0F, 1.0F);
      if (!(a <= 0.01F) && !(size <= 0.01F)) {
         pose.pushPose();
         pose.translate(x, y, z);
         pose.mulPose(cam);
         if (flip) {
            pose.scale(-1.0F, 1.0F, 1.0F);
         }

         VertexConsumer vc = Fx.bufT(buf, PEGASUS, 0.88F * a);
         Fx.billboard(vc, pose, new Quaternionf(), 0.0, 0.0, 0.0, size, 0.0F, new float[]{0.2F, 0.55F, 1.0F});
         Fx.alpha = 1.0F;
         vc = Fx.buf(buf, PEGASUS);
         Fx.billboard(vc, pose, new Quaternionf(), 0.0, 0.0, 0.0, size * 1.05F, 0.0F, k(new float[]{0.7F, 0.9F, 1.0F}, 0.35F * a));
         pose.popPose();
      }
   }

   public static void render(PegasusEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      if (e.isShielded()) {
         wings(e, t, ex, ey, ez, pose, buf);
      }

      float st = t - (float)e.starTime;
      float nv = t - (float)e.novaTime;
      int ab = e.clientAbility;
      float at = e.abilityAge(partial);
      if (nv >= 0.0F && nv < 24.0F) {
         nova(e, nv, t, ex, ey, ez, pose, buf, cam, ab == 5 || ab == 0 && e.starTime < e.novaTime - 60);
      }

      float ra = t - (float)e.ricoTime;
      if (ra >= 0.0F && ra < 8.0F) {
         VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
         float f = 1.0F - ra / 8.0F;
         Fx.ring(vc, pose.last().pose(), 0.3F, 0.2F + ra * 0.25F, 0.6F + ra * 0.3F, 32, BLACK, k(SKY, 1.3F * f), 3.0F, 0.0F);
      }

      if (ab != 0 && !(at < 0.0F)) {
         switch (ab) {
            case 2:
               storm(e, at, t, partial, ex, ey, ez, pose, buf, cam);
               break;
            case 3:
               star(e, at, t, ex, ey, ez, pose, buf, cam);
               break;
            case 4:
               float g = Mth.clamp(at / 4.0F, 0.0F, 1.0F)
                  * (at > (float)(PegasusEntity.PG_DURATION[4] - 6) ? Math.max(0.0F, ((float)PegasusEntity.PG_DURATION[4] - at) / 6.0F) : 1.0F);
               double vx = e.getX() - e.xo;
               spirit(buf, pose, cam, 0.0, 0.9, 0.0, 3.0F, g, vx > 0.01);
               VertexConsumer vc = Fx.buf(buf, Fx.FLARE);
               Fx.billboard(vc, pose, cam, 0.0, 0.4, 0.0, 2.0F * g, t * 8.0F, k(SKY, g));
               break;
            case 5:
               gallop(e, at, t, ex, ey, ez, pose, buf, cam);
         }
      }
   }

   private static void wings(PegasusEntity e, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf) {
      int rem = e.shieldRemaining();
      float fade = rem < 14 ? (float)rem / 14.0F : 1.0F;
      float grow = easeOut((float)(200 - rem) / 8.0F);
      double vx = e.getX() - e.xo;
      double vz = e.getZ() - e.zo;
      Vec3 fwd = vx * vx + vz * vz > 1.0E-4 ? new Vec3(vx, 0.0, vz).normalize() : new Vec3(0.0, 0.0, -1.0);
      Vec3 side = new Vec3(-fwd.z, 0.0, fwd.x);
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, EagleFx.FEATHER);
      float flap = Mth.sin(t * 0.55F);

      for (int s = -1; s <= 1; s += 2) {
         for (int i = 0; i < 6; i++) {
            double spread = Math.toRadians((double)((float)(-10 + i * 16) + flap * 12.0F));
            Vec3 dir = side.scale((double)s * Math.cos(spread)).add(0.0, Math.sin(spread) + 0.25, 0.0).add(fwd.scale(-0.45)).normalize();
            float len = (1.5F - (float)i * 0.1F) * grow;
            Vec3 c = side.scale((double)s * 0.4).add(0.0, 0.6, 0.0).add(dir.scale((double)(len / 2.0F) + 0.1));
            EagleFx.feather(vc, m, c, dir, len, 0.38F, k(new float[]{0.55F, 0.85F, 1.0F}, 1.1F * fade));
         }
      }

      vc = Fx.buf(buf, STAR);

      for (int i = 1; i < 6; i++) {
         float f = 1.0F - (float)i / 6.0F;
         Fx.billboard(
            vc,
            pose,
            PegasusFx.Minecraft.cam(),
            e.trailX[i] - ex,
            e.trailY[i] - ey + 0.5,
            e.trailZ[i] - ez,
            0.6F * f,
            t * 10.0F + (float)i * 40.0F,
            k(WHITE, 0.8F * f * fade)
         );
      }
   }

   private static void storm(
      PegasusEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam
   ) {
      float dur = (float)PegasusEntity.PG_DURATION[2];
      float f = easeOut(at / 14.0F) * (at > dur - 10.0F ? Math.max(0.0F, (dur - at) / 10.0F) : 1.0F);
      // Storm Bringer's tornado around the opponent (or around Pegasus when it has none)
      if (at > 6.0F && at < 82.0F) {
         net.minecraft.world.entity.Entity tg = e.level().getEntity(e.getTargetId());
         double fx = (tg != null ? Mth.lerp(partial, tg.xo, tg.getX()) : Mth.lerp(partial, e.xo, e.getX())) - ex;
         double fz = (tg != null ? Mth.lerp(partial, tg.zo, tg.getZ()) : Mth.lerp(partial, e.zo, e.getZ())) - ez;
         double fy = (tg != null ? tg.getY() : e.getY()) - ey;
         float g = Mth.clamp((at - 6.0F) / 14.0F, 0.0F, 1.0F) * Mth.clamp((82.0F - at) / 6.0F, 0.0F, 1.0F);
         TornadoFx.draw(buf, pose, fx, fy - 0.2, fz, 1.2F + 1.5F * g, 4.5F * g + 1.0F, 12.0F * g, t, new float[]{0.65F, 0.85F, 1.0F}, g);
      }
      double bx = e.abilityX - ex;
      double by = e.abilityY - ey;
      double bz = e.abilityZ - ez;
      double cy = by + 9.0;
      float R = 11.05F * f;

      for (int l = 0; l < 2; l++) {
         pose.pushPose();
         pose.translate(bx, cy + (double)l * 0.4, bz);
         pose.mulPose(Axis.YP.rotationDegrees((float)(l == 0 ? 1 : -1) * t * 1.5F));
         VertexConsumer vc = Fx.bufT(buf, Fx.WISP, 0.85F * f);
         Fx.ring(
            vc,
            pose.last().pose(),
            0.0F,
            0.0F,
            R * (1.0F - (float)l * 0.15F),
            40,
            new float[]{0.18F, 0.2F, 0.28F},
            new float[]{0.35F, 0.38F, 0.48F},
            3.0F,
            (float)l * 0.5F
         );
         pose.popPose();
      }

      Fx.alpha = 1.0F;
      Matrix4f m = pose.last().pose();

      for (int k = 0; k < 16; k++) {
         float sa = at - (float)(14 + k * 5);
         if (!(sa < -1.0F) && !(sa > 5.0F)) {
            Entity tgt = e.strikeTarget(k);
            Vec3 tp = tgt != null
               ? new Vec3(
                  Mth.lerp((double)partial, tgt.xo, tgt.getX()) - ex,
                  Mth.lerp((double)partial, tgt.yo, tgt.getY()) - ey,
                  Mth.lerp((double)partial, tgt.zo, tgt.getZ()) - ez
               )
               : PegasusEntity.strikePoint(k, e.abilityX, e.abilityY, e.abilityZ).subtract(ex, ey, ez);
            float g = sa < 0.0F ? 0.4F : Math.max(0.0F, 1.0F - sa / 5.0F);
            Vec3 top = new Vec3(tp.x + Math.sin((double)k * 1.7) * 1.5, cy, tp.z + Math.cos((double)k * 1.3) * 1.5);
            Vec3 a1 = top.add(tp.subtract(top).scale(0.35)).add(Math.sin((double)k * 3.1) * 0.8, 0.0, Math.cos((double)k * 2.3) * 0.8);
            Vec3 a2 = top.add(tp.subtract(top).scale(0.7)).add(Math.cos((double)k * 1.9) * 0.6, 0.0, Math.sin((double)k * 2.9) * 0.6);
            VertexConsumer vc = Fx.buf(buf, Fx.BOLT);

            for (Vec3[] seg : new Vec3[][]{{top, a1}, {a1, a2}, {a2, tp.add(0.0, 0.2, 0.0)}}) {
               Fx.beamV(vc, m, seg[0], seg[1], 1.1F, k(new float[]{0.75F, 0.9F, 1.0F}, 1.4F * g), 0.5F, t * 0.7F + (float)k);
            }

            vc = Fx.buf(buf, Fx.SOFT);
            Fx.beam(vc, m, top, tp, 0.25F, k(WHITE, 0.6F * g), k(WHITE, 1.0F * g), 0.2F, 0.0F);
            if (sa >= 0.0F) {
               vc = Fx.buf(buf, Fx.FLARE);
               Fx.billboard(vc, pose, cam, tp.x, tp.y + 0.3, tp.z, 3.2F * g, (float)k * 30.0F, k(new float[]{0.8F, 0.9F, 1.0F}, 1.2F * g));
               vc = Fx.buf(buf, Fx.WAVE);
               pose.pushPose();
               pose.translate(tp.x, tp.y, tp.z);
               float r = 0.3F + sa * 0.45F;
               Fx.ring(vc, pose.last().pose(), 0.06F, r - 0.6F, r, 32, BLACK, k(SKY, 1.2F * g), 3.0F, 0.0F);
               pose.popPose();
               pose.pushPose();
               pose.translate(top.x, top.y, top.z);
               vc = Fx.buf(buf, Fx.FLARE);
               Fx.billboard(vc, pose, cam, 0.0, 0.0, 0.0, 6.0F * g, 0.0F, k(new float[]{0.6F, 0.75F, 1.0F}, 0.8F * g));
               pose.popPose();
            }
         }
      }
   }

   private static Vec3 starPoint(int i, float r) {
      double a = Math.toRadians((double)(-90 + i * 72));
      return new Vec3(Math.cos(a) * (double)r, 0.6, Math.sin(a) * (double)r);
   }

   private static void star(PegasusEntity e, float at, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      // the bey itself: Pegasus' spirit spreads its wings above it in the sky, and it falls as a shooting star
      if (at > 4.0F && at < 62.0F) {
         float g = Mth.clamp((at - 4.0F) / 8.0F, 0.0F, 1.0F) * Mth.clamp((62.0F - at) / 6.0F, 0.0F, 1.0F);
         spirit(buf, pose, cam, 0.0, 1.4, 0.0, 4.5F, g, false);
         VertexConsumer fl = Fx.buf(buf, Fx.FLARE);
         Fx.billboard(fl, pose, cam, 0.0, 0.4, 0.0, 2.6F * g, t * 6.0F, k(SKY, 1.2F * g));
         double vy = e.getY() - e.yo;
         double vx = e.getX() - e.xo;
         double vz = e.getZ() - e.zo;
         double sp = Math.sqrt(vx * vx + vy * vy + vz * vz);
         if (sp > 0.4) {
            // comet tail along the flight path
            VertexConsumer tv = Fx.buf(buf, Fx.SOFT);
            Vec3 back = new Vec3(-vx, -vy, -vz).normalize().scale(Math.min(14.0, sp * 7.0));
            Fx.beam(tv, pose.last().pose(), new Vec3(0.0, 0.4, 0.0), back.add(0.0, 0.4, 0.0), 1.3F, k(WHITE, 1.4F), k(SKY, 0.0F), 0.3F, t * 0.2F);
            Fx.beam(tv, pose.last().pose(), new Vec3(0.0, 0.4, 0.0), back.scale(0.6).add(0.0, 0.4, 0.0), 2.4F, k(SKY, 0.9F), k(SKY, 0.0F), 0.3F, t * 0.1F);
         }
      }

      if (!(at > 46.0F)) {
         pose.pushPose();
         pose.translate(e.starX - ex, e.starY - ey, e.starZ - ez);
         Matrix4f m = pose.last().pose();
         float collapse = at > 40.0F ? easeOut((at - 40.0F) / 6.0F) : 0.0F;
         float r = 4.0F * (1.0F - collapse);
         VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
         Fx.ring(vc, m, 0.06F, r - 0.6F, r, 64, BLACK, k(GOLD, 0.9F), 5.0F, t * 0.02F);
         if (at >= 24.0F) {
            float lf = Mth.clamp((at - 24.0F) / 6.0F, 0.0F, 1.0F);
            vc = Fx.buf(buf, Fx.SOFT);

            for (int i = 0; i < 5; i++) {
               Vec3 a = starPoint(i, r);
               Vec3 b = starPoint((i + 2) % 5, r);
               Fx.beam(vc, m, a, a.add(b.subtract(a).scale((double)lf)), 0.3F, k(GOLD, 1.1F), k(WHITE, 1.1F), 0.5F, t * 0.3F);
            }

            vc = Fx.buf(buf, Fx.SOFT);
            Fx.ring(vc, m, 0.05F, 0.0F, r, 48, k(GOLD, 0.35F * lf), BLACK, 2.0F, 0.0F);
         }

         vc = Fx.buf(buf, STAR);

         for (int i = 0; i < 5; i++) {
            float sa = at - (float)(6 + i * 4);
            if (!(sa < 0.0F)) {
               float g = easeOut(sa / 4.0F);
               Vec3 p = starPoint(i, r);
               Fx.billboard(
                  vc,
                  pose,
                  cam,
                  p.x,
                  p.y + 0.2 * (double)Mth.sin(t * 0.3F + (float)i),
                  p.z,
                  1.5F * g * (1.0F + 0.15F * Mth.sin(t * 0.8F + (float)i)),
                  t * 3.0F + (float)i * 72.0F,
                  k(new float[]{1.0F, 0.95F, 0.7F}, 1.2F)
               );
            }
         }

         pose.popPose();
      }
   }

   private static void nova(
      PegasusEntity e, float nv, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam, boolean atSelf
   ) {
      float f = 1.0F - nv / 24.0F;
      pose.pushPose();
      if (!atSelf) {
         pose.translate(e.starX - ex, e.starY - ey, e.starZ - ez);
      }

      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
      float r = 0.5F + easeOut(nv / 12.0F) * 8.0F;
      Fx.ring(vc, m, 0.08F, r - 1.8F, r, 64, BLACK, k(GOLD, 1.5F * f), 6.0F, 0.0F);
      Fx.ring(vc, m, 0.1F, r * 0.6F - 1.0F, r * 0.6F, 64, BLACK, k(SKY, 1.2F * f), 6.0F, 0.3F);
      vc = Fx.buf(buf, STAR);
      Fx.billboard(vc, pose, cam, 0.0, 1.2, 0.0, 7.0F * easeOut(nv / 5.0F) * f + 0.5F, t * 4.0F, k(WHITE, 1.3F * f));
      vc = Fx.buf(buf, Fx.FLARE);
      Fx.billboard(vc, pose, cam, 0.0, 0.8, 0.0, 9.0F * f, t * 2.0F, k(new float[]{0.8F, 0.9F, 1.0F}, f));
      pose.popPose();
   }

   private static void gallop(PegasusEntity e, float at, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      double[] ring = PegasusEntity.boosterRing(e.level(), e.abilityX, e.abilityZ);
      float br = (float)ring[2];
      double vx = e.getX() - e.xo;
      double vy = e.getY() - e.yo;
      double vz = e.getZ() - e.zo;
      if (at < 54.0F) {
         float g = Mth.clamp(at / 8.0F, 0.0F, 1.0F) * (at > 48.0F ? Math.max(0.0F, 1.0F - (at - 48.0F) / 6.0F) : 1.0F);
         pose.pushPose();
         pose.translate(ring[0] - ex, e.abilityY - ey + 0.12, ring[1] - ez);
         Matrix4f m = pose.last().pose();
         VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
         Fx.ring(vc, m, 0.02F, br - 1.2F, br + 0.5F, 72, BLACK, k(SKY, 1.0F * g), 6.0F, -t * 0.08F);
         vc = Fx.buf(buf, Fx.SOFT);

         for (int i = 0; i < 4; i++) {
            Fx.helix(
               vc,
               m,
               0.05F,
               0.3F,
               br,
               0.4F,
               -t * 0.25F * (float)e.spinDir() + (float)i * (float) (Math.PI / 2),
               0.9F * (float)e.spinDir(),
               0.35F,
               30,
               k(SKY, 0.8F * g),
               k(WHITE, 0.3F * g),
               t * 0.1F
            );
         }

         pose.popPose();
         spirit(buf, pose, cam, 0.0, 1.4, 0.0, 3.6F, g, vx > 0.01);
         vc = Fx.buf(buf, Fx.FLARE);
         Fx.billboard(vc, pose, cam, 0.0, 0.35, 0.0, 2.2F * g, t * 9.0F, k(SKY, g));
      }

      if (at >= 48.0F && at < 90.0F && e.novaTime < e.clientAbilityStart) {
         float g = Mth.clamp((at - 48.0F) / 6.0F, 0.0F, 1.0F);
         spirit(buf, pose, cam, 0.0, 1.2, 0.0, 6.5F * g, g, vx > 0.01);
         VertexConsumer vc = Fx.buf(buf, STAR);
         Fx.billboard(vc, pose, cam, 0.0, 0.4, 0.0, 3.5F * g + 0.6F * Mth.sin(t * 1.3F), t * 6.0F, k(GOLD, 1.2F * g));
         vc = Fx.buf(buf, Fx.FLARE);
         Fx.billboard(vc, pose, cam, 0.0, 0.4, 0.0, 4.5F * g, -t * 4.0F, k(SKY, g));
         Vec3 vel = new Vec3(vx, vy, vz);
         if (at >= 68.0F && vel.lengthSqr() > 0.001) {
            Vec3 tail = vel.normalize().scale(-8.0);
            vc = Fx.bufT(buf, Fx.WISP, 0.8F);
            Fx.beam(
               vc,
               pose.last().pose(),
               new Vec3(0.0, 0.35, 0.0),
               tail,
               2.0F,
               new float[]{0.6F, 0.85F, 1.0F},
               new float[]{0.1F, 0.3F, 0.9F},
               0.3F,
               t * 0.5F
            );
            Fx.alpha = 1.0F;
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.beam(vc, pose.last().pose(), new Vec3(0.0, 0.35, 0.0), tail.scale(0.6), 0.8F, k(WHITE, 1.1F), BLACK, 0.5F, t);
         }
      }
   }

   private static final class Minecraft {
      static Quaternionf cam() {
         return net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
      }
   }
}
