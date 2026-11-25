package com.krolasyon.bloodsea.compat;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** GeckoLib, synched data and smooth movement for the raft (Forge 1.20.1). */
public abstract class RaftBase extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(RaftBase.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected int lerpSteps;
    protected double lerpX, lerpY, lerpZ, lerpYRot;

    protected RaftBase(EntityType<?> type, Level level) { super(type, level); }

    @Override
    protected void defineSynchedData() { this.entityData.define(FLAGS, 0); }

    public int getFlags() { return this.entityData.get(FLAGS); }
    public void setFlags(int f) { this.entityData.set(FLAGS, f); }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps, boolean teleport) {
        lerpX = x; lerpY = y; lerpZ = z; lerpYRot = yRot;
        lerpSteps = 10;
    }

    public abstract String animName();

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
        reg.add(new AnimationController<>(this, "main", 8, st -> st.setAndContinue(RawAnimation.begin().thenLoop("animation.raft." + animName()))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
