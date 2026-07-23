package com.nightbeam.tbos.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class YesterglassClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue EFFECT_QUALITY = BUILDER
            .comment("Reconstruction effect quality: 0 minimal, 1 low, 2 medium, 3 high.")
            .defineInRange("effectQuality", 2, 0, 3);

    public static final ModConfigSpec.BooleanValue REDUCED_MOTION = BUILDER
            .comment("Use a restrained crossfade-style particle motion.")
            .define("reducedMotion", false);

    public static final ModConfigSpec.DoubleValue OVERLAY_INTENSITY = BUILDER
            .comment("Reserved intensity control for the Lens HUD overlay.")
            .defineInRange("overlayIntensity", 0.65D, 0.0D, 1.0D);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private YesterglassClientConfig() {
    }
}
