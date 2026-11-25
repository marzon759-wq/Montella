package com.krolasyon.bloodsea.story;

import com.krolasyon.bloodsea.registry.ModBlocks;
import com.krolasyon.bloodsea.registry.ModProps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The places the endings need that are not on the sea: a hospital room, the inside of a stomach (both sealed in the
 * rock under the raft's start) and a patch of grey ocean with a fishing boat on it, high over the overworld.
 * Each returns the point its cutscene is framed on.
 */
public final class Sets {
    private Sets() {}

    private static void box(ServerLevel l, BlockPos o, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) l.setBlock(o.offset(x, y, z), s, 2);
    }

    private static void set(ServerLevel l, BlockPos o, int x, int y, int z, BlockState s) { l.setBlock(o.offset(x, y, z), s, 2); }

    /** a small white room: a bed, a monitor, a drip stand, a window full of light, a chair */
    public static Vec3 hospital(ServerLevel l) {
        BlockPos o = new BlockPos(8, -56, 8);
        box(l, o, -6, -2, -6, 6, 6, 6, Blocks.STONE.defaultBlockState());
        box(l, o, -5, -1, -5, 5, 5, 5, Blocks.WHITE_CONCRETE.defaultBlockState());
        box(l, o, -4, 0, -4, 4, 4, 4, Blocks.AIR.defaultBlockState());
        box(l, o, -4, -1, -4, 4, -1, 4, Blocks.SMOOTH_STONE.defaultBlockState());
        for (int x = -2; x <= 2; x += 4) for (int z = -2; z <= 2; z += 4) set(l, o, x, 5, z, Blocks.SEA_LANTERN.defaultBlockState());
        set(l, o, 0, 5, 0, Blocks.SEA_LANTERN.defaultBlockState());
        // the bed: three slabs and white sheets
        for (int z = -1; z <= 1; z++) {
            set(l, o, 0, 0, z, Blocks.SMOOTH_QUARTZ_SLAB.defaultBlockState());
            set(l, o, -1, 0, z, Blocks.AIR.defaultBlockState());
        }
        set(l, o, 0, 0, 2, Blocks.IRON_BARS.defaultBlockState());
        set(l, o, 0, 1, 2, Blocks.IRON_BARS.defaultBlockState());
        // the monitor and the drip
        set(l, o, 1, 0, 2, Blocks.IRON_BARS.defaultBlockState());
        set(l, o, 1, 1, 2, Blocks.BLACK_CONCRETE.defaultBlockState());
        set(l, o, -1, 0, 1, Blocks.IRON_BARS.defaultBlockState());
        set(l, o, -1, 1, 1, Blocks.IRON_BARS.defaultBlockState());
        set(l, o, -1, 2, 1, Blocks.END_ROD.defaultBlockState());
        // a window of white light, a door, a chair, flowers
        for (int x = -2; x <= 2; x++) for (int y = 1; y <= 3; y++) {
            set(l, o, x, y, 5, Blocks.WHITE_STAINED_GLASS.defaultBlockState());
            set(l, o, x, y, 6, Blocks.SEA_LANTERN.defaultBlockState());
        }
        set(l, o, 3, 0, -5, Blocks.DARK_OAK_DOOR.defaultBlockState());
        set(l, o, 3, 1, -5, Blocks.DARK_OAK_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        set(l, o, 2, 0, -1, Blocks.OAK_STAIRS.defaultBlockState().setValue(net.minecraft.world.level.block.StairBlock.FACING, Direction.EAST));
        set(l, o, -3, 0, 3, Blocks.OAK_SLAB.defaultBlockState());
        set(l, o, -3, 1, 3, Blocks.POTTED_POPPY.defaultBlockState());
        return new Vec3(o.getX() + 0.5, o.getY(), o.getZ() + 0.5);
    }

    /** a chamber of flesh, wet and close, with the bones of what it ate before */
    public static Vec3 stomach(ServerLevel l) {
        BlockPos o = new BlockPos(40, -56, 8);
        BlockState flesh = ModBlocks.FLESH.get().defaultBlockState();
        java.util.Random r = new java.util.Random(7);
        for (int x = -9; x <= 9; x++) for (int y = -2; y <= 9; y++) for (int z = -10; z <= 10; z++) {
            double k = (x * x) / 49.0 + ((y - 3) * (y - 3)) / 30.0 + (z * z) / 72.0;
            BlockPos q = o.offset(x, y, z);
            if (k < 1.0 && y >= 0) l.setBlock(q, Blocks.AIR.defaultBlockState(), 2);
            else if (k < 1.6 || y < 0) l.setBlock(q, r.nextInt(9) == 0 ? Blocks.NETHER_WART_BLOCK.defaultBlockState()
                    : r.nextInt(40) == 0 ? Blocks.SHROOMLIGHT.defaultBlockState() : flesh, 2);
        }
        String[] props = {"skull", "ribs", "bone_pile", "hand", "skull_side", "spine", "ribcage"};
        for (int i = 0; i < 14; i++) {
            int x = r.nextInt(11) - 5, z = r.nextInt(15) - 7;
            if (Math.abs(x) < 2 && Math.abs(z) < 2) continue;
            Block b = ModProps.get(props[r.nextInt(props.length)]);
            if (b != null) l.setBlock(o.offset(x, 0, z), b.defaultBlockState(), 2);
        }
        Block pile = ModProps.get("bone_pile");
        if (pile != null) l.setBlock(o.offset(0, 0, -1), pile.defaultBlockState(), 2);
        return new Vec3(o.getX() + 0.5, o.getY(), o.getZ() + 0.5);
    }

    /** grey water in the rain, and a fishing boat */
    public static Vec3 rescue(ServerLevel l) {
        BlockPos o = new BlockPos(6000, 200, 6000);
        box(l, o, -22, -5, -22, 22, -5, 22, Blocks.BARRIER.defaultBlockState());
        box(l, o, -22, -4, -22, 22, -1, 22, Blocks.WATER.defaultBlockState());
        for (int i = -22; i <= 22; i++) for (int y = -4; y <= 1; y++) {
            set(l, o, i, y, -22, Blocks.BARRIER.defaultBlockState());
            set(l, o, i, y, 22, Blocks.BARRIER.defaultBlockState());
            set(l, o, -22, y, i, Blocks.BARRIER.defaultBlockState());
            set(l, o, 22, y, i, Blocks.BARRIER.defaultBlockState());
        }
        // the hull and the deck
        box(l, o, -1, -2, -4, 1, -1, 4, Blocks.SPRUCE_PLANKS.defaultBlockState());
        box(l, o, -1, 0, -4, 1, 0, 4, Blocks.SPRUCE_PLANKS.defaultBlockState());
        for (int z = -4; z <= 4; z++) {
            set(l, o, -2, 0, z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
            set(l, o, 2, 0, z, Blocks.DARK_OAK_PLANKS.defaultBlockState());
        }
        box(l, o, -1, 1, -4, 1, 2, -3, Blocks.WHITE_TERRACOTTA.defaultBlockState());
        set(l, o, 0, 2, -3, Blocks.GLASS_PANE.defaultBlockState());
        box(l, o, -1, 3, -4, 1, 3, -3, Blocks.SPRUCE_SLAB.defaultBlockState());
        set(l, o, -1, 1, 2, Blocks.SPRUCE_TRAPDOOR.defaultBlockState());
        set(l, o, 0, 1, 4, Blocks.CHAIN.defaultBlockState());
        set(l, o, 0, 2, 4, Blocks.LANTERN.defaultBlockState());
        l.setWeatherParameters(0, 12000, true, true);
        l.setDayTime(5500);
        return new Vec3(o.getX() + 0.5, o.getY(), o.getZ() + 0.5);
    }
}
