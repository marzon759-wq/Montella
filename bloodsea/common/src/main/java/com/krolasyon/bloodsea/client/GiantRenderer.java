package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.compat.Compat;
import com.krolasyon.bloodsea.entity.GiantEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LightLayer;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Draws a giant: the model is chosen by its variant; it is lit by the air above the water, not the murk it lies in. */
public class GiantRenderer extends GeoEntityRenderer<GiantEntity> {
    public GiantRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        this.shadowRadius = 0f;
    }

    /** GeckoLib does not turn entities that are not alive: turn the model to the giant's yaw ourselves */
    @Override
    public void render(GiantEntity e, float yaw, float pt, com.mojang.blaze3d.vertex.PoseStack ps, net.minecraft.client.renderer.MultiBufferSource buf, int light) {
        ps.pushPose();
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180f - e.getYRot()));
        super.render(e, yaw, pt, ps, buf, light);
        ps.popPose();
    }

    @Override
    protected int getSkyLightLevel(GiantEntity e, BlockPos p) { return e.level().getBrightness(LightLayer.SKY, p.above(8)); }

    @Override
    protected int getBlockLightLevel(GiantEntity e, BlockPos p) { return e.level().getBrightness(LightLayer.BLOCK, p.above(8)); }

    public static class Model extends GeoModel<GiantEntity> {
        private static final ResourceLocation TEX = Compat.rl("textures/entity/giant.png");
        private static final ResourceLocation ANIM = Compat.rl("animations/giant.animation.json");

        @Override public ResourceLocation getModelResource(GiantEntity e) { return Compat.rl("geo/" + e.getVariant() + ".geo.json"); }

        @Override public ResourceLocation getTextureResource(GiantEntity e) { return TEX; }

        @Override public ResourceLocation getAnimationResource(GiantEntity e) { return ANIM; }
    }
}
