package com.nightbeam.tbos.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nightbeam.tbos.blockentity.ArchiveCoreBlockEntity;
import com.nightbeam.tbos.config.YesterglassClientConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** 1.20.1 immediate-mode rendition of the rotating Archive Core. */
public final class ArchiveCoreRenderer implements BlockEntityRenderer<ArchiveCoreBlockEntity> {
    private static final int CORE = 0xE0FFE7A8;
    private static final int SHELL = 0xA89C8AE8;
    private static final int RING = 0xC86EE7E0;

    public ArchiveCoreRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            ArchiveCoreBlockEntity core,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        float gameTime = core.getLevel() == null ? 0.0F : core.getLevel().getGameTime() + partialTick;
        float time = YesterglassClientConfig.REDUCED_MOTION.getAsBoolean() ? 0.0F : gameTime;
        float pulse = 1.0F + (float) Math.sin(time * 0.06F) * 0.12F;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.1F));
        poseStack.mulPose(Axis.XP.rotationDegrees(35.0F + (float) Math.sin(time * 0.03F) * 12.0F));
        poseStack.scale(pulse, pulse, pulse);
        ArchiveGeometry.submitCenteredBox(poseStack, buffers, 0.0F, 0.0F, 0.0F, 0.24F, 0.24F, 0.24F, CORE);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        ArchiveGeometry.submitCenteredBox(poseStack, buffers, 0.0F, 0.0F, 0.0F, 0.20F, 0.20F, 0.20F, SHELL);
        poseStack.popPose();
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 0.7F));
        for (int index = 0; index < 6; index++) {
            double angle = Math.PI * 2.0D * index / 6.0D;
            ArchiveGeometry.submitCenteredBox(
                    poseStack,
                    buffers,
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
