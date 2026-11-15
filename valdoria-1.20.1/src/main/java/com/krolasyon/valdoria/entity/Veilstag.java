package com.krolasyon.valdoria.entity;

import com.krolasyon.valdoria.registry.ModBlocks;
import com.krolasyon.valdoria.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.List;

/**
 * The veilstag: a tall herd deer with vine-wrapped, glowing antlers.
 * <ul>
 *   <li>Herds stay together; stragglers walk back to the nearest adult.</li>
 *   <li>Grazes veil ferns (the ferns slowly spread back on their own).</li>
 *   <li>One alarmed stag — hurt, or spotting a predator or a sprinting player — alarms the whole herd and they bolt.</li>
 *   <li>Adults turn on any predator that goes after a calf.</li>
 *   <li>Calm stags can be led and bred with shimmercaps.</li>
 * </ul>
 */
public class Veilstag extends Animal {
    private static final byte EVENT_GRAZE = 10;

    private int alarmTicks;
    private Vec3 alarmFrom = Vec3.ZERO;
    /** Client-side: ticks left of the head-down grazing animation. */
    public int grazeAnim;

    public Veilstag(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.2)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.5, true) {
            @Override public boolean canUse() { return !isBaby() && super.canUse(); }
        });
        goalSelector.addGoal(2, new HerdFleeGoal());
        goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        goalSelector.addGoal(4, new TemptGoal(this, 1.1, Ingredient.of(ModBlocks.SHIMMERCAP.get()), false));
        goalSelector.addGoal(5, new FollowParentGoal(this, 1.15));
        goalSelector.addGoal(6, new GrazeGoal());
        goalSelector.addGoal(7, new FollowHerdGoal());
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new DefendCalvesGoal());
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return Ecology.isShimmercap(stack);
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
        return ModEntities.VEILSTAG.get().create(level);
    }

    public boolean isAlarmed() {
        return alarmTicks > 0;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (alarmTicks > 0) alarmTicks--;
        if (tickCount % 10 == 0 && alarmTicks <= 0 && getTarget() == null) {
            // keep watch: predators, and players that come running at the herd
            List<LivingEntity> threats = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(12, 4, 12),
                    e -> e != this && e.isAlive() && (Ecology.isPredator(e)
                            || (Ecology.isHuntablePlayer(e) && e.isSprinting() && distanceToSqr(e) < 49 && !Ecology.isShimmercap(((Player) e).getMainHandItem()))));
            for (LivingEntity t : threats) {
                if (hasLineOfSight(t)) {
                    raiseAlarm(t.position());
                    break;
                }
            }
        }
    }

    /** Alarms this stag and every stag within earshot. */
    public void raiseAlarm(Vec3 from) {
        Ecology.log("veilstag {} raised the alarm", getId());
        for (Veilstag s : level().getEntitiesOfClass(Veilstag.class, getBoundingBox().inflate(24, 8, 24))) {
            s.alarmTicks = 100 + s.random.nextInt(60);
            s.alarmFrom = from;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide) {
            Vec3 from = source.getEntity() != null ? source.getEntity().position() : position();
            raiseAlarm(from);
            // a grown stag that is attacked by a predator may stand its ground
            if (!isBaby() && source.getEntity() instanceof LivingEntity attacker && Ecology.isPredator(attacker) && getHealth() > getMaxHealth() * 0.5F && random.nextInt(3) == 0) {
                setTarget(attacker);
            }
        }
        return hurt;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_GRAZE) grazeAnim = 40;
        else super.handleEntityEvent(id);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && grazeAnim > 0) grazeAnim--;
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.GOAT_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.GOAT_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.GOAT_DEATH; }
    @Override public float getVoicePitch() { return super.getVoicePitch() * 0.75F; }

    // ------------------------------------------------------------------------------------------------

    /** The whole herd runs from wherever the alarm came from. */
    class HerdFleeGoal extends Goal {
        HerdFleeGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override public boolean canUse() { return alarmTicks > 0 && getTarget() == null && pickPath(); }
        @Override public boolean canContinueToUse() { return alarmTicks > 0 && getTarget() == null && !getNavigation().isDone(); }
        @Override public void tick() { if (getNavigation().isDone()) pickPath(); }

        private boolean pickPath() {
            Vec3 away = DefaultRandomPos.getPosAway(Veilstag.this, 16, 6, alarmFrom);
            return away != null && getNavigation().moveTo(away.x, away.y, away.z, isBaby() ? 1.9 : 1.75);
        }
    }

    /** Walk to a nearby fern, lower the head, eat it. */
    class GrazeGoal extends Goal {
        private BlockPos food;
        private int eatTicks;

        GrazeGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }

        @Override
        public boolean canUse() {
            if (alarmTicks > 0 || getTarget() != null || random.nextInt(isBaby() ? 120 : 260) != 0) return false;
            food = findFood();
            return food != null;
        }

        @Override public boolean canContinueToUse() { return food != null && alarmTicks <= 0 && isForage(level().getBlockState(food)) && eatTicks < 40; }

        @Override public void start() { eatTicks = 0; getNavigation().moveTo(food.getX() + 0.5, food.getY(), food.getZ() + 0.5, 0.9); }

        @Override public void stop() { food = null; eatTicks = 0; }

        @Override
        public void tick() {
            getLookControl().setLookAt(food.getX() + 0.5, food.getY() + 0.2, food.getZ() + 0.5);
            if (distanceToSqr(Vec3.atBottomCenterOf(food)) < 3.2) {
                getNavigation().stop();
                if (eatTicks == 0) level().broadcastEntityEvent(Veilstag.this, EVENT_GRAZE);
                if (++eatTicks == 30) {
                    level().destroyBlock(food, false);
                    Ecology.log("veilstag {} grazed", getId());
                    heal(2.0F);
                    if (isBaby()) ageUp(60);
                    eatTicks = 40;
                }
            } else if (getNavigation().isDone()) {
                getNavigation().moveTo(food.getX() + 0.5, food.getY(), food.getZ() + 0.5, 0.9);
            }
        }

        private BlockPos findFood() {
            BlockPos base = blockPosition();
            for (int i = 0; i < 40; i++) {
                BlockPos p = base.offset(random.nextInt(17) - 8, random.nextInt(5) - 2, random.nextInt(17) - 8);
                if (isForage(level().getBlockState(p))) return p;
            }
            return null;
        }

        private boolean isForage(BlockState s) {
            return s.is(ModBlocks.VEIL_FERN.get()) || s.is(ModBlocks.TALL_VEIL_FERN.get())
                    || s.is(net.minecraft.world.level.block.Blocks.GRASS) || s.is(net.minecraft.world.level.block.Blocks.FERN);
        }
    }

    /** Stragglers return to the nearest adult of the herd. */
    class FollowHerdGoal extends Goal {
        private Veilstag leader;

        FollowHerdGoal() { setFlags(EnumSet.of(Flag.MOVE)); }

        @Override
        public boolean canUse() {
            if (tickCount % 20 != 0 || alarmTicks > 0) return false;
            leader = null;
            double best = Double.MAX_VALUE;
            for (Veilstag s : level().getEntitiesOfClass(Veilstag.class, getBoundingBox().inflate(28, 8, 28))) {
                if (s == Veilstag.this || s.isBaby()) continue;
                double d = distanceToSqr(s);
                if (d < best) { best = d; leader = s; }
            }
            return leader != null && best > 81;
        }

        @Override public boolean canContinueToUse() { return leader != null && leader.isAlive() && distanceToSqr(leader) > 25 && alarmTicks <= 0; }
        @Override public void tick() { if (tickCount % 10 == 0) getNavigation().moveTo(leader, 1.0); }
        @Override public void stop() { leader = null; getNavigation().stop(); }
    }

    /** Adults charge any predator that targets a calf of the herd. */
    class DefendCalvesGoal extends TargetGoal {
        private LivingEntity threat;

        DefendCalvesGoal() { super(Veilstag.this, false); }

        @Override
        public boolean canUse() {
            if (isBaby() || getHealth() < getMaxHealth() * 0.4F || tickCount % 10 != 0) return false;
            threat = null;
            for (Mob m : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(16, 6, 16), Ecology::isPredator)) {
                if (m.getTarget() instanceof Veilstag calf && calf.isBaby()) { threat = m; break; }
            }
            return threat != null && canAttack(threat, TargetingConditions.DEFAULT);
        }

        @Override
        public void start() {
            mob.setTarget(threat);
            super.start();
        }
    }

    public static boolean isGrazing(Veilstag s) {
        return s.grazeAnim > 0;
    }
}
