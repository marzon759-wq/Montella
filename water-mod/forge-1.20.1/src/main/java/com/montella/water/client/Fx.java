package com.montella.water.client;

import com.montella.water.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;

final class Fx {
    private Fx() {}
    static final RandomSource R = RandomSource.create();

    static double density() { return Config.PARTICLE_DENSITY.get(); }

    /** spawns with probability-rounded counts, so fractional amounts per tick still work */
    static int count(double expected) {
        int n = (int) expected;
        if (R.nextDouble() < expected - n) n++;
        return n;
    }

    static void add(ParticleOptions type, double x, double y, double z, double vx, double vy, double vz) {
        Minecraft.getInstance().particleEngine.createParticle(type, x, y, z, vx, vy, vz);
    }

    static double sym(double a) { return (R.nextDouble() * 2 - 1) * a; }
}
