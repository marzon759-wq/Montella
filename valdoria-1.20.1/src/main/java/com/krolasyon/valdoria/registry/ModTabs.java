package com.krolasyon.valdoria.registry;

import com.krolasyon.valdoria.ValdoriaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModTabs {
    private ModTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ValdoriaMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.valdoria"))
            .icon(() -> new ItemStack(ModBlocks.GLIMMERING_VEILWOOD_LEAVES.get()))
            .displayItems((params, out) -> ModItems.TAB_ORDER.forEach(i -> out.accept(i.get())))
            .build());
}
