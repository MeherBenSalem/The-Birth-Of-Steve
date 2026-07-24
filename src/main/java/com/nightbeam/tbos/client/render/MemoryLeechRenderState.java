package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.MemoryLeechEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Client-only values used by the Memory Leech's procedural model animation. */
public final class MemoryLeechRenderState extends LivingEntityRenderState {
    public MemoryLeechEntity.PouncePhase pouncePhase = MemoryLeechEntity.PouncePhase.IDLE;
    public float pounceProgress;
    public float attackTime;
    public float hurtTime;
}
