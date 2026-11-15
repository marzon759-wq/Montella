package com.krolasyon.valdoria.block;

import com.krolasyon.valdoria.ValdoriaMod;
import com.krolasyon.valdoria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/** Grows into a full giant veilwood (the same feature the forest itself uses). */
public class VeilwoodSaplingBlock extends SaplingBlock {
    public static final ResourceKey<ConfiguredFeature<?, ?>> GIANT_TREE =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, ValdoriaMod.id("giant_veilwood_tree"));

    public VeilwoodSaplingBlock(Properties props) {
        super(new AbstractTreeGrower() {
            @Override
            protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource rand, boolean flowers) {
                return GIANT_TREE;
            }
        }, props);
    }

    @Override
    protected boolean mayPlaceOn(BlockState ground, BlockGetter level, BlockPos pos) {
        return ground.is(BlockTags.DIRT) || ground.is(ModBlocks.DUSKGRASS_BLOCK.get());
    }
}
