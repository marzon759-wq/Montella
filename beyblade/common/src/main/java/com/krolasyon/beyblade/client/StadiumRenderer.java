package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.compat.VClient;
import com.krolasyon.beyblade.entity.StadiumEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class StadiumRenderer extends EntityRenderer<StadiumEntity> {
   private static final ResourceLocation TEX = com.krolasyon.beyblade.compat.V.rl("librabeyblade", "textures/entity/stadium.png");
   private static final int SEGS = 128;
   private static float[] mesh;
   private static final ResourceLocation CRACKS = com.krolasyon.beyblade.compat.V.rl("librabeyblade", "textures/fx/cracks.png");
   private static final ResourceLocation XLINE = com.krolasyon.beyblade.compat.V.rl("librabeyblade", "textures/entity/xline.png");
   private static final List<float[]> V = new ArrayList<>();

   public StadiumRenderer(Context ctx) {
      super(ctx);
      this.shadowRadius = 0.0F;
   }

   public void render(StadiumEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
      if (mesh == null) {
         mesh = build();
      }

      VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(TEX));
      Matrix4f m = pose.last().pose();
      Matrix3f n = pose.last().normal();
      float[] d = mesh;

      for (int i = 0; i < d.length; i += 8) {
         VClient.vertex(
            vc, m, n, d[i], d[i + 1], d[i + 2], 1.0F, 1.0F, 1.0F, 1.0F, d[i + 3], d[i + 4], OverlayTexture.NO_OVERLAY, light, d[i + 5], d[i + 6], d[i + 7]
         );
      }

      this.renderPads(e, partial, pose, buf);
      this.renderXtreme(e, partial, pose, buf, light);
      this.renderScars(e, partial, pose, buf);
      super.render(e, yaw, partial, pose, buf, light);
   }

   private void renderScars(StadiumEntity e, float partial, PoseStack pose, MultiBufferSource buf) {
      if (!e.scars.isEmpty()) {
         Matrix4f m = pose.last().pose();
         float now = (float)e.tickCount + partial;
         int G = 6;

         for (float[] sc : e.scars) {
            float age = now - sc[3];
            if (!(age < 0.0F) && !(age > 1800.0F)) {
               float a = Math.min(1.0F, (1800.0F - age) / 300.0F) * 0.9F;
               VertexConsumer vc = Fx.bufT(buf, CRACKS, a);
               float r = sc[2];
               float cr = Mth.cos(sc[4]);
               float sr = Mth.sin(sc[4]);

               for (int i = 0; i < G; i++) {
                  for (int j = 0; j < G; j++) {
                     float[][] q = new float[4][];
                     float[][] uv = new float[4][];
                     int[][] c = new int[][]{{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}};

                     for (int k = 0; k < 4; k++) {
                        float u = (float)c[k][0] / (float)G * 2.0F - 1.0F;
                        float v = (float)c[k][1] / (float)G * 2.0F - 1.0F;
                        float x = sc[0] + (u * cr - v * sr) * r;
                        float z = sc[1] + (u * sr + v * cr) * r;
                        float y = (float)StadiumEntity.surface(Math.sqrt((double)(x * x + z * z))) + 0.02F;
                        q[k] = new float[]{x, y, z};
                        uv[k] = new float[]{(u + 1.0F) / 2.0F, (v + 1.0F) / 2.0F};
                     }

                     float[] w = new float[]{1.0F, 1.0F, 1.0F};
                     Fx.quad(vc, m, q[0], q[1], q[2], q[3], uv[0][0], uv[0][1], uv[2][0], uv[2][1], w, w);
                  }
               }

               Fx.alpha = 1.0F;
            }
         }
      }
   }

   private void renderPads(StadiumEntity e, float partial, PoseStack pose, MultiBufferSource buf) {
      float t = (float)e.tickCount + partial;
      boolean live = e.matchState() != 0;

      for (int side = 0; side < 2; side++) {
         float[] c = side == 0 ? new float[]{0.25F, 0.55F, 1.0F} : new float[]{1.0F, 0.25F, 0.2F};
         float pulse = 0.65F + 0.35F * Mth.sin(t * (live ? 0.3F : 0.1F) + (float)side * 3.0F);
         pose.pushPose();
         pose.translate(0.0, 0.03, side == 0 ? 23.5 : -23.5);
         Matrix4f m = pose.last().pose();
         VertexConsumer vc = Fx.buf(buf, Fx.SIGIL);
         Fx.groundQuad(vc, m, 0.02F, 1.6F, Fx.c(c[0], c[1], c[2], 0.9F * pulse));
         vc = Fx.buf(buf, Fx.SOFT);
         Fx.ring(vc, m, 0.03F, 1.52F, 1.84F, 32, Fx.c(c[0], c[1], c[2], pulse), 1.0F, 0.0F);
         Fx.band(vc, m, 1.68F, 0.0F, 1.68F, 1.4F * pulse, 32, Fx.c(c[0], c[1], c[2], 0.5F * pulse), new float[]{0.0F, 0.0F, 0.0F}, 1.0F, 0.0F, 1.0F, 0.0F);
         pose.popPose();
      }
   }

   public ResourceLocation getTextureLocation(StadiumEntity e) {
      return TEX;
   }

   static double[][] wallArcs() {
      double x0 = 10.0;
      double x1 = 350.0;
      double o0 = (double)StadiumEntity.OVER_DEG[0];
      double o1 = (double)StadiumEntity.OVER_DEG[1];
      return new double[][]{{x0, o0 - 8.0}, {o0 + 8.0, o1 - 8.0}, {o1 + 8.0, x1}};
   }

   private void renderXtreme(StadiumEntity e, float partial, PoseStack pose, MultiBufferSource buf, int light) {
      Matrix4f m = pose.last().pose();
      Matrix3f n = pose.last().normal();
      VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(XLINE));
      int RS = 110;
      double a0d = 20.0;
      double span = 320.0;

      for (int i = 0; i < RS; i++) {
         double a0 = Math.toRadians(a0d + span * (double)i / (double)RS);
         double a1 = Math.toRadians(a0d + span * (double)(i + 1) / (double)RS);
         double[][] q = new double[][]{{9.45F, a0, 0.0, 0.0}, {10.35F, a0, 0.0, 1.0}, {10.35F, a1, 1.0, 1.0}, {9.45F, a1, 1.0, 0.0}};

         for (double[] p : q) {
            double y = StadiumEntity.surface(p[0]) + 0.03;
            VClient.vertex(
               vc,
               m,
               n,
               (float)(Math.cos(p[1]) * p[0]),
               (float)y,
               (float)(Math.sin(p[1]) * p[0]),
               1.0F,
               1.0F,
               1.0F,
               1.0F,
               (float)p[2],
               (float)p[3],
               OverlayTexture.NO_OVERLAY,
               light,
               0.0F,
               1.0F,
               0.0F
            );
         }
      }

      float t = (float)e.tickCount + partial;
      vc = buf.getBuffer(RenderType.entityCutoutNoCull(BeyMesh.PLAIN));
      double back = 13.0;
      double fy = 1.7100000476837158;
      double[][] pockets = new double[][]{{0.0, 10.0, 1.0}, {(double)StadiumEntity.OVER_DEG[0], 8.0, 0.0}, {(double)StadiumEntity.OVER_DEG[1], 8.0, 0.0}};

      for (double[] pk : pockets) {
         boolean x = pk[2] > 0.0;
         float[] col = x ? new float[]{1.0F, 0.45F, 0.1F} : new float[]{0.2F, 0.5F, 1.0F};
         double c = Math.toRadians(pk[0]);
         double h = Math.toRadians(pk[1]);
         double a0 = c - h;
         double a1 = c + h;
         quad(vc, m, n, light, pt(11.1F, a0, fy), pt(back, a0, fy), pt(back, a1, fy), pt(11.1F, a1, fy), col, 0.75F, 0.0, 1.0, 0.0);
         float[] wc = new float[]{0.55F, 0.57F, 0.62F};
         quad(vc, m, n, light, pt(back, a0, fy), pt(back, a0, fy + 1.1), pt(back, a1, fy + 1.1), pt(back, a1, fy), wc, 1.0F, -Math.cos(c), 0.0, -Math.sin(c));

         for (double a : new double[]{a0, a1}) {
            quad(vc, m, n, light, pt(11.5, a, fy), pt(11.5, a, fy + 1.1), pt(back, a, fy + 1.1), pt(back, a, fy), wc, 0.9F, -Math.sin(a), 0.0, Math.cos(a));
         }
      }

      for (double[] pk : pockets) {
         boolean x = pk[2] > 0.0;
         float[] col = x ? new float[]{1.0F, 0.45F, 0.1F} : new float[]{0.2F, 0.5F, 1.0F};
         double c = Math.toRadians(pk[0]);
         pose.pushPose();
         double mr = (11.1F + back) / 2.0;
         pose.translate(Math.cos(c) * mr, fy + 0.02, Math.sin(c) * mr);
         float pulse = 0.6F + 0.4F * Mth.sin(t * 0.15F + (float)pk[0]);
         VertexConsumer g = Fx.buf(buf, Fx.SIGIL);
         Fx.groundQuad(g, pose.last().pose(), 0.0F, x ? 1.3F : 1.0F, Fx.c(col[0], col[1], col[2], 0.9F * pulse));
         pose.popPose();
      }
   }

   private static float[] pt(double r, double a, double y) {
      return new float[]{(float)(Math.cos(a) * r), (float)y, (float)(Math.sin(a) * r)};
   }

   private static void quad(
      VertexConsumer vc, Matrix4f m, Matrix3f n, int light, float[] a, float[] b, float[] c, float[] d, float[] col, float k, double nx, double ny, double nz
   ) {
      for (float[] p : new float[][]{a, b, c, d}) {
         VClient.vertex(
            vc, m, n, p[0], p[1], p[2], col[0] * k, col[1] * k, col[2] * k, 1.0F, 0.5F, 0.5F, OverlayTexture.NO_OVERLAY, light, (float)nx, (float)ny, (float)nz
         );
      }
   }

   private static void vert(double x, double y, double z, double u, double v, double nx, double ny, double nz) {
      double l = Math.sqrt(nx * nx + ny * ny + nz * nz);
      if (l < 1.0E-6) {
         l = 1.0;
      }

      V.add(new float[]{(float)x, (float)y, (float)z, (float)(u / 512.0), (float)(v / 512.0), (float)(nx / l), (float)(ny / l), (float)(nz / l)});
   }

   private static double ang(int i) {
      return (double)i * Math.PI * 2.0 / 128.0;
   }

   private static double octR(double apothem, double a) {
      double deg = Math.toDegrees(a);
      double phi = (deg % 45.0 + 45.0) % 45.0 - 22.5;
      return apothem / Math.cos(Math.toRadians(phi));
   }

   private static void band(StadiumRenderer.RF r0, double y0, StadiumRenderer.RF r1, double y1, double u0, double v0, double u1, double v1, boolean flatTop) {
      for (int i = 0; i < 128; i++) {
         double a0 = ang(i);
         double a1 = ang(i + 1);
         double ra0 = r0.r(a0);
         double ra1 = r0.r(a1);
         double rb0 = r1.r(a0);
         double rb1 = r1.r(a1);
         double uu0 = u0 + (u1 - u0) * (double)i / 128.0;
         double uu1 = u0 + (u1 - u0) * (double)(i + 1) / 128.0;
         double am = (a0 + a1) / 2.0;
         double nx;
         double ny;
         double nz;
         if (flatTop) {
            nx = 0.0;
            ny = 1.0;
            nz = 0.0;
         } else {
            double dr = r1.r(am) - r0.r(am);
            double dy = y1 - y0;
            nx = -dy * Math.cos(am);
            ny = dr;
            nz = -dy * Math.sin(am);
         }

         vert(Math.cos(a0) * ra0, y0, Math.sin(a0) * ra0, uu0, v0, nx, ny, nz);
         vert(Math.cos(a0) * rb0, y1, Math.sin(a0) * rb0, uu0, v1, nx, ny, nz);
         vert(Math.cos(a1) * rb1, y1, Math.sin(a1) * rb1, uu1, v1, nx, ny, nz);
         vert(Math.cos(a1) * ra1, y0, Math.sin(a1) * ra1, uu1, v0, nx, ny, nz);
      }
   }

   private static float[] build() {
      V.clear();
      int RINGS = 32;

      for (int j = 0; j < RINGS; j++) {
         double r0 = (double)(10.5F * (float)j / (float)RINGS);
         double r1 = (double)(10.5F * (float)(j + 1) / (float)RINGS);

         for (int i = 0; i < 128; i++) {
            double a0 = ang(i);
            double a1 = ang(i + 1);
            double[][] p = new double[][]{{r0, a0}, {r1, a0}, {r1, a1}, {r0, a1}};

            for (double[] q : p) {
               double x = Math.cos(q[1]) * q[0];
               double z = Math.sin(q[1]) * q[0];
               double s = StadiumEntity.slope(q[0]);
               vert(x, StadiumEntity.surface(q[0]), z, 128.0 + x / 10.5 * 124.0, 128.0 + z / 10.5 * 124.0, -s * Math.cos(q[1]), 1.0, -s * Math.sin(q[1]));
            }
         }
      }

      band(ax -> 10.5, 1.5, ax -> 11.1F, 1.7F, 256.0, 160.0, 512.0, 200.0, false);
      band(ax -> 11.1F, 1.7F, ax -> 12.2F, 1.7F, 256.0, 200.0, 512.0, 250.0, true);
      band(ax -> 12.2F, 1.7F, ax -> 12.8F, 1.1F, 256.0, 130.0, 512.0, 160.0, false);
      band(ax -> 12.8F, 1.1F, ax -> octR(13.4F, ax), 1.1F, 256.0, 140.0, 512.0, 250.0, true);
      band(ax -> octR(13.4F, ax), 1.1F, ax -> octR(13.4F, ax), 0.55F, 256.0, 10.0, 512.0, 60.0, false);
      band(ax -> octR(13.4F, ax), 0.55F, ax -> octR(14.4F, ax), 0.55F, 256.0, 140.0, 512.0, 250.0, true);

      for (int i = 0; i < 128; i++) {
         double a0 = ang(i);
         double a1 = ang(i + 1);
         double am = (a0 + a1) / 2.0;
         double r0 = octR(14.4F, a0);
         double r1 = octR(14.4F, a1);
         double deg = Math.toDegrees(am);
         boolean front = deg > 67.5 && deg < 112.5;
         double u1;
         double v0;
         double v1;
         double u0;
         if (front) {
            double f0 = (Math.toDegrees(a0) - 67.5) / 45.0;
            double f1 = (Math.toDegrees(a1) - 67.5) / 45.0;
            u0 = 512.0 * (1.0 - f0);
            u1 = 512.0 * (1.0 - f1);
            v0 = 448.0;
            v1 = 384.0;
         } else {
            u0 = 256.0 + 256.0 * (double)i / 128.0;
            u1 = 256.0 + 256.0 * (double)(i + 1) / 128.0;
            v0 = 120.0;
            v1 = 70.0;
         }

         double nx = Math.cos(Math.toRadians((double)Math.round(Math.toDegrees(am) / 45.0) * 45.0));
         double nz = Math.sin(Math.toRadians((double)Math.round(Math.toDegrees(am) / 45.0) * 45.0));
         vert(Math.cos(a0) * r0, 0.0, Math.sin(a0) * r0, u0, v0, nx, 0.0, nz);
         vert(Math.cos(a0) * r0, 0.55F, Math.sin(a0) * r0, u0, v1, nx, 0.0, nz);
         vert(Math.cos(a1) * r1, 0.55F, Math.sin(a1) * r1, u1, v1, nx, 0.0, nz);
         vert(Math.cos(a1) * r1, 0.0, Math.sin(a1) * r1, u1, v0, nx, 0.0, nz);
      }

      double[][] arcs = wallArcs();

      for (double[] arc : arcs) {
         double a0d = arc[0];
         double a1d = arc[1];
         int WS = Math.max(4, (int)Math.round((a1d - a0d) / 90.0 * 40.0));
         double c = Math.toRadians((a0d + a1d) / 2.0);
         double h = Math.toRadians((a1d - a0d) / 2.0);

         for (int i = 0; i < WS; i++) {
            double a0 = c - h + 2.0 * h * (double)i / (double)WS;
            double a1 = c - h + 2.0 * h * (double)(i + 1) / (double)WS;
            double cu0 = 512.0 * (double)i / (double)WS;
            double cu1 = 512.0 * (double)(i + 1) / (double)WS;
            double c0 = Math.cos(a0);
            double s0 = Math.sin(a0);
            double c1 = Math.cos(a1);
            double s1 = Math.sin(a1);
            double am = (a0 + a1) / 2.0;
            vert(c0 * 11.1F, 1.7F, s0 * 11.1F, cu0, 318.0, -Math.cos(am), 0.0, -Math.sin(am));
            vert(c0 * 11.1F, 2.75, s0 * 11.1F, cu0, 258.0, -Math.cos(am), 0.0, -Math.sin(am));
            vert(c1 * 11.1F, 2.75, s1 * 11.1F, cu1, 258.0, -Math.cos(am), 0.0, -Math.sin(am));
            vert(c1 * 11.1F, 1.7F, s1 * 11.1F, cu1, 318.0, -Math.cos(am), 0.0, -Math.sin(am));
            double tu0 = 512.0 * (double)i / (double)WS;
            double tu1 = 512.0 * (double)(i + 1) / (double)WS;
            vert(c0 * 11.1F, 2.75, s0 * 11.1F, tu0 * 0.5 + 256.0, 150.0, 0.0, 1.0, 0.0);
            vert(c0 * 11.5, 2.75, s0 * 11.5, tu0 * 0.5 + 256.0, 160.0, 0.0, 1.0, 0.0);
            vert(c1 * 11.5, 2.75, s1 * 11.5, tu1 * 0.5 + 256.0, 160.0, 0.0, 1.0, 0.0);
            vert(c1 * 11.1F, 2.75, s1 * 11.1F, tu1 * 0.5 + 256.0, 150.0, 0.0, 1.0, 0.0);
            vert(c0 * 11.5, 1.7F, s0 * 11.5, tu0 * 0.5 + 256.0, 240.0, Math.cos(am), 0.0, Math.sin(am));
            vert(c0 * 11.5, 2.75, s0 * 11.5, tu0 * 0.5 + 256.0, 200.0, Math.cos(am), 0.0, Math.sin(am));
            vert(c1 * 11.5, 2.75, s1 * 11.5, tu1 * 0.5 + 256.0, 200.0, Math.cos(am), 0.0, Math.sin(am));
            vert(c1 * 11.5, 1.7F, s1 * 11.5, tu1 * 0.5 + 256.0, 240.0, Math.cos(am), 0.0, Math.sin(am));
         }

         for (double a : new double[]{c - h, c + h}) {
            double ca = Math.cos(a);
            double sa = Math.sin(a);
            double tx = -sa * (double)(a < c ? -1 : 1);
            double tz = ca * (double)(a < c ? -1 : 1);
            vert(ca * 11.1F, 1.7F, sa * 11.1F, 300.0, 240.0, tx, 0.0, tz);
            vert(ca * 11.1F, 2.75, sa * 11.1F, 300.0, 200.0, tx, 0.0, tz);
            vert(ca * 11.5, 2.75, sa * 11.5, 330.0, 200.0, tx, 0.0, tz);
            vert(ca * 11.5, 1.7F, sa * 11.5, 330.0, 240.0, tx, 0.0, tz);
         }
      }

      float[] out = new float[V.size() * 8];

      for (int i = 0; i < V.size(); i++) {
         System.arraycopy(V.get(i), 0, out, i * 8, 8);
      }

      V.clear();
      return out;
   }

   private interface RF {
      double r(double var1);
   }
}
