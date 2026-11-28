package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * The clash itself: the two beys' energies meet in a flash of both colours, wind fans out sideways from the
 * contact point in curling streaks, a dust ring races across the floor; heavy hits add radial speed lines.
 */
public final class ClashFx {
   private ClashFx() {
   }

   public static void render(BeyRig rig, PoseStack pose, MultiBufferSource buf, Quaternionf cam) {
      BeybladeEntity e = rig.e;
      if (e.getId() > e.clashOther) {
         return;
      }
      float age = rig.t - (float)e.clashTime;
      boolean heavy = e.clashHeavy;
      float life = heavy ? 18.0F : 11.0F;
      if (age < 0.0F || age > life) {
         return;
      }
      float f = 1.0F - age / life;
      float g = easeOut(age / life);
      Vec3 c = new Vec3(e.clashX - rig.ex, e.clashY - rig.ey, e.clashZ - rig.ez);
      Vec3 axis = new Vec3(e.clashNX, 0.0, e.clashNZ);
      Vec3 perp = new Vec3(-axis.z, 0.0, axis.x);
      float[] ca = rig.glowCol;
      float[] cb = ca;
      Entity o = e.level().getEntity(e.clashOther);
      if (o instanceof BeybladeEntity ob) {
         cb = BeyRig.of(ob, rig.partial).glowCol;
         BeyRig.of(e, rig.partial);
      }
      Matrix4f m = pose.last().pose();
      // wind fans, both sides
      VertexConsumer vc = Fx.buf(buf, Fx.SOFT);
      int streaks = heavy ? 7 : 4;
      float reach = (heavy ? 5.5F : 2.8F) * g + 0.4F;
      for (int side = -1; side <= 1; side += 2) {
         for (int k = 0; k < streaks; k++) {
            float spread = (k - (streaks - 1) * 0.5F) / streaks;
            Vec3 prev = c.add(perp.scale(side * 0.25));
            for (int i = 1; i <= 8; i++) {
               float u = i / 8.0F;
               Vec3 p = c.add(perp.scale(side * (0.25 + u * reach)))
                  .add(axis.scale(spread * u * reach * 0.9 + Math.sin(u * 3.0F + k) * 0.25 * u))
                  .add(0.0, 0.05 + u * u * (heavy ? 0.9 : 0.4) * (0.3 + 0.7 * ((k * 7) % 5) / 4.0), 0.0);
               float al = f * (1.0F - u * 0.7F) * 0.9F;
               float[] col = k % 2 == 0 ? Fx.c(0.92F, 0.96F, 1.0F, al) : Fx.c((ca[0] + cb[0]) * 0.5F, (ca[1] + cb[1]) * 0.5F, (ca[2] + cb[2]) * 0.5F, al);
               Fx.beam(vc, m, prev, p, (heavy ? 0.16F : 0.1F) * (1.0F - u * 0.6F), col, col, 1.0F, rig.t * 0.2F);
               prev = p;
            }
         }
      }
      // dust ring along the floor
      vc = Fx.buf(buf, Fx.WAVE);
      float rr = 0.3F + g * (heavy ? 4.5F : 2.2F);
      pose.pushPose();
      pose.translate(c.x, c.y - 0.33, c.z);
      Fx.ring(vc, pose.last().pose(), 0.02F, rr - (heavy ? 0.9F : 0.5F), rr, 40, new float[]{0.0F, 0.0F, 0.0F}, Fx.c(0.85F, 0.8F, 0.7F, 0.9F * f), 4.0F, 0.0F);
      if (heavy) {
         // speed lines
         vc = Fx.buf(buf, Fx.SOFT);
         Matrix4f mm = pose.last().pose();
         for (int i = 0; i < 20; i++) {
            double a = i / 20.0 * Math.PI * 2.0 + rig.t * 0.05;
            Vec3 d = new Vec3(Math.cos(a), 0.0, Math.sin(a));
            float r0 = 0.6F + g * 2.0F;
            Fx.beam(vc, mm, d.scale(r0).add(0.0, 0.3, 0.0), d.scale(r0 + 1.6F * f).add(0.0, 0.35, 0.0), 0.05F, Fx.c(1.0F, 1.0F, 1.0F, f), Fx.c(1.0F, 1.0F, 1.0F, 0.0F), 1.0F, 0.0F);
         }
      }
      pose.popPose();
      // the two energies meet
      if (age < 7.0F) {
         float k = 1.0F - age / 7.0F;
         vc = Fx.buf(buf, Fx.FLARE);
         float s = (heavy ? 3.2F : 1.6F) * (0.6F + 0.4F * easeOut(age / 2.0F));
         Fx.billboard(vc, pose, cam, c.x - axis.x * 0.2, c.y, c.z - axis.z * 0.2, s, rig.t * 9.0F, Fx.c(ca[0], ca[1], ca[2], 1.4F * k));
         Fx.billboard(vc, pose, cam, c.x + axis.x * 0.2, c.y, c.z + axis.z * 0.2, s, -rig.t * 9.0F, Fx.c(cb[0], cb[1], cb[2], 1.4F * k));
         Fx.billboard(vc, pose, cam, c.x, c.y, c.z, s * 0.5F, 0.0F, Fx.c(1.0F, 1.0F, 1.0F, 1.6F * k));
      }
   }

   private static float easeOut(float x) {
      x = Mth.clamp(x, 0.0F, 1.0F);
      return 1.0F - (1.0F - x) * (1.0F - x) * (1.0F - x);
   }
}
