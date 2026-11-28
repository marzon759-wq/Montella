package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "librabeyblade",
   bus = Bus.FORGE,
   value = {Dist.CLIENT}
)
public final class ClientEvents {
   private ClientEvents() {
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent event) {
      if (event.phase == Phase.END) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            while (ClientSetup.RECALL_KEY.consumeClick()) {
               ModNetwork.sendCommand(0);
            }

            while (ClientSetup.SPIRIT_KEY.consumeClick()) {
               ModNetwork.sendCommand(50);
            }

            while (ClientSetup.MODE_KEY.consumeClick()) {
               ModNetwork.sendCommand(39);
            }

            while (ClientSetup.COMMAND_KEY.consumeClick()) {
               if (mc.screen == null) {
                  mc.setScreen(new CommandScreen());
               }
            }

            for (int i = 0; i < ClientSetup.ABILITY_KEYS.length; i++) {
               while (ClientSetup.ABILITY_KEYS[i].consumeClick()) {
                  ModNetwork.sendCommand(i + 1);
               }
            }
         }
      }
   }
}
