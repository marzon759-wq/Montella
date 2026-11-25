package com.krolasyon.bloodsea.entity;

import com.krolasyon.bloodsea.world.Regions;
import com.krolasyon.bloodsea.world.SeaTime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Wake Follower. At night it swims in the blood behind whoever is on the water, never closer, never further. If
 * they stop rowing it stops following and comes in. Only the ridge of its back ever shows - until it lunges.
 */
public class WakeEntity extends SeaCreature {
    public static final int FOLLOW = 0, APPROACH = 1, LUNGE = 2, DIVE = 3;
    private int still;
    private double lastSpeed;

    public WakeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, true);
        this.noCulling = true;
    }

    @Override public String kind() { return "wake"; }
    @Override public String[] actionNames() { return new String[]{"lunge"}; }
    @Override public boolean holds(String a) { return a.equals("dive"); }
    @Override protected double drawRange() { return 200; }

    @Override
    public String loopAnim() {
        return switch (getState()) {
            case DIVE -> "dive";
            case FOLLOW -> lastSpeed > 0.15 ? "follow" : "swim";
            default -> "follow";
        };
    }

    private Vec3 forward() { return Vec3.directionFromRotation(0, getYRot()); }

    @Override
    protected void brain(ServerLevel lvl) {
        ServerPlayer p = nearest(120);
        double y = com.krolasyon.bloodsea.world.Waves.MEAN - 0.1;
        if (p == null) return;
        if (SeaTime.isDay(lvl) && getState() != DIVE) go(DIVE);
        double x0 = getX(), z0 = getZ();
        switch (getState()) {
            case FOLLOW -> {
                Vec3 f = horizontalFacing(p);
                Vec3 side = new Vec3(-f.z, 0, f.x).scale(Math.sin(life / 70.0) * 5);
                Vec3 t = p.position().subtract(f.scale(22)).add(side);
                double d = t.distanceTo(new Vec3(getX(), t.y, getZ()));
                glide(t.x, t.z, Math.min(0.5, 0.08 + d * 0.025), y);
                if (speedOf(p) < 0.04) still++;
                else still = Math.max(0, still - 3);
                if (life % 160 == 0) sound("wake_swim", 1.6f, rnd(0.7f, 0.9f));
                if (still > 120) {
                    go(APPROACH);
                    sound("wake_surface", 2f, 0.8f);
                }
            }
            case APPROACH -> {
                // bring the head (eight blocks ahead of the middle of it) to them
                Vec3 to = p.position().subtract(getX(), p.getY(), getZ());
                Vec3 dir = to.lengthSqr() > 1e-4 ? to.normalize() : forward();
                Vec3 t = p.position().subtract(dir.scale(7.0));
                glide(t.x, t.z, 0.22, y);
                if (speedOf(p) > 0.18 && stateTicks > 40) {
                    still = 0;
                    go(FOLLOW);
                } else if (p.distanceTo(this) < 10 && t.distanceTo(new Vec3(getX(), t.y, getZ())) < 2.5) {
                    go(LUNGE);
                    playAction("lunge");
                    sound("wake_lunge", 3f, 0.9f);
                }
            }
            case LUNGE -> {
                face(p, 8f);
                setPos(getX(), y, getZ());
                if (stateTicks == 22) {
                    Vec3 head = position().add(forward().scale(7.5));
                    if (head.distanceTo(p.position()) < 6) {
                        strike(p, 7f);
                        if (p.isPassenger()) p.stopRiding();
                        p.push(forward().x * 0.8, 0.5, forward().z * 0.8);
                        p.hurtMarked = true;
                    }
                }
                if (stateTicks > 44) go(DIVE);
            }
            case DIVE -> {
                setPos(getX(), y, getZ());
                if (stateTicks > 45) discard();
            }
        }
        lastSpeed = Math.hypot(getX() - x0, getZ() - z0);
    }
}
