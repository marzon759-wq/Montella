package com.krolasyon.beyblade.compat;

import com.krolasyon.beyblade.client.LaunchHud;
import com.krolasyon.beyblade.client.LibraHud;
import com.krolasyon.beyblade.client.MatchHud;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "librabeyblade",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
public final class HudSetup {
   private static final LibraHud LIBRA_HUD = new LibraHud();
   private static final MatchHud MATCH_HUD = new MatchHud();
   private static final LaunchHud LAUNCH_HUD = new LaunchHud();

   private HudSetup() {
   }

   @SubscribeEvent
   public static void registerOverlays(RegisterGuiOverlaysEvent event) {
      event.registerAboveAll("libra_hud", (gui, g, partial, w, h) -> LIBRA_HUD.render(g, partial, w, h));
      event.registerAboveAll("match_hud", (gui, g, partial, w, h) -> MATCH_HUD.render(g, partial, w, h));
      event.registerAboveAll("launch_hud", (gui, g, partial, w, h) -> LAUNCH_HUD.render(g, partial, w, h));
   }
}
