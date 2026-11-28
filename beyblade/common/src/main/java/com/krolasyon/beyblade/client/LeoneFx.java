package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.compat.VClient;
import com.krolasyon.beyblade.entity.LeoneEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class LeoneFx {
   public static final ResourceLocation LION = V.rl("librabeyblade", "textures/fx/lion.png");
   public static final ResourceLocation FANG = V.rl("librabeyblade", "textures/fx/fang.png");
   private static final ResourceLocation STONE = V.rl("minecraft", "textures/block/stone.png");
   private static final ResourceLocation ROCK = V.rl("minecraft", "textures/block/cobblestone.png");
   private static final float[] WIND = new float[]{0.62F, 1.0F, 0.72F};
   private static final float[] EMERALD = new float[]{0.2F, 1.0F, 0.45F};
   private static final float[] EARTH = new float[]{0.95F, 0.78F, 0.45F};
   private static final float[] WHITE = new float[]{1.0F, 1.0F, 1.0F};
   private static final float[] BLACK = new float[]{0.0F, 0.0F, 0.0F};
   private static final float[] GREY_LIGHT = new float[]{0.78F, 0.8F, 0.82F};
   private static final float C_HOLD_HEIGHT_F = 2.4F;

   private LeoneFx() {
   }

   private static float[] k(float[] c, float k) {
      return new float[]{c[0] * k, c[1] * k, c[2] * k};
   }

   private static float easeOut(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return 1.0F - (1.0F - x) * (1.0F - x) * (1.0F - x);
   }

   private static void emblem(MultiBufferSource buf, PoseStack pose, Quaternionf cam, double x, double y, double z, float size, float strength) {
      float a = Mth.clamp(strength, 0.0F, 1.0F);
      if (!(a <= 0.01F) && !(size <= 0.01F)) {
         VertexConsumer vc = Fx.bufT(buf, LION, 0.9F * a);
         Fx.billboard(vc, pose, cam, x, y, z, size, 0.0F, new float[]{0.15F, 0.85F, 0.3F});
         Fx.alpha = 1.0F;
         vc = Fx.buf(buf, LION);
         Fx.billboard(vc, pose, cam, x, y, z, size * 1.04F, 0.0F, k(new float[]{0.6F, 1.0F, 0.3F}, 0.4F * a));
      }
   }

   public static void render(
      LeoneEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam, int light
   ) {
      if (e.isShielded()) {
         windWall(e, t, pose, buf, cam);
      }

      float qa = t - (float)e.quakeTime;
      if (qa >= 0.0F && qa < 56.0F) {
         quake(qa, t, pose, buf, light);
      }

      float ba = t - (float)e.cageBlastTime;
      if (ba >= 0.0F && ba < 26.0F) {
         cageBlast(e, ba, t, ex, ey, ez, pose, buf, cam);
      }

      int ab = e.clientAbility;
      float at = e.abilityAge(partial);
      if (ab != 0 && !(at < 0.0F)) {
         switch (ab) {
            case 2:
               fangs(e, at, t, partial, ex, ey, ez, pose, buf, cam);
               break;
            case 3:
               pounce(e, at, t, pose, buf, cam);
            case 4:
            default:
               break;
            case 5:
               cage(e, at, t, partial, ex, ey, ez, pose, buf, cam);
               break;
            case 6:
               giant(e, at, t, partial, ex, ey, ez, pose, buf, cam);
         }
      }
   }

   private static void windWall(LeoneEntity e, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      int rem = e.shieldRemaining();
      float age = (float)(180 - rem);
      float grow = easeOut(age / 8.0F);
      float fade = rem < 14 ? (float)rem / 14.0F : 1.0F;
      float hit = Math.max(0.0F, 1.0F - (t - (float)e.wallHitTime) / 7.0F);
      float R = 1.7F * grow;
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, Fx.WISP);
      Fx.band(vc, m, R, 0.0F, R * 1.12F, 2.3F, 40, k(WIND, (0.55F + hit * 0.5F) * fade), BLACK, 3.0F, t * 0.22F, 1.5F, -t * 0.03F);
      Fx.band(vc, m, R * 0.86F, 0.0F, R * 0.95F, 1.9F, 40, k(WHITE, (0.35F + hit * 0.4F) * fade), BLACK, 4.0F, -t * 0.3F, 1.0F, t * 0.04F);

      for (int i = 0; i < 8; i++) {
         Fx.helix(
            vc,
            m,
            0.05F,
            2.2F,
            R * 1.02F,
            R * 1.1F,
            (float)i * (float) Math.PI / 4.0F + t * 0.45F,
            0.7F,
            0.12F,
            24,
            k(WHITE, 0.8F * fade),
            k(WIND, 0.1F * fade),
            t * 0.2F
         );
      }

      vc = Fx.buf(buf, Fx.SOFT);
      Fx.ring(vc, m, 0.04F, R - 0.35F, R + 0.35F, 48, BLACK, k(EMERALD, (0.8F + hit) * fade), 3.0F, t * 0.05F);
      emblem(buf, pose, cam, 0.0, 2.6 + 0.1 * (double)Mth.sin(t * 0.2F), 0.0, 1.1F * grow, 0.8F * fade);
   }

   private static void fangs(
      LeoneEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam
   ) {
      Entity tgt = e.level().getEntity(e.getTargetId());
      if (tgt != null) {
         Vec3 to = new Vec3(
            Mth.lerp((double)partial, tgt.xo, tgt.getX()) - ex,
            Mth.lerp((double)partial, tgt.yo, tgt.getY()) + (double)tgt.getBbHeight() * 0.5 - ey,
            Mth.lerp((double)partial, tgt.zo, tgt.getZ()) - ez
         );
         Vec3 from = new Vec3(0.0, 0.5, 0.0);
         Vec3 d = to.subtract(from);
         Vec3 side = new Vec3(-d.z, 0.0, d.x);
         side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
         Matrix4f m = pose.last().pose();
         VertexConsumer vc = Fx.buf(buf, Fx.FLARE);
         Fx.billboard(vc, pose, cam, 0.0, 0.5, 0.0, 1.6F + 0.3F * Mth.sin(t * 2.0F), t * 8.0F, k(EMERALD, 0.7F));

         for (int k = 0; k < 20; k++) {
            float age = at - (float)(6 + k * 2);
            if (!(age < 0.0F) && !(age >= 8.0F)) {
               float f = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
               float sgn = k % 2 == 0 ? 1.0F : -1.0F;
               float swing = (1.0F + (float)(k % 3) * 0.4F) * Mth.sin(f * (float) Math.PI);
               Vec3 p = from.add(d.scale((double)f))
                  .add(side.scale((double)(sgn * swing)))
                  .add(0.0, 0.6 * (double)Mth.sin(f * (float) Math.PI), 0.0);
               float f0 = Math.max(0.0F, f - 0.3F);
               float swing0 = (1.0F + (float)(k % 3) * 0.4F) * Mth.sin(f0 * (float) Math.PI);
               Vec3 p0 = from.add(d.scale((double)f0))
                  .add(side.scale((double)(sgn * swing0)))
                  .add(0.0, 0.6 * (double)Mth.sin(f0 * (float) Math.PI), 0.0);
               if (age < 6.0F) {
                  vc = Fx.buf(buf, Fx.SOFT);
                  Fx.beam(vc, m, p0, p, 0.3F, BLACK, k(WIND, 0.9F), 1.0F, 0.0F);
                  vc = Fx.buf(buf, FANG);
                  Fx.billboard(vc, pose, cam, p.x, p.y, p.z, 1.1F, (float)k * 47.0F + age * 30.0F, k(new float[]{0.7F, 1.0F, 0.8F}, 1.3F));
               } else {
                  float g = 1.0F - (age - 6.0F) / 2.0F;
                  vc = Fx.buf(buf, Fx.FLARE);
                  Fx.billboard(vc, pose, cam, to.x, to.y, to.z, 1.4F * g, (float)k * 33.0F, k(WIND, g));
               }
            }
         }
      }
   }

   private static void pounce(LeoneEntity e, float at, float t, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      Matrix4f m = pose.last().pose();
      if (!(at < 12.0F)) {
         float end = 36.0F;
         if (at < end) {
            float g = at > end - 6.0F ? (end - at) / 6.0F : 1.0F;
            emblem(buf, pose, cam, 0.0, 0.45, 0.0, 3.6F + 0.2F * Mth.sin(t * 1.5F), g);
            VertexConsumer vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, 0.0, 0.4, 0.0, 2.6F * g, t * 6.0F, k(EMERALD, 0.7F * g));
            if (at >= 20.0F) {
               Vec3 vel = new Vec3(e.getX() - e.xo, e.getY() - e.yo, e.getZ() - e.zo);
               if (vel.lengthSqr() > 0.001) {
                  Vec3 tail = vel.normalize().scale(-6.0);
                  vc = Fx.buf(buf, Fx.SOFT);
                  Fx.beam(vc, m, new Vec3(0.0, 0.4, 0.0), tail, 1.8F * g, k(WHITE, 0.9F * g), BLACK, 0.5F, t * 0.6F);
                  Fx.beam(vc, m, new Vec3(0.0, 0.4, 0.0), tail.scale(0.8), 0.7F * g, k(WIND, 1.1F * g), BLACK, 0.8F, t);
               }
            }
         }
      } else {
         float f = at / 12.0F;
         VertexConsumer vc = Fx.buf(buf, Fx.WAVE);

         for (int i = 0; i < 3; i++) {
            float g = (at * 0.12F + (float)i / 3.0F) % 1.0F;
            float r = 3.5F * (1.0F - g) + 0.3F;
            Fx.ring(vc, m, 0.05F, r - 0.4F, r, 48, k(WIND, g), BLACK, 4.0F, 0.0F);
         }

         emblem(buf, pose, cam, 0.0, 1.0, 0.0, 2.6F * easeOut(f), f);
      }
   }

   private static void quake(float qa, float t, PoseStack pose, MultiBufferSource buf, int light) {
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, Fx.WAVE);

      for (int i = 0; i < 3; i++) {
         float age = qa - (float)(i * 10);
         if (!(age < 0.0F) && !(age >= 18.0F)) {
            float fade = 1.0F - age / 18.0F;
            float front = 0.8F + age * 0.48F * (1.0F + (float)i * 0.25F);
            Fx.ring(vc, m, 0.06F, front - 1.4F, front, 64, BLACK, k(EARTH, 1.3F * fade), 6.0F, (float)i * 0.3F);
         }
      }

      vc = Fx.bufT(buf, Fx.WISP, 0.7F);

      for (int ix = 0; ix < 3; ix++) {
         float age = qa - (float)(ix * 10);
         if (!(age < 0.0F) && !(age >= 18.0F)) {
            float fade = 1.0F - age / 18.0F;
            float front = 0.8F + age * 0.48F * (1.0F + (float)ix * 0.25F);
            Fx.alpha = 0.7F * fade;
            Fx.band(
               vc,
               m,
               front - 0.2F,
               0.0F,
               front + 0.2F,
               1.1F * fade,
               48,
               new float[]{0.6F, 0.5F, 0.38F},
               new float[]{0.8F, 0.72F, 0.6F},
               6.0F,
               t * 0.05F,
               1.0F,
               -t * 0.1F
            );
         }
      }

      Fx.alpha = 1.0F;
      VertexConsumer stone = buf.getBuffer(RenderType.entityCutoutNoCull(STONE));
      spikes(stone, pose, qa, light, false);
      VertexConsumer rock = buf.getBuffer(RenderType.entityCutoutNoCull(ROCK));
      spikes(rock, pose, qa, light, true);
   }

   private static void spikes(VertexConsumer vc, PoseStack pose, float qa, int light, boolean odd) {
      for (int i = 0; i < 3; i++) {
         float speed = 0.48F * (1.0F + (float)i * 0.25F);
         int n = 9 + i * 4;

         for (int j = 0; j < n; j++) {
            if (j % 2 == 1 == odd) {
               float rho = 1.5F + (float)(j % 3) * 1.3F + (float)i * 0.7F;
               float age = qa - (float)(i * 10) - (rho - 0.8F) / speed;
               if (!(age < 0.0F) && !(age >= 18.0F)) {
                  float h;
                  if (age < 3.0F) {
                     h = age / 3.0F;
                  } else if (age < 11.0F) {
                     h = 1.0F;
                  } else {
                     h = (18.0F - age) / 7.0F;
                  }

                  float hmax = 0.7F + (float)((j * 7 + i * 3) % 5) * 0.22F;
                  float a = (float)j * (float) (Math.PI * 2) / (float)n + (float)i * 0.37F;
                  pose.pushPose();
                  pose.translate(Mth.cos(a) * rho, -0.25F, Mth.sin(a) * rho);
                  pose.mulPose(Axis.YP.rotationDegrees(-a * (180.0F / (float)Math.PI) + 45.0F));
                  pose.mulPose(Axis.ZP.rotationDegrees(12.0F + (float)(j % 4) * 5.0F));
                  spike(vc, pose, 0.32F + (float)(j % 3) * 0.06F, hmax * h + 0.01F, light);
                  pose.popPose();
               }
            }
         }
      }
   }

   private static void spike(VertexConsumer vc, PoseStack pose, float w, float h, int light) {
      Matrix4f m = pose.last().pose();
      Matrix3f n = pose.last().normal();
      float[][] base = new float[][]{{-w, -w}, {w, -w}, {w, w}, {-w, w}};

      for (int i = 0; i < 4; i++) {
         float[] a = base[i];
         float[] b = base[(i + 1) % 4];
         float mx = (a[0] + b[0]) / 2.0F;
         float mz = (a[1] + b[1]) / 2.0F;
         float nl = (float)Math.sqrt((double)(mx * mx + mz * mz + w * w * 0.25F));
         float nx = mx / nl;
         float ny = w * 0.5F / nl;
         float nz = mz / nl;
         sv(vc, m, n, a[0], 0.0F, a[1], 0.0F, 1.0F, nx, ny, nz, light);
         sv(vc, m, n, b[0], 0.0F, b[1], 1.0F, 1.0F, nx, ny, nz, light);
         sv(vc, m, n, 0.0F, h, 0.0F, 0.55F, 0.0F, nx, ny, nz, light);
         sv(vc, m, n, 0.0F, h, 0.0F, 0.45F, 0.0F, nx, ny, nz, light);
      }
   }

   private static void sv(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, float nx, float ny, float nz, int light) {
      VClient.vertex(vc, m, n, x, y, z, 1.0F, 1.0F, 1.0F, 1.0F, u, v, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
   }

   private static void cage(
      LeoneEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam
   ) {
      float form = easeOut(at / 14.0F);
      float fade = at > 205.0F ? Math.max(0.0F, 1.0F - (at - 205.0F) / 45.0F) : 1.0F;
      if (!(fade <= 0.0F)) {
         float H = 21.0F * form * (0.55F + 0.45F * fade);
         Vec3[] pil = new Vec3[4];

         for (int i = 0; i < 4; i++) {
            pil[i] = LeoneEntity.pillar(e.cageX - ex, e.cageY - ey - 0.05, e.cageZ - ez, i, at);
         }

         for (int i = 0; i < 4; i++) {
            twister(pose, buf, pil[i], H, t, i, fade, 0.55F, 5.75F, 1.0F, 22, 2.0F);
         }

         if (at >= 8.400001F) {
            Matrix4f m = pose.last().pose();
            Vec3 c = new Vec3(e.cageX - ex, e.cageY - ey + 2.4, e.cageZ - ez);
            VertexConsumer vc = Fx.buf(buf, Fx.WAVE);

            for (int r = 0; r < 3; r++) {
               float tilt = (float)r * 60.0F + t * 4.0F;
               Vec3 axis = new Vec3(
                  Math.sin(Math.toRadians((double)tilt)), Math.cos(Math.toRadians((double)tilt)), Math.sin(Math.toRadians((double)tilt * 0.7)) * 0.5
               );
               Fx.ringAround(vc, m, c, axis, 1.0F, 1.35F, 40, k(GREY_LIGHT, 0.7F * fade), t * 0.05F);
            }

            emblem(buf, pose, cam, c.x, c.y + 3.2, c.z, 3.2F * form, 0.9F * fade);
         }
      }
   }

   private static void giant(
      LeoneEntity e, float at, float t, float partial, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam
   ) {
      if (e.giantTime < e.clientAbilityStart) {
         VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
         Fx.ring(vc, pose.last().pose(), 0.06F, 0.6F, 1.6F, 32, BLACK, k(GREY_LIGHT, 0.8F), 3.0F, -t * 0.1F);
      } else {
         float ca = t - (float)e.giantTime;
         float form = easeOut(ca / 16.0F);
         float fade = ca > 205.0F ? Math.max(0.0F, 1.0F - (ca - 205.0F) / 45.0F) : 1.0F;
         if (!(fade <= 0.0F)) {
            float H = 70.0F * form * (0.5F + 0.5F * fade);
            Vec3 base = new Vec3(e.cageX - ex, e.cageY - ey - 0.05, e.cageZ - ez);
            twister(pose, buf, base, H, t, 0, fade, 3.2F, 22.0F, 4.0F, 44, 6.0F);
            if (ca >= 9.6F) {
               Matrix4f m = pose.last().pose();
               Vec3 c = base.add(0.0, 1.2, 0.0);
               VertexConsumer vc = Fx.buf(buf, Fx.WAVE);

               for (int r = 0; r < 3; r++) {
                  float tilt = (float)r * 60.0F + t * 4.0F;
                  Vec3 axis = new Vec3(
                     Math.sin(Math.toRadians((double)tilt)), Math.cos(Math.toRadians((double)tilt)), Math.sin(Math.toRadians((double)tilt * 0.7)) * 0.5
                  );
                  Fx.ringAround(vc, m, c, axis, 1.0F, 1.35F, 40, k(GREY_LIGHT, 0.7F * fade), t * 0.05F);
               }

               emblem(buf, pose, cam, base.x, base.y + 14.0, base.z, 7.0F * form, 0.9F * fade);
            }
         }
      }
   }

   private static void twister(
      PoseStack pose, MultiBufferSource buf, Vec3 base, float H, float t, int seed, float fade, float baseR, float topR, float swayK, int SEG, float tile
   ) {
      if (!(H < 0.2F)) {
         int SL = 26;
         float[][] cx = new float[SL + 1][3];
         float[] rad = new float[SL + 1];

         for (int j = 0; j <= SL; j++) {
            float f = (float)j / (float)SL;
            float h = f * H;
            float sway = Mth.sin(t * 0.11F + (float)seed * 1.9F) * 1.1F * f * f * swayK;
            float wig = Mth.sin(h * 0.55F / swayK - t * 0.32F + (float)seed) * 0.45F * f * swayK;
            float wig2 = Mth.cos(h * 0.42F / swayK - t * 0.27F + (float)seed * 2.3F) * 0.45F * f * swayK;
            cx[j][0] = (float)base.x + sway + wig;
            cx[j][1] = (float)base.y + h;
            cx[j][2] = (float)base.z + sway * 0.6F + wig2;
            rad[j] = (baseR + (float)Math.pow((double)f, 1.5) * (topR - baseR) + 0.12F * baseR * Mth.sin(t * 0.7F + (float)j)) * (0.6F + 0.4F * fade);
         }

         Matrix4f m = pose.last().pose();

         for (int layer = 0; layer < 3; layer++) {
            float rs = layer == 0 ? 1.0F : (layer == 1 ? 0.72F : 1.12F);
            float a = (layer == 0 ? 0.85F : (layer == 1 ? 0.95F : 0.4F)) * fade;
            float[] cb = layer == 1 ? new float[]{0.36F, 0.37F, 0.39F} : (layer == 0 ? new float[]{0.56F, 0.57F, 0.6F} : new float[]{0.75F, 0.76F, 0.78F});
            float[] ct = layer == 1 ? new float[]{0.5F, 0.51F, 0.54F} : new float[]{0.7F, 0.71F, 0.74F};
            VertexConsumer vc = Fx.bufT(buf, Fx.SMOKE, a);
            float spin = (layer == 1 ? -1.1F : 0.75F) * t * 0.09F;

            for (int j = 0; j < SL; j++) {
               float f0 = (float)j / (float)SL;
               float f1 = (float)(j + 1) / (float)SL;
               float[] c0 = Fx.lerp(f0, cb, ct);
               float[] c1 = Fx.lerp(f1, cb, ct);
               float tw0 = spin + f0 * 1.6F;
               float tw1 = spin + f1 * 1.6F;

               for (int i = 0; i < SEG; i++) {
                  float a0 = (float)i * (float) (Math.PI * 2) / (float)SEG;
                  float a1 = (float)(i + 1) * (float) (Math.PI * 2) / (float)SEG;
                  float r0 = rad[j] * rs;
                  float r1 = rad[j + 1] * rs;
                  float[] p00 = new float[]{cx[j][0] + Mth.cos(a0) * r0, cx[j][1], cx[j][2] + Mth.sin(a0) * r0};
                  float[] p01 = new float[]{cx[j + 1][0] + Mth.cos(a0) * r1, cx[j + 1][1], cx[j + 1][2] + Mth.sin(a0) * r1};
                  float[] p11 = new float[]{cx[j + 1][0] + Mth.cos(a1) * r1, cx[j + 1][1], cx[j + 1][2] + Mth.sin(a1) * r1};
                  float[] p10 = new float[]{cx[j][0] + Mth.cos(a1) * r0, cx[j][1], cx[j][2] + Mth.sin(a1) * r0};
                  float u0 = (float)i / (float)SEG * tile;
                  float u1 = (float)(i + 1) / (float)SEG * tile;
                  Fx.v(vc, m, p00[0], p00[1], p00[2], c0[0], c0[1], c0[2], u0 + tw0, f0 * tile - t * 0.04F);
                  Fx.v(vc, m, p01[0], p01[1], p01[2], c1[0], c1[1], c1[2], u0 + tw1, f1 * tile - t * 0.04F);
                  Fx.v(vc, m, p11[0], p11[1], p11[2], c1[0], c1[1], c1[2], u1 + tw1, f1 * tile - t * 0.04F);
                  Fx.v(vc, m, p10[0], p10[1], p10[2], c0[0], c0[1], c0[2], u1 + tw0, f0 * tile - t * 0.04F);
                  Fx.v(vc, m, p10[0], p10[1], p10[2], c0[0], c0[1], c0[2], u1 + tw0, f0 * tile - t * 0.04F);
                  Fx.v(vc, m, p11[0], p11[1], p11[2], c1[0], c1[1], c1[2], u1 + tw1, f1 * tile - t * 0.04F);
                  Fx.v(vc, m, p01[0], p01[1], p01[2], c1[0], c1[1], c1[2], u0 + tw1, f1 * tile - t * 0.04F);
                  Fx.v(vc, m, p00[0], p00[1], p00[2], c0[0], c0[1], c0[2], u0 + tw0, f0 * tile - t * 0.04F);
               }
            }

            Fx.alpha = 1.0F;
         }

         VertexConsumer vc = Fx.bufT(buf, Fx.SMOKE, 0.7F * fade);

         for (int s = 0; s < 3; s++) {
            float ph = t * 0.35F + (float)s * (float) (Math.PI * 2) / 3.0F + (float)seed;

            for (int j = 0; j < SL; j++) {
               float f0 = (float)j / (float)SL;
               float f1 = (float)(j + 1) / (float)SL;
               float a0 = ph + f0 * 9.0F;
               float a1 = ph + f1 * 9.0F;
               float r0 = rad[j] * 1.05F;
               float r1 = rad[j + 1] * 1.05F;
               float w = (0.18F + f0 * 0.3F) * baseR * 0.6F;
               float x0 = cx[j][0] + Mth.cos(a0) * r0;
               float z0 = cx[j][2] + Mth.sin(a0) * r0;
               float x1 = cx[j + 1][0] + Mth.cos(a1) * r1;
               float z1 = cx[j + 1][2] + Mth.sin(a1) * r1;
               float[] dc = new float[]{0.3F, 0.29F, 0.28F};
               Fx.quad(
                  vc,
                  m,
                  new float[]{x0, cx[j][1] - w, z0},
                  new float[]{x0, cx[j][1] + w, z0},
                  new float[]{x1, cx[j + 1][1] + w, z1},
                  new float[]{x1, cx[j + 1][1] - w, z1},
                  f0 * 3.0F,
                  0.0F,
                  f1 * 3.0F,
                  1.0F,
                  dc,
                  dc
               );
            }
         }

         Fx.alpha = 1.0F;
         pose.pushPose();
         pose.translate(cx[0][0], cx[0][1], cx[0][2]);
         Matrix4f pm = pose.last().pose();
         vc = Fx.bufT(buf, Fx.SMOKE, 0.5F * fade);
         Fx.band(
            vc,
            pm,
            baseR * 0.2F,
            0.0F,
            baseR * 1.4F,
            baseR * 0.4F,
            32,
            new float[]{0.5F, 0.47F, 0.42F},
            new float[]{0.62F, 0.6F, 0.56F},
            2.0F,
            -t * 0.12F,
            1.0F,
            0.0F
         );
         Fx.alpha = 1.0F;
         pose.popPose();
      }
   }

   private static void cageBlast(LeoneEntity e, float ba, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      float f = 1.0F - ba / 26.0F;
      Vec3 c = new Vec3(e.cageX - ex, e.cageY - ey, e.cageZ - ez);
      pose.pushPose();
      pose.translate(c.x, c.y, c.z);
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
      float r = 0.5F + easeOut(ba / 12.0F) * 8.0F;
      Fx.ring(vc, m, 0.08F, r - 1.8F, r, 64, BLACK, k(GREY_LIGHT, 1.5F * f), 6.0F, 0.0F);
      vc = Fx.buf(buf, Fx.SOFT);
      Fx.sphere(vc, m, 2.4F, 0.5F + easeOut(ba / 10.0F) * 4.5F, (float) Math.PI, 12, 24, k(GREY_LIGHT, 0.5F * f * f), 2.0F, 1.0F, 0.0F, 0.0F);
      vc = Fx.buf(buf, Fx.FLARE);
      Fx.billboard(vc, pose, cam, 0.0, 2.4, 0.0, 8.0F * f, t * 3.0F, k(new float[]{0.8F, 1.0F, 0.85F}, 1.1F * f));
      emblem(buf, pose, cam, 0.0, 4.9, 0.0, 6.0F * easeOut(ba / 6.0F), f);
      pose.popPose();
   }
}
