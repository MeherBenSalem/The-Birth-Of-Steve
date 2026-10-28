package com.nightbeam.tbos.codex;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nightbeam.tbos.rpg.Json;
import com.nightbeam.tbos.rpg.JsonStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/**
 * The server's leaderboards, kept in {@code data/tbos/records.json}: the ten best
 * expeditions ever Remembered (highest tier, then deepest floor, then Fracture),
 * and the ten deepest runs of the current Weekly Recollection.
 */
public final class HallOfRecords {
    public record Entry(UUID player, String name, int tier, int fracture, long floor, long seconds) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("player", player.toString());
            json.addProperty("name", name);
            json.addProperty("tier", tier);
            json.addProperty("fracture", fracture);
            json.addProperty("floor", floor);
            json.addProperty("seconds", seconds);
            return json;
        }

        static Entry fromJson(JsonObject json) {
            return new Entry(UUID.fromString(Json.str(json, "player", "")), Json.str(json, "name", "?"),
                    Json.integer(json, "tier", 0), Json.integer(json, "fracture", 0),
                    Json.lng(json, "floor", 0), Json.lng(json, "seconds", 0));
        }
    }

    public static final int SIZE = 10;
    private static final Comparator<Entry> ALL_TIME = Comparator.comparingInt(Entry::tier).reversed()
            .thenComparing(Comparator.comparingLong(Entry::floor).reversed())
            .thenComparing(Comparator.comparingInt(Entry::fracture).reversed());
    private static final Comparator<Entry> WEEKLY = Comparator.comparingLong(Entry::floor).reversed()
            .thenComparingLong(Entry::seconds);

    private static final List<Entry> ALL = new ArrayList<>();
    private static final List<Entry> WEEK = new ArrayList<>();
    private static long week = Long.MIN_VALUE;

    private HallOfRecords() {
    }

    public static List<Entry> allTime() {
        return List.copyOf(ALL);
    }

    public static List<Entry> weekly() {
        if (week != Weekly.currentWeek()) {
            return List.of();
        }
        return List.copyOf(WEEK);
    }

    public static void submit(MinecraftServer server, Entry entry) {
        upsert(ALL, entry, ALL_TIME);
        save(server);
    }

    public static void submitWeekly(MinecraftServer server, long entryWeek, Entry entry) {
        if (entryWeek != Weekly.currentWeek()) {
            return;
        }
        if (week != entryWeek) {
            week = entryWeek;
            WEEK.clear();
        }
        upsert(WEEK, entry, WEEKLY);
        save(server);
    }

    /** Keeps each player's best entry only. */
    private static void upsert(List<Entry> list, Entry entry, Comparator<Entry> order) {
        Entry previous = null;
        for (Entry existing : list) {
            if (existing.player.equals(entry.player)) {
                previous = existing;
            }
        }
        if (previous != null) {
            if (order.compare(entry, previous) >= 0) {
                return;
            }
            list.remove(previous);
        }
        list.add(entry);
        list.sort(order);
        while (list.size() > SIZE) {
            list.remove(list.size() - 1);
        }
    }

    public static void load(MinecraftServer server) {
        ALL.clear();
        WEEK.clear();
        JsonObject json = JsonStore.read(JsonStore.directory(server, "records.json"));
        read(Json.arr(json, "all_time"), ALL);
        week = Json.lng(json, "week", Long.MIN_VALUE);
        read(Json.arr(json, "weekly"), WEEK);
        ALL.sort(ALL_TIME);
        WEEK.sort(WEEKLY);
    }

    private static void read(JsonArray array, List<Entry> into) {
        for (var element : array) {
            try {
                into.add(Entry.fromJson(element.getAsJsonObject()));
            } catch (RuntimeException ignored) {
                // Skip entries that no longer parse.
            }
        }
    }

    private static void save(MinecraftServer server) {
        JsonObject json = new JsonObject();
        JsonArray all = new JsonArray();
        ALL.forEach(entry -> all.add(entry.toJson()));
        JsonArray weekly = new JsonArray();
        WEEK.forEach(entry -> weekly.add(entry.toJson()));
        json.add("all_time", all);
        json.addProperty("week", week);
        json.add("weekly", weekly);
        JsonStore.write(JsonStore.directory(server, "records.json"), json);
    }

    public static void clear() {
        ALL.clear();
        WEEK.clear();
        week = Long.MIN_VALUE;
    }
}
