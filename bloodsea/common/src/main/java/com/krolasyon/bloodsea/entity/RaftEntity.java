package com.krolasyon.bloodsea.entity;

import com.krolasyon.bloodsea.compat.RaftBase;
import com.krolasyon.bloodsea.world.Regions;
import com.krolasyon.bloodsea.world.Waves;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.function.Supplier;

/**
 * The raft: five blocks of logs you can stand on. Sit at the steering oar (use it) and row: forward and back, left and
 * right to turn. It only moves while someone rows it; at rest it turns square to the world so its deck and the space
 * you can stand on agree. It rides the swell - the drawing of it, and whoever sits on it.
 */
public class RaftEntity extends RaftBase {
    public static final double Y = Waves.MEAN - 0.05;
    public static final float HALF = 2.5f, TOP = 0.55f;
    /** the client's steering keys: {forward, strafe} - set by the client setup */
    public static Supplier<float[]> INPUT = () -> new float[]{0, 0};
    public static final int ROWING = 1;
    private float turn;

    public RaftEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.blocksBuilding = true;
    }

    @Override public String animName() { return (getFlags() & ROWING) != 0 ? "row" : "idle"; }
    @Override public boolean canBeCollidedWith() { return true; }
    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 256 * 256; }
    @Override protected boolean canAddPassenger(Entity e) { return getPassengers().size() < 4; }

    @Override
    public InteractionResult interact(Player p, InteractionHand hand) {
        if (p.isSecondaryUseActive() || p.isPassenger()) return InteractionResult.PASS;
        if (!level().isClientSide && getControllingPassenger() == null) p.startRiding(this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public LivingEntity getControllingPassenger() {
        for (Entity e : getPassengers()) if (e instanceof Player p) return p;
        return null;
    }

    /** the swell under the middle of the raft, on the client only (the server keeps it flat) */
    public double bob(float pt) { return level().isClientSide ? Waves.height(getX(), getZ(), Waves.time(level(), pt)) * 0.8 : 0; }

    @Override
    protected void positionRider(Entity e, Entity.MoveFunction f) {
        // the rower at the stern by the oar, anyone else (the cat) on the blanket at the back left
        boolean rower = e == getControllingPassenger();
        double lx = rower ? 0 : -1.4, lz = rower ? 1.55 : 1.45;
        double yaw = Math.toRadians(getYRot());
        double c = Math.cos(yaw), s = Math.sin(yaw);
        double wx = getX() + lx * c - lz * s, wz = getZ() + lx * s + lz * c;
        double y = getY() + TOP + bob(1f) + (rower ? -0.05 : 0.0);
        f.accept(e, wx, y + (e instanceof Player ? -0.45 : 0), wz);
        if (rower) {
            e.setYBodyRot(getYRot());
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity e) {
        double yaw = Math.toRadians(getYRot());
        return new Vec3(getX() - Math.sin(yaw) * 0.0 + Math.cos(yaw) * 0.0, getY() + TOP + 0.01, getZ() + Math.cos(yaw) * 1.4);
    }

    @Override
    public void tick() {
        super.tick();
        if (lerpSteps > 0 && !isControlledByLocalInstance()) {
            double x = getX() + (lerpX - getX()) / lerpSteps, y = getY() + (lerpY - getY()) / lerpSteps, z = getZ() + (lerpZ - getZ()) / lerpSteps;
            setYRot(getYRot() + (float) Mth.wrapDegrees(lerpYRot - getYRot()) / lerpSteps);
            lerpSteps--;
            setPos(x, y, z);
        }
        LivingEntity driver = getControllingPassenger();
        if (isControlledByLocalInstance() && driver != null) {
            float[] in = level().isClientSide ? INPUT.get() : new float[]{0, 0};
            turn = Mth.lerp(0.2f, turn, -in[1] * 2.2f);
            setYRot(getYRot() + turn);
            Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
            Vec3 v = getDeltaMovement().scale(0.965);
            v = v.add(fwd.scale(in[0] > 0 ? 0.012 : in[0] < 0 ? -0.006 : 0));
            double sp = Math.sqrt(v.x * v.x + v.z * v.z);
            if (sp > 0.34) v = v.scale(0.34 / sp);
            setDeltaMovement(v.x, 0, v.z);
            move(MoverType.SELF, getDeltaMovement());
            setPos(getX(), Y, getZ());
            if (level().isClientSide) setFlags(Math.abs(in[0]) > 0.01 || Math.abs(in[1]) > 0.01 ? ROWING : 0);
        } else if (!level().isClientSide) {
            // nobody rowing: it slows, and squares itself to the world
            Vec3 v = getDeltaMovement().scale(0.9);
            setDeltaMovement(v.x, 0, v.z);
            if (v.lengthSqr() > 1e-5) move(MoverType.SELF, v);
            float sq = Math.round(getYRot() / 90f) * 90f;
            setYRot(Mth.approachDegrees(getYRot(), sq, 1.5f));
            if (Math.abs(getY() - Y) > 1e-3) setPos(getX(), Y, getZ());
            setFlags(0);
        }
        // rowing stirs the blood
        if (level().isClientSide && getDeltaMovement().horizontalDistanceSqr() > 0.01 && random.nextInt(2) == 0) {
            Vec3 back = Vec3.directionFromRotation(0, getYRot()).scale(-2.8);
            level().addParticle(new DustParticleOptions(new Vector3f(0.45f, 0.02f, 0.03f), 1.6f),
                    getX() + back.x + (random.nextFloat() - 0.5) * 3, Waves.MEAN + 0.1, getZ() + back.z + (random.nextFloat() - 0.5) * 3, 0, 0.05, 0);
        }
    }

    /** the deck you can stand on, in the world */
    public AABB deck() { return new AABB(getX() - HALF, getY(), getZ() - HALF, getX() + HALF, getY() + TOP, getZ() + HALF); }

    @Override protected void readAdditionalSaveData(CompoundTag t) {}
    @Override protected void addAdditionalSaveData(CompoundTag t) {}
}
