package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.ThemeExclusiveEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class ThemeExclusiveRenderState extends LivingEntityRenderState {
    public ThemeExclusiveEntity.AbilityPhase abilityPhase = ThemeExclusiveEntity.AbilityPhase.IDLE;
    public float abilityProgress;
    public float attackTime;
    public float hurtTime;
    public String texturePath = "shard_drifter";
}
