package com.krolasyon.bloodsea.world;

import com.krolasyon.bloodsea.block.BonePropBlock;
import com.krolasyon.bloodsea.registry.ModBlocks;
import com.krolasyon.bloodsea.registry.ModProps;
import com.krolasyon.bloodsea.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;

import static com.krolasyon.bloodsea.world.Regions.*;

/**
 * Every block of the Blood Sea, column by column. One function decides a column (used by the chunk generator and by
 * height queries alike), so the sea is always the same and nothing depends on neighbouring chunks.
 */
public final class SeaShape {
    public static final int HEIGHT = 384;
    private static final long S = 4471;

    private SeaShape() {}

    private static BlockState st(net.minecraftforge.registries.RegistryObject<Block> b) { return b.get().defaultBlockState(); }

    private static BlockState pillar(net.minecraftforge.registries.RegistryObject<Block> b, Direction.Axis a) {
        return b.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, a);
    }

    private static void put(BlockState[] col, int y, BlockState s) {
        int i = y - MIN_Y;
        if (i >= 0 && i < col.length) col[i] = s;
    }

    private static void fill(BlockState[] col, int y0, int y1, BlockState s) {
        for (int y = Math.max(y0, MIN_Y); y <= y1; y++) put(col, y, s);
    }

    /** true when the column is the waterfall at the end of the sea */
    public static boolean column(int x, int z, BlockState[] col) {
        java.util.Arrays.fill(col, null);
        Region reg = Regions.at(x, z);
        double r = Regions.dist(x, z);
        BlockState blood = st(ModBlocks.BLOOD);

        // ---- the end of the sea: past the rim there is nothing at all; on the rim the blood pours over
        if (reg == Region.EDGE && r > EDGE_R) {
            if (r <= EDGE_R + 1.6) {
                BlockState falling = ModFluids.BLOOD_FLOWING.get().getFlowing(8, true).createLegacyBlock();
                fill(col, MIN_Y, SEA, falling);
                return true;
            }
            return false;
        }

        int floor = switch (reg) {
            case DRIFT -> (int) (30 + 7 * Noise.fbm(x, z, 90, S + 1));
            case BONES -> (int) (38 + 7 * Noise.fbm(x, z, 80, S + 2));
            case LEVIATHAN -> {
                double t = Math.min(1, Math.max(0, (r - DRIFT_R) / 260));
                yield (int) (30 + (-22 + 12 * Noise.fbm(x, z, 110, S + 3) - 30) * t);
            }
            case EDGE -> (int) (40 + 8 * Noise.fbm(x, z, 100, S + 4) + 12 * Math.max(0, (r - DRIFT_R) / (EDGE_R - DRIFT_R)));
            case MIRAGE -> (int) (40 + 6 * Noise.fbm(x, z, 90, S + 5));
        };
        // the sea bed: deep black stone, a layer of flesh, a skin of clotted blood
        put(col, MIN_Y, Blocks.BEDROCK.defaultBlockState());
        fill(col, MIN_Y + 1, floor - 6, st(ModBlocks.EDGE_STONE));
        fill(col, floor - 5, floor - 1, st(ModBlocks.FLESH));
        BlockState skin = st(ModBlocks.CLOT);
        if (reg == Region.BONES) skin = st(ModBlocks.BONE_MUD);
        if (reg == Region.LEVIATHAN && Noise.rand(x, z, S + 7) < 0.3) skin = st(ModBlocks.EDGE_STONE);
        put(col, floor, skin);
        fill(col, floor + 1, SEA, blood);

        switch (reg) {
            case BONES -> bones(x, z, floor, col);
            case LEVIATHAN -> leviathan(x, z, floor, col);
            case EDGE -> {
                // the rim: a lip of black stone just under the surface, and the blood running over it
                if (r > EDGE_R - 5) fill(col, floor, SEA - 3, st(ModBlocks.EDGE_STONE));
                // broken black spires along the rim, the last things standing before nothing
                if (r > EDGE_R - 16) {
                    int cx = Math.floorDiv(x, 7), cz = Math.floorDiv(z, 7);
                    double sx = cx * 7 + 3.5, sz = cz * 7 + 3.5;
                    double ds = Math.sqrt((x - sx) * (x - sx) + (z - sz) * (z - sz));
                    if (Noise.rand(cx, cz, S + 70) < 0.35 && ds < 1.8 && Regions.dist(sx, sz) < EDGE_R - 1)
                        fill(col, floor, SEA + 4 + (int) (Noise.rand(cx, cz, S + 71) * 18 * (1 - ds / 2.2)), st(ModBlocks.EDGE_STONE));
                }
            }
            case MIRAGE -> mirage(x, z, floor, col);
            case DRIFT -> {
                raft(x, z, col);
                if (Regions.dist(x, z) > 40 && Noise.rand(x, z, S + 19) < 0.0015) put(col, SEA + 1, prop(Noise.rand(x, z, S + 9) < 0.6 ? "skull_sunk" : "hand", x, z, false));
            }
        }
        return false;
    }

    // ================================================================== the Bone Reach
    private static final String[] GROUND = {"skull", "skull", "skull", "skull_side", "skull_side", "bone_pile", "bone_pile", "ribs", "ribs",
            "skeleton", "skeleton", "spine", "ribcage", "skull_heap"};
    private static final String[] FLOAT = {"skull_sunk", "skull_sunk", "skull_sunk", "ribs_sunk", "ribs_sunk", "skeleton_sunk", "skeleton_sunk", "hand", "hand"};

    /** one of the bone props, turned some way, lying in blood or not */
    static BlockState prop(String name, int x, int z, boolean blood) {
        Direction[] dirs = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        return ModProps.get(name).defaultBlockState().setValue(BonePropBlock.FACING, dirs[(int) (Noise.rand(x, z, S + 90) * 4)])
                .setValue(BonePropBlock.BLOODLOGGED, blood);
    }

    private static BlockState layer(int n) {
        return ModBlocks.MUD_LAYER.get().defaultBlockState().setValue(com.krolasyon.bloodsea.block.MudLayerBlock.LAYERS, n);
    }

    private static String pick(String[] list, int x, int z, long salt) { return list[(int) (Noise.rand(x, z, salt) * list.length)]; }

    private static void bones(int x, int z, int floor, BlockState[] col) {
        BlockState mud = st(ModBlocks.BONE_MUD);
        // islands of wet black mud, and the dead lying on them
        double v = Noise.fbm(x, z, 70, S + 10);
        if (v > 0.3) {
            double hf = Math.min(SEA + 9.9, SEA + 1 + (v - 0.3) * 40);
            int top = (int) hf;
            fill(col, floor, top, mud);
            int layers = (int) Math.round((hf - top) * 8);
            if (Noise.rand(x, z, S + 11) < 0.16) put(col, top + 1, prop(pick(GROUND, x, z, S + 12), x, z, false));
            else if (layers > 0) put(col, top + 1, layer(Math.min(8, layers)));
        } else if (Noise.rand(x, z, S + 15) < 0.014) {
            // the drowned, floating
            put(col, SEA + 1, prop(pick(FLOAT, x, z, S + 16), x, z, false));
        } else if (Noise.rand(x, z, S + 17) < 0.02 && floor < SEA - 2) {
            // and the ones that sank
            put(col, floor + 1, prop(pick(GROUND, x, z, S + 18), x, z, true));
        }
        // the Ossuary Throne: a hill of mud and skulls, and on top a throne of skulls with a heart on the seat
        double dt = Math.sqrt((x - THRONE_X) * (double) (x - THRONE_X) + (z - THRONE_Z) * (double) (z - THRONE_Z));
        if (dt < 40) {
            int P = SEA + 17;
            double hf = Math.min(P, SEA + 3 + (40 - dt) * 0.42);
            int top = (int) hf;
            for (int y = SEA + 1; y <= P + 12; y++) put(col, y, null);
            fill(col, floor, top, mud);
            int layers = (int) Math.round((hf - top) * 8);
            if (dt > 5 && Noise.rand(x, z, S + 30) < 0.34) put(col, top + 1, prop(Noise.rand(x, z, S + 31) < 0.3 ? "skull_heap" : pick(GROUND, x, z, S + 32), x, z, false));
            else if (layers > 0 && top < P) put(col, top + 1, layer(Math.min(8, layers)));
            int lx = x - THRONE_X, lz = z - THRONE_Z;
            if (Math.abs(lx) <= 4 && Math.abs(lz) <= 4) {
                fill(col, floor, P, mud);
                for (int y = P + 1; y <= P + 12; y++) put(col, y, null);
                BlockState heap = prop("skull_heap", x, z, false);
                if (lz == -2 && Math.abs(lx) <= 2) {                                    // the back
                    int h = Math.abs(lx) == 2 ? 5 : 4;
                    for (int y = P + 1; y <= P + h; y++) put(col, y, prop("skull_heap", x, z + y, false));
                    put(col, P + h + 1, prop("skull", x, z + 7, false));
                }
                if (lz == -2 && Math.abs(lx) == 3) { put(col, P + 1, prop("ribs", x, z, false)); put(col, P + 2, prop("ribs", x + 1, z, false)); }  // horns
                if (Math.abs(lx) == 2 && lz >= -1 && lz <= 0) put(col, P + 1, heap);    // the arms
                if (lx == 0 && lz == -1) {
                    put(col, P + 1, heap);                                               // the seat
                    put(col, P + 2, ModProps.get("heart").defaultBlockState().setValue(BonePropBlock.FACING, Direction.SOUTH)); // and on it, the heart
                }
                if (lz >= 1 && Noise.rand(x, z, S + 33) < 0.25) put(col, P + 1, prop("skull", x, z, false));
            }
        }
    }

    // ================================================================== Leviathan's Deep
    private static double spineZ(double x) { return 140 * Math.sin(x / 200.0); }

    private static void leviathan(int x, int z, int floor, BlockState[] col) {
        // the bones themselves are giants (models); here only the throat in the middle of the Maw
        double dm = Math.sqrt((x - MAW_X) * (double) (x - MAW_X) + (z - MAW_Z) * (double) (z - MAW_Z));
        if (dm < 6) {
            fill(col, floor, SEA - 3, st(ModBlocks.EDGE_STONE));
            fill(col, SEA - 2, SEA, st(ModBlocks.MAW_CORE));
        }
    }

    // ================================================================== the giant dead
    public record Giant(String variant, double x, double y, double z, float yaw) {}

    private static java.util.List<Giant> LEVIATHAN = null;

    /** the dead leviathan, laid out along its spine: a tail, segments of ribs, and its skull at the east end */
    static synchronized java.util.List<Giant> leviathanParts() {
        if (LEVIATHAN != null) return LEVIATHAN;
        java.util.List<Giant> out = new java.util.ArrayList<>();
        double x = 540, along = 0, next = 0;
        int n = 0;
        while (x < 1300) {
            double z = spineZ(x);
            if (along >= next) {
                double dz = 0.7 * Math.cos(x / 200.0);
                float yaw = (float) -Math.toDegrees(Math.atan2(1, dz));
                String v = n == 0 ? "leviathan_tail_end" : n < 3 ? "leviathan_tail" : "leviathan_segment";
                out.add(new Giant(v, x, SEA - 5, z, yaw));
                next += 32;
                n++;
            }
            double nx = x + 0.25;
            along += Math.sqrt(0.0625 + Math.pow(spineZ(nx) - z, 2));
            x = nx;
        }
        double dz = 0.7 * Math.cos(x / 200.0);
        float yaw = (float) -Math.toDegrees(Math.atan2(1, dz));
        out.add(new Giant("leviathan_head", x + 10, SEA + 4, spineZ(x + 10), yaw + 180));  // its snout away from the body
        out.add(new Giant("maw", MAW_X + 0.5, SEA - 4, MAW_Z + 0.5, 0));
        LEVIATHAN = out;
        return out;
    }

    /** the giant dead whose anchor falls in this chunk */
    public static java.util.List<Giant> giantsIn(int cx, int cz) {
        java.util.List<Giant> out = new java.util.ArrayList<>();
        for (Giant g : leviathanParts()) if (Math.floorDiv((int) Math.floor(g.x()), 16) == cx && Math.floorDiv((int) Math.floor(g.z()), 16) == cz) out.add(g);
        // the Bone Reach: giant rib cages and skulls, one per cell at most
        int cell = 56;
        for (int gx = Math.floorDiv(cx * 16, cell); gx <= Math.floorDiv(cx * 16 + 15, cell); gx++)
            for (int gz = Math.floorDiv(cz * 16, cell); gz <= Math.floorDiv(cz * 16 + 15, cell); gz++) {
                if (Noise.rand(gx, gz, S + 20) > 0.42) continue;
                double ox = gx * cell + Noise.rand(gx, gz, S + 21) * cell, oz = gz * cell + Noise.rand(gx, gz, S + 22) * cell;
                if (Math.floorDiv((int) Math.floor(ox), 16) != cx || Math.floorDiv((int) Math.floor(oz), 16) != cz) continue;
                if (Regions.at(ox, oz) != Region.BONES || Regions.dist(ox - THRONE_X, oz - THRONE_Z) < 60) continue;
                if (Noise.fbm(ox, oz, 70, S + 10) > 0.22) continue;  // not inside an island
                float yaw = (float) (Noise.rand(gx, gz, S + 23) * 360);
                double k = Noise.rand(gx, gz, S + 24);
                if (k < 0.22) out.add(new Giant("giant_skull", ox, SEA + 1, oz, yaw));
                else out.add(new Giant(k < 0.6 ? "giant_ribcage" : "giant_ribcage_broken", ox, SEA - 2, oz, yaw));
            }
        return out;
    }

    /** called once per chunk when it is generated */
    public static void spawnGiants(net.minecraft.server.level.WorldGenRegion region) {
        net.minecraft.world.level.ChunkPos cp = region.getCenter();
        for (Giant g : giantsIn(cp.x, cp.z)) {
            com.krolasyon.bloodsea.entity.GiantEntity e = new com.krolasyon.bloodsea.entity.GiantEntity(com.krolasyon.bloodsea.registry.ModEntities.GIANT.get(), region.getLevel());
            e.moveTo(g.x(), g.y(), g.z(), g.yaw(), 0);
            e.setYRot(g.yaw());
            e.setVariant(g.variant());
            region.addFreshEntity(e);
        }
    }

    // ================================================================== the Mirage
    private static void mirage(int x, int z, int floor, BlockState[] col) {
        // islands that are there, and islands that are not
        int cell = 64;
        int cx0 = Math.floorDiv(x, cell), cz0 = Math.floorDiv(z, cell);
        for (int cx = cx0 - 1; cx <= cx0 + 1; cx++)
            for (int cz = cz0 - 1; cz <= cz0 + 1; cz++) {
                if (Noise.rand(cx, cz, S + 50) > 0.55) continue;
                double ox = cx * cell + 12 + Noise.rand(cx, cz, S + 51) * (cell - 24), oz = cz * cell + 12 + Noise.rand(cx, cz, S + 52) * (cell - 24);
                if (Regions.at(ox, oz) != Region.MIRAGE) continue;
                double R = 7 + Noise.rand(cx, cz, S + 53) * 9;
                double d = Math.sqrt((x - ox) * (x - ox) + (z - oz) * (z - oz)) + Noise.value(x, z, 6, S + 54) * 1.5;
                if (d > R) continue;
                boolean fake = Noise.rand(cx, cz, S + 55) < 0.72;
                int top = SEA + 2 + (int) ((R - d) * 0.3);
                BlockState sand = fake ? st(ModBlocks.MIRAGE_SAND) : Blocks.SAND.defaultBlockState();
                BlockState grass = fake ? st(ModBlocks.MIRAGE_GRASS) : Blocks.GRASS_BLOCK.defaultBlockState();
                fill(col, fake ? SEA - 2 : floor, top, sand);
                if (R - d > 2.5) put(col, top, grass);
                // one tree in the middle, swaying in no wind
                if (d < 0.75 && R > 9) {
                    BlockState log = fake ? st(ModBlocks.MIRAGE_LOG) : Blocks.OAK_LOG.defaultBlockState();
                    fill(col, top + 1, top + 6, log);
                }
                double dd = Math.sqrt((x - ox) * (x - ox) + (z - oz) * (z - oz));
                if (R > 9 && dd < 2.8) {
                    int ty = SEA + 2 + (int) (R * 0.3);
                    BlockState leaves = fake ? st(ModBlocks.MIRAGE_LEAVES) : Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
                    for (int y = ty + 5; y <= ty + 7; y++) if (dd < 2.8 - (y - ty - 5) * 0.8 && (dd > 0.6 || y == ty + 7)) put(col, y, leaves);
                }
            }
        // walls you cannot see
        double r = Regions.dist(x, z);
        if (r > DRIFT_R + 80 && Math.abs(Noise.fbm(x, z, 90, S + 60)) < 0.02) fill(col, SEA + 1, SEA + 5, st(ModBlocks.PHANTOM_WALL));
        // the lighthouse of home
        double dl = Math.sqrt((x - LIGHTHOUSE_X) * (double) (x - LIGHTHOUSE_X) + (z - LIGHTHOUSE_Z) * (double) (z - LIGHTHOUSE_Z));
        if (dl < 18) {
            for (int y = SEA - 1; y <= SEA + 40; y++) if (col[y - MIN_Y] != null && col[y - MIN_Y].getBlock() == ModBlocks.PHANTOM_WALL.get()) put(col, y, null);
            int top = SEA + 2 + (int) Math.max(0, (18 - dl) * 0.12);
            fill(col, floor, top, Blocks.SAND.defaultBlockState());
            if (dl < 15) put(col, top, Blocks.GRASS_BLOCK.defaultBlockState());
            int base = SEA + 4;
            if (dl <= 4.4) fill(col, top, base - 1, Blocks.STONE_BRICKS.defaultBlockState());
            if (dl <= 3.4) {
                if (dl >= 2.4) {
                    for (int y = base; y <= base + 30; y++) put(col, y, st(ModBlocks.LIGHTHOUSE_WALL));
                    if (x == LIGHTHOUSE_X + 3 && z == LIGHTHOUSE_Z) { put(col, base, st(ModBlocks.HOME_DOOR)); put(col, base + 1, st(ModBlocks.HOME_DOOR)); }
                }
                put(col, base + 31, st(ModBlocks.LIGHTHOUSE_WALL));
                if (dl >= 1.5) { put(col, base + 32, Blocks.GLASS.defaultBlockState()); put(col, base + 33, Blocks.GLASS.defaultBlockState()); }
                else { put(col, base + 32, st(ModBlocks.LIGHTHOUSE_LAMP)); put(col, base + 33, st(ModBlocks.LIGHTHOUSE_LAMP)); }
                put(col, base + 34, st(ModBlocks.LIGHTHOUSE_WALL));
            }
        }
    }

    // ================================================================== the raft at the start
    /** the raft is an entity now (it moves): nothing is built for it */
    private static void raft(int x, int z, BlockState[] col) {}

    // ================================================================== into a chunk
    public static void fillChunk(ChunkAccess chunk) {
        BlockState[] col = new BlockState[HEIGHT];
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        Heightmap floorMap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surfMap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int bx = chunk.getPos().getMinBlockX(), bz = chunk.getPos().getMinBlockZ();
        for (int lx = 0; lx < 16; lx++)
            for (int lz = 0; lz < 16; lz++) {
                boolean fall = column(bx + lx, bz + lz, col);
                for (int i = 0; i < col.length; i++) {
                    BlockState s = col[i];
                    if (s == null) continue;
                    m.set(bx + lx, MIN_Y + i, bz + lz);
                    chunk.setBlockState(m, s, false);
                    floorMap.update(lx, MIN_Y + i, lz, s);
                    surfMap.update(lx, MIN_Y + i, lz, s);
                }
                // the waterfall moves: let its top and the blood beside it tick once the chunk is done
                if (fall) chunk.markPosForPostprocessing(new BlockPos(bx + lx, SEA, bz + lz));
            }
        colliders(chunk);
    }

    // ================================================================== the solid parts of the giants
    private static final java.util.Map<String, int[][]> CELLS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final boolean SHOW = Boolean.getBoolean("bloodsea.showcolliders");

    static int[][] cells(String variant) {
        return CELLS.computeIfAbsent(variant, v -> {
            try (var in = SeaShape.class.getResourceAsStream("/data/bloodsea/giant_collision/" + v + ".json")) {
                if (in == null) return new int[0][];
                var arr = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in)).getAsJsonObject().getAsJsonArray("cells");
                int[][] out = new int[arr.size()][3];
                for (int i = 0; i < arr.size(); i++) {
                    var c = arr.get(i).getAsJsonArray();
                    out[i][0] = c.get(0).getAsInt();
                    out[i][1] = c.get(1).getAsInt();
                    out[i][2] = c.get(2).getAsInt();
                }
                return out;
            } catch (Exception e) {
                return new int[0][];
            }
        });
    }

    /** lay invisible solid blocks along every giant that reaches into this chunk */
    private static void colliders(ChunkAccess chunk) {
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        int bx = cx * 16, bz = cz * 16;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int ox = cx - 3; ox <= cx + 3; ox++)
            for (int oz = cz - 3; oz <= cz + 3; oz++)
                for (Giant g : giantsIn(ox, oz)) {
                    // the renderer turns the model by 180 - yaw and GeckoLib by another 180: -yaw in all, x mirrored
                    double a = Math.toRadians(-g.yaw());
                    double ca = Math.cos(a), sa = Math.sin(a);
                    for (int[] c : cells(g.variant())) {
                        double mx = -(c[0] + 0.5), my = c[1] + 0.5, mz = c[2] + 0.5;
                        double wx = g.x() + mx * ca + mz * sa, wz = g.z() - mx * sa + mz * ca;
                        int x = (int) Math.floor(wx), y = (int) Math.floor(g.y() + my), z = (int) Math.floor(wz);
                        if (x < bx || x >= bx + 16 || z < bz || z >= bz + 16 || y <= MIN_Y || y >= MIN_Y + HEIGHT) continue;
                        m.set(x, y, z);
                        BlockState here = chunk.getBlockState(m);
                        boolean blood = here.getBlock() == ModBlocks.BLOOD.get();
                        if (!here.isAir() && !blood) continue;
                        BlockState put = SHOW ? Blocks.GLASS.defaultBlockState()
                                : ModBlocks.BONE_COLLIDER.get().defaultBlockState().setValue(com.krolasyon.bloodsea.block.BoneColliderBlock.BLOODLOGGED, blood);
                        chunk.setBlockState(m, put, false);
                    }
                }
    }

    /** the highest non-air block of a column, for height queries */
    public static int top(int x, int z) {
        BlockState[] col = new BlockState[HEIGHT];
        column(x, z, col);
        for (int i = col.length - 1; i >= 0; i--) if (col[i] != null) return MIN_Y + i + 1;
        return MIN_Y;
    }
}
