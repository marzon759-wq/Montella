package com.krolasyon.bloodsea.compat;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** GeckoLib and synched data plumbing for the giant dead (Forge 1.21.1). */
public abstract class GiantBase extends Entity implements GeoEntity {
    private static final EntityDataAccessor<String> VARIANT = SynchedEntityData.defineId(GiantBase.class, EntityDataSerializers.STRING);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected GiantBase(EntityType<?> type, Level level) { super(type, level); }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) { b.define(VARIANT, "leviathan_segment"); }

    public String getVariant() { return this.entityData.get(VARIANT); }

    public void setVariant(String v) { this.entityData.set(VARIANT, v); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
