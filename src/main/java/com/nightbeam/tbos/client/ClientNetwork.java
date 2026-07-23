package com.nightbeam.tbos.client;

import com.nightbeam.tbos.network.payload.BeginTransitionPayload;
import com.nightbeam.tbos.network.payload.SiteSnapshotPayload;
import com.nightbeam.tbos.network.payload.ArchiveQuestPayload;
import com.nightbeam.tbos.network.payload.ArchivePuzzlePayload;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

public final class ClientNetwork {
    private ClientNetwork() {
    }

    public static void register(RegisterClientPayloadHandlersEvent event) {
        event.register(BeginTransitionPayload.TYPE, (payload, context) -> ClientTransitionTracker.begin(payload));
        event.register(SiteSnapshotPayload.TYPE, (payload, context) -> ClientTransitionTracker.snapshot(payload));
        event.register(ArchiveQuestPayload.TYPE, (payload, context) -> ArchiveQuestHud.accept(payload));
        event.register(ArchivePuzzlePayload.TYPE, (payload, context) -> ArchivePuzzleHud.accept(payload));
    }
}
