package com.nightbeam.tbos;

import com.nightbeam.tbos.config.YesterglassConfig;
import com.nightbeam.tbos.gametest.ModGameTests;
import com.nightbeam.tbos.item.MemoryPlateItem;
import com.nightbeam.tbos.item.MemoryScene;
import com.nightbeam.tbos.network.YesterglassNetwork;
import com.nightbeam.tbos.registry.ModBlockEntities;
import com.nightbeam.tbos.registry.ModBlocks;
import com.nightbeam.tbos.registry.ModDataComponents;
import com.nightbeam.tbos.registry.ModEntities;
import com.nightbeam.tbos.registry.ModItems;
import com.nightbeam.tbos.registry.ModSounds;
import com.nightbeam.tbos.run.ArchiveRunEvents;
import com.nightbeam.tbos.site.TemporalSiteEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
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
        ModGameTests.register(modBus);
        modBus.addListener(YesterglassNetwork::register);
        modBus.addListener(ModGameTests::registerTests);
        modBus.addListener(this::addCreativeTabContents);
        TemporalSiteEvents.register();
        ArchiveRunEvents.register();
        container.registerConfig(ModConfig.Type.COMMON, YesterglassConfig.SPEC);
    }

    private void addCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.CRACKED_YESTERGLASS_LENS.get().getDefaultInstance());
            event.accept(ModItems.YESTERGLASS_LENS.get().getDefaultInstance());
            event.accept(ModItems.CURATOR_CORE.get().getDefaultInstance());
            event.accept(ModItems.CHRONICLE_SHARD.get().getDefaultInstance());
            event.accept(ModItems.RECALLED_HOUR.get().getDefaultInstance());
            event.accept(ModItems.CANTOR_SIGIL.get().getDefaultInstance());
            event.accept(ModItems.ARCHIVE_SURVEY_MAP.get().getDefaultInstance());
            for (MemoryScene scene : MemoryScene.values()) {
                event.accept(MemoryPlateItem.forScene(scene));
            }
        }
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.ARCHIVE_STONE.get().getDefaultInstance());
            event.accept(ModItems.CRACKED_ARCHIVE_STONE.get().getDefaultInstance());
            event.accept(ModItems.ARCHIVE_BRICKS.get().getDefaultInstance());
            event.accept(ModItems.WEATHERED_ARCHIVE_BRICKS.get().getDefaultInstance());
            event.accept(ModItems.MOSSY_ARCHIVE_STONE.get().getDefaultInstance());
            event.accept(ModItems.CHISELED_ARCHIVE_STONE.get().getDefaultInstance());
            event.accept(ModItems.CHRONICLE_TILE.get().getDefaultInstance());
            event.accept(ModItems.CANTOR_FLOOR.get().getDefaultInstance());
            event.accept(ModItems.CANTOR_WALL.get().getDefaultInstance());
            event.accept(ModItems.CANTOR_RUNE.get().getDefaultInstance());
            event.accept(ModItems.MERIDIAN_TILE.get().getDefaultInstance());
            event.accept(ModItems.ENGRAVED_MERIDIAN_TILE.get().getDefaultInstance());
            event.accept(ModItems.YESTERGLASS.get().getDefaultInstance());
            event.accept(ModItems.MEMORY_ANCHOR.get().getDefaultInstance());
            event.accept(ModItems.PHASE_PLATFORM.get().getDefaultInstance());
            event.accept(ModItems.RESONANCE_LAMP.get().getDefaultInstance());
            event.accept(ModItems.CHRONICLE_BRONZE.get().getDefaultInstance());
            event.accept(ModItems.LENSWORK_CRYSTAL.get().getDefaultInstance());
            event.accept(ModItems.ARCHIVE_SEAL.get().getDefaultInstance());
            event.accept(ModItems.ARCHIVE_CACHE.get().getDefaultInstance());
            event.accept(ModItems.FRACTURE_COFFER.get().getDefaultInstance());
            event.accept(ModItems.ALIGNMENT_DIAL.get().getDefaultInstance());
            event.accept(ModItems.RESONANT_BELL.get().getDefaultInstance());
            event.accept(ModItems.MERIDIAN_RELAY.get().getDefaultInstance());
            event.accept(ModItems.ARCHIVE_CORE.get().getDefaultInstance());
            event.accept(ModItems.MEMORY_LANTERN.get().getDefaultInstance());
            event.accept(ModItems.RIFT_THRESHOLD.get().getDefaultInstance());
            event.accept(ModItems.ARCHIVE_CRATE.get().getDefaultInstance());
            event.accept(ModItems.ARCHIVE_CRATE_STACK.get().getDefaultInstance());
            event.accept(ModItems.ARCHIVE_LARGE_CRATE.get().getDefaultInstance());
            event.accept(ModItems.ARCHIVE_LARGE_CRATE_STACK.get().getDefaultInstance());
            event.accept(ModItems.ARCHIVE_BARREL.get().getDefaultInstance());
            event.accept(ModItems.ARCHIVE_BARREL_STACK.get().getDefaultInstance());
            event.accept(ModItems.ARCHIVE_MIXED_STACK_1.get().getDefaultInstance());
            event.accept(ModItems.ARCHIVE_MIXED_STACK_2.get().getDefaultInstance());
            event.accept(ModItems.ARCHIVE_MIXED_STACK_3.get().getDefaultInstance());
        }
    }
}
