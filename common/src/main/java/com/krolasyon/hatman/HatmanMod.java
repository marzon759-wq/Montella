package com.krolasyon.hatman;

import com.krolasyon.hatman.compat.Compat;
import com.krolasyon.hatman.compat.GuestBase;
import com.krolasyon.hatman.compat.HatmanBase;
import com.krolasyon.hatman.compat.ServerCompat;
import com.krolasyon.hatman.registry.ModBlocks;
import com.krolasyon.hatman.registry.ModEntities;
import com.krolasyon.hatman.registry.ModItems;
import com.krolasyon.hatman.registry.ModSounds;
import com.krolasyon.hatman.story.ServerEvents;
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

@Mod(HatmanMod.MODID)
public class HatmanMod {
    public static final String MODID = "hatman";
    public static final Logger LOG = LogManager.getLogger("Hatman");

    public HatmanMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(bus);
        ModItems.ITEMS.register(bus);
        ModItems.TABS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModSounds.SOUNDS.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(ModEntities.HATMAN.get(), HatmanBase.baseAttributes().build());
            e.put(ModEntities.GUEST.get(), GuestBase.baseAttributes().build());
        });
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(Compat::initNetwork));
        ServerCompat.init();
        MinecraftForge.EVENT_BUS.register(ServerEvents.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.krolasyon.hatman.client.ClientEvents.init(bus);
        }
    }
}
