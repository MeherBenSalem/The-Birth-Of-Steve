package com.nightbeam.tbos.client.render;

import static com.nightbeam.tbos.client.render.ArchiveEntityModels.TEX_SCALE;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.HourCantorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Archive's conductor: a legless robed column carrying a caged metronome,
 * two hour rings, and four arms that beat the refrain out in four.
 */
public final class HourCantorModel extends EntityModel<HourCantorEntity> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "hour_cantor"),
            "main");

    private static final float LOW_RING_TILT = 0.28F;
    private static final float HIGH_RING_TILT = 0.42F;

    private final ModelPart rootPart;
    private final ModelPart column;
    private final ModelPart head;
    private final ModelPart crown;
    private final ModelPart cage;
    private final ModelPart pendulum;
    private final ModelPart ringLow;
    private final ModelPart ringHigh;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftArmLower;
    private final ModelPart rightArmLower;
    private final ModelPart streamerLeft;
    private final ModelPart streamerRight;
    private final ModelPart streamerBack;

    public HourCantorModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        rootPart = root;
        column = root.getChild("column");
        head = column.getChild("head");
        crown = head.getChild("crown");
        cage = column.getChild("cage");
        pendulum = column.getChild("pendulum");
        ringLow = column.getChild("ring_low");
        ringHigh = column.getChild("ring_high");
        leftArm = column.getChild("left_arm");
        rightArm = column.getChild("right_arm");
        leftArmLower = column.getChild("left_arm_lower");
        rightArmLower = column.getChild("right_arm_lower");
        streamerLeft = column.getChild("streamer_left");
        streamerRight = column.getChild("streamer_right");
        streamerBack = column.getChild("streamer_back");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition column = root.addOrReplaceChild(
                "column",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-5.0F, -12.0F, -5.0F, 10.0F, 12.0F, 10.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                        .texOffs(0, 23)
                        .addBox(-4.0F, -20.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.offset(0.0F, -8.0F, 0.0F));

        PartDefinition head = column.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(33, 23)
                        .addBox(-3.5F, -6.0F, -3.5F, 7.0F, 6.0F, 7.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.offset(0.0F, -20.5F, 0.0F));
        head.addOrReplaceChild(
                "crown",
                CubeListBuilder.create()
                        .texOffs(0, 40)
                        .addBox(-4.5F, -1.0F, -4.5F, 9.0F, 1.0F, 9.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.offset(0.0F, -6.0F, 0.0F));

        // The cage sits in the band between the hour rings and the lower robe, and
        // the pendulum hangs in front of it rather than through it.
        column.addOrReplaceChild(
                "cage",
                CubeListBuilder.create()
                        .texOffs(41, 10)
                        .addBox(-3.0F, -3.0F, -0.5F, 6.0F, 6.0F, 1.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.offset(0.0F, -15.5F, -4.5F));
        column.addOrReplaceChild(
                "pendulum",
                CubeListBuilder.create()
                        .texOffs(48, 0)
                        .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 5.0F, 1.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                        .texOffs(53, 0)
                        .addBox(-1.0F, 5.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.offset(0.0F, -18.0F, -6.0F));

        column.addOrReplaceChild("ring_low", ringBars(), PartPose.offsetAndRotation(
                0.0F, -19.0F, 0.0F, LOW_RING_TILT, 0.0F, 0.0F));
        column.addOrReplaceChild("ring_high", ringBars(), PartPose.offsetAndRotation(
                0.0F, -20.0F, 0.0F, 0.0F, 0.0F, HIGH_RING_TILT));

        CubeListBuilder conductingArm = CubeListBuilder.create()
                .texOffs(55, 40)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                .texOffs(37, 40)
                .addBox(-0.5F, 11.0F, -1.5F, 1.0F, 7.0F, 3.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE);
        // Conducting arms sit outboard of the hour rings and behind the folded pair.
        column.addOrReplaceChild(
                "left_arm", conductingArm, PartPose.offsetAndRotation(7.5F, -17.0F, 1.0F, 0.0F, 0.0F, -0.15F));
        column.addOrReplaceChild(
                "right_arm", conductingArm, PartPose.offsetAndRotation(-7.5F, -17.0F, 1.0F, 0.0F, 0.0F, 0.15F));

        CubeListBuilder foldedArm = CubeListBuilder.create()
                .texOffs(0, 51)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE);
        column.addOrReplaceChild(
                "left_arm_lower",
                foldedArm,
                PartPose.offsetAndRotation(7.5F, -13.0F, -3.0F, -0.9F, 0.0F, -0.2F));
        column.addOrReplaceChild(
                "right_arm_lower",
                foldedArm,
                PartPose.offsetAndRotation(-7.5F, -13.0F, -3.0F, -0.9F, 0.0F, 0.2F));

        CubeListBuilder streamer = CubeListBuilder.create()
                .texOffs(41, 0)
                .addBox(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE);
        column.addOrReplaceChild("streamer_left", streamer, PartPose.offset(3.0F, 0.0F, -2.0F));
        column.addOrReplaceChild("streamer_right", streamer, PartPose.offset(-3.0F, 0.0F, -2.0F));
        column.addOrReplaceChild("streamer_back", streamer, PartPose.offset(0.0F, 0.0F, 3.5F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** A five-radius hoop; the two rings differ by tilt and spin, not size. */
    private static CubeListBuilder ringBars() {
        return CubeListBuilder.create()
                .texOffs(37, 37)
                .addBox(-5.0F, -0.5F, -6.0F, 10.0F, 1.0F, 1.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                .texOffs(37, 37)
                .addBox(-5.0F, -0.5F, 5.0F, 10.0F, 1.0F, 1.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                .texOffs(13, 51)
                .addBox(-6.0F, -0.5F, -5.0F, 1.0F, 1.0F, 10.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                .texOffs(13, 51)
                .addBox(5.0F, -0.5F, -5.0F, 1.0F, 1.0F, 10.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE);
    }

    @Override
    public void setupAnim(
            HourCantorEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        rootPart.getAllParts().forEach(ModelPart::resetPose);
        HourCantorRenderState state = new HourCantorRenderState();
        state.ageInTicks = ageInTicks;
        state.walkAnimationPos = limbSwing;
        state.walkAnimationSpeed = limbSwingAmount;
        state.yRot = netHeadYaw;
        state.xRot = headPitch;
        state.refrainPhase = entity.getRefrainPhase();
        state.refrainProgress = entity.getRefrainProgress(ageInTicks - entity.tickCount);
        state.cadence = 1.0F - Mth.clamp(entity.getRefrainCooldown() / 80.0F, 0.0F, 1.0F);
        state.escalated = entity.isEscalated();
        state.attackTime = attackTime;
        state.hurtTime = Mth.clamp(entity.hurtTime / 10.0F, 0.0F, 1.0F);

        float hover = Mth.sin(state.ageInTicks * 0.05F);
        float drift = Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
        float lean = Mth.cos(state.walkAnimationPos * 0.5F) * drift;

        rootPart.y += hover * 0.9F;
        column.xRot += drift * 0.1F;
        column.zRot += lean * 0.04F;

        head.yRot += state.yRot * (float) (Math.PI / 180.0);
        head.xRot += state.xRot * (float) (Math.PI / 180.0);
        crown.yRot -= state.ageInTicks * 0.04F;

        // The pendulum is the boss's own telegraph: its swing tightens as the
        // next refrain approaches, and again once the Cantor is wounded.
        float cadence = state.cadence + (state.escalated ? 0.35F : 0.0F);
        pendulum.zRot += Mth.sin(state.ageInTicks * (0.15F + cadence * 0.25F)) * (0.35F + cadence * 0.3F);
        cage.zRot += hover * 0.02F;

        ringLow.yRot += state.ageInTicks * 0.045F;
        ringHigh.yRot -= state.ageInTicks * 0.075F;
        if (state.escalated) {
            ringLow.xRot += 0.3F;
            ringHigh.zRot -= 0.3F;
        }

        streamerLeft.xRot += hover * 0.09F - drift * 0.25F;
        streamerRight.xRot -= hover * 0.09F + drift * 0.25F;
        streamerBack.xRot += Mth.sin(state.ageInTicks * 0.07F) * 0.11F - drift * 0.2F;
        streamerLeft.zRot += lean * 0.12F;
        streamerRight.zRot += lean * 0.12F;

        float idleConduct = Mth.sin(state.ageInTicks * 0.06F);
        leftArm.xRot += idleConduct * 0.14F;
        rightArm.xRot -= idleConduct * 0.14F;
        leftArmLower.xRot += idleConduct * 0.05F;
        rightArmLower.xRot -= idleConduct * 0.05F;

        float swipe = Mth.sin(state.attackTime * (float) Math.PI);
        leftArm.xRot -= swipe * 1.5F;
        rightArm.xRot -= swipe * 1.5F;
        column.xRot += swipe * 0.14F;

        float recoil = Mth.sin(state.hurtTime * (float) Math.PI);
        column.zRot += recoil * 0.16F;
        head.zRot -= recoil * 0.22F;
        ringLow.zRot += recoil * 0.3F;

        applyRefrain(state);
    }

    /** Beats the intone out in four, throws the release wide, then settles. */
    private void applyRefrain(HourCantorRenderState state) {
        float progress = state.refrainProgress;
        switch (state.refrainPhase) {
            case IDLE -> {
            }
            case INTONE -> {
                float beat = progress * 4.0F;
                float within = beat - Mth.floor(beat);
                float stroke = Mth.sin(within * (float) Math.PI);
                boolean leading = ((int) beat & 1) == 0;
                leftArm.xRot -= stroke * (leading ? 1.5F : 0.5F);
                rightArm.xRot -= stroke * (leading ? 0.5F : 1.5F);
                leftArm.zRot -= stroke * (leading ? 0.55F : 0.2F);
                rightArm.zRot += stroke * (leading ? 0.2F : 0.55F);
                head.xRot -= stroke * 0.12F;
                openRings(progress * 0.5F);
                rootPart.y -= progress * 1.1F;
            }
            case REFRAIN -> {
                float release = Mth.sin(progress * (float) Math.PI);
                leftArm.xRot -= 2.3F;
                rightArm.xRot -= 2.3F;
                leftArm.zRot -= 1.1F + release * 0.4F;
                rightArm.zRot += 1.1F + release * 0.4F;
                leftArmLower.xRot -= release * 0.8F;
                rightArmLower.xRot -= release * 0.8F;
                column.xRot -= release * 0.3F;
                head.xRot -= release * 0.35F;
                openRings(0.5F + release * 0.9F);
                rootPart.y -= 1.1F + release * 0.8F;
            }
            case REST -> {
                float settle = 1.0F - progress;
                leftArm.xRot -= settle * 1.2F;
                rightArm.xRot -= settle * 1.2F;
                leftArm.zRot -= settle * 0.55F;
                rightArm.zRot += settle * 0.55F;
                openRings(settle * 0.6F);
                rootPart.y -= settle * 0.9F;
            }
        }
    }

    private void openRings(float amount) {
        ringLow.y -= amount * 1.6F;
        ringHigh.y -= amount * 2.6F;
        ringLow.xRot += amount * 0.5F;
        ringHigh.zRot -= amount * 0.5F;
        ringLow.yRot -= amount * 2.4F;
        ringHigh.yRot += amount * 3.4F;
    }

    @Override
    public void renderToBuffer(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            int color) {
        rootPart.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
