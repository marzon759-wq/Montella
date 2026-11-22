package com.krolasyon.kaiju.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Destoroyah: everything specific lives in data/kaiju/kaiju/destoroyah.json and its model (wings, horn, pincer tail) */
public class DestoroyahEntity extends KaijuEntity {
    public DestoroyahEntity(EntityType<? extends KaijuEntity> type, Level level) {
        super(type, level, "destoroyah");
    }
}
