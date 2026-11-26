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

    /** a creature in front of the camera, held in a state, then an action, pictures of both */
    static void creature(String name, String kind, double x, double z, float yaw, double cx, double cy, double cz, float cyaw, float cpitch, int state, String action, int actionWait, int time) {
        cue(5, () -> cmd("kill @e[type=!player]"));
        cue(5, () -> cmd("time set " + time));
        cue(5, () -> cmd("execute in bloodsea:blood_sea run tp @s " + cx + " " + cy + " " + cz + " " + cyaw + " " + cpitch));
        cue(60, () -> cmd("execute in bloodsea:blood_sea run summon bloodsea:" + kind + " " + x + (kind.equals("changer") ? " 63 " : " 62.9 ") + z + " {Rotation:[" + yaw + "f,0f]}"));
        cue(5, () -> cmd("bloodsea debug state " + state));
        cue(100, () -> shot("c_" + name));
        if (action != null) {
            cue(5, () -> cmd("bloodsea debug state " + state + " " + action));
            cue(actionWait, () -> shot("c_" + name + "_" + action));
        }
    }

    /** wait for a cutscene to start, then pictures at these ticks into it */
    static void shots(String cut, int... at) {
        cue(1, () -> {});
        int base = at.length > 0 ? 0 : 0;
        int prev = 0;
        for (int a : at) {
            int d = a - prev;
            prev = a;
            cue(d, () -> shot("s_" + cut + "_" + String.format("%03d", Math.max(0, n))));
        }
        var c = com.krolasyon.bloodsea.story.Cuts.ALL.get(cut);
        cue(Math.max(60, (c == null ? 0 : c.length) - prev + 140), () -> {});
    }

    static boolean waiting;

    static void look(float yaw, float pitch) {
        var pl = Minecraft.getInstance().player;
        pl.setYRot(yaw); pl.yRotO = yaw; pl.setXRot(pitch); pl.xRotO = pitch;
    }

    static void lookAtNearest(Class<? extends net.minecraft.world.entity.Entity> c) {
        var mc = Minecraft.getInstance();
        var pl = mc.player;
        net.minecraft.world.entity.Entity best = null;
        for (var e : mc.level.entitiesForRendering())
            if (c.isInstance(e) && (best == null || e.distanceTo(pl) > best.distanceTo(pl))) best = e;
        if (best == null) { BloodSea.LOG.info("[AUTOTEST] nothing to look at"); return; }
        double dx = best.getX() - pl.getX(), dz = best.getZ() - pl.getZ(), dy = best.getY() + 1.2 - pl.getEyeY();
        look((float) (Math.atan2(dz, dx) * 180 / Math.PI) - 90f, (float) -(Math.atan2(dy, Math.hypot(dx, dz)) * 180 / Math.PI));
        BloodSea.LOG.info("[AUTOTEST] looking at {} {} blocks", best, best.distanceTo(pl));
    }

    static final class Story2 { static java.util.function.Supplier<float[]> saved; }

    static void waitFree() { cue(1, () -> waiting = true); }

    static void stageShots(String stage, String cut, int... at) {
        waitFree();
        cue(5, () -> cmd("bloodsea stage " + stage));
        cue(30, () -> {});
        shots(cut, at);
        cue(Math.max(60, 0), () -> {});
    }

    private static void build() {
        at = 60;
        if (mode.contains("video")) {
            // a two-minute look: the story starting by itself, a little rowing in first person, then scenes from later on
            cue(120, () -> { cmd("gamerule sendCommandFeedback false"); });
            waitFree();
            cue(20, () -> cmd("ride @s mount @e[type=bloodsea:raft,limit=1,sort=nearest]"));
            cue(10, () -> { Story2.saved = com.krolasyon.bloodsea.entity.RaftEntity.INPUT; com.krolasyon.bloodsea.entity.RaftEntity.INPUT = () -> new float[]{1f, 0.25f}; });
            cue(260, () -> com.krolasyon.bloodsea.entity.RaftEntity.INPUT = Story2.saved);
            cue(40, () -> {});
            for (String id : new String[]{"gift", "eye", "taken"}) {
                waitFree();
                cue(10, () -> cmd("bloodsea cut " + id));
                var c = com.krolasyon.bloodsea.story.Cuts.ALL.get(id);
                cue(c.length + 40, () -> {});
            }
            cue(20, () -> Minecraft.getInstance().stop());
            return;
        }
        if (mode.contains("death")) {
            // die in the story and come back on the raft, with the story going on
            cue(1100, () -> {});
            waitFree();
            cue(40, () -> { cmd("gamerule sendCommandFeedback false"); cmd("kill @s"); });
            cue(60, () -> shot("d_dead"));
            cue(20, () -> Minecraft.getInstance().player.respawn());
            cue(120, () -> { var mc = Minecraft.getInstance(); BloodSea.LOG.info("[DEATHTEST] dim={} pos={} screen={}", mc.level.dimension().location(), mc.player.position(), mc.screen); shot("d_back"); });
            cue(10, () -> { var sv = Minecraft.getInstance().getSingleplayerServer(); sv.execute(() -> { var sp = sv.getPlayerList().getPlayers().get(0); sp.getPersistentData().remove("bloodsea_story"); sp.getPersistentData().remove("bloodsea_began"); }); });
            cue(20, () -> cmd("kill @s"));
            cue(60, () -> Minecraft.getInstance().player.respawn());
            cue(120, () -> { var mc = Minecraft.getInstance(); BloodSea.LOG.info("[DEATHTEST2] dim={} pos={}", mc.level.dimension().location(), mc.player.position()); shot("d_back2"); });
            cue(20, () -> Minecraft.getInstance().stop());
            return;
        }
        if (mode.contains("night")) {
            // dusk, the Blood Moon rising, the night with its clock, and first light
            cue(1100, () -> {});
            waitFree();
            cue(20, () -> { cmd("gamerule sendCommandFeedback false"); cmd("time set 10800"); cmd("bloodsea stage dusk"); });
            cue(100, () -> look(90, -12f));
            cue(5, () -> shot("n_dusk1"));
            cue(300, () -> shot("n_dusk2"));
            waitFree();
            cue(60, () -> look(-90, -30f));
            cue(10, () -> shot("n_moonrise"));
            cue(200, () -> shot("n_night1"));
            waitFree();
            cue(100, () -> look(-90, -55f));
            cue(10, () -> shot("n_moon_high"));
            cue(120, () -> look(0, -5f));
            cue(5, () -> shot("n_night_sea"));
            cue(200, () -> shot("n_night_late"));
            waitFree();
            cue(300, () -> shot("n_after"));
            cue(900, () -> {});
            waitFree();
            cue(100, () -> shot("n_morning"));
            cue(20, () -> Minecraft.getInstance().stop());
            return;
        }
        if (mode.contains("showcase")) {
            // for a viewer: the drift music while rowing, the sun turning into the eye, then the omens one by one
            cue(1100, () -> {});
            waitFree();
            cue(20, () -> { cmd("gamerule sendCommandFeedback false"); cmd("time set 2600"); });
            cue(10, () -> look(-150, -8f));
            cue(200, () -> look(-120, -25f));
            cue(30, () -> look(-90, -40f));
            cue(100, () -> cmd("bloodsea omen blink"));
            cue(5, () -> com.krolasyon.bloodsea.client.WatcherClient.open(260));
            cue(300, () -> { cmd("time set 6000"); look(0, 0); });
            cue(5, () -> cmd("bloodsea omen ghost"));
            cue(60, () -> lookAtNearest(com.krolasyon.bloodsea.entity.ActorEntity.class));
            cue(200, () -> cmd("bloodsea omen rain"));
            cue(300, () -> cmd("bloodsea omen moan"));
            cue(240, () -> { cmd("time set 18000"); cmd("bloodsea omen lights"); });
            cue(300, () -> Minecraft.getInstance().stop());
            return;
        }
        if (mode.contains("omens")) {
            // the sun and the eye, then each omen, from the raft (after the opening scene has played)
            cue(1100, () -> {});
            waitFree();
            cue(20, () -> { cmd("gamerule sendCommandFeedback false"); cmd("time set 2600"); });
            cue(60, () -> look(-90, -40f));
            cue(40, () -> shot("o_sun_closed"));
            cue(5, () -> cmd("bloodsea omen blink"));
            cue(12, () -> shot("o_sun_crack"));
            cue(14, () -> shot("o_sun_opening"));
            cue(22, () -> shot("o_sun_open"));
            cue(5, () -> com.krolasyon.bloodsea.client.WatcherClient.open(200));
            cue(80, () -> shot("o_sun_open2"));
            cue(140, () -> { cmd("time set 6000"); look(0, 0); });
            cue(5, () -> cmd("bloodsea omen ghost"));
            cue(80, () -> lookAtNearest(com.krolasyon.bloodsea.entity.ActorEntity.class));
            cue(10, () -> shot("o_ghost"));
            cue(5, () -> cmd("bloodsea omen rain"));
            cue(200, () -> { look(Minecraft.getInstance().player.getYRot() + 120, 5); });
            cue(10, () -> shot("o_rain"));
            cue(5, () -> cmd("bloodsea omen moan"));
            cue(60, () -> shot("o_moan"));
            cue(60, () -> { cmd("time set 18000"); });
            cue(10, () -> cmd("bloodsea omen lights"));
            cue(60, () -> shot("o_lights"));
            cue(5, () -> look(Minecraft.getInstance().player.getYRot() + 180, 2));
            cue(30, () -> shot("o_lights2"));
            cue(40, () -> Minecraft.getInstance().stop());
            return;
        }
        if (mode.contains("auto")) {
            cue(200, () -> shot("a_start0"));
            cue(150, () -> shot("a_start1"));
            cue(300, () -> shot("a_start2"));
            cue(40, () -> Minecraft.getInstance().stop());
            return;
        }
        if (mode.contains("cuts")) {
            // every cutscene played on the raft, a picture from the middle of every shot
            cue(0, () -> { cmd("gamemode survival"); cmd("gamerule sendCommandFeedback false"); cmd("bloodsea start"); });
            {
                var w = com.krolasyon.bloodsea.story.Cuts.ALL.get("wake");
                int prev = -2, k = 0;
                for (var sh : w.shots) {
                    int mid = (sh.t0 + sh.t1) / 2 + 1;
                    int kk = k++;
                    cue(Math.max(1, mid - prev), () -> shot("k_wake_" + kk));
                    prev = mid;
                }
                cue(w.length - prev + 80, () -> {});
            }
            // row off and turn first, the way a player would, so the scenes start from a raft that moved and turned
            waitFree();
            cue(10, () -> cmd("ride @s mount @e[type=bloodsea:raft,limit=1,sort=nearest]"));
            cue(20, () -> { var keep = com.krolasyon.bloodsea.entity.RaftEntity.INPUT; com.krolasyon.bloodsea.entity.RaftEntity.INPUT = () -> new float[]{1f, 0.8f};
                Story2.saved = keep; });
            cue(170, () -> { com.krolasyon.bloodsea.entity.RaftEntity.INPUT = Story2.saved; BloodSea.LOG.info("[AUTOTEST] rowed to {} raft yaw {}", Minecraft.getInstance().player.position(),
                    Minecraft.getInstance().player.getVehicle() == null ? "none" : Minecraft.getInstance().player.getVehicle().getYRot()); });
            cue(40, () -> {});
            String only = System.getProperty("bloodsea.cuts", "");
            String[] ids = only.isEmpty() ? new String[]{"rule", "gift", "moon", "bump", "dawn", "mem1", "eye", "bones", "lev", "mem2", "voice", "mirage", "throne", "maw", "edge", "taken"}
                    : only.split(",");
            for (String id : ids) {
                waitFree();
                cue(10, () -> cmd("bloodsea cut " + id));
                var c = com.krolasyon.bloodsea.story.Cuts.ALL.get(id);
                int prev = 0, k = 0;
                for (var sh : c.shots) {
                    int mid = (sh.t0 + sh.t1) / 2 + 1;
                    int kk = k++;
                    cue(Math.max(1, mid - prev), () -> shot("k_" + id + "_" + kk));
                    prev = mid;
                }
                cue(c.length - prev + 60, () -> {});
            }
            cue(40, () -> Minecraft.getInstance().stop());
            return;
        }
        if (mode.contains("story")) {
            // the whole story, stage by stage, with pictures through every cutscene
            String ending = mode.contains("bone") ? "accept" : mode.contains("lev") ? "courage" : mode.contains("edge") ? "fury" : "hope";
            cue(0, () -> { cmd("gamemode survival"); cmd("bloodsea start"); });
            if (mode.contains("mid")) {
                cue(900, () -> {});
                waitFree(); cue(5, () -> cmd("bloodsea moveraft 800.5 0.5"));
                cue(60, () -> {});
                stageShots("east", "lev", 60, 160, 260, 340);
                cue(200, () -> {});
                shots("mem2", 120, 330);
                waitFree(); cue(5, () -> cmd("bloodsea moveraft -700.5 0.5"));
                cue(60, () -> {});
                stageShots("west", "voice", 60, 160, 260);
                stageShots("fog", "taken", 60, 130, 200, 250, 270, 300, 340, 420, 560);
                cue(300, () -> {});
            }
            if (mode.contains("full")) {
                shots("wake", 60, 150, 260, 330, 420, 520, 640, 760);
                cue(40, () -> cmd("execute as @s run ride @s mount @e[type=bloodsea:raft,limit=1,sort=nearest]"));
                stageShots("rowing", "rule", 40, 120, 220);
                stageShots("day1", "gift", 30, 120, 220);
                stageShots("dusk", "moon", 60, 180, 280);
                stageShots("night1", "bump", 30, 90, 160);
                stageShots("night1b", "dawn", 80, 200);
                shots("mem1", 120, 330, 420);
                stageShots("north", "eye", 60, 160, 260);
                waitFree(); cue(5, () -> cmd("bloodsea moveraft 0.5 -900.5"));
                cue(60, () -> {});
                stageShots("north2", "bones", 60, 160, 260, 380);
                waitFree(); cue(5, () -> cmd("bloodsea moveraft 800.5 0.5"));
                cue(60, () -> {});
                stageShots("east", "lev", 60, 160, 260, 340);
                cue(200, () -> {});
                shots("mem2", 120, 330);
                waitFree(); cue(5, () -> cmd("bloodsea moveraft -700.5 0.5"));
                cue(60, () -> {});
                stageShots("west", "voice", 60, 160, 260);
                stageShots("fog", "taken", 60, 130, 200, 250, 270, 300, 340, 420, 560);
                cue(300, () -> {});
            } else {
                cue(200, () -> {});
            }
            // the ending
            double[] goal = switch (ending) { case "accept" -> new double[]{0.5, -1380}; case "courage" -> new double[]{1370, 0.5}; case "fury" -> new double[]{0.5, 1870}; default -> new double[]{-1410, 0.5}; };
            cue(5, () -> cmd("bloodsea stage dest_" + ending));
            waitFree(); cue(5, () -> cmd("bloodsea moveraft " + goal[0] + " " + goal[1]));
            cue(80, () -> {});
            cue(10, () -> cmd("bloodsea stage dest"));
            for (int k = 0; k < 40; k++) { int kk = k; cue(60, () -> shot("z_" + ending + "_" + kk)); }
            cue(20, () -> Minecraft.getInstance().stop());
            return;
        }
        if (mode.contains("sea")) {
            cue(0, () -> { cmd("gamerule doDaylightCycle false"); cmd("gamerule doMobSpawning false"); cmd("time set 4000"); cmd("bloodsea enter"); cmd("gamemode survival"); });
            cue(120, () -> { Minecraft.getInstance().options.hideGui = true; shot("w_deck"); });
            cue(5, () -> { Minecraft mc = Minecraft.getInstance(); BloodSea.LOG.info("[SEATEST] on deck y={} vehicle={}", mc.player.getY(), mc.player.getVehicle()); });
            // walk to the edge: the guard should hold
            cue(5, () -> Minecraft.getInstance().options.keyUp.setDown(true));
            cue(40, () -> { Minecraft.getInstance().options.keyUp.setDown(false); BloodSea.LOG.info("[SEATEST] after walking y={} z={}", Minecraft.getInstance().player.getY(), Minecraft.getInstance().player.getZ()); });
            // sit at the oar and row
            cue(5, () -> cmd("execute as @s at @s run ride @s mount @e[type=bloodsea:raft,limit=1,sort=nearest]"));
            cue(20, () -> { com.krolasyon.bloodsea.entity.RaftEntity.INPUT = () -> new float[]{1f, 0.3f}; });
            cue(160, () -> { shot("w_rowing"); BloodSea.LOG.info("[SEATEST] rowing at {}", Minecraft.getInstance().player.position()); });
            cue(5, () -> { com.krolasyon.bloodsea.entity.RaftEntity.INPUT = () -> new float[]{0f, 0f}; cmd("ride @s dismount"); });
            cue(60, () -> cmd("time set 13500"));
            cue(80, () -> shot("w_dusk"));
            cue(5, () -> { cmd("gamemode spectator"); cmd("time set 5000"); });
            cue(5, () -> cmd("execute as @s at @e[type=bloodsea:raft,limit=1] run tp @s ~9 ~5 ~9 135 25"));
            cue(80, () -> shot("w_raft_view"));
            cue(5, () -> cmd("execute as @s at @e[type=bloodsea:raft,limit=1] run tp @s ~0 ~2.2 ~0 0 12"));
            cue(80, () -> shot("w_sea_far"));
            cue(5, () -> cmd("time set 17000"));
            cue(80, () -> shot("w_sea_night"));
            // into the blood
            cue(5, () -> { cmd("gamemode survival"); cmd("execute as @s at @e[type=bloodsea:raft,limit=1] run tp @s ~6 ~ ~6"); });
            cue(60, () -> BloodSea.LOG.info("[SEATEST] after blood alive={} health={}", Minecraft.getInstance().player.isAlive(), Minecraft.getInstance().player.getHealth()));
            cue(20, () -> Minecraft.getInstance().stop());
            return;
        }
        if (mode.contains("creatures")) {
            cue(0, () -> { cmd("gamerule doDaylightCycle false"); cmd("gamerule doMobSpawning false"); cmd("bloodsea enter"); cmd("gamemode spectator"); });
            cue(80, () -> { cmd("bloodsea debug freeze true"); Minecraft.getInstance().options.hideGui = true; });
            // the Watcher: the sun by day
            view("c_watcher", 0.5, 64.6, 0.5, -90, -50, 2500);
            creature("caller", "caller", 0.5, -14.5, 0, 3.5, 68.5, -4.5, 160, -16, 1, "scream", 18, 6000);
            creature("caller_side", "caller", 0.5, -14.5, 0, 12.5, 66.5, -10.5, 110, -14, 2, "lunge", 9, 6000);
            creature("caller_night", "caller", 0.5, -14.5, 0, 4.5, 67.5, -2.5, 160, -16, 3, null, 0, 18000);
            creature("gatherer", "gatherer", 0.5, -18.5, 0, 3.5, 70, -6.5, 165, -14, 1, "swipe", 12, 6000);
            creature("gatherer_head", "gatherer", 0.5, -18.5, 0, 1.5, 71.5, -12.5, 170, -8, 1, "roar", 20, 6000);
            creature("gatherer_night", "gatherer", 0.5, -18.5, 0, 8.5, 67, -9.5, 130, -16, 0, null, 0, 17000);
            creature("wake", "wake", 0.5, -20.5, -60, 8.5, 69, -8.5, 140, 26, 1, "lunge", 14, 6000);
            creature("changer", "changer", 2.5, 2.5, 200, 1.2, 64.2, 0.4, 330, 26, 0, null, 0, 6000);
            creature("changer_peek", "changer", 2.5, 2.5, 200, 1.2, 64.2, 0.4, 330, 26, 1, "reveal", 20, 6000);
            creature("changer_run", "changer", 2.5, 2.5, 200, 1.2, 64.2, 0.4, 330, 26, 2, "bite", 6, 6000);
            creature("leviathan", "leviathan", 0.5, -36.5, 0, 0.5, 70, 2.5, 180, -10, 1, "roar", 30, 6000);
            creature("leviathan_side", "leviathan", 0.5, -36.5, 0, 46.5, 72, -30.5, 100, -4, 1, "bite", 20, 6000);
            creature("leviathan_night", "leviathan", 0.5, -36.5, 0, 20.5, 70, 0.5, 145, -8, 1, "roar", 30, 17000);
            // the real thing: no freezing, the director and the brains; does it come for you?
            cue(5, () -> { cmd("kill @e[type=!player]"); cmd("bloodsea debug freeze false"); cmd("gamemode survival"); cmd("time set 16000"); });
            cue(5, () -> cmd("execute in bloodsea:blood_sea run tp @s 0.5 63 0.5 0 0"));
            cue(20, () -> cmd("bloodsea summon changer"));
            cue(300, () -> { shot("c_ai_changer"); BloodSea.LOG.info("[AITEST] health after changer {}", Minecraft.getInstance().player.getHealth()); });
            cue(5, () -> { cmd("kill @e[type=!player]"); cmd("effect give @s minecraft:instant_health 1 5"); cmd("bloodsea summon gatherer"); });
            cue(400, () -> { shot("c_ai_gatherer"); BloodSea.LOG.info("[AITEST] health after gatherer {}", Minecraft.getInstance().player.getHealth()); });
            cue(5, () -> { cmd("kill @e[type=!player]"); cmd("effect give @s minecraft:instant_health 1 5"); cmd("bloodsea summon caller"); });
            cue(400, () -> { shot("c_ai_caller"); BloodSea.LOG.info("[AITEST] health after caller {}", Minecraft.getInstance().player.getHealth()); });
            cue(5, () -> { cmd("kill @e[type=!player]"); cmd("effect give @s minecraft:instant_health 1 5"); cmd("bloodsea summon wake"); });
            cue(300, () -> { shot("c_ai_wake"); BloodSea.LOG.info("[AITEST] health after wake {}", Minecraft.getInstance().player.getHealth()); });
            cue(5, () -> { cmd("kill @e[type=!player]"); cmd("effect give @s minecraft:instant_health 1 5"); cmd("bloodsea summon leviathan"); });
            cue(160, () -> shot("c_ai_leviathan_rise"));
            cue(140, () -> { shot("c_ai_leviathan"); BloodSea.LOG.info("[AITEST] health after leviathan {} alive {}", Minecraft.getInstance().player.getHealth(), Minecraft.getInstance().player.isAlive()); });
            cue(40, () -> Minecraft.getInstance().stop());
            return;
        }
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
            if (mode.contains("menu")) {
                // the title screen itself: pictures of it, the sun closed and open
                if (mc.screen instanceof TitleScreen) {
                    menu++;
                    BloodSea.LOG.info("[MENUTEST] screen {}", menu == 1 ? mc.screen.getClass().getName() : "");
                    if (menu == 80) Screenshot.grab(mc.gameDirectory, "m_menu.png", mc.getMainRenderTarget(), c -> {});
                    if (menu == 82 && mc.screen instanceof TitleMenu tm) tm.forceEye();
                    if (menu == 150) Screenshot.grab(mc.gameDirectory, "m_menu_eye.png", mc.getMainRenderTarget(), c -> {});
                    if (menu == 160) mc.stop();
                }
                return;
            }
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
        if (waiting) {
            if (mc.screen instanceof CutScreen || mc.screen instanceof ChoiceScreen) {
                if (mc.screen instanceof ChoiceScreen cs && (mc.level.getGameTime() % 40 == 0)) cs.pick(0);
                return;
            }
            waiting = false;
        }
        n++;
        if (mc.screen instanceof ChoiceScreen cs && n % 40 == 0) { shot("choice_" + n); cs.pick(0); }
        if (mc.screen instanceof EndingScreen && n % 100 == 0) shot("ending_" + n);
        for (Object[] c : cues) if ((int) c[0] == n) {
            try { ((Runnable) c[1]).run(); } catch (Throwable t) { BloodSea.LOG.error("[AUTOTEST] cue failed", t); }
        }
    }
}
