package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.entity.CrateEntity;
import com.krolasyon.bloodsea.world.Waves;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/** A barrel of cargo, low in the water, rolling with the swell. */
public class CrateRenderer extends EntityRenderer<CrateEntity> {
    public CrateRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    @Override
    public void render(CrateEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        double t = Waves.time(e.level(), pt);
        double[] s = Waves.slope(e.getX(), e.getZ(), t);
        ps.pushPose();
        ps.translate(0, Waves.height(e.getX(), e.getZ(), t) * 0.9, 0);
        ps.mulPose(Axis.YP.rotationDegrees(e.getYRot()));
        ps.mulPose(Axis.XP.rotationDegrees((float) Math.toDegrees(Math.atan(s[1])) + 72f));
        ps.mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(Math.atan(s[0]))));
        ps.scale(0.95f, 0.95f, 0.95f);
        ps.translate(-0.5, -0.5, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.BARREL.defaultBlockState(), ps, buf, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CrateEntity e) { return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS; }
}
