package com.krolasyon.kaiju.client;

import com.krolasyon.kaiju.KaijuMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = KaijuMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(KaijuModel.LAYER, KaijuModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(KaijuMod.KAIJU.get(), KaijuRenderer::new);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        if (System.getProperty("kaiju.shots") != null) MinecraftForge.EVENT_BUS.register(ShotHook.class);
    }
}
