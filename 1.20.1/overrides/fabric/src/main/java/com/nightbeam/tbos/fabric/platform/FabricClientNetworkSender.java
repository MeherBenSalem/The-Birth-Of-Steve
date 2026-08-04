package com.nightbeam.tbos.fabric.platform;

import com.nightbeam.tbos.network.payload.TbosPacket;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Client-only send path, kept in its own class so that
 * {@link FabricNetworkHelper} — which the dedicated server does load — never
 * pulls {@code ClientPlayNetworking} onto the server classpath.
 */
@Environment(EnvType.CLIENT)
final class FabricClientNetworkSender {
    private FabricClientNetworkSender() {
    }

    static void sendToServer(TbosPacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        packet.write(buffer);
        ClientPlayNetworking.send(packet.id(), buffer);
    }
}
