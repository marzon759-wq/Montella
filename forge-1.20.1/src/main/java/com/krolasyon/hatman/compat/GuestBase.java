package com.krolasyon.hatman.compat;

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

public abstract class GuestBase extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> DATA = SynchedEntityData.defineId(GuestBase.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected GuestBase(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder baseAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA, 0);
    }

    /** bits 0-1 variant, bit 2 sitting, bit 3 pointing */
    public int getData() { return this.entityData.get(DATA); }
    public void setData(int d) { this.entityData.set(DATA, d); }

    public void playAction(String name) { this.triggerAnim("action", name); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
        reg.add(new AnimationController<>(this, "pose", 8, st -> st.setAndContinue(RawAnimation.begin().thenLoop((getData() & 4) != 0 ? "animation.guest.sit" : "animation.guest.stand"))));
        AnimationController<GuestBase> act = new AnimationController<>(this, "action", 2, st -> PlayState.STOP);
        act.triggerableAnim("twitch", RawAnimation.begin().thenPlay("animation.guest.twitch"));
        act.triggerableAnim("point", RawAnimation.begin().thenPlayAndHold("animation.guest.point"));
        reg.add(act);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
