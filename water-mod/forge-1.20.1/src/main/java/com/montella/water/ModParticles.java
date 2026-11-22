package com.montella.water;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    private ModParticles() {}
    public static final DeferredRegister<ParticleType<?>> REG = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, WaterMod.ID);
    public static final RegistryObject<SimpleParticleType> SPRAY = REG.register("spray", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> MIST = REG.register("mist", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> FOAM = REG.register("foam", () -> new SimpleParticleType(true));
    public static final RegistryObject<SimpleParticleType> RIPPLE = REG.register("ripple", () -> new SimpleParticleType(true));
}
