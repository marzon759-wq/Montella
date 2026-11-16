package com.krolasyon.valdoria.world.mistfall;

import com.krolasyon.valdoria.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The skyroot: a massive, slightly twisting trunk on a flared base whose roots crawl over the ground and drape down
 * cliff faces, crowned by thick branches that end in broad, flat, layered pads of blue-teal (sometimes golden) leaves.
 */
public class SkyrootTreeFeature extends Feature<NoneFeatureConfiguration> {
    private static final int REACH = 16;

    public SkyrootTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos o = ctx.origin();
        RandomSource r = ctx.random();
        BlockState ground = level.getBlockState(o.below());
        if (!ground.is(BlockTags.DIRT)) return false;
        if (!level.getBlockState(o).getFluidState().isEmpty()) return false;

        BlockState log = ModBlocks.SKYROOT_LOG.get().defaultBlockState();
        BlockState wood = ModBlocks.SKYROOT_WOOD.get().defaultBlockState();
        boolean golden = r.nextInt(7) == 0;
        BlockState leaf = (golden ? ModBlocks.GOLDEN_SKYROOT_LEAVES : ModBlocks.SKYROOT_LEAVES).get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        BlockState leafAlt = (golden ? ModBlocks.SKYROOT_LEAVES : ModBlocks.GOLDEN_SKYROOT_LEAVES).get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);

        int height = 12 + r.nextInt(11);
        double twist = r.nextDouble() * Math.PI * 2;
        double lean = 0.6 + r.nextDouble() * 1.4;
        double topX = 0, topZ = 0;
        for (int y = -2; y <= height; y++) {
            double t = Math.max(0, y) / (double) height;
            double ox = Math.cos(twist + t * 1.5) * lean * t, oz = Math.sin(twist + t * 1.5) * lean * t;
            double radius = Mth.lerp(t, 2.4, 1.5) + (y < 3 ? (3 - y) * 0.55 : 0);
            disc(level, o, ox, y, oz, radius, log);
            topX = ox;
            topZ = oz;
        }

        // roots that crawl outward and pour over edges
        int roots = 5 + r.nextInt(4);
        double start = r.nextDouble() * Math.PI * 2;
        for (int i = 0; i < roots; i++) {
            double a = start + i * Math.PI * 2 / roots + (r.nextDouble() - 0.5) * 0.5;
            double len = 6 + r.nextDouble() * 6;
            double y = 2 + r.nextDouble() * 2;
            for (double d = 1.5; d <= len; d += 0.5) {
                double x = Math.cos(a) * d, z = Math.sin(a) * d;
                if (Math.abs(x) > REACH - 1 || Math.abs(z) > REACH - 1) break;
                BlockPos col = o.offset(Mth.floor(x), 0, Mth.floor(z));
                int groundY = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, col.getX(), col.getZ()) - o.getY();
                y = Math.max(y - 0.45, groundY - 0.4);
                if (y > groundY + 0.5) y -= 0.3;
                put(level, o, x, y, z, wood, true);
                if (d < 4) put(level, o, x, y + 1, z, wood, true);
                if (groundY < y - 3) {
                    // over an edge: hang straight down the rock face
                    for (int k = 0; k < 7 && y - k > groundY; k++) put(level, o, x, y - k, z, wood, true);
                    break;
                }
            }
        }

        // crown: thick branches ending in flat leaf pads
        pad(level, o, r, topX, height + 1, topZ, 5 + r.nextInt(3), leaf, leafAlt);
        int branches = 3 + r.nextInt(3);
        double bs = r.nextDouble() * Math.PI * 2;
        for (int i = 0; i < branches; i++) {
            double a = bs + i * Math.PI * 2 / branches + (r.nextDouble() - 0.5) * 0.6;
            double sy = height * (0.65 + r.nextDouble() * 0.3);
            double len = 4 + r.nextDouble() * 4;
            double rise = 2 + r.nextDouble() * 4;
            double ex = 0, ey = 0, ez = 0;
            for (double d = 0; d <= len; d += 0.4) {
                double f = d / len;
                ex = topX * (sy / height) + Math.cos(a) * d;
                ez = topZ * (sy / height) + Math.sin(a) * d;
                ey = sy + rise * Math.sqrt(f);
                put(level, o, ex, ey, ez, wood, true);
                if (d < len * 0.5) put(level, o, ex, ey - 1, ez, wood, true);
            }
            pad(level, o, r, ex, ey + 1, ez, 3 + r.nextInt(3), leaf, leafAlt);
        }
        return true;
    }

    /** A broad flat canopy pad: wide bottom layer, narrower ragged top, a few leaves dangling from the rim. */
    private void pad(WorldGenLevel level, BlockPos o, RandomSource r, double cx, double cy, double cz, int radius, BlockState leaf, BlockState alt) {
        for (int layer = 0; layer < 3; layer++) {
            double rr = radius - layer * (layer == 2 ? 1.6 : 1.2);
            if (rr < 1.2) break;
            int ir = (int) Math.ceil(rr);
            for (int dx = -ir; dx <= ir; dx++)
                for (int dz = -ir; dz <= ir; dz++) {
                    double n = (dx * dx + dz * dz) / (rr * rr);
                    if (n > 1 || (n > 0.75 && r.nextFloat() < 0.4)) continue;
                    BlockState s = r.nextFloat() < 0.08 ? alt : leaf;
                    put(level, o, cx + dx, cy + layer, cz + dz, s, false);
                    if (layer == 0 && n > 0.6 && r.nextFloat() < 0.12) put(level, o, cx + dx, cy - 1, cz + dz, s, false);
                }
        }
    }

    private void disc(WorldGenLevel level, BlockPos o, double cx, int y, double cz, double radius, BlockState s) {
        int ir = (int) Math.ceil(radius);
        for (int dx = -ir; dx <= ir; dx++)
            for (int dz = -ir; dz <= ir; dz++)
                if (dx * dx + dz * dz <= radius * radius) put(level, o, cx + dx, y, cz + dz, s, true);
    }

    private void put(WorldGenLevel level, BlockPos o, double x, double y, double z, BlockState s, boolean solid) {
        if (Math.abs(x) > REACH || Math.abs(z) > REACH) return;
        BlockPos p = o.offset(Mth.floor(x), Mth.floor(y), Mth.floor(z));
        if (level.isOutsideBuildHeight(p)) return;
        BlockState cur = level.getBlockState(p);
        if (solid) {
            if (!(cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.LEAVES) || cur.is(BlockTags.DIRT) && p.getY() <= o.getY())) return;
        } else if (!cur.isAir()) return;
        level.setBlock(p, s, Block.UPDATE_CLIENTS);
    }
}
