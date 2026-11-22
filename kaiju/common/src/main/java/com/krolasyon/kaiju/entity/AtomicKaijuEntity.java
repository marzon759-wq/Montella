package com.krolasyon.kaiju.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** the atomic kaiju: everything specific to it lives in data/kaiju/kaiju/atomic_kaiju.json and its model */
public class AtomicKaijuEntity extends KaijuEntity {
    public AtomicKaijuEntity(EntityType<? extends KaijuEntity> type, Level level) {
        super(type, level, "atomic_kaiju");
    }
}
