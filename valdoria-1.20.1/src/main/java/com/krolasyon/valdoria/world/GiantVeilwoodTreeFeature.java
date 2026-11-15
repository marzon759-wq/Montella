package com.krolasyon.valdoria.world;

import com.krolasyon.valdoria.block.VeilVineBlock;
import com.krolasyon.valdoria.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The giant veilwood: a tall, dark, slightly leaning trunk with buttress roots. Near the top it splits into
 * long branches that rise and then arch back down like a willow. Clumps of purple leaves sit on top of the arches,
 * dark vines (some with glowing cyan buds) drape from their undersides, and glowing lichen creeps up the bark.
 *
 * Every block stays within 14 blocks horizontally of the origin, so the tree never writes outside the 3x3 chunk
 * area a feature is allowed to touch.
 */
public class GiantVeilwoodTreeFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MAX_REACH = 14;

    public GiantVeilwoodTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource rand = ctx.random();
        BlockState ground = level.getBlockState(origin.below());
        if (!(ground.is(BlockTags.DIRT) || ground.is(ModBlocks.DUSKGRASS_BLOCK.get()))) return false;
        if (origin.getY() + 60 >= level.getMaxBuildHeight()) return false;
        return new Builder(level, origin, rand).build();
    }

    private static final class Builder {
        final WorldGenLevel level;
        final BlockPos origin;
        final RandomSource rand;
        final BlockState log;
        final BlockState wood;
        final BlockState leaves;
        final BlockState glimmerLeaves;
        final Set<BlockPos> woodBlocks = new HashSet<>();
        final List<BlockPos> vineAnchors = new ArrayList<>();

        Builder(WorldGenLevel level, BlockPos origin, RandomSource rand) {
            this.level = level;
            this.origin = origin;
            this.rand = rand;
            this.log = ModBlocks.VEILWOOD_LOG.get().defaultBlockState();
            this.wood = ModBlocks.VEILWOOD_WOOD.get().defaultBlockState();
            this.leaves = ModBlocks.VEILWOOD_LEAVES.get().defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
            this.glimmerLeaves = ModBlocks.GLIMMERING_VEILWOOD_LEAVES.get().defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
        }

        boolean build() {
            int height = 24 + rand.nextInt(16);
            // a gentle lean, strongest at the top
            double leanAngle = rand.nextDouble() * Math.PI * 2;
            double leanAmount = rand.nextDouble() * 2.5;
            double leanX = Math.cos(leanAngle) * leanAmount;
            double leanZ = Math.sin(leanAngle) * leanAmount;
            boolean thick = rand.nextInt(3) != 0;

            // trunk
            double topX = 0, topZ = 0;
            for (int y = -2; y <= height; y++) {
                double t = Math.max(0, y) / (double) height;
                double ox = leanX * t * t, oz = leanZ * t * t;
                double radius = thick ? Mth.lerp(t, 1.9, 1.1) : Mth.lerp(t, 1.4, 0.8);
                disc(ox, y, oz, radius, log);
                topX = ox;
                topZ = oz;
            }

            // buttress roots that flare out and dive into the ground
            int roots = 4 + rand.nextInt(3);
            double rootStart = rand.nextDouble() * Math.PI * 2;
            for (int i = 0; i < roots; i++) {
                double a = rootStart + i * (Math.PI * 2 / roots) + (rand.nextDouble() - 0.5) * 0.6;
                double len = 3 + rand.nextDouble() * 3;
                double rise = 3 + rand.nextDouble() * 4;
                for (double d = 0; d <= len; d += 0.35) {
                    double f = d / len;
                    double y = rise * (1 - f) * (1 - f) - f * 1.5;
                    put(Math.cos(a) * (d + 0.8), y, Math.sin(a) * (d + 0.8), wood, true);
                    if (f < 0.5) put(Math.cos(a) * (d + 0.8), y - 1, Math.sin(a) * (d + 0.8), wood, true);
                }
            }

            // the arching branches
            int branches = 5 + rand.nextInt(4);
            double branchStart = rand.nextDouble() * Math.PI * 2;
            for (int i = 0; i < branches; i++) {
                double angle = branchStart + i * (Math.PI * 2 / branches) + (rand.nextDouble() - 0.5) * 0.5;
                double startY = height * (0.55 + rand.nextDouble() * 0.4);
                double t = startY / height;
                double sx = leanX * t * t, sz = leanZ * t * t;
                double len = 7 + rand.nextDouble() * 4;
                double peak = 3 + rand.nextDouble() * 5;
                arch(sx, startY, sz, angle, len, peak, 2, true);
            }

            // crown at the very top
            blob(topX, height + 1, topZ, 3.5 + rand.nextDouble() * 1.5, 2.5, 0.12);
            for (int i = 0; i < 3; i++) {
                double a = rand.nextDouble() * Math.PI * 2;
                double r = 2 + rand.nextDouble() * 2;
                blob(topX + Math.cos(a) * r, height - 1 + rand.nextInt(3), topZ + Math.sin(a) * r, 2.5, 1.8, 0.12);
            }

            hangVines();
            lichen();
            return true;
        }

        /**
         * One arching branch: rises from the trunk to a peak at ~45% of its length, then droops down past its start
         * height, ending in a steep drooping tip. Leaves sit on top of it, vines hang from below.
         */
        void arch(double sx, double sy, double sz, double angle, double len, double peak, int thickness, boolean mayFork) {
            double dirX = Math.cos(angle), dirZ = Math.sin(angle);
            double dp = len * 0.45;
            double c2 = -peak / (dp * dp);
            double c1 = 2 * peak / dp;
            double tipDrop = 2 + rand.nextDouble() * 4;
            double total = len + tipDrop * 0.4;
            double lastLeaf = -10;
            boolean forked = false;
            for (double d = 0.5; d <= total; d += 0.25) {
                double y;
                double h;
                if (d <= len) {
                    h = d;
                    y = sy + c1 * d + c2 * d * d;
                } else {
                    double e = (d - len) / (tipDrop * 0.4);
                    h = len + (d - len);
                    double endY = sy + c1 * len + c2 * len * len;
                    y = endY - e * tipDrop;
                }
                double x = sx + dirX * h, z = sz + dirZ * h;
                if (!within(x, z, 1)) break;
                BlockPos p = put(x, y, z, wood, true);
                if (p != null && thickness > 1 && d < len * 0.35) put(x, y + 0.6, z, wood, true);
                if (p != null && d > 1.5) vineAnchors.add(p);

                if (d - lastLeaf > 2.2 && d > 2) {
                    double r = d < len ? 2.2 + rand.nextDouble() * 1.4 : 1.6;
                    blob(x, y + 1.2, z, r, r * 0.6, 0.06);
                    lastLeaf = d;
                }
                if (mayFork && !forked && d > len * 0.3 && d < len * 0.6 && rand.nextInt(10) == 0) {
                    forked = true;
                    double side = rand.nextBoolean() ? 0.7 : -0.7;
                    arch(x, y, z, angle + side, len * 0.45, peak * 0.4, 1, false);
                }
            }
        }

        /** A squashed leaf ellipsoid; ragged at the edges, its underside sprouting vines. */
        void blob(double cx, double cy, double cz, double r, double ry, double glimmerChance) {
            int ir = (int) Math.ceil(r), iry = (int) Math.ceil(ry);
            for (int dx = -ir; dx <= ir; dx++)
                for (int dz = -ir; dz <= ir; dz++)
                    for (int dy = -iry; dy <= iry; dy++) {
                        double n = (dx * dx + dz * dz) / (r * r) + (dy * dy) / (ry * ry);
                        if (n > 1 || (n > 0.6 && rand.nextFloat() < 0.35)) continue;
                        double x = cx + dx, z = cz + dz;
                        if (!within(x, z, 0)) continue;
                        BlockState s = rand.nextFloat() < glimmerChance ? glimmerLeaves : leaves;
                        BlockPos p = put(x, cy + dy, z, s, false);
                        if (p != null && dy <= 0 && rand.nextInt(4) == 0) vineAnchors.add(p);
                    }
        }

        void disc(double cx, int y, double cz, double r, BlockState s) {
            int ir = (int) Math.ceil(r);
            for (int dx = -ir; dx <= ir; dx++)
                for (int dz = -ir; dz <= ir; dz++) {
                    double ddx = dx + 0.5 - (cx - Math.floor(cx)), ddz = dz + 0.5 - (cz - Math.floor(cz));
                    if (ddx * ddx + ddz * ddz <= r * r) put(Math.floor(cx) + dx, y, Math.floor(cz) + dz, s, true);
                }
        }

        boolean within(double x, double z, int margin) {
            return Math.abs(x) <= MAX_REACH - margin && Math.abs(z) <= MAX_REACH - margin;
        }

        /** Places a block at an offset from the origin. Wood may replace leaves/plants/soil; leaves only fill air. */
        BlockPos put(double x, double y, double z, BlockState state, boolean isWood) {
            BlockPos p = origin.offset(Mth.floor(x), Mth.floor(y), Mth.floor(z));
            if (level.isOutsideBuildHeight(p)) return null;
            BlockState cur = level.getBlockState(p);
            if (isWood) {
                if (cur.is(ModBlocks.VEILWOOD_LOG.get()) || cur.is(ModBlocks.VEILWOOD_WOOD.get())) return p;
                boolean below = p.getY() < origin.getY();
                if (!(cur.isAir() || cur.canBeReplaced() || cur.is(BlockTags.LEAVES) || cur.is(BlockTags.REPLACEABLE_BY_TREES)
                        || (below && (cur.is(BlockTags.DIRT) || cur.is(ModBlocks.DUSKGRASS_BLOCK.get()) || cur.is(BlockTags.BASE_STONE_OVERWORLD)))))
                    return null;
                BlockState s = state;
                if (s.hasProperty(RotatedPillarBlock.AXIS) && s.is(ModBlocks.VEILWOOD_LOG.get())) s = s.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
                level.setBlock(p, s, Block.UPDATE_CLIENTS);
                woodBlocks.add(p.immutable());
                return p;
            } else {
                if (!(cur.isAir() || cur.is(ModBlocks.VEIL_VINES.get()))) return null;
                level.setBlock(p, state, Block.UPDATE_CLIENTS);
                return p;
            }
        }

        void hangVines() {
            Block vine = ModBlocks.VEIL_VINES.get();
            for (BlockPos anchor : vineAnchors) {
                if (rand.nextInt(3) == 0) continue;
                BlockPos start = anchor.below();
                if (!level.isEmptyBlock(start)) continue;
                int len = 2 + rand.nextInt(rand.nextInt(4) == 0 ? 16 : 8);
                List<BlockPos> run = new ArrayList<>();
                BlockPos p = start;
                for (int i = 0; i < len && level.isEmptyBlock(p) && !level.isOutsideBuildHeight(p); i++) {
                    run.add(p);
                    p = p.below();
                }
                // keep a little air under the tip so vines don't touch the ground
                while (run.size() > 1 && !level.isEmptyBlock(run.get(run.size() - 1).below(2))) run.remove(run.size() - 1);
                for (int i = 0; i < run.size(); i++) {
                    boolean end = i == run.size() - 1;
                    boolean glow = end ? rand.nextInt(3) == 0 : rand.nextInt(14) == 0;
                    level.setBlock(run.get(i), vine.defaultBlockState().setValue(VeilVineBlock.END, end).setValue(VeilVineBlock.GLOW, glow), Block.UPDATE_CLIENTS);
                }
            }
        }

        void lichen() {
            BlockState base = ModBlocks.GLIMMER_LICHEN.get().defaultBlockState();
            for (BlockPos w : woodBlocks) {
                if (rand.nextInt(9) != 0) continue;
                Direction d = Direction.from2DDataValue(rand.nextInt(4));
                if (rand.nextInt(5) == 0) d = Direction.DOWN;
                BlockPos p = w.relative(d);
                if (!level.isEmptyBlock(p)) continue;
                BlockState s = base;
                for (Direction f : Direction.values()) s = s.setValue(MultifaceBlock.getFaceProperty(f), false);
                s = s.setValue(MultifaceBlock.getFaceProperty(d.getOpposite()), true);
                level.setBlock(p, s, Block.UPDATE_CLIENTS);
            }
        }
    }
}
