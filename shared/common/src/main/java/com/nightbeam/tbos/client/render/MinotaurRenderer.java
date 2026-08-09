package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.MinotaurEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native model-layer renderer for the chamber minotaur. */
public final class MinotaurRenderer
        extends MobRenderer<MinotaurEntity, MinotaurRenderState, MinotaurModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/minotaur.png");

    public MinotaurRenderer(EntityRendererProvider.Context context) {
        super(context, new MinotaurModel(context.bakeLayer(MinotaurModel.MODEL_LAYER)), 0.9F);
    }

    @Override
    public MinotaurRenderState createRenderState() {
        return new MinotaurRenderState();
    }

    @Override
    public void extractRenderState(
            MinotaurEntity entity,
            MinotaurRenderState state,
            float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.phase = entity.getPhase();
        state.phaseProgress = entity.getPhaseProgress(partialTick);
        state.attackTime = entity.getAttackAnim(partialTick);
        state.hurtTime = Mth.clamp((entity.hurtTime - partialTick) / 10.0F, 0.0F, 1.0F);
    }

    @Override
    public Identifier getTextureLocation(MinotaurRenderState state) {
        return TEXTURE;
    }
}
