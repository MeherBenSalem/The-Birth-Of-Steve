package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.MeridianSentinelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native model-layer renderer for the Meridian Sentinel. */
public final class MeridianSentinelRenderer
        extends MobRenderer<MeridianSentinelEntity, MeridianSentinelRenderState, MeridianSentinelModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/meridian_sentinel.png");
    private static final Identifier CORE =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/meridian_sentinel_core.png");

    public MeridianSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new MeridianSentinelModel(context.bakeLayer(MeridianSentinelModel.MODEL_LAYER)), 0.55F);
        addLayer(new ArchiveEmissiveLayer<>(this, CORE));
    }

    @Override
    public MeridianSentinelRenderState createRenderState() {
        return new MeridianSentinelRenderState();
    }

    @Override
    public void extractRenderState(
            MeridianSentinelEntity entity,
            MeridianSentinelRenderState state,
            float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.slamPhase = entity.getSlamPhase();
        state.slamProgress = entity.getSlamProgress(partialTick);
        state.attackTime = entity.getAttackAnim(partialTick);
        state.hurtTime = Mth.clamp((entity.hurtTime - partialTick) / 10.0F, 0.0F, 1.0F);
    }

    @Override
    public Identifier getTextureLocation(MeridianSentinelRenderState state) {
        return TEXTURE;
    }
}
