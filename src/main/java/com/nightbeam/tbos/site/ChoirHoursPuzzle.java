package com.nightbeam.tbos.site;

import java.util.List;

public final class ChoirHoursPuzzle {
    public static final int CHOIR_COMPLETE = 1 << 8;
    private static final int CURSOR_SHIFT = 1;
    private static final int CURSOR_MASK = 0b111;
    private static final int FAILURES_SHIFT = 4;
    private static final int FAILURES_MASK = 0b11;
    private static final List<Integer> SEQUENCE = List.of(0, 2, 1, 3);

    private ChoirHoursPuzzle() {
    }

    public static List<Integer> sequence() {
        return SEQUENCE;
    }

    public static int initialise(int flags) {
        return clearField(clearField(flags, CURSOR_MASK, CURSOR_SHIFT), FAILURES_MASK, FAILURES_SHIFT)
                & ~CHOIR_COMPLETE;
    }

    public static int cursor(int flags) {
        return flags >> CURSOR_SHIFT & CURSOR_MASK;
    }

    public static int failedAttempts(int flags) {
        return flags >> FAILURES_SHIFT & FAILURES_MASK;
    }

    public static Submission submit(int flags, int bellIndex) {
        if (bellIndex < 0 || bellIndex > 3) {
            throw new IllegalArgumentException("Choir bell index out of bounds: " + bellIndex);
        }
        if ((flags & CHOIR_COMPLETE) != 0) {
            return new Submission(flags, true, true, failedAttempts(flags) >= 2, cursor(flags));
        }

        int cursor = cursor(flags);
        boolean correct = bellIndex == SEQUENCE.get(cursor);
        if (!correct) {
            int failures = Math.min(FAILURES_MASK, failedAttempts(flags) + 1);
            int updated = setField(flags, CURSOR_MASK, CURSOR_SHIFT, 0);
            updated = setField(updated, FAILURES_MASK, FAILURES_SHIFT, failures);
            return new Submission(updated, false, false, failures >= 2, 0);
        }

        int nextCursor = cursor + 1;
        if (nextCursor == SEQUENCE.size()) {
            int completed = setField(flags | CHOIR_COMPLETE, CURSOR_MASK, CURSOR_SHIFT, 0);
            return new Submission(completed, true, true, failedAttempts(flags) >= 2, 0);
        }
        int updated = setField(flags, CURSOR_MASK, CURSOR_SHIFT, nextCursor);
        return new Submission(updated, true, false, failedAttempts(flags) >= 2, nextCursor);
    }

    public static int resetAttempt(int flags) {
        int reset = setField(flags, CURSOR_MASK, CURSOR_SHIFT, 0);
        return setField(reset, FAILURES_MASK, FAILURES_SHIFT, 0);
    }

    private static int setField(int flags, int mask, int shift, int value) {
        return clearField(flags, mask, shift) | (value & mask) << shift;
    }

    private static int clearField(int flags, int mask, int shift) {
        return flags & ~(mask << shift);
    }

    public record Submission(
            int progressFlags,
            boolean correct,
            boolean complete,
            boolean showStrongHint,
            int nextExpectedIndex) {
    }
}
