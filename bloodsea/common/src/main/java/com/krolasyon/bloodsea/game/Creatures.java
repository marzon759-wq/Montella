package com.krolasyon.bloodsea.game;

import com.krolasyon.bloodsea.entity.*;
import com.krolasyon.bloodsea.registry.ModEntities;
import com.krolasyon.bloodsea.registry.ModSounds;
import com.krolasyon.bloodsea.world.Regions;
import com.krolasyon.bloodsea.world.SeaTime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Who comes, where and when. Each player has their own: the Wake behind them on the open blood at night, the Bone
 * Gatherer in the Bone Reach, the Name Caller in the fog of the Mirage (and, rarely, under the Blood Moon anywhere),
 * the Changer among the cargo of the raft, and once in a long while the Leviathan in the eastern deep.
 */
public final class Creatures {
    private static final Map<UUID, Integer> LEV_COOLDOWN = new HashMap<>();
    private static final Map<UUID, Integer> CHANGER_COOLDOWN = new HashMap<>();

    private Creatures() {}

    /** called every 20 ticks for a player in the sea */
    public static void tick(ServerPlayer p, ServerLevel lvl, Regions.Region r) {
        boolean day = SeaTime.isDay(lvl);
        UUID id = p.getUUID();
        double dist = Regions.dist(p.getX(), p.getZ());
        boolean onBlood = p.getVehicle() != null || p.isInFluidType(com.krolasyon.bloodsea.registry.ModFluids.BLOOD_TYPE.get());
        // ---- the Wake: on the open blood at night
        if (!day && onBlood && none(lvl, p, ModEntities.WAKE.get(), 110) && lvl.random.nextInt(25) == 0) spawn(lvl, p, "wake");
        // ---- the Bone Gatherer: anywhere in the Bone Reach
        if (r == Regions.Region.BONES && none(lvl, p, ModEntities.GATHERER.get(), 110) && lvl.random.nextInt(day ? 45 : 25) == 0) spawn(lvl, p, "gatherer");
        // ---- the Name Caller: the Mirage's fog, or the Blood Moon
        boolean callerZone = r == Regions.Region.MIRAGE || (!day && r != Regions.Region.EDGE && lvl.random.nextInt(4) == 0);
        if (callerZone && none(lvl, p, ModEntities.CALLER.get(), 120) && lvl.random.nextInt(30) == 0) spawn(lvl, p, "caller");
        // ---- the Changer: near the raft
        int cc = CHANGER_COOLDOWN.getOrDefault(id, 0) - 1;
        CHANGER_COOLDOWN.put(id, cc);
        if (dist < 14 && cc <= 0 && none(lvl, p, ModEntities.CHANGER.get(), 40) && lvl.random.nextInt(day ? 60 : 25) == 0) {
            if (spawn(lvl, p, "changer")) CHANGER_COOLDOWN.put(id, 120);
        }
        // ---- the Leviathan: the eastern deep. Its heart first, louder the deeper you go
        int lc = LEV_COOLDOWN.getOrDefault(id, 30) - 1;
        LEV_COOLDOWN.put(id, lc);
        if (r == Regions.Region.LEVIATHAN) {
            float deep = (float) Mth.clamp((dist - Regions.DRIFT_R) / 900.0, 0.1, 1.0);
            if (lvl.random.nextInt(3) == 0)
                lvl.playSound(null, p.getX(), p.getY() - 8, p.getZ(), ModSounds.get("lev_heartbeat"), SoundSource.HOSTILE, 0.6f + deep * 2.4f, 0.75f + deep * 0.1f);
            if (dist > 650 && lc <= 0 && onBlood && none(lvl, p, ModEntities.LEVIATHAN.get(), 300) && lvl.random.nextInt(12) == 0) {
                if (spawn(lvl, p, "leviathan")) LEV_COOLDOWN.put(id, 240);
            }
        }
    }

    private static boolean none(ServerLevel lvl, ServerPlayer p, EntityType<?> t, double r) {
        return lvl.getEntities(t, new AABB(p.blockPosition()).inflate(r), e -> true).isEmpty();
    }

    private static Vec3 flat(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        return h.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : h.normalize();
    }

    /** put one where it should come from; false if there was nowhere to put it */
    public static boolean spawn(ServerLevel lvl, ServerPlayer p, String kind) {
        Vec3 look = flat(p.getViewVector(1f));
        double sea = Regions.SEA + 0.9;
        switch (kind) {
            case "wake" -> {
                Vec3 at = p.position().subtract(look.scale(34));
                WakeEntity w = ModEntities.WAKE.get().create(lvl);
                if (w == null) return false;
                w.moveTo(at.x, sea, at.z, (float) (Mth.atan2(look.z, look.x) * Mth.RAD_TO_DEG) - 90f, 0);
                return lvl.addFreshEntity(w);
            }
            case "gatherer" -> {
                Vec3 at = p.position().add(look.scale(30)).add(new Vec3(-look.z, 0, look.x).scale(lvl.random.nextGaussian() * 8));
                GathererEntity g = ModEntities.GATHERER.get().create(lvl);
                if (g == null) return false;
                double y = Math.max(sea, lvl.getHeight(Heightmap.Types.OCEAN_FLOOR, Mth.floor(at.x), Mth.floor(at.z)));
                g.moveTo(at.x, y, at.z, 0, 0);
                return lvl.addFreshEntity(g);
            }
            case "caller" -> {
                double a = lvl.random.nextDouble() * Math.PI * 2;
                Vec3 at = p.position().add(Math.cos(a) * 36, 0, Math.sin(a) * 36);
                CallerEntity c = ModEntities.CALLER.get().create(lvl);
                if (c == null) return false;
                double y = Math.max(sea, lvl.getHeight(Heightmap.Types.OCEAN_FLOOR, Mth.floor(at.x), Mth.floor(at.z)));
                c.moveTo(at.x, y, at.z, 0, 0);
                return lvl.addFreshEntity(c);
            }
            case "changer" -> {
                // a dry, open spot on the raft or the land under them, behind their back
                for (int k = 0; k < 24; k++) {
                    double a = Math.atan2(-look.z, -look.x) + (lvl.random.nextDouble() - 0.5) * 2.0;
                    double rr = 2.5 + lvl.random.nextDouble() * 3;
                    BlockPos b = BlockPos.containing(p.getX() + Math.cos(a) * rr, p.getY() + 1, p.getZ() + Math.sin(a) * rr);
                    for (int dy = 0; dy < 4; dy++) {
                        BlockPos f = b.below(dy);
                        if (lvl.getBlockState(f).isAir() && lvl.getBlockState(f.above()).isAir() && lvl.getBlockState(f.below()).isFaceSturdy(lvl, f.below(), net.minecraft.core.Direction.UP)) {
                            ChangerEntity c = ModEntities.CHANGER.get().create(lvl);
                            if (c == null) return false;
                            c.moveTo(f.getX() + 0.5, f.getY(), f.getZ() + 0.5, lvl.random.nextFloat() * 360f, 0);
                            return lvl.addFreshEntity(c);
                        }
                    }
                }
                return false;
            }
            case "leviathan" -> {
                Vec3 at = p.position().add(look.scale(28));
                LeviathanEntity l = ModEntities.LEVIATHAN.get().create(lvl);
                if (l == null) return false;
                l.moveTo(at.x, sea, at.z, (float) (Mth.atan2(-look.z, -look.x) * Mth.RAD_TO_DEG) - 90f, 0);
                return lvl.addFreshEntity(l);
            }
        }
        return false;
    }
}
