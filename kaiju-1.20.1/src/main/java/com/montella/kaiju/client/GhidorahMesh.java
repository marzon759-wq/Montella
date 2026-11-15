package com.montella.kaiju.client;

import com.montella.kaiju.client.Mesh.*;
import com.montella.kaiju.entity.GhidorahEntity;
import com.montella.kaiju.rig.Skeleton;
import com.montella.kaiju.rig.Skeleton.Joint;
import org.joml.Vector3f;

public final class GhidorahMesh {
    public static final int SKIN = 0, BELLY = 1, WING = 2, HORN = 3, TEETH = 4, MOUTH = 5, EYE = 6;
    public static final int G_CRACKLE = 0, G_EYE = 1, G_WOUND = 2;
    /** Visibility groups: 10+h = stump of head h shown while it regrows. */
    public static final int VIS_STUMP = 10;

    private GhidorahMesh() {}

    static Blob blob(Mesh m, int mat, Joint j, float x, float y, float z, float rx, float ryU, float ryD, float rzF, float rzB) {
        Blob b = m.add(new Blob(mat, j, x, y, z, rx, ryU, ryD, rzF, rzB));
        if (mat == SKIN || mat == BELLY) b.glow(G_CRACKLE, 0);
        return b;
    }

    static void bellyBlob(Mesh m, Joint j, float x, float y, float z, float rx, float ryU, float ryD, float rzF, float rzB, float dot) {
        Blob b = blob(m, SKIN, j, x, y, z, rx, ryU, ryD, rzF, rzB).belly(BELLY, dot);
        m.add(b.bellyPart()).glow(G_CRACKLE, 0);
    }

    /** Head and jaw shapes; style 0 = middle head (long straight horns), 1/2 = side heads (curved horns). */
    public static void head(Mesh m, Joint head, Joint jaw, int style) {
        blob(m, SKIN, head, 0, 0.35f, 0.55f, 1.15f, 1.0f, 0.7f, 1.6f, 1.35f);
        blob(m, SKIN, head, 0, 0.08f, 2.6f, 0.82f, 0.66f, 0.42f, 2.0f, 1.2f);
        blob(m, SKIN, head, 0, 0.2f, 4.3f, 0.62f, 0.45f, 0.32f, 0.55f, 0.6f);
        for (int sd = -1; sd <= 1; sd += 2) {
            blob(m, SKIN, head, sd * 0.68f, 0.95f, 1.45f, 0.42f, 0.36f, 0.25f, 1.25f, 0.8f);
            blob(m, SKIN, head, sd * 0.9f, 0.05f, 0.5f, 0.45f, 0.7f, 0.6f, 1.0f, 0.9f);
            m.add(new Blob(EYE, head, sd * 0.8f, 0.78f, 1.75f, 0.15f, 0.13f, 0.13f, 0.2f, 0.12f)).glow(G_EYE, 0);
            // horns
            if (style == 0) {
                m.add(new Spike(HORN, head, 6, new float[]{sd * 0.45f, 1.0f, -0.2f, 0.3f}, new float[]{sd * 0.6f, 1.45f, -1.5f, 0.22f},
                        new float[]{sd * 0.7f, 1.8f, -2.9f, 0.12f}, new float[]{sd * 0.74f, 2.0f, -4.1f, 0.01f}));
                m.add(new Spike(HORN, head, 5, new float[]{sd * 0.8f, 0.7f, -0.3f, 0.18f}, new float[]{sd * 1.2f, 0.95f, -1.4f, 0.1f}, new float[]{sd * 1.35f, 1.05f, -2.2f, 0.01f}));
            } else {
                m.add(new Spike(HORN, head, 6, new float[]{sd * 0.45f, 1.0f, -0.2f, 0.28f}, new float[]{sd * 0.85f, 1.3f, -1.3f, 0.2f},
                        new float[]{sd * 1.1f, 1.1f, -2.3f, 0.11f}, new float[]{sd * 1.05f, 0.5f, -3.0f, 0.01f}));
                m.add(new Spike(HORN, head, 5, new float[]{sd * 0.85f, 0.6f, -0.3f, 0.16f}, new float[]{sd * 1.35f, 0.5f, -1.2f, 0.09f}, new float[]{sd * 1.5f, 0.1f, -1.8f, 0.01f}));
            }
            // cheek / jaw frills
            for (int k = 0; k < 4; k++)
                m.add(new Spike(HORN, head, 4, new float[]{sd * 0.95f, 0.4f - k * 0.3f, -0.1f + k * 0.15f, 0.12f},
                        new float[]{sd * (1.55f + k * 0.1f), 0.3f - k * 0.45f, -0.9f + k * 0.15f, 0.01f}));
            for (int k = 0; k < 11; k++) {
                float z = 1.2f + k * 0.3f, x = sd * (0.72f - k * 0.035f), len = 0.32f + (k % 3 == 1 ? 0.18f : 0);
                m.add(new Spike(TEETH, head, 4, new float[]{x, -0.25f, z, 0.08f}, new float[]{x, -0.25f - len, z + 0.04f, 0.005f}));
                m.add(new Spike(TEETH, jaw, 4, new float[]{x * 0.95f, -0.05f, z - 0.1f, 0.08f}, new float[]{x * 0.93f, 0.25f + len * 0.3f, z - 0.06f, 0.005f}));
            }
        }
        // crest ridge on the skull
        for (int k = 0; k < 4; k++)
            m.add(new Spike(HORN, head, 4, new float[]{0, 1.2f - k * 0.05f, 1.2f - k * 0.55f, 0.13f}, new float[]{0, 1.65f - k * 0.08f, 0.8f - k * 0.6f, 0.01f}));
        blob(m, MOUTH, head, 0, -0.25f, 2.4f, 0.7f, 0.16f, 0.2f, 2.2f, 1.1f);
        blob(m, SKIN, jaw, 0, -0.25f, 1.9f, 0.78f, 0.26f, 0.42f, 2.6f, 0.85f);
        blob(m, MOUTH, jaw, 0, -0.05f, 2.0f, 0.62f, 0.18f, 0.15f, 2.4f, 0.6f);
        blob(m, SKIN, jaw, 0, -0.55f, 0.5f, 0.8f, 0.4f, 0.4f, 1.1f, 0.8f);
        for (int k = 0; k < 3; k++)
            m.add(new Spike(HORN, jaw, 4, new float[]{0, -0.6f, 1.2f - k * 0.5f, 0.1f}, new float[]{0, -1.05f, 0.9f - k * 0.5f, 0.01f}));
    }

    public static Mesh build(Skeleton s) {
        Mesh m = new Mesh();
        Joint pelvis = s.get("pelvis"), spine = s.get("spine"), chest = s.get("chest");
        s.resetPose();
        s.compute();

        // ---- body: one upright skinned trunk with a ridged belly
        Joint ta = s.get("torsoA"), tb = s.get("torsoB"), tc = s.get("torsoC");
        Tube body = m.add(new Tube(SKIN, 16, 0, 1f / Mesh.TILE).belly(BELLY, 0.85f))
                .ring(ta, 0, 0.5f, -3.0f, 2.0f, 1.9f).ring(ta, 0, 0.2f, -2.0f, 3.6f, 3.2f).ring(ta, 0, 0, -0.4f, 4.1f, 3.7f)
                .ring(ta, 0, -0.2f, 1.2f, 4.0f, 3.7f).ring(tb, 0, -0.4f, 1.0f, 3.9f, 3.7f).ring(tb, 0, -0.4f, 2.6f, 3.9f, 3.5f)
                .ring(tc, 0, -0.3f, 0.8f, 4.1f, 3.4f).ring(tc, 0, -0.1f, 2.5f, 4.3f, 3.2f).ring(tc, 0, 0.2f, 3.8f, 4.0f, 2.8f)
                .ring(tc, 0, 0.3f, 4.8f, 2.6f, 2.0f);
        body.glow(G_CRACKLE, 0);
        m.add(body.bellyPart()).glow(G_CRACKLE, 0);
        blob(m, SKIN, chest, 2.7f, 3.0f, -0.9f, 1.8f, 1.6f, 1.5f, 1.8f, 1.8f);
        blob(m, SKIN, chest, -2.7f, 3.0f, -0.9f, 1.8f, 1.6f, 1.5f, 1.8f, 1.8f);
        blob(m, SKIN, pelvis, 2.6f, -0.2f, -0.2f, 1.9f, 2.0f, 1.8f, 2.4f, 2.4f);
        blob(m, SKIN, pelvis, -2.6f, -0.2f, -0.2f, 1.9f, 2.0f, 1.8f, 2.4f, 2.4f);
        // back spines
        for (int k = 0; k < 6; k++) {
            Joint j = k < 3 ? chest : spine;
            float y = k < 3 ? 4.2f - k * 1.3f : 3.0f - (k - 3) * 1.3f;
            float z = k < 3 ? -2.4f - k * 0.3f : -3.0f;
            m.add(new Spike(HORN, j, 5, new float[]{0, y, z, 0.32f}, new float[]{0, y + 0.5f, z - 1.3f, 0.01f}));
        }

        // ---- three necks and heads
        for (int h = 0; h < 3; h++) {
            Joint[] n = new Joint[GhidorahEntity.NECK + 1];
            for (int i = 0; i <= GhidorahEntity.NECK; i++) n[i] = s.get("n" + h + "_" + i);
            Joint hd = s.get("h" + h), jw = s.get("j" + h);
            Tube neck = m.add(new Tube(SKIN, 12, 0, 1f / Mesh.TILE).belly(BELLY, 0.95f));
            neck.glow(G_CRACKLE, 0);
            neck.ring(n[0], 0, 0, -1.6f, 2.1f, 2.0f);
            for (int i = 0; i <= GhidorahEntity.NECK; i++) neck.ring(n[i], GhidorahEntity.NECK_R[i], GhidorahEntity.NECK_R[i] * 0.95f);
            neck.ring(hd, 0, 0.3f, -0.3f, 0.95f, 0.9f);
            m.add(neck.bellyPart()).glow(G_CRACKLE, 0);
            for (int i = 2; i <= GhidorahEntity.NECK; i++) {
                float r = GhidorahEntity.NECK_R[i];
                float len = i > 6 ? 0.7f : 0.45f;
                m.add(new Spike(HORN, n[i], 4, new float[]{0, r * 0.85f, 0, 0.2f}, new float[]{0, r + len, -0.5f, 0.01f}));
            }
            head(m, hd, jw, h);
            // wound / regrowth stump
            m.add(new Blob(MOUTH, n[6], 0, 0, 1.0f, 1.0f, 0.95f, 0.95f, 0.3f, 0.25f)).glow(G_WOUND, 0).vis(VIS_STUMP + h);
        }

        // ---- wings: bones and membranes stretched between them
        for (int w = 0; w < 2; w++) {
            String n = w == 0 ? "L" : "R";
            float sd = w == 0 ? 1 : -1;
            Joint w0 = s.get("w" + n + "0"), w1 = s.get("w" + n + "1"), w2 = s.get("w" + n + "2");
            blob(m, SKIN, w0, 0, 0, 0.6f, 1.4f, 1.3f, 1.3f, 1.6f, 1.0f);
            m.add(new Tube(SKIN, 8, 0, 1f / Mesh.TILE)).ring(w0, 0.95f, 0.9f).ring(w0, 0, 0, 4.6f, 0.62f, 0.6f).ring(w1, 0.6f, 0.58f)
                    .ring(w1, 0, 0, 5.6f, 0.45f, 0.42f).ring(w2, 0.5f, 0.45f).glow(G_CRACKLE, 0);
            blob(m, SKIN, w1, 0, 0, 0, 0.75f, 0.7f, 0.7f, 0.7f, 0.7f);
            blob(m, SKIN, w2, 0, 0, 0, 0.6f, 0.55f, 0.55f, 0.6f, 0.6f);
            m.add(new Spike(HORN, w2, 5, new float[]{0, 0, 0.2f, 0.35f}, new float[]{0, 0.2f, 1.8f, 0.01f}));
            Membrane mem = m.add(new Membrane(WING));
            Joint pel = pelvis;
            int S = vtx(mem, w0, 0, 0, 0), E = vtx(mem, w1, 0, 0, 0), W = vtx(mem, w2, 0, 0, 0);
            int B = vtx(mem, pel, sd * 2.2f, 0.4f, -2.6f);
            int B2 = vtx(mem, spine, sd * 3.0f, 2.0f, -1.6f);
            int[] M = new int[4], T = new int[4];
            for (int k = 0; k < 4; k++) {
                Joint f = s.get("f" + n + k), fb = s.get("f" + n + k + "b");
                float len = GhidorahEntity.FINGER_LEN[k];
                m.add(new Tube(SKIN, 6, 0, 1f / Mesh.TILE)).ring(f, 0.32f, 0.3f).ring(fb, 0.24f, 0.22f).tip(fb, 0, 0, len * 0.5f);
                M[k] = vtx(mem, fb, 0, 0, 0);
                T[k] = vtx(mem, fb, 0, 0, len * 0.5f);
            }
            for (int k = 0; k < 3; k++) {
                int mid = vtx(mem, s.get("f" + n + k + "b"), 0, 0, GhidorahEntity.FINGER_LEN[k] * 0.12f);
                // scalloped trailing edge: pull the edge between finger tips towards the wrist
                mem.tri(W, M[k], M[k + 1]).tri(M[k], T[k], M[k + 1]).tri(T[k], T[k + 1], M[k + 1]);
                if (mid < 0) break;
            }
            mem.tri(W, M[3], E).tri(E, M[3], B2).tri(M[3], T[3], B).tri(M[3], B, B2).tri(S, E, B2);
            uv(mem, sd);
        }

        // ---- legs
        for (String sd : new String[]{"L", "R"}) {
            Joint th = s.get("thigh" + sd), sh = s.get("shin" + sd), ft = s.get("foot" + sd);
            float side = sd.equals("L") ? 1 : -1;
            m.add(new Tube(SKIN, 12, 0, 1f / Mesh.TILE)).ring(th, 0, 0, -1.0f, 2.3f, 2.4f).ring(th, 0, 0.1f, 1.0f, 2.4f, 2.6f)
                    .ring(th, 0, 0.2f, 3.0f, 2.0f, 2.2f).ring(sh, 0, 0, -0.3f, 1.7f, 1.8f).glow(G_CRACKLE, 0);
            blob(m, SKIN, sh, 0, 0, 0, 1.7f, 1.7f, 1.7f, 1.6f, 1.4f);
            m.add(new Tube(SKIN, 10, 0, 1f / Mesh.TILE)).ring(sh, 0, -0.1f, 0.3f, 1.6f, 1.7f).ring(sh, 0, 0, 2.2f, 1.3f, 1.4f).ring(sh, 0, 0, 3.7f, 1.1f, 1.15f)
                    .ring(ft, 0, 0.3f, -0.1f, 1.1f, 0.9f).glow(G_CRACKLE, 0);
            blob(m, SKIN, ft, 0, -0.1f, 0.6f, 1.25f, 0.6f, 0.5f, 1.6f, 1.0f);
            for (int k = -1; k <= 1; k++) {
                float x = k * 0.85f;
                m.add(new Spike(SKIN, ft, 6, new float[]{x, -0.15f, 1.6f, 0.38f}, new float[]{x * 1.3f, -0.3f, 2.6f, 0.28f}));
                m.add(new Spike(HORN, ft, 4, new float[]{x * 1.3f, -0.3f, 2.6f, 0.26f}, new float[]{x * 1.35f, -0.4f, 3.2f, 0.13f}, new float[]{x * 1.35f, -0.65f, 3.5f, 0.01f}));
            }
            m.add(new Spike(HORN, ft, 4, new float[]{0, -0.2f, -0.6f, 0.22f}, new float[]{0, -0.6f, -1.4f, 0.01f}));
        }

        // ---- twin tails with rattles
        for (int k = 0; k < 2; k++) {
            Tube tail = m.add(new Tube(SKIN, 10, 0, 1f / Mesh.TILE).belly(BELLY, 0.85f));
            tail.glow(G_CRACKLE, 0);
            tail.ring(s.get("t" + k + "_0"), 0, 0, -1.2f, 2.3f, 2.2f);
            for (int i = 0; i <= GhidorahEntity.TAIL; i++) {
                float r = GhidorahEntity.TAIL_R[i];
                tail.ring(s.get("t" + k + "_" + i), r, r * 0.92f);
            }
            Joint end = s.get("t" + k + "_" + GhidorahEntity.TAIL);
            tail.tip(end, 0, 0, 0.8f);
            m.add(tail.bellyPart()).glow(G_CRACKLE, 0);
            for (int i = 1; i < GhidorahEntity.TAIL; i += 1) {
                float r = GhidorahEntity.TAIL_R[i];
                m.add(new Spike(HORN, s.get("t" + k + "_" + i), 4, new float[]{0, r * 0.8f, 0, 0.18f}, new float[]{0, r + 0.5f, 0.5f, 0.01f}));
            }
            for (int a = 0; a < 7; a++) {
                double ang = a / 7.0 * Math.PI * 2;
                float cx = (float) Math.cos(ang), cy = (float) Math.sin(ang);
                m.add(new Spike(HORN, end, 4, new float[]{cx * 0.3f, cy * 0.3f, 0.2f, 0.18f}, new float[]{cx * 1.2f, cy * 1.2f, 1.4f, 0.01f}));
                m.add(new Spike(HORN, s.get("t" + k + "_" + (GhidorahEntity.TAIL - 1)), 4, new float[]{cx * 0.35f, cy * 0.35f, 0.6f, 0.16f}, new float[]{cx * 1.1f, cy * 1.1f, 0.2f, 0.01f}));
            }
        }
        return m;
    }

    private static int vtx(Membrane mem, Joint j, float x, float y, float z) {
        Vector3f p = j.point(x, y, z, new Vector3f());
        return mem.vert(j, x, y, z, p.x, p.z); // uv fixed up below from rest-pose positions
    }

    /** Planar UVs from the spread rest pose: u along the span, v front to back. */
    private static void uv(Membrane mem, float side) {
        for (Object[] v : mem.verts) {
            float x = (float) v[2], z = (float) v[3];
            v[2] = Math.abs(x) / 26f * 2f;
            v[3] = (2f - z) / 20f * 2f;
        }
    }

    /** Standalone head mesh for a torn-off head. */
    public static Mesh severed(Skeleton s) {
        Mesh m = new Mesh();
        Joint n = s.get("n"), h = s.get("h"), j = s.get("j");
        Tube neck = m.add(new Tube(SKIN, 10, 0, 1f / Mesh.TILE));
        neck.ring(n, 0, 0, -2.0f, 1.0f, 0.95f).ring(n, 0, 0, 0, 0.98f, 0.93f).ring(h, 0, 0.3f, -0.3f, 0.95f, 0.9f);
        m.add(new Blob(MOUTH, n, 0, 0, -2.0f, 0.98f, 0.93f, 0.93f, 0.2f, 0.25f)).glow(G_WOUND, 0);
        head(m, h, j, 1);
        return m;
    }
}
