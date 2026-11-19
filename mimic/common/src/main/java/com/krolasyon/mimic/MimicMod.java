package com.krolasyon.mimic;

import com.krolasyon.mimic.compat.Compat;
import com.krolasyon.mimic.compat.GuestBase;
import com.krolasyon.mimic.compat.MimicBase;
import com.krolasyon.mimic.compat.ServerCompat;
import com.krolasyon.mimic.registry.ModBlocks;
import com.krolasyon.mimic.registry.ModEntities;
import com.krolasyon.mimic.registry.ModItems;
import com.krolasyon.mimic.registry.ModSounds;
import com.krolasyon.mimic.story.ServerEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MimicMod.MODID)
public class MimicMod {
    public static final String MODID = "mimicry";
    public static final Logger LOG = LogManager.getLogger("Mimic");

    public MimicMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModSounds.SOUNDS.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(ModEntities.MIMIC.get(), MimicBase.baseAttributes().build());
            e.put(ModEntities.LILY.get(), GuestBase.baseAttributes().build());
        });
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(Compat::initNetwork));
        ServerCompat.init();
        MinecraftForge.EVENT_BUS.register(ServerEvents.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.krolasyon.mimic.client.ClientEvents.init(bus);
        }
    }
}
