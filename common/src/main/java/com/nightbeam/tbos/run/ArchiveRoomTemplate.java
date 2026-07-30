package com.nightbeam.tbos.run;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/**
 * Reusable code-native schematic metadata. Geometry and every semantic marker are
 * transformed through the same {@link ArchiveTransform} before placement.
 */
public record ArchiveRoomTemplate(
        Identifier id,
        ArchiveRoomCategory category,
        ArchiveRoomSize size,
        Set<ArchiveDirection> doors,
        List<BlockPos> monsterMarkers,
        List<BlockPos> chestMarkers,
        List<BlockPos> lootMarkers,
        List<BlockPos> trapMarkers,
        List<BlockPos> decorationMarkers,
        List<BlockPos> puzzleMarkers,
        List<BlockPos> secretWallMarkers,
        List<BlockPos> bossMarkers,
        List<BlockPos> playerEntryMarkers,
        List<Identifier> lootTables,
        List<Identifier> monsterGroups,
        int weight,
        boolean rotationSafe,
        boolean mirrorSafe) {
    public ArchiveRoomTemplate {
        id = Objects.requireNonNull(id, "id");
        category = Objects.requireNonNull(category, "category");
        size = Objects.requireNonNull(size, "size");
        doors = Set.copyOf(doors);
        monsterMarkers = validateMarkers("monster", monsterMarkers, size);
        chestMarkers = validateMarkers("chest", chestMarkers, size);
        lootMarkers = validateMarkers("loot", lootMarkers, size);
        trapMarkers = validateMarkers("trap", trapMarkers, size);
        decorationMarkers = validateMarkers("decoration", decorationMarkers, size);
        puzzleMarkers = validateMarkers("puzzle", puzzleMarkers, size);
        secretWallMarkers = validateMarkers("secret wall", secretWallMarkers, size);
        bossMarkers = validateMarkers("boss", bossMarkers, size);
        playerEntryMarkers = validateMarkers("player entry", playerEntryMarkers, size);
        lootTables = List.copyOf(lootTables);
        monsterGroups = List.copyOf(monsterGroups);
        if (doors.isEmpty() || playerEntryMarkers.isEmpty() || weight < 1 || weight > 1_000) {
            throw new IllegalArgumentException("Archive template has no doors/entry marker or invalid weight: " + id);
        }
        if ((category.combat()
                        || category == ArchiveRoomCategory.TRAP
                        || category == ArchiveRoomCategory.PUZZLE)
                && monsterMarkers.isEmpty() && bossMarkers.isEmpty()) {
            throw new IllegalArgumentException("Combat archive template has no spawn markers: " + id);
        }
        if (category == ArchiveRoomCategory.FINAL_BOSS && bossMarkers.isEmpty()) {
            throw new IllegalArgumentException("Final boss template has no boss marker: " + id);
        }
    }

    public boolean supports(ArchiveDirection worldDirection, ArchiveTransform transform) {
        return doors.stream().map(transform::apply).anyMatch(worldDirection::equals);
    }

    public List<BlockPos> transformed(List<BlockPos> markers, ArchiveTransform transform) {
        return markers.stream().map(marker -> transform.apply(marker, size)).toList();
    }

    public List<String> validate() {
        java.util.ArrayList<String> errors = new java.util.ArrayList<>();
        if (rotationSafe && size.width() != size.depth()) {
            errors.add(id + " declares rotation-safe geometry with a non-square footprint");
        }
        if (category == ArchiveRoomCategory.SECRET && secretWallMarkers.isEmpty()) {
            errors.add(id + " is secret but has no secret-wall marker");
        }
        if ((category == ArchiveRoomCategory.TREASURE || category == ArchiveRoomCategory.EXIT_REWARD)
                && chestMarkers.isEmpty() && lootMarkers.isEmpty()) {
            errors.add(id + " is a reward room without a chest or direct-loot marker");
        }
        return List.copyOf(errors);
    }

    private static List<BlockPos> validateMarkers(String name, List<BlockPos> markers, ArchiveRoomSize size) {
        List<BlockPos> immutable = markers.stream().map(BlockPos::immutable).distinct().toList();
        for (BlockPos marker : immutable) {
            if (marker.getX() < 0 || marker.getX() >= size.width()
                    || marker.getY() < 0 || marker.getY() >= size.height()
                    || marker.getZ() < 0 || marker.getZ() >= size.depth()) {
                throw new IllegalArgumentException("Archive " + name + " marker lies outside template "
                        + size + ": " + marker);
            }
        }
        return immutable;
    }
}
