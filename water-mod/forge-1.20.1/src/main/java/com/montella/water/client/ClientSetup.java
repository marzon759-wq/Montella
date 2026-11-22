package com.montella.water.client;

import com.montella.water.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::particles);
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::tick);
        MinecraftForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> { WaterfallTracker.reset(); WaterFx.reset(); });
    }

    private static void particles(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(ModParticles.SPRAY.get(), WaterParticles.Spray.Provider::new);
        e.registerSpriteSet(ModParticles.MIST.get(), WaterParticles.Mist.Provider::new);
        e.registerSpriteSet(ModParticles.FOAM.get(), WaterParticles.Foam.Provider::new);
        e.registerSpriteSet(ModParticles.RIPPLE.get(), WaterParticles.Ripple.Provider::new);
    }

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        WaterfallTracker.tick(mc);
        WaterFx.tick(mc);
    }
}
