package com.nightbeam.tbos.site;

import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.world.AdventureWorldManager;
import com.nightbeam.tbos.world.FractureShrineQueue;
import com.nightbeam.tbos.world.PlayerOnboarding;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Authored-site runtime hooks, expressed in vanilla terms.
 *
 * <p>Nothing here knows about a loader. Cancellation is a return value rather
 * than a mutated event, so the NeoForge adapter can call {@code setCanceled} and
 * the Fabric adapter can return {@code false} from its callback, and both get the
 * same answer from the same code.
 */
public final class TemporalSiteEvents {
    /** Chunks whose site geometry is waiting for a tick, keyed by dimension. */
    private static final Map<ResourceKey<Level>, Set<ChunkPos>> PENDING_SITE_GEOMETRY =
            new ConcurrentHashMap<>();

    private TemporalSiteEvents() {
    }

    public static void onServerTick(MinecraftServer server) {
        drainPendingSiteGeometry(server);
        TemporalSiteManager.tick(server);
        FractureShrineQueue.drain(server.overworld());
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getGameTime() % 20L != 0L) {
                continue;
            }
            for (ServerPlayer player : level.players()) {
                AdventureWorldManager.nearestShrine(player).ifPresent(shrine -> {
                    if (ModAdvancements.tryAwardDiscoverFractureShrine(player)) {
                        player.displayClientMessage(Component.translatable(
                                        "message.tbos.shrine.discovered." + shrine.variant().serializedName())
                                .withStyle(net.minecraft.ChatFormatting.AQUA), true);
                    }
                });
                TemporalSiteManager.data(level).findContaining(player.blockPosition())
                        .filter(site -> site.definitionId().equals(BuiltInTemporalSites.PARALLAX_ATRIUM_ID))
                        .ifPresent(site -> ModAdvancements.awardEnterMeridianArchive(player));
            }
        }
    }

    public static void onServerStarted(MinecraftServer server) {
        server.getAllLevels().forEach(TemporalSiteManager::recover);
        AdventureWorldManager.plannedShrines(server.overworld());
    }

    public static void onServerStopped(MinecraftServer server) {
        TemporalSiteManager.clearRuntimeState();
        FractureShrineQueue.clear();
        PENDING_SITE_GEOMETRY.clear();
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        planOverworldShrines(player);
        PlayerOnboarding.onPlayerLoggedIn(player);
        TemporalSiteManager.sendNearbySnapshots(player);
    }

    public static void onPlayerChangedDimension(ServerPlayer player) {
        planOverworldShrines(player);
        TemporalSiteManager.sendNearbySnapshots(player);
    }

    private static void planOverworldShrines(ServerPlayer player) {
        if (player.level() instanceof ServerLevel level && level.dimension().equals(Level.OVERWORLD)) {
            AdventureWorldManager.plannedShrines(level);
        }
    }

    /** @return false to deny the break; the caller must also resync the client */
    public static boolean allowBreak(ServerLevel level, BlockPos position, ServerPlayer player) {
        if (!TemporalSiteManager.isProtected(level, position)) {
            return true;
        }
        player.sendSystemMessage(Component.translatable("message.tbos.protected"));
        return false;
    }

    /** @return false to deny the placement */
    public static boolean allowPlace(ServerLevel level, BlockPos position, Entity placer) {
        if (!TemporalSiteManager.isProtected(level, position)) {
            return true;
        }
        if (placer instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable("message.tbos.protected"));
        }
        return false;
    }

    /** Strips protected positions from an explosion's block list, in place. */
    public static void filterExplosion(ServerLevel level, List<BlockPos> affected) {
        // Temporal sites are the overworld Meridian Archive path. The separate
        // Fractured Archive dimension has its own ArchiveRun protection and is
        // intentionally unaffected by this handler.
        affected.removeIf(pos -> TemporalSiteManager.isProtected(level, pos));
    }

    public static void onChunkLoaded(ServerLevel level, ChunkPos chunk) {
        // Chunk-load callbacks must not trigger nested chunk loading, so the
        // chunk is only recorded here; the server tick does the work.
        //
        // Site phase geometry has to defer for the same reason the shrine queue
        // does. Fabric 1.20.1 fires this callback from inside ChunkMap's
        // proto-to-full conversion, and TemporalSiteManager.applyPhaseGeometry
        // writes blocks: a write reaching a chunk that is not already resident
        // re-enters the chunk loader and parks the server thread on a future
        // that only the same thread could complete, hanging the server on the
        // first load of a world that already has a site.
        PENDING_SITE_GEOMETRY
                .computeIfAbsent(level.dimension(), key -> ConcurrentHashMap.newKeySet())
                .add(chunk);
        if (level.dimension().equals(Level.OVERWORLD)) {
            FractureShrineQueue.onChunkLoaded(level, chunk);
        }
    }

    /** Applies the geometry the chunk-load callbacks deferred, on the tick. */
    private static void drainPendingSiteGeometry(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            Set<ChunkPos> pending = PENDING_SITE_GEOMETRY.remove(level.dimension());
            if (pending == null) {
                continue;
            }
            // A chunk that unloaded again in the meantime is skipped by
            // TemporalSiteManager's own loaded-chunk guard.
            for (ChunkPos chunk : pending) {
                TemporalSiteManager.onChunkLoaded(level, chunk);
            }
        }
    }
}
