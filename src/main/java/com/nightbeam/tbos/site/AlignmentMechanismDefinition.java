package com.nightbeam.tbos.site;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record AlignmentMechanismDefinition(
        BlockPos position,
        Direction targetDirection,
        BlockPos target,
        List<BlockPos> beamSegments) {

    public AlignmentMechanismDefinition {
        if (!targetDirection.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("Alignment targets must use a horizontal direction");
        }
        position = position.immutable();
        target = target.immutable();
        beamSegments = beamSegments.stream().map(BlockPos::immutable).distinct().toList();
        if (beamSegments.isEmpty()) {
            throw new IllegalArgumentException("Alignment mechanisms need at least one beam segment");
        }
    }

    public List<BlockPos> allPositions() {
        java.util.ArrayList<BlockPos> positions = new java.util.ArrayList<>(beamSegments.size() + 2);
        positions.add(position);
        positions.add(target);
        positions.addAll(beamSegments);
        return List.copyOf(positions);
    }
}
