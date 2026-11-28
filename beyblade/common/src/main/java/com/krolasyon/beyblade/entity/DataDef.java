package com.krolasyon.beyblade.entity;

import net.minecraft.network.syncher.EntityDataAccessor;

/** Version neutral way to define synched entity data (1.20.1 defines on the entity, 1.21.1 on a builder). */
@FunctionalInterface
public interface DataDef {
   <T> void define(EntityDataAccessor<T> key, T value);
}
