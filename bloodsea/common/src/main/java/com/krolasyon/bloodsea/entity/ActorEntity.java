package com.krolasyon.bloodsea.entity;

import com.krolasyon.bloodsea.compat.CreatureBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

import java.util.Set;

/** A person in a cutscene: Elias (or the man who pulls him out of the water). Plays what it is told; nothing else. */
public class ActorEntity extends CreatureBase {
    private static final Set<String> HOLD = Set.of("wake", "recoil", "collapse", "sit_up_bed", "jump");

    public ActorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    @Override public String kind() { return "elias"; }
    @Override public String loopAnim() { return "stand"; }
    @Override public String[] actionNames() { return new String[0]; }
    @Override public boolean holds(String a) { return HOLD.contains(a); }
    @Override protected void registerGoals() {}
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean hurt(DamageSource s, float a) { return s.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(s, a); }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 200 * 200; }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > 1400 && getTags().contains(RaftEntity.GHOST)) { discard(); return; }
        this.yBodyRot = getYRot();
        this.yHeadRot = getYRot();
        this.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    }
}
