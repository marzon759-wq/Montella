package com.krolasyon.kaiju.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** The kaiju itself. For now it only walks, looks around and shows off its model: no attacks yet. */
public class KaijuEntity extends PathfinderMob {
    public static final float SCALE = 2.5F;
    public static final int VARIANTS = 2; // 0 = atomic (blue dorsal glow), 1 = burning (molten fissures)
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(KaijuEntity.class, EntityDataSerializers.INT);

    public KaijuEntity(EntityType<? extends KaijuEntity> type, Level level) {
        super(type, level);
        setMaxUpStep(3.0F);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(VARIANT, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 48.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public int getVariant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(int v) {
        entityData.set(VARIANT, Math.floorMod(v, VARIANTS));
    }

    /** empty hand: switch between the atomic and the burning skin */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player.getItemInHand(hand).isEmpty()) {
            if (!level().isClientSide) setVariant(getVariant() + 1);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Variant"));
    }

    @Override
    public int getMaxHeadYRot() {
        return 40;
    }

    /** the tail and the fins reach far past the hitbox: do not cull the model while any of it can be on screen */
    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(14.0D, 4.0D, 14.0D);
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }
}
