package com.krolasyon.bloodsea.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** A wall you cannot see. The Mirage is full of them: the sea looks open, and it is not. */
public class PhantomWallBlock extends Block {
    public PhantomWallBlock(Properties p) { super(p); }

    @Override
    public RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }

    @Override
    public float getShadeBrightness(BlockState s, BlockGetter l, BlockPos p) { return 1f; }

    @Override
    public boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) { return true; }
}
