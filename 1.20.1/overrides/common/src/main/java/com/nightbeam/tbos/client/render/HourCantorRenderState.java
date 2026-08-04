package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.HourCantorEntity;

public final class HourCantorRenderState {
    public HourCantorEntity.RefrainPhase refrainPhase = HourCantorEntity.RefrainPhase.IDLE;
    public float refrainProgress;
    public float cadence;
    public boolean escalated;
    public float attackTime;
    public float hurtTime;
    public float ageInTicks;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float yRot;
    public float xRot;
}
