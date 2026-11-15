package com.krolasyon.titans.client;

import com.krolasyon.titans.TitansMod;
import com.krolasyon.titans.entity.TitanEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TitansMod.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(TitansMod.KORGATH.get(), TitanRenderer::new);
        e.registerEntityRenderer(TitansMod.AZHDAR.get(), TitanRenderer::new);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        TitanEntity.SHAKER = Shake::add;
        MinecraftForge.EVENT_BUS.addListener(Shake::tick);
        MinecraftForge.EVENT_BUS.addListener(Shake::camera);
        AutoTest.init();
    }
}
