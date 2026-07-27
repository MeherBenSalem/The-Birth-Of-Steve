package com.nightbeam.tbos.client;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.config.YesterglassClientConfig;
import com.nightbeam.tbos.client.render.AlignmentDialRenderer;
import com.nightbeam.tbos.client.render.ArchiveCoreRenderer;
import com.nightbeam.tbos.client.render.MemoryLanternRenderer;
import com.nightbeam.tbos.client.render.HourCantorModel;
import com.nightbeam.tbos.client.render.HourCantorRenderer;
import com.nightbeam.tbos.client.render.LenswardModel;
import com.nightbeam.tbos.client.render.LenswardRenderer;
import com.nightbeam.tbos.client.render.MemoryLeechModel;
import com.nightbeam.tbos.client.render.MemoryLeechRenderer;
import com.nightbeam.tbos.client.render.MeridianSentinelModel;
import com.nightbeam.tbos.client.render.MeridianSentinelRenderer;
import com.nightbeam.tbos.client.render.ParallaxWraithModel;
import com.nightbeam.tbos.client.render.ParallaxWraithRenderer;
import com.nightbeam.tbos.client.render.ThemeExclusiveModel;
import com.nightbeam.tbos.client.render.ThemeExclusiveRenderer;
import com.nightbeam.tbos.registry.ModBlockEntities;
import com.nightbeam.tbos.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.minecraft.resources.Identifier;

@Mod(value = Yesterglass.MOD_ID, dist = Dist.CLIENT)
public final class YesterglassClient {
    public YesterglassClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, YesterglassClientConfig.SPEC);
        modBus.addListener(ClientNetwork::register);
        modBus.addListener(ModKeyMappings::register);
        modBus.addListener(YesterglassClient::registerLayerDefinitions);
        modBus.addListener(YesterglassClient::registerRenderers);
        modBus.addListener(YesterglassClient::registerGuiLayers);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onInteractionKey);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive_quest"),
                ArchiveQuestHud::render);
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive_puzzle"),
                ArchivePuzzleHud::render);
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive_floor_intro"),
                ArchiveFloorIntroHud::render);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MEMORY_LANTERN.get(), MemoryLanternRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ARCHIVE_CORE.get(), ArchiveCoreRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ALIGNMENT_DIAL.get(), AlignmentDialRenderer::new);
        event.registerEntityRenderer(ModEntities.MEMORY_LEECH.get(), MemoryLeechRenderer::new);
        event.registerEntityRenderer(ModEntities.LENSWARD.get(), LenswardRenderer::new);
        event.registerEntityRenderer(ModEntities.PARALLAX_WRAITH.get(), ParallaxWraithRenderer::new);
        event.registerEntityRenderer(ModEntities.MERIDIAN_SENTINEL.get(), MeridianSentinelRenderer::new);
        event.registerEntityRenderer(ModEntities.HOUR_CANTOR.get(), HourCantorRenderer::new);
        event.registerEntityRenderer(ModEntities.SHARD_DRIFTER.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.WAKE_CUTTER.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.NULL_PORTRAIT.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.GALLERY_MOTH.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.GNOMON_KNIGHT.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.ARMILLARY_SCOUT.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.DUST_CANTORILE.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.ASH_CHORISTER.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.PRISM_STALKER.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.SHARDLING_SWARM.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.INDEX_WIGHT.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.SHELF_CRAWLER.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.METRONOME_HOUND.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.LABYRINTH_USHER.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.BLANK_CHRONIST.get(), ThemeExclusiveRenderer::new);
        event.registerEntityRenderer(ModEntities.HOUR_HAND_WRAITH.get(), ThemeExclusiveRenderer::new);
    }

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MemoryLeechModel.MODEL_LAYER, MemoryLeechModel::createBodyLayer);
        event.registerLayerDefinition(LenswardModel.MODEL_LAYER, LenswardModel::createBodyLayer);
        event.registerLayerDefinition(ParallaxWraithModel.MODEL_LAYER, ParallaxWraithModel::createBodyLayer);
        event.registerLayerDefinition(
                MeridianSentinelModel.MODEL_LAYER, MeridianSentinelModel::createBodyLayer);
        event.registerLayerDefinition(HourCantorModel.MODEL_LAYER, HourCantorModel::createBodyLayer);
        event.registerLayerDefinition(ThemeExclusiveModel.MODEL_LAYER, ThemeExclusiveModel::createBodyLayer);
    }
}
