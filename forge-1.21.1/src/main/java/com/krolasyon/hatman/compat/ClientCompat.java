package com.krolasyon.hatman.compat;

import com.krolasyon.hatman.client.ClientEvents;
import com.krolasyon.hatman.client.ClientState;
import com.krolasyon.hatman.client.Hud;
import com.krolasyon.hatman.entity.GuestEntity;
import com.krolasyon.hatman.entity.HatmanEntity;
import com.krolasyon.hatman.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Client glue: Forge 1.21.1 / GeckoLib 4.8 */
public final class ClientCompat {
    private ClientCompat() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(ModEntities.HATMAN.get(), HatmanRenderer::new);
            e.registerEntityRenderer(ModEntities.GUEST.get(), GuestRenderer::new);
        });
        MinecraftForge.EVENT_BUS.addListener((CustomizeGuiOverlayEvent.Chat e) ->
                Hud.render(e.getGuiGraphics(), e.getPartialTick(), e.getWindow().getGuiScaledWidth(), e.getWindow().getGuiScaledHeight()));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post e) -> ClientEvents.clientTick());
        MinecraftForge.EVENT_BUS.addListener((TickEvent.RenderTickEvent.Pre e) -> ClientEvents.renderTick());
    }

    public static class HatmanModel extends DefaultedEntityGeoModel<HatmanEntity> {
        public HatmanModel() { super(Compat.rl("hatman")); }

        @Override
        public void setCustomAnimations(HatmanEntity e, long id, AnimationState<HatmanEntity> st) {
            GeoBone look = getAnimationProcessor().getBone("head_look");
            if (look != null && e.headTracks()) {
                EntityModelData d = st.getData(DataTickets.ENTITY_MODEL_DATA);
                if (d != null) {
                    look.setRotX(look.getRotX() + Mth.clamp(d.headPitch(), -50, 50) * Mth.DEG_TO_RAD);
                    look.setRotY(look.getRotY() + Mth.clamp(d.netHeadYaw(), -80, 80) * Mth.DEG_TO_RAD);
                }
            }
        }
    }

    public static class HatmanRenderer extends GeoEntityRenderer<HatmanEntity> {
        public HatmanRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new HatmanModel());
            this.shadowRadius = 0.0f;
        }

        @Override
        public void render(HatmanEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            if (e.cameraOnly() && !ClientState.nvOn) return;
            super.render(e, yaw, pt, ps, buf, light);
        }
    }

    public static class GuestModel extends DefaultedEntityGeoModel<GuestEntity> {
        public GuestModel() { super(Compat.rl("guest")); }

        @Override
        public ResourceLocation getTextureResource(GuestEntity e) {
            return Compat.rl("textures/entity/guest_" + (e.getData() & 3) % 3 + ".png");
        }

        @Override
        public void setCustomAnimations(GuestEntity e, long id, AnimationState<GuestEntity> st) {
            GeoBone head = getAnimationProcessor().getBone("head");
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
}
