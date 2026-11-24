package com.krolasyon.bloodsea.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix4f;

/** Client glue: Forge 1.21.1 - the sky of the Blood Sea */
public final class ClientCompat {
    private ClientCompat() {}

    public static void init(IEventBus bus) {
        bus.addListener((RegisterDimensionSpecialEffectsEvent e) -> e.register(Compat.rl("blood_sea"), new SeaSky()));
        com.krolasyon.bloodsea.client.AutoTest.init();
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent.Post e) -> com.krolasyon.bloodsea.client.AutoTest.tick());
    }

    static final ResourceLocation SUN = Compat.rl("textures/environment/pale_sun.png");
    static final ResourceLocation MOON = Compat.rl("textures/environment/blood_moon.png");
    static final ResourceLocation GLOW = Compat.rl("textures/environment/glow.png");

    public static class SeaSky extends DimensionSpecialEffects {
        public SeaSky() { super(Float.NaN, true, SkyType.NORMAL, false, false); }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 c, float b) { return c.multiply(b * 0.9 + 0.1, b * 0.9 + 0.08, b * 0.9 + 0.08); }

        @Override public boolean isFoggyAt(int x, int z) { return false; }

        @Override
        public boolean renderClouds(ClientLevel level, int ticks, float pt, PoseStack ps, double x, double y, double z, Matrix4f proj) { return true; }

        @Override
        public boolean renderSky(ClientLevel level, int ticks, float pt, Camera camera, Matrix4f modelView, boolean foggy, Runnable setupFog) {
            PoseStack ps = new PoseStack();
            ps.mulPose(modelView);
            setupFog.run();
            Vec3 sky = level.getSkyColor(camera.getPosition(), pt);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            // the dome
            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderSystem.setShaderColor((float) sky.x, (float) sky.y, (float) sky.z, 1f);
            quad(ps.last().pose(), 400, 16, false, false);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(-90f));
            ps.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(pt) * 360f));
            Matrix4f m = ps.last().pose();
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            // the Pale Sun: white, too big, too bright - a halo first, then the disc
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderTexture(0, GLOW);
            RenderSystem.setShaderColor(1f, 0.97f, 0.95f, 0.55f);
            quad(m, 95, 100, true, false);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.setShaderTexture(0, SUN);
            quad(m, 38, 100, true, false);
            // the Blood Moon: a red halo and a red face
            RenderSystem.setShaderTexture(0, GLOW);
            RenderSystem.setShaderColor(0.9f, 0.05f, 0.02f, 0.65f);
            quad(m, 90, -100, true, true);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, MOON);
            quad(m, 34, -100, true, true);
            ps.popPose();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            return true;
        }

        private static void quad(Matrix4f m, float s, float y, boolean tex, boolean flip) {
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, tex ? DefaultVertexFormat.POSITION_TEX : DefaultVertexFormat.POSITION);
            float[][] v = flip ? new float[][]{{-s, s, 0, 1}, {s, s, 1, 1}, {s, -s, 1, 0}, {-s, -s, 0, 0}}
                    : new float[][]{{-s, -s, 0, 0}, {s, -s, 1, 0}, {s, s, 1, 1}, {-s, s, 0, 1}};
            for (float[] p : v) {
                if (tex) bb.addVertex(m, p[0], y, p[1]).setUv(p[2], p[3]);
                else bb.addVertex(m, p[0], y, p[1]);
            }
            BufferUploader.drawWithShader(bb.buildOrThrow());
        }
    }
}
