package com.nightbeam.tbos.client.rpg;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nightbeam.tbos.client.GreekGui;
import com.nightbeam.tbos.expedition.Distortion;
import com.nightbeam.tbos.expedition.Recollections;
import com.nightbeam.tbos.relic.Rarity;
import com.nightbeam.tbos.rpg.Json;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;

/**
 * Attuning a Memory Key before an expedition: tier, the eight Distortions to toggle,
 * what the run will pay, who is coming, and the Begin button. Nothing is spent
 * until Begin; the server checks everything again then.
 */
public final class AttunementScreen extends RpgScreen {
    private static final int W = 324;
    private static final int H = 226;
    private final RpgUi ui = new RpgUi();
    private long opened = System.currentTimeMillis();

    public AttunementScreen() {
        super(Component.translatable("screen.tbos.attune.title"));
    }

    @Override
    protected void draw(Canvas c, int mx, int my, float partialTick) {
        ui.begin();
        JsonObject data = RpgClient.attune;
        if (data == null) {
            return;
        }
        float appear = Math.min(1, (System.currentTimeMillis() - opened) / 180.0F);
        int x = (c.width() - W) / 2;
        int y = (c.height() - H) / 2 + Math.round((1 - appear) * 8);
        c.fill(0, 0, c.width(), c.height(), RpgUi.alpha(0x99000000, appear));
        c.panel(GreekGui.FRAME, x, y, W, H);

        int tier = Json.integer(data, "tier", 1);
        int mask = Json.integer(data, "distortions", 0);
        boolean weekly = Json.bool(data, "weekly", false);
        int heat = Distortion.heat(mask);

        c.icon(Icons.KEY, x + 12, y + 9, 22, 0xFFFFFFFF);
        Component title = weekly
                ? Component.translatable("item.tbos.memory_key.weekly_name", tier)
                : Component.translatable("item.tbos.memory_key.tiered_name", tier);
        c.text(title, x + 38, y + 11, RpgUi.INK, false);
        c.text(placeLine(data), x + 38, y + 22, RpgUi.MUTED, false);
        RpgUi.rule(c, x + 12, y + 36, W - 24);

        // Distortions
        c.text(Component.translatable("screen.tbos.attune.distortions"), x + 14, y + 42, RpgUi.BRONZE, false);
        Distortion[] all = Distortion.values();
        List<Component> hover = null;
        for (int i = 0; i < all.length; i++) {
            Distortion distortion = all[i];
            int cx = x + 12 + (i % 2) * 90;
            int cy = y + 54 + (i / 2) * 31;
            boolean on = distortion.in(mask);
            boolean over = RpgUi.over(mx, my, cx, cy, 86, 28);
            c.panel(on ? GreekGui.SELECTED : over && !weekly ? GreekGui.HOVER : GreekGui.CARD, cx, cy, 86, 28);
            c.icon(Icons.distortion(i), cx + 4, cy + 4, 20, on ? 0xFFFFFFFF : 0xB0FFFFFF);
            c.text(RpgUi.fit(Component.translatable(distortion.key()).getString(), 56), cx + 27, cy + 6, RpgUi.INK, false);
            c.text(RpgUi.fit(Component.translatable(on ? "screen.tbos.attune.active" : "screen.tbos.attune.heat").getString(), 56),
                    cx + 27, cy + 16, on ? RpgUi.EMBER : RpgUi.MUTED, false);
            final int ordinal = i;
            ui.area(cx, cy, 86, 28, !weekly, () -> {
                JsonObject body = new JsonObject();
                body.addProperty("d", ordinal);
                RpgClient.send("attune_toggle", body);
            });
            if (over) {
                hover = List.of(Component.translatable(distortion.key()).withStyle(s -> s.withColor(0xE6B85C)),
                        Component.translatable(distortion.key() + ".description"),
                        Component.translatable(weekly ? "screen.tbos.attune.sealed" : "screen.tbos.attune.toggle_hint")
                                .withStyle(s -> s.withColor(0xA89B86)));
            }
        }

        // Summary
        int sx = x + 196;
        int sy = y + 42;
        int sw = W - 196 - 12;
        c.panel(GreekGui.WELL, sx, sy, sw, 150);
        int line = sy + 7;
        line = stat(c, sx + 7, line, sw - 14, Component.translatable("screen.tbos.attune.tier"), Integer.toString(tier), RpgUi.INK);
        line = stat(c, sx + 7, line, sw - 14, Component.translatable("screen.tbos.attune.heat_total"), Integer.toString(heat),
                heat > 0 ? RpgUi.EMBER : RpgUi.INK);
        line = stat(c, sx + 7, line, sw - 14, Component.translatable("screen.tbos.attune.fracture"), Integer.toString(tier + heat), RpgUi.TEAL);
        line = stat(c, sx + 7, line, sw - 14, Component.translatable("screen.tbos.attune.rewards"),
                String.format(Locale.ROOT, "×%.2f", Json.dbl(data, "factor", 1)), RpgUi.BRONZE);
        line = stat(c, sx + 7, line, sw - 14, Component.translatable("screen.tbos.attune.boss_renown"),
                RpgUi.number(Json.lng(data, "boss_renown", 0)), RpgUi.BRONZE);
        if (Distortion.HOURGLASS.in(mask)) {
            line = stat(c, sx + 7, line, sw - 14, Component.translatable("screen.tbos.attune.hourglass"),
                    RpgUi.clock(Json.integer(data, "hourglass", 480)), RpgUi.EMBER);
        }
        line += 3;
        c.text(Component.translatable("screen.tbos.attune.relic_odds"), sx + 7, line, RpgUi.MUTED, false);
        line += 11;
        JsonArray odds = Json.arr(data, "odds");
        for (int r = 0; r < odds.size() && r < Rarity.values().length; r++) {
            double percent = odds.get(r).getAsDouble();
            if (percent <= 0) {
                continue;
            }
            Rarity rarity = Rarity.values()[r];
            // Common's pale grey would vanish into the parchment, so its bar takes the muted ink.
            int fill = rarity == Rarity.COMMON ? RpgUi.MUTED : RpgUi.opaque(rarity.color);
            int barX = sx + 19;
            int barW = sw - 12 - 40;
            c.icon(Icons.rarity(r), sx + 7, line, 9, 0xFFFFFFFF);
            c.fill(barX, line + 3, barW, 4, 0x33302B24);
            c.fill(barX, line + 3, Math.max(1, (int) Math.round(barW * Math.min(1, percent / 60.0))), 4, fill);
            String label = percent < 1 ? String.format(Locale.ROOT, "%.1f%%", percent) : Math.round(percent) + "%";
            c.text(label, sx + sw - 7 - Canvas.width(label), line + 1, RpgUi.INK, false);
            line += 11;
        }

        // Party
        int py = y + 178;
        c.text(Component.translatable("screen.tbos.attune.party"), x + 14, py, RpgUi.BRONZE, false);
        JsonArray party = Json.arr(data, "party");
        int px = x + 14 + Canvas.width(Component.translatable("screen.tbos.attune.party")) + 6;
        for (var element : party) {
            JsonObject member = element.getAsJsonObject();
            String name = Json.str(member, "name", "?");
            boolean busy = Json.bool(member, "busy", false);
            c.text(name, px, py, busy ? RpgUi.EMBER : RpgUi.TEAL, false);
            px += Canvas.width(name) + 8;
        }
        if (party.size() <= 1) {
            c.text(Component.translatable("screen.tbos.attune.solo"), px, py, RpgUi.MUTED, false);
        }

        ui.button(c, x + 12, y + H - 28, 80, 18, Component.translatable("screen.tbos.cancel"), mx, my, true, false, this::onClose);
        ui.button(c, x + W - 12 - 132, y + H - 28, 132, 18, Component.translatable("screen.tbos.attune.begin"), mx, my, true, true,
                () -> RpgClient.send("attune_begin"));
        if (hover != null) {
            c.tooltip(hover, mx, my);
        }
    }

    private static Component placeLine(JsonObject data) {
        String place = Json.str(data, "place", "waystone");
        if (Json.bool(data, "weekly", false)) {
            return Component.translatable("screen.tbos.attune.place.weekly", Json.lng(data, "week", 0));
        }
        return switch (place) {
            case "recollection" -> Component.translatable("screen.tbos.attune.place.recollection",
                    Recollections.name(Json.str(data, "structure", "")));
            case "gateway" -> Component.translatable("screen.tbos.attune.place.gateway");
            default -> Component.translatable("screen.tbos.attune.place.waystone");
        };
    }

    private static int stat(Canvas c, int x, int y, int w, Component label, String value, int color) {
        c.text(label, x, y, RpgUi.MUTED, false);
        c.text(value, x + w - Canvas.width(value), y, color, false);
        return y + 11;
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        return button == 0 && ui.click(mx, my);
    }

    @Override
    protected boolean onKey(int key) {
        if (key == KEY_ENTER) {
            RpgClient.send("attune_begin");
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        RpgClient.send("attune_close");
        RpgClient.attune = null;
        super.onClose();
    }
}
