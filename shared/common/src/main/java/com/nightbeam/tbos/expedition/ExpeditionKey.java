package com.nightbeam.tbos.expedition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.rpg.Json;
import com.nightbeam.tbos.rpg.RpgConfig;
import com.nightbeam.tbos.rpg.RpgItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

/**
 * What a Memory Key carries: a Fracture Tier, its attuned Distortions and, for the
 * Weekly Recollection, the week and fixed seed. A crafted key has no data yet and
 * reads as tier 1 with no Distortions.
 */
public record ExpeditionKey(int tier, int distortions, long week, long seed) {
    public static final long NOT_WEEKLY = Long.MIN_VALUE;
    public static final int GOLD = 0xE6B85C;
    public static final int EMBER = 0xD9583B;
    public static final int MUTED = 0xA89B86;

    public ExpeditionKey {
        tier = Math.max(1, Math.min(RpgConfig.get().expeditions.maxTier, tier));
        distortions &= Distortion.mask();
    }

    public static ExpeditionKey basic(int tier) {
        return new ExpeditionKey(tier, 0, NOT_WEEKLY, 0L);
    }

    public boolean weekly() {
        return week != NOT_WEEKLY;
    }

    public int heat() {
        return Distortion.heat(distortions);
    }

    public int fracture() {
        return tier + heat();
    }

    public ExpeditionKey withDistortions(int mask) {
        return weekly() ? this : new ExpeditionKey(tier, mask, week, seed);
    }

    public ExpeditionKey withTier(int value) {
        return new ExpeditionKey(value, distortions, NOT_WEEKLY, 0L);
    }

    public static boolean isKey(ItemStack stack) {
        return !stack.isEmpty() && stack.is(RpgItems.MEMORY_KEY.get());
    }

    public static ExpeditionKey read(ItemStack stack) {
        String raw = RpgCompat.readData(stack);
        if (raw.isEmpty()) {
            return basic(1);
        }
        try {
            JsonObject key = Json.obj(JsonParser.parseString(raw).getAsJsonObject(), "key");
            return new ExpeditionKey(Json.integer(key, "tier", 1), Json.integer(key, "distortions", 0),
                    Json.lng(key, "week", NOT_WEEKLY), Json.lng(key, "seed", 0L));
        } catch (RuntimeException exception) {
            return basic(1);
        }
    }

    public ItemStack stack() {
        ItemStack stack = new ItemStack(RpgItems.MEMORY_KEY.get());
        write(stack);
        return stack;
    }

    /** Stores the data and refreshes the name, lore and glint that describe it. */
    public void write(ItemStack stack) {
        JsonObject key = new JsonObject();
        key.addProperty("tier", tier);
        key.addProperty("distortions", distortions);
        if (weekly()) {
            key.addProperty("week", week);
            key.addProperty("seed", seed);
        }
        JsonObject root = new JsonObject();
        root.add("key", key);
        RpgCompat.writeData(stack, root.toString());
        RpgCompat.setName(stack, name());
        RpgCompat.setLore(stack, lore());
        RpgCompat.setGlint(stack, weekly() || tier >= 10 || heat() >= 3);
    }

    public Component name() {
        return Component.translatable(weekly() ? "item.tbos.memory_key.weekly_name" : "item.tbos.memory_key.tiered_name", tier)
                .withStyle(Style.EMPTY.withItalic(false).withColor(weekly() ? 0x8FD3E8 : GOLD));
    }

    public List<Component> lore() {
        List<Component> lines = new ArrayList<>();
        if (weekly()) {
            lines.add(line(Component.translatable("item.tbos.memory_key.weekly", week), 0x8FD3E8));
        }
        if (distortions == 0) {
            lines.add(line(Component.translatable("item.tbos.memory_key.no_distortions"), MUTED));
        } else {
            MutableComponent names = Component.empty();
            List<Distortion> active = Distortion.of(distortions);
            for (int i = 0; i < active.size(); i++) {
                if (i > 0) {
                    names.append(Component.literal(", "));
                }
                names.append(Component.translatable(active.get(i).key()));
            }
            lines.add(line(Component.translatable("item.tbos.memory_key.distortions", names), EMBER));
        }
        lines.add(line(Component.translatable("item.tbos.memory_key.fracture", heat(), fracture()), MUTED));
        lines.add(line(Component.translatable(weekly() ? "item.tbos.memory_key.sealed" : "item.tbos.memory_key.use"), MUTED));
        return lines;
    }

    private static Component line(Component text, int color) {
        return Component.empty().append(text).withStyle(Style.EMPTY.withItalic(false).withColor(color));
    }

    /** Keeps formatting codes out of a JSON-safe preview line for the screen. */
    public static ChatFormatting tierColor(int tier) {
        return tier >= 15 ? ChatFormatting.RED : tier >= 10 ? ChatFormatting.GOLD : tier >= 5 ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
    }
}
