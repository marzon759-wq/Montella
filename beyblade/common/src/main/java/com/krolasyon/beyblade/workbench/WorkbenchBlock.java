package com.krolasyon.beyblade.workbench;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;

public class WorkbenchBlock extends com.krolasyon.beyblade.compat.UseBlock {
   private static final Component TITLE = Component.translatable("container.librabeyblade.workbench");

   public WorkbenchBlock(Properties props) {
      super(props);
   }

   @Override
   protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player) {
      if (!level.isClientSide()) {
         player.openMenu(new SimpleMenuProvider((id, inv, p) -> new WorkbenchMenu(id, inv, ContainerLevelAccess.create(level, pos)), TITLE));
      }

      return InteractionResult.sidedSuccess(level.isClientSide());
   }
}
