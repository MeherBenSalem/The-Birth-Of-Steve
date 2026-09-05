package com.nightbeam.tbos.run;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.nightbeam.tbos.network.payload.ArchiveQuestPayload;
import com.nightbeam.tbos.network.payload.ArchivePuzzlePayload;
import com.nightbeam.tbos.network.payload.ArchiveFloorIntroPayload;
import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.platform.Services;

/**
 * Runtime hooks for shared revives, reconnect recovery, and void rescue.
 *
 * <p>Vanilla signatures only, with cancellation as a return value, so both
 * loader adapters drive the same decisions: NeoForge maps them onto its event
 * bus, Fabric onto Fabric API callbacks plus three mixins for the hooks Fabric
 * API does not expose.
 */
public final class ArchiveRunEvents {
    private static final Map<UUID, Long> PENDING_CHECKPOINT_RECOVERY = new HashMap<>();
    private static final Map<UUID, ServerBossEvent> RETURN_BARS = new HashMap<>();

    private ArchiveRunEvents() {
    }

    /**
     * @return false to cancel the death, keeping the player alive at half health
     *     and queueing the checkpoint teleport
     */
    public static boolean allowDeath(LivingEntity entity) {
        if (!entity.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
            return true;
        }
        if (!(entity instanceof ServerPlayer player)) {
            ArchiveEncounterManager.handleEnemyDeath(entity);
            return true;
        }
        MinecraftServer server = player.level().getServer();
        long tick = server.overworld().getGameTime();
        ArchiveRunManager.DeathResult result = ArchiveRunManager.handleDeath(
                ArchiveRunSavedData.get(server), player.getUUID(), tick);
        if (result == ArchiveRunManager.DeathResult.NOT_IN_ACTIVE_RUN) {
            return true;
        }

        player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.5F));
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        player.resetFallDistance();
        player.clearFire();
        com.nightbeam.tbos.memory.MemoryCombat.forget(player.getUUID());
        if (result == ArchiveRunManager.DeathResult.REVIVED) {
            PENDING_CHECKPOINT_RECOVERY.put(player.getUUID(), tick + 1L);
        } else if (result == ArchiveRunManager.DeathResult.RUN_FAILED) {
            player.displayClientMessage(Component.translatable("message.tbos.archive.run_failed"), true);
        }
        return false;
    }

    /** @return the heal amount after any REDUCED_HEALING room modifier */
    public static float scaleHeal(LivingEntity entity, float amount) {
        if (!(entity instanceof ServerPlayer player)
                || !player.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
            return amount;
        }
        ArchiveRun run = ArchiveRunSavedData.get(player.level().getServer())
                .findByMember(player.getUUID())
                .orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE) {
            return amount;
        }
        java.util.OptionalInt roomIndex = ArchiveRoomPlacer.roomContaining(run, player.blockPosition());
        if (ArchiveRunSavedData.get(player.level().getServer()).memories(run.runId()) != null
                && com.nightbeam.tbos.memory.MemoryService.build(player, run).debt == 2) amount *= 0.75F;
        if (roomIndex.isPresent()
                && run.dungeonGraph().room(roomIndex.getAsInt()).modifiers()
                        .contains(ArchiveRoomModifier.REDUCED_HEALING)) {
            return amount * 0.5F;
        }
        return amount;
    }

    public static void onServerTick(MinecraftServer server) {
        long tick = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Long>> pending = PENDING_CHECKPOINT_RECOVERY.entrySet().iterator();
        while (pending.hasNext()) {
            Map.Entry<UUID, Long> recovery = pending.next();
            if (recovery.getValue() > tick) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(recovery.getKey());
            if (player != null) {
                ArchiveRunManager.teleportToCheckpoint(player);
            }
            pending.remove();
        }

        ArchiveRunSavedData storage = ArchiveRunSavedData.get(server);
        ArchiveGenerationQueue.tick(server, storage);
        com.nightbeam.tbos.memory.MemoryService.tick(server);
        ArchiveDebugOverlay.tick(server, storage, tick);
        for (ArchiveRun run : storage.all()) {
            if (run.status().isReturning()) {
                ArchiveRunManager.ReturnResult result =
                        ArchiveRunManager.completeReturnIfDue(storage, run.runId(), tick);
                if (result == ArchiveRunManager.ReturnResult.COMPLETED) {
                    removeReturnBar(run.runId());
                    for (ArchiveRunMember member : run.members()) {
                        ServerPlayer player = server.getPlayerList().getPlayer(member.playerId());
                        if (player != null) {
                            ArchiveRunManager.returnMemberHome(storage, player);
                        }
                    }
                } else if (result == ArchiveRunManager.ReturnResult.NOT_DUE) {
                    updateReturnBar(server, run, tick);
                }
                continue;
            }
            if (run.status().isTerminal()) {
                for (ArchiveRunMember member : run.members()) {
                    if (member.returned()) {
                        continue;
                    }
                    ServerPlayer player = server.getPlayerList().getPlayer(member.playerId());
                    if (player != null) {
                        ArchiveRunManager.returnMemberHome(storage, player);
                    }
                }
                ArchiveRun latest = storage.find(run.runId()).orElse(run);
                if (latest.allMembersReturned() && !ArchiveRunManager.retainCompletedRuns()) {
                    ArchiveGenerationQueue.enqueueRemoval(latest);
                }
                continue;
            }
            if (run.status() != ArchiveRunStatus.ACTIVE) {
                continue;
            }
            ArchiveEncounterManager.tick(server, storage, run);
            ArchiveRun latest = storage.find(run.runId()).orElse(run);
            if (tick % 10L == 0L) {
                ArchiveQuestPayload payload = ArchiveQuestPayload.from(
                        latest.runId(), ArchiveQuestProgress.from(latest.dungeonGraph()), tick);
                for (ArchiveRunMember member : latest.members()) {
                    ServerPlayer player = server.getPlayerList().getPlayer(member.playerId());
                    if (player != null
                            && player.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
                        Services.NETWORK.sendToPlayer(player, payload);
                        Services.NETWORK.sendToPlayer(
                                player,
                                ArchiveEncounterManager.puzzlePayload(
                                        (net.minecraft.server.level.ServerLevel) player.level(),
                                        latest,
                                        player,
                                        tick));
                    }
                }
            }
            for (ArchiveRunMember member : run.members()) {
                ServerPlayer player = server.getPlayerList().getPlayer(member.playerId());
                if (player != null
                        && player.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)
                        && player.getY() < 32.0D) {
                    ArchiveRunManager.teleportToCheckpoint(player);
                }
            }
        }
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        com.nightbeam.tbos.memory.MemoryCombat.forget(player.getUUID());
        reconcileActiveMember(player, true);
    }

    public static void onPlayerChangedDimension(ServerPlayer player) {
        reconcileActiveMember(player, false);
    }

    /**
     * Classifies a break inside a run.
     *
     * <p>Returning false denies the break, which also suppresses vanilla drops.
     * That is deliberate: the crate and cache branches below release the real
     * reward through the persisted loot-claim path instead. The caller is
     * responsible for resyncing the block to the client.
     */
    public static boolean allowBreak(ServerLevel level, BlockPos position, ServerPlayer player) {
        if (!level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
            return true;
        }
        ArchiveRun run = runAt(level, position);
        if (run == null) {
            return true;
        }
        if (com.nightbeam.tbos.memory.MemorySockets.protectedCover(level,run,position)) return false;
        BlockState state = level.getBlockState(position);
        ArchiveRunProtection.Decision decision = ArchiveRunProtection.classify(run, position, state);
        if (decision == ArchiveRunProtection.Decision.OUTSIDE
                || decision == ArchiveRunProtection.Decision.BREAKABLE) {
            return true;
        }
        if (decision == ArchiveRunProtection.Decision.CRATE_PROP && player != null) {
            ArchiveEncounterManager.breakArchiveCrate(player, position);
            return false;
        }
        if ((decision == ArchiveRunProtection.Decision.ROOM_CACHE
                        || decision == ArchiveRunProtection.Decision.CANTOR_CACHE)
                && player != null) {
            ArchiveEncounterManager.breakArchiveCache(player, position);
            return false;
        }
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.tbos.archive.protected"));
        }
        return false;
    }

    /** @return false to deny the placement */
    public static boolean allowPlace(ServerLevel level, BlockPos position, Entity placer) {
        if (!level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)
                || runAt(level, position) == null) {
            return true;
        }
        if (placer instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable("message.tbos.archive.protected"));
        }
        return false;
    }

    /** Strips positions inside any allocated run from an explosion's block list. */
    public static void filterExplosion(ServerLevel level, List<BlockPos> affected) {
        if (!level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
            return;
        }
        // Players cannot bring building blocks into a run, but hostile or
        // command-spawned explosions must not become a second break path.
        affected.removeIf(position -> runAt(level, position) != null);
    }

    private static ArchiveRun runAt(ServerLevel level, BlockPos position) {
        return ArchiveRunSavedData.get(level.getServer()).all().stream()
                .filter(run -> ArchiveInstanceLayout.boundsForSlot(run.instanceSlot()).isInside(position)
                        || run.floorState().retiredFloors().stream().anyMatch(retired ->
                                ArchiveInstanceLayout.boundsForSlot(retired.instanceSlot()).isInside(position)))
                .findFirst()
                .orElse(null);
    }

    private static void reconcileActiveMember(ServerPlayer player, boolean reconnecting) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        if (storage.findPendingReturnByMember(player.getUUID()).isPresent()) {
            ArchiveRunManager.returnMemberHome(storage, player);
            return;
        }
        ArchiveRun run = storage
                .findByMember(player.getUUID())
                .orElse(null);
        if (run != null && run.status() == ArchiveRunStatus.ACTIVE) {
            ArchiveRunMember member = run.member(player.getUUID()).orElse(null);
            boolean inArchive = ArchiveDimensions.isFracturedArchive(player.level());
            // A waystone bind is an intentional leave. The run stays ACTIVE so an
            // Overworld waystone can resume it; do not yank the player back.
            if (member != null && member.waystoneBound() && !inArchive) {
                return;
            }
            boolean outsideCurrentFloor = !inArchive
                    || !ArchiveInstanceLayout.boundsForSlot(run.instanceSlot()).isInside(player.blockPosition());
            if ((reconnecting || outsideCurrentFloor) && ArchiveRunManager.teleportToCheckpoint(player)) {
                ModAdvancements.awardEnterFracturedArchive(player);
                Services.NETWORK.sendToPlayer(
                        player, new ArchiveFloorIntroPayload(run.floor(), run.mode().ominous()));
            }
        }
    }

    public static void onServerStopped(MinecraftServer server) {
        PENDING_CHECKPOINT_RECOVERY.clear();
        RETURN_BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        RETURN_BARS.clear();
        com.nightbeam.tbos.memory.MemoryService.clear();
        ArchiveRunManager.clearRuntimeState();
        ArchiveDebugOverlay.clear();
    }

    private static void updateReturnBar(MinecraftServer server, ArchiveRun run, long tick) {
        ServerBossEvent bar = RETURN_BARS.computeIfAbsent(run.runId(), ignored -> new ServerBossEvent(
                Component.empty(),
                run.status() == ArchiveRunStatus.RETURNING_VICTORY
                        ? BossEvent.BossBarColor.GREEN
                        : BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.PROGRESS));
        long remainingTicks = Math.max(0L, run.returnDeadlineTick() - tick);
        int duration = run.status() == ArchiveRunStatus.RETURNING_VICTORY
                ? ArchiveRunManager.LEGACY_VICTORY_RETURN_DELAY_TICKS
                : ArchiveRunManager.FAILURE_RETURN_DELAY_TICKS;
        int seconds = (int) ((remainingTicks + 19L) / 20L);
        bar.setName(Component.translatable("message.tbos.archive.return_countdown", seconds));
        bar.setProgress(Math.max(0.0F, Math.min(1.0F, remainingTicks / (float) duration)));
        for (ArchiveRunMember member : run.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(member.playerId());
            if (player != null && !bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
        }
    }

    private static void removeReturnBar(UUID runId) {
        ServerBossEvent bar = RETURN_BARS.remove(runId);
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }
}
