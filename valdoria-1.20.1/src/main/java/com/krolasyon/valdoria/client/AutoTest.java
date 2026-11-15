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

    private record Shot(String name, double dx, double dy, double dz, float yaw, float pitch, long time) {}

    private static final List<Shot> SHOTS = new ArrayList<>();
    static {
        SHOTS.add(new Shot("vd_up_north", 0, 0, 0, 180, -38, 6000));
        SHOTS.add(new Shot("vd_up_east", 0, 0, 0, -90, -30, 6000));
        SHOTS.add(new Shot("vd_level_south", 0, 0, 0, 0, -8, 6000));
        SHOTS.add(new Shot("vd_level_west", 0, 0, 0, 90, -12, 6000));
        SHOTS.add(new Shot("vd_dusk_up", 0, 0, 0, 135, -35, 13000));
        SHOTS.add(new Shot("vd_night_up", 0, 0, 0, 45, -30, 18000));
        SHOTS.add(new Shot("vd_aerial", 0, 45, 0, 30, 40, 6000));
    }

    private static boolean created;
    private static int menuTicks;
    private static int worldTicks;
    private static BlockPos target;
    private static int stage;
    private static int wait;

    public static boolean enabled() {
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
            mc.options.renderDistance().set(10);
            mc.options.fov().set(85);
            LevelSettings ls = new LevelSettings("valdoria_test", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            long seed = Long.getLong("valdoria.seed", 20261115L);
            mc.createWorldOpenFlows().createFreshLevel("valdoria_test", ls, new WorldOptions(seed, true, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).createWorldDimensions());
            return;
        }
        if (mc.player == null || mc.level == null) return;
        worldTicks++;
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
                    Pair<BlockPos, Holder<Biome>> found = level.findClosestBiome3d(h -> h.is(ValdoriaBiomes.GLIMMERVEIL_FOREST),
                            BlockPos.containing(0, 80, 0), 6400, 32, 64);
                    if (found == null) {
                        ValdoriaMod.LOG.error("[AUTOTEST] Glimmerveil Forest not found");
                        mc.execute(mc::stop);
                        return;
                    }
                    BlockPos p = found.getFirst();
                    // walk a bit further in so we are deep inside the forest, not at its border
                    int best = 0;
                    BlockPos bestPos = p;
                    for (int dx = -192; dx <= 192; dx += 48)
                        for (int dz = -192; dz <= 192; dz += 48) {
                            int score = 0;
                            for (int ox = -64; ox <= 64; ox += 32)
                                for (int oz = -64; oz <= 64; oz += 32)
                                    if (level.getBiome(p.offset(dx + ox, 0, dz + oz)).is(ValdoriaBiomes.GLIMMERVEIL_FOREST)) score++;
                            if (score > best) { best = score; bestPos = p.offset(dx, 0, dz); }
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
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());
                    target = new BlockPos(target.getX(), y, target.getZ());
                    ValdoriaMod.LOG.info("[AUTOTEST] ground at {}", target);
                    mc.execute(() -> { stage = 2; wait = 5; });
                });
            }
            default -> {
                if (stage < 2) return;
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
                    cmd("tp @a " + (target.getX() + 0.5 + s.dx) + " " + (target.getY() + 0.2 + s.dy) + " " + (target.getZ() + 0.5 + s.dz) + " " + s.yaw + " " + s.pitch);
                    mc.player.getAbilities().flying = s.dy > 0;
                    wait = s.dy > 0 ? 200 : 80;
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
