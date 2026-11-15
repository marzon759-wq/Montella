package com.krolasyon.valdoria.client.render;

import com.krolasyon.valdoria.ValdoriaMod;
import com.krolasyon.valdoria.client.model.GloomlynxModel;
import com.krolasyon.valdoria.client.model.LumenMothModel;
import com.krolasyon.valdoria.client.model.ValdoriaModels;
import com.krolasyon.valdoria.client.model.VeilstagModel;
import com.krolasyon.valdoria.client.model.VinestalkerModel;
import com.krolasyon.valdoria.entity.Gloomlynx;
import com.krolasyon.valdoria.entity.LumenMoth;
import com.krolasyon.valdoria.entity.Veilstag;
import com.krolasyon.valdoria.entity.Vinestalker;
import com.krolasyon.valdoria.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ValdoriaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ValdoriaRenderers {
    private ValdoriaRenderers() {}

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ValdoriaModels.VEILSTAG, ValdoriaModels::createVeilstag);
        event.registerLayerDefinition(ValdoriaModels.GLOOMLYNX, ValdoriaModels::createGloomlynx);
        event.registerLayerDefinition(ValdoriaModels.VINESTALKER, ValdoriaModels::createVinestalker);
        event.registerLayerDefinition(ValdoriaModels.LUMEN_MOTH, ValdoriaModels::createLumenMoth);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.VEILSTAG.get(), ctx -> new Glowing<Veilstag, VeilstagModel>(ctx, new VeilstagModel(ctx.bakeLayer(ValdoriaModels.VEILSTAG)), 0.7F, "veilstag") {
            @Override
            protected void scale(Veilstag e, PoseStack pose, float pt) {
                if (e.isBaby()) pose.scale(0.55F, 0.55F, 0.55F);
            }
        });
        event.registerEntityRenderer(ModEntities.GLOOMLYNX.get(), ctx -> new Glowing<>(ctx, new GloomlynxModel(ctx.bakeLayer(ValdoriaModels.GLOOMLYNX)), 0.5F, "gloomlynx"));
        event.registerEntityRenderer(ModEntities.VINESTALKER.get(), ctx -> new Glowing<Vinestalker, VinestalkerModel>(ctx, new VinestalkerModel(ctx.bakeLayer(ValdoriaModels.VINESTALKER)), 0.8F, "vinestalker") {
            @Override
            protected void setupRotations(Vinestalker e, PoseStack pose, float bob, float yaw, float pt) {
                super.setupRotations(e, pose, bob, yaw, pt);
                if (e.isHanging()) {
                    pose.translate(0, e.getBbHeight() + 0.05F, 0);
                    pose.mulPose(Axis.ZP.rotationDegrees(180));
                }
            }
        });
        event.registerEntityRenderer(ModEntities.LUMEN_MOTH.get(), ctx -> new Glowing<LumenMoth, LumenMothModel>(ctx, new LumenMothModel(ctx.bakeLayer(ValdoriaModels.LUMEN_MOTH)), 0.0F, "lumen_moth") {
            @Override
            protected void scale(LumenMoth e, PoseStack pose, float pt) {
                pose.scale(0.6F, 0.6F, 0.6F);
            }

            @Override
            protected int getBlockLightLevel(LumenMoth e, BlockPos pos) {
                return 15;
            }
        });
    }

    /** A mob renderer with an extra full-bright layer for eyes, antler tips, spots and wing veins. */
    static class Glowing<T extends Mob, M extends EntityModel<T>> extends MobRenderer<T, M> {
        private final ResourceLocation texture;

        Glowing(EntityRendererProvider.Context ctx, M model, float shadow, String name) {
            super(ctx, model, shadow);
            this.texture = ValdoriaMod.id("textures/entity/" + name + ".png");
            RenderType glow = RenderType.eyes(ValdoriaMod.id("textures/entity/" + name + "_glow.png"));
            addLayer(new EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return glow;
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(T entity) {
            return texture;
        }
    }
}
