package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.PhoenixGuardianEntity;

/**
 * Client-only values used by the Last Curator's procedural model animation.
 *
 * <p>This version has no vanilla render state, so the model builds one of these
 * itself in {@code setupAnim}. The field names match the 26.x
 * {@code LivingEntityRenderState} on purpose: it lets the authored animation
 * body in {@code tools/models/anim/phoenix_guardian.frag} be shared verbatim.
 */
public final class PhoenixGuardianRenderState {
    public PhoenixGuardianEntity.Phase phase = PhoenixGuardianEntity.Phase.IDLE;
    public float phaseProgress;
    public float attackTime;
    public float hurtTime;
    public float rebirth;
    public boolean risen;
    public float ageInTicks;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float yRot;
    public float xRot;
}
