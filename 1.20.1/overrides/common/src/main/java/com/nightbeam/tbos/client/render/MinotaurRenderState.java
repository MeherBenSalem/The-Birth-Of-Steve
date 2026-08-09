package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.MinotaurEntity;

/**
 * Client-only values used by the minotaur's procedural model animation.
 *
 * <p>Field names match the 26.x {@code LivingEntityRenderState} so the authored
 * animation body in {@code tools/models/anim/minotaur.frag} is shared verbatim.
 */
public final class MinotaurRenderState {
    public MinotaurEntity.Phase phase = MinotaurEntity.Phase.IDLE;
    public float phaseProgress;
    public float attackTime;
    public float hurtTime;
    public float ageInTicks;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float yRot;
    public float xRot;
}
