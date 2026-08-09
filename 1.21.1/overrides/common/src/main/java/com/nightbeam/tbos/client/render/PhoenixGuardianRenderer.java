package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.PhoenixGuardianEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Native model-layer renderer for the Last Curator. */
public final class PhoenixGuardianRenderer
        extends MobRenderer<PhoenixGuardianEntity, PhoenixGuardianModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Yesterglass.MOD_ID, "textures/entity/phoenix_guardian.png");

    public PhoenixGuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new PhoenixGuardianModel(context.bakeLayer(PhoenixGuardianModel.MODEL_LAYER)), 1.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(PhoenixGuardianEntity entity) {
        return TEXTURE;
    }
}
