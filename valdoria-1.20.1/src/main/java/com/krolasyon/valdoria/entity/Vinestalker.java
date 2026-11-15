package com.krolasyon.valdoria.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * The vinestalker: a long-legged ambush predator that hangs motionless under the canopy among the vines.
 * <ul>
 *   <li>Climbs up a thread of vine to a branch and hangs there upside-down, almost invisible among the vine curtains.</li>
 *   <li>Anything that walks right underneath it — a player, a veilstag, even a gloomlynx — gets dropped on.</li>
 *   <li>The ambush strike hits twice as hard and entangles the prey (slowness).</li>
 *   <li>If the prey gets away it gives up after a short chase and climbs back up to wait again.</li>
 * </ul>
 */
public class Vinestalker extends Monster {
    private static final EntityDataAccessor<Boolean> HANGING = SynchedEntityData.defineId(Vinestalker.class, EntityDataSerializers.BOOLEAN);

    private enum State { GROUND, ASCENDING, HANGING, DROPPING }

    private State state = State.GROUND;
    private BlockPos anchor;
    private int chaseTicks;
    private int idleTicks;
    private boolean ambushStrike;

    public Vinestalker(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.29)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 20.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HANGING, false);
    }

    /** True while climbing up its thread or hanging from the canopy (rendered upside-down). */
    public boolean isHanging() {
        return entityData.get(HANGING);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    @Override
    public boolean onClimbable() {
        return state == State.GROUND && horizontalCollision;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, false) {
            @Override public boolean canUse() { return state == State.GROUND && super.canUse(); }
        });
        goalSelector.addGoal(4, new ReturnToCanopyGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7) {
            @Override public boolean canUse() { return state == State.GROUND && super.canUse(); }
        });
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    private void setState(State s) {
        state = s;
        entityData.set(HANGING, s == State.ASCENDING || s == State.HANGING);
        setNoGravity(s == State.ASCENDING || s == State.HANGING);
        if (s != State.GROUND) getNavigation().stop();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        switch (state) {
            case ASCENDING -> {
                if (anchor == null) { setState(State.GROUND); break; }
                double dx = (anchor.getX() + 0.5 - getX()) * 0.2, dz = (anchor.getZ() + 0.5 - getZ()) * 0.2;
                setDeltaMovement(dx, 0.22, dz);
                BlockPos above = BlockPos.containing(getX(), getBoundingBox().maxY + 0.1, getZ());
                if (canHangFrom(level().getBlockState(above)) || getY() > anchor.getY() + 1) {
                    setDeltaMovement(Vec3.ZERO);
                    setState(State.HANGING);
                    Ecology.log("vinestalker {} is hanging at {}", getId(), blockPosition());
                } else if (!level().getBlockState(above).isAir() && !level().getBlockState(above).canBeReplaced()) {
                    setState(State.GROUND);
                }
            }
            case HANGING -> {
                setDeltaMovement(Vec3.ZERO);
                if (!canHangFrom(level().getBlockState(BlockPos.containing(getX(), getBoundingBox().maxY + 0.1, getZ())))) {
                    setState(State.DROPPING);
                    break;
                }
                if (tickCount % 5 == 0) {
                    LivingEntity prey = findPreyBelow();
                    if (prey != null) {
                        setTarget(prey);
                        ambushStrike = true;
                        setState(State.DROPPING);
                        Vec3 to = prey.position().subtract(position());
                        setDeltaMovement(to.x * 0.12, -0.4, to.z * 0.12);
                        playSound(SoundEvents.SPIDER_AMBIENT, 1.0F, 0.6F);
                        Ecology.log("vinestalker {} ambushed {}", getId(), prey.getType().toShortString());
                    }
                }
            }
            case DROPPING -> {
                if (onGround() || isInWater()) {
                    setState(State.GROUND);
                    chaseTicks = 0;
                }
            }
            case GROUND -> {
                LivingEntity t = getTarget();
                if (t != null) {
                    idleTicks = 0;
                    if (++chaseTicks > 260 || distanceToSqr(t) > 18 * 18) {
                        // gives up: ambushers do not chase for long
                        setTarget(null);
                        chaseTicks = 0;
                    }
                } else {
                    idleTicks++;
                }
            }
        }
    }

    private static boolean canHangFrom(BlockState s) {
        return s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || (s.isSolid() && !s.isAir());
    }

    private LivingEntity findPreyBelow() {
        AABB below = new AABB(getX() - 2.2, getY() - 16, getZ() - 2.2, getX() + 2.2, getY(), getZ() + 2.2);
        List<LivingEntity> list = level().getEntitiesOfClass(LivingEntity.class, below,
                e -> e != this && e.isAlive() && !(e instanceof Vinestalker) && !(e instanceof LumenMoth)
                        && (Ecology.isHuntablePlayer(e) || e instanceof Veilstag || e instanceof Gloomlynx)
                        && hasLineOfSight(e));
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        for (LivingEntity e : list) {
            double d = distanceToSqr(e);
            if (d < bd) { bd = d; best = e; }
        }
        return best;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ambush = ambushStrike;
        ambushStrike = false;
        if (ambush) getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10.0);
        boolean hit = super.doHurtTarget(target);
        if (ambush) getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5.0);
        if (hit && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ambush ? 80 : 30, ambush ? 2 : 0), this);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean r = super.hurt(source, amount);
        if (r && (state == State.HANGING || state == State.ASCENDING)) {
            setState(State.DROPPING);
        }
        return r;
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        return false;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, SpawnGroupData data, CompoundTag tag) {
        SpawnGroupData r = super.finalizeSpawn(level, diff, reason, data, tag);
        BlockPos hang = isNoAi() ? null : findHangSpotAbove(blockPosition());
        if (hang != null) {
            anchor = hang;
            setState(State.ASCENDING);
        }
        return r;
    }

    /** Looks straight up for leaves/wood with enough open air underneath. */
    private BlockPos findHangSpotAbove(BlockPos feet) {
        for (int y = 3; y <= 22; y++) {
            BlockPos p = feet.above(y);
            BlockState s = level().getBlockState(p);
            if (s.isAir() || s.canBeReplaced()) continue;
            return canHangFrom(s) && y >= 4 ? p.below() : null;
        }
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("StalkState", state.name());
        if (anchor != null) tag.putLong("Anchor", anchor.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Anchor")) anchor = BlockPos.of(tag.getLong("Anchor"));
        try {
            setState(State.valueOf(tag.getString("StalkState")));
        } catch (IllegalArgumentException e) {
            setState(State.GROUND);
        }
    }

    @Override protected SoundEvent getAmbientSound() { return isHanging() ? null : SoundEvents.SPIDER_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SPIDER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SPIDER_DEATH; }
    @Override public float getVoicePitch() { return super.getVoicePitch() * 0.6F; }

    // ------------------------------------------------------------------------------------------------

    /** With nothing to hunt, walk under a branch and climb back up to wait. */
    class ReturnToCanopyGoal extends Goal {
        private BlockPos foot;

        ReturnToCanopyGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() {
            if (state != State.GROUND || getTarget() != null || idleTicks < 60 || random.nextInt(10) != 0) return false;
            for (int i = 0; i < 24; i++) {
                BlockPos f = blockPosition().offset(random.nextInt(17) - 8, 0, random.nextInt(17) - 8);
                f = level().getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, f);
                // the heightmap stops under logs too; we want open sky-ish columns under leaves
                BlockPos hang = findHangSpotAbove(f);
                if (hang != null) {
                    foot = f;
                    anchor = hang;
                    return true;
                }
            }
            return false;
        }

        @Override public void start() { getNavigation().moveTo(foot.getX() + 0.5, foot.getY(), foot.getZ() + 0.5, 0.9); }

        @Override public boolean canContinueToUse() { return state == State.GROUND && getTarget() == null && foot != null && !getNavigation().isDone(); }

        @Override
        public void stop() {
            if (state == State.GROUND && getTarget() == null && foot != null
                    && distanceToSqr(Vec3.atBottomCenterOf(foot)) < 2.5) {
                setState(State.ASCENDING);
                idleTicks = 0;
            }
            foot = null;
        }
    }
}
