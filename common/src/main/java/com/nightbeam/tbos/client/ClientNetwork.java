package com.nightbeam.tbos.client;

import com.nightbeam.tbos.network.payload.ArchiveFloorIntroPayload;
import com.nightbeam.tbos.network.payload.ArchivePuzzlePayload;
import com.nightbeam.tbos.network.payload.ArchiveQuestPayload;
import com.nightbeam.tbos.network.payload.BeginTransitionPayload;
import com.nightbeam.tbos.network.payload.JournalQuestSnapshotPayload;
import com.nightbeam.tbos.network.payload.SiteSnapshotPayload;

/**
 * Client-side handling for the six server-to-client payloads.
 *
 * <p>Each loader project routes its own receiver registration into these
 * methods, and is responsible for calling them on the client thread.
 */
public final class ClientNetwork {
    private ClientNetwork() {
    }

    public static void handleBeginTransition(BeginTransitionPayload payload) {
        ClientTransitionTracker.begin(payload);
    }

    public static void handleSiteSnapshot(SiteSnapshotPayload payload) {
        ClientTransitionTracker.snapshot(payload);
    }

    public static void handleArchiveQuest(ArchiveQuestPayload payload) {
        ArchiveQuestHud.accept(payload);
    }

    public static void handleArchivePuzzle(ArchivePuzzlePayload payload) {
        ArchivePuzzleHud.accept(payload);
    }

    public static void handleArchiveFloorIntro(ArchiveFloorIntroPayload payload) {
        ArchiveFloorIntroHud.begin(payload);
    }

    public static void handleJournalQuestSnapshot(JournalQuestSnapshotPayload payload) {
        ArchivistQuestScreen.accept(payload);
    }
}
