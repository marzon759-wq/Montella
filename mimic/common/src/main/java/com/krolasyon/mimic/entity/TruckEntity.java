package com.krolasyon.mimic.entity;

import com.krolasyon.mimic.compat.TruckBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Dad's 1979 pickup. It does not drive itself - except in the endings, along a path. */
public class TruckEntity extends TruckBase {
    public boolean story = false;
    private boolean stale = false;
    private Vec3[] path = null;
    private int leg = 0;
    private double speed = 0;

    public TruckEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void lights(boolean on) { setLights(on); }

    /** drive through these points, accelerating */
    public void driveTo(Vec3... pts) {
        path = pts;
        leg = 0;
        speed = 0.05;
        setMoving(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (stale) { discard(); return; }
        if (path != null && leg < path.length) {
            Vec3 t = path[leg];
            Vec3 to = t.subtract(position());
            double d = Math.sqrt(to.x * to.x + to.z * to.z);
            speed = Math.min(0.55, speed + 0.012);
            if (d < speed + 0.1) {
                leg++;
                if (leg >= path.length) {
                    setMoving(false);
                    path = null;
                }
                return;
            }
            float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90f;
            float cur = getYRot();
            setYRot(cur + Mth.clamp(Mth.wrapDegrees(yaw - cur), -6f, 6f));
            Vec3 step = to.normalize().scale(speed);
            setPos(getX() + step.x, t.y, getZ() + step.z);
        }
    }

    @Override
    public boolean isPickable() { return true; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean canBeCollidedWith() { return true; }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        story = tag.getBoolean("Story");
        if (story) stale = true;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Story", story);
    }
}
