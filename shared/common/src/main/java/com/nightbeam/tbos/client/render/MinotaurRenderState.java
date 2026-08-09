package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.MinotaurEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Client-only values used by the minotaur's procedural model animation. */
public final class MinotaurRenderState extends LivingEntityRenderState {
    public MinotaurEntity.Phase phase = MinotaurEntity.Phase.IDLE;
    /** Zero at the start of the current phase, one at its end. */
    public float phaseProgress;
    public float attackTime;
    public float hurtTime;
}
