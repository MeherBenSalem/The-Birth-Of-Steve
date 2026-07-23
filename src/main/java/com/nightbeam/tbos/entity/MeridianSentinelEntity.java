package com.nightbeam.tbos.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/** A heavy Archive defender used for guardian and puzzle-wave encounters. */
public final class MeridianSentinelEntity extends Zombie {
    public MeridianSentinelEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
        setCanBreakDoors(false);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }
}
