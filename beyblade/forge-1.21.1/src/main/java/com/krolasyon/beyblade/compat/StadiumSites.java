package com.krolasyon.beyblade.compat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/** Where world generated stadiums were built, so they keep their distance (Forge 1.21.1). */
public class StadiumSites extends SavedData {
   private final List<long[]> list = new ArrayList<>();

   public static StadiumSites get(ServerLevel level) {
      return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(StadiumSites::new, (t, r) -> load(t), null), "librabeyblade_stadiums");
   }

   static StadiumSites load(CompoundTag tag) {
      StadiumSites s = new StadiumSites();
      for (Tag t : tag.getList("Sites", 4)) {
         BlockPos p = BlockPos.of(((LongTag)t).getAsLong());
         s.list.add(new long[]{(long)p.getX(), (long)p.getZ()});
      }
      return s;
   }

   public boolean near(int x, int z) {
      for (long[] p : this.list) {
         if ((p[0] - (long)x) * (p[0] - (long)x) + (p[1] - (long)z) * (p[1] - (long)z) < 360000L) {
            return true;
         }
      }
      return false;
   }

   public void add(int x, int z) {
      this.list.add(new long[]{(long)x, (long)z});
      this.setDirty();
   }

   @Override
   public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider reg) {
      ListTag l = new ListTag();
      for (long[] p : this.list) {
         l.add(LongTag.valueOf(new BlockPos((int)p[0], 0, (int)p[1]).asLong()));
      }
      tag.put("Sites", l);
      return tag;
   }
}
