package com.krolasyon.mimic.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Stateful prop: lamps (0 off, 1 on, 2 dying), TV (0 off, 1 static, 2 picture), phone, generator, music box. */
public class ModeBlock extends FacingBlock {
    public static final IntegerProperty MODE = IntegerProperty.create("mode", 0, 2);

    public ModeBlock(Properties p, double[] box, boolean collide) {
        super(p, box, collide);
        this.registerDefaultState(this.defaultBlockState().setValue(MODE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(MODE);
    }
}
