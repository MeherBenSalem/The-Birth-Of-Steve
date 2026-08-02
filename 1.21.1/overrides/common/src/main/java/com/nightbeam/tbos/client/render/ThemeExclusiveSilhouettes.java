package com.nightbeam.tbos.client.render;

import com.nightbeam.tbos.Yesterglass;
import com.nightbeam.tbos.entity.ThemeExclusiveKind;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * The sixteen floor-theme exclusives, one silhouette apiece.
 *
 * <p>They shared a single box-and-two-legs mesh until this table existed, which
 * made every theme's pair read as the same creature in a dark room — the exact
 * failure the five hand-built Archive creatures were designed to avoid. Shape
 * arrives before colour and long before a health bar, so each silhouette here is
 * chosen to telegraph that kind's signature move: the Wake Cutter's blade, the
 * Metronome Hound's pendulum tail, the Ash Chorister's split seam.
 *
 * <p>Each kind gets its own {@link ModelLayerLocation} rather than sharing one
 * baked mesh. A shared layer would force sixteen creatures' worth of boxes into
 * a single 64-unit UV budget; separate layers give each one the whole sheet, and
 * registering an extra layer costs nothing.
 *
 * <p>Meshes are authored at true size — 16 model units to the block — so they
 * match the hitbox declared in {@link ThemeExclusiveKind} without runtime
 * scaling. {@code tools/textures/archive_exclusives.py} mirrors the UV constants
 * and {@code tools/textures/check_uv_sync.py} fails the build if the two drift.
 */
public final class ThemeExclusiveSilhouettes {
    /** Poses one baked silhouette from server-synced render state. */
    @FunctionalInterface
    public interface Poser {
        void pose(Rig rig, ThemeExclusiveRenderState state);
    }

    /**
     * The named parts of one baked silhouette.
     *
     * <p>Parts are resolved once from slash-separated paths declared beside the
     * mesh, because {@code ModelPart} exposes no way to walk its children by
     * name at runtime and a per-frame {@code getChild} chain would allocate.
     */
    public static final class Rig {
        private final ModelPart root;
        private final Map<String, ModelPart> parts;

        private Rig(ModelPart root, String[] paths) {
            this.root = root;
            this.parts = new LinkedHashMap<>(paths.length);
            for (String path : paths) {
                ModelPart part = root;
                for (String step : path.split("/")) {
                    part = part.getChild(step);
                }
                parts.put(path, part);
            }
        }

        public ModelPart root() {
            return root;
        }

        public ModelPart get(String path) {
            ModelPart part = parts.get(path);
            if (part == null) {
                throw new IllegalArgumentException("Silhouette has no part '" + path + "'");
            }
            return part;
        }

        /** True when the mesh declares this part; lets posers share helpers. */
        public boolean has(String path) {
            return parts.containsKey(path);
        }
    }

    private record Spec(
            ModelLayerLocation layer,
            Supplier<LayerDefinition> mesh,
            String[] paths,
            Poser poser,
            float shadowRadius) {
    }

    private static final Map<ThemeExclusiveKind.Silhouette, Spec> SPECS = build();

    private ThemeExclusiveSilhouettes() {
    }

    private static Map<ThemeExclusiveKind.Silhouette, Spec> build() {
        Map<ThemeExclusiveKind.Silhouette, Spec> specs =
                new LinkedHashMap<>(ThemeExclusiveKind.Silhouette.values().length);
        put(specs, ThemeExclusiveKind.Silhouette.ORBIT_SHARDS, ThemeExclusiveMeshes::orbitShards,
                ThemeExclusiveMeshes.ORBIT_SHARDS_PARTS, ThemeExclusivePosers::orbitShards, 0.4F);
        put(specs, ThemeExclusiveKind.Silhouette.SCYTHE, ThemeExclusiveMeshes::scythe,
                ThemeExclusiveMeshes.SCYTHE_PARTS, ThemeExclusivePosers::scythe, 0.4F);
        put(specs, ThemeExclusiveKind.Silhouette.FLAT_FRAME, ThemeExclusiveMeshes::flatFrame,
                ThemeExclusiveMeshes.FLAT_FRAME_PARTS, ThemeExclusivePosers::flatFrame, 0.45F);
        put(specs, ThemeExclusiveKind.Silhouette.WINGED, ThemeExclusiveMeshes::winged,
                ThemeExclusiveMeshes.WINGED_PARTS, ThemeExclusivePosers::winged, 0.3F);
        put(specs, ThemeExclusiveKind.Silhouette.HEAVY_KNIGHT, ThemeExclusiveMeshes::heavyKnight,
                ThemeExclusiveMeshes.HEAVY_KNIGHT_PARTS, ThemeExclusivePosers::heavyKnight, 0.5F);
        put(specs, ThemeExclusiveKind.Silhouette.RINGED, ThemeExclusiveMeshes::ringed,
                ThemeExclusiveMeshes.RINGED_PARTS, ThemeExclusivePosers::ringed, 0.3F);
        put(specs, ThemeExclusiveKind.Silhouette.ROBED, ThemeExclusiveMeshes::robed,
                ThemeExclusiveMeshes.ROBED_PARTS, ThemeExclusivePosers::robed, 0.4F);
        put(specs, ThemeExclusiveKind.Silhouette.SPLIT_CORE, ThemeExclusiveMeshes::splitCore,
                ThemeExclusiveMeshes.SPLIT_CORE_PARTS, ThemeExclusivePosers::splitCore, 0.35F);
        put(specs, ThemeExclusiveKind.Silhouette.PRISM, ThemeExclusiveMeshes::prism,
                ThemeExclusiveMeshes.PRISM_PARTS, ThemeExclusivePosers::prism, 0.4F);
        put(specs, ThemeExclusiveKind.Silhouette.SWARM, ThemeExclusiveMeshes::swarm,
                ThemeExclusiveMeshes.SWARM_PARTS, ThemeExclusivePosers::swarm, 0.3F);
        put(specs, ThemeExclusiveKind.Silhouette.PAGE, ThemeExclusiveMeshes::page,
                ThemeExclusiveMeshes.PAGE_PARTS, ThemeExclusivePosers::page, 0.35F);
        put(specs, ThemeExclusiveKind.Silhouette.CLINGER, ThemeExclusiveMeshes::clinger,
                ThemeExclusiveMeshes.CLINGER_PARTS, ThemeExclusivePosers::clinger, 0.5F);
        put(specs, ThemeExclusiveKind.Silhouette.HOUND, ThemeExclusiveMeshes::hound,
                ThemeExclusiveMeshes.HOUND_PARTS, ThemeExclusivePosers::hound, 0.45F);
        put(specs, ThemeExclusiveKind.Silhouette.USHER, ThemeExclusiveMeshes::usher,
                ThemeExclusiveMeshes.USHER_PARTS, ThemeExclusivePosers::usher, 0.4F);
        put(specs, ThemeExclusiveKind.Silhouette.BLANK, ThemeExclusiveMeshes::blank,
                ThemeExclusiveMeshes.BLANK_PARTS, ThemeExclusivePosers::blank, 0.4F);
        put(specs, ThemeExclusiveKind.Silhouette.LONG_ARM, ThemeExclusiveMeshes::longArm,
                ThemeExclusiveMeshes.LONG_ARM_PARTS, ThemeExclusivePosers::longArm, 0.4F);
        return Map.copyOf(specs);
    }

    private static void put(
            Map<ThemeExclusiveKind.Silhouette, Spec> specs,
            ThemeExclusiveKind.Silhouette silhouette,
            Supplier<LayerDefinition> mesh,
            String[] paths,
            Poser poser,
            float shadowRadius) {
        specs.put(silhouette, new Spec(
                new ModelLayerLocation(
                        ResourceLocation.fromNamespaceAndPath(
                                Yesterglass.MOD_ID, "theme_exclusive/" + name(silhouette)),
                        "main"),
                mesh,
                paths,
                poser,
                shadowRadius));
    }

    private static String name(ThemeExclusiveKind.Silhouette silhouette) {
        return silhouette.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static Spec spec(ThemeExclusiveKind.Silhouette silhouette) {
        Spec spec = SPECS.get(silhouette);
        if (spec == null) {
            throw new IllegalStateException("No silhouette mesh for " + silhouette);
        }
        return spec;
    }

    public static ModelLayerLocation layer(ThemeExclusiveKind.Silhouette silhouette) {
        return spec(silhouette).layer();
    }

    public static Poser poser(ThemeExclusiveKind.Silhouette silhouette) {
        return spec(silhouette).poser();
    }

    public static float shadowRadius(ThemeExclusiveKind.Silhouette silhouette) {
        return spec(silhouette).shadowRadius();
    }

    public static Rig rig(ThemeExclusiveKind.Silhouette silhouette, ModelPart root) {
        return new Rig(root, spec(silhouette).paths());
    }

    /** Feeds every layer to {@code RegisterLayerDefinitions} in one pass. */
    public static void forEachLayer(BiConsumer<ModelLayerLocation, Supplier<LayerDefinition>> sink) {
        for (Spec spec : SPECS.values()) {
            sink.accept(spec.layer(), spec.mesh());
        }
    }
}
