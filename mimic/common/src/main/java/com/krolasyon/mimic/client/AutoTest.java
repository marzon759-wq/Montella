package com.krolasyon.mimic.client;

import com.krolasyon.mimic.MimicMod;
import com.krolasyon.mimic.compat.TestWorld;
import com.krolasyon.mimic.story.Story;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Development hook: with -Dmimic.autotest=<scenario> the client creates a world by itself, starts the story and plays
 * or photographs it. Does nothing at all in a normal game.
 */
public final class AutoTest {
    private static String mode = null;
    private static int menuTicks = 0, n = 0, noteT = 0;
    private static boolean created = false;
    private static final List<Object[]> cues = new ArrayList<>();
    private static int at = 0;
    static PlayDriver drive = null;
    private static String ansNode = null;
    private static int ansIdx = 0;

    static void pendingAnswer(String node, int idx) {
        ansNode = node;
        ansIdx = idx;
    }

    private AutoTest() {}

    public static void init() {
        mode = System.getProperty("mimic.autotest");
        if (mode == null || mode.isEmpty()) { mode = null; return; }
        MimicMod.LOG.info("[AUTOTEST] enabled: {}", mode);
        build();
    }

    static void cue(int delay, Runnable r) {
        at += delay;
        cues.add(new Object[]{at, r});
    }

    static void srv(String cmd) {
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

    static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        MimicMod.LOG.info("[AUTOTEST] shot {} step={} t={}", name, Story.S.step, n);
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {});
    }

    /** teleport (farm-local, eye level is +1.62) and take a picture */
    static void view(String name, double x, double y, double z, float yaw, float pitch) {
        cue(8, () -> srv("tp:" + x + "," + y + "," + z + "," + yaw + "," + pitch));
        cue(30, () -> shot(name));
    }

    private static void build() {
        at = 60;
        cue(0, () -> srv("start"));
        cue(40, () -> {});
        if (mode.contains("tour")) {
            cue(10, () -> srv("step:c4_silo"));
            cue(10, () -> srv("gm:spec"));
            cue(10, () -> srv("light:on"));
            cue(5, () -> Minecraft.getInstance().options.hideGui = true);
            view("t_road", 50.5, 6, 112, 180, 8);
            view("t_gate", 50.5, 1, 103, 180, 0);
            view("t_farm_air", 40, 30, 110, 180, 30);
            view("t_house_front", 25.5, 2, 82, 180, 4);
            view("t_barn_front", 70.5, 3, 62, 180, 6);
            view("t_silo", 96, 3, 40, 135, 10);
            view("t_shed", 44.5, 2, 38, 180, 4);
            view("t_corn", 76.5, 1, 56, 0, 4);
            view("t_scarecrows", 40, 2, 86, -60, 4);
            view("t_hall", 25.5, 1, 68.5, 180, 4);
            view("t_living", 22, 1, 67.5, 135, 12);
            view("t_dining", 22, 1, 58.5, 45, 12);
            view("t_kitchen", 28.2, 1, 59, -135, 10);
            view("t_study", 28.2, 1, 61.5, -45, 10);
            view("t_landing", 25.5, 6, 67, 180, 4);
            view("t_lily", 22, 6, 61.5, 135, 14);
            view("t_bath", 22, 6, 58.5, 45, 14);
            view("t_master", 28.2, 6, 61.5, -45, 12);
            view("t_cellar", 34, -4, 59, 135, 14);
            view("t_barn_in", 70.5, 1, 44, 180, 2);
            view("t_barn_up", 70.5, 1, 36, 180, -40);
            view("t_stalls", 67.5, 1, 22, 150, 12);
            view("t_loft", 64, 7, 44, 180, 12);
            view("t_workshop", 74.5, 1, 33.5, -150, 10);
            view("t_slaughter", 74.5, 1, 31, -160, 6);
            view("t_shed_in", 44.5, 1, 27, 180, 14);
            cue(5, () -> { srv("mm:70.5,6.2,30.5,180"); srv("sleep"); });
            view("m_sleeping", 70.5, 1, 35.5, 180, -45);
            cue(5, () -> { srv("kill"); srv("mm:25.5,6,53.5,0"); srv("flag:1024:1"); });
            view("m_landing", 25.5, 6, 61.5, 180, 0);
            cue(5, () -> { srv("kill"); srv("mm:30.5,1,56.5,180"); srv("act:roar"); });
            view("m_kitchen", 30.5, 1, 61.0, 180, 4);
            cue(5, () -> { srv("kill"); srv("truck"); srv("lily:44.5,1,49.5"); });
            view("m_truck", 41, 1, 52, -135, 4);
            view("t_silo_in", 86, 1, 31.5, -90, 6);
            cue(10, () -> srv("audit"));
            cue(60, () -> Minecraft.getInstance().stop());
            return;
        }
        if (mode.contains("gallery")) {
            cue(10, () -> srv("gm:spec"));
            cue(5, () -> Minecraft.getInstance().options.hideGui = true);
            cue(10, () -> srv("time:6000"));
            view("g_props_house", 25.5, 1, 68.5, 180, 8);
            cue(60, () -> Minecraft.getInstance().stop());
            return;
        }
        if (mode.contains("play")) {
            Qte.auto = true;
            Scenarios.play(mode);
            return;
        }
        cue(100, () -> Minecraft.getInstance().stop());
    }

    public static void tick() {
        if (mode == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (drive != null && mc.player != null && n > 80) drive.tick();
        if (mc.level == null || mc.player == null) {
            if (!created && mc.screen instanceof TitleScreen && ++menuTicks > 40) {
                created = true;
                MimicMod.LOG.info("[AUTOTEST] creating world");
                try {
                    TestWorld.create("mimic_test_" + System.currentTimeMillis() % 100000);
                } catch (Throwable t) {
                    MimicMod.LOG.error("[AUTOTEST] world creation failed", t);
                    mc.stop();
                }
            }
            return;
        }
        n++;
        if (ansNode != null && DialogueScreen.pending != null && ansNode.equals(DialogueScreen.pending.s("node"))) {
            MimicMod.LOG.info("[PLAY] decide {} -> {}", ansNode, ansIdx);
            srv("advance:" + ansIdx);
            ansNode = null;
        }
        // test runs read every note for a second and put it down again
        if (Qte.auto && mc.screen instanceof NoteScreen) {
            if (++noteT > 30) {
                noteT = 0;
                mc.setScreen(null);
            }
        } else noteT = 0;
        for (Object[] c : cues) {
            if ((int) c[0] == n) {
                try {
                    ((Runnable) c[1]).run();
                } catch (Throwable t) {
                    MimicMod.LOG.error("[AUTOTEST] cue failed", t);
                }
            }
        }
    }
}
