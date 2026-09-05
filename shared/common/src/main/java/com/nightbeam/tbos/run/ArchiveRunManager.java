package com.nightbeam.tbos.run;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.config.YesterglassConfig;
import com.nightbeam.tbos.registry.ModItems;
import com.nightbeam.tbos.advancement.ModAdvancements;
import com.nightbeam.tbos.network.payload.ArchiveFloorIntroPayload;
import com.nightbeam.tbos.platform.Services;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;

/** The server-authoritative mutation boundary for archive entry and run allocation. */
public final class ArchiveRunManager {
    private static final double SHRINE_RING_RADIUS_SQR = 16.0D;
    public static final int FAILURE_RETURN_DELAY_TICKS = 100;
    public static final int LEGACY_VICTORY_RETURN_DELAY_TICKS = 600;
    private static final Map<UUID, Long> LAST_HANDLED_DEATH_TICK = new ConcurrentHashMap<>();

    private ArchiveRunManager() {
    }

    public static EntryResult enterFromThreshold(ServerPlayer activator, BlockPos thresholdPos) {
        if (!hasRepairedLens(activator)) {
            feedback(activator, "message.tbos.archive.lens_required");
            return EntryResult.NO_LENS;
        }
        if (!hasCuratorCore(activator)) {
            feedback(activator, "message.tbos.archive.curator_core_required");
            return EntryResult.NO_CURATOR_CORE;
        }

        MinecraftServer server = activator.level().getServer();
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(server);
        if (storage.findByMember(activator.getUUID()).isPresent()) {
            feedback(activator, "message.tbos.archive.already_in_run");
            return EntryResult.ALREADY_IN_RUN;
        }

        ServerLevel archive = server.getLevel(ArchiveDimensions.FRACTURED_ARCHIVE);
        if (archive == null) {
            feedback(activator, "message.tbos.archive.unavailable");
            return EntryResult.ARCHIVE_UNAVAILABLE;
        }

        List<ServerPlayer> party = collectParty(activator, thresholdPos);
        if (party.stream().anyMatch(member -> storage.findByMember(member.getUUID()).isPresent())) {
            feedback(activator, "message.tbos.archive.party_member_busy");
            return EntryResult.PARTY_MEMBER_BUSY;
        }
        int instanceSlot = storage.nextFreeSlot();
        if (instanceSlot >= ArchiveInstanceLayout.MAX_INSTANCE_SLOTS) {
            feedback(activator, "message.tbos.archive.full");
            return EntryResult.NO_FREE_SLOT;
        }

        UUID runId = UUID.randomUUID();
        ArchiveDungeonSettings settings = com.nightbeam.tbos.memory.MemoryRules.expedition(dungeonSettings());
        long seed = settings.rules().forcedSeed() == ArchiveDungeonRules.RANDOM_SEED
                ? mix64(server.overworld().getSeed() ^ thresholdPos.asLong()
                        ^ runId.getMostSignificantBits() ^ runId.getLeastSignificantBits())
                : settings.rules().forcedSeed();
        List<ArchiveRunMember> members = party.stream()
                .map(member -> new ArchiveRunMember(member.getUUID(), captureReturnPoint(member)))
                .toList();
        ArchiveRunMode mode = activator.hasEffect(MobEffects.BAD_OMEN)
                ? ArchiveRunMode.OMINOUS
                : ArchiveRunMode.NORMAL;
        ArchiveRun preparing;
        try {
            ArchiveRunGenerator.GenerationResult generated = ArchiveRunGenerator.generateDetailed(
                    seed, 0L, settings);
            preparing = ArchiveRun.create(runId, seed, instanceSlot, members, generated.graph(), mode);
            storage.register(preparing);
            storage.startMemories(preparing);
            ArchiveGenerationQueue.enqueue(preparing);
            if (mode.ominous()) {
                activator.removeEffect(MobEffects.BAD_OMEN);
            }
            if (debugEnabled()) {
                ArchiveRunGenerator.GenerationMetrics metrics = generated.metrics();
                Yesterglass.LOGGER.info(
                        "Generated archive graph {} seed {}: {} rooms, {} branches, {} vertical, {} loops, {} retries",
                        runId,
                        seed,
                        metrics.roomCount(),
                        metrics.branchCount(),
                        metrics.verticalRoomCount(),
                        metrics.loopCount(),
                        metrics.rejectedAttempts());
                generated.rejectedPlacements().forEach(message ->
                        Yesterglass.LOGGER.debug("Rejected archive placement for {}: {}", runId, message));
            }
        } catch (RuntimeException exception) {
            ArchiveRun registered = storage.find(runId).orElse(null);
            if (registered != null && registered.status() == ArchiveRunStatus.PREPARING) {
                storage.replace(registered.abortPreparation());
            }
            Yesterglass.LOGGER.error("Failed to prepare archive run {}", runId, exception);
            feedback(activator, "message.tbos.archive.placement_failed");
            return EntryResult.PLACEMENT_FAILED;
        }
        for (ServerPlayer member : party) {
            feedback(member, "message.tbos.archive.preparing");
        }
        return EntryResult.STARTED;
    }

    static void finishQueuedPreparation(
            MinecraftServer server,
            ArchiveRunSavedData storage,
            UUID runId,
            BlockPos spawn) {
        ArchiveRun preparing = storage.find(runId).orElse(null);
        ServerLevel archive = server.getLevel(ArchiveDimensions.FRACTURED_ARCHIVE);
        if (preparing == null || preparing.status() != ArchiveRunStatus.PREPARING || archive == null) {
            return;
        }
        archive.getChunkAt(spawn);
        if (!archive.getBlockState(spawn).isAir()
                || !archive.getBlockState(spawn.above()).isAir()
                || archive.getBlockState(spawn.below()).isAir()) {
            storage.replace(preparing.abortPreparation());
            Yesterglass.LOGGER.error(
                    "Archive floor {} for run {} has an unsafe generated spawn {}; teleport canceled",
                    preparing.floor(),
                    preparing.runId(),
                    spawn);
            return;
        }
        ArchiveRun active = preparing.markGeometryPlaced().activate();
        storage.replace(active);
        Vec3 destination = Vec3.atBottomCenterOf(spawn);
        for (ArchiveRunMember member : active.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(member.playerId());
            if (player == null) {
                continue;
            }
            player.teleport(new TeleportTransition(
                    archive,
                    destination,
                    Vec3.ZERO,
                    0.0F,
                    0.0F,
                    TeleportTransition.DO_NOTHING));
            player.resetFallDistance();
            player.clearFire();
            ModAdvancements.awardEnterFracturedArchive(player);
            Services.NETWORK.sendToPlayer(
                    player, new ArchiveFloorIntroPayload(active.floor(), active.mode().ominous()));
            player.sendOverlayMessage(Component.translatable(
                    "message.tbos.archive.entered",
                    ArchiveFloorPresentation.displayFloor(active.floor()),
                    ArchiveFloorPresentation.name(active.floor()),
                    active.sharedRevives()));
            if (active.mode().ominous()) {
                player.sendOverlayMessage(Component.translatable("message.tbos.archive.ominous_started"));
            }
        }
    }

    public static EntryResult startDebugRun(ServerPlayer player, long seed) {
        MinecraftServer server = player.level().getServer();
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(server);
        if (storage.findByMember(player.getUUID()).isPresent()) {
            return EntryResult.ALREADY_IN_RUN;
        }
        if (server.getLevel(ArchiveDimensions.FRACTURED_ARCHIVE) == null) {
            return EntryResult.ARCHIVE_UNAVAILABLE;
        }
        int instanceSlot = storage.nextFreeSlot();
        if (instanceSlot >= ArchiveInstanceLayout.MAX_INSTANCE_SLOTS) {
            return EntryResult.NO_FREE_SLOT;
        }
        try {
            UUID runId = UUID.randomUUID();
            ArchiveDungeonGraph graph = ArchiveRunGenerator.generateDungeon(seed, 0L, com.nightbeam.tbos.memory.MemoryRules.expedition(dungeonSettings()));
            ArchiveRun run = ArchiveRun.create(
                    runId,
                    seed,
                    instanceSlot,
                    List.of(new ArchiveRunMember(player.getUUID(), captureReturnPoint(player))),
                    graph);
            storage.register(run);
            storage.startMemories(run);
            ArchiveGenerationQueue.enqueue(run);
            feedback(player, "message.tbos.archive.preparing");
            return EntryResult.STARTED;
        } catch (RuntimeException exception) {
            Yesterglass.LOGGER.error("Failed to create debug archive run with seed {}", seed, exception);
            return EntryResult.PLACEMENT_FAILED;
        }
    }

    public static boolean regenerateCurrentRun(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(server);
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        if (run == null || !run.status().holdsInstanceSlot()) {
            return false;
        }
        ArchiveDungeonGraph graph = ArchiveRunGenerator.generateDungeon(run.seed(), run.floor(), dungeonSettings());
        ArchiveRun preparing = run.regenerate(graph);
        ArchiveGenerationQueue.cancel(run.runId());
        storage.replace(preparing);
        evacuateMembers(server, preparing);
        ArchiveGenerationQueue.enqueue(preparing);
        return true;
    }

    public static boolean removeCurrentRun(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(server);
        ArchiveRun run = storage.findByMember(player.getUUID())
                .or(() -> storage.findPendingReturnByMember(player.getUUID()))
                .orElse(null);
        if (run == null) {
            return false;
        }
        ArchiveGenerationQueue.enqueueRemoval(run);
        storage.remove(run.runId());
        evacuateMembers(server, run);
        return true;
    }

    public static boolean enterCurrentRun(ServerPlayer player) {
        ArchiveRun run = ArchiveRunSavedData.get(player.level().getServer())
                .findByMember(player.getUUID())
                .orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE) {
            return false;
        }
        return teleportToCheckpoint(player);
    }

    public static ArchiveReturnPoint captureReturnPoint(ServerPlayer player) {
        return new ArchiveReturnPoint(
                player.level().dimension().identifier(),
                player.blockPosition(),
                player.getYRot(),
                player.getXRot());
    }

    public static DeathResult handleDeath(ArchiveRunSavedData storage, UUID playerId, long currentTick) {
        ArchiveRun run = storage.findByMember(playerId).orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE) {
            return DeathResult.NOT_IN_ACTIVE_RUN;
        }
        Long previousTick = LAST_HANDLED_DEATH_TICK.put(playerId, currentTick);
        if (previousTick != null && previousTick == currentTick) {
            return DeathResult.DUPLICATE_EVENT;
        }
        if (run.sharedRevives() > 0) {
            storage.replace(run.consumeRevive());
            return DeathResult.REVIVED;
        }
        storage.replace(run.fail(currentTick + FAILURE_RETURN_DELAY_TICKS));
        return DeathResult.RUN_FAILED;
    }

    public static Optional<ArchiveRun> beginNextFloor(ArchiveRunSavedData storage, UUID playerId) {
        ArchiveRun run = storage.findByMember(playerId).orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE) {
            return Optional.empty();
        }
        int nextInstanceSlot = storage.nextFreeSlot();
        if (nextInstanceSlot >= ArchiveInstanceLayout.MAX_INSTANCE_SLOTS) {
            Yesterglass.LOGGER.error("Cannot advance Archive run {} from floor {}: no free instance slot",
                    run.runId(), run.floor());
            return Optional.empty();
        }
        long nextFloor = run.floor() + 1L;
        long candidateSeed = nextFloorSeed(run, nextFloor);
        try {
            ArchiveDungeonGraph graph = null;
            for (int attempt = 0; attempt < 16; attempt++) {
                graph = ArchiveRunGenerator.generateDungeon(candidateSeed, nextFloor, storage.memories(run.runId()) == null ? dungeonSettings() : com.nightbeam.tbos.memory.MemoryRules.expedition(dungeonSettings()));
                if (!sameStructure(run.dungeonGraph(), graph)) {
                    break;
                }
                candidateSeed = mix64(candidateSeed + 0x9E3779B97F4A7C15L + attempt);
            }
            if (graph == null || sameStructure(run.dungeonGraph(), graph)) {
                throw new IllegalStateException("Could not produce a structurally distinct next Archive floor");
            }
            ArchiveRun preparing = run.advanceToNextFloor(candidateSeed, nextInstanceSlot, graph);
            storage.replace(preparing);
            ArchiveGenerationQueue.enqueue(preparing);
            return Optional.of(preparing);
        } catch (RuntimeException exception) {
            Yesterglass.LOGGER.error(
                    "Failed to prepare Archive run {} floor {}", run.runId(), nextFloor, exception);
            return Optional.empty();
        }
    }

    public static Optional<ArchiveRun> beginFailureReturn(
            ArchiveRunSavedData storage, UUID playerId, long currentTick) {
        ArchiveRun run = storage.findByMember(playerId).orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE) {
            return Optional.empty();
        }
        ArchiveRun returning = run.fail(currentTick + FAILURE_RETURN_DELAY_TICKS);
        storage.replace(returning);
        return Optional.of(returning);
    }

    public static GatewayAdvanceResult advanceFromRewardGateway(ServerPlayer player, BlockPos gatewayPos) {
        if (!player.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
            return GatewayAdvanceResult.NOT_ARCHIVE;
        }
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        if (run == null) {
            feedback(player, "message.tbos.archive.gateway.not_member");
            return GatewayAdvanceResult.NOT_MEMBER;
        }
        if (run.status() != ArchiveRunStatus.ACTIVE) {
            feedback(player, "message.tbos.archive.gateway.busy");
            return GatewayAdvanceResult.RUN_BUSY;
        }
        if (!gatewayPos.equals(ArchiveRoomPlacer.rewardGatewayPosition(run))) {
            feedback(player, "message.tbos.archive.gateway.invalid");
            return GatewayAdvanceResult.INVALID_GATEWAY;
        }
        ArchiveRoomNode reward = run.dungeonGraph().room(run.dungeonGraph().rewardRoom());
        if (!reward.runtime().completed()) {
            feedback(player, "message.tbos.archive.gateway.locked");
            return GatewayAdvanceResult.REWARD_LOCKED;
        }
        if ((!com.nightbeam.tbos.memory.MemoryService.mayAdvance(storage, run)
                || (storage.memories(run.runId()) != null && run.floor() >= 2
                    && run.members().stream().anyMatch(m -> player.level().getServer().getPlayerList().getPlayer(m.playerId()) == null)))) {
            player.sendSystemMessage(Component.literal("Living Memories: open your loadout (K) at the gateway to extract or vote to continue."));
            return GatewayAdvanceResult.REWARD_LOCKED;
        }
        Optional<ArchiveRun> nextFloor = beginNextFloor(storage, player.getUUID());
        if (nextFloor.isEmpty()) {
            feedback(player, "message.tbos.archive.gateway.failed");
            return GatewayAdvanceResult.PREPARATION_FAILED;
        }
        ArchiveRun preparing = nextFloor.get();
        Component message = Component.translatable(
                "message.tbos.archive.gateway.advancing",
                ArchiveFloorPresentation.displayFloor(run.floor()),
                ArchiveFloorPresentation.displayFloor(preparing.floor()));
        for (ArchiveRunMember member : preparing.members()) {
            ServerPlayer online = player.level().getServer().getPlayerList().getPlayer(member.playerId());
            if (online != null) {
                online.sendOverlayMessage(message);
            }
        }
        return GatewayAdvanceResult.ADVANCING;
    }

    public static ReturnResult completeReturnIfDue(
            ArchiveRunSavedData storage, UUID runId, long currentTick) {
        ArchiveRun run = storage.find(runId).orElse(null);
        if (run == null) {
            return ReturnResult.UNKNOWN_RUN;
        }
        if (run.status().isTerminal()) {
            return ReturnResult.ALREADY_TERMINAL;
        }
        if (!run.status().isReturning()) {
            return ReturnResult.NOT_RETURNING;
        }
        if (currentTick < run.returnDeadlineTick()) {
            return ReturnResult.NOT_DUE;
        }
        storage.replace(run.complete());
        return ReturnResult.COMPLETED;
    }

    public static ReturnDestination resolveReturnDestination(
            MinecraftServer server, ArchiveReturnPoint returnPoint) {
        ResourceKey<Level> destinationKey = ResourceKey.create(Registries.DIMENSION, returnPoint.dimension());
        ServerLevel destination = server.getLevel(destinationKey);
        if (destination != null && !ArchiveDimensions.isFracturedArchive(destination)) {
            return new ReturnDestination(
                    destination,
                    returnPoint.position(),
                    returnPoint.yRot(),
                    returnPoint.xRot());
        }
        ServerLevel overworld = server.overworld();
        var respawn = overworld.getRespawnData();
        return new ReturnDestination(overworld, respawn.pos(), respawn.yaw(), respawn.pitch());
    }

    public static boolean returnMemberHome(ArchiveRunSavedData storage, ServerPlayer player) {
        ArchiveRun run = storage.findPendingReturnByMember(player.getUUID()).orElse(null);
        if (run == null) {
            return false;
        }
        ArchiveRunMember member = run.member(player.getUUID()).orElseThrow();
        ReturnDestination destination = resolveReturnDestination(player.level().getServer(), member.returnPoint());
        destination.level().getChunkAt(destination.position());
        player.teleport(new TeleportTransition(
                destination.level(),
                Vec3.atBottomCenterOf(destination.position()),
                Vec3.ZERO,
                destination.yRot(),
                destination.xRot(),
                TeleportTransition.DO_NOTHING));
        ArchiveRun latest = storage.find(run.runId()).orElseThrow();
        storage.replace(latest.markMemberReturned(player.getUUID()));
        return true;
    }

    public static boolean reachCheckpoint(ServerPlayer player, int roomIndex) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE) {
            return false;
        }
        if (roomIndex < 0 || roomIndex >= run.rooms().size()
                || !run.dungeonGraph().room(roomIndex).runtime().visited()) {
            return false;
        }
        storage.replace(run.checkpoint(player.getUUID(), roomIndex));
        return true;
    }

    public static boolean useWaystone(ServerPlayer player) {
        if (isStandingInArchive(player)) {
            return leaveViaWaystone(player);
        }
        return resumeViaWaystone(player);
    }

    public static boolean isStandingInArchive(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ServerLevel overworld = server.overworld();
        if (player.level() != overworld) {
            return true;
        }
        ArchiveRun run = ArchiveRunSavedData.get(server).findByMember(player.getUUID()).orElse(null);
        return run != null
                && run.status() == ArchiveRunStatus.ACTIVE
                && ArchiveInstanceLayout.boundsForSlot(run.instanceSlot()).isInside(player.blockPosition());
    }

    public static boolean leaveViaWaystone(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ServerLevel overworld = server.overworld();
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(server);
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        if (run != null
                && run.status() == ArchiveRunStatus.ACTIVE
                && run.dungeonGraph().waystoneRoom() >= 0) {
            storage.replace(run.bindWaystone(player.getUUID()));
            run = storage.find(run.runId()).orElse(run);
        }
        ReturnDestination destination = overworldHome(overworld);
        if (run != null) {
            ArchiveRunMember member = run.member(player.getUUID()).orElse(null);
            if (member != null) {
                ReturnDestination preferred = resolveReturnDestination(server, member.returnPoint());
                if (preferred.level() == overworld) {
                    destination = preferred;
                }
            }
        }
        overworld.getChunkAt(destination.position());
        ServerPlayer arrived = player.teleport(new TeleportTransition(
                overworld,
                Vec3.atBottomCenterOf(destination.position()),
                Vec3.ZERO,
                destination.yRot(),
                destination.xRot(),
                TeleportTransition.DO_NOTHING));
        if (arrived == null) {
            return false;
        }
        arrived.resetFallDistance();
        arrived.clearFire();
        arrived.sendSystemMessage(Component.translatable("message.tbos.waystone.saved"));
        arrived.sendOverlayMessage(Component.translatable("message.tbos.waystone.saved"));
        return true;
    }

    private static ReturnDestination overworldHome(ServerLevel overworld) {
        var respawn = overworld.getRespawnData();
        return new ReturnDestination(overworld, respawn.pos(), respawn.yaw(), respawn.pitch());
    }

    public static boolean resumeViaWaystone(ServerPlayer player) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE || !run.geometryPlaced()) {
            player.sendOverlayMessage(Component.translatable("message.tbos.waystone.no_checkpoint"));
            return false;
        }
        ArchiveRunMember member = run.member(player.getUUID()).orElse(null);
        if (member == null || !member.waystoneBound() || run.dungeonGraph().waystoneRoom() < 0) {
            player.sendOverlayMessage(Component.translatable("message.tbos.waystone.no_checkpoint"));
            return false;
        }
        ServerLevel archive = player.level().getServer().getLevel(ArchiveDimensions.FRACTURED_ARCHIVE);
        if (archive == null) {
            player.sendOverlayMessage(Component.translatable("message.tbos.waystone.no_checkpoint"));
            return false;
        }
        BlockPos spawn = ArchiveRoomPlacer.waystoneSpawn(run);
        archive.getChunkAt(spawn);
        player.teleport(new TeleportTransition(
                archive,
                Vec3.atBottomCenterOf(spawn),
                Vec3.ZERO,
                0.0F,
                0.0F,
                TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        player.clearFire();
        player.sendOverlayMessage(Component.translatable("message.tbos.waystone.resumed"));
        return true;
    }

    public static boolean teleportToCheckpoint(ServerPlayer player) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE) {
            return false;
        }
        ServerLevel archive = player.level().getServer().getLevel(ArchiveDimensions.FRACTURED_ARCHIVE);
        if (archive == null) {
            return false;
        }
        ArchiveRunMember member = run.member(player.getUUID()).orElse(null);
        if (member == null) {
            return false;
        }
        BlockPos checkpoint = ArchiveRoomPlacer.roomSpawn(run, member.checkpointRoom());
        archive.getChunkAt(checkpoint);
        player.teleport(new TeleportTransition(
                archive,
                Vec3.atBottomCenterOf(checkpoint),
                Vec3.ZERO,
                0.0F,
                0.0F,
                TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        player.clearFire();
        player.sendOverlayMessage(Component.translatable(
                "message.tbos.archive.revived", run.sharedRevives()));
        return true;
    }

    private static void evacuateMembers(MinecraftServer server, ArchiveRun run) {
        for (ArchiveRunMember member : run.members()) {
            ServerPlayer online = server.getPlayerList().getPlayer(member.playerId());
            if (online == null) {
                continue;
            }
            ReturnDestination destination = resolveReturnDestination(server, member.returnPoint());
            destination.level().getChunkAt(destination.position());
            online.teleport(new TeleportTransition(
                    destination.level(),
                    Vec3.atBottomCenterOf(destination.position()),
                    Vec3.ZERO,
                    destination.yRot(),
                    destination.xRot(),
                    TeleportTransition.DO_NOTHING));
        }
    }

    public static void clearRuntimeState() {
        LAST_HANDLED_DEATH_TICK.clear();
        ArchiveGenerationQueue.clear();
    }

    public static boolean discoverNearestSecret(ServerPlayer player) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE
                || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        for (ArchiveRoomNode room : run.dungeonGraph().rooms()) {
            if (room.category() != ArchiveRoomCategory.SECRET || room.runtime().secretDiscovered()) {
                continue;
            }
            boolean nearby = ArchiveRoomPlacer.secretWallPositions(run, room.index()).stream()
                    .anyMatch(position -> position.distSqr(player.blockPosition()) <= 36.0D);
            if (!nearby) {
                continue;
            }
            ArchiveRun discovered = run.discoverSecretRoom(room.index());
            storage.replace(discovered);
            ArchiveRoomPlacer.revealSecretConnection(level, discovered, room.index());
            player.sendOverlayMessage(Component.translatable("message.tbos.archive.secret_discovered"));
            return true;
        }
        return false;
    }

    public static List<ServerPlayer> collectParty(ServerPlayer activator, BlockPos thresholdPos) {
        PlayerTeam team = activator.getTeam();
        Vec3 center = Vec3.atCenterOf(thresholdPos);
        return activator.level().getServer().getPlayerList().getPlayers().stream()
                .filter(candidate -> candidate.level() == activator.level())
                .filter(candidate -> candidate == activator || team != null && team.equals(candidate.getTeam()))
                .filter(candidate -> candidate == activator || candidate.distanceToSqr(center) <= SHRINE_RING_RADIUS_SQR)
                .sorted(Comparator.comparing(ServerPlayer::getUUID))
                .limit(ArchiveRun.MAX_PARTY_SIZE)
                .toList();
    }

    private static boolean hasRepairedLens(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        return mainHand.is(ModItems.YESTERGLASS_LENS.get()) || offHand.is(ModItems.YESTERGLASS_LENS.get());
    }

    private static boolean hasCuratorCore(ServerPlayer player) {
        return player.getInventory().contains(stack -> stack.is(ModItems.CURATOR_CORE.get()));
    }

    private static ArchiveDungeonSettings dungeonSettings() {
        try {
            return YesterglassConfig.dungeonSettings();
        } catch (IllegalStateException exception) {
            return ArchiveDungeonSettings.DEFAULT;
        }
    }

    private static boolean debugEnabled() {
        try {
            return YesterglassConfig.DUNGEON_DEBUG.get();
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    static boolean retainCompletedRuns() {
        try {
            return YesterglassConfig.dungeonRules().retainCompletedRuns();
        } catch (IllegalStateException | IllegalArgumentException exception) {
            return ArchiveDungeonRules.DEFAULT.retainCompletedRuns();
        }
    }

    private static void feedback(ServerPlayer player, String translationKey) {
        player.sendOverlayMessage(Component.translatable(translationKey));
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static long nextFloorSeed(ArchiveRun run, long nextFloor) {
        long seed = mix64(run.seed()
                ^ run.runId().getMostSignificantBits()
                ^ Long.rotateLeft(run.runId().getLeastSignificantBits(), 17)
                ^ nextFloor * 0x9E3779B97F4A7C15L);
        return seed == run.seed() ? mix64(seed + 0xD1B54A32D192ED03L) : seed;
    }

    private static boolean sameStructure(ArchiveDungeonGraph first, ArchiveDungeonGraph second) {
        if (first.rooms().size() != second.rooms().size()) {
            return false;
        }
        for (int index = 0; index < first.rooms().size(); index++) {
            ArchiveRoomNode left = first.room(index);
            ArchiveRoomNode right = second.room(index);
            if (!left.templateId().equals(right.templateId())
                    || !left.placement().equals(right.placement())
                    || !sameConnectionLayout(left.connections(), right.connections())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameConnectionLayout(
            List<ArchiveConnection> first, List<ArchiveConnection> second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (int index = 0; index < first.size(); index++) {
            ArchiveConnection left = first.get(index);
            ArchiveConnection right = second.get(index);
            if (left.targetRoom() != right.targetRoom() || left.direction() != right.direction()) {
                return false;
            }
        }
        return true;
    }

    public enum EntryResult {
        STARTED,
        NO_LENS,
        NO_CURATOR_CORE,
        ALREADY_IN_RUN,
        PARTY_MEMBER_BUSY,
        ARCHIVE_UNAVAILABLE,
        NO_FREE_SLOT,
        PLACEMENT_FAILED
    }

    public enum GatewayAdvanceResult {
        ADVANCING,
        NOT_ARCHIVE,
        NOT_MEMBER,
        RUN_BUSY,
        INVALID_GATEWAY,
        REWARD_LOCKED,
        PREPARATION_FAILED
    }

    public enum DeathResult {
        REVIVED,
        DUPLICATE_EVENT,
        RUN_FAILED,
        NOT_IN_ACTIVE_RUN
    }

    public enum ReturnResult {
        COMPLETED,
        NOT_DUE,
        ALREADY_TERMINAL,
        NOT_RETURNING,
        UNKNOWN_RUN
    }

    public record ReturnDestination(ServerLevel level, BlockPos position, float yRot, float xRot) {
    }
}
