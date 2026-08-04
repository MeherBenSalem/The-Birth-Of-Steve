package com.nightbeam.tbos.platform.services;

import com.nightbeam.tbos.network.payload.TbosPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Classic packet transport used by Minecraft 1.20.1's loader APIs. */
public interface INetworkHelper {
    void sendToPlayer(ServerPlayer player, TbosPacket packet);

    void sendToPlayersNear(
            ServerLevel level, double x, double y, double z, double radius, TbosPacket packet);

    /** Client-side only; throws on a dedicated server. */
    void sendToServer(TbosPacket packet);
}
