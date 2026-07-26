package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.HourCantorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native model-layer renderer for the Hour Cantor. */
public final class HourCantorRenderer
        extends MobRenderer<HourCantorEntity, HourCantorRenderState, HourCantorModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/hour_cantor.png");
    private static final Identifier CORE =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/hour_cantor_core.png");

    /** Matches the Cantor's own post-refrain cooldown, so the pendulum reads true. */
    private static final float CADENCE_TICKS = 80.0F;

    public HourCantorRenderer(EntityRendererProvider.Context context) {
        super(context, new HourCantorModel(context.bakeLayer(HourCantorModel.MODEL_LAYER)), 0.8F);
        addLayer(new ArchiveEmissiveLayer<>(this, CORE));
    }

    @Override
    public HourCantorRenderState createRenderState() {
        return new HourCantorRenderState();
    }

    @Override
    public void extractRenderState(
            HourCantorEntity entity,
            HourCantorRenderState state,
            float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.refrainPhase = entity.getRefrainPhase();
        state.refrainProgress = entity.getRefrainProgress(partialTick);
        state.cadence = 1.0F - Mth.clamp(entity.getRefrainCooldown() / CADENCE_TICKS, 0.0F, 1.0F);
        state.escalated = entity.isEscalated();
        state.attackTime = entity.getAttackAnim(partialTick);
        state.hurtTime = Mth.clamp((entity.hurtTime - partialTick) / 10.0F, 0.0F, 1.0F);
    }

    @Override
    public Identifier getTextureLocation(HourCantorRenderState state) {
        return TEXTURE;
    }
}
