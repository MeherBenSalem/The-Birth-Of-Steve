package com.nightbeam.tbos.rpg;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Null-safe readers for hand-written JSON, so a missing or mistyped field falls back instead of throwing. */
public final class Json {
    private Json() {
    }

    public static String str(JsonObject json, String key, String fallback) {
        JsonElement value = json == null ? null : json.get(key);
        return value instanceof JsonPrimitive primitive && primitive.isString() ? primitive.getAsString() : fallback;
    }

    public static int integer(JsonObject json, String key, int fallback) {
        JsonElement value = json == null ? null : json.get(key);
        try {
            return value instanceof JsonPrimitive primitive && primitive.isNumber() ? primitive.getAsInt() : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static long lng(JsonObject json, String key, long fallback) {
        JsonElement value = json == null ? null : json.get(key);
        try {
            return value instanceof JsonPrimitive primitive && primitive.isNumber() ? primitive.getAsLong() : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static double dbl(JsonObject json, String key, double fallback) {
        JsonElement value = json == null ? null : json.get(key);
        try {
            double read = value instanceof JsonPrimitive primitive && primitive.isNumber() ? primitive.getAsDouble() : fallback;
            return Double.isFinite(read) ? read : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public static boolean bool(JsonObject json, String key, boolean fallback) {
        JsonElement value = json == null ? null : json.get(key);
        return value instanceof JsonPrimitive primitive && primitive.isBoolean() ? primitive.getAsBoolean() : fallback;
    }

    public static JsonObject obj(JsonObject json, String key) {
        JsonElement value = json == null ? null : json.get(key);
        return value instanceof JsonObject object ? object : new JsonObject();
    }

    public static JsonArray arr(JsonObject json, String key) {
        JsonElement value = json == null ? null : json.get(key);
        return value instanceof JsonArray array ? array : new JsonArray();
    }
}
