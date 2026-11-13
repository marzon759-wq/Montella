package com.krolasyon.hatman.client;

import com.krolasyon.hatman.compat.ClientCompat;
import com.krolasyon.hatman.compat.Compat;
import com.krolasyon.hatman.net.Msg;
import com.krolasyon.hatman.net.Net;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ClientEvents {
    public static final KeyMapping KEY_CAM = new KeyMapping("key.hatman.camera", 86, "key.categories.hatman");
    private static ResourceLocation FX_NV, FX_FILM;
    private static boolean nvEffectGiven = false;
    private static int unhideCooldown = 0;
    private static String effectWanted = "";

    private ClientEvents() {}

    public static void init(IEventBus modBus) {
        FX_NV = Compat.rl("shaders/post/nv.json");
        FX_FILM = Compat.rl("shaders/post/film.json");
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KEY_CAM));
        ClientCompat.init(modBus);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onFog);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onFogColor);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onFov);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onAngles);
        MinecraftForge.EVENT_BUS.addListener(ClientEvents::onInput);
        AutoTest.init();
    }

    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        AutoTest.tick();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            if (ClientState.active) ClientState.reset();
            return;
        }
        if (mc.isPaused()) return;
        ClientState.ticks++;
        while (KEY_CAM.consumeClick()) {
            if (ClientState.active && ClientState.hasCam && ClientState.cutId == null) Net.toServer(Msg.of("cam"));
        }
        if (ClientState.staticT > 0) ClientState.staticT--;
        if (ClientState.shakeT > 0) ClientState.shakeT--;
        if (ClientState.flashT > 0) ClientState.flashT--;
        if (ClientState.jumpT > 0) ClientState.jumpT--;
        if (ClientState.fadeInT > 0) ClientState.fadeInT--;
        if (ClientState.freezeT > 0) ClientState.freezeT--;
        if (ClientState.slatsT > 0) ClientState.slatsT--;
        if (unhideCooldown > 0) unhideCooldown--;
        ClientState.objFlash++;
        if (ClientState.chapterT >= 0 && ++ClientState.chapterT > 150) ClientState.chapterT = -1;
        if (ClientState.lineKey != null && ++ClientState.lineT > ClientState.lineDur) ClientState.lineKey = null;
        if (ClientState.hintKey != null && ++ClientState.hintT > 260) ClientState.hintKey = null;
        if (ClientState.cutId != null) ClientState.cutT++;
        ClientState.fearSmooth += (ClientState.fear - ClientState.fearSmooth) * 0.08f;
        boolean nv = ClientState.active && ClientState.nvOn;
        ClientState.nvFade += ((nv ? 1f : 0f) - ClientState.nvFade) * 0.3f;
        // night vision: a client-only potion effect lifts the light map, the shader does the rest
        if (nv) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, false));
            nvEffectGiven = true;
        } else if (nvEffectGiven) {
            p.removeEffect(MobEffects.NIGHT_VISION);
            nvEffectGiven = false;
        }
        // post processing
        String want = !ClientState.active ? "" : nv && ClientState.cutId == null ? "nv" : "film";
        try {
            PostChain cur = mc.gameRenderer.currentEffect();
            String name = cur == null ? "" : cur.getName();
            boolean ours = name.contains("hatman");
            if (want.isEmpty()) {
                if (ours) mc.gameRenderer.shutdownEffect();
            } else {
                ResourceLocation rl = want.equals("nv") ? FX_NV : FX_FILM;
                if (!name.equals(rl.toString()) && (ClientState.ticks % 5 == 0 || !want.equals(effectWanted))) mc.gameRenderer.loadEffect(rl);
            }
            effectWanted = want;
        } catch (Throwable ignored) {
            // shader packs may refuse post chains; the HUD tint still gives a usable night vision
        }
    }

    public static void renderTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof CutsceneScreen cs) cs.updateCamera(Compat.partialTick());
    }

    private static void onFog(ViewportEvent.RenderFog e) {
        if (!ClientState.active) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        float target;
        if (ClientState.cutId != null) {
            var cut = com.krolasyon.hatman.story.Cutscenes.ALL.get(ClientState.cutId);
            target = cut == null ? 40 : cut.fog;
        } else {
            int light = mc.level.getMaxLocalRawBrightness(mc.player.blockPosition().above());
            boolean outside = ClientState.amb.equals("outside");
            target = ClientState.nvOn ? 30 : outside ? 34 : light >= 9 ? 30 : light >= 5 ? 19 : 10;
            if (ClientState.fire) target = Math.min(target, 22);
        }
        ClientState.fogFar += (target - ClientState.fogFar) * 0.04f;
        e.setNearPlaneDistance(ClientState.fogFar * 0.12f);
        e.setFarPlaneDistance(ClientState.fogFar);
        e.setCanceled(true);
    }

    private static void onFogColor(ViewportEvent.ComputeFogColor e) {
        if (!ClientState.active) return;
        if (ClientState.fire) {
            e.setRed(0.16f);
            e.setGreen(0.05f);
            e.setBlue(0.02f);
        } else {
            float f = ClientState.flashT > 0 ? 0.25f : 0f;
            e.setRed(0.012f + f);
            e.setGreen(0.014f + f);
            e.setBlue(0.02f + f);
        }
    }

    private static void onFov(ViewportEvent.ComputeFov e) {
        if (!ClientState.active) return;
        if (ClientState.cutId != null && ClientState.cutFov > 0) {
            e.setFOV(ClientState.cutFov);
        } else if (ClientState.dialogue) {
            e.setFOV(e.getFOV() * 0.84);
        } else if (ClientState.freezeT > 0) {
            e.setFOV(e.getFOV() * (0.8 + 0.04 * Math.sin(ClientState.ticks * 0.3)));
        } else if (ClientState.chase) {
            e.setFOV(e.getFOV() * (1.04 + 0.01 * Math.sin(ClientState.ticks * 0.9)));
        }
    }

    private static void onAngles(ViewportEvent.ComputeCameraAngles e) {
        if (!ClientState.active) return;
        float t = ClientState.ticks + Compat.partialTick();
        float amp = 0;
        if (ClientState.shakeT > 0) amp = Math.min(2.4f, ClientState.shakeT * 0.2f);
        if (ClientState.nvOn && ClientState.cutId == null) amp += 0.12f; // hand held camera
        if (ClientState.chase) amp += 0.35f;
        amp += ClientState.fearSmooth * 0.3f;
        if (amp <= 0) return;
        e.setRoll(e.getRoll() + (float) (Math.sin(t * 1.9) * 0.6 + Math.sin(t * 4.7) * 0.4) * amp);
        e.setPitch(e.getPitch() + (float) (Math.sin(t * 2.6 + 1) * 0.5 + Math.sin(t * 6.1) * 0.3) * amp * 0.6f);
        e.setYaw(e.getYaw() + (float) (Math.sin(t * 2.2 + 2) * 0.5) * amp * 0.5f);
    }

    private static void onInput(MovementInputUpdateEvent e) {
        if (!ClientState.active) return;
        if (ClientState.hidden || ClientState.freezeT > 0) {
            boolean leave = e.getInput().shiftKeyDown || e.getInput().jumping;
            e.getInput().forwardImpulse = 0;
            e.getInput().leftImpulse = 0;
            e.getInput().jumping = false;
            e.getInput().shiftKeyDown = false;
            if (ClientState.hidden && leave && unhideCooldown == 0) {
                unhideCooldown = 20;
                Net.toServer(Msg.of("unhide"));
            }
        }
    }
}
