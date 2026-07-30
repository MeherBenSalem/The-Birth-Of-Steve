package com.nightbeam.tbos.site;

public final class BrokenMeridianPuzzle {
    public static final int POSITION_COUNT = 3;
    public static final int TARGET_POSITION = 2;
    public static final int ROUTE_ARMED = 1 << 2;
    public static final int BROKEN_MERIDIAN_COMPLETE = 1 << 4;
    private static final int POSITION_MASK = 0b11;

    private BrokenMeridianPuzzle() {
    }

    public static int initialise(int flags) {
        return ((flags & ~POSITION_MASK) & ~ROUTE_ARMED) & ~BROKEN_MERIDIAN_COMPLETE;
    }

    public static int position(int flags) {
        int position = flags & POSITION_MASK;
        return position < POSITION_COUNT ? position : 0;
    }

    public static Move advance(int flags) {
        if (isComplete(flags)) {
            return new Move(flags, position(flags), true);
        }
        if (!isRouteArmed(flags)) {
            return new Move(flags, position(flags), false);
        }
        int nextPosition = (position(flags) + 1) % POSITION_COUNT;
        int updated = ((flags & ~POSITION_MASK) & ~ROUTE_ARMED) | nextPosition;
        boolean complete = nextPosition == TARGET_POSITION;
        if (complete) {
            updated |= BROKEN_MERIDIAN_COMPLETE;
        }
        return new Move(updated, nextPosition, complete);
    }

    public static int reset(int flags) {
        return initialise(flags);
    }

    public static int armRoute(int flags) {
        return isComplete(flags) ? flags : flags | ROUTE_ARMED;
    }

    public static boolean isRouteArmed(int flags) {
        return (flags & ROUTE_ARMED) != 0;
    }

    public static int nextPosition(int flags) {
        return Math.min(position(flags) + 1, TARGET_POSITION);
    }

    public static boolean isComplete(int flags) {
        return (flags & BROKEN_MERIDIAN_COMPLETE) != 0;
    }

    public record Move(int progressFlags, int position, boolean complete) {
    }
}
