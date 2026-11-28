package com.krolasyon.beyblade.compat;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Item base: version neutral tooltip and use duration hooks (Forge 1.21.1). */
public class BeyItem extends Item {
   public BeyItem(Item.Properties p) {
      super(p);
   }

   protected void tooltip(ItemStack stack, List<Component> tip, TooltipFlag flag) {
   }

   protected int useDuration(ItemStack stack) {
      return 0;
   }

   @Override
   public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
      this.tooltip(stack, tip, flag);
   }

   @Override
   public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity user) {
      return this.useDuration(stack);
   }
}
