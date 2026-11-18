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
        double d = 1.6 + size * (size > 2.2 ? 1.75 : 1.25), eye = size * 0.85, look = size * 0.55;
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
        if (mode.contains("promo")) promo();
        if (mode.contains("faces")) {
            cue(5, () -> { Minecraft.getInstance().options.fov().set(30); clear(); });
            cue(20, () -> { put("zombie", 0, 0, 180, ""); put("villager", 2.2, 0, 180, "VillagerData:{profession:\"minecraft:farmer\",level:2,type:\"minecraft:plains\"}");
                put("creeper", -2.2, 0, 180, ""); cmd("tp @s 0 -59.0 -5.5 0 4"); });
            burst("f_faces", 8, 30, 10);
        }
        if (mode.contains("tall")) {
            gallery("enderman", "carriedBlockState:{Name:\"minecraft:grass_block\"}", 2.9);
            gallery("iron_golem", "", 2.7);
            gallery("spider", "", 1.0);
        }
        if (mode.contains("new")) {
            gallery("zombie", "", 2);
            gallery("husk", "", 2.1);
            gallery("chicken", "", 0.7);
            gallery("cow", "", 1.4);
            gallery("pig", "", 0.9);
            gallery("spider", "", 1.0);
            gallery("sheep", "Color:14b", 1.3);
            gallery("iron_golem", "", 2.7);
            gallery("enderman", "carriedBlockState:{Name:\"minecraft:grass_block\"}", 2.9);
            gallery("villager", "VillagerData:{profession:\"minecraft:farmer\",level:3,type:\"minecraft:plains\"}", 2.0);
            gallery("wandering_trader", "", 2.0);
        }
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
            gallery("sheep", "Color:14b", 1.3);
            gallery("iron_golem", "", 2.7);
            gallery("enderman", "carriedBlockState:{Name:\"minecraft:grass_block\"}", 2.9);
            gallery("villager", "VillagerData:{profession:\"minecraft:farmer\",level:3,type:\"minecraft:plains\"}", 2.0);
            gallery("wandering_trader", "", 2.0);
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

    private static void line(double z, String... mobs) {
        double x = (mobs.length - 1) * 1.1;
        for (String m : mobs) {
            String[] p = m.split("\\|", 2);
            cmd(String.format(java.util.Locale.ROOT, "summon %s %.2f -60 %.2f {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]%s}",
                    p[0], x, z, p.length > 1 ? "," + p[1] : ""));
            x -= 2.2;
        }
    }

    private static void clear() {
        cmd("tp @e[type=!player] 0 -1000 0");   // drop out of the world far below: no death clutter on screen
    }

    private static void put(String id, double x, double z, float yaw, String nbt) {
        cmd(String.format(java.util.Locale.ROOT, "summon %s %.2f -60 %.2f {NoAI:1b,PersistenceRequired:1b,Rotation:[%.1ff,0f]%s}",
                id, x, z, yaw, nbt.isEmpty() ? "" : "," + nbt));
    }

    private static void burst(String name, int n, int first, int gap) {
        for (int i = 0; i < n; i++) { final int k = i; cue(i == 0 ? first : gap, () -> shot(name + "_" + k)); }
    }

    /** Staged scenes for the CurseForge page. */
    private static void promo() {
        String sword = "HandItems:[{id:\"minecraft:iron_sword\",Count:1b},{}]", bow = "HandItems:[{id:\"minecraft:bow\",Count:1b},{}]";
        // 1. hero group (cover)
        cue(5, () -> { Minecraft.getInstance().options.fov().set(48); clear(); });
        cue(20, () -> {
            put("iron_golem", 0.3, 6.2, 180, "");
            put("enderman", -2.6, 5.6, 195, "carriedBlockState:{Name:\"minecraft:grass_block\"}");
            put("villager", 2.9, 4.8, 165, "VillagerData:{profession:\"minecraft:librarian\",level:4,type:\"minecraft:taiga\"}");
            put("cow", -4.4, 4.0, 210, "");
            put("creeper", -1.3, 3.2, 185, "");
            put("skeleton", 1.6, 3.0, 170, bow);
            put("zombie", 0.1, 1.9, 180, sword);
            put("sheep", 4.2, 2.6, 150, "Color:2b");
            put("spider", -3.0, 1.9, 205, "");
            put("pig", 2.6, 1.0, 160, "");
            put("chicken", -1.2, 0.6, 195, "");
            cmd("tp @s 0 -59.0 -5.2 0 9");
        });
        burst("p_hero", 5, 40, 9);
        // 2. hostile row
        cue(5, this_clear());
        cue(20, () -> {
            put("zombie", 3.6, 2.5, 180, sword); put("skeleton", 1.8, 2.5, 180, bow); put("creeper", 0, 2.5, 180, "");
            put("husk", -1.8, 2.5, 180, ""); put("drowned", -3.6, 2.5, 180, "HandItems:[{id:\"minecraft:trident\",Count:1b},{}]");
            put("spider", 0.2, 0.2, 180, "");
            cmd("tp @s 0 -59.2 -5.0 0 7");
        });
        burst("p_hostile", 3, 35, 11);
        // 3. friendly row
        cue(5, this_clear());
        cue(20, () -> {
            put("villager", 3.6, 2.5, 180, "VillagerData:{profession:\"minecraft:farmer\",level:2,type:\"minecraft:plains\"}");
            put("iron_golem", 1.3, 3.2, 180, ""); put("wandering_trader", -1.2, 2.5, 180, ""); put("enderman", -3.4, 2.8, 180, "");
            put("sheep", 2.2, 0.4, 200, "Color:0b"); put("pig", 0, 0.2, 180, ""); put("chicken", -1.6, -0.2, 170, ""); put("cow", -3.6, 0.6, 160, "");
            cmd("tp @s 0 -59.0 -5.6 0 8");
        });
        burst("p_friendly", 3, 35, 11);
        // 4. close-ups
        cue(5, () -> { clear(); Minecraft.getInstance().options.fov().set(30); });
        cue(20, () -> { put("chicken", 0, 0, 150, ""); put("chicken", 1.0, 0.6, 205, ""); cmd("tp @s 0.3 -60 -3.2 0 18"); });
        burst("p_chicken", 3, 30, 11);
        cue(5, this_clear());
        cue(20, () -> { put("zombie", 0, 0, 180, ""); cmd("tp @s 0 -59.4 -2.6 0 2"); });
        burst("p_zombieface", 6, 30, 7);
        cue(5, this_clear());
        cue(20, () -> { put("villager", 0, 0, 180, "VillagerData:{profession:\"minecraft:cleric\",level:5,type:\"minecraft:snow\"}");
            put("sheep", 1.6, 1.2, 210, "Color:6b"); cmd("tp @s 0.6 -59.5 -3.4 0 6"); });
        burst("p_villager", 3, 30, 9);
        cue(5, this_clear());
        cue(20, () -> { put("spider", 0, 0, 160, ""); put("cave_spider", 1.3, 0.4, 210, ""); cmd("tp @s 0.4 -60 -3.0 0 20"); });
        burst("p_spider", 2, 30, 9);
        // 5. golem vs zombies (side view)
        cue(5, () -> { clear(); Minecraft.getInstance().options.fov().set(50); });
        cue(20, () -> {
            cmd("summon iron_golem 0 -60 2 {PersistenceRequired:1b}");
            cmd("summon zombie 0 -60 5 {PersistenceRequired:1b}"); cmd("summon zombie 1 -60 6 {PersistenceRequired:1b}");
            cmd("tp @s 7 -59.4 4 90 6"); });
        burst("p_golem", 14, 20, 4);
        // 6. horde coming at the player
        cue(5, () -> { clear(); cmd("gamemode survival");
            cmd("effect give @s resistance 600 255 true"); cmd("effect give @s regeneration 600 255 true"); });
        cue(20, () -> { cmd("tp @s 0 -60 -4 0 4");
            cmd("summon zombie -1.5 -60 3 {PersistenceRequired:1b}");
            cmd("summon husk 1.8 -60 5 {PersistenceRequired:1b}");
            cmd("summon spider -3.5 -60 6 {PersistenceRequired:1b}"); });
        burst("p_attack", 10, 14, 5);
        // 7. death
        cue(5, () -> { clear(); cmd("gamemode creative"); });
        cue(20, () -> { cmd("tp @s 0 -59.6 -4.2 0 8");
            put("cow", -1.5, 0, 150, ""); put("pig", 1.5, 0, 220, ""); });
        cue(30, () -> { cmd("kill @e[type=cow]"); cmd("damage @e[type=pig,limit=1] 1 minecraft:player_attack"); });
        burst("p_death", 6, 3, 3);
    }

    private static Runnable this_clear() { return AutoTest::clear; }

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
