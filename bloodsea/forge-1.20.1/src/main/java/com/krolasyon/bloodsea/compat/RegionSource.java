package com.krolasyon.bloodsea.compat;

import com.krolasyon.bloodsea.world.Regions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.stream.Stream;

/** One biome per region of the Blood Sea - for the sky, the fog and the debug screen. */
public class RegionSource extends BiomeSource {
    public static final Codec<RegionSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Biome.CODEC.fieldOf("drift").forGetter(s -> s.drift),
            Biome.CODEC.fieldOf("bones").forGetter(s -> s.bones),
            Biome.CODEC.fieldOf("leviathan").forGetter(s -> s.leviathan),
            Biome.CODEC.fieldOf("edge").forGetter(s -> s.edge),
            Biome.CODEC.fieldOf("mirage").forGetter(s -> s.mirage)).apply(i, RegionSource::new));

    private final Holder<Biome> drift, bones, leviathan, edge, mirage;

    public RegionSource(Holder<Biome> drift, Holder<Biome> bones, Holder<Biome> leviathan, Holder<Biome> edge, Holder<Biome> mirage) {
        this.drift = drift;
        this.bones = bones;
        this.leviathan = leviathan;
        this.edge = edge;
        this.mirage = mirage;
    }

    @Override protected Codec<? extends BiomeSource> codec() { return CODEC; }

    @Override protected Stream<Holder<Biome>> collectPossibleBiomes() { return Stream.of(drift, bones, leviathan, edge, mirage); }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler s) {
        return switch (Regions.at(qx * 4 + 2, qz * 4 + 2)) {
            case BONES -> bones;
            case LEVIATHAN -> leviathan;
            case EDGE -> edge;
            case MIRAGE -> mirage;
            default -> drift;
        };
    }
}
