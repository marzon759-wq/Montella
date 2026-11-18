package com.krolasyon.livingmotion.client.layer;

import com.krolasyon.livingmotion.client.model.MobRenderer;
import com.krolasyon.livingmotion.client.model.ReplacedMob;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/** The block an enderman carries, held in front of its body like vanilla. */
public class CarriedBlockLayer extends GeoRenderLayer<ReplacedMob> {
    private final MobRenderer<?> mob;
    private final BlockRenderDispatcher blocks;

    public CarriedBlockLayer(MobRenderer<?> renderer, EntityRendererProvider.Context ctx) {
        super(renderer);
        this.mob = renderer;
        this.blocks = ctx.getBlockRenderDispatcher();
    }

    @Override
    public void renderForBone(PoseStack ps, ReplacedMob animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource buffers, VertexConsumer buffer, float partialTick, int light, int overlay) {
        if (!"torso".equals(bone.getName()) || !(mob.getCurrentEntity() instanceof EnderMan e)) return;
        BlockState state = e.getCarriedBlock();
        if (state == null) return;
        ps.pushPose();
        ps.translate(0, 1.501f, 0);      // back to vanilla model space
        ps.scale(-1f, -1f, 1f);
        ps.translate(0.0F, 0.6875F, -0.75F);
        ps.mulPose(Axis.XP.rotationDegrees(20.0F));
        ps.mulPose(Axis.YP.rotationDegrees(45.0F));
        ps.translate(0.25F, 0.1875F, 0.25F);
        ps.scale(-0.5F, -0.5F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(90.0F));
        blocks.renderSingleBlock(state, ps, buffers, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        buffers.getBuffer(renderType);
    }
}
