package com.krolasyon.beyblade.compat;

import com.krolasyon.beyblade.entity.DataDef;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Entity base with version neutral synched data definition (Forge 1.21.1). */
public abstract class DataEntity extends Entity {
   protected DataEntity(EntityType<?> type, Level level) {
      super(type, level);
   }

   @Override
   protected final void defineSynchedData(SynchedEntityData.Builder b) {
      this.defineData(b::define);
   }

   protected abstract void defineData(DataDef d);

   private float stepHeight = 0.0F;

   protected void setStepHeight(float h) {
      this.stepHeight = h;
   }

   @Override
   public float maxUpStep() {
      return this.stepHeight;
   }
}
