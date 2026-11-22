package com.krolasyon.kaiju.client;

import com.krolasyon.kaiju.KaijuMod;
import com.krolasyon.kaiju.data.KaijuSpec;
import com.krolasyon.kaiju.entity.KaijuEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws the kaiju at its own scale, then re-renders it in extra passes:
 *  - GLOW: each dorsal plate glows by how far it has slid out (its bone's y offset / its travel), so the light runs
 *    tail-to-head with the plates; the mouth glows while charging/firing
 *  - BURN / BURN_GLOW: burnt regions show the cracked, molten skin, as strongly as that region's heat
 *  - REGEN: a regrowing limb is raw red flesh that fades back to hide as it finishes
 * and finally the atomic beam as layered additive ribbons.
 */
public class KaijuRenderer extends GeoEntityRenderer<KaijuEntity> {
    enum Pass { NORMAL, GLOW, BURN, BURN_GLOW, REGEN }

    static final ResourceLocation GLOW_TEX = new ResourceLocation(KaijuMod.MODID, "textures/entity/kaiju_glowmask.png");
    static final ResourceLocation BURN_TEX = new ResourceLocation(KaijuMod.MODID, "textures/entity/kaiju_burning.png");
    static final ResourceLocation BURN_GLOW_TEX = new ResourceLocation(KaijuMod.MODID, "textures/entity/kaiju_burning_glowmask.png");
    static final ResourceLocation REGEN_TEX = new ResourceLocation(KaijuMod.MODID, "textures/entity/kaiju_regen.png");

    Pass pass = Pass.NORMAL;
    KaijuEntity current;
    final Map<String, Float> travel = new HashMap<>();
    final Map<String, Integer> regionOf = new HashMap<>();
    final Map<String, Integer> severOf = new HashMap<>();

    public KaijuRenderer(EntityRendererProvider.Context ctx, GeoModel<KaijuEntity> model) {
        super(ctx, model);
        this.shadowRadius = 0;
        addRenderLayer(new PassLayer(this, Pass.BURN));
        addRenderLayer(new PassLayer(this, Pass.BURN_GLOW));
        addRenderLayer(new PassLayer(this, Pass.REGEN));
        addRenderLayer(new PassLayer(this, Pass.GLOW));
    }

    @Override
    public void scaleModelForRender(float w, float h, PoseStack ps, KaijuEntity e, BakedGeoModel model, boolean re, float pt, int light, int overlay) {
        float s = e.scale();
        super.scaleModelForRender(w * s, h * s, ps, e, model, re, pt, light, overlay);
    }

    @Override
    public boolean shouldRender(KaijuEntity e, Frustum f, double x, double y, double z) {
        return e.flag(KaijuEntity.F_FIRING) || super.shouldRender(e, f, x, y, z);
    }

    void index(KaijuEntity e, BakedGeoModel model) {
        if (!travel.isEmpty()) return;
        KaijuSpec spec = e.spec;
        for (KaijuSpec.Plate p : spec.plates) travel.put(p.bone(), p.travel());
        Map<String, Integer> roots = new HashMap<>();
        for (int i = 0; i < spec.regionNames.size(); i++) for (String b : spec.regions.get(spec.regionNames.get(i))) roots.put(b, i);
        Map<String, Integer> sev = new HashMap<>();
        for (int g = 0; g < spec.severGroups.size(); g++) sev.put(spec.severBones.get(spec.severGroups.get(g)), g);
        for (GeoBone top : model.topLevelBones()) walk(top, -1, -1, roots, sev);
    }

    void walk(GeoBone b, int region, int sever, Map<String, Integer> roots, Map<String, Integer> sev) {
        if (roots.containsKey(b.getName())) region = roots.get(b.getName());
        if (sev.containsKey(b.getName())) sever = sev.get(b.getName());
        if (b.getName().startsWith("plate")) region = -1;
        regionOf.put(b.getName(), region);
        severOf.put(b.getName(), sever);
        for (GeoBone c : b.getChildBones()) walk(c, region, sever, roots, sev);
    }

    @Override
    public void actuallyRender(PoseStack ps, KaijuEntity e, BakedGeoModel model, RenderType rt, MultiBufferSource buf, VertexConsumer vc,
                               boolean re, float pt, int light, int overlay, float r, float g, float b, float a) {
        current = e;
        index(e, model);
        super.actuallyRender(ps, e, model, rt, buf, vc, re, pt, light, overlay, r, g, b, a);
    }

    @Override
    public void renderCubesOfBone(PoseStack ps, GeoBone bone, VertexConsumer vc, int light, int overlay, float r, float g, float b, float a) {
        KaijuEntity e = current;
        switch (pass) {
            case NORMAL -> super.renderCubesOfBone(ps, bone, vc, light, overlay, r, g, b, a);
            case GLOW -> {
                float v = glow(e, bone);
                if (v > 0.01f) super.renderCubesOfBone(ps, bone, vc, LightTexture.FULL_BRIGHT, overlay, v, v, v, 1f);
            }
            case BURN -> {
                int reg = regionOf.getOrDefault(bone.getName(), -1);
                float h = reg < 0 || e == null ? 0 : e.heatOf(reg);
                if (h > 0.02f) super.renderCubesOfBone(ps, bone, vc, light, overlay, 1f, 1f, 1f, Math.min(1f, h * 1.3f));
            }
            case BURN_GLOW -> {
                int reg = regionOf.getOrDefault(bone.getName(), -1);
                float h = reg < 0 || e == null ? 0 : e.heatOf(reg);
                if (h > 0.05f) {
                    float flick = 0.8f + 0.2f * Mth.sin((e.tickCount + bone.getName().hashCode() % 17) * 0.4f);
                    float v = Math.min(1f, h * 1.2f) * flick;
                    super.renderCubesOfBone(ps, bone, vc, LightTexture.FULL_BRIGHT, overlay, v, v, v, 1f);
                }
            }
            case REGEN -> {
                int g2 = severOf.getOrDefault(bone.getName(), -1);
                if (g2 >= 0 && e != null && e.severedClient(g2)) {
                    float p = e.regenClient(g2);
                    if (p > 0.001f) super.renderCubesOfBone(ps, bone, vc, light, overlay, 1f, 1f, 1f, Mth.clamp(1.2f - p * 1.2f, 0, 1));
                }
            }
        }
    }

    float glow(KaijuEntity e, GeoBone bone) {
        if (e == null) return 0;
        Float tr = travel.get(bone.getName());
        if (tr != null) {
            float ext = Mth.clamp(bone.getPosY() / Math.max(0.01f, tr), 0, 1.2f);
            if (ext < 0.02f) return 0;
            float flick = 0.88f + 0.12f * Mth.sin(e.tickCount * 1.7f + bone.getName().hashCode());
            return Math.min(1f, ext) * flick * (e.flag(KaijuEntity.F_FIRING) ? 1f : 0.85f);
        }
        String n = bone.getName();
        if (n.equals("head") || n.equals("jaw") || n.startsWith("tongue")) {
            float c = e.flag(KaijuEntity.F_FIRING) ? 1f : e.flag(KaijuEntity.F_CHARGING) ? e.chargeClient() * 0.8f : 0f;
            return c;
        }
        return 0;
    }

    @Override
    public void render(KaijuEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        pass = Pass.NORMAL;
        super.render(e, yaw, pt, ps, buf, light);
        pass = Pass.NORMAL;
        if (e.flag(KaijuEntity.F_FIRING)) renderBeam(e, pt, ps, buf);
        else if (e.flag(KaijuEntity.F_CHARGING)) renderCharge(e, pt, ps, buf);
        if (KaijuClient.debugNodes) DebugNodes.render(e, pt, ps, buf);
    }

    static class PassLayer extends GeoRenderLayer<KaijuEntity> {
        final Pass p;
        final KaijuRenderer r;

        PassLayer(KaijuRenderer r, Pass p) { super(r); this.r = r; this.p = p; }

        @Override
        public void render(PoseStack ps, KaijuEntity e, BakedGeoModel model, RenderType rt, MultiBufferSource buf, VertexConsumer vc, float pt, int light, int overlay) {
            boolean any = switch (p) {
                case GLOW -> e.flag(KaijuEntity.F_CHARGING) || e.flag(KaijuEntity.F_FIRING) || e.chargeClient() > 0.01f || anyPlateOut(e);
                case BURN, BURN_GLOW -> anyHeat(e);
                case REGEN -> e.severMaskClient() != 0;
                default -> false;
            };
            if (!any) return;
            RenderType type = switch (p) {
                case GLOW -> RenderType.eyes(GLOW_TEX);
                case BURN -> RenderType.entityTranslucent(BURN_TEX);
                case BURN_GLOW -> RenderType.eyes(BURN_GLOW_TEX);
                case REGEN -> RenderType.entityTranslucent(REGEN_TEX);
                default -> rt;
            };
            r.pass = p;
            getRenderer().reRender(model, ps, buf, e, type, buf.getBuffer(type), pt, light, OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);
            r.pass = Pass.NORMAL;
        }

        boolean anyHeat(KaijuEntity e) {
            for (int i = 0; i < 8; i++) if (e.heatOf(i) > 0.02f) return true;
            return false;
        }

        boolean anyPlateOut(KaijuEntity e) {
            String n = e.spec.clipNames.get(Math.max(0, Math.min(e.spec.clipNames.size() - 1, e.animIndex())));
            return n.startsWith("atomic") || n.startsWith("nuclear");
        }
    }

    // ------------------------------------------------------------------ the beam
    /** mouth position relative to the render origin, from the same pose track the server uses */
    Vec3 mouthLocal(KaijuEntity e, float pt) {
        KaijuSpec spec = e.spec;
        int idx = e.animIndex();
        if (idx < 0 || idx >= spec.clipNames.size()) return new Vec3(0, e.getBbHeight() * 0.9, 0);
        KaijuSpec.Clip c = spec.clip(spec.clipNames.get(idx));
        float[] p = new float[3];
        spec.nodePx(c, e.animTick(), spec.node("mouth"), p);
        return KaijuSpec.toWorld(p[0], p[1], p[2], Mth.rotLerp(pt, e.yRotO, e.getYRot()), e.k());
    }

    void renderBeam(KaijuEntity e, float pt, PoseStack ps, MultiBufferSource buf) {
        Vec3 a = e.beamA(), b = e.beamB();
        if (a.distanceToSqr(b) < 1) return;
        double ex = Mth.lerp(pt, e.xo, e.getX()), ey = Mth.lerp(pt, e.yo, e.getY()), ez = Mth.lerp(pt, e.zo, e.getZ());
        Vec3 o = new Vec3(ex, ey, ez);
        Vec3 la = a.subtract(o), lb = b.subtract(o);
        Vec3 ax = lb.subtract(la);
        double len = ax.length();
        Vec3 dir = ax.scale(1 / len);
        Vec3 up = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = dir.cross(up).normalize(), v = dir.cross(u).normalize();
        float t = e.tickCount + pt;
        float k = e.k() / 0.5f;
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        Matrix4f m = ps.last().pose();
        int segs = (int) Mth.clamp(len / 3.5, 4, 160);
        double segLen = len / segs;
        // layered body: haze, glow, body, white-hot core; it gathers out of the mouth over the first blocks and
        // carries bright pulses travelling away from the kaiju
        float[][] layers = {{5.6f, 0.03f, 0.22f, 0.85f, 0.22f}, {3.4f, 0.08f, 0.5f, 1f, 0.42f}, {2.0f, 0.42f, 0.85f, 1f, 0.75f}, {0.85f, 1f, 1f, 1f, 1f}};
        for (float[] L : layers) {
            for (int sI = 0; sI < segs; sI++) {
                double s0 = sI * segLen, s1 = (sI + 1) * segLen;
                float r0 = radius(L[0], s0, t, k), r1 = radius(L[0], s1, t, k);
                float p0 = pulse(s0, t), p1 = pulse(s1, t);
                Vec3 c0 = la.add(dir.scale(s0)).add(jitter(u, v, s0, t, L[0] * 0.05f * k));
                Vec3 c1 = la.add(dir.scale(s1)).add(jitter(u, v, s1, t, L[0] * 0.05f * k));
                for (int pI = 0; pI < 4; pI++) {
                    double ang = pI * Math.PI / 4 + t * 0.04;
                    Vec3 off = u.scale(Math.cos(ang)).add(v.scale(Math.sin(ang)));
                    quad2(vc, m, c0.subtract(off.scale(r0)), c0.add(off.scale(r0)), c1.add(off.scale(r1)), c1.subtract(off.scale(r1)),
                            L[1], L[2], L[3], L[4] * p0, L[4] * p1);
                }
            }
        }
        // a double helix of energy spiralling around the beam
        for (int strand = 0; strand < 2; strand++) {
            Vec3 prev = null;
            for (int sI = 0; sI <= segs; sI++) {
                double sd = sI * segLen;
                double phi = sd * 0.22 - t * 0.9 + strand * Math.PI;
                float R = radius(2.9f, sd, t, k) * 1.15f;
                Vec3 pnt = la.add(dir.scale(sd)).add(u.scale(Math.cos(phi) * R)).add(v.scale(Math.sin(phi) * R));
                if (prev != null) ribbon(vc, m, prev, pnt, dir, 0.32f * k, 0.6f, 0.95f, 1f, 0.75f);
                prev = pnt;
            }
        }
        // shock rings rolling out from the mouth along the beam
        for (int j = 0; j < 4; j++) {
            double sd = ((t * 4.5 + j * len / 4) % Math.max(1, len));
            float age = (float) (sd / Math.max(1, len));
            float R = (2.5f + 4f * age) * k;
            ring(vc, m, la.add(dir.scale(sd)), u, v, R, 0.35f * k, 0.5f, 0.9f, 1f, 0.55f * (1 - age));
        }
        // forked lightning crawling over the beam and lashing out at the impact
        java.util.Random rnd = new java.util.Random((e.getId() * 31L) + e.tickCount / 2);
        for (int j = 0; j < 7; j++) {
            double sd = j < 4 ? len * (0.85 + rnd.nextDouble() * 0.15) : rnd.nextDouble() * len;
            Vec3 pnt = la.add(dir.scale(sd));
            Vec3 d0 = u.scale(rnd.nextGaussian()).add(v.scale(rnd.nextGaussian())).add(dir.scale(rnd.nextGaussian() * 0.5)).normalize();
            bolt(vc, m, pnt, d0, (j < 4 ? 9f : 5f) * k, rnd);
        }
        // the flare where it lands and at the mouth
        boolean clash = e.flag(KaijuEntity.F_CLASH);
        float flare = (clash ? 26f : 12f) * k * (0.85f + 0.15f * Mth.sin(t * 3));
        star(vc, m, lb, u, v, flare, 0.35f, 0.75f, 1f, 0.55f);
        star(vc, m, lb, u, v, flare * 0.55f, 0.85f, 0.97f, 1f, 0.9f);
        star(vc, m, la, u, v, 5.5f * k * (0.9f + 0.1f * Mth.sin(t * 5)), 0.5f, 0.9f, 1f, 0.85f);
        star(vc, m, la, u, v, 2.6f * k, 1f, 1f, 1f, 1f);
    }

    static float radius(float base, double s, float t, float k) {
        float gather = (float) Math.min(1, 0.25 + s / 10.0);         // narrow at the lips, full width a few blocks out
        float pulse = 1 + 0.13f * Mth.sin((float) (s * 0.3 - t * 1.7)); // bulges travelling outward
        return base * k * gather * pulse;
    }

    static float pulse(double s, float t) { return 0.72f + 0.28f * Mth.sin((float) (s * 0.3 - t * 1.7)); }

    static Vec3 jitter(Vec3 u, Vec3 v, double s, float t, float amp) {
        return u.scale(Mth.sin((float) (s * 0.9 + t * 2.3)) * amp).add(v.scale(Mth.cos((float) (s * 0.7 + t * 1.9)) * amp));
    }

    static void ribbon(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 dir, float w, float r, float g, float bl, float al) {
        Vec3 side = b.subtract(a).cross(dir.cross(new Vec3(0.3, 1, 0.2))).normalize().scale(w);
        if (!Double.isFinite(side.x)) return;
        quad2(vc, m, a.subtract(side), a.add(side), b.add(side), b.subtract(side), r, g, bl, al, al);
        Vec3 side2 = side.cross(b.subtract(a)).normalize().scale(w);
        quad2(vc, m, a.subtract(side2), a.add(side2), b.add(side2), b.subtract(side2), r, g, bl, al, al);
    }

    static void ring(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 u, Vec3 v, float R, float w, float r, float g, float bl, float al) {
        int n = 18;
        for (int i = 0; i < n; i++) {
            double a0 = i * Math.PI * 2 / n, a1 = (i + 1) * Math.PI * 2 / n;
            Vec3 d0 = u.scale(Math.cos(a0)).add(v.scale(Math.sin(a0))), d1 = u.scale(Math.cos(a1)).add(v.scale(Math.sin(a1)));
            quad2(vc, m, c.add(d0.scale(R - w)), c.add(d0.scale(R + w)), c.add(d1.scale(R + w)), c.add(d1.scale(R - w)), r, g, bl, al, al);
        }
    }

    static void bolt(VertexConsumer vc, Matrix4f m, Vec3 from, Vec3 d, float len, java.util.Random rnd) {
        Vec3 p = from;
        int n = 5;
        for (int i = 0; i < n; i++) {
            Vec3 step = d.scale(len / n).add(new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).scale(len / n * 0.45));
            Vec3 q = p.add(step);
            ribbon(vc, m, p, q, d, 0.18f, 0.75f, 0.95f, 1f, 0.95f - i * 0.12f);
            if (i == 2 && rnd.nextBoolean()) bolt(vc, m, q, d.add(new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).scale(0.8)).normalize(), len * 0.45f, rnd);
            p = q;
        }
    }

    /** the charge: a ball of blue light swelling in the mouth, spitting sparks */
    void renderCharge(KaijuEntity e, float pt, PoseStack ps, MultiBufferSource buf) {
        float c = e.chargeClient();
        if (c < 0.35f) return;
        float k = e.k() / 0.5f;
        float g = (c - 0.35f) / 0.65f;
        float t = e.tickCount + pt;
        Vec3 mouth = mouthLocal(e, pt);
        float yr = Mth.rotLerp(pt, e.yRotO, e.getYRot()) * Mth.DEG_TO_RAD;
        Vec3 fwd = new Vec3(-Mth.sin(yr), 0, Mth.cos(yr));
        Vec3 u = fwd.cross(new Vec3(0, 1, 0)).normalize(), v = fwd.cross(u).normalize();
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        Matrix4f m = ps.last().pose();
        float R = (0.6f + 2.6f * g) * k * (0.9f + 0.1f * Mth.sin(t * 2.2f));
        star(vc, m, mouth, u, v, R * 1.8f, 0.2f, 0.55f, 1f, 0.35f * g);
        star(vc, m, mouth, u, v, R, 0.6f, 0.92f, 1f, 0.8f * g);
        star(vc, m, mouth, u, v, R * 0.45f, 1f, 1f, 1f, g);
        java.util.Random rnd = new java.util.Random(e.getId() * 17L + e.tickCount / 2);
        for (int j = 0; j < (int) (2 + g * 4); j++)
            bolt(vc, m, mouth, new Vec3(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).normalize(), R * 2.2f, rnd);
    }

    static void quad2(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float r, float g, float bl, float a0, float a1) {
        vc.vertex(m, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, a0).endVertex();
        vc.vertex(m, (float) b.x, (float) b.y, (float) b.z).color(r, g, bl, a0).endVertex();
        vc.vertex(m, (float) c.x, (float) c.y, (float) c.z).color(r, g, bl, a1).endVertex();
        vc.vertex(m, (float) d.x, (float) d.y, (float) d.z).color(r, g, bl, a1).endVertex();
    }

    static void star(VertexConsumer vc, Matrix4f m, Vec3 c, Vec3 u, Vec3 v, float r, float cr, float cg, float cb, float ca) {
        for (int i = 0; i < 4; i++) {
            double ang = i * Math.PI / 4;
            Vec3 a = u.scale(Math.cos(ang) * r).add(v.scale(Math.sin(ang) * r));
            Vec3 b = u.scale(-Math.sin(ang) * r).add(v.scale(Math.cos(ang) * r));
            Vec3 w = u.cross(v).scale(r * 0.4);
            quad(vc, m, c.subtract(a).subtract(w), c.add(b), c.add(a).add(w), c.subtract(b), cr, cg, cb, ca);
        }
    }

    static void quad(VertexConsumer vc, Matrix4f m, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float r, float g, float bl, float al) {
        vc.vertex(m, (float) a.x, (float) a.y, (float) a.z).color(r, g, bl, al).endVertex();
        vc.vertex(m, (float) b.x, (float) b.y, (float) b.z).color(r, g, bl, al).endVertex();
        vc.vertex(m, (float) c.x, (float) c.y, (float) c.z).color(r, g, bl, al * 0.7f).endVertex();
        vc.vertex(m, (float) d.x, (float) d.y, (float) d.z).color(r, g, bl, al * 0.7f).endVertex();
    }
}
