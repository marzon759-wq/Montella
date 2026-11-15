package com.krolasyon.titans.client;

import com.krolasyon.titans.entity.Fx;
import com.krolasyon.titans.entity.Rig;
import com.krolasyon.titans.entity.TitanEntity;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.Random;

import static com.krolasyon.titans.entity.TitanEntity.*;

/** Draws a titan as lofted, procedurally skinned geometry straight from its skeleton, plus beams and effects. */
public class TitanRenderer extends EntityRenderer<TitanEntity> {
    private static final ResourceLocation SCALES = new ResourceLocation("titanclash", "dyn/scales"), PUFF = new ResourceLocation("titanclash", "dyn/puff");
    private static boolean made;
    private static final float DEG = Rig.DEG, PI = Rig.PI;

    private static final float[][] KH = {{-1.4f, 2.5f, 2.3f, 1.8f}, {1.2f, 3.1f, 2.9f, 1.3f}, {3.2f, 2.7f, 2.3f, 0.35f}, {5.3f, 2.2f, 1.7f, 0.3f}, {6.8f, 1.6f, 1.15f, 0.28f}, {7.5f, 0.9f, 0.6f, 0.2f}};
    private static final float[][] KJ = {{0f, 2.5f, 0.35f, 1.5f}, {2.2f, 2.3f, 0.3f, 1.25f}, {4.3f, 1.8f, 0.25f, 0.9f}, {5.8f, 1.2f, 0.2f, 0.6f}, {6.4f, 0.6f, 0.15f, 0.3f}};
    private static final float[][] HH = {{-1.4f, 1.8f, 1.7f, 1.4f}, {1.2f, 2.3f, 2.1f, 1.0f}, {3.4f, 1.9f, 1.6f, 0.32f}, {5.8f, 1.5f, 1.15f, 0.28f}, {7.8f, 1.1f, 0.8f, 0.25f}, {8.6f, 0.6f, 0.4f, 0.18f}};
    private static final float[][] HJ = {{0f, 1.9f, 0.3f, 1.1f}, {2.5f, 1.7f, 0.28f, 0.9f}, {5f, 1.3f, 0.22f, 0.65f}, {6.8f, 0.9f, 0.18f, 0.45f}, {7.4f, 0.45f, 0.12f, 0.25f}};

    private final Mesh m = new Mesh();
    private final Vector3f a = new Vector3f(), b = new Vector3f(), c = new Vector3f(), d = new Vector3f(), e2 = new Vector3f(), rv = new Vector3f(), fj = new Vector3f(), uj = new Vector3f();
    private final Vector3f[] tmp3 = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
    private final float[] tmpR = new float[4];
    private final Random rnd = new Random();

    // palette of the creature being drawn
    private float bkR, bkG, bkB, blR, blG, blB, gR, gG, gB, glow, flesh;
    private boolean hy;

    public TitanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0;
    }

    @Override public ResourceLocation getTextureLocation(TitanEntity e) { return SCALES; }
    @Override public boolean shouldRender(TitanEntity e, Frustum f, double x, double y, double z) { return true; }

    private static void makeTextures() {
        made = true;
        int n = 32;
        Random r = new Random(7);
        NativeImage img = new NativeImage(n, n, false);
        float[][] shade = new float[8][9];
        for (float[] row : shade) for (int j = 0; j < row.length; j++) row[j] = 0.74f + r.nextFloat() * 0.26f;
        for (int x = 0; x < n; x++) for (int y = 0; y < n; y++) {
            int row = y / 4, off = (row & 1) * 2, col = ((x + off) / 4) % 8;
            float g = shade[row][col];
            int ly = y % 4, lx = (x + off) % 4;
            if (ly == 3) g *= 0.62f; else if (lx == 3) g *= 0.74f; else if (ly == 0 && lx == 0) g *= 1.08f;
            g += (r.nextFloat() - 0.5f) * 0.05f;
            int v = (int) (Mth.clamp(g, 0.2f, 1f) * 255);
            img.setPixelRGBA(x, y, 0xFF000000 | v << 16 | v << 8 | v);
        }
        reg(SCALES, img);
        NativeImage pf = new NativeImage(32, 32, false);
        for (int x = 0; x < 32; x++) for (int y = 0; y < 32; y++) {
            float dx = (x - 15.5f) / 15.5f, dy = (y - 15.5f) / 15.5f;
            float al = Mth.clamp(1 - Mth.sqrt(dx * dx + dy * dy), 0, 1);
            al = (float) Math.pow(al, 1.3) * (0.8f + 0.2f * r.nextFloat());
            int v = 215 + r.nextInt(40);
            pf.setPixelRGBA(x, y, ((int) (al * 255)) << 24 | v << 16 | v << 8 | v);
        }
        reg(PUFF, pf);
    }

    private static void reg(ResourceLocation id, NativeImage img) {
        DynamicTexture t = new DynamicTexture(img);
        Minecraft.getInstance().getTextureManager().register(id, t);
        t.bind();
        GlStateManager._texParameter(3553, 10242, 10497);
        GlStateManager._texParameter(3553, 10243, 10497);
    }

    @Override
    public void render(TitanEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int packed) {
        if (!made) makeTextures();
        Rig rig = e.rig;
        rig.compute(e, pt);
        float[] p = e.params(pt);
        float time = e.tickCount + pt;
        float bodyYaw = e.yawBack(0, pt);
        int light = LevelRenderer.getLightColor(e.level(), e.blockPosition().above(24));
        int overlay = OverlayTexture.pack(OverlayTexture.u(0), OverlayTexture.v(e.hurtTime > 6));
        hy = e.hydra;
        glow = p[GLOW];
        if (hy) { bkR = 0.21f; bkG = 0.26f; bkB = 0.36f; blR = 0.66f; blG = 0.69f; blB = 0.76f; gR = 0.72f; gG = 0.5f; gB = 1f; }
        else { bkR = 0.27f; bkG = 0.255f; bkB = 0.25f; blR = 0.47f; blG = 0.39f; blB = 0.3f; gR = 1f; gG = 0.5f; gB = 0.1f; }
        if (e.getState() == DEAD) { float k = Math.min(1, e.stateTime / 120f) * 0.35f; bkR *= 1 - k; bkG *= 1 - k; bkB *= 1 - k; blR *= 1 - k; blG *= 1 - k; blB *= 1 - k; }

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
        m.begin(buf.getBuffer(RenderType.entityCutoutNoCull(SCALES)), ps.last(), light, overlay, false);
        body(e, rig, p, time, false);

        // additive pass in body space: crystal halos, mouth glow, crawling arcs
        m.begin(buf.getBuffer(RenderType.lightning()), ps.last(), light, overlay, true);
        float beam = e.getBeam();
        float charge = e.getState() == BREATH ? Math.min(1, e.stateTime / (float) BREATH_CHARGE) : 0;
        float mg = Math.max(beam, charge);
        if (!hy && glow > 0.25f) body(e, rig, p, time, true);
        if (mg > 0.05f) {
            for (int h = 0; h < rig.heads; h++) {
                if (!e.headActive(h)) continue;
                Vector3f mo = rig.mouth[h];
                float r = (hy ? 1.5f : 2.4f) * mg * (0.85f + 0.15f * Mth.sin(time * 1.7f + h));
                m.alpha = 0.5f; m.ball(mo.x, mo.y, mo.z, r * 1.6f, gR, gG, gB);
                m.alpha = 0.9f; m.ball(mo.x, mo.y, mo.z, r * 0.8f, 1, 0.95f, 0.9f);
                if (hy) arcs(rig.neck[h], rig.nn + 1, rig.neckR[h], time, h, mg);
            }
            if (hy) arcs(rig.torso, Rig.NT + 1, rig.torsoR, time, 7, mg);
        }
        ps.popPose();

        // world-aligned pass: beams and sparks
        double ex = Mth.lerp(pt, e.xo, e.getX()), ey = Mth.lerp(pt, e.yo, e.getY()), ez = Mth.lerp(pt, e.zo, e.getZ());
        m.begin(buf.getBuffer(RenderType.lightning()), ps.last(), light, overlay, true);
        if (beam > 0.02f) {
            float cy = Mth.cos(bodyYaw * DEG), sy = Mth.sin(bodyYaw * DEG);
            float bx = (float) (Mth.lerp(pt, e.beamO.x, e.beamC.x) - ex), by = (float) (Mth.lerp(pt, e.beamO.y, e.beamC.y) - ey), bz = (float) (Mth.lerp(pt, e.beamO.z, e.beamC.z) - ez);
            float flick = e.getState() == BREATH ? 1 : 0.65f + 0.35f * Mth.sin(time * 2.9f);
            for (int h = 0; h < rig.heads; h++) {
                if (!e.headActive(h)) continue;
                Vector3f mo = rig.mouth[h];
                float ax = mo.x * cy - mo.z * sy, ay = mo.y, az = mo.x * sy + mo.z * cy;
                if (hy) lightning(ax, ay, az, bx, by, bz, beam, flick, time, h);
                else plasma(ax, ay, az, bx, by, bz, beam, flick, time);
            }
            boolean clash = e.isClash();
            float er = (clash ? 7.5f + 2f * Mth.sin(time * 1.3f) : (hy ? 3f : 4.2f)) * Mth.sqrt(beam);
            m.alpha = 0.3f * flick; m.ball(bx, by, bz, er * 1.5f, gR, gG, gB);
            m.alpha = 0.55f * flick; m.ball(bx, by, bz, er, gR, gG * 1.2f, gB);
            m.alpha = 0.9f * flick; m.ball(bx, by, bz, er * 0.5f, 1, 1, 1);
            if (clash) {
                rnd.setSeed((long) (time / 2) * 31 + e.getId());
                for (int i = 0; i < 9; i++) {
                    a.set(bx, by, bz);
                    b.set((float) rnd.nextGaussian(), (float) rnd.nextGaussian(), (float) rnd.nextGaussian());
                    m.alpha = 0.8f;
                    m.cone(a, b, er * (1.5f + rnd.nextFloat() * 2f), er * 0.18f, 4, 1, 1, 1, gR, gG, gB, 1);
                }
            }
        }
        Vector3f cl = entityRenderDispatcher.camera.getLeftVector(), cu = entityRenderDispatcher.camera.getUpVector();
        for (Fx f : e.fx) {
            if (f.type != 1) continue;
            float k = 1 - (f.age + pt) / f.life, s = f.size * (0.4f + 0.6f * k);
            float x = (float) (Mth.lerp(pt, f.xo, f.x) - ex), y = (float) (Mth.lerp(pt, f.yo, f.y) - ey), z = (float) (Mth.lerp(pt, f.zo, f.z) - ez);
            m.alpha = Mth.clamp(k * 1.3f, 0, 1);
            m.vert(x + cl.x() * s, y + cl.y() * s, z + cl.z() * s, f.r, f.g, f.b, 0, 0, 0, 1, 0, 1);
            m.vert(x + cu.x() * s, y + cu.y() * s, z + cu.z() * s, 1, 1, 1, 0, 0, 0, 1, 0, 1);
            m.vert(x - cl.x() * s, y - cl.y() * s, z - cl.z() * s, f.r, f.g, f.b, 0, 0, 0, 1, 0, 1);
            m.vert(x - cu.x() * s, y - cu.y() * s, z - cu.z() * s, f.r, f.g, f.b, 0, 0, 0, 1, 0, 1);
        }
        boolean any = false;
        for (Fx f : e.fx) if (f.type == 0) { any = true; break; }
        if (any) {
            m.begin(buf.getBuffer(RenderType.entityTranslucent(PUFF)), ps.last(), light, OverlayTexture.NO_OVERLAY, false);
            for (Fx f : e.fx) {
                if (f.type != 0) continue;
                float k = 1 - (f.age + pt) / f.life, s = f.size;
                float x = (float) (Mth.lerp(pt, f.xo, f.x) - ex), y = (float) (Mth.lerp(pt, f.yo, f.y) - ey), z = (float) (Mth.lerp(pt, f.zo, f.z) - ez);
                m.alpha = Mth.clamp(k * 1.6f, 0, 1) * 0.6f;
                m.vert(x + (cl.x() - cu.x()) * s, y + (cl.y() - cu.y()) * s, z + (cl.z() - cu.z()) * s, f.r, f.g, f.b, 0, 1, 0, 1, 0, 0);
                m.vert(x + (cl.x() + cu.x()) * s, y + (cl.y() + cu.y()) * s, z + (cl.z() + cu.z()) * s, f.r, f.g, f.b, 0, 0, 0, 1, 0, 0);
                m.vert(x - (cl.x() - cu.x()) * s, y - (cl.y() - cu.y()) * s, z - (cl.z() - cu.z()) * s, f.r, f.g, f.b, 1, 0, 0, 1, 0, 0);
                m.vert(x - (cl.x() + cu.x()) * s, y - (cl.y() + cu.y()) * s, z - (cl.z() + cu.z()) * s, f.r, f.g, f.b, 1, 1, 0, 1, 0, 0);
            }
        }
        m.alpha = 1;
    }

    // ---------------------------------------------------------------- body

    /** Skin: dark back, pale belly; the reptile's belly cracks light up as it charges. */
    private void skin(int i, float ca, float sa, float[] o) {
        float k = Mth.clamp((sa + 0.6f) / 0.8f, 0, 1);
        k = k * k * (3 - 2 * k);
        float shade = 0.92f + 0.08f * Mth.sin(i * 1.7f + ca * 3);
        o[0] = Mth.lerp(k, blR, bkR) * shade; o[1] = Mth.lerp(k, blG, bkG) * shade; o[2] = Mth.lerp(k, blB, bkB) * shade;
        if (flesh > 0) { o[0] = Mth.lerp(flesh, o[0], 0.62f); o[1] = Mth.lerp(flesh, o[1], 0.3f); o[2] = Mth.lerp(flesh, o[2], 0.4f); }
        if (!hy && glow > 0.2f) {
            float cr = (1 - k) * (glow - 0.2f) * (0.45f + 0.55f * Mth.sin(i * 2.3f + ca * 5)) * 0.9f;
            if (cr > 0) { o[0] += cr; o[1] += cr * 0.38f; o[2] += cr * 0.05f; o[3] = cr; }
        }
    }

    private float spineGlow(float q) { return Math.max(hy ? 0 : 0.1f, Mth.clamp((glow * 1.45f - 0.3f - q) * 4, 0, 1)); }

    private void body(TitanEntity e, Rig rig, float[] p, float time, boolean halo) {
        Mesh.Paint sk = (i, f, ca, sa, o) -> skin(i, ca, sa, o);
        Mesh.Paint limb = (i, f, ca, sa, o) -> skin(i, ca, 0.15f + sa * 0.3f, o);
        flesh = 0;
        // torso
        m.chain(rig.torso, rig.torsoR, Rig.NT + 1, 2, rig.torsoUp, 0.93f);
        if (!halo) m.loft(14, sk, true, true, 5, 0.16f);
        for (int i = 1; i < m.n; i++) {
            float f = i / (float) (m.n - 1);
            if (hy) { if (!halo) spine(i, 2.2f + 1.2f * Mth.sin(PI * f), -0.35f); }
            else crystals(i, (3.4f + 4.8f * Mth.sin(PI * Mth.clamp(f * 0.9f + 0.08f, 0, 1))) * (i % 2 == 0 ? 1 : 0.72f), -0.35f, spineGlow(0.5f + 0.33f * f), true, halo);
        }
        // tail
        m.chain(rig.tail, rig.tailR, Rig.NTL + 1, 2, rig.tailUp, 0.95f);
        if (!halo) m.loft(12, sk, false, true, 4, 0.16f);
        for (int i = 1; i < m.n; i++) {
            float f = i / (float) (m.n - 1);
            if (hy) {
                if (!halo && i < m.n - 4) spine(i, 2.4f * (1 - f) + 0.7f, 0.4f);
            } else if (i < m.n - 5) crystals(i, (5.0f * (float) Math.pow(1 - f, 1.3) + 0.8f) * (i % 2 == 0 ? 1 : 0.7f), 0.4f, spineGlow(0.5f * (1 - f)), f < 0.5f, halo);
        }
        if (hy) { if (!halo) tailBlade(); } else tailClub(spineGlow(0), halo);
        // necks and heads
        for (int h = 0; h < rig.heads; h++) {
            float g = hy ? e.headGrowth(h, 1) : 1;
            flesh = hy ? Mth.clamp(1 - g * 1.15f, 0, 1) * 0.8f : 0;
            m.chain(rig.neck[h], rig.neckR[h], rig.nn + 1, 2, rig.neckUp[h], 1f);
            boolean stump = rig.headScale[h] <= 0;
            if (!halo) {
                final boolean st = stump;
                m.loft(12, st ? (i, f, ca, sa, o) -> { skin(i, ca, sa, o); if (f > 0.75f) { o[0] = 0.5f; o[1] = 0.07f; o[2] = 0.12f; } } : sk, false, stump, 3, 0.16f);
            }
            if (!stump) for (int i = 2; i < m.n - 1; i++) {
                float f = i / (float) (m.n - 1);
                if (hy) { if (!halo && i % 2 == 0) spine(i, 1.7f * (0.5f + 0.5f * g), -0.3f); }
                else crystals(i, 2.3f - 1.3f * f, -0.3f, spineGlow(0.84f + 0.16f * f), false, halo);
            }
            if (!stump && !halo) head(rig.headPos[h], rig.headFwd[h], rig.headUp[h], rig.headScale[h], rig.jaw[h],
                    Math.max(e.getBeam(), e.getState() == BREATH ? Math.min(1, e.stateTime / (float) BREATH_CHARGE) * 0.7f : 0) * (e.headActive(h) ? 1 : 0));
        }
        flesh = 0;
        if (halo) return;
        // legs
        b.set(0, 0, -1);
        rig.root.transformDirection(b);
        for (int k = 0; k < 2; k++) {
            Vector3f[] L = rig.leg[k];
            tmpR[0] = hy ? 4.8f : 6.0f; tmpR[1] = hy ? 3.5f : 4.4f; tmpR[2] = hy ? 2.3f : 3.0f;
            m.chain(L, tmpR, 3, 3, b, 1f);
            m.loft(10, limb, true, false, 3, 0.16f);
            tmp3[0].set(L[2]); tmp3[1].set(L[3]);
            tmpR[0] = hy ? 2.5f : 3.2f; tmpR[1] = hy ? 1.6f : 2.1f;
            a.set(0, 1, 0);
            rig.root.transformDirection(a);
            m.chain(tmp3, tmpR, 2, 2, a, 0.6f);
            m.loft(8, limb, true, true, 2, 0.16f);
            c.set(L[3]).sub(L[2]).normalize();
            a.cross(c, d);
            for (int q = -1; q <= 1; q++) {
                e2.set(L[3]).fma(q * 1.1f, d).fma(-0.3f, a);
                rv.set(c).fma(q * 0.3f, d).fma(-0.25f, a);
                m.cone(e2, rv, 2.3f, 0.6f, 5, 0.2f, 0.18f, 0.16f, 0.85f, 0.8f, 0.7f, 0);
            }
        }
        if (!hy) {
            for (int k = 0; k < 2; k++) {
                Vector3f[] A = rig.arm[k];
                tmpR[0] = 2.3f; tmpR[1] = 1.8f; tmpR[2] = 1.35f; tmpR[3] = 1.0f;
                m.chain(A, tmpR, 4, 2, b, 1f);
                m.loft(8, limb, true, true, 2, 0.16f);
                c.set(A[3]).sub(A[2]).normalize();
                d.set(k == 0 ? 1 : -1, 0, 0);
                rig.root.transformDirection(d);
                for (int q = -1; q <= 1; q++) {
                    rv.set(c).fma(q * 0.45f, d).add(0, -0.35f, 0);
                    m.cone(A[3], rv, 1.9f, 0.4f, 4, 0.2f, 0.18f, 0.16f, 0.9f, 0.85f, 0.75f, 0);
                }
            }
        } else wings(rig);
    }

    /** One ring's worth of magma crystals: a tall centre shard flanked by two smaller ones. */
    private void crystals(int i, float h, float lean, float g, boolean sides, boolean halo) {
        if (halo && g < 0.3f) return;
        Vector3f C = m.C[i], U = m.U[i], R = m.R[i], T = m.T[i];
        float r1 = Mth.lerp(g, 0.3f, 1f), g1 = Mth.lerp(g, 0.24f, 0.62f), b1 = Mth.lerp(g, 0.24f, 0.16f);
        float r0 = 0.1f + 0.5f * g, g0 = 0.09f + 0.14f * g, b0 = 0.1f;
        float sc = halo ? 1.22f : 1;
        if (halo) { m.alpha = 0.28f * g; r0 = 1; g0 = 0.4f; b0 = 0.05f; r1 = 1; g1 = 0.7f; b1 = 0.3f; }
        for (int s = sides ? -1 : 0; s <= (sides ? 1 : 0); s++) {
            float an = s * 34 * DEG, cs = Mth.cos(an), sn = Mth.sin(an);
            a.set(U).mul(cs * m.RYT[i] * 0.86f).fma(sn * m.RX[i] * 0.86f, R).add(C);
            c.set(U).mul(cs).fma(sn, R).fma(lean, T).normalize();
            float hh = h * (s == 0 ? 1 : 0.55f) * sc;
            m.shard(a, T, c, R, hh, hh * 0.62f, hh * 0.24f, lean * hh * 0.3f, r0, g0, b0, r1, g1, b1, g);
        }
    }

    private void spine(int i, float h, float lean) {
        a.set(m.C[i]).fma(m.RYT[i] * 0.85f, m.U[i]);
        c.set(m.U[i]).fma(lean, m.T[i]);
        m.cone(a, c, h, h * 0.24f, 4, bkR * 0.8f, bkG * 0.8f, bkB * 0.8f, 0.82f, 0.84f, 0.9f, 0);
    }

    private void tailClub(float g, boolean halo) {
        int i = m.n - 3;
        for (int s = -2; s <= 2; s++) {
            if (halo && g < 0.3f) return;
            float an = s * 42 * DEG;
            c.set(m.U[i]).mul(Mth.cos(an)).fma(Mth.sin(an), m.R[i]).fma(0.25f, m.T[i]).normalize();
            a.set(m.C[i]).fma(m.RX[i] * 0.7f, c);
            float hh = (s == 0 ? 4.2f : 3.3f) * (halo ? 1.2f : 1);
            if (halo) m.alpha = 0.28f * g;
            m.shard(a, m.T[i], c, m.R[i], hh, hh * 0.6f, hh * 0.3f, 0.6f, halo ? 1 : 0.1f + 0.5f * g, halo ? 0.4f : 0.09f + 0.14f * g, halo ? 0.05f : 0.1f,
                    Mth.lerp(g, 0.3f, 1f), Mth.lerp(g, 0.24f, 0.62f), Mth.lerp(g, 0.24f, 0.16f), g);
        }
    }

    private void tailBlade() {
        int i = m.n - 1, j = m.n - 6, k = m.n - 3;
        for (int s = -1; s <= 1; s += 2) {
            a.set(m.C[j]).fma(s * m.RX[j], m.R[j]);
            b.set(m.C[k]).fma(s * 6.5f, m.R[k]).fma(0.6f, m.U[k]);
            c.set(m.C[i]).fma(3.5f, m.T[i]);
            m.tri(a, b, c, bkR, bkG, bkB, 0.7f, 0.72f, 0.82f, 1.5f);
            d.set(m.C[i]);
            m.tri(a, d, c, bkR, bkG, bkB, 0.5f, 0.52f, 0.62f, 1.5f);
        }
        a.set(m.C[i]);
        m.cone(a, m.T[i], 4.5f, 0.7f, 4, bkR, bkG, bkB, 0.85f, 0.87f, 0.95f, 0);
    }

    private static float st(float[][] t, float z, int col) {
        if (z <= t[0][0]) return t[0][col];
        for (int i = 1; i < t.length; i++) if (z <= t[i][0]) return Mth.lerp((z - t[i - 1][0]) / (t[i][0] - t[i - 1][0]), t[i - 1][col], t[i][col]);
        return t[t.length - 1][col];
    }

    private void head(Vector3f P, Vector3f F, Vector3f Uu, float s, float jaw, float mouthGlow) {
        float[][] up = hy ? HH : KH, lo = hy ? HJ : KJ;
        F.cross(Uu, rv);
        Mesh.Paint upper = (i, f, ca, sa, o) -> {
            skin(i, ca, sa, o);
            if (sa < -0.3f && i >= 2) mouth(o, mouthGlow);
        };
        m.n = up.length;
        for (int i = 0; i < m.n; i++) {
            m.C[i].set(P).fma(up[i][0] * s, F);
            m.U[i].set(Uu); m.R[i].set(rv); m.T[i].set(F);
            m.RX[i] = up[i][1] * s; m.RYT[i] = up[i][2] * s; m.RYB[i] = up[i][3] * s; m.VV[i] = up[i][0] * s;
        }
        m.loft(12, upper, false, true, 2, 0.16f);
        // lower jaw, hinged
        float ja = jaw * 40 * DEG;
        fj.set(F).mul(Mth.cos(ja)).fma(-Mth.sin(ja), Uu);
        uj.set(Uu).mul(Mth.cos(ja)).fma(Mth.sin(ja), F);
        a.set(P).fma(1.3f * s, F).fma(-0.75f * s, Uu);
        m.n = lo.length;
        for (int i = 0; i < m.n; i++) {
            m.C[i].set(a).fma(lo[i][0] * s, fj);
            m.U[i].set(uj); m.R[i].set(rv); m.T[i].set(fj);
            m.RX[i] = lo[i][1] * s; m.RYT[i] = lo[i][2] * s; m.RYB[i] = lo[i][3] * s; m.VV[i] = lo[i][0] * s;
        }
        m.loft(12, (i, f, ca, sa, o) -> { skin(i, ca, -0.9f, o); if (sa > 0.2f) mouth(o, mouthGlow); }, true, true, 2, 0.16f);
        // teeth
        float zmax = up[up.length - 1][0] - 0.5f;
        for (float z = 2.0f; z < zmax; z += 0.75f) for (int sd = -1; sd <= 1; sd += 2) {
            b.set(P).fma(z * s, F).fma(sd * st(up, z, 1) * 0.86f * s, rv).fma(-st(up, z, 3) * s, Uu);
            c.set(Uu).negate();
            m.cone(b, c, (0.9f - 0.04f * z) * s, 0.2f * s, 3, 0.8f, 0.76f, 0.66f, 0.97f, 0.95f, 0.88f, 0);
        }
        float jmax = lo[lo.length - 1][0] - 0.3f;
        for (float z = 1.4f; z < jmax; z += 0.75f) for (int sd = -1; sd <= 1; sd += 2) {
            b.set(a).fma(z * s, fj).fma(sd * st(lo, z, 1) * 0.84f * s, rv).fma(st(lo, z, 2) * s, uj);
            m.cone(b, uj, (0.8f - 0.04f * z) * s, 0.19f * s, 3, 0.8f, 0.76f, 0.66f, 0.97f, 0.95f, 0.88f, 0);
        }
        // eyes
        for (int sd = -1; sd <= 1; sd += 2) {
            float ez = 2.3f;
            b.set(P).fma(ez * s, F).fma(sd * st(up, ez, 1) * 0.9f * s, rv).fma(st(up, ez, 2) * 0.5f * s, Uu);
            float er = hy ? 0.85f : 1f, eg = hy ? 0.7f : 0.62f, eb = hy ? 1f : 0.1f;
            m.vert(b.x + F.x * 0.8f * s, b.y + F.y * 0.8f * s, b.z + F.z * 0.8f * s, er, eg, eb, 0, 0, rv.x * sd, rv.y * sd, rv.z * sd, 1);
            m.vert(b.x + Uu.x * 0.42f * s, b.y + Uu.y * 0.42f * s, b.z + Uu.z * 0.42f * s, er, eg, eb, 0, 0, rv.x * sd, rv.y * sd, rv.z * sd, 1);
            m.vert(b.x - F.x * 0.7f * s, b.y - F.y * 0.7f * s, b.z - F.z * 0.7f * s, 1, 1, 1, 0, 0, rv.x * sd, rv.y * sd, rv.z * sd, 1);
            m.vert(b.x - Uu.x * 0.3f * s, b.y - Uu.y * 0.3f * s, b.z - Uu.z * 0.3f * s, er, eg, eb, 0, 0, rv.x * sd, rv.y * sd, rv.z * sd, 1);
            // horns
            if (hy) {
                b.set(P).fma(0.6f * s, F).fma(sd * 1.5f * s, rv).fma(1.6f * s, Uu);
                c.set(F).mul(-1f).fma(0.55f, Uu).fma(sd * 0.3f, rv);
                m.cone(b, c, 5.6f * s, 0.6f * s, 5, bkR, bkG, bkB, 0.9f, 0.9f, 0.95f, 0);
                b.set(P).fma(0.3f * s, F).fma(sd * 2.1f * s, rv).fma(0.3f * s, Uu);
                c.set(F).mul(-1f).fma(0.1f, Uu).fma(sd * 0.6f, rv);
                m.cone(b, c, 3.4f * s, 0.45f * s, 4, bkR, bkG, bkB, 0.9f, 0.9f, 0.95f, 0);
                b.set(P).fma(-0.4f * s, F).fma(sd * 1.6f * s, rv).fma(-0.9f * s, Uu);
                c.set(F).mul(-1f).fma(-0.35f, Uu).fma(sd * 0.45f, rv);
                m.cone(b, c, 2.4f * s, 0.4f * s, 4, bkR, bkG, bkB, 0.9f, 0.9f, 0.95f, 0);
            } else {
                b.set(P).fma(1.6f * s, F).fma(sd * 2.0f * s, rv).fma(2.4f * s, Uu);
                c.set(F).mul(-0.75f).fma(0.7f, Uu).fma(sd * 0.45f, rv);
                m.cone(b, c, 3.6f * s, 0.75f * s, 5, 0.16f, 0.15f, 0.15f, 0.75f, 0.68f, 0.55f, 0);
                b.set(P).fma(0.2f * s, F).fma(sd * 2.7f * s, rv).fma(-0.2f * s, Uu);
                c.set(F).mul(-0.8f).fma(sd * 0.7f, rv);
                m.cone(b, c, 2.2f * s, 0.55f * s, 4, 0.16f, 0.15f, 0.15f, 0.75f, 0.68f, 0.55f, 0);
            }
        }
        if (hy) {
            b.set(a).fma((jmax - 0.6f) * s, fj).fma(-0.4f * s, uj);
            c.set(uj).negate().fma(-0.4f, fj);
            m.cone(b, c, 1.8f * s, 0.35f * s, 4, bkR, bkG, bkB, 0.9f, 0.9f, 0.95f, 0);
        } else {
            b.set(P).fma(6.2f * s, F).fma(1.2f * s, Uu);
            c.set(Uu).fma(0.3f, F);
            m.cone(b, c, 1.5f * s, 0.5f * s, 4, 0.16f, 0.15f, 0.15f, 0.75f, 0.68f, 0.55f, 0);
        }
    }

    private void mouth(float[] o, float mg) {
        o[0] = Mth.lerp(mg, 0.5f, gR); o[1] = Mth.lerp(mg, 0.1f, gG * 1.2f); o[2] = Mth.lerp(mg, 0.11f, gB); o[3] = mg;
    }

    private void wings(Rig rig) {
        Mesh.Paint sk = (i, f, ca, sa, o) -> skin(i, ca, 0.8f, o);
        float mr = 0.17f, mg2 = 0.15f, mb = 0.29f, er = 0.34f, eg = 0.3f, eb = 0.5f;
        for (int k = 0; k < 2; k++) {
            Vector3f[] A = rig.arm[k], Fg = rig.finger[k];
            a.set(0, 0, -1);
            rig.root.transformDirection(a);
            tmpR[0] = 2.1f; tmpR[1] = 1.6f; tmpR[2] = 1.1f; tmpR[3] = 0.6f;
            m.chain(A, tmpR, 4, 2, a, 1f);
            m.loft(8, sk, true, true, 2, 0.16f);
            c.set(A[3]).sub(A[2]).normalize();
            m.cone(A[3], c, 2.6f, 0.5f, 4, bkR, bkG, bkB, 0.9f, 0.9f, 0.95f, 0);
            Vector3f wrist = A[2];
            for (int q = 0; q < 4; q++) {
                tmp3[0].set(wrist); tmp3[1].set(Fg[q]);
                tmpR[0] = 0.7f; tmpR[1] = 0.18f;
                m.chain(tmp3, tmpR, 2, 3, a, 1f);
                m.loft(5, sk, false, false, 1, 0.16f);
                if (q < 3) {
                    b.set(Fg[q]).add(Fg[q + 1]).mul(0.5f).lerp(wrist, 0.16f);
                    m.tri(wrist, Fg[q], b, mr, mg2, mb, er, eg, eb, 3);
                    m.tri(wrist, b, Fg[q + 1], mr, mg2, mb, er, eg, eb, 3);
                }
            }
            d.set(rig.torso[1]).lerp(rig.torso[0], 0.3f);
            e2.set(Fg[3]).add(d).mul(0.5f).lerp(wrist, 0.2f);
            m.tri(wrist, Fg[3], e2, mr, mg2, mb, er, eg, eb, 3);
            m.tri(wrist, e2, d, mr, mg2, mb, mr, mg2, mb, 3);
            m.tri(wrist, d, A[1], mr, mg2, mb, mr, mg2, mb, 3);
            m.tri(A[1], d, A[0], mr, mg2, mb, mr, mg2, mb, 3);
        }
    }

    // ---------------------------------------------------------------- energy

    private void arcs(Vector3f[] pts, int count, float[] rad, float time, int seed, float amount) {
        rnd.setSeed((long) (time / 2) * 131 + seed * 977L);
        m.n = count;
        for (int i = 0; i < count; i++) {
            float r = rad[i] * 1.25f;
            m.C[i].set(pts[i]).add((float) rnd.nextGaussian() * r * 0.75f, (float) rnd.nextGaussian() * r * 0.5f, (float) rnd.nextGaussian() * r * 0.75f);
            m.RX[i] = m.RYT[i] = m.RYB[i] = 0.16f + 0.14f * amount;
        }
        a.set(0, 0, 1);
        m.frames(a);
        m.alpha = 0.75f * amount;
        m.loft(4, (i, f, ca, sa, o) -> { o[0] = 0.85f; o[1] = 0.75f; o[2] = 1f; }, false, false, 1, 1);
    }

    private void shape(float radius, float time, float wob, float startTaper) {
        for (int i = 0; i < m.n; i++) {
            float t = i / (float) (m.n - 1);
            float r = radius * (1 + wob * Mth.sin(i * 1.3f - time * 1.6f)) * Math.min(1, startTaper + t * 12);
            m.RX[i] = m.RYT[i] = m.RYB[i] = r;
        }
    }

    private void plasma(float ax, float ay, float az, float bx, float by, float bz, float beam, float flick, float time) {
        float len = Mth.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
        int segs = Mth.clamp((int) (len / 3.5f), 4, 60);
        float rad = 2.6f * Mth.sqrt(beam);
        a.set(0, 1, 0.01f);
        float[][] layers = {{1.25f, 0.9f, 0.22f, 0.03f, 0.22f}, {0.8f, 1f, 0.55f, 0.12f, 0.5f}, {0.4f, 1f, 0.96f, 0.82f, 0.95f}};
        for (float[] l : layers) {
            m.line(ax, ay, az, bx, by, bz, segs);
            shape(rad * l[0], time, 0.16f, 0.35f);
            m.frames(a);
            m.alpha = l[4] * beam * flick;
            m.loft(8, (i, f, ca, sa, o) -> { o[0] = l[1]; o[1] = l[2]; o[2] = l[3]; }, false, false, 1, 1);
        }
    }

    private void lightning(float ax, float ay, float az, float bx, float by, float bz, float beam, float flick, float time, int h) {
        float len = Mth.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
        int segs = Mth.clamp((int) (len / 4f), 4, 56);
        a.set(0, 1, 0.01f);
        m.line(ax, ay, az, bx, by, bz, segs);
        shape(1.5f * Mth.sqrt(beam), time, 0.2f, 0.3f);
        m.frames(a);
        m.alpha = 0.2f * beam * flick;
        m.loft(6, (i, f, ca, sa, o) -> { o[0] = 0.45f; o[1] = 0.25f; o[2] = 1f; }, false, false, 1, 1);
        for (int k = 0; k < 3; k++) {
            rnd.setSeed((long) (time / 2) * 53 + h * 19L + k * 7L);
            m.line(ax, ay, az, bx, by, bz, segs);
            float amp = (1.0f + k * 0.9f) * (0.6f + 0.4f * beam);
            for (int i = 1; i < m.n - 1; i++) m.C[i].add((float) rnd.nextGaussian() * amp, (float) rnd.nextGaussian() * amp, (float) rnd.nextGaussian() * amp);
            for (int i = 0; i < m.n; i++) m.RX[i] = m.RYT[i] = m.RYB[i] = (k == 0 ? 0.55f : 0.3f) * Mth.sqrt(beam);
            m.frames(a);
            m.alpha = (k == 0 ? 0.95f : 0.7f) * beam * flick;
            final boolean core = k == 0;
            m.loft(4, (i, f, ca, sa, o) -> { o[0] = core ? 1f : 0.78f; o[1] = core ? 0.95f : 0.6f; o[2] = 1f; }, false, false, 1, 1);
        }
    }
}
