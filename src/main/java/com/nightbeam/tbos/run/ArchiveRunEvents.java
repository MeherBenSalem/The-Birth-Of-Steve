package com.nightbeam.tbos.run;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import com.nightbeam.tbos.network.payload.ArchiveQuestPayload;
import com.nightbeam.tbos.network.payload.ArchivePuzzlePayload;

/** Runtime hooks for shared revives, reconnect recovery, and void rescue. */
public final class ArchiveRunEvents {
    private static final Map<UUID, Long> PENDING_CHECKPOINT_RECOVERY = new HashMap<>();
    private static final Map<UUID, ServerBossEvent> RETURN_BARS = new HashMap<>();

    private ArchiveRunEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ArchiveRunEvents::onLivingDeath);
        NeoForge.EVENT_BUS.addListener(ArchiveRunEvents::onLivingHeal);
        NeoForge.EVENT_BUS.addListener(ArchiveRunEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(ArchiveRunEvents::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(ArchiveRunEvents::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(ArchiveRunEvents::onBreakBlock);
        NeoForge.EVENT_BUS.addListener(ArchiveRunEvents::onPlaceBlock);
        NeoForge.EVENT_BUS.addListener(ArchiveRunEvents::onExplosion);
        NeoForge.EVENT_BUS.addListener(ArchiveRunEvents::onServerStopped);
    }

    private static void onLivingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            ArchiveEncounterManager.handleEnemyDeath(event.getEntity());
            return;
        }
        MinecraftServer server = player.level().getServer();
        long tick = server.overworld().getGameTime();
        ArchiveRunManager.DeathResult result = ArchiveRunManager.handleDeath(
                ArchiveRunSavedData.get(server), player.getUUID(), tick);
        if (result == ArchiveRunManager.DeathResult.NOT_IN_ACTIVE_RUN) {
            return;
        }

        event.setCanceled(true);
        player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.5F));
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        player.resetFallDistance();
        player.clearFire();
        if (result == ArchiveRunManager.DeathResult.REVIVED) {
            PENDING_CHECKPOINT_RECOVERY.put(player.getUUID(), tick + 1L);
        } else if (result == ArchiveRunManager.DeathResult.RUN_FAILED) {
            player.sendOverlayMessage(Component.translatable("message.tbos.archive.run_failed"));
        }
    }

    private static void onLivingHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !player.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
            return;
        }
        ArchiveRun run = ArchiveRunSavedData.get(player.level().getServer())
                .findByMember(player.getUUID())
                .orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE) {
            return;
        }
        ArchiveRoomPlacer.roomContaining(run, player.blockPosition()).ifPresent(roomIndex -> {
            if (run.dungeonGraph().room(roomIndex).modifiers()
                    .contains(ArchiveRoomModifier.REDUCED_HEALING)) {
                event.setAmount(event.getAmount() * 0.5F);
            }
        });
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
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
                        PacketDistributor.sendToPlayer(player, payload);
                        PacketDistributor.sendToPlayer(
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

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reconcileActiveMember(player);
        }
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reconcileActiveMember(player);
        }
    }

    private static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                || !level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
            return;
        }
        ArchiveRun run = runAt(level, event.getPos());
        if (run == null) {
            return;
        }
        BlockState state = level.getBlockState(event.getPos());
        ArchiveRunProtection.Decision decision =
                ArchiveRunProtection.classify(run, event.getPos(), state);
        if (decision == ArchiveRunProtection.Decision.OUTSIDE) {
            return;
        }
        event.setCanceled(true);
        event.setNotifyClient(true);
        if ((decision == ArchiveRunProtection.Decision.ROOM_CACHE
                        || decision == ArchiveRunProtection.Decision.CANTOR_CACHE)
                && event.getPlayer() instanceof ServerPlayer player) {
            ArchiveEncounterManager.breakArchiveCache(player, event.getPos());
            return;
        }
        event.getPlayer().sendSystemMessage(Component.translatable("message.tbos.archive.protected"));
    }

    private static void onPlaceBlock(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                || !level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)
                || runAt(level, event.getPos()) == null) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.translatable("message.tbos.archive.protected"));
        }
    }

    private static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                || !level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
            return;
        }
        // Players cannot bring building blocks into a run, but hostile or
        // command-spawned explosions must not become a second break path.
        event.getAffectedBlocks().removeIf(position -> runAt(level, position) != null);
    }

    private static ArchiveRun runAt(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos position) {
        return ArchiveRunSavedData.get(level.getServer()).all().stream()
                .filter(run -> ArchiveInstanceLayout.boundsForSlot(run.instanceSlot()).isInside(position))
                .findFirst()
                .orElse(null);
    }

    private static void reconcileActiveMember(ServerPlayer player) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        if (storage.findPendingReturnByMember(player.getUUID()).isPresent()) {
            ArchiveRunManager.returnMemberHome(storage, player);
            return;
        }
        ArchiveRun run = storage
                .findByMember(player.getUUID())
                .orElse(null);
        if (run != null
                && run.status() == ArchiveRunStatus.ACTIVE
                && !player.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
            ArchiveRunManager.teleportToCheckpoint(player);
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        PENDING_CHECKPOINT_RECOVERY.clear();
        RETURN_BARS.values().forEach(ServerBossEvent::removeAllPlayers);
        RETURN_BARS.clear();
        ArchiveRunManager.clearRuntimeState();
        ArchiveDebugOverlay.clear();
    }

    private static void updateReturnBar(MinecraftServer server, ArchiveRun run, long tick) {
        ServerBossEvent bar = RETURN_BARS.computeIfAbsent(run.runId(), ignored -> new ServerBossEvent(
                run.runId(),
                Component.empty(),
                run.status() == ArchiveRunStatus.RETURNING_VICTORY
                        ? BossEvent.BossBarColor.GREEN
                        : BossEvent.BossBarColor.RED,
                BossEvent.BossBarOverlay.PROGRESS));
        long remainingTicks = Math.max(0L, run.returnDeadlineTick() - tick);
        int duration = run.status() == ArchiveRunStatus.RETURNING_VICTORY
                ? ArchiveRunManager.VICTORY_RETURN_DELAY_TICKS
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
