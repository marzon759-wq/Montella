package com.krolasyon.bloodsea.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Skulls heaped up from the blood. Under the Blood Moon their eyes open (LIT): the dead are watching. */
public class SkullPileBlock extends Block {
    public static final BooleanProperty LIT = BooleanProperty.create("lit");

    public SkullPileBlock(Properties p) {
        super(p.randomTicks());
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LIT); }

    @Override
    public void randomTick(BlockState s, ServerLevel lvl, BlockPos pos, RandomSource r) {
        boolean night = !com.krolasyon.bloodsea.world.SeaTime.isDay(lvl);
        if (s.getValue(LIT) != night) lvl.setBlock(pos, s.setValue(LIT, night), 2);
    }
}
