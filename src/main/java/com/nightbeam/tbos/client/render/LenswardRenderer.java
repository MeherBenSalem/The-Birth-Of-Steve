package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.LenswardEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class LenswardRenderer
        extends MobRenderer<LenswardEntity, LenswardRenderState, LenswardModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/lensward.png");

    public LenswardRenderer(EntityRendererProvider.Context context) {
        super(context, new LenswardModel(context.bakeLayer(LenswardModel.MODEL_LAYER)), 0.45F);
        addLayer(new LenswardCoreLayer(this));
    }

    @Override
    public LenswardRenderState createRenderState() {
        return new LenswardRenderState();
    }

    @Override
    public void extractRenderState(
            LenswardEntity entity,
            LenswardRenderState state,
            float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.beamPhase = entity.getBeamPhase();
        state.beamProgress = entity.getBeamProgress(partialTick);
        state.hurtTime = Mth.clamp((entity.hurtTime - partialTick) / 10.0F, 0.0F, 1.0F);
    }

    @Override
    public Identifier getTextureLocation(LenswardRenderState state) {
        return TEXTURE;
    }
}
