package com.krolasyon.bloodsea.block;

import com.krolasyon.bloodsea.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * The solid part of a giant: invisible blocks laid along its bones by the world generator, so a boat stops against a
 * rib and you can climb the leviathan's skull. Under the surface they hold blood like any other place in the sea.
 */
public class BoneColliderBlock extends Block {
    public static final BooleanProperty BLOODLOGGED = BonePropBlock.BLOODLOGGED;

    public BoneColliderBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(BLOODLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(BLOODLOGGED); }

    @Override
    public RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }

    @Override
    public FluidState getFluidState(BlockState s) {
        return s.getValue(BLOODLOGGED) ? ModFluids.BLOOD.get().getSource(false) : Fluids.EMPTY.defaultFluidState();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState s, BlockGetter l, BlockPos p) { return true; }

    @Override
    public float getShadeBrightness(BlockState s, BlockGetter l, BlockPos p) { return 1f; }
}
