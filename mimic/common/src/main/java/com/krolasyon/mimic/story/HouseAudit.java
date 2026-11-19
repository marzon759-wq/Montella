package com.krolasyon.mimic.story;

import com.krolasyon.mimic.MimicMod;
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
        for (String s : a.issues) MimicMod.LOG.warn("[AUDIT] {}", s);
        MimicMod.LOG.info("[AUDIT] done: {} issues, {} reachable cells", a.issues.size(), a.reach.size());
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
        BlockPos start = new BlockPos(24, 1, 67);
        if (!stand(start)) issues.add("start cell in the foyer is not standable " + fmt(start));
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(start);
        reach.add(start);
        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            for (Direction d : Direction.Plane.HORIZONTAL) {
                for (int dy = 1; dy >= -1; dy--) {
                    BlockPos n = p.relative(d).above(dy);
                    if (n.getX() < -6 || n.getX() > 106 || n.getZ() < -6 || n.getZ() > 112 || n.getY() < -8 || n.getY() > 26) continue;
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
        // ---------------------------------------------------------------- 5. nothing floats: floor props stand on something, hanging things hang from
        // something, wall things have a wall behind them
        java.util.Set<String> floor = java.util.Set.of("chair", "chair_fallen", "bench", "table", "table_cloth", "stool", "armchair", "bookcase", "counter",
                "sink", "cook_stove", "wood_stove", "bed_head", "bed_foot", "dresser", "nightstand", "cabinet", "desk", "trunk", "crate", "barrel",
                "toolbox", "wardrobe", "grandfather_clock", "rug", "runner", "blood_pool", "bathtub", "toilet", "lantern", "oil_lamp", "radio_set",
                "cassette_player", "generator", "workbench", "bucket", "milk_can", "gas_can", "sacks", "hay_pile", "hay_mound", "straw_layer", "trough",
                "chopping_block", "butcher_table", "bone_pile", "skull", "corpse_head", "corpse_legs", "drifter", "scarecrow", "corn", "dry_grass",
                "well", "tire", "note_paper", "photo_frame", "candles", "walkie", "meat_chunk", "pitchfork", "shovel_prop", "post");
        java.util.Set<String> hanging = java.util.Set.of("lantern_hang", "chandelier", "bulb");
        java.util.Set<String> wall = java.util.Set.of("painting", "mirror", "sconce", "phone", "saddle", "bridle", "tool_rack", "antlers", "handprint",
                "chain_wall", "fuse_box", "padlock", "shelf_jars", "birdbox");
        for (int x = -6; x <= 106; x++) for (int z = -6; z <= 112; z++) for (int y = -6; y <= 28; y++) {
            BlockPos l = new BlockPos(x, y, z);
            BlockState s = st(l);
            if (s.isAir()) continue;
            Block b = s.getBlock();
            String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(b).getPath();
            boolean upper = s.hasProperty(com.krolasyon.mimic.block.TallBlock.HALF) && s.getValue(com.krolasyon.mimic.block.TallBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER;
            if (floor.contains(id) && !upper) {
                BlockState below = st(l.below());
                if (below.isAir() || below.getCollisionShape(lvl, o.offset(l.below())).isEmpty() && below.getBlock() != b && !below.is(Blocks.LIGHT))
                    issues.add("floating " + id + " at " + fmt(l));
            }
            if (hanging.contains(id) && st(l.above()).isAir()) issues.add(id + " hangs from nothing at " + fmt(l));
            if ((id.equals("carcass") || id.equals("hanging_body") || id.equals("meat_hook")) && upper && st(l.above()).isAir())
                issues.add(id + " hangs from nothing at " + fmt(l));
            if (wall.contains(id)) {
                Direction f = s.getValue(com.krolasyon.mimic.block.FacingBlock.FACING);
                BlockPos back = l.relative(f.getOpposite());
                if (st(back).isAir() || st(back).getCollisionShape(lvl, o.offset(back)).isEmpty() && !id.equals("birdbox"))
                    issues.add("wall prop " + id + " has no wall behind it at " + fmt(l));
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
