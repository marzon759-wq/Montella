package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.compat.ClientCompat;
import com.krolasyon.bloodsea.game.SeaEvents;
import com.krolasyon.bloodsea.registry.ModBlocks;
import com.krolasyon.bloodsea.registry.ModFluids;
import com.krolasyon.bloodsea.world.Regions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientSetup {
    private ClientSetup() {}

    public static void init(IEventBus bus) {
        bus.addListener((FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModFluids.BLOOD.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.BLOOD_FLOWING.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.MIRAGE_LEAVES.get(), RenderType.cutoutMipped());
        }));
        ClientCompat.init(bus);
        com.krolasyon.bloodsea.entity.RaftEntity.INPUT = () -> {
            Minecraft mc = Minecraft.getInstance();
            return mc.player == null ? new float[]{0, 0} : new float[]{mc.player.input.forwardImpulse, mc.player.input.leftImpulse};
        };
        MinecraftForge.EVENT_BUS.addListener((ViewportEvent.ComputeCameraAngles e) -> {
            float[] s = SeaClient.sway((float) e.getPartialTick());
            if (s != null) {
                e.setPitch(e.getPitch() + s[0]);
                e.setRoll(e.getRoll() + s[1]);
            }
            float[] k = StoryHud.shake((float) e.getPartialTick());
            if (k != null) {
                e.setPitch(e.getPitch() + k[0]);
                e.setYaw(e.getYaw() + k[1]);
                e.setRoll(e.getRoll() + k[2]);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((ViewportEvent.ComputeFov e) -> {
            if (CutScreen.fov > 0 && Minecraft.getInstance().screen instanceof CutScreen) e.setFOV(CutScreen.fov);
        });
        com.krolasyon.bloodsea.item.SeaMapItem.OPEN = () -> Minecraft.getInstance().setScreen(new MapScreen());
        bus.addListener((net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(com.krolasyon.bloodsea.registry.ModEntities.GIANT.get(), GiantRenderer::new);
            e.registerEntityRenderer(com.krolasyon.bloodsea.registry.ModEntities.RAFT.get(), RaftRenderer::new);
            e.registerEntityRenderer(com.krolasyon.bloodsea.registry.ModEntities.PIP.get(), c -> new CreatureRenderer<>(c, "pip", 0.25f));
            e.registerEntityRenderer(com.krolasyon.bloodsea.registry.ModEntities.ACTOR.get(), c -> new CreatureRenderer<>(c, "elias", 0.4f));
            e.registerEntityRenderer(com.krolasyon.bloodsea.registry.ModEntities.RESCUER.get(), c -> new CreatureRenderer<>(c, "rescuer", 0.4f));
            e.registerEntityRenderer(com.krolasyon.bloodsea.registry.ModEntities.CRATE.get(), CrateRenderer::new);
            e.registerEntityRenderer(com.krolasyon.bloodsea.registry.ModEntities.WAKE.get(), c -> new CreatureRenderer<>(c, "wake", 0f));
            e.registerEntityRenderer(com.krolasyon.bloodsea.registry.ModEntities.GATHERER.get(), c -> new CreatureRenderer<>(c, "gatherer", 0f));
            e.registerEntityRenderer(com.krolasyon.bloodsea.registry.ModEntities.CALLER.get(), c -> new CreatureRenderer<>(c, "caller", 0f));
            e.registerEntityRenderer(com.krolasyon.bloodsea.registry.ModEntities.CHANGER.get(), c -> new CreatureRenderer<>(c, "changer", 0.6f));
            e.registerEntityRenderer(com.krolasyon.bloodsea.registry.ModEntities.LEVIATHAN.get(), c -> new CreatureRenderer<>(c, "leviathan", 0f));
        });
        // each region has its own air: the Mirage is a haze, the Deep is close and dark, the Edge is white and far
        MinecraftForge.EVENT_BUS.addListener((ViewportEvent.RenderFog e) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.level.dimension() != SeaEvents.SEA || e.getMode() != net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN) return;
            if (mc.player != null && mc.player.isEyeInFluidType(ModFluids.BLOOD_TYPE.get())) return;
            Regions.Region r = Regions.at(e.getCamera().getPosition().x, e.getCamera().getPosition().z);
            float far = e.getFarPlaneDistance();
            float[] nf = switch (r) {
                case MIRAGE -> new float[]{4, Math.min(far, 72)};
                case LEVIATHAN -> new float[]{8, Math.min(far, 110)};
                case EDGE -> new float[]{30, Math.min(far, 260)};
                case BONES -> new float[]{10, Math.min(far, 150)};
                default -> new float[]{16, Math.min(far, 180)};
            };
            if (!com.krolasyon.bloodsea.world.SeaTime.isDay(mc.level)) { nf[0] *= 0.7f; nf[1] *= 0.9f; }
            e.setNearPlaneDistance(nf[0]);
            e.setFarPlaneDistance(nf[1]);
            e.setCanceled(true);
        });
    }
}
