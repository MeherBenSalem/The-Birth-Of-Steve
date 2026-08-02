package com.nightbeam.tbos.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Shared untextured box emission for the 1.21.1 block-entity renderer API. */
public final class ArchiveGeometry {
    private ArchiveGeometry() {
    }

    public static void submitBox(
            PoseStack poseStack,
            MultiBufferSource buffers,
            float centerX,
            float bottomY,
            float centerZ,
            float width,
            float height,
            float depth,
            int color) {
        poseStack.pushPose();
        poseStack.translate(centerX, bottomY, centerZ);
        drawBox(poseStack.last(), buffers.getBuffer(RenderType.debugQuads()), width, height, depth, color);
        poseStack.popPose();
    }

    public static void submitCenteredBox(
            PoseStack poseStack,
            MultiBufferSource buffers,
            float centerX,
            float centerY,
            float centerZ,
            float width,
            float height,
            float depth,
            int color) {
        submitBox(poseStack, buffers, centerX, centerY - height * 0.5F, centerZ, width, height, depth, color);
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
            PoseStack.Pose pose, VertexConsumer vertices, int color, float x, float y, float z) {
        vertices.addVertex(pose, x, y, z).setColor(color);
    }
}
