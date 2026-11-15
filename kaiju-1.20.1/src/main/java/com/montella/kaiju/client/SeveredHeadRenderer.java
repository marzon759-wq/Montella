package com.montella.kaiju.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.montella.kaiju.entity.SeveredHeadEntity;
import com.montella.kaiju.rig.Skeleton;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class SeveredHeadRenderer extends EntityRenderer<SeveredHeadEntity> {
    private final Skeleton skel = new Skeleton();
    private final Mesh mesh;
    private final ResourceLocation[] mats;
    private final ResourceLocation wound;
    private final Mesh.Out out = new Mesh.Out();

    public SeveredHeadRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        skel.add("n", null, 0, 0, 0);
        skel.add("h", "n", 0, 0, 0.9f);
        skel.add("j", "h", 0, -0.42f, 0.55f);
        mesh = GhidorahMesh.severed(skel);
        String[] names = {"skin", "belly", "wing", "horn", "teeth", "mouth", "eye"};
        mats = new ResourceLocation[names.length];
        for (int i = 0; i < names.length; i++) mats[i] = TitanRenderer.tex("ghidorah", names[i]);
        wound = TitanRenderer.tex("ghidorah", "wound");
        shadowRadius = 1.2f;
    }

    @Override public ResourceLocation getTextureLocation(SeveredHeadEntity e) { return mats[0]; }

    @Override
    public void render(SeveredHeadEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        skel.resetPose();
        float twitch = e.tickCount < 200 ? Math.max(0, Mth.sin((e.tickCount + pt) * 0.9f)) * 0.5f : 0;
        skel.get("j").rx = 0.5f + twitch;
        skel.compute();
        ps.pushPose();
        ps.translate(0, 1.0, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(pt, e.prevSpin, e.spin)));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, e.prevRoll, e.roll)));
        ps.scale(1.25f, 1.25f, 1.25f);
        out.pose = ps.last(); out.light = light; out.overlay = OverlayTexture.NO_OVERLAY;
        out.r = out.g = out.b = out.a = 1;
        for (int m = 0; m < mats.length; m++) { out.vc = buf.getBuffer(RenderType.entityCutoutNoCull(mats[m])); mesh.render(skel, m, out, v -> true); }
        float fade = Math.max(0, 1 - e.tickCount / 600f);
        if (fade > 0) {
            out.light = LightTexture.FULL_BRIGHT;
            out.r = fade; out.g = 0.7f * fade; out.b = 0.3f * fade;
            out.vc = buf.getBuffer(RenderType.eyes(wound));
            mesh.renderGlow(skel, GhidorahMesh.G_WOUND, 0, out, v -> true);
            out.vc = buf.getBuffer(RenderType.eyes(TitanRenderer.tex("ghidorah", "eye_glow")));
            out.r = 0.6f * fade; out.g = 0.25f * fade; out.b = 0.1f * fade;
            mesh.renderGlow(skel, GhidorahMesh.G_EYE, 0, out, v -> true);
        }
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }
}
