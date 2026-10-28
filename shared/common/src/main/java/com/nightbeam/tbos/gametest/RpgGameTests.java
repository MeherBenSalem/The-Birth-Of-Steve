package com.nightbeam.tbos.gametest;

import com.nightbeam.tbos.codex.HallOfRecords;
import com.nightbeam.tbos.codex.Insight;
import com.nightbeam.tbos.codex.PlayerProfile;
import com.nightbeam.tbos.codex.Weekly;
import com.nightbeam.tbos.compat.RpgCompat;
import com.nightbeam.tbos.expedition.Distortion;
import com.nightbeam.tbos.expedition.ExpeditionKey;
import com.nightbeam.tbos.expedition.Expeditions;
import com.nightbeam.tbos.relic.Affix;
import com.nightbeam.tbos.relic.Rarity;
import com.nightbeam.tbos.relic.Relic;
import com.nightbeam.tbos.relic.RelicGenerator;
import com.nightbeam.tbos.rpg.RpgConfig;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * GameTests for the 1.0 systems. They touch only the RPG classes and their seams,
 * so every target runs this one file: 26.x registers the methods as test functions
 * from {@link ModGameTests}, and the 1.21.1 and 1.20.1 loader suites wrap them in
 * {@code @GameTest} methods.
 */
public final class RpgGameTests {
    private RpgGameTests() {
    }

    /** Memory Keys survive the item round trip, clamp their tier and keep weekly keys fixed. */
    public static void keyRoundTrip(GameTestHelper helper) {
        int pair = Distortion.FORTIFIED.bit() | Distortion.VOLATILE.bit();
        List<ExpeditionKey> keys = List.of(
                ExpeditionKey.basic(1),
                new ExpeditionKey(7, pair, ExpeditionKey.NOT_WEEKLY, 0L),
                new ExpeditionKey(9, Distortion.HOURGLASS.bit() | Distortion.AMNESIA.bit(), 2_900L, 0x5EEDL));
        for (ExpeditionKey key : keys) {
            ItemStack stack = key.stack();
            helper.assertTrue(ExpeditionKey.isKey(stack), "A key stack is not recognised as a Memory Key");
            helper.assertTrue(ExpeditionKey.read(stack).equals(key), "Key " + key + " read back as " + ExpeditionKey.read(stack));
        }
        ExpeditionKey hot = keys.get(1);
        helper.assertTrue(hot.heat() == 2 && hot.fracture() == 9, "Two Distortions on tier 7 should be Heat 2, Fracture 9");
        ExpeditionKey weekly = keys.get(2);
        helper.assertTrue(weekly.weekly() && weekly.withDistortions(0).equals(weekly), "Weekly keys must not be re-attuned");
        int max = RpgConfig.get().expeditions.maxTier;
        helper.assertTrue(ExpeditionKey.basic(max + 40).tier() == max && ExpeditionKey.basic(-3).tier() == 1,
                "Key tiers are not clamped to 1.." + max);
        ItemStack corrupt = ExpeditionKey.basic(4).stack();
        RpgCompat.writeData(corrupt, "{not json");
        helper.assertTrue(ExpeditionKey.read(corrupt).equals(ExpeditionKey.basic(1)), "A corrupt key should read as tier 1");
        double factor = Expeditions.rewardFactor(hot);
        RpgConfig.Expeditions config = RpgConfig.get().expeditions;
        double expected = 1 + config.rewardPerTier * 6 + config.rewardPerHeat * 2;
        helper.assertTrue(Math.abs(factor - expected) < 1e-9, "Reward factor " + factor + " should be " + expected);
        helper.succeed();
    }

    /** The weekly key is a pure function of world seed and week, within the configured band. */
    public static void weeklyIsDeterministic(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        RpgConfig.Weekly config = RpgConfig.get().weekly;
        Set<Integer> masks = new HashSet<>();
        Set<Integer> tiers = new HashSet<>();
        for (long week = 2_800; week < 2_852; week++) {
            ExpeditionKey first = Weekly.key(server, week);
            helper.assertTrue(first.equals(Weekly.key(server, week)), "Week " + week + " produced two different keys");
            helper.assertTrue(first.weekly() && first.week() == week, "Week " + week + " key is not marked weekly");
            helper.assertTrue(first.tier() >= config.minTier && first.tier() <= config.maxTier,
                    "Week " + week + " tier " + first.tier() + " is outside " + config.minTier + ".." + config.maxTier);
            helper.assertTrue(first.heat() == config.distortions,
                    "Week " + week + " has " + first.heat() + " Distortions, not " + config.distortions);
            masks.add(first.distortions());
            tiers.add(first.tier());
        }
        helper.assertTrue(masks.size() >= 5 && tiers.size() >= 2, "A year of weekly keys barely varies: "
                + masks.size() + " Distortion sets, " + tiers.size() + " tiers");
        helper.succeed();
    }

    /** Generated relics are deterministic, round-trip through the stack and follow the tier rules. */
    public static void relicGeneration(GameTestHelper helper) {
        RpgConfig.Relics config = RpgConfig.get().relics;
        for (int tier = 1; tier <= 15; tier++) {
            double[] weights = RelicGenerator.weights(tier, 0);
            for (double weight : weights) {
                helper.assertTrue(weight >= 0, "Negative rarity weight at tier " + tier);
            }
            helper.assertTrue((weights[Rarity.REMNANT.ordinal()] > 0) == (tier >= config.remnantFromTier),
                    "Remnant odds at tier " + tier + " ignore remnantFromTier " + config.remnantFromTier);
            double[] keen = RelicGenerator.weights(tier, 3);
            helper.assertTrue(share(keen, Rarity.EPIC) > share(weights, Rarity.EPIC),
                    "Keen Eye does not improve Epic odds at tier " + tier);
            for (Rarity rarity : Rarity.values()) {
                long seed = 0x9E37_79B9L * tier + rarity.ordinal();
                ItemStack stack = RelicGenerator.generate(seed, tier, rarity);
                Relic relic = Relic.read(stack);
                helper.assertTrue(relic != null, "Tier " + tier + " " + rarity + " relic has no relic data");
                Relic again = Relic.read(RelicGenerator.generate(seed, tier, rarity));
                helper.assertTrue(relic.equals(again), "Relic generation is not deterministic for seed " + seed);
                helper.assertTrue(relic.rarity() == rarity, "Relic rarity changed in the round trip");
                helper.assertTrue(relic.itemLevel() == 10 + 5 * tier, "Item level should be 10 + 5 x tier");
                helper.assertTrue(relic.affixes().size() == rarity.affixes,
                        rarity + " relic has " + relic.affixes().size() + " affixes, not " + rarity.affixes);
                relic.affixes().forEach((affix, value) -> helper.assertTrue(value > 0 && value <= affix.cap + 1e-9,
                        affix + " rolled " + value + " outside (0, " + affix.cap + "]"));
                helper.assertTrue((relic.signature() != null) == (rarity == Rarity.REMNANT),
                        "Only Remnant relics carry a signature");
                String id = RpgCompat.itemId(stack);
                boolean ranged = id.endsWith("bow");
                String material = tier <= config.ironUntilTier ? "iron" : tier <= config.diamondUntilTier ? "diamond" : "netherite";
                helper.assertTrue(ranged || id.contains(material), "Tier " + tier + " relic base " + id + " is not " + material);
            }
        }
        helper.succeed();
    }

    private static double share(double[] weights, Rarity rarity) {
        double total = 0;
        for (double weight : weights) {
            total += weight;
        }
        return weights[rarity.ordinal()] / total;
    }

    /** Renown levels, Insight points, rank caps, respec and the profile file format. */
    public static void renownCurve(GameTestHelper helper) {
        RpgConfig.Codex config = RpgConfig.get().codex;
        PlayerProfile profile = new PlayerProfile(UUID.fromString("00000000-0000-0000-0000-00000000c0de"));
        helper.assertTrue(profile.level() == 1 && profile.unspentInsight() == 0, "A new profile should be level 1 with no Insight");
        helper.assertTrue(PlayerProfile.costOf(4) == config.levelBase + 4L * config.levelStep, "Level cost does not follow the config");
        helper.assertTrue(profile.addRenown(PlayerProfile.costOf(1) - 1) == 0, "One Renown short still levelled up");
        helper.assertTrue(profile.addRenown(1) == 1 && profile.level() == 2, "Exactly the level cost should reach level 2");
        helper.assertTrue(profile.unspentInsight() == 1, "Level 2 should grant one Insight point");
        helper.assertTrue(profile.learn(Insight.STEADY_HAND), "Could not learn Steady Hand with a free point");
        helper.assertTrue(!profile.learn(Insight.STEADY_HAND), "Learned an Insight with no points left");
        long toMax = 0;
        for (int level = 1; level < config.maxLevel; level++) {
            toMax += PlayerProfile.costOf(level);
        }
        profile.addRenown(toMax * 2);
        helper.assertTrue(profile.level() == config.maxLevel, "Renown past the cap should stop at level " + config.maxLevel);
        for (int i = 0; i < 5; i++) {
            profile.learn(Insight.STEADY_HAND);
        }
        helper.assertTrue(profile.rank(Insight.STEADY_HAND) == Insight.STEADY_HAND.maxRanks, "Steady Hand went past its rank cap");
        profile.learn(Insight.DEEP_RECALL);
        PlayerProfile copy = PlayerProfile.fromJson(profile.id, profile.toJson());
        helper.assertTrue(copy.level() == profile.level() && copy.rank(Insight.DEEP_RECALL) == 1
                && copy.unspentInsight() == profile.unspentInsight(), "The profile file lost Renown or Insights");
        profile.respec();
        helper.assertTrue(profile.spentInsight() == 0 && profile.unspentInsight() == config.maxLevel - 1,
                "Respec should refund every point");
        helper.succeed();
    }

    /** The Hall keeps each player's best run, sorted, and at most ten. */
    public static void hallOfRecords(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        HallOfRecords.clear();
        try {
            UUID ada = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
            HallOfRecords.submit(server, new HallOfRecords.Entry(ada, "Ada", 5, 7, 3, 400));
            HallOfRecords.submit(server, new HallOfRecords.Entry(ada, "Ada", 4, 9, 9, 100));
            helper.assertTrue(HallOfRecords.allTime().size() == 1 && HallOfRecords.allTime().get(0).tier() == 5,
                    "A worse run replaced the player's best");
            HallOfRecords.submit(server, new HallOfRecords.Entry(ada, "Ada", 5, 7, 6, 500));
            helper.assertTrue(HallOfRecords.allTime().get(0).floor() == 6, "A deeper run at the same tier was not kept");
            for (int i = 0; i < 14; i++) {
                UUID other = new UUID(0x7E57L, i);
                HallOfRecords.submit(server, new HallOfRecords.Entry(other, "P" + i, 1 + i % 8, i, i % 5, 60));
            }
            List<HallOfRecords.Entry> all = HallOfRecords.allTime();
            helper.assertTrue(all.size() == HallOfRecords.SIZE, "The Hall holds " + all.size() + " entries, not " + HallOfRecords.SIZE);
            for (int i = 1; i < all.size(); i++) {
                HallOfRecords.Entry a = all.get(i - 1);
                HallOfRecords.Entry b = all.get(i);
                helper.assertTrue(a.tier() > b.tier() || (a.tier() == b.tier() && a.floor() >= b.floor()),
                        "The Hall is out of order at " + i);
            }
            long week = Weekly.currentWeek();
            HallOfRecords.submitWeekly(server, week - 1, new HallOfRecords.Entry(ada, "Ada", 8, 10, 4, 90));
            helper.assertTrue(HallOfRecords.weekly().isEmpty(), "A run from last week entered this week's board");
            HallOfRecords.submitWeekly(server, week, new HallOfRecords.Entry(ada, "Ada", 8, 10, 4, 90));
            helper.assertTrue(HallOfRecords.weekly().size() == 1, "This week's run is missing from the weekly board");
        } finally {
            HallOfRecords.clear();
        }
        helper.succeed();
    }

    /** Distortion bits are unique and the mask covers exactly the declared set. */
    public static void distortionMask(GameTestHelper helper) {
        int seen = 0;
        for (Distortion distortion : Distortion.values()) {
            helper.assertTrue((seen & distortion.bit()) == 0, distortion + " reuses another Distortion's bit");
            seen |= distortion.bit();
            helper.assertTrue(Distortion.of(distortion.bit()).equals(List.of(distortion)), distortion + " does not decode alone");
        }
        helper.assertTrue(seen == Distortion.mask() && Distortion.heat(-1) == Distortion.values().length,
                "The Distortion mask and Heat disagree");
        for (Affix affix : Affix.values()) {
            helper.assertTrue(affix.min > 0 && affix.min <= affix.max && affix.max <= affix.cap,
                    affix + " has an inverted or uncapped range");
        }
        helper.succeed();
    }
}
