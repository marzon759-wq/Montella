package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.compat.DataEntity;
import com.krolasyon.beyblade.registry.ModBlocks;
import com.krolasyon.beyblade.registry.ModEntities;
import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class StadiumEntity extends DataEntity {
   public static final float BOWL_R = 10.5F;
   public static final float FLOOR_Y0 = 0.35F;
   public static final float FLOOR_RISE = 1.15F;
   public static final float LIP_R = 11.1F;
   public static final float LIP_Y = 1.7F;
   public static final float LEDGE_R = 12.2F;
   public static final float WALL_IN = 11.1F;
   public static final float WALL_OUT = 11.5F;
   public static final float WALL_TOP = 2.75F;
   public static final float SLOPE_R = 12.8F;
   public static final float TIER2_Y = 1.1F;
   public static final float TIER1_Y = 0.55F;
   public static final float APOTHEM2 = 13.4F;
   public static final float APOTHEM1 = 14.4F;
   public static final float WALL_HALF_DEG = 45.0F;
   public static final float PAD_R = 23.5F;
   public static final float PAD_HALF = 1.6F;
   public static final float RAIL_R0 = 9.45F;
   public static final float RAIL_R1 = 10.35F;
   public static final float RAIL_GAP = 20.0F;
   public static final float XTREME_DEG = 0.0F;
   public static final float XTREME_HALF = 10.0F;
   public static final float OVER_HALF = 8.0F;
   public static final float[] OVER_DEG = new float[]{140.0F, 220.0F};
   public static final double POCKET_V = 0.7;
   public static final int Z_NONE = 0;
   public static final int Z_OVER = 1;
   public static final int Z_XTREME = 2;
   public static final int DECK_K0 = 15;
   public static final int DECK_K1 = 18;
   public static final int DECK_Y = 3;
   public static final int DECK_HALF = 2;
   public static final double BARRIER_R0 = 14.6;
   public static final double BARRIER_R1 = 15.6;
   public static final int BARRIER_H = 3;
   public static final int M_IDLE = 0;
   public static final int M_COUNTDOWN = 1;
   public static final int M_BATTLE = 2;
   public static final int M_ROUND_END = 3;
   public static final int M_MATCH_END = 4;
   public static final int WIN_POINTS = 4;
   public static final int ROUND_TICKS = 2400;
   public static final int LAUNCH_GRACE = 300;
   private static final EntityDataAccessor<Integer> DATA_MSTATE = SynchedEntityData.defineId(StadiumEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_SCORE_A = SynchedEntityData.defineId(StadiumEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_SCORE_B = SynchedEntityData.defineId(StadiumEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_TIMER = SynchedEntityData.defineId(StadiumEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_ROUND = SynchedEntityData.defineId(StadiumEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<String> DATA_NAME_A = SynchedEntityData.defineId(StadiumEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<String> DATA_NAME_B = SynchedEntityData.defineId(StadiumEntity.class, EntityDataSerializers.STRING);
   private final List<BlockPos> barriers = new ArrayList<>();
   private boolean barriersDone;
   private boolean balconyDone;
   private final List<BlockPos> balcony = new ArrayList<>();
   private int barrierRetry;
   private int balconyRetry;
   @Nullable
   private UUID playerA;
   @Nullable
   private UUID playerB;
   @Nullable
   private UUID aiBey;
   private boolean vsAI;
   private int sneakTicks;
   private int waitMsg;
   private int graceTicks;
   private int outTicksA;
   private int outTicksB;
   private boolean seenA;
   private boolean seenB;
   private int timer;
   public final List<float[]> scars = new ArrayList<>();
   public static final int SCAR_LIFE = 1800;

   public static double deg(double dx, double dz) {
      double d = Math.toDegrees(Math.atan2(dz, dx));
      return d < 0.0 ? d + 360.0 : d;
   }

   public static double angDist(double a, double b) {
      double d = Math.abs(((a - b) % 360.0 + 360.0) % 360.0);
      return d > 180.0 ? 360.0 - d : d;
   }

   public static int zoneAt(double deg) {
      if (angDist(deg, 0.0) < 10.0) {
         return 2;
      } else {
         for (float o : OVER_DEG) {
            if (angDist(deg, (double)o) < 8.0) {
               return 1;
            }
         }

         return 0;
      }
   }

   public static boolean onRail(double deg) {
      return angDist(deg, 0.0) > 20.0;
   }

   public void addScar(double x, double z, float r) {
      this.scars.add(new float[]{(float)(x - this.getX()), (float)(z - this.getZ()), r, (float)this.tickCount, this.random.nextFloat() * 6.283F});
      if (this.scars.size() > 28) {
         this.scars.remove(0);
      }
   }

   public StadiumEntity(EntityType<? extends StadiumEntity> type, Level level) {
      super(type, level);
      this.noPhysics = true;
      this.blocksBuilding = false;
   }

   public static boolean isGap(double angleRad) {
      double deg = Math.toDegrees(angleRad);
      double m = ((deg - 45.0) % 90.0 + 90.0) % 90.0;
      double off = Math.min(m, 90.0 - m);
      return off > 45.0;
   }

   public static double surface(double r) {
      if (r <= 10.5) {
         double k = r / 10.5;
         return 0.35F + 1.15F * k * k;
      } else if (r <= 11.1F) {
         return 1.5 + 0.20000005F * (r - 10.5) / 0.6000004F;
      } else if (r <= 12.2F) {
         return 1.7F;
      } else if (r <= 12.8F) {
         return 1.7F + -0.6F * (r - 12.2F) / 0.6000004F;
      } else if (r <= 13.4F) {
         return 1.1F;
      } else {
         return r <= 14.4F ? 0.55F : -1.0;
      }
   }

   public static double slope(double r) {
      return r <= 10.5 ? 2.3F * r / 110.25 : 0.0;
   }

   protected void defineData(DataDef d) {
      d.define(DATA_MSTATE, 0);
      d.define(DATA_SCORE_A, 0);
      d.define(DATA_SCORE_B, 0);
      d.define(DATA_TIMER, 0);
      d.define(DATA_ROUND, 0);
      d.define(DATA_NAME_A, "");
      d.define(DATA_NAME_B, "");
   }

   public int matchState() {
      return (Integer)this.entityData.get(DATA_MSTATE);
   }

   public int scoreA() {
      return (Integer)this.entityData.get(DATA_SCORE_A);
   }

   public int scoreB() {
      return (Integer)this.entityData.get(DATA_SCORE_B);
   }

   public int timerTicks() {
      return (Integer)this.entityData.get(DATA_TIMER);
   }

   public int round() {
      return (Integer)this.entityData.get(DATA_ROUND);
   }

   public String nameA() {
      return (String)this.entityData.get(DATA_NAME_A);
   }

   public String nameB() {
      return (String)this.entityData.get(DATA_NAME_B);
   }

   public Vec3 padPos(int side) {
      return this.position().add(0.0, 0.0, side == 0 ? 23.5 : -23.5);
   }

   public void tick() {
      super.tick();
      this.setDeltaMovement(0.0, 0.0, 0.0);
      if (!this.level().isClientSide()) {
         if (!this.barriersDone && --this.barrierRetry <= 0) {
            this.placeBarriers();
         }

         if (!this.balconyDone && --this.balconyRetry <= 0) {
            this.balconyRetry = 100;
            this.placeBalconies();
         }

         this.tickMatch();
      }
   }

   private void placeBarriers() {
      this.barrierRetry = 100;
      Level l = this.level();
      int y0 = Mth.floor(this.getY());
      int R = (int)Math.ceil(15.6) + 1;
      boolean all = true;
      BlockState wall = ((Block)ModBlocks.STADIUM_BARRIER.get()).defaultBlockState();

      for (int dx = -R; dx <= R; dx++) {
         for (int dz = -R; dz <= R; dz++) {
            BlockPos col = BlockPos.containing(this.getX() + (double)dx, (double)y0, this.getZ() + (double)dz);
            double cx = (double)col.getX() + 0.5 - this.getX();
            double cz = (double)col.getZ() + 0.5 - this.getZ();
            double r = Math.sqrt(cx * cx + cz * cz);
            if (!(r < 14.6) && !(r > 15.6)) {
               if (!l.isLoaded(col)) {
                  all = false;
               } else {
                  for (int h = 0; h < 3; h++) {
                     BlockPos p = col.above(h);
                     BlockState s = l.getBlockState(p);
                     if (!s.is(wall.getBlock()) && (s.isAir() || s.canBeReplaced())) {
                        l.setBlock(p, wall, 2);
                        this.barriers.add(p.immutable());
                     }
                  }
               }
            }
         }
      }

      this.barriersDone = all;
   }

   public Vec3 balconyPos(int side) {
      BlockPos c = BlockPos.containing(this.getX(), this.getY(), this.getZ());
      int s = side == 0 ? 1 : -1;
      return new Vec3((double)c.getX() + 0.5, (double)(c.getY() + 3 + 1), (double)(c.getZ() + s * 16) + 0.5);
   }

   private void placeBalconies() {
      Level l = this.level();
      BlockPos c = BlockPos.containing(this.getX(), this.getY(), this.getZ());
      boolean all = true;
      BlockState deck = Blocks.SMOOTH_STONE.defaultBlockState();
      BlockState pillar = Blocks.STONE_BRICKS.defaultBlockState();
      BlockState rail = Blocks.IRON_BARS.defaultBlockState();

      for (int side = 0; side < 2; side++) {
         int s = side == 0 ? 1 : -1;
         Direction up = side == 0 ? Direction.NORTH : Direction.SOUTH;
         BlockState stand = (side == 0 ? Blocks.BLUE_CONCRETE : Blocks.RED_CONCRETE).defaultBlockState();
         BlockState stair = (BlockState)Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, up);
         if (!l.isLoaded(c.offset(0, 0, s * 21))) {
            all = false;
         } else {
            for (int dx = -2; dx <= 2; dx++) {
               for (int k = 15; k <= 18; k++) {
                  boolean edge = Math.abs(dx) == 2;
                  this.put(l, c.offset(dx, 3, s * k), !edge && k == 16 ? stand : deck);
                  if (edge || k == 15) {
                     this.put(l, c.offset(dx, 4, s * k), rail);
                  }
               }

               if (Math.abs(dx) == 2) {
                  for (int kx : new int[]{16, 18}) {
                     for (int h = 0; h < 3; h++) {
                        this.put(l, c.offset(dx, h, s * kx), pillar);
                     }
                  }
               }
            }

            for (int dx = -1; dx <= 1; dx++) {
               for (int i = 0; i < 3; i++) {
                  int kx = 19 + i;
                  int top = 2 - i;
                  this.put(l, c.offset(dx, top, s * kx), stair);

                  for (int h = 0; h < top; h++) {
                     this.put(l, c.offset(dx, h, s * kx), pillar);
                  }
               }
            }
         }
      }

      this.balconyDone = all;
   }

   private void put(Level l, BlockPos p, BlockState st) {
      BlockState cur = l.getBlockState(p);
      if (!cur.is(st.getBlock())) {
         if (cur.isAir() || cur.canBeReplaced()) {
            l.setBlock(p, st, 2);
            this.balcony.add(p.immutable());
         }
      }
   }

   private void removeBalconies() {
      Level l = this.level();

      for (BlockPos p : this.balcony) {
         if (l.isLoaded(p)) {
            BlockState st = l.getBlockState(p);
            if (st.is(Blocks.SMOOTH_STONE)
               || st.is(Blocks.STONE_BRICKS)
               || st.is(Blocks.IRON_BARS)
               || st.is(Blocks.STONE_BRICK_STAIRS)
               || st.is(Blocks.BLUE_CONCRETE)
               || st.is(Blocks.RED_CONCRETE)) {
               l.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
         }
      }

      this.balcony.clear();
   }

   private void sendToBalcony(@Nullable ServerPlayer p, int side) {
      if (p != null) {
         Vec3 b = this.balconyPos(side);
         p.connection.teleport(b.x, b.y, b.z, side == 0 ? 180.0F : 0.0F, 30.0F);
         p.displayClientMessage(Component.translatable("msg.librabeyblade.match.balcony").withStyle(ChatFormatting.AQUA), true);
      }
   }

   private void removeBarriers() {
      Level l = this.level();

      for (BlockPos p : this.barriers) {
         if (l.isLoaded(p) && l.getBlockState(p).is((Block)ModBlocks.STADIUM_BARRIER.get())) {
            l.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
         }
      }

      this.barriers.clear();
   }

   public void remove(RemovalReason reason) {
      if (!this.level().isClientSide() && reason.shouldDestroy()) {
         this.removeBarriers();
         this.removeBalconies();
         this.despawnAI();
      }

      super.remove(reason);
   }

   @Nullable
   private ServerPlayer onPad(int side) {
      Vec3 p = this.padPos(side);
      ServerPlayer best = null;
      double bd = 5.1200004F;

      for (Player pl : this.level().players()) {
         if (!pl.isSpectator() && pl.isAlive()) {
            double dx = pl.getX() - p.x;
            double dz = pl.getZ() - p.z;
            if (!(Math.abs(pl.getY() - p.y) > 3.0)) {
               double d = dx * dx + dz * dz;
               if (d < bd) {
                  bd = d;
                  best = (ServerPlayer)pl;
               }
            }
         }
      }

      return best;
   }

   @Nullable
   private ServerPlayer player(@Nullable UUID id) {
      if (id == null) {
         return null;
      } else {
         Player p = this.level().getPlayerByUUID(id);
         if (p instanceof ServerPlayer sp && p.isAlive() && p.distanceToSqr(this) < 3136.0) {
            return sp;
         }

         return null;
      }
   }

   private void setState(int s, int t) {
      this.entityData.set(DATA_MSTATE, s);
      this.timer = t;
      this.entityData.set(DATA_TIMER, t);
   }

   private void tickMatch() {
      int st = this.matchState();
      if (st == 0) {
         if (this.tickCount % 5 == 0) {
            ServerPlayer a = this.onPad(0);
            ServerPlayer b = this.onPad(1);
            if (a != null && b != null && a != b) {
               this.startMatch(a, b);
            } else {
               ServerPlayer solo = a != null ? a : b;
               if (solo == null) {
                  this.sneakTicks = 0;
               } else {
                  if (solo.isShiftKeyDown()) {
                     this.sneakTicks += 5;
                     solo.displayClientMessage(Component.translatable("msg.librabeyblade.match.ai_hold", new Object[]{Math.max(0, (40 - this.sneakTicks + 19) / 20)}), true);
                     if (this.sneakTicks >= 40) {
                        this.sneakTicks = 0;
                        if (a != null) {
                           this.startMatch(a, null);
                        } else {
                           this.startMatch(null, b);
                        }
                     }
                  } else {
                     this.sneakTicks = 0;
                     if (++this.waitMsg % 8 == 0) {
                        solo.displayClientMessage(Component.translatable("msg.librabeyblade.match.waiting"), true);
                     }
                  }
               }
            }
         }
      } else {
         ServerPlayer pa = this.player(this.playerA);
         ServerPlayer pb = this.player(this.playerB);
         if ((this.playerA == null || pa != null) && (this.playerB == null || pb != null)) {
            this.timer--;
            if (this.tickCount % 4 == 0) {
               this.entityData.set(DATA_TIMER, Math.max(0, this.timer));
            }

            switch (st) {
               case 1:
                  if (this.timer == 60 || this.timer == 40 || this.timer == 20) {
                     this.broadcast(
                        Component.literal(String.valueOf(this.timer / 20)).withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}),
                        Component.translatable("msg.librabeyblade.match.ready"),
                        (SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value()
                     );
                  }

                  if (this.timer <= 0) {
                     this.broadcast(
                        Component.translatable("msg.librabeyblade.match.go").withStyle(new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.BOLD}),
                        Component.translatable("msg.librabeyblade.match.go_sub"),
                        SoundEvents.PLAYER_ATTACK_SWEEP
                     );
                     this.playAll((SoundEvent)SoundEvents.NOTE_BLOCK_PLING.value(), 1.4F);
                     this.setState(2, 0);
                     this.graceTicks = 300;
                     this.seenA = this.seenB = false;
                     this.outTicksA = this.outTicksB = 0;
                     if (this.vsAI) {
                        this.spawnAI();
                     }
                  }
                  break;
               case 2:
                  this.tickBattle(pa, pb);
                  break;
               case 3:
                  if (this.timer <= 0) {
                     if (this.scoreA() < 4 && this.scoreB() < 4) {
                        this.recallAll(pa, pb);
                        this.startCountdown();
                     } else {
                        boolean aWon = this.scoreA() >= 4;
                        String w = aWon ? this.nameA() : this.nameB();
                        this.broadcast(
                           Component.translatable("msg.librabeyblade.match.winner", new Object[]{w})
                              .withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}),
                           Component.translatable("msg.librabeyblade.match.final", new Object[]{this.scoreA(), this.scoreB()}),
                           SoundEvents.UI_TOAST_CHALLENGE_COMPLETE
                        );
                        ServerPlayer wp = aWon ? pa : pb;
                        if (wp != null) {
                           BeybladeEntity.grantXp(wp, 150);
                           wp.displayClientMessage(Component.translatable("msg.librabeyblade.match.xp", new Object[]{150}), false);
                        }

                        ServerPlayer lp = aWon ? pb : pa;
                        if (lp != null) {
                           BeybladeEntity.grantXp(lp, 40);
                        }

                        this.recallAll(pa, pb);
                        this.setState(4, 100);
                     }
                  }
                  break;
               case 4:
                  if (this.timer <= 0) {
                     this.endMatch();
                  }
            }
         } else {
            this.broadcast(Component.translatable("msg.librabeyblade.match.cancel"), Component.empty(), (SoundEvent)SoundEvents.NOTE_BLOCK_BASS.value());
            this.endMatch();
         }
      }
   }

   private void startMatch(@Nullable ServerPlayer a, @Nullable ServerPlayer b) {
      this.vsAI = a == null || b == null;
      this.playerA = a == null ? null : a.getUUID();
      this.playerB = b == null ? null : b.getUUID();
      this.entityData.set(DATA_NAME_A, a == null ? Component.translatable("msg.librabeyblade.match.ai").getString() : a.getGameProfile().getName());
      this.entityData.set(DATA_NAME_B, b == null ? Component.translatable("msg.librabeyblade.match.ai").getString() : b.getGameProfile().getName());
      this.entityData.set(DATA_SCORE_A, 0);
      this.entityData.set(DATA_SCORE_B, 0);
      this.entityData.set(DATA_ROUND, 0);
      this.broadcast(
         Component.translatable("msg.librabeyblade.match.start").withStyle(new ChatFormatting[]{ChatFormatting.YELLOW, ChatFormatting.BOLD}),
         Component.literal(this.nameA() + "  vs  " + this.nameB()),
         SoundEvents.UI_TOAST_IN
      );
      this.recallAll(a, b);
      this.startCountdown();
   }

   public void debugStartVsAI(ServerPlayer p) {
      this.startMatch(p, null);
   }

   private void startCountdown() {
      this.despawnAI();
      this.sendToBalcony(this.player(this.playerA), 0);
      this.sendToBalcony(this.player(this.playerB), 1);
      this.entityData.set(DATA_ROUND, this.round() + 1);
      this.setState(1, 80);
      this.broadcast(
         Component.translatable("msg.librabeyblade.match.round", new Object[]{this.round()}).withStyle(ChatFormatting.YELLOW),
         Component.translatable("msg.librabeyblade.match.first_to", new Object[]{4}),
         (SoundEvent)SoundEvents.NOTE_BLOCK_BELL.value()
      );
   }

   private void endMatch() {
      this.despawnAI();
      this.playerA = this.playerB = null;
      this.vsAI = false;
      this.entityData.set(DATA_NAME_A, "");
      this.entityData.set(DATA_NAME_B, "");
      this.setState(0, 0);
      this.sneakTicks = 0;
   }

   @Nullable
   private BeybladeEntity beyOf(@Nullable ServerPlayer p, boolean aiSide) {
      if (aiSide) {
         if (this.aiBey == null) {
            return null;
         } else {
            if (((ServerLevel)this.level()).getEntity(this.aiBey) instanceof BeybladeEntity b && b.isAlive()) {
               return b;
            }

            return null;
         }
      } else if (p == null) {
         return null;
      } else {
         BeybladeEntity b = BeybladeEntity.findActive(p);
         return b != null && b.distanceToSqr(this) < 900.0 ? b : null;
      }
   }

   private boolean inside(BeybladeEntity b) {
      double dx = b.getX() - this.getX();
      double dz = b.getZ() - this.getZ();
      return dx * dx + dz * dz < 123.21001F && b.getY() > this.getY() - 1.0 && b.getY() < this.getY() + 8.0;
   }

   private void tickBattle(@Nullable ServerPlayer pa, @Nullable ServerPlayer pb) {
      BeybladeEntity ba = this.beyOf(pa, this.playerA == null);
      BeybladeEntity bb = this.beyOf(pb, this.playerB == null);
      if (ba != null && this.inside(ba)) {
         this.seenA = true;
      }

      if (bb != null && this.inside(bb)) {
         this.seenB = true;
      }

      if (this.graceTicks > 0) {
         this.graceTicks--;
         if (this.graceTicks % 100 == 0 && this.graceTicks > 0) {
            if (!this.seenA && pa != null) {
               pa.displayClientMessage(Component.translatable("msg.librabeyblade.match.launch_now", new Object[]{(this.graceTicks + 19) / 20}), true);
            }

            if (!this.seenB && pb != null) {
               pb.displayClientMessage(Component.translatable("msg.librabeyblade.match.launch_now", new Object[]{(this.graceTicks + 19) / 20}), true);
            }
         }

         if (this.graceTicks == 0) {
            if (!this.seenA && !this.seenB) {
               this.roundOver(-1, 0, "msg.librabeyblade.match.no_launch");
               return;
            }

            if (!this.seenA) {
               this.roundOver(1, 1, "msg.librabeyblade.match.no_launch");
               return;
            }

            if (!this.seenB) {
               this.roundOver(0, 1, "msg.librabeyblade.match.no_launch");
               return;
            }
         }
      }

      boolean downA = this.seenA && (ba == null || ba.getState() != 0);
      boolean downB = this.seenB && (bb == null || bb.getState() != 0);
      if (this.seenA && ba != null && !downA && !this.inside(ba)) {
         if (++this.outTicksA > 100) {
            downA = true;
         }
      } else {
         this.outTicksA = 0;
      }

      if (this.seenB && bb != null && !downB && !this.inside(bb)) {
         if (++this.outTicksB > 100) {
            downB = true;
         }
      } else {
         this.outTicksB = 0;
      }

      if (downA && downB) {
         this.roundOver(-1, 0, "msg.librabeyblade.match.draw");
      } else if (downA) {
         this.scoreFinish(1, ba);
      } else if (downB) {
         this.scoreFinish(0, bb);
      }
   }

   private void scoreFinish(int winner, @Nullable BeybladeEntity loser) {
      if (loser == null || loser.getState() == 2 || !loser.isAlive()) {
         this.roundOver(winner, 1, "msg.librabeyblade.match.forfeit");
      } else if (loser.outZone() == 2) {
         this.roundOver(winner, 3, "msg.librabeyblade.match.xtreme_finish");
      } else if (loser.outZone() == 1) {
         this.roundOver(winner, 2, "msg.librabeyblade.match.over_finish");
      } else if (loser.finishedByAbility()) {
         this.roundOver(winner, 2, "msg.librabeyblade.match.ability_finish");
      } else {
         this.roundOver(winner, 1, "msg.librabeyblade.match.spin_finish");
      }
   }

   private void roundOver(int winner, int points, String reason) {
      if (winner == 0) {
         this.entityData.set(DATA_SCORE_A, this.scoreA() + points);
      }

      if (winner == 1) {
         this.entityData.set(DATA_SCORE_B, this.scoreB() + points);
      }

      Component title = winner < 0
         ? Component.translatable("msg.librabeyblade.match.draw_title")
         : Component.translatable("msg.librabeyblade.match.point", new Object[]{winner == 0 ? this.nameA() : this.nameB(), points});
      this.broadcast(
         title.copy()
            .withStyle(new ChatFormatting[]{winner == 0 ? ChatFormatting.BLUE : (winner == 1 ? ChatFormatting.RED : ChatFormatting.GRAY), ChatFormatting.BOLD}),
         Component.translatable(reason).append("  " + this.scoreA() + " - " + this.scoreB()),
         SoundEvents.FIREWORK_ROCKET_BLAST
      );
      ServerPlayer wp = winner == 0 ? this.player(this.playerA) : (winner == 1 ? this.player(this.playerB) : null);
      if (wp != null) {
         BeybladeEntity.grantXp(wp, 25 * points);
      }

      this.setState(3, 80);
   }

   private void recallAll(@Nullable ServerPlayer a, @Nullable ServerPlayer b) {
      for (ServerPlayer p : new ServerPlayer[]{a, b}) {
         if (p != null) {
            BeybladeEntity bey = BeybladeEntity.findActive(p);
            if (bey != null) {
               bey.recall();
            }
         }
      }

      this.despawnAI();
   }

   private void spawnAI() {
      int side = this.playerA == null ? 0 : 1;
      ServerPlayer human = this.player(side == 0 ? this.playerB : this.playerA);
      int lvl = 1;
      if (human != null) {
         BeybladeEntity hb = BeybladeEntity.findActive(human);
         lvl = hb != null ? hb.getLevel() : BeybladeEntity.heldLevel(human);
      }

      List<EntityType<? extends BeybladeEntity>> types = List.of(
         (EntityType)ModEntities.LIBRA.get(),
         (EntityType)ModEntities.LDRAGO.get(),
         (EntityType)ModEntities.LEONE.get(),
         (EntityType)ModEntities.EAGLE.get(),
         (EntityType)ModEntities.PEGASUS.get(),
         (EntityType)ModEntities.TEMPO.get(),
         (EntityType)ModEntities.VARIARES.get(),
         (EntityType)ModEntities.BEFFAL.get(),
         (EntityType)ModEntities.COMMON.get(),
         (EntityType)ModEntities.COMMON.get(),
         (EntityType)ModEntities.COMMON.get()
      );
      EntityType<? extends BeybladeEntity> type = types.get(this.random.nextInt(types.size()));
      double z = side == 0 ? 7.5 : -7.5;
      Vec3 pos = this.position().add(0.0, 3.0, z);
      BeybladeEntity b = BeybladeEntity.launch(type, this.level(), null, ItemStack.EMPTY, pos, new Vec3(0.0, 0.05, side == 0 ? -0.35 : 0.35), 1.0F);
      if (b instanceof CommonBeybladeEntity c) {
         c.setVariant(this.random.nextInt(CommonBeybladeEntity.VARIANTS.length));
      }

      b.setLevel(Mth.clamp(lvl + this.random.nextInt(3) - 1, 1, 30));
      b.healSpin(b.getMaxSpin());
      this.aiBey = b.getUUID();
      this.playAll((SoundEvent)ModSounds.LAUNCH.get(), 1.0F);
   }

   private void despawnAI() {
      if (this.aiBey != null && this.level() instanceof ServerLevel sl) {
         Entity e = sl.getEntity(this.aiBey);
         if (e != null) {
            e.discard();
         }

         this.aiBey = null;
      }
   }

   private List<ServerPlayer> audience() {
      List<ServerPlayer> out = new ArrayList<>();

      for (Player p : this.level().players()) {
         if (p instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)p;
            if (p.distanceToSqr(this) < 2304.0) {
               out.add(sp);
            }
         }
      }

      return out;
   }

   private void broadcast(Component title, Component sub, @Nullable SoundEvent sound) {
      for (ServerPlayer p : this.audience()) {
         p.connection.send(new ClientboundSetTitlesAnimationPacket(4, 30, 8));
         p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
         p.connection.send(new ClientboundSetTitleTextPacket(title));
      }

      if (sound != null) {
         this.playAll(sound, 1.0F);
      }
   }

   private void playAll(SoundEvent s, float pitch) {
      for (ServerPlayer p : this.audience()) {
         this.level().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, 0.9F, pitch);
      }
   }

   private void pickUp(Player player) {
      if (!this.level().isClientSide() && !this.isRemoved()) {
         if (this.matchState() != 0) {
            this.endMatch();
         }

         ItemStack s = new ItemStack((ItemLike)ModItems.STADIUM.get());
         if (!player.getAbilities().instabuild && !player.getInventory().add(s)) {
            this.spawnAtLocation(s, 0.5F);
         }

         this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 0.7F);
         this.discard();
      }
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      if (!player.isShiftKeyDown()) {
         return InteractionResult.PASS;
      } else {
         this.pickUp(player);
         return InteractionResult.sidedSuccess(this.level().isClientSide());
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      if (this.isInvulnerableTo(source)) {
         return false;
      } else {
         if (source.getEntity() instanceof Player p && p.isShiftKeyDown()) {
            this.pickUp(p);
            return true;
         }

         return false;
      }
   }

   public boolean isPickable() {
      return !this.isRemoved();
   }

   public boolean isPushable() {
      return false;
   }

   public boolean canBeCollidedWith() {
      return false;
   }

   public AABB getBoundingBoxForCulling() {
      return this.getBoundingBox().inflate(19.0, 3.5, 19.0);
   }

   public boolean shouldRenderAtSqrDistance(double d) {
      return d < 36864.0;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.barriers.clear();

      for (Tag t : tag.getList("Barriers", 4)) {
         this.barriers.add(BlockPos.of(((LongTag)t).getAsLong()));
      }

      this.barriersDone = tag.getBoolean("BarriersDone");
      this.balcony.clear();

      for (Tag t : tag.getList("Balcony", 4)) {
         this.balcony.add(BlockPos.of(((LongTag)t).getAsLong()));
      }

      this.balconyDone = tag.getBoolean("BalconyDone");
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      ListTag list = new ListTag();

      for (BlockPos p : this.barriers) {
         list.add(LongTag.valueOf(p.asLong()));
      }

      tag.put("Barriers", list);
      tag.putBoolean("BarriersDone", this.barriersDone);
      ListTag bl = new ListTag();

      for (BlockPos p : this.balcony) {
         bl.add(LongTag.valueOf(p.asLong()));
      }

      tag.put("Balcony", bl);
      tag.putBoolean("BalconyDone", this.balconyDone);
   }
}
