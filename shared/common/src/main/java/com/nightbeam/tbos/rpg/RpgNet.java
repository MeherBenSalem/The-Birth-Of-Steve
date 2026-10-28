package com.nightbeam.tbos.rpg;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.network.payload.RpgSyncPayload;
import com.nightbeam.tbos.platform.Services;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerPlayer;

/**
 * One JSON channel each way for every 1.0 system. A message carries its type in
 * {@code "t"} (server to client) or {@code "a"} (client to server). Keeping the
 * transport generic means only the two payload records are version-specific; the
 * systems themselves never touch networking types.
 */
public final class RpgNet {
    private static final Map<String, BiConsumer<ServerPlayer, JsonObject>> ACTIONS = new HashMap<>();
    private static final Map<UUID, long[]> BUDGET = new HashMap<>();
    /** Requests a player may make per second; enough for any screen, too few to flood the server. */
    private static final int REQUESTS_PER_SECOND = 12;

    private RpgNet() {
    }

    public static void on(String action, BiConsumer<ServerPlayer, JsonObject> handler) {
        ACTIONS.put(action, handler);
    }

    public static void send(ServerPlayer player, String type, JsonObject body) {
        body.addProperty("t", type);
        String json = body.toString();
        if (json.length() > RpgSyncPayload.MAX_LENGTH) {
            Yesterglass.LOGGER.warn("Dropped oversized {} message for {}", type, player.getUUID());
            return;
        }
        Services.NETWORK.sendToPlayer(player, new RpgSyncPayload(json));
    }

    /** Server thread. Unknown, malformed or over-budget requests are ignored. */
    public static void handleAction(ServerPlayer player, String json) {
        long second = player.level().getGameTime() / 20L;
        long[] budget = BUDGET.computeIfAbsent(player.getUUID(), id -> new long[]{second, 0});
        if (budget[0] != second) {
            budget[0] = second;
            budget[1] = 0;
        }
        if (++budget[1] > REQUESTS_PER_SECOND) {
            return;
        }
        JsonObject body;
        try {
            JsonElement element = JsonParser.parseString(json);
            if (!element.isJsonObject()) {
                return;
            }
            body = element.getAsJsonObject();
        } catch (RuntimeException exception) {
            return;
        }
        String action = Json.str(body, "a", "");
        BiConsumer<ServerPlayer, JsonObject> handler = ACTIONS.get(action);
        if (handler == null) {
            return;
        }
        try {
            handler.accept(player, body);
        } catch (RuntimeException exception) {
            Yesterglass.LOGGER.error("RPG action '{}' from {} failed", action, player.getUUID(), exception);
        }
    }

    public static void forget(UUID player) {
        BUDGET.remove(player);
    }
}
