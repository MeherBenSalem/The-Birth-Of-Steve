package com.nightbeam.tbos.gametest;

import com.nightbeam.tbos.Yesterglass;
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
import com.nightbeam.tbos.block.FractureCofferBlock;
import com.nightbeam.tbos.block.MeridianRelayBlock;
import com.nightbeam.tbos.entity.MemoryLeechEntity;
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
import com.nightbeam.tbos.run.ArchiveRoomPlacer;
import com.nightbeam.tbos.run.ArchiveReturnPoint;
import com.nightbeam.tbos.run.ArchiveRoomPlan;
import com.nightbeam.tbos.run.ArchiveRun;
import com.nightbeam.tbos.run.ArchiveRunGenerator;
import com.nightbeam.tbos.run.ArchiveRunManager;
import com.nightbeam.tbos.run.ArchiveRunMember;
import com.nightbeam.tbos.run.ArchiveRunProtection;
import com.nightbeam.tbos.run.ArchiveRunSavedData;
import com.nightbeam.tbos.run.ArchiveRunStatus;
import com.nightbeam.tbos.world.AdventureWorldManager;
import com.nightbeam.tbos.world.FractureShrinePlacement;
import com.nightbeam.tbos.world.FractureShrineVariant;
import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModGameTests {
    /*
     * Several integration fixtures place authored sites up to 32 blocks wide
     * outside Minecraft's one-block "empty" test structure. Keep their worlds
     * disjoint so players and entities from parallel tests cannot trip another
     * fixture's collision-safety checks.
     */
    private static final int TEST_FIXTURE_PADDING = 48;

    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(
            BuiltInRegistries.TEST_FUNCTION, Yesterglass.MOD_ID);

    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REGISTRATION_IDEMPOTENT =
            FUNCTIONS.register("registration_idempotent", () -> ModGameTests::registrationIsIdempotent);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> TRANSITION_COMPLETES =
            FUNCTIONS.register("transition_completes", () -> ModGameTests::transitionCompletes);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INTERRUPTED_TRANSITION =
            FUNCTIONS.register("interrupted_transition", () -> ModGameTests::interruptedTransitionReconciles);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> REVERSE_TRANSITION =
            FUNCTIONS.register("reverse_transition", () -> ModGameTests::reverseTransitionCompletes);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INVALID_STATE_ID =
            FUNCTIONS.register("invalid_state_id", () -> ModGameTests::invalidStateIdFailsSafely);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> PHASE_GEOMETRY_ROUND_TRIP =
            FUNCTIONS.register("phase_geometry_round_trip", () -> ModGameTests::phaseGeometryRoundTrip);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> DEFINITION_ROTATION =
            FUNCTIONS.register("definition_rotation", () -> ModGameTests::definitionRotationResolvesMarkers);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SITE_CODEC_ROUND_TRIP =
            FUNCTIONS.register("site_codec_round_trip", () -> ModGameTests::siteCodecPreservesAuthoredState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> INVALID_DEFINITION_MARKER =
            FUNCTIONS.register("invalid_definition_marker", () -> ModGameTests::invalidDefinitionMarkerIsRejected);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ALIGNMENT_LOGIC =
            FUNCTIONS.register("alignment_logic", () -> ModGameTests::alignmentLogicIsDiscreteAndResettable);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HALL_GEOMETRY =
            FUNCTIONS.register("hall_geometry", () -> ModGameTests::hallGeometryFollowsPersistentCompletion);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HALL_ROTATION =
            FUNCTIONS.register("hall_rotation", () -> ModGameTests::hallMarkersRotateTogether);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> SNAPSHOT_METADATA =
            FUNCTIONS.register("snapshot_metadata", () -> ModGameTests::snapshotCarriesAuthoredPuzzleState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CHOIR_LOGIC =
            FUNCTIONS.register("choir_logic", () -> ModGameTests::choirHintEscalatesWithoutPunishment);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CHOIR_WORLD_COMPLETION =
            FUNCTIONS.register("choir_world_completion", () -> ModGameTests::choirCompletionPersistsAndOpensRoute);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CHOIR_PLAYBACK =
            FUNCTIONS.register("choir_playback", () -> ModGameTests::choirPlaybackUsesVisualAndSymbolState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CHOIR_ROTATION =
            FUNCTIONS.register("choir_rotation", () -> ModGameTests::choirMarkersRotateTogether);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MERIDIAN_LOGIC =
            FUNCTIONS.register("meridian_logic", () -> ModGameTests::meridianRelayUsesAuthoredPositions);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MERIDIAN_WORLD_COMPLETION =
            FUNCTIONS.register("meridian_world_completion", () -> ModGameTests::meridianCompletionBuildsDecayedRoute);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MERIDIAN_RESET_SAFETY =
            FUNCTIONS.register("meridian_reset_safety", () -> ModGameTests::meridianResetAndDestinationSafety);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MERIDIAN_ROTATION =
            FUNCTIONS.register("meridian_rotation", () -> ModGameTests::meridianMarkersRotateTogether);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CURATOR_PROGRESS =
            FUNCTIONS.register("curator_progress", () -> ModGameTests::curatorProgressIsStateDriven);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ORRERY_GEOMETRY =
            FUNCTIONS.register("orrery_geometry", () -> ModGameTests::orreryGeometryFollowsTemporalState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ORRERY_INTERACTION =
            FUNCTIONS.register("orrery_interaction", () -> ModGameTests::orreryCoreAndAnchorsControlEncounter);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> CURATOR_RUNTIME =
            FUNCTIONS.register("curator_runtime", () -> ModGameTests::curatorRuntimePersistsHealthAndReward);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MEMORY_PLATE_VARIANTS =
            FUNCTIONS.register("memory_plate_variants", () -> ModGameTests::memoryPlateVariantsRemainDistinct);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MEMORY_LANTERN_PERSISTENCE =
            FUNCTIONS.register("memory_lantern_persistence", () -> ModGameTests::memoryLanternPersistsPlaybackState);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FRACTURE_SHRINE_VARIANTS =
            FUNCTIONS.register("fracture_shrine_variants", () -> ModGameTests::fractureShrinesDistributeAdventureItems);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FRACTURE_SHRINE_PLACEMENT =
            FUNCTIONS.register("fracture_shrine_placement", () -> ModGameTests::fractureShrinesUseWorldSeededLocations);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ADVENTURE_WORLD_METADATA =
            FUNCTIONS.register("adventure_world_metadata", () -> ModGameTests::adventureWorldMetadataPersistsCoordinates);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FRACTURED_ARCHIVE_DIMENSION =
            FUNCTIONS.register("fractured_archive_dimension", () -> ModGameTests::fracturedArchiveDimensionLoads);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARCHIVE_RUN_CODEC =
            FUNCTIONS.register("archive_run_codec", () -> ModGameTests::archiveRunCodecRoundTrip);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARCHIVE_RUN_STORAGE =
            FUNCTIONS.register("archive_run_storage", () -> ModGameTests::archiveRunStorageIndexesActiveRuns);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARCHIVE_RUN_GENERATOR =
            FUNCTIONS.register("archive_run_generator", () -> ModGameTests::archiveRunGeneratorIsDeterministicAndVaried);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARCHIVE_ROOM_BLUEPRINT =
            FUNCTIONS.register("archive_room_blueprint", () -> ModGameTests::archiveRoomBlueprintIsBoundedAndWalkable);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARCHIVE_RUN_ENTRY =
            FUNCTIONS.register("archive_run_entry", () -> ModGameTests::archiveRunEntryValidatesBeforeMutation);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARCHIVE_SHARED_REVIVES =
            FUNCTIONS.register("archive_shared_revives", () -> ModGameTests::archiveSharedRevivesFailOnFourthDeath);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARCHIVE_RETURN_STATE =
            FUNCTIONS.register("archive_return_state", () -> ModGameTests::archiveReturnStateIsDeadlineDriven);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARCHIVE_CHOIR_PATTERNS =
            FUNCTIONS.register("archive_choir_patterns", () -> ModGameTests::archiveChoirPatternsAreDeterministic);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARCHIVE_ENCOUNTER_STATE =
            FUNCTIONS.register("archive_encounter_state", () -> ModGameTests::archiveEncounterStatePersistsProgress);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> ARCHIVE_DUNGEON_CONTRACT =
            FUNCTIONS.register("archive_dungeon_contract", () -> ModGameTests::archiveDungeonContractIsComplete);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> MEMORY_LEECH_POUNCE =
            FUNCTIONS.register("memory_leech_pounce", () -> ModGameTests::memoryLeechPounceSiphonsOnce);
    private static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> FRACTURE_SHRINE_MIN_HEIGHT =
            FUNCTIONS.register("fracture_shrine_min_height", () -> ModGameTests::fractureShrineClampsToMinHeight);

    private ModGameTests() {
    }

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
    }

    public static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "default"),
                new TestEnvironmentDefinition.AllOf(List.of()));

        registerTest(event, "registration_idempotent", environment, REGISTRATION_IDEMPOTENT);
        registerTest(event, "transition_completes", environment, TRANSITION_COMPLETES);
        registerTest(event, "interrupted_transition", environment, INTERRUPTED_TRANSITION);
        registerTest(event, "reverse_transition", environment, REVERSE_TRANSITION);
        registerTest(event, "invalid_state_id", environment, INVALID_STATE_ID);
        registerTest(event, "phase_geometry_round_trip", environment, PHASE_GEOMETRY_ROUND_TRIP, 140);
        registerTest(event, "definition_rotation", environment, DEFINITION_ROTATION);
        registerTest(event, "site_codec_round_trip", environment, SITE_CODEC_ROUND_TRIP);
        registerTest(event, "invalid_definition_marker", environment, INVALID_DEFINITION_MARKER);
        registerTest(event, "alignment_logic", environment, ALIGNMENT_LOGIC);
        registerTest(event, "hall_geometry", environment, HALL_GEOMETRY, 80);
        registerTest(event, "hall_rotation", environment, HALL_ROTATION);
        registerTest(event, "snapshot_metadata", environment, SNAPSHOT_METADATA);
        registerTest(event, "choir_logic", environment, CHOIR_LOGIC);
        registerTest(event, "choir_world_completion", environment, CHOIR_WORLD_COMPLETION, 80);
        registerTest(event, "choir_playback", environment, CHOIR_PLAYBACK, 80);
        registerTest(event, "choir_rotation", environment, CHOIR_ROTATION);
        registerTest(event, "meridian_logic", environment, MERIDIAN_LOGIC);
        registerTest(event, "meridian_world_completion", environment, MERIDIAN_WORLD_COMPLETION, 80);
        registerTest(event, "meridian_reset_safety", environment, MERIDIAN_RESET_SAFETY, 80);
        registerTest(event, "meridian_rotation", environment, MERIDIAN_ROTATION);
        registerTest(event, "curator_progress", environment, CURATOR_PROGRESS);
        registerTest(event, "orrery_geometry", environment, ORRERY_GEOMETRY, 80);
        registerTest(event, "orrery_interaction", environment, ORRERY_INTERACTION, 80);
        registerTest(event, "curator_runtime", environment, CURATOR_RUNTIME, 80);
        registerTest(event, "memory_plate_variants", environment, MEMORY_PLATE_VARIANTS);
        registerTest(event, "memory_lantern_persistence", environment, MEMORY_LANTERN_PERSISTENCE);
        registerTest(event, "fracture_shrine_variants", environment, FRACTURE_SHRINE_VARIANTS);
        registerTest(event, "adventure_world_metadata", environment, ADVENTURE_WORLD_METADATA);
        registerTest(event, "fractured_archive_dimension", environment, FRACTURED_ARCHIVE_DIMENSION);
        registerTest(event, "archive_run_codec", environment, ARCHIVE_RUN_CODEC);
        registerTest(event, "archive_run_storage", environment, ARCHIVE_RUN_STORAGE);
        registerTest(event, "archive_run_generator", environment, ARCHIVE_RUN_GENERATOR);
        registerTest(event, "archive_room_blueprint", environment, ARCHIVE_ROOM_BLUEPRINT);
        registerTest(event, "archive_run_entry", environment, ARCHIVE_RUN_ENTRY);
        registerTest(event, "archive_shared_revives", environment, ARCHIVE_SHARED_REVIVES);
        registerTest(event, "archive_return_state", environment, ARCHIVE_RETURN_STATE);
        registerTest(event, "archive_choir_patterns", environment, ARCHIVE_CHOIR_PATTERNS);
        registerTest(event, "archive_encounter_state", environment, ARCHIVE_ENCOUNTER_STATE);
        registerTest(event, "archive_dungeon_contract", environment, ARCHIVE_DUNGEON_CONTRACT, 80);
        registerTest(event, "memory_leech_pounce", environment, MEMORY_LEECH_POUNCE, 120);
        registerTest(event, "fracture_shrine_min_height", environment, FRACTURE_SHRINE_MIN_HEIGHT);
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            String name,
            Holder<TestEnvironmentDefinition<?>> environment,
            DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function) {
        registerTest(event, name, environment, function, 20);
    }

    private static void registerTest(
            RegisterGameTestsEvent event,
            String name,
            Holder<TestEnvironmentDefinition<?>> environment,
            DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> function,
            int maxTicks) {
        TestData<Holder<TestEnvironmentDefinition<?>>> testData = new TestData<>(
                environment,
                Identifier.withDefaultNamespace("empty"),
                maxTicks,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                TEST_FIXTURE_PADDING);
        event.registerTest(
                Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, name),
                new FunctionGameTestInstance(function.getKey(), testData));
    }

    private static void fracturedArchiveDimensionLoads(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var resources = server.getServerResources().resourceManager();
        helper.assertTrue(
                resources.getResource(Identifier.fromNamespaceAndPath(
                                Yesterglass.MOD_ID, "dimension/fractured_archive.json"))
                        .isPresent(),
                "The fractured archive dimension definition is missing from server resources");
        helper.assertTrue(
                resources.getResource(Identifier.fromNamespaceAndPath(
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

    private static void archiveRunCodecRoundTrip(GameTestHelper helper) {
        UUID firstPlayer = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondPlayer = UUID.fromString("00000000-0000-0000-0000-000000000002");
        ArchiveReturnPoint firstReturn = new ArchiveReturnPoint(
                Identifier.withDefaultNamespace("overworld"), new BlockPos(12, 70, -8), 45.0F, 10.0F);
        ArchiveReturnPoint secondReturn = new ArchiveReturnPoint(
                Identifier.withDefaultNamespace("overworld"), new BlockPos(14, 70, -8), 90.0F, 0.0F);
        List<ArchiveRunMember> members = List.of(
                new ArchiveRunMember(firstPlayer, firstReturn),
                new ArchiveRunMember(secondPlayer, secondReturn));
        List<ArchiveRoomPlan> rooms = java.util.stream.IntStream.range(0, 9)
                .mapToObj(index -> new ArchiveRoomPlan(
                        Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "test_room_" + index),
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

    private static void archiveRunStorageIndexesActiveRuns(GameTestHelper helper) {
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
                Identifier.withDefaultNamespace("overworld"), new BlockPos(0, 72, 0), 0.0F, 0.0F);
        List<ArchiveRoomPlan> rooms = java.util.stream.IntStream.range(0, 9)
                .mapToObj(index -> new ArchiveRoomPlan(
                        Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "storage_room_" + index),
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

    private static void archiveRunGeneratorIsDeterministicAndVaried(GameTestHelper helper) {
        ArchiveDungeonGraph seedEleven = ArchiveRunGenerator.generateDungeon(11L, ArchiveDungeonSettings.DEFAULT);
        ArchiveDungeonGraph repeatedEleven = ArchiveRunGenerator.generateDungeon(11L, ArchiveDungeonSettings.DEFAULT);
        ArchiveDungeonGraph seedTwelve = ArchiveRunGenerator.generateDungeon(12L, ArchiveDungeonSettings.DEFAULT);

        helper.assertTrue(seedEleven.equals(repeatedEleven), "Equal archive seeds generated different graphs");
        helper.assertTrue(!seedEleven.equals(seedTwelve), "Neighboring archive seeds generated the same graph");
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

    private static void archiveRoomBlueprintIsBoundedAndWalkable(GameTestHelper helper) {
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
                Identifier.withDefaultNamespace("overworld"), new BlockPos(4, 80, 4), 0.0F, 0.0F);
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
                        helper.assertTrue(
                                state != null && state.is(ModBlocks.ARCHIVE_SEAL.get()),
                                "Persistently locked archive door omitted its visible seal at " + door);
                    } else {
                        helper.assertTrue(
                                state == null || !state.is(ModBlocks.ARCHIVE_SEAL.get()),
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

    private static void archiveDungeonContractIsComplete(GameTestHelper helper) {
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
                                Identifier.withDefaultNamespace("overworld"), new BlockPos(0, 80, 0), 0.0F, 0.0F))),
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
                ModEntities.MEMORY_LEECH.get().create(helper.getLevel(), EntitySpawnReason.EVENT);
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
                        && ArchiveEncounterManager.abilitiesFor(ArchiveEnemyKind.MERIDIAN_SENTINEL, 13L, true)
                                .containsAll(Set.of(
                                        ArchiveEnemyAbility.MERIDIAN_SHOCKWAVE,
                                        ArchiveEnemyAbility.WARD_AURA)),
                "Archive enemies did not retain ranged, splitting, shockwave, and lesser-boss mutations");
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

        Identifier selectedLoot = ArchiveLootRoller.selectTable(
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
                                                Identifier.withDefaultNamespace("overworld"),
                                                new BlockPos(0, 80, 0),
                                                0.0F,
                                                0.0F)),
                                new ArchiveRunMember(
                                        secondMemberId,
                                        new ArchiveReturnPoint(
                                                Identifier.withDefaultNamespace("overworld"),
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
    private static void memoryLeechPounceSiphonsOnce(GameTestHelper helper) {
        for (int x = 0; x <= 9; x++) {
            for (int z = 2; z <= 6; z++) {
                helper.getLevel().setBlock(
                        helper.absolutePos(new BlockPos(x, 0, z)),
                        Blocks.STONE.defaultBlockState(),
                        3);
            }
        }

        var victim = helper.spawn(
                EntityType.SHEEP,
                new Vec3(5.5D, 1.0D, 4.5D),
                EntitySpawnReason.EVENT);
        victim.setNoAi(true);

        net.minecraft.server.level.ServerPlayer observer = helper.makeMockServerPlayerInLevel();
        Vec3 observerPosition = helper.absoluteVec(new Vec3(0.5D, 1.0D, 4.5D));
        observer.snapTo(observerPosition.x, observerPosition.y, observerPosition.z, 90.0F, 0.0F);

        MemoryLeechEntity leech = helper.spawn(
                ModEntities.MEMORY_LEECH.get(),
                new Vec3(1.5D, 1.0D, 4.5D),
                EntitySpawnReason.EVENT);
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

    @SuppressWarnings("removal")
    private static void archiveRunEntryValidatesBeforeMutation(GameTestHelper helper) {
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos threshold = player.blockPosition();
        player.setYRot(37.0F);
        player.setXRot(-12.0F);
        ArchiveReturnPoint captured = ArchiveRunManager.captureReturnPoint(player);
        helper.assertTrue(
                captured.dimension().equals(player.level().dimension().identifier())
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
        helper.assertTrue(
                ArchiveRunManager.enterFromThreshold(player, threshold)
                        == ArchiveRunManager.EntryResult.ARCHIVE_UNAVAILABLE,
                "GameTest entry did not fail safely when its archive dimension was absent");

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

    private static void archiveSharedRevivesFailOnFourthDeath(GameTestHelper helper) {
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

    private static void archiveReturnStateIsDeadlineDriven(GameTestHelper helper) {
        ArchiveRunSavedData storage = new ArchiveRunSavedData();
        UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000041");
        ArchiveRun active = testArchiveRun(
                        UUID.fromString("10000000-0000-0000-0000-000000000041"), 0, memberId)
                .markGeometryPlaced()
                .activate();
        storage.register(active);
        ArchiveRun returning = ArchiveRunManager.beginVictoryReturn(storage, memberId, 1000L).orElseThrow();
        helper.assertTrue(
                returning.status() == ArchiveRunStatus.RETURNING_VICTORY
                        && returning.returnDeadlineTick() == 1600L,
                "Victory return did not persist an exact 600-tick deadline");
        helper.assertTrue(
                ArchiveRunManager.completeReturnIfDue(storage, active.runId(), 1599L)
                        == ArchiveRunManager.ReturnResult.NOT_DUE,
                "Victory return completed one tick early");
        helper.assertTrue(
                ArchiveRunManager.completeReturnIfDue(storage, active.runId(), 1600L)
                        == ArchiveRunManager.ReturnResult.COMPLETED,
                "Victory return did not complete on its deadline");
        helper.assertTrue(
                storage.find(active.runId()).orElseThrow().status() == ArchiveRunStatus.COMPLETED,
                "Victory return did not persist terminal state before teleportation");
        helper.assertTrue(
                ArchiveRunManager.completeReturnIfDue(storage, active.runId(), 1601L)
                        == ArchiveRunManager.ReturnResult.ALREADY_TERMINAL,
                "Repeated victory return was not idempotent");

        ArchiveReturnPoint missingDimension = new ArchiveReturnPoint(
                Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "missing_test_dimension"),
                new BlockPos(99, 99, 99),
                20.0F,
                5.0F);
        ArchiveRunManager.ReturnDestination fallback = ArchiveRunManager.resolveReturnDestination(
                helper.getLevel().getServer(), missingDimension);
        helper.assertTrue(
                fallback.level() == helper.getLevel().getServer().overworld()
                        && fallback.position().equals(helper.getLevel().getServer().overworld().getRespawnData().pos()),
                "Missing return dimension did not fall back to the Overworld spawn");
        helper.succeed();
    }

    private static void archiveChoirPatternsAreDeterministic(GameTestHelper helper) {
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

    private static void archiveEncounterStatePersistsProgress(GameTestHelper helper) {
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

    private static void fractureShrineClampsToMinHeight(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos testOrigin = helper.absolutePos(new BlockPos(40, 0, 40));
        BlockPos unsafeOrigin = new BlockPos(testOrigin.getX(), level.getMinY(), testOrigin.getZ());
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

    private static void registrationIsIdempotent(GameTestHelper helper) {
        TemporalSiteSavedData data = TemporalSiteManager.data(helper.getLevel());
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        TemporalSite first = data.register(origin);
        TemporalSite second = data.register(origin);
        helper.assertTrue(first.siteId().equals(second.siteId()), "Repeated registration changed the site id");
        long matches = data.all().stream().filter(site -> site.origin().equals(origin)).count();
        helper.assertTrue(matches == 1, "Repeated registration created a duplicate site");
        helper.succeed();
    }

    private static void transitionCompletes(GameTestHelper helper) {
        TemporalSite site = TemporalSite.ruin(java.util.UUID.randomUUID(), BlockPos.ZERO)
                .beginTransition(100L, 40, 7L)
                .finishIfDue(140L);
        helper.assertTrue(site.state() == TemporalState.REMEMBERED, "Ruin-to-remembered transition did not complete");
        helper.assertTrue(!site.isTransitioning(), "Completed site remained transitional");
        helper.succeed();
    }

    private static void interruptedTransitionReconciles(GameTestHelper helper) {
        TemporalSite transitioning = TemporalSite.ruin(java.util.UUID.randomUUID(), BlockPos.ZERO)
                .beginTransition(200L, 40, 9L);
        helper.assertTrue(transitioning.finishIfDue(239L).isTransitioning(), "Transition completed too early");
        helper.assertTrue(
                transitioning.finishIfDue(240L).state() == TemporalState.REMEMBERED,
                "Interrupted transition did not reconcile deterministically");
        helper.succeed();
    }

    private static void reverseTransitionCompletes(GameTestHelper helper) {
        TemporalSite site = TemporalSite.ruin(java.util.UUID.randomUUID(), BlockPos.ZERO)
                .stable(TemporalState.REMEMBERED)
                .beginTransition(300L, 40, 11L)
                .finishIfDue(340L);
        helper.assertTrue(site.state() == TemporalState.RUIN, "Remembered-to-ruin transition did not complete");
        helper.succeed();
    }

    private static void invalidStateIdFailsSafely(GameTestHelper helper) {
        boolean rejected = false;
        try {
            TemporalState.fromNetworkId(99);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        helper.assertTrue(rejected, "Invalid network state id was accepted");
        helper.succeed();
    }

    private static void phaseGeometryRoundTrip(GameTestHelper helper) {
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
            helper.spawn(EntityType.ARMOR_STAND, occupiedRelative);
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
                helper.succeed();
            });
        });
    }

    private static void definitionRotationResolvesMarkers(GameTestHelper helper) {
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

    private static void siteCodecPreservesAuthoredState(GameTestHelper helper) {
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

    private static void invalidDefinitionMarkerIsRejected(GameTestHelper helper) {
        boolean rejected = false;
        try {
            new TemporalSiteDefinition(
                    Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "invalid_test"),
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

    private static void alignmentLogicIsDiscreteAndResettable(GameTestHelper helper) {
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

    private static void hallGeometryFollowsPersistentCompletion(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        TemporalSite hall = TemporalSiteManager.placeHallOfAlignment(helper.getLevel(), origin, Rotation.NONE);
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
                    helper.getLevel().getBlockState(beam).is(ModBlocks.YESTERGLASS.get()),
                    "An aligned mechanism did not render its solid beam");
        }

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

    private static void hallMarkersRotateTogether(GameTestHelper helper) {
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

    private static void snapshotCarriesAuthoredPuzzleState(GameTestHelper helper) {
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

    private static void choirHintEscalatesWithoutPunishment(GameTestHelper helper) {
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
    private static void choirCompletionPersistsAndOpensRoute(GameTestHelper helper) {
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

    private static void choirPlaybackUsesVisualAndSymbolState(GameTestHelper helper) {
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

    private static void choirMarkersRotateTogether(GameTestHelper helper) {
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

    private static void meridianRelayUsesAuthoredPositions(GameTestHelper helper) {
        int flags = BrokenMeridianPuzzle.initialise(0);
        helper.assertTrue(BrokenMeridianPuzzle.position(flags) == 0, "Relay did not start at the western socket");
        BrokenMeridianPuzzle.Move first = BrokenMeridianPuzzle.advance(flags);
        helper.assertTrue(first.position() == 1 && !first.complete(), "First relay move skipped the center socket");
        BrokenMeridianPuzzle.Move second = BrokenMeridianPuzzle.advance(first.progressFlags());
        helper.assertTrue(second.position() == 2 && second.complete(), "Eastern target did not complete the relay path");
        int reset = BrokenMeridianPuzzle.reset(first.progressFlags());
        helper.assertTrue(BrokenMeridianPuzzle.position(reset) == 0, "Relay reset did not restore the first socket");
        helper.assertTrue(!BrokenMeridianPuzzle.isComplete(reset), "Relay reset preserved completion");
        helper.succeed();
    }

    @SuppressWarnings("removal")
    private static void meridianCompletionBuildsDecayedRoute(GameTestHelper helper) {
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

        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        helper.assertTrue(TemporalSiteManager.moveMeridianRelay(player, relays.get(0)), "First relay move was rejected");
        helper.assertTrue(TemporalSiteManager.moveMeridianRelay(player, relays.get(1)), "Second relay move was rejected");
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
    private static void meridianResetAndDestinationSafety(GameTestHelper helper) {
        BlockPos relativeOrigin = new BlockPos(2, 1, 2);
        BlockPos origin = helper.absolutePos(relativeOrigin);
        TemporalSite site = TemporalSiteManager.placeBrokenMeridian(helper.getLevel(), origin, Rotation.NONE)
                .stable(TemporalState.REMEMBERED);
        TemporalSiteManager.data(helper.getLevel()).replace(site);
        TemporalSiteManager.recover(helper.getLevel());
        net.minecraft.server.level.ServerPlayer player = helper.makeMockServerPlayerInLevel();
        List<BlockPos> relays = TemporalSiteManager.meridianRelayPositions(site);

        net.minecraft.world.entity.decoration.ArmorStand obstacle =
                helper.spawn(EntityType.ARMOR_STAND, relativeOrigin.offset(10, 1, 13));
        helper.assertTrue(
                TemporalSiteManager.moveMeridianRelay(player, relays.getFirst()),
                "Occupied relay movement was not handled");
        TemporalSite blocked = TemporalSiteManager.data(helper.getLevel()).find(site.siteId()).orElseThrow();
        helper.assertTrue(BrokenMeridianPuzzle.position(blocked.progressFlags()) == 0, "Occupied destination changed relay state");

        obstacle.discard();
        helper.assertTrue(TemporalSiteManager.moveMeridianRelay(player, relays.getFirst()), "Safe relay movement failed");
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

    private static void meridianMarkersRotateTogether(GameTestHelper helper) {
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

    private static void curatorProgressIsStateDriven(GameTestHelper helper) {
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

    private static void orreryGeometryFollowsTemporalState(GameTestHelper helper) {
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
    private static void orreryCoreAndAnchorsControlEncounter(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        TemporalSite site = TemporalSiteManager.placeGrandOrrery(helper.getLevel(), origin, Rotation.NONE);
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

        helper.runAfterDelay(45L, () -> {
            TemporalSite remembered = TemporalSiteManager.data(helper.getLevel()).find(site.siteId()).orElseThrow();
            helper.assertTrue(remembered.state() == TemporalState.REMEMBERED, "Encounter did not settle in Remembered");
            helper.assertTrue(
                    TemporalSiteManager.activateCuratorAnchor(
                            player, TemporalSiteManager.orreryAnchorPositions(remembered).getFirst()),
                    "Orrery Memory Anchor interaction was rejected");
            TemporalSite shifting = TemporalSiteManager.data(helper.getLevel()).find(site.siteId()).orElseThrow();
            helper.assertTrue(
                    shifting.state() == TemporalState.TRANSITION_TO_RUIN
                            || shifting.state() == TemporalState.RUIN,
                    "Orrery Memory Anchor did not reverse the arena toward Ruin");
            LastCuratorEncounterTracker.stop(helper.getLevel(), shifting, true);
            helper.succeed();
        });
    }

    private static void curatorRuntimePersistsHealthAndReward(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        TemporalSite base = TemporalSiteManager.placeGrandOrrery(helper.getLevel(), origin, Rotation.NONE);
        TemporalSite active = base.withProgressFlags(LastCuratorProgress.start(0)).stable(TemporalState.REMEMBERED);
        TemporalSiteManager.data(helper.getLevel()).replace(active);
        TemporalSiteManager.recover(helper.getLevel());
        LastCuratorEncounterTracker.startIfAbsent(helper.getLevel(), active);
        LastCuratorEncounterTracker.tick(helper.getLevel().getServer());

        net.minecraft.world.entity.animal.golem.IronGolem curator =
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
    private static void memoryPlateVariantsRemainDistinct(GameTestHelper helper) {
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

    private static void memoryLanternPersistsPlaybackState(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(pos, ModBlocks.MEMORY_LANTERN.get().defaultBlockState(), 3);
        MemoryLanternBlockEntity lantern = (MemoryLanternBlockEntity) helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(lantern != null, "Memory Lantern did not create its block entity");
        lantern.select(MemoryScene.FINAL_COMMAND);
        helper.assertTrue(lantern.togglePlayback(), "Loaded Memory Lantern did not start playback");

        net.minecraft.nbt.CompoundTag saved = lantern.saveWithoutMetadata(helper.getLevel().registryAccess());
        MemoryLanternBlockEntity restored = new MemoryLanternBlockEntity(pos, lantern.getBlockState());
        restored.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(
                net.minecraft.util.ProblemReporter.DISCARDING,
                helper.getLevel().registryAccess(),
                saved));
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

    private static void fractureShrinesDistributeAdventureItems(GameTestHelper helper) {
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

    private static void fractureShrinesUseWorldSeededLocations(GameTestHelper helper) {
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

    private static void adventureWorldMetadataPersistsCoordinates(GameTestHelper helper) {
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
