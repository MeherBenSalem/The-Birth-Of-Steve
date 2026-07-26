package com.nightbeam.tbos.site;

import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.command.YesterglassCommands;
import com.nightbeam.tbos.world.AdventureWorldManager;
import com.nightbeam.tbos.world.FractureShrineQueue;
import com.nightbeam.tbos.world.PlayerOnboarding;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class TemporalSiteEvents {
    private TemporalSiteEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(YesterglassCommands::register);
        NeoForge.EVENT_BUS.addListener(TemporalSiteEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(TemporalSiteEvents::onServerStarted);
        NeoForge.EVENT_BUS.addListener(TemporalSiteEvents::onServerStopped);
        NeoForge.EVENT_BUS.addListener(TemporalSiteEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(TemporalSiteEvents::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(TemporalSiteEvents::onBreakBlock);
        NeoForge.EVENT_BUS.addListener(TemporalSiteEvents::onPlaceBlock);
        NeoForge.EVENT_BUS.addListener(TemporalSiteEvents::onExplosion);
        NeoForge.EVENT_BUS.addListener(TemporalSiteEvents::onChunkLoaded);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        TemporalSiteManager.tick(event.getServer());
        FractureShrineQueue.drain(event.getServer().overworld());
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.getGameTime() % 20L != 0L) {
                continue;
            }
            for (ServerPlayer player : level.players()) {
                if (AdventureWorldManager.nearestShrine(player).isPresent()) {
                    ModAdvancements.awardDiscoverFractureShrine(player);
                }
                TemporalSiteManager.data(level).findContaining(player.blockPosition())
                        .filter(site -> site.definitionId().equals(BuiltInTemporalSites.PARALLAX_ATRIUM_ID))
                        .ifPresent(site -> ModAdvancements.awardEnterMeridianArchive(player));
            }
        }
    }

    private static void onServerStarted(ServerStartedEvent event) {
        event.getServer().getAllLevels().forEach(TemporalSiteManager::recover);
        AdventureWorldManager.plannedShrines(event.getServer().overworld());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        TemporalSiteManager.clearRuntimeState();
        FractureShrineQueue.clear();
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            planOverworldShrines(player);
            PlayerOnboarding.onPlayerLoggedIn(player);
            TemporalSiteManager.sendNearbySnapshots(player);
        }
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            planOverworldShrines(player);
            TemporalSiteManager.sendNearbySnapshots(player);
        }
    }

    private static void planOverworldShrines(ServerPlayer player) {
        if (player.level().dimension().equals(Level.OVERWORLD)) {
            AdventureWorldManager.plannedShrines(player.level());
        }
    }

    private static void onBreakBlock(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level && TemporalSiteManager.isProtected(level, event.getPos())) {
            event.setCanceled(true);
            event.setNotifyClient(true);
            event.getPlayer().sendSystemMessage(Component.translatable("message.tbos.protected"));
        }
    }

    private static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && TemporalSiteManager.isProtected(level, event.getPos())) {
            event.setCanceled(true);
            if (event.getEntity() instanceof ServerPlayer player) {
                player.sendSystemMessage(Component.translatable("message.tbos.protected"));
            }
        }
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            // Temporal sites are the overworld Meridian Archive path. The
            // separate Fractured Archive dimension has its own ArchiveRun
            // protection and is intentionally unaffected by this handler.
            event.getAffectedBlocks().removeIf(pos -> TemporalSiteManager.isProtected(level, pos));
        }
    }

    private static void onChunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TemporalSiteManager.onChunkLoaded(level, event.getChunk().getPos());
            if (level.dimension().equals(Level.OVERWORLD)) {
                // Chunk-load callbacks must not trigger nested chunk loading, so a
                // chunk covering a planned shrine only queues it; the server tick
                // builds it.
                FractureShrineQueue.onChunkLoaded(level, event.getChunk().getPos());
            }
        }
    }
}
