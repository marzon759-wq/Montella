package com.krolasyon.valdoria.entity;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import com.krolasyon.valdoria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The lumen moth: a small glowing moth with long trailing hind wings that lives in loose swarms.
 * <ul>
 *   <li>Flocks: moths keep together, match each other's heading and avoid bumping into each other.</li>
 *   <li>Drawn to light: lichen, glimmering leaves, shimmercaps, lanterns — and players holding something that glows.</li>
 *   <li>Scatters from predators.</li>
 *   <li>Pollinates: moths hovering around shimmercaps spread new ones onto the forest floor.</li>
 * </ul>
 */
public class LumenMoth extends AmbientCreature implements GeoEntity {
    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("animation.lumen_moth.fly");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar reg) {
        reg.add(new AnimationController<>(this, "main", 0, st -> st.setAndContinue(FLY)));
    }

    private Vec3 attractor;
    private int attractorTicks;
    private List<LumenMoth> flock = List.of();

    public LumenMoth(EntityType<? extends AmbientCreature> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 3.0).add(Attributes.MOVEMENT_SPEED, 0.2);
    }

    public static boolean checkMothSpawnRules(EntityType<LumenMoth> type, LevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource rand) {
        return level.getFluidState(pos).isEmpty() && level.getFluidState(pos.below()).isEmpty();
    }

    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(net.minecraft.world.entity.Entity e) {}
    @Override protected void pushEntities() {}
    @Override public boolean causeFallDamage(float d, float m, DamageSource s) { return false; }
    @Override protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}
    @Override public boolean isIgnoringBlockTriggers() { return true; }
    @Override protected float getSoundVolume() { return 0.2F; }
    @Override protected SoundEvent getAmbientSound() { return null; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.BAT_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.BAT_DEATH; }
    @Override public float getVoicePitch() { return 1.8F; }

    @Override
    public void tick() {
        super.tick();
        // gentle constant flutter
        setDeltaMovement(getDeltaMovement().multiply(1.0, 0.85, 1.0));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 5 == 0) {
            flock = level().getEntitiesOfClass(LumenMoth.class, getBoundingBox().inflate(6), m -> m != this);
        }
        if (--attractorTicks <= 0 || attractor == null || position().distanceToSqr(attractor) < 1.5) pickAttractor();

        Vec3 pos = position();
        Vec3 vel = getDeltaMovement();
        Vec3 steer = Vec3.ZERO;

        // --- flocking ---
        if (!flock.isEmpty()) {
            Vec3 center = Vec3.ZERO, heading = Vec3.ZERO, apart = Vec3.ZERO;
            for (LumenMoth m : flock) {
                center = center.add(m.position());
                heading = heading.add(m.getDeltaMovement());
                double d = m.distanceToSqr(this);
                if (d < 1.2) apart = apart.add(pos.subtract(m.position()).normalize().scale(1.2 - d));
            }
            center = center.scale(1.0 / flock.size());
            heading = heading.scale(1.0 / flock.size());
            steer = steer.add(center.subtract(pos).scale(0.004)).add(heading.subtract(vel).scale(0.04)).add(apart.scale(0.05));
        }

        // --- light ---
        if (attractor != null) steer = steer.add(attractor.subtract(pos).normalize().scale(0.012));

        // --- predators ---
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5), Ecology::isPredator)) {
            steer = steer.add(pos.subtract(e.position()).normalize().scale(0.06));
        }

        // --- stay between the ground and the canopy ---
        int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(getX()), Mth.floor(getZ()));
        double above = getY() - ground;
        if (above < 1.2) steer = steer.add(0, 0.03, 0);
        else if (above > 14) steer = steer.add(0, -0.02, 0);

        // a little random flutter
        steer = steer.add((random.nextDouble() - 0.5) * 0.02, (random.nextDouble() - 0.5) * 0.02, (random.nextDouble() - 0.5) * 0.02);

        Vec3 nv = vel.add(steer);
        double speed = nv.length();
        if (speed > 0.13) nv = nv.scale(0.13 / speed);
        setDeltaMovement(nv);
        float yaw = (float) (Mth.atan2(nv.z, nv.x) * (180F / Math.PI)) - 90.0F;
        setYRot(Mth.wrapDegrees(getYRot() + Mth.wrapDegrees(yaw - getYRot()) * 0.3F));
        yBodyRot = getYRot();
        yHeadRot = getYRot();

        if (random.nextInt(400) == 0) pollinate();
    }

    /** Pick something glowing to hover around: a glow-carrying player, a glowing block, or just a random spot. */
    private void pickAttractor() {
        attractorTicks = 80 + random.nextInt(120);
        Player p = level().getNearestPlayer(this, 10);
        if (p != null && Ecology.holdsLight(p) && !p.isSpectator()) {
            attractor = p.getEyePosition().add(0, 0.6, 0);
            attractorTicks = 20;
            return;
        }
        BlockPos base = blockPosition();
        for (int i = 0; i < 30; i++) {
            BlockPos q = base.offset(random.nextInt(25) - 12, random.nextInt(13) - 6, random.nextInt(25) - 12);
            if (level().getBlockState(q).getLightEmission(level(), q) >= 7) {
                attractor = Vec3.atCenterOf(q).add((random.nextDouble() - 0.5) * 2.5, random.nextDouble() * 1.5, (random.nextDouble() - 0.5) * 2.5);
                return;
            }
        }
        attractor = position().add((random.nextDouble() - 0.5) * 16, (random.nextDouble() - 0.5) * 6, (random.nextDouble() - 0.5) * 16);
    }

    /** Moths dusted with shimmercap spores seed new shimmercaps on the forest floor. */
    private void pollinate() {
        BlockPos base = blockPosition();
        BlockPos cap = null;
        for (BlockPos q : BlockPos.betweenClosed(base.offset(-4, -6, -4), base.offset(4, 1, 4))) {
            if (level().getBlockState(q).is(ModBlocks.SHIMMERCAP.get())) { cap = q.immutable(); break; }
        }
        if (cap == null) return;
        int count = 0;
        for (BlockPos q : BlockPos.betweenClosed(cap.offset(-4, -2, -4), cap.offset(4, 2, 4))) {
            if (level().getBlockState(q).is(ModBlocks.SHIMMERCAP.get())) count++;
        }
        if (count >= 6) return;
        for (int i = 0; i < 8; i++) {
            BlockPos q = cap.offset(random.nextInt(7) - 3, random.nextInt(3) - 1, random.nextInt(7) - 3);
            BlockState s = ModBlocks.SHIMMERCAP.get().defaultBlockState();
            if (level().isEmptyBlock(q) && s.canSurvive(level(), q)) {
                level().setBlockAndUpdate(q, s);
                Ecology.log("lumen moth {} pollinated a shimmercap at {}", getId(), q);
                return;
            }
        }
    }
}
