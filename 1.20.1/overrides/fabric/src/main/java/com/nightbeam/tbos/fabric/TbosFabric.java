package com.nightbeam.tbos.fabric;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.command.YesterglassCommands;
import com.nightbeam.tbos.fabric.platform.FabricRegistryHelper;
import com.nightbeam.tbos.network.YesterglassNetwork;
import com.nightbeam.tbos.network.payload.JournalQuestRequest;
import com.nightbeam.tbos.network.payload.LensUseRequest;
import com.nightbeam.tbos.registry.ModEntities;
import com.nightbeam.tbos.run.ArchiveRunEvents;
import com.nightbeam.tbos.site.TemporalSiteEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric entry point: loads the common mod, commits the registrars, then maps
 * Fabric API callbacks onto the loader-neutral handlers.
 *
 * <p>Three protection hooks have no Fabric API equivalent — heal scaling,
 * explosion block filtering and non-player block placement — and are wired from
 * mixins in {@code com.nightbeam.tbos.fabric.mixin} instead.
 */
public final class TbosFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Yesterglass.init();
        FabricRegistryHelper.flushAll();

        ModEntities.forEachAttribute(FabricDefaultAttributeRegistry::register);
        registerPayloads();

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registry, environment) -> YesterglassCommands.register(dispatcher));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TemporalSiteEvents.onServerTick(server);
            ArchiveRunEvents.onServerTick(server);
        });
        ServerLifecycleEvents.SERVER_STARTED.register(TemporalSiteEvents::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            TemporalSiteEvents.onServerStopped(server);
            ArchiveRunEvents.onServerStopped(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            TemporalSiteEvents.onPlayerLoggedIn(handler.player);
            ArchiveRunEvents.onPlayerLoggedIn(handler.player);
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            TemporalSiteEvents.onPlayerChangedDimension(player);
            ArchiveRunEvents.onPlayerChangedDimension(player);
        });

        ServerChunkEvents.CHUNK_LOAD.register(
                (level, chunk) -> TemporalSiteEvents.onChunkLoaded(level, chunk.getPos()));

        // Returning false cancels the break and makes Fabric resync the block to
        // the client, which is what NeoForge's setNotifyClient(true) does.
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (!(level instanceof ServerLevel serverLevel)) {
                return true;
            }
            ServerPlayer serverPlayer = player instanceof ServerPlayer casted ? casted : null;
            return TemporalSiteEvents.allowBreak(serverLevel, pos, serverPlayer)
                    && ArchiveRunEvents.allowBreak(serverLevel, pos, serverPlayer);
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register(
                (entity, damageSource, damageAmount) -> ArchiveRunEvents.allowDeath(entity));
    }

    private void registerPayloads() {
        // Minecraft 1.20.1 Fabric has no payload-type registry; both client-to-
        // server packets carry no data, so classic channel receivers suffice.
        // Both handlers touch server state, so they are scheduled onto the
        // server thread rather than run on the netty thread that delivered them.
        ServerPlayNetworking.registerGlobalReceiver(
                LensUseRequest.ID,
                (server, player, handler, buf, responseSender) ->
                        server.execute(() -> YesterglassNetwork.handleLensUse(player)));
        ServerPlayNetworking.registerGlobalReceiver(
                JournalQuestRequest.ID,
                (server, player, handler, buf, responseSender) ->
                        server.execute(() -> YesterglassNetwork.handleJournalQuestRequest(player)));
    }
}
