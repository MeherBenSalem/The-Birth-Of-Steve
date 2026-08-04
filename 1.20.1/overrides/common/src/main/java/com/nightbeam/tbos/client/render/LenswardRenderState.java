package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.LenswardEntity;

public final class LenswardRenderState {
    public LenswardEntity.BeamPhase beamPhase = LenswardEntity.BeamPhase.IDLE;
    public float beamProgress;
    public float hurtTime;
    public float ageInTicks;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float yRot;
    public float xRot;
}
