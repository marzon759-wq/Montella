package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.compat.V;

import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
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
import org.joml.Vector3f;

public class LDragoEntity extends BeybladeEntity {
   public static final int DR_AWAKEN = 1;
   public static final int DR_VORTEX = 2;
   public static final int DR_METEOR = 3;
   public static final int DR_CLAW = 4;
   public static final int DR_ROAR = 5;
   public static final int[] DR_DURATION = new int[]{0, 0, 100, 130, 54, 84};
   public static final int[] DR_COOLDOWN = new int[]{0, 600, 500, 700, 400, 900};
   public static final int AWAKEN_TICKS = 200;
   @Nullable
   private BeybladeEntity grabbed;
   private int grabStart;
   public int grabTime = -1000;
   public static final double VORTEX_RADIUS = 7.5;
   public static final int M_RISE = 8;
   public static final int M_HANG = 30;
   public static final int M_DIVE = 44;
   public static final double M_HEIGHT = 16.0;
   public static final double M_RADIUS = 6.5;
   public static final int CLAW_FIRST = 4;
   public static final int CLAW_STEP = 9;
   public static final int CLAW_PASSES = 5;
   public static final int CLAW_UPPER = 48;
   public static final int R_BREATH = 20;
   public static final int R_END = 62;
   public static final double R_LENGTH = 13.0;
   public static final double R_HALF_ANGLE = Math.toRadians(26.0);
   public static final byte EV_CLAW = 70;
   public static final byte EV_AWAKEN = 71;
   public static final byte EV_UPPER = 72;
   public static final byte EV_GRAB_UP = 73;
   public static final int GRAB_UP = 16;
   private static final DustParticleOptions CRIMSON = new DustParticleOptions(new Vector3f(0.85F, 0.03F, 0.12F), 1.6F);
   private static final DustParticleOptions EMBER = new DustParticleOptions(new Vector3f(1.0F, 0.45F, 0.05F), 1.2F);
   private static final DustParticleOptions SHADOW = new DustParticleOptions(new Vector3f(0.12F, 0.0F, 0.04F), 2.0F);
   public int awakenTime = -1000;
   public final int[] clawTimes = new int[]{-1000, -1000, -1000, -1000, -1000, -1000};
   public final double[][] clawPos = new double[6][3];
   public final float[] clawRot = new float[6];
   private int clawIndex;
   private static final EntityDataAccessor<Float> DATA_RESERVE = SynchedEntityData.defineId(LDragoEntity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> DATA_REVIVED = SynchedEntityData.defineId(LDragoEntity.class, EntityDataSerializers.BOOLEAN);
   public static final float REVIVE_MIN = 60.0F;
   public static final byte EV_REVIVE = 74;
   public int reviveTime = -1000;

   @Override
   protected void endAbility() {
      if (this.grabbed != null) {
         this.grabbed.setNoGravity(false);
         this.grabbed = null;
      }

      super.endAbility();
   }

   @Override
   protected void defineData(DataDef d) {
      super.defineData(d);
      d.define(DATA_RESERVE, 0.0F);
      d.define(DATA_REVIVED, false);
   }

   public void debugCharge(float amount) {
      this.entityData.set(DATA_RESERVE, this.reserve() + amount);
   }

   public float reserve() {
      return (Float)this.entityData.get(DATA_RESERVE);
   }

   public boolean revived() {
      return (Boolean)this.entityData.get(DATA_REVIVED);
   }

   @Override
   protected void onClash(BeybladeEntity other, float dealt, float taken) {
      if (!this.revived()) {
         this.entityData.set(DATA_RESERVE, this.reserve() + dealt * 0.5F + taken * 0.5F);
      }
   }

   @Override
   protected boolean lastStand() {
      if (!this.revived() && !(this.reserve() < 60.0F)) {
         this.entityData.set(DATA_REVIVED, true);
         this.entityData.set(DATA_RESERVE, 0.0F);
         this.healSpin(this.getMaxSpin());
         this.stun(0);
         this.play((SoundEvent)ModSounds.DRAGON_ROAR.get(), 2.5F, 0.8F);
         this.play((SoundEvent)ModSounds.METEOR_IMPACT.get(), 2.0F, 1.2F);
         this.level().broadcastEntityEvent(this, (byte)74);

         for (Entity e : this.enemiesWithin(5.0)) {
            Vec3 out = flat(e.position().subtract(this.position()));
            out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
            e.setDeltaMovement(e.getDeltaMovement().add(out.scale(0.8)).add(0.0, 0.2, 0.0));
            e.hurtMarked = true;
         }

         Player owner = this.getOwner();
         if (owner != null) {
            com.krolasyon.beyblade.Msg.say(owner, 
               Component.translatable("msg.librabeyblade.ldrago_revive").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}), false
            );
         }

         return true;
      } else {
         return false;
      }
   }

   @Override
   public Component passiveLine() {
      return this.revived()
         ? Component.translatable("hud.librabeyblade.ldrago_revived")
         : Component.translatable(
            this.reserve() >= 60.0F ? "hud.librabeyblade.ldrago_ready" : "hud.librabeyblade.ldrago_reserve", new Object[]{Mth.floor(this.reserve())}
         );
   }

   public LDragoEntity(EntityType<? extends LDragoEntity> type, Level level) {
      super(type, level);
   }

   @Override
   public int spinDir() {
      return -1;
   }

   @Override
   protected void onDodge(BeybladeEntity attacker) {
      float st = Math.min(40.0F, attacker.getSpin());
      attacker.damageSpin(st, this);
      this.healSpin(st);
   }

   @Override
   public int nativeMode() {
      return 0;
   }

   @Override
   public String kindId() {
      return "ldrago";
   }

   @Override
   public String abilityKey(int id) {
      return "ability.librabeyblade.ldrago." + id;
   }

   @Override
   public int duration(int id) {
      return DR_DURATION[id];
   }

   @Override
   public int cooldown(int id) {
      return DR_COOLDOWN[id];
   }

   @Override
   public int buffAbility() {
      return 1;
   }

   @Override
   public int buffTicks() {
      return 200;
   }

   @Override
   public boolean requiresTarget(int id) {
      return id == 4 || id == 5;
   }

   @Override
   public boolean buffReflects() {
      return false;
   }

   @Override
   public float buffAttackMul() {
      return this.isShielded() ? 1.8F : 1.0F;
   }

   @Override
   public double buffSpeedMul() {
      return this.isShielded() ? 1.45 : 1.0;
   }

   @Override
   protected ItemStack defaultItem() {
      return new ItemStack((ItemLike)ModItems.LDRAGO.get());
   }

   @Override
   protected float clientSpinSpeed(int ab, float at, float speed) {
      if (ab == 2) {
         return 100.0F;
      } else if (ab == 3 && at >= 8.0F) {
         return 110.0F;
      } else {
         return this.isShielded() ? speed * 1.2F : speed;
      }
   }

   @Override
   protected void onBuffStart(@Nullable Player owner) {
      this.play((SoundEvent)ModSounds.DRAGON_ROAR.get(), 2.0F, 1.0F);
      this.level().broadcastEntityEvent(this, (byte)71);
      ServerLevel sl = (ServerLevel)this.level();
      Px.send(sl, ParticleTypes.FLAME, this.getX(), this.getY() + 0.4, this.getZ(), 40, 0.4, 0.3, 0.4, 0.25);
      Px.send(sl, CRIMSON, this.getX(), this.getY() + 0.6, this.getZ(), 30, 0.8, 0.6, 0.8, 0.0);

      for (Entity e : this.enemiesWithin(4.5)) {
         Vec3 out = flat(e.position().subtract(this.position()));
         out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
         if (e instanceof BeybladeEntity b) {
            b.damageSpin(40.0F, this);
            b.setDeltaMovement(b.getDeltaMovement().add(out.scale(0.65)).add(0.0, 0.15, 0.0));
         } else if (e instanceof LivingEntity l) {
            l.hurt(this.damageSources().indirectMagic(this, owner), 4.0F);
            l.knockback(1.0, -out.x, -out.z);
         }
      }
   }

   @Override
   protected void tickBuff() {
      if (this.tickCount % 4 == 0) {
         Px.send((ServerLevel)this.level(), ParticleTypes.FLAME, this.getX(), this.getY() + 0.3, this.getZ(), 2, 0.3, 0.1, 0.3, 0.02);
      }
   }

   @Override
   protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
      ServerLevel sl = (ServerLevel)this.level();
      switch (ab) {
         case 2:
            if (t == 1) {
               this.play((SoundEvent)ModSounds.VORTEX.get(), 1.8F, 1.0F);
            }

            if (t % 20 == 10) {
               this.play((SoundEvent)ModSounds.DRAIN.get(), 1.2F, 0.7F);
            }

            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));

            for (Entity e : this.enemiesWithin(7.5)) {
               Vec3 rel = flat(e.position().subtract(this.position()));
               double d = rel.length();
               Vec3 dir = d < 0.001 ? new Vec3(1.0, 0.0, 0.0) : rel.scale(1.0 / d);
               Vec3 swirl = new Vec3(dir.z, 0.0, -dir.x).scale(-0.26).add(dir.scale(d > 1.6 ? -0.07 : 0.05));
               Vec3 v = e.getDeltaMovement();
               e.setDeltaMovement(new Vec3(v.x * 0.6 + swirl.x, v.y, v.z * 0.6 + swirl.z));
               e.hurtMarked = true;
               if (e instanceof BeybladeEntity b) {
                  float steal = Math.min(3.5F, b.getSpin());
                  b.damageSpin(steal, this);
                  this.healSpin(steal * 0.7F);
               } else if (e instanceof LivingEntity) {
                  LivingEntity l = (LivingEntity)e;
                  if (t % 10 == 0) {
                     l.hurt(this.damageSources().indirectMagic(this, owner), 2.0F);
                     l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2));
                  }
               }
            }

            return true;
         case 3:
            if (t < 8) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 1.0, 0.5));
               return true;
            } else {
               if (t == 8) {
                  this.play((SoundEvent)ModSounds.DRAGON_RISE.get(), 2.0F, 1.0F);
                  Px.send(sl, ParticleTypes.LAVA, this.getX(), this.getY() + 0.2, this.getZ(), 12, 0.5, 0.1, 0.5, 0.0);
               }

               if (t < 44) {
                  if (t == 30) {
                     this.play((SoundEvent)ModSounds.METEOR_CHARGE.get(), 2.0F, 1.0F);
                  }

                  this.hover(16.0, t < 30 ? 0.22 : 0.3);
                  return true;
               } else {
                  if (t == 44) {
                     Vec3 aim = this.target != null ? this.target.position() : new Vec3(this.getX(), this.abilityBaseY, this.getZ());
                     Vec3 d = aim.subtract(this.position());
                     if (d.lengthSqr() < 1.0E-4) {
                        d = new Vec3(0.0, -1.0, 0.0);
                     }

                     this.setNoGravity(true);
                     this.setDeltaMovement(d.normalize().scale(2.2));
                     this.diving = true;
                     this.play((SoundEvent)ModSounds.DRAGON_ROAR.get(), 1.6F, 1.3F);
                  }

                  if (this.diving && this.target != null && this.target.isAlive()) {
                     Vec3 d = this.target.position().add(0.0, 0.3, 0.0).subtract(this.position());
                     if (d.lengthSqr() > 1.0E-4) {
                        this.setDeltaMovement(d.normalize().scale(2.2));
                     }
                  }

                  if (this.diving && this.target instanceof BeybladeEntity tb && this.target.distanceToSqr(this) < 3.24) {
                     this.diving = false;
                     this.grabbed = tb;
                     this.grabStart = t;
                     tb.stun(40);
                     tb.damageSpin(60.0F * this.powerMul(), this);
                     this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 2.5F, 0.6F);
                     this.play((SoundEvent)ModSounds.DRAGON_ROAR.get(), 2.0F, 0.9F);
                     this.level().broadcastEntityEvent(this, (byte)73);
                     this.level().broadcastEntityEvent(tb, (byte)61);
                  }

                  if (this.grabbed != null) {
                     int g = t - this.grabStart;
                     if (!this.grabbed.isAlive()) {
                        this.grabbed = null;
                        this.setNoGravity(false);
                        return true;
                     } else {
                        if (g < 16) {
                           this.setNoGravity(true);
                           this.setDeltaMovement(0.0, 0.55 * (1.0 - (double)g / 16.0) + 0.1, 0.0);
                        } else if (g < 20) {
                           this.setDeltaMovement(0.0, 0.0, 0.0);
                        } else {
                           this.setDeltaMovement(0.0, -1.9, 0.0);
                           if (g == 20) {
                              this.play((SoundEvent)ModSounds.METEOR_CHARGE.get(), 2.0F, 1.4F);
                           }
                        }

                        this.grabbed.setNoGravity(true);
                        this.grabbed.setPos(this.getX(), this.getY() - 0.75, this.getZ());
                        this.grabbed.setDeltaMovement(this.getDeltaMovement());
                        this.grabbed.hurtMarked = true;
                        boolean landed = g > 20 && (this.grabbed.onGround() || this.onGround() || this.verticalCollision || g > 38);
                        if (landed) {
                           this.grabbed.setNoGravity(false);
                           this.grabbed.damageSpin(280.0F * this.powerMul(), this);
                           this.grabbed.shockwave(this, 1.0);
                           this.grabbed = null;
                           this.setNoGravity(false);
                           this.meteorImpact(owner);
                           this.abilityTicks = Math.max(this.abilityTicks, DR_DURATION[3] - 18);
                        }

                        return true;
                     }
                  } else {
                     if (this.diving && (this.onGround() || this.verticalCollision || this.horizontalCollision || t > 64)) {
                        this.diving = false;
                        this.setNoGravity(false);
                        this.meteorImpact(owner);
                        this.abilityTicks = Math.max(this.abilityTicks, DR_DURATION[3] - 18);
                     }

                     return true;
                  }
               }
            }
         case 4:
            if (this.target != null && this.target.isAlive()) {
               this.setNoGravity(t < 50);
               int rel = t - 4;
               if (rel >= 0 && rel < 45) {
                  int k = rel / 9;
                  int s = rel % 9;
                  if (s == 0) {
                     Vec3 tp = this.target.position();
                     Vec3 through = flat(tp.subtract(this.position()));
                     through = through.lengthSqr() < 0.001 ? new Vec3(1.0, 0.0, 0.0) : through.normalize();
                     Vec3 side = new Vec3(-through.z, 0.0, through.x).scale(k % 2 == 0 ? 0.75 : -0.75);
                     Vec3 to = tp.add(through.scale(2.2)).add(side).add(0.0, 0.15, 0.0);
                     this.setDeltaMovement(to.subtract(this.position()).scale(0.3333333333333333));
                     this.play((SoundEvent)ModSounds.CLAW_SLASH.get(), 1.2F, 0.9F + (float)k * 0.08F);
                  } else if (s == 2) {
                     this.clawHit(owner, 45.0F, false);
                  } else if (s >= 3) {
                     this.setDeltaMovement(this.getDeltaMovement().scale(0.35));
                  }
               } else if (t == 48) {
                  Vec3 d = this.target.position().subtract(this.position());
                  this.setDeltaMovement(d.scale(0.5).add(0.0, 0.4, 0.0));
                  this.play((SoundEvent)ModSounds.CLAW_SLASH.get(), 1.5F, 0.7F);
               } else if (t == 49) {
                  this.clawHit(owner, 70.0F, true);
               }

               return true;
            }

            this.endAbilityEarly();
            return true;
         case 5:
            if (this.target != null && this.target.isAlive()) {
               if (t == 1) {
                  this.play((SoundEvent)ModSounds.DRAGON_ROAR.get(), 2.2F, 0.85F);
               }

               if (t < 62) {
                  this.hover(2.2, 0.2);
               } else {
                  this.setNoGravity(false);
               }

               if (t == 20) {
                  this.play((SoundEvent)ModSounds.FIRE_BREATH.get(), 2.0F, 1.0F);
               }

               if (t >= 20 && t < 62) {
                  Vec3 from = this.position().add(0.0, 0.4, 0.0);
                  Vec3 dir = breathDir((float)t, from, this.target.position().add(0.0, (double)this.target.getBbHeight() * 0.5, 0.0));

                  for (Entity ex : this.enemiesWithin(14.0)) {
                     Vec3 c = ex.position().add(0.0, (double)ex.getBbHeight() * 0.5, 0.0).subtract(from);
                     double along = c.dot(dir);
                     if (!(along < 0.0) && !(along > 13.0)) {
                        double ang = Math.acos(Mth.clamp(along / Math.max(1.0E-4, c.length()), -1.0, 1.0));
                        if (!(ang > R_HALF_ANGLE)) {
                           if (ex instanceof BeybladeEntity b) {
                              b.damageSpin(7.0F, this);
                              b.setDeltaMovement(b.getDeltaMovement().add(flat(dir).scale(0.05)));
                           } else if (ex instanceof LivingEntity) {
                              LivingEntity l = (LivingEntity)ex;
                              if (t % 5 == 0) {
                                 l.hurt(this.damageSources().indirectMagic(this, owner), 2.0F);
                                 V.ignite(l, 5);
                              }
                           }
                        }
                     }
                  }

                  if (t % 3 == 0) {
                     Vec3 p = from.add(dir.scale(9.1));
                     Px.send(sl, ParticleTypes.LAVA, p.x, p.y, p.z, 2, 1.0, 0.6, 1.0, 0.0);
                  }
               }

               return true;
            }

            if (t < 20) {
               this.endAbilityEarly();
            }

            return true;
         default:
            return false;
      }
   }

   public static Vec3 breathDir(float t, Vec3 from, Vec3 to) {
      Vec3 d = to.subtract(from);
      if (d.lengthSqr() < 1.0E-4) {
         d = new Vec3(1.0, 0.0, 0.0);
      }

      d = d.normalize();
      double sweep = Math.toRadians(15.0) * Math.sin((double)(t - 20.0F) * 0.16);
      double c = Math.cos(sweep);
      double s = Math.sin(sweep);
      return new Vec3(d.x * c - d.z * s, d.y, d.x * s + d.z * c).normalize();
   }

   private void clawHit(@Nullable Player owner, float spin, boolean upper) {
      if (this.target != null && !(this.target.distanceToSqr(this) > 10.240000000000002)) {
         Vec3 side = flat(this.target.position().subtract(this.position()));
         side = side.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
         if (this.target instanceof BeybladeEntity b) {
            b.damageSpin(spin * (this.isShielded() ? 1.4F : 1.0F), this);
            b.setDeltaMovement(b.getDeltaMovement().add(side.scale(0.35)).add(0.0, upper ? 0.75 : 0.08, 0.0));
         } else if (this.target instanceof LivingEntity l) {
            l.hurt(this.damageSources().indirectMagic(this, owner), upper ? 8.0F : 5.0F);
            l.knockback(0.6, -side.x, -side.z);
            if (upper) {
               l.setDeltaMovement(l.getDeltaMovement().add(0.0, 0.6, 0.0));
            }
         }

         ServerLevel sl = (ServerLevel)this.level();
         Px.send(sl, ParticleTypes.SWEEP_ATTACK, this.target.getX(), this.target.getY() + 0.5, this.target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
         Px.send(sl, ParticleTypes.CRIT, this.target.getX(), this.target.getY() + 0.5, this.target.getZ(), 12, 0.2, 0.2, 0.2, 0.5);
         Px.send(sl, CRIMSON, this.target.getX(), this.target.getY() + 0.5, this.target.getZ(), 10, 0.3, 0.3, 0.3, 0.0);
         this.level().broadcastEntityEvent(this, (byte)(upper ? 72 : 70));
      }
   }

   private void meteorImpact(@Nullable Player owner) {
      ServerLevel sl = (ServerLevel)this.level();
      this.play((SoundEvent)ModSounds.METEOR_IMPACT.get(), 3.0F, 1.0F);
      this.level().broadcastEntityEvent(this, (byte)65);
      Px.send(sl, ParticleTypes.LAVA, this.getX(), this.getY() + 0.3, this.getZ(), 30, 2.0, 0.3, 2.0, 0.0);
      Px.send(sl, CRIMSON, this.getX(), this.getY() + 0.5, this.getZ(), 40, 2.5, 0.5, 2.5, 0.0);

      for (int i = 0; i < 60; i++) {
         double a = (double)i / 60.0 * Math.PI * 2.0;
         Px.send(
            sl,
            ParticleTypes.FLAME,
            this.getX() + Math.cos(a),
            this.getY() + 0.15,
            this.getZ() + Math.sin(a),
            0,
            Math.cos(a),
            0.08,
            Math.sin(a),
            0.6
         );
      }

      for (Entity e : this.enemiesWithin(6.5)) {
         Vec3 out = flat(e.position().subtract(this.position()));
         out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
         double falloff = 1.0 - Math.min(1.0, (double)e.distanceTo(this) / 6.5) * 0.5;
         if (e instanceof BeybladeEntity b) {
            b.damageSpin((float)(220.0 * falloff), this);
            b.setDeltaMovement(b.getDeltaMovement().add(out.scale(1.0 * falloff)).add(0.0, 0.45, 0.0));
         } else if (e instanceof LivingEntity l) {
            l.hurt(this.damageSources().indirectMagic(this, owner), (float)(14.0 * falloff));
            V.ignite(l, 6);
            l.knockback(1.4 * falloff, -out.x, -out.z);
         }
      }
   }

   @Override
   public void handleEntityEvent(byte id) {
      switch (id) {
         case 70:
         case 72:
            Entity t = this.level().getEntity(this.getTargetId());
            int i = this.clawIndex++ % this.clawTimes.length;
            this.clawTimes[i] = this.tickCount;
            Entity at = (Entity)(t != null ? t : this);
            this.clawPos[i][0] = at.getX();
            this.clawPos[i][1] = at.getY() + 0.45 + (id == 72 ? 0.4 : 0.0);
            this.clawPos[i][2] = at.getZ();
            this.clawRot[i] = id == 72 ? 90.0F : this.random.nextFloat() * 360.0F;

            for (int k = 0; k < 12; k++) {
               Px.add(
                  this.level(),
                  EMBER,
                  at.getX(),
                  at.getY() + 0.5,
                  at.getZ(),
                  (this.random.nextDouble() - 0.5) * 0.6,
                  this.random.nextDouble() * 0.4,
                  (this.random.nextDouble() - 0.5) * 0.6
               );
            }
            break;
         case 71:
            this.awakenTime = this.tickCount;
            break;
         case 73:
            this.grabTime = this.tickCount;
            break;
         case 74:
            this.reviveTime = this.tickCount;
            break;
         default:
            super.handleEntityEvent(id);
      }
   }

   @Override
   protected void clientBuffParticles(Level lv) {
      double a = this.random.nextDouble() * Math.PI * 2.0;
      Px.add(lv, ParticleTypes.FLAME, this.getX() + Math.cos(a) * 0.45, this.getY() + 0.25, this.getZ() + Math.sin(a) * 0.45, 0.0, 0.05, 0.0);
      if (this.random.nextInt(2) == 0) {
         Px.add(lv, CRIMSON, this.getX() + Math.cos(a + 1.0) * 0.6, this.getY() + 0.5, this.getZ() + Math.sin(a + 1.0) * 0.6, 0.0, 0.04, 0.0);
      }
   }

   @Override
   protected void clientAbilityParticles(int ab, float at, Level lv) {
      switch (ab) {
         case 2:
            for (int i = 0; i < 4; i++) {
               double a = this.random.nextDouble() * Math.PI * 2.0;
               double r = 1.0 + this.random.nextDouble() * 6.5;
               double x = this.getX() + Math.cos(a) * r;
               double z = this.getZ() + Math.sin(a) * r;
               Px.add(
                  lv,
                  i % 2 == 0 ? SHADOW : CRIMSON,
                  x,
                  this.getY() + 0.1,
                  z,
                  -Math.sin(a) * -0.3 - Math.cos(a) * 0.1,
                  0.02,
                  Math.cos(a) * -0.3 - Math.sin(a) * 0.1
               );
            }

            if (this.tickCount % 2 == 0) {
               for (Entity e : this.enemiesWithin(7.5)) {
                  double f = this.random.nextDouble();
                  Px.add(
                     lv,
                     CRIMSON,
                     Mth.lerp(f, e.getX(), this.getX()),
                     Mth.lerp(f, e.getY() + 0.5, this.getY() + 0.4),
                     Mth.lerp(f, e.getZ(), this.getZ()),
                     (this.getX() - e.getX()) * 0.05,
                     0.02,
                     (this.getZ() - e.getZ()) * 0.05
                  );
               }
            }
            break;
         case 3:
            if (at >= 8.0F) {
               for (int i = 0; i < 4; i++) {
                  Px.add(
                     lv,
                     (ParticleOptions)(i % 2 == 0 ? ParticleTypes.FLAME : EMBER),
                     this.getX() + (this.random.nextDouble() - 0.5) * 0.6,
                     this.getY() + this.random.nextDouble() * 0.6,
                     this.getZ() + (this.random.nextDouble() - 0.5) * 0.6,
                     0.0,
                     at < 44.0F ? -0.15 : 0.1,
                     0.0
                  );
               }

               if (at >= 44.0F) {
                  Px.add(lv, ParticleTypes.LAVA, this.getX(), this.getY() + 0.4, this.getZ(), 0.0, 0.0, 0.0);
               }
            }
         case 4:
         default:
            break;
         case 5:
            Entity t = this.level().getEntity(this.getTargetId());
            if (t == null) {
               return;
            }

            if (at < 20.0F) {
               Px.add(
                  lv,
                  EMBER,
                  this.getX() + (this.random.nextDouble() - 0.5) * 2.0,
                  this.getY() + 0.4 + this.random.nextDouble(),
                  this.getZ() + (this.random.nextDouble() - 0.5) * 2.0,
                  0.0,
                  0.02,
                  0.0
               );
            } else if (at < 62.0F) {
               Vec3 from = this.position().add(0.0, 0.4, 0.0);
               Vec3 dir = breathDir(at, from, t.position().add(0.0, (double)t.getBbHeight() * 0.5, 0.0));

               for (int i = 0; i < 6; i++) {
                  double sp = 0.55 + this.random.nextDouble() * 0.5;
                  Vec3 j = new Vec3(this.random.nextGaussian(), this.random.nextGaussian(), this.random.nextGaussian()).scale(0.12);
                  Vec3 v = dir.add(j).scale(sp);
                  Px.add(
                     lv,
                     i % 3 == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME,
                     from.x + dir.x * 0.8,
                     from.y + dir.y * 0.8,
                     from.z + dir.z * 0.8,
                     v.x,
                     v.y,
                     v.z
                  );
               }
            }
      }
   }
}
