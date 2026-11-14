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
        } else if (!mode.contains("story") && !mode.contains("trailer") && !mode.contains("gameplay")) {
            cue(60, () -> Minecraft.getInstance().execute(() -> com.krolasyon.hatman.net.Net.toServer(com.krolasyon.hatman.net.Msg.of("cut_done").s("id", "intro"))));
            at += 60;
        } else {
            at += 40;
        }
        if (mode.contains("tour")) {
            cue(10, () -> srv("step:tour"));
            cue(10, () -> srv("power:on"));
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
            cue(10, () -> srv("step:tour"));
            cue(10, () -> srv("power:on"));
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
            cue(5, () -> { srv("flag:16:0"); srv("tp:22.5,1,19.2,180,-8"); srv("kill"); srv("hm:31.5,1,15.5,90"); });
            cue(20, () -> srv("move:12.5,1,15.5,2.0"));
            cue(70, () -> shot("anim_walk_a"));
            cue(8, () -> shot("anim_walk_b"));
            cue(8, () -> shot("anim_walk_c"));
            cue(5, () -> { srv("kill"); srv("hm:34.5,1,15.5,90"); });
            cue(12, () -> srv("move:10.5,1,15.5,5.4"));
            cue(34, () -> shot("anim_run_a"));
            cue(3, () -> shot("anim_run_b"));
            cue(3, () -> shot("anim_run_c"));
            cue(10, () -> { srv("kill"); srv("nv:on"); srv("tp:11.0,-5,34.5,-90,-4"); srv("hm:21.5,-5,34.5,90"); });
            cue(30, () -> shot("anim_duck_still"));
            cue(5, () -> srv("move:16.5,-5,34.5,3.0"));
            cue(22, () -> shot("anim_duck_move"));
            cue(5, () -> { srv("kill"); srv("hm:21.5,-5,34.5,90"); });
            cue(10, () -> srv("hunt"));
            cue(34, () -> shot("anim_crawl"));
            cue(5, () -> { srv("kill"); srv("nv:off"); });
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
            cue(10, () -> { srv("step:c3_talk"); srv("tp:34.5,7,9.5,-60,0"); });
            cue(120, () -> shot("screen_dialogue_a"));
            cue(5, () -> Minecraft.getInstance().execute(() -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().screen.mouseClicked(0, 0, 0); }));
            cue(15, () -> Minecraft.getInstance().execute(() -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().screen.mouseClicked(0, 0, 0); }));
            cue(15, () -> Minecraft.getInstance().execute(() -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().screen.mouseClicked(0, 0, 0); }));
            cue(90, () -> shot("screen_dialogue_b"));
            cue(5, () -> srv("fx:jumpscare:40"));
            cue(12, () -> shot("screen_jumpscare"));
        }
        if (mode.contains("story")) story();
        if (mode.contains("safe")) {
            cue(20, () -> { srv("give"); srv("step:c5_walk"); });
            tp(3.5, 7, 21.5); use("safe"); close(); cue(4, () -> srv("code:0731"));
            for (int i = 0; i < 12; i++) close();
            tp(36.5, 1, 19.5); at += 20; skipCut(); expect("c5_talk");
        }
        if (mode.contains("quick")) {
            cue(10, () -> srv("step:c3_talk"));
            cue(10, () -> srv("tp:34.5,7,9.5,-60,0"));
            cue(110, () -> shot("q_dialogue"));
            cue(2, () -> srv("advance:0"));
            cue(30, () -> shot("q_dialogue2"));
            cue(5, () -> srv("note:confession"));
            cue(30, () -> shot("q_note"));
            close();
            cue(10, () -> { srv("kill"); srv("step:c2_evidence"); srv("power:off"); srv("nv:on"); srv("tp:2.5,1,15.5,-90,0"); srv("hm:14.5,1,15.5,90,1"); });
            cue(60, () -> shot("q_nv"));
            cue(5, () -> srv("nv:off"));
        }
        if (mode.contains("audit")) {
            cue(20, () -> srv("audit"));
            cue(10, () -> { srv("step:tour"); srv("power:on"); srv("gm:spec"); srv("obj"); });
            view("ext_front", 22.5, 9, 62.0, 180, 8);
            view("ext_front2", 40.0, 6, 52.0, 140, 6);
            view("ext_side_e", 60.0, 10, 15.0, 90, 10);
            view("ext_back", 22.5, 12, -16.0, 0, 14);
            view("ext_side_w", -16.0, 10, 15.0, -90, 10);
            view("ext_aerial", 60.0, 34, 62.0, 135, 30);
            view("ext_porch", 22.5, 2, 37.0, 180, -10);
        }
        if (mode.contains("gameplay")) gameplay();
        if (mode.contains("cover")) cover();
        if (mode.contains("promoall")) { promo(); promo2(); }
        else if (mode.contains("promo2")) promo2();
        else if (mode.contains("promo")) promo();
        if (mode.contains("trailer2")) trailer2();
        else if (mode.contains("trailer")) trailer();
        if (mode.contains("finale")) for (String e : new String[]{"closed", "newface", "worn", "burn", "run"}) finale(e);
        if (!mode.contains("gameplay")) cue(60, () -> {
            HatmanMod.LOG.info("[AUTOTEST] finished");
            Minecraft.getInstance().stop();
        });
        cues.sort((a, b) -> Integer.compare((Integer) a[0], (Integer) b[0]));
    }

    private static void expect(String step) {
        cue(4, () -> {
            if (Story.S.step.equals(step)) HatmanMod.LOG.info("[AUTOTEST] OK   step {}", step);
            else {
                var pl = Minecraft.getInstance().player;
                HatmanMod.LOG.error("[AUTOTEST] EXPECT FAIL wanted {} got {} (t={}) player local {} {} {} screen={}", step, Story.S.step, Story.S.t,
                        pl == null ? 0 : pl.getX() - Story.S.ox, pl == null ? 0 : pl.getY() - Story.S.oy, pl == null ? 0 : pl.getZ() - Story.S.oz, Minecraft.getInstance().screen);
            }
        });
    }

    private static void client(Runnable r) { Minecraft.getInstance().execute(r); }

    private static void skipCut() {
        cue(12, () -> client(() -> {
            if (ClientState.cutId != null) com.krolasyon.hatman.net.Net.toServer(com.krolasyon.hatman.net.Msg.of("cut_done").s("id", ClientState.cutId));
            else HatmanMod.LOG.error("[AUTOTEST] EXPECT FAIL no cutscene running, step {}", Story.S.step);
        }));
        at += 8;
    }

    private static void close() {
        cue(8, () -> client(() -> { if (Minecraft.getInstance().screen != null) Minecraft.getInstance().setScreen(null); }));
        at += 4;
    }

    private static void use(String tag) { cue(6, () -> srv("use:" + tag)); }

    private static void tp(double x, double y, double z) { cue(6, () -> srv("tp:" + x + "," + y + "," + z + ",0,0")); }

    private static void talk(int choice, int times) {
        for (int i = 0; i < times; i++) cue(5, () -> srv("advance:" + choice));
    }

    /** plays the whole story through its server logic and reports every step that does not arrive */
    private static void story() {
        skipCut(); expect("c1_enter");
        tp(22.5, 1, 26); expect("c1_camp");
        tp(7.5, 1, 8); expect("c1_cam");
        use("camcorder"); expect("c1_file");
        use("note:case_file"); close(); expect("c1_key");
        use("drawer:item:cellar_key"); expect("c1_cellar");
        use("door:cellar");
        tp(27.5, -5, 2.5); expect("c1_fuel");
        use("drawer:item:fuel_can"); expect("c1_gen");
        use("generator"); at += 70; skipCut(); expect("c1_phone");
        use("phone"); talk(0, 10); at += 190; expect("c2_evidence");
        use("note:guest_list"); close(); use("drawer:note:maid"); close();
        tp(9.5, 1, 24.5); at += 10; skipCut(); at += 10; expect("c2_clock");
        use("clock"); expect("c2_workshop");
        use("door:workshop");
        tp(9.5, -5, 4.5); expect("c2_ledger");
        use("ledger"); close(); expect("c2_chase");
        tp(7.5, 1, 10); use("door:study"); expect("c2_door");
        at += 290; talk(0, 10); at += 360; expect("c3_up");
        use("door:stairgate");
        tp(22.5, 7, 15.5); at += 6; skipCut(); expect("c3_masks");
        use("plates"); close(); expect("c3_trail");
        cue(2, () -> srv("haunt:0"));
        use("drawer:note:diary"); close(); use("drawer:note:ruth_bag"); close(); use("drawer:note:mirror_note"); close();
        at += 10; expect("c3_master");
        tp(8.5, 7, 26.5); use("recorder"); at += 530; expect("c3_hide");
        cue(20, () -> srv("hide:master")); at += 860; expect("c3_sneak");
        cue(4, () -> srv("unhide"));
        tp(19.5, 7, 24.5); expect("c3_chase");
        use("door:library");
        tp(41.5, 7, 20.5); cue(4, () -> srv("hide:child")); at += 760; expect("c3_lib");
        cue(4, () -> srv("unhide"));
        tp(36.5, 7, 9.5); expect("c3_talk");
        at += 60; talk(0, 12); at += 230; expect("c4_attic");
        use("door:hatch");
        tp(17.5, 13, 10.5); at += 6; skipCut(); expect("c4_search");
        use("hatbox"); close();
        use("drawer:item:lamp_oil"); use("drawer:item:lighter");
        use("badge"); close(); expect("c4_mirrors"); at += 120; expect("c4_chase");
        tp(17.5, 7, 9.5); at += 10; tp(36.5, 1, 8.5); at += 50; tp(20.5, -5, 6.5); at += 10;
        tp(9.5, -5, 10.5); use("hatblock"); tp(7.5, -5, 16.5); expect("c4_cold");
        at += 60; talk(0, 8); at += 130; expect("c5_walk");
        tp(3.5, 7, 21.5); use("safe"); close(); cue(4, () -> srv("code:0731"));
        for (int i = 0; i < 12; i++) close();
        tp(36.5, 1, 19.5); at += 6; skipCut(); expect("c5_talk");
        talk(0, 8); at += 20;
        cue(4, () -> HatmanMod.LOG.info("[AUTOTEST] ending={} step={}", Story.S.ending, Story.S.step));
        skipCut(); expect("credits");
        close(); at += 20;
        cue(4, () -> HatmanMod.LOG.info("[AUTOTEST] story finished: active={} ending={} notes={} anger={} claim={}", Story.S.active, Story.S.ending, Story.S.notes.size(), Story.S.anger, Story.S.claim));
    }

    /** jumps to the last chapter and drives the final conversation to one ending */
    private static void finale(String ending) {
        cue(20, () -> { srv("give"); srv("mood:" + (ending.equals("newface") ? "10:80" : "0:0")); srv("step:c5_walk"); });
        at += 20;
        tp(36.5, 1, 19.5); at += 20; skipCut(); expect("c5_talk");
        // f1 -> f2x -> f3 choices: 0 face, 1 yes, 2 no / 3 bound-yes, 4 burn (indices of the node, not of the list)
        cue(6, () -> srv("advance:0"));
        cue(6, () -> srv("advance:0"));
        int c = switch (ending) { case "closed", "worn" -> 0; case "newface" -> 1; case "run" -> 2; default -> 4; };
        cue(6, () -> srv("advance:" + c));
        if (ending.equals("closed")) cue(6, () -> srv("advance:0"));
        if (ending.equals("worn")) cue(6, () -> srv("advance:1"));
        cue(6, () -> srv("advance:0"));
        at += 30;
        cue(2, () -> HatmanMod.LOG.info("[AUTOTEST] finale {} -> step={} ending={}", ending, Story.S.step, Story.S.ending));
        if (ending.equals("burn")) {
            expect("c5_fire");
            cue(20, () -> shot("finale_fire"));
            tp(22.5, 1, 55.5); at += 20; skipCut(); expect("credits");
        } else if (ending.equals("run")) {
            expect("c5_run");
            at += 160;
            cue(2, () -> shot("finale_run"));
            tp(10.5, 1, 41.5); at += 10; tp(22.5, 1, 55.5); at += 20; skipCut(); expect("credits");
        } else {
            cue(30, () -> shot("finale_" + ending));
            skipCut(); expect("credits");
        }
        cue(40, () -> shot("credits_" + ending));
        close(); at += 30;
        cue(2, () -> HatmanMod.LOG.info("[AUTOTEST] after credits active={}", Story.S.active));
        cue(10, () -> srv("start"));
        at += 60;
        cue(2, () -> client(() -> { if (ClientState.cutId != null) com.krolasyon.hatman.net.Net.toServer(com.krolasyon.hatman.net.Msg.of("cut_done").s("id", ClientState.cutId)); }));
        at += 30;
    }

    private static void mark(String name) {
        cue(0, () -> HatmanMod.LOG.info("[AUTOTEST] MARK {} {}", name, System.currentTimeMillis()));
    }

    /** a sequence of staged scenes, recorded from the screen for the promo video */
    private static void trailer() {
        // the intro cutscene plays from the start: give it its full length
        mark("intro");
        at += 690;
        // corridor: he stands at the far end, then walks, then runs
        cue(10, () -> { srv("step:tour"); srv("power:on"); srv("gm:spec"); srv("obj"); srv("kill"); });
        cue(10, () -> srv("tp:42.5,1,15.5,90,2"));
        cue(10, () -> { srv("hm:5.5,1,15.5,-90"); });
        at += 30;
        mark("corridor");
        at += 90;
        cue(0, () -> srv("move:30.5,1,15.5,1.6"));
        at += 150;
        cue(0, () -> srv("fx:static:8"));
        mark("chase");
        cue(4, () -> { srv("kill"); srv("hm:12.5,1,15.5,-90"); });
        cue(10, () -> srv("act:scream"));
        cue(40, () -> srv("move:41.5,1,15.5,5.6"));
        at += 120;
        cue(0, () -> srv("kill"));
        // night vision: dark corridor, he is only visible on the camera
        cue(5, () -> { srv("power:off"); srv("nv:on"); srv("tp:2.5,1,15.5,-90,0"); });
        cue(10, () -> srv("hm:24.5,1,15.5,90"));
        cue(5, () -> srv("flag:16:1"));
        at += 20;
        mark("nv");
        at += 90;
        cue(0, () -> { srv("kill"); srv("hm:9.5,1,15.5,90"); srv("fx:static:6"); });
        cue(4, () -> srv("flag:16:1"));
        cue(40, () -> srv("act:twitch"));
        at += 60;
        cue(0, () -> { srv("kill"); srv("nv:off"); srv("power:on"); srv("gm:adv"); });
        // the wardrobe
        cue(10, () -> srv("step:c3_hide"));
        cue(30, () -> srv("hide:master2"));
        mark("hide");
        at += 700;
        cue(0, () -> srv("unhide"));
        // the last supper, the question, the countdown, the chase
        cue(10, () -> { srv("kill"); srv("mood:0:0"); srv("step:c5_walk"); });
        cue(20, () -> srv("tp:36.5,1,19.5,0,0"));
        mark("supper");
        at += 410;
        mark("dialogue");
        cue(80, () -> srv("advance:0"));
        cue(90, () -> srv("advance:0"));
        cue(160, () -> srv("advance:2"));
        cue(70, () -> srv("advance:0"));
        mark("run");
        at += 260;
        mark("end");
    }

    private static void trailer2() {
        mark("intro");
        at += 700;
        cue(10, () -> { srv("power:off"); srv("cut:power"); });
        mark("power");
        at += 390;
        cue(10, () -> { srv("power:on"); srv("cut:tv"); });
        mark("tv");
        at += 360;
        cue(10, () -> { srv("step:tour"); srv("power:on"); srv("gm:spec"); srv("obj"); srv("kill"); });
        cue(10, () -> srv("tp:42.5,1,15.5,90,2"));
        cue(5, () -> srv("hm:27.5,1,15.5,-90"));
        cue(5, () -> srv("flag:16:1"));
        at += 20;
        mark("corridor");
        at += 70;
        cue(0, () -> { srv("fx:static:6"); srv("kill"); srv("hm:33.5,1,15.5,-90"); });
        cue(2, () -> srv("act:emerge"));
        mark("emerge");
        at += 60;
        cue(0, () -> srv("act:scream"));
        cue(36, () -> srv("move:41.8,1,15.5,5.6"));
        at += 50;
        mark("emerge_end");
        cue(0, () -> { srv("kill"); srv("gm:adv"); srv("cut:fitting"); });
        mark("fitting");
        at += 330;
        cue(0, () -> { srv("tp:22.5,1,27.5,180,-6"); srv("hm:22.5,1,21.5,0"); });
        at += 30;
        cue(0, () -> srv("act:hat_tip"));
        mark("hattip");
        at += 60;
        mark("end");
    }

    private static PlayDriver drive = null;

    /** a full playthrough driven like a player, for the gameplay video */
    private static void gameplay() {
        PlayDriver d = new PlayDriver(AutoTest::srv);
        drive = d;
        double[][] none = {};
        d.mark("intro").cutStart().srv("sflag:nohaunt").srv("speedcap:3.9").cutEnd()
                // ---------------- chapter 1
                .mark("enter").pitch(4)
                .walk(false, PlayDriver.P(22.5, 1, 45), PlayDriver.P(22.5, 1, 36))
                .srv("open:front")
                .walk(false, PlayDriver.P(22.5, 1, 32.5), PlayDriver.P(22.5, 1, 29), PlayDriver.P(22.5, 1, 26.5))
                .wait(40).look(22.5, 2.2, 30.5, 40).wait(30)
                .walk(false, PlayDriver.P(22.5, 1, 20), PlayDriver.P(22.5, 1, 15.5), PlayDriver.P(9, 1, 15.5), PlayDriver.P(7.5, 1, 15.2))
                .srv("open:study")
                .walk(false, PlayDriver.P(7.5, 1, 12.3), PlayDriver.P(6.5, 1, 6), PlayDriver.P(5.5, 1, 3.2))
                .wait(20).lookAtTag("camcorder", 25).use("camcorder").wait(90)
                .read("note:case_file", 110).step("c1_key").wait(20)
                .mark("corridor")
                .walk(false, PlayDriver.P(6.5, 1, 7), PlayDriver.P(7.5, 1, 12.3), PlayDriver.P(7.5, 1, 15.5), PlayDriver.P(12, 1, 15.5))
                .wait(30).cam(true).wait(20)
                .look(41.5, 3.0, 15.5, 90).wait(30)
                .walk(false, PlayDriver.P(20, 1, 15.5), PlayDriver.P(22.5, 1, 17.5), PlayDriver.P(18.5, 1, 23.5))
                .cam(false).lookAtTag("drawer:item:cellar_key", 20).use("drawer:item:cellar_key").wait(40)
                .step("c1_cellar")
                .mark("cellar")
                .walk(false, PlayDriver.P(22.5, 1, 17.5), PlayDriver.P(22.5, 1, 14.5), PlayDriver.P(22.5, 1, 12.3), PlayDriver.P(27.5, 1, 12.0), PlayDriver.P(28.5, 1, 11.4))
                .use("door:cellar").srv("open:cellar").cam(true).pitch(12)
                .walk(false, PlayDriver.P(28.5, 1, 9.5), PlayDriver.P(28.0, 0, 8), PlayDriver.P(28.0, -3, 5), PlayDriver.P(28.0, -5, 2.4))
                .pitch(3).wait(70)
                .walk(false, PlayDriver.P(24.5, -5, 4.5), PlayDriver.P(22.5, -5, 10.5), PlayDriver.P(22.5, -5, 12.2))
                .srv("open:winecellar")
                .walk(false, PlayDriver.P(22.5, -5, 14.5), PlayDriver.P(21.5, -5, 22), PlayDriver.P(20.5, -5, 29.0))
                .lookAtTag("drawer:item:fuel_can", 20).use("drawer:item:fuel_can").wait(30).step("c1_gen")
                .mark("generator")
                .walk(false, PlayDriver.P(21.5, -5, 22), PlayDriver.P(22.5, -5, 13.5), PlayDriver.P(26.5, -5, 11.6), PlayDriver.P(29.5, -5, 11.5))
                .srv("open:generator")
                .walk(false, PlayDriver.P(31.5, -5, 11.5), PlayDriver.P(35.5, -5, 7.5), PlayDriver.P(37.5, -5, 4.2))
                .lookAtTag("generator", 20).use("generator").cam(false).cutStart().mark("power").cutEnd()
                // ---------------- the telephone
                .mark("phone").tp(22.5, 1, 20.5, 180).pitch(4)
                .walk(false, PlayDriver.P(19.5, 1, 22.0), PlayDriver.P(18.0, 1, 22.3))
                .lookAtTag("phone", 25).use("phone").talk(0, 0).step("c2_evidence").wait(40)
                // ---------------- chapter 2
                .mark("tv")
                .walk(false, PlayDriver.P(22.5, 1, 17.5), PlayDriver.P(22.5, 1, 15.5), PlayDriver.P(8.5, 1, 15.5), PlayDriver.P(7.5, 1, 16.2))
                .srv("open:living")
                .walk(false, PlayDriver.P(7.5, 1, 18.5), PlayDriver.P(9.5, 1, 20.8), PlayDriver.P(11.5, 1, 21.2))
                .cutStart().cutEnd().wait(20)
                .use("note:guest_list").wait(10).run(() -> Minecraft.getInstance().setScreen(null)).use("drawer:note:maid").wait(10).run(() -> Minecraft.getInstance().setScreen(null))
                .step("c2_clock").srv("tp:27.0,1,23.5,-90,6").wait(5).lookAtTag("clock", 25).use("clock").wait(120)
                .mark("workshop").tp(16.5, -5, 6.5, 90).srv("open:workshop").pitch(3)
                .walk(false, PlayDriver.P(13.5, -5, 6.5), PlayDriver.P(11.5, -5, 4.5), PlayDriver.P(10.5, -5, 3.0))
                .read("ledger", 120).step("c2_chase")
                .mark("chase1")
                .walk(true, PlayDriver.P(12.5, -5, 6.5), PlayDriver.P(15.5, -5, 6.5), PlayDriver.P(23.5, -5, 3.0), PlayDriver.P(27.5, -5, 2.4),
                        PlayDriver.P(27.5, -1, 6.5), PlayDriver.P(27.5, 0, 8.4), PlayDriver.P(28.5, 1, 9.6), PlayDriver.P(28.5, 1, 11.4), PlayDriver.P(26.5, 1, 12.2),
                        PlayDriver.P(22.5, 1, 12.6), PlayDriver.P(22.5, 1, 15.5), PlayDriver.P(9.5, 1, 15.5), PlayDriver.P(7.5, 1, 14.6), PlayDriver.P(7.5, 1, 12.2))
                .use("door:study").step("c2_door").walk(false, PlayDriver.P(6.5, 1, 9.5)).look(7.5, 2.0, 13.0, 30)
                .talk(0, 0).step("c3_up")
                // ---------------- chapter 3
                .mark("upstairs").pitch(4)
                .walk(false, PlayDriver.P(7.5, 1, 12.3), PlayDriver.P(7.5, 1, 15.5), PlayDriver.P(21.5, 1, 15.5), PlayDriver.P(22.5, 1, 12.6))
                .use("door:stairgate").pitch(-12)
                .walk(false, PlayDriver.P(22.5, 1, 11.2), PlayDriver.P(22.5, 3, 8.5), PlayDriver.P(22.5, 6, 5.2), PlayDriver.P(22.5, 7, 3.5))
                .pitch(3).cutStart().mark("masquerade").cutEnd()
                .walk(false, PlayDriver.P(22.5, 7, 12.5), PlayDriver.P(25.0, 7, 15.0))
                .read("plate_ruth", 90).step("c3_trail")
                .walk(false, PlayDriver.P(36.5, 7, 15.5), PlayDriver.P(37.5, 7, 16.0)).srv("open:child")
                .walk(false, PlayDriver.P(37.5, 7, 18.5), PlayDriver.P(35.0, 7, 20.2))
                .wait(30).read("drawer:note:diary", 110)
                .use("drawer:note:ruth_bag").wait(8).run(() -> Minecraft.getInstance().setScreen(null)).use("drawer:note:mirror_note").wait(8).run(() -> Minecraft.getInstance().setScreen(null))
                .step("c3_master")
                .mark("master").tp(7.5, 7, 15.5, 0)
                .walk(false, PlayDriver.P(7.5, 7, 18.5), PlayDriver.P(8.5, 7, 24.5), PlayDriver.P(8.5, 7, 27.2))
                .lookAtTag("recorder", 20).use("recorder").step("c3_hide")
                .walk(true, PlayDriver.P(10.5, 7, 22.5), PlayDriver.P(12.2, 7, 21.5)).lookAtTag("wardrobe:master2", 10).use("wardrobe:master2")
                .mark("hide").step("c3_sneak").wait(40).srv("unhide").wait(10)
                .srv("open:dress")
                .walk(false, PlayDriver.P(13.0, 7, 24.5), PlayDriver.P(16.0, 7, 24.5), PlayDriver.P(18.6, 7, 24.5))
                .step("c3_chase").mark("chase2")
                .srv("open:dressing")
                .walk(true, PlayDriver.P(21.5, 7, 22.0), PlayDriver.P(22.5, 7, 18.5), PlayDriver.P(22.5, 7, 15.5), PlayDriver.P(36.5, 7, 15.5), PlayDriver.P(37.5, 7, 14.4))
                .use("door:library").srv("open:child")
                .walk(true, PlayDriver.P(37.5, 7, 16.6), PlayDriver.P(38.5, 7, 19.5), PlayDriver.P(42.0, 7, 20.5))
                .use("wardrobe:child").step("c3_lib").wait(30).srv("unhide").wait(10)
                .walk(false, PlayDriver.P(39.5, 7, 19.0), PlayDriver.P(37.5, 7, 16.5), PlayDriver.P(37.5, 7, 13.5), PlayDriver.P(37.5, 7, 11.0))
                .mark("library").talk(0, 0, 0).step("c4_attic")
                // ---------------- chapter 4
                .mark("attic").tp(22.5, 7, 12.5, 90).srv("open:hatch").pitch(-30)
                .walk(false, PlayDriver.P(17.0, 7, 9.5), PlayDriver.P(16.2, 7, 9.5), PlayDriver.L(15.5, 13, 9.5))
                .walk(false, PlayDriver.P(17.0, 13, 9.5)).pitch(3).cutStart().cutEnd()
                .walk(false, PlayDriver.P(19.5, 13, 9.2)).read("badge", 110).step("c4_mirrors").step("c4_chase").mark("chase3").wait(30)
                .walk(true, PlayDriver.P(17.5, 13, 9.5), PlayDriver.P(16.2, 13, 9.5))
                .wait(40)
                .tp(13.0, -5, 10.5, 180).lookAtTag("hatblock", 15).use("hatblock").wait(20)
                .walk(true, PlayDriver.P(7.5, -5, 12.0), PlayDriver.P(7.5, -5, 16.0)).step("c4_cold")
                .mark("ruth").walk(false, PlayDriver.P(7.5, -5, 17.6)).talk(2).step("c5_walk")
                // ---------------- chapter 5
                .mark("safe").tp(3.5, 7, 21.0, 90).wait(10).lookAtTag("safe", 20).use("safe").code("0731")
                .until(() -> Minecraft.getInstance().screen instanceof NoteScreen, 120, "safe note")
                .add(a -> { if (!(Minecraft.getInstance().screen instanceof NoteScreen ns)) return true; if (a > 0 && a % 110 == 0) ns.mouseClicked(0, 0, 0); return false; })
                .mark("supper").tp(37.5, 1, 15.5, 0).pitch(3)
                .walk(false, PlayDriver.P(37.5, 1, 16.6)).srv("open:dining").walk(false, PlayDriver.P(37.5, 1, 19.3))
                .cutStart().cutEnd().mark("final").talk(0, 0).cutStart().mark("ending").cutEnd()
                .until(() -> Minecraft.getInstance().screen instanceof CreditsScreen, 200, "credits").mark("credits").wait(500)
                .run(() -> { HatmanMod.LOG.info("[AUTOTEST] finished"); Minecraft.getInstance().stop(); });
    }

    private static void gui(boolean hide) { cue(0, () -> client(() -> Minecraft.getInstance().options.hideGui = hide)); }

    private static void pose(String name, String setup, double x, double y, double z, float yaw, float pitch, int settle) {
        cue(4, () -> { srv("kill"); for (String c : setup.split(";")) if (!c.isEmpty()) srv(c); });
        cue(6, () -> srv("tp:" + x + "," + y + "," + z + "," + yaw + "," + pitch));
        cue(settle, () -> shot(name));
    }

    /** posed screenshots for the mod page */
    private static void promo() {
        cue(60, () -> client(() -> { if (ClientState.cutId != null) com.krolasyon.hatman.net.Net.toServer(com.krolasyon.hatman.net.Msg.of("cut_done").s("id", ClientState.cutId)); }));
        cue(10, () -> { srv("sflag:nohaunt"); srv("step:tour"); srv("gm:spec"); });
        at += 200;
        cue(0, () -> { srv("power:on"); srv("obj"); });
        gui(true);
        // the hero: he stands in the opening at the end of the hall
        pose("p_foyer", "hm:22.5,1,18.2,0", 22.5, 1.2, 27.8, 180, -9, 50);
        cue(4, () -> srv("act:hat_tip"));
        cue(22, () -> shot("p_hattip"));
        // supper
        pose("p_dining", "hm:42.5,1,24.5,90;flag:8:1", 32.2, 1.55, 24.5, -90, 5, 60);
        pose("p_dining2", "hm:42.5,1,24.5,90;flag:8:1", 34.0, 2.6, 20.0, -55, 18, 40);
        // scream in the corridor
        pose("p_scream", "hm:24.5,1,15.5,-90", 31.5, 1.0, 15.5, 90, -14, 30);
        cue(2, () -> srv("act:scream"));
        cue(16, () -> shot("p_scream"));
        // emerging from the floor
        pose("p_emerge0", "hm:25.5,1,15.5,-90", 32.5, 1.0, 15.5, 90, -8, 10);
        cue(2, () -> srv("act:emerge"));
        cue(30, () -> shot("p_emerge"));
        // the masks
        pose("p_masks", "hm:30.5,7,15.5,90", 2.5, 7.3, 15.4, -90, -2, 40);
        // the attic fitting room
        pose("p_attic", "hm:6.5,13,13.5,-60", 16.5, 13.4, 16.5, 120, -4, 40);
        // the grab, close
        pose("p_grab0", "hm:24.5,1,15.5,-90", 28.2, 1.0, 15.5, 90, -18, 10);
        cue(2, () -> srv("act:grab"));
        cue(5, () -> shot("p_grab"));
        // outside, lightning
        pose("p_exterior", "hm:22.5,1,33.6,0", 9.5, 3.5, 48.5, 155, -6, 40);
        cue(2, () -> srv("fx:flash:8"));
        cue(3, () -> shot("p_exterior_flash"));
        pose("p_exterior2", "hm:22.5,1,33.6,0", 22.5, 1.6, 50.5, 180, -8, 40);
        // night vision
        cue(4, () -> { srv("power:off"); srv("gm:spec"); });
        gui(false);
        pose("p_nv", "nv:on;hm:15.5,1,15.5,90;flag:16:1", 2.5, 1.0, 15.5, -90, -4, 60);
        pose("p_nv2", "nv:on;hm:8.5,1,15.5,90;act:twitch", 2.5, 1.0, 15.5, -90, -10, 14);
        cue(4, () -> { srv("nv:off"); srv("power:on"); srv("kill"); });
        // the conversation in the library
        cue(10, () -> { srv("gm:adv"); srv("step:c3_talk"); srv("tp:34.6,7,9.6,-60,-10"); });
        at += 175;
        cue(0, () -> shot("p_dialogue"));
        cue(5, () -> srv("advance:0"));
        at += 120;
        cue(0, () -> shot("p_dialogue2"));
        cue(10, () -> { srv("note:confession"); });
        cue(30, () -> shot("p_note"));
        close();
    }

    private static void promo2() {
        cue(60, () -> client(() -> { if (ClientState.cutId != null) com.krolasyon.hatman.net.Net.toServer(com.krolasyon.hatman.net.Msg.of("cut_done").s("id", ClientState.cutId)); }));
        cue(10, () -> { srv("sflag:nohaunt"); srv("step:tour"); srv("gm:spec"); });
        at += 200;
        cue(0, () -> { srv("power:on"); srv("obj"); });
        gui(true);
        // low angle hero shots: he looms
        pose("h_hero", "hm:22.5,1,19.0,0", 22.5, 0.6, 23.4, 180, -32, 50);
        pose("h_hero2", "hm:22.5,1,19.0,0", 21.3, 0.4, 22.2, 200, -38, 30);
        cue(2, () -> srv("act:hat_tip"));
        cue(22, () -> shot("h_hattip"));
        pose("h_lean", "hm:22.5,1,19.4,0", 22.5, 1.0, 22.6, 180, -12, 10);
        cue(2, () -> srv("act:lean_in"));
        cue(38, () -> shot("h_lean"));
        pose("h_offer", "hm:22.5,1,19.4,0", 22.5, 1.0, 23.4, 180, -14, 10);
        cue(2, () -> srv("act:offer"));
        cue(64, () -> shot("h_offer"));
        // at the head of the table, seen past the guests
        pose("h_table", "hm:42.5,1,24.5,90;flag:8:1", 37.5, 2.3, 22.1, -75, 12, 50);
        pose("h_table2", "hm:42.5,1,24.5,90;flag:8:1", 39.6, 1.6, 26.6, -125, 8, 40);
        // attic, lit
        pose("h_attic", "hm:12.5,13,15.5,-90;flag:16:1", 18.5, 13.3, 13.0, 110, -10, 40);
        // crawling in the cellar tunnel under night vision
        gui(false);
        pose("h_crawl", "nv:on;hm:18.5,-5,34.5,90;hunt", 11.2, -5, 34.5, -90, 0, 30);
        cue(4, () -> { srv("nv:off"); srv("kill"); });
    }

    /** dramatic close-ups for the cover image */
    private static void cover() {
        cue(60, () -> client(() -> { if (ClientState.cutId != null) com.krolasyon.hatman.net.Net.toServer(com.krolasyon.hatman.net.Msg.of("cut_done").s("id", ClientState.cutId)); }));
        cue(10, () -> { srv("sflag:nohaunt"); srv("step:tour"); srv("gm:spec"); });
        at += 200;
        cue(0, () -> { srv("power:on"); srv("obj"); });
        gui(true);
        String hm = "hm:24.5,1,15.5,-90";
        pose("c_up", hm, 26.4, 0.2, 15.5, 90, -55, 40);
        pose("c_up2", hm, 26.0, 0.5, 15.1, 80, -50, 40);
        pose("c_lean0", hm, 26.7, 1.0, 15.5, 90, -6, 6);
        cue(2, () -> srv("act:lean_in"));
        cue(44, () -> shot("c_lean"));
        pose("c_tilt0", hm, 26.2, 1.3, 15.5, 90, -40, 6);
        cue(2, () -> srv("act:head_tilt"));
        cue(16, () -> shot("c_tilt"));
        pose("c_scream0", hm, 27.6, 0.4, 15.5, 90, -32, 6);
        cue(2, () -> srv("act:scream"));
        cue(15, () -> shot("c_scream"));
        pose("c_grab0", hm, 27.2, 0.8, 15.5, 90, -24, 6);
        cue(2, () -> srv("act:grab"));
        cue(6, () -> shot("c_grab"));
        // the same, in the dark, through the camera
        cue(4, () -> { srv("power:off"); });
        gui(false);
        pose("c_nv", "nv:on;" + hm + ";flag:16:1", 26.6, 0.6, 15.5, 90, -45, 50);
        pose("c_nv_lean0", "nv:on;" + hm, 26.7, 1.0, 15.5, 90, -6, 6);
        cue(2, () -> srv("act:lean_in"));
        cue(44, () -> shot("c_nv_lean"));
        cue(4, () -> { srv("nv:off"); });
        gui(true);
        // silhouette: dark corridor, one lamp behind him
        pose("c_dark", hm, 27.5, 0.8, 15.5, 90, -28, 40);
        cue(4, () -> srv("power:on"));
    }

    public static void tick() {
        if (mode == null) return;
        if (drive != null && Minecraft.getInstance().player != null && n > 80) drive.tick();
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
