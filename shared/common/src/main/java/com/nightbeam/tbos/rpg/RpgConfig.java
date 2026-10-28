package com.nightbeam.tbos.rpg;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.platform.Services;
import java.nio.file.Path;

/**
 * {@code config/tbos-expeditions.json}: every number behind Expeditions, the Codex,
 * Relics and parties. Missing keys come back with their defaults on load, and the
 * file is rewritten so new keys appear for server owners to tune. Each section
 * carries an {@code _about} line explaining it.
 */
public final class RpgConfig {
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final String FILE = "tbos-expeditions.json";
    private static RpgConfig current = new RpgConfig();

    public Expeditions expeditions = new Expeditions();
    public Distortions distortions = new Distortions();
    public Codex codex = new Codex();
    public Weekly weekly = new Weekly();
    public Relics relics = new Relics();
    public Party party = new Party();

    public static RpgConfig get() {
        return current;
    }

    public static void load() {
        Path file = Services.PLATFORM.configDir().resolve(FILE);
        JsonObject json = JsonStore.read(file);
        RpgConfig loaded;
        try {
            loaded = json.size() == 0 ? new RpgConfig() : GSON.fromJson(json, RpgConfig.class);
        } catch (RuntimeException exception) {
            Yesterglass.LOGGER.error("{} is invalid; using defaults until it is fixed", FILE, exception);
            current = new RpgConfig();
            return;
        }
        current = loaded == null ? new RpgConfig() : loaded.sanitized();
        JsonStore.write(file, GSON.toJsonTree(current));
    }

    /** Applies sane bounds so a typo cannot crash a server. */
    private RpgConfig sanitized() {
        if (expeditions == null) expeditions = new Expeditions();
        if (distortions == null) distortions = new Distortions();
        if (codex == null) codex = new Codex();
        if (weekly == null) weekly = new Weekly();
        if (relics == null) relics = new Relics();
        if (party == null) party = new Party();
        expeditions.maxTier = clamp(expeditions.maxTier, 1, 50);
        expeditions.hourglassSeconds = clamp(expeditions.hourglassSeconds, 60, 3600);
        expeditions.partyRadius = clamp(expeditions.partyRadius, 4, 128);
        codex.maxLevel = clamp(codex.maxLevel, 1, 100);
        weekly.minTier = clamp(weekly.minTier, 1, expeditions.maxTier);
        weekly.maxTier = clamp(weekly.maxTier, weekly.minTier, expeditions.maxTier);
        weekly.distortions = clamp(weekly.distortions, 0, 4);
        party.maxSize = clamp(party.maxSize, 2, 4);
        party.inviteSeconds = clamp(party.inviteSeconds, 10, 600);
        return this;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static final class Expeditions {
        public String _about = "Memory Keys, Fracture Tiers and the Remember-or-Forget reward loop.";
        public int maxTier = 20;
        public double healthPerTier = 0.10;
        public double damagePerTier = 0.05;
        public double rewardPerTier = 0.12;
        public double rewardPerHeat = 0.15;
        public double momentumPerFloor = 0.25;
        public int renownRoom = 15;
        public int renownWarden = 40;
        public int renownBoss = 120;
        public int hourglassSeconds = 480;
        public double hourglassRenownBonus = 0.25;
        public int partyRadius = 24;
        public double valorFraction = 0.25;
        public boolean dimmedKeyOnFailure = true;
        public boolean recollections = true;
        public boolean recollectOncePerStructure = true;
        public boolean keyRecipe = true;
    }

    public static final class Distortions {
        public String _about = "Heat modifiers chosen on a Memory Key. Each active Distortion adds one Heat.";
        public double fortifiedHealth = 0.30;
        public double tyrannicalHealth = 0.40;
        public double tyrannicalDamage = 0.15;
        public double bolsterRadius = 10.0;
        public int bolsterSeconds = 10;
        public double volatileDamage = 4.0;
        public double volatileRadius = 3.0;
        public int hauntSeconds = 30;
    }

    public static final class Codex {
        public String _about = "Renown levels and Insights. Level L to L+1 costs level_base + level_step * L Renown.";
        public int maxLevel = 30;
        public int levelBase = 150;
        public int levelStep = 50;
        public boolean freeRespec = true;
    }

    public static final class Weekly {
        public String _about = "The Weekly Recollection: one fixed-seed key per player per week (weeks start Monday 00:00 UTC).";
        public boolean enabled = true;
        public int minTier = 6;
        public int maxTier = 10;
        public int distortions = 2;
    }

    public static final class Relics {
        public String _about = "Relic gear recovered from floor bosses and wardens.";
        public boolean enabled = true;
        public boolean announceEpic = true;
        public int ironUntilTier = 6;
        public int diamondUntilTier = 13;
        public int remnantFromTier = 10;
    }

    public static final class Party {
        public String _about = "The /party system and party frames.";
        public int maxSize = 4;
        public int inviteSeconds = 60;
        public boolean frames = true;
    }
}
