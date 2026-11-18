package com.krolasyon.livingmotion.client.model;

import com.krolasyon.livingmotion.LivingMotion;
import net.minecraft.resources.ResourceLocation;

/** Everything that differs between the remodelled vanilla mobs. */
public enum MobKind {
    ZOMBIE(Family.ZOMBIE, "zombie", "zombie/zombie", 1f, 0.5f, null),
    HUSK(Family.ZOMBIE, "zombie", "zombie/husk", 1.0625f, 0.5f, null),
    DROWNED(Family.ZOMBIE, "drowned", "zombie/drowned", 1f, 0.5f, "zombie/drowned_outer_layer"),
    SKELETON(Family.SKELETON, "skeleton", "skeleton/skeleton", 1f, 0.5f, null),
    STRAY(Family.SKELETON, "skeleton", "skeleton/stray", 1f, 0.5f, "skeleton/stray_overlay"),
    WITHER_SKELETON(Family.SKELETON, "skeleton", "skeleton/wither_skeleton", 1.2f, 0.6f, null),
    CREEPER(Family.CREEPER, "creeper", "creeper/creeper", 1f, 0.5f, null),
    SPIDER(Family.SPIDER, "spider", "spider/spider", 1f, 0.8f, null),
    CAVE_SPIDER(Family.SPIDER, "spider", "spider/cave_spider", 0.7f, 0.56f, null),
    COW(Family.QUADRUPED, "cow", "cow/cow", 1f, 0.7f, null),
    PIG(Family.QUADRUPED, "pig", "pig/pig", 1f, 0.7f, null),
    CHICKEN(Family.CHICKEN, "chicken", "chicken", 1f, 0.3f, null),
    SHEEP(Family.QUADRUPED, "sheep", "sheep/sheep", 1f, 0.7f, "sheep/sheep_fur"),
    IRON_GOLEM(Family.GOLEM, "iron_golem", "iron_golem/iron_golem", 1f, 0.7f, null),
    ENDERMAN(Family.ENDERMAN, "enderman", "enderman/enderman", 1f, 0.5f, null),
    VILLAGER(Family.VILLAGER, "villager", "villager/villager", 0.9375f, 0.5f, null),
    WANDERING_TRADER(Family.VILLAGER, "villager", "wandering_trader", 0.9375f, 0.5f, null);

    public enum Family { ZOMBIE, SKELETON, CREEPER, SPIDER, QUADRUPED, CHICKEN, GOLEM, ENDERMAN, VILLAGER }

    public final Family family;
    public final ResourceLocation geo, anim, texture;
    public final ResourceLocation overlay;
    public final float scale, shadow;
    public final String animPrefix;

    MobKind(Family family, String geo, String texture, float scale, float shadow, String overlay) {
        this.family = family;
        this.geo = new ResourceLocation(LivingMotion.MODID, "geo/" + geo + ".geo.json");
        String animFile = switch (family) {
            case ZOMBIE, SKELETON -> "humanoid";
            case CREEPER -> "creeper";
            case SPIDER -> "spider";
            case QUADRUPED -> "quadruped";
            case CHICKEN -> "chicken";
            case GOLEM -> "iron_golem";
            case ENDERMAN -> "enderman";
            case VILLAGER -> "villager";
        };
        this.animPrefix = "animation." + animFile + ".";
        this.anim = new ResourceLocation(LivingMotion.MODID, "animations/" + animFile + ".animation.json");
        this.texture = new ResourceLocation("minecraft", "textures/entity/" + texture + ".png");
        this.overlay = overlay == null ? null : new ResourceLocation("minecraft", "textures/entity/" + overlay + ".png");
        this.scale = scale;
        this.shadow = shadow;
    }

    public boolean humanoid() {
        return family == Family.ZOMBIE || family == Family.SKELETON;
    }
}
