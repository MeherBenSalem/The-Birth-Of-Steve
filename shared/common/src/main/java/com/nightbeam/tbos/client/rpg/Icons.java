package com.nightbeam.tbos.client.rpg;

/**
 * Cells of {@code textures/gui/rpg/icons.png}: an 8-column sheet of 128-pixel painted
 * icons in the Greek GUI's bronze-and-patina style. The authoring source and the
 * cell order live in {@code tools/rpg/icons.py}; keep both in step.
 */
public final class Icons {
    /** 0–7: the Distortions, in {@code Distortion} order. */
    public static int distortion(int ordinal) {
        return ordinal;
    }

    /** 8–23: the Insights, in {@code Insight} order. */
    public static int insight(int ordinal) {
        return 8 + ordinal;
    }

    /** 24–29: the Relic rarities, in {@code Rarity} order. */
    public static int rarity(int ordinal) {
        return 24 + ordinal;
    }

    public static final int RENOWN = 30;
    public static final int KEY = 31;
    public static final int HOURGLASS = 32;
    public static final int PARTY = 33;
    public static final int WEEKLY = 34;
    public static final int RECORDS = 35;
    public static final int BESTIARY = 36;
    public static final int RELIC = 37;
    public static final int REMEMBER = 38;
    public static final int FORGET = 39;
    public static final int MOMENTUM = 40;
    public static final int INSIGHT = 41;
    public static final int LEADER = 42;
    public static final int ARCHIVE = 43;
    public static final int SEALED = 44;
    public static final int COUNT = 45;

    private Icons() {
    }
}
