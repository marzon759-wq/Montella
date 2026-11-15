package com.krolasyon.titans.entity;

/** A tiny client-side effect sprite (dust puff or glowing spark), sized for creatures this large. */
public final class Fx {
    public double x, y, z, xo, yo, zo, vx, vy, vz;
    public float age, life, size, r, g, b;
    public int type;

    public static Fx dust(double x, double y, double z, double vx, double vy, double vz, float size, int life) {
        Fx f = new Fx();
        f.x = f.xo = x; f.y = f.yo = y; f.z = f.zo = z; f.vx = vx; f.vy = vy; f.vz = vz; f.size = size; f.life = life;
        f.r = 0.56f; f.g = 0.52f; f.b = 0.46f;
        return f;
    }

    public static Fx spark(double x, double y, double z, double vx, double vy, double vz, float size, int life, boolean violet) {
        Fx f = dust(x, y, z, vx, vy, vz, size, life);
        f.type = 1;
        if (violet) { f.r = 0.75f; f.g = 0.55f; f.b = 1f; } else { f.r = 1f; f.g = 0.6f; f.b = 0.15f; }
        return f;
    }

    public Fx tint(float r, float g, float b) { this.r = r; this.g = g; this.b = b; return this; }

    public boolean tick() {
        age++;
        xo = x; yo = y; zo = z;
        x += vx; y += vy; z += vz;
        vx *= 0.93; vz *= 0.93;
        if (type == 0) { vy *= 0.93; size *= 1.035f; } else vy -= 0.04;
        return age < life;
    }
}
