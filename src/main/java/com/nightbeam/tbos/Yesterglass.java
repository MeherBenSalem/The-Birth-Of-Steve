package com.nightbeam.tbos;

import com.nightbeam.tbos.config.YesterglassConfig;
import com.nightbeam.tbos.gametest.ModGameTests;
import com.nightbeam.tbos.network.YesterglassNetwork;
import com.nightbeam.tbos.registry.ModBlockEntities;
import com.nightbeam.tbos.registry.ModBlocks;
import com.nightbeam.tbos.registry.ModCreativeModeTabs;
import com.nightbeam.tbos.registry.ModDataComponents;
import com.nightbeam.tbos.registry.ModEntities;
import com.nightbeam.tbos.registry.ModItems;
import com.nightbeam.tbos.registry.ModSounds;
import com.nightbeam.tbos.run.ArchiveRunEvents;
import com.nightbeam.tbos.site.TemporalSiteEvents;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(Yesterglass.MOD_ID)
public final class Yesterglass {
    public static final String MOD_ID = "tbos";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Yesterglass(IEventBus modBus, ModContainer container) {
        ModDataComponents.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModEntities.register(modBus);
        ModSounds.register(modBus);
        ModItems.register(modBus);
        ModCreativeModeTabs.register(modBus);
        ModGameTests.register(modBus);
        modBus.addListener(YesterglassNetwork::register);
        modBus.addListener(ModGameTests::registerTests);
        TemporalSiteEvents.register();
        ArchiveRunEvents.register();
        container.registerConfig(ModConfig.Type.COMMON, YesterglassConfig.SPEC);
    }

}
