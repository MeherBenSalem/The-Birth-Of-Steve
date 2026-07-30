package com.nightbeam.tbos.neoforge;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.command.YesterglassCommands;
import com.nightbeam.tbos.neoforge.platform.NeoForgeRegistryHelper;
import com.nightbeam.tbos.network.YesterglassNetwork;
import com.nightbeam.tbos.network.payload.ArchiveFloorIntroPayload;
import com.nightbeam.tbos.network.payload.ArchivePuzzlePayload;
import com.nightbeam.tbos.network.payload.ArchiveQuestPayload;
import com.nightbeam.tbos.network.payload.BeginTransitionPayload;
import com.nightbeam.tbos.network.payload.JournalQuestRequest;
import com.nightbeam.tbos.network.payload.JournalQuestSnapshotPayload;
import com.nightbeam.tbos.network.payload.LensUseRequest;
import com.nightbeam.tbos.network.payload.SiteSnapshotPayload;
import com.nightbeam.tbos.registry.ModEntities;
import com.nightbeam.tbos.run.ArchiveRunEvents;
import com.nightbeam.tbos.site.TemporalSiteEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * NeoForge entry point: loads the common mod, then adapts NeoForge's event bus,
 * payload registrar and command event onto the loader-neutral handlers.
 */
@Mod(Yesterglass.MOD_ID)
public final class TbosNeoForge {
    public TbosNeoForge(IEventBus modBus) {
        Yesterglass.init();
        NeoForgeRegistryHelper.attachAll(modBus);

        modBus.addListener(TbosNeoForge::registerPayloads);
        modBus.addListener(TbosNeoForge::createAttributes);

        NeoForge.EVENT_BUS.addListener(TbosNeoForge::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(TbosNeoForge::onServerTick);
        NeoForge.EVENT_BUS.addListener(TbosNeoForge::onServerStarted);
        NeoForge.EVENT_BUS.addListener(TbosNeoForge::onServerStopped);
        NeoForge.EVENT_BUS.addListener(TbosNeoForge::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(TbosNeoForge::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(TbosNeoForge::onBreakBlock);
        NeoForge.EVENT_BUS.addListener(TbosNeoForge::onPlaceBlock);
        NeoForge.EVENT_BUS.addListener(TbosNeoForge::onExplosion);
        NeoForge.EVENT_BUS.addListener(TbosNeoForge::onChunkLoaded);
        NeoForge.EVENT_BUS.addListener(TbosNeoForge::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(TbosNeoForge::onLivingHeal);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(YesterglassNetwork.NETWORK_VERSION);
        registrar.playToServer(
                LensUseRequest.TYPE, LensUseRequest.STREAM_CODEC, TbosNeoForge::handleLensUse);
        registrar.playToServer(
                JournalQuestRequest.TYPE,
                JournalQuestRequest.STREAM_CODEC,
                TbosNeoForge::handleJournalQuestRequest);
        registrar.playToClient(BeginTransitionPayload.TYPE, BeginTransitionPayload.STREAM_CODEC);
        registrar.playToClient(SiteSnapshotPayload.TYPE, SiteSnapshotPayload.STREAM_CODEC);
        registrar.playToClient(ArchiveQuestPayload.TYPE, ArchiveQuestPayload.STREAM_CODEC);
        registrar.playToClient(ArchivePuzzlePayload.TYPE, ArchivePuzzlePayload.STREAM_CODEC);
        registrar.playToClient(ArchiveFloorIntroPayload.TYPE, ArchiveFloorIntroPayload.STREAM_CODEC);
        registrar.playToClient(JournalQuestSnapshotPayload.TYPE, JournalQuestSnapshotPayload.STREAM_CODEC);
    }

    private static void handleLensUse(LensUseRequest request, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            YesterglassNetwork.handleLensUse(player);
        }
    }

    private static void handleJournalQuestRequest(JournalQuestRequest request, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            YesterglassNetwork.handleJournalQuestRequest(player);
        }
    }

    private static void createAttributes(EntityAttributeCreationEvent event) {
        ModEntities.forEachAttribute((type, builder) -> event.put(type, builder.build()));
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        YesterglassCommands.register(event.getDispatcher());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        TemporalSiteEvents.onServerTick(event.getServer());
        ArchiveRunEvents.onServerTick(event.getServer());
    }

    private static void onServerStarted(ServerStartedEvent event) {
        TemporalSiteEvents.onServerStarted(event.getServer());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        TemporalSiteEvents.onServerStopped(event.getServer());
        ArchiveRunEvents.onServerStopped(event.getServer());
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TemporalSiteEvents.onPlayerLoggedIn(player);
            ArchiveRunEvents.onPlayerLoggedIn(player);
        }
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TemporalSiteEvents.onPlayerChangedDimension(player);
            ArchiveRunEvents.onPlayerChangedDimension(player);
        }
    }

    private static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer player = event.getPlayer() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
        boolean allowed = TemporalSiteEvents.allowBreak(level, event.getPos(), player)
                && ArchiveRunEvents.allowBreak(level, event.getPos(), player);
        if (!allowed) {
            event.setCanceled(true);
            event.setNotifyClient(true);
        }
    }

    private static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        boolean allowed = TemporalSiteEvents.allowPlace(level, event.getPos(), event.getEntity())
                && ArchiveRunEvents.allowPlace(level, event.getPos(), event.getEntity());
        if (!allowed) {
            event.setCanceled(true);
        }
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TemporalSiteEvents.filterExplosion(level, event.getAffectedBlocks());
            ArchiveRunEvents.filterExplosion(level, event.getAffectedBlocks());
        }
    }

    private static void onChunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TemporalSiteEvents.onChunkLoaded(level, event.getChunk().getPos());
        }
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (!ArchiveRunEvents.allowDeath(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private static void onLivingHeal(LivingHealEvent event) {
        event.setAmount(ArchiveRunEvents.scaleHeal(event.getEntity(), event.getAmount()));
    }
}
