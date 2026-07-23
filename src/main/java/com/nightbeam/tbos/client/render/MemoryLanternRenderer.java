package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.blockentity.MemoryLanternBlockEntity;
import com.nightbeam.tbos.config.YesterglassClientConfig;
import com.nightbeam.tbos.item.MemoryScene;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

public final class MemoryLanternRenderer
        implements BlockEntityRenderer<MemoryLanternBlockEntity, MemoryLanternRenderState> {
    private static final int PALE = 0xC8EAF7FF;
    private static final int GOLD = 0xD8FFD16A;
    private static final int BRONZE = 0xC8C88755;
    private static final int CYAN = 0xC86EE7E0;
    private static final int INDIGO = 0xB88878E8;

    public MemoryLanternRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public MemoryLanternRenderState createRenderState() {
        return new MemoryLanternRenderState();
    }

    @Override
    public void extractRenderState(
            MemoryLanternBlockEntity lantern,
            MemoryLanternRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                lantern,
                state,
                partialTick,
                cameraPosition,
                breakProgress);
        state.scene = lantern.scene().orElse(null);
        state.playing = lantern.isPlaying();
        state.time = (lantern.getLevel() == null ? 0.0F : lantern.getLevel().getGameTime()) + partialTick;
    }

    @Override
    public void submit(
            MemoryLanternRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState cameraState) {
        if (!state.playing || state.scene == null) {
            return;
        }
        float time = YesterglassClientConfig.REDUCED_MOTION.getAsBoolean() ? 0.0F : state.time;
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.15F + (float) Math.sin(time * 0.08F) * 0.025F, 0.5F);
        poseStack.scale(0.72F, 0.72F, 0.72F);
        renderScene(state.scene, time, poseStack, collector);
        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 24;
    }

    private static void renderScene(
            MemoryScene scene,
            float time,
            PoseStack poseStack,
            SubmitNodeCollector collector) {
        switch (scene) {
            case ASTRONOMERS -> renderAstronomers(time, poseStack, collector);
            case CURATOR_SMITH -> renderSmith(time, poseStack, collector);
            case CELESTIAL_FAMILY -> renderFamily(time, poseStack, collector);
            case ARCHIVE_EVACUATION -> renderEvacuation(time, poseStack, collector);
            case FINAL_COMMAND -> renderFinalCommand(time, poseStack, collector);
            case ARCHIVE_FALL -> renderArchiveFall(time, poseStack, collector);
        }
    }

    private static void renderAstronomers(float time, PoseStack poseStack, SubmitNodeCollector collector) {
        submitFigure(poseStack, collector, -0.42F, 0.0F, 0.12F, 0.72F, PALE);
        submitFigure(poseStack, collector, 0.42F, 0.0F, 0.12F, 0.72F, PALE);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotation(time * 0.045F));
        for (int index = 0; index < 8; index++) {
            double angle = Math.PI * 2.0D * index / 8.0D;
            submitBox(
                    poseStack,
                    collector,
                    (float) Math.cos(angle) * 0.38F,
                    0.45F + (float) Math.sin(angle) * 0.14F,
                    (float) Math.sin(angle) * 0.38F,
                    0.12F,
                    0.07F,
                    0.07F,
                    GOLD);
        }
        poseStack.popPose();
    }

    private static void renderSmith(float time, PoseStack poseStack, SubmitNodeCollector collector) {
        submitFigure(poseStack, collector, -0.35F, 0.0F, 0.05F, 0.78F, BRONZE);
        submitBox(poseStack, collector, 0.18F, 0.12F, 0.0F, 0.45F, 0.18F, 0.28F, CYAN);
        submitBox(poseStack, collector, 0.28F, 0.30F, 0.0F, 0.24F, 0.18F, 0.22F, INDIGO);
        poseStack.pushPose();
        poseStack.translate(-0.06F, 0.64F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-25.0F + (float) Math.sin(time * 0.18F) * 32.0F));
        submitBox(poseStack, collector, 0.0F, -0.28F, 0.0F, 0.07F, 0.52F, 0.07F, GOLD);
        submitBox(poseStack, collector, 0.0F, 0.0F, 0.0F, 0.28F, 0.12F, 0.12F, GOLD);
        poseStack.popPose();
    }

    private static void renderFamily(float time, PoseStack poseStack, SubmitNodeCollector collector) {
        submitFigure(poseStack, collector, -0.42F, 0.0F, 0.18F, 0.72F, PALE);
        submitFigure(poseStack, collector, 0.0F, 0.0F, 0.08F, 0.48F, CYAN);
        submitFigure(poseStack, collector, 0.42F, 0.0F, 0.18F, 0.72F, PALE);
        float orbit = time * 0.035F;
        submitBox(
                poseStack,
                collector,
                (float) Math.cos(orbit) * 0.34F,
                0.95F,
                (float) Math.sin(orbit) * 0.16F,
                0.18F,
                0.18F,
                0.18F,
                GOLD);
    }

    private static void renderEvacuation(float time, PoseStack poseStack, SubmitNodeCollector collector) {
        submitBox(poseStack, collector, 0.48F, 0.0F, 0.0F, 0.10F, 0.88F, 0.10F, GOLD);
        submitBox(poseStack, collector, 0.48F, 0.76F, 0.0F, 0.52F, 0.10F, 0.10F, GOLD);
        for (int index = 0; index < 4; index++) {
            float stride = ((time * 0.018F + index * 0.24F) % 1.2F) - 0.62F;
            submitFigure(
                    poseStack,
                    collector,
                    stride,
                    0.0F,
                    0.18F - index * 0.11F,
                    index == 2 ? 0.48F : 0.64F,
                    index % 2 == 0 ? CYAN : PALE);
        }
    }

    private static void renderFinalCommand(float time, PoseStack poseStack, SubmitNodeCollector collector) {
        submitFigure(poseStack, collector, -0.42F, 0.0F, 0.0F, 0.72F, PALE);
        submitFigure(poseStack, collector, 0.38F, 0.0F, 0.0F, 1.0F, BRONZE);
        float pulse = 0.03F + ((float) Math.sin(time * 0.13F) + 1.0F) * 0.025F;
        submitBox(poseStack, collector, -0.02F, 0.58F, 0.0F, 0.68F, pulse, pulse, GOLD);
        for (int index = 0; index < 5; index++) {
            double angle = Math.PI * 2.0D * index / 5.0D + time * 0.025F;
            submitBox(
                    poseStack,
                    collector,
                    0.38F + (float) Math.cos(angle) * 0.24F,
                    0.92F,
                    (float) Math.sin(angle) * 0.24F,
                    0.07F,
                    0.07F,
                    0.07F,
                    CYAN);
        }
    }

    private static void renderArchiveFall(float time, PoseStack poseStack, SubmitNodeCollector collector) {
        for (int index = 0; index < 4; index++) {
            poseStack.pushPose();
            float x = -0.48F + index * 0.32F;
            float fall = (float) Math.sin(time * 0.045F + index) * 0.08F;
            poseStack.translate(x, 0.0F, index % 2 == 0 ? -0.12F : 0.12F);
            poseStack.mulPose(Axis.ZP.rotationDegrees((index - 1.5F) * 12.0F + fall * 55.0F));
            submitBox(poseStack, collector, 0.0F, 0.05F, 0.0F, 0.15F, 0.82F, 0.15F, INDIGO);
            poseStack.popPose();
        }
        submitBox(
                poseStack,
                collector,
                0.0F,
                1.0F - ((time * 0.012F) % 0.55F),
                0.0F,
                0.28F,
                0.08F,
                0.28F,
                GOLD);
    }

    private static void submitFigure(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float x,
            float y,
            float z,
            float height,
            int color) {
        submitBox(poseStack, collector, x, y + height * 0.18F, z, height * 0.24F, height * 0.52F, height * 0.18F, color);
        submitBox(poseStack, collector, x, y + height * 0.76F, z, height * 0.22F, height * 0.22F, height * 0.22F, color);
    }

    private static void submitBox(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float centerX,
            float bottomY,
            float centerZ,
            float width,
            float height,
            float depth,
            int color) {
        poseStack.pushPose();
        poseStack.translate(centerX, bottomY, centerZ);
        collector.submitCustomGeometry(
                poseStack,
                RenderTypes.debugQuads(),
                (pose, vertices) -> drawBox(pose, vertices, width, height, depth, color));
        poseStack.popPose();
    }

    private static void drawBox(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            float width,
            float height,
            float depth,
            int color) {
        float x0 = -width * 0.5F;
        float x1 = width * 0.5F;
        float z0 = -depth * 0.5F;
        float z1 = depth * 0.5F;
        quad(pose, vertices, color, x0, 0.0F, z1, x1, 0.0F, z1, x1, height, z1, x0, height, z1);
        quad(pose, vertices, color, x1, 0.0F, z0, x0, 0.0F, z0, x0, height, z0, x1, height, z0);
        quad(pose, vertices, color, x0, 0.0F, z0, x0, 0.0F, z1, x0, height, z1, x0, height, z0);
        quad(pose, vertices, color, x1, 0.0F, z1, x1, 0.0F, z0, x1, height, z0, x1, height, z1);
        quad(pose, vertices, color, x0, height, z1, x1, height, z1, x1, height, z0, x0, height, z0);
        quad(pose, vertices, color, x0, 0.0F, z0, x1, 0.0F, z0, x1, 0.0F, z1, x0, 0.0F, z1);
    }

    private static void quad(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            int color,
            float ax,
            float ay,
            float az,
            float bx,
            float by,
            float bz,
            float cx,
            float cy,
            float cz,
            float dx,
            float dy,
            float dz) {
        vertex(pose, vertices, color, ax, ay, az);
        vertex(pose, vertices, color, bx, by, bz);
        vertex(pose, vertices, color, cx, cy, cz);
        vertex(pose, vertices, color, dx, dy, dz);
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer vertices,
            int color,
            float x,
            float y,
            float z) {
        vertices.addVertex(pose, x, y, z).setColor(color);
    }
}
