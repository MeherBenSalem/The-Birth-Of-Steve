package com.nightbeam.tbos.expedition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nightbeam.tbos.relic.Rarity;
import com.nightbeam.tbos.rpg.Json;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Everything an expedition adds on top of an Archive run: its key, the
 * Recollection each member is carrying, Momentum, the Hourglass and the
 * Remember-or-Forget vote. Kept beside the run (keyed by run id) rather than inside
 * it, so the run's own save format and its per-version copies stay untouched.
 */
public final class ExpeditionState {
    /** A Relic earned but not yet banked: rolled from {@code seed} when Remembered. */
    public record RelicRoll(long seed, int tier, int minRarity) {
    }

    public static final class Pool {
        public double renown;
        public double floorRenown;
        public final List<RelicRoll> relics = new ArrayList<>();
    }

    public final UUID run;
    public ExpeditionKey key;
    public UUID holder;
    /** Empty for the Archive itself, otherwise the recollected structure's id. */
    public String place = "";
    public long floor;
    public double momentum = 1.0;
    public long floorStartTick = -1;
    public long hourglassBonusTicks;
    public int hourglassWins;
    public int floorsCleared;
    public boolean bossCredited;
    public boolean choiceOpen;
    public boolean finalized;
    public boolean steadyUsed;
    public int lastRevives = -1;
    public long lastHaunt;
    public String lastStatus = "";
    public final Set<Integer> credited = new HashSet<>();
    public final Set<UUID> forgetVotes = new HashSet<>();
    public final Set<UUID> remembered = new HashSet<>();
    public final Set<UUID> lastStandUsed = new HashSet<>();
    public final Set<UUID> rerollUsed = new HashSet<>();
    public final Map<UUID, Pool> pools = new LinkedHashMap<>();
    public final Map<UUID, String> names = new HashMap<>();

    public ExpeditionState(UUID run, ExpeditionKey key, UUID holder) {
        this.run = run;
        this.key = key;
        this.holder = holder;
    }

    public Pool pool(UUID player) {
        return pools.computeIfAbsent(player, id -> new Pool());
    }

    public int fracture() {
        return key.fracture();
    }

    /** Hourglass ticks remaining on this floor, or -1 when the floor has no Hourglass. */
    public long hourglassRemaining(long now, int masteryRanks, int baseSeconds) {
        if (!Distortion.HOURGLASS.in(key.distortions()) || floorStartTick < 0) {
            return -1;
        }
        long limit = (baseSeconds + 60L * masteryRanks) * 20L + hourglassBonusTicks;
        return Math.max(0, limit - (now - floorStartTick));
    }

    public void beginFloor(long newFloor) {
        floor = newFloor;
        floorStartTick = -1;
        hourglassBonusTicks = 0;
        bossCredited = false;
        choiceOpen = false;
        credited.clear();
        forgetVotes.clear();
        lastStandUsed.clear();
        rerollUsed.clear();
        pools.values().forEach(pool -> pool.floorRenown = 0);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("run", run.toString());
        json.addProperty("tier", key.tier());
        json.addProperty("distortions", key.distortions());
        json.addProperty("week", key.week());
        json.addProperty("seed", key.seed());
        json.addProperty("holder", holder == null ? "" : holder.toString());
        json.addProperty("place", place);
        json.addProperty("floor", floor);
        json.addProperty("momentum", momentum);
        json.addProperty("floor_start", floorStartTick);
        json.addProperty("hourglass_bonus", hourglassBonusTicks);
        json.addProperty("hourglass_wins", hourglassWins);
        json.addProperty("floors_cleared", floorsCleared);
        json.addProperty("boss_credited", bossCredited);
        json.addProperty("choice_open", choiceOpen);
        json.addProperty("finalized", finalized);
        json.addProperty("steady_used", steadyUsed);
        json.addProperty("last_revives", lastRevives);
        json.addProperty("last_status", lastStatus);
        json.add("credited", ints(credited));
        json.add("forget", uuids(forgetVotes));
        json.add("remembered", uuids(remembered));
        json.add("last_stand", uuids(lastStandUsed));
        json.add("reroll", uuids(rerollUsed));
        JsonObject poolJson = new JsonObject();
        pools.forEach((player, pool) -> {
            JsonObject entry = new JsonObject();
            entry.addProperty("renown", pool.renown);
            entry.addProperty("floor_renown", pool.floorRenown);
            entry.addProperty("name", names.getOrDefault(player, ""));
            JsonArray relics = new JsonArray();
            for (RelicRoll roll : pool.relics) {
                JsonArray triple = new JsonArray();
                triple.add(roll.seed());
                triple.add(roll.tier());
                triple.add(roll.minRarity());
                relics.add(triple);
            }
            entry.add("relics", relics);
            poolJson.add(player.toString(), entry);
        });
        json.add("pools", poolJson);
        return json;
    }

    public static ExpeditionState fromJson(JsonObject json) {
        UUID run = UUID.fromString(Json.str(json, "run", ""));
        ExpeditionKey key = new ExpeditionKey(Json.integer(json, "tier", 1), Json.integer(json, "distortions", 0),
                Json.lng(json, "week", ExpeditionKey.NOT_WEEKLY), Json.lng(json, "seed", 0L));
        String holder = Json.str(json, "holder", "");
        ExpeditionState state = new ExpeditionState(run, key, holder.isEmpty() ? null : UUID.fromString(holder));
        state.place = Json.str(json, "place", "");
        state.floor = Json.lng(json, "floor", 0);
        state.momentum = Math.max(1.0, Json.dbl(json, "momentum", 1.0));
        state.floorStartTick = Json.lng(json, "floor_start", -1);
        state.hourglassBonusTicks = Json.lng(json, "hourglass_bonus", 0);
        state.hourglassWins = Json.integer(json, "hourglass_wins", 0);
        state.floorsCleared = Json.integer(json, "floors_cleared", 0);
        state.bossCredited = Json.bool(json, "boss_credited", false);
        state.choiceOpen = Json.bool(json, "choice_open", false);
        state.finalized = Json.bool(json, "finalized", false);
        state.steadyUsed = Json.bool(json, "steady_used", false);
        state.lastRevives = Json.integer(json, "last_revives", -1);
        state.lastStatus = Json.str(json, "last_status", "");
        for (var element : Json.arr(json, "credited")) {
            state.credited.add(element.getAsInt());
        }
        readUuids(Json.arr(json, "forget"), state.forgetVotes);
        readUuids(Json.arr(json, "remembered"), state.remembered);
        readUuids(Json.arr(json, "last_stand"), state.lastStandUsed);
        readUuids(Json.arr(json, "reroll"), state.rerollUsed);
        JsonObject poolJson = Json.obj(json, "pools");
        for (String id : poolJson.keySet()) {
            UUID player = UUID.fromString(id);
            JsonObject entry = Json.obj(poolJson, id);
            Pool pool = state.pool(player);
            pool.renown = Json.dbl(entry, "renown", 0);
            pool.floorRenown = Json.dbl(entry, "floor_renown", 0);
            state.names.put(player, Json.str(entry, "name", ""));
            for (var element : Json.arr(entry, "relics")) {
                JsonArray triple = element.getAsJsonArray();
                pool.relics.add(new RelicRoll(triple.get(0).getAsLong(), triple.get(1).getAsInt(),
                        Math.min(Rarity.REMNANT.ordinal(), triple.get(2).getAsInt())));
            }
        }
        return state;
    }

    private static JsonArray ints(Set<Integer> values) {
        JsonArray array = new JsonArray();
        values.forEach(array::add);
        return array;
    }

    private static JsonArray uuids(Set<UUID> values) {
        JsonArray array = new JsonArray();
        values.forEach(value -> array.add(value.toString()));
        return array;
    }

    private static void readUuids(JsonArray array, Set<UUID> into) {
        for (var element : array) {
            try {
                into.add(UUID.fromString(element.getAsString()));
            } catch (RuntimeException ignored) {
                // Skip malformed ids.
            }
        }
    }
}
