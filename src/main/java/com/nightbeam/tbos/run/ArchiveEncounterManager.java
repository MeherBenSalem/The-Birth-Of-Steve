package com.nightbeam.tbos.run;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.block.AlignmentDialBlock;
import com.nightbeam.tbos.config.YesterglassConfig;
import com.nightbeam.tbos.network.payload.ArchivePuzzlePayload;
import com.nightbeam.tbos.registry.ModBlocks;
import com.nightbeam.tbos.registry.ModEntities;
import com.nightbeam.tbos.registry.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** Instance-scoped room locks, independent room encounters, puzzles, loot, and victory. */
public final class ArchiveEncounterManager {
    private static final long CHOIR_PATTERN_SALT = 0x43484F49525F5041L;
    private static final String KIND_TAG_PREFIX = "tbos.kind.";
    private static final String LESSER_BOSS_TAG = "tbos.lesser_boss";
    private static final String SPLIT_CHILD_TAG = "tbos.split_child";

    private ArchiveEncounterManager() {
    }

    public static void tick(MinecraftServer server, ArchiveRunSavedData storage, ArchiveRun run) {
        if (run.status() != ArchiveRunStatus.ACTIVE) {
            return;
        }
        ServerLevel level = server.getLevel(ArchiveDimensions.FRACTURED_ARCHIVE);
        if (level == null) {
            return;
        }

        // Make at most one durable encounter transition per run per tick. This
        // permits party splits without two stale snapshots overwriting one another.
        for (ArchiveRoomNode room : run.dungeonGraph().rooms()) {
            int roomIndex = room.index();
            List<UUID> presentMembers = membersInRoom(server, run, roomIndex);
            if (presentMembers.isEmpty()) {
                continue;
            }
            applyRoomModifiers(level, run, roomIndex);
            tickEnemyAbilities(level, run, roomIndex);
            boolean memberPositionChanged = presentMembers.stream().anyMatch(memberId ->
                    run.member(memberId).orElseThrow().currentRoom() != roomIndex);
            if (!room.runtime().visited() || memberPositionChanged) {
                ArchiveRun visited = run.visitRoom(roomIndex, presentMembers);
                storage.replace(visited);
                if (!room.runtime().visited()) {
                    announce(server, visited, roomIntro(visited, roomIndex));
                }
                return;
            }
            if (room.runtime().completed()) {
                continue;
            }
            ArchiveEncounterState state = run.roomEncounterStates().get(roomIndex);
            if (!state.started()) {
                boolean shouldLock = dungeonRules().lockCombatDoors()
                        && room.encounterKind() != ArchiveEncounterKind.EXPLORATION
                        && room.encounterKind() != ArchiveEncounterKind.REWARD;
                ArchiveRun locked = shouldLock ? run.lockRoom(roomIndex) : run;
                if (shouldLock && !ArchiveRoomPlacer.lockRoomDoors(level, locked, roomIndex)) {
                    // Someone is still crossing the threshold. Defer the
                    // durable lock and encounter start until the doorway is
                    // physically clear on a later tick.
                    return;
                }
                storage.replace(locked);
                startEncounter(server, level, storage, locked, roomIndex);
                return;
            }
            if (state.waveActive() && activeEnemyCount(level, run, roomIndex) == 0) {
                finishWave(server, level, storage, run, roomIndex);
                return;
            }
        }
    }

    public static boolean ringChoirBell(ServerPlayer player, BlockPos position) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE) {
            return false;
        }
        int roomIndex = run.dungeonGraph().rooms().stream()
                .filter(room -> room.encounterKind() == ArchiveEncounterKind.CHOIR)
                .filter(room -> ArchiveRoomPlacer.choirBellPositions(run, room.index()).contains(position))
                .mapToInt(ArchiveRoomNode::index)
                .findFirst()
                .orElse(-1);
        if (roomIndex < 0) {
            return false;
        }
        List<BlockPos> bells = ArchiveRoomPlacer.choirBellPositions(run, roomIndex);
        int symbol = bells.indexOf(position);
        ArchiveEncounterState state = run.roomEncounterStates().get(roomIndex);
        if (!state.started()) {
            sendPuzzleHud(player, run);
            return true;
        }
        if (state.complete()) {
            sendPuzzleHud(player, run);
            return true;
        }
        if (state.waveActive()) {
            sendPuzzleHud(player, run);
            return true;
        }

        List<Integer> pattern = choirPattern(run.rooms().get(roomIndex).encounterSeed(), state.puzzlePhase());
        lightBell((ServerLevel) player.level(), position);
        if (symbol != pattern.get(state.puzzleCursor())) {
            ArchiveRun rejected = run.withRoomEncounterState(roomIndex, state.rejectPuzzleInput());
            storage.replace(rejected);
            puzzleFailureEffect((ServerLevel) player.level(), position);
            sendPuzzleHud(player, rejected);
            return true;
        }
        if (state.puzzleCursor() + 1 < pattern.size()) {
            ArchiveRun accepted = run.withRoomEncounterState(roomIndex, state.acceptPuzzleInput());
            storage.replace(accepted);
            puzzleSuccessEffect((ServerLevel) player.level(), position, false);
            sendPuzzleHud(player, accepted);
            return true;
        }

        ArchiveEncounterState wave = state.finishPuzzleSequence();
        ArchiveRun updated = run.withRoomEncounterState(roomIndex, wave);
        storage.replace(updated);
        spawnWave((ServerLevel) player.level(), updated, roomIndex, wave.wave());
        puzzleSuccessEffect((ServerLevel) player.level(), position, true);
        sendPuzzleHud(player, updated);
        return true;
    }

    public static boolean rotateHallDial(ServerPlayer player, BlockPos position) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE) {
            return false;
        }
        int roomIndex = run.dungeonGraph().rooms().stream()
                .filter(room -> room.encounterKind() == ArchiveEncounterKind.HALL)
                .filter(room -> ArchiveRoomPlacer.hallDialPositions(run, room.index()).contains(position))
                .mapToInt(ArchiveRoomNode::index)
                .findFirst()
                .orElse(-1);
        if (roomIndex < 0) {
            return false;
        }
        ArchiveEncounterState encounter = run.roomEncounterStates().get(roomIndex);
        if (!encounter.started()) {
            sendPuzzleHud(player, run);
            return true;
        }
        if (encounter.waveActive() || encounter.complete()) {
            sendPuzzleHud(player, run);
            return true;
        }

        ServerLevel level = (ServerLevel) player.level();
        var blockState = level.getBlockState(position);
        if (!blockState.is(ModBlocks.ALIGNMENT_DIAL.get())) {
            return false;
        }
        level.setBlock(
                position,
                blockState.setValue(AlignmentDialBlock.FACING,
                        blockState.getValue(AlignmentDialBlock.FACING).getClockWise()),
                3);
        puzzleSuccessEffect(level, position, false);
        List<BlockPos> dials = ArchiveRoomPlacer.hallDialPositions(run, roomIndex);
        List<Direction> targets = hallTargets(run.rooms().get(roomIndex).encounterSeed());
        int aligned = 0;
        for (int index = 0; index < dials.size(); index++) {
            var dialState = level.getBlockState(dials.get(index));
            if (dialState.is(ModBlocks.ALIGNMENT_DIAL.get())
                    && dialState.getValue(AlignmentDialBlock.FACING) == targets.get(index)) {
                aligned++;
            }
        }
        if (aligned < dials.size()) {
            sendPuzzleHud(player, run);
            return true;
        }

        ArchiveRun updated = run.withRoomEncounterState(roomIndex, encounter.startWave(1));
        storage.replace(updated);
        spawnWave(level, updated, roomIndex, 1);
        puzzleSuccessEffect(level, position, true);
        sendPuzzleHud(player, updated);
        return true;
    }

    public static List<Direction> hallTargets(long encounterSeed) {
        RandomSource random = RandomSource.create(mix64(encounterSeed ^ 0x48414C4C5F444941L));
        Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        return List.of(
                directions[random.nextInt(directions.length)],
                directions[random.nextInt(directions.length)],
                directions[random.nextInt(directions.length)]);
    }

    public static List<Integer> choirPattern(long encounterSeed, int phase) {
        if (phase < 0 || phase >= 3) {
            throw new IllegalArgumentException("Choir phase must be between 0 and 2");
        }
        ArrayList<Integer> pattern = new ArrayList<>(List.of(0, 1, 2, 3));
        RandomSource random = RandomSource.create(mix64(
                encounterSeed ^ CHOIR_PATTERN_SALT ^ (0x9E3779B97F4A7C15L * (phase + 1L))));
        for (int index = pattern.size() - 1; index > 0; index--) {
            Collections.swap(pattern, index, random.nextInt(index + 1));
        }
        if (phase > 0 && pattern.equals(choirPattern(encounterSeed, phase - 1))) {
            Collections.rotate(pattern, 1);
        }
        return List.copyOf(pattern);
    }

    public static ArchivePuzzlePayload puzzlePayload(
            ServerLevel level, ArchiveRun run, ServerPlayer player, long serverTick) {
        int roomIndex = ArchiveRoomPlacer.roomContaining(run, player.blockPosition()).orElse(-1);
        if (roomIndex < 0) {
            return ArchivePuzzlePayload.clear(run.runId(), serverTick);
        }
        ArchiveEncounterKind kind = run.dungeonGraph().room(roomIndex).encounterKind();
        if (kind != ArchiveEncounterKind.HALL && kind != ArchiveEncounterKind.CHOIR) {
            return ArchivePuzzlePayload.clear(run.runId(), serverTick);
        }
        ArchiveEncounterState encounter = run.roomEncounterStates().get(roomIndex);
        ArchivePuzzlePayload.PuzzleState state = !encounter.started()
                ? ArchivePuzzlePayload.PuzzleState.WAITING
                : encounter.complete()
                        ? ArchivePuzzlePayload.PuzzleState.COMPLETE
                        : encounter.waveActive()
                                ? ArchivePuzzlePayload.PuzzleState.COMBAT
                                : ArchivePuzzlePayload.PuzzleState.SOLVING;
        if (kind == ArchiveEncounterKind.CHOIR) {
            int phase = Math.min(2, encounter.puzzlePhase());
            List<Integer> pattern = choirPattern(run.rooms().get(roomIndex).encounterSeed(), phase);
            int progress = state == ArchivePuzzlePayload.PuzzleState.COMBAT
                            || state == ArchivePuzzlePayload.PuzzleState.COMPLETE
                    ? pattern.size()
                    : encounter.puzzleCursor();
            return new ArchivePuzzlePayload(
                    run.runId(),
                    roomIndex,
                    ArchivePuzzlePayload.PuzzleKind.CHOIR,
                    state,
                    Math.min(3, encounter.puzzlePhase() + 1),
                    3,
                    progress,
                    pattern.size(),
                    encounter.failures(),
                    pattern,
                    serverTick);
        }

        List<Direction> targets = hallTargets(run.rooms().get(roomIndex).encounterSeed());
        List<BlockPos> dials = ArchiveRoomPlacer.hallDialPositions(run, roomIndex);
        int aligned = 0;
        for (int index = 0; index < Math.min(dials.size(), targets.size()); index++) {
            var dialState = level.getBlockState(dials.get(index));
            if (dialState.is(ModBlocks.ALIGNMENT_DIAL.get())
                    && dialState.getValue(AlignmentDialBlock.FACING) == targets.get(index)) {
                aligned++;
            }
        }
        int stage = state == ArchivePuzzlePayload.PuzzleState.COMBAT
                ? Math.max(1, Math.min(2, encounter.wave()))
                : state == ArchivePuzzlePayload.PuzzleState.COMPLETE ? 2 : 1;
        int progress = state == ArchivePuzzlePayload.PuzzleState.COMBAT
                        || state == ArchivePuzzlePayload.PuzzleState.COMPLETE
                ? targets.size()
                : aligned;
        return new ArchivePuzzlePayload(
                run.runId(),
                roomIndex,
                ArchivePuzzlePayload.PuzzleKind.HALL,
                state,
                stage,
                2,
                progress,
                targets.size(),
                encounter.failures(),
                targets.stream().map(ArchiveEncounterManager::directionGlyph).toList(),
                serverTick);
    }

    public static boolean inspectArchiveCache(ServerPlayer player, BlockPos position) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        ArchiveRun run = storage.findByMember(player.getUUID())
                .or(() -> storage.findPendingReturnByMember(player.getUUID()))
                .orElse(null);
        if (run == null || !player.level().getBlockState(position).is(ModBlocks.ARCHIVE_CACHE.get())) {
            return false;
        }
        if (position.equals(ArchiveRoomPlacer.rewardCachePosition(run))) {
            ArchiveRunMember member = run.member(player.getUUID()).orElse(null);
            if (member != null && member.rewardClaimed()) {
                player.sendOverlayMessage(Component.literal("CANTOR CACHE - Your recollection was already released")
                        .withStyle(ChatFormatting.GRAY));
            } else if (run.status() == ArchiveRunStatus.RETURNING_VICTORY
                    || run.status() == ArchiveRunStatus.COMPLETED) {
                player.sendOverlayMessage(Component.literal("CANTOR CACHE - Break the seal to release its recollection")
                        .withStyle(ChatFormatting.GOLD));
            } else {
                player.sendOverlayMessage(Component.literal("CANTOR CACHE - The Hour Cantor still binds this seal")
                        .withStyle(ChatFormatting.RED));
            }
            return true;
        }
        if (run.status() != ArchiveRunStatus.ACTIVE) {
            return false;
        }
        int roomIndex = run.dungeonGraph().rooms().stream()
                .filter(room -> ArchiveRoomPlacer.chestPositions(run, room.index()).contains(position))
                .mapToInt(ArchiveRoomNode::index)
                .findFirst()
                .orElse(-1);
        if (roomIndex < 0) {
            player.sendOverlayMessage(Component.literal("ARCHIVE CACHE - No reward is bound here")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        if (room.category().combat() && !room.runtime().completed()) {
            player.sendOverlayMessage(Component.literal("ARCHIVE CACHE - Guardians still bind this room")
                    .withStyle(ChatFormatting.RED));
            return true;
        }
        int marker = ArchiveRoomPlacer.chestPositions(run, roomIndex).indexOf(position);
        ArchiveDungeonRules rules = dungeonRules();
        boolean alreadyClaimed = rules.lootMode() == ArchiveLootMode.INDIVIDUAL
                ? run.hasMemberClaimedContainer(player.getUUID(), roomIndex, marker)
                : room.runtime().openedContainers().contains(marker);
        if (alreadyClaimed) {
            player.sendOverlayMessage(Component.literal("ARCHIVE CACHE - Your recollection was already released")
                    .withStyle(ChatFormatting.GRAY));
            return true;
        }
        player.sendOverlayMessage(Component.literal("ARCHIVE CACHE - Break the seal to release its recollection")
                .withStyle(ChatFormatting.GOLD));
        return true;
    }

    public static boolean breakArchiveCache(ServerPlayer player, BlockPos position) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        ArchiveRun run = storage.findByMember(player.getUUID())
                .or(() -> storage.findPendingReturnByMember(player.getUUID()))
                .orElse(null);
        if (run == null
                || !(player.level() instanceof ServerLevel level)
                || !level.getBlockState(position).is(ModBlocks.ARCHIVE_CACHE.get())) {
            return false;
        }
        if (position.equals(ArchiveRoomPlacer.rewardCachePosition(run))) {
            return breakCantorCache(player, position);
        }
        if (run.status() != ArchiveRunStatus.ACTIVE) {
            return false;
        }
        int roomIndex = run.dungeonGraph().rooms().stream()
                .filter(room -> ArchiveRoomPlacer.chestPositions(run, room.index()).contains(position))
                .mapToInt(ArchiveRoomNode::index)
                .findFirst()
                .orElse(-1);
        if (roomIndex < 0) {
            return false;
        }
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        if (room.category().combat() && !room.runtime().completed()) {
            player.sendOverlayMessage(Component.literal("ARCHIVE CACHE - Guardians still bind this room")
                    .withStyle(ChatFormatting.RED));
            level.playSound(
                    null,
                    position,
                    SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                    SoundSource.BLOCKS,
                    0.45F,
                    1.45F);
            return false;
        }
        int marker = ArchiveRoomPlacer.chestPositions(run, roomIndex).indexOf(position);
        ArchiveDungeonRules rules = dungeonRules();
        boolean alreadyClaimed = rules.lootMode() == ArchiveLootMode.INDIVIDUAL
                ? run.hasMemberClaimedContainer(player.getUUID(), roomIndex, marker)
                : room.runtime().openedContainers().contains(marker);
        if (alreadyClaimed) {
            player.sendOverlayMessage(Component.literal("ARCHIVE CACHE - Your recollection was already released")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }

        ArchiveRun opened = rules.lootMode() == ArchiveLootMode.INDIVIDUAL
                ? run.claimMemberContainer(player.getUUID(), roomIndex, marker)
                : run.openContainer(roomIndex, marker);
        storage.replace(opened);
        grantRoomLoot(player, opened, roomIndex, marker, position);
        ArchiveContainerKind containerKind = ArchiveRoomPlacer.containerKind(opened, roomIndex, marker);
        if (containerKind == ArchiveContainerKind.TRAPPED) {
            player.hurtServer(level, player.damageSources().magic(), 3.0F);
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 0));
        } else if (containerKind == ArchiveContainerKind.CURSED) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
        }

        boolean removeCache = rules.lootMode() == ArchiveLootMode.SHARED
                || opened.allMembersClaimedContainer(roomIndex, marker);
        if (removeCache) {
            level.removeBlock(position, false);
        } else {
            level.sendBlockUpdated(position, level.getBlockState(position), level.getBlockState(position), 3);
        }
        cacheBreakEffect(level, position, containerKind);
        if (debugEnabled()) {
            Yesterglass.LOGGER.info("Archive container {} room {} marker {} type {} mode {} removed {}",
                    run.runId(), roomIndex, marker, containerKind, rules.lootMode(), removeCache);
        }
        return true;
    }

    public static boolean breakCantorCache(ServerPlayer player, BlockPos position) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        ArchiveRun run = storage.findByMember(player.getUUID())
                .or(() -> storage.findPendingReturnByMember(player.getUUID()))
                .orElse(null);
        if (run == null
                || !position.equals(ArchiveRoomPlacer.rewardCachePosition(run))
                || (run.status() != ArchiveRunStatus.RETURNING_VICTORY
                        && run.status() != ArchiveRunStatus.COMPLETED)
                || !(player.level() instanceof ServerLevel level)
                || !level.getBlockState(position).is(ModBlocks.ARCHIVE_CACHE.get())) {
            return false;
        }
        ArchiveRunMember member = run.member(player.getUUID()).orElse(null);
        if (member == null || member.rewardClaimed()) {
            player.sendOverlayMessage(Component.literal("CANTOR CACHE - Your recollection was already released")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }

        ArchiveRun claimed = run.claimReward(player.getUUID());
        storage.replace(claimed);
        int shards = 4 + Math.floorMod((int) (run.seed() ^ player.getUUID().getMostSignificantBits()), 5);
        dropAtCache(level, position, player, new ItemStack(ModItems.CHRONICLE_SHARD.get(), shards));
        ArchiveRoomNode rewardRoom = run.dungeonGraph().room(run.dungeonGraph().rewardRoom());
        long rewardSeed = mix64(run.seed()
                ^ player.getUUID().getMostSignificantBits()
                ^ player.getUUID().getLeastSignificantBits());
        ArchiveLootRoller.roll(
                        level,
                        player,
                        position,
                        rewardRoom,
                        dungeonRules(),
                        rewardSeed,
                        true)
                .forEach(stack -> dropAtCache(level, position, player, stack));

        boolean removeCache = dungeonRules().lootMode() == ArchiveLootMode.SHARED
                || claimed.members().stream().allMatch(ArchiveRunMember::rewardClaimed);
        if (removeCache) {
            level.removeBlock(position, false);
        } else {
            level.sendBlockUpdated(position, level.getBlockState(position), level.getBlockState(position), 3);
        }
        level.sendParticles(
                ParticleTypes.END_ROD,
                position.getX() + 0.5D,
                position.getY() + 0.8D,
                position.getZ() + 0.5D,
                24,
                0.35D,
                0.45D,
                0.35D,
                0.04D);
        level.playSound(null, position, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1.0F, 0.65F);
        level.playSound(null, position, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.1F, 0.82F);
        player.sendOverlayMessage(Component.literal("CANTOR CACHE - Recollection released")
                .withStyle(ChatFormatting.AQUA));
        return true;
    }

    public static boolean restoreSharedRevive(ServerPlayer player) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE
                || run.sharedRevives() >= ArchiveRun.MAX_SHARED_REVIVES) {
            player.sendOverlayMessage(Component.literal("RECALLED HOUR - Shared revives are already stable")
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }
        ArchiveRun restored = run.restoreRevive();
        storage.replace(restored);
        announce(player.level().getServer(), restored, Component.literal(
                        "RECALLED HOUR - " + restored.sharedRevives() + " shared revives")
                .withStyle(ChatFormatting.AQUA));
        return true;
    }

    public static boolean forceClearCurrentRoom(ServerPlayer player) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        if (run == null || run.status() != ArchiveRunStatus.ACTIVE
                || !(player.level() instanceof ServerLevel level)) {
            return false;
        }
        int roomIndex = ArchiveRoomPlacer.roomContaining(run, player.blockPosition()).orElse(run.currentRoom());
        ArchiveEncounterState current = run.roomEncounterStates().get(roomIndex);
        ArchiveEncounterState completeState = current.started()
                ? current.clearWave().markComplete()
                : current.startWithoutWave().markComplete();
        ArchiveRun complete = run.withRoomEncounterState(roomIndex, completeState).completeRoom(roomIndex);
        storage.replace(complete);
        level.getEntitiesOfClass(
                        Monster.class,
                        ArchiveRoomPlacer.roomAabb(complete, roomIndex),
                        mob -> mob.entityTags().contains(runTag(run.runId()))
                                && mob.entityTags().contains(roomTag(roomIndex)))
                .forEach(mob -> mob.discard());
        unlockRoomAndQuestGate(level, run, complete, roomIndex);
        return true;
    }

    private static void startEncounter(
            MinecraftServer server,
            ServerLevel level,
            ArchiveRunSavedData storage,
            ArchiveRun run,
            int roomIndex) {
        ArchiveEncounterKind kind = run.rooms().get(roomIndex).encounterKind();
        ArchiveEncounterState current = run.roomEncounterStates().get(roomIndex);
        if (kind == ArchiveEncounterKind.EXPLORATION || kind == ArchiveEncounterKind.REWARD) {
            ArchiveRun complete = run.withRoomEncounterState(roomIndex, current.startWithoutWave().markComplete())
                    .completeRoom(roomIndex);
            storage.replace(complete);
            unlockRoomAndQuestGate(level, run, complete, roomIndex);
            spawnDirectLoot(level, complete, roomIndex);
            if (roomIndex == complete.dungeonGraph().rewardRoom()) {
                ArchiveRunManager.beginVictoryReturn(
                        storage,
                        complete.members().getFirst().playerId(),
                        server.overworld().getGameTime());
                announce(server, complete, Component.literal("LAST RECOLLECTION - Cache open - Return in 30s")
                        .withStyle(ChatFormatting.AQUA));
            } else {
                announce(server, complete, Component.literal(encounterTitle(kind) + " - Path recorded")
                        .withStyle(ChatFormatting.AQUA));
            }
            return;
        }
        if (kind == ArchiveEncounterKind.CHOIR) {
            ArchiveRun started = run.withRoomEncounterState(roomIndex, current.startWithoutWave());
            storage.replace(started);
            return;
        }
        if (kind == ArchiveEncounterKind.HALL) {
            ArchiveRun started = run.withRoomEncounterState(roomIndex, current.startWithoutWave());
            storage.replace(started);
            return;
        }
        ArchiveRun started = run.withRoomEncounterState(roomIndex, current.startWave(1));
        storage.replace(started);
        spawnWave(level, started, roomIndex, 1);
        announce(server, started, Component.literal(encounterTitle(kind) + " - Wave 1/" + totalWaves(kind))
                .withStyle(kind == ArchiveEncounterKind.BOSS ? ChatFormatting.DARK_PURPLE : ChatFormatting.GOLD));
    }

    private static void finishWave(
            MinecraftServer server,
            ServerLevel level,
            ArchiveRunSavedData storage,
            ArchiveRun run,
            int roomIndex) {
        ArchiveEncounterKind kind = run.rooms().get(roomIndex).encounterKind();
        ArchiveEncounterState state = run.roomEncounterStates().get(roomIndex);
        if (kind == ArchiveEncounterKind.CHOIR) {
            ArchiveEncounterState next = state.finishPuzzleWave();
            ArchiveRun updated = run.withRoomEncounterState(roomIndex, next);
            if (next.complete()) {
                updated = updated.completeRoom(roomIndex);
            }
            storage.replace(updated);
            if (next.complete()) {
                unlockRoomAndQuestGate(level, run, updated, roomIndex);
                spawnDirectLoot(level, updated, roomIndex);
                playRoomClearCue(level, updated, roomIndex, kind);
            }
            return;
        }

        int totalWaves = totalWaves(kind)
                + (run.dungeonGraph().room(roomIndex).modifiers()
                        .contains(ArchiveRoomModifier.CONTINUOUS_WAVES) ? 1 : 0);
        if (state.wave() < totalWaves) {
            int nextWave = state.wave() + 1;
            ArchiveRun updated = run.withRoomEncounterState(roomIndex, state.startWave(nextWave));
            storage.replace(updated);
            spawnWave(level, updated, roomIndex, nextWave);
            if (kind != ArchiveEncounterKind.HALL) {
                announce(server, updated, Component.literal(
                                encounterTitle(kind) + " - Wave " + nextWave + "/" + totalWaves)
                        .withStyle(ChatFormatting.GOLD));
            }
            return;
        }

        ArchiveRun complete = run.withRoomEncounterState(roomIndex, state.markComplete()).completeRoom(roomIndex);
        storage.replace(complete);
        if (kind == ArchiveEncounterKind.HALL) {
            ArchiveRoomPlacer.buildHallBridge(level, complete, roomIndex);
        }
        unlockRoomAndQuestGate(level, run, complete, roomIndex);
        spawnDirectLoot(level, complete, roomIndex);
        playRoomClearCue(level, complete, roomIndex, kind);
        if (kind != ArchiveEncounterKind.HALL) {
            announce(server, complete, Component.literal(kind == ArchiveEncounterKind.BOSS
                            ? "HOUR CANTOR DEFEATED - The Last Recollection is open"
                            : encounterTitle(kind) + " - Cleared; path opened")
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    private static void spawnWave(ServerLevel level, ArchiveRun run, int roomIndex, int wave) {
        int party = Math.max(1, activePartyInRoom(level.getServer(), run, roomIndex));
        ArchiveDungeonRules rules = dungeonRules();
        List<ArchiveEnemyKind> composition = planWave(
                run.dungeonGraph().room(roomIndex), run.rooms().get(roomIndex).encounterSeed(), wave, party, rules);
        playWaveStartCue(level, run, roomIndex, wave);
        for (int ordinal = 0; ordinal < composition.size(); ordinal++) {
            spawn(level, run, roomIndex, composition.get(ordinal), ordinal, wave, party, rules);
        }
        if (debugEnabled()) {
            Yesterglass.LOGGER.info(
                    "Archive encounter selection {} room {} wave {} party {} groups {} composition {}",
                    run.runId(),
                    roomIndex,
                    wave,
                    party,
                    run.dungeonGraph().room(roomIndex).allowedMonsterGroups(),
                    composition);
        }
    }

    private static void playWaveStartCue(ServerLevel level, ArchiveRun run, int roomIndex, int wave) {
        BlockPos origin = ArchiveRoomPlacer.roomSpawn(run, roomIndex);
        ArchiveEncounterKind kind = run.rooms().get(roomIndex).encounterKind();
        if (kind == ArchiveEncounterKind.BOSS) {
            level.playSound(
                    null,
                    origin,
                    SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                    SoundSource.HOSTILE,
                    1.0F,
                    0.52F);
        } else if (kind == ArchiveEncounterKind.GUARDIAN || kind == ArchiveEncounterKind.HUNT) {
            level.playSound(null, origin, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.75F, 0.72F);
        } else {
            level.playSound(
                    null,
                    origin,
                    SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.HOSTILE,
                    0.58F,
                    Math.min(1.45F, 0.92F + wave * 0.12F));
        }
    }

    private static void playRoomClearCue(
            ServerLevel level, ArchiveRun run, int roomIndex, ArchiveEncounterKind kind) {
        BlockPos origin = ArchiveRoomPlacer.roomSpawn(run, roomIndex);
        level.playSound(
                null,
                origin,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS,
                kind == ArchiveEncounterKind.BOSS ? 1.25F : 0.85F,
                kind == ArchiveEncounterKind.BOSS ? 0.65F : 1.20F);
        level.playSound(
                null,
                origin,
                SoundEvents.PLAYER_LEVELUP,
                SoundSource.PLAYERS,
                kind == ArchiveEncounterKind.BOSS ? 1.0F : 0.55F,
                kind == ArchiveEncounterKind.BOSS ? 0.65F : 1.35F);
    }

    private static void unlockRoomAndQuestGate(
            ServerLevel level, ArchiveRun before, ArchiveRun after, int roomIndex) {
        ArchiveRoomPlacer.unlockRoomDoors(level, after, roomIndex);
        boolean justCompletedQuest = !ArchiveQuestProgress.from(before.dungeonGraph()).complete()
                && ArchiveQuestProgress.from(after.dungeonGraph()).complete();
        if (!justCompletedQuest) {
            return;
        }
        ArchiveRoomPlacer.unlockRoomDoors(level, after, after.dungeonGraph().bossRoom());
        for (ArchiveConnection connection : after.dungeonGraph().room(after.dungeonGraph().bossRoom()).connections()) {
            if (connection.targetRoom() == after.dungeonGraph().rewardRoom() || connection.locked()) {
                continue;
            }
            for (BlockPos position : ArchiveRoomPlacer.doorPositions(
                    after, after.dungeonGraph().bossRoom(), connection.direction())) {
                level.sendParticles(
                        ParticleTypes.END_ROD,
                        position.getX() + 0.5D,
                        position.getY() + 0.5D,
                        position.getZ() + 0.5D,
                        4,
                        0.15D,
                        0.15D,
                        0.15D,
                        0.03D);
            }
        }
        level.playSound(
                null,
                ArchiveRoomPlacer.roomSpawn(after, after.dungeonGraph().bossRoom()),
                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
                net.minecraft.sounds.SoundSource.BLOCKS,
                1.2F,
                0.75F);
        announce(
                level.getServer(),
                after,
                Component.literal("CANTOR SEAL RECONSTRUCTED - Final boss room open")
                        .withStyle(ChatFormatting.GOLD));
    }

    public static List<ArchiveEnemyKind> planWave(
            ArchiveRoomNode room,
            long encounterSeed,
            int wave,
            int activePlayers,
            ArchiveDungeonRules rules) {
        ArchiveEncounterKind kind = room.encounterKind();
        if (kind == ArchiveEncounterKind.BOSS) {
            return List.of(ArchiveEnemyKind.HOUR_CANTOR);
        }
        int base = switch (kind) {
            case EXPLORATION, REWARD -> 0;
            case TRAP -> 1;
            case SKIRMISH -> 2;
            case HUNT -> 2 + wave;
            case GUARDIAN -> 2;
            case HALL -> 1 + wave;
            case CHOIR -> 1 + wave;
            case BOSS -> 1;
        };
        int area = room.placement().size().width() * room.placement().size().depth();
        int sizeBonus = area >= 900 && base > 0 ? 1 : 0;
        int difficultyBonus = room.difficulty() >= 25 && base > 0 ? 1 : 0;
        int partyBonus = (int) Math.ceil(
                Math.max(0, activePlayers - 1) * rules.enemiesPerAdditionalPlayer());
        int count = Math.min(16, base + sizeBonus + difficultyBonus + partyBonus);
        if (count == 0) {
            return List.of();
        }
        List<Identifier> groups = room.allowedMonsterGroups().isEmpty()
                ? List.of(ArchiveDungeonRules.FORGOTTEN_LEGION)
                : room.allowedMonsterGroups();
        RandomSource random = RandomSource.create(mix64(
                encounterSeed ^ (0x574156455F504F4FL * Math.max(1, wave))
                        ^ (0x50415254595F5343L * Math.max(1, activePlayers))));
        ArrayList<ArchiveEnemyKind> result = new ArrayList<>(count);
        if (room.category() == ArchiveRoomCategory.MINI_BOSS && wave >= 2) {
            List<ArchiveEnemyKind> lesserBosses = List.of(
                    ArchiveEnemyKind.MERIDIAN_SENTINEL,
                    ArchiveEnemyKind.VINDICATOR,
                    ArchiveEnemyKind.EVOKER,
                    ArchiveEnemyKind.RAVAGER);
            result.add(lesserBosses.get(random.nextInt(lesserBosses.size())));
        }
        for (int ordinal = result.size(); ordinal < count; ordinal++) {
            Identifier group = groups.get(random.nextInt(groups.size()));
            result.add(rules.chooseEnemy(group, random));
        }
        return List.copyOf(result);
    }

    private static void spawn(
            ServerLevel level,
            ArchiveRun run,
            int roomIndex,
            ArchiveEnemyKind kind,
            int ordinal,
            int wave,
            int party,
            ArchiveDungeonRules rules) {
        EntityType<? extends Mob> type = entityType(kind);
        Mob mob = type.create(level, EntitySpawnReason.EVENT);
        if (mob == null) {
            return;
        }
        List<BlockPos> markers = ArchiveRoomPlacer.monsterSpawnPositions(run, roomIndex);
        BlockPos position = safeSpawnMarker(level, run, roomIndex, markers, ordinal);
        if (position == null) {
            Yesterglass.LOGGER.warn("No safe enemy marker for archive run {} room {}", run.runId(), roomIndex);
            return;
        }
        long spawnSeed = mix64(run.rooms().get(roomIndex).encounterSeed()
                ^ (0xD1B54A32D192ED03L * (ordinal + 1L)) ^ wave);
        int difficulty = run.dungeonGraph().room(roomIndex).difficulty();
        List<ArchiveRoomModifier> modifiers = run.dungeonGraph().room(roomIndex).modifiers();
        double partyScale = 1.0D + Math.max(0, party - 1) * rules.healthPerAdditionalPlayer();
        double difficultyScale = 1.0D + difficulty * rules.healthPerDifficulty();
        boolean lesserBoss = run.dungeonGraph().room(roomIndex).category() == ArchiveRoomCategory.MINI_BOSS
                && wave >= 2
                && ordinal == 0;
        double reinforcement = modifiers.contains(ArchiveRoomModifier.REINFORCED_ENEMIES) ? 1.35D : 1.0D;
        double lesserBossHealth = lesserBoss ? 1.75D : 1.0D;
        var health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.setBaseValue(
                    health.getBaseValue() * partyScale * difficultyScale * reinforcement * lesserBossHealth);
            mob.setHealth(mob.getMaxHealth());
        }
        var damage = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            double curse = modifiers.contains(ArchiveRoomModifier.ANCIENT_CURSE) ? 1.20D : 1.0D;
            damage.setBaseValue(damage.getBaseValue()
                    * (1.0D + difficulty * rules.damagePerDifficulty())
                    * reinforcement
                    * curse
                    * (lesserBoss ? 1.20D : 1.0D));
        }
        var movement = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (movement != null && modifiers.contains(ArchiveRoomModifier.TIME_DISTORTION)) {
            movement.setBaseValue(movement.getBaseValue() * 1.15D);
        }
        if (modifiers.contains(ArchiveRoomModifier.REGENERATING_GUARDIANS)) {
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 60 * 30, 0, false, true));
        }
        mob.snapTo(position, (float) Math.floorMod(spawnSeed, 360L), 0.0F);
        mob.setPersistenceRequired();
        mob.setCustomName(lesserBoss
                ? Component.translatable("entity.tbos.lesser_boss." + kind.name().toLowerCase(java.util.Locale.ROOT))
                : Component.translatable(type.getDescriptionId()));
        mob.addTag(runTag(run.runId()));
        mob.addTag(roomTag(roomIndex));
        mob.addTag(KIND_TAG_PREFIX + kind.name().toLowerCase(java.util.Locale.ROOT));
        if (lesserBoss) {
            mob.addTag(LESSER_BOSS_TAG);
        }
        abilitiesFor(kind, spawnSeed, lesserBoss).forEach(ability -> mob.addTag(ability.entityTag()));
        level.addFreshEntity(mob);
        level.sendParticles(
                ParticleTypes.REVERSE_PORTAL,
                mob.getX(),
                mob.getY() + mob.getBbHeight() * 0.5D,
                mob.getZ(),
                lesserBoss ? 18 : 7,
                mob.getBbWidth() * 0.35D,
                mob.getBbHeight() * 0.25D,
                mob.getBbWidth() * 0.35D,
                0.02D);
        if (debugEnabled()) {
            Yesterglass.LOGGER.info("Archive encounter {} room {} spawned {} at {}",
                    run.runId(), roomIndex, type.getDescriptionId(), position);
        }
    }

    private static BlockPos safeSpawnMarker(
            ServerLevel level,
            ArchiveRun run,
            int roomIndex,
            List<BlockPos> markers,
            int ordinal) {
        if (markers.isEmpty()) {
            return null;
        }
        for (int offset = 0; offset < markers.size(); offset++) {
            BlockPos candidate = markers.get(Math.floorMod(ordinal + offset, markers.size()));
            if (!ArchiveRoomPlacer.isInsideRoom(run, roomIndex, candidate)
                    || !level.getBlockState(candidate).isAir()
                    || level.getBlockState(candidate.below()).isAir()) {
                continue;
            }
            boolean besidePlayer = level.getServer().getPlayerList().getPlayers().stream()
                    .filter(player -> run.containsMember(player.getUUID()))
                    .anyMatch(player -> player.blockPosition().distSqr(candidate) < 25.0D);
            if (!besidePlayer) {
                return candidate;
            }
        }
        return null;
    }

    private static int activeEnemyCount(ServerLevel level, ArchiveRun run, int roomIndex) {
        String runTag = runTag(run.runId());
        String roomTag = roomTag(roomIndex);
        return level.getEntitiesOfClass(
                        Monster.class,
                        ArchiveRoomPlacer.roomAabb(run, roomIndex),
                        mob -> mob.entityTags().contains(runTag)
                                && mob.entityTags().contains(roomTag)
                                && mob.isAlive())
                .size();
    }

    public static Set<ArchiveEnemyAbility> abilitiesFor(
            ArchiveEnemyKind kind, long spawnSeed, boolean lesserBoss) {
        EnumSet<ArchiveEnemyAbility> abilities = switch (kind) {
            case PARALLAX_WRAITH -> EnumSet.of(ArchiveEnemyAbility.PARALLAX_BLINK);
            case MERIDIAN_SENTINEL, HUSK, VINDICATOR, RAVAGER ->
                    EnumSet.of(ArchiveEnemyAbility.MERIDIAN_SHOCKWAVE);
            case HOUR_CANTOR -> EnumSet.of(
                    ArchiveEnemyAbility.ECHO_BOLT,
                    ArchiveEnemyAbility.PARALLAX_BLINK,
                    ArchiveEnemyAbility.WARD_AURA);
            case SKELETON, STRAY, EVOKER -> EnumSet.of(ArchiveEnemyAbility.ECHO_BOLT);
            case CAVE_SPIDER, SILVERFISH -> EnumSet.of(ArchiveEnemyAbility.SPLITTER);
        };
        // A deterministic secondary mutation keeps repeated runs varied while
        // preserving exact replayability from the encounter seed.
        if (kind != ArchiveEnemyKind.HOUR_CANTOR && Math.floorMod(spawnSeed, 5L) == 0L) {
            abilities.add(ArchiveEnemyAbility.PARALLAX_BLINK);
        }
        if (lesserBoss) {
            abilities.add(ArchiveEnemyAbility.WARD_AURA);
        }
        return Set.copyOf(abilities);
    }

    public static ArchiveEnemyDropKind rollEnemyDrop(
            ArchiveEnemyKind kind, long dropSeed, boolean lesserBoss) {
        RandomSource random = RandomSource.create(mix64(
                dropSeed ^ (0x4C4F4F545F484541L * (kind.ordinal() + 1L))));
        int roll = random.nextInt(100);
        if (lesserBoss) {
            return roll < 30
                    ? ArchiveEnemyDropKind.SOUL_HEART
                    : roll < 55
                            ? ArchiveEnemyDropKind.ECHO_HEART
                            : roll < 72
                                    ? ArchiveEnemyDropKind.CHARGE
                                    : roll < 87
                                            ? ArchiveEnemyDropKind.KEY
                                            : ArchiveEnemyDropKind.COIN;
        }
        return switch (roll) {
            case 0, 1, 2, 3, 4 -> ArchiveEnemyDropKind.SOUL_HEART;
            case 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22 ->
                    ArchiveEnemyDropKind.ECHO_HEART;
            case 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,
                    43, 44, 45, 46, 47 -> ArchiveEnemyDropKind.COIN;
            case 48, 49, 50, 51, 52, 53, 54, 55 -> ArchiveEnemyDropKind.KEY;
            case 56, 57, 58, 59, 60, 61, 62, 63, 64, 65 -> ArchiveEnemyDropKind.BOMB;
            case 66, 67, 68, 69, 70, 71, 72, 73, 74 -> ArchiveEnemyDropKind.CHARGE;
            default -> ArchiveEnemyDropKind.NONE;
        };
    }

    public static void handleEnemyDeath(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || !level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)
                || entity.entityTags().contains(SPLIT_CHILD_TAG)) {
            return;
        }
        ArchiveRun run = ArchiveRunSavedData.get(level.getServer()).all().stream()
                .filter(candidate -> entity.entityTags().contains(runTag(candidate.runId())))
                .findFirst()
                .orElse(null);
        if (run == null) {
            return;
        }
        int roomIndex = run.dungeonGraph().rooms().stream()
                .mapToInt(ArchiveRoomNode::index)
                .filter(index -> entity.entityTags().contains(roomTag(index)))
                .findFirst()
                .orElse(-1);
        ArchiveEnemyKind kind = java.util.Arrays.stream(ArchiveEnemyKind.values())
                .filter(candidate -> entity.entityTags().contains(
                        KIND_TAG_PREFIX + candidate.name().toLowerCase(java.util.Locale.ROOT)))
                .findFirst()
                .orElse(null);
        if (roomIndex < 0 || kind == null) {
            return;
        }

        long dropSeed = run.rooms().get(roomIndex).encounterSeed()
                ^ entity.getUUID().getMostSignificantBits()
                ^ entity.getUUID().getLeastSignificantBits();
        boolean lesserBoss = entity.entityTags().contains(LESSER_BOSS_TAG);
        ArchiveEnemyDropKind dropKind = rollEnemyDrop(kind, dropSeed, lesserBoss);
        ItemStack pickup = pickupStack(dropKind, dropSeed);
        if (!pickup.isEmpty()) {
            ItemEntity dropped = new ItemEntity(level, entity.getX(), entity.getY() + 0.35D, entity.getZ(), pickup);
            dropped.setDeltaMovement(0.0D, 0.22D, 0.0D);
            level.addFreshEntity(dropped);
            level.sendParticles(
                    dropKind == ArchiveEnemyDropKind.SOUL_HEART
                            ? ParticleTypes.SOUL_FIRE_FLAME
                            : ParticleTypes.HEART,
                    entity.getX(),
                    entity.getY() + 0.75D,
                    entity.getZ(),
                    lesserBoss ? 14 : 7,
                    0.35D,
                    0.35D,
                    0.35D,
                    0.03D);
            level.playSound(
                    null,
                    entity.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS,
                    0.55F,
                    dropKind == ArchiveEnemyDropKind.SOUL_HEART ? 0.65F : 1.45F);
        }
        if (entity.entityTags().contains(ArchiveEnemyAbility.SPLITTER.entityTag())) {
            spawnSplitChildren(level, run, roomIndex, entity);
        }
    }

    private static ItemStack pickupStack(ArchiveEnemyDropKind kind, long seed) {
        ItemStack stack = switch (kind) {
            case NONE -> ItemStack.EMPTY;
            case ECHO_HEART -> PotionContents.createItemStack(Items.POTION, Potions.HEALING);
            case SOUL_HEART -> PotionContents.createItemStack(Items.SPLASH_POTION, Potions.REGENERATION);
            case COIN -> new ItemStack(Items.GOLD_NUGGET, 1 + Math.floorMod((int) seed, 3));
            case KEY -> new ItemStack(Items.TRIPWIRE_HOOK);
            case BOMB -> new ItemStack(Items.FIRE_CHARGE);
            case CHARGE -> new ItemStack(Items.EXPERIENCE_BOTTLE, 1 + Math.floorMod((int) seed, 2));
        };
        if (!stack.isEmpty()) {
            Component name = switch (kind) {
                case ECHO_HEART -> Component.translatable("pickup.tbos.echo_heart");
                case SOUL_HEART -> Component.translatable("pickup.tbos.soul_heart");
                case COIN -> Component.translatable("pickup.tbos.coin");
                case KEY -> Component.translatable("pickup.tbos.key");
                case BOMB -> Component.translatable("pickup.tbos.bomb");
                case CHARGE -> Component.translatable("pickup.tbos.charge");
                case NONE -> Component.empty();
            };
            stack.set(DataComponents.CUSTOM_NAME, name);
        }
        return stack;
    }

    private static void spawnSplitChildren(
            ServerLevel level, ArchiveRun run, int roomIndex, LivingEntity parent) {
        for (int ordinal = 0; ordinal < 2; ordinal++) {
            Mob child = EntityType.SILVERFISH.create(level, EntitySpawnReason.TRIGGERED);
            if (child == null) {
                continue;
            }
            double offset = ordinal == 0 ? -0.45D : 0.45D;
            child.snapTo(parent.getX() + offset, parent.getY(), parent.getZ() - offset, parent.getYRot(), 0.0F);
            child.setPersistenceRequired();
            child.addTag(runTag(run.runId()));
            child.addTag(roomTag(roomIndex));
            child.addTag(KIND_TAG_PREFIX + ArchiveEnemyKind.SILVERFISH.name().toLowerCase(java.util.Locale.ROOT));
            child.addTag(SPLIT_CHILD_TAG);
            child.addTag(ArchiveEnemyAbility.PARALLAX_BLINK.entityTag());
            var health = child.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(Math.max(4.0D, health.getBaseValue() * 0.65D));
                child.setHealth(child.getMaxHealth());
            }
            level.addFreshEntity(child);
        }
        level.sendParticles(
                ParticleTypes.POOF,
                parent.getX(),
                parent.getY() + 0.4D,
                parent.getZ(),
                16,
                0.45D,
                0.25D,
                0.45D,
                0.06D);
        level.playSound(
                null,
                parent.blockPosition(),
                SoundEvents.SILVERFISH_AMBIENT,
                SoundSource.HOSTILE,
                0.8F,
                0.65F);
    }

    private static void tickEnemyAbilities(ServerLevel level, ArchiveRun run, int roomIndex) {
        String runTag = runTag(run.runId());
        String roomTag = roomTag(roomIndex);
        for (Monster mob : level.getEntitiesOfClass(
                Monster.class,
                ArchiveRoomPlacer.roomAabb(run, roomIndex),
                candidate -> candidate.isAlive()
                        && candidate.entityTags().contains(runTag)
                        && candidate.entityTags().contains(roomTag))) {
            ServerPlayer target = nearestTarget(level, run, roomIndex, mob);
            if (target == null) {
                continue;
            }
            long offset = mob.getUUID().getLeastSignificantBits() & Long.MAX_VALUE;
            if (mob.entityTags().contains(ArchiveEnemyAbility.ECHO_BOLT.entityTag())) {
                tickEchoBolt(level, mob, target, offset);
            }
            if (mob.entityTags().contains(ArchiveEnemyAbility.MERIDIAN_SHOCKWAVE.entityTag())) {
                tickShockwave(level, run, roomIndex, mob, offset);
            }
            if (mob.entityTags().contains(ArchiveEnemyAbility.PARALLAX_BLINK.entityTag())) {
                tickBlink(level, mob, target, offset);
            }
            if (mob.entityTags().contains(ArchiveEnemyAbility.WARD_AURA.entityTag())) {
                tickWardAura(level, mob, runTag, roomTag, offset);
            }
        }
    }

    private static ServerPlayer nearestTarget(
            ServerLevel level, ArchiveRun run, int roomIndex, LivingEntity mob) {
        ServerPlayer nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (ArchiveRunMember member : run.members()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(member.playerId());
            if (player == null || player.level() != level || !player.isAlive() || player.isSpectator()
                    || !ArchiveRoomPlacer.isInsideRoom(run, roomIndex, player.blockPosition())) {
                continue;
            }
            double distance = player.distanceToSqr(mob);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static void tickEchoBolt(
            ServerLevel level, Monster mob, ServerPlayer target, long offset) {
        long phase = Math.floorMod(level.getGameTime() + offset, 90L);
        if (phase == 0L) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getEyeY(), mob.getZ(),
                    12, 0.25D, 0.25D, 0.25D, 0.03D);
            level.playSound(null, mob.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 0.5F, 1.7F);
        } else if (phase == 14L && mob.distanceToSqr(target) <= 256.0D) {
            particleLine(level, ParticleTypes.END_ROD, mob.getEyePosition(), target.getEyePosition(), 12);
            target.hurtServer(level, level.damageSources().magic(), 2.5F);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 35, 0, false, true));
        }
    }

    private static void tickShockwave(
            ServerLevel level, ArchiveRun run, int roomIndex, Monster mob, long offset) {
        long phase = Math.floorMod(level.getGameTime() + offset, 110L);
        if (phase == 0L) {
            level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 0.2D, mob.getZ(),
                    22, 1.2D, 0.1D, 1.2D, 0.01D);
            level.playSound(null, mob.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                    SoundSource.HOSTILE, 0.8F, 0.7F);
        } else if (phase == 16L) {
            for (ArchiveRunMember member : run.members()) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(member.playerId());
                if (player == null || player.level() != level
                        || !ArchiveRoomPlacer.isInsideRoom(run, roomIndex, player.blockPosition())
                        || player.distanceToSqr(mob) > 36.0D) {
                    continue;
                }
                Vec3 push = player.position().subtract(mob.position());
                double horizontal = Math.max(0.1D, Math.sqrt(push.x * push.x + push.z * push.z));
                player.push(push.x / horizontal * 0.65D, 0.32D, push.z / horizontal * 0.65D);
                player.hurtServer(level, level.damageSources().magic(), 3.0F);
            }
            level.sendParticles(ParticleTypes.SONIC_BOOM, mob.getX(), mob.getY() + 0.25D, mob.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static void tickBlink(
            ServerLevel level, Monster mob, ServerPlayer target, long offset) {
        long phase = Math.floorMod(level.getGameTime() + offset, 125L);
        if (phase == 0L) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY() + 0.7D, mob.getZ(),
                    18, 0.35D, 0.55D, 0.35D, 0.02D);
        } else if (phase == 9L && mob.distanceToSqr(target) > 9.0D) {
            Vec3 behind = target.position().subtract(target.getLookAngle().multiply(2.5D, 0.0D, 2.5D));
            BlockPos destination = BlockPos.containing(behind.x, target.getY(), behind.z);
            if (level.getBlockState(destination).isAir()
                    && level.getBlockState(destination.above()).isAir()
                    && !level.getBlockState(destination.below()).isAir()) {
                Vec3 origin = mob.position();
                mob.teleportTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D);
                level.sendParticles(ParticleTypes.PORTAL, origin.x, origin.y + 0.6D, origin.z,
                        20, 0.35D, 0.45D, 0.35D, 0.05D);
                level.playSound(null, destination, SoundEvents.ENDERMAN_TELEPORT,
                        SoundSource.HOSTILE, 0.7F, 1.25F);
            }
        }
    }

    private static void tickWardAura(
            ServerLevel level, Monster source, String runTag, String roomTag, long offset) {
        if (Math.floorMod(level.getGameTime() + offset, 100L) != 0L) {
            return;
        }
        AABB aura = source.getBoundingBox().inflate(6.0D);
        for (Monster ally : level.getEntitiesOfClass(
                Monster.class,
                aura,
                candidate -> candidate.isAlive()
                        && candidate.entityTags().contains(runTag)
                        && candidate.entityTags().contains(roomTag))) {
            ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 55, 0, false, true));
        }
        level.sendParticles(ParticleTypes.ENCHANT, source.getX(), source.getY() + 1.0D, source.getZ(),
                24, 1.4D, 0.7D, 1.4D, 0.02D);
    }

    private static void particleLine(
            ServerLevel level,
            net.minecraft.core.particles.SimpleParticleType particle,
            Vec3 from,
            Vec3 to,
            int points) {
        Vec3 delta = to.subtract(from);
        for (int index = 0; index <= points; index++) {
            Vec3 point = from.add(delta.scale(index / (double) points));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static int totalWaves(ArchiveEncounterKind kind) {
        return switch (kind) {
            case EXPLORATION, REWARD -> 0;
            case TRAP, SKIRMISH, BOSS -> 1;
            case HUNT, GUARDIAN, HALL -> 2;
            case CHOIR -> 3;
        };
    }

    private static int activePartyInRoom(MinecraftServer server, ArchiveRun run, int roomIndex) {
        return membersInRoom(server, run, roomIndex).size();
    }

    private static List<UUID> membersInRoom(MinecraftServer server, ArchiveRun run, int roomIndex) {
        ArrayList<UUID> members = new ArrayList<>();
        for (ArchiveRunMember member : run.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(member.playerId());
            if (player != null
                    && player.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)
                    && ArchiveRoomPlacer.isInsideRoom(run, roomIndex, player.blockPosition())) {
                members.add(member.playerId());
            }
        }
        return List.copyOf(members);
    }

    private static void applyRoomModifiers(ServerLevel level, ArchiveRun run, int roomIndex) {
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        if (room.runtime().completed()) {
            return;
        }
        List<ArchiveRoomModifier> modifiers = room.modifiers();
        long tick = level.getGameTime();
        boolean refreshEffects = tick % 20L == Math.floorMod(
                run.rooms().get(roomIndex).encounterSeed(), 20L);
        int trapCadence = modifiers.contains(ArchiveRoomModifier.FASTER_TRAPS) ? 20 : 40;
        boolean triggerTrap = (room.category() == ArchiveRoomCategory.TRAP
                        || modifiers.contains(ArchiveRoomModifier.UNSTABLE_FLOORS)
                        || modifiers.contains(ArchiveRoomModifier.FASTER_TRAPS))
                && tick % trapCadence == Math.floorMod(
                        run.rooms().get(roomIndex).encounterSeed(), trapCadence);
        List<BlockPos> traps = triggerTrap ? ArchiveRoomPlacer.trapPositions(run, roomIndex) : List.of();
        if (triggerTrap && traps.isEmpty()) {
            traps = List.of(ArchiveRoomPlacer.roomSpawn(run, roomIndex));
        }
        for (ArchiveRunMember member : run.members()) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(member.playerId());
            if (player == null || player.level() != level
                    || !ArchiveRoomPlacer.isInsideRoom(run, roomIndex, player.blockPosition())) {
                continue;
            }
            if (refreshEffects) {
                if (modifiers.contains(ArchiveRoomModifier.DARKNESS)) {
                    player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
                }
                if (modifiers.contains(ArchiveRoomModifier.TIME_DISTORTION)) {
                    player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, true));
                }
                if (modifiers.contains(ArchiveRoomModifier.ANCIENT_CURSE)) {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, true));
                }
            }
            if (triggerTrap && traps.stream().anyMatch(
                    trap -> trap.distSqr(player.blockPosition()) <= 9.0D)) {
                float damage = modifiers.contains(ArchiveRoomModifier.ANCIENT_CURSE) ? 4.0F : 2.0F;
                player.hurtServer(level, level.damageSources().magic(), damage);
            }
        }
        if (triggerTrap) {
            for (BlockPos trap : traps) {
                level.sendParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        trap.getX() + 0.5D,
                        trap.getY() + 0.2D,
                        trap.getZ() + 0.5D,
                        8,
                        0.4D,
                        0.1D,
                        0.4D,
                        0.01D);
            }
        }
    }

    private static void spawnDirectLoot(ServerLevel level, ArchiveRun run, int roomIndex) {
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        if (room.category() == ArchiveRoomCategory.EXIT_REWARD) {
            return;
        }
        List<BlockPos> markers = ArchiveRoomPlacer.lootPositions(run, roomIndex);
        if (markers.isEmpty()) {
            return;
        }
        long lootSeed = mix64(run.rooms().get(roomIndex).encounterSeed() ^ 0x4C4F4F545F444952L);
        RandomSource random = RandomSource.create(lootSeed);
        ArchiveDungeonRules rules = dungeonRules();
        if (random.nextDouble() > rules.directLootProbability()
                && room.category() != ArchiveRoomCategory.TREASURE) {
            return;
        }
        BlockPos marker = markers.get(random.nextInt(markers.size()));
        List<ItemStack> loot = ArchiveLootRoller.roll(
                level, null, marker, room, rules, lootSeed, false);
        for (ItemStack stack : loot) {
            level.addFreshEntity(new ItemEntity(
                    level,
                    marker.getX() + 0.5D,
                    marker.getY() + 0.2D,
                    marker.getZ() + 0.5D,
                    stack));
        }
    }

    private static void grantRoomLoot(
            ServerPlayer player, ArchiveRun run, int roomIndex, int markerIndex, BlockPos cachePosition) {
        long lootSeed = mix64(run.rooms().get(roomIndex).encounterSeed()
                ^ (0x9E3779B97F4A7C15L * (markerIndex + 1L)));
        ArchiveRoomNode room = run.dungeonGraph().room(roomIndex);
        List<ItemStack> loot = ArchiveLootRoller.roll(
                (ServerLevel) player.level(),
                player,
                cachePosition,
                room,
                dungeonRules(),
                lootSeed,
                true);
        int qualityBonus = Math.min(3, room.difficulty() / 12);
        for (ItemStack stack : loot) {
            if (stack.is(ModItems.CHRONICLE_SHARD.get())) {
                stack.grow(qualityBonus);
            }
            dropAtCache((ServerLevel) player.level(), cachePosition, player, stack);
        }
        player.sendOverlayMessage(Component.literal("ARCHIVE CACHE - Recollection recovered")
                .withStyle(ChatFormatting.AQUA));
        if (debugEnabled()) {
            Yesterglass.LOGGER.info("Archive loot roll {} room {} marker {} seed {} tables {}",
                    run.runId(), roomIndex, markerIndex, lootSeed,
                    run.dungeonGraph().room(roomIndex).allowedLootTables());
        }
    }

    private static void cacheBreakEffect(
            ServerLevel level, BlockPos position, ArchiveContainerKind containerKind) {
        var particle = switch (containerKind) {
            case TRAPPED -> ParticleTypes.SMOKE;
            case CURSED -> ParticleTypes.WITCH;
            case HIDDEN, BOSS_REWARD -> ParticleTypes.END_ROD;
            default -> ParticleTypes.HAPPY_VILLAGER;
        };
        level.sendParticles(
                particle,
                position.getX() + 0.5D,
                position.getY() + 0.75D,
                position.getZ() + 0.5D,
                18,
                0.35D,
                0.35D,
                0.35D,
                0.035D);
        level.playSound(null, position, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.9F, 0.82F);
        switch (containerKind) {
            case TRAPPED ->
                    level.playSound(null, position, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.8F, 0.62F);
            case CURSED -> level.playSound(
                    null,
                    position,
                    SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),
                    SoundSource.BLOCKS,
                    0.7F,
                    1.25F);
            case HIDDEN, BOSS_REWARD -> level.playSound(
                    null,
                    position,
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.BLOCKS,
                    0.85F,
                    1.35F);
            case LOCKED -> level.playSound(
                    null,
                    position,
                    SoundEvents.RESPAWN_ANCHOR_CHARGE,
                    SoundSource.BLOCKS,
                    0.65F,
                    1.05F);
            case ORDINARY -> {
            }
        }
    }

    private static void announce(MinecraftServer server, ArchiveRun run, Component message) {
        for (ArchiveRunMember member : run.members()) {
            ServerPlayer player = server.getPlayerList().getPlayer(member.playerId());
            if (player != null) {
                player.sendOverlayMessage(message);
            }
        }
    }

    private static Component roomIntro(ArchiveRun run, int roomIndex) {
        ArchiveRoomPlan room = run.rooms().get(roomIndex);
        return Component.literal("LEVEL " + (room.level() + 1) + " - ROOM "
                        + (roomIndex + 1) + "/" + run.rooms().size() + " - "
                        + run.dungeonGraph().room(roomIndex).category().name().replace('_', ' '))
                .withStyle(ChatFormatting.AQUA);
    }

    private static String encounterTitle(ArchiveEncounterKind kind) {
        return switch (kind) {
            case EXPLORATION -> "ECHO CHAMBER";
            case REWARD -> "RECOLLECTION";
            case TRAP -> "ANCIENT TRAP";
            case SKIRMISH -> "FRACTURE";
            case HUNT -> "PARALLAX HUNT";
            case GUARDIAN -> "MERIDIAN GUARDIAN";
            case HALL -> "HALL ALIGNMENT";
            case CHOIR -> "CHOIR";
            case BOSS -> "HOUR CANTOR";
        };
    }

    private static int directionGlyph(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> throw new IllegalArgumentException("Puzzle direction must be horizontal");
        };
    }

    private static void sendPuzzleHud(ServerPlayer player, ArchiveRun run) {
        ServerLevel level = (ServerLevel) player.level();
        PacketDistributor.sendToPlayer(
                player,
                puzzlePayload(level, run, player, level.getServer().overworld().getGameTime()));
    }

    private static void puzzleSuccessEffect(ServerLevel level, BlockPos position, boolean completed) {
        level.sendParticles(
                completed ? ParticleTypes.FIREWORK : ParticleTypes.END_ROD,
                position.getX() + 0.5D,
                position.getY() + 0.85D,
                position.getZ() + 0.5D,
                completed ? 28 : 8,
                completed ? 0.75D : 0.25D,
                completed ? 0.60D : 0.25D,
                completed ? 0.75D : 0.25D,
                completed ? 0.09D : 0.025D);
        level.playSound(
                null,
                position,
                SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS,
                completed ? 1.1F : 0.55F,
                completed ? 0.72F : 1.55F);
    }

    private static void puzzleFailureEffect(ServerLevel level, BlockPos position) {
        level.sendParticles(
                ParticleTypes.WITCH,
                position.getX() + 0.5D,
                position.getY() + 0.8D,
                position.getZ() + 0.5D,
                16,
                0.4D,
                0.35D,
                0.4D,
                0.06D);
        level.playSound(
                null,
                position,
                SoundEvents.GLASS_BREAK,
                SoundSource.BLOCKS,
                0.75F,
                0.6F);
    }

    private static void lightBell(ServerLevel level, BlockPos position) {
        var state = level.getBlockState(position);
        if (state.is(ModBlocks.RESONANT_BELL.get())) {
            level.setBlock(position, state.setValue(com.nightbeam.tbos.block.ResonantBellBlock.LIT, true), 3);
            level.scheduleTick(position, ModBlocks.RESONANT_BELL.get(), 12);
        }
    }

    private static void dropAtCache(
            ServerLevel level, BlockPos position, ServerPlayer player, ItemStack stack) {
        ItemEntity item = new ItemEntity(
                level,
                position.getX() + 0.5D,
                position.getY() + 0.65D,
                position.getZ() + 0.5D,
                stack);
        item.setDefaultPickUpDelay();
        item.setTarget(player.getUUID());
        level.addFreshEntity(item);
    }

    private static boolean debugEnabled() {
        try {
            return YesterglassConfig.DUNGEON_DEBUG.get();
        } catch (IllegalStateException exception) {
            return false;
        }
    }

    private static ArchiveDungeonRules dungeonRules() {
        try {
            return YesterglassConfig.dungeonRules();
        } catch (IllegalStateException | IllegalArgumentException exception) {
            return ArchiveDungeonRules.DEFAULT;
        }
    }

    private static EntityType<? extends Mob> entityType(ArchiveEnemyKind kind) {
        return switch (kind) {
            case PARALLAX_WRAITH -> ModEntities.PARALLAX_WRAITH.get();
            case MERIDIAN_SENTINEL -> ModEntities.MERIDIAN_SENTINEL.get();
            case HOUR_CANTOR -> ModEntities.HOUR_CANTOR.get();
            case HUSK -> EntityType.HUSK;
            case SKELETON -> EntityType.SKELETON;
            case STRAY -> EntityType.STRAY;
            case CAVE_SPIDER -> EntityType.CAVE_SPIDER;
            case SILVERFISH -> EntityType.SILVERFISH;
            case VINDICATOR -> EntityType.VINDICATOR;
            case EVOKER -> EntityType.EVOKER;
            case RAVAGER -> EntityType.RAVAGER;
        };
    }

    private static String runTag(UUID runId) {
        return "tbos.run." + runId;
    }

    private static String roomTag(int room) {
        return "tbos.room." + room;
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
