package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.ThemeExclusiveEntity;

public final class ThemeExclusiveRenderState {
    public ThemeExclusiveEntity.AbilityPhase abilityPhase = ThemeExclusiveEntity.AbilityPhase.IDLE;
    public float abilityProgress;
    public float attackTime;
    public float hurtTime;
    public boolean finalBoss;
    public int bossPhase;
    public String texturePath = "shard_drifter";
    public float ageInTicks;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float yRot;
    public float xRot;
}
