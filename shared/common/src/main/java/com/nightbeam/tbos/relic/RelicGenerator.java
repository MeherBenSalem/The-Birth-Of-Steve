package com.nightbeam.tbos.relic;

import com.nightbeam.tbos.rpg.RpgConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Rolls Relics. Everything is derived from a seed, so a Relic earned on a floor is
 * the Relic you get when you Remember, no matter when that is. Rarity odds shift
 * towards the rarer grades with each tier above 1 and each Keen Eye rank.
 */
public final class RelicGenerator {
    private static final String[] THEMES = {"wake", "gallery", "meridian", "choir", "vault", "catalogue", "labyrinth", "unwritten"};

    private RelicGenerator() {
    }

    /** Relative weights of each rarity at {@code tier} with {@code keenEye} ranks. */
    public static double[] weights(int tier, int keenEye) {
        double shift = 0.04 * (tier - 1) + 0.06 * keenEye;
        RpgConfig.Relics config = RpgConfig.get().relics;
        double remnant = tier >= config.remnantFromTier ? 0.15 * (tier - config.remnantFromTier + 1) * (1 + shift) : 0;
        return new double[]{
            50 * Math.max(0.15, 1 - 0.7 * shift),
            30 * (1 + 0.2 * shift),
            15 * (1 + shift),
            4.5 * (1 + 2 * shift),
            0.5 * (1 + 4 * shift),
            remnant
        };
    }

    public static Rarity rollRarity(Random random, int tier, int keenEye, Rarity floor) {
        double[] weights = weights(tier, keenEye);
        double total = 0;
        for (int i = floor.ordinal(); i < weights.length; i++) {
            total += weights[i];
        }
        double roll = random.nextDouble() * total;
        for (int i = floor.ordinal(); i < weights.length; i++) {
            roll -= weights[i];
            if (roll < 0) {
                return Rarity.byOrdinal(i);
            }
        }
        return floor;
    }

    private record Base(String noun, String slot, Item iron, Item diamond, Item netherite) {
        Item pick(int tier) {
            RpgConfig.Relics config = RpgConfig.get().relics;
            return tier <= config.ironUntilTier ? iron : tier <= config.diamondUntilTier ? diamond : netherite;
        }
    }

    private static final List<Base> WEAPONS = List.of(
            new Base("blade", "weapon", Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD),
            new Base("blade", "weapon", Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD),
            new Base("cleaver", "weapon", Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE),
            new Base("longbow", "weapon", Items.BOW, Items.BOW, Items.BOW),
            new Base("arbalest", "weapon", Items.CROSSBOW, Items.CROSSBOW, Items.CROSSBOW));
    private static final List<Base> ARMOR = List.of(
            new Base("helm", "head", Items.IRON_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET),
            new Base("cuirass", "chest", Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE),
            new Base("greaves", "legs", Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS),
            new Base("sabatons", "feet", Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS));

    public static ItemStack generate(long seed, int tier, Rarity rarity) {
        Random random = new Random(seed);
        boolean weapon = random.nextDouble() < 0.45;
        Base base = (weapon ? WEAPONS : ARMOR).get(random.nextInt(weapon ? WEAPONS.size() : ARMOR.size()));
        double quality = 1 + 0.06 * (tier - 1);
        List<Affix> pool = new ArrayList<>();
        for (Affix affix : Affix.values()) {
            if (affix.weapon == weapon) {
                pool.add(affix);
            }
        }
        Collections.shuffle(pool, random);
        Map<Affix, Double> affixes = new LinkedHashMap<>();
        for (int i = 0; i < rarity.affixes && i < pool.size(); i++) {
            Affix affix = pool.get(i);
            double value = affix.min + random.nextDouble() * (affix.max - affix.min);
            if (affix.scales) {
                value *= quality;
            }
            // Rarer grades roll towards the top of each range.
            value *= 1 + 0.05 * rarity.ordinal();
            affixes.put(affix, Math.min(affix.cap, value));
        }
        Affix.Signature signature = rarity == Rarity.REMNANT
                ? Affix.Signature.values()[random.nextInt(Affix.Signature.values().length)]
                : null;
        Relic relic = new Relic(rarity, 10 + 5 * tier, base.slot, affixes, signature, random.nextInt(Relic.FLAVOR_LINES));
        ItemStack stack = new ItemStack(base.pick(tier));
        relic.write(stack, name(base, relic, random));
        return stack;
    }

    private static Component name(Base base, Relic relic, Random random) {
        Component noun = Component.translatable("relic.tbos.base." + base.noun);
        if (relic.signature() != null) {
            return Component.translatable("relic.tbos.name.signature." + relic.signature().id(), noun);
        }
        Affix lead = relic.affixes().keySet().stream().findFirst().orElse(null);
        Component prefix = lead == null ? Component.empty() : Component.translatable("relic.tbos.prefix." + lead.id());
        if (relic.rarity().ordinal() >= Rarity.EPIC.ordinal()) {
            return Component.translatable("relic.tbos.name.epic", prefix, noun,
                    Component.translatable("relic.tbos.suffix." + THEMES[random.nextInt(THEMES.length)]));
        }
        return Component.translatable("relic.tbos.name.plain", prefix, noun);
    }
}
