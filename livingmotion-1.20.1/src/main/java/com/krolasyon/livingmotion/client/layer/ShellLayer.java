package com.krolasyon.livingmotion.client.layer;

import com.krolasyon.livingmotion.client.model.MobRenderer;
import com.krolasyon.livingmotion.client.model.ReplacedMob;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Re-renders the model's "*_outer" shell bones with another texture: stray clothing, the drowned outer layer and
 * the charged creeper energy swirl. The shells are children of the animated bones, so they follow every animation.
 */
public class ShellLayer extends GeoRenderLayer<ReplacedMob> {
    private final MobRenderer<?> mob;
    private final Predicate<LivingEntity> when;
    private final BiFunction<LivingEntity, Float, RenderType> type;
    private final boolean swirl;

    public ShellLayer(MobRenderer<?> renderer, Predicate<LivingEntity> when, BiFunction<LivingEntity, Float, RenderType> type, boolean swirl) {
        super(renderer);
        this.mob = renderer;
        this.when = when;
        this.type = type;
        this.swirl = swirl;
    }

    @Override
    public void render(PoseStack poseStack, ReplacedMob animatable, BakedGeoModel model, RenderType renderType,
                       MultiBufferSource buffers, VertexConsumer buffer, float partialTick, int light, int overlay) {
        LivingEntity e = mob.getCurrentEntity();
        if (e == null || !when.test(e) || e.isInvisible()) return;
        RenderType rt = type.apply(e, partialTick);
        mob.mobModel().showOverlay();
        float c = swirl ? 0.5f : 1f;
        mob.reRender(model, poseStack, buffers, animatable, rt, buffers.getBuffer(rt), partialTick, light,
                swirl ? OverlayTexture.NO_OVERLAY : overlay, c, c, c, 1f);
        mob.mobModel().showMain();
        buffers.getBuffer(renderType);
    }
}
