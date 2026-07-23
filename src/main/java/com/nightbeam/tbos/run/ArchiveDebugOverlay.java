package com.nightbeam.tbos.run;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Operator-only, runtime-only visualization of generated bounds and semantic markers. */
public final class ArchiveDebugOverlay {
    private static final Set<UUID> BOUNDARIES = new HashSet<>();
    private static final Set<UUID> MARKERS = new HashSet<>();

    private ArchiveDebugOverlay() {
    }

    public static boolean toggleBoundaries(UUID playerId) {
        return toggle(BOUNDARIES, playerId);
    }

    public static boolean toggleMarkers(UUID playerId) {
        return toggle(MARKERS, playerId);
    }

    public static void tick(MinecraftServer server, ArchiveRunSavedData storage, long tick) {
        if (tick % 10L != 0L) {
            return;
        }
        HashSet<UUID> viewers = new HashSet<>(BOUNDARIES);
        viewers.addAll(MARKERS);
        for (UUID playerId : viewers) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            ArchiveRun run = storage.findByMember(playerId).orElse(null);
            if (player == null || run == null || !(player.level() instanceof ServerLevel level)) {
                continue;
            }
            if (BOUNDARIES.contains(playerId)) {
                showBoundaries(level, run);
            }
            if (MARKERS.contains(playerId)) {
                showMarkers(level, run);
            }
        }
    }

    public static void clear() {
        BOUNDARIES.clear();
        MARKERS.clear();
    }

    private static void showBoundaries(ServerLevel level, ArchiveRun run) {
        for (ArchiveRoomNode room : run.dungeonGraph().rooms()) {
            var bounds = ArchiveRoomPlacer.roomBounds(run, room.index());
            int y = bounds.minY() + 1;
            particle(level, new BlockPos(bounds.minX(), y, bounds.minZ()));
            particle(level, new BlockPos(bounds.maxX(), y, bounds.minZ()));
            particle(level, new BlockPos(bounds.minX(), y, bounds.maxZ()));
            particle(level, new BlockPos(bounds.maxX(), y, bounds.maxZ()));
        }
    }

    private static void showMarkers(ServerLevel level, ArchiveRun run) {
        for (ArchiveRoomNode room : run.dungeonGraph().rooms()) {
            ArchiveRoomPlacer.monsterSpawnPositions(run, room.index()).forEach(position -> particle(level, position));
            ArchiveRoomPlacer.chestPositions(run, room.index()).forEach(position -> particle(level, position.above()));
            ArchiveRoomPlacer.lootPositions(run, room.index()).forEach(position -> particle(level, position));
        }
    }

    private static void particle(ServerLevel level, BlockPos position) {
        level.sendParticles(
                ParticleTypes.END_ROD,
                position.getX() + 0.5D,
                position.getY() + 0.5D,
                position.getZ() + 0.5D,
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D);
    }

    private static boolean toggle(Set<UUID> set, UUID playerId) {
        if (set.remove(playerId)) {
            return false;
        }
        set.add(playerId);
        return true;
    }
}
