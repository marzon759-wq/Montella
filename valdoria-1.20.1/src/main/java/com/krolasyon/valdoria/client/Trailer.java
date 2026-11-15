package com.krolasyon.valdoria.client;

import com.krolasyon.valdoria.ValdoriaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Development only: a scripted ~1 minute flythrough of the Glimmerveil Forest for the promo video. The camera is the
 * player in spectator mode, moved smoothly every client tick; creatures are summoned on cue. Each scene logs a
 * "[AUTOTEST] MARK" line with a timestamp so the screen recording can be cut and captioned afterwards.
 */
final class Trailer {
    private Trailer() {}

    /** A camera keyframe relative to the forest spot: position, yaw, pitch. */
    record Cam(double x, double y, double z, float yaw, float pitch) {
        Cam lerp(Cam o, double t) {
            double s = t * t * (3 - 2 * t);
            return new Cam(Mth.lerp(s, x, o.x), Mth.lerp(s, y, o.y), Mth.lerp(s, z, o.z),
                    (float) Mth.lerp(s, yaw, o.yaw), (float) Mth.lerp(s, pitch, o.pitch));
        }
    }

    record Cue(int at, String cmd) {}

    static final class Scene {
        final String name;
        final int ticks;
        final long time;
        final List<Cam> path = new ArrayList<>();
        final List<Cue> cues = new ArrayList<>();

        Scene(String name, int ticks, long time) {
            this.name = name;
            this.ticks = ticks;
            this.time = time;
        }

        Scene cam(double x, double y, double z, float yaw, float pitch) {
            path.add(new Cam(x, y, z, yaw, pitch));
            return this;
        }

        Scene at(int tick, String... cmds) {
            for (String c : cmds) cues.add(new Cue(tick, c));
            return this;
        }

        Cam camAt(double t) {
            if (path.size() == 1) return path.get(0);
            double f = t * (path.size() - 1);
            int i = Math.min(path.size() - 2, (int) f);
            return path.get(i).lerp(path.get(i + 1), f - i);
        }
    }

    static final List<Scene> SCENES = new ArrayList<>();

    static {
        String clear = "kill @e[type=!player,type=!item]";
        String clearItems = "kill @e[type=item]";
        // 1. descend from above the purple canopy into the forest
        SCENES.add(new Scene("intro", 220, 6000)
                .at(0, "gamerule doMobSpawning false", clear, "gamemode spectator @a")
                .cam(-34, 62, 46, 145, 32).cam(-16, 34, 24, 160, 22).cam(-4, 10, 8, 175, 4).cam(0, 2.2, 4, 180, -4));
        // 2. look up into the vine curtains and the glow
        SCENES.add(new Scene("vines", 160, 6000)
                .cam(0, 1.6, 3, 180, -48).cam(0, 1.6, 2.5, 250, -30));
        // 3. a veilstag herd grazing close by, moths drifting past
        SCENES.add(new Scene("herd", 200, 6000)
                .at(0, clear,
                        "summon valdoria:veilstag {x} {y} {z-6}", "summon valdoria:veilstag {x+2} {y} {z-8}", "summon valdoria:veilstag {x-2} {y} {z-7}",
                        "summon valdoria:veilstag {x+1} {y} {z-9}", "summon valdoria:veilstag {x-1} {y} {z-5} {Age:-24000}",
                        "summon valdoria:lumen_moth {x} {y+2} {z-3}", "summon valdoria:lumen_moth {x+1} {y+2} {z-4}", "summon valdoria:lumen_moth {x-1} {y+3} {z-4}",
                        "summon valdoria:lumen_moth {x} {y+1} {z-2}", "summon valdoria:lumen_moth {x+2} {y+3} {z-5}", "summon valdoria:lumen_moth {x-2} {y+2} {z-3}")
                .cam(0, 0.4, 3, 180, 2).cam(0, 0.3, 1, 180, 4));
        // 4. a hungry gloomlynx pack slips in from the west; the herd bolts
        SCENES.add(new Scene("hunt", 160, 6000)
                .at(0, "summon valdoria:gloomlynx {x-10} {y} {z-6} {Satiety:0}", "summon valdoria:gloomlynx {x-11} {y} {z-8} {Satiety:0}",
                        "summon valdoria:gloomlynx {x-12} {y} {z-5} {Satiety:0}")
                .cam(3, 0.8, 2, 160, 6).cam(3, 0.8, 1, 175, 6));
        // 5. night: glowing eyes, glowing moths; meanwhile a vinestalker climbs into the canopy
        SCENES.add(new Scene("night", 180, 18000)
                .at(0, clear, clearItems,
                        "summon valdoria:gloomlynx {x+1} {y} {z-4}", "summon valdoria:gloomlynx {x-2} {y} {z-5}",
                        "summon valdoria:lumen_moth {x} {y+2} {z-2}", "summon valdoria:lumen_moth {x+1} {y+2} {z-3}", "summon valdoria:lumen_moth {x-1} {y+2} {z-3}",
                        "summon valdoria:lumen_moth {x+2} {y+2} {z-4}", "summon valdoria:lumen_moth {x-2} {y+3} {z-2}", "summon valdoria:lumen_moth {x} {y+3} {z-4}",
                        "summon valdoria:vinestalker {x-3} {y} {z-5}")
                .cam(0, 0.4, 1, 165, 4).cam(0, 0.4, 0.5, 195, 2));
        // 6. the vinestalker hangs above; a stag wanders underneath; the drop
        SCENES.add(new Scene("ambush", 220, 6000)
                .at(0, "kill @e[type=valdoria:gloomlynx]", "kill @e[type=valdoria:lumen_moth]")
                .at(80, "summon valdoria:veilstag {x-3} {y} {z-5}")
                .cam(-3, 0.6, 2, 180, -64).cam(-3, 0.6, 2, 180, -64).cam(-3, 0.6, 2.5, 180, -6).cam(-3, 0.6, 3, 180, 4));
        // 7. rise back out through the canopy
        SCENES.add(new Scene("outro", 170, 6000)
                .at(0, clear, clearItems)
                .cam(0, 2, 3, 180, -10).cam(6, 22, 16, 200, 25).cam(14, 64, 34, 205, 38));
    }

    private static BlockPos origin;
    private static int scene = -1;
    private static int tick;

    /** Game slow-motion factor: the game runs this many times slower while recording, the video is sped up again. */
    static final int SLOWMO = Integer.getInteger("valdoria.slowmo", 6);

    static void start(BlockPos target) {
        origin = target;
        slowDown();
        scene = 0;
        tick = 0;
        ValdoriaMod.LOG.info("[AUTOTEST] MARK trailer_start {}", System.currentTimeMillis());
    }

    static boolean running() {
        return scene >= 0;
    }

    /** Called every client tick at the END phase. Returns false when the trailer is over. */
    static boolean tick(Consumer<String> cmd) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || scene < 0) return false;
        if (scene >= SCENES.size()) {
            ValdoriaMod.LOG.info("[AUTOTEST] MARK trailer_end {}", System.currentTimeMillis());
            scene = -1;
            return false;
        }
        Scene s = SCENES.get(scene);
        if (tick == 0) {
            ValdoriaMod.LOG.info("[AUTOTEST] MARK {} {}", s.name, System.currentTimeMillis());
            cmd.accept("time set " + s.time);
        }
        for (Cue c : s.cues) if (c.at == tick) cmd.accept(c.cmd);

        Cam c = s.camAt(Math.min(1.0, tick / (double) (s.ticks - 1)));
        Vec3 p = new Vec3(origin.getX() + 0.5 + c.x, origin.getY() + c.y, origin.getZ() + 0.5 + c.z);
        if (tick == 0) {
            mc.player.absMoveTo(p.x, p.y, p.z, c.yaw, c.pitch);
        } else {
            mc.player.setPos(p.x, p.y, p.z);
            mc.player.setYRot(c.yaw);
            mc.player.setXRot(c.pitch);
        }
        mc.player.setYHeadRot(c.yaw);
        mc.player.setDeltaMovement(Vec3.ZERO);

        if (++tick >= s.ticks) {
            scene++;
            tick = 0;
        }
        return true;
    }

    /**
     * Software rendering only manages a few frames per second, so the whole game (client and integrated server) is
     * slowed down {@link #SLOWMO} times; every game tick then gets that many more rendered frames, and the recording
     * is sped back up to real time afterwards.
     */
    private static void slowDown() {
        if (SLOWMO <= 1) return;
        try {
            java.lang.reflect.Field tf = Minecraft.class.getDeclaredField("timer");
            tf.setAccessible(true);
            Object timer = tf.get(Minecraft.getInstance());
            java.lang.reflect.Field ms = timer.getClass().getDeclaredField("msPerTick");
            ms.setAccessible(true);
            ms.setFloat(timer, 50.0F * SLOWMO);
        } catch (ReflectiveOperationException e) {
            ValdoriaMod.LOG.error("[AUTOTEST] could not slow the client timer", e);
        }
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ServerTickEvent e) -> {
            if (e.phase != net.minecraftforge.event.TickEvent.Phase.END || scene < 0) return;
            try {
                Thread.sleep(50L * (SLOWMO - 1));
            } catch (InterruptedException ignored) {
            }
        });
        ValdoriaMod.LOG.info("[AUTOTEST] slow motion x{}", SLOWMO);
    }

    @SuppressWarnings("unused")
    private static MinecraftServer server() {
        return Minecraft.getInstance().getSingleplayerServer();
    }
}
