package com.nightbeam.tbos;

import com.mojang.logging.LogUtils;
import com.nightbeam.tbos.config.YesterglassConfig;
import com.nightbeam.tbos.gametest.ModGameTests;
import com.nightbeam.tbos.registry.ModBlockEntities;
import com.nightbeam.tbos.registry.ModBlocks;
import com.nightbeam.tbos.registry.ModCreativeModeTabs;
import com.nightbeam.tbos.registry.ModDataComponents;
import com.nightbeam.tbos.registry.ModEntities;
import com.nightbeam.tbos.registry.ModItems;
import com.nightbeam.tbos.registry.ModSounds;
import org.slf4j.Logger;

/**
 * The common entry point.
 *
 * <p>Despite the name this is the whole mod, not a subsystem: the project was
 * renamed from {@code yesterglass} to {@code tbos} and several classes kept the
 * old prefix. {@link #MOD_ID} is {@code "tbos"}.
 *
 * <p>{@link #init()} only queues work. Each {@code Mod*} class fills its
 * registrar from a static initialiser, so touching the class is the
 * registration; the loader flushes the registrars afterwards and wires up its
 * own events, networking and commands.
 */
public final class Yesterglass {
    public static final String MOD_ID = "tbos";
    public static final Logger LOGGER = LogUtils.getLogger();

    private Yesterglass() {
    }

    public static void init() {
        YesterglassConfig.load();
        // Order matters on Fabric, where the registrars are flushed in this
        // order and a BlockItem cannot be built before its block exists.
        ModDataComponents.register();
        ModBlocks.register();
        ModBlockEntities.register();
        ModEntities.register();
        ModSounds.register();
        ModItems.register();
        ModCreativeModeTabs.register();
        ModGameTests.register();
    }
}
