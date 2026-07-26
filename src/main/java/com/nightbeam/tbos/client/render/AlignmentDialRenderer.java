package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.block.AlignmentDialBlock;
import com.nightbeam.tbos.blockentity.AlignmentDialBlockEntity;
import com.nightbeam.tbos.config.YesterglassClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Cosmetic armillary rings above the Alignment Dial. Dial puzzle state stays in
 * the site's progress flags, so this animation is driven only by world time and
 * the block's facing.
 */
public final class AlignmentDialRenderer
        implements BlockEntityRenderer<AlignmentDialBlockEntity, AlignmentDialRenderState> {
    private static final int OUTER = 0xC8E0B85B;
    private static final int INNER = 0xB88CEFE4;
    private static final int GLYPH = 0xE0FFE7A8;

    public AlignmentDialRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AlignmentDialRenderState createRenderState() {
        return new AlignmentDialRenderState();
    }

    @Override
    public void extractRenderState(
            AlignmentDialBlockEntity dial,
            AlignmentDialRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(dial, state, partialTick, cameraPosition, breakProgress);
        state.time = (dial.getLevel() == null ? 0.0F : dial.getLevel().getGameTime()) + partialTick;
        BlockState blockState = dial.getBlockState();
        state.facingDegrees = blockState.hasProperty(AlignmentDialBlock.FACING)
                ? blockState.getValue(AlignmentDialBlock.FACING).toYRot()
                : 0.0F;
    }

    @Override
    public void submit(
            AlignmentDialRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        boolean still = YesterglassClientConfig.REDUCED_MOTION.getAsBoolean();
        float time = still ? 0.0F : state.time;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.72F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-state.facingDegrees));
        // The dial face is tilted 22.5 degrees in the JSON model; the rings share it.
        poseStack.mulPose(Axis.XP.rotationDegrees(-22.5F));

        ring(poseStack, collector, time * 0.9F, 0.38F, 10, OUTER);
        ring(poseStack, collector, -time * 1.4F, 0.26F, 8, INNER);

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.14F + (float) Math.sin(time * 0.07F) * 0.02F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 1.8F));
        ArchiveGeometry.submitCenteredBox(poseStack, collector, 0.0F, 0.0F, 0.0F, 0.10F, 0.10F, 0.03F, GLYPH);
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        ArchiveGeometry.submitCenteredBox(poseStack, collector, 0.0F, 0.0F, 0.0F, 0.10F, 0.10F, 0.03F, GLYPH);
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void ring(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float spinDegrees,
            float radius,
            int segments,
            int color) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(spinDegrees));
        for (int index = 0; index < segments; index++) {
            double angle = Math.PI * 2.0D * index / segments;
            ArchiveGeometry.submitCenteredBox(
                    poseStack,
                    collector,
                    (float) Math.cos(angle) * radius,
                    (float) Math.sin(angle) * radius,
                    0.0F,
                    0.05F,
                    0.05F,
                    0.05F,
                    color);
        }
        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 24;
    }
}
