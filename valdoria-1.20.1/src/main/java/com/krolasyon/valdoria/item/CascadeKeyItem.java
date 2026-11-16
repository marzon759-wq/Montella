package com.krolasyon.valdoria.item;

import com.krolasyon.valdoria.registry.ModBlocks;
import com.krolasyon.valdoria.world.mistfall.MistfallPortal;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Use on the inside face of a cascade stone frame to open a Mistfall portal. */
public class CascadeKeyItem extends Item {
    public CascadeKeyItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (!level.getBlockState(ctx.getClickedPos()).is(ModBlocks.CASCADE_STONE.get())) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (MistfallPortal.tryOpen(level, ctx.getClickedPos().relative(ctx.getClickedFace()))) {
            level.playSound(null, ctx.getClickedPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.playSound(null, ctx.getClickedPos(), SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
            if (ctx.getPlayer() != null) ctx.getItemInHand().hurtAndBreak(1, ctx.getPlayer(), p -> p.broadcastBreakEvent(ctx.getHand()));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.FAIL;
    }
}
