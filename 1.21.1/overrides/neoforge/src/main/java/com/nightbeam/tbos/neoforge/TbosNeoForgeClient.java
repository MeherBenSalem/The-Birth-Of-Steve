package com.nightbeam.tbos.neoforge;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.client.ArchiveFloorIntroHud;
import com.nightbeam.tbos.client.ArchivePuzzleHud;
import com.nightbeam.tbos.client.ArchiveQuestHud;
import com.nightbeam.tbos.client.ClientEvents;
import com.nightbeam.tbos.client.ModKeyMappings;
import com.nightbeam.tbos.client.YesterglassClient;
import com.nightbeam.tbos.client.render.AlignmentDialRenderer;
import com.nightbeam.tbos.client.render.ArchiveCoreRenderer;
import com.nightbeam.tbos.client.render.HourCantorRenderer;
import com.nightbeam.tbos.client.render.LenswardRenderer;
import com.nightbeam.tbos.client.render.MemoryLanternRenderer;
import com.nightbeam.tbos.client.render.MemoryLeechRenderer;
import com.nightbeam.tbos.client.render.MeridianSentinelRenderer;
import com.nightbeam.tbos.client.render.MinotaurRenderer;
import com.nightbeam.tbos.client.render.ParallaxWraithRenderer;
import com.nightbeam.tbos.client.render.PhoenixGuardianRenderer;
import com.nightbeam.tbos.client.render.ThemeExclusiveRenderer;
import com.nightbeam.tbos.entity.ThemeExclusiveKind;
import com.nightbeam.tbos.registry.ModBlockEntities;
import com.nightbeam.tbos.registry.ModBlocks;
import com.nightbeam.tbos.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Yesterglass.MOD_ID, dist = Dist.CLIENT)
public final class TbosNeoForgeClient {
    public TbosNeoForgeClient(IEventBus modBus) {
        YesterglassClient.init();
        modBus.addListener(TbosNeoForgeClient::registerRenderLayers);
        modBus.addListener(TbosNeoForgeClient::registerKeyMappings);
        modBus.addListener(TbosNeoForgeClient::registerLayerDefinitions);
        modBus.addListener(TbosNeoForgeClient::registerRenderers);
        modBus.addListener(TbosNeoForgeClient::registerGuiLayers);
        NeoForge.EVENT_BUS.addListener(TbosNeoForgeClient::onInteractionKey);
        NeoForge.EVENT_BUS.addListener(TbosNeoForgeClient::onClientTick);
    }

    /** Keeps the target-local 1.21.1 legacy model adapters translucent. */
    private static void registerRenderLayers(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.INK_POOL.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIGHT_DUST.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.MEMORY_IMPRINT.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.PARALLAX_PANEL.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.SHATTER_PANE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.YESTERGLASS.get(), RenderType.translucent());
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.TOGGLE_OBJECTIVES);
    }

    private static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive_quest"),
                (graphics, deltaTracker) -> ArchiveQuestHud.render(
                        graphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive_puzzle"),
                (graphics, deltaTracker) -> ArchivePuzzleHud.render(
                        graphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive_floor_intro"),
                (graphics, deltaTracker) -> ArchiveFloorIntroHud.render(
                        graphics, deltaTracker.getGameTimeDeltaPartialTick(false)));
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
        event.registerEntityRenderer(ModEntities.PHOENIX_GUARDIAN.get(), PhoenixGuardianRenderer::new);
        event.registerEntityRenderer(ModEntities.MINOTAUR.get(), MinotaurRenderer::new);
        // Each kind renders through its own silhouette, so the renderer needs to
        // know which one it is serving before it can bake a layer.
        for (ThemeExclusiveKind kind : ThemeExclusiveKind.values()) {
            event.registerEntityRenderer(
                    ModEntities.themeExclusive(kind).get(),
                    context -> new ThemeExclusiveRenderer(context, kind));
        }
    }

    private static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        YesterglassClient.forEachLayerDefinition(event::registerLayerDefinition);
    }

    private static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return;
        }
        switch (ClientEvents.onUseItem(event.getHand())) {
            case CONSUME_NO_SWING -> {
                event.setCanceled(true);
                event.setSwingHand(false);
            }
            case CONSUME_AND_SWING -> {
                event.setCanceled(true);
                event.setSwingHand(true);
            }
            case PASS -> {
            }
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ClientEvents.onClientTick(Minecraft.getInstance());
    }
}
