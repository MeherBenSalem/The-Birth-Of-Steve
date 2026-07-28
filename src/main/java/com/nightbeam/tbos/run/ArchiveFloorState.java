package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;

/** Unbounded run-floor number plus durable cleanup work left by earlier floors. */
public record ArchiveFloorState(
        long floor, List<ArchiveFloorSnapshot> retiredFloors, ArchiveRunMode runMode) {
    public static final ArchiveFloorState FIRST =
            new ArchiveFloorState(0L, List.of(), ArchiveRunMode.NORMAL);
    public static final Codec<ArchiveFloorState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.optionalFieldOf("floor", 0L).forGetter(ArchiveFloorState::floor),
            ArchiveFloorSnapshot.CODEC.listOf().optionalFieldOf("retired_floors", List.of())
                    .forGetter(ArchiveFloorState::retiredFloors),
            ArchiveRunMode.CODEC.optionalFieldOf("run_mode", ArchiveRunMode.NORMAL)
                    .forGetter(ArchiveFloorState::runMode)
    ).apply(instance, ArchiveFloorState::new));

    public ArchiveFloorState {
        retiredFloors = List.copyOf(retiredFloors);
        runMode = java.util.Objects.requireNonNull(runMode, "runMode");
        if (floor < 0L) {
            throw new IllegalArgumentException("Archive floor must not be negative");
        }
        if (new HashSet<>(retiredFloors.stream().map(ArchiveFloorSnapshot::instanceSlot).toList()).size()
                != retiredFloors.size()) {
            throw new IllegalArgumentException("Retired Archive floor slots must be unique");
        }
    }

    public ArchiveFloorState advance(ArchiveFloorSnapshot completedFloor) {
        if (floor == Long.MAX_VALUE) {
            throw new IllegalStateException("Archive floor counter exhausted");
        }
        java.util.ArrayList<ArchiveFloorSnapshot> retired = new java.util.ArrayList<>(retiredFloors);
        retired.add(completedFloor);
        return new ArchiveFloorState(floor + 1L, retired, runMode);
    }

    public ArchiveFloorState removeRetiredSlot(int instanceSlot) {
        List<ArchiveFloorSnapshot> remaining = retiredFloors.stream()
                .filter(snapshot -> snapshot.instanceSlot() != instanceSlot)
                .toList();
        return remaining.size() == retiredFloors.size()
                ? this
                : new ArchiveFloorState(floor, remaining, runMode);
    }
}
