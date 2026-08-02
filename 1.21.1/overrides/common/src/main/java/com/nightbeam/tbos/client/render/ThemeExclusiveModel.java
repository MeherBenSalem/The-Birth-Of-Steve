package com.nightbeam.tbos.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nightbeam.tbos.entity.ThemeExclusiveEntity;
import com.nightbeam.tbos.entity.ThemeExclusiveKind;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/** A 1.21.1 entity-model adapter for the same authored silhouettes and posers. */
public final class ThemeExclusiveModel extends EntityModel<ThemeExclusiveEntity> {
    private final ThemeExclusiveSilhouettes.Rig rig;
    private final ThemeExclusiveSilhouettes.Poser poser;

    public ThemeExclusiveModel(ModelPart root, ThemeExclusiveKind kind) {
        super(RenderType::entityCutoutNoCull);
        rig = ThemeExclusiveSilhouettes.rig(kind.silhouette(), root);
        poser = ThemeExclusiveSilhouettes.poser(kind.silhouette());
    }

    @Override
    public void setupAnim(
            ThemeExclusiveEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        rig.root().getAllParts().forEach(ModelPart::resetPose);
        ThemeExclusiveRenderState state = new ThemeExclusiveRenderState();
        state.ageInTicks = ageInTicks;
        state.walkAnimationPos = limbSwing;
        state.walkAnimationSpeed = limbSwingAmount;
        state.yRot = netHeadYaw;
        state.xRot = headPitch;
        state.abilityPhase = entity.getAbilityPhase();
        state.abilityProgress = entity.getAbilityProgress(ageInTicks - entity.tickCount);
        state.attackTime = attackTime;
        state.hurtTime = Mth.clamp(entity.hurtTime / 10.0F, 0.0F, 1.0F);
        state.finalBoss = entity.isFinalBoss();
        state.bossPhase = entity.bossPhase();
        state.texturePath = entity.kind().texturePath();
        poser.pose(rig, state);
        if (state.finalBoss) {
            float scale = state.bossPhase >= 2 ? 1.20F : 1.16F;
            rig.root().xScale *= scale;
            rig.root().yScale *= scale;
            rig.root().zScale *= scale;
        }
    }

    @Override
    public void renderToBuffer(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            int color) {
        rig.root().render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
