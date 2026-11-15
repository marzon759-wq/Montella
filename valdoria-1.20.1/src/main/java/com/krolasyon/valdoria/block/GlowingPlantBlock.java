package com.krolasyon.valdoria.block;

import com.krolasyon.valdoria.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shimmercaps: small glowing cyan mushrooms that release motes into the air. */
public class GlowingPlantBlock extends VeilPlantBlock {
    private static final VoxelShape SHAPE = box(3, 0, 3, 13, 8, 13);

    public GlowingPlantBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(6) == 0) {
            level.addParticle(ModParticles.GLIMMER.get(), pos.getX() + 0.2 + rand.nextDouble() * 0.6, pos.getY() + 0.5, pos.getZ() + 0.2 + rand.nextDouble() * 0.6, 0, 0.02, 0);
        }
    }
}
