package com.montella.kaiju.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A torn-off Ghidorah head: tumbles, bounces, twitches and finally crumbles. */
public class SeveredHeadEntity extends Entity {
    public float spin, prevSpin, roll, prevRoll;
    private float spinVel = 12f;
    private boolean landed;

    public SeveredHeadEntity(EntityType<? extends SeveredHeadEntity> type, Level level) { super(type, level); noCulling = true; }

    @Override protected void defineSynchedData() {}

    @Override
    public void tick() {
        super.tick();
        prevSpin = spin; prevRoll = roll;
        Vec3 v = getDeltaMovement().add(0, -0.08, 0);
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        if (onGround()) {
            if (!landed && v.y < -0.4) {
                playSound(SoundEvents.GENERIC_EXPLODE, 3f, 0.6f);
                if (level().isClientSide) for (int i = 0; i < 20; i++) level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 0.5, getZ(), random.nextGaussian() * 0.1, 0.05, random.nextGaussian() * 0.1);
            }
            Vec3 d = getDeltaMovement();
            setDeltaMovement(d.x * 0.6, -d.y * 0.25, d.z * 0.6);
            spinVel *= 0.6f;
            if (Math.abs(d.y) < 0.15) landed = true;
        }
        spin += spinVel;
        if (!landed) roll += spinVel * 0.6f;
        else roll += (90 - roll % 360) * 0.2f;
        if (landed && tickCount % 20 == 0 && tickCount < 200 && random.nextFloat() < 0.4f) { spinVel = 3f; playSound(SoundEvents.SLIME_SQUISH, 2f, 0.4f); }
        if (level().isClientSide && tickCount < 200 && random.nextFloat() < 0.3f)
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1, getZ(), random.nextGaussian() * 0.2, 0.1, random.nextGaussian() * 0.2);
        if (!level().isClientSide && tickCount > 1800) discard();
    }

    @Override public boolean isPickable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 256 * 256; }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { tickCount = tag.getInt("Age"); }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { tag.putInt("Age", tickCount); }
}
