package com.krolasyon.valdoria.registry;

import com.krolasyon.valdoria.ValdoriaMod;
import com.krolasyon.valdoria.world.GiantVeilwoodTreeFeature;
import com.krolasyon.valdoria.world.VioletRuinFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModFeatures {
    private ModFeatures() {}

    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, ValdoriaMod.MODID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> GIANT_VEILWOOD_TREE =
            FEATURES.register("giant_veilwood_tree", () -> new GiantVeilwoodTreeFeature(NoneFeatureConfiguration.CODEC));
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> VIOLET_RUIN =
            FEATURES.register("violet_ruin", () -> new VioletRuinFeature(NoneFeatureConfiguration.CODEC));
}
