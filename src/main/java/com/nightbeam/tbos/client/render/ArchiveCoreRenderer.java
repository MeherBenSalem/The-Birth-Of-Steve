package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.blockentity.ArchiveCoreBlockEntity;
import com.nightbeam.tbos.config.YesterglassClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/**
 * Cosmetic inner core for the Archive Core housing. It reads nothing from the
 * Curator encounter; the animation is a pure function of world time so the
 * puzzle keeps a single state store in the site's progress flags.
 */
public final class ArchiveCoreRenderer
        implements BlockEntityRenderer<ArchiveCoreBlockEntity, ArchiveCoreRenderState> {
    private static final int CORE = 0xE0FFE7A8;
    private static final int SHELL = 0xA89C8AE8;
    private static final int RING = 0xC86EE7E0;

    public ArchiveCoreRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public ArchiveCoreRenderState createRenderState() {
        return new ArchiveCoreRenderState();
    }

    @Override
    public void extractRenderState(
            ArchiveCoreBlockEntity core,
            ArchiveCoreRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(core, state, partialTick, cameraPosition, breakProgress);
        state.time = (core.getLevel() == null ? 0.0F : core.getLevel().getGameTime()) + partialTick;
    }

    @Override
    public void submit(
            ArchiveCoreRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        boolean still = YesterglassClientConfig.REDUCED_MOTION.getAsBoolean();
        float time = still ? 0.0F : state.time;
        float pulse = 1.0F + (float) Math.sin(time * 0.06F) * 0.12F;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.1F));
        poseStack.mulPose(Axis.XP.rotationDegrees(35.0F + (float) Math.sin(time * 0.03F) * 12.0F));
        poseStack.scale(pulse, pulse, pulse);

        // Two interpenetrating boxes read as a faceted octahedron at this scale.
        ArchiveGeometry.submitCenteredBox(poseStack, collector, 0.0F, 0.0F, 0.0F, 0.24F, 0.24F, 0.24F, CORE);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        ArchiveGeometry.submitCenteredBox(poseStack, collector, 0.0F, 0.0F, 0.0F, 0.20F, 0.20F, 0.20F, SHELL);
        poseStack.popPose();
        poseStack.popPose();

        // Counter-rotating index ring around the housing.
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 0.7F));
        for (int index = 0; index < 6; index++) {
            double angle = Math.PI * 2.0D * index / 6.0D;
            ArchiveGeometry.submitCenteredBox(
                    poseStack,
                    collector,
                    (float) Math.cos(angle) * 0.36F,
                    (float) Math.sin(angle * 2.0D) * 0.06F,
                    (float) Math.sin(angle) * 0.36F,
                    0.06F,
                    0.06F,
                    0.06F,
                    RING);
        }
        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 32;
    }
}
