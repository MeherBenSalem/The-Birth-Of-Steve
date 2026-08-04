package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.ThemeExclusiveEntity;
import com.nightbeam.tbos.entity.ThemeExclusiveKind;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Renders one theme-exclusive kind.
 *
 * <p>One instance exists per entity type, so the kind is constructor state: it
 * picks the silhouette's baked layer, its shadow radius, and the fixed base and
 * emissive textures. That is also why {@link ArchiveEmissiveLayer} needs no
 * per-entity lookup — a per-renderer texture is already a per-kind texture.
 */
public final class ThemeExclusiveRenderer extends MobRenderer<ThemeExclusiveEntity, ThemeExclusiveModel> {
    private final ResourceLocation texture;

    public ThemeExclusiveRenderer(EntityRendererProvider.Context context, ThemeExclusiveKind kind) {
        super(
                context,
                new ThemeExclusiveModel(
                        context.bakeLayer(ThemeExclusiveSilhouettes.layer(kind.silhouette())), kind),
                ThemeExclusiveSilhouettes.shadowRadius(kind.silhouette()));
        this.texture = sheet(kind, "");
        addLayer(new ArchiveEmissiveLayer<>(this, sheet(kind, "_core")));
    }

    private static ResourceLocation sheet(ThemeExclusiveKind kind, String suffix) {
        return new ResourceLocation(
                Yesterglass.MOD_ID, "textures/entity/" + kind.texturePath() + suffix + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(ThemeExclusiveEntity entity) {
        return texture;
    }
}
