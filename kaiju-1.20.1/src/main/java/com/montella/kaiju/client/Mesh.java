package com.montella.kaiju.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.montella.kaiju.rig.Skeleton;
import com.montella.kaiju.rig.Skeleton.Joint;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

/**
 * Organic meshes skinned onto a {@link Skeleton}: tubes whose rings ride on joints (smooth bending necks and tails),
 * asymmetric ellipsoid muscle masses, extruded leaf-shaped plates, tapered spikes and membranes stretched between bones.
 * Everything is regenerated every frame from the posed skeleton.
 */
public final class Mesh {
    /** Writes vertices with a fixed material colour. */
    public static final class Out {
        public VertexConsumer vc;
        public PoseStack.Pose pose;
        public int light, overlay;
        public float r = 1, g = 1, b = 1, a = 1;
        final Vector3f p = new Vector3f(), n = new Vector3f();

        void v(Vector3f pos, Vector3f nor, float u, float vv) {
            Matrix4f m = pose.pose();
            Matrix3f nm = pose.normal();
            vc.vertex(m, pos.x, pos.y, pos.z).color(r, g, b, a).uv(u, vv).overlayCoords(overlay).uv2(light).normal(nm, nor.x, nor.y, nor.z).endVertex();
        }

        void quad(Vector3f p0, Vector3f p1, Vector3f p2, Vector3f p3, Vector3f n0, Vector3f n1, Vector3f n2, Vector3f n3,
                  float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
            v(p0, n0, u0, v0); v(p1, n1, u1, v1); v(p2, n2, u2, v2); v(p3, n3, u3, v3);
        }
    }

    public abstract static class Shape {
        public int mat;
        public int glowTex = -1;
        public int glowGroup = 0;
        /** Visibility group, checked by the renderer (e.g. a severed head). -1 = always. */
        public int vis = -1;

        public Shape glow(int tex, int group) { glowTex = tex; glowGroup = group; return this; }

        public Shape vis(int v) { vis = v; return this; }

        abstract void emit(Out o, Skeleton s);
    }

    public final List<Shape> shapes = new ArrayList<>();

    public <T extends Shape> T add(T s) { shapes.add(s); return s; }

    // ------------------------------------------------------------------ helpers
    static Vector3f tp(Joint j, float x, float y, float z) { return j.world.transformPosition(x, y, z, new Vector3f()); }

    static Vector3f tn(Joint j, float x, float y, float z) {
        Vector3f v = j.world.transformDirection(x, y, z, new Vector3f());
        float l = v.length();
        return l < 1e-6f ? v.set(0, 1, 0) : v.div(l);
    }

    // ------------------------------------------------------------------ tube
    public static final class Ring {
        final Joint j; final float x, y, z, rx, ry;
        public Ring(Joint j, float x, float y, float z, float rx, float ry) { this.j = j; this.x = x; this.y = y; this.z = z; this.rx = rx; this.ry = ry; }
    }

    public static final class Tube extends Shape {
        final List<Ring> rings = new ArrayList<>();
        final int sides;
        final float uRep, vScale;
        int bellyMat = -1;
        float bellyHalfAngle = 0.9f;
        Vector3f tipOffset = null;
        Joint tipJoint = null;
        float flatten = 0;

        public Tube(int mat, int sides, float uRep, float vScale) { this.mat = mat; this.sides = sides; this.uRep = uRep; this.vScale = vScale; }

        public Tube ring(Joint j, float x, float y, float z, float rx, float ry) { rings.add(new Ring(j, x, y, z, rx, ry)); return this; }

        public Tube ring(Joint j, float rx, float ry) { return ring(j, 0, 0, 0, rx, ry); }

        public Tube belly(int m, float halfAngle) { bellyMat = m; bellyHalfAngle = halfAngle; return this; }

        public Tube tip(Joint j, float x, float y, float z) { tipJoint = j; tipOffset = new Vector3f(x, y, z); return this; }

        /** Copy of this tube restricted to belly faces, so the renderer can draw them with another texture. */
        public Tube bellyPart() {
            Tube t = new Tube(bellyMat, sides, uRep, vScale);
            t.rings.addAll(rings); t.bellyMat = -2; t.bellyHalfAngle = bellyHalfAngle;
            t.vis = vis;
            return t;
        }

        @Override
        void emit(Out o, Skeleton s) {
            int n = rings.size();
            Vector3f[][] P = new Vector3f[n][sides + 1], N = new Vector3f[n][sides + 1];
            float[] vv = new float[n];
            Vector3f prevC = null;
            float acc = 0;
            for (int i = 0; i < n; i++) {
                Ring r = rings.get(i);
                Vector3f c = tp(r.j, r.x, r.y, r.z);
                if (prevC != null) acc += c.distance(prevC);
                prevC = c;
                vv[i] = acc * vScale;
                for (int k = 0; k <= sides; k++) {
                    double th = Math.PI * 2 * k / sides - Math.PI / 2;
                    float cx = (float) Math.cos(th), sy = (float) Math.sin(th);
                    P[i][k] = tp(r.j, r.x + r.rx * cx, r.y + r.ry * sy, r.z);
                    N[i][k] = tn(r.j, cx / Math.max(r.rx, 0.01f), sy / Math.max(r.ry, 0.01f), 0);
                }
            }
            for (int i = 0; i < n - 1; i++) {
                for (int k = 0; k < sides; k++) {
                    double mid = Math.PI * 2 * (k + 0.5) / sides; // 0 at the belly line
                    double a = Math.min(mid, Math.PI * 2 - mid);
                    boolean belly = a < bellyHalfAngle;
                    if (bellyMat == -2 ? !belly : (bellyMat >= 0 && belly)) continue;
                    float u0 = uRep * k / sides, u1 = uRep * (k + 1) / sides;
                    o.quad(P[i][k], P[i + 1][k], P[i + 1][k + 1], P[i][k + 1], N[i][k], N[i + 1][k], N[i + 1][k + 1], N[i][k + 1],
                            u0, vv[i], u0, vv[i + 1], u1, vv[i + 1], u1, vv[i]);
                }
            }
            if (tipJoint != null && bellyMat != -2) {
                Vector3f tip = tp(tipJoint, tipOffset.x, tipOffset.y, tipOffset.z);
                int i = n - 1;
                Vector3f tn = new Vector3f(tip).sub(rings.get(i).j.world.getTranslation(new Vector3f())).normalize();
                float vt = vv[i] + tip.distance(tp(rings.get(i).j, rings.get(i).x, rings.get(i).y, rings.get(i).z)) * vScale;
                for (int k = 0; k < sides; k++) {
                    float u0 = uRep * k / sides, u1 = uRep * (k + 1) / sides;
                    o.quad(P[i][k], tip, tip, P[i][k + 1], N[i][k], tn, tn, N[i][k + 1], u0, vv[i], u0, vt, u1, vt, u1, vv[i]);
                }
            }
        }
    }

    // ------------------------------------------------------------------ ellipsoid
    public static final class Blob extends Shape {
        final Joint j;
        final float cx, cy, cz, rx, ryUp, ryDown, rzF, rzB;
        final Matrix3f rot = new Matrix3f();
        final int lat, lon;
        final float uRep, vRep;
        int bellyMat = -1;
        float bellyDot = 0.45f;
        boolean bellyOnly;

        public Blob(int mat, Joint j, float cx, float cy, float cz, float rx, float ryUp, float ryDown, float rzF, float rzB) {
            this.mat = mat; this.j = j; this.cx = cx; this.cy = cy; this.cz = cz;
            this.rx = rx; this.ryUp = ryUp; this.ryDown = ryDown; this.rzF = rzF; this.rzB = rzB;
            this.lat = 10; this.lon = 14;
            float size = Math.max(rx, Math.max(ryUp + ryDown, rzF + rzB) * 0.5f);
            this.uRep = Math.max(1, Math.round(size * 0.9f)); this.vRep = Math.max(1, Math.round(size * 0.5f));
        }

        public Blob(int mat, Joint j, float cx, float cy, float cz, float rx, float ry, float rz) { this(mat, j, cx, cy, cz, rx, ry, ry, rz, rz); }

        public Blob rot(float x, float y, float z) { rot.identity().rotateY(y).rotateX(x).rotateZ(z); return this; }

        public Blob belly(int m, float dot) { bellyMat = m; bellyDot = dot; return this; }

        public Blob bellyPart() {
            Blob b = new Blob(bellyMat, j, cx, cy, cz, rx, ryUp, ryDown, rzF, rzB);
            b.rot.set(rot); b.bellyMat = bellyMat; b.bellyDot = bellyDot; b.bellyOnly = true; b.vis = vis;
            return b;
        }

        @Override
        void emit(Out o, Skeleton s) {
            Vector3f[][] P = new Vector3f[lat + 1][lon + 1], N = new Vector3f[lat + 1][lon + 1];
            float[][] fz = new float[lat + 1][lon + 1];
            Vector3f tmp = new Vector3f(), nt = new Vector3f();
            for (int a = 0; a <= lat; a++) {
                double ph = Math.PI * a / lat;
                float sy = (float) Math.cos(ph), sr = (float) Math.sin(ph);
                for (int b = 0; b <= lon; b++) {
                    double th = Math.PI * 2 * b / lon;
                    float ux = (float) Math.sin(th) * sr, uz = (float) Math.cos(th) * sr;
                    float ry = sy > 0 ? ryUp : ryDown, rz = uz > 0 ? rzF : rzB;
                    tmp.set(ux * rx, sy * ry, uz * rz);
                    rot.transform(tmp);
                    P[a][b] = tp(j, cx + tmp.x, cy + tmp.y, cz + tmp.z);
                    nt.set(ux / rx, sy / ry, uz / rz);
                    rot.transform(nt);
                    fz[a][b] = nt.normalize().z;
                    N[a][b] = tn(j, nt.x, nt.y, nt.z);
                }
            }
            for (int a = 0; a < lat; a++) for (int b = 0; b < lon; b++) {
                if (bellyMat >= 0) {
                    float f = (fz[a][b] + fz[a + 1][b] + fz[a][b + 1] + fz[a + 1][b + 1]) * 0.25f;
                    boolean belly = f > bellyDot && a > 0 && a < lat - 1;
                    if (belly != bellyOnly) continue;
                }
                float u0 = uRep * b / lon, u1 = uRep * (b + 1) / lon, v0 = vRep * a / lat, v1 = vRep * (a + 1) / lat;
                o.quad(P[a][b], P[a + 1][b], P[a + 1][b + 1], P[a][b + 1], N[a][b], N[a + 1][b], N[a + 1][b + 1], N[a][b + 1],
                        u0, v0, u0, v1, u1, v1, u1, v0);
            }
        }
    }

    // ------------------------------------------------------------------ static curved spike (horns, claws, teeth)
    public static final class Spike extends Shape {
        final Joint j;
        final float[][] pts; // x,y,z,radius
        final int sides;

        public Spike(int mat, Joint j, int sides, float[]... pts) { this.mat = mat; this.j = j; this.sides = sides; this.pts = pts; }

        @Override
        void emit(Out o, Skeleton s) {
            int n = pts.length;
            Vector3f[] c = new Vector3f[n];
            for (int i = 0; i < n; i++) c[i] = new Vector3f(pts[i][0], pts[i][1], pts[i][2]);
            Vector3f[][] P = new Vector3f[n][sides + 1], N = new Vector3f[n][sides + 1];
            Vector3f up = new Vector3f(0, 1, 0);
            for (int i = 0; i < n; i++) {
                Vector3f t = new Vector3f(c[Math.min(n - 1, i + 1)]).sub(c[Math.max(0, i - 1)]).normalize();
                Vector3f ref = Math.abs(t.y) > 0.9f ? new Vector3f(1, 0, 0) : up;
                Vector3f a = new Vector3f(t).cross(ref).normalize();
                Vector3f b = new Vector3f(t).cross(a).normalize();
                float r = pts[i][3];
                for (int k = 0; k <= sides; k++) {
                    double th = Math.PI * 2 * k / sides;
                    float ca = (float) Math.cos(th), sb = (float) Math.sin(th);
                    Vector3f d = new Vector3f(a).mul(ca).add(new Vector3f(b).mul(sb));
                    P[i][k] = tp(j, c[i].x + d.x * r, c[i].y + d.y * r, c[i].z + d.z * r);
                    N[i][k] = tn(j, d.x, d.y, d.z);
                }
            }
            for (int i = 0; i < n - 1; i++) for (int k = 0; k < sides; k++) {
                float u0 = (float) k / sides, u1 = (float) (k + 1) / sides, v0 = (float) i / (n - 1), v1 = (float) (i + 1) / (n - 1);
                o.quad(P[i][k], P[i + 1][k], P[i + 1][k + 1], P[i][k + 1], N[i][k], N[i + 1][k], N[i + 1][k + 1], N[i][k + 1], u0, v0, u0, v1, u1, v1, u1, v0);
            }
        }
    }

    // ------------------------------------------------------------------ extruded plate (dorsal plates)
    public static final class Plate extends Shape {
        final Joint j;
        final float ox, oy, oz;
        final Matrix3f rot = new Matrix3f();
        final float[][] outline; // (z, y) in plate plane
        final float thick;
        final float cz, cy;
        final float scale;

        public Plate(int mat, Joint j, float ox, float oy, float oz, float rx, float ry, float rz, float scale, float thick, float[][] outline) {
            this.mat = mat; this.j = j; this.ox = ox; this.oy = oy; this.oz = oz; this.outline = outline; this.thick = thick; this.scale = scale;
            rot.identity().rotateY(ry).rotateX(rx).rotateZ(rz);
            float sz = 0, sy = 0;
            for (float[] p : outline) { sz += p[0]; sy += p[1]; }
            cz = sz / outline.length; cy = sy / outline.length * 0.6f;
        }

        Vector3f local(float x, float y, float z) {
            Vector3f v = new Vector3f(x * scale, y * scale, z * scale);
            rot.transform(v);
            return tp(j, ox + v.x, oy + v.y, oz + v.z);
        }

        @Override
        void emit(Out o, Skeleton s) {
            int n = outline.length;
            float h = thick * 0.5f / scale;
            Vector3f nx = new Vector3f(1, 0, 0); rot.transform(nx);
            Vector3f nL = tn(j, nx.x, nx.y, nx.z), nR = new Vector3f(nL).negate();
            Vector3f cL = local(h * 0.8f, cy, cz), cR = local(-h * 0.8f, cy, cz);
            float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE, maxY = 0;
            for (float[] p : outline) { minZ = Math.min(minZ, p[0]); maxZ = Math.max(maxZ, p[0]); maxY = Math.max(maxY, p[1]); }
            float w = maxZ - minZ, hgt = Math.max(maxY, 0.01f);
            float cu = (cz - minZ) / w, cv = 1 - cy / hgt;
            for (int i = 0; i < n; i++) {
                float[] a = outline[i], b = outline[(i + 1) % n];
                Vector3f aL = local(h * edgeTaper(a), a[1], a[0]), bL = local(h * edgeTaper(b), b[1], b[0]);
                Vector3f aR = local(-h * edgeTaper(a), a[1], a[0]), bR = local(-h * edgeTaper(b), b[1], b[0]);
                float ua = (a[0] - minZ) / w, va = 1 - a[1] / hgt, ub = (b[0] - minZ) / w, vb = 1 - b[1] / hgt;
                o.quad(cL, aL, bL, bL, nL, nL, nL, nL, cu, cv, ua, va, ub, vb, ub, vb);
                o.quad(cR, bR, aR, aR, nR, nR, nR, nR, cu, cv, ub, vb, ua, va, ua, va);
                Vector3f en = new Vector3f(b[1] - a[1], -(b[0] - a[0]), 0);
                Vector3f e = new Vector3f(0, en.x, en.y);
                rot.transform(e);
                Vector3f edgeN = tn(j, e.x, e.y, e.z).negate();
                o.quad(aL, aR, bR, bL, edgeN, edgeN, edgeN, edgeN, ua, va, ua, va + 0.02f, ub, vb + 0.02f, ub, vb);
            }
        }

        private float edgeTaper(float[] p) { return 0.35f + 0.65f * Math.max(0, 1 - p[1] / 3f); }
    }

    // ------------------------------------------------------------------ membrane
    public static final class Membrane extends Shape {
        final List<Object[]> verts = new ArrayList<>(); // Joint, Vector3f local, u, v
        final List<int[]> tris = new ArrayList<>();

        public Membrane(int mat) { this.mat = mat; }

        public int vert(Joint j, float x, float y, float z, float u, float v) { verts.add(new Object[]{j, new Vector3f(x, y, z), u, v}); return verts.size() - 1; }

        public Membrane tri(int a, int b, int c) { tris.add(new int[]{a, b, c}); return this; }

        @Override
        void emit(Out o, Skeleton s) {
            Vector3f[] P = new Vector3f[verts.size()];
            for (int i = 0; i < P.length; i++) {
                Object[] v = verts.get(i);
                Vector3f l = (Vector3f) v[1];
                P[i] = tp((Joint) v[0], l.x, l.y, l.z);
            }
            for (int[] t : tris) {
                Vector3f a = P[t[0]], b = P[t[1]], c = P[t[2]];
                Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
                if (n.lengthSquared() < 1e-8f) continue;
                n.normalize();
                Object[] va = verts.get(t[0]), vb = verts.get(t[1]), vc = verts.get(t[2]);
                o.quad(a, b, c, c, n, n, n, n, (float) va[2], (float) va[3], (float) vb[2], (float) vb[3], (float) vc[2], (float) vc[3], (float) vc[2], (float) vc[3]);
            }
        }
    }

    // ------------------------------------------------------------------ rendering
    public void render(Skeleton s, int mat, Out o, IntPredicate visible) {
        for (Shape sh : shapes) if (sh.mat == mat && (sh.vis < 0 || visible.test(sh.vis))) sh.emit(o, s);
    }

    public void renderGlow(Skeleton s, int glowTex, int group, Out o, IntPredicate visible) {
        for (Shape sh : shapes) if (sh.glowTex == glowTex && sh.glowGroup == group && (sh.vis < 0 || visible.test(sh.vis))) sh.emit(o, s);
    }
}
