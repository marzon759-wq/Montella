package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(
   modid = "librabeyblade",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class MenuSetup {
   private MenuSetup() {
   }

   @SubscribeEvent
   public static void clientSetup(FMLClientSetupEvent event) {
      event.enqueueWork(() -> MenuScreens.register((MenuType)ModMenus.WORKBENCH.get(), WorkbenchScreen::new));
   }
}
