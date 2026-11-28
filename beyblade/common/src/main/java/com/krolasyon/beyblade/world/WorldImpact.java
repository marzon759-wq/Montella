package com.krolasyon.beyblade.world;

import com.krolasyon.beyblade.entity.BeybladeEntity;
import com.krolasyon.beyblade.entity.Px;
import com.krolasyon.beyblade.registry.ModBlocks;
import com.krolasyon.beyblade.registry.ModSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * What special moves do to the world around the stadium, anime style: craters, tornadoes that tear up grass and
 * leaves and carry mobs and items, shock waves that flatten plants and shatter glass, magma and sand fields.
 *
 * Every block change is temporary: the original block comes back after about half a minute (outer ring first),
 * and everything is put back when the world is closed. Containers, block entities and unbreakable blocks are never
 * touched. /gamerule beybladeWorldDamage false turns the block changes off (entity effects stay).
 */
@EventBusSubscriber(modid = "librabeyblade")
public final class WorldImpact {
   public static final GameRules.Key<GameRules.BooleanValue> WORLD_DAMAGE = GameRules.register(
      "beybladeWorldDamage", GameRules.Category.MISC, GameRules.BooleanValue.create(true)
   );
   private static final int RESTORE = 600;
   private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

   private record Change(BlockState old, BlockState now, long restoreAt) {
   }

   private static final Map<ResourceKey<Level>, LinkedHashMap<BlockPos, Change>> CHANGES = new HashMap<>();

   private WorldImpact() {
   }

   public static void init() {
      // class load registers the gamerule
   }

   public enum Style {
      EARTH,
      FIRE,
      SAND,
      STAR,
      STORM
   }

   // ------------------------------------------------------------------ temporary blocks
   private static boolean allowed(ServerLevel l) {
      return l.getGameRules().getBoolean(WORLD_DAMAGE);
   }

   private static boolean editable(ServerLevel l, BlockPos p, BlockState s) {
      if (s.hasBlockEntity() || s.getDestroySpeed(l, p) < 0.0F || s.is(ModBlocks.STADIUM_BARRIER.get())) {
         return false;
      }
      return !s.is(Blocks.WATER) && !s.is(Blocks.LAVA);
   }

   /** Replaces a block until the restore time. Returns false when the block may not be changed. */
   public static boolean setTemp(ServerLevel l, BlockPos p, BlockState to, int ticks) {
      if (!allowed(l) || !l.isLoaded(p)) {
         return false;
      }
      BlockState s = l.getBlockState(p);
      if (s == to || !editable(l, p, s)) {
         return false;
      }
      LinkedHashMap<BlockPos, Change> m = CHANGES.computeIfAbsent(l.dimension(), k -> new LinkedHashMap<>());
      BlockPos key = p.immutable();
      Change prev = m.get(key);
      BlockState orig = prev != null ? prev.old() : s;
      // a gravity block resting on this one would fall into the hole: lift it out too
      BlockPos up = key.above();
      if (to.isAir() && l.getBlockState(up).getBlock() instanceof FallingBlock) {
         setTemp(l, up, Blocks.AIR.defaultBlockState(), ticks + 5);
      }
      l.setBlock(key, to, FLAGS);
      m.put(key, new Change(orig, to, l.getGameTime() + (long)ticks));
      return true;
   }

   private static void restoreOne(ServerLevel l, BlockPos p, Change c, boolean fx) {
      BlockState cur = l.getBlockState(p);
      if (cur == c.now() || cur.isAir() || cur.is(Blocks.FIRE)) {
         l.setBlock(p, c.old(), FLAGS | Block.UPDATE_NEIGHBORS);
         if (fx && !c.old().isAir()) {
            Px.send(l, new BlockParticleOption(ParticleTypes.BLOCK, c.old()), p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.05);
         }
      }
   }

   @SubscribeEvent
   public static void onLevelTick(TickEvent.LevelTickEvent e) {
      if (e.phase != TickEvent.Phase.END || !(e.level instanceof ServerLevel l)) {
         return;
      }
      LinkedHashMap<BlockPos, Change> m = CHANGES.get(l.dimension());
      if (m == null || m.isEmpty()) {
         return;
      }
      long now = l.getGameTime();
      int budget = 64;
      Iterator<Map.Entry<BlockPos, Change>> it = m.entrySet().iterator();
      while (it.hasNext() && budget > 0) {
         Map.Entry<BlockPos, Change> en = it.next();
         if (en.getValue().restoreAt() <= now) {
            restoreOne(l, en.getKey(), en.getValue(), true);
            it.remove();
            budget--;
         }
      }
   }

   private static void restoreAll(ServerLevel l) {
      LinkedHashMap<BlockPos, Change> m = CHANGES.remove(l.dimension());
      if (m != null) {
         // reverse order: the deepest / latest changes go back first
         List<Map.Entry<BlockPos, Change>> list = new ArrayList<>(m.entrySet());
         for (int i = list.size() - 1; i >= 0; i--) {
            restoreOne(l, list.get(i).getKey(), list.get(i).getValue(), false);
         }
      }
   }

   @SubscribeEvent
   public static void onUnload(LevelEvent.Unload e) {
      if (e.getLevel() instanceof ServerLevel l) {
         restoreAll(l);
      }
   }

   @SubscribeEvent
   public static void onStopping(ServerStoppingEvent e) {
      for (ServerLevel l : e.getServer().getAllLevels()) {
         restoreAll(l);
      }
   }

   // ------------------------------------------------------------------ helpers
   /** The solid ground block under (or at) this point. */
   @Nullable
   public static BlockPos ground(ServerLevel l, double x, double y, double z) {
      BlockPos p = BlockPos.containing(x, y + 0.5, z);
      for (int i = 0; i < 6; i++) {
         BlockState s = l.getBlockState(p);
         if (!s.isAir() && s.isSolidRender(l, p)) {
            return p;
         }
         p = p.below();
      }
      return null;
   }

   private static void sound(ServerLevel l, Vec3 c, SoundEvent s, float vol, float pitch) {
      l.playSound(null, c.x, c.y, c.z, s, SoundSource.NEUTRAL, vol, pitch);
   }

   private static boolean isPlant(BlockState s) {
      if (s.isAir() || !s.getFluidState().isEmpty() || s.is(Blocks.FIRE) || s.is(Blocks.SOUL_FIRE)) {
         return false;
      }
      return s.is(BlockTags.REPLACEABLE) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(BlockTags.REPLACEABLE_BY_TREES) || s.is(BlockTags.CROPS);
   }

   // ------------------------------------------------------------------ effects
   /**
    * A crater: the ground is blown out in a bowl, the rim is scorched in the move's style and the dirt is thrown
    * into the air. {@code radius} in blocks, {@code depth} at the centre.
    */
   public static void crater(ServerLevel l, Vec3 c, double radius, double depth, Style style) {
      BlockPos g = ground(l, c.x, c.y, c.z);
      sound(l, c, ModSounds.CRATER.get(), 3.0F, 0.8F + l.random.nextFloat() * 0.2F);
      sound(l, c, ModSounds.DEBRIS.get(), 2.0F, 1.0F);
      Px.send(l, ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 0.3, c.z, 1, 0.0, 0.0, 0.0, 0.0);
      Px.send(l, ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + 0.3, c.z, 20, radius * 0.4, 0.3, radius * 0.4, 0.02);
      if (g == null) {
         return;
      }
      int r = Mth.ceil(radius + 1.0);
      for (int dx = -r; dx <= r; dx++) {
         for (int dz = -r; dz <= r; dz++) {
            double d = Math.sqrt((double)(dx * dx + dz * dz));
            if (d > radius + 1.0) {
               continue;
            }
            BlockPos col = g.offset(dx, 0, dz);
            // follow the terrain a little: find this column's surface near the centre's height
            BlockPos top = surface(l, col);
            if (top == null) {
               continue;
            }
            BlockState topState = l.getBlockState(top);
            int delay = RESTORE + (int)((radius + 1.0 - d) * 12.0) + l.random.nextInt(40);
            if (d <= radius) {
               int h = (int)Math.round(depth * (1.0 - (d / radius) * (d / radius)) + 0.35);
               for (int k = 0; k < h; k++) {
                  BlockPos p = top.below(k);
                  BlockState s = l.getBlockState(p);
                  if (setTemp(l, p, Blocks.AIR.defaultBlockState(), delay + k * 4) && k == 0 && l.random.nextInt(3) == 0) {
                     Px.send(l, new BlockParticleOption(ParticleTypes.BLOCK, s), p.getX() + 0.5, p.getY() + 0.8, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.6);
                  }
               }
               // the plant on top goes with it
               BlockPos above = top.above();
               if (isPlant(l.getBlockState(above))) {
                  setTemp(l, above, Blocks.AIR.defaultBlockState(), delay);
               }
               BlockPos floor = top.below(Math.max(0, h));
               if (h > 0 && style != Style.EARTH) {
                  setTemp(l, floor, floorBlock(style, l), delay + 8);
               }
            } else {
               // scorched rim
               if (l.random.nextFloat() < 0.75F) {
                  setTemp(l, top, rimBlock(style, l, topState), delay);
               }
               BlockPos above = top.above();
               if (isPlant(l.getBlockState(above))) {
                  setTemp(l, above, Blocks.AIR.defaultBlockState(), delay);
               }
            }
         }
      }
      // dirt thrown into the air
      BlockState dirt = l.getBlockState(g);
      if (!dirt.isAir()) {
         Px.send(l, new BlockParticleOption(ParticleTypes.BLOCK, dirt), c.x, c.y + 0.5, c.z, (int)(60 * radius), radius * 0.5, 0.5, radius * 0.5, 0.9);
         Px.send(l, new BlockParticleOption(ParticleTypes.FALLING_DUST, dirt), c.x, c.y + 2.5, c.z, (int)(30 * radius), radius * 0.7, 1.5, radius * 0.7, 0.0);
      }
      switch (style) {
         case FIRE -> Px.send(l, ParticleTypes.LAVA, c.x, c.y + 0.3, c.z, 30, radius * 0.5, 0.2, radius * 0.5, 0.0);
         case STAR -> Px.send(l, ParticleTypes.END_ROD, c.x, c.y + 0.5, c.z, 80, radius * 0.4, 1.0, radius * 0.4, 0.3);
         case SAND -> Px.send(l, new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), c.x, c.y + 3.0, c.z, 120, radius, 2.5, radius, 0.0);
         case STORM -> Px.send(l, ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 0.5, c.z, 60, radius * 0.5, 0.5, radius * 0.5, 0.6);
         default -> {
         }
      }
   }

   @Nullable
   private static BlockPos surface(ServerLevel l, BlockPos col) {
      BlockPos p = col.above(2);
      for (int i = 0; i < 6; i++) {
         BlockState s = l.getBlockState(p);
         if (!s.isAir() && !isPlant(s) && s.isSolidRender(l, p)) {
            return p;
         }
         p = p.below();
      }
      return null;
   }

   private static BlockState rimBlock(Style style, ServerLevel l, BlockState was) {
      return switch (style) {
         case FIRE -> l.random.nextInt(3) == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
         case SAND -> Blocks.SANDSTONE.defaultBlockState();
         case STAR -> l.random.nextInt(9) == 0 ? Blocks.AMETHYST_BLOCK.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
         case STORM -> l.random.nextInt(3) == 0 ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.COARSE_DIRT.defaultBlockState();
         default -> was.is(BlockTags.DIRT) || was.is(Blocks.GRASS_BLOCK) ? Blocks.COARSE_DIRT.defaultBlockState()
            : (l.random.nextBoolean() ? Blocks.COBBLESTONE.defaultBlockState() : Blocks.GRAVEL.defaultBlockState());
      };
   }

   private static BlockState floorBlock(Style style, ServerLevel l) {
      return switch (style) {
         case FIRE -> Blocks.MAGMA_BLOCK.defaultBlockState();
         case SAND -> Blocks.SANDSTONE.defaultBlockState();
         case STAR -> Blocks.COARSE_DIRT.defaultBlockState();
         default -> Blocks.COARSE_DIRT.defaultBlockState();
      };
   }

   /**
    * One tick of a tornado: pulls and lifts mobs, items and other beys into a spiral, tears plants and leaves off
    * the ground around it. {@code outward} makes it a wall that throws everything away instead (Gale Force Wall).
    */
   public static void tornado(ServerLevel l, Entity caster, Vec3 c, double radius, double height, double strength, boolean outward, int t) {
      AABB box = new AABB(c.x - radius, c.y - 1.0, c.z - radius, c.x + radius, c.y + height, c.z + radius);
      Player owner = caster instanceof BeybladeEntity b ? b.getOwner() : null;
      for (Entity e : l.getEntities(caster, box, x -> x != owner && !(x instanceof Player p && (p.isSpectator() || p.isCreative())))) {
         Vec3 rel = new Vec3(e.getX() - c.x, 0.0, e.getZ() - c.z);
         double d = rel.length();
         if (d > radius) {
            continue;
         }
         Vec3 dir = d < 0.01 ? new Vec3(1.0, 0.0, 0.0) : rel.scale(1.0 / d);
         Vec3 tan = new Vec3(-dir.z, 0.0, dir.x);
         double k = strength * (1.0 - d / radius * 0.6);
         Vec3 v;
         if (outward) {
            v = dir.scale(0.35 * k).add(tan.scale(0.2 * k)).add(0.0, 0.18 * k, 0.0);
         } else {
            double lift = e.getY() < c.y + height * 0.8 ? 0.16 * k : -0.02;
            v = dir.scale(-0.1 * k).add(tan.scale(0.32 * k)).add(0.0, lift, 0.0);
         }
         if (e instanceof BeybladeEntity) {
            v = v.scale(0.5);
         }
         e.setDeltaMovement(e.getDeltaMovement().scale(0.7).add(v));
         e.hurtMarked = true;
         e.fallDistance = 0.0F;
         if (e instanceof ItemEntity) {
            e.setDeltaMovement(e.getDeltaMovement().add(0.0, 0.04, 0.0));
         }
      }
      // spiral of wind and dust
      for (int i = 0; i < 6; i++) {
         double a = t * 0.45 + i * Math.PI / 3.0;
         double y = (t * 0.7 + i * 1.3) % height;
         double rr = radius * (0.35 + 0.65 * y / height);
         Px.send(l, ParticleTypes.CLOUD, c.x + Math.cos(a) * rr, c.y + y, c.z + Math.sin(a) * rr, 1, 0.05, 0.05, 0.05, 0.02);
      }
      if (t % 2 == 0) {
         Px.send(l, ParticleTypes.POOF, c.x, c.y + 0.2, c.z, 4, radius * 0.5, 0.1, radius * 0.5, 0.05);
      }
      if (t % 20 == 0) {
         sound(l, c, ModSounds.TORNADO.get(), 2.2F, 0.9F + l.random.nextFloat() * 0.2F);
      }
      // tear up plants, leaves and snow
      if (t % 3 == 0) {
         for (int n = 0; n < 6; n++) {
            double a = l.random.nextDouble() * Math.PI * 2.0;
            double rr = l.random.nextDouble() * radius;
            BlockPos col = BlockPos.containing(c.x + Math.cos(a) * rr, c.y, c.z + Math.sin(a) * rr);
            for (int dy = 4; dy >= -2; dy--) {
               BlockPos p = col.above(dy);
               BlockState s = l.getBlockState(p);
               if (isPlant(s) || s.is(BlockTags.LEAVES)) {
                  if (setTemp(l, p, Blocks.AIR.defaultBlockState(), RESTORE / 2 + l.random.nextInt(200))) {
                     Px.send(l, new BlockParticleOption(ParticleTypes.BLOCK, s), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.4);
                  }
                  break;
               }
            }
         }
      }
   }

   /** A shock wave ring: plants flattened, glass shattered, everything pushed away. */
   public static void shockwave(ServerLevel l, Entity caster, Vec3 c, double radius) {
      sound(l, c, ModSounds.SHOCKWAVE.get(), 2.6F, 0.9F);
      int r = Mth.ceil(radius);
      for (int i = 0; i < 48; i++) {
         double a = i / 48.0 * Math.PI * 2.0;
         Px.send(l, ParticleTypes.SWEEP_ATTACK, c.x + Math.cos(a) * radius * 0.8, c.y + 0.3, c.z + Math.sin(a) * radius * 0.8, 1, 0.0, 0.0, 0.0, 0.0);
         Px.send(l, ParticleTypes.CLOUD, c.x + Math.cos(a) * radius, c.y + 0.2, c.z + Math.sin(a) * radius, 1, 0.0, 0.0, 0.0, 0.05);
      }
      for (int dx = -r; dx <= r; dx++) {
         for (int dz = -r; dz <= r; dz++) {
            if (dx * dx + dz * dz > r * r) {
               continue;
            }
            for (int dy = -2; dy <= 3; dy++) {
               BlockPos p = BlockPos.containing(c.x + dx, c.y + dy, c.z + dz);
               BlockState s = l.getBlockState(p);
               if (isPlant(s) || s.is(BlockTags.IMPERMEABLE) || s.is(Blocks.GLASS_PANE) || s.is(BlockTags.LEAVES) && dy >= 1) {
                  if (setTemp(l, p, Blocks.AIR.defaultBlockState(), RESTORE / 2 + l.random.nextInt(200))) {
                     Px.send(l, new BlockParticleOption(ParticleTypes.BLOCK, s), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.3);
                  }
               }
            }
         }
      }
      Player owner = caster instanceof BeybladeEntity b ? b.getOwner() : null;
      for (Entity e : l.getEntities(caster, new AABB(c, c).inflate(radius, 3.0, radius), x -> x != owner)) {
         Vec3 rel = new Vec3(e.getX() - c.x, 0.0, e.getZ() - c.z);
         double d = rel.length();
         if (d < radius && d > 0.01 && (e instanceof LivingEntity || e instanceof ItemEntity)) {
            e.setDeltaMovement(e.getDeltaMovement().add(rel.scale(0.6 / d * (1.0 - d / radius))).add(0.0, 0.25, 0.0));
            e.hurtMarked = true;
         }
      }
   }

   /** Plants and snow around a point are laid flat (blown away) for a while. */
   public static void flatten(ServerLevel l, Vec3 c, double radius) {
      int r = Mth.ceil(radius);
      for (int dx = -r; dx <= r; dx++) {
         for (int dz = -r; dz <= r; dz++) {
            if (dx * dx + dz * dz > radius * radius) {
               continue;
            }
            for (int dy = -1; dy <= 1; dy++) {
               BlockPos p = BlockPos.containing(c.x + dx, c.y + dy, c.z + dz);
               BlockState s = l.getBlockState(p);
               if (isPlant(s) && setTemp(l, p, Blocks.AIR.defaultBlockState(), RESTORE / 2 + l.random.nextInt(200))) {
                  Px.send(l, new BlockParticleOption(ParticleTypes.BLOCK, s), p.getX() + 0.5, p.getY() + 0.3, p.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.3);
               }
            }
         }
      }
   }

   /** Ground turns to the move's material around a point (magma for fire moves, sand for Inferno Blast). */
   public static void field(ServerLevel l, Vec3 c, double radius, Style style, int perTick) {
      for (int n = 0; n < perTick; n++) {
         double a = l.random.nextDouble() * Math.PI * 2.0;
         double rr = Math.sqrt(l.random.nextDouble()) * radius;
         BlockPos top = surface(l, BlockPos.containing(c.x + Math.cos(a) * rr, c.y, c.z + Math.sin(a) * rr));
         if (top == null) {
            continue;
         }
         BlockState to = switch (style) {
            case FIRE -> l.random.nextInt(3) == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.BLACKSTONE.defaultBlockState();
            case SAND -> Blocks.SAND.defaultBlockState();
            case STAR -> Blocks.CALCITE.defaultBlockState();
            default -> Blocks.COARSE_DIRT.defaultBlockState();
         };
         BlockPos above = top.above();
         if (isPlant(l.getBlockState(above))) {
            setTemp(l, above, Blocks.AIR.defaultBlockState(), RESTORE);
         }
         if (setTemp(l, top, to, RESTORE + l.random.nextInt(120))) {
            Px.send(l, new BlockParticleOption(ParticleTypes.BLOCK, to), top.getX() + 0.5, top.getY() + 1.0, top.getZ() + 0.5, 4, 0.3, 0.1, 0.3, 0.1);
            if (style == Style.FIRE) {
               Px.send(l, ParticleTypes.FLAME, top.getX() + 0.5, top.getY() + 1.1, top.getZ() + 0.5, 2, 0.2, 0.05, 0.2, 0.02);
            }
         }
      }
   }

   /** A column of sand / fire / light rising from the ground (visual + lift). */
   public static void pillar(ServerLevel l, Vec3 c, double radius, double height, Style style, int t) {
      for (int i = 0; i < 10; i++) {
         double a = t * 0.6 + i * Math.PI * 0.2;
         double y = (t * 0.9 + i * 0.9) % height;
         double rr = radius * (0.6 + 0.4 * Math.sin(y));
         double x = c.x + Math.cos(a) * rr;
         double z = c.z + Math.sin(a) * rr;
         switch (style) {
            case SAND -> Px.send(l, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), x, c.y + y, z, 2, 0.1, 0.1, 0.1, 0.1);
            case FIRE -> Px.send(l, ParticleTypes.FLAME, x, c.y + y, z, 2, 0.05, 0.05, 0.05, 0.02);
            case STAR -> Px.send(l, ParticleTypes.END_ROD, x, c.y + y, z, 1, 0.05, 0.05, 0.05, 0.01);
            default -> Px.send(l, ParticleTypes.CLOUD, x, c.y + y, z, 1, 0.05, 0.05, 0.05, 0.01);
         }
      }
   }
}
