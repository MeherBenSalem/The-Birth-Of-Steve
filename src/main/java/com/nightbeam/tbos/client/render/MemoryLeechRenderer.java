package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.MemoryLeechEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native model-layer renderer for the Memory Leech. */
public final class MemoryLeechRenderer
        extends MobRenderer<MemoryLeechEntity, MemoryLeechRenderState, MemoryLeechModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/memory_leech.png");

    public MemoryLeechRenderer(EntityRendererProvider.Context context) {
        super(context, new MemoryLeechModel(context.bakeLayer(MemoryLeechModel.MODEL_LAYER)), 0.4F);
    }

    @Override
    public MemoryLeechRenderState createRenderState() {
        return new MemoryLeechRenderState();
    }

    @Override
    public void extractRenderState(
            MemoryLeechEntity entity,
            MemoryLeechRenderState state,
            float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.pouncePhase = entity.getPouncePhase();
        state.pounceProgress = entity.getPounceProgress(partialTick);
        state.attackTime = entity.getAttackAnim(partialTick);
        state.hurtTime = Mth.clamp((entity.hurtTime - partialTick) / 10.0F, 0.0F, 1.0F);
    }

    @Override
    public Identifier getTextureLocation(MemoryLeechRenderState state) {
        return TEXTURE;
    }
}
