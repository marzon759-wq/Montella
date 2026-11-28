package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.entity.LDragoEntity;
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
import org.joml.Vector3f;

public final class LDragoFx {
   public static final ResourceLocation DRAGON = V.rl("librabeyblade", "textures/fx/dragon.png");
   public static final ResourceLocation CLAW = V.rl("librabeyblade", "textures/fx/claw.png");
   private static final float[] CRIMSON = new float[]{1.0F, 0.1F, 0.22F};
   private static final float[] MAGENTA = new float[]{1.0F, 0.25F, 0.65F};
   private static final float[] ORANGE = new float[]{1.0F, 0.45F, 0.08F};
   private static final float[] GOLD = new float[]{1.0F, 0.78F, 0.2F};
   private static final float[] WHITE = new float[]{1.0F, 1.0F, 1.0F};
   private static final float[] BLACK = new float[]{0.0F, 0.0F, 0.0F};
   public static final ResourceLocation DRAGON_FLIGHT = V.rl("librabeyblade", "textures/fx/dragon_flight.png");

   private LDragoFx() {
   }

   private static void emblem(MultiBufferSource buf, PoseStack pose, Quaternionf cam, double x, double y, double z, float size, float strength) {
      // the flat logo is only a faint halo now: the 3D dragon (DragonSpirit) is the spirit
      float a = Mth.clamp(strength, 0.0F, 1.0F) * 0.18F;
      if (!(a <= 0.01F) && !(size <= 0.01F)) {
         VertexConsumer vc = Fx.bufT(buf, DRAGON, 0.9F * a);
         Fx.billboard(vc, pose, cam, x, y, z, size, 0.0F, new float[]{0.9F, 0.06F, 0.3F});
         Fx.alpha = 1.0F;
         vc = Fx.buf(buf, DRAGON);
         Fx.billboard(vc, pose, cam, x, y, z, size * 1.04F, 0.0F, k(MAGENTA, 0.35F * a));
      }
   }

   /** The 3D dragon spirit, coiled round L-Drago while its powers are active. */
   private static void dragon3d(LDragoEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      int ab = e.clientAbility;
      float at = e.abilityAge(partial);
      boolean buffOn = e.isShielded();
      if (ab == 0 && !buffOn) {
         return;
      }
      float dur = ab > 0 && ab < LDragoEntity.DR_DURATION.length ? (float)LDragoEntity.DR_DURATION[ab] : 0.0F;
      float alpha;
      if (ab > 0 && dur > 0.0F) {
         alpha = Mth.clamp(at / 10.0F, 0.0F, 1.0F) * Mth.clamp((dur - at) / 10.0F, 0.0F, 1.0F);
      } else {
         alpha = 0.85F;
      }
      double radius = 2.4;
      double height = 3.4;
      float size = 1.15F;
      float jaw = 0.1F + 0.15F * Mth.sin(t * 0.2F) * Mth.sin(t * 0.2F);
      float fire = 0.0F;
      float lunge = 0.0F;
      net.minecraft.world.entity.Entity tg = e.level().getEntity(e.getTargetId());
      Vec3 target = tg == null ? null : new Vec3(Mth.lerp(partial, tg.xo, tg.getX()) - ex, Mth.lerp(partial, tg.yo, tg.getY()) - ey + 0.5, Mth.lerp(partial, tg.zo, tg.getZ()) - ez);
      switch (ab) {
         case LDragoEntity.DR_VORTEX -> {
            radius = 2.7;
            height = 3.6;
         }
         case LDragoEntity.DR_METEOR -> {
            size = 1.2F;
            radius = 2.4;
            height = 5.0;
            jaw = 0.5F;
         }
         case LDragoEntity.DR_CLAW -> {
            lunge = 0.5F + 0.5F * Mth.sin(at * 0.45F);
            jaw = 0.3F + 0.5F * lunge;
         }
         case LDragoEntity.DR_ROAR -> {
            size = 1.45F;
            radius = 2.8;
            height = 6.5;
            float k = Mth.clamp((at - 18.0F) / 10.0F, 0.0F, 1.0F) * Mth.clamp((dur - 12.0F - at) / 8.0F, 0.0F, 1.0F);
            lunge = 0.85F * k;
            jaw = 0.25F + 0.75F * k;
            fire = k;
         }
         default -> {
            // awakening: rears up and roars once
            float r = buffOn ? Mth.clamp(1.0F - (float)Math.abs((t % 120.0F) - 20.0F) / 20.0F, 0.0F, 1.0F) : 0.0F;
            jaw = Math.max(jaw, r);
         }
      }
      Vec3[] spine = DragonSpirit.coil(46, 0.0, 0.0, 0.0, t, radius, height, (float)e.spinDir(), target, lunge);
      DragonSpirit.render(pose, buf, cam, spine, size, t, alpha, jaw, fire, target);
   }

   private static float[] k(float[] c, float k) {
      return new float[]{c[0] * k, c[1] * k, c[2] * k};
   }

   private static float easeOut(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return 1.0F - (1.0F - x) * (1.0F - x) * (1.0F - x);
   }

   public static void render(LDragoEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      dragon3d(e, partial, t, ex, ey, ez, pose, buf, cam);
      if (e.isShielded()) {
         awaken(e, t, pose, buf, cam);
      }

      float rv = t - (float)e.reviveTime;
      if (rv >= 0.0F && rv < 50.0F) {
         float f = rv < 6.0F ? rv / 6.0F : 1.0F - (rv - 6.0F) / 44.0F;
         VertexConsumer vc = Fx.bufT(buf, Fx.WISP, 0.8F * f);
         Fx.band(
            vc,
            pose.last().pose(),
            1.2F,
            0.0F,
            0.4F,
            12.0F,
            32,
            new float[]{1.0F, 0.3F, 0.08F},
            new float[]{0.6F, 0.02F, 0.08F},
            3.0F,
            t * 0.2F,
            2.0F,
            -t * 0.15F
         );
         Fx.alpha = 1.0F;
         vc = Fx.buf(buf, Fx.WAVE);
         float r = 0.5F + easeOut(rv / 16.0F) * 6.0F;
         Fx.ring(vc, pose.last().pose(), 0.08F, r - 1.4F, r, 64, BLACK, k(CRIMSON, 1.5F * f), 6.0F, 0.0F);
         dragonSilhouette(buf, pose, cam, 0.0, 2.5 + (double)(easeOut(rv / 20.0F) * 5.0F), 0.0, 10.0F, f, new Vec3(0.0, 1.0, 0.0), t);
      }

      float aw = t - (float)e.awakenTime;
      if (aw >= 0.0F && aw < 34.0F) {
         awakenBurst(aw, t, pose, buf, cam);
      }

      claws(e, t, ex, ey, ez, pose, buf, cam);
      float ia = t - (float)e.impactTime;
      if (ia >= 0.0F && ia < 34.0F) {
         meteorImpact(ia, t, pose, buf, cam);
      }

      int ab = e.clientAbility;
      float at = e.abilityAge(partial);
      if (ab != 0 && !(at < 0.0F)) {
         double bx = e.abilityX - ex;
         double by = e.abilityY - ey;
         double bz = e.abilityZ - ez;
         switch (ab) {
            case 2:
               vortex(e, at, t, partial, ex, ey, ez, pose, buf, cam);
               break;
            case 3:
               meteor(e, at, t, bx, by, bz, pose, buf, cam);
               break;
            case 4:
               VertexConsumer vc = Fx.buf(buf, Fx.FLARE);
               Fx.billboard(vc, pose, cam, 0.0, 0.35, 0.0, 2.2F + 0.3F * Mth.sin(t * 2.0F), t * 9.0F, k(CRIMSON, 0.8F));
               break;
            case 5:
               roar(e, at, t, partial, ex, ey, ez, pose, buf, cam);
         }
      }
   }

   private static void awaken(LDragoEntity e, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      int rem = e.shieldRemaining();
      float fade = rem < 16 ? (float)rem / 16.0F : 1.0F;
      VertexConsumer vc = Fx.buf(buf, Fx.WISP);
      Matrix4f m = pose.last().pose();

      for (int i = 0; i < 2; i++) {
         Fx.helix(
            vc,
            m,
            0.0F,
            1.7F,
            1.15F,
            0.55F,
            t * 0.32F + (float)i * (float) Math.PI,
            1.6F,
            0.45F,
            36,
            k(CRIMSON, 1.1F * fade),
            k(ORANGE, 0.5F * fade),
            -t * 0.15F
         );
      }

      Fx.ring(vc, m, 0.04F, 0.35F, 1.35F, 40, k(ORANGE, 0.9F * fade), k(CRIMSON, 0.1F * fade), 3.0F, t * 0.08F);
      float pulse = 0.8F + 0.2F * Mth.sin(t * 0.6F);
      emblem(buf, pose, cam, 0.0, 1.55, 0.0, 1.0F * pulse, 0.9F * fade);
   }

   private static void awakenBurst(float aw, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      float f = 1.0F - aw / 34.0F;
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
      float r = 0.5F + easeOut(aw / 14.0F) * 5.0F;
      Fx.ring(vc, m, 0.1F, r - 1.2F, r, 64, BLACK, k(CRIMSON, 1.5F * f), 6.0F, 0.0F);
      float grow = easeOut(aw / 8.0F);
      emblem(buf, pose, cam, 0.0, 2.6, 0.0, 5.5F * grow, 1.2F * f);
      vc = Fx.buf(buf, Fx.FLARE);
      Fx.billboard(vc, pose, cam, 0.0, 0.4, 0.0, 4.0F * f, t * 5.0F, k(CRIMSON, f));
   }

   private static void vortex(
      LDragoEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam
   ) {
      float dur = (float)LDragoEntity.DR_DURATION[2];
      float f = Mth.clamp(at / 8.0F, 0.0F, 1.0F) * (at > dur - 10.0F ? Math.max(0.0F, (dur - at) / 10.0F) : 1.0F);
      float R = 7.5F * easeOut(at / 10.0F);
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.bufT(buf, Fx.WISP, 0.6F * f);
      pose.pushPose();
      pose.mulPose(Axis.YP.rotationDegrees(t * 9.0F));
      Fx.ring(vc, pose.last().pose(), 0.03F, 0.0F, R, 48, new float[]{0.08F, 0.0F, 0.02F}, new float[]{0.35F, 0.02F, 0.06F}, 3.0F, 0.0F);
      pose.popPose();
      Fx.alpha = 1.0F;
      vc = Fx.buf(buf, Fx.SOFT);

      for (int i = 0; i < 6; i++) {
         spiralArm(
            vc,
            m,
            0.06F,
            0.4F,
            R,
            (float)i * (float) (Math.PI * 2) / 6.0F + t * 0.22F,
            0.55F,
            0.35F + 0.25F * (R / 7.5F),
            30,
            k(MAGENTA, 1.1F * f),
            k(CRIMSON, 0.15F * f)
         );
      }

      vc = Fx.buf(buf, Fx.WAVE);
      Fx.ring(vc, m, 0.08F, R - 0.8F, R, 64, BLACK, k(CRIMSON, 1.1F * f), 8.0F, -t * 0.05F);
      vc = Fx.buf(buf, Fx.SOFT);
      Vec3 core = new Vec3(0.0, 0.45, 0.0);

      for (Entity en : e.enemiesWithin(7.5)) {
         Vec3 pe = new Vec3(
            Mth.lerp((double)partial, en.xo, en.getX()) - ex,
            Mth.lerp((double)partial, en.yo, en.getY()) + (double)en.getBbHeight() * 0.5 - ey,
            Mth.lerp((double)partial, en.zo, en.getZ()) - ez
         );
         Fx.beam(vc, m, pe, core, 0.28F * f, k(MAGENTA, 0.3F * f), k(CRIMSON, 1.1F * f), 0.9F, t * 0.7F);
      }

      vc = Fx.buf(buf, Fx.FLARE);
      Fx.billboard(vc, pose, cam, 0.0, 0.4, 0.0, 2.6F * f + 0.3F * Mth.sin(t), -t * 6.0F, k(CRIMSON, 0.9F * f));
   }

   private static void spiralArm(
      VertexConsumer vc, Matrix4f m, float y, float r0, float r1, float a0, float turns, float width, int steps, float[] cIn, float[] cOut
   ) {
      for (int i = 0; i < steps; i++) {
         float f0 = (float)i / (float)steps;
         float f1 = (float)(i + 1) / (float)steps;
         float rr0 = Mth.lerp(f0, r0, r1);
         float rr1 = Mth.lerp(f1, r0, r1);
         float aa0 = a0 - f0 * turns * (float) (Math.PI * 2);
         float aa1 = a0 - f1 * turns * (float) (Math.PI * 2);
         float w0 = width * (0.3F + 0.7F * f0);
         float w1 = width * (0.3F + 0.7F * f1);
         float[] c0 = Fx.lerp(f0, cIn, cOut);
         float[] c1 = Fx.lerp(f1, cIn, cOut);
         float cx0 = Mth.cos(aa0);
         float sx0 = Mth.sin(aa0);
         float cx1 = Mth.cos(aa1);
         float sx1 = Mth.sin(aa1);
         float[] p0 = new float[]{cx0 * (rr0 - w0 / 2.0F), y, sx0 * (rr0 - w0 / 2.0F)};
         float[] p1 = new float[]{cx0 * (rr0 + w0 / 2.0F), y, sx0 * (rr0 + w0 / 2.0F)};
         float[] p2 = new float[]{cx1 * (rr1 + w1 / 2.0F), y, sx1 * (rr1 + w1 / 2.0F)};
         float[] p3 = new float[]{cx1 * (rr1 - w1 / 2.0F), y, sx1 * (rr1 - w1 / 2.0F)};
         Fx.v(vc, m, p0[0], p0[1], p0[2], c0[0], c0[1], c0[2], f0, 0.0F);
         Fx.v(vc, m, p1[0], p1[1], p1[2], c0[0], c0[1], c0[2], f0, 1.0F);
         Fx.v(vc, m, p2[0], p2[1], p2[2], c1[0], c1[1], c1[2], f1, 1.0F);
         Fx.v(vc, m, p3[0], p3[1], p3[2], c1[0], c1[1], c1[2], f1, 0.0F);
         Fx.v(vc, m, p3[0], p3[1], p3[2], c1[0], c1[1], c1[2], f1, 0.0F);
         Fx.v(vc, m, p2[0], p2[1], p2[2], c1[0], c1[1], c1[2], f1, 1.0F);
         Fx.v(vc, m, p1[0], p1[1], p1[2], c0[0], c0[1], c0[2], f0, 1.0F);
         Fx.v(vc, m, p0[0], p0[1], p0[2], c0[0], c0[1], c0[2], f0, 0.0F);
      }
   }

   private static void meteor(LDragoEntity e, float at, float t, double bx, double by, double bz, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      Matrix4f m = pose.last().pose();
      if (at < 14.0F) {
         float f = at < 8.0F ? Mth.clamp(at / 4.0F, 0.0F, 1.0F) : 1.0F - (at - 8.0F) / 6.0F;
         VertexConsumer vc = Fx.buf(buf, Fx.WISP);
         Fx.ring(vc, m, 0.04F, 0.0F, 1.8F, 40, k(ORANGE, 1.2F * f), k(CRIMSON, 0.1F * f), 3.0F, t * 0.1F);
         vc = Fx.buf(buf, Fx.FLARE);
         Fx.billboard(vc, pose, cam, 0.0, 0.2, 0.0, 2.2F * f, t * 8.0F, k(ORANGE, f));
      }

      if (at >= 8.0F && at < 46.0F) {
         float f = at < 44.0F ? 1.0F : 1.0F - (at - 44.0F) / 2.0F;
         Vec3 base = new Vec3(bx, by, bz);
         Vec3 top = new Vec3(0.0, 0.3, 0.0);
         VertexConsumer vc = Fx.bufT(buf, Fx.WISP, 0.75F * f);
         Fx.beam(vc, m, base, top, 1.6F, new float[]{1.0F, 0.35F, 0.05F}, new float[]{0.9F, 0.1F, 0.1F}, 0.25F, -t * 0.4F);
         Fx.alpha = 1.0F;
         vc = Fx.buf(buf, Fx.SOFT);
         Fx.beam(vc, m, base, top, 0.6F, k(new float[]{1.0F, 0.8F, 0.4F}, 0.9F * f), k(ORANGE, 0.5F * f), 0.4F, -t * 0.8F);
      }

      if (at >= 26.0F && at < 74.0F) {
         float f = Mth.clamp((at - 30.0F + 4.0F) / 8.0F, 0.0F, 1.0F);
         VertexConsumer vc = Fx.buf(buf, Fx.WISP);
         Fx.sphere(vc, m, 0.35F, 1.1F + 0.15F * Mth.sin(t * 1.4F), (float) Math.PI, 10, 20, k(ORANGE, 0.9F * f), 2.0F, 2.0F, t * 0.05F, -t * 0.08F);
         if (at < 48.0F) {
            float g = at < 44.0F ? f : 1.0F - (at - 44.0F) / 4.0F;
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, 0.0, 0.35, 0.0, 5.0F * f, t * 4.0F, k(ORANGE, 0.9F * g));
         }
      }

      if (at >= 26.0F && e.impactTime < e.clientAbilityStart) {
         float f = Mth.clamp((at - 30.0F + 4.0F) / 6.0F, 0.0F, 1.0F);
         Vec3 vel = new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo);
         Vec3 dir = !(at < 44.0F) && !(vel.lengthSqr() < 0.001) ? vel.normalize() : new Vec3(0.0, -1.0, 0.0);
         boolean grab = e.grabTime > e.clientAbilityStart;
         dragonSilhouette(buf, pose, cam, 0.0, 0.6, 0.0, grab ? 11.0F : 9.0F, f, dir, t);
      }

      if (at >= 44.0F && e.impactTime < e.clientAbilityStart) {
         Vec3 vel = new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo);
         if (vel.lengthSqr() > 0.001) {
            Vec3 tail = vel.normalize().scale(-7.0);
            VertexConsumer vc = Fx.bufT(buf, Fx.WISP, 0.85F);
            Fx.beam(vc, m, new Vec3(0.0, 0.35, 0.0), tail, 2.2F, new float[]{1.0F, 0.5F, 0.1F}, new float[]{0.6F, 0.02F, 0.05F}, 0.3F, t * 0.5F);
            Fx.alpha = 1.0F;
            vc = Fx.buf(buf, Fx.SOFT);
            Fx.beam(vc, m, new Vec3(0.0, 0.35, 0.0), tail.scale(0.6), 0.9F, k(new float[]{1.0F, 0.85F, 0.5F}, 1.1F), BLACK, 0.5F, t);
         }
      }
   }

   private static void dragonSilhouette(
      MultiBufferSource buf, PoseStack pose, Quaternionf cam, double x, double y, double z, float size, float a, Vec3 dir, float t
   ) {
      if (!(a <= 0.01F)) {
         Vector3f v = new Vector3f((float)dir.x, (float)dir.y, (float)dir.z);
         new Quaternionf(cam).conjugate().transform(v);
         float rot = (float)Math.toDegrees(Math.atan2((double)v.y, (double)(-v.x))) + 90.0F;
         float flap = 1.0F + 0.06F * Mth.sin(t * 0.9F);
         VertexConsumer vc = Fx.bufT(buf, DRAGON_FLIGHT, 0.92F * a);
         Fx.billboard(vc, pose, cam, x, y, z, size * flap, rot, new float[]{1.0F, 1.0F, 1.0F});
         Fx.alpha = 1.0F;
      }
   }

   private static void meteorImpact(float ia, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      float f = 1.0F - ia / 34.0F;
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
      float r = 0.5F + easeOut(ia / 16.0F) * 6.5F * 1.6F;
      Fx.ring(vc, m, 0.08F, r - 2.4F, r, 72, BLACK, k(CRIMSON, 1.6F * f), 6.0F, 0.0F);
      Fx.ring(vc, m, 0.1F, r * 0.6F - 1.2F, r * 0.6F, 72, BLACK, k(ORANGE, 1.0F * f), 6.0F, 0.4F);
      vc = Fx.buf(buf, Fx.BOLT);
      float len = Math.min(1.0F, ia / 5.0F) * 7.0F;

      for (int i = 0; i < 10; i++) {
         double a = (double)i * Math.PI * 2.0 / 10.0 + (double)(i % 2) * 0.2;
         float l = len * (0.65F + 0.35F * (float)(i * 7 % 5) / 4.0F);
         Vec3 b = new Vec3(Math.cos(a) * (double)l, 0.05, Math.sin(a) * (double)l);
         Fx.beamV(vc, m, new Vec3(0.0, 0.05, 0.0), b, 0.7F, k(ORANGE, 1.4F * f), 0.4F, (float)i * 0.37F);
      }

      vc = Fx.bufT(buf, Fx.WISP, 0.85F * f);
      Fx.band(
         vc,
         m,
         r * 0.8F,
         0.0F,
         r * 0.8F + 0.3F,
         1.6F * f,
         64,
         new float[]{1.0F, 0.35F, 0.05F},
         new float[]{0.7F, 0.05F, 0.08F},
         8.0F,
         t * 0.1F,
         1.0F,
         -t * 0.12F
      );
      Fx.alpha = 1.0F;
      if (ia < 14.0F) {
         float g = 1.0F - ia / 14.0F;
         vc = Fx.buf(buf, Fx.FLARE);
         Fx.billboard(vc, pose, cam, 0.0, 0.8, 0.0, 11.0F * g, t * 2.0F, k(new float[]{1.0F, 0.6F, 0.3F}, 1.2F * g));
         emblem(buf, pose, cam, 0.0, 3.0, 0.0, 7.0F * easeOut(ia / 6.0F), 1.2F * g);
      }
   }

   private static void claws(LDragoEntity e, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      VertexConsumer vc = null;

      for (int i = 0; i < e.clawTimes.length; i++) {
         float ca = t - (float)e.clawTimes[i];
         if (!(ca < 0.0F) && !(ca >= 12.0F)) {
            if (vc == null) {
               vc = Fx.buf(buf, CLAW);
            }

            float f = 1.0F - ca / 12.0F;
            float grow = easeOut(ca / 3.0F);
            double x = e.clawPos[i][0] - ex;
            double y = e.clawPos[i][1] - ey;
            double z = e.clawPos[i][2] - ez;
            Fx.billboard(vc, pose, cam, x, y, z, 3.4F * (0.6F + 0.4F * grow), e.clawRot[i], k(CRIMSON, 1.5F * f));
            Fx.billboard(vc, pose, cam, x, y, z, 2.6F * (0.6F + 0.4F * grow), e.clawRot[i], k(WHITE, 0.7F * f * f));
         }
      }
   }

   private static void roar(
      LDragoEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam
   ) {
      Entity tgt = e.level().getEntity(e.getTargetId());
      if (tgt != null) {
         Vec3 fromW = new Vec3(ex, ey + 0.4, ez);
         Vec3 toW = new Vec3(
            Mth.lerp((double)partial, tgt.xo, tgt.getX()),
            Mth.lerp((double)partial, tgt.yo, tgt.getY()) + (double)tgt.getBbHeight() * 0.5,
            Mth.lerp((double)partial, tgt.zo, tgt.getZ())
         );
         Vec3 dir = LDragoEntity.breathDir(Math.max(at, 20.0F), fromW, toW);
         Vec3 from = new Vec3(0.0, 0.4, 0.0);
         Vec3 mouth = from.add(dir.scale(0.7));
         Matrix4f m = pose.last().pose();
         float in = easeOut(at / 14.0F);
         float out = at > 62.0F ? Math.max(0.0F, 1.0F - (at - 62.0F) / 10.0F) : 1.0F;
         Vec3 behind = from.add(dir.scale(-1.2)).add(0.0, 2.0, 0.0);
         emblem(buf, pose, cam, behind.x, behind.y, behind.z, 6.5F * in, 1.0F * in * out);
         if (at < 20.0F) {
            float ch = at / 20.0F;
            VertexConsumer vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, mouth.x, mouth.y, mouth.z, 0.5F + 2.2F * ch, t * 7.0F, k(ORANGE, 0.6F + 0.6F * ch));
            vc = Fx.buf(buf, Fx.WAVE);

            for (int i = 0; i < 3; i++) {
               float f = (at * 0.15F + (float)i / 3.0F) % 1.0F;
               Fx.ringAround(vc, m, mouth, dir, 2.2F * (1.0F - f), 2.2F * (1.0F - f) + 0.3F, 32, k(ORANGE, f), 0.0F);
            }
         } else {
            float ff = at < 23.0F ? (at - 20.0F) / 3.0F : out * (at > 58.0F ? Math.max(0.0F, (62.0F - at) / 4.0F + 0.3F) : 1.0F);
            float L = 13.0F * Mth.clamp((at - 20.0F) / 5.0F, 0.0F, 1.0F);
            float tan = (float)Math.tan(LDragoEntity.R_HALF_ANGLE);
            VertexConsumer vc = Fx.bufT(buf, Fx.WISP, 0.8F * ff);
            cone(vc, m, mouth, dir, L, tan, 28, 8, new float[]{1.0F, 0.85F, 0.35F}, new float[]{0.85F, 0.12F, 0.04F}, -t * 0.25F);
            Fx.alpha = 1.0F;
            vc = Fx.buf(buf, Fx.WISP);
            cone(vc, m, mouth, dir, L * 0.8F, tan * 0.45F, 20, 6, k(new float[]{1.0F, 0.75F, 0.3F}, 1.0F * ff), k(ORANGE, 0.2F * ff), -t * 0.4F);
            vc = Fx.buf(buf, Fx.WAVE);

            for (int i = 0; i < 7; i++) {
               float s = ((at - 20.0F) * 0.9F + (float)i * L / 7.0F) % Math.max(0.5F, L);
               float r = 0.25F + s * tan;
               Fx.ringAround(vc, m, mouth.add(dir.scale((double)s)), dir, r * 0.55F, r, 32, k(ORANGE, ff * (1.1F - s / Math.max(0.5F, L))), 0.0F);
            }

            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, mouth.x, mouth.y, mouth.z, 2.4F * ff, t * 9.0F, k(new float[]{1.0F, 0.8F, 0.5F}, ff));
         }
      }
   }

   private static void cone(
      VertexConsumer vc, Matrix4f m, Vec3 apex, Vec3 axis, float length, float tan, int segs, int rings, float[] cNear, float[] cFar, float vOff
   ) {
      Vec3 an = axis.normalize();
      Vec3 up = Math.abs(an.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 e1 = an.cross(up).normalize();
      Vec3 e2 = an.cross(e1).normalize();

      for (int j = 0; j < rings; j++) {
         float s0 = length * (float)j / (float)rings;
         float s1 = length * (float)(j + 1) / (float)rings;
         float r0 = 0.15F + s0 * tan;
         float r1 = 0.15F + s1 * tan;
         float[] c0 = Fx.lerp((float)j / (float)rings, cNear, cFar);
         float[] c1 = Fx.lerp((float)(j + 1) / (float)rings, cNear, cFar);

         for (int i = 0; i < segs; i++) {
            double a0 = (double)i * Math.PI * 2.0 / (double)segs;
            double a1 = (double)(i + 1) * Math.PI * 2.0 / (double)segs;
            Vec3 d0 = e1.scale(Math.cos(a0)).add(e2.scale(Math.sin(a0)));
            Vec3 d1 = e1.scale(Math.cos(a1)).add(e2.scale(Math.sin(a1)));
            Vec3 p0 = apex.add(an.scale((double)s0)).add(d0.scale((double)r0));
            Vec3 p1 = apex.add(an.scale((double)s1)).add(d0.scale((double)r1));
            Vec3 p2 = apex.add(an.scale((double)s1)).add(d1.scale((double)r1));
            Vec3 p3 = apex.add(an.scale((double)s0)).add(d1.scale((double)r0));
            float u0 = 2.0F * (float)i / (float)segs;
            float u1 = 2.0F * (float)(i + 1) / (float)segs;
            float v0 = vOff + (float)j / (float)rings * 2.0F;
            float v1 = vOff + (float)(j + 1) / (float)rings * 2.0F;
            Fx.quad(vc, m, f(p0), f(p1), f(p2), f(p3), u0, v0, u1, v1, c0, c1);
         }
      }
   }

   private static float[] f(Vec3 v) {
      return new float[]{(float)v.x, (float)v.y, (float)v.z};
   }
}
