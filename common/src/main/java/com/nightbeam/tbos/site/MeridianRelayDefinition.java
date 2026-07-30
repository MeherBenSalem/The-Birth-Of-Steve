package com.nightbeam.tbos.site;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;

public record MeridianRelayDefinition(
        List<BlockPos> positions,
        List<List<BlockPos>> powerChannels,
        int targetPosition) {

    public MeridianRelayDefinition {
        positions = positions.stream().map(BlockPos::immutable).distinct().toList();
        powerChannels = powerChannels.stream()
                .map(channel -> channel.stream().map(BlockPos::immutable).distinct().toList())
                .map(List::copyOf)
                .toList();
        if (positions.size() != BrokenMeridianPuzzle.POSITION_COUNT
                || powerChannels.size() != positions.size()) {
            throw new IllegalArgumentException("A Meridian relay requires one power channel for each authored position");
        }
        if (powerChannels.stream().anyMatch(List::isEmpty)) {
            throw new IllegalArgumentException("Meridian relay power channels cannot be empty");
        }
        if (targetPosition < 0 || targetPosition >= positions.size()) {
            throw new IllegalArgumentException("Meridian relay target position is out of bounds");
        }
    }

    public List<BlockPos> allPositions() {
        return Stream.concat(positions.stream(), powerChannels.stream().flatMap(List::stream))
                .distinct()
                .toList();
    }
}
