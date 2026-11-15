package com.krolasyon.valdoria.world;

import com.krolasyon.valdoria.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

/**
 * Crumbling violetstone ruins half-swallowed by the forest: broken walls, toppled pillars, archways and rubble.
 * Some still carry an old lantern that somehow never went out.
 */
public class VioletRuinFeature extends Feature<NoneFeatureConfiguration> {
    public VioletRuinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource rand = ctx.random();
        BlockPos o = ctx.origin();
        if (!level.getFluidState(o).isEmpty() || !level.getFluidState(o.below()).isEmpty()) return false;
        switch (rand.nextInt(4)) {
            case 0 -> wall(level, rand, o);
            case 1 -> pillars(level, rand, o);
            case 2 -> archway(level, rand, o);
            default -> { }
        }
        rubble(level, rand, o, 3 + rand.nextInt(3), 6 + rand.nextInt(10));
        return true;
    }

    private static BlockState stone(RandomSource rand) {
        int r = rand.nextInt(20);
        if (r < 9) return ModBlocks.VIOLETSTONE_BRICKS.get().defaultBlockState();
        if (r < 16) return ModBlocks.MOSSY_VIOLETSTONE_BRICKS.get().defaultBlockState();
        return ModBlocks.VIOLETSTONE.get().defaultBlockState();
    }

    private static int ground(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
    }

    private static void set(WorldGenLevel level, BlockPos p, BlockState s) {
        if (level.isOutsideBuildHeight(p)) return;
        level.setBlock(p, s, Block.UPDATE_CLIENTS);
    }

    /** A column of stone from a little below the ground up to {@code height} blocks above it. */
    private static int column(WorldGenLevel level, RandomSource rand, int x, int z, int height) {
        int g = ground(level, x, z);
        for (int y = g - 3; y < g + height; y++) set(level, new BlockPos(x, y, z), stone(rand));
        return g + height;
    }

    private void wall(WorldGenLevel level, RandomSource rand, BlockPos o) {
        Direction dir = Direction.from2DDataValue(rand.nextInt(4));
        int len = 5 + rand.nextInt(6);
        int base = 3 + rand.nextInt(4);
        for (int i = 0; i < len; i++) {
            int h = Math.max(0, base - rand.nextInt(3) - (i == 0 || i == len - 1 ? 2 : 0) - (rand.nextInt(4) == 0 ? 2 : 0));
            BlockPos c = o.relative(dir, i);
            int top = column(level, rand, c.getX(), c.getZ(), h);
            BlockPos t = new BlockPos(c.getX(), top, c.getZ());
            if (h > 0 && rand.nextInt(3) == 0) {
                set(level, t, rand.nextBoolean()
                        ? ModBlocks.VIOLETSTONE_BRICK_SLAB.get().defaultBlockState()
                        : ModBlocks.VIOLETSTONE_BRICK_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, rand.nextBoolean() ? dir : dir.getOpposite()));
            } else if (h > 2 && rand.nextInt(7) == 0) {
                lantern(level, t);
            }
        }
    }

    private void pillars(WorldGenLevel level, RandomSource rand, BlockPos o) {
        int count = 2 + rand.nextInt(3);
        Direction dir = Direction.from2DDataValue(rand.nextInt(4));
        int gap = 3 + rand.nextInt(2);
        for (int i = 0; i < count; i++) {
            BlockPos c = o.relative(dir, i * gap);
            int h = 2 + rand.nextInt(6);
            int top = column(level, rand, c.getX(), c.getZ(), h);
            BlockPos t = new BlockPos(c.getX(), top, c.getZ());
            if (rand.nextInt(3) == 0) set(level, t, ModBlocks.VIOLETSTONE_BRICK_WALL.get().defaultBlockState());
            else if (rand.nextInt(3) == 0) lantern(level, t);
        }
    }

    private void archway(WorldGenLevel level, RandomSource rand, BlockPos o) {
        Direction dir = Direction.from2DDataValue(rand.nextInt(4));
        int width = 3 + rand.nextInt(2);
        int h = 4 + rand.nextInt(3);
        BlockPos a = o, b = o.relative(dir, width + 1);
        int ta = column(level, rand, a.getX(), a.getZ(), h);
        int tb = column(level, rand, b.getX(), b.getZ(), h - (rand.nextInt(3) == 0 ? 2 : 0));
        int top = Math.max(ta, tb);
        boolean broken = rand.nextInt(3) == 0;
        for (int i = 0; i <= width + 1; i++) {
            if (broken && i > width / 2 + 1) break;
            BlockPos p = new BlockPos(o.relative(dir, i).getX(), top, o.relative(dir, i).getZ());
            if (i == 0 || i == width + 1) {
                set(level, p, stone(rand));
            } else if (i == 1 || i == width) {
                Direction face = i == 1 ? dir.getOpposite() : dir;
                set(level, p.below(), ModBlocks.VIOLETSTONE_BRICK_STAIRS.get().defaultBlockState().setValue(StairBlock.FACING, face).setValue(StairBlock.HALF, Half.TOP));
                set(level, p, stone(rand));
            } else {
                set(level, p, stone(rand));
            }
        }
        if (!broken && rand.nextBoolean()) {
            BlockPos mid = o.relative(dir, (width + 1) / 2);
            BlockPos under = new BlockPos(mid.getX(), top - 1, mid.getZ());
            if (level.isEmptyBlock(under)) set(level, under, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
    }

    private void rubble(WorldGenLevel level, RandomSource rand, BlockPos o, int radius, int count) {
        for (int i = 0; i < count; i++) {
            int x = o.getX() + rand.nextInt(radius * 2 + 1) - radius;
            int z = o.getZ() + rand.nextInt(radius * 2 + 1) - radius;
            int g = ground(level, x, z);
            BlockPos p = new BlockPos(x, g, z);
            BlockState below = level.getBlockState(p.below());
            if (!below.isSolid() || !level.getFluidState(p).isEmpty()) continue;
            BlockState s = switch (rand.nextInt(4)) {
                case 0 -> ModBlocks.VIOLETSTONE_BRICK_SLAB.get().defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
                case 1 -> ModBlocks.VIOLETSTONE.get().defaultBlockState();
                default -> stone(rand);
            };
            set(level, p.below(), stone(rand));
            if (level.getBlockState(p).canBeReplaced() || level.isEmptyBlock(p)) set(level, p, s);
        }
    }

    private static void lantern(WorldGenLevel level, BlockPos p) {
        if (level.isEmptyBlock(p) && level.getFluidState(p).is(Fluids.EMPTY)) {
            set(level, p, Blocks.LANTERN.defaultBlockState());
        }
    }
}
