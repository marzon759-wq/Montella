package com.krolasyon.titans.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.joml.Vector3f;

/** Hit box that follows one piece of the skeleton, so heads, necks and the tail can really be struck. */
public class TitanPart extends PartEntity<TitanEntity> {
    public static final int HEAD = 0, NECK = 1, TORSO = 2, TAIL = 3;
    public final int kind, index;
    private final EntityDimensions size;
    private final Vector3f tmp = new Vector3f();

    public TitanPart(TitanEntity parent, int kind, int index, float s) {
        super(parent);
        this.kind = kind;
        this.index = index;
        size = EntityDimensions.scalable(s, s);
        refreshDimensions();
    }

    public void place() {
        TitanEntity p = getParent();
        Rig r = p.rig;
        Vector3f m = switch (kind) {
            case HEAD -> tmp.set(r.headPos[index]).fma(3 * r.headScale[index], r.headFwd[index]);
            case NECK -> r.neck[index][r.nn / 2];
            case TORSO -> r.torso[index];
            default -> r.tail[index];
        };
        Vec3 w = p.toWorld(m);
        xo = xOld = getX(); yo = yOld = getY(); zo = zOld = getZ();
        setPos(w.x, w.y - size.height / 2, w.z);
    }

    @Override protected void defineSynchedData() {}
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
    @Override public boolean isPickable() { return true; }
    @Override public boolean hurt(DamageSource src, float amt) { return !isInvulnerableTo(src) && getParent().hurtPart(this, src, amt); }
    @Override public boolean is(Entity e) { return this == e || getParent() == e; }
    @Override public EntityDimensions getDimensions(Pose pose) { return size; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { throw new UnsupportedOperationException(); }
}
