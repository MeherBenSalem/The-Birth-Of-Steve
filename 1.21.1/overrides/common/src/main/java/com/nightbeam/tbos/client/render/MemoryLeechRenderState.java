package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.MemoryLeechEntity;

/** Values sampled by the 1.21.1 renderer before its model is posed. */
public final class MemoryLeechRenderState {
    public MemoryLeechEntity.PouncePhase pouncePhase = MemoryLeechEntity.PouncePhase.IDLE;
    public float pounceProgress;
    public float attackTime;
    public float hurtTime;
    public float ageInTicks;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float yRot;
    public float xRot;
}
