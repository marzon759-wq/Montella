package com.krolasyon.mimic.compat;

import com.krolasyon.mimic.client.ClientEvents;
import com.krolasyon.mimic.client.ClientState;
import com.krolasyon.mimic.client.Hud;
import com.krolasyon.mimic.entity.GuestEntity;
import com.krolasyon.mimic.entity.MimicEntity;
import com.krolasyon.mimic.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Client glue: Forge 1.20.1 / GeckoLib 4.4 */
public final class ClientCompat {
    private ClientCompat() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(ModEntities.MIMIC.get(), MimicRenderer::new);
            e.registerEntityRenderer(ModEntities.LILY.get(), GuestRenderer::new);
            e.registerEntityRenderer(ModEntities.TRUCK.get(), TruckRenderer::new);
        });
        modBus.addListener((RegisterGuiOverlaysEvent e) ->
                e.registerAboveAll("mimic_hud", (gui, g, pt, w, h) -> Hud.render(g, pt, w, h)));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> {
            if (e.phase == TickEvent.Phase.END) ClientEvents.clientTick();
        });
        MinecraftForge.EVENT_BUS.addListener((TickEvent.RenderTickEvent e) -> {
            if (e.phase == TickEvent.Phase.START) ClientEvents.renderTick();
        });
    }

    public static class MimicModel extends DefaultedEntityGeoModel<MimicEntity> {
        public MimicModel() { super(Compat.rl("mimic")); }

        @Override
        public void setCustomAnimations(MimicEntity e, long id, AnimationState<MimicEntity> st) {
            CoreGeoBone look = getAnimationProcessor().getBone("head_look");
            if (look != null && e.headTracks()) {
                EntityModelData d = st.getData(DataTickets.ENTITY_MODEL_DATA);
                if (d != null) {
                    look.setRotX(look.getRotX() + Mth.clamp(d.headPitch(), -50, 50) * Mth.DEG_TO_RAD);
                    look.setRotY(look.getRotY() + Mth.clamp(d.netHeadYaw(), -80, 80) * Mth.DEG_TO_RAD);
                }
            }
        }
    }

    public static class MimicRenderer extends GeoEntityRenderer<MimicEntity> {
        public MimicRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new MimicModel());
            this.shadowRadius = 0.7f;
            addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this));
        }

        @Override
        public void render(MimicEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            super.render(e, yaw, pt, ps, buf, light);
        }
    }

    public static class GuestModel extends DefaultedEntityGeoModel<GuestEntity> {
        public GuestModel() { super(Compat.rl("lily")); }


        @Override
        public void setCustomAnimations(GuestEntity e, long id, AnimationState<GuestEntity> st) {
            CoreGeoBone head = getAnimationProcessor().getBone("head");
            EntityModelData d = st.getData(DataTickets.ENTITY_MODEL_DATA);
            if (head != null && d != null) {
                head.setRotX(head.getRotX() + Mth.clamp(d.headPitch(), -40, 40) * Mth.DEG_TO_RAD);
                head.setRotY(head.getRotY() + Mth.clamp(d.netHeadYaw(), -85, 85) * Mth.DEG_TO_RAD);
            }
        }
    }

    public static class GuestRenderer extends GeoEntityRenderer<GuestEntity> {
        public GuestRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new GuestModel());
            this.shadowRadius = 0.3f;
        }
    }

    public static class TruckModel extends DefaultedEntityGeoModel<com.krolasyon.mimic.entity.TruckEntity> {
        public TruckModel() { super(Compat.rl("truck")); }

        @Override
        public ResourceLocation getTextureResource(com.krolasyon.mimic.entity.TruckEntity e) {
            return Compat.rl(e.lightsOn() ? "textures/entity/truck_lit.png" : "textures/entity/truck.png");
        }
    }

    public static class TruckRenderer extends GeoEntityRenderer<com.krolasyon.mimic.entity.TruckEntity> {
        public TruckRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new TruckModel());
            this.shadowRadius = 1.4f;
            addRenderLayer(new software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer<>(this));
        }
    }
}
