package com.nightbeam.tbos.fabric.platform;

import com.nightbeam.tbos.network.payload.TbosPacket;
import com.nightbeam.tbos.platform.services.INetworkHelper;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class FabricNetworkHelper implements INetworkHelper {
    /**
     * The {@code canSend} guard matters more here than on Forge: a Fabric server
     * accepts clients that do not have the mod, and sending them a {@code tbos:}
     * packet would disconnect them.
     */
    @Override
    public void sendToPlayer(ServerPlayer player, TbosPacket packet) {
        if (ServerPlayNetworking.canSend(player, packet.id())) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            packet.write(buffer);
            ServerPlayNetworking.send(player, packet.id(), buffer);
        }
    }

    @Override
    public void sendToPlayersNear(
            ServerLevel level, double x, double y, double z, double radius, TbosPacket packet) {
        for (ServerPlayer player : PlayerLookup.around(level, new Vec3(x, y, z), radius)) {
            sendToPlayer(player, packet);
        }
    }

    @Override
    public void sendToServer(TbosPacket packet) {
        // Split out so the dedicated server never classloads ClientPlayNetworking.
        FabricClientNetworkSender.sendToServer(packet);
    }
}
