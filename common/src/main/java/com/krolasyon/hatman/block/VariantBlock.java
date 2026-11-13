package com.krolasyon.hatman.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class VariantBlock extends FacingBlock {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 5);

    public VariantBlock(Properties p, double[] box, boolean collide) {
        super(p, box, collide);
        this.registerDefaultState(this.defaultBlockState().setValue(VARIANT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(VARIANT);
    }
}
