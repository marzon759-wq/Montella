package com.krolasyon.mimic.compat;

import com.krolasyon.mimic.MimicMod;
import net.minecraft.resources.ResourceLocation;

/** Version specific glue: Forge 1.21.1 */
public final class Compat {
    public static final String MC = "1.21.1";

    private Compat() {}

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(MimicMod.MODID, path);
    }
}
