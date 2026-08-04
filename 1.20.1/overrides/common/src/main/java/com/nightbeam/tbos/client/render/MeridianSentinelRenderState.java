package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.MeridianSentinelEntity;

public final class MeridianSentinelRenderState {
    public MeridianSentinelEntity.SlamPhase slamPhase = MeridianSentinelEntity.SlamPhase.IDLE;
    public float slamProgress;
    public float attackTime;
    public float hurtTime;
    public float ageInTicks;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float yRot;
    public float xRot;
}
