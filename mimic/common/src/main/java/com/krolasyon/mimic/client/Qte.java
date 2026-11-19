package com.krolasyon.mimic.client;

import com.krolasyon.mimic.net.Msg;
import com.krolasyon.mimic.net.Net;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * The moments where your hands decide: hold your breath (hold SPACE, not too long), don't move (touch nothing,
 * not even the mouse) and fight (hammer SPACE).
 */
public final class Qte {
    public static String type = null;
    public static int t, max;
    public static float breath, mash, motion;
    private static int released;
    private static float lastYaw, lastPitch;
    private static boolean lastJump;
    /** automated test runs: perfect hands */
    public static boolean auto = false;

    private Qte() {}

    public static void start(String ty, int n) {
        type = ty;
        t = n;
        max = Math.max(1, n);
        breath = 100;
        mash = 25;
        motion = 0;
        released = 0;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null) {
            lastYaw = p.getYRot();
            lastPitch = p.getXRot();
        }
        lastJump = Minecraft.getInstance().options.keyJump.isDown();
    }

    public static void cancel() { type = null; }

    private static void finish(boolean ok) {
        String ty = type;
        type = null;
        Net.toServer(Msg.of("qte_res").s("t", ty).b("ok", ok));
        if (!ok) ClientState.shakeT = Math.max(ClientState.shakeT, 16);
    }

    public static void tick() {
        if (type == null) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) { type = null; return; }
        boolean jump = mc.options.keyJump.isDown() || (auto && (!type.equals("mash") || (t & 1) == 0) && !type.equals("still"));
        int elapsed = max - t;
        switch (type) {
            case "breath" -> {
                if (jump) {
                    breath -= 1.05f;
                    released = 0;
                    if (breath <= 0) { finish(false); return; }
                } else {
                    breath = Math.min(100, breath + 0.2f);
                    if (elapsed > 14 && ++released > 10) { finish(false); return; }
                }
            }
            case "still" -> {
                float dy = Math.abs(net.minecraft.util.Mth.wrapDegrees(p.getYRot() - lastYaw)) + Math.abs(p.getXRot() - lastPitch);
                lastYaw = p.getYRot();
                lastPitch = p.getXRot();
                boolean keys = mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown() || jump;
                motion = Math.max(0, motion * 0.92f + dy * 1.6f + (keys ? 6 : 0));
                if (motion > 40) { finish(false); return; }
            }
            case "mash" -> {
                if (jump && !lastJump) mash += 7.5f;
                mash = Math.max(0, mash - 0.55f);
                if (mash >= 100) { finish(true); return; }
            }
            default -> {}
        }
        lastJump = jump;
        if (--t <= 0) finish(!type.equals("mash"));
    }
}
