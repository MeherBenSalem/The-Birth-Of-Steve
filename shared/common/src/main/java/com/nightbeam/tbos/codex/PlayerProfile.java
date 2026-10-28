package com.nightbeam.tbos.codex;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nightbeam.tbos.rpg.Json;
import com.nightbeam.tbos.rpg.RpgConfig;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

/** One player's permanent Archive Codex: Renown, Insights, Bestiary and records. */
public final class PlayerProfile {
    public static final int RARITIES = 6;

    public final UUID id;
    public String name = "";
    public long renown;
    public final Map<Insight, Integer> insights = new EnumMap<>(Insight.class);
    public final Map<String, Integer> kills = new TreeMap<>();
    public final Set<String> recollected = new LinkedHashSet<>();
    public long weeklyClaimed = Long.MIN_VALUE;
    /** Rewards banked while offline: keys as {tier, distortions}, Relics as {seed, tier, rarity}. */
    public final java.util.List<int[]> pendingKeys = new java.util.ArrayList<>();
    public final java.util.List<long[]> pendingRelics = new java.util.ArrayList<>();

    // Records
    public int bestTier;
    public int bestFracture;
    public long deepestFloor;
    public int remembered;
    public int forgotten;
    public long totalRenownBanked;
    public final int[] relics = new int[RARITIES];
    public int fastestHourglassSeconds;
    public long weeklyBestWeek = Long.MIN_VALUE;
    public long weeklyBestFloor;

    boolean dirty;

    public PlayerProfile(UUID id) {
        this.id = id;
    }

    // ---- Renown and Insight -----------------------------------------------------------------------------

    /** Renown needed to go from {@code level} to {@code level + 1}. */
    public static long costOf(int level) {
        RpgConfig.Codex codex = RpgConfig.get().codex;
        return codex.levelBase + (long) codex.levelStep * level;
    }

    public int level() {
        int level = 1;
        long remaining = renown;
        int max = RpgConfig.get().codex.maxLevel;
        while (level < max && remaining >= costOf(level)) {
            remaining -= costOf(level);
            level++;
        }
        return level;
    }

    /** Renown already earned towards the next level, and the cost of that level. */
    public long[] progress() {
        int level = 1;
        long remaining = renown;
        int max = RpgConfig.get().codex.maxLevel;
        while (level < max && remaining >= costOf(level)) {
            remaining -= costOf(level);
            level++;
        }
        return level >= max ? new long[]{1, 1} : new long[]{remaining, costOf(level)};
    }

    public int rank(Insight insight) {
        return insights.getOrDefault(insight, 0);
    }

    public int spentInsight() {
        int spent = 0;
        for (int ranks : insights.values()) {
            spent += ranks;
        }
        return spent;
    }

    public int unspentInsight() {
        return Math.max(0, level() - 1 - spentInsight());
    }

    /** Adds Renown; returns the number of levels gained. */
    public int addRenown(long amount) {
        if (amount <= 0) {
            return 0;
        }
        int before = level();
        renown += amount;
        totalRenownBanked += amount;
        dirty = true;
        return level() - before;
    }

    public boolean learn(Insight insight) {
        if (unspentInsight() <= 0 || rank(insight) >= insight.maxRanks) {
            return false;
        }
        insights.merge(insight, 1, Integer::sum);
        dirty = true;
        return true;
    }

    public void respec() {
        insights.clear();
        dirty = true;
    }

    public void credit(String entityId) {
        kills.merge(entityId, 1, Integer::sum);
        dirty = true;
    }

    public void markDirty() {
        dirty = true;
    }

    // ---- Serialization ----------------------------------------------------------------------------------

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("version", 1);
        json.addProperty("name", name);
        json.addProperty("renown", renown);
        JsonObject insightJson = new JsonObject();
        insights.forEach((insight, ranks) -> insightJson.addProperty(insight.id(), ranks));
        json.add("insights", insightJson);
        JsonObject killJson = new JsonObject();
        kills.forEach(killJson::addProperty);
        json.add("kills", killJson);
        JsonArray recollectedJson = new JsonArray();
        recollected.forEach(recollectedJson::add);
        json.add("recollected", recollectedJson);
        json.addProperty("weekly_claimed", weeklyClaimed);
        JsonObject records = new JsonObject();
        records.addProperty("best_tier", bestTier);
        records.addProperty("best_fracture", bestFracture);
        records.addProperty("deepest_floor", deepestFloor);
        records.addProperty("remembered", remembered);
        records.addProperty("forgotten", forgotten);
        records.addProperty("total_renown", totalRenownBanked);
        JsonArray relicJson = new JsonArray();
        for (int count : relics) {
            relicJson.add(count);
        }
        records.add("relics", relicJson);
        records.addProperty("fastest_hourglass", fastestHourglassSeconds);
        records.addProperty("weekly_best_week", weeklyBestWeek);
        records.addProperty("weekly_best_floor", weeklyBestFloor);
        json.add("records", records);
        JsonArray keys = new JsonArray();
        for (int[] key : pendingKeys) {
            JsonArray pair = new JsonArray();
            pair.add(key[0]);
            pair.add(key[1]);
            keys.add(pair);
        }
        json.add("pending_keys", keys);
        JsonArray relicRolls = new JsonArray();
        for (long[] roll : pendingRelics) {
            JsonArray triple = new JsonArray();
            triple.add(roll[0]);
            triple.add(roll[1]);
            triple.add(roll[2]);
            relicRolls.add(triple);
        }
        json.add("pending_relics", relicRolls);
        return json;
    }

    public static PlayerProfile fromJson(UUID id, JsonObject json) {
        PlayerProfile profile = new PlayerProfile(id);
        profile.name = Json.str(json, "name", "");
        profile.renown = Math.max(0, Json.lng(json, "renown", 0));
        JsonObject insightJson = Json.obj(json, "insights");
        for (String key : insightJson.keySet()) {
            Insight insight = Insight.byId(key);
            if (insight != null) {
                int ranks = Math.max(0, Math.min(insight.maxRanks, Json.integer(insightJson, key, 0)));
                if (ranks > 0) {
                    profile.insights.put(insight, ranks);
                }
            }
        }
        JsonObject killJson = Json.obj(json, "kills");
        for (String key : killJson.keySet()) {
            int count = Json.integer(killJson, key, 0);
            if (count > 0) {
                profile.kills.put(key, count);
            }
        }
        for (var element : Json.arr(json, "recollected")) {
            if (element.isJsonPrimitive()) {
                profile.recollected.add(element.getAsString());
            }
        }
        profile.weeklyClaimed = Json.lng(json, "weekly_claimed", Long.MIN_VALUE);
        JsonObject records = Json.obj(json, "records");
        profile.bestTier = Json.integer(records, "best_tier", 0);
        profile.bestFracture = Json.integer(records, "best_fracture", 0);
        profile.deepestFloor = Json.lng(records, "deepest_floor", 0);
        profile.remembered = Json.integer(records, "remembered", 0);
        profile.forgotten = Json.integer(records, "forgotten", 0);
        profile.totalRenownBanked = Json.lng(records, "total_renown", 0);
        JsonArray relicJson = Json.arr(records, "relics");
        for (int i = 0; i < RARITIES && i < relicJson.size(); i++) {
            profile.relics[i] = relicJson.get(i).getAsInt();
        }
        profile.fastestHourglassSeconds = Json.integer(records, "fastest_hourglass", 0);
        profile.weeklyBestWeek = Json.lng(records, "weekly_best_week", Long.MIN_VALUE);
        profile.weeklyBestFloor = Json.lng(records, "weekly_best_floor", 0);
        for (var element : Json.arr(json, "pending_keys")) {
            JsonArray pair = element.getAsJsonArray();
            profile.pendingKeys.add(new int[]{pair.get(0).getAsInt(), pair.get(1).getAsInt()});
        }
        for (var element : Json.arr(json, "pending_relics")) {
            JsonArray triple = element.getAsJsonArray();
            profile.pendingRelics.add(new long[]{triple.get(0).getAsLong(), triple.get(1).getAsLong(), triple.get(2).getAsLong()});
        }
        // Spent ranks above what the current level allows (a lowered max level) are refunded.
        if (profile.spentInsight() > profile.level() - 1) {
            profile.insights.clear();
            profile.dirty = true;
        }
        return profile;
    }
}
