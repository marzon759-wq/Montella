package com.krolasyon.titans.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Korgath: a basalt-skinned reptile titan with magma crystals down its back and a crystal tail club. */
public class KorgathEntity extends TitanEntity {
    public KorgathEntity(EntityType<? extends KorgathEntity> type, Level level) { super(type, level, false); }
}
