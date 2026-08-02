package com.nightbeam.tbos.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nightbeam.tbos.block.AlignmentDialBlock;
import com.nightbeam.tbos.blockentity.AlignmentDialBlockEntity;
import com.nightbeam.tbos.config.YesterglassClientConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** 1.21.1 immediate-mode rendition of the Alignment Dial's armillary rings. */
public final class AlignmentDialRenderer implements BlockEntityRenderer<AlignmentDialBlockEntity> {
    private static final int OUTER = 0xC8E0B85B;
    private static final int INNER = 0xB88CEFE4;
    private static final int GLYPH = 0xE0FFE7A8;

    public AlignmentDialRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            AlignmentDialBlockEntity dial,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        float gameTime = dial.getLevel() == null ? 0.0F : dial.getLevel().getGameTime() + partialTick;
        float time = YesterglassClientConfig.REDUCED_MOTION.getAsBoolean() ? 0.0F : gameTime;
        float facing = dial.getBlockState().hasProperty(AlignmentDialBlock.FACING)
                ? dial.getBlockState().getValue(AlignmentDialBlock.FACING).toYRot()
                : 0.0F;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.72F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing));
        poseStack.mulPose(Axis.XP.rotationDegrees(-22.5F));
        ring(poseStack, buffers, time * 0.9F, 0.38F, 10, OUTER);
        ring(poseStack, buffers, -time * 1.4F, 0.26F, 8, INNER);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.14F + (float) Math.sin(time * 0.07F) * 0.02F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(time * 1.8F));
        ArchiveGeometry.submitCenteredBox(poseStack, buffers, 0.0F, 0.0F, 0.0F, 0.10F, 0.10F, 0.03F, GLYPH);
        poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F));
        ArchiveGeometry.submitCenteredBox(poseStack, buffers, 0.0F, 0.0F, 0.0F, 0.10F, 0.10F, 0.03F, GLYPH);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void ring(
            PoseStack poseStack,
            MultiBufferSource buffers,
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
                    buffers,
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
