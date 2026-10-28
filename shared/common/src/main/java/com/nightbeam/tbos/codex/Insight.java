package com.nightbeam.tbos.codex;

import java.util.Locale;

/**
 * Permanent Codex perks bought with Insight (one per Renown level). Stable save
 * names come from {@link #id()}; append new entries, never rename.
 */
public enum Insight {
    STEADY_HAND(1),
    DEEP_RECALL(3),
    KEEN_EYE(3),
    ARCHIVISTS_GRACE(3),
    BULWARK_MEMORY(3),
    ECHO_STRIKE(3),
    VAMPIRIC_RECALL(2),
    HOURGLASS_MASTERY(2),
    SWIFT_RECOLLECTION(1),
    WAYSTONE_WARD(1),
    SECOND_DRAFT(1),
    LUCID_START(1),
    KEYMAKER(1),
    HOARDER(1),
    PARTY_BOND(1),
    LAST_STAND(1);

    public final int maxRanks;

    Insight(int maxRanks) {
        this.maxRanks = maxRanks;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String key() {
        return "codex.tbos.insight." + id();
    }

    public static Insight byId(String id) {
        for (Insight insight : values()) {
            if (insight.id().equals(id)) {
                return insight;
            }
        }
        return null;
    }

    public static int totalRanks() {
        int total = 0;
        for (Insight insight : values()) {
            total += insight.maxRanks;
        }
        return total;
    }
}
