package com.krolasyon.beyblade.registry;

import com.krolasyon.beyblade.entity.CommonBeybladeEntity;
import com.krolasyon.beyblade.item.LauncherItem;
import com.krolasyon.beyblade.item.LibraItem;
import com.krolasyon.beyblade.item.PartItem;
import com.krolasyon.beyblade.item.RivalLibraItem;
import com.krolasyon.beyblade.item.StadiumItem;
import com.krolasyon.beyblade.parts.BeyPart;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
   public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "librabeyblade");
   public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "librabeyblade");
   public static final RegistryObject<Item> LIBRA = ITEMS.register(
      "flame_libra", () -> new LibraItem(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant())
   );
   public static final RegistryObject<Item> LAUNCHER = ITEMS.register(
      "libra_launcher", () -> new LauncherItem(new Properties().stacksTo(1).rarity(Rarity.RARE), LIBRA, ModEntities.LIBRA)
   );
   public static final RegistryObject<Item> RIVAL = ITEMS.register(
      "rival_libra", () -> new RivalLibraItem(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.LIBRA)
   );
   public static final RegistryObject<Item> LDRAGO = ITEMS.register(
      "ldrago_destruction", () -> new LibraItem(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "ldrago")
   );
   public static final RegistryObject<Item> LDRAGO_LAUNCHER = ITEMS.register(
      "ldrago_launcher", () -> new LauncherItem(new Properties().stacksTo(1).rarity(Rarity.RARE), LDRAGO, ModEntities.LDRAGO)
   );
   public static final RegistryObject<Item> RIVAL_LDRAGO = ITEMS.register(
      "rival_ldrago", () -> new RivalLibraItem(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.LDRAGO)
   );
   public static final RegistryObject<Item> LEONE = ITEMS.register(
      "rock_leone", () -> new LibraItem(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "leone")
   );
   public static final RegistryObject<Item> LEONE_LAUNCHER = ITEMS.register(
      "leone_launcher", () -> new LauncherItem(new Properties().stacksTo(1).rarity(Rarity.RARE), LEONE, ModEntities.LEONE)
   );
   public static final RegistryObject<Item> RIVAL_LEONE = ITEMS.register(
      "rival_leone", () -> new RivalLibraItem(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.LEONE)
   );
   public static final RegistryObject<Item> EAGLE = ITEMS.register(
      "earth_eagle", () -> new LibraItem(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "eagle")
   );
   public static final RegistryObject<Item> EAGLE_LAUNCHER = ITEMS.register(
      "eagle_launcher", () -> new LauncherItem(new Properties().stacksTo(1).rarity(Rarity.RARE), EAGLE, ModEntities.EAGLE)
   );
   public static final RegistryObject<Item> RIVAL_EAGLE = ITEMS.register(
      "rival_eagle", () -> new RivalLibraItem(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.EAGLE)
   );
   public static final RegistryObject<Item> PEGASUS = ITEMS.register(
      "storm_pegasus", () -> new LibraItem(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "pegasus")
   );
   public static final RegistryObject<Item> PEGASUS_LAUNCHER = ITEMS.register(
      "pegasus_launcher", () -> new LauncherItem(new Properties().stacksTo(1).rarity(Rarity.RARE), PEGASUS, ModEntities.PEGASUS)
   );
   public static final RegistryObject<Item> RIVAL_PEGASUS = ITEMS.register(
      "rival_pegasus", () -> new RivalLibraItem(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.PEGASUS)
   );
   public static final RegistryObject<Item> TEMPO = ITEMS.register(
      "twisted_tempo", () -> new LibraItem(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "tempo")
   );
   public static final RegistryObject<Item> TEMPO_LAUNCHER = ITEMS.register(
      "tempo_launcher", () -> new LauncherItem(new Properties().stacksTo(1).rarity(Rarity.RARE), TEMPO, ModEntities.TEMPO)
   );
   public static final RegistryObject<Item> RIVAL_TEMPO = ITEMS.register(
      "rival_tempo", () -> new RivalLibraItem(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.TEMPO)
   );
   public static final RegistryObject<Item> VARIARES = ITEMS.register(
      "variares", () -> new LibraItem(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "variares")
   );
   public static final RegistryObject<Item> VARIARES_LAUNCHER = ITEMS.register(
      "variares_launcher", () -> new LauncherItem(new Properties().stacksTo(1).rarity(Rarity.RARE), VARIARES, ModEntities.VARIARES)
   );
   public static final RegistryObject<Item> RIVAL_VARIARES = ITEMS.register(
      "rival_variares", () -> new RivalLibraItem(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.VARIARES)
   );
   public static final RegistryObject<Item> BEFFAL = ITEMS.register(
      "evil_beffal", () -> new LibraItem(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), "beffal")
   );
   public static final RegistryObject<Item> BEFFAL_LAUNCHER = ITEMS.register(
      "beffal_launcher", () -> new LauncherItem(new Properties().stacksTo(1).rarity(Rarity.RARE), BEFFAL, ModEntities.BEFFAL)
   );
   public static final RegistryObject<Item> RIVAL_BEFFAL = ITEMS.register(
      "rival_beffal", () -> new RivalLibraItem(new Properties().stacksTo(16).rarity(Rarity.UNCOMMON), ModEntities.BEFFAL)
   );
   public static final List<RegistryObject<Item>> COMMONS = new ArrayList<>();
   public static final Map<String, RegistryObject<Item>> PARTS;
   public static final RegistryObject<Item> WORKBENCH;
   public static final RegistryObject<Item> STANDARD_LAUNCHER;
   public static final RegistryObject<Item> STADIUM;
   public static final RegistryObject<CreativeModeTab> TAB;

   private ModItems() {
   }

   public static Item part(String id) {
      return (Item)PARTS.get(id).get();
   }

   static {
      for (int i = 0; i < CommonBeybladeEntity.VARIANTS.length; i++) {
         int v = i;
         COMMONS.add(
            ITEMS.register(
               "common_" + CommonBeybladeEntity.VARIANTS[i].id(), () -> new LibraItem(new Properties().stacksTo(1).rarity(Rarity.COMMON), "common_" + v)
            )
         );
      }

      PARTS = new LinkedHashMap<>();

      for (BeyPart p : BeyPart.all()) {
         Rarity r = switch (p.rarity()) {
            case 1 -> Rarity.UNCOMMON;
            case 2 -> Rarity.RARE;
            case 3 -> Rarity.EPIC;
            default -> Rarity.COMMON;
         };
         PARTS.put(p.id(), ITEMS.register(p.itemId(), () -> new PartItem(new Properties().stacksTo(16).rarity(r), p)));
      }

      WORKBENCH = ITEMS.register("bey_workbench", () -> new BlockItem((Block)ModBlocks.WORKBENCH.get(), new Properties()));
      STANDARD_LAUNCHER = ITEMS.register("standard_launcher", () -> new LauncherItem(new Properties().stacksTo(1), null, null));
      STADIUM = ITEMS.register("stadium", () -> new StadiumItem(new Properties().stacksTo(1).rarity(Rarity.RARE)));
      TAB = TABS.register(
         "beyblade",
         () -> CreativeModeTab.builder()
               .title(Component.translatable("itemGroup.librabeyblade"))
               .icon(() -> new ItemStack((ItemLike)LIBRA.get()))
               .displayItems((params, output) -> {
                  output.accept((ItemLike)STANDARD_LAUNCHER.get());

                  for (RegistryObject<Item> c : COMMONS) {
                     output.accept((ItemLike)c.get());
                  }

                  output.accept((ItemLike)LIBRA.get());
                  output.accept((ItemLike)LAUNCHER.get());
                  output.accept((ItemLike)RIVAL.get());
                  output.accept((ItemLike)LDRAGO.get());
                  output.accept((ItemLike)LDRAGO_LAUNCHER.get());
                  output.accept((ItemLike)RIVAL_LDRAGO.get());
                  output.accept((ItemLike)LEONE.get());
                  output.accept((ItemLike)LEONE_LAUNCHER.get());
                  output.accept((ItemLike)RIVAL_LEONE.get());
                  output.accept((ItemLike)EAGLE.get());
                  output.accept((ItemLike)EAGLE_LAUNCHER.get());
                  output.accept((ItemLike)RIVAL_EAGLE.get());
                  output.accept((ItemLike)PEGASUS.get());
                  output.accept((ItemLike)PEGASUS_LAUNCHER.get());
                  output.accept((ItemLike)RIVAL_PEGASUS.get());
                  output.accept((ItemLike)TEMPO.get());
                  output.accept((ItemLike)TEMPO_LAUNCHER.get());
                  output.accept((ItemLike)RIVAL_TEMPO.get());
                  output.accept((ItemLike)VARIARES.get());
                  output.accept((ItemLike)VARIARES_LAUNCHER.get());
                  output.accept((ItemLike)RIVAL_VARIARES.get());
                  output.accept((ItemLike)BEFFAL.get());
                  output.accept((ItemLike)BEFFAL_LAUNCHER.get());
                  output.accept((ItemLike)RIVAL_BEFFAL.get());
                  output.accept((ItemLike)STADIUM.get());
                  output.accept((ItemLike)WORKBENCH.get());

                  for (RegistryObject<Item> px : PARTS.values()) {
                     output.accept((ItemLike)px.get());
                  }
               })
               .build()
      );
   }
}
