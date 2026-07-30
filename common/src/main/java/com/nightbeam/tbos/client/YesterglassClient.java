package com.nightbeam.tbos.client;

import com.nightbeam.tbos.client.render.HourCantorModel;
import com.nightbeam.tbos.client.render.LenswardModel;
import com.nightbeam.tbos.client.render.MemoryLeechModel;
import com.nightbeam.tbos.client.render.MeridianSentinelModel;
import com.nightbeam.tbos.client.render.ParallaxWraithModel;
import com.nightbeam.tbos.client.render.ThemeExclusiveSilhouettes;
import com.nightbeam.tbos.config.YesterglassClientConfig;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;

/**
 * The common client entry point.
 *
 * <p>Renderer, HUD and key-mapping registration all go through loader-specific
 * registries, so those stay in the loader projects. What lives here is what both
 * of them need in the same shape: the client config, and the entity model
 * layers, which are plain vanilla objects.
 */
public final class YesterglassClient {
    private YesterglassClient() {
    }

    public static void init() {
        YesterglassClientConfig.load();
    }

    /**
     * Feeds every entity model layer to the loader's layer registry.
     *
     * <p>Each theme-exclusive kind bakes its own silhouette, so the sixteen of
     * them come from {@link ThemeExclusiveSilhouettes} rather than being spelled
     * out both here and in the renderer registration.
     */
    public static void forEachLayerDefinition(
            BiConsumer<ModelLayerLocation, Supplier<LayerDefinition>> sink) {
        sink.accept(MemoryLeechModel.MODEL_LAYER, MemoryLeechModel::createBodyLayer);
        sink.accept(LenswardModel.MODEL_LAYER, LenswardModel::createBodyLayer);
        sink.accept(ParallaxWraithModel.MODEL_LAYER, ParallaxWraithModel::createBodyLayer);
        sink.accept(MeridianSentinelModel.MODEL_LAYER, MeridianSentinelModel::createBodyLayer);
        sink.accept(HourCantorModel.MODEL_LAYER, HourCantorModel::createBodyLayer);
        ThemeExclusiveSilhouettes.forEachLayer(sink);
    }
}
