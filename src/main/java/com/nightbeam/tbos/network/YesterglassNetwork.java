package com.nightbeam.tbos.network;

import com.nightbeam.tbos.network.payload.BeginTransitionPayload;
import com.nightbeam.tbos.network.payload.ArchiveFloorIntroPayload;
import com.nightbeam.tbos.network.payload.LensUseRequest;
import com.nightbeam.tbos.network.payload.JournalQuestRequest;
import com.nightbeam.tbos.network.payload.JournalQuestSnapshotPayload;
import com.nightbeam.tbos.network.payload.SiteSnapshotPayload;
import com.nightbeam.tbos.network.payload.ArchiveQuestPayload;
import com.nightbeam.tbos.network.payload.ArchivePuzzlePayload;
import com.nightbeam.tbos.registry.ModItems;
import com.nightbeam.tbos.site.TemporalSiteManager;
import com.nightbeam.tbos.run.ArchiveRunManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class YesterglassNetwork {
    private static final String NETWORK_VERSION = "5";
    private static final long JOURNAL_REQUEST_INTERVAL_TICKS = 10L;
    private static final Map<UUID, Long> LAST_JOURNAL_REQUEST = new ConcurrentHashMap<>();

    private YesterglassNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(LensUseRequest.TYPE, LensUseRequest.STREAM_CODEC, YesterglassNetwork::handleLensUse);
        registrar.playToServer(
                JournalQuestRequest.TYPE,
                JournalQuestRequest.STREAM_CODEC,
                YesterglassNetwork::handleJournalQuestRequest);
        registrar.playToClient(BeginTransitionPayload.TYPE, BeginTransitionPayload.STREAM_CODEC);
        registrar.playToClient(SiteSnapshotPayload.TYPE, SiteSnapshotPayload.STREAM_CODEC);
        registrar.playToClient(ArchiveQuestPayload.TYPE, ArchiveQuestPayload.STREAM_CODEC);
        registrar.playToClient(ArchivePuzzlePayload.TYPE, ArchivePuzzlePayload.STREAM_CODEC);
        registrar.playToClient(ArchiveFloorIntroPayload.TYPE, ArchiveFloorIntroPayload.STREAM_CODEC);
        registrar.playToClient(JournalQuestSnapshotPayload.TYPE, JournalQuestSnapshotPayload.STREAM_CODEC);
    }

    private static void handleLensUse(LensUseRequest request, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            if (!ArchiveRunManager.discoverNearestSecret(player)) {
                TemporalSiteManager.handleLensUse(player);
            }
        }
    }

    private static void handleJournalQuestRequest(JournalQuestRequest request, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || (!player.getMainHandItem().is(ModItems.ARCHIVISTS_JOURNAL.get())
                        && !player.getOffhandItem().is(ModItems.ARCHIVISTS_JOURNAL.get()))) {
            return;
        }
        long tick = player.level().getServer().getTickCount();
        Long previous = LAST_JOURNAL_REQUEST.get(player.getUUID());
        if (previous != null && tick - previous < JOURNAL_REQUEST_INTERVAL_TICKS) {
            return;
        }
        LAST_JOURNAL_REQUEST.put(player.getUUID(), tick);
        PacketDistributor.sendToPlayer(player, JournalQuestSnapshotPayload.from(player));
    }
}
