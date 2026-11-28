package com.krolasyon.beyblade.compat;

import com.krolasyon.beyblade.client.LaunchHud;
import com.krolasyon.beyblade.client.LibraHud;
import com.krolasyon.beyblade.client.MatchHud;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/** HUD overlays (Forge 1.21.1: drawn from the chat overlay hook, above the vanilla HUD). */
@EventBusSubscriber(modid = "librabeyblade", bus = Bus.FORGE, value = {Dist.CLIENT})
public final class HudSetup {
   private static final LibraHud LIBRA_HUD = new LibraHud();
   private static final MatchHud MATCH_HUD = new MatchHud();
   private static final LaunchHud LAUNCH_HUD = new LaunchHud();

   private HudSetup() {
   }

   @SubscribeEvent
   public static void onChat(CustomizeGuiOverlayEvent.Chat e) {
      int w = e.getWindow().getGuiScaledWidth();
      int h = e.getWindow().getGuiScaledHeight();
      LIBRA_HUD.render(e.getGuiGraphics(), e.getPartialTick(), w, h);
      MATCH_HUD.render(e.getGuiGraphics(), e.getPartialTick(), w, h);
      LAUNCH_HUD.render(e.getGuiGraphics(), e.getPartialTick(), w, h);
   }
}
