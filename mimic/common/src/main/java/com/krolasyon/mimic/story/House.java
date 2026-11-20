package com.krolasyon.mimic.story;

import com.krolasyon.mimic.block.FacingBlock;
import com.krolasyon.mimic.block.ModeBlock;
import com.krolasyon.mimic.block.OpenableBlock;
import com.krolasyon.mimic.block.TallBlock;
import com.krolasyon.mimic.block.VariantBlock;
import com.krolasyon.mimic.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Harrow Farm. Local coordinates: x east, z south, y up; the ground surface is y=0, people walk at y=1.
 * The road comes in from the south (z ~ 104); the farmhouse is west, the barn and silo east, the shed north, the corn south-east.
 * Running the builder with a null level only fills the registries (hotspots, rooms, lamps, doors, hides).
 */
public final class House {
    /** floors: house ground y0 (walk 1), house upper floor block y5 (walk 6), cellar floor y-5 (walk -4), hayloft floor y6 (walk 7) */
    public static final int G = 0, F = 5, B = -5, L = 6;

    public static final Map<BlockPos, String> HOT = new HashMap<>();
    public static final Map<String, int[]> ROOMS = new LinkedHashMap<>();
    public static final Map<String, BlockPos> DOORS = new HashMap<>();
    public static final Map<String, List<BlockPos>> TAGGED = new HashMap<>();
    public static final List<Lamp> LAMPS = new ArrayList<>();
    public static final List<int[]> GUESTS = new ArrayList<>();
    public static final List<Hide> HIDES = new ArrayList<>();
    /** cells (local, y = walking level) that count as cover in the corn */
    public static final java.util.Set<Long> CORN = new java.util.HashSet<>();
    private static boolean indexed = false;
    private static Map<String, Block> BLK = null;

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
    private final Random rnd = new Random(1987);

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

    public static boolean inCorn(double x, double y, double z) {
        ensureIndexed();
        return CORN.contains(BlockPos.asLong((int) Math.floor(x), (int) Math.floor(y + 0.2), (int) Math.floor(z)));
    }

    public static BlockPos first(String tag) {
        ensureIndexed();
        List<BlockPos> l = TAGGED.get(tag);
        return l == null || l.isEmpty() ? null : l.get(0);
    }

    // ------------------------------------------------------------------ primitives
    static Block blk(String name) {
        if (BLK == null) {
            BLK = new HashMap<>();
            for (RegistryObject<Block> r : ModBlocks.ALL) BLK.put(r.getId().getPath(), r.get());
        }
        Block b = BLK.get(name);
        if (b == null) throw new IllegalArgumentException("no block " + name);
        return b;
    }

    private BlockState st(String name) { return lvl == null ? null : blk(name).defaultBlockState(); }

    private void set(int x, int y, int z, BlockState s) {
        if (lvl != null && s != null) lvl.setBlock(o.offset(x, y, z), s, 2 | 16);
    }

    private void set(int x, int y, int z, Block b) { if (lvl != null) set(x, y, z, b.defaultBlockState()); }

    private void set(int x, int y, int z, String name) { if (lvl != null) set(x, y, z, st(name)); }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
        if (lvl == null) return;
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    lvl.setBlock(o.offset(x, y, z), s, 2 | 16);
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block b) { if (lvl != null) fill(x1, y1, z1, x2, y2, z2, b.defaultBlockState()); }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, String name) { if (lvl != null) fill(x1, y1, z1, x2, y2, z2, st(name)); }

    private void air(int x1, int y1, int z1, int x2, int y2, int z2) { fill(x1, y1, z1, x2, y2, z2, Blocks.AIR); }

    private void tag(int x, int y, int z, String t) {
        BlockPos p = new BlockPos(x, y, z);
        HOT.put(p, t);
        TAGGED.computeIfAbsent(t, k -> new ArrayList<>()).add(p);
    }

    /** mark a position without making it interactive */
    private void mark(int x, int y, int z, String t) {
        TAGGED.computeIfAbsent(t, k -> new ArrayList<>()).add(new BlockPos(x, y, z));
    }

    private int hash(int x, int y, int z) {
        int h = x * 73428767 ^ y * 912931 ^ z * 42317861;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;
        return h & 0x7fffffff;
    }

    private BlockState face(String name, Direction d) { return lvl == null ? null : blk(name).defaultBlockState().setValue(FacingBlock.FACING, d); }

    private void prop(int x, int y, int z, String name, Direction d) { set(x, y, z, face(name, d)); }

    private void prop(int x, int y, int z, String name, Direction d, String hot) {
        prop(x, y, z, name, d);
        tag(x, y, z, hot);
    }

    private void mode(int x, int y, int z, String name, Direction d, int m) {
        if (lvl != null) set(x, y, z, face(name, d).setValue(ModeBlock.MODE, m));
    }

    private void variant(int x, int y, int z, String name, Direction d, int v) {
        if (lvl != null) set(x, y, z, face(name, d).setValue(VariantBlock.VARIANT, v));
    }

    private void tall(int x, int y, int z, String name, Direction d) {
        if (lvl == null) return;
        set(x, y, z, face(name, d).setValue(TallBlock.HALF, DoubleBlockHalf.LOWER));
        set(x, y + 1, z, face(name, d).setValue(TallBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private void lamp(int x, int y, int z, String name, Direction d, String room, int quality) {
        mode(x, y, z, name, d, quality == 0 ? 1 : quality == 1 ? 2 : 0);
        LAMPS.add(new Lamp(new BlockPos(x, y, z), room, quality));
    }

    /** a hiding place: wardrobe (two tall) or hay mound; the player looks out through its front (facing) */
    private void hide(String id, int x, int y, int z, String name, Direction d) {
        tall(x, y, z, name, d);
        HIDES.add(new Hide(id, new BlockPos(x, y, z), d));
        tag(x, y, z, "wardrobe:" + id);
        tag(x, y + 1, z, "wardrobe:" + id);
    }

    private void drawer(int x, int y, int z, String name, Direction d, String content) {
        prop(x, y, z, name, d);
        tag(x, y, z, "drawer:" + content);
    }

    private void door(int x, int y, int z, String name, Direction facing, boolean rightHinge, String id) {
        if (lvl != null) {
            BlockState b = blk(name).defaultBlockState().setValue(DoorBlock.FACING, facing)
                    .setValue(DoorBlock.HINGE, rightHinge ? DoorHingeSide.RIGHT : DoorHingeSide.LEFT).setValue(DoorBlock.OPEN, false);
            set(x, y, z, b.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            set(x, y + 1, z, b.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }
        DOORS.put(id, new BlockPos(x, y, z));
        tag(x, y, z, "door:" + id);
        tag(x, y + 1, z, "door:" + id);
    }

    private void room(String name, int x1, int y, int z1, int x2, int z2) { room(name, x1, y, z1, x2, z2, 3); }

    private void room(String name, int x1, int y, int z1, int x2, int z2, int h) {
        ROOMS.put(name, new int[]{x1, y, z1, x2, y + h, z2});
    }

    private BlockState stairs(String name, Direction d, boolean top) {
        if (lvl == null) return null;
        return blk(name).defaultBlockState().setValue(StairBlock.FACING, d).setValue(StairBlock.HALF, top ? Half.TOP : Half.BOTTOM);
    }

    private BlockState slab(String name, boolean top) {
        if (lvl == null) return null;
        return blk(name).defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM);
    }

    private void ladder(int x, int y1, int y2, int z, Direction d) {
        if (lvl == null) return;
        for (int y = y1; y <= y2; y++) set(x, y, z, blk("farm_ladder").defaultBlockState().setValue(LadderBlock.FACING, d));
    }

    private void fence(int x1, int z1, int x2, int z2, int y) {
        if (lvl == null) return;
        BlockState f = blk("barbed_fence").defaultBlockState();
        boolean alongX = z1 == z2;
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                BlockState s = f.setValue(FenceBlock.NORTH, !alongX).setValue(FenceBlock.SOUTH, !alongX)
                        .setValue(FenceBlock.EAST, alongX).setValue(FenceBlock.WEST, alongX);
                set(x, y, z, s);
            }
    }

    // ------------------------------------------------------------------ the build
    private void run() {
        HOT.clear();
        ROOMS.clear();
        DOORS.clear();
        TAGGED.clear();
        LAMPS.clear();
        HIDES.clear();
        GUESTS.clear();
        CORN.clear();
        // rooms first (lookups go in insertion order: the specific ones before the big outdoor zones)
        rooms();
        ground();
        farmhouse();
        barn();
        silo();
        shed();
        yard();
        fields();
        indexed = true;
    }

    private void rooms() {
        room("CELLAR", 28, B + 1, 52, 34, 59);
        room("H_HALL", 24, G + 1, 52, 26, 69);
        room("H_LIVING", 16, G + 1, 61, 22, 68);
        room("H_DINING", 16, G + 1, 52, 22, 59);
        room("H_KITCHEN", 28, G + 1, 51, 34, 59);
        room("H_STUDY", 28, G + 1, 61, 34, 68);
        room("U_LANDING", 24, F + 1, 52, 26, 68);
        room("U_LILY", 16, F + 1, 61, 22, 68);
        room("U_BATH", 16, F + 1, 52, 22, 59);
        room("U_MASTER", 28, F + 1, 61, 34, 68);
        room("U_STORAGE", 28, F + 1, 52, 34, 59);
        room("PORCH", 17, G + 1, 70, 33, 73);
        room("LOFT", 60, L + 1, 18, 66, 44, 4);
        room("STALLS", 60, G + 1, 18, 66, 44, 5);
        room("WORKSHOP", 74, G + 1, 33, 80, 44, 5);
        room("SLAUGHTER", 74, G + 1, 18, 80, 31, 5);
        room("BARN", 60, G + 1, 17, 80, 46, 9);
        room("SILO", 85, G + 1, 28, 91, 34, 6);
        room("SHED", 39, G + 1, 17, 49, 27, 4);
        room("ROAD", 30, G, 99, 70, 112, 6);
        room("FIELD", 56, G, 52, 98, 96, 6);
        room("YARD", -4, G - 1, -4, 104, 104, 24);
    }

    // ------------------------------------------------------------------ terrain
    private void ground() {
        if (lvl == null) {
            return;
        }
        for (int x = -6; x <= 106; x++)
            for (int z = -6; z <= 112; z++) {
                int h = hash(x, 0, z);
                fill(x, -9, z, x, -2, z, Blocks.STONE);
                set(x, -1, z, Blocks.DIRT);
                int k = h % 100;
                BlockState top = k < 50 ? Blocks.COARSE_DIRT.defaultBlockState() : k < 68 ? Blocks.PODZOL.defaultBlockState()
                        : k < 84 ? blk("packed_dirt").defaultBlockState() : Blocks.DIRT.defaultBlockState();
                set(x, 0, z, top);
                for (int y = 1; y <= 34; y++) {
                    BlockPos p = o.offset(x, y, z);
                    if (!lvl.getBlockState(p).isAir()) lvl.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
                }
                // dry grass everywhere that is not path or building - trimmed later by the builders
                if ((h >> 8) % 100 < 14) prop(x, 1, z, "dry_grass", Direction.from2DDataValue((h >> 4) & 3));
            }
        // the road and the yard paths
        path(48, 74, 52, 112);
        path(24, 74, 52, 76);
        path(24, 71, 26, 76);
        path(44, 40, 52, 74);
        path(38, 40, 72, 48);
        path(68, 46, 72, 50);
        path(42, 28, 46, 40);
        path(76, 28, 84, 34);
        path(66, 8, 74, 16);
        path(46, 8, 66, 12);
    }

    private void path(int x1, int z1, int x2, int z2) {
        if (lvl == null) return;
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                int h = hash(x, 7, z);
                boolean edge = x == x1 || x == x2 || z == z1 || z == z2;
                set(x, 0, z, edge && h % 3 == 0 ? Blocks.COARSE_DIRT.defaultBlockState() : h % 7 == 0 ? blk("mud").defaultBlockState() : Blocks.DIRT_PATH.defaultBlockState());
                set(x, 1, z, Blocks.AIR);
            }
    }

    // ------------------------------------------------------------------ the farmhouse
    /** x 14..36, z 50..70; two board thick outer walls: siding outside, panelling inside */
    private void farmhouse() {
        int x1 = 14, x2 = 36, z1 = 50, z2 = 70;
        if (lvl != null) {
            air(x1 - 1, 1, z1 - 1, x2 + 1, 24, z2 + 4);
            fill(x1, B - 1, z1, x2, 0, z2, "stone_foundation");
            // floors
            fill(x1 + 1, G, z1 + 1, x2 - 1, G, z2 - 1, "floorboards");
            fill(x1 + 1, F, z1 + 1, x2 - 1, F, z2 - 1, "floorboards_dark");
            fill(x1, 10, z1, x2, 10, z2, "floorboards_dark");
            // outer shell
            for (int y = 1; y <= 9; y++) {
                if (y == F) continue;
                for (int x = x1; x <= x2; x++) {
                    set(x, y, z1, "siding");
                    set(x, y, z2, "siding");
                    set(x, y, z1 + 1, y == 1 || y == F + 1 ? "wainscot" : "wallpaper");
                    set(x, y, z2 - 1, y == 1 || y == F + 1 ? "wainscot" : "wallpaper");
                }
                for (int z = z1; z <= z2; z++) {
                    set(x1, y, z, "siding");
                    set(x2, y, z, "siding");
                    if (z > z1 && z < z2) {
                        set(x1 + 1, y, z, y == 1 || y == F + 1 ? "wainscot" : "wallpaper");
                        set(x2 - 1, y, z, y == 1 || y == F + 1 ? "wainscot" : "wallpaper");
                    }
                }
            }
            fill(x1, F, z1, x2, F, z1 + 1, "siding_dark");
            fill(x1, F, z2 - 1, x2, F, z2, "siding_dark");
            fill(x1, F, z1, x1 + 1, F, z2, "siding_dark");
            fill(x2 - 1, F, z1, x2, F, z2, "siding_dark");
            // partitions (both floors): hall x 24..26 between walls at x=23 and x=27; north/south split at z=60
            for (int base : new int[]{G, F}) {
                for (int y = base + 1; y <= base + 4; y++) {
                    String w = y == base + 1 ? "wainscot" : base == F && hash(0, y, 1) % 2 == 0 ? "wallpaper_peeled" : "wallpaper";
                    fill(23, y, z1 + 2, 23, y, z2 - 2, st(w));
                    fill(27, y, z1 + 2, 27, y, z2 - 2, st(w));
                    fill(x1 + 2, y, 60, 22, y, 60, st(w));
                    fill(28, y, 60, x2 - 2, y, 60, st(w));
                }
            }
            // roof: gable, ridge along x at z=60
            for (int k = 0; k <= 10; k++) {
                int y = 10 + k;
                for (int x = x1 - 1; x <= x2 + 1; x++) {
                    if (71 - k > 60) set(x, y, 71 - k, stairs("shingle_stairs", Direction.NORTH, false));
                    if (49 + k < 60) set(x, y, 49 + k, stairs("shingle_stairs", Direction.SOUTH, false));
                }
                // gable walls under the slope
                for (int z = 50 + k; z <= 70 - k; z++) {
                    if (y > 10) {
                        set(x1, y, z, "siding");
                        set(x2, y, z, "siding");
                    }
                }
            }
            for (int x = x1 - 1; x <= x2 + 1; x++) set(x, 20, 60, slab("shingle_slab", false));
            // brick chimney through the west roof
            fill(17, 1, 62, 17, 23, 62, "brick");
            fill(17, 1, 62, 17, 1, 62, "brick");
        }
        // ---- ground floor
        // front door through the two board wall, a little vestibule
        door(25, G + 1, 70, "farm_door", Direction.NORTH, false, "front");
        air(25, G + 1, 69, 25, G + 2, 69);
        door(31, G + 1, 50, "farm_door", Direction.SOUTH, false, "back");
        air(31, G + 1, 51, 31, G + 2, 51);
        // doorways off the hall
        air(23, G + 1, 64, 23, G + 2, 65);                              // living (open arch)
        door(23, G + 1, 55, "farm_door", Direction.WEST, false, "dining");
        door(27, G + 1, 55, "farm_door", Direction.EAST, true, "kitchen");
        door(27, G + 1, 68, "farm_door", Direction.EAST, false, "study");
        // staircase: x 25..26, rising north from z=66 (y1) to z=62 (y5)
        if (lvl != null) {
            for (int i = 0; i < 5; i++) {
                int z = 66 - i, y = G + 1 + i;
                for (int x = 25; x <= 26; x++) {
                    set(x, y, z, stairs("staircase", Direction.NORTH, false));
                    for (int yy = G + 1; yy < y; yy++) set(x, yy, z, "floorboards_dark");
                }
            }
            // the hole in the upper floor over the stairs, with a banister along it
            air(25, F, 63, 26, F, 66);
            for (int z = 63; z <= 66; z++) set(24, F + 1, z, face("banister", Direction.WEST));
            set(25, F + 1, 67, face("banister", Direction.SOUTH));
            set(26, F + 1, 67, face("banister", Direction.SOUTH));
        }
        // HALL
        for (int z = 52; z <= 68; z++) if (z < 62 || z > 67) prop(24, G + 1, z, "runner", Direction.NORTH);
        for (int z = 52; z <= 61; z++) prop(25, G + 1, z, "runner", Direction.NORTH);
        lamp(25, G + 4, 57, "chandelier", Direction.NORTH, "H_HALL", 1);
        mode(24, G + 2, 58, "phone", Direction.EAST, 0);
        tag(24, G + 2, 58, "phone");
        tall(26, G + 1, 53, "grandfather_clock", Direction.WEST);
        tag(26, G + 1, 53, "clock");
        tag(26, G + 2, 53, "clock");
        variant(24, G + 3, 54, "painting", Direction.EAST, 1);
        tag(24, G + 3, 54, "look:portrait");
        lamp(26, G + 3, 68, "sconce", Direction.WEST, "H_HALL", 1);
        prop(24, G + 1, 66, "chair", Direction.EAST);
        prop(26, G + 1, 52, "handprint", Direction.SOUTH);
        drawer(24, G + 1, 52, "nightstand", Direction.EAST, "battery");
        // LIVING x16..22 z61..68
        if (lvl != null) for (int x = 18; x <= 20; x++) for (int z = 63; z <= 65; z++) prop(x, G + 1, z, "rug", Direction.NORTH);
        mode(17, G + 1, 61, "wood_stove", Direction.SOUTH, 1);
        tag(17, G + 1, 61, "stove");
        prop(17, G + 2, 61, "stove_pipe", Direction.SOUTH);
        prop(17, G + 3, 61, "stove_pipe", Direction.SOUTH);
        prop(17, G + 4, 61, "stove_pipe", Direction.SOUTH);
        prop(18, G + 1, 63, "armchair", Direction.WEST);
        prop(21, G + 1, 63, "armchair", Direction.EAST);
        prop(19, G + 1, 66, "table", Direction.NORTH);
        lamp(19, G + 2, 66, "oil_lamp", Direction.NORTH, "H_LIVING", 1);
        prop(20, G + 1, 67, "chair_fallen", Direction.NORTH);
        prop(16, G + 1, 66, "bench", Direction.EAST);
        prop(16, G + 1, 67, "bench", Direction.EAST);
        prop(22, G + 1, 68, "bookcase", Direction.NORTH, "look:books");
        prop(21, G + 1, 68, "bookcase", Direction.NORTH);
        drawer(18, G + 1, 68, "cabinet", Direction.NORTH, "note:clipping");
        variant(16, G + 3, 62, "painting", Direction.EAST, 0);
        window(14, G + 2, 64, Direction.EAST, false);
        window(14, G + 2, 66, Direction.EAST, false);
        window(19, G + 2, 70, Direction.NORTH, false);
        prop(18, G + 4, 61, "cobweb", Direction.NORTH);
        prop(20, G + 1, 62, "blood_pool", Direction.NORTH);
        // DINING x16..22 z52..59
        for (int x = 18; x <= 20; x++) {
            prop(x, G + 1, 55, "table_cloth", Direction.NORTH);
            prop(x, G + 1, 56, "table_cloth", Direction.NORTH);
            prop(x, G + 1, 54, "chair", Direction.SOUTH);
            prop(x, G + 1, 57, x == 19 ? "chair_fallen" : "chair", Direction.NORTH);
        }
        prop(17, G + 1, 55, "chair", Direction.EAST);
        prop(21, G + 1, 56, "chair", Direction.WEST);
        prop(19, G + 2, 55, "candles", Direction.NORTH);
        lamp(19, G + 4, 56, "chandelier", Direction.NORTH, "H_DINING", 2);
        drawer(16, G + 1, 53, "cabinet", Direction.EAST, "item:lighter");
        drawer(16, G + 1, 58, "dresser", Direction.EAST, "empty");
        variant(22, G + 3, 56, "painting", Direction.WEST, 2);
        tag(22, G + 3, 56, "look:family");
        window(14, G + 2, 55, Direction.EAST, false);
        prop(20, G + 1, 58, "blood_pool", Direction.NORTH);
        prop(16, G + 4, 52, "cobweb", Direction.EAST);
        // KITCHEN x28..34 z51..59
        prop(29, G + 1, 52, "cook_stove", Direction.SOUTH, "look:stove");
        prop(30, G + 1, 52, "counter", Direction.SOUTH);
        prop(32, G + 1, 52, "counter", Direction.SOUTH);
        prop(33, G + 1, 52, "sink", Direction.SOUTH, "sink");
        prop(34, G + 1, 53, "counter", Direction.WEST);
        prop(34, G + 1, 54, "counter", Direction.WEST);
        prop(34, G + 3, 55, "shelf_jars", Direction.WEST);
        prop(34, G + 3, 56, "shelf_jars", Direction.WEST);
        hide("pantry", 34, G + 1, 57, "wardrobe", Direction.WEST);
        prop(30, G + 1, 56, "table", Direction.NORTH);
        prop(31, G + 1, 56, "table", Direction.NORTH);
        prop(29, G + 1, 56, "chair", Direction.EAST);
        prop(32, G + 1, 57, "chair", Direction.NORTH);
        prop(30, G + 2, 56, "walkie", Direction.SOUTH, "pickup:walkie");
        prop(31, G + 2, 56, "note_paper", Direction.SOUTH, "note:kitchen");
        lamp(34, G + 2, 54, "oil_lamp", Direction.NORTH, "H_KITCHEN", 0);
        drawer(32, G + 1, 52, "counter", Direction.SOUTH, "battery");
        if (lvl != null) {
            set(33, G, 59, blk("hatch").defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.NORTH).setValue(TrapDoorBlock.HALF, Half.TOP).setValue(TrapDoorBlock.OPEN, false));
        }
        DOORS.put("cellar", new BlockPos(33, G, 59));
        tag(33, G, 59, "door:cellar");
        prop(28, G + 1, 58, "bucket", Direction.NORTH, "kick");
        prop(30, G + 1, 58, "blood_pool", Direction.NORTH);
        window(36, G + 2, 54, Direction.WEST, false);
        // STUDY x28..34 z61..68
        drawer(31, G + 1, 66, "desk", Direction.NORTH, "note:journal1");
        prop(31, G + 1, 65, "chair", Direction.SOUTH);
        lamp(31, G + 2, 66, "oil_lamp", Direction.NORTH, "H_STUDY", 2);
        prop(34, G + 1, 62, "bookcase", Direction.WEST);
        prop(34, G + 1, 63, "bookcase", Direction.WEST);
        mode(28, G + 1, 61, "radio_set", Direction.SOUTH, 0);
        tag(28, G + 1, 61, "hamradio");
        prop(28, G + 2, 61, "stool", Direction.SOUTH);
        drawer(33, G + 1, 68, "trunk", Direction.NORTH, "note:sheriff");
        drawer(29, G + 1, 68, "cabinet", Direction.NORTH, "note:ledger");
        hide("study_wd", 34, G + 1, 66, "wardrobe", Direction.WEST);
        prop(28, G + 3, 64, "antlers", Direction.EAST, "look:antlers");
        window(36, G + 2, 64, Direction.WEST, false);
        window(31, G + 2, 70, Direction.NORTH, false);
        prop(30, G + 1, 63, "rug", Direction.NORTH);
        prop(31, G + 1, 63, "rug", Direction.NORTH);
        // ---- upper floor
        door(23, F + 1, 68, "farm_door", Direction.WEST, false, "lily");
        door(23, F + 1, 55, "farm_door", Direction.WEST, true, "bath");
        door(27, F + 1, 68, "farm_door", Direction.EAST, true, "master");
        door(27, F + 1, 55, "farm_door", Direction.EAST, false, "storage");
        for (int z = 52; z <= 61; z++) prop(25, F + 1, z, "runner", Direction.NORTH);
        lamp(25, F + 4, 56, "bulb", Direction.NORTH, "U_LANDING", 2);
        prop(26, F + 1, 52, "chair", Direction.WEST);
        prop(24, F + 3, 58, "handprint", Direction.EAST);
        prop(26, F + 2, 59, "handprint", Direction.WEST);
        prop(25, F + 1, 54, "blood_pool", Direction.NORTH);
        prop(24, F + 4, 52, "cobweb", Direction.SOUTH);
        // LILY x16..22 z61..68
        prop(17, F + 1, 67, "bed_head", Direction.NORTH);
        prop(17, F + 1, 66, "bed_foot", Direction.NORTH);
        drawer(18, F + 1, 68, "nightstand", Direction.NORTH, "note:diary");
        lamp(18, F + 2, 68, "lantern", Direction.NORTH, "U_LILY", 0);
        hide("lily_wd", 22, F + 1, 62, "wardrobe", Direction.WEST);
        drawer(16, F + 1, 62, "dresser", Direction.EAST, "note:drawing");
        prop(20, F + 1, 65, "rug", Direction.NORTH);
        prop(21, F + 1, 66, "chair", Direction.WEST);
        prop(16, F + 3, 64, "handprint", Direction.EAST);
        variant(22, F + 3, 66, "painting", Direction.WEST, 3);
        litWindow(14, F + 2, 65);
        litWindow(14, F + 2, 66);
        window(19, F + 2, 70, Direction.NORTH, false);
        // BATH x16..22 z52..59
        prop(17, F + 1, 52, "bathtub", Direction.EAST, "look:tub");
        prop(18, F + 1, 52, "bathtub", Direction.WEST);
        prop(21, F + 1, 52, "toilet", Direction.SOUTH);
        prop(16, F + 1, 57, "sink", Direction.EAST);
        prop(16, F + 3, 57, "mirror", Direction.EAST, "mirror");
        prop(19, F + 1, 55, "blood_pool", Direction.NORTH);
        prop(18, F + 1, 54, "blood_pool", Direction.NORTH);
        prop(22, F + 4, 52, "cobweb", Direction.SOUTH);
        window(14, F + 2, 55, Direction.EAST, false);
        // MASTER x28..34 z61..68
        prop(32, F + 1, 67, "bed_head", Direction.NORTH);
        prop(32, F + 1, 66, "bed_foot", Direction.NORTH);
        prop(33, F + 1, 67, "bed_head", Direction.NORTH);
        prop(33, F + 1, 66, "bed_foot", Direction.NORTH);
        drawer(34, F + 1, 68, "nightstand", Direction.NORTH, "battery");
        hide("master_wd", 28, F + 1, 62, "wardrobe", Direction.EAST);
        drawer(34, F + 1, 62, "dresser", Direction.WEST, "note:funeral");
        prop(34, F + 3, 62, "mirror", Direction.WEST);
        prop(30, F + 1, 65, "chair_fallen", Direction.EAST);
        variant(28, F + 3, 66, "painting", Direction.EAST, 2);
        window(36, F + 2, 64, Direction.WEST, false);
        window(31, F + 2, 70, Direction.NORTH, false);
        prop(31, F + 1, 64, "blood_pool", Direction.NORTH);
        // STORAGE x28..34 z52..59 - nailed shut: the boards say "Do not open"
        drawer(29, F + 1, 52, "crate", Direction.SOUTH, "battery");
        drawer(30, F + 1, 52, "crate", Direction.SOUTH, "empty");
        prop(29, F + 2, 52, "crate", Direction.SOUTH);
        drawer(33, F + 1, 53, "trunk", Direction.WEST, "note:mother");
        prop(34, F + 1, 58, "sacks", Direction.WEST);
        prop(31, F + 1, 58, "barrel", Direction.NORTH);
        if (lvl != null) set(36, F + 2, 55, "boarded_window");
        // ROOT CELLAR under the kitchen x28..34 z52..59, floor y-5
        if (lvl != null) {
            air(28, B + 1, 52, 34, -1, 59);
            fill(28, B, 52, 34, B, 59, "cellar_stone");
            for (int y = B + 1; y <= -1; y++) {
                for (int x = 27; x <= 35; x++) {
                    set(x, y, 51, "cellar_stone");
                    set(x, y, 60, "cellar_stone");
                }
                for (int z = 51; z <= 60; z++) {
                    set(27, y, z, "cellar_stone");
                    set(35, y, z, "cellar_stone");
                }
            }
            ladder(33, B + 1, -1, 59, Direction.NORTH);
        }
        prop(28, B + 1, 52, "shelf_jars", Direction.EAST);
        prop(28, B + 2, 52, "shelf_jars", Direction.EAST);
        prop(29, B + 1, 52, "shelf_jars", Direction.SOUTH);
        prop(29, B + 2, 52, "shelf_jars", Direction.SOUTH);
        drawer(31, B + 1, 52, "barrel", Direction.SOUTH, "battery");
        drawer(32, B + 1, 52, "crate", Direction.SOUTH, "note:cellar");
        prop(34, B + 1, 52, "bone_pile", Direction.NORTH);
        prop(33, B + 1, 53, "skull", Direction.WEST);
        prop(30, B + 1, 57, "blood_pool", Direction.NORTH);
        lamp(31, -1, 55, "bulb", Direction.NORTH, "CELLAR", 2);
        // PORCH
        if (lvl != null) {
            fill(17, G, 71, 33, G, 73, "floorboards");
            for (int x : new int[]{17, 25, 33}) {
                fill(x, G + 1, 73, x, G + 4, 73, face("post", Direction.NORTH));
            }
            for (int x = 16; x <= 34; x++) for (int z = 71; z <= 74; z++) set(x, G + 5, z, slab("shingle_slab", false));
        }
        prop(19, G + 1, 72, "chair", Direction.SOUTH);
        prop(21, G + 1, 71, "bench", Direction.SOUTH);
        prop(31, G + 1, 72, "milk_can", Direction.NORTH);
        prop(32, G + 1, 71, "bucket", Direction.NORTH);
        lamp(24, G + 4, 72, "lantern_hang", Direction.NORTH, "PORCH", 1);
        prop(26, G + 2, 73, "birdbox", Direction.SOUTH, "birdbox");
    }

    private void window(int x, int y, int z, Direction look, boolean lit) {
        if (lvl == null) return;
        set(x, y, z, face(lit ? "window_lit" : "window_pane", look));
        set(x, y + 1, z, face(lit ? "window_lit" : "window_pane", look));
        // the inner board of the two board wall opens up, with a curtain
        int ix = x + look.getStepX(), iz = z + look.getStepZ();
        set(ix, y, iz, face("curtain", look.getOpposite()));
        set(ix, y + 1, iz, face("curtain", look.getOpposite()));
    }

    private void litWindow(int x, int y, int z) {
        if (lvl == null) return;
        set(x, y, z, face("window_lit", Direction.EAST));
        set(x, y + 1, z, face("window_lit", Direction.EAST));
        set(x + 1, y, z, Blocks.AIR);
        set(x + 1, y + 1, z, Blocks.AIR);
    }

    // ------------------------------------------------------------------ the red barn
    /** x 58..82, z 16..46, two board walls, 45 degree tin roof with the ridge along z at x=70 */
    private void barn() {
        int x1 = 58, x2 = 82, z1 = 16, z2 = 46;
        if (lvl != null) {
            air(x1 - 1, 1, z1 - 1, x2 + 1, 28, z2 + 1);
            fill(x1, G, z1, x2, G, z2, "barn_floor");
            fill(66, G, 18, 74, G, 44, "packed_dirt");
            for (int y = 1; y <= 10; y++) {
                for (int x = x1; x <= x2; x++) {
                    set(x, y, z1, "barn_wall");
                    set(x, y, z2, "barn_wall");
                    set(x, y, z1 + 1, "barn_wall_dark");
                    set(x, y, z2 - 1, "barn_wall_dark");
                }
                for (int z = z1; z <= z2; z++) {
                    set(x1, y, z, "barn_wall");
                    set(x2, y, z, "barn_wall");
                    if (z > z1 && z < z2) {
                        set(x1 + 1, y, z, "barn_wall_dark");
                        set(x2 - 1, y, z, "barn_wall_dark");
                    }
                }
            }
            // a white trim band, like the photograph
            for (int x = x1; x <= x2; x++) {
                set(x, 6, z1, "siding");
                set(x, 6, z2, "siding");
            }
            // roof
            for (int k = 0; k <= 12; k++) {
                int y = 11 + k;
                for (int z = z1 - 1; z <= z2 + 1; z++) {
                    if (57 + k < 70) set(57 + k, y, z, stairs("tin_stairs", Direction.EAST, false));
                    if (83 - k > 70) set(83 - k, y, z, stairs("tin_stairs", Direction.WEST, false));
                }
                for (int x = 58 + k; x <= 82 - k; x++) {
                    if (y > 10) {
                        set(x, y, z1, "barn_wall");
                        set(x, y, z2, "barn_wall");
                        set(x, y, z1 + 1, "barn_wall_dark");
                        set(x, y, z2 - 1, "barn_wall_dark");
                    }
                }
            }
            for (int z = z1 - 1; z <= z2 + 1; z++) set(70, 23, z, slab("tin_slab", false));
            // two cupolas on the ridge
            for (int cz : new int[]{24, 38}) {
                fill(69, 23, cz - 1, 71, 25, cz + 1, "barn_wall");
                fill(69, 24, cz, 71, 24, cz, Blocks.AIR);
                for (int x = 68; x <= 72; x++) for (int z = cz - 2; z <= cz + 2; z++) set(x, 26, z, slab("tin_slab", false));
                set(70, 27, cz, face("post", Direction.NORTH));
            }
            // the big south door: an opening x 68..72 y1..5 with the two leaves slid aside
            air(68, 1, z2 - 1, 72, 5, z2);
            fill(64, 1, z2 + 1, 67, 5, z2 + 1, "barn_wall");
            fill(73, 1, z2 + 1, 74, 5, z2 + 1, "barn_wall");
            for (int x = 63; x <= 75; x++) set(x, 6, z2 + 1, face("beam", Direction.EAST));
            // the hayloft door in the south gable, above the hay wagon
            air(62, L + 1, z2 - 1, 63, L + 2, z2);
            // small dirty windows down both long sides
            for (int z = 21; z <= 41; z += 6) {
                set(x1, 4, z, face("window_pane", Direction.WEST));
                set(x1 + 1, 4, z, Blocks.AIR);
                set(x2, 4, z, face("window_pane", Direction.EAST));
                set(x2 - 1, 4, z, Blocks.AIR);
            }
        }
        door(70, G + 1, 16, "barn_door", Direction.SOUTH, false, "barn_n");
        air(70, G + 1, 17, 70, G + 2, 17);
        // ---- timber frame: posts at x=66 and x=74 every six blocks, beams across at y10, braces
        if (lvl != null) {
            for (int z = 18; z <= 44; z += 6) {
                for (int x : new int[]{66, 74}) {
                    for (int y = 1; y <= 9; y++) set(x, y, z, face("post", Direction.NORTH));
                }
                for (int x = 60; x <= 80; x++) set(x, 10, z, face("beam", Direction.EAST));
                set(67, 9, z, face("brace", Direction.EAST));
                set(73, 9, z, face("brace", Direction.WEST));
            }
            for (int z = 18; z <= 44; z++) {
                set(66, 10, z, face("beam", Direction.NORTH));
                set(74, 10, z, face("beam", Direction.NORTH));
            }
        }
        // ---- the hayloft: y6 floor over the stalls, ladder at x=67 z=38
        if (lvl != null) {
            fill(60, L, 18, 65, L, 44, "barn_floor");
            for (int z = 18; z <= 44; z++) if (z != 38) set(66, L + 1, z, Blocks.DARK_OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, z > 18 && z != 39).setValue(FenceBlock.SOUTH, z < 44 && z != 37));
            fill(66, 1, 38, 66, L, 38, "barn_wall_dark");
            ladder(67, 1, L + 1, 38, Direction.EAST);
            // stacked bales
            for (int z = 18; z <= 24; z++) for (int y = L + 1; y <= L + 3; y++) for (int x = 60; x <= 62; x++) if (hash(x, y, z) % 5 != 0 || y == L + 1) set(x, y, z, "hay_bale");
            for (int z = 28; z <= 32; z++) for (int x = 60; x <= 61; x++) set(x, L + 1, z, "hay_bale");
            set(60, L + 2, 30, "hay_bale");
            for (int z = 26; z <= 44; z++) for (int x = 60; x <= 65; x++) if (hash(x, L, z) % 4 == 0 && lvl.getBlockState(o.offset(x, L + 1, z)).isAir()) prop(x, L + 1, z, "straw_layer", Direction.NORTH);
        }
        hide("loft_hay", 63, L + 1, 34, "hay_mound", Direction.EAST);
        drawer(60, L + 1, 27, "trunk", Direction.EAST, "note:journal2");
        lamp(64, 9, 42, "lantern_hang", Direction.NORTH, "LOFT", 2);
        prop(62, L + 1, 42, "pitchfork", Direction.SOUTH, "pickup:pitchfork");
        // ---- the stalls on the west side
        if (lvl != null) {
            fill(60, G, 18, 65, G, 44, "straw_floor");
            for (int z : new int[]{24, 30, 36}) {
                for (int x = 60; x <= 65; x++) {
                    set(x, 1, z, "barn_wall_dark");
                    set(x, 2, z, "barn_wall_dark");
                }
            }
            for (int z = 18; z <= 44; z++) {
                if (z % 6 == 0) continue;
                boolean gap = z == 21 || z == 27 || z == 33 || z == 40 || z == 41;
                if (!gap) set(66, 1, z, Blocks.DARK_OAK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true).setValue(FenceBlock.SOUTH, true));
            }
        }
        // stall 1 (z 18..23)
        prop(60, 1, 20, "trough", Direction.EAST);
        prop(60, 3, 19, "saddle", Direction.EAST, "look:saddle");
        prop(60, 3, 22, "bridle", Direction.EAST);
        hide("hay1", 64, 1, 19, "hay_mound", Direction.EAST);
        // stall 2 (z 25..29)
        prop(60, 3, 26, "saddle", Direction.EAST);
        prop(60, 3, 28, "bridle", Direction.EAST);
        prop(61, 1, 28, "bucket", Direction.NORTH, "kick");
        prop(62, 1, 26, "bone_pile", Direction.NORTH);
        prop(63, 1, 27, "blood_pool", Direction.NORTH);
        // stall 3 (z 31..35): the saddle trunk with the crowbar
        drawer(60, 1, 33, "trunk", Direction.EAST, "item:crowbar");
        prop(60, 3, 32, "saddle", Direction.EAST);
        prop(60, 3, 34, "bridle", Direction.EAST);
        prop(63, 1, 32, "sacks", Direction.NORTH);
        // stall 4 (z 37..44)
        hide("hay2", 61, 1, 42, "hay_mound", Direction.EAST);
        prop(60, 1, 38, "trough", Direction.EAST);
        prop(60, 3, 40, "saddle", Direction.EAST);
        prop(63, 1, 39, "skull", Direction.EAST);
        prop(64, 1, 44, "bucket", Direction.NORTH);
        // ---- the aisle
        lamp(68, 9, 24, "lantern_hang", Direction.EAST, "BARN", 1);
        lamp(63, 5, 26, "lantern_hang", Direction.EAST, "STALLS", 1);
        lamp(72, 9, 36, "lantern_hang", Direction.WEST, "BARN", 2);
        prop(72, 1, 42, "wagon_wheel", Direction.WEST);
        prop(68, 1, 34, "bucket", Direction.NORTH, "kick");
        prop(73, 1, 30, "blood_pool", Direction.NORTH);
        prop(70, 1, 27, "blood_pool", Direction.NORTH);
        mark(70, 9, 30, "sleep");   // where it hangs from the rafters
        // ---- workshop x74..80 z33..44 (east, south half)
        if (lvl != null) for (int x = 75; x <= 80; x++) for (int y = 1; y <= 2; y++) if (x != 77) set(x, y, 32, "barn_wall_dark");
        prop(80, 1, 36, "workbench", Direction.WEST);
        prop(80, 1, 37, "workbench", Direction.WEST);
        prop(80, 3, 39, "tool_rack", Direction.WEST);
        prop(80, 3, 35, "tool_rack", Direction.WEST);
        drawer(80, 2, 36, "toolbox", Direction.WEST, "battery");
        mode(76, 1, 44, "generator", Direction.NORTH, 0);
        tag(76, 1, 44, "generator");
        mode(80, 3, 42, "fuse_box", Direction.WEST, 0);
        prop(78, 1, 44, "barrel", Direction.NORTH);
        prop(79, 1, 44, "barrel", Direction.NORTH);
        drawer(80, 1, 44, "crate", Direction.NORTH, "note:vet");
        prop(80, 2, 44, "crate", Direction.NORTH);
        prop(75, 1, 43, "gas_can", Direction.NORTH, "pickup:gas_can");
        prop(80, 1, 41, "cabinet", Direction.WEST, "toolcab");
        prop(77, 1, 39, "shovel_prop", Direction.EAST);
        lamp(77, 9, 36, "bulb", Direction.NORTH, "WORKSHOP", 2);
        prop(75, 1, 34, "tire", Direction.NORTH);
        // ---- the slaughter end x74..80 z18..31 (east, north half)
        if (lvl != null) fill(75, G, 18, 80, G, 31, "butcher_tile");
        for (int z = 20; z <= 28; z += 2) {
            int x = 77;
            String what = z == 24 ? "hanging_body" : "carcass";
            tall(x, 4, z, what, Direction.WEST);
        }
        if (lvl != null) for (int z = 19; z <= 29; z++) set(77, 6, z, face("beam", Direction.NORTH));
        for (int z = 20; z <= 28; z += 2) prop(77, 1, z, "blood_pool", Direction.NORTH);
        prop(79, 1, 23, "butcher_table", Direction.WEST);
        prop(79, 1, 24, "butcher_table", Direction.WEST);
        prop(75, 1, 27, "chopping_block", Direction.EAST);
        prop(80, 1, 19, "bone_pile", Direction.NORTH);
        prop(80, 1, 29, "trough", Direction.WEST);
        prop(76, 1, 30, "bucket", Direction.NORTH);
        prop(80, 3, 21, "chain_wall", Direction.WEST);
        prop(80, 3, 27, "chain_wall", Direction.WEST);
        prop(75, 2, 31, "handprint", Direction.NORTH);
        lamp(75, 9, 24, "bulb", Direction.NORTH, "SLAUGHTER", 2);
        // ---- the hay wagon under the loft door, outside
        if (lvl != null) {
            fill(60, 1, 48, 64, 1, 51, slab("barn_slab", true));
            for (int x = 60; x <= 64; x++) for (int z = 48; z <= 51; z++) if (hash(x, 2, z) % 3 != 0) set(x, 2, z, "hay_bale");
            prop(59, 1, 48, "wagon_wheel", Direction.EAST);
            prop(59, 1, 51, "wagon_wheel", Direction.EAST);
            prop(65, 1, 48, "wagon_wheel", Direction.WEST);
            prop(65, 1, 51, "wagon_wheel", Direction.WEST);
        }
        lamp(69, 5, 47, "lantern_hang", Direction.SOUTH, "YARD", 1);
    }

    // ------------------------------------------------------------------ the silo
    private void silo() {
        int cx = 88, cz = 31;
        if (lvl != null) {
            // the yard between barn and silo: the barn's outer boards there are gone (its inner wall at x=81 stays), but its roof eave stays
            air(cx - 6, 1, cz - 6, cx - 5, 10, cz + 6);
            air(cx - 4, 1, cz - 6, cx + 6, 28, cz + 6);
            // the two barn windows on that stretch sit in the inner wall now
            for (int z : new int[]{27, 33}) set(cx - 7, 4, z, face("window_pane", Direction.EAST));
            for (int x = cx - 6; x <= cx + 6; x++)
                for (int z = cz - 6; z <= cz + 6; z++) {
                    double d = Math.sqrt((x - cx) * (x - cx) + (z - cz) * (z - cz));
                    if (d <= 4.6) set(x, G, z, "packed_dirt");
                    if (d > 3.5 && d <= 4.6) for (int y = 1; y <= 24; y++) set(x, y, z, "silo_wall");
                    if (d <= 4.7) {
                        set(x, 25, z, slab("tin_slab", false));
                        if (d <= 3.2) set(x, 25, z, "barn_roof");
                        if (d <= 2.2) set(x, 26, z, slab("tin_slab", false));
                    }
                }
            ladder(cx + 3, 1, 22, cz, Direction.WEST);
        }
        door(84, G + 1, 31, "farm_door", Direction.WEST, false, "silo");
        air(85, G + 1, 31, 85, G + 2, 31);
        prop(83, G + 1, 32, "padlock", Direction.WEST);
        // Lily's nest inside
        prop(88, 1, 29, "sacks", Direction.SOUTH);
        prop(87, 1, 33, "rug", Direction.NORTH);
        prop(88, 1, 33, "rug", Direction.NORTH);
        prop(89, 1, 29, "bucket", Direction.NORTH);
        lamp(90, 1, 33, "lantern", Direction.WEST, "SILO", 0);
        prop(86, 1, 29, "note_paper", Direction.NORTH, "note:silo");
        GUESTS.add(new int[]{88, 1, 32, 90, 0, 1});
    }

    // ------------------------------------------------------------------ Dad's butcher shed
    private void shed() {
        int x1 = 38, x2 = 50, z1 = 16, z2 = 28;
        if (lvl != null) {
            air(x1, 1, z1, x2, 8, z2);
            fill(x1, G, z1, x2, G, z2, "slaughter_floor");
            fill(41, G, 19, 47, G, 25, "butcher_tile");
            for (int y = 1; y <= 5; y++) {
                for (int x = x1; x <= x2; x++) {
                    set(x, y, z1, "siding_dark");
                    set(x, y, z2, "siding_dark");
                }
                for (int z = z1; z <= z2; z++) {
                    set(x1, y, z, "siding_dark");
                    set(x2, y, z, "siding_dark");
                }
            }
            fill(x1 - 1, 6, z1 - 1, x2 + 1, 6, z2 + 1, slab("tin_slab", false));
            fill(x1, 6, z1, x2, 6, z2, "barn_roof");
            fill(x1 + 1, 5, z1 + 1, x2 - 1, 5, z2 - 1, Blocks.AIR);
            for (int x = x1 + 1; x <= x2 - 1; x++) set(x, 5, 22, face("beam", Direction.EAST));
            set(x1, 3, 22, "boarded_window");
            set(x2, 3, 20, "boarded_window");
        }
        door(44, G + 1, 28, "farm_door", Direction.NORTH, false, "shed");
        // hooks along the beam
        for (int x = 40; x <= 48; x += 2) {
            String what = x == 42 || x == 46 ? "hanging_body" : "carcass";
            tall(x, 3, 22, what, Direction.SOUTH);
            prop(x, 1, 22, "blood_pool", Direction.NORTH);
        }
        // Dad, on the floor by the table, in his coat
        prop(43, 1, 19, "corpse_head", Direction.SOUTH, "coat");
        prop(43, 1, 20, "corpse_legs", Direction.SOUTH, "coat");
        prop(42, 1, 19, "blood_pool", Direction.NORTH);
        prop(44, 1, 19, "blood_pool", Direction.NORTH);
        prop(43, 1, 21, "blood_pool", Direction.NORTH);
        prop(46, 1, 18, "butcher_table", Direction.SOUTH);
        prop(47, 1, 18, "butcher_table", Direction.SOUTH);
        prop(46, 2, 18, "meat_chunk", Direction.SOUTH, "pickup:meat");
        prop(40, 1, 18, "workbench", Direction.SOUTH);
        mode(40, 2, 18, "cassette_player", Direction.SOUTH, 0);
        tag(40, 2, 18, "tape");
        prop(39, 3, 18, "antlers", Direction.EAST);
        prop(39, 3, 20, "antlers", Direction.EAST);
        prop(49, 1, 17, "bone_pile", Direction.NORTH);
        prop(49, 1, 18, "skull", Direction.WEST);
        prop(39, 1, 26, "drifter", Direction.EAST, "look:drifter");
        prop(41, 1, 27, "sacks", Direction.NORTH);
        prop(48, 1, 26, "chopping_block", Direction.NORTH);
        tall(49, 1, 25, "scarecrow", Direction.WEST);
        tag(49, 1, 25, "look:scarecrow_half");
        prop(49, 3, 21, "chain_wall", Direction.WEST);
        prop(39, 3, 24, "chain_wall", Direction.EAST);
        prop(45, 3, 16 + 1, "handprint", Direction.SOUTH);
        drawer(48, 1, 27, "crate", Direction.NORTH, "note:butcher");
        lamp(45, 4, 22, "bulb", Direction.NORTH, "SHED", 1);
    }

    // ------------------------------------------------------------------ the yard, fences and the gate
    private void yard() {
        // perimeter fence with the chained gate at x 48..52 on z=99, and a broken stretch at x=56 to squeeze through
        fence(1, 3, 99, 3, 1);
        fence(1, 3, 1, 99, 1);
        fence(99, 3, 99, 99, 1);
        fence(1, 99, 47, 99, 1);
        fence(53, 99, 55, 99, 1);
        fence(58, 99, 99, 99, 1);
        if (lvl != null) {
            set(57, 1, 99, Blocks.AIR);
            set(56, 1, 99, Blocks.AIR);
            prop(56, 1, 100, "brace", Direction.EAST);
            for (int x = 48; x <= 52; x++) {
                set(x, 1, 99, face("farm_gate", Direction.NORTH).setValue(OpenableBlock.OPEN, false));
                set(x, 2, 99, Blocks.AIR);
            }
            fill(47, 1, 99, 47, 3, 99, face("post", Direction.NORTH));
            fill(53, 1, 99, 53, 3, 99, face("post", Direction.NORTH));
        }
        for (int x = 48; x <= 52; x++) tag(x, 1, 99, "gate");
        prop(50, 2, 98, "padlock", Direction.SOUTH);
        // the mailbox post by the road
        if (lvl != null) set(46, 1, 101, face("post", Direction.NORTH));
        prop(46, 2, 101, "birdbox", Direction.WEST, "look:mailbox");
        // the well, between house and yard
        prop(40, 1, 62, "well", Direction.SOUTH, "well");
        // the dinner bell on its post by the porch
        if (lvl != null) fill(36, 1, 74, 36, 3, 74, face("post", Direction.NORTH));
        if (lvl != null) set(36, 4, 74, Blocks.BELL);
        tag(36, 4, 74, "bell");
        // the outhouse
        if (lvl != null) {
            fill(6, 1, 40, 8, 4, 42, "siding_dark");
            air(7, 1, 41, 7, 3, 41);
            fill(5, 5, 39, 9, 5, 43, slab("tin_slab", false));
        }
        door(7, 1, 42, "farm_door", Direction.NORTH, false, "outhouse");
        prop(7, 1, 41, "toilet", Direction.SOUTH);
        // dead trees
        tree(10, 60);
        tree(44, 70);
        tree(30, 30);
        tree(92, 8);
        // where the truck stands
        mark(46, 1, 46, "truck");
        // junk in the yard
        prop(52, 1, 44, "tire", Direction.NORTH);
        prop(53, 1, 45, "barrel", Direction.WEST);
        prop(40, 1, 46, "milk_can", Direction.EAST);
        prop(56, 1, 52, "trough", Direction.NORTH);
        prop(38, 1, 52, "wagon_wheel", Direction.EAST);
        prop(55, 1, 40, "sacks", Direction.WEST);
        // scarecrows along the road, like a warning (the photograph)
        int[][] crows = {{46, 88}, {54, 84}, {46, 78}, {54, 92}, {20, 88}, {30, 84}, {12, 80}};
        for (int[] c : crows) {
            tall(c[0], 1, c[1], "scarecrow", (c[0] < 50) ? Direction.EAST : Direction.WEST);
            tag(c[0], 1, c[1], "look:scarecrow");
            tag(c[0], 2, c[1], "look:scarecrow");
        }
        // the old pasture fence the scarecrows hang on
        fence(4, 82, 44, 82, 1);
        fence(4, 90, 44, 90, 1);
    }

    private void tree(int x, int z) {
        if (lvl == null) return;
        BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
        int h = 7 + hash(x, 1, z) % 4;
        fill(x, 1, z, x, h, z, log);
        Random r = new Random(x * 31L + z);
        for (int b = 0; b < 5; b++) {
            int y = 4 + r.nextInt(h - 3);
            Direction d = Direction.from2DDataValue(r.nextInt(4));
            int len = 2 + r.nextInt(3);
            for (int i = 1; i <= len; i++) {
                set(x + d.getStepX() * i, y + i / 2, z + d.getStepZ() * i, log.setValue(RotatedPillarBlock.AXIS, d.getAxis()));
            }
        }
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if ((dx == 0) != (dz == 0)) set(x + dx, 1, z + dz, Blocks.DARK_OAK_WOOD);
    }

    // ------------------------------------------------------------------ the fields
    private void fields() {
        // the corn, south-east: rows east-west, two blocks apart, a north-south lane at x=76 and a cross lane at z=74
        for (int z = 54; z <= 94; z += 2) {
            for (int x = 58; x <= 96; x++) {
                if (x == 76 || x == 77 || z == 74) continue;
                int h = hash(x, 3, z);
                if (h % 11 == 0) continue;
                tall(x, 1, z, "corn", Direction.from2DDataValue(h & 3));
                CORN.add(BlockPos.asLong(x, 1, z));
                CORN.add(BlockPos.asLong(x, 1, z + 1));
            }
        }
        if (lvl != null) for (int z = 54; z <= 95; z++) for (int x = 58; x <= 96; x++) if (z % 2 == 1 || x == 76 || x == 77) {
            BlockPos p = o.offset(x, 1, z);
            if (lvl.getBlockState(p).isAir() && hash(x, 9, z) % 4 == 0) prop(x, 1, z, "dry_grass", Direction.NORTH);
        }
        // scarecrows deep in the corn, on the lane
        int[][] crows = {{76, 60}, {77, 70}, {76, 82}, {66, 74}, {88, 74}};
        for (int[] c : crows) {
            tall(c[0], 1, c[1], "scarecrow", Direction.from2DDataValue(hash(c[0], 1, c[1]) & 3));
            tag(c[0], 1, c[1], "look:scarecrow");
        }
        fence(56, 52, 98, 52, 1);
        if (lvl != null) for (int x = 74; x <= 79; x++) set(x, 1, 52, Blocks.AIR);
        mark(76, 1, 84, "field_lily");
    }

    static Direction dummy() { return Direction.NORTH; }

    static Block air() { return Blocks.AIR; }
}
