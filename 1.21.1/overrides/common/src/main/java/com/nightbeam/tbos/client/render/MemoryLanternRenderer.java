package com.nightbeam.tbos.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nightbeam.tbos.blockentity.MemoryLanternBlockEntity;
import com.nightbeam.tbos.config.YesterglassClientConfig;
import com.nightbeam.tbos.item.MemoryScene;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/**
 * 1.21.1 immediate-mode memory tableau.  The scene selection and playback
 * timing stay server-owned by the block entity; this class only draws its
 * colored figures and orbiting recollection motes.
 */
public final class MemoryLanternRenderer implements BlockEntityRenderer<MemoryLanternBlockEntity> {
    private static final int PALE = 0xC8EAF7FF;
    private static final int GOLD = 0xD8FFD16A;
    private static final int BRONZE = 0xC8C88755;
    private static final int CYAN = 0xC86EE7E0;
    private static final int INDIGO = 0xB88878E8;

    public MemoryLanternRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            MemoryLanternBlockEntity lantern,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        float gameTime = lantern.getLevel() == null ? 0.0F : lantern.getLevel().getGameTime() + partialTick;
        float time = YesterglassClientConfig.REDUCED_MOTION.getAsBoolean() ? 0.0F : gameTime;
        MemoryScene scene = lantern.scene().orElse(null);
        if (!lantern.isPlaying() || scene == null) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.42F + (float) Math.sin(time * 0.05F) * 0.06F, 0.5F);
            poseStack.mulPose(Axis.YP.rotation(time * 0.03F));
            ArchiveGeometry.submitCenteredBox(poseStack, buffers, 0.0F, 0.0F, 0.0F, 0.09F, 0.09F, 0.09F, CYAN);
            poseStack.popPose();
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.15F + (float) Math.sin(time * 0.08F) * 0.025F, 0.5F);
        poseStack.scale(0.72F, 0.72F, 0.72F);
        int sceneColor = sceneColor(scene);
        float spread = scene == MemoryScene.ARCHIVE_EVACUATION ? 0.54F : 0.38F;
        figure(poseStack, buffers, -spread, 0.0F, 0.08F, scene == MemoryScene.CELESTIAL_FAMILY ? 0.72F : 0.82F, PALE);
        figure(poseStack, buffers, spread, 0.0F, -0.05F, 0.76F, sceneColor);
        if (scene == MemoryScene.CELESTIAL_FAMILY) {
            figure(poseStack, buffers, 0.0F, 0.0F, 0.18F, 0.46F, CYAN);
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotation(time * (scene == MemoryScene.ARCHIVE_FALL ? -0.06F : 0.045F)));
        for (int index = 0; index < 6; index++) {
            double angle = Math.PI * 2.0D * index / 6.0D;
            float vertical = 0.58F + (float) Math.sin(angle * 2.0D + time * 0.10F) * 0.14F;
            ArchiveGeometry.submitCenteredBox(
                    poseStack,
                    buffers,
                    (float) Math.cos(angle) * 0.40F,
                    vertical,
                    (float) Math.sin(angle) * 0.28F,
                    0.09F,
                    0.09F,
                    0.09F,
                    index % 2 == 0 ? GOLD : sceneColor);
        }
        poseStack.popPose();
        poseStack.popPose();
    }

    private static int sceneColor(MemoryScene scene) {
        return switch (scene) {
            case ASTRONOMERS -> GOLD;
            case CURATOR_SMITH -> BRONZE;
            case CELESTIAL_FAMILY -> CYAN;
            case ARCHIVE_EVACUATION -> PALE;
            case FINAL_COMMAND -> GOLD;
            case ARCHIVE_FALL -> INDIGO;
        };
    }

    private static void figure(
            PoseStack poseStack,
            MultiBufferSource buffers,
            float x,
            float y,
            float z,
            float height,
            int color) {
        ArchiveGeometry.submitBox(
                poseStack, buffers, x, y + height * 0.18F, z, height * 0.24F, height * 0.52F, height * 0.18F, color);
        ArchiveGeometry.submitBox(
                poseStack, buffers, x, y + height * 0.76F, z, height * 0.22F, height * 0.22F, height * 0.22F, color);
    }

    @Override
    public int getViewDistance() {
        return 24;
    }
}
