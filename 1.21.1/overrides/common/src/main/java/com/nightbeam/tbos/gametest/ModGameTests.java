package com.nightbeam.tbos.gametest;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.compat.VanillaCompat;
import com.nightbeam.tbos.site.BuiltInTemporalSites;
import com.nightbeam.tbos.site.TemporalSite;
import com.nightbeam.tbos.site.TemporalSiteDefinition;
import com.nightbeam.tbos.site.TemporalSiteSavedData;
import com.nightbeam.tbos.site.TemporalSiteManager;
import com.nightbeam.tbos.site.TemporalState;
import com.nightbeam.tbos.site.HallAlignmentPuzzle;
import com.nightbeam.tbos.site.ChoirHoursPuzzle;
import com.nightbeam.tbos.site.BrokenMeridianPuzzle;
import com.nightbeam.tbos.site.LastCuratorEncounterTracker;
import com.nightbeam.tbos.site.LastCuratorProgress;
import com.nightbeam.tbos.site.OrreryDefinition;
import com.nightbeam.tbos.item.MemoryPlateItem;
import com.nightbeam.tbos.item.MemoryScene;
import com.nightbeam.tbos.blockentity.MemoryLanternBlockEntity;
import com.nightbeam.tbos.block.ResonantBellBlock;
import com.nightbeam.tbos.block.EngravedMeridianTileBlock;
import com.nightbeam.tbos.block.FractureCofferBlock;
import com.nightbeam.tbos.block.MeridianRelayBlock;
import com.nightbeam.tbos.entity.HourCantorEntity;
import com.nightbeam.tbos.entity.LenswardEntity;
import com.nightbeam.tbos.entity.MemoryLeechEntity;
import com.nightbeam.tbos.entity.MeridianSentinelEntity;
import com.nightbeam.tbos.entity.ParallaxWraithEntity;
import com.nightbeam.tbos.entity.ThemeExclusiveEntity;
import com.nightbeam.tbos.registry.ModBlocks;
import com.nightbeam.tbos.registry.ModEntities;
import com.nightbeam.tbos.registry.ModItems;
import com.nightbeam.tbos.network.payload.SiteSnapshotPayload;
import com.nightbeam.tbos.run.ArchiveDimensions;
import com.nightbeam.tbos.run.ArchiveDungeonGraph;
import com.nightbeam.tbos.run.ArchiveDungeonRules;
import com.nightbeam.tbos.run.ArchiveDungeonSettings;
import com.nightbeam.tbos.run.ArchiveEnemyKind;
import com.nightbeam.tbos.run.ArchiveEnemyAbility;
import com.nightbeam.tbos.run.ArchiveEnemyDropKind;
import com.nightbeam.tbos.run.ArchiveLootRoller;
import com.nightbeam.tbos.run.ArchiveQuestProgress;
import com.nightbeam.tbos.run.ArchiveRoomCategory;
import com.nightbeam.tbos.run.ArchiveRoomNode;
import com.nightbeam.tbos.run.ArchiveRoomTemplate;
import com.nightbeam.tbos.run.ArchiveRoomTemplates;
import com.nightbeam.tbos.run.ArchiveEncounterKind;
import com.nightbeam.tbos.run.ArchiveEncounterManager;
import com.nightbeam.tbos.run.ArchiveEncounterState;
import com.nightbeam.tbos.run.ArchiveInstanceLayout;
import com.nightbeam.tbos.run.ArchiveGenerationQueue;
import com.nightbeam.tbos.run.ArchiveRoomPlacer;
import com.nightbeam.tbos.run.ArchiveReturnPoint;
import com.nightbeam.tbos.run.ArchiveRoomPlan;
import com.nightbeam.tbos.run.ArchiveRun;
import com.nightbeam.tbos.run.ArchiveRunGenerator;
import com.nightbeam.tbos.run.ArchiveRunManager;
import com.nightbeam.tbos.run.ArchiveRunMode;
import com.nightbeam.tbos.run.ArchiveRunMember;
import com.nightbeam.tbos.run.ArchiveRunProtection;
import com.nightbeam.tbos.run.ArchiveRunSavedData;
import com.nightbeam.tbos.run.ArchiveRunStatus;
import com.nightbeam.tbos.run.ArchiveFloorPresentation;
import com.nightbeam.tbos.run.ArchiveFloorTheme;
import com.nightbeam.tbos.world.AdventureWorldManager;
import com.nightbeam.tbos.world.FractureShrinePlacement;
import com.nightbeam.tbos.world.FractureShrinePlan;
import com.nightbeam.tbos.world.FractureShrineVariant;
import com.nightbeam.tbos.blockentity.AlignmentDialBlockEntity;
import com.nightbeam.tbos.blockentity.ArchiveCoreBlockEntity;
import com.nightbeam.tbos.block.AlignmentDialBlock;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The mod's GameTest bodies.
 *
 * <p>The 26.x targets register these through Minecraft's data-driven
 * TestFunction registry. Minecraft 1.21.1 predates that registry, so target
 * loader adapters invoke the same bodies through the annotation-based GameTest
 * system instead.
 */
public final class ModGameTests {
    private ModGameTests() {
    }

    /**
     * The 1.21.1 test API has no {@code Registries.TEST_FUNCTION}: it discovers
     * annotated methods instead. Loader-local adapters retain all of these
     * bodies and register them through Fabric's entrypoint and NeoForge's
     * {@code @GameTestHolder}, respectively.
     */
    public static void register() {
        // The adapters are discovered by their loaders; this keeps common init
        // idempotent without pretending that a TestFunction registry exists.
    }

    public static void fracturedArchiveDimensionLoads(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var resources = server.getResourceManager();
        helper.assertTrue(
                resources.getResource(ResourceLocation.fromNamespaceAndPath(
                                Yesterglass.MOD_ID, "dimension/fractured_archive.json"))
                        .isPresent(),
                "The fractured archive dimension definition is missing from server resources");
        helper.assertTrue(
                resources.getResource(ResourceLocation.fromNamespaceAndPath(
                                Yesterglass.MOD_ID, "dimension_type/fractured_archive.json"))
                        .isPresent(),
                "The fractured archive dimension type is missing from server resources");
        helper.assertTrue(
                net.minecraft.core.registries.Registries.levelToLevelStem(ArchiveDimensions.FRACTURED_ARCHIVE)
                        .equals(ArchiveDimensions.FRACTURED_ARCHIVE_STEM),
                "The archive level and level-stem keys do not correspond");
        var archive = server.getLevel(ArchiveDimensions.FRACTURED_ARCHIVE);
        if (archive != null) {
            helper.assertTrue(
                    archive.dimensionTypeRegistration().is(ArchiveDimensions.FRACTURED_ARCHIVE_TYPE),
                    "The live archive level loaded with the wrong dimension type");
        }
        helper.succeed();
    }

    public static void archiveRunCodecRoundTrip(GameTestHelper helper) {
        UUID firstPlayer = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondPlayer = UUID.fromString("00000000-0000-0000-0000-000000000002");
        ArchiveReturnPoint firstReturn = new ArchiveReturnPoint(
                ResourceLocation.withDefaultNamespace("overworld"), new BlockPos(12, 70, -8), 45.0F, 10.0F);
        ArchiveReturnPoint secondReturn = new ArchiveReturnPoint(
                ResourceLocation.withDefaultNamespace("overworld"), new BlockPos(14, 70, -8), 90.0F, 0.0F);
        List<ArchiveRunMember> members = List.of(
                new ArchiveRunMember(firstPlayer, firstReturn),
                new ArchiveRunMember(secondPlayer, secondReturn));
        List<ArchiveRoomPlan> rooms = java.util.stream.IntStream.range(0, 9)
                .mapToObj(index -> new ArchiveRoomPlan(
                        ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "test_room_" + index),
                        Math.min(2, index / 3),
                        index,
                        1000L + index))
                .toList();
        ArchiveRun expected = new ArchiveRun(
                ArchiveRun.SCHEMA_REVISION,
                UUID.fromString("10000000-0000-0000-0000-000000000001"),
                918273645L,
                4,
                members,
                rooms,
                2,
                2,
                3,
                ArchiveRunStatus.ACTIVE,
                -1L,
                true);

        com.google.gson.JsonElement encoded = ArchiveRun.CODEC.encodeStart(JsonOps.INSTANCE, expected).getOrThrow();
        ArchiveRun decoded = ArchiveRun.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        helper.assertTrue(decoded.equals(expected), "Archive run codec changed durable state");
        boolean emptyPartyRejected = false;
        try {
            new ArchiveRun(
                    ArchiveRun.SCHEMA_REVISION,
                    UUID.randomUUID(),
                    1L,
                    0,
                    List.of(),
                    rooms,
                    0,
                    0,
                    3,
                    ArchiveRunStatus.PREPARING,
                    -1L,
                    false);
        } catch (IllegalArgumentException exception) {
            emptyPartyRejected = true;
        }
        helper.assertTrue(emptyPartyRejected, "Archive run accepted an empty party");
        boolean shortLayoutRejected = false;
        try {
            new ArchiveRun(
                    ArchiveRun.SCHEMA_REVISION,
                    UUID.randomUUID(),
                    2L,
                    0,
                    members,
                    rooms.subList(0, 6),
                    0,
                    0,
                    3,
                    ArchiveRunStatus.PREPARING,
                    -1L,
                    false);
        } catch (IllegalArgumentException exception) {
            shortLayoutRejected = true;
        }
        helper.assertTrue(shortLayoutRejected, "Archive run accepted fewer than seven rooms");
        helper.succeed();
    }

    public static void archiveRunStorageIndexesActiveRuns(GameTestHelper helper) {
        ArchiveRunSavedData data = new ArchiveRunSavedData();
        UUID firstPlayer = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID secondPlayer = UUID.fromString("00000000-0000-0000-0000-000000000012");
        ArchiveRun first = testArchiveRun(
                UUID.fromString("10000000-0000-0000-0000-000000000011"), 0, firstPlayer);

        ArchiveRun firstRegistration = data.register(first);
        ArchiveRun repeatedRegistration = data.register(first);
        helper.assertTrue(firstRegistration.equals(repeatedRegistration), "Repeated registration changed the run");
        helper.assertTrue(data.size() == 1, "Repeated registration created a duplicate run");
        helper.assertTrue(data.nextFreeSlot() == 1, "Run storage did not reserve the active instance slot");
        helper.assertTrue(
                data.findByMember(firstPlayer).map(ArchiveRun::runId).orElseThrow().equals(first.runId()),
                "Run storage did not index its member");

        boolean slotConflictRejected = false;
        try {
            data.register(testArchiveRun(
                    UUID.fromString("10000000-0000-0000-0000-000000000012"), 0, secondPlayer));
        } catch (IllegalArgumentException exception) {
            slotConflictRejected = true;
        }
        helper.assertTrue(slotConflictRejected, "Run storage allowed two live runs to share a slot");

        boolean memberConflictRejected = false;
        try {
            data.register(testArchiveRun(
                    UUID.fromString("10000000-0000-0000-0000-000000000013"), 1, firstPlayer));
        } catch (IllegalArgumentException exception) {
            memberConflictRejected = true;
        }
        helper.assertTrue(memberConflictRejected, "Run storage allowed a member to join two live runs");

        ArchiveRun terminalFirst = first.markGeometryPlaced().activate().beginReturn(600L).complete();
        data.replace(terminalFirst);
        helper.assertTrue(data.nextFreeSlot() == 0, "A completed run continued to reserve its instance slot");
        helper.assertTrue(data.findByMember(firstPlayer).isEmpty(), "A completed run continued to reserve its member");

        ArchiveRun replacement = testArchiveRun(
                UUID.fromString("10000000-0000-0000-0000-000000000014"), 0, firstPlayer);
        data.register(replacement);
        helper.assertTrue(
                data.findByMember(firstPlayer).map(ArchiveRun::runId).orElseThrow().equals(replacement.runId()),
                "Replacing a terminal allocation did not rebuild the member index");
        helper.succeed();
    }

    private static ArchiveRun testArchiveRun(UUID runId, int instanceSlot, UUID memberId) {
        ArchiveReturnPoint returnPoint = new ArchiveReturnPoint(
                ResourceLocation.withDefaultNamespace("overworld"), new BlockPos(0, 72, 0), 0.0F, 0.0F);
        List<ArchiveRoomPlan> rooms = java.util.stream.IntStream.range(0, 9)
                .mapToObj(index -> new ArchiveRoomPlan(
                        ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "storage_room_" + index),
                        Math.min(2, index / 3),
                        index,
                        7000L + index))
                .toList();
        return new ArchiveRun(
                ArchiveRun.SCHEMA_REVISION,
                runId,
                123456L,
                instanceSlot,
                List.of(new ArchiveRunMember(memberId, returnPoint)),
                rooms,
                0,
                0,
                ArchiveRun.MAX_SHARED_REVIVES,
                ArchiveRunStatus.PREPARING,
                -1L,
                false);
    }

    public static void archiveRunGeneratorIsDeterministicAndVaried(GameTestHelper helper) {
        ArchiveDungeonGraph seedEleven = ArchiveRunGenerator.generateDungeon(11L, ArchiveDungeonSettings.DEFAULT);
        ArchiveDungeonGraph repeatedEleven = ArchiveRunGenerator.generateDungeon(11L, ArchiveDungeonSettings.DEFAULT);
        ArchiveDungeonGraph seedTwelve = ArchiveRunGenerator.generateDungeon(12L, ArchiveDungeonSettings.DEFAULT);
        ArchiveDungeonGraph deepFloor = ArchiveRunGenerator.generateDungeon(
                11L, 40L, ArchiveDungeonSettings.DEFAULT);
        ArchiveDungeonGraph repeatedDeepFloor = ArchiveRunGenerator.generateDungeon(
                11L, 40L, ArchiveDungeonSettings.DEFAULT);

        helper.assertTrue(seedEleven.equals(repeatedEleven), "Equal archive seeds generated different graphs");
        helper.assertTrue(!seedEleven.equals(seedTwelve), "Neighboring archive seeds generated the same graph");
        helper.assertTrue(deepFloor.equals(repeatedDeepFloor),
                "Floor-aware archive generation was not deterministic");
        helper.assertTrue(
                deepFloor.rooms().size() >= ArchiveDungeonSettings.DEFAULT.minimumRooms()
                                + ArchiveFloorPresentation.MAX_ADDITIONAL_ROOMS
                        && deepFloor.rooms().size() <= ArchiveDungeonSettings.DEFAULT.maximumRooms()
                                + ArchiveFloorPresentation.MAX_ADDITIONAL_ROOMS
                        && deepFloor.rooms().stream().allMatch(room -> room.difficulty() <= 100),
                "Late-floor room count or difficulty escaped its configured cap");
        helper.assertTrue(
                ArchiveFloorPresentation.displayFloor(0L) == 1L
                        && ArchiveFloorPresentation.displayFloor(8L) == 9L
                        && ArchiveFloorPresentation.nameIndex(0L) == 0
                        && ArchiveFloorPresentation.nameIndex(7L) == 7
                        && ArchiveFloorPresentation.nameIndex(8L) == 0
                        && ArchiveFloorPresentation.echoCycle(8L) == 2L,
                "Archive floor presentation lost one-based numbering or its eight-name cycle");
        helper.assertTrue(
                ArchiveFloorPresentation.difficultyBonus(0L) == 0
                        && ArchiveFloorPresentation.difficultyBonus(10L) == 40
                        && ArchiveFloorPresentation.difficultyBonus(Long.MAX_VALUE) == 40
                        && ArchiveFloorPresentation.additionalRooms(12L) == 6
                        && ArchiveFloorPresentation.additionalRooms(Long.MAX_VALUE) == 6
                        && ArchiveFloorPresentation.additionalEnemies(12L) == 4
                        && ArchiveFloorPresentation.additionalEnemies(Long.MAX_VALUE) == 4,
                "Endless-floor scaling did not stop at every configured cap");
        ArchiveRoomNode scalableCombat = seedEleven.rooms().stream()
                .filter(room -> room.category() != ArchiveRoomCategory.FINAL_BOSS
                        && room.category() != ArchiveRoomCategory.EXIT_REWARD
                        && !ArchiveEncounterManager.planWave(
                                room, 9988L, 1, 1, 0L, ArchiveDungeonRules.DEFAULT).isEmpty())
                .findFirst()
                .orElseThrow();
        int baseWave = ArchiveEncounterManager.planWave(
                scalableCombat, 9988L, 1, 1, 0L, ArchiveDungeonRules.DEFAULT).size();
        int cappedWave = ArchiveEncounterManager.planWave(
                scalableCombat, 9988L, 1, 1, Long.MAX_VALUE, ArchiveDungeonRules.DEFAULT).size();
        helper.assertTrue(cappedWave == Math.min(16, baseWave + 4),
                "Late-floor enemy waves did not add exactly four non-boss enemies at the cap");
        helper.assertTrue(
                seedEleven.rooms().size() >= ArchiveDungeonSettings.DEFAULT.minimumRooms()
                        && seedEleven.rooms().size() <= ArchiveDungeonSettings.DEFAULT.maximumRooms(),
                "Generated archive room count escaped its configured limits");
        helper.assertTrue(seedEleven.overlapCount() == 0, "Generated archive graph contains overlapping rooms");
        helper.assertTrue(seedEleven.unreachableRoomCount() == 0, "Generated archive graph contains unreachable rooms");
        helper.assertTrue(
                seedEleven.room(seedEleven.startingRoom()).category() == ArchiveRoomCategory.STARTING
                        && seedEleven.room(seedEleven.bossRoom()).category() == ArchiveRoomCategory.FINAL_BOSS
                        && seedEleven.room(seedEleven.rewardRoom()).category() == ArchiveRoomCategory.EXIT_REWARD,
                "Generated archive graph omitted a mandatory room category");
        helper.assertTrue(
                seedEleven.room(seedEleven.bossRoom()).graphDepth()
                        > seedEleven.rooms().stream().mapToInt(room -> room.graphDepth()).average().orElse(0.0D),
                "Final boss did not generate far from the starting room");
        helper.assertTrue(ArchiveRoomTemplates.validateAll().isEmpty(), "Built-in archive templates are invalid");
        var encoded = ArchiveDungeonGraph.CODEC.encodeStart(JsonOps.INSTANCE, seedEleven).getOrThrow();
        helper.assertTrue(
                ArchiveDungeonGraph.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(seedEleven),
                "Archive dungeon graph codec changed generated room or door state");
        for (int index = 0; index < seedEleven.rooms().size(); index++) {
            helper.assertTrue(seedEleven.room(index).index() == index, "Archive graph indices are not contiguous");
        }
        helper.succeed();
    }

    public static void archiveRoomBlueprintIsBoundedAndWalkable(GameTestHelper helper) {
        List<BlockPos> origins = java.util.stream.IntStream.range(0, ArchiveInstanceLayout.MAX_INSTANCE_SLOTS)
                .mapToObj(ArchiveInstanceLayout::originForSlot)
                .toList();
        for (int first = 0; first < origins.size(); first++) {
            BlockPos origin = origins.get(first);
            helper.assertTrue(
                    Math.abs(origin.getX()) < 29_000_000 && Math.abs(origin.getZ()) < 29_000_000,
                    "Archive instance origin is outside the safe world border");
            for (int second = first + 1; second < origins.size(); second++) {
                BlockPos other = origins.get(second);
                int separation = Math.max(
                        Math.abs(origin.getX() - other.getX()), Math.abs(origin.getZ() - other.getZ()));
                helper.assertTrue(
                        separation >= ArchiveInstanceLayout.CELL_SIZE,
                        "Archive instance origins are not cell-separated");
                helper.assertTrue(
                        !ArchiveInstanceLayout.boundsForSlot(first)
                                .intersects(ArchiveInstanceLayout.boundsForSlot(second)),
                        "Archive instance bounds overlap");
            }
        }

        UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000021");
        ArchiveReturnPoint returnPoint = new ArchiveReturnPoint(
                ResourceLocation.withDefaultNamespace("overworld"), new BlockPos(4, 80, 4), 0.0F, 0.0F);
        ArchiveDungeonSettings blueprintSettings = new ArchiveDungeonSettings(
                7, 7, 4, 1, 8, 0.45D, 0.15D, 0.05D, 1.0D,
                2, 2, 0.05D, 0.5D, 4096, 64);
        long hallSeed = java.util.stream.LongStream.range(0L, 256L)
                .filter(seed -> ArchiveRunGenerator.generateDungeon(seed, blueprintSettings)
                        .rooms().stream().anyMatch(room -> room.encounterKind() == ArchiveEncounterKind.HALL))
                .findFirst()
                .orElseThrow();
        ArchiveDungeonGraph graph = ArchiveRunGenerator.generateDungeon(hallSeed, blueprintSettings);
        ArchiveRun run = ArchiveRun.create(
                UUID.fromString("10000000-0000-0000-0000-000000000021"),
                hallSeed,
                0,
                List.of(new ArchiveRunMember(memberId, returnPoint)),
                graph);
        ArchiveRoomPlacer.Blueprint blueprint = ArchiveRoomPlacer.blueprint(run);
        java.util.Map<BlockPos, BlockState> states = blueprint.placements().stream()
                .collect(java.util.stream.Collectors.toMap(
                        ArchiveRoomPlacer.Placement::position,
                        ArchiveRoomPlacer.Placement::state));
        helper.assertTrue(!states.isEmpty(), "Archive room blueprint did not contain any blocks");
        for (ArchiveRoomPlacer.Placement placement : blueprint.placements()) {
            helper.assertTrue(
                    blueprint.bounds().isInside(placement.position()),
                    "Archive room blueprint escaped its instance bounds at " + placement.position());
            helper.assertTrue(
                    placement.state().is(ArchiveRoomPlacer.ARCHIVE_RUN_PALETTE),
                    "Archive room blueprint used an untagged block at " + placement.position());
        }
        for (int roomIndex = 0; roomIndex < run.rooms().size(); roomIndex++) {
            var bounds = ArchiveRoomPlacer.roomBounds(run, roomIndex);
            BlockPos spawnFloor = ArchiveRoomPlacer.roomSpawn(run, roomIndex).below();
            helper.assertTrue(states.containsKey(spawnFloor), "Archive room has no safe entry floor at " + spawnFloor);
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    BlockPos roof = new BlockPos(x, bounds.maxY(), z);
                    helper.assertTrue(
                            states.containsKey(roof),
                            "Archive room has no complete ceiling at " + roof);
                }
            }
            for (var connection : run.dungeonGraph().room(roomIndex).connections()) {
                for (BlockPos door : ArchiveRoomPlacer.doorPositions(run, roomIndex, connection.direction())) {
                    BlockState state = states.get(door);
                    if (connection.hidden()) {
                        helper.assertTrue(
                                state != null && state.is(ModBlocks.CRACKED_ARCHIVE_STONE.get()),
                                "Hidden archive door omitted its cracked secret wall at " + door);
                    } else if (connection.locked()) {
                        // A door into a boss room carries the Cantor Gate so the
                        // player can tell it apart before opening it; every other
                        // locked door stays an Archive Seal.
                        ArchiveRoomCategory sourceCategory =
                                run.dungeonGraph().room(roomIndex).category();
                        ArchiveRoomCategory targetCategory =
                                run.dungeonGraph().room(connection.targetRoom()).category();
                        boolean boss = sourceCategory == ArchiveRoomCategory.FINAL_BOSS
                                || sourceCategory == ArchiveRoomCategory.MINI_BOSS
                                || targetCategory == ArchiveRoomCategory.FINAL_BOSS
                                || targetCategory == ArchiveRoomCategory.MINI_BOSS;
                        helper.assertTrue(
                                state != null && state.is(boss
                                        ? ModBlocks.CANTOR_GATE.get()
                                        : ModBlocks.ARCHIVE_SEAL.get()),
                                (boss ? "Locked boss door is not an Archive Boss Gate at "
                                        : "Persistently locked archive door omitted its visible seal at ")
                                        + door);
                    } else {
                        helper.assertTrue(
                                state == null || (!state.is(ModBlocks.ARCHIVE_SEAL.get())
                                        && !state.is(ModBlocks.CANTOR_GATE.get())),
                                "Unlocked archive door generated as a sealed wall at " + door);
                    }
                }
            }
            if (run.rooms().get(roomIndex).encounterKind() == ArchiveEncounterKind.HALL) {
                for (BlockPos dial : ArchiveRoomPlacer.hallDialPositions(run, roomIndex)) {
                    helper.assertTrue(
                            states.get(dial).is(ModBlocks.ALIGNMENT_DIAL.get()),
                            "Hall omitted an interactive alignment dial at " + dial);
                }
            }
        }
        for (ArchiveRoomNode room : run.dungeonGraph().rooms()) {
            for (var connection : room.connections()) {
                if (room.index() >= connection.targetRoom()) {
                    continue;
                }
                var first = ArchiveRoomPlacer.roomBounds(run, room.index());
                var second = ArchiveRoomPlacer.roomBounds(run, connection.targetRoom());
                int floorY = Math.min(first.minY(), second.minY());
                int centerX = (first.minX() + first.maxX() + 1) / 2;
                int centerZ = (first.minZ() + first.maxZ() + 1) / 2;
                if (!connection.direction().vertical()) {
                    if (connection.direction() == com.nightbeam.tbos.run.ArchiveDirection.NORTH
                            || connection.direction() == com.nightbeam.tbos.run.ArchiveDirection.SOUTH) {
                        int passageZ = ((first.minZ() + first.maxZ()) / 2
                                + (second.minZ() + second.maxZ()) / 2) / 2;
                        helper.assertTrue(
                                states.containsKey(new BlockPos(centerX - 1, floorY, passageZ))
                                        && states.containsKey(new BlockPos(centerX, floorY, passageZ)),
                                "Horizontal archive passage has no two-wide floor");
                        helper.assertTrue(
                                states.containsKey(new BlockPos(centerX - 1, floorY + 4, passageZ))
                                        && states.containsKey(new BlockPos(centerX, floorY + 4, passageZ)),
                                "Horizontal archive passage is exposed instead of roofed");
                        helper.assertTrue(
                                states.containsKey(new BlockPos(centerX - 2, floorY + 2, passageZ))
                                        && states.containsKey(new BlockPos(centerX + 1, floorY + 2, passageZ)),
                                "Horizontal archive passage is not enclosed by full walls");
                    } else {
                        int passageX = ((first.minX() + first.maxX()) / 2
                                + (second.minX() + second.maxX()) / 2) / 2;
                        helper.assertTrue(
                                states.containsKey(new BlockPos(passageX, floorY, centerZ - 1))
                                        && states.containsKey(new BlockPos(passageX, floorY, centerZ)),
                                "Horizontal archive passage has no two-wide floor");
                        helper.assertTrue(
                                states.containsKey(new BlockPos(passageX, floorY + 4, centerZ - 1))
                                        && states.containsKey(new BlockPos(passageX, floorY + 4, centerZ)),
                                "Horizontal archive passage is exposed instead of roofed");
                        helper.assertTrue(
                                states.containsKey(new BlockPos(passageX, floorY + 2, centerZ - 2))
                                        && states.containsKey(new BlockPos(passageX, floorY + 2, centerZ + 1)),
                                "Horizontal archive passage is not enclosed by full walls");
                    }
                    continue;
                }
                int highY = Math.max(first.minY(), second.minY());
                int lowY = Math.min(first.minY(), second.minY());
                int rise = highY - lowY;
                for (int step = 0; step <= rise; step++) {
                    int stairX = centerX - 9 + Math.min(16, step);
                    int stairY = lowY + step;
                    for (int width = 0; width < 2; width++) {
                        BlockPos stair = new BlockPos(stairX, stairY, centerZ - 1 + width);
                        helper.assertTrue(
                                states.containsKey(stair)
                                        && states.get(stair).is(ModBlocks.ARCHIVE_STAIRS.get())
                                        && states.get(stair).getValue(StairBlock.FACING) == Direction.EAST,
                                "Vertical archive connection omitted its east-facing stair "
                                        + step + " at " + stair);
                        helper.assertTrue(
                                !states.containsKey(stair.above())
                                        && !states.containsKey(stair.above(2))
                                        && !states.containsKey(stair.above(3)),
                                "Vertical archive stair has blocked headroom at " + stair);
                        helper.assertTrue(
                                states.containsKey(stair.above(4)),
                                "Vertical archive stair is exposed instead of roofed at " + stair);
                    }
                    helper.assertTrue(
                            states.containsKey(new BlockPos(stairX, stairY, centerZ - 2))
                                    && states.containsKey(new BlockPos(stairX, stairY, centerZ + 1))
                                    && states.containsKey(new BlockPos(stairX, stairY + 3, centerZ - 2))
                                    && states.containsKey(new BlockPos(stairX, stairY + 3, centerZ + 1)),
                            "Vertical archive stair is not enclosed by four-block walls at step " + step);
                }
                for (int width = 0; width < 2; width++) {
                    BlockPos lowerLanding = new BlockPos(centerX - 9, lowY, centerZ - 1 + width);
                    BlockPos upperLanding = new BlockPos(centerX + 8, highY, centerZ - 1 + width);
                    helper.assertTrue(
                            states.containsKey(lowerLanding)
                                    && states.get(lowerLanding).is(ModBlocks.ARCHIVE_STAIRS.get())
                                    && states.get(lowerLanding).getValue(StairBlock.FACING) == Direction.EAST
                                    && states.containsKey(upperLanding)
                                    && states.get(upperLanding).is(ModBlocks.PHASE_PLATFORM.get()),
                            "Vertical archive stairs omit the lower stair or upper floor landing");
                    helper.assertTrue(
                            !states.containsKey(lowerLanding.above())
                                    && !states.containsKey(upperLanding.above()),
                            "Vertical archive landing is blocked at the room threshold");
                }
            }
        }
        helper.succeed();
    }

    public static void archiveDungeonContractIsComplete(GameTestHelper helper) {
        ArchiveDungeonSettings minimumSettings = new ArchiveDungeonSettings(
                7, 7, 4, 2, 8, 0.45D, 0.15D, 0.10D, 0.25D,
                2, 2, 0.10D, 0.50D, 4096, 64);
        boolean sawUp = false;
        boolean sawDown = false;
        for (long seed = 0L; seed < 64L; seed++) {
            ArchiveDungeonGraph graph = ArchiveRunGenerator.generateDungeon(seed, minimumSettings);
            helper.assertTrue(graph.rooms().size() == 7, "Minimum-size generation did not produce exactly seven rooms");
            helper.assertTrue(graph.overlapCount() == 0, "Minimum-size generation produced overlapping rooms");
            helper.assertTrue(graph.unreachableRoomCount() == 0, "Minimum-size generation produced unreachable rooms");
            helper.assertTrue(
                    graph.rooms().stream().filter(room -> room.category() == ArchiveRoomCategory.MINI_BOSS).count() == 1,
                    "Seven-room generation did not guarantee exactly one lesser boss");
            helper.assertTrue(
                    graph.rooms().stream().filter(room -> room.placement().coordinates().y() > 0).count()
                            <= minimumSettings.maximumRoomsAbove(),
                    "Generation exceeded the configured above-start room count");
            helper.assertTrue(
                    graph.rooms().stream().filter(room -> room.placement().coordinates().y() < 0).count()
                            <= minimumSettings.maximumRoomsBelow(),
                    "Generation exceeded the configured below-start room count");
            sawUp |= graph.rooms().stream().flatMap(room -> room.connections().stream())
                    .anyMatch(connection -> connection.direction() == com.nightbeam.tbos.run.ArchiveDirection.UP);
            sawDown |= graph.rooms().stream().flatMap(room -> room.connections().stream())
                    .anyMatch(connection -> connection.direction() == com.nightbeam.tbos.run.ArchiveDirection.DOWN);
        }
        helper.assertTrue(sawUp && sawDown, "Seed range did not exercise both vertical connection directions");

        ArchiveDungeonSettings maximumSettings = new ArchiveDungeonSettings(
                48, 48, 12, 4, 48, 0.55D, 0.10D, 0.12D, 0.30D,
                16, 16, 0.12D, 0.50D, 4096, 128);
        ArchiveDungeonGraph maximum = ArchiveRunGenerator.generateDungeon(0x5EEDBEEFL, maximumSettings);
        helper.assertTrue(
                ArchiveRunGenerator.lesserBossCountFor(7) == 1
                        && ArchiveRunGenerator.lesserBossCountFor(11) == 1
                        && ArchiveRunGenerator.lesserBossCountFor(12) == 2
                        && ArchiveRunGenerator.lesserBossCountFor(23) == 2
                        && ArchiveRunGenerator.lesserBossCountFor(24) == 3
                        && ArchiveRunGenerator.lesserBossCountFor(48) == 3,
                "Lesser-boss count thresholds do not match the dungeon-size contract");
        helper.assertTrue(maximum.rooms().size() == 48, "Maximum-size generation did not produce 48 rooms");
        helper.assertTrue(maximum.overlapCount() == 0 && maximum.unreachableRoomCount() == 0,
                "Maximum-size graph violated overlap or reachability invariants");
        helper.assertTrue(
                maximum.rooms().stream().filter(room -> room.category() == ArchiveRoomCategory.MINI_BOSS).count() == 3,
                "Maximum-size generation did not cap its guaranteed lesser bosses at three");

        for (ArchiveRoomNode room : maximum.rooms()) {
            ArchiveRoomTemplate template = ArchiveRoomTemplates.require(room.templateId());
            for (var connection : room.connections()) {
                helper.assertTrue(
                        template.supports(connection.direction(), room.placement().transform()),
                        "A transformed template does not support its persisted door direction");
            }
            for (BlockPos marker : template.playerEntryMarkers()) {
                BlockPos transformed = room.placement().transform().apply(marker, template.size());
                helper.assertTrue(
                        transformed.getX() >= 0 && transformed.getZ() >= 0
                                && transformed.getX() < template.size().width()
                                && transformed.getZ() < template.size().depth(),
                        "Template rotation or mirroring moved an entry marker outside its footprint");
            }
        }
        ArchiveRoomNode reward = maximum.room(maximum.rewardRoom());
        helper.assertTrue(
                reward.runtime().doorsLocked()
                        && reward.connections().stream().allMatch(com.nightbeam.tbos.run.ArchiveConnection::locked),
                "Final reward room was not sealed behind the undefeated boss");
        ArchiveRoomNode boss = maximum.room(maximum.bossRoom());
        helper.assertTrue(
                boss.connections().stream()
                        .filter(connection -> connection.targetRoom() != maximum.rewardRoom())
                        .allMatch(com.nightbeam.tbos.run.ArchiveConnection::locked),
                "Final boss entrance was not sealed behind the Cantor Seal quest");

        ArchiveQuestProgress initialQuest = ArchiveQuestProgress.from(maximum);
        helper.assertTrue(
                initialQuest.roomsCleared() == 0
                        && initialQuest.lesserBossesDefeated() == 0
                        && !initialQuest.complete(),
                "Cantor Seal quest did not begin in a locked, empty state");
        ArchiveDungeonGraph questGraph = maximum;
        for (ArchiveRoomNode room : maximum.rooms()) {
            if (room.category().mandatory()
                    || room.category() == ArchiveRoomCategory.SECRET
                    || room.category() == ArchiveRoomCategory.MINI_BOSS
                    || ArchiveQuestProgress.from(questGraph).roomsCleared()
                            >= initialQuest.roomsRequired()) {
                continue;
            }
            questGraph = questGraph.completeRoom(room.index());
        }
        helper.assertTrue(
                !ArchiveQuestProgress.from(questGraph).complete()
                        && questGraph.room(questGraph.bossRoom()).connections().stream()
                                .filter(connection -> connection.targetRoom() != maximum.rewardRoom())
                                .allMatch(com.nightbeam.tbos.run.ArchiveConnection::locked),
                "Room-clearing alone opened the final boss before the lesser bosses fell");
        for (ArchiveRoomNode room : maximum.rooms()) {
            if (room.category() == ArchiveRoomCategory.MINI_BOSS) {
                questGraph = questGraph.completeRoom(room.index());
            }
        }
        helper.assertTrue(
                ArchiveQuestProgress.from(questGraph).complete()
                        && questGraph.room(questGraph.bossRoom()).connections().stream()
                                .filter(connection -> connection.targetRoom() != maximum.rewardRoom())
                                .noneMatch(com.nightbeam.tbos.run.ArchiveConnection::locked),
                "Completing the Cantor Seal quest did not open the final boss entrance");

        UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000099");
        ArchiveRun run = ArchiveRun.create(
                UUID.fromString("10000000-0000-0000-0000-000000000099"),
                maximum.seed(),
                0,
                List.of(new ArchiveRunMember(
                        memberId,
                        new ArchiveReturnPoint(
                                ResourceLocation.withDefaultNamespace("overworld"), new BlockPos(0, 80, 0), 0.0F, 0.0F))),
                maximum);
        ArchiveRun activeRun = run.markGeometryPlaced().activate();
        BlockPos roomCache = activeRun.dungeonGraph().rooms().stream()
                .filter(room -> room.index() != activeRun.dungeonGraph().rewardRoom())
                .map(room -> ArchiveRoomPlacer.chestPositions(activeRun, room.index()))
                .filter(positions -> !positions.isEmpty())
                .map(List::getFirst)
                .findFirst()
                .orElseThrow();
        helper.assertTrue(
                ArchiveRunProtection.classify(
                                activeRun,
                                roomCache,
                                ModBlocks.ARCHIVE_CACHE.get().defaultBlockState())
                        == ArchiveRunProtection.Decision.ROOM_CACHE,
                "A generated room cache could not be broken to claim its loot");
        int startingRoom = activeRun.dungeonGraph().startingRoom();
        BlockPos roomInterior = ArchiveRoomPlacer.roomSpawn(activeRun, startingRoom);
        helper.assertTrue(
                ArchiveRunProtection.classify(
                                activeRun,
                                roomInterior,
                                Blocks.BOOKSHELF.defaultBlockState())
                        == ArchiveRunProtection.Decision.BREAKABLE,
                "Interior room dressing was not breakable during an active run");
        helper.assertTrue(
                ArchiveRunProtection.classify(
                                activeRun,
                                roomInterior.below(),
                                ModBlocks.ARCHIVE_STONE.get().defaultBlockState())
                        == ArchiveRunProtection.Decision.DENY,
                "The generated room floor was not protected during an active run");
        var startingBounds = ArchiveRoomPlacer.roomBounds(activeRun, startingRoom);
        BlockPos wallPosition = new BlockPos(
                startingBounds.minX(),
                startingBounds.minY() + 1,
                roomInterior.getZ());
        helper.assertTrue(
                ArchiveRunProtection.classify(
                                activeRun,
                                wallPosition,
                                ModBlocks.ARCHIVE_BRICKS.get().defaultBlockState())
                        == ArchiveRunProtection.Decision.DENY,
                "The generated room wall was not protected during an active run");
        ArchiveRun victoryRun = activeRun.beginReturn(200L);
        helper.assertTrue(
                ArchiveRunProtection.classify(
                                victoryRun,
                                ArchiveRoomPlacer.roomSpawn(victoryRun, victoryRun.dungeonGraph().startingRoom()).below(),
                                ModBlocks.ARCHIVE_STONE.get().defaultBlockState())
                        == ArchiveRunProtection.Decision.DENY,
                "Ordinary Archive blocks were not protected from player edits");
        helper.assertTrue(
                ArchiveRunProtection.classify(
                                victoryRun,
                                ArchiveRoomPlacer.rewardCachePosition(victoryRun),
                                ModBlocks.ARCHIVE_CACHE.get().defaultBlockState())
                        == ArchiveRunProtection.Decision.CANTOR_CACHE,
                "The victorious Cantor Cache was not classified as a break-to-claim reward");
        helper.assertTrue(
                ArchiveRunProtection.classify(
                                victoryRun,
                                new BlockPos(
                                        ArchiveInstanceLayout.boundsForSlot(victoryRun.instanceSlot()).maxX() + 1,
                                        ArchiveInstanceLayout.BASE_Y,
                                        ArchiveInstanceLayout.boundsForSlot(victoryRun.instanceSlot()).maxZ() + 1),
                                ModBlocks.ARCHIVE_STONE.get().defaultBlockState())
                        == ArchiveRunProtection.Decision.OUTSIDE,
                "Archive protection escaped its allocated instance bounds");
        var vertical = maximum.rooms().stream()
                .flatMap(room -> room.connections().stream()
                        .filter(connection -> connection.direction().vertical())
                        .map(connection -> java.util.Map.entry(room.index(), connection)))
                .findFirst()
                .orElseThrow();
        List<BlockPos> firstSeal = ArchiveRoomPlacer.doorPositions(
                run, vertical.getKey(), vertical.getValue().direction());
        List<BlockPos> reciprocalSeal = ArchiveRoomPlacer.doorPositions(
                run,
                vertical.getValue().targetRoom(),
                vertical.getValue().direction().opposite());
        helper.assertTrue(firstSeal.size() == 6 && new HashSet<>(firstSeal).equals(new HashSet<>(reciprocalSeal)),
                "Vertical room doors do not seal the same two-wide stair cross-section");
        helper.assertTrue(
                !ArchiveRoomPlacer.doorwayClear(
                        run,
                        vertical.getKey(),
                        List.of(new AABB(firstSeal.getFirst()).inflate(0.1D))),
                "Archive doorway safety allowed a seal to close through an occupant");
        BlockPos safeRoomCenter = ArchiveRoomPlacer.roomSpawn(run, vertical.getKey());
        helper.assertTrue(
                ArchiveRoomPlacer.doorwayClear(
                        run,
                        vertical.getKey(),
                        List.of(new AABB(safeRoomCenter).inflate(0.1D))),
                "Archive doorway safety rejected an occupant who had cleared the threshold");

        ArchiveRoomNode combat = maximum.rooms().stream()
                .filter(room -> room.category().combat() && room.category() != ArchiveRoomCategory.FINAL_BOSS)
                .findFirst()
                .orElseThrow();
        helper.assertTrue(
                ArchiveEnemyKind.parse("tbos:memory_leech").orElseThrow() == ArchiveEnemyKind.MEMORY_LEECH
                        && ArchiveEnemyKind.parse("memory_leech").orElseThrow()
                                == ArchiveEnemyKind.MEMORY_LEECH,
                "Memory Leech identifiers were not accepted by encounter config parsing");
        helper.assertTrue(
                ArchiveDungeonRules.DEFAULT.enemyPool(ArchiveDungeonRules.FORGOTTEN_LEGION).stream()
                                .anyMatch(entry -> entry.kind() == ArchiveEnemyKind.MEMORY_LEECH
                                        && entry.weight() == 2)
                        && ArchiveDungeonRules.DEFAULT.enemyPool(ArchiveDungeonRules.ELITE_ECHOES).stream()
                                .anyMatch(entry -> entry.kind() == ArchiveEnemyKind.MEMORY_LEECH
                                        && entry.weight() == 3),
                "Memory Leech weights were not present in both built-in encounter pools");
        helper.assertTrue(
                java.util.stream.LongStream.range(0L, 256L)
                                .mapToObj(seed -> ArchiveDungeonRules.DEFAULT.chooseEnemy(
                                        ArchiveDungeonRules.FORGOTTEN_LEGION,
                                        net.minecraft.util.RandomSource.create(seed)))
                                .anyMatch(kind -> kind == ArchiveEnemyKind.MEMORY_LEECH)
                        && java.util.stream.LongStream.range(0L, 256L)
                                .mapToObj(seed -> ArchiveDungeonRules.DEFAULT.chooseEnemy(
                                        ArchiveDungeonRules.ELITE_ECHOES,
                                        net.minecraft.util.RandomSource.create(seed)))
                                .anyMatch(kind -> kind == ArchiveEnemyKind.MEMORY_LEECH),
                "Seeded encounter selection could not choose the Memory Leech");
        helper.assertTrue(
                ArchiveEncounterManager.abilitiesFor(ArchiveEnemyKind.MEMORY_LEECH, 10L, false).isEmpty(),
                "Memory Leech received a tag ability that can interrupt its native pounce");
        MemoryLeechEntity memoryLeech =
                ModEntities.MEMORY_LEECH.get().create(helper.getLevel());
        helper.assertTrue(memoryLeech != null, "Registered Memory Leech type could not create an entity");
        helper.assertTrue(
                memoryLeech.getAttributeValue(Attributes.MAX_HEALTH) == 32.0D
                        && memoryLeech.getAttributeValue(Attributes.ATTACK_DAMAGE) == 6.0D
                        && memoryLeech.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.31D
                        && memoryLeech.getAttributeValue(Attributes.ARMOR) == 3.0D
                        && memoryLeech.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0.15D
                        && memoryLeech.getAttributeValue(Attributes.FOLLOW_RANGE) == 32.0D
                        && memoryLeech.getPouncePhase() == MemoryLeechEntity.PouncePhase.IDLE,
                "Memory Leech attributes or initial pounce state did not match its elite profile");
        List<ArchiveEnemyKind> solo = ArchiveEncounterManager.planWave(
                combat, 12345L, 1, 1, ArchiveDungeonRules.DEFAULT);
        List<ArchiveEnemyKind> party = ArchiveEncounterManager.planWave(
                combat, 12345L, 1, 4, ArchiveDungeonRules.DEFAULT);
        helper.assertTrue(!solo.isEmpty() && party.size() > solo.size(),
                "Weighted encounter composition did not scale with active party size");
        Set<ArchiveEnemyKind> vanillaEnemies = Set.of(
                ArchiveEnemyKind.HUSK,
                ArchiveEnemyKind.SKELETON,
                ArchiveEnemyKind.STRAY,
                ArchiveEnemyKind.CAVE_SPIDER,
                ArchiveEnemyKind.SILVERFISH,
                ArchiveEnemyKind.VINDICATOR,
                ArchiveEnemyKind.EVOKER,
                ArchiveEnemyKind.RAVAGER);
        boolean sawVanillaEnemy = java.util.stream.LongStream.range(0L, 128L)
                .mapToObj(encounterSeed -> ArchiveEncounterManager.planWave(
                        combat, encounterSeed, 1, 4, ArchiveDungeonRules.DEFAULT))
                .flatMap(List::stream)
                .anyMatch(vanillaEnemies::contains);
        helper.assertTrue(sawVanillaEnemy, "Weighted Archive waves never selected a supported vanilla monster");
        ArchiveRoomNode lesserBossRoom = maximum.rooms().stream()
                .filter(room -> room.category() == ArchiveRoomCategory.MINI_BOSS)
                .findFirst()
                .orElseThrow();
        List<ArchiveEnemyKind> lesserBossWave = ArchiveEncounterManager.planWave(
                lesserBossRoom, 54321L, 2, 1, ArchiveDungeonRules.DEFAULT);
        helper.assertTrue(
                !lesserBossWave.isEmpty()
                        && Set.of(
                                        ArchiveEnemyKind.MERIDIAN_SENTINEL,
                                        ArchiveEnemyKind.VINDICATOR,
                                        ArchiveEnemyKind.EVOKER,
                                        ArchiveEnemyKind.RAVAGER)
                                .contains(lesserBossWave.getFirst())
                        && !lesserBossWave.contains(ArchiveEnemyKind.HOUR_CANTOR)
                        && !lesserBossWave.contains(ArchiveEnemyKind.MEMORY_LEECH),
                "Lesser-boss room did not choose a varied boss below the Hour Cantor tier");
        helper.assertTrue(
                ArchiveEncounterManager.abilitiesFor(ArchiveEnemyKind.SKELETON, 11L, false)
                        .contains(ArchiveEnemyAbility.ECHO_BOLT)
                        && ArchiveEncounterManager.abilitiesFor(ArchiveEnemyKind.CAVE_SPIDER, 12L, false)
                                .contains(ArchiveEnemyAbility.SPLITTER)
                        && ArchiveEncounterManager.abilitiesFor(ArchiveEnemyKind.VINDICATOR, 13L, true)
                                .containsAll(Set.of(
                                        ArchiveEnemyAbility.MERIDIAN_SHOCKWAVE,
                                        ArchiveEnemyAbility.WARD_AURA)),
                "Archive enemies did not retain ranged, splitting, shockwave, and lesser-boss mutations");
        helper.assertTrue(
                ArchiveEncounterManager.abilitiesFor(ArchiveEnemyKind.MERIDIAN_SENTINEL, 13L, true)
                        .equals(Set.of(ArchiveEnemyAbility.WARD_AURA)),
                "A lesser-boss Sentinel kept a tag shockwave now that it slams natively");
        for (ArchiveEnemyKind native_ : List.of(
                ArchiveEnemyKind.PARALLAX_WRAITH,
                ArchiveEnemyKind.MERIDIAN_SENTINEL,
                ArchiveEnemyKind.HOUR_CANTOR,
                ArchiveEnemyKind.MEMORY_LEECH,
                ArchiveEnemyKind.LENSWARD,
                ArchiveEnemyKind.SHARD_DRIFTER,
                ArchiveEnemyKind.WAKE_CUTTER,
                ArchiveEnemyKind.NULL_PORTRAIT,
                ArchiveEnemyKind.GALLERY_MOTH,
                ArchiveEnemyKind.GNOMON_KNIGHT,
                ArchiveEnemyKind.ARMILLARY_SCOUT,
                ArchiveEnemyKind.DUST_CANTORILE,
                ArchiveEnemyKind.ASH_CHORISTER,
                ArchiveEnemyKind.PRISM_STALKER,
                ArchiveEnemyKind.SHARDLING_SWARM,
                ArchiveEnemyKind.INDEX_WIGHT,
                ArchiveEnemyKind.SHELF_CRAWLER,
                ArchiveEnemyKind.METRONOME_HOUND,
                ArchiveEnemyKind.LABYRINTH_USHER,
                ArchiveEnemyKind.BLANK_CHRONIST,
                ArchiveEnemyKind.HOUR_HAND_WRAITH)) {
            helper.assertTrue(
                    ArchiveEncounterManager.ownsNativeAbility(native_)
                            && java.util.stream.LongStream.range(0L, 64L)
                                    .noneMatch(seed -> ArchiveEncounterManager
                                            .abilitiesFor(native_, seed, false)
                                            .contains(ArchiveEnemyAbility.PARALLAX_BLINK)),
                    "A creature that telegraphs its own move received a blink mutation: " + native_);
        }
        for (long floor = 0L; floor < ArchiveFloorPresentation.NAME_COUNT; floor++) {
            ArchiveFloorTheme theme = ArchiveFloorPresentation.theme(floor);
            int expectedNormalExclusives =
                    theme == ArchiveFloorTheme.CANTORS_LABYRINTH ? 2 : 1;
            helper.assertTrue(
                    theme.exclusiveWeights().size() == expectedNormalExclusives,
                    "Floor theme " + theme + " exposed the wrong normal-exclusive enemy count");
            Set<ArchiveEnemyKind> exclusives = theme.exclusiveWeights().stream()
                    .map(ArchiveDungeonRules.EnemyWeight::kind)
                    .collect(java.util.stream.Collectors.toSet());
            helper.assertTrue(
                    !exclusives.contains(theme.bossKind())
                            && ArchiveEncounterManager.finalBossKind(floor) == theme.bossKind(),
                    "Theme boss leaked into the normal pool for " + theme);
            boolean sawExclusive = java.util.stream.LongStream.range(0L, 512L)
                    .mapToObj(seed -> ArchiveDungeonRules.DEFAULT.chooseEnemy(
                            ArchiveDungeonRules.FORGOTTEN_LEGION,
                            net.minecraft.util.RandomSource.create(seed),
                            theme,
                            1L))
                    .anyMatch(exclusives::contains);
            helper.assertTrue(sawExclusive, "Theme exclusives never appeared in merged pools for " + theme);
            for (long other = 0L; other < ArchiveFloorPresentation.NAME_COUNT; other++) {
                if (other == floor) {
                    continue;
                }
                ArchiveFloorTheme otherTheme = ArchiveFloorPresentation.theme(other);
                Set<ArchiveEnemyKind> otherExclusives = otherTheme.exclusiveWeights().stream()
                        .map(ArchiveDungeonRules.EnemyWeight::kind)
                        .collect(java.util.stream.Collectors.toSet());
                helper.assertTrue(
                        exclusives.stream().noneMatch(otherExclusives::contains),
                        "Theme exclusive sets overlapped between " + theme + " and " + otherTheme);
            }
        }
        helper.assertTrue(
                ArchiveEncounterManager.abilitiesFor(ArchiveEnemyKind.HOUR_CANTOR, 14L, false)
                        .equals(Set.of(ArchiveEnemyAbility.WARD_AURA)),
                "The Hour Cantor kept tag mutations that would interrupt its own refrain");
        Set<ArchiveEnemyDropKind> sampledDrops = new HashSet<>();
        for (long dropSeed = 0L; dropSeed < 256L; dropSeed++) {
            sampledDrops.add(ArchiveEncounterManager.rollEnemyDrop(
                    ArchiveEnemyKind.PARALLAX_WRAITH, dropSeed, false));
        }
        helper.assertTrue(
                sampledDrops.contains(ArchiveEnemyDropKind.ECHO_HEART)
                        && sampledDrops.contains(ArchiveEnemyDropKind.SOUL_HEART)
                        && sampledDrops.contains(ArchiveEnemyDropKind.COIN)
                        && sampledDrops.size() >= 6,
                "Archive enemy drops did not expose varied Isaac-style healing and utility pickups");
        helper.assertTrue(
                ArchiveEncounterManager.rollEnemyDrop(
                                ArchiveEnemyKind.VINDICATOR, 55L, true)
                        != ArchiveEnemyDropKind.NONE,
                "A lesser boss was allowed to roll an empty pickup");
        helper.assertTrue(
                ArchiveEncounterManager.rollEnemyDrop(
                                ArchiveEnemyKind.PARALLAX_WRAITH, 0L, false)
                        == ArchiveEnemyDropKind.KEY
                        && ArchiveEncounterManager.rollEnemyDrop(
                                        ArchiveEnemyKind.MERIDIAN_SENTINEL, 12345L, false)
                                == ArchiveEnemyDropKind.ECHO_HEART
                        && ArchiveEncounterManager.rollEnemyDrop(
                                        ArchiveEnemyKind.HOUR_CANTOR, 1L, true)
                                == ArchiveEnemyDropKind.ECHO_HEART
                        && ArchiveEncounterManager.rollEnemyDrop(
                                        ArchiveEnemyKind.RAVAGER, 55L, false)
                                == ArchiveEnemyDropKind.ECHO_HEART,
                "Appending the Memory Leech changed deterministic drops for existing enemies");

        ResourceLocation selectedLoot = ArchiveLootRoller.selectTable(
                combat, ArchiveDungeonRules.DEFAULT, net.minecraft.util.RandomSource.create(99L));
        helper.assertTrue(
                combat.allowedLootTables().isEmpty() || combat.allowedLootTables().contains(selectedLoot),
                "Weighted loot selection escaped the room's allowed tables");
        ArchiveRun claimed = run.claimMemberContainer(memberId, combat.index(), 0);
        helper.assertTrue(
                claimed.hasMemberClaimedContainer(memberId, combat.index(), 0)
                        && !run.hasMemberClaimedContainer(memberId, combat.index(), 0),
                "Individual multiplayer cache claim was not isolated and persisted immutably");
        UUID cachePartnerId = UUID.fromString("00000000-0000-0000-0000-000000000100");
        ArchiveRun partyClaims = ArchiveRun.create(
                UUID.fromString("10000000-0000-0000-0000-000000000100"),
                maximum.seed(),
                1,
                List.of(
                        new ArchiveRunMember(memberId, run.members().getFirst().returnPoint()),
                        new ArchiveRunMember(cachePartnerId, run.members().getFirst().returnPoint())),
                maximum);
        ArchiveRun firstClaim = partyClaims.claimMemberContainer(memberId, combat.index(), 0);
        helper.assertTrue(
                !firstClaim.allMembersClaimedContainer(combat.index(), 0)
                        && firstClaim
                                .claimMemberContainer(cachePartnerId, combat.index(), 0)
                                .allMembersClaimedContainer(combat.index(), 0),
                "An individual cache did not remain until every party member claimed it");
        var encoded = ArchiveRun.CODEC.encodeStart(JsonOps.INSTANCE, claimed).getOrThrow();
        helper.assertTrue(
                ArchiveRun.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow()
                        .hasMemberClaimedContainer(memberId, combat.index(), 0),
                "Individual cache claim was lost in the restart codec");

        UUID secondMemberId = UUID.fromString("00000000-0000-0000-0000-000000000100");
        ArchiveRun splitParty = ArchiveRun.create(
                        UUID.fromString("10000000-0000-0000-0000-000000000100"),
                        maximum.seed(),
                        1,
                        List.of(
                                new ArchiveRunMember(
                                        memberId,
                                        new ArchiveReturnPoint(
                                                ResourceLocation.withDefaultNamespace("overworld"),
                                                new BlockPos(0, 80, 0),
                                                0.0F,
                                                0.0F)),
                                new ArchiveRunMember(
                                        secondMemberId,
                                        new ArchiveReturnPoint(
                                                ResourceLocation.withDefaultNamespace("overworld"),
                                                new BlockPos(1, 80, 0),
                                                0.0F,
                                                0.0F))),
                        maximum)
                .markGeometryPlaced()
                .activate()
                .visitRoom(1, List.of(memberId))
                .visitRoom(2, List.of(secondMemberId));
        helper.assertTrue(
                splitParty.member(memberId).orElseThrow().checkpointRoom() == 1
                        && splitParty.member(secondMemberId).orElseThrow().checkpointRoom() == 2,
                "Split-party members did not retain independent death/re-entry checkpoints");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    public static void memoryLeechPounceSiphonsOnce(GameTestHelper helper) {
        // Same floor as before, but force-loaded: the leech's pounce is gated on
        // onGround(), which never becomes true while the floor's chunk is still
        // arriving, and the assertion below is counted in ticks rather than time.
        combatFloor(helper, 9);

        var victim = helper.spawn(
                VanillaCompat.SHEEP,
                new Vec3(5.5D, 1.0D, 4.5D));
        victim.setNoAi(true);

        net.minecraft.server.level.ServerPlayer observer = helper.makeMockServerPlayerInLevel();
        Vec3 observerPosition = helper.absoluteVec(new Vec3(0.5D, 1.0D, 4.5D));
        observer.moveTo(observerPosition.x, observerPosition.y, observerPosition.z, 90.0F, 0.0F);

        MemoryLeechEntity leech = helper.spawn(
                ModEntities.MEMORY_LEECH.get(),
                new Vec3(1.5D, 1.0D, 4.5D));
        leech.setHealth(20.0F);
        leech.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        leech.setTarget(victim);

        helper.runAfterDelay(75L, () -> {
            helper.assertTrue(
                    victim.hasEffect(MobEffects.WEAKNESS),
                    "Memory Leech pounce did not apply its three-second Weakness effect"
                            + " [phase=" + leech.getPouncePhase()
                            + ", cooldown=" + leech.getPounceCooldown()
                            + ", leech=" + leech.position()
                            + ", victim=" + victim.position()
                            + ", distanceSqr=" + leech.distanceToSqr(victim)
                            + ", onGround=" + leech.onGround()
                            + ", hasTarget=" + (leech.getTarget() == victim)
                            + ", lineOfSight=" + leech.getSensing().hasLineOfSight(victim)
                            + "]");
            helper.assertTrue(
                    Math.abs(leech.getHealth() - 24.0F) < 0.01F,
                    "Memory Leech pounce did not heal exactly four health once");
            helper.assertTrue(
                    leech.getPouncePhase() == MemoryLeechEntity.PouncePhase.IDLE
                            && leech.getPounceCooldown() > 0,
                    "Memory Leech did not enter its post-pounce cooldown");
            helper.succeed();
        });
    }

    public static void lenswardContractIsComplete(GameTestHelper helper) {
        helper.assertTrue(
                ArchiveEnemyKind.parse("tbos:lensward").orElseThrow() == ArchiveEnemyKind.LENSWARD
                        && ArchiveEnemyKind.parse("lensward").orElseThrow() == ArchiveEnemyKind.LENSWARD,
                "Lensward identifiers were not accepted by encounter config parsing");
        helper.assertTrue(
                ArchiveEnemyKind.values()[ArchiveEnemyKind.values().length - 1] == ArchiveEnemyKind.LENSWARD,
                "Lensward must stay last in ArchiveEnemyKind; rollEnemyDrop seeds on ordinal()");
        helper.assertTrue(
                ArchiveDungeonRules.DEFAULT.enemyPool(ArchiveDungeonRules.FORGOTTEN_LEGION).stream()
                                .anyMatch(entry -> entry.kind() == ArchiveEnemyKind.LENSWARD
                                        && entry.weight() == 1)
                        && ArchiveDungeonRules.DEFAULT.enemyPool(ArchiveDungeonRules.ELITE_ECHOES).stream()
                                .anyMatch(entry -> entry.kind() == ArchiveEnemyKind.LENSWARD
                                        && entry.weight() == 3),
                "Lensward weights were not present in both built-in encounter pools");
        helper.assertTrue(
                ArchiveDungeonRules.DEFAULT.enemyPool(ArchiveDungeonRules.RUINED_GUARDIAN).stream()
                                .noneMatch(entry -> entry.kind() == ArchiveEnemyKind.LENSWARD)
                        && ArchiveDungeonRules.DEFAULT.enemyPool(ArchiveDungeonRules.HOUR_CANTOR_POOL).stream()
                                .noneMatch(entry -> entry.kind() == ArchiveEnemyKind.LENSWARD),
                "Lensward leaked into a lesser-boss or final-boss pool; it is not a boss");
        helper.assertTrue(
                java.util.stream.LongStream.range(0L, 256L)
                        .mapToObj(seed -> ArchiveDungeonRules.DEFAULT.chooseEnemy(
                                ArchiveDungeonRules.ELITE_ECHOES,
                                net.minecraft.util.RandomSource.create(seed)))
                        .anyMatch(kind -> kind == ArchiveEnemyKind.LENSWARD),
                "Seeded encounter selection could not choose the Lensward");
        helper.assertTrue(
                ArchiveEncounterManager.abilitiesFor(ArchiveEnemyKind.LENSWARD, 10L, false).isEmpty()
                        && java.util.stream.LongStream.range(0L, 64L)
                                .noneMatch(seed -> ArchiveEncounterManager
                                        .abilitiesFor(ArchiveEnemyKind.LENSWARD, seed, false)
                                        .contains(ArchiveEnemyAbility.PARALLAX_BLINK)),
                "Lensward received a mutation that would blink it off the anchor it guards");

        LenswardEntity lensward =
                ModEntities.LENSWARD.get().create(helper.getLevel());
        helper.assertTrue(lensward != null, "Registered Lensward type could not create an entity");
        helper.assertTrue(
                lensward.getAttributeValue(Attributes.MAX_HEALTH) == 28.0D
                        && lensward.getAttributeValue(Attributes.ATTACK_DAMAGE) == 5.0D
                        && lensward.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.24D
                        && lensward.getAttributeValue(Attributes.ARMOR) == 6.0D
                        && lensward.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) == 0.6D
                        && lensward.getAttributeValue(Attributes.FOLLOW_RANGE) == 24.0D
                        && lensward.getBeamPhase() == LenswardEntity.BeamPhase.IDLE,
                "Lensward attributes or initial beam state did not match its guardian profile");
        helper.assertTrue(
                lensward.isNoGravity(),
                "Lensward must hover; gravity would drop it off its ward");
        helper.succeed();
    }

    private static void combatFloor(GameTestHelper helper, int maxX) {
        // Force-load chunks so spawned entities tick; without this they sit at tickCount==0.
        BlockPos min = helper.absolutePos(new BlockPos(-2, 0, 0));
        BlockPos max = helper.absolutePos(new BlockPos(maxX + 2, 0, 8));
        for (int chunkX = Math.min(min.getX(), max.getX()) >> 4;
                chunkX <= Math.max(min.getX(), max.getX()) >> 4;
                chunkX++) {
            for (int chunkZ = Math.min(min.getZ(), max.getZ()) >> 4;
                    chunkZ <= Math.max(min.getZ(), max.getZ()) >> 4;
                    chunkZ++) {
                helper.getLevel().setChunkForced(chunkX, chunkZ, true);
            }
        }
        for (int x = 0; x <= maxX; x++) {
            for (int z = 2; z <= 6; z++) {
                helper.getLevel().setBlock(
                        helper.absolutePos(new BlockPos(x, 0, z)),
                        Blocks.STONE.defaultBlockState(),
                        3);
            }
        }
    }

    @SuppressWarnings("removal")
    public static void lenswardBeamStrikesOnce(GameTestHelper helper) {
        combatFloor(helper, 11);
        var victim = helper.spawn(
                VanillaCompat.SHEEP, new Vec3(9.5D, 1.0D, 4.5D));
        victim.setNoAi(true);
        float startHealth = victim.getHealth();

        net.minecraft.server.level.ServerPlayer observer = helper.makeMockServerPlayerInLevel();
        Vec3 observerPosition = helper.absoluteVec(new Vec3(0.5D, 1.0D, 4.5D));
        observer.moveTo(observerPosition.x, observerPosition.y, observerPosition.z, 90.0F, 0.0F);

        LenswardEntity lensward = helper.spawn(
                ModEntities.LENSWARD.get(), new Vec3(2.5D, 1.5D, 4.5D));
        lensward.setTarget(victim);

        float expected = startHealth - 6.0F;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        Math.abs(victim.getHealth() - expected) < 0.01F,
                        "Lensward beam never dealt exactly one strike of damage"
                                + " [phase=" + lensward.getBeamPhase()
                                + ", cooldown=" + lensward.getBeamCooldown()
                                + ", ticks=" + lensward.tickCount
                                + ", alive=" + lensward.isAlive()
                                + ", health=" + victim.getHealth() + "/" + startHealth
                                + ", hasTarget=" + (lensward.getTarget() == victim)
                                + ", lineOfSight=" + lensward.getSensing().hasLineOfSight(victim)
                                + "]"))
                .thenIdle(30)
                .thenExecute(() -> {
                    helper.assertTrue(
                            victim.isAlive() && Math.abs(victim.getHealth() - expected) < 0.01F,
                            "Lensward struck more than once inside a single cooldown");
                    helper.assertTrue(
                            lensward.getBeamPhase() == LenswardEntity.BeamPhase.IDLE
                                    && lensward.getBeamCooldown() > 0,
                            "Lensward did not enter its post-beam cooldown");
                })
                .thenSucceed();
    }

    @SuppressWarnings("removal")
    public static void lenswardBeamAbortsWithoutLineOfSight(GameTestHelper helper) {
        combatFloor(helper, 11);
        var victim = helper.spawn(
                VanillaCompat.SHEEP, new Vec3(9.5D, 1.0D, 4.5D));
        victim.setNoAi(true);
        float startHealth = victim.getHealth();

        net.minecraft.server.level.ServerPlayer observer = helper.makeMockServerPlayerInLevel();
        Vec3 observerPosition = helper.absoluteVec(new Vec3(0.5D, 1.0D, 4.5D));
        observer.moveTo(observerPosition.x, observerPosition.y, observerPosition.z, 90.0F, 0.0F);

        LenswardEntity lensward = helper.spawn(
                ModEntities.LENSWARD.get(), new Vec3(2.5D, 1.5D, 4.5D));
        lensward.setTarget(victim);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        lensward.getBeamPhase() == LenswardEntity.BeamPhase.CHARGING,
                        "Lensward never began charging, so the cancel was never exercised"
                                + " [phase=" + lensward.getBeamPhase()
                                + ", cooldown=" + lensward.getBeamCooldown()
                                + ", ticks=" + lensward.tickCount
                                + ", alive=" + lensward.isAlive()
                                + "]"))
                .thenExecute(() -> {
                    for (int y = 1; y <= 4; y++) {
                        for (int z = 2; z <= 6; z++) {
                            helper.getLevel().setBlock(
                                    helper.absolutePos(new BlockPos(6, y, z)),
                                    Blocks.STONE.defaultBlockState(),
                                    3);
                        }
                    }
                })
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            victim.getHealth() == startHealth,
                            "Breaking line of sight during the charge did not cancel the Lensward beam"
                                    + " [phase=" + lensward.getBeamPhase()
                                    + ", cooldown=" + lensward.getBeamCooldown()
                                    + ", health=" + victim.getHealth() + "/" + startHealth
                                    + ", lineOfSight=" + lensward.getSensing().hasLineOfSight(victim)
                                    + "]");
                    helper.assertTrue(
                            lensward.getBeamPhase() == LenswardEntity.BeamPhase.IDLE,
                            "Lensward stayed in a beam phase after losing line of sight");
                })
                .thenSucceed();
    }

    @SuppressWarnings("removal")
    public static void lenswardHoldsItsWard(GameTestHelper helper) {
        combatFloor(helper, 20);

        net.minecraft.server.level.ServerPlayer observer = helper.makeMockServerPlayerInLevel();
        Vec3 observerPosition = helper.absoluteVec(new Vec3(0.5D, 1.0D, 4.5D));
        observer.moveTo(observerPosition.x, observerPosition.y, observerPosition.z, 90.0F, 0.0F);

        BlockPos anchor = helper.absolutePos(new BlockPos(5, 1, 4));
        LenswardEntity lensward = helper.spawn(
                ModEntities.LENSWARD.get(), new Vec3(5.5D, 1.5D, 4.5D));
        lensward.setAnchor(anchor);

        var near = helper.spawn(VanillaCompat.SHEEP, new Vec3(8.5D, 1.0D, 4.5D));
        near.setNoAi(true);
        var far = helper.spawn(VanillaCompat.SHEEP, new Vec3(19.5D, 1.0D, 4.5D));
        far.setNoAi(true);

        helper.assertTrue(
                lensward.withinWard(near),
                "Lensward rejected a target standing three blocks from its anchor");
        helper.assertTrue(
                !lensward.withinWard(far),
                "Lensward accepted a target fourteen blocks outside its ward radius");

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        lensward.tickCount > 40,
                        "Lensward never ticked, so its station-keeping was never exercised"))
                .thenExecute(() -> {
                    double drift = Math.sqrt(lensward.distanceToSqr(Vec3.atCenterOf(anchor)));
                    helper.assertTrue(
                            drift <= LenswardEntity.LEASH_RADIUS,
                            "Lensward drifted outside its own ward radius while idle"
                                    + " [drift=" + drift + ", radius=" + LenswardEntity.LEASH_RADIUS + "]");
                })
                .thenSucceed();
    }

    /** Parks a mock observer clear of the fixture so entities are tracked and ticked. */
    @SuppressWarnings("removal")
    private static void combatObserver(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer observer = helper.makeMockServerPlayerInLevel();
        Vec3 position = helper.absoluteVec(new Vec3(0.5D, 1.0D, 4.5D));
        observer.moveTo(position.x, position.y, position.z, 90.0F, 0.0F);
    }

    public static void parallaxWraithDisplacesBehindTarget(GameTestHelper helper) {
        combatFloor(helper, 15);
        combatObserver(helper);

        var victim = helper.spawn(VanillaCompat.SHEEP, new Vec3(10.5D, 1.0D, 4.5D));
        victim.setNoAi(true);
        Vec3 victimPosition = helper.absoluteVec(new Vec3(10.5D, 1.0D, 4.5D));
        // Facing +X, so the landing block behind the sheep sits back down the floor.
        victim.moveTo(victimPosition.x, victimPosition.y, victimPosition.z, 270.0F, 0.0F);
        float startHealth = victim.getHealth();

        ParallaxWraithEntity wraith = helper.spawn(
                ModEntities.PARALLAX_WRAITH.get(), new Vec3(2.5D, 1.0D, 4.5D));
        wraith.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        wraith.setTarget(victim);

        helper.assertTrue(
                wraith.distanceToSqr(victim) > 9.0D,
                "The wraith fixture began inside its own minimum displacement range");

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        wraith.getDisplacePhase() != ParallaxWraithEntity.DisplacePhase.IDLE,
                        "Parallax Wraith never began a displacement"
                                + " [phase=" + wraith.getDisplacePhase()
                                + ", cooldown=" + wraith.getDisplaceCooldown()
                                + ", ticks=" + wraith.tickCount
                                + ", hasTarget=" + (wraith.getTarget() == victim)
                                + ", lineOfSight=" + wraith.getSensing().hasLineOfSight(victim)
                                + "]"))
                .thenWaitUntil(() -> helper.assertTrue(
                        wraith.getDisplacePhase() == ParallaxWraithEntity.DisplacePhase.IDLE
                                && wraith.getDisplaceCooldown() > 0,
                        "Parallax Wraith never finished reforming into its cooldown"
                                + " [phase=" + wraith.getDisplacePhase()
                                + ", cooldown=" + wraith.getDisplaceCooldown()
                                + "]"))
                .thenExecute(() -> {
                    helper.assertTrue(
                            wraith.distanceToSqr(victim) <= 9.0D,
                            "Parallax Wraith did not reassemble beside its target"
                                    + " [distanceSqr=" + wraith.distanceToSqr(victim) + "]");
                    helper.assertTrue(
                            victim.isAlive() && victim.getHealth() == startHealth,
                            "The displacement dealt damage; it is a reposition, not an attack");
                })
                .thenSucceed();
    }

    public static void meridianSentinelSlamStrikesInRadius(GameTestHelper helper) {
        combatFloor(helper, 15);
        combatObserver(helper);

        var near = helper.spawn(VanillaCompat.SHEEP, new Vec3(5.5D, 1.0D, 4.5D));
        near.setNoAi(true);
        var far = helper.spawn(VanillaCompat.SHEEP, new Vec3(14.5D, 1.0D, 4.5D));
        far.setNoAi(true);
        float nearStart = near.getHealth();
        float farStart = far.getHealth();

        MeridianSentinelEntity sentinel = helper.spawn(
                ModEntities.MERIDIAN_SENTINEL.get(), new Vec3(2.5D, 1.0D, 4.5D));
        sentinel.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        sentinel.setTarget(near);

        float expected = nearStart - 3.0F;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        Math.abs(near.getHealth() - expected) < 0.01F,
                        "Meridian Sentinel slam never landed on a target inside its radius"
                                + " [phase=" + sentinel.getSlamPhase()
                                + ", cooldown=" + sentinel.getSlamCooldown()
                                + ", ticks=" + sentinel.tickCount
                                + ", health=" + near.getHealth() + "/" + nearStart
                                + ", distanceSqr=" + sentinel.distanceToSqr(near)
                                + "]"))
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            Math.abs(near.getHealth() - expected) < 0.01F,
                            "Meridian Sentinel slammed more than once inside a single cooldown");
                    helper.assertTrue(
                            far.getHealth() == farStart,
                            "Meridian Sentinel slam reached a target twelve blocks away"
                                    + " [radius=" + MeridianSentinelEntity.SLAM_RADIUS
                                    + ", distanceSqr=" + sentinel.distanceToSqr(far) + "]");
                    helper.assertTrue(
                            sentinel.getSlamPhase() == MeridianSentinelEntity.SlamPhase.IDLE
                                    && sentinel.getSlamCooldown() > 0,
                            "Meridian Sentinel did not settle into its post-slam cooldown");
                })
                .thenSucceed();
    }

    public static void hourCantorRefrainSlowsAudience(GameTestHelper helper) {
        combatFloor(helper, 15);
        combatObserver(helper);

        var victim = helper.spawn(VanillaCompat.SHEEP, new Vec3(7.5D, 1.0D, 4.5D));
        victim.setNoAi(true);
        float startHealth = victim.getHealth();

        HourCantorEntity cantor = helper.spawn(
                ModEntities.HOUR_CANTOR.get(), new Vec3(2.5D, 1.0D, 4.5D));
        cantor.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        cantor.setTarget(victim);

        helper.assertTrue(
                !cantor.isEscalated(),
                "A Cantor at full health already reported its wounded cadence");

        float expected = startHealth - 2.5F;
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        Math.abs(victim.getHealth() - expected) < 0.01F && victim.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                        "Hour Cantor refrain never damaged and slowed its audience"
                                + " [phase=" + cantor.getRefrainPhase()
                                + ", cooldown=" + cantor.getRefrainCooldown()
                                + ", ticks=" + cantor.tickCount
                                + ", health=" + victim.getHealth() + "/" + startHealth
                                + ", slowed=" + victim.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                                + "]"))
                .thenIdle(40)
                .thenExecute(() -> {
                    helper.assertTrue(
                            Math.abs(victim.getHealth() - expected) < 0.01F,
                            "Hour Cantor released more than one refrain inside a single cooldown");
                    helper.assertTrue(
                            cantor.getRefrainPhase() == HourCantorEntity.RefrainPhase.IDLE
                                    && cantor.getRefrainCooldown() > 0,
                            "Hour Cantor did not rest into its post-refrain cooldown");
                    cantor.setHealth(cantor.getMaxHealth() * 0.4F);
                    helper.assertTrue(
                            cantor.isEscalated(),
                            "A Cantor below half health did not tighten its cadence");
                })
                .thenSucceed();
    }

    public static void themeExclusiveAbilityTelegraphs(GameTestHelper helper) {
        combatFloor(helper, 15);
        combatObserver(helper);

        var victim = helper.spawn(VanillaCompat.SHEEP, new Vec3(8.5D, 1.0D, 4.5D));
        victim.setNoAi(true);

        ThemeExclusiveEntity exclusive = helper.spawn(
                ModEntities.GNOMON_KNIGHT.get(), new Vec3(2.5D, 1.0D, 4.5D));
        exclusive.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.0D);
        exclusive.setTarget(victim);

        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(
                        exclusive.getAbilityPhase() != ThemeExclusiveEntity.AbilityPhase.IDLE,
                        "Theme exclusive never began a signature ability"
                                + " [phase=" + exclusive.getAbilityPhase()
                                + ", cooldown=" + exclusive.getAbilityCooldown()
                                + ", ticks=" + exclusive.tickCount + "]"))
                .thenWaitUntil(() -> helper.assertTrue(
                        exclusive.getAbilityPhase() == ThemeExclusiveEntity.AbilityPhase.IDLE
                                && exclusive.getAbilityCooldown() > 0,
                        "Theme exclusive never returned to cooldown after its signature"))
                .thenSucceed();
    }

    @SuppressWarnings("removal")
    public static void archiveRunEntryValidatesBeforeMutation(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos threshold = player.blockPosition();
        player.setYRot(37.0F);
        player.setXRot(-12.0F);
        ArchiveReturnPoint captured = ArchiveRunManager.captureReturnPoint(player);
        helper.assertTrue(
                captured.dimension().equals(player.level().dimension().location())
                        && captured.position().equals(player.blockPosition())
                        && captured.yRot() == 37.0F
                        && captured.xRot() == -12.0F,
                "Archive entry did not capture the exact return point");
        helper.assertTrue(
                ArchiveRunManager.enterFromThreshold(player, threshold) == ArchiveRunManager.EntryResult.NO_LENS,
                "Archive entry accepted a player without the repaired Lens");

        player.setItemInHand(
                net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.item.ItemStack(ModItems.YESTERGLASS_LENS.get()));
        helper.assertTrue(
                ArchiveRunManager.enterFromThreshold(player, threshold)
                        == ArchiveRunManager.EntryResult.NO_CURATOR_CORE,
                "Archive entry accepted a player who had not recovered the Curator Core");
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModItems.CURATOR_CORE.get()));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.BAD_OMEN, 1200, 2));
        helper.assertTrue(
                ArchiveRunManager.enterFromThreshold(player, threshold)
                        == ArchiveRunManager.EntryResult.ARCHIVE_UNAVAILABLE,
                "GameTest entry did not fail safely when its archive dimension was absent");
        helper.assertTrue(
                player.hasEffect(net.minecraft.world.effect.MobEffects.BAD_OMEN),
                "A failed Archive entry consumed Bad Omen");

        ArchiveRunSavedData storage = ArchiveRunSavedData.get(helper.getLevel().getServer());
        ArchiveRun existing = testArchiveRun(UUID.randomUUID(), storage.nextFreeSlot(), player.getUUID());
        storage.register(existing);
        helper.assertTrue(
                ArchiveRunManager.enterFromThreshold(player, threshold)
                        == ArchiveRunManager.EntryResult.ALREADY_IN_RUN,
                "Archive entry accepted a player who already owns a live run");
        helper.assertTrue(!existing.geometryPlaced(), "Rejected archive entry mutated the existing run");
        helper.succeed();
    }

    public static void archiveSharedRevivesFailOnFourthDeath(GameTestHelper helper) {
        ArchiveRunSavedData storage = new ArchiveRunSavedData();
        UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000031");
        ArchiveRun active = testArchiveRun(
                        UUID.fromString("10000000-0000-0000-0000-000000000031"), 0, memberId)
                .markGeometryPlaced()
                .activate();
        storage.register(active);

        helper.assertTrue(
                ArchiveRunManager.handleDeath(storage, memberId, 10L) == ArchiveRunManager.DeathResult.REVIVED,
                "First archive death was not handled as a revive");
        helper.assertTrue(
                storage.find(active.runId()).orElseThrow().sharedRevives() == 2,
                "First archive death did not consume one shared revive");
        helper.assertTrue(
                ArchiveRunManager.handleDeath(storage, memberId, 10L)
                        == ArchiveRunManager.DeathResult.DUPLICATE_EVENT,
                "Duplicate death handling consumed the same event twice");
        helper.assertTrue(
                storage.find(active.runId()).orElseThrow().sharedRevives() == 2,
                "Duplicate death handling changed the revive pool");

        ArchiveRunManager.handleDeath(storage, memberId, 11L);
        helper.assertTrue(
                storage.find(active.runId()).orElseThrow().sharedRevives() == 1,
                "Second archive death did not consume one shared revive");
        ArchiveRunManager.handleDeath(storage, memberId, 12L);
        helper.assertTrue(
                storage.find(active.runId()).orElseThrow().sharedRevives() == 0,
                "Third archive death did not consume the final shared revive");
        helper.assertTrue(
                ArchiveRunManager.handleDeath(storage, memberId, 13L) == ArchiveRunManager.DeathResult.RUN_FAILED,
                "Fourth archive death did not fail the run");
        ArchiveRun failed = storage.find(active.runId()).orElseThrow();
        helper.assertTrue(
                failed.status() == ArchiveRunStatus.RETURNING_FAILURE && failed.returnDeadlineTick() == 113L,
                "Failed archive run did not persist its five-second return deadline");
        ArchiveRunManager.clearRuntimeState();
        helper.succeed();
    }

    public static void archiveFloorProgressionIsEndless(GameTestHelper helper) {
        helper.assertTrue(
                ArchiveGenerationQueue.fairShare(4096, 0) == 0
                        && ArchiveGenerationQueue.fairShare(0, 1) == 0
                        && ArchiveGenerationQueue.fairShare(4096, 2) == 2048,
                "Archive queue budgeting can divide by zero during a generation-to-cleanup handoff");
        ArchiveRunSavedData storage = new ArchiveRunSavedData();
        UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000041");
        ArchiveRun active = testArchiveRun(
                        UUID.fromString("10000000-0000-0000-0000-000000000041"), 0, memberId)
                .markGeometryPlaced()
                .activate()
                .consumeRevive();
        ArchiveRun ominous = ArchiveRun.create(
                UUID.fromString("20000000-0000-0000-0000-000000000051"),
                active.seed(),
                5,
                active.members(),
                active.dungeonGraph(),
                ArchiveRunMode.OMINOUS);
        com.google.gson.JsonElement encodedRun = ArchiveRun.CODEC
                .encodeStart(JsonOps.INSTANCE, ominous)
                .getOrThrow();
        ArchiveRun decodedRun = ArchiveRun.CODEC.parse(JsonOps.INSTANCE, encodedRun).getOrThrow();
        helper.assertTrue(
                decodedRun.mode() == ArchiveRunMode.OMINOUS
                        && decodedRun.schemaRevision() == ArchiveRun.SCHEMA_REVISION,
                "Ominous Archive mode did not survive run persistence");
        storage.register(active);
        helper.assertTrue(
                active.floor() == 0L,
                "A new Archive run did not begin on visible floor 0");

        ArchiveRun floorOne = ArchiveRunManager.beginNextFloor(storage, memberId).orElseThrow();
        helper.assertTrue(
                floorOne.status() == ArchiveRunStatus.PREPARING
                        && floorOne.floor() == 1L
                        && floorOne.returnDeadlineTick() == -1L,
                "Floor victory entered an Overworld-return state instead of preparing floor 1");
        helper.assertTrue(
                floorOne.runId().equals(active.runId())
                        && floorOne.sharedRevives() == active.sharedRevives()
                        && floorOne.members().getFirst().returnPoint().equals(active.members().getFirst().returnPoint()),
                "Floor progression did not preserve durable run, party, and revive state");
        helper.assertTrue(
                floorOne.seed() != active.seed()
                        && floorOne.instanceSlot() != active.instanceSlot()
                        && !floorOne.dungeonGraph().rooms().equals(active.dungeonGraph().rooms()),
                "Floor progression did not allocate a fresh generated Archive layout");
        helper.assertTrue(
                floorOne.floorState().retiredFloors().size() == 1
                        && floorOne.floorState().retiredFloors().getFirst().instanceSlot() == active.instanceSlot(),
                "The completed floor was not durably queued for safe geometry deletion");

        ArchiveRun floorTwoActive = floorOne.markGeometryPlaced().activate();
        storage.replace(floorTwoActive);
        ArchiveRun floorTwo = ArchiveRunManager.beginNextFloor(storage, memberId).orElseThrow();
        helper.assertTrue(
                floorTwo.floor() == 2L
                        && floorTwo.status() == ArchiveRunStatus.PREPARING
                        && floorTwo.floorState().retiredFloors().size() == 2,
                "The Archive floor sequence did not continue from 0 to 1 to 2");
        com.google.gson.JsonElement encoded = ArchiveRun.CODEC.encodeStart(JsonOps.INSTANCE, floorTwo).getOrThrow();
        ArchiveRun decoded = ArchiveRun.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        helper.assertTrue(decoded.equals(floorTwo), "Archive floor and cleanup state did not survive serialization");
        ArchiveRunManager.clearRuntimeState();
        helper.succeed();
    }

    public static void archiveChoirPatternsAreDeterministic(GameTestHelper helper) {
        long seed = 0x71A5C0DEL;
        List<List<Integer>> patterns = java.util.stream.IntStream.range(0, 3)
                .mapToObj(phase -> ArchiveEncounterManager.choirPattern(seed, phase))
                .toList();
        for (int phase = 0; phase < patterns.size(); phase++) {
            List<Integer> pattern = patterns.get(phase);
            helper.assertTrue(
                    pattern.equals(ArchiveEncounterManager.choirPattern(seed, phase)),
                    "Choir pattern changed for the same seed and phase");
            helper.assertTrue(
                    new HashSet<>(pattern).equals(Set.of(0, 1, 2, 3)),
                    "Choir phase is not a four-symbol permutation");
            if (phase > 0) {
                helper.assertTrue(
                        !pattern.equals(patterns.get(phase - 1)),
                        "Consecutive Choir phases repeated the same pattern");
            }
        }
        helper.assertTrue(
                !patterns.equals(java.util.stream.IntStream.range(0, 3)
                        .mapToObj(phase -> ArchiveEncounterManager.choirPattern(seed + 1L, phase))
                        .toList()),
                "Different encounter seeds generated the same complete Choir sequence");
        helper.succeed();
    }

    public static void archiveEncounterStatePersistsProgress(GameTestHelper helper) {
        ArchiveEncounterState state = ArchiveEncounterState.IDLE
                .startWithoutWave()
                .acceptPuzzleInput()
                .acceptPuzzleInput()
                .rejectPuzzleInput();
        helper.assertTrue(
                state.started() && state.puzzleCursor() == 0 && state.failures() == 1,
                "Choir input state did not retain its durable reset and failure count");
        ArchiveEncounterState wave = state
                .acceptPuzzleInput()
                .acceptPuzzleInput()
                .acceptPuzzleInput()
                .finishPuzzleSequence();
        helper.assertTrue(
                wave.waveActive() && wave.wave() == 1 && wave.puzzlePhase() == 0,
                "First Choir phase did not release its persistent wave");
        ArchiveEncounterState phaseTwo = wave.finishPuzzleWave();
        helper.assertTrue(
                !phaseTwo.waveActive() && phaseTwo.puzzlePhase() == 1 && !phaseTwo.complete(),
                "Choir wave completion did not advance to phase two");
        com.google.gson.JsonElement encoded = ArchiveEncounterState.CODEC
                .encodeStart(JsonOps.INSTANCE, phaseTwo)
                .getOrThrow();
        helper.assertTrue(
                ArchiveEncounterState.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(phaseTwo),
                "Archive encounter codec changed puzzle progress");
        ArchiveEncounterState ominousBatch = wave.nextReinforcementBatch();
        helper.assertTrue(
                ominousBatch.reinforcementBatch() == 1
                        && ArchiveEncounterState.CODEC
                                .parse(
                                        JsonOps.INSTANCE,
                                        ArchiveEncounterState.CODEC
                                                .encodeStart(JsonOps.INSTANCE, ominousBatch)
                                                .getOrThrow())
                                .getOrThrow()
                                .equals(ominousBatch),
                "Ominous reinforcement progress was not durable");

        UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000051");
        ArchiveRun active = testArchiveRun(
                        UUID.fromString("10000000-0000-0000-0000-000000000051"), 0, memberId)
                .markGeometryPlaced()
                .activate()
                .consumeRevive();
        helper.assertTrue(active.restoreRevive().sharedRevives() == ArchiveRun.MAX_SHARED_REVIVES,
                "Recalled Hour state did not restore one shared revive");
        ArchiveRun claimed = active.beginReturn(600L).claimReward(memberId);
        helper.assertTrue(
                claimed.member(memberId).orElseThrow().rewardClaimed()
                        && claimed.claimReward(memberId).equals(claimed),
                "Victory reward claim was not persisted idempotently");
        helper.succeed();
    }

    public static void fractureShrineClampsToMinHeight(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos testOrigin = helper.absolutePos(new BlockPos(40, 0, 40));
        BlockPos unsafeOrigin = new BlockPos(testOrigin.getX(), level.getMinBuildHeight(), testOrigin.getZ());
        AdventureWorldManager.placeShrine(level, unsafeOrigin, FractureShrineVariant.OBSERVATORY);
        BlockPos safeOrigin = unsafeOrigin.above();
        helper.assertTrue(
                level.getBlockState(safeOrigin).is(ModBlocks.ENGRAVED_MERIDIAN_TILE.get()),
                "Minimum-height shrine did not move its dormant center marker into the buildable range");
        helper.assertTrue(
                level.getBlockState(safeOrigin.below()).is(ArchiveRoomPlacer.ARCHIVE_RUN_PALETTE),
                "Minimum-height shrine floor was placed below the world");
        helper.succeed();
    }

    public static void registrationIsIdempotent(GameTestHelper helper) {
        TemporalSiteSavedData data = TemporalSiteManager.data(helper.getLevel());
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        TemporalSite first = data.register(origin);
        TemporalSite second = data.register(origin);
        helper.assertTrue(first.siteId().equals(second.siteId()), "Repeated registration changed the site id");
        long matches = data.all().stream().filter(site -> site.origin().equals(origin)).count();
        helper.assertTrue(matches == 1, "Repeated registration created a duplicate site");
        helper.succeed();
    }

    public static void transitionCompletes(GameTestHelper helper) {
        TemporalSite site = TemporalSite.ruin(java.util.UUID.randomUUID(), BlockPos.ZERO)
                .beginTransition(100L, 40, 7L)
                .finishIfDue(140L);
        helper.assertTrue(site.state() == TemporalState.REMEMBERED, "Ruin-to-remembered transition did not complete");
        helper.assertTrue(!site.isTransitioning(), "Completed site remained transitional");
        helper.succeed();
    }

    public static void interruptedTransitionReconciles(GameTestHelper helper) {
        TemporalSite transitioning = TemporalSite.ruin(java.util.UUID.randomUUID(), BlockPos.ZERO)
                .beginTransition(200L, 40, 9L);
        helper.assertTrue(transitioning.finishIfDue(239L).isTransitioning(), "Transition completed too early");
        helper.assertTrue(
                transitioning.finishIfDue(240L).state() == TemporalState.REMEMBERED,
                "Interrupted transition did not reconcile deterministically");
        helper.succeed();
    }

    public static void reverseTransitionCompletes(GameTestHelper helper) {
        TemporalSite site = TemporalSite.ruin(java.util.UUID.randomUUID(), BlockPos.ZERO)
                .stable(TemporalState.REMEMBERED)
                .beginTransition(300L, 40, 11L)
                .finishIfDue(340L);
        helper.assertTrue(site.state() == TemporalState.RUIN, "Remembered-to-ruin transition did not complete");
        helper.succeed();
    }

    public static void invalidStateIdFailsSafely(GameTestHelper helper) {
        boolean rejected = false;
        try {
            TemporalState.fromNetworkId(99);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Invalid network state id was accepted");
        helper.succeed();
    }

    public static void phaseGeometryRoundTrip(GameTestHelper helper) {
        BlockPos relativeOrigin = new BlockPos(2, 1, 2);
        BlockPos origin = helper.absolutePos(relativeOrigin);
        TemporalSite site = TemporalSiteManager.placePrototype(helper.getLevel(), origin);
        for (BlockPos phasePos : TemporalSiteManager.phasePositions(origin)) {
            helper.assertTrue(helper.getLevel().getBlockState(phasePos).isAir(), "Ruin phase geometry was not empty");
        }
        for (BlockPos lampPos : TemporalSiteManager.lampPositions(site)) {
            helper.assertTrue(helper.getLevel().getBlockState(lampPos).isAir(), "Ruin resonance lamp was not empty");
        }

        TemporalSite transitioning = site.beginTransition(helper.getLevel().getGameTime(), 40, 13L);
        TemporalSiteManager.data(helper.getLevel()).replace(transitioning);
        TemporalSiteManager.recover(helper.getLevel());

        helper.runAfterDelay(45L, () -> {
            for (BlockPos phasePos : TemporalSiteManager.phasePositions(origin)) {
                helper.assertTrue(
                        helper.getLevel().getBlockState(phasePos).is(ModBlocks.PHASE_PLATFORM.get()),
                        "Remembered phase platform was not applied");
            }
            for (BlockPos lampPos : TemporalSiteManager.lampPositions(site)) {
                helper.assertTrue(
                        helper.getLevel().getBlockState(lampPos).is(ModBlocks.RESONANCE_LAMP.get()),
                        "Remembered resonance lamp was not applied");
            }

            TemporalSite remembered = TemporalSiteManager.data(helper.getLevel())
                    .findNearest(origin, 32.0D)
                    .orElseThrow();
            helper.assertTrue(remembered.state() == TemporalState.REMEMBERED, "Transition did not reach remembered state");

            BlockPos occupiedRelative = relativeOrigin.offset(7, 2, 8);
            // Held so it can be removed again below. Leaving it behind poisons every
            // later run against the same GameTest world: it is a LivingEntity sitting
            // in a phase volume, so isTransitionSafe cancels the next fixture's
            // transition as "blocked late" and the failure surfaces somewhere else.
            net.minecraft.world.entity.decoration.ArmorStand obstacle =
                    helper.spawn(VanillaCompat.ARMOR_STAND, occupiedRelative);
            TemporalSite reverse = remembered.beginTransition(helper.getLevel().getGameTime(), 40, 17L);
            TemporalSiteManager.data(helper.getLevel()).replace(reverse);
            TemporalSiteManager.recover(helper.getLevel());

            helper.runAfterDelay(45L, () -> {
                TemporalSite cancelled = TemporalSiteManager.data(helper.getLevel())
                        .findNearest(origin, 32.0D)
                        .orElseThrow();
                helper.assertTrue(cancelled.state() == TemporalState.REMEMBERED, "Unsafe phase removal changed stable state");
                for (BlockPos phasePos : TemporalSiteManager.phasePositions(origin)) {
                    helper.assertTrue(
                            helper.getLevel().getBlockState(phasePos).is(ModBlocks.PHASE_PLATFORM.get()),
                            "Cancelled removal changed phase geometry");
                }
                obstacle.discard();
                helper.succeed();
            });
        });
    }

    public static void definitionRotationResolvesMarkers(GameTestHelper helper) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.parallaxAtrium();
        BlockPos origin = new BlockPos(100, 40, 200);
        BlockPos relativeLamp = new BlockPos(3, 2, 3);
        BlockPos rotated = definition.worldPosition(origin, relativeLamp, Rotation.CLOCKWISE_90);
        helper.assertTrue(
                rotated.equals(origin.offset(12, 2, 3)),
                "Clockwise site rotation did not resolve the authored marker correctly");
        helper.assertTrue(
                definition.contains(origin, Rotation.CLOCKWISE_90, rotated),
                "Rotated authored marker fell outside the site bounds");
        helper.succeed();
    }

    public static void siteCodecPreservesAuthoredState(GameTestHelper helper) {
        TemporalSite original = TemporalSite.create(
                        java.util.UUID.randomUUID(),
                        BuiltInTemporalSites.PARALLAX_ATRIUM_ID,
                        new BlockPos(12, 34, 56),
                        Rotation.COUNTERCLOCKWISE_90)
                .withProgressFlag(1)
                .stable(TemporalState.REMEMBERED);
        com.google.gson.JsonElement encoded = TemporalSite.CODEC.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        TemporalSite decoded = TemporalSite.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        helper.assertTrue(decoded.definitionId().equals(original.definitionId()), "Definition id was not preserved");
        helper.assertTrue(decoded.rotation() == original.rotation(), "Site rotation was not preserved");
        helper.assertTrue(decoded.state() == TemporalState.REMEMBERED, "Stable state was not preserved");
        helper.assertTrue(decoded.hasProgressFlag(1), "Puzzle progress flags were not preserved");
        helper.succeed();
    }

    public static void invalidDefinitionMarkerIsRejected(GameTestHelper helper) {
        boolean rejected = false;
        try {
            new TemporalSiteDefinition(
                    ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "invalid_test"),
                    16,
                    16,
                    -1,
                    8,
                    new BlockPos(8, 2, 8),
                    new BlockPos(8, 1, 4),
                    List.of(new BlockPos(16, 1, 8)),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of());
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Out-of-bounds authored marker was accepted");
        helper.succeed();
    }

    public static void alignmentLogicIsDiscreteAndResettable(GameTestHelper helper) {
        int flags = HallAlignmentPuzzle.initialise(0);
        helper.assertTrue(
                HallAlignmentPuzzle.direction(flags, 0) == Direction.NORTH,
                "The first dial did not start at its north mark");
        helper.assertTrue(!HallAlignmentPuzzle.allAligned(flags), "The initial dial pattern started solved");

        for (int index = 0; index < 3; index++) {
            int rotations = 0;
            while (!HallAlignmentPuzzle.isAligned(flags, index) && rotations++ < 4) {
                flags = HallAlignmentPuzzle.rotateClockwise(flags, index);
            }
            helper.assertTrue(HallAlignmentPuzzle.isAligned(flags, index), "A dial could not reach its target");
        }
        helper.assertTrue(HallAlignmentPuzzle.allAligned(flags), "The three aligned dials did not complete the pattern");
        flags = HallAlignmentPuzzle.markComplete(flags);
        helper.assertTrue(
                (flags & HallAlignmentPuzzle.HALL_ALIGNMENT_COMPLETE) != 0,
                "Hall completion was not recorded");

        int reset = HallAlignmentPuzzle.reset(flags);
        helper.assertTrue(!HallAlignmentPuzzle.allAligned(reset), "Reset preserved a solved dial pattern");
        helper.assertTrue(
                (reset & HallAlignmentPuzzle.HALL_ALIGNMENT_COMPLETE) == 0,
                "Reset preserved the Hall completion flag");
        helper.succeed();
    }

    public static void hallGeometryFollowsPersistentCompletion(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        TemporalSite hall = TemporalSiteManager.placeHallOfAlignment(helper.getLevel(), origin, Rotation.NONE);
        TemporalSiteDefinition hallDefinition = BuiltInTemporalSites.hallOfAlignment();
        for (int x = 0; x < hallDefinition.sizeX(); x++) {
            for (int z = 0; z < hallDefinition.sizeZ(); z++) {
                BlockPos roof = hallDefinition.worldPosition(origin, new BlockPos(x, 6, z), Rotation.NONE);
                helper.assertTrue(!helper.getLevel().getBlockState(roof).isAir(), "Overworld Hall ceiling has a gap");
            }
        }
        helper.assertTrue(
                TemporalSiteManager.isProtected(helper.getLevel(), origin.offset(1, 0, 1))
                        && TemporalSiteManager.isProtected(helper.getLevel(), origin.offset(2, 2, 2)),
                "Overworld Hall protection did not cover ordinary floor and room volume");
        TemporalSite remembered = hall.stable(TemporalState.REMEMBERED);
        TemporalSiteManager.data(helper.getLevel()).replace(remembered);
        TemporalSiteManager.recover(helper.getLevel());
        for (BlockPos dial : TemporalSiteManager.alignmentDialPositions(remembered)) {
            helper.assertTrue(
                    helper.getLevel().getBlockState(dial).is(ModBlocks.ALIGNMENT_DIAL.get()),
                    "Remembered Hall did not materialize an alignment dial");
        }

        int flags = remembered.progressFlags();
        for (int index = 0; index < 3; index++) {
            while (!HallAlignmentPuzzle.isAligned(flags, index)) {
                flags = HallAlignmentPuzzle.rotateClockwise(flags, index);
            }
        }
        flags = HallAlignmentPuzzle.markComplete(flags);
        TemporalSite solved = remembered.withProgressFlags(flags);
        TemporalSiteManager.data(helper.getLevel()).replace(solved);
        TemporalSiteManager.recover(helper.getLevel());
        for (BlockPos beam : TemporalSiteManager.alignmentBeamPositions(solved)) {
            helper.assertTrue(
                    helper.getLevel().getBlockState(beam).isAir(),
                    "An aligned mechanism still placed a physical glass beam");
        }
        for (BlockPos reward : TemporalSiteManager.ruinRewardPositions(solved)) {
            helper.assertTrue(
                    helper.getLevel().getBlockState(reward).is(ModBlocks.PHASE_PLATFORM.get()),
                    "Solved Hall did not project its crossing immediately");
        }
        BlockPos raisedCenterTarget = TemporalSiteManager.alignmentTargetPositions(solved).get(1);
        helper.assertTrue(
                helper.getLevel().getBlockState(raisedCenterTarget).is(ModBlocks.ENGRAVED_MERIDIAN_TILE.get())
                        && helper.getLevel().getBlockState(raisedCenterTarget.below()).isAir(),
                "Central Hall target still obstructed bridge headroom");

        TemporalSite ruin = solved.stable(TemporalState.RUIN);
        TemporalSiteManager.data(helper.getLevel()).replace(ruin);
        TemporalSiteManager.recover(helper.getLevel());
        for (BlockPos reward : TemporalSiteManager.ruinRewardPositions(ruin)) {
            helper.assertTrue(
                    helper.getLevel().getBlockState(reward).is(ModBlocks.PHASE_PLATFORM.get()),
                    "Solved Hall did not project its Ruin-state crossing");
        }
        for (BlockPos dial : TemporalSiteManager.alignmentDialPositions(ruin)) {
            helper.assertTrue(helper.getLevel().getBlockState(dial).isAir(), "A Hall dial remained physical in Ruin");
        }
        helper.succeed();
    }

    public static void hallMarkersRotateTogether(GameTestHelper helper) {
        TemporalSiteDefinition hall = BuiltInTemporalSites.hallOfAlignment();
        BlockPos origin = new BlockPos(40, 70, 90);
        BlockPos localDial = hall.alignmentMechanisms().getFirst().position();
        BlockPos localTarget = hall.alignmentMechanisms().getFirst().target();
        BlockPos rotatedDial = hall.worldPosition(origin, localDial, Rotation.CLOCKWISE_90);
        BlockPos rotatedTarget = hall.worldPosition(origin, localTarget, Rotation.CLOCKWISE_90);
        helper.assertTrue(hall.contains(origin, Rotation.CLOCKWISE_90, rotatedDial), "Rotated Hall dial left its bounds");
        helper.assertTrue(hall.contains(origin, Rotation.CLOCKWISE_90, rotatedTarget), "Rotated Hall target left its bounds");
        helper.assertTrue(!rotatedDial.equals(rotatedTarget), "Hall rotation collapsed a dial onto its target");
        helper.succeed();
    }

    public static void snapshotCarriesAuthoredPuzzleState(GameTestHelper helper) {
        int flags = HallAlignmentPuzzle.markComplete(HallAlignmentPuzzle.initialise(0));
        TemporalSite site = TemporalSite.create(
                        java.util.UUID.randomUUID(),
                        BuiltInTemporalSites.HALL_OF_ALIGNMENT_ID,
                        new BlockPos(14, 60, 22),
                        Rotation.CLOCKWISE_90)
                .withProgressFlags(flags)
                .stable(TemporalState.REMEMBERED);
        SiteSnapshotPayload snapshot = SiteSnapshotPayload.fromSite(site);
        helper.assertTrue(snapshot.definitionId().equals(site.definitionId()), "Snapshot lost the site definition");
        helper.assertTrue(snapshot.rotation() == site.rotation(), "Snapshot lost the site rotation");
        helper.assertTrue(snapshot.progressFlags() == flags, "Snapshot lost persistent puzzle progress");
        helper.assertTrue(
                snapshot.center().equals(BuiltInTemporalSites.hallOfAlignment()
                        .transitionCenter(site.origin(), site.rotation())),
                "Snapshot did not resolve the authored transition center");
        helper.succeed();
    }

    public static void choirHintEscalatesWithoutPunishment(GameTestHelper helper) {
        int flags = ChoirHoursPuzzle.initialise(0);
        ChoirHoursPuzzle.Submission firstFailure = ChoirHoursPuzzle.submit(flags, 1);
        helper.assertTrue(!firstFailure.correct(), "An incorrect first bell was accepted");
        helper.assertTrue(!firstFailure.showStrongHint(), "The strong hint appeared before two failures");
        helper.assertTrue(
                ChoirHoursPuzzle.failedAttempts(firstFailure.progressFlags()) == 1,
                "The first failed attempt was not counted");

        ChoirHoursPuzzle.Submission secondFailure = ChoirHoursPuzzle.submit(firstFailure.progressFlags(), 3);
        helper.assertTrue(!secondFailure.correct(), "An incorrect second bell was accepted");
        helper.assertTrue(secondFailure.showStrongHint(), "The strong hint did not appear after two failures");
        helper.assertTrue(
                ChoirHoursPuzzle.cursor(secondFailure.progressFlags()) == 0,
                "A failed attempt did not safely return to the first step");

        int progress = secondFailure.progressFlags();
        for (int bell : ChoirHoursPuzzle.sequence()) {
            progress = ChoirHoursPuzzle.submit(progress, bell).progressFlags();
        }
        helper.assertTrue(
                (progress & ChoirHoursPuzzle.CHOIR_COMPLETE) != 0,
                "The correct sequence did not complete after earlier failures");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    public static void choirCompletionPersistsAndOpensRoute(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        TemporalSite site = TemporalSiteManager.placeChoirOfHours(helper.getLevel(), origin, Rotation.NONE);
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        List<BlockPos> bells = TemporalSiteManager.choirBellPositions(site);
        for (int bellIndex : ChoirHoursPuzzle.sequence()) {
            helper.assertTrue(
                    TemporalSiteManager.ringChoirBell(player, bells.get(bellIndex)),
                    "A server-authoritative Choir input was rejected");
        }

        TemporalSite completed = TemporalSiteManager.data(helper.getLevel()).find(site.siteId()).orElseThrow();
        helper.assertTrue(
                completed.hasProgressFlag(ChoirHoursPuzzle.CHOIR_COMPLETE),
                "Choir completion was not persisted in SavedData");
        for (BlockPos reward : TemporalSiteManager.ruinRewardPositions(completed)) {
            helper.assertTrue(
                    helper.getLevel().getBlockState(reward).is(ModBlocks.PHASE_PLATFORM.get()),
                    "Choir completion did not open its Ruin route");
        }
        helper.succeed();
    }

    public static void choirPlaybackUsesVisualAndSymbolState(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        TemporalSite site = TemporalSiteManager.placeChoirOfHours(helper.getLevel(), origin, Rotation.NONE);
        TemporalSite transitioning = site.beginTransition(helper.getLevel().getGameTime(), 5, 29L);
        TemporalSiteManager.data(helper.getLevel()).replace(transitioning);
        TemporalSiteManager.recover(helper.getLevel());

        helper.runAfterDelay(18L, () -> {
            TemporalSite remembered = TemporalSiteManager.data(helper.getLevel()).find(site.siteId()).orElseThrow();
            helper.assertTrue(remembered.state() == TemporalState.REMEMBERED, "Choir did not reach Remembered state");
            boolean litBell = TemporalSiteManager.choirBellPositions(remembered).stream()
                    .map(helper.getLevel()::getBlockState)
                    .anyMatch(state -> state.is(ModBlocks.RESONANT_BELL.get())
                            && state.getValue(ResonantBellBlock.LIT));
            helper.assertTrue(litBell, "Remembered playback did not light a symbol bell");
            boolean imprintVisible = TemporalSiteManager.choirImprintPositions(remembered).stream()
                    .anyMatch(pos -> helper.getLevel().getBlockState(pos).is(ModBlocks.MEMORY_IMPRINT.get()));
            helper.assertTrue(imprintVisible, "Remembered playback did not show its non-colliding imprint");
            helper.succeed();
        });
    }

    public static void choirMarkersRotateTogether(GameTestHelper helper) {
        TemporalSiteDefinition choir = BuiltInTemporalSites.choirOfHours();
        BlockPos origin = new BlockPos(30, 64, 50);
        BlockPos bell = choir.worldPosition(
                origin,
                choir.choirBells().getFirst().position(),
                Rotation.COUNTERCLOCKWISE_90);
        BlockPos imprint = choir.worldPosition(
                origin,
                choir.choirBells().getFirst().imprintPositions().getFirst(),
                Rotation.COUNTERCLOCKWISE_90);
        helper.assertTrue(choir.contains(origin, Rotation.COUNTERCLOCKWISE_90, bell), "Rotated Choir bell left its bounds");
        helper.assertTrue(
                choir.contains(origin, Rotation.COUNTERCLOCKWISE_90, imprint),
                "Rotated Choir imprint left its bounds");
        helper.assertTrue(!bell.equals(imprint), "Choir rotation collapsed a bell onto its imprint");
        helper.succeed();
    }

    public static void meridianRelayUsesAuthoredPositions(GameTestHelper helper) {
        int flags = BrokenMeridianPuzzle.initialise(0);
        helper.assertTrue(BrokenMeridianPuzzle.position(flags) == 0, "Relay did not start at the western socket");
        BrokenMeridianPuzzle.Move unarmed = BrokenMeridianPuzzle.advance(flags);
        helper.assertTrue(unarmed.position() == 0, "An unarmed route moved the relay");
        BrokenMeridianPuzzle.Move first =
                BrokenMeridianPuzzle.advance(BrokenMeridianPuzzle.armRoute(flags));
        helper.assertTrue(first.position() == 1 && !first.complete(), "First relay move skipped the center socket");
        helper.assertTrue(!BrokenMeridianPuzzle.isRouteArmed(first.progressFlags()), "Relay move left its route armed");
        BrokenMeridianPuzzle.Move second =
                BrokenMeridianPuzzle.advance(BrokenMeridianPuzzle.armRoute(first.progressFlags()));
        helper.assertTrue(second.position() == 2 && second.complete(), "Eastern target did not complete the relay path");
        int reset = BrokenMeridianPuzzle.reset(first.progressFlags());
        helper.assertTrue(BrokenMeridianPuzzle.position(reset) == 0, "Relay reset did not restore the first socket");
        helper.assertTrue(!BrokenMeridianPuzzle.isComplete(reset), "Relay reset preserved completion");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    public static void meridianCompletionBuildsDecayedRoute(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        TemporalSite site = TemporalSiteManager.placeBrokenMeridian(helper.getLevel(), origin, Rotation.NONE);
        TemporalSite remembered = site.stable(TemporalState.REMEMBERED);
        TemporalSiteManager.data(helper.getLevel()).replace(remembered);
        TemporalSiteManager.recover(helper.getLevel());
        List<BlockPos> relays = TemporalSiteManager.meridianRelayPositions(remembered);
        helper.assertTrue(
                helper.getLevel().getBlockState(relays.getFirst()).is(ModBlocks.MERIDIAN_RELAY.get()),
                "Remembered relay did not appear at its initial authored socket");
        for (BlockPos bridge : TemporalSiteManager.phasePositions(remembered)) {
            helper.assertTrue(
                    helper.getLevel().getBlockState(bridge).is(ModBlocks.PHASE_PLATFORM.get()),
                    "Remembered first crossing was missing");
        }
        helper.assertTrue(
                TemporalSiteManager.meridianPowerChannelPositions(remembered).stream()
                        .noneMatch(pos -> helper.getLevel().getBlockState(pos).is(ModBlocks.YESTERGLASS.get())),
                "Broken Meridian still used physical glass for its live channel");
        helper.assertTrue(
                helper.getLevel()
                        .getBlockState(TemporalSiteManager.meridianPowerChannelPositions(remembered).getFirst())
                        .getValue(EngravedMeridianTileBlock.CHARGED),
                "Initial Broken Meridian channel was not visibly charged");

        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        List<BlockPos> seals = TemporalSiteManager.meridianRoutingSealPositions(remembered);
        List<BlockPos> sockets = TemporalSiteManager.meridianSocketPositions(remembered);
        helper.assertTrue(TemporalSiteManager.routeBrokenMeridian(player, seals.get(0)), "First route seal was rejected");
        helper.assertTrue(TemporalSiteManager.routeBrokenMeridian(player, sockets.get(1)), "Center socket was rejected");
        helper.assertTrue(TemporalSiteManager.routeBrokenMeridian(player, seals.get(1)), "Second route seal was rejected");
        helper.assertTrue(TemporalSiteManager.routeBrokenMeridian(player, sockets.get(2)), "Eastern socket was rejected");
        TemporalSite completed = TemporalSiteManager.data(helper.getLevel()).find(site.siteId()).orElseThrow();
        helper.assertTrue(BrokenMeridianPuzzle.isComplete(completed.progressFlags()), "Relay completion was not persisted");
        helper.assertTrue(
                helper.getLevel().getBlockState(relays.get(2)).is(ModBlocks.MERIDIAN_RELAY.get())
                        && helper.getLevel().getBlockState(relays.get(2)).getValue(MeridianRelayBlock.POWERED),
                "Target relay did not show its powered state");

        TemporalSite ruin = completed.stable(TemporalState.RUIN);
        TemporalSiteManager.data(helper.getLevel()).replace(ruin);
        TemporalSiteManager.recover(helper.getLevel());
        for (BlockPos reward : TemporalSiteManager.ruinRewardPositions(ruin)) {
            helper.assertTrue(
                    helper.getLevel().getBlockState(reward).is(ModBlocks.CRACKED_ARCHIVE_STONE.get()),
                    "The correctly positioned relay did not leave a decayed Ruin crossing");
        }
        for (BlockPos relay : relays) {
            helper.assertTrue(helper.getLevel().getBlockState(relay).isAir(), "A relay remained physical in Ruin");
        }
        helper.succeed();
    }

    @SuppressWarnings("removal")
    public static void meridianResetAndDestinationSafety(GameTestHelper helper) {
        BlockPos relativeOrigin = new BlockPos(2, 1, 2);
        BlockPos origin = helper.absolutePos(relativeOrigin);
        TemporalSite site = TemporalSiteManager.placeBrokenMeridian(helper.getLevel(), origin, Rotation.NONE)
                .stable(TemporalState.REMEMBERED);
        TemporalSiteManager.data(helper.getLevel()).replace(site);
        TemporalSiteManager.recover(helper.getLevel());
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        List<BlockPos> relays = TemporalSiteManager.meridianRelayPositions(site);
        List<BlockPos> seals = TemporalSiteManager.meridianRoutingSealPositions(site);
        List<BlockPos> sockets = TemporalSiteManager.meridianSocketPositions(site);

        net.minecraft.world.entity.decoration.ArmorStand obstacle =
                helper.spawn(VanillaCompat.ARMOR_STAND, relativeOrigin.offset(10, 1, 13));
        helper.assertTrue(
                TemporalSiteManager.routeBrokenMeridian(player, seals.getFirst()),
                "Initial route seal was not handled");
        helper.assertTrue(
                TemporalSiteManager.routeBrokenMeridian(player, sockets.get(1)),
                "Occupied destination socket was not handled");
        TemporalSite blocked = TemporalSiteManager.data(helper.getLevel()).find(site.siteId()).orElseThrow();
        helper.assertTrue(BrokenMeridianPuzzle.position(blocked.progressFlags()) == 0, "Occupied destination changed relay state");

        obstacle.discard();
        helper.assertTrue(TemporalSiteManager.routeBrokenMeridian(player, sockets.get(1)), "Safe relay movement failed");
        helper.assertTrue(
                TemporalSiteManager.resetBrokenMeridianPuzzle(player, TemporalSiteManager.anchorPosition(site)),
                "Memory Anchor reset was rejected");
        TemporalSite reset = TemporalSiteManager.data(helper.getLevel()).find(site.siteId()).orElseThrow();
        helper.assertTrue(BrokenMeridianPuzzle.position(reset.progressFlags()) == 0, "Anchor reset lost the initial socket");
        helper.assertTrue(
                helper.getLevel().getBlockState(relays.getFirst()).is(ModBlocks.MERIDIAN_RELAY.get()),
                "Anchor reset did not restore relay geometry");
        helper.succeed();
    }

    public static void meridianMarkersRotateTogether(GameTestHelper helper) {
        TemporalSiteDefinition meridian = BuiltInTemporalSites.brokenMeridian();
        BlockPos origin = new BlockPos(30, 64, 50);
        BlockPos relay = meridian.worldPosition(
                origin,
                meridian.meridianRelays().getFirst().positions().getFirst(),
                Rotation.CLOCKWISE_90);
        BlockPos channel = meridian.worldPosition(
                origin,
                meridian.meridianRelays().getFirst().powerChannels().getFirst().getFirst(),
                Rotation.CLOCKWISE_90);
        helper.assertTrue(meridian.contains(origin, Rotation.CLOCKWISE_90, relay), "Rotated relay left its site bounds");
        helper.assertTrue(meridian.contains(origin, Rotation.CLOCKWISE_90, channel), "Rotated power channel left its site bounds");
        helper.assertTrue(!relay.equals(channel), "Meridian rotation collapsed relay and channel markers");
        helper.succeed();
    }

    public static void curatorProgressIsStateDriven(GameTestHelper helper) {
        int flags = LastCuratorProgress.start(0);
        helper.assertTrue(LastCuratorProgress.health(flags) == 300, "Curator did not begin at full health");
        helper.assertTrue(
                LastCuratorProgress.phase(flags) == LastCuratorProgress.Phase.CATALOGUE,
                "Full health did not begin the Catalogue phase");
        helper.assertTrue(
                LastCuratorProgress.isVulnerable(flags, TemporalState.REMEMBERED)
                        && !LastCuratorProgress.isVulnerable(flags, TemporalState.RUIN),
                "Catalogue vulnerability was not exclusive to Remembered");

        flags = LastCuratorProgress.recordHealth(flags, 200);
        helper.assertTrue(
                LastCuratorProgress.phase(flags) == LastCuratorProgress.Phase.REVISION,
                "The 200-health threshold did not begin Revision");
        helper.assertTrue(
                LastCuratorProgress.isVulnerable(flags, TemporalState.RUIN)
                        && !LastCuratorProgress.isVulnerable(flags, TemporalState.REMEMBERED),
                "Revision vulnerability was not exclusive to Ruin");

        flags = LastCuratorProgress.recordHealth(flags, 100);
        helper.assertTrue(
                LastCuratorProgress.phase(flags) == LastCuratorProgress.Phase.ERASURE,
                "The 100-health threshold did not begin Erasure");
        helper.assertTrue(
                LastCuratorProgress.isVulnerable(flags, TemporalState.RUIN)
                        && LastCuratorProgress.isVulnerable(flags, TemporalState.REMEMBERED),
                "Erasure was not vulnerable in both stable states");
        helper.assertTrue(
                !LastCuratorProgress.isVulnerable(flags, TemporalState.TRANSITION_TO_REMEMBERED),
                "A transitioning arena exposed the Curator");

        flags = LastCuratorProgress.recordHealth(flags, 0);
        helper.assertTrue(LastCuratorProgress.isDefeated(flags), "Zero health did not persist defeat");
        flags = LastCuratorProgress.markRewardGranted(flags);
        helper.assertTrue(LastCuratorProgress.isRewardGranted(flags), "Reward grant was not persisted");
        helper.succeed();
    }

    public static void orreryGeometryFollowsTemporalState(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        TemporalSite site = TemporalSiteManager.placeGrandOrrery(helper.getLevel(), origin, Rotation.NONE);
        helper.assertTrue(
                helper.getLevel().getBlockState(TemporalSiteManager.orreryCorePositions(site).getFirst())
                        .is(ModBlocks.ARCHIVE_CORE.get()),
                "Grand Orrery Archive Core was not placed");
        helper.assertTrue(
                TemporalSiteManager.orreryAnchorPositions(site).stream()
                        .allMatch(pos -> helper.getLevel().getBlockState(pos).is(ModBlocks.MEMORY_ANCHOR.get())),
                "Grand Orrery did not place all four Memory Anchors");
        helper.assertTrue(
                TemporalSiteManager.orreryRingPositions(site).stream()
                        .allMatch(pos -> helper.getLevel().getBlockState(pos).isAir()),
                "Ruin began with remembered ring segments visible");

        TemporalSite remembered = site.stable(TemporalState.REMEMBERED);
        TemporalSiteManager.data(helper.getLevel()).replace(remembered);
        TemporalSiteManager.recover(helper.getLevel());
        helper.assertTrue(
                TemporalSiteManager.orreryRingPositions(remembered).stream()
                        .allMatch(pos -> helper.getLevel().getBlockState(pos).is(ModBlocks.MEMORY_IMPRINT.get())),
                "Remembered state did not reconstruct every Orrery ring segment");
        helper.assertTrue(
                TemporalSiteManager.phasePositions(remembered).stream()
                        .allMatch(pos -> helper.getLevel().getBlockState(pos).is(ModBlocks.PHASE_PLATFORM.get())),
                "Remembered state did not reconstruct the four arena covers");

        TemporalSiteDefinition definition = BuiltInTemporalSites.grandOrrery();
        OrreryDefinition orrery = definition.orreries().getFirst();
        BlockPos rotatedCore = definition.worldPosition(origin, orrery.archiveCore(), Rotation.CLOCKWISE_90);
        BlockPos rotatedAnchor = definition.worldPosition(
                origin, orrery.memoryAnchors().getFirst(), Rotation.CLOCKWISE_90);
        helper.assertTrue(
                definition.contains(origin, Rotation.CLOCKWISE_90, rotatedCore)
                        && definition.contains(origin, Rotation.CLOCKWISE_90, rotatedAnchor)
                        && !rotatedCore.equals(rotatedAnchor),
                "Orrery rotation did not preserve distinct authored mechanisms");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    public static void orreryCoreAndAnchorsControlEncounter(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        TemporalSite site = TemporalSiteManager.placeGrandOrrery(helper.getLevel(), origin, Rotation.NONE);
        // A transition refuses to start unless all four corners of the site are
        // loaded. In play the player standing at the Orrery holds those chunks
        // resident; the mock player below is placed at the level's shared spawn
        // instead, so nothing keeps the far corners of a 32-block site loaded and
        // one of them gets released partway through the reconstruction. That is
        // what made this test fail roughly one run in six.
        setSiteChunksForced(helper, site, true);
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(
                TemporalSiteManager.startCuratorEncounter(
                        player, TemporalSiteManager.orreryCorePositions(site).getFirst()),
                "Archive Core did not start the Curator encounter");
        TemporalSite started = TemporalSiteManager.data(helper.getLevel()).find(site.siteId()).orElseThrow();
        helper.assertTrue(
                LastCuratorProgress.isStarted(started.progressFlags()),
                "Archive Core did not persist the encounter start");
        helper.assertTrue(started.isTransitioning(), "Ruin start did not begin reconstruction");

        BlockPos anchor = TemporalSiteManager.orreryAnchorPositions(site).getFirst();
        // Polled rather than timed. The reconstruction runs for a configured
        // number of ticks, so any fixed delay here is a guess about a value the
        // test does not own; and because activateCuratorAnchor reports only that
        // the interaction was *handled* ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â it returns true even when the
        // underlying beginTransition refuses ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â the site's own state is the only
        // honest signal that the anchor did anything.
        helper.succeedWhen(() -> {
            TemporalSite current =
                    TemporalSiteManager.data(helper.getLevel()).find(site.siteId()).orElseThrow();
            helper.assertTrue(
                    current.state().isStable(), "Reconstruction has not settled yet");
            if (current.state() == TemporalState.REMEMBERED) {
                helper.assertTrue(
                        TemporalSiteManager.activateCuratorAnchor(player, anchor),
                        "Orrery Memory Anchor did not resolve to the Grand Orrery");
                current = TemporalSiteManager.data(helper.getLevel()).find(site.siteId()).orElseThrow();
            }
            helper.assertTrue(
                    current.state() == TemporalState.TRANSITION_TO_RUIN
                            || current.state() == TemporalState.RUIN,
                    "Orrery Memory Anchor did not reverse the arena toward Ruin");
            LastCuratorEncounterTracker.stop(helper.getLevel(), current, true);
            setSiteChunksForced(helper, site, false);
        });
    }

    /**
     * Hold (or release) every chunk covering an authored site.
     *
     * <p>{@code TemporalSiteManager.beginTransition} checks that the site's four
     * corners are loaded and silently refuses otherwise, which makes any test
     * that transitions a site after a delay depend on chunk residency it never
     * asked for.
     */
    private static void setSiteChunksForced(GameTestHelper helper, TemporalSite site, boolean forced) {
        TemporalSiteDefinition definition = BuiltInTemporalSites.require(site.definitionId());
        BlockPos origin = site.origin();
        List<BlockPos> corners = List.of(
                definition.worldPosition(origin, BlockPos.ZERO, site.rotation()),
                definition.worldPosition(
                        origin, new BlockPos(definition.sizeX() - 1, 0, 0), site.rotation()),
                definition.worldPosition(
                        origin, new BlockPos(0, 0, definition.sizeZ() - 1), site.rotation()),
                definition.worldPosition(
                        origin,
                        new BlockPos(definition.sizeX() - 1, 0, definition.sizeZ() - 1),
                        site.rotation()));
        int minChunkX = Integer.MAX_VALUE;
        int maxChunkX = Integer.MIN_VALUE;
        int minChunkZ = Integer.MAX_VALUE;
        int maxChunkZ = Integer.MIN_VALUE;
        for (BlockPos corner : corners) {
            int chunkX = net.minecraft.core.SectionPos.blockToSectionCoord(corner.getX());
            int chunkZ = net.minecraft.core.SectionPos.blockToSectionCoord(corner.getZ());
            minChunkX = Math.min(minChunkX, chunkX);
            maxChunkX = Math.max(maxChunkX, chunkX);
            minChunkZ = Math.min(minChunkZ, chunkZ);
            maxChunkZ = Math.max(maxChunkZ, chunkZ);
        }
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                helper.getLevel().setChunkForced(chunkX, chunkZ, forced);
            }
        }
    }

    /**
     * Block-level invariants across many seeds.
     *
     * <p>A play session produced floors whose rooms were sealed boxes floating in
     * a void with no corridors between them, and diagonal lines of blocks hanging
     * in mid-air, while {@code runDungeonSimulation} reported zero failures across
     * a thousand seeds. It could not have caught any of it: it exercises
     * {@link ArchiveRunGenerator} only and never reaches the block pipeline. This
     * test is the gate that covers that gap.
     */
    public static void archiveBlueprintHoldsAcrossSeeds(GameTestHelper helper) {
        UUID memberId = UUID.fromString("10000000-0000-0000-0000-0000000000aa");
        ArchiveReturnPoint returnPoint = new ArchiveReturnPoint(
                ResourceLocation.withDefaultNamespace("overworld"), new BlockPos(4, 80, 4), 0.0F, 0.0F);
        for (long seed = 0L; seed < 32L; seed++) {
            ArchiveDungeonGraph graph =
                    ArchiveRunGenerator.generateDungeon(seed, ArchiveDungeonSettings.DEFAULT);
            ArchiveRun run = ArchiveRun.create(
                    new UUID(0x2000000000000000L, seed),
                    seed,
                    0,
                    List.of(new ArchiveRunMember(memberId, returnPoint)),
                    graph);
            ArchiveRoomPlacer.Blueprint blueprint = ArchiveRoomPlacer.blueprint(run);
            java.util.Set<BlockPos> solid = new java.util.HashSet<>(blueprint.placements().size() * 2);
            for (ArchiveRoomPlacer.Placement placement : blueprint.placements()) {
                solid.add(placement.position());
            }

            for (ArchiveRoomPlacer.Placement placement : blueprint.placements()) {
                helper.assertTrue(
                        blueprint.bounds().isInside(placement.position()),
                        "seed " + seed + ": blueprint escaped its instance cell at " + placement.position());
            }

            // A finished floor is one connected solid. Anything that is not
            // face-reachable from the rest of it is hanging in the air.
            //
            // Counting each block's own neighbours is not enough and was tried
            // first: the stairwell emitted its ceiling two blocks at a time, so
            // every floating block had a floating partner and a per-block test
            // saw nothing wrong. Only whole-component analysis catches a clump.
            java.util.Set<BlockPos> unvisited = new java.util.HashSet<>(solid);
            java.util.List<java.util.List<BlockPos>> components = new java.util.ArrayList<>();
            while (!unvisited.isEmpty()) {
                BlockPos root = unvisited.iterator().next();
                java.util.List<BlockPos> component = new java.util.ArrayList<>();
                java.util.ArrayDeque<BlockPos> frontier = new java.util.ArrayDeque<>();
                unvisited.remove(root);
                frontier.add(root);
                while (!frontier.isEmpty()) {
                    BlockPos current = frontier.removeFirst();
                    component.add(current);
                    for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
                        BlockPos neighbour = current.relative(direction);
                        if (unvisited.remove(neighbour)) {
                            frontier.add(neighbour);
                        }
                    }
                }
                components.add(component);
            }
            if (components.size() > 1) {
                components.sort(java.util.Comparator.comparingInt(java.util.List::size));
                java.util.List<BlockPos> smallest = components.getFirst();
                helper.fail("seed " + seed + ": floor is not one connected solid - "
                        + components.size() + " separate pieces, smallest is "
                        + smallest.size() + " block(s) adrift at " + smallest.getFirst());
            }

            // Every open connection must leave a walkable channel between its two
            // rooms: floor underfoot and two blocks of headroom, the whole way.
            for (ArchiveRoomNode room : graph.rooms()) {
                for (var connection : room.connections()) {
                    if (room.index() >= connection.targetRoom()
                            || connection.direction().vertical()
                            || connection.locked()
                            || connection.hidden()) {
                        continue;
                    }
                    var first = ArchiveRoomPlacer.roomBounds(run, room.index());
                    var second = ArchiveRoomPlacer.roomBounds(run, connection.targetRoom());
                    int floorY = first.minY();
                    boolean alongZ = connection.direction() == com.nightbeam.tbos.run.ArchiveDirection.NORTH
                            || connection.direction() == com.nightbeam.tbos.run.ArchiveDirection.SOUTH;
                    int centerX = (first.minX() + first.maxX() + 1) / 2;
                    int centerZ = (first.minZ() + first.maxZ() + 1) / 2;
                    int from = alongZ
                            ? Math.min(first.maxZ(), second.maxZ())
                            : Math.min(first.maxX(), second.maxX());
                    int to = alongZ
                            ? Math.max(first.minZ(), second.minZ())
                            : Math.max(first.minX(), second.minX());
                    // Strictly between the two shells, so the rooms' own door
                    // openings are not mistaken for a gap in the corridor.
                    for (int step = from + 1; step <= to - 1; step++) {
                        BlockPos floor = alongZ
                                ? new BlockPos(centerX, floorY, step)
                                : new BlockPos(step, floorY, centerZ);
                        helper.assertTrue(
                                solid.contains(floor),
                                "seed " + seed + ": corridor has no floor at " + floor);
                        helper.assertTrue(
                                !solid.contains(floor.above()) && !solid.contains(floor.above(2)),
                                "seed " + seed + ": corridor is blocked at " + floor.above());
                    }
                }
            }
        }
        helper.succeed();
    }

    /**
     * The Curator's rebirth must fire exactly once.
     *
     * <p>Health is zeroed before each call because 1.20.1 and 1.21.1 define
     * {@code isDeadOrDying} purely as {@code health <= 0}, with no {@code dead}
     * flag, so a killing blow has to look like one on every version.
     *
     * <p>Driven through {@code die} rather than by dealing damage, because that
     * is the seam the mechanic hangs on: if the override ever stops intercepting,
     * the first blow kills and this fails immediately.
     */
    public static void phoenixGuardianRisesExactlyOnce(GameTestHelper helper) {
        combatFloor(helper, 9);
        com.nightbeam.tbos.entity.PhoenixGuardianEntity curator = helper.spawn(
                ModEntities.PHOENIX_GUARDIAN.get(),
                new Vec3(4.5D, 1.0D, 4.5D));
        curator.setNoAi(true);
        helper.assertTrue(!curator.hasRisen(), "Curator spawned already risen");

        float half = curator.getMaxHealth()
                * com.nightbeam.tbos.entity.PhoenixGuardianEntity.REBIRTH_HEALTH_FRACTION;
        curator.setHealth(0.0F);
        curator.die(helper.getLevel().damageSources().generic());
        helper.assertTrue(curator.hasRisen(), "First killing blow did not trigger the rebirth");
        helper.assertTrue(curator.isAlive(), "Curator did not survive its own rebirth");
        helper.assertTrue(curator.getHealth() == half, "Rebirth did not restore half health");

        curator.setHealth(0.0F);
        curator.die(helper.getLevel().damageSources().generic());
        helper.assertTrue(curator.isDeadOrDying(), "Second killing blow was survived");
        curator.discard();
        helper.succeed();
    }

    /**
     * A site-managed Curator must not run its own rebirth.
     *
     * <p>{@code LastCuratorEncounterTracker} owns the three-phase health curve;
     * a second life underneath it would desynchronise {@code LastCuratorProgress}.
     */
    public static void siteManagedCuratorDoesNotRise(GameTestHelper helper) {
        combatFloor(helper, 9);
        com.nightbeam.tbos.entity.PhoenixGuardianEntity curator = helper.spawn(
                ModEntities.PHOENIX_GUARDIAN.get(),
                new Vec3(4.5D, 1.0D, 4.5D));
        curator.setNoAi(true);
        curator.setSiteManaged(true);

        curator.setHealth(0.0F);
        curator.die(helper.getLevel().damageSources().generic());
        helper.assertTrue(!curator.hasRisen(), "Site-managed Curator ran its own rebirth");
        helper.assertTrue(curator.isDeadOrDying(), "Site-managed Curator survived a killing blow");
        curator.discard();
        helper.succeed();
    }

    /** The minotaur must be reachable from encounter-pool config and spawn configured. */
    public static void chamberMinotaurResolvesFromPool(GameTestHelper helper) {
        helper.assertTrue(
                com.nightbeam.tbos.run.ArchiveEnemyKind.parse("tbos:minotaur").isPresent(),
                "Minotaur is not addressable from encounter-pool config");
        combatFloor(helper, 9);
        com.nightbeam.tbos.entity.MinotaurEntity minotaur = helper.spawn(
                ModEntities.MINOTAUR.get(),
                new Vec3(4.5D, 1.0D, 4.5D));
        minotaur.setNoAi(true);
        helper.assertTrue(minotaur.isAlive(), "Minotaur did not spawn alive");
        helper.assertTrue(
                minotaur.getPhase() == com.nightbeam.tbos.entity.MinotaurEntity.Phase.IDLE,
                "Minotaur did not start in its idle phase");
        helper.assertTrue(
                minotaur.getMaxHealth() == 46.0F,
                "Minotaur did not receive its authored attributes");
        minotaur.discard();
        helper.succeed();
    }

    public static void curatorRuntimePersistsHealthAndReward(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        TemporalSite base = TemporalSiteManager.placeGrandOrrery(helper.getLevel(), origin, Rotation.NONE);
        TemporalSite active = base.withProgressFlags(LastCuratorProgress.start(0)).stable(TemporalState.REMEMBERED);
        TemporalSiteManager.data(helper.getLevel()).replace(active);
        TemporalSiteManager.recover(helper.getLevel());
        LastCuratorEncounterTracker.startIfAbsent(helper.getLevel(), active);
        LastCuratorEncounterTracker.tick(helper.getLevel().getServer());

        com.nightbeam.tbos.entity.PhoenixGuardianEntity curator =
                LastCuratorEncounterTracker.findCurator(helper.getLevel(), active).orElseThrow();
        helper.assertTrue(curator.getMaxHealth() == 300.0F, "Runtime Curator did not receive its authored health");
        curator.setHealth(195.0F);
        LastCuratorEncounterTracker.tick(helper.getLevel().getServer());
        TemporalSite revision = TemporalSiteManager.data(helper.getLevel()).find(active.siteId()).orElseThrow();
        helper.assertTrue(
                LastCuratorProgress.health(revision.progressFlags()) == 195
                        && LastCuratorProgress.phase(revision.progressFlags()) == LastCuratorProgress.Phase.REVISION,
                "Valid damage did not persist the Revision threshold");

        curator.setHealth(150.0F);
        LastCuratorEncounterTracker.tick(helper.getLevel().getServer());
        helper.assertTrue(curator.getHealth() == 195.0F, "Wrong-state damage was not reconciled safely");

        TemporalSite ruin = revision.stable(TemporalState.RUIN);
        TemporalSiteManager.data(helper.getLevel()).replace(ruin);
        curator.setHealth(95.0F);
        LastCuratorEncounterTracker.tick(helper.getLevel().getServer());
        TemporalSite erasure = TemporalSiteManager.data(helper.getLevel()).find(active.siteId()).orElseThrow();
        helper.assertTrue(
                LastCuratorProgress.phase(erasure.progressFlags()) == LastCuratorProgress.Phase.ERASURE,
                "Ruin damage did not persist the Erasure threshold");

        curator.setHealth(0.0F);
        LastCuratorEncounterTracker.tick(helper.getLevel().getServer());
        TemporalSite defeated = TemporalSiteManager.data(helper.getLevel()).find(active.siteId()).orElseThrow();
        helper.assertTrue(
                LastCuratorProgress.isDefeated(defeated.progressFlags())
                        && LastCuratorProgress.isRewardGranted(defeated.progressFlags()),
                "Curator defeat and reward flags were not persisted together");
        helper.assertTrue(
                LastCuratorEncounterTracker.rewardEntityCount(helper.getLevel(), defeated) == 1,
                "Curator did not drop exactly one reward");
        helper.assertTrue(
                LastCuratorEncounterTracker.lanternRewardEntityCount(helper.getLevel(), defeated) == 1,
                "Curator did not release exactly one Memory Lantern");
        helper.assertTrue(
                LastCuratorEncounterTracker.archiveFallPlateEntityCount(helper.getLevel(), defeated) == 1,
                "Curator did not release the Archive Fall Memory Plate");
        helper.assertTrue(
                helper.getLevel().getBlockState(TemporalSiteManager.orreryCorePositions(defeated).getFirst())
                        .is(ModBlocks.RIFT_THRESHOLD.get()),
                "Defeating the Last Curator did not transform the Archive Core into the dimension gateway");
        LastCuratorEncounterTracker.startIfAbsent(helper.getLevel(), defeated);
        LastCuratorEncounterTracker.tick(helper.getLevel().getServer());
        helper.assertTrue(
                LastCuratorEncounterTracker.rewardEntityCount(helper.getLevel(), defeated) == 1,
                "Defeated encounter duplicated its reward after reconciliation");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    public static void memoryPlateVariantsRemainDistinct(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        java.util.Set<MemoryScene> observed = new java.util.HashSet<>();
        for (MemoryScene scene : MemoryScene.values()) {
            net.minecraft.world.item.ItemStack plate = MemoryPlateItem.forScene(scene);
            helper.assertTrue(plate.is(com.nightbeam.tbos.registry.ModItems.MEMORY_PLATE.get()), "Scene used the wrong item");
            observed.add(MemoryPlateItem.scene(plate));
            player.addItem(plate);
        }
        helper.assertTrue(observed.size() == 6, "Memory Plate component collapsed distinct scenes");
        helper.assertTrue(MemoryPlateItem.hasAllScenes(player), "Inventory did not recognize all six Memory Plates");
        helper.succeed();
    }

    public static void memoryLanternPersistsPlaybackState(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(pos, ModBlocks.MEMORY_LANTERN.get().defaultBlockState(), 3);
        MemoryLanternBlockEntity lantern = (MemoryLanternBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(lantern != null, "Memory Lantern did not create its block entity");
        lantern.select(MemoryScene.FINAL_COMMAND);
        helper.assertTrue(lantern.togglePlayback(), "Loaded Memory Lantern did not start playback");

        net.minecraft.nbt.CompoundTag saved = lantern.saveWithoutMetadata(helper.getLevel().registryAccess());
        MemoryLanternBlockEntity restored = new MemoryLanternBlockEntity(pos, lantern.getBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.assertTrue(
                restored.scene().orElseThrow() == MemoryScene.FINAL_COMMAND && restored.isPlaying(),
                "Memory Lantern did not persist its selected active scene");

        for (int tick = 0; tick < MemoryLanternBlockEntity.PLAYBACK_DURATION_TICKS; tick++) {
            MemoryLanternBlockEntity.serverTick(helper.getLevel(), pos, lantern.getBlockState(), lantern);
        }
        helper.assertTrue(lantern.playbackTicks() == 0, "Complete scene did not reset its bounded playback counter");
        helper.assertTrue(lantern.scene().orElseThrow() == MemoryScene.FINAL_COMMAND, "Playback lost its selected scene");
        helper.succeed();
    }

    public static void fractureShrinesDistributeAdventureItems(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(12, 1, 12));
        Set<MemoryScene> scenes = new HashSet<>();
        for (FractureShrineVariant variant : FractureShrineVariant.values()) {
            AdventureWorldManager.placeShrine(helper.getLevel(), origin, variant);
            BlockState coffer = helper.getLevel().getBlockState(origin.offset(0, 0, 2));
            helper.assertTrue(
                    coffer.is(ModBlocks.FRACTURE_COFFER.get())
                            && coffer.getValue(FractureCofferBlock.VARIANT) == variant.ordinal()
                            && !coffer.getValue(FractureCofferBlock.OPENED),
                    variant + " did not place its sealed custom Fracture Coffer");
            helper.assertTrue(
                    helper.getLevel().getBlockState(origin).is(ModBlocks.ENGRAVED_MERIDIAN_TILE.get()),
                    variant + " placed an active dimension gateway before the Last Curator");
            List<net.minecraft.world.item.ItemStack> loot = FractureCofferBlock.lootForVariant(variant);
            helper.assertTrue(loot.get(0).is(ModItems.CRACKED_YESTERGLASS_LENS.get()),
                    variant + " did not contain a Cracked Lens");
            helper.assertTrue(loot.get(1).is(ModItems.ARCHIVE_SURVEY_MAP.get()),
                    variant + " did not contain an Archive Survey Map");
            helper.assertTrue(loot.get(2).is(ModItems.MEMORY_PLATE.get())
                            && loot.get(3).is(ModItems.MEMORY_PLATE.get()),
                    variant + " did not contain two Memory Plates");
            scenes.add(MemoryPlateItem.scene(loot.get(2)));
            scenes.add(MemoryPlateItem.scene(loot.get(3)));
            helper.assertTrue(loot.get(4).is(ModItems.CHRONICLE_SHARD.get())
                            && loot.get(5).is(ModItems.YESTERGLASS.get())
                            && loot.get(6).is(ModItems.LENSWORK_CRYSTAL.get()),
                    variant + " did not contain the custom Lens repair kit");
        }
        helper.assertTrue(scenes.size() == MemoryScene.values().length,
                "The three shrine variants did not distribute all six Memory Plate scenes exactly once");
        helper.succeed();
    }

    public static void fractureShrinesUseWorldSeededLocations(GameTestHelper helper) {
        BlockPos worldSpawn = new BlockPos(24, 80, -32);
        List<BlockPos> first = AdventureWorldManager.shrineTargets(1001L, worldSpawn);
        List<BlockPos> repeated = AdventureWorldManager.shrineTargets(1001L, worldSpawn);
        List<BlockPos> differentSeed = AdventureWorldManager.shrineTargets(2002L, worldSpawn);
        helper.assertTrue(first.equals(repeated), "The same world seed changed its Shrine locations");
        helper.assertTrue(!first.equals(differentSeed), "Different world seeds produced identical Shrine locations");
        helper.assertTrue(
                first.size() == FractureShrineVariant.values().length
                        && new HashSet<>(first).size() == first.size(),
                "World placement did not produce one distinct target per Shrine variant");
        for (BlockPos target : first) {
            double distance = Math.sqrt(worldSpawn.distSqr(target));
            helper.assertTrue(
                    distance >= AdventureWorldManager.MIN_SHRINE_DISTANCE - 1
                            && distance <= AdventureWorldManager.MAX_SHRINE_DISTANCE + 1,
                    "A Shrine target fell outside the authored world-spawn search ring");
        }
        helper.succeed();
    }

    public static void fractureShrinePlanIsStableAndBuildsOnce(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        TemporalSiteSavedData data = TemporalSiteManager.data(level);
        List<FractureShrinePlacement> saved = List.copyOf(data.fractureShrines());
        try {
            List<FractureShrinePlan> plans = AdventureWorldManager.plannedShrines(level);
            com.google.gson.JsonElement encoded =
                    FractureShrinePlan.CODEC.encodeStart(JsonOps.INSTANCE, plans.get(0)).getOrThrow();
            helper.assertTrue(
                    FractureShrinePlan.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(plans.get(0)),
                    "Fracture Shrine plan codec lost its variant or target");

            helper.assertTrue(plans.size() == FractureShrineVariant.values().length,
                    "The shrine plan did not cover every variant");
            helper.assertTrue(
                    plans.stream().map(FractureShrinePlan::variant).distinct().count() == plans.size(),
                    "The shrine plan repeated a variant");
            helper.assertTrue(AdventureWorldManager.plannedShrines(level).equals(plans),
                    "Re-reading the shrine plan produced different targets");
            for (FractureShrinePlan plan : plans) {
                helper.assertTrue(
                        plan.chunk().equals(new net.minecraft.world.level.ChunkPos(plan.target())),
                        "A shrine plan reported a chunk that does not contain its target");
            }

            FractureShrineVariant variant = plans.get(0).variant();
            data.setFractureShrines(List.of());
            helper.assertTrue(AdventureWorldManager.unbuiltShrines(level).size() == plans.size(),
                    "An unbuilt world reported shrines as already generated");
            AdventureWorldManager.registerShrine(level, helper.absolutePos(new BlockPos(2, 1, 2)), variant);
            helper.assertTrue(AdventureWorldManager.isShrineBuilt(level, variant),
                    "A registered shrine was not reported as generated");
            helper.assertTrue(!AdventureWorldManager.materializeShrine(level, plans.get(0)),
                    "A shrine variant was built a second time");
            helper.assertTrue(AdventureWorldManager.unbuiltShrines(level).size() == plans.size() - 1,
                    "Building one shrine did not remove it from the pending plan");
        } finally {
            data.setFractureShrines(saved);
        }
        helper.succeed();
    }

    public static void forcedShrineRegistersForDiscovery(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        TemporalSiteSavedData data = TemporalSiteManager.data(level);
        List<FractureShrinePlacement> saved = List.copyOf(data.fractureShrines());
        FractureShrineVariant variant = FractureShrineVariant.CURATOR_WORKSHOP;
        BlockPos origin = helper.absolutePos(new BlockPos(12, 1, 12));
        try {
            data.setFractureShrines(List.of());
            AdventureWorldManager.placeShrine(level, origin, variant);
            FractureShrinePlacement placement = AdventureWorldManager.registerShrine(level, origin, variant);
            helper.assertTrue(placement.origin().equals(origin),
                    "A forced shrine did not record the requested origin");
            helper.assertTrue(level.getBlockState(origin.offset(0, 0, 2)).is(ModBlocks.FRACTURE_COFFER.get()),
                    "A forced shrine did not build its Fracture Coffer");
            helper.assertTrue(data.fractureShrines().size() == 1,
                    "A forced shrine did not register itself for discovery");

            // Forcing the same variant again must move it, not duplicate it.
            AdventureWorldManager.registerShrine(level, origin.above(), variant);
            helper.assertTrue(data.fractureShrines().size() == 1,
                    "Re-forcing a shrine variant duplicated its placement");
            helper.assertTrue(data.fractureShrines().get(0).origin().equals(origin.above()),
                    "Re-forcing a shrine variant kept the stale origin");
        } finally {
            data.setFractureShrines(saved);
        }
        helper.succeed();
    }

    public static void onboardingGreetsEachPlayerOnce(GameTestHelper helper) {
        TemporalSiteSavedData data = TemporalSiteManager.data(helper.getLevel());
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        helper.assertTrue(data.markGreeted(first), "A first-time player was not greeted");
        helper.assertTrue(!data.markGreeted(first), "A returning player was greeted twice");
        helper.assertTrue(data.hasBeenGreeted(first), "The greeted player was not remembered");
        helper.assertTrue(!data.hasBeenGreeted(second), "An unseen player was recorded as greeted");
        helper.assertTrue(data.markGreeted(second), "A second player was not greeted independently");
        helper.succeed();
    }

    public static void utilityBlocksCarryTheirRenderAnchors(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        BlockPos dial = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos core = helper.absolutePos(new BlockPos(3, 1, 1));
        BlockPos relay = helper.absolutePos(new BlockPos(5, 1, 1));

        level.setBlock(
                dial,
                ModBlocks.ALIGNMENT_DIAL.get().defaultBlockState()
                        .setValue(AlignmentDialBlock.FACING, Direction.EAST),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.assertTrue(level.getBlockEntity(dial) instanceof AlignmentDialBlockEntity,
                "The Alignment Dial did not create its render anchor");
        helper.assertTrue(
                level.getBlockState(dial).getValue(AlignmentDialBlock.FACING) == Direction.EAST,
                "The Alignment Dial lost its facing after gaining a block entity");

        level.setBlock(
                core,
                ModBlocks.ARCHIVE_CORE.get().defaultBlockState(),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.assertTrue(level.getBlockEntity(core) instanceof ArchiveCoreBlockEntity,
                "The Archive Core did not create its render anchor");

        level.setBlock(
                relay,
                ModBlocks.MERIDIAN_RELAY.get().defaultBlockState().setValue(MeridianRelayBlock.POWERED, true),
                net.minecraft.world.level.block.Block.UPDATE_ALL);
        helper.assertTrue(level.getBlockState(relay).getValue(MeridianRelayBlock.POWERED),
                "The Meridian Relay lost its powered state");
        helper.assertTrue(level.getBlockEntity(relay) == null,
                "The Meridian Relay gained an unnecessary block entity");

        for (BlockPos pos : List.of(dial, core, relay)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
        helper.succeed();
    }

    public static void adventureWorldMetadataPersistsCoordinates(GameTestHelper helper) {
        BlockPos shrineOrigin = helper.absolutePos(new BlockPos(4, 1, 4));
        BlockPos archiveOrigin = helper.absolutePos(new BlockPos(40, 1, 40));
        FractureShrinePlacement placement = new FractureShrinePlacement(
                FractureShrineVariant.EVACUATION_GATE,
                shrineOrigin);
        com.google.gson.JsonElement encoded =
                FractureShrinePlacement.CODEC.encodeStart(JsonOps.INSTANCE, placement).getOrThrow();
        FractureShrinePlacement decoded = FractureShrinePlacement.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow();
        helper.assertTrue(decoded.equals(placement), "Fracture Shrine placement codec lost its variant or origin");

        TemporalSiteSavedData data = TemporalSiteManager.data(helper.getLevel());
        data.setFractureShrines(List.of(placement));
        data.setArchiveOrigin(archiveOrigin);
        helper.assertTrue(data.fractureShrines().equals(List.of(placement)),
                "Adventure SavedData lost the provisioned Shrine coordinates");
        helper.assertTrue(data.archiveOrigin().orElseThrow().equals(archiveOrigin),
                "Adventure SavedData lost the Meridian Archive entrance");
        helper.succeed();
    }
}
