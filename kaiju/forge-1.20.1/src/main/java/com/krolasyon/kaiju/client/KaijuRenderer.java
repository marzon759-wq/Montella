package com.krolasyon.kaiju.client;

import com.krolasyon.kaiju.KaijuMod;
import com.krolasyon.kaiju.entity.KaijuEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class KaijuRenderer extends MobRenderer<KaijuEntity, KaijuModel> {
    static final String[] NAMES = {"atomic", "burning"};
    private static final ResourceLocation[] SKIN = new ResourceLocation[NAMES.length];
    private static final ResourceLocation[] GLOW = new ResourceLocation[NAMES.length];

    static {
        for (int i = 0; i < NAMES.length; i++) {
            SKIN[i] = new ResourceLocation(KaijuMod.MODID, "textures/entity/kaiju_" + NAMES[i] + ".png");
            GLOW[i] = new ResourceLocation(KaijuMod.MODID, "textures/entity/kaiju_" + NAMES[i] + "_glow.png");
        }
    }

    public KaijuRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KaijuModel(ctx.bakeLayer(KaijuModel.LAYER)), 2.4F * KaijuEntity.SCALE);
        addLayer(new Glow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(KaijuEntity e) {
        return SKIN[Math.floorMod(e.getVariant(), SKIN.length)];
    }

    @Override
    protected void scale(KaijuEntity e, PoseStack pose, float partialTick) {
        pose.scale(KaijuEntity.SCALE, KaijuEntity.SCALE, KaijuEntity.SCALE);
    }

    /** full-bright additive layer: blue dorsal fins (atomic) or molten fissures (burning), slowly pulsing */
    static class Glow extends RenderLayer<KaijuEntity, KaijuModel> {
        Glow(RenderLayerParent<KaijuEntity, KaijuModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buf, int light, KaijuEntity e, float swing, float amount,
                           float partial, float age, float headYaw, float headPitch) {
            int v = Math.floorMod(e.getVariant(), GLOW.length);
            float k = v == 0 ? 0.4F + 0.15F * Mth.sin(age * 0.05F) : 0.8F + 0.2F * Mth.sin(age * 0.11F) * Mth.sin(age * 0.037F);
            getParentModel().renderToBuffer(pose, buf.getBuffer(RenderType.eyes(GLOW[v])), 15728640,
                    OverlayTexture.NO_OVERLAY, k, k, k, 1.0F);
        }
    }
}
