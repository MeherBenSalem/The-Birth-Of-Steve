package com.nightbeam.tbos.run;

import com.nightbeam.tbos.Yesterglass;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

/**
 * Content and runtime rules paired with the numeric graph limits. Empty template
 * and weight maps mean "use the built-in catalog defaults".
 */
public record ArchiveDungeonRules(
        Set<Identifier> allowedTemplates,
        Map<Identifier, Integer> templateWeights,
        Map<Identifier, Integer> lootTableWeights,
        Map<Identifier, List<EnemyWeight>> enemyPools,
        Identifier bossTemplate,
        double trapRoomProbability,
        double roomModifierChance,
        double directLootProbability,
        double healthPerDifficulty,
        double damagePerDifficulty,
        double healthPerAdditionalPlayer,
        double enemiesPerAdditionalPlayer,
        boolean lockCombatDoors,
        long forcedSeed,
        ArchiveLootMode lootMode,
        boolean retainCompletedRuns) {
    public static final long RANDOM_SEED = Long.MIN_VALUE;

    public static final Identifier FORGOTTEN_LEGION = id("encounters/forgotten_legion");
    public static final Identifier ELITE_ECHOES = id("encounters/elite_echoes");
    public static final Identifier RUINED_GUARDIAN = id("encounters/ruined_guardian");
    public static final Identifier HOUR_CANTOR_POOL = id("encounters/hour_cantor");
    public static final Identifier DEFAULT_BOSS = id("hour_cantor");

    public static final ArchiveDungeonRules DEFAULT = new ArchiveDungeonRules(
            Set.of(),
            Map.of(),
            Map.of(
                    id("loot/common"), 8,
                    id("loot/secret"), 4,
                    id("loot/lesser_boss"), 2,
                    id("loot/boss_reward"), 1),
            Map.of(
                    FORGOTTEN_LEGION, List.of(
                            new EnemyWeight(ArchiveEnemyKind.PARALLAX_WRAITH, 4),
                            new EnemyWeight(ArchiveEnemyKind.HUSK, 4),
                            new EnemyWeight(ArchiveEnemyKind.SKELETON, 3),
                            new EnemyWeight(ArchiveEnemyKind.STRAY, 2),
                            new EnemyWeight(ArchiveEnemyKind.CAVE_SPIDER, 2),
                            new EnemyWeight(ArchiveEnemyKind.SILVERFISH, 2),
                            new EnemyWeight(ArchiveEnemyKind.MERIDIAN_SENTINEL, 1),
                            new EnemyWeight(ArchiveEnemyKind.MEMORY_LEECH, 2)),
                    ELITE_ECHOES, List.of(
                            new EnemyWeight(ArchiveEnemyKind.PARALLAX_WRAITH, 2),
                            new EnemyWeight(ArchiveEnemyKind.MERIDIAN_SENTINEL, 4),
                            new EnemyWeight(ArchiveEnemyKind.MEMORY_LEECH, 3),
                            new EnemyWeight(ArchiveEnemyKind.STRAY, 2),
                            new EnemyWeight(ArchiveEnemyKind.VINDICATOR, 3),
                            new EnemyWeight(ArchiveEnemyKind.EVOKER, 1)),
                    RUINED_GUARDIAN, List.of(
                            new EnemyWeight(ArchiveEnemyKind.MERIDIAN_SENTINEL, 4),
                            new EnemyWeight(ArchiveEnemyKind.VINDICATOR, 3),
                            new EnemyWeight(ArchiveEnemyKind.EVOKER, 2),
                            new EnemyWeight(ArchiveEnemyKind.RAVAGER, 1),
                            new EnemyWeight(ArchiveEnemyKind.PARALLAX_WRAITH, 1)),
                    HOUR_CANTOR_POOL, List.of(new EnemyWeight(ArchiveEnemyKind.HOUR_CANTOR, 1))),
            DEFAULT_BOSS,
            0.10D,
            0.18D,
            0.42D,
            0.025D,
            0.015D,
            0.25D,
            1.0D,
            true,
            RANDOM_SEED,
            ArchiveLootMode.INDIVIDUAL,
            false);

    public ArchiveDungeonRules {
        allowedTemplates = Set.copyOf(Objects.requireNonNull(allowedTemplates, "allowedTemplates"));
        templateWeights = Map.copyOf(Objects.requireNonNull(templateWeights, "templateWeights"));
        lootTableWeights = Map.copyOf(Objects.requireNonNull(lootTableWeights, "lootTableWeights"));
        enemyPools = Objects.requireNonNull(enemyPools, "enemyPools").entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())));
        bossTemplate = Objects.requireNonNull(bossTemplate, "bossTemplate");
        lootMode = Objects.requireNonNull(lootMode, "lootMode");
        validateProbability("trap room", trapRoomProbability);
        validateProbability("room modifier", roomModifierChance);
        validateProbability("direct loot", directLootProbability);
        if (!Double.isFinite(healthPerDifficulty) || healthPerDifficulty < 0.0D || healthPerDifficulty > 1.0D
                || !Double.isFinite(damagePerDifficulty) || damagePerDifficulty < 0.0D || damagePerDifficulty > 1.0D
                || !Double.isFinite(healthPerAdditionalPlayer)
                || healthPerAdditionalPlayer < 0.0D || healthPerAdditionalPlayer > 4.0D
                || !Double.isFinite(enemiesPerAdditionalPlayer)
                || enemiesPerAdditionalPlayer < 0.0D || enemiesPerAdditionalPlayer > 8.0D) {
            throw new IllegalArgumentException("Archive encounter scaling rules are outside supported bounds");
        }
        if (templateWeights.values().stream().anyMatch(weight -> weight < 1 || weight > 10_000)
                || lootTableWeights.values().stream().anyMatch(weight -> weight < 1 || weight > 10_000)
                || enemyPools.values().stream().anyMatch(List::isEmpty)) {
            throw new IllegalArgumentException("Archive content weights must be nonempty and between 1 and 10000");
        }
    }

    public boolean allows(Identifier templateId) {
        return allowedTemplates.isEmpty() || allowedTemplates.contains(templateId);
    }

    public int templateWeight(ArchiveRoomTemplate template) {
        return templateWeights.getOrDefault(template.id(), template.weight());
    }

    public int lootTableWeight(Identifier table) {
        return lootTableWeights.getOrDefault(table, 1);
    }

    public List<EnemyWeight> enemyPool(Identifier group) {
        return enemyPools.getOrDefault(group, DEFAULT.enemyPools.getOrDefault(group,
                DEFAULT.enemyPools.get(FORGOTTEN_LEGION)));
    }

    public ArchiveEnemyKind chooseEnemy(Identifier group, RandomSource random) {
        List<EnemyWeight> pool = enemyPool(group);
        int total = pool.stream().mapToInt(EnemyWeight::weight).sum();
        int choice = random.nextInt(total);
        for (EnemyWeight entry : pool) {
            choice -= entry.weight();
            if (choice < 0) {
                return entry.kind();
            }
        }
        return pool.getLast().kind();
    }

    private static void validateProbability(String label, double value) {
        if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
            throw new IllegalArgumentException("Archive " + label + " probability must be between zero and one");
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, path);
    }

    public record EnemyWeight(ArchiveEnemyKind kind, int weight) {
        public EnemyWeight {
            kind = Objects.requireNonNull(kind, "kind");
            if (weight < 1 || weight > 10_000) {
                throw new IllegalArgumentException("Archive enemy weights must be between 1 and 10000");
            }
        }
    }
}
