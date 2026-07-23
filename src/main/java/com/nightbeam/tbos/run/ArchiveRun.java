package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;

public record ArchiveRun(
        int schemaRevision,
        UUID runId,
        long seed,
        int instanceSlot,
        List<ArchiveRunMember> members,
        List<ArchiveRoomPlan> rooms,
        int currentRoom,
        int checkpointRoom,
        int sharedRevives,
        ArchiveRunStatus status,
        long returnDeadlineTick,
        boolean geometryPlaced,
        ArchiveEncounterState encounterState,
        ArchiveDungeonGraph dungeonGraph,
        List<ArchiveEncounterState> roomEncounterStates) {
    public static final int SCHEMA_REVISION = 4;
    public static final int MAX_PARTY_SIZE = 4;
    public static final int MAX_SHARED_REVIVES = 3;
    public static final int MAX_PARTY_CONTAINER_KEYS = 48 * 64;

    public static final Codec<ArchiveRun> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_revision", SCHEMA_REVISION).forGetter(ArchiveRun::schemaRevision),
            UUIDUtil.CODEC.fieldOf("run_id").forGetter(ArchiveRun::runId),
            Codec.LONG.fieldOf("seed").forGetter(ArchiveRun::seed),
            Codec.INT.fieldOf("instance_slot").forGetter(ArchiveRun::instanceSlot),
            ArchiveRunMember.CODEC.listOf().fieldOf("members").forGetter(ArchiveRun::members),
            ArchiveRoomPlan.CODEC.listOf().fieldOf("rooms").forGetter(ArchiveRun::rooms),
            Codec.INT.optionalFieldOf("current_room", 0).forGetter(ArchiveRun::currentRoom),
            Codec.INT.optionalFieldOf("checkpoint_room", 0).forGetter(ArchiveRun::checkpointRoom),
            Codec.INT.optionalFieldOf("shared_revives", MAX_SHARED_REVIVES).forGetter(ArchiveRun::sharedRevives),
            ArchiveRunStatus.CODEC.optionalFieldOf("status", ArchiveRunStatus.PREPARING).forGetter(ArchiveRun::status),
            Codec.LONG.optionalFieldOf("return_deadline_tick", -1L).forGetter(ArchiveRun::returnDeadlineTick),
            Codec.BOOL.optionalFieldOf("geometry_placed", false).forGetter(ArchiveRun::geometryPlaced),
            ArchiveEncounterState.CODEC.optionalFieldOf("encounter", ArchiveEncounterState.IDLE)
                    .forGetter(ArchiveRun::encounterState),
            ArchiveDungeonGraph.CODEC.optionalFieldOf("dungeon_graph")
                    .forGetter(run -> Optional.of(run.dungeonGraph())),
            ArchiveEncounterState.CODEC.listOf().optionalFieldOf("room_encounters", List.of())
                    .forGetter(ArchiveRun::roomEncounterStates)
    ).apply(instance, (schemaRevision, runId, seed, instanceSlot, members, rooms, currentRoom,
            checkpointRoom, sharedRevives, status, returnDeadlineTick, geometryPlaced,
            encounterState, graph, roomEncounters) -> new ArchiveRun(
                    schemaRevision,
                    runId,
                    seed,
                    instanceSlot,
                    members,
                    rooms,
                    currentRoom,
                    checkpointRoom,
                    sharedRevives,
                    status,
                    returnDeadlineTick,
                    geometryPlaced,
                    encounterState,
                    graph.orElseGet(() -> ArchiveDungeonGraph.fromLegacy(seed, rooms)),
                    normalizeEncounters(rooms.size(), currentRoom, encounterState, roomEncounters))));

    public ArchiveRun(
            int schemaRevision,
            UUID runId,
            long seed,
            int instanceSlot,
            List<ArchiveRunMember> members,
            List<ArchiveRoomPlan> rooms,
            int currentRoom,
            int checkpointRoom,
            int sharedRevives,
            ArchiveRunStatus status,
            long returnDeadlineTick,
            boolean geometryPlaced) {
        this(
                schemaRevision,
                runId,
                seed,
                instanceSlot,
                members,
                rooms,
                currentRoom,
                checkpointRoom,
                sharedRevives,
                status,
                returnDeadlineTick,
                geometryPlaced,
                ArchiveEncounterState.IDLE,
                ArchiveDungeonGraph.fromLegacy(seed, rooms),
                idleEncounters(rooms.size()));
    }

    public ArchiveRun(
            int schemaRevision,
            UUID runId,
            long seed,
            int instanceSlot,
            List<ArchiveRunMember> members,
            List<ArchiveRoomPlan> rooms,
            int currentRoom,
            int checkpointRoom,
            int sharedRevives,
            ArchiveRunStatus status,
            long returnDeadlineTick,
            boolean geometryPlaced,
            ArchiveEncounterState encounterState) {
        this(
                schemaRevision,
                runId,
                seed,
                instanceSlot,
                members,
                rooms,
                currentRoom,
                checkpointRoom,
                sharedRevives,
                status,
                returnDeadlineTick,
                geometryPlaced,
                encounterState,
                ArchiveDungeonGraph.fromLegacy(seed, rooms),
                normalizeEncounters(rooms.size(), currentRoom, encounterState, List.of()));
    }

    public static ArchiveRun create(
            UUID runId,
            long seed,
            int instanceSlot,
            List<ArchiveRunMember> members,
            ArchiveDungeonGraph dungeonGraph) {
        List<ArchiveRoomPlan> plans = dungeonGraph.roomPlans();
        return new ArchiveRun(
                SCHEMA_REVISION,
                runId,
                seed,
                instanceSlot,
                members,
                plans,
                dungeonGraph.startingRoom(),
                dungeonGraph.startingRoom(),
                MAX_SHARED_REVIVES,
                ArchiveRunStatus.PREPARING,
                -1L,
                false,
                ArchiveEncounterState.IDLE,
                dungeonGraph,
                idleEncounters(plans.size()));
    }

    public ArchiveRun {
        runId = Objects.requireNonNull(runId, "runId");
        members = List.copyOf(Objects.requireNonNull(members, "members"));
        rooms = List.copyOf(Objects.requireNonNull(rooms, "rooms"));
        status = Objects.requireNonNull(status, "status");
        encounterState = Objects.requireNonNull(encounterState, "encounterState");
        dungeonGraph = Objects.requireNonNull(dungeonGraph, "dungeonGraph");
        roomEncounterStates = List.copyOf(Objects.requireNonNull(roomEncounterStates, "roomEncounterStates"));
        if (schemaRevision < 1) {
            throw new IllegalArgumentException("Archive run schema revision must be positive");
        }
        if (instanceSlot < 0) {
            throw new IllegalArgumentException("Archive run instance slot must not be negative");
        }
        if (members.isEmpty() || members.size() > MAX_PARTY_SIZE) {
            throw new IllegalArgumentException("Archive run party size must be between 1 and " + MAX_PARTY_SIZE);
        }
        if (new HashSet<>(members.stream().map(ArchiveRunMember::playerId).toList()).size() != members.size()) {
            throw new IllegalArgumentException("Archive run members must be unique");
        }
        if (rooms.size() < 7 || rooms.size() > 48) {
            throw new IllegalArgumentException("Archive run must contain between 7 and 48 rooms");
        }
        if (new HashSet<>(rooms.stream().map(ArchiveRoomPlan::slot).toList()).size() != rooms.size()) {
            throw new IllegalArgumentException("Archive run room slots must be unique");
        }
        if (currentRoom < 0 || currentRoom >= rooms.size()) {
            throw new IllegalArgumentException("Current room is outside the run plan: " + currentRoom);
        }
        if (checkpointRoom < 0 || checkpointRoom >= rooms.size()) {
            throw new IllegalArgumentException("Checkpoint room is outside the run plan");
        }
        if (sharedRevives < 0 || sharedRevives > MAX_SHARED_REVIVES) {
            throw new IllegalArgumentException("Shared revives must be between 0 and " + MAX_SHARED_REVIVES);
        }
        if (status.isReturning() && returnDeadlineTick < 0L) {
            throw new IllegalArgumentException("Returning archive runs require a nonnegative deadline");
        }
        if (!status.isReturning() && returnDeadlineTick != -1L) {
            throw new IllegalArgumentException("Only returning archive runs may store a return deadline");
        }
        if (status != ArchiveRunStatus.PREPARING && status != ArchiveRunStatus.FAILED && !geometryPlaced) {
            throw new IllegalArgumentException("A non-preparing archive run requires placed geometry");
        }
        if (dungeonGraph.seed() != seed || dungeonGraph.rooms().size() != rooms.size()) {
            throw new IllegalArgumentException("Archive dungeon graph does not match the run seed or room count");
        }
        if (roomEncounterStates.size() != rooms.size()) {
            throw new IllegalArgumentException("Archive run requires one persisted encounter state per room");
        }
        int roomCount = rooms.size();
        for (ArchiveRunMember member : members) {
            if (member.currentRoom() >= roomCount || member.checkpointRoom() >= roomCount) {
                throw new IllegalArgumentException("Archive member room state is outside the run graph");
            }
        }
        encounterState = roomEncounterStates.get(currentRoom);
    }

    public Optional<ArchiveRunMember> member(UUID playerId) {
        return members.stream().filter(member -> member.playerId().equals(playerId)).findFirst();
    }

    public boolean containsMember(UUID playerId) {
        return member(playerId).isPresent();
    }

    public ArchiveRun markGeometryPlaced() {
        return copy(currentRoom, checkpointRoom, sharedRevives, status, returnDeadlineTick, true);
    }

    public ArchiveRun activate() {
        requireStatus(ArchiveRunStatus.PREPARING);
        if (!geometryPlaced) {
            throw new IllegalStateException("Archive geometry must be placed before activation");
        }
        return copy(currentRoom, checkpointRoom, sharedRevives, ArchiveRunStatus.ACTIVE, -1L, true);
    }

    public ArchiveRun abortPreparation() {
        requireStatus(ArchiveRunStatus.PREPARING);
        return copy(currentRoom, checkpointRoom, sharedRevives, ArchiveRunStatus.FAILED, -1L, geometryPlaced);
    }

    public ArchiveRun advanceTo(int roomIndex) {
        requireStatus(ArchiveRunStatus.ACTIVE);
        if (roomIndex < currentRoom || roomIndex > Math.min(currentRoom + 1, rooms.size() - 1)) {
            throw new IllegalArgumentException("Archive run may advance by at most one room");
        }
        return copy(roomIndex, checkpointRoom, sharedRevives, status, -1L, true, ArchiveEncounterState.IDLE);
    }

    public ArchiveRun visitRoom(int roomIndex) {
        return visitRoom(roomIndex, members.stream().map(ArchiveRunMember::playerId).toList());
    }

    public ArchiveRun visitRoom(int roomIndex, List<UUID> visitingMembers) {
        requireStatus(ArchiveRunStatus.ACTIVE);
        if (roomIndex < 0 || roomIndex >= rooms.size()) {
            throw new IllegalArgumentException("Visited archive room is outside the run plan");
        }
        Set<UUID> visitors = Set.copyOf(visitingMembers);
        if (visitors.stream().anyMatch(visitor -> !containsMember(visitor))) {
            throw new IllegalArgumentException("A room visitor is not a member of archive run " + runId);
        }
        ArchiveDungeonGraph graph = dungeonGraph.visitRoom(roomIndex);
        List<ArchiveRunMember> updatedMembers = members.stream()
                .map(member -> visitors.contains(member.playerId()) ? member.visitRoom(roomIndex) : member)
                .toList();
        return copy(
                roomIndex,
                roomIndex,
                sharedRevives,
                status,
                -1L,
                true,
                roomEncounterStates.get(roomIndex),
                graph,
                roomEncounterStates,
                updatedMembers);
    }

    public ArchiveRun withEncounterState(ArchiveEncounterState state) {
        return withRoomEncounterState(currentRoom, state);
    }

    public ArchiveRun withRoomEncounterState(int roomIndex, ArchiveEncounterState state) {
        requireStatus(ArchiveRunStatus.ACTIVE);
        if (roomIndex < 0 || roomIndex >= rooms.size()) {
            throw new IllegalArgumentException("Archive encounter room is outside the run plan");
        }
        java.util.ArrayList<ArchiveEncounterState> updated = new java.util.ArrayList<>(roomEncounterStates);
        updated.set(roomIndex, Objects.requireNonNull(state, "state"));
        ArchiveEncounterState current = roomIndex == currentRoom ? state : encounterState;
        return copy(currentRoom, checkpointRoom, sharedRevives, status, -1L, true,
                current, dungeonGraph, updated);
    }

    public ArchiveRun withDungeonGraph(ArchiveDungeonGraph graph) {
        requireStatus(ArchiveRunStatus.ACTIVE);
        return copy(currentRoom, checkpointRoom, sharedRevives, status, -1L, true,
                encounterState, graph, roomEncounterStates);
    }

    public ArchiveRun lockRoom(int roomIndex) {
        return withDungeonGraph(dungeonGraph.setRoomLocked(roomIndex, true));
    }

    public ArchiveRun completeRoom(int roomIndex) {
        return withDungeonGraph(dungeonGraph.completeRoom(roomIndex));
    }

    public ArchiveRun discoverSecretRoom(int roomIndex) {
        return withDungeonGraph(dungeonGraph.discoverSecret(roomIndex));
    }

    public ArchiveRun openContainer(int roomIndex, int markerIndex) {
        return withDungeonGraph(dungeonGraph.openContainer(roomIndex, markerIndex));
    }

    public boolean hasMemberClaimedContainer(UUID playerId, int roomIndex, int markerIndex) {
        return member(playerId).orElseThrow().hasClaimedContainer(containerKey(roomIndex, markerIndex));
    }

    public ArchiveRun claimMemberContainer(UUID playerId, int roomIndex, int markerIndex) {
        int key = containerKey(roomIndex, markerIndex);
        boolean found = false;
        List<ArchiveRunMember> updatedMembers = new java.util.ArrayList<>(members.size());
        for (ArchiveRunMember member : members) {
            if (member.playerId().equals(playerId)) {
                found = true;
                updatedMembers.add(member.claimContainer(key));
            } else {
                updatedMembers.add(member);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Player is not a member of archive run " + runId + ": " + playerId);
        }
        return copyWithMembers(updatedMembers);
    }

    public boolean allMembersClaimedContainer(int roomIndex, int markerIndex) {
        int key = containerKey(roomIndex, markerIndex);
        return members.stream().allMatch(member -> member.hasClaimedContainer(key));
    }

    public ArchiveRun unlockAllRooms() {
        return withDungeonGraph(dungeonGraph.unlockAllDoors());
    }

    public ArchiveRun regenerate(ArchiveDungeonGraph graph) {
        if (!status.holdsInstanceSlot()) {
            throw new IllegalStateException("Only a live archive run can be regenerated");
        }
        List<ArchiveRoomPlan> plans = graph.roomPlans();
        return new ArchiveRun(
                SCHEMA_REVISION,
                runId,
                seed,
                instanceSlot,
                members.stream().map(member -> member.resetForRegeneration(graph.startingRoom())).toList(),
                plans,
                graph.startingRoom(),
                graph.startingRoom(),
                MAX_SHARED_REVIVES,
                ArchiveRunStatus.PREPARING,
                -1L,
                false,
                ArchiveEncounterState.IDLE,
                graph,
                idleEncounters(plans.size()));
    }

    public ArchiveRun checkpoint(int roomIndex) {
        requireStatus(ArchiveRunStatus.ACTIVE);
        if (roomIndex < 0 || roomIndex >= rooms.size()) {
            throw new IllegalArgumentException("Checkpoint must be inside the current dungeon graph");
        }
        return copy(currentRoom, roomIndex, sharedRevives, status, -1L, true);
    }

    public ArchiveRun checkpoint(UUID playerId, int roomIndex) {
        requireStatus(ArchiveRunStatus.ACTIVE);
        if (roomIndex < 0 || roomIndex >= rooms.size()) {
            throw new IllegalArgumentException("Checkpoint must be inside the current dungeon graph");
        }
        boolean found = false;
        List<ArchiveRunMember> updated = new java.util.ArrayList<>(members.size());
        for (ArchiveRunMember member : members) {
            if (member.playerId().equals(playerId)) {
                found = true;
                updated.add(member.checkpoint(roomIndex));
            } else {
                updated.add(member);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Player is not a member of archive run " + runId + ": " + playerId);
        }
        return copy(
                currentRoom,
                checkpointRoom,
                sharedRevives,
                status,
                -1L,
                true,
                encounterState,
                dungeonGraph,
                roomEncounterStates,
                updated);
    }

    public ArchiveRun consumeRevive() {
        requireStatus(ArchiveRunStatus.ACTIVE);
        if (sharedRevives == 0) {
            throw new IllegalStateException("No shared revives remain");
        }
        return copy(currentRoom, checkpointRoom, sharedRevives - 1, status, -1L, true);
    }

    public ArchiveRun restoreRevive() {
        requireStatus(ArchiveRunStatus.ACTIVE);
        if (sharedRevives >= MAX_SHARED_REVIVES) {
            throw new IllegalStateException("Shared revives are already full");
        }
        return copy(currentRoom, checkpointRoom, sharedRevives + 1, status, -1L, true);
    }

    public ArchiveRun beginReturn(long deadlineTick) {
        requireStatus(ArchiveRunStatus.ACTIVE);
        return copy(currentRoom, checkpointRoom, sharedRevives, ArchiveRunStatus.RETURNING_VICTORY, deadlineTick, true);
    }

    public ArchiveRun fail(long deadlineTick) {
        requireStatus(ArchiveRunStatus.ACTIVE);
        return copy(currentRoom, checkpointRoom, sharedRevives, ArchiveRunStatus.RETURNING_FAILURE, deadlineTick, true);
    }

    public ArchiveRun complete() {
        if (status == ArchiveRunStatus.RETURNING_VICTORY) {
            return copy(currentRoom, checkpointRoom, sharedRevives, ArchiveRunStatus.COMPLETED, -1L, true);
        }
        if (status == ArchiveRunStatus.RETURNING_FAILURE) {
            return copy(currentRoom, checkpointRoom, sharedRevives, ArchiveRunStatus.FAILED, -1L, true);
        }
        throw new IllegalStateException("Only a returning archive run can complete");
    }

    public ArchiveRun markMemberReturned(UUID playerId) {
        if (!status.isTerminal()) {
            throw new IllegalStateException("Members may be marked returned only after terminal state is persisted");
        }
        boolean found = false;
        List<ArchiveRunMember> updatedMembers = new java.util.ArrayList<>(members.size());
        for (ArchiveRunMember member : members) {
            if (member.playerId().equals(playerId)) {
                found = true;
                updatedMembers.add(member.markReturned());
            } else {
                updatedMembers.add(member);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Player is not a member of archive run " + runId + ": " + playerId);
        }
        return copyWithMembers(updatedMembers);
    }

    public ArchiveRun claimReward(UUID playerId) {
        if (status != ArchiveRunStatus.RETURNING_VICTORY && status != ArchiveRunStatus.COMPLETED) {
            throw new IllegalStateException("Archive rewards may only be claimed after victory");
        }
        boolean found = false;
        List<ArchiveRunMember> updatedMembers = new java.util.ArrayList<>(members.size());
        for (ArchiveRunMember member : members) {
            if (member.playerId().equals(playerId)) {
                found = true;
                updatedMembers.add(member.claimReward());
            } else {
                updatedMembers.add(member);
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Player is not a member of archive run " + runId + ": " + playerId);
        }
        return copyWithMembers(updatedMembers);
    }

    public boolean allMembersReturned() {
        return members.stream().allMatch(ArchiveRunMember::returned);
    }

    private ArchiveRun copyWithMembers(List<ArchiveRunMember> updatedMembers) {
        return new ArchiveRun(
                schemaRevision,
                runId,
                seed,
                instanceSlot,
                updatedMembers,
                rooms,
                currentRoom,
                checkpointRoom,
                sharedRevives,
                status,
                returnDeadlineTick,
                geometryPlaced,
                encounterState,
                dungeonGraph,
                roomEncounterStates);
    }

    private static int containerKey(int roomIndex, int markerIndex) {
        if (roomIndex < 0 || roomIndex >= 48 || markerIndex < 0 || markerIndex >= 64) {
            throw new IllegalArgumentException("Archive room or container marker is outside the claim key range");
        }
        return roomIndex * 64 + markerIndex;
    }

    private void requireStatus(ArchiveRunStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Expected archive run status " + expected + " but was " + status);
        }
    }

    private ArchiveRun copy(
            int newCurrentRoom,
            int newCheckpointRoom,
            int newSharedRevives,
            ArchiveRunStatus newStatus,
            long newReturnDeadlineTick,
            boolean newGeometryPlaced) {
        return copy(
                newCurrentRoom,
                newCheckpointRoom,
                newSharedRevives,
                newStatus,
                newReturnDeadlineTick,
                newGeometryPlaced,
                encounterState);
    }

    private ArchiveRun copy(
            int newCurrentRoom,
            int newCheckpointRoom,
            int newSharedRevives,
            ArchiveRunStatus newStatus,
            long newReturnDeadlineTick,
            boolean newGeometryPlaced,
            ArchiveEncounterState newEncounterState) {
        return copy(
                newCurrentRoom,
                newCheckpointRoom,
                newSharedRevives,
                newStatus,
                newReturnDeadlineTick,
                newGeometryPlaced,
                newEncounterState,
                dungeonGraph,
                roomEncounterStates);
    }

    private ArchiveRun copy(
            int newCurrentRoom,
            int newCheckpointRoom,
            int newSharedRevives,
            ArchiveRunStatus newStatus,
            long newReturnDeadlineTick,
            boolean newGeometryPlaced,
            ArchiveEncounterState newEncounterState,
            ArchiveDungeonGraph newDungeonGraph,
            List<ArchiveEncounterState> newRoomEncounterStates) {
        return copy(
                newCurrentRoom,
                newCheckpointRoom,
                newSharedRevives,
                newStatus,
                newReturnDeadlineTick,
                newGeometryPlaced,
                newEncounterState,
                newDungeonGraph,
                newRoomEncounterStates,
                members);
    }

    private ArchiveRun copy(
            int newCurrentRoom,
            int newCheckpointRoom,
            int newSharedRevives,
            ArchiveRunStatus newStatus,
            long newReturnDeadlineTick,
            boolean newGeometryPlaced,
            ArchiveEncounterState newEncounterState,
            ArchiveDungeonGraph newDungeonGraph,
            List<ArchiveEncounterState> newRoomEncounterStates,
            List<ArchiveRunMember> newMembers) {
        return new ArchiveRun(
                schemaRevision,
                runId,
                seed,
                instanceSlot,
                newMembers,
                rooms,
                newCurrentRoom,
                newCheckpointRoom,
                newSharedRevives,
                newStatus,
                newReturnDeadlineTick,
                newGeometryPlaced,
                newEncounterState,
                newDungeonGraph,
                newRoomEncounterStates);
    }

    private static List<ArchiveEncounterState> idleEncounters(int roomCount) {
        return List.copyOf(java.util.Collections.nCopies(roomCount, ArchiveEncounterState.IDLE));
    }

    private static List<ArchiveEncounterState> normalizeEncounters(
            int roomCount,
            int currentRoom,
            ArchiveEncounterState encounterState,
            List<ArchiveEncounterState> decoded) {
        if (decoded.size() == roomCount) {
            return List.copyOf(decoded);
        }
        java.util.ArrayList<ArchiveEncounterState> result = new java.util.ArrayList<>(idleEncounters(roomCount));
        if (currentRoom >= 0 && currentRoom < roomCount) {
            result.set(currentRoom, encounterState);
        }
        return List.copyOf(result);
    }
}
