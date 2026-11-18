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

import java.util.List;
import java.util.function.Function;

/**
 * Draws the same animated mesh again with extra textures on top: glowing eyes (spider, enderman), iron golem
 * cracks, villager biome / profession / level outfits.
 */
public class OverlayLayer extends GeoRenderLayer<ReplacedMob> {
    private final MobRenderer<?> mob;
    private final Function<LivingEntity, List<ResourceLocation>> textures;
    private final boolean glow;

    public OverlayLayer(MobRenderer<?> renderer, Function<LivingEntity, List<ResourceLocation>> textures, boolean glow) {
        super(renderer);
        this.mob = renderer;
        this.textures = textures;
        this.glow = glow;
    }

    @Override
    public void render(PoseStack poseStack, ReplacedMob animatable, BakedGeoModel model, RenderType renderType,
                       MultiBufferSource buffers, VertexConsumer buffer, float partialTick, int light, int overlay) {
        LivingEntity e = mob.getCurrentEntity();
        if (e == null || e.isInvisible()) return;
        List<ResourceLocation> list = textures.apply(e);
        if (list == null || list.isEmpty()) return;
        for (ResourceLocation tex : list) {
            RenderType rt = glow ? RenderType.eyes(tex) : RenderType.entityCutoutNoCull(tex);
            mob.reRender(model, poseStack, buffers, animatable, rt, buffers.getBuffer(rt), partialTick,
                    glow ? 0xF00000 : light, glow ? OverlayTexture.NO_OVERLAY : overlay, 1f, 1f, 1f, 1f);
        }
        buffers.getBuffer(renderType);
    }
}
