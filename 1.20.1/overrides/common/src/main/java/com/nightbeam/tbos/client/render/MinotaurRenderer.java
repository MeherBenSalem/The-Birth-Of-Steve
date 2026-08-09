package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.MinotaurEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Native model-layer renderer for the chamber minotaur. */
public final class MinotaurRenderer extends MobRenderer<MinotaurEntity, MinotaurModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Yesterglass.MOD_ID, "textures/entity/minotaur.png");

    public MinotaurRenderer(EntityRendererProvider.Context context) {
        super(context, new MinotaurModel(context.bakeLayer(MinotaurModel.MODEL_LAYER)), 0.9F);
    }

    @Override
    public ResourceLocation getTextureLocation(MinotaurEntity entity) {
        return TEXTURE;
    }
}
