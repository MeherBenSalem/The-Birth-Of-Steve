package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.MeridianSentinelEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Client-only values used by the Meridian Sentinel's procedural model animation. */
public final class MeridianSentinelRenderState extends LivingEntityRenderState {
    public MeridianSentinelEntity.SlamPhase slamPhase = MeridianSentinelEntity.SlamPhase.IDLE;
    public float slamProgress;
    public float attackTime;
    public float hurtTime;
}
