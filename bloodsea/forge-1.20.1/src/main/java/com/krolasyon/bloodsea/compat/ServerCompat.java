package com.krolasyon.bloodsea.compat;

import com.krolasyon.bloodsea.game.SeaEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

public final class ServerCompat {
    private ServerCompat() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent e) -> {
            if (e.phase != TickEvent.Phase.END) return;
            for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) { SeaEvents.bloodTick(p); SeaEvents.playerTick(p); }
            com.krolasyon.bloodsea.story.Story.serverTick(e.getServer());
        });
    }
}
