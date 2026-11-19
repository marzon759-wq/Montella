package com.krolasyon.mimic.compat;

import com.krolasyon.mimic.story.Story;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

public final class ServerCompat {
    private ServerCompat() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent.Post e) -> Story.serverTick(e.getServer()));
    }
}
