package com.krolasyon.valdoria.registry;

import com.krolasyon.valdoria.ValdoriaMod;
import net.minecraft.world.item.BlockItem;
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
            RegistryObject<Item> item = name.equals("tall_veil_fern")
                    ? ITEMS.register(name, () -> new DoubleHighBlockItem(block.get(), new Item.Properties()))
                    : ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
            TAB_ORDER.add(item);
        }
    }
}
