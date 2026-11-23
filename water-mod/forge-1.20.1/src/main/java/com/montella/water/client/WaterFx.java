package com.montella.water.client;

import com.montella.water.Config;
import com.montella.water.Currents;
import com.montella.water.ModParticles;
import com.montella.water.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Splashes, wakes, rain rings and the sound of streams. */
public final class WaterFx {
    private WaterFx() {}

    private static final Map<Integer, Boolean> WAS_IN_WATER = new HashMap<>();
    private static LoopSound stream;
    private static long tick = 0;

    static void reset() {
        WAS_IN_WATER.clear();
        if (stream != null) stream.fading = true;
        stream = null;
    }

    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        Player p = mc.player;
        tick++;
        // the boat the local player steers is simulated here
        if (p.getVehicle() instanceof Boat b && b.isControlledByLocalInstance() && b.isInWater()) Currents.apply(b, 0.05, 0.4);

        AABB area = p.getBoundingBox().inflate(40);
        Set<Integer> seen = new HashSet<>();
        for (Entity e : level.getEntitiesOfClass(Entity.class, area)) {
            if (e.isSpectator()) continue;
            int id = e.getId();
            seen.add(id);
            boolean in = e.isInWater();
            Boolean was = WAS_IN_WATER.put(id, in);
            double dy = e.getY() - e.yo;
            if (e == p) dy = Math.min(dy, p.getDeltaMovement().y);
            if (in && was != null && !was && dy < -0.22 && Config.SPLASHES.get()) splash(level, e, -dy);
            if (Config.WAKES.get() && tick % 3 == (id & 1)) wake(level, e);
        }
        WAS_IN_WATER.keySet().retainAll(seen);
        if (Config.RAIN_RIPPLES.get()) rain(level, p);
        if (tick % 20 == 0) streamSound(mc, level, p);
    }

    /** y of the water surface at the entity, or NaN */
    static double surfaceAt(ClientLevel level, double x, double y, double z) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int by = (int) Math.floor(y);
        for (int k = -1; k <= 3; k++) {
            m.set(Math.floor(x), by + k, Math.floor(z));
            FluidState fs = level.getFluidState(m);
            if (!fs.is(FluidTags.WATER)) continue;
            FluidState up = level.getFluidState(m.above());
            if (!up.is(FluidTags.WATER)) return m.getY() + fs.getHeight(level, m);
        }
        return Double.NaN;
    }

    static void splash(ClientLevel level, Entity e, double speed) {
        double s = surfaceAt(level, e.getX(), e.getY(), e.getZ());
        if (Double.isNaN(s)) return;
        double size = Math.sqrt(e.getBbWidth() * Math.max(0.5, e.getBbHeight()));
        double energy = Math.min(3.0, speed * size);
        double d = Fx.density();
        double w = e.getBbWidth() * 0.5 + 0.1;
        // the crown: a ring of droplets thrown up and out
        for (int i = Fx.count(d * (10 + energy * 70)); i > 0; i--) {
            double a = Fx.R.nextDouble() * Math.PI * 2;
            double out = 0.05 + Fx.R.nextDouble() * 0.12 * (0.5 + energy);
            double up = 0.12 + Fx.R.nextDouble() * 0.22 * (0.4 + energy);
            Fx.add(ModParticles.SPRAY.get(), e.getX() + Math.cos(a) * w, s + 0.05, e.getZ() + Math.sin(a) * w, Math.cos(a) * out, up, Math.sin(a) * out);
        }
        // a column of water shoots up for big impacts
        if (energy > 0.8) {
            for (int i = Fx.count(d * energy * 15); i > 0; i--)
                Fx.add(ModParticles.SPRAY.get(), e.getX() + Fx.sym(w * 0.5), s + 0.1, e.getZ() + Fx.sym(w * 0.5), Fx.sym(0.03), 0.3 + Fx.R.nextDouble() * 0.25 * energy, Fx.sym(0.03));
            for (int i = Fx.count(d * energy * 3); i > 0; i--)
                Fx.add(ModParticles.MIST.get(), e.getX() + Fx.sym(1), s + 0.3, e.getZ() + Fx.sym(1), Fx.sym(0.02), 0.01, Fx.sym(0.02));
        }
        for (int i = 0; i < 2 + (int) (energy * 2); i++)
            Fx.add(ModParticles.RIPPLE.get(), e.getX() + Fx.sym(0.2), s + 0.015, e.getZ() + Fx.sym(0.2), 0, w + 0.6 + energy * (0.8 + i * 0.6), 0);
        for (int i = Fx.count(d * (1 + energy * 4)); i > 0; i--) {
            double a = Fx.R.nextDouble() * Math.PI * 2;
            double sp = 0.02 + Fx.R.nextDouble() * 0.05 * (0.5 + energy);
            Fx.add(ModParticles.FOAM.get(), e.getX() + Math.cos(a) * w, s + 0.01, e.getZ() + Math.sin(a) * w, Math.cos(a) * sp, 0.3 + energy * 0.25, Math.sin(a) * sp);
        }
        float vol = (float) Math.min(1.6, 0.25 + energy * 0.6) * Config.SOUND_VOLUME.get().floatValue();
        float pitch = (float) (1.25 - Math.min(1.0, energy) * 0.45 + Fx.sym(0.08));
        level.playLocalSound(e.getX(), s, e.getZ(), energy > 0.7 ? ModSounds.SPLASH_BIG.get() : ModSounds.SPLASH_SMALL.get(),
                SoundSource.NEUTRAL, vol, pitch, false);
    }

    /** rings and a foam trail behind things moving along the surface */
    static void wake(ClientLevel level, Entity e) {
        if (!e.isInWater() && !(e instanceof Boat)) return;
        if (e.isUnderWater() && !(e instanceof Boat)) return;
        double vx = e.getX() - e.xo, vz = e.getZ() - e.zo;
        double sp = Math.sqrt(vx * vx + vz * vz);
        if (sp < 0.04) return;
        double s = surfaceAt(level, e.getX(), e.getY(), e.getZ());
        if (Double.isNaN(s) || Math.abs(s - e.getY()) > e.getBbHeight() + 0.3) return;
        double w = e.getBbWidth() * 0.5;
        double d = Fx.density();
        Fx.add(ModParticles.RIPPLE.get(), e.getX() - vx * 2, s + 0.015, e.getZ() - vz * 2, 0, w + 0.4 + sp * 4, 0);
        // bow wave: foam pushed to both sides, wider and whiter with speed
        double nx = -vz / sp, nz = vx / sp;
        for (int i = Fx.count(d * (0.1 + sp * 3)); i > 0; i--) {
            double side = Fx.R.nextBoolean() ? 1 : -1;
            Fx.add(ModParticles.FOAM.get(), e.getX() + nx * side * w - vx * 3, s + 0.01, e.getZ() + nz * side * w - vz * 3,
                    nx * side * sp * 0.3, 0.25 + Math.min(0.5, sp * 1.5), nz * side * sp * 0.3);
        }
        if (sp > 0.18)
            for (int i = Fx.count(d * sp * 6); i > 0; i--)
                Fx.add(ModParticles.SPRAY.get(), e.getX() + vx * 3 + Fx.sym(w), s + 0.05, e.getZ() + vz * 3 + Fx.sym(w), vx * 0.5 + Fx.sym(0.05), 0.08 + Fx.R.nextDouble() * 0.1, vz * 0.5 + Fx.sym(0.05));
    }

    /** raindrops hitting water leave small rings */
    static void rain(ClientLevel level, Player p) {
        float rain = level.getRainLevel(1f);
        if (rain < 0.05) return;
        double d = Fx.density();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int i = Fx.count(d * rain * 30); i > 0; i--) {
            int x = p.getBlockX() + (int) Fx.sym(16), z = p.getBlockZ() + (int) Fx.sym(16);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            m.set(x, y - 1, z);
            FluidState fs = level.getFluidState(m);
            if (!fs.is(FluidTags.WATER) || !level.isRainingAt(m.above())) continue;
            double sy = m.getY() + fs.getHeight(level, m) + 0.015;
            double fx = x + Fx.R.nextDouble(), fz = z + Fx.R.nextDouble();
            Fx.add(ModParticles.RIPPLE.get(), fx, sy, fz, 0, 0.25 + Fx.R.nextDouble() * 0.25, 0);
            if (Fx.R.nextInt(3) == 0) Fx.add(ModParticles.SPRAY.get(), fx, sy, fz, Fx.sym(0.02), 0.08 + Fx.R.nextDouble() * 0.05, Fx.sym(0.02));
        }
    }

    /** a babbling loop placed at the busiest flowing water nearby */
    static void streamSound(Minecraft mc, ClientLevel level, Player p) {
        if (!Config.STREAM_SOUNDS.get()) {
            if (stream != null) { stream.fading = true; stream = null; }
            return;
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int hits = 0;
        double sx = 0, sy = 0, sz = 0;
        for (int i = 0; i < 400; i++) {
            m.set(p.getBlockX() + (int) Fx.sym(12), p.getBlockY() + (int) Fx.sym(5), p.getBlockZ() + (int) Fx.sym(12));
            FluidState fs = level.getFluidState(m);
            if (!fs.is(FluidTags.WATER) || fs.isSource()) continue;
            if (fs.hasProperty(FlowingFluid.FALLING) && fs.getValue(FlowingFluid.FALLING)) continue;
            Vec3 f = fs.getFlow(level, m);
            if (f.lengthSqr() < 0.01) continue;
            hits++;
            sx += m.getX() + 0.5; sy += m.getY() + 0.5; sz += m.getZ() + 0.5;
        }
        if (hits < 3) {
            if (stream != null) { stream.fading = true; stream = null; }
            return;
        }
        if (stream == null || stream.isStopped()) {
            stream = new LoopSound(ModSounds.STREAM.get(), sx / hits, sy / hits, sz / hits, 0.95f + (float) Fx.sym(0.08));
            mc.getSoundManager().play(stream);
        }
        stream.moveTo(sx / hits, sy / hits, sz / hits);
        stream.target = (float) Math.min(1.0, 0.15 + hits * 0.02) * Config.SOUND_VOLUME.get().floatValue();
    }
}
