package com.krolasyon.kaiju.entity;

import com.krolasyon.kaiju.data.KaijuSpec;
import com.krolasyon.kaiju.net.Net;
import com.krolasyon.kaiju.world.WorldSmash;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A torn-out piece of the world: a whole tree with its root ball, a chunk of a house, a slab of ground. It is carried
 * in the kaiju's hands, swung like a club or thrown on a ballistic arc. On impact it delivers its momentum
 * (mass = number of blocks) to whatever it hits and bursts into falling blocks, so the wreckage lands as real rubble.
 */
public class DebrisEntity extends Entity implements IEntityAdditionalSpawnData {
    public final List<BlockState> states = new ArrayList<>();
    public final List<int[]> rel = new ArrayList<>();
    public KaijuEntity holder, thrower;
    public boolean flying;
    public float spinX, spinY, rotX, rotY, prevRotX, prevRotY;
    int flyTicks;
    public float radius = 2;

    public DebrisEntity(EntityType<? extends DebrisEntity> t, Level l) {
        super(t, l);
        noPhysics = true;
        noCulling = true;
    }

    public float mass() { return states.size() * 1.6f; }

    public void capture(ServerLevel sl, List<BlockPos> picked) {
        double cx = 0, cy = 0, cz = 0;
        for (BlockPos p : picked) { cx += p.getX(); cy += p.getY(); cz += p.getZ(); }
        cx /= picked.size(); cy /= picked.size(); cz /= picked.size();
        BlockPos c = BlockPos.containing(cx, cy, cz);
        float rmax = 1;
        for (BlockPos p : picked) {
            BlockState s = sl.getBlockState(p);
            if (s.isAir()) continue;
            states.add(s);
            int[] r = {p.getX() - c.getX(), p.getY() - c.getY(), p.getZ() - c.getZ()};
            rel.add(r);
            rmax = Math.max(rmax, Mth.sqrt(r[0] * r[0] + r[1] * r[1] + r[2] * r[2]));
            sl.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        radius = rmax;
        setPos(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY(), getZ(), 60, radius * 0.6, 1, radius * 0.6, 0.03);
    }

    public void followHands(KaijuEntity e) {
        if (flying) return;
        Vec3 l = e.nodePos(e.spec.node("hand_l")), r = e.nodePos(e.spec.node("hand_r"));
        Vec3 mid = l.add(r).scale(0.5).add(0, radius * 0.4, 0).add(e.forward().scale(radius * 0.3));
        setPos(mid.x, mid.y, mid.z);
        setYRot(e.getYRot());
        prevRotY = rotY;
        rotY = -e.getYRot();
    }

    public void throwAt(KaijuEntity e, Vec3 target, float speed) {
        holder = null;
        if (e.heldDebris == this) e.heldDebris = null;
        thrower = e;
        flying = true;
        flyTicks = 0;
        Vec3 from = position();
        Vec3 d = target.subtract(from);
        double horiz = Math.sqrt(d.x * d.x + d.z * d.z);
        double g = 0.05;
        double v = Math.max(speed, Math.sqrt(g * horiz) * 1.05);
        // low-angle ballistic solution
        double v2 = v * v, disc = v2 * v2 - g * (g * horiz * horiz + 2 * d.y * v2);
        double ang = disc >= 0 ? Math.atan((v2 - Math.sqrt(disc)) / (g * horiz)) : Math.PI / 4;
        Vec3 h = new Vec3(d.x, 0, d.z).normalize();
        setDeltaMovement(h.scale(v * Math.cos(ang)).add(0, v * Math.sin(ang), 0));
        spinX = (random.nextFloat() - 0.5f) * 14;
        spinY = (random.nextFloat() - 0.5f) * 14;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 10f, 0.4f);
    }

    public void drop() {
        if (holder != null && holder.heldDebris == this) holder.heldDebris = null;
        holder = null;
        flying = true;
        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void tick() {
        super.tick();
        prevRotX = rotX;
        prevRotY = rotY;
        if (level().isClientSide) {
            if (flying) { rotX += spinX; rotY += spinY; }
            return;
        }
        if (!flying) {
            if (holder == null || holder.isRemoved() || holder.heldDebris != this) drop();
            return;
        }
        flyTicks++;
        rotX += spinX;
        rotY += spinY;
        Vec3 v = getDeltaMovement().add(0, -0.05, 0);
        setDeltaMovement(v);
        Vec3 next = position().add(v);
        // kaiju bodies
        for (KaijuEntity k : level().getEntitiesOfClass(KaijuEntity.class, new AABB(position(), next).inflate(radius + 30))) {
            if (k == thrower && flyTicks < 15) continue;
            for (int i = 0; i < k.nodes.length; i++) {
                if (k.nodes[i] == null || k.nodeR(i) <= 0) continue;
                double t = KaijuCombat.segT(position(), next, k.nodes[i]);
                Vec3 q = position().add(next.subtract(position()).scale(t));
                if (q.distanceTo(k.nodes[i]) < k.nodeR(i) + radius * 0.6) {
                    hitKaiju(k, i, v, q);
                    return;
                }
            }
        }
        for (LivingEntity le : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius), x -> !(x instanceof KaijuEntity))) {
            le.hurt(damageSources().fallingBlock(this), 40);
        }
        // terrain
        if (flyTicks > 2) {
            for (int i = 0; i < 6; i++) {
                Vec3 p = next.add((i % 3 - 1) * radius * 0.5, -radius * 0.5 + (i / 3) * radius * 0.5, 0);
                BlockPos bp = BlockPos.containing(p);
                BlockState s = level().getBlockState(bp);
                if (!s.isAir() && s.getFluidState().isEmpty() || p.y < level().getMinBuildHeight()) {
                    shatter(next, (float) Mth.clamp(v.length(), 0.5, 3));
                    return;
                }
            }
        }
        setPos(next.x, next.y, next.z);
        if (flyTicks > 600) discard();
    }

    void hitKaiju(KaijuEntity k, int node, Vec3 v, Vec3 at) {
        float m = mass();
        double speed = v.length();
        double mu = m * k.mass() / (m + k.mass());
        double dv = 0.4 * (1 + 0.2) * mu * speed / k.mass() * 40;
        Vec3 dir = new Vec3(v.x, 0, v.z).normalize();
        k.knock = k.knock.add(dir.scale(dv));
        float dmg = (float) (0.035 * m * speed * speed + 0.4 * m * speed) * 0.25f;
        k.damage(dmg, node, "crush");
        k.poise -= Math.min(90, m * speed * 0.05f);
        if (k.poise <= 0 || dv > 0.25) k.knockdown(dir.scale(dv * 0.5), KaijuCombat.dirFrom(k, position()));
        else k.react(KaijuCombat.dirFrom(k, position()), (float) dv);
        k.hitstop = 5;
        shatter(at, (float) Mth.clamp(speed, 0.5, 3));
    }

    public void shatter(Vec3 at, float power) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (holder != null && holder.heldDebris == this) holder.heldDebris = null;
        Random rnd = new Random();
        int n = Math.min(states.size(), 90);
        for (int i = 0; i < n; i++) {
            int idx = rnd.nextInt(states.size());
            BlockState s = states.get(idx);
            int[] r = rel.get(idx);
            BlockPos p = BlockPos.containing(at.x + r[0] * 0.6, at.y + Math.abs(r[1]) * 0.4 + 2, at.z + r[2] * 0.6);
            if (!sl.getBlockState(p).isAir()) continue;
            FallingBlockEntity fb = FallingBlockEntity.fall(sl, p, s);
            Vec3 out = new Vec3(r[0], Math.abs(r[1]) + 1, r[2]).normalize().scale(0.4 * power + rnd.nextFloat() * 0.5);
            fb.setDeltaMovement(out.add(getDeltaMovement().scale(0.3)));
            fb.dropItem = false;
        }
        for (int i = 0; i < 10; i++) {
            BlockState s = states.get(rnd.nextInt(states.size()));
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), at.x, at.y, at.z, 30, radius * 0.6, radius * 0.6, radius * 0.6, 0.3);
        }
        WorldSmash.crater(sl, at, Math.min(10, radius * 0.6f), 2, thrower != null ? thrower : holder);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 12f, 0.5f);
        sl.playSound(null, at.x, at.y, at.z, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.BLOCKS, 12f, 0.4f);
        Net.shake(sl, at, 1.2f, 200);
        discard();
    }

    @Override protected void defineSynchedData() { }
    @Override protected void readAdditionalSaveData(CompoundTag t) { discard(); }
    @Override protected void addAdditionalSaveData(CompoundTag t) { }
    @Override public boolean shouldRenderAtSqrDistance(double d) { return d < 600 * 600; }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }

    @Override
    public void writeSpawnData(FriendlyByteBuf b) {
        b.writeVarInt(states.size());
        for (int i = 0; i < states.size(); i++) {
            b.writeVarInt(Block.getId(states.get(i)));
            int[] r = rel.get(i);
            b.writeByte(r[0]);
            b.writeByte(r[1]);
            b.writeByte(r[2]);
        }
        b.writeBoolean(flying);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf b) {
        int n = b.readVarInt();
        float rmax = 1;
        for (int i = 0; i < n; i++) {
            states.add(Block.stateById(b.readVarInt()));
            int[] r = {b.readByte(), b.readByte(), b.readByte()};
            rel.add(r);
            rmax = Math.max(rmax, Mth.sqrt(r[0] * r[0] + r[1] * r[1] + r[2] * r[2]));
        }
        radius = rmax;
        flying = b.readBoolean();
    }
}
