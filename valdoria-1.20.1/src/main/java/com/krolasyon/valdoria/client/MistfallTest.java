package com.krolasyon.valdoria.client;

import com.krolasyon.valdoria.ValdoriaMod;
import com.krolasyon.valdoria.registry.ModBlocks;
import com.krolasyon.valdoria.world.mistfall.MistfallChunkGenerator;
import com.krolasyon.valdoria.world.mistfall.MistfallPortal;
import com.krolasyon.valdoria.world.mistfall.MistfallTerrain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Development only (-Dvaldoria.autotest=mistfall): builds and opens a portal next to spawn, walks through it, then
 * photographs the Mistfall Vale from viewpoints picked around a real waterfall, and walks back home.
 */
final class MistfallTest {
    private MistfallTest() {}

    private static int stage, wait;
    private static final List<Runnable> steps = new ArrayList<>();
    private static BlockPos home, fallPos;
    private static int fallTop, fallBottom, dirX, dirZ;

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (wait > 0) { wait--; return; }
        if (steps.isEmpty()) plan();
        if (stage >= steps.size()) {
            ValdoriaMod.LOG.info("[AUTOTEST] finished");
            stage = -999;
            mc.stop();
            return;
        }
        if (stage < 0) return;
        steps.get(stage++).run();
    }

    private static MinecraftServer server() { return Minecraft.getInstance().getSingleplayerServer(); }
    private static ServerPlayer sp() { return server().getPlayerList().getPlayers().get(0); }
    private static void onServer(Runnable r) { server().execute(r); }
    private static void shot(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), c -> {});
        ValdoriaMod.LOG.info("[AUTOTEST] shot {}", name);
    }
    private static void step(int waitAfter, Runnable r) { steps.add(() -> { r.run(); wait = waitAfter; }); }
    private static void cam(double x, double y, double z, float yaw, float pitch) {
        onServer(() -> sp().teleportTo(sp().serverLevel(), x, y, z, yaw, pitch));
    }

    private static void plan() {
        // 1. build a cascade stone frame next to spawn and open it with the key logic
        step(40, () -> onServer(() -> {
            ServerPlayer p = sp();
            p.setGameMode(GameType.CREATIVE);
            ServerLevel l = p.serverLevel();
            BlockPos base = l.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.blockPosition().offset(0, 0, -5));
            home = base;
            BlockState frame = ModBlocks.CASCADE_STONE.get().defaultBlockState();
            for (int x = -1; x <= 2; x++) for (int y = -1; y <= 3; y++) l.setBlock(base.offset(x, y, 0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            for (int x = -1; x <= 2; x++) { l.setBlock(base.offset(x, -1, 0), frame, Block.UPDATE_ALL); l.setBlock(base.offset(x, 3, 0), frame, Block.UPDATE_ALL); }
            for (int y = 0; y <= 2; y++) { l.setBlock(base.offset(-1, y, 0), frame, Block.UPDATE_ALL); l.setBlock(base.offset(2, y, 0), frame, Block.UPDATE_ALL); }
            boolean ok = MistfallPortal.tryOpen(l, base.offset(1, 1, 0));
            ValdoriaMod.LOG.info("[AUTOTEST] portal opened in the overworld: {}", ok);
            server().getCommands().performPrefixedCommand(server().createCommandSourceStack().withSuppressedOutput(), "time set 1000");
        }));
        step(60, () -> cam(home.getX() + 1.0, home.getY(), home.getZ() + 4.5, 180, 5));
        step(5, () -> shot("mf_portal_overworld"));
        // 2. step into the curtain
        step(120, () -> cam(home.getX() + 1.0, home.getY(), home.getZ() + 0.5, 180, 0));
        step(200, () -> ValdoriaMod.LOG.info("[AUTOTEST] now in {}", Minecraft.getInstance().level.dimension().location()));
        step(5, () -> {
            onServer(() -> {
                ServerPlayer p = sp();
                p.teleportTo(p.serverLevel(), p.getX(), p.getY(), p.getZ(), 180, 5);
            });
        });
        step(40, () -> {});
        step(5, () -> shot("mf_arrival"));
        // 3. find a tall waterfall pouring into a canyon near the arrival point
        step(10, () -> onServer(() -> {
            ServerPlayer p = sp();
            ServerLevel l = p.serverLevel();
            if (!(l.getChunkSource().getGenerator() instanceof MistfallChunkGenerator gen)) {
                ValdoriaMod.LOG.error("[AUTOTEST] not in the Mistfall Vale");
                return;
            }
            MistfallTerrain t = gen.terrain(l.getChunkSource().randomState());
            BlockPos c = p.blockPosition();
            int best = 0;
            for (int x = c.getX() - 700; x <= c.getX() + 700; x += 2)
                for (int z = c.getZ() - 700; z <= c.getZ() + 700; z += 2) {
                    if (t.canyonFactor(x, z) < 0.6) continue;
                    int f = t.fallTop(x, z);
                    if (f < 0) continue;
                    int bottom = Math.max(t.solidTop(x, z), t.waterTop(x, z));
                    int h = f - bottom;
                    if (h > best) {
                        best = h; fallPos = new BlockPos(x, bottom, z); fallTop = f; fallBottom = bottom;
                        dirX = 0; dirZ = 0;
                        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                        for (int[] d : dirs) if (t.isRiver(x + d[0], z + d[1]) && t.waterTop(x + d[0], z + d[1]) == f) { dirX = d[0]; dirZ = d[1]; }
                    }
                }
            ValdoriaMod.LOG.info("[AUTOTEST] tallest waterfall: {} from y={} down to y={} (river comes from {},{})", fallPos, fallTop, fallBottom, dirX, dirZ);
            p.setGameMode(GameType.SPECTATOR);
        }));
        // 4. viewpoints around it: facing the falls from across the canyon, from above, from the pool, from the plateau
        step(5, () -> {
            if (fallPos == null) return;
            double ax = fallPos.getX() + 0.5 - dirX * 34, az = fallPos.getZ() + 0.5 - dirZ * 34;
            cam(ax, (fallTop + fallBottom) / 2.0 + 6, az, look(dirX, dirZ), 6);
        });
        step(260, () -> {});
        step(5, () -> shot("mf_waterfall"));
        step(5, () -> {
            if (fallPos == null) return;
            cam(fallPos.getX() + 0.5 - dirX * 55 + dirZ * 8, fallTop + 34, fallPos.getZ() + 0.5 - dirZ * 55 + dirX * 8, look(dirX, dirZ) + 6, 24);
        });
        step(220, () -> {});
        step(5, () -> shot("mf_aerial"));
        step(5, () -> {
            if (fallPos == null) return;
            cam(fallPos.getX() + 0.5 - dirX * 14, fallBottom + 2.5, fallPos.getZ() + 0.5 - dirZ * 14, look(dirX, dirZ), -28);
        });
        step(160, () -> {});
        step(5, () -> shot("mf_pool"));
        step(5, () -> {
            if (fallPos == null) return;
            cam(fallPos.getX() + 0.5 + dirX * 30, fallTop + 3, fallPos.getZ() + 0.5 + dirZ * 30, look(-dirX, -dirZ), 6);
        });
        step(220, () -> {});
        step(5, () -> shot("mf_plateau"));
        step(5, () -> {
            if (fallPos == null) return;
            cam(fallPos.getX() + 0.5 + dirZ * 60, fallTop + 70, fallPos.getZ() + 0.5 + dirX * 60, look(-dirZ, -dirX), 30);
        });
        step(240, () -> {});
        step(5, () -> shot("mf_overview"));
        // 5. back home through the return portal
        step(5, () -> onServer(() -> sp().setGameMode(GameType.CREATIVE)));
        step(5, () -> onServer(() -> {
            ServerPlayer p = sp();
            BlockPos r = BlockPos.of(p.getPersistentData().getLong("valdoria_return"));
            ValdoriaMod.LOG.info("[AUTOTEST] return marker {}", r);
            MistfallPortal.travel(p);
        }));
        step(120, () -> ValdoriaMod.LOG.info("[AUTOTEST] back in {}", Minecraft.getInstance().level.dimension().location()));
    }

    /** Minecraft yaw that looks along the horizontal vector (vx, vz). */
    private static float look(double vx, double vz) {
        return (float) Math.toDegrees(Math.atan2(-vx, vz));
    }
}
