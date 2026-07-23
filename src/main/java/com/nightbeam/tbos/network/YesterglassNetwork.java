package com.nightbeam.tbos.network;

import com.nightbeam.tbos.network.payload.BeginTransitionPayload;
import com.nightbeam.tbos.network.payload.LensUseRequest;
import com.nightbeam.tbos.network.payload.SiteSnapshotPayload;
import com.nightbeam.tbos.network.payload.ArchiveQuestPayload;
import com.nightbeam.tbos.network.payload.ArchivePuzzlePayload;
import com.nightbeam.tbos.site.TemporalSiteManager;
import com.nightbeam.tbos.run.ArchiveRunManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class YesterglassNetwork {
    private static final String NETWORK_VERSION = "4";

    private YesterglassNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(LensUseRequest.TYPE, LensUseRequest.STREAM_CODEC, YesterglassNetwork::handleLensUse);
        registrar.playToClient(BeginTransitionPayload.TYPE, BeginTransitionPayload.STREAM_CODEC);
        registrar.playToClient(SiteSnapshotPayload.TYPE, SiteSnapshotPayload.STREAM_CODEC);
        registrar.playToClient(ArchiveQuestPayload.TYPE, ArchiveQuestPayload.STREAM_CODEC);
        registrar.playToClient(ArchivePuzzlePayload.TYPE, ArchivePuzzlePayload.STREAM_CODEC);
    }

    private static void handleLensUse(LensUseRequest request, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            if (!ArchiveRunManager.discoverNearestSecret(player)) {
                TemporalSiteManager.handleLensUse(player);
            }
        }
    }
}
