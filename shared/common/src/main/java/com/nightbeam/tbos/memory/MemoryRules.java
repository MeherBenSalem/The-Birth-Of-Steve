package com.nightbeam.tbos.memory;

import com.nightbeam.tbos.run.ArchiveDungeonSettings;

/** New-run profile. Legacy allocations never acquire this profile on load. */
public final class MemoryRules {
    private MemoryRules(){}
    public static ArchiveDungeonSettings expedition(ArchiveDungeonSettings original) {
        return new ArchiveDungeonSettings(9,12,original.horizontalLimit(),original.verticalLimit(),8,
            original.branchingProbability(),original.deadEndProbability(),original.loopProbability(),original.specialRoomFrequency(),
            2,2,original.secretRoomProbability(),original.chestProbability(),original.blockBudgetPerTick(),original.generationAttempts(),original.rules());
    }
}
