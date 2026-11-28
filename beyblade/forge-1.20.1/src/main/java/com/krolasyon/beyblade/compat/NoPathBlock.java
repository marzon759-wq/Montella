package com.krolasyon.beyblade.compat;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

/** A block mobs never path through (Forge 1.20.1). */
public class NoPathBlock extends Block {
   public NoPathBlock(Block.Properties p) {
      super(p);
   }

   @Override
   public boolean isPathfindable(BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, PathComputationType type) {
      return false;
   }
}
