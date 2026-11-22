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
    void renderBeam(KaijuEntity e, float pt, PoseStack ps, MultiBufferSource buf) {
        Vec3 a = e.beamA(), b = e.beamB();
        if (a.distanceToSqr(b) < 1) return;
        double ex = Mth.lerp(pt, e.xo, e.getX()), ey = Mth.lerp(pt, e.yo, e.getY()), ez = Mth.lerp(pt, e.zo, e.getZ());
        Vec3 o = new Vec3(ex, ey, ez);
        Vec3 la = a.subtract(o), lb = b.subtract(o);
        Vec3 dir = lb.subtract(la).normalize();
        Vec3 up = Math.abs(dir.y) > 0.9 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = dir.cross(up).normalize(), v = dir.cross(u).normalize();
        float t = (e.tickCount + pt);
        float k = e.k() / 0.5f;
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        Matrix4f m = ps.last().pose();
        // outer haze, body, hot core (additive)
        float[][] layers = {{4.6f, 0.05f, 0.25f, 0.9f, 0.35f}, {2.9f, 0.15f, 0.55f, 1f, 0.6f}, {1.6f, 0.55f, 0.9f, 1f, 0.8f}, {0.7f, 1f, 1f, 1f, 1f}};
        for (float[] L : layers) {
            float w = L[0] * k * (0.92f + 0.08f * Mth.sin(t * 2.3f + L[0]));
            for (int i = 0; i < 4; i++) {
                double ang = i * Math.PI / 4 + t * 0.05;
                Vec3 off = u.scale(Math.cos(ang) * w).add(v.scale(Math.sin(ang) * w));
                quad(vc, m, la.subtract(off), la.add(off), lb.add(off.scale(1.25)), lb.subtract(off.scale(1.25)), L[1], L[2], L[3], L[4]);
            }
        }
        // the clash / impact flare and the mouth glow
        float flare = (e.flag(KaijuEntity.F_CLASH) ? 22f : 10f) * k * (0.85f + 0.15f * Mth.sin(t * 3));
        star(vc, m, lb, u, v, flare, 0.6f, 0.9f, 1f, 0.8f);
        star(vc, m, la, u, v, 3.5f * k, 0.6f, 0.95f, 1f, 0.9f);
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
