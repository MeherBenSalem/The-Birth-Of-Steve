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
import com.nightbeam.tbos.client.render.MinotaurRenderer;
import com.nightbeam.tbos.client.render.ParallaxWraithRenderer;
import com.nightbeam.tbos.client.render.PhoenixGuardianRenderer;
import com.nightbeam.tbos.client.render.ThemeExclusiveRenderer;
import com.nightbeam.tbos.entity.ThemeExclusiveKind;
import com.nightbeam.tbos.neoforge.platform.NeoForgeNetworkHelper;
import com.nightbeam.tbos.network.payload.ArchiveFloorIntroPayload;
import com.nightbeam.tbos.network.payload.ArchivePuzzlePayload;
import com.nightbeam.tbos.network.payload.ArchiveQuestPayload;
import com.nightbeam.tbos.network.payload.BeginTransitionPayload;
import com.nightbeam.tbos.network.payload.JournalQuestSnapshotPayload;
import com.nightbeam.tbos.network.payload.SiteSnapshotPayload;
import com.nightbeam.tbos.network.payload.TbosPacket;
import com.nightbeam.tbos.registry.ModBlockEntities;
import com.nightbeam.tbos.registry.ModBlocks;
import com.nightbeam.tbos.registry.ModEntities;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.api.distmarker.OnlyIn;


/**
 * Forge 1.20.1 client bootstrap.
 *
 * <p>Forge's {@code @Mod} carries no dist attribute and forbids two containers
 * for one mod id. The main {@link TbosNeoForge} entry calls {@link #init()} via
 * {@code DistExecutor} on the client, and the {@code static void} methods below
 * are picked up by {@code @Mod.EventBusSubscriber} scanning on the client dist.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Yesterglass.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TbosNeoForgeClient {
    private TbosNeoForgeClient() {
    }

    /** Invoked once from {@link TbosNeoForge}'s constructor on the client dist. */
    public static void init() {
        YesterglassClient.init();
        registerClientDispatch();
    }

    @SubscribeEvent
    public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(),
                "archive_quest",
                (forgeGui, graphics, partialTick, width, height) -> ArchiveQuestHud.render(graphics, partialTick));
        event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(),
                "archive_puzzle",
                (forgeGui, graphics, partialTick, width, height) -> ArchivePuzzleHud.render(graphics, partialTick));
        event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(),
                "archive_floor_intro",
                (forgeGui, graphics, partialTick, width, height) -> ArchiveFloorIntroHud.render(graphics, partialTick));
    }

    @SubscribeEvent
    public static void registerRenderLayers(FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.INK_POOL.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.LIGHT_DUST.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.MEMORY_IMPRINT.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.PARALLAX_PANEL.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.SHATTER_PANE.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.YESTERGLASS.get(), RenderType.translucent());
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.TOGGLE_OBJECTIVES);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
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
        for (ThemeExclusiveKind kind : ThemeExclusiveKind.values()) {
            event.registerEntityRenderer(
                    ModEntities.themeExclusive(kind).get(),
                    context -> new ThemeExclusiveRenderer(context, kind));
        }
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        YesterglassClient.forEachLayerDefinition(event::registerLayerDefinition);
    }

    private static void registerClientDispatch() {
        Map<ResourceLocation, Consumer<TbosPacket>> dispatch = NeoForgeNetworkHelper.clientDispatch();
        dispatch.put(BeginTransitionPayload.ID, packet -> ClientNetwork.handleBeginTransition((BeginTransitionPayload) packet));
        dispatch.put(SiteSnapshotPayload.ID, packet -> ClientNetwork.handleSiteSnapshot((SiteSnapshotPayload) packet));
        dispatch.put(ArchiveQuestPayload.ID, packet -> ClientNetwork.handleArchiveQuest((ArchiveQuestPayload) packet));
        dispatch.put(ArchivePuzzlePayload.ID, packet -> ClientNetwork.handleArchivePuzzle((ArchivePuzzlePayload) packet));
        dispatch.put(ArchiveFloorIntroPayload.ID, packet -> ClientNetwork.handleArchiveFloorIntro((ArchiveFloorIntroPayload) packet));
        dispatch.put(JournalQuestSnapshotPayload.ID, packet -> ClientNetwork.handleJournalQuestSnapshot((JournalQuestSnapshotPayload) packet));
    }

    @Mod.EventBusSubscriber(value = Dist.CLIENT, modid = Yesterglass.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class GameBus {
        private GameBus() {
        }

        @SubscribeEvent
        public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
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

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                ClientEvents.onClientTick(Minecraft.getInstance());
            }
        }
    }
}
