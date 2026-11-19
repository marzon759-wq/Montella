package com.krolasyon.mimic.item;

import com.krolasyon.mimic.story.Story;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** A story item: right click hands it to the director (the walkie talks, the meat is thrown, the flashlight clicks). */
public class StoryItem extends Item {
    private final String id;

    public StoryItem(String id, Properties p) {
        super(p);
        this.id = id;
    }

    public String id() { return id; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp && Story.isStoryPlayer(sp)) {
            if (Story.useItem(sp, id)) return InteractionResultHolder.success(st);
        }
        return InteractionResultHolder.pass(st);
    }
}
