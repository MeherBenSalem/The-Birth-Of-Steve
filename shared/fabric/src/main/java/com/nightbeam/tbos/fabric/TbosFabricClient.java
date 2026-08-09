package com.nightbeam.tbos.fabric;

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
import com.nightbeam.tbos.client.render.MinotaurRenderer;
import com.nightbeam.tbos.client.render.PhoenixGuardianRenderer;
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
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;

public final class TbosFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        YesterglassClient.init();
        registerClientPayloads();

        KeyMappingHelper.registerKeyMapping(ModKeyMappings.TOGGLE_OBJECTIVES);

        YesterglassClient.forEachLayerDefinition(
                (layer, definition) -> ModelLayerRegistry.registerModelLayer(layer, definition::get));
        registerRenderers();
        registerHudElements();

        ClientTickEvents.END_CLIENT_TICK.register(ClientEvents::onClientTick);

        // Two callbacks because a right-click that hits a block never reaches
        // UseItemCallback. Both funnel into the same verdict.
        UseItemCallback.EVENT.register((player, level, hand) -> interact(hand));
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> interact(hand));
    }

    private static InteractionResult interact(net.minecraft.world.InteractionHand hand) {
        return switch (ClientEvents.onUseItem(hand)) {
            // FAIL stops the interaction without an arm swing; SUCCESS swings it.
            case CONSUME_NO_SWING -> InteractionResult.FAIL;
            case CONSUME_AND_SWING -> InteractionResult.SUCCESS;
            case PASS -> InteractionResult.PASS;
        };
    }

    private static void registerClientPayloads() {
        ClientPlayNetworking.registerGlobalReceiver(BeginTransitionPayload.TYPE,
                (payload, context) -> context.client().execute(
                        () -> ClientNetwork.handleBeginTransition(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SiteSnapshotPayload.TYPE,
                (payload, context) -> context.client().execute(
                        () -> ClientNetwork.handleSiteSnapshot(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ArchiveQuestPayload.TYPE,
                (payload, context) -> context.client().execute(
                        () -> ClientNetwork.handleArchiveQuest(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ArchivePuzzlePayload.TYPE,
                (payload, context) -> context.client().execute(
                        () -> ClientNetwork.handleArchivePuzzle(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ArchiveFloorIntroPayload.TYPE,
                (payload, context) -> context.client().execute(
                        () -> ClientNetwork.handleArchiveFloorIntro(payload)));
        ClientPlayNetworking.registerGlobalReceiver(JournalQuestSnapshotPayload.TYPE,
                (payload, context) -> context.client().execute(
                        () -> ClientNetwork.handleJournalQuestSnapshot(payload)));
    }

    private static void registerRenderers() {
        BlockEntityRendererRegistry.register(ModBlockEntities.MEMORY_LANTERN.get(), MemoryLanternRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.ARCHIVE_CORE.get(), ArchiveCoreRenderer::new);
        BlockEntityRendererRegistry.register(ModBlockEntities.ALIGNMENT_DIAL.get(), AlignmentDialRenderer::new);
        EntityRendererRegistry.register(ModEntities.MEMORY_LEECH.get(), MemoryLeechRenderer::new);
        EntityRendererRegistry.register(ModEntities.LENSWARD.get(), LenswardRenderer::new);
        EntityRendererRegistry.register(ModEntities.PARALLAX_WRAITH.get(), ParallaxWraithRenderer::new);
        EntityRendererRegistry.register(ModEntities.MERIDIAN_SENTINEL.get(), MeridianSentinelRenderer::new);
        EntityRendererRegistry.register(ModEntities.HOUR_CANTOR.get(), HourCantorRenderer::new);
        EntityRendererRegistry.register(ModEntities.PHOENIX_GUARDIAN.get(), PhoenixGuardianRenderer::new);
        EntityRendererRegistry.register(ModEntities.MINOTAUR.get(), MinotaurRenderer::new);
        // Each kind renders through its own silhouette, so the renderer needs to
        // know which one it is serving before it can bake a layer.
        for (ThemeExclusiveKind kind : ThemeExclusiveKind.values()) {
            EntityRendererRegistry.register(
                    ModEntities.themeExclusive(kind).get(),
                    context -> new ThemeExclusiveRenderer(context, kind));
        }
    }

    private static void registerHudElements() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive_quest"),
                ArchiveQuestHud::render);
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive_puzzle"),
                ArchivePuzzleHud::render);
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "archive_floor_intro"),
                ArchiveFloorIntroHud::render);
    }
}
