package com.krolasyon.beyblade.network;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class CommandHandler {
   private CommandHandler() {
   }

   public static void handle(ServerPlayer player, int cmd) {
      if (player != null) {
         BeybladeEntity bey = BeybladeEntity.findActive(player);
         if (bey == null) {
            com.krolasyon.beyblade.Msg.say(player, Component.translatable("msg.librabeyblade.none_active"), true);
         } else {
            if (cmd == 0) {
               bey.recall();
            } else if (cmd >= 20 && cmd < 34) {
               bey.setOrder(cmd - 20);
            } else if (cmd == 50) {
               bey.releaseSpirit();
            } else if (cmd == 39) {
               bey.setMode(bey.getMode() + 1);
            } else if (cmd >= 40 && cmd < 44) {
               bey.setMode(cmd - 40);
            } else if (cmd >= 1 && cmd <= 6) {
               bey.tryAbility(cmd, true);
            }
         }
      }
   }
}
