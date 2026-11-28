package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.compat.VClient;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * L-Drago's dragon as a real 3D creature: a long Eastern dragon built every frame along a spline - a scaled,
 * tapering serpent body with dorsal fins and four clawed legs, a lofted crocodile head with horns, glowing eyes,
 * whiskers and a jaw that opens to roar fire. A glow shell around it gives the spirit look.
 *
 * The caller gives the spine (head first) in render space; {@link #coil} makes the usual path: the dragon winds
 * around its bey in a rising spiral with its head reared up.
 */
public final class DragonSpirit {
   private static final ResourceLocation SCALES = V.rl("librabeyblade", "textures/fx/dragon_scales.png");
   private static final ResourceLocation WHITE = V.rl("librabeyblade", "textures/fx/white.png");
   private static final int FULL = 15728880;
   private static final int SIDES = 14;
   // colours: dark crimson back, red flanks, gold belly, magenta glow
   private static final float[] BACK = {0.42F, 0.02F, 0.08F};
   private static final float[] FLANK = {0.95F, 0.12F, 0.16F};
   private static final float[] BELLY = {1.0F, 0.62F, 0.22F};
   private static final float[] GLOW = {1.0F, 0.18F, 0.38F};
   private static final float[] HORN = {1.0F, 0.88F, 0.6F};
   private static final Vec3 LIGHT = new Vec3(0.3, 0.9, 0.3).normalize();

   private DragonSpirit() {
   }

   // ------------------------------------------------------------------ path
   /**
    * Spine of a dragon coiled around (cx,cy,cz): {@code n} points head first. {@code rise} lifts the head,
    * {@code lunge} (0..1) straightens the front of the body toward {@code target} (relative), as for a strike.
    */
   public static Vec3[] coil(int n, double cx, double cy, double cz, float t, double radius, double height, float spinDir, Vec3 target, float lunge) {
      Vec3[] p = new Vec3[n];
      double headA = t * 0.09 * spinDir;
      for (int i = 0; i < n; i++) {
         double s = (double)i / (n - 1);
         double a = headA - s * Math.PI * 2.6 * spinDir;
         double r = radius * (0.75 + 0.35 * s) + 0.25 * Math.sin(t * 0.21 + s * 9.0);
         double y = height * (1.0 - s) * (1.0 - 0.15 * s) + 0.35 * Math.sin(t * 0.17 + s * 12.0) + 0.6;
         // the neck rears up above the coil
         if (s < 0.18) {
            double k = 1.0 - s / 0.18;
            y += k * k * height * 0.35;
            r -= k * radius * 0.35;
         }
         Vec3 q = new Vec3(cx + Math.cos(a) * r, cy + y, cz + Math.sin(a) * r);
         if (lunge > 0.0F && target != null && s < 0.45) {
            double k = (1.0 - s / 0.45);
            k = k * k * lunge;
            Vec3 anchor = new Vec3(cx, cy + height * 0.6, cz);
            Vec3 strike = anchor.lerp(target, 1.0 - s / 0.45 * 0.9);
            q = q.lerp(strike, k);
         }
         p[i] = q;
      }
      return p;
   }

   // ------------------------------------------------------------------ render
   /**
    * @param spine   head first, render space
    * @param size    body thickness scale (1 = 0.5 block at the thickest)
    * @param alpha   fade 0..1
    * @param jaw     jaw opening 0..1
    * @param fire    fire breath strength 0..1
    */
   public static void render(PoseStack pose, MultiBufferSource buf, Quaternionf cam, Vec3[] spine, float size, float t, float alpha, float jaw, float fire) {
      render(pose, buf, cam, spine, size, t, alpha, jaw, fire, null);
   }

   /** @param look where the head looks (render space point), or null to look along the neck */
   public static void render(PoseStack pose, MultiBufferSource buf, Quaternionf cam, Vec3[] spine, float size, float t, float alpha, float jaw, float fire, Vec3 look) {
      if (alpha <= 0.01F || spine.length < 4) {
         return;
      }
      int n = spine.length;
      Vec3[] tan = new Vec3[n];
      Vec3[] nor = new Vec3[n];
      Vec3[] bin = new Vec3[n];
      for (int i = 0; i < n; i++) {
         Vec3 a = spine[Math.max(0, i - 1)];
         Vec3 b = spine[Math.min(n - 1, i + 1)];
         tan[i] = b.subtract(a).normalize();
      }
      // parallel transport frames, the first "up" is world up
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      for (int i = 0; i < n; i++) {
         Vec3 nn = up.subtract(tan[i].scale(up.dot(tan[i])));
         if (nn.lengthSqr() < 1.0E-6) {
            nn = new Vec3(1.0, 0.0, 0.0);
         }
         nor[i] = nn.normalize();
         bin[i] = tan[i].cross(nor[i]).normalize();
         up = nor[i];
      }
      float[] rad = new float[n];
      for (int i = 0; i < n; i++) {
         float s = (float)i / (n - 1);
         float r;
         if (s < 0.06F) {
            r = 0.3F + s / 0.06F * 0.08F;
         } else if (s < 0.3F) {
            r = 0.38F + (s - 0.06F) / 0.24F * 0.14F;
         } else {
            float k = (s - 0.3F) / 0.7F;
            r = 0.52F * (1.0F - k * k * 0.92F);
         }
         rad[i] = r * size;
      }
      Matrix4f m = pose.last().pose();
      Matrix3f nm = pose.last().normal();
      // the head sits on the first spine point, looking along -tangent
      Vec3 fwd = tan[0].scale(-1.0);
      Vec3 hup = nor[0];
      {
         Vec3 want = look != null ? look.subtract(spine[0]) : new Vec3(spine[0].x, 0.0, spine[0].z);
         want = new Vec3(want.x, Math.max(-0.35 * want.horizontalDistance(), Math.min(0.15 * want.horizontalDistance(), want.y)), want.z);
         if (want.lengthSqr() > 1.0E-4) {
            fwd = fwd.scale(0.3).add(want.normalize().scale(0.7)).normalize();
            Vec3 side0 = fwd.cross(new Vec3(0.0, 1.0, 0.0));
            if (side0.lengthSqr() > 1.0E-4) {
               hup = side0.normalize().cross(fwd).normalize();
            }
         }
      }
      float hs = size * 1.45F;
      VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(SCALES));
      body(vc, m, nm, spine, nor, bin, rad, alpha, 1.0F, false);
      head(vc, null, m, nm, spine[0], fwd, hup, hs, alpha, jaw, t);
      // legs, fins, horns and teeth on the plain white texture
      VertexConsumer wv = buf.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
      fins(wv, m, nm, spine, tan, nor, rad, alpha, t);
      legs(wv, m, nm, spine, tan, nor, bin, rad, alpha, t, size);
      head(null, wv, m, nm, spine[0], fwd, hup, hs, alpha, jaw, t);
      horns(wv, m, nm, spine[0], fwd, hup, hs, alpha);
      // glow shell, eyes, whiskers, fire (additive)
      VertexConsumer gv = Fx.buf(buf, Fx.SOFT);
      body(gv, m, nm, spine, nor, bin, scaled(rad, 1.45F), alpha * 0.45F, 1.0F, true);
      Vec3 side = fwd.cross(hup).normalize();
      Vec3 eyeBase = spine[0].add(fwd.scale(0.42 * hs)).add(hup.scale(0.2 * hs));
      VertexConsumer ev = Fx.buf(buf, Fx.FLARE);
      float blink = 0.85F + 0.15F * Mth.sin(t * 0.7F);
      for (int sd = -1; sd <= 1; sd += 2) {
         Vec3 e = eyeBase.add(side.scale(0.2 * size * sd));
         Fx.billboard(ev, pose, cam, e.x, e.y, e.z, 0.3F * hs, t * 3.0F, Fx.c(1.0F, 0.85F, 0.2F, 1.6F * alpha * blink));
      }
      whiskers(buf, m, spine[0], fwd, hup, side, hs, alpha, t);
      if (fire > 0.01F) {
         breath(buf, pose, cam, spine[0].add(fwd.scale(1.1 * hs)), fwd, hs, fire * alpha, t);
      }
   }

   private static float[] scaled(float[] r, float k) {
      float[] o = new float[r.length];
      for (int i = 0; i < r.length; i++) {
         o[i] = r[i] * k;
      }
      return o;
   }

   private static void vtx(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vec3 p, float[] c, float a, float u, float v, Vec3 n) {
      VClient.vertex(vc, m, nm, (float)p.x, (float)p.y, (float)p.z, c[0], c[1], c[2], a, u, v, OverlayTexture.NO_OVERLAY, FULL, (float)n.x, (float)n.y, (float)n.z);
   }

   private static float shade(Vec3 n) {
      return 0.5F + 0.5F * (float)Math.max(0.0, n.dot(LIGHT));
   }

   private static float[] skin(double cosTop, float shade, float k) {
      // cosTop: 1 = back, -1 = belly
      float[] c;
      if (cosTop > 0.2) {
         float f = (float)Math.min(1.0, (cosTop - 0.2) / 0.8);
         c = lerp(FLANK, BACK, f);
      } else if (cosTop > -0.45) {
         c = FLANK;
      } else {
         float f = (float)Math.min(1.0, (-cosTop - 0.45) / 0.4);
         c = lerp(FLANK, BELLY, f);
      }
      return new float[]{c[0] * shade * k, c[1] * shade * k, c[2] * shade * k};
   }

   private static float[] lerp(float[] a, float[] b, float f) {
      return new float[]{a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
   }

   private static void body(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vec3[] sp, Vec3[] nor, Vec3[] bin, float[] rad, float alpha, float k, boolean glow) {
      int n = sp.length;
      for (int i = 0; i < n - 1; i++) {
         for (int j = 0; j < SIDES; j++) {
            Vec3[] q = new Vec3[4];
            Vec3[] qn = new Vec3[4];
            double[] ct = new double[4];
            int[][] idx = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
            for (int c = 0; c < 4; c++) {
               int ii = idx[c][0];
               double a = (double)idx[c][1] / SIDES * Math.PI * 2.0;
               Vec3 dir = nor[ii].scale(Math.cos(a)).add(bin[ii].scale(Math.sin(a)));
               // belly is a little flatter than the back
               double r = rad[ii] * (Math.cos(a) < 0.0 ? 0.88 : 1.0);
               q[c] = sp[ii].add(dir.scale(r));
               qn[c] = dir;
               ct[c] = Math.cos(a);
            }
            float s0 = (float)i / (n - 1);
            float fadeTail = s0 > 0.9F ? (1.0F - s0) / 0.1F : 1.0F;
            for (int c = 0; c < 4; c++) {
               float a = alpha * (glow ? (0.5F + 0.5F * (float)Math.max(0.0, -ct[c])) * fadeTail : Math.min(1.0F, 0.35F + fadeTail));
               float[] col = glow ? Fx.c(GLOW[0], GLOW[1], GLOW[2], a) : skin(ct[c], shade(qn[c]), k);
               float u = (float)idx[c][1] / SIDES * 3.0F;
               float v = (float)idx[c][0] * 0.45F;
               vtx(vc, m, nm, q[c], col, glow ? 1.0F : a, u, v, qn[c]);
            }
         }
      }
   }

   /** Triangular dorsal fins along the back, two sided. */
   private static void fins(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vec3[] sp, Vec3[] tan, Vec3[] nor, float[] rad, float alpha, float t) {
      int n = sp.length;
      for (int i = 4; i < n - 4; i += 3) {
         float s = (float)i / (n - 1);
         Vec3 base = sp[i].add(nor[i].scale(rad[i] * 0.92));
         Vec3 back = sp[i + 2].add(nor[i + 2].scale(rad[i + 2] * 0.92));
         float h = rad[i] * (0.75F + 0.15F * Mth.sin(t * 0.3F + i));
         Vec3 tip = base.add(nor[i].scale(h)).add(tan[i].scale(rad[i] * 1.2));
         float[] cb = Fx.c(BACK[0], BACK[1], BACK[2], 1.4F);
         float[] ct = Fx.c(1.0F, 0.3F, 0.5F, 1.0F);
         Vec3 nside = tan[i].cross(nor[i]).normalize();
         for (int side = 0; side < 2; side++) {
            Vec3 nn = side == 0 ? nside : nside.scale(-1.0);
            vtx(vc, m, nm, base, cb, alpha, 0.0F, 0.0F, nn);
            vtx(vc, m, nm, side == 0 ? tip : back, side == 0 ? ct : cb, alpha, 0.0F, 1.0F, nn);
            vtx(vc, m, nm, side == 0 ? back : tip, side == 0 ? cb : ct, alpha, 1.0F, 1.0F, nn);
            vtx(vc, m, nm, base, cb, alpha, 1.0F, 0.0F, nn);
         }
      }
   }

   /** A tapered tube along a polyline (limbs, horns, claws). */
   private static void tube(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vec3[] pts, float r0, float r1, float[] c0, float[] c1, float alpha) {
      int n = pts.length;
      int sides = 8;
      Vec3 up = new Vec3(0.0, 1.0, 0.0);
      Vec3[] nor = new Vec3[n];
      Vec3[] bin = new Vec3[n];
      for (int i = 0; i < n; i++) {
         Vec3 tg = pts[Math.min(n - 1, i + 1)].subtract(pts[Math.max(0, i - 1)]).normalize();
         Vec3 nn = up.subtract(tg.scale(up.dot(tg)));
         if (nn.lengthSqr() < 1.0E-6) {
            nn = new Vec3(1.0, 0.0, 0.0);
         }
         nor[i] = nn.normalize();
         bin[i] = tg.cross(nor[i]).normalize();
         up = nor[i];
      }
      for (int i = 0; i < n - 1; i++) {
         float f0 = (float)i / (n - 1);
         float f1 = (float)(i + 1) / (n - 1);
         for (int j = 0; j < sides; j++) {
            int[][] idx = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
            for (int[] id : idx) {
               double a = (double)id[1] / sides * Math.PI * 2.0;
               Vec3 dir = nor[id[0]].scale(Math.cos(a)).add(bin[id[0]].scale(Math.sin(a)));
               float f = id[0] == i ? f0 : f1;
               float r = r0 + (r1 - r0) * f;
               float sh = shade(dir);
               float[] c = lerp(c0, c1, f);
               vtx(vc, m, nm, pts[id[0]].add(dir.scale(r)), new float[]{c[0] * sh, c[1] * sh, c[2] * sh}, alpha, 0.0F, 0.0F, dir);
            }
         }
      }
   }

   private static void legs(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vec3[] sp, Vec3[] tan, Vec3[] nor, Vec3[] bin, float[] rad, float alpha, float t, float size) {
      int n = sp.length;
      float[] at = {0.17F, 0.21F, 0.52F, 0.56F};
      for (int k = 0; k < 4; k++) {
         int i = Math.round(at[k] * (n - 1));
         double side = k % 2 == 0 ? 1.0 : -1.0;
         double swing = Math.sin(t * 0.25 + k * 1.7) * 0.35;
         Vec3 hip = sp[i].add(bin[i].scale(side * rad[i] * 0.8)).subtract(nor[i].scale(rad[i] * 0.3));
         Vec3 knee = hip.add(bin[i].scale(side * 0.45 * size)).subtract(nor[i].scale(0.25 * size)).add(tan[i].scale((0.15 + swing) * size));
         Vec3 foot = knee.subtract(nor[i].scale(0.4 * size)).add(tan[i].scale((-0.25 + swing) * size)).add(bin[i].scale(side * 0.1 * size));
         tube(vc, m, nm, new Vec3[]{hip, knee, foot}, 0.16F * size, 0.08F * size, FLANK, BACK, alpha);
         for (int c = -1; c <= 1; c++) {
            Vec3 dir = tan[i].scale(-0.6).add(bin[i].scale(side * 0.4 + c * 0.35)).subtract(nor[i].scale(0.5)).normalize();
            Vec3 mid = foot.add(dir.scale(0.12 * size));
            Vec3 tip = mid.add(dir.add(nor[i].scale(-0.6)).normalize().scale(0.14 * size));
            tube(vc, m, nm, new Vec3[]{foot, mid, tip}, 0.045F * size, 0.0F, HORN, HORN, alpha);
         }
      }
   }

   private static void horns(VertexConsumer vc, Matrix4f m, Matrix3f nm, Vec3 neck, Vec3 fwd, Vec3 up, float size, float alpha) {
      Vec3 side = fwd.cross(up).normalize();
      for (int sd = -1; sd <= 1; sd += 2) {
         Vec3[] pts = new Vec3[6];
         Vec3 base = neck.add(fwd.scale(0.12 * size)).add(up.scale(0.28 * size)).add(side.scale(0.17 * size * sd));
         for (int i = 0; i < 6; i++) {
            double f = i / 5.0;
            pts[i] = base.add(fwd.scale(-0.75 * f * size)).add(up.scale((0.45 * f - 0.25 * f * f) * size)).add(side.scale(0.18 * f * size * sd));
         }
         tube(vc, m, nm, pts, 0.09F * size, 0.005F, HORN, new float[]{0.7F, 0.5F, 0.3F}, alpha);
      }
   }

   /** Lofted head: skull + snout (upper half) and a jaw that swings open, with teeth. */
   private static void head(VertexConsumer vc, VertexConsumer wv, Matrix4f m, Matrix3f nm, Vec3 neck, Vec3 fwd, Vec3 up, float size, float alpha, float jaw, float t) {
      Vec3 side = fwd.cross(up).normalize();
      int K = 10;
      int P = 9;
      float L = 1.15F * size;
      // width / height of the head along its length
      float[] w = new float[K + 1];
      float[] h = new float[K + 1];
      for (int k = 0; k <= K; k++) {
         float x = (float)k / K;
         w[k] = size * (x < 0.35F ? 0.36F + 0.06F * Mth.sin(x / 0.35F * (float)Math.PI) : 0.36F - (x - 0.35F) / 0.65F * 0.2F);
         h[k] = size * (x < 0.3F ? 0.34F : 0.34F - (x - 0.3F) / 0.7F * 0.22F) + (x > 0.85F ? size * 0.03F : 0.0F);
      }
      double open = Math.toRadians(8.0 + 42.0 * jaw);
      for (int half = 0; half < 2; half++) {
         boolean upper = half == 0;
         for (int k = 0; k < (vc == null ? 0 : K); k++) {
            for (int j = 0; j < P; j++) {
               int[][] idx = {{k, j}, {k + 1, j}, {k + 1, j + 1}, {k, j + 1}};
               if (!upper) {
                  idx = new int[][]{{k, j}, {k, j + 1}, {k + 1, j + 1}, {k + 1, j}};
               }
               for (int[] id : idx) {
                  double x = (double)id[0] / K;
                  double a = (double)id[1] / P * Math.PI;
                  double cy = Math.sin(a) * h[id[0]] * (upper ? 1.0 : -0.55);
                  double cs = Math.cos(a) * w[id[0]] * (upper ? 1.0 : 0.85);
                  Vec3 local = new Vec3(x * L, cy, cs);
                  if (!upper) {
                     // jaw hinge near the back of the skull
                     double hx = 0.22 * L;
                     double dx = local.x - hx;
                     double dy = local.y;
                     double c = Math.cos(open);
                     double s = Math.sin(open);
                     local = new Vec3(hx + dx * c + dy * s, -dx * s + dy * c, local.z);
                  }
                  Vec3 p = neck.add(fwd.scale(local.x)).add(up.scale(local.y)).add(side.scale(local.z));
                  Vec3 nrm = up.scale(Math.sin(a) * (upper ? 1.0 : -1.0)).add(side.scale(Math.cos(a))).normalize();
                  float[] col = skin(upper ? Math.sin(a) : -Math.sin(a), shade(nrm), 1.0F);
                  vtx(vc, m, nm, p, col, alpha, (float)(id[1] / (double)P), (float)x * 2.0F, nrm);
               }
            }
         }
         // teeth along the rim
         for (int k = 2; k < (wv == null ? 0 : K); k++) {
            for (int sd = -1; sd <= 1; sd += 2) {
               double x = (double)k / K;
               double hx = 0.22 * L;
               Vec3 base;
               Vec3 dir;
               if (upper) {
                  base = new Vec3(x * L, 0.0, w[k] * 0.92 * sd);
                  dir = new Vec3(0.02, -1.0, 0.0);
               } else {
                  double dx = x * L - hx;
                  base = new Vec3(hx + dx * Math.cos(open), -dx * Math.sin(open), w[k] * 0.8 * sd);
                  dir = new Vec3(Math.sin(open), Math.cos(open), 0.0);
               }
               Vec3 b = neck.add(fwd.scale(base.x)).add(up.scale(base.y)).add(side.scale(base.z));
               Vec3 d = fwd.scale(dir.x).add(up.scale(dir.y)).normalize();
               Vec3 tip = b.add(d.scale(0.11 * size));
               Vec3 b2 = b.add(fwd.scale(0.06 * size));
               float[] white = {1.0F, 0.97F, 0.9F};
               vtx(wv, m, nm, b, white, alpha, 0, 0, side);
               vtx(wv, m, nm, tip, white, alpha, 0, 1, side);
               vtx(wv, m, nm, b2, white, alpha, 1, 1, side);
               vtx(wv, m, nm, b, white, alpha, 1, 0, side);
               vtx(wv, m, nm, b2, white, alpha, 1, 0, side);
               vtx(wv, m, nm, tip, white, alpha, 1, 1, side);
               vtx(wv, m, nm, b, white, alpha, 0, 1, side);
               vtx(wv, m, nm, b, white, alpha, 0, 0, side);
            }
         }
      }
      // inside of the mouth: a glowing slab between the jaws
      if (jaw > 0.05F && wv != null) {
         float[] in = {1.0F, 0.35F, 0.1F};
         Vec3 a0 = neck.add(fwd.scale(0.25 * L)).add(side.scale(0.25 * size));
         Vec3 a1 = neck.add(fwd.scale(0.25 * L)).add(side.scale(-0.25 * size));
         Vec3 b0 = neck.add(fwd.scale(0.95 * L)).add(side.scale(0.12 * size)).add(up.scale(-0.2 * jaw * size));
         Vec3 b1 = neck.add(fwd.scale(0.95 * L)).add(side.scale(-0.12 * size)).add(up.scale(-0.2 * jaw * size));
         vtx(wv, m, nm, a0, in, alpha, 0, 0, up);
         vtx(wv, m, nm, b0, in, alpha, 0, 1, up);
         vtx(wv, m, nm, b1, in, alpha, 1, 1, up);
         vtx(wv, m, nm, a1, in, alpha, 1, 0, up);
      }
   }

   private static void whiskers(MultiBufferSource buf, Matrix4f m, Vec3 neck, Vec3 fwd, Vec3 up, Vec3 side, float size, float alpha, float t) {
      VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
      for (int sd = -1; sd <= 1; sd += 2) {
         Vec3 prev = neck.add(fwd.scale(1.0 * size)).add(side.scale(0.16 * size * sd));
         for (int i = 1; i <= 8; i++) {
            double f = i / 8.0;
            Vec3 next = neck.add(fwd.scale((1.0 - 1.6 * f) * size)).add(side.scale((0.16 + 0.9 * f) * size * sd))
               .add(up.scale(Math.sin(t * 0.25 + f * 5.0 + sd) * 0.35 * f * size - 0.2 * f * size));
            Fx.beam(vc, m, prev, next, 0.05F * size, Fx.c(1.0F, 0.75F, 0.4F, alpha * (float)(1.0 - f * 0.7)), Fx.c(1.0F, 0.4F, 0.3F, alpha * (float)(1.0 - f)), 1.0F, 0.0F);
            prev = next;
         }
      }
   }

   private static void breath(MultiBufferSource buf, PoseStack pose, Quaternionf cam, Vec3 mouth, Vec3 fwd, float size, float a, float t) {
      VertexConsumer vc = Fx.buf(buf, Fx.FLARE);
      for (int i = 0; i < 14; i++) {
         float f = (i + (t * 0.6F) % 1.0F) / 14.0F;
         Vec3 p = mouth.add(fwd.scale(f * 6.0 * size)).add(0.0, Math.sin(t * 0.5 + i) * 0.15 * f, 0.0);
         float s = (0.4F + f * 2.2F) * size;
         float[] c = f < 0.3F ? Fx.c(1.0F, 0.95F, 0.6F, a * 1.4F) : Fx.c(1.0F, 0.35F + 0.3F * (1.0F - f), 0.08F, a * (1.2F - f));
         Fx.billboard(vc, pose, cam, p.x, p.y, p.z, s, t * 7.0F + i * 40.0F, c);
      }
   }
}
