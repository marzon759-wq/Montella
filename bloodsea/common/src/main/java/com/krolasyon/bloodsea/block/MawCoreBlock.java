package com.krolasyon.bloodsea.block;

import com.krolasyon.bloodsea.game.Endings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The throat of the Leviathan: a black whirl in the middle of its jaws. Sail into it and it takes you. */
public class MawCoreBlock extends Block {
    public MawCoreBlock(Properties p) { super(p); }

    @Override
    public void entityInside(BlockState s, Level lvl, BlockPos pos, Entity e) {
        if (lvl.isClientSide) return;
        Entity who = e.getControllingPassenger() != null ? e.getControllingPassenger() : e;
        if (who instanceof ServerPlayer sp) Endings.begin(sp, "leviathan");
    }

    @Override
    public void animateTick(BlockState s, Level lvl, BlockPos pos, RandomSource r) {
        if (r.nextInt(3) != 0) return;
        lvl.addParticle(ParticleTypes.SMOKE, pos.getX() + r.nextDouble(), pos.getY() + 0.9, pos.getZ() + r.nextDouble(), 0, 0.05, 0);
    }
}
