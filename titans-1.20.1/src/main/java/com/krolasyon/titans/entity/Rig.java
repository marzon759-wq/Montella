package com.krolasyon.titans.entity;

import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static com.krolasyon.titans.entity.TitanEntity.*;

/**
 * Procedural skeleton shared by server (hit detection) and client (mesh). Everything is in model space:
 * +z forward, +y up, units are blocks, origin at the entity position.
 */
public final class Rig {
    public static final float DEG = (float) (Math.PI / 180.0), PI = (float) Math.PI, LAG = 0.3f;
    public static final int NT = 6, NTL = 16;

    public final boolean hydra;
    public final int heads, nn;
    public final Vector3f hip = new Vector3f();
    public final Vector3f[] torso = arr(NT + 1), tail = arr(NTL + 1);
    public final float[] torsoR = new float[NT + 1], tailR = new float[NTL + 1];
    public final Vector3f[][] neck;
    public final float[][] neckR;
    public final Vector3f[] headPos, headFwd, headUp, mouth;
    public final float[] headScale, jaw, neckLen;
    public final Vector3f[][] leg = {arr(4), arr(4)}, arm = {arr(4), arr(4)}, finger = {arr(4), arr(4)};
    public final Vector3f torsoUp = new Vector3f(), tailUp = new Vector3f();
    public final Vector3f[] neckUp;
    public final Matrix4f root = new Matrix4f();

    private final float hipH, hipZ, torsoPitch, torsoLen, tailLen, neckSeg, hw, thigh, shin, pivotY;
    private final float[] torsoBase;
    private final Vector3f d = new Vector3f(), v = new Vector3f(), w = new Vector3f();

    private static Vector3f[] arr(int n) {
        Vector3f[] a = new Vector3f[n];
        for (int i = 0; i < n; i++) a[i] = new Vector3f();
        return a;
    }

    public Rig(boolean hydra) {
        this.hydra = hydra;
        heads = hydra ? 3 : 1;
        nn = hydra ? 11 : 4;
        neck = new Vector3f[heads][];
        neckR = new float[heads][nn + 1];
        for (int h = 0; h < heads; h++) neck[h] = arr(nn + 1);
        headPos = arr(heads); headFwd = arr(heads); headUp = arr(heads); mouth = arr(heads); neckUp = arr(heads);
        headScale = new float[heads]; jaw = new float[heads]; neckLen = new float[heads];
        if (hydra) {
            hipH = 16f; hipZ = -1.5f; torsoPitch = 76; torsoLen = 3.1f; tailLen = 3.6f; neckSeg = 2.3f; hw = 4.8f; thigh = 7.4f; shin = 6.8f; pivotY = 19;
            torsoBase = new float[]{5.8f, 6.6f, 6.7f, 6.4f, 6.3f, 6.4f, 5.4f};
        } else {
            hipH = 15.5f; hipZ = -3f; torsoPitch = 70; torsoLen = 3.6f; tailLen = 3.4f; neckSeg = 2.3f; hw = 5.4f; thigh = 7.2f; shin = 6.4f; pivotY = 18;
            torsoBase = new float[]{7.2f, 7.9f, 7.7f, 7.1f, 6.3f, 5.3f, 4.3f};
        }
        for (int i = 0; i <= NTL; i++) {
            float f = i / (float) NTL;
            float r = (hydra ? 5.5f : 6.9f) * (float) Math.pow(1 - f, 1.05) + 0.5f;
            if (!hydra && i >= NTL - 2) r = i == NTL - 1 ? 2.7f : i == NTL ? 1.9f : 1.7f;
            tailR[i] = r;
        }
    }

    private static void dir(float yawDeg, float pitchDeg, Vector3f out) {
        float y = yawDeg * DEG, p = pitchDeg * DEG, c = Mth.cos(p);
        out.set(Mth.sin(y) * c, Mth.sin(p), Mth.cos(y) * c);
    }

    public void compute(TitanEntity e, float pt) {
        float[] p = e.params(pt);
        float time = e.tickCount + pt;
        float walk = Mth.lerp(pt, e.walkAmtO, e.walkAmt), ph = Mth.lerp(pt, e.walkPhaseO, e.walkPhase);
        float aimYaw = Mth.lerp(pt, e.aimYawO, e.aimYaw), aimPitch = Mth.lerp(pt, e.aimPitchO, e.aimPitch);
        float roll = p[ROLL] * DEG, pivot = p[PIVOT], crouch = p[CROUCH], kick = p[KICK], limp = p[LIMP], shake = p[SHAKE];
        float lie = Math.abs(Mth.sin(roll)) * (hydra ? 5.2f : 6.4f) * (1 - pivot);
        root.identity().translate(0, lie + pivotY * pivot, 0).rotateZ(-roll).rotateX(-p[PITCH] * DEG).translate(0, -pivotY * pivot, 0);

        hip.set(Mth.sin(ph) * 0.9f * walk, hipH - p[DROP] - crouch * 2.6f + Mth.sin(ph * 2) * 0.55f * walk, hipZ);
        float lean = p[LEAN], twist = p[TWIST];
        float breathe = 1 + 0.014f * Mth.sin(time * 0.07f);
        torso[0].set(hip);
        torsoR[0] = torsoBase[0];
        float topPitch = torsoPitch;
        for (int i = 1; i <= NT; i++) {
            float f = i / (float) NT;
            topPitch = torsoPitch - lean * (0.35f + 0.65f * f);
            dir(twist * f, topPitch, d);
            torso[i].set(torso[i - 1]).fma(torsoLen, d);
            torsoR[i] = torsoBase[i] * breathe;
        }
        dir(0, torsoPitch - lean * 0.5f + 90, torsoUp);

        // tail: every segment keeps the heading the body had a moment ago, so it trails and whips
        tail[0].set(hip).add(0, -0.6f, -1.0f);
        float up = Mth.clamp(p[TAIL_UP] / 16f, 0, 1);
        float yawNow = e.yawBack(0, pt);
        for (int i = 1; i <= NTL; i++) {
            float f = i / (float) NTL;
            float rel = Mth.clamp(e.yawBack(i * LAG, pt) - yawNow, -160, 160);
            float yaw = 180 - rel + Mth.sin(f * 5.6f) * 11 * (1 - up) + Mth.sin(time * 0.05f - i * 0.45f) * 6 * f + Mth.sin(ph - i * 0.5f) * 8 * walk * f
                    + Mth.sin(time * 0.5f - i * 0.6f) * 14 * kick * f;
            float pitch = (-38 * Mth.sin(PI * Math.min(1, f / 0.55f)) + Math.max(0, f - 0.72f) * 60) * (1 - up) + up * 5 * (1 - f) + Mth.cos(time * 0.4f - i * 0.5f) * 9 * kick * f;
            dir(yaw, pitch, d);
            tail[i].set(tail[i - 1]).fma(tailLen, d);
            float min = tailR[i] * 0.85f;
            if (tail[i].y < min) tail[i].y = min;
        }
        tailUp.set(0, 1, 0);

        // necks and heads
        Vector3f top = torso[NT];
        for (int h = 0; h < heads; h++) {
            float side = hydra ? (h - 1) : 0;
            float g = hydra ? e.headGrowth(h, pt) : 1;
            float gs = g * g * (3 - 2 * g);
            float ls = Math.max(0.14f, gs);
            neck[h][0].set(top).add(side * 4.6f, hydra ? (side == 0 ? -1.0f : -3.2f) : -0.5f, hydra ? 0.3f : 0.6f);
            float seg = neckSeg * (side == 0 && hydra ? 1.12f : 1f) * ls * (1 + 0.12f * Math.max(0, p[REACH]));
            neckLen[h] = seg * nn;
            float reach = p[REACH] * (h == e.biteHead() || !hydra ? 1 : 0.35f);
            float yaw = 0, pitch = 0;
            float wob = 1 - gs * 0.7f;
            for (int i = 1; i <= nn; i++) {
                float s = i / (float) nn;
                if (hydra) {
                    pitch = (side == 0 ? 96 : 86) - 84 * s + (side == 0 ? 24 : 18) * Mth.sin(2 * PI * s);
                    yaw = side * 50 * (1 - 0.8f * s) + side * 16 * p[ARM] * s * (1 - s) * 4 * 0.5f;
                } else {
                    pitch = Mth.lerp(s, 58, 10);
                    yaw = 0;
                }
                pitch -= lean * 0.5f;
                float idle = (1 - limp) * (0.4f + wob);
                yaw += Mth.sin(time * 0.045f + h * 2.1f + s * 2.2f) * 8 * s * idle + Mth.sin(time * 2.3f + h * 1.7f) * 2.6f * shake * s;
                pitch += Mth.sin(time * 0.06f + h * 1.3f + s) * 5 * s * idle + Mth.cos(time * 2.9f + h) * 1.6f * shake * s;
                float aw = (float) Math.pow(s, 1.4);
                yaw += (aimYaw - yaw * 0.6f) * aw * 0.9f + p[NECK_Y] * s;
                pitch += aimPitch * aw * 0.75f + p[NECK_P] * s;
                if (reach > 0) pitch = Mth.lerp(reach * 0.75f * s, pitch, aimPitch + 4);
                else pitch += reach * 34 * Mth.sin(PI * s);
                if (limp > 0) {
                    pitch = Mth.lerp(limp, pitch, topPitch - 14 * s + (hydra ? side * side * 6 : 0));
                    yaw = Mth.lerp(limp, yaw, side * (14 + 18 * s));
                }
                pitch = Mth.clamp(pitch, -85, 110);
                dir(yaw, pitch, d);
                neck[h][i].set(neck[h][i - 1]).fma(seg, d);
            }
            for (int i = 0; i <= nn; i++) {
                float s = i / (float) nn;
                neckR[h][i] = (hydra ? Mth.lerp(s, 3.1f, 1.9f) : Mth.lerp(s, 4.0f, 3.1f)) * (0.45f + 0.55f * gs);
            }
            float hp = Mth.lerp(0.55f, pitch, aimPitch + p[NECK_P] * 0.6f) * (1 - limp) + pitch * limp;
            dir(yaw, hp, headFwd[h]);
            dir(yaw, hp + 90, headUp[h]);
            dir(0, 150, neckUp[h]);
            headPos[h].set(neck[h][nn]);
            headScale[h] = (hydra ? 1.0f : 1.22f) * (g < 0.04f ? 0 : 0.3f + 0.7f * gs);
            float j = p[JAW];
            if (hydra && h != e.biteHead() && e.getState() == PIN) j = 0.25f + 0.2f * Mth.sin(time * 0.6f + h * 2);
            if (hydra) j = Mth.clamp(j + Mth.sin(time * 0.13f + h * 2.4f) * 0.05f, 0, 1);
            jaw[h] = j;
            mouth[h].set(headPos[h]).fma(5.6f * headScale[h], headFwd[h]).fma(-0.8f * headScale[h], headUp[h]);
        }

        // legs: two-bone IK onto planted feet
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1 : -1;
            float phi = ph + (k == 0 ? 0 : PI);
            float stride = (hydra ? 5.2f : 4.6f) * Math.min(1.3f, walk);
            Vector3f H = leg[k][0].set(hip).add(sx * hw, -1.5f, 0.3f);
            float lift = Math.max(0, Mth.sin(phi)) * 3.4f * Math.min(1, walk);
            Vector3f A = leg[k][2].set(sx * (hw + 1.1f + crouch * 2.2f), 2.3f + lift, hipZ + 0.6f - stride * Mth.cos(phi));
            if (kick > 0) A.add(sx * kick * 2, kick * (4 + 3 * Mth.sin(time * 0.5f + sx * 1.3f)), kick * 5 * Mth.sin(time * 0.42f + sx * 2));
            v.set(A).sub(H);
            float dist = Mth.clamp(v.length(), 1f, thigh + shin - 0.05f);
            v.normalize();
            float ca = Mth.clamp((thigh * thigh + dist * dist - shin * shin) / (2 * thigh * dist), -1, 1);
            float sa = Mth.sqrt(1 - ca * ca);
            w.set(0, 0.25f, 1).fma(-(v.z + 0.25f * v.y), v);
            if (w.lengthSquared() < 1e-4f) w.set(0, 0, 1);
            w.normalize();
            leg[k][1].set(H).fma(thigh * ca, v).fma(thigh * sa, w);
            A.set(H).fma(dist, v);
            leg[k][3].set(A).add(sx * 0.6f, 0, hydra ? 5.2f : 5.6f);
            leg[k][3].y = Math.max(0.9f, A.y - 1.5f + kick * 2);
        }

        // arms (reptile) or wings (hydra)
        float armP = p[ARM];
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1 : -1;
            if (!hydra) {
                Vector3f S = arm[k][0].set(torso[NT - 1]).add(sx * 5.0f, -1.2f, 1.2f);
                float sw = Mth.sin(ph + sx) * 7 * walk + Mth.sin(time * 0.6f + sx * 2) * 30 * kick;
                dir(sx * (22 + 55 * armP), -62 + 48 * armP - lean * 0.7f + sw, d);
                arm[k][1].set(S).fma(5.6f, d);
                dir(sx * (-8 + 20 * armP), -8 + 36 * armP - lean * 0.7f + sw + Mth.sin(time * 0.08f + sx) * 4, d);
                arm[k][2].set(arm[k][1]).fma(5.0f, d);
                arm[k][3].set(arm[k][2]).fma(2.2f, d);
            } else {
                Vector3f S = arm[k][0].set(torso[NT - 1]).add(sx * 4.6f, -1.0f, -2.2f);
                float flap = Mth.sin(time * 0.36f) * p[FLAP] + Mth.sin(time * 0.05f + 1) * 0.06f + Mth.sin(time * 0.7f + sx) * 0.3f * kick;
                dir(sx * Mth.lerp(armP, 148, 98), Mth.lerp(armP, 8, 34) + flap * 22 - lean * 0.3f, d);
                arm[k][1].set(S).fma(14f, d);
                dir(sx * Mth.lerp(armP, 172, 78), Mth.lerp(armP, 58, 30) + flap * 28, d);
                arm[k][2].set(arm[k][1]).fma(17f, d);
                arm[k][3].set(arm[k][2]).fma(2.5f, d);
                for (int q = 0; q < 4; q++) {
                    float len = new float[]{26, 32, 31, 25}[q] * Mth.lerp(armP, 0.78f, 1f);
                    dir(sx * Mth.lerp(armP, 176 - q * 3, 84 + q * 7), Mth.lerp(armP, -52 - q * 9, 38 - q * 37) + flap * (14 + q * 5), d);
                    finger[k][q].set(arm[k][2]).fma(len, d);
                }
            }
        }

        // root transform (falling over, tumbling)
        root.transformPosition(hip);
        tp(torso); tp(tail);
        for (int h = 0; h < heads; h++) {
            tp(neck[h]);
            root.transformPosition(headPos[h]); root.transformPosition(mouth[h]);
            root.transformDirection(headFwd[h]); root.transformDirection(headUp[h]); root.transformDirection(neckUp[h]);
        }
        for (int k = 0; k < 2; k++) { tp(leg[k]); tp(arm[k]); if (hydra) tp(finger[k]); }
        root.transformDirection(torsoUp); root.transformDirection(tailUp);
    }

    private void tp(Vector3f[] a) {
        for (Vector3f x : a) root.transformPosition(x);
    }
}
