package com.nightbeam.tbos.site;

import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.command.YesterglassCommands;
import com.nightbeam.tbos.world.AdventureWorldManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
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
        NeoForge.EVENT_BUS.addListener(TemporalSiteEvents::onChunkLoaded);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        TemporalSiteManager.tick(event.getServer());
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
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        TemporalSiteManager.clearRuntimeState();
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ensureOverworldShrines(player);
            TemporalSiteManager.sendNearbySnapshots(player);
        }
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ensureOverworldShrines(player);
            TemporalSiteManager.sendNearbySnapshots(player);
        }
    }

    private static void ensureOverworldShrines(ServerPlayer player) {
        if (player.level().dimension().equals(Level.OVERWORLD)) {
            AdventureWorldManager.ensureShrines(player.level(), player.blockPosition());
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

    private static void onChunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TemporalSiteManager.onChunkLoaded(level, event.getChunk().getPos());
        }
    }
}
