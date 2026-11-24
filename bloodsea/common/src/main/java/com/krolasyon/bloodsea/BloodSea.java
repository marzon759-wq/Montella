package com.krolasyon.bloodsea;

import com.krolasyon.bloodsea.compat.Compat;
import com.krolasyon.bloodsea.compat.ServerCompat;
import com.krolasyon.bloodsea.compat.WorldCompat;
import com.krolasyon.bloodsea.game.SeaEvents;
import com.krolasyon.bloodsea.registry.ModBlocks;
import com.krolasyon.bloodsea.registry.ModFluids;
import com.krolasyon.bloodsea.registry.ModItems;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** BLOOD SEA: an endless ocean of blood under a white sun and a blood moon. */
@Mod(BloodSea.MODID)
public class BloodSea {
    public static final String MODID = "bloodsea";
    public static final Logger LOG = LogManager.getLogger("BloodSea");

    public BloodSea() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        com.krolasyon.bloodsea.registry.ModProps.init();
        ModFluids.FLUID_TYPES.register(bus);
        ModFluids.FLUIDS.register(bus);
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        com.krolasyon.bloodsea.registry.ModEntities.ENTITIES.register(bus);
        WorldCompat.register(bus);
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(Compat::initNetwork));
        ServerCompat.init();
        MinecraftForge.EVENT_BUS.register(SeaEvents.class);
        if (FMLEnvironment.dist == Dist.CLIENT) com.krolasyon.bloodsea.client.ClientSetup.init(bus);
    }
}
