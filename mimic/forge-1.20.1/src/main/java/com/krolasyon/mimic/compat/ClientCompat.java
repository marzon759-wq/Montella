package com.krolasyon.mimic.compat;

import com.krolasyon.mimic.MimicMod;
import com.krolasyon.mimic.client.AutoTest;
import com.krolasyon.mimic.entity.MimicEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Client glue (Forge 1.20.1). */
public final class ClientCompat {
    private ClientCompat() {}

    public static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) -> e.registerEntityRenderer(MimicMod.MIMIC.get(), MimicRenderer::new));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> { if (e.phase == TickEvent.Phase.END) AutoTest.tick(); });
        AutoTest.init();
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
            this.shadowRadius = 0.6f;
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
    }
}
