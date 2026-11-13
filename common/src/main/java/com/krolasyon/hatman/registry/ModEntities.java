package com.krolasyon.hatman.registry;

import com.krolasyon.hatman.HatmanMod;
import com.krolasyon.hatman.entity.GuestEntity;
import com.krolasyon.hatman.entity.HatmanEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HatmanMod.MODID);

    public static final RegistryObject<EntityType<HatmanEntity>> HATMAN = ENTITIES.register("hatman",
            () -> EntityType.Builder.<HatmanEntity>of(HatmanEntity::new, MobCategory.MISC).sized(0.6f, 1.9f).clientTrackingRange(12).updateInterval(1).build("hatman"));
    public static final RegistryObject<EntityType<GuestEntity>> GUEST = ENTITIES.register("guest",
            () -> EntityType.Builder.<GuestEntity>of(GuestEntity::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10).build("guest"));

    private ModEntities() {}
}
