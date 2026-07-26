package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.ParallaxWraithEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Client-only values used by the Parallax Wraith's procedural model animation. */
public final class ParallaxWraithRenderState extends LivingEntityRenderState {
    public ParallaxWraithEntity.DisplacePhase displacePhase = ParallaxWraithEntity.DisplacePhase.IDLE;
    public float displaceProgress;
    public float attackTime;
    public float hurtTime;
}
