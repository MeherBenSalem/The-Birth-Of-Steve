package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.LenswardEntity;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public final class LenswardRenderState extends LivingEntityRenderState {
    public LenswardEntity.BeamPhase beamPhase = LenswardEntity.BeamPhase.IDLE;
    public float beamProgress;
    public float hurtTime;
}
