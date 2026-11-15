package com.montella.kaiju.client;

import com.montella.kaiju.KaijuMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class ClientProxy extends KaijuMod.Proxy {
    static float shake;

    @Override
    public void shake(Vec3 at, float strength) {
        var p = Minecraft.getInstance().player;
        if (p == null) return;
        double d = p.position().distanceTo(at);
        float s = strength * (float) Math.max(0, 1 - d / 140.0);
        shake = Math.min(2.5f, Math.max(shake, s) + s * 0.3f);
    }
}
