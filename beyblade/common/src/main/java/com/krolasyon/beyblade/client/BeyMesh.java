package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.compat.VClient;
import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.CommonBeybladeEntity;
import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.Loadout;
import com.krolasyon.beyblade.parts.PartType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class BeyMesh {
   public static final BeyMesh LIBRA = new BeyMesh("libra", 1.3F, 1.46F, 1.0222223F);
   public static final BeyMesh LDRAGO = new BeyMesh("ldrago", 0.98F, 1.18F, 1.1072464F);
   public static final BeyMesh LEONE = new BeyMesh("leone", 1.27F, 1.48F, 1.0552148F);
   public static final BeyMesh EAGLE = new BeyMesh("eagle", 1.27F, 1.48F, 1.0434783F);
   public static final BeyMesh PEGASUS = new BeyMesh("pegasus", 1.03F, 1.24F, 1.0484581F);
   private static final BeyMesh[] COMMON = new BeyMesh[10];
   public static final BeyMesh TEMPO = new BeyMesh("tempo", 1.27F, 1.48F, 1.0404859F);
   public static final BeyMesh VARIARES = new BeyMesh("variares", 1.28F, 1.48F, 1.048F);
   public static final BeyMesh BEFFAL = new BeyMesh("beffal", 1.28F, 1.48F, 1.045977F);
   public static final BeyMesh BIGBANG = new BeyMesh("bigbang", 1.3F, 1.5F, 1.05F);
   public static final BeyMesh PHOENIX = new BeyMesh("phoenix", 1.3F, 1.5F, 1.05F);
   public static final BeyMesh INFERNO = new BeyMesh("inferno", 1.3F, 1.5F, 1.05F);
   public final ResourceLocation FACE;
   public final ResourceLocation GLOW;
   public final ResourceLocation PARTS;
   public final float TOP_Y;
   public final float HEIGHT;
   private final String name;
   private final String kind;
   private final float ghostHalf;
   private static final int TOP = 0;
   private static final int SIDE = 1;
   private static final int PART = 2;
   private float[][] groups;
   public static final ResourceLocation PLAIN = V.rl("librabeyblade", "textures/entity/part_plain.png");
   private float[] under;
   private float[] lathe;
   private float wheelBottom;

   public static BeyMesh common(int variant) {
      int v = Math.floorMod(variant, COMMON.length);
      if (COMMON[v] == null) {
         COMMON[v] = new BeyMesh("common_" + v, "common", 1.07F, 1.26F, 1.04F);
      }

      return COMMON[v];
   }

   public static BeyMesh of(BeybladeEntity e) {
      return e instanceof CommonBeybladeEntity c ? common(c.getVariant()) : of(e.kindId());
   }

   public static BeyMesh forLoadout(Loadout lo, BeyMesh fallback) {
      BeyPart w = lo == null ? null : lo.get(PartType.WHEEL);
      return w == null ? fallback : of(w.mesh());
   }

   public static BeyMesh of(String kind) {
      if (kind.startsWith("common_")) {
         return common(Integer.parseInt(kind.substring(7)));
      } else {
         return switch (kind) {
            case "bigbang" -> BIGBANG;
            case "phoenix" -> PHOENIX;
            case "inferno" -> INFERNO;
            case "pegasus" -> PEGASUS;
            case "tempo" -> TEMPO;
            case "variares" -> VARIARES;
            case "beffal" -> BEFFAL;
            case "eagle" -> EAGLE;
            case "ldrago" -> LDRAGO;
            case "leone" -> LEONE;
            default -> LIBRA;
         };
      }
   }

   private BeyMesh(String name, float topY, float height, float ghostHalf) {
      this(name, name, topY, height, ghostHalf);
   }

   private BeyMesh(String name, String meshName, float topY, float height, float ghostHalf) {
      this.name = meshName;
      this.kind = name;
      this.FACE = V.rl("librabeyblade", "textures/entity/" + name + "_face.png");
      this.GLOW = V.rl("librabeyblade", "textures/entity/" + name + "_glow.png");
      this.PARTS = V.rl("librabeyblade", "textures/entity/" + name + "_parts.png");
      this.TOP_Y = topY;
      this.HEIGHT = height;
      this.ghostHalf = ghostHalf;
   }

   private float[][] groups() {
      if (this.groups == null) {
         float[][] g = new float[3][];

         try (InputStream raw = BeyMesh.class.getResourceAsStream("/assets/librabeyblade/models/" + this.name + "_mesh.bin")) {
            if (raw == null) {
               throw new IllegalStateException(this.name + "_mesh.bin missing");
            }

            DataInputStream in = new DataInputStream(new BufferedInputStream(raw));
            in.readInt();
            int n = in.readInt();

            for (int i = 0; i < n; i++) {
               int id = in.readInt();
               int quads = in.readInt();
               float[] data = new float[quads * 4 * 8];

               for (int k = 0; k < data.length; k++) {
                  data[k] = in.readFloat();
               }

               if (id >= 0 && id < 3) {
                  g[id] = data;
               }
            }
         } catch (Exception var12) {
            throw new RuntimeException("Failed to load beyblade mesh " + this.name, var12);
         }

         for (int i = 0; i < 3; i++) {
            if (g[i] == null) {
               g[i] = new float[0];
            }
         }

         this.groups = g;
      }

      return this.groups;
   }

   public void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float r, float g, float b) {
      float[][] gr = this.groups();
      VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(this.FACE));
      emit(pose, vc, gr[0], light, overlay, r, g, b, 1.0F);
      emit(pose, vc, gr[1], light, overlay, r, g, b, 1.0F);
      vc = buffers.getBuffer(RenderType.entityCutoutNoCull(this.PARTS));
      emit(pose, vc, gr[2], light, overlay, r, g, b, 1.0F);
   }

   private void split() {
      if (this.under == null) {
         float[] p = this.groups()[2];
         float wb = -1.0F;

         for (int q = 0; q < p.length; q += 32) {
            boolean narrow = true;
            float maxY = -9.0F;

            for (int v = 0; v < 4; v++) {
               int o = q + v * 8;
               if (Math.sqrt((double)(p[o] * p[o] + p[o + 2] * p[o + 2])) > 0.305) {
                  narrow = false;
               }

               maxY = Math.max(maxY, p[o + 1]);
            }

            if (narrow) {
               wb = Math.max(wb, maxY);
            }
         }

         List<Float> u = new ArrayList<>();
         List<Float> l = new ArrayList<>();
         float minUnder = 9.0F;

         for (int q = 0; q < p.length; q += 32) {
            float minY = 9.0F;

            for (int v = 0; v < 4; v++) {
               minY = Math.min(minY, p[q + v * 8 + 1]);
            }

            List<Float> dst = wb > 0.0F && minY < wb - 1.0E-4F ? l : u;
            if (dst == u) {
               minUnder = Math.min(minUnder, minY);
            }

            for (int k = 0; k < 32; k++) {
               dst.add(p[q + k]);
            }
         }

         this.wheelBottom = wb > 0.0F ? wb : (minUnder < 9.0F ? minUnder : 0.86F);
         this.under = toArray(u);
         this.lathe = toArray(l);
      }
   }

   private static float[] toArray(List<Float> l) {
      float[] a = new float[l.size()];

      for (int i = 0; i < a.length; i++) {
         a[i] = l.get(i);
      }

      return a;
   }

   private boolean stockBottom(Loadout lo) {
      this.split();
      if (this.lathe.length == 0) {
         return false;
      } else {
         String[] st = BeyPart.stock(this.kind);
         BeyPart tr = lo.get(PartType.TRACK);
         BeyPart tp = lo.get(PartType.TIP);
         return tr != null && tp != null && tr.id().equals(st[3]) && tp.id().equals(st[4]);
      }
   }

   public float lift(Loadout lo) {
      return lo != null && !this.stockBottom(lo) ? PartLathe.wheelBottom(lo) - this.wheelBottom : 0.0F;
   }

   public void renderAssembled(PoseStack pose, MultiBufferSource buffers, int light, int overlay, Loadout lo) {
      if (lo != null && !this.stockBottom(lo)) {
         float dy = this.lift(lo);
         pose.pushPose();
         pose.translate(0.0F, dy, 0.0F);
         float[][] gr = this.groups();
         VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(this.FACE));
         emit(pose, vc, gr[0], light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
         emit(pose, vc, gr[1], light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
         vc = buffers.getBuffer(RenderType.entityCutoutNoCull(this.PARTS));
         emit(pose, vc, this.under, light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
         pose.popPose();
         PartLathe.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(PLAIN)), light, overlay, lo);
      } else {
         this.render(pose, buffers, light, overlay, 1.0F, 1.0F, 1.0F);
      }
   }

   public void renderGlowAssembled(PoseStack pose, MultiBufferSource buffers, float r, float g, float b, Loadout lo) {
      float dy = this.lift(lo);
      pose.pushPose();
      pose.translate(0.0F, dy, 0.0F);
      this.renderGlow(pose, buffers, r, g, b);
      pose.popPose();
   }

   public void renderGlow(PoseStack pose, MultiBufferSource buffers, float r, float g, float b) {
      if (!(r <= 0.003F) || !(g <= 0.003F) || !(b <= 0.003F)) {
         VertexConsumer vc = buffers.getBuffer(RenderType.eyes(this.GLOW));
         emit(pose, vc, this.groups()[0], 15728880, OverlayTexture.NO_OVERLAY, r, g, b, 1.0F);
      }
   }

   // ---------------------------------------------------------------- per part slices for the GeckoLib bones
   public static final int S_BOLT = 0;
   public static final int S_RING = 1;
   public static final int S_WHEEL = 2;
   public static final int S_TRACK = 3;
   public static final int S_TIP = 4;
   private float[][] faceSlices;
   private float[][] partSlices;
   private float[][] glowSlices;

   /** Radius with the fewest top-surface quads between lo and hi: the seam between two parts. */
   private static float seam(float[] top, float lo, float hi) {
      int bins = 24;
      int[] h = new int[bins];
      for (int q = 0; q < top.length; q += 32) {
         float r = 0.0F;
         for (int v = 0; v < 4; v++) {
            int o = q + v * 8;
            r += (float)Math.sqrt((double)(top[o] * top[o] + top[o + 2] * top[o + 2]));
         }
         r /= 4.0F;
         int b = (int)((r - lo) / (hi - lo) * (float)bins);
         if (b >= 0 && b < bins) {
            h[b]++;
         }
      }
      int best = bins / 2;
      for (int b = 1; b < bins - 1; b++) {
         if (h[b] < h[best]) {
            best = b;
         }
      }
      return lo + (hi - lo) * ((float)best + 0.5F) / (float)bins;
   }

   private void slice() {
      if (this.faceSlices == null) {
         this.split();
         float[][] gr = this.groups();
         float boltR = seam(gr[0], 0.2F, 0.42F);
         float ringR = seam(gr[0], 0.52F, 0.86F);
         List<List<Float>> face = new ArrayList<>();
         List<List<Float>> part = new ArrayList<>();
         List<List<Float>> glow = new ArrayList<>();
         for (int i = 0; i < 5; i++) {
            face.add(new ArrayList<>());
            part.add(new ArrayList<>());
            glow.add(new ArrayList<>());
         }
         for (int g = 0; g < 2; g++) {
            float[] d = gr[g];
            for (int q = 0; q < d.length; q += 32) {
               int dst = S_WHEEL;
               if (g == 0) {
                  float r = 0.0F;
                  for (int v = 0; v < 4; v++) {
                     int o = q + v * 8;
                     r += (float)Math.sqrt((double)(d[o] * d[o] + d[o + 2] * d[o + 2]));
                  }
                  r /= 4.0F;
                  dst = r < boltR ? S_BOLT : (r < ringR ? S_RING : S_WHEEL);
               }
               for (int k = 0; k < 32; k++) {
                  face.get(dst).add(d[q + k]);
                  if (g == 0) {
                     glow.get(dst).add(d[q + k]);
                  }
               }
            }
         }
         for (float f : this.under) {
            part.get(S_WHEEL).add(f);
         }
         String[] st = BeyPart.stock(this.kind);
         BeyPart stockTip = BeyPart.get(st[4]);
         float tipTop = stockTip == null ? 0.36F : PartLathe.tipTop(stockTip.tip());
         float[] l = this.lathe;
         for (int q = 0; q < l.length; q += 32) {
            float y = 0.0F;
            for (int v = 0; v < 4; v++) {
               y += l[q + v * 8 + 1];
            }
            int dst = y / 4.0F < tipTop ? S_TIP : S_TRACK;
            for (int k = 0; k < 32; k++) {
               part.get(dst).add(l[q + k]);
            }
         }
         float[][] fs = new float[5][];
         float[][] ps = new float[5][];
         float[][] gs = new float[5][];
         for (int i = 0; i < 5; i++) {
            gs[i] = toArray(glow.get(i));
            fs[i] = toArray(face.get(i));
            ps[i] = toArray(part.get(i));
         }
         this.partSlices = ps;
         this.glowSlices = gs;
         this.faceSlices = fs;
      }
   }

   /**
    * Draws one part of the bey (for the GeckoLib bone of the same name). The pose must already hold the bone
    * transform and the mesh scale. Bolt, ring and wheel come from the bey's own mesh; track and tip come from the
    * mesh when they are the stock ones, otherwise they are turned on the lathe from the loadout.
    */
   public void renderSlice(int slice, PoseStack pose, MultiBufferSource buffers, int light, int overlay, Loadout lo, float gr, float gg, float gb) {
      this.slice();
      boolean stock = lo == null || this.stockBottom(lo);
      if (slice <= S_WHEEL) {
         float dy = stock ? 0.0F : this.lift(lo);
         pose.pushPose();
         pose.translate(0.0F, dy, 0.0F);
         emit(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(this.FACE)), this.faceSlices[slice], light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
         if (this.partSlices[slice].length > 0) {
            emit(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(this.PARTS)), this.partSlices[slice], light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
         }
         if (gr > 0.003F || gg > 0.003F || gb > 0.003F) {
            emit(pose, buffers.getBuffer(RenderType.eyes(this.GLOW)), this.glowSlices[slice], 15728880, OverlayTexture.NO_OVERLAY, gr, gg, gb, 1.0F);
         }
         pose.popPose();
      } else if (stock) {
         emit(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(this.PARTS)), this.partSlices[slice], light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
      } else if (slice == S_TRACK) {
         PartLathe.renderTrack(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(PLAIN)), light, overlay, lo);
      } else {
         PartLathe.renderTip(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(PLAIN)), light, overlay, lo);
      }
   }

   public void renderGhostDisc(PoseStack pose, MultiBufferSource buffers, int light, float alpha, float y) {
      VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(this.FACE));
      Matrix4f m = pose.last().pose();
      Matrix3f n = pose.last().normal();
      float s = this.ghostHalf;
      gv(vc, m, n, -s, y, -s, 0.0F, 0.0F, light, alpha);
      gv(vc, m, n, -s, y, s, 0.0F, 1.0F, light, alpha);
      gv(vc, m, n, s, y, s, 1.0F, 1.0F, light, alpha);
      gv(vc, m, n, s, y, -s, 1.0F, 0.0F, light, alpha);
   }

   private static void gv(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v, int light, float a) {
      VClient.vertex(vc, m, n, x, y, z, 1.0F, 1.0F, 1.0F, a, u, v, OverlayTexture.NO_OVERLAY, light, 0.0F, 1.0F, 0.0F);
   }

   static void emit(PoseStack pose, VertexConsumer vc, float[] d, int light, int overlay, float r, float g, float b, float a) {
      Matrix4f m = pose.last().pose();
      Matrix3f n = pose.last().normal();

      for (int i = 0; i < d.length; i += 8) {
         VClient.vertex(vc, m, n, d[i], d[i + 1], d[i + 2], r, g, b, a, d[i + 3], d[i + 4], overlay, light, d[i + 5], d[i + 6], d[i + 7]);
      }
   }
}
