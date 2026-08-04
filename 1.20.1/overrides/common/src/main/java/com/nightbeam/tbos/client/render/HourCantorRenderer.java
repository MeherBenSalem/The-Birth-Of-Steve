package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.HourCantorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Native model-layer renderer for the Hour Cantor. */
public final class HourCantorRenderer extends MobRenderer<HourCantorEntity, HourCantorModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Yesterglass.MOD_ID, "textures/entity/hour_cantor.png");
    private static final ResourceLocation CORE =
            new ResourceLocation(Yesterglass.MOD_ID, "textures/entity/hour_cantor_core.png");

    public HourCantorRenderer(EntityRendererProvider.Context context) {
        super(context, new HourCantorModel(context.bakeLayer(HourCantorModel.MODEL_LAYER)), 0.8F);
        addLayer(new ArchiveEmissiveLayer<>(this, CORE));
    }

    @Override
    public ResourceLocation getTextureLocation(HourCantorEntity entity) {
        return TEXTURE;
    }
}
