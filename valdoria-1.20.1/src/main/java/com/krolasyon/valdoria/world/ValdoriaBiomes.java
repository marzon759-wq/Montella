package com.krolasyon.valdoria.world;

import com.krolasyon.valdoria.ValdoriaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class ValdoriaBiomes {
    private ValdoriaBiomes() {}

    /** Giant arching veilwoods, purple canopies, glowing cyan motes and teal mist. */
    public static final ResourceKey<Biome> GLIMMERVEIL_FOREST = ResourceKey.create(Registries.BIOME, ValdoriaMod.id("glimmerveil_forest"));
}
