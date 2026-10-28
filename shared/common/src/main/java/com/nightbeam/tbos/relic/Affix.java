package com.nightbeam.tbos.relic;

import com.nightbeam.tbos.compat.RpgCompat;
import java.util.Locale;

/**
 * Rolled Relic properties. Stat affixes become transient attribute modifiers while
 * the Relic is worn (weapons only in the main hand); the others are read by
 * {@link RelicEffects} in combat. Values scale with the quality of the tier that
 * dropped the Relic and stop at {@link #cap}. Ids are saved on items: append only.
 */
public enum Affix {
    KEEN(true, RpgCompat.Stat.ATTACK_DAMAGE, false, 1.0, 2.5, true, 6.0),
    QUICK(true, RpgCompat.Stat.ATTACK_SPEED, true, 0.04, 0.10, true, 0.25),
    THIRSTING(true, null, false, 0.02, 0.05, true, 0.12),
    SMOLDERING(true, null, false, 0.10, 0.25, false, 0.35),
    ECHOING(true, null, false, 0.06, 0.14, false, 0.25),
    ARCHIVE_BREAKING(true, null, false, 0.12, 0.30, true, 0.60),
    REAPING(true, null, false, 0.15, 0.35, false, 0.60),
    STALWART(false, RpgCompat.Stat.MAX_HEALTH, false, 1.0, 3.0, true, 8.0),
    PLATED(false, RpgCompat.Stat.ARMOR, false, 0.5, 1.5, true, 4.0),
    TEMPERED(false, RpgCompat.Stat.ARMOR_TOUGHNESS, false, 0.5, 1.5, true, 4.0),
    FLEET(false, RpgCompat.Stat.MOVEMENT_SPEED, true, 0.02, 0.05, true, 0.12),
    ROOTED(false, RpgCompat.Stat.KNOCKBACK_RESISTANCE, false, 0.04, 0.10, false, 0.20),
    MIRRORED(false, null, false, 0.08, 0.20, false, 0.35),
    WARDED(false, null, false, 1.0, 3.0, true, 6.0),
    STORIED(false, null, false, 0.04, 0.10, false, 0.15);

    public final boolean weapon;
    public final RpgCompat.Stat stat;
    public final boolean multiply;
    public final double min;
    public final double max;
    public final boolean scales;
    public final double cap;

    Affix(boolean weapon, RpgCompat.Stat stat, boolean multiply, double min, double max, boolean scales, double cap) {
        this.weapon = weapon;
        this.stat = stat;
        this.multiply = multiply;
        this.min = min;
        this.max = max;
        this.scales = scales;
        this.cap = cap;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String key() {
        return "relic.tbos.affix." + id();
    }

    /** Whether the stored value reads as a percentage. */
    public boolean percent() {
        return max < 1.0;
    }

    public static Affix byId(String id) {
        for (Affix affix : values()) {
            if (affix.id().equals(id)) {
                return affix;
            }
        }
        return null;
    }

    /** Remnant-only signature powers; one per Remnant Relic. */
    public enum Signature {
        LAST_HOUR, FRACTURE_STEP, CHOIRS_ECHO, YESTERGLASS;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public String key() {
            return "relic.tbos.signature." + id();
        }

        public static Signature byId(String id) {
            for (Signature signature : values()) {
                if (signature.id().equals(id)) {
                    return signature;
                }
            }
            return null;
        }
    }
}
