package com.krolasyon.livingmotion.client;

import com.krolasyon.livingmotion.LivingMotion;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Development hook only: with -Dlivingmotion.autotest=1 the client creates a flat test world, lines up the
 * remodelled mobs, provokes hits/deaths/attacks and saves screenshots, then quits. Does nothing in a normal game.
 */
public final class AutoTest {
    private AutoTest() {}

    private static final List<Object[]> CUES = new ArrayList<>();
    private static int at, n;
    private static boolean created;

    public static void init() {
        String mode = System.getProperty("livingmotion.autotest");
        if (mode == null || mode.isEmpty()) return;
        LivingMotion.LOG.info("[AUTOTEST] enabled");
        build();
        MinecraftForge.EVENT_BUS.addListener(AutoTest::tick);
    }

    private static void cue(int delay, Runnable r) {
        at += delay;
        CUES.add(new Object[]{at, r});
    }

    private static void cmd(String c) {
        Minecraft mc = Minecraft.getInstance();
        var s = mc.getSingleplayerServer();
        if (s == null || mc.player == null) return;
        s.execute(() -> {
            ServerPlayer p = s.getPlayerList().getPlayer(mc.player.getUUID());
            if (p != null) s.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4), c);
        });
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        LivingMotion.LOG.info("[AUTOTEST] shot {}", name);
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {});
    }

    private static double[] cam(double dist, double h, double angDeg) {
        double a = Math.toRadians(angDeg);
        double x = Math.sin(a) * dist, z = -Math.cos(a) * dist;
        float yaw = (float) Math.toDegrees(Math.atan2(-(0 - x), (0 - z)));
        return new double[]{x, h, z, yaw};
    }

    private static void view(double dist, double eye, double look, double ang) {
        double[] c = cam(dist, eye, ang);
        eye = Math.max(eye, 1.62);
        double pitch = Math.toDegrees(Math.atan2(eye - look, dist));
        cmd(String.format(java.util.Locale.ROOT, "tp @s %.3f %.3f %.3f %.2f %.2f", c[0], -60 + eye - 1.62, c[2], c[3], pitch));
    }

    /** One mob: front, three-quarter, hurt and death shots. */
    private static void gallery(String id, String nbt, double size) {
        cue(4, () -> cmd("kill @e[type=!player]"));
        cue(4, () -> cmd("summon " + id + " 0 -60 0 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]" + (nbt.isEmpty() ? "" : "," + nbt) + "}"));
        double d = 1.6 + size * 1.25, eye = size * 0.85, look = size * 0.6;
        cue(2, () -> view(d, eye, look, 0));
        cue(28, () -> shot(id + "_1front"));
        cue(1, () -> view(d * 1.05, eye, look, 55));
        cue(6, () -> shot(id + "_2side"));
        cue(1, () -> view(d * 1.1, eye, look, 95));
        cue(6, () -> shot(id + "_3profile"));
        cue(1, () -> view(d, eye, look, 30));
        cue(4, () -> cmd("damage @e[type=" + id + ",limit=1] 1 minecraft:player_attack"));
        cue(2, () -> shot(id + "_4hurt"));
        cue(3, () -> cmd("kill @e[type=" + id + "]"));
        cue(9, () -> shot(id + "_5dying"));
        cue(8, () -> shot(id + "_6dead"));
    }

    private static void build() {
        at = 40;
        cue(0, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.hideGui = true;
            mc.options.fov().set(30);
            cmd("gamerule doDaylightCycle false"); cmd("time set 18000"); cmd("weather clear");
            cmd("gamerule doMobSpawning false"); cmd("gamerule doMobLoot false"); cmd("difficulty easy");
            cmd("gamemode creative"); cmd("effect give @s night_vision 99999 0 true");
        });
        String mode = System.getProperty("livingmotion.autotest");
        if (mode.contains("gallery") || mode.equals("1")) {
            gallery("zombie", "HandItems:[{id:\"minecraft:iron_sword\",Count:1b},{}],ArmorItems:[{},{},{id:\"minecraft:iron_chestplate\",Count:1b},{}]", 2);
            gallery("husk", "", 2.1);
            gallery("drowned", "HandItems:[{id:\"minecraft:trident\",Count:1b},{}]", 2);
            gallery("skeleton", "HandItems:[{id:\"minecraft:bow\",Count:1b},{}],ArmorItems:[{},{},{},{id:\"minecraft:carved_pumpkin\",Count:1b}]", 2);
            gallery("stray", "HandItems:[{id:\"minecraft:bow\",Count:1b},{}]", 2);
            gallery("wither_skeleton", "HandItems:[{id:\"minecraft:stone_sword\",Count:1b},{}]", 2.4);
            gallery("creeper", "", 1.7);
            gallery("spider", "", 1.0);
            gallery("cave_spider", "", 0.6);
            gallery("cow", "", 1.4);
            gallery("pig", "", 0.9);
            gallery("chicken", "", 0.7);
        }
        if (mode.contains("combat") || mode.equals("1")) {
            cue(10, () -> {
                Minecraft.getInstance().options.fov().set(70);
                cmd("kill @e[type=!player]");
                cmd("gamemode survival");
                cmd("effect give @s resistance 600 255 true");
                cmd("effect give @s regeneration 600 255 true");
                cmd("tp @s 0 -60 -4 0 5");
            });
            cue(5, () -> {
                cmd("summon zombie -2 -60 6 {PersistenceRequired:1b}");
                cmd("summon skeleton 3 -60 7 {PersistenceRequired:1b,HandItems:[{id:\"minecraft:bow\",Count:1b},{}]}");
                cmd("summon spider 0 -60 9 {PersistenceRequired:1b}");
            });
            cue(30, () -> shot("c_combat_a"));
            for (int i = 0; i < 6; i++) {
                final int k = i;
                cue(7, () -> shot("c_combat_" + (char) ('b' + k)));
            }
            cue(5, () -> {
                cmd("kill @e[type=!player]");
                cmd("tp @s 0 -60 -4 0 5");
                cmd("summon creeper 0 -60 0 {PersistenceRequired:1b}");
            });
            cue(18, () -> shot("c_creeper_a"));
            cue(6, () -> shot("c_creeper_b"));
            cue(6, () -> shot("c_creeper_c"));
            cue(10, () -> cmd("kill @e[type=!player]"));
        }
        cue(40, () -> Minecraft.getInstance().stop());
    }

    private static void tick(TickEvent.ClientTickEvent ev) {
        if (ev.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!created && mc.screen instanceof TitleScreen) {
            created = true;
            LevelSettings ls = new LevelSettings("lm_test", GameType.CREATIVE, false, Difficulty.EASY, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            mc.createWorldOpenFlows().createFreshLevel("lm_test", ls, new WorldOptions(1962L, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());
            return;
        }
        if (mc.player == null || mc.level == null) return;
        n++;
        for (Object[] c : CUES) if ((int) c[0] == n) ((Runnable) c[1]).run();
    }
}
