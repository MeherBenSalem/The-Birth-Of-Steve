package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Shared silhouette set for floor-theme exclusive Archive creatures. */
public final class ThemeExclusiveModel extends EntityModel<ThemeExclusiveRenderState> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "theme_exclusive"),
            "main");

    private final ModelPart rootPart;
    private final ModelPart body;
    private final ModelPart accent;

    public ThemeExclusiveModel(ModelPart root) {
        super(root);
        rootPart = root;
        body = root.getChild("body");
        accent = body.getChild("accent");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -3.0F, 8.0F, 10.0F, 6.0F, CubeDeformation.NONE, ArchiveEntityModels.TEX_SCALE, ArchiveEntityModels.TEX_SCALE),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        body.addOrReplaceChild(
                "accent",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-5.0F, -10.0F, -1.0F, 10.0F, 4.0F, 2.0F, CubeDeformation.NONE, ArchiveEntityModels.TEX_SCALE, ArchiveEntityModels.TEX_SCALE)
                        .texOffs(0, 20)
                        .addBox(-2.0F, -12.0F, -2.0F, 4.0F, 3.0F, 4.0F, CubeDeformation.NONE, ArchiveEntityModels.TEX_SCALE, ArchiveEntityModels.TEX_SCALE)
                        .texOffs(32, 16)
                        .addBox(-6.0F, -3.0F, 2.0F, 3.0F, 8.0F, 2.0F, CubeDeformation.NONE, ArchiveEntityModels.TEX_SCALE, ArchiveEntityModels.TEX_SCALE)
                        .texOffs(48, 16)
                        .addBox(3.0F, -3.0F, 2.0F, 3.0F, 8.0F, 2.0F, CubeDeformation.NONE, ArchiveEntityModels.TEX_SCALE, ArchiveEntityModels.TEX_SCALE),
                PartPose.ZERO);
        body.addOrReplaceChild(
                "limb_left",
                CubeListBuilder.create()
                        .texOffs(16, 32)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F, CubeDeformation.NONE, ArchiveEntityModels.TEX_SCALE, ArchiveEntityModels.TEX_SCALE),
                PartPose.offset(-2.5F, 2.0F, 0.0F));
        body.addOrReplaceChild(
                "limb_right",
                CubeListBuilder.create()
                        .texOffs(32, 32)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 8.0F, 3.0F, CubeDeformation.NONE, ArchiveEntityModels.TEX_SCALE, ArchiveEntityModels.TEX_SCALE),
                PartPose.offset(2.5F, 2.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(ThemeExclusiveRenderState state) {
        rootPart.getAllParts().forEach(ModelPart::resetPose);
        float walk = state.walkAnimationPos;
        float walkSpeed = state.walkAnimationSpeed;
        ModelPart left = body.getChild("limb_left");
        ModelPart right = body.getChild("limb_right");
        left.xRot = Mth.cos(walk * 0.6662F) * 1.2F * walkSpeed;
        right.xRot = Mth.cos(walk * 0.6662F + (float) Math.PI) * 1.2F * walkSpeed;
        float phase = state.abilityProgress;
        switch (state.abilityPhase) {
            case WINDUP -> {
                accent.yRot = phase * 0.8F;
                body.xRot = -0.15F * phase;
            }
            case ACTIVE -> {
                accent.yRot = 1.2F + phase;
                body.yRot = Mth.sin(phase * (float) Math.PI) * 0.4F;
                left.zRot = -0.5F;
                right.zRot = 0.5F;
            }
            case RECOVERY -> {
                accent.yRot = (1.0F - phase) * 0.5F;
                body.xRot = 0.1F * (1.0F - phase);
            }
            case IDLE -> {
            }
        }
        if (state.hurtTime > 0.0F) {
            body.zRot = Mth.sin(state.hurtTime * (float) Math.PI) * 0.12F;
        }
    }
}
