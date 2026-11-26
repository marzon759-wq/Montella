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
        MinecraftForge.EVENT_BUS.addListener(ClientCompat::drawSea);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.client.event.CustomizeGuiOverlayEvent.Chat e) ->
                com.krolasyon.bloodsea.client.StoryHud.render(e.getGuiGraphics(), e.getPartialTick(), e.getWindow().getGuiScaledWidth(), e.getWindow().getGuiScaledHeight()));
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent.Post e) -> { com.krolasyon.bloodsea.client.AutoTest.tick(); com.krolasyon.bloodsea.client.WatcherClient.tick(); com.krolasyon.bloodsea.client.SeaClient.tick(); com.krolasyon.bloodsea.client.StoryHud.tick(); com.krolasyon.bloodsea.client.MusicClient.tick(); com.krolasyon.bloodsea.client.OmenClient.tick(); });
    }

    /** the swell of the blood, drawn over the blood blocks before the entities so they sink into it */
    private static void drawSea(net.minecraftforge.client.event.RenderLevelStageEvent e) {
        if (e.getStage() != net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) return;
        int n = com.krolasyon.bloodsea.client.SeaSurface.build(e.getCamera(), e.getPartialTick());
        if (n == 0) return;
        float[] d = com.krolasyon.bloodsea.client.SeaSurface.data();
        Matrix4f m = e.getPoseStack();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < n; i++) {
            int o = i * 7;
            bb.addVertex(m, d[o], d[o + 1], d[o + 2]).setColor(d[o + 3], d[o + 4], d[o + 5], 1f);
        }
        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.enableCull();
    }

    static final ResourceLocation SUN = Compat.rl("textures/environment/pale_sun.png");
    static final ResourceLocation MOON = Compat.rl("textures/environment/blood_moon.png");
    static final ResourceLocation GLOW = Compat.rl("textures/environment/glow.png");
    static final ResourceLocation VANILLA_SUN = Compat.rl("minecraft", "textures/environment/sun.png");
    static final ResourceLocation WHITE_SUN = Compat.rl("textures/environment/white_sun.png");
    static final ResourceLocation SEAM = Compat.rl("textures/environment/sun_seam.png");
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
            // the Pale Sun is the Watcher: veins across the sky, a halo, and an eye that blinks and looks about
            float eo = com.krolasyon.bloodsea.client.WatcherClient.openness(pt);
            float stare = com.krolasyon.bloodsea.client.WatcherClient.stare(pt) * eo;
            float pu = com.krolasyon.bloodsea.client.WatcherClient.pupil(pt);
            float lx = com.krolasyon.bloodsea.client.WatcherClient.lookX(pt), ly = com.krolasyon.bloodsea.client.WatcherClient.lookY(pt);
            final float E = 44f;
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderTexture(0, VEINS);
            float up = (float) Math.max(0, Math.cos(level.getSunAngle(pt)) + 0.15);
            RenderSystem.setShaderColor(1f, 1f, 1f, eo * Math.min(1f, up * 2f) * (0.5f + 0.45f * stare));
            rect(m, 0, 0, 250, 250, 100);
            RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderTexture(0, GLOW);
            RenderSystem.setShaderColor(1f, 0.97f - 0.03f * eo, 0.94f - 0.04f * eo, 0.32f + 0.18f * eo + 0.2f * stare);
            quad(m, 110, 100, true, false);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            // the white sun, exactly the eye's size: until the story opens it, it is only a sun
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.setShaderTexture(0, WHITE_SUN);
            rect(m, 0, 0, E, E, 100);
            // when it opens, it opens like an eyelid: a crack across the middle, then the white parts from it
            float raw = eo * (1f - com.krolasyon.bloodsea.client.WatcherClient.blink(pt));
            float o = raw * raw * (3 - 2 * raw);
            if (eo > 0.002f) {
                float h = E * o;
                if (h > 0.25f) {
                    RenderSystem.setShaderTexture(0, EYE);
                    rectUV(m, 0, 0, E, h, 100, 0, 0.5f - o / 2, 1, 0.5f + o / 2);
                    // the iris glows
                    RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
                    RenderSystem.setShaderColor(1f, 1f, 1f, 0.2f + 0.2f * stare);
                    rectUV(m, 0, 0, E, h, 100, 0, 0.5f - o / 2, 1, 0.5f + o / 2);
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderTexture(0, PUPIL);
                    float ph = Math.min(E * 0.62f, h * 0.92f);
                    rect(m, lx * E, Math.max(-h + ph, Math.min(h - ph, ly * E)), E * 0.42f * (0.5f + 0.5f * pu), ph, 100);
                }
                // the lid edges, dark and wet, where the white has split
                float cw = (float) Math.sqrt(Math.max(0, E * E - h * h)) * 0.99f;
                RenderSystem.setShaderTexture(0, SEAM);
                RenderSystem.setShaderColor(1f, 1f, 1f, Math.min(1f, eo * 5f));
                rect(m, 0, -h, cw, 1.5f, 100);
                rect(m, 0, h, cw, 1.5f, 100);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
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

        private static void rectUV(Matrix4f m, float cx, float cz, float hx, float hz, float y, float u0, float v0, float u1, float v1) {
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bb.addVertex(m, cx - hx, y, cz - hz).setUv(u0, v0);
            bb.addVertex(m, cx + hx, y, cz - hz).setUv(u1, v0);
            bb.addVertex(m, cx + hx, y, cz + hz).setUv(u1, v1);
            bb.addVertex(m, cx - hx, y, cz + hz).setUv(u0, v1);
            BufferUploader.drawWithShader(bb.buildOrThrow());
        }

        /** a textured rectangle centred at (cx, cz) on the plane y, half sizes hx, hz */
        private static void rect(Matrix4f m, float cx, float cz, float hx, float hz, float y) {
            BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bb.addVertex(m, cx - hx, y, cz - hz).setUv(0, 0);
            bb.addVertex(m, cx + hx, y, cz - hz).setUv(1, 0);
            bb.addVertex(m, cx + hx, y, cz + hz).setUv(1, 1);
            bb.addVertex(m, cx - hx, y, cz + hz).setUv(0, 1);
            BufferUploader.drawWithShader(bb.buildOrThrow());
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
