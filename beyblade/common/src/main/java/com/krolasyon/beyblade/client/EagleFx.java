package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.entity.EagleEntity;
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

public final class EagleFx {
   public static final ResourceLocation EAGLE = V.rl("librabeyblade", "textures/fx/eagle.png");
   public static final ResourceLocation FEATHER = V.rl("librabeyblade", "textures/fx/feather.png");
   public static final ResourceLocation CLAW = V.rl("librabeyblade", "textures/fx/claw.png");
   private static final float[] VIOLET = new float[]{0.65F, 0.35F, 1.0F};
   private static final float[] SILVER = new float[]{0.85F, 0.85F, 1.0F};
   private static final float[] GOLD = new float[]{1.0F, 0.8F, 0.3F};
   private static final float[] BLACK = new float[]{0.0F, 0.0F, 0.0F};

   private EagleFx() {
   }

   private static float[] k(float[] c, float k) {
      return new float[]{c[0] * k, c[1] * k, c[2] * k};
   }

   private static float easeOut(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return 1.0F - (1.0F - x) * (1.0F - x) * (1.0F - x);
   }

   private static void eagle(MultiBufferSource buf, PoseStack pose, Quaternionf cam, double x, double y, double z, float size, float strength, float flap) {
      float a = Mth.clamp(strength, 0.0F, 1.0F);
      if (!(a <= 0.01F) && !(size <= 0.01F)) {
         pose.pushPose();
         pose.translate(x, y, z);
         pose.mulPose(cam);
         pose.scale(1.0F + 0.12F * flap, 1.0F - 0.18F * flap, 1.0F);
         VertexConsumer vc = Fx.bufT(buf, EAGLE, 0.88F * a);
         Fx.billboard(vc, pose, new Quaternionf(), 0.0, 0.0, 0.0, size, 0.0F, new float[]{0.36F, 0.12F, 0.78F});
         Fx.alpha = 1.0F;
         vc = Fx.buf(buf, EAGLE);
         Fx.billboard(vc, pose, new Quaternionf(), 0.0, 0.0, 0.0, size * 1.05F, 0.0F, k(new float[]{0.55F, 0.35F, 1.0F}, 0.3F * a));
         pose.popPose();
      }
   }

   static void feather(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 dir, float len, float w, float[] col) {
      Vec3 d = dir.normalize();
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      Vec3 side = d.cross(up);
      if (side.lengthSqr() < 1.0E-4) {
         side = new Vec3(1.0, 0.0, 0.0);
      }

      side = side.normalize();
      Vec3 tip = c.add(d.scale((double)(len / 2.0F)));
      Vec3 back = c.subtract(d.scale((double)(len / 2.0F)));

      for (Vec3 s : new Vec3[]{side.scale((double)(w / 2.0F)), up.scale((double)(w / 2.0F))}) {
         Fx.quad(vc, m, f(tip.subtract(s)), f(back.subtract(s)), f(back.add(s)), f(tip.add(s)), 0.0F, 0.0F, 1.0F, 1.0F, col, col);
      }
   }

   private static float[] f(Vec3 v) {
      return new float[]{(float)v.x, (float)v.y, (float)v.z};
   }

   public static void render(EagleEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      if (e.isShielded()) {
         eye(e, t, pose, buf, cam);
      }

      float da = t - (float)e.dodgeTime;
      if (da >= 0.0F && da < 10.0F) {
         dodge(e, da, ex, ey, ez, pose, buf);
      }

      float sa = t - (float)e.slamTime;
      if (sa >= 0.0F && sa < 20.0F) {
         slam(e, sa, t, partial, ex, ey, ez, pose, buf, cam);
      }

      float sc = t - (float)e.screechTime;
      if (sc >= 0.0F && sc < 24.0F) {
         screech(sc, pose, buf, cam);
      }

      int ab = e.clientAbility;
      float at = e.abilityAge(partial);
      if (ab != 0 && !(at < 0.0F)) {
         switch (ab) {
            case 2:
               featherStorm(at, t, pose, buf, cam);
               break;
            case 3:
               talon(e, at, t, pose, buf, cam);
               break;
            case 4:
               wings(at, t, pose, buf);
               break;
            case 5:
               sky(e, at, t, partial, ex, ey, ez, pose, buf, cam);
         }
      }
   }

   private static void eye(EagleEntity e, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      int rem = e.shieldRemaining();
      float fade = rem < 14 ? (float)rem / 14.0F : 1.0F;
      float grow = easeOut((float)(200 - rem) / 8.0F);
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, FEATHER);

      for (int i = 0; i < 6; i++) {
         double a = (double)i * Math.PI / 3.0 + (double)t * 0.12;
         Vec3 c = new Vec3(Math.cos(a) * 0.95 * (double)grow, 0.55 + 0.1 * Math.sin((double)t * 0.3 + (double)i), Math.sin(a) * 0.95 * (double)grow);
         feather(vc, m, c, new Vec3(-Math.sin(a), 0.15, Math.cos(a)), 0.7F, 0.28F, k(SILVER, 0.9F * fade));
      }

      vc = Fx.buf(buf, Fx.SOFT);
      Fx.ring(vc, m, 0.04F, 0.6F, 1.25F, 40, BLACK, k(VIOLET, 0.8F * fade), 3.0F, t * 0.05F);
      eagle(buf, pose, cam, 0.0, 1.7 + 0.1 * (double)Mth.sin(t * 0.2F), 0.0, 1.2F * grow, 0.85F * fade, Mth.sin(t * 0.4F));
   }

   private static void dodge(EagleEntity e, float da, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf) {
      float f = 1.0F - da / 10.0F;
      pose.pushPose();
      pose.translate(e.dodgeX - ex, e.dodgeY - ey, e.dodgeZ - ez);
      pose.scale(0.45F, 0.45F, 0.45F);
      BeyMesh.EAGLE.renderGhostDisc(pose, buf, 15728880, 0.5F * f, BeyMesh.EAGLE.TOP_Y - 0.2F);
      pose.popPose();
      VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
      pose.pushPose();
      pose.translate(e.dodgeX - ex, e.dodgeY - ey, e.dodgeZ - ez);
      Fx.ring(vc, pose.last().pose(), 0.3F, 0.2F + da * 0.12F, 0.5F + da * 0.15F, 32, BLACK, k(VIOLET, 1.2F * f), 3.0F, 0.0F);
      pose.popPose();
   }

   private static void featherStorm(float at, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, FEATHER);
      if (at < 46.0F) {
         int left = 12 - Math.max(0, Math.min(3, (int)((at - 20.0F) / 12.0F) + (at >= 20.0F ? 1 : 0))) * 4;
         float grow = easeOut(at / 10.0F);

         for (int i = 0; i < left; i++) {
            double a = (double)i * Math.PI * 2.0 / 12.0 + (double)t * 0.35;
            Vec3 c = new Vec3(Math.cos(a) * 1.5 * (double)grow, 0.6 + 0.15 * Math.sin(a * 3.0 + (double)t * 0.2), Math.sin(a) * 1.5 * (double)grow);
            feather(vc, m, c, new Vec3(-Math.sin(a), 0.0, Math.cos(a)), 0.9F, 0.32F, k(SILVER, 1.1F));
         }
      }

      for (int b = 0; b < 3; b++) {
         float age = at - (float)(20 + b * 12);
         if (!(age < 0.0F) && !(age >= 11.0F)) {
            float front = 0.8F + age * 0.9F;
            float fade = 1.0F - age / 11.0F;

            for (int i = 0; i < 16; i++) {
               double a = (double)i * Math.PI * 2.0 / 16.0 + (double)b * 0.2;
               Vec3 dir = new Vec3(Math.cos(a), 0.0, Math.sin(a));
               Vec3 c = dir.scale((double)front).add(0.0, 0.55, 0.0);
               feather(vc, m, c, dir, 1.0F, 0.35F, k(new float[]{0.9F, 0.8F, 1.0F}, 1.3F * fade));
            }
         }
      }

      vc = Fx.buf(buf, Fx.WAVE);

      for (int bx = 0; bx < 3; bx++) {
         float age = at - (float)(20 + bx * 12);
         if (!(age < 0.0F) && !(age >= 11.0F)) {
            float front = 0.8F + age * 0.9F;
            Fx.ring(vc, m, 0.55F, front - 1.0F, front, 64, BLACK, k(VIOLET, 0.9F * (1.0F - age / 11.0F)), 6.0F, (float)bx * 0.3F);
         }
      }

      vc = Fx.buf(buf, Fx.FLARE);
      Fx.billboard(vc, pose, cam, 0.0, 0.55, 0.0, 1.6F + 0.4F * Mth.sin(t * 1.5F), t * 6.0F, k(VIOLET, 0.8F));
   }

   private static void talon(EagleEntity e, float at, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      float end = (float)EagleEntity.EA_DURATION[3];
      float g = Mth.clamp(at / 8.0F, 0.0F, 1.0F) * (at > end - 8.0F ? Math.max(0.0F, (end - at) / 8.0F) : 1.0F);
      boolean carrying = t - (float)e.grabTime < 32.0F && e.grabTime > e.clientAbilityStart;
      eagle(buf, pose, cam, 0.0, 0.8, 0.0, 4.2F + (carrying ? 0.6F : 0.0F), g, Mth.sin(t * (carrying ? 0.9F : 0.5F)));
      Matrix4f m = pose.last().pose();
      if (at >= 10.0F) {
         Vec3 vel = new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo);
         if (vel.lengthSqr() > 0.01) {
            Vec3 tail = vel.normalize().scale(-5.0);
            VertexConsumer vc = Fx.buf(buf, Fx.SOFT);

            for (int s = -1; s <= 1; s += 2) {
               Vec3 side = new Vec3(-vel.z, 0.0, vel.x);
               side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize().scale(1.4 * (double)s);
               Fx.beam(vc, m, side.add(0.0, 0.6, 0.0), side.add(tail).add(0.0, 0.6, 0.0), 0.4F, k(SILVER, 0.9F * g), BLACK, 0.5F, t);
            }
         }
      }

      float ga = t - (float)e.grabTime;
      if (ga >= 0.0F && ga < 12.0F) {
         VertexConsumer vc = Fx.buf(buf, CLAW);
         float f = 1.0F - ga / 12.0F;
         Fx.billboard(vc, pose, cam, 0.0, -0.3, 0.0, 2.6F, 90.0F, k(VIOLET, 1.5F * f));
      }
   }

   private static void slam(
      EagleEntity e, float sa, float t, float partial, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam
   ) {
      Entity tgt = e.level().getEntity(e.getTargetId());
      if (tgt != null) {
         float f = 1.0F - sa / 20.0F;
         pose.pushPose();
         pose.translate(
            Mth.lerp((double)partial, tgt.xo, tgt.getX()) - ex,
            Mth.lerp((double)partial, tgt.yo, tgt.getY()) - ey,
            Mth.lerp((double)partial, tgt.zo, tgt.getZ()) - ez
         );
         VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
         Fx.beam(vc, pose.last().pose(), new Vec3(0.0, 6.0, 0.0), new Vec3(0.0, 0.3, 0.0), 1.2F * f, k(VIOLET, 0.3F * f), k(SILVER, 1.1F * f), 0.4F, t);
         if (sa > 4.0F) {
            vc = Fx.buf(buf, Fx.WAVE);
            float r = 0.5F + easeOut((sa - 4.0F) / 10.0F) * 4.0F;
            Fx.ring(vc, pose.last().pose(), 0.06F, r - 1.0F, r, 48, BLACK, k(VIOLET, 1.4F * f), 4.0F, 0.0F);
         }

         pose.popPose();
      }
   }

   private static void wings(float at, float t, PoseStack pose, MultiBufferSource buf) {
      float open = at < 8.0F ? easeOut(at / 8.0F) : (at > 62.0F ? Math.max(0.0F, 1.0F - (at - 62.0F) / 8.0F) : 1.0F);
      if (!(open <= 0.0F)) {
         float R = 3.3F * open;
         float rot = -t * 38.0F;
         pose.pushPose();
         pose.translate(0.0, 0.45, 0.0);
         pose.mulPose(Axis.YP.rotationDegrees(rot));
         Matrix4f m = pose.last().pose();
         VertexConsumer vc = Fx.buf(buf, FEATHER);

         for (int i = 0; i < 2; i++) {
            Vec3 dir = new Vec3(i == 0 ? 1.0 : -1.0, 0.0, 0.0);
            Vec3 c = dir.scale(0.35 + (double)(R / 2.0F));
            feather(vc, m, c, dir, R, 1.0F * open, k(SILVER, 1.25F));
            feather(vc, m, c.add(0.0, 0.05, 0.0), dir, R * 0.85F, 0.55F * open, k(VIOLET, 1.0F));
         }

         pose.popPose();
         vc = Fx.buf(buf, Fx.SOFT);
         m = pose.last().pose();
         Fx.ring(vc, m, 0.45F, R * 0.35F, R, 64, k(VIOLET, 0.15F * open), k(SILVER, 0.5F * open), 2.0F, 0.0F);
         vc = Fx.buf(buf, Fx.WAVE);
         Fx.ring(vc, m, 0.45F, R - 0.35F, R + 0.05F, 64, BLACK, k(SILVER, 1.0F * open), 8.0F, t * 0.1F);
      }
   }

   private static void sky(
      EagleEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam
   ) {
      float end = (float)EagleEntity.EA_DURATION[5];
      float fade = at > end - 10.0F ? Math.max(0.0F, (end - at) / 10.0F) : 1.0F;
      double bx = e.abilityX - ex;
      double by = e.abilityY - ey;
      double bz = e.abilityZ - ez;
      float hY = 9.0F + 13.0F * (1.0F - easeOut(at / 20.0F));
      Vec3 sky = new Vec3(bx, by + (double)hY, bz);
      eagle(buf, pose, cam, sky.x, sky.y, sky.z, 13.0F, Mth.clamp(at / 6.0F, 0.0F, 1.0F) * fade, Mth.sin(t * 0.25F));
      Matrix4f m = pose.last().pose();
      pose.pushPose();
      pose.translate(bx, by, bz);
      VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
      Fx.ring(vc, pose.last().pose(), 0.05F, 0.0F, 10.5F, 48, k(VIOLET, 0.35F * fade), BLACK, 2.0F, 0.0F);
      pose.popPose();

      for (int k = 0; k < 27; k++) {
         float spawn = (float)(20 + k * 3);
         float impact = spawn + 8.0F;
         if (!(at < spawn) && !(at > impact + 8.0F)) {
            Entity tgt = e.lanceTarget(k);
            Vec3 tp;
            if (tgt != null) {
               tp = new Vec3(
                  Mth.lerp((double)partial, tgt.xo, tgt.getX()) - ex,
                  Mth.lerp((double)partial, tgt.yo, tgt.getY()) - ey,
                  Mth.lerp((double)partial, tgt.zo, tgt.getZ()) - ez
               );
            } else {
               double a = (double)k * 0.9;
               tp = new Vec3(bx + Math.cos(a) * 4.0, by, bz + Math.sin(a) * 4.0);
            }

            if (at < impact) {
               float fr = (at - spawn) / 8.0F;
               Vec3 head = sky.add(tp.add(0.0, 0.4, 0.0).subtract(sky).scale((double)(fr * fr)));
               Vec3 dir = tp.subtract(sky).normalize();
               vc = Fx.buf(buf, Fx.SOFT);
               Fx.beam(vc, m, head.subtract(dir.scale(3.5)), head, 0.45F, BLACK, k(GOLD, 1.1F), 0.6F, 0.0F);
               vc = Fx.buf(buf, FEATHER);
               feather(vc, m, head, dir, 1.3F, 0.45F, k(new float[]{1.0F, 0.9F, 1.0F}, 1.3F));
               vc = Fx.buf(buf, Fx.WAVE);
               pose.pushPose();
               pose.translate(tp.x, tp.y, tp.z);
               Fx.ring(vc, pose.last().pose(), 0.06F, 0.7F * (1.0F - fr), 1.0F * (1.0F - fr) + 0.2F, 24, BLACK, k(VIOLET, 0.9F), 2.0F, 0.0F);
               pose.popPose();
            } else {
               float g = 1.0F - (at - impact) / 8.0F;
               vc = Fx.buf(buf, Fx.FLARE);
               Fx.billboard(vc, pose, cam, tp.x, tp.y + 0.4, tp.z, 2.2F * g, (float)k * 40.0F, k(GOLD, g));
               vc = Fx.buf(buf, Fx.WAVE);
               pose.pushPose();
               pose.translate(tp.x, tp.y, tp.z);
               float r = 0.3F + (at - impact) * 0.25F;
               Fx.ring(vc, pose.last().pose(), 0.06F, r - 0.5F, r, 24, BLACK, k(VIOLET, 1.2F * g), 2.0F, 0.0F);
               pose.popPose();
            }
         }
      }
   }

   private static void screech(float sc, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      float f = 1.0F - sc / 24.0F;
      VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
      float r = 0.5F + easeOut(sc / 14.0F) * 14.0F;
      Fx.ring(vc, pose.last().pose(), 0.5F, r - 2.0F, r, 72, BLACK, k(VIOLET, 1.5F * f), 8.0F, 0.0F);
      Fx.ring(vc, pose.last().pose(), 0.1F, r * 0.7F - 1.2F, r * 0.7F, 72, BLACK, k(GOLD, 0.9F * f), 8.0F, 0.4F);
      eagle(buf, pose, cam, 0.0, 2.8, 0.0, 6.0F * easeOut(sc / 5.0F), f, 1.0F);
   }
}
