package com.montella.kaiju.client;

import com.montella.kaiju.client.Mesh.*;
import com.montella.kaiju.entity.GodzillaEntity;
import com.montella.kaiju.rig.Skeleton;
import com.montella.kaiju.rig.Skeleton.Joint;
import net.minecraft.util.Mth;

public final class GodzillaMesh {
    public static final int SKIN = 0, BELLY = 1, PLATE = 2, TEETH = 3, MOUTH = 4, EYE = 5, CLAW = 6;
    public static final int G_PLATE = 0, G_EYE = 1;
    /** Plate glow groups: 0..NECK_G-1 neck, then back, then tail. */
    public static final int PLATE_GROUPS = 24;

    /** Maple-leaf dorsal plate outline (z along body, y up). */
    static final float[][] LEAF = {
            {-1.0f, 0f}, {-0.8f, 0.5f}, {-1.25f, 0.95f}, {-0.7f, 1.05f}, {-0.85f, 1.7f}, {-0.35f, 1.7f}, {-0.55f, 2.65f},
            {-0.05f, 1.95f}, {0.3f, 2.25f}, {0.35f, 1.45f}, {0.8f, 1.55f}, {0.6f, 0.9f}, {1.0f, 0.8f}, {0.75f, 0.35f}, {1.0f, 0f}};
    static final float[][] LEAF_SMALL = {
            {-1.0f, 0f}, {-0.9f, 0.6f}, {-1.2f, 1.0f}, {-0.5f, 1.15f}, {-0.6f, 2.0f}, {0.05f, 1.4f}, {0.5f, 1.65f}, {0.5f, 0.9f}, {0.95f, 0.65f}, {1.0f, 0f}};

    private GodzillaMesh() {}

    static Blob blob(Mesh m, int mat, Joint j, float x, float y, float z, float rx, float ryU, float ryD, float rzF, float rzB) {
        return m.add(new Blob(mat, j, x, y, z, rx, ryU, ryD, rzF, rzB));
    }

    static void bellyBlob(Mesh m, Joint j, float x, float y, float z, float rx, float ryU, float ryD, float rzF, float rzB, float dot) {
        Blob b = blob(m, SKIN, j, x, y, z, rx, ryU, ryD, rzF, rzB).belly(BELLY, dot);
        m.add(b.bellyPart());
    }

    static void plate(Mesh m, Joint j, float x, float y, float z, float rx, float ry, float rz, float s, int group, boolean small) {
        m.add(new Plate(PLATE, j, x, y, z, rx, ry, rz, s, 0.32f * s, small ? LEAF_SMALL : LEAF)).glow(G_PLATE, group);
    }

    public static Mesh build(Skeleton s) {
        Mesh m = new Mesh();
        Joint pelvis = s.get("pelvis"), spine = s.get("spine"), chest = s.get("chest"), n0 = s.get("neck0"), n1 = s.get("neck1"),
                n2 = s.get("neck2"), head = s.get("head"), jaw = s.get("jaw");

        // ---- torso + neck: one continuous skinned body with a ridged belly down the front
        Joint ta = s.get("torsoA"), tb = s.get("torsoB"), tc = s.get("torsoC");
        Tube body = m.add(new Tube(SKIN, 18, 0, 1f / Mesh.TILE).belly(BELLY, 0.85f))
                .ring(ta, 0, 0.6f, -3.4f, 2.6f, 2.4f).ring(ta, 0, 0.2f, -2.4f, 4.6f, 4.0f).ring(ta, 0, 0f, -0.8f, 5.3f, 4.7f)
                .ring(ta, 0, -0.2f, 1.0f, 5.4f, 4.9f).ring(tb, 0, -0.5f, 0.8f, 5.3f, 5.0f).ring(tb, 0, -0.5f, 2.6f, 5.1f, 4.8f)
                .ring(tc, 0, -0.4f, 0.6f, 4.95f, 4.5f).ring(tc, 0, -0.2f, 2.4f, 4.75f, 4.1f).ring(tc, 0, 0f, 3.8f, 4.45f, 3.8f)
                .ring(n0, 0, 0, -1.2f, 4.0f, 3.7f).ring(n0, 0, 0, 0.3f, 3.55f, 3.3f).ring(n1, 3.1f, 2.95f)
                .ring(n2, 2.6f, 2.45f).ring(head, 0, 0.35f, -0.5f, 1.95f, 1.75f);
        m.add(body.bellyPart());
        // muscle masses over the skin tube
        blob(m, SKIN, chest, 2.3f, 2.8f, 3.0f, 2.1f, 1.5f, 1.6f, 1.4f, 1.6f);
        blob(m, SKIN, chest, -2.3f, 2.8f, 3.0f, 2.1f, 1.5f, 1.6f, 1.4f, 1.6f);
        blob(m, SKIN, chest, 3.4f, 3.6f, 0.4f, 1.6f, 1.8f, 2.0f, 2.2f, 2.0f);
        blob(m, SKIN, chest, -3.4f, 3.6f, 0.4f, 1.6f, 1.8f, 2.0f, 2.2f, 2.0f);
        blob(m, SKIN, pelvis, 3.3f, -0.2f, -0.4f, 2.4f, 2.6f, 2.2f, 3.0f, 3.0f);
        blob(m, SKIN, pelvis, -3.3f, -0.2f, -0.4f, 2.4f, 2.6f, 2.2f, 3.0f, 3.0f);

        // ---- head: compact skull, heavy brow, broad snout
        blob(m, SKIN, head, 0, 0.45f, 0.9f, 1.75f, 1.45f, 1.0f, 2.0f, 1.8f);
        blob(m, SKIN, head, 0, 0.15f, 2.8f, 1.3f, 0.95f, 0.55f, 1.6f, 1.2f);
        blob(m, SKIN, head, 0, 0.3f, 4.05f, 0.9f, 0.55f, 0.4f, 0.55f, 0.6f);
        blob(m, SKIN, head, 0.95f, 1.25f, 1.9f, 0.55f, 0.42f, 0.35f, 1.15f, 0.9f);
        blob(m, SKIN, head, -0.95f, 1.25f, 1.9f, 0.55f, 0.42f, 0.35f, 1.15f, 0.9f);
        blob(m, SKIN, head, 1.3f, 0.1f, 1.0f, 0.62f, 0.9f, 0.8f, 1.25f, 1.0f);
        blob(m, SKIN, head, -1.3f, 0.1f, 1.0f, 0.62f, 0.9f, 0.8f, 1.25f, 1.0f);
        blob(m, MOUTH, head, 0, -0.28f, 2.4f, 1.05f, 0.22f, 0.25f, 2.0f, 1.2f);
        blob(m, SKIN, jaw, 0, -0.35f, 1.6f, 1.2f, 0.35f, 0.6f, 2.35f, 0.95f);
        blob(m, MOUTH, jaw, 0, -0.08f, 1.7f, 1.0f, 0.24f, 0.22f, 2.1f, 0.7f);
        blob(m, SKIN, jaw, 0, -0.75f, 0.6f, 1.25f, 0.5f, 0.5f, 1.2f, 1.0f);
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < 9; k++) {
                float z = 1.0f + k * 0.38f, x = side * (1.08f - k * 0.055f), len = 0.42f + (k > 1 && k < 5 ? 0.18f : 0f);
                m.add(new Spike(TEETH, head, 4, new float[]{x, -0.28f, z, 0.12f}, new float[]{x * 0.97f, -0.28f - len, z + 0.06f, 0.01f}));
                m.add(new Spike(TEETH, jaw, 4, new float[]{x * 0.93f, -0.05f, z - 0.15f, 0.11f}, new float[]{x * 0.9f, 0.3f + len * 0.3f, z - 0.1f, 0.01f}));
            }
            m.add(new Blob(EYE, head, side * 1.18f, 0.88f, 2.05f, 0.19f, 0.17f, 0.17f, 0.22f, 0.15f)).glow(G_EYE, 0);
            // gill-like ridges behind the cheeks
            for (int k = 0; k < 3; k++)
                m.add(new Spike(SKIN, head, 4, new float[]{side * 1.45f, 0.5f - k * 0.45f, -0.2f - k * 0.1f, 0.22f}, new float[]{side * 1.75f, 0.45f - k * 0.45f, -0.9f, 0.02f}));
        }

        // ---- arms
        for (String sd : new String[]{"L", "R"}) {
            Joint a0 = s.get("arm" + sd + "0"), a1 = s.get("arm" + sd + "1"), hd = s.get("hand" + sd);
            blob(m, SKIN, a0, 0, 0, 0.3f, 1.5f, 1.5f, 1.5f, 1.4f, 1.2f);
            m.add(new Tube(SKIN, 10, 0, 1f / Mesh.TILE)).ring(a0, 1.25f, 1.15f).ring(a0, 0, 0, 1.5f, 1.15f, 1.05f).ring(a1, 0.95f, 0.9f)
                    .ring(a1, 0, 0, 1.3f, 0.8f, 0.75f).ring(hd, 0.68f, 0.55f);
            blob(m, SKIN, a1, 0, 0, 0, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f);
            blob(m, SKIN, hd, 0, 0, 0.6f, 0.75f, 0.42f, 0.42f, 0.85f, 0.4f);
            for (int k = 0; k < 4; k++) {
                float x = -0.48f + k * 0.32f;
                m.add(new Spike(SKIN, hd, 5, new float[]{x, 0, 1.0f, 0.22f}, new float[]{x * 1.1f, -0.15f, 1.6f, 0.17f}));
                m.add(new Spike(CLAW, hd, 4, new float[]{x * 1.1f, -0.15f, 1.6f, 0.16f}, new float[]{x * 1.15f, -0.45f, 2.0f, 0.08f}, new float[]{x * 1.15f, -0.85f, 2.15f, 0.01f}));
            }
        }

        // ---- legs: massive thighs, columnar shins, broad feet
        for (String sd : new String[]{"L", "R"}) {
            Joint th = s.get("thigh" + sd), sh = s.get("shin" + sd), ft = s.get("foot" + sd);
            float side = sd.equals("L") ? 1 : -1;
            m.add(new Tube(SKIN, 14, 0, 1f / Mesh.TILE)).ring(th, 0, 0, -1.2f, 2.9f, 3.0f).ring(th, 0, 0.1f, 1.0f, 3.0f, 3.3f)
                    .ring(th, 0, 0.2f, 3.0f, 2.6f, 2.9f).ring(sh, 0, 0, -0.4f, 2.2f, 2.4f);
            blob(m, SKIN, th, side * 0.4f, 0.6f, 1.6f, 2.4f, 2.6f, 2.4f, 2.8f, 2.6f);
            blob(m, SKIN, sh, 0, 0, 0.1f, 2.2f, 2.2f, 2.2f, 2.0f, 1.6f);
            m.add(new Tube(SKIN, 12, 0, 1f / Mesh.TILE)).ring(sh, 0, -0.2f, 0.3f, 2.25f, 2.4f).ring(sh, 0, -0.1f, 2.0f, 1.95f, 2.15f).ring(sh, 0, 0, 3.6f, 1.7f, 1.8f).ring(ft, 0, 0.3f, -0.1f, 1.6f, 1.25f);
            blob(m, SKIN, sh, 0, -0.9f, 1.6f, 1.6f, 1.2f, 1.4f, 1.8f, 1.6f);
            blob(m, SKIN, ft, 0, -0.35f, 0.9f, 1.75f, 0.8f, 0.7f, 2.4f, 1.3f);
            blob(m, SKIN, ft, 0, 0.3f, -0.1f, 1.55f, 1.0f, 0.8f, 1.4f, 1.3f);
            for (int k = -1; k <= 1; k++) {
                float x = k * 1.05f;
                m.add(new Spike(SKIN, ft, 6, new float[]{x, -0.45f, 2.3f, 0.5f}, new float[]{x * 1.1f, -0.6f, 3.0f, 0.36f}));
                m.add(new Spike(CLAW, ft, 4, new float[]{x * 1.1f, -0.6f, 3.0f, 0.33f}, new float[]{x * 1.12f, -0.75f, 3.55f, 0.18f}, new float[]{x * 1.12f, -1.05f, 3.85f, 0.01f}));
            }
            m.add(new Spike(CLAW, ft, 4, new float[]{side * 0.9f, -0.5f, -0.9f, 0.25f}, new float[]{side * 1.1f, -0.95f, -1.5f, 0.01f}));
        }

        // ---- tail: long, heavy, tapering, dragging behind
        Tube tail = m.add(new Tube(SKIN, 12, 0, 1f / Mesh.TILE).belly(BELLY, 0.85f)).ring(s.get("tail0"), 0, 0, -1.6f, 3.9f, 3.6f);
        for (int i = 0; i <= GodzillaEntity.TAIL; i++) {
            float r = GodzillaEntity.TAIL_R[i];
            tail.ring(s.get("tail" + i), r, r * 0.93f);
        }
        tail.tip(s.get("tail" + GodzillaEntity.TAIL), 0, 0, 1.6f);
        m.add(tail.bellyPart());

        // ---- dorsal plates: three rows, largest over the shoulders, glowing in a wave when charging
        int g = 0;
        plate(m, n2, 0, 2.2f, -0.3f, -0.15f, 0, 0, 0.7f, g, true);
        plate(m, n1, 0, 2.7f, -0.2f, -0.15f, 0, 0, 0.95f, g + 1, true);
        plate(m, n0, 0, 3.3f, 0.1f, -0.1f, 0, 0, 1.25f, g + 2, false);
        plate(m, n0, 1.3f, 2.8f, 0.5f, -0.1f, 0, -0.5f, 0.75f, g + 2, true);
        plate(m, n0, -1.3f, 2.8f, 0.5f, -0.1f, 0, 0.5f, 0.75f, g + 2, true);
        g = 3;
        float[][] back = { // joint, y, z, rx, scale
                {2, 4.6f, -2.6f, -0.65f, 1.75f}, {2, 3.0f, -3.4f, -0.95f, 2.05f}, {2, 1.2f, -3.9f, -1.15f, 2.1f},
                {1, 1.6f, -4.2f, -1.3f, 1.95f}, {0, 2.6f, -4.6f, -1.45f, 1.7f}, {0, 0.6f, -5.0f, -1.6f, 1.5f}};
        Joint[] bj = {pelvis, spine, chest};
        for (float[] b : back) {
            Joint j = bj[(int) b[0]];
            plate(m, j, 0, b[1], b[2], b[3], 0, 0, b[4], g, false);
            plate(m, j, 1.55f, b[1] - 0.4f, b[2] + 0.35f, b[3], 0, -0.42f, b[4] * 0.62f, g, true);
            plate(m, j, -1.55f, b[1] - 0.4f, b[2] + 0.35f, b[3], 0, 0.42f, b[4] * 0.62f, g, true);
            g++;
        }
        for (int i = 0; i <= GodzillaEntity.TAIL - 1; i++) {
            Joint j = s.get("tail" + i);
            float r = GodzillaEntity.TAIL_R[i];
            float sc = Mth.lerp(i / (float) GodzillaEntity.TAIL, 1.45f, 0.35f);
            plate(m, j, 0, r * 0.82f, 0.4f, -0.3f, Mth.PI, 0, sc, g, i > 6);
            plate(m, j, r * 0.55f, r * 0.62f, 0.9f, -0.3f, Mth.PI, 0.45f, sc * 0.6f, g, true);
            plate(m, j, -r * 0.55f, r * 0.62f, 0.9f, -0.3f, Mth.PI, -0.45f, sc * 0.6f, g, true);
            g++;
        }
        return m;
    }
}
