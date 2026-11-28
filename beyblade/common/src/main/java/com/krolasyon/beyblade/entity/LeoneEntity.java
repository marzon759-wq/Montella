package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class LeoneEntity extends BeybladeEntity {
   public static final int LE_WALL = 1;
   public static final int LE_FANG = 2;
   public static final int LE_POUNCE = 3;
   public static final int LE_QUAKE = 4;
   public static final int LE_CAGE = 5;
   public static final int LE_GIANT = 6;
   public static final int[] LE_DURATION = new int[]{0, 0, 54, 60, 92, 250, 282};
   public static final int[] LE_COOLDOWN = new int[]{0, 550, 450, 600, 650, 1000, 1300};
   public static final int WALL_TICKS = 180;
   public static final double WALL_RADIUS = 1.7;
   public static final int FANG_FIRST = 6;
   public static final int FANG_STEP = 2;
   public static final int FANG_COUNT = 20;
   public static final int FANG_FLIGHT = 6;
   public static final int P_LEAP = 12;
   public static final int P_DASH = 20;
   public static final int P_DASH_TICKS = 10;
   public static final int Q_SLAM = 14;
   public static final int Q_RINGS = 3;
   public static final int Q_RING_GAP = 10;
   public static final int Q_RING_LIFE = 18;
   public static final float Q_SPEED = 0.48F;
   public static final double Q_RADIUS = 9.0;
   public static final int C_FORM = 14;
   public static final int C_BLAST = 205;
   public static final int C_FADE = 45;
   public static final int G_FORM = 16;
   public static final int G_END = 205;
   public static final int G_FADE = 45;
   public static final int G_TRAVEL = 32;
   public static final int G_LIFT = 36;
   public static final double G_PULL = 40.0;
   public static final double G_CAPTURE = 4.6;
   public static final double G_TOP = 24.0;
   public static final double G_TOP_R = 22.0;
   public static final double G_TORNADO_H = 70.0;
   public static final double C_PILLAR_DIST = 5.2;
   public static final double C_PULL_RANGE = 20.0;
   public static final double C_HOLD_HEIGHT = 2.4;
   public static final byte EV_QUAKE = 80;
   public static final byte EV_CAGE = 81;
   public static final byte EV_CAGE_BLAST = 82;
   public static final byte EV_WALL_HIT = 83;
   public static final byte EV_GIANT = 84;
   public static final byte EV_GIANT_END = 85;
   private static final DustParticleOptions LEAF = new DustParticleOptions(new Vector3f(0.3F, 0.95F, 0.4F), 1.2F);
   private static final DustParticleOptions DUST = new DustParticleOptions(new Vector3f(0.55F, 0.48F, 0.38F), 1.8F);
   private Vec3 pounceDir = Vec3.ZERO;
   private int quakeStart = -1;
   @Nullable
   private Vec3 cageCenter;
   private boolean stormSet;
   private final Map<Integer, double[]> captured = new HashMap<>();
   private final Map<Integer, Integer> released = new HashMap<>();
   private final Map<Integer, Integer> slamming = new HashMap<>();
   private final Set<Integer>[] ringHits = new Set[]{new HashSet(), new HashSet(), new HashSet()};
   private final Set<Integer> pounceHits = new HashSet<>();
   public int giantTime = -1000;
   public int quakeTime = -1000;
   public int cageTime = -1000;
   public int cageBlastTime = -1000;
   public int wallHitTime = -1000;
   public double cageX;
   public double cageY;
   public double cageZ;

   private void clearStorm(ServerLevel sl) {
      if (this.stormSet) {
         this.stormSet = false;
         sl.setWeatherParameters(6000, 0, false, false);
      }
   }

   @Override
   protected void endAbility() {
      if (this.stormSet
         && this.level() instanceof ServerLevel sl
         && (this.getAbility() == 5 && this.abilityTicks < 245 || this.getAbility() == 6 && this.abilityTicks < 277)) {
         this.clearStorm(sl);
      }

      for (Integer id : this.slamming.keySet()) {
         Entity e = this.level().getEntity(id);
         if (e != null) {
            e.setNoGravity(false);
         }
      }

      this.slamming.clear();

      for (Integer idx : this.captured.keySet()) {
         Entity e = this.level().getEntity(idx);
         if (e != null) {
            e.setNoGravity(false);
         }
      }

      this.captured.clear();
      super.endAbility();
   }

   public LeoneEntity(EntityType<? extends LeoneEntity> type, Level level) {
      super(type, level);
   }

   @Override
   protected float dodgeChance() {
      return 0.08F;
   }

   @Override
   public int nativeMode() {
      return 1;
   }

   @Override
   public String kindId() {
      return "leone";
   }

   @Override
   public String abilityKey(int id) {
      return "ability.librabeyblade.leone." + id;
   }

   @Override
   public int duration(int id) {
      return LE_DURATION[id];
   }

   @Override
   public int cooldown(int id) {
      return LE_COOLDOWN[id];
   }

   @Override
   public int buffAbility() {
      return 1;
   }

   @Override
   public int buffTicks() {
      return 180;
   }

   @Override
   public boolean requiresTarget(int id) {
      return id == 2 || id == 3;
   }

   @Override
   protected boolean ramsInto(int id) {
      return id == 2 || id == 3 || id == 4 || id == 5;
   }

   @Override
   public int abilityCount() {
      return 6;
   }

   @Override
   public boolean buffReflects() {
      return true;
   }

   @Override
   public float incomingMul() {
      return this.isShielded() ? 0.5F : 0.75F;
   }

   @Override
   protected ItemStack defaultItem() {
      return new ItemStack((ItemLike)ModItems.LEONE.get());
   }

   @Override
   protected float clientSpinSpeed(int ab, float at, float speed) {
      if (ab == 3 && at >= 12.0F) {
         return 100.0F;
      } else {
         return ab == 5 ? 95.0F : speed;
      }
   }

   @Override
   protected void onBuffStart(@Nullable Player owner) {
      this.play((SoundEvent)ModSounds.WIND_WALL.get(), 1.8F, 1.0F);
      this.play((SoundEvent)ModSounds.LION_ROAR.get(), 1.0F, 1.3F);
   }

   @Override
   protected void tickBuff() {
      this.tickShield();
      ServerLevel sl = (ServerLevel)this.level();

      for (Entity e : this.enemiesWithin(2.6)) {
         Vec3 out = flat(e.position().subtract(this.position()));
         double d = out.length();
         out = d < 0.001 ? new Vec3(1.0, 0.0, 0.0) : out.scale(1.0 / d);
         Vec3 v = e.getDeltaMovement();
         if (v.dot(out) < 0.4) {
            e.setDeltaMovement(v.add(out.scale(0.45)).add(0.0, 0.12, 0.0));
            e.hurtMarked = true;
            if (e instanceof BeybladeEntity b) {
               b.damageSpin(8.0F, this);
            } else if (e instanceof LivingEntity) {
               LivingEntity l = (LivingEntity)e;
               if (l.hurtTime == 0) {
                  l.hurt(this.damageSources().indirectMagic(this, this.getOwner()), 2.0F);
               }
            }

            if (this.tickCount % 3 == 0) {
               this.level().broadcastEntityEvent(this, (byte)83);
               Px.send(sl, ParticleTypes.CLOUD, e.getX(), e.getY() + 0.4, e.getZ(), 4, 0.2, 0.2, 0.2, 0.05);
            }
         }
      }
   }

   @Override
   protected void shieldHit() {
      this.play((SoundEvent)ModSounds.FANG_SHOT.get(), 1.0F, 0.7F);
      this.level().broadcastEntityEvent(this, (byte)83);
   }

   @Override
   protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
      ServerLevel sl = (ServerLevel)this.level();
      switch (ab) {
         case 2:
            if (this.target != null && this.target.isAlive()) {
               if (t == 1) {
                  this.play((SoundEvent)ModSounds.LION_ROAR.get(), 1.2F, 1.4F);
               }

               this.hover(0.5, 0.25);
               int rel = t - 6;
               if (rel >= 0 && rel % 2 == 0 && rel / 2 < 20) {
                  this.play((SoundEvent)ModSounds.FANG_SHOT.get(), 0.8F, 0.9F + this.random.nextFloat() * 0.4F);
               }

               int landed = t - 6 - 6;
               if (landed >= 0 && landed % 2 == 0 && landed / 2 < 20) {
                  Vec3 push = flat(this.target.position().subtract(this.position()));
                  push = push.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : push.normalize();
                  if (this.target instanceof BeybladeEntity b) {
                     b.damageSpin(10.0F, this);
                     b.setDeltaMovement(b.getDeltaMovement().add(push.scale(0.08)));
                  } else if (this.target instanceof LivingEntity l) {
                     l.invulnerableTime = 0;
                     l.hurt(this.damageSources().indirectMagic(this, owner), 1.5F);
                     l.knockback(0.15, -push.x, -push.z);
                  }

                  Px.send(sl, LEAF, this.target.getX(), this.target.getY() + 0.5, this.target.getZ(), 4, 0.2, 0.2, 0.2, 0.0);
               }

               return true;
            }

            this.endAbilityEarly();
            return true;
         case 3:
            if (t == 1) {
               this.play((SoundEvent)ModSounds.LION_ROAR.get(), 2.0F, 0.9F);
               this.pounceHits.clear();
            }

            if (t < 12) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 1.0, 0.5));
               return true;
            } else if (t < 20) {
               this.hover(3.0, 0.3);
               if (this.target != null) {
                  Vec3 d = this.target.position().add(0.0, 0.3, 0.0).subtract(this.position());
                  this.pounceDir = d.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : d.normalize();
               }

               return true;
            } else {
               if (t == 20) {
                  this.play((SoundEvent)ModSounds.WIND_WALL.get(), 1.6F, 1.6F);
               }

               if (t < 30) {
                  this.setNoGravity(true);
                  this.setDeltaMovement(this.pounceDir.scale(1.5));

                  for (Entity e : this.enemiesWithin(2.0)) {
                     if (this.pounceHits.add(e.getId())) {
                        Vec3 side = flat(e.position().subtract(this.position()));
                        side = side.lengthSqr() < 1.0E-4 ? new Vec3(this.pounceDir.z, 0.0, -this.pounceDir.x) : side.normalize();
                        if (e instanceof BeybladeEntity b) {
                           b.damageSpin(130.0F, this);
                           b.setDeltaMovement(b.getDeltaMovement().add(side.scale(0.7)).add(0.0, 0.6, 0.0));
                        } else if (e instanceof LivingEntity l) {
                           l.hurt(this.damageSources().indirectMagic(this, owner), 9.0F);
                           l.knockback(1.2, -side.x, -side.z);
                           l.setDeltaMovement(l.getDeltaMovement().add(0.0, 0.5, 0.0));
                        }

                        Px.send(sl, ParticleTypes.SWEEP_ATTACK, e.getX(), e.getY() + 0.5, e.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                        Px.send(sl, ParticleTypes.CLOUD, e.getX(), e.getY() + 0.5, e.getZ(), 8, 0.3, 0.3, 0.3, 0.1);
                        this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 1.2F, 0.8F);
                     }
                  }

                  return true;
               }

               this.setNoGravity(false);
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
               return true;
            }
         case 4:
            if (t == 1) {
               this.quakeStart = -1;

               for (Set<Integer> s : this.ringHits) {
                  s.clear();
               }
            }

            if (t < 14) {
               this.hover(1.6, 0.3);
               return true;
            } else {
               if (t == 14) {
                  this.setNoGravity(false);
                  this.setDeltaMovement(0.0, -1.6, 0.0);
               }

               if (this.quakeStart < 0 && (this.onGround() || t > 26)) {
                  int newT = LE_DURATION[4] - 40;
                  if (t < newT) {
                     this.abilityTicks = newT;
                     t = newT;
                  }

                  this.quakeStart = t;
                  this.play((SoundEvent)ModSounds.QUAKE.get(), 2.4F, 1.0F);
                  this.level().broadcastEntityEvent(this, (byte)80);
                  BlockState ground = this.level().getBlockState(BlockPos.containing(this.getX(), this.getY() - 0.2, this.getZ()));
                  if (!ground.isAir()) {
                     Px.send(
                        sl,
                        new BlockParticleOption(ParticleTypes.BLOCK, ground),
                        this.getX(),
                        this.getY() + 0.1,
                        this.getZ(),
                        60,
                        1.5,
                        0.1,
                        1.5,
                        0.3
                     );
                  }
               }

               if (this.quakeStart >= 0) {
                  this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 1.0, 0.3));

                  for (int ix = 0; ix < 3; ix++) {
                     int age = t - this.quakeStart - ix * 10;
                     if (age >= 0 && age < 18) {
                        if (age == 0 && ix > 0) {
                           this.play((SoundEvent)ModSounds.QUAKE.get(), 1.4F, 1.2F + (float)ix * 0.15F);
                        }

                        double front = 0.8 + (double)((float)age * 0.48F) * (1.0 + (double)ix * 0.25);

                        for (Entity exxx : this.enemiesWithin(front + 0.6)) {
                           double dx = Math.sqrt(flat(exxx.position().subtract(this.position())).lengthSqr());
                           if (!(dx < front - 1.2) && this.ringHits[ix].add(exxx.getId())) {
                              if (exxx instanceof BeybladeEntity b) {
                                 b.damageSpin(55.0F, this);
                                 b.stun(30);
                                 b.setDeltaMovement(b.getDeltaMovement().multiply(0.2, 1.0, 0.2).add(0.0, 0.45, 0.0));
                              } else if (exxx instanceof LivingEntity l) {
                                 l.hurt(this.damageSources().indirectMagic(this, owner), 5.0F);
                                 l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 5));
                                 l.setDeltaMovement(l.getDeltaMovement().add(0.0, 0.55, 0.0));
                                 l.hurtMarked = true;
                              }

                              Px.send(sl, DUST, exxx.getX(), exxx.getY() + 0.2, exxx.getZ(), 10, 0.4, 0.2, 0.4, 0.0);
                           }
                        }
                     }
                  }
               }

               return true;
            }
         case 5:
            if (t == 1) {
               this.cageCenter = this.position();
               this.level().broadcastEntityEvent(this, (byte)81);
               this.play((SoundEvent)ModSounds.CAGE_WIND.get(), 2.2F, 0.9F);
               this.play((SoundEvent)ModSounds.LION_ROAR.get(), 1.6F, 1.0F);
               if (!sl.isThundering()) {
                  this.stormSet = true;
                  sl.setWeatherParameters(0, LE_DURATION[5] + 60, true, true);
               }
            }

            if (this.cageCenter == null) {
               this.cageCenter = this.position();
            }

            if (t >= 14 && t < 205 && t % 35 == 0) {
               LightningBolt bolt = (LightningBolt)EntityType.LIGHTNING_BOLT.create(sl);
               if (bolt != null) {
                  Vec3 p = pillar(this.cageCenter.x, this.cageCenter.y, this.cageCenter.z, this.random.nextInt(4), (float)t);
                  bolt.moveTo(p.x + this.random.nextGaussian() * 2.0, this.cageCenter.y, p.z + this.random.nextGaussian() * 2.0);
                  bolt.setVisualOnly(true);
                  sl.addFreshEntity(bolt);
               }
            }

            if (t == 245) {
               this.clearStorm(sl);
            }

            if (t >= 14 && t < 205) {
               if ((t - 14) % 30 == 0) {
                  this.play((SoundEvent)ModSounds.CAGE_WIND.get(), 2.0F, 0.85F + this.random.nextFloat() * 0.2F);
               }

               for (Entity exx : this.enemiesWithin(20.0)) {
                  Vec3 best = null;
                  double bd = 1.0E9;

                  for (int i = 0; i < 4; i++) {
                     Vec3 p = pillar(this.cageCenter.x, this.cageCenter.y, this.cageCenter.z, i, (float)t);
                     double dx = flat(p.subtract(exx.position())).lengthSqr();
                     if (dx < bd) {
                        bd = dx;
                        best = p;
                     }
                  }

                  Vec3 to = flat(best.subtract(exx.position()));
                  double dx = Math.sqrt(bd);
                  if (!(dx < 0.001)) {
                     Vec3 dir = to.scale(1.0 / dx);
                     Vec3 swirl = new Vec3(-dir.z, 0.0, dir.x);
                     double k = exx == this.target ? 1.4 : 1.0;
                     double pull = dx < 1.8 ? 0.03 : Math.min(0.22, 0.9 / (dx + 1.5)) * k;
                     Vec3 v = exx.getDeltaMovement().add(dir.scale(pull)).add(swirl.scale(dx < 3.2 ? 0.2 : 0.06));
                     if (dx < 3.0) {
                        v = new Vec3(v.x, Math.min(0.32, v.y + 0.1), v.z);
                        exx.fallDistance = 0.0F;
                        if (exx instanceof BeybladeEntity b) {
                           b.damageSpin(exx == this.target ? 5.0F : 3.0F, this);
                        } else if (exx instanceof LivingEntity l && t % 6 == 0) {
                           l.invulnerableTime = 0;
                           l.hurt(this.damageSources().indirectMagic(this, owner), 1.5F);
                        }
                     }

                     exx.setDeltaMovement(v);
                     exx.hurtMarked = true;
                  }
               }
            }

            if (t == 205) {
               this.play((SoundEvent)ModSounds.CAGE_BLAST.get(), 2.8F, 0.9F);
               this.level().broadcastEntityEvent(this, (byte)82);

               for (Entity exx : this.enemiesWithin(9.0)) {
                  Vec3 out = flat(exx.position().subtract(this.position()));
                  out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
                  if (exx instanceof BeybladeEntity b) {
                     b.damageSpin(exx == this.target ? 120.0F : 60.0F, this);
                     b.setDeltaMovement(out.scale(1.0).add(0.0, 0.3, 0.0));
                  } else if (exx instanceof LivingEntity l) {
                     l.hurt(this.damageSources().indirectMagic(this, owner), exx == this.target ? 10.0F : 5.0F);
                     l.knockback(1.5, -out.x, -out.z);
                  }
               }
            }

            return t < 14;
         case 6:
            if (t <= 32) {
               if (t == 1) {
                  this.play((SoundEvent)ModSounds.LION_ROAR.get(), 2.0F, 0.9F);
                  Vec3 c = this.position();
                  Iterator var33 = this.level().getEntitiesOfClass(StadiumEntity.class, this.getBoundingBox().inflate(20.0, 6.0, 20.0)).iterator();
                  if (var33.hasNext()) {
                     StadiumEntity st = (StadiumEntity)var33.next();
                     double dx = this.getX() - st.getX();
                     double dz = this.getZ() - st.getZ();
                     if (dx * dx + dz * dz < 123.21001F) {
                        c = new Vec3(st.getX(), this.getY(), st.getZ());
                     }
                  }

                  this.cageCenter = c;
               }

               Vec3 d = flat(this.cageCenter.subtract(this.position()));
               if (d.length() > 0.5 && t < 32) {
                  Vec3 v = d.normalize().scale(Math.min(0.75, d.length() * 0.5));
                  this.setDeltaMovement(v.x, this.getDeltaMovement().y, v.z);
                  return true;
               }

               this.abilityTicks = 33;
               this.cageCenter = new Vec3(this.getX(), this.getY(), this.getZ());
               this.captured.clear();
               this.released.clear();
               this.level().broadcastEntityEvent(this, (byte)84);
               this.play((SoundEvent)ModSounds.CAGE_WIND.get(), 3.0F, 0.6F);
               this.play((SoundEvent)ModSounds.LION_ROAR.get(), 2.4F, 0.8F);
               if (!sl.isThundering()) {
                  this.stormSet = true;
                  sl.setWeatherParameters(0, LE_DURATION[6] + 60, true, true);
               }

               return true;
            }

            if (this.cageCenter == null) {
               this.cageCenter = this.position();
            }

            int a = t - 32;
            Vec3 hold = flat(this.cageCenter.subtract(this.position()));
            this.setDeltaMovement(hold.x * 0.4, this.getDeltaMovement().y, hold.z * 0.4);
            if (a >= 16 && a < 205 && a % 30 == 0) {
               LightningBolt bolt = (LightningBolt)EntityType.LIGHTNING_BOLT.create(sl);
               if (bolt != null) {
                  double ang = this.random.nextDouble() * Math.PI * 2.0;
                  double r = 8.0 + this.random.nextDouble() * 10.0;
                  bolt.moveTo(this.cageCenter.x + Math.cos(ang) * r, this.cageCenter.y, this.cageCenter.z + Math.sin(ang) * r);
                  bolt.setVisualOnly(true);
                  sl.addFreshEntity(bolt);
               }
            }

            if (a == 245) {
               this.clearStorm(sl);
            }

            if (a >= 16 && a < 205) {
               if ((a - 16) % 25 == 0) {
                  this.play((SoundEvent)ModSounds.CAGE_WIND.get(), 3.0F, 0.6F + this.random.nextFloat() * 0.15F);
               }

               for (Entity ex : this.enemiesWithin(40.0)) {
                  if (!this.captured.containsKey(ex.getId())) {
                     Vec3 to = flat(this.cageCenter.subtract(ex.position()));
                     double dx = to.length();
                     if (!(dx < 0.001)) {
                        Integer relx = this.released.get(ex.getId());
                        boolean cooling = relx != null && t - relx < 45;
                        if (!cooling && dx < 4.6) {
                           this.captured
                              .put(
                                 ex.getId(),
                                 new double[]{(double)t, Math.atan2(ex.getZ() - this.cageCenter.z, ex.getX() - this.cageCenter.x)}
                              );
                           ex.setNoGravity(true);
                           this.play((SoundEvent)ModSounds.CAGE_WIND.get(), 1.6F, 1.4F);
                        } else {
                           Vec3 dir = to.scale(1.0 / dx);
                           Vec3 swirl = new Vec3(-dir.z, 0.0, dir.x);
                           double pull = cooling ? 0.02 : Math.min(0.26, 2.4 / (dx + 3.0)) * (ex == this.target ? 1.3 : 1.0);
                           ex.setDeltaMovement(ex.getDeltaMovement().add(dir.scale(pull)).add(swirl.scale(0.07)));
                           ex.hurtMarked = true;
                        }
                     }
                  }
               }
            }

            Iterator<Entry<Integer, double[]>> it = this.captured.entrySet().iterator();

            while (it.hasNext()) {
               Entry<Integer, double[]> en = it.next();
               Entity exx = this.level().getEntity(en.getKey());
               if (exx != null && exx.isAlive()) {
                  double prog = ((double)t - en.getValue()[0]) / 36.0;
                  if (prog < 1.0 && a < 205) {
                     double ang = en.getValue()[1] + prog * Math.PI * 3.0 * (double)this.spinDir();
                     double r = 3.9999999999999996 * (1.0 - 0.45 * prog);
                     double y = this.cageCenter.y + 0.3 + 24.0 * prog * (2.0 - prog);
                     exx.setPos(this.cageCenter.x + Math.cos(ang) * r, y, this.cageCenter.z + Math.sin(ang) * r);
                     exx.setDeltaMovement(new Vec3(-Math.sin(ang), 0.2, Math.cos(ang)).scale(0.3 * (double)this.spinDir()));
                     exx.hurtMarked = true;
                     exx.fallDistance = 0.0F;
                     if (exx instanceof BeybladeEntity b) {
                        b.stun(5);
                     }
                  } else {
                     exx.setNoGravity(false);
                     exx.setDeltaMovement(0.0, -3.2, 0.0);
                     exx.hurtMarked = true;
                     this.play((SoundEvent)ModSounds.CAGE_BLAST.get(), 1.6F, 1.5F);
                     this.slamming.put(en.getKey(), t);
                     it.remove();
                  }
               } else {
                  it.remove();
               }
            }

            Iterator<Entry<Integer, Integer>> it2 = this.slamming.entrySet().iterator();

            while (it2.hasNext()) {
               Entry<Integer, Integer> en = it2.next();
               Entity exx = this.level().getEntity(en.getKey());
               if (exx != null && exx.isAlive()) {
                  if (exx.getDeltaMovement().y > -1.0 && t - en.getValue() < 30 && !exx.onGround()) {
                     exx.setDeltaMovement(exx.getDeltaMovement().x, -3.2, exx.getDeltaMovement().z);
                  }

                  boolean down = exx.onGround() || exx.getY() <= this.cageCenter.y + 0.8 || t - en.getValue() > 30;
                  if (down) {
                     if (exx instanceof BeybladeEntity b) {
                        b.damageSpin(220.0F * this.powerMul(), this);
                        b.setDeltaMovement(0.0, 0.35, 0.0);
                        b.shockwave(this, 1.0);
                        this.level().broadcastEntityEvent(b, (byte)66);
                     } else if (exx instanceof LivingEntity l) {
                        l.hurt(this.damageSources().indirectMagic(this, owner), 14.0F);
                     }

                     exx.fallDistance = 0.0F;
                     this.play((SoundEvent)ModSounds.QUAKE.get(), 2.6F, 0.8F);
                     this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 2.6F, 0.5F);
                     this.released.put(en.getKey(), t);
                     it2.remove();
                  }
               } else {
                  it2.remove();
               }
            }

            if (a == 205) {
               this.play((SoundEvent)ModSounds.CAGE_BLAST.get(), 2.8F, 0.8F);
               this.level().broadcastEntityEvent(this, (byte)85);
            }

            return true;
         default:
            return false;
      }
   }

   public static Vec3 pillar(double cx, double cy, double cz, int i, float t) {
      double dir = i % 2 == 0 ? 1.0 : -1.0;
      double a = (Math.PI / 4) + (double)i * Math.PI / 2.0 + dir * (double)t * 0.065;
      double r = 5.2 + Math.sin((double)t * 0.083 + (double)i * 2.1) * 1.3 + Math.sin((double)t * 0.21 + (double)i) * 0.4;
      return new Vec3(cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r);
   }

   @Override
   public void handleEntityEvent(byte id) {
      switch (id) {
         case 80:
            this.quakeTime = this.tickCount;
            this.clientScar(4.5F);
            break;
         case 81:
            this.cageTime = this.tickCount;
            this.cageX = this.getX();
            this.cageY = this.getY();
            this.cageZ = this.getZ();
            break;
         case 82:
            this.cageBlastTime = this.tickCount;
            this.clientScar(4.0F);
            this.cageX = this.getX();
            this.cageY = this.getY();
            this.cageZ = this.getZ();
            break;
         case 83:
            this.wallHitTime = this.tickCount;
            break;
         case 84:
            this.giantTime = this.tickCount;
            this.cageX = this.getX();
            this.cageY = this.getY();
            this.cageZ = this.getZ();
            break;
         case 85:
            this.cageBlastTime = this.tickCount;
            this.cageX = this.getX();
            this.cageY = this.getY();
            this.cageZ = this.getZ();
            this.clientScar(4.0F);
            break;
         default:
            super.handleEntityEvent(id);
      }
   }

   @Override
   protected void clientBuffParticles(Level lv) {
      for (int i = 0; i < 2; i++) {
         double a = this.random.nextDouble() * Math.PI * 2.0;
         double h = this.random.nextDouble() * 2.0;
         double r = 1.7;
         Px.add(
            lv,
            (ParticleOptions)(i == 0 ? ParticleTypes.CLOUD : LEAF),
            this.getX() + Math.cos(a) * r,
            this.getY() + h,
            this.getZ() + Math.sin(a) * r,
            -Math.sin(a) * 0.35,
            0.04,
            Math.cos(a) * 0.35
         );
      }
   }

   @Override
   protected void clientAbilityParticles(int ab, float at, Level lv) {
      switch (ab) {
         case 2:
            if (this.random.nextInt(2) == 0) {
               Px.add(lv, LEAF, this.getX(), this.getY() + 0.5, this.getZ(), this.random.nextGaussian() * 0.1, 0.05, this.random.nextGaussian() * 0.1);
            }
            break;
         case 3:
            if (at < 12.0F) {
               double a = this.random.nextDouble() * Math.PI * 2.0;
               Px.add(
                  lv,
                  LEAF,
                  this.getX() + Math.cos(a) * 1.5,
                  this.getY() + 0.2,
                  this.getZ() + Math.sin(a) * 1.5,
                  -Math.cos(a) * 0.15,
                  0.05,
                  -Math.sin(a) * 0.15
               );
            } else if (at >= 20.0F && at < 30.0F) {
               for (int i = 0; i < 3; i++) {
                  Px.add(
                     lv,
                     ParticleTypes.CLOUD,
                     this.getX() + this.random.nextGaussian() * 0.3,
                     this.getY() + 0.4,
                     this.getZ() + this.random.nextGaussian() * 0.3,
                     0.0,
                     0.02,
                     0.0
                  );
               }
            }
            break;
         case 4:
            if (at < 14.0F) {
               Px.add(
                  lv,
                  DUST,
                  this.getX() + this.random.nextGaussian() * 0.5,
                  this.getY() - 0.3,
                  this.getZ() + this.random.nextGaussian() * 0.5,
                  0.0,
                  -0.05,
                  0.0
               );
            }
            break;
         case 5:
            if (at >= 14.0F && at < 205.0F) {
               for (int i = 0; i < 4; i++) {
                  Vec3 p = pillar(this.cageX, this.cageY, this.cageZ, i, at);
                  double a = this.random.nextDouble() * Math.PI * 2.0;
                  double h = this.random.nextDouble() * 6.0;
                  Px.add(
                     lv,
                     (ParticleOptions)(i % 2 == 0 ? ParticleTypes.CLOUD : LEAF),
                     p.x + Math.cos(a) * 0.8,
                     p.y + h,
                     p.z + Math.sin(a) * 0.8,
                     -Math.sin(a) * 0.3,
                     0.1,
                     Math.cos(a) * 0.3
                  );
               }
            }
      }
   }

   public static float clampAge(float x) {
      return Mth.clamp(x, 0.0F, 1.0F);
   }
}
