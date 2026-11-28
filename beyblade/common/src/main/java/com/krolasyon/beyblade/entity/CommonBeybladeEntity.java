package com.krolasyon.beyblade.entity;

import com.krolasyon.beyblade.item.LibraItem;
import com.krolasyon.beyblade.registry.ModItems;
import com.krolasyon.beyblade.registry.ModSounds;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CommonBeybladeEntity extends BeybladeEntity {
   public static final CommonBeybladeEntity.Variant[] VARIANTS = new CommonBeybladeEntity.Variant[]{
      new CommonBeybladeEntity.Variant("iron_bull", 0, 14168616, 16761402),
      new CommonBeybladeEntity.Variant("frost_wolf", 3, 7260415, 15792895),
      new CommonBeybladeEntity.Variant("jade_turtle", 1, 3121738, 12124058),
      new CommonBeybladeEntity.Variant("thunder_viper", 0, 16110110, 10112255),
      new CommonBeybladeEntity.Variant("shadow_bat", 2, 7024565, 14268671),
      new CommonBeybladeEntity.Variant("sky_falcon", 3, 4099824, 16777215),
      new CommonBeybladeEntity.Variant("crimson_scorpion", 0, 10489898, 16747066),
      new CommonBeybladeEntity.Variant("golden_phoenix", 2, 16756768, 16734746),
      new CommonBeybladeEntity.Variant("ocean_shark", 1, 1724320, 4251856),
      new CommonBeybladeEntity.Variant("storm_fox", 3, 16742938, 16773856)
   };
   public static final int CM_RUSH = 1;
   public static final int CM_GUARD = 2;
   public static final int CM_BURST = 3;
   public static final int[] CM_DURATION = new int[]{0, 30, 0, 40, 0, 0};
   public static final int[] CM_COOLDOWN = new int[]{0, 260, 520, 720, 1, 1};
   public static final int GUARD_TICKS = 160;
   public static final int BURST_UNLOCK = 5;
   private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(CommonBeybladeEntity.class, EntityDataSerializers.INT);

   public CommonBeybladeEntity(EntityType<? extends CommonBeybladeEntity> type, Level level) {
      super(type, level);
   }

   @Override
   protected void defineData(DataDef d) {
      super.defineData(d);
      d.define(DATA_VARIANT, 0);
   }

   public int getVariant() {
      return Math.floorMod((Integer)this.entityData.get(DATA_VARIANT), VARIANTS.length);
   }

   public void setVariant(int v) {
      this.entityData.set(DATA_VARIANT, Math.floorMod(v, VARIANTS.length));
   }

   public CommonBeybladeEntity.Variant variant() {
      return VARIANTS[this.getVariant()];
   }

   @Override
   protected void initFromItem(ItemStack stack) {
      if (stack.getItem() instanceof LibraItem li && li.variant() >= 0) {
         this.setVariant(li.variant());
      }
   }

   @Override
   protected float dodgeChance() {
      return 0.06F;
   }

   @Override
   public int nativeMode() {
      return this.variant().mode();
   }

   @Override
   public String kindId() {
      return "common";
   }

   @Override
   public String titleKey() {
      return "item.librabeyblade.common_" + this.variant().id();
   }

   @Override
   public String abilityKey(int id) {
      if (id == 3) {
         return "ability.librabeyblade.common.3." + MODE_IDS[this.variant().mode()];
      } else {
         return id > 3 ? "ability.librabeyblade.common.none" : "ability.librabeyblade.common." + id;
      }
   }

   @Override
   public int duration(int id) {
      return CM_DURATION[id];
   }

   @Override
   public int cooldown(int id) {
      return CM_COOLDOWN[id];
   }

   @Override
   public int buffAbility() {
      return 2;
   }

   @Override
   public int buffTicks() {
      return 160;
   }

   @Override
   public boolean requiresTarget(int id) {
      return id == 1;
   }

   @Override
   public boolean buffReflects() {
      return false;
   }

   @Override
   public float incomingMul() {
      return this.isShielded() ? 0.5F : 1.0F;
   }

   @Override
   public boolean abilityAvailable(int id) {
      return id <= 2 || id == 3 && this.getLevel() >= 5;
   }

   @Override
   protected boolean ramsInto(int id) {
      return id == 1 || id == 3 && (this.variant().mode() == 0 || this.variant().mode() == 3);
   }

   @Override
   protected ItemStack defaultItem() {
      return new ItemStack((ItemLike)ModItems.COMMONS.get(this.getVariant()).get());
   }

   @Override
   protected float clientSpinSpeed(int ab, float at, float speed) {
      return speed;
   }

   @Override
   protected void onBuffStart(@Nullable Player owner) {
      this.play((SoundEvent)ModSounds.SHIELD_UP.get(), 1.2F, 1.3F);
   }

   @Override
   protected void tickBuff() {
      this.tickShield();
   }

   @Override
   protected boolean tickAbility(int ab, int t, @Nullable Player owner) {
      switch (ab) {
         case 1:
            if (t == 1) {
               this.strike(2.4, 70.0F, 0.7, 0.15);
            }

            if (t == 12 && this.target != null) {
               Vec3 d = flat(this.target.position().subtract(this.position()));
               if (d.lengthSqr() > 1.0E-4) {
                  this.setDeltaMovement(this.getDeltaMovement().add(d.normalize().scale(0.8)));
               }
            }

            if (t > 12 && t < 22 && this.target != null && this.target.distanceToSqr(this) < 2.5600000000000005) {
               this.strike(2.0, 45.0F, 0.6, 0.2);
               this.abilityTicks = 22;
            }

            return t > 12;
         case 3:
            switch (this.variant().mode()) {
               case 0:
                  if (t == 1) {
                     this.strike(2.6, 90.0F, 0.35, 0.8);
                  }
                  break;
               case 1:
                  if (t == 6) {
                     this.strike(5.5, 55.0F, 0.9, 0.2);
                  }
                  break;
               case 2:
                  if (t == 1) {
                     this.play((SoundEvent)ModSounds.SHIELD_UP.get(), 1.0F, 1.6F);
                  }

                  if (t % 4 == 0) {
                     this.healSpin(this.getMaxSpin() * 0.013F);
                  }

                  this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 1.0, 0.8));
                  break;
               default:
                  if (t == 1) {
                     this.strike(3.2, 65.0F, 0.6, 0.15);
                     this.healSpin(this.getMaxSpin() * 0.06F);
                  }
            }

            return true;
         default:
            return false;
      }
   }

   private void strike(double r, float dmg, double knock, double up) {
      this.play((SoundEvent)ModSounds.CLASH_HEAVY.get(), 1.5F, 0.85F + this.random.nextFloat() * 0.2F);
      this.level().broadcastEntityEvent(this, (byte)66);

      for (Entity e : this.enemiesWithin(r)) {
         Vec3 out = flat(e.position().subtract(this.position()));
         out = out.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : out.normalize();
         if (e instanceof BeybladeEntity) {
            BeybladeEntity b = (BeybladeEntity)e;
            if (!b.tryDodge(this)) {
               b.damageSpin(dmg * this.powerMul(), this);
               b.setDeltaMovement(b.getDeltaMovement().add(out.scale(knock * b.knockMul())).add(0.0, up, 0.0));
               this.level().broadcastEntityEvent(b, (byte)61);
            }
         } else if (e instanceof LivingEntity l) {
            l.hurt(this.damageSources().indirectMagic(this, this.getOwner()), dmg / 10.0F);
            l.knockback(knock + 0.3, -out.x, -out.z);
         }
      }
   }

   @Override
   protected void readAdditionalSaveData(CompoundTag tag) {
      super.readAdditionalSaveData(tag);
      this.setVariant(tag.getInt("Variant"));
   }

   @Override
   protected void addAdditionalSaveData(CompoundTag tag) {
      super.addAdditionalSaveData(tag);
      tag.putInt("Variant", this.getVariant());
   }

   public static int clampVariant(int v) {
      return Mth.clamp(v, 0, VARIANTS.length - 1);
   }

   public static record Variant(String id, int mode, int primary, int secondary) {
      public float[] rgb(boolean primaryColour) {
         int c = primaryColour ? this.primary : this.secondary;
         return new float[]{(float)(c >> 16 & 0xFF) / 255.0F, (float)(c >> 8 & 0xFF) / 255.0F, (float)(c & 0xFF) / 255.0F};
      }
   }
}
