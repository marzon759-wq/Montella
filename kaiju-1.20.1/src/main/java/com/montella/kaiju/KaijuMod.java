package com.montella.kaiju;

import com.montella.kaiju.entity.GhidorahEntity;
import com.montella.kaiju.entity.GodzillaEntity;
import com.montella.kaiju.entity.SeveredHeadEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(KaijuMod.ID)
public class KaijuMod {
    public static final String ID = "kaiju";
    public static final Logger LOG = LoggerFactory.getLogger("Kaiju");

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);

    public static final RegistryObject<EntityType<GodzillaEntity>> GODZILLA = ENTITIES.register("godzilla",
            () -> EntityType.Builder.<GodzillaEntity>of(GodzillaEntity::new, MobCategory.MONSTER).sized(10f, 23f)
                    .clientTrackingRange(24).updateInterval(1).fireImmune().build("godzilla"));
    public static final RegistryObject<EntityType<GhidorahEntity>> GHIDORAH = ENTITIES.register("king_ghidorah",
            () -> EntityType.Builder.<GhidorahEntity>of(GhidorahEntity::new, MobCategory.MONSTER).sized(9f, 25f)
                    .clientTrackingRange(24).updateInterval(1).fireImmune().build("king_ghidorah"));
    public static final RegistryObject<EntityType<SeveredHeadEntity>> SEVERED_HEAD = ENTITIES.register("severed_head",
            () -> EntityType.Builder.<SeveredHeadEntity>of(SeveredHeadEntity::new, MobCategory.MISC).sized(2.5f, 2f)
                    .clientTrackingRange(16).updateInterval(2).build("severed_head"));

    public static final RegistryObject<Item> GODZILLA_EGG = ITEMS.register("godzilla_spawn_egg",
            () -> new ForgeSpawnEggItem(GODZILLA, 0x2b2d2f, 0x4fd8ff, new Item.Properties()));
    public static final RegistryObject<Item> GHIDORAH_EGG = ITEMS.register("king_ghidorah_spawn_egg",
            () -> new ForgeSpawnEggItem(GHIDORAH, 0xc39a3c, 0xffe25a, new Item.Properties()));

    public static Proxy PROXY = new Proxy();

    public KaijuMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::attributes);
        bus.addListener(this::tabs);
        MinecraftForge.EVENT_BUS.addListener(KaijuCommand::register);
        DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> {
            PROXY = new com.montella.kaiju.client.ClientProxy();
            com.montella.kaiju.client.ClientSetup.init(bus);
        });
    }

    private void attributes(EntityAttributeCreationEvent e) {
        e.put(GODZILLA.get(), GodzillaEntity.attributes().build());
        e.put(GHIDORAH.get(), GhidorahEntity.attributes().build());
    }

    private void tabs(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) { e.accept(GODZILLA_EGG); e.accept(GHIDORAH_EGG); }
    }

    /** Side-safe hooks; the client overrides them. */
    public static class Proxy {
        public void shake(Vec3 at, float strength) {}
    }
}
