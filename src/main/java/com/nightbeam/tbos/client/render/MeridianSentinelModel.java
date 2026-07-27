package com.nightbeam.tbos.client.render;

import static com.nightbeam.tbos.client.render.ArchiveEntityModels.TEX_SCALE;

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

/**
 * A headless bronze golem. Two counter-rotating armillary rings sit in the
 * shoulder gap where a head would be, around a glowing gnomon.
 */
public final class MeridianSentinelModel extends EntityModel<MeridianSentinelRenderState> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "meridian_sentinel"),
            "main");

    private static final float OUTER_TILT = 0.35F;
    private static final float INNER_TILT = 0.55F;

    private final ModelPart rootPart;
    private final ModelPart torso;
    private final ModelPart gearColumn;
    private final ModelPart ringMount;
    private final ModelPart ringOuter;
    private final ModelPart ringInner;
    private final ModelPart gnomon;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public MeridianSentinelModel(ModelPart root) {
        super(root);
        rootPart = root;
        torso = root.getChild("torso");
        gearColumn = torso.getChild("gear_column");
        ringMount = torso.getChild("ring_mount");
        ringOuter = ringMount.getChild("ring_outer");
        ringInner = ringMount.getChild("ring_inner");
        gnomon = ringMount.getChild("gnomon");
        leftArm = torso.getChild("left_arm");
        rightArm = torso.getChild("right_arm");
        leftLeg = root.getChild("left_leg");
        rightLeg = root.getChild("right_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        CubeListBuilder leg = CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.5F, 0.0F, -2.5F, 5.0F, 10.0F, 5.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE);
        root.addOrReplaceChild(
                "left_leg", leg, PartPose.offsetAndRotation(3.5F, 14.0F, 0.0F, 0.0F, 0.0F, 0.09F));
        root.addOrReplaceChild(
                "right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 10.0F, 5.0F,
                                CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.offsetAndRotation(-3.5F, 14.0F, 0.0F, 0.0F, 0.0F, -0.09F));

        PartDefinition torso = root.addOrReplaceChild(
                "torso",
                CubeListBuilder.create()
                        .texOffs(22, 0)
                        .addBox(-6.0F, -12.0F, -4.0F, 12.0F, 12.0F, 8.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.offset(0.0F, 13.0F, 0.0F));
        torso.addOrReplaceChild(
                "gear_column",
                CubeListBuilder.create()
                        .texOffs(0, 21)
                        .addBox(-3.0F, -4.0F, -0.5F, 6.0F, 8.0F, 1.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.offset(0.0F, -6.0F, -4.5F));

        // The ring set clears the torso's top face rather than sinking into it.
        PartDefinition ringMount = torso.addOrReplaceChild(
                "ring_mount", CubeListBuilder.create(), PartPose.offset(0.0F, -14.5F, 0.0F));
        ringMount.addOrReplaceChild(
                "ring_outer",
                CubeListBuilder.create()
                        .texOffs(30, 37)
                        .addBox(-6.0F, -0.5F, -7.0F, 12.0F, 1.0F, 1.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                        .texOffs(30, 37)
                        .addBox(-6.0F, -0.5F, 6.0F, 12.0F, 1.0F, 1.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                        .texOffs(30, 40)
                        .addBox(-7.0F, -0.5F, -6.0F, 1.0F, 1.0F, 12.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                        .texOffs(30, 40)
                        .addBox(6.0F, -0.5F, -6.0F, 1.0F, 1.0F, 12.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.rotation(OUTER_TILT, 0.0F, 0.0F));
        ringMount.addOrReplaceChild(
                "ring_inner",
                CubeListBuilder.create()
                        .texOffs(0, 47)
                        .addBox(-4.0F, -0.5F, -5.0F, 8.0F, 1.0F, 1.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                        .texOffs(0, 47)
                        .addBox(-4.0F, -0.5F, 4.0F, 8.0F, 1.0F, 1.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                        .texOffs(0, 50)
                        .addBox(-5.0F, -0.5F, -4.0F, 1.0F, 1.0F, 8.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                        .texOffs(0, 50)
                        .addBox(4.0F, -0.5F, -4.0F, 1.0F, 1.0F, 8.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.rotation(0.0F, 0.0F, INNER_TILT));
        ringMount.addOrReplaceChild(
                "gnomon",
                CubeListBuilder.create()
                        .texOffs(16, 21)
                        .addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.ZERO);

        // Maul heads are hung outboard on each shaft, clear of the torso and legs.
        CubeListBuilder leftMaul = CubeListBuilder.create()
                .texOffs(30, 21)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                .texOffs(0, 32)
                .addBox(-2.0F, 10.0F, -3.5F, 7.0F, 6.0F, 7.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE);
        CubeListBuilder rightMaul = CubeListBuilder.create()
                .texOffs(30, 21)
                .mirror()
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 10.0F, 4.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                .texOffs(0, 32)
                .mirror()
                .addBox(-5.0F, 10.0F, -3.5F, 7.0F, 6.0F, 7.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE);
        torso.addOrReplaceChild(
                "left_arm", leftMaul, PartPose.offsetAndRotation(8.0F, -10.0F, 0.0F, 0.0F, 0.0F, -0.08F));
        torso.addOrReplaceChild(
                "right_arm", rightMaul, PartPose.offsetAndRotation(-8.0F, -10.0F, 0.0F, 0.0F, 0.0F, 0.08F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(MeridianSentinelRenderState state) {
        super.setupAnim(state);

        float idle = Mth.sin(state.ageInTicks * 0.07F);
        float walk = Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
        float stride = Mth.cos(state.walkAnimationPos * 0.6F) * walk;
        float footfall = Math.abs(Mth.sin(state.walkAnimationPos * 0.6F)) * walk;

        leftLeg.xRot += stride * 0.85F;
        rightLeg.xRot -= stride * 0.85F;
        rootPart.y += footfall * 0.9F;
        torso.xRot += footfall * 0.05F;
        torso.zRot += stride * 0.03F;

        // The rings never track the target; they keep their own time.
        ringOuter.yRot -= state.ageInTicks * 0.035F;
        ringInner.yRot += state.ageInTicks * 0.09F;
        ringMount.yRot += state.yRot * (float) (Math.PI / 540.0);
        gnomon.y += idle * 0.35F;
        gnomon.yRot += state.ageInTicks * 0.12F;
        gearColumn.xRot += idle * 0.02F;

        leftArm.xRot += stride * 0.3F + idle * 0.04F;
        rightArm.xRot -= stride * 0.3F - idle * 0.04F;

        float swing = Mth.sin(state.attackTime * (float) Math.PI);
        leftArm.xRot -= swing * 1.1F;
        rightArm.xRot -= swing * 1.1F;
        torso.xRot += swing * 0.12F;

        float recoil = Mth.sin(state.hurtTime * (float) Math.PI);
        torso.xRot -= recoil * 0.18F;
        leftArm.zRot -= recoil * 0.35F;
        rightArm.zRot += recoil * 0.35F;
        ringOuter.xRot += recoil * 0.4F;
        ringInner.zRot -= recoil * 0.4F;

        applySlam(state);
    }

    /** Raises both mauls to full height, drives them down, then settles. */
    private void applySlam(MeridianSentinelRenderState state) {
        float progress = state.slamProgress;
        switch (state.slamPhase) {
            case IDLE -> {
            }
            case RAISE -> {
                float lift = progress * progress;
                raiseArms(lift * 2.7F);
                torso.xRot -= lift * 0.35F;
                rootPart.y -= lift * 1.2F;
                spinRings(lift * 1.6F);
                leftLeg.xRot -= lift * 0.2F;
                rightLeg.xRot -= lift * 0.2F;
            }
            case SLAM -> {
                float drive = progress;
                raiseArms(2.7F - drive * 3.3F);
                torso.xRot += drive * 0.55F;
                rootPart.y += drive * 1.4F;
                spinRings(1.6F);
            }
            case SETTLE -> {
                // Damped rebound so the mauls do not teleport back to rest.
                float rebound = Mth.sin((1.0F - progress) * (float) Math.PI * 2.0F) * (1.0F - progress);
                raiseArms(rebound * -0.6F);
                torso.xRot += rebound * 0.25F;
                spinRings(rebound * 0.9F);
            }
        }
    }

    private void raiseArms(float amount) {
        leftArm.xRot -= amount;
        rightArm.xRot -= amount;
        leftArm.zRot -= amount * 0.12F;
        rightArm.zRot += amount * 0.12F;
    }

    private void spinRings(float amount) {
        ringOuter.yRot -= amount * 2.0F;
        ringInner.yRot += amount * 3.0F;
        ringOuter.xRot += amount * 0.12F;
        ringInner.zRot -= amount * 0.12F;
    }
}
