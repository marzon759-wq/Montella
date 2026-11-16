package com.krolasyon.valdoria.block;

import com.krolasyon.valdoria.ValdoriaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

/** A sapling that grows into one of Valdoria's configured tree features. */
public class FeatureSaplingBlock extends SaplingBlock {
    public FeatureSaplingBlock(Properties props, ResourceKey<ConfiguredFeature<?, ?>> tree) {
        super(new AbstractTreeGrower() {
            @Override
            protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource rand, boolean flowers) {
                return tree;
            }
        }, props);
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> key(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ValdoriaMod.id(name));
    }
}
