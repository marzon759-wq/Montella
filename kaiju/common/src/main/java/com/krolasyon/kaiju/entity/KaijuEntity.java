package com.krolasyon.kaiju.entity;

import com.krolasyon.kaiju.KaijuMod;
import com.krolasyon.kaiju.ai.KaijuBrain;
import com.krolasyon.kaiju.data.KaijuSpec;
import com.krolasyon.kaiju.net.ControlInput;
import com.krolasyon.kaiju.world.WorldSmash;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.*;

/**
 * A film-scale kaiju. The server owns everything physical: it plays clips on a tick clock, reads the clip's pose track
 * to place collision nodes exactly where the animated body is, and resolves hits from the nodes' real velocities
 * (impulse = reduced mass x closing speed), so a fast tail tip hits harder than a slow shove and the victim is moved,
 * staggered or knocked down by the momentum it actually received. The client only plays the clip it is told to.
 */
public class KaijuEntity extends Mob implements GeoEntity {
    // ------------------------------------------------------------------ synced state
    static final EntityDataAccessor<Integer> ANIM = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> ANIM_SEQ = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> ANIM_TICK = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> HITSTOP = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Float> HP = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> CHARGE = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Vector3f> BEAM_A = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.VECTOR3);
    static final EntityDataAccessor<Vector3f> BEAM_B = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.VECTOR3);
    static final EntityDataAccessor<Integer> HEAT_A = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> HEAT_B = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> SEVER = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> REGEN = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Float> AIM_YAW = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> AIM_PITCH = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Integer> BEAM_STYLE = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> POISE = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.INT);

    public static final int F_DOWN = 1, F_HELD = 2, F_GUARD = 4, F_CONTROLLED = 8, F_CHARGING = 16, F_FIRING = 32, F_HOLDING = 64,
            F_DEAD = 128, F_CLASH = 256, F_THROWN = 512, F_AI_ASSIST = 1024, F_FLYING = 2048;

    public enum State { IDLE, ACTION, REACT, DOWN, GETUP, HELD, THROWN, DEAD }

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    public final KaijuSpec spec;

    // ------------------------------------------------------------------ server state
    public State state = State.IDLE;
    public KaijuSpec.Clip clip;
    public int clipTick;
    float clipBaseYaw;
    public KaijuSpec.Attack attack;
    public int atkTick, phase, phaseTick, phaseLen;
    public final Map<String, Integer> cooldown = new HashMap<>();
    public final Set<Long> hitOnce = new HashSet<>();
    public float hp, poise = 100, stamina = 100;
    /** flight (species with wings): cruising altitude above the ground, set by take-off, dives and climbs */
    public boolean flying;
    public double flyAlt;
    public int flyTicks;
    public int hitstop;
    public Vec3 knock = Vec3.ZERO;  // momentum-driven sliding, blocks/tick
    public double vy;
    public boolean onGround = true;
    public int airTicks;
    public float moveFwd, moveSide, faceYaw, wantSpeed;
    public int downTicks;
    public boolean downOnFront;
    public int guardTicks, dodgeTicks, parryTicks, pinTicks, kdImmune;
    @Nullable public KaijuEntity heldBy, holding;
    public int gripNode = -1, heldNode = -1;
    @Nullable public DebrisEntity heldDebris;
    public final float[] heat = new float[8];
    public float[] partHp;
    public int severMask;
    public final float[] regen = new float[3];
    public final int[] regenDelay = new int[3];
    public final KaijuBeam beam = new KaijuBeam(this);
    public final KaijuBrain brain = new KaijuBrain(this);
    @Nullable public UUID owner;
    @Nullable public ServerPlayer controller;
    public ControlInput input = new ControlInput();
    public boolean allowDismount;
    public String special = "tail_spin_360";
    public int deathTicks;
    public final Map<String, float[]> stats = new HashMap<>(); // attack id -> [tries, landed]
    public int lastLanded;

    // node world positions (current / previous tick) and the carve bookkeeping
    public Vec3[] nodes, prevNodes;
    final Vec3[] lastCarve;
    int carveCursor;
    private final float[] tmp = new float[3];

    public KaijuEntity(EntityType<? extends KaijuEntity> type, Level level, String specId) {
        super(type, level);
        this.spec = KaijuSpec.get(specId);
        this.hp = spec.maxHp;
        this.partHp = new float[spec.severGroups.size()];
        Arrays.fill(partHp, partMaxHp());
        this.nodes = new Vec3[spec.nodes.size()];
        this.prevNodes = new Vec3[spec.nodes.size()];
        this.lastCarve = new Vec3[spec.nodes.size()];
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.clip = spec.clip("idle");
        this.noCulling = true;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 1024).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 256);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ANIM, 0);
        entityData.define(ANIM_SEQ, 0);
        entityData.define(ANIM_TICK, 0);
        entityData.define(HITSTOP, 0);
        entityData.define(FLAGS, 0);
        entityData.define(HP, 1f);
        entityData.define(CHARGE, 0f);
        entityData.define(BEAM_A, new Vector3f());
        entityData.define(BEAM_B, new Vector3f());
        entityData.define(HEAT_A, 0);
        entityData.define(HEAT_B, 0);
        entityData.define(SEVER, 0);
        entityData.define(REGEN, 0);
        entityData.define(AIM_YAW, 0f);
        entityData.define(AIM_PITCH, 0f);
        entityData.define(SCALE, 0f);
        entityData.define(POISE, 100);
        entityData.define(BEAM_STYLE, 0);
    }

    // ------------------------------------------------------------------ geometry helpers
    public float scale() {
        float s = entityData.get(SCALE);
        return s > 0 ? s : spec.scale;
    }

    public void setScale(float s) {
        entityData.set(SCALE, s);
        refreshDimensions();
    }

    /** blocks per model px */
    public float k() { return scale() / 16f; }

    public float mass() { float r = scale() / spec.scale; return spec.mass * r * r * r; }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float s = scale() / spec.scale;
        EntityDimensions d = getType().getDimensions();
        return EntityDimensions.scalable(d.width * s, (isDown() ? d.height * 0.35f : d.height) * s);
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(90 * k() / 0.5, 20 * k() / 0.5, 90 * k() / 0.5);
    }

    public float partMaxHp() { return spec.maxHp * 0.14f; }

    public Vec3 nodePos(int i) {
        Vec3 v = nodes[i];
        return v != null ? v : position().add(0, getBbHeight() * 0.5, 0);
    }

    public float nodeR(int i) { return spec.nodes.get(i).r() * k(); }

    public boolean partOk(int group) { return group < 0 || (severMask & (1 << group)) == 0 || regen[group] >= 1f; }

    public void computeNodes() {
        KaijuSpec.Clip c = clip;
        float k = k();
        float yaw = getYRot();
        for (int i = 0; i < nodes.length; i++) {
            prevNodes[i] = nodes[i];
            spec.nodePx(c, clipTick, i, tmp);
            nodes[i] = position().add(KaijuSpec.toWorld(tmp[0], tmp[1], tmp[2], yaw, k));
            if (prevNodes[i] == null) prevNodes[i] = nodes[i];
        }
        if (Math.abs(aimY) > 0.3f || Math.abs(aimP) > 0.3f) aimHead(k, yaw);
    }

    /** head turned toward the breath's target: the head-end nodes swing about the base of the neck by the aim angles */
    public float aimY, aimP;  // degrees: yaw + = to the kaiju's left, pitch + = down
    private int[] headNodes;

    private void aimHead(float k, float yaw) {
        if (headNodes == null) headNodes = new int[]{spec.node("head"), spec.node("snout"), spec.node("jaw"), spec.node("mouth"), spec.node("aim"), spec.node("horn"), spec.node("horn_tip")};
        Vec3 pivot = position().add(KaijuSpec.toWorld(0, 74, -2, yaw, k));
        Vec3 f = forward(), l = left(), up = new Vec3(0, 1, 0);
        double A = Math.toRadians(aimY), P = Math.toRadians(aimP);
        double cA = Math.cos(A), sA = Math.sin(A), cP = Math.cos(P), sP = Math.sin(P);
        for (int i : headNodes) {
            if (i < 0 || nodes[i] == null) continue;
            Vec3 w = nodes[i].subtract(pivot);
            double a = w.dot(f), b = w.dot(l), c = w.y;
            double a1 = a * cP + c * sP, c1 = -a * sP + c * cP;      // pitch down about the left axis
            double a2 = a1 * cA - b * sA, b2 = a1 * sA + b * cA;     // yaw toward the left about the vertical
            nodes[i] = pivot.add(f.scale(a2)).add(l.scale(b2)).add(up.scale(c1));
        }
    }

    public Vec3 forward() {
        float yr = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yr), 0, Mth.cos(yr));
    }

    public Vec3 left() {
        float yr = getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(yr), 0, Mth.sin(yr));
    }

    // ------------------------------------------------------------------ flags
    public int flags() { return entityData.get(FLAGS); }

    public boolean flag(int f) { return (flags() & f) != 0; }

    void setFlag(int f, boolean on) {
        int v = flags();
        int n = on ? v | f : v & ~f;
        if (n != v) entityData.set(FLAGS, n);
    }

    public boolean isDown() { return state == State.DOWN || state == State.GETUP && clipTick < 10 || flag(F_DOWN); }

    public boolean isDeadKaiju() { return state == State.DEAD; }

    public float hpRatio() { return level().isClientSide ? entityData.get(HP) : hp / spec.maxHp; }

    public int animSeq() { return entityData.get(ANIM_SEQ); }

    public int animIndex() { return entityData.get(ANIM); }

    public int animTick() { return entityData.get(ANIM_TICK); }

    public int hitstopSynced() { return entityData.get(HITSTOP); }

    public Vec3 beamA() { Vector3f v = entityData.get(BEAM_A); return new Vec3(v.x, v.y, v.z); }

    public Vec3 beamB() { Vector3f v = entityData.get(BEAM_B); return new Vec3(v.x, v.y, v.z); }

    public float heatOf(int region) {
        int packed = region < 4 ? entityData.get(HEAT_A) : entityData.get(HEAT_B);
        return ((packed >>> ((region % 4) * 8)) & 0xFF) / 255f;
    }

    public int severMaskClient() { return entityData.get(SEVER); }

    public boolean severedClient(int g) { return (entityData.get(SEVER) & (1 << g)) != 0; }

    public float regenClient(int g) { return ((entityData.get(REGEN) >>> (g * 8)) & 0xFF) / 255f; }

    public float aimYaw() { return entityData.get(AIM_YAW); }

    public float aimPitch() { return entityData.get(AIM_PITCH); }

    public float chargeClient() { return entityData.get(CHARGE); }

    public int beamStyle() { return entityData.get(BEAM_STYLE); }

    void setBeamStyle(int s) { entityData.set(BEAM_STYLE, s); }

    public float poiseClient() { return entityData.get(POISE); }

    // ------------------------------------------------------------------ clips
    public void play(String name) {
        KaijuSpec.Clip c = spec.clip(name);
        if (c == null) {
            KaijuMod.LOG.warn("missing clip {}", name);
            return;
        }
        clip = c;
        clipTick = 0;
        clipBaseYaw = getYRot();
        entityData.set(ANIM, c.index);
        entityData.set(ANIM_SEQ, entityData.get(ANIM_SEQ) + 1);
    }

    void loopClip(String name) {
        if (clip == null || !clip.name.equals(name)) play(name);
    }

    // ------------------------------------------------------------------ attacks
    public boolean ready(KaijuSpec.Attack a) {
        Integer cd = cooldown.get(a.id);
        return cd == null || cd <= tickCount;
    }

    /** how much a move tires: longer and heavier moves cost more; guarding, dodging and roaring cost nothing */
    public float cost(KaijuSpec.Attack a) {
        if (a.cat.equals("defense") || a.cat.equals("intimidate")) return 0;
        return 4 + a.len * 5 + (a.isBreath() ? 18 : 0) + (a.tags.contains("heavy") ? 6 : 0);
    }

    public boolean canAct() {
        return state == State.IDLE && hitstop == 0;
    }

    public boolean meets(KaijuSpec.Attack a, @Nullable KaijuEntity target) {
        for (String n : a.need) {
            switch (n) {
                case "arm_l" -> { if (!partOk(spec.severGroups.indexOf("arm_l"))) return false; }
                case "arm_r" -> { if (!partOk(spec.severGroups.indexOf("arm_r"))) return false; }
                case "tail" -> { if (!partOk(spec.severGroups.indexOf("tail_tip"))) return false; }
                case "holding" -> { if (heldDebris == null) return false; }
                case "not_holding" -> { if (heldDebris != null) return false; }
                case "enemy_down" -> { if (target == null || !target.isDown()) return false; }
                case "self_down" -> { if (state != State.DOWN) return false; }
                case "enemy_low" -> { if (target == null || !(target.isDown() || target.getBbHeight() < getBbHeight() * 0.6)) return false; }
                case "breath_ready" -> { if (beam.cooldown > tickCount) return false; }
                case "can_fly" -> { if (!spec.canFly) return false; }
                case "on_ground" -> { if (flying) return false; }
                case "flying" -> { if (!flying) return false; }
                default -> { }
            }
        }
        if (flying && !a.need.contains("flying") && !a.cat.equals("intimidate")) return false;  // in the air only air moves
        if (state == State.DOWN && !a.need.contains("self_down")) return false;
        if (a.need.contains("self_down") && state != State.DOWN) return false;
        return true;
    }

    public boolean startAttack(String id) {
        KaijuSpec.Attack a = spec.attackById.get(id);
        if (a == null) return false;
        if (!(state == State.IDLE || state == State.DOWN && a.need.contains("self_down"))) return false;
        if (!ready(a) || !meets(a, brain.target)) return false;
        attack = a;
        atkTick = 0;
        phase = 0;
        phaseTick = 0;
        hitOnce.clear();
        state = State.ACTION;
        cooldown.put(a.id, tickCount + (int) (a.cooldown * 20));
        stats.computeIfAbsent(a.id, k -> new float[2])[0] += 1;
        stamina = Math.max(0, stamina - cost(a));
        KaijuMod.LOG.info("[KAIJU] #{} starts {} dist={} stamina={}", getId(), a.id, brain.target == null ? -1 : (int) brain.target.distanceTo(this), (int) stamina);
        if (a.phases != null) {
            phaseLen = (int) (a.phases.get(0).secs() * 20);
            play(a.phases.get(0).clip());
        } else {
            play(a.clip);
        }
        if (a.isBreath()) beam.begin(a);
        if (a.need.contains("self_down")) downTicks = 0;
        return true;
    }

    void endAttack() {
        KaijuSpec.Attack a = attack;
        attack = null;
        beam.stop();
        setFlag(F_CHARGING, false);
        if (holding != null && a != null && !a.tags.contains("grapple")) KaijuCombat.releaseVictim(this, Vec3.ZERO);
        if (a != null && a.need.contains("self_down")) {
            if (a.tags.contains("getup")) { state = State.IDLE; setFlag(F_DOWN, false); refreshDimensions(); }
            else if (a.id.equals("roll_away")) { state = State.DOWN; downOnFront = false; play("lying_back"); downTicks = 10; }
            else { state = State.DOWN; play(downOnFront ? "lying_front" : "lying_back"); }
            return;
        }
        state = State.IDLE;
        if (holding != null) KaijuCombat.releaseVictim(this, Vec3.ZERO);
    }

    private void tickAttack() {
        KaijuSpec.Attack a = attack;
        float tPrev = atkTick / 20f;
        atkTick++;
        float t = atkTick / 20f;
        // multi-clip attacks (breath): advance phases; the fire phase can be held longer by a controlling player
        if (a.phases != null) {
            phaseTick++;
            if (phaseTick >= phaseLen) {
                boolean extend = phase == 1 && a.isBreath() && controller != null && input.down(ControlInput.BREATH) && phaseTick < 20 * 7;
                if (!extend) {
                    phase++;
                    if (phase >= a.phases.size()) { endAttack(); return; }
                    phaseTick = 0;
                    phaseLen = (int) (a.phases.get(phase).secs() * 20);
                    play(a.phases.get(phase).clip());
                }
            }
            setFlag(F_CHARGING, phase == 0);
            entityData.set(CHARGE, phase == 0 ? Math.min(1f, phaseTick / (float) phaseLen) : phase == 1 ? 1f : Math.max(0f, 1f - phaseTick / (float) phaseLen));
            beam.phase(phase, phaseTick, phaseLen);
        } else if (atkTick >= (int) (a.len * 20)) {
            endAttack();
            return;
        }
        float at = a.phases == null ? t : -1;
        if (a.phases == null) {
            for (KaijuSpec.Event e : a.events) {
                if (e.t() > tPrev && e.t() <= t) KaijuCombat.event(this, a, e);
            }
            for (int hi = 0; hi < a.hits.size(); hi++) {
                KaijuSpec.Hit h = a.hits.get(hi);
                if (at >= h.t0() && at <= h.t1()) KaijuCombat.sweepHits(this, a, h, hi);
            }
            for (KaijuSpec.Move m : a.moves) {
                if (at >= m.t0() && at <= m.t1()) {
                    // root motion: blend toward the move's velocity (film-scale speeds: a charge is ~15 m/s)
                    float kk = k() / 20f * 0.22f;
                    Vec3 d = forward().scale(m.fwd() * kk).add(left().scale(m.side() * kk));
                    knock = knock.add(d.subtract(knock.multiply(1, 0, 1)).scale(0.35));
                }
            }
        } else {
            for (KaijuSpec.Event e : a.events) {
                if (e.t() > tPrev && e.t() <= t) KaijuCombat.event(this, a, e);
            }
        }
    }

    // ------------------------------------------------------------------ main tick
    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // the body always faces where the entity faces (vanilla would let it lag behind fast turns)
            yBodyRot = getYRot();
            yBodyRotO = yRotO;
            yHeadRot = getYRot();
            yHeadRotO = yRotO;
            KaijuClientHooks.tick(this);
            return;
        }
        serverTick();
    }

    @Override
    public void aiStep() {
        // vanilla movement is replaced by serverTick (travel() does nothing), but the client still needs
        // LivingEntity's position/rotation interpolation from server updates, which lives here
        super.aiStep();
    }


    @Override
    public void travel(Vec3 v) { }

    static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> TICKET =
            net.minecraft.server.level.TicketType.create("kaiju", Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 60);

    private void serverTick() {
        if (nodes[0] == null) computeNodes();
        // a kaiju keeps its own surroundings simulated, so a fight goes on even far from any player
        if (tickCount % 20 == 0 && level() instanceof ServerLevel sl) {
            net.minecraft.world.level.ChunkPos cp = chunkPosition();
            sl.getChunkSource().addRegionTicket(TICKET, cp, 5, cp);
        }
        regenAndDecay();
        if (hitstop > 0) {
            hitstop--;
            entityData.set(HITSTOP, hitstop);
            computeNodes();
            return;
        }
        entityData.set(HITSTOP, 0);
        if (controller != null && (controller.isRemoved() || controller.getVehicle() != this)) releaseControl();
        setFlag(F_CONTROLLED, controller != null);

        switch (state) {
            case DEAD -> {
                if (++deathTicks > 100) {
                    if (level() instanceof ServerLevel sl)
                        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY() + 4, getZ(), 6, 6, 3, 6, 0);
                    discard();
                }
            }
            case HELD -> {
                if (heldBy == null || heldBy.isRemoved() || heldBy.holding != this) {
                    heldBy = null;
                    knockdown(Vec3.ZERO, "b");
                }
                loopClip("held");
            }
            case THROWN -> loopClip("thrown");
            case DOWN -> {
                downTicks++;
                if (pinTicks > 0) { pinTicks--; loopClip("pinned"); }
                else loopClip(downOnFront ? "lying_front" : "lying_back");
                brain.tickDown();
            }
            case GETUP -> {
                if (clipTick >= clip.ticks - 1) { state = State.IDLE; setFlag(F_DOWN, false); refreshDimensions(); kdImmune = 70; }
            }
            case REACT -> {
                if (clipTick >= clip.ticks - 1) {
                    if (clip.name.startsWith("knockdown")) {
                        state = State.DOWN;
                        downTicks = 0;
                        play(downOnFront ? "lying_front" : "lying_back");
                    } else state = State.IDLE;
                }
            }
            case ACTION -> {
                if (attack == null) state = State.IDLE;
                else tickAttack();
            }
            case IDLE -> { }
        }
        if (state == State.IDLE || state == State.DOWN) {
            if (controller != null && !flag(F_AI_ASSIST)) brain.tickControlled();
            else brain.tick();
        }
        if (state == State.IDLE) locomotion();

        clipTick++;
        if (clip.yaw != null && clip.yaw.length > 0) {
            // the clip turns the whole body (a spin, turning round to slam): the entity really rotates
            float y = clip.yaw[Math.min(clip.yaw.length - 1, clipTick)];
            setYRot(clipBaseYaw - y);
            faceYaw = getYRot();
        }
        entityData.set(ANIM_TICK, clipTick);
        clipImpacts();
        physics();
        computeNodes();
        KaijuCombat.bodyContacts(this);
        WorldSmash.carveBody(this);
        beam.tick();
        if (holding != null) KaijuCombat.carryVictim(this);
        if (heldDebris != null) heldDebris.followHands(this);
        sync();
    }

    private void locomotion() {
        float speed = wantSpeed;
        if (flying) {
            float diff = Mth.wrapDegrees(faceYaw - getYRot());
            setYRot(getYRot() + Mth.clamp(diff, -6f, 6f));
            loopClip(Math.abs(speed) > 0.01f ? "fly" : "hover");
            float v = Math.abs(speed) * k() / 20f;
            Vec3 dir = forward().scale(v);
            knock = knock.add(dir.subtract(knock.multiply(1, 0, 1)).scale(0.12));
            return;
        }
        float diff = Mth.wrapDegrees(faceYaw - getYRot());
        float rate = controller != null && !flag(F_AI_ASSIST) ? 9f : spec.turnDeg * (speed > 0 ? 1.4f : 1f);  // a rider turns at once
        float turn = Mth.clamp(diff, -rate, rate);
        setYRot(getYRot() + turn);
        boolean hold = heldDebris != null;
        if (Math.abs(speed) > 0.01f) {
            float px = speed > 0 ? speed : -speed;
            String name = hold ? "hold_walk" : speed < 0 ? "walk_back" : (px > spec.walkPx * 1.2f ? "walk_fast" : "walk");
            loopClip(name);
            float v = speed * k() / 20f;
            Vec3 dir = forward().scale(moveFwd).add(left().scale(moveSide));
            if (dir.lengthSqr() < 1e-4) dir = forward();
            dir = dir.normalize().scale(Math.abs(v) * Math.signum(speed == 0 ? 1 : 1));
            knock = knock.add(dir.subtract(knock.multiply(1, 0, 1)).scale(controller != null ? 0.45 : 0.25));
        } else if (Math.abs(diff) > 6) {
            loopClip(hold ? "hold_walk" : "turn");
        } else {
            loopClip(hold ? "hold_idle" : (stamina < 40 || hp < spec.maxHp * 0.3f) ? "tired" : "idle");
        }
    }

    private void clipImpacts() {
        if (clip == null || clip.impacts.length == 0) return;
        float t0 = (clipTick - 1) / 20f, t1 = clipTick / 20f;
        float len = clip.len;
        for (float imp : clip.impacts) {
            float a = clip.loop ? t0 % len : t0, b = clip.loop ? t1 % len : t1;
            if (b < a) b += len;
            if (imp > a && imp <= b || imp + len > a && imp + len <= b) KaijuCombat.impact(this, clip.name, imp);
        }
    }

    private void physics() {
        float k = k();
        // ground under the feet (solid blocks, not water: a kaiju walks along the sea floor)
        int fl = spec.node("foot_l"), fr = spec.node("foot_r"), hp_ = spec.node("hips");
        double ground = groundAt(position());
        if (fl >= 0 && nodes[fl] != null && !isDown()) ground = Math.max(groundAt(nodes[fl]), groundAt(nodes[fr]));
        else if (hp_ >= 0 && nodes[hp_] != null) ground = Math.max(ground, groundAt(nodes[hp_]));
        double y = getY();
        if (flying && (state == State.HELD || state == State.DEAD || state == State.REACT && clip.name.startsWith("knockdown"))) flying = false; // knocked out of the sky
        boolean flying = state == State.THROWN || vy > 0.05;
        if (this.flying) {
            // on the wings: hold the cruising height above whatever is below, glide down onto it when landing
            flyTicks++;
            double want = Math.max(ground + 18 * k / 0.5, flyAlt);
            vy += Mth.clamp((want - y) * 0.03, -0.12, 0.12) - vy * 0.12;
            vy = Mth.clamp(vy, -1.6, 1.2);
            y += vy;
            if (y < ground) { y = ground; vy = 0; }
            onGround = false;
            airTicks = 0;
        } else if (state == State.HELD) {
            vy = 0;
        } else if (!flying && y - ground < 1.2 && ground - y < 14 * k / 0.5) {
            // step up/down smoothly onto terrain, like the legs absorbing it
            y += Mth.clamp(ground - y, -1.5, 1.0);
            if (airTicks > 4 && vy < -0.6) KaijuCombat.landing(this, -vy);
            vy = 0;
            onGround = true;
            airTicks = 0;
        } else {
            vy = Math.max(-3.0, vy - 0.08);
            y += vy;
            onGround = false;
            airTicks++;
            if (y <= ground) {
                if (state == State.THROWN || -vy > 0.6) KaijuCombat.landing(this, -vy);
                y = ground;
                vy = 0;
                onGround = true;
                airTicks = 0;
            }
        }
        double sp = knock.horizontalDistance(), cap = (this.flying ? 3.2 : 2.2) * k() / 0.5;
        if (sp > cap) knock = knock.scale(cap / sp);
        if (state != State.HELD) {
            Vec3 p = position().add(knock.x, 0, knock.z);
            setPos(p.x, y, p.z);
        }
        double fric = state == State.THROWN ? 0.99 : (state == State.DOWN || state == State.REACT ? 0.86 : controller != null && state == State.IDLE && wantSpeed == 0 ? 0.6 : 0.8);
        knock = knock.multiply(fric, 1, fric);
        if (knock.lengthSqr() < 1e-5) knock = Vec3.ZERO;
        if (y < level().getMinBuildHeight() - 20) discard();
    }

    public double groundAt(Vec3 p) {
        BlockPos bp = BlockPos.containing(p.x, 0, p.z);
        if (!level().hasChunkAt(bp)) return getY();
        return level().getHeight(Heightmap.Types.OCEAN_FLOOR, bp.getX(), bp.getZ());
    }

    private void regenAndDecay() {
        poise = Math.min(100, poise + (state == State.DOWN ? 1.2f : 0.45f));
        stamina = Math.min(100, stamina + (state == State.IDLE ? (Math.abs(wantSpeed) < 0.01f ? 0.32f : 0.16f) : state == State.DOWN ? 0.2f : 0f));
        for (int i = 0; i < heat.length; i++) heat[i] = Math.max(0, heat[i] - 0.0015f);
        for (int g = 0; g < regen.length && g < spec.severGroups.size(); g++) {
            if ((severMask & (1 << g)) == 0) continue;
            if (regenDelay[g] > 0) { regenDelay[g]--; continue; }
            if (regen[g] < 1f) {
                regen[g] = Math.min(1f, regen[g] + 1f / 600f);
                if (regen[g] >= 1f) {
                    severMask &= ~(1 << g);
                    regen[g] = 0;
                    partHp[g] = partMaxHp();
                    level().playSound(null, getX(), getY() + 20, getZ(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 6f, 0.4f);
                }
            }
        }
        if (state != State.DEAD && hp < spec.maxHp && tickCount - lastHurt > 200) hp = Math.min(spec.maxHp, hp + spec.maxHp * 0.00008f);
        if (guardTicks > 0) guardTicks--;
        if (kdImmune > 0) kdImmune--;
        if (dodgeTicks > 0) dodgeTicks--;
        if (parryTicks > 0) parryTicks--;
        setFlag(F_GUARD, guardTicks > 0);
    }

    int lastHurt;

    public int lastHurtTick() { return lastHurt; }

    private void sync() {
        entityData.set(HP, Math.max(0, hp / spec.maxHp));
        entityData.set(POISE, (int) poise);
        int a = 0, b = 0;
        for (int i = 0; i < 4; i++) a |= ((int) (Mth.clamp(heat[i], 0, 1) * 255)) << (i * 8);
        for (int i = 4; i < 8; i++) b |= ((int) (Mth.clamp(heat[i], 0, 1) * 255)) << ((i - 4) * 8);
        entityData.set(HEAT_A, a);
        entityData.set(HEAT_B, b);
        entityData.set(SEVER, severMask);
        int r = 0;
        for (int g = 0; g < 3; g++) r |= ((int) (regen[g] * 255)) << (g * 8);
        entityData.set(REGEN, r);
        setFlag(F_HOLDING, heldDebris != null);
        setFlag(F_DOWN, state == State.DOWN || state == State.REACT && clip.name.startsWith("knockdown"));
        setFlag(F_HELD, state == State.HELD);
        setFlag(F_THROWN, state == State.THROWN);
        setFlag(F_DEAD, state == State.DEAD);
        setFlag(F_FLYING, flying);
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
        yBodyRotO = yRotO;
    }

    void syncBeam(Vec3 a, Vec3 b, boolean on, boolean clash) {
        entityData.set(BEAM_A, new Vector3f((float) a.x, (float) a.y, (float) a.z));
        entityData.set(BEAM_B, new Vector3f((float) b.x, (float) b.y, (float) b.z));
        setFlag(F_FIRING, on);
        setFlag(F_CLASH, clash);
    }

    public void setAiAssist(boolean on) { setFlag(F_AI_ASSIST, on); }

    public void setAim(float yaw, float pitch) {
        entityData.set(AIM_YAW, yaw);
        entityData.set(AIM_PITCH, pitch);
    }

    // ------------------------------------------------------------------ being hit
    public void react(String dir, float dv) {
        if (state == State.DEAD || state == State.HELD) return;
        if (state == State.DOWN || state == State.GETUP && clipTick < 20) {
            downTicks = Math.max(0, downTicks - 10);
            return;
        }
        if (attack != null && dv < 0.04f && attack.tags.contains("heavy")) return; // super armour on heavy moves
        boolean big = (dv > 0.3f || poise <= 0) && kdImmune <= 0;
        if (big) { knockdown(Vec3.ZERO, dir); return; }
        if (attack != null) endAttack();
        state = State.REACT;
        play((dv > 0.07f ? "stagger_" : "flinch_") + dir);
    }

    public void knockdown(Vec3 push, String dir) {
        if (state == State.DEAD) return;
        if (attack != null) endAttack();
        if (holding != null) KaijuCombat.releaseVictim(this, Vec3.ZERO);
        if (heldDebris != null) heldDebris.drop();
        heldBy = null;
        state = State.REACT;
        String kd = switch (dir) { case "b" -> "knockdown_f"; case "l" -> "knockdown_l"; case "r" -> "knockdown_r"; default -> "knockdown_b"; };
        downOnFront = kd.equals("knockdown_f");
        play(kd);
        knock = knock.add(push);
        poise = 100;
        refreshDimensions();
    }

    public void getUp() {
        if (state != State.DOWN) return;
        state = State.GETUP;
        play(downOnFront ? "getup_front" : "getup_back");
    }

    public void die(String why) {
        if (state == State.DEAD) return;
        if (attack != null) endAttack();
        if (holding != null) KaijuCombat.releaseVictim(this, Vec3.ZERO);
        state = State.DEAD;
        deathTicks = 0;
        play("death");
        level().playSound(null, getX(), getY() + 30, getZ(), SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 12f, 0.5f);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (level().isClientSide) return false;
        if (src.is(DamageTypes.FELL_OUT_OF_WORLD) || src.is(DamageTypes.GENERIC_KILL)) {
            hp = 0;
            die("kill");
            return super.hurt(src, Float.MAX_VALUE);
        }
        if (src.getEntity() == this || src.getDirectEntity() == this) return false;
        if (src.is(DamageTypeTags.IS_FALL) || src.is(DamageTypeTags.IS_FIRE) || src.is(DamageTypes.IN_WALL) || src.is(DamageTypes.DROWN)
                || src.is(DamageTypes.CRAMMING) || src.is(DamageTypes.FLY_INTO_WALL)) return false;
        if (src.is(DamageTypeTags.IS_EXPLOSION)) amount *= 0.2f;
        if (src.getEntity() instanceof LivingEntity le && !(le instanceof KaijuEntity)) brain.provoke(le);
        damage(amount * 0.6f, -1, "blunt");
        return true;
    }

    /** raw damage from kaiju combat, already scaled */
    public void damage(float amount, int node, String kind) {
        if (state == State.DEAD) return;
        if (dodgeTicks > 0) return;
        if (guardTicks > 0) amount *= 0.3f;
        hp -= amount;
        lastHurt = tickCount;
        if (node >= 0) {
            KaijuSpec.Node n = spec.nodes.get(node);
            if (n.sever() >= 0 && partOk(n.sever())) {
                float mul = kind.equals("cut") || kind.equals("bite") || kind.equals("beam") ? 1.4f : 0.45f;
                partHp[n.sever()] -= amount * mul;
                if (partHp[n.sever()] <= 0 && !kind.equals("blunt") && !kind.equals("stomp")) sever(n.sever());
            }
        }
        if (hp <= 0) die("combat");
    }

    public void sever(int g) {
        if (g < 0 || (severMask & (1 << g)) != 0) return;
        severMask |= 1 << g;
        KaijuMod.LOG.info("[KAIJU] #{} lost {}", getId(), spec.severGroups.get(g));
        regen[g] = 0;
        regenDelay[g] = 140;
        if (level() instanceof ServerLevel sl) {
            String group = spec.severGroups.get(g);
            int node = group.equals("arm_l") ? spec.node("elbow_l") : group.equals("arm_r") ? spec.node("elbow_r") : spec.node("tail11");
            Vec3 at = nodePos(node);
            SeveredPartEntity part = new SeveredPartEntity(KaijuMod.SEVERED.get(), sl);
            part.setup(this, g, at);
            sl.addFreshEntity(part);
            sl.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new Vector3f(0.45f, 0.02f, 0.02f), 4f), at.x, at.y, at.z, 120, 2, 2, 2, 0.2);
            level().playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.HOSTILE, 10f, 0.3f);
            level().playSound(null, at.x, at.y, at.z, SoundEvents.ENDER_DRAGON_HURT, SoundSource.HOSTILE, 10f, 0.5f);
        }
        if (attack != null && !partOk(g)) endAttack();
        if (heldDebris != null && g < 2) heldDebris.drop();
    }

    // ------------------------------------------------------------------ control
    public void takeControl(ServerPlayer p) {
        if (controller != null && controller != p) releaseControl();
        controller = p;
        allowDismount = false;
        p.startRiding(this, true);
        setFlag(F_CONTROLLED, true);
    }

    public void releaseControl() {
        ServerPlayer p = controller;
        controller = null;
        input = new ControlInput();
        setFlag(F_CONTROLLED, false);
        if (p != null && p.getVehicle() == this) {
            allowDismount = true;
            p.stopRiding();
            allowDismount = false;
            Vec3 safe = position().add(forward().scale(getBbWidth() * 1.2)).add(0, 2, 0);
            p.teleportTo(safe.x, groundAt(safe) + 1, safe.z);
        }
    }

    @Override
    protected net.minecraft.world.InteractionResult mobInteract(Player p, net.minecraft.world.InteractionHand hand) {
        var stack = p.getItemInHand(hand);
        boolean ctrl = stack.is(KaijuMod.CONTROLLER.get());
        if (!ctrl && !(stack.isEmpty() && p.isShiftKeyDown())) return net.minecraft.world.InteractionResult.PASS;
        if (level().isClientSide) return net.minecraft.world.InteractionResult.SUCCESS;
        if (p instanceof ServerPlayer sp) {
            if (owner != null && !owner.equals(sp.getUUID()) && !sp.getAbilities().instabuild) {
                sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.kaiju.not_yours"), true);
                return net.minecraft.world.InteractionResult.CONSUME;
            }
            if (state == State.DEAD) return net.minecraft.world.InteractionResult.CONSUME;
            takeControl(sp);
            sp.displayClientMessage(net.minecraft.network.chat.Component.translatable("msg.kaiju.controlling"), true);
        }
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    @Override
    protected boolean canAddPassenger(Entity e) { return getPassengers().isEmpty() && e instanceof Player; }

    @Override
    public boolean shouldRiderSit() { return false; }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() { return null; }

    @Override
    protected void positionRider(Entity p, Entity.MoveFunction f) {
        // chase camera: the rider's eye floats behind and above the head, orbiting with where the rider looks
        float k = k();
        int head = spec.node("head");
        Vec3 h = position().add(0, 88 * k, 0);
        float yr = p.getYRot() * Mth.DEG_TO_RAD;
        float pr = Mth.clamp(p.getXRot(), -60, 60) * Mth.DEG_TO_RAD;
        double dist = 70 * k;
        Vec3 look = new Vec3(-Mth.sin(yr) * Mth.cos(pr), -Mth.sin(pr), Mth.cos(yr) * Mth.cos(pr));
        Vec3 cam = h.subtract(look.scale(dist)).add(0, 16 * k, 0);
        double eye = p.getEyeHeight();
        f.accept(p, cam.x, cam.y - eye, cam.z);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity p) {
        Vec3 safe = position().add(forward().scale(getBbWidth() * 1.2));
        return new Vec3(safe.x, groundAt(safe) + 1, safe.z);
    }

    // ------------------------------------------------------------------ misc entity behaviour
    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity e) { }
    @Override public void push(Entity e) { }
    @Override public boolean canBeCollidedWith() { return false; }
    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean causeFallDamage(float a, float b, DamageSource s) { return false; }
    @Override protected void checkFallDamage(double y, boolean g, net.minecraft.world.level.block.state.BlockState s, BlockPos p) { }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean fireImmune() { return true; }
    @Override public boolean isInvulnerableTo(DamageSource s) { return s.is(DamageTypes.IN_WALL) || super.isInvulnerableTo(s); }
    @Override public int getMaxHeadYRot() { return 1; }
    @Override public boolean shouldShowName() { return false; }
    @Override public boolean ignoreExplosion() { return true; }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) { return d < 1024 * 1024; }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putFloat("KaijuHp", hp);
        t.putFloat("KaijuScale", entityData.get(SCALE));
        t.putInt("Sever", severMask);
        if (owner != null) t.putUUID("Owner", owner);
        t.putString("Special", special);
        t.putBoolean("Flying", flying);
        t.putDouble("FlyAlt", flyAlt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.contains("KaijuHp")) hp = t.getFloat("KaijuHp");
        if (t.contains("KaijuScale")) setScale(t.getFloat("KaijuScale"));
        severMask = t.getInt("Sever");
        if (t.hasUUID("Owner")) owner = t.getUUID("Owner");
        if (t.contains("Special")) special = t.getString("Special");
        flying = t.getBoolean("Flying");
        flyAlt = t.getDouble("FlyAlt");
    }

    @Override
    public void remove(RemovalReason r) {
        beam.stop();
        if (holding != null) KaijuCombat.releaseVictim(this, Vec3.ZERO);
        if (heldDebris != null) heldDebris.drop();
        super.remove(r);
    }

    // ------------------------------------------------------------------ GeckoLib
    public int clientSeq = -1;
    public RawAnimation clientRaw;

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
        AnimationController<KaijuEntity> c = new AnimationController<>(this, "main", 3, st -> {
            KaijuEntity e = st.getAnimatable();
            int seq = e.animSeq();
            if (seq != e.clientSeq || e.clientRaw == null) {
                int idx = e.animIndex();
                String name = idx >= 0 && idx < e.spec.clipNames.size() ? e.spec.clipNames.get(idx) : "idle";
                KaijuSpec.Clip cl = e.spec.clip(name);
                e.clientRaw = RawAnimation.begin().then("animation.kaiju." + name,
                        cl != null && cl.loop ? Animation.LoopType.LOOP : Animation.LoopType.HOLD_ON_LAST_FRAME);
                if (e.clientSeq != -1) st.getController().forceAnimationReset();
                e.clientSeq = seq;
            }
            return st.setAndContinue(e.clientRaw);
        });
        c.setAnimationSpeedHandler(e -> e.hitstopSynced() > 0 ? 0.04 : 1.0);
        reg.add(c);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return geoCache; }
}
