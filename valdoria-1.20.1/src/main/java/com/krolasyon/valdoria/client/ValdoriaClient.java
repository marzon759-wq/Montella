package com.krolasyon.valdoria.client;

import com.krolasyon.valdoria.ValdoriaMod;
import com.krolasyon.valdoria.registry.ModParticles;
import com.krolasyon.valdoria.world.ValdoriaBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only visuals of the Glimmerveil Forest: the glimmer particle and the thick teal mist. */
public final class ValdoriaClient {
    private ValdoriaClient() {}

    /** 0 = normal fog, 1 = full Glimmerveil mist. Eased in and out as the player crosses the biome border. */
    private static float mist;
    private static float mistPrev;

    @Mod.EventBusSubscriber(modid = ValdoriaMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void particles(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.GLIMMER.get(), GlimmerParticle.Provider::new);
        }
    }

    @Mod.EventBusSubscriber(modid = ValdoriaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBus {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (AutoTest.enabled()) AutoTest.tick();
            Minecraft mc = Minecraft.getInstance();
            mistPrev = mist;
            if (mc.level == null || mc.player == null) {
                mist = 0;
                return;
            }
            boolean inside = mc.level.getBiome(mc.player.blockPosition()).is(ValdoriaBiomes.GLIMMERVEIL_FOREST);
            mist += ((inside ? 1f : 0f) - mist) * 0.02f;
        }

        @SubscribeEvent
        public static void fog(ViewportEvent.RenderFog event) {
            if (event.getType() != FogType.NONE) return;
            float m = Mth.lerp((float) event.getPartialTick(), mistPrev, mist);
            if (m < 0.01f) return;
            float far = event.getFarPlaneDistance();
            float mistFar = Math.min(far, 72f);
            float newFar = Mth.lerp(m, far, mistFar);
            float newNear = event.getMode() == FogRenderer.FogMode.FOG_SKY ? 0 : Mth.lerp(m, event.getNearPlaneDistance(), -8f);
            event.setFarPlaneDistance(newFar);
            event.setNearPlaneDistance(newNear);
            event.setCanceled(true);
        }
    }
}
