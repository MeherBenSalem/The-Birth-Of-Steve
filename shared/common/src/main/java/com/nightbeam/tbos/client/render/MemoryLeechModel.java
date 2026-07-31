package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Low, faceted Archive parasite with independently articulated plates,
 * mandibles, limbs, and memory tendrils.
 */
public final class MemoryLeechModel extends EntityModel<MemoryLeechRenderState> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "memory_leech"),
            "main");

    private final ModelPart rootPart;
    private final ModelPart body;
    private final ModelPart carapace;
    private final ModelPart head;
    private final ModelPart leftMandible;
    private final ModelPart rightMandible;
    private final ModelPart leftFrontLimb;
    private final ModelPart rightFrontLimb;
    private final ModelPart leftRearLimb;
    private final ModelPart rightRearLimb;
    private final ModelPart leftTendril;
    private final ModelPart rightTendril;
    private final ModelPart memoryCore;

    public MemoryLeechModel(ModelPart root) {
        super(root);
        rootPart = root;
        body = root.getChild("body");
        carapace = body.getChild("carapace");
        head = body.getChild("head");
        leftMandible = head.getChild("left_mandible");
        rightMandible = head.getChild("right_mandible");
        leftFrontLimb = body.getChild("left_front_limb");
        rightFrontLimb = body.getChild("right_front_limb");
        leftRearLimb = body.getChild("left_rear_limb");
        rightRearLimb = body.getChild("right_rear_limb");
        leftTendril = body.getChild("left_tendril");
        rightTendril = body.getChild("right_tendril");
        memoryCore = body.getChild("memory_core");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-5.0F, -3.0F, -5.0F, 10.0F, 6.0F, 10.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));

        body.addOrReplaceChild(
                "carapace",
                CubeListBuilder.create()
                        .texOffs(0, 26)
                        .addBox(-4.0F, -1.0F, -3.5F, 8.0F, 2.0F, 7.0F),
                PartPose.offset(0.0F, -3.3F, 0.7F));
        body.addOrReplaceChild(
                "memory_core",
                CubeListBuilder.create()
                        .texOffs(32, 42)
                        .addBox(-2.0F, -1.0F, -2.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, -4.1F, 0.7F));

        PartDefinition head = body.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(32, 26)
                        .addBox(-4.0F, -2.0F, -3.0F, 8.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 0.2F, -4.5F));
        head.addOrReplaceChild(
                "left_mandible",
                CubeListBuilder.create()
                        .texOffs(48, 38)
                        .addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(2.2F, 1.0F, -2.0F, 0.0F, -0.22F, 0.08F));
        head.addOrReplaceChild(
                "right_mandible",
                CubeListBuilder.create()
                        .texOffs(48, 38)
                        .mirror()
                        .addBox(-1.0F, -1.0F, -4.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offsetAndRotation(-2.2F, 1.0F, -2.0F, 0.0F, 0.22F, -0.08F));

        CubeListBuilder leftLimb = CubeListBuilder.create()
                .texOffs(0, 38)
                .addBox(0.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F);
        CubeListBuilder rightLimb = CubeListBuilder.create()
                .texOffs(0, 38)
                .mirror()
                .addBox(-6.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F);
        body.addOrReplaceChild(
                "left_front_limb",
                leftLimb,
                PartPose.offsetAndRotation(4.0F, 1.7F, -2.7F, 0.0F, -0.55F, 0.42F));
        body.addOrReplaceChild(
                "right_front_limb",
                rightLimb,
                PartPose.offsetAndRotation(-4.0F, 1.7F, -2.7F, 0.0F, 0.55F, -0.42F));
        body.addOrReplaceChild(
                "left_rear_limb",
                leftLimb,
                PartPose.offsetAndRotation(4.0F, 1.9F, 2.7F, 0.0F, 0.45F, 0.52F));
        body.addOrReplaceChild(
                "right_rear_limb",
                rightLimb,
                PartPose.offsetAndRotation(-4.0F, 1.9F, 2.7F, 0.0F, -0.45F, -0.52F));

        CubeListBuilder tendril = CubeListBuilder.create()
                .texOffs(16, 38)
                .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F);
        body.addOrReplaceChild(
                "left_tendril",
                tendril,
                PartPose.offsetAndRotation(2.2F, 0.4F, 4.3F, 0.0F, 0.28F, 0.08F));
        body.addOrReplaceChild(
                "right_tendril",
                CubeListBuilder.create()
                        .texOffs(16, 38)
                        .mirror()
                        .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F),
                PartPose.offsetAndRotation(-2.2F, 0.4F, 4.3F, 0.0F, -0.28F, -0.08F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MemoryLeechRenderState state) {
        super.setupAnim(state);

        float idle = Mth.sin(state.ageInTicks * 0.12F);
        float walkPosition = state.walkAnimationPos * 1.8F;
        float walkSpeed = Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
        float opposingWalk = Mth.cos(walkPosition) * 0.42F * walkSpeed;
        float step = Math.abs(Mth.sin(walkPosition)) * 0.18F * walkSpeed;

        rootPart.y += idle * 0.4F;
        body.zRot += idle * 0.035F;
        carapace.zRot -= idle * 0.025F;
        memoryCore.y += Mth.sin(state.ageInTicks * 0.18F) * 0.16F;
        head.yRot += state.yRot * (float) (Math.PI / 360.0);
        head.xRot += state.xRot * (float) (Math.PI / 540.0);

        leftFrontLimb.yRot += opposingWalk;
        rightFrontLimb.yRot -= opposingWalk;
        leftRearLimb.yRot -= opposingWalk;
        rightRearLimb.yRot += opposingWalk;
        leftFrontLimb.zRot += step;
        rightFrontLimb.zRot -= step;
        leftRearLimb.zRot += step;
        rightRearLimb.zRot -= step;
        leftTendril.yRot += Mth.sin(state.ageInTicks * 0.16F) * 0.18F + opposingWalk * 0.35F;
        rightTendril.yRot -= Mth.sin(state.ageInTicks * 0.16F + 0.8F) * 0.18F + opposingWalk * 0.35F;

        float bite = Mth.sin(state.attackTime * (float) Math.PI);
        leftMandible.yRot -= bite * 0.55F;
        rightMandible.yRot += bite * 0.55F;
        head.xRot += bite * 0.12F;

        float recoil = Mth.sin(state.hurtTime * (float) Math.PI);
        body.zRot += recoil * 0.16F;
        carapace.xRot -= recoil * 0.1F;

        switch (state.pouncePhase) {
            case IDLE -> {
            }
            case WINDUP -> {
                rootPart.y += state.pounceProgress * 1.6F;
                body.xRot -= state.pounceProgress * 0.38F;
                leftMandible.yRot -= state.pounceProgress * 0.3F;
                rightMandible.yRot += state.pounceProgress * 0.3F;
                leftFrontLimb.zRot += state.pounceProgress * 0.35F;
                rightFrontLimb.zRot -= state.pounceProgress * 0.35F;
            }
            case AIRBORNE -> {
                body.xRot += 0.52F;
                leftFrontLimb.zRot += 0.48F;
                rightFrontLimb.zRot -= 0.48F;
                leftRearLimb.zRot += 0.35F;
                rightRearLimb.zRot -= 0.35F;
                leftTendril.xRot -= 0.3F;
                rightTendril.xRot -= 0.3F;
                leftMandible.yRot -= 0.28F;
                rightMandible.yRot += 0.28F;
            }
            case RECOVERY -> {
                float recovery = Mth.sin(state.pounceProgress * (float) Math.PI);
                body.xRot -= recovery * 0.26F;
                rootPart.y += recovery * 0.8F;
                leftTendril.yRot += recovery * 0.2F;
                rightTendril.yRot -= recovery * 0.2F;
            }
        }
    }
}
