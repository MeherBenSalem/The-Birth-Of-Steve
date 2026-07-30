package com.nightbeam.tbos.fabric.platform;

import com.nightbeam.tbos.platform.services.INetworkHelper;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class FabricNetworkHelper implements INetworkHelper {
    /**
     * The {@code canSend} guard matters more here than on NeoForge: a Fabric
     * server accepts clients that do not have the mod, and sending them a
     * {@code tbos:} payload would disconnect them.
     */
    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    @Override
    public void sendToPlayersNear(
            ServerLevel level, double x, double y, double z, double radius, CustomPacketPayload payload) {
        for (ServerPlayer player : PlayerLookup.around(level, new Vec3(x, y, z), radius)) {
            sendToPlayer(player, payload);
        }
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        // Split out so the dedicated server never classloads ClientPlayNetworking.
        FabricClientNetworkSender.sendToServer(payload);
    }
}
