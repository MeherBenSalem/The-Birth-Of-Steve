package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.MeridianSentinelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Native model-layer renderer for the Meridian Sentinel. */
public final class MeridianSentinelRenderer extends MobRenderer<MeridianSentinelEntity, MeridianSentinelModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/meridian_sentinel.png");
    private static final ResourceLocation CORE =
            ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/meridian_sentinel_core.png");

    public MeridianSentinelRenderer(EntityRendererProvider.Context context) {
        super(context, new MeridianSentinelModel(context.bakeLayer(MeridianSentinelModel.MODEL_LAYER)), 0.55F);
        addLayer(new ArchiveEmissiveLayer<>(this, CORE));
    }

    @Override
    public ResourceLocation getTextureLocation(MeridianSentinelEntity entity) {
        return TEXTURE;
    }
}
