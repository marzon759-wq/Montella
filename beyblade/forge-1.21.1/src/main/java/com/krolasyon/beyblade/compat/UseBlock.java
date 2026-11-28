package com.krolasyon.beyblade.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A block that does something when right clicked with any hand (Forge 1.21.1). */
public class UseBlock extends Block {
   public UseBlock(Block.Properties p) {
      super(p);
   }

   protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player) {
      return InteractionResult.PASS;
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
      return this.onUse(state, level, pos, player);
   }
}
