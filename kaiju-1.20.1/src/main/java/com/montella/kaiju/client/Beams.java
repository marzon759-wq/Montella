package com.montella.kaiju.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Additive energy effects: atomic breath, gravity-beam lightning, charge glows, shockwave rings. */
public final class Beams {
    private Beams() {}

    private static void quad(VertexConsumer vc, Matrix4f m, Vector3f a, Vector3f b, Vector3f side, float wa, float wb,
                             float r, float g, float bl, float alpha) {
        vc.vertex(m, a.x - side.x * wa, a.y - side.y * wa, a.z - side.z * wa).color(r, g, bl, alpha).endVertex();
        vc.vertex(m, b.x - side.x * wb, b.y - side.y * wb, b.z - side.z * wb).color(r, g, bl, alpha).endVertex();
        vc.vertex(m, b.x + side.x * wb, b.y + side.y * wb, b.z + side.z * wb).color(r, g, bl, alpha).endVertex();
        vc.vertex(m, a.x + side.x * wa, a.y + side.y * wa, a.z + side.z * wa).color(r, g, bl, alpha).endVertex();
    }

    /** A soft cylinder of crossed planes from a to b. */
    public static void tube(VertexConsumer vc, Matrix4f m, Vector3f a, Vector3f b, float wa, float wb, float r, float g, float bl, float alpha, int planes, float twist) {
        Vector3f d = new Vector3f(b).sub(a);
        if (d.lengthSquared() < 1e-6f) return;
        d.normalize();
        Vector3f ref = Math.abs(d.y) > 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(d).cross(ref).normalize(), v = new Vector3f(d).cross(u).normalize();
        for (int i = 0; i < planes; i++) {
            double ang = Math.PI * i / planes + twist;
            Vector3f side = new Vector3f(u).mul((float) Math.cos(ang)).add(new Vector3f(v).mul((float) Math.sin(ang)));
            quad(vc, m, a, b, side, wa, wb, r, g, bl, alpha);
        }
    }

    /** Godzilla's atomic breath: white core, cyan body, blue haze and a spiral of energy around it. */
    public static void atomic(PoseStack ps, MultiBufferSource buf, Vector3f from, Vector3f to, float power, float age, boolean clash) {
        if (power <= 0.01f) return;
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        Matrix4f m = ps.last().pose();
        float flick = 0.9f + 0.1f * (float) Math.sin(age * 3.1f);
        float p = power * flick;
        // fading: the end stays, the start detaches from the mouth as the beam dies
        Vector3f a = new Vector3f(from);
        if (power < 1f) a.lerp(to, (1 - power) * 0.35f);
        float spread = 1f + (1 - power) * 1.5f;
        tube(vc, m, a, to, 3.8f * p * spread, 5.0f * p * spread, 0.05f, 0.25f, 0.9f, 0.2f * power, 6, age * 0.05f);
        tube(vc, m, a, to, 2.2f * p, 2.8f * p, 0.15f, 0.6f, 1.0f, 0.5f * power, 6, age * 0.1f);
        tube(vc, m, a, to, 1.0f * p, 1.2f * p, 0.85f, 0.95f, 1.0f, 0.95f * power, 4, 0);
        // spiral
        Vector3f d = new Vector3f(to).sub(a);
        float len = d.length();
        if (len > 1) {
            d.div(len);
            Vector3f ref = Math.abs(d.y) > 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
            Vector3f u = new Vector3f(d).cross(ref).normalize(), v = new Vector3f(d).cross(u).normalize();
            int n = (int) Math.min(240, len * 2.5f);
            Vector3f prev = null;
            for (int s = 0; s <= n; s++) {
                float t = s / (float) n;
                float ang = t * len * 0.9f - age * 0.8f;
                float rad = 1.8f * p;
                Vector3f pt = new Vector3f(a).add(new Vector3f(d).mul(t * len))
                        .add(new Vector3f(u).mul((float) Math.cos(ang) * rad)).add(new Vector3f(v).mul((float) Math.sin(ang) * rad));
                if (prev != null) tube(vc, m, prev, pt, 0.25f * p, 0.25f * p, 0.4f, 0.8f, 1f, 0.55f * power, 2, 0);
                prev = pt;
            }
        }
        glow(vc, m, from, 2.4f * p, 0.4f, 0.8f, 1f, 0.6f * power);
        glow(vc, m, to, (clash ? 6f : 3.5f) * p, 0.6f, 0.85f, 1f, 0.55f * power);
    }

    /** Ghidorah's gravity beams: jagged branching lightning, regenerated a few times per second. */
    public static void lightning(PoseStack ps, MultiBufferSource buf, Vector3f from, Vector3f to, float power, int seedTick, int salt, boolean clash) {
        if (power <= 0.01f) return;
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        Matrix4f m = ps.last().pose();
        RandomSource rnd = RandomSource.create(seedTick * 31L + salt * 7919L);
        Vector3f a = new Vector3f(from);
        if (power < 1f) a.lerp(to, (1 - power) * 0.4f);
        bolt(vc, m, rnd, a, to, 1.0f * power, power, 3);
        glow(vc, m, from, 1.6f * power, 1f, 0.85f, 0.3f, 0.6f * power);
        glow(vc, m, to, (clash ? 6f : 3f) * power, 1f, 0.9f, 0.4f, 0.5f * power);
    }

    private static void bolt(VertexConsumer vc, Matrix4f m, RandomSource rnd, Vector3f a, Vector3f b, float width, float alpha, int depth) {
        float len = a.distance(b);
        int n = Math.max(3, (int) (len / 2.2f));
        Vector3f prev = new Vector3f(a);
        float jit = Math.min(2.2f, len * 0.08f);
        for (int i = 1; i <= n; i++) {
            float t = i / (float) n;
            Vector3f p = new Vector3f(a).lerp(b, t);
            if (i < n) p.add((rnd.nextFloat() - 0.5f) * jit * 2, (rnd.nextFloat() - 0.5f) * jit * 2, (rnd.nextFloat() - 0.5f) * jit * 2);
            tube(vc, m, prev, p, width * 1.8f, width * 1.8f, 0.9f, 0.6f, 0.1f, 0.25f * alpha, 3, 0);
            tube(vc, m, prev, p, width * 0.6f, width * 0.6f, 1f, 0.95f, 0.6f, 0.9f * alpha, 3, 0);
            if (depth > 0 && rnd.nextFloat() < 0.18f) {
                Vector3f dir = new Vector3f(b).sub(a).normalize().mul(len * 0.12f)
                        .add((rnd.nextFloat() - 0.5f) * 6, (rnd.nextFloat() - 0.5f) * 6, (rnd.nextFloat() - 0.5f) * 6);
                bolt(vc, m, rnd, p, new Vector3f(p).add(dir), width * 0.5f, alpha * 0.8f, depth - 1);
            }
            prev = p;
        }
    }

    /** Camera-independent glowing star (crossed quads). */
    public static void glow(VertexConsumer vc, Matrix4f m, Vector3f c, float r, float cr, float cg, float cb, float a) {
        for (int i = 0; i < 3; i++) {
            Vector3f d = i == 0 ? new Vector3f(1, 0, 0) : i == 1 ? new Vector3f(0, 1, 0) : new Vector3f(0, 0, 1);
            Vector3f s = i == 0 ? new Vector3f(0, 1, 0) : i == 1 ? new Vector3f(0, 0, 1) : new Vector3f(1, 0, 0);
            Vector3f p0 = new Vector3f(c).sub(new Vector3f(d).mul(r)), p1 = new Vector3f(c).add(new Vector3f(d).mul(r));
            vc.vertex(m, p0.x, p0.y, p0.z).color(cr, cg, cb, 0f).endVertex();
            vc.vertex(m, c.x - s.x * r, c.y - s.y * r, c.z - s.z * r).color(cr, cg, cb, 0f).endVertex();
            vc.vertex(m, p1.x, p1.y, p1.z).color(cr, cg, cb, 0f).endVertex();
            vc.vertex(m, c.x + s.x * r * 0.2f, c.y + s.y * r * 0.2f, c.z + s.z * r * 0.2f).color(cr, cg, cb, a).endVertex();
            quadCentered(vc, m, c, d, s, r * 0.5f, cr, cg, cb, a);
        }
    }

    private static void quadCentered(VertexConsumer vc, Matrix4f m, Vector3f c, Vector3f d, Vector3f s, float r, float cr, float cg, float cb, float a) {
        vc.vertex(m, c.x - d.x * r - s.x * r, c.y - d.y * r - s.y * r, c.z - d.z * r - s.z * r).color(cr, cg, cb, a).endVertex();
        vc.vertex(m, c.x + d.x * r - s.x * r, c.y + d.y * r - s.y * r, c.z + d.z * r - s.z * r).color(cr, cg, cb, a).endVertex();
        vc.vertex(m, c.x + d.x * r + s.x * r, c.y + d.y * r + s.y * r, c.z + d.z * r + s.z * r).color(cr, cg, cb, a).endVertex();
        vc.vertex(m, c.x - d.x * r + s.x * r, c.y - d.y * r + s.y * r, c.z - d.z * r + s.z * r).color(cr, cg, cb, a).endVertex();
    }

    /** Expanding horizontal shockwave ring. */
    public static void ring(PoseStack ps, MultiBufferSource buf, float radius, float y, float width, float r, float g, float b, float a) {
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        Matrix4f m = ps.last().pose();
        int n = 64;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float ri = radius - width, ro = radius + width;
            vc.vertex(m, c0 * ri, y, s0 * ri).color(r, g, b, 0f).endVertex();
            vc.vertex(m, c1 * ri, y, s1 * ri).color(r, g, b, 0f).endVertex();
            vc.vertex(m, c1 * radius, y + width * 0.6f, s1 * radius).color(r, g, b, a).endVertex();
            vc.vertex(m, c0 * radius, y + width * 0.6f, s0 * radius).color(r, g, b, a).endVertex();
            vc.vertex(m, c0 * radius, y + width * 0.6f, s0 * radius).color(r, g, b, a).endVertex();
            vc.vertex(m, c1 * radius, y + width * 0.6f, s1 * radius).color(r, g, b, a).endVertex();
            vc.vertex(m, c1 * ro, y, s1 * ro).color(r, g, b, 0f).endVertex();
            vc.vertex(m, c0 * ro, y, s0 * ro).color(r, g, b, 0f).endVertex();
        }
    }
}
