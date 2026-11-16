package com.krolasyon.valdoria.world.mistfall;

import com.krolasyon.valdoria.ValdoriaMod;
import com.krolasyon.valdoria.block.MistfallPortalBlock;
import com.krolasyon.valdoria.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

/**
 * Mistfall portals: a rectangle of cascade stone (inside 2x3 up to 21x21, standing upright) opened with a cascade key.
 * Standing in the curtain for a few seconds carries a player between the Overworld and the Mistfall Vale; a portal
 * is found near the matching spot on the other side, or built there.
 */
public final class MistfallPortal {
    private MistfallPortal() {}

    public static final ResourceKey<Level> MISTFALL = ResourceKey.create(Registries.DIMENSION, ValdoriaMod.id("mistfall"));
    private static final int MAX = 21;

    // ------------------------------------------------------------------ frame detection

    private static boolean frame(Level l, BlockPos p) {
        return l.getBlockState(p).is(ModBlocks.CASCADE_STONE.get());
    }

    private static boolean empty(Level l, BlockPos p) {
        BlockState s = l.getBlockState(p);
        return s.isAir() || s.is(BlockTags.FIRE) || (s.canBeReplaced() && s.getFluidState().isEmpty() && !s.is(ModBlocks.MISTFALL_PORTAL.get()));
    }

    /** Tries to open a portal whose inside contains {@code inside}. */
    public static boolean tryOpen(Level level, BlockPos inside) {
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            if (openAlong(level, inside, axis)) return true;
        }
        return false;
    }

    private static boolean openAlong(Level l, BlockPos inside, Direction.Axis axis) {
        if (!empty(l, inside)) return false;
        Direction right = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction left = right.getOpposite();
        BlockPos p = inside;
        int n = 0;
        while (empty(l, p.below()) && n++ < MAX) p = p.below();
        if (!frame(l, p.below())) return false;
        n = 0;
        while (empty(l, p.relative(left)) && n++ < MAX) p = p.relative(left);
        if (!frame(l, p.relative(left))) return false;
        BlockPos corner = p;
        int w = 0;
        while (w <= MAX && empty(l, corner.relative(right, w))) w++;
        if (w < 2 || w > MAX || !frame(l, corner.relative(right, w))) return false;
        int h = 0;
        rows:
        while (h <= MAX) {
            for (int i = 0; i < w; i++) if (!empty(l, corner.relative(right, i).above(h))) break rows;
            if (!frame(l, corner.relative(left).above(h)) || !frame(l, corner.relative(right, w).above(h))) return false;
            h++;
        }
        if (h < 3 || h > MAX) return false;
        for (int i = 0; i < w; i++) {
            if (!frame(l, corner.relative(right, i).below()) || !frame(l, corner.relative(right, i).above(h))) return false;
        }
        BlockState portal = ModBlocks.MISTFALL_PORTAL.get().defaultBlockState().setValue(MistfallPortalBlock.AXIS, axis);
        for (int y = 0; y < h; y++)
            for (int i = 0; i < w; i++)
                l.setBlock(corner.relative(right, i).above(y), portal, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        return true;
    }

    // ------------------------------------------------------------------ travel

    public static void playerInside(ServerPlayer p) {
        CompoundTag d = p.getPersistentData();
        long now = p.level().getGameTime();
        if (now < d.getLong("valdoria_portal_cd")) return;
        long last = d.getLong("valdoria_portal_last");
        int t = now - last <= 2 ? d.getInt("valdoria_portal_t") + 1 : 1;
        d.putLong("valdoria_portal_last", now);
        d.putInt("valdoria_portal_t", t);
        if (t == 1) p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.6F);
        if (t >= (p.isCreative() ? 10 : 80)) {
            d.putInt("valdoria_portal_t", 0);
            d.putLong("valdoria_portal_cd", now + 60);
            travel(p);
        }
    }

    public static void travel(ServerPlayer p) {
        ServerLevel from = p.serverLevel();
        boolean leaving = from.dimension() == MISTFALL;
        ServerLevel dest = p.server.getLevel(leaving ? Level.OVERWORLD : MISTFALL);
        if (dest == null) {
            ValdoriaMod.LOG.error("Mistfall dimension is missing");
            return;
        }
        CompoundTag d = p.getPersistentData();
        BlockPos approx;
        if (leaving && d.contains("valdoria_return")) {
            approx = BlockPos.of(d.getLong("valdoria_return"));
        } else {
            approx = p.blockPosition();
            if (!leaving) d.putLong("valdoria_return", p.blockPosition().asLong());
        }
        BlockPos portal = findPortal(dest, approx);
        if (portal == null) portal = buildPortal(dest, approx, leaving);
        BlockState ps = dest.getBlockState(portal);
        Direction.Axis axis = ps.hasProperty(MistfallPortalBlock.AXIS) ? ps.getValue(MistfallPortalBlock.AXIS) : Direction.Axis.X;
        // arrive standing just in front of the curtain, facing away from it
        Vec3 arrive = axis == Direction.Axis.X ? new Vec3(portal.getX() + 0.5, portal.getY(), portal.getZ() + 1.6)
                : new Vec3(portal.getX() + 1.6, portal.getY(), portal.getZ() + 0.5);
        float yaw = axis == Direction.Axis.X ? 0 : -90;
        p.changeDimension(dest, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity entity, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(arrive, Vec3.ZERO, yaw, 0);
            }

            @Override
            public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld) {
                return false;
            }
        });
        dest.playSound(null, BlockPos.containing(arrive), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.7F);
        ValdoriaMod.LOG.info("[PORTAL] {} -> {} at {}", from.dimension().location(), dest.dimension().location(), BlockPos.containing(arrive));
    }

    /** The lowest portal block near the given spot, or null. */
    private static BlockPos findPortal(ServerLevel l, BlockPos near) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        Block portal = ModBlocks.MISTFALL_PORTAL.get();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -24; dx <= 24; dx++)
            for (int dz = -24; dz <= 24; dz++) {
                int top = l.getHeight(Heightmap.Types.MOTION_BLOCKING, near.getX() + dx, near.getZ() + dz);
                for (int y = Math.max(l.getMinBuildHeight(), Math.min(near.getY(), top) - 32); y <= Math.min(l.getMaxBuildHeight() - 1, Math.max(near.getY(), top) + 8); y++) {
                    m.set(near.getX() + dx, y, near.getZ() + dz);
                    if (l.getBlockState(m).is(portal) && !l.getBlockState(m.below()).is(portal)) {
                        double dist = m.distSqr(near);
                        if (dist < bd) { bd = dist; best = m.immutable(); }
                    }
                }
            }
        return best;
    }

    /** Builds a 2x3 portal on solid, dry ground near the spot and returns its bottom-left curtain block. */
    private static BlockPos buildPortal(ServerLevel l, BlockPos near, boolean toOverworld) {
        BlockPos base = null;
        search:
        for (int r = 0; r <= 48; r += 3)
            for (int dx = -r; dx <= r; dx += 3)
                for (int dz = -r; dz <= r; dz += 3) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int x = near.getX() + dx, z = near.getZ() + dz;
                    int y = l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockState below = l.getBlockState(new BlockPos(x, y - 1, z));
                    if (below.getFluidState().isEmpty() && below.isSolid() && y > l.getMinBuildHeight() + 5 && y < l.getMaxBuildHeight() - 10) {
                        base = new BlockPos(x, y, z);
                        break search;
                    }
                }
        if (base == null) base = new BlockPos(near.getX(), Math.max(near.getY(), 80), near.getZ());
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState floor = (toOverworld ? ModBlocks.CASCADE_STONE : ModBlocks.MISTROCK_BRICKS).get().defaultBlockState();
        BlockState frame = ModBlocks.CASCADE_STONE.get().defaultBlockState();
        for (int x = -2; x <= 3; x++)
            for (int z = -2; z <= 2; z++) {
                l.setBlock(base.offset(x, -1, z), floor, Block.UPDATE_ALL);
                for (int y = 0; y <= 4; y++) l.setBlock(base.offset(x, y, z), air, Block.UPDATE_ALL);
            }
        for (int x = -1; x <= 2; x++) {
            l.setBlock(base.offset(x, -1, 0), frame, Block.UPDATE_ALL);
            l.setBlock(base.offset(x, 3, 0), frame, Block.UPDATE_ALL);
        }
        for (int y = 0; y <= 2; y++) {
            l.setBlock(base.offset(-1, y, 0), frame, Block.UPDATE_ALL);
            l.setBlock(base.offset(2, y, 0), frame, Block.UPDATE_ALL);
        }
        BlockState portal = ModBlocks.MISTFALL_PORTAL.get().defaultBlockState().setValue(MistfallPortalBlock.AXIS, Direction.Axis.X);
        for (int x = 0; x <= 1; x++)
            for (int y = 0; y <= 2; y++)
                l.setBlock(base.offset(x, y, 0), portal, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        return base;
    }
}
