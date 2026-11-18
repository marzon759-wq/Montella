package com.krolasyon.livingmotion.client.model;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoReplacedEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.ArrayList;
import java.util.List;

/**
 * The GeckoLib stand-in animatable for one vanilla entity type. GeckoLib keeps one animation manager per entity id;
 * the controllers read the real entity from the animation state every frame and pick the animation from its state.
 */
public class ReplacedMob implements GeoReplacedEntity {
    private static final List<ReplacedMob> ALL = new ArrayList<>();

    public final EntityType<?> type;
    public final MobKind kind;
    private final Cache cache = new Cache(this);
    private final Int2ObjectOpenHashMap<Track> tracks = new Int2ObjectOpenHashMap<>();

    final RawAnimation idle, walk, run, attack, hurt, death, special, graze;

    public ReplacedMob(EntityType<?> type, MobKind kind) {
        this.type = type;
        this.kind = kind;
        String p = kind.animPrefix;
        switch (kind.family) {
            case ZOMBIE -> {
                idle = loop(p + "zombie_idle"); walk = loop(p + "zombie_walk"); run = loop(p + "zombie_chase");
                attack = once(p + "zombie_attack"); special = null; graze = null;
            }
            case SKELETON -> {
                idle = loop(p + "skeleton_idle"); walk = loop(p + "skeleton_walk"); run = loop(p + "skeleton_run");
                attack = once(p + "skeleton_attack"); special = loop(p + "skeleton_draw"); graze = null;
            }
            case CREEPER -> {
                idle = loop(p + "idle"); walk = loop(p + "walk"); run = walk; attack = null;
                special = loop(p + "fuse"); graze = null;
            }
            case SPIDER -> {
                idle = loop(p + "idle"); walk = loop(p + "walk"); run = walk; attack = once(p + "attack");
                special = null; graze = null;
            }
            case QUADRUPED -> {
                idle = loop(p + "idle"); walk = loop(p + "walk"); run = loop(p + "run"); attack = null;
                special = null; graze = once(p + "graze");
            }
            default -> { // chicken
                idle = loop(p + "idle"); walk = loop(p + "walk"); run = walk; attack = null;
                special = loop(p + "flap"); graze = null;
            }
        }
        hurt = once(p + "hurt");
        death = RawAnimation.begin().thenPlayAndHold(p + "death");
        ALL.add(this);
    }

    private static RawAnimation loop(String n) { return RawAnimation.begin().thenLoop(n); }

    private static RawAnimation once(String n) { return RawAnimation.begin().thenPlay(n); }

    public static void forget(int id) {
        for (ReplacedMob m : ALL) {
            m.tracks.remove(id);
            m.cache.remove(id);
        }
    }

    public Track track(Entity e) {
        Track t = tracks.get(e.getId());
        if (t == null) {
            t = new Track();
            t.seed = (int) (e.getUUID().getLeastSignificantBits() & 0x7fffffff);
            tracks.put(e.getId(), t);
        }
        return t;
    }

    @Override
    public EntityType<?> getReplacingEntityType() { return type; }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
        reg.add(new AnimationController<>(this, "move", 4, this::move));
        reg.add(new AnimationController<>(this, "action", 1, this::action));
    }

    private static LivingEntity ent(AnimationState<?> st) {
        return st.getData(DataTickets.ENTITY) instanceof LivingEntity l ? l : null;
    }

    public static boolean aimingBow(LivingEntity e) {
        return e instanceof Mob m && m.isAggressive() && m.getMainHandItem().getItem() instanceof ProjectileWeaponItem;
    }

    // ---------------------------------------------------------------------------------------------------------
    private PlayState move(AnimationState<ReplacedMob> st) {
        LivingEntity e = ent(st);
        if (e == null) return PlayState.STOP;
        AnimationController<ReplacedMob> c = st.getController();
        c.setAnimationSpeed(1.0);
        if (e.deathTime > 0 || !e.isAlive()) return st.setAndContinue(death);
        Track t = track(e);
        float amount = st.getLimbSwingAmount();
        boolean moving = st.isMoving() || amount > 0.15f;
        Vec3 v = e.getDeltaMovement();
        double speed = Math.sqrt(v.x * v.x + v.z * v.z);

        // timed idle variants (cows/pigs lower their head and graze now and then)
        if (graze != null && e.tickCount != t.lastTick) {
            t.lastTick = e.tickCount;
            if (!moving && t.variant == null && e.getRandom().nextInt(500) == 0) {
                t.variant = graze;
                t.variantUntil = e.tickCount + 60;
                c.forceAnimationReset();
            }
        }
        if (t.variant != null) {
            if (moving || e.tickCount > t.variantUntil || e.hurtTime > 0) t.variant = null;
            else return st.setAndContinue(t.variant);
        }

        switch (kind.family) {
            case ZOMBIE -> {
                if (moving) {
                    boolean chase = e instanceof Mob m && m.isAggressive();
                    c.setAnimationSpeed(chase ? 0.7 + amount * 0.8 : 0.5 + amount * 1.3);
                    return st.setAndContinue(chase ? run : walk);
                }
                return st.setAndContinue(idle);
            }
            case SKELETON -> {
                if (aimingBow(e)) {
                    if (moving) { c.setAnimationSpeed(0.6 + amount); return st.setAndContinue(walk); }
                    return st.setAndContinue(special);
                }
                if (moving) {
                    boolean chase = e instanceof Mob m && m.isAggressive();
                    c.setAnimationSpeed(0.6 + amount);
                    return st.setAndContinue(chase ? run : walk);
                }
                return st.setAndContinue(idle);
            }
            case CREEPER -> {
                if (e instanceof Creeper cr && cr.getSwelling(st.getPartialTick()) > 0.01f) return st.setAndContinue(special);
                if (moving) { c.setAnimationSpeed(0.6 + amount * 1.2); return st.setAndContinue(walk); }
                return st.setAndContinue(idle);
            }
            case SPIDER -> {
                if (moving || e.onClimbable()) { c.setAnimationSpeed(0.6 + amount * 1.4); return st.setAndContinue(walk); }
                return st.setAndContinue(idle);
            }
            case QUADRUPED -> {
                if (moving) {
                    boolean panic = speed > 0.14 || amount > 0.75f;
                    c.setAnimationSpeed(panic ? 0.8 + amount * 0.5 : 0.5 + amount * 1.3);
                    return st.setAndContinue(panic ? run : walk);
                }
                return st.setAndContinue(idle);
            }
            default -> {
                if (!e.onGround() && v.y < -0.02 && !e.isInWater()) return st.setAndContinue(special);
                if (moving) { c.setAnimationSpeed(0.6 + amount * 1.4); return st.setAndContinue(walk); }
                return st.setAndContinue(idle);
            }
        }
    }

    private PlayState action(AnimationState<ReplacedMob> st) {
        LivingEntity e = ent(st);
        if (e == null) return PlayState.STOP;
        Track t = track(e);
        float age = e.tickCount + st.getPartialTick();
        if (e.deathTime > 0 || !e.isAlive()) { t.action = null; return PlayState.STOP; }

        boolean newHurt = e.hurtTime > t.lastHurt;
        t.lastHurt = e.hurtTime;
        boolean newSwing = e.swinging && (!t.wasSwinging || e.swingTime < t.lastSwingTime);
        t.wasSwinging = e.swinging;
        t.lastSwingTime = e.swingTime;

        if (newSwing) {
            t.attackFlash = age;
            RawAnimation a = (kind.family == MobKind.Family.SKELETON && aimingBow(e)) ? null : attack;
            if (a != null) start(st, t, a, kind.family == MobKind.Family.SPIDER ? 12 : 10, age);
        } else if (newHurt) {
            t.hurtFlash = age;
            start(st, t, hurt, 9, age);
        }
        if (t.action != null) {
            if (age - t.actionStart > t.actionLen) { t.action = null; return PlayState.STOP; }
            return st.setAndContinue(t.action);
        }
        return PlayState.STOP;
    }

    private static void start(AnimationState<ReplacedMob> st, Track t, RawAnimation a, float lenTicks, float age) {
        t.action = a;
        t.actionStart = age;
        t.actionLen = lenTicks;
        st.getController().forceAnimationReset();
    }

    /** Instance cache that can drop the managers of entities that left the level. */
    static final class Cache extends SingletonAnimatableInstanceCache {
        Cache(ReplacedMob m) { super(m); }

        void remove(long id) { managers.remove(id); }
    }
}
