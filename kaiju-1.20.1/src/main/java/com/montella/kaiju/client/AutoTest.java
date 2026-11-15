package com.montella.kaiju.client;

import com.montella.kaiju.KaijuMod;
import com.montella.kaiju.entity.GhidorahEntity;
import com.montella.kaiju.entity.GodzillaEntity;
import com.montella.kaiju.entity.TitanEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
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
import java.util.function.Consumer;

/** Development only (-Dkaiju.autotest=scenario): builds a flat world, poses the titans and takes screenshots. */
public final class AutoTest {
    private static String mode;
    private static int menu, n;
    private static boolean created;
    private static final List<Object[]> cues = new ArrayList<>();
    private static int at;

    private AutoTest() {}

    static void init() {
        mode = System.getProperty("kaiju.autotest");
        MinecraftForge.EVENT_BUS.addListener(AutoTest::tick);
        build();
    }

    private static void cue(int delay, Runnable r) { at += delay; cues.add(new Object[]{at, r}); }

    private static void cmd(String c) {
        var s = Minecraft.getInstance().getSingleplayerServer();
        if (s == null) return;
        s.execute(() -> s.getCommands().performPrefixedCommand(s.createCommandSourceStack().withSuppressedOutput(), c));
    }

    private static void server(Consumer<ServerLevel> f) {
        var s = Minecraft.getInstance().getSingleplayerServer();
        if (s == null) return;
        s.execute(() -> f.accept(s.overworld()));
    }

    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        KaijuMod.LOG.info("[AUTOTEST] shot {}", name);
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {});
    }

    private static void view(String name, double x, double y, double z, float yaw, float pitch) {
        cue(5, () -> cmd("tp @p " + x + " " + y + " " + z + " " + yaw + " " + pitch));
        cue(25, () -> shot(name));
    }

    private static void forceState(Class<? extends TitanEntity> c, int state, int time) {
        server(l -> {
            for (var e : l.getAllEntities()) if (c.isInstance(e)) { ((TitanEntity) e).setState(state); ((TitanEntity) e).stateTime = time; }
        });
    }

    private static void build() {
        at = 40;
        cue(0, () -> { cmd("time set 6000"); cmd("gamerule doDaylightCycle false"); cmd("weather clear"); cmd("gamerule doMobSpawning false"); cmd("gamemode creative @p"); cmd("effect give @p night_vision 9999 0 true"); });
        if (mode.contains("model")) {
            cue(20, () -> { cmd("summon kaiju:godzilla 0 -60 0 {Rotation:[0f,0f]}"); cmd("summon kaiju:king_ghidorah 60 -60 0 {Rotation:[0f,0f]}"); });
            cue(40, () -> {});
            view("model_godzilla_front", 0, -47, 46, 180, 2);
            view("model_godzilla_side", 46, -47, 0, 90, 2);
            view("model_godzilla_back", -32, -44, -36, -42, 4);
            view("model_godzilla_3q", -32, -46, 32, -135, 2);
            view("model_ghidorah_front", 60, -46, 46, 180, 0);
            view("model_ghidorah_side", 106, -46, 0, 90, 0);
            view("model_ghidorah_3q", 28, -44, 32, -135, 2);
            view("model_ghidorah_head", 66, -35, 15, 160, -3);
            view("model_godzilla_head", 6, -38, 14, 160, 0);
            cue(5, () -> forceState(GodzillaEntity.class, com.montella.kaiju.entity.S.ROAR, 30));
            cue(0, () -> forceState(GhidorahEntity.class, com.montella.kaiju.entity.S.ROAR, 40));
            view("pose_roar_g", -32, -46, 32, -135, 2);
            view("pose_roar_k", 28, -44, 32, -135, 2);
            cue(5, () -> forceState(GodzillaEntity.class, com.montella.kaiju.entity.S.CHARGE_BEAM, 28));
            view("pose_charge_g", -32, -46, 32, -135, 2);
            cue(5, () -> forceState(GhidorahEntity.class, com.montella.kaiju.entity.S.FLY, 0));
            view("pose_fly_k", 60, -30, 60, 180, 10);
            cue(5, () -> forceState(GodzillaEntity.class, com.montella.kaiju.entity.S.FALLEN, 100));
            view("pose_fallen_g", -34, -46, 24, -125, 10);
        }
        if (mode.contains("fight")) {
            cue(20, () -> { cmd("tp @p 0 -55 0 0 10"); cmd("kaiju fight"); });
            for (int i = 0; i < 40; i++) {
                final int k = i;
                cue(60, () -> {
                    var mc = Minecraft.getInstance();
                    var lvl = mc.level;
                    if (lvl == null) return;
                    GodzillaEntity g = null; GhidorahEntity k2 = null;
                    for (var e : lvl.entitiesForRendering()) { if (e instanceof GodzillaEntity a) g = a; if (e instanceof GhidorahEntity b) k2 = b; }
                    if (g != null && k2 != null) {
                        var mid = g.position().add(k2.position()).scale(0.5);
                        var dir = k2.position().subtract(g.position()).normalize();
                        var side = new net.minecraft.world.phys.Vec3(-dir.z, 0, dir.x);
                        var cam = mid.add(side.scale(62)).add(0, 16, 0);
                        float yaw = (float) Math.toDegrees(Math.atan2(mid.z - cam.z, mid.x - cam.x)) - 90f;
                        cmd("tp @p " + cam.x + " " + cam.y + " " + cam.z + " " + yaw + " 8");
                        KaijuMod.LOG.info("[AUTOTEST] states g={} k={}", g.state(), k2.state());
                    }
                });
                cue(15, () -> shot("fight_" + String.format("%02d", k)));
            }
        }
        cue(40, () -> Minecraft.getInstance().stop());
    }

    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (!created) {
            if (mc.screen != null && !(mc.screen instanceof TitleScreen) && mc.screen.getClass().getSimpleName().contains("Onboarding")) mc.setScreen(new TitleScreen());
            if (mc.screen instanceof TitleScreen && ++menu > 40) {
                created = true;
                String name = "kaiju_test_" + System.currentTimeMillis() % 100000;
                GameRules rules = new GameRules();
                LevelSettings ls = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.NORMAL, true, rules, WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(name, ls, new WorldOptions(1962L, false, false),
                        r -> r.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());
            }
            return;
        }
        if (mc.player == null || mc.level == null) return;
        mc.options.hideGui = true;
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        n++;
        for (Object[] c : cues) if ((int) c[0] == n) ((Runnable) c[1]).run();
    }
}
