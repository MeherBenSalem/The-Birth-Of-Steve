package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.ParallaxWraithEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Native model-layer renderer for the Parallax Wraith. */
public final class ParallaxWraithRenderer extends MobRenderer<ParallaxWraithEntity, ParallaxWraithModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/parallax_wraith.png");
    private static final ResourceLocation CORE =
            ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/parallax_wraith_core.png");

    public ParallaxWraithRenderer(EntityRendererProvider.Context context) {
        super(context, new ParallaxWraithModel(context.bakeLayer(ParallaxWraithModel.MODEL_LAYER)), 0.45F);
        addLayer(new ArchiveEmissiveLayer<>(this, CORE));
    }

    @Override
    public ResourceLocation getTextureLocation(ParallaxWraithEntity entity) {
        return TEXTURE;
    }
}
