package com.nightbeam.tbos.expedition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.codex.HallOfRecords;
import com.nightbeam.tbos.codex.Insight;
import com.nightbeam.tbos.codex.PlayerProfile;
import com.nightbeam.tbos.codex.Profiles;
import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.memory.MemoryBuild;
import com.nightbeam.tbos.memory.MemoryExpedition;
import com.nightbeam.tbos.relic.Rarity;
import com.nightbeam.tbos.relic.RelicEffects;
import com.nightbeam.tbos.relic.RelicGenerator;
import com.nightbeam.tbos.rpg.Json;
import com.nightbeam.tbos.rpg.JsonStore;
import com.nightbeam.tbos.rpg.RasBridge;
import com.nightbeam.tbos.rpg.RpgConfig;
import com.nightbeam.tbos.rpg.RpgNet;
import com.nightbeam.tbos.rpg.RpgSounds;
import com.nightbeam.tbos.run.ArchiveDimensions;
import com.nightbeam.tbos.run.ArchiveEncounterKind;
import com.nightbeam.tbos.run.ArchiveRoomPlacer;
import com.nightbeam.tbos.run.ArchiveRun;
import com.nightbeam.tbos.run.ArchiveRunManager;
import com.nightbeam.tbos.run.ArchiveRunMember;
import com.nightbeam.tbos.run.ArchiveRunSavedData;
import com.nightbeam.tbos.run.ArchiveRunStatus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * The expedition layer over Archive runs. It never changes how a run is built or
 * fought; it watches each run from the server tick and reacts: crediting Renown as
 * rooms clear, opening the Remember-or-Forget choice after a floor boss, scaling
 * every enemy to the key's Fracture Tier, running the Distortions, and banking or
 * forfeiting each member's Recollection when the run ends. Every run is an
 * expedition; one started without a key is tier 1 with no Distortions.
 */
public final class Expeditions {
    public static final String TIERED_TAG = "tbos.tiered";
    public static final String SHADE_TAG = "tbos.shade";
    private static final String RUN_TAG_PREFIX = "tbos.run.";

    private static final Map<UUID, ExpeditionState> STATES = new LinkedHashMap<>();
    private static final Set<UUID> HUD_ACTIVE = new HashSet<>();
    private static boolean dirty;

    private Expeditions() {
    }

    // ---- Lookup -----------------------------------------------------------------------------------------

    public static ExpeditionState state(UUID run) {
        return STATES.get(run);
    }

    public static ExpeditionState stateOf(ServerPlayer player) {
        ArchiveRun run = ArchiveRunSavedData.get(player.level().getServer()).findByMember(player.getUUID()).orElse(null);
        return run == null ? null : STATES.get(run.runId());
    }

    public static boolean isArchiveCreature(LivingEntity entity) {
        if (RpgCompat.entityId(entity).startsWith(Yesterglass.MOD_ID + ":")) {
            return true;
        }
        for (String tag : RpgCompat.tags(entity)) {
            if (tag.startsWith(RUN_TAG_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private static ExpeditionState stateFor(Entity entity) {
        for (String tag : RpgCompat.tags(entity)) {
            if (tag.startsWith(RUN_TAG_PREFIX)) {
                try {
                    return STATES.get(UUID.fromString(tag.substring(RUN_TAG_PREFIX.length())));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    /** Living Memories draft size for this run: two choices under Amnesia. */
    public static int draftSize(UUID run) {
        ExpeditionState state = STATES.get(run);
        return state != null && Distortion.AMNESIA.in(state.key.distortions()) ? 2 : 3;
    }

    public static double rewardFactor(ExpeditionKey key) {
        RpgConfig.Expeditions config = RpgConfig.get().expeditions;
        return 1 + config.rewardPerTier * (key.tier() - 1) + config.rewardPerHeat * key.heat();
    }

    // ---- Starting ---------------------------------------------------------------------------------------

    /** Called by the launcher right after the run is registered. */
    public static ExpeditionState begin(ArchiveRun run, ExpeditionKey key, ServerPlayer holder, String place) {
        ExpeditionState state = new ExpeditionState(run.runId(), key, holder.getUUID());
        state.place = place == null ? "" : place;
        for (ArchiveRunMember member : run.members()) {
            state.pool(member.playerId());
        }
        STATES.put(run.runId(), state);
        dirty = true;
        return state;
    }

    /** One-off effects once the first floor is live: Frailty, Lucid Start. */
    private static void activate(MinecraftServer server, ArchiveRunSavedData storage, ArchiveRun run, ExpeditionState state) {
        if (Distortion.FRAILTY.in(state.key.distortions()) && run.sharedRevives() > 0) {
            run = storage.replace(run.consumeRevive());
        }
        state.lastRevives = run.sharedRevives();
        MemoryExpedition memories = storage.memories(run.runId());
        if (memories != null) {
            for (MemoryBuild build : memories.members) {
                if (Profiles.get(server, build.player).rank(Insight.LUCID_START) > 0) {
                    List<Integer> choices = new ArrayList<>();
                    for (int id = 6; id <= 13; id++) {
                        if (!build.owns(id)) {
                            choices.add(id);
                        }
                    }
                    if (!choices.isEmpty()) {
                        build.acquire(choices.get(new Random(run.seed() ^ build.player.getLeastSignificantBits()).nextInt(choices.size())));
                    }
                }
            }
            storage.setDirty();
        }
        dirty = true;
    }

    // ---- Tick -------------------------------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(server);
        long now = server.overworld().getGameTime();
        Set<UUID> live = new HashSet<>();
        for (ArchiveRun run : storage.all()) {
            live.add(run.runId());
            ExpeditionState state = STATES.get(run.runId());
            if (state == null) {
                if (run.status().isTerminal()) {
                    continue;
                }
                UUID first = run.members().isEmpty() ? null : run.members().get(0).playerId();
                state = new ExpeditionState(run.runId(), ExpeditionKey.basic(1), first);
                state.floor = run.floor();
                for (ArchiveRunMember member : run.members()) {
                    state.pool(member.playerId());
                }
                STATES.put(run.runId(), state);
                dirty = true;
            }
            observe(server, storage, run, state, now);
        }
        for (Iterator<ExpeditionState> iterator = STATES.values().iterator(); iterator.hasNext(); ) {
            ExpeditionState state = iterator.next();
            if (!live.contains(state.run)) {
                if (!state.finalized) {
                    forfeit(server, state);
                }
                iterator.remove();
                dirty = true;
            }
        }
        DistortionEffects.tickBursts(server, now);
        if (server.getTickCount() % 10 == 0) {
            scaleEnemies(server);
            DistortionEffects.tickShades(server, storage, STATES, now);
            sync(server, storage, now);
        }
        if (dirty && server.getTickCount() % 100 == 0) {
            save(server);
        }
    }

    private static void observe(MinecraftServer server, ArchiveRunSavedData storage, ArchiveRun run,
            ExpeditionState state, long now) {
        ArchiveRunStatus status = run.status();
        for (ArchiveRunMember member : run.members()) {
            if (!state.pools.containsKey(member.playerId())) {
                state.pool(member.playerId());
                dirty = true;
            }
            ServerPlayer online = server.getPlayerList().getPlayer(member.playerId());
            if (online != null) {
                state.names.put(member.playerId(), RpgCompat.playerName(online));
            }
        }
        if (status == ArchiveRunStatus.ACTIVE && state.lastRevives < 0) {
            activate(server, storage, run, state);
            run = storage.find(run.runId()).orElse(run);
        }
        if (status == ArchiveRunStatus.ACTIVE) {
            int revives = run.sharedRevives();
            if (revives < state.lastRevives && !state.steadyUsed && anyMemberHas(server, run, Insight.STEADY_HAND)
                    && revives < ArchiveRun.MAX_SHARED_REVIVES) {
                run = storage.replace(run.restoreRevive());
                state.steadyUsed = true;
                revives = run.sharedRevives();
                tell(server, run, Component.translatable("expedition.tbos.steady_hand"), null);
            }
            state.lastRevives = revives;
        }
        if (run.floor() != state.floor) {
            if (run.floor() > state.floor) {
                state.momentum += RpgConfig.get().expeditions.momentumPerFloor;
                for (ArchiveRunMember member : run.members()) {
                    com.nightbeam.tbos.rpg.RpgAdvancements.award(server.getPlayerList().getPlayer(member.playerId()), com.nightbeam.tbos.rpg.RpgAdvancements.FORGET);
                }
            }
            state.beginFloor(run.floor());
            dirty = true;
        }
        if (status == ArchiveRunStatus.ACTIVE && state.floorStartTick < 0 && run.geometryPlaced()) {
            state.floorStartTick = now;
            dirty = true;
            InsightEffects.onFloorStart(server, run);
        }
        if (status == ArchiveRunStatus.ACTIVE) {
            credit(server, storage, run, state, now);
        }
        if (!status.name().equals(state.lastStatus)) {
            state.lastStatus = status.name();
            dirty = true;
            if ((status == ArchiveRunStatus.RETURNING_FAILURE || status == ArchiveRunStatus.FAILED) && !state.finalized) {
                forfeit(server, state);
            } else if ((status == ArchiveRunStatus.RETURNING_VICTORY || status == ArchiveRunStatus.COMPLETED) && !state.finalized) {
                for (ArchiveRunMember member : run.members()) {
                    if (!state.remembered.contains(member.playerId())) {
                        bank(server, state, member.playerId());
                    }
                }
                state.finalized = true;
            }
        }
    }

    private static void credit(MinecraftServer server, ArchiveRunSavedData storage, ArchiveRun run,
            ExpeditionState state, long now) {
        RpgConfig.Expeditions config = RpgConfig.get().expeditions;
        double factor = rewardFactor(state.key);
        for (int i = 0; i < run.rooms().size() && i < run.roomEncounterStates().size(); i++) {
            if (state.credited.contains(i) || !run.roomEncounterStates().get(i).complete()) {
                continue;
            }
            state.credited.add(i);
            dirty = true;
            ArchiveEncounterKind kind = run.rooms().get(i).encounterKind();
            int base = switch (kind) {
                case EXPLORATION, REWARD -> 0;
                case GUARDIAN -> config.renownWarden;
                case BOSS -> config.renownBoss;
                default -> config.renownRoom;
            };
            if (base <= 0) {
                continue;
            }
            for (ArchiveRunMember member : run.members()) {
                ExpeditionState.Pool pool = state.pool(member.playerId());
                pool.renown += base * factor;
                pool.floorRenown += base * factor;
            }
            if (kind == ArchiveEncounterKind.BOSS && !state.bossCredited) {
                state.bossCredited = true;
                state.floorsCleared++;
                long remaining = state.hourglassRemaining(now, maxRank(server, run, Insight.HOURGLASS_MASTERY), config.hourglassSeconds);
                boolean beatClock = remaining > 0;
                if (beatClock) {
                    state.hourglassWins++;
                    for (ArchiveRunMember member : run.members()) {
                        com.nightbeam.tbos.rpg.RpgAdvancements.award(server.getPlayerList().getPlayer(member.playerId()), com.nightbeam.tbos.rpg.RpgAdvancements.HOURGLASS);
                    }
                    long seconds = (now - state.floorStartTick) / 20L;
                    for (ArchiveRunMember member : run.members()) {
                        ExpeditionState.Pool pool = state.pool(member.playerId());
                        pool.renown += pool.floorRenown * config.hourglassRenownBonus;
                        PlayerProfile profile = Profiles.get(server, member.playerId());
                        if (profile.fastestHourglassSeconds <= 0 || seconds < profile.fastestHourglassSeconds) {
                            profile.fastestHourglassSeconds = (int) seconds;
                            profile.markDirty();
                        }
                    }
                }
                for (ArchiveRunMember member : run.members()) {
                    long seed = run.seed() ^ (state.floor * 0x9E3779B97F4A7C15L) ^ member.playerId().getMostSignificantBits();
                    state.pool(member.playerId()).relics.add(new ExpeditionState.RelicRoll(seed, state.key.tier(), Rarity.UNCOMMON.ordinal()));
                }
                tell(server, run, Component.translatable(beatClock ? "expedition.tbos.boss_recorded_hourglass" : "expedition.tbos.boss_recorded"),
                        RpgSounds.EXPEDITION_KEY_RISE.get());
            } else {
                for (ArchiveRunMember member : run.members()) {
                    ServerPlayer online = server.getPlayerList().getPlayer(member.playerId());
                    if (online != null) {
                        online.sendSystemMessage(Component.translatable("expedition.tbos.recorded",
                                Math.round(base * factor * state.momentum)), true);
                        online.level().playSound(null, online.blockPosition(), RpgSounds.EXPEDITION_RECORDED.get(), SoundSource.PLAYERS, 0.5F, 1.0F);
                    }
                }
            }
        }
        if (state.bossCredited && !state.choiceOpen) {
            int reward = run.dungeonGraph().rewardRoom();
            boolean open = reward < 0 || run.dungeonGraph().room(reward).runtime().completed();
            if (open) {
                state.choiceOpen = true;
                dirty = true;
                MemoryExpedition memories = storage.memories(run.runId());
                if (memories != null) {
                    memories.climax = true;
                    storage.setDirty();
                }
                for (ArchiveRunMember member : run.members()) {
                    ServerPlayer online = server.getPlayerList().getPlayer(member.playerId());
                    if (online != null) {
                        prompt(online);
                    }
                }
            }
        }
    }

    // ---- Remember or Forget -----------------------------------------------------------------------------

    /** Opens the Recollection screen for a member who has a choice to make. */
    public static void prompt(ServerPlayer player) {
        ExpeditionState state = stateOf(player);
        if (state == null || !state.choiceOpen || state.remembered.contains(player.getUUID())) {
            return;
        }
        JsonObject body = describe(player.level().getServer(), state, player.getUUID(), player.level().getGameTime());
        RpgNet.send(player, "choice", body);
    }

    public static void remember(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(server);
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        ExpeditionState state = run == null ? null : STATES.get(run.runId());
        if (state == null || run.status() != ArchiveRunStatus.ACTIVE) {
            player.sendSystemMessage(Component.translatable("expedition.tbos.no_choice"), true);
            return;
        }
        if (!state.choiceOpen) {
            player.sendSystemMessage(Component.translatable("expedition.tbos.choice_locked"), true);
            return;
        }
        if (state.remembered.contains(player.getUUID())) {
            return;
        }
        bank(server, state, player.getUUID());
        state.forgetVotes.remove(player.getUUID());
        if (run.members().size() >= 2) {
            UUID id = player.getUUID();
            if (ArchiveRunManager.leaveViaWaystone(player)) {
                ArchiveRun current = storage.find(run.runId()).orElse(run);
                if (current.status() == ArchiveRunStatus.ACTIVE && current.members().size() >= 2) {
                    storage.replace(current.removeMemoryMember(id));
                }
                MemoryExpedition memories = storage.memories(run.runId());
                if (memories != null) {
                    memories.removeMember(id);
                }
                com.nightbeam.tbos.memory.MemoryCombat.forget(id);
                storage.setDirty();
                tell(server, storage.find(run.runId()).orElse(run),
                        Component.translatable("expedition.tbos.member_remembered", player.getDisplayName()), null);
                advanceIfAgreed(server, storage, storage.find(run.runId()).orElse(run), state);
            }
        } else {
            storage.replace(run.beginReturn(player.level().getGameTime() + 40L));
            state.finalized = true;
        }
        dirty = true;
    }

    public static void forget(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(server);
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        ExpeditionState state = run == null ? null : STATES.get(run.runId());
        if (state == null || run.status() != ArchiveRunStatus.ACTIVE) {
            player.sendSystemMessage(Component.translatable("expedition.tbos.no_choice"), true);
            return;
        }
        if (!state.choiceOpen) {
            player.sendSystemMessage(Component.translatable("expedition.tbos.choice_locked"), true);
            return;
        }
        if (!state.forgetVotes.add(player.getUUID())) {
            return;
        }
        MemoryExpedition memories = storage.memories(run.runId());
        if (memories != null) {
            MemoryBuild build = memories.member(player.getUUID());
            if (build != null) {
                build.continueVote = true;
                storage.setDirty();
            }
        }
        player.level().playSound(null, player.blockPosition(), RpgSounds.EXPEDITION_FORGET.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        tell(server, run, Component.translatable("expedition.tbos.voted_forget", player.getDisplayName(),
                state.forgetVotes.size(), run.members().size()), null);
        dirty = true;
        advanceIfAgreed(server, storage, run, state);
    }

    private static void advanceIfAgreed(MinecraftServer server, ArchiveRunSavedData storage, ArchiveRun run, ExpeditionState state) {
        if (run.status() != ArchiveRunStatus.ACTIVE || !state.choiceOpen) {
            return;
        }
        for (ArchiveRunMember member : run.members()) {
            if (!state.forgetVotes.contains(member.playerId())) {
                return;
            }
        }
        ServerPlayer leader = null;
        for (ArchiveRunMember member : run.members()) {
            ServerPlayer online = server.getPlayerList().getPlayer(member.playerId());
            if (online != null) {
                leader = online;
                break;
            }
        }
        if (leader != null) {
            ArchiveRunManager.advanceFromRewardGateway(leader, ArchiveRoomPlacer.rewardGatewayPosition(run));
        }
    }

    /** Banks one member's Recollection into their Codex and inventory. */
    private static void bank(MinecraftServer server, ExpeditionState state, UUID id) {
        if (state.remembered.contains(id)) {
            return;
        }
        state.remembered.add(id);
        ExpeditionState.Pool pool = state.pool(id);
        PlayerProfile profile = Profiles.get(server, id);
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        RpgConfig.Expeditions config = RpgConfig.get().expeditions;
        double multiplier = state.momentum * (1 + 0.10 * profile.rank(Insight.DEEP_RECALL))
                * (1 + (player == null ? 0 : RelicEffects.renownBonus(player)));
        long renown = Math.round(pool.renown * multiplier);
        int levels = profile.addRenown(renown);

        List<ExpeditionState.RelicRoll> rolls = new ArrayList<>(pool.relics);
        if (profile.rank(Insight.HOARDER) > 0 && state.floorsCleared > 0) {
            rolls.add(new ExpeditionState.RelicRoll(state.run.getLeastSignificantBits() ^ id.getMostSignificantBits() ^ 0x484F4152L,
                    state.key.tier(), Rarity.COMMON.ordinal()));
        }
        JsonArray relicJson = new JsonArray();
        List<ItemStack> items = new ArrayList<>();
        if (RpgConfig.get().relics.enabled) {
            for (ExpeditionState.RelicRoll roll : rolls) {
                Rarity rarity = RelicGenerator.rollRarity(new Random(roll.seed() ^ 0x5DEECE66DL), roll.tier(),
                        profile.rank(Insight.KEEN_EYE), Rarity.byOrdinal(roll.minRarity()));
                items.add(RelicGenerator.generate(roll.seed(), roll.tier(), rarity));
                profile.relics[rarity.ordinal()]++;
                JsonArray entry = new JsonArray();
                entry.add(roll.seed());
                entry.add(roll.tier());
                entry.add(rarity.ordinal());
                relicJson.add(entry);
            }
        }
        int keyTier = 0;
        if (state.floorsCleared > 0) {
            keyTier = Math.min(config.maxTier, state.key.tier() + state.floorsCleared + state.hourglassWins
                    + profile.rank(Insight.KEYMAKER));
            ExpeditionKey reward = new ExpeditionKey(keyTier, state.key.distortions(), ExpeditionKey.NOT_WEEKLY, 0L);
            items.add(reward.stack());
            profile.bestTier = Math.max(profile.bestTier, state.key.tier());
            profile.bestFracture = Math.max(profile.bestFracture, state.key.fracture());
            profile.deepestFloor = Math.max(profile.deepestFloor, state.floorsCleared);
            String name = state.names.getOrDefault(id, profile.name);
            HallOfRecords.submit(server, new HallOfRecords.Entry(id, name, state.key.tier(), state.key.fracture(),
                    state.floorsCleared, 0));
            if (state.key.weekly()) {
                if (profile.weeklyBestWeek != state.key.week() || profile.weeklyBestFloor < state.floorsCleared) {
                    profile.weeklyBestWeek = state.key.week();
                    profile.weeklyBestFloor = state.floorsCleared;
                }
                HallOfRecords.submitWeekly(server, state.key.week(), new HallOfRecords.Entry(id, name, state.key.tier(),
                        state.key.fracture(), state.floorsCleared, Math.max(0, (server.overworld().getGameTime() - state.floorStartTick) / 20L)));
            }
        }
        profile.remembered++;
        profile.markDirty();
        pool.renown = 0;
        pool.relics.clear();
        dirty = true;

        if (player != null) {
            for (ItemStack stack : items) {
                RpgCompat.give(player, stack);
            }
            if (renown > 0) {
                RasBridge.addValor(player, renown * config.valorFraction);
            }
            player.level().playSound(null, player.blockPosition(), RpgSounds.EXPEDITION_REMEMBER.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            JsonObject body = new JsonObject();
            body.addProperty("renown", renown);
            body.addProperty("momentum", state.momentum);
            body.addProperty("levels", levels);
            body.addProperty("level", profile.level());
            body.addProperty("key", keyTier);
            body.add("relics", relicJson);
            RpgNet.send(player, "banked", body);
            com.nightbeam.tbos.codex.Codex.sync(player);
            awardBanked(player, state, profile, relicJson);
        } else {
            for (int i = 0; i < relicJson.size(); i++) {
                JsonArray entry = relicJson.get(i).getAsJsonArray();
                profile.pendingRelics.add(new long[]{entry.get(0).getAsLong(), entry.get(1).getAsLong(), entry.get(2).getAsLong()});
            }
            if (keyTier > 0) {
                profile.pendingKeys.add(new int[]{keyTier, state.key.distortions()});
            }
        }
    }

    private static void awardBanked(ServerPlayer player, ExpeditionState state, PlayerProfile profile, JsonArray relics) {
        com.nightbeam.tbos.rpg.RpgAdvancements.award(player, com.nightbeam.tbos.rpg.RpgAdvancements.REMEMBER);
        if (state.momentum >= 2.0 - 1e-6) {
            com.nightbeam.tbos.rpg.RpgAdvancements.award(player, com.nightbeam.tbos.rpg.RpgAdvancements.MOMENTUM);
        }
        if (state.floorsCleared > 0 && state.key.fracture() >= 15) {
            com.nightbeam.tbos.rpg.RpgAdvancements.award(player, com.nightbeam.tbos.rpg.RpgAdvancements.FRACTURED);
        }
        if (state.floorsCleared > 0 && state.key.tier() >= 20) {
            com.nightbeam.tbos.rpg.RpgAdvancements.award(player, com.nightbeam.tbos.rpg.RpgAdvancements.LAST_KEY);
        }
        if (state.floorsCleared > 0 && state.key.weekly()) {
            com.nightbeam.tbos.rpg.RpgAdvancements.award(player, com.nightbeam.tbos.rpg.RpgAdvancements.WEEKLY);
        }
        if (state.floorsCleared > 0 && state.pools.size() >= 4) {
            com.nightbeam.tbos.rpg.RpgAdvancements.award(player, com.nightbeam.tbos.rpg.RpgAdvancements.FELLOWSHIP);
        }
        for (int i = 0; i < relics.size(); i++) {
            if (relics.get(i).getAsJsonArray().get(2).getAsInt() == Rarity.REMNANT.ordinal()) {
                com.nightbeam.tbos.rpg.RpgAdvancements.award(player, com.nightbeam.tbos.rpg.RpgAdvancements.REMNANT_RELIC);
            }
        }
        if (profile.level() >= 10) {
            com.nightbeam.tbos.rpg.RpgAdvancements.award(player, com.nightbeam.tbos.rpg.RpgAdvancements.STORIED);
        }
        if (profile.level() >= RpgConfig.get().codex.maxLevel) {
            com.nightbeam.tbos.rpg.RpgAdvancements.award(player, com.nightbeam.tbos.rpg.RpgAdvancements.ARCHIVIST_ETERNAL);
        }
    }

    /** The run ended badly: every unbanked Recollection is lost. */
    private static void forfeit(MinecraftServer server, ExpeditionState state) {
        state.finalized = true;
        dirty = true;
        for (Map.Entry<UUID, ExpeditionState.Pool> entry : state.pools.entrySet()) {
            if (state.remembered.contains(entry.getKey())) {
                continue;
            }
            ExpeditionState.Pool pool = entry.getValue();
            long lost = Math.round(pool.renown * state.momentum);
            int relics = pool.relics.size();
            pool.renown = 0;
            pool.relics.clear();
            PlayerProfile profile = Profiles.get(server, entry.getKey());
            profile.forgotten++;
            profile.markDirty();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                player.sendSystemMessage(Component.translatable("expedition.tbos.forgotten", lost, relics));
                player.level().playSound(null, player.blockPosition(), RpgSounds.EXPEDITION_FORGOTTEN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                JsonObject body = new JsonObject();
                body.addProperty("renown", lost);
                body.addProperty("relics", relics);
                RpgNet.send(player, "forgotten", body);
            }
        }
        if (state.holder != null && RpgConfig.get().expeditions.dimmedKeyOnFailure) {
            ExpeditionKey dimmed = new ExpeditionKey(Math.max(1, state.key.tier() - 1), state.key.weekly() ? 0 : state.key.distortions(),
                    ExpeditionKey.NOT_WEEKLY, 0L);
            ServerPlayer holder = server.getPlayerList().getPlayer(state.holder);
            if (holder != null) {
                ItemStack key = dimmed.stack();
                RpgCompat.give(holder, key);
                holder.sendSystemMessage(Component.translatable("expedition.tbos.key_dimmed", dimmed.tier()));
            } else {
                Profiles.get(server, state.holder).pendingKeys.add(new int[]{dimmed.tier(), dimmed.distortions()});
            }
        }
    }

    // ---- Enemies ----------------------------------------------------------------------------------------

    private static void scaleEnemies(MinecraftServer server) {
        ServerLevel archive = server.getLevel(ArchiveDimensions.FRACTURED_ARCHIVE);
        if (archive == null || STATES.isEmpty()) {
            return;
        }
        RpgConfig.Expeditions config = RpgConfig.get().expeditions;
        RpgConfig.Distortions distortions = RpgConfig.get().distortions;
        for (Entity entity : archive.getAllEntities()) {
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                continue;
            }
            Set<String> tags = RpgCompat.tags(mob);
            if (tags.contains(TIERED_TAG)) {
                continue;
            }
            ExpeditionState state = stateFor(mob);
            if (state == null) {
                continue;
            }
            int tier = state.key.tier();
            int mask = state.key.distortions();
            boolean boss = tags.contains("tbos.final_boss") || tags.contains("tbos.lesser_boss");
            double health = (1 + config.healthPerTier * (tier - 1))
                    * (Distortion.FORTIFIED.in(mask) && !boss ? 1 + distortions.fortifiedHealth : 1)
                    * (Distortion.TYRANNICAL.in(mask) && boss ? 1 + distortions.tyrannicalHealth : 1);
            double damage = (1 + config.damagePerTier * (tier - 1))
                    * (Distortion.TYRANNICAL.in(mask) && boss ? 1 + distortions.tyrannicalDamage : 1);
            if (health > 1.0001) {
                RpgCompat.setModifier(mob, RpgCompat.Stat.MAX_HEALTH, "tier_health", health - 1, true);
                mob.setHealth(mob.getMaxHealth());
            }
            if (damage > 1.0001) {
                RpgCompat.setModifier(mob, RpgCompat.Stat.ATTACK_DAMAGE, "tier_damage", damage - 1, true);
            }
            mob.addTag(TIERED_TAG);
        }
    }

    /** Death of any living entity, from the loaders' death hook. */
    public static void onDeath(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level) || entity instanceof ServerPlayer) {
            return;
        }
        ExpeditionState state = stateFor(entity);
        if (state != null) {
            DistortionEffects.onDeath(level, entity, state);
        }
    }

    public static void extendHourglass(ServerPlayer player, int seconds) {
        ExpeditionState state = stateOf(player);
        if (state != null && Distortion.HOURGLASS.in(state.key.distortions()) && !state.bossCredited) {
            state.hourglassBonusTicks += seconds * 20L;
            dirty = true;
        }
    }

    // ---- Living Memories hooks --------------------------------------------------------------------------

    /** Second Draft: reroll the current Living Memories offer, once per floor. */
    public static void reroll(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(server);
        ArchiveRun run = storage.findByMember(player.getUUID()).orElse(null);
        ExpeditionState state = run == null ? null : STATES.get(run.runId());
        if (state == null || Profiles.get(player).rank(Insight.SECOND_DRAFT) <= 0) {
            return;
        }
        MemoryExpedition memories = storage.memories(run.runId());
        MemoryBuild build = memories == null ? null : memories.member(player.getUUID());
        if (build == null || build.offers.isEmpty() || !state.rerollUsed.add(player.getUUID())) {
            player.sendSystemMessage(Component.translatable("expedition.tbos.reroll_unavailable"), true);
            return;
        }
        boolean active = build.offers.stream().allMatch(offer -> offer < 6);
        build.offers.clear();
        build.offer(run.seed() ^ 0x7265726F6C6CL ^ state.floor, active, draftSize(run.runId()));
        storage.setDirty();
        dirty = true;
        player.level().playSound(null, player.blockPosition(), RpgSounds.CODEX_PAGE.get(), SoundSource.PLAYERS, 0.9F, 1.1F);
        player.sendSystemMessage(Component.translatable("expedition.tbos.rerolled"), true);
    }

    /** Whether the player may still use Last Stand on this floor; consumes it if so. */
    public static boolean consumeLastStand(ServerPlayer player) {
        ExpeditionState state = stateOf(player);
        if (state == null || !state.lastStandUsed.add(player.getUUID())) {
            return false;
        }
        dirty = true;
        return true;
    }

    // ---- HUD sync ---------------------------------------------------------------------------------------

    private static void sync(MinecraftServer server, ArchiveRunSavedData storage, long now) {
        Set<UUID> active = new HashSet<>();
        for (ExpeditionState state : STATES.values()) {
            ArchiveRun run = storage.find(state.run).orElse(null);
            if (run == null || state.finalized || run.status() != ArchiveRunStatus.ACTIVE) {
                continue;
            }
            for (ArchiveRunMember member : run.members()) {
                ServerPlayer player = server.getPlayerList().getPlayer(member.playerId());
                if (player == null || !player.level().dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
                    continue;
                }
                active.add(player.getUUID());
                RpgNet.send(player, "exp", describe(server, state, player.getUUID(), now));
            }
        }
        for (UUID previous : HUD_ACTIVE) {
            if (!active.contains(previous)) {
                ServerPlayer player = server.getPlayerList().getPlayer(previous);
                if (player != null) {
                    JsonObject body = new JsonObject();
                    body.addProperty("none", true);
                    RpgNet.send(player, "exp", body);
                }
            }
        }
        HUD_ACTIVE.clear();
        HUD_ACTIVE.addAll(active);
    }

    static JsonObject describe(MinecraftServer server, ExpeditionState state, UUID viewer, long now) {
        JsonObject body = new JsonObject();
        ExpeditionState.Pool pool = state.pool(viewer);
        body.addProperty("tier", state.key.tier());
        body.addProperty("distortions", state.key.distortions());
        body.addProperty("weekly", state.key.weekly());
        body.addProperty("floor", state.floor + 1);
        body.addProperty("cleared", state.floorsCleared);
        body.addProperty("momentum", state.momentum);
        body.addProperty("renown", Math.round(pool.renown * state.momentum));
        body.addProperty("relics", pool.relics.size());
        body.addProperty("place", state.place);
        int mastery = Profiles.get(server, viewer).rank(Insight.HOURGLASS_MASTERY);
        body.addProperty("hourglass", state.hourglassRemaining(now, mastery, RpgConfig.get().expeditions.hourglassSeconds));
        body.addProperty("choice", state.choiceOpen && !state.remembered.contains(viewer));
        body.addProperty("voted", state.forgetVotes.contains(viewer));
        body.addProperty("votes", state.forgetVotes.size());
        int keyTier = Math.min(RpgConfig.get().expeditions.maxTier, state.key.tier() + state.floorsCleared + state.hourglassWins
                + Profiles.get(server, viewer).rank(Insight.KEYMAKER));
        body.addProperty("next_key", state.floorsCleared > 0 ? keyTier : 0);
        body.addProperty("reroll", Profiles.get(server, viewer).rank(Insight.SECOND_DRAFT) > 0
                && !state.rerollUsed.contains(viewer));
        JsonArray party = new JsonArray();
        state.pools.keySet().forEach(id -> {
            JsonObject member = new JsonObject();
            member.addProperty("name", state.names.getOrDefault(id, "?"));
            member.addProperty("voted", state.forgetVotes.contains(id));
            member.addProperty("remembered", state.remembered.contains(id));
            party.add(member);
        });
        body.add("party", party);
        return body;
    }

    // ---- Helpers ----------------------------------------------------------------------------------------

    private static boolean anyMemberHas(MinecraftServer server, ArchiveRun run, Insight insight) {
        for (ArchiveRunMember member : run.members()) {
            if (Profiles.get(server, member.playerId()).rank(insight) > 0) {
                return true;
            }
        }
        return false;
    }

    private static int maxRank(MinecraftServer server, ArchiveRun run, Insight insight) {
        int rank = 0;
        for (ArchiveRunMember member : run.members()) {
            rank = Math.max(rank, Profiles.get(server, member.playerId()).rank(insight));
        }
        return rank;
    }

    private static void tell(MinecraftServer server, ArchiveRun run, Component message, SoundEvent sound) {
        for (ArchiveRunMember member : run.members()) {
            ServerPlayer online = server.getPlayerList().getPlayer(member.playerId());
            if (online != null) {
                online.sendSystemMessage(message);
                if (sound != null) {
                    online.level().playSound(null, online.blockPosition(), sound, SoundSource.PLAYERS, 0.9F, 1.0F);
                }
            }
        }
    }

    // ---- Persistence ------------------------------------------------------------------------------------

    public static void load(MinecraftServer server) {
        STATES.clear();
        JsonObject json = JsonStore.read(JsonStore.directory(server, "expeditions.json"));
        for (var element : Json.arr(json, "runs")) {
            try {
                ExpeditionState state = ExpeditionState.fromJson(element.getAsJsonObject());
                STATES.put(state.run, state);
            } catch (RuntimeException exception) {
                Yesterglass.LOGGER.warn("Skipped an unreadable expedition record", exception);
            }
        }
        dirty = false;
    }

    public static void save(MinecraftServer server) {
        JsonArray runs = new JsonArray();
        STATES.values().forEach(state -> runs.add(state.toJson()));
        JsonObject json = new JsonObject();
        json.add("runs", runs);
        JsonStore.write(JsonStore.directory(server, "expeditions.json"), json);
        dirty = false;
    }

    public static void forgetPlayer(UUID player) {
        HUD_ACTIVE.remove(player);
    }

    public static void clear() {
        STATES.clear();
        HUD_ACTIVE.clear();
        DistortionEffects.clear();
        dirty = false;
    }

    static void markDirty() {
        dirty = true;
    }
}
