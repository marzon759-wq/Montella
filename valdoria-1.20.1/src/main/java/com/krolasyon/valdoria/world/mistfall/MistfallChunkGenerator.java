package com.krolasyon.valdoria.world.mistfall;

import com.krolasyon.valdoria.ValdoriaMod;
import com.krolasyon.valdoria.registry.ModBlocks;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.material.Fluids;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Builds the Mistfall Vale column by column from {@link MistfallTerrain}; trees and flowers come from the biome. */
public class MistfallChunkGenerator extends ChunkGenerator {
    public static final Codec<MistfallChunkGenerator> CODEC = RecordCodecBuilder.create(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
    ).apply(i, MistfallChunkGenerator::new));

    private volatile MistfallTerrain terrain;
    private volatile long terrainSeed;

    public MistfallChunkGenerator(BiomeSource biomes) {
        super(biomes);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    public MistfallTerrain terrain(RandomState rs) {
        long seed = rs.getOrCreateRandomFactory(ValdoriaMod.id("mistfall")).at(0, 0, 0).nextLong();
        MistfallTerrain t = terrain;
        if (t == null || terrainSeed != seed) {
            synchronized (this) {
                if (terrain == null || terrainSeed != seed) {
                    terrain = new MistfallTerrain(seed);
                    terrainSeed = seed;
                }
                t = terrain;
            }
        }
        return t;
    }

    /** The column from bottom to top: bedrock, banded rock, soil, grass, water, falling water. */
    private BlockState[] column(MistfallTerrain t, int x, int z) {
        int top = t.solidTop(x, z);
        int water = t.waterTop(x, z);
        int fall = t.fallTop(x, z);
        int height = Math.max(top, Math.max(water, fall)) + 1;
        BlockState[] col = new BlockState[Math.max(height, 1)];
        boolean steep = t.isSteep(x, z);
        boolean underwater = water >= top + 1;
        BlockState rock0 = ModBlocks.MISTROCK.get().defaultBlockState();
        BlockState rock1 = ModBlocks.BANDED_MISTROCK.get().defaultBlockState();
        BlockState rock2 = ModBlocks.DEEP_MISTROCK.get().defaultBlockState();
        for (int y = 0; y <= top && y < col.length; y++) {
            BlockState s;
            if (y <= 1 || (y <= 3 && MistfallTerrain.hash(x, z, y) < 0.5)) s = Blocks.BEDROCK.defaultBlockState();
            else {
                int b = t.band(x, y, z);
                s = b == 1 ? rock1 : b == 3 ? rock2 : rock0;
            }
            int depth = top - y;
            if (!steep && depth < 4) {
                if (underwater) s = depth == 0 ? (MistfallTerrain.hash(x, z, 7) < 0.6 ? Blocks.SAND.defaultBlockState() : Blocks.GRAVEL.defaultBlockState()) : s;
                else s = depth == 0 ? Blocks.GRASS_BLOCK.defaultBlockState() : Blocks.DIRT.defaultBlockState();
            } else if (steep && depth == 0 && !underwater && MistfallTerrain.hash(x, z, 3) < 0.25) {
                s = Blocks.MOSS_BLOCK.defaultBlockState();
            }
            col[y] = s;
        }
        BlockState waterState = Blocks.WATER.defaultBlockState();
        for (int y = top + 1; y <= water && y < col.length; y++) col[y] = waterState;
        // boulders poking out of rivers and pools
        if (water > top && MistfallTerrain.hash(x, z, 11) < 0.035) {
            for (int y = top + 1; y <= water + 1 && y < col.length; y++) col[y] = rock2;
        }
        if (fall > 0) {
            BlockState falling = Fluids.FLOWING_WATER.getFlowing(8, true).createLegacyBlock();
            int from = Math.max(top, water) + 1;
            for (int y = from; y <= fall && y < col.length; y++) col[y] = falling;
        }
        for (int y = 0; y < col.length; y++) if (col[y] == null) col[y] = Blocks.AIR.defaultBlockState();
        return col;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor exec, Blender blender, RandomState rs, StructureManager sm, ChunkAccess chunk) {
        MistfallTerrain t = terrain(rs);
        ChunkPos cp = chunk.getPos();
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = cp.getMinBlockX() + lx, z = cp.getMinBlockZ() + lz;
                BlockState[] col = column(t, x, z);
                for (int y = 0; y < col.length && y < chunk.getMaxBuildHeight(); y++) {
                    BlockState s = col[y];
                    if (s.isAir()) continue;
                    LevelChunkSection sec = chunk.getSection(chunk.getSectionIndex(y));
                    sec.setBlockState(lx, y & 15, lz, s, false);
                    ocean.update(lx, y, lz, s);
                    surface.update(lx, y, lz, s);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState rs, BiomeManager bm, StructureManager sm, ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager sm, RandomState rs, ChunkAccess chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        ChunkPos cp = region.getCenter();
        Holder<Biome> biome = region.getBiome(cp.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(net.minecraft.world.level.levelgen.RandomSupport.generateUniqueSeed()));
        random.setDecorationSeed(region.getSeed(), cp.getMinBlockX(), cp.getMinBlockZ());
        NaturalSpawner.spawnMobsForChunkGeneration(region, biome, cp, random);
    }

    @Override
    public int getGenDepth() {
        return MistfallTerrain.HEIGHT;
    }

    @Override
    public int getSeaLevel() {
        return MistfallTerrain.FLOOR_WATER;
    }

    @Override
    public int getMinY() {
        return MistfallTerrain.MIN_Y;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState rs) {
        MistfallTerrain t = terrain(rs);
        int top = t.solidTop(x, z);
        if (type == Heightmap.Types.OCEAN_FLOOR || type == Heightmap.Types.OCEAN_FLOOR_WG) return top + 1;
        return Math.max(top, Math.max(t.waterTop(x, z), t.fallTop(x, z))) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState rs) {
        BlockState[] col = column(terrain(rs), x, z);
        BlockState[] full = new BlockState[level.getHeight()];
        for (int i = 0; i < full.length; i++) {
            int y = level.getMinBuildHeight() + i;
            full[i] = y >= 0 && y < col.length ? col[y] : Blocks.AIR.defaultBlockState();
        }
        return new NoiseColumn(level.getMinBuildHeight(), full);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState rs, BlockPos pos) {
        MistfallTerrain t = terrain(rs);
        info.add("Mistfall: canyon=" + String.format("%.2f", t.canyonFactor(pos.getX(), pos.getZ())) + " river=" + t.isRiver(pos.getX(), pos.getZ()));
    }
}
