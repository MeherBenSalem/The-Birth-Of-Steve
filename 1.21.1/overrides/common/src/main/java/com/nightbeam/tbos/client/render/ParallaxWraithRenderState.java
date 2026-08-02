package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.ParallaxWraithEntity;

public final class ParallaxWraithRenderState {
    public ParallaxWraithEntity.DisplacePhase displacePhase = ParallaxWraithEntity.DisplacePhase.IDLE;
    public float displaceProgress;
    public float attackTime;
    public float hurtTime;
    public float ageInTicks;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float yRot;
    public float xRot;
}
