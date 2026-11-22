package com.krolasyon.kaiju.client;

import com.krolasyon.kaiju.KaijuMod;
import com.krolasyon.kaiju.data.KaijuSpec;
import com.krolasyon.kaiju.entity.KaijuEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

/**
 * Per-frame touches on top of the animation: a severed part is hidden; while it regrows it swells out of the stump in
 * lumpy, pulsing flesh (each sub-bone throbs out of phase) until it settles at full size; a rider's aim tilts the neck.
 */
public class KaijuModel extends GeoModel<KaijuEntity> {
    static final ResourceLocation GEO = geo("kaiju"), TEX = tex("kaiju"), ANIM = anim("kaiju");

    static ResourceLocation geo(String base) { return new ResourceLocation(KaijuMod.MODID, "geo/entity/" + base + ".geo.json"); }
    static ResourceLocation tex(String base) { return new ResourceLocation(KaijuMod.MODID, "textures/entity/" + base + ".png"); }
    static ResourceLocation anim(String base) { return new ResourceLocation(KaijuMod.MODID, "animations/entity/" + base + ".animation.json"); }

    @Override public ResourceLocation getModelResource(KaijuEntity e) { return geo(e.spec.model); }
    @Override public ResourceLocation getTextureResource(KaijuEntity e) { return tex(e.spec.model); }
    @Override public ResourceLocation getAnimationResource(KaijuEntity e) { return anim(e.spec.model); }

    @Override
    public void setCustomAnimations(KaijuEntity e, long id, AnimationState<KaijuEntity> st) {
        super.setCustomAnimations(e, id, st);
        KaijuSpec spec = e.spec;
        float t = e.tickCount + st.getPartialTick();
        // baked bones are shared by every renderer of this model (the severed-part renderer hides most of them):
        // start every frame fully visible at normal scale
        for (var b : getAnimationProcessor().getRegisteredBones()) {
            b.setHidden(false);
            b.setChildrenHidden(false);
        }
        for (int g = 0; g < spec.severGroups.size(); g++) {
            GeoBone root = getBone(spec.severBones.get(spec.severGroups.get(g))).orElse(null);
            if (root == null) continue;
            if (!e.severedClient(g)) {
                root.setHidden(false);
                root.setChildrenHidden(false);
                continue;
            }
            float p = e.regenClient(g);
            if (p <= 0.001f) {
                root.setHidden(true);
                root.setChildrenHidden(true);
                continue;
            }
            root.setHidden(false);
            root.setChildrenHidden(false);
            float grow = (float) Math.pow(p, 0.75);
            float lump = (1 - p) * 0.35f;
            float s = grow * (1 + lump * Mth.sin(t * 0.5f));
            root.setScaleX(s * (1 + lump * Mth.sin(t * 0.37f + 1)));
            root.setScaleY(s);
            root.setScaleZ(s * (1 + lump * Mth.sin(t * 0.43f + 2)));
            int i = 0;
            for (GeoBone c : root.getChildBones()) {
                float ph = i++ * 1.7f;
                float cs = 1 + lump * 1.4f * Mth.sin(t * 0.6f + ph);
                c.setScaleX(cs);
                c.setScaleY(1 + lump * Mth.sin(t * 0.45f + ph));
                c.setScaleZ(cs);
            }
        }
        // the head turns toward where the breath goes (synced aim): spread over the neck and the head
        float ay = e.aimYaw(), ap = e.aimPitch();
        if (Math.abs(ay) > 0.3f || Math.abs(ap) > 0.3f) {
            float yr = ay * Mth.DEG_TO_RAD / 3f, pr = ap * Mth.DEG_TO_RAD / 3f;
            for (String n : new String[]{"neck1", "neck2", "head"}) {
                GeoBone b = getBone(n).orElse(null);
                if (b == null) continue;
                b.setRotY(b.getRotY() + yr);  // GeckoLib mirrors x, which flips the sense of yaw (not of pitch)
                b.setRotX(b.getRotX() - pr);
            }
        }
    }
}
