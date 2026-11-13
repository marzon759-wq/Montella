package com.krolasyon.hatman.client;

import com.krolasyon.hatman.HatmanMod;
import com.krolasyon.hatman.compat.TestWorld;
import com.krolasyon.hatman.story.Story;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Development hook: with -Dhatman.autotest=<scenario> the client creates a world by itself, drives the story
 * and saves screenshots. Does nothing at all in a normal game.
 */
public final class AutoTest {
    private static String mode = null;
    private static int menuTicks = 0, n = 0, cursor = 0;
    private static boolean created = false;
    private static final List<Object[]> cues = new ArrayList<>();
    private static int at = 0;

    private AutoTest() {}

    public static void init() {
        mode = System.getProperty("hatman.autotest");
        if (mode == null || mode.isEmpty()) { mode = null; return; }
        HatmanMod.LOG.info("[AUTOTEST] enabled: {}", mode);
        build();
    }

    private static void cue(int delay, Runnable r) {
        at += delay;
        cues.add(new Object[]{at, r});
    }

    private static void srv(String cmd) {
        Minecraft mc = Minecraft.getInstance();
        var s = mc.getSingleplayerServer();
        if (s == null || mc.player == null) return;
        s.execute(() -> {
            ServerPlayer p = s.getPlayerList().getPlayer(mc.player.getUUID());
            if (p == null) return;
            if (cmd.equals("start")) Story.start(p);
            else Story.test(p, cmd);
        });
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        HatmanMod.LOG.info("[AUTOTEST] shot {} step={} t={}", name, Story.S.step, n);
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {});
    }

    private static void view(String name, double x, double y, double z, float yaw, float pitch) {
        cue(10, () -> srv("tp:" + x + "," + y + "," + z + "," + yaw + "," + pitch));
        cue(26, () -> shot(name));
    }

    private static void build() {
        at = 60;
        cue(0, () -> srv("start"));
        if (mode.contains("intro")) {
            for (int t : new int[]{90, 260, 400, 470, 590}) {
                final int tt = t;
                cues.add(new Object[]{at + t, (Runnable) () -> shot("intro_" + tt)});
            }
            at += 700;
        } else {
            cue(60, () -> Minecraft.getInstance().execute(() -> com.krolasyon.hatman.net.Net.toServer(com.krolasyon.hatman.net.Msg.of("cut_done").s("id", "intro"))));
            at += 60;
        }
        if (mode.contains("tour")) {
            cue(10, () -> srv("step:c5_walk"));
            at += 40;
            view("g_foyer", 22.5, 1, 28.5, 180, 5);
            view("g_foyer2", 27.5, 1, 19.5, 40, 5);
            view("g_corridor", 2.5, 1, 15.5, -90, 3);
            view("g_living", 12.5, 1, 19.5, 40, 8);
            view("g_living2", 2.5, 1, 28.5, -130, 8);
            view("g_dining", 32.0, 1, 19.0, -50, 10);
            view("g_dining2", 42.5, 1, 28.5, 120, 8);
            view("g_study", 12.5, 1, 11.5, 130, 8);
            view("g_stairhall", 16.5, 1, 11.5, -140, 0);
            view("g_kitchen", 40.5, 1, 11.5, 130, 8);
            view("b_hall", 16.5, -5, 11.5, -140, 4);
            view("b_workshop", 12.5, -5, 11.5, 130, 6);
            view("b_generator", 32.5, -5, 11.5, -140, 6);
            view("b_cellar", 22.5, -5, 28.5, 180, 4);
            view("b_cold", 7.5, -5, 15.5, 0, 6);
            view("f_corridor", 2.5, 7, 15.5, -90, 3);
            view("f_corridor2", 30.5, 7, 14.5, 40, 0);
            view("f_master", 12.5, 7, 19.0, 40, 8);
            view("f_dressing", 16.0, 7, 19.0, -40, 8);
            view("f_child", 42.5, 7, 19.0, 50, 8);
            view("f_bath", 5.5, 7, 11.5, 150, 8);
            view("f_guest", 9.0, 7, 11.5, -150, 8);
            view("f_stairhall", 16.5, 7, 11.5, -140, 6);
            view("f_library", 32.5, 7, 11.5, -140, 6);
            view("attic", 42.0, 13, 15.5, 90, 4);
            view("attic2", 12.0, 13, 20.0, 120, 4);
            view("yard_front", 22.5, 1, 50.5, 180, -12);
            view("yard_side", 48.0, 1, 46.0, 135, -10);
        }
        if (mode.contains("anim")) {
            cue(10, () -> srv("step:c5_walk"));
            cue(30, () -> srv("tp:22.5,1,27.5,180,-6"));
            cue(10, () -> srv("hm:22.5,1,22.5,0"));
            cue(40, () -> shot("anim_idle"));
            String[] acts = {"hat_tip", "knock", "scream", "grab", "search", "point", "twitch", "head_tilt", "talk_calm", "talk_angry", "lean_in", "offer", "bow", "emerge", "drop", "burst", "vanish"};
            for (String a : acts) {
                cue(26, () -> { srv("kill"); srv("hm:22.5,1,22.5,0"); });
                cue(16, () -> srv("act:" + a));
                int mid = a.equals("grab") || a.equals("twitch") || a.equals("burst") ? 6 : a.equals("emerge") ? 24 : 20;
                cue(mid, () -> shot("anim_" + a + "_a"));
                cue(a.equals("offer") || a.equals("bow") || a.equals("search") ? 30 : 12, () -> shot("anim_" + a + "_b"));
            }
            cue(20, () -> { srv("kill"); srv("hm:22.5,1,22.5,0"); });
            cue(10, () -> srv("flag:8:1"));
            cue(30, () -> shot("anim_sit"));
            cue(5, () -> { srv("flag:8:0"); srv("flag:16:1"); });
            cue(30, () -> shot("anim_freeze"));
            cue(5, () -> { srv("flag:16:0"); srv("tp:16.5,1,15.5,-90,-4"); srv("kill"); srv("hm:40.5,1,15.5,90"); });
            cue(20, () -> srv("move:19.5,1,15.5,2.0"));
            cue(40, () -> shot("anim_walk_a"));
            cue(14, () -> shot("anim_walk_b"));
            cue(5, () -> { srv("kill"); srv("hm:40.5,1,15.5,90"); });
            cue(10, () -> srv("move:19.5,1,15.5,5.4"));
            cue(22, () -> shot("anim_run_a"));
            cue(6, () -> shot("anim_run_b"));
            cue(10, () -> { srv("kill"); srv("tp:7.5,1,10.5,0,-4"); srv("hm:7.5,1,16.5,180"); });
            cue(10, () -> srv("move:7.5,1,11.5,1.6"));
            cue(30, () -> shot("anim_duck_a"));
            cue(10, () -> shot("anim_duck_b"));
            cue(10, () -> srv("kill"));
        }
        if (mode.contains("dark")) {
            cue(10, () -> srv("step:c2_evidence"));
            cue(10, () -> srv("power:off"));
            view("dark_corridor", 2.5, 1, 15.5, -90, 3);
            cue(5, () -> { srv("nv:on"); srv("hm:30.5,1,15.5,90,1"); });
            cue(60, () -> shot("nv_corridor"));
            cue(5, () -> { srv("kill"); srv("hm:10.5,1,15.5,90,2"); });
            cue(50, () -> shot("nv_close"));
            cue(5, () -> { srv("kill"); srv("nv:off"); srv("power:on"); srv("tp:12.5,7,19.0,40,8"); });
            cue(20, () -> srv("hide:master"));
            cue(40, () -> shot("hidden"));
            cue(5, () -> srv("tp:22.5,1,27.5,180,0"));
            cue(20, () -> srv("note:ledger"));
            cue(30, () -> shot("screen_note"));
            cue(5, () -> Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(null)));
            cue(10, () -> srv("keypad"));
            cue(20, () -> shot("screen_keypad"));
            cue(5, () -> Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(null)));
            cue(10, () -> { srv("tp:35.5,7,7.5,-90,0"); srv("step:c3_talk"); });
            cue(120, () -> shot("screen_dialogue_a"));
            cue(5, () -> Minecraft.getInstance().execute(() -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().screen.mouseClicked(0, 0, 0); }));
            cue(15, () -> Minecraft.getInstance().execute(() -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().screen.mouseClicked(0, 0, 0); }));
            cue(15, () -> Minecraft.getInstance().execute(() -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().screen.mouseClicked(0, 0, 0); }));
            cue(90, () -> shot("screen_dialogue_b"));
            cue(5, () -> srv("fx:jumpscare:40"));
            cue(12, () -> shot("screen_jumpscare"));
        }
        cue(60, () -> {
            HatmanMod.LOG.info("[AUTOTEST] finished");
            Minecraft.getInstance().stop();
        });
        cues.sort((a, b) -> Integer.compare((Integer) a[0], (Integer) b[0]));
    }

    public static void tick() {
        if (mode == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (!created && mc.screen instanceof TitleScreen && ++menuTicks > 40) {
                created = true;
                HatmanMod.LOG.info("[AUTOTEST] creating world");
                try {
                    TestWorld.create("hatman_test_" + System.currentTimeMillis() % 100000);
                } catch (Throwable t) {
                    HatmanMod.LOG.error("[AUTOTEST] world creation failed", t);
                    mc.stop();
                }
            }
            return;
        }
        n++;
        while (cursor < cues.size() && (Integer) cues.get(cursor)[0] <= n) {
            try {
                ((Runnable) cues.get(cursor)[1]).run();
            } catch (Throwable t) {
                HatmanMod.LOG.error("[AUTOTEST] cue failed", t);
            }
            cursor++;
        }
    }
}
