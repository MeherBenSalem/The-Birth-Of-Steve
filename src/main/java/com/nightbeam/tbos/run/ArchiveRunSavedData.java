package com.nightbeam.tbos.run;

import com.nightbeam.tbos.Yesterglass;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Server-wide durable storage for archive runs and their exclusive allocations. */
public final class ArchiveRunSavedData extends SavedData {
    public static final int SCHEMA_REVISION = 1;

    private static final Codec<ArchiveRunSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_revision", SCHEMA_REVISION).forGetter(data -> SCHEMA_REVISION),
            ArchiveRun.CODEC.listOf().optionalFieldOf("runs", List.of())
                    .forGetter(data -> List.copyOf(data.runs.values()))
    ).apply(instance, ArchiveRunSavedData::fromCodec));

    public static final SavedDataType<ArchiveRunSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive_runs"),
            ArchiveRunSavedData::new,
            CODEC);

    private final Map<UUID, ArchiveRun> runs = new LinkedHashMap<>();
    private final Map<UUID, UUID> activeRunByMember = new LinkedHashMap<>();
    private final Map<Integer, UUID> activeRunBySlot = new LinkedHashMap<>();

    public static ArchiveRunSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public ArchiveRun register(ArchiveRun run) {
        ArchiveRun existing = runs.get(run.runId());
        if (existing != null) {
            if (!existing.equals(run)) {
                throw new IllegalArgumentException("Archive run ID is already registered with different state: "
                        + run.runId());
            }
            return existing;
        }

        validateAllocation(run, null);
        runs.put(run.runId(), run);
        rebuildIndexes();
        setDirty();
        return run;
    }

    public ArchiveRun replace(ArchiveRun run) {
        if (!runs.containsKey(run.runId())) {
            throw new IllegalArgumentException("Cannot replace an unknown archive run: " + run.runId());
        }

        validateAllocation(run, run.runId());
        runs.put(run.runId(), run);
        rebuildIndexes();
        setDirty();
        return run;
    }

    public Optional<ArchiveRun> find(UUID runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    public Optional<ArchiveRun> findBySlot(int instanceSlot) {
        UUID runId = activeRunBySlot.get(instanceSlot);
        return runId == null ? Optional.empty() : find(runId);
    }

    public Optional<ArchiveRun> findByMember(UUID playerId) {
        UUID runId = activeRunByMember.get(playerId);
        return runId == null ? Optional.empty() : find(runId);
    }

    public Optional<ArchiveRun> findPendingReturnByMember(UUID playerId) {
        return runs.values().stream()
                .filter(run -> run.status().isTerminal())
                .filter(run -> run.member(playerId).filter(member -> !member.returned()).isPresent())
                .reduce((first, second) -> second);
    }

    public List<ArchiveRun> all() {
        return List.copyOf(runs.values());
    }

    public int size() {
        return runs.size();
    }

    public int nextFreeSlot() {
        int slot = 0;
        while (activeRunBySlot.containsKey(slot) || ArchiveGenerationQueue.isSlotReserved(slot)) {
            slot++;
        }
        return slot;
    }

    public Optional<ArchiveRun> remove(UUID runId) {
        ArchiveRun removed = runs.remove(runId);
        if (removed == null) {
            return Optional.empty();
        }
        rebuildIndexes();
        setDirty();
        return Optional.of(removed);
    }

    private void validateAllocation(ArchiveRun run, UUID ignoredRunId) {
        if (!run.status().holdsInstanceSlot()) {
            return;
        }

        UUID slotOwner = activeRunBySlot.get(run.instanceSlot());
        if (slotOwner != null && !slotOwner.equals(ignoredRunId)) {
            throw new IllegalArgumentException(
                    "Archive instance slot " + run.instanceSlot() + " is already held by " + slotOwner);
        }
        for (ArchiveRunMember member : run.members()) {
            UUID memberRun = activeRunByMember.get(member.playerId());
            if (memberRun != null && !memberRun.equals(ignoredRunId)) {
                throw new IllegalArgumentException(
                        "Archive member " + member.playerId() + " is already assigned to " + memberRun);
            }
        }
    }

    private void rebuildIndexes() {
        activeRunByMember.clear();
        activeRunBySlot.clear();
        for (ArchiveRun run : runs.values()) {
            if (!run.status().holdsInstanceSlot()) {
                continue;
            }

            UUID previousSlot = activeRunBySlot.putIfAbsent(run.instanceSlot(), run.runId());
            if (previousSlot != null) {
                throw new IllegalStateException(
                        "Archive runs " + previousSlot + " and " + run.runId()
                                + " both reserve instance slot " + run.instanceSlot());
            }
            for (ArchiveRunMember member : run.members()) {
                UUID previousRun = activeRunByMember.putIfAbsent(member.playerId(), run.runId());
                if (previousRun != null) {
                    throw new IllegalStateException(
                            "Archive member " + member.playerId() + " belongs to both " + previousRun
                                    + " and " + run.runId());
                }
            }
        }
    }

    private static ArchiveRunSavedData fromCodec(int schemaRevision, List<ArchiveRun> decodedRuns) {
        if (schemaRevision < 1 || schemaRevision > SCHEMA_REVISION) {
            throw new IllegalArgumentException("Unsupported archive run storage schema revision: " + schemaRevision);
        }
        ArchiveRunSavedData data = new ArchiveRunSavedData();
        for (ArchiveRun run : decodedRuns) {
            ArchiveRun previous = data.runs.putIfAbsent(run.runId(), run);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate archive run ID in saved data: " + run.runId());
            }
        }
        data.rebuildIndexes();
        return data;
    }
}
