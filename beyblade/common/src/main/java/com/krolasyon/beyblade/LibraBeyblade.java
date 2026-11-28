package com.krolasyon.beyblade;

import com.krolasyon.beyblade.client.AutoTest;
import com.krolasyon.beyblade.network.ModNetwork;
import com.krolasyon.beyblade.registry.ModBlocks;
import com.krolasyon.beyblade.registry.ModEntities;
import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModMenus;
import com.krolasyon.beyblade.registry.ModSounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("librabeyblade")
public class LibraBeyblade {
   public static final String MODID = "librabeyblade";

   public LibraBeyblade() {
      IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
      ModBlocks.BLOCKS.register(bus);
      ModEntities.ENTITIES.register(bus);
      ModItems.ITEMS.register(bus);
      ModItems.TABS.register(bus);
      ModSounds.SOUNDS.register(bus);
      ModMenus.MENUS.register(bus);
      bus.addListener(this::commonSetup);
      if (Boolean.getBoolean("libra.autotest") && FMLEnvironment.dist == Dist.CLIENT) {
         AutoTest.init();
      }
      if (Boolean.getBoolean("libra.video") && FMLEnvironment.dist == Dist.CLIENT) {
         com.krolasyon.beyblade.client.VideoDirector.init();
      }
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(ModNetwork::register);
   }
}
