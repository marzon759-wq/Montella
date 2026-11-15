package com.krolasyon.valdoria.world;

import com.krolasyon.valdoria.registry.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;

public final class ValdoriaSurfaceRules {
    private ValdoriaSurfaceRules() {}

    /** Duskgrass over dirt, with patches of mossy violetstone poking through the forest floor. */
    public static SurfaceRules.RuleSource overworld() {
        SurfaceRules.RuleSource grass = SurfaceRules.state(ModBlocks.DUSKGRASS_BLOCK.get().defaultBlockState());
        SurfaceRules.RuleSource dirt = SurfaceRules.state(Blocks.DIRT.defaultBlockState());
        SurfaceRules.RuleSource violet = SurfaceRules.state(ModBlocks.VIOLETSTONE.get().defaultBlockState());
        SurfaceRules.RuleSource moss = SurfaceRules.state(Blocks.MOSS_BLOCK.defaultBlockState());
        SurfaceRules.ConditionSource dry = SurfaceRules.waterBlockCheck(-1, 0);
        return SurfaceRules.ifTrue(SurfaceRules.isBiome(ValdoriaBiomes.GLIMMERVEIL_FOREST),
                SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.ifTrue(dry, SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, 1.75, Double.MAX_VALUE), violet),
                                SurfaceRules.ifTrue(SurfaceRules.noiseCondition(Noises.SURFACE, -0.1, 0.1), moss),
                                grass))),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, dirt))));
    }
}
