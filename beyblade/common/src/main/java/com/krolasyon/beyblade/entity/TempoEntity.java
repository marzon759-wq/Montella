package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TempoEntity extends BeybladeEntity {
   public static final int TE_STOP = 1;
   public static final int TE_REWIND = 2;
   public static final int TE_HANDS = 3;
   public static final int TE_ACCEL = 4;
   public static final int TE_CLOCK = 5;
   public static final int[] TE_DURATION = new int[]{0, 0, 24, 52, 56, 116};
   public static final int[] TE_COOLDOWN = new int[]{0, 650, 500, 550, 450, 1100};
   public static final int STOP_TICKS = 140;
   public static final double STOP_RADIUS = 9.0;
   public static final double HANDS_LONG = 7.5;
   public static final double HANDS_SHORT = 5.5;
   public static final double CLOCK_RANGE = 12.0;
   public static final double CLOCK_HEIGHT = 7.0;
   public static final int HANDS_START = 6;
   public static final int HANDS_END = 48;
   public static final int ACCEL_STEP = 4;
   public static final int ACCEL_HITS = 12;
   public static final int CLOCK_FORM = 14;
   public static final int CLOCK_TOLL = 92;
   public static final byte EV_TOLL = 120;
   public static final byte EV_REWIND = 121;
   private final ArrayDeque<double[]> history = new ArrayDeque<>();
   private final List<double[]> rewindPath = new ArrayList<>();
   private final Map<Integer, Integer> handHits = new HashMap<>();
   private int accelHits;
   public int tollTime = -1000;
   public int rewindTime = -1000;

   public TempoEntity(EntityType<? extends TempoEntity> type, Level level) {
      super(type, level);
   }

   @Override
   public int nativeMode() {
      return 3;
   }

   @Override
   public String kindId() {
      return "tempo";
   }

   @Override
   public String abilityKey(int id) {
      return "ability.librabeyblade.tempo." + id;
   }

   @Override
   public int duration(int id) {
      return TE_DURATION[id];
   }

   @Override
   public int cooldown(int id) {
      return TE_COOLDOWN[id];
   }

   @Override
   public int buffAbility() {
      return 1;
   }

   @Override
   public int buffTicks() {
      return 140;
   }

   @Override
   public boolean requiresTarget(int id) {
      return id == 4;
   }

   @Override
   public boolean buffReflects() {
      return false;
   }

   @Override
   protected boolean ramsInto(int id) {
      return id == 3 || id == 4 || id == 5;
   }

   @Override
   protected float dodgeChance() {
      return this.isBuffActive() ? 0.3F : 0.12F;
   }

   @Override
   protected ItemStack defaultItem() {
      return new ItemStack((ItemLike)ModItems.TEMPO.get());
   }

   @Override
   protected float clientSpinSpeed(int ab, float at, float speed) {
      if (ab == 2) {
         return -speed * 1.3F;
      } else {
         return ab == 4 ? speed * 1.5F : speed;
      }
   }

   @Override
   protected void onBuffStart(@Nullable Player owner) {
      this.play((SoundEvent)ModSounds.TIME_STOP.get(), 2.0F, 1.0F);
      this.play((SoundEvent)ModSounds.CLOCK_BELL.get(), 1.4F, 1.4F);
   }

   @Override
   protected void tickBuff() {
      this.tickShield();
      if (this.tickCount % 20 == 0) {
         this.play((SoundEvent)ModSounds.CLOCK_TICK.get(), 1.2F, 0.8F);
      }

      for (Entity e : this.enemiesWithin(9.0)) {
         e.setDeltaMovement(e.getDeltaMovement().multiply(0.08, 0.5, 0.08));
         e.hurtMarked = true;
         if (e instanceof BeybladeEntity b) {
            b.stun(4);
         } else if (e instanceof LivingEntity l) {
            l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 6, false, false));
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && this.getState() == 0 && this.getAbility() != 2) {
         this.history.addLast(new double[]{this.getX(), this.getY(), this.getZ(), (double)this.getSpin()});

         while (this.history.size() > 70) {
            this.history.removeFirst();
         }
      }
   }

   @Override
   protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
      switch (ab) {
         case 2:
            if (t == 1) {
               this.rewindPath.clear();
               List<double[]> h = new ArrayList<>(this.history);

               for (int i = h.size() - 1; i >= 0; i--) {
                  this.rewindPath.add(h.get(i));
               }

               this.play((SoundEvent)ModSounds.REWIND.get(), 2.0F, 1.0F);
               this.level().broadcastEntityEvent(this, (byte)121);
            }

            if (this.rewindPath.isEmpty()) {
               return true;
            } else {
               int idx = Math.min(this.rewindPath.size() - 1, t * 3);
               double[] p = this.rewindPath.get(idx);
               Vec3 dx = new Vec3(p[0], p[1], p[2]).subtract(this.position());
               if (this.level().noCollision(this, this.getBoundingBox().move(dx))) {
                  this.setPos(p[0], p[1], p[2]);
               }

               this.setDeltaMovement(0.0, 0.0, 0.0);
               float best = 0.0F;

               for (int i = 0; i <= idx; i++) {
                  best = Math.max(best, (float)this.rewindPath.get(i)[3]);
               }

               if (best > this.getSpin()) {
                  this.healSpin((best - this.getSpin()) * 0.35F);
               }

               return true;
            }
         case 3:
            if (t == 1) {
               this.handHits.clear();
               this.play((SoundEvent)ModSounds.CLOCK_BELL.get(), 1.8F, 1.0F);
            }

            this.setDeltaMovement(this.getDeltaMovement().multiply(0.4, 1.0, 0.4));
            if (t % 6 == 0) {
               this.play((SoundEvent)ModSounds.CLOCK_TICK.get(), 1.4F, 1.0F + (float)t * 0.01F);
            }

            if (t >= 6 && t < 48) {
               double[] angles = new double[]{minuteAngle((float)t), hourAngle((float)t)};
               double[] lengths = new double[]{7.5, 5.5};

               for (Entity ex : this.enemiesWithin(8.0)) {
                  Vec3 rel = flat(ex.position().subtract(this.position()));
                  double dist = rel.length();
                  if (!(dist < 0.4)) {
                     double a = Math.atan2(rel.z, rel.x);

                     for (int k = 0; k < 2; k++) {
                        double diff = Math.abs(Math.atan2(Math.sin(a - angles[k]), Math.cos(a - angles[k])));
                        if (!(dist > lengths[k]) && !(diff > 0.28)) {
                           Integer last = this.handHits.get(ex.getId());
                           if (last == null || t - last >= 10) {
                              this.handHits.put(ex.getId(), t);
                              Vec3 tan = new Vec3(-Math.sin(angles[k]), 0.0, Math.cos(angles[k]));
                              if (ex instanceof BeybladeEntity bx) {
                                 bx.damageSpin(70.0F * this.powerMul(), this);
                                 bx.setDeltaMovement(bx.getDeltaMovement().add(tan.scale(0.8)).add(0.0, 0.12, 0.0));
                                 this.level().broadcastEntityEvent(bx, (byte)61);
                              } else if (ex instanceof LivingEntity l) {
                                 l.hurt(this.damageSources().indirectMagic(this, owner), 7.0F);
                                 l.knockback(0.8, -tan.x, -tan.z);
                              }

                              this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 1.4F, 1.2F);
                           }
                        }
                     }
                  }
               }

               return true;
            }

            return true;
         case 4:
            if (t == 1) {
               this.accelHits = 0;
               this.play((SoundEvent)ModSounds.TIME_STOP.get(), 1.5F, 1.8F);
            }

            if (this.target != null && this.target.isAlive()) {
               if (t % 4 == 0 && this.accelHits < 12) {
                  Vec3 to = flat(this.target.position().subtract(this.position()));
                  double side = (double)(this.accelHits % 2 == 0 ? 1 : -1) * 1.4;
                  Vec3 dir = to.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : to.normalize();
                  Vec3 from = this.target.position().subtract(dir.scale(1.6)).add(new Vec3(-dir.z, 0.0, dir.x).scale(side));
                  if (this.level().noCollision(this, this.getBoundingBox().move(from.subtract(this.position())))) {
                     this.setPos(from.x, this.getY(), from.z);
                  }

                  this.accelHits++;
                  if (this.target instanceof BeybladeEntity b) {
                     if (!b.tryDodge(this)) {
                        b.damageSpin(28.0F * this.powerMul(), this);
                        b.setDeltaMovement(b.getDeltaMovement().add(dir.scale(0.18)));
                        this.level().broadcastEntityEvent(b, (byte)60);
                     }
                  } else if (this.target instanceof LivingEntity l) {
                     l.invulnerableTime = 0;
                     l.hurt(this.damageSources().indirectMagic(this, owner), 2.5F);
                  }

                  this.play((SoundEvent)ModSounds.CLASH.get(), 1.2F, 1.3F + (float)this.accelHits * 0.03F);
                  if (this.accelHits % 3 == 0) {
                     this.play((SoundEvent)ModSounds.CLOCK_TICK.get(), 1.4F, 1.6F);
                  }
               }

               this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 1.0, 0.3));
               return true;
            }

            this.endAbilityEarly();
            return true;
         case 5:
            if (t == 1) {
               this.play((SoundEvent)ModSounds.CLOCK_BELL.get(), 2.5F, 0.7F);
            }

            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
            if (t >= 14 && t < 92) {
               if (t % 10 == 0) {
                  this.play((SoundEvent)ModSounds.CLOCK_TICK.get(), 1.8F, 0.6F + (float)(t - 14) / 80.0F);
               }

               for (Entity e : this.enemiesWithin(12.0)) {
                  Vec3 in = flat(this.position().subtract(e.position()));
                  double d = in.length();
                  if (d > 1.5) {
                     e.setDeltaMovement(e.getDeltaMovement().add(in.scale(0.05 / d)));
                  }

                  e.hurtMarked = true;
                  if (e instanceof BeybladeEntity bx) {
                     float drain = 4.0F;
                     bx.damageSpin(drain, this);
                     this.healSpin(drain * 0.4F);
                  } else if (e instanceof LivingEntity l && t % 10 == 0) {
                     l.hurt(this.damageSources().indirectMagic(this, owner), 1.5F);
                     l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                  }
               }
            }

            if (t == 92) {
               this.play((SoundEvent)ModSounds.CLOCK_BELL.get(), 3.0F, 0.5F);
               this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 2.5F, 0.5F);
               this.level().broadcastEntityEvent(this, (byte)120);

               for (Entity e : this.enemiesWithin(12.0)) {
                  if (e instanceof BeybladeEntity bx) {
                     bx.damageSpin(170.0F * this.powerMul(), this);
                     bx.setDeltaMovement(bx.getDeltaMovement().add(0.0, 0.4, 0.0));
                     bx.shockwave(this, 0.8);
                  } else if (e instanceof LivingEntity l) {
                     l.hurt(this.damageSources().indirectMagic(this, owner), 12.0F);
                  }
               }
            }

            return true;
         default:
            return false;
      }
   }

   public static double minuteAngle(float t) {
      return (-Math.PI / 2) + (double)Math.max(0.0F, t - 6.0F) / 42.0 * Math.PI * 4.0;
   }

   public static double hourAngle(float t) {
      return (Math.PI / 2) + (double)Math.max(0.0F, t - 6.0F) / 42.0 * Math.PI * 2.0;
   }

   @Override
   public void handleEntityEvent(byte id) {
      switch (id) {
         case 120:
            this.tollTime = this.tickCount;
            this.clientScar(3.5F);
            break;
         case 121:
            this.rewindTime = this.tickCount;
            break;
         default:
            super.handleEntityEvent(id);
      }
   }
}
