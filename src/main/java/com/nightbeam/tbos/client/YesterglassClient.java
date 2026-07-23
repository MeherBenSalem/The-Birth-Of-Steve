package com.nightbeam.tbos.client;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.config.YesterglassClientConfig;
import com.nightbeam.tbos.client.render.MemoryLanternRenderer;
import com.nightbeam.tbos.client.render.ArchiveZombieRenderer;
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
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.MEMORY_LANTERN.get(), MemoryLanternRenderer::new);
        event.registerEntityRenderer(
                ModEntities.PARALLAX_WRAITH.get(),
                context -> new ArchiveZombieRenderer<>(context, texture("parallax_wraith"), 0.45F));
        event.registerEntityRenderer(
                ModEntities.MERIDIAN_SENTINEL.get(),
                context -> new ArchiveZombieRenderer<>(context, texture("meridian_sentinel"), 0.55F));
        event.registerEntityRenderer(
                ModEntities.HOUR_CANTOR.get(),
                context -> new ArchiveZombieRenderer<>(context, texture("hour_cantor"), 0.8F));
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(Yesterglass.MOD_ID, "textures/entity/" + name + ".png");
    }
}
