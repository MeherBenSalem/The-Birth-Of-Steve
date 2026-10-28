package com.nightbeam.tbos.expedition;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Heat modifiers attuned onto a Memory Key. Each adds one Heat; rewards scale with
 * tier + Heat ("Fracture"). The ordinal is the bit stored on keys and runs, so
 * append new entries and never reorder.
 */
public enum Distortion {
    FORTIFIED,
    TYRANNICAL,
    BOLSTERING,
    VOLATILE,
    HAUNTED,
    FRAILTY,
    HOURGLASS,
    AMNESIA;

    public int bit() {
        return 1 << ordinal();
    }

    public boolean in(int mask) {
        return (mask & bit()) != 0;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String key() {
        return "expedition.tbos.distortion." + id();
    }

    public static int mask() {
        return (1 << values().length) - 1;
    }

    public static int heat(int mask) {
        return Integer.bitCount(mask & mask());
    }

    public static List<Distortion> of(int mask) {
        List<Distortion> list = new ArrayList<>();
        for (Distortion distortion : values()) {
            if (distortion.in(mask)) {
                list.add(distortion);
            }
        }
        return list;
    }
}
