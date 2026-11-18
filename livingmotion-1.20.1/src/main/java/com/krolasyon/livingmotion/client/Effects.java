package com.krolasyon.livingmotion.client;

import com.krolasyon.livingmotion.LMConfig;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Client-side particle effects for every living entity in range: impact bits and sparks, attack swipes, running
 * dust, landing rings, death bursts, creeper fuse sparks and cold-weather breath. Also records landings for the
 * procedural squash in {@link MotionFX}.
 */
public final class Effects {
    private Effects() {}

    static final class State {
        int lastHurt, lastSwingTime, lastDeath, seen;
        boolean wasSwinging, wasOnGround = true;
        double peakY;
        int landTick = -100;
        float landPower;
    }

    private static final Int2ObjectOpenHashMap<State> STATES = new Int2ObjectOpenHashMap<>();
    private static int clock;

    /** 0..1 squash amount for an entity that landed recently. */
    public static float landing(LivingEntity e, float pt) {
        State s = STATES.get(e.getId());
        if (s == null) return 0f;
        float t = (e.tickCount - s.landTick) + pt;
        if (t < 0 || t > 8) return 0f;
        return s.landPower * Mth.sin(t / 8f * Mth.PI) * (1f - t / 10f);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            if (level == null) STATES.clear();
            return;
        }
        clock++;
        boolean fx = LMConfig.on(LMConfig.FX);
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double range = LMConfig.range();
        double r2 = range * range;
        for (Entity ent : level.entitiesForRendering()) {
            if (!(ent instanceof LivingEntity e)) continue;
            if (e.distanceToSqr(cam) > r2) continue;
            State s = STATES.get(e.getId());
            if (s == null) {
                s = new State();
                s.lastHurt = e.hurtTime;
                s.peakY = e.getY();
                s.wasOnGround = e.onGround();
                STATES.put(e.getId(), s);
            }
            s.seen = clock;
            update(level, e, s, fx && !(e == mc.player && mc.options.getCameraType().isFirstPerson()));
        }
        if (clock % 100 == 0) STATES.int2ObjectEntrySet().removeIf(en -> clock - en.getValue().seen > 100);
    }

    private static void update(ClientLevel level, LivingEntity e, State s, boolean fx) {
        RandomSource rnd = e.getRandom();
        // --- hit --------------------------------------------------------------------------------------------
        if (e.hurtTime > s.lastHurt && e.hurtTime > 0 && fx && LMConfig.on(LMConfig.FX_IMPACT)) impact(level, e, rnd, 10);
        s.lastHurt = e.hurtTime;

        // --- attack swipe -------------------------------------------------------------------------------------
        boolean swingStart = e.swinging && (!s.wasSwinging || e.swingTime < s.lastSwingTime);
        s.wasSwinging = e.swinging;
        s.lastSwingTime = e.swingTime;
        if (swingStart && fx && LMConfig.on(LMConfig.FX_SWING) && e instanceof Enemy && !(e instanceof Player)) {
            Vec3 look = Vec3.directionFromRotation(0, e.yBodyRot);
            double d = e.getBbWidth() * 0.6 + 0.5;
            double x = e.getX() + look.x * d, y = e.getY() + e.getBbHeight() * 0.65, z = e.getZ() + look.z * d;
            level.addParticle(ParticleTypes.SWEEP_ATTACK, x, y, z, 0.7 - Math.min(0.5, e.getBbWidth() * 0.3), 0, 0);
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.CRIT, x, y, z, look.x * 0.3 + rnd.nextGaussian() * 0.1, 0.05, look.z * 0.3 + rnd.nextGaussian() * 0.1);
            }
        }

        // --- landing ------------------------------------------------------------------------------------------
        boolean ground = e.onGround();
        if (!ground) {
            if (s.wasOnGround) s.peakY = e.getY();
            s.peakY = Math.max(s.peakY, e.getY());
        } else if (!s.wasOnGround) {
            double drop = s.peakY - e.getY();
            if (drop > 0.9 && !e.isInWater()) {
                s.landTick = e.tickCount;
                s.landPower = (float) Mth.clamp(drop / 5.0, 0.25, 1.0);
                if (fx && LMConfig.on(LMConfig.FX_LANDING)) ring(level, e, rnd, (int) Mth.clamp(drop * 5, 6, 28));
            }
        }
        s.wasOnGround = ground;

        // --- running dust -------------------------------------------------------------------------------------
        if (fx && ground && LMConfig.on(LMConfig.FX_FOOTSTEPS) && !(e instanceof Player) && !e.isInWater()) {
            Vec3 v = e.getDeltaMovement();
            double hv = Math.sqrt(v.x * v.x + v.z * v.z);
            if (hv > 0.11 && e.tickCount % 3 == 0) {
                BlockState under = e.getBlockStateOn();
                if (under.getRenderShape() != RenderShape.INVISIBLE) {
                    for (int i = 0; i < 2; i++) {
                        level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, under).setPos(e.getOnPos()),
                                e.getX() + (rnd.nextDouble() - 0.5) * e.getBbWidth(), e.getY() + 0.1,
                                e.getZ() + (rnd.nextDouble() - 0.5) * e.getBbWidth(), -v.x * 2, 0.15, -v.z * 2);
                    }
                }
            }
        }

        // --- death --------------------------------------------------------------------------------------------
        if (e.deathTime > 0 && s.lastDeath == 0 && fx && LMConfig.on(LMConfig.FX_DEATH)) {
            impact(level, e, rnd, 16);
            boolean undead = e.getMobType() == MobType.UNDEAD;
            for (int i = 0; i < 8; i++) {
                level.addParticle(undead ? ParticleTypes.SOUL : ParticleTypes.POOF,
                        e.getRandomX(0.6), e.getRandomY(), e.getRandomZ(0.6), 0, 0.02 + rnd.nextDouble() * 0.03, 0);
            }
        }
        s.lastDeath = e.deathTime;

        // --- creeper fuse -------------------------------------------------------------------------------------
        if (fx && e instanceof Creeper c && LMConfig.on(LMConfig.FX_FUSE)) {
            float sw = c.getSwelling(1f);
            if (sw > 0.01f) {
                double y = e.getY() + e.getBbHeight() + 0.1;
                level.addParticle(ParticleTypes.SMOKE, e.getRandomX(0.4), y, e.getRandomZ(0.4), 0, 0.03, 0);
                if (rnd.nextFloat() < 0.3f + sw) {
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, e.getRandomX(0.7), e.getRandomY(), e.getRandomZ(0.7),
                            rnd.nextGaussian() * 0.2, rnd.nextGaussian() * 0.2, rnd.nextGaussian() * 0.2);
                }
            }
        }

        // --- breath -------------------------------------------------------------------------------------------
        if (fx && LMConfig.on(LMConfig.FX_BREATH) && e.isAlive() && breathes(e) && !e.isUnderWater()) {
            int period = 50 + (e.getId() * 7) % 30;
            if ((e.tickCount + e.getId()) % period == 0) {
                BlockPos eye = BlockPos.containing(e.getEyePosition());
                if (level.getBiome(eye).value().coldEnoughToSnow(eye)) {
                    Vec3 look = Vec3.directionFromRotation(e.getXRot(), e.yHeadRot);
                    Vec3 p = e.getEyePosition().add(look.scale(e.getBbWidth() * 0.5 + 0.1)).add(0, -0.12, 0);
                    for (int i = 0; i < 3; i++) {
                        level.addParticle(ParticleTypes.POOF, p.x, p.y, p.z, look.x * 0.03 + rnd.nextGaussian() * 0.005,
                                0.01, look.z * 0.03 + rnd.nextGaussian() * 0.005);
                    }
                }
            }
        }
    }

    private static boolean breathes(LivingEntity e) {
        return e.getMobType() != MobType.UNDEAD && !(e instanceof AbstractGolem) && !(e instanceof Blaze)
                && !(e instanceof EnderMan) && !(e instanceof Vex) && !(e instanceof Slime) && !(e instanceof WaterAnimal);
    }

    /** What an entity "is made of" when it gets hit. */
    private static ParticleOptions material(LivingEntity e) {
        boolean blood = LMConfig.on(LMConfig.FX_BLOOD);
        if (e instanceof Blaze || e instanceof MagmaCube) return ParticleTypes.FLAME;
        if (e instanceof EnderMan || e instanceof Endermite || e instanceof Shulker || e instanceof EnderDragon) return ParticleTypes.PORTAL;
        if (e instanceof Vex || e instanceof Phantom || e instanceof Ghast) return ParticleTypes.ENCHANTED_HIT;
        if (e instanceof IronGolem) return block(Blocks.IRON_BLOCK.defaultBlockState());
        if (e instanceof SnowGolem) return block(Blocks.SNOW_BLOCK.defaultBlockState());
        if (e instanceof Slime) return ParticleTypes.ITEM_SLIME;
        if (e instanceof Creeper) return block(Blocks.LIME_CONCRETE.defaultBlockState());
        if (e instanceof AbstractSkeleton || e instanceof WitherBoss) return block(Blocks.BONE_BLOCK.defaultBlockState());
        if (e.getMobType() == MobType.ARTHROPOD) return blood ? block(Blocks.MOSS_BLOCK.defaultBlockState()) : null;
        if (e.getMobType() == MobType.UNDEAD) return blood ? block(Blocks.NETHER_WART_BLOCK.defaultBlockState()) : null;
        if (e instanceof WaterAnimal) return ParticleTypes.BUBBLE;
        return blood ? block(Blocks.REDSTONE_BLOCK.defaultBlockState()) : null;
    }

    private static ParticleOptions block(BlockState s) {
        return new BlockParticleOption(ParticleTypes.BLOCK, s);
    }

    private static void impact(ClientLevel level, LivingEntity e, RandomSource rnd, int count) {
        double cx = e.getX(), cy = e.getY() + e.getBbHeight() * 0.6, cz = e.getZ();
        double w = e.getBbWidth() * 0.5;
        ParticleOptions mat = material(e);
        for (int i = 0; i < count; i++) {
            double vx = rnd.nextGaussian() * 0.12, vy = 0.08 + rnd.nextDouble() * 0.18, vz = rnd.nextGaussian() * 0.12;
            if (mat != null) level.addParticle(mat, cx + rnd.nextGaussian() * w * 0.5, cy + rnd.nextGaussian() * 0.2,
                    cz + rnd.nextGaussian() * w * 0.5, vx, vy, vz);
            if (i % 2 == 0) level.addParticle(ParticleTypes.CRIT, cx, cy, cz, vx * 3, vy * 2, vz * 3);
        }
    }

    private static void ring(ClientLevel level, LivingEntity e, RandomSource rnd, int count) {
        BlockState under = e.getBlockStateOn();
        if (under.isAir() || under.getRenderShape() == RenderShape.INVISIBLE) return;
        ParticleOptions p = new BlockParticleOption(ParticleTypes.BLOCK, under).setPos(e.getOnPos());
        double rad = e.getBbWidth() * 0.6 + 0.2;
        for (int i = 0; i < count; i++) {
            double a = i / (double) count * Math.PI * 2;
            double c = Math.cos(a), s = Math.sin(a);
            level.addParticle(p, e.getX() + c * rad, e.getY() + 0.05, e.getZ() + s * rad, c * 0.25, 0.12 + rnd.nextDouble() * 0.1, s * 0.25);
        }
        for (int i = 0; i < count / 3; i++) {
            level.addParticle(ParticleTypes.POOF, e.getX() + rnd.nextGaussian() * rad, e.getY() + 0.1, e.getZ() + rnd.nextGaussian() * rad, 0, 0.02, 0);
        }
    }
}
