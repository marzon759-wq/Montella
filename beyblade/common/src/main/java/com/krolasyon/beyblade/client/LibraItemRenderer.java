package com.krolasyon.beyblade.client;

import com.krolasyon.beyblade.compat.VClient;
import com.krolasyon.beyblade.parts.Loadout;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class LibraItemRenderer extends BlockEntityWithoutLevelRenderer {
   private static final Map<String, LibraItemRenderer> CACHE = new HashMap<>();
   private final BeyMesh mesh;

   public static LibraItemRenderer get(String kind) {
      return CACHE.computeIfAbsent(kind, k -> new LibraItemRenderer(BeyMesh.of(k)));
   }

   private LibraItemRenderer(BeyMesh mesh) {
      super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
      this.mesh = mesh;
   }

   public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buf, int light, int overlay) {
      Minecraft mc = Minecraft.getInstance();
      float time = (float)(mc.level != null ? mc.level.getGameTime() : 0L) + VClient.partialTick();
      pose.pushPose();
      pose.translate(0.5, 0.5, 0.5);

      float s = switch (ctx) {
         case GUI -> {
            pose.mulPose(Axis.XP.rotationDegrees(38.0F));
            pose.mulPose(Axis.YP.rotationDegrees(time * 1.5F));
            yield 0.43F;
         }
         case GROUND -> 0.35F;
         case FIXED -> {
            pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            yield 0.5F;
         }
         case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
            pose.translate(0.05, 0.2, -0.1);
            pose.mulPose(Axis.XP.rotationDegrees(20.0F));
            pose.mulPose(Axis.YP.rotationDegrees(time * 0.8F));
            yield 0.19F;
         }
         case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
            pose.translate(0.0, 0.12, 0.05);
            pose.mulPose(Axis.XP.rotationDegrees(-60.0F));
            yield 0.2F;
         }
         case HEAD -> {
            pose.translate(0.0, 0.3, 0.0);
            yield 0.5F;
         }
         default -> 0.4F;
      };
      pose.scale(s, s, s);
      Loadout parts = Loadout.of(stack);
      BeyMesh m = BeyMesh.forLoadout(parts, this.mesh);
      pose.translate(0.0, (double)(-(m.HEIGHT + m.lift(parts))) * 0.5, 0.0);
      m.renderAssembled(pose, buf, light, overlay, parts);
      pose.popPose();
   }
}
