package com.nightbeam.tbos.run;

/** Derived, restart-safe progress for the final Cantor Seal gate. */
public record ArchiveQuestProgress(
        int roomsCleared,
        int roomsRequired,
        int eligibleRooms,
        int lesserBossesDefeated,
        int lesserBossesTotal,
        boolean complete) {
    public static final String QUEST_TITLE = "Reconstruct the Cantor Seal";

    public ArchiveQuestProgress {
        if (roomsCleared < 0 || roomsRequired < 0 || eligibleRooms < 0
                || lesserBossesDefeated < 0 || lesserBossesTotal < 0
                || roomsCleared > eligibleRooms
                || roomsRequired > eligibleRooms
                || lesserBossesDefeated > lesserBossesTotal) {
            throw new IllegalArgumentException("Archive quest progress is outside its derived bounds");
        }
    }

    public static ArchiveQuestProgress from(ArchiveDungeonGraph graph) {
        int eligible = 0;
        int cleared = 0;
        int lesserBosses = 0;
        int lesserBossesDefeated = 0;
        for (ArchiveRoomNode room : graph.rooms()) {
            if (!room.category().mandatory() && room.category() != ArchiveRoomCategory.SECRET) {
                eligible++;
                if (room.runtime().completed()) {
                    cleared++;
                }
            }
            if (room.category() == ArchiveRoomCategory.MINI_BOSS) {
                lesserBosses++;
                if (room.runtime().completed()) {
                    lesserBossesDefeated++;
                }
            }
        }
        int required = eligible == 0 ? 0 : Math.max(1, (eligible * 3 + 4) / 5);
        boolean complete = cleared >= required
                && lesserBossesDefeated >= lesserBosses
                && lesserBosses > 0;
        return new ArchiveQuestProgress(
                cleared,
                required,
                eligible,
                lesserBossesDefeated,
                lesserBosses,
                complete);
    }
}
