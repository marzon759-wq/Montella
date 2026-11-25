package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.compat.Compat;
import com.krolasyon.bloodsea.entity.RaftEntity;
import com.krolasyon.bloodsea.world.Waves;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Draws the raft riding the swell: lifted and tipped by the waves under its four corners. */
public class RaftRenderer extends GeoEntityRenderer<RaftEntity> {
    public RaftRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model());
        this.shadowRadius = 0f;
    }

    @Override
    public void render(RaftEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        double t = Waves.time(e.level(), pt);
        float ry = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        double a = Math.toRadians(ry), fx = -Math.sin(a), fz = Math.cos(a), rx = -fz, rz = fx;
        double x = Mth.lerp(pt, e.xo, e.getX()), z = Mth.lerp(pt, e.zo, e.getZ());
        double hf = Waves.height(x + fx * 2, z + fz * 2, t), hb = Waves.height(x - fx * 2, z - fz * 2, t);
        double hr = Waves.height(x + rx * 2, z + rz * 2, t), hl = Waves.height(x - rx * 2, z - rz * 2, t);
        ps.pushPose();
        ps.translate(0, e.bob(pt), 0);
        // GeckoLib turns an entity that is not alive by a fixed half turn; turn the rest of the way to the raft's
        // heading, tip it on the swell in its own frame, then hand GeckoLib its half turn back
        ps.mulPose(Axis.YP.rotationDegrees(180f - ry));
        ps.mulPose(Axis.XP.rotationDegrees((float) Math.toDegrees(Math.atan2(hb - hf, 4)) * 0.8f));
        ps.mulPose(Axis.ZP.rotationDegrees((float) Math.toDegrees(Math.atan2(hr - hl, 4)) * 0.8f));
        ps.mulPose(Axis.YP.rotationDegrees(-180f));
        super.render(e, yaw, pt, ps, buf, light);
        ps.popPose();
    }

    public static class Model extends GeoModel<RaftEntity> {
        private static final ResourceLocation GEO = Compat.rl("geo/raft.geo.json"), TEX = Compat.rl("textures/entity/raft.png"),
                ANIM = Compat.rl("animations/raft.animation.json");

        @Override public ResourceLocation getModelResource(RaftEntity e) { return GEO; }
        @Override public ResourceLocation getTextureResource(RaftEntity e) { return TEX; }
        @Override public ResourceLocation getAnimationResource(RaftEntity e) { return ANIM; }
    }
}
