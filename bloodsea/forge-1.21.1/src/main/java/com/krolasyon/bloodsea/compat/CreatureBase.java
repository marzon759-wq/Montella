package com.krolasyon.bloodsea.compat;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/** GeckoLib and synched data plumbing for the living things of the Blood Sea (Forge 1.21.1 / GeckoLib 4.8). */
public abstract class CreatureBase extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(CreatureBase.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected CreatureBase(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes(double health, double speed) {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.FOLLOW_RANGE, 64.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 1.1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(STATE, 0);
    }

    public int getState() { return this.entityData.get(STATE); }
    public void setState(int s) { this.entityData.set(STATE, s); }

    /** the creature's name in its animation file: animation.KIND.name */
    public abstract String kind();
    /** the animation that should be playing now */
    public abstract String loopAnim();
    /** one-shot animations that can be fired on top */
    public abstract String[] actionNames();
    /** animations that play once and freeze on their last frame when they are the loop */
    public boolean holds(String anim) { return false; }

    public void playAction(String name) { this.triggerAnim("action", name); }

    /** bones that turn toward what the creature looks at, and how far (0..1) */
    public String[] lookBones() { return new String[0]; }
    public float lookWeight() { return 1f; }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
        reg.add(new AnimationController<>(this, "move", 6, st -> {
            String a = loopAnim();
            String full = "animation." + kind() + "." + a;
            return st.setAndContinue(holds(a) ? RawAnimation.begin().thenPlayAndHold(full) : RawAnimation.begin().thenLoop(full));
        }));
        AnimationController<CreatureBase> act = new AnimationController<>(this, "action", 3, st -> PlayState.STOP);
        for (String a : actionNames()) act.triggerableAnim(a, RawAnimation.begin().thenPlay("animation." + kind() + "." + a));
        reg.add(act);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
