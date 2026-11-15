package com.krolasyon.valdoria.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * The gloomlynx: a pack hunter with glowing eyes.
 * <ul>
 *   <li>Sleeps through the day curled up in the shade; wakes if hurt or if someone blunders right up to it.</li>
 *   <li>Hunts only when hungry: veilstags (calves first) and, at night, players.</li>
 *   <li>When one lynx picks a target the rest of the pack joins in.</li>
 *   <li>After a kill the pack is fed and leaves everything alone for a while.</li>
 *   <li>Afraid of light: it backs off from anyone carrying a torch, lantern or other glowing thing.</li>
 * </ul>
 */
public class Gloomlynx extends Animal {
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(Gloomlynx.class, EntityDataSerializers.BOOLEAN);

    /** Ticks until hungry again. */
    private int satiety;

    public Gloomlynx(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.satiety = 1200 + random.nextInt(4800);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.31)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SLEEPING, false);
    }

    public boolean isNapping() {
        return entityData.get(SLEEPING);
    }

    public void setSleepingFlag(boolean sleeping) {
        entityData.set(SLEEPING, sleeping);
    }

    public boolean isHungry() {
        return satiety <= 0;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SleepGoal());
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 9.0F, 1.0, 1.35, e -> Ecology.isHuntablePlayer(e) && Ecology.holdsLight(e)));
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.38F));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.35, true));
        goalSelector.addGoal(5, new StayWithPackGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(Gloomlynx.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Veilstag.class, 10, true, false,
                e -> isHungry() && !isNapping() && (e.isBaby() || random.nextInt(3) == 0)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> isHungry() && !isNapping() && !level().isDay() && Ecology.isHuntablePlayer(e) && !Ecology.holdsLight(e)));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (satiety > 0) satiety--;
        LivingEntity target = getTarget();
        if (target != null) {
            if (isNapping()) setSleepingFlag(false);
            // a lynx that sees a torch in its prey's hand loses its nerve
            if (Ecology.holdsLight(target) && getLastHurtByMob() != target) setTarget(null);
            else if (tickCount % 20 == 0) {
                for (Gloomlynx mate : level().getEntitiesOfClass(Gloomlynx.class, getBoundingBox().inflate(16, 6, 16))) {
                    if (mate != this && mate.getTarget() == null) {
                        mate.setSleepingFlag(false);
                        mate.setTarget(target);
                    }
                }
            }
        }
    }

    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        boolean r = super.killedEntity(level, victim);
        // the pack shares the kill
        for (Gloomlynx mate : level.getEntitiesOfClass(Gloomlynx.class, getBoundingBox().inflate(16, 6, 16))) {
            mate.satiety = 6000 + mate.random.nextInt(6000);
            mate.heal(8.0F);
            if (mate.getTarget() == victim || mate.getTarget() == null) mate.setTarget(null);
        }
        return r;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isNapping()) setSleepingFlag(false);
        return super.hurt(source, amount);
    }

    @Override public boolean causeFallDamage(float dist, float mult, DamageSource src) { return dist > 6 && super.causeFallDamage(dist - 5, mult, src); }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override public boolean removeWhenFarAway(double dist) { return false; }

    @Override protected SoundEvent getAmbientSound() { return isNapping() ? SoundEvents.FOX_SLEEP : getTarget() != null ? SoundEvents.CAT_HISS : SoundEvents.FOX_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.CAT_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.CAT_DEATH; }
    @Override public float getVoicePitch() { return super.getVoicePitch() * 0.7F; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Satiety", satiety);
        tag.putBoolean("Sleeping", isNapping());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        satiety = tag.getInt("Satiety");
        setSleepingFlag(tag.getBoolean("Sleeping"));
    }

    // ------------------------------------------------------------------------------------------------

    /** Curls up during the day when nothing is going on. */
    class SleepGoal extends Goal {
        SleepGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }

        private boolean calm() {
            if (getTarget() != null || getLastHurtByMob() != null && tickCount - getLastHurtByMobTimestamp() < 600) return false;
            Player near = level().getNearestPlayer(Gloomlynx.this, 4.0);
            return near == null || near.isCrouching() || !Ecology.isHuntablePlayer(near);
        }

        @Override public boolean canUse() { return level().isDay() && !isInWater() && onGround() && calm() && random.nextInt(80) == 0; }
        @Override public boolean canContinueToUse() { return level().isDay() && calm(); }
        @Override public void start() { getNavigation().stop(); setSleepingFlag(true); }
        @Override public void stop() { setSleepingFlag(false); }
        @Override public void tick() { getNavigation().stop(); }
    }

    /** Keeps the pack within sight of each other. */
    class StayWithPackGoal extends Goal {
        private Gloomlynx mate;

        StayWithPackGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() {
            if (tickCount % 20 != 0 || getTarget() != null) return false;
            mate = null;
            double best = Double.MAX_VALUE;
            for (Gloomlynx g : level().getEntitiesOfClass(Gloomlynx.class, getBoundingBox().inflate(32, 8, 32))) {
                if (g == Gloomlynx.this) continue;
                double d = distanceToSqr(g);
                if (d < best) { best = d; mate = g; }
            }
            return mate != null && best > 100;
        }

        @Override public boolean canContinueToUse() { return mate != null && mate.isAlive() && distanceToSqr(mate) > 25 && getTarget() == null; }
        @Override public void tick() { if (tickCount % 10 == 0) getNavigation().moveTo(mate, 1.0); }
        @Override public void stop() { mate = null; }
    }
}
