package com.krolasyon.bloodsea.world;

import com.krolasyon.bloodsea.registry.ModBlocks;
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
        if (reg == Region.BONES && Noise.rand(x, z, S + 6) < 0.45) skin = st(ModBlocks.HUMAN_BONES);
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
            case DRIFT -> raft(x, z, col);
        }
        return false;
    }

    // ================================================================== the Bone Reach
    private static void bones(int x, int z, int floor, BlockState[] col) {
        BlockState bones = st(ModBlocks.HUMAN_BONES), skulls = st(ModBlocks.SKULL_PILE);
        // islands of the dead, heaped out of the blood
        double v = Noise.fbm(x, z, 70, S + 10);
        if (v > 0.3) {
            int top = Math.min(SEA + 14, SEA + 1 + (int) ((v - 0.3) * 55));
            fill(col, floor, top, bones);
            put(col, top, Noise.rand(x, z, S + 11) < 0.35 ? skulls : bones);
            if (Noise.rand(x, z, S + 12) < 0.04) fill(col, top + 1, top + 1 + (int) (Noise.rand(x, z, S + 13) * 3), skulls);
        } else if (Noise.fbm(x, z, 22, S + 15) > 0.18) {
            // mats of the drowned floating on the surface, bones knotted together
            put(col, SEA, Noise.rand(x, z, S + 16) < 0.18 ? skulls : bones);
            if (Noise.rand(x, z, S + 17) < 0.06) put(col, SEA + 1, Noise.rand(x, z, S + 18) < 0.5 ? skulls : pillar(ModBlocks.RIB_BONE, Direction.Axis.Y));
        } else if (Noise.rand(x, z, S + 14) < 0.015) {
            // a skull just breaking the surface
            fill(col, floor, SEA, bones);
            put(col, SEA + 1, skulls);
        }
        // rib cages arching out of the sea
        int cell = 56;
        int cx0 = Math.floorDiv(x, cell), cz0 = Math.floorDiv(z, cell);
        for (int cx = cx0 - 1; cx <= cx0 + 1; cx++)
            for (int cz = cz0 - 1; cz <= cz0 + 1; cz++) {
                if (Noise.rand(cx, cz, S + 20) > 0.42) continue;
                double ox = cx * cell + Noise.rand(cx, cz, S + 21) * cell, oz = cz * cell + Noise.rand(cx, cz, S + 22) * cell;
                if (Regions.at(ox, oz) != Region.BONES) continue;
                double a = Noise.rand(cx, cz, S + 23) * Math.PI;
                double R = 9 + Noise.rand(cx, cz, S + 24) * 9;
                int ribs = 3 + (int) (Noise.rand(cx, cz, S + 25) * 4);
                double dx = x - ox, dz = z - oz;
                double along = dx * Math.cos(a) + dz * Math.sin(a), across = -dx * Math.sin(a) + dz * Math.cos(a);
                double half = (ribs - 1) / 2.0 * 4;
                double crown = SEA - 6 + R * 1.25;
                if (Math.abs(across) < 0.8 && Math.abs(along) <= half + 1) put(col, (int) crown + 1, pillar(ModBlocks.RIB_BONE, Math.abs(Math.cos(a)) > 0.7 ? Direction.Axis.X : Direction.Axis.Z));
                for (int k = 0; k < ribs; k++) {
                    double t = (k - (ribs - 1) / 2.0) * 4;
                    if (Math.abs(along - t) > 0.55 || Math.abs(across) > R) continue;
                    double ys = SEA - 6 + Math.sqrt(R * R - across * across) * 1.25;
                    fill(col, (int) ys - 1, (int) ys + 1, pillar(ModBlocks.RIB_BONE, Direction.Axis.Y));
                    if (Math.abs(across) > R - 0.9) fill(col, floor, (int) ys, pillar(ModBlocks.RIB_BONE, Direction.Axis.Y));
                }
            }
        // the Ossuary Throne
        double dt = Math.sqrt((x - THRONE_X) * (double) (x - THRONE_X) + (z - THRONE_Z) * (double) (z - THRONE_Z));
        if (dt < 40) {
            int P = SEA + 17;
            int top = Math.min(P, SEA + 3 + (int) ((40 - dt) * 0.42));
            fill(col, floor, top, bones);
            put(col, top, dt > 6 && Noise.rand(x, z, S + 30) < 0.4 ? skulls : bones);
            int lx = x - THRONE_X, lz = z - THRONE_Z;
            if (Math.abs(lx) <= 4 && Math.abs(lz) <= 4) {
                fill(col, floor, P, bones);
                for (int y = P + 1; y <= P + 12; y++) put(col, y, null);
                if (lz == -2 && Math.abs(lx) <= 2) fill(col, P + 1, P + (Math.abs(lx) == 2 ? 7 : 6), skulls);       // the back
                if (lz == -2 && Math.abs(lx) == 3) fill(col, P + 1, P + 9, pillar(ModBlocks.RIB_BONE, Direction.Axis.Y)); // two horns
                if (Math.abs(lx) == 2 && lz >= -1 && lz <= 0) fill(col, P + 1, P + 2, skulls);                        // the arms
                if (lx == 0 && lz == -1) {
                    put(col, P + 1, skulls);                                                                           // the seat
                    put(col, P + 2, st(ModBlocks.THRONE_HEART));                                                       // and on it, the heart
                }
            }
        }
    }

    // ================================================================== Leviathan's Deep
    private static double spineZ(double x) { return 140 * Math.sin(x / 200.0); }

    private static void leviathan(int x, int z, int floor, BlockState[] col) {
        BlockState bone = pillar(ModBlocks.LEVIATHAN_BONE, Direction.Axis.Y), vert = st(ModBlocks.VERTEBRA);
        // the bones of the last one, as long as a town
        if (x >= 600 && x <= 1300) {
            double dz = z - spineZ(x);
            if (Math.abs(dz) <= 3) fill(col, 32, 38, vert);
            if (x % 8 == 0 && Math.abs(dz) <= 1) fill(col, 38, SEA + 7, bone);
            if (x >= 650 && x <= 1250 && Math.floorMod(x, 12) <= 1 && Math.abs(dz) <= 34) {
                double ys = 36 + Math.sqrt(34 * 34 - dz * dz) * 1.5;
                fill(col, (int) ys - 1, (int) ys + 1, bone);
                if (Math.abs(dz) > 30) fill(col, floor, (int) ys, bone);
            }
            // its skull, half sunk
            double sx = 1320, sz = spineZ(1320);
        }
        double ds = Math.sqrt((x - 1320) * (double) (x - 1320) + (z - spineZ(1320)) * (z - spineZ(1320)));
        if (ds < 20) fill(col, floor, (int) (36 + Math.sqrt(400 - ds * ds) * 1.35), ds > 6 && ds < 9 && Noise.rand(x, z, S + 40) < 0.5 ? st(ModBlocks.EDGE_STONE) : vert);
        // the Maw: a ring of teeth taller than a church, and a black throat in the middle
        double dm = Math.sqrt((x - MAW_X) * (double) (x - MAW_X) + (z - MAW_Z) * (double) (z - MAW_Z));
        if (dm < 30) {
            double ang = Math.toDegrees(Math.atan2(z - MAW_Z, x - MAW_X)) + 360;
            double inTooth = Math.abs(((ang % 16) + 16) % 16 - 8);
            // each fang leans in over the throat and thins to a point
            for (int y = floor; y <= SEA + 30; y++) {
                double up = Math.max(0, y - SEA);
                double rc = 23 - up * up * 0.012;
                double w = 2.6 * (1 - up / 31.0);
                double wa = 4.0 * (1 - up / 34.0);
                if (Math.abs(dm - rc) < w && inTooth < wa) put(col, y, pillar(ModBlocks.LEVIATHAN_TOOTH, Direction.Axis.Y));
            }
            if (dm < 6) {
                fill(col, floor, SEA - 3, st(ModBlocks.EDGE_STONE));
                fill(col, SEA - 2, SEA, st(ModBlocks.MAW_CORE));
            }
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
    private static void raft(int x, int z, BlockState[] col) {
        if (Math.abs(x) <= 2 && Math.abs(z) <= 2) {
            put(col, SEA, Blocks.SPRUCE_PLANKS.defaultBlockState());
            if (x == 0 && z == -2) {
                fill(col, SEA + 1, SEA + 4, Blocks.SPRUCE_FENCE.defaultBlockState());
                put(col, SEA + 5, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
            }
        }
    }

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
    }

    /** the highest non-air block of a column, for height queries */
    public static int top(int x, int z) {
        BlockState[] col = new BlockState[HEIGHT];
        column(x, z, col);
        for (int i = col.length - 1; i >= 0; i--) if (col[i] != null) return MIN_Y + i + 1;
        return MIN_Y;
    }
}
