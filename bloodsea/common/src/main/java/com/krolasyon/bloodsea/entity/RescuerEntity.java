package com.krolasyon.bloodsea.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/** The fisherman who hauls Elias out of the grey water: the same body, oilskins instead of a sweater. */
public class RescuerEntity extends ActorEntity {
    public RescuerEntity(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

    @Override public String kind() { return "rescuer"; }
}
