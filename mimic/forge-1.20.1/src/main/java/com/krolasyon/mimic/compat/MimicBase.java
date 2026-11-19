package com.krolasyon.mimic.compat;

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
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/** GeckoLib + synched data plumbing for the creature (Forge 1.20.1). */
public abstract class MimicBase extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(MimicBase.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(MimicBase.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected MimicBase(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.1f);
    }

    public static AttributeSupplier.Builder baseAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 96.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, 0);
        this.entityData.define(FLAGS, 0);
    }

    public int getState() { return this.entityData.get(STATE); }
    public void setStateRaw(int s) { this.entityData.set(STATE, s); }
    public int getFlags() { return this.entityData.get(FLAGS); }
    public void setFlags(int f) { this.entityData.set(FLAGS, f); }

    /** the looping animation that should play now, without the animation.mimic. prefix */
    public abstract String loopAnim();
    public abstract String[] actionNames();

    public void playAction(String name) { this.triggerAnim("action", name); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
        reg.add(new AnimationController<>(this, "move", 6, st -> st.setAndContinue(RawAnimation.begin().thenLoop("animation.mimic." + loopAnim()))));
        AnimationController<MimicBase> act = new AnimationController<>(this, "action", 2, st -> PlayState.STOP);
        for (String a : actionNames()) {
            boolean hold = a.equals("death");
            act.triggerableAnim(a, hold ? RawAnimation.begin().thenPlayAndHold("animation.mimic." + a) : RawAnimation.begin().thenPlay("animation.mimic." + a));
        }
        reg.add(act);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
