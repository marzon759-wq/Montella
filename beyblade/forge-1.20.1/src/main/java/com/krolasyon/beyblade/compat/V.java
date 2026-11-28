package com.krolasyon.beyblade.compat;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class V {
   private V() {
   }

   public static ResourceLocation rl(String namespace, String path) {
      return new ResourceLocation(namespace, path);
   }

   public static ResourceLocation rl(String id) {
      return new ResourceLocation(id);
   }

   @Nullable
   public static CompoundTag tag(ItemStack stack) {
      return stack.getTag();
   }

   public static void editTag(ItemStack stack, Consumer<CompoundTag> edit) {
      edit.accept(stack.getOrCreateTag());
   }

   public static CompoundTag saveStack(Level level, ItemStack stack) {
      return stack.save(new CompoundTag());
   }

   public static ItemStack loadStack(Level level, CompoundTag tag) {
      return ItemStack.of(tag);
   }
}
