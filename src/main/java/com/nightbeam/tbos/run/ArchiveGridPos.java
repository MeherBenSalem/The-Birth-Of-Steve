package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Logical, instance-local room coordinates. */
public record ArchiveGridPos(int x, int y, int z) {
    public static final Codec<ArchiveGridPos> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(ArchiveGridPos::x),
            Codec.INT.fieldOf("y").forGetter(ArchiveGridPos::y),
            Codec.INT.fieldOf("z").forGetter(ArchiveGridPos::z)
    ).apply(instance, ArchiveGridPos::new));

    public ArchiveGridPos offset(ArchiveDirection direction) {
        return new ArchiveGridPos(
                x + direction.stepX(),
                y + direction.stepY(),
                z + direction.stepZ());
    }

    public ArchiveDirection directionTo(ArchiveGridPos other) {
        int dx = other.x - x;
        int dy = other.y - y;
        int dz = other.z - z;
        for (ArchiveDirection direction : ArchiveDirection.values()) {
            if (direction.stepX() == dx && direction.stepY() == dy && direction.stepZ() == dz) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Archive positions are not cardinal neighbors: " + this + " -> " + other);
    }
}
