package com.krolasyon.mimic.registry;

import com.krolasyon.mimic.MimicMod;
import com.krolasyon.mimic.item.StoryItem;
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
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MimicMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MimicMod.MODID);
    /** story items by short id */
    public static final Map<String, RegistryObject<Item>> STORY = new LinkedHashMap<>();
    public static final String[] STORY_IDS = {
            "flashlight", "battery", "house_key", "walkie", "crowbar", "bolt_cutters", "gas_can", "truck_keys", "meat", "lighter", "pitchfork"
    };

    static {
        for (String id : STORY_IDS) {
            int stack = id.equals("battery") ? 16 : 1;
            STORY.put(id, ITEMS.register(id, () -> new StoryItem(id, new Item.Properties().stacksTo(stack))));
        }
        for (RegistryObject<Block> b : ModBlocks.ALL) {
            // a prop and the item of the same name: the item is the real thing
            if (STORY.containsKey(b.getId().getPath())) continue;
            ITEMS.register(b.getId().getPath(), () -> new BlockItem(b.get(), new Item.Properties()));
        }
    }

    public static final RegistryObject<Item> MIMIC_EGG = ITEMS.register("mimic_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.MIMIC, 0x050506, 0xd8d0b8, new Item.Properties()));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.mimicry.main"))
            .icon(() -> new ItemStack(STORY.get("flashlight").get()))
            .displayItems((params, out) -> {
                for (RegistryObject<Item> i : STORY.values()) out.accept(i.get());
                out.accept(MIMIC_EGG.get());
                for (RegistryObject<Block> b : ModBlocks.ALL) if (!STORY.containsKey(b.getId().getPath())) out.accept(b.get());
            }).build());

    public static Item story(String id) {
        return STORY.get(id).get();
    }

    private ModItems() {}
}
