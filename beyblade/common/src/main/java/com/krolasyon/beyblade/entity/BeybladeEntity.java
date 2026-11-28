package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.client.ClientHooks;
import com.krolasyon.beyblade.compat.V;
import com.krolasyon.beyblade.compat.GeoBeyBase;
import com.krolasyon.beyblade.item.LibraItem;
import com.krolasyon.beyblade.parts.BeyPart;
import com.krolasyon.beyblade.parts.Loadout;
import com.krolasyon.beyblade.parts.PartType;
import com.krolasyon.beyblade.registry.ModEntities;
import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class BeybladeEntity extends GeoBeyBase {
   public static final float WIDTH = 0.9F;
   public static final float HEIGHT = 0.66F;
   public static final float RADIUS = 0.45F;
   public static final float MAX_SPIN = 5000.0F;
   public static final int STATE_BATTLE = 0;
   public static final int STATE_DEFEATED = 1;
   public static final int STATE_RECALL = 2;
   public static final int AB_NONE = 0;
   public static final int AB_SONIC_WAVE = 1;
   public static final int AB_SHIELD = 2;
   public static final int AB_INFERNO = 3;
   public static final int AB_BUSTER = 4;
   public static final int AB_JUDGMENT = 5;
   public static final int ABILITY_COUNT = 6;
   public static final int[] DURATION = new int[]{0, 46, 0, 84, 62, 112};
   public static final int[] COOLDOWN = new int[]{0, 200, 500, 600, 700, 1200};
   public static final int SHIELD_TICKS = 170;
   public static final int[] WAVE_PULSES = new int[]{10, 20, 30};
   public static final int WAVE_LIFE = 16;
   public static final float WAVE_SPEED = 0.62F;
   public static final int INF_TORNADO = 24;
   public static final int INF_DIVE = 52;
   public static final double INF_RADIUS = 3.2;
   public static final double INF_HEIGHT = 9.0;
   public static final double INF_IMPACT_RADIUS = 5.5;
   public static final int BUS_CHARGE = 12;
   public static final int BUS_FIRE = 32;
   public static final int BUS_END = 52;
   public static final int JUD_LINK = 22;
   public static final int JUD_STRIKE = 86;
   public static final double JUD_RANGE = 14.0;
   public static final int MODE_ATTACK = 0;
   public static final int MODE_DEFENSE = 1;
   public static final int MODE_STAMINA = 2;
   public static final int MODE_BALANCE = 3;
   public static final int MODE_COUNT = 4;
   public static final String[] MODE_IDS = new String[]{"attack", "defense", "stamina", "balance"};
   public static final int OR_AUTO = 0;
   public static final int OR_ATTACK = 1;
   public static final int OR_RETREAT = 2;
   public static final int OR_CIRCLE = 3;
   public static final int OR_CENTER = 4;
   public static final int OR_FOLLOW = 5;
   public static final int OR_GUARD = 6;
   public static final int OR_RUSH = 7;
   public static final int OR_DEFEND = 8;
   public static final int OR_ZIGZAG = 9;
   public static final int OR_BERSERK = 10;
   public static final int OR_CONSERVE = 11;
   public static final int OR_FLANK = 12;
   public static final int OR_UPPER = 13;
   public static final int ORDER_COUNT = 14;
   public static final String[] ORDER_IDS = new String[]{
      "auto", "attack", "retreat", "circle", "center", "follow", "guard", "rush", "defend", "zigzag", "berserk", "conserve", "flank", "upper"
   };
   public static final int MAX_LEVEL = 30;
   public static final byte EV_HIT = 60;
   public static final byte EV_HEAVY_HIT = 61;
   public static final byte EV_LAND = 62;
   public static final byte EV_SHIELD_HIT = 108;
   public static final byte EV_WALL = 64;
   public static final byte EV_IMPACT = 65;
   public static final byte EV_STRIKE = 66;
   public static final byte EV_GRIND = 67;
   public static final byte EV_SHOCK = 68;
   public static final byte EV_SPIRIT = 69;
   public static final byte EV_EVADE = 110;
   private static final EntityDataAccessor<Optional<UUID>> DATA_OWNER = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
   private static final EntityDataAccessor<Float> DATA_SPIN = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_MAX_SPIN = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_ABILITY = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_SHIELD_END = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_MODE = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_ORDER = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_LEVEL = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_SPIRIT = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_SPIRIT_END = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_XP = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<String> DATA_PARTS = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Integer> DATA_SCLASH = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DATA_SCLASH_RATIO = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_WOBBLE = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer>[] DATA_COOLDOWN = new EntityDataAccessor[6];
   private ItemStack stored = ItemStack.EMPTY;
   protected int abilityTicks;
   protected double abilityBaseY;
   protected double abilityBaseX;
   protected double abilityBaseZ;
   protected boolean diving;
   private int dashCooldown = 20;
   private int dashTicks;
   private int pendingAbility;
   private int ramTicks;
   private int rushPhase;
   private int trainTicks;
   private int retreatTicks;
   private int lockTicks;
   private int flungTicks;
   private boolean launchFlight;
   private int railTicks;
   private int railDir;
   private int railCooldown;
   private int xdashTicks;
   private int outZone;
   private double railSpeed;
   public static final byte EV_RAIL = 86;
   public static final byte EV_XDASH = 87;
   public static final byte EV_OUT = 88;
   public int railClientTime = -1000;
   public int xdashClientTime = -1000;
   public int outClientTime = -1000;
   @Nullable
   private BeybladeEntity lockPartner;
   @Nullable
   private BeybladeEntity lastAttacker;
   private long lastAttackTick = -1000L;
   private long lastAbilityHitTick = -1000L;
   private int lerpSteps;
   private double lerpX;
   private double lerpY;
   private double lerpZ;
   private int ownerMissing;
   private int stateTicks;
   private int targetRefresh;
   private int wallSoundCooldown;
   private int clashCooldown;
   @Nullable
   private StadiumEntity stadium;
   private boolean inBowl;
   private boolean contained;
   @Nullable
   protected Entity target;
   @Nullable
   private Vec3 home;
   private boolean frozen;
   public static boolean rivalAbilities;
   private final Set<Integer>[] waveHits = new Set[]{new HashSet(), new HashSet(), new HashSet()};
   public float spinAngle;
   public float spinAngleO;
   public float precess;
   public float precessO;
   public int clientAbility;
   public int clientAbilityStart = -1000;
   public int clientStateStart;
   public double abilityX;
   public double abilityY;
   public double abilityZ;
   public int grindTime = -1000;
   public int shockTime = -1000;
   public int spiritTime = -1000;
   public int evadeTime = -1000;
   public double shockDirX;
   public double shockDirZ;
   public double shockX;
   public double shockY;
   public double shockZ;
   public int hitTime = -1000;
   /** phase of the tip's flower pattern drive */
   private float tipPhase;
   /** client only: this defeat is drawn as a burst (parts flying apart) instead of a sleep out */
   public boolean burstClient;
   /** client only: spin angle at the moment of defeat (the fallen bey stops turning) */
   public float frozenSpin;
   public int heavyHitTime = -1000;
   public int landTime = -1000;
   public int shieldHitTime = -1000;
   public int impactTime = -1000;
   public int strikeTime = -1000;
   public final double[] trailX = new double[8];
   public final double[] trailY = new double[8];
   public final double[] trailZ = new double[8];
   private boolean soundStarted;
   private boolean trailInit;
   public static final int SPIRIT_MAX = 100;
   public static final int SPIRIT_TICKS = 180;
   protected int stunTicks;
   public static boolean debugFlight;
   private boolean wobbleLanded;
   private float wobbleNow;
   public static final int SCLASH_TICKS = 30;
   public static final byte EV_SCLASH_BOOM = 104;
   private long specialStart = -1000L;
   private int sclashTicks;
   private int sclashCooldown;
   @Nullable
   private BeybladeEntity sclashPartner;
   private boolean sclashLeader;
   private boolean sclashPerfect;
   private float sclashPower;
   public int sclashClientStart = -1000;
   public int sclashBoomTime = -1000;
   public double boomX;
   public double boomY;
   public double boomZ;
   public boolean boomDraw;
   private static final DustParticleOptions GOLD;
   private static final DustParticleOptions LIME;
   private static final DustParticleOptions SAND;
   private static final DustParticleOptions STEAL;
   private static final DustParticleOptions RED;
   @Nullable
   private Loadout loadout;
   private String clientPartsKey;
   private Loadout clientLoadout;

   public BeybladeEntity(EntityType<? extends BeybladeEntity> type, Level level) {
      super(type, level);
      this.setStepHeight(0.6F);
      this.blocksBuilding = true;
   }

   public static BeybladeEntity launch(Level level, @Nullable Player owner, ItemStack stack, Vec3 pos, Vec3 velocity, float power) {
      return launch((EntityType<? extends BeybladeEntity>)ModEntities.LIBRA.get(), level, owner, stack, pos, velocity, power);
   }

   public static BeybladeEntity launch(
      EntityType<? extends BeybladeEntity> type, Level level, @Nullable Player owner, ItemStack stack, Vec3 pos, Vec3 velocity, float power
   ) {
      BeybladeEntity b = (BeybladeEntity)type.create(level);
      if (b == null) {
         throw new IllegalStateException("cannot create beyblade");
      } else {
         b.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
         b.setDeltaMovement(velocity);
         if (owner != null) {
            b.entityData.set(DATA_OWNER, Optional.of(owner.getUUID()));
         }

         b.stored = stack.copy();
         b.initFromItem(stack);
         b.refreshLoadout();
         CompoundTag t = V.tag(stack);
         int lvl = t != null && t.contains("BeyLevel") ? Mth.clamp(t.getInt("BeyLevel"), 1, 30) : 1;
         b.entityData.set(DATA_LEVEL, lvl);
         b.entityData.set(DATA_XP, t != null ? t.getInt("BeyXp") : 0);
         if (t != null && t.contains("BeyMode")) {
            b.entityData.set(DATA_MODE, Mth.clamp(t.getInt("BeyMode"), 0, 3));
         }

         float max = maxSpinFor(lvl) * b.partsSpinMul();
         b.entityData.set(DATA_MAX_SPIN, max);
         b.entityData.set(DATA_SPIN, max * Mth.clamp(power, 0.35F, 1.0F));
         b.home = pos;
         b.launchFlight = true;
         level.addFreshEntity(b);
         return b;
      }
   }

   protected void defineData(DataDef d) {
      d.define(DATA_OWNER, Optional.empty());
      d.define(DATA_SPIN, 5000.0F);
      d.define(DATA_MAX_SPIN, 5000.0F);
      d.define(DATA_STATE, 0);
      d.define(DATA_ABILITY, 0);
      d.define(DATA_TARGET, -1);
      d.define(DATA_SHIELD_END, 0);
      d.define(DATA_MODE, -1);
      d.define(DATA_ORDER, 0);
      d.define(DATA_LEVEL, 1);
      d.define(DATA_XP, 0);
      d.define(DATA_PARTS, "");
      d.define(DATA_SCLASH, -1);
      d.define(DATA_SCLASH_RATIO, 0.5F);
      d.define(DATA_WOBBLE, 0.0F);
      d.define(DATA_SPIRIT, 0);
      d.define(DATA_SPIRIT_END, 0);

      for (EntityDataAccessor<Integer> a : DATA_COOLDOWN) {
         d.define(a, 0);
      }
   }

   @Nullable
   public UUID getOwnerId() {
      return (UUID)((Optional)this.entityData.get(DATA_OWNER)).orElse(null);
   }

   public boolean isRival() {
      return this.getOwnerId() == null;
   }

   @Nullable
   public Player getOwner() {
      UUID id = this.getOwnerId();
      return id == null ? null : this.level().getPlayerByUUID(id);
   }

   public float getSpin() {
      return (Float)this.entityData.get(DATA_SPIN);
   }

   public float getMaxSpin() {
      return (Float)this.entityData.get(DATA_MAX_SPIN);
   }

   public boolean burstFinish() {
      return this.burstClient;
   }

   public float spinRatio() {
      return Mth.clamp(this.getSpin() / Math.max(1.0F, this.getMaxSpin()), 0.0F, 1.0F);
   }

   private void setSpin(float v) {
      this.entityData.set(DATA_SPIN, Mth.clamp(v, 0.0F, this.getMaxSpin()));
   }

   public int getState() {
      return (Integer)this.entityData.get(DATA_STATE);
   }

   private void setState(int s) {
      this.entityData.set(DATA_STATE, s);
      this.stateTicks = 0;
   }

   public int getAbility() {
      return (Integer)this.entityData.get(DATA_ABILITY);
   }

   public int getTargetId() {
      return (Integer)this.entityData.get(DATA_TARGET);
   }

   public boolean isShielded() {
      return this.level().getGameTime() < (long)((Integer)this.entityData.get(DATA_SHIELD_END)).intValue();
   }

   public int shieldRemaining() {
      return (int)Math.max(0L, (long)((Integer)this.entityData.get(DATA_SHIELD_END)).intValue() - this.level().getGameTime());
   }

   public int cooldownRemaining(int ability) {
      return (int)Math.max(0L, (long)((Integer)this.entityData.get(DATA_COOLDOWN[ability - 1])).intValue() - this.level().getGameTime());
   }

   public int nativeMode() {
      return 2;
   }

   public int getMode() {
      int m = (Integer)this.entityData.get(DATA_MODE);
      return m < 0 ? this.nativeMode() : m;
   }

   public void setMode(int m) {
      this.entityData.set(DATA_MODE, Math.floorMod(m, 4));
      Player owner = this.getOwner();
      if (owner != null) {
         owner.displayClientMessage(Component.translatable("msg.librabeyblade.mode", new Object[]{Component.translatable("mode.librabeyblade." + MODE_IDS[this.getMode()])}), true);
      }

      this.play((SoundEvent)ModSounds.CHARGE.get(), 0.7F, 1.4F + (float)this.getMode() * 0.1F);
   }

   public int getOrder() {
      return Mth.clamp((Integer)this.entityData.get(DATA_ORDER), 0, 13);
   }

   public void setOrder(int o) {
      this.entityData.set(DATA_ORDER, Mth.clamp(o, 0, 13));
      this.rushPhase = 0;
      Player owner = this.getOwner();
      if (owner != null) {
         owner.displayClientMessage(
            Component.translatable("msg.librabeyblade.order", new Object[]{Component.translatable("order.librabeyblade." + ORDER_IDS[this.getOrder()])}), true
         );
      }
   }

   public int getLevel() {
      return (Integer)this.entityData.get(DATA_LEVEL);
   }

   public int getXp() {
      return (Integer)this.entityData.get(DATA_XP);
   }

   public static int xpNeeded(int level) {
      return 60 + 40 * level;
   }

   public static float maxSpinFor(int level) {
      return 5000.0F * (1.0F + 0.04F * (float)(level - 1));
   }

   public void setLevel(int lvl) {
      lvl = Mth.clamp(lvl, 1, 30);
      this.entityData.set(DATA_LEVEL, lvl);
      float old = this.getMaxSpin();
      this.entityData.set(DATA_MAX_SPIN, maxSpinFor(lvl));
      this.setSpin(this.getSpin() + (this.getMaxSpin() - old));
   }

   public void addXp(int amount) {
      if (amount > 0 && !this.isRival() && this.getLevel() < 30) {
         int xp = this.getXp() + amount;
         int lvl = this.getLevel();

         boolean up;
         for (up = false; lvl < 30 && xp >= xpNeeded(lvl); up = true) {
            xp -= xpNeeded(lvl);
            lvl++;
         }

         this.entityData.set(DATA_XP, lvl >= 30 ? 0 : xp);
         if (up) {
            this.setLevel(lvl);
            this.healSpin(this.getMaxSpin() * 0.15F);
            Player owner = this.getOwner();
            if (owner != null) {
               owner.displayClientMessage(
                  Component.translatable("msg.librabeyblade.level_up", new Object[]{Component.translatable(this.titleKey()), lvl}).withStyle(ChatFormatting.GOLD),
                  false
               );
               this.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9F, 1.2F);
            }
         }
      }
   }

   public int getSpirit() {
      return (Integer)this.entityData.get(DATA_SPIRIT);
   }

   public void addSpirit(int n) {
      if (this.getState() == 0 && !this.isSpiritActive()) {
         Loadout l = this.parts();
         BeyPart bolt = l.get(PartType.BOLT);
         if (bolt != null && n > 0) {
            n = Math.round((float)n * (1.0F + 0.08F * (float)bolt.rarity() + 0.06F * (float)l.level(PartType.BOLT)));
         }

         this.entityData.set(DATA_SPIRIT, Mth.clamp(this.getSpirit() + n, 0, 100));
      }
   }

   public boolean isSpiritActive() {
      return this.level().getGameTime() < (long)((Integer)this.entityData.get(DATA_SPIRIT_END)).intValue();
   }

   public int spiritRemaining() {
      return (int)Math.max(0L, (long)((Integer)this.entityData.get(DATA_SPIRIT_END)).intValue() - this.level().getGameTime());
   }

   public void releaseSpirit() {
      Player owner = this.getOwner();
      if (this.getState() == 0) {
         if (this.getSpirit() < 100) {
            if (owner != null) {
               owner.displayClientMessage(Component.translatable("msg.librabeyblade.spirit_low", new Object[]{this.getSpirit()}), true);
            }
         } else {
            this.entityData.set(DATA_SPIRIT, 0);
            this.entityData.set(DATA_SPIRIT_END, (int)(this.level().getGameTime() + 180L));
            this.healSpin(this.getMaxSpin() * 0.15F);
            long now = this.level().getGameTime();

            for (int i = 0; i < 6; i++) {
               int end = (Integer)this.entityData.get(DATA_COOLDOWN[i]);
               if ((long)end > now) {
                  this.entityData.set(DATA_COOLDOWN[i], (int)(now + ((long)end - now) / 2L));
               }
            }

            this.play((SoundEvent)ModSounds.SHIELD_UP.get(), 2.0F, 0.7F);
            this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 1.5F, 0.6F);
            this.level().broadcastEntityEvent(this, (byte)69);
            if (owner != null) {
               owner.displayClientMessage(
                  Component.translatable("msg.librabeyblade.spirit", new Object[]{Component.translatable(this.titleKey())})
                     .withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}),
                  true
               );
            }
         }
      }
   }

   public boolean isFinalDrive() {
      return this.getMode() == 0 && this.spinRatio() < 0.35F && this.getState() == 0;
   }

   public boolean isLifeAfterDeath() {
      return this.getMode() == 2 && this.spinRatio() < 0.12F && this.getState() == 0;
   }

   private boolean nativeBonus() {
      return this.getMode() == this.nativeMode();
   }

   public float powerMul() {
      float m = switch (this.getMode()) {
         case 0 -> 1.3F;
         case 1 -> 0.85F;
         case 2 -> 0.8F;
         default -> 1.05F;
      };
      if (this.nativeBonus()) {
         m *= 1.1F;
      }

      if (this.getOrder() == 10) {
         m *= 1.15F;
      }

      if (this.getOrder() == 13) {
         m *= 0.9F;
      }

      if (this.isSpiritActive()) {
         m *= 1.3F;
      }

      if (this.isFinalDrive()) {
         m *= 1.15F;
      }

      return m * (1.0F + 0.03F * (float)(this.getLevel() - 1)) * Loadout.mul(this.parts().attack());
   }

   public float guardMul() {
      float m = switch (this.getMode()) {
         case 0 -> 1.15F;
         case 1 -> 0.65F;
         case 2 -> 0.95F;
         default -> 0.9F;
      };
      if (this.nativeBonus()) {
         m *= 0.92F;
      }

      if (this.getOrder() == 8) {
         m *= 0.7F;
      }

      if (this.getOrder() == 10) {
         m *= 1.15F;
      }

      Loadout l = this.parts();
      if (l.trackTrait() == BeyPart.Trait.DEFENSE_RING) {
         m *= 0.92F;
      }

      return m / Loadout.mul(l.defense());
   }

   public double modeSpeed() {
      double m = switch (this.getMode()) {
         case 0 -> 1.25;
         case 1 -> 0.85;
         case 2 -> 0.9;
         default -> 1.05;
      };
      if (this.isSpiritActive()) {
         m *= 1.2;
      }

      if (this.isFinalDrive()) {
         m *= 1.25;
      }

      Loadout l = this.parts();
      if (l.tip() == BeyPart.Tip.FINAL_DRIVE && this.spinRatio() < 0.35F) {
         m *= 1.2;
      }

      return m * (1.0 + 0.008 * (double)(this.getLevel() - 1)) * (double)Loadout.mul(l.speed());
   }

   public float modeDecay() {
      float m = switch (this.getMode()) {
         case 0 -> 1.25F;
         case 1 -> 0.95F;
         case 2 -> 0.55F;
         default -> 0.85F;
      };
      if (this.getMode() == 2 && this.nativeBonus()) {
         m *= 0.85F;
      }

      if (this.getOrder() == 11) {
         m *= 0.6F;
      }

      if (this.getOrder() == 10) {
         m *= 1.3F;
      }

      if (this.isLifeAfterDeath()) {
         m *= 0.4F;
      }

      Loadout l = this.parts();
      BeyPart.Trait tr = l.trackTrait();
      if (tr == BeyPart.Trait.WING) {
         m *= 0.92F;
      }

      if (tr == BeyPart.Trait.TORNADO) {
         m *= 0.9F;
      }

      if (l.tip() == BeyPart.Tip.FINAL_SURVIVE && this.spinRatio() < 0.3F) {
         m *= 0.6F;
      }

      m *= com.krolasyon.beyblade.parts.TipPhysics.decay(l.tip());
      return m / Loadout.mul(l.stamina());
   }

   public double knockMul() {
      double m = switch (this.getMode()) {
         case 0 -> 1.1;
         case 1 -> 0.45;
         case 2 -> 1.0;
         default -> 0.8;
      };
      m *= 1.0 + 0.6 * (double)this.getWobble();
      Loadout l = this.parts();
      m *= Mth.clamp(46.0 / (double)Math.max(20.0F, l.weight()), 0.72, 1.35);
      switch (l.trackTrait()) {
         case BOOST_DISK:
            m *= 0.8;
            break;
         case RUBBER_RING:
            m *= 0.85;
            break;
         case GRAVITY:
            m *= 1.15;
      }

      m *= com.krolasyon.beyblade.parts.TipPhysics.knock(l.tip());
      return this.getOrder() == 8 ? m * 0.5 : m;
   }

   public boolean isRamming() {
      return this.pendingAbility != 0;
   }

   public boolean finishedByAbility() {
      return this.level().getGameTime() - this.lastAbilityHitTick < 60L;
   }

   @Nullable
   public BeybladeEntity lastAttacker() {
      return this.level().getGameTime() - this.lastAttackTick < 200L ? this.lastAttacker : null;
   }

   public boolean isEnemy(BeybladeEntity o) {
      if (o != this && o.isAlive() && o.getState() == 0) {
         UUID a = this.getOwnerId();
         return a == null || !a.equals(o.getOwnerId());
      } else {
         return false;
      }
   }

   public boolean isHostileLiving(LivingEntity e) {
      return e.isAlive() && !(e instanceof Player) && !this.isRival() ? e == this.target || e instanceof Enemy : false;
   }

   @Nullable
   public static BeybladeEntity findActive(Player player) {
      List<BeybladeEntity> list = player.level()
         .getEntitiesOfClass(BeybladeEntity.class, player.getBoundingBox().inflate(96.0), b -> b.isAlive() && player.getUUID().equals(b.getOwnerId()));
      return list.isEmpty() ? null : list.get(0);
   }

   public int spinDir() {
      return 1;
   }

   public String kindId() {
      return "libra";
   }

   public String titleKey() {
      return "hud.librabeyblade.title." + this.kindId();
   }

   public String abilityKey(int id) {
      return "ability.librabeyblade." + id;
   }

   public int duration(int id) {
      return DURATION[id];
   }

   public int cooldown(int id) {
      return COOLDOWN[id];
   }

   public int buffAbility() {
      return 2;
   }

   public int buffTicks() {
      return 170;
   }

   public boolean requiresTarget(int id) {
      return id == 4;
   }

   public boolean buffReflects() {
      return true;
   }

   public float buffAttackMul() {
      return 1.0F;
   }

   public double buffSpeedMul() {
      return 1.0;
   }

   protected void onBuffStart(@Nullable Player owner) {
      this.play((SoundEvent)ModSounds.SHIELD_UP.get(), 1.4F, 1.0F);
   }

   protected void tickBuff() {
      this.tickShield();
   }

   protected ItemStack defaultItem() {
      return new ItemStack((ItemLike)ModItems.LIBRA.get());
   }

   protected float clientSpinSpeed(int ab, float at, float speed) {
      return ab == 3 && at < 24.0F ? 96.0F : speed;
   }

   public boolean tryDodge(BeybladeEntity attacker) {
      float c = this.dodgeChance();
      if (!(c <= 0.0F) && this.getState() == 0 && !(this.random.nextFloat() >= c)) {
         Vec3 away = flat(this.position().subtract(attacker.position()));
         away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
         Vec3 side = new Vec3(-away.z, 0.0, away.x).scale(this.random.nextBoolean() ? 1.0 : -1.0);
         Vec3 jump = side.scale(0.9).add(away.scale(0.35));
         if (this.level().noCollision(this, this.getBoundingBox().move(jump))) {
            this.setPos(this.getX() + jump.x, this.getY(), this.getZ() + jump.z);
         }

         this.setDeltaMovement(this.getDeltaMovement().add(side.scale(0.35)));
         this.level().broadcastEntityEvent(this, (byte)110);
         this.play((SoundEvent)ModSounds.CHARGE.get(), 0.9F, 1.9F);
         this.onDodge(attacker);
         Player owner = this.getOwner();
         if (owner != null) {
            owner.displayClientMessage(Component.translatable("dodge.librabeyblade." + this.kindId()).withStyle(ChatFormatting.AQUA), true);
         }

         return true;
      } else {
         return false;
      }
   }

   protected float dodgeChance() {
      return this.isBuffActive() ? 0.22F : 0.1F;
   }

   protected void onDodge(BeybladeEntity attacker) {
   }

   public float incomingMul() {
      return 1.0F;
   }

   public void stun(int ticks) {
      this.stunTicks = Math.max(this.stunTicks, ticks);
   }

   public boolean isBuffActive() {
      return this.isShielded();
   }

   public void tick() {
      super.tick();
      if (this.level().isClientSide()) {
         if (this.tickCount == 2 && this.getState() == STATE_BATTLE) {
            this.playAction(com.krolasyon.beyblade.client.BeyAnims.LAUNCH);
         }
         this.clientTick();
         com.krolasyon.beyblade.client.ClientHooks.tipFx(this);
      } else {
         this.serverTick();
      }
   }

   private void serverTick() {
      this.stateTicks++;
      if (this.wallSoundCooldown > 0) {
         this.wallSoundCooldown--;
      }

      if (this.clashCooldown > 0) {
         this.clashCooldown--;
      }

      if (this.home == null) {
         this.home = this.position();
      }

      Player owner = this.getOwner();
      if (this.getOwnerId() != null) {
         if (owner != null && owner.isAlive()) {
            this.ownerMissing = 0;
            if (this.getState() != 2 && owner.distanceToSqr(this) > 2304.0) {
               this.recall();
            }
         } else if (++this.ownerMissing > 100) {
            this.dropStored();
            this.discard();
            return;
         }
      }

      if (this.getY() < (double)(this.level().getMinBuildHeight() - 8)) {
         if (owner != null) {
            this.giveTo(owner);
         } else {
            this.dropStored();
         }

         this.discard();
      } else {
         switch (this.getState()) {
            case 1:
               this.tickDefeated(owner);
               break;
            case 2:
               this.tickRecall(owner);
               break;
            default:
               this.tickBattle(owner);
         }
      }
   }

   private void tickBattle(@Nullable Player owner) {
      float wobble = this.getWobble();
      float decay = 0.2F * this.modeDecay() * (this.isInWater() ? 4.0F : 1.0F) * (this.isShielded() ? 1.15F : 1.0F) * (1.0F + 1.8F * wobble);
      if (wobble > 0.0F) {
         this.tickWobble(owner, wobble);
      }

      if (++this.trainTicks >= 100) {
         this.trainTicks = 0;
         if (!this.isRival()) {
            this.addXp(this.target != null ? 2 : 1);
         }

         this.addSpirit(this.spinRatio() < 0.3F ? 8 : (this.target != null ? 3 : 1));
      }

      if ((this.isRival() || this.getOrder() == 10) && this.getSpirit() >= 100 && this.target != null) {
         this.releaseSpirit();
      }

      this.setSpin(this.getSpin() - decay);
      if (this.getSpin() <= this.getMaxSpin() * 0.02F) {
         this.lastStand();
      }

      if (this.getSpin() <= 0.0F) {
         this.defeat();
      } else {
         int ab = this.getAbility();
         if (ab == 0 && --this.targetRefresh <= 0) {
            this.targetRefresh = 10;
            this.target = this.findTarget(owner);
         }

         if (this.target != null
            && (!this.target.isAlive() || this.target.distanceToSqr(this) > 1024.0 || this.target instanceof BeybladeEntity b && !this.isEnemy(b))) {
            this.target = null;
         }

         int tid = this.target == null ? -1 : this.target.getId();
         if (tid != this.getTargetId()) {
            this.entityData.set(DATA_TARGET, tid);
         }

         boolean controlled = false;
         if (this.sclashCooldown > 0) {
            this.sclashCooldown--;
         }

         if (this.sclashTicks > 0 || this.startSpecialClash()) {
            controlled = true;
            this.tickSpecialClash();
         } else if (this.pendingAbility != 0) {
            controlled = this.tickRam(owner);
         } else if (ab != 0) {
            this.abilityTicks++;
            controlled = this.tickAbility(ab, this.abilityTicks, owner);
            if (this.abilityTicks >= this.duration(ab)) {
               this.endAbility();
            }
         } else if (this.isRival() && rivalAbilities) {
            this.rivalUseAbilities();
         } else if (this.getOrder() == 10 && this.target != null && this.tickCount % 25 == 0) {
            for (int k = 0; k < 3; k++) {
               int id = 1 + this.random.nextInt(this.abilityCount());
               if (this.cooldownRemaining(id) == 0 && this.abilityAvailable(id) && this.tryAbility(id, false)) {
                  break;
               }
            }
         }

         if (this.isShielded()) {
            this.tickBuff();
         }

         if (this.stunTicks > 0) {
            this.stunTicks--;
            controlled = true;
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
         }

         if (this.flungTicks > 0) {
            this.flungTicks--;
            controlled = true;
         }

         if (!this.frozen && this.tickRail(owner)) {
            controlled = true;
         }

         if (this.launchFlight) {
            if (!this.onGround() && this.tickCount <= 80) {
               controlled = true;
            } else {
               this.launchFlight = false;
            }
         }

         if (!controlled && !this.frozen) {
            this.steer(owner);
         }

         if (this.frozen && this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, 1.0, 0.7));
         }

         this.physics();
         this.stadiumPhysics();
         this.collideBeyblades();
         this.collideLiving(owner);
      }
   }

   @Nullable
   protected Entity findTarget(@Nullable Player owner) {
      BeybladeEntity best = null;
      double bestD = 576.0;

      for (BeybladeEntity b : this.level().getEntitiesOfClass(BeybladeEntity.class, this.getBoundingBox().inflate(24.0), this::isEnemy)) {
         double d = b.distanceToSqr(this);
         if (d < bestD) {
            bestD = d;
            best = b;
         }
      }

      if (best != null) {
         return best;
      } else {
         if (owner != null) {
            LivingEntity m = owner.getLastHurtMob();
            if (m != null && m.isAlive() && owner.tickCount - owner.getLastHurtMobTimestamp() < 300 && m.distanceToSqr(this) < 576.0 && !(m instanceof Player)) {
               return m;
            }

            LivingEntity by = owner.getLastHurtByMob();
            if (by != null && by.isAlive() && !(by instanceof Player) && by.distanceToSqr(this) < 576.0) {
               return by;
            }
         }

         return null;
      }
   }

   private void steer(@Nullable Player owner) {
      Vec3 v = this.getDeltaMovement();
      double ratio = (double)this.spinRatio();
      double mul = (0.55 + 0.45 * ratio) * (this.onGround() ? 1.0 : 0.25) * this.buffSpeedMul() * this.modeSpeed();
      boolean arena = this.stadium != null && this.inBowl;
      Vec3 base = arena ? this.stadium.position() : (owner != null ? owner.position() : (this.home != null ? this.home : this.position()));
      int order = this.getOrder();
      Entity tg = this.target;
      if (order == 5) {
         tg = null;
      }

      if (order == 6 && tg != null && owner != null && tg.distanceToSqr(owner) > 49.0) {
         tg = null;
      }

      int mode = this.getMode();
      if (order == 8) {
         this.setDeltaMovement(v.multiply(0.86, 1.0, 0.86));
      } else if (this.lockTicks > 0
         && this.lockPartner != null
         && this.lockPartner.isAlive()
         && this.lockPartner.getState() == 0
         && this.lockPartner.distanceToSqr(this) < 4.0) {
         this.lockTicks--;
         Vec3 to = flat(this.lockPartner.position().subtract(this.position()));
         Vec3 dir = to.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : to.normalize();
         Vec3 desired = dir.scale(0.22).add(new Vec3(-dir.z, 0.0, dir.x).scale(0.1 * (double)this.spinDir()));
         this.setDeltaMovement(v.add((desired.x - v.x) * 0.4, 0.0, (desired.z - v.z) * 0.4));
         this.clashCooldown = Math.max(this.clashCooldown, 2);
         if (this.tickCount % 2 == 0) {
            this.level().broadcastEntityEvent(this, (byte)67);
         }

         if (this.lockTicks == 0) {
            this.resolveLock(this.lockPartner);
         }
      } else {
         this.lockTicks = 0;
         if (this.retreatTicks > 0 && tg != null) {
            this.retreatTicks--;
            Vec3 tox = flat(tg.position().subtract(this.position()));
            Vec3 dirx = tox.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : tox.normalize();
            Vec3 desiredx = dirx.scale(-0.42 * mul).add(new Vec3(dirx.z, 0.0, -dirx.x).scale(0.12 * (double)this.spinDir()));
            this.setDeltaMovement(v.add((desiredx.x - v.x) * 0.3, 0.0, (desiredx.z - v.z) * 0.3));
            if (this.retreatTicks == 0) {
               this.dashCooldown = 0;
            }
         } else if (tg != null && order != 3 && order != 4 && order != 11) {
            Vec3 tox = flat(tg.position().subtract(this.position()));
            double dist = tox.length();
            Vec3 dirx = dist > 0.001 ? tox.scale(1.0 / dist) : new Vec3(1.0, 0.0, 0.0);
            switch (order) {
               case 2: {
                  Vec3 away = dirx.scale(-0.3).add(new Vec3(dirx.z, 0.0, -dirx.x).scale(0.14));
                  if (arena) {
                     Vec3 fromC = flat(this.position().subtract(base));
                     if (fromC.length() > 8.4) {
                        away = away.add(fromC.normalize().scale(-0.25));
                     }
                  }

                  Vec3 d = away.scale(mul);
                  this.setDeltaMovement(v.add((d.x - v.x) * 0.14, 0.0, (d.z - v.z) * 0.14));
                  return;
               }
               case 7:
                  if (this.rushPhase > 0) {
                     this.rushPhase--;
                     Vec3 dx = dirx.scale(-0.3 * mul);
                     this.setDeltaMovement(v.add((dx.x - v.x) * 0.2, 0.0, (dx.z - v.z) * 0.2));
                     if (this.rushPhase == 0 && this.onGround()) {
                        this.setDeltaMovement(this.getDeltaMovement().add(dirx.scale(0.85 * (0.6 + 0.4 * ratio) * this.buffSpeedMul())));
                        this.dashTicks = 12;
                     }

                     return;
                  } else {
                     if (this.dashTicks > 0) {
                        this.dashTicks--;
                        return;
                     }

                     if (dist < 1.4) {
                        this.rushPhase = 12;
                     } else {
                        this.setDeltaMovement(this.orbit(v, tg.position(), 0.2, 0.05, mul * 1.2, 0.15));
                     }

                     return;
                  }
               case 9: {
                  Vec3 side = new Vec3(dirx.z, 0.0, -dirx.x).scale(Math.sin((double)this.tickCount * 0.38) * 0.42);
                  Vec3 d = dirx.scale(0.3).add(side).scale(mul);
                  this.setDeltaMovement(v.add((d.x - v.x) * 0.2, 0.0, (d.z - v.z) * 0.2));
                  return;
               }
               case 12:
                  Vec3 tv = flat(tg.getDeltaMovement());
                  boolean behind = tv.lengthSqr() > 0.001 && tv.normalize().dot(dirx) > 0.3;
                  if (behind && --this.dashCooldown <= 0 && this.onGround()) {
                     this.setDeltaMovement(v.add(dirx.scale(0.75 * (0.6 + 0.4 * ratio))));
                     this.dashTicks = 8;
                     this.dashCooldown = 14;
                     return;
                  } else {
                     if (this.dashTicks > 0) {
                        this.dashTicks--;
                        return;
                     }

                     this.setDeltaMovement(this.orbit(v, tg.position(), 2.0, 0.4, mul, 0.15));
                     return;
                  }
               default:
                  double r = 0.3;
                  int dashMin = 6;
                  int dashRand = 8;
                  double dashPow = 0.58;
                  if (order == 1 || order == 10 || order == 13) {
                     r = 0.1;
                     dashMin = 3;
                     dashRand = 5;
                     dashPow = 0.68;
                  } else if (mode == 0) {
                     r = 0.15;
                     dashMin = 4;
                     dashRand = 6;
                     dashPow = 0.66;
                  } else if (mode == 1) {
                     r = 0.6;
                     dashMin = 10;
                     dashRand = 10;
                     dashPow = 0.5;
                  } else if (mode == 2) {
                     r = 0.8;
                     dashMin = 12;
                     dashRand = 12;
                     dashPow = 0.46;
                  }

                  if (--this.dashCooldown <= 0 && this.onGround()) {
                     this.setDeltaMovement(v.add(dirx.scale(dashPow * (0.6 + 0.4 * ratio) * this.buffSpeedMul())));
                     this.dashTicks = 5;
                     this.dashCooldown = (int)((double)(dashMin + this.random.nextInt(dashRand)) / this.buffSpeedMul());
                  } else if (this.dashTicks > 0) {
                     this.dashTicks--;
                  } else {
                     this.setDeltaMovement(this.orbit(v, tg.position(), r, 0.22, mul, 0.14));
                  }
            }
         } else {
            Vec3 c = order != 5 && order != 6 ? base : (owner != null ? owner.position() : base);
            double rx;
            double tan;
            switch (order) {
               case 3:
                  rx = arena ? 8.4 : 5.0;
                  tan = 0.95;
                  break;
               case 4:
                  rx = 0.25;
                  tan = 0.04;
                  break;
               case 5:
                  rx = 1.8;
                  tan = 0.18;
                  break;
               case 6:
                  rx = 2.2;
                  tan = 0.22;
                  break;
               case 7:
               case 8:
               case 9:
               case 10:
               default:
                  rx = arena ? 3.4 : 2.8;
                  tan = 0.25;
                  break;
               case 11:
                  rx = arena ? 1.2 : 1.5;
                  tan = 0.1;
            }

            this.setDeltaMovement(this.orbit(v, c, rx, tan, order == 3 ? Math.max(mul, 0.9 * this.modeSpeed()) : mul, order == 3 ? 0.3 : 0.12));
         }
      }
   }

   private Vec3 orbit(Vec3 v, Vec3 c, double r, double tan, double mul, double gain) {
      Vec3 to = flat(c.subtract(this.position()));
      double dist = to.length();
      if (dist < 0.001) {
         return v;
      } else {
         Vec3 dir = to.scale(1.0 / dist);
         Vec3 tangent = new Vec3(dir.z, 0.0, -dir.x).scale((double)this.spinDir());
         double radial = Mth.clamp((dist - r) * 0.12, -0.18, dist > 12.0 ? 0.55 : 0.32);
         Vec3 desired = tangent.scale(tan).add(dir.scale(radial)).scale(mul);
         return v.add((desired.x - v.x) * gain, 0.0, (desired.z - v.z) * gain);
      }
   }

   private boolean tickRam(@Nullable Player owner) {
      int id;
      this.ramTicks++;
      Entity tg = this.target;
      id = this.pendingAbility;
      label25:
      if (tg != null && tg.isAlive() && this.ramTicks <= 50) {
         if (tg instanceof BeybladeEntity b && b.getState() != 0) {
            break label25;
         }

         Vec3 to = flat(tg.position().subtract(this.position()));
         double dist = to.length();
         if (dist <= 1.1999999761581421 && Math.abs(tg.getY() - this.getY()) < 1.2) {
            this.pendingAbility = 0;
            this.ramImpact(tg);
            this.beginAbility(id, owner);
            return true;
         }

         Vec3 dir = to.scale(1.0 / Math.max(0.001, dist));
         double sp = 0.62 * (0.7 + 0.3 * (double)this.spinRatio()) * this.modeSpeed();
         Vec3 v = this.getDeltaMovement();
         this.setDeltaMovement(v.x + (dir.x * sp - v.x) * 0.45, v.y, v.z + (dir.z * sp - v.z) * 0.45);
         return true;
      }

      this.pendingAbility = 0;
      this.beginAbility(id, owner);
      return true;
   }

   private void ramImpact(Entity tg) {
      Vec3 dir = flat(tg.position().subtract(this.position()));
      dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : dir.normalize();
      if (tg instanceof BeybladeEntity b) {
         if (!b.tryDodge(this)) {
            b.damageSpin(45.0F * this.powerMul(), this);
         }

         b.setDeltaMovement(b.getDeltaMovement().add(dir.scale(0.45 * b.knockMul())).add(0.0, 0.08, 0.0));
         this.level().broadcastEntityEvent(b, (byte)61);
      } else if (tg instanceof LivingEntity l && this.isHostileLiving(l)) {
         l.hurt(this.damageSources().thrown(this, this.getOwner()), 6.0F);
         l.knockback(0.8, -dir.x, -dir.z);
      }

      this.setDeltaMovement(this.getDeltaMovement().scale(0.2));
      this.level().broadcastEntityEvent(this, (byte)61);
      this.level()
         .playSound(
            null,
            this.getX(),
            this.getY(),
            this.getZ(),
            (SoundEvent)ModSounds.CLASH_HEAVY.get(),
            SoundSource.NEUTRAL,
            1.4F,
            0.8F + this.random.nextFloat() * 0.2F
         );
   }

   protected static Vec3 flat(Vec3 v) {
      return new Vec3(v.x, 0.0, v.z);
   }

   private void physics() {
      if (debugFlight && this.tickCount < 40 && this.tickCount % 2 == 0 && this.getOwnerId() != null) {
         LogUtils.getLogger()
            .info(
               "[AUTOTEST] flight t={} pos={} vel={} ground={} hcol={}",
               new Object[]{this.tickCount, this.position(), this.getDeltaMovement(), this.onGround(), this.horizontalCollision}
            );
      }

      Vec3 v = this.getDeltaMovement();
      if (!this.isNoGravity()) {
         v = v.add(0.0, -0.06, 0.0);
      }

      Vec3 p0 = this.position();
      boolean wasGround = this.onGround();
      this.setDeltaMovement(v);
      this.move(MoverType.SELF, v);
      Vec3 moved = this.position().subtract(p0);
      v = this.getDeltaMovement();
      if (this.horizontalCollision) {
         boolean hx = Math.abs(moved.x - v.x) > 1.0E-4;
         boolean hz = Math.abs(moved.z - v.z) > 1.0E-4;
         v = new Vec3(hx ? -v.x * 0.75 : v.x, v.y, hz ? -v.z * 0.75 : v.z);
         double speed = flat(v).length();
         if (speed > 0.14 && this.wallSoundCooldown == 0) {
            this.wallSoundCooldown = 6;
            this.play((SoundEvent)ModSounds.WALL_HIT.get(), 0.7F, 0.9F + this.random.nextFloat() * 0.3F);
            Px.send(
               (ServerLevel)this.level(),
               ParticleTypes.ELECTRIC_SPARK,
               this.getX() + v.x * 3.0,
               this.getY() + 0.3,
               this.getZ() + v.z * 3.0,
               8,
               0.1,
               0.1,
               0.1,
               0.3
            );
            this.setSpin(this.getSpin() - 4.0F);
            this.level().broadcastEntityEvent(this, (byte)64);
         }
      }

      if (this.onGround() && !wasGround && v.y < -0.32) {
         v = new Vec3(v.x, -v.y * 0.3, v.z);
         this.play((SoundEvent)ModSounds.LAND.get(), 0.8F, 1.0F);
         this.level().broadcastEntityEvent(this, (byte)62);
      }

      if (this.onGround()) {
         BeyPart.Tip tipK = this.parts().tip();
         double fr = com.krolasyon.beyblade.parts.TipPhysics.groundFriction(tipK);
         v = new Vec3(v.x * fr, v.y, v.z * fr);
         if (this.getState() == 0) {
            // the tip drives the bey around the bowl: flat and rubber tips trace the flower pattern, sharp sits still
            double drive = com.krolasyon.beyblade.parts.TipPhysics.drive(tipK, this.spinRatio(), this.spinDir());
            if (drive > 0.0) {
               this.tipPhase += 0.07F * (float)this.spinDir();
               v = v.add(Math.cos(this.tipPhase) * drive, 0.0, Math.sin(this.tipPhase) * drive);
            }
            if (tipK == BeyPart.Tip.QUAKE && this.tickCount % 9 == 0) {
               // Quake: the uneven tip kicks the bey in random directions
               double a = this.random.nextDouble() * Math.PI * 2.0;
               v = v.add(Math.cos(a) * 0.05, 0.04, Math.sin(a) * 0.05);
            }
         }
      } else {
         v = new Vec3(v.x * 0.99, v.y * 0.98, v.z * 0.99);
      }

      this.setDeltaMovement(v);
   }

   private void collideBeyblades() {
      for (BeybladeEntity o : this.level().getEntitiesOfClass(BeybladeEntity.class, this.getBoundingBox().inflate(0.5), b -> b != this && b.getState() != 2)) {
         if (o.getId() >= this.getId() || o.getState() != 0) {
            this.resolve(o);
         }
      }
   }

   private void resolve(BeybladeEntity o) {
      double dx = o.getX() - this.getX();
      double dz = o.getZ() - this.getZ();
      if (!(Math.abs(o.getY() - this.getY()) > 0.66F)) {
         double dist = Math.sqrt(dx * dx + dz * dz);
         double min = 0.9F;
         if (!(dist >= min)) {
            if (dist < 1.0E-4) {
               dx = this.random.nextDouble() - 0.5;
               dz = this.random.nextDouble() - 0.5;
               dist = Math.sqrt(dx * dx + dz * dz);
            }

            double nx = dx / dist;
            double nz = dz / dist;
            double overlap = min - dist + 0.01;
            shift(this, -nx * overlap * 0.5, -nz * overlap * 0.5);
            shift(o, nx * overlap * 0.5, nz * overlap * 0.5);
            double rx = o.getX() - this.getX();
            double rz = o.getZ() - this.getZ();
            if (rx * rx + rz * rz < (min - 0.02) * (min - 0.02)) {
               double d2 = Math.sqrt(rx * rx + rz * rz);
               double rest = min - d2 + 0.01;
               shift(o, nx * rest, nz * rest);
               shift(this, -nx * rest, -nz * rest);
            }

            Vec3 va = this.getDeltaMovement();
            Vec3 vb = o.getDeltaMovement();
            double rel = (vb.x - va.x) * nx + (vb.z - va.z) * nz;
            boolean fight = this.isEnemy(o) && this.getState() == 0 && o.getState() == 0;
            boolean opposite = this.spinDir() != o.spinDir();
            if (fight && (this.clashCooldown > 0 || o.clashCooldown > 0) && rel >= -0.05) {
               o.damageSpin(0.7F * this.powerMul(), this);
               this.damageSpin(0.7F * o.powerMul(), o);
               double tx = -nz;
               double s = opposite ? 0.0 : 0.012 * (double)this.spinDir();
               this.setDeltaMovement(this.getDeltaMovement().add(-nx * 0.012 + tx * s, 0.0, -nz * 0.012 + nx * s));
               o.setDeltaMovement(o.getDeltaMovement().add(nx * 0.012 - tx * s, 0.0, nz * 0.012 - nx * s));
               if (this.tickCount % 5 == 0) {
                  this.level()
                     .playSound(
                        null,
                        this.getX() + nx * 0.45F,
                        this.getY() + 0.3,
                        this.getZ() + nz * 0.45F,
                        (SoundEvent)ModSounds.GRIND.get(),
                        SoundSource.NEUTRAL,
                        0.55F,
                        0.9F + this.random.nextFloat() * 0.25F
                     );
                  this.level().broadcastEntityEvent(this, (byte)67);
               }
            } else if (!(rel >= 0.0) || fight) {
               if (rel > -0.02 && fight) {
                  rel = -0.02;
               }

               double e = fight ? 0.55 : 0.3;
               double j = -(1.0 + e) * rel * 0.5;
               double kick = fight ? (opposite ? 0.035 : 0.06) : 0.0;
               double tx = -nz;
               double side = fight && !opposite ? 0.05 * (double)this.spinDir() : 0.0;
               double ka = this.knockMul();
               double kb = o.knockMul();
               va = va.add((-nx * (j + kick) + tx * side) * ka, fight ? 0.02 : 0.0, (-nz * (j + kick) + nx * side) * ka);
               vb = vb.add((nx * (j + kick) - tx * side) * kb, fight ? 0.02 : 0.0, (nz * (j + kick) - nx * side) * kb);
               if (fight && this.getOrder() == 13) {
                  vb = vb.add(0.0, 0.34, 0.0);
               }

               if (fight && o.getOrder() == 13) {
                  va = va.add(0.0, 0.34, 0.0);
               }

               this.setDeltaMovement(va);
               o.setDeltaMovement(vb);
               if (fight && this.clashCooldown <= 0 && o.clashCooldown <= 0) {
                  this.clashCooldown = 4;
                  o.clashCooldown = 4;
                  double impact = -rel;
                  boolean heavy = impact > 0.42;
                  float base = (float)(10.0 + 150.0 * impact);
                  float toO = base * (0.6F + 0.6F * this.spinRatio()) * this.buffAttackMul() * this.powerMul() * this.partsClashMul(o);
                  float toMe = base * (0.6F + 0.6F * o.spinRatio()) * o.buffAttackMul() * o.powerMul() * o.partsClashMul(this);
                  if (this.xdashTicks > 0) {
                     toO *= 1.5F;
                  }

                  if (o.xdashTicks > 0) {
                     toMe *= 1.5F;
                  }

                  if (this.xdashTicks > 0) {
                     o.setDeltaMovement(o.getDeltaMovement().add(nx * 0.7 * Mth.clamp(o.knockMul(), 0.5, 1.3), 0.1, nz * 0.7 * Mth.clamp(o.knockMul(), 0.5, 1.3)));
                     o.flungTicks = 10;
                     this.xdashTicks = 0;
                  }

                  if (o.xdashTicks > 0) {
                     this.setDeltaMovement(
                        this.getDeltaMovement().add(-nx * 0.7 * Mth.clamp(this.knockMul(), 0.5, 1.3), 0.1, -nz * 0.7 * Mth.clamp(this.knockMul(), 0.5, 1.3))
                     );
                     this.flungTicks = 10;
                     o.xdashTicks = 0;
                  }

                  if (this.heightEdge(o) && this.partsUpperKick()) {
                     o.setDeltaMovement(o.getDeltaMovement().add(0.0, 0.22, 0.0));
                  }

                  if (o.heightEdge(this) && o.partsUpperKick()) {
                     this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.22, 0.0));
                  }

                  if (toO >= 15.0F) {
                     this.addXp(1);
                  }

                  if (toMe >= 15.0F) {
                     o.addXp(1);
                  }

                  if (o.tryDodge(this)) {
                     toO = 0.0F;
                  }

                  if (this.tryDodge(o)) {
                     toMe = 0.0F;
                  }

                  if (o.isShielded() && o.buffReflects()) {
                     toMe += toO * 0.6F;
                     toO *= 0.2F;
                     this.setDeltaMovement(this.getDeltaMovement().add(-nx * 0.35, 0.08, -nz * 0.35));
                     o.shieldHit();
                  }

                  if (this.isShielded() && this.buffReflects()) {
                     toO += toMe * 0.6F;
                     toMe *= 0.2F;
                     o.setDeltaMovement(o.getDeltaMovement().add(nx * 0.35, 0.08, nz * 0.35));
                     this.shieldHit();
                  }

                  o.damageSpin(toO, this);
                  this.damageSpin(toMe, o);
                  this.onClash(o, toO, toMe);
                  o.onClash(this, toMe, toO);
                  ServerLevel sl = (ServerLevel)this.level();
                  if (opposite) {
                     BeybladeEntity thief = this.spinDir() < 0 ? this : o;
                     thief.healSpin((thief == this ? toO : toMe) * 0.4F);
                     Px.send(sl, STEAL, thief.getX(), thief.getY() + 0.4, thief.getZ(), 10, 0.35, 0.2, 0.35, 0.0);
                  }

                  double cx = this.getX() + nx * 0.45F;
                  double cy = this.getY() + 0.38;
                  double cz = this.getZ() + nz * 0.45F;
                  Px.send(sl, ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, heavy ? 28 : 12, 0.08, 0.08, 0.08, heavy ? 0.7 : 0.45);
                  Px.send(sl, ParticleTypes.CRIT, cx, cy, cz, heavy ? 14 : 6, 0.05, 0.05, 0.05, 0.6);
                  if (heavy) {
                     Px.send(sl, ParticleTypes.END_ROD, cx, cy, cz, 14, 0.1, 0.1, 0.1, 0.25);
                     Px.send(sl, ParticleTypes.FIREWORK, cx, cy, cz, 10, 0.05, 0.05, 0.05, 0.25);
                  }

                  this.level()
                     .playSound(
                        null,
                        cx,
                        cy,
                        cz,
                        heavy ? (SoundEvent)ModSounds.CLASH_HEAVY.get() : (SoundEvent)ModSounds.CLASH.get(),
                        SoundSource.NEUTRAL,
                        heavy ? 2.2F : 1.4F,
                        0.8F + this.random.nextFloat() * 0.3F
                     );
                  this.level().broadcastEntityEvent(this, (byte)(heavy ? 61 : 60));
                  this.level().broadcastEntityEvent(o, (byte)(heavy ? 61 : 60));
                  if (heavy) {
                     BeybladeEntity loser = toO >= toMe ? o : this;
                     BeybladeEntity winner = loser == o ? this : o;
                     loser.shockwave(winner, Math.min(1.0, impact));
                  }

                  this.addSpirit(heavy ? 6 : 3);
                  o.addSpirit(heavy ? 6 : 3);
                  float r = this.random.nextFloat();
                  if (r < 0.3F && this.lockTicks == 0 && o.lockTicks == 0) {
                     int n = 24 + this.random.nextInt(18);
                     this.lockTicks = n;
                     o.lockTicks = n;
                     this.lockPartner = o;
                     o.lockPartner = this;
                     this.level().playSound(null, cx, cy, cz, (SoundEvent)ModSounds.GRIND.get(), SoundSource.NEUTRAL, 1.4F, 0.8F);
                  } else if (r < 0.7F) {
                     this.retreatTicks = 6 + this.random.nextInt(8);
                     o.retreatTicks = 6 + this.random.nextInt(8);
                  }

                  if (o.target == null) {
                     o.target = this;
                  }

                  if (this.target == null) {
                     this.target = o;
                  }
               }
            }
         }
      }
   }

   public int outZone() {
      return this.outZone;
   }

   public boolean isXtremeDashing() {
      return this.xdashTicks > 0;
   }

   private boolean tickRail(@Nullable Player owner) {
      if (this.railCooldown > 0) {
         this.railCooldown--;
      }

      if (this.xdashTicks > 0) {
         this.xdashTicks--;
         return this.xdashTicks > 4;
      } else if (this.stadium != null && this.inBowl && this.getState() == 0) {
         double dx = this.getX() - this.stadium.getX();
         double dz = this.getZ() - this.stadium.getZ();
         double r = Math.sqrt(dx * dx + dz * dz);
         if (r < 0.001) {
            return false;
         } else {
            double nx = dx / r;
            double nz = dz / r;
            double tx = -nz;
            double deg = StadiumEntity.deg(dx, dz);
            Vec3 v = this.getDeltaMovement();
            double vt = v.x * tx + v.z * nx;
            BeyPart.Tip tip = this.parts().tip();
            boolean grippy = com.krolasyon.beyblade.parts.TipPhysics.grippy(tip);
            boolean point = com.krolasyon.beyblade.parts.TipPhysics.point(tip);
            if (this.railTicks == 0) {
               double need = grippy ? 0.14 : (point ? 0.32 : 0.2);
               if (this.railCooldown <= 0 && this.onGround() && !(r < 9.45F) && !(r > 10.35F) && StadiumEntity.onRail(deg) && !(Math.abs(vt) < need)) {
                  this.railTicks = 1;
                  this.railDir = vt > 0.0 ? 1 : -1;
                  this.railSpeed = Math.abs(vt);
                  this.level().broadcastEntityEvent(this, (byte)86);
                  this.play((SoundEvent)ModSounds.GRIND.get(), 1.4F, 1.6F);
                  this.play((SoundEvent)ModSounds.CHARGE.get(), 1.0F, 1.8F);
                  return true;
               } else {
                  return false;
               }
            } else {
               this.railTicks++;
               double top = (grippy ? 1.3 : 1.15) * (0.75 + 0.25 * (double)this.spinRatio());
               this.railSpeed = Math.min(top, this.railSpeed + (grippy ? 0.1 : 0.075));
               double rr = 9.9F;
               Vec3 tan = new Vec3(tx * (double)this.railDir, 0.0, nx * (double)this.railDir);
               this.setDeltaMovement(tan.scale(this.railSpeed).add(nx * (rr - r) * 0.5, v.y, nz * (rr - r) * 0.5));
               if (this.railTicks % 3 == 0) {
                  this.level().broadcastEntityEvent(this, (byte)86);
                  this.play((SoundEvent)ModSounds.GRIND.get(), 0.9F, 1.5F + (float)this.railSpeed * 0.3F);
               }

               Vec3 aim = null;
               if (this.target != null && this.target.isAlive() && this.railTicks > 5) {
                  Vec3 to = flat(this.target.position().subtract(this.position()));
                  if (to.length() < 12.0 && to.normalize().dot(tan) > 0.75) {
                     aim = to.normalize();
                  }
               }

               boolean railEnds = !StadiumEntity.onRail(deg + (double)(this.railDir * 14));
               if (aim != null || railEnds || this.railTicks > 26) {
                  this.xtremeDash(aim != null ? aim : tan.add(-nx * 0.5, 0.0, -nz * 0.5), owner);
               }

               return true;
            }
         }
      } else {
         this.railTicks = 0;
         return false;
      }
   }

   private void xtremeDash(Vec3 dir, @Nullable Player owner) {
      this.railTicks = 0;
      this.railCooldown = 80;
      this.xdashTicks = 16;
      this.setDeltaMovement(dir.normalize().scale(Math.max(this.railSpeed, 0.9) * 1.05).add(0.0, 0.02, 0.0));
      this.level().broadcastEntityEvent(this, (byte)87);
      this.play((SoundEvent)ModSounds.LAUNCH.get(), 2.0F, 1.4F);
      this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 1.2F, 1.6F);
      this.addSpirit(4);
      if (owner != null) {
         owner.displayClientMessage(Component.translatable("msg.librabeyblade.xdash").withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD}), true);
      }
   }

   private boolean pocketed(double vr) {
      if (this.railTicks <= 0 && this.xdashTicks <= 0 && this.level().getGameTime() - this.lastAttackTick <= 30L) {
         double need = 0.7 + 0.35 * (double)this.spinRatio();
         if (vr < need) {
            return false;
         } else {
            double chance = Mth.clamp((vr - need) / 0.8 + 0.25, 0.0, 0.85);
            return this.random.nextDouble() < chance;
         }
      } else {
         return false;
      }
   }

   private void knockOut(StadiumEntity st, int zone, double nx, double nz) {
      this.outZone = zone;
      this.contained = false;
      if (debugFlight) {
         LogUtils.getLogger().info("[AUTOTEST] pocket {} {}", zone == 2 ? "XTREME" : "OVER", this.kindId());
      }

      this.railTicks = this.xdashTicks = 0;
      double pr = 11.95;
      this.setPos(st.getX() + nx * pr, st.getY() + 1.7F + 0.05, st.getZ() + nz * pr);
      this.setDeltaMovement(0.0, 0.05, 0.0);
      this.level().broadcastEntityEvent(this, (byte)88);
      this.play((SoundEvent)ModSounds.METEOR_IMPACT.get(), 2.4F, zone == 2 ? 0.8F : 1.1F);
      this.play((SoundEvent)ModSounds.WALL_HIT.get(), 2.0F, 0.6F);
      Component title = Component.translatable(
         zone == 2 ? "msg.librabeyblade.xtreme_finish" : "msg.librabeyblade.over_finish", new Object[]{Component.translatable(this.titleKey())}
      );

      for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(40.0))) {
         p.displayClientMessage(title.copy().withStyle(new ChatFormatting[]{zone == 2 ? ChatFormatting.GOLD : ChatFormatting.AQUA, ChatFormatting.BOLD}), true);
      }

      this.defeat();
   }

   public float getWobble() {
      return this.level().isClientSide() ? (Float)this.entityData.get(DATA_WOBBLE) : this.wobbleNow;
   }

   public void setLaunchWobble(float w) {
      this.wobbleNow = Mth.clamp(w, 0.0F, 1.0F);
      this.entityData.set(DATA_WOBBLE, this.wobbleNow);
      this.wobbleLanded = false;
   }

   private void tickWobble(@Nullable Player owner, float w) {
      if (this.onGround()) {
         if (!this.wobbleLanded) {
            this.wobbleLanded = true;
            if (w > 0.55F) {
               this.setSpin(this.getSpin() - this.getMaxSpin() * 0.14F * w);
               this.play((SoundEvent)ModSounds.WALL_HIT.get(), 1.0F, 0.6F);
               this.play((SoundEvent)ModSounds.GRIND.get(), 1.2F, 0.7F);
               this.level().broadcastEntityEvent(this, (byte)60);
               if (owner != null) {
                  owner.displayClientMessage(Component.translatable("msg.librabeyblade.wobble_land").withStyle(ChatFormatting.RED), true);
               }
            }
         }

         Vec3 v = this.getDeltaMovement();
         this.setDeltaMovement(v.add((double)((this.random.nextFloat() - 0.5F) * 0.1F * w), 0.0, (double)((this.random.nextFloat() - 0.5F) * 0.1F * w)));
      }

      this.wobbleNow = Math.max(0.0F, w - 0.0028F);
      if (this.wobbleNow == 0.0F || this.tickCount % 5 == 0) {
         this.entityData.set(DATA_WOBBLE, this.wobbleNow);
      }
   }

   public int getSpecialClashPartner() {
      return (Integer)this.entityData.get(DATA_SCLASH);
   }

   public static double sclashMeet(float ratio, float k) {
      double end = Mth.clamp((double)ratio, 0.15, 0.85);
      double e = (double)(k * k * (3.0F - 2.0F * k));
      return 0.5 + (end - 0.5) * e;
   }

   public float getSpecialClashRatio() {
      return (Float)this.entityData.get(DATA_SCLASH_RATIO);
   }

   private int specialMove() {
      if (this.pendingAbility != 0) {
         return this.pendingAbility;
      } else {
         int ab = this.getAbility();
         return ab != 0 && ab != this.buffAbility() ? ab : 0;
      }
   }

   private float specialPower(boolean initiative, boolean perfect) {
      int id = this.specialMove();
      float tier = id >= this.abilityCount() ? 1.35F : 1.0F + 0.06F * (float)id;
      double v = Math.sqrt(this.getDeltaMovement().x * this.getDeltaMovement().x + this.getDeltaMovement().z * this.getDeltaMovement().z);
      float speed = 0.85F + 0.3F * (float)Mth.clamp(v / 0.6, 0.0, 1.0) + 0.1F * (float)(this.modeSpeed() - 1.0);
      float p = tier * this.powerMul() * (0.55F + 0.45F * this.spinRatio()) * speed;
      if (perfect) {
         p *= 1.1F;
      } else if (initiative) {
         p *= 1.12F;
      }

      if (this.isSpiritActive()) {
         p *= 1.2F;
      }

      return p * (0.9F + 0.2F * this.random.nextFloat());
   }

   private boolean startSpecialClash() {
      if (this.sclashCooldown <= 0 && this.specialMove() != 0 && this.tickCount % 2 == 0) {
         long now = this.level().getGameTime();
         if (now - this.specialStart > 60L) {
            return false;
         } else {
            BeybladeEntity best = null;
            double bd = 196.0;

            for (BeybladeEntity b : this.level().getEntitiesOfClass(BeybladeEntity.class, this.getBoundingBox().inflate(14.0, 6.0, 14.0))) {
               if (b != this
                  && this.isEnemy(b)
                  && b.sclashTicks <= 0
                  && b.sclashCooldown <= 0
                  && b.specialMove() != 0
                  && Math.abs(b.specialStart - this.specialStart) <= 40L) {
                  double d = b.distanceToSqr(this);
                  if (d < bd) {
                     bd = d;
                     best = b;
                  }
               }
            }

            if (best == null) {
               return false;
            } else {
               long dt = this.specialStart - best.specialStart;
               boolean perfect = Math.abs(dt) <= 4L;
               this.sclashPower = this.specialPower(!perfect && dt < 0L, perfect);
               best.sclashPower = best.specialPower(!perfect && dt > 0L, perfect);
               float ratio = this.sclashPower / Math.max(0.01F, this.sclashPower + best.sclashPower);
               this.sclashPartner = best;
               best.sclashPartner = this;
               this.sclashLeader = true;
               best.sclashLeader = false;
               this.sclashPerfect = best.sclashPerfect = perfect;
               this.sclashTicks = 30;
               best.sclashTicks = 33;
               this.entityData.set(DATA_SCLASH, best.getId());
               best.entityData.set(DATA_SCLASH, this.getId());
               this.entityData.set(DATA_SCLASH_RATIO, ratio);
               best.entityData.set(DATA_SCLASH_RATIO, 1.0F - ratio);
               this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 2.6F, 0.5F);
               this.play((SoundEvent)ModSounds.THUNDER_STRIKE.get(), 1.8F, 1.3F);
               this.announceNear(
                  Component.translatable("msg.librabeyblade.sclash", new Object[]{Component.translatable(this.titleKey()), Component.translatable(best.titleKey())})
                     .append(perfect ? Component.literal(" ").append(Component.translatable("msg.librabeyblade.sclash_perfect")) : Component.empty())
                     .withStyle(new ChatFormatting[]{ChatFormatting.GOLD, ChatFormatting.BOLD})
               );
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private void tickSpecialClash() {
      BeybladeEntity o = this.sclashPartner;
      if (o != null && o.isAlive() && o.getState() == 0 && this.getState() == 0) {
         this.sclashTicks--;
         Vec3 to = flat(o.position().subtract(this.position()));
         double dist = to.length();
         Vec3 dir = dist < 0.001 ? new Vec3(1.0, 0.0, 0.0) : to.scale(1.0 / dist);
         if (dist < 5.0 && 30 - this.sclashTicks < 10) {
            this.setDeltaMovement(dir.scale(-0.32).add(0.0, this.getDeltaMovement().y, 0.0));
         } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.4, 1.0, 0.4).add(dir.scale(0.015)));
         }

         if (this.sclashLeader) {
            if (this.sclashTicks % 6 == 0) {
               this.play((SoundEvent)ModSounds.GRIND.get(), 1.4F, 1.4F + this.random.nextFloat() * 0.3F);
               this.play((SoundEvent)ModSounds.THUNDER_STRIKE.get(), 0.7F, 1.8F);
            }

            if (this.sclashTicks <= 0) {
               this.resolveSpecialClash(o);
            }
         } else if (this.sclashTicks <= 0) {
            this.endSpecialClash();
         }
      } else {
         this.endSpecialClash();
      }
   }

   private void endSpecialClash() {
      this.sclashTicks = 0;
      this.sclashPartner = null;
      this.sclashCooldown = 100;
      this.entityData.set(DATA_SCLASH, -1);
   }

   private void resolveSpecialClash(BeybladeEntity o) {
      float a = this.sclashPower;
      float b = o.sclashPower;
      float margin = Math.max(a, b) / Math.max(0.01F, Math.min(a, b));
      boolean draw = margin < 1.07F;
      BeybladeEntity win = a >= b ? this : o;
      BeybladeEntity lose = win == this ? o : this;
      this.level().broadcastEntityEvent(this, (byte)(draw ? 105 : 104));
      double cx = (this.getX() + o.getX()) / 2.0;
      double cy = this.getY() + 0.5;
      double cz = (this.getZ() + o.getZ()) / 2.0;
      ServerLevel sl = (ServerLevel)this.level();
      Px.send(sl, ParticleTypes.EXPLOSION, cx, cy, cz, 3, 0.4, 0.2, 0.4, 0.0);
      Px.send(sl, ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 40, 0.3, 0.3, 0.3, 1.0);
      this.level().playSound(null, cx, cy, cz, (SoundEvent)ModSounds.METEOR_IMPACT.get(), SoundSource.NEUTRAL, 3.0F, draw ? 1.1F : 0.8F);
      this.level().playSound(null, cx, cy, cz, (SoundEvent)ModSounds.CLASH_HEAVY.get(), SoundSource.NEUTRAL, 2.6F, 0.6F);
      if (draw) {
         for (BeybladeEntity e : List.of(this, o)) {
            BeybladeEntity other = e == this ? o : this;
            Vec3 away = flat(e.position().subtract(other.position()));
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
            e.cancelSpecial();
            e.damageSpin(70.0F * other.powerMul(), other);
            e.setDeltaMovement(away.scale(1.1 * Mth.clamp(e.knockMul(), 0.5, 1.3)).add(0.0, 0.3, 0.0));
            e.flungTicks = 10;
         }

         this.announceNear(Component.translatable("msg.librabeyblade.sclash_draw").withStyle(ChatFormatting.YELLOW));
      } else {
         Vec3 away = flat(lose.position().subtract(win.position()));
         away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
         lose.cancelSpecial();
         lose.damageSpin(Mth.clamp(80.0F + 140.0F * (margin - 1.0F), 80.0F, 260.0F) * win.powerMul(), win);
         lose.setDeltaMovement(
            away.scale(Mth.clamp(1.2 + 0.8 * (double)(margin - 1.0F), 1.2, 2.2) * Mth.clamp(lose.knockMul(), 0.55, 1.4)).add(0.0, 0.35, 0.0)
         );
         lose.flungTicks = 14;
         lose.shockwave(win, 1.0);
         win.addSpirit(15);
         lose.lastAbilityHitTick = this.level().getGameTime();
         this.announceNear(
            Component.translatable(
                  "msg.librabeyblade.sclash_win",
                  new Object[]{
                     Component.translatable(win.titleKey()), Component.translatable(win.abilityKey(win.specialMove())), Component.translatable(lose.titleKey())
                  }
               )
               .withStyle(ChatFormatting.GOLD)
         );
      }

      this.endSpecialClash();
      o.endSpecialClash();
   }

   private void cancelSpecial() {
      this.pendingAbility = 0;
      if (this.getAbility() != 0) {
         this.endAbility();
      }
   }

   private void announceNear(Component c) {
      for (Player p : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(32.0))) {
         p.displayClientMessage(c, true);
      }
   }

   private float lockPower() {
      float p = this.getSpin() * this.powerMul() * (float)Math.sqrt((double)(Math.max(20.0F, this.parts().weight()) / 40.0F));
      if (this.isSpiritActive()) {
         p *= 1.25F;
      }

      if (this.isShielded()) {
         p *= 1.1F;
      }

      return p * (0.85F + 0.3F * this.random.nextFloat());
   }

   private void resolveLock(@Nullable BeybladeEntity o) {
      this.lockPartner = null;
      if (o != null && o.isAlive() && o.getState() == 0 && this.getState() == 0) {
         o.lockTicks = 0;
         o.lockPartner = null;
         float a = this.lockPower();
         float b = o.lockPower();
         BeybladeEntity win = a >= b ? this : o;
         BeybladeEntity lose = win == this ? o : this;
         float margin = Math.max(a, b) / Math.max(1.0F, Math.min(a, b));
         Vec3 dir = flat(lose.position().subtract(win.position()));
         dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : dir.normalize();
         if (!lose.tryDodge(win)) {
            double power = Mth.clamp(1.1 + 0.7 * (double)(margin - 1.0F), 1.1, 2.0) * Mth.clamp(lose.knockMul(), 0.55, 1.3);
            lose.setDeltaMovement(dir.scale(power).add(0.0, 0.32, 0.0));
            lose.hurtMarked = true;
            lose.flungTicks = 14;
            lose.damageSpin(Mth.clamp(45.0F * margin, 45.0F, 120.0F) * win.powerMul(), win);
            lose.shockwave(win, 1.0);
            win.setDeltaMovement(win.getDeltaMovement().multiply(0.2, 1.0, 0.2));
            win.retreatTicks = 0;
            win.addSpirit(8);
            double cx = (this.getX() + o.getX()) / 2.0;
            double cy = this.getY() + 0.4;
            double cz = (this.getZ() + o.getZ()) / 2.0;
            ServerLevel sl = (ServerLevel)this.level();
            Px.send(sl, ParticleTypes.ELECTRIC_SPARK, cx, cy, cz, 30, 0.1, 0.1, 0.1, 0.8);
            Px.send(sl, ParticleTypes.CRIT, cx, cy, cz, 16, 0.08, 0.08, 0.08, 0.7);
            this.level().playSound(null, cx, cy, cz, (SoundEvent)ModSounds.CLASH_HEAVY.get(), SoundSource.NEUTRAL, 2.6F, 0.55F);
            this.level().playSound(null, cx, cy, cz, (SoundEvent)ModSounds.WALL_HIT.get(), SoundSource.NEUTRAL, 1.6F, 0.7F);
            this.level().broadcastEntityEvent(lose, (byte)61);
            this.level().broadcastEntityEvent(win, (byte)61);
            Player wo = win.getOwner();
            Player lo = lose.getOwner();
            if (wo != null) {
               wo.displayClientMessage(
                  Component.translatable("msg.librabeyblade.lock_win", new Object[]{Component.translatable(win.titleKey())}).withStyle(ChatFormatting.GOLD), true
               );
            }

            if (lo != null && lo != wo) {
               lo.displayClientMessage(
                  Component.translatable("msg.librabeyblade.lock_lose", new Object[]{Component.translatable(lose.titleKey())}).withStyle(ChatFormatting.RED), true
               );
            }
         }
      }
   }

   public void shockwave(Entity from, double strength) {
      Vec3 dir = flat(this.position().subtract(from.position()));
      dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : dir.normalize();
      this.level().broadcastEntityEvent(this, (byte)68);
      Vec3 c = this.position().add(from.position()).scale(0.5);
      double range = 3.5 + 2.5 * strength;

      for (Entity e : this.level().getEntities(this, this.getBoundingBox().inflate(range, 2.0, range))) {
         if (e != from && !(e instanceof StadiumEntity)) {
            if (e instanceof Player) {
               Player p = (Player)e;
               if (p.isSpectator()) {
                  continue;
               }
            }

            Vec3 rel = flat(e.position().subtract(c));
            double d = rel.length();
            if (!(d < 0.3) && !(d > range) && !(rel.scale(1.0 / d).dot(dir) < 0.55)) {
               double k = (1.0 - d / range) * strength;
               if (e instanceof BeybladeEntity) {
                  BeybladeEntity b = (BeybladeEntity)e;
                  if (b.getState() == 0) {
                     b.setDeltaMovement(b.getDeltaMovement().add(dir.scale(0.35 * k)));
                     if (this.isEnemy(b) || b.isEnemy(this)) {
                        b.damageSpin((float)(25.0 * k), from);
                     }
                  }
               } else if (e instanceof LivingEntity l) {
                  l.push(dir.x * 0.6 * k, 0.15 * k, dir.z * 0.6 * k);
                  l.hurtMarked = true;
               }
            }
         }
      }
   }

   private static void shift(Entity e, double dx, double dz) {
      AABB box = e.getBoundingBox().move(dx, 0.0, dz);
      if (e.level().noCollision(e, box)) {
         e.setPos(e.getX() + dx, e.getY(), e.getZ() + dz);
      } else {
         AABB half = e.getBoundingBox().move(dx * 0.5, 0.0, dz * 0.5);
         if (e.level().noCollision(e, half)) {
            e.setPos(e.getX() + dx * 0.5, e.getY(), e.getZ() + dz * 0.5);
         }
      }
   }

   private void collideLiving(@Nullable Player owner) {
      for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.2), l -> l.isAlive() && !l.isSpectator())) {
         double dx = e.getX() - this.getX();
         double dz = e.getZ() - this.getZ();
         double dist = Math.sqrt(dx * dx + dz * dz);
         double min = 0.45F + (double)e.getBbWidth() / 2.0;
         if (!(dist >= min) && !(dist < 1.0E-4) && !(e.getY() > this.getY() + 0.66F) && !(e.getY() + (double)e.getBbHeight() < this.getY())) {
            double nx = dx / dist;
            double nz = dz / dist;
            shift(this, -nx * (min - dist) * 0.8, -nz * (min - dist) * 0.8);
            Vec3 v = this.getDeltaMovement();
            double sp = flat(v).length();
            if (this.isHostileLiving(e) && sp > 0.1 && e.hurtTime == 0) {
               e.hurt(this.damageSources().thrown(this, owner), 3.0F + (float)(sp * 16.0) * (0.6F + 0.4F * this.spinRatio()));
               e.knockback(0.5 + sp, -nx, -nz);
               this.setDeltaMovement(v.add(-nx * (sp * 1.3 + 0.1), 0.05, -nz * (sp * 1.3 + 0.1)));
               Px.send(
                  (ServerLevel)this.level(),
                  ParticleTypes.ELECTRIC_SPARK,
                  this.getX() + nx * 0.45F,
                  this.getY() + 0.35,
                  this.getZ() + nz * 0.45F,
                  10,
                  0.1,
                  0.1,
                  0.1,
                  0.4
               );
               this.play((SoundEvent)ModSounds.CLASH.get(), 0.7F, 1.2F);
               this.setSpin(this.getSpin() - 6.0F);
               this.level().broadcastEntityEvent(this, (byte)60);
            } else {
               double along = v.x * nx + v.z * nz;
               if (along > 0.0) {
                  this.setDeltaMovement(v.add(-nx * along * 1.6, 0.0, -nz * along * 1.6));
               }

               e.push(nx * 0.04, 0.0, nz * 0.04);
            }
         }
      }
   }

   public void damageSpin(float amount, @Nullable Entity source) {
      if (this.getState() == 0 && !(amount <= 0.0F)) {
         float taken = amount * this.incomingMul() * this.guardMul();
         if (this.parts().tip().freeSpin()) {
            taken *= 0.86F;
         }

         this.setSpin(this.getSpin() - taken);
         if (taken > 30.0F) {
            this.addSpirit(4);
         }

         if (this.getMode() == 1 && source instanceof BeybladeEntity att && att != this && att.getAbility() == 0 && taken > 12.0F) {
            att.setSpin(att.getSpin() - taken * 0.15F);
         }

         if (source instanceof BeybladeEntity b && b != this) {
            this.lastAttacker = b;
            this.lastAttackTick = this.level().getGameTime();
            if (b.getAbility() != 0 || b.isBuffActive()) {
               this.lastAbilityHitTick = this.lastAttackTick;
            }
         }

         if (source != null && this.target == null && (!(source instanceof BeybladeEntity bx) || this.isEnemy(bx))) {
            this.target = source;
         }
      }
   }

   public void healSpin(float amount) {
      if (this.getState() == 0) {
         this.setSpin(this.getSpin() + amount);
      }
   }

   protected void shieldHit() {
      this.play((SoundEvent)ModSounds.SHIELD_HIT.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.3F);
      this.level().broadcastEntityEvent(this, (byte)108);
   }

   public boolean tryAbility(int id, boolean fromPlayer) {
      if (id >= 1 && id <= this.abilityCount() && this.getState() == 0) {
         Player owner = this.getOwner();
         int cd = this.cooldownRemaining(id);
         if (cd > 0) {
            if (fromPlayer && owner != null) {
               owner.displayClientMessage(Component.translatable("msg.librabeyblade.cooldown", new Object[]{Component.translatable(this.abilityKey(id)), (cd + 19) / 20}), true);
            }

            return false;
         } else if (id == this.buffAbility()) {
            this.entityData.set(DATA_SHIELD_END, (int)(this.level().getGameTime() + (long)this.buffTicks()));
            this.startCooldown(id);
            this.onBuffStart(owner);
            this.announce(owner, id);
            return true;
         } else if (!this.abilityAvailable(id)) {
            if (fromPlayer && owner != null) {
               owner.displayClientMessage(Component.translatable("msg.librabeyblade.locked", new Object[]{Component.translatable(this.abilityKey(id))}), true);
            }

            return false;
         } else if (this.getAbility() == 0 && this.pendingAbility == 0) {
            if (this.requiresTarget(id)) {
               if (this.target == null) {
                  this.target = this.findTarget(owner);
               }

               if (this.target == null) {
                  if (fromPlayer && owner != null) {
                     owner.displayClientMessage(Component.translatable("msg.librabeyblade.no_target"), true);
                  }

                  return false;
               }

               this.entityData.set(DATA_TARGET, this.target.getId());
            }

            this.startCooldown(id);
            this.addXp(4);
            this.specialStart = this.level().getGameTime();
            Entity tg = this.target != null ? this.target : this.findTarget(owner);
            if (this.ramsInto(id)
               && tg instanceof BeybladeEntity b
               && b.getState() == 0
               && !this.frozen
               && this.stunTicks == 0
               && flat(tg.position().subtract(this.position())).length() > 1.1999999761581421) {
               this.target = tg;
               this.entityData.set(DATA_TARGET, tg.getId());
               this.pendingAbility = id;
               this.ramTicks = 0;
               if (owner != null) {
                  owner.displayClientMessage(Component.translatable("msg.librabeyblade.ram", new Object[]{Component.translatable(this.abilityKey(id))}), true);
               }

               this.play((SoundEvent)ModSounds.CHARGE.get(), 1.0F, 1.5F);
               return true;
            }

            this.beginAbility(id, owner);
            return true;
         } else {
            if (fromPlayer && owner != null) {
               owner.displayClientMessage(Component.translatable("msg.librabeyblade.busy"), true);
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private void beginAbility(int id, @Nullable Player owner) {
      this.abilityTicks = 0;
      this.abilityBaseY = this.getY();
      this.abilityBaseX = this.getX();
      this.abilityBaseZ = this.getZ();
      this.diving = false;

      for (Set<Integer> s : this.waveHits) {
         s.clear();
      }

      this.entityData.set(DATA_ABILITY, id);
      this.announce(owner, id);
   }

   public void launchBonus(boolean perfect, boolean power) {
      if (perfect) {
         this.entityData.set(DATA_SPIRIT, 40);
      }

      if (power) {
         this.dashTicks = 10;
      }
   }

   public int abilityCount() {
      return 5;
   }

   protected void onClash(BeybladeEntity other, float dealt, float taken) {
   }

   protected boolean lastStand() {
      return false;
   }

   @Nullable
   public Component passiveLine() {
      return null;
   }

   protected boolean ramsInto(int id) {
      return id != this.buffAbility();
   }

   protected void initFromItem(ItemStack stack) {
   }

   public boolean abilityAvailable(int id) {
      return true;
   }

   private void announce(@Nullable Player owner, int id) {
      if (owner != null) {
         owner.displayClientMessage(Component.translatable("msg.librabeyblade.ability." + this.kindId(), new Object[]{Component.translatable(this.abilityKey(id))}), true);
      }
   }

   private void startCooldown(int id) {
      this.entityData.set(DATA_COOLDOWN[id - 1], (int)(this.level().getGameTime() + (long)this.cooldown(id)));
   }

   protected void endAbility() {
      this.pendingAbility = 0;
      if (this.getAbility() != 0) {
         this.entityData.set(DATA_ABILITY, 0);
      }

      this.setNoGravity(false);
      this.diving = false;
      this.abilityTicks = 0;
   }

   private void rivalUseAbilities() {
      if (this.target != null && !(this.target.distanceToSqr(this) > 144.0) && this.random.nextInt(45) == 0) {
         int id = 1 + this.random.nextInt(this.abilityCount());
         if (this.cooldownRemaining(id) == 0) {
            this.tryAbility(id, false);
         }
      }
   }

   public List<Entity> enemiesWithin(double r) {
      List<Entity> out = new ArrayList<>();
      AABB box = this.getBoundingBox().inflate(r, r, r);

      for (Entity e : this.level().getEntities(this, box)) {
         if (!(e.distanceToSqr(this) > r * r)) {
            if (e instanceof BeybladeEntity) {
               BeybladeEntity b = (BeybladeEntity)e;
               if (!this.isEnemy(b)) {
                  continue;
               }
            } else {
               if (!(e instanceof LivingEntity)) {
                  continue;
               }

               LivingEntity l = (LivingEntity)e;
               if (!this.isHostileLiving(l)) {
                  continue;
               }
            }

            out.add(e);
         }
      }

      return out;
   }

   protected void hover(double height, double strength) {
      this.setNoGravity(true);
      Vec3 v = this.getDeltaMovement();
      double dy = this.abilityBaseY + height - this.getY();
      this.setDeltaMovement(v.x * 0.82, Mth.clamp(dy * strength, -0.6, 0.6), v.z * 0.82);
   }

   protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
      ServerLevel sl = (ServerLevel)this.level();
      switch (ab) {
         case 1:
            if (t < 40) {
               this.hover(0.7, 0.3);
            } else {
               this.setNoGravity(false);
            }

            int i = 0;

            for (; i < WAVE_PULSES.length; i++) {
               int age = t - WAVE_PULSES[i];
               if (age == 0) {
                  this.waveHits[i].clear();
                  this.play((SoundEvent)ModSounds.SONIC_WAVE.get(), 1.6F, 0.9F + (float)i * 0.1F);
                  Px.send(sl, ParticleTypes.SONIC_BOOM, this.getX(), this.getY() + 0.4, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
               }

               if (age >= 0 && age < 16) {
                  double front = 0.6 + (double)((float)age * 0.62F);

                  for (Entity e : this.enemiesWithin(front + 0.5)) {
                     double d = Math.sqrt(flat(e.position().subtract(this.position())).lengthSqr());
                     if (!(d < front - 1.0) && this.waveHits[i].add(e.getId())) {
                        Vec3 out = flat(e.position().subtract(this.position()));
                        out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
                        if (e instanceof BeybladeEntity b) {
                           b.damageSpin(60.0F, this);
                           b.setDeltaMovement(b.getDeltaMovement().add(out.scale(0.6)).add(0.0, 0.18, 0.0));
                           Px.send(sl, ParticleTypes.ELECTRIC_SPARK, b.getX(), b.getY() + 0.4, b.getZ(), 8, 0.2, 0.2, 0.2, 0.3);
                        } else if (e instanceof LivingEntity l) {
                           l.hurt(this.damageSources().indirectMagic(this, owner), 5.0F);
                           l.knockback(0.9, -out.x, -out.z);
                        }
                     }
                  }
               }
            }

            return true;
         case 2:
         default:
            return false;
         case 3:
            BlockPos below = BlockPos.containing(this.abilityX(), this.abilityBaseY - 0.2, this.abilityZ());
            if (t == 1) {
               this.play((SoundEvent)ModSounds.INFERNO_DRILL.get(), 1.4F, 1.0F);
            }

            if (t < 24) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.55, 1.0, 0.55));

               for (Entity exxx : this.enemiesWithin(9.0)) {
                  Vec3 pull = flat(this.position().subtract(exxx.position()));
                  if (!(pull.lengthSqr() < 0.25)) {
                     exxx.setDeltaMovement(exxx.getDeltaMovement().add(pull.normalize().scale(exxx instanceof BeybladeEntity ? 0.07 : 0.05)));
                     exxx.hurtMarked = true;
                  }
               }

               BlockState ground = this.level().getBlockState(below);
               if (!ground.isAir() && t % 2 == 0) {
                  Px.send(
                     sl,
                     new BlockParticleOption(ParticleTypes.BLOCK, ground),
                     this.getX(),
                     this.getY() + 0.1,
                     this.getZ(),
                     10,
                     0.5,
                     0.05,
                     0.5,
                     0.2
                  );
               }

               return true;
            } else {
               if (t < 52) {
                  if (t == 24) {
                     this.play((SoundEvent)ModSounds.INFERNO_TORNADO.get(), 2.0F, 1.0F);
                     Px.send(sl, ParticleTypes.LAVA, this.getX(), this.getY() + 0.3, this.getZ(), 10, 0.8, 0.2, 0.8, 0.0);
                  }

                  this.hover(6.0, 0.12);

                  for (Entity exxxx : this.enemiesWithin(9.0)) {
                     Vec3 rel = flat(exxxx.position().subtract(new Vec3(this.getX(), 0.0, this.getZ())));
                     double d = rel.length();
                     if (!(d > 3.2) && !(exxxx.getY() < this.abilityBaseY - 1.0) && !(exxxx.getY() > this.abilityBaseY + 9.0)) {
                        Vec3 dirx = d < 0.001 ? new Vec3(1.0, 0.0, 0.0) : rel.scale(1.0 / d);
                        Vec3 swirl = new Vec3(dirx.z, 0.0, -dirx.x).scale(0.34).add(dirx.scale(-0.08));
                        Vec3 nv = new Vec3(swirl.x, Math.min(0.32, exxxx.getDeltaMovement().y + 0.12), swirl.z);
                        exxxx.setDeltaMovement(nv);
                        exxxx.hurtMarked = true;
                        exxxx.fallDistance = 0.0F;
                        if (exxxx instanceof BeybladeEntity b) {
                           b.damageSpin(6.0F, this);
                        } else if (exxxx instanceof LivingEntity) {
                           LivingEntity l = (LivingEntity)exxxx;
                           if (t % 8 == 0) {
                              l.hurt(this.damageSources().indirectMagic(this, owner), 3.0F);
                              V.ignite(l, 4);
                           }
                        }
                     }
                  }

                  return true;
               }

               if (t == 52) {
                  this.setNoGravity(false);
                  Vec3 aim = this.target != null ? this.target.position() : new Vec3(this.getX(), this.abilityBaseY, this.getZ());
                  Vec3 d = flat(aim.subtract(this.position()));
                  this.setDeltaMovement(d.x * 0.14, -1.5, d.z * 0.14);
                  this.diving = true;
               }

               if (this.diving && (this.onGround() || t > 76)) {
                  this.diving = false;
                  this.infernoImpact(owner);
                  this.abilityTicks = Math.max(this.abilityTicks, DURATION[3] - 16);
               }

               return true;
            }
         case 4:
            if (t == 1) {
               this.play((SoundEvent)ModSounds.BUSTER_CHARGE.get(), 1.5F, 1.0F);
            }

            if (t < 52) {
               this.hover(2.4, 0.2);
            } else {
               this.setNoGravity(false);
            }

            if (this.target == null) {
               if (t < 32) {
                  this.endAbilityEarly();
               }

               return true;
            }

            if (t == 32) {
               this.play((SoundEvent)ModSounds.BUSTER_FIRE.get(), 2.0F, 1.0F);
            }

            if (t >= 32 && t < 52) {
               Vec3 from = this.position().add(0.0, 0.35, 0.0);
               Vec3 to = this.target.position().add(0.0, (double)this.target.getBbHeight() * 0.5, 0.0);
               Vec3 dir = to.subtract(from);
               double len = dir.length();
               if (len > 0.001) {
                  dir = dir.scale(1.0 / len);
               }

               for (Entity exx : this.enemiesWithin(len + 2.0)) {
                  Vec3 c = exx.position().add(0.0, (double)exx.getBbHeight() * 0.5, 0.0);
                  double along = c.subtract(from).dot(dir);
                  if (!(along < 0.0) && !(along > len + 1.0)) {
                     double off = c.subtract(from.add(dir.scale(along))).length();
                     if (!(off > 1.1) || exx == this.target) {
                        if (exx instanceof BeybladeEntity b) {
                           b.damageSpin(exx == this.target ? 14.0F : 7.0F, this);
                           b.setDeltaMovement(b.getDeltaMovement().add(flat(dir).scale(0.09)));
                        } else if (exx instanceof LivingEntity) {
                           LivingEntity l = (LivingEntity)exx;
                           if (t % 4 == 0) {
                              l.hurt(this.damageSources().indirectMagic(this, owner), 3.0F);
                              l.knockback(0.3, -dir.x, -dir.z);
                           }
                        }
                     }
                  }
               }

               if (t % 5 == 0) {
                  Px.send(sl, ParticleTypes.SONIC_BOOM, to.x, to.y, to.z, 1, 0.0, 0.0, 0.0, 0.0);
               }

               Px.send(sl, ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 6, 0.3, 0.3, 0.3, 0.4);
            }

            return true;
         case 5:
            if (t == 1) {
               this.play((SoundEvent)ModSounds.JUDGMENT_START.get(), 2.0F, 1.0F);
            }

            if (t < DURATION[5] - 10) {
               this.hover(1.2, 0.15);
            } else {
               this.setNoGravity(false);
            }

            if (t >= 22 && t < 86) {
               if ((t - 22) % 20 == 0) {
                  this.play((SoundEvent)ModSounds.DRAIN.get(), 1.2F, 1.0F);
               }

               for (Entity ex : this.enemiesWithin(14.0)) {
                  if (ex instanceof BeybladeEntity b) {
                     float d = Math.min(5.0F, b.getSpin());
                     b.damageSpin(d, this);
                     this.healSpin(d * 0.6F);
                     b.setDeltaMovement(b.getDeltaMovement().multiply(0.8, 1.0, 0.8));
                  } else if (ex instanceof LivingEntity) {
                     LivingEntity l = (LivingEntity)ex;
                     if (t % 10 == 0) {
                        l.hurt(this.damageSources().indirectMagic(this, owner), 2.0F);
                        l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                        l.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
                     }
                  }
               }
            }

            if (t == 86) {
               this.play((SoundEvent)ModSounds.JUDGMENT_STRIKE.get(), 2.5F, 1.0F);
               this.level().broadcastEntityEvent(this, (byte)66);

               for (Entity exx : this.enemiesWithin(14.0)) {
                  if (exx instanceof BeybladeEntity b) {
                     b.damageSpin(130.0F, this);
                     b.setDeltaMovement(b.getDeltaMovement().add(0.0, 0.5, 0.0));
                  } else if (exx instanceof LivingEntity l) {
                     l.hurt(this.damageSources().indirectMagic(this, owner), 12.0F);
                  }

                  Px.send(sl, ParticleTypes.END_ROD, exx.getX(), exx.getY() + 1.0, exx.getZ(), 30, 0.3, 1.5, 0.3, 0.08);
                  Px.send(sl, ParticleTypes.END_ROD, exx.getX(), exx.getY() + 0.5, exx.getZ(), 14, 0.1, 0.1, 0.1, 0.25);
               }
            }

            return true;
      }
   }

   private double abilityX() {
      return this.getX();
   }

   private double abilityZ() {
      return this.getZ();
   }

   protected void endAbilityEarly() {
      this.abilityTicks = this.duration(this.getAbility());
   }

   private void infernoImpact(@Nullable Player owner) {
      ServerLevel sl = (ServerLevel)this.level();
      this.play((SoundEvent)ModSounds.INFERNO_IMPACT.get(), 2.5F, 1.0F);
      this.level().broadcastEntityEvent(this, (byte)65);
      Px.send(sl, ParticleTypes.END_ROD, this.getX(), this.getY() + 0.5, this.getZ(), 14, 0.1, 0.1, 0.1, 0.25);
      Px.send(sl, ParticleTypes.LAVA, this.getX(), this.getY() + 0.3, this.getZ(), 20, 1.5, 0.2, 1.5, 0.0);

      for (int i = 0; i < 48; i++) {
         double a = (double)i / 48.0 * Math.PI * 2.0;
         Px.send(
            sl,
            ParticleTypes.FLAME,
            this.getX() + Math.cos(a) * 0.8,
            this.getY() + 0.15,
            this.getZ() + Math.sin(a) * 0.8,
            0,
            Math.cos(a),
            0.05,
            Math.sin(a),
            0.45
         );
      }

      for (Entity e : this.enemiesWithin(5.5)) {
         Vec3 out = flat(e.position().subtract(this.position()));
         out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
         double falloff = 1.0 - Math.min(1.0, (double)e.distanceTo(this) / 5.5) * 0.5;
         if (e instanceof BeybladeEntity b) {
            b.damageSpin((float)(160.0 * falloff), this);
            b.setDeltaMovement(b.getDeltaMovement().add(out.scale(0.9 * falloff)).add(0.0, 0.35, 0.0));
         } else if (e instanceof LivingEntity l) {
            l.hurt(this.damageSources().indirectMagic(this, owner), (float)(10.0 * falloff));
            V.ignite(l, 5);
            l.knockback(1.2 * falloff, -out.x, -out.z);
         }
      }
   }

   protected void tickShield() {
      for (Projectile p : this.level().getEntitiesOfClass(Projectile.class, this.getBoundingBox().inflate(1.6))) {
         Entity po = p.getOwner();
         if (po == null || !po.getUUID().equals(this.getOwnerId())) {
            Vec3 v = p.getDeltaMovement();
            Vec3 away = p.position().subtract(this.position());
            if (v.dot(away) < 0.0) {
               p.setDeltaMovement(v.scale(-0.7));
               p.hurtMarked = true;
               this.shieldHit();
            }
         }
      }
   }

   public void recall() {
      if (this.getState() != 2 && !this.isRival()) {
         this.endAbility();
         this.setState(2);
         this.setNoGravity(true);
         this.noPhysics = true;
         this.play((SoundEvent)ModSounds.RECALL.get(), 1.0F, 1.0F);
      }
   }

   private void tickRecall(@Nullable Player owner) {
      this.noPhysics = true;
      if (owner == null) {
         this.dropStored();
         this.discard();
      } else {
         Vec3 goal = owner.position().add(0.0, (double)owner.getBbHeight() * 0.55, 0.0);
         Vec3 d = goal.subtract(this.position());
         double dist = d.length();
         if (!(dist < 1.1) && this.stateTicks <= 200) {
            double speed = Math.min(1.5, 0.25 + (double)this.stateTicks * 0.07);
            Vec3 arc = this.stateTicks < 8 ? new Vec3(0.0, 0.25, 0.0) : Vec3.ZERO;
            Vec3 v = d.scale(Math.min(speed, dist) / dist).add(arc);
            this.setDeltaMovement(v);
            this.setPos(this.getX() + v.x, this.getY() + v.y, this.getZ() + v.z);
         } else {
            this.giveTo(owner);
            this.level()
               .playSound(null, owner.getX(), owner.getY(), owner.getZ(), (SoundEvent)ModSounds.CATCH.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            this.discard();
         }
      }
   }

   private void stadiumPhysics() {
      List<StadiumEntity> list = this.level().getEntitiesOfClass(StadiumEntity.class, this.getBoundingBox().inflate(18.0, 6.0, 18.0));
      if (list.isEmpty()) {
         this.stadium = null;
         this.inBowl = false;
         this.contained = false;
      } else {
         StadiumEntity st = list.get(0);
         double dx = this.getX() - st.getX();
         double dz = this.getZ() - st.getZ();
         double r = Math.sqrt(dx * dx + dz * dz);
         double rel = this.getY() - st.getY();
         if (!(r > 15.9F) && !(rel < -1.5) && !(rel > 14.0)) {
            this.stadium = st;
            double nx = r < 1.0E-4 ? 1.0 : dx / r;
            double nz = r < 1.0E-4 ? 0.0 : dz / r;
            Vec3 v = this.getDeltaMovement();
            double wallR = 10.650001F;
            if (r < wallR && rel > -1.0) {
               this.contained = true;
            }

            if (this.outZone != 0) {
               this.contained = false;
            }

            if (this.contained && this.getState() == 0 && r > wallR) {
               int zone = StadiumEntity.zoneAt(StadiumEntity.deg(dx, dz));
               if (zone != 0 && this.pocketed(v.x * nx + v.z * nz)) {
                  this.knockOut(st, zone, nx, nz);
                  return;
               }
            }

            if (this.contained && this.getState() != 2 && r > wallR) {
               this.setPos(st.getX() + nx * wallR, this.getY(), st.getZ() + nz * wallR);
               r = wallR;
               double vr = v.x * nx + v.z * nz;
               if (vr > 0.0) {
                  v = v.subtract(nx * vr * 1.7, 0.0, nz * vr * 1.7);
                  if (vr > 0.7 && this.getState() == 0) {
                     this.damageSpin((float)((vr - 0.4) * 150.0), this.lastAttacker());
                     this.level()
                        .playSound(
                           null, this.getX(), this.getY(), this.getZ(), (SoundEvent)ModSounds.CLASH_HEAVY.get(), SoundSource.NEUTRAL, 2.2F, 0.55F
                        );
                     this.level().broadcastEntityEvent(this, (byte)61);
                  }

                  if (vr > 0.12 && this.wallSoundCooldown == 0 && rel < 3.25) {
                     this.wallSoundCooldown = 6;
                     this.play((SoundEvent)ModSounds.WALL_HIT.get(), 0.9F, 0.9F + this.random.nextFloat() * 0.3F);
                     this.setSpin(this.getSpin() - 3.0F);
                     this.level().broadcastEntityEvent(this, (byte)64);
                  }
               }
            }

            double surf = StadiumEntity.surface(r);
            if (surf >= 0.0 && this.getState() != 2) {
               double fy = st.getY() + surf;
               if (this.getY() <= fy + 0.06 && rel > -1.2) {
                  this.setPos(this.getX(), fy, this.getZ());
                  if (v.y < 0.0) {
                     v = new Vec3(v.x, v.y < -0.35 ? -v.y * 0.25 : 0.0, v.z);
                  }

                  this.setOnGround(true);
                  double sl = StadiumEntity.slope(r);
                  if (r > 6.51) {
                     double tx = -nz;
                     double vt = v.x * tx + v.z * nx;
                     if (Math.abs(vt) > 0.25 && Math.abs(vt) < 1.05) {
                        v = v.add(tx * vt * 0.045, 0.0, nx * vt * 0.045);
                     }
                  }

                  v = new Vec3((v.x - nx * sl * 0.045) * 0.965, v.y, (v.z - nz * sl * 0.045) * 0.965);
               }
            }

            this.inBowl = r < 11.1F && (this.contained || rel < 3.25);
            this.setDeltaMovement(v);
         } else {
            this.stadium = null;
            this.inBowl = false;
            this.contained = false;
         }
      }
   }

   private void defeat() {
      this.endAbility();
      this.setSpin(0.0F);
      this.setState(1);
      this.play((SoundEvent)ModSounds.DEFEAT.get(), 1.2F, 1.0F);
      Player owner = this.getOwner();
      if (owner != null) {
         owner.displayClientMessage(Component.translatable("msg.librabeyblade.defeated"), true);
      }

      BeybladeEntity winner = this.lastAttacker();
      if (winner != null && winner.isAlive() && winner.getState() == 0) {
         winner.addXp(this.isRival() ? 30 : 50);
         Player wo = winner.getOwner();
         if (wo != null) {
            wo.displayClientMessage(Component.translatable(this.finishedByAbility() ? "msg.librabeyblade.ko_ability" : "msg.librabeyblade.ko"), true);
         }
      }
   }

   private void tickDefeated(@Nullable Player owner) {
      Vec3 v = this.getDeltaMovement();
      this.setDeltaMovement(this.outZone != 0 ? 0.0 : v.x * 0.9, v.y, this.outZone != 0 ? 0.0 : v.z * 0.9);
      this.physics();
      this.stadiumPhysics();
      this.collideBeyblades();
      if (this.stateTicks == 70 && owner != null) {
         this.recall();
      } else {
         if (this.stateTicks > 90 && owner == null) {
            Px.send((ServerLevel)this.level(), ParticleTypes.POOF, this.getX(), this.getY() + 0.3, this.getZ(), 12, 0.3, 0.2, 0.3, 0.02);
            this.discard();
         }
      }
   }

   public static int heldLevel(Player p) {
      int best = 1;

      for (ItemStack st : p.getInventory().items) {
         if (st.getItem() instanceof LibraItem) {
            best = Math.max(best, LibraItem.level(st));
         }
      }

      return best;
   }

   public static void grantXp(Player p, int amount) {
      BeybladeEntity active = findActive(p);
      if (active != null) {
         active.addXp(amount);
      } else {
         ItemStack target = p.getMainHandItem().getItem() instanceof LibraItem ? p.getMainHandItem() : ItemStack.EMPTY;
         if (target.isEmpty()) {
            for (ItemStack st : p.getInventory().items) {
               if (st.getItem() instanceof LibraItem) {
                  target = st;
                  break;
               }
            }
         }

         if (!target.isEmpty()) {
            CompoundTag t0 = V.tag(target);
            int lvl = t0 == null ? 1 : Math.max(1, t0.getInt("BeyLevel"));
            int xp = (t0 == null ? 0 : t0.getInt("BeyXp")) + amount;

            int before;
            for (before = lvl; lvl < 30 && xp >= xpNeeded(lvl); lvl++) {
               xp -= xpNeeded(lvl);
            }

            int nl = lvl;
            int nx = lvl >= 30 ? 0 : xp;
            V.editTag(target, t -> {
               t.putInt("BeyLevel", nl);
               t.putInt("BeyXp", nx);
            });
            if (lvl > before) {
               p.displayClientMessage(Component.translatable("msg.librabeyblade.level_up", new Object[]{target.getHoverName(), lvl}).withStyle(ChatFormatting.GOLD), false);
            }
         }
      }
   }

   public ItemStack itemWithProgress() {
      ItemStack s = this.stored.isEmpty() ? this.defaultItem() : this.stored.copy();
      V.editTag(s, t -> {
         t.putInt("BeyLevel", this.getLevel());
         t.putInt("BeyXp", this.getXp());
         t.putInt("BeyMode", this.getMode());
      });
      return s;
   }

   private void giveTo(Player p) {
      if (!this.isRival()) {
         ItemStack s = this.itemWithProgress();
         this.stored = ItemStack.EMPTY;
         if (!p.getInventory().add(s)) {
            p.drop(s, false);
         }
      }
   }

   private void dropStored() {
      if (!this.isRival()) {
         ItemStack s = this.itemWithProgress();
         this.stored = ItemStack.EMPTY;
         this.spawnAtLocation(s, 0.3F);
      }
   }

   protected void play(SoundEvent s, float vol, float pitch) {
      this.level().playSound(null, this.getX(), this.getY(), this.getZ(), s, SoundSource.NEUTRAL, vol, pitch);
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      if (player.getUUID().equals(this.getOwnerId())) {
         if (!this.level().isClientSide()) {
            this.recall();
         }

         return InteractionResult.sidedSuccess(this.level().isClientSide());
      } else {
         return InteractionResult.PASS;
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      if (!this.level().isClientSide() && !this.isInvulnerableTo(source) && this.getState() == 0) {
         if (source.getEntity() instanceof Player p) {
            if (p.getUUID().equals(this.getOwnerId())) {
               return false;
            } else {
               this.damageSpin(amount * 8.0F, null);
               Vec3 push = flat(this.position().subtract(p.position()));
               if (push.lengthSqr() > 1.0E-4) {
                  this.setDeltaMovement(this.getDeltaMovement().add(push.normalize().scale(0.4)).add(0.0, 0.15, 0.0));
               }

               this.level().broadcastEntityEvent(this, (byte)60);
               this.play((SoundEvent)ModSounds.CLASH.get(), 0.7F, 1.3F);
               return true;
            }
         } else if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            this.damageSpin(amount * 4.0F, null);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean isPickable() {
      return !this.isRemoved();
   }

   public boolean isPushable() {
      return false;
   }

   public void push(Entity e) {
   }

   public boolean canBeCollidedWith() {
      return false;
   }

   public boolean displayFireAnimation() {
      return false;
   }

   public AABB getBoundingBoxForCulling() {
      if (this.clientAbility != 0 || this.getState() == 2) {
         return this.getBoundingBox().inflate(30.0, 80.0, 30.0);
      } else {
         return this.isShielded() ? this.getBoundingBox().inflate(2.0) : this.getBoundingBox().inflate(1.5);
      }
   }

   public boolean shouldRenderAtSqrDistance(double d) {
      return d < 16384.0;
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      super.onSyncedDataUpdated(key);
      if (this.level() != null && this.level().isClientSide()) {
         if (DATA_SCLASH.equals(key) && this.getSpecialClashPartner() >= 0) {
            this.sclashClientStart = this.tickCount;
         }

         if (DATA_ABILITY.equals(key)) {
            this.clientAbility = this.getAbility();
            this.clientAbilityStart = this.tickCount;
            this.abilityX = this.getX();
            this.abilityY = this.getY();
            this.abilityZ = this.getZ();
            if (this.clientAbility != 0 && this.tickCount > 2) {
               this.playAction(com.krolasyon.beyblade.client.BeyAnims.ability(this, this.clientAbility));
            }
         } else if (DATA_STATE.equals(key)) {
            this.clientStateStart = this.tickCount;
            int st = this.getState();
            // a defeat right after a heavy blow is shown as a burst finish: the parts fly apart
            this.burstClient = st == STATE_DEFEATED && this.tickCount - this.heavyHitTime < 10 && this.tickCount > 5;
            this.frozenSpin = this.spinAngle;
         }
      }
   }

   public void handleEntityEvent(byte id) {
      switch (id) {
         case 60:
            this.hitTime = this.tickCount;
            this.playAction(com.krolasyon.beyblade.client.BeyAnims.HIT_LIGHT);
            break;
         case 64:
            this.hitTime = this.tickCount;
            this.playAction(com.krolasyon.beyblade.client.BeyAnims.HIT_WALL);
            break;
         case 61:
            this.hitTime = this.tickCount;
            this.heavyHitTime = this.tickCount;
            this.playAction(com.krolasyon.beyblade.client.BeyAnims.HIT_HEAVY);
            break;
         case 62:
            this.landTime = this.tickCount;
            this.playAction(com.krolasyon.beyblade.client.BeyAnims.LAND);

            for (int i = 0; i < 20; i++) {
               double a = (double)i / 20.0 * Math.PI * 2.0;
               Px.add(
                  this.level(),
                  ParticleTypes.CLOUD,
                  this.getX() + Math.cos(a) * 0.4,
                  this.getY() + 0.05,
                  this.getZ() + Math.sin(a) * 0.4,
                  Math.cos(a) * 0.12,
                  0.01,
                  Math.sin(a) * 0.12
               );
            }
            break;
         case 63:
         case 70:
         case 71:
         case 72:
         case 73:
         case 74:
         case 75:
         case 76:
         case 77:
         case 78:
         case 79:
         case 80:
         case 81:
         case 82:
         case 83:
         case 84:
         case 85:
         case 89:
         case 90:
         case 91:
         case 92:
         case 93:
         case 94:
         case 95:
         case 96:
         case 97:
         case 98:
         case 99:
         case 100:
         case 101:
         case 102:
         case 103:
         case 106:
         case 107:
         case 109:
         default:
            super.handleEntityEvent(id);
            break;
         case 65:
            this.impactTime = this.tickCount;
            this.clientScar(3.4F);
            break;
         case 66:
            this.strikeTime = this.tickCount;
            this.clientScar(1.8F);
            break;
         case 67:
            this.grindTime = this.tickCount;
            this.playAction(com.krolasyon.beyblade.client.BeyAnims.GRIND);
            break;
         case 68:
            BeybladeEntity other = null;
            double best = 9.0;

            for (BeybladeEntity b : this.level().getEntitiesOfClass(BeybladeEntity.class, this.getBoundingBox().inflate(2.5), bx -> bx != this)) {
               double d = b.distanceToSqr(this);
               if (d < best) {
                  best = d;
                  other = b;
               }
            }

            Vec3 dir = other == null ? new Vec3(1.0, 0.0, 0.0) : flat(this.position().subtract(other.position()));
            dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : dir.normalize();
            this.shockTime = this.tickCount;
            this.clientScar(1.2F);
            this.shockDirX = dir.x;
            this.shockDirZ = dir.z;
            Vec3 c = other == null ? this.position() : this.position().add(other.position()).scale(0.5);
            this.shockX = c.x;
            this.shockY = c.y;
            this.shockZ = c.z;
            break;
         case 69:
            this.spiritTime = this.tickCount;
            this.playAction(com.krolasyon.beyblade.client.BeyAnims.SPIRIT);
            break;
         case 86:
            if (this.tickCount - this.railClientTime > 6) {
               this.playAction(com.krolasyon.beyblade.client.BeyAnims.RAIL);
            }
            this.railClientTime = this.tickCount;
            break;
         case 87:
            this.xdashClientTime = this.tickCount;
            this.playAction(com.krolasyon.beyblade.client.BeyAnims.XDASH);
            break;
         case 88:
            this.outClientTime = this.tickCount;
            break;
         case 104:
         case 105:
            this.sclashBoomTime = this.tickCount;
            this.boomDraw = id != 104;
            this.playAction(com.krolasyon.beyblade.client.BeyAnims.CLASH);
            Entity o = this.level().getEntity(this.getSpecialClashPartner());
            double f = this.boomDraw ? 0.5 : sclashMeet(this.getSpecialClashRatio(), 1.0F);
            if (o != null) {
               this.boomX = this.getX() + (o.getX() - this.getX()) * f;
               this.boomY = this.getY() + (o.getY() - this.getY()) * f + 0.55;
               this.boomZ = this.getZ() + (o.getZ() - this.getZ()) * f;
               Iterator dirIt = this.level().getEntitiesOfClass(StadiumEntity.class, this.getBoundingBox().inflate(18.0, 6.0, 18.0)).iterator();
               if (dirIt.hasNext()) {
                  StadiumEntity st = (StadiumEntity)dirIt.next();
                  st.addScar(this.boomX, this.boomZ, 3.2F);
               }
            }
            break;
         case 108:
            this.shieldHitTime = this.tickCount;
            this.playAction(com.krolasyon.beyblade.client.BeyAnims.SHIELD_HIT);
            break;
         case 110:
            this.evadeTime = this.tickCount;
            this.playAction(com.krolasyon.beyblade.client.BeyAnims.EVADE);
      }
   }

   public static float spinSpeedFor(float ratio) {
      return 70.0F + 175.0F * ratio;
   }

   public void clientScar(float r) {
      Iterator var2 = this.level().getEntitiesOfClass(StadiumEntity.class, this.getBoundingBox().inflate(18.0, 6.0, 18.0)).iterator();
      if (var2.hasNext()) {
         StadiumEntity st = (StadiumEntity)var2.next();
         double dx = this.getX() - st.getX();
         double dz = this.getZ() - st.getZ();
         if (dx * dx + dz * dz < 100.0) {
            st.addScar(this.getX(), this.getZ(), r);
         }
      }
   }

   public float abilityAge(float partial) {
      return this.clientAbility == 0 ? -1.0F : (float)(this.tickCount - this.clientAbilityStart) + partial;
   }

   @Override
   protected void lerpToImpl(double x, double y, double z) {
      this.lerpX = x;
      this.lerpY = y;
      this.lerpZ = z;
      this.lerpSteps = 2;
      if (this.position().distanceToSqr(x, y, z) > 64.0) {
         this.setPos(x, y, z);
         this.lerpSteps = 0;
      }
   }

   private void clientTick() {
      if (this.lerpSteps > 0) {
         this.setPos(
            this.getX() + (this.lerpX - this.getX()) / (double)this.lerpSteps,
            this.getY() + (this.lerpY - this.getY()) / (double)this.lerpSteps,
            this.getZ() + (this.lerpZ - this.getZ()) / (double)this.lerpSteps
         );
         this.lerpSteps--;
      }

      if (!this.soundStarted) {
         this.soundStarted = true;
         ClientHooks.startSpinSound(this);
      }

      float ratio = this.spinRatio();
      int state = this.getState();
      this.spinAngleO = this.spinAngle;
      this.precessO = this.precess;
      float speed;
      if (state == 1) {
         speed = Math.max(0.0F, 34.0F * (1.0F - (float)(this.tickCount - this.clientStateStart) / 40.0F));
      } else if (state == 2) {
         speed = 70.0F;
      } else {
         speed = spinSpeedFor(ratio);
      }

      int ab = this.getAbility();
      float at = this.abilityAge(0.0F);
      float s2 = this.clientSpinSpeed(ab, at, speed);
      if (s2 < 0.0F) {
         speed = s2 * 1.45F;
      } else if (s2 != speed) {
         speed = Math.max(speed, s2 * 1.45F);
      }

      this.spinAngle += speed;
      this.precess = this.precess
         + (state == 1 ? Math.max(0.0F, 22.0F * (1.0F - (float)(this.tickCount - this.clientStateStart) / 50.0F)) : 10.0F + (1.0F - ratio) * 22.0F);
      if (this.spinAngle > 36000.0F) {
         this.spinAngle -= 36000.0F;
         this.spinAngleO -= 36000.0F;
      }

      if (this.precess > 36000.0F) {
         this.precess -= 36000.0F;
         this.precessO -= 36000.0F;
      }

      if (!this.trailInit) {
         this.trailInit = true;

         for (int i = 0; i < this.trailX.length; i++) {
            this.trailX[i] = this.getX();
            this.trailY[i] = this.getY();
            this.trailZ[i] = this.getZ();
         }
      }

      for (int i = this.trailX.length - 1; i > 0; i--) {
         this.trailX[i] = this.trailX[i - 1];
         this.trailY[i] = this.trailY[i - 1];
         this.trailZ[i] = this.trailZ[i - 1];
      }

      this.trailX[0] = this.getX();
      this.trailY[0] = this.getY();
      this.trailZ[0] = this.getZ();
      Level lv = this.level();
      double vx = this.getX() - this.xo;
      double vz = this.getZ() - this.zo;
      double sp = Math.sqrt(vx * vx + vz * vz);
      if (state == 0 && this.onGround() && sp > 0.2 && this.random.nextInt(2) == 0) {
         Px.add(lv, ParticleTypes.ELECTRIC_SPARK, this.getX() - vx * 2.0, this.getY() + 0.03, this.getZ() - vz * 2.0, -vx * 0.5, 0.05, -vz * 0.5);
      }

      if (state == 0 && ratio < 0.2F && this.random.nextInt(6) == 0) {
         Px.add(lv, ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.02, 0.0);
      }

      if (this.isRival() && state == 0 && this.random.nextInt(3) == 0) {
         double a = this.random.nextDouble() * Math.PI * 2.0;
         Px.add(lv, RED, this.getX() + Math.cos(a) * 0.5, this.getY() + 0.5, this.getZ() + Math.sin(a) * 0.5, 0.0, 0.02, 0.0);
      }

      if (state == 2) {
         Px.add(lv, LIME, this.getX(), this.getY() + 0.3, this.getZ(), 0.0, 0.0, 0.0);
         Px.add(lv, ParticleTypes.END_ROD, this.getX(), this.getY() + 0.3, this.getZ(), 0.0, 0.0, 0.0);
      }

      if (this.isShielded()) {
         this.clientBuffParticles(lv);
      }

      if (ab != 0) {
         this.clientAbilityParticles(ab, at, lv);
      }
   }

   protected void clientBuffParticles(Level lv) {
      if (this.random.nextInt(3) == 0) {
         double a = this.random.nextDouble() * Math.PI * 2.0;
         double b = this.random.nextDouble() * Math.PI * 0.5;
         Px.add(
            lv,
            ParticleTypes.END_ROD,
            this.getX() + Math.cos(a) * Math.cos(b) * 1.3,
            this.getY() + 0.3 + Math.sin(b) * 1.3,
            this.getZ() + Math.sin(a) * Math.cos(b) * 1.3,
            0.0,
            0.0,
            0.0
         );
      }
   }

   protected void clientAbilityParticles(int ab, float at, Level lv) {
      switch (ab) {
         case 1:
            if (this.random.nextInt(2) == 0) {
               Px.add(
                  lv,
                  ParticleTypes.NOTE,
                  this.getX() + (this.random.nextDouble() - 0.5) * 2.0,
                  this.getY() + 0.6 + this.random.nextDouble(),
                  this.getZ() + (this.random.nextDouble() - 0.5) * 2.0,
                  0.25 + this.random.nextDouble() * 0.2,
                  0.0,
                  0.0
               );
            }
         case 2:
         default:
            break;
         case 3:
            if (at < 24.0F) {
               for (int i = 0; i < 4; i++) {
                  double a = this.random.nextDouble() * Math.PI * 2.0;
                  double r = 1.0 + this.random.nextDouble() * 3.0;
                  double x = this.getX() + Math.cos(a) * r;
                  double z = this.getZ() + Math.sin(a) * r;
                  Px.add(lv, SAND, x, this.getY() + 0.1, z, -Math.cos(a) * 0.25 - Math.sin(a) * 0.2, 0.05, -Math.sin(a) * 0.25 + Math.cos(a) * 0.2);
               }

               Px.add(
                  lv,
                  ParticleTypes.FLAME,
                  this.getX(),
                  this.getY() + 0.1,
                  this.getZ(),
                  (this.random.nextDouble() - 0.5) * 0.2,
                  0.05,
                  (this.random.nextDouble() - 0.5) * 0.2
               );
            } else if (at < 54.0F) {
               for (int i = 0; i < 6; i++) {
                  double a = this.random.nextDouble() * Math.PI * 2.0;
                  double h = this.random.nextDouble() * 9.0;
                  double r = 1.4 + h * 0.13;
                  double x = this.abilityX + Math.cos(a) * r;
                  double z = this.abilityZ + Math.sin(a) * r;
                  ParticleOptions p = (ParticleOptions)(i % 3 == 0 ? ParticleTypes.FLAME : (i % 3 == 1 ? SAND : ParticleTypes.SMALL_FLAME));
                  Px.add(lv, p, x, this.abilityY + h, z, Math.sin(a) * 0.35, 0.12, -Math.cos(a) * 0.35);
               }
            }
            break;
         case 4:
            if (at >= 12.0F && at < 32.0F) {
               for (int i = 0; i < 3; i++) {
                  double a = this.random.nextDouble() * Math.PI * 2.0;
                  double b = (this.random.nextDouble() - 0.5) * Math.PI;
                  double r = 2.2;
                  double ox = Math.cos(a) * Math.cos(b) * r;
                  double oy = Math.sin(b) * r;
                  double oz = Math.sin(a) * Math.cos(b) * r;
                  Px.add(lv, ParticleTypes.END_ROD, this.getX() + ox, this.getY() + 0.4 + oy, this.getZ() + oz, -ox * 0.1, -oy * 0.1, -oz * 0.1);
               }
            }
            break;
         case 5:
            for (int i = 0; i < 2; i++) {
               double a = this.random.nextDouble() * Math.PI * 2.0;
               double r = this.random.nextDouble() * 6.0;
               Px.add(lv, GOLD, this.abilityX + Math.cos(a) * r, this.abilityY + 0.1, this.abilityZ + Math.sin(a) * r, 0.0, 0.06, 0.0);
            }

            if (at >= 22.0F && at < 86.0F && this.tickCount % 2 == 0) {
               for (Entity e : this.enemiesWithin(14.0)) {
                  double f = this.random.nextDouble();
                  double x = Mth.lerp(f, e.getX(), this.getX());
                  double y = Mth.lerp(f, e.getY() + (double)e.getBbHeight() * 0.5, this.getY() + 0.4);
                  double z = Mth.lerp(f, e.getZ(), this.getZ());
                  Px.add(
                     lv,
                     GOLD,
                     x,
                     y,
                     z,
                     (this.getX() - e.getX()) * 0.03,
                     (this.getY() - e.getY()) * 0.03,
                     (this.getZ() - e.getZ()) * 0.03
                  );
               }
            }
      }
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.hasUUID("Owner")) {
         this.entityData.set(DATA_OWNER, Optional.of(tag.getUUID("Owner")));
      }

      if (tag.contains("MaxSpin")) {
         this.entityData.set(DATA_MAX_SPIN, tag.getFloat("MaxSpin"));
      }

      if (tag.contains("Spin")) {
         this.entityData.set(DATA_SPIN, tag.getFloat("Spin"));
      }

      int st = tag.getInt("State");
      this.entityData.set(DATA_STATE, st == 2 ? 2 : (st == 1 ? 1 : 0));
      if (st == 2) {
         this.noPhysics = true;
         this.setNoGravity(true);
      }

      this.frozen = tag.getBoolean("Frozen");
      if (tag.contains("Level")) {
         this.entityData.set(DATA_LEVEL, Mth.clamp(tag.getInt("Level"), 1, 30));
      }

      this.entityData.set(DATA_XP, tag.getInt("Xp"));
      if (tag.contains("Mode")) {
         this.entityData.set(DATA_MODE, Mth.clamp(tag.getInt("Mode"), 0, 3));
      }

      this.entityData.set(DATA_ORDER, Mth.clamp(tag.getInt("Order"), 0, 13));
      this.entityData.set(DATA_SPIRIT, Mth.clamp(tag.getInt("Spirit"), 0, 100));
      if (tag.contains("Level") && !tag.contains("MaxSpin")) {
         this.entityData.set(DATA_MAX_SPIN, maxSpinFor(this.getLevel()));
      }

      if (tag.contains("Item")) {
         this.stored = V.loadStack(this.level(), tag.getCompound("Item"));
      }

      this.refreshLoadout();
      if (tag.contains("HomeX")) {
         this.home = new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ"));
      }

      long now = this.level() == null ? 0L : this.level().getGameTime();

      for (int i = 0; i < 6; i++) {
         int left = tag.getInt("Cooldown" + (i + 1));
         if (left > 0) {
            this.entityData.set(DATA_COOLDOWN[i], (int)(now + (long)left));
         }
      }
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      UUID o = this.getOwnerId();
      if (o != null) {
         tag.putUUID("Owner", o);
      }

      tag.putFloat("Spin", this.getSpin());
      tag.putFloat("MaxSpin", this.getMaxSpin());
      tag.putInt("State", this.getState());
      if (this.frozen) {
         tag.putBoolean("Frozen", true);
      }

      tag.putInt("Level", this.getLevel());
      tag.putInt("Xp", this.getXp());
      tag.putInt("Mode", this.getMode());
      tag.putInt("Order", this.getOrder());
      tag.putInt("Spirit", this.getSpirit());
      if (!this.stored.isEmpty()) {
         tag.put("Item", V.saveStack(this.level(), this.stored));
      }

      if (this.home != null) {
         tag.putDouble("HomeX", this.home.x);
         tag.putDouble("HomeY", this.home.y);
         tag.putDouble("HomeZ", this.home.z);
      }

      for (int i = 1; i <= 6; i++) {
         tag.putInt("Cooldown" + i, this.cooldownRemaining(i));
      }
   }

   public void refreshLoadout() {
      this.loadout = Loadout.of(this.stored.isEmpty() ? this.defaultItem() : this.stored);
      this.entityData.set(DATA_PARTS, this.loadout.encode());
   }

   public Loadout parts() {
      if (!this.level().isClientSide()) {
         if (this.loadout == null) {
            this.refreshLoadout();
         }

         return this.loadout;
      } else {
         String k = (String)this.entityData.get(DATA_PARTS);
         if (this.clientLoadout == null || !k.equals(this.clientPartsKey)) {
            this.clientPartsKey = k;
            this.clientLoadout = Loadout.decode(this.defaultItem().getItem() instanceof LibraItem li ? li.kind() : this.kindId(), k);
         }

         return this.clientLoadout;
      }
   }

   public float partsSpinMul() {
      return 0.92F + 0.08F * Loadout.mul(this.parts().stamina());
   }

   public boolean heightEdge(BeybladeEntity o) {
      int a = this.parts().height();
      int b = o.parts().height();
      return this.parts().trackTrait() == BeyPart.Trait.TWIN_HEIGHT && b <= 145 ? true : a - b >= 20;
   }

   public boolean partsUpperKick() {
      BeyPart.Trait t = this.parts().trackTrait();
      return t == BeyPart.Trait.UPPER_WING || t == BeyPart.Trait.CLAW || t == BeyPart.Trait.GRAVITY;
   }

   public float partsClashMul(BeybladeEntity o) {
      float m = (float)Math.pow((double)(Math.max(20.0F, this.parts().weight()) / Math.max(20.0F, o.parts().weight())), 0.35);
      if (this.heightEdge(o)) {
         BeyPart.Trait t = this.parts().trackTrait();
         m *= t == BeyPart.Trait.GRAVITY ? 1.2F : (t == BeyPart.Trait.CLAW ? 1.18F : 1.1F);
      }

      return Mth.clamp(m, 0.7F, 1.4F);
   }

   public void debugLock(BeybladeEntity o, int ticks) {
      this.lockTicks = ticks;
      o.lockTicks = ticks + 5;
      this.lockPartner = o;
      o.lockPartner = this;
   }

   public void setFrozen(boolean f) {
      this.frozen = f;
   }

   /** Test hook: a heavy blow that bursts the bey. */
   public void debugBurst() {
      this.level().broadcastEntityEvent(this, (byte)61);
      this.defeat();
   }

   /** Test hook: plain sleep out. */
   public void debugSleepOut() {
      this.defeat();
   }

   public void debugForceAbility(int id) {
      this.entityData.set(DATA_COOLDOWN[id - 1], 0);
      this.pendingAbility = 0;
      if (id != this.buffAbility() && this.getAbility() != 0) {
         this.endAbility();
      }

      this.tryAbility(id, false);
   }

   static {
      for (int i = 0; i < 6; i++) {
         DATA_COOLDOWN[i] = SynchedEntityData.defineId(BeybladeEntity.class, EntityDataSerializers.INT);
      }

      rivalAbilities = true;
      GOLD = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.3F), 1.4F);
      LIME = new DustParticleOptions(new Vector3f(0.6F, 1.0F, 0.25F), 1.1F);
      SAND = new DustParticleOptions(new Vector3f(0.92F, 0.78F, 0.5F), 1.6F);
      STEAL = new DustParticleOptions(new Vector3f(0.9F, 0.05F, 0.2F), 1.3F);
      RED = new DustParticleOptions(new Vector3f(1.0F, 0.15F, 0.1F), 0.9F);
   }
}
