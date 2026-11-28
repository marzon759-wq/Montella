package com.krolasyon.beyblade.world;

import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.entity.StadiumEntity;
import com.krolasyon.beyblade.registry.ModEntities;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.level.ChunkEvent.Load;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.slf4j.Logger;

@EventBusSubscriber(
   modid = "librabeyblade",
   bus = Bus.FORGE
)
public final class StadiumWorldGen {
   private static final Logger LOG = LogUtils.getLogger();
   public static final int CHANCE = 260;
   public static final int SPACING = 600;
   public static final int RADIUS = 26;
   private static final ConcurrentLinkedQueue<long[]> QUEUE = new ConcurrentLinkedQueue<>();
   private static final ResourceLocation CHEST_LOOT = V.rl("librabeyblade", "chests/stadium");

   private StadiumWorldGen() {
   }

   @SubscribeEvent
   public static void onChunkLoad(Load event) {
      if (event.isNewChunk() && event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
         ChunkPos cp = event.getChunk().getPos();
         RandomSource r = RandomSource.create(level.getSeed() ^ (long)cp.x * 341873128712L + (long)cp.z * 132897987541L);
         if (r.nextInt(260) == 0) {
            if (Math.abs(cp.x) >= 4 || Math.abs(cp.z) >= 4) {
               QUEUE.add(new long[]{(long)cp.getMiddleBlockX(), (long)cp.getMiddleBlockZ(), 0L});
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLevelTick(LevelTickEvent event) {
      if (event.phase == Phase.END && event.level instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
         if (!QUEUE.isEmpty() && level.getGameTime() % 20L == 0L) {
            Iterator<long[]> it = QUEUE.iterator();

            while (it.hasNext()) {
               long[] q = it.next();
               int x = (int)q[0];
               int z = (int)q[1];
               if (++q[2] > 60L) {
                  it.remove();
               } else if (level.hasChunksAt(x - 26 - 2, z - 26 - 2, x + 26 + 2, z + 26 + 2)) {
                  it.remove();
                  com.krolasyon.beyblade.compat.StadiumSites sites = com.krolasyon.beyblade.compat.StadiumSites.get(level);
                  if (!sites.near(x, z)) {
                     if (build(level, x, z)) {
                        sites.add(x, z);
                        LOG.info("[librabeyblade] a beyblade stadium appeared at {} {}", x, z);
                     }
                     break;
                  }
               }
            }
         }
      }
   }

   private static boolean build(ServerLevel level, int x, int z) {
      int y = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      if (y > level.getSeaLevel() && y <= level.getMaxBuildHeight() - 20) {
         int lo = y;
         int hi = y;

         for (int dx = -26; dx <= 26; dx += 3) {
            for (int dz = -26; dz <= 26; dz += 3) {
               if (dx * dx + dz * dz <= 676) {
                  int h = level.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                  if (!level.getFluidState(new BlockPos(x + dx, h - 1, z + dz)).isEmpty()) {
                     return false;
                  }

                  lo = Math.min(lo, h);
                  hi = Math.max(hi, h);
               }
            }
         }

         if (hi - lo > 6) {
            return false;
         } else {
            BlockState top = level.getBlockState(new BlockPos(x, y - 1, z));
            if (!top.is(BlockTags.DIRT) && !top.is(Blocks.SAND) && !top.is(Blocks.SNOW_BLOCK) && !top.is(Blocks.GRASS_BLOCK)) {
               top = Blocks.GRASS_BLOCK.defaultBlockState();
            }

            BlockState fill = top.is(Blocks.SAND) ? Blocks.SANDSTONE.defaultBlockState() : Blocks.DIRT.defaultBlockState();

            for (int dx = -26; dx <= 26; dx++) {
               for (int dzx = -26; dzx <= 26; dzx++) {
                  double d = Math.sqrt((double)(dx * dx + dzx * dzx));
                  if (!(d > 26.5)) {
                     for (int dy = 0; dy < 14; dy++) {
                        BlockPos p = new BlockPos(x + dx, y + dy, z + dzx);
                        if (!level.getBlockState(p).isAir()) {
                           level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                     }

                     for (int dyx = 1; dyx <= 6; dyx++) {
                        BlockPos p = new BlockPos(x + dx, y - dyx, z + dzx);
                        BlockState s = level.getBlockState(p);
                        if (dyx == 1) {
                           level.setBlock(p, top, 2);
                        } else if (s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty()) {
                           level.setBlock(p, fill, 2);
                        }
                     }
                  }
               }
            }

            for (int side = 0; side < 2; side++) {
               int pz = z + (side == 0 ? 23 : -24);

               for (int dx = -1; dx <= 1; dx++) {
                  for (int dzxx = 0; dzxx <= 1; dzxx++) {
                     level.setBlock(new BlockPos(x + dx, y - 1, pz + dzxx), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                  }
               }
            }

            StadiumEntity st = (StadiumEntity)((EntityType)ModEntities.STADIUM.get()).create(level);
            if (st == null) {
               return false;
            } else {
               st.moveTo((double)x, (double)y, (double)z, 0.0F, 0.0F);
               level.addFreshEntity(st);
               BlockPos chest = new BlockPos(x + 3, y, z + 23);
               level.setBlock(chest, (BlockState)Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH), 2);
               if (level.getBlockEntity(chest) instanceof ChestBlockEntity cbe) {
                  V.chestLoot(cbe, CHEST_LOOT, level.random.nextLong());
               }

               return true;
            }
         }
      } else {
         return false;
      }
   }
}
