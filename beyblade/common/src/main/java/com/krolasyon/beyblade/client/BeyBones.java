package com.krolasyon.beyblade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/** What is drawn at each GeckoLib bone of the beyblade (called by the version specific render layer). */
public final class BeyBones {
   public static final String ROOT = "root";
   public static final String TILT = "tilt";
   public static final String MOTION = "motion";
   public static final String SPIN = "spin";
   public static final String TIP_LAG = "tip_lag";
   public static final String TIP = "tip";
   public static final String TRACK = "track";
   public static final String WHEEL = "wheel";
   public static final String RING = "ring";
   public static final String BOLT = "bolt";

   private BeyBones() {
   }

   public static void render(BeyRig rig, String bone, PoseStack pose, MultiBufferSource buf, int light) {
      int slice = switch (bone) {
         case BOLT -> BeyMesh.S_BOLT;
         case RING -> BeyMesh.S_RING;
         case WHEEL -> BeyMesh.S_WHEEL;
         case TRACK -> BeyMesh.S_TRACK;
         case TIP -> BeyMesh.S_TIP;
         case MOTION -> -2;
         default -> -1;
      };
      if (slice == -1) {
         return;
      }
      pose.pushPose();
      pose.scale(BeyRig.SCALE, BeyRig.SCALE, BeyRig.SCALE);
      if (slice == -2) {
         BeyFx.ghosts(rig, pose, buf, light);
      } else {
         float g = rig.glow + rig.flash * 0.8F;
         rig.mesh.renderSlice(slice, pose, buf, light, rig.overlay, rig.parts, rig.glowCol[0] * g, rig.glowCol[1] * g, rig.glowCol[2] * g);
      }
      pose.popPose();
   }
}
