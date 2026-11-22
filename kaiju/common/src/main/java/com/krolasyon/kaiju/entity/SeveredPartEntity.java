package com.krolasyon.kaiju.entity;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/** a torn-off arm or tail tip: it falls, bounces, twitches and bleeds for a while, then rots away */
public class SeveredPartEntity extends Entity implements GeoEntity {
    static final EntityDataAccessor<Integer> GROUP = SynchedEntityData.defineId(SeveredPartEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(SeveredPartEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<String> SPEC = SynchedEntityData.defineId(SeveredPartEntity.class, EntityDataSerializers.STRING);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public float roll, prevRoll, rollSpeed;
    int life;

    public SeveredPartEntity(EntityType<? extends SeveredPartEntity> t, Level l) {
        super(t, l);
        noCulling = true;
    }

    public void setup(KaijuEntity owner, int group, Vec3 at) {
        entityData.set(GROUP, group);
        entityData.set(SCALE, owner.scale());
        entityData.set(SPEC, owner.spec.id);
        setPos(at.x, at.y, at.z);
        setYRot(owner.getYRot());
        Vec3 out = at.subtract(owner.position()).multiply(1, 0, 1).normalize().scale(0.5);
        setDeltaMovement(out.add(owner.knock).add(0, 0.6, 0));
        rollSpeed = 12;
    }

    public int group() { return entityData.get(GROUP); }
    public float scale() { return entityData.get(SCALE); }
    public String specId() { return entityData.get(SPEC); }

    @Override
    public void tick() {
        super.tick();
        prevRoll = roll;
        roll += rollSpeed;
        Vec3 v = getDeltaMovement().add(0, -0.06, 0);
        Vec3 n = position().add(v);
        double ground = level().getHeight(Heightmap.Types.OCEAN_FLOOR, (int) Math.floor(n.x), (int) Math.floor(n.z));
        if (n.y <= ground) {
            n = new Vec3(n.x, ground, n.z);
            if (v.y < -0.4 && !level().isClientSide) level().playSound(null, n.x, n.y, n.z, SoundEvents.SLIME_BLOCK_FALL, SoundSource.HOSTILE, 8f, 0.4f);
            v = new Vec3(v.x * 0.6, -v.y * 0.3, v.z * 0.6);
            rollSpeed *= 0.6f;
            if (Math.abs(v.y) < 0.1) v = new Vec3(v.x * 0.7, 0, v.z * 0.7);
        }
        setDeltaMovement(v);
        setPos(n.x, n.y, n.z);
        if (!level().isClientSide) {
            life++;
            if (life < 300 && life % 4 == 0)
                ((ServerLevel) level()).sendParticles(new DustParticleOptions(new Vector3f(0.4f, 0.02f, 0.02f), 3f), getX(), getY() + 2, getZ(), 6, 1.5, 1, 1.5, 0.05);
            if (life % 60 == 0 && life < 400) rollSpeed += (random.nextFloat() - 0.5f) * 6; // twitch
            if (life > 20 * 120) discard();
        }
    }

    @Override protected void defineSynchedData() {
        entityData.define(GROUP, 0);
        entityData.define(SCALE, 8f);
        entityData.define(SPEC, "atomic_kaiju");
    }
    @Override protected void readAdditionalSaveData(CompoundTag t) { discard(); }
    @Override protected void addAdditionalSaveData(CompoundTag t) { }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 500 * 500; }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar r) { }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
