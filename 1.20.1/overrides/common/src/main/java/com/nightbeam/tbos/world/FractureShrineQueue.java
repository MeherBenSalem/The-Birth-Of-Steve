package com.nightbeam.tbos.world;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Deferred build queue for world-generated Fracture Shrines.
 *
 * <p>Chunk-load callbacks must not trigger nested chunk loading, so a chunk that
 * contains a planned shrine only enqueues it here. The server tick drains the
 * queue one shrine at a time, when loading the surrounding chunks is safe.
 *
 * <p>Chunk loading is a hot path, so the pending chunk keys are cached and
 * probed with a single set lookup. Once all three shrines exist the cache is
 * empty and the probe costs nothing.
 */
public final class FractureShrineQueue {
    private static final Map<ResourceKey<Level>, Deque<FractureShrinePlan>> PENDING = new LinkedHashMap<>();
    private static final Map<ResourceKey<Level>, Set<Long>> PENDING_CHUNKS = new LinkedHashMap<>();

    private FractureShrineQueue() {
    }

    /** Queues any planned shrine covered by a freshly loaded chunk. */
    public static void onChunkLoaded(ServerLevel level, ChunkPos chunkPos) {
        if (!pendingChunks(level).contains(chunkPos.toLong())) {
            return;
        }
        for (FractureShrinePlan plan : AdventureWorldManager.unbuiltShrines(level)) {
            if (plan.chunk().equals(chunkPos)) {
                enqueue(level, plan);
            }
        }
    }

    /** Builds at most one queued shrine. Returns true when a shrine was built. */
    public static boolean drain(ServerLevel level) {
        FractureShrinePlan plan = poll(level);
        return plan != null && AdventureWorldManager.materializeShrine(level, plan);
    }

    /** Drops the cached chunk index after the set of built shrines changes. */
    public static synchronized void invalidate(ServerLevel level) {
        PENDING_CHUNKS.remove(level.dimension());
    }

    public static synchronized void clear() {
        PENDING.clear();
        PENDING_CHUNKS.clear();
    }

    private static synchronized void enqueue(ServerLevel level, FractureShrinePlan plan) {
        Deque<FractureShrinePlan> queue = PENDING.computeIfAbsent(level.dimension(), ignored -> new ArrayDeque<>());
        if (queue.stream().noneMatch(pending -> pending.variant() == plan.variant())) {
            queue.addLast(plan);
        }
    }

    private static synchronized FractureShrinePlan poll(ServerLevel level) {
        Deque<FractureShrinePlan> queue = PENDING.get(level.dimension());
        return queue == null ? null : queue.pollFirst();
    }

    private static synchronized Set<Long> pendingChunks(ServerLevel level) {
        return PENDING_CHUNKS.computeIfAbsent(level.dimension(), ignored -> {
            Set<Long> chunks = new HashSet<>();
            for (FractureShrinePlan plan : AdventureWorldManager.unbuiltShrines(level)) {
                chunks.add(plan.chunk().toLong());
            }
            return chunks;
        });
    }
}
