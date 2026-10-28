package com.nightbeam.tbos.codex;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.expedition.ExpeditionKey;
import com.nightbeam.tbos.expedition.Expeditions;
import com.nightbeam.tbos.relic.RelicGenerator;
import com.nightbeam.tbos.relic.Rarity;
import com.nightbeam.tbos.rpg.Json;
import com.nightbeam.tbos.rpg.RpgConfig;
import com.nightbeam.tbos.rpg.RpgNet;
import com.nightbeam.tbos.rpg.RpgSounds;
import com.nightbeam.tbos.run.ArchiveRunSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Server side of the Archive Codex screen: one snapshot message with everything
 * the four tabs show, and the handful of actions a player can take from it.
 */
public final class Codex {
    private Codex() {
    }

    public static void registerActions() {
        RpgNet.on("codex_open", (player, body) -> {
            sync(player);
            JsonObject open = new JsonObject();
            open.addProperty("tab", Json.integer(body, "tab", 0));
            RpgNet.send(player, "open_codex", open);
        });
        RpgNet.on("codex_sync", (player, body) -> sync(player));
        RpgNet.on("learn", (player, body) -> learn(player, Json.str(body, "insight", "")));
        RpgNet.on("respec", (player, body) -> respec(player));
        RpgNet.on("weekly", (player, body) -> claimWeekly(player));
        RpgNet.on("remember", (player, body) -> Expeditions.remember(player));
        RpgNet.on("forget", (player, body) -> Expeditions.forget(player));
        RpgNet.on("reroll", (player, body) -> Expeditions.reroll(player));
        RpgNet.on("choice", (player, body) -> Expeditions.prompt(player));
    }

    public static void learn(ServerPlayer player, String id) {
        Insight insight = Insight.byId(id);
        PlayerProfile profile = Profiles.get(player);
        if (insight == null || !profile.learn(insight)) {
            return;
        }
        player.level().playSound(null, player.blockPosition(), RpgSounds.CODEX_INSIGHT.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        sync(player);
    }

    public static void respec(ServerPlayer player) {
        if (!RpgConfig.get().codex.freeRespec) {
            return;
        }
        if (ArchiveRunSavedData.get(player.level().getServer()).findByMember(player.getUUID()).isPresent()) {
            player.sendSystemMessage(Component.translatable("codex.tbos.respec_in_run"), true);
            return;
        }
        Profiles.get(player).respec();
        player.level().playSound(null, player.blockPosition(), RpgSounds.CODEX_PAGE.get(), SoundSource.PLAYERS, 0.9F, 0.8F);
        sync(player);
    }

    public static void claimWeekly(ServerPlayer player) {
        if (!RpgConfig.get().weekly.enabled) {
            return;
        }
        PlayerProfile profile = Profiles.get(player);
        long week = Weekly.currentWeek();
        if (profile.weeklyClaimed == week) {
            player.sendSystemMessage(Component.translatable("codex.tbos.weekly.claimed_already"), true);
            return;
        }
        profile.weeklyClaimed = week;
        profile.markDirty();
        give(player, Weekly.key(player.level().getServer(), week).stack());
        player.level().playSound(null, player.blockPosition(), RpgSounds.EXPEDITION_KEY_RISE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        sync(player);
    }

    /** Hands over anything banked while the player was offline. */
    public static void deliverPending(ServerPlayer player) {
        PlayerProfile profile = Profiles.get(player);
        if (profile.pendingKeys.isEmpty() && profile.pendingRelics.isEmpty()) {
            return;
        }
        for (int[] key : profile.pendingKeys) {
            give(player, new ExpeditionKey(key[0], key[1], ExpeditionKey.NOT_WEEKLY, 0L).stack());
        }
        for (long[] relic : profile.pendingRelics) {
            give(player, RelicGenerator.generate(relic[0], (int) relic[1], Rarity.byOrdinal((int) relic[2])));
        }
        player.sendSystemMessage(Component.translatable("codex.tbos.pending_delivered",
                profile.pendingKeys.size() + profile.pendingRelics.size()));
        profile.pendingKeys.clear();
        profile.pendingRelics.clear();
        profile.markDirty();
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        RpgCompat.give(player, stack);
    }

    public static void sync(ServerPlayer player) {
        PlayerProfile profile = Profiles.get(player);
        JsonObject body = new JsonObject();
        body.addProperty("level", profile.level());
        body.addProperty("max_level", RpgConfig.get().codex.maxLevel);
        long[] progress = profile.progress();
        body.addProperty("progress", progress[0]);
        body.addProperty("needed", progress[1]);
        body.addProperty("renown", profile.renown);
        body.addProperty("insight", profile.unspentInsight());
        body.addProperty("respec", RpgConfig.get().codex.freeRespec);
        JsonObject insights = new JsonObject();
        profile.insights.forEach((insight, ranks) -> insights.addProperty(insight.id(), ranks));
        body.add("insights", insights);
        JsonObject kills = new JsonObject();
        for (Bestiary.Entry entry : Bestiary.ENTRIES) {
            int count = profile.kills.getOrDefault(entry.id(), 0);
            if (count > 0) {
                kills.addProperty(entry.id(), count);
            }
        }
        body.add("kills", kills);
        JsonObject records = new JsonObject();
        records.addProperty("best_tier", profile.bestTier);
        records.addProperty("best_fracture", profile.bestFracture);
        records.addProperty("deepest", profile.deepestFloor);
        records.addProperty("remembered", profile.remembered);
        records.addProperty("forgotten", profile.forgotten);
        records.addProperty("total_renown", profile.totalRenownBanked);
        records.addProperty("hourglass", profile.fastestHourglassSeconds);
        JsonArray relics = new JsonArray();
        for (int count : profile.relics) {
            relics.add(count);
        }
        records.add("relics", relics);
        body.add("records", records);
        body.add("hall", entries(HallOfRecords.allTime()));
        long week = Weekly.currentWeek();
        JsonObject weekly = new JsonObject();
        weekly.addProperty("enabled", RpgConfig.get().weekly.enabled);
        weekly.addProperty("week", week);
        weekly.addProperty("claimed", profile.weeklyClaimed == week);
        weekly.addProperty("seconds_left", Weekly.secondsLeft());
        ExpeditionKey key = Weekly.key(player.level().getServer(), week);
        weekly.addProperty("tier", key.tier());
        weekly.addProperty("distortions", key.distortions());
        weekly.addProperty("best", profile.weeklyBestWeek == week ? profile.weeklyBestFloor : 0);
        weekly.add("board", entries(HallOfRecords.weekly()));
        body.add("weekly", weekly);
        RpgNet.send(player, "codex", body);
    }

    private static JsonArray entries(java.util.List<HallOfRecords.Entry> list) {
        JsonArray array = new JsonArray();
        for (HallOfRecords.Entry entry : list) {
            JsonObject json = new JsonObject();
            json.addProperty("name", entry.name());
            json.addProperty("tier", entry.tier());
            json.addProperty("fracture", entry.fracture());
            json.addProperty("floor", entry.floor());
            json.addProperty("seconds", entry.seconds());
            array.add(json);
        }
        return array;
    }
}
