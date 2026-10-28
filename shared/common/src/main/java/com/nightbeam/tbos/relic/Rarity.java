package com.nightbeam.tbos.relic;

import java.util.Locale;

/** Relic grades. The ordinal is saved on items and in records; append only. */
public enum Rarity {
    COMMON(0xC9C4BA, 1),
    UNCOMMON(0x7FD67A, 2),
    RARE(0x62A9F2, 2),
    EPIC(0xB97AF0, 3),
    LEGENDARY(0xF2A43A, 4),
    REMNANT(0xEA4B3F, 4);

    public final int color;
    public final int affixes;

    Rarity(int color, int affixes) {
        this.color = color;
        this.affixes = affixes;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String key() {
        return "relic.tbos.rarity." + id();
    }

    public static Rarity byOrdinal(int ordinal) {
        Rarity[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }
}
