package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Locale;

/** The six directions in which procedural archive rooms may connect. */
public enum ArchiveDirection {
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    EAST(1, 0, 0),
    WEST(-1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1, 0);

    public static final Codec<ArchiveDirection> CODEC = Codec.STRING.comapFlatMap(
            value -> {
                try {
                    return DataResult.success(valueOf(value.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException exception) {
                    return DataResult.error(() -> "Unknown archive direction: " + value);
                }
            },
            direction -> direction.name().toLowerCase(Locale.ROOT));

    private final int stepX;
    private final int stepY;
    private final int stepZ;

    ArchiveDirection(int stepX, int stepY, int stepZ) {
        this.stepX = stepX;
        this.stepY = stepY;
        this.stepZ = stepZ;
    }

    public int stepX() {
        return stepX;
    }

    public int stepY() {
        return stepY;
    }

    public int stepZ() {
        return stepZ;
    }

    public boolean vertical() {
        return stepY != 0;
    }

    public ArchiveDirection opposite() {
        return switch (this) {
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case EAST -> WEST;
            case WEST -> EAST;
            case UP -> DOWN;
            case DOWN -> UP;
        };
    }
}
