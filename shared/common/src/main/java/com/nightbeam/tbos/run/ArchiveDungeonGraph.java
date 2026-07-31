package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

/** Immutable, codec-backed 3D dungeon graph and its durable room/door state. */
public record ArchiveDungeonGraph(
        int schemaRevision,
        long seed,
        List<ArchiveRoomNode> rooms,
        int startingRoom,
        int bossRoom,
        int rewardRoom) {
    public static final int SCHEMA_REVISION = 2;
    public static final int GRID_SPACING_XZ = 34;
    public static final int GRID_SPACING_Y = 16;

    public static final Codec<ArchiveDungeonGraph> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_revision", SCHEMA_REVISION)
                    .forGetter(ArchiveDungeonGraph::schemaRevision),
            Codec.LONG.fieldOf("seed").forGetter(ArchiveDungeonGraph::seed),
            ArchiveRoomNode.CODEC.listOf().fieldOf("rooms").forGetter(ArchiveDungeonGraph::rooms),
            Codec.INT.optionalFieldOf("starting_room", 0).forGetter(ArchiveDungeonGraph::startingRoom),
            Codec.INT.fieldOf("boss_room").forGetter(ArchiveDungeonGraph::bossRoom),
            Codec.INT.fieldOf("reward_room").forGetter(ArchiveDungeonGraph::rewardRoom)
    ).apply(instance, ArchiveDungeonGraph::new));

    public ArchiveDungeonGraph {
        rooms = List.copyOf(Objects.requireNonNull(rooms, "rooms"));
        if (schemaRevision < 1 || schemaRevision > SCHEMA_REVISION) {
            throw new IllegalArgumentException("Unsupported archive dungeon graph schema: " + schemaRevision);
        }
        if (rooms.size() < 7 || rooms.size() > 48) {
            throw new IllegalArgumentException("Archive dungeon graph must contain between 7 and 48 rooms");
        }
        requireIndex("starting", startingRoom, rooms.size());
        requireIndex("boss", bossRoom, rooms.size());
        requireIndex("reward", rewardRoom, rooms.size());
        for (int index = 0; index < rooms.size(); index++) {
            ArchiveRoomNode room = rooms.get(index);
            if (room.index() != index) {
                throw new IllegalArgumentException("Archive dungeon room indices must be contiguous: " + room.index());
            }
            for (ArchiveConnection connection : room.connections()) {
                requireIndex("connection", connection.targetRoom(), rooms.size());
                ArchiveRoomNode target = rooms.get(connection.targetRoom());
                if (!room.placement().coordinates().offset(connection.direction())
                        .equals(target.placement().coordinates())) {
                    throw new IllegalArgumentException("Archive connection does not join cardinal neighbors: "
                            + room.index() + " -> " + target.index());
                }
                boolean reciprocal = target.connections().stream().anyMatch(candidate ->
                        candidate.targetRoom() == room.index()
                                && candidate.direction() == connection.direction().opposite()
                                && candidate.hidden() == connection.hidden()
                                && candidate.locked() == connection.locked());
                if (!reciprocal) {
                    throw new IllegalArgumentException("Archive connection is not reciprocal: "
                            + room.index() + " -> " + target.index());
                }
            }
        }
        if (overlapCount(rooms) != 0) {
            throw new IllegalArgumentException("Archive dungeon graph contains overlapping room volumes");
        }
        if (reachableIndices(rooms, startingRoom).size() != rooms.size()) {
            throw new IllegalArgumentException("Archive dungeon graph contains rooms unreachable from the start");
        }
        if (schemaRevision >= 2) {
            if (startingRoom == bossRoom || bossRoom == rewardRoom || startingRoom == rewardRoom) {
                throw new IllegalArgumentException("Procedural start, boss, and reward rooms must be distinct");
            }
            if (rooms.get(startingRoom).category() != ArchiveRoomCategory.STARTING
                    || rooms.get(bossRoom).category() != ArchiveRoomCategory.FINAL_BOSS
                    || rooms.get(rewardRoom).category() != ArchiveRoomCategory.EXIT_REWARD) {
                throw new IllegalArgumentException("Archive mandatory room indices do not match their categories");
            }
        }
    }

    public ArchiveRoomNode room(int index) {
        requireIndex("room", index, rooms.size());
        return rooms.get(index);
    }

    public List<ArchiveRoomPlan> roomPlans() {
        return rooms.stream().map(room -> room.toPlan(seed)).toList();
    }

    public Set<Integer> reachableRooms() {
        return Set.copyOf(reachableIndices(rooms, startingRoom));
    }

    public int unreachableRoomCount() {
        return rooms.size() - reachableIndices(rooms, startingRoom).size();
    }

    public int overlapCount() {
        return overlapCount(rooms);
    }

    public int branchCount() {
        return (int) rooms.stream().filter(room -> room.connections().size() >= 3).count();
    }

    public int verticalRoomCount() {
        return (int) rooms.stream().filter(room -> room.placement().coordinates().y() != 0).count();
    }

    public int loopCount() {
        int undirectedEdges = rooms.stream().mapToInt(room -> room.connections().size()).sum() / 2;
        return Math.max(0, undirectedEdges - rooms.size() + 1);
    }

    public OptionalInt roomAt(ArchiveGridPos coordinates) {
        return rooms.stream()
                .filter(room -> room.placement().coordinates().equals(coordinates))
                .mapToInt(ArchiveRoomNode::index)
                .findFirst();
    }

    public ArchiveDungeonGraph visitRoom(int roomIndex) {
        return withRuntime(roomIndex, room(roomIndex).runtime().visit());
    }

    public ArchiveDungeonGraph setRoomLocked(int roomIndex, boolean locked) {
        ArchiveRoomNode room = room(roomIndex);
        ArrayList<ArchiveRoomNode> updated = new ArrayList<>(rooms);
        updated.set(roomIndex, room.withRuntime(room.runtime().visit().withDoorsLocked(locked))
                .withConnections(room.connections().stream().map(connection -> connection.withLocked(locked)).toList()));
        for (ArchiveConnection connection : room.connections()) {
            ArchiveRoomNode target = updated.get(connection.targetRoom());
            updated.set(target.index(), target.withConnections(target.connections().stream()
                    .map(candidate -> candidate.targetRoom() == roomIndex ? candidate.withLocked(locked) : candidate)
                    .toList()));
        }
        return copy(updated);
    }

    public ArchiveDungeonGraph setConnectionLocked(int firstRoom, int secondRoom, boolean locked) {
        ArchiveRoomNode first = room(firstRoom);
        ArchiveRoomNode second = room(secondRoom);
        boolean connected = first.connections().stream()
                .anyMatch(connection -> connection.targetRoom() == secondRoom);
        if (!connected) {
            throw new IllegalArgumentException(
                    "Archive rooms are not connected: " + firstRoom + " -> " + secondRoom);
        }
        ArrayList<ArchiveRoomNode> updated = new ArrayList<>(rooms);
        updated.set(firstRoom, first.withConnections(first.connections().stream()
                .map(connection -> connection.targetRoom() == secondRoom
                        ? connection.withLocked(locked)
                        : connection)
                .toList()));
        updated.set(secondRoom, second.withConnections(second.connections().stream()
                .map(connection -> connection.targetRoom() == firstRoom
                        ? connection.withLocked(locked)
                        : connection)
                .toList()));
        return copy(updated);
    }

    public ArchiveDungeonGraph completeRoom(int roomIndex) {
        ArchiveDungeonGraph marked = withRuntime(roomIndex, room(roomIndex).runtime().complete());
        ArchiveDungeonGraph unlocked = marked.setRoomLocked(roomIndex, false);
        if (roomIndex == bossRoom) {
            unlocked = unlocked.setRoomLocked(rewardRoom, false);
            return unlocked;
        }
        return unlocked.setBossEntranceLocked(!ArchiveQuestProgress.from(unlocked).complete());
    }

    public ArchiveDungeonGraph discoverSecret(int roomIndex) {
        ArchiveRoomNode room = room(roomIndex);
        if (room.category() != ArchiveRoomCategory.SECRET) {
            throw new IllegalArgumentException("Only a secret archive room may be discovered");
        }
        ArrayList<ArchiveRoomNode> updated = new ArrayList<>(rooms);
        updated.set(roomIndex, room.withRuntime(room.runtime().discoverSecret())
                .withConnections(room.connections().stream().map(ArchiveConnection::reveal).toList()));
        for (ArchiveConnection connection : room.connections()) {
            ArchiveRoomNode target = updated.get(connection.targetRoom());
            updated.set(target.index(), target.withConnections(target.connections().stream()
                    .map(candidate -> candidate.targetRoom() == roomIndex ? candidate.reveal() : candidate)
                    .toList()));
        }
        return copy(updated);
    }

    public ArchiveDungeonGraph openContainer(int roomIndex, int markerIndex) {
        return withRuntime(roomIndex, room(roomIndex).runtime().openContainer(markerIndex));
    }

    public ArchiveDungeonGraph claimUniqueReward(int roomIndex) {
        return withRuntime(roomIndex, room(roomIndex).runtime().claimUniqueReward());
    }

    public ArchiveDungeonGraph unlockAllDoors() {
        ArrayList<ArchiveRoomNode> updated = new ArrayList<>(rooms.size());
        for (ArchiveRoomNode room : rooms) {
            updated.add(room.withRuntime(room.runtime().withDoorsLocked(false))
                    .withConnections(room.connections().stream()
                            .map(connection -> connection.withLocked(false))
                            .toList()));
        }
        return copy(updated);
    }

    public static ArchiveDungeonGraph fromLegacy(long seed, List<ArchiveRoomPlan> plans) {
        if (plans.size() < 7) {
            throw new IllegalArgumentException("Legacy archive plans require at least seven rooms");
        }
        ArrayList<ArchiveRoomNode> nodes = new ArrayList<>();
        for (int index = 0; index < plans.size(); index++) {
            ArchiveRoomPlan plan = plans.get(index);
            ArchiveRoomTemplate template = ArchiveRoomTemplates.require(plan.roomId());
            ArrayList<ArchiveConnection> connections = new ArrayList<>();
            if (index > 0) {
                connections.add(new ArchiveConnection(index - 1, ArchiveDirection.NORTH, false, false));
            }
            if (index < plans.size() - 1) {
                connections.add(new ArchiveConnection(index + 1, ArchiveDirection.SOUTH, false, false));
            }
            ArchiveRoomCategory category = index == 0
                    ? ArchiveRoomCategory.STARTING
                    : plan.encounterKind() == ArchiveEncounterKind.BOSS
                            ? ArchiveRoomCategory.FINAL_BOSS
                            : template.category();
            ArchiveRoomRuntimeState runtime = index == 0
                    ? new ArchiveRoomRuntimeState(true, false, true, false, List.of(), false)
                    : ArchiveRoomRuntimeState.UNVISITED;
            nodes.add(new ArchiveRoomNode(
                    index,
                    plan.roomId(),
                    category,
                    new ArchiveRoomPlacement(new ArchiveGridPos(0, 0, index), template.size(), ArchiveTransform.IDENTITY),
                    connections,
                    index,
                    1 + index / 2,
                    template.lootTables(),
                    template.monsterGroups(),
                    List.of(),
                    runtime));
        }
        int boss = plans.size() - 1;
        return new ArchiveDungeonGraph(1, seed, nodes, 0, boss, boss);
    }

    private ArchiveDungeonGraph withRuntime(int roomIndex, ArchiveRoomRuntimeState runtime) {
        ArrayList<ArchiveRoomNode> updated = new ArrayList<>(rooms);
        updated.set(roomIndex, room(roomIndex).withRuntime(runtime));
        return copy(updated);
    }

    private ArchiveDungeonGraph setBossEntranceLocked(boolean locked) {
        ArchiveDungeonGraph updatedGraph = this;
        for (ArchiveConnection connection : room(bossRoom).connections()) {
            if (connection.targetRoom() != rewardRoom && connection.locked() != locked) {
                updatedGraph = updatedGraph.setConnectionLocked(
                        bossRoom, connection.targetRoom(), locked);
            }
        }
        ArchiveRoomNode boss = updatedGraph.room(bossRoom);
        return updatedGraph.withRuntime(
                bossRoom, boss.runtime().withDoorsLocked(locked));
    }

    private ArchiveDungeonGraph copy(List<ArchiveRoomNode> updated) {
        return new ArchiveDungeonGraph(schemaRevision, seed, updated, startingRoom, bossRoom, rewardRoom);
    }

    private static Set<Integer> reachableIndices(List<ArchiveRoomNode> nodes, int start) {
        HashSet<Integer> visited = new HashSet<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            nodes.get(current).connections().stream()
                    .map(ArchiveConnection::targetRoom)
                    .filter(target -> !visited.contains(target))
                    .forEach(queue::addLast);
        }
        return visited;
    }

    private static int overlapCount(List<ArchiveRoomNode> nodes) {
        int overlaps = 0;
        for (int first = 0; first < nodes.size(); first++) {
            Volume a = volume(nodes.get(first));
            for (int second = first + 1; second < nodes.size(); second++) {
                if (a.intersects(volume(nodes.get(second)))) {
                    overlaps++;
                }
            }
        }
        return overlaps;
    }

    private static Volume volume(ArchiveRoomNode room) {
        ArchiveGridPos coordinates = room.placement().coordinates();
        ArchiveRoomSize size = room.placement().size();
        int width = room.placement().transform().quarterTurns() % 2 == 0 ? size.width() : size.depth();
        int depth = room.placement().transform().quarterTurns() % 2 == 0 ? size.depth() : size.width();
        int centerX = coordinates.x() * GRID_SPACING_XZ;
        int floorY = coordinates.y() * GRID_SPACING_Y;
        int centerZ = coordinates.z() * GRID_SPACING_XZ;
        return new Volume(
                centerX - width / 2,
                floorY,
                centerZ - depth / 2,
                centerX - width / 2 + width - 1,
                floorY + size.height() - 1,
                centerZ - depth / 2 + depth - 1);
    }

    private static void requireIndex(String label, int index, int size) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Archive " + label + " room index is outside the graph: " + index);
        }
    }

    private record Volume(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private boolean intersects(Volume other) {
            return minX <= other.maxX && maxX >= other.minX
                    && minY <= other.maxY && maxY >= other.minY
                    && minZ <= other.maxZ && maxZ >= other.minZ;
        }
    }
}
