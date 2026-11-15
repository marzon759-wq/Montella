package com.montella.kaiju.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.montella.kaiju.entity.GhidorahEntity;
import com.montella.kaiju.entity.S;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.function.IntPredicate;

public class GhidorahRenderer extends TitanRenderer<GhidorahEntity> {
    public GhidorahRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, "ghidorah", new String[]{"skin", "belly", "wing", "horn", "teeth", "mouth", "eye"}, new String[]{"crackle", "eye_glow", "wound"});
    }

    @Override protected Mesh build(GhidorahEntity e) { return GhidorahMesh.build(e.skel); }

    @Override protected int glowGroups(int g) { return 1; }

    @Override
    protected float[] glowColor(int g) {
        return switch (g) { case 0 -> new float[]{1f, 0.85f, 0.35f}; case 1 -> new float[]{1f, 0.45f, 0.15f}; default -> new float[]{1f, 0.7f, 0.3f}; };
    }

    @Override
    protected float glowLevel(GhidorahEntity e, int g, int grp, float pt) {
        if (g == GhidorahMesh.G_EYE) return 0.9f + 0.1f * Mth.sin((e.tickCount + pt) * 0.2f);
        if (g == GhidorahMesh.G_WOUND) return 0.6f + 0.3f * Mth.sin((e.tickCount + pt) * 0.4f);
        return e.crackle(pt);
    }

    @Override
    protected IntPredicate visible(GhidorahEntity e) {
        return v -> v >= GhidorahMesh.VIS_STUMP && v < GhidorahMesh.VIS_STUMP + 3 ? e.growth(v - GhidorahMesh.VIS_STUMP) < 100 : true;
    }

    @Override
    protected void effects(GhidorahEntity e, float pt, PoseStack ps, MultiBufferSource buf) {
        int st = e.state();
        float p = e.beamPower();
        Vector3f end = relWorld(e, e.beamEnd(), pt);
        for (int h = 0; h < 3; h++) {
            if (!e.headUp(h)) continue;
            Vector3f mouth = rel(e, "h" + h, 0, -0.3f, 4.0f, pt);
            if (st == S.CHARGE_BEAM) {
                float c = Mth.clamp(e.stateTime(pt) / 26f, 0, 1);
                Beams.glow(buf.getBuffer(RenderType.lightning()), ps.last().pose(), mouth, 1.2f * c, 1f, 0.85f, 0.3f, 0.5f * c);
                if (e.tickCount % 3 == h) Beams.lightning(ps, buf, mouth, new Vector3f(mouth).add((h - 1) * 2f, 2f, 1f), 0.4f * c, e.tickCount, h + 10, false);
            }
            if (p > 0.01f) Beams.lightning(ps, buf, mouth, end, p, e.tickCount / 2, h, e.clashing());
        }
    }
}
