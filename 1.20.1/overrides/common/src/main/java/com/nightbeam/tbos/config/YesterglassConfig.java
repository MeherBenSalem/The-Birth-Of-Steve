package com.nightbeam.tbos.config;

import com.nightbeam.tbos.platform.Services;
import com.nightbeam.tbos.run.ArchiveDungeonRules;
import com.nightbeam.tbos.run.ArchiveDungeonSettings;
import com.nightbeam.tbos.run.ArchiveEnemyKind;
import com.nightbeam.tbos.run.ArchiveLootMode;
import com.nightbeam.tbos.run.ArchiveRoomTemplates;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * The mod's common config, at {@code config/tbos-common.json}.
 *
 * <p>Every list default is derived from {@link ArchiveDungeonRules#DEFAULT}
 * rather than restated, so a freshly generated file and the "config not loaded"
 * fallback describe exactly the same dungeon. That matters: weighted enemy picks
 * scan their list in order, so a re-ordered restatement of the same weights
 * would quietly change which mob a given seed spawns.
 */
public final class YesterglassConfig {
    private static final ConfigSchema SCHEMA = new ConfigSchema("tbos-common.json");

    public static final ConfigValue.Bool GRANT_SURVEY_MAP = SCHEMA.define(
            "grantSurveyMap", true,
            "Grant a Folded Survey Map once to new players.");

    public static final ConfigValue.Bool PROTECT_ACTIVE_SITE = SCHEMA.define(
            "protectActiveSite", true,
            "Protect authored phase geometry while its memory site is active.");

    public static final ConfigValue.Int TRANSITION_TICKS = SCHEMA.defineInRange(
            "transitionTicks", 40, 36, 48,
            "Duration of a temporal transition in ticks (36-48 = 1.8-2.4 seconds).");

    static {
        SCHEMA.push("echoesOfThePastDungeon");
    }

    public static final ConfigValue.Int DUNGEON_MIN_ROOMS = SCHEMA.defineInRange(
            "minimumRooms", 14, 7, 48,
            "Minimum number of rooms in a generated Echoes of the Past dungeon.");
    public static final ConfigValue.Int DUNGEON_MAX_ROOMS = SCHEMA.defineInRange(
            "maximumRooms", 20, 7, 48,
            "Maximum number of rooms in a generated Echoes of the Past dungeon.");
    public static final ConfigValue.Int DUNGEON_HORIZONTAL_LIMIT = SCHEMA.defineInRange(
            "horizontalExpansion", 6, 2, 12,
            "Maximum logical room distance east/west and north/south from the start.");
    public static final ConfigValue.Int DUNGEON_VERTICAL_LIMIT = SCHEMA.defineInRange(
            "verticalExpansion", 2, 1, 4,
            "Maximum number of logical floors above or below the starting floor.");
    public static final ConfigValue.Int DUNGEON_MAX_DEPTH = SCHEMA.defineInRange(
            "maximumDepth", 16, 4, 48,
            "Maximum graph distance from the starting room.");
    public static final ConfigValue.Double DUNGEON_BRANCHING = SCHEMA.defineInRange(
            "branchingProbability", 0.48D, 0.0D, 1.0D,
            "Relative preference for expanding rooms that already have multiple connections.");
    public static final ConfigValue.Double DUNGEON_DEAD_END = SCHEMA.defineInRange(
            "deadEndProbability", 0.16D, 0.0D, 1.0D,
            "Relative preference for leaving one-door rooms as dead ends.");
    public static final ConfigValue.Double DUNGEON_LOOP = SCHEMA.defineInRange(
            "loopProbability", 0.10D, 0.0D, 1.0D,
            "Chance to connect adjacent rooms into a navigational loop.");
    public static final ConfigValue.Double DUNGEON_SPECIAL_FREQUENCY = SCHEMA.defineInRange(
            "specialRoomFrequency", 0.30D, 0.0D, 1.0D,
            "Chance for a non-mandatory room to use a special category.");
    public static final ConfigValue.Double DUNGEON_SECRET_FREQUENCY = SCHEMA.defineInRange(
            "secretRoomProbability", 0.12D, 0.0D, 1.0D,
            "Chance for a generated branch room to be hidden behind a secret wall.");
    public static final ConfigValue.Double DUNGEON_CHEST_FREQUENCY = SCHEMA.defineInRange(
            "chestProbability", 0.50D, 0.0D, 1.0D,
            "Chance that an optional room chest marker receives a container.");
    public static final ConfigValue.Double DUNGEON_TRAP_FREQUENCY = SCHEMA.defineInRange(
            "trapRoomProbability", 0.10D, 0.0D, 1.0D,
            "Chance that an ordinary non-vertical room becomes a trap encounter.");
    public static final ConfigValue.Double DUNGEON_MODIFIER_FREQUENCY = SCHEMA.defineInRange(
            "roomModifierProbability", 0.18D, 0.0D, 1.0D,
            "Base chance for a room modifier; depth can increase this chance.");
    public static final ConfigValue.Double DUNGEON_DIRECT_LOOT_FREQUENCY = SCHEMA.defineInRange(
            "directLootProbability", 0.42D, 0.0D, 1.0D,
            "Chance that a completion reward is rolled directly onto a valid room marker.");
    public static final ConfigValue.Int DUNGEON_MAX_ABOVE = SCHEMA.defineInRange(
            "maximumRoomsAbove", 4, 0, 48,
            "Maximum count of generated rooms above the starting level.");
    public static final ConfigValue.Int DUNGEON_MAX_BELOW = SCHEMA.defineInRange(
            "maximumRoomsBelow", 4, 0, 48,
            "Maximum count of generated rooms below the starting level.");
    public static final ConfigValue.Int DUNGEON_BLOCK_BUDGET = SCHEMA.defineInRange(
            "blockBudgetPerTick", 4096, 256, 65536,
            "Maximum archive blocks changed by the staged generator each server tick.");
    public static final ConfigValue.Int DUNGEON_GENERATION_ATTEMPTS = SCHEMA.defineInRange(
            "generationAttempts", 64, 1, 512,
            "Maximum deterministic retry attempts before a seed is rejected.");
    public static final ConfigValue.Bool DUNGEON_DEBUG = SCHEMA.define(
            "debugMode", false,
            "Log template selection, rejected placements, doors, encounters, and loot rolls.");
    public static final ConfigValue.Bool DUNGEON_REGENERATE_INCOMPLETE = SCHEMA.define(
            "regenerateIncompleteAfterRestart", true,
            "Resume or regenerate a PREPARING dungeon after a server restart.");

    static {
        SCHEMA.push("content");
    }

    public static final ConfigValue.TextList DUNGEON_ALLOWED_TEMPLATES = SCHEMA.defineList(
            "allowedTemplates", List.of(), YesterglassConfig::isIdentifier,
            "Allowed reusable room template IDs, e.g. \"tbos:parallax_wake\". The mandatory start,"
                    + " boss, and reward templates must remain. An empty list enables the complete"
                    + " built-in Echoes of the Past catalog.");
    public static final ConfigValue.TextList DUNGEON_ROOM_WEIGHTS = SCHEMA.defineList(
            "roomWeights", List.of(), YesterglassConfig::isWeightedIdentifier,
            "Optional room-template weight overrides in namespace:path=weight form,"
                    + " e.g. \"tbos:starless_gallery=100\".");
    public static final ConfigValue.TextList DUNGEON_LOOT_WEIGHTS = SCHEMA.defineList(
            "lootTableWeights", defaultLootWeights(), YesterglassConfig::isWeightedIdentifier,
            "Loot-table selection weights in namespace:path=weight form.");
    public static final ConfigValue.Text DUNGEON_BOSS_TEMPLATE = SCHEMA.define(
            "bossTemplate", "tbos:hour_cantor", YesterglassConfig::isIdentifier,
            "Final-boss room template ID.");

    static {
        SCHEMA.pop();
        SCHEMA.push("encounters");
    }

    public static final ConfigValue.TextList DUNGEON_ENEMY_POOLS = SCHEMA.defineList(
            "enemyPools", defaultEnemyPools(), YesterglassConfig::isEnemyPoolEntry,
            "Weighted encounter entries in group|enemy=weight form. Registered Yesterglass enemies"
                    + " and supported vanilla monsters are accepted.");
    public static final ConfigValue.Double DUNGEON_HEALTH_PER_DIFFICULTY = SCHEMA.defineInRange(
            "healthPerDifficulty", 0.025D, 0.0D, 1.0D,
            "Enemy max-health increase per room difficulty point.");
    public static final ConfigValue.Double DUNGEON_DAMAGE_PER_DIFFICULTY = SCHEMA.defineInRange(
            "damagePerDifficulty", 0.015D, 0.0D, 1.0D,
            "Enemy attack-damage increase per room difficulty point.");
    public static final ConfigValue.Double DUNGEON_HEALTH_PER_PLAYER = SCHEMA.defineInRange(
            "healthPerAdditionalPlayer", 0.25D, 0.0D, 4.0D,
            "Enemy max-health increase per additional active player in that room.");
    public static final ConfigValue.Double DUNGEON_ENEMIES_PER_PLAYER = SCHEMA.defineInRange(
            "enemiesPerAdditionalPlayer", 1.0D, 0.0D, 8.0D,
            "Additional weighted enemy picks per additional active player and wave.");

    static {
        SCHEMA.pop();
        SCHEMA.push("doorsAndLoot");
    }

    public static final ConfigValue.Bool DUNGEON_LOCK_COMBAT_DOORS = SCHEMA.define(
            "lockCombatDoors", true,
            "Seal all connected routes while an uncleared combat encounter is active.");
    public static final ConfigValue.OfEnum<ArchiveLootMode> DUNGEON_LOOT_MODE = SCHEMA.defineEnum(
            "lootMode", ArchiveLootMode.INDIVIDUAL,
            "INDIVIDUAL gives each party member one durable claim per cache; SHARED consumes it once.");

    static {
        SCHEMA.pop();
        SCHEMA.push("seedAndPersistence");
    }

    public static final ConfigValue.Long DUNGEON_FORCED_SEED = SCHEMA.defineInRange(
            "forcedSeed", ArchiveDungeonRules.RANDOM_SEED, Long.MIN_VALUE, Long.MAX_VALUE,
            "Long.MIN_VALUE chooses a fresh derived seed. Any other value forces normal dungeon generation.");
    public static final ConfigValue.Bool DUNGEON_RETAIN_COMPLETED = SCHEMA.define(
            "retainCompletedRuns", false,
            "Keep terminal run records and geometry after every member returns; false queues cleanup.");

    static {
        SCHEMA.pop();
        SCHEMA.pop();
    }

    private YesterglassConfig() {
    }

    /** Called once by each loader's entry point. */
    public static void load() {
        SCHEMA.load(Services.PLATFORM.configDir());
    }

    public static ArchiveDungeonSettings dungeonSettings() {
        int minimum = DUNGEON_MIN_ROOMS.get();
        int maximum = Math.max(minimum, DUNGEON_MAX_ROOMS.get());
        return new ArchiveDungeonSettings(
                minimum,
                maximum,
                DUNGEON_HORIZONTAL_LIMIT.get(),
                DUNGEON_VERTICAL_LIMIT.get(),
                DUNGEON_MAX_DEPTH.get(),
                DUNGEON_BRANCHING.get(),
                DUNGEON_DEAD_END.get(),
                DUNGEON_LOOP.get(),
                DUNGEON_SPECIAL_FREQUENCY.get(),
                Math.min(maximum, DUNGEON_MAX_ABOVE.get()),
                Math.min(maximum, DUNGEON_MAX_BELOW.get()),
                DUNGEON_SECRET_FREQUENCY.get(),
                DUNGEON_CHEST_FREQUENCY.get(),
                DUNGEON_BLOCK_BUDGET.get(),
                DUNGEON_GENERATION_ATTEMPTS.get(),
                dungeonRules());
    }

    public static ArchiveDungeonRules dungeonRules() {
        Set<ResourceLocation> allowed = DUNGEON_ALLOWED_TEMPLATES.get().stream()
                .map(String::valueOf)
                .map(YesterglassConfig::ResourceLocation)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<ResourceLocation, Integer> templateWeights = parseWeights(DUNGEON_ROOM_WEIGHTS.get());
        Map<ResourceLocation, Integer> lootWeights = parseWeights(DUNGEON_LOOT_WEIGHTS.get());
        Map<ResourceLocation, List<ArchiveDungeonRules.EnemyWeight>> enemyPools =
                parseEnemyPools(DUNGEON_ENEMY_POOLS.get());
        ResourceLocation boss = ResourceLocation(DUNGEON_BOSS_TEMPLATE.get());
        if (ArchiveRoomTemplates.find(boss).isEmpty()) {
            throw new IllegalArgumentException("Configured archive boss template is unknown: " + boss);
        }
        return new ArchiveDungeonRules(
                allowed,
                templateWeights,
                lootWeights,
                enemyPools,
                boss,
                DUNGEON_TRAP_FREQUENCY.get(),
                DUNGEON_MODIFIER_FREQUENCY.get(),
                DUNGEON_DIRECT_LOOT_FREQUENCY.get(),
                DUNGEON_HEALTH_PER_DIFFICULTY.get(),
                DUNGEON_DAMAGE_PER_DIFFICULTY.get(),
                DUNGEON_HEALTH_PER_PLAYER.get(),
                DUNGEON_ENEMIES_PER_PLAYER.get(),
                DUNGEON_LOCK_COMBAT_DOORS.get(),
                DUNGEON_FORCED_SEED.get(),
                DUNGEON_LOOT_MODE.get(),
                DUNGEON_RETAIN_COMPLETED.get());
    }

    /** Renders {@link ArchiveDungeonRules#DEFAULT}'s loot weights as config lines. */
    private static List<String> defaultLootWeights() {
        List<String> lines = new ArrayList<>();
        ArchiveDungeonRules.DEFAULT.lootTableWeights()
                .forEach((table, weight) -> lines.add(table + "=" + weight));
        lines.sort(null);
        return List.copyOf(lines);
    }

    /**
     * Renders {@link ArchiveDungeonRules#DEFAULT}'s enemy pools as config lines,
     * preserving each group's list order because weighted picks depend on it.
     */
    private static List<String> defaultEnemyPools() {
        List<String> lines = new ArrayList<>();
        List<ResourceLocation> groups = new ArrayList<>(ArchiveDungeonRules.DEFAULT.enemyPools().keySet());
        groups.sort(java.util.Comparator.comparing(ResourceLocation::toString));
        for (ResourceLocation group : groups) {
            for (ArchiveDungeonRules.EnemyWeight entry
                    : ArchiveDungeonRules.DEFAULT.enemyPools().get(group)) {
                lines.add(group + "|" + entry.kind().id() + "=" + entry.weight());
            }
        }
        return List.copyOf(lines);
    }

    private static Map<ResourceLocation, Integer> parseWeights(List<? extends String> entries) {
        LinkedHashMap<ResourceLocation, Integer> result = new LinkedHashMap<>();
        for (String raw : entries) {
            int separator = raw.lastIndexOf('=');
            ResourceLocation id = ResourceLocation(raw.substring(0, separator));
            int weight = Integer.parseInt(raw.substring(separator + 1));
            result.put(id, weight);
        }
        return Map.copyOf(result);
    }

    private static Map<ResourceLocation, List<ArchiveDungeonRules.EnemyWeight>> parseEnemyPools(
            List<? extends String> entries) {
        LinkedHashMap<ResourceLocation, List<ArchiveDungeonRules.EnemyWeight>> mutable = new LinkedHashMap<>();
        for (String raw : entries) {
            int groupSeparator = raw.indexOf('|');
            int weightSeparator = raw.lastIndexOf('=');
            ResourceLocation group = ResourceLocation(raw.substring(0, groupSeparator));
            ArchiveEnemyKind enemy = ArchiveEnemyKind.parse(
                            raw.substring(groupSeparator + 1, weightSeparator))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown archive enemy in config: " + raw));
            int weight = Integer.parseInt(raw.substring(weightSeparator + 1));
            mutable.computeIfAbsent(group, ignored -> new ArrayList<>())
                    .add(new ArchiveDungeonRules.EnemyWeight(enemy, weight));
        }
        return mutable.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    private static boolean isIdentifier(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        try {
            ResourceLocation(text);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean isWeightedIdentifier(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        int separator = text.lastIndexOf('=');
        if (separator <= 0 || !isIdentifier(text.substring(0, separator))) {
            return false;
        }
        try {
            int weight = Integer.parseInt(text.substring(separator + 1));
            return weight >= 1 && weight <= 10_000;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean isEnemyPoolEntry(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        int groupSeparator = text.indexOf('|');
        int weightSeparator = text.lastIndexOf('=');
        return groupSeparator > 0
                && weightSeparator > groupSeparator + 1
                && isIdentifier(text.substring(0, groupSeparator))
                && ArchiveEnemyKind.parse(text.substring(groupSeparator + 1, weightSeparator)).isPresent()
                && isWeight(text.substring(weightSeparator + 1));
    }

    private static boolean isWeight(String text) {
        try {
            int weight = Integer.parseInt(text);
            return weight >= 1 && weight <= 10_000;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static ResourceLocation ResourceLocation(String value) {
        return new ResourceLocation(value.trim());
    }
}
