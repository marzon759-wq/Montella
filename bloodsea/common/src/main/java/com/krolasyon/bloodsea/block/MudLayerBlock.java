package com.krolasyon.bloodsea.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A thin skin of mud (1 to 8 eighths of a block) that smooths the steps of the bone islands. */
public class MudLayerBlock extends Block {
    public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
    private static final VoxelShape[] SHAPES = new VoxelShape[9];

    static {
        for (int i = 1; i <= 8; i++) SHAPES[i] = Block.box(0, 0, 0, 16, i * 2, 16);
    }

    public MudLayerBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LAYERS, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LAYERS); }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPES[s.getValue(LAYERS)]; }

    @Override
    public boolean useShapeForLightOcclusion(BlockState s) { return true; }
}
