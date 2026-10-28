package com.nightbeam.tbos.client.rpg;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nightbeam.tbos.client.GreekGui;
import com.nightbeam.tbos.codex.Bestiary;
import com.nightbeam.tbos.codex.Insight;
import com.nightbeam.tbos.expedition.Distortion;
import com.nightbeam.tbos.relic.Rarity;
import com.nightbeam.tbos.rpg.Json;
import com.nightbeam.tbos.rpg.RpgSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

/**
 * The Archive Codex: permanent progress across every expedition. Four tabs:
 * Insights (perks bought with Renown levels), the Bestiary, personal and server
 * records, and the Weekly Recollection.
 */
public final class CodexScreen extends RpgScreen {
    private static final int W = 368;
    private static final int H = 244;
    private static final String[] TABS = {"insights", "bestiary", "records", "weekly"};
    private static final int[] TAB_ICONS = {Icons.INSIGHT, Icons.BESTIARY, Icons.RECORDS, Icons.WEEKLY};
    private final RpgUi ui = new RpgUi();
    private int tab;
    private int selected;
    private int scroll;
    private final long opened = System.currentTimeMillis();

    public CodexScreen(int tab) {
        super(Component.translatable("screen.tbos.codex.title"));
        this.tab = Math.max(0, Math.min(TABS.length - 1, tab));
        Canvas.playUi(RpgSounds.CODEX_OPEN.get(), 1.0F, 0.8F);
    }

    @Override
    protected void draw(Canvas c, int mx, int my, float partialTick) {
        ui.begin();
        JsonObject data = RpgClient.codex;
        float appear = Math.min(1, (System.currentTimeMillis() - opened) / 200.0F);
        int x = (c.width() - W) / 2;
        int y = (c.height() - H) / 2 + Math.round((1 - appear) * 6);
        c.fill(0, 0, c.width(), c.height(), RpgUi.alpha(0x90000000, appear));
        c.panel(GreekGui.FRAME, x, y, W, H);
        if (data == null) {
            Component wait = Component.translatable("screen.tbos.codex.loading");
            c.text(wait, x + (W - Canvas.width(wait)) / 2, y + H / 2, RpgUi.MUTED, false);
            return;
        }
        drawHeader(c, x, y, data);
        for (int i = 0; i < TABS.length; i++) {
            int tx = x + 12 + i * 87;
            int ty = y + 34;
            boolean active = i == tab;
            boolean over = RpgUi.over(mx, my, tx, ty, 84, 18);
            c.panel(active ? GreekGui.TAB_ACTIVE : over ? GreekGui.HOVER : GreekGui.TAB_IDLE, tx, ty, 84, 18);
            c.icon(TAB_ICONS[i], tx + 4, ty + 1, 16, 0xFFFFFFFF);
            c.text(RpgUi.fit(Component.translatable("screen.tbos.codex.tab." + TABS[i]).getString(), 60), tx + 22, ty + 5,
                    active ? RpgUi.INK : RpgUi.MUTED, false);
            final int target = i;
            ui.area(tx, ty, 84, 18, !active, () -> {
                tab = target;
                scroll = 0;
                Canvas.playUi(RpgSounds.CODEX_PAGE.get(), 1.0F, 0.7F);
            });
        }
        int cx = x + 12;
        int cy = y + 58;
        int cw = W - 24;
        int ch = H - 58 - 12;
        List<Component> hover = switch (tab) {
            case 0 -> drawInsights(c, data, cx, cy, cw, ch, mx, my);
            case 1 -> drawBestiary(c, data, cx, cy, cw, ch, mx, my);
            case 2 -> drawRecords(c, data, cx, cy, cw, ch);
            default -> drawWeekly(c, data, cx, cy, cw, ch, mx, my);
        };
        if (hover != null) {
            c.tooltip(hover, mx, my);
        }
    }

    private void drawHeader(Canvas c, int x, int y, JsonObject data) {
        c.icon(Icons.RENOWN, x + 12, y + 8, 22, 0xFFFFFFFF);
        c.text(Component.translatable("screen.tbos.codex.title"), x + 38, y + 10, RpgUi.INK, false);
        c.text(Component.translatable("screen.tbos.codex.subtitle"), x + 38, y + 21, RpgUi.MUTED, false);
        int level = Json.integer(data, "level", 1);
        int max = Json.integer(data, "max_level", 30);
        long progress = Json.lng(data, "progress", 0);
        long needed = Math.max(1, Json.lng(data, "needed", 1));
        Component levelText = Component.translatable("screen.tbos.codex.level", level, max);
        int rx = x + W - 12;
        c.text(levelText, rx - Canvas.width(levelText), y + 9, RpgUi.BRONZE, false);
        boolean capped = level >= max;
        c.progress(rx - 110, y + 21, 110, 4, capped ? 1F : (float) progress / needed, capped);
        String renown = capped ? Component.translatable("screen.tbos.codex.max").getString()
                : RpgUi.number(progress) + " / " + RpgUi.number(needed);
        c.text(renown, rx - 110 - 4 - Canvas.width(renown), y + 19, RpgUi.MUTED, false);
    }

    // ---- Insights ---------------------------------------------------------------------------------------

    private List<Component> drawInsights(Canvas c, JsonObject data, int x, int y, int w, int h, int mx, int my) {
        int unspent = Json.integer(data, "insight", 0);
        JsonObject ranks = Json.obj(data, "insights");
        Component points = Component.translatable("screen.tbos.codex.unspent", unspent);
        c.text(points, x + 2, y + 1, unspent > 0 ? RpgUi.TEAL : RpgUi.MUTED, false);
        if (Json.bool(data, "respec", true)) {
            ui.button(c, x + w - 70, y - 3, 70, 14, Component.translatable("screen.tbos.codex.respec"), mx, my, !ranks.keySet().isEmpty(), false,
                    () -> RpgClient.send("respec"));
        }
        List<Component> hover = null;
        Insight[] all = Insight.values();
        int cols = 4;
        int cardW = (w - (cols - 1) * 4) / cols;
        int cardH = 40;
        for (int i = 0; i < all.length; i++) {
            Insight insight = all[i];
            int cx = x + (i % cols) * (cardW + 4);
            int cy = y + 14 + (i / cols) * (cardH + 4);
            int rank = Json.integer(ranks, insight.id(), 0);
            boolean maxed = rank >= insight.maxRanks;
            boolean learnable = !maxed && unspent > 0;
            boolean over = RpgUi.over(mx, my, cx, cy, cardW, cardH);
            int tile = maxed ? GreekGui.COMPLETE : rank > 0 ? GreekGui.SELECTED : over && learnable ? GreekGui.HOVER : learnable ? GreekGui.CARD : GreekGui.LOCKED;
            c.panel(tile, cx, cy, cardW, cardH);
            c.icon(Icons.insight(i), cx + 4, cy + 4, 22, rank > 0 || learnable ? 0xFFFFFFFF : 0x90FFFFFF);
            boolean dark = tile == GreekGui.SELECTED;
            c.text(RpgUi.fit(Component.translatable(insight.key()).getString(), cardW - 32), cx + 29, cy + 6,
                    dark ? RpgUi.LIGHT : RpgUi.INK, dark);
            for (int r = 0; r < insight.maxRanks; r++) {
                int pip = r < rank ? (dark ? 0xFFF3EBDD : 0xFF205C59) : 0x55302B24;
                c.fill(cx + 29 + r * 8, cy + 19, 6, 6, pip);
            }
            c.text(rank + "/" + insight.maxRanks, cx + cardW - 6 - Canvas.width(rank + "/" + insight.maxRanks), cy + 28,
                    dark ? RpgUi.LIGHT_TEAL : RpgUi.MUTED, false);
            ui.area(cx, cy, cardW, cardH, learnable, () -> {
                JsonObject body = new JsonObject();
                body.addProperty("insight", insight.id());
                RpgClient.send("learn", body);
            });
            if (over) {
                hover = new ArrayList<>();
                hover.add(Component.translatable(insight.key()).withStyle(s -> s.withColor(0xE6B85C)));
                hover.add(Component.translatable(insight.key() + ".description"));
                hover.add(Component.translatable("screen.tbos.codex.rank", rank, insight.maxRanks).withStyle(s -> s.withColor(0xA89B86)));
                if (learnable) {
                    hover.add(Component.translatable("screen.tbos.codex.learn_hint").withStyle(s -> s.withColor(0x7FD6C8)));
                }
            }
        }
        return hover;
    }

    // ---- Bestiary ---------------------------------------------------------------------------------------

    private List<Component> drawBestiary(Canvas c, JsonObject data, int x, int y, int w, int h, int mx, int my) {
        JsonObject kills = Json.obj(data, "kills");
        List<Bestiary.Entry> entries = Bestiary.ENTRIES;
        int listW = 150;
        c.panel(GreekGui.WELL, x, y, listW, h);
        int found = 0;
        for (Bestiary.Entry entry : entries) {
            if (Json.integer(kills, entry.id(), 0) > 0) {
                found++;
            }
        }
        Component count = Component.translatable("screen.tbos.codex.bestiary.count", found, entries.size());
        c.text(count, x + listW + 8, y + h - 10, RpgUi.MUTED, false);
        int rows = (h - 8) / 12;
        scroll = Math.max(0, Math.min(scroll, entries.size() - rows));
        selected = Math.max(0, Math.min(selected, entries.size() - 1));
        c.clip(x + 2, y + 2, listW - 4, h - 4);
        for (int i = 0; i < rows && i + scroll < entries.size(); i++) {
            int index = i + scroll;
            Bestiary.Entry entry = entries.get(index);
            int ry = y + 4 + i * 12;
            int killsOf = Json.integer(kills, entry.id(), 0);
            boolean known = killsOf > 0;
            boolean over = RpgUi.over(mx, my, x + 2, ry - 1, listW - 4, 12);
            if (index == selected) {
                c.fill(x + 3, ry - 1, listW - 6, 11, 0x40205C59);
            } else if (over) {
                c.fill(x + 3, ry - 1, listW - 6, 11, 0x22765021);
            }
            String name = known ? Component.translatable(entry.nameKey()).getString() : "???";
            c.text(RpgUi.fit(name, listW - 40), x + 7, ry, known ? RpgUi.INK : RpgUi.MUTED, false);
            String kc = known ? Integer.toString(killsOf) : "";
            c.text(kc, x + listW - 7 - Canvas.width(kc), ry, RpgUi.BRONZE, false);
            ui.area(x + 2, ry - 1, listW - 4, 12, true, () -> selected = index);
        }
        c.unclip();
        // Detail pane.
        Bestiary.Entry entry = entries.get(selected);
        int killsOf = Json.integer(kills, entry.id(), 0);
        int dx = x + listW + 8;
        int dw = w - listW - 8;
        if (killsOf <= 0) {
            c.icon(Icons.SEALED, dx + dw / 2 - 20, y + 20, 40, 0xA0FFFFFF);
            Component sealed = Component.translatable("screen.tbos.codex.bestiary.sealed");
            RpgUi.paragraph(c, sealed, dx, y + 70, dw, RpgUi.MUTED, 4);
            return null;
        }
        c.text(Component.translatable(entry.nameKey()), dx, y + 2, RpgUi.INK, false);
        Component role = Component.translatable("screen.tbos.codex.bestiary.role." + entry.role().name().toLowerCase(java.util.Locale.ROOT));
        Component home = entry.home() >= 0 ? Component.translatable("floor.tbos.name." + entry.home())
                : Component.translatable(entry.home() == -1 ? "screen.tbos.codex.bestiary.everywhere" : "screen.tbos.codex.bestiary.campaign");
        c.text(Component.translatable("screen.tbos.codex.bestiary.where", role, home), dx, y + 13, RpgUi.MUTED, false);
        c.text(Component.translatable("screen.tbos.codex.bestiary.threat"), dx, y + 26, RpgUi.MUTED, false);
        int tx = dx + Canvas.width(Component.translatable("screen.tbos.codex.bestiary.threat")) + 4;
        for (int i = 0; i < 5; i++) {
            c.fill(tx + i * 8, y + 27, 6, 6, i < entry.threat() ? 0xFFD9583B : 0x40302B24);
        }
        c.text(Component.translatable("screen.tbos.codex.bestiary.kills", killsOf), dx, y + 38, RpgUi.BRONZE, false);
        RpgUi.rule(c, dx, y + 51, dw);
        RpgUi.paragraph(c, Component.translatable(entry.loreKey()), dx, y + 56, dw, RpgUi.INK, 11);
        return null;
    }

    // ---- Records ----------------------------------------------------------------------------------------

    private List<Component> drawRecords(Canvas c, JsonObject data, int x, int y, int w, int h) {
        JsonObject records = Json.obj(data, "records");
        int half = (w - 8) / 2;
        c.panel(GreekGui.WELL, x, y, half, h);
        c.text(Component.translatable("screen.tbos.codex.records.yours"), x + 8, y + 6, RpgUi.BRONZE, false);
        int line = y + 20;
        line = row(c, x + 8, line, half - 16, "best_tier", Integer.toString(Json.integer(records, "best_tier", 0)));
        line = row(c, x + 8, line, half - 16, "best_fracture", Integer.toString(Json.integer(records, "best_fracture", 0)));
        line = row(c, x + 8, line, half - 16, "deepest", Long.toString(Json.lng(records, "deepest", 0)));
        line = row(c, x + 8, line, half - 16, "remembered", Integer.toString(Json.integer(records, "remembered", 0)));
        line = row(c, x + 8, line, half - 16, "forgotten", Integer.toString(Json.integer(records, "forgotten", 0)));
        line = row(c, x + 8, line, half - 16, "total_renown", RpgUi.number(Json.lng(records, "total_renown", 0)));
        int hourglass = Json.integer(records, "hourglass", 0);
        line = row(c, x + 8, line, half - 16, "hourglass", hourglass > 0 ? RpgUi.clock(hourglass) : "—");
        line += 4;
        c.text(Component.translatable("screen.tbos.codex.records.relics"), x + 8, line, RpgUi.MUTED, false);
        line += 12;
        JsonArray relics = Json.arr(records, "relics");
        int rx = x + 8;
        for (int r = 0; r < Rarity.values().length && r < relics.size(); r++) {
            c.icon(Icons.rarity(r), rx, line - 2, 12, 0xFFFFFFFF);
            String value = Integer.toString(relics.get(r).getAsInt());
            c.text(value, rx + 13, line, RpgUi.opaque(Rarity.values()[r].color), true);
            rx += 15 + Canvas.width(value) + 6;
        }
        int hx = x + half + 8;
        c.panel(GreekGui.WELL, hx, y, half, h);
        c.text(Component.translatable("screen.tbos.codex.records.hall"), hx + 8, y + 6, RpgUi.BRONZE, false);
        board(c, Json.arr(data, "hall"), hx + 8, y + 20, half - 16, false);
        return null;
    }

    private static int row(Canvas c, int x, int y, int w, String key, String value) {
        c.text(Component.translatable("screen.tbos.codex.records." + key), x, y, RpgUi.MUTED, false);
        c.text(value, x + w - Canvas.width(value), y, RpgUi.INK, false);
        return y + 11;
    }

    private static void board(Canvas c, JsonArray entries, int x, int y, int w, boolean weekly) {
        if (entries.isEmpty()) {
            c.text(Component.translatable("screen.tbos.codex.records.empty"), x, y, RpgUi.MUTED, false);
            return;
        }
        for (int i = 0; i < entries.size() && i < 10; i++) {
            JsonObject entry = entries.get(i).getAsJsonObject();
            int color = i == 0 ? 0xFFB07A1E : i < 3 ? RpgUi.BRONZE : RpgUi.INK;
            c.text((i + 1) + ".", x, y + i * 11, color, false);
            c.text(RpgUi.fit(Json.str(entry, "name", "?"), w - 70), x + 16, y + i * 11, color, false);
            String value = weekly
                    ? Component.translatable("screen.tbos.codex.records.floor_short", Json.lng(entry, "floor", 0)).getString()
                    : Component.translatable("screen.tbos.codex.records.tier_short", Json.integer(entry, "tier", 0),
                    Json.lng(entry, "floor", 0)).getString();
            c.text(value, x + w - Canvas.width(value), y + i * 11, RpgUi.MUTED, false);
        }
    }

    // ---- Weekly -----------------------------------------------------------------------------------------

    private List<Component> drawWeekly(Canvas c, JsonObject data, int x, int y, int w, int h, int mx, int my) {
        JsonObject weekly = Json.obj(data, "weekly");
        if (!Json.bool(weekly, "enabled", true)) {
            c.text(Component.translatable("screen.tbos.codex.weekly.disabled"), x + 4, y + 4, RpgUi.MUTED, false);
            return null;
        }
        int half = (w - 8) / 2;
        c.panel(GreekGui.CARD, x, y, half, h);
        c.icon(Icons.WEEKLY, x + 8, y + 8, 28, 0xFFFFFFFF);
        c.text(Component.translatable("screen.tbos.codex.weekly.title", Json.lng(weekly, "week", 0)), x + 42, y + 10, RpgUi.INK, false);
        c.text(Component.translatable("screen.tbos.codex.weekly.ends", RpgUi.clock(Json.lng(weekly, "seconds_left", 0))),
                x + 42, y + 21, RpgUi.MUTED, false);
        int line = y + 44;
        c.text(Component.translatable("screen.tbos.codex.weekly.tier", Json.integer(weekly, "tier", 1)), x + 10, line, RpgUi.BRONZE, false);
        line += 13;
        List<Component> hover = null;
        for (Distortion distortion : Distortion.of(Json.integer(weekly, "distortions", 0))) {
            c.icon(Icons.distortion(distortion.ordinal()), x + 10, line - 3, 14, 0xFFFFFFFF);
            c.text(Component.translatable(distortion.key()), x + 28, line, RpgUi.EMBER, false);
            if (RpgUi.over(mx, my, x + 8, line - 4, half - 16, 14)) {
                hover = List.of(Component.translatable(distortion.key()), Component.translatable(distortion.key() + ".description"));
            }
            line += 15;
        }
        long best = Json.lng(weekly, "best", 0);
        c.text(Component.translatable("screen.tbos.codex.weekly.best", best), x + 10, y + h - 46, RpgUi.MUTED, false);
        boolean claimed = Json.bool(weekly, "claimed", false);
        ui.button(c, x + 10, y + h - 30, half - 20, 20,
                Component.translatable(claimed ? "screen.tbos.codex.weekly.claimed" : "screen.tbos.codex.weekly.claim"),
                mx, my, !claimed, true, () -> RpgClient.send("weekly"));
        int bx = x + half + 8;
        c.panel(GreekGui.WELL, bx, y, half, h);
        c.text(Component.translatable("screen.tbos.codex.weekly.board"), bx + 8, y + 6, RpgUi.BRONZE, false);
        board(c, Json.arr(weekly, "board"), bx + 8, y + 20, half - 16, true);
        return hover;
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        return button == 0 && ui.click(mx, my);
    }

    @Override
    protected boolean onScroll(double mx, double my, double amount) {
        if (tab == 1) {
            scroll -= (int) Math.signum(amount);
            return true;
        }
        return false;
    }

    @Override
    protected boolean onKey(int key) {
        if (key == KEY_TAB) {
            tab = (tab + 1) % TABS.length;
            scroll = 0;
            Canvas.playUi(RpgSounds.CODEX_PAGE.get(), 1.0F, 0.7F);
            return true;
        }
        if (tab == 1 && (key == KEY_DOWN || key == KEY_UP)) {
            selected = Math.max(0, Math.min(Bestiary.ENTRIES.size() - 1, selected + (key == KEY_DOWN ? 1 : -1)));
            return true;
        }
        if (key == com.nightbeam.tbos.client.ModKeyMappings.CODEX.getDefaultKey().getValue() && com.nightbeam.tbos.client.ModKeyMappings.CODEX.isDefault()) {
            onClose();
            return true;
        }
        return false;
    }
}
