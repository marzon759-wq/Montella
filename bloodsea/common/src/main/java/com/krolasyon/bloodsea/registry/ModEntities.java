package com.krolasyon.bloodsea.registry;

import com.krolasyon.bloodsea.BloodSea;
import com.krolasyon.bloodsea.entity.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BloodSea.MODID);

    /** the giant dead: the leviathan, the Maw, giant rib cages and skulls. Static models, placed by the world generator. */
    public static final RegistryObject<EntityType<GiantEntity>> GIANT = ENTITIES.register("giant", () -> EntityType.Builder.<GiantEntity>of(GiantEntity::new, MobCategory.MISC)
            .sized(2f, 2f).clientTrackingRange(20).updateInterval(400).fireImmune().build("giant"));

    public static final RegistryObject<EntityType<WakeEntity>> WAKE = ENTITIES.register("wake", () -> EntityType.Builder.<WakeEntity>of(WakeEntity::new, MobCategory.MISC)
            .sized(3.0f, 1.2f).clientTrackingRange(16).updateInterval(2).fireImmune().build("wake"));
    public static final RegistryObject<EntityType<GathererEntity>> GATHERER = ENTITIES.register("gatherer", () -> EntityType.Builder.<GathererEntity>of(GathererEntity::new, MobCategory.MISC)
            .sized(2.4f, 8.0f).clientTrackingRange(16).updateInterval(2).fireImmune().build("gatherer"));
    public static final RegistryObject<EntityType<CallerEntity>> CALLER = ENTITIES.register("caller", () -> EntityType.Builder.<CallerEntity>of(CallerEntity::new, MobCategory.MISC)
            .sized(1.6f, 7.0f).clientTrackingRange(16).updateInterval(2).fireImmune().build("caller"));
    public static final RegistryObject<EntityType<ChangerEntity>> CHANGER = ENTITIES.register("changer", () -> EntityType.Builder.<ChangerEntity>of(ChangerEntity::new, MobCategory.MISC)
            .sized(1.1f, 1.4f).clientTrackingRange(10).updateInterval(2).fireImmune().build("changer"));
    public static final RegistryObject<EntityType<LeviathanEntity>> LEVIATHAN = ENTITIES.register("leviathan", () -> EntityType.Builder.<LeviathanEntity>of(LeviathanEntity::new, MobCategory.MISC)
            .sized(10f, 10f).clientTrackingRange(32).updateInterval(2).fireImmune().build("leviathan"));

    public static void attributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent e) {
        e.put(WAKE.get(), WakeEntity.attributes(200, 0.3).build());
        e.put(GATHERER.get(), GathererEntity.attributes(300, 0.25).build());
        e.put(CALLER.get(), CallerEntity.attributes(200, 0.25).build());
        e.put(CHANGER.get(), ChangerEntity.attributes(20, 0.32).build());
        e.put(LEVIATHAN.get(), LeviathanEntity.attributes(2000, 0.2).build());
    }

    private ModEntities() {}
}
