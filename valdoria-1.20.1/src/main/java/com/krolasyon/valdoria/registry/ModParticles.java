package com.krolasyon.valdoria.registry;

import com.krolasyon.valdoria.ValdoriaMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    private ModParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ValdoriaMod.MODID);

    /** Slow drifting cyan motes that fill the air of the Glimmerveil Forest. */
    public static final RegistryObject<SimpleParticleType> GLIMMER = PARTICLES.register("glimmer", () -> new SimpleParticleType(false));
}
