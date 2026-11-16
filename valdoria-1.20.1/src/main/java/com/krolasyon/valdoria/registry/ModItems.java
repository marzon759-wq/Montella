package com.krolasyon.valdoria.registry;

import com.krolasyon.valdoria.ValdoriaMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ValdoriaMod.MODID);
    /** Everything shown in the Valdoria creative tab, in order. */
    public static final List<RegistryObject<? extends Item>> TAB_ORDER = new ArrayList<>();

    static {
        for (var block : ModBlocks.BLOCKS.getEntries()) {
            String name = block.getId().getPath();
            if (name.equals("mistfall_portal")) continue;
            RegistryObject<Item> item = name.startsWith("tall_")
                    ? ITEMS.register(name, () -> new DoubleHighBlockItem(block.get(), new Item.Properties()))
                    : ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
            TAB_ORDER.add(item);
        }
    }

    /** Opens a Mistfall portal inside a cascade stone frame. */
    public static final RegistryObject<Item> CASCADE_KEY = tab(ITEMS.register("cascade_key", () -> new com.krolasyon.valdoria.item.CascadeKeyItem(new Item.Properties().durability(32))));

    private static RegistryObject<Item> tab(RegistryObject<Item> item) {
        TAB_ORDER.add(item);
        return item;
    }

    public static final RegistryObject<Item> VEILSTAG_SPAWN_EGG = egg("veilstag", ModEntities.VEILSTAG, 0x2A1E31, 0x5FF3FF);
    public static final RegistryObject<Item> LUMEN_MOTH_SPAWN_EGG = egg("lumen_moth", ModEntities.LUMEN_MOTH, 0x9FE8E0, 0x8F52C0);
    public static final RegistryObject<Item> GLOOMLYNX_SPAWN_EGG = egg("gloomlynx", ModEntities.GLOOMLYNX, 0x3B3550, 0x7CF5FF);
    public static final RegistryObject<Item> VINESTALKER_SPAWN_EGG = egg("vinestalker", ModEntities.VINESTALKER, 0x140E19, 0x6B379B);

    private static RegistryObject<Item> egg(String name, RegistryObject<? extends EntityType<? extends Mob>> type, int bg, int fg) {
        RegistryObject<Item> item = ITEMS.register(name + "_spawn_egg", () -> new ForgeSpawnEggItem(type, bg, fg, new Item.Properties()));
        TAB_ORDER.add(item);
        return item;
    }
}
