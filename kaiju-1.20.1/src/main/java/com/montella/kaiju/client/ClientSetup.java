package com.montella.kaiju.client;

import com.montella.kaiju.KaijuMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::renderers);
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::camera);
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::tick);
        if (System.getProperty("kaiju.autotest") != null) AutoTest.init();
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(KaijuMod.GODZILLA.get(), GodzillaRenderer::new);
        e.registerEntityRenderer(KaijuMod.GHIDORAH.get(), GhidorahRenderer::new);
        e.registerEntityRenderer(KaijuMod.SEVERED_HEAD.get(), SeveredHeadRenderer::new);
    }

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END) ClientProxy.shake *= 0.86f;
    }

    private static void camera(ViewportEvent.ComputeCameraAngles e) {
        float s = ClientProxy.shake;
        if (s < 0.01f || Minecraft.getInstance().isPaused()) return;
        double t = (Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime()) + e.getPartialTick();
        e.setPitch(e.getPitch() + (float) (Math.sin(t * 2.7) * s * 1.6));
        e.setYaw(e.getYaw() + (float) (Math.sin(t * 2.1 + 1) * s * 1.2));
        e.setRoll(e.getRoll() + (float) (Math.sin(t * 3.3 + 2) * s * 1.0));
    }
}
