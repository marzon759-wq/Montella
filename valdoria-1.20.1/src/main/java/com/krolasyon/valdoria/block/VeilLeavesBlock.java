package com.krolasyon.valdoria.block;

import com.krolasyon.valdoria.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Purple veilwood leaves. Glimmering leaves shed cyan motes now and then. */
public class VeilLeavesBlock extends LeavesBlock {
    public VeilLeavesBlock(Properties props) {
        super(props);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        super.animateTick(state, level, pos, rand);
        if (state.getLightEmission(level, pos) > 0 && rand.nextInt(12) == 0 && level.isEmptyBlock(pos.below())) {
            level.addParticle(ModParticles.GLIMMER.get(), pos.getX() + rand.nextDouble(), pos.getY() - 0.05, pos.getZ() + rand.nextDouble(), 0, -0.01, 0);
        }
    }

    @Override public boolean isFlammable(BlockState s, BlockGetter l, BlockPos p, Direction d) { return true; }
    @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction d) { return 60; }
    @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction d) { return 30; }
}
