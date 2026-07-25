package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.LenswardEntity;
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

public final class LenswardModel extends EntityModel<LenswardRenderState> {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "lensward"),
            "main");

    private static final int SHARD_COUNT = 4;

    private final ModelPart rootPart;
    private final ModelPart body;
    private final ModelPart housing;
    private final ModelPart core;
    private final ModelPart outerRing;
    private final ModelPart innerRing;
    private final ModelPart[] shards = new ModelPart[SHARD_COUNT];

    public LenswardModel(ModelPart root) {
        super(root);
        rootPart = root;
        body = root.getChild("body");
        housing = body.getChild("housing");
        core = body.getChild("core");
        outerRing = body.getChild("outer_ring");
        innerRing = body.getChild("inner_ring");
        for (int index = 0; index < SHARD_COUNT; index++) {
            shards[index] = body.getChild("shard" + index);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild(
                "body", CubeListBuilder.create(), PartPose.offset(0.0F, 13.0F, 0.0F));

        body.addOrReplaceChild(
                "housing",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.ZERO);

        body.addOrReplaceChild(
                "core",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-2.0F, -2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.ZERO);

        body.addOrReplaceChild(
                "outer_ring",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-6.0F, -0.5F, -6.5F, 12.0F, 1.0F, 1.0F)
                        .texOffs(0, 16)
                        .addBox(-6.0F, -0.5F, 5.5F, 12.0F, 1.0F, 1.0F)
                        .texOffs(0, 18)
                        .addBox(5.5F, -0.5F, -5.5F, 1.0F, 1.0F, 11.0F)
                        .texOffs(0, 18)
                        .addBox(-6.5F, -0.5F, -5.5F, 1.0F, 1.0F, 11.0F),
                PartPose.ZERO);

        body.addOrReplaceChild(
                "inner_ring",
                CubeListBuilder.create()
                        .texOffs(26, 16)
                        .addBox(-4.5F, -0.5F, -5.0F, 9.0F, 1.0F, 1.0F)
                        .texOffs(26, 16)
                        .addBox(-4.5F, -0.5F, 4.0F, 9.0F, 1.0F, 1.0F)
                        .texOffs(24, 18)
                        .addBox(4.0F, -0.5F, -4.0F, 1.0F, 1.0F, 8.0F)
                        .texOffs(24, 18)
                        .addBox(-5.0F, -0.5F, -4.0F, 1.0F, 1.0F, 8.0F),
                PartPose.rotation((float) (Math.PI / 2.0), 0.0F, 0.0F));

        CubeListBuilder shard = CubeListBuilder.create()
                .texOffs(44, 18)
                .addBox(-0.5F, 0.0F, -0.5F, 1.0F, 3.0F, 1.0F);
        float[][] shardOffsets = {{2.5F, 2.5F}, {-2.5F, 2.5F}, {2.5F, -2.5F}, {-2.5F, -2.5F}};
        for (int index = 0; index < SHARD_COUNT; index++) {
            body.addOrReplaceChild(
                    "shard" + index,
                    shard,
                    PartPose.offset(shardOffsets[index][0], 3.6F, shardOffsets[index][1]));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LenswardRenderState state) {
        super.setupAnim(state);

        float age = state.ageInTicks;
        float charging = state.beamPhase == LenswardEntity.BeamPhase.CHARGING ? state.beamProgress : 0.0F;
        boolean firing = state.beamPhase == LenswardEntity.BeamPhase.FIRING;
        float recovery = state.beamPhase == LenswardEntity.BeamPhase.RECOVERY
                ? Mth.sin(state.beamProgress * (float) Math.PI)
                : 0.0F;

        rootPart.y += Mth.sin(age * 0.09F) * 0.6F;
        body.zRot += Mth.sin(age * 0.07F) * 0.03F;

        float spinRate = 0.05F + charging * 0.22F + (firing ? 0.3F : 0.0F);
        outerRing.yRot += age * spinRate;
        innerRing.yRot += -age * spinRate * 1.35F;
        innerRing.xRot += Mth.sin(age * 0.05F) * 0.12F - charging * 0.25F;
        outerRing.xRot += charging * 0.3F - recovery * 0.2F;

        float swell = 1.0F + charging * 0.45F + (firing ? 0.5F : 0.0F) - recovery * 0.15F;
        core.xScale = swell;
        core.yScale = swell;
        core.zScale = swell;
        core.y += Mth.sin(age * 0.15F) * 0.15F;

        housing.xRot += charging * 0.12F;
        body.xRot += state.xRot * (float) (Math.PI / 900.0);

        for (int index = 0; index < SHARD_COUNT; index++) {
            float phase = age * 0.11F + index * 1.7F;
            shards[index].xRot += Mth.sin(phase) * 0.22F - charging * 0.35F;
            shards[index].zRot += Mth.cos(phase * 0.8F) * 0.18F;
        }

        float recoil = Mth.sin(state.hurtTime * (float) Math.PI);
        body.zRot += recoil * 0.18F;
        housing.xRot -= recoil * 0.12F;
    }
}
