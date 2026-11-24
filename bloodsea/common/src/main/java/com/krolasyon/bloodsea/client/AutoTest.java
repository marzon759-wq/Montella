package com.krolasyon.bloodsea.client;

import com.krolasyon.bloodsea.BloodSea;
import com.krolasyon.bloodsea.compat.TestWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Development only: with -Dbloodsea.autotest=<mode> the client makes a world, sails to every region and takes pictures. */
public final class AutoTest {
    private static String mode = null;
    private static int menu = 0, n = 0, at = 0;
    private static boolean created = false;
    private static com.krolasyon.bloodsea.world.SeaShape.Giant giantRib = null, giantSkull = null;
    private static final List<Object[]> cues = new ArrayList<>();

    private AutoTest() {}

    public static void init() {
        mode = System.getProperty("bloodsea.autotest");
        if (mode == null || mode.isEmpty()) { mode = null; return; }
        BloodSea.LOG.info("[AUTOTEST] {}", mode);
        build();
    }

    static void cue(int delay, Runnable r) { at += delay; cues.add(new Object[]{at, r}); }

    /** run a command as the player, with full permission */
    static void cmd(String c) {
        Minecraft mc = Minecraft.getInstance();
        var s = mc.getSingleplayerServer();
        if (s == null || mc.player == null) return;
        s.execute(() -> {
            ServerPlayer p = s.getPlayerList().getPlayer(mc.player.getUUID());
            if (p != null) s.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4), c);
        });
    }

    static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        BloodSea.LOG.info("[AUTOTEST] shot {} at {}", name, mc.player.position());
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {});
    }

    /** go somewhere in the Blood Sea, wait for the chunks, take a picture */
    static void view(String name, double x, double y, double z, float yaw, float pitch, int time) {
        cue(5, () -> cmd("time set " + time));
        cue(5, () -> cmd("execute in bloodsea:blood_sea run tp @s " + x + " " + y + " " + z + " " + yaw + " " + pitch));
        cue(160, () -> shot(name));
    }

    /** stand by an ending, trigger it the way a player would, skip the screen, check we are home again */
    static void ending(String id, double x, double y, double z, float yaw, float pitch, int bx, int by, int bz) {
        cue(5, () -> cmd("execute in bloodsea:blood_sea run tp @s " + x + " " + y + " " + z + " " + yaw + " " + pitch));
        cue(120, () -> {
            if (bx == Integer.MIN_VALUE) return;
            Minecraft mc = Minecraft.getInstance();
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(new net.minecraft.core.BlockPos(bx, by, bz)),
                    net.minecraft.core.Direction.UP, new net.minecraft.core.BlockPos(bx, by, bz), false);
            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        });
        cue(80, () -> {
            Minecraft mc = Minecraft.getInstance();
            BloodSea.LOG.info("[ENDTEST] {} screen={}", id, mc.screen == null ? "none" : mc.screen.getClass().getSimpleName());
            shot("e_" + id);
        });
        cue(80, () -> { Minecraft mc = Minecraft.getInstance(); if (mc.screen instanceof EndingScreen es) es.keyPressed(256, 0, 0); });
        cue(60, () -> BloodSea.LOG.info("[ENDTEST] {} after: {}", id, Minecraft.getInstance().level.dimension().location()));
        cue(5, () -> cmd("bloodsea enter"));
        cue(60, () -> {});
    }

    private static void build() {
        at = 60;
        if (mode.contains("endings")) {
            cue(0, () -> { cmd("gamerule doDaylightCycle false"); cmd("time set 6000"); cmd("bloodsea enter"); cmd("gamemode adventure"); });
            cue(80, () -> {});
            ending("bone", 0.5, 80, -1447.5, 180, 30, 0, 81, -1451);
            ending("mirage", -1445.5, 66, 0.5, 90, 10, -1447, 66, 0);
            ending("leviathan", 1450.5, 64, 0.5, 0, 0, Integer.MIN_VALUE, 0, 0);
            ending("edge", 0.5, 0, 1915, 0, 0, Integer.MIN_VALUE, 0, 0);
            cue(20, () -> Minecraft.getInstance().stop());
            return;
        }
        cue(0, () -> { cmd("gamerule doDaylightCycle false"); cmd("bloodsea enter"); });
        cue(80, () -> cmd("gamemode spectator"));
        cue(5, () -> Minecraft.getInstance().options.hideGui = true);
        // every bone prop in a row on a platform by the raft
        cue(5, () -> cmd("execute in bloodsea:blood_sea run fill -16 62 -12 16 62 -5 minecraft:spruce_planks"));
        String[] props = {"skull", "skull_side", "skull_heap", "bone_pile", "ribs", "ribcage", "skeleton", "spine", "hand", "heart"};
        for (int i = 0; i < props.length; i++) {
            int x = -14 + i * 3;
            String pn = props[i];
            cue(2, () -> cmd("execute in bloodsea:blood_sea run setblock " + x + " 63 -9 bloodsea:" + pn + "[facing=south]"));
        }
        view("s_props", 0.5, 64.2, -1.5, 180, 22, 6000);
        view("s_props_close", -9.5, 63.5, -6.0, 180, 30, 6000);
        view("s_props_night", 0.5, 64.2, -1.5, 180, 22, 17000);
        cue(1, () -> { if (giantRib != null) cmd("execute in bloodsea:blood_sea run tp @s " + giantRib.x() + " 70 " + (giantRib.z() + 30) + " 180 12"); });
        cue(160, () -> shot("s_giant_ribcage"));
        cue(1, () -> { if (giantSkull != null) cmd("execute in bloodsea:blood_sea run tp @s " + giantSkull.x() + " 66 " + (giantSkull.z() + 18) + " 180 8"); });
        cue(160, () -> shot("s_giant_skull"));
        view("s_island", 0.5, 68, -1385, 180, 18, 4000);
        view("s_lev_head", 1285, 70, 75, -150, 12, 4000);
        // the giants of the Bone Reach: find the nearest ones and look at them
        cue(1, () -> {
            for (int cx = -30; cx <= 30 && giantRib == null; cx++)
                for (int cz = -70; cz <= -40 && giantRib == null; cz++)
                    for (var g : com.krolasyon.bloodsea.world.SeaShape.giantsIn(cx, cz))
                        if (g.variant().startsWith("giant_ribcage") && giantRib == null) giantRib = g;
            for (int cx = -30; cx <= 30 && giantSkull == null; cx++)
                for (int cz = -70; cz <= -40 && giantSkull == null; cz++)
                    for (var g : com.krolasyon.bloodsea.world.SeaShape.giantsIn(cx, cz))
                        if (g.variant().equals("giant_skull") && giantSkull == null) giantSkull = g;
            BloodSea.LOG.info("[AUTOTEST] giant rib cage {}, skull {}", giantRib, giantSkull);
        });
        if (mode.contains("giants")) {
            view("s_maw", 1408, 76, 0.5, -90, 6, 5000);
            view("s_leviathan", 900.5, 80, -60, 180, 10, 3500);
            view("s_lev_head", 1285, 70, 75, -150, 12, 4000);
            cue(1, () -> { if (giantRib != null) cmd("execute in bloodsea:blood_sea run tp @s " + giantRib.x() + " 70 " + (giantRib.z() + 30) + " 180 12"); });
            cue(160, () -> shot("s_giant_ribcage"));
            cue(1, () -> { if (giantSkull != null) cmd("execute in bloodsea:blood_sea run tp @s " + (giantSkull.x() + 14) + " 68 " + (giantSkull.z() + 14) + " 135 15"); });
            cue(160, () -> shot("s_giant_skull"));
            view("s_head_close", 1346, 69, 40, 100, 16, 4000);
            view("s_island_smooth", 0.5, 76, -1418, 180, 30, 4000);
            // stand on the giant skull: the colliders must hold you up
            cue(5, () -> cmd("gamemode adventure"));
            cue(5, () -> { if (giantSkull != null) cmd("execute in bloodsea:blood_sea run tp @s " + giantSkull.x() + " 80 " + giantSkull.z()); });
            cue(100, () -> BloodSea.LOG.info("[AUTOTEST] standing on the giant skull at y={}", Minecraft.getInstance().player.getY()));
            cue(20, () -> Minecraft.getInstance().stop());
            return;
        }
        view("s_raft_day", 3.5, 64.6, 6.5, 150, 12, 1000);
        view("s_sun", 0.5, 64.6, 0.5, -90, -50, 2500);
        view("s_moon", 0.5, 64.6, 0.5, -90, -60, 16500);
        view("s_raft_night", 3.5, 64.6, 6.5, 150, 8, 15000);
        view("s_bones", 0.5, 72, -980, 180, 6, 3000);
        view("s_bones_night", 0.5, 72, -980, 180, 6, 17000);
        view("s_throne", 0.5, 84, -1441, 180, 18, 4000);
        view("s_leviathan", 900.5, 80, -60, 180, 10, 3500);
        view("s_maw", 1408, 84, 0.5, -90, 12, 5000);
        view("s_edge", 0.5, 70, 1870, 0, 8, 6000);
        view("s_waterfall", 40, 50, 1935, 160, 20, 6000);
        view("s_mirage", -880, 68, 0.5, 90, 4, 3000);
        view("s_mirage_night", -880, 68, 0.5, 90, 4, 17500);
        view("s_lighthouse", -1415, 72, 6, 120, -6, 13000);
        view("s_under", 0.5, 52, 20.5, 180, 0, 3000);
        cue(20, () -> Minecraft.getInstance().stop());
    }

    public static void tick() {
        if (mode == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (!created && mc.screen instanceof TitleScreen && ++menu > 40) {
                created = true;
                try {
                    TestWorld.create("bloodsea_test_" + System.currentTimeMillis() % 100000);
                } catch (Throwable t) {
                    BloodSea.LOG.error("[AUTOTEST] world creation failed", t);
                    mc.stop();
                }
            }
            return;
        }
        n++;
        for (Object[] c : cues) if ((int) c[0] == n) {
            try { ((Runnable) c[1]).run(); } catch (Throwable t) { BloodSea.LOG.error("[AUTOTEST] cue failed", t); }
        }
    }
}
