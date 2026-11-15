package com.krolasyon.titans.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Immediate-mode builder for smooth lofted bodies: a chain of joints becomes a tube of elliptical rings. */
final class Mesh {
    static final int MAXR = 96, MAXS = 16;
    static final float PI = (float) Math.PI;

    interface Paint { void at(int ring, float f, float ca, float sa, float[] out); }

    VertexConsumer vc;
    Matrix4f pose;
    Matrix3f nrm;
    int light, overlay;
    boolean plain;
    float alpha = 1;

    final Vector3f[] C = arr(MAXR), U = arr(MAXR), R = arr(MAXR), T = arr(MAXR);
    final float[] RX = new float[MAXR], RYT = new float[MAXR], RYB = new float[MAXR], VV = new float[MAXR];
    int n;
    private final float[][] ra = new float[MAXS + 1][10], rb = new float[MAXS + 1][10];
    private final float[] col = new float[4];
    private final Vector3f a = new Vector3f(), b = new Vector3f(), c = new Vector3f(), d = new Vector3f();

    private static Vector3f[] arr(int n) {
        Vector3f[] x = new Vector3f[n];
        for (int i = 0; i < n; i++) x[i] = new Vector3f();
        return x;
    }

    void begin(VertexConsumer vc, PoseStack.Pose p, int light, int overlay, boolean plain) {
        this.vc = vc; pose = p.pose(); nrm = p.normal(); this.light = light; this.overlay = overlay; this.plain = plain; alpha = 1;
    }

    void vert(float x, float y, float z, float r, float g, float bl, float u, float v, float nx, float ny, float nz, float glow) {
        r = Mth.clamp(r, 0, 1); g = Mth.clamp(g, 0, 1); bl = Mth.clamp(bl, 0, 1);
        if (plain) { vc.vertex(pose, x, y, z).color(r, g, bl, alpha).endVertex(); return; }
        int l = light;
        if (glow > 0) {
            int e = (int) (Mth.clamp(glow, 0, 1) * 15);
            l = LightTexture.pack(Math.max(LightTexture.block(light), e), Math.max(LightTexture.sky(light), e));
        }
        vc.vertex(pose, x, y, z).color(r, g, bl, alpha).uv(u, v).overlayCoords(overlay).uv2(l).normal(nrm, nx, ny, nz).endVertex();
    }

    private static void cr(Vector3f[] p, int count, int i, float t, Vector3f out) {
        Vector3f p0 = p[Math.max(i - 1, 0)], p1 = p[i], p2 = p[i + 1], p3 = p[Math.min(i + 2, count - 1)];
        float t2 = t * t, t3 = t2 * t;
        float a0 = -0.5f * t3 + t2 - 0.5f * t, a1 = 1.5f * t3 - 2.5f * t2 + 1, a2 = -1.5f * t3 + 2 * t2 + 0.5f * t, a3 = 0.5f * t3 - 0.5f * t2;
        out.set(p0.x * a0 + p1.x * a1 + p2.x * a2 + p3.x * a3, p0.y * a0 + p1.y * a1 + p2.y * a2 + p3.y * a3, p0.z * a0 + p1.z * a1 + p2.z * a2 + p3.z * a3);
    }

    /** Resample a joint chain (Catmull-Rom) into rings. */
    void chain(Vector3f[] pts, float[] rad, int count, int sub, Vector3f up0, float flat) {
        n = 0;
        for (int i = 0; i < count - 1; i++)
            for (int k = 0; k < sub; k++) {
                float t = k / (float) sub;
                cr(pts, count, i, t, C[n]);
                RX[n] = Mth.lerp(t, rad[i], rad[i + 1]);
                n++;
            }
        C[n].set(pts[count - 1]);
        RX[n] = rad[count - 1];
        n++;
        frames(up0);
        for (int i = 0; i < n; i++) RYT[i] = RYB[i] = RX[i] * flat;
    }

    /** Parallel-transport frames along the current ring centres. */
    void frames(Vector3f up0) {
        for (int i = 0; i < n; i++) {
            T[i].set(C[Math.min(i + 1, n - 1)]).sub(C[Math.max(i - 1, 0)]);
            if (T[i].lengthSquared() < 1e-8f) T[i].set(0, 0, 1);
            T[i].normalize();
            Vector3f up = i == 0 ? up0 : U[i - 1];
            U[i].set(up).fma(-up.dot(T[i]), T[i]);
            if (U[i].lengthSquared() < 1e-6f) { U[i].set(T[i].y, T[i].z, T[i].x); U[i].fma(-U[i].dot(T[i]), T[i]); }
            U[i].normalize();
            T[i].cross(U[i], R[i]);
            VV[i] = i == 0 ? 0 : VV[i - 1] + C[i].distance(C[i - 1]);
        }
    }

    private void ring(int i, int sides, Paint paint, float[][] out) {
        float f = n > 1 ? i / (float) (n - 1) : 0;
        for (int j = 0; j <= sides; j++) {
            float ang = -PI / 2 + 2 * PI * j / sides, ca = Mth.cos(ang), sa = Mth.sin(ang);
            float rx = RX[i], ry = sa >= 0 ? RYT[i] : RYB[i];
            float[] o = out[j];
            o[0] = C[i].x + R[i].x * ca * rx + U[i].x * sa * ry;
            o[1] = C[i].y + R[i].y * ca * rx + U[i].y * sa * ry;
            o[2] = C[i].z + R[i].z * ca * rx + U[i].z * sa * ry;
            float nx = R[i].x * ca * ry + U[i].x * sa * rx, ny = R[i].y * ca * ry + U[i].y * sa * rx, nz = R[i].z * ca * ry + U[i].z * sa * rx;
            float l = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            if (l < 1e-5f) { nx = U[i].x * sa; ny = U[i].y * sa; nz = U[i].z * sa; l = 1; }
            o[3] = nx / l; o[4] = ny / l; o[5] = nz / l;
            col[3] = 0;
            paint.at(i, f, ca, sa, col);
            o[6] = col[0]; o[7] = col[1]; o[8] = col[2]; o[9] = col[3];
        }
    }

    private void pv(float[] o, float u, float v) { vert(o[0], o[1], o[2], o[6], o[7], o[8], u, v, o[3], o[4], o[5], o[9]); }

    void loft(int sides, Paint paint, boolean capA, boolean capB, float uRep, float vScale) {
        if (!plain) { boxes(paint); return; }
        float[][] p = ra, q = rb;
        ring(0, sides, paint, p);
        if (capA) cap(0, sides, p, -1);
        for (int i = 1; i < n; i++) {
            ring(i, sides, paint, q);
            float v0 = VV[i - 1] * vScale, v1 = VV[i] * vScale;
            for (int j = 0; j < sides; j++) {
                float u0 = j * uRep / sides, u1 = (j + 1) * uRep / sides;
                pv(p[j], u0, v0); pv(p[j + 1], u1, v0); pv(q[j + 1], u1, v1); pv(q[j], u0, v1);
            }
            float[][] t = p; p = q; q = t;
        }
        if (capB) cap(n - 1, sides, p, 1);
    }

    private final float[][] k8 = new float[8][3];
    private static final int[][] FACES = {{0, 1, 5, 4}, {3, 2, 6, 7}, {1, 2, 6, 5}, {0, 3, 7, 4}, {0, 1, 2, 3}, {4, 5, 6, 7}};
    private static final float[][] FSA = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {0.7f, 0}, {0.7f, 0}};

    /** Minecraft-style body: every segment is its own hard-edged, slightly tapered box. */
    private void boxes(Paint paint) {
        final float px = 1 / 8f;
        for (int i = 0; i < n - 1; i++) {
            a.set(C[i + 1]).sub(C[i]);
            float len = a.length();
            if (len < 1e-4f) continue;
            a.div(len);
            b.set(U[i]).add(U[i + 1]);
            b.fma(-b.dot(a), a);
            if (b.lengthSquared() < 1e-6f) b.set(U[i]);
            b.normalize();
            a.cross(b, c);
            float f = n > 1 ? (i + 0.5f) / (n - 1) : 0;
            for (int e = 0; e < 2; e++) {
                int j = i + e;
                float ext = (e == 0 ? -1 : 1) * Math.min(RX[j] * 0.3f, len * 0.3f);
                float rx = RX[j], rt = RYT[j], rbm = RYB[j];
                float cx = C[j].x + a.x * ext, cy = C[j].y + a.y * ext, cz = C[j].z + a.z * ext;
                set(k8[e * 4], cx, cy, cz, -rx, rt);
                set(k8[e * 4 + 1], cx, cy, cz, rx, rt);
                set(k8[e * 4 + 2], cx, cy, cz, rx, -rbm);
                set(k8[e * 4 + 3], cx, cy, cz, -rx, -rbm);
            }
            float w = (RX[i] + RX[i + 1]), h = (RYT[i] + RYB[i] + RYT[i + 1] + RYB[i + 1]) / 2;
            for (int q = 0; q < 6; q++) {
                int[] fc = FACES[q];
                float nx, ny, nz, us, vs;
                switch (q) {
                    case 0 -> { nx = b.x; ny = b.y; nz = b.z; us = w; vs = len; }
                    case 1 -> { nx = -b.x; ny = -b.y; nz = -b.z; us = w; vs = len; }
                    case 2 -> { nx = c.x; ny = c.y; nz = c.z; us = h; vs = len; }
                    case 3 -> { nx = -c.x; ny = -c.y; nz = -c.z; us = h; vs = len; }
                    case 4 -> { nx = -a.x; ny = -a.y; nz = -a.z; us = w; vs = h; }
                    default -> { nx = a.x; ny = a.y; nz = a.z; us = w; vs = h; }
                }
                col[3] = 0;
                paint.at(q == 5 ? i + 1 : i, q == 5 ? (i + 1f) / (n - 1) : q == 4 ? i / (float) (n - 1) : f, FSA[q][0], FSA[q][1], col);
                float u0 = (q * 5 + i * 3) % 8 * px, v0 = VV[i] * px;
                float du = Math.round(us * 4) / 32f, dv = Math.round(vs * 4) / 32f;
                float[] p0 = k8[fc[0]], p1 = k8[fc[1]], p2 = k8[fc[2]], p3 = k8[fc[3]];
                boolean pl = scales3d && q < 4 && us > 1.3f;
                if (pl) { col[0] *= 0.55f; col[1] *= 0.55f; col[2] *= 0.55f; }
                vert(p0[0], p0[1], p0[2], col[0], col[1], col[2], u0, v0, nx, ny, nz, col[3]);
                vert(p1[0], p1[1], p1[2], col[0], col[1], col[2], u0 + du, v0, nx, ny, nz, col[3]);
                vert(p2[0], p2[1], p2[2], col[0], col[1], col[2], u0 + du, v0 + dv, nx, ny, nz, col[3]);
                vert(p3[0], p3[1], p3[2], col[0], col[1], col[2], u0, v0 + dv, nx, ny, nz, col[3]);
                if (pl) { col[0] /= 0.55f; col[1] /= 0.55f; col[2] /= 0.55f; plates(p0, p1, p2, p3, us, vs, nx, ny, nz, q == 1, i * 31 + q * 7); }
            }
        }
    }

    boolean scales3d;
    private static final float[][] OB = {{-1, 1, -1}, {-1, 1, 1}, {-1, -1, 1}, {-1, -1, -1}, {1, 1, -1}, {1, 1, 1}, {1, -1, 1}, {1, -1, -1}};

    /** Oriented box: half extents hf/hu/hr along the three axes. */
    void obox(Vector3f ctr, Vector3f F, Vector3f Uu, Vector3f Rr, float hf, float hu, float hr, float r, float g, float bl, float glow) {
        for (int k = 0; k < 8; k++) {
            float f = OB[k][0] * hf, u = OB[k][1] * hu, w = OB[k][2] * hr;
            k8[k][0] = ctr.x + F.x * f + Uu.x * u + Rr.x * w;
            k8[k][1] = ctr.y + F.y * f + Uu.y * u + Rr.y * w;
            k8[k][2] = ctr.z + F.z * f + Uu.z * u + Rr.z * w;
        }
        for (int q = 0; q < 6; q++) {
            int[] fc = FACES[q];
            float sg = (q & 1) == 0 ? 1 : -1;
            Vector3f nv = q < 2 ? Uu : q < 4 ? Rr : F;
            if (q >= 4) sg = q == 4 ? -1 : 1;
            float sh = q == 0 ? 1.1f : q == 1 ? 0.7f : 0.9f;
            for (int k = 0; k < 4; k++) {
                float[] pp = k8[fc[k]];
                vert(pp[0], pp[1], pp[2], r * sh, g * sh, bl * sh, (k == 1 || k == 2) ? 0.125f : 0, k >= 2 ? 0.125f : 0, nv.x * sg, nv.y * sg, nv.z * sg, glow);
            }
        }
    }
    private final float[][] s8 = new float[8][3];

    private static void bil(float[] p0, float[] p1, float[] p2, float[] p3, float u, float v, float lift, float nx, float ny, float nz, float[] o) {
        for (int k = 0; k < 3; k++) {
            float t = p0[k] + (p1[k] - p0[k]) * u, w = p3[k] + (p2[k] - p3[k]) * u;
            o[k] = t + (w - t) * v;
        }
        o[0] += nx * lift; o[1] += ny * lift; o[2] += nz * lift;
    }

    /** Real relief: every face is tiled with raised, overlapping scale plates (wide scutes on the belly). */
    private void plates(float[] p0, float[] p1, float[] p2, float[] p3, float us, float vs, float nx, float ny, float nz, boolean belly, int seed) {
        int nu = belly ? Math.max(1, Math.round(us / 5f)) : Math.max(1, Math.round(us / 1.7f));
        int nv = Math.max(1, Math.round(vs / (belly ? 1.5f : 1.7f)));
        float size = Math.min(us / nu, vs / nv);
        float h0 = size * 0.08f, h1 = size * (belly ? 0.2f : 0.3f);
        float cr = col[0], cg = col[1], cb = col[2], gl = col[3];
        for (int b2 = 0; b2 < nv; b2++) for (int a2 = 0; a2 < nu; a2++) {
            float off = (!belly && (b2 & 1) == 1) ? 0.5f : 0;
            float ua = (a2 + off) / nu, ub = (a2 + off + 1) / nu;
            if (ub > 1.001f) { ub = 1; if (ub - ua < 0.3f / nu) continue; }
            float va = b2 / (float) nv, vb = (b2 + 1) / (float) nv;
            float mu = (ub - ua) * 0.1f, mv = (vb - va) * 0.06f;
            bil(p0, p1, p2, p3, ua, va, 0, nx, ny, nz, s8[0]); bil(p0, p1, p2, p3, ub, va, 0, nx, ny, nz, s8[1]);
            bil(p0, p1, p2, p3, ub, vb, 0, nx, ny, nz, s8[2]); bil(p0, p1, p2, p3, ua, vb, 0, nx, ny, nz, s8[3]);
            bil(p0, p1, p2, p3, ua + mu, va + mv, h0, nx, ny, nz, s8[4]); bil(p0, p1, p2, p3, ub - mu, va + mv, h0, nx, ny, nz, s8[5]);
            bil(p0, p1, p2, p3, ub - mu, vb + mv, h1, nx, ny, nz, s8[6]); bil(p0, p1, p2, p3, ua + mu, vb + mv, h1, nx, ny, nz, s8[7]);
            int hsh = (seed * 73856093) ^ (a2 * 19349663) ^ (b2 * 83492791);
            float var = 0.9f + ((hsh >>> 8) & 255) / 255f * 0.22f;
            float r = cr * var, g = cg * var, bl = cb * var;
            float tu = (hsh & 7) / 8f, tv = ((hsh >> 3) & 7) / 8f;
            // top
            pq(s8[4], s8[5], s8[6], s8[7], r * 1.12f, g * 1.12f, bl * 1.12f, tu, tv, nx, ny, nz, gl);
            // rim: the raised trailing edge and the two flanks
            pq(s8[7], s8[6], s8[2], s8[3], r * 0.5f, g * 0.5f, bl * 0.5f, tu, tv, nx, ny, nz, gl);
            pq(s8[4], s8[7], s8[3], s8[0], r * 0.68f, g * 0.68f, bl * 0.68f, tu, tv, nx, ny, nz, gl);
            pq(s8[6], s8[5], s8[1], s8[2], r * 0.68f, g * 0.68f, bl * 0.68f, tu, tv, nx, ny, nz, gl);
            pq(s8[5], s8[4], s8[0], s8[1], r * 0.85f, g * 0.85f, bl * 0.85f, tu, tv, nx, ny, nz, gl);
        }
        col[0] = cr; col[1] = cg; col[2] = cb;
    }

    private void pq(float[] a0, float[] a1, float[] a2, float[] a3, float r, float g, float bl, float u, float v, float nx, float ny, float nz, float glow) {
        vert(a0[0], a0[1], a0[2], r, g, bl, u, v, nx, ny, nz, glow);
        vert(a1[0], a1[1], a1[2], r, g, bl, u + 0.125f, v, nx, ny, nz, glow);
        vert(a2[0], a2[1], a2[2], r, g, bl, u + 0.125f, v + 0.125f, nx, ny, nz, glow);
        vert(a3[0], a3[1], a3[2], r, g, bl, u, v + 0.125f, nx, ny, nz, glow);
    }

    private void set(float[] o, float cx, float cy, float cz, float r, float u) {
        o[0] = cx + c.x * r + b.x * u; o[1] = cy + c.y * r + b.y * u; o[2] = cz + c.z * r + b.z * u;
    }

    private void cap(int i, int sides, float[][] r, float sign) {
        float nx = T[i].x * sign, ny = T[i].y * sign, nz = T[i].z * sign;
        float[] m = r[sides / 2];
        for (int j = 0; j < sides; j++) {
            float[] o = r[j], o2 = r[j + 1];
            vert(C[i].x, C[i].y, C[i].z, m[6], m[7], m[8], 0.5f, 0.5f, nx, ny, nz, m[9]);
            vert(C[i].x, C[i].y, C[i].z, m[6], m[7], m[8], 0.5f, 0.5f, nx, ny, nz, m[9]);
            vert(o[0], o[1], o[2], o[6], o[7], o[8], 0, 0, nx, ny, nz, o[9]);
            vert(o2[0], o2[1], o2[2], o2[6], o2[7], o2[8], 1, 0, nx, ny, nz, o2[9]);
        }
    }

    /** Pointed spike of the given length along {@code dir}. */
    void cone(Vector3f base, Vector3f dir, float len, float rad, int sides, float r0, float g0, float b0, float r1, float g1, float b1, float glow) {
        a.set(dir).normalize();
        if (Math.abs(a.y) < 0.9f) b.set(0, 1, 0); else b.set(1, 0, 0);
        a.cross(b, c).normalize();
        a.cross(c, d).normalize();
        float tx = base.x + a.x * len, ty = base.y + a.y * len, tz = base.z + a.z * len;
        for (int j = 0; j < sides; j++) {
            float a0 = 2 * PI * j / sides, a1 = 2 * PI * (j + 1) / sides, am = (a0 + a1) / 2;
            float x0 = Mth.cos(a0), y0 = Mth.sin(a0), x1 = Mth.cos(a1), y1 = Mth.sin(a1), xm = Mth.cos(am), ym = Mth.sin(am);
            float nx = c.x * xm + d.x * ym, ny = c.y * xm + d.y * ym, nz = c.z * xm + d.z * ym;
            vert(base.x + (c.x * x0 + d.x * y0) * rad, base.y + (c.y * x0 + d.y * y0) * rad, base.z + (c.z * x0 + d.z * y0) * rad, r0, g0, b0, 0, 0, nx, ny, nz, glow * 0.4f);
            vert(base.x + (c.x * x1 + d.x * y1) * rad, base.y + (c.y * x1 + d.y * y1) * rad, base.z + (c.z * x1 + d.z * y1) * rad, r0, g0, b0, 0.2f, 0, nx, ny, nz, glow * 0.4f);
            vert(tx, ty, tz, r1, g1, b1, 0.1f, 0.3f, nx, ny, nz, glow);
            vert(tx, ty, tz, r1, g1, b1, 0.1f, 0.3f, nx, ny, nz, glow);
        }
    }

    /** Flat crystal shard: a diamond base rising to an off-centre tip. */
    void shard(Vector3f base, Vector3f along, Vector3f up, Vector3f side, float h, float w, float th, float sweep,
               float r0, float g0, float b0, float r1, float g1, float b1, float glow) {
        float fx = base.x + along.x * w / 2, fy = base.y + along.y * w / 2, fz = base.z + along.z * w / 2;
        float kx = base.x - along.x * w / 2, ky = base.y - along.y * w / 2, kz = base.z - along.z * w / 2;
        float lx = base.x - side.x * th / 2, ly = base.y - side.y * th / 2, lz = base.z - side.z * th / 2;
        float rx = base.x + side.x * th / 2, ry = base.y + side.y * th / 2, rz = base.z + side.z * th / 2;
        float tx = base.x + up.x * h + along.x * sweep, ty = base.y + up.y * h + along.y * sweep, tz = base.z + up.z * h + along.z * sweep;
        face(fx, fy, fz, rx, ry, rz, tx, ty, tz, r0, g0, b0, r1, g1, b1, glow);
        face(rx, ry, rz, kx, ky, kz, tx, ty, tz, r0, g0, b0, r1, g1, b1, glow);
        face(kx, ky, kz, lx, ly, lz, tx, ty, tz, r0, g0, b0, r1, g1, b1, glow);
        face(lx, ly, lz, fx, fy, fz, tx, ty, tz, r0, g0, b0, r1, g1, b1, glow);
    }

    private void face(float ax, float ay, float az, float bx, float by, float bz, float tx, float ty, float tz,
                      float r0, float g0, float b0, float r1, float g1, float b1, float glow) {
        float ux = bx - ax, uy = by - ay, uz = bz - az, vx = tx - ax, vy = ty - ay, vz = tz - az;
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float l = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-6f) return;
        nx /= l; ny /= l; nz /= l;
        vert(ax, ay, az, r0, g0, b0, 0, 0, nx, ny, nz, glow * 0.35f);
        vert(bx, by, bz, r0, g0, b0, 0.25f, 0, nx, ny, nz, glow * 0.35f);
        vert(tx, ty, tz, r1, g1, b1, 0.12f, 0.4f, nx, ny, nz, glow);
        vert(tx, ty, tz, r1, g1, b1, 0.12f, 0.4f, nx, ny, nz, glow);
    }

    void tri(Vector3f p0, Vector3f p1, Vector3f p2, float r0, float g0, float b0, float r1, float g1, float b1, float uv) {
        float ux = p1.x - p0.x, uy = p1.y - p0.y, uz = p1.z - p0.z, vx = p2.x - p0.x, vy = p2.y - p0.y, vz = p2.z - p0.z;
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float l = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-6f) return;
        nx /= l; ny /= l; nz /= l;
        if (ny < 0) { nx = -nx; ny = -ny; nz = -nz; }
        vert(p0.x, p0.y, p0.z, r0, g0, b0, 0, 0, nx, ny, nz, 0);
        vert(p0.x, p0.y, p0.z, r0, g0, b0, 0, 0, nx, ny, nz, 0);
        vert(p1.x, p1.y, p1.z, r1, g1, b1, uv, 0, nx, ny, nz, 0);
        vert(p2.x, p2.y, p2.z, r1, g1, b1, uv * 0.5f, uv, nx, ny, nz, 0);
    }

    /** Sphere made of rings; used for glows. */
    void ball(float x, float y, float z, float r, float cr, float cg, float cb) {
        n = 7;
        for (int i = 0; i < n; i++) {
            float th = PI * i / (n - 1);
            C[i].set(x, y + r * Mth.cos(th), z);
            RX[i] = RYT[i] = RYB[i] = Math.max(0.001f, r * Mth.sin(th));
            U[i].set(0, 0, 1); R[i].set(1, 0, 0); T[i].set(0, -1, 0); VV[i] = i;
        }
        loft(8, (i, f, ca, sa, o) -> { o[0] = cr; o[1] = cg; o[2] = cb; }, false, false, 1, 1);
    }

    /** Straight line of rings from a to b, ready for radius shaping and {@link #loft}. */
    void line(float ax, float ay, float az, float bx, float by, float bz, int segs) {
        n = Math.min(MAXR, segs + 1);
        for (int i = 0; i < n; i++) {
            float t = i / (float) (n - 1);
            C[i].set(ax + (bx - ax) * t, ay + (by - ay) * t, az + (bz - az) * t);
        }
    }
}
