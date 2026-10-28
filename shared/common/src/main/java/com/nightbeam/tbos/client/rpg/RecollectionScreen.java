package com.nightbeam.tbos.client.rpg;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.nightbeam.tbos.client.GreekGui;
import com.nightbeam.tbos.expedition.Recollections;
import com.nightbeam.tbos.relic.Rarity;
import com.nightbeam.tbos.relic.Relic;
import com.nightbeam.tbos.relic.RelicGenerator;
import com.nightbeam.tbos.rpg.Json;
import com.nightbeam.tbos.rpg.RpgSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * The expedition's three big moments: the Remember-or-Forget choice after a floor
 * boss, the reveal of everything you banked, and the loss when a run is forgotten.
 */
public final class RecollectionScreen extends RpgScreen {
    public enum Mode { CHOICE, BANKED, FORGOTTEN }

    private static final int W = 316;
    private final Mode mode;
    private final JsonObject data;
    private final RpgUi ui = new RpgUi();
    private final long opened = System.currentTimeMillis();
    private final List<ItemStack> relics = new ArrayList<>();
    private final List<Rarity> rarities = new ArrayList<>();
    private int revealed;

    public RecollectionScreen(Mode mode, JsonObject data) {
        super(Component.translatable("screen.tbos.recollection.title"));
        this.mode = mode;
        this.data = data;
        if (mode == Mode.BANKED) {
            for (var element : Json.arr(data, "relics")) {
                JsonArray roll = element.getAsJsonArray();
                Rarity rarity = Rarity.byOrdinal(roll.get(2).getAsInt());
                relics.add(RelicGenerator.generate(roll.get(0).getAsLong(), roll.get(1).getAsInt(), rarity));
                rarities.add(rarity);
            }
        }
    }

    private long elapsed() {
        return System.currentTimeMillis() - opened;
    }

    @Override
    protected void draw(Canvas c, int mx, int my, float partialTick) {
        ui.begin();
        switch (mode) {
            case CHOICE -> drawChoice(c, mx, my);
            case BANKED -> drawBanked(c, mx, my);
            case FORGOTTEN -> drawForgotten(c, mx, my);
        }
    }

    // ---- Choice -----------------------------------------------------------------------------------------

    private void drawChoice(Canvas c, int mx, int my) {
        JsonObject live = RpgClient.expedition != null ? RpgClient.expedition : data;
        int h = 214;
        int x = (c.width() - W) / 2;
        int y = (c.height() - h) / 2;
        c.fill(0, 0, c.width(), c.height(), 0xA0000000);
        c.panel(GreekGui.FRAME, x, y, W, h);
        RpgUi.heading(c, Component.translatable("screen.tbos.recollection.choose"), x + W / 2, y + 12, RpgUi.INK);
        String place = Json.str(live, "place", "");
        Component where = Component.translatable("screen.tbos.recollection.floor_done", Json.lng(live, "floor", 1),
                Recollections.name(place));
        String whereText = RpgUi.fit(where.getString(), W - 30);
        c.text(whereText, x + (W - Canvas.width(whereText)) / 2, y + 26, RpgUi.MUTED, false);

        int wx = x + 14;
        int wy = y + 40;
        int ww = W - 28;
        c.panel(GreekGui.WELL, wx, wy, ww, 58);
        double momentum = Json.dbl(live, "momentum", 1);
        long renown = Json.lng(live, "renown", 0);
        c.icon(Icons.RENOWN, wx + 8, wy + 7, 22, 0xFFFFFFFF);
        c.text(Component.translatable("screen.tbos.recollection.renown", RpgUi.number(renown)), wx + 34, wy + 9, RpgUi.INK, false);
        c.text(Component.translatable("screen.tbos.recollection.momentum", String.format(Locale.ROOT, "%.2f", momentum)),
                wx + 34, wy + 20, RpgUi.MUTED, false);
        c.icon(Icons.RELIC, wx + ww / 2 + 6, wy + 7, 22, 0xFFFFFFFF);
        c.text(Component.translatable("screen.tbos.recollection.sealed", Json.integer(live, "relics", 0)),
                wx + ww / 2 + 32, wy + 9, RpgUi.INK, false);
        int nextKey = Json.integer(live, "next_key", 0);
        c.text(nextKey > 0 ? Component.translatable("screen.tbos.recollection.next_key", nextKey)
                : Component.translatable("screen.tbos.recollection.no_key"), wx + ww / 2 + 32, wy + 20, RpgUi.MUTED, false);
        // Who has chosen what.
        JsonArray party = Json.arr(live, "party");
        int px = wx + 8;
        for (var element : party) {
            JsonObject member = element.getAsJsonObject();
            boolean voted = Json.bool(member, "voted", false);
            boolean remembered = Json.bool(member, "remembered", false);
            String name = Json.str(member, "name", "?");
            int color = remembered ? RpgUi.BRONZE : voted ? RpgUi.TEAL : RpgUi.MUTED;
            String mark = remembered ? "◆ " : voted ? "↓ " : "· ";
            c.text(mark + name, px, wy + 40, color, false);
            px += Canvas.width(mark + name) + 10;
        }

        boolean choice = Json.bool(live, "choice", true);
        boolean voted = Json.bool(live, "voted", false);
        int cardW = (W - 28 - 8) / 2;
        int cy = wy + 66;
        card(c, mx, my, x + 14, cy, cardW, 74, Icons.REMEMBER, GreekGui.HOVER,
                Component.translatable("screen.tbos.recollection.remember"),
                Component.translatable("screen.tbos.recollection.remember.description"), choice, () -> RpgClient.send("remember"));
        card(c, mx, my, x + 14 + cardW + 8, cy, cardW, 74, Icons.FORGET, GreekGui.SELECTED,
                Component.translatable(voted ? "screen.tbos.recollection.forget_voted" : "screen.tbos.recollection.forget"),
                Component.translatable("screen.tbos.recollection.forget.description",
                        String.format(Locale.ROOT, "%.2f", momentum + 0.25)), choice && !voted, () -> RpgClient.send("forget"));
        ui.button(c, x + (W - 120) / 2, y + h - 26, 120, 16, Component.translatable("screen.tbos.recollection.later"), mx, my, true, false, this::onClose);
    }

    private void card(Canvas c, int mx, int my, int x, int y, int w, int h, int icon, int hoverTile,
            Component title, Component body, boolean enabled, Runnable action) {
        boolean over = enabled && RpgUi.over(mx, my, x, y, w, h);
        c.panel(!enabled ? GreekGui.DISABLED : over ? hoverTile : GreekGui.CARD, x, y, w, h);
        c.icon(icon, x + 8, y + 8, 28, enabled ? 0xFFFFFFFF : 0x80FFFFFF);
        c.text(RpgUi.fit(title.getString(), w - 50), x + 42, y + 12, enabled ? RpgUi.INK : RpgUi.MUTED, false);
        RpgUi.paragraph(c, body, x + 8, y + 40, w - 16, RpgUi.MUTED, 3);
        ui.area(x, y, w, h, enabled, action);
    }

    // ---- Banked -----------------------------------------------------------------------------------------

    private void drawBanked(Canvas c, int mx, int my) {
        long t = elapsed();
        int count = relics.size();
        int perRow = Math.max(1, Math.min(5, count + 1));
        int rows = (count + 1 + perRow - 1) / perRow;
        int h = 104 + rows * 84;
        int x = (c.width() - W) / 2;
        int y = Math.max(4, (c.height() - h) / 2);
        c.fill(0, 0, c.width(), c.height(), 0xB0000000);
        c.panel(GreekGui.FRAME, x, y, W, h);
        RpgUi.heading(c, Component.translatable("screen.tbos.recollection.remembered"), x + W / 2, y + 12, RpgUi.INK);

        long renown = Json.lng(data, "renown", 0);
        float count01 = Math.min(1, t / 1200.0F);
        float eased = 1 - (1 - count01) * (1 - count01) * (1 - count01);
        String value = "+" + RpgUi.number(Math.round(renown * eased));
        c.push();
        c.translate(x + W / 2.0F, y + 30);
        c.scale(2.0F);
        c.text(value, -Canvas.width(value) / 2, 0, RpgUi.BRONZE, false);
        c.pop();
        Component sub = Component.translatable("screen.tbos.recollection.renown_banked",
                String.format(Locale.ROOT, "%.2f", Json.dbl(data, "momentum", 1)));
        c.text(sub, x + (W - Canvas.width(sub)) / 2, y + 52, RpgUi.MUTED, false);
        int levels = Json.integer(data, "levels", 0);
        if (levels > 0 && t > 1200) {
            Component level = Component.translatable("screen.tbos.recollection.level_up", Json.integer(data, "level", 1));
            c.text(level, x + (W - Canvas.width(level)) / 2, y + 64, RpgUi.TEAL, false);
        }

        // Relics and the key flip in one after another once the count settles.
        int shown = (int) Math.max(0, (t - 1300) / 550 + 1);
        int total = count + (Json.integer(data, "key", 0) > 0 ? 1 : 0);
        while (revealed < Math.min(shown, total)) {
            if (revealed < count) {
                Rarity rarity = rarities.get(revealed);
                Canvas.playUi(switch (rarity) {
                    case COMMON, UNCOMMON -> RpgSounds.RELIC_COMMON.get();
                    case RARE -> RpgSounds.RELIC_RARE.get();
                    case EPIC -> RpgSounds.RELIC_EPIC.get();
                    case LEGENDARY -> RpgSounds.RELIC_LEGENDARY.get();
                    case REMNANT -> RpgSounds.RELIC_REMNANT.get();
                }, 1.0F, 1.0F);
            } else {
                Canvas.playUi(RpgSounds.EXPEDITION_KEY_RISE.get(), 1.0F, 0.9F);
            }
            revealed++;
        }
        int cardW = 56;
        int gap = 4;
        int rowW = Math.min(total, perRow) * (cardW + gap) - gap;
        List<Component> hover = null;
        for (int i = 0; i < total; i++) {
            int col = i % perRow;
            int row = i / perRow;
            int cx = x + (W - rowW) / 2 + col * (cardW + gap);
            int cy = y + 80 + row * 84;
            boolean visible = i < revealed;
            if (!visible) {
                c.panel(GreekGui.TRACK, cx, cy, cardW, 78);
                c.icon(Icons.SEALED, cx + 14, cy + 22, 28, 0x60FFFFFF);
                continue;
            }
            if (i < count) {
                Rarity rarity = rarities.get(i);
                ItemStack stack = relics.get(i);
                c.panel(GreekGui.CARD, cx, cy, cardW, 78);
                c.fill(cx + 3, cy + 3, cardW - 6, 2, RpgUi.opaque(rarity.color));
                c.icon(Icons.rarity(rarity.ordinal()), cx + cardW / 2 - 9, cy + 8, 18, 0xFFFFFFFF);
                c.push();
                c.translate(cx + cardW / 2.0F - 16, cy + 28);
                c.scale(2.0F);
                c.item(stack, 0, 0);
                c.pop();
                String label = RpgUi.fit(Component.translatable(rarity.key()).getString(), cardW - 6);
                c.text(label, cx + (cardW - Canvas.width(label)) / 2, cy + 64, RpgUi.opaque(darken(rarity.color)), false);
                if (RpgUi.over(mx, my, cx, cy, cardW, 78)) {
                    List<Component> lines = new ArrayList<>();
                    lines.add(stack.getHoverName());
                    Relic relic = Relic.read(stack);
                    if (relic != null) {
                        lines.addAll(relic.lore());
                    }
                    hover = lines;
                }
            } else {
                c.panel(GreekGui.HOVER, cx, cy, cardW, 78);
                c.icon(Icons.KEY, cx + cardW / 2 - 14, cy + 14, 28, 0xFFFFFFFF);
                Component tier = Component.translatable("screen.tbos.recollection.key_card", Json.integer(data, "key", 0));
                String text = RpgUi.fit(tier.getString(), cardW - 6);
                c.text(text, cx + (cardW - Canvas.width(text)) / 2, cy + 52, RpgUi.INK, false);
            }
        }
        boolean done = revealed >= total && t > 1300;
        ui.button(c, x + (W - 120) / 2, y + h - 24, 120, 16,
                Component.translatable(done ? "screen.tbos.close" : "screen.tbos.recollection.skip"), mx, my, true, done,
                done ? this::onClose : () -> revealed = 0x3FFFFFFF);
        if (hover != null) {
            c.tooltip(hover, mx, my);
        }
    }

    private static int darken(int rgb) {
        int r = (rgb >> 16 & 0xFF) * 3 / 5;
        int g = (rgb >> 8 & 0xFF) * 3 / 5;
        int b = (rgb & 0xFF) * 3 / 5;
        return r << 16 | g << 8 | b;
    }

    // ---- Forgotten --------------------------------------------------------------------------------------

    private void drawForgotten(Canvas c, int mx, int my) {
        float fade = Math.min(1, elapsed() / 900.0F);
        int h = 120;
        int x = (c.width() - W) / 2;
        int y = (c.height() - h) / 2;
        c.fill(0, 0, c.width(), c.height(), RpgUi.alpha(0xD0200808, fade));
        c.panel(GreekGui.WARNING, x, y, W, h);
        RpgUi.heading(c, Component.translatable("screen.tbos.recollection.forgotten"), x + W / 2, y + 14, RpgUi.LIGHT);
        Component lost = Component.translatable("screen.tbos.recollection.forgotten.lost",
                RpgUi.number(Json.lng(data, "renown", 0)), Json.integer(data, "relics", 0));
        RpgUi.paragraph(c, lost, x + 20, y + 40, W - 40, RpgUi.LIGHT, 3);
        Component hint = Component.translatable("screen.tbos.recollection.forgotten.hint");
        RpgUi.paragraph(c, hint, x + 20, y + 66, W - 40, 0xFFF0C8B8, 2);
        ui.button(c, x + (W - 100) / 2, y + h - 24, 100, 16, Component.translatable("screen.tbos.close"), mx, my, true, false, this::onClose);
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        return button == 0 && ui.click(mx, my);
    }
}
