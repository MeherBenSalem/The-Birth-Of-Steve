package com.nightbeam.tbos.memory;

/** Stable wire/save IDs: append entries, never reorder. */
public enum MemoryArtifact {
    SPLIT_PRISM, SEEKING_GLASS, EMBER_SCRIPT, STORM_FILAMENT, SHATTER_SEAL, PIERCING_INDEX,
    RETURNING_THREAD, DELAYED_INK, RESONANT_NAIL, MEMORY_WICK, WARD_FRAGMENT, MASONS_REMNANT;
    public String key() { return "memory.tbos.artifact." + name().toLowerCase(java.util.Locale.ROOT); }
}
