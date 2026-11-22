package com.krolasyon.kaiju.client;

import com.krolasyon.kaiju.KaijuMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Development only (-Dkaiju.shots=1): makes a flat world, summons the kaiju, photographs it from a ring of cameras in
 * both skins, day and night, then quits. Never registered in a normal game.
 */
public final class ShotHook {
    private static final List<Object[]> cues = new ArrayList<>();
    private static int t = 0, at = 0, menu = 0;
    private static boolean created = false, built = false;
    private static final double EX = 0.5, EY = -60, EZ = 0.5;

    private ShotHook() {}

    private static void cue(int delay, Runnable r) {
        at += delay;
        cues.add(new Object[]{at, r});
    }

    private static void cmd(String c) {
        Minecraft mc = Minecraft.getInstance();
        var s = mc.getSingleplayerServer();
        if (s == null || mc.player == null) return;
        s.execute(() -> {
            ServerPlayer p = s.getPlayerList().getPlayer(mc.player.getUUID());
            if (p != null) s.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), c);
        });
    }

    /** camera on a ring around the kaiju: angle 0 = in front of its face */
    private static void view(String name, double angle, double r, double h, double th, double tz) {
        double a = Math.toRadians(angle);
        double px = EX + Math.sin(a) * r, py = EY + h, pz = EZ + Math.cos(a) * r;
        double dx = EX - px, dy = EY + th - py, dz = EZ + tz - pz;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        cue(6, () -> cmd(String.format(java.util.Locale.ROOT, "tp @s %.2f %.2f %.2f %.1f %.1f", px, py, pz, yaw, pitch)));
        cue(30, () -> shot(name));
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        KaijuMod.LOG.info("[SHOTS] {}", name);
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {});
    }

    private static void ring(String tag) {
        view(tag + "_01_front", 0, 27, 8, 9, 0);
        view(tag + "_02_front34", 38, 28, 8, 9, -3);
        view(tag + "_03_side", 90, 36, 7, 8, -7);
        view(tag + "_04_back34", 145, 32, 11, 8, -6);
        view(tag + "_05_back", 180, 30, 14, 9, 0);
        view(tag + "_06_low", 25, 20, 1.5, 12, 0);
        view(tag + "_07_head", 30, 11, 17, 16, 2);
        view(tag + "_08_fins", 120, 15, 18, 12, -3);
    }

    private static void build() {
        at = 0;
        cue(1, () -> {
            Minecraft.getInstance().options.hideGui = true;
            cmd("gamemode spectator");
            cmd("gamerule doDaylightCycle false");
            cmd("gamerule doWeatherCycle false");
            cmd("gamerule doMobSpawning false");
            cmd("time set 6000");
            cmd("weather clear");
            cmd("kill @e[type=!player]");
        });
        cue(20, () -> cmd("summon kaiju:kaiju " + EX + " " + EY + " " + EZ + " {NoAI:1b,Variant:0,Rotation:[0f,0f]}"));
        cue(60, () -> {});
        ring("atomic_day");
        cue(5, () -> cmd("data merge entity @e[type=kaiju:kaiju,limit=1] {Variant:1}"));
        ring("burning_day");
        cue(5, () -> cmd("time set 18000"));
        view("burning_night_02_front34", 38, 28, 8, 9, -3);
        view("burning_night_06_low", 25, 20, 1.5, 12, 0);
        view("burning_night_07_head", 30, 13, 17, 16, 2);
        cue(5, () -> cmd("data merge entity @e[type=kaiju:kaiju,limit=1] {Variant:0}"));
        view("atomic_night_03_side", 90, 36, 7, 8, -7);
        view("atomic_night_04_back34", 145, 32, 11, 8, -6);
        view("atomic_night_02_front34", 38, 28, 8, 9, -3);
        view("atomic_night_06_low", 25, 20, 1.5, 12, 0);
        cue(40, () -> Minecraft.getInstance().stop());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!created) {
            if ((mc.screen instanceof TitleScreen || mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) && ++menu > 40) {
                created = true;
                LevelSettings ls = new LevelSettings("kaiju_shots", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("kaiju_shots", ls, new WorldOptions(1954L, false, false),
                        r -> r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());
            }
            return;
        }
        if (mc.player == null || mc.level == null) return;
        if (!built) {
            built = true;
            build();
            t = 0;
        }
        t++;
        while (!cues.isEmpty() && (int) cues.get(0)[0] <= t) ((Runnable) cues.remove(0)[1]).run();
    }
}
