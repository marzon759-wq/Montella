package com.krolasyon.hatman.story;

import com.krolasyon.hatman.HatmanMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Development tool: walks the built house like a player would and reports everything that is wrong with it. */
public final class HouseAudit {
    private final ServerLevel lvl;
    private final BlockPos o;
    private final List<String> issues = new ArrayList<>();
    private final Set<BlockPos> reach = new HashSet<>();
    private final Set<BlockPos> secret = new HashSet<>();

    private HouseAudit(ServerLevel lvl, BlockPos origin) {
        this.lvl = lvl;
        this.o = origin;
    }

    public static List<String> run(ServerLevel lvl, BlockPos origin) {
        HouseAudit a = new HouseAudit(lvl, origin);
        a.go();
        for (String s : a.issues) HatmanMod.LOG.warn("[AUDIT] {}", s);
        HatmanMod.LOG.info("[AUDIT] done: {} issues, {} reachable cells", a.issues.size(), a.reach.size());
        return a.issues;
    }

    private BlockState st(BlockPos l) { return lvl.getBlockState(o.offset(l)); }

    private double top(BlockPos l) {
        BlockState s = st(l);
        if (isOpenable(s) || secret.contains(l) || s.getBlock() instanceof LadderBlock) return 0;
        VoxelShape sh = s.getCollisionShape(lvl, o.offset(l));
        return sh.isEmpty() ? 0 : sh.max(Direction.Axis.Y);
    }

    private static boolean isOpenable(BlockState s) {
        Block b = s.getBlock();
        return b instanceof DoorBlock || b instanceof FenceGateBlock || b instanceof TrapDoorBlock;
    }

    private boolean free(BlockPos l) { return top(l) == 0; }

    private boolean ladder(BlockPos l) {
        Block b = st(l).getBlock();
        // an open trapdoor over a ladder is climbable, like the attic hatch
        return b instanceof LadderBlock || b instanceof TrapDoorBlock && st(l.below()).getBlock() instanceof LadderBlock;
    }

    /** can a player stand with feet in cell l */
    private boolean stand(BlockPos l) {
        double h = top(l);
        if (h > 0.55) return false;
        if (!free(l.above())) return false;
        if (h > 0) return free(l.above(2)) || top(l.above()) == 0;
        if (ladder(l)) return true;
        double b = top(l.below());
        return b >= 0.5 || ladder(l.below());
    }

    private String fmt(BlockPos l) { return "(" + l.getX() + "," + l.getY() + "," + l.getZ() + ")"; }

    private void go() {
        House.ensureIndexed();
        for (BlockPos p : House.TAGGED.getOrDefault("secret_shelf", List.of())) secret.add(p);
        // ---------------------------------------------------------------- 1. walk the house from the front door
        BlockPos start = new BlockPos(22, 1, 28);
        if (!stand(start)) issues.add("start cell in the foyer is not standable " + fmt(start));
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(start);
        reach.add(start);
        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos n = p.relative(d).above(dy);
                    if (Math.abs(n.getX() - 22) > 40 || Math.abs(n.getZ() - 25) > 40 || n.getY() < -8 || n.getY() > 20) continue;
                    if (reach.contains(n) || !stand(n)) continue;
                    if (dy == 1 && !free(p.above(2))) continue;
                    if (dy == -1 && !free(n.above(2))) continue;
                    reach.add(n);
                    q.add(n);
                    break;
                }
            }
            if (ladder(p) || ladder(p.below())) {
                for (int dy : new int[]{1, -1}) {
                    BlockPos n = p.above(dy);
                    if (!reach.contains(n) && (ladder(n) || stand(n)) && free(n.above())) {
                        reach.add(n);
                        q.add(n);
                    }
                }
            }
        }
        // ---------------------------------------------------------------- 2. every room must be walkable
        Map<String, Integer> perRoom = new HashMap<>();
        for (BlockPos p : reach) {
            String r = House.roomAt(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            perRoom.merge(r, 1, Integer::sum);
        }
        for (String r : House.ROOMS.keySet()) {
            if (r.equals("B_TUNNEL") || r.equals("ROAD")) continue;
            if (!perRoom.containsKey(r)) issues.add("room " + r + " cannot be reached on foot");
        }
        // ---------------------------------------------------------------- 3. every hotspot must be within reach
        Set<String> seenTags = new HashSet<>();
        for (Map.Entry<BlockPos, String> e : House.HOT.entrySet()) {
            String tag = e.getValue();
            BlockPos h = e.getKey();
            if (tag.equals("tunnelgate")) continue;
            boolean ok = false;
            for (BlockPos c : reach) {
                double dx = c.getX() + 0.5 - (h.getX() + 0.5), dy = c.getY() + 1.62 - (h.getY() + 0.5), dz = c.getZ() + 0.5 - (h.getZ() + 0.5);
                if (dx * dx + dy * dy + dz * dz <= 4.2 * 4.2) { ok = true; break; }
            }
            if (!ok && seenTags.add(tag + fmt(h))) issues.add("hotspot " + tag + " at " + fmt(h) + " is out of reach");
            // the block must actually be there
            BlockState s = st(h);
            if (s.isAir() && !tag.startsWith("door:")) issues.add("hotspot " + tag + " at " + fmt(h) + " is air");
        }
        // ---------------------------------------------------------------- 4. doors: both sides open floor
        for (Map.Entry<String, BlockPos> e : House.DOORS.entrySet()) {
            BlockPos d = e.getValue();
            BlockState s = st(d);
            if (!isOpenable(s)) {
                issues.add("door " + e.getKey() + " at " + fmt(d) + " is " + s.getBlock());
                continue;
            }
            if (s.getBlock() instanceof TrapDoorBlock) continue;
            Direction f = s.hasProperty(DoorBlock.FACING) ? s.getValue(DoorBlock.FACING) : s.getValue(FenceGateBlock.FACING);
            for (Direction side : new Direction[]{f, f.getOpposite()}) {
                BlockPos n = d.relative(side);
                if (!stand(n) && !stand(n.below()) && !stand(n.above())) issues.add("door " + e.getKey() + " is blocked on its " + side + " side at " + fmt(n) + " by " + st(n).getBlock());
            }
            if (s.getBlock() instanceof DoorBlock && !(st(d.above()).getBlock() instanceof DoorBlock)) issues.add("door " + e.getKey() + " has no upper half");
        }
        // ---------------------------------------------------------------- 5. the shell must be closed
        for (int fy : new int[]{House.G, House.F}) {
            for (int y = fy + 1; y <= fy + 4; y++) {
                for (int x = -1; x <= 45; x++) for (int z : new int[]{-1, 31}) hole(new BlockPos(x, y, z));
                for (int z = -1; z <= 31; z++) for (int x : new int[]{-1, 45}) hole(new BlockPos(x, y, z));
            }
            // ceilings
            for (int x = 0; x <= 44; x++) for (int z = 0; z <= 30; z++) {
                BlockPos c = new BlockPos(x, fy + 5, z);
                if (st(c).isAir() && !stairwell(c)) issues.add("hole in the ceiling at " + fmt(c));
            }
        }
        // ---------------------------------------------------------------- 6. nothing floats, nothing is buried
        for (int x = -10; x <= 54; x++) for (int z = -10; z <= 60; z++) for (int y = -7; y <= 26; y++) {
            BlockPos l = new BlockPos(x, y, z);
            BlockState s = st(l);
            Block b = s.getBlock();
            boolean needsFloor = b instanceof CarpetBlock || b instanceof CandleBlock || b instanceof FlowerPotBlock
                    || (b instanceof LanternBlock && !s.getValue(LanternBlock.HANGING)) || b == Blocks.DEAD_BUSH || b == Blocks.FERN
                    || b instanceof BedBlock || (b.getDescriptionId().contains("hatman") && !b.getDescriptionId().contains("lamp")
                    && !b.getDescriptionId().contains("mirror") && !b.getDescriptionId().contains("mask") && !b.getDescriptionId().contains("plate")
                    && !b.getDescriptionId().contains("streamer") && !b.getDescriptionId().contains("wallpaper") && !b.getDescriptionId().contains("wainscot")
                    && !b.getDescriptionId().contains("plaster") && !b.getDescriptionId().contains("planks") && !b.getDescriptionId().contains("window")
                    && !b.getDescriptionId().contains("brick") && !b.getDescriptionId().contains("tile") && !b.getDescriptionId().contains("can_shelf"));
            if (needsFloor) {
                BlockState below = st(l.below());
                VoxelShape sh = below.getCollisionShape(lvl, o.offset(l.below()));
                if (below.isAir() || sh.isEmpty() && !(below.getBlock() == b))
                    issues.add("floating " + b.getDescriptionId().replace("block.", "") + " at " + fmt(l));
            }
            if (b instanceof LanternBlock && s.getValue(LanternBlock.HANGING) && st(l.above()).isAir()) issues.add("hanging lantern with nothing above at " + fmt(l));
            if (b.getDescriptionId().contains("ceiling_lamp") && st(l.above()).isAir()) issues.add("ceiling lamp hangs from nothing at " + fmt(l));
            if (b.getDescriptionId().contains("wall_lamp") || b.getDescriptionId().contains("mirror") || b.getDescriptionId().contains("wall_mask") || b.getDescriptionId().contains("name_plate")) {
                Direction f = s.getValue(com.krolasyon.hatman.block.FacingBlock.FACING);
                // these hang on the wall behind them (their back is toward -facing ... the model is drawn against the north face)
                BlockPos back = l.relative(f.getOpposite());
                BlockPos front = l.relative(f);
                if (st(back).isAir() && st(front).isAir()) issues.add("wall decoration " + b.getDescriptionId().replace("block.hatman.", "") + " floats at " + fmt(l));
            }
        }
    }

    private boolean stairwell(BlockPos c) {
        int x = c.getX(), y = c.getY(), z = c.getZ();
        if (y == House.G + 5 && x >= 21 && x <= 23 && z >= 5 && z <= 10) return true;
        if (y == House.G + 5 && x == 43 && z >= 5 && z <= 9) return true;
        if (y == House.G + 5 && (x == 27 || x == 28) && z >= 3 && z <= 9) return true;
        if (y == House.F + 5 && x == 15 && z == 9) return true;
        return false;
    }

    private void hole(BlockPos l) {
        BlockState s = st(l);
        if (s.isAir() && !(l.getX() == 22 && l.getZ() == 31)) issues.add("hole in the outer wall at " + fmt(l));
    }
}
