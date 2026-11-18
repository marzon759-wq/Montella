package com.krolasyon.livingmotion.client.layer;

import com.krolasyon.livingmotion.client.model.MobRenderer;
import com.krolasyon.livingmotion.client.model.ReplacedMob;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** Full-bright eyes (vanilla spider_eyes texture) drawn over the animated model. */
public class GlowLayer extends GeoRenderLayer<ReplacedMob> {
    private final MobRenderer<?> mob;
    private final ResourceLocation texture;

    public GlowLayer(MobRenderer<?> renderer, ResourceLocation texture) {
        super(renderer);
        this.mob = renderer;
        this.texture = texture;
    }

    @Override
    public void render(PoseStack poseStack, ReplacedMob animatable, BakedGeoModel model, RenderType renderType,
                       MultiBufferSource buffers, VertexConsumer buffer, float partialTick, int light, int overlay) {
        LivingEntity e = mob.getCurrentEntity();
        if (e == null || e.isInvisible()) return;
        RenderType rt = RenderType.eyes(texture);
        mob.reRender(model, poseStack, buffers, animatable, rt, buffers.getBuffer(rt), partialTick, 0xF00000,
                OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
        buffers.getBuffer(renderType);
    }
}
