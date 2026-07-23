package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

/** A template-safe quarter rotation followed by an optional local X mirror. */
public record ArchiveTransform(int quarterTurns, boolean mirrored) {
    public static final ArchiveTransform IDENTITY = new ArchiveTransform(0, false);
    public static final Codec<ArchiveTransform> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("quarter_turns", 0).forGetter(ArchiveTransform::quarterTurns),
            Codec.BOOL.optionalFieldOf("mirrored", false).forGetter(ArchiveTransform::mirrored)
    ).apply(instance, ArchiveTransform::new));

    public ArchiveTransform {
        quarterTurns = Math.floorMod(quarterTurns, 4);
    }

    public ArchiveDirection apply(ArchiveDirection direction) {
        if (direction.vertical()) {
            return direction;
        }
        ArchiveDirection transformed = direction;
        if (mirrored) {
            transformed = switch (transformed) {
                case EAST -> ArchiveDirection.WEST;
                case WEST -> ArchiveDirection.EAST;
                default -> transformed;
            };
        }
        for (int turn = 0; turn < quarterTurns; turn++) {
            transformed = switch (transformed) {
                case NORTH -> ArchiveDirection.EAST;
                case EAST -> ArchiveDirection.SOUTH;
                case SOUTH -> ArchiveDirection.WEST;
                case WEST -> ArchiveDirection.NORTH;
                default -> transformed;
            };
        }
        return transformed;
    }

    public BlockPos apply(BlockPos position, ArchiveRoomSize size) {
        int x = position.getX();
        int z = position.getZ();
        int width = size.width();
        int depth = size.depth();
        if (mirrored) {
            x = width - 1 - x;
        }
        for (int turn = 0; turn < quarterTurns; turn++) {
            int nextX = depth - 1 - z;
            int nextZ = x;
            x = nextX;
            z = nextZ;
            int oldWidth = width;
            width = depth;
            depth = oldWidth;
        }
        return new BlockPos(x, position.getY(), z);
    }
}
