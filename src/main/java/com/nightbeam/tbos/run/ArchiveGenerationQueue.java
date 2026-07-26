package com.nightbeam.tbos.run;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.config.YesterglassConfig;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/** Server-thread, block-budgeted room generation with restart reconciliation. */
public final class ArchiveGenerationQueue {
    private static final int UPDATE_FLAGS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
    private static final Map<UUID, Task> TASKS = new LinkedHashMap<>();
    private static final Map<UUID, Task> CLEANUP_TASKS = new LinkedHashMap<>();
    private static final Map<CleanupKey, Task> RETIRED_CLEANUP_TASKS = new LinkedHashMap<>();

    private ArchiveGenerationQueue() {
    }

    public static void enqueue(ArchiveRun run) {
        if (run.status() != ArchiveRunStatus.PREPARING || run.geometryPlaced()) {
            throw new IllegalArgumentException("Only an unplaced PREPARING archive run may be queued");
        }
        TASKS.computeIfAbsent(run.runId(), ignored -> new Task(run, ArchiveRoomPlacer.blueprint(run), true));
    }

    public static void enqueueRemoval(ArchiveRun run) {
        TASKS.remove(run.runId());
        CLEANUP_TASKS.computeIfAbsent(
                run.runId(), ignored -> new Task(run, ArchiveRoomPlacer.blueprint(run), false));
        enqueueRetiredFloorCleanup(run);
    }

    public static void enqueueRetiredFloorCleanup(ArchiveRun run) {
        for (ArchiveFloorSnapshot retired : run.floorState().retiredFloors()) {
            enqueueRetiredFloorCleanup(run, retired);
        }
    }

    public static void tick(MinecraftServer server, ArchiveRunSavedData storage) {
        ServerLevel level = server.getLevel(ArchiveDimensions.FRACTURED_ARCHIVE);
        if (level == null) {
            return;
        }
        if (regenerateIncomplete()) {
            for (ArchiveRun run : storage.all()) {
                if (run.status() == ArchiveRunStatus.PREPARING && !run.geometryPlaced()) {
                    try {
                        enqueue(run);
                    } catch (RuntimeException exception) {
                        storage.replace(run.abortPreparation());
                        Yesterglass.LOGGER.error("Could not rebuild queued archive run {}", run.runId(), exception);
                    }
                }
                if (run.status() == ArchiveRunStatus.ACTIVE) {
                    enqueueVacatedRetiredFloorCleanup(server, run);
                }
            }
        }

        int budget = blockBudget();
        int generationBudget = 0;
        if (!TASKS.isEmpty()) {
            boolean cleanupPending = !RETIRED_CLEANUP_TASKS.isEmpty() || !CLEANUP_TASKS.isEmpty();
            generationBudget = fairShare(budget, cleanupPending ? 2 : 1);
        }
        int remainingGenerationTasks = Math.max(1, TASKS.size());
        Iterator<Map.Entry<UUID, Task>> iterator = TASKS.entrySet().iterator();
        while (iterator.hasNext() && generationBudget > 0) {
            Map.Entry<UUID, Task> entry = iterator.next();
            ArchiveRun latest = storage.find(entry.getKey()).orElse(null);
            if (latest == null || latest.status() != ArchiveRunStatus.PREPARING) {
                iterator.remove();
                continue;
            }
            Task task = entry.getValue();
            int allowance = Math.max(1, generationBudget / remainingGenerationTasks--);
            int consumed = task.apply(level, allowance);
            budget -= consumed;
            generationBudget -= consumed;
            if (task.complete()) {
                iterator.remove();
                if (task.hasSafeSpawn(level)) {
                    ArchiveRunManager.finishQueuedPreparation(
                            server, storage, latest.runId(), task.blueprint.spawn());
                } else {
                    storage.replace(latest.abortPreparation());
                    Yesterglass.LOGGER.error(
                            "Archive floor {} for run {} finished generation without a safe spawn; teleport canceled",
                            latest.floor(),
                            latest.runId());
                }
            }
        }
        int retiredCleanupBudget = 0;
        if (!RETIRED_CLEANUP_TASKS.isEmpty()) {
            retiredCleanupBudget = fairShare(budget, CLEANUP_TASKS.isEmpty() ? 1 : 2);
        }
        int remainingRetiredCleanupTasks = Math.max(1, RETIRED_CLEANUP_TASKS.size());
        Iterator<Map.Entry<CleanupKey, Task>> retiredCleanup = RETIRED_CLEANUP_TASKS.entrySet().iterator();
        while (retiredCleanup.hasNext() && retiredCleanupBudget > 0) {
            Map.Entry<CleanupKey, Task> entry = retiredCleanup.next();
            Task task = entry.getValue();
            int allowance = Math.max(1, retiredCleanupBudget / remainingRetiredCleanupTasks--);
            int consumed = task.apply(level, allowance);
            budget -= consumed;
            retiredCleanupBudget -= consumed;
            if (task.complete()) {
                retiredCleanup.remove();
                clearFloorEntities(level, task.instanceSlot);
                ArchiveRun latest = storage.find(entry.getKey().runId()).orElse(null);
                if (latest != null) {
                    storage.replace(latest.finishRetiredFloorCleanup(entry.getKey().instanceSlot()));
                }
            }
        }
        int remainingCleanupTasks = Math.max(1, CLEANUP_TASKS.size());
        Iterator<Map.Entry<UUID, Task>> cleanup = CLEANUP_TASKS.entrySet().iterator();
        while (cleanup.hasNext() && budget > 0) {
            Map.Entry<UUID, Task> entry = cleanup.next();
            Task task = entry.getValue();
            int allowance = Math.max(1, budget / remainingCleanupTasks--);
            budget -= task.apply(level, allowance);
            if (task.complete()) {
                cleanup.remove();
                clearFloorEntities(level, task.instanceSlot);
                storage.remove(entry.getKey());
            }
        }
    }

    public static void cancel(UUID runId) {
        TASKS.remove(runId);
        CLEANUP_TASKS.remove(runId);
        RETIRED_CLEANUP_TASKS.keySet().removeIf(key -> key.runId().equals(runId));
    }

    public static int pendingCount() {
        return TASKS.size() + CLEANUP_TASKS.size() + RETIRED_CLEANUP_TASKS.size();
    }

    public static double progress(UUID runId) {
        Task task = TASKS.get(runId);
        return task == null ? 0.0D : task.progress();
    }

    /** Divides a tick budget without allowing a transient empty queue set to crash the server. */
    public static int fairShare(int availableBudget, int activeQueues) {
        if (availableBudget <= 0 || activeQueues <= 0) {
            return 0;
        }
        return Math.max(1, availableBudget / activeQueues);
    }

    public static boolean isSlotReserved(int instanceSlot) {
        return TASKS.values().stream().anyMatch(task -> task.instanceSlot == instanceSlot)
                || CLEANUP_TASKS.values().stream().anyMatch(task -> task.instanceSlot == instanceSlot)
                || RETIRED_CLEANUP_TASKS.values().stream().anyMatch(task -> task.instanceSlot == instanceSlot);
    }

    public static void clear() {
        TASKS.clear();
        CLEANUP_TASKS.clear();
        RETIRED_CLEANUP_TASKS.clear();
    }

    private static int blockBudget() {
        try {
            return YesterglassConfig.DUNGEON_BLOCK_BUDGET.get();
        } catch (IllegalStateException exception) {
            return ArchiveDungeonSettings.DEFAULT.blockBudgetPerTick();
        }
    }

    private static boolean regenerateIncomplete() {
        try {
            return YesterglassConfig.DUNGEON_REGENERATE_INCOMPLETE.get();
        } catch (IllegalStateException exception) {
            return true;
        }
    }

    private static void enqueueVacatedRetiredFloorCleanup(MinecraftServer server, ArchiveRun run) {
        for (ArchiveFloorSnapshot retired : run.floorState().retiredFloors()) {
            BoundingBox bounds = ArchiveInstanceLayout.boundsForSlot(retired.instanceSlot());
            boolean occupied = server.getPlayerList().getPlayers().stream()
                    .filter(player -> player.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE))
                    .anyMatch(player -> bounds.isInside(player.blockPosition()));
            if (!occupied) {
                enqueueRetiredFloorCleanup(run, retired);
            }
        }
    }

    private static void enqueueRetiredFloorCleanup(ArchiveRun run, ArchiveFloorSnapshot retired) {
        CleanupKey key = new CleanupKey(run.runId(), retired.instanceSlot());
        RETIRED_CLEANUP_TASKS.computeIfAbsent(key, ignored -> {
            ArchiveRun cleanupRun = ArchiveRun.create(
                    run.runId(),
                    retired.seed(),
                    retired.instanceSlot(),
                    run.members().stream()
                            .map(member -> member.resetForRegeneration(retired.dungeonGraph().startingRoom()))
                            .toList(),
                    retired.dungeonGraph());
            return new Task(cleanupRun, ArchiveRoomPlacer.blueprint(cleanupRun), false);
        });
    }

    private static void clearFloorEntities(ServerLevel level, int instanceSlot) {
        BoundingBox bounds = ArchiveInstanceLayout.boundsForSlot(instanceSlot);
        AABB area = new AABB(
                bounds.minX(),
                bounds.minY(),
                bounds.minZ(),
                bounds.maxX() + 1.0D,
                bounds.maxY() + 1.0D,
                bounds.maxZ() + 1.0D);
        level.getEntitiesOfClass(
                        Entity.class,
                        area,
                        entity -> !(entity instanceof ServerPlayer))
                .forEach(Entity::discard);
    }

    private static final class Task {
        private final ArchiveRoomPlacer.Blueprint blueprint;
        private final boolean placeGeometry;
        private final int instanceSlot;
        private int clearVolumeIndex;
        private int clearX;
        private int clearY;
        private int clearZ;
        private long clearedBlocks;
        private final long totalClearBlocks;
        private int placementIndex;
        private long loadedChunk = Long.MIN_VALUE;

        private Task(ArchiveRun run, ArchiveRoomPlacer.Blueprint blueprint, boolean placeGeometry) {
            this.blueprint = blueprint;
            this.placeGeometry = placeGeometry;
            this.instanceSlot = run.instanceSlot();
            this.totalClearBlocks = blueprint.clearVolumes().stream().mapToLong(volume ->
                    (long) (volume.maxX() - volume.minX() + 1)
                            * (volume.maxY() - volume.minY() + 1)
                            * (volume.maxZ() - volume.minZ() + 1)).sum();
            if (!blueprint.clearVolumes().isEmpty()) {
                resetClearCursor(blueprint.clearVolumes().getFirst());
            }
            if (debugEnabled()) {
                Yesterglass.LOGGER.info(
                        "Queued archive dungeon {}: {} rooms, {} clear volumes, {} placed blocks",
                        run.runId(),
                        run.rooms().size(),
                        blueprint.clearVolumes().size(),
                        blueprint.placements().size());
            }
        }

        private int apply(ServerLevel level, int budget) {
            int consumed = 0;
            while (consumed < budget && clearVolumeIndex < blueprint.clearVolumes().size()) {
                BoundingBox volume = blueprint.clearVolumes().get(clearVolumeIndex);
                BlockPos position = new BlockPos(clearX, clearY, clearZ);
                loadChunk(level, position);
                if (!level.getBlockState(position).isAir()) {
                    level.setBlock(position, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
                }
                advanceClearCursor(volume);
                clearedBlocks++;
                consumed++;
            }
            while (placeGeometry && consumed < budget && placementIndex < blueprint.placements().size()) {
                ArchiveRoomPlacer.Placement placement = blueprint.placements().get(placementIndex++);
                loadChunk(level, placement.position());
                if (!level.getBlockState(placement.position()).equals(placement.state())) {
                    level.setBlock(placement.position(), placement.state(), UPDATE_FLAGS);
                }
                consumed++;
            }
            return consumed;
        }

        private void loadChunk(ServerLevel level, BlockPos position) {
            int chunkX = position.getX() >> 4;
            int chunkZ = position.getZ() >> 4;
            long chunk = (chunkX & 0xffffffffL) | ((long) chunkZ << 32);
            if (chunk != loadedChunk) {
                level.getChunkAt(position);
                loadedChunk = chunk;
            }
        }

        private boolean complete() {
            return clearVolumeIndex >= blueprint.clearVolumes().size()
                    && (!placeGeometry || placementIndex >= blueprint.placements().size());
        }

        private boolean hasSafeSpawn(ServerLevel level) {
            if (!placeGeometry || !complete()) {
                return false;
            }
            BlockPos spawn = blueprint.spawn();
            level.getChunkAt(spawn);
            return level.getBlockState(spawn).isAir()
                    && level.getBlockState(spawn.above()).isAir()
                    && !level.getBlockState(blueprint.entranceFloor()).isAir();
        }

        private double progress() {
            long total = totalClearBlocks + (placeGeometry ? blueprint.placements().size() : 0);
            return total == 0L ? 1.0D : (clearedBlocks + placementIndex) / (double) total;
        }

        private void advanceClearCursor(BoundingBox volume) {
            clearY++;
            if (clearY <= volume.maxY()) {
                return;
            }
            clearY = volume.minY();
            clearZ++;
            if (clearZ <= volume.maxZ()) {
                return;
            }
            clearZ = volume.minZ();
            clearX++;
            if (clearX <= volume.maxX()) {
                return;
            }
            clearVolumeIndex++;
            if (clearVolumeIndex < blueprint.clearVolumes().size()) {
                resetClearCursor(blueprint.clearVolumes().get(clearVolumeIndex));
            }
        }

        private void resetClearCursor(BoundingBox volume) {
            clearX = volume.minX();
            clearY = volume.minY();
            clearZ = volume.minZ();
        }
    }

    private record CleanupKey(UUID runId, int instanceSlot) {
    }

    private static boolean debugEnabled() {
        try {
            return YesterglassConfig.DUNGEON_DEBUG.get();
        } catch (IllegalStateException exception) {
            return false;
        }
    }
}
