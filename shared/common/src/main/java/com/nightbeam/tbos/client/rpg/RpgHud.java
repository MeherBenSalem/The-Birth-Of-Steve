package com.nightbeam.tbos.client.rpg;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nightbeam.tbos.client.GreekGui;
import com.nightbeam.tbos.client.ModKeyMappings;
import com.nightbeam.tbos.expedition.Distortion;
import com.nightbeam.tbos.rpg.Json;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * In-world overlays for 1.0: the expedition strip (tier, Distortions, the
 * Recollection you are carrying, the Hourglass), party frames, and the Renown
 * level-up banner. All of it hides with the HUD (F1).
 */
public final class RpgHud {
    private static final int STRIP_W = 138;

    private RpgHud() {
    }

    public static void render(Canvas c) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || Canvas.hudHidden()) {
            return;
        }
        if (RpgClient.expedition != null) {
            strip(c, RpgClient.expedition);
        }
        if (RpgClient.party != null) {
            frames(c, RpgClient.party, minecraft.player.getUUID().toString());
        }
        banner(c);
    }

    private static void strip(Canvas c, JsonObject data) {
        long hourglass = Json.lng(data, "hourglass", -1);
        boolean choice = Json.bool(data, "choice", false);
        int rows = 2 + (hourglass >= 0 ? 1 : 0) + (choice ? 1 : 0);
        int h = 10 + rows * 12;
        int x = c.width() - STRIP_W - 4;
        int y = 54;
        c.panel(GreekGui.HUD, x, y, STRIP_W, h);
        int line = y + 6;
        int tier = Json.integer(data, "tier", 1);
        c.icon(Icons.KEY, x + 5, line - 2, 12, 0xFFFFFFFF);
        Component tierText = Component.translatable(Json.bool(data, "weekly", false) ? "hud.tbos.expedition.weekly_tier" : "hud.tbos.expedition.tier",
                tier, Json.lng(data, "floor", 1));
        c.text(tierText, x + 20, line, RpgUi.GOLD, true);
        List<Distortion> distortions = Distortion.of(Json.integer(data, "distortions", 0));
        int dx = x + STRIP_W - 6 - distortions.size() * 11;
        for (Distortion distortion : distortions) {
            c.icon(Icons.distortion(distortion.ordinal()), dx, line - 1, 10, 0xFFFFFFFF);
            dx += 11;
        }
        line += 12;
        c.icon(Icons.RENOWN, x + 5, line - 2, 12, 0xFFFFFFFF);
        String renown = RpgUi.number(Json.lng(data, "renown", 0));
        c.text(renown, x + 20, line, RpgUi.LIGHT, true);
        String momentum = String.format(Locale.ROOT, "×%.2f", Json.dbl(data, "momentum", 1));
        c.text(momentum, x + 24 + Canvas.width(renown), line, RpgUi.LIGHT_TEAL, true);
        String relics = Integer.toString(Json.integer(data, "relics", 0));
        c.icon(Icons.RELIC, x + STRIP_W - 18 - Canvas.width(relics), line - 2, 12, 0xFFFFFFFF);
        c.text(relics, x + STRIP_W - 5 - Canvas.width(relics), line, RpgUi.LIGHT, true);
        if (hourglass >= 0) {
            line += 12;
            long seconds = hourglass / 20;
            boolean urgent = seconds < 60;
            boolean blink = urgent && (System.currentTimeMillis() / 400) % 2 == 0;
            c.icon(Icons.HOURGLASS, x + 5, line - 2, 12, blink ? 0xFFFF8060 : 0xFFFFFFFF);
            Component clock = seconds <= 0
                    ? Component.translatable("hud.tbos.expedition.hourglass_out")
                    : Component.translatable("hud.tbos.expedition.hourglass", RpgUi.clock(seconds));
            c.text(clock, x + 20, line, seconds <= 0 ? RpgUi.DIM : urgent ? 0xFFFF7A5C : RpgUi.LIGHT, true);
        }
        if (choice) {
            line += 12;
            boolean pulse = (System.currentTimeMillis() / 600) % 2 == 0;
            Component hint = Component.translatable("hud.tbos.expedition.choose", ModKeyMappings.CODEX.getTranslatedKeyMessage());
            c.text(RpgUi.fit(hint.getString(), STRIP_W - 10), x + 5, line, pulse ? RpgUi.GOLD : 0xFFC9A04A, true);
        }
    }

    private static void frames(Canvas c, JsonObject data, String self) {
        JsonArray members = Json.arr(data, "members");
        int others = 0;
        for (var element : members) {
            if (!Json.str(element.getAsJsonObject(), "id", "").equals(self)) {
                others++;
            }
        }
        if (others == 0) {
            return;
        }
        int w = 104;
        int h = 22;
        int y = c.height() / 2 - (others * (h + 3)) / 2;
        for (var element : members) {
            JsonObject member = element.getAsJsonObject();
            if (Json.str(member, "id", "").equals(self)) {
                continue;
            }
            int x = 4;
            boolean offline = Json.bool(member, "offline", false);
            boolean dead = Json.bool(member, "dead", false);
            c.panel(GreekGui.HUD, x, y, w, h);
            int nx = x + 5;
            if (Json.bool(member, "leader", false)) {
                c.icon(Icons.LEADER, nx, y + 3, 9, 0xFFFFFFFF);
                nx += 11;
            }
            String name = RpgUi.fit(Json.str(member, "name", "?"), w - (nx - x) - 16);
            c.text(name, nx, y + 4, offline || dead ? RpgUi.DIM : RpgUi.LIGHT, true);
            if (Json.bool(member, "archive", false)) {
                c.icon(Icons.ARCHIVE, x + w - 14, y + 3, 9, 0xFFFFFFFF);
            }
            int barX = x + 5;
            int barY = y + 14;
            int barW = w - 10;
            c.fill(barX, barY, barW, 4, 0xFF1E1A16);
            if (offline) {
                c.text(Component.translatable("hud.tbos.party.offline"), barX + 1, barY - 2, RpgUi.DIM, false);
            } else {
                float hp = (float) Json.dbl(member, "hp", 0);
                float max = (float) Math.max(1, Json.dbl(member, "max", 20));
                float abs = (float) Json.dbl(member, "abs", 0);
                int fill = Math.round(barW * Math.max(0, Math.min(1, hp / max)));
                int color = dead ? 0xFF4A4038 : hp / max < 0.3F ? 0xFFE04A3A : 0xFFC8423A;
                c.fill(barX, barY, fill, 4, color);
                c.fill(barX, barY, fill, 1, dead ? 0xFF5A5048 : 0xFFF07060);
                if (abs > 0) {
                    int absW = Math.round(barW * Math.min(1, abs / max));
                    c.fill(barX, barY + 3, absW, 1, 0xFFF2C94C);
                }
            }
            y += h + 3;
        }
    }

    private static void banner(Canvas c) {
        long age = System.currentTimeMillis() - RpgClient.bannerAt;
        if (RpgClient.bannerLevel <= 0 || age > 3800) {
            return;
        }
        float alpha = age < 300 ? age / 300F : age > 3200 ? (3800 - age) / 600F : 1F;
        Component title = Component.translatable("hud.tbos.codex.level_up", RpgClient.bannerLevel);
        Component sub = Component.translatable("hud.tbos.codex.level_up.sub", ModKeyMappings.CODEX.getTranslatedKeyMessage());
        int w = Math.max(Canvas.width(title) * 2, Canvas.width(sub)) + 60;
        int x = (c.width() - w) / 2;
        int y = 36;
        c.gradient(x, y, w, 40, RpgUi.alpha(0xB0000000, alpha), RpgUi.alpha(0x50000000, alpha));
        c.fill(x + 20, y, w - 40, 1, RpgUi.alpha(0xFFE6B85C, alpha));
        c.fill(x + 20, y + 39, w - 40, 1, RpgUi.alpha(0xFFE6B85C, alpha));
        c.icon(Icons.RENOWN, x + 8, y + 6, 28, RpgUi.alpha(0xFFFFFFFF, alpha));
        c.icon(Icons.RENOWN, x + w - 36, y + 6, 28, RpgUi.alpha(0xFFFFFFFF, alpha));
        c.push();
        c.translate(c.width() / 2.0F, y + 7);
        c.scale(2.0F);
        c.text(title, -Canvas.width(title) / 2, 0, RpgUi.alpha(0xFFE6B85C, alpha), true);
        c.pop();
        c.text(sub, (c.width() - Canvas.width(sub)) / 2, y + 27, RpgUi.alpha(0xFFE8DFCF, alpha), true);
    }
}
