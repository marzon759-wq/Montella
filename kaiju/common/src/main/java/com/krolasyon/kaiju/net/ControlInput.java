package com.krolasyon.kaiju.net;

import net.minecraft.network.FriendlyByteBuf;

/** what a rider is pressing this tick (sent every client tick while riding a kaiju) */
public final class ControlInput {
    public static final int ATTACK = 1, HEAVY = 2, BREATH = 4, GRAB = 8, TAIL = 16, BITE = 32, PULSE = 64, ROAR = 128,
            GUARD = 256, DODGE = 512, SPECIAL = 1024, JUMP = 2048, SPRINT = 4096, AI = 8192;

    public float forward, strafe, yaw, pitch;
    public int buttons, latched, latchedAt;
    public boolean aiToggle;

    public boolean down(int b) { return (buttons & b) != 0; }

    /** pressed since the last time the brain consumed input (edges are latched so a 1-tick tap is never lost) */
    public boolean pressed(int b) { return (latched & b) != 0; }

    public void consume() { latched = 0; }

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(forward);
        buf.writeFloat(strafe);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeVarInt(buttons);
    }

    public static ControlInput read(FriendlyByteBuf buf) {
        ControlInput c = new ControlInput();
        c.forward = buf.readFloat();
        c.strafe = buf.readFloat();
        c.yaw = buf.readFloat();
        c.pitch = buf.readFloat();
        c.buttons = buf.readVarInt();
        return c;
    }

    /** merge a fresh packet into the server-side state, latching new presses */
    public void update(ControlInput n, int now) {
        int fresh = n.buttons & ~buttons;
        if (fresh != 0) latchedAt = now;
        latched |= fresh;
        forward = n.forward;
        strafe = n.strafe;
        yaw = n.yaw;
        pitch = n.pitch;
        buttons = n.buttons;
    }
}
