package com.krolasyon.kaiju.client;

import com.krolasyon.kaiju.KaijuMod;
import com.krolasyon.kaiju.entity.KaijuEntity;
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
 * Development only: with -Dkaiju.autotest=&lt;scenario&gt; the client makes a flat test world with a little town, sets two
 * kaiju on each other and photographs the fight; it also forces a beam clash and drives one kaiju as a rider.
 * Does nothing in a normal game.
 */
public final class AutoTest {
    static String mode;
    static boolean created;
    static int menu, n, at;
    static double camDist = 150, camAng = 0, camUp = 22;
    static final List<Object[]> cues = new ArrayList<>();

    public static void init() {
        mode = System.getProperty("kaiju.autotest");
        if (mode == null || mode.isEmpty()) { mode = null; return; }
        KaijuMod.LOG.info("[AUTOTEST] {}", mode);
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent e) -> { if (e.phase == TickEvent.Phase.END) tick(); });
        build();
    }

    static void cue(int delay, Runnable r) { at += delay; cues.add(new Object[]{at, r}); }

    static void cmd(String c) {
        Minecraft mc = Minecraft.getInstance();
        var s = mc.getSingleplayerServer();
        if (s == null || mc.player == null) return;
        s.execute(() -> {
            ServerPlayer p = s.getPlayerList().getPlayer(mc.player.getUUID());
            if (p != null) s.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4).withSuppressedOutput(), c);
        });
    }

    static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        KaijuMod.LOG.info("[AUTOTEST] shot {}", name);
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {});
    }

    static void ride() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        var v = mc.player.getVehicle();
        KaijuMod.LOG.info("[RIDE] vehicle={} player={},{},{} rot={},{} kaiju={}", v == null ? "none" : v.getId(), (int) mc.player.getX(), (int) mc.player.getY(), (int) mc.player.getZ(),
                (int) mc.player.getYRot(), (int) mc.player.getXRot(), v == null ? "-" : (int) v.getX() + "," + (int) v.getY() + "," + (int) v.getZ());
    }

    static void status() {
        Minecraft mc = Minecraft.getInstance();
        var s = mc.getSingleplayerServer();
        if (s == null) return;
        s.execute(() -> {
            for (var lvl : s.getAllLevels())
                for (KaijuEntity k : lvl.getEntitiesOfClass(KaijuEntity.class, new net.minecraft.world.phys.AABB(-2000, -100, -2000, 2000, 400, 2000)))
                    KaijuMod.LOG.info("[STATUS] #{} t={} {} hp={} st={} clip={} pos={},{},{} want={} knock={} ground={} retreat={} tgt={}", k.getId(), k.tickCount, k.state, (int) k.hp, (int) k.stamina,
                            k.clip.name, (int) k.getX(), String.format("%.1f", k.getY()), (int) k.getZ(), k.wantSpeed, String.format("%.2f", k.knock.horizontalDistance()),
                            k.groundAt(k.position()), k.brain.retreatTicks, k.brain.target == null ? -1 : k.brain.target.getId());
        });
    }

    static void demo() {
        // one kaiju next to the town: tear a house off and throw it, uproot a tree, lose an arm and grow it back
        String K = "@e[type=kaiju:atomic_kaiju,limit=1,sort=nearest]";
        cue(30, () -> { cmd("kaiju ai false"); cmd("kill @e[type=kaiju:atomic_kaiju,limit=1,sort=furthest]"); });
        cue(20, () -> cmd("tp " + K + " -4 -60 122 0 0"));
        cue(20, () -> cmd("tp @s 45 -35 102 facing -4 -45 135"));
        cue(30, () -> shot("d00_stand"));
        cue(5, () -> cmd("kaiju attack grab_chunk"));
        cue(25, () -> shot("d20_reach"));
        cue(24, () -> shot("d21_lift"));
        cue(20, () -> shot("d22_hold"));
        cue(10, () -> cmd("kaiju attack throw_held"));
        cue(17, () -> shot("d23_throw"));
        cue(6, () -> shot("d24_fly"));
        cue(8, () -> shot("d25_fly2"));
        cue(25, () -> shot("d26_impact"));
        cue(20, () -> { cmd("tp " + K + " 60 -60 140 -90 0"); cmd("place feature minecraft:oak 77 -60 140"); cmd("place feature minecraft:oak 79 -60 146"); cmd("tp @s 62 -40 98 facing 76 -42 140"); });
        cue(20, () -> shot("d29_tree"));
        cue(5, () -> cmd("kaiju attack uproot_tree"));
        cue(28, () -> shot("d30_reach_tree"));
        cue(22, () -> shot("d31_uproot"));
        cue(25, () -> shot("d32_hold_tree"));
        cue(10, () -> cmd("kaiju attack throw_held"));
        cue(20, () -> shot("d33_throw_tree"));
        cue(10, () -> shot("d34_tree_fly"));
        cue(30, () -> { cmd("tp " + K + " -4 -60 122 0 0"); cmd("tp @s 40 -30 100 facing -4 -38 122"); });
        cue(20, () -> cmd("kaiju sever arm_l"));
        cue(12, () -> shot("d01_severed"));
        cue(40, () -> cmd("tp @s 25 -40 105 facing 8 -36 116"));
        cue(30, () -> shot("d02_stump"));
        for (int i = 0; i < 7; i++) { final int k = i; cue(100, () -> shot(String.format("d1%d_regen", k))); }
        cue(40, () -> Minecraft.getInstance().stop());
    }

    static void rlog(String what) {
        Minecraft mc = Minecraft.getInstance();
        var v = mc.player == null ? null : mc.player.getVehicle();
        int count = mc.level == null ? -1 : mc.level.getEntitiesOfClass(KaijuEntity.class, mc.player.getBoundingBox().inflate(1000)).size();
        KaijuMod.LOG.info("[RC] {} riding={} kaiju_count={}", what, v instanceof KaijuEntity, count);
    }

    static void rightClick() {
        net.minecraft.client.KeyMapping.click(Minecraft.getInstance().options.keyUse.getKey());
    }

    static void rc() {
        String K = "@e[type=kaiju:atomic_kaiju,limit=1,sort=nearest]";
        cue(30, () -> { cmd("kaiju ai false"); cmd("kill @e[type=kaiju:atomic_kaiju,limit=1,sort=furthest]"); });
        cue(20, () -> { cmd("tp " + K + " 0 -60 120 180 0"); cmd("gamemode creative"); cmd("clear @s"); cmd("give @s kaiju:kaiju_controller"); cmd("give @s kaiju:atomic_kaiju_egg"); });
        cue(20, () -> { Minecraft.getInstance().player.getInventory().selected = 0; cmd("tp @s 0 -58 95 facing 0 -40 120"); });
        cue(20, () -> rlog("before"));
        cue(2, AutoTest::rightClick);
        cue(20, () -> rlog("after right-click ON the kaiju (expect riding)"));
        cue(20, () -> shot("r01_riding"));
        cue(5, AutoTest::rightClick);
        cue(30, () -> rlog("after right-click while riding (expect released)"));
        cue(10, () -> cmd("tp @s 0 -58 60 180 -60"));
        cue(10, AutoTest::rightClick);
        cue(30, () -> rlog("after right-click in the air (expect riding)"));
        cue(5, () -> net.minecraft.client.KeyMapping.click(KaijuClient.RELEASE.getKey()));
        cue(30, () -> rlog("after K (expect released)"));
        cue(10, () -> { cmd("tp @s 0 -50 60 0 30"); Minecraft.getInstance().player.getInventory().selected = 1; });
        cue(10, AutoTest::rightClick);
        cue(40, () -> rlog("after egg right-click (expect 2 kaiju)"));
        // frame sequences: a spin and a claw combo, looking for heads or hands whirling
        cue(20, () -> { cmd("kill @e[type=kaiju:atomic_kaiju,limit=1,sort=nearest]"); cmd("gamemode spectator"); });
        cue(30, () -> { cmd("tp " + K + " 0 -60 120 180 0"); cmd("tp @s 70 -35 120 facing 0 -38 120"); });
        cue(20, () -> cmd("kaiju attack tail_spin_360"));
        for (int i = 0; i < 20; i++) { final int k = i; cue(3, () -> shot(String.format("r1%02d_spin", k))); }
        cue(20, () -> cmd("kaiju attack claw_combo3"));
        for (int i = 0; i < 16; i++) { final int k = i; cue(3, () -> shot(String.format("r2%02d_claw", k))); }
        cue(30, () -> cmd("kaiju attack roar"));
        for (int i = 0; i < 12; i++) { final int k = i; cue(5, () -> shot(String.format("r3%02d_roar", k))); }
        cue(40, () -> Minecraft.getInstance().stop());
    }

    static void build() {
        at = 100;
        cue(0, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.options.renderDistance().set(16);
            mc.options.hideGui = false;
            cmd("gamerule doDaylightCycle false");
            cmd("gamerule doMobSpawning false");
            cmd("time set 6000");
            cmd("weather clear");
            cmd("gamemode spectator");
            cmd("tp @s 0 -40 0 0 0");
        });
        cue(40, () -> {
            // a little town between where the two will stand, and a wood on each side
            int[][] houses = {{-30, 140}, {-8, 150}, {14, 138}, {-20, 172}, {6, 170}, {28, 160}, {-44, 160}};
            for (int[] h : houses) {
                int x = h[0], z = h[1];
                cmd(String.format("fill %d -60 %d %d -53 %d minecraft:oak_planks hollow", x, z, x + 9, z + 7));
                cmd(String.format("fill %d -52 %d %d -51 %d minecraft:bricks", x - 1, z - 1, x + 10, z + 8));
                cmd(String.format("fill %d -58 %d %d -57 %d minecraft:glass_pane", x + 3, z, x + 5, z));
                cmd(String.format("fill %d -59 %d %d -54 %d minecraft:stone_bricks", x, z, x, z));
            }
            for (int i = 0; i < 14; i++) {
                int x = (i % 2 == 0 ? -1 : 1) * (60 + (i * 37) % 70);
                int z = 110 + (i * 53) % 90;
                cmd(String.format("place feature minecraft:oak %d -60 %d", x, z));
                cmd(String.format("place feature minecraft:birch %d -60 %d", x + 6, z + 9));
            }
        });
        cue(60, () -> cmd("kaiju fight 150"));
        cue(30, () -> { cmd("tp @s 0 -36 -70 0 2"); });
        if (mode.contains("demo")) { demo(); return; }
        if (mode.contains("rc")) { rc(); return; }
        cue(60, () -> shot("k01_faceoff"));
        cue(5, () -> cmd("kaiju attack roar"));
        cue(40, () -> shot("k02_roar"));
        // let them fight on their own, photographing from the side
        for (int i = 0; i < 24; i++) {
            final int k = i;
            cue(50, () -> { shot(String.format("k1%02d_fight", k)); status(); });
        }
        // a beam clash: both breathe at once
        cue(20, () -> { cmd("kaiju ai false"); cmd("execute as @e[type=kaiju:atomic_kaiju] run tp @s ~ ~ ~ facing entity @e[type=kaiju:atomic_kaiju,sort=furthest,limit=1]"); });
        cue(20, () -> {
            var s = Minecraft.getInstance().getSingleplayerServer();
            s.execute(() -> {
                for (KaijuEntity k : s.overworld().getEntitiesOfClass(KaijuEntity.class, new net.minecraft.world.phys.AABB(-2000, -100, -2000, 2000, 400, 2000))) {
                    if (k.state != KaijuEntity.State.IDLE) { k.state = KaijuEntity.State.IDLE; k.attack = null; }
                    k.brain.target = null;
                    k.brain.enabled = true;
                    k.cooldown.clear();
                    k.startAttack("atomic_breath");
                }
            });
        });
        for (int i = 0; i < 8; i++) {
            final int k = i;
            cue(16, () -> { shot(String.format("k2%02d_clash", k)); status(); });
        }
        cue(80, () -> cmd("kaiju ai true"));
        // ride one
        cue(20, () -> { cmd("gamemode creative"); cmd("kaiju control"); });
        cue(40, () -> { shot("k30_ride"); ride(); });
        cue(5, () -> Minecraft.getInstance().options.keyUp.setDown(true));
        cue(60, () -> { shot("k31_ride_walk"); ride(); });
        cue(5, () -> { Minecraft.getInstance().options.keyUp.setDown(false); KaijuClient.TAIL.setDown(true); });
        cue(4, () -> KaijuClient.TAIL.setDown(false));
        cue(20, () -> shot("k32_ride_tail"));
        cue(30, () -> KaijuClient.BREATH.setDown(true));
        cue(60, () -> { shot("k33_ride_charge"); ride(); });
        cue(30, () -> { shot("k34_ride_fire"); ride(); });
        cue(40, () -> { KaijuClient.BREATH.setDown(false); shot("k35_ride_fire2"); });
        cue(60, () -> { cmd("kaiju release"); cmd("gamemode spectator"); cmd("tp @s 0 -36 -70 0 2"); });
        for (int i = 0; i < 10; i++) {
            final int k = i;
            cue(50, () -> { shot(String.format("k4%02d_fight", k)); status(); });
        }
        int[][] cams = {{80, 20, 14}, {70, -35, 10}, {90, 60, 30}, {60, 150, 18}, {75, -100, 8}, {85, 10, 40}};
        for (int i = 0; i < 18; i++) {
            final int k = i;
            final int[] c = cams[i % cams.length];
            cue(30, () -> { camDist = c[0]; camAng = c[1]; camUp = c[2]; });
            cue(40, () -> { shot(String.format("k5%02d_close", k)); status(); });
        }
        cue(40, () -> Minecraft.getInstance().stop());
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!created) {
            if (mc.screen instanceof TitleScreen && ++menu > 40) {
                created = true;
                LevelSettings ls = new LevelSettings("kaiju_test", GameType.CREATIVE, false, Difficulty.NORMAL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("kaiju_test", ls, new WorldOptions(1954L, false, false),
                        r -> r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());
            }
            return;
        }
        if (mc.player == null || mc.level == null) return;
        n++;
        for (Object[] c : cues) if ((int) c[0] == n) ((Runnable) c[1]).run();
        // keep the spectator camera aimed at the middle of the fight
        if (mc.player.isSpectator() && n % 10 == 0 && n > 200 && !mode.contains("demo") && !mode.contains("rc")) {
            var list = mc.level.getEntitiesOfClass(KaijuEntity.class, mc.player.getBoundingBox().inflate(800));
            if (list.size() >= 1) {
                double x = 0, y = 0, z = 0;
                for (KaijuEntity k : list) { x += k.getX(); y += k.getY(); z += k.getZ(); }
                x /= list.size(); y /= list.size(); z /= list.size();
                double a = Math.toRadians(camAng);
                cmd(String.format(java.util.Locale.ROOT, "tp @s %.1f %.1f %.1f facing %.1f %.1f %.1f", x + Math.sin(a) * camDist, y + camUp, z - Math.cos(a) * camDist, x, y + 20, z));
            }
        }
    }
}
