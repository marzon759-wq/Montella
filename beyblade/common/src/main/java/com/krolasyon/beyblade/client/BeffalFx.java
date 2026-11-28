package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.entity.BeffalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.awt.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class BeffalFx {
   public static final ResourceLocation PEACOCK = V.rl("librabeyblade", "textures/fx/peacock.png");
   public static final ResourceLocation FEATHER = V.rl("librabeyblade", "textures/fx/eyefeather.png");
   public static final ResourceLocation EYE = V.rl("librabeyblade", "textures/fx/evileye.png");
   private static final float[] PINK = new float[]{1.0F, 0.35F, 0.75F};
   private static final float[] VIOLET = new float[]{0.65F, 0.25F, 1.0F};
   private static final float[] GOLD = new float[]{1.0F, 0.8F, 0.35F};
   private static final float[] WHITE = new float[]{1.0F, 1.0F, 1.0F};
   private static final float[] BLACK = new float[]{0.0F, 0.0F, 0.0F};

   private BeffalFx() {
   }

   private static float[] k(float[] c, float k) {
      return new float[]{c[0] * k, c[1] * k, c[2] * k};
   }

   private static float easeOut(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return 1.0F - (1.0F - x) * (1.0F - x) * (1.0F - x);
   }

   private static float[] rainbow(float h, float k) {
      int c = Color.HSBtoRGB(h - (float)Math.floor((double)h), 0.75F, 1.0F);
      return new float[]{(float)(c >> 16 & 0xFF) / 255.0F * k, (float)(c >> 8 & 0xFF) / 255.0F * k, (float)(c & 0xFF) / 255.0F * k};
   }

   private static void upright(
      MultiBufferSource buf, ResourceLocation tex, Quaternionf cam, Matrix4f m, double x, double y, double z, float w, float h, float a
   ) {
      if (!(a <= 0.01F) && !(w <= 0.01F)) {
         Vector3f right = new Vector3f(1.0F, 0.0F, 0.0F);
         cam.transform(right);
         Vec3 r = new Vec3((double)right.x(), 0.0, (double)right.z());
         if (r.lengthSqr() < 1.0E-4) {
            r = new Vec3(1.0, 0.0, 0.0);
         }

         r = r.normalize().scale((double)(w / 2.0F));
         VertexConsumer vc = Fx.bufT(buf, tex, a);
         float[] p0 = new float[]{(float)(x - r.x), (float)(y + (double)h), (float)(z - r.z)};
         float[] p1 = new float[]{(float)(x - r.x), (float)y, (float)(z - r.z)};
         float[] p2 = new float[]{(float)(x + r.x), (float)y, (float)(z + r.z)};
         float[] p3 = new float[]{(float)(x + r.x), (float)(y + (double)h), (float)(z + r.z)};
         Fx.quad(vc, m, p0, p1, p2, p3, 0.0F, 0.0F, 1.0F, 1.0F, WHITE, WHITE);
         Fx.alpha = 1.0F;
      }
   }

   private static void feather(VertexConsumer vc, Matrix4f m, Vec3 p, Vec3 dir, float len, float w) {
      Vec3 d = dir.normalize();
      Vec3 side = d.cross(new Vec3(0.0, 1.0, 0.0));
      if (side.lengthSqr() < 1.0E-4) {
         side = new Vec3(1.0, 0.0, 0.0);
      }

      side = side.normalize().scale((double)(w / 2.0F));
      Vec3 up = side.cross(d).normalize().scale((double)(w / 2.0F));
      Vec3 tail = p.subtract(d.scale((double)len));

      for (Vec3 s : new Vec3[]{side, up}) {
         float[] a = new float[]{(float)(p.x - s.x), (float)(p.y - s.y), (float)(p.z - s.z)};
         float[] b = new float[]{(float)(tail.x - s.x), (float)(tail.y - s.y), (float)(tail.z - s.z)};
         float[] c = new float[]{(float)(tail.x + s.x), (float)(tail.y + s.y), (float)(tail.z + s.z)};
         float[] e = new float[]{(float)(p.x + s.x), (float)(p.y + s.y), (float)(p.z + s.z)};
         Fx.quad(vc, m, a, b, c, e, 0.0F, 0.0F, 1.0F, 1.0F, WHITE, WHITE);
      }
   }

   public static void render(BeffalEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      Matrix4f m = pose.last().pose();
      if (e.isShielded()) {
         float p = 0.8F + 0.2F * Mth.sin(t * 0.5F);
         upright(buf, PEACOCK, cam, m, 0.0, -0.1, 0.0, 3.6F + 0.15F * Mth.sin(t * 0.3F), 3.6F, 0.8F * p);
         VertexConsumer vc = Fx.bufT(buf, Fx.HEX, 0.45F * p);
         Fx.band(vc, m, 1.15F, 0.0F, 1.25F, 1.4F, 30, PINK, k(VIOLET, 0.4F), 3.0F, t * 0.05F, 1.0F, 0.0F);
         Fx.alpha = 1.0F;
         vc = Fx.buf(buf, Fx.SOFT);
         Fx.ring(vc, m, 0.05F, 0.9F, 1.3F, 40, k(PINK, 0.7F * p), BLACK, 1.0F, 0.0F);
      }

      renderGaze(e, t, ex, ey, ez, pose, buf, cam);
      renderVolleys(e, partial, t, pose, buf);
      float ms = t - (float)e.mirageStrikeTime;
      if (ms >= 0.0F && ms < 8.0F) {
         VertexConsumer vc = Fx.buf(buf, Fx.FLARE);
         Fx.billboard(vc, pose, cam, 0.0, 0.6, 0.0, 3.0F * (1.0F - ms / 8.0F), t * 10.0F, rainbow(t * 0.05F, 1.2F * (1.0F - ms / 8.0F)));
      }

      float hit = t - (float)e.requiemHitTime;
      if (hit >= 0.0F && hit < 40.0F) {
         requiemImpact(e, hit, t, ex, ey, ez, pose, buf, cam);
      }

      int ab = e.clientAbility;
      float at = e.abilityAge(partial);
      if (ab != 0 && !(at < 0.0F)) {
         switch (ab) {
            case 4:
               mirage(e, at, t, pose, buf);
               break;
            case 5:
               requiem(e, at, t, ex, ey, ez, pose, buf, cam);
         }
      }
   }

   private static void renderGaze(BeffalEntity e, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      float g = t - (float)e.gazeTime;
      if (!(g < 0.0F) && !(g > 94.0F)) {
         Entity tg = e.level().getEntity(e.gazeTargetId);
         Matrix4f m = pose.last().pose();
         if (g < 40.0F) {
            float open = easeOut(g / 10.0F) * Mth.clamp((40.0F - g) / 8.0F, 0.0F, 1.0F);
            VertexConsumer vc = Fx.bufT(buf, EYE, 0.95F * open);
            Vec3[] eyes = new Vec3[8];

            for (int i = 0; i < 8; i++) {
               double a = (double)i * Math.PI / 4.0 + (double)t * 0.03;
               eyes[i] = new Vec3(Math.cos(a) * 1.5, 1.1 + 0.25 * Math.sin((double)t * 0.2 + (double)i), Math.sin(a) * 1.5);
               Fx.billboard(vc, pose, cam, eyes[i].x, eyes[i].y, eyes[i].z, 0.75F * open, 0.0F, WHITE);
            }

            Fx.alpha = 1.0F;
            if (tg != null && g >= 10.0F && g < 26.0F) {
               float f = 1.0F - Math.abs(g - 18.0F) / 8.0F;
               Vec3 to = new Vec3(tg.getX() - ex, tg.getY() - ey + 0.3, tg.getZ() - ez);
               vc = Fx.buf(buf, Fx.SOFT);

               for (Vec3 p : eyes) {
                  Fx.beam(vc, m, p, to, 0.14F * f, k(PINK, 1.3F * f), k(VIOLET, 1.3F * f), 0.5F, 0.0F);
               }
            }
         }

         if (tg != null && g >= 8.0F) {
            float life = Mth.clamp((94.0F - g) / 12.0F, 0.0F, 1.0F) * Mth.clamp((g - 8.0F) / 6.0F, 0.0F, 1.0F);
            double tx = tg.getX() - ex;
            double ty = tg.getY() - ey;
            double tz = tg.getZ() - ez;
            VertexConsumer vc = Fx.bufT(buf, EYE, 0.95F * life);
            Fx.billboard(vc, pose, cam, tx, ty + 1.9 + 0.1 * Math.sin((double)t * 0.15), tz, 1.5F * life, 0.0F, WHITE);
            Fx.alpha = 1.0F;
            pose.pushPose();
            pose.translate(tx, ty, tz);
            Matrix4f mt = pose.last().pose();
            vc = Fx.buf(buf, Fx.SOFT);

            for (int i = 0; i < 2; i++) {
               Fx.helix(
                  vc,
                  mt,
                  0.0F,
                  1.4F,
                  0.9F,
                  0.3F,
                  -t * 0.35F + (float)i * (float) Math.PI,
                  2.2F,
                  0.12F,
                  30,
                  k(VIOLET, 1.1F * life),
                  k(PINK, 0.4F * life),
                  t * 0.1F
               );
            }

            vc = Fx.buf(buf, Fx.WAVE);
            Fx.ring(vc, mt, 0.06F, 0.4F, 1.1F, 40, BLACK, k(VIOLET, 0.9F * life), 3.0F, -t * 0.08F);
            pose.popPose();
         }
      }
   }

   private static void renderVolleys(BeffalEntity e, float partial, float t, PoseStack pose, MultiBufferSource buf) {
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = null;

      for (int s = 0; s < e.volleyTime.length; s++) {
         float a = t - (float)e.volleyTime[s];
         if (!(a < 0.0F) && !(a > 9.0F)) {
            if (vc == null) {
               vc = Fx.bufT(buf, FEATHER, 0.95F);
            }

            Fx.alpha = 0.95F * Mth.clamp((9.0F - a) / 3.0F, 0.0F, 1.0F);
            Vec3 dir = new Vec3(e.volleyDir[s][0], 0.0, e.volleyDir[s][2]);
            float dist = 0.6F + easeOut(a / 7.0F) * 11.0F;

            for (int i = 0; i < 5; i++) {
               double rot = ((double)i - 2.0) * 0.17;
               Vec3 d = new Vec3(dir.x * Math.cos(rot) - dir.z * Math.sin(rot), 0.02, dir.x * Math.sin(rot) + dir.z * Math.cos(rot));
               Vec3 p = d.scale((double)dist).add(0.0, 0.55 + 0.12 * Math.sin((double)i * 1.7 + (double)a), 0.0);
               feather(vc, m, p, d, 1.3F, 0.55F);
            }
         }
      }

      Fx.alpha = 1.0F;
   }

   private static void mirage(BeffalEntity e, float at, float t, PoseStack pose, MultiBufferSource buf) {
      float f = Mth.clamp(at / 8.0F, 0.0F, 1.0F) * Mth.clamp(((float)BeffalEntity.BF_DURATION[4] - at) / 8.0F, 0.0F, 1.0F);

      for (int i = 0; i < 3; i++) {
         double a = (double)t * 0.22 * (double)e.spinDir() + (double)i * Math.PI * 2.0 / 3.0;
         double x = Math.cos(a) * 1.7;
         double z = Math.sin(a) * 1.7;
         pose.pushPose();
         pose.translate(x, 0.02, z);
         pose.mulPose(Axis.YP.rotationDegrees(-t * 40.0F * (float)e.spinDir()));
         pose.scale(0.45F, 0.45F, 0.45F);
         BeyMesh.BEFFAL.renderGhostDisc(pose, buf, 15728880, 0.5F * f, BeyMesh.BEFFAL.TOP_Y - 0.2F);
         pose.popPose();
         pose.pushPose();
         pose.translate(x, 0.0, z);
         VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
         Fx.ring(vc, pose.last().pose(), 0.04F, 0.3F, 0.65F, 24, rainbow(t * 0.03F + (float)i / 3.0F, 0.8F * f), BLACK, 1.0F, 0.0F);
         pose.popPose();
      }

      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
      Fx.ring(vc, m, 0.06F, 1.35F, 2.05F, 60, BLACK, rainbow(t * 0.02F, 0.7F * f), 6.0F, t * 0.04F);
   }

   private static void requiem(BeffalEntity e, float at, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      Matrix4f m = pose.last().pose();
      float form = easeOut(at / 28.0F);
      float fade = Mth.clamp((74.0F - at) / 10.0F, 0.0F, 1.0F);
      upright(buf, PEACOCK, cam, m, 0.0, -1.4, 0.0, 10.5F * form, 10.5F * form, 0.85F * fade);
      VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
      Fx.ring(vc, m, -1.2F, 0.6F, 2.4F * form, 40, k(VIOLET, 0.8F * fade), BLACK, 1.0F, 0.0F);
      if (e.requiemTime >= e.clientAbilityStart) {
         double sx = e.reqX - ex;
         double sy = e.reqY - ey;
         double sz = e.reqZ - ez;
         float shrink = at < 34.0F ? 1.0F : 1.0F - 0.35F * Mth.clamp((at - 34.0F) / 26.0F, 0.0F, 1.0F);
         Vec3 target = new Vec3(sx, sy + 0.3, sz);
         Vec3[] eyes = new Vec3[12];

         for (int i = 0; i < 12; i++) {
            double a = (double)i * Math.PI * 2.0 / 12.0 + (double)t * 0.04;
            eyes[i] = new Vec3(
               sx + Math.cos(a) * 4.2 * (double)shrink, sy + 1.6 + 0.4 * Math.sin(a * 2.0 + (double)t * 0.1), sz + Math.sin(a) * 4.2 * (double)shrink
            );
         }

         vc = Fx.bufT(buf, EYE, 0.95F);

         for (int i = 0; i < 12; i++) {
            float appear = Mth.clamp((at - 4.0F - (float)i * 1.8F) / 4.0F, 0.0F, 1.0F);
            if (!(appear <= 0.0F)) {
               Fx.alpha = 0.95F * appear;
               Fx.billboard(vc, pose, cam, eyes[i].x, eyes[i].y, eyes[i].z, 1.1F * appear, 0.0F, WHITE);
            }
         }

         Fx.alpha = 1.0F;
         if (at >= 34.0F && at < 60.0F) {
            float pulse = 0.6F + 0.4F * Mth.sin(t * 1.3F);
            vc = Fx.buf(buf, Fx.SOFT);

            for (int ix = 0; ix < 12; ix++) {
               Fx.beam(vc, m, eyes[ix], target, 0.18F * pulse, k(PINK, 1.4F * pulse), k(VIOLET, 1.4F * pulse), 0.5F, 0.0F);
            }

            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, target.x, target.y, target.z, 2.2F * pulse, t * 6.0F, k(PINK, pulse));
         }

         pose.pushPose();
         pose.translate(sx, sy + 0.08, sz);
         vc = Fx.buf(buf, Fx.WAVE);
         Fx.ring(vc, pose.last().pose(), 0.0F, 4.2F * shrink - 0.4F, 4.2F * shrink + 0.2F, 60, BLACK, rainbow(t * 0.02F, 1.0F * form), 8.0F, t * 0.03F);
         vc = Fx.buf(buf, Fx.SIGIL);
         Fx.groundQuad(vc, pose.last().pose(), 0.02F, 4.2F * shrink, k(VIOLET, 0.7F * form));
         pose.popPose();
      }
   }

   private static void requiemImpact(
      BeffalEntity e, float hit, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam
   ) {
      float f = 1.0F - hit / 40.0F;
      double sx = e.reqX - ex;
      double sy = e.reqY - ey;
      double sz = e.reqZ - ez;
      pose.pushPose();
      pose.translate(sx, sy + 0.1, sz);
      Matrix4f m = pose.last().pose();
      VertexConsumer vc = Fx.buf(buf, Fx.WAVE);
      float r = 0.5F + easeOut(hit / 14.0F) * 9.0F;
      Fx.ring(vc, m, 0.05F, r - 2.0F, r, 72, BLACK, k(PINK, 1.5F * f), 6.0F, 0.0F);
      Fx.ring(vc, m, 0.08F, r * 0.6F - 1.0F, r * 0.6F, 72, BLACK, rainbow(t * 0.05F, 1.2F * f), 6.0F, 0.3F);
      vc = Fx.bufT(buf, FEATHER, 0.9F * f);
      float d = 0.5F + easeOut(hit / 12.0F) * 6.0F;

      for (int i = 0; i < 16; i++) {
         double a = (double)i * Math.PI / 8.0 + 0.2;
         Vec3 dir = new Vec3(Math.cos(a), 0.25, Math.sin(a));
         feather(vc, m, dir.scale((double)d).add(0.0, 0.4, 0.0), dir, 1.4F, 0.6F);
      }

      Fx.alpha = 1.0F;
      if (hit < 12.0F) {
         float g = 1.0F - hit / 12.0F;
         vc = Fx.buf(buf, Fx.FLARE);
         Fx.billboard(vc, pose, cam, 0.0, 1.2, 0.0, 12.0F * g, t * 2.0F, k(new float[]{1.0F, 0.5F, 0.85F}, 1.3F * g));
      }

      pose.popPose();
   }
}
