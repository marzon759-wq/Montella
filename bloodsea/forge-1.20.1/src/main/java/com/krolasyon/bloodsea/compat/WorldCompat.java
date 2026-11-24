package com.krolasyon.bloodsea.compat;

import com.krolasyon.bloodsea.BloodSea;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

public final class WorldCompat {
    private static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS = DeferredRegister.create(Registries.CHUNK_GENERATOR, BloodSea.MODID);
    private static final DeferredRegister<Codec<? extends BiomeSource>> SOURCES = DeferredRegister.create(Registries.BIOME_SOURCE, BloodSea.MODID);

    static {
        GENERATORS.register("blood_sea", () -> SeaGenerator.CODEC);
        SOURCES.register("regions", () -> RegionSource.CODEC);
    }

    private WorldCompat() {}

    public static void register(IEventBus bus) {
        GENERATORS.register(bus);
        SOURCES.register(bus);
    }
}
