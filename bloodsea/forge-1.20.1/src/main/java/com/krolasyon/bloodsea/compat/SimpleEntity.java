package com.krolasyon.bloodsea.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** An entity with no synched data of its own (Forge 1.20.1). */
public abstract class SimpleEntity extends Entity {
    protected SimpleEntity(EntityType<?> type, Level level) { super(type, level); }

    @Override protected void defineSynchedData() {}
}
