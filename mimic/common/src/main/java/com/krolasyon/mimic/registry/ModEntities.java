package com.krolasyon.mimic.registry;

import com.krolasyon.mimic.MimicMod;
import com.krolasyon.mimic.entity.GuestEntity;
import com.krolasyon.mimic.entity.MimicEntity;
import com.krolasyon.mimic.entity.TruckEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MimicMod.MODID);

    /** the hitbox is a door's width and height: the model ducks and folds itself to fit, like it does in the house */
    public static final RegistryObject<EntityType<MimicEntity>> MIMIC = ENTITIES.register("mimic",
            () -> EntityType.Builder.<MimicEntity>of(MimicEntity::new, MobCategory.MISC).sized(0.8f, 1.95f).clientTrackingRange(16).updateInterval(1).build("mimic"));
    public static final RegistryObject<EntityType<GuestEntity>> LILY = ENTITIES.register("lily",
            () -> EntityType.Builder.<GuestEntity>of(GuestEntity::new, MobCategory.MISC).sized(0.6f, 1.7f).clientTrackingRange(12).build("lily"));
    public static final RegistryObject<EntityType<TruckEntity>> TRUCK = ENTITIES.register("truck",
            () -> EntityType.Builder.<TruckEntity>of(TruckEntity::new, MobCategory.MISC).sized(2.4f, 2.2f).clientTrackingRange(16).build("truck"));

    private ModEntities() {}
}
