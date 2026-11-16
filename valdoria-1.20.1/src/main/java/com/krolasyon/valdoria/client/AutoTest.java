package com.krolasyon.valdoria.client;

import com.krolasyon.valdoria.ValdoriaMod;
import com.krolasyon.valdoria.world.ValdoriaBiomes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.ArrayList;
import java.util.List;

/**
 * Development only (enabled with -Dvaldoria.autotest=1): creates a normal world, flies to the nearest
 * Glimmerveil Forest and takes a set of screenshots into run/screenshots/, then quits.
 */
public final class AutoTest {
    private AutoTest() {}

    private record Shot(String name, double dx, double dy, double dz, float yaw, float pitch, long time, int settle, String... cmds) {
        Shot(String name, double dx, double dy, double dz, float yaw, float pitch, long time) {
            this(name, dx, dy, dz, yaw, pitch, time, 0);
        }
    }

    private static final List<Shot> SHOTS = new ArrayList<>();
    static {
        SHOTS.add(new Shot("vd_up_north", 0, 0, 0, 180, -38, 6000));
        SHOTS.add(new Shot("vd_up_east", 0, 0, 0, -90, -30, 6000));
        SHOTS.add(new Shot("vd_level_south", 0, 0, 0, 0, -8, 6000));
        SHOTS.add(new Shot("vd_level_west", 0, 0, 0, 90, -12, 6000));
        SHOTS.add(new Shot("vd_up_south", 0, 0, 0, 20, -25, 6000));
        SHOTS.add(new Shot("vd_dusk_up", 0, 0, 0, 135, -35, 12500));
        SHOTS.add(new Shot("vd_night_up", 0, 0, 0, 45, -30, 18000));
        SHOTS.add(new Shot("vd_aerial", 0, 70, 0, 30, 35, 6000));
        // --- creatures: portraits (frozen, right in front of the camera) ---
        SHOTS.add(new Shot("pt_veilstag", 0, 0, 0, 180, 8, 6000, 60, "kill @e[type=!player,type=!item]",
                "summon valdoria:veilstag {x} {y} {z-5} {NoAI:1b,Rotation:[120f,0f]}",
                "summon valdoria:veilstag {x+2} {y} {z-4} {NoAI:1b,Age:-24000,Rotation:[150f,0f]}"));
        SHOTS.add(new Shot("pt_gloomlynx", 0, 0, 0, 180, 18, 6000, 60, "kill @e[type=!player,type=!item]",
                "summon valdoria:gloomlynx {x} {y} {z-3} {NoAI:1b,Rotation:[120f,0f]}"));
        SHOTS.add(new Shot("pt_gloomlynx_night", 0, 0, 0, 180, 18, 18000, 40));
        SHOTS.add(new Shot("pt_vinestalker", 0, 0, 0, 180, 18, 6000, 60, "kill @e[type=!player,type=!item]",
                "summon valdoria:vinestalker {x} {y} {z-4} {NoAI:1b,Rotation:[150f,0f]}"));
        SHOTS.add(new Shot("pt_lumen_moth", 0, 0, 0, 180, 10, 13000, 60, "kill @e[type=!player,type=!item]",
                "summon valdoria:lumen_moth {x} {y+1} {z-2} {NoAI:1b,Rotation:[150f,0f]}",
                "summon valdoria:lumen_moth {x+1} {y+2} {z-3} {NoAI:1b,Rotation:[60f,0f]}"));
        // --- creatures: live behaviour ---
        SHOTS.add(new Shot("cr_herd", 0, 0, 0, 180, 4, 6000, 120, "kill @e[type=!player,type=!item]",
                "summon valdoria:veilstag {x} {y} {z-8}", "summon valdoria:veilstag {x+2} {y} {z-10}", "summon valdoria:veilstag {x-3} {y} {z-9}",
                "summon valdoria:veilstag {x+4} {y} {z-12}", "summon valdoria:veilstag {x-1} {y} {z-7} {Age:-24000}",
                "summon valdoria:lumen_moth {x} {y+3} {z-5}", "summon valdoria:lumen_moth {x+1} {y+3} {z-5}", "summon valdoria:lumen_moth {x-1} {y+4} {z-6}",
                "summon valdoria:lumen_moth {x} {y+2} {z-4}", "summon valdoria:lumen_moth {x+2} {y+4} {z-6}", "summon valdoria:lumen_moth {x-2} {y+3} {z-4}"));
        SHOTS.add(new Shot("cr_herd_later", 0, 0, 0, 180, 4, 6000, 400));
        SHOTS.add(new Shot("cr_lynx_day", 0, 0, 0, 180, 10, 6000, 300, "kill @e[type=valdoria:veilstag]",
                "summon valdoria:gloomlynx {x} {y} {z-5}", "summon valdoria:gloomlynx {x+2} {y} {z-6}", "summon valdoria:gloomlynx {x-2} {y} {z-7}"));
        SHOTS.add(new Shot("cr_hunt_night", 0, 0, 0, 180, 5, 18000, 60, "kill @e[type=!player,type=!item]",
                "summon valdoria:veilstag {x} {y} {z-9}", "summon valdoria:veilstag {x+2} {y} {z-10}", "summon valdoria:veilstag {x-1} {y} {z-8} {Age:-24000}",
                "summon valdoria:gloomlynx {x+5} {y} {z-4} {Satiety:0}", "summon valdoria:gloomlynx {x+6} {y} {z-6} {Satiety:0}"));
        SHOTS.add(new Shot("cr_hunt_night2", 0, 0, 0, 180, 5, 18000, 100));
        SHOTS.add(new Shot("cr_hunt_night3", 0, 0, 0, 180, 5, 18000, 400));
        SHOTS.add(new Shot("cr_stalker", 0, 0, 0, 180, -35, 6000, 200, "kill @e[type=!player,type=!item]",
                "summon valdoria:vinestalker {x} {y} {z-4}", "summon valdoria:vinestalker {x+2} {y} {z-6}", "summon valdoria:vinestalker {x-3} {y} {z-5}"));
        SHOTS.add(new Shot("cr_stalker_ambush", 0, 0, 0, 180, -10, 6000, 200,
                "summon valdoria:veilstag {x} {y} {z-4}", "summon valdoria:veilstag {x+2} {y} {z-6}", "summon valdoria:veilstag {x-3} {y} {z-5}"));
    }

    private static boolean created;
    private static int menuTicks;
    private static int worldTicks;
    private static BlockPos target;
    private static int stage;
    private static int wait;

    private static String expand(String c) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < c.length()) {
            int a = c.indexOf('{', i);
            if (a < 0 || c.indexOf('}', a) < 0 || "xyz".indexOf(c.charAt(a + 1)) < 0) { out.append(c.substring(i)); break; }
            int b = c.indexOf('}', a);
            out.append(c, i, a);
            String expr = c.substring(a + 1, b);
            int base = switch (expr.charAt(0)) { case 'x' -> target.getX(); case 'y' -> target.getY(); default -> target.getZ(); };
            int off = expr.length() > 1 ? Integer.parseInt(expr.substring(1).replace("+", "")) : 0;
            out.append(base + off);
            i = b + 1;
        }
        return out.toString();
    }

    private static boolean isForest(ServerLevel level, int x, int z) {
        return level.getUncachedNoiseBiome(x >> 2, 80 >> 2, z >> 2).is(ValdoriaBiomes.GLIMMERVEIL_FOREST);
    }

    private static boolean filtered;

    public static boolean enabled() {
        if (!filtered) {
            filtered = true;
            String only = System.getProperty("valdoria.autotest", "1");
            if (!only.equals("1")) SHOTS.removeIf(s -> java.util.Arrays.stream(only.split(",")).noneMatch(s.name()::startsWith));
        }
        return System.getProperty("valdoria.autotest") != null;
    }

    private static void cmd(String c) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), c));
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!created && mc.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            mc.options.onboardAccessibility = false;
            mc.setScreen(new TitleScreen());
            return;
        }
        if (!created && mc.screen instanceof TitleScreen && ++menuTicks > 40) {
            created = true;
            mc.options.hideGui = true;
            mc.options.renderDistance().set(12);
            mc.options.fov().set(85);
            String worldName = "valdoria_test_" + System.currentTimeMillis();
            LevelSettings ls = new LevelSettings(worldName, GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            long seed = Long.getLong("valdoria.seed", 20261115L);
            mc.createWorldOpenFlows().createFreshLevel(worldName, ls, new WorldOptions(seed, true, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).createWorldDimensions());
            return;
        }
        if (mc.player == null || mc.level == null) return;
        worldTicks++;
        if ("mistfall".equals(System.getProperty("valdoria.autotest"))) {
            if (worldTicks > 60) MistfallTest.tick();
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return;
        switch (stage) {
            case 0 -> {
                if (worldTicks < 60) return;
                stage = -1;
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    // scan the biome noise (no chunk generation needed) for the spot deepest inside a Glimmerveil Forest
                    int best = -1;
                    BlockPos bestPos = null;
                    for (int x = -3000; x <= 3000; x += 96)
                        for (int z = -3000; z <= 3000; z += 96) {
                            if (!isForest(level, x, z)) continue;
                            int score = 0;
                            for (int ox = -96; ox <= 96; ox += 32)
                                for (int oz = -96; oz <= 96; oz += 32)
                                    if (isForest(level, x + ox, z + oz)) score++;
                            int dist = (Math.abs(x) + Math.abs(z)) / 600;
                            if (score - dist > best) { best = score - dist; bestPos = new BlockPos(x, 80, z); }
                        }
                    if (bestPos == null) {
                        ValdoriaMod.LOG.error("[AUTOTEST] Glimmerveil Forest not found");
                        mc.execute(mc::stop);
                        return;
                    }
                    target = bestPos;
                    ValdoriaMod.LOG.info("[AUTOTEST] Glimmerveil Forest at {} (score {})", target, best);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "gamerule doDaylightCycle false");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "gamerule doWeatherCycle false");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "weather clear");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(),
                            "tp @a " + target.getX() + " 200 " + target.getZ());
                    mc.execute(() -> { stage = 1; wait = 300; });
                });
            }
            case 1 -> {
                stage = -1;
                server.execute(() -> {
                    ServerLevel level = server.overworld();
                    // find an open spot on the forest floor (not inside a trunk, not in water)
                    BlockPos found = null;
                    for (int r = 0; r <= 24 && found == null; r++)
                        for (int dx = -r; dx <= r && found == null; dx++)
                            for (int dz = -r; dz <= r && found == null; dz++) {
                                if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                                int x = target.getX() + dx, z = target.getZ() + dz;
                                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                                BlockPos g = new BlockPos(x, y, z);
                                var below = level.getBlockState(g.below());
                                if (below.is(net.minecraft.tags.BlockTags.DIRT) && level.getFluidState(g).isEmpty()
                                        && level.getBlockState(g.above()).isAir() && level.getBlockState(g.above(2)).isAir()) found = g;
                            }
                    target = found != null ? found : new BlockPos(target.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ()), target.getZ());
                    ValdoriaMod.LOG.info("[AUTOTEST] ground at {}", target);
                    mc.execute(() -> { stage = 2; wait = 5; });
                });
            }
            default -> {
                if (stage < 2) return;
                if ("trailer".equals(System.getProperty("valdoria.autotest"))) {
                    if (stage == 2) {
                        Trailer.start(target);
                        stage = 3;
                    }
                    if (!Trailer.tick(c -> cmd(expand(c)))) {
                        ValdoriaMod.LOG.info("[AUTOTEST] finished");
                        stage = -1;
                        mc.stop();
                    }
                    return;
                }
                int i = (stage - 2) / 2;
                if (i >= SHOTS.size()) {
                    ValdoriaMod.LOG.info("[AUTOTEST] finished");
                    stage = -1;
                    mc.stop();
                    return;
                }
                Shot s = SHOTS.get(i);
                if ((stage - 2) % 2 == 0) {
                    cmd("time set " + s.time);
                    for (String c : s.cmds) cmd(expand(c));
                    cmd("tp @a " + (target.getX() + 0.5 + s.dx) + " " + (target.getY() + 0.2 + s.dy) + " " + (target.getZ() + 0.5 + s.dz) + " " + s.yaw + " " + s.pitch);
                    mc.player.getAbilities().flying = s.dy > 0;
                    wait = s.settle > 0 ? s.settle : s.dy > 0 ? 200 : 80;
                } else {
                    Screenshot.grab(mc.gameDirectory, s.name + ".png", mc.getMainRenderTarget(), c -> {});
                    ValdoriaMod.LOG.info("[AUTOTEST] shot {}", s.name);
                    wait = 5;
                }
                stage++;
            }
        }
    }
}
