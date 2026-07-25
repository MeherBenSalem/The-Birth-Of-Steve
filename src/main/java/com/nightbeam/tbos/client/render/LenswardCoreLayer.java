package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

public final class LenswardCoreLayer extends EyesLayer<LenswardRenderState, LenswardModel> {
    private static final RenderType CORE = RenderTypes.eyes(
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/lensward_core.png"));

    public LenswardCoreLayer(RenderLayerParent<LenswardRenderState, LenswardModel> renderer) {
        super(renderer);
    }

    @Override
    public RenderType renderType() {
        return CORE;
    }
}
