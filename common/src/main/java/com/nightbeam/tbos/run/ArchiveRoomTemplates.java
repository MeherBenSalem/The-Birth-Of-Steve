package com.nightbeam.tbos.run;

import com.nightbeam.tbos.Yesterglass;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Built-in Echoes of the Past template catalog and marker validation. */
public final class ArchiveRoomTemplates {
    private static final ArchiveRoomSize STANDARD = new ArchiveRoomSize(24, 8, 24);
    private static final ArchiveRoomSize LARGE = new ArchiveRoomSize(32, 10, 32);
    // The vertical connector needs one landing block on both ends of its
    // sixteen-block rise. A 24-wide shell keeps those landings inside the
    // protected room instead of ending the stairs flush against a wall.
    private static final ArchiveRoomSize SHAFT = new ArchiveRoomSize(24, 10, 24);
    private static final EnumSet<ArchiveDirection> ALL_DOORS = EnumSet.allOf(ArchiveDirection.class);

    private static final List<ArchiveRoomTemplate> TEMPLATES = List.of(
            template("parallax_wake", ArchiveRoomCategory.STARTING, STANDARD, 100),
            template("starless_gallery", ArchiveRoomCategory.STANDARD_COMBAT, STANDARD, 100),
            template("lenswright_crossing", ArchiveRoomCategory.STANDARD_COMBAT, STANDARD, 100),
            template("echo_foundry", ArchiveRoomCategory.TRAP, STANDARD, 70),
            template("meridian_sentinel", ArchiveRoomCategory.ELITE_COMBAT, LARGE, 60),
            template("glassbound_huntsman", ArchiveRoomCategory.ELITE_COMBAT, LARGE, 60),
            template("hall_alignment_reforged", ArchiveRoomCategory.PUZZLE, LARGE, 65),
            template("choir_of_hours_reforged", ArchiveRoomCategory.PUZZLE, LARGE, 55),
            template("chronicle_stack", ArchiveRoomCategory.ANCIENT_LIBRARY, STANDARD, 55),
            template("sealed_reliquary", ArchiveRoomCategory.TREASURE, STANDARD, 45),
            template("quiet_ossuary", ArchiveRoomCategory.SANCTUARY, STANDARD, 45),
            template("annalist_vigil", ArchiveRoomCategory.LORE, STANDARD, 45),
            template("veiled_broker", ArchiveRoomCategory.MERCHANT, STANDARD, 30),
            template("meridian_drop", ArchiveRoomCategory.VERTICAL_SHAFT, SHAFT, 75),
            template("buried_index", ArchiveRoomCategory.SECRET, STANDARD, 35),
            template("crownless_chapel", ArchiveRoomCategory.CURSED, STANDARD, 45),
            template("bronze_reviser", ArchiveRoomCategory.MINI_BOSS, LARGE, 55),
            template("archivist_tribunal", ArchiveRoomCategory.MINI_BOSS, LARGE, 50),
            template("hour_cantor", ArchiveRoomCategory.FINAL_BOSS, LARGE, 100),
            template("last_recollection", ArchiveRoomCategory.EXIT_REWARD, STANDARD, 100));

    private static final Map<Identifier, ArchiveRoomTemplate> BY_ID = TEMPLATES.stream()
            .collect(Collectors.toUnmodifiableMap(ArchiveRoomTemplate::id, Function.identity()));

    private ArchiveRoomTemplates() {
    }

    public static List<ArchiveRoomTemplate> all() {
        return TEMPLATES;
    }

    public static Optional<ArchiveRoomTemplate> find(Identifier id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static ArchiveRoomTemplate require(Identifier id) {
        ArchiveRoomTemplate exact = BY_ID.get(id);
        if (exact != null) {
            return exact;
        }
        return TEMPLATES.stream()
                .filter(template -> template.category() == ArchiveRoomCategory.STANDARD_COMBAT)
                .findFirst()
                .orElseThrow();
    }

    public static List<ArchiveRoomTemplate> forCategory(ArchiveRoomCategory category) {
        return TEMPLATES.stream().filter(template -> template.category() == category).toList();
    }

    public static List<String> validateAll() {
        java.util.ArrayList<String> errors = new java.util.ArrayList<>();
        if (BY_ID.size() != TEMPLATES.size()) {
            errors.add("The archive template catalog contains duplicate IDs");
        }
        for (ArchiveRoomCategory category : ArchiveRoomCategory.values()) {
            if (forCategory(category).isEmpty()) {
                errors.add("No archive template is registered for category " + category);
            }
        }
        TEMPLATES.forEach(template -> errors.addAll(template.validate()));
        return List.copyOf(errors);
    }

    private static ArchiveRoomTemplate template(
            String path, ArchiveRoomCategory category, ArchiveRoomSize size, int weight) {
        int centerX = size.width() / 2;
        int centerZ = size.depth() / 2;
        boolean enemyEncounter = category.combat()
                || category == ArchiveRoomCategory.TRAP
                || category == ArchiveRoomCategory.PUZZLE;
        List<BlockPos> monsterMarkers = enemyEncounter
                ? List.of(
                        new BlockPos(5, 1, 6),
                        new BlockPos(size.width() - 6, 1, 6),
                        new BlockPos(5, 1, size.depth() - 7),
                        new BlockPos(size.width() - 6, 1, size.depth() - 7),
                        new BlockPos(centerX, 1, centerZ))
                : List.of();
        List<BlockPos> chestMarkers = category == ArchiveRoomCategory.TREASURE
                        || category == ArchiveRoomCategory.SECRET
                        || category == ArchiveRoomCategory.EXIT_REWARD
                ? List.of(new BlockPos(centerX, 1, centerZ), new BlockPos(4, 1, size.depth() - 5))
                : List.of(new BlockPos(size.width() - 5, 1, size.depth() - 5));
        List<BlockPos> puzzleMarkers = category == ArchiveRoomCategory.PUZZLE
                ? List.of(new BlockPos(5, 1, 6), new BlockPos(centerX, 1, 6),
                        new BlockPos(size.width() - 6, 1, 6), new BlockPos(centerX, 1, size.depth() - 7))
                : List.of();
        List<BlockPos> secretMarkers = category == ArchiveRoomCategory.SECRET
                ? List.of(new BlockPos(centerX, 2, 0))
                : List.of();
        List<BlockPos> bossMarkers = category == ArchiveRoomCategory.FINAL_BOSS
                ? List.of(new BlockPos(centerX, 1, centerZ))
                : List.of();
        Identifier commonLoot = id(category == ArchiveRoomCategory.EXIT_REWARD
                ? "loot/boss_reward"
                : category == ArchiveRoomCategory.SECRET
                        ? "loot/secret"
                        : category == ArchiveRoomCategory.MINI_BOSS ? "loot/lesser_boss" : "loot/common");
        Identifier monsterGroup = id(switch (category) {
            case ELITE_COMBAT, CURSED -> "encounters/elite_echoes";
            case MINI_BOSS -> "encounters/ruined_guardian";
            case FINAL_BOSS -> "encounters/hour_cantor";
            default -> "encounters/forgotten_legion";
        });
        return new ArchiveRoomTemplate(
                id(path),
                category,
                size,
                ALL_DOORS,
                monsterMarkers,
                chestMarkers,
                List.of(new BlockPos(centerX, 1, Math.max(3, centerZ - 3))),
                category == ArchiveRoomCategory.TRAP
                        ? List.of(new BlockPos(centerX - 2, 1, centerZ), new BlockPos(centerX + 2, 1, centerZ))
                        : List.of(),
                List.of(new BlockPos(3, 1, 3), new BlockPos(size.width() - 4, 1, size.depth() - 4)),
                puzzleMarkers,
                secretMarkers,
                bossMarkers,
                List.of(new BlockPos(centerX, 1, 3)),
                List.of(commonLoot),
                enemyEncounter ? List.of(monsterGroup) : List.of(),
                weight,
                true,
                true);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, path);
    }
}
