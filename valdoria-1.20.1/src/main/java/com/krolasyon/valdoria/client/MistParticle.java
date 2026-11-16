package com.krolasyon.valdoria.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** A big, soft, slowly rising and spreading puff of white mist. */
public class MistParticle extends TextureSheetParticle {
    private final float maxAlpha;

    protected MistParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z);
        this.xd = vx + (random.nextDouble() - 0.5) * 0.03;
        this.yd = vy + 0.01 + random.nextDouble() * 0.02;
        this.zd = vz + (random.nextDouble() - 0.5) * 0.03;
        this.lifetime = 60 + random.nextInt(60);
        this.quadSize = 0.6f + random.nextFloat() * 0.9f;
        this.gravity = 0;
        this.friction = 0.96f;
        this.hasPhysics = false;
        float c = 0.92f + random.nextFloat() * 0.08f;
        this.rCol = c;
        this.gCol = c;
        this.bCol = 1.0f;
        this.maxAlpha = 0.18f + random.nextFloat() * 0.14f;
        this.alpha = 0;
    }

    @Override
    public void tick() {
        super.tick();
        float life = age / (float) lifetime;
        this.alpha = maxAlpha * Math.min(1, Math.min(life * 5, (1 - life) * 2));
        this.quadSize *= 1.012f;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            MistParticle p = new MistParticle(level, x, y, z, vx, vy, vz);
            p.pickSprite(sprites);
            return p;
        }
    }
}
