package com.nightbeam.tbos.neoforge.platform;

import com.nightbeam.tbos.platform.services.INetworkHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public final class NeoForgeNetworkHelper implements INetworkHelper {
    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection.hasChannel(payload.type())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /**
     * Iterates and filters rather than calling {@code PacketDistributor
     * .sendToPlayersNear}, which does not check whether each recipient
     * negotiated the channel and throws when one has not. GameTest mock players
     * are exactly that case.
     */
    @Override
    public void sendToPlayersNear(
            ServerLevel level, double x, double y, double z, double radius, CustomPacketPayload payload) {
        double radiusSqr = radius * radius;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= radiusSqr) {
                sendToPlayer(player, payload);
            }
        }
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        // Split out so a dedicated server never classloads ClientPacketDistributor.
        NeoForgeClientNetworkSender.sendToServer(payload);
    }
}
