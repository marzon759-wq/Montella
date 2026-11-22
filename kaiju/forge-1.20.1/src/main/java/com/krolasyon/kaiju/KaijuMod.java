package com.krolasyon.kaiju;

import com.krolasyon.kaiju.entity.KaijuEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(KaijuMod.MODID)
public final class KaijuMod {
    public static final String MODID = "kaiju";
    public static final Logger LOG = LoggerFactory.getLogger("Kaiju");

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    /** model is ~7.3 blocks tall at scale 1; the renderer draws it at {@link KaijuEntity#SCALE}. */
    public static final RegistryObject<EntityType<KaijuEntity>> KAIJU = ENTITIES.register("kaiju",
            () -> EntityType.Builder.of(KaijuEntity::new, MobCategory.CREATURE)
                    .sized(6.0F, 17.0F).clientTrackingRange(16).fireImmune().build("kaiju"));

    public static final RegistryObject<Item> KAIJU_EGG = ITEMS.register("kaiju_spawn_egg",
            () -> new ForgeSpawnEggItem(KAIJU, 0x2b2a28, 0x5fd8ff, new Item.Properties()));

    public KaijuMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::tabs);
    }

    private void attributes(EntityAttributeCreationEvent e) {
        e.put(KAIJU.get(), KaijuEntity.createAttributes().build());
    }

    private void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) e.accept(KAIJU_EGG);
    }
}
