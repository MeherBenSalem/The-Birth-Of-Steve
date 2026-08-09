package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.PhoenixGuardianEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native model-layer renderer for the Last Curator. */
public final class PhoenixGuardianRenderer
        extends MobRenderer<PhoenixGuardianEntity, PhoenixGuardianRenderState, PhoenixGuardianModel> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            Yesterglass.MOD_ID, "textures/entity/phoenix_guardian.png");

    public PhoenixGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new PhoenixGuardianModel(context.bakeLayer(PhoenixGuardianModel.MODEL_LAYER)), 1.2F);
    }

    @Override
    public PhoenixGuardianRenderState createRenderState() {
        return new PhoenixGuardianRenderState();
    }

    @Override
    public void extractRenderState(
            PhoenixGuardianEntity entity,
            PhoenixGuardianRenderState state,
            float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.phase = entity.getPhase();
        state.phaseProgress = entity.getPhaseProgress(partialTick);
        state.rebirth = entity.getRebirthProgress(partialTick);
        state.risen = entity.hasRisen();
        state.attackTime = entity.getAttackAnim(partialTick);
        state.hurtTime = Mth.clamp((entity.hurtTime - partialTick) / 10.0F, 0.0F, 1.0F);
    }

    @Override
    public Identifier getTextureLocation(PhoenixGuardianRenderState state) {
        return TEXTURE;
    }
}
