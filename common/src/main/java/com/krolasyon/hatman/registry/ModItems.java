package com.krolasyon.hatman.registry;

import com.krolasyon.hatman.HatmanMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, HatmanMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HatmanMod.MODID);
    /** story items by short id */
    public static final Map<String, RegistryObject<Item>> STORY = new LinkedHashMap<>();
    public static final String[] STORY_IDS = {
            "camcorder", "battery", "cellar_key", "fuel_can", "workshop_key", "ledger", "stair_key",
            "recorder_tape", "attic_key", "badge", "lamp_oil", "lighter", "original_face", "cold_key"
    };

    static {
        for (String id : STORY_IDS) {
            int stack = id.equals("battery") ? 16 : 1;
            STORY.put(id, ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(stack))));
        }
        for (RegistryObject<Block> b : ModBlocks.ALL) {
            // the camcorder on Ruth's desk is a prop; the item of the same name is the real thing
            if (STORY.containsKey(b.getId().getPath())) continue;
            ITEMS.register(b.getId().getPath(), () -> new BlockItem(b.get(), new Item.Properties()));
        }
    }

    public static final RegistryObject<Item> HATMAN_EGG = ITEMS.register("hatman_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.HATMAN, 0x101014, 0x601012, new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.hatman.main"))
            .icon(() -> new ItemStack(STORY.get("camcorder").get()))
            .displayItems((params, out) -> {
                for (RegistryObject<Item> i : STORY.values()) out.accept(i.get());
                out.accept(HATMAN_EGG.get());
                for (RegistryObject<Block> b : ModBlocks.ALL) if (!STORY.containsKey(b.getId().getPath())) out.accept(b.get());
            }).build());

    public static Item story(String id) {
        return STORY.get(id).get();
    }

    private ModItems() {}
}
