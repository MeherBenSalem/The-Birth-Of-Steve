package com.nightbeam.tbos.neoforge.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Client-only send path, kept in its own class so that
 * {@link NeoForgeNetworkHelper} — which the dedicated server does load — never
 * pulls {@code ClientPacketDistributor} onto the server classpath.
 */
final class NeoForgeClientNetworkSender {
    private NeoForgeClientNetworkSender() {
    }

    static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
