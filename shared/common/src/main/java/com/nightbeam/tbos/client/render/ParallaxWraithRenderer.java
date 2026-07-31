package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.ParallaxWraithEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Native model-layer renderer for the Parallax Wraith. */
public final class ParallaxWraithRenderer
        extends MobRenderer<ParallaxWraithEntity, ParallaxWraithRenderState, ParallaxWraithModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/parallax_wraith.png");
    private static final Identifier CORE =
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/parallax_wraith_core.png");

    public ParallaxWraithRenderer(EntityRendererProvider.Context context) {
        super(context, new ParallaxWraithModel(context.bakeLayer(ParallaxWraithModel.MODEL_LAYER)), 0.45F);
        addLayer(new ArchiveEmissiveLayer<>(this, CORE));
    }

    @Override
    public ParallaxWraithRenderState createRenderState() {
        return new ParallaxWraithRenderState();
    }

    @Override
    public void extractRenderState(
            ParallaxWraithEntity entity,
            ParallaxWraithRenderState state,
            float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.displacePhase = entity.getDisplacePhase();
        state.displaceProgress = entity.getDisplaceProgress(partialTick);
        state.attackTime = entity.getAttackAnim(partialTick);
        state.hurtTime = Mth.clamp((entity.hurtTime - partialTick) / 10.0F, 0.0F, 1.0F);
    }

    @Override
    public Identifier getTextureLocation(ParallaxWraithRenderState state) {
        return TEXTURE;
    }
}
