package com.krolasyon.kaiju.client;

import com.krolasyon.kaiju.entity.DebrisEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** a carried or flying piece of the world, drawn block by block */
public class DebrisRenderer extends EntityRenderer<DebrisEntity> {
    public DebrisRenderer(EntityRendererProvider.Context c) { super(c); shadowRadius = 0; }

    @Override
    public boolean shouldRender(DebrisEntity e, Frustum f, double x, double y, double z) { return true; }

    @Override
    public void render(DebrisEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        BlockRenderDispatcher br = Minecraft.getInstance().getBlockRenderer();
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pt, e.prevRotY, e.rotY)));
        ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(pt, e.prevRotX, e.rotX)));
        for (int i = 0; i < e.states.size(); i++) {
            int[] r = e.rel.get(i);
            ps.pushPose();
            ps.translate(r[0] - 0.5, r[1], r[2] - 0.5);
            br.renderSingleBlock(e.states.get(i), ps, buf, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DebrisEntity e) { return TextureAtlas.LOCATION_BLOCKS; }
}
