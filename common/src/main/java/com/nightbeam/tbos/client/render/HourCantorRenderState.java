package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.HourCantorEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Client-only values used by the Hour Cantor's procedural model animation. */
public final class HourCantorRenderState extends LivingEntityRenderState {
    public HourCantorEntity.RefrainPhase refrainPhase = HourCantorEntity.RefrainPhase.IDLE;
    public float refrainProgress;
    /** Zero just after a refrain, one when the next is due. Drives the pendulum. */
    public float cadence;
    public boolean escalated;
    public float attackTime;
    public float hurtTime;
}
