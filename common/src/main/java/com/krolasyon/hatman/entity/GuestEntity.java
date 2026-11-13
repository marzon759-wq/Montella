package com.krolasyon.hatman.entity;

import com.krolasyon.hatman.compat.GuestBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A masked guest of the 1962 masquerade. They do not move. Their heads do - when you are not looking. */
public class GuestEntity extends GuestBase {
    public float homeYaw = 0;
    public boolean story = false;
    private boolean stale = false;
    private float headTarget = 0;

    public GuestEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setSilent(true);
    }

    @Override
    protected void registerGoals() {}

    public void setup(float yaw, int variant, boolean sitting) {
        homeYaw = yaw;
        setData((variant & 3) | (sitting ? 4 : 0));
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    public void setSitting(boolean s) { setData(s ? getData() | 4 : getData() & ~4); }

    public void setWatching(boolean w) { setData(w ? getData() | 8 : getData() & ~8); }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (stale) { discard(); return; }
        setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        setYRot(homeYaw);
        yBodyRot = homeYaw;
        Player p = level().getNearestPlayer(this, 14);
        if (p != null && (getData() & 8) != 0) {
            Vec3 to = p.position().subtract(position());
            float want = Mth.wrapDegrees((float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90f - homeYaw);
            want = Mth.clamp(want, -85, 85);
            // only turn while unobserved
            Vec3 look = p.getViewVector(1f);
            Vec3 dir = position().add(0, 1.4, 0).subtract(p.getEyePosition()).normalize();
            boolean seen = look.dot(dir) > 0.75;
            if (!seen) headTarget = want;
        } else if (p == null) {
            headTarget = 0;
        }
        float cur = Mth.wrapDegrees(yHeadRot - homeYaw);
        float next = cur + Mth.clamp(headTarget - cur, -6f, 6f);
        if (Math.abs(headTarget - cur) > 30 && tickCount % 7 == 0 && random.nextInt(4) == 0) playAction("twitch");
        yHeadRot = homeYaw + next;
        setXRot((getData() & 8) != 0 ? -8 : 10);
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        return src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(src, amount);
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity e) {}

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    protected SoundEvent getAmbientSound() { return null; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Story", story);
        tag.putFloat("HomeYaw", homeYaw);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        story = tag.getBoolean("Story");
        homeYaw = tag.getFloat("HomeYaw");
        if (story) stale = true;
    }
}
