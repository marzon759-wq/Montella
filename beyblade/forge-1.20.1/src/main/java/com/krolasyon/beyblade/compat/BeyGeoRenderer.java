package com.krolasyon.beyblade.compat;

import com.krolasyon.beyblade.client.BeyBones;
import com.krolasyon.beyblade.client.BeyFx;
import com.krolasyon.beyblade.client.BeyRig;
import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** GeckoLib renderer of every beyblade (Forge 1.20.1 / GeckoLib 4.4). The logic is in BeyRig / BeyBones / BeyFx. */
public class BeyGeoRenderer extends GeoEntityRenderer<BeybladeEntity> {
   public static final ResourceLocation GEO = V.rl("librabeyblade", "geo/beyblade.geo.json");
   public static final ResourceLocation ANIM = V.rl("librabeyblade", "animations/beyblade.animation.json");
   public static final ResourceLocation TEX = V.rl("librabeyblade", "textures/entity/part_plain.png");

   public BeyGeoRenderer(EntityRendererProvider.Context ctx) {
      super(ctx, new Model());
      this.shadowRadius = 0.42F;
      this.shadowStrength = 0.8F;
      this.addRenderLayer(new PartsLayer(this));
   }

   @Override
   public void render(BeybladeEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buf, int light) {
      BeyRig rig = BeyRig.of(e, partial);
      BeyFx.before(rig, pose, buf, light);
      super.render(e, yaw, partial, pose, buf, light);
      BeyFx.after(rig, pose, buf, light, this.entityRenderDispatcher.cameraOrientation());
   }

   /** The bey's own spin is the rig's business, not the entity yaw. */
   @Override
   protected void applyRotations(BeybladeEntity e, PoseStack pose, float age, float yaw, float partial) {
   }

   @Override
   public RenderType getRenderType(BeybladeEntity e, ResourceLocation tex, MultiBufferSource buf, float partial) {
      return RenderType.entityCutoutNoCull(tex);
   }

   public static class Model extends GeoModel<BeybladeEntity> {
      @Override
      public ResourceLocation getModelResource(BeybladeEntity e) {
         return GEO;
      }

      @Override
      public ResourceLocation getTextureResource(BeybladeEntity e) {
         return TEX;
      }

      @Override
      public ResourceLocation getAnimationResource(BeybladeEntity e) {
         return ANIM;
      }

      @Override
      public void setCustomAnimations(BeybladeEntity e, long id, AnimationState<BeybladeEntity> st) {
         BeyRig rig = BeyRig.of(e, st.getPartialTick());
         rig.apply(bone(BeyBones.ROOT), bone(BeyBones.TILT), bone(BeyBones.SPIN), bone(BeyBones.TIP_LAG));
      }

      private GeoBone bone(String n) {
         return (GeoBone)this.getAnimationProcessor().getBone(n);
      }
   }

   public static class PartsLayer extends GeoRenderLayer<BeybladeEntity> {
      public PartsLayer(GeoRenderer<BeybladeEntity> r) {
         super(r);
      }

      @Override
      public void renderForBone(PoseStack pose, BeybladeEntity e, GeoBone bone, RenderType type, MultiBufferSource buf, VertexConsumer vc, float partial, int light, int overlay) {
         BeyBones.render(BeyRig.of(e, partial), bone.getName(), pose, buf, light);
      }
   }
}
