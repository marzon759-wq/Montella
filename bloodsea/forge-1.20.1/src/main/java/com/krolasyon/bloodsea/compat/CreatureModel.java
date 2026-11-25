package com.krolasyon.bloodsea.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/** Model, texture and animations of a creature by its kind; its look bones turn toward what it looks at (GeckoLib 4.4). */
public class CreatureModel<T extends CreatureBase> extends GeoModel<T> {
    private final ResourceLocation geo, tex, anim;

    public CreatureModel(String kind) {
        geo = Compat.rl("geo/" + kind + ".geo.json");
        tex = Compat.rl("textures/entity/" + kind + ".png");
        anim = Compat.rl("animations/" + kind + ".animation.json");
    }

    @Override public ResourceLocation getModelResource(T e) { return geo; }
    @Override public ResourceLocation getTextureResource(T e) { return tex; }
    @Override public ResourceLocation getAnimationResource(T e) { return anim; }

    @Override
    public void setCustomAnimations(T e, long id, AnimationState<T> st) {
        String[] bones = e.lookBones();
        if (bones.length == 0) return;
        EntityModelData d = st.getData(DataTickets.ENTITY_MODEL_DATA);
        if (d == null) return;
        float w = e.lookWeight();
        float yaw = Mth.clamp(d.netHeadYaw(), -80, 80) * Mth.DEG_TO_RAD * w;
        float pitch = Mth.clamp(d.headPitch(), -45, 45) * Mth.DEG_TO_RAD * w;
        for (int i = 0; i < bones.length; i++) {
            CoreGeoBone b = getAnimationProcessor().getBone(bones[i]);
            if (b == null) continue;
            b.setRotX(b.getRotX() + pitch);
            b.setRotY(b.getRotY() + yaw);
        }
    }
}
