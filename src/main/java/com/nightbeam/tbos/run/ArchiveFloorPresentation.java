package com.nightbeam.tbos.run;

import net.minecraft.network.chat.Component;

/** Shared one-based floor presentation and capped endless-run difficulty curve. */
public final class ArchiveFloorPresentation {
    public static final int NAME_COUNT = 8;
    public static final int MAX_DIFFICULTY_BONUS = 40;
    public static final int MAX_ADDITIONAL_ROOMS = 6;
    public static final int MAX_ADDITIONAL_ENEMIES = 4;

    private ArchiveFloorPresentation() {
    }

    public static long displayFloor(long floorIndex) {
        requireFloor(floorIndex);
        return floorIndex == Long.MAX_VALUE ? Long.MAX_VALUE : floorIndex + 1L;
    }

    public static int nameIndex(long floorIndex) {
        requireFloor(floorIndex);
        return (int) (floorIndex % NAME_COUNT);
    }

    public static ArchiveFloorTheme theme(long floorIndex) {
        return ArchiveFloorTheme.of(floorIndex);
    }

    public static long echoCycle(long floorIndex) {
        requireFloor(floorIndex);
        return floorIndex / NAME_COUNT + 1L;
    }

    public static Component name(long floorIndex) {
        Component base = Component.translatable("floor.tbos.name." + nameIndex(floorIndex));
        long cycle = echoCycle(floorIndex);
        return cycle == 1L
                ? base
                : Component.translatable("floor.tbos.name.echo", base, cycleLabel(cycle));
    }

    public static int difficultyBonus(long floorIndex) {
        requireFloor(floorIndex);
        return (int) Math.min(MAX_DIFFICULTY_BONUS, Math.min(floorIndex, 10L) * 4L);
    }

    public static int additionalRooms(long floorIndex) {
        requireFloor(floorIndex);
        return (int) Math.min(MAX_ADDITIONAL_ROOMS, floorIndex / 2L);
    }

    public static int additionalEnemies(long floorIndex) {
        requireFloor(floorIndex);
        return (int) Math.min(MAX_ADDITIONAL_ENEMIES, floorIndex / 3L);
    }

    static String cycleLabel(long cycle) {
        if (cycle < 1L) {
            throw new IllegalArgumentException("Archive echo cycle must be positive");
        }
        if (cycle > 3_999L) {
            return Long.toString(cycle);
        }
        int remaining = (int) cycle;
        StringBuilder roman = new StringBuilder();
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] glyphs = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        for (int index = 0; index < values.length; index++) {
            while (remaining >= values[index]) {
                roman.append(glyphs[index]);
                remaining -= values[index];
            }
        }
        return roman.toString();
    }

    private static void requireFloor(long floorIndex) {
        if (floorIndex < 0L) {
            throw new IllegalArgumentException("Archive floor index must not be negative");
        }
    }
}
