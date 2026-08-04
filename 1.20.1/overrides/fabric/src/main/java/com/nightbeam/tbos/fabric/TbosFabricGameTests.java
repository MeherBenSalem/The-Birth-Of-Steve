package com.nightbeam.tbos.fabric;

import com.nightbeam.tbos.gametest.ModGameTests;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric 1.20.1 GameTest bridge.
 *
 * <p>Fabric discovers this class through the {@code fabric-gametest} entrypoint.
 * The common suite remains loader-neutral because 1.20.1 has no TestFunction
 * registry to mirror the newer data-driven registration API. The adapter uses
 * the target-local {@code tbos:empty} structure rather than Fabric API's 8x8
 * virtual empty template so parallel fixtures retain their 48-block isolation.
 */
public final class TbosFabricGameTests implements FabricGameTest {
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void adventureWorldMetadataPersistsCoordinates(GameTestHelper helper) {
        ModGameTests.adventureWorldMetadataPersistsCoordinates(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void alignmentLogicIsDiscreteAndResettable(GameTestHelper helper) {
        ModGameTests.alignmentLogicIsDiscreteAndResettable(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveBlueprintHoldsAcrossSeeds(GameTestHelper helper) {
        ModGameTests.archiveBlueprintHoldsAcrossSeeds(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveChoirPatternsAreDeterministic(GameTestHelper helper) {
        ModGameTests.archiveChoirPatternsAreDeterministic(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveDungeonContractIsComplete(GameTestHelper helper) {
        ModGameTests.archiveDungeonContractIsComplete(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveEncounterStatePersistsProgress(GameTestHelper helper) {
        ModGameTests.archiveEncounterStatePersistsProgress(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveFloorProgressionIsEndless(GameTestHelper helper) {
        ModGameTests.archiveFloorProgressionIsEndless(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveRoomBlueprintIsBoundedAndWalkable(GameTestHelper helper) {
        ModGameTests.archiveRoomBlueprintIsBoundedAndWalkable(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveRunCodecRoundTrip(GameTestHelper helper) {
        ModGameTests.archiveRunCodecRoundTrip(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveRunEntryValidatesBeforeMutation(GameTestHelper helper) {
        ModGameTests.archiveRunEntryValidatesBeforeMutation(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveRunGeneratorIsDeterministicAndVaried(GameTestHelper helper) {
        ModGameTests.archiveRunGeneratorIsDeterministicAndVaried(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveRunStorageIndexesActiveRuns(GameTestHelper helper) {
        ModGameTests.archiveRunStorageIndexesActiveRuns(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void archiveSharedRevivesFailOnFourthDeath(GameTestHelper helper) {
        ModGameTests.archiveSharedRevivesFailOnFourthDeath(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void choirCompletionPersistsAndOpensRoute(GameTestHelper helper) {
        ModGameTests.choirCompletionPersistsAndOpensRoute(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void choirHintEscalatesWithoutPunishment(GameTestHelper helper) {
        ModGameTests.choirHintEscalatesWithoutPunishment(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void choirMarkersRotateTogether(GameTestHelper helper) {
        ModGameTests.choirMarkersRotateTogether(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void choirPlaybackUsesVisualAndSymbolState(GameTestHelper helper) {
        ModGameTests.choirPlaybackUsesVisualAndSymbolState(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void curatorProgressIsStateDriven(GameTestHelper helper) {
        ModGameTests.curatorProgressIsStateDriven(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void curatorRuntimePersistsHealthAndReward(GameTestHelper helper) {
        ModGameTests.curatorRuntimePersistsHealthAndReward(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void definitionRotationResolvesMarkers(GameTestHelper helper) {
        ModGameTests.definitionRotationResolvesMarkers(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void forcedShrineRegistersForDiscovery(GameTestHelper helper) {
        ModGameTests.forcedShrineRegistersForDiscovery(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void fracturedArchiveDimensionLoads(GameTestHelper helper) {
        ModGameTests.fracturedArchiveDimensionLoads(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void fractureShrineClampsToMinHeight(GameTestHelper helper) {
        ModGameTests.fractureShrineClampsToMinHeight(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void fractureShrinePlanIsStableAndBuildsOnce(GameTestHelper helper) {
        ModGameTests.fractureShrinePlanIsStableAndBuildsOnce(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void fractureShrinesDistributeAdventureItems(GameTestHelper helper) {
        ModGameTests.fractureShrinesDistributeAdventureItems(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void fractureShrinesUseWorldSeededLocations(GameTestHelper helper) {
        ModGameTests.fractureShrinesUseWorldSeededLocations(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void hallGeometryFollowsPersistentCompletion(GameTestHelper helper) {
        ModGameTests.hallGeometryFollowsPersistentCompletion(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void hallMarkersRotateTogether(GameTestHelper helper) {
        ModGameTests.hallMarkersRotateTogether(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void hourCantorRefrainSlowsAudience(GameTestHelper helper) {
        ModGameTests.hourCantorRefrainSlowsAudience(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void interruptedTransitionReconciles(GameTestHelper helper) {
        ModGameTests.interruptedTransitionReconciles(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void invalidDefinitionMarkerIsRejected(GameTestHelper helper) {
        ModGameTests.invalidDefinitionMarkerIsRejected(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void invalidStateIdFailsSafely(GameTestHelper helper) {
        ModGameTests.invalidStateIdFailsSafely(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void lenswardBeamAbortsWithoutLineOfSight(GameTestHelper helper) {
        ModGameTests.lenswardBeamAbortsWithoutLineOfSight(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void lenswardBeamStrikesOnce(GameTestHelper helper) {
        ModGameTests.lenswardBeamStrikesOnce(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void lenswardContractIsComplete(GameTestHelper helper) {
        ModGameTests.lenswardContractIsComplete(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void lenswardHoldsItsWard(GameTestHelper helper) {
        ModGameTests.lenswardHoldsItsWard(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void memoryLanternPersistsPlaybackState(GameTestHelper helper) {
        ModGameTests.memoryLanternPersistsPlaybackState(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void memoryLeechPounceSiphonsOnce(GameTestHelper helper) {
        ModGameTests.memoryLeechPounceSiphonsOnce(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void memoryPlateVariantsRemainDistinct(GameTestHelper helper) {
        ModGameTests.memoryPlateVariantsRemainDistinct(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void meridianCompletionBuildsDecayedRoute(GameTestHelper helper) {
        ModGameTests.meridianCompletionBuildsDecayedRoute(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void meridianMarkersRotateTogether(GameTestHelper helper) {
        ModGameTests.meridianMarkersRotateTogether(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void meridianRelayUsesAuthoredPositions(GameTestHelper helper) {
        ModGameTests.meridianRelayUsesAuthoredPositions(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void meridianResetAndDestinationSafety(GameTestHelper helper) {
        ModGameTests.meridianResetAndDestinationSafety(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void meridianSentinelSlamStrikesInRadius(GameTestHelper helper) {
        ModGameTests.meridianSentinelSlamStrikesInRadius(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void onboardingGreetsEachPlayerOnce(GameTestHelper helper) {
        ModGameTests.onboardingGreetsEachPlayerOnce(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void orreryCoreAndAnchorsControlEncounter(GameTestHelper helper) {
        ModGameTests.orreryCoreAndAnchorsControlEncounter(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void orreryGeometryFollowsTemporalState(GameTestHelper helper) {
        ModGameTests.orreryGeometryFollowsTemporalState(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void parallaxWraithDisplacesBehindTarget(GameTestHelper helper) {
        ModGameTests.parallaxWraithDisplacesBehindTarget(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void phaseGeometryRoundTrip(GameTestHelper helper) {
        ModGameTests.phaseGeometryRoundTrip(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void registrationIsIdempotent(GameTestHelper helper) {
        ModGameTests.registrationIsIdempotent(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void reverseTransitionCompletes(GameTestHelper helper) {
        ModGameTests.reverseTransitionCompletes(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void siteCodecPreservesAuthoredState(GameTestHelper helper) {
        ModGameTests.siteCodecPreservesAuthoredState(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void snapshotCarriesAuthoredPuzzleState(GameTestHelper helper) {
        ModGameTests.snapshotCarriesAuthoredPuzzleState(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void themeExclusiveAbilityTelegraphs(GameTestHelper helper) {
        ModGameTests.themeExclusiveAbilityTelegraphs(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void transitionCompletes(GameTestHelper helper) {
        ModGameTests.transitionCompletes(helper);
    }
    @GameTest(template = "tbos:empty", batch = "tbos", timeoutTicks = 1000)
    public void utilityBlocksCarryTheirRenderAnchors(GameTestHelper helper) {
        ModGameTests.utilityBlocksCarryTheirRenderAnchors(helper);
    }
}
