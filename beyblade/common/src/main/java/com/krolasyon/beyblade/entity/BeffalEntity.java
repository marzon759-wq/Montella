package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import java.util.HashMap;
import java.util.Iterator;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class BeffalEntity extends BeybladeEntity {
   public static final int BF_PLUMAGE = 1;
   public static final int BF_GAZE = 2;
   public static final int BF_FEATHERS = 3;
   public static final int BF_MIRAGE = 4;
   public static final int BF_REQUIEM = 5;
   public static final int[] BF_DURATION = new int[]{0, 0, 50, 40, 110, 84};
   public static final int[] BF_COOLDOWN = new int[]{0, 620, 520, 460, 560, 1150};
   public static final int PLUMAGE_TICKS = 170;
   public static final int HYPNO_TICKS = 70;
   public static final int[] VOLLEY_AT = new int[]{8, 16, 24, 32};
   public static final int VOLLEY_N = 5;
   public static final double VOLLEY_RANGE = 11.0;
   public static final double MIRAGE_R = 1.7;
   public static final int MIRAGE_N = 3;
   public static final int REQ_FORM = 28;
   public static final int REQ_FIRE = 34;
   public static final int REQ_BLAST = 60;
   public static final int REQ_EYES = 12;
   public static final double REQ_RING = 4.2;
   public static final byte EV_GAZE = 111;
   public static final byte EV_VOLLEY = 112;
   public static final byte EV_REQUIEM = 113;
   public static final byte EV_REQUIEM_HIT = 114;
   public static final byte EV_MIRAGE_STRIKE = 115;
   private final Map<Integer, Integer> hypnotised = new HashMap<>();
   @Nullable
   private Vec3 requiemAt;
   public int gazeTime = -1000;
   public int requiemTime = -1000;
   public int requiemHitTime = -1000;
   public int mirageStrikeTime = -1000;
   public int gazeTargetId = -1;
   public final int[] volleyTime = new int[]{-1000, -1000, -1000, -1000};
   public final double[][] volleyDir = new double[4][3];
   public double reqX;
   public double reqY;
   public double reqZ;

   public BeffalEntity(EntityType<? extends BeffalEntity> type, Level level) {
      super(type, level);
   }

   @Override
   public int nativeMode() {
      return 3;
   }

   @Override
   public String kindId() {
      return "beffal";
   }

   @Override
   public String abilityKey(int id) {
      return "ability.librabeyblade.beffal." + id;
   }

   @Override
   public int duration(int id) {
      return BF_DURATION[id];
   }

   @Override
   public int cooldown(int id) {
      return BF_COOLDOWN[id];
   }

   @Override
   public int buffAbility() {
      return 1;
   }

   @Override
   public int buffTicks() {
      return 170;
   }

   @Override
   public boolean requiresTarget(int id) {
      return id == 2 || id == 3 || id == 5;
   }

   @Override
   public boolean buffReflects() {
      return true;
   }

   @Override
   public float incomingMul() {
      return this.isShielded() ? 0.55F : 0.9F;
   }

   @Override
   protected boolean ramsInto(int id) {
      return false;
   }

   @Override
   protected ItemStack defaultItem() {
      return new ItemStack((ItemLike)ModItems.BEFFAL.get());
   }

   @Override
   protected float dodgeChance() {
      if (this.getAbility() == 4) {
         return 0.55F;
      } else {
         return this.isBuffActive() ? 0.25F : 0.11F;
      }
   }

   @Override
   protected void onDodge(BeybladeEntity attacker) {
      if (this.getAbility() == 4) {
         attacker.damageSpin(45.0F * this.powerMul(), this);
         Vec3 away = flat(attacker.position().subtract(this.position()));
         away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
         attacker.setDeltaMovement(attacker.getDeltaMovement().add(away.scale(0.6 * attacker.knockMul())).add(0.0, 0.15, 0.0));
         this.level().broadcastEntityEvent(this, (byte)115);
         this.level().broadcastEntityEvent(attacker, (byte)61);
         this.play((SoundEvent)ModSounds.WING_BLADE.get(), 1.6F, 1.2F);
      }
   }

   @Override
   protected float clientSpinSpeed(int ab, float at, float speed) {
      return ab != 4 && ab != 5 ? speed : speed * 1.35F;
   }

   @Override
   protected void onBuffStart(@Nullable Player owner) {
      this.play((SoundEvent)ModSounds.FEATHER_WHIRL.get(), 1.8F, 0.8F);
      this.play((SoundEvent)ModSounds.EAGLE_SCREECH.get(), 1.4F, 1.5F);
      this.play((SoundEvent)ModSounds.SHIELD_UP.get(), 1.4F, 0.8F);
   }

   @Override
   protected void tickBuff() {
      this.tickShield();
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level().isClientSide() && !this.hypnotised.isEmpty()) {
         this.hypnotised.entrySet().removeIf(en -> {
            Entity e = this.level().getEntity(en.getKey());
            if (e != null && e.isAlive() && this.tickCount <= en.getValue()) {
               if (e instanceof BeybladeEntity b) {
                  b.stun(3);
                  if (this.tickCount % 6 == 0) {
                     double a = this.random.nextDouble() * Math.PI * 2.0;
                     b.setDeltaMovement(Math.cos(a) * 0.32, b.getDeltaMovement().y, Math.sin(a) * 0.32);
                  }

                  b.damageSpin(2.2F * this.powerMul(), this);
               } else if (e instanceof LivingEntity l && this.tickCount % 10 == 0) {
                  l.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, false));
                  l.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, false, false));
               }

               return false;
            } else {
               return true;
            }
         });
      }
   }

   @Override
   protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
      switch (ab) {
         case 2:
            if (this.target != null && this.target.isAlive()) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.4, 1.0, 0.4));
               if (t == 1) {
                  this.play((SoundEvent)ModSounds.JUDGMENT_START.get(), 1.6F, 1.4F);
                  this.play((SoundEvent)ModSounds.EAGLE_SCREECH.get(), 1.4F, 0.7F);
                  this.level().broadcastEntityEvent(this, (byte)111);
               }

               if (t == 18) {
                  this.play((SoundEvent)ModSounds.DRAIN.get(), 1.8F, 0.7F);
                  this.hypnotised.put(this.target.getId(), this.tickCount + 70);
                  if (this.target instanceof BeybladeEntity b) {
                     b.damageSpin(60.0F * this.powerMul(), this);
                  } else if (this.target instanceof LivingEntity l) {
                     l.hurt(this.damageSources().indirectMagic(this, owner), 5.0F);
                  }

                  for (Entity ex : this.enemiesWithin(4.5)) {
                     if (ex != this.target && ex instanceof BeybladeEntity b2) {
                        this.hypnotised.put(b2.getId(), this.tickCount + 35);
                     }
                  }
               }

               return true;
            } else {
               this.endAbilityEarly();
               return true;
            }
         case 3:
            if (this.target != null && this.target.isAlive()) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 1.0, 0.5));
               if (t == 1) {
                  this.play((SoundEvent)ModSounds.FEATHER_WHIRL.get(), 1.6F, 1.1F);
               }

               for (int k = 0; k < VOLLEY_AT.length; k++) {
                  if (t == VOLLEY_AT[k]) {
                     Vec3 to = flat(this.target.position().subtract(this.position()));
                     Vec3 dir = to.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : to.normalize();
                     this.level().broadcastEntityEvent(this, (byte)112);
                     this.play((SoundEvent)ModSounds.FEATHER_BURST.get(), 1.6F, 1.0F + (float)k * 0.08F);

                     for (Entity e : this.enemiesWithin(11.0)) {
                        Vec3 d = flat(e.position().subtract(this.position()));
                        double len = d.length();
                        if (!(len < 0.001) && !(d.scale(1.0 / len).dot(dir) < 0.82)) {
                           if (e instanceof BeybladeEntity) {
                              BeybladeEntity b = (BeybladeEntity)e;
                              if (!b.tryDodge(this)) {
                                 b.damageSpin((e == this.target ? 42.0F : 26.0F) * this.powerMul(), this);
                                 b.setDeltaMovement(b.getDeltaMovement().add(dir.scale(0.25)));
                                 this.level().broadcastEntityEvent(b, (byte)60);
                              }
                           } else if (e instanceof LivingEntity l) {
                              l.invulnerableTime = 0;
                              l.hurt(this.damageSources().indirectMagic(this, owner), 4.0F);
                           }
                        }
                     }
                  }
               }

               return true;
            } else {
               if (t > VOLLEY_AT[0]) {
                  return true;
               }

               this.endAbilityEarly();
               return true;
            }
         case 4:
            if (t == 1) {
               this.play((SoundEvent)ModSounds.STAR_CHIME.get(), 1.8F, 0.8F);
               this.play((SoundEvent)ModSounds.FEATHER_WHIRL.get(), 1.4F, 1.4F);
            }

            if (t % 20 == 0) {
               this.play((SoundEvent)ModSounds.STAR_CHIME.get(), 0.8F, 1.2F + (float)(t % 40) * 0.01F);
            }

            return false;
         case 5:
            if (t == 1) {
               this.play((SoundEvent)ModSounds.EAGLE_SCREECH.get(), 2.4F, 0.55F);
               this.play((SoundEvent)ModSounds.JUDGMENT_START.get(), 2.0F, 0.8F);
            }

            this.hover(1.3, 0.2);
            if (this.target != null && this.target.isAlive() && t < 60) {
               this.requiemAt = this.target.position();
               if (t == 2 || t % 6 == 0) {
                  this.level().broadcastEntityEvent(this, (byte)113);
               }

               if (t >= 28) {
                  this.target.setDeltaMovement(this.target.getDeltaMovement().multiply(0.2, 0.5, 0.2));
                  if (this.target instanceof BeybladeEntity b) {
                     b.stun(4);
                  }
               }
            }

            if (t >= 34 && t < 60 && (t - 34) % 2 == 0) {
               this.play((SoundEvent)ModSounds.WING_BLADE.get(), 1.2F, 1.4F + (float)(t - 34) * 0.02F);
               if (this.target instanceof BeybladeEntity b) {
                  b.damageSpin(9.0F * this.powerMul(), this);
               }
            }

            if (t == 60 && this.requiemAt != null) {
               this.play((SoundEvent)ModSounds.JUDGMENT_STRIKE.get(), 3.0F, 0.7F);
               this.play((SoundEvent)ModSounds.METEOR_IMPACT.get(), 2.6F, 0.9F);
               this.level().broadcastEntityEvent(this, (byte)114);

               for (Entity e : this.level().getEntities(this, new AABB(this.requiemAt, this.requiemAt).inflate(6.0, 4.0, 6.0))) {
                  boolean var10000;
                  if (e instanceof BeybladeEntity b) {
                     var10000 = this.isEnemy(b);
                  } else {
                     label178: {
                        if (e instanceof LivingEntity l && this.isHostileLiving(l)) {
                           var10000 = true;
                           break label178;
                        }

                        var10000 = false;
                     }
                  }

                  boolean enemy = var10000;
                  if (enemy || e == this.target) {
                     Vec3 out = flat(e.position().subtract(this.requiemAt));
                     double d = out.length();
                     out = d < 0.001 ? new Vec3(1.0, 0.0, 0.0) : out.scale(1.0 / d);
                     boolean main = e == this.target || d < 1.5;
                     if (e instanceof BeybladeEntity b) {
                        b.damageSpin((main ? 290.0F : 100.0F) * this.powerMul(), this);
                        b.setDeltaMovement(b.getDeltaMovement().add(out.scale(main ? 0.5 : 0.9)).add(0.0, main ? 0.45 : 0.3, 0.0));
                        if (main) {
                           b.shockwave(this, 1.0);
                        }

                        this.hypnotised.put(b.getId(), this.tickCount + 30);
                     } else if (e instanceof LivingEntity l) {
                        l.hurt(this.damageSources().indirectMagic(this, owner), main ? 17.0F : 8.0F);
                        l.knockback(1.3, -out.x, -out.z);
                     }
                  }
               }
            }

            if (t > 66) {
               this.setNoGravity(false);
            }

            return true;
         default:
            return false;
      }
   }

   @Override
   public void handleEntityEvent(byte id) {
      switch (id) {
         case 111:
            this.gazeTime = this.tickCount;
            this.gazeTargetId = this.getTargetId();
            break;
         case 112:
            int slot = 0;

            for (int i = 1; i < this.volleyTime.length; i++) {
               if (this.volleyTime[i] < this.volleyTime[slot]) {
                  slot = i;
               }
            }

            this.volleyTime[slot] = this.tickCount;
            Entity tx = this.level().getEntity(this.getTargetId());
            double dx = 1.0;
            double dz = 0.0;
            if (tx != null) {
               dx = tx.getX() - this.getX();
               dz = tx.getZ() - this.getZ();
               double l = Math.sqrt(dx * dx + dz * dz);
               if (l > 0.001) {
                  dx /= l;
                  dz /= l;
               } else {
                  dx = 1.0;
                  dz = 0.0;
               }
            }

            this.volleyDir[slot][0] = dx;
            this.volleyDir[slot][2] = dz;
            break;
         case 113:
            if (this.requiemTime < this.clientAbilityStart) {
               this.requiemTime = this.tickCount;
            }

            Entity tx2 = this.level().getEntity(this.getTargetId());
            if (tx2 != null) {
               this.reqX = tx2.getX();
               this.reqY = tx2.getY();
               this.reqZ = tx2.getZ();
            }
            break;
         case 114:
            this.requiemHitTime = this.tickCount;
            Iterator t = this.level().getEntitiesOfClass(StadiumEntity.class, this.getBoundingBox().inflate(18.0, 6.0, 18.0)).iterator();
            if (t.hasNext()) {
               StadiumEntity st = (StadiumEntity)t.next();
               double dx2 = this.reqX - st.getX();
               double dz2 = this.reqZ - st.getZ();
               if (dx2 * dx2 + dz2 * dz2 < 110.25) {
                  st.addScar(this.reqX, this.reqZ, 4.0F);
               }
            }
            break;
         case 115:
            this.mirageStrikeTime = this.tickCount;
            break;
         default:
            super.handleEntityEvent(id);
      }
   }
}
