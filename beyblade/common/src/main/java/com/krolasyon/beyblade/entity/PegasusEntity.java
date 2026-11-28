package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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

public class PegasusEntity extends BeybladeEntity {
   public static final int PG_WINGS = 1;
   public static final int PG_STORM = 2;
   public static final int PG_STAR = 3;
   public static final int PG_RICO = 4;
   public static final int PG_GALLOP = 5;
   public static final int[] PG_DURATION = new int[]{0, 0, 100, 72, 64, 110};
   public static final int[] PG_COOLDOWN = new int[]{0, 600, 600, 550, 500, 1100};
   public static final int WINGS_TICKS = 200;
   public static final int S_FORM = 14;
   public static final int S_STRIKES = 16;
   public static final int S_STEP = 5;
   public static final double S_RANGE = 13.0;
   public static final double S_CLOUD_Y = 9.0;
   public static final int C_STAR0 = 6;
   public static final int C_STAR_STEP = 4;
   public static final int C_LINES = 24;
   public static final int C_COLLAPSE = 40;
   public static final int C_BLAST = 46;
   public static final double C_RADIUS = 4.0;
   public static final int R_FIRST = 4;
   public static final int R_STEP = 14;
   public static final int R_DASH = 11;
   public static final int R_COUNT = 4;
   public static final int G_START = 15;
   public static final int G_END = 95;
   public static final int B_RIDE_END = 48;
   public static final int B_TOP = 60;
   public static final int B_DIVE = 68;
   public static final double B_HEIGHT = 9.0;
   public static final double G_RADIUS = 6.5;
   public static final byte EV_STAR = 100;
   public static final byte EV_RICO = 101;
   public static final byte EV_NOVA = 102;
   private static final DustParticleOptions SKY = new DustParticleOptions(new Vector3f(0.35F, 0.7F, 1.0F), 1.3F);
   private Vec3 ricoDir = new Vec3(1.0, 0.0, 0.0);
   @Nullable
   private Vec3 starCenter;
   private final Set<Integer> hits = new HashSet<>();
   private int lap = -1;
   private double bcx;
   private double bcz;
   private double br;
   private double bang;
   public int starTime = -1000;
   public int ricoTime = -1000;
   public int novaTime = -1000;
   public double starX;
   public double starY;
   public double starZ;

   public PegasusEntity(EntityType<? extends PegasusEntity> type, Level level) {
      super(type, level);
   }

   @Override
   protected float dodgeChance() {
      return this.isBuffActive() ? 0.3F : 0.16F;
   }

   @Override
   public int nativeMode() {
      return 0;
   }

   @Override
   public String kindId() {
      return "pegasus";
   }

   @Override
   public String abilityKey(int id) {
      return "ability.librabeyblade.pegasus." + id;
   }

   @Override
   public int duration(int id) {
      return PG_DURATION[id];
   }

   @Override
   public int cooldown(int id) {
      return PG_COOLDOWN[id];
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
   public float buffAttackMul() {
      return this.isShielded() ? 1.4F : 1.1F;
   }

   @Override
   public double buffSpeedMul() {
      return this.isShielded() ? 1.65 : 1.12;
   }

   @Override
   protected ItemStack defaultItem() {
      return new ItemStack((ItemLike)ModItems.PEGASUS.get());
   }

   @Override
   protected float clientSpinSpeed(int ab, float at, float speed) {
      if (ab != 4 && ab != 5) {
         return this.isShielded() ? speed * 1.2F : speed;
      } else {
         return 105.0F;
      }
   }

   @Override
   protected void onBuffStart(@Nullable Player owner) {
      this.play((SoundEvent)ModSounds.PEGASUS_NEIGH.get(), 1.8F, 1.0F);
      Px.send((ServerLevel)this.level(), SKY, this.getX(), this.getY() + 0.6, this.getZ(), 30, 0.7, 0.4, 0.7, 0.0);
   }

   @Override
   protected void tickBuff() {
      if (this.tickCount % 3 == 0) {
         Px.send((ServerLevel)this.level(), ParticleTypes.END_ROD, this.getX(), this.getY() + 0.4, this.getZ(), 1, 0.2, 0.1, 0.2, 0.01);
      }
   }

   @Override
   protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
      ServerLevel sl = (ServerLevel)this.level();
      switch (ab) {
         case 2:
            if (t == 1) {
               this.play((SoundEvent)ModSounds.STORM_CLOUD.get(), 2.0F, 1.0F);
            }

            this.hover(0.6, 0.2);
            int rel0 = t - 14;
            if (rel0 >= 0 && rel0 % 5 == 0 && rel0 / 5 < 16) {
               int k = rel0 / 5;
               Entity tgt = this.strikeTarget(k);
               Vec3 p = tgt != null ? tgt.position() : strikePoint(k, this.getX(), this.getY(), this.getZ());
               this.level()
                  .playSound(
                     null,
                     p.x,
                     p.y,
                     p.z,
                     (SoundEvent)ModSounds.THUNDER_STRIKE.get(),
                     SoundSource.NEUTRAL,
                     1.6F,
                     0.9F + this.random.nextFloat() * 0.3F
                  );
               Px.send(sl, ParticleTypes.ELECTRIC_SPARK, p.x, p.y + 0.3, p.z, 20, 0.3, 0.3, 0.3, 0.5);
               if (tgt instanceof BeybladeEntity b) {
                  b.damageSpin(28.0F, this);
                  b.stun(6);
               } else if (tgt instanceof LivingEntity l) {
                  l.invulnerableTime = 0;
                  l.hurt(this.damageSources().indirectMagic(this, owner), 4.0F);
               }
            }

            return true;
         case 3:
            if (t == 1) {
               if (this.target == null) {
                  this.endAbilityEarly();
                  return true;
               }

               this.starCenter = this.target.position();
               this.level().broadcastEntityEvent(this, (byte)100);
               this.play((SoundEvent)ModSounds.STAR_CHIME.get(), 1.6F, 1.0F);
            }

            if (this.starCenter == null) {
               return true;
            }

            this.hover(1.0, 0.2);
            int relx = t - 6;
            if (relx >= 0 && relx % 4 == 0 && relx / 4 < 5) {
               this.play((SoundEvent)ModSounds.STAR_CHIME.get(), 1.0F, 1.2F + (float)(relx / 4) * 0.12F);
            }

            if (t >= 24 && t < 46) {
               for (Entity exx : this.enemiesWithin(24.0)) {
                  Vec3 dxx = flat(exx.position().subtract(this.starCenter));
                  if (dxx.length() < 4.5) {
                     exx.setDeltaMovement(exx.getDeltaMovement().multiply(0.4, 1.0, 0.4).subtract(dxx.scale(0.04)));
                     exx.hurtMarked = true;
                  }
               }
            }

            if (t == 46) {
               this.play((SoundEvent)ModSounds.THUNDER_STRIKE.get(), 2.0F, 1.4F);
               this.play((SoundEvent)ModSounds.STAR_CHIME.get(), 2.0F, 0.7F);
               this.level().broadcastEntityEvent(this, (byte)102);
               Px.send(sl, ParticleTypes.END_ROD, this.starCenter.x, this.starCenter.y + 0.5, this.starCenter.z, 60, 1.5, 1.0, 1.5, 0.25);

               for (Entity exxx : this.enemiesWithin(30.0)) {
                  Vec3 dxx = flat(exxx.position().subtract(this.starCenter));
                  double dist = dxx.length();
                  if (!(dist > 5.0)) {
                     Vec3 out = dist < 0.001 ? new Vec3(1.0, 0.0, 0.0) : dxx.scale(1.0 / dist);
                     if (exxx instanceof BeybladeEntity b) {
                        b.damageSpin(150.0F, this);
                        b.setDeltaMovement(out.scale(0.6).add(0.0, 0.7, 0.0));
                     } else if (exxx instanceof LivingEntity l) {
                        l.hurt(this.damageSources().indirectMagic(this, owner), 12.0F);
                        l.knockback(1.0, -out.x, -out.z);
                     }
                  }
               }
            }

            return true;
         case 4:
            int rel = t - 4;
            if (rel < 0) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 1.0, 0.5));
               return true;
            } else {
               int k = rel / 14;
               int s = rel % 14;
               if (k >= 4) {
                  this.setNoGravity(false);
                  this.setDeltaMovement(this.getDeltaMovement().multiply(0.6, 1.0, 0.6));
                  return true;
               }

               if (s == 0) {
                  this.hits.clear();
                  Vec3 base = new Vec3(this.abilityX(), this.abilityBaseY, this.abilityZ0());
                  Vec3 dir;
                  if (k == 0 && this.target != null) {
                     dir = flat(this.target.position().subtract(this.position()));
                  } else {
                     dir = rotate(this.ricoDir, Math.toRadians((double)(140 + this.random.nextInt(60) - 30)));
                  }

                  if (flat(this.position().subtract(base)).length() > 6.0) {
                     dir = flat(base.subtract(this.position()));
                  }

                  this.ricoDir = dir.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : dir.normalize();
                  this.play((SoundEvent)ModSounds.RICOCHET.get(), 1.4F, 0.9F + (float)k * 0.1F);
                  this.level().broadcastEntityEvent(this, (byte)101);
               }

               if (s < 11) {
                  this.setNoGravity(true);
                  this.hover(0.35, 0.3);
                  this.setDeltaMovement(this.ricoDir.x * 1.15, this.getDeltaMovement().y, this.ricoDir.z * 1.15);

                  for (Entity ex : this.enemiesWithin(1.7)) {
                     if (this.hits.add(ex.getId())) {
                        Vec3 side = new Vec3(-this.ricoDir.z, 0.0, this.ricoDir.x);
                        if (side.dot(ex.position().subtract(this.position())) < 0.0) {
                           side = side.scale(-1.0);
                        }

                        if (ex instanceof BeybladeEntity b) {
                           b.damageSpin(70.0F, this);
                           b.setDeltaMovement(b.getDeltaMovement().add(side.scale(0.6)).add(this.ricoDir.scale(0.3)).add(0.0, 0.2, 0.0));
                        } else if (ex instanceof LivingEntity l) {
                           l.hurt(this.damageSources().indirectMagic(this, owner), 7.0F);
                           l.knockback(0.9, -side.x, -side.z);
                        }

                        Px.send(sl, ParticleTypes.SWEEP_ATTACK, ex.getX(), ex.getY() + 0.5, ex.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                        this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 1.1F, 1.3F);
                     }
                  }
               } else {
                  this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 1.0, 0.3));
               }

               return true;
            }
         case 5:
            if (t == 1) {
               double[] c = boosterRing(this.level(), this.getX(), this.getZ());
               this.bcx = c[0];
               this.bcz = c[1];
               this.br = c[2];
               this.bang = Math.atan2(this.getZ() - this.bcz, this.getX() - this.bcx);
               this.hits.clear();
               this.play((SoundEvent)ModSounds.PEGASUS_NEIGH.get(), 2.2F, 0.9F);
            }

            if (t < 48) {
               double speed = 0.55 + 0.85 * (double)t / 48.0;
               this.bang = this.bang + speed / this.br * (double)this.spinDir();
               Vec3 goal = new Vec3(this.bcx + Math.cos(this.bang) * this.br, this.getY(), this.bcz + Math.sin(this.bang) * this.br);
               Vec3 d = flat(goal.subtract(this.position()));
               this.setDeltaMovement(d.x, this.getDeltaMovement().y, d.z);
               if (t % 12 == 1) {
                  this.play((SoundEvent)ModSounds.GALLOP.get(), 1.6F, 1.0F + (float)t * 0.01F);
               }

               if (t % 16 == 0) {
                  this.hits.clear();
               }

               for (Entity e : this.enemiesWithin(this.br + 8.0)) {
                  Vec3 relV = flat(e.position().subtract(new Vec3(this.bcx, 0.0, this.bcz)));
                  double dist = relV.length();
                  if (!(dist < 0.001)) {
                     Vec3 in = relV.scale(-1.0 / dist);
                     Vec3 tan = new Vec3(-in.z, 0.0, in.x).scale((double)this.spinDir());
                     e.setDeltaMovement(e.getDeltaMovement().add(in.scale(dist > 1.5 ? 0.08 : 0.0)).add(tan.scale(0.05)));
                     e.hurtMarked = true;
                     if (e.distanceToSqr(this) < 2.5600000000000005 && this.hits.add(e.getId())) {
                        if (e instanceof BeybladeEntity b) {
                           b.damageSpin(45.0F * this.powerMul(), this);
                           b.setDeltaMovement(b.getDeltaMovement().add(in.scale(0.5)));
                           this.level().broadcastEntityEvent(b, (byte)61);
                        } else if (e instanceof LivingEntity l) {
                           l.hurt(this.damageSources().indirectMagic(this, owner), 5.0F);
                        }

                        this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 1.2F, 1.1F);
                     }
                  }
               }

               return true;
            } else {
               Vec3 aim = this.target != null && this.target.isAlive() ? this.target.position() : new Vec3(this.bcx, this.abilityBaseY, this.bcz);
               if (t < 68) {
                  this.setNoGravity(true);
                  if (t == 48) {
                     this.play((SoundEvent)ModSounds.PEGASUS_NEIGH.get(), 2.4F, 1.2F);
                     this.play((SoundEvent)ModSounds.STAR_CHIME.get(), 2.0F, 1.0F);
                  }

                  Vec3 dx = aim.add(0.0, 9.0, 0.0).subtract(this.position());
                  this.setDeltaMovement(dx.scale(t < 60 ? 0.18 : 0.3));
                  return true;
               }

               if (t == 68) {
                  this.diving = true;
                  this.play((SoundEvent)ModSounds.THUNDER_STRIKE.get(), 2.2F, 1.3F);
               }

               if (this.diving) {
                  Vec3 dx = aim.add(0.0, 0.2, 0.0).subtract(this.position());
                  if (dx.lengthSqr() > 1.0E-4) {
                     this.setDeltaMovement(dx.normalize().scale(2.6));
                  }

                  boolean hit = this.target != null && this.target.distanceToSqr(this) < 2.5600000000000005;
                  if (hit || this.onGround() || this.verticalCollision || t > 88) {
                     this.diving = false;
                     this.setNoGravity(false);
                     this.setDeltaMovement(0.0, 0.0, 0.0);
                     this.boosterImpact(owner);
                     this.abilityTicks = Math.max(this.abilityTicks, PG_DURATION[5] - 14);
                  }
               }

               return true;
            }
         default:
            return false;
      }
   }

   public static double[] boosterRing(Level level, double x, double z) {
      for (StadiumEntity st : level.getEntitiesOfClass(StadiumEntity.class, new AABB(x - 20.0, -1000.0, z - 20.0, x + 20.0, 1000.0, z + 20.0))) {
         double dx = x - st.getX();
         double dz = z - st.getZ();
         if (dx * dx + dz * dz < 123.21001F) {
            return new double[]{st.getX(), st.getZ(), 8.4};
         }
      }

      return new double[]{x, z, 5.5};
   }

   private void boosterImpact(@Nullable Player owner) {
      this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 3.0F, 0.6F);
      this.play((SoundEvent)ModSounds.THUNDER_STRIKE.get(), 2.5F, 0.9F);
      this.level().broadcastEntityEvent(this, (byte)102);
      this.level().broadcastEntityEvent(this, (byte)65);

      for (Entity e : this.enemiesWithin(5.5)) {
         Vec3 out = flat(e.position().subtract(this.position()));
         out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
         boolean main = e == this.target;
         if (e instanceof BeybladeEntity) {
            BeybladeEntity b = (BeybladeEntity)e;
            b.damageSpin((main ? 260.0F : 90.0F) * this.powerMul(), this);
            b.setDeltaMovement(b.getDeltaMovement().add(out.scale(main ? 0.9 : 0.6)).add(0.0, 0.35, 0.0));
            if (main) {
               b.shockwave(this, 1.0);
            }
         } else if (e instanceof LivingEntity l) {
            l.hurt(this.damageSources().indirectMagic(this, owner), main ? 14.0F : 6.0F);
            l.knockback(1.2, -out.x, -out.z);
         }
      }
   }

   private double abilityX() {
      return this.abilityBaseX;
   }

   private double abilityZ0() {
      return this.abilityBaseZ;
   }

   private static Vec3 rotate(Vec3 v, double a) {
      double c = Math.cos(a);
      double s = Math.sin(a);
      return new Vec3(v.x * c - v.z * s, 0.0, v.x * s + v.z * c);
   }

   public static double gallopAngle(float t) {
      return (double)(Math.max(0.0F, t - 15.0F) / 80.0F) * Math.PI * 4.0;
   }

   @Nullable
   public Entity strikeTarget(int k) {
      List<Entity> list = new ArrayList<>(this.enemiesWithin(13.0));
      if (list.isEmpty()) {
         return null;
      } else {
         list.sort(Comparator.comparingInt(Entity::getId));
         return list.get(k % list.size());
      }
   }

   public static Vec3 strikePoint(int k, double x, double y, double z) {
      double a = (double)k * 2.4;
      double r = 2.5 + (double)(k % 3) * 2.0;
      return new Vec3(x + Math.cos(a) * r, y, z + Math.sin(a) * r);
   }

   @Override
   public void handleEntityEvent(byte id) {
      switch (id) {
         case 100:
            this.starTime = this.tickCount;
            Entity t = this.level().getEntity(this.getTargetId());
            Entity c = (Entity)(t != null ? t : this);
            this.starX = c.getX();
            this.starY = c.getY();
            this.starZ = c.getZ();
            break;
         case 101:
            this.ricoTime = this.tickCount;
            break;
         case 102:
            this.novaTime = this.tickCount;
            this.clientScar(2.5F);
            break;
         default:
            super.handleEntityEvent(id);
      }
   }

   @Override
   protected void clientBuffParticles(Level lv) {
      Px.add(
         lv, SKY, this.getX() + this.random.nextGaussian() * 0.3, this.getY() + 0.5, this.getZ() + this.random.nextGaussian() * 0.3, 0.0, 0.03, 0.0
      );
   }

   @Override
   protected void clientAbilityParticles(int ab, float at, Level lv) {
      switch (ab) {
         case 2:
            for (int i = 0; i < 3; i++) {
               double a = this.random.nextDouble() * Math.PI * 2.0;
               double r = this.random.nextDouble() * 13.0 * 0.8;
               Px.add(lv, ParticleTypes.FALLING_WATER, this.abilityX + Math.cos(a) * r, this.abilityY + 9.0 - 0.5, this.abilityZ + Math.sin(a) * r, 0.0, -0.5, 0.0);
            }
         case 3:
         default:
            break;
         case 4:
         case 5:
            Px.add(lv, SKY, this.getX(), this.getY() + 0.4, this.getZ(), 0.0, 0.02, 0.0);
      }
   }
}
