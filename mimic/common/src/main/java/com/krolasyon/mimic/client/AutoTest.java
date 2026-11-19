package com.krolasyon.mimic.client;

import com.krolasyon.mimic.MimicMod;
import com.krolasyon.mimic.compat.TestWorld;
import com.krolasyon.mimic.entity.MimicEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Development only (-Dmimic.autotest=...): makes a world, spawns the creature and photographs every animation. */
public final class AutoTest {
    private static String mode;
    private static boolean created;
    private static int menu, n, at;
    private static final List<Object[]> cues = new ArrayList<>();

    private AutoTest() {}

    public static void init() {
        mode = System.getProperty("mimic.autotest");
        if (mode == null || mode.isEmpty()) { mode = null; return; }
        MimicMod.LOG.info("[AUTOTEST] enabled: {}", mode);
        build();
    }

    private static void cue(int d, Runnable r) { at += d; cues.add(new Object[]{at, r}); }

    private static void srv(Consumer<ServerPlayer> c) {
        Minecraft mc = Minecraft.getInstance();
        var s = mc.getSingleplayerServer();
        if (s == null || mc.player == null) return;
        s.execute(() -> {
            ServerPlayer p = s.getPlayerList().getPlayer(mc.player.getUUID());
            if (p != null) c.accept(p);
        });
    }

    private static MimicEntity find(ServerPlayer p) {
        for (Entity e : p.serverLevel().getAllEntities()) if (e instanceof MimicEntity m) return m;
        return null;
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        MimicMod.LOG.info("[AUTOTEST] shot {}", name);
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {});
    }

    private static void view(double dx, double dy, double dz, float yaw, float pitch) {
        srv(p -> p.connection.teleport(0.5 + dx, 1 + dy, 0.5 + dz, yaw, pitch));
    }

    // ------------------------------------------------------------------ showcase recording: a smooth camera around the creature
    private static double[] camFrom, camTo;
    private static int camStart, camLen;

    private static void cam(int len, double[] from, double[] to) {
        cue(0, () -> { camFrom = from; camTo = to; camStart = n; camLen = len; });
    }

    private static double[] C(double x, double y, double z, double yaw, double pitch) { return new double[]{x, y, z, yaw, pitch}; }

    private static void camTick() {
        if (camFrom == null) return;
        var p = Minecraft.getInstance().player;
        double f = Math.min(1, (n - camStart) / (double) Math.max(1, camLen));
        f = f * f * (3 - 2 * f);
        double[] c = new double[5];
        for (int i = 0; i < 5; i++) c[i] = camFrom[i] + (camTo[i] - camFrom[i]) * f;
        p.setPos(0.5 + c[0], 1 + c[1], 0.5 + c[2]);
        p.setDeltaMovement(0, 0, 0);
        p.setYRot((float) c[3]);
        p.setXRot((float) c[4]);
        p.yRotO = p.getYRot();
        p.xRotO = p.getXRot();
        p.setYHeadRot((float) c[3]);
    }

    private static void act(String loop, String action) {
        cue(0, () -> srv(p -> {
            MimicEntity m = find(p);
            if (m == null) return;
            m.setLoop(loop);
            if (action != null) m.playAction(action);
        }));
    }

    private static void stage(ServerLevel l) {
        var dark = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        for (int x = -9; x <= 9; x++) for (int z = -9; z <= 9; z++) {
            l.setBlockAndUpdate(new BlockPos(x, 0, z), (x * 7 + z * 3 & 7) == 0 ? Blocks.MUD.defaultBlockState() : (x + z & 1) == 0 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : dark);
            for (int y = 1; y <= 7; y++) l.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
            boolean wall = Math.abs(x) == 9 || Math.abs(z) == 9;
            if (wall) for (int y = 1; y <= 6; y++) l.setBlockAndUpdate(new BlockPos(x, y, z), (x + z + y) % 4 == 0 ? Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
            l.setBlockAndUpdate(new BlockPos(x, 7, z), dark);
        }
        for (int[] h : new int[][]{{-7, 6}, {-6, 7}, {-7, 7}, {7, 6}, {6, 7}, {7, 7}, {-7, -7}}) {
            l.setBlockAndUpdate(new BlockPos(h[0], 1, h[1]), Blocks.HAY_BLOCK.defaultBlockState());
        }
        for (int[] c : new int[][]{{-3, 3}, {3, 3}, {-5, -6}, {5, -6}, {-1, 1}}) {
            for (int y = 6; y >= 4; y--) l.setBlockAndUpdate(new BlockPos(c[0], y, c[1]), Blocks.CHAIN.defaultBlockState());
            l.setBlockAndUpdate(new BlockPos(c[0], 3, c[1]), Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        }
        l.setBlockAndUpdate(new BlockPos(-8, 1, -2), Blocks.REDSTONE_BLOCK.defaultBlockState());
    }

    private static void setupStage() {
        cue(0, () -> srv(p -> {
            ServerLevel l = p.serverLevel();
            l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, l.getServer());
            l.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, l.getServer());
            l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, l.getServer());
            l.setDayTime(18000);
            p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            stage(l);
            MimicEntity m = MimicMod.MIMIC.get().create(l);
            m.moveTo(0.5, 1, 0.5, 180, 0);
            m.setYHeadRot(180);
            m.yBodyRot = 180;
            l.addFreshEntity(m);
        }));
    }

    private static void buildShow() {
        at = 60;
        setupStage();
        cue(60, () -> Minecraft.getInstance().options.hideGui = true);
        cue(0, () -> MimicMod.LOG.info("[AUTOTEST] MARK show {}", System.currentTimeMillis()));
        // 1. slow approach from the dark to the face
        act("idle", null);
        cam(160, C(0.00, 1.00, -5.44, 0, -8), C(0.00, 1.60, -2.60, 0, -10));
        cue(170, () -> {});
        // 2. it tilts its head and talks
        act("mimic_talk", null);
        cam(100, C(-0.80, 1.70, -2.40, -18, -12), C(0.80, 1.70, -2.40, 18, -12));
        cue(100, () -> {});
        // 3. the roar: pull back fast as the arms open
        act("idle", "roar");
        cam(40, C(0.00, 1.40, -3.00, 0, -14), C(0.00, 0.80, -4.42, 0, -12));
        cue(70, () -> {});
        act("spread", null);
        cam(90, C(0.00, 0.80, -4.42, 0, -12), C(-2.45, 0.50, -3.40, -36, -10));
        cue(90, () -> {});
        // 4. listening, sniffing, searching - three-quarter orbit
        act("listen", null);
        cam(80, C(3.06, 0.60, -2.38, 52, 0), C(2.58, 0.60, -2.86, 42, 0));
        cue(80, () -> {});
        act("sniff", null);
        cam(60, C(2.58, 0.60, -2.86, 42, 0), C(1.63, 0.40, -3.13, 27, 0));
        cue(60, () -> {});
        act("search", null);
        cam(90, C(-2.72, 0.60, -2.72, -45, 0), C(-1.36, 0.60, -3.40, -22, 0));
        cue(90, () -> {});
        // 5. movement cycles (in place), side views
        for (String a : new String[]{"walk", "stalk", "run", "crawl"}) {
            act(a, null);
            cam(70, C(4.08, 0.40, -0.41, 85, 0), C(3.81, 0.40, -1.22, 72, 0));
            cue(70, () -> {});
        }
        act("climb_wall", null);
        cam(70, C(0.00, 0.80, -4.08, 0, -10), C(0.00, 0.80, -3.67, 0, -12));
        cue(70, () -> {});
        act("bang", null);
        cam(60, C(2.38, 0.60, -2.38, 45, -4), C(2.04, 0.60, -2.72, 37, -4));
        cue(60, () -> {});
        act("eat", null);
        cam(70, C(1.70, 0.40, -2.04, 40, 10), C(1.02, 0.40, -2.31, 24, 10));
        cue(70, () -> {});
        act("twitch", null);
        cam(60, C(0.00, 1.40, -2.31, 0, -6), C(0.00, 1.40, -3.00, 0, -6));
        cue(60, () -> {});
        // 6. the grab and the kill, close, from the victim's eyes
        act("idle", "grab");
        cam(30, C(0.00, 0.80, -3.00, 0, -18), C(0.00, 0.80, -2.80, 0, -18));
        cue(40, () -> {});
        act("idle", "kill");
        cam(80, C(0.00, 1.70, -2.20, 0, -14), C(0.00, 1.70, -1.90, 0, -8));
        cue(85, () -> {});
        act("idle", "scream");
        cam(44, C(0.00, 1.70, -2.60, 0, -8), C(0.00, 1.70, -2.30, 0, -8));
        cue(50, () -> {});
        // 7. it goes into the floor and comes back out of it
        act("idle", "emerge");
        cam(70, C(-2.45, 0.60, -2.99, -40, 2), C(-1.77, 1.00, -2.99, -30, -6));
        cue(75, () -> {});
        act("idle", "death");
        cam(70, C(2.72, 0.60, -1.90, 55, 6), C(2.99, 0.80, -1.50, 62, 10));
        cue(80, () -> {});
        cue(0, () -> MimicMod.LOG.info("[AUTOTEST] MARK end {}", System.currentTimeMillis()));
        cue(20, () -> Minecraft.getInstance().stop());
    }

    private static void build() {
        if (mode.contains("show")) { buildShow(); return; }
        if (mode.contains("dbg")) {
            at = 60;
            setupStage();
            cue(60, () -> Minecraft.getInstance().options.hideGui = true);
            cam(5, C(3.8, 0.6, 0, 90, 0), C(3.8, 0.6, 0, 90, 0));
            act("walk", null);
            cue(20, () -> shot("d1_walk"));
            cue(7, () -> shot("d2_walk"));
            act("walk", "roar");
            cue(20, () -> shot("d3_roar"));
            cue(80, () -> shot("d4_after"));
            cue(7, () -> shot("d5_after"));
            act("crawl", null);
            cue(30, () -> shot("d6_crawl"));
            act("crawl", "grab");
            cue(60, () -> shot("d7_aftergrab"));
            cue(20, () -> Minecraft.getInstance().stop());
            return;
        }
        at = 60;
        cue(0, () -> srv(p -> {
            ServerLevel l = p.serverLevel();
            l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, l.getServer());
            l.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, l.getServer());
            l.setDayTime(6000);
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            for (int x = -12; x <= 12; x++) for (int z = -12; z <= 12; z++) {
                l.setBlockAndUpdate(new BlockPos(x, 0, z), (x + z & 1) == 0 ? Blocks.SPRUCE_PLANKS.defaultBlockState() : Blocks.DARK_OAK_PLANKS.defaultBlockState());
            }
            MimicEntity m = MimicMod.MIMIC.get().create(l);
            m.moveTo(0.5, 1, 0.5, 180, 0);
            m.setYHeadRot(180);
            m.yBodyRot = 180;
            l.addFreshEntity(m);
        }));
        cue(40, () -> Minecraft.getInstance().options.hideGui = true);
        // three views of the rest pose (front, three quarter, side)
        cue(10, () -> view(0, 0.6, -5.2, 0, 0));
        cue(25, () -> shot("m_front"));
        cue(5, () -> view(3.6, 0.6, -3.9, 43, 2));
        cue(25, () -> shot("m_34"));
        cue(5, () -> view(5.2, 0.6, 0, 90, 0));
        cue(25, () -> shot("m_side"));
        cue(5, () -> view(0, 1.4, -2.2, 0, -4));
        cue(25, () -> shot("m_face"));
        for (String a : MimicEntity.LOOPS) {
            cue(5, () -> { view(3.6, 0.6, -3.9, 43, 2); srv(p -> { MimicEntity m = find(p); if (m != null) m.setLoop(a); }); });
            cue(30, () -> shot("loop_" + a));
        }
        for (String a : MimicEntity.ACTIONS) {
            cue(5, () -> srv(p -> { MimicEntity m = find(p); if (m != null) { m.setLoop("idle"); m.playAction(a); } }));
            cue(a.equals("grab") ? 10 : 28, () -> shot("act_" + a));
        }
        cue(40, () -> Minecraft.getInstance().stop());
    }

    public static void tick() {
        if (mode == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            if (!created && mc.screen instanceof TitleScreen && ++menu > 40) {
                created = true;
                TestWorld.create("mimic_test_" + System.currentTimeMillis() % 100000);
            }
            return;
        }
        n++;
        camTick();
        for (Object[] c : cues) if ((int) c[0] == n) {
            try { ((Runnable) c[1]).run(); } catch (Throwable t) { MimicMod.LOG.error("[AUTOTEST] cue failed", t); }
        }
    }
}
