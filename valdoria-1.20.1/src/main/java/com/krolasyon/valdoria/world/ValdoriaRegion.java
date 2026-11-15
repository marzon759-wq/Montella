package com.krolasyon.valdoria.world;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import terrablender.api.Region;
import terrablender.api.RegionType;

import java.util.function.Consumer;

/**
 * Valdoria's overworld region. It is a copy of the vanilla layout in which the temperate forests are replaced by the
 * Glimmerveil Forest, so the forest shows up as large, continuous areas wherever those forests would be.
 */
public class ValdoriaRegion extends Region {
    public ValdoriaRegion(ResourceLocation name, int weight) {
        super(name, RegionType.OVERWORLD, weight);
    }

    @Override
    public void addBiomes(Registry<Biome> registry, Consumer<Pair<Climate.ParameterPoint, ResourceKey<Biome>>> mapper) {
        this.addModifiedVanillaOverworldBiomes(mapper, builder -> {
            builder.replaceBiome(Biomes.DARK_FOREST, ValdoriaBiomes.GLIMMERVEIL_FOREST);
            builder.replaceBiome(Biomes.FOREST, ValdoriaBiomes.GLIMMERVEIL_FOREST);
            builder.replaceBiome(Biomes.BIRCH_FOREST, ValdoriaBiomes.GLIMMERVEIL_FOREST);
            builder.replaceBiome(Biomes.OLD_GROWTH_BIRCH_FOREST, ValdoriaBiomes.GLIMMERVEIL_FOREST);
            builder.replaceBiome(Biomes.FLOWER_FOREST, ValdoriaBiomes.GLIMMERVEIL_FOREST);
        });
    }
}
