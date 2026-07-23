package com.nightbeam.tbos.run;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.block.ResonantBellBlock;
import com.nightbeam.tbos.config.YesterglassConfig;
import com.nightbeam.tbos.registry.ModBlocks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/** Builds reusable transformed room templates and their horizontal/vertical connections. */
public final class ArchiveRoomPlacer {
    public static final TagKey<Block> ARCHIVE_RUN_PALETTE = TagKey.create(
            Registries.BLOCK, Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive_run_palette"));

    private static final int UPDATE_FLAGS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;

    private ArchiveRoomPlacer() {
    }

    public static Blueprint blueprint(ArchiveRun run) {
        Objects.requireNonNull(run, "run");
        ArchiveDungeonGraph graph = run.dungeonGraph();
        BoundingBox instanceBounds = ArchiveInstanceLayout.boundsForSlot(run.instanceSlot());
        Map<BlockPos, BlockState> placements = new LinkedHashMap<>();
        List<BoundingBox> clearVolumes = new ArrayList<>();

        for (ArchiveRoomNode room : graph.rooms()) {
            addRoomClearVolume(clearVolumes, run, room.index());
            placeRoom(placements, run, room);
        }
        for (ArchiveRoomNode room : graph.rooms()) {
            for (ArchiveConnection connection : room.connections()) {
                if (room.index() < connection.targetRoom()) {
                    placeConnection(placements, clearVolumes, run, room, connection);
                }
            }
        }
        for (ArchiveRoomNode room : graph.rooms()) {
            if (room.runtime().doorsLocked()) {
                placeDoorSeals(placements, run, room.index());
            }
        }

        List<Placement> immutablePlacements = placements.entrySet().stream()
                .map(entry -> new Placement(entry.getKey(), entry.getValue()))
                .toList();
        for (Placement placement : immutablePlacements) {
            if (!instanceBounds.isInside(placement.position())) {
                throw new IllegalStateException("Archive blueprint escaped its allocated bounds: " + placement.position());
            }
        }
        if (clearVolumes.stream().anyMatch(volume ->
                !instanceBounds.isInside(new BlockPos(volume.minX(), volume.minY(), volume.minZ()))
                        || !instanceBounds.isInside(new BlockPos(volume.maxX(), volume.maxY(), volume.maxZ())))) {
            throw new IllegalStateException("Archive clear volume escaped its allocated instance bounds");
        }
        BlockPos spawn = roomSpawn(run, graph.startingRoom());
        BlockPos reward = roomSpawn(run, graph.rewardRoom()).below();
        int routeXMin = graph.rooms().stream().mapToInt(room -> roomBounds(run, room.index()).minX()).min().orElse(spawn.getX());
        return new Blueprint(
                instanceBounds,
                spawn,
                spawn.below(),
                reward,
                routeXMin,
                immutablePlacements,
                List.copyOf(clearVolumes));
    }

    /** Synchronous helper for tests and explicit admin previews. Normal entry uses the tick queue. */
    public static Blueprint place(ServerLevel level, ArchiveRun run) {
        Blueprint blueprint = blueprint(run);
        BlockState air = Blocks.AIR.defaultBlockState();
        for (BoundingBox volume : blueprint.clearVolumes()) {
            for (int x = volume.minX(); x <= volume.maxX(); x++) {
                for (int z = volume.minZ(); z <= volume.maxZ(); z++) {
                    for (int y = volume.minY(); y <= volume.maxY(); y++) {
                        level.setBlock(new BlockPos(x, y, z), air, UPDATE_FLAGS);
                    }
                }
            }
        }
        for (Placement placement : blueprint.placements()) {
            level.setBlock(placement.position(), placement.state(), UPDATE_FLAGS);
        }
        return blueprint;
    }

    public static BlockPos roomSpawn(ArchiveRun run, int roomIndex) {
        ArchiveRoomNode node = run.dungeonGraph().room(roomIndex);
        ArchiveRoomTemplate template = ArchiveRoomTemplates.require(node.templateId());
        BlockPos marker = template.playerEntryMarkers().getFirst();
        return markerToWorld(run, node, marker);
    }

    public static BoundingBox roomBounds(ArchiveRun run, int roomIndex) {
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        ArchiveGridPos grid = room.placement().coordinates();
        ArchiveRoomSize size = room.placement().size();
        boolean quarterTurn = room.placement().transform().quarterTurns() % 2 != 0;
        int width = quarterTurn ? size.depth() : size.width();
        int depth = quarterTurn ? size.width() : size.depth();
        BlockPos instance = ArchiveInstanceLayout.originForSlot(run.instanceSlot());
        int centerX = instance.getX() + grid.x() * ArchiveDungeonGraph.GRID_SPACING_XZ;
        int floorY = ArchiveInstanceLayout.BASE_Y + grid.y() * ArchiveDungeonGraph.GRID_SPACING_Y;
        int centerZ = instance.getZ() + grid.z() * ArchiveDungeonGraph.GRID_SPACING_XZ;
        return new BoundingBox(
                centerX - width / 2,
                floorY,
                centerZ - depth / 2,
                centerX - width / 2 + width - 1,
                floorY + size.height() - 1,
                centerZ - depth / 2 + depth - 1);
    }

    public static AABB roomAabb(ArchiveRun run, int roomIndex) {
        BoundingBox bounds = roomBounds(run, roomIndex);
        return new AABB(
                bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX() + 1.0D, bounds.maxY() + 1.0D, bounds.maxZ() + 1.0D);
    }

    public static boolean isInsideRoom(ArchiveRun run, int roomIndex, BlockPos position) {
        return roomBounds(run, roomIndex).isInside(position);
    }

    public static OptionalInt roomContaining(ArchiveRun run, BlockPos position) {
        return run.dungeonGraph().rooms().stream()
                .filter(room -> roomBounds(run, room.index()).isInside(position))
                .mapToInt(ArchiveRoomNode::index)
                .findFirst();
    }

    public static List<BlockPos> exitGatePositions(ArchiveRun run, int roomIndex) {
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        ArchiveConnection next = room.connections().stream()
                .filter(connection -> connection.targetRoom() == roomIndex + 1)
                .findFirst()
                .orElse(room.connections().stream().findFirst().orElse(null));
        return next == null ? List.of() : doorPositions(run, roomIndex, next.direction());
    }

    public static List<BlockPos> doorPositions(ArchiveRun run, int roomIndex) {
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        return room.connections().stream()
                .flatMap(connection -> doorPositions(run, roomIndex, connection.direction()).stream())
                .distinct()
                .toList();
    }

    public static List<BlockPos> doorPositions(ArchiveRun run, int roomIndex, ArchiveDirection direction) {
        BoundingBox bounds = roomBounds(run, roomIndex);
        int centerX = (bounds.minX() + bounds.maxX() + 1) / 2;
        int centerZ = (bounds.minZ() + bounds.maxZ() + 1) / 2;
        ArrayList<BlockPos> positions = new ArrayList<>();
        if (direction.vertical()) {
            ArchiveConnection connection = run.dungeonGraph().room(roomIndex).connections().stream()
                    .filter(candidate -> candidate.direction() == direction)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Archive room " + roomIndex + " has no " + direction + " connection"));
            BoundingBox target = roomBounds(run, connection.targetRoom());
            int lowY = Math.min(bounds.minY(), target.minY());
            int barrierStep = ArchiveDungeonGraph.GRID_SPACING_Y / 2;
            int barrierX = centerX - 8 + barrierStep - 1;
            int stairY = lowY + barrierStep;
            // The vertical route is a two-wide rising phase stair. Seal the
            // actual walkable cross-section midway between floors so neither
            // direction can bypass a locked room through the stairwell.
            for (int z = centerZ - 1; z <= centerZ; z++) {
                for (int head = 1; head <= 3; head++) {
                    positions.add(new BlockPos(barrierX, stairY + head, z));
                }
            }
            return List.copyOf(positions);
        }
        for (int y = bounds.minY() + 1; y <= bounds.minY() + 3; y++) {
            switch (direction) {
                case NORTH -> {
                    positions.add(new BlockPos(centerX - 1, y, bounds.minZ()));
                    positions.add(new BlockPos(centerX, y, bounds.minZ()));
                }
                case SOUTH -> {
                    positions.add(new BlockPos(centerX - 1, y, bounds.maxZ()));
                    positions.add(new BlockPos(centerX, y, bounds.maxZ()));
                }
                case EAST -> {
                    positions.add(new BlockPos(bounds.maxX(), y, centerZ - 1));
                    positions.add(new BlockPos(bounds.maxX(), y, centerZ));
                }
                case WEST -> {
                    positions.add(new BlockPos(bounds.minX(), y, centerZ - 1));
                    positions.add(new BlockPos(bounds.minX(), y, centerZ));
                }
                default -> throw new IllegalStateException("Unexpected vertical direction");
            }
        }
        return List.copyOf(positions);
    }

    /**
     * Closes a room only after every member has fully cleared its seal blocks.
     * A player's full collision box is checked instead of only their feet so a
     * fast boundary crossing cannot be trapped by a seal on the following tick.
     */
    public static boolean lockRoomDoors(ServerLevel level, ArchiveRun run, int roomIndex) {
        List<AABB> memberBoxes = run.members().stream()
                .map(member -> level.getServer().getPlayerList().getPlayer(member.playerId()))
                .filter(Objects::nonNull)
                .filter(player -> !player.isSpectator())
                .filter(player -> player.level() == level)
                .map(ServerPlayer::getBoundingBox)
                .toList();
        if (!doorwayClear(run, roomIndex, memberBoxes)) {
            return false;
        }
        for (BlockPos position : doorPositions(run, roomIndex)) {
            level.setBlock(position, ModBlocks.ARCHIVE_SEAL.get().defaultBlockState(), UPDATE_FLAGS);
            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
                    position.getX() + 0.5D,
                    position.getY() + 0.5D,
                    position.getZ() + 0.5D,
                    3,
                    0.20D,
                    0.30D,
                    0.20D,
                    0.01D);
        }
        BlockPos soundOrigin = doorPositions(run, roomIndex).stream()
                .findFirst()
                .orElse(roomSpawn(run, roomIndex));
        level.playSound(
                null,
                soundOrigin,
                SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.BLOCKS,
                0.65F,
                1.35F);
        return true;
    }

    public static boolean doorwayClear(ArchiveRun run, int roomIndex, List<AABB> occupantBoxes) {
        Objects.requireNonNull(occupantBoxes, "occupantBoxes");
        for (BlockPos position : doorPositions(run, roomIndex)) {
            AABB safetyVolume = new AABB(position).inflate(0.60D, 0.25D, 0.60D);
            if (occupantBoxes.stream().anyMatch(safetyVolume::intersects)) {
                return false;
            }
        }
        return true;
    }

    public static boolean unlockRoomDoors(ServerLevel level, ArchiveRun run, int roomIndex) {
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        boolean openedAny = false;
        for (ArchiveConnection connection : room.connections()) {
            if (connection.locked()) {
                continue;
            }
            ArchiveRoomNode target = run.dungeonGraph().room(connection.targetRoom());
            if (connection.hidden() && !target.runtime().secretDiscovered() && !room.runtime().secretDiscovered()) {
                continue;
            }
            openedAny |= openConnection(level, run, roomIndex, connection);
        }
        if (openedAny) {
            BlockPos origin = doorPositions(run, roomIndex).stream()
                    .findFirst()
                    .orElse(roomSpawn(run, roomIndex));
            level.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.END_ROD,
                    origin.getX() + 0.5D,
                    origin.getY() + 1.0D,
                    origin.getZ() + 0.5D,
                    10,
                    0.45D,
                    0.75D,
                    0.45D,
                    0.035D);
            level.playSound(null, origin, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.8F, 0.72F);
        }
        return openedAny;
    }

    public static void unlockAllDoors(ServerLevel level, ArchiveRun run) {
        for (ArchiveRoomNode room : run.dungeonGraph().rooms()) {
            unlockRoomDoors(level, run, room.index());
        }
    }

    public static void openExit(ServerLevel level, ArchiveRun run, int roomIndex) {
        unlockRoomDoors(level, run, roomIndex);
    }

    public static List<BlockPos> choirBellPositions(ArchiveRun run, int roomIndex) {
        ArchiveRoomNode node = run.dungeonGraph().room(roomIndex);
        ArchiveRoomTemplate template = ArchiveRoomTemplates.require(node.templateId());
        return template.puzzleMarkers().stream().limit(4).map(marker -> markerToWorld(run, node, marker)).toList();
    }

    public static List<BlockPos> hallDialPositions(ArchiveRun run, int roomIndex) {
        ArchiveRoomNode node = run.dungeonGraph().room(roomIndex);
        ArchiveRoomTemplate template = ArchiveRoomTemplates.require(node.templateId());
        return template.puzzleMarkers().stream().limit(3).map(marker -> markerToWorld(run, node, marker)).toList();
    }

    public static List<BlockPos> monsterSpawnPositions(ArchiveRun run, int roomIndex) {
        ArchiveRoomNode node = run.dungeonGraph().room(roomIndex);
        ArchiveRoomTemplate template = ArchiveRoomTemplates.require(node.templateId());
        List<BlockPos> markers = node.category() == ArchiveRoomCategory.FINAL_BOSS
                ? template.bossMarkers()
                : template.monsterMarkers();
        return markers.stream().map(marker -> markerToWorld(run, node, marker)).toList();
    }

    public static List<BlockPos> chestPositions(ArchiveRun run, int roomIndex) {
        ArchiveRoomNode node = run.dungeonGraph().room(roomIndex);
        ArchiveRoomTemplate template = ArchiveRoomTemplates.require(node.templateId());
        return template.chestMarkers().stream().map(marker -> markerToWorld(run, node, marker)).toList();
    }

    public static ArchiveContainerKind containerKind(ArchiveRun run, int roomIndex, int markerIndex) {
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        if (room.category() == ArchiveRoomCategory.EXIT_REWARD) {
            return ArchiveContainerKind.BOSS_REWARD;
        }
        if (room.category() == ArchiveRoomCategory.SECRET) {
            return ArchiveContainerKind.HIDDEN;
        }
        if (room.category() == ArchiveRoomCategory.CURSED) {
            return ArchiveContainerKind.CURSED;
        }
        if (room.category() == ArchiveRoomCategory.TRAP) {
            return ArchiveContainerKind.TRAPPED;
        }
        if (room.category().combat()) {
            return ArchiveContainerKind.LOCKED;
        }
        RandomSource random = RandomSource.create(ArchiveRunGenerator.encounterSeedFor(
                run.seed(), roomIndex, 0x43414348 ^ markerIndex));
        double roll = random.nextDouble();
        if (roll < 0.10D) {
            return ArchiveContainerKind.TRAPPED;
        }
        if (roll < 0.18D) {
            return ArchiveContainerKind.CURSED;
        }
        return ArchiveContainerKind.ORDINARY;
    }

    public static List<BlockPos> lootPositions(ArchiveRun run, int roomIndex) {
        ArchiveRoomNode node = run.dungeonGraph().room(roomIndex);
        ArchiveRoomTemplate template = ArchiveRoomTemplates.require(node.templateId());
        return template.lootMarkers().stream().map(marker -> markerToWorld(run, node, marker)).toList();
    }

    public static List<BlockPos> trapPositions(ArchiveRun run, int roomIndex) {
        ArchiveRoomNode node = run.dungeonGraph().room(roomIndex);
        ArchiveRoomTemplate template = ArchiveRoomTemplates.require(node.templateId());
        return template.trapMarkers().stream().map(marker -> markerToWorld(run, node, marker)).toList();
    }

    public static List<BlockPos> secretWallPositions(ArchiveRun run, int secretRoomIndex) {
        ArchiveRoomNode secret = run.dungeonGraph().room(secretRoomIndex);
        if (secret.category() != ArchiveRoomCategory.SECRET) {
            return List.of();
        }
        return secret.connections().stream()
                .flatMap(connection -> doorPositions(run, connection.targetRoom(), connection.direction().opposite()).stream())
                .distinct()
                .toList();
    }

    public static BlockPos rewardCachePosition(ArchiveRun run) {
        List<BlockPos> positions = chestPositions(run, run.dungeonGraph().rewardRoom());
        return positions.isEmpty() ? roomSpawn(run, run.dungeonGraph().rewardRoom()) : positions.getFirst();
    }

    public static void buildHallBridge(ServerLevel level, ArchiveRun run, int roomIndex) {
        BoundingBox bounds = roomBounds(run, roomIndex);
        int centerX = (bounds.minX() + bounds.maxX() + 1) / 2;
        int centerZ = (bounds.minZ() + bounds.maxZ() + 1) / 2;
        for (int z = centerZ - 3; z <= centerZ + 3; z++) {
            for (int x = centerX - 1; x <= centerX; x++) {
                level.setBlock(new BlockPos(x, bounds.minY(), z),
                        ModBlocks.PHASE_PLATFORM.get().defaultBlockState(), UPDATE_FLAGS);
            }
        }
    }

    public static void revealSecretConnection(ServerLevel level, ArchiveRun run, int secretRoomIndex) {
        ArchiveRoomNode secret = run.dungeonGraph().room(secretRoomIndex);
        for (ArchiveConnection connection : secret.connections()) {
            openConnection(level, run, secretRoomIndex, connection);
        }
    }

    /** Places one isolated reusable template for operator inspection. */
    public static int placeTemplatePreview(ServerLevel level, ArchiveRoomTemplate template, BlockPos center) {
        int minX = center.getX() - template.size().width() / 2;
        int minZ = center.getZ() - template.size().depth() / 2;
        int floorY = center.getY();
        int maxX = minX + template.size().width() - 1;
        int maxZ = minZ + template.size().depth() - 1;
        int changed = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                level.setBlock(new BlockPos(x, floorY, z),
                        ModBlocks.ARCHIVE_STONE.get().defaultBlockState(), UPDATE_FLAGS);
                changed++;
            }
        }
        for (int y = floorY + 1; y < floorY + template.size().height(); y++) {
            for (int x = minX; x <= maxX; x++) {
                if (!previewDoor(x, y, minX, maxX, minZ, maxZ, floorY, true)) {
                    level.setBlock(new BlockPos(x, y, minZ),
                            ModBlocks.CRACKED_ARCHIVE_STONE.get().defaultBlockState(), UPDATE_FLAGS);
                    level.setBlock(new BlockPos(x, y, maxZ),
                            ModBlocks.CRACKED_ARCHIVE_STONE.get().defaultBlockState(), UPDATE_FLAGS);
                    changed += 2;
                }
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                if (!previewDoor(z, y, minZ, maxZ, minX, maxX, floorY, false)) {
                    level.setBlock(new BlockPos(minX, y, z),
                            ModBlocks.CRACKED_ARCHIVE_STONE.get().defaultBlockState(), UPDATE_FLAGS);
                    level.setBlock(new BlockPos(maxX, y, z),
                            ModBlocks.CRACKED_ARCHIVE_STONE.get().defaultBlockState(), UPDATE_FLAGS);
                    changed += 2;
                }
            }
        }
        for (BlockPos marker : template.chestMarkers()) {
            level.setBlock(new BlockPos(minX, floorY, minZ).offset(marker),
                    ModBlocks.ARCHIVE_CACHE.get().defaultBlockState(), UPDATE_FLAGS);
            changed++;
        }
        return changed;
    }

    private static boolean previewDoor(
            int coordinate,
            int y,
            int min,
            int max,
            int unusedMin,
            int unusedMax,
            int floorY,
            boolean northSouth) {
        int center = (min + max + 1) / 2;
        return (coordinate == center - 1 || coordinate == center) && y <= floorY + 3;
    }

    private static void placeRoom(Map<BlockPos, BlockState> placements, ArchiveRun run, ArchiveRoomNode room) {
        BoundingBox bounds = roomBounds(run, room.index());
        RoomPalette palette = paletteFor(room);
        BlockState floor = palette.floor();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                put(placements, new BlockPos(x, bounds.minY(), z), floor);
            }
        }

        BlockState wall = palette.wall();
        for (int y = bounds.minY() + 1; y <= bounds.maxY(); y++) {
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                putWallUnlessDoor(placements, run, room, new BlockPos(x, y, bounds.minZ()), ArchiveDirection.NORTH, wall);
                putWallUnlessDoor(placements, run, room, new BlockPos(x, y, bounds.maxZ()), ArchiveDirection.SOUTH, wall);
            }
            for (int z = bounds.minZ() + 1; z < bounds.maxZ(); z++) {
                putWallUnlessDoor(placements, run, room, new BlockPos(bounds.minX(), y, z), ArchiveDirection.WEST, wall);
                putWallUnlessDoor(placements, run, room, new BlockPos(bounds.maxX(), y, z), ArchiveDirection.EAST, wall);
            }
        }
        BlockState roof = palette.roof();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                put(placements, new BlockPos(x, bounds.maxY(), z), roof);
            }
        }

        ArchiveRoomTemplate template = ArchiveRoomTemplates.require(room.templateId());
        for (BlockPos marker : template.transformed(template.decorationMarkers(), room.placement().transform())) {
            BlockPos world = localToWorld(run, room, marker);
            put(placements, world, room.graphDepth() % 2 == 0
                    ? palette.trim()
                    : ModBlocks.LENSWORK_CRYSTAL.get().defaultBlockState());
            if (world.getY() + 2 <= bounds.maxY()) {
                put(placements, world.above(), ModBlocks.RESONANCE_LAMP.get().defaultBlockState());
            }
        }
        placeCrateProps(placements, run, room, template);

        int sigil = room.templateId().hashCode();
        for (int bit = 0; bit < 9; bit++) {
            if (((sigil >>> bit) & 1) != 0) {
                int x = Math.min(bounds.maxX() - 2, bounds.minX() + 3 + bit % 3);
                int z = Math.min(bounds.maxZ() - 2, bounds.minZ() + 4 + bit / 3);
                put(placements, new BlockPos(x, bounds.minY(), z),
                        palette.trim());
            }
        }
        if (room.category() == ArchiveRoomCategory.FINAL_BOSS) {
            int centerX = (bounds.minX() + bounds.maxX() + 1) / 2;
            int centerZ = (bounds.minZ() + bounds.maxZ() + 1) / 2;
            for (int offset = -5; offset <= 5; offset++) {
                put(placements, new BlockPos(centerX + offset, bounds.minY(), centerZ),
                        ModBlocks.CANTOR_RUNE.get().defaultBlockState());
                put(placements, new BlockPos(centerX, bounds.minY(), centerZ + offset),
                        ModBlocks.CANTOR_RUNE.get().defaultBlockState());
            }
            for (int xOffset : new int[] {-6, 6}) {
                for (int zOffset : new int[] {-6, 6}) {
                    for (int y = bounds.minY() + 1; y <= bounds.minY() + 4; y++) {
                        put(placements, new BlockPos(centerX + xOffset, y, centerZ + zOffset),
                                y == bounds.minY() + 3
                                        ? ModBlocks.CANTOR_RUNE.get().defaultBlockState()
                                        : ModBlocks.CANTOR_WALL.get().defaultBlockState());
                    }
                }
            }
        }
        placeTemplateMarkers(placements, run, room, template);
    }

    private static RoomPalette paletteFor(ArchiveRoomNode room) {
        return switch (room.category()) {
            case FINAL_BOSS -> new RoomPalette(
                    ModBlocks.CANTOR_FLOOR.get().defaultBlockState(),
                    ModBlocks.CANTOR_WALL.get().defaultBlockState(),
                    ModBlocks.CANTOR_WALL.get().defaultBlockState(),
                    ModBlocks.CANTOR_RUNE.get().defaultBlockState());
            case MINI_BOSS -> new RoomPalette(
                    ModBlocks.CHRONICLE_TILE.get().defaultBlockState(),
                    ModBlocks.CHISELED_ARCHIVE_STONE.get().defaultBlockState(),
                    ModBlocks.ARCHIVE_BRICKS.get().defaultBlockState(),
                    ModBlocks.CHRONICLE_BRONZE.get().defaultBlockState());
            case ANCIENT_LIBRARY, LORE, PUZZLE -> new RoomPalette(
                    ModBlocks.CHRONICLE_TILE.get().defaultBlockState(),
                    ModBlocks.ARCHIVE_BRICKS.get().defaultBlockState(),
                    ModBlocks.WEATHERED_ARCHIVE_BRICKS.get().defaultBlockState(),
                    ModBlocks.CHISELED_ARCHIVE_STONE.get().defaultBlockState());
            case SECRET, CURSED, TRAP -> new RoomPalette(
                    ModBlocks.WEATHERED_ARCHIVE_BRICKS.get().defaultBlockState(),
                    room.index() % 2 == 0
                            ? ModBlocks.MOSSY_ARCHIVE_STONE.get().defaultBlockState()
                            : ModBlocks.CRACKED_ARCHIVE_STONE.get().defaultBlockState(),
                    ModBlocks.WEATHERED_ARCHIVE_BRICKS.get().defaultBlockState(),
                    ModBlocks.CHISELED_ARCHIVE_STONE.get().defaultBlockState());
            default -> switch (Math.floorMod(room.index() + room.graphDepth(), 3)) {
                case 0 -> new RoomPalette(
                        ModBlocks.ARCHIVE_STONE.get().defaultBlockState(),
                        ModBlocks.ARCHIVE_BRICKS.get().defaultBlockState(),
                        ModBlocks.WEATHERED_ARCHIVE_BRICKS.get().defaultBlockState(),
                        ModBlocks.CHISELED_ARCHIVE_STONE.get().defaultBlockState());
                case 1 -> new RoomPalette(
                        ModBlocks.CHRONICLE_TILE.get().defaultBlockState(),
                        ModBlocks.WEATHERED_ARCHIVE_BRICKS.get().defaultBlockState(),
                        ModBlocks.ARCHIVE_BRICKS.get().defaultBlockState(),
                        ModBlocks.MERIDIAN_TILE.get().defaultBlockState());
                default -> new RoomPalette(
                        ModBlocks.MERIDIAN_TILE.get().defaultBlockState(),
                        ModBlocks.MOSSY_ARCHIVE_STONE.get().defaultBlockState(),
                        ModBlocks.ARCHIVE_BRICKS.get().defaultBlockState(),
                        ModBlocks.ENGRAVED_MERIDIAN_TILE.get().defaultBlockState());
            };
        };
    }

    private static void placeTemplateMarkers(
            Map<BlockPos, BlockState> placements,
            ArchiveRun run,
            ArchiveRoomNode room,
            ArchiveRoomTemplate template) {
        if (room.encounterKind() == ArchiveEncounterKind.CHOIR) {
            List<BlockPos> bells = choirBellPositions(run, room.index());
            for (int symbol = 0; symbol < bells.size(); symbol++) {
                BlockPos bell = bells.get(symbol);
                put(placements, bell.below(), ModBlocks.ENGRAVED_MERIDIAN_TILE.get().defaultBlockState());
                put(placements, bell, ModBlocks.RESONANT_BELL.get().defaultBlockState()
                        .setValue(ResonantBellBlock.SYMBOL, symbol));
            }
        } else if (room.encounterKind() == ArchiveEncounterKind.HALL) {
            for (BlockPos dial : hallDialPositions(run, room.index())) {
                put(placements, dial.below(), ModBlocks.ENGRAVED_MERIDIAN_TILE.get().defaultBlockState());
                put(placements, dial, ModBlocks.ALIGNMENT_DIAL.get().defaultBlockState());
            }
        }

        RandomSource random = RandomSource.create(ArchiveRunGenerator.encounterSeedFor(
                run.seed(), room.index(), room.templateId().hashCode()));
        double probability = chestProbability();
        List<BlockPos> chests = chestPositions(run, room.index());
        boolean mandatory = room.category() == ArchiveRoomCategory.TREASURE
                || room.category() == ArchiveRoomCategory.SECRET
                || room.category() == ArchiveRoomCategory.EXIT_REWARD;
        int placed = 0;
        for (BlockPos chest : chests) {
            if ((mandatory && placed == 0) || random.nextDouble() < probability) {
                put(placements, chest, ModBlocks.ARCHIVE_CACHE.get().defaultBlockState());
                placed++;
            }
        }
        if (room.category() == ArchiveRoomCategory.FINAL_BOSS) {
            for (BlockPos marker : template.bossMarkers()) {
                put(placements, markerToWorld(run, room, marker).below(),
                        ModBlocks.CANTOR_RUNE.get().defaultBlockState());
            }
        }
        if (room.category() == ArchiveRoomCategory.TRAP) {
            for (BlockPos marker : template.trapMarkers()) {
                put(placements, markerToWorld(run, room, marker).below(),
                        ModBlocks.ENGRAVED_MERIDIAN_TILE.get().defaultBlockState());
            }
        }
    }

    private static void placeCrateProps(
            Map<BlockPos, BlockState> placements,
            ArchiveRun run,
            ArchiveRoomNode room,
            ArchiveRoomTemplate template) {
        if (room.category() == ArchiveRoomCategory.EXIT_REWARD) {
            return;
        }
        RandomSource random = RandomSource.create(ArchiveRunGenerator.encounterSeedFor(
                run.seed(), room.index(), 0x43524154));
        double chance = crateChance(room.category());
        if (random.nextDouble() > chance) {
            return;
        }
        BoundingBox bounds = roomBounds(run, room.index());
        int width = template.size().width();
        int depth = template.size().depth();
        List<BlockPos> candidates = List.of(
                new BlockPos(3, 1, 3),
                new BlockPos(width - 4, 1, 3),
                new BlockPos(3, 1, depth - 4),
                new BlockPos(width - 4, 1, depth - 4),
                new BlockPos(width / 2 - 5, 1, 4),
                new BlockPos(width / 2 + 5, 1, depth - 5),
                new BlockPos(4, 1, depth / 2 - 5),
                new BlockPos(width - 5, 1, depth / 2 + 5));
        Set<BlockPos> reserved = java.util.stream.Stream.of(
                        template.monsterMarkers(),
                        template.chestMarkers(),
                        template.lootMarkers(),
                        template.trapMarkers(),
                        template.puzzleMarkers(),
                        template.secretWallMarkers(),
                        template.bossMarkers(),
                        template.playerEntryMarkers())
                .flatMap(List::stream)
                .map(marker -> localToWorld(run, room, room.placement().transform().apply(marker, template.size())))
                .collect(java.util.stream.Collectors.toSet());
        int target = 1 + random.nextInt(maxCrates(room.category()));
        int start = random.nextInt(candidates.size());
        int placed = 0;
        for (int index = 0; index < candidates.size() && placed < target; index++) {
            BlockPos local = candidates.get((start + index) % candidates.size());
            BlockPos world = localToWorld(run, room, room.placement().transform().apply(local, template.size()));
            if (!bounds.isInside(world) || reserved.contains(world) || doorPositions(run, room.index()).contains(world)) {
                continue;
            }
            placements.remove(world.above());
            put(placements, world, crateState(random, room, placed));
            placed++;
        }
    }

    private static double crateChance(ArchiveRoomCategory category) {
        return switch (category) {
            case STARTING, SANCTUARY -> 0.45D;
            case TREASURE, ANCIENT_LIBRARY, SECRET, MERCHANT -> 0.90D;
            case MINI_BOSS, FINAL_BOSS -> 0.55D;
            default -> 0.70D;
        };
    }

    private static int maxCrates(ArchiveRoomCategory category) {
        return switch (category) {
            case TREASURE, ANCIENT_LIBRARY, SECRET -> 4;
            case FINAL_BOSS, MINI_BOSS -> 3;
            case STARTING, SANCTUARY, VERTICAL_SHAFT -> 2;
            default -> 3;
        };
    }

    private static BlockState crateState(RandomSource random, ArchiveRoomNode room, int placed) {
        int index = Math.floorMod(random.nextInt(ModBlocks.ARCHIVE_CRATES.size())
                + room.index() + room.graphDepth() + placed, ModBlocks.ARCHIVE_CRATES.size());
        return ModBlocks.ARCHIVE_CRATES.get(index).get().defaultBlockState();
    }

    private static void placeConnection(
            Map<BlockPos, BlockState> placements,
            List<BoundingBox> clearVolumes,
            ArchiveRun run,
            ArchiveRoomNode source,
            ArchiveConnection connection) {
        ArchiveRoomNode target = run.dungeonGraph().room(connection.targetRoom());
        if (connection.direction().vertical()) {
            placeVerticalConnection(placements, clearVolumes, run, source, target);
            return;
        }
        BoundingBox first = roomBounds(run, source.index());
        BoundingBox second = roomBounds(run, target.index());
        int floorY = first.minY();
        BlockState floor = ModBlocks.PHASE_PLATFORM.get().defaultBlockState();
        BlockState wall = (source.index() + target.index()) % 2 == 0
                ? ModBlocks.ARCHIVE_BRICKS.get().defaultBlockState()
                : ModBlocks.WEATHERED_ARCHIVE_BRICKS.get().defaultBlockState();
        BlockState roof = ModBlocks.CHISELED_ARCHIVE_STONE.get().defaultBlockState();
        if (connection.direction() == ArchiveDirection.NORTH || connection.direction() == ArchiveDirection.SOUTH) {
            int centerX = (first.minX() + first.maxX() + 1) / 2;
            int minZ = Math.min(first.maxZ(), second.maxZ());
            int maxZ = Math.max(first.minZ(), second.minZ());
            for (int z = minZ; z <= maxZ; z++) {
                put(placements, new BlockPos(centerX - 1, floorY, z), floor);
                put(placements, new BlockPos(centerX, floorY, z), floor);
                for (int y = floorY + 1; y <= floorY + 4; y++) {
                    put(placements, new BlockPos(centerX - 2, y, z), wall);
                    put(placements, new BlockPos(centerX + 1, y, z), wall);
                }
                put(placements, new BlockPos(centerX - 1, floorY + 4, z), roof);
                put(placements, new BlockPos(centerX, floorY + 4, z), roof);
            }
            clearVolumes.add(new BoundingBox(centerX - 2, floorY, minZ, centerX + 1, floorY + 4, maxZ));
        } else {
            int centerZ = (first.minZ() + first.maxZ() + 1) / 2;
            int minX = Math.min(first.maxX(), second.maxX());
            int maxX = Math.max(first.minX(), second.minX());
            for (int x = minX; x <= maxX; x++) {
                put(placements, new BlockPos(x, floorY, centerZ - 1), floor);
                put(placements, new BlockPos(x, floorY, centerZ), floor);
                for (int y = floorY + 1; y <= floorY + 4; y++) {
                    put(placements, new BlockPos(x, y, centerZ - 2), wall);
                    put(placements, new BlockPos(x, y, centerZ + 1), wall);
                }
                put(placements, new BlockPos(x, floorY + 4, centerZ - 1), roof);
                put(placements, new BlockPos(x, floorY + 4, centerZ), roof);
            }
            clearVolumes.add(new BoundingBox(minX, floorY, centerZ - 2, maxX, floorY + 4, centerZ + 1));
        }
    }

    private static void placeVerticalConnection(
            Map<BlockPos, BlockState> placements,
            List<BoundingBox> clearVolumes,
            ArchiveRun run,
            ArchiveRoomNode first,
            ArchiveRoomNode second) {
        BoundingBox firstBounds = roomBounds(run, first.index());
        BoundingBox secondBounds = roomBounds(run, second.index());
        int lowY = Math.min(firstBounds.minY(), secondBounds.minY());
        int highY = Math.max(firstBounds.minY(), secondBounds.minY());
        int centerX = (firstBounds.minX() + firstBounds.maxX() + 1) / 2;
        int centerZ = (firstBounds.minZ() + firstBounds.maxZ() + 1) / 2;
        int rise = highY - lowY;
        BlockState wall = ModBlocks.ARCHIVE_BRICKS.get().defaultBlockState();
        BlockState roof = ModBlocks.CHISELED_ARCHIVE_STONE.get().defaultBlockState();
        BlockState stairState = ModBlocks.ARCHIVE_STAIRS.get().defaultBlockState()
                .setValue(StairBlock.FACING, net.minecraft.core.Direction.EAST);

        // A full floor block before and after the run makes the transition
        // walkable in both directions without jumping or breaking the shell.
        for (int width = 0; width < 2; width++) {
            BlockPos lowerLanding = new BlockPos(centerX - 9, lowY, centerZ - 1 + width);
            BlockPos upperLanding = new BlockPos(centerX + 8, highY, centerZ - 1 + width);
            put(placements, lowerLanding, ModBlocks.PHASE_PLATFORM.get().defaultBlockState());
            put(placements, upperLanding, ModBlocks.PHASE_PLATFORM.get().defaultBlockState());
            for (int head = 1; head <= 3; head++) {
                placements.remove(lowerLanding.above(head));
                placements.remove(upperLanding.above(head));
            }
            put(placements, lowerLanding.above(4), roof);
            put(placements, upperLanding.above(4), roof);
        }
        for (int step = 1; step <= rise; step++) {
            int x = centerX - 8 + Math.min(15, step - 1);
            int y = lowY + step;
            for (int width = 0; width < 2; width++) {
                BlockPos stair = new BlockPos(x, y, centerZ - 1 + width);
                // Connections are added after room shells. Remove both head
                // blocks so neither the lower ceiling nor upper floor clips
                // the continuous descending route.
                placements.remove(stair.above());
                placements.remove(stair.above(2));
                placements.remove(stair.above(3));
                put(placements, stair, stairState);
                put(placements, stair.above(4), roof);
            }
            for (int head = 1; head <= 3; head++) {
                put(placements, new BlockPos(x, y + head, centerZ - 2), wall);
                put(placements, new BlockPos(x, y + head, centerZ + 1), wall);
            }
        }
        clearVolumes.add(new BoundingBox(
                centerX - 9, lowY, centerZ - 3,
                centerX + 9, highY + 4, centerZ + 3));
    }

    private static void placeDoorSeals(Map<BlockPos, BlockState> placements, ArchiveRun run, int roomIndex) {
        for (BlockPos position : doorPositions(run, roomIndex)) {
            put(placements, position, ModBlocks.ARCHIVE_SEAL.get().defaultBlockState());
        }
    }

    private static void putWallUnlessDoor(
            Map<BlockPos, BlockState> placements,
            ArchiveRun run,
            ArchiveRoomNode room,
            BlockPos position,
            ArchiveDirection direction,
            BlockState wall) {
        ArchiveConnection connection = room.connections().stream()
                .filter(candidate -> candidate.direction() == direction)
                .findFirst()
                .orElse(null);
        if (connection == null || !doorPositions(run, room.index(), direction).contains(position)) {
            put(placements, position, wall);
            return;
        }
        ArchiveRoomNode target = run.dungeonGraph().room(connection.targetRoom());
        boolean revealed = !connection.hidden() || room.runtime().secretDiscovered() || target.runtime().secretDiscovered();
        if (!revealed) {
            put(placements, position, ModBlocks.CRACKED_ARCHIVE_STONE.get().defaultBlockState());
        } else if (connection.locked()) {
            put(placements, position, ModBlocks.ARCHIVE_SEAL.get().defaultBlockState());
        }
    }

    private static boolean openConnection(
            ServerLevel level, ArchiveRun run, int roomIndex, ArchiveConnection connection) {
        boolean opened = false;
        for (BlockPos position : doorPositions(run, roomIndex, connection.direction())) {
            opened |= !level.getBlockState(position).isAir();
            level.setBlock(position, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
        for (BlockPos position : doorPositions(
                run, connection.targetRoom(), connection.direction().opposite())) {
            opened |= !level.getBlockState(position).isAir();
            level.setBlock(position, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS);
        }
        return opened;
    }

    private static BlockPos markerToWorld(ArchiveRun run, ArchiveRoomNode room, BlockPos marker) {
        ArchiveRoomTemplate template = ArchiveRoomTemplates.require(room.templateId());
        return localToWorld(run, room, room.placement().transform().apply(marker, template.size()));
    }

    private static BlockPos localToWorld(ArchiveRun run, ArchiveRoomNode room, BlockPos transformedMarker) {
        BoundingBox bounds = roomBounds(run, room.index());
        return new BlockPos(
                bounds.minX() + transformedMarker.getX(),
                bounds.minY() + transformedMarker.getY(),
                bounds.minZ() + transformedMarker.getZ());
    }

    private static void addRoomClearVolume(List<BoundingBox> clearVolumes, ArchiveRun run, int roomIndex) {
        BoundingBox bounds = roomBounds(run, roomIndex);
        clearVolumes.add(new BoundingBox(
                bounds.minX() - 1,
                bounds.minY(),
                bounds.minZ() - 1,
                bounds.maxX() + 1,
                bounds.maxY() + 2,
                bounds.maxZ() + 1));
    }

    private static double chestProbability() {
        try {
            return YesterglassConfig.DUNGEON_CHEST_FREQUENCY.get();
        } catch (IllegalStateException exception) {
            return ArchiveDungeonSettings.DEFAULT.chestProbability();
        }
    }

    private static void put(Map<BlockPos, BlockState> placements, BlockPos position, BlockState state) {
        placements.put(position.immutable(), state);
    }

    private record RoomPalette(BlockState floor, BlockState wall, BlockState roof, BlockState trim) {
    }

    public record Placement(BlockPos position, BlockState state) {
        public Placement {
            position = Objects.requireNonNull(position, "position").immutable();
            state = Objects.requireNonNull(state, "state");
            if (state.isAir()) {
                throw new IllegalArgumentException("Archive blueprint placements must be visible blocks");
            }
        }
    }

    public record Blueprint(
            BoundingBox bounds,
            BlockPos spawn,
            BlockPos entranceFloor,
            BlockPos exitFloor,
            int routeXMin,
            List<Placement> placements,
            List<BoundingBox> clearVolumes) {
        public Blueprint {
            bounds = Objects.requireNonNull(bounds, "bounds");
            spawn = Objects.requireNonNull(spawn, "spawn").immutable();
            entranceFloor = Objects.requireNonNull(entranceFloor, "entranceFloor").immutable();
            exitFloor = Objects.requireNonNull(exitFloor, "exitFloor").immutable();
            placements = List.copyOf(Objects.requireNonNull(placements, "placements"));
            clearVolumes = List.copyOf(Objects.requireNonNull(clearVolumes, "clearVolumes"));
        }
    }
}
