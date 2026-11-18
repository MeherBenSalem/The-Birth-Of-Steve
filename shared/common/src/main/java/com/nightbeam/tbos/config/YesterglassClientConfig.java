package com.nightbeam.tbos.config;

import com.nightbeam.tbos.platform.Services;

/** Client-only presentation settings, at {@code config/tbos-client.json}. */
public final class YesterglassClientConfig {
    private static final ConfigSchema SCHEMA = new ConfigSchema("tbos-client.json");

    public static final ConfigValue.Int EFFECT_QUALITY = SCHEMA.defineInRange(
            "effectQuality", 2, 0, 3,
            "Reconstruction effect quality: 0 minimal, 1 low, 2 medium, 3 high.");

    public static final ConfigValue.Bool REDUCED_MOTION = SCHEMA.define(
            "reducedMotion", false,
            "Use a restrained crossfade-style particle motion.");

    public static final ConfigValue.Double OVERLAY_INTENSITY = SCHEMA.defineInRange(
            "overlayIntensity", 0.65D, 0.0D, 1.0D,
            "Reserved intensity control for the Lens HUD overlay.");

    static {
        SCHEMA.push("abilityHud");
    }

    public static final ConfigValue.Bool ABILITY_HUD_VISIBLE = SCHEMA.define(
            "visible", true,
            "Show the Memory ability strip and its pending-choice hint.");

    public static final ConfigValue.OfEnum<HudAnchor> ABILITY_HUD_ANCHOR = SCHEMA.defineEnum(
            "anchor", HudAnchor.BOTTOM_CENTER,
            "Where the ability strip is placed from: BOTTOM_CENTER (above the hotbar), BOTTOM_LEFT,"
                    + " BOTTOM_RIGHT, TOP_LEFT, TOP_CENTER or TOP_RIGHT.");

    public static final ConfigValue.Int ABILITY_HUD_OFFSET_X = SCHEMA.defineInRange(
            "offsetX", 0, -4000, 4000,
            "GUI pixels to move the ability strip right from its anchor (negative moves it left).");

    public static final ConfigValue.Int ABILITY_HUD_OFFSET_Y = SCHEMA.defineInRange(
            "offsetY", 0, -4000, 4000,
            "GUI pixels to move the ability strip down from its anchor (negative moves it up).");

    static {
        SCHEMA.pop();
    }

    private YesterglassClientConfig() {
    }

    /** Called once by each loader's client entry point. */
    public static void load() {
        SCHEMA.load(Services.PLATFORM.configDir());
    }
}
