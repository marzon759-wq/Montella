package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class EagleEntity extends BeybladeEntity {
   public static final int EA_EYE = 1;
   public static final int EA_FEATHER = 2;
   public static final int EA_TALON = 3;
   public static final int EA_WING = 4;
   public static final int EA_SKY = 5;
   public static final int[] EA_DURATION = new int[]{0, 0, 60, 84, 70, 124};
   public static final int[] EA_COOLDOWN = new int[]{0, 600, 450, 650, 550, 1100};
   public static final int EYE_TICKS = 200;
   public static final int F_GATHER = 20;
   public static final int F_GAP = 12;
   public static final int F_BURSTS = 3;
   public static final int F_COUNT = 16;
   public static final int F_LIFE = 11;
   public static final float F_SPEED = 0.9F;
   public static final int T_SWOOP = 10;
   public static final int T_SWOOP_END = 26;
   public static final int T_CARRY = 32;
   public static final double T_LIFT = 7.0;
   public static final int W_OPEN = 8;
   public static final int W_CLOSE = 62;
   public static final double W_RADIUS = 3.3;
   public static final int S_DESCEND = 20;
   public static final int S_LANCES = 27;
   public static final int S_STEP = 3;
   public static final int S_FALL = 8;
   public static final int S_SCREECH = 104;
   public static final double S_RANGE = 14.0;
   public static final byte EV_DODGE = 90;
   public static final byte EV_GRAB = 91;
   public static final byte EV_SLAM = 92;
   public static final byte EV_SCREECH = 93;
   private static final DustParticleOptions VIOLET = new DustParticleOptions(new Vector3f(0.6F, 0.3F, 1.0F), 1.3F);
   private static final DustParticleOptions SILVER = new DustParticleOptions(new Vector3f(0.9F, 0.9F, 1.0F), 1.0F);
   private final Set<Integer>[] burstHits = new Set[]{new HashSet(), new HashSet(), new HashSet()};
   private final Map<Integer, Integer> wingHit = new HashMap<>();
   private int grabTick = -1;
   public int dodgeTime = -1000;
   public int grabTime = -1000;
   public int slamTime = -1000;
   public int screechTime = -1000;
   public double dodgeX;
   public double dodgeY;
   public double dodgeZ;

   public EagleEntity(EntityType<? extends EagleEntity> type, Level level) {
      super(type, level);
   }

   @Override
   public int nativeMode() {
      return 2;
   }

   @Override
   public String kindId() {
      return "eagle";
   }

   @Override
   public String abilityKey(int id) {
      return "ability.librabeyblade.eagle." + id;
   }

   @Override
   public int duration(int id) {
      return EA_DURATION[id];
   }

   @Override
   public int cooldown(int id) {
      return EA_COOLDOWN[id];
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
      return id == 3;
   }

   @Override
   public boolean buffReflects() {
      return false;
   }

   @Override
   public double buffSpeedMul() {
      return this.isShielded() ? 1.5 : 1.12;
   }

   @Override
   public float incomingMul() {
      return 0.9F;
   }

   @Override
   protected ItemStack defaultItem() {
      return new ItemStack((ItemLike)ModItems.EAGLE.get());
   }

   @Override
   protected float clientSpinSpeed(int ab, float at, float speed) {
      if (ab == 4) {
         return 105.0F;
      } else {
         return ab == 2 ? 90.0F : speed;
      }
   }

   @Override
   protected void onBuffStart(@Nullable Player owner) {
      this.play((SoundEvent)ModSounds.EAGLE_SCREECH.get(), 1.6F, 1.1F);
      Px.send((ServerLevel)this.level(), VIOLET, this.getX(), this.getY() + 0.6, this.getZ(), 30, 0.6, 0.4, 0.6, 0.0);
   }

   @Override
   public boolean tryDodge(BeybladeEntity attacker) {
      if (!this.isShielded()) {
         return super.tryDodge(attacker);
      } else if (this.random.nextFloat() >= 0.5F) {
         return false;
      } else {
         Vec3 away = flat(this.position().subtract(attacker.position()));
         away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
         Vec3 side = new Vec3(-away.z, 0.0, away.x).scale(this.random.nextBoolean() ? 1.3 : -1.3).add(away.scale(0.5));
         this.level().broadcastEntityEvent(this, (byte)90);
         if (this.level().noCollision(this, this.getBoundingBox().move(side))) {
            this.setPos(this.getX() + side.x, this.getY(), this.getZ() + side.z);
         }

         this.setDeltaMovement(this.getDeltaMovement().add(side.scale(0.15)));
         this.play((SoundEvent)ModSounds.FEATHER_BURST.get(), 0.7F, 1.5F);
         return true;
      }
   }

   @Override
   protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
      ServerLevel sl = (ServerLevel)this.level();
      switch (ab) {
         case 2:
            if (t == 1) {
               this.play((SoundEvent)ModSounds.FEATHER_WHIRL.get(), 1.6F, 1.0F);

               for (Set<Integer> s : this.burstHits) {
                  s.clear();
               }
            }

            this.hover(0.6, 0.25);

            for (int i = 0; i < 3; i++) {
               int age = t - (20 + i * 12);
               if (age == 0) {
                  this.play((SoundEvent)ModSounds.FEATHER_BURST.get(), 1.6F, 0.9F + (float)i * 0.15F);
               }

               if (age >= 0 && age < 11) {
                  double front = 0.8 + (double)((float)age * 0.9F);

                  for (Entity exx : this.enemiesWithin(front + 0.6)) {
                     double d = Math.sqrt(flat(exx.position().subtract(this.position())).lengthSqr());
                     if (!(d < front - 1.2) && this.burstHits[i].add(exx.getId())) {
                        Vec3 out = flat(exx.position().subtract(this.position()));
                        out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
                        if (exx instanceof BeybladeEntity b) {
                           b.damageSpin(32.0F, this);
                           b.setDeltaMovement(b.getDeltaMovement().add(out.scale(0.35)));
                        } else if (exx instanceof LivingEntity l) {
                           l.hurt(this.damageSources().indirectMagic(this, owner), 3.0F);
                           l.knockback(0.5, -out.x, -out.z);
                        }

                        Px.send(sl, SILVER, exx.getX(), exx.getY() + 0.5, exx.getZ(), 6, 0.2, 0.2, 0.2, 0.0);
                     }
                  }
               }
            }

            return true;
         case 3:
            if (this.target != null && this.target.isAlive()) {
               if (t == 1) {
                  this.grabTick = -1;
                  this.play((SoundEvent)ModSounds.EAGLE_SCREECH.get(), 1.8F, 1.0F);
               }

               if (t < 10) {
                  this.hover(4.0, 0.3);
                  return true;
               }

               if (this.grabTick < 0) {
                  if (t == 10) {
                     this.play((SoundEvent)ModSounds.SKY_DIVE.get(), 1.6F, 1.0F);
                  }

                  this.setNoGravity(true);
                  Vec3 d = this.target.position().add(0.0, 0.5, 0.0).subtract(this.position());
                  double len = d.length();
                  this.setDeltaMovement(d.scale(Math.min(1.2, Math.max(0.0, len - 0.9)) / Math.max(0.001, len)));
                  if (len < 2.2) {
                     this.setDeltaMovement(Vec3.ZERO);
                     this.grabTick = t;
                     this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 1.4F, 1.2F);
                     this.level().broadcastEntityEvent(this, (byte)91);
                     AnimeMoves.slam(this, 3.5, com.krolasyon.beyblade.world.WorldImpact.Style.EARTH);
                     if (this.target instanceof BeybladeEntity b) {
                        b.damageSpin(40.0F, this);
                     } else if (this.target instanceof LivingEntity l) {
                        l.hurt(this.damageSources().indirectMagic(this, owner), 5.0F);
                     }
                  } else if (t > 26) {
                     this.endAbilityEarly();
                  }

                  return true;
               }

               int carry = t - this.grabTick;
               if (carry < 32) {
                  this.hover(7.0 * Math.min(1.0, (double)carry / 18.0) + 0.5, 0.25);
                  Vec3 hold = this.position().add(0.0, -0.75, 0.0);
                  this.target.setDeltaMovement(hold.subtract(this.target.position()).scale(0.6));
                  this.target.hurtMarked = true;
                  this.target.fallDistance = 0.0F;
                  if (this.target instanceof BeybladeEntity b) {
                     b.damageSpin(2.5F, this);
                  }

                  if (carry % 4 == 0) {
                     Px.send(sl, VIOLET, this.getX(), this.getY() - 0.3, this.getZ(), 4, 0.3, 0.2, 0.3, 0.0);
                  }

                  return true;
               }

               if (carry == 32) {
                  this.play((SoundEvent)ModSounds.SKY_DIVE.get(), 1.4F, 1.4F);
                  this.target.setDeltaMovement(new Vec3(0.0, -2.2, 0.0));
                  this.target.hurtMarked = true;
                  if (this.target instanceof BeybladeEntity b) {
                     b.damageSpin(110.0F, this);
                     b.stun(25);
                  } else if (this.target instanceof LivingEntity l) {
                     l.hurt(this.damageSources().indirectMagic(this, owner), 9.0F);
                  }

                  this.level().broadcastEntityEvent(this, (byte)92);
                  this.abilityTicks = Math.max(this.abilityTicks, EA_DURATION[3] - 14);
               }

               this.setNoGravity(false);
               return true;
            }

            this.grabTick = -1;
            this.endAbilityEarly();
            return true;
         case 4:
            if (t == 1) {
               this.wingHit.clear();
               this.play((SoundEvent)ModSounds.WING_BLADE.get(), 1.8F, 1.0F);
            }

            if (t == 38) {
               this.play((SoundEvent)ModSounds.WING_BLADE.get(), 1.4F, 1.15F);
            }

            if (this.target != null && t > 8 && t < 62) {
               Vec3 d = flat(this.target.position().subtract(this.position()));
               if (d.lengthSqr() > 1.0) {
                  this.setDeltaMovement(this.getDeltaMovement().scale(0.6).add(d.normalize().scale(0.18)));
               }
            } else {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, 1.0, 0.7));
            }

            if (t >= 8 && t < 62) {
               for (Entity ex : this.enemiesWithin(3.8)) {
                  Integer last = this.wingHit.get(ex.getId());
                  if (last == null || t - last >= 8) {
                     this.wingHit.put(ex.getId(), t);
                     Vec3 relx = flat(ex.position().subtract(this.position()));
                     Vec3 out = relx.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : relx.normalize();
                     Vec3 tang = new Vec3(out.z, 0.0, -out.x);
                     if (ex instanceof BeybladeEntity b) {
                        b.damageSpin(42.0F, this);
                        b.setDeltaMovement(b.getDeltaMovement().add(tang.scale(0.55)).add(out.scale(0.35)).add(0.0, 0.1, 0.0));
                     } else if (ex instanceof LivingEntity l) {
                        l.hurt(this.damageSources().indirectMagic(this, owner), 6.0F);
                        l.knockback(0.9, -out.x, -out.z);
                     }

                     Px.send(sl, ParticleTypes.SWEEP_ATTACK, ex.getX(), ex.getY() + 0.5, ex.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                     Px.send(sl, ParticleTypes.ELECTRIC_SPARK, ex.getX(), ex.getY() + 0.5, ex.getZ(), 10, 0.2, 0.2, 0.2, 0.3);
                     this.play((SoundEvent)ModSounds.CLASH.get(), 1.0F, 1.3F);
                  }
               }
            }

            return true;
         case 5:
            if (t == 1) {
               this.play((SoundEvent)ModSounds.EAGLE_SCREECH.get(), 2.4F, 0.8F);
            }

            if (t == 20) {
               this.play((SoundEvent)ModSounds.LANCE_RAIN.get(), 2.2F, 1.0F);
            }

            if (t < 112) {
               this.hover(1.5, 0.2);
            } else {
               this.setNoGravity(false);
            }

            int rel = t - 20 - 8;
            if (rel >= 0 && rel % 3 == 0 && rel / 3 < 27) {
               Entity tgt = this.lanceTarget(rel / 3);
               if (tgt != null) {
                  if (tgt instanceof BeybladeEntity b) {
                     b.damageSpin(26.0F, this);
                     b.setDeltaMovement(b.getDeltaMovement().add(0.0, 0.2, 0.0));
                  } else if (tgt instanceof LivingEntity l) {
                     l.invulnerableTime = 0;
                     l.hurt(this.damageSources().indirectMagic(this, owner), 4.0F);
                  }

                  Px.send(sl, ParticleTypes.END_ROD, tgt.getX(), tgt.getY() + 0.3, tgt.getZ(), 8, 0.2, 0.1, 0.2, 0.15);
               }
            }

            if (t == 104) {
               this.play((SoundEvent)ModSounds.EAGLE_SCREECH.get(), 2.6F, 0.7F);
               this.level().broadcastEntityEvent(this, (byte)93);

               for (Entity e : this.enemiesWithin(14.0)) {
                  Vec3 out = flat(e.position().subtract(this.position()));
                  out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
                  if (e instanceof BeybladeEntity b) {
                     b.damageSpin(60.0F, this);
                     b.setDeltaMovement(b.getDeltaMovement().add(out.scale(0.6)));
                  } else if (e instanceof LivingEntity l) {
                     l.hurt(this.damageSources().indirectMagic(this, owner), 6.0F);
                     l.knockback(1.0, -out.x, -out.z);
                  }
               }
            }

            return true;
         default:
            return false;
      }
   }

   @Nullable
   public Entity lanceTarget(int k) {
      List<Entity> list = new ArrayList<>(this.enemiesWithin(14.0));
      if (list.isEmpty()) {
         return null;
      } else {
         list.sort(Comparator.comparingInt(Entity::getId));
         return list.get(k % list.size());
      }
   }

   @Override
   public void handleEntityEvent(byte id) {
      switch (id) {
         case 90:
            this.dodgeTime = this.tickCount;
            this.dodgeX = this.getX();
            this.dodgeY = this.getY();
            this.dodgeZ = this.getZ();

            for (int i = 0; i < 14; i++) {
               Px.add(
                  this.level(),
                  i % 2 == 0 ? VIOLET : SILVER,
                  this.getX(),
                  this.getY() + 0.4,
                  this.getZ(),
                  this.random.nextGaussian() * 0.15,
                  0.08,
                  this.random.nextGaussian() * 0.15
               );
            }
            break;
         case 91:
            this.grabTime = this.tickCount;
            break;
         case 92:
            this.slamTime = this.tickCount;
            this.clientScar(3.0F);
            break;
         case 93:
            this.screechTime = this.tickCount;
            break;
         default:
            super.handleEntityEvent(id);
      }
   }

   @Override
   protected void clientBuffParticles(Level lv) {
      if (this.random.nextInt(2) == 0) {
         double a = this.random.nextDouble() * Math.PI * 2.0;
         Px.add(
            lv,
            VIOLET,
            this.getX() + Math.cos(a) * 0.5,
            this.getY() + 0.5,
            this.getZ() + Math.sin(a) * 0.5,
            -this.getDeltaMovement().x * 0.3,
            0.02,
            -this.getDeltaMovement().z * 0.3
         );
      }
   }

   @Override
   protected void clientAbilityParticles(int ab, float at, Level lv) {
      switch (ab) {
         case 2:
            if (at < 20.0F) {
               double ax = this.random.nextDouble() * Math.PI * 2.0;
               Px.add(
                  lv,
                  SILVER,
                  this.getX() + Math.cos(ax) * 2.5,
                  this.getY() + 0.6,
                  this.getZ() + Math.sin(ax) * 2.5,
                  -Math.cos(ax) * 0.2,
                  0.0,
                  -Math.sin(ax) * 0.2
               );
            }
            break;
         case 3:
            Px.add(lv, VIOLET, this.getX(), this.getY() + 0.3, this.getZ(), 0.0, -0.05, 0.0);
            break;
         case 4:
            if (at > 8.0F && at < 62.0F) {
               double ax = this.random.nextDouble() * Math.PI * 2.0;
               Px.add(
                  lv,
                  ParticleTypes.ELECTRIC_SPARK,
                  this.getX() + Math.cos(ax) * 3.3,
                  this.getY() + 0.4,
                  this.getZ() + Math.sin(ax) * 3.3,
                  0.0,
                  0.05,
                  0.0
               );
            }
            break;
         case 5:
            double a = this.random.nextDouble() * Math.PI * 2.0;
            double r = this.random.nextDouble() * 14.0;
            Px.add(
               lv,
               VIOLET,
               this.abilityX + Math.cos(a) * r,
               this.abilityY + 6.0 + this.random.nextDouble() * 6.0,
               this.abilityZ + Math.sin(a) * r,
               0.0,
               -0.1,
               0.0
            );
      }
   }

   public static AABB unused() {
      return new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
   }

   public static float clamp01(float x) {
      return Mth.clamp(x, 0.0F, 1.0F);
   }
}
