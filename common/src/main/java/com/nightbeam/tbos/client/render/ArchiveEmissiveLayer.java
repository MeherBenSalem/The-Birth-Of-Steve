package com.nightbeam.tbos.client.render;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Full-brightness overlay for the glowing cores of Archive creatures. One
 * parameterised layer rather than a near-identical subclass per mob.
 */
public final class ArchiveEmissiveLayer<S extends LivingEntityRenderState, M extends EntityModel<S>>
        extends EyesLayer<S, M> {
    private final RenderType renderType;

    public ArchiveEmissiveLayer(RenderLayerParent<S, M> renderer, Identifier texture) {
        super(renderer);
        renderType = RenderTypes.eyes(texture);
    }

    @Override
    public RenderType renderType() {
        return renderType;
    }
}
