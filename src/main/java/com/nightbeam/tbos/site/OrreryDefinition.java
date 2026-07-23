package com.nightbeam.tbos.site;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;

public record OrreryDefinition(
        BlockPos archiveCore,
        BlockPos bossSpawn,
        List<BlockPos> memoryAnchors,
        List<BlockPos> rememberedRingSegments) {

    public OrreryDefinition {
        archiveCore = archiveCore.immutable();
        bossSpawn = bossSpawn.immutable();
        memoryAnchors = memoryAnchors.stream().map(BlockPos::immutable).distinct().toList();
        rememberedRingSegments = rememberedRingSegments.stream().map(BlockPos::immutable).distinct().toList();
        if (memoryAnchors.size() < 3) {
            throw new IllegalArgumentException("The Grand Orrery requires at least three Memory Anchors");
        }
        if (rememberedRingSegments.isEmpty()) {
            throw new IllegalArgumentException("The Grand Orrery requires remembered ring segments");
        }
        if (rememberedRingSegments.contains(archiveCore)
                || rememberedRingSegments.contains(bossSpawn)
                || memoryAnchors.stream().anyMatch(rememberedRingSegments::contains)) {
            throw new IllegalArgumentException("Grand Orrery rings cannot overwrite encounter mechanisms");
        }
    }

    public List<BlockPos> allPositions() {
        return Stream.concat(
                        Stream.of(archiveCore, bossSpawn),
                        Stream.concat(memoryAnchors.stream(), rememberedRingSegments.stream()))
                .distinct()
                .toList();
    }
}
