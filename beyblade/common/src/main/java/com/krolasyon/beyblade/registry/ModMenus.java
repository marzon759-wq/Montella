package com.krolasyon.beyblade.registry;

import com.krolasyon.beyblade.workbench.WorkbenchMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
   public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "librabeyblade");
   public static final RegistryObject<MenuType<WorkbenchMenu>> WORKBENCH = MENUS.register(
      "workbench", () -> new MenuType(WorkbenchMenu::new, FeatureFlags.DEFAULT_FLAGS)
   );

   private ModMenus() {
   }
}
