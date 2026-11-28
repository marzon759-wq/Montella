package com.krolasyon.beyblade.item;

import com.krolasyon.beyblade.client.LibraItemRenderer;
import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.CommonBeybladeEntity;
import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.Loadout;
import com.krolasyon.beyblade.parts.PartType;
import com.krolasyon.beyblade.registry.ModEntities;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public class LibraItem extends com.krolasyon.beyblade.compat.BeyItem {
   private final String kind;
   private final int variant;

   public LibraItem(Properties props) {
      this(props, "libra");
   }

   public LibraItem(Properties props, String kind) {
      super(props);
      this.kind = kind;
      this.variant = kind.startsWith("common_") ? Integer.parseInt(kind.substring(7)) : -1;
   }

   public String kind() {
      return this.kind;
   }

   public int variant() {
      return this.variant;
   }

   public EntityType<? extends BeybladeEntity> spawnType() {
      if (this.variant >= 0) {
         return (EntityType<? extends BeybladeEntity>)ModEntities.COMMON.get();
      } else {
         String var1 = this.kind;

         return switch (var1) {
            case "ldrago" -> (EntityType)ModEntities.LDRAGO.get();
            case "leone" -> (EntityType)ModEntities.LEONE.get();
            case "eagle" -> (EntityType)ModEntities.EAGLE.get();
            case "pegasus" -> (EntityType)ModEntities.PEGASUS.get();
            case "tempo" -> (EntityType)ModEntities.TEMPO.get();
            case "variares" -> (EntityType)ModEntities.VARIARES.get();
            case "beffal" -> (EntityType)ModEntities.BEFFAL.get();
            default -> (EntityType)ModEntities.LIBRA.get();
         };
      }
   }

   public static int level(ItemStack s) {
      CompoundTag t = V.tag(s);
      return t != null && t.contains("BeyLevel") ? Math.max(1, t.getInt("BeyLevel")) : 1;
   }

   public boolean isFoil(ItemStack stack) {
      return level(stack) >= 10;
   }

   @Override
   protected void tooltip(ItemStack stack, List<Component> tip, TooltipFlag flag) {
      String base = this.getDescriptionId();
      int lvl = level(stack);
      CompoundTag t = V.tag(stack);
      int xp = t != null ? t.getInt("BeyXp") : 0;
      int mode = t != null && t.contains("BeyMode") ? t.getInt("BeyMode") : -1;
      if (this.variant >= 0) {
         CommonBeybladeEntity.Variant v = CommonBeybladeEntity.VARIANTS[this.variant];
         if (mode < 0) {
            mode = v.mode();
         }

         tip.add(
            Component.translatable("item.librabeyblade.common.desc", new Object[]{Component.translatable("mode.librabeyblade." + BeybladeEntity.MODE_IDS[v.mode()])})
               .withStyle(ChatFormatting.AQUA)
         );
      } else {
         String ab = this.kind;

         ChatFormatting col = switch (ab) {
            case "ldrago" -> ChatFormatting.RED;
            case "eagle" -> ChatFormatting.LIGHT_PURPLE;
            case "pegasus" -> ChatFormatting.AQUA;
            case "tempo" -> ChatFormatting.WHITE;
            case "variares" -> ChatFormatting.DARK_RED;
            case "beffal" -> ChatFormatting.LIGHT_PURPLE;
            default -> ChatFormatting.GREEN;
         };
         tip.add(Component.translatable("item.librabeyblade.legendary").withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}));
         tip.add(Component.translatable(base + ".desc").withStyle(col));
      }

      tip.add(Component.translatable("item.librabeyblade.level", new Object[]{lvl, xp, BeybladeEntity.xpNeeded(lvl)}).withStyle(ChatFormatting.YELLOW));
      if (mode >= 0) {
         tip.add(
            Component.translatable("item.librabeyblade.mode", new Object[]{Component.translatable("mode.librabeyblade." + BeybladeEntity.MODE_IDS[mode])})
               .withStyle(ChatFormatting.BLUE)
         );
      }

      Loadout parts = Loadout.of(stack);

      for (PartType pt : PartType.values()) {
         BeyPart bp = parts.get(pt);
         int pl = parts.level(pt);
         tip.add(
            Component.literal(" ▪ ")
               .withStyle(ChatFormatting.DARK_GRAY)
               .append(Component.translatable(pt.nameKey()).withStyle(ChatFormatting.GRAY))
               .append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
               .append(
                  bp == null
                     ? Component.translatable("item.librabeyblade.part.missing").withStyle(ChatFormatting.RED)
                     : Component.translatable(bp.nameKey()).withStyle(ChatFormatting.WHITE)
               )
               .append(Component.literal(pl > 0 ? " " + "★".repeat(pl) : "").withStyle(ChatFormatting.GOLD))
         );
      }

      tip.add(
         Component.translatable(
               "item.librabeyblade.parts.stats",
               new Object[]{
                  Math.round(parts.attack()),
                  Math.round(parts.defense()),
                  Math.round(parts.stamina()),
                  Math.round(parts.speed()),
                  String.format(Locale.ROOT, "%.1f", parts.weight())
               }
            )
            .withStyle(ChatFormatting.DARK_AQUA)
      );
      if (!parts.complete()) {
         tip.add(Component.translatable("msg.librabeyblade.incomplete").withStyle(ChatFormatting.RED));
      }

      if (this.variant >= 0) {
         CommonBeybladeEntity.Variant v = CommonBeybladeEntity.VARIANTS[this.variant];
         tip.add(Component.translatable("item.librabeyblade.common.desc2").withStyle(ChatFormatting.GRAY));
         tip.add(
            Component.literal(" ◆ ")
               .withStyle(ChatFormatting.GOLD)
               .append(Component.translatable("ability.librabeyblade.common.1").withStyle(ChatFormatting.YELLOW))
         );
         tip.add(
            Component.literal(" ◆ ")
               .withStyle(ChatFormatting.GOLD)
               .append(Component.translatable("ability.librabeyblade.common.2").withStyle(ChatFormatting.YELLOW))
         );
         tip.add(
            Component.literal(" ◆ ")
               .withStyle(ChatFormatting.GOLD)
               .append(
                  Component.translatable("ability.librabeyblade.common.3." + BeybladeEntity.MODE_IDS[v.mode()])
                     .withStyle(lvl >= 5 ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY)
               )
               .append(
                  lvl >= 5 ? Component.empty() : Component.translatable("item.librabeyblade.unlock", new Object[]{5}).withStyle(ChatFormatting.DARK_GRAY)
               )
         );
      } else {
         tip.add(Component.translatable(base + ".desc2").withStyle(ChatFormatting.GRAY));
         String ab = this.kind.equals("libra") ? "ability.librabeyblade." : "ability.librabeyblade." + this.kind + ".";

         for (int i = 1; i <= (this.kind.equals("leone") ? 6 : 5); i++) {
            tip.add(Component.literal(" ◆ ").withStyle(ChatFormatting.GOLD).append(Component.translatable(ab + i).withStyle(ChatFormatting.YELLOW)));
         }
      }
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(new IClientItemExtensions() {
         public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            return LibraItemRenderer.get(LibraItem.this.kind);
         }
      });
   }
}
