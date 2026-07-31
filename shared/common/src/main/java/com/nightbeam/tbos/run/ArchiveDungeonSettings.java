package com.nightbeam.tbos.run;

/** Validated generation settings; server config is snapshotted at run creation. */
public record ArchiveDungeonSettings(
        int minimumRooms,
        int maximumRooms,
        int horizontalLimit,
        int verticalLimit,
        int maximumGraphDepth,
        double branchingProbability,
        double deadEndProbability,
        double loopProbability,
        double specialRoomFrequency,
        int maximumRoomsAbove,
        int maximumRoomsBelow,
        double secretRoomProbability,
        double chestProbability,
        int blockBudgetPerTick,
        int generationAttempts,
        ArchiveDungeonRules rules) {
    public static final ArchiveDungeonSettings DEFAULT = new ArchiveDungeonSettings(
            14, 20, 6, 2, 16, 0.48D, 0.16D, 0.10D, 0.30D,
            4, 4, 0.12D, 0.50D, 4_096, 64, ArchiveDungeonRules.DEFAULT);

    public ArchiveDungeonSettings(
            int minimumRooms,
            int maximumRooms,
            int horizontalLimit,
            int verticalLimit,
            int maximumGraphDepth,
            double branchingProbability,
            double deadEndProbability,
            double loopProbability,
            double specialRoomFrequency,
            int maximumRoomsAbove,
            int maximumRoomsBelow,
            double secretRoomProbability,
            double chestProbability,
            int blockBudgetPerTick,
            int generationAttempts) {
        this(
                minimumRooms,
                maximumRooms,
                horizontalLimit,
                verticalLimit,
                maximumGraphDepth,
                branchingProbability,
                deadEndProbability,
                loopProbability,
                specialRoomFrequency,
                maximumRoomsAbove,
                maximumRoomsBelow,
                secretRoomProbability,
                chestProbability,
                blockBudgetPerTick,
                generationAttempts,
                ArchiveDungeonRules.DEFAULT);
    }

    public ArchiveDungeonSettings {
        rules = java.util.Objects.requireNonNull(rules, "rules");
        if (minimumRooms < 7 || maximumRooms > 48 || minimumRooms > maximumRooms) {
            throw new IllegalArgumentException("Archive room limits must satisfy 7 <= min <= max <= 48");
        }
        if (horizontalLimit < 2 || horizontalLimit > 12 || verticalLimit < 1 || verticalLimit > 4) {
            throw new IllegalArgumentException("Archive horizontal or vertical expansion limit is invalid");
        }
        if (maximumGraphDepth < 4 || maximumGraphDepth > 48) {
            throw new IllegalArgumentException("Archive maximum graph depth must be between 4 and 48");
        }
        requireProbability("branching", branchingProbability);
        requireProbability("dead end", deadEndProbability);
        requireProbability("loop", loopProbability);
        requireProbability("special room", specialRoomFrequency);
        requireProbability("secret room", secretRoomProbability);
        requireProbability("chest", chestProbability);
        if (maximumRoomsAbove < 0 || maximumRoomsBelow < 0
                || maximumRoomsAbove > maximumRooms || maximumRoomsBelow > maximumRooms) {
            throw new IllegalArgumentException("Archive vertical room counts are outside the total room limit");
        }
        if (blockBudgetPerTick < 256 || blockBudgetPerTick > 65_536 || generationAttempts < 1 || generationAttempts > 512) {
            throw new IllegalArgumentException("Archive generation budget or retry count is invalid");
        }
    }

    private static void requireProbability(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException("Archive " + name + " probability must be between zero and one");
        }
    }
}
