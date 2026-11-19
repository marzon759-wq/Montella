package com.krolasyon.mimic.compat;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Dad's pickup: a GeckoLib prop entity (Forge 1.21.1). */
public abstract class TruckBase extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Boolean> LIGHTS = SynchedEntityData.defineId(TruckBase.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MOVING = SynchedEntityData.defineId(TruckBase.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected TruckBase(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(LIGHTS, false);
        b.define(MOVING, false);
    }

    public boolean lightsOn() { return this.entityData.get(LIGHTS); }
    public void setLights(boolean b) { this.entityData.set(LIGHTS, b); }
    public boolean moving() { return this.entityData.get(MOVING); }
    public void setMoving(boolean b) { this.entityData.set(MOVING, b); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
        reg.add(new AnimationController<>(this, "drive", 4, st -> st.setAndContinue(RawAnimation.begin().thenLoop(moving() ? "animation.truck.drive" : "animation.truck.idle"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
