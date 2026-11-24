package com.krolasyon.bloodsea.world;

import net.minecraft.world.level.Level;

/** The Pale Sun is up from dawn to dusk; the Blood Moon the rest of the time. The same answer on client and server. */
public final class SeaTime {
    private SeaTime() {}

    public static boolean isDay(Level lvl) {
        long t = lvl.getDayTime() % 24000L;
        return t < 12600 || t > 23400;
    }
}
