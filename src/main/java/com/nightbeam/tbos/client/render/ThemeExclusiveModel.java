package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.entity.ThemeExclusiveKind;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * One theme-exclusive creature's baked silhouette.
 *
 * <p>Deliberately thin: the geometry lives in {@link ThemeExclusiveMeshes} and
 * the motion in {@link ThemeExclusivePosers}, keyed by silhouette. A renderer is
 * constructed per entity type, so each instance resolves exactly one kind's rig
 * once and reuses it every frame.
 */
public final class ThemeExclusiveModel extends EntityModel<ThemeExclusiveRenderState> {
    private final ThemeExclusiveSilhouettes.Rig rig;
    private final ThemeExclusiveSilhouettes.Poser poser;

    public ThemeExclusiveModel(ModelPart root, ThemeExclusiveKind kind) {
        super(root);
        this.rig = ThemeExclusiveSilhouettes.rig(kind.silhouette(), root);
        this.poser = ThemeExclusiveSilhouettes.poser(kind.silhouette());
    }

    @Override
    public void setupAnim(ThemeExclusiveRenderState state) {
        // Resets every part first; the posers then apply their motion as deltas.
        super.setupAnim(state);
        poser.pose(rig, state);
        if (state.finalBoss) {
            float scale = state.bossPhase >= 2 ? 1.20F : 1.16F;
            rig.root().xScale *= scale;
            rig.root().yScale *= scale;
            rig.root().zScale *= scale;
        }
    }
}
