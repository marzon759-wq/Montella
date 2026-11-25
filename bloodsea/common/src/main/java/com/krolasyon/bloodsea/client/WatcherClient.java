package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.game.SeaEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The Watcher: the Pale Sun is an eye. It looks about the sea by itself, it blinks, and when you look at it the
 * pupil closes to a thread and it stops looking about - it is looking at you.
 */
public final class WatcherClient {
    private static float px, py, tx, ty, ppx, ppy;
    private static float pupil = 1f, pupilO = 1f, stare, stareO;
    private static int nextSaccade, blinkT, blinkO;
    private static final int BLINK = 9;

    private WatcherClient() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.level.dimension() != SeaEvents.SEA || mc.isPaused()) return;
        ppx = px;
        ppy = py;
        pupilO = pupil;
        stareO = stare;
        blinkO = blinkT;
        double a = mc.level.getSunAngle(1f);
        Vec3 sun = new Vec3(-Math.sin(a), Math.cos(a), 0);
        boolean open = mc.level.canSeeSky(BlockPos.containing(mc.player.getEyePosition()));
        boolean staring = open && sun.y > -0.05 && mc.player.getViewVector(1f).dot(sun) > 0.965;
        stare = Mth.approach(stare, staring ? 1f : 0f, staring ? 0.08f : 0.03f);
        if (--nextSaccade <= 0) {
            tx = (mc.level.random.nextFloat() - 0.5f) * 0.36f;
            ty = (mc.level.random.nextFloat() - 0.5f) * 0.22f;
            nextSaccade = 15 + mc.level.random.nextInt(70);
        }
        float gx = tx * (1 - stare), gy = ty * (1 - stare);
        px += (gx - px) * 0.4f;
        py += (gy - py) * 0.4f;
        if (blinkT > 0) blinkT--;
        else if (!staring && mc.level.random.nextInt(260) == 0) blinkT = BLINK;
        float breathe = 1f + 0.08f * Mth.sin(mc.player.tickCount * 0.05f);
        pupil += ((staring ? 0.38f : breathe) - pupil) * 0.12f;
    }

    public static float blink(float pt) {
        float t = Mth.lerp(pt, blinkO, blinkT);
        return t <= 0 ? 0f : Mth.sin((float) Math.PI * (BLINK - t) / BLINK);
    }

    public static float pupil(float pt) { return Mth.lerp(pt, pupilO, pupil); }
    public static float lookX(float pt) { return Mth.lerp(pt, ppx, px); }
    public static float lookY(float pt) { return Mth.lerp(pt, ppy, py); }
    public static float stare(float pt) { return Mth.lerp(pt, stareO, stare); }
}
