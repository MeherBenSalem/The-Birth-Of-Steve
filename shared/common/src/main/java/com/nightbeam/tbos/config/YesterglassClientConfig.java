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

    private YesterglassClientConfig() {
    }

    /** Called once by each loader's client entry point. */
    public static void load() {
        SCHEMA.load(Services.PLATFORM.configDir());
    }
}
