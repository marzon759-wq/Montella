package com.krolasyon.livingmotion.client.model;

import com.krolasyon.livingmotion.LMConfig;
import com.krolasyon.livingmotion.client.layer.EquipmentLayer;
import com.krolasyon.livingmotion.client.layer.CarriedBlockLayer;
import com.krolasyon.livingmotion.client.layer.OverlayLayer;
import com.krolasyon.livingmotion.client.layer.ShellLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

/**
 * Renders a vanilla mob with its GeckoLib remodel. Holds the vanilla renderer too, so the remodel can be switched
 * off per mob in the config at any time without a restart.
 */
public class MobRenderer<E extends LivingEntity> extends GeoReplacedEntityRenderer<E, ReplacedMob> {
    private static final ResourceLocation SPIDER_EYES = new ResourceLocation("textures/entity/spider_eyes.png");
    private static final ResourceLocation CREEPER_ARMOR = new ResourceLocation("textures/entity/creeper/creeper_armor.png");

    private final EntityRenderer<E> vanilla;
    private final String id;
    public final MobKind kind;

    public MobRenderer(EntityRendererProvider.Context ctx, EntityType<E> type, String id, MobKind kind, EntityRenderer<E> vanilla) {
        super(ctx, new MobModel(kind), new ReplacedMob(type, kind));
        this.vanilla = vanilla;
        this.id = id;
        this.kind = kind;
        this.shadowRadius = kind.shadow;
        if (kind.scale != 1f) withScale(kind.scale);
        if (kind.humanoid()) addRenderLayer(new EquipmentLayer(this, ctx, kind));
        if (kind.overlay != null && kind != MobKind.SHEEP) {
            addRenderLayer(new ShellLayer(this, e -> true, (e, pt) -> RenderType.entityCutoutNoCull(kind.overlay), false));
        }
        if (kind.family == MobKind.Family.CREEPER) {
            addRenderLayer(new ShellLayer(this, e -> e instanceof Creeper c && c.isPowered(), (e, pt) -> {
                float f = e.tickCount + pt;
                return RenderType.energySwirl(CREEPER_ARMOR, (f * 0.01f) % 1f, (f * 0.01f) % 1f);
            }, true));
        }
        if (kind.family == MobKind.Family.SPIDER) addRenderLayer(new OverlayLayer(this, e -> List.of(SPIDER_EYES), true));
        if (kind == MobKind.SHEEP) {
            addRenderLayer(new ShellLayer(this, e -> e instanceof Sheep s && !s.isSheared(),
                    (e, pt) -> RenderType.entityCutoutNoCull(kind.overlay), false, MobRenderer::woolColor));
        }
        if (kind == MobKind.IRON_GOLEM) addRenderLayer(new OverlayLayer(this, MobRenderer::cracks, false));
        if (kind == MobKind.ENDERMAN) {
            addRenderLayer(new OverlayLayer(this, e -> List.of(ENDERMAN_EYES), true));
            addRenderLayer(new CarriedBlockLayer(this, ctx));
        }
        if (kind == MobKind.VILLAGER) addRenderLayer(new OverlayLayer(this, MobRenderer::villagerOutfit, false));
    }

    private static final ResourceLocation ENDERMAN_EYES = new ResourceLocation("textures/entity/enderman/enderman_eyes.png");
    private static final Map<String, ResourceLocation> TEX = new HashMap<>();
    private static final String[] LEVELS = {"stone", "iron", "gold", "emerald", "diamond"};

    private static ResourceLocation tex(String ns, String path) {
        return TEX.computeIfAbsent(ns + ":" + path, k -> new ResourceLocation(ns, path));
    }

    /** Vanilla SheepFurLayer colours, including the jeb_ rainbow. */
    private static float[] woolColor(LivingEntity e, float pt) {
        Sheep s = (Sheep) e;
        if (s.hasCustomName() && "jeb_".equals(s.getName().getString())) {
            int n = s.tickCount / 25 + s.getId();
            int k = DyeColor.values().length;
            float f = ((s.tickCount % 25) + pt) / 25f;
            float[] a = Sheep.getColorArray(DyeColor.byId(n % k)), b = Sheep.getColorArray(DyeColor.byId((n + 1) % k));
            return new float[]{a[0] * (1 - f) + b[0] * f, a[1] * (1 - f) + b[1] * f, a[2] * (1 - f) + b[2] * f};
        }
        return Sheep.getColorArray(s.getColor());
    }

    private static List<ResourceLocation> cracks(LivingEntity e) {
        if (!(e instanceof IronGolem g)) return List.of();
        String c = switch (g.getCrackiness()) {
            case LOW -> "low";
            case MEDIUM -> "medium";
            case HIGH -> "high";
            default -> null;
        };
        return c == null ? List.of() : List.of(tex("minecraft", "textures/entity/iron_golem/iron_golem_crackiness_" + c + ".png"));
    }

    /** Vanilla VillagerProfessionLayer: biome outfit, then profession, then the badge for the trade level. */
    private static List<ResourceLocation> villagerOutfit(LivingEntity e) {
        if (!(e instanceof Villager v) || v.isInvisible()) return List.of();
        VillagerData d = v.getVillagerData();
        List<ResourceLocation> out = new ArrayList<>(3);
        ResourceLocation type = BuiltInRegistries.VILLAGER_TYPE.getKey(d.getType());
        out.add(tex(type.getNamespace(), "textures/entity/villager/type/" + type.getPath() + ".png"));
        if (!v.isBaby() && d.getProfession() != VillagerProfession.NONE) {
            ResourceLocation prof = BuiltInRegistries.VILLAGER_PROFESSION.getKey(d.getProfession());
            out.add(tex(prof.getNamespace(), "textures/entity/villager/profession/" + prof.getPath() + ".png"));
            if (d.getProfession() != VillagerProfession.NITWIT) {
                out.add(tex("minecraft", "textures/entity/villager/profession_level/" + LEVELS[Mth.clamp(d.getLevel(), 1, 5) - 1] + ".png"));
            }
        }
        return out;
    }

    public MobModel mobModel() { return (MobModel) getGeoModel(); }

    private boolean active() { return LMConfig.mob(id); }

    @Override
    public void render(E entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light) {
        if (!active()) { vanilla.render(entity, yaw, partialTick, poseStack, buffers, light); return; }
        super.render(entity, yaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public boolean shouldRender(E entity, Frustum frustum, double x, double y, double z) {
        return active() ? super.shouldRender(entity, frustum, x, y, z) : vanilla.shouldRender(entity, frustum, x, y, z);
    }

    @Override
    public Vec3 getRenderOffset(E entity, float partialTick) {
        return active() ? super.getRenderOffset(entity, partialTick) : vanilla.getRenderOffset(entity, partialTick);
    }

    @Override
    public void preRender(PoseStack poseStack, ReplacedMob animatable, BakedGeoModel model, MultiBufferSource buffers,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int light, int overlay,
                          float r, float g, float b, float a) {
        super.preRender(poseStack, animatable, model, buffers, buffer, isReRender, partialTick, light, overlay, r, g, b, a);
        if (isReRender || currentEntity == null) return;
        if (currentEntity.isBaby()) poseStack.scale(0.5f, 0.5f, 0.5f);
        if (currentEntity instanceof Creeper c) {   // vanilla creeper swelling, same formula
            float f = c.getSwelling(partialTick);
            float wobble = 1f + Mth.sin(f * 100f) * f * 0.01f;
            f = Mth.clamp(f, 0f, 1f);
            f = f * f * f * f;
            float xz = (1f + f * 0.4f) * wobble, y = (1f + f * 0.1f) / wobble;
            poseStack.scale(xz, y, xz);
        }
    }

    @Override
    public int getPackedOverlay(ReplacedMob animatable, float u, float partialTick) {
        if (currentEntity instanceof Creeper c) {
            float f = c.getSwelling(partialTick);
            u = (int) (f * 10f) % 2 == 0 ? 0f : Mth.clamp(f, 0.5f, 1f);
        }
        return OverlayTexture.pack(OverlayTexture.u(u),
                OverlayTexture.v(currentEntity.hurtTime > 0 || currentEntity.deathTime > 0));
    }

    @Override
    public boolean isShaking(ReplacedMob animatable) {
        if (currentEntity == null) return false;
        if (currentEntity.isFullyFrozen()) return true;
        if (currentEntity instanceof Zombie z && z.isUnderWaterConverting()) return true;
        return currentEntity instanceof AbstractSkeleton s && s.isShaking();
    }

    /** Same as GeckoLib's, minus the vanilla "tip over" death rotation: the death animations do that properly. */
    @Override
    protected void applyRotations(ReplacedMob animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        LivingEntity e = currentEntity;
        if (isShaking(animatable)) rotationYaw += (float) (Math.cos(e.tickCount * 3.25d) * Math.PI * 0.4d);
        if (e.getPose() != Pose.SLEEPING) poseStack.mulPose(Axis.YP.rotationDegrees(180f - rotationYaw));
        if (e.hasCustomName()) {
            String name = ChatFormatting.stripFormatting(e.getName().getString());
            if ("Dinnerbone".equals(name) || "Grumm".equals(name)) {
                poseStack.translate(0, e.getBbHeight() + 0.1f, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
            }
        }
    }
}
