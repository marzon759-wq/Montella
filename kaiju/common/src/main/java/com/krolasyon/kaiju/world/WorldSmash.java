package com.krolasyon.kaiju.world;

import com.krolasyon.kaiju.KaijuConfig;
import com.krolasyon.kaiju.KaijuMod;
import com.krolasyon.kaiju.entity.DebrisEntity;
import com.krolasyon.kaiju.entity.KaijuEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Everything a kaiju does to the world: bodies plough through what they move into, feet leave craters, falls flatten
 * houses, shockwaves throw rubble, the beam vaporises terrain, and hands rip out whole trees (roots and all), chunks of
 * buildings or slabs of ground to carry, swing or throw. All of it respects the mobGriefing game rule and a per-tick
 * block budget so a fight cannot stall the server.
 */
public final class WorldSmash {
    private WorldSmash() {}

    static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static int budgetTick = -1, budget;

    static double ground(ServerLevel sl, Vec3 p, KaijuEntity e) {
        if (e != null) return e.groundAt(p);
        return sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, Mth.floor(p.x), Mth.floor(p.z));
    }

    static boolean griefing(ServerLevel sl) {
        return KaijuConfig.griefing && sl.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
    }

    static boolean take(ServerLevel sl, int n) {
        int t = (int) sl.getGameTime();
        if (t != budgetTick) { budgetTick = t; budget = KaijuConfig.blocksPerTick; }
        if (budget <= 0) return false;
        budget -= n;
        return true;
    }

    static boolean breakable(ServerLevel sl, BlockPos p, BlockState s) {
        if (s.isAir()) return false;
        if (!s.getFluidState().isEmpty() && s.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) return false;
        float h = s.getDestroySpeed(sl, p);
        return h >= 0 && h < 60;
    }

    /** remove a block; sometimes it flies off as a falling block, sometimes it leaves dust */
    static void smash(ServerLevel sl, BlockPos p, BlockState s, Vec3 from, float fling, Random rnd) {
        if (fling > 0 && rnd.nextFloat() < 0.035f && KaijuConfig.flyingDebris) {
            FallingBlockEntity fb = FallingBlockEntity.fall(sl, p, s);
            Vec3 out = Vec3.atCenterOf(p).subtract(from).normalize().scale(fling).add(0, 0.4 + rnd.nextFloat() * fling * 0.6, 0);
            fb.setDeltaMovement(out);
            fb.dropItem = false;
            fb.time = 1;
        } else {
            sl.setBlock(p, AIR, 2 | 16);
        }
    }

    // ------------------------------------------------------------------ the body ploughing through the world
    public static void carveBody(KaijuEntity e) {
        if (!(e.level() instanceof ServerLevel sl) || !griefing(sl)) return;
        Random rnd = new Random(e.tickCount * 31L + e.getId());
        int n = e.nodes.length;
        int per = Math.min(n, 12);
        for (int c = 0; c < per; c++) {
            int i = (e.tickCount * per + c) % n;
            Vec3 p = e.nodes[i];
            if (p == null) continue;
            float r = e.nodeR(i) * 0.92f;
            if (r <= 0.5f || !e.partOk(e.spec.nodes.get(i).sever())) continue;
            Vec3 last = lastCarve(e, i);
            if (last != null && last.distanceToSqr(p) < 0.36) continue;
            setLastCarve(e, i, p);
            Vec3 vel = e.prevNodes[i] == null ? Vec3.ZERO : p.subtract(e.prevNodes[i]);
            carveSphere(sl, p, r, vel.length() > 0.4 ? (float) Math.min(1.6, vel.length()) : 0.3f, rnd, p.subtract(vel.scale(4)));
        }
    }

    private static final Map<Integer, Vec3[]> LAST = new WeakHashMap<>();

    static Vec3 lastCarve(KaijuEntity e, int i) {
        Vec3[] a = LAST.computeIfAbsent(e.getId(), x -> new Vec3[e.nodes.length]);
        return i < a.length ? a[i] : null;
    }

    static void setLastCarve(KaijuEntity e, int i, Vec3 p) {
        Vec3[] a = LAST.computeIfAbsent(e.getId(), x -> new Vec3[e.nodes.length]);
        if (i < a.length) a[i] = p;
    }

    public static int carveSphere(ServerLevel sl, Vec3 c, float r, float fling, Random rnd, Vec3 from) {
        int ri = Mth.ceil(r);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int count = 0;
        double r2 = r * r;
        int cx = Mth.floor(c.x), cy = Mth.floor(c.y), cz = Mth.floor(c.z);
        for (int y = -ri; y <= ri; y++) {
            int wy = cy + y;
            if (wy < sl.getMinBuildHeight() || wy >= sl.getMaxBuildHeight()) continue;
            for (int x = -ri; x <= ri; x++) {
                for (int z = -ri; z <= ri; z++) {
                    double dx = cx + x + 0.5 - c.x, dy = wy + 0.5 - c.y, dz = cz + z + 0.5 - c.z;
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    m.set(cx + x, wy, cz + z);
                    BlockState s = sl.getBlockState(m);
                    if (!breakable(sl, m, s)) continue;
                    if (!take(sl, 1)) return count;
                    smash(sl, m.immutable(), s, from, fling, rnd);
                    count++;
                }
            }
        }
        if (count > 0 && rnd.nextFloat() < 0.3f) {
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y, c.z, Math.min(12, count / 4 + 1), r * 0.5, r * 0.5, r * 0.5, 0.02);
            sl.playSound(null, c.x, c.y, c.z, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 4f, 0.4f);
        }
        return count;
    }

    // ------------------------------------------------------------------ feet, falls, shockwaves, beam
    public static void footprint(ServerLevel sl, Vec3 foot, float r, KaijuEntity e) {
        if (!griefing(sl)) return;
        Random rnd = new Random();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int ri = Mth.ceil(r);
        double gy = ground(sl, foot, e);
        for (int x = -ri; x <= ri; x++)
            for (int z = -ri; z <= ri; z++) {
                if (x * x + z * z > r * r) continue;
                int depth = (x * x + z * z) < r * r * 0.4 ? 2 : 1;
                for (int d = 0; d < depth + 3; d++) {
                    m.set(Mth.floor(foot.x) + x, (int) gy - depth + d, Mth.floor(foot.z) + z);
                    BlockState s = sl.getBlockState(m);
                    if (d < depth && breakable(sl, m, s) && take(sl, 1)) sl.setBlock(m, AIR, 2 | 16);
                    else if (d >= depth && breakable(sl, m, s) && !s.is(BlockTags.DIRT) && take(sl, 1)) smash(sl, m.immutable(), s, foot, 0.6f, rnd);
                }
            }
        BlockState under = sl.getBlockState(BlockPos.containing(foot.x, gy - 1, foot.z));
        if (!under.isAir())
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), foot.x, gy, foot.z, 60, r * 0.6, 0.5, r * 0.6, 0.3);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, foot.x, gy + 1, foot.z, 12, r * 0.6, 0.5, r * 0.6, 0.02);
    }

    public static void crater(ServerLevel sl, Vec3 c, float r, int depth, KaijuEntity e) {
        if (!griefing(sl)) return;
        Random rnd = new Random();
        double gy = ground(sl, c, e);
        Vec3 center = new Vec3(c.x, Math.min(c.y, gy + r * 0.3), c.z);
        carveSphere(sl, center, r * 0.75f, 1.1f, rnd, center.subtract(0, 4, 0));
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int ri = Mth.ceil(r);
        for (int x = -ri; x <= ri; x++)
            for (int z = -ri; z <= ri; z++) {
                double d2 = x * x + z * z;
                if (d2 > r * r) continue;
                int dd = (int) (depth * (1 - Math.sqrt(d2) / r)) + 1;
                for (int y = 0; y < dd; y++) {
                    m.set(Mth.floor(c.x) + x, (int) gy - y, Mth.floor(c.z) + z);
                    BlockState s = sl.getBlockState(m);
                    if (breakable(sl, m, s) && take(sl, 1)) smash(sl, m.immutable(), s, center.subtract(0, 3, 0), 0.9f, rnd);
                }
            }
        BlockState under = sl.getBlockState(BlockPos.containing(c.x, gy - 1, c.z));
        if (!under.isAir()) sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under), c.x, gy, c.z, 120, r * 0.6, 1, r * 0.6, 0.4);
    }

    public static void shockwave(ServerLevel sl, Vec3 c, float r, float power, KaijuEntity e) {
        // ring of thrown rubble and dust; the ground right under the impact is broken
        sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 1, c.z, 20, r * 0.4, 0.5, r * 0.4, 0);
        for (int i = 0; i < 48; i++) {
            double a = i / 48.0 * Math.PI * 2;
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x + Math.cos(a) * r * 0.7, c.y + 1, c.z + Math.sin(a) * r * 0.7, 2, 1, 0.3, 1, 0.05);
        }
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 14f, 0.3f);
        if (griefing(sl)) {
            crater(sl, c, r * 0.3f, 2, e);
            Random rnd = new Random();
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            for (int i = 0; i < (int) (40 * power); i++) {
                double a = rnd.nextDouble() * Math.PI * 2, d = r * (0.3 + rnd.nextDouble() * 0.6);
                m.set(c.x + Math.cos(a) * d, ground(sl, c.add(Math.cos(a) * d, 0, Math.sin(a) * d), e) - 1, c.z + Math.sin(a) * d);
                BlockState s = sl.getBlockState(m);
                if (breakable(sl, m, s) && take(sl, 1) && KaijuConfig.flyingDebris) {
                    FallingBlockEntity fb = FallingBlockEntity.fall(sl, m.immutable(), s);
                    fb.setDeltaMovement(Math.cos(a) * 0.5 * power, 0.6 + rnd.nextDouble() * 0.6 * power, Math.sin(a) * 0.5 * power);
                    fb.dropItem = false;
                }
            }
        }
        for (net.minecraft.world.entity.LivingEntity le : sl.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(r, r * 0.4, r), x -> !(x instanceof KaijuEntity) && x.getVehicle() != e)) {
            double d = le.position().distanceTo(c);
            float f = (float) Math.max(0, 1 - d / r);
            le.hurt(e != null ? e.damageSources().mobAttack(e) : sl.damageSources().generic(), 20 * f * power);
            le.setDeltaMovement(le.position().subtract(c).normalize().scale(1.5 * f).add(0, 0.8 * f, 0));
            le.hurtMarked = true;
        }
    }

    /** the beam burning through terrain: returns blocks used */
    public static int vaporize(ServerLevel sl, Vec3 c, float r, int max, KaijuEntity e) {
        if (!griefing(sl)) return 0;
        int ri = Mth.ceil(r);
        int n = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        Random rnd = new Random();
        for (int x = -ri; x <= ri && n < max; x++)
            for (int y = -ri; y <= ri && n < max; y++)
                for (int z = -ri; z <= ri && n < max; z++) {
                    if (x * x + y * y + z * z > r * r) continue;
                    m.set(Mth.floor(c.x) + x, Mth.floor(c.y) + y, Mth.floor(c.z) + z);
                    BlockState s = sl.getBlockState(m);
                    if (!breakable(sl, m, s) || !take(sl, 1)) continue;
                    // the rim melts to magma and catches fire
                    boolean rim = x * x + y * y + z * z > (r - 1.2) * (r - 1.2);
                    if (rim && rnd.nextFloat() < 0.25f && s.isSolidRender(sl, m)) sl.setBlock(m, (rnd.nextBoolean() ? Blocks.MAGMA_BLOCK : Blocks.BLACKSTONE).defaultBlockState(), 2 | 16);
                    else sl.setBlock(m, rim && rnd.nextFloat() < 0.15f ? Blocks.FIRE.defaultBlockState() : AIR, 2 | 16);
                    n++;
                }
        return n;
    }

    // ------------------------------------------------------------------ picking things up
    public static DebrisEntity capture(ServerLevel sl, KaijuEntity e, String kind) {
        if (!griefing(sl)) return null;
        float k = e.k();
        Vec3 hands = e.nodePos(e.spec.node("hand_r")).add(e.nodePos(e.spec.node("hand_l"))).scale(0.5);
        Vec3 reachPt = new Vec3(hands.x, ground(sl, hands, e), hands.z).add(e.forward().scale(6 * k));
        List<BlockPos> picked = new ArrayList<>();
        switch (kind) {
            case "tree" -> picked = findTree(sl, reachPt, 40 * k);
            case "chunk" -> picked = boxAt(sl, reachPt, (int) (12 * k), (int) (22 * k), 900, true);
            case "slab" -> picked = boxAt(sl, reachPt.add(0, -3, 0), (int) (13 * k), 4, 900, false);
            case "kick" -> {
                Vec3 f = e.nodePos(e.spec.node("foot_r")).add(e.forward().scale(8 * k));
                picked = boxAt(sl, new Vec3(f.x, ground(sl, f, e) - 2, f.z), (int) (6 * k), 3, 200, false);
            }
            default -> { }
        }
        KaijuMod.LOG.info("[KAIJU] capture {} -> {} blocks", kind, picked.size());
        if (picked.size() < 6) return null;
        DebrisEntity d = new DebrisEntity(KaijuMod.DEBRIS.get(), sl);
        d.capture(sl, picked);
        sl.addFreshEntity(d);
        d.holder = e;
        sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 12f, 0.3f);
        sl.playSound(null, d.getX(), d.getY(), d.getZ(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.BLOCKS, 12f, 0.4f);
        return d;
    }

    /** the nearest tree: connected logs and their leaves, plus the root ball of earth under the trunk */
    static List<BlockPos> findTree(ServerLevel sl, Vec3 around, float radius) {
        BlockPos center = BlockPos.containing(around);
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        int r = (int) radius;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x += 1)
            for (int z = -r; z <= r; z += 1) {
                int top = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, center.getX() + x, center.getZ() + z);
                for (int y = top; y > top - 30; y--) {
                    m.set(center.getX() + x, y, center.getZ() + z);
                    if (sl.getBlockState(m).is(BlockTags.LOGS)) {
                        double d = x * x + z * z;
                        if (d < bd) { bd = d; best = m.immutable(); }
                        break;
                    }
                }
            }
        if (best == null) return new ArrayList<>();
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> q = new ArrayDeque<>();
        q.add(best);
        seen.add(best);
        int lowest = best.getY();
        while (!q.isEmpty() && out.size() < 1400) {
            BlockPos p = q.poll();
            BlockState s = sl.getBlockState(p);
            boolean log = s.is(BlockTags.LOGS), leaf = s.is(BlockTags.LEAVES);
            if (!log && !leaf) continue;
            out.add(p);
            if (log) lowest = Math.min(lowest, p.getY());
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos n = p.offset(dx, dy, dz);
                        if (seen.add(n) && n.distSqr(best) < 30 * 30) q.add(n);
                    }
        }
        // the root ball
        BlockPos base = new BlockPos(best.getX(), lowest - 1, best.getZ());
        for (int x = -2; x <= 2; x++)
            for (int y = -2; y <= 0; y++)
                for (int z = -2; z <= 2; z++) {
                    if (x * x + z * z > 5) continue;
                    BlockPos p = base.offset(x, y, z);
                    BlockState s = sl.getBlockState(p);
                    if (!s.isAir() && breakable(sl, p, s) && s.getFluidState().isEmpty()) out.add(p);
                }
        return out;
    }

    /** a building or a lump of ground: everything solid inside a box resting on the ground */
    static List<BlockPos> boxAt(ServerLevel sl, Vec3 c, int r, int h, int max, boolean preferBuilt) {
        List<BlockPos> out = new ArrayList<>();
        int gy = Mth.floor(c.y);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = -2; y < h && out.size() < max; y++)
            for (int x = -r; x <= r && out.size() < max; x++)
                for (int z = -r; z <= r && out.size() < max; z++) {
                    if (x * x + z * z > r * r + r) continue;
                    m.set(Mth.floor(c.x) + x, gy + y, Mth.floor(c.z) + z);
                    BlockState s = sl.getBlockState(m);
                    if (s.isAir() || !s.getFluidState().isEmpty() || !breakable(sl, m, s)) continue;
                    if (preferBuilt && y < 0 && isNatural(s)) continue;
                    out.add(m.immutable());
                }
        return out;
    }

    static boolean isNatural(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL);
    }
}
