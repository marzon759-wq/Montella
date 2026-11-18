package com.krolasyon.livingmotion.client.layer;

import com.krolasyon.livingmotion.client.model.MobKind;
import com.krolasyon.livingmotion.client.model.MobRenderer;
import com.krolasyon.livingmotion.client.model.ReplacedMob;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeableArmorItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraftforge.client.ForgeHooksClient;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;
import software.bernie.geckolib.util.RenderUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Held items, armour (with dye, trims and glint) and head items for the humanoid remodels.
 * Each piece is drawn at its bone's pivot in vanilla model space, so vanilla armour/item transforms apply 1:1
 * while following the GeckoLib animation of that bone.
 */
public class EquipmentLayer extends GeoRenderLayer<ReplacedMob> {
    private static final Map<String, ResourceLocation> ARMOR_CACHE = new HashMap<>();
    private final MobRenderer<?> mob;
    private final HumanoidModel<LivingEntity> inner, outer;
    private final ItemInHandRenderer items;
    private final TextureAtlas trims;
    private final Map<SkullBlock.Type, SkullModelBase> skulls;
    private final float handShift;

    public EquipmentLayer(MobRenderer<?> renderer, EntityRendererProvider.Context ctx, MobKind kind) {
        super(renderer);
        this.mob = renderer;
        boolean skel = kind.family == MobKind.Family.SKELETON;
        this.inner = new HumanoidModel<>(ctx.bakeLayer(skel ? ModelLayers.SKELETON_INNER_ARMOR : ModelLayers.ZOMBIE_INNER_ARMOR));
        this.outer = new HumanoidModel<>(ctx.bakeLayer(skel ? ModelLayers.SKELETON_OUTER_ARMOR : ModelLayers.ZOMBIE_OUTER_ARMOR));
        this.items = ctx.getItemInHandRenderer();
        this.trims = ctx.getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET);
        this.skulls = SkullBlockRenderer.createSkullRenderers(ctx.getModelSet());
        this.handShift = skel ? 1f / 16f : 0f;   // vanilla SkeletonModel.translateToHand nudges the thin arm
    }

    @Override
    public void renderForBone(PoseStack ps, ReplacedMob animatable, GeoBone bone, RenderType renderType,
                              MultiBufferSource buffers, VertexConsumer buffer, float partialTick, int light, int overlay) {
        LivingEntity e = mob.getCurrentEntity();
        if (e == null) return;
        String n = bone.getName();
        boolean drew = false;
        switch (n) {
            case "skull" -> {
                ItemStack head = e.getItemBySlot(EquipmentSlot.HEAD);
                if (!head.isEmpty()) {
                    enter(ps, bone);
                    if (head.getItem() instanceof ArmorItem) {
                        armor(ps, buffers, e, head, EquipmentSlot.HEAD, light, outer.head, outer.hat);
                    } else {
                        customHead(ps, buffers, e, head, light);
                    }
                    ps.popPose();
                    drew = true;
                }
            }
            case "body" -> drew = piece(ps, bone, buffers, e, light, EquipmentSlot.CHEST, outer.body)
                    | piece(ps, bone, buffers, e, light, EquipmentSlot.LEGS, inner.body);
            case "right_arm" -> drew = piece(ps, bone, buffers, e, light, EquipmentSlot.CHEST, outer.rightArm)
                    | hand(ps, bone, buffers, e, HumanoidArm.RIGHT, light);
            case "left_arm" -> drew = piece(ps, bone, buffers, e, light, EquipmentSlot.CHEST, outer.leftArm)
                    | hand(ps, bone, buffers, e, HumanoidArm.LEFT, light);
            case "right_leg" -> drew = piece(ps, bone, buffers, e, light, EquipmentSlot.LEGS, inner.rightLeg)
                    | piece(ps, bone, buffers, e, light, EquipmentSlot.FEET, outer.rightLeg);
            case "left_leg" -> drew = piece(ps, bone, buffers, e, light, EquipmentSlot.LEGS, inner.leftLeg)
                    | piece(ps, bone, buffers, e, light, EquipmentSlot.FEET, outer.leftLeg);
            default -> {}
        }
        if (drew) buffers.getBuffer(renderType);   // resume GeckoLib's batch
    }

    /** Move to the bone pivot and switch to vanilla model space (y down, x mirrored). */
    private static void enter(PoseStack ps, GeoBone bone) {
        ps.pushPose();
        RenderUtils.translateToPivotPoint(ps, bone);
        ps.scale(-1f, -1f, 1f);
    }

    private boolean piece(PoseStack ps, GeoBone bone, MultiBufferSource buffers, LivingEntity e, int light,
                          EquipmentSlot slot, ModelPart part) {
        ItemStack stack = e.getItemBySlot(slot);
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem ai) || ai.getEquipmentSlot() != slot) return false;
        enter(ps, bone);
        armor(ps, buffers, e, stack, slot, light, part);
        ps.popPose();
        return true;
    }

    private void armor(PoseStack ps, MultiBufferSource buffers, LivingEntity e, ItemStack stack, EquipmentSlot slot,
                       int light, ModelPart... parts) {
        if (!(stack.getItem() instanceof ArmorItem ai) || ai.getEquipmentSlot() != slot) return;
        boolean innerModel = slot == EquipmentSlot.LEGS;
        for (ModelPart p : parts) {
            p.setPos(0, 0, 0);
            p.setRotation(0, 0, 0);
            p.visible = true;
        }
        if (ai instanceof DyeableArmorItem dye) {
            int c = dye.getColor(stack);
            float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, b = (c & 255) / 255f;
            draw(ps, buffers.getBuffer(RenderType.armorCutoutNoCull(tex(e, stack, slot, null))), light, r, g, b, parts);
            draw(ps, buffers.getBuffer(RenderType.armorCutoutNoCull(tex(e, stack, slot, "overlay"))), light, 1, 1, 1, parts);
        } else {
            draw(ps, buffers.getBuffer(RenderType.armorCutoutNoCull(tex(e, stack, slot, null))), light, 1, 1, 1, parts);
        }
        ArmorTrim.getTrim(e.level().registryAccess(), stack).ifPresent(trim -> {
            TextureAtlasSprite sprite = trims.getSprite(innerModel ? trim.innerTexture(ai.getMaterial()) : trim.outerTexture(ai.getMaterial()));
            draw(ps, sprite.wrap(buffers.getBuffer(Sheets.armorTrimsSheet())), light, 1, 1, 1, parts);
        });
        if (stack.hasFoil()) draw(ps, buffers.getBuffer(RenderType.armorEntityGlint()), light, 1, 1, 1, parts);
    }

    private static void draw(PoseStack ps, VertexConsumer vc, int light, float r, float g, float b, ModelPart... parts) {
        for (ModelPart p : parts) p.render(ps, vc, light, OverlayTexture.NO_OVERLAY, r, g, b, 1f);
    }

    private static ResourceLocation tex(LivingEntity e, ItemStack stack, EquipmentSlot slot, String type) {
        ArmorItem item = (ArmorItem) stack.getItem();
        String texture = item.getMaterial().getName();
        String domain = "minecraft";
        int idx = texture.indexOf(':');
        if (idx != -1) {
            domain = texture.substring(0, idx);
            texture = texture.substring(idx + 1);
        }
        String path = String.format(Locale.ROOT, "%s:textures/models/armor/%s_layer_%d%s.png", domain, texture,
                slot == EquipmentSlot.LEGS ? 2 : 1, type == null ? "" : "_" + type);
        path = ForgeHooksClient.getArmorTexture(e, stack, path, slot, type);
        return ARMOR_CACHE.computeIfAbsent(path, ResourceLocation::new);
    }

    private boolean hand(PoseStack ps, GeoBone bone, MultiBufferSource buffers, LivingEntity e, HumanoidArm arm, int light) {
        boolean main = e.getMainArm() == arm;
        ItemStack stack = main ? e.getMainHandItem() : e.getOffhandItem();
        if (stack.isEmpty()) return false;
        boolean left = arm == HumanoidArm.LEFT;
        enter(ps, bone);
        ps.translate(left ? -handShift : handShift, 0, 0);
        ps.mulPose(Axis.XP.rotationDegrees(-90f));
        ps.mulPose(Axis.YP.rotationDegrees(180f));
        ps.translate((left ? -1 : 1) / 16f, 0.125f, -0.625f);
        items.renderItem(e, stack, left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                left, ps, buffers, light);
        ps.popPose();
        return true;
    }

    private void customHead(PoseStack ps, MultiBufferSource buffers, LivingEntity e, ItemStack stack, int light) {
        if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof AbstractSkullBlock skull) {
            GameProfile profile = null;
            CompoundTag tag = stack.getTag();
            if (tag != null && tag.contains("SkullOwner", 10)) profile = NbtUtils.readGameProfile(tag.getCompound("SkullOwner"));
            SkullBlock.Type type = skull.getType();
            SkullModelBase model = skulls.get(type);
            if (model == null) return;
            ps.scale(1.1875f, -1.1875f, -1.1875f);
            ps.translate(-0.5, 0, -0.5);
            SkullBlockRenderer.renderSkull(null, 180f, 0f, ps, buffers, light, model, SkullBlockRenderer.getRenderType(type, profile));
        } else {
            ps.translate(0, -0.25, 0);
            ps.mulPose(Axis.YP.rotationDegrees(180f));
            ps.scale(0.625f, -0.625f, -0.625f);
            items.renderItem(e, stack, ItemDisplayContext.HEAD, false, ps, buffers, light);
        }
    }
}
