package com.krolasyon.hatman.story;

import com.krolasyon.hatman.block.FacingBlock;
import com.krolasyon.hatman.block.ModeBlock;
import com.krolasyon.hatman.block.OpenableBlock;
import com.krolasyon.hatman.block.TallBlock;
import com.krolasyon.hatman.block.VariantBlock;
import com.krolasyon.hatman.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Marlowe House. Everything is expressed in local coordinates: x east, z south (the front door faces +z), y up.
 * Ground floor block is y=0, first floor y=6, attic y=12, cellar floor y=-6.
 * Running the builder with a null level only fills the registries (hotspots, rooms, lamps, doors).
 */
public final class House {
    public static final int G = 0, F = 6, A = 12, B = -6;

    public static final Map<BlockPos, String> HOT = new HashMap<>();
    public static final Map<String, int[]> ROOMS = new LinkedHashMap<>();
    public static final Map<String, BlockPos> DOORS = new HashMap<>();
    public static final Map<String, List<BlockPos>> TAGGED = new HashMap<>();
    public static final List<Lamp> LAMPS = new ArrayList<>();
    public static final List<int[]> GUESTS = new ArrayList<>();
    public static final List<Hide> HIDES = new ArrayList<>();
    private static boolean indexed = false;

    public static final class Lamp {
        public final BlockPos pos;
        public final String room;
        public final int quality; // 0 healthy, 1 dying, 2 dead
        Lamp(BlockPos p, String r, int q) { pos = p; room = r; quality = q; }
    }

    public static final class Hide {
        public final String id;
        public final BlockPos pos;
        public final Direction facing;
        Hide(String id, BlockPos p, Direction f) { this.id = id; pos = p; facing = f; }
    }

    private final ServerLevel lvl;
    private final BlockPos o;
    private final Random rnd = new Random(1962);
    private String room = "";

    private House(ServerLevel lvl, BlockPos origin) {
        this.lvl = lvl;
        this.o = origin;
    }

    public static synchronized void ensureIndexed() {
        if (!indexed) new House(null, BlockPos.ZERO).run();
    }

    public static synchronized void build(ServerLevel level, BlockPos origin) {
        new House(level, origin).run();
    }

    public static String roomAt(double x, double y, double z) {
        ensureIndexed();
        for (Map.Entry<String, int[]> e : ROOMS.entrySet()) {
            int[] r = e.getValue();
            if (x >= r[0] && x < r[3] + 1 && y >= r[1] - 0.5 && y < r[4] + 1 && z >= r[2] && z < r[5] + 1) return e.getKey();
        }
        return "";
    }

    public static BlockPos first(String tag) {
        ensureIndexed();
        List<BlockPos> l = TAGGED.get(tag);
        return l == null || l.isEmpty() ? null : l.get(0);
    }

    // ------------------------------------------------------------------ primitives
    private void set(int x, int y, int z, BlockState s) {
        if (lvl != null) lvl.setBlock(o.offset(x, y, z), s, 2);
    }

    private void set(int x, int y, int z, Block b) { set(x, y, z, b.defaultBlockState()); }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        if (lvl == null) return;
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    lvl.setBlock(o.offset(x, y, z), s, 2);
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) { fill(x1, y1, z1, x2, y2, z2, b.defaultBlockState()); }

    private void air(int x1, int y1, int z1, int x2, int y2, int z2) { fill(x1, y1, z1, x2, y2, z2, Blocks.AIR); }

    private void tag(int x, int y, int z, String t) {
        BlockPos p = new BlockPos(x, y, z);
        HOT.put(p, t);
        TAGGED.computeIfAbsent(t, k -> new ArrayList<>()).add(p);
    }

    private int hash(int x, int y, int z) {
        int h = x * 73428767 ^ y * 912931 ^ z * 42317861;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return h & 0x7fffffff;
    }

    private BlockState facing(Block b, Direction d) { return b.defaultBlockState().setValue(FacingBlock.FACING, d); }

    private static BlockState log(Block b, Direction.Axis a) { return b.defaultBlockState().setValue(RotatedPillarBlock.AXIS, a); }

    private void room(String name, int x1, int y, int z1, int x2, int z2) {
        ROOMS.put(name, new int[]{x1, y, z1, x2, y + 3, z2});
        room = name;
    }

    // ------------------------------------------------------------------ fixtures
    private void door(String id, int x, int y, int z, Direction f, boolean open) {
        BlockState s = Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, f).setValue(DoorBlock.HINGE, DoorHingeSide.LEFT).setValue(DoorBlock.OPEN, open);
        air(x, y, z, x, y + 1, z);
        set(x, y, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, y + 1, z, s.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        // lintel above the door
        if (id != null) {
            DOORS.put(id, new BlockPos(x, y, z));
            tag(x, y, z, "door:" + id);
            tag(x, y + 1, z, "door:" + id);
        }
    }

    private void lamp(int x, int y, int z) { lampQ(x, y, z, hash(x, y, z) % 7 == 0 ? 1 : 0); }

    private void lampQ(int x, int y, int z, int q) {
        set(x, y, z, ModBlocks.CEILING_LAMP.get());
        LAMPS.add(new Lamp(new BlockPos(x, y, z), room, q));
    }

    private void sconce(int x, int y, int z, Direction f) {
        int q = hash(x, y, z) % 5 == 0 ? 2 : hash(x, y, z) % 5 == 1 ? 1 : 0;
        set(x, y, z, facing(ModBlocks.WALL_LAMP.get(), f));
        LAMPS.add(new Lamp(new BlockPos(x, y, z), room, q));
    }

    private void desk(int x, int y, int z, Direction f, String t) {
        set(x, y, z, facing(ModBlocks.DESK.get(), f));
        tag(x, y, z, "drawer:" + (t == null ? "empty" : t));
    }

    private void cab(int x, int y, int z, Direction f, String t) {
        set(x, y, z, facing(ModBlocks.CABINET.get(), f));
        tag(x, y, z, "drawer:" + (t == null ? "empty" : t));
    }

    private void trunk(int x, int y, int z, Direction f, String t) {
        set(x, y, z, facing(ModBlocks.TRUNK.get(), f));
        tag(x, y, z, "drawer:" + (t == null ? "empty" : t));
    }

    private void paper(int x, int y, int z, Direction f, int variant, String t) {
        set(x, y, z, facing(ModBlocks.PAPER.get(), f).setValue(VariantBlock.VARIANT, variant));
        if (t != null) tag(x, y, z, t);
    }

    private void tall(Block b, int x, int y, int z, Direction f, String t) {
        set(x, y, z, facing(b, f).setValue(TallBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, y + 1, z, facing(b, f).setValue(TallBlock.HALF, DoubleBlockHalf.UPPER));
        if (t != null) {
            tag(x, y, z, t);
            tag(x, y + 1, z, t);
        }
    }

    private void wardrobe(String id, int x, int y, int z, Direction f) {
        tall(ModBlocks.WARDROBE.get(), x, y, z, f, "wardrobe:" + id);
        HIDES.add(new Hide(id, new BlockPos(x, y, z), f));
    }

    private void put(Block b, int x, int y, int z, Direction f) { set(x, y, z, facing(b, f)); }

    private void mode(Block b, int x, int y, int z, Direction f, int m, String t) {
        set(x, y, z, facing(b, f).setValue(ModeBlock.MODE, m));
        if (t != null) tag(x, y, z, t);
    }

    private void variant(Block b, int x, int y, int z, Direction f, int v) {
        set(x, y, z, facing(b, f).setValue(VariantBlock.VARIANT, v));
    }

    private void table(int x1, int z1, int x2, int z2, int y) {
        fill(x1, y, z1, x2, y, z2, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP));
    }

    private void carpet(int x1, int z1, int x2, int z2, int y, Block c) {
        if (lvl == null) return;
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++)
                if (lvl.getBlockState(o.offset(x, y, z)).isAir() && hash(x, y, z) % 11 != 0) set(x, y, z, c);
    }

    private void web(int x, int y, int z) {
        if (lvl != null && lvl.getBlockState(o.offset(x, y, z)).isAir()) set(x, y, z, Blocks.COBWEB);
    }

    private void webs(int x1, int z1, int x2, int z2, int yTop, int count) {
        for (int i = 0; i < count; i++) {
            int c = rnd.nextInt(4);
            int x = c == 0 || c == 2 ? x1 : x2, z = c == 0 || c == 1 ? z1 : z2;
            if (rnd.nextBoolean()) x += (x == x1 ? 1 : -1) * rnd.nextInt(2);
            if (rnd.nextBoolean()) z += (z == z1 ? 1 : -1) * rnd.nextInt(2);
            web(x, yTop - rnd.nextInt(2), z);
        }
    }

    private void bed(int x, int y, int z, Direction head, Block b) {
        BlockPos foot = new BlockPos(x, y, z).relative(head.getOpposite());
        set(x, y, z, b.defaultBlockState().setValue(BedBlock.FACING, head).setValue(BedBlock.PART, BedPart.HEAD));
        set(foot.getX(), foot.getY(), foot.getZ(), b.defaultBlockState().setValue(BedBlock.FACING, head).setValue(BedBlock.PART, BedPart.FOOT));
        tag(x, y, z, "bed");
        tag(foot.getX(), foot.getY(), foot.getZ(), "bed");
    }

    private BlockState wallpaper(int floorY, int x, int y, int z) {
        int h = hash(x, y, z) % 100;
        if (h < 14) return ModBlocks.WALLPAPER_STAINED.get().defaultBlockState();
        return (floorY == F ? ModBlocks.WALLPAPER_RED.get() : ModBlocks.WALLPAPER_GREEN.get()).defaultBlockState();
    }

    /** an interior wall line with wainscot at the bottom */
    private void wall(int x1, int z1, int x2, int z2, int fy) {
        if (lvl == null) return;
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                set(x, fy + 1, z, ModBlocks.WAINSCOT.get());
                for (int y = fy + 2; y <= fy + 4; y++) set(x, y, z, wallpaper(fy, x, y, z));
            }
    }

    private BlockState stone(int x, int y, int z) {
        int h = hash(x, y, z) % 100;
        return (h < 22 ? Blocks.CRACKED_STONE_BRICKS : h < 36 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState();
    }

    private BlockState floorState(int x, int y, int z) {
        int h = hash(x, y, z) % 100;
        return (h < 12 ? ModBlocks.ROTTEN_PLANKS.get() : h < 30 ? Blocks.SPRUCE_PLANKS : Blocks.DARK_OAK_PLANKS).defaultBlockState();
    }

    private void window(int x, int fy, int z, Direction out) {
        for (int y = fy + 2; y <= fy + 3; y++) {
            set(x, y, z, ModBlocks.BOARDED_WINDOW.get());
            set(x + out.getStepX(), y, z + out.getStepZ(), Blocks.TINTED_GLASS);
        }
    }

    // ------------------------------------------------------------------ the build
    private void run() {
        HOT.clear();
        ROOMS.clear();
        DOORS.clear();
        TAGGED.clear();
        LAMPS.clear();
        GUESTS.clear();
        HIDES.clear();
        site();
        shell();
        ground();
        first();
        attic();
        cellar();
        yard();
        indexed = true;
    }

    private void site() {
        if (lvl == null) return;
        // clear the plot and lay new ground
        for (int x = -12; x <= 56; x++)
            for (int z = -12; z <= 62; z++) {
                for (int y = 34; y >= 1; y--) lvl.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                int h = hash(x, 0, z) % 100;
                BlockState top = (h < 38 ? Blocks.PODZOL : h < 70 ? Blocks.COARSE_DIRT : h < 86 ? Blocks.GRASS_BLOCK : Blocks.ROOTED_DIRT).defaultBlockState();
                lvl.setBlock(o.offset(x, 0, z), top, 2);
                for (int y = -1; y >= -3; y--) lvl.setBlock(o.offset(x, y, z), Blocks.DIRT.defaultBlockState(), 2);
            }
        // solid foundation under the house; the cellar is carved out of it later
        for (int x = -2; x <= 46; x++)
            for (int z = -2; z <= 46; z++)
                for (int y = -8; y <= -1; y++) {
                    if (z > 32 && (x < 8 || x > 24)) continue;
                    lvl.setBlock(o.offset(x, y, z), Blocks.STONE.defaultBlockState(), 2);
                }
    }

    private void shell() {
        if (lvl == null) return;
        // outer skin and inner lining, floors and ceilings
        for (int fy : new int[]{G, F}) {
            for (int x = 0; x <= 44; x++)
                for (int z = 0; z <= 30; z++) {
                    boolean tile = false;
                    set(x, fy, z, floorState(x, fy, z));
                    set(x, fy + 5, z, ModBlocks.PLASTER.get());
                    boolean edge = x == 0 || x == 44 || z == 0 || z == 30;
                    for (int y = fy + 1; y <= fy + 4; y++) {
                        if (edge) set(x, y, z, y == fy + 1 ? ModBlocks.WAINSCOT.get().defaultBlockState() : wallpaper(fy, x, y, z));
                    }
                }
            for (int y = fy; y <= fy + 5; y++) {
                for (int x = -1; x <= 45; x++) {
                    set(x, y, -1, stone(x, y, -1));
                    set(x, y, 31, stone(x, y, 31));
                }
                for (int z = -1; z <= 31; z++) {
                    set(-1, y, z, stone(-1, y, z));
                    set(45, y, z, stone(45, y, z));
                }
            }
        }
        // corner posts and a belt course
        for (int[] c : new int[][]{{-1, -1}, {45, -1}, {-1, 31}, {45, 31}, {14, 31}, {30, 31}, {14, -1}, {30, -1}})
            fill(c[0], 0, c[1], c[0], 11, c[1], log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        // interior partitions for both floors
        for (int fy : new int[]{G, F}) {
            wall(1, 13, 43, 13, fy);
            wall(1, 17, 43, 17, fy);
            wall(14, 1, 14, 12, fy);
            wall(30, 1, 30, 12, fy);
            wall(14, 18, 14, 29, fy);
            wall(30, 18, 30, 29, fy);
        }
        // windows
        for (int fy : new int[]{G, F}) {
            for (int x : new int[]{4, 7, 10, 18, 26, 34, 37, 40}) {
                if (!(fy == G && x >= 20 && x <= 24)) window(x, fy, 30, Direction.SOUTH);
                window(x, fy, 0, Direction.NORTH);
            }
            for (int z : new int[]{4, 8, 21, 25, 15}) {
                window(0, fy, z, Direction.WEST);
                if (!(z == 4 || z == 8)) window(44, fy, z, Direction.EAST);
            }
        }
        // attic floor and roof
        for (int x = 0; x <= 44; x++)
            for (int z = 0; z <= 30; z++) set(x, A, z, hash(x, A, z) % 5 == 0 ? ModBlocks.ROTTEN_PLANKS.get().defaultBlockState() : Blocks.SPRUCE_PLANKS.defaultBlockState());
        for (int x = -1; x <= 45; x++) {
            set(x, A, -1, stone(x, A, -1));
            set(x, A, 31, stone(x, A, 31));
        }
        for (int z = -1; z <= 31; z++) {
            set(-1, A, z, stone(-1, A, z));
            set(45, A, z, stone(45, A, z));
        }
        for (int z = -3; z <= 33; z++) {
            int d = Math.min(z + 3, 33 - z);
            int y = A + 1 + d / 2;
            BlockState slab = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, d % 2 == 0 ? SlabType.BOTTOM : SlabType.TOP);
            for (int x = -3; x <= 47; x++) {
                set(x, y, z, hash(x, y, z) % 23 == 0 ? Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, d % 2 == 0 ? SlabType.BOTTOM : SlabType.TOP) : slab);
            }
            // gable ends
            for (int yy = A + 1; yy < y + (d % 2 == 0 ? 0 : 1); yy++) {
                if (z >= -1 && z <= 31) {
                    set(-1, yy, z, stone(-1, yy, z));
                    set(45, yy, z, stone(45, yy, z));
                    if (z >= 0 && z <= 30) {
                        set(0, yy, z, Blocks.SPRUCE_PLANKS);
                        set(44, yy, z, Blocks.SPRUCE_PLANKS);
                    }
                }
            }
        }
        for (int x = -3; x <= 47; x++) set(x, A + 1 + 9, 15, Blocks.DEEPSLATE_TILES);
        // chimneys
        fill(2, A + 1, 23, 3, A + 12, 24, Blocks.BRICKS);
        fill(40, A + 1, 6, 41, A + 12, 7, Blocks.BRICKS);
    }

    // ================================================================== ground floor
    private void ground() {
        int y = G + 1;
        // ---------------- foyer
        room("G_FOYER", 15, y, 18, 29, 29);
        air(20, y, 17, 24, y + 2, 17);
        fill(20, y + 3, 17, 24, y + 3, 17, Blocks.DARK_OAK_PLANKS);
        air(22, y, 31, 22, y + 1, 31);
        door("front", 22, y, 30, Direction.SOUTH, false);
        carpet(19, 19, 25, 28, y, Blocks.RED_CARPET);
        desk(16, y, 22, Direction.EAST, "note:phone_pad");
        mode(ModBlocks.PHONE.get(), 16, y + 1, 22, Direction.EAST, 0, "phone");
        cab(16, y, 24, Direction.EAST, "item:cellar_key");
        set(16, y + 1, 24, Blocks.POTTED_DEAD_BUSH);
        put(ModBlocks.MIRROR.get(), 15, y + 1, 26, Direction.EAST);
        put(ModBlocks.MIRROR.get(), 15, y + 2, 26, Direction.EAST);
        tall(ModBlocks.CLOCK.get(), 29, y, 23, Direction.WEST, "clock");
        put(ModBlocks.ARMCHAIR.get(), 28, y, 27, Direction.WEST);
        cab(28, y, 20, Direction.WEST, null);
        variant(ModBlocks.HAT_STAND.get(), 28, y + 1, 20, Direction.WEST, 1);
        tall(ModBlocks.MANNEQUIN.get(), 16, y, 28, Direction.EAST, null);
        set(28, y, 29, Blocks.BARREL);
        lampQ(22, y + 3, 24, 0);
        sconce(15, y + 2, 20, Direction.EAST);
        sconce(15, y + 2, 28, Direction.EAST);
        sconce(29, y + 2, 19, Direction.WEST);
        sconce(29, y + 2, 26, Direction.WEST);
        webs(15, 18, 29, 29, y + 3, 7);

        // ---------------- corridor
        room("G_CORRIDOR", 1, y, 14, 43, 16);
        carpet(2, 15, 42, 15, y, Blocks.GRAY_CARPET);
        for (int x = 3; x <= 42; x += 6) {
            sconce(x, y + 2, 14, Direction.SOUTH);
            sconce(x + 3 > 43 ? 43 : x + 3, y + 2, 16, Direction.NORTH);
        }
        cab(4, y, 14, Direction.SOUTH, "note:doctor");
        set(4, y + 1, 14, Blocks.POTTED_WITHER_ROSE);
        cab(11, y, 16, Direction.NORTH, "battery");
        variant(ModBlocks.HAT_STAND.get(), 11, y + 1, 16, Direction.NORTH, 0);
        cab(33, y, 16, Direction.NORTH, null);
        set(33, y + 1, 16, Blocks.POTTED_DEAD_BUSH);
        cab(40, y, 14, Direction.SOUTH, "battery");
        put(ModBlocks.MIRROR.get(), 27, y + 1, 14, Direction.SOUTH);
        put(ModBlocks.ARMCHAIR.get(), 1, y, 16, Direction.EAST);
        put(ModBlocks.DINING_CHAIR.get(), 43, y, 14, Direction.WEST);
        set(43, y, 16, Blocks.BARREL);
        webs(1, 14, 43, 16, y + 3, 10);

        // ---------------- living room
        room("G_LIVING", 1, y, 18, 13, 29);
        door("living", 7, y, 17, Direction.NORTH, false);
        carpet(4, 21, 11, 27, y, Blocks.BROWN_CARPET);
        cab(13, y, 24, Direction.WEST, "battery");
        mode(ModBlocks.CRT_TV.get(), 13, y + 1, 24, Direction.WEST, 0, "tv");
        for (int z = 23; z <= 25; z++) put(ModBlocks.ARMCHAIR.get(), 8, y, z, Direction.EAST);
        put(ModBlocks.ARMCHAIR.get(), 10, y, 20, Direction.SOUTH);
        table(10, 23, 10, 25, y);
        paper(10, y + 1, 24, Direction.WEST, 5, "note:newspaper");
        set(10, y + 1, 23, Blocks.POTTED_DEAD_BUSH);
        fill(1, y, 22, 1, y + 2, 26, Blocks.BRICKS);
        set(1, y, 24, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        air(1, y + 1, 24, 1, y + 1, 24);
        fill(1, y + 3, 22, 1, y + 3, 26, Blocks.DARK_OAK_PLANKS);
        fill(1, y, 18, 4, y + 2, 18, Blocks.BOOKSHELF);
        set(2, y + 1, 18, Blocks.CHISELED_BOOKSHELF);
        cab(1, y, 28, Direction.EAST, null);
        variant(ModBlocks.HAT_STAND.get(), 1, y + 1, 28, Direction.EAST, 3);
        cab(12, y, 18, Direction.SOUTH, null);
        set(12, y + 1, 18, Blocks.CANDLE);
        set(13, y, 29, Blocks.BARREL);
        set(12, y, 29, Blocks.BARREL);
        set(13, y + 1, 29, Blocks.BARREL);
        paper(5, y, 26, Direction.NORTH, 1, "litter");
        paper(11, y, 21, Direction.EAST, 1, "litter");
        lampQ(7, y + 3, 24, 0);
        sconce(13, y + 2, 20, Direction.WEST);
        sconce(13, y + 2, 28, Direction.WEST);
        lampQ(4, y + 3, 20, 1);
        sconce(1, y + 2, 20, Direction.EAST);
        webs(1, 18, 13, 29, y + 3, 9);

        // ---------------- dining room
        room("G_DINING", 31, y, 18, 43, 29);
        door("dining", 37, y, 17, Direction.NORTH, false);
        carpet(32, 21, 42, 26, y, Blocks.RED_CARPET);
        table(33, 23, 41, 24, y);
        int v = 0;
        for (int x = 33; x <= 41; x += 2) {
            put(ModBlocks.DINING_CHAIR.get(), x, y, 22, Direction.SOUTH);
            put(ModBlocks.DINING_CHAIR.get(), x, y, 25, Direction.NORTH);
            paper(x, y + 1, 23, Direction.NORTH, 4, null);
            paper(x, y + 1, 24, Direction.SOUTH, 4, null);
            GUESTS.add(new int[]{x, y, 22, 0, v++ % 3, 1});
            if (x != 37) GUESTS.add(new int[]{x, y, 25, 180, v++ % 3, 1});
        }
        paper(36, y + 1, 23, Direction.EAST, 0, "note:guest_list");
        set(34, y + 1, 24, Blocks.CANDLE);
        set(38, y + 1, 23, Blocks.CANDLE);
        set(40, y + 1, 24, Blocks.POTTED_WITHER_ROSE);
        put(ModBlocks.ARMCHAIR.get(), 42, y, 24, Direction.WEST);
        tag(42, y, 24, "host_chair");
        put(ModBlocks.DINING_CHAIR.get(), 32, y, 24, Direction.EAST);
        tag(32, y, 24, "seat");
        for (int x = 32; x <= 35; x++) cab(x, y, 29, Direction.NORTH, x == 33 ? "battery" : null);
        variant(ModBlocks.HAT_STAND.get(), 34, y + 1, 29, Direction.NORTH, 0);
        set(32, y + 1, 29, Blocks.CANDLE);
        put(ModBlocks.MIRROR.get(), 43, y + 1, 21, Direction.WEST);
        put(ModBlocks.MIRROR.get(), 43, y + 2, 21, Direction.WEST);
        lampQ(35, y + 3, 23, 0);
        lampQ(40, y + 3, 24, 0);
        sconce(31, y + 2, 20, Direction.EAST);
        sconce(31, y + 2, 27, Direction.EAST);
        webs(31, 18, 43, 29, y + 3, 8);

        // ---------------- study (Ruth's base camp)
        room("G_STUDY", 1, y, 1, 13, 12);
        door("study", 7, y, 13, Direction.SOUTH, false);
        carpet(4, 4, 10, 9, y, Blocks.GREEN_CARPET);
        desk(3, y, 1, Direction.SOUTH, "note:ruth_rules");
        desk(4, y, 1, Direction.SOUTH, "battery");
        put(ModBlocks.CAMCORDER.get(), 3, y + 1, 1, Direction.SOUTH);
        tag(3, y + 1, 1, "camcorder");
        paper(4, y + 1, 1, Direction.SOUTH, 1, "note:case_file");
        set(5, y, 1, Blocks.DARK_OAK_PLANKS);
        set(5, y + 1, 1, Blocks.LANTERN);
        put(ModBlocks.DINING_CHAIR.get(), 4, y, 2, Direction.NORTH);
        fill(1, y, 4, 1, y + 2, 11, Blocks.BOOKSHELF);
        set(1, y + 1, 6, Blocks.CHISELED_BOOKSHELF);
        set(1, y + 2, 9, Blocks.COBWEB);
        table(6, 6, 7, 7, y);
        paper(6, y + 1, 6, Direction.EAST, 3, "note:photo");
        paper(7, y + 1, 7, Direction.NORTH, 1, "litter");
        desk(13, y, 5, Direction.WEST, null);
        desk(13, y, 6, Direction.WEST, null);
        variant(ModBlocks.HAT_STAND.get(), 13, y + 1, 5, Direction.WEST, 2);
        put(ModBlocks.ARMCHAIR.get(), 11, y, 6, Direction.EAST);
        cab(13, y, 10, Direction.WEST, null);
        carpet(9, 10, 11, 11, y, Blocks.LIGHT_GRAY_CARPET); // Ruth's bedroll
        set(12, y, 11, Blocks.BARREL);
        lampQ(7, y + 3, 6, 0);
        sconce(13, y + 2, 8, Direction.WEST);
        lampQ(10, y + 3, 10, 0);
        sconce(1, y + 2, 2, Direction.EAST);
        webs(1, 1, 13, 12, y + 3, 6);

        // ---------------- stair hall
        room("G_STAIRHALL", 15, y, 1, 29, 12);
        air(20, y, 13, 24, y + 2, 13);
        fill(20, y + 3, 13, 24, y + 3, 13, Blocks.DARK_OAK_PLANKS);
        air(21, G + 5, 5, 23, G + 6, 10);
        for (int k = 0; k <= 5; k++) {
            int z = 10 - k;
            for (int x = 21; x <= 23; x++) {
                set(x, y + k, z, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
                for (int yy = y; yy < y + k; yy++) set(x, yy, z, Blocks.DARK_OAK_PLANKS);
            }
            // banister
            for (int x : new int[]{20, 24}) {
                for (int yy = y; yy < y + k; yy++) set(x, yy, z, Blocks.DARK_OAK_PLANKS);
                set(x, y + k, z, Blocks.DARK_OAK_PLANKS);
                set(x, y + k + 1, z, Blocks.DARK_OAK_FENCE);
            }
        }
        fill(20, y, 1, 24, G + 5, 4, Blocks.DARK_OAK_PLANKS); // solid mass under the landing
        for (int x = 21; x <= 23; x++) {
            set(x, y, 11, Blocks.DARK_OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.NORTH));
            tag(x, y, 11, "door:stairgate");
        }
        DOORS.put("stairgate", new BlockPos(22, y, 11));
        carpet(21, 12, 23, 12, y, Blocks.RED_CARPET);
        // cellar stairwell
        wall(26, 1, 26, 10, G);
        wall(26, 10, 29, 10, G);
        door("cellar", 28, y, 10, Direction.SOUTH, false);
        for (int k = 0; k <= 5; k++) {
            int z = 8 - k;
            for (int x = 27; x <= 28; x++) {
                air(x, -k, z, x, y + 3, z);
                set(x, -k, z, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
            }
        }
        air(27, y, 9, 28, y + 3, 9);
        fill(29, y, 1, 29, y + 3, 9, Blocks.DARK_OAK_PLANKS);
        // west side
        put(ModBlocks.PIANO.get(), 16, y, 1, Direction.SOUTH);
        put(ModBlocks.PIANO.get(), 17, y, 1, Direction.SOUTH);
        tag(16, y, 1, "piano");
        tag(17, y, 1, "piano");
        put(ModBlocks.DINING_CHAIR.get(), 16, y, 3, Direction.NORTH);
        cab(15, y, 7, Direction.EAST, null);
        variant(ModBlocks.HAT_STAND.get(), 15, y + 1, 7, Direction.EAST, 4);
        put(ModBlocks.MIRROR.get(), 15, y + 1, 10, Direction.EAST);
        put(ModBlocks.MIRROR.get(), 15, y + 2, 10, Direction.EAST);
        lampQ(18, y + 3, 7, 0);
        sconce(25, y + 2, 12, Direction.NORTH);
        webs(15, 1, 25, 12, y + 3, 6);

        // ---------------- kitchen
        room("G_KITCHEN", 31, y, 1, 43, 12);
        door("kitchen", 37, y, 13, Direction.SOUTH, false);
        fill(31, G, 1, 43, G, 12, ModBlocks.TILE_FLOOR.get());
        wall(42, 1, 42, 10, G);
        air(43, G + 5, 5, 43, G + 6, 9);
        for (int k = 0; k <= 5; k++) {
            int z = 10 - k;
            set(43, y + k, z, Blocks.SPRUCE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH));
            for (int yy = y; yy < y + k; yy++) set(43, yy, z, Blocks.SPRUCE_PLANKS);
        }
        fill(43, y, 1, 43, G + 5, 4, Blocks.SPRUCE_PLANKS);
        for (int x = 31; x <= 36; x++) cab(x, y, 1, Direction.SOUTH, x == 32 ? "note:maid" : x == 35 ? "battery" : null);
        set(37, y, 1, Blocks.SMOKER);
        set(38, y, 1, Blocks.CAULDRON);
        set(39, y, 1, Blocks.BARREL);
        set(40, y, 1, Blocks.BARREL);
        set(40, y + 1, 1, Blocks.BARREL);
        set(33, y + 1, 1, Blocks.POTTED_DEAD_BUSH);
        set(36, y + 1, 1, Blocks.CANDLE);
        for (int z = 4; z <= 8; z++) {
            put(ModBlocks.CAN_SHELF.get(), 31, y + 1, z, Direction.EAST);
            put(ModBlocks.CAN_SHELF.get(), 31, y + 2, z, Direction.EAST);
            cab(31, y, z, Direction.EAST, null);
        }
        table(35, 6, 37, 7, y);
        paper(36, y + 1, 6, Direction.NORTH, 4, null);
        set(37, y + 1, 7, Blocks.CANDLE);
        put(ModBlocks.DINING_CHAIR.get(), 36, y, 5, Direction.SOUTH);
        put(ModBlocks.DINING_CHAIR.get(), 36, y, 8, Direction.NORTH);
        put(ModBlocks.DINING_CHAIR.get(), 34, y, 6, Direction.EAST);
        set(39, y + 3, 5, Blocks.CHAIN);
        set(39, y + 2, 5, Blocks.CHAIN);
        set(34, y + 3, 9, Blocks.CHAIN);
        set(41, y, 11, Blocks.COMPOSTER);
        set(40, y, 12, Blocks.BARREL);
        lampQ(36, y + 3, 6, 1);
        sconce(31, y + 2, 10, Direction.EAST);
        lampQ(34, y + 3, 10, 0);
        webs(31, 1, 41, 12, y + 3, 7);
    }

    // ================================================================== first floor
    private void first() {
        int y = F + 1;
        // ---------------- party corridor
        room("F_CORRIDOR", 1, y, 14, 43, 16);
        carpet(2, 15, 42, 15, y, Blocks.PURPLE_CARPET);
        for (int x = 2; x <= 42; x++) {
            if (x % 2 == 0) put(ModBlocks.STREAMER.get(), x, y + 3, 14 + (x / 2) % 3, (x / 2) % 2 == 0 ? Direction.NORTH : Direction.EAST);
        }
        int mv = 0;
        for (int x = 3; x <= 12; x += 3) {
            variant(ModBlocks.WALL_MASK.get(), x, y + 1, 14, Direction.SOUTH, mv++ % 5);
            put(ModBlocks.NAME_PLATE.get(), x, y, 14, Direction.SOUTH);
            tag(x, y, 14, "plates");
            tag(x, y + 1, 14, "plates");
            variant(ModBlocks.WALL_MASK.get(), x + 1, y + 1, 16, Direction.NORTH, mv++ % 5);
            put(ModBlocks.NAME_PLATE.get(), x + 1, y, 16, Direction.NORTH);
            tag(x + 1, y, 16, "plates");
            tag(x + 1, y + 1, 16, "plates");
        }
        for (int x = 32; x <= 41; x += 3) {
            variant(ModBlocks.WALL_MASK.get(), x, y + 1, 14, Direction.SOUTH, mv++ % 5);
            put(ModBlocks.NAME_PLATE.get(), x, y, 14, Direction.SOUTH);
            tag(x, y, 14, "plates");
            tag(x, y + 1, 14, "plates");
            variant(ModBlocks.WALL_MASK.get(), x + 1, y + 1, 16, Direction.NORTH, mv++ % 5);
            put(ModBlocks.NAME_PLATE.get(), x + 1, y, 16, Direction.NORTH);
            tag(x + 1, y, 16, "plates");
            tag(x + 1, y + 1, 16, "plates");
        }
        // Ruth's mask - and an empty hook
        variant(ModBlocks.WALL_MASK.get(), 26, y + 1, 16, Direction.NORTH, 5);
        put(ModBlocks.NAME_PLATE.get(), 26, y, 16, Direction.NORTH);
        tag(26, y, 16, "plate_ruth");
        tag(26, y + 1, 16, "plate_ruth");
        put(ModBlocks.NAME_PLATE.get(), 28, y, 16, Direction.NORTH);
        tag(28, y, 16, "plate_you");
        cab(18, y, 16, Direction.NORTH, null);
        variant(ModBlocks.HAT_STAND.get(), 18, y + 1, 16, Direction.NORTH, 5);
        set(17, y, 16, Blocks.CARVED_PUMPKIN.defaultBlockState());
        set(41, y, 14, Blocks.CARVED_PUMPKIN.defaultBlockState());
        set(2, y, 14, Blocks.FLOWER_POT);
        paper(9, y, 15, Direction.EAST, 1, "litter");
        paper(34, y, 16, Direction.NORTH, 1, "litter");
        for (int x = 5; x <= 41; x += 9) {
            sconce(x, y + 2, 14, Direction.SOUTH);
            sconce(x + 4, y + 2, 16, Direction.NORTH);
        }
        webs(1, 14, 43, 16, y + 3, 12);

        // ---------------- master bedroom
        room("F_MASTER", 1, y, 18, 13, 29);
        door("master", 7, y, 17, Direction.NORTH, false);
        carpet(4, 22, 10, 27, y, Blocks.RED_CARPET);
        bed(6, y, 28, Direction.SOUTH, Blocks.RED_BED);
        bed(7, y, 28, Direction.SOUTH, Blocks.RED_BED);
        cab(5, y, 29, Direction.NORTH, "battery");
        cab(8, y, 29, Direction.NORTH, null);
        put(ModBlocks.RECORDER.get(), 8, y + 1, 29, Direction.NORTH);
        tag(8, y + 1, 29, "recorder");
        set(5, y + 1, 29, Blocks.CANDLE);
        wardrobe("master", 13, y, 20, Direction.WEST);
        wardrobe("master2", 13, y, 21, Direction.WEST);
        desk(1, y, 24, Direction.EAST, "note:eleanor");
        put(ModBlocks.MIRROR.get(), 1, y + 1, 24, Direction.EAST);
        put(ModBlocks.DINING_CHAIR.get(), 2, y, 24, Direction.WEST);
        set(1, y, 20, facing(ModBlocks.SAFE.get(), Direction.EAST));
        tag(1, y, 20, "safe");
        set(1, y + 1, 20, Blocks.DARK_OAK_PLANKS);
        put(ModBlocks.MIRROR.get(), 1, y + 2, 20, Direction.EAST);
        trunk(6, y, 25, Direction.NORTH, null);
        put(ModBlocks.ARMCHAIR.get(), 11, y, 27, Direction.WEST);
        tall(ModBlocks.MANNEQUIN.get(), 2, y, 28, Direction.EAST, null);
        door("dress", 14, y, 24, Direction.EAST, false);
        lampQ(7, y + 3, 24, 0);
        sconce(13, y + 2, 26, Direction.WEST);
        lampQ(10, y + 3, 20, 0);
        sconce(1, y + 2, 27, Direction.EAST);
        webs(1, 18, 13, 29, y + 3, 8);

        // ---------------- dressing room
        room("F_DRESSING", 15, y, 18, 29, 29);
        door("dressing", 22, y, 17, Direction.NORTH, false);
        carpet(18, 21, 26, 26, y, Blocks.GRAY_CARPET);
        for (int[] m : new int[][]{{17, 20}, {19, 27}, {21, 21}, {24, 27}, {26, 20}, {27, 24}})
            tall(ModBlocks.MANNEQUIN.get(), m[0], y, m[1], m[1] > 24 ? Direction.NORTH : Direction.SOUTH, "mannequin");
        put(ModBlocks.PIANO.get(), 22, y, 29, Direction.NORTH);
        put(ModBlocks.PIANO.get(), 23, y, 29, Direction.NORTH);
        tag(22, y, 29, "piano");
        tag(23, y, 29, "piano");
        for (int z = 21; z <= 25; z += 2) {
            put(ModBlocks.MIRROR.get(), 29, y, z, Direction.WEST);
            put(ModBlocks.MIRROR.get(), 29, y + 1, z, Direction.WEST);
        }
        wardrobe("dress", 16, y, 29, Direction.NORTH);
        put(ModBlocks.HAT_BOX.get(), 28, y, 28, Direction.WEST);
        put(ModBlocks.HAT_BOX.get(), 28, y, 29, Direction.NORTH);
        put(ModBlocks.HAT_BOX.get(), 28, y + 1, 29, Direction.EAST);
        desk(15, y, 21, Direction.EAST, "battery");
        variant(ModBlocks.HAT_STAND.get(), 15, y + 1, 21, Direction.EAST, 0);
        lampQ(22, y + 3, 23, 1);
        lampQ(18, y + 3, 27, 0);
        sconce(29, y + 2, 28, Direction.WEST);
        webs(15, 18, 29, 29, y + 3, 8);

        // ---------------- Thomas's room
        room("F_CHILD", 31, y, 18, 43, 29);
        door("child", 37, y, 17, Direction.NORTH, false);
        for (int yy = y + 1; yy <= y + 3; yy++) {
            for (int x = 31; x <= 43; x++) if (!isWindow(x, yy, true)) set(x, yy, 30, ModBlocks.WALLPAPER_BLUE.get());
            for (int z = 18; z <= 29; z++) if (!isWindow(z, yy, false)) set(44, yy, z, ModBlocks.WALLPAPER_BLUE.get());
        }
        carpet(34, 21, 40, 26, y, Blocks.LIGHT_BLUE_CARPET);
        bed(42, y, 28, Direction.SOUTH, Blocks.BLUE_BED);
        desk(33, y, 18, Direction.SOUTH, "note:diary");
        desk(34, y, 18, Direction.SOUTH, null);
        mode(ModBlocks.MUSIC_BOX.get(), 33, y + 1, 18, Direction.SOUTH, 0, "musicbox");
        paper(34, y + 1, 18, Direction.SOUTH, 3, "note:drawing");
        put(ModBlocks.DINING_CHAIR.get(), 33, y, 19, Direction.NORTH);
        wardrobe("child", 43, y, 20, Direction.WEST);
        trunk(38, y, 29, Direction.NORTH, "battery");
        cab(41, y, 29, Direction.NORTH, null);
        variant(ModBlocks.HAT_STAND.get(), 41, y + 1, 29, Direction.NORTH, 5);
        set(32, y, 28, Blocks.NOTE_BLOCK);
        set(31, y, 26, Blocks.FLOWER_POT);
        put(ModBlocks.STREAMER.get(), 36, y + 3, 23, Direction.NORTH);
        put(ModBlocks.STREAMER.get(), 38, y + 3, 24, Direction.EAST);
        lampQ(37, y + 3, 24, 0);
        sconce(31, y + 2, 22, Direction.EAST);
        sconce(43, y + 2, 25, Direction.WEST);
        webs(31, 18, 43, 29, y + 3, 6);

        // ---------------- bathroom
        room("F_BATH", 1, y, 1, 6, 12);
        wall(7, 1, 7, 12, F);
        door("bath", 4, y, 13, Direction.SOUTH, false);
        fill(1, F, 1, 6, F, 12, ModBlocks.TILE_FLOOR.get());
        put(ModBlocks.BATHTUB.get(), 2, y, 1, Direction.EAST);
        put(ModBlocks.BATHTUB.get(), 3, y, 1, Direction.WEST);
        cab(1, y, 7, Direction.EAST, "note:mirror_note");
        put(ModBlocks.MIRROR.get(), 1, y + 1, 7, Direction.EAST);
        tag(1, y + 1, 7, "bathmirror");
        set(1, y, 10, Blocks.CAULDRON);
        set(6, y, 11, Blocks.BARREL);
        set(5, y + 3, 2, Blocks.CHAIN);
        lampQ(3, y + 3, 6, 0);
        webs(1, 1, 6, 12, y + 3, 4);

        // ---------------- guest room (Ruth slept here once)
        room("F_GUEST", 8, y, 1, 13, 12);
        door("guest", 10, y, 13, Direction.SOUTH, false);
        carpet(9, 4, 12, 9, y, Blocks.GREEN_CARPET);
        bed(12, y, 2, Direction.NORTH, Blocks.GREEN_BED);
        trunk(9, y, 2, Direction.SOUTH, "note:ruth_bag");
        wardrobe("guest", 13, y, 6, Direction.WEST);
        desk(8, y, 8, Direction.EAST, "battery");
        set(8, y + 1, 8, Blocks.CANDLE);
        put(ModBlocks.DINING_CHAIR.get(), 9, y, 8, Direction.WEST);
        lampQ(10, y + 3, 6, 0);
        webs(8, 1, 13, 12, y + 3, 4);

        // ---------------- upper stair hall
        room("F_STAIRHALL", 15, y, 1, 29, 12);
        air(20, y, 13, 24, y + 2, 13);
        fill(20, y + 3, 13, 24, y + 3, 13, Blocks.DARK_OAK_PLANKS);
        for (int z = 5; z <= 11; z++) {
            set(20, y, z, Blocks.DARK_OAK_FENCE);
            set(24, y, z, Blocks.DARK_OAK_FENCE);
        }
        for (int x = 20; x <= 24; x++) set(x, y, 11, Blocks.DARK_OAK_FENCE);
        for (int yy = y; yy <= A; yy++) {
            air(15, yy, 9, 15, yy, 9);
            set(15, yy, 9, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.EAST));
        }
        set(15, A, 9, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.EAST).setValue(TrapDoorBlock.HALF, Half.TOP));
        tag(15, A, 9, "door:hatch");
        DOORS.put("hatch", new BlockPos(15, A, 9));
        cab(27, y, 1, Direction.SOUTH, null);
        variant(ModBlocks.HAT_STAND.get(), 27, y + 1, 1, Direction.SOUTH, 1);
        put(ModBlocks.ARMCHAIR.get(), 28, y, 6, Direction.WEST);
        tall(ModBlocks.CLOCK.get(), 29, y, 9, Direction.WEST, null);
        put(ModBlocks.MIRROR.get(), 22, y + 1, 1, Direction.SOUTH);
        put(ModBlocks.MIRROR.get(), 22, y + 2, 1, Direction.SOUTH);
        carpet(21, 1, 23, 4, y, Blocks.RED_CARPET);
        lampQ(22, y + 3, 3, 0);
        sconce(17, y + 2, 1, Direction.SOUTH);
        sconce(29, y + 2, 4, Direction.WEST);
        webs(15, 1, 29, 12, y + 3, 7);

        // ---------------- library
        room("F_LIBRARY", 31, y, 1, 43, 12);
        door("library", 37, y, 13, Direction.SOUTH, false);
        wall(42, 1, 42, 10, F);
        door("servant", 42, y, 2, Direction.EAST, false);
        fill(31, y, 1, 40, y + 2, 1, Blocks.BOOKSHELF);
        fill(31, y, 2, 31, y + 2, 11, Blocks.BOOKSHELF);
        set(31, y + 1, 5, Blocks.CHISELED_BOOKSHELF);
        set(34, y + 1, 1, Blocks.CHISELED_BOOKSHELF);
        set(38, y + 2, 1, Blocks.COBWEB);
        fill(41, y, 5, 41, y + 2, 9, Blocks.BRICKS);
        set(41, y, 7, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        air(41, y + 1, 7, 41, y + 1, 7);
        tag(41, y, 7, "fireplace");
        put(ModBlocks.ARMCHAIR.get(), 38, y, 7, Direction.WEST);
        tag(38, y, 7, "his_chair");
        put(ModBlocks.ARMCHAIR.get(), 35, y, 7, Direction.EAST);
        table(36, 9, 37, 9, y);
        paper(36, y + 1, 9, Direction.NORTH, 2, "note:folklore");
        set(37, y + 1, 9, Blocks.CANDLE);
        desk(34, y, 12, Direction.NORTH, "note:invoice");
        desk(35, y, 12, Direction.NORTH, null);
        put(ModBlocks.DINING_CHAIR.get(), 34, y, 11, Direction.SOUTH);
        carpet(33, 4, 39, 10, y, Blocks.RED_CARPET);
        lampQ(36, y + 3, 6, 0);
        sconce(40, y + 2, 12, Direction.NORTH);
        sconce(32, y + 2, 12, Direction.NORTH);
        webs(31, 1, 41, 12, y + 3, 7);
    }

    private static boolean isWindow(int c, int yy, boolean southFace) {
        if (yy < F + 2 || yy > F + 3) return false;
        if (southFace) return c == 34 || c == 37 || c == 40;
        return c == 21 || c == 25;
    }

    // ================================================================== attic
    private void attic() {
        int y = A + 1;
        room("ATTIC", 1, y, 7, 43, 23);
        ROOMS.put("ATTIC", new int[]{1, y, 7, 43, y + 6, 23});
        fill(1, y, 6, 43, y + 2, 6, Blocks.SPRUCE_PLANKS);
        fill(1, y, 24, 43, y + 2, 24, Blocks.SPRUCE_PLANKS);
        for (int x = 6; x <= 40; x += 7)
            for (int z = 7; z <= 23; z++) {
                int d = Math.min(z + 3, 33 - z);
                int ry = A + 1 + d / 2 - 1;
                set(x, Math.min(ry, y + 5), z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Z));
            }
        // the fitting room (west)
        for (int x = 3; x <= 11; x += 2) {
            put(ModBlocks.MIRROR.get(), x, y, 7, Direction.SOUTH);
            put(ModBlocks.MIRROR.get(), x, y + 1, 7, Direction.SOUTH);
            tag(x, y, 7, "atticmirror");
            tag(x, y + 1, 7, "atticmirror");
        }
        for (int z = 9; z <= 21; z += 2) {
            put(ModBlocks.NAME_PLATE.get(), 1, y + 1, z, Direction.EAST);
            if (z != 15) variant(ModBlocks.HAT_STAND.get(), 2, y, z, Direction.EAST, (z / 2) % 3);
            tag(1, y + 1, z, z == 15 ? "hook_you" : "hooks");
        }
        put(ModBlocks.HAT_BOX.get(), 2, y, 15, Direction.EAST);
        tag(2, y, 15, "hatbox");
        tall(ModBlocks.MANNEQUIN.get(), 8, y, 15, Direction.WEST, "ruth_coat");
        carpet(4, 12, 10, 18, y, Blocks.RED_CARPET);
        desk(18, y, 7, Direction.SOUTH, "note:confession");
        desk(19, y, 7, Direction.SOUTH, null);
        paper(19, y + 1, 7, Direction.SOUTH, 3, "badge");
        set(18, y + 1, 7, Blocks.CANDLE);
        put(ModBlocks.DINING_CHAIR.get(), 18, y, 8, Direction.NORTH);
        for (int[] m : new int[][]{{12, 21}, {14, 22}, {5, 21}, {21, 20}})
            tall(ModBlocks.MANNEQUIN.get(), m[0], y, m[1], Direction.NORTH, null);
        // storage (east)
        trunk(40, y, 20, Direction.WEST, "item:lamp_oil");
        trunk(38, y, 22, Direction.NORTH, "item:lighter");
        trunk(34, y, 8, Direction.SOUTH, "battery");
        trunk(30, y, 22, Direction.NORTH, null);
        for (int[] b : new int[][]{{42, 9}, {42, 10}, {41, 9}, {36, 22}, {37, 22}, {26, 8}, {27, 8}, {42, 18}}) {
            set(b[0], y, b[1], Blocks.BARREL);
            if (hash(b[0], y, b[1]) % 2 == 0) set(b[0], y + 1, b[1], Blocks.BARREL);
        }
        for (int[] h : new int[][]{{24, 21}, {25, 22}, {32, 9}, {29, 9}}) put(ModBlocks.HAT_BOX.get(), h[0], y, h[1], Direction.NORTH);
        put(ModBlocks.ARMCHAIR.get(), 36, y, 14, Direction.WEST);
        put(ModBlocks.CRT_TV.get(), 39, y, 12, Direction.WEST);
        lampQ(8, y + 4, 15, 0);
        lampQ(22, y + 4, 15, 0);
        lampQ(36, y + 4, 15, 1);
        lampQ(15, y + 3, 20, 1);
        for (int i = 0; i < 40; i++) web(1 + rnd.nextInt(43), y + rnd.nextInt(4), 7 + rnd.nextInt(17));
    }

    // ================================================================== cellar
    private void cellar() {
        int y = B + 1;
        if (lvl != null) {
            carve(15, 1, 29, 12);
            carve(1, 1, 13, 12);
            carve(31, 1, 43, 12);
            carve(15, 14, 29, 29);
            carve(1, 14, 13, 22);
        }
        // ---------------- hall at the bottom of the stairs
        room("B_HALL", 15, y, 1, 29, 12);
        for (int k = 0; k <= 5; k++) {
            int z = 8 - k;
            for (int x = 27; x <= 28; x++) {
                air(x, -k + 1, z, x, -2, z);
                air(x, -1, z, x, G, z);
                set(x, -k, z, Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
                for (int yy = y; yy < -k; yy++) set(x, yy, z, Blocks.STONE_BRICKS);
            }
        }
        fill(29, y, 3, 29, -1, 9, Blocks.STONE_BRICKS);
        fill(26, y, 3, 26, -1, 9, Blocks.STONE_BRICKS);
        air(27, y, 9, 28, -1, 9);
        fill(27, y, 9, 28, -1, 9, Blocks.STONE_BRICKS);
        door("workshop", 14, y, 6, Direction.EAST, false);
        door("generator", 30, y, 6, Direction.WEST, false);
        door("winecellar", 22, y, 13, Direction.SOUTH, false);
        set(16, y, 1, Blocks.BARREL);
        set(17, y, 1, Blocks.BARREL);
        set(16, y + 1, 1, Blocks.BARREL);
        cab(15, y, 10, Direction.EAST, "battery");
        set(15, y + 1, 10, Blocks.CANDLE);
        put(ModBlocks.CAN_SHELF.get(), 19, y + 1, 1, Direction.SOUTH);
        put(ModBlocks.CAN_SHELF.get(), 20, y + 1, 1, Direction.SOUTH);
        put(ModBlocks.CAN_SHELF.get(), 19, y + 2, 1, Direction.SOUTH);
        set(19, y, 1, Blocks.BARREL);
        set(20, y, 1, Blocks.BARREL);
        set(24, y, 12, Blocks.COMPOSTER);
        lampQ(21, y + 3, 6, 0);
        sconce(25, y + 2, 12, Direction.NORTH);
        sconce(15, y + 2, 4, Direction.EAST);
        webs(15, 1, 25, 12, y + 3, 9);

        // ---------------- the hatter's workshop
        room("B_WORKSHOP", 1, y, 1, 13, 12);
        for (int z = 2; z <= 10; z++) {
            cab(1, y, z, Direction.EAST, z == 4 ? "note:making_log" : z == 8 ? "battery" : null);
            put(ModBlocks.CAN_SHELF.get(), 1, y + 1, z, Direction.EAST);
            if (z % 3 != 0) put(ModBlocks.CAN_SHELF.get(), 1, y + 2, z, Direction.EAST);
        }
        for (int x = 3; x <= 8; x++) {
            put(ModBlocks.CAN_SHELF.get(), x, y + 1, 1, Direction.SOUTH);
            put(ModBlocks.CAN_SHELF.get(), x, y + 2, 1, Direction.SOUTH);
            if (x % 2 == 1) set(x, y, 1, Blocks.BARREL);
            else cab(x, y, 1, Direction.SOUTH, null);
        }
        desk(10, y, 1, Direction.SOUTH, null);
        desk(11, y, 1, Direction.SOUTH, null);
        paper(10, y + 1, 1, Direction.SOUTH, 2, "ledger");
        set(11, y + 1, 1, Blocks.CANDLE);
        put(ModBlocks.ARMCHAIR.get(), 11, y, 3, Direction.NORTH);
        table(5, 5, 8, 6, y);
        variant(ModBlocks.HAT_STAND.get(), 5, y + 1, 5, Direction.SOUTH, 3);
        variant(ModBlocks.HAT_STAND.get(), 6, y + 1, 6, Direction.EAST, 4);
        variant(ModBlocks.HAT_STAND.get(), 8, y + 1, 5, Direction.SOUTH, 0);
        paper(7, y + 1, 5, Direction.EAST, 1, "litter");
        set(3, y, 9, Blocks.CAULDRON);
        set(4, y, 9, Blocks.CAULDRON);
        set(3, y, 11, Blocks.SMITHING_TABLE);
        set(12, y, 11, Blocks.LOOM);
        set(13, y, 9, Blocks.BARREL);
        trunk(12, y, 8, Direction.WEST, null);
        // the south wall hides a door behind shelving
        for (int yy = y; yy <= y + 1; yy++) {
            set(7, yy, 13, Blocks.BOOKSHELF);
            tag(7, yy, 13, "secret_shelf");
        }
        cab(9, y, 12, Direction.NORTH, null);
        variant(ModBlocks.HAT_STAND.get(), 9, y + 1, 12, Direction.NORTH, 3);
        tag(9, y + 1, 12, "hatblock");
        variant(ModBlocks.HAT_STAND.get(), 5, y, 12, Direction.NORTH, 3);
        variant(ModBlocks.HAT_STAND.get(), 4, y, 12, Direction.NORTH, 4);
        lampQ(7, y + 3, 6, 0);
        lampQ(11, y + 3, 10, 1);
        for (int i = 0; i < 26; i++) web(1 + rnd.nextInt(13), y + 1 + rnd.nextInt(3), 1 + rnd.nextInt(12));

        // ---------------- generator room
        room("B_GENERATOR", 31, y, 1, 43, 12);
        mode(ModBlocks.GENERATOR.get(), 37, y, 2, Direction.SOUTH, 0, "generator");
        mode(ModBlocks.GENERATOR.get(), 38, y, 2, Direction.SOUTH, 0, "generator");
        fill(36, y, 1, 39, y + 3, 1, Blocks.IRON_BARS);
        set(40, y + 2, 2, Blocks.CHAIN);
        set(40, y + 3, 2, Blocks.CHAIN);
        fill(42, y, 2, 43, y, 5, Blocks.COAL_BLOCK);
        set(43, y + 1, 3, Blocks.COAL_BLOCK);
        cab(31, y, 3, Direction.EAST, "note:gen_log");
        cab(31, y, 4, Direction.EAST, "battery");
        set(31, y + 1, 3, Blocks.CANDLE);
        set(33, y, 11, Blocks.BARREL);
        set(34, y, 12, Blocks.BARREL);
        set(42, y, 11, Blocks.CAULDRON);
        fill(35, y + 3, 6, 41, y + 3, 6, Blocks.CHAIN.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        lampQ(37, y + 3, 7, 0);
        sconce(31, y + 2, 9, Direction.EAST);
        webs(31, 1, 43, 12, y + 3, 7);

        // ---------------- wine cellar
        room("B_CELLAR", 15, y, 14, 29, 29);
        for (int z = 16; z <= 27; z += 3) {
            for (int x = 15; x <= 19; x++) {
                set(x, y, z, Blocks.BARREL.defaultBlockState());
                if (hash(x, y, z) % 3 != 0) set(x, y + 1, z, Blocks.BARREL.defaultBlockState());
            }
            for (int x = 25; x <= 29; x++) {
                set(x, y, z, Blocks.BARREL.defaultBlockState());
                if (hash(x, y, z) % 3 != 0) set(x, y + 1, z, Blocks.BARREL.defaultBlockState());
            }
        }
        trunk(17, y, 29, Direction.NORTH, "item:fuel_can");
        cab(27, y, 29, Direction.NORTH, "note:cellar_list");
        set(27, y + 1, 29, Blocks.CANDLE);
        // tunnel gate
        air(22, y, 30, 22, y + 1, 31);
        for (int yy = y; yy <= y + 1; yy++) {
            set(22, yy, 30, Blocks.IRON_BARS);
            tag(22, yy, 30, "tunnelgate");
        }
        lampQ(22, y + 3, 17, 0);
        lampQ(22, y + 3, 25, 1);
        webs(15, 14, 29, 29, y + 3, 10);

        // ---------------- cold room
        room("B_COLD", 1, y, 14, 13, 22);
        if (lvl != null) {
            for (int x = 1; x <= 13; x++)
                for (int z = 14; z <= 22; z++)
                    if (hash(x, B, z) % 4 == 0) set(x, B, z, Blocks.PACKED_ICE);
        }
        for (int x = 3; x <= 11; x += 4) {
            set(x, y + 3, 18, Blocks.CHAIN);
            set(x, y + 2, 18, Blocks.CHAIN);
        }
        put(ModBlocks.DINING_CHAIR.get(), 7, y, 20, Direction.NORTH);
        tag(7, y, 20, "ruth_chair");
        set(6, y, 21, Blocks.SOUL_LANTERN);
        cab(12, y, 21, Direction.WEST, null);
        carpet(5, 18, 9, 21, y, Blocks.WHITE_CARPET);

        // ---------------- escape tunnel
        if (lvl != null) {
            air(22, y, 32, 22, y + 1, 34);
            air(10, y, 34, 22, y + 1, 34);
            air(10, y, 34, 10, y + 1, 43);
            air(10, y, 43, 10, 3, 43);
            for (int yy = y; yy <= 0; yy++) set(10, yy, 43, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
            for (int yy = y; yy <= 0; yy++) set(10, yy, 44, Blocks.STONE_BRICKS);
            set(16, y + 1, 34, Blocks.COBWEB);
            set(10, y + 1, 38, Blocks.COBWEB);
        }
        ROOMS.put("B_TUNNEL", new int[]{9, y, 31, 23, y + 1, 44});
    }

    private void carve(int x1, int z1, int x2, int z2) {
        int y = B + 1;
        for (int x = x1 - 1; x <= x2 + 1; x++)
            for (int z = z1 - 1; z <= z2 + 1; z++) {
                boolean inside = x >= x1 && x <= x2 && z >= z1 && z <= z2;
                if (inside) {
                    set(x, B, z, hash(x, B, z) % 5 == 0 ? Blocks.COBBLESTONE : Blocks.STONE_BRICKS);
                    for (int yy = y; yy <= y + 3; yy++) set(x, yy, z, Blocks.AIR);
                    set(x, -1, z, hash(x, -1, z) % 6 == 0 ? Blocks.DARK_OAK_PLANKS.defaultBlockState() : ModBlocks.CELLAR_BRICK.get().defaultBlockState());
                } else {
                    for (int yy = y; yy <= y + 3; yy++) {
                        BlockState cur = lvl.getBlockState(o.offset(x, yy, z));
                        if (cur.is(Blocks.STONE)) set(x, yy, z, ModBlocks.CELLAR_BRICK.get());
                    }
                }
            }
    }

    // ================================================================== grounds
    private void yard() {
        ROOMS.put("YARD", new int[]{-8, 1, 32, 52, 6, 52});
        ROOMS.put("ROAD", new int[]{-10, 1, 53, 54, 6, 60});
        if (lvl == null) {
            DOORS.put("gate", new BlockPos(22, 1, 52));
            for (int x = 21; x <= 23; x++) tag(x, 1, 52, "door:gate");
            return;
        }
        // porch
        fill(18, 5, 32, 26, 5, 34, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
        for (int x : new int[]{18, 26}) fill(x, 1, 34, x, 4, 34, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        fill(18, 0, 32, 26, 0, 34, Blocks.STONE_BRICKS);
        set(19, 1, 32, Blocks.POTTED_DEAD_BUSH);
        set(22, 4, 33, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        set(18, 4, 33, Blocks.CHAIN);
        set(26, 4, 33, Blocks.CHAIN);
        set(25, 1, 32, Blocks.BARREL);
        // path to the gate
        for (int z = 35; z <= 57; z++)
            for (int x = 21; x <= 23; x++) set(x, 0, z, hash(x, 0, z) % 4 == 0 ? Blocks.COBBLESTONE : Blocks.GRAVEL);
        for (int x = -12; x <= 56; x++)
            for (int z = 54; z <= 57; z++) set(x, 0, z, hash(x, 0, z) % 5 == 0 ? Blocks.COARSE_DIRT : Blocks.GRAVEL);
        // perimeter wall with iron railings
        for (int x = -8; x <= 52; x++)
            for (int z : new int[]{-8, 52}) fenceAt(x, z);
        for (int z = -8; z <= 52; z++)
            for (int x : new int[]{-8, 52}) fenceAt(x, z);
        // the gate
        for (int x = 21; x <= 23; x++) {
            air(x, 1, 52, x, 4, 52);
            set(x, 1, 52, Blocks.DARK_OAK_FENCE_GATE.defaultBlockState().setValue(FenceGateBlock.FACING, Direction.SOUTH).setValue(FenceGateBlock.OPEN, true));
            tag(x, 1, 52, "door:gate");
        }
        DOORS.put("gate", new BlockPos(22, 1, 52));
        for (int x : new int[]{20, 24}) {
            fill(x, 1, 52, x, 4, 52, Blocks.STONE_BRICKS);
            set(x, 5, 52, Blocks.STONE_BRICK_SLAB);
        }
        set(20, 5, 52, Blocks.STONE_BRICK_WALL);
        set(20, 6, 52, Blocks.SOUL_LANTERN);
        set(24, 5, 52, Blocks.STONE_BRICK_WALL);
        set(24, 6, 52, Blocks.SOUL_LANTERN);
        for (int[] lp : new int[][]{{-8, 22}, {52, 22}, {-8, 46}, {52, 46}, {8, 52}, {38, 52}}) {
            set(lp[0], 4, lp[1], Blocks.STONE_BRICK_WALL);
            set(lp[0], 5, lp[1], Blocks.SOUL_LANTERN);
        }
        // hedge that hides the world outside, and a barrier so nobody wanders off
        for (int x = -12; x <= 56; x++)
            for (int z = -12; z <= 62; z++) {
                boolean ring = x <= -10 || x >= 54 || z <= -10 || z >= 60;
                if (ring) {
                    int h = 5 + hash(x, 1, z) % 4;
                    for (int yy = 1; yy <= h; yy++)
                        set(x, yy, z, hash(x, yy, z) % 5 == 0 ? Blocks.DARK_OAK_LOG.defaultBlockState() : Blocks.DARK_OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
                }
            }
        for (int x = -9; x <= 53; x++) for (int yy = 1; yy <= 4; yy++) set(x, yy, 59, Blocks.BARRIER);
        for (int z = 53; z <= 59; z++) for (int yy = 1; yy <= 4; yy++) {
            set(-9, yy, z, Blocks.BARRIER);
            set(53, yy, z, Blocks.BARRIER);
        }
        for (int x = -9; x <= 53; x++) for (int yy = 1; yy <= 4; yy++) if (x < 20 || x > 24) set(x, yy, 53, Blocks.BARRIER);
        // dead trees
        for (int[] t : new int[][]{{6, 40, 7}, {38, 44, 9}, {-3, 20, 8}, {49, 12, 7}, {31, 37, 6}, {12, 48, 8}, {-4, 46, 6}, {47, 36, 9}, {3, -5, 7}, {40, -5, 8}, {45, 49, 6}})
            tree(t[0], t[1], t[2]);
        for (int i = 0; i < 70; i++) {
            int x = -7 + rnd.nextInt(58), z = -7 + rnd.nextInt(58);
            if (x >= -2 && x <= 46 && z >= -2 && z <= 34) continue;
            if (x >= 20 && x <= 24) continue;
            if (lvl.getBlockState(o.offset(x, 1, z)).isAir()) set(x, 1, z, rnd.nextInt(3) == 0 ? Blocks.DEAD_BUSH : Blocks.FERN);
        }
        // the old well - the far end of the cellar tunnel
        for (int x = 9; x <= 11; x++)
            for (int z = 42; z <= 44; z++) {
                if (x == 10 && z == 43) continue;
                set(x, 1, z, stone(x, 1, z));
            }
        air(10, 1, 43, 10, 2, 43);
        set(10, 1, 43, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH));
        set(10, 1, 42, Blocks.AIR.defaultBlockState());
        set(10, 2, 44, Blocks.STONE_BRICK_WALL);
        for (int x : new int[]{9, 11}) for (int z : new int[]{42, 44}) fill(x, 2, z, x, 3, z, Blocks.DARK_OAK_FENCE);
        fill(9, 4, 42, 11, 4, 44, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
        // graves for a family that was never buried
        for (int i = 0; i < 4; i++) {
            int x = 34 + i * 3;
            set(x, 1, 46, Blocks.MOSSY_STONE_BRICK_WALL);
            set(x, 0, 47, Blocks.COARSE_DIRT);
            set(x, 0, 48, Blocks.COARSE_DIRT);
        }
    }

    private void fenceAt(int x, int z) {
        if (z == 52 && x >= 20 && x <= 24) return;
        set(x, 1, z, stone(x, 1, z));
        if ((x + z) % 6 == 0) {
            set(x, 2, z, Blocks.STONE_BRICKS);
            set(x, 3, z, Blocks.STONE_BRICKS);
            set(x, 4, z, Blocks.STONE_BRICK_SLAB);
        } else {
            set(x, 2, z, Blocks.IRON_BARS);
            set(x, 3, z, Blocks.IRON_BARS);
        }
    }

    private void tree(int x, int z, int h) {
        fill(x, 1, z, x, h, z, log(Blocks.DARK_OAK_LOG, Direction.Axis.Y));
        Random r = new Random(x * 31L + z);
        for (int i = 0; i < 5; i++) {
            int by = h - r.nextInt(4);
            int dx = r.nextInt(3) - 1, dz = r.nextInt(3) - 1;
            if (dx == 0 && dz == 0) dx = 1;
            int len = 2 + r.nextInt(3);
            for (int k = 1; k <= len; k++)
                set(x + dx * k, by + k / 2, z + dz * k, log(Blocks.DARK_OAK_LOG, dx != 0 ? Direction.Axis.X : Direction.Axis.Z));
        }
    }
}
