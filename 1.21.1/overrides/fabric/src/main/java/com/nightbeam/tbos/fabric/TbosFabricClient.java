package com.nightbeam.tbos.fabric;

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
import com.nightbeam.tbos.registry.ModBlocks;
import com.nightbeam.tbos.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.RenderType;

/** Fabric 1.21.1 client wiring: old HUD and model-layer registries, same gameplay handlers. */
public final class TbosFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        YesterglassClient.init();
        registerRenderLayers();
        registerClientPayloads();
        KeyBindingHelper.registerKeyBinding(ModKeyMappings.TOGGLE_OBJECTIVES);
        YesterglassClient.forEachLayerDefinition(
                (layer, definition) -> EntityModelLayerRegistry.registerModelLayer(layer, definition::get));
        registerRenderers();
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> {
            float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
            ArchiveQuestHud.render(graphics, partialTick);
            ArchivePuzzleHud.render(graphics, partialTick);
            ArchiveFloorIntroHud.render(graphics, partialTick);
        });
        ClientTickEvents.END_CLIENT_TICK.register(ClientEvents::onClientTick);
        UseItemCallback.EVENT.register((player, level, hand) -> interactItem(player.getItemInHand(hand), hand));
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> interact(hand));
    }

    /**
     * 1.21.1 does not consume the newer per-model translucent texture object used
     * by the shared resource set. Keep these six block models on the translucent
     * render layer after their target-local model adapters restore the legacy JSON
     * form.
     */
    private static void registerRenderLayers() {
        BlockRenderLayerMap.INSTANCE.putBlocks(
                RenderType.translucent(),
                ModBlocks.INK_POOL.get(),
                ModBlocks.LIGHT_DUST.get(),
                ModBlocks.MEMORY_IMPRINT.get(),
                ModBlocks.PARALLAX_PANEL.get(),
                ModBlocks.SHATTER_PANE.get(),
                ModBlocks.YESTERGLASS.get());
    }

    private static InteractionResult interact(InteractionHand hand) {
        return switch (ClientEvents.onUseItem(hand)) {
            case CONSUME_NO_SWING -> InteractionResult.FAIL;
            case CONSUME_AND_SWING -> InteractionResult.SUCCESS;
            case PASS -> InteractionResult.PASS;
        };
    }

    private static InteractionResultHolder<ItemStack> interactItem(ItemStack stack, InteractionHand hand) {
        return new InteractionResultHolder<>(interact(hand), stack);
    }

    private static void registerClientPayloads() {
        ClientPlayNetworking.registerGlobalReceiver(BeginTransitionPayload.TYPE, (payload, context) -> context.client().execute(() -> ClientNetwork.handleBeginTransition(payload)));
        ClientPlayNetworking.registerGlobalReceiver(SiteSnapshotPayload.TYPE, (payload, context) -> context.client().execute(() -> ClientNetwork.handleSiteSnapshot(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ArchiveQuestPayload.TYPE, (payload, context) -> context.client().execute(() -> ClientNetwork.handleArchiveQuest(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ArchivePuzzlePayload.TYPE, (payload, context) -> context.client().execute(() -> ClientNetwork.handleArchivePuzzle(payload)));
        ClientPlayNetworking.registerGlobalReceiver(ArchiveFloorIntroPayload.TYPE, (payload, context) -> context.client().execute(() -> ClientNetwork.handleArchiveFloorIntro(payload)));
        ClientPlayNetworking.registerGlobalReceiver(JournalQuestSnapshotPayload.TYPE, (payload, context) -> context.client().execute(() -> ClientNetwork.handleJournalQuestSnapshot(payload)));
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
        for (ThemeExclusiveKind kind : ThemeExclusiveKind.values()) {
            EntityRendererRegistry.register(ModEntities.themeExclusive(kind).get(), context -> new ThemeExclusiveRenderer(context, kind));
        }
    }
}
