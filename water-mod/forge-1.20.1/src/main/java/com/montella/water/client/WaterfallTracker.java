package com.montella.water.client;

import com.montella.water.Config;
import com.montella.water.ModParticles;
import com.montella.water.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Finds waterfalls around the player (columns of falling water) and keeps them alive with spray, mist, foam,
 * ripples and a roar. The area is swept a slice per tick so the cost stays flat.
 */
public final class WaterfallTracker {
    private WaterfallTracker() {}

    /** where a falling column lands: x/z of the column, y of the surface it hits, height in blocks */
    record Fall(int x, int z, double surface, int height, boolean pool, long seen) {
        double dist2(Vec3 p) { double dx = x + 0.5 - p.x, dy = surface - p.y, dz = z + 0.5 - p.z; return dx * dx + dy * dy + dz * dz; }
    }

    static final Map<Long, Fall> FALLS = new HashMap<>();
    static final Map<Long, LoopSound> SOUNDS = new HashMap<>();
    private static int sweep = 0;
    private static long tick = 0;

    static void reset() {
        FALLS.clear();
        SOUNDS.values().forEach(s -> s.fading = true);
        SOUNDS.clear();
        sweep = 0;
    }

    static boolean isFalling(FluidState fs) {
        return fs.is(FluidTags.WATER) && fs.hasProperty(FlowingFluid.FALLING) && fs.getValue(FlowingFluid.FALLING);
    }

    static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        Player p = mc.player;
        tick++;
        int r = Config.SCAN_RADIUS.get();
        int side = 2 * r + 1;
        int total = side * side;
        int perTick = Math.max(1, total / 20);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int px = p.getBlockX(), py = p.getBlockY(), pz = p.getBlockZ();
        int yTop = Math.min(py + r, level.getMaxBuildHeight() - 1), yBot = Math.max(py - r, level.getMinBuildHeight() + 1);
        for (int k = 0; k < perTick; k++) {
            int c = sweep++;
            if (sweep >= total) sweep = 0;
            int x = px - r + c % side, z = pz - r + c / side;
            if (!level.hasChunkAt(m.set(x, py, z))) continue;
            for (int y = yTop; y >= yBot; y--) {
                FluidState fs = level.getFluidState(m.set(x, y, z));
                if (!isFalling(fs)) continue;
                FluidState below = level.getFluidState(m.set(x, y - 1, z));
                if (isFalling(below)) continue;
                int h = 1;
                while (h < 64 && isFalling(level.getFluidState(m.set(x, y + h, z)))) h++;
                boolean pool = below.is(FluidTags.WATER);
                double surface = pool ? y - 1 + below.getHeight(level, m.set(x, y - 1, z)) : y;
                FALLS.put(BlockPos.asLong(x, y, z), new Fall(x, z, surface, h, pool, tick));
                y -= h;
            }
        }
        // forget falls that were not confirmed by the last full sweeps or are far away
        Vec3 eye = p.getEyePosition();
        FALLS.values().removeIf(f -> tick - f.seen > 60 || f.dist2(eye) > (r + 16.0) * (r + 16.0));
        emit(level, eye);
        sounds(mc, eye);
    }

    private static void emit(ClientLevel level, Vec3 eye) {
        double d = Fx.density();
        if (d <= 0) return;
        for (Fall f : FALLS.values()) {
            double dist2 = f.dist2(eye);
            if (dist2 > 48 * 48) continue;
            double lod = dist2 < 20 * 20 ? 1.0 : 0.5;
            double e = Math.min(f.height, 30);
            double sx = f.x + 0.5, sz = f.z + 0.5, sy = f.surface;
            // spray: droplets thrown out where the water hits
            for (int i = Fx.count(d * lod * (0.5 + e * 0.22)); i > 0; i--) {
                double a = Fx.R.nextDouble() * Math.PI * 2;
                double sp = 0.03 + Fx.R.nextDouble() * 0.05 * Math.sqrt(e);
                Fx.add(ModParticles.SPRAY.get(), sx + Fx.sym(0.5), sy + 0.05, sz + Fx.sym(0.5),
                        Math.cos(a) * sp, 0.06 + Fx.R.nextDouble() * 0.07 * Math.sqrt(e), Math.sin(a) * sp);
            }
            // mist rising from the plunge
            if (f.height >= 2) {
                for (int i = Fx.count(d * lod * e * 0.018); i > 0; i--) {
                    double a = Fx.R.nextDouble() * Math.PI * 2;
                    double sp = 0.01 + Fx.R.nextDouble() * 0.03;
                    Fx.add(ModParticles.MIST.get(), sx + Fx.sym(1.0), sy + 0.2 + Fx.R.nextDouble() * Math.min(2.0, e * 0.15), sz + Fx.sym(1.0),
                            Math.cos(a) * sp, 0.005 + Fx.R.nextDouble() * 0.015, Math.sin(a) * sp);
                }
                // droplets peeling off the falling sheet
                for (int i = Fx.count(d * lod * e * 0.04); i > 0; i--) {
                    double hy = sy + Fx.R.nextDouble() * f.height;
                    double a = Fx.R.nextDouble() * Math.PI * 2;
                    Fx.add(ModParticles.SPRAY.get(), sx + Math.cos(a) * 0.52, hy, sz + Math.sin(a) * 0.52,
                            Math.cos(a) * 0.04, -0.1, Math.sin(a) * 0.04);
                }
            }
            if (f.pool) {
                // foam pushed out across the pool and rings spreading from the impact
                for (int i = Fx.count(d * lod * (0.04 + e * 0.01)); i > 0; i--) {
                    double a = Fx.R.nextDouble() * Math.PI * 2;
                    double rr = 0.3 + Fx.R.nextDouble() * 0.8;
                    double sp = 0.02 + Fx.R.nextDouble() * 0.04 + e * 0.002;
                    Fx.add(ModParticles.FOAM.get(), sx + Math.cos(a) * rr, sy + 0.01, sz + Math.sin(a) * rr,
                            Math.cos(a) * sp, 0.35 + Fx.R.nextDouble() * 0.5, Math.sin(a) * sp);
                }
                if (Fx.R.nextDouble() < d * lod * (0.06 + e * 0.005))
                    Fx.add(ModParticles.RIPPLE.get(), sx + Fx.sym(1.2), sy + 0.015, sz + Fx.sym(1.2), 0, 1.0 + Fx.R.nextDouble() * (0.8 + e * 0.05), 0);
            }
        }
    }

    private static void sounds(Minecraft mc, Vec3 eye) {
        if (tick % 10 != 0) return;
        Set<Long> keep = new HashSet<>();
        if (Config.WATERFALL_SOUNDS.get()) {
            // loudest falls first; one sound per cluster of neighbouring columns
            List<Fall> list = new ArrayList<>(FALLS.values());
            list.removeIf(f -> f.height < 2);
            list.sort(Comparator.comparingDouble(f -> -loudness(f, eye)));
            List<Fall> picked = new ArrayList<>();
            for (Fall f : list) {
                if (picked.size() >= 3) break;
                boolean near = false;
                for (Fall q : picked) if (Math.abs(q.x - f.x) <= 5 && Math.abs(q.z - f.z) <= 5) near = true;
                if (near) continue;
                picked.add(f);
                int width = 0;
                for (Fall q : list) if (Math.abs(q.x - f.x) <= 3 && Math.abs(q.z - f.z) <= 3) width++;
                long key = BlockPos.asLong(f.x >> 2, (int) f.surface >> 2, f.z >> 2);
                keep.add(key);
                LoopSound s = SOUNDS.get(key);
                if (s == null || s.isStopped()) {
                    float pitch = (float) (1.12 - Math.min(f.height, 30) * 0.012 + Fx.sym(0.04));
                    s = new LoopSound(ModSounds.WATERFALL.get(), f.x + 0.5, f.surface + 1, f.z + 0.5, pitch);
                    SOUNDS.put(key, s);
                    mc.getSoundManager().play(s);
                }
                s.moveTo(f.x + 0.5, f.surface + Math.min(f.height, 6) * 0.5, f.z + 0.5);
                // volume grows with height and width; > 1 also widens the audible range
                s.target = (float) (Math.min(2.2, 0.3 + Math.min(f.height, 24) * 0.05 + Math.min(width, 9) * 0.06) * Config.SOUND_VOLUME.get());
            }
        }
        Iterator<Map.Entry<Long, LoopSound>> it = SOUNDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, LoopSound> en = it.next();
            if (!keep.contains(en.getKey())) { en.getValue().fading = true; it.remove(); }
        }
    }

    private static double loudness(Fall f, Vec3 eye) {
        return Math.min(f.height, 24) / (1.0 + f.dist2(eye) / 64.0);
    }
}
