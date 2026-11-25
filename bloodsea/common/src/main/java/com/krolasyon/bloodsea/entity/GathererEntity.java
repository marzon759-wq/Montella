package com.krolasyon.bloodsea.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Bone Gatherer. It is the dead of the Bone Reach, all of them, put together. It comes slowly while you look at it
 * and quickly while you do not, and every skull in its crown turns to follow you.
 */
public class GathererEntity extends SeaCreature {
    public static final int ASSEMBLE = 0, STALK = 1, SWIPE = 2;
    private static final String[] SKULLS = new String[13];

    static {
        for (int i = 0; i < SKULLS.length; i++) SKULLS[i] = "sk" + i;
    }

    public GathererEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, true);
        this.noCulling = true;
    }

    @Override public String kind() { return "gatherer"; }
    @Override public String[] actionNames() { return new String[]{"swipe", "roar"}; }
    @Override public boolean holds(String a) { return a.equals("assemble"); }
    @Override public String[] lookBones() { return getState() == ASSEMBLE ? new String[0] : SKULLS; }

    @Override
    public String loopAnim() {
        return switch (getState()) {
            case ASSEMBLE -> "assemble";
            case STALK -> "walk";
            default -> "idle";
        };
    }

    @Override
    protected void brain(ServerLevel lvl) {
        ServerPlayer p = nearest(110);
        if (p == null) return;
        double y = surfaceAt(getX(), getZ());
        lookAt(p);
        switch (getState()) {
            case ASSEMBLE -> {
                setPos(getX(), y, getZ());
                face(p, 3f);
                if (stateTicks == 2) sound("gatherer_assemble", 3f, 0.9f);
                if (stateTicks == 85) {
                    go(STALK);
                    playAction("roar");
                    sound("gatherer_roar", 4f, 0.8f);
                }
            }
            case STALK -> {
                boolean watched = watchedBy(p, 0.55);
                double d = glide(p.getX(), p.getZ(), watched ? 0.045 : 0.14, y);
                if (stateTicks % 26 == 0) sound("gatherer_step", 1.6f, rnd(0.8f, 1.0f));
                if (random.nextInt(70) == 0) sound("gatherer_rattle", 1.4f, rnd(0.8f, 1.1f));
                if (d < 4.4 && stateTicks > 20) {
                    go(SWIPE);
                    playAction("swipe");
                    sound("gatherer_swipe", 2f, 0.8f);
                }
            }
            case SWIPE -> {
                face(p, 10f);
                setPos(getX(), y, getZ());
                if (stateTicks == 13 && p.distanceTo(this) < 6) {
                    strike(p, 9f);
                    Vec3 k = p.position().subtract(position()).normalize();
                    p.push(k.x * 1.1, 0.45, k.z * 1.1);
                    p.hurtMarked = true;
                }
                if (stateTicks > 34) go(STALK);
            }
        }
    }
}
