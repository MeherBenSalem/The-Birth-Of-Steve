package com.nightbeam.tbos.expedition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nightbeam.tbos.codex.Insight;
import com.nightbeam.tbos.codex.PlayerProfile;
import com.nightbeam.tbos.codex.Profiles;
import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.party.Parties;
import com.nightbeam.tbos.registry.ModBlocks;
import com.nightbeam.tbos.relic.RelicGenerator;
import com.nightbeam.tbos.rpg.Json;
import com.nightbeam.tbos.rpg.RpgConfig;
import com.nightbeam.tbos.rpg.RpgNet;
import com.nightbeam.tbos.rpg.RpgSounds;
import com.nightbeam.tbos.run.ArchiveDimensions;
import com.nightbeam.tbos.run.ArchiveRunSavedData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Using a Memory Key: it opens the Attunement screen where the holder sees the
 * tier, toggles Distortions and checks who is coming, and only "Begin" (validated
 * again on the server) consumes the key and starts the expedition.
 */
public final class Attunement {
    public enum Place { WAYSTONE, GATEWAY, RECOLLECTION }

    private record Session(InteractionHand hand, Place place, BlockPos pos, RpgCompat.StructureHit structure, long opened) {
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final long SESSION_TICKS = 20L * 60 * 5;

    private Attunement() {
    }

    public static void registerActions() {
        RpgNet.on("attune_toggle", (player, body) -> toggle(player, Json.integer(body, "d", -1)));
        RpgNet.on("attune_begin", (player, body) -> begin(player));
        RpgNet.on("attune_close", (player, body) -> SESSIONS.remove(player.getUUID()));
    }

    /** From the key item, or from a Waystone or Gateway clicked with a key. */
    public static InteractionResult useKey(Level level, BlockPos clicked, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        if (level.dimension().equals(ArchiveDimensions.FRACTURED_ARCHIVE)) {
            serverPlayer.sendSystemMessage(Component.translatable("expedition.tbos.key_inside_archive"), true);
            return InteractionResult.CONSUME;
        }
        if (ArchiveRunSavedData.get(serverLevel.getServer()).findByMember(player.getUUID()).isPresent()) {
            serverPlayer.sendSystemMessage(Component.translatable("message.tbos.archive.already_in_run"), true);
            return InteractionResult.CONSUME;
        }
        BlockState state = level.getBlockState(clicked);
        Place place;
        RpgCompat.StructureHit structure = null;
        if (state.is(ModBlocks.WAYSTONE.get())) {
            place = Place.WAYSTONE;
        } else if (state.is(ModBlocks.RIFT_THRESHOLD.get())) {
            place = Place.GATEWAY;
        } else {
            structure = Recollections.find(serverLevel, serverPlayer.blockPosition()).orElse(null);
            if (structure == null) {
                serverPlayer.sendSystemMessage(Component.translatable("expedition.tbos.key_needs_place"), true);
                return InteractionResult.CONSUME;
            }
            PlayerProfile profile = Profiles.get(serverPlayer);
            if (RpgConfig.get().expeditions.recollectOncePerStructure
                    && profile.recollected.contains(Recollections.memoryKey(structure))) {
                serverPlayer.sendSystemMessage(Component.translatable("expedition.tbos.already_recollected",
                        Recollections.name(structure.id())), true);
                return InteractionResult.CONSUME;
            }
            place = Place.RECOLLECTION;
        }
        SESSIONS.put(player.getUUID(), new Session(hand, place, clicked.immutable(), structure, level.getGameTime()));
        level.playSound(null, player.blockPosition(), RpgSounds.EXPEDITION_ATTUNE.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        send(serverPlayer, true);
        com.nightbeam.tbos.rpg.RpgAdvancements.award(serverPlayer, com.nightbeam.tbos.rpg.RpgAdvancements.ROOT);
        return InteractionResult.SUCCESS;
    }

    private static Session session(ServerPlayer player) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || player.level().getGameTime() - session.opened > SESSION_TICKS) {
            SESSIONS.remove(player.getUUID());
            return null;
        }
        return session;
    }

    private static void toggle(ServerPlayer player, int ordinal) {
        Session session = session(player);
        if (session == null || ordinal < 0 || ordinal >= Distortion.values().length) {
            return;
        }
        ItemStack stack = player.getItemInHand(session.hand);
        if (!ExpeditionKey.isKey(stack)) {
            return;
        }
        ExpeditionKey key = ExpeditionKey.read(stack);
        if (key.weekly()) {
            return;
        }
        ExpeditionKey toggled = key.withDistortions(key.distortions() ^ Distortion.values()[ordinal].bit());
        toggled.write(stack);
        player.level().playSound(null, player.blockPosition(), RpgSounds.EXPEDITION_DISTORTION.get(), SoundSource.PLAYERS, 0.8F,
                Distortion.values()[ordinal].in(toggled.distortions()) ? 0.9F : 1.2F);
        send(player, false);
    }

    private static void begin(ServerPlayer player) {
        Session session = session(player);
        if (session == null) {
            return;
        }
        ItemStack stack = player.getItemInHand(session.hand);
        if (!ExpeditionKey.isKey(stack)) {
            player.sendSystemMessage(Component.translatable("expedition.tbos.key_missing"), true);
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        long placeKey;
        String name = "";
        switch (session.place) {
            case WAYSTONE, GATEWAY -> {
                BlockState state = level.getBlockState(session.pos);
                boolean valid = session.place == Place.WAYSTONE ? state.is(ModBlocks.WAYSTONE.get()) : state.is(ModBlocks.RIFT_THRESHOLD.get());
                if (!valid || player.blockPosition().distSqr(session.pos) > 12 * 12) {
                    player.sendSystemMessage(Component.translatable("expedition.tbos.moved_away"), true);
                    return;
                }
                placeKey = session.pos.asLong();
            }
            default -> {
                RpgCompat.StructureHit hit = Recollections.find(level, player.blockPosition()).orElse(null);
                if (hit == null || session.structure == null || hit.startKey() != session.structure.startKey()) {
                    player.sendSystemMessage(Component.translatable("expedition.tbos.moved_away"), true);
                    return;
                }
                placeKey = hit.startKey() ^ hit.id().hashCode();
                name = hit.id();
            }
        }
        ExpeditionKey key = ExpeditionKey.read(stack);
        List<ServerPlayer> party = Parties.nearby(player, RpgConfig.get().expeditions.partyRadius);
        long seed = ExpeditionLauncher.seed(level.getServer(), key, placeKey, session.place != Place.RECOLLECTION);
        ExpeditionLauncher.Result result = ExpeditionLauncher.launch(player, key, seed, name, party);
        if (result != ExpeditionLauncher.Result.STARTED) {
            player.sendSystemMessage(Component.translatable("expedition.tbos.launch." + result.name().toLowerCase(java.util.Locale.ROOT)), true);
            return;
        }
        if (session.place == Place.RECOLLECTION && session.structure != null) {
            PlayerProfile profile = Profiles.get(player);
            profile.recollected.add(Recollections.memoryKey(session.structure));
            profile.markDirty();
            com.nightbeam.tbos.rpg.RpgAdvancements.award(player, com.nightbeam.tbos.rpg.RpgAdvancements.RECOLLECTION);
        }
        stack.shrink(1);
        SESSIONS.remove(player.getUUID());
        level.playSound(null, player.blockPosition(),
                session.place == Place.RECOLLECTION ? RpgSounds.EXPEDITION_RECOLLECTION.get() : RpgSounds.EXPEDITION_BEGIN.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        RpgNet.send(player, "attune_done", new JsonObject());
    }

    private static void send(ServerPlayer player, boolean open) {
        Session session = session(player);
        if (session == null) {
            return;
        }
        ItemStack stack = player.getItemInHand(session.hand);
        if (!ExpeditionKey.isKey(stack)) {
            return;
        }
        ExpeditionKey key = ExpeditionKey.read(stack);
        PlayerProfile profile = Profiles.get(player);
        JsonObject body = new JsonObject();
        body.addProperty("open", open);
        body.addProperty("tier", key.tier());
        body.addProperty("distortions", key.distortions());
        body.addProperty("weekly", key.weekly());
        body.addProperty("week", key.weekly() ? key.week() : 0);
        body.addProperty("factor", Expeditions.rewardFactor(key));
        body.addProperty("place", session.place.name().toLowerCase(java.util.Locale.ROOT));
        body.addProperty("structure", session.structure == null ? "" : session.structure.id());
        body.addProperty("boss_renown", Math.round(RpgConfig.get().expeditions.renownBoss * Expeditions.rewardFactor(key)));
        body.addProperty("hourglass", RpgConfig.get().expeditions.hourglassSeconds + 60 * profile.rank(Insight.HOURGLASS_MASTERY));
        JsonArray odds = new JsonArray();
        double[] weights = RelicGenerator.weights(key.tier(), profile.rank(Insight.KEEN_EYE));
        double total = 0;
        for (double weight : weights) {
            total += weight;
        }
        for (double weight : weights) {
            odds.add(Math.round(weight / total * 1000.0) / 10.0);
        }
        body.add("odds", odds);
        JsonArray party = new JsonArray();
        ArchiveRunSavedData storage = ArchiveRunSavedData.get(player.level().getServer());
        for (ServerPlayer member : Parties.nearby(player, RpgConfig.get().expeditions.partyRadius)) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", RpgCompat.playerName(member));
            entry.addProperty("busy", member != player && storage.findByMember(member.getUUID()).isPresent());
            party.add(entry);
        }
        body.add("party", party);
        RpgNet.send(player, "attune", body);
    }

    public static void forget(UUID player) {
        SESSIONS.remove(player);
    }

    public static void clear() {
        SESSIONS.clear();
    }
}
