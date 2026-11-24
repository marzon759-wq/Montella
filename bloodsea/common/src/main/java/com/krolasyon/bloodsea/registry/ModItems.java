package com.krolasyon.bloodsea.registry;

import com.krolasyon.bloodsea.BloodSea;
import com.krolasyon.bloodsea.compat.Compat;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BloodSea.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BloodSea.MODID);

    public static final RegistryObject<Item> BLOOD_BUCKET = ITEMS.register("blood_bucket",
            () -> new BucketItem(ModFluids.BLOOD, new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    static {
        for (RegistryObject<Block> b : ModBlocks.WITH_ITEM) ITEMS.register(b.getId().getPath(), () -> new BlockItem(b.get(), new Item.Properties()));
    }

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.bloodsea.main"))
            .icon(() -> new ItemStack(ModBlocks.SKULL_PILE.get()))
            .displayItems((params, out) -> {
                out.accept(BLOOD_BUCKET.get());
                for (RegistryObject<Block> b : ModBlocks.WITH_ITEM) out.accept(b.get());
            }).build());

    private ModItems() {}
}
