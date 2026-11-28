package com.krolasyon.beyblade.item;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.PartType;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;

public class PartItem extends Item {
   private final BeyPart part;

   public PartItem(Properties props, BeyPart part) {
      super(props);
      this.part = part;
   }

   public BeyPart part() {
      return this.part;
   }

   public static int level(ItemStack s) {
      CompoundTag t = V.tag(s);
      return t == null ? 0 : Math.max(0, Math.min(5, t.getInt("PartLevel")));
   }

   public static void setLevel(ItemStack s, int lvl) {
      V.editTag(s, t -> t.putInt("PartLevel", lvl));
   }

   public boolean isFoil(ItemStack stack) {
      return level(stack) >= 3;
   }

   public Component getName(ItemStack stack) {
      int lvl = level(stack);
      Component base = Component.translatable(this.part.nameKey());
      return (Component)(lvl > 0
         ? Component.empty().append(base).append(Component.literal(" " + "★".repeat(lvl)).withStyle(ChatFormatting.GOLD))
         : base);
   }

   public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
      appendPartLines(this.part, level(stack), tip);
      tip.add(Component.translatable("item.librabeyblade.part.hint").withStyle(ChatFormatting.DARK_GRAY));
   }

   public static void appendPartLines(BeyPart part, int lvl, List<Component> tip) {
      ChatFormatting rc = switch (part.rarity()) {
         case 1 -> ChatFormatting.AQUA;
         case 2 -> ChatFormatting.GOLD;
         case 3 -> ChatFormatting.LIGHT_PURPLE;
         default -> ChatFormatting.GRAY;
      };
      tip.add(
         Component.translatable(part.type().nameKey())
            .withStyle(ChatFormatting.YELLOW)
            .append(Component.literal("  "))
            .append(Component.translatable("rarity.librabeyblade." + part.rarity()).withStyle(rc))
      );
      float k = 1.0F + 0.07F * (float)lvl;
      tip.add(
         stat("atk", (float)part.attack() * k, ChatFormatting.RED)
            .append(Component.literal("  "))
            .append(stat("def", (float)part.defense() * k, ChatFormatting.BLUE))
            .append(Component.literal("  "))
            .append(stat("sta", (float)part.stamina() * k, ChatFormatting.GREEN))
            .append(Component.literal("  "))
            .append(stat("spd", (float)part.speed() * k, ChatFormatting.AQUA))
      );
      tip.add(Component.translatable("stat.librabeyblade.weight", new Object[]{String.format(Locale.ROOT, "%.1f", part.weight())}).withStyle(ChatFormatting.GRAY));
      if (part.type() == PartType.TRACK) {
         tip.add(
            Component.translatable("stat.librabeyblade.height", new Object[]{String.format(Locale.ROOT, "%.1f", (float)part.height() / 10.0F)})
               .withStyle(ChatFormatting.GRAY)
         );
      }

      if (part.type() == PartType.TIP) {
         tip.add(Component.translatable("tip.librabeyblade." + part.tip().name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.DARK_AQUA));
      }

      if (part.trait() != BeyPart.Trait.NONE) {
         tip.add(Component.translatable("trait.librabeyblade." + part.trait().name().toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.DARK_AQUA));
      }

      if (lvl > 0) {
         tip.add(Component.translatable("item.librabeyblade.part.level", new Object[]{lvl, 5}).withStyle(ChatFormatting.GOLD));
      }
   }

   private static MutableComponent stat(String key, float v, ChatFormatting c) {
      return Component.translatable("stat.librabeyblade." + key + ".short")
         .withStyle(c)
         .append(Component.literal(" " + Math.round(v)).withStyle(ChatFormatting.WHITE));
   }
}
