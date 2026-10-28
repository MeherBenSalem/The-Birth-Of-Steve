package com.nightbeam.tbos.codex;

import com.nightbeam.tbos.expedition.Distortion;
import com.nightbeam.tbos.expedition.ExpeditionKey;
import com.nightbeam.tbos.rpg.RpgConfig;
import java.time.LocalDate;
import java.time.ZoneOffset;
import net.minecraft.server.MinecraftServer;

/**
 * The Weekly Recollection: one fixed seed, tier and Distortion set for the whole
 * server, derived from the world seed and the week. Weeks start Monday 00:00 UTC,
 * so every player on a server races the same dungeon until the next Monday.
 */
public final class Weekly {
    private Weekly() {
    }

    /** Week number counted from Monday 29 December 1969. */
    public static long currentWeek() {
        long epochDay = LocalDate.now(ZoneOffset.UTC).toEpochDay();
        return Math.floorDiv(epochDay + 3, 7);
    }

    /** Seconds until the next week begins. */
    public static long secondsLeft() {
        long now = java.time.Instant.now().getEpochSecond();
        long nextWeekDay = (currentWeek() + 1) * 7 - 3;
        return Math.max(0, nextWeekDay * 86400L - now);
    }

    public static ExpeditionKey key(MinecraftServer server, long week) {
        long seed = mix(server.overworld().getSeed() ^ (week * 0x9E3779B97F4A7C15L));
        RpgConfig.Weekly config = RpgConfig.get().weekly;
        int span = config.maxTier - config.minTier + 1;
        int tier = config.minTier + (int) Math.floorMod(mix(seed ^ 0x51ED270B), span);
        int mask = 0;
        Distortion[] all = Distortion.values();
        long roll = mix(seed ^ 0x2545F4914F6CDD1DL);
        for (int picked = 0, guard = 0; picked < config.distortions && guard < 64; guard++) {
            Distortion candidate = all[(int) Math.floorMod(roll, all.length)];
            roll = mix(roll + guard + 1);
            if (!candidate.in(mask)) {
                mask |= candidate.bit();
                picked++;
            }
        }
        return new ExpeditionKey(tier, mask, week, seed);
    }

    private static long mix(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
