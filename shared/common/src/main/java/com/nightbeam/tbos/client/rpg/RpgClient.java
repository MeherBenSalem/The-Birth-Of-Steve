package com.nightbeam.tbos.client.rpg;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightbeam.tbos.client.ModKeyMappings;
import com.nightbeam.tbos.network.payload.RpgActionPayload;
import com.nightbeam.tbos.platform.Services;
import com.nightbeam.tbos.rpg.Json;
import com.nightbeam.tbos.rpg.RpgSounds;
import net.minecraft.client.Minecraft;

/**
 * Client half of the 1.0 JSON channel: keeps the latest snapshot of each system
 * for the HUD and screens, opens screens the server asks for, and sends requests.
 */
public final class RpgClient {
    public static JsonObject codex;
    public static JsonObject expedition;
    public static JsonObject party;
    public static JsonObject attune;
    public static JsonObject choice;
    /** Renown level-up banner: the new level and when it was shown. */
    public static int bannerLevel;
    public static long bannerAt;
    private static long lastHourglassSecond = -1;
    private static int lastLevel = -1;

    private RpgClient() {
    }

    /** Second Draft is learned and unspent on this floor. */
    public static boolean rerollAvailable() {
        return expedition != null && Json.bool(expedition, "reroll", false);
    }

    public static void send(String action, JsonObject body) {
        body.addProperty("a", action);
        Services.NETWORK.sendToServer(new RpgActionPayload(body.toString()));
    }

    public static void send(String action) {
        send(action, new JsonObject());
    }

    /** Client thread. */
    public static void accept(String json) {
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
        switch (Json.str(body, "t", "")) {
            case "codex" -> {
                int level = Json.integer(body, "level", 1);
                if (lastLevel > 0 && level > lastLevel) {
                    bannerLevel = level;
                    bannerAt = System.currentTimeMillis();
                    Canvas.playUi(RpgSounds.CODEX_RENOWN.get(), 1.0F, 1.0F);
                }
                lastLevel = level;
                codex = body;
            }
            case "open_codex" -> Canvas.open(new CodexScreen(Json.integer(body, "tab", 0)));
            case "exp" -> {
                expedition = Json.bool(body, "none", false) ? null : body;
                if (Canvas.current() instanceof com.nightbeam.tbos.client.MemoryScreen screen) {
                    screen.refresh();
                }
            }
            case "party" -> party = Json.arr(body, "members").isEmpty() ? null : body;
            case "attune" -> {
                attune = body;
                if (Json.bool(body, "open", false) || !(Canvas.current() instanceof AttunementScreen)) {
                    Canvas.open(new AttunementScreen());
                }
            }
            case "attune_done" -> {
                attune = null;
                if (Canvas.current() instanceof AttunementScreen) {
                    Canvas.open(null);
                }
            }
            case "choice" -> {
                choice = body;
                Canvas.open(new RecollectionScreen(RecollectionScreen.Mode.CHOICE, body));
            }
            case "banked" -> {
                choice = null;
                Canvas.open(new RecollectionScreen(RecollectionScreen.Mode.BANKED, body));
            }
            case "forgotten" -> {
                choice = null;
                Canvas.open(new RecollectionScreen(RecollectionScreen.Mode.FORGOTTEN, body));
            }
            default -> {
            }
        }
    }

    /** End of every client tick. */
    public static void tick(Minecraft minecraft) {
        while (ModKeyMappings.CODEX.consumeClick()) {
            if (expedition != null && Json.bool(expedition, "choice", false)) {
                send("choice");
            } else {
                JsonObject body = new JsonObject();
                body.addProperty("tab", 0);
                send("codex_open", body);
            }
        }
        if (minecraft.player == null) {
            codex = null;
            expedition = null;
            party = null;
            attune = null;
            choice = null;
            lastLevel = -1;
            return;
        }
        // The Hourglass ticks audibly through its last minute.
        if (expedition != null) {
            long ticks = Json.lng(expedition, "hourglass", -1);
            long second = ticks < 0 ? -1 : ticks / 20;
            if (second != lastHourglassSecond) {
                if (second == 0 && lastHourglassSecond > 0) {
                    Canvas.playUi(RpgSounds.HOURGLASS_END.get(), 1.0F, 0.9F);
                } else if (second > 0 && (second <= 10 || second == 30 || second == 60)) {
                    Canvas.playUi(RpgSounds.HOURGLASS_TICK.get(), second <= 10 ? 1.15F : 1.0F, 0.7F);
                }
                lastHourglassSecond = second;
            }
        } else {
            lastHourglassSecond = -1;
        }
    }
}
