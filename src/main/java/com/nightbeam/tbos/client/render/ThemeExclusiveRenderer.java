package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.ThemeExclusiveEntity;
import com.nightbeam.tbos.entity.ThemeExclusiveKind;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Renders one theme-exclusive kind.
 *
 * <p>One instance exists per entity type, so the kind is constructor state: it
 * picks the silhouette's baked layer, its shadow radius, and the fixed base and
 * emissive textures. That is also why {@link ArchiveEmissiveLayer} needs no
 * per-entity lookup — a per-renderer texture is already a per-kind texture.
 */
public final class ThemeExclusiveRenderer
        extends MobRenderer<ThemeExclusiveEntity, ThemeExclusiveRenderState, ThemeExclusiveModel> {
    private final Identifier texture;

    public ThemeExclusiveRenderer(EntityRendererProvider.Context context, ThemeExclusiveKind kind) {
        super(
                context,
                new ThemeExclusiveModel(
                        context.bakeLayer(ThemeExclusiveSilhouettes.layer(kind.silhouette())), kind),
                ThemeExclusiveSilhouettes.shadowRadius(kind.silhouette()));
        this.texture = sheet(kind, "");
        addLayer(new ArchiveEmissiveLayer<>(this, sheet(kind, "_core")));
    }

    private static Identifier sheet(ThemeExclusiveKind kind, String suffix) {
        return Identifier.fromNamespaceAndPath(
                Yesterglass.MOD_ID, "textures/entity/" + kind.texturePath() + suffix + ".png");
    }

    @Override
    public ThemeExclusiveRenderState createRenderState() {
        return new ThemeExclusiveRenderState();
    }

    @Override
    public void extractRenderState(
            ThemeExclusiveEntity entity, ThemeExclusiveRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.abilityPhase = entity.getAbilityPhase();
        state.abilityProgress = entity.getAbilityProgress(partialTick);
        state.attackTime = entity.getAttackAnim(partialTick);
        state.hurtTime = Mth.clamp((entity.hurtTime - partialTick) / 10.0F, 0.0F, 1.0F);
        state.finalBoss = entity.isFinalBoss();
        state.bossPhase = entity.bossPhase();
    }

    @Override
    public Identifier getTextureLocation(ThemeExclusiveRenderState state) {
        return texture;
    }
}
