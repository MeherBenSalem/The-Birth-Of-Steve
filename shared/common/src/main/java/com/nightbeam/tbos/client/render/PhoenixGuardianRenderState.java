package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.PhoenixGuardianEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Client-only values used by the Last Curator's procedural model animation. */
public final class PhoenixGuardianRenderState extends LivingEntityRenderState {
    public PhoenixGuardianEntity.Phase phase = PhoenixGuardianEntity.Phase.IDLE;
    /** Zero at the start of the current phase, one at its end. */
    public float phaseProgress;
    public float attackTime;
    public float hurtTime;
    /** Zero outside the rebirth, rising to one across it. Drives the curl-and-flare. */
    public float rebirth;
    public boolean risen;
}
