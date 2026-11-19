package com.krolasyon.mimic.client;

import com.krolasyon.mimic.compat.ClientCompat;
import com.krolasyon.mimic.compat.Compat;
import com.krolasyon.mimic.net.Msg;
import com.krolasyon.mimic.net.Net;
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
    public static final KeyMapping KEY_LIGHT = new KeyMapping("key.mimic.light", 70, "key.categories.mimic");
    private static ResourceLocation FX_FILM;
    private static int unhideCooldown = 0;
    private static String effectWanted = "";

    private ClientEvents() {}

    public static void init(IEventBus modBus) {
        FX_FILM = Compat.rl("shaders/post/film.json");
        modBus.addListener((RegisterKeyMappingsEvent e) -> e.register(KEY_LIGHT));
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
        if (DialogueScreen.pending != null && mc.screen == null) DialogueScreen.show(DialogueScreen.pending);
        while (KEY_LIGHT.consumeClick()) {
            if (ClientState.active && ClientState.hasLight && ClientState.cutId == null) Net.toServer(Msg.of("light"));
        }
        Qte.tick();
        if (ClientState.staticT > 0) ClientState.staticT--;
        if (ClientState.shakeT > 0) ClientState.shakeT--;
        if (ClientState.flashT > 0) ClientState.flashT--;
        if (ClientState.jumpT > 0) ClientState.jumpT--;
        if (ClientState.killT > 0) ClientState.killT--;
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
        ClientState.lightFade += ((ClientState.active && ClientState.lightOn ? 1f : 0f) - ClientState.lightFade) * 0.35f;
        // post processing
        String want = !ClientState.active ? "" : "film";
        try {
            PostChain cur = mc.gameRenderer.currentEffect();
            String name = cur == null ? "" : cur.getName();
            boolean ours = name.contains("mimic");
            if (want.isEmpty()) {
                if (ours) mc.gameRenderer.shutdownEffect();
            } else {
                ResourceLocation rl = FX_FILM;
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
            var cut = com.krolasyon.mimic.story.Cutscenes.ALL.get(ClientState.cutId);
            target = cut == null ? 40 : cut.fog;
        } else {
            int light = mc.level.getMaxLocalRawBrightness(mc.player.blockPosition().above());
            boolean outside = ClientState.amb.equals("outside");
            target = ClientState.dawn ? 120 : outside ? 46 : light >= 9 ? 34 : light >= 5 ? 22 : 14;
            if (ClientState.lightOn) target = Math.max(target, 26);
        }
        ClientState.fogFar += (target - ClientState.fogFar) * 0.04f;
        e.setNearPlaneDistance(ClientState.fogFar * 0.22f);
        e.setFarPlaneDistance(ClientState.fogFar);
        e.setCanceled(true);
    }

    private static void onFogColor(ViewportEvent.ComputeFogColor e) {
        if (!ClientState.active) return;
        if (ClientState.dawn) {
            e.setRed(0.42f);
            e.setGreen(0.30f);
            e.setBlue(0.26f);
        } else {
            float f = ClientState.flashT > 0 ? 0.25f : 0f;
            boolean outside = ClientState.amb.equals("outside");
            e.setRed((outside ? 0.045f : 0.014f) + f);
            e.setGreen((outside ? 0.04f : 0.012f) + f);
            e.setBlue((outside ? 0.05f : 0.012f) + f);
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
        if (ClientState.lightOn && ClientState.cutId == null) amp += 0.05f; // a torch in a shaking hand
        if (ClientState.chase) amp += 0.35f;
        amp += ClientState.fearSmooth * 0.3f;
        if (amp <= 0) return;
        e.setRoll(e.getRoll() + (float) (Math.sin(t * 1.9) * 0.6 + Math.sin(t * 4.7) * 0.4) * amp);
        e.setPitch(e.getPitch() + (float) (Math.sin(t * 2.6 + 1) * 0.5 + Math.sin(t * 6.1) * 0.3) * amp * 0.6f);
        e.setYaw(e.getYaw() + (float) (Math.sin(t * 2.2 + 2) * 0.5) * amp * 0.5f);
    }

    private static void onInput(MovementInputUpdateEvent e) {
        if (!ClientState.active) return;
        if (ClientState.hidden || ClientState.freezeT > 0 || "breath".equals(Qte.type)) {
            boolean leave = (e.getInput().shiftKeyDown || e.getInput().jumping) && Qte.type == null;
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
