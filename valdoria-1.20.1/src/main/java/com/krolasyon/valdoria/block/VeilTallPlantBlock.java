package com.krolasyon.valdoria.block;

import com.krolasyon.valdoria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/** The tall, two-block teal fronds that grow between the veilwood roots. */
public class VeilTallPlantBlock extends DoublePlantBlock {
    public VeilTallPlantBlock(Properties props) {
        super(props);
    }

    @Override
    protected boolean mayPlaceOn(BlockState ground, BlockGetter level, BlockPos pos) {
        return ground.is(BlockTags.DIRT) || ground.is(ModBlocks.DUSKGRASS_BLOCK.get());
    }
}
