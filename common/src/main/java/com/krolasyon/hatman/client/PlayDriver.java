package com.krolasyon.hatman.client;

import com.krolasyon.hatman.HatmanMod;
import com.krolasyon.hatman.net.Msg;
import com.krolasyon.hatman.net.Net;
import com.krolasyon.hatman.story.House;
import com.krolasyon.hatman.story.Story;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Development only: plays the story like a person would, for the gameplay recording. A queue of actions that each run
 * until they are finished: walking along waypoints with smooth turning, looking at things, reading notes, answering.
 */
final class PlayDriver {
    interface Act {
        /** @return true when finished */
        boolean tick(int age);
    }

    private final ArrayDeque<Act> queue = new ArrayDeque<>();
    private Act cur = null;
    private int age = 0;
    private float wantPitch = 3;
    private final Consumer<String> srv;

    PlayDriver(Consumer<String> srv) { this.srv = srv; }

    boolean done() { return cur == null && queue.isEmpty(); }

    static Minecraft mc() { return Minecraft.getInstance(); }

    static Vec3 w(double x, double y, double z) { return new Vec3(Story.S.ox + x, Story.S.oy + y, Story.S.oz + z); }

    private static void keys(boolean fwd, boolean sprint, boolean sneak) {
        mc().options.keyUp.setDown(fwd);
        mc().options.keySprint.setDown(sprint);
        mc().options.keyShift.setDown(sneak);
        LocalPlayer p = mc().player;
        if (p != null) p.setSprinting(sprint && fwd);
    }

    /** turn toward a yaw/pitch by at most `rate` degrees per tick, eased */
    private static boolean turn(float yaw, float pitch, float rate) {
        LocalPlayer p = mc().player;
        float dy = Mth.wrapDegrees(yaw - p.getYRot());
        float dp = pitch - p.getXRot();
        float sy = Mth.clamp(dy * 0.25f, -rate, rate);
        float sp = Mth.clamp(dp * 0.2f, -rate * 0.6f, rate * 0.6f);
        p.setYRot(p.getYRot() + sy);
        p.setXRot(p.getXRot() + sp);
        p.yHeadRot = p.getYRot();
        return Math.abs(dy) < 2 && Math.abs(dp) < 2;
    }

    private static float yawTo(Vec3 from, Vec3 to) {
        return (float) (Mth.atan2(to.z - from.z, to.x - from.x) * Mth.RAD_TO_DEG) - 90f;
    }

    private static float pitchTo(Vec3 from, Vec3 to) {
        double dx = to.x - from.x, dz = to.z - from.z;
        return (float) -(Mth.atan2(to.y - from.y, Math.sqrt(dx * dx + dz * dz)) * Mth.RAD_TO_DEG);
    }

    void tick() {
        if (mc().player == null) return;
        if (cur == null) {
            cur = queue.poll();
            age = 0;
            if (cur == null) return;
        }
        boolean fin;
        try {
            fin = cur.tick(age++);
        } catch (Throwable t) {
            HatmanMod.LOG.error("[PLAY] action failed", t);
            fin = true;
        }
        if (fin) {
            keys(false, false, false);
            cur = null;
        }
    }

    // ------------------------------------------------------------------ building blocks
    PlayDriver add(Act a) { queue.add(a); return this; }

    PlayDriver run(Runnable r) { return add(a -> { r.run(); return true; }); }

    PlayDriver srv(String cmd) { return run(() -> srv.accept(cmd)); }

    PlayDriver mark(String name) { return run(() -> HatmanMod.LOG.info("[AUTOTEST] MARK {} {}", name, System.currentTimeMillis())); }

    PlayDriver wait(int ticks) { return add(a -> a >= ticks); }

    PlayDriver until(BooleanSupplier c, int timeout, String what) {
        return add(a -> {
            if (c.getAsBoolean()) return true;
            if (a > timeout) {
                HatmanMod.LOG.error("[PLAY] timeout waiting for {} (step {})", what, Story.S.step);
                return true;
            }
            return false;
        });
    }

    PlayDriver step(String s) { return until(() -> Story.S.step.equals(s), 2400, "step " + s); }

    PlayDriver cutStart() { return until(() -> ClientState.cutId != null, 600, "cutscene"); }

    PlayDriver cutEnd() { return until(() -> ClientState.cutId == null && !(mc().screen instanceof CutsceneScreen), 1800, "cutscene end"); }

    PlayDriver tp(double x, double y, double z, float yaw) { return srv("tp:" + x + "," + y + "," + z + "," + yaw + ",3").wait(4); }

    PlayDriver pitch(float p) { return run(() -> wantPitch = p); }

    PlayDriver cam(boolean on) { return run(() -> { if (ClientState.nvOn != on) Net.toServer(Msg.of("cam")); }); }

    /** walk (or run) through local waypoints; {x,y,z} or {x,y,z,1} for a ladder target reached by height */
    PlayDriver walk(boolean sprint, double[]... pts) {
        List<double[]> list = new ArrayList<>(List.of(pts));
        final int[] idx = {0};
        final double[] best = {1e9};
        final int[] bestAge = {0};
        return add(a -> {
            LocalPlayer p = mc().player;
            if (idx[0] >= list.size()) return true;
            double[] pt = list.get(idx[0]);
            Vec3 t = w(pt[0], pt[1], pt[2]);
            boolean ladder = pt.length > 3;
            double dx = t.x - p.getX(), dz = t.z - p.getZ();
            double hd = Math.sqrt(dx * dx + dz * dz);
            boolean arrived = ladder ? Math.abs(p.getY() - t.y) < 0.4 : hd < 0.5 && Math.abs(p.getY() - t.y) < 1.6;
            if (arrived) {
                idx[0]++;
                best[0] = 1e9;
                bestAge[0] = a;
                return idx[0] >= list.size();
            }
            float yaw = hd > 0.2 ? yawTo(p.position(), t) : p.getYRot();
            float pitch = wantPitch + (float) Mth.clamp((p.getY() - t.y) * 4, -10, 18);
            turn(yaw, pitch, sprint ? 14 : 9);
            float err = Math.abs(Mth.wrapDegrees(yaw - p.getYRot()));
            keys(err < 55 || ladder, sprint && err < 30, false);
            double dist = hd + Math.abs(p.getY() - t.y);
            if (dist < best[0] - 0.05) {
                best[0] = dist;
                bestAge[0] = a;
            } else if (a - bestAge[0] > 50) {
                HatmanMod.LOG.warn("[PLAY] stuck near {} going to {},{},{} - teleporting", p.position().subtract(Story.S.ox, Story.S.oy, Story.S.oz), pt[0], pt[1], pt[2]);
                srv.accept("tp:" + pt[0] + "," + pt[1] + "," + pt[2] + "," + p.getYRot() + "," + p.getXRot());
                bestAge[0] = a;
            }
            return false;
        });
    }

    static double[] P(double x, double y, double z) { return new double[]{x, y, z}; }

    static double[] L(double x, double y, double z) { return new double[]{x, y, z, 1}; }

    /** slowly turn to look at a point (local coordinates) and hold */
    PlayDriver look(double x, double y, double z, int hold) {
        return add(a -> {
            LocalPlayer p = mc().player;
            Vec3 t = w(x, y, z);
            turn(yawTo(p.getEyePosition(), t), pitchTo(p.getEyePosition(), t), 7);
            return a >= hold;
        });
    }

    PlayDriver lookAtTag(String tag, int hold) {
        final BlockPos[] at = {null};
        return add(a -> {
            if (at[0] == null) {
                at[0] = House.first(tag);
                if (at[0] == null) {
                    HatmanMod.LOG.error("[PLAY] no hotspot {}", tag);
                    return true;
                }
            }
            LocalPlayer p = mc().player;
            Vec3 t = w(at[0].getX() + 0.5, at[0].getY() + 0.5, at[0].getZ() + 0.5);
            turn(yawTo(p.getEyePosition(), t), pitchTo(p.getEyePosition(), t), 7);
            return a >= hold;
        });
    }

    PlayDriver use(String tag) { return srv("use:" + tag); }

    /** look at it, use it, hold each page of the note on screen, turn pages, close */
    PlayDriver read(String tag, int perPage) {
        lookAtTag(tag, 22);
        use(tag);
        until(() -> mc().screen instanceof NoteScreen, 80, "note " + tag);
        return add(a -> {
            if (!(mc().screen instanceof NoteScreen ns)) return true;
            if (a > 0 && a % perPage == 0) ns.mouseClicked(0, 0, 0);
            return false;
        });
    }

    /** answer a conversation: answers are positions in the shown list, used in order for nodes with choices */
    PlayDriver talk(int... answers) {
        final int[] next = {0};
        final String[] node = {""};
        final int[] readyAge = {-1};
        final int[] gone = {0};
        final boolean[] started = {false};
        return add(a -> {
            if (mc().screen instanceof DialogueScreen ds) {
                started[0] = true;
                gone[0] = 0;
                if (!ds.nodeId().equals(node[0])) {
                    node[0] = ds.nodeId();
                    readyAge[0] = -1;
                }
                if (ds.ready()) {
                    if (readyAge[0] < 0) readyAge[0] = a;
                    int need = ds.choiceCount() > 0 ? 70 : 34 + ds.textLength() / 3;
                    if (a - readyAge[0] >= need) {
                        if (ds.choiceCount() > 0) {
                            int c = next[0] < answers.length ? answers[next[0]] : 0;
                            next[0]++;
                            ds.keyPressed(49 + Math.min(c, ds.choiceCount() - 1), 0, 0);
                        } else ds.keyPressed(32, 0, 0);
                        readyAge[0] = Integer.MAX_VALUE / 2;
                    }
                }
                return false;
            }
            if (started[0] && ++gone[0] > 30 && DialogueScreen.pending == null) return true;
            return a > 2400;
        });
    }

    /** type a code on the safe's dial */
    PlayDriver code(String digits) {
        until(() -> mc().screen instanceof KeypadScreen, 80, "keypad");
        final int[] i = {0};
        return add(a -> {
            if (!(mc().screen instanceof KeypadScreen k)) return true;
            if (a % 14 == 13) {
                if (i[0] < digits.length()) k.keyPressed(48 + (digits.charAt(i[0]) - '0'), 0, 0);
                else if (i[0] == digits.length()) k.keyPressed(257, 0, 0);
                i[0]++;
            }
            return false;
        });
    }
}
