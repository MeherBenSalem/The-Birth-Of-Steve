package com.nightbeam.tbos.run;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.block.GraveyardPropBlock;
import com.nightbeam.tbos.block.ResonantBellBlock;
import com.nightbeam.tbos.config.YesterglassConfig;
import com.nightbeam.tbos.platform.registry.RegistryEntry;
import com.nightbeam.tbos.registry.ModBlocks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

/** Builds reusable transformed room templates and their horizontal/vertical connections. */
public final class ArchiveRoomPlacer {
    public static final TagKey<Block> ARCHIVE_RUN_PALETTE = TagKey.create(
            Registries.BLOCK, new ResourceLocation(Yesterglass.MOD_ID, "archive_run_palette"));

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

        // A stair cut can remove the floor under a surface hazard. Drop that hazard,
        // never refill the cut: restoring its support would obstruct the route.
        placements.entrySet().removeIf(entry ->
                (entry.getValue().is(ModBlocks.PARALLAX_PANEL.get())
                    || entry.getValue().is(ModBlocks.SHATTER_PANE.get())
                    || entry.getValue().is(ModBlocks.FALSE_SHELF.get()))
                && !placements.containsKey(entry.getKey().below()));
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
        BlockPos marker = template.playerEntryMarkers().get(0);
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
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        for (ArchiveConnection connection : room.connections()) {
            BlockState seal = doorSeal(room, run.dungeonGraph().room(connection.targetRoom()));
            for (BlockPos position : doorPositions(run, roomIndex, connection.direction())) {
                level.setBlock(position, seal, UPDATE_FLAGS);
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

    public static List<BlockPos> memorySocketPositions(ArchiveRun run,int roomIndex) {
        ArchiveRoomNode node=run.dungeonGraph().room(roomIndex);
        ArchiveRoomTemplate template=ArchiveRoomTemplates.require(node.templateId());
        return template.memorySocketMarkers().stream().map(marker->markerToWorld(run,node,marker)).toList();
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

    public static BlockPos waystonePosition(ArchiveRun run) {
        int roomIndex = run.dungeonGraph().waystoneRoom();
        if (roomIndex < 0) {
            throw new IllegalArgumentException("Archive graph has no waystone room");
        }
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        ArchiveRoomSize size = room.placement().size();
        return markerToWorld(run, room, new BlockPos(size.width() / 2, 1, size.depth() / 2));
    }

    public static BlockPos waystoneSpawn(ArchiveRun run) {
        return waystonePosition(run).above();
    }

    public static BlockPos rewardCachePosition(ArchiveRun run) {
        List<BlockPos> positions = chestPositions(run, run.dungeonGraph().rewardRoom());
        return positions.isEmpty() ? roomSpawn(run, run.dungeonGraph().rewardRoom()) : positions.get(0);
    }

    public static BlockPos rewardGatewayPosition(ArchiveRun run) {
        List<BlockPos> positions = lootPositions(run, run.dungeonGraph().rewardRoom());
        return positions.isEmpty()
                ? roomSpawn(run, run.dungeonGraph().rewardRoom()).offset(0, 0, -3)
                : positions.get(0);
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
        RoomPalette palette = paletteFor(run, room);
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
        placeRoomSetDressing(placements, run, room, template, palette);
        placeGraveyardProps(placements, run, room, template);

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
        placeThemeHazards(placements, run, room, bounds, palette);
        if (room.category() == ArchiveRoomCategory.WAYSTONE) {
            put(placements, waystonePosition(run), ModBlocks.WAYSTONE.get().defaultBlockState());
        }
    }

    private static void placeThemeHazards(
            Map<BlockPos, BlockState> placements,
            ArchiveRun run,
            ArchiveRoomNode room,
            BoundingBox bounds,
            RoomPalette palette) {
        if (room.category() == ArchiveRoomCategory.FINAL_BOSS
                || room.category() == ArchiveRoomCategory.EXIT_REWARD
                || room.category() == ArchiveRoomCategory.STARTING
                || room.category() == ArchiveRoomCategory.WAYSTONE) {
            return;
        }
        ArchiveFloorTheme theme = ArchiveFloorPresentation.theme(run.floor());
        BlockState hazard = hazardBlock(theme);
        BlockState underlayer = palette.floor();
        int hash = room.index() * 31 + room.graphDepth();
        int count = 3 + Math.floorMod(hash, 3);
        for (int i = 0; i < count; i++) {
            int x = bounds.minX() + 2 + Math.floorMod(hash + i * 17, Math.max(1, bounds.maxX() - bounds.minX() - 3));
            int z = bounds.minZ() + 2 + Math.floorMod(hash + i * 29, Math.max(1, bounds.maxZ() - bounds.minZ() - 3));
            BlockPos floorPos = new BlockPos(x, bounds.minY(), z);
            switch (theme.hazard()) {
                case COLLAPSING_TILE, BRITTLE_ASH -> {
                    put(placements, floorPos.below(), underlayer);
                    put(placements, floorPos, hazard);
                }
                case SHATTER_PANE, FALSE_SHELF -> {
                    put(placements, floorPos.above(), hazard);
                }
                case PARALLAX_PANEL -> {
                    // Sits on the floor. The old alternating offset put half of
                    // them a second block up with nothing underneath, which is
                    // a panel hanging in mid-air rather than a hazard.
                    put(placements, floorPos.above(), hazard);
                }
                case LIGHT_DUST, INK_POOL, RESONANT_PLATE -> put(placements, floorPos, hazard);
            }
        }
    }

    private static BlockState hazardBlock(ArchiveFloorTheme theme) {
        return switch (theme.hazard()) {
            case PARALLAX_PANEL -> ModBlocks.PARALLAX_PANEL.get().defaultBlockState();
            case LIGHT_DUST -> ModBlocks.LIGHT_DUST.get().defaultBlockState();
            case COLLAPSING_TILE -> ModBlocks.COLLAPSING_MERIDIAN.get().defaultBlockState();
            case BRITTLE_ASH -> ModBlocks.BRITTLE_ASH.get().defaultBlockState();
            case SHATTER_PANE -> ModBlocks.SHATTER_PANE.get().defaultBlockState();
            case FALSE_SHELF -> ModBlocks.FALSE_SHELF.get().defaultBlockState();
            case RESONANT_PLATE -> ModBlocks.RESONANT_PLATE.get().defaultBlockState();
            case INK_POOL -> ModBlocks.INK_POOL.get().defaultBlockState();
        };
    }

    private static RoomPalette paletteFor(ArchiveRun run, ArchiveRoomNode room) {
        ArchiveFloorTheme theme = ArchiveFloorPresentation.theme(run.floor());
        RoomPalette themed = themeDefaultPalette(theme, room);
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
                    themed.floor(),
                    themed.wall(),
                    ModBlocks.WEATHERED_ARCHIVE_BRICKS.get().defaultBlockState(),
                    themed.trim());
            case SECRET, CURSED, TRAP -> new RoomPalette(
                    themed.floor(),
                    room.index() % 2 == 0
                            ? ModBlocks.MOSSY_ARCHIVE_STONE.get().defaultBlockState()
                            : ModBlocks.CRACKED_ARCHIVE_STONE.get().defaultBlockState(),
                    themed.roof(),
                    themed.trim());
            default -> themed;
        };
    }

    private static RoomPalette themeDefaultPalette(ArchiveFloorTheme theme, ArchiveRoomNode room) {
        return switch (theme) {
            case PARALLAX_WAKE -> new RoomPalette(
                    ModBlocks.WAKE_FLOOR.get().defaultBlockState(),
                    ModBlocks.WAKE_WALL.get().defaultBlockState(),
                    ModBlocks.WAKE_ROOF.get().defaultBlockState(),
                    ModBlocks.WAKE_TRIM.get().defaultBlockState());
            case STARLESS_GALLERY -> new RoomPalette(
                    ModBlocks.GALLERY_FLOOR.get().defaultBlockState(),
                    ModBlocks.GALLERY_WALL.get().defaultBlockState(),
                    ModBlocks.GALLERY_ROOF.get().defaultBlockState(),
                    ModBlocks.GALLERY_TRIM.get().defaultBlockState());
            case MERIDIAN_DESCENT -> new RoomPalette(
                    ModBlocks.DESCENT_FLOOR.get().defaultBlockState(),
                    ModBlocks.DESCENT_WALL.get().defaultBlockState(),
                    ModBlocks.DESCENT_ROOF.get().defaultBlockState(),
                    ModBlocks.DESCENT_TRIM.get().defaultBlockState());
            case CHOIR_OF_DUST -> new RoomPalette(
                    ModBlocks.CHOIR_FLOOR.get().defaultBlockState(),
                    ModBlocks.CHOIR_WALL.get().defaultBlockState(),
                    ModBlocks.CHOIR_ROOF.get().defaultBlockState(),
                    ModBlocks.CHOIR_TRIM.get().defaultBlockState());
            case GLASSBOUND_VAULT -> new RoomPalette(
                    ModBlocks.VAULT_FLOOR.get().defaultBlockState(),
                    ModBlocks.VAULT_WALL.get().defaultBlockState(),
                    ModBlocks.VAULT_ROOF.get().defaultBlockState(),
                    ModBlocks.VAULT_TRIM.get().defaultBlockState());
            case HOLLOW_CATALOGUE -> new RoomPalette(
                    ModBlocks.CATALOGUE_FLOOR.get().defaultBlockState(),
                    ModBlocks.CATALOGUE_WALL.get().defaultBlockState(),
                    ModBlocks.CATALOGUE_ROOF.get().defaultBlockState(),
                    ModBlocks.CATALOGUE_TRIM.get().defaultBlockState());
            case CANTORS_LABYRINTH -> new RoomPalette(
                    ModBlocks.LABYRINTH_FLOOR.get().defaultBlockState(),
                    ModBlocks.LABYRINTH_WALL.get().defaultBlockState(),
                    ModBlocks.LABYRINTH_ROOF.get().defaultBlockState(),
                    ModBlocks.LABYRINTH_TRIM.get().defaultBlockState());
            case UNWRITTEN_HOUR -> new RoomPalette(
                    ModBlocks.UNWRITTEN_FLOOR.get().defaultBlockState(),
                    ModBlocks.UNWRITTEN_WALL.get().defaultBlockState(),
                    ModBlocks.UNWRITTEN_ROOF.get().defaultBlockState(),
                    ModBlocks.UNWRITTEN_TRIM.get().defaultBlockState());
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
        if (room.category() == ArchiveRoomCategory.EXIT_REWARD) {
            put(placements, rewardGatewayPosition(run), ModBlocks.RIFT_THRESHOLD.get().defaultBlockState());
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
                new BlockPos(width - 5, 1, depth / 2 + 5),
                new BlockPos(6, 1, 3),
                new BlockPos(width - 7, 1, 3),
                new BlockPos(6, 1, depth - 4),
                new BlockPos(width - 7, 1, depth - 4),
                new BlockPos(3, 1, 6),
                new BlockPos(3, 1, depth - 7),
                new BlockPos(width - 4, 1, 6),
                new BlockPos(width - 4, 1, depth - 7));
        Set<BlockPos> reserved = reservedPositions(run, room, template);
        List<BlockPos> doors = doorPositions(run, room.index());
        int target = minimumCrates(room.category())
                + random.nextInt(maxCrates(room.category()) - minimumCrates(room.category()) + 1);
        int start = random.nextInt(candidates.size());
        int placed = 0;
        for (int index = 0; index < candidates.size() && placed < target; index++) {
            BlockPos local = candidates.get((start + index) % candidates.size());
            BlockPos world = localToWorld(run, room, room.placement().transform().apply(local, template.size()));
            if (!bounds.isInside(world)
                    || isNearAny(world, reserved, 2)
                    || isNearAny(world, doors, 3)
                    || placements.containsKey(world)) {
                continue;
            }
            placements.remove(world.above());
            put(placements, world, crateState(random, room, placed));
            placed++;
        }
    }

    private static void placeGraveyardProps(
            Map<BlockPos, BlockState> placements,
            ArchiveRun run,
            ArchiveRoomNode room,
            ArchiveRoomTemplate template) {
        if (room.category() != ArchiveRoomCategory.CURSED
                && room.category() != ArchiveRoomCategory.TRAP
                && room.category() != ArchiveRoomCategory.SECRET) {
            return;
        }
        RandomSource random = RandomSource.create(ArchiveRunGenerator.encounterSeedFor(
                run.seed(), room.index(), 0x47524156));
        BoundingBox bounds = roomBounds(run, room.index());
        int width = template.size().width();
        int depth = template.size().depth();
        List<BlockPos> candidates = List.of(
                new BlockPos(5, 1, 5),
                new BlockPos(width - 6, 1, 5),
                new BlockPos(5, 1, depth - 6),
                new BlockPos(width - 6, 1, depth - 6),
                new BlockPos(width / 2 - 4, 1, 5),
                new BlockPos(width / 2 + 4, 1, depth - 6),
                new BlockPos(5, 1, depth / 2 - 4),
                new BlockPos(width - 6, 1, depth / 2 + 4),
                new BlockPos(8, 1, 4),
                new BlockPos(width - 9, 1, 4),
                new BlockPos(8, 1, depth - 5),
                new BlockPos(width - 9, 1, depth - 5),
                new BlockPos(4, 1, 8),
                new BlockPos(4, 1, depth - 9),
                new BlockPos(width - 5, 1, 8),
                new BlockPos(width - 5, 1, depth - 9));
        Set<BlockPos> reserved = reservedPositions(run, room, template);
        List<BlockPos> doors = doorPositions(run, room.index());
        BlockPos roomCenter = new BlockPos(
                (bounds.minX() + bounds.maxX()) / 2,
                bounds.minY() + 1,
                (bounds.minZ() + bounds.maxZ()) / 2);
        int target = 4 + random.nextInt(4);
        int start = random.nextInt(candidates.size());
        int placed = 0;
        for (int index = 0; index < candidates.size() && placed < target; index++) {
            BlockPos local = candidates.get((start + index) % candidates.size());
            BlockPos world = localToWorld(run, room, room.placement().transform().apply(local, template.size()));
            if (!bounds.isInside(world)
                    || isNearAny(world, reserved, 2)
                    || isNearAny(world, doors, 3)
                    || placements.containsKey(world)) {
                continue;
            }
            placements.remove(world.above());
            Direction facing = random.nextFloat() < 0.7F
                    ? facingToward(world, roomCenter)
                    : Direction.Plane.HORIZONTAL.getRandomDirection(random);
            put(placements, world, graveyardPropState(random, facing));
            placed++;
        }
    }

    private static Direction facingToward(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        if (dz != 0) {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        return dx >= 0 ? Direction.EAST : Direction.WEST;
    }

    private static BlockState graveyardPropState(RandomSource random, Direction facing) {
        RegistryEntry<GraveyardPropBlock> chosen = pickGraveyardProp(random);
        return chosen.get().defaultBlockState().setValue(GraveyardPropBlock.FACING, facing);
    }

    private static RegistryEntry<GraveyardPropBlock> pickGraveyardProp(RandomSource random) {
        float roll = random.nextFloat();
        List<RegistryEntry<GraveyardPropBlock>> pool;
        if (roll < 0.42F) {
            pool = ModBlocks.GRAVEYARD_PROPS.stream()
                    .filter(prop -> prop.id().getPath().startsWith("gravestone_type_"))
                    .toList();
        } else if (roll < 0.58F) {
            pool = ModBlocks.GRAVEYARD_PROPS.stream()
                    .filter(prop -> prop.id().getPath().contains("grave_cross"))
                    .toList();
        } else if (roll < 0.78F) {
            pool = ModBlocks.GRAVEYARD_PROPS.stream()
                    .filter(prop -> prop.id().getPath().contains("flower"))
                    .toList();
        } else {
            pool = ModBlocks.GRAVEYARD_PROPS;
        }
        if (pool.isEmpty()) {
            pool = ModBlocks.GRAVEYARD_PROPS;
        }
        return pool.get(random.nextInt(pool.size()));
    }

    private static int minimumCrates(ArchiveRoomCategory category) {
        return switch (category) {
            case TREASURE, ANCIENT_LIBRARY, SECRET, MERCHANT -> 6;
            case MINI_BOSS, FINAL_BOSS -> 4;
            case STARTING, SANCTUARY, WAYSTONE, VERTICAL_SHAFT -> 3;
            default -> 5;
        };
    }

    private static int maxCrates(ArchiveRoomCategory category) {
        return switch (category) {
            case TREASURE, ANCIENT_LIBRARY, SECRET, MERCHANT -> 9;
            case FINAL_BOSS, MINI_BOSS -> 7;
            case STARTING, SANCTUARY, WAYSTONE, VERTICAL_SHAFT -> 5;
            default -> 8;
        };
    }

    private static BlockState crateState(RandomSource random, ArchiveRoomNode room, int placed) {
        if (placed % 3 == 0 || random.nextFloat() < 0.28F) {
            return random.nextBoolean()
                    ? ModBlocks.ARCHIVE_BARREL.get().defaultBlockState()
                    : ModBlocks.ARCHIVE_BARREL_STACK.get().defaultBlockState();
        }
        int index = Math.floorMod(random.nextInt(ModBlocks.ARCHIVE_CRATES.size())
                + room.index() + room.graphDepth() + placed, ModBlocks.ARCHIVE_CRATES.size());
        return ModBlocks.ARCHIVE_CRATES.get(index).get().defaultBlockState();
    }

    private static void placeRoomSetDressing(
            Map<BlockPos, BlockState> placements,
            ArchiveRun run,
            ArchiveRoomNode room,
            ArchiveRoomTemplate template,
            RoomPalette palette) {
        BoundingBox bounds = roomBounds(run, room.index());
        RandomSource random = RandomSource.create(ArchiveRunGenerator.encounterSeedFor(
                run.seed(), room.index(), 0x4445434F));
        Set<BlockPos> reserved = reservedPositions(run, room, template);
        List<BlockPos> doors = doorPositions(run, room.index());

        weatherRoomShell(placements, bounds, reserved, doors, random, room);
        placeWeatheredFloorPatches(placements, run, room, bounds, reserved, doors);
        placeWallRibs(placements, bounds, palette, reserved, doors, room);
        placeBarredAlcoves(placements, bounds, reserved, doors, random, room);
        placeHangingLights(placements, bounds, reserved, doors, random, room);
        placeAncientRuinClusters(placements, bounds, reserved, doors, random, room);
        placeCandleShrines(placements, bounds, reserved, doors, random, room);
        placeFloorDetails(placements, bounds, reserved, doors, random, room);
        placeCobwebGrowth(placements, bounds, reserved, doors, random, room);
    }

    private static void weatherRoomShell(
            Map<BlockPos, BlockState> placements,
            BoundingBox bounds,
            Set<BlockPos> reserved,
            List<BlockPos> doors,
            RandomSource random,
            ArchiveRoomNode room) {
        int floorPatches = room.placement().size().width() >= 30 ? 28 : 18;
        for (int index = 0; index < floorPatches; index++) {
            int x = bounds.minX() + 1 + random.nextInt(bounds.getXSpan() - 2);
            int z = bounds.minZ() + 1 + random.nextInt(bounds.getZSpan() - 2);
            BlockPos floor = new BlockPos(x, bounds.minY(), z);
            if (!isNearAny(floor, reserved, 1) && !isNearAny(floor, doors, 3)) {
                put(placements, floor, weatheredRuinState(random, index));
            }
        }

        int wallPatches = room.placement().size().width() >= 30 ? 32 : 22;
        for (int index = 0; index < wallPatches; index++) {
            int y = bounds.minY() + 1 + random.nextInt(Math.max(1, bounds.getYSpan() - 2));
            int side = random.nextInt(4);
            BlockPos wall = switch (side) {
                case 0 -> new BlockPos(
                        bounds.minX() + 2 + random.nextInt(bounds.getXSpan() - 4), y, bounds.minZ());
                case 1 -> new BlockPos(
                        bounds.minX() + 2 + random.nextInt(bounds.getXSpan() - 4), y, bounds.maxZ());
                case 2 -> new BlockPos(
                        bounds.minX(), y, bounds.minZ() + 2 + random.nextInt(bounds.getZSpan() - 4));
                default -> new BlockPos(
                        bounds.maxX(), y, bounds.minZ() + 2 + random.nextInt(bounds.getZSpan() - 4));
            };
            if (!isNearAny(wall, reserved, 1) && !isNearAny(wall, doors, 3)) {
                put(placements, wall, weatheredRuinState(random, index + 17));
            }
        }

        int roofPatches = room.placement().size().width() >= 30 ? 18 : 12;
        for (int index = 0; index < roofPatches; index++) {
            int x = bounds.minX() + 1 + random.nextInt(bounds.getXSpan() - 2);
            int z = bounds.minZ() + 1 + random.nextInt(bounds.getZSpan() - 2);
            put(placements, new BlockPos(x, bounds.maxY(), z), weatheredRuinState(random, index + 31));
        }
    }

    private static BlockState weatheredRuinState(RandomSource random, int ordinal) {
        List<BlockState> states = List.of(
                ModBlocks.CRACKED_ARCHIVE_STONE.get().defaultBlockState(),
                ModBlocks.MOSSY_ARCHIVE_STONE.get().defaultBlockState(),
                ModBlocks.WEATHERED_ARCHIVE_BRICKS.get().defaultBlockState(),
                Blocks.CRACKED_STONE_BRICKS.defaultBlockState(),
                Blocks.MOSSY_STONE_BRICKS.defaultBlockState(),
                Blocks.DEEPSLATE_TILES.defaultBlockState(),
                Blocks.CHISELED_DEEPSLATE.defaultBlockState());
        return states.get(Math.floorMod(random.nextInt(states.size()) + ordinal, states.size()));
    }

    private static void placeWeatheredFloorPatches(
            Map<BlockPos, BlockState> placements,
            ArchiveRun run,
            ArchiveRoomNode room,
            BoundingBox bounds,
            Set<BlockPos> reserved,
            List<BlockPos> doors) {
        RandomSource random = RandomSource.create(ArchiveRunGenerator.encounterSeedFor(
                run.seed(), room.index(), 0x464C4F52));
        ArrayList<BlockPos> candidates = new ArrayList<>();
        for (int x = bounds.minX() + 1; x < bounds.maxX(); x++) {
            for (int z = bounds.minZ() + 1; z < bounds.maxZ(); z++) {
                BlockPos floor = new BlockPos(x, bounds.minY(), z);
                if (!isNearAny(floor, reserved, 2) && !isNearAny(floor, doors, 4)) {
                    candidates.add(floor);
                }
            }
        }
        for (int index = candidates.size() - 1; index > 0; index--) {
            int swapIndex = random.nextInt(index + 1);
            BlockPos value = candidates.get(index);
            candidates.set(index, candidates.get(swapIndex));
            candidates.set(swapIndex, value);
        }

        int target = Math.min(
                candidates.size(),
                Math.max(1, Math.round(bounds.getXSpan() * bounds.getZSpan() * 0.10F)));
        for (int index = 0; index < target; index++) {
            BlockState patch = switch (Math.floorMod(random.nextInt(4) + index + room.index(), 4)) {
                case 0 -> ModBlocks.CRACKED_ARCHIVE_STONE.get().defaultBlockState();
                case 1 -> ModBlocks.WEATHERED_ARCHIVE_BRICKS.get().defaultBlockState();
                case 2 -> Blocks.DEEPSLATE_TILES.defaultBlockState();
                default -> Blocks.CHISELED_DEEPSLATE.defaultBlockState();
            };
            put(placements, candidates.get(index), patch);
        }
    }

    private static void placeWallRibs(
            Map<BlockPos, BlockState> placements,
            BoundingBox bounds,
            RoomPalette palette,
            Set<BlockPos> reserved,
            List<BlockPos> doors,
            ArchiveRoomNode room) {
        int floorY = bounds.minY();
        int ribHeight = Math.min(bounds.maxY() - 1, floorY + 5);
        int phase = Math.floorMod(room.index() + room.graphDepth(), 3);
        for (int x = bounds.minX() + 4 + phase; x <= bounds.maxX() - 4; x += 6) {
            placeWallRib(placements, new BlockPos(x, floorY + 1, bounds.minZ()), ribHeight,
                    palette, reserved, doors, room);
            placeWallRib(placements, new BlockPos(x, floorY + 1, bounds.maxZ()), ribHeight,
                    palette, reserved, doors, room);
        }
        for (int z = bounds.minZ() + 4 + phase; z <= bounds.maxZ() - 4; z += 6) {
            placeWallRib(placements, new BlockPos(bounds.minX(), floorY + 1, z), ribHeight,
                    palette, reserved, doors, room);
            placeWallRib(placements, new BlockPos(bounds.maxX(), floorY + 1, z), ribHeight,
                    palette, reserved, doors, room);
        }
    }

    private static void placeWallRib(
            Map<BlockPos, BlockState> placements,
            BlockPos base,
            int topY,
            RoomPalette palette,
            Set<BlockPos> reserved,
            List<BlockPos> doors,
            ArchiveRoomNode room) {
        if (isNearAny(base, reserved, 1) || isNearAny(base, doors, 3)) {
            return;
        }
        for (int y = base.getY(); y <= topY; y++) {
            BlockState state = y == base.getY() + 2 && (room.index() + base.getX() + base.getZ()) % 3 == 0
                    ? ModBlocks.LENSWORK_CRYSTAL.get().defaultBlockState()
                    : palette.trim();
            put(placements, new BlockPos(base.getX(), y, base.getZ()), state);
        }
    }

    private static void placeBarredAlcoves(
            Map<BlockPos, BlockState> placements,
            BoundingBox bounds,
            Set<BlockPos> reserved,
            List<BlockPos> doors,
            RandomSource random,
            ArchiveRoomNode room) {
        int y = bounds.minY() + 1;
        int centerX = (bounds.minX() + bounds.maxX()) / 2;
        int centerZ = (bounds.minZ() + bounds.maxZ()) / 2;
        List<BarredAlcove> candidates = List.of(
                new BarredAlcove(new BlockPos(centerX - 1, y, bounds.minZ() + 2), 1, 0, 0, -1),
                new BarredAlcove(new BlockPos(centerX - 1, y, bounds.maxZ() - 2), 1, 0, 0, 1),
                new BarredAlcove(new BlockPos(bounds.minX() + 2, y, centerZ - 1), 0, 1, -1, 0),
                new BarredAlcove(new BlockPos(bounds.maxX() - 2, y, centerZ - 1), 0, 1, 1, 0),
                new BarredAlcove(new BlockPos(bounds.minX() + 6, y, bounds.minZ() + 2), 1, 0, 0, -1),
                new BarredAlcove(new BlockPos(bounds.maxX() - 8, y, bounds.maxZ() - 2), 1, 0, 0, 1));
        int target = room.placement().size().width() >= 30 ? 3 : 2;
        int start = random.nextInt(candidates.size());
        int placed = 0;
        for (int offset = 0; offset < candidates.size() && placed < target; offset++) {
            BarredAlcove alcove = candidates.get((start + offset) % candidates.size());
            boolean blocked = false;
            for (int width = 0; width < 3 && !blocked; width++) {
                for (int height = 0; height < 3; height++) {
                    BlockPos bar = alcove.base()
                            .offset(alcove.stepX() * width, height, alcove.stepZ() * width);
                    if (isNearAny(bar, reserved, 2)
                            || isNearAny(bar, doors, 3)
                            || placements.containsKey(bar)) {
                        blocked = true;
                        break;
                    }
                }
            }
            if (blocked) {
                continue;
            }
            for (int width = 0; width < 3; width++) {
                for (int height = 0; height < 3; height++) {
                    put(placements, alcove.base().offset(
                            alcove.stepX() * width,
                            height,
                            alcove.stepZ() * width), Blocks.IRON_BARS.defaultBlockState());
                }
            }
            BlockPos relic = alcove.base()
                    .offset(alcove.stepX(), 0, alcove.stepZ())
                    .offset(alcove.recessX(), 0, alcove.recessZ());
            if (!placements.containsKey(relic)) {
                put(placements, relic, candleState(random));
            }
            BlockPos backing = relic.offset(alcove.recessX(), 1, alcove.recessZ());
            put(placements, backing, random.nextBoolean()
                    ? ModBlocks.ENGRAVED_MERIDIAN_TILE.get().defaultBlockState()
                    : ModBlocks.CHISELED_ARCHIVE_STONE.get().defaultBlockState());
            placed++;
        }
    }

    private static void placeHangingLights(
            Map<BlockPos, BlockState> placements,
            BoundingBox bounds,
            Set<BlockPos> reserved,
            List<BlockPos> doors,
            RandomSource random,
            ArchiveRoomNode room) {
        int y = bounds.maxY() - 1;
        List<BlockPos> candidates = List.of(
                new BlockPos(bounds.minX() + 5, y, bounds.minZ() + 5),
                new BlockPos(bounds.maxX() - 5, y, bounds.minZ() + 5),
                new BlockPos(bounds.minX() + 5, y, bounds.maxZ() - 5),
                new BlockPos(bounds.maxX() - 5, y, bounds.maxZ() - 5),
                new BlockPos((bounds.minX() + bounds.maxX()) / 2 - 5, y, bounds.minZ() + 6),
                new BlockPos((bounds.minX() + bounds.maxX()) / 2 + 5, y, bounds.maxZ() - 6),
                new BlockPos(bounds.minX() + 6, y, (bounds.minZ() + bounds.maxZ()) / 2 + 5),
                new BlockPos(bounds.maxX() - 6, y, (bounds.minZ() + bounds.maxZ()) / 2 - 5));
        int target = room.placement().size().width() >= 30 ? 8 : 6;
        int start = random.nextInt(candidates.size());
        int placed = 0;
        for (int offset = 0; offset < candidates.size() && placed < target; offset++) {
            BlockPos anchor = candidates.get((start + offset) % candidates.size());
            if (isNearAny(anchor, reserved, 2) || isNearAny(anchor, doors, 3)) {
                continue;
            }
            int chainLength = 2 + random.nextInt(3);
            int lanternY = anchor.getY() - chainLength;
            for (int chainY = anchor.getY(); chainY > lanternY; chainY--) {
                put(placements, new BlockPos(anchor.getX(), chainY, anchor.getZ()),
                        Blocks.CHAIN.defaultBlockState());
            }
            boolean snappedChain = room.category() != ArchiveRoomCategory.SANCTUARY
                    && random.nextFloat() < 0.28F;
            if (snappedChain) {
                placed++;
                continue;
            }
            BlockState lantern = (room.category() == ArchiveRoomCategory.CURSED
                            || room.category() == ArchiveRoomCategory.SECRET
                            || room.category() == ArchiveRoomCategory.FINAL_BOSS)
                    ? Blocks.SOUL_LANTERN.defaultBlockState()
                    : Blocks.LANTERN.defaultBlockState();
            put(placements, new BlockPos(anchor.getX(), lanternY, anchor.getZ()),
                    lantern.setValue(BlockStateProperties.HANGING, true));
            placed++;
        }
    }

    private static void placeAncientRuinClusters(
            Map<BlockPos, BlockState> placements,
            BoundingBox bounds,
            Set<BlockPos> reserved,
            List<BlockPos> doors,
            RandomSource random,
            ArchiveRoomNode room) {
        int y = bounds.minY() + 1;
        List<BlockPos> candidates = List.of(
                new BlockPos(bounds.minX() + 4, y, bounds.minZ() + 7),
                new BlockPos(bounds.minX() + 7, y, bounds.minZ() + 4),
                new BlockPos(bounds.maxX() - 4, y, bounds.minZ() + 7),
                new BlockPos(bounds.maxX() - 7, y, bounds.minZ() + 4),
                new BlockPos(bounds.minX() + 4, y, bounds.maxZ() - 7),
                new BlockPos(bounds.minX() + 7, y, bounds.maxZ() - 4),
                new BlockPos(bounds.maxX() - 4, y, bounds.maxZ() - 7),
                new BlockPos(bounds.maxX() - 7, y, bounds.maxZ() - 4),
                new BlockPos(bounds.minX() + 3, y, (bounds.minZ() + bounds.maxZ()) / 2 - 6),
                new BlockPos(bounds.maxX() - 3, y, (bounds.minZ() + bounds.maxZ()) / 2 + 6));
        int target = room.placement().size().width() >= 30 ? 6 : 4;
        int start = random.nextInt(candidates.size());
        int placed = 0;
        for (int offset = 0; offset < candidates.size() && placed < target; offset++) {
            BlockPos base = candidates.get((start + offset) % candidates.size());
            if (!safeFloorDecoration(base, placements, reserved, doors, 2)) {
                continue;
            }
            int height = 1 + random.nextInt(3);
            put(placements, base, random.nextBoolean()
                    ? Blocks.DEEPSLATE_TILES.defaultBlockState()
                    : ModBlocks.CHISELED_ARCHIVE_STONE.get().defaultBlockState());
            for (int pillarY = 1; pillarY < height; pillarY++) {
                put(placements, base.above(pillarY), pillarY == height - 1
                        ? Blocks.CHISELED_DEEPSLATE.defaultBlockState()
                        : Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
            }
            if (base.getY() + height < bounds.maxY()) {
                put(placements, base.above(height), Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
            }

            int rubbleX = random.nextBoolean() ? 1 : -1;
            int rubbleZ = random.nextBoolean() ? 1 : -1;
            BlockPos rubble = base.offset(rubbleX, 0, rubbleZ);
            if (safeFloorDecoration(rubble, placements, reserved, doors, 1)) {
                put(placements, rubble, random.nextBoolean()
                        ? Blocks.DEEPSLATE_TILE_WALL.defaultBlockState()
                        : Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
            }
            placed++;
        }
    }

    private static void placeCandleShrines(
            Map<BlockPos, BlockState> placements,
            BoundingBox bounds,
            Set<BlockPos> reserved,
            List<BlockPos> doors,
            RandomSource random,
            ArchiveRoomNode room) {
        int y = bounds.minY() + 1;
        int centerX = (bounds.minX() + bounds.maxX()) / 2;
        int centerZ = (bounds.minZ() + bounds.maxZ()) / 2;
        List<BlockPos> candidates = List.of(
                new BlockPos(centerX - 6, y, bounds.minZ() + 3),
                new BlockPos(centerX + 6, y, bounds.maxZ() - 3),
                new BlockPos(bounds.minX() + 3, y, centerZ + 6),
                new BlockPos(bounds.maxX() - 3, y, centerZ - 6),
                new BlockPos(bounds.minX() + 6, y, bounds.minZ() + 6),
                new BlockPos(bounds.maxX() - 6, y, bounds.maxZ() - 6));
        int target = room.placement().size().width() >= 30 ? 5 : 3;
        int start = random.nextInt(candidates.size());
        int placed = 0;
        for (int offset = 0; offset < candidates.size() && placed < target; offset++) {
            BlockPos pedestal = candidates.get((start + offset) % candidates.size());
            if (!safeFloorDecoration(pedestal, placements, reserved, doors, 2)
                    || placements.containsKey(pedestal.above())) {
                continue;
            }
            put(placements, pedestal, placed % 3 == 0
                    ? ModBlocks.CHRONICLE_BRONZE.get().defaultBlockState()
                    : Blocks.CHISELED_DEEPSLATE.defaultBlockState());
            put(placements, pedestal.above(), candleState(random));
            placed++;
        }
    }

    private static void placeFloorDetails(
            Map<BlockPos, BlockState> placements,
            BoundingBox bounds,
            Set<BlockPos> reserved,
            List<BlockPos> doors,
            RandomSource random,
            ArchiveRoomNode room) {
        int y = bounds.minY() + 1;
        int centerX = (bounds.minX() + bounds.maxX()) / 2;
        int centerZ = (bounds.minZ() + bounds.maxZ()) / 2;
        List<BlockPos> candidates = List.of(
                new BlockPos(bounds.minX() + 2, y, bounds.minZ() + 5),
                new BlockPos(bounds.minX() + 2, y, centerZ - 4),
                new BlockPos(bounds.minX() + 2, y, centerZ + 4),
                new BlockPos(bounds.minX() + 2, y, bounds.maxZ() - 5),
                new BlockPos(bounds.maxX() - 2, y, bounds.minZ() + 5),
                new BlockPos(bounds.maxX() - 2, y, centerZ - 4),
                new BlockPos(bounds.maxX() - 2, y, centerZ + 4),
                new BlockPos(bounds.maxX() - 2, y, bounds.maxZ() - 5),
                new BlockPos(bounds.minX() + 5, y, bounds.minZ() + 2),
                new BlockPos(centerX - 4, y, bounds.minZ() + 2),
                new BlockPos(centerX + 4, y, bounds.minZ() + 2),
                new BlockPos(bounds.maxX() - 5, y, bounds.minZ() + 2),
                new BlockPos(bounds.minX() + 5, y, bounds.maxZ() - 2),
                new BlockPos(centerX - 4, y, bounds.maxZ() - 2),
                new BlockPos(centerX + 4, y, bounds.maxZ() - 2),
                new BlockPos(bounds.maxX() - 5, y, bounds.maxZ() - 2));
        int target = room.placement().size().width() >= 30 ? 14 : 10;
        int start = random.nextInt(candidates.size());
        int placed = 0;
        for (int offset = 0; offset < candidates.size() && placed < target; offset++) {
            BlockPos position = candidates.get((start + offset) % candidates.size());
            if (isNearAny(position, reserved, 2)
                    || isNearAny(position, doors, 3)
                    || placements.containsKey(position)
                    || placements.containsKey(position.above())) {
                continue;
            }
            BlockState detail = floorDetail(room.category(), random, placed);
            put(placements, position, detail);
            if ((detail.is(Blocks.BOOKSHELF) || detail.is(Blocks.CHISELED_BOOKSHELF))
                    && position.getY() + 1 < bounds.maxY()
                    && !placements.containsKey(position.above())) {
                put(placements, position.above(), random.nextBoolean()
                        ? Blocks.BOOKSHELF.defaultBlockState()
                        : Blocks.CHISELED_BOOKSHELF.defaultBlockState());
            }
            placed++;
        }
    }

    private static void placeCobwebGrowth(
            Map<BlockPos, BlockState> placements,
            BoundingBox bounds,
            Set<BlockPos> reserved,
            List<BlockPos> doors,
            RandomSource random,
            ArchiveRoomNode room) {
        int high = bounds.maxY() - 2;
        int low = bounds.minY() + 2;
        List<BlockPos> candidates = List.of(
                new BlockPos(bounds.minX() + 1, high, bounds.minZ() + 1),
                new BlockPos(bounds.maxX() - 1, high, bounds.minZ() + 1),
                new BlockPos(bounds.minX() + 1, high, bounds.maxZ() - 1),
                new BlockPos(bounds.maxX() - 1, high, bounds.maxZ() - 1),
                // Hard against the side wall. Two blocks in from every surface
                // leaves the web strung between nothing at all, which reads as a
                // block hanging in the air rather than as cobweb.
                new BlockPos(bounds.minX() + 1, low, bounds.minZ() + 2),
                new BlockPos(bounds.maxX() - 1, low, bounds.minZ() + 2),
                new BlockPos(bounds.minX() + 1, low, bounds.maxZ() - 2),
                new BlockPos(bounds.maxX() - 1, low, bounds.maxZ() - 2),
                new BlockPos(bounds.minX() + 1, high - 2, bounds.minZ() + 7),
                new BlockPos(bounds.maxX() - 1, high - 2, bounds.minZ() + 7),
                new BlockPos(bounds.minX() + 1, high - 2, bounds.maxZ() - 7),
                new BlockPos(bounds.maxX() - 1, high - 2, bounds.maxZ() - 7),
                new BlockPos(bounds.minX() + 7, high, bounds.minZ() + 1),
                new BlockPos(bounds.maxX() - 7, high, bounds.minZ() + 1),
                new BlockPos(bounds.minX() + 7, high, bounds.maxZ() - 1),
                new BlockPos(bounds.maxX() - 7, high, bounds.maxZ() - 1));
        int target = room.placement().size().width() >= 30 ? 12 : 8;
        int start = random.nextInt(candidates.size());
        int placed = 0;
        for (int offset = 0; offset < candidates.size() && placed < target; offset++) {
            BlockPos web = candidates.get((start + offset) % candidates.size());
            if (isNearAny(web, reserved, 1)
                    || isNearAny(web, doors, 3)
                    || placements.containsKey(web)) {
                continue;
            }
            put(placements, web, Blocks.COBWEB.defaultBlockState());
            placed++;
        }
    }

    private static BlockState floorDetail(
            ArchiveRoomCategory category,
            RandomSource random,
            int ordinal) {
        List<BlockState> states = switch (category) {
            case ANCIENT_LIBRARY, LORE, PUZZLE -> List.of(
                    Blocks.BOOKSHELF.defaultBlockState(),
                    Blocks.CHISELED_BOOKSHELF.defaultBlockState(),
                    Blocks.LECTERN.defaultBlockState(),
                    Blocks.DECORATED_POT.defaultBlockState(),
                    Blocks.CANDLE.defaultBlockState());
            case CURSED, SECRET, TRAP -> List.of(
                    Blocks.COBWEB.defaultBlockState(),
                    Blocks.BONE_BLOCK.defaultBlockState(),
                    Blocks.SOUL_LANTERN.defaultBlockState(),
                    Blocks.IRON_BARS.defaultBlockState(),
                    Blocks.DECORATED_POT.defaultBlockState());
            case MINI_BOSS, FINAL_BOSS, ELITE_COMBAT -> List.of(
                    Blocks.IRON_BARS.defaultBlockState(),
                    Blocks.CAULDRON.defaultBlockState(),
                    Blocks.ANVIL.defaultBlockState(),
                    Blocks.BONE_BLOCK.defaultBlockState(),
                    Blocks.SOUL_LANTERN.defaultBlockState());
            case TREASURE, MERCHANT, EXIT_REWARD -> List.of(
                    Blocks.DECORATED_POT.defaultBlockState(),
                    Blocks.CAULDRON.defaultBlockState(),
                    Blocks.ANVIL.defaultBlockState(),
                    Blocks.CHISELED_BOOKSHELF.defaultBlockState(),
                    Blocks.CRAFTING_TABLE.defaultBlockState());
            case SANCTUARY, WAYSTONE -> List.of(
                    Blocks.CANDLE.defaultBlockState(),
                    Blocks.LANTERN.defaultBlockState(),
                    Blocks.BOOKSHELF.defaultBlockState(),
                    Blocks.DECORATED_POT.defaultBlockState(),
                    Blocks.MOSS_CARPET.defaultBlockState());
            default -> List.of(
                    Blocks.DECORATED_POT.defaultBlockState(),
                    Blocks.CAULDRON.defaultBlockState(),
                    Blocks.COBWEB.defaultBlockState(),
                    Blocks.IRON_BARS.defaultBlockState(),
                    Blocks.CANDLE.defaultBlockState(),
                    Blocks.BOOKSHELF.defaultBlockState(),
                    Blocks.MOSS_CARPET.defaultBlockState());
        };
        return states.get(Math.floorMod(random.nextInt(states.size()) + ordinal, states.size()));
    }

    private static BlockState candleState(RandomSource random) {
        return Blocks.CANDLE.defaultBlockState()
                .setValue(BlockStateProperties.CANDLES, 1 + random.nextInt(4))
                .setValue(BlockStateProperties.LIT, random.nextFloat() < 0.42F);
    }

    private static boolean safeFloorDecoration(
            BlockPos position,
            Map<BlockPos, BlockState> placements,
            Set<BlockPos> reserved,
            List<BlockPos> doors,
            int radius) {
        return !isNearAny(position, reserved, radius)
                && !isNearAny(position, doors, 3)
                && !placements.containsKey(position)
                && !placements.containsKey(position.above());
    }

    private static Set<BlockPos> reservedPositions(
            ArchiveRun run,
            ArchiveRoomNode room,
            ArchiveRoomTemplate template) {
        return java.util.stream.Stream.of(
                        template.monsterMarkers(),
                        template.chestMarkers(),
                        template.lootMarkers(),
                        template.trapMarkers(),
                        template.decorationMarkers(),
                        template.puzzleMarkers(),
                        template.secretWallMarkers(),
                        template.bossMarkers(),
                        template.playerEntryMarkers(),
                        template.memorySocketMarkers())
                .flatMap(List::stream)
                .map(marker -> localToWorld(run, room, room.placement().transform().apply(marker, template.size())))
                .collect(java.util.stream.Collectors.toSet());
    }

    private static boolean isNearAny(BlockPos position, Iterable<BlockPos> protectedPositions, int radius) {
        for (BlockPos protectedPosition : protectedPositions) {
            if (Math.abs(position.getX() - protectedPosition.getX()) <= radius
                    && Math.abs(position.getZ() - protectedPosition.getZ()) <= radius) {
                return true;
            }
        }
        return false;
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
                // The floor runs the full cleared width, including under the two
                // wall columns. Flooring only the walkway leaves the cleared
                // columns at centerX-2 and centerX+1 open to the void for the
                // whole length of every corridor, because the walls start a
                // block higher.
                for (int x = centerX - 2; x <= centerX + 1; x++) {
                    put(placements, new BlockPos(x, floorY, z), floor);
                }
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
                for (int z = centerZ - 2; z <= centerZ + 1; z++) {
                    put(placements, new BlockPos(x, floorY, z), floor);
                }
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

        // The stairwell runs inside both rooms' footprints and through the void
        // between them. Where it is inside a room, that room's own shell already
        // encloses it and adding tunnel geometry there just hangs blocks in open
        // air; where it is in the void it needs a full tube of its own.
        BoundingBox lower = firstBounds.minY() == lowY ? firstBounds : secondBounds;
        BoundingBox upper = firstBounds.minY() == highY ? firstBounds : secondBounds;

        // The lower route begins with a stair at floor level; a full upper
        // landing completes the transition without jumping or breaking the shell.
        for (int width = 0; width < 2; width++) {
            BlockPos upperLanding = new BlockPos(centerX + 8, highY, centerZ - 1 + width);
            put(placements, upperLanding, ModBlocks.PHASE_PLATFORM.get().defaultBlockState());
            for (int head = 1; head <= 3; head++) {
                placements.remove(upperLanding.above(head));
            }
            putEnclosure(placements, upperLanding.above(4), roof, lower, upper);
        }
        for (int step = 0; step <= rise; step++) {
            int x = centerX - 9 + Math.min(16, step);
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
                // Two courses, not one. Consecutive steps rise a block as they
                // advance a block, so a single-course ceiling only ever touches
                // its neighbour at a corner and reads as a line of loose blocks
                // hanging in the air. The second course is what joins them.
                putEnclosure(placements, stair.above(4), roof, lower, upper);
                putEnclosure(placements, stair.above(5), roof, lower, upper);
                // Nothing held the stairs up over the gap between floors.
                putEnclosure(placements, stair.below(), wall, lower, upper);
            }
            for (int head = 0; head <= 3; head++) {
                put(placements, new BlockPos(x, y + head, centerZ - 2), wall);
                put(placements, new BlockPos(x, y + head, centerZ + 1), wall);
            }
        }
        clearVolumes.add(new BoundingBox(
                centerX - 9, lowY, centerZ - 3,
                centerX + 9, highY + 4, centerZ + 3));
    }

    /**
     * Place a piece of stairwell tube only where it is not already inside a
     * room, so the tunnel never deposits blocks in a room's open interior.
     */
    private static void putEnclosure(
            Map<BlockPos, BlockState> placements,
            BlockPos position,
            BlockState state,
            BoundingBox lower,
            BoundingBox upper) {
        if (insideInterior(lower, position) || insideInterior(upper, position)) {
            return;
        }
        put(placements, position, state);
    }

    /** True for a room's open interior — inside the shell, not part of it. */
    private static boolean insideInterior(BoundingBox bounds, BlockPos position) {
        return position.getX() > bounds.minX() && position.getX() < bounds.maxX()
                && position.getY() > bounds.minY() && position.getY() < bounds.maxY()
                && position.getZ() > bounds.minZ() && position.getZ() < bounds.maxZ();
    }

    /**
     * Seal every doorway of a room whose encounter is holding it shut.
     *
     * <p>Walks the connections rather than the room's flat door list so each
     * doorway gets the seal that matches what is on the other side of it. Doing
     * this by position alone re-sealed a boss door with an ordinary Archive Seal
     * moments after the wall pass had correctly given it a Cantor Gate.
     */
    private static void placeDoorSeals(Map<BlockPos, BlockState> placements, ArchiveRun run, int roomIndex) {
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        for (ArchiveConnection connection : room.connections()) {
            BlockState seal = doorSeal(room, run.dungeonGraph().room(connection.targetRoom()));
            for (BlockPos position : doorPositions(run, roomIndex, connection.direction())) {
                put(placements, position, seal);
            }
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
            put(placements, position, doorSeal(room, target));
        }
    }

    /**
     * The block a locked doorway is filled with.
     *
     * <p>Every locked door used to be an Archive Seal, so nothing distinguished
     * a door merely held shut from the one with a boss behind it until the
     * player was already through. A door into a boss room gets the Cantor Gate
     * instead, which is the same language in cyan and reads from across a room.
     */
    private static BlockState doorSeal(ArchiveRoomNode room, ArchiveRoomNode target) {
        return isBossRoom(room.category()) || isBossRoom(target.category())
                ? ModBlocks.CANTOR_GATE.get().defaultBlockState()
                : ModBlocks.ARCHIVE_SEAL.get().defaultBlockState();
    }

    private static boolean isBossRoom(ArchiveRoomCategory category) {
        return category == ArchiveRoomCategory.FINAL_BOSS || category == ArchiveRoomCategory.MINI_BOSS;
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
                // One course below the floor plane, because breakable floor
                // hazards put a solid catch there so a collapsing tile does not
                // drop the player into the void. Clearing only from the floor up
                // left those catch blocks behind on every regeneration.
                bounds.minY() - 1,
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

    private record BarredAlcove(
            BlockPos base,
            int stepX,
            int stepZ,
            int recessX,
            int recessZ) {
        private BarredAlcove {
            base = base.immutable();
        }
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
