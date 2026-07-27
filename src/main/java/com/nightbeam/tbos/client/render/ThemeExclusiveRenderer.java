package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.ThemeExclusiveEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class ThemeExclusiveRenderer
        extends MobRenderer<ThemeExclusiveEntity, ThemeExclusiveRenderState, ThemeExclusiveModel> {
    public ThemeExclusiveRenderer(EntityRendererProvider.Context context) {
        super(context, new ThemeExclusiveModel(context.bakeLayer(ThemeExclusiveModel.MODEL_LAYER)), 0.45F);
    }

    @Override
    public ThemeExclusiveRenderState createRenderState() {
        return new ThemeExclusiveRenderState();
    }

    @Override
    public void extractRenderState(
            ThemeExclusiveEntity entity, ThemeExclusiveRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.abilityPhase = entity.getAbilityPhase();
        state.abilityProgress = entity.getAbilityProgress(partialTick);
        state.attackTime = entity.getAttackAnim(partialTick);
        state.hurtTime = Mth.clamp((entity.hurtTime - partialTick) / 10.0F, 0.0F, 1.0F);
        state.texturePath = entity.kind().texturePath();
    }

    @Override
    public Identifier getTextureLocation(ThemeExclusiveRenderState state) {
        return Identifier.fromNamespaceAndPath(
                Yesterglass.MOD_ID, "textures/entity/" + state.texturePath + ".png");
    }
}
