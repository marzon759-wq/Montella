package com.montella.water.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public final class WaterParticles {
    private WaterParticles() {}

    static boolean inWater(ClientLevel level, double x, double y, double z) {
        BlockPos p = BlockPos.containing(x, y, z);
        FluidState fs = level.getFluidState(p);
        return fs.is(FluidTags.WATER) && y < p.getY() + fs.getHeight(level, p);
    }

    /** water droplets thrown out of a waterfall or a splash: fall under gravity, vanish when they hit water */
    public static class Spray extends TextureSheetParticle {
        Spray(ClientLevel l, double x, double y, double z, double vx, double vy, double vz, SpriteSet s) {
            super(l, x, y, z);
            this.xd = vx; this.yd = vy; this.zd = vz;
            this.gravity = 0.9f;
            this.friction = 0.97f;
            this.lifetime = 14 + random.nextInt(18);
            this.quadSize = 0.035f + random.nextFloat() * 0.05f;
            this.alpha = 0.75f + random.nextFloat() * 0.25f;
            float c = 0.9f + random.nextFloat() * 0.1f;
            this.rCol = c * 0.95f; this.gCol = c * 0.98f; this.bCol = c;
            this.hasPhysics = true;
            pickSprite(s);
        }
        @Override public void tick() {
            super.tick();
            if (yd < 0 && inWater(level, x, y, z)) remove();
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
        public record Provider(SpriteSet s) implements ParticleProvider<SimpleParticleType> {
            public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double vx, double vy, double vz) {
                return new Spray(l, x, y, z, vx, vy, vz, s);
            }
        }
    }

    /** spray mist: big soft puffs that swell, rise slowly and drift with the air */
    public static class Mist extends TextureSheetParticle {
        private final float start, end;
        Mist(ClientLevel l, double x, double y, double z, double vx, double vy, double vz, SpriteSet s) {
            super(l, x, y, z);
            this.xd = vx; this.yd = vy; this.zd = vz;
            this.gravity = -0.004f;
            this.friction = 0.96f;
            this.lifetime = 50 + random.nextInt(70);
            this.start = 0.5f + random.nextFloat() * 0.5f;
            this.end = start * (2.5f + random.nextFloat() * 1.5f);
            this.quadSize = start;
            this.roll = this.oRoll = random.nextFloat() * Mth.TWO_PI;
            this.hasPhysics = false;
            pickSprite(s);
            this.alpha = 0f;
        }
        @Override public void tick() {
            super.tick();
            float t = (float) age / lifetime;
            quadSize = Mth.lerp(1f - (1f - t) * (1f - t), start, end);
            alpha = 0.18f * Math.min(1f, t * 6f) * (1f - t);
            oRoll = roll;
            roll += 0.004f;
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
        public record Provider(SpriteSet s) implements ParticleProvider<SimpleParticleType> {
            public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double vx, double vy, double vz) {
                return new Mist(l, x, y, z, vx, vy, vz, s);
            }
        }
    }

    /** a particle drawn as a flat quad lying on the water surface */
    abstract static class Flat extends TextureSheetParticle {
        float angle;
        Flat(ClientLevel l, double x, double y, double z) {
            super(l, x, y, z);
            this.angle = random.nextFloat() * Mth.TWO_PI;
            this.hasPhysics = false;
            this.gravity = 0f;
        }
        @Override
        public void render(VertexConsumer vc, Camera cam, float pt) {
            Vec3 c = cam.getPosition();
            float px = (float) (Mth.lerp(pt, xo, x) - c.x());
            float py = (float) (Mth.lerp(pt, yo, y) - c.y());
            float pz = (float) (Mth.lerp(pt, zo, z) - c.z());
            float s = getQuadSize(pt);
            float ca = Mth.cos(angle) * s, sa = Mth.sin(angle) * s;
            float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
            int light = getLightColor(pt);
            float[][] k = {{-ca + sa, -sa - ca}, {-ca - sa, -sa + ca}, {ca - sa, sa + ca}, {ca + sa, sa - ca}};
            float[][] uv = {{u1, v1}, {u1, v0}, {u0, v0}, {u0, v1}};
            // both windings so the quad is seen from above and below
            for (int i = 0; i < 4; i++) vc.vertex(px + k[i][0], py, pz + k[i][1]).uv(uv[i][0], uv[i][1]).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
            for (int i = 3; i >= 0; i--) vc.vertex(px + k[i][0], py, pz + k[i][1]).uv(uv[i][0], uv[i][1]).color(rCol, gCol, bCol, alpha).uv2(light).endVertex();
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
    }

    /** foam patch floating on the surface, pushed outwards and carried by the current, slowly breaking up */
    public static class Foam extends Flat {
        private final float size;
        Foam(ClientLevel l, double x, double y, double z, double vx, double vy, double vz, SpriteSet s) {
            super(l, x, y, z);
            this.xd = vx; this.zd = vz; this.yd = 0;
            this.friction = 0.95f;
            this.lifetime = 35 + random.nextInt(45);
            this.size = (float) (vy > 0 ? vy : 0.35f + random.nextFloat() * 0.55f) * 0.75f;
            this.rCol = 0.86f; this.gCol = 0.9f; this.bCol = 0.92f;
            this.quadSize = size * 0.6f;
            pickSprite(s);
        }
        @Override public void tick() {
            xo = x; yo = y; zo = z;
            if (age++ >= lifetime) { remove(); return; }
            // ride the current
            BlockPos p = BlockPos.containing(x, y - 0.05, z);
            FluidState fs = level.getFluidState(p);
            if (!fs.is(FluidTags.WATER)) { remove(); return; }
            Vec3 f = fs.getFlow(level, p);
            xd = xd * friction + f.x * 0.004;
            zd = zd * friction + f.z * 0.004;
            double surface = p.getY() + fs.getHeight(level, p) + 0.01;
            x += xd; z += zd; y += (surface - y) * 0.3;
            float t = (float) age / lifetime;
            quadSize = size * (0.5f + 0.4f * Math.min(1f, t * 3f));
            alpha = 0.45f * Math.min(1f, t * 8f) * (t > 0.55f ? 1f - (t - 0.55f) / 0.45f : 1f);
            angle += 0.002f;
        }
        public record Provider(SpriteSet s) implements ParticleProvider<SimpleParticleType> {
            public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double vx, double vy, double vz) {
                return new Foam(l, x, y, z, vx, vy, vz, s);
            }
        }
    }

    /** expanding ring on the surface (drops, footsteps, splashes). vy = final radius */
    public static class Ripple extends Flat {
        private final float radius;
        Ripple(ClientLevel l, double x, double y, double z, double vx, double vy, double vz, SpriteSet s) {
            super(l, x, y, z);
            this.radius = (float) (vy > 0 ? vy : 0.6);
            this.lifetime = (int) (12 + radius * 14);
            this.quadSize = 0.05f;
            this.rCol = 0.82f; this.gCol = 0.88f; this.bCol = 0.92f;
            pickSprite(s);
        }
        @Override public void tick() {
            xo = x; yo = y; zo = z;
            if (age++ >= lifetime) { remove(); return; }
            float t = (float) age / lifetime;
            quadSize = radius * (float) Math.sqrt(t) + 0.05f;
            alpha = 0.16f * (1f - t);
        }
        public record Provider(SpriteSet s) implements ParticleProvider<SimpleParticleType> {
            public Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z, double vx, double vy, double vz) {
                return new Ripple(l, x, y, z, vx, vy, vz, s);
            }
        }
    }
}
