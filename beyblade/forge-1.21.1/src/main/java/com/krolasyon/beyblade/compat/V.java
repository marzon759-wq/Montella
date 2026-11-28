package com.krolasyon.beyblade.compat;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Version specific glue (Forge 1.21.1): item NBT lives in the custom_data component. */
public final class V {
   private V() {
   }

   public static ResourceLocation rl(String namespace, String path) {
      return ResourceLocation.fromNamespaceAndPath(namespace, path);
   }

   public static ResourceLocation rl(String id) {
      return ResourceLocation.parse(id);
   }

   /** A copy of the stack's custom data (read only), or null. */
   @Nullable
   public static CompoundTag tag(ItemStack stack) {
      CustomData d = stack.get(DataComponents.CUSTOM_DATA);
      return d == null ? null : d.copyTag();
   }

   public static void editTag(ItemStack stack, Consumer<CompoundTag> edit) {
      CustomData.update(DataComponents.CUSTOM_DATA, stack, edit);
   }

   public static CompoundTag saveStack(Level level, ItemStack stack) {
      if (stack.isEmpty()) {
         return new CompoundTag();
      }
      Tag t = stack.save(level.registryAccess());
      return t instanceof CompoundTag c ? c : new CompoundTag();
   }

   public static ItemStack loadStack(Level level, CompoundTag tag) {
      return ItemStack.parseOptional(level.registryAccess(), tag);
   }

   public static void ignite(net.minecraft.world.entity.Entity l, int seconds) {
      l.igniteForSeconds((float)seconds);
   }

   public static void chestLoot(net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity chest, ResourceLocation table, long seed) {
      chest.setLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, table), seed);
   }
}
