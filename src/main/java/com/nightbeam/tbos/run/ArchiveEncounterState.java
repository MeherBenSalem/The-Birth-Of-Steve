package com.nightbeam.tbos.run;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Compact durable state for the one room that can be active in a run.
 * Puzzle patterns are derived from the room seed and therefore need no storage.
 */
public record ArchiveEncounterState(
        boolean started,
        boolean waveActive,
        int wave,
        int puzzlePhase,
        int puzzleCursor,
        int failures,
        boolean complete) {
    public static final ArchiveEncounterState IDLE = new ArchiveEncounterState(false, false, 0, 0, 0, 0, false);

    public static final Codec<ArchiveEncounterState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("started", false).forGetter(ArchiveEncounterState::started),
            Codec.BOOL.optionalFieldOf("wave_active", false).forGetter(ArchiveEncounterState::waveActive),
            Codec.INT.optionalFieldOf("wave", 0).forGetter(ArchiveEncounterState::wave),
            Codec.INT.optionalFieldOf("puzzle_phase", 0).forGetter(ArchiveEncounterState::puzzlePhase),
            Codec.INT.optionalFieldOf("puzzle_cursor", 0).forGetter(ArchiveEncounterState::puzzleCursor),
            Codec.INT.optionalFieldOf("failures", 0).forGetter(ArchiveEncounterState::failures),
            Codec.BOOL.optionalFieldOf("complete", false).forGetter(ArchiveEncounterState::complete)
    ).apply(instance, ArchiveEncounterState::new));

    public ArchiveEncounterState {
        if (wave < 0 || wave > 8) {
            throw new IllegalArgumentException("Archive encounter wave must be between 0 and 8");
        }
        if (puzzlePhase < 0 || puzzlePhase > 3) {
            throw new IllegalArgumentException("Archive puzzle phase must be between 0 and 3");
        }
        if (puzzleCursor < 0 || puzzleCursor > 4) {
            throw new IllegalArgumentException("Archive puzzle cursor must be between 0 and 4");
        }
        if (failures < 0 || failures > 255) {
            throw new IllegalArgumentException("Archive puzzle failures must be between 0 and 255");
        }
        if (!started && (waveActive || wave != 0 || puzzlePhase != 0 || puzzleCursor != 0 || failures != 0 || complete)) {
            throw new IllegalArgumentException("An idle archive encounter cannot carry progress");
        }
        if (complete && waveActive) {
            throw new IllegalArgumentException("A complete archive encounter cannot have an active wave");
        }
    }

    public ArchiveEncounterState startWave(int nextWave) {
        return new ArchiveEncounterState(true, true, nextWave, puzzlePhase, puzzleCursor, failures, false);
    }

    public ArchiveEncounterState startWithoutWave() {
        return new ArchiveEncounterState(true, false, wave, puzzlePhase, puzzleCursor, failures, false);
    }

    public ArchiveEncounterState clearWave() {
        return new ArchiveEncounterState(true, false, wave, puzzlePhase, puzzleCursor, failures, false);
    }

    public ArchiveEncounterState acceptPuzzleInput() {
        return new ArchiveEncounterState(true, waveActive, wave, puzzlePhase, puzzleCursor + 1, failures, false);
    }

    public ArchiveEncounterState rejectPuzzleInput() {
        return new ArchiveEncounterState(true, waveActive, wave, puzzlePhase, 0, Math.min(255, failures + 1), false);
    }

    public ArchiveEncounterState finishPuzzleSequence() {
        return new ArchiveEncounterState(true, true, puzzlePhase + 1, puzzlePhase, 0, failures, false);
    }

    public ArchiveEncounterState finishPuzzleWave() {
        int nextPhase = puzzlePhase + 1;
        return nextPhase >= 3
                ? markComplete()
                : new ArchiveEncounterState(true, false, wave, nextPhase, 0, failures, false);
    }

    public ArchiveEncounterState markComplete() {
        return new ArchiveEncounterState(true, false, wave, puzzlePhase, puzzleCursor, failures, true);
    }
}
