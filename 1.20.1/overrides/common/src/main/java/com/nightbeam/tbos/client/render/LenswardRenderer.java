package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.LenswardEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class LenswardRenderer extends MobRenderer<LenswardEntity, LenswardModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Yesterglass.MOD_ID, "textures/entity/lensward.png");
    private static final ResourceLocation CORE =
            new ResourceLocation(Yesterglass.MOD_ID, "textures/entity/lensward_core.png");

    public LenswardRenderer(EntityRendererProvider.Context context) {
        super(context, new LenswardModel(context.bakeLayer(LenswardModel.MODEL_LAYER)), 0.45F);
        addLayer(new ArchiveEmissiveLayer<>(this, CORE));
    }

    @Override
    public ResourceLocation getTextureLocation(LenswardEntity entity) {
        return TEXTURE;
    }
}
