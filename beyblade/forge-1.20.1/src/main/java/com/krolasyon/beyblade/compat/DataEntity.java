package com.krolasyon.beyblade.compat;

import com.krolasyon.beyblade.entity.DataDef;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Entity base with version neutral synched data definition (Forge 1.20.1). */
public abstract class DataEntity extends Entity {
   protected DataEntity(EntityType<?> type, Level level) {
      super(type, level);
   }

   @Override
   protected final void defineSynchedData() {
      this.defineData(this.entityData::define);
   }

   protected abstract void defineData(DataDef d);

   protected void setStepHeight(float h) {
      this.setMaxUpStep(h);
   }
}
