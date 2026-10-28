package com.nightbeam.tbos.codex;

import java.util.List;

/**
 * The Codex's catalogue of Archive creatures. Kills are counted for any of these,
 * anywhere. {@code home} is a floor-name index (see {@code floor.tbos.name.N}), or
 * -1 for creatures found on every floor, -2 for the campaign.
 */
public final class Bestiary {
    public enum Role { CAMPAIGN, ORIGINAL, WARDEN, EXCLUSIVE, BOSS }

    public record Entry(String id, Role role, int home, int threat) {
        public String path() {
            return id.substring(id.indexOf(':') + 1);
        }

        public String nameKey() {
            return "entity.tbos." + path();
        }

        public String loreKey() {
            return "codex.tbos.bestiary." + path();
        }
    }

    public static final List<Entry> ENTRIES = List.of(
            new Entry("tbos:phoenix_guardian", Role.CAMPAIGN, -2, 5),
            new Entry("tbos:parallax_wraith", Role.ORIGINAL, -1, 2),
            new Entry("tbos:meridian_sentinel", Role.ORIGINAL, -1, 3),
            new Entry("tbos:memory_leech", Role.ORIGINAL, -1, 1),
            new Entry("tbos:lensward", Role.ORIGINAL, -1, 3),
            new Entry("tbos:minotaur", Role.WARDEN, -1, 4),
            new Entry("tbos:shard_drifter", Role.EXCLUSIVE, 0, 2),
            new Entry("tbos:wake_cutter", Role.BOSS, 0, 4),
            new Entry("tbos:gallery_moth", Role.EXCLUSIVE, 1, 2),
            new Entry("tbos:null_portrait", Role.BOSS, 1, 4),
            new Entry("tbos:armillary_scout", Role.EXCLUSIVE, 2, 2),
            new Entry("tbos:gnomon_knight", Role.BOSS, 2, 4),
            new Entry("tbos:ash_chorister", Role.EXCLUSIVE, 3, 3),
            new Entry("tbos:dust_cantorile", Role.BOSS, 3, 4),
            new Entry("tbos:shardling_swarm", Role.EXCLUSIVE, 4, 2),
            new Entry("tbos:prism_stalker", Role.BOSS, 4, 4),
            new Entry("tbos:shelf_crawler", Role.EXCLUSIVE, 5, 2),
            new Entry("tbos:index_wight", Role.BOSS, 5, 4),
            new Entry("tbos:metronome_hound", Role.EXCLUSIVE, 6, 2),
            new Entry("tbos:labyrinth_usher", Role.EXCLUSIVE, 6, 3),
            new Entry("tbos:hour_cantor", Role.BOSS, 6, 5),
            new Entry("tbos:blank_chronist", Role.EXCLUSIVE, 7, 2),
            new Entry("tbos:hour_hand_wraith", Role.BOSS, 7, 5));

    private Bestiary() {
    }

    public static boolean tracked(String entityId) {
        for (Entry entry : ENTRIES) {
            if (entry.id.equals(entityId)) {
                return true;
            }
        }
        return false;
    }

    public static int discovered(PlayerProfile profile) {
        int count = 0;
        for (Entry entry : ENTRIES) {
            if (profile.kills.getOrDefault(entry.id, 0) > 0) {
                count++;
            }
        }
        return count;
    }
}
