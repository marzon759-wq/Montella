package com.krolasyon.bloodsea.entity;

import com.krolasyon.bloodsea.compat.CreatureBase;
import com.krolasyon.bloodsea.registry.ModSounds;
import com.krolasyon.bloodsea.world.Regions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * What every creature of the Blood Sea shares: a state machine run on the server, movement that glides over the blood
 * (they do not swim, they are part of it), who is watching them, and the end of them when nobody is near.
 */
public abstract class SeaCreature extends CreatureBase {
    /** development: hold every creature still in the state it is given, to look at it */
    public static boolean FROZEN = false;
    /** driven by a cutscene: its own brain is off */
    public boolean scripted = false;
    protected int stateTicks;
    protected int life;

    protected SeaCreature(EntityType<? extends PathfinderMob> type, Level level, boolean floats) {
        super(type, level);
        this.xpReward = 0;
        if (floats) {
            this.noPhysics = true;
            this.setNoGravity(true);
        }
    }

    @Override protected void registerGoals() {}
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public void checkDespawn() {}
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override protected void doPush(Entity e) {}
    @Override public boolean fireImmune() { return true; }
    @Override public int getMaxAirSupply() { return 100000; }

    /** they cannot be killed; only /kill takes them out of the world */
    @Override
    public boolean isInvulnerableTo(DamageSource s) { return !s.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(s); }

    /** how far away it is still drawn (blocks) */
    protected double drawRange() { return 160; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < drawRange() * drawRange(); }

    public void go(int s) {
        if (Boolean.getBoolean("bloodsea.debugai")) com.krolasyon.bloodsea.BloodSea.LOG.info("[AI] {} {} -> {}", kind(), getState(), s);
        setState(s);
        stateTicks = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (FROZEN || scripted) {
            this.yBodyRot = getYRot();
            this.yHeadRot = getYRot();
            if (noPhysics) setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (!level().isClientSide) {
            stateTicks++;
            life++;
            if (noPhysics) setDeltaMovement(Vec3.ZERO);
            brain((ServerLevel) level());
            if (life % 40 == 0 && nearest(144) == null) discard();
        }
    }

    protected abstract void brain(ServerLevel lvl);

    /** the Watcher has seen p and every creature in the sea now knows where they are */
    public void alert(Player p) {}

    // ------------------------------------------------------------------ helpers
    protected ServerPlayer nearest(double r) {
        ServerPlayer best = null;
        double bd = r * r;
        for (ServerPlayer p : ((ServerLevel) level()).players()) {
            if (p.isSpectator() || !p.isAlive()) continue;
            double d = p.distanceToSqr(this);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    /** is p looking at it (cos of the angle off the centre of their view), with nothing in between */
    protected boolean watchedBy(Player p, double cos) {
        Vec3 to = position().add(0, getBbHeight() * 0.5, 0).subtract(p.getEyePosition());
        double len = to.length();
        if (len < 1e-3) return true;
        return to.scale(1 / len).dot(p.getViewVector(1f)) > cos && p.hasLineOfSight(this);
    }

    /** the top of the blood, or of the mud where there is land */
    protected double surfaceAt(double x, double z) {
        int floor = level().getHeight(Heightmap.Types.OCEAN_FLOOR, Mth.floor(x), Mth.floor(z));
        return Math.max(com.krolasyon.bloodsea.world.Waves.MEAN - 0.1, floor);
    }

    /** glide toward a point at a speed (blocks per tick), turning to face the way it goes; returns the distance left */
    protected double glide(double tx, double tz, double speed, double y) {
        double dx = tx - getX(), dz = tz - getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d > 1e-3) {
            double s = Math.min(speed, d);
            setPos(getX() + dx / d * s, y, getZ() + dz / d * s);
            turnTo((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f, 6f);
        } else setPos(getX(), y, getZ());
        return d;
    }

    protected void turnTo(float yaw, float maxStep) {
        float y = Mth.approachDegrees(getYRot(), yaw, maxStep);
        setYRot(y);
        this.yBodyRot = y;
        this.yRotO = y;
    }

    protected void face(Entity e, float maxStep) {
        turnTo((float) (Mth.atan2(e.getZ() - getZ(), e.getX() - getX()) * Mth.RAD_TO_DEG) - 90f, maxStep);
    }

    protected void lookAt(Entity e) { getLookControl().setLookAt(e, 40f, 40f); }

    protected void sound(String name, float vol, float pitch) {
        level().playSound(null, getX(), getY() + getBbHeight() * 0.6, getZ(), ModSounds.get(name), SoundSource.HOSTILE, vol, pitch);
    }

    protected void soundAt(Player p, String name, double ox, double oz, float vol, float pitch) {
        level().playSound(null, p.getX() + ox, p.getY(), p.getZ() + oz, ModSounds.get(name), SoundSource.HOSTILE, vol, pitch);
    }

    protected float rnd(float a, float b) { return a + random.nextFloat() * (b - a); }

    protected void strike(Player p, float dmg) { p.hurt(damageSources().mobAttack(this), dmg); }

    protected Vec3 horizontalFacing(Player p) {
        Vec3 v = p.getVehicle() != null ? p.getVehicle().getDeltaMovement() : p.getDeltaMovement();
        Vec3 h = new Vec3(v.x, 0, v.z);
        if (h.lengthSqr() < 0.0004) h = new Vec3(p.getViewVector(1f).x, 0, p.getViewVector(1f).z);
        return h.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : h.normalize();
    }

    protected static double speedOf(Player p) {
        Entity e = p.getVehicle() != null ? p.getVehicle() : p;
        Vec3 v = e.getDeltaMovement();
        return Math.sqrt(v.x * v.x + v.z * v.z);
    }

    protected BlockPos below() { return BlockPos.containing(getX(), getY() - 0.5, getZ()); }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putInt("SeaState", getState());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        setState(t.getInt("SeaState"));
    }
}
