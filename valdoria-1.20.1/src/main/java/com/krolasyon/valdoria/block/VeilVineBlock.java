package com.krolasyon.valdoria.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Dark hanging vines that drip from veilwood branches. They hang from logs, leaves, solid blocks or more vines,
 * can be climbed, and some segments carry glowing cyan buds ({@link #GLOW}).
 */
public class VeilVineBlock extends Block {
    public static final BooleanProperty END = BooleanProperty.create("end");
    public static final BooleanProperty GLOW = BooleanProperty.create("glow");
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 16, 12);

    public VeilVineBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(END, true).setValue(GLOW, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(END, GLOW);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    public static boolean canHangFrom(BlockState above, LevelReader level, BlockPos abovePos, Block self) {
        return above.is(self) || above.is(BlockTags.LEAVES) || above.is(BlockTags.LOGS)
                || above.isFaceSturdy(level, abovePos, Direction.DOWN);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canHangFrom(level.getBlockState(pos.above()), level, pos.above(), this);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState below = ctx.getLevel().getBlockState(ctx.getClickedPos().below());
        return defaultBlockState().setValue(END, !below.is(this));
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        if (dir == Direction.UP && !canSurvive(state, level, pos)) return Blocks.AIR.defaultBlockState();
        if (dir == Direction.DOWN) return state.setValue(END, !other.is(this));
        return state;
    }

    @Override
    public boolean isLadder(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) {
        return true;
    }
}
