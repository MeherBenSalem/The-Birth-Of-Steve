package com.nightbeam.tbos.neoforge.platform;

import com.nightbeam.tbos.network.payload.TbosPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Reserves the client-only send boundary on the dedicated Forge server.
 *
 * <p>{@link NeoForgeNetworkHelper} forwards «sendToServer» through the shared
 * {@code SimpleChannel} and never touches this class; it exists so a future
 * client-only packet path has a dist-isolated home rather than dressing the
 * server classloader with {@code Minecraft}.
 */
@OnlyIn(Dist.CLIENT)
final class NeoForgeClientNetworkSender {
    private NeoForgeClientNetworkSender() {
    }

    static void sendToServer(TbosPacket packet) {
        // The instance transport is reached through Services.NETWORK on the client;
        // this boundary intentionally holds no Minecraft references.
    }
}
