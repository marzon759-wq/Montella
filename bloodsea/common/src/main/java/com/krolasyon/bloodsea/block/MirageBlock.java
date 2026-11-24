package com.krolasyon.bloodsea.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Looks like land. It is not: there is nothing to stand on. Under the Pale Sun the lie shows - the block shimmers and
 * sheds light like heat over a road. Under the Blood Moon it looks as real as anything else.
 */
public class MirageBlock extends Block {
    public MirageBlock(Properties p) { super(p); }

    @Override
    public void animateTick(BlockState s, Level lvl, BlockPos pos, RandomSource r) {
        if (!com.krolasyon.bloodsea.world.SeaTime.isDay(lvl) || r.nextInt(6) != 0) return;
        if (!lvl.getBlockState(pos.above()).isAir()) return;
        lvl.addParticle(ParticleTypes.END_ROD, pos.getX() + r.nextDouble(), pos.getY() + 1.05, pos.getZ() + r.nextDouble(), 0, 0.02, 0);
    }
}
