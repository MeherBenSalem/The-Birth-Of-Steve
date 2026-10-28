package com.nightbeam.tbos.relic;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.rpg.Json;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

/**
 * The Relic data on a vanilla weapon or armor piece. {@code slot} is "weapon" for
 * main-hand Relics, or the armor slot ("head", "chest", "legs", "feet").
 */
public record Relic(Rarity rarity, int itemLevel, String slot, Map<Affix, Double> affixes,
        Affix.Signature signature, int flavor) {
    public static final int FLAVOR_LINES = 12;

    public boolean weapon() {
        return "weapon".equals(slot);
    }

    public double value(Affix affix) {
        return affixes.getOrDefault(affix, 0.0);
    }

    public static Relic read(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        String raw = RpgCompat.readData(stack);
        if (raw.isEmpty() || !raw.contains("\"relic\"")) {
            return null;
        }
        try {
            JsonObject relic = Json.obj(JsonParser.parseString(raw).getAsJsonObject(), "relic");
            Map<Affix, Double> affixes = new LinkedHashMap<>();
            for (var element : Json.arr(relic, "a")) {
                JsonArray pair = element.getAsJsonArray();
                Affix affix = Affix.byId(pair.get(0).getAsString());
                if (affix != null) {
                    affixes.put(affix, Math.min(affix.cap, pair.get(1).getAsDouble()));
                }
            }
            return new Relic(Rarity.byOrdinal(Json.integer(relic, "r", 0)), Json.integer(relic, "lvl", 10),
                    Json.str(relic, "slot", "weapon"), affixes, Affix.Signature.byId(Json.str(relic, "sig", "")),
                    Json.integer(relic, "f", 0));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public void write(ItemStack stack, Component name) {
        JsonObject relic = new JsonObject();
        relic.addProperty("r", rarity.ordinal());
        relic.addProperty("lvl", itemLevel);
        relic.addProperty("slot", slot);
        JsonArray list = new JsonArray();
        affixes.forEach((affix, value) -> {
            JsonArray pair = new JsonArray();
            pair.add(affix.id());
            pair.add(Math.round(value * 1000.0) / 1000.0);
            list.add(pair);
        });
        relic.add("a", list);
        if (signature != null) {
            relic.addProperty("sig", signature.id());
        }
        relic.addProperty("f", flavor);
        JsonObject root = new JsonObject();
        root.add("relic", relic);
        RpgCompat.writeData(stack, root.toString());
        RpgCompat.setName(stack, name.copy().withStyle(Style.EMPTY.withItalic(false).withColor(rarity.color)));
        RpgCompat.setLore(stack, lore());
        RpgCompat.setGlint(stack, rarity.ordinal() >= Rarity.EPIC.ordinal());
    }

    public List<Component> lore() {
        List<Component> lines = new ArrayList<>();
        lines.add(styled(Component.translatable("relic.tbos.header", Component.translatable(rarity.key()), itemLevel), rarity.color));
        affixes.forEach((affix, value) -> lines.add(styled(
                Component.translatable(affix.key(), format(affix, value)),
                affix.stat != null ? 0x9CC9F0 : 0xE6C27A)));
        if (signature != null) {
            lines.add(styled(Component.translatable(signature.key()), Rarity.REMNANT.color));
        }
        lines.add(Component.translatable("relic.tbos.flavor." + Math.floorMod(flavor, FLAVOR_LINES))
                .withStyle(Style.EMPTY.withItalic(true).withColor(0x8A8070)));
        return lines;
    }

    public static String format(Affix affix, double value) {
        if (affix.percent()) {
            return Math.round(value * 100.0) + "%";
        }
        double rounded = Math.round(value * 10.0) / 10.0;
        return rounded == Math.floor(rounded) ? Long.toString((long) rounded) : Double.toString(rounded);
    }

    private static Component styled(Component text, int color) {
        return Component.empty().append(text).withStyle(Style.EMPTY.withItalic(false).withColor(color));
    }
}
