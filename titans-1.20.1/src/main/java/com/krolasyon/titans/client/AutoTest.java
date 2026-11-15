package com.krolasyon.titans.client;

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

/**
 * Development hook: with -Dtitan.autotest="c cmd;w 40;s name;q" the client makes a flat world, runs the
 * script (c = command, w = wait ticks, s = screenshot, q = quit). Does nothing in a normal game.
 */
final class AutoTest {
    private static String[] script;
    private static int at, wait, menu;
    private static boolean created;

    private AutoTest() {}

    static void init() {
        String s = System.getProperty("titan.autotest");
        if (s == null || s.isEmpty()) return;
        script = s.split(";");
        MinecraftForge.EVENT_BUS.addListener(AutoTest::tick);
    }

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            if (!created && mc.screen instanceof TitleScreen && ++menu > 40) {
                created = true;
                LevelSettings ls = new LevelSettings("titantest", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("titantest", ls, new WorldOptions(1962L, false, false),
                        r -> r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());
            }
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        mc.options.hideGui = true;
        mc.options.pauseOnLostFocus = false;
        if (mc.screen != null) mc.setScreen(null);
        if (wait > 0) { wait--; return; }
        while (at < script.length && wait == 0) {
            String line = script[at++].trim();
            if (line.isEmpty()) continue;
            char k = line.charAt(0);
            String arg = line.length() > 2 ? line.substring(2) : "";
            if (k == 'w') wait = Integer.parseInt(arg);
            else if (k == 's') { Screenshot.grab(mc.gameDirectory, arg + ".png", mc.getMainRenderTarget(), c -> {}); wait = 2; }
            else if (k == 'q') mc.stop();
            else if (k == 'c') {
                var srv = mc.getSingleplayerServer();
                var id = mc.player.getUUID();
                srv.execute(() -> {
                    ServerPlayer p = srv.getPlayerList().getPlayer(id);
                    if (p != null) srv.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), arg);
                });
            }
        }
    }
}
