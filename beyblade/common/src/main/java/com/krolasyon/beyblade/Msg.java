package com.krolasyon.beyblade;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * All of the mod's pop-up texts (action bar, chat, titles) go through here. They are switched off: battles are
 * shown by the effects alone. Set SHOW to true to bring them back.
 */
public final class Msg {
   public static final boolean SHOW = false;

   private Msg() {
   }

   public static void say(Player p, Component c, boolean actionBar) {
      if (SHOW && p != null) {
         p.displayClientMessage(c, actionBar);
      }
   }

   public static void chat(Player p, Component c) {
      if (SHOW && p != null) {
         p.sendSystemMessage(c);
      }
   }
}
