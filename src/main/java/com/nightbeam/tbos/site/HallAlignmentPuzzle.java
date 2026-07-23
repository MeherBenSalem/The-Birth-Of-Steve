package com.nightbeam.tbos.site;

import java.util.List;
import net.minecraft.core.Direction;

public final class HallAlignmentPuzzle {
    public static final int FIRST_RECONSTRUCTION_COMPLETE = 1;
    public static final int HALL_ALIGNMENT_COMPLETE = 1 << 7;
    private static final int ORIENTATION_BITS = 2;
    private static final int ORIENTATION_MASK = 0b11;
    private static final int FIRST_ORIENTATION_SHIFT = 1;
    private static final Direction INITIAL_DIRECTION = Direction.NORTH;
    private static final List<Direction> TARGETS = List.of(Direction.WEST, Direction.SOUTH, Direction.EAST);

    private HallAlignmentPuzzle() {
    }

    public static int initialise(int flags) {
        int result = clearOrientations(flags);
        for (int index = 0; index < TARGETS.size(); index++) {
            result = setDirection(result, index, INITIAL_DIRECTION);
        }
        return result;
    }

    public static int reset(int flags) {
        return initialise(flags & ~HALL_ALIGNMENT_COMPLETE);
    }

    public static Direction direction(int flags, int mechanismIndex) {
        validateIndex(mechanismIndex);
        int shift = FIRST_ORIENTATION_SHIFT + mechanismIndex * ORIENTATION_BITS;
        return Direction.from2DDataValue((flags >> shift) & ORIENTATION_MASK);
    }

    public static int rotateClockwise(int flags, int mechanismIndex) {
        return setDirection(flags, mechanismIndex, direction(flags, mechanismIndex).getClockWise());
    }

    public static boolean isAligned(int flags, int mechanismIndex) {
        validateIndex(mechanismIndex);
        return direction(flags, mechanismIndex) == TARGETS.get(mechanismIndex);
    }

    public static boolean allAligned(int flags) {
        for (int index = 0; index < TARGETS.size(); index++) {
            if (!isAligned(flags, index)) {
                return false;
            }
        }
        return true;
    }

    public static int markComplete(int flags) {
        return flags | HALL_ALIGNMENT_COMPLETE;
    }

    private static int setDirection(int flags, int mechanismIndex, Direction direction) {
        validateIndex(mechanismIndex);
        if (!direction.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("Alignment direction must be horizontal");
        }
        int shift = FIRST_ORIENTATION_SHIFT + mechanismIndex * ORIENTATION_BITS;
        int cleared = flags & ~(ORIENTATION_MASK << shift);
        return cleared | direction.get2DDataValue() << shift;
    }

    private static int clearOrientations(int flags) {
        int allOrientationBits = ((1 << (TARGETS.size() * ORIENTATION_BITS)) - 1) << FIRST_ORIENTATION_SHIFT;
        return flags & ~allOrientationBits;
    }

    private static void validateIndex(int mechanismIndex) {
        if (mechanismIndex < 0 || mechanismIndex >= TARGETS.size()) {
            throw new IllegalArgumentException("Alignment mechanism index out of bounds: " + mechanismIndex);
        }
    }
}
