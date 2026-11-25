package com.krolasyon.bloodsea.entity;

import com.krolasyon.bloodsea.world.Regions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Leviathan. Its heart is the sound of the eastern sea. When it comes up it comes up in front of you, slowly,
 * and it roars, and then it takes whatever is still in front of it. Row.
 */
public class LeviathanEntity extends SeaCreature {
    public static final int RISE = 0, LOOM = 1, ROAR = 2, BITE = 3, SINK = 4;

    public LeviathanEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level, true);
        this.noCulling = true;
    }

    @Override public String kind() { return "leviathan"; }
    @Override public String[] actionNames() { return new String[]{"roar", "bite"}; }
    @Override public boolean holds(String a) { return a.equals("rise") || a.equals("sink"); }
    @Override protected double drawRange() { return 420; }
    @Override public boolean isPickable() { return false; }

    @Override
    public String loopAnim() {
        return switch (getState()) {
            case RISE -> "rise";
            case SINK -> "sink";
            default -> "idle";
        };
    }

    /** is p inside the reach of its jaws: [ahead, aside] in blocks */
    private double[] reach(ServerPlayer p) {
        Vec3 f = Vec3.directionFromRotation(0, getYRot());
        Vec3 d = p.position().subtract(position());
        double ahead = d.x * f.x + d.z * f.z;
        double aside = Math.abs(d.x * f.z - d.z * f.x);
        return new double[]{ahead, aside};
    }

    /** it comes on slowly while it looks at you, never closer than its jaws can reach */
    private void closeIn(ServerPlayer p) {
        double d = Math.hypot(p.getX() - getX(), p.getZ() - getZ());
        if (d > 17) glide(p.getX(), p.getZ(), 0.07, Regions.SEA + 0.9);
    }

    @Override
    protected void brain(ServerLevel lvl) {
        ServerPlayer p = nearest(260);
        setPos(getX(), Regions.SEA + 0.9, getZ());
        if (p == null) return;
        switch (getState()) {
            case RISE -> {
                face(p, 1.5f);
                if (stateTicks == 2) sound("lev_rise", 8f, 0.8f);
                if (stateTicks > 100) go(LOOM);
            }
            case LOOM -> {
                face(p, 1.2f);
                closeIn(p);
                if (stateTicks > 30) go(ROAR);
            }
            case ROAR -> {
                face(p, 0.8f);
                closeIn(p);
                if (stateTicks == 1) {
                    playAction("roar");
                    sound("lev_roar", 10f, 0.85f);
                }
                if (stateTicks > 80) {
                    double[] r = reach(p);
                    if (r[0] > -2 && r[0] < 30 && r[1] < 11) go(BITE);
                    else go(SINK);
                }
            }
            case BITE -> {
                if (stateTicks == 1) {
                    playAction("bite");
                    sound("lev_bite", 10f, 0.8f);
                }
                if (stateTicks == 24) {
                    double[] r = reach(p);
                    if (r[0] > -2 && r[0] < 24 && r[1] < 10) {
                        if (p.isPassenger()) p.stopRiding();
                        strike(p, 40f);
                    }
                }
                if (stateTicks > 54) go(SINK);
            }
            case SINK -> {
                if (stateTicks > 100) discard();
            }
        }
    }
}
