package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.compat.CreatureBase;
import com.krolasyon.bloodsea.compat.CreatureModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Draws any creature of the Blood Sea from its kind's model, painted texture and animations. */
public class CreatureRenderer<T extends CreatureBase> extends GeoEntityRenderer<T> {
    public CreatureRenderer(EntityRendererProvider.Context ctx, String kind, float shadow) {
        super(ctx, new CreatureModel<>(kind));
        this.shadowRadius = shadow;
    }

    /** the dead do not fall over and turn red: they have their own ways of going */
    @Override
    protected float getDeathMaxRotation(T e) { return 0f; }
}
