package com.krolasyon.beyblade.world;

import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.PartType;
import com.krolasyon.beyblade.registry.ModItems;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootPool.Builder;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.RegistryObject;

@EventBusSubscriber(
   modid = "librabeyblade",
   bus = Bus.FORGE
)
public final class ModEvents {
   private static final String STARTER = "librabeyblade:starter_given";
   private static final Set<String> SPECIAL_TABLES = Set.of(
      "chests/ancient_city",
      "chests/end_city_treasure",
      "chests/bastion_treasure",
      "chests/buried_treasure",
      "chests/woodland_mansion",
      "chests/stronghold_corridor",
      "chests/desert_pyramid",
      "chests/jungle_temple",
      "chests/simple_dungeon",
      "chests/abandoned_mineshaft",
      "chests/shipwreck_treasure"
   );
   private static final Set<String> COMMON_TABLES = Set.of(
      "chests/simple_dungeon",
      "chests/abandoned_mineshaft",
      "chests/village/village_weaponsmith",
      "chests/village/village_toolsmith",
      "chests/village/village_plains_house",
      "chests/village/village_desert_house",
      "chests/village/village_taiga_house",
      "chests/village/village_savanna_house",
      "chests/village/village_snowy_house",
      "chests/pillager_outpost",
      "chests/shipwreck_supply",
      "chests/desert_pyramid",
      "chests/jungle_temple",
      "chests/stronghold_corridor"
   );

   private ModEvents() {
   }

   @SubscribeEvent
   public static void onLogin(PlayerLoggedInEvent event) {
      Player p = event.getEntity();
      if (!p.level().isClientSide()) {
         CompoundTag root = p.getPersistentData();
         CompoundTag keep = root.getCompound("PlayerPersisted");
         if (!keep.getBoolean("librabeyblade:starter_given")) {
            keep.putBoolean("librabeyblade:starter_given", true);
            root.put("PlayerPersisted", keep);
            int v = Math.floorMod(p.getUUID().hashCode(), ModItems.COMMONS.size());
            ItemStack bey = new ItemStack((ItemLike)ModItems.COMMONS.get(v).get());
            ItemStack launcher = new ItemStack((ItemLike)ModItems.STANDARD_LAUNCHER.get());
            if (!p.getInventory().add(launcher)) {
               p.drop(launcher, false);
            }

            if (!p.getInventory().add(bey)) {
               p.drop(bey, false);
            }

            com.krolasyon.beyblade.Msg.chat(p, Component.translatable("msg.librabeyblade.starter", new Object[]{bey.getHoverName()}).withStyle(ChatFormatting.GOLD));
            com.krolasyon.beyblade.Msg.chat(p, Component.translatable("msg.librabeyblade.starter2").withStyle(ChatFormatting.GRAY));
         }
      }
   }

   @SubscribeEvent
   public static void onLootLoad(LootTableLoadEvent event) {
      ResourceLocation id = event.getName();
      if (id.getNamespace().equals("minecraft")) {
         String path = id.getPath();
         if (SPECIAL_TABLES.contains(path)) {
            int empty = !path.contains("ancient_city") && !path.contains("end_city") && !path.contains("bastion") ? 95 : 28;
            Builder b = LootPool.lootPool().name("librabeyblade_legendary").setRolls(ConstantValue.exactly(1.0F));

            for (Item it : List.of(
               (Item)ModItems.LIBRA.get(),
               (Item)ModItems.LDRAGO.get(),
               (Item)ModItems.LEONE.get(),
               (Item)ModItems.EAGLE.get(),
               (Item)ModItems.PEGASUS.get(),
               (Item)ModItems.TEMPO.get(),
               (Item)ModItems.VARIARES.get(),
               (Item)ModItems.BEFFAL.get()
            )) {
               b.add(LootItem.lootTableItem(it).setWeight(1));
            }

            b.add(EmptyLootItem.emptyItem().setWeight(empty));
            event.getTable().addPool(b.build());
         }

         if (COMMON_TABLES.contains(path)) {
            Builder b = LootPool.lootPool().name("librabeyblade_common").setRolls(ConstantValue.exactly(1.0F));

            for (RegistryObject<Item> it : ModItems.COMMONS) {
               b.add(LootItem.lootTableItem((ItemLike)it.get()).setWeight(2));
            }

            b.add(LootItem.lootTableItem((ItemLike)ModItems.STANDARD_LAUNCHER.get()).setWeight(4));
            b.add(EmptyLootItem.emptyItem().setWeight(60));
            event.getTable().addPool(b.build());
            Builder pb = LootPool.lootPool().name("librabeyblade_parts").setRolls(ConstantValue.exactly(1.0F));

            for (BeyPart part : BeyPart.all()) {
               if (part.type() == PartType.TRACK || part.type() == PartType.TIP) {
                  pb.add(LootItem.lootTableItem(ModItems.part(part.id())).setWeight(part.rarity() == 0 ? 6 : (part.rarity() == 1 ? 3 : 1)));
               }
            }

            pb.add(EmptyLootItem.emptyItem().setWeight(70));
            event.getTable().addPool(pb.build());
         }

         if (SPECIAL_TABLES.contains(path)) {
            Builder wb = LootPool.lootPool().name("librabeyblade_wheels").setRolls(ConstantValue.exactly(1.0F));

            for (String pid : List.of(
               "wheel_bigbang",
               "wheel_phoenix",
               "wheel_inferno",
               "ring_bigbang",
               "ring_phoenix",
               "ring_inferno",
               "bolt_bigbang",
               "bolt_phoenix",
               "bolt_inferno",
               "track_th170",
               "track_230",
               "tip_fd",
               "tip_xf"
            )) {
               wb.add(LootItem.lootTableItem(ModItems.part(pid)).setWeight(pid.startsWith("wheel") ? 1 : 2));
            }

            wb.add(EmptyLootItem.emptyItem().setWeight(!path.contains("ancient_city") && !path.contains("end_city") ? 110 : 40));
            event.getTable().addPool(wb.build());
         }
      }
   }
}
