package com.krolasyon.titans.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.ForgeEventFactory;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** A colossal creature driven by a small combat state machine; all animation is derived from the synced state. */
public abstract class TitanEntity extends Monster {
    public static final int IDLE = 0, ROAR = 1, TAIL = 2, BITE = 3, GRAB = 4, THROW = 5, HELD = 6, BREATH = 7, STAGGER = 8,
            FALL = 9, DOWN = 10, GETUP = 11, PIN = 12, COUNTER = 13, THROWN = 14, DEAD = 15, CHARGE = 16;
    public static final int DROP = 0, LEAN = 1, ROLL = 2, PITCH = 3, NECK_P = 4, NECK_Y = 5, JAW = 6, ARM = 7, TAIL_UP = 8, REACH = 9,
            CROUCH = 10, SHAKE = 11, TWIST = 12, FLAP = 13, KICK = 14, LIMP = 15, PIVOT = 16, GLOW = 17, NP = 18;
    public static final int BREATH_CHARGE = 36;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SIDE = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> TARGET = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PARTNER = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> BEAM = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Vector3f> BEAM_END = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Boolean> CLASH = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Vector3f> HEADS = SynchedEntityData.defineId(TitanEntity.class, EntityDataSerializers.VECTOR3);

    private static final boolean DEBUG = System.getProperty("titan.autotest") != null;

    public interface Shaker { void shake(double x, double y, double z, float amount); }
    public static Shaker SHAKER = (x, y, z, a) -> {};

    public final Rig rig;
    public final boolean hydra;
    private final float[] p = new float[NP], po = new float[NP], goal = new float[NP], out = new float[NP];
    private final float[] hist = new float[64];
    private int histN;
    private float yawAcc, lastYaw;
    private boolean histInit;
    public int stateTime;
    private int lastRaw, seq;
    public float walkPhase, walkPhaseO, walkAmt, walkAmtO, aimYaw, aimYawO, aimPitch, aimPitchO;
    public final List<Fx> fx = new ArrayList<>();
    public final Vector3f beamO = new Vector3f(), beamC = new Vector3f();
    private float beamPrev;
    private final TitanPart[] parts;

    // server only
    protected Vec3 kb = Vec3.ZERO;
    protected float poise, surge = 1, clashPos = 0.5f, tailOff, walk;
    protected int breathCd = 100, tailCd = 40, biteCd, roarCd = 20, chargeCd = 120, downTime, pinDur, breathEnd, clashT, tailPhase;
    protected boolean firing, wantPin;
    protected LivingEntity foe;
    protected TitanEntity partner;
    protected Vec3 aim = Vec3.ZERO;
    private final Vec3[] tailPrev = new Vec3[Rig.NTL + 1];

    protected TitanEntity(EntityType<? extends TitanEntity> type, Level level, boolean hydra) {
        super(type, level);
        this.hydra = hydra;
        rig = new Rig(hydra);
        noCulling = true;
        setMaxUpStep(1.6f);
        setPersistenceRequired();
        xpReward = 500;
        List<TitanPart> list = new ArrayList<>();
        for (int h = 0; h < rig.heads; h++) list.add(new TitanPart(this, TitanPart.HEAD, h, hydra ? 7f : 9.5f));
        if (hydra) for (int h = 0; h < 3; h++) list.add(new TitanPart(this, TitanPart.NECK, h, 5.5f));
        list.add(new TitanPart(this, TitanPart.TORSO, 4, 14f));
        list.add(new TitanPart(this, TitanPart.TORSO, 1, 16f));
        for (int i = 3; i <= 15; i += 3) list.add(new TitanPart(this, TitanPart.TAIL, i, Math.max(4.5f, rig.tailR[i] * 2)));
        parts = list.toArray(new TitanPart[0]);
        setId(ENTITY_COUNTER.getAndAdd(parts.length + 1) + 1);
    }

    public static AttributeSupplier.Builder attrs() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1024).add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.ARMOR, 12).add(Attributes.FOLLOW_RANGE, 400).add(Attributes.ATTACK_DAMAGE, 30);
    }

    // ---- per species ----
    public float bodyRadius() { return hydra ? 7 : 8; }
    protected float walkSpeed() { return hydra ? 0.42f : 0.36f; }
    protected float chargeSpeed() { return hydra ? 0.95f : 0.9f; }
    protected float biteRange() { return hydra ? 40 : 31; }
    public float headGrowth(int h, float pt) { return 1; }
    public boolean headActive(int h) { return true; }
    public int biteHead() { return 0; }
    public void damageHead(int h, float amount, TitanEntity from) {}
    public int randomHead() { return 0; }
    protected float beamFactor() { return 1; }

    // ---- synced data ----
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(STATE, 0);
        entityData.define(SIDE, 1);
        entityData.define(TARGET, 0);
        entityData.define(PARTNER, 0);
        entityData.define(BEAM, 0f);
        entityData.define(BEAM_END, new Vector3f());
        entityData.define(CLASH, false);
        entityData.define(HEADS, new Vector3f(1, 1, 1));
    }

    public int getState() { return entityData.get(STATE) & 255; }
    public int getSeq() { return entityData.get(STATE) >> 8; }
    public int getSide() { return entityData.get(SIDE); }
    public float getBeam() { return entityData.get(BEAM); }
    public Vector3f getBeamEnd() { return entityData.get(BEAM_END); }
    public boolean isClash() { return entityData.get(CLASH); }
    public int getPartnerId() { return entityData.get(PARTNER); }
    public Entity getTargetEntity() { int id = entityData.get(TARGET); return id == 0 ? null : level().getEntity(id); }
    protected void setSide(int s) { entityData.set(SIDE, s); }

    public void setState(int s) {
        int old = getState();
        seq = (seq + 1) & 0xFFFF;
        int raw = s | (seq << 8);
        entityData.set(STATE, raw);
        lastRaw = raw;
        stateTime = 0;
        if (s != BREATH) firing = false;
        if (s != TAIL) tailOff = 0;
        if (DEBUG) System.out.println("[TITAN] t=" + tickCount + " " + (hydra ? "A" : "K") + " " + old + "->" + s + " hp=" + (int) getHealth());
        onStateChanged(old, s);
    }

    protected void onStateChanged(int old, int s) {
        if (s == DOWN || s == DEAD) {
            float target = 90 * getSide();
            p[ROLL] = target + Mth.wrapDegrees(p[ROLL] - target);
            po[ROLL] = p[ROLL];
        }
        if (level().isClientSide) {
            if (s == DOWN) slamFx();
            if (s == ROAR) SHAKER.shake(getX(), getY(), getZ(), 0.4f);
        }
    }

    // ---- helpers ----
    public Vec3 toWorld(Vector3f m) {
        float y = getYRot() * Rig.DEG, c = Mth.cos(y), s = Mth.sin(y);
        return new Vec3(getX() + m.x * c - m.z * s, getY() + m.y, getZ() + m.x * s + m.z * c);
    }
    public Vec3 forward() { float y = getYRot() * Rig.DEG; return new Vec3(-Mth.sin(y), 0, Mth.cos(y)); }
    public Vec3 left() { float y = getYRot() * Rig.DEG; return new Vec3(Mth.cos(y), 0, Mth.sin(y)); }
    public Vec3 chest() { return position().add(0, getBbHeight() * 0.62, 0); }
    public boolean isDown() { int s = getState(); return s == DOWN || s == FALL || s == THROWN || s == DEAD || s == COUNTER; }

    public Vec3 mouthWorld() {
        Vector3f m = new Vector3f();
        int n = 0;
        for (int h = 0; h < rig.heads; h++) if (headActive(h)) { m.add(rig.mouth[h]); n++; }
        if (n == 0) return toWorld(rig.torso[Rig.NT]);
        return toWorld(m.div(n));
    }

    public float[] params(float pt) {
        for (int i = 0; i < NP; i++) out[i] = po[i] + (p[i] - po[i]) * pt;
        return out;
    }

    /** Unwrapped body yaw as it was {@code delay} ticks ago. */
    public float yawBack(float delay, float pt) {
        if (!histInit) return getYRot();
        float f = histN - 2 + pt - delay;
        int lo = Math.max(histN - hist.length, 0);
        if (f <= lo) return hist[lo % hist.length];
        int i = (int) Math.floor(f);
        if (i >= histN - 1) return hist[(histN - 1) % hist.length];
        float a = hist[i % hist.length], b = hist[(i + 1) % hist.length];
        return a + (b - a) * (f - i);
    }

    protected void turn(float deg) { setYRot(getYRot() + deg); }

    protected void face(Vec3 to, float max) {
        float want = (float) (Mth.atan2(-(to.x - getX()), to.z - getZ()) * 180 / Math.PI);
        turn(Mth.clamp(Mth.wrapDegrees(want - getYRot()), -max, max));
    }

    protected float angleTo(Vec3 to) {
        float want = (float) (Mth.atan2(-(to.x - getX()), to.z - getZ()) * 180 / Math.PI);
        return Math.abs(Mth.wrapDegrees(want - getYRot()));
    }

    protected void snd(SoundEvent e, float vol, float pitch) {
        level().playSound(null, getX(), getY() + 20, getZ(), e, SoundSource.HOSTILE, vol, pitch);
    }

    // ---- tick ----
    @Override
    public void tick() {
        stateTime++;
        if (!level().isClientSide) serverTick();
        else {
            int raw = entityData.get(STATE);
            if (raw != lastRaw) {
                int old = lastRaw & 255;
                lastRaw = raw;
                stateTime = 0;
                onStateChanged(old, raw & 255);
            }
        }
        super.tick();
        book();
        if (level().isClientSide) clientTick();
    }

    private void book() {
        float yr = getYRot();
        if (!histInit) { histInit = true; yawAcc = yr; lastYaw = yr; for (int i = 0; i < hist.length; i++) hist[i] = yr; histN = hist.length; }
        yawAcc += Mth.wrapDegrees(yr - lastYaw);
        lastYaw = yr;
        hist[histN % hist.length] = yawAcc;
        histN++;

        double dx = getX() - xo, dz = getZ() - zo;
        float sp = (float) Math.sqrt(dx * dx + dz * dz);
        int s = getState();
        boolean stepping = s == IDLE || s == CHARGE || s == STAGGER || s == BREATH || s == BITE || s == TAIL || s == ROAR;
        if (!stepping || !onGround()) sp = 0;
        walkAmtO = walkAmt;
        walkAmt += (Mth.clamp(sp / 0.36f, 0, 1.4f) - walkAmt) * 0.25f;
        walkPhaseO = walkPhase;
        walkPhase += sp / (hydra ? 10.5f : 9.5f) * Rig.PI;
        if (s == TAIL) walkPhase += 0.22f * Math.min(1, Math.abs(Mth.wrapDegrees(yr - yRotO)) / 12f);
        if (Math.floor(walkPhase / Rig.PI) != Math.floor(walkPhaseO / Rig.PI) && onGround()) footfall(((int) Math.floor(walkPhase / Rig.PI)) & 1);

        // aim of the neck(s)
        aimYawO = aimYaw; aimPitchO = aimPitch;
        Vec3 tp = null;
        if (getBeam() > 0.03f) { Vector3f b = getBeamEnd(); tp = new Vec3(b.x, b.y, b.z); }
        else if (s != DOWN && s != FALL && s != THROWN && s != DEAD && s != HELD && s != PIN && s != GETUP) {
            Entity te = getTargetEntity();
            if (te != null) tp = te.position().add(0, te.getBbHeight() * (te instanceof TitanEntity t && t.isDown() ? 0.15 : 0.7), 0);
        }
        float wy = 0, wp = 0;
        if (tp != null) {
            Vec3 from = position().add(0, getBbHeight() * (hydra ? 1.1 : 0.95), 0);
            Vec3 d = tp.subtract(from);
            float yaw = (float) (Mth.atan2(-d.x, d.z) * 180 / Math.PI);
            wy = -Mth.clamp(Mth.wrapDegrees(yaw - yr), -65, 65);
            wp = Mth.clamp((float) (Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * 180 / Math.PI), -70, 50);
        }
        aimYaw += (wy - aimYaw) * 0.22f;
        aimPitch += (wp - aimPitch) * 0.22f;

        float bm = getBeam();
        beamO.set(beamC);
        beamC.set(getBeamEnd());
        if (beamPrev <= 0.02f) beamO.set(beamC);
        beamPrev = bm;

        updateParams();
        if (!level().isClientSide) for (int i = 0; i <= Rig.NTL; i++) tailPrev[i] = toWorld(rig.tail[i]);
        rig.compute(this, 1f);
        for (TitanPart part : parts) part.place();
    }

    private void updateParams() {
        System.arraycopy(p, 0, po, 0, NP);
        float[] g = goal;
        java.util.Arrays.fill(g, 0);
        int s = getState();
        float t = stateTime, sd = getSide(), rate = 0.2f;
        boolean hy = hydra, direct = false;
        g[ARM] = hy ? 0.45f : 0f;
        g[JAW] = 0.06f;
        g[GLOW] = Math.max(0.1f + 0.05f * Mth.sin(tickCount * 0.05f), getBeam());
        g[LEAN] = 5 * walkAmt;
        switch (s) {
            case CHARGE -> { g[LEAN] = 22; g[NECK_P] = -10; g[JAW] = 0.55f; g[ARM] = hy ? 0.65f : 0.5f; g[TAIL_UP] = 14; g[FLAP] = hy ? 0.4f : 0; }
            case ROAR -> {
                if (t < 12) { g[LEAN] = 14; g[NECK_P] = -24; g[CROUCH] = 0.4f; }
                else if (t < 66) { g[LEAN] = -14; g[NECK_P] = 30; g[JAW] = 1; g[ARM] = 1; g[SHAKE] = 1; g[TAIL_UP] = 12; g[FLAP] = hy ? 0.25f : 0; }
                rate = 0.2f;
            }
            case TAIL -> {
                g[CROUCH] = 0.5f; g[LEAN] = t < 14 ? 8 : 17; g[TWIST] = t < 14 ? 26 * sd : (t < 36 ? -20 * sd : 0);
                g[TAIL_UP] = t < 44 ? 16 : 0; g[ARM] = hy ? 0.7f : 0.8f; g[JAW] = 0.4f; rate = 0.3f;
            }
            case BITE -> {
                if (t < 8) { g[REACH] = -0.7f; g[JAW] = 1; g[LEAN] = -4; }
                else if (t < 22) { g[REACH] = 1; g[LEAN] = 26; g[JAW] = t < 12 ? 1 : 0; g[CROUCH] = 0.3f; }
                rate = 0.45f;
            }
            case GRAB -> { g[REACH] = 0.85f; g[LEAN] = 20; g[JAW] = 0.12f; g[NECK_Y] = Mth.sin(t * 0.5f) * 22; g[CROUCH] = 0.4f; g[ARM] = hy ? 0.85f : 0.6f; g[FLAP] = hy ? 0.5f : 0; rate = 0.4f; }
            case THROW -> { g[REACH] = 0.85f; g[LEAN] = 16; g[JAW] = t < 15 ? 0.12f : 1; g[NECK_Y] = (t < 10 ? 45 : -55) * sd; g[CROUCH] = 0.4f; g[ARM] = 0.8f; rate = 0.45f; }
            case HELD -> { g[LEAN] = -12; g[ROLL] = Mth.sin(t * 0.45f) * 9; g[KICK] = 0.6f; g[JAW] = 0.85f; g[ARM] = 0.9f; g[SHAKE] = 0.6f; g[FLAP] = hy ? 1 : 0; g[NECK_P] = 16; rate = 0.35f; }
            case BREATH -> {
                if (t < BREATH_CHARGE) { g[LEAN] = 15; g[NECK_P] = -16; g[JAW] = 0.22f; g[CROUCH] = 0.4f; g[GLOW] = Math.max(g[GLOW], t / BREATH_CHARGE); g[ARM] = hy ? 0.6f : 0.3f; }
                else { g[LEAN] = 7; g[JAW] = 1; g[SHAKE] = 0.5f; g[GLOW] = 1; g[TAIL_UP] = 10; g[ARM] = hy ? 0.95f : 0.45f; g[CROUCH] = 0.25f; }
                rate = 0.25f;
            }
            case STAGGER -> { g[LEAN] = -24; g[NECK_P] = 18; g[JAW] = 0.75f; g[ARM] = 0.9f; g[CROUCH] = 0.35f; g[ROLL] = 8 * sd; g[SHAKE] = 0.3f; g[FLAP] = hy ? 0.7f : 0; rate = 0.35f; }
            case FALL -> {
                float u = Math.min(1, t / 22f);
                g[ROLL] = sd * 90 * u * u; direct = true;
                g[LEAN] = -10; g[JAW] = 0.9f; g[ARM] = 1; g[KICK] = 0.6f; g[LIMP] = Math.min(1, t / 20f) * 0.7f; g[FLAP] = hy ? 0.8f : 0;
            }
            case DOWN -> {
                boolean pinned = getPartnerId() != 0;
                g[ROLL] = 90 * sd; g[LIMP] = pinned ? 0.6f : 1; g[JAW] = pinned ? 0.75f : 0.25f; g[KICK] = pinned ? 0.5f : 0.1f; g[ARM] = pinned ? 0.8f : 0.3f;
                g[SHAKE] = pinned ? 0.6f : 0; rate = 0.3f;
            }
            case GETUP -> {
                float u = Math.min(1, t / 46f), e = u * u * (3 - 2 * u);
                g[ROLL] = 90 * sd * (1 - e); direct = true;
                g[CROUCH] = 0.9f * (1 - u); g[LEAN] = 26 * (1 - u); g[ARM] = 0.9f; g[JAW] = 0.5f; g[FLAP] = hy ? 0.8f : 0; g[KICK] = 0.3f * (1 - u);
            }
            case PIN -> {
                float cyc = t % 20;
                g[CROUCH] = 0.7f; g[LEAN] = 42; g[ARM] = 0.9f; g[FLAP] = hy ? 0.35f : 0;
                if (cyc < 8) { g[REACH] = -0.4f; g[JAW] = 1; g[NECK_P] = -34; } else { g[REACH] = 0.9f; g[JAW] = 0.05f; g[NECK_P] = -64; }
                rate = 0.4f;
            }
            case COUNTER -> {
                g[ROLL] = 90 * sd * (t < 20 ? 0.8f : 0.45f); g[NECK_P] = 32; g[REACH] = t < 10 ? -0.5f : 1; g[JAW] = t < 12 ? 1 : 0.1f;
                g[KICK] = 1; g[ARM] = 1; g[FLAP] = hy ? 1 : 0; rate = 0.4f;
            }
            case THROWN -> { g[ROLL] = sd * Math.min(t * 14, 450); direct = true; g[PIVOT] = 1; g[KICK] = 1; g[JAW] = 0.9f; g[ARM] = 1; g[LIMP] = 0.3f; g[FLAP] = hy ? 1 : 0; rate = 0.35f; }
            case DEAD -> { float u = Math.min(1, t / 26f); g[ROLL] = 90 * sd * u * u; direct = true; g[LIMP] = 1; g[JAW] = 0.4f; g[GLOW] = 0; g[ARM] = 0.2f; }
            default -> {}
        }
        for (int i = 0; i < NP; i++) p[i] += (g[i] - p[i]) * rate;
        if (direct) p[ROLL] = g[ROLL];
        p[PIVOT] += (g[PIVOT] - p[PIVOT]) * 0.3f;
    }

    // ---- server brain ----
    private void findFoe() {
        TitanEntity best = null;
        double bd = 420 * 420;
        for (Entity e : ((ServerLevel) level()).getAllEntities()) {
            if (e instanceof TitanEntity t && t != this && t.isAlive() && t.getType() != getType()) {
                double d = t.distanceToSqr(this);
                if (d < bd) { bd = d; best = t; }
            }
        }
        LivingEntity f = best;
        if (f == null) {
            LivingEntity a = getLastHurtByMob();
            if (a != null && a.isAlive() && a.distanceToSqr(this) < 220 * 220 && !(a instanceof Player pl && (pl.isCreative() || pl.isSpectator()))
                    && !(a instanceof TitanEntity)) f = a;
        }
        foe = f;
        entityData.set(TARGET, f == null ? 0 : f.getId());
    }

    private void serverTick() {
        if (isDeadOrDying()) { firing = false; updateBeam(null); yBodyRot = yHeadRot = getYRot(); return; }
        if (breathCd > 0) breathCd--;
        if (tailCd > 0) tailCd--;
        if (biteCd > 0) biteCd--;
        if (roarCd > 0) roarCd--;
        if (chargeCd > 0) chargeCd--;
        poise = Math.max(0, poise - 0.012f);
        if (tickCount % 20 == 1 || (foe != null && !foe.isAlive())) findFoe();
        LivingEntity foe = this.foe;
        TitanEntity ft = foe instanceof TitanEntity x ? x : null;
        if (partner != null && !partner.isAlive()) breakLink();
        int s = getState(), t = stateTime;
        walk = 0;
        switch (s) {
            case IDLE -> think(foe, ft);
            case CHARGE -> {
                if (foe == null || t > 140) { setState(IDLE); break; }
                face(foe.position(), 2.4f);
                walk = chargeSpeed();
                double d = hdist(foe);
                float reach = bodyRadius() + (ft != null ? ft.bodyRadius() : 2) + 6;
                if (d < reach) {
                    Vec3 dir = hdir(foe);
                    snd(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 14, 0.45f);
                    if (ft != null) {
                        boolean both = ft.getState() == CHARGE;
                        ft.impact(this, both ? 0.9f : 1.25f, dir, 22);
                        if (both) impact(ft, 0.9f, dir.scale(-1), 16); else { kb = kb.add(dir.scale(-0.5)); setState(IDLE); }
                    } else { foe.hurt(damageSources().mobAttack(this), 40); fling(foe, dir, 2.2); setState(IDLE); }
                    chargeCd = 260 + random.nextInt(160);
                }
            }
            case ROAR -> {
                if (foe != null) face(foe.position(), 1.5f);
                if (t == 12) roarSound();
                if (t >= 80) setState(IDLE);
            }
            case TAIL -> tailTick(t, ft);
            case BITE -> {
                if (foe != null && t < 10) face(foe.position(), 3);
                if (t < 10 && foe != null && hdist(foe) > biteRange() - 8) walk = walkSpeed() * 1.2f;
                if (t == 2) snd(SoundEvents.RAVAGER_ATTACK, 12, 0.5f);
                if (t == 12 && foe != null && hdist(foe) < biteRange() + 5 && angleTo(foe.position()) < 40) {
                    snd(SoundEvents.EVOKER_FANGS_ATTACK, 14, 0.45f);
                    Vec3 dir = hdir(foe);
                    if (ft != null) {
                        ft.titanHurt(this, 20);
                        ft.damageHead(ft.randomHead(), 30, this);
                        level().broadcastEntityEvent(ft, (byte) 70);
                        int fs = ft.getState();
                        if (!ft.isDown() && fs != HELD && fs != GETUP && fs != PIN && ft.partner == null && partner == null && ft.isAlive() && random.nextFloat() < 0.5f) {
                            link(ft);
                            setSide(random.nextBoolean() ? 1 : -1);
                            setState(GRAB);
                            ft.setState(HELD);
                        } else ft.impact(this, 0.55f, dir, 0);
                    } else { foe.hurt(damageSources().mobAttack(this), 45); fling(foe, dir, 1.2); }
                }
                if (t >= 32 && getState() == BITE) { biteCd = 70 + random.nextInt(50); setState(IDLE); }
            }
            case GRAB -> {
                TitanEntity v = partner;
                if (v == null || v.getState() != HELD) { breakLink(); setState(IDLE); break; }
                turn(Mth.cos(t * 0.5f) * 5.5f);
                drag(v, 0.3);
                if (t % 10 == 5) { v.titanHurt(this, 6); v.damageHead(v.biteHead(), 8, this); level().broadcastEntityEvent(v, (byte) 70); snd(SoundEvents.RAVAGER_STUNNED, 10, 0.5f); }
                if (t >= 48) setState(THROW);
            }
            case THROW -> {
                TitanEntity v = partner;
                float sd = getSide();
                if (t <= 10) turn(-sd * 4.5f); else if (t <= 17) turn(sd * 16f); else if (t < 34) turn(-sd * 4f);
                if (v != null && t < 15) drag(v, 0.6);
                if (v != null && t == 15) {
                    Vec3 dir = forward().add(left().scale(-sd * 0.9)).normalize();
                    breakLink();
                    v.setSide(random.nextBoolean() ? 1 : -1);
                    v.launch(dir.scale(1.5).add(0, 1.05, 0));
                    wantPin = random.nextFloat() < 0.7f;
                    snd(SoundEvents.ENDER_DRAGON_FLAP, 14, 0.5f);
                }
                if (t >= 36) { biteCd = 120; setState(IDLE); }
            }
            case HELD -> { if (partner == null || t > 140) { breakLink(); setState(IDLE); } }
            case BREATH -> {
                if (foe == null) { setState(IDLE); break; }
                face(foe.position(), 2.2f);
                if (t == 1) snd(hydra ? SoundEvents.TRIDENT_THUNDER : SoundEvents.BEACON_ACTIVATE, 14, hydra ? 0.7f : 0.5f);
                if (t == BREATH_CHARGE) {
                    firing = true;
                    Vec3 to = foe.position().subtract(position());
                    aim = position().add(to.scale(0.45)).add(0, 1, 0);
                    breathEnd = BREATH_CHARGE + 80 + random.nextInt(50);
                    snd(SoundEvents.WARDEN_SONIC_BOOM, 16, hydra ? 0.9f : 0.6f);
                }
                if (t > BREATH_CHARGE) {
                    Vec3 want = foe instanceof TitanEntity ff ? (ff.isDown() ? ff.position().add(0, 6, 0) : ff.chest()) : foe.position().add(0, foe.getBbHeight() * 0.5, 0);
                    Vec3 dd = want.subtract(aim);
                    double l = dd.length();
                    aim = l < 3.4 ? want : aim.add(dd.scale(3.4 / l));
                    if (t % 9 == 0) snd(hydra ? SoundEvents.LIGHTNING_BOLT_THUNDER : SoundEvents.BLAZE_SHOOT, 12, hydra ? 1.5f : 0.5f);
                    surge = Mth.clamp(surge + (random.nextFloat() - 0.5f) * 0.1f, 0.7f, 1.3f);
                    if (isClash()) breathEnd = Math.max(breathEnd, t + 6);
                    if (t >= breathEnd) { breathCd = 320 + random.nextInt(240); setState(IDLE); }
                }
            }
            case STAGGER -> { if (t >= 26) setState(IDLE); }
            case FALL -> { if (t >= 22) { slam(); setState(DOWN); } }
            case DOWN -> {
                if (partner != null && partner.getState() != PIN) breakLink();
                if (partner == null && t >= downTime) setState(GETUP);
            }
            case GETUP -> { if (t >= 50) { poise = 0; setState(IDLE); } }
            case PIN -> {
                TitanEntity v = partner;
                if (v == null || v.getState() != DOWN) { endPin(); if (getState() == PIN) setState(IDLE); break; }
                Vec3 r = v.left().scale(v.getSide());
                Vec3 want = v.position().add(r.scale(19)).add(0, hydra ? 10.5 : 12, 0);
                Vec3 np = position().lerp(want, 0.18);
                setPos(np.x, np.y, np.z);
                setDeltaMovement(Vec3.ZERO);
                face(position().add(r), 7);
                if (t % 20 == 10) {
                    v.titanHurt(this, 10); v.damageHead(v.randomHead(), 15, this);
                    level().broadcastEntityEvent(v, (byte) 70);
                    snd(SoundEvents.EVOKER_FANGS_ATTACK, 12, 0.45f);
                }
                if (t >= pinDur) v.setState(COUNTER);
            }
            case COUNTER -> {
                TitanEntity a = partner;
                if (t == 12 && a != null) { a.titanHurt(this, 22); a.damageHead(a.randomHead(), 20, this); level().broadcastEntityEvent(a, (byte) 70); snd(SoundEvents.EVOKER_FANGS_ATTACK, 14, 0.5f); }
                if (t == 20 && a != null) {
                    Vec3 dir = hdir(a);
                    a.endPin();
                    a.setSide(random.nextBoolean() ? 1 : -1);
                    a.launch(dir.scale(1.25).add(0, 0.95, 0));
                    snd(SoundEvents.ENDER_DRAGON_FLAP, 14, 0.5f);
                }
                if (t >= 30) { breakLink(); setState(GETUP); }
            }
            case THROWN -> {
                if (t > 6 && (onGround() || t > 200)) { slam(); titanHurt(null, 30); downTime = 130 + random.nextInt(60); if (!isDeadOrDying()) setState(DOWN); }
            }
            default -> {}
        }
        updateBeam(ft);

        // titans do not walk through each other
        int cs = getState();
        if (ft != null && partner == null && ft.partner == null && cs != DOWN && cs != THROWN && cs != FALL && !ft.isDown()) {
            double d = hdist(ft), min = bodyRadius() + ft.bodyRadius() + 5;
            if (d < min && d > 0.01) kb = kb.add(hdir(ft).scale(-0.07 * (min - d)));
        }
        if (!isNoGravity()) {
            Vec3 f = forward(), dm = getDeltaMovement();
            setDeltaMovement(f.x * walk + kb.x, dm.y, f.z * walk + kb.z);
        }
        kb = kb.scale(onGround() ? 0.88 : 0.985);
        if (kb.lengthSqr() < 1e-4) kb = Vec3.ZERO;
        if (tickCount % 2 == 0 && (walk != 0 || kb.lengthSqr() > 0.004) && !isNoGravity()) crush();
        yBodyRot = yHeadRot = getYRot();
    }

    private void think(LivingEntity foe, TitanEntity ft) {
        if (foe == null) {
            if (roarCd == 0 && random.nextInt(400) == 0) { roarCd = 900; setState(ROAR); }
            return;
        }
        double d = hdist(foe);
        float ang = angleTo(foe.position());
        face(foe.position(), 3f);
        float ws = walkSpeed();
        if (ft != null && ft.getState() == DOWN && ft.partner == null && wantPin && ft.stateTime < ft.downTime - 30) {
            if (d > 30) walk = ws * 1.5f;
            else { startPin(ft); }
            return;
        }
        if (ft != null && ft.isDown()) {
            if (roarCd == 0) { roarCd = 500; setState(ROAR); return; }
            if (d > 60) walk = ws;
            return;
        }
        if (ft != null && ft.getState() == BREATH && breathCd < 200 && ang < 30 && ft.stateTime < BREATH_CHARGE + 30 && beamFactor() > 0) {
            if (random.nextFloat() < 0.25f) { setState(BREATH); return; }
        }
        float melee = bodyRadius() + (ft != null ? ft.bodyRadius() : 2) + 30;
        if (d > 120) {
            if (roarCd == 0 && random.nextInt(40) == 0) { roarCd = 700; setState(ROAR); return; }
            if (breathCd == 0 && d < 260 && ang < 12 && beamFactor() > 0) { setState(BREATH); return; }
            walk = ws;
        } else if (d > melee) {
            if (breathCd == 0 && ang < 12 && random.nextInt(25) == 0 && beamFactor() > 0) { setState(BREATH); return; }
            if (chargeCd == 0 && ang < 15) { snd(SoundEvents.RAVAGER_ROAR, 14, 0.5f); setState(CHARGE); return; }
            walk = ws;
        } else {
            if (ang < 30) {
                if (tailCd == 0 && random.nextInt(12) == 0) { startTail(); return; }
                if (biteCd == 0 && d < biteRange() + 2 && random.nextInt(6) == 0) { setState(BITE); return; }
                if (breathCd == 0 && random.nextInt(120) == 0 && beamFactor() > 0) { setState(BREATH); return; }
            }
            float keep = bodyRadius() + (ft != null ? ft.bodyRadius() : 2) + 10;
            if (d > biteRange() - 4) walk = ws * 0.8f; else if (d < keep) walk = -ws * 0.5f;
        }
    }

    private double hdist(Entity e) { double dx = e.getX() - getX(), dz = e.getZ() - getZ(); return Math.sqrt(dx * dx + dz * dz); }
    private Vec3 hdir(Entity e) {
        Vec3 d = new Vec3(e.getX() - getX(), 0, e.getZ() - getZ());
        return d.lengthSqr() < 1e-6 ? forward() : d.normalize();
    }
    private void fling(Entity e, Vec3 dir, double pow) { e.setDeltaMovement(dir.x * pow, 0.6 + pow * 0.3, dir.z * pow); e.hurtMarked = true; }

    private void link(TitanEntity o) { partner = o; o.partner = this; entityData.set(PARTNER, o.getId()); o.entityData.set(PARTNER, getId()); }

    public void breakLink() {
        TitanEntity o = partner;
        partner = null;
        entityData.set(PARTNER, 0);
        setNoGravity(false);
        if (o != null) {
            o.partner = null;
            o.entityData.set(PARTNER, 0);
            o.setNoGravity(false);
            int os = o.getState();
            if (os == HELD || os == GRAB || os == THROW || os == PIN) o.setState(IDLE);
        }
    }

    private void drag(TitanEntity v, double k) {
        Vec3 want = position().add(forward().scale(bodyRadius() + v.bodyRadius() + 7));
        v.kb = new Vec3((want.x - v.getX()) * k, 0, (want.z - v.getZ()) * k);
        v.face(position(), 6);
    }

    public void launch(Vec3 vel) {
        breakLink();
        kb = new Vec3(vel.x, 0, vel.z);
        setDeltaMovement(vel);
        setOnGround(false);
        hasImpulse = true;
        setState(THROWN);
    }

    private void startPin(TitanEntity v) {
        link(v);
        pinDur = 90 + random.nextInt(50);
        wantPin = false;
        setNoGravity(true);
        setState(PIN);
        snd(SoundEvents.RAVAGER_ROAR, 14, hydra ? 0.8f : 0.5f);
    }

    private void endPin() { setNoGravity(false); breakLink(); }

    // ---- tail swipe: wind up against the swing, spin, and stop where the tail lands ----
    private void startTail() {
        setSide(random.nextBoolean() ? 1 : -1);
        tailPhase = 0;
        setState(TAIL);
        tailOff = 0;
        snd(SoundEvents.RAVAGER_ROAR, 12, 0.6f);
    }

    private static float tailCurve(float t) {
        if (t <= 14) { float u = t / 14f; return -35 * u * u * (3 - 2 * u); }
        if (t <= 34) { float u = (t - 14) / 20f; return -35 + 395 * u * u * (3 - 2 * u); }
        return 360;
    }

    private void tailTick(int t, TitanEntity ft) {
        float sd = getSide();
        if (tailPhase == 0) {
            float d = tailCurve(t) - tailCurve(t - 1);
            tailOff += d;
            turn(sd * d);
            if (t == 15) snd(SoundEvents.ENDER_DRAGON_FLAP, 14, 0.4f);
            if (t > 16 && t <= 36 && tailPrev[0] != null) tailHits(ft);
            if (t >= 46 && tailPhase == 0) { tailCd = 100 + random.nextInt(100); setState(IDLE); }
        } else {
            float d = tailOff * 0.13f;
            tailOff -= d;
            turn(-sd * d);
            if (Math.abs(tailOff) < 2 || t > 90) { turn(-sd * tailOff); tailCd = 100 + random.nextInt(100); setState(IDLE); }
        }
    }

    private void tailHits(TitanEntity ft) {
        boolean grief = ForgeEventFactory.getMobGriefingEvent(level(), this);
        List<LivingEntity> small = null;
        for (int i = 5; i <= Rig.NTL; i++) {
            Vec3 a = tailPrev[i], b = toWorld(rig.tail[i]);
            Vec3 tan = new Vec3(b.x - a.x, 0, b.z - a.z);
            if (tan.lengthSqr() < 0.01) continue;
            tan = tan.normalize();
            for (int k = 1; k <= 3; k++) {
                Vec3 q = a.lerp(b, k / 3.0);
                if (ft != null && ft.isAlive() && !ft.isDown() && ft.getState() != PIN) {
                    double dx = q.x - ft.getX(), dz = q.z - ft.getZ();
                    if (dx * dx + dz * dz < Mth.square(ft.bodyRadius() + rig.tailR[i] + 1) && q.y < ft.getY() + ft.getBbHeight()) {
                        float power = (1.0f + random.nextFloat() * 0.8f) * (i > 11 ? 1.2f : 1f);
                        ft.impact(this, power, tan, 28 * power);
                        snd(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 16, 0.4f);
                        snd(SoundEvents.GENERIC_EXPLODE, 10, 0.6f);
                        // the tail stops on the body it hit: rewind to the moment this segment was there, then swing back
                        float rew = Mth.clamp(yawBack(0, 1) - yawBack(i * Rig.LAG, 1), -120, 120);
                        turn(-rew);
                        tailOff -= rew * getSide();
                        tailPhase = 1;
                        return;
                    }
                }
            }
            if (i % 3 == 0) {
                if (small == null) small = level().getEntitiesOfClass(LivingEntity.class, new AABB(position(), position()).inflate(70, 30, 70), e -> !(e instanceof TitanEntity) && e.isAlive());
                for (LivingEntity e : small) if (e.distanceToSqr(b) < Mth.square(rig.tailR[i] + 3)) { e.hurt(damageSources().mobAttack(this), 30); fling(e, tan, 2.5); }
                if (grief && i >= 6) breakSphere(b, rig.tailR[i] + 1.2f, getY() + 1.5);
            }
        }
    }

    // ---- breath ----
    protected float beamPower() { return getBeam() * (0.75f + 0.5f * getHealth() / getMaxHealth()) * surge * beamFactor(); }

    private void updateBeam(TitanEntity ft) {
        float b = getBeam();
        if (firing && beamFactor() <= 0) firing = false;
        b = firing ? Math.min(1, b + 0.07f) : Math.max(0, b - 0.028f);
        entityData.set(BEAM, b);
        if (b <= 0.02f) { if (isClash()) entityData.set(CLASH, false); clashT = 0; clashPos = 0.5f; return; }
        boolean clash = firing && ft != null && ft.firing && b > 0.3f && ft.getBeam() > 0.3f && ft.foe == this && foe == ft;
        if (clash) { if (getId() < ft.getId()) clash(ft); return; }
        if (isClash()) entityData.set(CLASH, false);
        clashT = 0; clashPos = 0.5f;
        Vec3 m = mouthWorld();
        Vec3 dir = aim.subtract(m);
        if (dir.lengthSqr() < 1e-4) return;
        dir = dir.normalize();
        Vec3 far = m.add(dir.scale(300));
        BlockHitResult bh = level().clip(new ClipContext(m, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = bh.getType() == HitResult.Type.MISS ? far : bh.getLocation();
        boolean block = bh.getType() != HitResult.Type.MISS;
        TitanEntity hit = null;
        if (ft != null && ft.isAlive()) {
            Vec3 c = ft.isDown() ? ft.position().add(0, 6, 0) : ft.chest();
            Vec3 se = end.subtract(m);
            double u = Mth.clamp(c.subtract(m).dot(se) / se.lengthSqr(), 0, 1);
            Vec3 q = m.add(se.scale(u));
            if (q.distanceTo(c) < ft.bodyRadius() + 2.5) { end = q.subtract(dir.scale(ft.bodyRadius() * 0.7)); hit = ft; block = false; }
        }
        if (b > 0.25f) {
            if (hit != null) hit.beamHit(this, dir, b);
            else if (block && tickCount % 4 == 0)
                level().explode(this, end.x, end.y, end.z, 2.5f + 2 * b, !hydra, Level.ExplosionInteraction.MOB);
            if (tickCount % 5 == 0) {
                AABB box = new AABB(m, end).inflate(5);
                Vec3 se = end.subtract(m);
                for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, x -> !(x instanceof TitanEntity) && x.isAlive())) {
                    Vec3 c = e.position().add(0, e.getBbHeight() / 2, 0);
                    double u = Mth.clamp(c.subtract(m).dot(se) / se.lengthSqr(), 0, 1);
                    if (m.add(se.scale(u)).distanceToSqr(c) < 16) { e.hurt(damageSources().mobAttack(this), 16 * b); if (!hydra) e.setSecondsOnFire(6); }
                }
            }
        }
        entityData.set(BEAM_END, new Vector3f((float) end.x, (float) end.y, (float) end.z));
    }

    private void beamHit(TitanEntity from, Vec3 dir, float b) {
        int s = getState();
        boolean bracing = s == BREATH;
        kb = kb.add(dir.x * 0.04 * b, 0, dir.z * 0.04 * b);
        poise += (bracing ? 0.008f : 0.032f) * b;
        if (tickCount % 5 == 0) { titanHurt(from, 4.5f * b); level().broadcastEntityEvent(this, (byte) 70); }
        if (isDeadOrDying() || s == FALL || s == DOWN || s == THROWN || s == HELD || s == COUNTER || s == GETUP || s == PIN) return;
        if (poise >= 2f) { knockdown(dir); from.wantPin = from.random.nextFloat() < 0.7f; }
        else if (poise >= 1f && s != STAGGER && !bracing) { breakLink(); stagger(dir); }
    }

    /** Two beams meet head on. The stronger one pushes the meeting point back until it reaches the weaker titan. */
    private void clash(TitanEntity o) {
        Vec3 ma = mouthWorld(), mb = o.mouthWorld();
        clashT++;
        float pa = beamPower(), pb = o.beamPower();
        clashPos += (pa - pb) * 0.03f * (1 + clashT / 50f) + (random.nextFloat() - 0.5f) * 0.012f;
        if (clashT < 40) clashPos = Mth.clamp(clashPos, 0.3f, 0.7f);
        Vec3 c = ma.lerp(mb, Mth.clamp(clashPos, 0.04, 0.96));
        Vector3f cv = new Vector3f((float) c.x, (float) c.y, (float) c.z);
        entityData.set(BEAM_END, cv); o.entityData.set(BEAM_END, cv);
        entityData.set(CLASH, true); o.entityData.set(CLASH, true);
        if (clashT % 8 == 0) level().playSound(null, c.x, c.y, c.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 16, 0.6f);
        if (clashPos >= 0.92f || clashPos <= 0.08f || clashT > 220) {
            TitanEntity win = clashPos >= 0.5f ? this : o, lose = win == this ? o : this;
            Vec3 dir = win.hdir(lose);
            Vec3 lm = lose.mouthWorld();
            level().explode(win, lm.x, lm.y, lm.z, 7f, false, Level.ExplosionInteraction.NONE);
            lose.firing = false;
            lose.entityData.set(CLASH, false); win.entityData.set(CLASH, false);
            lose.titanHurt(win, 70);
            lose.kb = lose.kb.add(dir.scale(2.4));
            level().broadcastEntityEvent(lose, (byte) 70);
            if (!lose.isDeadOrDying()) { lose.breathCd = 400; lose.knockdown(dir); }
            win.wantPin = win.random.nextFloat() < 0.7f;
            win.aim = lose.chest();
            win.breathEnd = win.stateTime + 16;
            clashT = 0; clashPos = 0.5f; o.clashT = 0; o.clashPos = 0.5f;
        }
    }

    // ---- reactions ----
    public void titanHurt(TitanEntity from, float dmg) {
        if (dmg <= 0) return;
        invulnerableTime = 0;
        hurt(from == null ? damageSources().generic() : damageSources().mobAttack(from), dmg);
    }

    /** A physical blow: damage, a shove along {@code dir}, and a stagger or a fall depending on how hard it was. */
    public void impact(TitanEntity from, float power, Vec3 dir, float dmg) {
        titanHurt(from, dmg);
        level().broadcastEntityEvent(this, (byte) 70);
        if (isDeadOrDying()) return;
        int s = getState();
        kb = kb.add(dir.x * power * 0.9, 0, dir.z * power * 0.9);
        if (s == FALL || s == DOWN || s == THROWN || s == HELD || s == COUNTER || s == GETUP) return;
        poise += power;
        if (poise >= 2f || power >= 1.5f) { knockdown(dir); if (from != null) from.wantPin = from.random.nextFloat() < 0.7f; }
        else { breakLink(); stagger(dir); }
    }

    private void stagger(Vec3 dir) {
        setSide(dir.dot(left()) >= 0 ? 1 : -1);
        setState(STAGGER);
        snd(hydra ? SoundEvents.ENDER_DRAGON_HURT : SoundEvents.RAVAGER_HURT, 12, hydra ? 0.8f : 0.45f);
    }

    public void knockdown(Vec3 dir) {
        breakLink();
        setSide(dir.dot(left()) >= 0 ? 1 : -1);
        poise = 0;
        downTime = 130 + random.nextInt(70);
        setState(FALL);
        snd(hydra ? SoundEvents.ENDER_DRAGON_HURT : SoundEvents.RAVAGER_HURT, 14, hydra ? 0.6f : 0.4f);
    }

    private void slam() {
        snd(SoundEvents.GENERIC_EXPLODE, 16, 0.45f);
        snd(SoundEvents.LIGHTNING_BOLT_IMPACT, 10, 0.4f);
        Vec3 c = toWorld(rig.torso[3]);
        if (ForgeEventFactory.getMobGriefingEvent(level(), this)) {
            for (int i = 1; i <= Rig.NT; i += 2) breakSphere(toWorld(rig.torso[i]), 9, -1e9);
            for (int h = 0; h < rig.heads; h++) breakSphere(toWorld(rig.headPos[h]), 5, -1e9);
        }
        level().explode(this, c.x, getY() + 1, c.z, 4f, false, Level.ExplosionInteraction.MOB);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(26, 12, 26), x -> !(x instanceof TitanEntity)))
            e.hurt(damageSources().mobAttack(this), 30);
    }

    private void footfall(int which) {
        Vec3 f = toWorld(rig.leg[which][3]);
        if (level().isClientSide) {
            SHAKER.shake(f.x, f.y, f.z, getState() == CHARGE ? 0.5f : 0.3f);
            for (int i = 0; i < 6; i++) fx.add(Fx.dust(f.x + random.nextGaussian() * 3, getY() + 1, f.z + random.nextGaussian() * 3, random.nextGaussian() * 0.12, 0.05 + random.nextFloat() * 0.1, random.nextGaussian() * 0.12, 3 + random.nextFloat() * 2, 28));
        } else {
            level().playSound(null, f.x, f.y, f.z, SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 10, 0.5f);
            level().playSound(null, f.x, f.y, f.z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 3, 0.5f);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(f, f).inflate(6.5, 4, 6.5), x -> !(x instanceof TitanEntity)))
                e.hurt(damageSources().mobAttack(this), 24);
        }
    }

    protected void roarSound() {
        if (hydra) { snd(SoundEvents.ENDER_DRAGON_GROWL, 16, 0.75f); snd(SoundEvents.ENDER_DRAGON_GROWL, 14, 0.95f); snd(SoundEvents.PHANTOM_DEATH, 10, 0.5f); }
        else { snd(SoundEvents.ENDER_DRAGON_GROWL, 16, 0.5f); snd(SoundEvents.WARDEN_ROAR, 16, 0.6f); }
    }

    // ---- blocks ----
    private void crush() {
        if (!ForgeEventFactory.getMobGriefingEvent(level(), this)) return;
        AABB b = getBoundingBox().inflate(1.5, 0, 1.5);
        breakBox(Mth.floor(b.minX), Mth.floor(b.minY + 1.7), Mth.floor(b.minZ), Mth.floor(b.maxX), Mth.floor(b.minY + getBbHeight() * 0.8), Mth.floor(b.maxZ), null, 0);
    }

    protected void breakSphere(Vec3 c, float r, double minY) {
        int y0 = (int) Math.max(Math.floor(c.y - r), Math.floor(minY));
        breakBox(Mth.floor(c.x - r), y0, Mth.floor(c.z - r), Mth.floor(c.x + r), Mth.floor(c.y + r), Mth.floor(c.z + r), c, r);
    }

    private void breakBox(int x0, int y0, int z0, int x1, int y1, int z1, Vec3 c, float r) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        Level lv = level();
        int n = 0;
        y0 = Math.max(y0, lv.getMinBuildHeight());
        y1 = Math.min(y1, lv.getMaxBuildHeight() - 1);
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = y0; y <= y1; y++) {
            if (c != null && c.distanceToSqr(x + 0.5, y + 0.5, z + 0.5) > r * r) continue;
            m.set(x, y, z);
            BlockState st = lv.getBlockState(m);
            if (st.isAir() || st.getBlock() instanceof LiquidBlock || st.getDestroySpeed(lv, m) < 0) continue;
            if ((n++ & 31) == 0) lv.destroyBlock(m, false); else lv.setBlock(m, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    // ---- client effects ----
    protected void clientTick() {
        for (int i = fx.size() - 1; i >= 0; i--) if (!fx.get(i).tick()) fx.remove(i);
        int s = getState();
        if (s == ROAR && stateTime > 12 && stateTime < 66) SHAKER.shake(getX(), getY(), getZ(), 0.16f);
        float b = getBeam();
        if (b > 0.1f) {
            Vector3f e = getBeamEnd();
            SHAKER.shake(e.x, e.y, e.z, 0.14f * b);
            boolean clash = isClash();
            for (int i = 0; i < (clash ? 3 : 2); i++) {
                float sp = clash ? 1.2f : 0.5f;
                fx.add(Fx.spark(e.x, e.y, e.z, random.nextGaussian() * sp, random.nextGaussian() * sp + 0.2, random.nextGaussian() * sp, 1.2f + random.nextFloat() * 2, 10 + random.nextInt(10), hydra));
            }
            if (!clash && tickCount % 2 == 0) fx.add(Fx.dust(e.x, e.y, e.z, random.nextGaussian() * 0.25, 0.25 + random.nextFloat() * 0.3, random.nextGaussian() * 0.25, 4 + random.nextFloat() * 3, 40));
        }
        if (s == DOWN && getPartnerId() != 0 && tickCount % 4 == 0) SHAKER.shake(getX(), getY(), getZ(), 0.1f);
    }

    private void slamFx() {
        Vec3 c = toWorld(rig.torso[3]);
        SHAKER.shake(c.x, c.y, c.z, 1.6f);
        for (int i = 0; i < 46; i++) {
            double a = random.nextDouble() * Math.PI * 2, rr = 6 + random.nextDouble() * 16;
            fx.add(Fx.dust(c.x + Math.cos(a) * rr, getY() + 1 + random.nextDouble() * 3, c.z + Math.sin(a) * rr, Math.cos(a) * 0.5, 0.1 + random.nextDouble() * 0.35, Math.sin(a) * 0.5, 5 + random.nextFloat() * 5, 50 + random.nextInt(30)));
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 70) {
            Vec3 c = isDown() ? toWorld(rig.torso[3]) : chest();
            SHAKER.shake(c.x, c.y, c.z, 0.5f);
            for (int i = 0; i < 10; i++)
                fx.add(Fx.dust(c.x + random.nextGaussian() * 5, c.y + random.nextGaussian() * 6, c.z + random.nextGaussian() * 5, random.nextGaussian() * 0.4, random.nextGaussian() * 0.3, random.nextGaussian() * 0.4, 2 + random.nextFloat() * 2.5f, 22).tint(hydra ? 0.5f : 0.35f, hydra ? 0.1f : 0.08f, hydra ? 0.45f : 0.05f));
        } else super.handleEntityEvent(id);
    }

    // ---- vanilla plumbing ----
    @Override
    public boolean hurt(DamageSource src, float amt) {
        if (src.is(DamageTypeTags.IS_EXPLOSION) || src.is(DamageTypeTags.IS_FIRE) || src.is(DamageTypeTags.IS_FALL) || src.is(DamageTypeTags.IS_LIGHTNING)
                || src.is(DamageTypes.IN_WALL) || src.is(DamageTypes.CRAMMING) || src.is(DamageTypes.DROWN) || src.getEntity() == this) return false;
        return super.hurt(src, amt);
    }

    public boolean hurtPart(TitanPart part, DamageSource src, float amt) { return hurt(src, amt); }

    @Override
    public void die(DamageSource src) {
        if (!level().isClientSide) {
            breakLink();
            setSide(random.nextBoolean() ? 1 : -1);
            setState(DEAD);
            roarSound();
        }
        super.die(src);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (level().isClientSide) return;
        if (deathTime == 24) slam();
        if (deathTime >= 300) {
            ((ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 4, getZ(), 12, 10, 4, 10, 0);
            remove(RemovalReason.KILLED);
        }
    }

    public void forceAct(String a) {
        switch (a) {
            case "roar" -> setState(ROAR);
            case "tail" -> startTail();
            case "bite" -> setState(BITE);
            case "breath" -> { breathCd = 0; if (foe != null) setState(BREATH); }
            case "charge" -> setState(CHARGE);
            case "fall" -> knockdown(left());
            case "stagger" -> stagger(forward().scale(-1));
            case "throw" -> { setSide(1); launch(left().scale(1.4).add(0, 1, 0)); }
            case "pin" -> wantPin = true;
            default -> setState(IDLE);
        }
    }

    @Override public void setId(int id) { super.setId(id); if (parts != null) for (int i = 0; i < parts.length; i++) parts[i].setId(id + i + 1); }
    @Override public boolean isMultipartEntity() { return true; }
    @Override public PartEntity<?>[] getParts() { return parts; }
    @Override protected void registerGoals() {}
    @Override public boolean isPushable() { return false; }
    @Override public void push(Entity e) {}
    @Override protected void pushEntities() {}
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean causeFallDamage(float d, float m, DamageSource s) { return false; }
    @Override public boolean ignoreExplosion() { return true; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean isPushedByFluid() { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return null; }
    @Override protected SoundEvent getDeathSound() { return null; }
    @Override protected SoundEvent getAmbientSound() { return null; }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 600 * 600; }
    @Override public boolean canChangeDimensions() { return false; }
    @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); }
}
