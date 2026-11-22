package com.krolasyon.kaiju.client;

import com.krolasyon.kaiju.KaijuMod;
import com.krolasyon.kaiju.data.KaijuSpec;
import com.krolasyon.kaiju.entity.SeveredPartEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import java.util.HashSet;
import java.util.Set;

/** draws only the severed subtree of the kaiju model (an arm or the tail tip), tumbling about its stump */
public class SeveredPartRenderer extends GeoEntityRenderer<SeveredPartEntity> {
    public SeveredPartRenderer(EntityRendererProvider.Context c) {
        super(c, new PartModel());
        shadowRadius = 0;
    }

    static String rootOf(SeveredPartEntity e) {
        KaijuSpec s = KaijuSpec.get(e.specId());
        int g = Math.max(0, Math.min(s.severGroups.size() - 1, e.group()));
        return s.severBones.get(s.severGroups.get(g));
    }

    @Override
    public void scaleModelForRender(float w, float h, PoseStack ps, SeveredPartEntity e, BakedGeoModel model, boolean re, float pt, int light, int overlay) {
        super.scaleModelForRender(w * e.scale(), h * e.scale(), ps, e, model, re, pt, light, overlay);
    }

    @Override
    protected void applyRotations(SeveredPartEntity e, PoseStack ps, float age, float yaw, float pt) {
        super.applyRotations(e, ps, age, yaw, pt);
        GeoBone root = getGeoModel().getBone(rootOf(e)).orElse(null);
        if (root == null) return;
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, e.prevRoll, e.roll)));
        ps.translate(-root.getPivotX() / 16f, -root.getPivotY() / 16f + 0.3f, -root.getPivotZ() / 16f);
    }

    static class PartModel extends GeoModel<SeveredPartEntity> {
        static String base(SeveredPartEntity e) { return KaijuSpec.get(e.specId()).model; }
        @Override public ResourceLocation getModelResource(SeveredPartEntity e) { return KaijuModel.geo(base(e)); }
        @Override public ResourceLocation getTextureResource(SeveredPartEntity e) { return KaijuModel.tex(base(e)); }
        @Override public ResourceLocation getAnimationResource(SeveredPartEntity e) { return KaijuModel.anim(base(e)); }

        @Override
        public void setCustomAnimations(SeveredPartEntity e, long id, AnimationState<SeveredPartEntity> st) {
            String root = rootOf(e);
            BakedGeoModel m = getBakedModel(getModelResource(e));
            Set<String> keep = new HashSet<>();
            getBone(root).ifPresent(b -> collect(b, keep));
            for (GeoBone top : m.topLevelBones()) hideOthers(top, keep);
        }

        static void collect(GeoBone b, Set<String> out) {
            out.add(b.getName());
            for (GeoBone c : b.getChildBones()) collect(c, out);
        }

        static void hideOthers(GeoBone b, Set<String> keep) {
            b.setHidden(!keep.contains(b.getName()));
            b.setChildrenHidden(false);
            for (GeoBone c : b.getChildBones()) hideOthers(c, keep);
        }
    }
}
