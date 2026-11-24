package com.krolasyon.bloodsea.registry;

import com.krolasyon.bloodsea.BloodSea;
import com.krolasyon.bloodsea.entity.GiantEntity;
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

    private ModEntities() {}
}
