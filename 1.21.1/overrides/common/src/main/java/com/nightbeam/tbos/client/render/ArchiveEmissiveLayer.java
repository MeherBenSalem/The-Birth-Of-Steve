package com.nightbeam.tbos.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/**
 * Full-brightness overlay for the glowing cores of Archive creatures. One
 * parameterised layer rather than a near-identical subclass per mob.
 */
public final class ArchiveEmissiveLayer<T extends Entity, M extends EntityModel<T>>
        extends EyesLayer<T, M> {
    private final RenderType renderType;

    public ArchiveEmissiveLayer(RenderLayerParent<T, M> renderer, ResourceLocation texture) {
        super(renderer);
        renderType = RenderType.eyes(texture);
    }

    @Override
    public RenderType renderType() {
        return renderType;
    }
}
