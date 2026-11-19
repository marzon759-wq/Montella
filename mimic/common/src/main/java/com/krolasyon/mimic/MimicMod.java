package com.krolasyon.mimic;

import com.krolasyon.mimic.compat.MimicBase;
import com.krolasyon.mimic.entity.MimicEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(MimicMod.MODID)
public class MimicMod {
    public static final String MODID = "mimicry";
    public static final Logger LOG = LogManager.getLogger("Mimic");

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final RegistryObject<EntityType<MimicEntity>> MIMIC = ENTITIES.register("mimic",
            () -> EntityType.Builder.<MimicEntity>of(MimicEntity::new, MobCategory.MONSTER).sized(0.9f, 3.2f).clientTrackingRange(16).build("mimic"));

    public MimicMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e) -> e.put(MIMIC.get(), MimicBase.baseAttributes().build()));
        if (FMLEnvironment.dist == Dist.CLIENT) com.krolasyon.mimic.compat.ClientCompat.init(bus);
    }
}
