package com.krolasyon.valdoria.block;

import com.krolasyon.valdoria.registry.ModBlocks;
import com.krolasyon.valdoria.registry.ModParticles;
import com.krolasyon.valdoria.world.mistfall.MistfallPortal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The shimmering water-curtain inside a cascade stone frame. Stand in it for a few seconds to cross over. */
public class MistfallPortalBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    private static final VoxelShape X_SHAPE = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Z_SHAPE = Block.box(6, 0, 0, 10, 16, 16);

    public MistfallPortalBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AXIS);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_SHAPE : X_SHAPE;
    }

    /** Breaking any part of the frame (or the curtain) collapses the whole portal. */
    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        Direction.Axis axis = state.getValue(AXIS);
        boolean inPlane = dir.getAxis() == Direction.Axis.Y || dir.getAxis() == axis;
        if (inPlane && !other.is(this) && !other.is(ModBlocks.CASCADE_STONE.get())) return Blocks.AIR.defaultBlockState();
        return state;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player && !player.isPassenger() && !player.isVehicle()) {
            MistfallPortal.playerInside(player);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource rand) {
        if (rand.nextInt(120) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.4F, 0.8F + rand.nextFloat() * 0.3F, false);
        }
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + rand.nextDouble(), y = pos.getY() + rand.nextDouble(), z = pos.getZ() + rand.nextDouble();
            level.addParticle(rand.nextInt(3) == 0 ? ModParticles.MIST.get() : ParticleTypes.SPLASH, x, y, z, 0, -0.02, 0);
        }
        if (rand.nextInt(4) == 0) {
            level.addParticle(ModParticles.GLIMMER.get(), pos.getX() + rand.nextDouble(), pos.getY() + rand.nextDouble(), pos.getZ() + rand.nextDouble(), 0, 0.01, 0);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
