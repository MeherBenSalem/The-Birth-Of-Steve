package com.nightbeam.tbos.config;

import com.nightbeam.tbos.run.ArchiveDungeonSettings;
import com.nightbeam.tbos.run.ArchiveDungeonRules;
import com.nightbeam.tbos.run.ArchiveEnemyKind;
import com.nightbeam.tbos.run.ArchiveLootMode;
import com.nightbeam.tbos.run.ArchiveRoomTemplates;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class YesterglassConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue GRANT_SURVEY_MAP = BUILDER
            .comment("Grant a Folded Survey Map once to new players.")
            .define("grantSurveyMap", true);

    public static final ModConfigSpec.BooleanValue PROTECT_ACTIVE_SITE = BUILDER
            .comment("Protect authored phase geometry while its memory site is active.")
            .define("protectActiveSite", true);

    public static final ModConfigSpec.IntValue TRANSITION_TICKS = BUILDER
            .comment("Duration of a temporal transition in ticks (36-48 = 1.8-2.4 seconds).")
            .defineInRange("transitionTicks", 40, 36, 48);

    static {
        BUILDER.push("echoesOfThePastDungeon");
    }

    public static final ModConfigSpec.IntValue DUNGEON_MIN_ROOMS = BUILDER
            .comment("Minimum number of rooms in a generated Echoes of the Past dungeon.")
            .defineInRange("minimumRooms", 14, 7, 48);
    public static final ModConfigSpec.IntValue DUNGEON_MAX_ROOMS = BUILDER
            .comment("Maximum number of rooms in a generated Echoes of the Past dungeon.")
            .defineInRange("maximumRooms", 20, 7, 48);
    public static final ModConfigSpec.IntValue DUNGEON_HORIZONTAL_LIMIT = BUILDER
            .comment("Maximum logical room distance east/west and north/south from the start.")
            .defineInRange("horizontalExpansion", 6, 2, 12);
    public static final ModConfigSpec.IntValue DUNGEON_VERTICAL_LIMIT = BUILDER
            .comment("Maximum number of logical floors above or below the starting floor.")
            .defineInRange("verticalExpansion", 2, 1, 4);
    public static final ModConfigSpec.IntValue DUNGEON_MAX_DEPTH = BUILDER
            .comment("Maximum graph distance from the starting room.")
            .defineInRange("maximumDepth", 16, 4, 48);
    public static final ModConfigSpec.DoubleValue DUNGEON_BRANCHING = BUILDER
            .comment("Relative preference for expanding rooms that already have multiple connections.")
            .defineInRange("branchingProbability", 0.48D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue DUNGEON_DEAD_END = BUILDER
            .comment("Relative preference for leaving one-door rooms as dead ends.")
            .defineInRange("deadEndProbability", 0.16D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue DUNGEON_LOOP = BUILDER
            .comment("Chance to connect adjacent rooms into a navigational loop.")
            .defineInRange("loopProbability", 0.10D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue DUNGEON_SPECIAL_FREQUENCY = BUILDER
            .comment("Chance for a non-mandatory room to use a special category.")
            .defineInRange("specialRoomFrequency", 0.30D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue DUNGEON_SECRET_FREQUENCY = BUILDER
            .comment("Chance for a generated branch room to be hidden behind a secret wall.")
            .defineInRange("secretRoomProbability", 0.12D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue DUNGEON_CHEST_FREQUENCY = BUILDER
            .comment("Chance that an optional room chest marker receives a container.")
            .defineInRange("chestProbability", 0.50D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue DUNGEON_TRAP_FREQUENCY = BUILDER
            .comment("Chance that an ordinary non-vertical room becomes a trap encounter.")
            .defineInRange("trapRoomProbability", 0.10D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue DUNGEON_MODIFIER_FREQUENCY = BUILDER
            .comment("Base chance for a room modifier; depth can increase this chance.")
            .defineInRange("roomModifierProbability", 0.18D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue DUNGEON_DIRECT_LOOT_FREQUENCY = BUILDER
            .comment("Chance that a completion reward is rolled directly onto a valid room marker.")
            .defineInRange("directLootProbability", 0.42D, 0.0D, 1.0D);
    public static final ModConfigSpec.IntValue DUNGEON_MAX_ABOVE = BUILDER
            .comment("Maximum count of generated rooms above the starting level.")
            .defineInRange("maximumRoomsAbove", 4, 0, 48);
    public static final ModConfigSpec.IntValue DUNGEON_MAX_BELOW = BUILDER
            .comment("Maximum count of generated rooms below the starting level.")
            .defineInRange("maximumRoomsBelow", 4, 0, 48);
    public static final ModConfigSpec.IntValue DUNGEON_BLOCK_BUDGET = BUILDER
            .comment("Maximum archive blocks changed by the staged generator each server tick.")
            .defineInRange("blockBudgetPerTick", 4096, 256, 65536);
    public static final ModConfigSpec.IntValue DUNGEON_GENERATION_ATTEMPTS = BUILDER
            .comment("Maximum deterministic retry attempts before a seed is rejected.")
            .defineInRange("generationAttempts", 64, 1, 512);
    public static final ModConfigSpec.BooleanValue DUNGEON_DEBUG = BUILDER
            .comment("Log template selection, rejected placements, doors, encounters, and loot rolls.")
            .define("debugMode", false);
    public static final ModConfigSpec.BooleanValue DUNGEON_REGENERATE_INCOMPLETE = BUILDER
            .comment("Resume or regenerate a PREPARING dungeon after a server restart.")
            .define("regenerateIncompleteAfterRestart", true);

    static {
        BUILDER.push("content");
    }

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DUNGEON_ALLOWED_TEMPLATES = BUILDER
            .comment(
                    "Allowed reusable room template IDs. The mandatory start, boss, and reward templates must remain.",
                    "An empty list enables the complete built-in Echoes of the Past catalog.")
            .defineListAllowEmpty(
                    "allowedTemplates", List.of(), () -> "tbos:parallax_wake",
                    YesterglassConfig::isIdentifier);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DUNGEON_ROOM_WEIGHTS = BUILDER
            .comment("Optional room-template weight overrides in namespace:path=weight form.")
            .defineListAllowEmpty(
                    "roomWeights", List.of(), () -> "tbos:starless_gallery=100",
                    YesterglassConfig::isWeightedIdentifier);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> DUNGEON_LOOT_WEIGHTS = BUILDER
            .comment("Loot-table selection weights in namespace:path=weight form.")
            .defineList(
                    "lootTableWeights",
                    List.of(
                            "tbos:loot/common=8",
                            "tbos:loot/secret=4",
                            "tbos:loot/lesser_boss=2",
                            "tbos:loot/boss_reward=1"),
                    () -> "tbos:loot/common=1",
                    YesterglassConfig::isWeightedIdentifier);
    public static final ModConfigSpec.ConfigValue<String> DUNGEON_BOSS_TEMPLATE = BUILDER
            .comment("Final-boss room template ID.")
            .define("bossTemplate", "tbos:hour_cantor", YesterglassConfig::isIdentifier);

    static {
        BUILDER.pop();
        BUILDER.push("encounters");
    }

    public static final ModConfigSpec.ConfigValue<List<? extends String>> DUNGEON_ENEMY_POOLS = BUILDER
            .comment(
                    "Weighted encounter entries in group|enemy=weight form.",
                    "Registered Yesterglass enemies and supported vanilla monsters are accepted.")
            .defineList(
                    "enemyPools",
                    List.of(
                            "tbos:encounters/forgotten_legion|tbos:parallax_wraith=4",
                            "tbos:encounters/forgotten_legion|tbos:meridian_sentinel=1",
                            "tbos:encounters/forgotten_legion|minecraft:husk=4",
                            "tbos:encounters/forgotten_legion|minecraft:skeleton=3",
                            "tbos:encounters/forgotten_legion|minecraft:stray=2",
                            "tbos:encounters/forgotten_legion|minecraft:cave_spider=2",
                            "tbos:encounters/forgotten_legion|minecraft:silverfish=2",
                            "tbos:encounters/forgotten_legion|tbos:memory_leech=2",
                            "tbos:encounters/elite_echoes|tbos:parallax_wraith=2",
                            "tbos:encounters/elite_echoes|tbos:meridian_sentinel=4",
                            "tbos:encounters/elite_echoes|tbos:memory_leech=3",
                            "tbos:encounters/elite_echoes|minecraft:stray=2",
                            "tbos:encounters/elite_echoes|minecraft:vindicator=3",
                            "tbos:encounters/elite_echoes|minecraft:evoker=1",
                            "tbos:encounters/ruined_guardian|tbos:meridian_sentinel=4",
                            "tbos:encounters/ruined_guardian|tbos:parallax_wraith=1",
                            "tbos:encounters/ruined_guardian|minecraft:vindicator=3",
                            "tbos:encounters/ruined_guardian|minecraft:evoker=2",
                            "tbos:encounters/ruined_guardian|minecraft:ravager=1",
                            "tbos:encounters/hour_cantor|tbos:hour_cantor=1"),
                    () -> "tbos:encounters/forgotten_legion|tbos:parallax_wraith=1",
                    YesterglassConfig::isEnemyPoolEntry);
    public static final ModConfigSpec.DoubleValue DUNGEON_HEALTH_PER_DIFFICULTY = BUILDER
            .comment("Enemy max-health increase per room difficulty point.")
            .defineInRange("healthPerDifficulty", 0.025D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue DUNGEON_DAMAGE_PER_DIFFICULTY = BUILDER
            .comment("Enemy attack-damage increase per room difficulty point.")
            .defineInRange("damagePerDifficulty", 0.015D, 0.0D, 1.0D);
    public static final ModConfigSpec.DoubleValue DUNGEON_HEALTH_PER_PLAYER = BUILDER
            .comment("Enemy max-health increase per additional active player in that room.")
            .defineInRange("healthPerAdditionalPlayer", 0.25D, 0.0D, 4.0D);
    public static final ModConfigSpec.DoubleValue DUNGEON_ENEMIES_PER_PLAYER = BUILDER
            .comment("Additional weighted enemy picks per additional active player and wave.")
            .defineInRange("enemiesPerAdditionalPlayer", 1.0D, 0.0D, 8.0D);

    static {
        BUILDER.pop();
        BUILDER.push("doorsAndLoot");
    }

    public static final ModConfigSpec.BooleanValue DUNGEON_LOCK_COMBAT_DOORS = BUILDER
            .comment("Seal all connected routes while an uncleared combat encounter is active.")
            .define("lockCombatDoors", true);
    public static final ModConfigSpec.EnumValue<ArchiveLootMode> DUNGEON_LOOT_MODE = BUILDER
            .comment("INDIVIDUAL gives each party member one durable claim per cache; SHARED consumes it once.")
            .defineEnum("lootMode", ArchiveLootMode.INDIVIDUAL);

    static {
        BUILDER.pop();
        BUILDER.push("seedAndPersistence");
    }

    public static final ModConfigSpec.LongValue DUNGEON_FORCED_SEED = BUILDER
            .comment("Long.MIN_VALUE chooses a fresh derived seed. Any other value forces normal dungeon generation.")
            .defineInRange("forcedSeed", ArchiveDungeonRules.RANDOM_SEED, Long.MIN_VALUE, Long.MAX_VALUE);
    public static final ModConfigSpec.BooleanValue DUNGEON_RETAIN_COMPLETED = BUILDER
            .comment("Keep terminal run records and geometry after every member returns; false queues cleanup.")
            .define("retainCompletedRuns", false);

    static {
        BUILDER.pop();
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private YesterglassConfig() {
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
        Set<Identifier> allowed = DUNGEON_ALLOWED_TEMPLATES.get().stream()
                .map(String::valueOf)
                .map(YesterglassConfig::identifier)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<Identifier, Integer> templateWeights = parseWeights(DUNGEON_ROOM_WEIGHTS.get());
        Map<Identifier, Integer> lootWeights = parseWeights(DUNGEON_LOOT_WEIGHTS.get());
        Map<Identifier, List<ArchiveDungeonRules.EnemyWeight>> enemyPools =
                parseEnemyPools(DUNGEON_ENEMY_POOLS.get());
        Identifier boss = identifier(DUNGEON_BOSS_TEMPLATE.get());
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

    private static Map<Identifier, Integer> parseWeights(List<? extends String> entries) {
        LinkedHashMap<Identifier, Integer> result = new LinkedHashMap<>();
        for (String raw : entries) {
            int separator = raw.lastIndexOf('=');
            Identifier id = identifier(raw.substring(0, separator));
            int weight = Integer.parseInt(raw.substring(separator + 1));
            result.put(id, weight);
        }
        return Map.copyOf(result);
    }

    private static Map<Identifier, List<ArchiveDungeonRules.EnemyWeight>> parseEnemyPools(
            List<? extends String> entries) {
        LinkedHashMap<Identifier, ArrayList<ArchiveDungeonRules.EnemyWeight>> mutable = new LinkedHashMap<>();
        for (String raw : entries) {
            int groupSeparator = raw.indexOf('|');
            int weightSeparator = raw.lastIndexOf('=');
            Identifier group = identifier(raw.substring(0, groupSeparator));
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
            identifier(text);
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

    private static Identifier identifier(String value) {
        return Identifier.parse(value.trim());
    }
}
