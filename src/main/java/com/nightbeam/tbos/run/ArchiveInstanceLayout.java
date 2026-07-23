package com.nightbeam.tbos.run;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Maps durable allocation slots to isolated, bounded cells in the archive dimension. */
public final class ArchiveInstanceLayout {
    public static final int CELL_SIZE = 1536;
    public static final int GRID_COLUMNS = 16;
    public static final int MAX_INSTANCE_SLOTS = 256;
    public static final int BASE_Y = 96;
    public static final int RUN_WIDTH = 1344;
    public static final int RUN_HEIGHT = 192;
    public static final int RUN_DEPTH = 1344;

    private ArchiveInstanceLayout() {
    }

    public static BlockPos originForSlot(int slot) {
        validateSlot(slot);
        int column = slot % GRID_COLUMNS;
        int row = slot / GRID_COLUMNS;
        return new BlockPos(
                (column - GRID_COLUMNS / 2) * CELL_SIZE,
                BASE_Y,
                (row - GRID_COLUMNS / 2) * CELL_SIZE);
    }

    public static BoundingBox boundsForSlot(int slot) {
        BlockPos origin = originForSlot(slot);
        int halfWidth = RUN_WIDTH / 2;
        int halfDepth = RUN_DEPTH / 2;
        return new BoundingBox(
                origin.getX() - halfWidth,
                BASE_Y - 80,
                origin.getZ() - halfDepth,
                origin.getX() + halfWidth - 1,
                BASE_Y - 80 + RUN_HEIGHT - 1,
                origin.getZ() + halfDepth - 1);
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= MAX_INSTANCE_SLOTS) {
            throw new IllegalArgumentException(
                    "Archive instance slot must be between 0 and " + (MAX_INSTANCE_SLOTS - 1) + ": " + slot);
        }
    }
}
