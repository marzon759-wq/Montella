package com.krolasyon.mimic.entity;

import com.krolasyon.mimic.compat.GuestBase;
import com.krolasyon.mimic.story.Story;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Lily Harrow, sixteen. She hid in the silo for three days. Data bits: 1 following, 2 dragged, 4 sitting, 8 running. */
public class GuestEntity extends GuestBase {
    public float homeYaw = 0;
    public boolean story = false;
    private boolean stale = false;
    private Vec3 runTarget = null;

    public GuestEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        if (this.getNavigation() instanceof GroundPathNavigation nav) {
            nav.setCanOpenDoors(true);
            nav.setCanPassDoors(true);
        }
    }

    @Override
    protected void registerGoals() {}

    public void follow(boolean on) { setData(on ? (getData() | 1) & ~4 : getData() & ~1); runTarget = null; }

    public void dragged(boolean on) { setData(on ? getData() | 2 : getData() & ~2); if (on) getNavigation().stop(); }

    public void sit(boolean on) { setData(on ? getData() | 4 : getData() & ~4); }

    public void runTo(Vec3 t) {
        runTarget = t;
        setData((getData() | 8) & ~1);
    }

    @Override
    public String loopAnim() {
        int d = getData();
        if ((d & 2) != 0) return "dragged";
        if ((d & 4) != 0) return "sit";
        if (getDeltaMovement().horizontalDistanceSqr() > 0.004 || (d & 8) != 0) return getDeltaMovement().horizontalDistanceSqr() > 0.02 || (d & 8) != 0 ? "run" : "walk";
        return "idle";
    }

    private double speedMod(double mps) {
        return Math.sqrt(0.0227 * mps) / getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (stale) { discard(); return; }
        int d = getData();
        if ((d & 2) != 0) {
            getNavigation().stop();
            return;
        }
        if (runTarget != null) {
            if (tickCount % 10 == 0) getNavigation().moveTo(runTarget.x, runTarget.y, runTarget.z, speedMod(5.0));
            if (position().distanceToSqr(runTarget) < 2) {
                runTarget = null;
                setData(getData() & ~8);
                getNavigation().stop();
            }
            return;
        }
        if ((d & 1) != 0 && level() instanceof net.minecraft.server.level.ServerLevel sl) {
            ServerPlayer p = Story.player(sl.getServer());
            if (p != null) {
                double dist = distanceTo(p);
                // stuck: up a ladder, behind something, or not getting any closer - she catches up out of sight
                boolean apart = dist > 5 && (Math.abs(getY() - p.getY()) > 2.5 || dist > 24 || dist > bestDist - 0.3);
                if (dist < bestDist - 0.3 || dist <= 5) { bestDist = dist; stuckT = 0; }
                else if (apart) stuckT++;
                if (dist > 24 || stuckT > 60) {
                    catchUp(p);
                    stuckT = 0;
                    bestDist = 1e9;
                } else if (dist > 3 && tickCount % 8 == 0) {
                    getNavigation().moveTo(p, speedMod(dist > 7 ? 5.2 : 3.2));
                } else if (dist <= 2.5) getNavigation().stop();
                getLookControl().setLookAt(p, 30, 30);
            }
        } else {
            setYRot(homeYaw);
            yBodyRot = homeYaw;
        }
    }

    private int stuckT = 0;
    private double bestDist = 1e9;

    /** reappear a couple of steps behind the player, on free ground */
    private void catchUp(ServerPlayer p) {
        Vec3 back = p.getViewVector(1f).multiply(1, 0, 1).normalize();
        for (double k : new double[]{2.0, 1.4, 3.0, 0.8}) {
            Vec3 at = p.position().subtract(back.scale(k));
            net.minecraft.world.phys.AABB box = getDimensions(getPose()).makeBoundingBox(at);
            if (level().noCollision(this, box) && !level().getBlockState(net.minecraft.core.BlockPos.containing(at.x, at.y - 0.5, at.z)).getCollisionShape(level(), net.minecraft.core.BlockPos.containing(at.x, at.y - 0.5, at.z)).isEmpty()) {
                getNavigation().stop();
                teleportTo(at.x, at.y, at.z);
                return;
            }
        }
        getNavigation().stop();
        teleportTo(p.getX(), p.getY(), p.getZ());
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

    static float unused() { return Mth.PI; }
}
