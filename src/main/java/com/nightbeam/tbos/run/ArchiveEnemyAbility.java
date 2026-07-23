package com.nightbeam.tbos.run;

/** Deterministic combat mutations attached to spawned Archive enemies. */
public enum ArchiveEnemyAbility {
    ECHO_BOLT("echo_bolt"),
    SPLITTER("splitter"),
    PARALLAX_BLINK("parallax_blink"),
    MERIDIAN_SHOCKWAVE("meridian_shockwave"),
    WARD_AURA("ward_aura");

    private static final String TAG_PREFIX = "tbos.ability.";
    private final String serializedName;

    ArchiveEnemyAbility(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }

    public String entityTag() {
        return TAG_PREFIX + serializedName;
    }
}
