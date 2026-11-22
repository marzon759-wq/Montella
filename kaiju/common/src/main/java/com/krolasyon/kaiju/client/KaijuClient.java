package com.krolasyon.kaiju.client;

import com.krolasyon.kaiju.KaijuMod;
import com.krolasyon.kaiju.entity.KaijuEntity;
import com.krolasyon.kaiju.net.ControlInput;
import com.krolasyon.kaiju.net.Net;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.*;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.List;

public final class KaijuClient {
    private KaijuClient() {}

    static final String CAT = "key.categories.kaiju";
    public static final KeyMapping BREATH = key("breath", GLFW.GLFW_KEY_R);
    public static final KeyMapping GRAB = key("grab", GLFW.GLFW_KEY_G);
    public static final KeyMapping TAIL = key("tail", GLFW.GLFW_KEY_V);
    public static final KeyMapping BITE = key("bite", GLFW.GLFW_KEY_C);
    public static final KeyMapping PULSE = key("pulse", GLFW.GLFW_KEY_N);
    public static final KeyMapping ROAR = key("roar", GLFW.GLFW_KEY_B);
    public static final KeyMapping DODGE = key("dodge", GLFW.GLFW_KEY_LEFT_ALT);
    public static final KeyMapping SPECIAL = key("special", GLFW.GLFW_KEY_Y);
    public static final KeyMapping WHEEL = key("wheel", GLFW.GLFW_KEY_Z);
    public static final KeyMapping AI = key("ai", GLFW.GLFW_KEY_H);
    public static final KeyMapping DEBUG = key("debug", GLFW.GLFW_KEY_F7);
    public static final KeyMapping RELEASE = key("release", GLFW.GLFW_KEY_K);

    public static boolean debugNodes;
    static float shake, flash;
    static boolean guardHeld;

    static KeyMapping key(String n, int code) { return new KeyMapping("key.kaiju." + n, InputConstants.Type.KEYSYM, code, CAT); }

    public static void init(IEventBus mod) {
        mod.addListener((EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerEntityRenderer(KaijuMod.ATOMIC.get(), ctx -> new KaijuRenderer(ctx, new KaijuModel()));
            e.registerEntityRenderer(KaijuMod.DEBRIS.get(), DebrisRenderer::new);
            e.registerEntityRenderer(KaijuMod.SEVERED.get(), SeveredPartRenderer::new);
        });
        mod.addListener((RegisterKeyMappingsEvent e) -> {
            for (KeyMapping k : new KeyMapping[]{BREATH, GRAB, TAIL, BITE, PULSE, ROAR, DODGE, SPECIAL, WHEEL, AI, DEBUG, RELEASE}) e.register(k);
        });
        mod.addListener((RegisterGuiOverlaysEvent e) -> e.registerAbove(VanillaGuiOverlay.BOSS_EVENT_PROGRESS.id(), "kaiju_hud",
                (gui, g, pt, w, h) -> hud(g, pt, w, h)));
        Net.CLIENT_FX = fx -> {
            Minecraft mc = Minecraft.getInstance();
            if (fx.type() == Net.Fx.SHAKE) shake = Math.min(6f, shake + fx.strength());
            else if (fx.type() == Net.Fx.FLASH) flash = Math.min(1f, flash + fx.strength());
        };
        com.krolasyon.kaiju.entity.KaijuClientHooks.TICK = KaijuClient::entityTick;
        MinecraftForge.EVENT_BUS.addListener(KaijuClient::clientTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, KaijuClient::interact);
        MinecraftForge.EVENT_BUS.addListener(KaijuClient::movement);
        MinecraftForge.EVENT_BUS.addListener(KaijuClient::hidePlayer);
        MinecraftForge.EVENT_BUS.addListener(KaijuClient::hideHand);
        MinecraftForge.EVENT_BUS.addListener(KaijuClient::camera);
        MinecraftForge.EVENT_BUS.addListener(KaijuClient::fog);
        AutoTest.init();
    }

    static KaijuEntity riding() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() instanceof KaijuEntity k ? k : null;
    }

    static void entityTick(KaijuEntity e) {
        // footstep dust and steam for kaiju near the camera are spawned by the server; nothing per-entity needed yet
    }

    static void clientTick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        shake *= 0.86f;
        flash *= 0.9f;
        Minecraft mc = Minecraft.getInstance();
        while (DEBUG.consumeClick()) debugNodes = !debugNodes;
        KaijuEntity k = riding();
        if (k == null || mc.player == null) return;
        while (WHEEL.consumeClick()) mc.setScreen(new AttackWheelScreen(k));
        while (RELEASE.consumeClick()) Net.sendSelect("__release__", false);
        if (mc.screen != null) return;
        ControlInput in = new ControlInput();
        in.forward = mc.player.input.forwardImpulse;
        in.strafe = mc.player.input.leftImpulse;
        in.yaw = mc.player.getYRot();
        in.pitch = mc.player.getXRot();
        int b = 0;
        if (mc.options.keyAttack.isDown()) b |= ControlInput.ATTACK;
        if (mc.options.keyUse.isDown() && !holdingController()) b |= ControlInput.HEAVY;
        if (BREATH.isDown()) b |= ControlInput.BREATH;
        if (GRAB.isDown()) b |= ControlInput.GRAB;
        if (TAIL.isDown()) b |= ControlInput.TAIL;
        if (BITE.isDown()) b |= ControlInput.BITE;
        if (PULSE.isDown()) b |= ControlInput.PULSE;
        if (ROAR.isDown()) b |= ControlInput.ROAR;
        if (DODGE.isDown()) b |= ControlInput.DODGE;
        if (SPECIAL.isDown()) b |= ControlInput.SPECIAL;
        if (AI.isDown()) b |= ControlInput.AI;
        if (guardHeld) b |= ControlInput.GUARD;
        if (mc.options.keyJump.isDown()) b |= ControlInput.JUMP;
        if (mc.options.keySprint.isDown()) b |= ControlInput.SPRINT;
        in.buttons = b;
        Net.sendControl(in);
    }

    /** riding a kaiju: mouse buttons drive the kaiju, not the player's hands */
    static boolean holdingController() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && (mc.player.getMainHandItem().is(KaijuMod.CONTROLLER.get()) || mc.player.getOffhandItem().is(KaijuMod.CONTROLLER.get()));
    }

    static void interact(InputEvent.InteractionKeyMappingTriggered e) {
        // holding the controller, right-click still uses it: that is how you let go of the kaiju
        if (riding() != null && !(e.isUseItem() && holdingController())) {
            e.setCanceled(true);
            e.setSwingHand(false);
        }
    }

    /** sneak would dismount; while riding a kaiju it means "guard" instead */
    static void movement(MovementInputUpdateEvent e) {
        if (riding() != null) {
            guardHeld = e.getInput().shiftKeyDown;
            e.getInput().shiftKeyDown = false;
        } else guardHeld = false;
    }

    static void hidePlayer(RenderPlayerEvent.Pre e) {
        if (e.getEntity().getVehicle() instanceof KaijuEntity) e.setCanceled(true);
    }

    static void hideHand(RenderHandEvent e) {
        if (riding() != null) e.setCanceled(true);
    }

    static void camera(ViewportEvent.ComputeCameraAngles e) {
        if (shake > 0.02f) {
            float t = (float) (e.getPartialTick() + Minecraft.getInstance().level.getGameTime());
            e.setPitch(e.getPitch() + Mth.sin(t * 2.1f) * shake * 0.9f);
            e.setYaw(e.getYaw() + Mth.sin(t * 1.7f + 1) * shake * 0.7f);
            e.setRoll(e.getRoll() + Mth.sin(t * 2.9f + 2) * shake * 0.5f);
        }
    }

    static void fog(ViewportEvent.RenderFog e) {
        if (riding() != null && e.getFarPlaneDistance() < 400) {
            e.setFarPlaneDistance(Math.max(e.getFarPlaneDistance(), 300));
            e.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ HUD
    static void hud(GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (flash > 0.01f) g.fill(0, 0, w, h, ((int) (Math.min(1, flash) * 230) << 24) | 0xEAF6FF);
        List<KaijuEntity> near = mc.level.getEntitiesOfClass(KaijuEntity.class, mc.player.getBoundingBox().inflate(600), x -> !x.flag(KaijuEntity.F_DEAD));
        near.sort(Comparator.comparingDouble(x -> x.distanceToSqr(mc.player)));
        KaijuEntity mine = riding();
        int y = 4;
        int shown = 0;
        for (KaijuEntity k : near) {
            if (shown >= 3) break;
            boolean me = k == mine;
            int bw = 200;
            int x = w / 2 - bw / 2;
            String name = Component.translatable("entity.kaiju.atomic_kaiju").getString() + (me ? "  ★" : "");
            g.drawCenteredString(mc.font, name, w / 2, y, me ? 0x9FE8FF : 0xFFB0A0);
            y += 10;
            g.fill(x - 1, y - 1, x + bw + 1, y + 6, 0xAA000000);
            g.fill(x, y, x + (int) (bw * Mth.clamp(k.hpRatio(), 0, 1)), y + 5, me ? 0xFF2EB8E6 : 0xFFD8402A);
            y += 7;
            g.fill(x, y, x + (int) (bw * Mth.clamp(k.poiseClient() / 100f, 0, 1)), y + 2, 0xFFE6D27A);
            y += 6;
            shown++;
        }
        if (mine == null) return;
        // own status
        int bx = 8, by = h - 74;
        float charge = mine.chargeClient();
        String st = mine.flag(KaijuEntity.F_FIRING) ? "§b" + tr("hud.kaiju.firing") : mine.flag(KaijuEntity.F_CHARGING) ? "§3" + tr("hud.kaiju.charging") + " " + (int) (charge * 100) + "%" : "";
        g.drawString(mc.font, st, bx, by, 0xFFFFFF);
        g.drawString(mc.font, tr("hud.kaiju.special") + ": §e" + tr("attack.kaiju." + mine.special), bx, by + 11, 0xDDDDDD);
        StringBuilder burns = new StringBuilder();
        for (int i = 0; i < 8; i++) if (mine.heatOf(i) > 0.15f) burns.append(tr("region.kaiju." + mine.spec.regionNames.get(i))).append(' ');
        if (burns.length() > 0) g.drawString(mc.font, "§6" + tr("hud.kaiju.burns") + ": " + burns, bx, by + 22, 0xFFFFFF);
        StringBuilder sev = new StringBuilder();
        for (int gI = 0; gI < mine.spec.severGroups.size(); gI++)
            if (mine.severedClient(gI)) sev.append(tr("part.kaiju." + mine.spec.severGroups.get(gI))).append(' ').append((int) (mine.regenClient(gI) * 100)).append("%  ");
        if (sev.length() > 0) g.drawString(mc.font, "§c" + tr("hud.kaiju.regen") + ": " + sev, bx, by + 33, 0xFFFFFF);
        if (mine.flag(KaijuEntity.F_AI_ASSIST)) g.drawString(mc.font, "§a" + tr("hud.kaiju.ai"), bx, by + 44, 0xFFFFFF);
        g.drawString(mc.font, "§7" + tr("hud.kaiju.keys"), bx, h - 12, 0xAAAAAA);
    }

    static String tr(String k) { return Component.translatable(k).getString(); }
}
