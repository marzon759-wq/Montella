package com.krolasyon.bloodsea.entity;

import com.krolasyon.bloodsea.compat.GiantBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Bones too big for blocks: a model placed once by the world generator and never moving. It does nothing but be there,
 * so it does not tick, cannot be hit and is drawn from far away.
 */
public class GiantEntity extends GiantBase {
    public GiantEntity(EntityType<? extends GiantEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
        this.setNoGravity(true);
    }

    @Override public void tick() {}

    @Override public boolean isPickable() { return false; }

    @Override public boolean isPushable() { return false; }

    @Override public boolean hurt(DamageSource s, float amount) { return false; }

    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 320 * 320; }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        if (t.contains("Variant")) setVariant(t.getString("Variant"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) { t.putString("Variant", getVariant()); }
}
