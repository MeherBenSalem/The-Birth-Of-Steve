package com.nightbeam.tbos.run;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Standalone deterministic invariant/performance simulation used by CI and release reports. */
public final class ArchiveDungeonSimulation {
    private ArchiveDungeonSimulation() {
    }

    public static void main(String[] args) throws IOException {
        int simulations = args.length == 0 ? 1_000 : Integer.parseInt(args[0]);
        if (simulations < 1 || simulations > 100_000) {
            throw new IllegalArgumentException("Simulation count must be between 1 and 100000");
        }
        ArchiveDungeonSettings settings = ArchiveDungeonSettings.DEFAULT;
        int failed = 0;
        long rooms = 0L;
        long branches = 0L;
        long vertical = 0L;
        long loops = 0L;
        long unreachable = 0L;
        long overlaps = 0L;
        long lesserBossMismatches = 0L;
        long questGateViolations = 0L;
        long generationNanos = 0L;
        long maximumNanos = 0L;
        for (int index = 0; index < simulations; index++) {
            long seed = 0x594553544552474CL + index;
            try {
                ArchiveRunGenerator.GenerationResult generated =
                        ArchiveRunGenerator.generateDetailed(seed, settings);
                ArchiveRunGenerator.GenerationMetrics metrics = generated.metrics();
                ArchiveDungeonGraph graph = generated.graph();
                rooms += metrics.roomCount();
                branches += metrics.branchCount();
                vertical += metrics.verticalRoomCount();
                loops += metrics.loopCount();
                unreachable += metrics.unreachableRoomCount();
                overlaps += metrics.overlapCount();
                long lesserBosses = graph.rooms().stream()
                        .filter(room -> room.category() == ArchiveRoomCategory.MINI_BOSS)
                        .count();
                if (lesserBosses != ArchiveRunGenerator.lesserBossCountFor(graph.rooms().size())) {
                    lesserBossMismatches++;
                }
                boolean bossGateLocked = graph.room(graph.bossRoom()).connections().stream()
                        .filter(connection -> connection.targetRoom() != graph.rewardRoom())
                        .allMatch(ArchiveConnection::locked);
                if (!bossGateLocked || ArchiveQuestProgress.from(graph).complete()) {
                    questGateViolations++;
                }
                generationNanos += metrics.generationNanos();
                maximumNanos = Math.max(maximumNanos, metrics.generationNanos());
            } catch (RuntimeException exception) {
                failed++;
            }
        }
        double successful = Math.max(1, simulations - failed);
        String json = """
                {
                  "simulations": %d,
                  "failed_generations": %d,
                  "failed_generation_percentage": %.4f,
                  "average_room_count": %.3f,
                  "average_branch_count": %.3f,
                  "average_vertical_room_count": %.3f,
                  "average_loop_count": %.3f,
                  "unreachable_room_count": %d,
                  "overlap_count": %d,
                  "lesser_boss_count_mismatches": %d,
                  "quest_gate_violations": %d,
                  "average_generation_milliseconds": %.4f,
                  "maximum_generation_milliseconds": %.4f
                }
                """.formatted(
                simulations,
                failed,
                failed * 100.0D / simulations,
                rooms / successful,
                branches / successful,
                vertical / successful,
                loops / successful,
                unreachable,
                overlaps,
                lesserBossMismatches,
                questGateViolations,
                generationNanos / successful / 1_000_000.0D,
                maximumNanos / 1_000_000.0D);
        Path output = Path.of("build", "reports", "tbos", "archive-dungeon-simulation.json")
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(output.getParent());
        Files.writeString(output, json, StandardCharsets.UTF_8);
        System.out.println(json);
        System.out.println("Report: " + output);
        if (failed != 0 || unreachable != 0L || overlaps != 0L
                || lesserBossMismatches != 0L || questGateViolations != 0L) {
            throw new IllegalStateException("Procedural archive simulation violated a required invariant");
        }
    }
}
