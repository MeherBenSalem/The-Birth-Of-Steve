package com.nightbeam.tbos.network;

import com.nightbeam.tbos.network.payload.JournalQuestSnapshotPayload;
import com.nightbeam.tbos.platform.Services;
import com.nightbeam.tbos.registry.ModItems;
import com.nightbeam.tbos.run.ArchiveRunManager;
import com.nightbeam.tbos.site.TemporalSiteManager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side handling for the two client-to-server payloads.
 *
 * <p>Payload registration itself is loader work — NeoForge has a payload
 * registrar keyed by protocol version, Fabric has a type registry plus global
 * receivers — so each loader project wires its transport to these two methods.
 * The payload records and their stream codecs stay vanilla and shared.
 */
public final class YesterglassNetwork {
    /**
     * NeoForge's protocol handshake version. Bump only alongside a payload
     * change or an explicit compatibility break. Fabric has no equivalent
     * handshake, so the value is NeoForge-only.
     */
    public static final String NETWORK_VERSION = "6";

    private static final long JOURNAL_REQUEST_INTERVAL_TICKS = 10L;
    private static final Map<UUID, Long> LAST_JOURNAL_REQUEST = new ConcurrentHashMap<>();

    private YesterglassNetwork() {
    }

    public static void handleLensUse(ServerPlayer player) {
        if (!ArchiveRunManager.discoverNearestSecret(player)) {
            TemporalSiteManager.handleLensUse(player);
        }
    }

    public static void handleJournalQuestRequest(ServerPlayer player) {
        if (!player.getMainHandItem().is(ModItems.ARCHIVISTS_JOURNAL.get())
                && !player.getOffhandItem().is(ModItems.ARCHIVISTS_JOURNAL.get())) {
            return;
        }
        long tick = player.level().getServer().getTickCount();
        Long previous = LAST_JOURNAL_REQUEST.get(player.getUUID());
        if (previous != null && tick - previous < JOURNAL_REQUEST_INTERVAL_TICKS) {
            return;
        }
        LAST_JOURNAL_REQUEST.put(player.getUUID(), tick);
        Services.NETWORK.sendToPlayer(player, JournalQuestSnapshotPayload.from(player));
    }
}
