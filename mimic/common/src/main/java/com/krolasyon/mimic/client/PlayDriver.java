package com.krolasyon.mimic.client;

import com.krolasyon.mimic.MimicMod;
import com.krolasyon.mimic.net.Msg;
import com.krolasyon.mimic.net.Net;
import com.krolasyon.mimic.story.House;
import com.krolasyon.mimic.story.Story;
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

    static boolean sneaking = false;

    PlayDriver sneak(boolean on) { return run(() -> sneaking = on); }

    private static void keys(boolean fwd, boolean sprint, boolean sneak) {
        sneak |= sneaking;
        sprint &= !sneaking;
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
            MimicMod.LOG.error("[PLAY] action failed", t);
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

    PlayDriver mark(String name) { return run(() -> MimicMod.LOG.info("[AUTOTEST] MARK {} {}", name, System.currentTimeMillis())); }

    /** put a story item in the hand, like scrolling the hotbar to it */
    PlayDriver hold(String id) {
        return run(() -> {
            var inv = mc().player.getInventory();
            for (int i = 0; i < 9; i++)
                if (inv.getItem(i).is(com.krolasyon.mimic.registry.ModItems.story(id))) { inv.selected = i; return; }
        });
    }

    PlayDriver wait(int ticks) { return add(a -> a >= ticks); }

    PlayDriver until(BooleanSupplier c, int timeout, String what) {
        return add(a -> {
            if (c.getAsBoolean()) return true;
            if (a > timeout) {
                MimicMod.LOG.error("[PLAY] timeout waiting for {} (step {})", what, Story.S.step);
                timeouts++;
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

    PlayDriver light(boolean on) { return run(() -> { if (ClientState.lightOn != on) Net.toServer(Msg.of("light")); }); }

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
                MimicMod.LOG.warn("[PLAY] stuck near {} going to {},{},{} - teleporting", p.position().subtract(Story.S.ox, Story.S.oy, Story.S.oz), pt[0], pt[1], pt[2]);
                stuck++;
                srv.accept("tp:" + pt[0] + "," + pt[1] + "," + pt[2] + "," + p.getYRot() + "," + p.getXRot());
                bestAge[0] = a;
            }
            return false;
        });
    }

    // ------------------------------------------------------------------ finding a way like a person would: around things, through open doors
    private static double topAt(BlockPos w) {
        var lvl = mc().level;
        var st = lvl.getBlockState(w);
        if (st.getBlock() instanceof net.minecraft.world.level.block.LadderBlock) return 0;
        if (st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock && st.getValue(net.minecraft.world.level.block.DoorBlock.OPEN)) return 0;
        if (st.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock && st.getValue(net.minecraft.world.level.block.TrapDoorBlock.OPEN)) return 0;
        if (st.hasProperty(com.krolasyon.mimic.block.OpenableBlock.OPEN) && st.getValue(com.krolasyon.mimic.block.OpenableBlock.OPEN)) return 0;
        var sh = st.getCollisionShape(lvl, w);
        return sh.isEmpty() ? 0 : sh.max(net.minecraft.core.Direction.Axis.Y);
    }

    /** feet in world cell c: is that a place to stand */
    private static boolean standAt(BlockPos c) {
        double h = topAt(c);
        if (h > 0.55) return false;
        if (topAt(c.above()) > 0 || (h > 0 && topAt(c.above(2)) > 0)) return false;
        return h > 0 || topAt(c.below()) >= 0.5;
    }

    /** breadth-first path over standable cells, in local coordinates */
    static List<double[]> path(Vec3 fromW, double tx, double ty, double tz) {
        BlockPos start = BlockPos.containing(fromW.x, fromW.y + 0.2, fromW.z);
        BlockPos goal = BlockPos.containing(Story.S.ox + tx, Story.S.oy + ty + 0.2, Story.S.oz + tz);
        java.util.Map<BlockPos, BlockPos> prev = new java.util.HashMap<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(start);
        prev.put(start, start);
        BlockPos found = null;
        while (!q.isEmpty() && prev.size() < 60000) {
            BlockPos c = q.poll();
            if (c.getX() == goal.getX() && c.getZ() == goal.getZ() && Math.abs(c.getY() - goal.getY()) <= 1) { found = c; break; }
            for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos n = c.relative(d).above(dy);
                    if (prev.containsKey(n) || !standAt(n)) continue;
                    if (dy == 1 && topAt(c.above(2)) > 0) continue;
                    if (dy == -1 && topAt(n.above(2)) > 0) continue;
                    prev.put(n, c);
                    q.add(n);
                    break;
                }
            }
        }
        if (found == null) return null;
        List<double[]> out = new ArrayList<>();
        for (BlockPos c = found; !c.equals(start); c = prev.get(c))
            out.add(0, new double[]{c.getX() + 0.5 - Story.S.ox, c.getY() - Story.S.oy, c.getZ() + 0.5 - Story.S.oz});
        // keep every third cell: smoother walking, the turns are still there
        List<double[]> thin = new ArrayList<>();
        for (int i = 0; i < out.size(); i++) if (i % 3 == 2 || i == out.size() - 1) thin.add(out.get(i));
        return thin;
    }

    /** walk to a place by a path found around everything in the way */
    PlayDriver go(boolean sprint, double x, double y, double z) {
        final PlayDriver[] inner = {null};
        final List<double[]>[] pts = new List[]{null};
        final int[] i = {0};
        return add(a -> {
            if (pts[0] == null) {
                pts[0] = path(mc().player.position(), x, y, z);
                if (pts[0] == null) {
                    misses++;
                    MimicMod.LOG.warn("[REAL] no way on foot from {} to {},{},{}", mc().player.position().subtract(Story.S.ox, Story.S.oy, Story.S.oz), x, y, z);
                    pts[0] = new ArrayList<>(List.of(new double[]{x, y, z}));
                }
                inner[0] = new PlayDriver(srv);
                inner[0].walk(sprint, pts[0].toArray(new double[0][]));
            }
            inner[0].tick();
            return inner[0].done();
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
                    MimicMod.LOG.error("[PLAY] no hotspot {}", tag);
                    return true;
                }
            }
            LocalPlayer p = mc().player;
            Vec3 t = w(at[0].getX() + 0.5, at[0].getY() + 0.5, at[0].getZ() + 0.5);
            turn(yawTo(p.getEyePosition(), t), pitchTo(p.getEyePosition(), t), 7);
            return a >= hold;
        });
    }

    PlayDriver use(String tag) { return real ? click(tag) : srv("use:" + tag); }

    /** problems a real player would have had: a click that did not land on the thing, a walk that got stuck */
    static int misses = 0, stuck = 0, timeouts = 0;
    /** true: never cheat (no teleports when stuck, real clicks only) */
    static boolean real = false;

    /** a real right click, through the client like a player's: look at the thing, then click what the crosshair is on */
    PlayDriver click(String tag) {
        lookAtTag(tag, 14);
        return run(() -> {
            var mc = mc();
            BlockPos want = House.first(tag);
            if (want == null) { misses++; MimicMod.LOG.error("[REAL] no hotspot {}", tag); return; }
            net.minecraft.world.phys.BlockHitResult bh = null;
            if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult h && h.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                BlockPos l = h.getBlockPos().offset(-Story.S.ox, -Story.S.oy, -Story.S.oz);
                String got = House.HOT.get(l);
                if (tag.equals(got)) bh = h;
                else MimicMod.LOG.warn("[REAL] aiming at {} but the crosshair is on {} ({})", tag, got, l);
            }
            if (bh == null) {
                misses++;
                Vec3 c = w(want.getX() + 0.5, want.getY() + 0.5, want.getZ() + 0.5);
                bh = new net.minecraft.world.phys.BlockHitResult(c, net.minecraft.core.Direction.UP, want.offset(Story.S.ox, Story.S.oy, Story.S.oz), false);
            }
            double d = mc.player.getEyePosition().distanceTo(bh.getLocation());
            if (d > 4.5) { misses++; MimicMod.LOG.warn("[REAL] {} is {} away: out of reach", tag, d); }
            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, bh);
            MimicMod.LOG.info("[REAL] click {} step={}", tag, Story.S.step);
        });
    }

    /** right click with the item in the hand */
    PlayDriver useHeld(String id) {
        hold(id);
        return run(() -> mc().gameMode.useItem(mc().player, net.minecraft.world.InteractionHand.MAIN_HAND));
    }

    /** right click on the truck */
    PlayDriver clickTruck() {
        return run(() -> {
            var mc = mc();
            net.minecraft.world.entity.Entity truck = null;
            for (var e : mc.level.entitiesForRendering())
                if (e instanceof com.krolasyon.mimic.entity.TruckEntity && e.distanceTo(mc.player) < 6) truck = e;
            if (truck == null) { misses++; MimicMod.LOG.warn("[REAL] no truck in reach"); return; }
            mc.gameMode.interact(mc.player, truck, net.minecraft.world.InteractionHand.MAIN_HAND);
        });
    }

    /** leave a hiding place: sneak */
    PlayDriver getOut() {
        return add(a -> {
            mc().options.keyShift.setDown(a < 6);
            return a > 10;
        });
    }

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

}
