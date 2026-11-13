package com.krolasyon.hatman.compat;

import com.krolasyon.hatman.story.Story;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

public final class ServerCompat {
    private ServerCompat() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ServerTickEvent.Post e) -> Story.serverTick(e.getServer()));
    }
}
