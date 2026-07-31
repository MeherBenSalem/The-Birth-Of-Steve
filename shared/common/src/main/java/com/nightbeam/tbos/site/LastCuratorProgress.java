package com.nightbeam.tbos.site;

public final class LastCuratorProgress {
    public static final int MAX_HEALTH = 300;
    public static final int STARTED = 1 << 9;
    public static final int DEFEATED = 1 << 10;
    public static final int REWARD_GRANTED = 1 << 11;
    private static final int HEALTH_MASK = 0x1FF;

    private LastCuratorProgress() {
    }

    public static int start(int flags) {
        return setHealth((flags | STARTED) & ~(DEFEATED | REWARD_GRANTED), MAX_HEALTH);
    }

    public static int health(int flags) {
        return flags & HEALTH_MASK;
    }

    public static int recordHealth(int flags, int health) {
        if (!isStarted(flags) || isDefeated(flags)) {
            return flags;
        }
        if (health <= 0) {
            return setHealth(flags | DEFEATED, 0);
        }
        return setHealth(flags, Math.min(MAX_HEALTH, health));
    }

    public static int markRewardGranted(int flags) {
        if (!isDefeated(flags)) {
            throw new IllegalStateException("The Curator reward cannot be granted before defeat");
        }
        return flags | REWARD_GRANTED;
    }

    public static boolean isStarted(int flags) {
        return (flags & STARTED) != 0;
    }

    public static boolean isDefeated(int flags) {
        return (flags & DEFEATED) != 0;
    }

    public static boolean isRewardGranted(int flags) {
        return (flags & REWARD_GRANTED) != 0;
    }

    public static Phase phase(int flags) {
        int health = health(flags);
        if (health > 200) {
            return Phase.CATALOGUE;
        }
        if (health > 100) {
            return Phase.REVISION;
        }
        return Phase.ERASURE;
    }

    public static boolean isVulnerable(int flags, TemporalState state) {
        if (!isStarted(flags) || isDefeated(flags) || !state.isStable()) {
            return false;
        }
        return switch (phase(flags)) {
            case CATALOGUE -> state == TemporalState.REMEMBERED;
            case REVISION -> state == TemporalState.RUIN;
            case ERASURE -> true;
        };
    }

    private static int setHealth(int flags, int health) {
        if (health < 0 || health > MAX_HEALTH) {
            throw new IllegalArgumentException("Curator health is out of bounds: " + health);
        }
        return (flags & ~HEALTH_MASK) | health;
    }

    public enum Phase {
        CATALOGUE,
        REVISION,
        ERASURE
    }
}
