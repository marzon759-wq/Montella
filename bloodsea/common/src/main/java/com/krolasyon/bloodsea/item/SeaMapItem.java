package com.krolasyon.bloodsea.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The hand-drawn map that was tied to the mast: four places and an X. Use it to unfold it. */
public class SeaMapItem extends Item {
    /** opens the map on the client; set by the client setup */
    public static Runnable OPEN = () -> {};

    public SeaMapItem(Properties p) { super(p); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        if (level.isClientSide) OPEN.run();
        return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand), level.isClientSide);
    }
}
