package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** One persisted, directed half of a reciprocal room connection. */
public record ArchiveConnection(int targetRoom, ArchiveDirection direction, boolean hidden, boolean locked) {
    public static final Codec<ArchiveConnection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("target_room").forGetter(ArchiveConnection::targetRoom),
            ArchiveDirection.CODEC.fieldOf("direction").forGetter(ArchiveConnection::direction),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(ArchiveConnection::hidden),
            Codec.BOOL.optionalFieldOf("locked", false).forGetter(ArchiveConnection::locked)
    ).apply(instance, ArchiveConnection::new));

    public ArchiveConnection withLocked(boolean value) {
        return new ArchiveConnection(targetRoom, direction, hidden, value);
    }

    public ArchiveConnection reveal() {
        return new ArchiveConnection(targetRoom, direction, false, locked);
    }
}
