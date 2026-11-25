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

/** Client glue: Forge 1.20.1 - the sky of the Blood Sea */
public final class ClientCompat {
    private ClientCompat() {}

    public static void init(IEventBus bus) {
        bus.addListener((RegisterDimensionSpecialEffectsEvent e) -> e.register(Compat.rl("blood_sea"), new SeaSky()));
        com.krolasyon.bloodsea.client.AutoTest.init();
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent e) -> {
            if (e.phase == net.minecraftforge.event.TickEvent.Phase.END) { com.krolasyon.bloodsea.client.AutoTest.tick(); com.krolasyon.bloodsea.client.WatcherClient.tick(); }
        });
    }

    static final ResourceLocation SUN = Compat.rl("textures/environment/pale_sun.png");
    static final ResourceLocation MOON = Compat.rl("textures/environment/blood_moon.png");
    static final ResourceLocation GLOW = Compat.rl("textures/environment/glow.png");
    static final ResourceLocation EYE = Compat.rl("textures/environment/watcher_eye.png");
    static final ResourceLocation PUPIL = Compat.rl("textures/environment/watcher_pupil.png");
    static final ResourceLocation LID = Compat.rl("textures/environment/watcher_lid.png");
    static final ResourceLocation VEINS = Compat.rl("textures/environment/watcher_veins.png");

    public static class SeaSky extends DimensionSpecialEffects {
        public SeaSky() { super(Float.NaN, true, SkyType.NORMAL, false, false); }

        @Override
        public Vec3 getBrightnessDependentFogColor(Vec3 c, float b) { return c.multiply(b * 0.9 + 0.1, b * 0.9 + 0.08, b * 0.9 + 0.08); }

        @Override public boolean isFoggyAt(int x, int z) { return false; }

        @Override
        public boolean renderClouds(ClientLevel level, int ticks, float pt, PoseStack ps, double x, double y, double z, Matrix4f proj) { return true; }

        @Override
        public boolean renderSky(ClientLevel level, int ticks, float pt, PoseStack ps, Camera camera, Matrix4f proj, boolean foggy, Runnable setupFog) {
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
            // the Pale Sun is the Watcher: veins across the sky, a halo, and an eye that blinks and looks about
            float blink = com.krolasyon.bloodsea.client.WatcherClient.blink(pt), stare = com.krolasyon.bloodsea.client.WatcherClient.stare(pt);
            float pu = com.krolasyon.bloodsea.client.WatcherClient.pupil(pt);
            float lx = com.krolasyon.bloodsea.client.WatcherClient.lookX(pt), ly = com.krolasyon.bloodsea.client.WatcherClient.lookY(pt);
            final float E = 44f;
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, VEINS);
            float up = (float) Math.max(0, Math.cos(level.getSunAngle(pt)) + 0.15);
            RenderSystem.setShaderColor(1f, 1f, 1f, Math.min(1f, up * 2f) * (0.5f + 0.45f * stare));
            rect(m, 0, 0, 250, 250, 100);
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderTexture(0, GLOW);
            RenderSystem.setShaderColor(1f, 0.96f, 0.93f, 0.5f + 0.2f * stare);
            quad(m, 110, 100, true, false);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.setShaderTexture(0, LID);
            rect(m, 0, 0, E * 1.04f, E * 1.04f, 100);
            float open = 1f - blink;
            if (open > 0.02f) {
                RenderSystem.setShaderTexture(0, EYE);
                rect(m, 0, 0, E, E * open, 100);
                // the iris glows
                RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
                RenderSystem.setShaderColor(1f, 1f, 1f, 0.2f + 0.2f * stare);
                rect(m, 0, 0, E, E * open, 100);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderTexture(0, PUPIL);
                rect(m, lx * E, ly * E * open, E * 0.42f * (0.5f + 0.5f * pu), E * 0.62f * open, 100);
            }
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

        /** a textured rectangle centred at (cx, cz) on the plane y, half sizes hx, hz */
        private static void rect(Matrix4f m, float cx, float cz, float hx, float hz, float y) {
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bb.vertex(m, cx - hx, y, cz - hz).uv(0, 0).endVertex();
            bb.vertex(m, cx + hx, y, cz - hz).uv(1, 0).endVertex();
            bb.vertex(m, cx + hx, y, cz + hz).uv(1, 1).endVertex();
            bb.vertex(m, cx - hx, y, cz + hz).uv(0, 1).endVertex();
            BufferUploader.drawWithShader(bb.end());
        }

        private static void quad(Matrix4f m, float s, float y, boolean tex, boolean flip) {
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, tex ? DefaultVertexFormat.POSITION_TEX : DefaultVertexFormat.POSITION);
            float[][] v = flip ? new float[][]{{-s, s, 0, 1}, {s, s, 1, 1}, {s, -s, 1, 0}, {-s, -s, 0, 0}}
                    : new float[][]{{-s, -s, 0, 0}, {s, -s, 1, 0}, {s, s, 1, 1}, {-s, s, 0, 1}};
            for (float[] p : v) {
                if (tex) bb.vertex(m, p[0], y, p[1]).uv(p[2], p[3]).endVertex();
                else bb.vertex(m, p[0], y, p[1]).endVertex();
            }
            BufferUploader.drawWithShader(bb.end());
        }
    }
}
