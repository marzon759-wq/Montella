package com.krolasyon.bloodsea.block;

import com.krolasyon.bloodsea.registry.ModFluids;
import com.krolasyon.bloodsea.world.SeaTime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A skull, a rib cage, a skeleton where it fell: shapes sculpted in tools/gen_props.py. Can lie in the blood
 * (BLOODLOGGED) so a skull can sit half sunk at the surface. Skulls open their eyes under the Blood Moon (LIT).
 */
public class BonePropBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final BooleanProperty BLOODLOGGED = BooleanProperty.create("bloodlogged");

    private final boolean litCapable;
    private final VoxelShape[] shapes = new VoxelShape[4];

    public BonePropBlock(boolean litCapable, double[] box, boolean heart) {
        super(props(litCapable, heart));
        this.litCapable = litCapable;
        for (Direction d : Direction.Plane.HORIZONTAL) shapes[d.get2DDataValue()] = box == null ? Shapes.empty() : rotate(box, d);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(BLOODLOGGED, false));
    }

    private static BlockBehaviour.Properties props(boolean lit, boolean heart) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().mapColor(heart ? MapColor.COLOR_RED : MapColor.SAND)
                .strength(heart ? -1f : 1.0f, heart ? 3600000f : 3f).sound(heart ? SoundType.SLIME_BLOCK : SoundType.BONE_BLOCK).noOcclusion()
                .lightLevel(s -> heart ? 6 : s.getValue(LIT) ? 4 : 0);
        return lit ? p.randomTicks() : p;
    }

    private static VoxelShape rotate(double[] b, Direction d) {
        double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
        return switch (d) {
            case EAST -> Block.box(16 - z1, b[1], x0, 16 - z0, b[4], x1);
            case SOUTH -> Block.box(16 - x1, b[1], 16 - z1, 16 - x0, b[4], 16 - z0);
            case WEST -> Block.box(z0, b[1], 16 - x1, z1, b[4], 16 - x0);
            default -> Block.box(x0, b[1], z0, x1, b[4], z1);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, LIT, BLOODLOGGED); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        boolean blood = c.getLevel().getFluidState(c.getClickedPos()).getType() == ModFluids.BLOOD.get();
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite()).setValue(BLOODLOGGED, blood);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        VoxelShape v = shapes[s.getValue(FACING).get2DDataValue()];
        return v.isEmpty() ? Block.box(4, 0, 4, 12, 6, 12) : v;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return shapes[s.getValue(FACING).get2DDataValue()];
    }

    @Override
    public FluidState getFluidState(BlockState s) {
        return s.getValue(BLOODLOGGED) ? ModFluids.BLOOD.get().getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) { return !s.getValue(BLOODLOGGED); }

    @Override
    public void randomTick(BlockState s, ServerLevel lvl, BlockPos pos, RandomSource r) {
        if (!litCapable) return;
        boolean night = !SeaTime.isDay(lvl);
        if (s.getValue(LIT) != night) lvl.setBlock(pos, s.setValue(LIT, night), 2);
    }
}
