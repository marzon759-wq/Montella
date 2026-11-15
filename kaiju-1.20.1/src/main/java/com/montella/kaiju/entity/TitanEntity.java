package com.montella.kaiju.entity;

import com.montella.kaiju.rig.Skeleton;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared titan behaviour: heavy inertial physics, knockdowns that topple with real angular acceleration, getting up,
 * beams that clash in mid-air and fade out slowly, impact reactions, terrain destruction and camera shake.
 */
public abstract class TitanEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> BEAM_END = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> BEAM = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HIT_YAW = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GRAB = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AUX = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> CLASH = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.BOOLEAN);

    public static final byte EV_SHAKE_SMALL = 70, EV_SHAKE_BIG = 71, EV_SHAKE_HUGE = 72;

    public final Skeleton skel = new Skeleton();
    public int stateTime;
    /** Walk cycle, interpolated on the client. */
    public float walkPhase, prevWalkPhase, walkAmt, prevWalkAmt;
    /** Smoothed head aim relative to the body, radians. */
    public float lookYaw, lookPitch, prevLookYaw, prevLookPitch;
    /** Topple physics: 0 standing .. 1 lying on the ground. */
    public float fall, prevFall, fallVel;
    /** Recoil after a hit, decays. */
    public float recoil, prevRecoil;
    public float bodyTwist, prevBodyTwist;

    // ---- server only ----
    protected double moveX, moveZ, moveY;
    protected boolean wantMove;
    protected float moveSpeed;
    protected int beamCooldown = 120, attackCooldown = 40, roarCooldown = 0, actionTicks;
    protected Vec3 beamDir = null;
    protected int beamHitTicks;   // how long this titan is being hit by a beam
    protected float clashRatio = -1f;
    protected int clashTicks;
    protected TitanEntity clashWith;
    protected float lastFallVelocity;
    protected boolean wasAirborne;
    private int stuckTicks;
    protected int pendingState = -1, pendingDelay;

    protected TitanEntity(EntityType<? extends TitanEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(3.1f);
        this.noCulling = true;
        this.xpReward = 500;
        this.setPersistenceRequired();
        buildSkeleton();
    }

    protected abstract void buildSkeleton();

    /** Poses {@link #skel} for this entity at the given partial tick and computes world matrices. */
    public abstract void pose(float partial);

    public abstract float mass();

    protected abstract void titanAi(LivingEntity target, double gap);

    protected abstract List<Vec3> beamOrigins();

    protected abstract float beamBasePower();

    protected abstract float beamDamage();

    protected abstract SoundEvent beamSound();

    public abstract float radius();

    public abstract boolean isEnemyTitan(Entity e);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(STATE, S.IDLE);
        entityData.define(BEAM_END, new Vector3f());
        entityData.define(BEAM, 0f);
        entityData.define(HIT_YAW, 0f);
        entityData.define(TARGET, -1);
        entityData.define(GRAB, -1);
        entityData.define(AUX, 0);
        entityData.define(CLASH, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) stateTime = 0;
        if (key == DATA_POSE) refreshDimensions();
    }

    // ------------------------------------------------------------------ accessors
    public int state() { return entityData.get(STATE); }

    public void setState(int s) {
        if (s != state()) { entityData.set(STATE, s); stateTime = 0; actionTicks = 0; }
        else stateTime = 0;
        boolean lying = S.down(s) && s != S.GET_UP;
        Pose want = lying ? Pose.SWIMMING : Pose.STANDING;
        if (getPose() != want) setPose(want);
    }

    public Vec3 beamEnd() { Vector3f v = entityData.get(BEAM_END); return new Vec3(v.x, v.y, v.z); }

    public float beamPower() { return entityData.get(BEAM); }

    public boolean clashing() { return entityData.get(CLASH); }

    public float hitYaw() { return entityData.get(HIT_YAW); }

    public int aux() { return entityData.get(AUX); }

    protected void setAux(int v) { entityData.set(AUX, v); }

    public Entity grabbed() { int id = entityData.get(GRAB); return id < 0 ? null : level().getEntity(id); }

    protected void setGrabbed(Entity e) { entityData.set(GRAB, e == null ? -1 : e.getId()); }

    public Entity titanTarget() { int id = entityData.get(TARGET); return id < 0 ? null : level().getEntity(id); }

    public float stateTime(float partial) { return stateTime + partial; }

    public boolean isFiring() { return state() == S.FIRE_BEAM && beamPower() > 0.5f; }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        EntityDimensions d = super.getDimensions(pose);
        if (pose == Pose.SWIMMING) return EntityDimensions.scalable(d.width * 1.5f, d.height * 0.34f);
        return d;
    }

    // ------------------------------------------------------------------ transforms
    /** Model space (blocks, +Z forward) to world. */
    public Vec3 toWorld(Vector3f m) { return toWorld(m, getX(), getY(), getZ(), yBodyRot); }

    public static Vec3 toWorld(Vector3f m, double x, double y, double z, float yawDeg) {
        float a = -yawDeg * Mth.DEG_TO_RAD;
        float c = Mth.cos(a), s = Mth.sin(a);
        return new Vec3(x + m.x * c + m.z * s, y + m.y, z - m.x * s + m.z * c);
    }

    public Vec3 jointWorld(String joint, float lx, float ly, float lz) {
        return toWorld(skel.get(joint).point(lx, ly, lz, new Vector3f()));
    }

    /** Applies the shared whole-body motion (topple, recoil lean, lift) to the skeleton root. */
    protected void poseRoot(float partial, float liftWhenLying) {
        float f = Mth.lerp(partial, prevFall, fall);
        float yawRel = (hitYaw() - Mth.rotLerp(partial, yBodyRotO, yBodyRot)) * Mth.DEG_TO_RAD;
        // direction the body is pushed towards, in model space
        float dx = -Mth.sin(-yawRel), dz = Mth.cos(-yawRel);
        dx = Mth.sin(yawRel) * -1f; dz = Mth.cos(yawRel);
        skel.rootMatrix.identity();
        float ease = f;
        skel.rootMatrix.translate(0, liftWhenLying * Mth.sin(ease * Mth.HALF_PI), 0);
        float r = Mth.lerp(partial, prevRecoil, recoil);
        float ang = ease * 1.48f + r * 0.32f;
        if (ang != 0) skel.rootMatrix.rotate(ang, dz, 0, -dx);
    }

    // ------------------------------------------------------------------ ticking
    @Override
    public void tick() {
        prevWalkPhase = walkPhase; prevWalkAmt = walkAmt;
        prevLookYaw = lookYaw; prevLookPitch = lookPitch;
        prevFall = fall; prevRecoil = recoil; prevBodyTwist = bodyTwist;
        super.tick();
        stateTime++;
        updateAnimationState();
        if (!level().isClientSide) serverTick();
    }

    @Override
    protected float tickHeadTurn(float yRot, float amount) {
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        return amount;
    }

    protected void updateAnimationState() {
        double dx = getX() - xo, dz = getZ() - zo;
        float speed = (float) Math.sqrt(dx * dx + dz * dz);
        int st = state();
        boolean walking = !S.down(st) && st != S.HELD && st != S.PIN && st != S.FLY && st != S.LIFT && onGround();
        float target = walking ? Mth.clamp(speed / 0.35f, 0, 1.6f) : 0;
        walkAmt += (target - walkAmt) * 0.15f;
        walkPhase += Math.min(speed, 0.9f) * (st == S.RAM ? 0.55f : 0.42f);

        // topple: accelerate like a falling column, bounce once on the ground
        float fallTarget = switch (st) {
            case S.FLUNG -> 0.75f;
            case S.FALLEN, S.PINNED, S.DEATH -> 1f;
            case S.GET_UP -> 1f - smooth(Mth.clamp((stateTime - 6) / 34f, 0, 1));
            default -> 0f;
        };
        if (st == S.GET_UP || fallTarget < fall - 0.001f) {
            fallVel = 0;
            fall += (fallTarget - fall) * (st == S.GET_UP ? 0.45f : 0.12f);
        } else if (fallTarget > fall) {
            fallVel += 0.010f + fall * 0.03f;
            fall += fallVel;
            if (fall >= fallTarget) {
                fall = fallTarget;
                if (fallVel > 0.05f && fallTarget >= 1f) onToppleImpact(fallVel);
                fallVel = -fallVel * 0.22f;
            }
        } else if (fallVel != 0) {
            fall += fallVel; fallVel += 0.03f;
            if (fall >= fallTarget) { fall = fallTarget; fallVel = 0; }
        }
        fall = Mth.clamp(fall, 0, 1);
        recoil *= 0.86f;
        if (st == S.STAGGER && stateTime < 4) recoil = Math.min(1f, recoil + 0.35f);

        Entity t = titanTarget();
        float wantYaw = 0, wantPitch = 0;
        if (t != null && !S.down(st)) {
            Vec3 eye = position().add(0, getBbHeight() * 0.85, 0);
            Vec3 d = t.position().add(0, t.getBbHeight() * 0.6, 0).subtract(eye);
            float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
            wantYaw = Mth.clamp(Mth.wrapDegrees(yaw - yBodyRot), -70, 70) * Mth.DEG_TO_RAD;
            wantPitch = Mth.clamp((float) -Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)), -0.9f, 0.9f);
        }
        lookYaw += (wantYaw - lookYaw) * 0.12f;
        lookPitch += (wantPitch - lookPitch) * 0.12f;
    }

    protected static float smooth(float x) { return x * x * (3 - 2 * x); }

    /** Called on both sides when a falling body hits the ground. */
    protected void onToppleImpact(float speed) {
        if (level().isClientSide) return;
        Vec3 dir = Vec3.directionFromRotation(0, hitYaw());
        Vec3 p = position().add(dir.scale(getBbHeight() * 0.5));
        ServerLevel sl = (ServerLevel) level();
        playSound(SoundEvents.GENERIC_EXPLODE, 6f, 0.45f);
        playSound(SoundEvents.ANVIL_LAND, 6f, 0.35f);
        level().broadcastEntityEvent(this, EV_SHAKE_HUGE);
        dust(sl, p, getBbHeight() * 0.5, 160);
        if (griefing()) crater(BlockPos.containing(p), 4);
        for (Entity e : level().getEntities(this, new AABB(p, p).inflate(getBbHeight() * 0.5, 4, getBbHeight() * 0.5))) {
            if (e instanceof TitanEntity) continue;
            fling(e, e.position().subtract(p).normalize().add(0, 0.6, 0).scale(1.2), 8f);
        }
        hurt(damageSources().fall(), 20f);
    }

    // ------------------------------------------------------------------ server logic
    protected void serverTick() {
        pose(1f);
        int st = state();
        if (beamCooldown > 0) beamCooldown--;
        if (attackCooldown > 0) attackCooldown--;
        if (roarCooldown > 0) roarCooldown--;
        actionTicks++;
        wantMove = false;
        moveY = 0;

        if (pendingState >= 0 && --pendingDelay <= 0) { int p = pendingState; pendingState = -1; setState(p); }

        if (tickCount % 10 == 0) acquireTarget();
        LivingEntity target = getTarget();
        entityData.set(TARGET, target == null ? -1 : target.getId());

        // landing after being thrown
        boolean air = !onGround();
        if (wasAirborne && !air && st == S.FLUNG) {
            setState(S.FALLEN);
            playSound(SoundEvents.GENERIC_EXPLODE, 5f, 0.5f);
        }
        if (air) lastFallVelocity = (float) getDeltaMovement().y;
        wasAirborne = air;

        tickBeam();
        separateTitans();

        switch (st) {
            case S.STAGGER -> { if (stateTime > 22) setState(S.IDLE); }
            case S.CLASH_RECOIL -> { if (stateTime > 30) setState(S.IDLE); }
            case S.FLUNG -> { if (stateTime > 120) setState(S.FALLEN); }
            case S.FALLEN -> { if (stateTime > 60 + (getId() % 3) * 10 && getHealth() > 0) setState(S.GET_UP); }
            case S.PINNED -> { if (stateTime > 400) setState(S.GET_UP); }
            case S.GET_UP -> {
                if (stateTime == 8) playSound(SoundEvents.RAVAGER_STEP, 5f, 0.4f);
                if (stateTime > 42) { setState(S.IDLE); if (roarCooldown <= 0 && random.nextFloat() < 0.6f) startRoar(); }
            }
            case S.HELD -> {
                Entity g = holder();
                if (g == null || stateTime > 200) setState(S.STAGGER);
            }
            case S.ROAR -> { tickRoar(); if (stateTime > roarLength()) setState(S.IDLE); }
            case S.IDLE -> {
                if (target != null && target.isAlive()) {
                    double gap = gapTo(target);
                    titanAi(target, gap);
                } else {
                    wander();
                }
            }
            default -> {}
        }
        tickState(target);
        if (wantMove) breakObstacles();
        if (getHealth() < getMaxHealth() && tickCount % 20 == 0 && !S.down(st)) heal(regenPerSecond());
    }

    protected float regenPerSecond() { return 1f; }

    /** Per-state behaviour handled by subclasses (attacks). */
    protected abstract void tickState(LivingEntity target);

    protected abstract int roarLength();

    protected abstract void tickRoar();

    public void startRoar() { setState(S.ROAR); roarCooldown = 600; }

    protected Entity holder() {
        AABB box = getBoundingBox().inflate(40);
        for (TitanEntity t : level().getEntitiesOfClass(TitanEntity.class, box)) {
            if (t != this && t.grabbed() == this) return t;
        }
        return null;
    }

    protected void acquireTarget() {
        LivingEntity cur = getTarget();
        if (cur != null && (!cur.isAlive() || cur.distanceTo(this) > 200 || (cur instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            setTarget(null); cur = null;
        }
        TitanEntity best = null; double bd = 200 * 200;
        for (TitanEntity t : level().getEntitiesOfClass(TitanEntity.class, getBoundingBox().inflate(200))) {
            if (t == this || !t.isAlive() || !isEnemyTitan(t)) continue;
            double d = t.distanceToSqr(this);
            if (d < bd) { bd = d; best = t; }
        }
        if (best != null) { setTarget(best); return; }
        if (cur == null) {
            LivingEntity attacker = getLastHurtByMob();
            if (attacker != null && attacker.isAlive() && !(attacker instanceof Player p && (p.isCreative() || p.isSpectator()))) setTarget(attacker);
            else if (huntsPlayers()) {
                Player p = level().getNearestPlayer(this, 64);
                if (p != null && !p.isCreative() && !p.isSpectator()) setTarget(p);
            }
        }
    }

    protected boolean huntsPlayers() { return false; }

    protected void wander() {
        if (tickCount % 400 == 0 && random.nextFloat() < 0.3f && roarCooldown <= 0) { startRoar(); return; }
        if (tickCount % 200 < 80 && (tickCount / 200) % 3 == 0) {
            float yaw = getYRot() + (float) Math.sin(tickCount * 0.01) * 0.8f;
            Vec3 f = Vec3.directionFromRotation(0, yaw);
            moveTowards(position().add(f.scale(20)), 0.12f, 2f);
        }
    }

    public double gapTo(Entity e) {
        double dx = e.getX() - getX(), dz = e.getZ() - getZ();
        double r2 = e instanceof TitanEntity t ? t.radius() : e.getBbWidth() / 2;
        return Math.sqrt(dx * dx + dz * dz) - radius() - r2;
    }

    public float yawTo(Vec3 p) {
        return (float) (Mth.atan2(p.z - getZ(), p.x - getX()) * Mth.RAD_TO_DEG) - 90f;
    }

    /** Turns the whole body; heavy titans turn slowly. Returns the remaining angle. */
    public float turnTo(float yaw, float maxDeg) {
        float d = Mth.wrapDegrees(yaw - getYRot());
        float step = Mth.clamp(d, -maxDeg, maxDeg);
        setYRot(getYRot() + step);
        yBodyRot = getYRot(); yHeadRot = getYRot();
        return d - step;
    }

    public void moveTowards(Vec3 p, float speed, float turn) {
        float rem = turnTo(yawTo(p), turn);
        if (Math.abs(rem) < 50) {
            Vec3 f = Vec3.directionFromRotation(0, getYRot());
            moveX = f.x * speed; moveZ = f.z * speed;
            wantMove = true;
        }
    }

    protected boolean canWalk() {
        int s = state();
        return s == S.IDLE || s == S.RAM || s == S.ROAR || s == S.FLY;
    }

    @Override
    public void travel(Vec3 in) {
        if (!isEffectiveAi()) { super.travel(in); return; }
        int st = state();
        if (st == S.HELD || st == S.PIN || st == S.LIFT && isLiftedVictim()) return;
        Vec3 v = getDeltaMovement();
        double vx = v.x, vy = v.y, vz = v.z;
        boolean flying = isFlying();
        if (wantMove && canWalk()) {
            double acc = st == S.RAM ? 0.12 : 0.07;
            vx += (moveX - vx) * acc; vz += (moveZ - vz) * acc;
        } else if (onGround()) {
            double fr = S.down(st) || st == S.STAGGER || st == S.CLASH_RECOIL ? 0.86 : 0.6;
            vx *= fr; vz *= fr;
        } else {
            vx *= 0.985; vz *= 0.985;
        }
        if (flying) {
            vy += (moveY - vy) * 0.1;
            vx *= 0.99; vz *= 0.99;
        } else {
            vy -= 0.09;
            if (isInWater()) { vy *= 0.8; vx *= 0.95; vz *= 0.95; }
        }
        setDeltaMovement(vx, vy, vz);
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision && wantMove) stuckTicks++; else stuckTicks = 0;
        if (onGround() && getDeltaMovement().y < 0) setDeltaMovement(getDeltaMovement().multiply(1, 0, 1));
    }

    protected boolean isLiftedVictim() { return false; }

    public boolean isFlying() { int s = state(); return s == S.FLY || s == S.TAKEOFF || s == S.LIFT || s == S.LAND; }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) { return false; }

    @Override
    public boolean ignoreExplosion() { return true; }

    @Override
    public boolean canBreatheUnderwater() { return true; }

    @Override
    public boolean fireImmune() { return true; }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void pushEntities() {}

    @Override
    public boolean canBeCollidedWith() { return false; }

    @Override
    public AABB getBoundingBoxForCulling() { return getBoundingBox().inflate(48, 24, 48); }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 512 * 512; }

    // ------------------------------------------------------------------ impacts & physics
    /**
     * Receive a physical hit. Impulse is in blocks/tick for a mass of 1. Power decides the reaction:
     * small flinch, stagger, knockdown or being sent flying.
     */
    public void receiveImpact(Vec3 impulse, float power, float damage, Entity from) {
        int st = state();
        if (st == S.DEATH || isRemoved()) return;
        Vec3 dv = impulse.scale(1.0 / mass());
        if (st == S.HELD || st == S.PINNED) dv = dv.scale(0.3);
        setDeltaMovement(getDeltaMovement().add(dv));
        hurtMarked = true;
        float yaw = (float) (Mth.atan2(impulse.z, impulse.x) * Mth.RAD_TO_DEG) - 90f;
        if (impulse.horizontalDistanceSqr() > 1e-4) entityData.set(HIT_YAW, yaw);
        if (damage > 0) hurt(from instanceof LivingEntity le ? damageSources().mobAttack(le) : damageSources().generic(), damage);
        if (!isAlive()) return;
        float p = power / mass();
        if (S.down(st) && st != S.GET_UP) {
            level().broadcastEntityEvent(this, EV_SHAKE_SMALL);
            return;
        }
        if (st == S.HELD || st == S.PIN || st == S.LIFT) {
            if (p >= 1.2f) { releaseGrabs(); goDown(dv.y > 0.25); }
            return;
        }
        if (p >= 1.0f) {
            onInterrupted();
            goDown(dv.y > 0.25 || p > 1.8f);
        } else if (p >= 0.35f) {
            onInterrupted();
            setState(S.STAGGER);
            recoil = Math.min(1, recoil + p);
            level().broadcastEntityEvent(this, EV_SHAKE_SMALL);
            playSound(hurtRoar(), 5f, 0.9f + random.nextFloat() * 0.2f);
        } else {
            recoil = Math.min(1, recoil + p);
        }
    }

    protected SoundEvent hurtRoar() { return SoundEvents.RAVAGER_HURT; }

    public void goDown(boolean airborne) {
        releaseGrabs();
        if (airborne && onGround()) setDeltaMovement(getDeltaMovement().add(0, 0.55, 0));
        setState(airborne ? S.FLUNG : S.FALLEN);
        playSound(hurtRoar(), 6f, 0.7f);
        level().broadcastEntityEvent(this, EV_SHAKE_BIG);
    }

    /** Stop beams / grabs when hit hard. */
    protected void onInterrupted() {
        if (state() == S.FIRE_BEAM || state() == S.CHARGE_BEAM) { setState(S.BEAM_END); beamCooldown = Math.max(beamCooldown, 120); }
        releaseGrabs();
    }

    protected void releaseGrabs() {
        Entity g = grabbed();
        if (g instanceof TitanEntity t && (t.state() == S.HELD || t.state() == S.PINNED || t.state() == S.LIFT)) t.setState(S.STAGGER);
        setGrabbed(null);
    }

    /** Applies a push to any entity: titans react physically, small things are flung. */
    public void fling(Entity e, Vec3 impulse, float damage) {
        if (e instanceof TitanEntity t) { t.receiveImpact(impulse, (float) impulse.length(), damage, this); return; }
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) {
            if (!p.isSpectator()) { p.setDeltaMovement(p.getDeltaMovement().add(impulse.scale(0.6))); p.hurtMarked = true; }
            return;
        }
        if (e instanceof LivingEntity le) {
            le.hurt(damageSources().mobAttack(this), damage);
            le.setDeltaMovement(le.getDeltaMovement().add(impulse));
            le.hurtMarked = true;
        } else if (e instanceof net.minecraft.world.entity.item.ItemEntity || e instanceof net.minecraft.world.entity.vehicle.AbstractMinecart || e instanceof net.minecraft.world.entity.vehicle.Boat) {
            e.setDeltaMovement(e.getDeltaMovement().add(impulse));
        }
    }

    /** Titans never pass through each other: push overlapping bodies apart. */
    protected void separateTitans() {
        int st = state();
        if (st == S.HELD || st == S.PIN || st == S.LIFT || st == S.PINNED) return;
        for (TitanEntity t : level().getEntitiesOfClass(TitanEntity.class, getBoundingBox().inflate(2))) {
            if (t == this) continue;
            int ts = t.state();
            if (ts == S.PIN || ts == S.HELD || ts == S.LIFT || grabbed() == t || t.grabbed() == this) continue;
            double dx = getX() - t.getX(), dz = getZ() - t.getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            double overlap = radius() + t.radius() - d;
            if (overlap > 0 && d > 1e-3) {
                double share = t.mass() / (mass() + t.mass());
                double push = Math.min(overlap, 3) * 0.25 * share;
                setDeltaMovement(getDeltaMovement().add(dx / d * push, 0, dz / d * push));
                // a running titan crashing into another one
                if (st == S.RAM && stateTime > 6) onRamContact(t);
            }
        }
    }

    protected void onRamContact(TitanEntity other) {}

    // ------------------------------------------------------------------ beams
    protected int chargeTime() { return 34; }

    protected int fireTime() { return 70; }

    public void startBeam(boolean quick) {
        setState(S.CHARGE_BEAM);
        stateTime = quick ? chargeTime() - 10 : 0;
        beamCooldown = 260 + random.nextInt(120);
        beamDir = null;
    }

    protected void tickBeam() {
        int st = state();
        float power = beamPower();
        if (st == S.CHARGE_BEAM) {
            if (stateTime == 1) playSound(SoundEvents.BEACON_ACTIVATE, 6f, 0.5f);
            if (stateTime % 6 == 0) playSound(SoundEvents.BEACON_AMBIENT, 4f, 0.6f + stateTime * 0.02f);
            LivingEntity t = getTarget();
            if (t != null) turnTo(yawTo(t.position()), 3f);
            if (stateTime >= chargeTime()) {
                setState(S.FIRE_BEAM);
                playSound(beamSound(), 8f, 0.5f);
                playSound(SoundEvents.GENERIC_EXPLODE, 4f, 1.4f);
            }
            entityData.set(BEAM, 0f);
            return;
        }
        if (st == S.FIRE_BEAM) {
            entityData.set(BEAM, Math.min(1f, power + 0.25f));
            LivingEntity t = getTarget();
            List<Vec3> origins = beamOrigins();
            Vec3 o = origins.get(0);
            Vec3 aim = t != null ? t.position().add(0, t.getBbHeight() * 0.62, 0) : o.add(Vec3.directionFromRotation(10, getYRot()).scale(60));
            Vec3 want = aim.subtract(o).normalize();
            if (beamDir == null) beamDir = want;
            else beamDir = beamDir.add(want.subtract(beamDir).scale(0.12)).normalize();
            if (t != null) turnTo(yawTo(t.position()), 2.2f);
            if (stateTime % 20 == 0) playSound(beamSound(), 7f, 0.55f + random.nextFloat() * 0.1f);

            boolean clash = handleClash(o);
            entityData.set(CLASH, clash);
            if (!clash) {
                Vec3 end = o.add(beamDir.scale(110));
                BlockHitResult bh = level().clip(new ClipContext(o, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
                if (bh.getType() != HitResult.Type.MISS) end = bh.getLocation();
                Entity hit = null; double best = Double.MAX_VALUE; Vec3 hitPos = null;
                for (Entity e : level().getEntities(this, new AABB(o, end).inflate(3), e -> e.isAlive() && !(e instanceof net.minecraft.world.entity.item.ItemEntity))) {
                    AABB bb = e.getBoundingBox().inflate(e instanceof TitanEntity ? 0.5 : 1.0);
                    var opt = bb.clip(o, end);
                    if (opt.isPresent()) {
                        double d = opt.get().distanceToSqr(o);
                        if (d < best) { best = d; hit = e; hitPos = opt.get(); }
                    } else if (bb.contains(o)) { hit = e; hitPos = o; best = 0; }
                }
                if (hit != null) {
                    end = hitPos;
                    onBeamHit(hit, hitPos);
                } else if (bh.getType() != HitResult.Type.MISS && stateTime % 4 == 0 && griefing()) {
                    level().explode(this, end.x, end.y, end.z, 3.2f, Level.ExplosionInteraction.MOB);
                } else if (bh.getType() != HitResult.Type.MISS && stateTime % 4 == 0) {
                    level().explode(this, end.x, end.y, end.z, 2.0f, Level.ExplosionInteraction.NONE);
                }
                entityData.set(BEAM_END, new Vector3f((float) end.x, (float) end.y, (float) end.z));
            }
            int max = clash ? fireTime() + 90 : fireTime();
            if (stateTime > max) { setState(S.BEAM_END); entityData.set(CLASH, false); }
            return;
        }
        entityData.set(CLASH, false);
        if (power > 0) entityData.set(BEAM, Math.max(0f, power - 0.045f));
        if (st == S.BEAM_END && stateTime > 26) setState(S.IDLE);
    }

    protected void onBeamHit(Entity hit, Vec3 at) {
        if (hit instanceof TitanEntity t) {
            t.beamHitTicks++;
            t.hurt(damageSources().mobAttack(this), beamDamage());
            Vec3 push = beamDir.multiply(1, 0.2, 1).scale(0.035);
            t.setDeltaMovement(t.getDeltaMovement().add(push.scale(1 / t.mass())));
            t.hurtMarked = true;
            t.onBeamStruck(this, at);
            if (t.beamHitTicks > 32) {
                t.beamHitTicks = 0;
                t.receiveImpact(beamDir.multiply(1, 0, 1).normalize().scale(1.0).add(0, 0.35, 0), 1.6f, 25f, this);
            }
            if (stateTime % 3 == 0) ((ServerLevel) level()).sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 2, 1.5, 1.5, 1.5, 0);
        } else {
            fling(hit, beamDir.scale(1.5).add(0, 0.5, 0), 14f);
            hit.setSecondsOnFire(5);
        }
    }

    /** React to being hit by a beam: stagger, and sometimes fire back so the beams meet. */
    protected void onBeamStruck(TitanEntity from, Vec3 at) {
        int st = state();
        if (st == S.IDLE || st == S.ROAR) {
            if (beamCooldown < 80 && random.nextFloat() < 0.55f) { setTarget(from); startBeam(true); }
            else { setState(S.STAGGER); recoil = Math.min(1, recoil + 0.5f); }
        } else if (st == S.STAGGER && stateTime > 12) stateTime = 4;
    }

    /** Beam struggle: the stronger beam pushes the meeting point back into the weaker titan's face. */
    private boolean handleClash(Vec3 o) {
        TitanEntity other = null;
        for (TitanEntity t : level().getEntitiesOfClass(TitanEntity.class, getBoundingBox().inflate(130))) {
            if (t != this && t.state() == S.FIRE_BEAM && t.beamDir != null) { other = t; break; }
        }
        if (other == null) { clashRatio = -1; clashWith = null; return false; }
        Vec3 o2 = other.beamOrigins().get(0);
        Vec3 ab = o2.subtract(o);
        double len = ab.length();
        if (len < 4) return false;
        Vec3 n = ab.scale(1 / len);
        boolean facing = beamDir.dot(n) > 0.86 && other.beamDir.dot(n.scale(-1)) > 0.86;
        if (!facing) { if (clashWith == other) { clashRatio = -1; clashWith = null; } return false; }
        // the titan with the lower id drives the struggle, the other mirrors it
        if (getId() > other.getId()) {
            if (other.clashWith == this && other.clashRatio >= 0) {
                clashWith = other; clashRatio = 1 - other.clashRatio;
                Vec3 p = o.add(ab.scale(clashRatio));
                entityData.set(BEAM_END, new Vector3f((float) p.x, (float) p.y, (float) p.z));
                return true;
            }
            return false;
        }
        if (clashWith != other || clashRatio < 0) { clashWith = other; clashRatio = 0.5f; clashTicks = 0; }
        clashTicks++;
        float pa = struggleStrength(clashTicks, clashRatio), pb = other.struggleStrength(clashTicks, 1 - clashRatio);
        clashRatio += (pa - pb) * 0.014f;
        clashRatio = Mth.clamp(clashRatio, 0.02f, 0.98f);
        Vec3 p = o.add(ab.scale(clashRatio));
        entityData.set(BEAM_END, new Vector3f((float) p.x, (float) p.y, (float) p.z));
        ServerLevel sl = (ServerLevel) level();
        sl.sendParticles(ParticleTypes.FLASH, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 30, 2.5, 2.5, 2.5, 0.8);
        sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 12, 1, 1, 1, 0.6);
        if (clashTicks % 10 == 0) { playSound(SoundEvents.GENERIC_EXPLODE, 6f, 0.7f + random.nextFloat() * 0.3f); level().broadcastEntityEvent(this, EV_SHAKE_SMALL); }
        if (clashTicks % 5 == 0 && griefing()) {
            BlockPos below = BlockPos.containing(p.x, p.y, p.z);
            for (int i = 0; i < 24; i++) { below = below.below(); if (!level().getBlockState(below).isAir()) break; }
            if (p.y - below.getY() < 12) level().explode(this, p.x, below.getY() + 1, p.z, 2.5f, Level.ExplosionInteraction.MOB);
        }
        TitanEntity loser = clashRatio >= 0.9f ? other : clashRatio <= 0.1f ? this : null;
        if (loser == null && clashTicks > 150) {
            // stalemate: the energy detonates between them
            level().explode(this, p.x, p.y, p.z, 6f, griefing() ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
            for (TitanEntity t : new TitanEntity[]{this, other}) {
                Vec3 away = t.position().subtract(p).multiply(1, 0, 1).normalize();
                t.setState(S.BEAM_END);
                t.receiveImpact(away.scale(0.9).add(0, 0.2, 0), 0.9f, 20f, t == this ? other : this);
            }
            clashRatio = -1; clashWith = null;
            return true;
        }
        if (loser != null) {
            TitanEntity winner = loser == this ? other : this;
            Vec3 hitAt = loser.beamOrigins().get(0);
            level().explode(winner, hitAt.x, hitAt.y, hitAt.z, 5f, Level.ExplosionInteraction.NONE);
            loser.setState(S.BEAM_END);
            loser.beamCooldown = 200;
            Vec3 dir = loser.position().subtract(winner.position()).multiply(1, 0, 1).normalize();
            loser.receiveImpact(dir.scale(1.4).add(0, 0.45, 0), 2.2f, 60f, winner);
            winner.stateTime = Math.max(0, winner.fireTime() - 25);
            clashRatio = -1; clashWith = null;
            other.clashRatio = -1; other.clashWith = null;
            return false;
        }
        return true;
    }

    /** Strength in a beam struggle; fluctuates, health matters, and a titan pushed to its face surges. */
    protected float struggleStrength(int t, float myRatio) {
        float hp = 0.55f + 0.45f * getHealth() / getMaxHealth();
        float wave = 1f + 0.32f * Mth.sin(t * 0.11f + getId() * 1.7f) + 0.18f * Mth.sin(t * 0.37f + getId());
        float surge = myRatio < 0.3f && random.nextFloat() < 0.08f ? 0.9f : 0f;
        return beamBasePower() * hp * wave + surge + (random.nextFloat() - 0.5f) * 0.2f;
    }

    // ------------------------------------------------------------------ world interaction
    public boolean griefing() { return ForgeEventFactory.getMobGriefingEvent(level(), this); }

    /** Plows through whatever stands in its way above step height. */
    protected void breakObstacles() {
        if (!griefing() || tickCount % 2 != 0) return;
        Vec3 f = Vec3.directionFromRotation(0, getYRot());
        AABB bb = getBoundingBox().move(f.scale(1.5));
        int minY = Mth.floor(bb.minY + (stuckTicks > 10 ? 1 : maxUpStep() + 0.5));
        int count = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int y = minY; y <= Mth.floor(bb.maxY) && count < 80; y++)
            for (int x = Mth.floor(bb.minX); x <= Mth.floor(bb.maxX) && count < 80; x++)
                for (int z = Mth.floor(bb.minZ); z <= Mth.floor(bb.maxZ) && count < 80; z++) {
                    m.set(x, y, z);
                    BlockState bs = level().getBlockState(m);
                    if (bs.isAir() || bs.getDestroySpeed(level(), m) < 0 || !bs.getFluidState().isEmpty()) continue;
                    level().destroyBlock(m, random.nextFloat() < 0.04f, this);
                    count++;
                }
    }

    public void crater(BlockPos c, int r) {
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) for (int y = -1; y <= 2; y++) {
            if (x * x + z * z + y * y * 4 > r * r) continue;
            BlockPos p = c.offset(x, y, z);
            BlockState bs = level().getBlockState(p);
            if (bs.isAir() || bs.getDestroySpeed(level(), p) < 0 || bs.is(Blocks.BEDROCK)) continue;
            if (y >= 0 || random.nextFloat() < 0.5f) level().destroyBlock(p, false, this);
        }
    }

    /** Destroys blocks inside a sphere (tail sweeps, claws). */
    public void smash(Vec3 c, double r, int max) {
        if (!griefing()) return;
        int n = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int ri = Mth.ceil(r);
        for (int x = -ri; x <= ri && n < max; x++) for (int y = -ri; y <= ri && n < max; y++) for (int z = -ri; z <= ri && n < max; z++) {
            if (x * x + y * y + z * z > r * r) continue;
            m.set(c.x + x, c.y + y, c.z + z);
            BlockState bs = level().getBlockState(m);
            if (bs.isAir() || bs.getDestroySpeed(level(), m) < 0 || !bs.getFluidState().isEmpty()) continue;
            if (m.getY() <= getY() - 1) continue;
            level().destroyBlock(m, false, this);
            n++;
        }
    }

    public void dust(ServerLevel sl, Vec3 p, double spread, int n) {
        BlockState ground = level().getBlockState(BlockPos.containing(p.x, getY() - 0.5, p.z));
        if (ground.isAir()) ground = Blocks.DIRT.defaultBlockState();
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), p.x, getY() + 0.5, p.z, n, spread * 0.5, 0.6, spread * 0.5, 0.3);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.x, getY() + 1, p.z, n / 8, spread * 0.4, 0.5, spread * 0.4, 0.04);
        sl.sendParticles(ParticleTypes.EXPLOSION, p.x, getY() + 1, p.z, n / 20, spread * 0.4, 0.5, spread * 0.4, 0);
    }

    /** Entities around a point (used for tails, claws, jaws), excluding this one. */
    public List<Entity> entitiesNear(Vec3 p, double r) {
        List<Entity> out = new ArrayList<>();
        for (Entity e : level().getEntities(this, new AABB(p, p).inflate(r + 12))) {
            if (!e.isAlive() || e.isSpectator()) continue;
            AABB bb = e.getBoundingBox();
            double cx = Mth.clamp(p.x, bb.minX, bb.maxX), cy = Mth.clamp(p.y, bb.minY, bb.maxY), cz = Mth.clamp(p.z, bb.minZ, bb.maxZ);
            double d = (cx - p.x) * (cx - p.x) + (cy - p.y) * (cy - p.y) + (cz - p.z) * (cz - p.z);
            if (d <= r * r) out.add(e);
        }
        return out;
    }

    // ------------------------------------------------------------------ death
    @Override
    public void die(DamageSource src) {
        super.die(src);
        if (!level().isClientSide) { releaseGrabs(); setState(S.DEATH); entityData.set(BEAM, 0f); }
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= 160 && !level().isClientSide) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    // ------------------------------------------------------------------ client events
    @Override
    public void handleEntityEvent(byte id) {
        if (id == EV_SHAKE_SMALL || id == EV_SHAKE_BIG || id == EV_SHAKE_HUGE) {
            float s = id == EV_SHAKE_SMALL ? 0.4f : id == EV_SHAKE_BIG ? 0.9f : 1.6f;
            com.montella.kaiju.KaijuMod.PROXY.shake(position(), s);
            return;
        }
        super.handleEntityEvent(id);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BeamCooldown", beamCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        beamCooldown = tag.getInt("BeamCooldown");
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.is(net.minecraft.tags.DamageTypeTags.IS_FALL) && !(src.getEntity() == null)) return false;
        if (src.getEntity() == this) return false;
        if (src.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) || src.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return false;
        return super.hurt(src, amount);
    }

    @Override
    public void knockback(double strength, double x, double z) {}
}
