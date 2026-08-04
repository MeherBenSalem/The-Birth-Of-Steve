package com.nightbeam.tbos.client.render;

import static com.nightbeam.tbos.client.render.ArchiveEntityModels.TEX_SCALE;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.ParallaxWraithEntity;
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
 * A legless hunter: four shard plates orbit a hollow core and lag the body's
 * own turn, so every change of heading smears the silhouette.
 */
public final class ParallaxWraithModel extends EntityModel<ParallaxWraithEntity> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
            new ResourceLocation(Yesterglass.MOD_ID, "parallax_wraith"),
            "main");

    /** How far the shard shell trails the head's yaw, in radians per degree. */
    private static final float SHELL_LAG = (float) (Math.PI / 300.0);

    private final ModelPart rootPart;
    private final ModelPart body;
    private final ModelPart shell;
    private final ModelPart core;
    private final ModelPart mask;
    private final ModelPart shardNorth;
    private final ModelPart shardSouth;
    private final ModelPart shardEast;
    private final ModelPart shardWest;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart finUpper;
    private final ModelPart finMiddle;
    private final ModelPart finLower;

    public ParallaxWraithModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        rootPart = root;
        body = root.getChild("body");
        shell = body.getChild("shell");
        core = body.getChild("core");
        mask = body.getChild("mask");
        shardNorth = shell.getChild("shard_north");
        shardSouth = shell.getChild("shard_south");
        shardEast = shell.getChild("shard_east");
        shardWest = shell.getChild("shard_west");
        leftArm = body.getChild("left_arm");
        rightArm = body.getChild("right_arm");
        finUpper = body.getChild("fin_upper");
        finMiddle = body.getChild("fin_middle");
        finLower = body.getChild("fin_lower");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild(
                "body", CubeListBuilder.create(), PartPose.offset(0.0F, 4.0F, 0.0F));

        body.addOrReplaceChild(
                "core",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.ZERO);
        body.addOrReplaceChild(
                "mask",
                CubeListBuilder.create()
                        .texOffs(0, 10)
                        .addBox(-3.0F, -4.0F, -7.0F, 6.0F, 4.0F, 7.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE)
                        .texOffs(48, 12)
                        .addBox(-2.0F, -3.0F, -8.0F, 4.0F, 2.0F, 1.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.offset(0.0F, -5.0F, 0.0F));

        PartDefinition shell = body.addOrReplaceChild("shell", CubeListBuilder.create(), PartPose.ZERO);
        CubeListBuilder plate = CubeListBuilder.create()
                .texOffs(28, 0)
                .addBox(-4.0F, -5.0F, -0.5F, 8.0F, 10.0F, 1.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE);
        shell.addOrReplaceChild("shard_north", plate, PartPose.offset(0.0F, 0.0F, -4.5F));
        shell.addOrReplaceChild("shard_south", plate, PartPose.offset(0.0F, 0.0F, 4.5F));
        shell.addOrReplaceChild(
                "shard_east",
                plate,
                PartPose.offsetAndRotation(4.5F, 0.0F, 0.0F, 0.0F, (float) (Math.PI / 2.0), 0.0F));
        shell.addOrReplaceChild(
                "shard_west",
                plate,
                PartPose.offsetAndRotation(-4.5F, 0.0F, 0.0F, 0.0F, (float) (-Math.PI / 2.0), 0.0F));

        // The arms hang from below the shard cage so nothing intersects it at rest.
        body.addOrReplaceChild(
                "left_arm",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.offsetAndRotation(3.5F, 5.5F, 0.0F, 0.1F, 0.0F, -0.12F));
        body.addOrReplaceChild(
                "right_arm",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .mirror()
                        .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE),
                PartPose.offsetAndRotation(-3.5F, 5.5F, 0.0F, 0.1F, 0.0F, 0.12F));

        CubeListBuilder fin = CubeListBuilder.create()
                .texOffs(0, 42)
                .addBox(-3.0F, -1.0F, 0.0F, 6.0F, 2.0F, 9.0F, CubeDeformation.NONE, TEX_SCALE, TEX_SCALE);
        body.addOrReplaceChild("fin_upper", fin, PartPose.offsetAndRotation(0.0F, -3.5F, 5.5F, -0.22F, 0.0F, 0.0F));
        body.addOrReplaceChild("fin_middle", fin, PartPose.offsetAndRotation(0.0F, -0.5F, 5.5F, 0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("fin_lower", fin, PartPose.offsetAndRotation(0.0F, 2.5F, 5.5F, 0.22F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(
            ParallaxWraithEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        rootPart.getAllParts().forEach(ModelPart::resetPose);
        ParallaxWraithRenderState state = new ParallaxWraithRenderState();
        state.ageInTicks = ageInTicks;
        state.walkAnimationPos = limbSwing;
        state.walkAnimationSpeed = limbSwingAmount;
        state.yRot = netHeadYaw;
        state.xRot = headPitch;
        state.displacePhase = entity.getDisplacePhase();
        state.displaceProgress = entity.getDisplaceProgress(ageInTicks - entity.tickCount);
        state.attackTime = attackTime;
        state.hurtTime = Mth.clamp(entity.hurtTime / 10.0F, 0.0F, 1.0F);

        float drift = Mth.sin(state.ageInTicks * 0.09F);
        float glide = Mth.clamp(state.walkAnimationSpeed, 0.0F, 1.0F);
        float sway = Mth.cos(state.walkAnimationPos * 0.7F) * glide;

        rootPart.y += drift * 0.7F;
        body.xRot += glide * 0.16F;
        body.zRot += sway * 0.06F;

        mask.yRot += state.yRot * (float) (Math.PI / 180.0);
        mask.xRot += state.xRot * (float) (Math.PI / 180.0);
        core.yRot += state.ageInTicks * 0.06F;

        // The shell trails the head rather than following it; that lag is the
        // whole "parallax" read and is what separates it from a solid body.
        shell.yRot -= state.yRot * SHELL_LAG;
        shell.zRot += sway * 0.09F;
        shardNorth.zRot += drift * 0.07F;
        shardSouth.zRot -= drift * 0.07F;
        shardEast.xRot += drift * 0.07F;
        shardWest.xRot -= drift * 0.07F;

        float armSwing = Mth.cos(state.walkAnimationPos * 0.7F) * 0.22F * glide;
        leftArm.xRot += armSwing + drift * 0.05F;
        rightArm.xRot -= armSwing - drift * 0.05F;

        finUpper.xRot -= glide * 0.3F + drift * 0.05F;
        finMiddle.xRot -= glide * 0.24F + drift * 0.07F;
        finLower.xRot -= glide * 0.18F + drift * 0.09F;
        finUpper.yRot += sway * 0.14F;
        finMiddle.yRot += sway * 0.2F;
        finLower.yRot += sway * 0.26F;

        float scythe = Mth.sin(state.attackTime * (float) Math.PI);
        leftArm.xRot -= scythe * 1.35F;
        rightArm.xRot -= scythe * 1.35F;
        leftArm.zRot -= scythe * 0.5F;
        rightArm.zRot += scythe * 0.5F;
        body.xRot += scythe * 0.18F;

        float recoil = Mth.sin(state.hurtTime * (float) Math.PI);
        body.zRot += recoil * 0.2F;
        shell.zRot -= recoil * 0.3F;

        applyDisplacement(state);
    }

    /** Scatters the shell outward, hollows the body, then snaps it back. */
    private void applyDisplacement(ParallaxWraithRenderState state) {
        float progress = state.displaceProgress;
        switch (state.displacePhase) {
            case IDLE -> {
            }
            case FRACTURE -> {
                spreadShell(progress * 3.5F, progress * 2.4F);
                mask.y -= progress * 1.2F;
                leftArm.xRot -= progress * 0.5F;
                rightArm.xRot -= progress * 0.5F;
            }
            case DISPLACED -> {
                spreadShell(3.5F, 2.4F + progress * 1.6F);
                mask.y -= 1.2F;
                mask.xRot -= 0.3F;
                body.zRot += progress * 0.4F;
            }
            case REFORM -> {
                // Overshoot on the way home so the plates visibly snap shut.
                float settle = Mth.sin((1.0F - progress) * (float) Math.PI * 1.5F);
                spreadShell(settle * 2.2F, settle * 1.8F);
                body.xRot -= settle * 0.2F;
            }
        }
    }

    private void spreadShell(float distance, float spin) {
        shardNorth.z -= distance;
        shardSouth.z += distance;
        shardEast.x += distance;
        shardWest.x -= distance;
        shardNorth.xRot -= spin * 0.6F;
        shardSouth.xRot += spin * 0.6F;
        shardEast.zRot += spin * 0.6F;
        shardWest.zRot -= spin * 0.6F;
        shell.yRot += spin * 0.35F;
        core.yRot += spin;
    }

    @Override
    public void renderToBuffer(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha) {
        rootPart.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
