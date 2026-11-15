package com.krolasyon.valdoria.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

import java.util.function.Supplier;

/** A log that burns like wood and can be stripped with an axe. */
public class VeilLogBlock extends RotatedPillarBlock {
    private final Supplier<? extends Block> stripped;

    public VeilLogBlock(Properties props, Supplier<? extends Block> stripped) {
        super(props);
        this.stripped = stripped;
    }

    @Override
    public BlockState getToolModifiedState(BlockState state, UseOnContext ctx, ToolAction action, boolean simulate) {
        if (action == ToolActions.AXE_STRIP && stripped != null) {
            return stripped.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS));
        }
        return super.getToolModifiedState(state, ctx, action, simulate);
    }

    @Override public boolean isFlammable(BlockState s, BlockGetter l, BlockPos p, Direction d) { return true; }
    @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return 5; }
    @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return 5; }
}
