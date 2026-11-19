package com.krolasyon.mimic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A horizontally rotatable decor block. The shape is given for FACING=NORTH (front toward -Z) in pixels. */
public class FacingBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final VoxelShape[] shapes = new VoxelShape[4];
    private final boolean collide;

    public FacingBlock(Properties p, double[] box, boolean collide) {
        super(p);
        this.collide = collide;
        for (Direction d : Direction.Plane.HORIZONTAL) shapes[d.get2DDataValue()] = rotate(box, d);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public static VoxelShape rotate(double[] b, Direction d) {
        double x1 = b[0], y1 = b[1], z1 = b[2], x2 = b[3], y2 = b[4], z2 = b[5];
        switch (d) {
            case SOUTH: return Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
            case WEST: return Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1);
            case EAST: return Block.box(16 - z2, y1, x1, 16 - z1, y2, x2);
            default: return Block.box(x1, y1, z1, x2, y2, z2);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return shapes[s.getValue(FACING).get2DDataValue()];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return collide ? shapes[s.getValue(FACING).get2DDataValue()] : Shapes.empty();
    }

    @Override
    public BlockState rotate(BlockState s, Rotation r) {
        return s.setValue(FACING, r.rotate(s.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState s, Mirror m) {
        return s.rotate(m.getRotation(s.getValue(FACING)));
    }
}
