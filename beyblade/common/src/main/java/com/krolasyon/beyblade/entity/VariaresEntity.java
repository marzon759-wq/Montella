package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class VariaresEntity extends BeybladeEntity {
   public static final int VA_FURY = 1;
   public static final int VA_FLASH = 2;
   public static final int VA_SHIELD = 3;
   public static final int VA_BLADES = 4;
   public static final int VA_FINAL = 5;
   public static final int[] VA_DURATION = new int[]{0, 0, 40, 36, 64, 80};
   public static final int[] VA_COOLDOWN = new int[]{0, 600, 420, 480, 520, 1100};
   public static final int FURY_TICKS = 180;
   public static final int[] FLASH_AT = new int[]{6, 17, 28};
   public static final int BLADES_N = 4;
   public static final double BLADES_R = 2.3;
   public static final int FINAL_FORM = 26;
   public static final int FINAL_FALL = 36;
   public static final double SWORD_HEIGHT = 15.0;
   public static final byte EV_SLASH = 125;
   public static final byte EV_SWORD = 126;
   public static final byte EV_SWORD_HIT = 127;
   private final Map<Integer, Integer> bladeHits = new HashMap<>();
   @Nullable
   private Vec3 swordAt;
   public int slashTime = -1000;
   public int swordTime = -1000;
   public int swordHitTime = -1000;
   public double slashX0;
   public double slashZ0;
   public double slashX1;
   public double slashZ1;
   public double slashY;
   public double swordX;
   public double swordY;
   public double swordZ;

   public VariaresEntity(EntityType<? extends VariaresEntity> type, Level level) {
      super(type, level);
   }

   @Override
   public int nativeMode() {
      return 0;
   }

   @Override
   public String kindId() {
      return "variares";
   }

   @Override
   public String abilityKey(int id) {
      return "ability.librabeyblade.variares." + id;
   }

   @Override
   public int duration(int id) {
      return VA_DURATION[id];
   }

   @Override
   public int cooldown(int id) {
      return VA_COOLDOWN[id];
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
      return id == 2 || id == 3 || id == 5;
   }

   @Override
   public boolean buffReflects() {
      return false;
   }

   @Override
   public float buffAttackMul() {
      return this.isShielded() ? 1.7F : 1.1F;
   }

   @Override
   public double buffSpeedMul() {
      return this.isShielded() ? 1.35 : 1.08;
   }

   @Override
   protected float dodgeChance() {
      return 0.09F;
   }

   @Override
   protected ItemStack defaultItem() {
      return new ItemStack((ItemLike)ModItems.VARIARES.get());
   }

   @Override
   protected float clientSpinSpeed(int ab, float at, float speed) {
      return ab != 4 && ab != 3 ? speed : speed * 1.4F;
   }

   @Override
   protected void onBuffStart(@Nullable Player owner) {
      this.play((SoundEvent)ModSounds.DRAGON_ROAR.get(), 1.8F, 0.6F);
      this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 1.6F, 0.5F);
   }

   @Override
   protected void tickBuff() {
      this.tickShield();
   }

   @Override
   protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
      switch (ab) {
         case 2:
            if (this.target != null && this.target.isAlive()) {
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.3, 1.0, 0.3));

               for (int k = 0; k < FLASH_AT.length; k++) {
                  if (t == FLASH_AT[k]) {
                     Vec3 to = flat(this.target.position().subtract(this.position()));
                     Vec3 dir = to.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : to.normalize();
                     double rot = (double)(k - 1) * 0.9;
                     Vec3 d = new Vec3(
                        dir.x * Math.cos(rot) - dir.z * Math.sin(rot), 0.0, dir.x * Math.sin(rot) + dir.z * Math.cos(rot)
                     );
                     Vec3 end = this.target.position().add(d.scale(2.6));
                     this.level().broadcastEntityEvent(this, (byte)125);
                     if (this.level().noCollision(this, this.getBoundingBox().move(end.subtract(this.position())))) {
                        this.setPos(end.x, this.getY(), end.z);
                     }

                     this.play((SoundEvent)ModSounds.CLAW_SLASH.get(), 1.8F, 1.3F);
                     this.play((SoundEvent)ModSounds.THUNDER_STRIKE.get(), 1.0F, 1.8F);
                     Entity var42 = this.target;
                     if (var42 instanceof BeybladeEntity) {
                        BeybladeEntity b = (BeybladeEntity)var42;
                        if (!b.tryDodge(this)) {
                           b.damageSpin(65.0F * this.powerMul(), this);
                           b.setDeltaMovement(b.getDeltaMovement().add(d.scale(-0.35)).add(0.0, 0.1, 0.0));
                           this.level().broadcastEntityEvent(b, (byte)61);
                        }
                     } else if (this.target instanceof LivingEntity l) {
                        l.invulnerableTime = 0;
                        l.hurt(this.damageSources().indirectMagic(this, owner), 6.0F);
                     }
                  }
               }

               return true;
            }

            this.endAbilityEarly();
            return true;
         case 3:
            if (t == 1) {
               this.play((SoundEvent)ModSounds.SHIELD_UP.get(), 1.6F, 0.7F);
            }

            Vec3 aim = this.target != null && this.target.isAlive() ? this.target.position() : this.position().add(this.getDeltaMovement().scale(10.0));
            Vec3 dirx = flat(aim.subtract(this.position()));
            dirx = dirx.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : dirx.normalize();
            if (t > 4) {
               this.setDeltaMovement(dirx.x * 0.85, this.getDeltaMovement().y, dirx.z * 0.85);
            }

            for (Entity ex : this.enemiesWithin(1.6)) {
               Integer last = this.bladeHits.get(ex.getId());
               if (last == null || t - last >= 12) {
                  this.bladeHits.put(ex.getId(), t);
                  if (ex instanceof BeybladeEntity bx) {
                     bx.damageSpin(80.0F * this.powerMul(), this);
                     bx.setDeltaMovement(dirx.scale(1.4 * bx.knockMul()).add(0.0, 0.2, 0.0));
                     bx.shockwave(this, 0.9);
                  } else if (ex instanceof LivingEntity l) {
                     l.hurt(this.damageSources().indirectMagic(this, owner), 8.0F);
                     l.knockback(1.6, -dirx.x, -dirx.z);
                  }

                  this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 2.2F, 0.6F);
               }
            }

            if (t == VA_DURATION[3] - 1) {
               this.bladeHits.clear();
            }

            return t > 4;
         case 4:
            if (t == 1) {
               this.bladeHits.clear();
               this.play((SoundEvent)ModSounds.CLAW_SLASH.get(), 1.6F, 0.8F);
            }

            if (t % 8 == 0) {
               this.play((SoundEvent)ModSounds.CLAW_SLASH.get(), 1.2F, 1.0F + (float)(t % 16) * 0.02F);
            }

            for (Entity e : this.enemiesWithin(3.0999999999999996)) {
               Integer last = this.bladeHits.get(e.getId());
               if (last == null || t - last >= 8) {
                  this.bladeHits.put(e.getId(), t);
                  Vec3 out = flat(e.position().subtract(this.position()));
                  out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
                  Vec3 tan = new Vec3(-out.z, 0.0, out.x).scale((double)this.spinDir());
                  if (e instanceof BeybladeEntity bx) {
                     bx.damageSpin(24.0F * this.powerMul(), this);
                     bx.setDeltaMovement(bx.getDeltaMovement().add(tan.scale(0.35)).add(out.scale(0.2)));
                     this.level().broadcastEntityEvent(bx, (byte)60);
                  } else if (e instanceof LivingEntity l) {
                     l.invulnerableTime = 0;
                     l.hurt(this.damageSources().indirectMagic(this, owner), 3.0F);
                  }
               }
            }

            return false;
         case 5:
            if (t == 1) {
               this.play((SoundEvent)ModSounds.DRAGON_ROAR.get(), 2.0F, 0.55F);
               this.play((SoundEvent)ModSounds.STORM_CLOUD.get(), 2.0F, 0.8F);
            }

            this.hover(1.5, 0.2);
            if (t < 36 && this.target != null && this.target.isAlive()) {
               this.swordAt = this.target.position();
               if (t == 2 || t % 5 == 0) {
                  this.level().broadcastEntityEvent(this, (byte)126);
               }
            }

            if (t == 26) {
               this.play((SoundEvent)ModSounds.THUNDER_STRIKE.get(), 2.5F, 0.7F);
            }

            if (t == 36 && this.swordAt != null) {
               ServerLevel sl = (ServerLevel)this.level();
               this.play((SoundEvent)ModSounds.METEOR_IMPACT.get(), 3.0F, 0.8F);
               this.play((SoundEvent)ModSounds.THUNDER_STRIKE.get(), 3.0F, 0.6F);
               this.level().broadcastEntityEvent(this, (byte)127);

               for (int i = 0; i < 3; i++) {
                  LightningBolt bolt = (LightningBolt)EntityType.LIGHTNING_BOLT.create(sl);
                  if (bolt != null) {
                     bolt.moveTo(
                        this.swordAt.x + this.random.nextGaussian() * 2.0, this.swordAt.y, this.swordAt.z + this.random.nextGaussian() * 2.0
                     );
                     bolt.setVisualOnly(true);
                     sl.addFreshEntity(bolt);
                  }
               }

               for (Entity e : this.level().getEntities(this, new AABB(this.swordAt, this.swordAt).inflate(6.5, 4.0, 6.5))) {
                  boolean var10000;
                  if (e instanceof BeybladeEntity b) {
                     var10000 = this.isEnemy(b);
                  } else {
                     label198: {
                        if (e instanceof LivingEntity l && this.isHostileLiving(l)) {
                           var10000 = true;
                           break label198;
                        }

                        var10000 = false;
                     }
                  }

                  boolean enemy = var10000;
                  if (enemy || e == this.target) {
                     Vec3 out = flat(e.position().subtract(this.swordAt));
                     double dx = out.length();
                     out = dx < 0.001 ? new Vec3(1.0, 0.0, 0.0) : out.scale(1.0 / dx);
                     boolean main = e == this.target || dx < 1.5;
                     if (e instanceof BeybladeEntity b) {
                        b.damageSpin((main ? 300.0F : 110.0F) * this.powerMul(), this);
                        b.setDeltaMovement(b.getDeltaMovement().add(out.scale(main ? 0.4 : 0.9)).add(0.0, main ? 0.5 : 0.3, 0.0));
                        if (main) {
                           b.shockwave(this, 1.0);
                        }
                     } else if (e instanceof LivingEntity l) {
                        l.hurt(this.damageSources().indirectMagic(this, owner), main ? 18.0F : 8.0F);
                        l.knockback(1.4, -out.x, -out.z);
                     }
                  }
               }
            }

            if (t > 42) {
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
         case 125:
            this.slashTime = this.tickCount;
            Entity tx = this.level().getEntity(this.getTargetId());
            this.slashX0 = this.getX();
            this.slashZ0 = this.getZ();
            this.slashY = this.getY() + 0.4;
            if (tx != null) {
               Vec3 d = new Vec3(tx.getX() - this.getX(), 0.0, tx.getZ() - this.getZ());
               Vec3 e = d.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : d.normalize();
               this.slashX1 = tx.getX() + e.x * 2.6;
               this.slashZ1 = tx.getZ() + e.z * 2.6;
            } else {
               this.slashX1 = this.getX() + 3.0;
               this.slashZ1 = this.getZ();
            }
            break;
         case 126:
            if (this.swordTime < this.clientAbilityStart) {
               this.swordTime = this.tickCount;
            }

            Entity tx2 = this.level().getEntity(this.getTargetId());
            if (tx2 != null) {
               this.swordX = tx2.getX();
               this.swordY = tx2.getY();
               this.swordZ = tx2.getZ();
            }
            break;
         case 127:
            this.swordHitTime = this.tickCount;
            Iterator t = this.level().getEntitiesOfClass(StadiumEntity.class, this.getBoundingBox().inflate(18.0, 6.0, 18.0)).iterator();
            if (t.hasNext()) {
               StadiumEntity st = (StadiumEntity)t.next();
               double dx = this.swordX - st.getX();
               double dz = this.swordZ - st.getZ();
               if (dx * dx + dz * dz < 110.25) {
                  st.addScar(this.swordX, this.swordZ, 4.5F);
               }
            }
            break;
         default:
            super.handleEntityEvent(id);
      }
   }
}
