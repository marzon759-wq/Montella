package com.krolasyon.valdoria.block;

import com.krolasyon.valdoria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Teal undergrowth of the Glimmerveil Forest. */
public class VeilPlantBlock extends BushBlock {
    private static final VoxelShape SHAPE = BushBlock.box(2, 0, 2, 14, 13, 14);

    public VeilPlantBlock(Properties props) {
        super(props);
    }

    @Override
    protected boolean mayPlaceOn(BlockState ground, BlockGetter level, BlockPos pos) {
        return ground.is(BlockTags.DIRT) || ground.is(ModBlocks.DUSKGRASS_BLOCK.get());
    }

    /** Veil ferns slowly creep back into grazed ground, so herds never strip the forest bare. */
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.is(ModBlocks.VEIL_FERN.get());
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(6) != 0) return;
        int nearby = 0;
        for (BlockPos q : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2))) {
            if (level.getBlockState(q).is(this)) nearby++;
        }
        if (nearby > 4) return;
        BlockPos target = pos.offset(rand.nextInt(5) - 2, rand.nextInt(3) - 1, rand.nextInt(5) - 2);
        if (level.isEmptyBlock(target) && state.canSurvive(level, target) && level.getRawBrightness(target, 0) < 13) {
            level.setBlockAndUpdate(target, state);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }
}
