package com.nightbeam.tbos.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.Level;

/** A quick Archive hunter whose silhouette slips between remembered positions. */
public final class ParallaxWraithEntity extends Zombie {
    public ParallaxWraithEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
        setCanBreakDoors(false);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }
}
