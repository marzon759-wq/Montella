package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.game.SeaEvents;
import com.krolasyon.bloodsea.world.Waves;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/**
 * The surface of the Blood Sea as it is drawn: a swell mesh over the blood blocks, fine near the eye and coarse far
 * off (one grid, uneven spacing, so there are no cracks), lit by the sun or the moon, glinting, and faded into the fog
 * by hand. Builds quads relative to the camera; each Minecraft version uploads them its own way.
 */
public final class SeaSurface {
    private static float[] buf = new float[0];
    private static int count;

    private SeaSurface() {}

    /** quads to draw this frame: x, y, z, r, g, b, a per vertex, four vertices a quad; returns the vertex count */
    public static int build(Camera cam, float pt) {
        Minecraft mc = Minecraft.getInstance();
        count = 0;
        if (mc.level == null || mc.level.dimension() != SeaEvents.SEA) return 0;
        Vec3 c = cam.getPosition();
        if (c.y < Waves.MEAN - 0.4) return 0;
        double t = Waves.time(mc.level, pt);
        float[] fog = RenderSystem.getShaderFogColor();
        float fogStart = RenderSystem.getShaderFogStart(), fogEnd = RenderSystem.getShaderFogEnd();
        double reach = Math.min(260, Math.max(40, fogEnd + 8));
        // light: the sun by day, the blood moon by night
        double a = mc.level.getSunAngle(pt);
        double sx = -Math.sin(a), sy = Math.cos(a);
        boolean day = sy > -0.05;
        double lx = day ? sx : -sx, ly = Math.abs(sy) * 0.9 + 0.1, lz = 0.25;
        double ll = Math.sqrt(lx * lx + ly * ly + lz * lz);
        lx /= ll; ly /= ll; lz /= ll;
        float lightK = day ? (float) Math.min(1, 0.35 + sy) : 0.35f;
        float[] specC = day ? new float[]{1.0f, 0.82f, 0.74f} : new float[]{0.9f, 0.12f, 0.08f};
        // the grid: 1 block near the eye, coarser further out, snapped to the world so it never swims
        int snap = 8;
        double ox = Math.floor(c.x / snap) * snap, oz = Math.floor(c.z / snap) * snap;
        double[] xs = axis(ox, reach), zs = axis(oz, reach);
        int nx = xs.length, nz = zs.length;
        int need = (nx - 1) * (nz - 1) * 4 * 7;
        if (buf.length < need) buf = new float[need];
        // vertex values once per grid point
        float[] vy = new float[nx * nz];
        float[][] vc = new float[nx * nz][];
        for (int i = 0; i < nx; i++)
            for (int j = 0; j < nz; j++) {
                double x = xs[i], z = zs[j];
                double h = Waves.height(x, z, t);
                double[] s = Waves.slope(x, z, t);
                double nxv = -s[0], nyv = 1, nzv = -s[1];
                double nl = Math.sqrt(nxv * nxv + nyv * nyv + nzv * nzv);
                nxv /= nl; nyv /= nl; nzv /= nl;
                double y = Waves.MEAN + h;
                double vxv = c.x - x, vyv = c.y - y, vzv = c.z - z;
                double dist = Math.sqrt(vxv * vxv + vzv * vzv);
                double vl = Math.sqrt(vxv * vxv + vyv * vyv + vzv * vzv) + 1e-6;
                vxv /= vl; vyv /= vl; vzv /= vl;
                double diff = Math.max(0, nxv * lx + nyv * ly + nzv * lz);
                double hx = lx + vxv, hy = ly + vyv, hz = lz + vzv;
                double hl = Math.sqrt(hx * hx + hy * hy + hz * hz) + 1e-6;
                double spec = Math.pow(Math.max(0, (nxv * hx + nyv * hy + nzv * hz) / hl), 60) * (day ? 1.4 : 0.8);
                double fres = Math.pow(1 - Math.max(0, nxv * vxv + nyv * vyv + nzv * vzv), 4);
                double crest = Math.max(0, Math.min(1, (h + 0.35) / 0.8));
                crest = crest * crest * (3 - 2 * crest);
                double lit = 0.35 + 0.9 * diff * lightK;
                double br = 0.10 + 0.34 * crest, bg = 0.004 + 0.022 * crest, bb = 0.010 + 0.02 * crest;
                float r = (float) (br * lit + spec * specC[0] * lightK * 1.6 + fres * fog[0] * 0.55);
                float g = (float) (bg * lit + spec * specC[1] * lightK * 0.9 + fres * fog[1] * 0.55);
                float b = (float) (bb * lit + spec * specC[2] * lightK * 0.8 + fres * fog[2] * 0.55);
                float f = (float) Math.pow(Math.max(0, Math.min(1, (dist - fogStart) / Math.max(1, fogEnd - fogStart))), 1.25);
                r = r * (1 - f) + fog[0] * f;
                g = g * (1 - f) + fog[1] * f;
                b = b * (1 - f) + fog[2] * f;
                int k = i * nz + j;
                vy[k] = (float) (y - c.y);
                vc[k] = new float[]{Math.min(1, r), Math.min(1, g), Math.min(1, b)};
            }
        int p = 0;
        for (int i = 0; i < nx - 1; i++)
            for (int j = 0; j < nz - 1; j++) {
                int[][] q = {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}};
                for (int[] v : q) {
                    int k = v[0] * nz + v[1];
                    buf[p++] = (float) (xs[v[0]] - c.x);
                    buf[p++] = vy[k];
                    buf[p++] = (float) (zs[v[1]] - c.z);
                    buf[p++] = vc[k][0];
                    buf[p++] = vc[k][1];
                    buf[p++] = vc[k][2];
                    buf[p++] = 1f;
                }
            }
        count = p / 7;
        return count;
    }

    private static double[] axis(double o, double reach) {
        java.util.ArrayList<Double> v = new java.util.ArrayList<>();
        for (double d = -Math.ceil(reach / 8) * 8; d < -40; d += 8) v.add(o + d);
        for (double d = -40; d < 40; d += 1) v.add(o + d);
        for (double d = 40; d <= Math.ceil(reach / 8) * 8; d += 8) v.add(o + d);
        double[] r = new double[v.size()];
        for (int i = 0; i < r.length; i++) r[i] = v.get(i);
        return r;
    }

    public static float[] data() { return buf; }
}
