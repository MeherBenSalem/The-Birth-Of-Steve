package com.nightbeam.tbos.site;

import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.world.AdventureWorldManager;
import com.nightbeam.tbos.world.FractureShrineQueue;
import com.nightbeam.tbos.world.PlayerOnboarding;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
    private TemporalSiteEvents() {
    }

    public static void onServerTick(MinecraftServer server) {
        TemporalSiteManager.tick(server);
        FractureShrineQueue.drain(server.overworld());
        for (ServerLevel level : server.getAllLevels()) {
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

    public static void onServerStarted(MinecraftServer server) {
        server.getAllLevels().forEach(TemporalSiteManager::recover);
        AdventureWorldManager.plannedShrines(server.overworld());
    }

    public static void onServerStopped(MinecraftServer server) {
        TemporalSiteManager.clearRuntimeState();
        FractureShrineQueue.clear();
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
        if (player.level().dimension().equals(Level.OVERWORLD)) {
            AdventureWorldManager.plannedShrines(player.level());
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
        TemporalSiteManager.onChunkLoaded(level, chunk);
        if (level.dimension().equals(Level.OVERWORLD)) {
            // Chunk-load callbacks must not trigger nested chunk loading, so a
            // chunk covering a planned shrine only queues it; the server tick
            // builds it.
            FractureShrineQueue.onChunkLoaded(level, chunk);
        }
    }
}
