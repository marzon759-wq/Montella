package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.StadiumEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class XtremeFx {
   private static final float[] ORANGE = new float[]{1.0F, 0.5F, 0.1F};
   private static final float[] HOT = new float[]{1.0F, 0.85F, 0.5F};
   private static final float[] BLUE = new float[]{0.3F, 0.6F, 1.0F};

   private XtremeFx() {
   }

   private static float[] k(float[] c, float k) {
      return new float[]{c[0] * k, c[1] * k, c[2] * k};
   }

   public static void render(BeybladeEntity e, float partial, float t, double ex, double ey, double ez, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      float rail = t - (float)e.railClientTime;
      float dash = t - (float)e.xdashClientTime;
      float out = t - (float)e.outClientTime;
      if (!(rail > 4.0F) || !(dash > 16.0F) || !(out > 30.0F)) {
         Matrix4f m = pose.last().pose();
         Vec3 vel = new Vec3(e.getX() - e.xo, 0.0, e.getZ() - e.zo);
         double sp = vel.length();
         Vec3 back = sp < 0.001 ? new Vec3(-1.0, 0.0, 0.0) : vel.scale(-1.0 / sp);
         if (rail >= 0.0F && rail <= 4.0F) {
            StadiumEntity st = stadium(e);
            Vec3 out2 = st == null ? new Vec3(1.0, 0.0, 0.0) : new Vec3(e.getX() - st.getX(), 0.0, e.getZ() - st.getZ()).normalize();
            Vec3 c = out2.scale(0.40499998927116393).add(0.0, 0.08, 0.0);
            VertexConsumer vc = Fx.buf(buf, Fx.BOLT);

            for (int i = 0; i < 4; i++) {
               double a = (double)t * 1.7 + (double)i * 1.6;
               Vec3 tip = c.add(back.scale(0.8 + 0.3 * Math.sin(a)))
                  .add(out2.scale(0.2 * Math.cos(a)))
                  .add(0.0, 0.15 + 0.15 * Math.sin(a * 1.3), 0.0);
               Fx.beamV(vc, m, c, tip, 0.25F, k(HOT, 1.4F), 0.5F, t + (float)i);
            }

            vc = Fx.buf(buf, Fx.SOFT);
            Fx.beam(
               vc,
               m,
               new Vec3(0.0, 0.25, 0.0),
               back.scale(Math.min(4.0, sp * 6.0)).add(0.0, 0.25, 0.0),
               0.45F,
               k(ORANGE, 1.4F),
               k(ORANGE, 0.0F),
               0.5F,
               0.0F
            );
            vc = Fx.buf(buf, Fx.FLARE);
            Fx.billboard(vc, pose, cam, c.x, c.y, c.z, 0.9F, t * 20.0F, k(HOT, 1.0F));
         }

         if (dash >= 0.0F && dash <= 16.0F) {
            float f = 1.0F - dash / 16.0F;
            VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
            Fx.beam(
               vc,
               m,
               new Vec3(0.0, 0.35, 0.0),
               back.scale(Math.min(7.0, sp * 8.0)).add(0.0, 0.35, 0.0),
               0.9F * f + 0.2F,
               k(HOT, 1.6F * f),
               k(ORANGE, 0.0F),
               0.5F,
               0.0F
            );
            vc = Fx.buf(buf, Fx.WAVE);
            Fx.ring(vc, m, 0.08F, 0.6F, 1.3F, 32, new float[]{0.0F, 0.0F, 0.0F}, k(ORANGE, 1.2F * f), 4.0F, t * 0.1F);
            if (dash < 6.0F) {
               vc = Fx.buf(buf, Fx.FLARE);
               Fx.billboard(vc, pose, cam, 0.0, 0.4, 0.0, 3.2F * (1.0F - dash / 6.0F), t * 8.0F, k(HOT, 1.3F));
            }
         }

         if (out >= 0.0F && out <= 30.0F) {
            StadiumEntity st = stadium(e);
            boolean xtreme = st != null && StadiumEntity.zoneAt(StadiumEntity.deg(e.getX() - st.getX(), e.getZ() - st.getZ())) == 2;
            float[] c = xtreme ? ORANGE : BLUE;
            float f = 1.0F - out / 30.0F;
            VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
            Fx.band(vc, m, 0.7F, 0.0F, 0.9F, 6.0F * Mth.clamp(out / 6.0F, 0.0F, 1.0F), 24, k(c, 1.4F * f), k(c, 0.0F), 1.0F, t * 0.1F, 1.0F, 0.0F);
            if (out < 10.0F) {
               vc = Fx.buf(buf, Fx.FLARE);
               Fx.billboard(vc, pose, cam, 0.0, 0.6, 0.0, 5.0F * (1.0F - out / 10.0F), t * 4.0F, k(c, 1.5F));
            }
         }
      }
   }

   private static StadiumEntity stadium(BeybladeEntity e) {
      Iterator var1 = e.level().getEntitiesOfClass(StadiumEntity.class, e.getBoundingBox().inflate(16.0, 6.0, 16.0)).iterator();
      return var1.hasNext() ? (StadiumEntity)var1.next() : null;
   }
}
