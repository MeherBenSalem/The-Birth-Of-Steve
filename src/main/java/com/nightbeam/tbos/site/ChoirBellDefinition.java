package com.nightbeam.tbos.site;

import java.util.List;
import net.minecraft.core.BlockPos;

public record ChoirBellDefinition(
        BlockPos position,
        int symbol,
        float pitch,
        List<BlockPos> imprintPositions) {

    public ChoirBellDefinition {
        if (symbol < 0 || symbol > 3) {
            throw new IllegalArgumentException("Choir bell symbol must be between 0 and 3");
        }
        if (pitch < 0.5F || pitch > 2.0F) {
            throw new IllegalArgumentException("Choir bell pitch must be between 0.5 and 2.0");
        }
        position = position.immutable();
        imprintPositions = imprintPositions.stream().map(BlockPos::immutable).distinct().toList();
        if (imprintPositions.isEmpty()) {
            throw new IllegalArgumentException("Choir bells need at least one visual imprint marker");
        }
    }

    public List<BlockPos> allPositions() {
        java.util.ArrayList<BlockPos> positions = new java.util.ArrayList<>(imprintPositions.size() + 1);
        positions.add(position);
        positions.addAll(imprintPositions);
        return List.copyOf(positions);
    }
}
