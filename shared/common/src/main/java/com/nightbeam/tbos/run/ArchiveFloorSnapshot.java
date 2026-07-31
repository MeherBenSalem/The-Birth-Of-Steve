package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;

/** Persisted geometry identity for a completed floor awaiting staged deletion. */
public record ArchiveFloorSnapshot(
        long seed,
        int instanceSlot,
        ArchiveDungeonGraph dungeonGraph) {
    public static final Codec<ArchiveFloorSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.LONG.fieldOf("seed").forGetter(ArchiveFloorSnapshot::seed),
            Codec.INT.fieldOf("instance_slot").forGetter(ArchiveFloorSnapshot::instanceSlot),
            ArchiveDungeonGraph.CODEC.fieldOf("dungeon_graph").forGetter(ArchiveFloorSnapshot::dungeonGraph)
    ).apply(instance, ArchiveFloorSnapshot::new));

    public ArchiveFloorSnapshot {
        dungeonGraph = Objects.requireNonNull(dungeonGraph, "dungeonGraph");
        if (instanceSlot < 0 || instanceSlot >= ArchiveInstanceLayout.MAX_INSTANCE_SLOTS) {
            throw new IllegalArgumentException("Retired Archive floor has an invalid instance slot: " + instanceSlot);
        }
        if (dungeonGraph.seed() != seed) {
            throw new IllegalArgumentException("Retired Archive floor graph does not match its seed");
        }
    }
}
