package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.VClient;
import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.Loadout;
import com.krolasyon.beyblade.parts.PartType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class PartLathe {
   private static final int SEG = 36;
   private static final float PER_TENTH_MM = 0.0034482758F;

   private PartLathe() {
   }

   public static float tipTop(BeyPart.Tip t) {
      return switch (t) {
         case FINAL_DRIVE, FINAL_SURVIVE -> 0.42F;
         case WIDE_DEFENSE, ETERNAL_WIDE_DEFENSE, WIDE_BALL, WIDE_FLAT, WIDE_SEMI_FLAT, GRAVITY_BALL, RUBBER_DEFENSE_FLAT -> 0.38F;
         case BEARING_DRIVE -> 0.4F;
         default -> 0.36F;
      };
   }

   public static float trackHeight(BeyPart track) {
      return track == null ? 0.0F : (float)track.height() * 0.0034482758F;
   }

   public static float wheelBottom(Loadout lo) {
      BeyPart tip = lo.get(PartType.TIP);
      return (tip == null ? 0.05F : tipTop(tip.tip())) + trackHeight(lo.get(PartType.TRACK));
   }

   private static List<PartLathe.P> tipProfile(BeyPart.Tip t, float top) {
      List<PartLathe.P> p = new ArrayList<>();
      p.add(new PartLathe.P(0.26F, top, false));
      switch (t) {
         case FINAL_DRIVE:
         case FINAL_SURVIVE:
            p.add(new PartLathe.P(0.36F, top - 0.02F, false));
            p.add(new PartLathe.P(0.38F, top - 0.12F, false));
            p.add(new PartLathe.P(0.36F, top - 0.2F, false));
            p.add(new PartLathe.P(0.22F, 0.1F, true));
            p.add(new PartLathe.P(t == BeyPart.Tip.FINAL_DRIVE ? 0.16F : 0.08F, 0.01F, true));
            break;
         case WIDE_DEFENSE:
         case ETERNAL_WIDE_DEFENSE:
            p.add(new PartLathe.P(0.42F, top - 0.08F, false));
            p.add(new PartLathe.P(t == BeyPart.Tip.ETERNAL_WIDE_DEFENSE ? 0.46F : 0.44F, top - 0.16F, false));
            p.add(new PartLathe.P(0.32F, 0.1F, true));
            p.add(new PartLathe.P(0.1F, 0.01F, true));
            break;
         case WIDE_BALL:
         case BALL:
            float w = t == BeyPart.Tip.WIDE_BALL ? 0.4F : 0.31F;
            p.add(new PartLathe.P(w, top - 0.1F, false));

            for (int i = 1; i <= 4; i++) {
               double a = (double)i / 4.0 * Math.PI / 2.0;
               float y = (float)((double)(top - 0.1F) * Math.cos(a));
               p.add(new PartLathe.P((float)((double)w * Math.cos(a * 0.85)) * 0.95F, y, i >= 2));
            }
            break;
         case FLAT:
         case METAL_FLAT:
         case EXTREME_FLAT:
            float c = t.contact;
            p.add(new PartLathe.P(0.31F, top - 0.08F, false));
            p.add(new PartLathe.P(0.31F, 0.12F, false));
            p.add(new PartLathe.P(c + 0.03F, 0.03F, true));
            p.add(new PartLathe.P(c, 0.0F, true));
            break;
         case RUBBER_FLAT:
         case RUBBER_SEMI_FLAT:
            p.add(new PartLathe.P(0.31F, top - 0.08F, false));
            p.add(new PartLathe.P(0.31F, 0.16F, false));
            p.add(new PartLathe.P(t.contact + 0.06F, 0.12F, true));
            p.add(new PartLathe.P(t.contact + 0.04F, 0.02F, true));
            p.add(new PartLathe.P(t.contact, 0.0F, true));
            break;
         case SEMI_FLAT:
            p.add(new PartLathe.P(0.3F, top - 0.08F, false));
            p.add(new PartLathe.P(0.28F, 0.14F, false));
            p.add(new PartLathe.P(0.16F, 0.03F, true));
            p.add(new PartLathe.P(t.contact, 0.0F, true));
            break;
         case SHARP:
         case ETERNAL_SHARP:
         case COAT_SHARP:
            p.add(new PartLathe.P(0.3F, top - 0.08F, false));
            p.add(new PartLathe.P(0.27F, 0.17F, false));
            p.add(new PartLathe.P(0.12F, 0.06F, true));
            p.add(new PartLathe.P(0.03F, 0.008F, true));
            break;
         case DEFENSE:
            p.add(new PartLathe.P(0.34F, top - 0.08F, false));
            p.add(new PartLathe.P(0.34F, 0.18F, false));
            p.add(new PartLathe.P(0.22F, 0.06F, true));
            p.add(new PartLathe.P(0.06F, 0.005F, true));
            break;
         case HOLE_FLAT:
            p.add(new PartLathe.P(0.31F, top - 0.08F, false));
            p.add(new PartLathe.P(0.31F, 0.12F, false));
            p.add(new PartLathe.P(0.23F, 0.03F, true));
            p.add(new PartLathe.P(0.2F, 0.0F, true));
            p.add(new PartLathe.P(0.1F, 0.0F, true));
            p.add(new PartLathe.P(0.08F, 0.05F, false));
            p.add(new PartLathe.P(0.0F, 0.05F, false));
            break;
         case WIDE_FLAT:
            p.add(new PartLathe.P(0.42F, top - 0.08F, false));
            p.add(new PartLathe.P(0.42F, 0.12F, false));
            p.add(new PartLathe.P(0.33F, 0.03F, true));
            p.add(new PartLathe.P(0.3F, 0.0F, true));
            break;
         case WIDE_SEMI_FLAT:
            p.add(new PartLathe.P(0.42F, top - 0.08F, false));
            p.add(new PartLathe.P(0.38F, 0.14F, false));
            p.add(new PartLathe.P(0.22F, 0.03F, true));
            p.add(new PartLathe.P(0.16F, 0.0F, true));
            break;
         case RUBBER_SHARP:
         case METAL_SHARP:
            p.add(new PartLathe.P(0.3F, top - 0.08F, false));
            p.add(new PartLathe.P(0.27F, 0.19F, t == BeyPart.Tip.METAL_SHARP));
            p.add(new PartLathe.P(0.14F, 0.07F, true));
            p.add(new PartLathe.P(0.04F, 0.01F, true));
            break;
         case RUBBER_BALL:
         case METAL_BALL:
         case GRAVITY_BALL:
            float bw = t == BeyPart.Tip.GRAVITY_BALL ? 0.36F : 0.31F;
            p.add(new PartLathe.P(bw, top - 0.1F, t == BeyPart.Tip.METAL_BALL));
            for (int i = 1; i <= 5; i++) {
               double a = (double)i / 5.0 * Math.PI / 2.0;
               float y = (float)((double)(top - 0.1F) * Math.cos(a));
               p.add(new PartLathe.P((float)((double)bw * Math.cos(a * 0.85)) * 0.95F, y, i >= 2 || t == BeyPart.Tip.METAL_BALL));
            }
            break;
         case RUBBER_DEFENSE_FLAT:
            p.add(new PartLathe.P(0.34F, top - 0.08F, false));
            p.add(new PartLathe.P(0.34F, 0.18F, false));
            p.add(new PartLathe.P(0.26F, 0.06F, true));
            p.add(new PartLathe.P(0.22F, 0.0F, true));
            break;
         case LEFT_RUBBER_FLAT:
            p.add(new PartLathe.P(0.31F, top - 0.08F, false));
            p.add(new PartLathe.P(0.31F, 0.16F, false));
            p.add(new PartLathe.P(t.contact + 0.06F, 0.12F, true));
            p.add(new PartLathe.P(t.contact + 0.04F, 0.02F, true));
            p.add(new PartLathe.P(t.contact, 0.0F, true));
            break;
         case SEMI_DEFENSE:
            p.add(new PartLathe.P(0.34F, top - 0.08F, false));
            p.add(new PartLathe.P(0.32F, 0.16F, false));
            p.add(new PartLathe.P(0.2F, 0.05F, true));
            p.add(new PartLathe.P(0.1F, 0.0F, true));
            break;
         case DEFENSE_SHARP:
            p.add(new PartLathe.P(0.34F, top - 0.08F, false));
            p.add(new PartLathe.P(0.34F, 0.18F, false));
            p.add(new PartLathe.P(0.14F, 0.07F, true));
            p.add(new PartLathe.P(0.04F, 0.008F, true));
            break;
         case QUAKE:
            p.add(new PartLathe.P(0.31F, top - 0.08F, false));
            p.add(new PartLathe.P(0.31F, 0.14F, false));
            p.add(new PartLathe.P(0.24F, 0.05F, true));
            p.add(new PartLathe.P(0.2F, 0.0F, true));
            p.add(new PartLathe.P(0.14F, 0.035F, true));
            p.add(new PartLathe.P(0.08F, 0.0F, true));
            break;
         case BEARING_DRIVE:
            p.add(new PartLathe.P(0.3F, top - 0.08F, false));
            p.add(new PartLathe.P(0.3F, 0.2F, false));
            p.add(new PartLathe.P(0.22F, 0.17F, true));
            p.add(new PartLathe.P(0.2F, 0.13F, true));
            p.add(new PartLathe.P(0.12F, 0.06F, false));
            p.add(new PartLathe.P(0.03F, 0.008F, true));
            break;
         default:
            p.add(new PartLathe.P(0.3F, top - 0.08F, false));
            p.add(new PartLathe.P(0.1F, 0.02F, true));
      }

      p.add(new PartLathe.P(0.0F, 0.0F, true));
      return p;
   }

   private static List<PartLathe.P> trackProfile(BeyPart track, float bottom, float top) {
      List<PartLathe.P> p = new ArrayList<>();
      p.add(new PartLathe.P(0.3F, top, false));
      p.add(new PartLathe.P(0.3F, top - 0.04F, false));
      p.add(new PartLathe.P(0.21F, top - 0.08F, false));
      float h = top - bottom;
      float fr = 0.0F;
      float fy = 0.0F;
      float ft = 0.04F;
      switch (track.trait()) {
         case BOOST_DISK:
            fr = 0.52F;
            fy = 0.35F;
            ft = 0.06F;
            break;
         case CLAW:
            fr = 0.42F;
            fy = 0.6F;
            break;
         case WING:
            fr = 0.46F;
            fy = 0.45F;
            ft = 0.025F;
            break;
         case UPPER_WING:
            fr = 0.46F;
            fy = 0.75F;
            ft = 0.03F;
            break;
         case TORNADO:
            fr = 0.36F;
            fy = 0.5F;
            ft = 0.06F;
            break;
         case DEFENSE_RING:
            fr = 0.44F;
            fy = 0.22F;
            ft = 0.05F;
            break;
         case RUBBER_RING:
            fr = 0.34F;
            fy = 0.5F;
            ft = 0.08F;
            break;
         case TWIN_HEIGHT:
            fr = 0.36F;
            fy = 0.65F;
            ft = 0.04F;
            break;
         case GRAVITY:
            fr = 0.3F;
            fy = 0.5F;
            ft = 0.2F;
      }

      if (fr > 0.0F && h > 0.2F) {
         float yc = bottom + h * fy;
         p.add(new PartLathe.P(0.2F, yc + ft / 2.0F + 0.03F, false));
         p.add(new PartLathe.P(fr, yc + ft / 2.0F, true));
         p.add(new PartLathe.P(fr, yc - ft / 2.0F, true));
         p.add(new PartLathe.P(0.2F, yc - ft / 2.0F - 0.03F, false));
      }

      p.add(new PartLathe.P(0.19F, bottom + 0.04F, false));
      p.add(new PartLathe.P(0.26F, bottom, false));
      return p;
   }

   public static void renderTrack(PoseStack pose, VertexConsumer vc, int light, int overlay, Loadout lo) {
      BeyPart tip = lo.get(PartType.TIP);
      BeyPart track = lo.get(PartType.TRACK);
      float bottom = tip == null ? 0.05F : tipTop(tip.tip());
      if (track != null) {
         lathe(pose, vc, light, overlay, trackProfile(track, bottom, bottom + trackHeight(track)), track.color(), track.color2());
      }
   }

   public static void renderTip(PoseStack pose, VertexConsumer vc, int light, int overlay, Loadout lo) {
      BeyPart tip = lo.get(PartType.TIP);
      if (tip != null) {
         lathe(pose, vc, light, overlay, tipProfile(tip.tip(), tipTop(tip.tip())), tip.color(), tip.color2(), tip.tip().rubber());
      }
   }

   public static void render(PoseStack pose, VertexConsumer vc, int light, int overlay, Loadout lo) {
      BeyPart tip = lo.get(PartType.TIP);
      BeyPart track = lo.get(PartType.TRACK);
      float bottom = tip == null ? 0.05F : tipTop(tip.tip());
      if (track != null) {
         lathe(pose, vc, light, overlay, trackProfile(track, bottom, bottom + trackHeight(track)), track.color(), track.color2());
      }

      if (tip != null) {
         lathe(pose, vc, light, overlay, tipProfile(tip.tip(), bottom), tip.color(), tip.color2());
      }
   }

   private static void lathe(PoseStack pose, VertexConsumer vc, int light, int overlay, List<PartLathe.P> prof, int col, int accent) {
      lathe(pose, vc, light, overlay, prof, col, accent, false);
   }

   /** Turns a profile around the Y axis. Rubber contact rings are drawn matte with tread grooves. */
   private static void lathe(PoseStack pose, VertexConsumer vc, int light, int overlay, List<PartLathe.P> prof, int col, int accent, boolean rubber) {
      Matrix4f m = pose.last().pose();
      Matrix3f n = pose.last().normal();

      for (int k = 0; k < prof.size() - 1; k++) {
         PartLathe.P a = prof.get(k);
         PartLathe.P b = prof.get(k + 1);
         int c = b.accent ? accent : col;
         float cr = (float)(c >> 16 & 0xFF) / 255.0F;
         float cg = (float)(c >> 8 & 0xFF) / 255.0F;
         float cb = (float)(c & 0xFF) / 255.0F;
         float dr = b.r - a.r;
         float dy = b.y - a.y;

         for (int i = 0; i < 36; i++) {
            double t0 = (Math.PI * 2) * (double)i / 36.0;
            double t1 = (Math.PI * 2) * (double)(i + 1) / 36.0;
            float[][] q = new float[][]{{a.r, a.y, (float)t0, 0.0F}, {b.r, b.y, (float)t0, 1.0F}, {b.r, b.y, (float)t1, 1.0F}, {a.r, a.y, (float)t1, 0.0F}};

            for (float[] v : q) {
               float cos = (float)Math.cos((double)v[2]);
               float sin = (float)Math.sin((double)v[2]);
               float nx = -dy * cos;
               float ny = dr;
               float nz = -dy * sin;
               float len = (float)Math.sqrt((double)(nx * nx + dr * dr + nz * nz));
               if (len < 1.0E-5F) {
                  nx = 0.0F;
                  ny = -1.0F;
                  nz = 0.0F;
                  len = 1.0F;
               }

               float shade = rubber && b.accent
                  ? 0.62F + 0.1F * (float)Math.sin((double)(v[2] * 18.0F))
                  : 0.85F + 0.15F * (float)Math.sin((double)(v[2] * 4.0F));
               VClient.vertex(
                  vc,
                  m,
                  n,
                  v[0] * cos,
                  v[1],
                  v[0] * sin,
                  cr * shade,
                  cg * shade,
                  cb * shade,
                  1.0F,
                  v[2] / (float) (Math.PI * 2),
                  v[3],
                  overlay,
                  light,
                  nx / len,
                  ny / len,
                  nz / len
               );
            }
         }
      }
   }

   private static record P(float r, float y, boolean accent) {
   }
}
