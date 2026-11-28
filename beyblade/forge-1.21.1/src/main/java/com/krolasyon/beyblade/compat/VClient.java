package com.krolasyon.beyblade.compat;

import com.mojang.blaze3d.vertex.VertexConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Client glue (Forge 1.21.1): the 1.21 vertex builder API. */
public final class VClient {
   private VClient() {
   }

   public static void vertex(
      VertexConsumer vc, Matrix4f m, @Nullable Matrix3f n, float x, float y, float z, float r, float g, float b, float a, float u, float v,
      int overlay, int light, float nx, float ny, float nz
   ) {
      vc.addVertex(m, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(overlay).setLight(light);
      if (n != null) {
         Vector3f t = n.transform(new Vector3f(nx, ny, nz));
         float l = t.length();
         if (l > 1.0E-6F) {
            t.div(l);
         }
         vc.setNormal(t.x, t.y, t.z);
      } else {
         vc.setNormal(nx, ny, nz);
      }
   }

   public static float partialTick() {
      return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
   }

   /** 1.21 screens draw their (blurred) background inside super.render, once per frame. */
   public static void background(net.minecraft.client.gui.screens.Screen screen, net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partial) {
   }

   public static void createFlatTestWorld(Minecraft mc, String name, net.minecraft.world.level.LevelSettings settings, net.minecraft.world.level.levelgen.WorldOptions options) {
      mc.createWorldOpenFlows().createFreshLevel(name, settings, options, reg -> reg.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
         .getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
   }

   /** Draws a 2D overlay on top of the finished frame (after the world and the HUD), with the GUI projection. */
   public static void overlay(java.util.function.Consumer<net.minecraft.client.gui.GuiGraphics> draw) {
      Minecraft mc = Minecraft.getInstance();
      com.mojang.blaze3d.platform.Window w = mc.getWindow();
      mc.getMainRenderTarget().bindWrite(false);
      Matrix4f proj = new Matrix4f().setOrtho(0.0F, (float)((double)w.getWidth() / w.getGuiScale()), (float)((double)w.getHeight() / w.getGuiScale()), 0.0F, 1000.0F, 21000.0F);
      com.mojang.blaze3d.systems.RenderSystem.backupProjectionMatrix();
      com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(proj, com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
      org.joml.Matrix4fStack mv = com.mojang.blaze3d.systems.RenderSystem.getModelViewStack();
      mv.pushMatrix();
      mv.identity();
      mv.translate(0.0F, 0.0F, -11000.0F);
      com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
      com.mojang.blaze3d.systems.RenderSystem.clear(256, Minecraft.ON_OSX);
      net.minecraft.client.gui.GuiGraphics g = new net.minecraft.client.gui.GuiGraphics(mc, mc.renderBuffers().bufferSource());
      draw.accept(g);
      g.flush();
      mv.popMatrix();
      com.mojang.blaze3d.systems.RenderSystem.applyModelViewMatrix();
      com.mojang.blaze3d.systems.RenderSystem.restoreProjectionMatrix();
   }
}
