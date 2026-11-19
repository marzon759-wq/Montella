package com.krolasyon.mimic.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class OpenableBlock extends FacingBlock {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;

    public OpenableBlock(Properties p, double[] box, boolean collide) {
        super(p, box, collide);
        this.registerDefaultState(this.defaultBlockState().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(OPEN);
    }
}
