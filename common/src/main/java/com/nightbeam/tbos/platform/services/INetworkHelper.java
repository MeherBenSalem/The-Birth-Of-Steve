package com.nightbeam.tbos.platform.services;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Payload transport.
 *
 * <p>The payload records themselves are vanilla {@link CustomPacketPayload}s
 * with vanilla stream codecs, so they stay in common; only the send calls and
 * the handler registration differ per loader.
 */
public interface INetworkHelper {
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /** Everyone within {@code radius} of the point, matching NeoForge's {@code sendToPlayersNear}. */
    void sendToPlayersNear(
            ServerLevel level, double x, double y, double z, double radius, CustomPacketPayload payload);

    /** Client-side only; throws on a dedicated server. */
    void sendToServer(CustomPacketPayload payload);
}
