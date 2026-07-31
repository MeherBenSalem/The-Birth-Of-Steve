package com.nightbeam.tbos.neoforge;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.client.ArchiveFloorIntroHud;
import com.nightbeam.tbos.client.ArchivePuzzleHud;
import com.nightbeam.tbos.client.ArchiveQuestHud;
import com.nightbeam.tbos.client.ClientEvents;
import com.nightbeam.tbos.client.ClientNetwork;
import com.nightbeam.tbos.client.ModKeyMappings;
import com.nightbeam.tbos.client.YesterglassClient;
import com.nightbeam.tbos.client.render.AlignmentDialRenderer;
import com.nightbeam.tbos.client.render.ArchiveCoreRenderer;
import com.nightbeam.tbos.client.render.HourCantorRenderer;
import com.nightbeam.tbos.client.render.LenswardRenderer;
import com.nightbeam.tbos.client.render.MemoryLanternRenderer;
import com.nightbeam.tbos.client.render.MemoryLeechRenderer;
import com.nightbeam.tbos.client.render.MeridianSentinelRenderer;
import com.nightbeam.tbos.client.render.ParallaxWraithRenderer;
import com.nightbeam.tbos.client.render.ThemeExclusiveRenderer;
import com.nightbeam.tbos.entity.ThemeExclusiveKind;
import com.nightbeam.tbos.network.payload.ArchiveFloorIntroPayload;
import com.nightbeam.tbos.network.payload.ArchivePuzzlePayload;
import com.nightbeam.tbos.network.payload.ArchiveQuestPayload;
import com.nightbeam.tbos.network.payload.BeginTransitionPayload;
import com.nightbeam.tbos.network.payload.JournalQuestSnapshotPayload;
import com.nightbeam.tbos.network.payload.SiteSnapshotPayload;
import com.nightbeam.tbos.registry.ModBlockEntities;
import com.nightbeam.tbos.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Yesterglass.MOD_ID, dist = Dist.CLIENT)
public final class TbosNeoForgeClient {
    public TbosNeoForgeClient(IEventBus modBus) {
        YesterglassClient.init();
        modBus.addListener(TbosNeoForgeClient::registerClientPayloads);
        modBus.addListener(TbosNeoForgeClient::registerKeyMappings);
        modBus.addListener(TbosNeoForgeClient::registerLayerDefinitions);
        modBus.addListener(TbosNeoForgeClient::registerRenderers);
        modBus.addListener(TbosNeoForgeClient::registerGuiLayers);
        NeoForge.EVENT_BUS.addListener(TbosNeoForgeClient::onInteractionKey);
        NeoForge.EVENT_BUS.addListener(TbosNeoForgeClient::onClientTick);
    }

    private static void registerClientPayloads(RegisterClientPayloadHandlersEvent event) {
        event.register(BeginTransitionPayload.TYPE,
                (payload, context) -> ClientNetwork.handleBeginTransition(payload));
        event.register(SiteSnapshotPayload.TYPE,
                (payload, context) -> ClientNetwork.handleSiteSnapshot(payload));
        event.register(ArchiveQuestPayload.TYPE,
                (payload, context) -> ClientNetwork.handleArchiveQuest(payload));
        event.register(ArchivePuzzlePayload.TYPE,
                (payload, context) -> ClientNetwork.handleArchivePuzzle(payload));
        event.register(ArchiveFloorIntroPayload.TYPE,
                (payload, context) -> ClientNetwork.handleArchiveFloorIntro(payload));
        event.register(JournalQuestSnapshotPayload.TYPE,
                (payload, context) -> ClientNetwork.handleJournalQuestSnapshot(payload));
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(ModKeyMappings.CATEGORY);
        event.register(ModKeyMappings.TOGGLE_OBJECTIVES);
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
