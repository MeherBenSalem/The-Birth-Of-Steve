package com.nightbeam.tbos.memory;

/** Stable wire/save IDs: append entries, never reorder. Cooldowns are ticks. */
public enum MemoryAbility {
    ECHO_LANCE(24), RECALL(160), PARALLAX_STEP(100), RESONANT_GUARD(100), RECONSTRUCT(120), MEMORY_WELL(200);
    public final int cooldown;
    MemoryAbility(int cooldown) { this.cooldown = cooldown; }
    public String key() { return "memory.tbos.ability." + name().toLowerCase(java.util.Locale.ROOT); }
}
