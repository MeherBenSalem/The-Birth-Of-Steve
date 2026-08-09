package com.nightbeam.tbos.neoforge;

import com.nightbeam.tbos.gametest.ModGameTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * NeoForge 1.21.1 GameTest bridge.
 *
 * <p>NeoForge scans {@link GameTestHolder} classes during its GameTest server
 * bootstrap. The target ships an all-air {@code tbos:empty} structure with the
 * 48-block isolation envelope that the newer test-instance padding supplies,
 * because 1.21.1 filters tests by template namespace rather than the newer
 * TestFunction data registry used by the 26.x targets.
 */
@GameTestHolder("tbos")
@PrefixGameTestTemplate(false)
public final class TbosNeoForgeGameTests {
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void phoenixGuardianRisesExactlyOnce(GameTestHelper helper) {
        ModGameTests.phoenixGuardianRisesExactlyOnce(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void siteManagedCuratorDoesNotRise(GameTestHelper helper) {
        ModGameTests.siteManagedCuratorDoesNotRise(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void chamberMinotaurResolvesFromPool(GameTestHelper helper) {
        ModGameTests.chamberMinotaurResolvesFromPool(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void adventureWorldMetadataPersistsCoordinates(GameTestHelper helper) {
        ModGameTests.adventureWorldMetadataPersistsCoordinates(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void alignmentLogicIsDiscreteAndResettable(GameTestHelper helper) {
        ModGameTests.alignmentLogicIsDiscreteAndResettable(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveBlueprintHoldsAcrossSeeds(GameTestHelper helper) {
        ModGameTests.archiveBlueprintHoldsAcrossSeeds(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveChoirPatternsAreDeterministic(GameTestHelper helper) {
        ModGameTests.archiveChoirPatternsAreDeterministic(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveDungeonContractIsComplete(GameTestHelper helper) {
        ModGameTests.archiveDungeonContractIsComplete(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveEncounterStatePersistsProgress(GameTestHelper helper) {
        ModGameTests.archiveEncounterStatePersistsProgress(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveFloorProgressionIsEndless(GameTestHelper helper) {
        ModGameTests.archiveFloorProgressionIsEndless(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveRoomBlueprintIsBoundedAndWalkable(GameTestHelper helper) {
        ModGameTests.archiveRoomBlueprintIsBoundedAndWalkable(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveRunCodecRoundTrip(GameTestHelper helper) {
        ModGameTests.archiveRunCodecRoundTrip(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveRunEntryValidatesBeforeMutation(GameTestHelper helper) {
        ModGameTests.archiveRunEntryValidatesBeforeMutation(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveRunGeneratorIsDeterministicAndVaried(GameTestHelper helper) {
        ModGameTests.archiveRunGeneratorIsDeterministicAndVaried(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveRunStorageIndexesActiveRuns(GameTestHelper helper) {
        ModGameTests.archiveRunStorageIndexesActiveRuns(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveSharedRevivesFailOnFourthDeath(GameTestHelper helper) {
        ModGameTests.archiveSharedRevivesFailOnFourthDeath(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void choirCompletionPersistsAndOpensRoute(GameTestHelper helper) {
        ModGameTests.choirCompletionPersistsAndOpensRoute(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void choirHintEscalatesWithoutPunishment(GameTestHelper helper) {
        ModGameTests.choirHintEscalatesWithoutPunishment(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void choirMarkersRotateTogether(GameTestHelper helper) {
        ModGameTests.choirMarkersRotateTogether(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void choirPlaybackUsesVisualAndSymbolState(GameTestHelper helper) {
        ModGameTests.choirPlaybackUsesVisualAndSymbolState(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void curatorProgressIsStateDriven(GameTestHelper helper) {
        ModGameTests.curatorProgressIsStateDriven(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void curatorRuntimePersistsHealthAndReward(GameTestHelper helper) {
        ModGameTests.curatorRuntimePersistsHealthAndReward(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void definitionRotationResolvesMarkers(GameTestHelper helper) {
        ModGameTests.definitionRotationResolvesMarkers(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void forcedShrineRegistersForDiscovery(GameTestHelper helper) {
        ModGameTests.forcedShrineRegistersForDiscovery(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void fracturedArchiveDimensionLoads(GameTestHelper helper) {
        ModGameTests.fracturedArchiveDimensionLoads(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void fractureShrineClampsToMinHeight(GameTestHelper helper) {
        ModGameTests.fractureShrineClampsToMinHeight(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void fractureShrinePlanIsStableAndBuildsOnce(GameTestHelper helper) {
        ModGameTests.fractureShrinePlanIsStableAndBuildsOnce(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void fractureShrinesDistributeAdventureItems(GameTestHelper helper) {
        ModGameTests.fractureShrinesDistributeAdventureItems(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void fractureShrinesUseWorldSeededLocations(GameTestHelper helper) {
        ModGameTests.fractureShrinesUseWorldSeededLocations(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void hallGeometryFollowsPersistentCompletion(GameTestHelper helper) {
        ModGameTests.hallGeometryFollowsPersistentCompletion(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void hallMarkersRotateTogether(GameTestHelper helper) {
        ModGameTests.hallMarkersRotateTogether(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void hourCantorRefrainSlowsAudience(GameTestHelper helper) {
        ModGameTests.hourCantorRefrainSlowsAudience(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void interruptedTransitionReconciles(GameTestHelper helper) {
        ModGameTests.interruptedTransitionReconciles(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void invalidDefinitionMarkerIsRejected(GameTestHelper helper) {
        ModGameTests.invalidDefinitionMarkerIsRejected(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void invalidStateIdFailsSafely(GameTestHelper helper) {
        ModGameTests.invalidStateIdFailsSafely(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void lenswardBeamAbortsWithoutLineOfSight(GameTestHelper helper) {
        ModGameTests.lenswardBeamAbortsWithoutLineOfSight(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void lenswardBeamStrikesOnce(GameTestHelper helper) {
        ModGameTests.lenswardBeamStrikesOnce(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void lenswardContractIsComplete(GameTestHelper helper) {
        ModGameTests.lenswardContractIsComplete(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void lenswardHoldsItsWard(GameTestHelper helper) {
        ModGameTests.lenswardHoldsItsWard(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void memoryLanternPersistsPlaybackState(GameTestHelper helper) {
        ModGameTests.memoryLanternPersistsPlaybackState(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void memoryLeechPounceSiphonsOnce(GameTestHelper helper) {
        ModGameTests.memoryLeechPounceSiphonsOnce(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void memoryPlateVariantsRemainDistinct(GameTestHelper helper) {
        ModGameTests.memoryPlateVariantsRemainDistinct(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void meridianCompletionBuildsDecayedRoute(GameTestHelper helper) {
        ModGameTests.meridianCompletionBuildsDecayedRoute(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void meridianMarkersRotateTogether(GameTestHelper helper) {
        ModGameTests.meridianMarkersRotateTogether(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void meridianRelayUsesAuthoredPositions(GameTestHelper helper) {
        ModGameTests.meridianRelayUsesAuthoredPositions(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void meridianResetAndDestinationSafety(GameTestHelper helper) {
        ModGameTests.meridianResetAndDestinationSafety(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void meridianSentinelSlamStrikesInRadius(GameTestHelper helper) {
        ModGameTests.meridianSentinelSlamStrikesInRadius(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void onboardingGreetsEachPlayerOnce(GameTestHelper helper) {
        ModGameTests.onboardingGreetsEachPlayerOnce(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void orreryCoreAndAnchorsControlEncounter(GameTestHelper helper) {
        ModGameTests.orreryCoreAndAnchorsControlEncounter(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void orreryGeometryFollowsTemporalState(GameTestHelper helper) {
        ModGameTests.orreryGeometryFollowsTemporalState(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void parallaxWraithDisplacesBehindTarget(GameTestHelper helper) {
        ModGameTests.parallaxWraithDisplacesBehindTarget(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void phaseGeometryRoundTrip(GameTestHelper helper) {
        ModGameTests.phaseGeometryRoundTrip(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void registrationIsIdempotent(GameTestHelper helper) {
        ModGameTests.registrationIsIdempotent(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void reverseTransitionCompletes(GameTestHelper helper) {
        ModGameTests.reverseTransitionCompletes(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void siteCodecPreservesAuthoredState(GameTestHelper helper) {
        ModGameTests.siteCodecPreservesAuthoredState(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void snapshotCarriesAuthoredPuzzleState(GameTestHelper helper) {
        ModGameTests.snapshotCarriesAuthoredPuzzleState(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void themeExclusiveAbilityTelegraphs(GameTestHelper helper) {
        ModGameTests.themeExclusiveAbilityTelegraphs(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void transitionCompletes(GameTestHelper helper) {
        ModGameTests.transitionCompletes(helper);
    }
    @GameTest(template = "empty", batch = "tbos", timeoutTicks = 1000)
    public void utilityBlocksCarryTheirRenderAnchors(GameTestHelper helper) {
        ModGameTests.utilityBlocksCarryTheirRenderAnchors(helper);
    }
}
