package com.nightbeam.tbos.expedition;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.config.YesterglassConfig;
import com.nightbeam.tbos.memory.MemoryRules;
import com.nightbeam.tbos.run.ArchiveDimensions;
import com.nightbeam.tbos.run.ArchiveDungeonGraph;
import com.nightbeam.tbos.run.ArchiveDungeonSettings;
import com.nightbeam.tbos.run.ArchiveGenerationQueue;
import com.nightbeam.tbos.run.ArchiveInstanceLayout;
import com.nightbeam.tbos.run.ArchiveRun;
import com.nightbeam.tbos.run.ArchiveRunGenerator;
import com.nightbeam.tbos.run.ArchiveRunManager;
import com.nightbeam.tbos.run.ArchiveRunMember;
import com.nightbeam.tbos.run.ArchiveRunMode;
import com.nightbeam.tbos.run.ArchiveRunSavedData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;

/**
 * Starts an expedition run for a party. Mirrors the Archive's own entry (a
 * persisted, queued, tick-budgeted floor) but takes its seed, party and key from
 * the expedition rather than from the Curator Gateway.
 */
public final class ExpeditionLauncher {
    public enum Result { STARTED, ALREADY_IN_RUN, UNAVAILABLE, FULL, FAILED }

    private ExpeditionLauncher() {
    }

    public static Result launch(ServerPlayer activator, ExpeditionKey key, long seed, String place, List<ServerPlayer> party) {
        MinecraftServer server = activator.level().getServer();
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(server);
        if (storage.findByMember(activator.getUUID()).isPresent()) {
            return Result.ALREADY_IN_RUN;
        }
        if (server.getLevel(ArchiveDimensions.FRACTURED_ARCHIVE) == null) {
            return Result.UNAVAILABLE;
        }
        List<ServerPlayer> members = new ArrayList<>();
        for (ServerPlayer member : party) {
            if (member == activator || storage.findByMember(member.getUUID()).isEmpty()) {
                members.add(member);
            } else {
                member.sendSystemMessage(Component.translatable("expedition.tbos.left_behind_busy"));
            }
        }
        if (members.size() > ArchiveRun.MAX_PARTY_SIZE) {
            members = members.subList(0, ArchiveRun.MAX_PARTY_SIZE);
        }
        int slot = storage.nextFreeSlot();
        if (slot >= ArchiveInstanceLayout.MAX_INSTANCE_SLOTS) {
            return Result.FULL;
        }
        UUID runId = UUID.randomUUID();
        try {
            ArchiveDungeonSettings settings;
            try {
                settings = YesterglassConfig.dungeonSettings();
            } catch (IllegalStateException exception) {
                settings = ArchiveDungeonSettings.DEFAULT;
            }
            ArchiveDungeonGraph graph = ArchiveRunGenerator.generateDungeon(seed, 0L, MemoryRules.expedition(settings));
            List<ArchiveRunMember> runMembers = members.stream()
                    .map(member -> new ArchiveRunMember(member.getUUID(), ArchiveRunManager.captureReturnPoint(member)))
                    .toList();
            boolean ominous = activator.hasEffect(MobEffects.BAD_OMEN);
            ArchiveRun run = ArchiveRun.create(runId, seed, slot, runMembers, graph,
                    ominous ? ArchiveRunMode.OMINOUS : ArchiveRunMode.NORMAL);
            storage.register(run);
            storage.startMemories(run);
            ArchiveGenerationQueue.enqueue(run);
            if (ominous) {
                activator.removeEffect(MobEffects.BAD_OMEN);
            }
            Expeditions.begin(run, key, activator, place);
            for (ServerPlayer member : members) {
                member.sendSystemMessage(Component.translatable("message.tbos.archive.preparing"));
            }
            return Result.STARTED;
        } catch (RuntimeException exception) {
            Yesterglass.LOGGER.error("Failed to prepare expedition {} (seed {})", runId, seed, exception);
            return Result.FAILED;
        }
    }

    /**
     * The run seed. The Weekly's is fixed; a Recollection's is derived from the
     * structure, so the same place always remembers the same dungeon; anything else
     * is fresh each time.
     */
    public static long seed(MinecraftServer server, ExpeditionKey key, long placeKey, boolean fresh) {
        if (key.weekly()) {
            return key.seed();
        }
        long value = server.overworld().getSeed() ^ placeKey * 0x9E3779B97F4A7C15L
                ^ (fresh ? UUID.randomUUID().getMostSignificantBits() ^ System.nanoTime() : 0x5245434FL);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
