package com.krolasyon.valdoria.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** A soft cyan mote that drifts lazily, pulses and glows at full brightness even in the dark. */
public class GlimmerParticle extends TextureSheetParticle {
    private final float baseSize;
    private final float phase;

    protected GlimmerParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z);
        this.xd = vx + (random.nextDouble() - 0.5) * 0.01;
        this.yd = vy + (random.nextDouble() - 0.3) * 0.006;
        this.zd = vz + (random.nextDouble() - 0.5) * 0.01;
        this.lifetime = 80 + random.nextInt(120);
        this.baseSize = 0.05f + random.nextFloat() * 0.07f;
        this.quadSize = baseSize;
        this.phase = random.nextFloat() * 6.28f;
        this.gravity = 0;
        this.friction = 0.98f;
        this.hasPhysics = false;
        float tint = random.nextFloat();
        this.rCol = 0.45f + tint * 0.25f;
        this.gCol = 0.95f;
        this.bCol = 1.0f;
        this.alpha = 0;
    }

    @Override
    public void tick() {
        super.tick();
        float life = age / (float) lifetime;
        float fade = Math.min(1, Math.min(life * 6, (1 - life) * 4));
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.15f + phase);
        this.alpha = fade * pulse;
        this.quadSize = baseSize * (0.8f + 0.2f * pulse);
        this.xd += (random.nextDouble() - 0.5) * 0.002;
        this.zd += (random.nextDouble() - 0.5) * 0.002;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            GlimmerParticle p = new GlimmerParticle(level, x, y, z, vx, vy, vz);
            p.pickSprite(sprites);
            return p;
        }
    }
}
