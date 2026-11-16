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

/** A slender, curving sun palm with drooping fronds. */
public class SunpalmFeature extends Feature<NoneFeatureConfiguration> {
    public SunpalmFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos o = ctx.origin();
        RandomSource r = ctx.random();
        if (!level.getBlockState(o.below()).is(BlockTags.DIRT) || !level.getBlockState(o).getFluidState().isEmpty()) return false;
        BlockState log = ModBlocks.SUNPALM_LOG.get().defaultBlockState();
        BlockState frond = ModBlocks.SUNPALM_FRONDS.get().defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        int h = 5 + r.nextInt(5);
        double a = r.nextDouble() * Math.PI * 2;
        double bend = 1.5 + r.nextDouble() * 2;
        double tx = 0, tz = 0;
        for (int y = 0; y <= h; y++) {
            double t = y / (double) h;
            tx = Math.cos(a) * bend * t * t;
            tz = Math.sin(a) * bend * t * t;
            put(level, o.offset(Mth.floor(tx), y, Mth.floor(tz)), log);
        }
        BlockPos top = o.offset(Mth.floor(tx), h + 1, Mth.floor(tz));
        put(level, top, frond);
        int fronds = 6 + r.nextInt(3);
        for (int i = 0; i < fronds; i++) {
            double fa = i * Math.PI * 2 / fronds + r.nextDouble() * 0.4;
            double len = 3 + r.nextDouble() * 2;
            for (double d = 1; d <= len; d += 0.5) {
                double dy = d < 2 ? 0.3 : -(d - 2) * 0.7;
                put(level, top.offset(Mth.floor(Math.cos(fa) * d), Mth.floor(dy), Mth.floor(Math.sin(fa) * d)), frond);
            }
        }
        return true;
    }

    private void put(WorldGenLevel level, BlockPos p, BlockState s) {
        BlockState cur = level.getBlockState(p);
        if (cur.isAir() || cur.canBeReplaced() || (s.is(BlockTags.LOGS) && cur.is(BlockTags.LEAVES))) level.setBlock(p, s, Block.UPDATE_CLIENTS);
    }
}
