package com.krolasyon.beyblade.compat;

import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class VClient {
   private VClient() {
   }

   public static void vertex(
      VertexConsumer vc,
      Matrix4f m,
      @Nullable Matrix3f n,
      float x,
      float y,
      float z,
      float r,
      float g,
      float b,
      float a,
      float u,
      float v,
      int overlay,
      int light,
      float nx,
      float ny,
      float nz
   ) {
      vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(overlay).uv2(light);
      if (n != null) {
         vc.normal(n, nx, ny, nz).endVertex();
      } else {
         vc.normal(nx, ny, nz).endVertex();
      }
   }

   public static float partialTick() {
      return Minecraft.getInstance().getFrameTime();
   }

   public static void background(net.minecraft.client.gui.screens.Screen screen, net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partial) {
      screen.renderBackground(g);
   }

   public static void createFlatTestWorld(Minecraft mc, String name, net.minecraft.world.level.LevelSettings settings, net.minecraft.world.level.levelgen.WorldOptions options) {
      mc.createWorldOpenFlows().createFreshLevel(name, settings, options, reg -> reg.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
         .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
   }

   /** Draws a 2D overlay on top of the finished frame (after the world and the HUD), with the GUI projection. */
   public static void overlay(java.util.function.Consumer<net.minecraft.client.gui.GuiGraphics> draw) {
      Minecraft mc = Minecraft.getInstance();
      com.mojang.blaze3d.platform.Window w = mc.getWindow();
      mc.getMainRenderTarget().bindWrite(false);
      Matrix4f proj = new Matrix4f().setOrtho(0.0F, (float)((double)w.getWidth() / w.getGuiScale()), (float)((double)w.getHeight() / w.getGuiScale()), 0.0F, 1000.0F, 21000.0F);
      com.mojang.blaze3d.systems.RenderSystem.backupProjectionMatrix();
      com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(proj, com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
      com.mojang.blaze3d.vertex.PoseStack mv = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
      mv.pushPose();
      mv.setIdentity();
      mv.translate(0.0, 0.0, -11000.0);
      com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
      com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);
      net.minecraft.client.gui.GuiGraphics g = new net.minecraft.client.gui.GuiGraphics(mc, mc.renderBuffers().bufferSource());
      draw.accept(g);
      g.flush();
      mv.popPose();
      com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
      com.mojang.blaze3d.systems.RenderSystem.restoreProjectionMatrix();
   }
}
