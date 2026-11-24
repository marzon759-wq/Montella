package com.krolasyon.bloodsea.compat;

import com.krolasyon.bloodsea.world.Regions;
import com.krolasyon.bloodsea.world.SeaShape;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** The Blood Sea's chunk generator: all the work is in SeaShape. */
public class SeaGenerator extends ChunkGenerator {
    public static final MapCodec<SeaGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)).apply(i, SeaGenerator::new));

    public SeaGenerator(BiomeSource source) { super(source); }

    @Override protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }

    @Override public void applyCarvers(WorldGenRegion r, long seed, RandomState rs, BiomeManager bm, StructureManager sm, ChunkAccess c, GenerationStep.Carving step) {}

    @Override public void buildSurface(WorldGenRegion r, StructureManager sm, RandomState rs, ChunkAccess c) {}

    @Override public void spawnOriginalMobs(WorldGenRegion r) {}

    @Override public int getGenDepth() { return SeaShape.HEIGHT; }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender b, RandomState rs, StructureManager sm, ChunkAccess c) {
        SeaShape.fillChunk(c);
        return CompletableFuture.completedFuture(c);
    }

    @Override public int getSeaLevel() { return Regions.SEA + 1; }

    @Override public int getMinY() { return Regions.MIN_Y; }

    @Override public int getBaseHeight(int x, int z, Heightmap.Types t, LevelHeightAccessor h, RandomState rs) { return SeaShape.top(x, z); }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor h, RandomState rs) {
        BlockState[] col = new BlockState[SeaShape.HEIGHT];
        SeaShape.column(x, z, col);
        for (int i = 0; i < col.length; i++) if (col[i] == null) col[i] = Blocks.AIR.defaultBlockState();
        return new NoiseColumn(Regions.MIN_Y, col);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState rs, BlockPos pos) {
        info.add("Blood Sea: " + Regions.at(pos.getX(), pos.getZ()) + " r=" + (int) Regions.dist(pos.getX(), pos.getZ()));
    }
}
