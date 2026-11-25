package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.entity.RaftEntity;
import com.krolasyon.bloodsea.game.SeaEvents;
import com.krolasyon.bloodsea.world.Waves;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Life on the raft, on the client: you see it through your own eyes (outside cutscenes there is no other view), you
 * cannot walk off its edge by accident, and you feel the swell under it.
 */
public final class SeaClient {
    private static double lastX, lastZ;
    private static boolean wasOn;
    /** set while a cutscene owns the camera */
    public static boolean cinematic = false;

    private SeaClient() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.level.dimension() != SeaEvents.SEA) return;
        if (!cinematic && mc.options.getCameraType() != CameraType.FIRST_PERSON && !mc.player.isSpectator())
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        Player p = mc.player;
        if (p.isPassenger() || p.isSpectator() || p.getAbilities().flying) {
            wasOn = false;
            return;
        }
        RaftEntity r = raftUnder(p, 0.0);
        boolean on = r != null;
        if (!on && wasOn) {
            RaftEntity near = raftUnder(p, 1.5);
            if (near != null && p.getY() < near.getY() + RaftEntity.TOP + 0.45) {
                // a step off the deck: no
                p.setPos(lastX, p.getY(), lastZ);
                p.setDeltaMovement(0, p.getDeltaMovement().y, 0);
                on = true;
            }
        }
        if (on) {
            lastX = p.getX();
            lastZ = p.getZ();
        }
        wasOn = on;
    }

    /** the raft whose deck p stands on (grow widens the deck) */
    public static RaftEntity raftUnder(Player p, double grow) {
        for (RaftEntity r : p.level().getEntitiesOfClass(RaftEntity.class, p.getBoundingBox().inflate(4))) {
            double top = r.getY() + RaftEntity.TOP;
            if (Math.abs(p.getY() - top) > 0.7) continue;
            if (Math.abs(p.getX() - r.getX()) < RaftEntity.HALF - 0.2 + grow && Math.abs(p.getZ() - r.getZ()) < RaftEntity.HALF - 0.2 + grow) return r;
        }
        return null;
    }

    /** the sea rocks you: a little roll and pitch from the swell under where you are */
    public static float[] sway(float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (cinematic || mc.level == null || mc.player == null || mc.level.dimension() != SeaEvents.SEA) return null;
        Player p = mc.player;
        if (!p.isPassenger() && raftUnder(p, 0) == null) return null;
        double t = Waves.time(mc.level, pt);
        double[] s = Waves.slope(p.getX(), p.getZ(), t);
        double a = Math.toRadians(p.getYRot());
        double fx = -Math.sin(a), fz = Math.cos(a);
        float pitch = (float) Math.toDegrees(Math.atan(s[0] * fx + s[1] * fz)) * 0.25f;
        float roll = (float) Math.toDegrees(Math.atan(s[0] * -fz + s[1] * fx)) * 0.25f;
        return new float[]{pitch, roll};
    }
}
