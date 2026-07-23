package com.nightbeam.tbos.run;

import com.nightbeam.tbos.Yesterglass;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

/** Deterministic, retry-bounded 3D graph generation isolated from the world RNG. */
public final class ArchiveRunGenerator {
    private static final long ATTEMPT_SALT = 0x4152434849564553L;
    private static final long ENCOUNTER_SALT = 0x454E434F554E5445L;

    public static final Identifier PARALLAX_WAKE = id("parallax_wake");
    public static final Identifier STARLESS_GALLERY = id("starless_gallery");
    public static final Identifier LENSWRIGHT_CROSSING = id("lenswright_crossing");
    public static final Identifier ECHO_FOUNDRY = id("echo_foundry");
    public static final Identifier MERIDIAN_SENTINEL = id("meridian_sentinel");
    public static final Identifier GLASSBOUND_HUNTSMAN = id("glassbound_huntsman");
    public static final Identifier HALL_ALIGNMENT_REFORGED = id("hall_alignment_reforged");
    public static final Identifier SPLIT_WORKSHOP = id("lenswright_crossing");
    public static final Identifier CHRONICLE_STACK = id("chronicle_stack");
    public static final Identifier BRONZE_REVISER = id("bronze_reviser");
    public static final Identifier PHASE_FOUNDRY = id("echo_foundry");
    public static final Identifier CLOCKLESS_VAULT = id("sealed_reliquary");
    public static final Identifier ARCHIVIST_TRIBUNAL = id("archivist_tribunal");
    public static final Identifier MERIDIAN_ENGINE = id("meridian_sentinel");
    public static final Identifier CHOIR_OF_HOURS_REFORGED = id("choir_of_hours_reforged");
    public static final Identifier INVERTED_SCRIPTORIUM = id("annalist_vigil");
    public static final Identifier MEMORY_MAZE = id("buried_index");
    public static final Identifier RESONANCE_CATWALK = id("meridian_drop");
    public static final Identifier HOUR_CANTOR = id("hour_cantor");

    private static final List<ArchiveRoomCategory> SPECIAL_CATEGORIES = List.of(
            ArchiveRoomCategory.TREASURE,
            ArchiveRoomCategory.ANCIENT_LIBRARY,
            ArchiveRoomCategory.SANCTUARY,
            ArchiveRoomCategory.LORE,
            ArchiveRoomCategory.MERCHANT,
            ArchiveRoomCategory.CURSED,
            ArchiveRoomCategory.MINI_BOSS,
            ArchiveRoomCategory.PUZZLE);

    private ArchiveRunGenerator() {
    }

    /** Compatibility view used by older callers; the durable graph is the source of truth. */
    public static List<ArchiveRoomPlan> generate(long seed) {
        return generateDungeon(seed, ArchiveDungeonSettings.DEFAULT).roomPlans();
    }

    public static ArchiveDungeonGraph generateDungeon(long seed, ArchiveDungeonSettings settings) {
        return generateDetailed(seed, settings).graph();
    }

    public static GenerationResult generateDetailed(long seed, ArchiveDungeonSettings settings) {
        if (!ArchiveRoomTemplates.validateAll().isEmpty()) {
            throw new IllegalStateException("The built-in archive room template catalog is invalid: "
                    + ArchiveRoomTemplates.validateAll());
        }
        validateConfiguredTemplates(settings);
        long started = System.nanoTime();
        ArrayList<String> rejected = new ArrayList<>();
        for (int attempt = 0; attempt < settings.generationAttempts(); attempt++) {
            try {
                ArchiveDungeonGraph graph = generateAttempt(seed, settings, attempt);
                long elapsed = System.nanoTime() - started;
                return new GenerationResult(
                        graph,
                        new GenerationMetrics(
                                graph.rooms().size(),
                                graph.branchCount(),
                                graph.verticalRoomCount(),
                                graph.loopCount(),
                                graph.unreachableRoomCount(),
                                graph.overlapCount(),
                                attempt,
                                elapsed),
                        List.copyOf(rejected));
            } catch (RejectedGeneration exception) {
                if (rejected.size() < 64) {
                    rejected.add("attempt " + attempt + ": " + exception.getMessage());
                }
            }
        }
        throw new IllegalStateException("Archive dungeon generation exhausted " + settings.generationAttempts()
                + " attempts for seed " + seed + ": " + rejected);
    }

    public static long encounterSeedFor(long dungeonSeed, int roomIndex, int templateSalt) {
        return mix64(dungeonSeed ^ ENCOUNTER_SALT
                ^ (0x9E3779B97F4A7C15L * (roomIndex + 1L)) ^ templateSalt);
    }

    public static ArchiveEncounterKind kindFor(Identifier roomId, int slot) {
        return ArchiveRoomTemplates.find(roomId)
                .map(template -> syntheticNode(template, slot).encounterKind())
                .orElse(slot == 0 ? ArchiveEncounterKind.EXPLORATION : ArchiveEncounterKind.SKIRMISH);
    }

    private static ArchiveDungeonGraph generateAttempt(
            long seed, ArchiveDungeonSettings settings, int attempt) throws RejectedGeneration {
        RandomSource random = RandomSource.create(mix64(seed ^ ATTEMPT_SALT ^ attempt));
        int targetRooms = settings.minimumRooms()
                + random.nextInt(settings.maximumRooms() - settings.minimumRooms() + 1);
        ArrayList<MutableNode> nodes = new ArrayList<>();
        Map<ArchiveGridPos, Integer> occupied = new HashMap<>();
        ArchiveRoomTemplate startTemplate = singleTemplate(ArchiveRoomCategory.STARTING, settings);
        MutableNode start = new MutableNode(
                0,
                startTemplate,
                new ArchiveGridPos(0, 0, 0),
                ArchiveTransform.IDENTITY,
                0);
        nodes.add(start);
        occupied.put(start.position, 0);

        int regularTarget = targetRooms - 2;
        int lesserBossTarget = lesserBossCountFor(targetRooms);
        int lesserBosses = 0;
        while (nodes.size() < regularTarget) {
            List<Frontier> frontiers = collectFrontiers(nodes, occupied, settings);
            if (frontiers.isEmpty()) {
                throw new RejectedGeneration("no legal frontier remains at " + nodes.size() + "/" + targetRooms);
            }
            int remainingRegularRooms = regularTarget - nodes.size();
            boolean forceLesserBoss = lesserBossTarget - lesserBosses >= remainingRegularRooms;
            if (forceLesserBoss) {
                frontiers = frontiers.stream()
                        .filter(frontier -> !frontier.direction.vertical())
                        .toList();
                if (frontiers.isEmpty()) {
                    throw new RejectedGeneration("no horizontal frontier remains for a guaranteed lesser boss");
                }
            }
            Frontier frontier = weightedFrontier(frontiers, nodes, settings, random);
            ArchiveRoomCategory category = forceLesserBoss
                    ? ArchiveRoomCategory.MINI_BOSS
                    : chooseCategory(frontier.direction, settings, random);
            if (category == ArchiveRoomCategory.MINI_BOSS && lesserBosses >= lesserBossTarget) {
                category = ArchiveRoomCategory.ELITE_COMBAT;
            }
            ArchiveRoomTemplate template = chooseTemplate(
                    category, frontier.direction.opposite(), settings, random);
            ArchiveTransform transform = randomTransform(template, random);
            if (!template.supports(frontier.direction.opposite(), transform)) {
                template = chooseTemplateSupporting(
                        category, frontier.direction.opposite(), transform, settings, random);
            }
            int index = nodes.size();
            MutableNode node = new MutableNode(index, template, frontier.position, transform,
                    nodes.get(frontier.parent).depth + 1);
            boolean hidden = category == ArchiveRoomCategory.SECRET;
            connect(nodes.get(frontier.parent), node, frontier.direction, hidden);
            nodes.add(node);
            occupied.put(node.position, index);
            if (category == ArchiveRoomCategory.MINI_BOSS) {
                lesserBosses++;
            }
        }

        int bossParent = chooseBossParent(nodes, occupied, settings, random);
        Frontier bossFrontier = chooseMandatoryFrontier(nodes.get(bossParent), occupied, settings, true, random);
        ArchiveRoomTemplate bossTemplate = ArchiveRoomTemplates.find(settings.rules().bossTemplate())
                .filter(template -> template.category() == ArchiveRoomCategory.FINAL_BOSS)
                .filter(template -> settings.rules().allows(template.id()))
                .orElseThrow(() -> new RejectedGeneration(
                        "configured boss template is unavailable: " + settings.rules().bossTemplate()));
        MutableNode boss = new MutableNode(
                nodes.size(),
                bossTemplate,
                bossFrontier.position,
                randomTransform(bossTemplate, random),
                nodes.get(bossParent).depth + 1);
        connect(nodes.get(bossParent), boss, bossFrontier.direction, false);
        lockConnection(nodes.get(bossParent), boss, bossFrontier.direction);
        nodes.add(boss);
        occupied.put(boss.position, boss.index);

        Frontier rewardFrontier = chooseMandatoryFrontier(boss, occupied, settings, false, random);
        ArchiveRoomTemplate rewardTemplate = singleTemplate(ArchiveRoomCategory.EXIT_REWARD, settings);
        MutableNode reward = new MutableNode(
                nodes.size(),
                rewardTemplate,
                rewardFrontier.position,
                randomTransform(rewardTemplate, random),
                boss.depth + 1);
        connect(boss, reward, rewardFrontier.direction, false);
        lockConnection(boss, reward, rewardFrontier.direction);
        nodes.add(reward);
        occupied.put(reward.position, reward.index);

        addLoops(nodes, occupied, settings, random);
        List<Integer> shortestDepths = shortestDepths(nodes);
        ArrayList<ArchiveRoomNode> immutable = new ArrayList<>(nodes.size());
        for (MutableNode node : nodes) {
            int depth = shortestDepths.get(node.index);
            int difficulty = Math.min(100, 1 + depth * 3);
            List<ArchiveRoomModifier> modifiers =
                    chooseModifiers(node.template.category(), difficulty, settings, random);
            ArchiveRoomRuntimeState runtime = node.index == 0
                    ? new ArchiveRoomRuntimeState(true, false, true, false, List.of(), false)
                    : node.index == reward.index || node.index == boss.index
                            ? ArchiveRoomRuntimeState.UNVISITED.withDoorsLocked(true)
                    : ArchiveRoomRuntimeState.UNVISITED;
            immutable.add(new ArchiveRoomNode(
                    node.index,
                    node.template.id(),
                    node.template.category(),
                    new ArchiveRoomPlacement(node.position, node.template.size(), node.transform),
                    List.copyOf(node.connections),
                    depth,
                    difficulty,
                    node.template.lootTables(),
                    node.template.monsterGroups(),
                    modifiers,
                    runtime));
        }
        return new ArchiveDungeonGraph(
                ArchiveDungeonGraph.SCHEMA_REVISION,
                seed,
                immutable,
                0,
                boss.index,
                reward.index);
    }

    public static int lesserBossCountFor(int totalRooms) {
        if (totalRooms < 7 || totalRooms > 48) {
            throw new IllegalArgumentException("Archive room count must be between 7 and 48");
        }
        if (totalRooms >= 24) {
            return 3;
        }
        if (totalRooms >= 12) {
            return 2;
        }
        return 1;
    }

    private static List<Frontier> collectFrontiers(
            List<MutableNode> nodes,
            Map<ArchiveGridPos, Integer> occupied,
            ArchiveDungeonSettings settings) {
        int above = (int) nodes.stream().filter(node -> node.position.y() > 0).count();
        int below = (int) nodes.stream().filter(node -> node.position.y() < 0).count();
        ArrayList<Frontier> result = new ArrayList<>();
        for (MutableNode node : nodes) {
            if (node.template.category() == ArchiveRoomCategory.SECRET || node.connections.size() >= 5) {
                continue;
            }
            for (ArchiveDirection direction : ArchiveDirection.values()) {
                if (node.connection(direction) != null || !node.template.supports(direction, node.transform)) {
                    continue;
                }
                ArchiveGridPos position = node.position.offset(direction);
                if (occupied.containsKey(position) || !withinBounds(position, settings)) {
                    continue;
                }
                if (position.y() > 0 && above >= settings.maximumRoomsAbove()
                        || position.y() < 0 && below >= settings.maximumRoomsBelow()) {
                    continue;
                }
                if (node.depth + 1 > settings.maximumGraphDepth() - 2) {
                    continue;
                }
                result.add(new Frontier(node.index, direction, position));
            }
        }
        return result;
    }

    private static Frontier weightedFrontier(
            List<Frontier> frontiers,
            List<MutableNode> nodes,
            ArchiveDungeonSettings settings,
            RandomSource random) {
        double total = 0.0D;
        double[] weights = new double[frontiers.size()];
        for (int index = 0; index < frontiers.size(); index++) {
            int degree = nodes.get(frontiers.get(index).parent).connections.size();
            double branchWeight = degree >= 2 ? Math.max(0.05D, settings.branchingProbability()) : 1.0D;
            double deadEndWeight = degree == 1 ? Math.max(0.05D, 1.0D - settings.deadEndProbability()) : 1.0D;
            double verticalWeight = frontiers.get(index).direction.vertical() ? 0.45D : 1.0D;
            weights[index] = branchWeight * deadEndWeight * verticalWeight;
            total += weights[index];
        }
        double choice = random.nextDouble() * total;
        for (int index = 0; index < frontiers.size(); index++) {
            choice -= weights[index];
            if (choice <= 0.0D) {
                return frontiers.get(index);
            }
        }
        return frontiers.getLast();
    }

    private static ArchiveRoomCategory chooseCategory(
            ArchiveDirection direction, ArchiveDungeonSettings settings, RandomSource random) {
        if (direction.vertical()) {
            return random.nextDouble() < 0.72D
                    ? ArchiveRoomCategory.VERTICAL_SHAFT
                    : ArchiveRoomCategory.STANDARD_COMBAT;
        }
        if (random.nextDouble() < settings.secretRoomProbability()) {
            return ArchiveRoomCategory.SECRET;
        }
        if (random.nextDouble() < settings.specialRoomFrequency()) {
            return SPECIAL_CATEGORIES.get(random.nextInt(SPECIAL_CATEGORIES.size()));
        }
        if (random.nextDouble() < settings.rules().trapRoomProbability()) {
            return ArchiveRoomCategory.TRAP;
        }
        double combatRoll = random.nextDouble();
        if (combatRoll < 0.68D) {
            return ArchiveRoomCategory.STANDARD_COMBAT;
        }
        if (combatRoll < 0.90D) {
            return ArchiveRoomCategory.ELITE_COMBAT;
        }
        return ArchiveRoomCategory.PUZZLE;
    }

    private static ArchiveRoomTemplate chooseTemplate(
            ArchiveRoomCategory category,
            ArchiveDirection requiredDoor,
            ArchiveDungeonSettings settings,
            RandomSource random) {
        return chooseTemplateSupporting(
                category, requiredDoor, ArchiveTransform.IDENTITY, settings, random);
    }

    private static ArchiveRoomTemplate chooseTemplateSupporting(
            ArchiveRoomCategory category,
            ArchiveDirection requiredDoor,
            ArchiveTransform transform,
            ArchiveDungeonSettings settings,
            RandomSource random) {
        List<ArchiveRoomTemplate> candidates = ArchiveRoomTemplates.forCategory(category).stream()
                .filter(template -> settings.rules().allows(template.id()))
                .filter(template -> template.supports(requiredDoor, transform))
                .toList();
        if (candidates.isEmpty()) {
            throw new RejectedGeneration("no " + category + " template supports " + requiredDoor);
        }
        int totalWeight = candidates.stream().mapToInt(settings.rules()::templateWeight).sum();
        int choice = random.nextInt(totalWeight);
        for (ArchiveRoomTemplate candidate : candidates) {
            choice -= settings.rules().templateWeight(candidate);
            if (choice < 0) {
                return candidate;
            }
        }
        return candidates.getLast();
    }

    private static int chooseBossParent(
            List<MutableNode> nodes,
            Map<ArchiveGridPos, Integer> occupied,
            ArchiveDungeonSettings settings,
            RandomSource random) {
        List<MutableNode> candidates = nodes.stream()
                .filter(node -> node.index != 0 && node.template.category() != ArchiveRoomCategory.SECRET)
                // The mandatory two-room tail is horizontal. Anchor it to the
                // starting floor so boss/reward rooms cannot consume extra
                // above/below slots after regular expansion reached its cap.
                .filter(node -> node.position.y() == 0)
                .filter(node -> node.depth <= settings.maximumGraphDepth() - 2)
                .filter(node -> hasTwoStepExtension(node, occupied, settings))
                .sorted(Comparator.comparingInt((MutableNode node) -> node.depth).reversed())
                .toList();
        if (candidates.isEmpty()) {
            throw new RejectedGeneration("no far room can host the boss and reward chain");
        }
        int farthestDepth = candidates.getFirst().depth;
        List<MutableNode> farthest = candidates.stream()
                .filter(node -> node.depth >= farthestDepth - 1)
                .toList();
        return farthest.get(random.nextInt(farthest.size())).index;
    }

    private static boolean hasTwoStepExtension(
            MutableNode node, Map<ArchiveGridPos, Integer> occupied, ArchiveDungeonSettings settings) {
        for (ArchiveDirection first : ArchiveDirection.values()) {
            ArchiveGridPos boss = node.position.offset(first);
            if (node.connection(first) != null || occupied.containsKey(boss) || !withinBounds(boss, settings)) {
                continue;
            }
            for (ArchiveDirection second : ArchiveDirection.values()) {
                ArchiveGridPos reward = boss.offset(second);
                if (second == first.opposite() || occupied.containsKey(reward) || !withinBounds(reward, settings)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private static Frontier chooseMandatoryFrontier(
            MutableNode parent,
            Map<ArchiveGridPos, Integer> occupied,
            ArchiveDungeonSettings settings,
            boolean requireContinuation,
            RandomSource random) {
        ArrayList<Frontier> candidates = new ArrayList<>();
        for (ArchiveDirection direction : ArchiveDirection.values()) {
            // Keep the guaranteed boss/reward tail horizontal. Regular expansion
            // already owns the configured above/below counts, so mandatory rooms
            // cannot silently exceed those limits.
            if (direction.vertical()) {
                continue;
            }
            ArchiveGridPos position = parent.position.offset(direction);
            if (parent.connection(direction) != null || occupied.containsKey(position) || !withinBounds(position, settings)) {
                continue;
            }
            if (requireContinuation) {
                boolean continuation = false;
                for (ArchiveDirection next : ArchiveDirection.values()) {
                    ArchiveGridPos nextPosition = position.offset(next);
                    if (next != direction.opposite()
                            && !occupied.containsKey(nextPosition)
                            && withinBounds(nextPosition, settings)) {
                        continuation = true;
                        break;
                    }
                }
                if (!continuation) {
                    continue;
                }
            }
            candidates.add(new Frontier(parent.index, direction, position));
        }
        if (candidates.isEmpty()) {
            throw new RejectedGeneration("mandatory room chain has no legal connection after room " + parent.index);
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    private static void addLoops(
            List<MutableNode> nodes,
            Map<ArchiveGridPos, Integer> occupied,
            ArchiveDungeonSettings settings,
            RandomSource random) {
        for (MutableNode node : nodes) {
            if (node.template.category() == ArchiveRoomCategory.EXIT_REWARD
                    || node.template.category() == ArchiveRoomCategory.FINAL_BOSS) {
                continue;
            }
            for (ArchiveDirection direction : ArchiveDirection.values()) {
                Integer otherIndex = occupied.get(node.position.offset(direction));
                if (otherIndex == null || otherIndex <= node.index || node.connection(direction) != null) {
                    continue;
                }
                MutableNode other = nodes.get(otherIndex);
                if (other.template.category() == ArchiveRoomCategory.EXIT_REWARD
                        || other.template.category() == ArchiveRoomCategory.FINAL_BOSS
                        || !node.template.supports(direction, node.transform)
                        || !other.template.supports(direction.opposite(), other.transform)
                        || random.nextDouble() >= settings.loopProbability()) {
                    continue;
                }
                boolean hidden = node.template.category() == ArchiveRoomCategory.SECRET
                        || other.template.category() == ArchiveRoomCategory.SECRET;
                connect(node, other, direction, hidden);
            }
        }
    }

    private static List<Integer> shortestDepths(List<MutableNode> nodes) {
        ArrayList<Integer> depths = new ArrayList<>(java.util.Collections.nCopies(nodes.size(), Integer.MAX_VALUE));
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        depths.set(0, 0);
        queue.add(0);
        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            int nextDepth = depths.get(current) + 1;
            for (ArchiveConnection connection : nodes.get(current).connections) {
                if (nextDepth < depths.get(connection.targetRoom())) {
                    depths.set(connection.targetRoom(), nextDepth);
                    queue.addLast(connection.targetRoom());
                }
            }
        }
        return List.copyOf(depths);
    }

    private static List<ArchiveRoomModifier> chooseModifiers(
            ArchiveRoomCategory category,
            int difficulty,
            ArchiveDungeonSettings settings,
            RandomSource random) {
        EnumSet<ArchiveRoomModifier> modifiers = EnumSet.noneOf(ArchiveRoomModifier.class);
        double chance = Math.min(0.75D,
                settings.rules().roomModifierChance() + Math.max(0, difficulty - 1) * 0.012D);
        if (random.nextDouble() < chance) {
            ArchiveRoomModifier[] values = ArchiveRoomModifier.values();
            modifiers.add(values[random.nextInt(values.length)]);
        }
        if (difficulty >= 25 && random.nextDouble() < chance * 0.35D) {
            ArchiveRoomModifier[] values = ArchiveRoomModifier.values();
            modifiers.add(values[random.nextInt(values.length)]);
        }
        if (category == ArchiveRoomCategory.CURSED) {
            modifiers.add(ArchiveRoomModifier.ANCIENT_CURSE);
        }
        return List.copyOf(modifiers);
    }

    private static ArchiveTransform randomTransform(ArchiveRoomTemplate template, RandomSource random) {
        return new ArchiveTransform(
                template.rotationSafe() ? random.nextInt(4) : 0,
                template.mirrorSafe() && random.nextBoolean());
    }

    private static boolean withinBounds(ArchiveGridPos position, ArchiveDungeonSettings settings) {
        return Math.abs(position.x()) <= settings.horizontalLimit()
                && Math.abs(position.z()) <= settings.horizontalLimit()
                && Math.abs(position.y()) <= settings.verticalLimit();
    }

    private static void connect(
            MutableNode source, MutableNode target, ArchiveDirection direction, boolean hidden) {
        if (source.connection(direction) != null || target.connection(direction.opposite()) != null) {
            throw new RejectedGeneration("attempted to overwrite an archive door connection");
        }
        source.connections.add(new ArchiveConnection(target.index, direction, hidden, false));
        target.connections.add(new ArchiveConnection(source.index, direction.opposite(), hidden, false));
    }

    private static void lockConnection(
            MutableNode source, MutableNode target, ArchiveDirection direction) {
        for (int index = 0; index < source.connections.size(); index++) {
            ArchiveConnection connection = source.connections.get(index);
            if (connection.targetRoom() == target.index && connection.direction() == direction) {
                source.connections.set(index, connection.withLocked(true));
            }
        }
        for (int index = 0; index < target.connections.size(); index++) {
            ArchiveConnection connection = target.connections.get(index);
            if (connection.targetRoom() == source.index && connection.direction() == direction.opposite()) {
                target.connections.set(index, connection.withLocked(true));
            }
        }
    }

    private static ArchiveRoomTemplate singleTemplate(
            ArchiveRoomCategory category, ArchiveDungeonSettings settings) {
        List<ArchiveRoomTemplate> templates = ArchiveRoomTemplates.forCategory(category).stream()
                .filter(template -> settings.rules().allows(template.id()))
                .toList();
        if (templates.isEmpty()) {
            throw new RejectedGeneration("missing mandatory template category " + category);
        }
        return templates.getFirst();
    }

    private static void validateConfiguredTemplates(ArchiveDungeonSettings settings) {
        if (!settings.rules().allowedTemplates().isEmpty()) {
            Set<Identifier> known = ArchiveRoomTemplates.all().stream()
                    .map(ArchiveRoomTemplate::id)
                    .collect(java.util.stream.Collectors.toSet());
            Set<Identifier> unknown = settings.rules().allowedTemplates().stream()
                    .filter(id -> !known.contains(id))
                    .collect(java.util.stream.Collectors.toSet());
            if (!unknown.isEmpty()) {
                throw new IllegalArgumentException("Unknown configured archive templates: " + unknown);
            }
        }
        for (ArchiveRoomCategory mandatory : List.of(
                ArchiveRoomCategory.STARTING,
                ArchiveRoomCategory.FINAL_BOSS,
                ArchiveRoomCategory.EXIT_REWARD)) {
            if (ArchiveRoomTemplates.forCategory(mandatory).stream()
                    .noneMatch(template -> settings.rules().allows(template.id()))) {
                throw new IllegalArgumentException(
                        "Archive template allowlist excludes mandatory category " + mandatory);
            }
        }
    }

    private static ArchiveRoomNode syntheticNode(ArchiveRoomTemplate template, int slot) {
        return new ArchiveRoomNode(
                Math.max(0, slot),
                template.id(),
                template.category(),
                new ArchiveRoomPlacement(new ArchiveGridPos(0, 0, Math.max(0, slot)), template.size(), ArchiveTransform.IDENTITY),
                List.of(),
                Math.max(0, slot),
                1,
                template.lootTables(),
                template.monsterGroups(),
                List.of(),
                ArchiveRoomRuntimeState.UNVISITED);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, path);
    }

    public record GenerationMetrics(
            int roomCount,
            int branchCount,
            int verticalRoomCount,
            int loopCount,
            int unreachableRoomCount,
            int overlapCount,
            int rejectedAttempts,
            long generationNanos) {
    }

    public record GenerationResult(
            ArchiveDungeonGraph graph,
            GenerationMetrics metrics,
            List<String> rejectedPlacements) {
    }

    private record Frontier(int parent, ArchiveDirection direction, ArchiveGridPos position) {
    }

    private static final class MutableNode {
        private final int index;
        private final ArchiveRoomTemplate template;
        private final ArchiveGridPos position;
        private final ArchiveTransform transform;
        private final int depth;
        private final ArrayList<ArchiveConnection> connections = new ArrayList<>();

        private MutableNode(
                int index,
                ArchiveRoomTemplate template,
                ArchiveGridPos position,
                ArchiveTransform transform,
                int depth) {
            this.index = index;
            this.template = template;
            this.position = position;
            this.transform = transform;
            this.depth = depth;
        }

        private ArchiveConnection connection(ArchiveDirection direction) {
            return connections.stream()
                    .filter(connection -> connection.direction() == direction)
                    .findFirst()
                    .orElse(null);
        }
    }

    private static final class RejectedGeneration extends RuntimeException {
        private RejectedGeneration(String message) {
            super(message);
        }
    }
}
