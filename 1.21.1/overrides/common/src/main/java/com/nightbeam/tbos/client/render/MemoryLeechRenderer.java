package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.MemoryLeechEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Native model-layer renderer for the Memory Leech. */
public final class MemoryLeechRenderer extends MobRenderer<MemoryLeechEntity, MemoryLeechModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/memory_leech.png");

    public MemoryLeechRenderer(EntityRendererProvider.Context context) {
        super(context, new MemoryLeechModel(context.bakeLayer(MemoryLeechModel.MODEL_LAYER)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(MemoryLeechEntity entity) {
        return TEXTURE;
    }
}
