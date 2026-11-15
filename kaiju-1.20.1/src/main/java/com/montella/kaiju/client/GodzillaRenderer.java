package com.montella.kaiju.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.montella.kaiju.entity.GodzillaEntity;
import com.montella.kaiju.entity.S;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class GodzillaRenderer extends TitanRenderer<GodzillaEntity> {
    public GodzillaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "godzilla", new String[]{"skin", "belly", "plate", "teeth", "mouth", "eye", "claw"}, new String[]{"plate_glow", "eye_glow"});
    }

    @Override protected Mesh build(GodzillaEntity e) { return GodzillaMesh.build(e.skel); }

    @Override protected int glowGroups(int g) { return g == GodzillaMesh.G_PLATE ? GodzillaMesh.PLATE_GROUPS : 1; }

    @Override protected float[] glowColor(int g) { return g == GodzillaMesh.G_PLATE ? new float[]{0.55f, 0.9f, 1f} : new float[]{1f, 0.85f, 0.5f}; }

    @Override
    protected float glowLevel(GodzillaEntity e, int g, int grp, float pt) {
        if (g == GodzillaMesh.G_EYE) return 0.35f + 0.5f * e.beamPower();
        return e.spineGlow(grp, 22, pt);
    }

    @Override
    protected void effects(GodzillaEntity e, float pt, PoseStack ps, MultiBufferSource buf) {
        int st = e.state();
        float age = e.tickCount + pt;
        Vector3f mouth = rel(e, "head", 0, -0.3f, 3.6f, pt);
        if (st == S.CHARGE_BEAM) {
            float c = Mth.clamp(e.stateTime(pt) / 34f, 0, 1);
            Beams.glow(buf.getBuffer(RenderType.lightning()), ps.last().pose(), mouth, 1.5f * c, 0.3f, 0.7f, 1f, 0.5f * c);
        }
        float p = e.beamPower();
        if (p > 0.01f) Beams.atomic(ps, buf, mouth, relWorld(e, e.beamEnd(), pt), p, age, e.clashing());
        if (st == S.PULSE) {
            float t = e.stateTime(pt);
            if (t > 26) {
                float k = (t - 26) / 16f;
                if (k < 1) {
                    Beams.ring(ps, buf, 4 + k * 34, 2, 3f, 0.4f, 0.8f, 1f, 0.9f * (1 - k));
                    Beams.ring(ps, buf, 2 + k * 24, 7, 2f, 0.7f, 0.9f, 1f, 0.7f * (1 - k));
                    Beams.glow(buf.getBuffer(RenderType.lightning()), ps.last().pose(), new Vector3f(0, 8, 0), 18 * (1 - k * 0.5f), 0.5f, 0.85f, 1f, 0.6f * (1 - k));
                }
            }
        }
    }
}
